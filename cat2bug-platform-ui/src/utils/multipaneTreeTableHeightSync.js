/**
 * 交付物树 + 表格 multipane 左侧高度：
 * - 表体有纵向滚动：与 el-table 外框等高；
 * - 表体无滚动：纵向撑满 multipane 行（置底）。
 */

export function resolveElTableRoot(tableComponentRef) {
  if (!tableComponentRef) return null;
  const wrapped = tableComponentRef.$refs && tableComponentRef.$refs.elTable;
  if (wrapped && wrapped.$el) return wrapped.$el;
  const root = tableComponentRef.$el;
  if (root && root.classList && root.classList.contains('el-table')) return root;
  return null;
}

export function clearMultipaneTreeModuleSync(treeModuleEl) {
  if (!treeModuleEl) return;
  treeModuleEl.classList.remove('tree-module--sync-table-height');
  treeModuleEl.classList.remove('tree-module--stretch-bottom');
  treeModuleEl.style.removeProperty('--tree-module-sync-height');
}

export function clearMultipaneResizerSync(resizerEl) {
  if (!resizerEl) return;
  resizerEl.classList.remove('multipane-resizer--sync-tree-height');
  resizerEl.style.removeProperty('--multipane-resizer-sync-height');
  resizerEl.style.removeProperty('height');
}

/** 分隔条高度与左侧交付物树外框一致 */
export function syncMultipaneResizerWithTree(resizerEl, treeModuleEl, minHeightPx) {
  if (!resizerEl || !treeModuleEl) {
    clearMultipaneResizerSync(resizerEl);
    return;
  }
  const treeH = Math.ceil(treeModuleEl.getBoundingClientRect().height);
  const height = Math.max(treeH, Math.ceil(Number(minHeightPx) || 0));
  if (height > 0) {
    resizerEl.classList.add('multipane-resizer--sync-tree-height');
    resizerEl.style.setProperty('--multipane-resizer-sync-height', `${height}px`);
    resizerEl.style.height = `${height}px`;
  } else {
    clearMultipaneResizerSync(resizerEl);
  }
}

function syncResizerAfterLayout(resizerEl, treeModuleEl, fallbackPaneEl) {
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      const fallbackH = fallbackPaneEl
        ? Math.ceil(fallbackPaneEl.getBoundingClientRect().height)
        : 0;
      syncMultipaneResizerWithTree(resizerEl, treeModuleEl, fallbackH);
    });
  });
}

export function syncMultipaneTreeModuleWithTable(treeModuleEl, tableComponentRef, resizerEl, fallbackPaneEl) {
  if (!treeModuleEl) return;
  const tableEl = resolveElTableRoot(tableComponentRef);
  if (!tableEl) {
    clearMultipaneTreeModuleSync(treeModuleEl);
    treeModuleEl.classList.add('tree-module--stretch-bottom');
    syncResizerAfterLayout(resizerEl, treeModuleEl, fallbackPaneEl);
    return;
  }
  const bodyWrapper = tableEl.querySelector('.el-table__body-wrapper');
  if (!bodyWrapper) {
    clearMultipaneTreeModuleSync(treeModuleEl);
    treeModuleEl.classList.add('tree-module--stretch-bottom');
    syncResizerAfterLayout(resizerEl, treeModuleEl, fallbackPaneEl);
    return;
  }
  const hasVerticalScroll = bodyWrapper.scrollHeight > bodyWrapper.clientHeight + 1;

  if (hasVerticalScroll) {
    treeModuleEl.classList.remove('tree-module--stretch-bottom');
    const height = Math.ceil(tableEl.getBoundingClientRect().height);
    if (height > 0) {
      treeModuleEl.classList.add('tree-module--sync-table-height');
      treeModuleEl.style.setProperty('--tree-module-sync-height', `${height}px`);
    } else {
      clearMultipaneTreeModuleSync(treeModuleEl);
    }
  } else {
    treeModuleEl.classList.remove('tree-module--sync-table-height');
    treeModuleEl.style.removeProperty('--tree-module-sync-height');
    treeModuleEl.classList.add('tree-module--stretch-bottom');
  }

  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      const fallbackH = fallbackPaneEl
        ? Math.ceil(fallbackPaneEl.getBoundingClientRect().height)
        : 0;
      syncMultipaneResizerWithTree(resizerEl, treeModuleEl, fallbackH);
    });
  });
}
