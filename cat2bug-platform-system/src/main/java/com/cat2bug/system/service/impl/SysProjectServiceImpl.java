package com.cat2bug.system.service.impl;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysReport;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.*;
import com.cat2bug.common.utils.uuid.UUID;
import com.cat2bug.system.domain.*;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.mapper.SysUserProjectRoleMapper;
import com.cat2bug.system.service.*;
import com.google.common.base.Preconditions;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 项目Service业务层处理
 * 
 * @author yuzhantao
 * @date 2023-11-13
 */
@Service
public class SysProjectServiceImpl implements ISysProjectService 
{
    /**
     * 接口提交TOKEN标识
     */
    private static final String AUTH_TOKEN_HEADER_NAME = "CAT2BUG-API-KEY";

    @Autowired
    private SysProjectMapper sysProjectMapper;
    @Autowired
    private SysUserProjectMapper sysUserProjectMapper;
    @Autowired
    private SysUserProjectRoleMapper sysUserProjectRoleMapper;
    @Autowired
    private ISysUserConfigService sysUserConfigService;
    @Autowired
    private ISysProjectDefectTabsService sysProjectDefectTabsService;
    @Autowired
    private ISysCaseService sysCaseService;
    @Autowired
    private ISysUserProjectService sysUserProjectService;
    @Autowired
    private ISysDefectService sysDefectService;
    @Autowired
    private ISysPlanService sysPlanService;
    @Autowired
    private ISysModuleService sysModuleService;
    @Autowired
    private ISysReportService sysReportService;
    @Autowired
    private ISysDocumentService sysDocumentService;
    @Autowired
    private ISysProjectApiService sysProjectApiService;

    @Value("${server.port}")
    private int httpPort;

    @Value("${cat2bug.temp}")
    private String tempPath;

    @Value("${cat2bug.version}")
    private String systemVersion;

    @Value("${cat2bug.cloud.host}")
    private String cloudUrl;
    /**
     * 查询项目
     * 
     * @param projectId 项目主键
     * @return 项目
     */
    @Override
    public SysProject selectSysProjectByProjectId(Long projectId)
    {
        return sysProjectMapper.selectSysProjectByProjectId(projectId,SecurityUtils.getUserId());
    }

    /**
     * 查询项目列表
     * 
     * @param sysProject 项目
     * @return 项目
     */
    @Override
    public List<SysProject> selectSysProjectList(SysProject sysProject)
    {
        Preconditions.checkNotNull(sysProject.getTeamId(), MessageUtils.message("project.team_cannot_empty"));
//        // 设置当前用户可查看哪些项目
//        if(sysProject.getParams()==null){
//            sysProject.setParams(new HashMap<>());
//        }
//        sysProject.getParams().put("userId",SecurityUtils.getUserId());

        List<SysProject> projectList = sysProjectMapper.selectSysProjectList(SecurityUtils.getUserId(), sysProject);
        // 设置项目中的成员
        projectList.stream().forEach(p->{
            List<SysUser> members = sysUserProjectMapper.selectSysUserListByProjectId(p.getProjectId(),new SysUser());
            p.setMembers(members);
        });
        return projectList;
    }

