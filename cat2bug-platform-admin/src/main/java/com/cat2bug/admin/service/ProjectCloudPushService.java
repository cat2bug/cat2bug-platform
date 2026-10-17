package com.cat2bug.admin.service;

import com.cat2bug.common.domain.*;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.CompressUtil;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.mapper.SysCommentMapper;
import com.cat2bug.system.mapper.SysDefectMapper;
import com.cat2bug.system.mapper.SysDocumentMapper;
import com.cat2bug.system.mapper.SysPlanMapper;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.service.SysCaseService;
import com.cat2bug.system.service.SysDefectLogService;
import com.cat2bug.system.service.SysModuleService;
import com.cat2bug.system.service.SysUserProjectService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProjectCloudPushService {

    private static final Logger LOG = Logger.getLogger(ProjectCloudPushService.class);
    private static final String AUTH_TOKEN_HEADER_NAME = "CAT2BUG-API-KEY";
    private static final String DEFECT_LOG_COMMENT_TYPE = "defect_log";
    private static final int LIST_ALL_LIMIT = Integer.MAX_VALUE;

    @Inject
    SysProjectMapper sysProjectMapper;

    @Inject
    SysUserProjectService sysUserProjectService;

    @Inject
    SysCaseService sysCaseService;

    @Inject
    SysDefectMapper sysDefectMapper;

    @Inject
    SysDefectLogService sysDefectLogService;

    @Inject
    SysCommentMapper sysCommentMapper;

    @Inject
    SysPlanMapper sysPlanMapper;

    @Inject
    SysModuleService sysModuleService;

    @Inject
    SysDocumentMapper sysDocumentMapper;

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "2020")
    int httpPort;

    @ConfigProperty(name = "cat2bug.temp", defaultValue = "./uploadTemp")
    String tempPath;

    @ConfigProperty(name = "cat2bug.version", defaultValue = "1.0.0")
    String systemVersion;

    @ConfigProperty(name = "cat2bug.cloud.host", defaultValue = "https://www.cat2bug.com/cloud/cloud-api")
    String cloudUrl;

    public boolean pushToCloud(Long projectId, String pullKey) throws IOException {
        if (projectId == null) {
            throw new ApiException("项目ID不能为空");
        }
        if (StringUtils.isBlank(pullKey)) {
            throw new ApiException("同步密钥不能为空");
        }
        Path tempDir = Path.of(tempPath);
        Files.createDirectories(tempDir);
        String fileName = "tempPullProject-" + UUID.randomUUID() + ".zip";
        String compressFileName = tempDir.resolve(fileName).toString();
        CompressUtil compressUtil = CompressUtil.createStream(compressFileName);
        try {
            SysProjectPush projectPush = new SysProjectPush();
            projectPush.setVersion(systemVersion);
            projectPush.setPushKey(pullKey);
            compressUtil.addJsonFile("system.json", projectPush);

            SysProject project = sysProjectMapper.selectSysProjectByProjectId(projectId, null);
            compressUtil.addJsonFile("project.json", project);

            List<SysUser> memberList = sysUserProjectService.selectSysUserListByProjectId(projectId);
            compressUtil.addJsonFile("member.json", memberList);
            for (SysUser member : memberList) {
                if (StringUtils.isNotBlank(member.getAvatar())) {
                    compressFile(compressUtil, member.getAvatar());
                }
            }

            SysCase caseQuery = new SysCase();
            caseQuery.setProjectId(projectId);
            List<SysCase> caseList = sysCaseService.selectSysCaseListAll(caseQuery);
            compressUtil.addJsonFile("case.json", caseList);
            for (SysCase c : caseList) {
                compressUrls(compressUtil, c.getImgUrls());
                compressUrls(compressUtil, c.getAnnexUrls());
            }

            SysDefect defectQuery = new SysDefect();
            defectQuery.setProjectId(projectId);
            List<SysDefect> defectList = sysDefectMapper.selectSysDefectList(defectQuery, 0, LIST_ALL_LIMIT);
            compressUtil.addJsonFile("defect.json", defectList);
            for (SysDefect d : defectList) {
                compressUrls(compressUtil, d.getImgUrls());
                compressUrls(compressUtil, d.getAnnexUrls());
            }

            List<SysDefectLog> defectLogList = new ArrayList<>();
            for (SysDefect d : defectList) {
                SysDefectLog logQuery = new SysDefectLog();
                logQuery.setDefectId(d.getDefectId());
                defectLogList.addAll(sysDefectLogService.selectSysDefectLogList(logQuery));
            }
            defectLogList = defectLogList.stream().filter(l -> l != null).collect(Collectors.toList());
            compressUtil.addJsonFile("defect_log.json", defectLogList);

            List<SysComment> defectCommentList = new ArrayList<>();
            for (SysDefectLog log : defectLogList) {
                SysComment comment = new SysComment();
                comment.setCorrelationId(log.getDefectId());
                comment.setModuleType(DEFECT_LOG_COMMENT_TYPE);
                defectCommentList.addAll(sysCommentMapper.selectSysCommentList(comment, 0, LIST_ALL_LIMIT));
            }
            defectCommentList = defectCommentList.stream().filter(l -> l != null).collect(Collectors.toList());
            compressUtil.addJsonFile("defect_log_comment.json", defectCommentList);

            SysPlan planQuery = new SysPlan();
            planQuery.setProjectId(projectId);
            List<SysPlan> planList = sysPlanMapper.selectSysPlanList(planQuery, 0, LIST_ALL_LIMIT);
            for (SysPlan plan : planList) {
                plan.setSysPlanItemList(sysPlanMapper.selectSysPlanItemListByPlanId(plan.getPlanId()));
            }
            compressUtil.addJsonFile("plan.json", planList);

            SysModule moduleQuery = new SysModule();
            moduleQuery.setProjectId(projectId);
            compressUtil.addJsonFile("module.json", sysModuleService.selectSysModuleList(moduleQuery));

            SysDocument documentQuery = new SysDocument();
            documentQuery.setProjectId(projectId);
            List<SysDocument> documentList =
                    sysDocumentMapper.selectSysDocumentList(documentQuery, 0, LIST_ALL_LIMIT);
            compressUtil.addJsonFile("document.json", documentList);
            for (SysDocument doc : documentList) {
                if (StringUtils.isNotBlank(doc.getFileUrl())) {
                    compressFile(compressUtil, doc.getFileUrl());
                }
            }
        } catch (Exception e) {
            LOG.error("打包项目同步文件失败", e);
            throw e instanceof IOException ? (IOException) e : new IOException(e);
        } finally {
            compressUtil.build();
        }

        File compressFile = new File(compressFileName);
        try {
            String url = cloudUrl + "/api/project/push";
            return postZip(url, pullKey.trim(), systemVersion, fileName, compressFile);
        } finally {
            if (!compressFile.delete()) {
                LOG.warnf("临时同步文件删除失败: %s", compressFileName);
            }
        }
    }

    private static boolean postZip(String url, String pullKey, String version, String fileName, File file)
            throws IOException {
        String boundary = "----Cat2Bug" + UUID.randomUUID().toString().replace("-", "");
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        String preamble = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"version\"\r\n\r\n"
                + version + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: application/zip\r\n\r\n";
        body.write(preamble.getBytes(StandardCharsets.UTF_8));
        body.write(fileBytes);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header(AUTH_TOKEN_HEADER_NAME, pullKey)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    private void compressUrls(CompressUtil compressUtil, String urls) throws IOException {
        if (StringUtils.isBlank(urls)) {
            return;
        }
        for (String url : urls.split(",")) {
            if (StringUtils.isNotBlank(url)) {
                compressFile(compressUtil, url.trim());
            }
        }
    }

    private void compressFile(CompressUtil compressUtil, String fileName) throws IOException {
        compressUtil.addUrlFile(fileName, "http://127.0.0.1:" + httpPort + fileName);
    }
}
