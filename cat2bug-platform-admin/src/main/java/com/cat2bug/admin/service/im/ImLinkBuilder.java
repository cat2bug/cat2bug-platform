package com.cat2bug.admin.service.im;

import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.mapper.SysProjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * 通知里的前端链接。前端是 History 路由，项目内页面为 {@code /{teamKey}/{projectKey}/{module}}；
 * 取不到路径键时退回 {@code /project/{module}}，由前端旧路径重定向处理。
 */
@ApplicationScoped
public class ImLinkBuilder {

    @Inject
    SysProjectMapper sysProjectMapper;

    public String defect(String srcHost, Long projectId, Long defectId) {
        if (StringUtils.isEmpty(srcHost) || defectId == null) {
            return "";
        }
        if (projectId == null) {
            return String.format("%s/project/defect?defectId=%d", trimHost(srcHost), defectId);
        }
        return String.format("%s/%s/defect?projectId=%d&defectId=%d",
                trimHost(srcHost), projectPath(projectId), projectId, defectId);
    }

    public String report(String srcHost, Long projectId, Long reportId) {
        if (StringUtils.isEmpty(srcHost) || reportId == null || projectId == null) {
            return "";
        }
        return String.format("%s/%s/report?projectId=%d&reportId=%d",
                trimHost(srcHost), projectPath(projectId), projectId, reportId);
    }

    public String notice(String srcHost, Long projectId) {
        if (StringUtils.isEmpty(srcHost)) {
            return "";
        }
        return projectId == null
                ? trimHost(srcHost) + "/notice/index"
                : String.format("%s/notice/index?projectId=%d", trimHost(srcHost), projectId);
    }

    private String projectPath(Long projectId) {
        try {
            SysProject project = sysProjectMapper.selectSysProjectByProjectId(projectId, null);
            if (project != null && StringUtils.isNotEmpty(project.getTeamKey())
                    && StringUtils.isNotEmpty(project.getProjectKey())) {
                return project.getTeamKey() + "/" + project.getProjectKey();
            }
        } catch (RuntimeException ignored) {
        }
        return "project";
    }

    static String trimHost(String srcHost) {
        String host = srcHost.trim();
        while (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        return host;
    }
}
