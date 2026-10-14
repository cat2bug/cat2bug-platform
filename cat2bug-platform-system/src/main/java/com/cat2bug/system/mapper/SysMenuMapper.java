package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysMenuMapper {

    List<String> selectMenuPermsByUserId(Long userId);

    List<String> selectMenuPermsByRoleId(Long roleId);

    List<SysMenu> selectMenuTreeAll();

    List<SysMenu> selectMenuTreeByUserId(@Param("teamId") Long teamId,
                                         @Param("projectId") Long projectId,
                                         @Param("userId") Long userId);

    List<SysMenu> selectMenuList(SysMenu menu);

    List<SysMenu> selectMenuListByUserId(SysMenu menu);

    SysMenu selectMenuById(Long menuId);

    List<Long> selectMenuListByRoleId(@Param("roleId") Long roleId,
                                      @Param("menuCheckStrictly") boolean menuCheckStrictly);

    int hasChildByMenuId(Long menuId);

    SysMenu checkMenuNameUnique(@Param("menuName") String menuName, @Param("parentId") Long parentId);

    int insertMenu(SysMenu menu);

    int updateMenu(SysMenu menu);

    int deleteMenuById(Long menuId);
}
