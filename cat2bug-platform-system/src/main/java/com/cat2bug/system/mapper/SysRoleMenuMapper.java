package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysRoleMenuMapper {

    int checkMenuExistRole(Long menuId);

    int deleteRoleMenuByRoleId(Long roleId);

    int deleteRoleMenu(Long[] roleIds);

    int batchRoleMenu(List<SysRoleMenu> roleMenuList);
}
