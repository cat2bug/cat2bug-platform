package com.cat2bug.system.service;

import com.cat2bug.common.constant.UserConstants;
import com.cat2bug.common.domain.SysDept;
import com.cat2bug.common.domain.TreeSelect;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.SecurityUtils;
import com.cat2bug.system.mapper.SysDeptMapper;
import com.cat2bug.system.security.DataScopeSqlBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class SysDeptService {

    @Inject
    SysDeptMapper sysDeptMapper;

    @Inject
    DataScopeSqlBuilder dataScopeSqlBuilder;

    public List<SysDept> selectDeptList(SysDept dept) {
        return sysDeptMapper.selectDeptList(dept, dataScopeSqlBuilder.forCurrentUser("d", null));
    }

    public List<TreeSelect> selectDeptTreeList(SysDept dept) {
        return buildDeptTreeSelect(selectDeptList(dept));
    }

    public SysDept selectDeptById(Long deptId) {
        return sysDeptMapper.selectDeptById(deptId);
    }

    public int selectNormalChildrenDeptById(Long deptId) {
        return sysDeptMapper.selectNormalChildrenDeptById(deptId);
    }

    public boolean hasChildByDeptId(Long deptId) {
        return sysDeptMapper.hasChildByDeptId(deptId) > 0;
    }

    public boolean checkDeptExistUser(Long deptId) {
        return sysDeptMapper.checkDeptExistUser(deptId) > 0;
    }

    public boolean checkDeptNameUnique(SysDept dept) {
        Long deptId = dept.getDeptId() == null ? -1L : dept.getDeptId();
        SysDept info = sysDeptMapper.checkDeptNameUnique(dept.getDeptName(), dept.getParentId());
        return info == null || info.getDeptId().longValue() == deptId.longValue();
    }

    /** 按当前登录用户的数据范围校验目标部门可见；超级管理员不受限 */
    public void checkDeptDataScope(Long deptId) {
        if (deptId == null || SecurityUtils.isAdmin()) {
            return;
        }
        SysDept query = new SysDept();
        query.setDeptId(deptId);
        if (selectDeptList(query).isEmpty()) {
            throw new ApiException("没有权限访问部门数据！");
        }
    }

    public int insertDept(SysDept dept) {
        if (dept.getParentId() == null || dept.getParentId() == 0L) {
            dept.setAncestors("0");
        } else {
            SysDept parent = sysDeptMapper.selectDeptById(dept.getParentId());
            if (parent == null) {
                throw new ApiException("父部门不存在");
            }
            if (!UserConstants.DEPT_NORMAL.equals(parent.getStatus())) {
                throw new ApiException("部门停用，不允许新增");
            }
            dept.setAncestors(parent.getAncestors() + "," + dept.getParentId());
        }
        return sysDeptMapper.insertDept(dept);
    }

    public int updateDept(SysDept dept) {
        SysDept newParentDept = sysDeptMapper.selectDeptById(dept.getParentId());
        SysDept oldDept = sysDeptMapper.selectDeptById(dept.getDeptId());
        if (newParentDept != null && oldDept != null) {
            String newAncestors = newParentDept.getAncestors() + "," + newParentDept.getDeptId();
            String oldAncestors = oldDept.getAncestors();
            dept.setAncestors(newAncestors);
            updateDeptChildren(dept.getDeptId(), newAncestors, oldAncestors);
        }
        int result = sysDeptMapper.updateDept(dept);
        if (UserConstants.DEPT_NORMAL.equals(dept.getStatus()) && StringUtils.isNotEmpty(dept.getAncestors())
                && !StringUtils.equals("0", dept.getAncestors())) {
            updateParentDeptStatusNormal(dept);
        }
        return result;
    }

    public int deleteDeptById(Long deptId) {
        return sysDeptMapper.deleteDeptById(deptId);
    }

    public List<SysDept> buildDeptTree(List<SysDept> depts) {
        List<SysDept> returnList = new ArrayList<>();
        List<Long> tempList = depts.stream().map(SysDept::getDeptId).collect(Collectors.toList());
        for (SysDept dept : depts) {
            if (!tempList.contains(dept.getParentId())) {
                recursionFn(depts, dept);
                returnList.add(dept);
            }
        }
        if (returnList.isEmpty()) {
            returnList = depts;
        }
        return returnList;
    }

    public List<TreeSelect> buildDeptTreeSelect(List<SysDept> depts) {
        return buildDeptTree(depts).stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    private void updateParentDeptStatusNormal(SysDept dept) {
        sysDeptMapper.updateDeptStatusNormal(toLongArray(dept.getAncestors()));
    }

    private void updateDeptChildren(Long deptId, String newAncestors, String oldAncestors) {
        List<SysDept> children = sysDeptMapper.selectChildrenDeptById(deptId);
        for (SysDept child : children) {
            child.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
        }
        if (!children.isEmpty()) {
            sysDeptMapper.updateDeptChildren(children);
        }
    }

    private static Long[] toLongArray(String str) {
        if (StringUtils.isEmpty(str)) {
            return new Long[0];
        }
        String[] parts = str.split(",");
        Long[] result = new Long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Long.parseLong(parts[i].trim());
        }
        return result;
    }

    private static void recursionFn(List<SysDept> list, SysDept parent) {
        List<SysDept> childList = getChildList(list, parent);
        parent.setChildren(childList);
        for (SysDept child : childList) {
            if (hasChild(list, child)) {
                recursionFn(list, child);
            }
        }
    }

    private static List<SysDept> getChildList(List<SysDept> list, SysDept parent) {
        List<SysDept> result = new ArrayList<>();
        for (SysDept item : list) {
            if (item.getParentId() != null && parent.getDeptId() != null
                    && item.getParentId().longValue() == parent.getDeptId().longValue()) {
                result.add(item);
            }
        }
        return result;
    }

    private static boolean hasChild(List<SysDept> list, SysDept parent) {
        return !getChildList(list, parent).isEmpty();
    }
}
