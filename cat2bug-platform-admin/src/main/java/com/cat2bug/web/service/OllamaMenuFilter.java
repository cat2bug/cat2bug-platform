package com.cat2bug.web.service;

import com.cat2bug.common.core.domain.entity.SysMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * 未启用 Ollama 时从动态路由菜单树中移除 Ollama 模型管理入口。
 */
public final class OllamaMenuFilter
{
    private OllamaMenuFilter()
    {
    }

    public static boolean isOllamaMenu(SysMenu menu)
    {
        if (menu == null)
        {
            return false;
        }
        if ("ollama".equals(menu.getPath()))
        {
            return true;
        }
        String component = menu.getComponent();
        return component != null && component.contains("system/project/ai/index");
    }

    public static List<SysMenu> removeOllamaMenus(List<SysMenu> menus)
    {
        if (menus == null || menus.isEmpty())
        {
            return menus;
        }
        List<SysMenu> result = new ArrayList<>();
        for (SysMenu menu : menus)
        {
            if (isOllamaMenu(menu))
            {
                continue;
            }
            List<SysMenu> children = menu.getChildren();
            if (children != null && !children.isEmpty())
            {
                menu.setChildren(removeOllamaMenus(children));
            }
            result.add(menu);
        }
        return result;
    }
}
