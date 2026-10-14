package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.constant.UserConstants;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.SysPost;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserPost;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.SecurityUtils;
import com.cat2bug.system.mapper.SysPostMapper;
import com.cat2bug.system.mapper.SysUserMapper;
import com.cat2bug.system.mapper.SysUserPostMapper;
import com.cat2bug.system.security.DataScopeSqlBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class SysUserService {

    private static final Long DEFAULT_ROLE_ID = 10L;

    @Inject
    SysUserMapper sysUserMapper;

    @Inject
    SysUserPostMapper sysUserPostMapper;

    @Inject
    SysPostMapper sysPostMapper;

    @Inject
    DataScopeSqlBuilder dataScopeSqlBuilder;

    public TableDataInfo selectUserPage(SysUser user, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        String dataScope = userDataScope();
        long total = sysUserMapper.countUserList(user, dataScope);
        var rows = sysUserMapper.selectUserList(user, dataScope, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public TableDataInfo selectAllocatedPage(SysUser user, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        String dataScope = userDataScope();
        long total = sysUserMapper.countAllocatedList(user, dataScope);
        var rows = sysUserMapper.selectAllocatedList(user, dataScope, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public TableDataInfo selectUnallocatedPage(SysUser user, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        String dataScope = userDataScope();
        long total = sysUserMapper.countUnallocatedList(user, dataScope);
        var rows = sysUserMapper.selectUnallocatedList(user, dataScope, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public SysUser selectUserById(Long userId) {
        return sysUserMapper.selectUserById(userId);
    }

    public SysUser selectUserByUserName(String userName) {
        return sysUserMapper.selectUserByUserName(userName);
    }

    public List<SysUser> selectUserListAll(SysUser user) {
        String dataScope = userDataScope();
        long total = sysUserMapper.countUserList(user, dataScope);
        if (total <= 0) {
            return Collections.emptyList();
        }
        int limit = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        return sysUserMapper.selectUserList(user, dataScope, 0, limit);
    }

    public boolean checkUserNameUnique(SysUser user) {
        SysUser info = sysUserMapper.checkUserNameUnique(user.getUserName());
        Long userId = user.getUserId() == null ? -1L : user.getUserId();
        if (info != null && info.getUserId().longValue() != userId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    public boolean checkPhoneUnique(SysUser user) {
        SysUser info = sysUserMapper.checkPhoneUnique(user.getPhoneNumber());
        Long userId = user.getUserId() == null ? -1L : user.getUserId();
        if (info != null && info.getUserId().longValue() != userId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    public boolean checkEmailUnique(SysUser user) {
        SysUser info = sysUserMapper.checkEmailUnique(user.getEmail());
        Long userId = user.getUserId() == null ? -1L : user.getUserId();
        if (info != null && info.getUserId().longValue() != userId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    public void checkUserAllowed(SysUser user) {
        if (user != null && user.getUserId() != null && user.isAdmin()) {
            throw new ApiException("不允许操作超级管理员用户");
        }
    }

    public void checkUserDataScope(Long userId, Long currentUserId) {
        if (SysUser.isAdmin(currentUserId)) {
            return;
        }
        checkUserDataScope(userId);
    }

    /** 按当前登录用户的数据范围校验目标用户可见；超级管理员不受限 */
    public void checkUserDataScope(Long userId) {
        if (userId == null || SecurityUtils.isAdmin()) {
            return;
        }
        SysUser query = new SysUser();
        query.setUserId(userId);
        if (sysUserMapper.countUserList(query, userDataScope()) == 0) {
            throw new ApiException("没有权限访问用户数据！");
        }
    }

    private String userDataScope() {
        return dataScopeSqlBuilder.forCurrentUser("d", "u");
    }

    @Transactional
    public int insertUser(SysUser user) {
        applyDefaultUserType(user);
        int rows = sysUserMapper.insertUser(user);
        insertUserRole(user);
        insertUserPost(user);
        return rows;
    }

    @Transactional
    public boolean registerUser(SysUser user) {
        applyDefaultUserType(user);
        int rows = sysUserMapper.insertUser(user);
        if (rows > 0) {
            sysUserMapper.insertUserRole(user.getUserId(), DEFAULT_ROLE_ID);
        }
        return rows > 0;
    }

    @Transactional
    public int updateUser(SysUser user) {
        Long userId = user.getUserId();
        sysUserMapper.deleteUserRoleByUserId(userId);
        insertUserRole(user);
        sysUserPostMapper.deleteUserPostByUserId(userId);
        insertUserPost(user);
        return sysUserMapper.updateUser(user);
    }

    @Transactional
    public int updateUserStatus(SysUser user) {
        return sysUserMapper.updateUser(user);
    }

    @Transactional
    public int resetPwd(SysUser user) {
        return sysUserMapper.updateUser(user);
    }

    @Transactional
    public void insertUserAuth(Long userId, Long[] roleIds) {
        sysUserMapper.deleteUserRoleByUserId(userId);
        insertUserRole(userId, roleIds);
    }

    @Transactional
    public int deleteUserByIds(Long[] userIds) {
        for (Long userId : userIds) {
            SysUser u = new SysUser();
            u.setUserId(userId);
            checkUserAllowed(u);
            checkUserDataScope(userId);
        }
        sysUserMapper.deleteUserRoleByUserIds(userIds);
        sysUserPostMapper.deleteUserPost(userIds);
        return sysUserMapper.deleteUserByIds(userIds);
    }

    private void insertUserRole(SysUser user) {
        insertUserRole(user.getUserId(), user.getRoleIds());
    }

    private void insertUserRole(Long userId, Long[] roleIds) {
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            if (roleId != null) {
                sysUserMapper.insertUserRole(userId, roleId);
            }
        }
    }

    @Transactional
    public int updateUserProfile(SysUser user) {
        return sysUserMapper.updateUser(user);
    }

    @Transactional
    public boolean updateUserAvatar(String userName, String avatar) {
        return sysUserMapper.updateUserAvatar(userName, avatar) > 0;
    }

    @Transactional
    public int resetUserPwd(String userName, String password) {
        return sysUserMapper.resetUserPwd(userName, password);
    }

    public String selectUserRoleGroup(SysUser user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return "";
        }
        return user.getRoles().stream()
                .map(role -> role.getRoleName())
                .filter(StringUtils::isNotEmpty)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    public String selectUserPostGroup(String userName) {
        List<SysPost> posts = sysPostMapper.selectPostsByUserName(userName);
        if (posts == null || posts.isEmpty()) {
            return "";
        }
        return posts.stream()
                .map(SysPost::getPostName)
                .filter(StringUtils::isNotEmpty)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    public List<Long> selectPostIdsByUserId(Long userId) {
        return sysPostMapper.selectPostListByUserId(userId);
    }

    public SysUser selectUserByWechatMp(String openId) {
        return sysUserMapper.selectUserByWechatMp(openId);
    }

    @Transactional
    public boolean updateWechatMp(String userName, String openId) {
        return sysUserMapper.updateWechatMp(userName, openId) > 0;
    }

    private void insertUserPost(SysUser user) {
        Long[] posts = user.getPostIds();
        if (posts == null || posts.length == 0) {
            return;
        }
        List<SysUserPost> list = new ArrayList<>(posts.length);
        for (Long postId : posts) {
            if (postId == null) {
                continue;
            }
            SysUserPost up = new SysUserPost();
            up.setUserId(user.getUserId());
            up.setPostId(postId);
            list.add(up);
        }
        if (!list.isEmpty()) {
            sysUserPostMapper.batchUserPost(list);
        }
    }

    private static void applyDefaultUserType(SysUser user) {
        if (StringUtils.isEmpty(user.getUserType())) {
            user.setUserType(UserConstants.USER_TYPE_SYSTEM);
        }
    }
}
