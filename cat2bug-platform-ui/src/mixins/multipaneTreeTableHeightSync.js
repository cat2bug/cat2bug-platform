import {
  syncMultipaneTreeModuleWithTable,
  clearMultipaneResizerSync,
} from '@/utils/multipaneTreeTableHeightSync';

/**
 * 缺陷 / 用例 / 计划 multipane：表体有滚动时左侧与表格等高，无滚动时左侧纵向置底；分隔条与左侧同高。
 * @param {string} tableRefName - 模板 ref，默认 cat2BugTable；新建计划页为 planItemTable
 */
export default {
  methods: {
    syncMultipaneTreeTableHeight(tableRefName = 'cat2BugTable') {
      if (!this.showModuleTree) return;
      this.$nextTick(() => {
        const treeEl = this.$refs.treeModule;
        const tableRef = this.$refs[tableRefName];
        const resizer = this.$refs.paneResizer;
        const resizerEl = resizer && (resizer.$el || resizer);
        if (!treeEl || !tableRef) return;
        syncMultipaneTreeModuleWithTable(treeEl, tableRef, resizerEl);
        this.$nextTick(() => {
          if (typeof this.scheduleSyncPaneResizerHandle === 'function') {
            this.scheduleSyncPaneResizerHandle();
          }
        });
      });
    },
    clearMultipaneTreeTableHeightSync() {
      const resizer = this.$refs.paneResizer;
      const resizerEl = resizer && (resizer.$el || resizer);
      clearMultipaneResizerSync(resizerEl);
    },
  },
};