    /**
     * 新增项目
     * 
     * @param sysProject 项目
     * @return 结果
     */
    @Transactional
    @Override
    public int insertSysProject(SysProject sysProject)
    {
        // 新增项目
        sysProject.setCreateBy(String.valueOf(SecurityUtils.getUserId()));
        sysProject.setCreateTime(DateUtils.getNowDate());
        sysProject.setUpdateBy(String.valueOf(SecurityUtils.getUserId()));
        sysProject.setUpdateTime(DateUtils.getNowDate());
        Preconditions.checkState(sysProjectMapper.insertSysProject(sysProject)==1,MessageUtils.message("project.insert_project_fail"));

        // 新增项目成员
        sysProject.getMembers().stream().forEach(m->{
            SysUserProject up = new SysUserProject();
            up.setProjectId(sysProject.getProjectId());
            up.setUserId(m.getUserId());
            Preconditions.checkState(sysUserProjectMapper.insertSysUserProject(up)==1,MessageUtils.message("project.insert_user_fail"));
            // 添加缺陷tab配置
            SysProjectDefectTabs tab1 = new SysProjectDefectTabs();
            tab1.setUserId(up.getUserId());
            tab1.setProjectId(up.getProjectId());
            tab1.setTabName(MessageUtils.message("my"));
            SysDefect sysDefect1 = new SysDefect();
            sysDefect1.setHandleBy(Arrays.asList(up.getUserId()));
            tab1.setConfig(sysDefect1);
            sysProjectDefectTabsService.insertSysProjectDefectTabs(tab1);
            // 新增成员角色
            for(Long roleId : m.getRoleIds()){
                SysUserProjectRole role = new SysUserProjectRole();
                role.setRoleId(roleId);
                role.setUserProjectId(up.getUserProjectId());
                Preconditions.checkState(sysUserProjectRoleMapper.insertSysUserProjectRole(role)==1,MessageUtils.message("project.insert_user_role_fail"));
            }
        });
        return 1;
    }

    /**
     * 修改项目
     * 
     * @param sysProject 项目
     * @return 结果
     */
    @Override
    public int updateSysProject(SysProject sysProject)
    {
        sysProject.setUpdateTime(DateUtils.getNowDate());
        return sysProjectMapper.updateSysProject(sysProject);
    }

    /**
     * 批量删除项目
     * 
     * @param projectIds 需要删除的项目主键
     * @return 结果
     */
    @Override
    public int deleteSysProjectByProjectIds(Long[] projectIds)
    {
        return sysProjectMapper.deleteSysProjectByProjectIds(projectIds);
    }

