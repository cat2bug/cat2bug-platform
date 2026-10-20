package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.constant.DefectConstants;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.SysDefect;
import com.cat2bug.common.domain.SysProjectDefectTabs;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.system.mapper.SysProjectDefectTabsMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SysProjectDefectTabsService {

    /** 「我的」页签默认状态：排除已关闭 */
    public static final List<Integer> MY_TAB_DEFECT_STATES = List.of(
            DefectConstants.STATE_PROCESSING,
            DefectConstants.STATE_AUDIT,
            DefectConstants.STATE_RESOLVED,
            DefectConstants.STATE_REJECTED
    );

    @Inject
    SysProjectDefectTabsMapper sysProjectDefectTabsMapper;

    public TableDataInfo selectTabsPage(SysProjectDefectTabs query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysProjectDefectTabsMapper.countSysProjectDefectTabsList(query);
        List<SysProjectDefectTabs> rows = sysProjectDefectTabsMapper.selectSysProjectDefectTabsList(
                query, page.getOffset(), page.getPageSize());
        upgradeLegacyMyTabs(rows);
        return new TableDataInfo(rows, total);
    }

    public List<SysProjectDefectTabs> selectSysProjectDefectTabsList(SysProjectDefectTabs query) {
        List<SysProjectDefectTabs> rows = sysProjectDefectTabsMapper.selectSysProjectDefectTabsListAll(query);
        upgradeLegacyMyTabs(rows);
        return rows;
    }

    public SysProjectDefectTabs selectSysProjectDefectTabsByTabId(Long tabId) {
        SysProjectDefectTabs tab = sysProjectDefectTabsMapper.selectSysProjectDefectTabsByTabId(tabId);
        if (tab != null) {
            upgradeLegacyMyTabs(List.of(tab));
        }
        return tab;
    }

    /**
     * 历史「我的」页签只有处理人、未写状态；补上排除已关闭并写回库，避免仍列出 CLOSED。
     */
    private void upgradeLegacyMyTabs(List<SysProjectDefectTabs> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (SysProjectDefectTabs tab : rows) {
            if (!isMyTabName(tab.getTabName()) || !needsMyTabStateUpgrade(tab.getConfig())) {
                continue;
            }
            SysDefect config = tab.getConfig();
            if (config == null) {
                config = new SysDefect();
                tab.setConfig(config);
            }
            Map<String, Object> params = config.getParams();
            if (params == null) {
                params = new HashMap<>();
                config.setParams(params);
            }
            params.put("defectStates", new ArrayList<>(MY_TAB_DEFECT_STATES));
            if (params.get("delFlag") == null || "".equals(params.get("delFlag"))) {
                params.put("delFlag", "0");
            }
            sysProjectDefectTabsMapper.updateSysProjectDefectTabs(tab);
        }
    }

    private static boolean isMyTabName(String tabName) {
        if (tabName == null || tabName.isBlank()) {
            return false;
        }
        String name = tabName.trim();
        if ("我的".equals(name) || "Mine".equalsIgnoreCase(name) || "My".equalsIgnoreCase(name)) {
            return true;
        }
        try {
            return name.equals(MessageUtils.message("my"));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean needsMyTabStateUpgrade(SysDefect config) {
        if (config == null) {
            return true;
        }
        Map<String, Object> params = config.getParams();
        if (params == null || params.get("defectStates") == null) {
            return true;
        }
        Object states = params.get("defectStates");
        if (!(states instanceof Collection<?> collection)) {
            return true;
        }
        return collection.isEmpty();
    }

    public SysProjectDefectTabs insertSysProjectDefectTabs(SysProjectDefectTabs tabs) {
        tabs.setCreateTime(new Date());
        int rows = sysProjectDefectTabsMapper.insertSysProjectDefectTabs(tabs);
        return rows == 1 ? tabs : null;
    }

    public int updateSysProjectDefectTabs(SysProjectDefectTabs tabs) {
        return sysProjectDefectTabsMapper.updateSysProjectDefectTabs(tabs);
    }

    public int updateSort(List<SysProjectDefectTabs> list) {
        return sysProjectDefectTabsMapper.updateSort(list);
    }

    public int deleteSysProjectDefectTabsByTabIds(Long[] tabIds) {
        return sysProjectDefectTabsMapper.deleteSysProjectDefectTabsByTabIds(tabIds);
    }
}
