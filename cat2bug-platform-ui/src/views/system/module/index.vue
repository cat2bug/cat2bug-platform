<template>
  <div class="app-container case-body module-page project-list-page-host" ref="moduleMain">
    <project-label class="module-project-label" />
    <div ref="moduleTools" class="module-tools" :class="{ 'wrapped-tools': moduleToolsWrapped }">
      <el-form class="left" :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="120px" :class="{ 'list-query-keyboard-nav': listQueryNavActive }">
        <el-form-item prop="moduleName" class="module-hint-query list-query-nav-item" data-query-key="moduleName">
          <el-input
            v-model="queryParams.moduleName"
            size="small"
            :placeholder="$t('module.enter-module-name')"
            prefix-icon="el-icon-files"
            clearable
            @input="handleQuery"
          />
        </el-form-item>
      </el-form>

      <div ref="moduleToolsRight" class="module-tools-right">
          <el-button
            class="module-hint-toggle"
            type="info"
            plain
            icon="el-icon-sort"
            size="small"
            @click="toggleExpandAll"
          >{{ $t('module.expand-collapse') }}</el-button>
          <el-button
            class="module-hint-create"
            type="primary"
            plain
            icon="el-icon-plus"
            size="small"
            @click="handleAdd"
            v-hasPermi="['system:module:add']"
          >{{ $t('module.new') }}</el-button>
      </div>
    </div>
    <div
      ref="moduleTableWrap"
      class="module-table-wrap module-hint-tree-nav"
      tabindex="-1"
      @focusin="onModuleTableFocusIn"
      @focusout="onModuleTableFocusOut"
    >
    <el-table
      ref="moduleTable"
      highlight-current-row
      v-if="refreshTable"
      v-loading="loading"
      :data="moduleList"
      row-key="moduleId"
      :default-expand-all="isExpandAll"
      :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
    >
      <el-table-column :label="$t('module.name')" align="start" prop="moduleName">
        <template slot-scope="scope">
          <span class="module-row-kbd-hint-anchor">{{ scope.row.moduleName }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('annex')" align="left" prop="annexUrls">
        <template slot-scope="scope">
          <div>
            <cat2-bug-text :content="file" type="down" :tooltip="file" v-for="(file,index) in getUrl(scope.row.annexUrls)" :key="index" />
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('remark')" align="center" prop="remark" />
      <el-table-column :label="$t('operate')" align="left" class-name="small-padding fixed-width" width="200">
        <template slot-scope="scope">
          <el-button
            size="small"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:module:edit']"
          >{{ $t('modify') }}</el-button>
          <el-button
            size="small"
            type="text"
            icon="el-icon-plus"
            @click="handleAdd(scope.row)"
            v-hasPermi="['system:module:add']"
          >{{ $t('add') }}</el-button>
          <el-button
            size="small"
            type="text"
            icon="el-icon-delete"
            class="red"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:module:remove']"
          >{{ $t('delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    </div>

    <!-- 添加或修改模块对话框 -->
    <module-dialog ref="moduleDialog" :project-id="getProjectId()" @added="getList" @updated="getList" />
  </div>
</template>

<script>
import { listModule, getModule, delModule, addModule, updateModule } from "@/api/system/module";
import ModuleDialog from "@/components/Module/ModuleDialog";
import ProjectLabel from "@/components/Project/ProjectLabel";
import Cat2BugText from "@/components/Cat2BugText";
import pageActionHints from '@/mixins/page-action-hints'
import listQueryKeyboardNav from '@/mixins/list-query-keyboard-nav'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { checkPermi } from '@/utils/permission'
import {
  assignRowHintLetters,
  collectHintLettersFromToolbar,
  getDefectTableScrollBody,
  isRowIntersectingContainer,
  resolveDefectTableRowHintAnchor,
  resolveDefectTableRowHintPositionRect,
  resolveElTableRowData
} from '@/utils/defect-row-kbd-hints'

const MODULE_KBD_SCOPE = 'module'

export default {
  name: "Module",
  mixins: [pageActionHints, listQueryKeyboardNav],
  components: {
    ProjectLabel,
    ModuleDialog,
    Cat2BugText
  },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 模块表格数据
      moduleList: [],
      // 是否展开，默认全部展开
      isExpandAll: true,
      // 重新渲染表格状态
      refreshTable: true,
      moduleToolsWrapped: false,
      moduleTableFocused: false,
      // 查询参数
      queryParams: {
        modulePid: null,
        moduleName: null,
        projectId: this.getProjectId(),
        pageNum: 1,
        pageSize: 10
      },
    };
  },
  computed: {
    getUrl: function () {
      return function (urls){
        if(urls) {
          let files = urls ? urls.split(',') : [];
          return files.map(i => {
            return process.env.VUE_APP_BASE_API + i;
          })
        } else {
          return [];
        }
      }
    },
  },
  created() {
    this.getList();
  },
  mounted() {
    // 初始化浮动菜单
    this.$nextTick(() => {
      this.syncModuleToolsWrapped();
    });
    window.addEventListener("resize", this.syncModuleToolsWrapped);
    this.registerModuleShortcuts();
    this.bindModuleTableKeys();
  },
  activated() {
    this.registerModuleShortcuts();
  },
  deactivated() {
    if (this.$shortcut) this.$shortcut.unregisterPage(MODULE_KBD_SCOPE);
  },
  destroyed() {
    window.removeEventListener("resize", this.syncModuleToolsWrapped);
    this.unbindModuleTableKeys();
    if (this.$shortcut) this.$shortcut.unregisterPage(MODULE_KBD_SCOPE);
  },
  beforeDestroy() {
    if (this.$shortcut) this.$shortcut.unregisterPage(MODULE_KBD_SCOPE);
  },
  methods: {
    registerModuleShortcuts() {
      if (!this.$shortcut) return
      this.$shortcut.registerPage(MODULE_KBD_SCOPE, [
        { key: 'query', defaultLetter: 'S', run: () => this.shortcutFocusQuery() },
        { key: 'toggleExpand', defaultLetter: 'F', run: () => this.toggleExpandAll() },
        { key: 'treeNav', defaultLetter: 'G', run: () => this.shortcutTreeNav() },
        { key: 'create', defaultLetter: 'E', run: () => this.shortcutCreateModule() }
      ])
    },
    getPageActionHintContainer() {
      return this.$refs.moduleMain || this.$el
    },
    getPageActionHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${MODULE_KBD_SCOPE}.${key}`, def)
      return [
        {
          key: 'query',
          letter: L('query', 'S'),
          badgeSelector: '.module-hint-query',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.shortcutFocusQuery()
        },
        {
          key: 'toggleExpand',
          letter: L('toggleExpand', 'F'),
          badgeSelector: '.module-hint-toggle',
          floatOffset: { placement: 'bottom-right-outset', outset: 2, dy: 5 },
          run: () => this.toggleExpandAll()
        },
        {
          key: 'treeNav',
          letter: L('treeNav', 'G'),
          badgeSelector: '.module-hint-tree-nav',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.shortcutTreeNav()
        },
        {
          key: 'create',
          letter: L('create', 'E'),
          badgeSelector: '.module-hint-create',
          floatOffset: { placement: 'bottom-right-outset', outset: 2, dy: 5 },
          run: () => this.shortcutCreateModule(),
          visible: () => checkPermi(['system:module:add'])
        }
      ]
    },
    /** ⌘ 按住：表格可见行名称列动态徽标（1–9 优先，字母补位） */
    getPageDynamicActionHints(ctx) {
      const used = (ctx && ctx.usedLetters) ? new Set(ctx.usedLetters) : new Set()
      collectHintLettersFromToolbar(this.getPageActionHints()).forEach((ch) => used.add(ch))
      const rowFloat = { placement: 'center-cell' }
      return this.buildModuleTableRowActionHints(used, rowFloat)
    },
    getPageActionHintScrollRoots() {
      const table = this.$refs.moduleTable
      if (!table || !table.$el) return []
      const bodyWrap = getDefectTableScrollBody(table.$el)
      return bodyWrap ? [bodyWrap] : []
    },
    buildModuleTableRowActionHints(usedLetters, rowFloat) {
      if (!checkPermi(['system:module:edit'])) return []
      const table = this.$refs.moduleTable
      if (!table || !table.$el || this.loading) return []
      const bodyWrap = getDefectTableScrollBody(table.$el)
      if (!bodyWrap) return []
      const tableRoot = table.$el
      const seen = new Set()
      const anchors = []
      bodyWrap.querySelectorAll('tbody tr.el-table__row').forEach((tr) => {
        if (!isRowIntersectingContainer(tr, bodyWrap)) return
        const row = resolveElTableRowData(tr)
        if (!row || row.moduleId == null) return
        const moduleId = String(row.moduleId)
        if (seen.has(moduleId)) return
        seen.add(moduleId)
        const anchor = resolveDefectTableRowHintAnchor(tr)
        if (!anchor) return
        anchors.push({
          anchor,
          getAnchorRect: () => resolveDefectTableRowHintPositionRect(tr, tableRoot),
          skipViewportCheck: true,
          run: () => {
            if (!checkPermi(['system:module:edit'])) return
            this.handleUpdate(row)
          }
        })
      })
      const letters = assignRowHintLetters(anchors.length, usedLetters)
      return anchors.map((item, i) => ({
        ...item,
        letter: letters[i],
        floatOffset: rowFloat,
        key: `row-${i}`
      })).filter((item) => item.letter)
    },
    shortcutFocusQuery() {
      this.enterListQueryKeyboardNav()
    },
    getListQueryNavItems() {
      return [{ key: 'moduleName' }]
    },
    getListQueryNavToolbarRef() {
      return 'moduleToolsRight'
    },
    shortcutCreateModule() {
      if (!checkPermi(['system:module:add'])) return
      this.handleAdd()
    },
    shortcutTreeNav() {
      this.enterModuleTableKeyboardNav()
    },
    onModuleTableFocusIn() {
      this.moduleTableFocused = true
    },
    enterModuleTableKeyboardNav() {
      const wrap = this.$refs.moduleTableWrap
      if (wrap && typeof wrap.focus === 'function') {
        wrap.focus()
      }
      this.moduleTableFocused = true
      const visible = this.getVisibleModuleTableRows()
      const table = this.$refs.moduleTable
      if (!table || !visible.length) return
      const current = table.store && table.store.states.currentRow
      const idx = current
        ? visible.findIndex((item) => item.row.moduleId === current.moduleId)
        : -1
      table.setCurrentRow(visible[idx >= 0 ? idx : 0].row)
      this.$nextTick(() => this.scrollModuleTableRowIntoView(table.store.states.currentRow))
    },
    getVisibleModuleTableRows() {
      const table = this.$refs.moduleTable
      if (!table || !table.$el) return []
      const bodyWrap = getDefectTableScrollBody(table.$el)
      if (!bodyWrap) return []
      const rows = []
      bodyWrap.querySelectorAll('tbody tr.el-table__row').forEach((tr) => {
        const row = resolveElTableRowData(tr)
        if (row && row.moduleId != null) {
          rows.push({ tr, row })
        }
      })
      return rows
    },
    getCurrentModuleTableRow() {
      const table = this.$refs.moduleTable
      return (table && table.store && table.store.states.currentRow) || null
    },
    moduleRowHasChildren(row) {
      return !!(row && row.children && row.children.length)
    },
    isModuleRowExpanded(row) {
      const table = this.$refs.moduleTable
      const store = table && table.store
      const treeData = store && store.states && store.states.treeData
      if (!row || !treeData) return false
      const node = treeData[row.moduleId]
      return !!(node && node.expanded)
    },
    expandModuleRow(row) {
      const table = this.$refs.moduleTable
      if (!table || !table.store || !this.moduleRowHasChildren(row)) return
      table.store.toggleTreeExpansion(row, true)
    },
    collapseModuleRow(row) {
      const table = this.$refs.moduleTable
      if (!table || !table.store) return
      table.store.toggleTreeExpansion(row, false)
    },
    scrollModuleTableRowIntoView(row) {
      if (!row) return
      const visible = this.getVisibleModuleTableRows()
      const item = visible.find((entry) => entry.row.moduleId === row.moduleId)
      if (item && item.tr && typeof item.tr.scrollIntoView === 'function') {
        item.tr.scrollIntoView({ block: 'nearest' })
      }
    },
    bindModuleTableKeys() {
      this._moduleTableKeyHandler = (e) => {
        if (!this.moduleTableFocused) return
        const tag = e.target && e.target.tagName
        if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return
        if (e.metaKey || e.ctrlKey || e.altKey) return
        if (e.key === 'ArrowUp') {
          e.preventDefault()
          this.navigateModuleRow(-1)
        } else if (e.key === 'ArrowDown') {
          e.preventDefault()
          this.navigateModuleRow(1)
        } else if (e.key === 'ArrowRight') {
          e.preventDefault()
          const row = this.getCurrentModuleTableRow()
          if (row) this.expandModuleRow(row)
        } else if (e.key === 'ArrowLeft') {
          e.preventDefault()
          const row = this.getCurrentModuleTableRow()
          if (row) this.collapseModuleRow(row)
        } else if (e.key === 'Enter') {
          const row = this.getCurrentModuleTableRow()
          if (!row || !checkPermi(['system:module:edit'])) return
          e.preventDefault()
          this.handleUpdate(row)
        } else if (e.key === 'Escape' || e.key === 'Esc') {
          e.preventDefault()
          const table = this.$refs.moduleTable
          if (table) table.setCurrentRow(null)
          this.moduleTableFocused = false
          const wrap = this.$refs.moduleTableWrap
          if (wrap && typeof wrap.blur === 'function') wrap.blur()
        }
      }
      window.addEventListener('keydown', this._moduleTableKeyHandler, true)
    },
    unbindModuleTableKeys() {
      if (this._moduleTableKeyHandler) {
        window.removeEventListener('keydown', this._moduleTableKeyHandler, true)
        this._moduleTableKeyHandler = null
      }
    },
    onModuleTableFocusOut(e) {
      const wrap = e.currentTarget
      if (wrap && e.relatedTarget && wrap.contains(e.relatedTarget)) return
      this.moduleTableFocused = false
    },
    navigateModuleRow(delta) {
      const table = this.$refs.moduleTable
      if (!table) return
      const visible = this.getVisibleModuleTableRows()
      if (!visible.length) return
      const current = table.store && table.store.states.currentRow
      let idx = current ? visible.findIndex((item) => item.row.moduleId === current.moduleId) : -1
      if (idx < 0) idx = 0
      else idx = Math.max(0, Math.min(visible.length - 1, idx + delta))
      table.setCurrentRow(visible[idx].row)
      this.$nextTick(() => this.scrollModuleTableRowIntoView(visible[idx].row))
    },
    syncModuleToolsWrapped() {
      const measure = () => {
        const tools = this.$refs.moduleTools;
        const left = tools && tools.querySelector(".left");
        const right = this.$refs.moduleToolsRight;
        if (!tools || !left || !right) {
          this.moduleToolsWrapped = false;
          return;
        }
        this.moduleToolsWrapped = right.offsetTop - left.offsetTop > 4;
      };
      this.$nextTick(() => {
        if (this.moduleToolsWrapped) {
          this.moduleToolsWrapped = false;
          this.$nextTick(measure);
          return;
        }
        measure();
      });
    },
    /** 查询模块列表 */
    getList() {
      this.loading = true;
      listModule(this.queryParams).then(response => {
        this.moduleList = this.handleTree(response.data, "moduleId", "modulePid");
        this.loading = false;
        this.syncModuleToolsWrapped();
      });
    },
    /** 获取项目id操作 */
    getProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 新增按钮操作 */
    handleAdd(row) {
      let modulePid = null;
      if (row != null && row.moduleId) {
        modulePid = row.moduleId;
      } else {
        modulePid = 0;
      }
      this.$refs.moduleDialog.open(null,modulePid);
    },
    /** 展开/折叠操作 */
    toggleExpandAll() {
      this.refreshTable = false;
      this.isExpandAll = !this.isExpandAll;
      this.$nextTick(() => {
        this.refreshTable = true;
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.$refs.moduleDialog.open(row.moduleId,row.modulePid);
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.$modal.confirm(this.$i18n.t('module.is-delete-module')).then(function() {
        return delModule(row.moduleId);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess(this.$i18n.t('delete-success'));
      }).catch(() => {});
    }
  }
};
</script>
<style lang="scss" scoped>
.app-container.case-body.module-page {
  padding: 20px 20px 0;
  box-sizing: border-box;
}
.module-page {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  min-height: 0;
  flex: 1 1 auto;
  width: 100%;
  box-sizing: border-box;
}
.module-page ::v-deep h3.module-project-label {
  margin-top: 0;
  margin-bottom: 0;
}
.module-tools {
  width: 100%;
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: center;
  align-content: flex-start;
  column-gap: var(--cat2bug-toolbar-section-gap, 10px);
  row-gap: var(--cat2bug-toolbar-row-gap, 8px);
  margin-top: 0;
  margin-bottom: 0;
  .el-form-item {
    margin-bottom: 0;
  }
}
.module-tools > .left {
  flex: 1 1 auto;
  min-width: 0;
  max-width: 100%;
  width: auto;
  display: inline-flex;
  flex-wrap: nowrap;
  align-items: center;
  row-gap: 8px;
  column-gap: var(--cat2bug-toolbar-item-gap, 10px);
  box-sizing: border-box;
  ::v-deep .el-form-item {
    flex: 0 0 auto;
    min-width: 0;
    margin-right: 0;
  }
  ::v-deep .el-form-item .el-form-item__content,
  ::v-deep .el-form-item .el-input {
    width: auto;
  }
}
.module-tools-right {
  flex: 0 0 auto;
  display: inline-flex;
  flex-wrap: nowrap;
  justify-content: flex-start;
  align-items: center;
  gap: var(--cat2bug-toolbar-item-gap, 10px);
  > * {
    flex: 0 0 auto;
    width: auto;
    min-width: 0;
    margin-left: 0;
  }
}
.module-tools.wrapped-tools {
  > .left {
    flex: 1 1 100%;
    width: 100%;
    max-width: 100%;
    display: inline-flex;
    flex-wrap: wrap;
    box-sizing: border-box;
  }
  > .left ::v-deep .el-form-item {
    display: inline-flex;
    flex: 1 1 0;
    min-width: 220px;
    margin-right: 0;
    margin-bottom: 0;
  }
  > .left ::v-deep .el-form-item .el-form-item__content,
  > .left ::v-deep .el-form-item .el-input {
    width: 100% !important;
    max-width: 100%;
    box-sizing: border-box;
  }
  > .module-tools-right {
    margin-left: 0 !important;
    flex: 1 1 100%;
    width: 100%;
    display: inline-flex;
    flex-wrap: wrap;
    gap: var(--cat2bug-toolbar-item-gap, 10px);
    justify-content: flex-start;
    align-items: center;
  }
}
.module-table-wrap {
  outline: none;
  &:focus-visible {
    outline: 2px solid var(--cat2bug-field-focus-color, #409eff);
    outline-offset: 2px;
    border-radius: var(--cat2bug-border-radius, 4px);
  }
}
.module-hint-toggle,
.module-hint-create {
  position: relative;
  overflow: visible !important;
}
</style>