    /**
     * 删除项目信息
     * 
     * @param projectId 项目主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteSysProjectByProjectId(Long projectId)
    {
        int ret = sysProjectMapper.deleteSysProjectByProjectId(projectId);
        SysUserConfig sysUserConfig = sysUserConfigService.selectSysUserConfigByCurrentUserId();
        // 如果删除的项目是当前用户项目，就选择另外一个项目作为当前项目
        if(sysUserConfig.getCurrentProjectId()==projectId) {
            // 查找当前用户在当前团队中，有哪些项目可操作
            SysProject sysProject = new SysProject();
            Map<String,Object> params = new HashMap<>();
            params.put("userId",SecurityUtils.getUserId());
            sysProject.setParams(params);
            sysProject.setTeamId(sysUserConfig.getCurrentTeamId());
            List<SysProject> projectList = sysProjectMapper.selectSysProjectList(SecurityUtils.getUserId(), sysProject);

            // 将查到的第一个非projectId项目作为当前默认项目
            Optional<SysProject> potProject = projectList.stream().filter(p->p.getProjectId()!=projectId).findFirst();
            if(potProject.isPresent()){
                sysUserConfig.setCurrentProjectId(potProject.get().getProjectId());
            } else {
                sysUserConfig.setCurrentProjectId(0L);
            }
            sysUserConfigService.updateSysUserConfig(sysUserConfig);
        }
        return ret;
    }

    @Override
    public boolean pushToCloud(Long projectId, String pullKey) throws IOException {
        // 创建文件夹
        File file = new File(this.tempPath);
        if(file.exists()==false) {
            file.mkdirs();
        }
        // 创建压缩文件
        String fileExtensions = "zip";
        String fileName = String.format("tempPullProject-%s.%s", UUID.fastUUID().toString(), fileExtensions);
        String compressFileName = String.format("%s/%s", tempPath, fileName);
        final CompressUtil compressUtil = CompressUtil.createStream(compressFileName);
        try {
            // 系统
            Map<String, Object> system = new HashMap<>();
            system.put("version", this.systemVersion);
            system.put("pushKey", pullKey);
            compressUtil.addJsonFile("system.json", system);
            // 项目
            SysProject project = this.selectSysProjectByProjectId(projectId);
            compressUtil.addJsonFile("project.json", project);
            // 成员
            List<SysUser> memberList = sysUserProjectService.selectNotSysUserListByProjectId(projectId, new SysUser());
            compressUtil.addJsonFile("member.json", memberList);
            for (SysUser member : memberList) {
                if(StringUtils.isNotBlank(member.getAvatar())){
                    this.compressFile(compressUtil, member.getAvatar());;
                }
            }
            // 用例
            SysCase sysCase = new SysCase();
            sysCase.setProjectId(projectId);
            List<SysCase> caseList = this.sysCaseService.selectSysCaseList(sysCase);
            compressUtil.addJsonFile("case.json", caseList);
            for(SysCase c : caseList) {
                // 压缩图片
                if(StringUtils.isNotBlank(c.getImgUrls())){
                    String[] imgs = c.getImgUrls().split(",");
                    for(String img : imgs) {
                        this.compressFile(compressUtil, img);
                    }
                }
                // 压缩附件
                if(StringUtils.isNotBlank(c.getAnnexUrls())){
                    String[] annexs = c.getAnnexUrls().split(",");
                    for(String annex : annexs) {
                        this.compressFile(compressUtil, annex);
                    }
                }
            }
            // 缺陷
            SysDefect sysDefect = new SysDefect();
            sysDefect.setProjectId(projectId);
            List<SysDefect> defectList = sysDefectService.selectSysDefectList(sysDefect);
            compressUtil.addJsonFile("defect.json", defectList);
            for(SysDefect d : defectList) {
                // 压缩图片
                if(StringUtils.isNotBlank(d.getImgUrls())){
                    String[] imgs = d.getImgUrls().split(",");
                    for(String img : imgs) {
                        this.compressFile(compressUtil, img);
                    }
                }
                // 压缩附件
                if(StringUtils.isNotBlank(d.getAnnexUrls())){
                    String[] annexs = d.getAnnexUrls().split(",");
                    for(String annex : annexs) {
                        this.compressFile(compressUtil, annex);
                    }
                }
            }
            // 计划
            SysPlan sysPlan = new SysPlan();
            sysPlan.setProjectId(projectId);
            List<SysPlan> planList = sysPlanService.selectSysPlanList(sysPlan);
            compressUtil.addJsonFile("plan.json", planList);
            // 交付物
            SysModule sysModule = new SysModule();
            sysModule.setProjectId(projectId);
            List<SysModule> moduleList = sysModuleService.selectSysModuleList(sysModule);
            compressUtil.addJsonFile("module.json", moduleList);
            // 报告
            SysReport sysReport = new SysReport();
            sysReport.setProjectId(projectId);
            List<SysReport> reportList = sysReportService.selectSysReportList(sysReport);
            compressUtil.addJsonFile("report.json", reportList);
            // 文档
            SysDocument sysDocument = new SysDocument();
            sysDocument.setProjectId(projectId);
            List<SysDocument> documentList = sysDocumentService.selectSysDocumentList(sysDocument);
            compressUtil.addJsonFile("document.json", documentList);
            for(SysDocument doc : documentList) {
                if(StringUtils.isNotBlank(doc.getFileUrl())) {
                    this.compressFile(compressUtil, doc.getFileUrl());
                }
            }
            // API
            SysProjectApi sysProjectApi = new SysProjectApi();
            sysProjectApi.setProjectId(projectId);
            List<SysProjectApi> sysProjectApiList = sysProjectApiService.selectSysProjectApiList(sysProjectApi);
            compressUtil.addJsonFile("api.json", sysProjectApiList);
        } finally {
            compressUtil.build();
        }

        OkHttpClient client = new OkHttpClient();
        File compressFile = new File(compressFileName);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("version", this.systemVersion)
                .addFormDataPart("file", fileName, RequestBody.create(MediaType.parse("application/zip"), compressFile))
                .build();
        String url = cloudUrl+"/api/project/push";
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .header(AUTH_TOKEN_HEADER_NAME, pullKey)
                .build();

        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            return true;
        }
        return false;
    }

    private void compressFile(CompressUtil compressUtil, String fileName) throws IOException {
        compressUtil.addUrlFile(fileName, String.format("http://127.0.0.1:%d%s",httpPort,fileName));
    }
}
