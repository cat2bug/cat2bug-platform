package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.constant.UserConstants;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.SysPost;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.system.mapper.SysPostMapper;
import com.cat2bug.system.mapper.SysUserPostMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class SysPostService {

    @Inject
    SysPostMapper sysPostMapper;

    @Inject
    SysUserPostMapper sysUserPostMapper;

    public TableDataInfo selectPostPage(SysPost post, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysPostMapper.countPostList(post);
        List<SysPost> rows = sysPostMapper.selectPostList(post, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public List<SysPost> selectPostListAll(SysPost post) {
        return sysPostMapper.selectPostListAll(post);
    }

    public List<SysPost> selectPostAll() {
        return sysPostMapper.selectPostAll();
    }

    public SysPost selectPostById(Long postId) {
        return sysPostMapper.selectPostById(postId);
    }

    public List<Long> selectPostListByUserId(Long userId) {
        return sysPostMapper.selectPostListByUserId(userId);
    }

    public String selectUserPostGroup(String userName) {
        List<SysPost> posts = sysPostMapper.selectPostsByUserName(userName);
        if (posts == null || posts.isEmpty()) {
            return "";
        }
        return posts.stream()
                .map(SysPost::getPostName)
                .filter(name -> name != null && !name.isBlank())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    public boolean checkPostNameUnique(SysPost post) {
        Long postId = post.getPostId() == null ? -1L : post.getPostId();
        SysPost info = sysPostMapper.checkPostNameUnique(post.getPostName());
        return info == null || info.getPostId().longValue() == postId.longValue();
    }

    public boolean checkPostCodeUnique(SysPost post) {
        Long postId = post.getPostId() == null ? -1L : post.getPostId();
        SysPost info = sysPostMapper.checkPostCodeUnique(post.getPostCode());
        return info == null || info.getPostId().longValue() == postId.longValue();
    }

    @Transactional
    public int insertPost(SysPost post) {
        return sysPostMapper.insertPost(post);
    }

    @Transactional
    public int updatePost(SysPost post) {
        return sysPostMapper.updatePost(post);
    }

    @Transactional
    public int deletePostByIds(Long[] postIds) {
        for (Long postId : postIds) {
            SysPost post = selectPostById(postId);
            if (post == null) {
                continue;
            }
            if (sysUserPostMapper.countUserPostById(postId) > 0) {
                throw new ApiException(post.getPostName() + "已分配,不能删除");
            }
        }
        return sysPostMapper.deletePostByIds(postIds);
    }
}
