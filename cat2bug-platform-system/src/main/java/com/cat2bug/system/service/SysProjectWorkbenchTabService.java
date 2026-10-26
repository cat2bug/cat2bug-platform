package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysProjectWorkbenchTab;
import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.system.mapper.SysProjectWorkbenchTabMapper;
import com.cat2bug.system.mapper.SysRoleMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@ApplicationScoped
public class SysProjectWorkbenchTabService {

    private static final Set<String> MANAGE_ROLE_KEYS = Set.of("project.create-by", "project.admin");
    private static final Set<String> CREATE_PRESETS = Set.of("manager", "tester", "developer", "document");

    @Inject
    SysProjectWorkbenchTabMapper tabMapper;

    @Inject
    SysRoleMapper sysRoleMapper;

    public List<SysProjectWorkbenchTab> listVisible(Long projectId, Long userId) {
        List<SysProjectWorkbenchTab> list = tabMapper.selectVisibleTabs(projectId, userId);
        return list == null ? List.of() : list;
    }

    public int countShared(Long projectId) {
        return tabMapper.countSharedByProject(projectId);
    }

    public boolean canManageShared(Long projectId, Long userId) {
        return hasManageRole(projectId, userId);
    }

    public List<String> listProjectRoleKeys(Long projectId, Long userId) {
        List<SysRole> roles = sysRoleMapper.selectRolePermissionByUserId(null, projectId, userId);
        List<String> keys = new ArrayList<>();
        if (roles == null) {
            return keys;
        }
        for (SysRole role : roles) {
            if (role != null && role.getRoleKey() != null && !role.getRoleKey().isBlank()) {
                keys.add(role.getRoleKey());
            }
        }
        return keys;
    }

    public SysProjectWorkbenchTab getVisibleOrThrow(Long tabId, Long userId) {
        SysProjectWorkbenchTab tab = tabMapper.selectById(tabId);
        if (tab == null) {
            throw new ApiException(404, "页签不存在");
        }
        if (!canView(tab, userId)) {
            throw new ApiException(403, "无权查看该页签");
        }
        return tab;
    }

    @Transactional
    public SysProjectWorkbenchTab create(Long projectId, Long userId, String tabName, String preset,
                                         String visibilityRequested, String configJson) {
        String name = normalizeName(tabName);
        String presetId = normalizeCreatePreset(preset);
        boolean manage = hasManageRole(projectId, userId);
        String visibility = SysProjectWorkbenchTab.VISIBILITY_PRIVATE;
        if (manage && SysProjectWorkbenchTab.VISIBILITY_PROJECT.equalsIgnoreCase(
                visibilityRequested == null ? "" : visibilityRequested.trim())) {
            visibility = SysProjectWorkbenchTab.VISIBILITY_PROJECT;
        }
        if (SysProjectWorkbenchTab.VISIBILITY_PROJECT.equals(visibility)) {
            int shared = tabMapper.countSharedByProject(projectId);
            if (shared >= SysProjectWorkbenchTab.MAX_SHARED_PER_PROJECT) {
                throw new ApiException(400, "项目共享页签已达上限（24）");
            }
        }
        if (configJson == null || configJson.isBlank()) {
            throw new ApiException(400, "configJson 不能为空");
        }
        Date now = new Date();
        SysProjectWorkbenchTab row = new SysProjectWorkbenchTab();
        row.setProjectId(projectId);
        row.setOwnerUserId(userId);
        row.setVisibility(visibility);
        row.setTabName(name);
        row.setPreset(presetId);
        row.setConfigJson(configJson);
        row.setTabSort(now.getTime());
        row.setCreateTime(now);
        row.setUpdateTime(now);
        tabMapper.insert(row);
        return row;
    }

