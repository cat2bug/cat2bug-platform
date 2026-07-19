package com.cat2bug.system.service.impl;

import java.util.List;
import com.cat2bug.common.core.domain.entity.SysReport;
import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cat2bug.system.domain.SysProject;
import com.cat2bug.system.mapper.SysCommentMapper;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysReportMapper;
import com.cat2bug.system.mapper.SysRoleMapper;
import com.cat2bug.system.domain.SysComment;
import com.cat2bug.system.service.ISysCommentService;

/**
 * 评论Service业务层处理
 * 
 * @author yuzhantao
 * @date 2024-02-29
 */
@Service
@SuppressWarnings("rawtypes")
public class SysCommentServiceImpl implements ISysCommentService 
{
    private static final String ANCHOR_KIND_LINE = "line";
    private static final String ANCHOR_KIND_SELECTION = "selection";

    @Autowired
    private SysCommentMapper sysCommentMapper;

    @Autowired
    private SysReportMapper sysReportMapper;

    @Autowired
    private SysProjectMapper sysProjectMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    /**
     * 查询评论
     * 
     * @param commentId 评论主键
     * @return 评论
     */
    @Override
    public SysComment selectSysCommentByCommentId(Long commentId)
    {
        return sysCommentMapper.selectSysCommentByCommentId(commentId);
    }

    /**
     * 查询评论列表
     * 
     * @param sysComment 评论
     * @return 评论
     */
    @Override
    public List<SysComment> selectSysCommentList(SysComment sysComment)
    {
        return sysCommentMapper.selectSysCommentList(sysComment);
    }

    /**
     * 新增评论
     * 
     * @param sysComment 评论
     * @return 结果
     */
    @Override
    public int insertSysComment(SysComment sysComment)
    {
        validateAnchorKind(sysComment.getAnchorKind());
        validateParent(sysComment);
        sysComment.setCreateTime(DateUtils.getNowDate());
        sysComment.setCreateById(SecurityUtils.getUserId());
        return sysCommentMapper.insertSysComment(sysComment);
    }

    /**
     * 修改评论
     * 
     * @param sysComment 评论
     * @return 结果
     */
    @Override
    public int updateSysComment(SysComment sysComment)
    {
        return sysCommentMapper.updateSysComment(sysComment);
    }

    /**
     * 批量删除评论：逐条校验删除权限，主评论删除时级联删除其扁平回复
     * 
     * @param commentIds 需要删除的评论主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteSysCommentByCommentIds(Long[] commentIds)
    {
        int rows = 0;
        if (commentIds == null)
        {
            return rows;
        }
        for (Long commentId : commentIds)
        {
            rows += deleteSysCommentByCommentId(commentId);
        }
        return rows;
    }

    /**
     * 删除评论信息：校验删除权限，若为主评论则级联删除其扁平回复
     * 
     * @param commentId 评论主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteSysCommentByCommentId(Long commentId)
    {
        SysComment existing = sysCommentMapper.selectSysCommentByCommentId(commentId);
        if (existing == null)
        {
            return 0;
        }
        checkDeletePermission(existing);
        // 主评论删除时级联删除其扁平回复，避免遗留孤儿回复；回复本身没有下级，此调用无副作用
        int cascaded = sysCommentMapper.deleteSysCommentByParentId(commentId);
        return cascaded + sysCommentMapper.deleteSysCommentByCommentId(commentId);
    }

    /**
     * 校验 anchorKind 仅允许 line / selection（未提供则跳过，兼容 defect_log 等不使用锚点的评论）
     */
    private void validateAnchorKind(String anchorKind)
    {
        if (anchorKind == null)
        {
            return;
        }
        if (!ANCHOR_KIND_LINE.equals(anchorKind) && !ANCHOR_KIND_SELECTION.equals(anchorKind))
        {
            throw new ServiceException("anchorKind 仅支持 line 或 selection");
        }
    }

    /**
     * 校验回复的父评论：必须存在、必须是主评论（禁止楼中楼），且模块 / 关联对象需与父评论一致
     */
    private void validateParent(SysComment sysComment)
    {
        Long parentId = sysComment.getParentId();
        if (parentId == null)
        {
            return;
        }
        SysComment parent = sysCommentMapper.selectSysCommentByCommentId(parentId);
        if (parent == null)
        {
            throw new ServiceException("父评论不存在");
        }
        if (parent.getParentId() != null)
        {
            throw new ServiceException("仅支持一级扁平回复，不能回复回复");
        }
        if (sysComment.getModuleType() != null && parent.getModuleType() != null
                && !sysComment.getModuleType().equals(parent.getModuleType()))
        {
            throw new ServiceException("回复与主评论所属模块不一致");
        }
        if (sysComment.getCorrelationId() != null && parent.getCorrelationId() != null
                && !sysComment.getCorrelationId().equals(parent.getCorrelationId()))
        {
            throw new ServiceException("回复与主评论关联对象不一致");
        }
    }

    /**
     * 删除鉴权：评论创建人本人始终可删；
     * 报告批注（moduleType=report）额外允许该报告所属项目的项目管理员 / 所属团队的团队管理员删除任意评论或回复；
     * 其它模块（如 defect_log）保持既有权限模型，不做创建人限制，由 Controller 侧 {@code @PreAuthorize} 控制。
     */
    private void checkDeletePermission(SysComment comment)
    {
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId != null && currentUserId.equals(comment.getCreateById()))
        {
            return;
        }
        if (!SysReport.KEY.equals(comment.getModuleType()))
        {
            return;
        }
        if (!isReportProjectOrTeamAdmin(comment.getCorrelationId(), currentUserId))
        {
            throw new ServiceException(MessageUtils.message("no-permission"));
        }
    }

    /**
     * 判断当前用户是否为该报告所属项目的项目管理员，或所属团队的团队管理员
     */
    private boolean isReportProjectOrTeamAdmin(Long reportId, Long userId)
    {
        if (reportId == null || userId == null)
        {
            return false;
        }
        SysReport report = sysReportMapper.selectSysReportByReportId(reportId);
        if (report == null || report.getProjectId() == null)
        {
            return false;
        }
        SysProject project = sysProjectMapper.selectSysProjectByProjectId(report.getProjectId(), null);
        if (project == null)
        {
            return false;
        }
        List<SysRole> roles = sysRoleMapper.selectRolePermissionByUserId(project.getTeamId(), project.getProjectId(), userId);
        if (roles == null)
        {
            return false;
        }
        return roles.stream().anyMatch(role ->
            role.isProjectAdmin() || role.isTeamAdmin() || role.isTeamCreateBy());
    }
}
