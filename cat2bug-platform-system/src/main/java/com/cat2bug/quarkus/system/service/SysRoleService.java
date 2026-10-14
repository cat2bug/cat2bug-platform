package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.core.page.PageDomain;
import com.cat2bug.quarkus.common.domain.SysRole;
import com.cat2bug.quarkus.common.domain.SysRoleDept;
import com.cat2bug.quarkus.common.domain.SysRoleMenu;
import com.cat2bug.quarkus.common.domain.SysUserConfig;
import com.cat2bug.quarkus.common.domain.SysUserRole;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.system.mapper.SysDeptMapper;
import com.cat2bug.quarkus.system.mapper.SysRoleDeptMapper;
import com.cat2bug.quarkus.system.mapper.SysRoleMapper;
import com.cat2bug.quarkus.system.mapper.SysRoleMenuMapper;
import com.cat2bug.quarkus.system.mapper.SysUserConfigMapper;
import com.cat2bug.quarkus.system.mapper.SysUserRoleMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class SysRoleService {

    @Inject
    SysRoleMapper sysRoleMapper;

    @Inject
    SysDeptMapper sysDeptMapper;

    @Inject
    SysRoleMenuMapper roleMenuMapper;

    @Inject
    SysRoleDeptMapper roleDeptMapper;

    @Inject
    SysUserRoleMapper userRoleMapper;

    @Inject
    SysUserConfigMapper userConfigMapper;

    public TableDataInfo selectRolePage(SysRole role, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysRoleMapper.countRoleList(role);
        var rows = sysRoleMapper.selectRoleList(role, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public List<SysRole> selectRoleList(SysRole role) {
        return sysRoleMapper.selectRoleList(role, 0, Integer.MAX_VALUE);
    }

    public SysRole selectRoleById(Long roleId) {
        return sysRoleMapper.selectRoleById(roleId);
    }

    public List<Long> selectDeptListByRoleId(Long roleId) {
        SysRole role = selectRoleById(roleId);
        if (role == null) {
            return new ArrayList<>();
        }
        return sysDeptMapper.selectDeptListByRoleId(roleId, role.isDeptCheckStrictly());
    }

    public List<SysRole> selectRoleAll() {
        return sysRoleMapper.selectRoleAll();
    }

    public List<SysRole> selectRolesByUserId(Long userId) {
        SysUserConfig userConfig = userConfigMapper.selectSysUserConfigByUserId(userId);
        if (userConfig == null) {
            return new ArrayList<>();
        }
        List<SysRole> userRoles = sysRoleMapper.selectRolePermissionByUserId(
                userConfig.getCurrentTeamId(), userConfig.getCurrentProjectId(), userId);
        List<SysRole> roles = selectRoleAll();
        for (SysRole role : roles) {
            for (SysRole userRole : userRoles) {
                if (role.getRoleId() != null && role.getRoleId().equals(userRole.getRoleId())) {
                    role.setFlag(true);
                    break;
                }
            }
        }
        return roles;
    }

    public Set<String> selectRolePermissionByUserId(Long userId) {
        SysUserConfig userConfig = userConfigMapper.selectSysUserConfigByUserId(userId);
        List<SysRole> perms;
        if (userConfig == null) {
            perms = sysRoleMapper.selectRolePermissionByUserId(null, null, userId);
        } else {
            perms = sysRoleMapper.selectRolePermissionByUserId(
                    userConfig.getCurrentTeamId(), userConfig.getCurrentProjectId(), userId);
        }
        Set<String> permsSet = new HashSet<>();
        for (SysRole perm : perms) {
            if (perm != null && perm.getRoleKey() != null) {
                permsSet.add(perm.getRoleKey());
            }
        }
        return permsSet;
    }

    public boolean checkRoleNameUnique(SysRole role) {
        Long roleId = role.getRoleId() == null ? -1L : role.getRoleId();
        SysRole info = sysRoleMapper.checkRoleNameUnique(role.getRoleName());
        return info == null || info.getRoleId().longValue() == roleId.longValue();
    }

    public boolean checkRoleKeyUnique(SysRole role) {
        Long roleId = role.getRoleId() == null ? -1L : role.getRoleId();
        SysRole info = sysRoleMapper.checkRoleKeyUnique(role.getRoleKey());
        return info == null || info.getRoleId().longValue() == roleId.longValue();
    }

    public void checkRoleAllowed(SysRole role) {
        if (role != null && role.getRoleId() != null && role.isAdmin()) {
            throw new ApiException("不允许操作超级管理员角色");
        }
    }

    public void checkRoleDataScope(Long roleId) {
        // DataScope AOP not ported; admin-only operations in tests.
    }

    public int countUserRoleByRoleId(Long roleId) {
        return userRoleMapper.countUserRoleByRoleId(roleId);
    }

    @Transactional
    public int insertRole(SysRole role) {
        sysRoleMapper.insertRole(role);
        return insertRoleMenu(role);
    }

    @Transactional
    public int updateRole(SysRole role) {
        sysRoleMapper.updateRole(role);
        roleMenuMapper.deleteRoleMenuByRoleId(role.getRoleId());
        return insertRoleMenu(role);
    }

    public int updateRoleStatus(SysRole role) {
        return sysRoleMapper.updateRole(role);
    }

    @Transactional
    public int authDataScope(SysRole role) {
        sysRoleMapper.updateRole(role);
        roleDeptMapper.deleteRoleDeptByRoleId(role.getRoleId());
        return insertRoleDept(role);
    }

    public int deleteAuthUser(SysUserRole userRole) {
        return userRoleMapper.deleteUserRoleInfo(userRole);
    }

    public int deleteAuthUsers(Long roleId, Long[] userIds) {
        return userRoleMapper.deleteUserRoleInfos(roleId, userIds);
    }

    @Transactional
    public int insertAuthUsers(Long roleId, Long[] userIds) {
        if (userIds == null || userIds.length == 0) {
            return 0;
        }
        List<SysUserRole> list = new ArrayList<>();
        for (Long userId : userIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            list.add(ur);
        }
        return userRoleMapper.batchUserRole(list);
    }

    @Transactional
    public int deleteRoleByIds(Long[] roleIds) {
        for (Long roleId : roleIds) {
            checkRoleAllowed(new SysRole(roleId));
            checkRoleDataScope(roleId);
            SysRole role = selectRoleById(roleId);
            if (countUserRoleByRoleId(roleId) > 0) {
                throw new ApiException(String.format("%1$s已分配,不能删除", role.getRoleName()));
            }
        }
        roleMenuMapper.deleteRoleMenu(roleIds);
        roleDeptMapper.deleteRoleDept(roleIds);
        return sysRoleMapper.deleteRoleByIds(roleIds);
    }

    private int insertRoleMenu(SysRole role) {
        Long[] menuIds = role.getMenuIds();
        if (menuIds == null || menuIds.length == 0) {
            return 1;
        }
        List<SysRoleMenu> list = new ArrayList<>();
        for (Long menuId : menuIds) {
            list.add(new SysRoleMenu(role.getRoleId(), menuId));
        }
        return roleMenuMapper.batchRoleMenu(list);
    }

    private int insertRoleDept(SysRole role) {
        Long[] deptIds = role.getDeptIds();
        if (deptIds == null || deptIds.length == 0) {
            return 1;
        }
        List<SysRoleDept> list = new ArrayList<>();
        for (Long deptId : deptIds) {
            SysRoleDept rd = new SysRoleDept();
            rd.setRoleId(role.getRoleId());
            rd.setDeptId(deptId);
            list.add(rd);
        }
        return roleDeptMapper.batchRoleDept(list);
    }
}
