package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysComment;
import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysReport;
import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.mapper.SysCommentMapper;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysReportMapper;
import com.cat2bug.system.mapper.SysRoleMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Date;
import java.util.List;

@ApplicationScoped
public class SysCommentService {

    private static final String ANCHOR_KIND_LINE = "line";
    private static final String ANCHOR_KIND_SELECTION = "selection";

    private static final long TEAM_ADMIN_ROLE_ID = 4L;
    private static final long PROJECT_ADMIN_ROLE_ID = 6L;
    private static final long TEAM_CREATE_BY_ROLE_ID = 12L;

    @Inject
    SysCommentMapper sysCommentMapper;

    @Inject
    SysReportMapper sysReportMapper;

    @Inject
    SysProjectMapper sysProjectMapper;

    @Inject
    SysRoleMapper sysRoleMapper;

    @Inject
    AuthContext authContext;

    public SysComment selectSysCommentByCommentId(Long commentId) {
        return sysCommentMapper.selectSysCommentByCommentId(commentId);
    }

    public TableDataInfo selectCommentPage(SysComment query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysCommentMapper.countSysCommentList(query);
        List<SysComment> rows = sysCommentMapper.selectSysCommentList(query, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public int insertSysComment(SysComment comment) {
        LoginUser loginUser = requireLoginUser();
        validateAnchorKind(comment.getAnchorKind());
        validateParent(comment);
        comment.setCreateTime(new Date());
        comment.setCreateById(loginUser.getUserId());
        return sysCommentMapper.insertSysComment(comment);
    }

    public int updateSysComment(SysComment comment) {
        return sysCommentMapper.updateSysComment(comment);
    }

    @Transactional
    public int deleteSysCommentByCommentIds(Long[] commentIds) {
        int rows = 0;
        if (commentIds == null) {
            return rows;
        }
        for (Long commentId : commentIds) {
            rows += deleteSysCommentByCommentId(commentId);
        }
        return rows;
    }

    @Transactional
    public int deleteSysCommentByCommentId(Long commentId) {
        SysComment existing = sysCommentMapper.selectSysCommentByCommentId(commentId);
        if (existing == null) {
            return 0;
        }
        checkDeletePermission(existing);
        int cascaded = sysCommentMapper.deleteSysCommentByParentId(commentId);
        return cascaded + sysCommentMapper.deleteSysCommentByCommentId(commentId);
    }

    private void validateAnchorKind(String anchorKind) {
        if (anchorKind == null) {
            return;
        }
        if (!ANCHOR_KIND_LINE.equals(anchorKind) && !ANCHOR_KIND_SELECTION.equals(anchorKind)) {
            throw new ApiException("anchorKind 仅支持 line 或 selection");
        }
    }

    /** 回复只能挂在主评论下（不支持楼中楼），且模块 / 关联对象须与主评论一致。 */
    private void validateParent(SysComment comment) {
        Long parentId = comment.getParentId();
        if (parentId == null) {
            return;
        }
        SysComment parent = sysCommentMapper.selectSysCommentByCommentId(parentId);
        if (parent == null) {
            throw new ApiException("父评论不存在");
        }
        if (parent.getParentId() != null) {
            throw new ApiException("仅支持一级扁平回复，不能回复回复");
        }
        if (comment.getModuleType() != null && parent.getModuleType() != null
                && !comment.getModuleType().equals(parent.getModuleType())) {
            throw new ApiException("回复与主评论所属模块不一致");
        }
        if (comment.getCorrelationId() != null && parent.getCorrelationId() != null
                && !comment.getCorrelationId().equals(parent.getCorrelationId())) {
            throw new ApiException("回复与主评论关联对象不一致");
        }
    }

    /**
     * 创建人始终可删；moduleType=report 时另允许报告所属项目的项目管理员 / 团队管理员 / 团队创建者删除；
     * 其它模块不限制创建人，仅由接口权限控制。
     */
    private void checkDeletePermission(SysComment comment) {
        LoginUser loginUser = authContext.getLoginUser();
        Long currentUserId = loginUser == null ? null : loginUser.getUserId();
        if (currentUserId != null && currentUserId.equals(comment.getCreateById())) {
            return;
        }
        if (!SysReport.KEY.equals(comment.getModuleType())) {
            return;
        }
        if (!isReportProjectOrTeamAdmin(comment.getCorrelationId(), currentUserId)) {
            throw new ApiException(MessageUtils.message("no-permission"));
        }
    }

    private boolean isReportProjectOrTeamAdmin(Long reportId, Long userId) {
        if (reportId == null || userId == null) {
            return false;
        }
        SysReport report = sysReportMapper.selectSysReportByReportId(reportId);
        if (report == null || report.getProjectId() == null) {
            return false;
        }
        SysProject project = sysProjectMapper.selectSysProjectByProjectId(report.getProjectId(), null);
        if (project == null) {
            return false;
        }
        List<SysRole> roles = sysRoleMapper.selectRolePermissionByUserId(
                project.getTeamId(), project.getProjectId(), userId);
        if (roles == null) {
            return false;
        }
        return roles.stream().anyMatch(role -> {
            Long roleId = role.getRoleId();
            return roleId != null && (roleId == PROJECT_ADMIN_ROLE_ID
                    || roleId == TEAM_ADMIN_ROLE_ID
                    || roleId == TEAM_CREATE_BY_ROLE_ID);
        });
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
