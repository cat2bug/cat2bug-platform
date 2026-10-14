package com.cat2bug.system.service;

import com.cat2bug.common.constant.UserConstants;
import com.cat2bug.common.domain.SysMenu;
import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserConfig;
import com.cat2bug.common.domain.vo.MetaVo;
import com.cat2bug.common.domain.vo.RouterVo;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.mapper.SysMenuMapper;
import com.cat2bug.system.mapper.SysRoleMapper;
import com.cat2bug.system.mapper.SysRoleMenuMapper;
import com.cat2bug.system.mapper.SysUserConfigMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.cat2bug.common.domain.TreeSelect;

@ApplicationScoped
public class SysMenuService {

    @Inject
    SysMenuMapper menuMapper;

    @Inject
    SysRoleMapper roleMapper;

    @Inject
    SysRoleMenuMapper roleMenuMapper;

    @Inject
    SysUserConfigMapper userConfigMapper;

    public List<SysMenu> selectMenuList(Long userId) {
        return selectMenuList(new SysMenu(), userId);
    }

    public List<SysMenu> selectMenuList(SysMenu menu, Long userId) {
        if (SysUser.isAdmin(userId)) {
            return menuMapper.selectMenuList(menu);
        }
        menu.getParams().put("userId", userId);
        return menuMapper.selectMenuListByUserId(menu);
    }

    public SysMenu selectMenuById(Long menuId) {
        return menuMapper.selectMenuById(menuId);
    }

    public List<Long> selectMenuListByRoleId(Long roleId) {
        SysRole role = roleMapper.selectRoleById(roleId);
        if (role == null) {
            return new ArrayList<>();
        }
        return menuMapper.selectMenuListByRoleId(roleId, role.isMenuCheckStrictly());
    }

    public boolean hasChildByMenuId(Long menuId) {
        return menuMapper.hasChildByMenuId(menuId) > 0;
    }

    public boolean checkMenuExistRole(Long menuId) {
        return roleMenuMapper.checkMenuExistRole(menuId) > 0;
    }

    public int insertMenu(SysMenu menu) {
        return menuMapper.insertMenu(menu);
    }

    public int updateMenu(SysMenu menu) {
        return menuMapper.updateMenu(menu);
    }

    public int deleteMenuById(Long menuId) {
        return menuMapper.deleteMenuById(menuId);
    }

    public boolean checkMenuNameUnique(SysMenu menu) {
        Long menuId = menu.getMenuId() == null ? -1L : menu.getMenuId();
        SysMenu info = menuMapper.checkMenuNameUnique(menu.getMenuName(), menu.getParentId());
        return info == null || info.getMenuId().longValue() == menuId.longValue();
    }

    public List<SysMenu> buildMenuTree(List<SysMenu> menus) {
        List<SysMenu> returnList = new ArrayList<>();
        List<Long> tempList = menus.stream().map(SysMenu::getMenuId).collect(Collectors.toList());
        for (Iterator<SysMenu> iterator = menus.iterator(); iterator.hasNext(); ) {
            SysMenu menu = iterator.next();
            if (!tempList.contains(menu.getParentId())) {
                recursionFn(menus, menu);
                returnList.add(menu);
            }
        }
        if (returnList.isEmpty()) {
            returnList = menus;
        }
        return returnList;
    }

    public List<TreeSelect> buildMenuTreeSelect(List<SysMenu> menus) {
        return buildMenuTree(menus).stream()
                .filter(m -> m.getOrderNum() == null || m.getOrderNum() < 99)
                .map(TreeSelect::new)
                .collect(Collectors.toList());
    }

    public List<SysMenu> selectMenuTreeByUserId(Long userId) {
        List<SysMenu> menus;
        if (SysUser.isAdmin(userId)) {
            menus = menuMapper.selectMenuTreeAll();
        } else {
            SysUserConfig config = userConfigMapper.selectSysUserConfigByUserId(userId);
            if (config == null) {
                menus = menuMapper.selectMenuTreeByUserId(null, null, userId);
            } else {
                menus = menuMapper.selectMenuTreeByUserId(
                        config.getCurrentTeamId(), config.getCurrentProjectId(), userId);
            }
        }
        return getChildPerms(menus, 0);
    }

