package com.cat2bug.system.service.impl;

import java.util.List;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysCommentMapper;
import com.cat2bug.system.domain.SysComment;
import com.cat2bug.system.service.ISysCommentService;

/**
 * 评论Service业务层处理
 * 
 * @author yuzhantao
 * @date 2024-02-29
 */
@Service
public class SysCommentServiceImpl implements ISysCommentService 
{
    @Autowired
    private SysCommentMapper sysCommentMapper;

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
     * 批量删除评论
     * 
     * @param commentIds 需要删除的评论主键
     * @return 结果
     */
    @Override
    public int deleteSysCommentByCommentIds(Long[] commentIds)
    {
        return sysCommentMapper.deleteSysCommentByCommentIds(commentIds);
    }

    /**
     * 删除评论信息
     * 
     * @param commentId 评论主键
     * @return 结果
     */
    @Override
    public int deleteSysCommentByCommentId(Long commentId)
    {
        return sysCommentMapper.deleteSysCommentByCommentId(commentId);
    }
}
