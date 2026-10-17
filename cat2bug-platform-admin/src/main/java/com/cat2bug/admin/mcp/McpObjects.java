package com.cat2bug.admin.mcp;

import com.cat2bug.common.domain.SysCase;
import com.cat2bug.common.domain.SysDefect;
import com.cat2bug.common.domain.SysDefectLog;
import com.cat2bug.common.domain.SysDocument;
import com.cat2bug.common.domain.SysGitRepository;
import com.cat2bug.common.domain.SysModule;
import com.cat2bug.common.domain.SysPlan;
import com.cat2bug.common.domain.SysPlanItem;
import com.cat2bug.system.mapper.SysCaseMapper;
import com.cat2bug.system.mapper.SysDefectLogMapper;
import com.cat2bug.system.mapper.SysDefectMapper;
import com.cat2bug.system.mapper.SysDocumentMapper;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysModuleMapper;
import com.cat2bug.system.mapper.SysPlanItemMapper;
import com.cat2bug.system.mapper.SysPlanMapper;
import io.quarkiverse.mcp.server.ToolCallException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * 按 ID 取对象并确认它属于本次调用的目标项目。网页接口按 ID 访问时不校验归属，
 * MCP 工具在转发前必须先过这一层，避免借 A 项目的授权读写 B 项目的数据。
 */
@ApplicationScoped
public class McpObjects {

    @Inject
    SysDefectMapper defectMapper;

    @Inject
    SysDefectLogMapper defectLogMapper;

    @Inject
    SysCaseMapper caseMapper;

    @Inject
    SysPlanMapper planMapper;

    @Inject
    SysPlanItemMapper planItemMapper;

    @Inject
    SysModuleMapper moduleMapper;

    @Inject
    SysDocumentMapper documentMapper;

    @Inject
    SysGitRepositoryMapper repositoryMapper;

    public SysDefect defect(McpScope scope, Long defectId) {
        SysDefect defect = defectId == null ? null : defectMapper.selectSysDefectByDefectId(defectId);
        return owned(scope, defect, defect == null ? null : defect.getProjectId(), "缺陷", defectId);
    }

    /** 按项目内编号（列表里显示的 #号）取缺陷。 */
    public SysDefect defectByNum(McpScope scope, Long projectNum) {
        SysDefect defect = projectNum == null ? null
                : defectMapper.selectByProjectIdAndProjectNum(scope.projectId(), projectNum);
        if (defect == null) {
            throw new ToolCallException("缺陷不存在：#" + projectNum);
        }
        return defect;
    }

    public SysDefectLog defectLog(McpScope scope, Long defectLogId) {
        SysDefectLog log = defectLogId == null ? null : defectLogMapper.selectSysDefectLogByDefectLogId(defectLogId);
        if (log == null) {
            throw new ToolCallException("缺陷日志不存在：" + defectLogId);
        }
        defect(scope, log.getDefectId());
        return log;
    }

    public SysCase testCase(McpScope scope, Long caseId) {
        SysCase c = caseId == null ? null : caseMapper.selectSysCaseByCaseId(caseId);
        return owned(scope, c, c == null ? null : c.getProjectId(), "用例", caseId);
    }

    public SysPlan plan(McpScope scope, String planId) {
        SysPlan plan = planId == null || planId.isBlank() ? null : planMapper.selectSysPlanByPlanId(planId);
        return owned(scope, plan, plan == null ? null : plan.getProjectId(), "测试计划", planId);
    }

    public SysPlanItem planItem(McpScope scope, String planItemId) {
        SysPlanItem item = planItemId == null || planItemId.isBlank()
                ? null : planItemMapper.selectSysPlanItemByPlanItemId(planItemId);
        if (item == null) {
            throw new ToolCallException("测试计划子项不存在：" + planItemId);
        }
        plan(scope, item.getPlanId());
        return item;
    }

    public SysModule module(McpScope scope, Long moduleId) {
        SysModule m = moduleId == null ? null : moduleMapper.selectSysModuleByModuleId(moduleId);
        return owned(scope, m, m == null ? null : m.getProjectId(), "交付物", moduleId);
    }

    public SysDocument document(McpScope scope, Long docId) {
        SysDocument d = docId == null ? null : documentMapper.selectSysDocumentByDocId(docId);
        return owned(scope, d, d == null ? null : d.getProjectId(), "文档", docId);
    }

    public SysGitRepository repository(McpScope scope, Long repositoryId) {
        SysGitRepository r = repositoryId == null ? null : repositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
        return owned(scope, r, r == null ? null : r.getProjectId(), "代码仓库", repositoryId);
    }

    private static <T> T owned(McpScope scope, T obj, Long projectId, String kind, Object id) {
        if (obj == null || !Objects.equals(projectId, scope.projectId())) {
            throw new ToolCallException(kind + "不存在或不属于该项目：" + id);
        }
        return obj;
    }
}
