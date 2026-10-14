package com.cat2bug.system.service.mcp;

import com.cat2bug.common.domain.SysProjectMcp;
import com.cat2bug.common.security.McpModules;
import com.cat2bug.system.mapper.SysProjectMcpMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collection;
import java.util.Date;

/**
 * 项目 MCP 设置。没有记录 = 未开启（老项目升级后默认关闭）；新建项目写入「开启 + 当时全部模块」。
 */
@ApplicationScoped
public class SysProjectMcpService {

    @Inject
    SysProjectMcpMapper mapper;

    @Inject
    McpAccessService accessService;

    public SysProjectMcp get(Long projectId) {
        SysProjectMcp row = projectId == null ? null : mapper.selectByProjectId(projectId);
        if (row == null) {
            row = new SysProjectMcp();
            row.setProjectId(projectId);
            row.setEnabled(false);
        } else {
            row.setModules(McpModules.normalize(row.getModules()));
        }
        return row;
    }

    public SysProjectMcp save(Long projectId, boolean enabled, Collection<String> modules, String updateBy) {
        SysProjectMcp row = new SysProjectMcp();
        row.setProjectId(projectId);
        row.setEnabled(enabled);
        row.setModules(McpModules.normalize(modules));
        row.setUpdateBy(updateBy);
        row.setUpdateTime(new Date());
        if (mapper.update(row) == 0) {
            mapper.insert(row);
        }
        accessService.invalidateProject(projectId);
        return get(projectId);
    }

    public void createDefault(Long projectId, String createBy) {
        save(projectId, true, McpModules.allKeys(), createBy);
    }

    public void deleteByProjectId(Long projectId) {
        mapper.deleteByProjectId(projectId);
        accessService.invalidateProject(projectId);
    }
}
