package com.cat2bug.system.mapper;

import java.util.List;
import com.cat2bug.system.domain.SysComment;

/**
 * 评论Mapper接口
 * 
 * @author yuzhantao
 * @date 2024-02-29
 */
public interface SysCommentMapper 
{
    /**
     * 查询评论
     * 
     * @param commentId 评论主键
     * @return 评论
     */
    public SysComment selectSysCommentByCommentId(Long commentId);

    /**
     * 查询评论列表
     * 
     * @param sysComment 评论
     * @return 评论集合
     */
    public List<SysComment> selectSysCommentList(SysComment sysComment);

    /**
     * 新增评论
     * 
     * @param sysComment 评论
     * @return 结果
     */
    public int insertSysComment(SysComment sysComment);

    /**
     * 修改评论
     * 
     * @param sysComment 评论
     * @return 结果
     */
    public int updateSysComment(SysComment sysComment);

    /**
     * 删除评论
     * 
     * @param commentId 评论主键
     * @return 结果
     */
    public int deleteSysCommentByCommentId(Long commentId);

    /**
     * 批量删除评论
     * 
     * @param commentIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysCommentByCommentIds(Long[] commentIds);
}