    @Transactional
    public SysProjectWorkbenchTab update(Long tabId, Long userId, String tabName, String preset, String configJson) {
        SysProjectWorkbenchTab existing = tabMapper.selectById(tabId);
        if (existing == null) {
            throw new ApiException(404, "页签不存在");
        }
        assertCanWrite(existing, userId);
        if (tabName != null) {
            existing.setTabName(normalizeName(tabName));
        }
        if (preset != null && !preset.isBlank()) {
            existing.setPreset(preset.trim().toLowerCase(Locale.ROOT));
        }
        if (configJson != null) {
            if (configJson.isBlank()) {
                throw new ApiException(400, "configJson 不能为空");
            }
            existing.setConfigJson(configJson);
        }
        existing.setUpdateTime(new Date());
        tabMapper.updateMetaAndConfig(existing);
        return tabMapper.selectById(tabId);
    }

    @Transactional
    public void delete(Long tabId, Long userId) {
        SysProjectWorkbenchTab existing = tabMapper.selectById(tabId);
        if (existing == null) {
            throw new ApiException(404, "页签不存在");
        }
        assertCanWrite(existing, userId);
        tabMapper.deleteById(tabId);
    }

    /**
     * 同可见性组内改序：body 为 {@code [{tabId, tabSort}, ...]}。
     * 共享页签需管理员；私有页签仅本人。不可跨 visibility 混排。
     */
    @Transactional
    public void reorder(Long projectId, Long userId, List<SysProjectWorkbenchTab> items) {
        if (items == null || items.isEmpty()) {
            throw new ApiException(400, "排序列表不能为空");
        }
        String visibility = null;
        List<SysProjectWorkbenchTab> patch = new ArrayList<>();
        for (SysProjectWorkbenchTab item : items) {
            if (item == null || item.getTabId() == null || item.getTabSort() == null) {
                throw new ApiException(400, "排序项须含 tabId 与 tabSort");
            }
            SysProjectWorkbenchTab existing = tabMapper.selectById(item.getTabId());
            if (existing == null || existing.getProjectId() == null
                    || !existing.getProjectId().equals(projectId)) {
                throw new ApiException(404, "页签不存在");
            }
            assertCanWrite(existing, userId);
            if (visibility == null) {
                visibility = existing.getVisibility();
            } else if (!visibility.equals(existing.getVisibility())) {
                throw new ApiException(400, "不可跨共享/私有组排序");
            }
            SysProjectWorkbenchTab row = new SysProjectWorkbenchTab();
            row.setTabId(existing.getTabId());
            row.setTabSort(item.getTabSort());
            patch.add(row);
        }
        tabMapper.updateSort(patch);
    }

    private void assertCanWrite(SysProjectWorkbenchTab tab, Long userId) {
        if (SysProjectWorkbenchTab.VISIBILITY_PRIVATE.equals(tab.getVisibility())) {
            if (userId == null || !userId.equals(tab.getOwnerUserId())) {
                throw new ApiException(403, "无权修改该私有页签");
            }
            return;
        }
        if (!hasManageRole(tab.getProjectId(), userId)) {
            throw new ApiException(403, "仅项目管理员可修改共享页签");
        }
    }

    private boolean canView(SysProjectWorkbenchTab tab, Long userId) {
        if (SysProjectWorkbenchTab.VISIBILITY_PROJECT.equals(tab.getVisibility())) {
            return true;
        }
        return userId != null && userId.equals(tab.getOwnerUserId());
    }

    private boolean hasManageRole(Long projectId, Long userId) {
        List<SysRole> roles = sysRoleMapper.selectRolePermissionByUserId(null, projectId, userId);
        if (roles == null) {
            return false;
        }
        for (SysRole role : roles) {
            if (role != null && MANAGE_ROLE_KEYS.contains(role.getRoleKey())) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeName(String tabName) {
        if (tabName == null || tabName.isBlank()) {
            throw new ApiException(400, "页签名称不能为空");
        }
        String name = tabName.trim();
        if (name.length() > SysProjectWorkbenchTab.MAX_NAME_LENGTH) {
            throw new ApiException(400, "页签名称不能超过12个字符");
        }
        return name;
    }

    private static String normalizeCreatePreset(String preset) {
        String id = preset == null ? "" : preset.trim().toLowerCase(Locale.ROOT);
        if (!CREATE_PRESETS.contains(id)) {
            throw new ApiException(400, "无效的预设");
        }
        return id;
    }
}
