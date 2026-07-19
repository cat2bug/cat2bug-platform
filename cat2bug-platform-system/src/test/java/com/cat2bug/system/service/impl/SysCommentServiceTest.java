package com.cat2bug.system.service.impl;

import com.cat2bug.common.core.domain.entity.SysReport;
import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.domain.SysComment;
import com.cat2bug.system.domain.SysProject;
import com.cat2bug.system.mapper.SysCommentMapper;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysReportMapper;
import com.cat2bug.system.mapper.SysRoleMapper;
import com.cat2bug.system.support.MessageSourceTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 报告批注扩展后的评论 Service：新增字段落库、扁平回复校验、删除鉴权与级联删除。
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SysCommentServiceTest {

    private static final Long REPORT_ID = 100L;
    private static final Long PROJECT_ID = 10L;
    private static final Long TEAM_ID = 1L;

    @Mock
    private SysCommentMapper sysCommentMapper;
    @Mock
    private SysReportMapper sysReportMapper;
    @Mock
    private SysProjectMapper sysProjectMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;

    @InjectMocks
    private SysCommentServiceImpl sysCommentService;

    private Runnable messageCleanup;

    @Before
    public void setUp() {
        messageCleanup = MessageSourceTestSupport.installMessageSource();
    }

    @After
    public void tearDown() {
        if (messageCleanup != null) {
            messageCleanup.run();
        }
        SecurityContextHolder.clearContext();
    }

    @Test
    public void insertSysComment_rootReportAnnotation_persistsAnchorFieldsAndSetsCreator() {
        loginAs(5L);
        SysComment comment = reportComment(null, "整行批注", "line");
        comment.setQuote("原文片段");
        comment.setContextBefore("前文");
        comment.setContextAfter("后文");
        comment.setAnchorHint("h2-背景");
        when(sysCommentMapper.insertSysComment(any())).thenReturn(1);

        int rows = sysCommentService.insertSysComment(comment);

        assertEquals(1, rows);
        assertEquals(Long.valueOf(5L), comment.getCreateById());
        assertNotNull(comment.getCreateTime());
        verify(sysCommentMapper).insertSysComment(comment);
    }

    @Test
    public void insertSysComment_flatReply_setsParentAndPersists() {
        loginAs(6L);
        SysComment rootComment = reportComment(null, "主评论", "selection");
        rootComment.setCommentId(1L);
        when(sysCommentMapper.selectSysCommentByCommentId(1L)).thenReturn(rootComment);
        when(sysCommentMapper.insertSysComment(any())).thenReturn(1);

        SysComment reply = reportComment(1L, "回复内容", null);

        int rows = sysCommentService.insertSysComment(reply);

        assertEquals(1, rows);
        assertEquals(Long.valueOf(6L), reply.getCreateById());
        verify(sysCommentMapper).insertSysComment(reply);
    }

    @Test
    public void insertSysComment_replyToReply_rejected() {
        loginAs(6L);
        SysComment rootComment = reportComment(null, "主评论", "line");
        rootComment.setCommentId(1L);
        SysComment firstReply = reportComment(1L, "第一条回复", null);
        firstReply.setCommentId(2L);
        when(sysCommentMapper.selectSysCommentByCommentId(2L)).thenReturn(firstReply);

        SysComment nestedReply = reportComment(2L, "楼中楼", null);
        try {
            sysCommentService.insertSysComment(nestedReply);
            fail("expected ServiceException for nested reply");
        } catch (ServiceException expected) {
            // 仅支持一级扁平回复
        }
        verify(sysCommentMapper, never()).insertSysComment(any());
    }

    @Test
    public void insertSysComment_parentNotFound_rejected() {
        loginAs(6L);
        when(sysCommentMapper.selectSysCommentByCommentId(99L)).thenReturn(null);
        SysComment reply = reportComment(99L, "回复内容", null);
        try {
            sysCommentService.insertSysComment(reply);
            fail("expected ServiceException for missing parent");
        } catch (ServiceException expected) {
            // 父评论不存在
        }
        verify(sysCommentMapper, never()).insertSysComment(any());
    }

    @Test
    public void insertSysComment_invalidAnchorKind_rejected() {
        loginAs(6L);
        SysComment comment = reportComment(null, "内容", "paragraph");
        try {
            sysCommentService.insertSysComment(comment);
            fail("expected ServiceException for invalid anchorKind");
        } catch (ServiceException expected) {
            // anchorKind 仅支持 line/selection
        }
        verify(sysCommentMapper, never()).insertSysComment(any());
    }

    @Test
    public void insertSysComment_staleContextStillPersisted_noAutoRematchDeletion() {
        // 正文可能已变更，quote/context 只是快照；后端不做自动重匹配或拒绝写入
        loginAs(7L);
        SysComment comment = reportComment(null, "针对旧原文的批注", "selection");
        comment.setQuote("已被修改前的原文");
        comment.setContextBefore("旧上文");
        comment.setContextAfter("旧下文");
        when(sysCommentMapper.insertSysComment(any())).thenReturn(1);

        int rows = sysCommentService.insertSysComment(comment);

        assertEquals(1, rows);
        verify(sysCommentMapper).insertSysComment(comment);
    }

    @Test
    public void deleteSysCommentByCommentId_creatorCanDeleteOwnRootComment_cascadesReplies() {
        loginAs(5L);
        SysComment root = reportComment(null, "主评论", "line");
        root.setCommentId(1L);
        root.setCreateById(5L);
        when(sysCommentMapper.selectSysCommentByCommentId(1L)).thenReturn(root);
        when(sysCommentMapper.deleteSysCommentByParentId(1L)).thenReturn(2);
        when(sysCommentMapper.deleteSysCommentByCommentId(1L)).thenReturn(1);

        int rows = sysCommentService.deleteSysCommentByCommentId(1L);

        assertEquals(3, rows);
        verify(sysCommentMapper).deleteSysCommentByParentId(1L);
        verify(sysCommentMapper).deleteSysCommentByCommentId(1L);
    }

    @Test
    public void deleteSysCommentByCommentId_projectAdmin_canDeleteOthersReportComment() {
        loginAs(9L);
        SysComment root = reportComment(null, "别人发的批注", "line");
        root.setCommentId(1L);
        root.setCreateById(5L);
        when(sysCommentMapper.selectSysCommentByCommentId(1L)).thenReturn(root);
        when(sysReportMapper.selectSysReportByReportId(REPORT_ID)).thenReturn(reportWithProject());
        when(sysProjectMapper.selectSysProjectByProjectId(PROJECT_ID, null)).thenReturn(project());
        when(sysRoleMapper.selectRolePermissionByUserId(TEAM_ID, PROJECT_ID, 9L))
                .thenReturn(Collections.singletonList(roleWithId(6L)));
        when(sysCommentMapper.deleteSysCommentByCommentId(1L)).thenReturn(1);

        int rows = sysCommentService.deleteSysCommentByCommentId(1L);

        assertEquals(1, rows);
        verify(sysCommentMapper).deleteSysCommentByCommentId(1L);
    }

    @Test
    public void deleteSysCommentByCommentId_teamAdmin_canDeleteOthersReportComment() {
        loginAs(9L);
        SysComment root = reportComment(null, "别人发的批注", "line");
        root.setCommentId(1L);
        root.setCreateById(5L);
        when(sysCommentMapper.selectSysCommentByCommentId(1L)).thenReturn(root);
        when(sysReportMapper.selectSysReportByReportId(REPORT_ID)).thenReturn(reportWithProject());
        when(sysProjectMapper.selectSysProjectByProjectId(PROJECT_ID, null)).thenReturn(project());
        when(sysRoleMapper.selectRolePermissionByUserId(TEAM_ID, PROJECT_ID, 9L))
                .thenReturn(Collections.singletonList(roleWithId(12L)));
        when(sysCommentMapper.deleteSysCommentByCommentId(1L)).thenReturn(1);

        int rows = sysCommentService.deleteSysCommentByCommentId(1L);

        assertEquals(1, rows);
        verify(sysCommentMapper).deleteSysCommentByCommentId(1L);
    }

    @Test
    public void deleteSysCommentByCommentId_unauthorizedMember_rejected() {
        loginAs(9L);
        SysComment root = reportComment(null, "别人发的批注", "line");
        root.setCommentId(1L);
        root.setCreateById(5L);
        when(sysCommentMapper.selectSysCommentByCommentId(1L)).thenReturn(root);
        when(sysReportMapper.selectSysReportByReportId(REPORT_ID)).thenReturn(reportWithProject());
        when(sysProjectMapper.selectSysProjectByProjectId(PROJECT_ID, null)).thenReturn(project());
        when(sysRoleMapper.selectRolePermissionByUserId(TEAM_ID, PROJECT_ID, 9L))
                .thenReturn(Collections.singletonList(roleWithId(2L)));

        try {
            sysCommentService.deleteSysCommentByCommentId(1L);
            fail("expected ServiceException for unauthorized delete");
        } catch (ServiceException expected) {
            // 非创建人且非项目/团队管理员
        }
        verify(sysCommentMapper, never()).deleteSysCommentByCommentId(any());
        verify(sysCommentMapper, never()).deleteSysCommentByParentId(any());
    }

    @Test
    public void deleteSysCommentByCommentId_replyAuthorCanDeleteOwnReply() {
        loginAs(6L);
        SysComment reply = reportComment(1L, "我的回复", null);
        reply.setCommentId(2L);
        reply.setCreateById(6L);
        when(sysCommentMapper.selectSysCommentByCommentId(2L)).thenReturn(reply);
        when(sysCommentMapper.deleteSysCommentByCommentId(2L)).thenReturn(1);

        int rows = sysCommentService.deleteSysCommentByCommentId(2L);

        assertEquals(1, rows);
        verify(sysReportMapper, never()).selectSysReportByReportId(any());
    }

    @Test
    public void deleteSysCommentByCommentId_defectLogComment_keepsLegacyBehavior_noCreatorRestriction() {
        // defect_log 路径保持兼容：不做创建人限制，交由 Controller 侧权限校验
        loginAs(9L);
        SysComment defectComment = new SysComment();
        defectComment.setCommentId(3L);
        defectComment.setModuleType("defect_log");
        defectComment.setCorrelationId(1L);
        defectComment.setCreateById(5L);
        when(sysCommentMapper.selectSysCommentByCommentId(3L)).thenReturn(defectComment);
        when(sysCommentMapper.deleteSysCommentByCommentId(3L)).thenReturn(1);

        int rows = sysCommentService.deleteSysCommentByCommentId(3L);

        assertEquals(1, rows);
        verify(sysReportMapper, never()).selectSysReportByReportId(any());
        verify(sysProjectMapper, never()).selectSysProjectByProjectId(any(), any());
    }

    @Test
    public void deleteSysCommentByCommentIds_batchSumsAffectedRows_skipsMissing() {
        loginAs(5L);
        SysComment first = reportComment(null, "第一条", "line");
        first.setCommentId(1L);
        first.setCreateById(5L);
        when(sysCommentMapper.selectSysCommentByCommentId(1L)).thenReturn(first);
        when(sysCommentMapper.deleteSysCommentByParentId(1L)).thenReturn(0);
        when(sysCommentMapper.deleteSysCommentByCommentId(1L)).thenReturn(1);
        when(sysCommentMapper.selectSysCommentByCommentId(404L)).thenReturn(null);

        int rows = sysCommentService.deleteSysCommentByCommentIds(new Long[]{1L, 404L});

        assertEquals(1, rows);
        verify(sysCommentMapper, times(1)).deleteSysCommentByCommentId(1L);
        verify(sysCommentMapper, never()).deleteSysCommentByCommentId(404L);
    }

    private void loginAs(Long userId) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName("user" + userId);
        LoginUser loginUser = new LoginUser(userId, null, user, Collections.emptySet());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private SysComment reportComment(Long parentId, String content, String anchorKind) {
        SysComment comment = new SysComment();
        comment.setModuleType(SysReport.KEY);
        comment.setCorrelationId(REPORT_ID);
        comment.setCommentContent(content);
        comment.setParentId(parentId);
        comment.setAnchorKind(anchorKind);
        return comment;
    }

    private SysReport reportWithProject() {
        SysReport report = new SysReport();
        report.setReportId(REPORT_ID);
        report.setProjectId(PROJECT_ID);
        return report;
    }

    private SysProject project() {
        SysProject project = new SysProject();
        project.setProjectId(PROJECT_ID);
        project.setTeamId(TEAM_ID);
        return project;
    }

    private SysRole roleWithId(Long roleId) {
        return new SysRole(roleId);
    }
}
