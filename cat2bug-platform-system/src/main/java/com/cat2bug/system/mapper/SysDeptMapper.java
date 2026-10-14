package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysDeptMapper {

    List<SysDept> selectDeptList(SysDept dept);

    SysDept selectDeptById(Long deptId);

    int checkDeptExistUser(Long deptId);

    int hasChildByDeptId(Long deptId);

    List<SysDept> selectChildrenDeptById(Long deptId);

    int selectNormalChildrenDeptById(Long deptId);

    SysDept checkDeptNameUnique(@Param("deptName") String deptName, @Param("parentId") Long parentId);

    int insertDept(SysDept dept);

    int updateDept(SysDept dept);

    int updateDeptChildren(@Param("depts") List<SysDept> depts);

    int updateDeptStatusNormal(@Param("array") Long[] deptIds);

    List<Long> selectDeptListByRoleId(@Param("roleId") Long roleId,
                                      @Param("deptCheckStrictly") boolean deptCheckStrictly);

    int deleteDeptById(Long deptId);
}
