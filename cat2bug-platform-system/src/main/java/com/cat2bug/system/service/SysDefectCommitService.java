package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysDefectCommit;
import com.cat2bug.common.domain.SysDefectLog;
import com.cat2bug.common.domain.SysGitRepository;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.system.git.browse.domain.GitCommitDetail;
import com.cat2bug.system.git.browse.service.ISysGitBrowseService;
import com.cat2bug.system.mapper.SysDefectCommitMapper;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 缺陷修复关联提交：托管仓库校验提交存在且当前用户可拉取，外部仓库只校验 SHA 格式与链接。
 */
@ApplicationScoped
public class SysDefectCommitService {

    static final int MAX_COMMITS = 20;
    private static final int MAX_MESSAGE = 1000;
    private static final Pattern SHA = Pattern.compile("^[0-9a-fA-F]{7,64}$");

    @Inject
    SysDefectCommitMapper mapper;

    @Inject
    SysGitRepositoryMapper repositoryMapper;

    @Inject
    ISysGitBrowseService gitBrowseService;

    /** 校验并补全（托管提交解析为完整 SHA、说明与时间）；任何一条不合法都抛异常。 */
    public List<SysDefectCommit> verify(Long projectId, List<SysDefectCommit> commits) {
        if (commits == null || commits.isEmpty()) {
            return List.of();
        }
        if (commits.size() > MAX_COMMITS) {
            throw new ApiException(MessageUtils.message("defect.commit.too-many", String.valueOf(MAX_COMMITS)));
        }
        List<SysDefectCommit> out = new ArrayList<>();
        for (SysDefectCommit in : commits) {
            String sha = in == null || in.getCommitSha() == null ? "" : in.getCommitSha().trim();
            if (!SHA.matcher(sha).matches()) {
                throw new ApiException(MessageUtils.message("defect.commit.invalid-sha", sha));
            }
            SysDefectCommit row = new SysDefectCommit();
            if (in.getRepositoryId() != null) {
                SysGitRepository repo = repositoryMapper.selectSysGitRepositoryByRepositoryId(in.getRepositoryId());
                if (repo == null || !Objects.equals(repo.getProjectId(), projectId)) {
                    throw new ApiException(MessageUtils.message("defect.commit.repo-not-in-project"));
                }
                GitCommitDetail detail;
                try {
                    detail = gitBrowseService.getCommitDetail(repo.getRepositoryId(), sha);
                } catch (ApiException e) {
                    if (e.getHttpStatus() == 404 || e.getHttpStatus() == 400) {
                        throw new ApiException(MessageUtils.message("defect.commit.not-found", sha));
                    }
                    throw e;
                }
                row.setRepositoryId(repo.getRepositoryId());
                row.setCommitSha(detail.getId());
                row.setCommitMessage(truncate(firstLine(detail.getFullMessage())));
                row.setCommitTime(detail.getCommitTime());
            } else {
                String url = in.getCommitUrl() == null ? null : in.getCommitUrl().trim();
                if (url != null && !url.isEmpty() && !isHttpUrl(url)) {
                    throw new ApiException(MessageUtils.message("defect.commit.invalid-url"));
                }
                row.setCommitSha(sha.toLowerCase(Locale.ROOT));
                row.setCommitUrl(url == null || url.isEmpty() ? null : url);
                row.setCommitMessage(truncate(in.getCommitMessage()));
            }
            out.add(row);
        }
        return out;
    }

    public void saveForLog(Long defectId, Long defectLogId, List<SysDefectCommit> verified, String createBy) {
        Date now = new Date();
        for (SysDefectCommit row : verified) {
            row.setDefectId(defectId);
            row.setDefectLogId(defectLogId);
            row.setCreateBy(createBy);
            row.setCreateTime(now);
            mapper.insert(row);
        }
    }

    /** 把关联提交挂到对应的日志条目上。 */
    public void attachToLogs(List<SysDefectLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        List<Long> ids = new ArrayList<>();
        for (SysDefectLog log : logs) {
            if (log.getDefectLogId() != null) {
                ids.add(log.getDefectLogId());
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, List<SysDefectCommit>> byLog = new HashMap<>();
        for (SysDefectCommit c : mapper.selectByDefectLogIds(ids)) {
            byLog.computeIfAbsent(c.getDefectLogId(), k -> new ArrayList<>()).add(c);
        }
        for (SysDefectLog log : logs) {
            List<SysDefectCommit> list = byLog.get(log.getDefectLogId());
            if (list != null) {
                log.setCommits(list);
            }
        }
    }

    static boolean isHttpUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            return uri.getHost() != null && ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String firstLine(String message) {
        if (message == null) {
            return null;
        }
        int nl = message.indexOf('\n');
        return (nl >= 0 ? message.substring(0, nl) : message).trim();
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > MAX_MESSAGE ? s.substring(0, MAX_MESSAGE) : s;
    }
}