    public List<RouterVo> buildMenus(List<SysMenu> menus) {
        List<RouterVo> routers = new LinkedList<>();
        for (SysMenu menu : menus) {
            RouterVo router = new RouterVo();
            router.setHidden("1".equals(menu.getVisible()));
            router.setName(getRouteName(menu));
            router.setPath(getRouterPath(menu));
            router.setComponent(getComponent(menu));
            router.setQuery(menu.getQuery());
            router.setMeta(new MetaVo(menu.getMenuName(), menu.getMenuNameI18nKey(), menu.getIcon(),
                    StringUtils.equals("1", menu.getIsCache()), menu.getPath()));
            List<SysMenu> children = menu.getChildren();
            if (StringUtils.isNotEmpty(children) && UserConstants.TYPE_DIR.equals(menu.getMenuType())) {
                router.setAlwaysShow(true);
                router.setRedirect("noRedirect");
                router.setChildren(buildMenus(children));
            } else if (isMenuFrame(menu)) {
                router.setMeta(null);
                List<RouterVo> childrenList = new ArrayList<>();
                RouterVo child = new RouterVo();
                child.setPath(menu.getPath());
                child.setComponent(menu.getComponent());
                child.setName(StringUtils.capitalize(menu.getPath()));
                child.setMeta(new MetaVo(menu.getMenuName(), menu.getMenuNameI18nKey(), menu.getIcon(),
                        StringUtils.equals("1", menu.getIsCache()), menu.getPath()));
                child.setQuery(menu.getQuery());
                childrenList.add(child);
                router.setChildren(childrenList);
            } else if (menu.getParentId() != null && menu.getParentId() == 0 && isInnerLink(menu)) {
                router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon()));
                router.setPath("/");
                List<RouterVo> childrenList = new ArrayList<>();
                RouterVo child = new RouterVo();
                String routerPath = innerLinkReplaceEach(menu.getPath());
                child.setPath(routerPath);
                child.setComponent(UserConstants.INNER_LINK);
                child.setName(StringUtils.capitalize(routerPath));
                child.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), menu.getPath()));
                childrenList.add(child);
                router.setChildren(childrenList);
            }
            routers.add(router);
        }
        return routers;
    }

    private static String getRouteName(SysMenu menu) {
        String routerName = StringUtils.capitalize(menu.getPath());
        if (isMenuFrame(menu)) {
            routerName = "";
        }
        return routerName;
    }

    private static String getRouterPath(SysMenu menu) {
        String routerPath = menu.getPath();
        if (menu.getParentId() != null && menu.getParentId() != 0 && isInnerLink(menu)) {
            routerPath = innerLinkReplaceEach(routerPath);
        } else if (menu.getParentId() != null && menu.getParentId() == 0
                && UserConstants.TYPE_DIR.equals(menu.getMenuType())
                && UserConstants.NO_FRAME.equals(menu.getIsFrame())) {
            routerPath = "/" + menu.getPath();
        } else if (isMenuFrame(menu)) {
            routerPath = "/";
        }
        return routerPath;
    }

    private static String getComponent(SysMenu menu) {
        String component = UserConstants.LAYOUT;
        if (StringUtils.isNotEmpty(menu.getComponent()) && !isMenuFrame(menu)) {
            component = menu.getComponent();
        } else if (StringUtils.isEmpty(menu.getComponent()) && menu.getParentId() != null
                && menu.getParentId() != 0 && isInnerLink(menu)) {
            component = UserConstants.INNER_LINK;
        } else if (StringUtils.isEmpty(menu.getComponent()) && isParentView(menu)) {
            component = UserConstants.PARENT_VIEW;
        }
        return component;
    }

    private static boolean isMenuFrame(SysMenu menu) {
        return menu.getParentId() != null && menu.getParentId() == 0
                && UserConstants.TYPE_MENU.equals(menu.getMenuType())
                && UserConstants.NO_FRAME.equals(menu.getIsFrame());
    }

    private static boolean isInnerLink(SysMenu menu) {
        return UserConstants.NO_FRAME.equals(menu.getIsFrame()) && StringUtils.ishttp(menu.getPath());
    }

    private static boolean isParentView(SysMenu menu) {
        return menu.getParentId() != null && menu.getParentId() != 0
                && UserConstants.TYPE_DIR.equals(menu.getMenuType());
    }

    private static String innerLinkReplaceEach(String path) {
        return StringUtils.isNotEmpty(path) ? path.replace("http://", "").replace("https://", "") : path;
    }

    private static List<SysMenu> getChildPerms(List<SysMenu> list, int parentId) {
        List<SysMenu> returnList = new ArrayList<>();
        for (Iterator<SysMenu> iterator = list.iterator(); iterator.hasNext(); ) {
            SysMenu item = iterator.next();
            if (item.getParentId() != null && item.getParentId() == parentId) {
                recursionFn(list, item);
                returnList.add(item);
            }
        }
        return returnList;
    }

    private static void recursionFn(List<SysMenu> list, SysMenu parent) {
        List<SysMenu> childList = getChildList(list, parent);
        parent.setChildren(childList);
        for (SysMenu child : childList) {
            if (hasChild(list, child)) {
                recursionFn(list, child);
            }
        }
    }

    private static List<SysMenu> getChildList(List<SysMenu> list, SysMenu parent) {
        List<SysMenu> result = new ArrayList<>();
        for (SysMenu item : list) {
            if (item.getParentId() != null && parent.getMenuId() != null
                    && item.getParentId().longValue() == parent.getMenuId().longValue()) {
                result.add(item);
            }
        }
        return result;
    }

    private static boolean hasChild(List<SysMenu> list, SysMenu parent) {
        return !getChildList(list, parent).isEmpty();
    }
}
