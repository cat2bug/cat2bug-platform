<template>
  <div
    class="defect-route-host project-list-page-host"
    :class="{ 'defect-route-host--excel': defectContentComponent === 'DefectExcel' }"
  >
    <div
      ref="defectMain"
      class="app-container case-body defect-page"
      :class="{ 'defect-page--excel-view': defectContentComponent === 'DefectExcel' }"
      :style="defectPageRootStyle"
    >
      <project-label ref="defectProjectLabel" class="defect-project-label" />
      <!-- 缺陷页标签-->
      <div
        ref="defectToolsTab"
        class="defect-tools-tab defect-list-hint-tabs"
        :class="{ 'defect-tab-keyboard-nav': defectTabNavActive }"
      >
        <el-tabs ref="defectTabs" v-model="activeDefectTabName" @tab-click="selectDefectTabHandle">
          <el-tab-pane v-for="tab in config.tabs" :key="tab.tabId+''" :name="tab.tabId+''">
            <span
              slot="label"
              class="defect-tab-label"
            >
              <svg-icon icon-class="list2" class="defect-tab-icon" />
              <span class="defect-tab-text" :title="tab.tabName">{{ tab.tabName }}</span>
              <i style="width: 14px;" class="el-icon-close" @click.stop="removeDefectTabHandle(tab.tabId)" />
            </span>
          </el-tab-pane>
          <el-tab-pane key="all-tab" :name="allTab">
            <span
              slot="label"
              class="defect-tab-label"
            >
              <svg-icon icon-class="all" class="defect-tab-icon" />
              <span class="defect-tab-text" :title="$t('defect.all-defect')">{{ $t('defect.all-defect') }}</span>
            </span>
          </el-tab-pane>
          <el-tab-pane key="deleted-tab" :name="deletedTab">
            <span
              slot="label"
              class="defect-tab-label"
            >
              <svg-icon icon-class="delete" class="defect-tab-icon" />
              <span class="defect-tab-text" :title="$t('defect.deleted-defect')">{{ $t('defect.deleted-defect') }}</span>
            </span>
          </el-tab-pane>
          <el-tab-pane :name="defectAddTabPaneName" disabled class="defect-tab-add-pane">
            <svg-icon
              slot="label"
              icon-class="add-tab"
              :class-name="defectTabAddBtnClass"
              :title="$t('defect.tab')"
              @click.native.stop="addDefectTabHandle"
              @mousedown.native.stop.prevent
            />
          </el-tab-pane>
        </el-tabs>
        <div class="defect-tools-tab-right">
          <span class="defect-list-hint-statistic-panel" aria-hidden="true" />
          <span class="defect-list-hint-statistic">
            <svg-icon v-show="statisticPanelVisible" class="defect-tools-button" icon-class="view-statistic" @click.native="addStatisticHandle" />
          </span>
        </div>
      </div>
      <!-- 统计板块-->
      <div
        v-show="defectStatisticWrapVisible"
        ref="defectStatisticWrap"
        class="defect-tools-statistic-wrap"
        :class="{ 'defect-statistic-keyboard-nav': defectStatisticNavActive }"
      >
        <cat2-bug-statistic
          ref="defectStatistic"
          class="defect-tools-statistic"
          :params="{}"
          :draggable="true"
          @change="onDefectStatisticListChange"
        />
        <span v-if="defectStatisticNavAvailable" class="defect-list-hint-stat-nav" aria-hidden="true" />
      </div>
      <!-- 动态缺陷显示组件-->
      <!--    <keep-alive>-->
      <component
        :is="defectContentComponent"
        ref="defectContentComponent"
        :class="['defect-content-slot', { 'defect-content-slot--excel': defectContentComponent === 'DefectExcel' }]"
        :query="queryParams"
        :project-id="projectId"
        v-bind="defectViewExtraProps"
        @defect-click="handleDefectClick"
        @refresh="handleRefreshQuery"
        @column-picker-visible-change="onDefectColumnPickerVisibleChange"
      >
        <template slot="left-tools">
          <!-- 搜索-->
          <div class="defect-tools-search" :class="{ 'defect-query-keyboard-nav': defectQueryNavActive }">
            <el-form v-show="showSearch" ref="queryForm" :model="queryParams" size="small" :inline="true" label-width="0">
              <el-form-item>
                <!-- 缺陷显示模式切换 -->
                <el-radio-group
                  v-model="defectContentComponent"
                  class="defect-content-view-switch defect-list-hint-view-switch"
                  size="mini"
                  @input="handleDefectContentChange"
                >
                  <!-- 表格模式 -->
                  <el-radio-button label="DefectTable">
                    <span class="defect-content-view-switch-inner" :title="$t('table')">
                      <svg-icon icon-class="table" />
                    </span>
                  </el-radio-button>
                  <!--                <el-radio-button label="DefectCalendar">-->
                  <!--                  <span class="defect-content-view-switch-inner" :title="$t('calendar')">-->
                  <!--                    <svg-icon icon-class="date" />-->
                  <!--                  </span>-->
                  <!--                </el-radio-button>-->
                  <!-- Excel模式 -->
                  <el-radio-button label="DefectExcel">
                    <span class="defect-content-view-switch-inner" :title="$t('excel')">
                      <svg-icon icon-class="excel2" />
                    </span>
                  </el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item prop="defectType" class="defect-list-query-nav-item" data-query-key="defectType">
                <el-dropdown
                  ref="defectTypeDropdown"
                  class="cat2bug-split-dropdown-kbd"
                  split-button
                  size="small"
                  @command="defectTypeChangeHandle"
                  @click="selectDefectTabHandle"
                  @visible-change="onDefectTypeDropdownVisibleChange"
                >
                  {{ $i18n.t(activeDefectTypeName) }}
                  <el-dropdown-menu slot="dropdown">
                    <el-dropdown-item command="">{{ $i18n.t('defect.all-type') }}</el-dropdown-item>
                    <el-dropdown-item v-for="type in config.types" :key="'type_'+type.key" :command="type.value">{{ $i18n.t(type.value) }}</el-dropdown-item>
                  </el-dropdown-menu>
                </el-dropdown>
              </el-form-item>
              <el-form-item prop="defectState" class="defect-list-query-nav-item" data-query-key="defectState">
                <el-select
                  ref="defectQueryStateSelect"
                  v-model="queryParams.params.defectStates"
                  class="defect-state-select"
                  size="small"
                  multiple
                  collapse-tags
                  clearable
                  :placeholder="$t('defect.select-state')"
                  @change="handleQuery()"
                >
                  <template #prefix>
                    <i class="select-header-icon el-icon-finished" />
                  </template>
                  <el-option
                    v-for="state in config.states"
                    :key="state.key"
                    :label="$i18n.t(state.value)"
                    :value="state.key"
                  />
                </el-select>
              </el-form-item>
              <el-form-item prop="handleBy" class="defect-list-query-nav-item" data-query-key="handleBy">
                <select-project-member
                  ref="defectQueryHandleBy"
                  v-model="queryParams.handleBy"
                  :project-id="projectId"
                  placeholder="defect.select-handle-by"
                  :is-head="false"
                  size="small"
                  icon="el-icon-user"
                  @input="handleQuery()"
                />
              </el-form-item>
              <el-form-item prop="nameVersionKeyword" class="defect-list-query-nav-item defect-list-hint-query" data-query-key="nameVersionKeyword">
                <el-input
                  v-model="queryParams.nameVersionKeyword"
                  size="small"
                  :placeholder="$t('defect.enter-name-or-version')"
                  prefix-icon="el-icon-search"
                  clearable
                  @input="handleQuery()"
                />
              </el-form-item>
            </el-form>
          </div>
        </template>
        <template slot="right-tools">
          <span v-if="defectAddToolbarVisible" class="defect-add-toolbar-kbd-wrap">
            <el-dropdown
            ref="defectAddDropdown"
            class="defect-add-dropdown defect-list-hint-add-menu cat2bug-split-dropdown-kbd"
            split-button
            trigger="click"
            size="small"
            type="primary"
            @click="handleAdd"
            @visible-change="onDefectAddDropdownVisibleChange"
          >
            <i class="el-icon-plus" />{{ $i18n.t('defect.create') }}
            <el-dropdown-menu slot="dropdown" class="defect-add-dropdown-menu">
              <el-dropdown-item @click.native="handleAdd"><i class="el-icon-plus" />{{ $i18n.t('defect.create') }}</el-dropdown-item>
              <el-divider class="defect-add-dropdown-divider" />
              <el-dropdown-item @click.native="handleImport"><i class="el-icon-upload2" />{{ $t('defect.import') }}</el-dropdown-item>
              <el-dropdown-item @click.native="handleExport"><i class="el-icon-download" />{{ $t('defect.export') }}</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
          </span>
        </template>
      </component>
      <!--    </keep-alive>-->
      <!-- 缺陷日历-->
      <!--    <defect-calendar v-else-if="defectContentId=='calendar'" ref="defectCalendar" />-->
      <!-- 添加或修改缺陷对话框 -->
      <add-defect ref="addDefectForm" :project-id="getProjectId()" @added="search(queryParams)" />
      <!-- 浏览缺陷对话框 -->
      <handle-defect ref="editDefectForm" :project-id="getProjectId()" @change="handleRefreshQuery" @delete="handleRefreshQuery" />
      <!-- 添加页签对话框 -->
      <defect-tab-dialog ref="defectTabDialog" :project-id="getProjectId()" :member-id="userId" @add="tabAddHandle" />
      <!-- 导入缺陷 -->
      <defect-import ref="defectImportDialog" :project-id="getProjectId()" @upload="search(queryParams)" />
    </div>
  </div>
</template>

<script>
import { checkPermi } from '@/utils/permission'
import { delTabs } from '@/api/system/DefectTabs'
import { configDefect } from '@/api/system/defect'
import AddDefect from '@/components/Defect/AddDefect'
import HandleDefect from '@/components/Defect/HandleDefect'
import SelectProjectMember from '@/components/Project/SelectProjectMember'
import ProjectLabel from '@/components/Project/ProjectLabel'
import Cat2BugStatistic from '@/components/Cat2BugStatistic'
import DefectTabDialog from '@/views/system/defect/DefectTabDialog'
import DefectImport from '@/views/system/defect/DefectImport'
import i18n from '@/utils/i18n/i18n'
import { getDefectTempTab, lifeTime, removeDefectTempTab } from '@/utils/defect'
import { resolveExportAssetHost } from '@/utils/ruoyi'
import {
  appendExportColumnParams,
  resolveDefectExportColumns
} from '@/utils/excel-export-columns'
import store from '@/store'
import DefectTable from './list/table'
import DefectExcel from './list/excel'
import { clearExtensionParams, hasParticipationExtension } from './query-extension'
import pageActionHints from '@/mixins/page-action-hints'
import splitDropdownKbd from '@/mixins/split-dropdown-kbd'
import {
  bindSplitDropdownHost,
  closeSplitDropdown,
  createDropdownMenuKeyboardState,
  focusInitialDropdownMenuItem,
  getSplitDropdownFocusTarget,
  shortcutOpenSplitDropdown
} from '@/utils/split-dropdown-kbd'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import {
  assignRowHintLetters,
  collectHintLettersFromToolbar,
  findExcelSheetRow,
  getDefectTableScrollBody,
  isExcelRowVisibleInViewport,
  isRowIntersectingContainer,
  parseExcelRowKeyFromTdId,
  resolveDefectTableRowHintAnchor,
  resolveDefectTableRowHintPositionRect,
  resolveExcelRowHintAnchor
} from '@/utils/defect-row-kbd-hints'
import { activateStatisticItemClick } from '@/utils/statistic-item-kbd'
import { findTopFormDrawerVm } from '@/utils/defect-drawer-shortcuts'

/** 记录Tab标签选项 */
const DEFECT_TAB_CACHE_KEY = 'defect-tab'
/** 缺陷列表/日历/Excel 视图组件名，与 el-radio-button label 一致；默认 DefectTable */
const DEFECT_CONTENT_VIEW_CACHE_KEY = 'defect.contentViewComponent'
const DEFECT_CONTENT_VIEW_ALLOWED = ['DefectTable', 'DefectExcel']
/** 名称等于所有选项的name */
const ALL_TAB_NAME = 'all-tab'
const DELETED_TAB_NAME = 'deleted-tab'
/** 添加 Tab 占位 pane，不作为真实页签选中 */
const DEFECT_ADD_TAB_PANE_NAME = '__defect_add_tab__'

/** 从本地缓存解析缺陷页签：不恢复「已删除」页签，避免刷新后新建按钮被隐藏 */
function resolveDefectTabFromCache(raw) {
  if (!raw || raw === DEFECT_ADD_TAB_PANE_NAME || raw === DELETED_TAB_NAME) {
    return null
  }
  if (raw === ALL_TAB_NAME) {
    return ALL_TAB_NAME
  }
  return String(raw)
}

/** 记录分析模版是否显示的缓存变量名 */
const CACHE_KEY_STATISTIC_PANEL_VISIBLE = 'defect.statisticPanelVisible'
export default {
  name: 'Defect',
  mixins: [pageActionHints, splitDropdownKbd],
  components: { AddDefect, HandleDefect, SelectProjectMember, ProjectLabel, Cat2BugStatistic, DefectTabDialog, DefectTable, DefectExcel, DefectImport },
  data() {
    return {
      // 当前缺陷面板的类型
      defectContentId: 'list',
      defectContentComponent: 'DefectTable',
      // tab相关配置
      // 所有tab的名称
      allTab: ALL_TAB_NAME,
      deletedTab: DELETED_TAB_NAME,
      defectAddTabPaneName: DEFECT_ADD_TAB_PANE_NAME,
      // 当前缺陷的tab页名
      activeDefectTabName: ALL_TAB_NAME, // this.$i18n.t('project.my-participated-in'),

      // 显示搜索条件
      showSearch: true,
      // 分析图表列表
      statisticList: [],
      // 查询中缺陷类型的名称
      activeDefectTypeName: 'defect.all-type',
      // 查询中缺陷状态的名称
      activeDefectStateName: 'defect.all-state',
      // 缺陷配置
      config: {},

      // 是否显示统计面板
      statisticPanelVisible: this.$cache.local.get(CACHE_KEY_STATISTIC_PANEL_VISIBLE) != 'false',
      /** Cmd/Ctrl+J 循环是否停在添加按钮（非真实 tab，需单独记序） */
      defectShortcutTabAtAdd: false,
      /** Cmd/Ctrl+J 后 Tab 条键盘导航（左右键切换，末项右切到添加按钮） */
      defectTabNavActive: false,
      defectTabNavIndex: -1,
      /** 键盘导航当前聚焦的 tab name 或添加 pane name */
      defectTabNavFocusedName: null,
      /** Tab 切换过程中忽略 focusout，避免误退出键盘导航 */
      defectTabNavSuppressBlur: false,
      /** Cmd/Ctrl+Q 后查询区键盘导航（左右键切换，默认末项名称/版本搜索框） */
      defectQueryNavActive: false,
      defectQueryNavIndex: -1,
      defectQueryNavSuppressBlur: false,
      /** Cmd/Ctrl+E 打开新建菜单后，左右键在右侧工具栏按钮间切换 */
      defectToolbarNavActive: false,
      defectToolbarNavIndex: -1,
      defectToolbarNavSuppressBlur: false,
      /** Cmd/Ctrl+G 后统计模块键盘导航（左右选择，Delete 移除） */
      defectStatisticNavActive: false,
      defectStatisticNavIndex: -1,
      defectStatisticNavSuppressBlur: false,
      /** 统计区当前模块数量（用于 G 快捷键显隐） */
      defectStatisticItemCount: 0,
      /** 键盘打开显示字段弹层后，↑↓ 在复选框行间切换 */
      defectColumnPickerKeyboardOpen: false,
      defectColumnPickerMenuIndex: 0,
      /** 键盘 ↓ 打开类型下拉（query nav） */
      defectTypeDropdownKeyboardOpen: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        orderByColumn: 'updateTime',
        isAsc: 'desc',
        defectType: null,
        defectName: null,
        nameVersionKeyword: null,
        projectId: 0,
        testPlanId: null,
        caseId: null,
        dataSources: null,
        dataSourcesParams: null,
        moduleId: null,
        moduleVersion: null,
        createBy: null,
        updateTime: null,
        createTime: null,
        updateBy: null,
        defectState: null,
        handleBy: null,
        handleTime: null,
        defectLevel: null,
        params: {
          defectStates: [],
          delFlag: '0'
        }
      }
    }
  },
  computed: {
    /** 获取当前用户id */
    currentUserId: function() {
      return this.$store.state.user.id
    },
    /** 获取项目id */
    projectId: function() {
      return this.$route.query.projectId ? parseInt(this.$route.query.projectId) : parseInt(this.$store.state.user.config.currentProjectId)
    },
    /** 获取用户id */
    userId: function() {
      return Number(this.$store.state.user.id)
    },
    /** Excel 视图额外参数（其它视图不声明对应 prop，避免落到根 DOM） */
    defectViewExtraProps() {
      if (this.defectContentComponent === 'DefectExcel') {
        return {
          defectTypeOptions: this.config.types || [],
          /** 底边贴齐主区域：编辑区高度用满 clientHeight，不在内部再减空白 */
          viewportBottomGap: 0
        }
      }
      return {}
    },
    /**
     * Excel：在 host 内 height:100% 铺满主列（与表格模式一致），勿再用 host flex-end 造成顶栏下大块留白。
     */
    isDeletedDefectTab() {
      return this.activeDefectTabName === this.deletedTab
    },
    /** 工具栏新建/导入/导出：非已删除页签且具备新增权限 */
    defectAddToolbarVisible() {
      return !this.isDeletedDefectTab && this.checkPermi(['system:defect:add'])
    },
    defectTabAddBtnClass() {
      return 'defect-tab-label defect-tab-add-btn'
    },
    defectPageRootStyle() {
      if (this.defectContentComponent === 'DefectExcel') {
        return {
          height: '100%',
          maxHeight: '100%',
          minHeight: 0,
          paddingBottom: 'env(safe-area-inset-bottom, 0px)',
          boxSizing: 'border-box',
          overflow: 'hidden'
        }
      }
      return {
        boxSizing: 'border-box'
      }
    },
    defectStatisticNavLetter() {
      return this.pageActionLetter('statisticNav') || 'G'
    },
    /** 统计区有模块时才显示/响应 G 导航快捷键 */
    defectStatisticNavAvailable() {
      return this.statisticPanelVisible && this.defectStatisticItemCount > 0
    },
    /** 无统计模块时不渲染占位 wrap，避免破坏 Tab 与工具栏间 10px gap */
    defectStatisticWrapVisible() {
      return this.statisticPanelVisible && this.defectStatisticItemCount > 0
    }
  },
  watch: {
    statisticPanelVisible(val) {
      if (val) {
        this.$nextTick(() => {
          const statistic = this.$refs.defectStatistic
          if (statistic && typeof statistic.refreshData === 'function') {
            statistic.refreshData()
          }
          this.syncDefectStatisticItemCount()
        })
      } else {
        this.defectStatisticItemCount = 0
        if (this.defectStatisticNavActive) {
          this.exitDefectStatisticKeyboardNav()
        }
        this.registerDefectShortcuts()
      }
    },
    'queryParams.defectType': function(newVal, oldVal) {
      if (newVal != oldVal) {
        // this.defectTypeChangeHandle(newVal);
      }
    },
    'queryParams.defectState': function(newVal, oldVal) {
      if (newVal != oldVal) {
        // this.handleQuery();
      }
    }
  },
  created() {
    let v = this.$cache.local.get(DEFECT_CONTENT_VIEW_CACHE_KEY)
    if (v != null && typeof v === 'string') v = v.trim()
    if (v && DEFECT_CONTENT_VIEW_ALLOWED.includes(v)) {
      this.defectContentComponent = v
    } else {
      this.defectContentComponent = 'DefectTable'
    }
    this.applyDefectTabCacheEarly()
  },
  mounted() {
    /** 根据项目ID跳转 */
    if (this.$route.query.projectId) {
      const _this = this
      store.dispatch('SwitchCurrentProject', this.$route.query.projectId).then(() => {
        _this.init()
      })
    } else {
      this.init()
    }
  },
  activated() {
    this.registerDefectShortcuts()
    this.$nextTick(() => {
      this.$_bindDefectQueryNavFocusIn()
      this.$_bindDefectToolbarNavFocusIn()
    })
    /** keep-alive 返回后刷新表格布局（固定列等） */
    this.$nextTick(() => {
      const c = this.$refs.defectContentComponent
      const tbl = c && c.$refs && c.$refs.cat2BugTable
      if (tbl && typeof tbl.doLayout === 'function') {
        tbl.doLayout()
      }
      this.$nextTick(() => {
        if (c && typeof c.setDragComponentSize === 'function') {
          c.setDragComponentSize()
        }
      })
    })
  },
  deactivated() {
    this.exitDefectTabKeyboardNav()
    this.exitDefectQueryKeyboardNav()
    this.exitDefectRightToolbarNav()
    this.exitDefectStatisticKeyboardNav()
    this.$_detachDefectColumnPickerMenuListeners()
    this.$_unbindDefectQueryNavFocusIn()
    this.$_unbindDefectToolbarNavFocusIn()
    this.clearTabDataLoadTimer()
    if (this.$shortcut) this.$shortcut.unregisterPage('defect')
  },
  // 移除滚动条监听
  destroyed() {
    this.exitDefectTabKeyboardNav()
    this.exitDefectQueryKeyboardNav()
    this.exitDefectRightToolbarNav()
    this.exitDefectStatisticKeyboardNav()
    this.$_detachDefectColumnPickerMenuListeners()
    this.$_unbindDefectQueryNavFocusIn()
    this.$_unbindDefectToolbarNavFocusIn()
    this.clearTabDataLoadTimer()
    if (this.$shortcut) this.$shortcut.unregisterPage('defect')
  },
  methods: {
    checkPermi,
    /** 首屏同步页签缓存，避免先渲染「全部」再切到「已删除」导致新建按钮一闪消失 */
    applyDefectTabCacheEarly() {
      const raw = this.$cache.local.get(DEFECT_TAB_CACHE_KEY)
      if (raw === DELETED_TAB_NAME) {
        this.$cache.local.set(DEFECT_TAB_CACHE_KEY, ALL_TAB_NAME)
      }
      const tab = resolveDefectTabFromCache(raw === DELETED_TAB_NAME ? ALL_TAB_NAME : raw)
      if (!tab) {
        return
      }
      this.activeDefectTabName = tab
      if (!this.queryParams.params) {
        this.$set(this.queryParams, 'params', { defectStates: [], delFlag: '0' })
      }
      this.$set(this.queryParams.params, 'delFlag', '0')
    },
    init() {
      this.queryParams.projectId = this.projectId
      // 初始化对象
      this.$refs.defectContentComponent.init()
      // 获取缺陷配置
      this.getDefectConfig()
      // 显示指定缺陷信息
      if (this.$route.query.defectId) {
        this.$refs.editDefectForm.open(this.$route.query.defectId)
      }
      this.registerDefectShortcuts()
      this.$nextTick(() => {
        this.$_bindDefectQueryNavFocusIn()
        this.$_bindDefectToolbarNavFocusIn()
      })
    },
    /** 向快捷键引擎注册缺陷页动作（动作引导键 Space 打开） */
    registerDefectShortcuts() {
      if (!this.$shortcut) return
      const actions = [
        { key: 'query', defaultLetter: 'S', run: () => this.shortcutFocusQuery() },
        { key: 'newDefect', defaultLetter: 'E', run: () => this.shortcutOpenDefectAddDropdown() },
        { key: 'switchTab', defaultLetter: 'J', run: () => this.shortcutSwitchTab() },
        { key: 'statistic', defaultLetter: 'I', run: () => this.addStatisticHandle() },
        { key: 'switchView', defaultLetter: 'O', run: () => this.shortcutSwitchView() },
        { key: 'prevPage', defaultLetter: 'B', run: () => this.shortcutChangePage(-1) },
        { key: 'nextPage', defaultLetter: 'P', run: () => this.shortcutChangePage(1) }
      ]
      if (this.defectStatisticNavAvailable) {
        actions.splice(4, 0, { key: 'statisticNav', defaultLetter: 'G', run: () => this.shortcutStatisticNav() })
      }
      this.$shortcut.registerPage('defect', actions)
    },
    getPageActionHintContainer() {
      return this.$refs.defectMain || this.$el
    },
    /** Cmd/Ctrl 按住时在工具栏显示字母徽标（与 Space 动作面板映射一致） */
    getPageActionHints() {
      const scopeKey = 'defect'
      const L = (key, def) => shortcutStore.getLetter(`action.${scopeKey}.${key}`, def)
      return [
        {
          key: 'query',
          letter: L('query', 'S'),
          badgeSelector: '.defect-list-hint-query input.el-input__inner',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.shortcutFocusQuery(),
          visible: () => this.showSearch
        },
        {
          key: 'newDefect',
          letter: L('newDefect', 'E'),
          badgeSelector: '.defect-list-hint-add-menu.cat2bug-split-dropdown-kbd .cat2bug-split-dropdown-focus-target',
          floatOffset: { placement: 'bottom-right-outset', outset: 3 },
          run: () => this.shortcutOpenDefectAddDropdown(),
          visible: () => this.defectAddToolbarVisible
        },
        {
          key: 'switchTab',
          letter: L('switchTab', 'J'),
          badgeSelector: '.defect-list-hint-tabs .el-tabs__item.is-active .defect-tab-label',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.shortcutSwitchTab()
        },
        {
          key: 'statistic',
          letter: L('statistic', 'I'),
          badgeSelector: '.defect-list-hint-statistic-panel',
          floatOffset: { placement: 'bottom-right-outset', outset: 2, dx: 5, dy: 5 },
          run: () => this.addStatisticHandle()
        },
        {
          key: 'statisticNav',
          letter: L('statisticNav', 'G'),
          badgeSelector: '.defect-list-hint-stat-nav',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.shortcutStatisticNav(),
          visible: () => this.defectStatisticNavAvailable
        },
        {
          key: 'switchView',
          letter: L('switchView', 'O'),
          badgeSelector: '.defect-list-hint-view-switch',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.shortcutSwitchView()
        },
        {
          key: 'prevPage',
          letter: L('prevPage', 'B'),
          badgeSelector: '.defect-table-pagination .btn-prev',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.shortcutChangePage(-1)
        },
        {
          key: 'nextPage',
          letter: L('nextPage', 'P'),
          badgeSelector: '.defect-table-pagination .btn-next',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.shortcutChangePage(1)
        }
      ]
    },
    /** ⌘ 按住：表格/Excel 可见行序号列动态徽标（1–9 优先，字母补位） */
    getPageDynamicActionHints(ctx) {
      const used = (ctx && ctx.usedLetters) ? new Set(ctx.usedLetters) : new Set()
      collectHintLettersFromToolbar(this.getPageActionHints()).forEach((ch) => used.add(ch))
      if (this.defectContentComponent === 'DefectTable') {
        const rowFloat = { placement: 'center-cell' }
        return this.buildDefectTableRowActionHints(used, rowFloat)
      }
      if (this.defectContentComponent === 'DefectExcel') {
        const rowFloat = { placement: 'below-right-outset', outset: 1, dx: 8, dy: -7, anchorTextBounds: true }
        return this.buildDefectExcelRowActionHints(used, rowFloat)
      }
      return []
    },
    /** ⌘ 按住时监听滚动，刷新行徽标可见性 */
    getPageActionHintScrollRoots() {
      const content = this.$refs.defectContentComponent
      if (!content) return []
      if (this.defectContentComponent === 'DefectExcel') {
        const tc = this.getDefectExcelTableContent(content)
        return tc ? [tc] : []
      }
      if (this.defectContentComponent === 'DefectTable') {
        const bodyWrap = this.getDefectTableScrollBody(content)
        return bodyWrap ? [bodyWrap] : []
      }
      return []
    },
    getDefectTableRootEl(content) {
      if (!content || !content.$el) return null
      return (content.$refs && content.$refs.cat2BugTable && content.$refs.cat2BugTable.$el) ||
        content.$el
    },
    getDefectTableScrollBody(content) {
      return getDefectTableScrollBody(this.getDefectTableRootEl(content))
    },
    getDefectExcelTableContent(content) {
      const ed = content && content.$refs && content.$refs.excelEditor
      if (!ed) return null
      return ed.tableContent || (ed.$refs && ed.$refs.tableContent) || null
    },
    buildDefectTableRowActionHints(usedLetters, rowFloat) {
      const content = this.$refs.defectContentComponent
      const bodyWrap = this.getDefectTableScrollBody(content)
      if (!bodyWrap || !content) return []
      const tableRoot = this.getDefectTableRootEl(content)
      const defectList = content.defectList || []
      const seen = new Set()
      const anchors = []
      bodyWrap.querySelectorAll('tbody tr.el-table__row').forEach((tr, rowIndex) => {
        if (!isRowIntersectingContainer(tr, bodyWrap)) return
        const row = defectList[rowIndex]
        if (!row || row.defectId == null) return
        const defectId = String(row.defectId)
        if (seen.has(defectId)) return
        seen.add(defectId)
        const anchor = resolveDefectTableRowHintAnchor(tr)
        if (!anchor) return
        anchors.push({
          anchor,
          getAnchorRect: () => resolveDefectTableRowHintPositionRect(tr, tableRoot),
          skipViewportCheck: true,
          run: () => this.handleDefectClick({ defectId: row.defectId })
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
    buildDefectExcelRowActionHints(usedLetters, rowFloat) {
      const content = this.$refs.defectContentComponent
      const tc = this.getDefectExcelTableContent(content)
      if (!tc) return []
      const sheetRows = content.sheetRows || []
      const seen = new Set()
      const anchors = []
      tc.querySelectorAll('tbody tr').forEach((tr) => {
        if (!isExcelRowVisibleInViewport(tr, tc)) return
        const anchor = resolveExcelRowHintAnchor(tr)
        if (!anchor) return
        const cell = tr.querySelector('td[id$="-projectNum"]') || tr.querySelector('td[id^="id-"]')
        if (!cell || !cell.id) return
        const rowKey = parseExcelRowKeyFromTdId(cell.id)
        const row = findExcelSheetRow(sheetRows, rowKey)
        if (!row || !row.defectId) return
        const defectId = String(row.defectId)
        if (seen.has(defectId)) return
        seen.add(defectId)
        anchors.push({
          anchor,
          skipViewportCheck: true,
          run: () => this.handleDefectClick({ defectId: row.defectId })
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
    /** ⌘+方向键：表格 / Excel 水平、垂直滚动（新建下拉打开时让位给菜单导航） */
    handlePageModifierArrowScroll(e) {
      if (this.isDefectAddDropdownOpen()) {
        return false
      }
      if (this.isDefectColumnPickerOpen()) {
        return false
      }
      if (this.defectContentComponent === 'DefectTable') {
        return this.scrollDefectTableByArrow(e.key)
      }
      if (this.defectContentComponent === 'DefectExcel') {
        return this.scrollDefectExcelByArrow(e.key)
      }
      return false
    },
    scrollDefectTableByArrow(key) {
      const content = this.$refs.defectContentComponent
      const bodyWrap = this.getDefectTableScrollBody(content)
      if (!bodyWrap) return false
      const stepY = Math.max(48, Math.round(bodyWrap.clientHeight * 0.35))
      const stepX = Math.max(80, Math.round(bodyWrap.clientWidth * 0.25))
      if (key === 'ArrowUp') {
        bodyWrap.scrollTop = Math.max(0, bodyWrap.scrollTop - stepY)
        return true
      }
      if (key === 'ArrowDown') {
        bodyWrap.scrollTop = Math.min(bodyWrap.scrollHeight - bodyWrap.clientHeight, bodyWrap.scrollTop + stepY)
        return true
      }
      if (key === 'ArrowLeft') {
        bodyWrap.scrollLeft = Math.max(0, bodyWrap.scrollLeft - stepX)
        return true
      }
      if (key === 'ArrowRight') {
        bodyWrap.scrollLeft = Math.min(bodyWrap.scrollWidth - bodyWrap.clientWidth, bodyWrap.scrollLeft + stepX)
        return true
      }
      return false
    },
    scrollDefectExcelByArrow(key) {
      const content = this.$refs.defectContentComponent
      const tc = this.getDefectExcelTableContent(content)
      const ed = content && content.$refs && content.$refs.excelEditor
      if (!tc) return false
      const stepY = Math.max(48, Math.round(tc.clientHeight * 0.35))
      const stepX = Math.max(80, Math.round(tc.clientWidth * 0.25))
      if (key === 'ArrowUp') {
        tc.scrollTop = Math.max(0, tc.scrollTop - stepY)
        return true
      }
      if (key === 'ArrowDown') {
        tc.scrollTop = Math.min(tc.scrollHeight - tc.clientHeight, tc.scrollTop + stepY)
        return true
      }
      if (key === 'ArrowLeft') {
        tc.scrollLeft = Math.max(0, tc.scrollLeft - stepX)
        if (ed && typeof ed.calVScroll === 'function') {
          try { ed.calVScroll() } catch (err) { /* ignore */ }
        }
        return true
      }
      if (key === 'ArrowRight') {
        tc.scrollLeft = Math.min(tc.scrollWidth - tc.clientWidth, tc.scrollLeft + stepX)
        if (ed && typeof ed.calVScroll === 'function') {
          try { ed.calVScroll() } catch (err) { /* ignore */ }
        }
        return true
      }
      return false
    },
    /** ⌘+S 或鼠标点击：聚焦查询区，左右键在查询控件间切换 */
    shortcutFocusQuery() {
      this.enterDefectQueryKeyboardNav()
    },
    shortcutOpenDefectAddDropdown() {
      if (!this.defectAddToolbarVisible) return
      shortcutOpenSplitDropdown(this.$el, '.defect-add-dropdown.cat2bug-split-dropdown-kbd')
    },
    getDefectQueryNavIndexByKey(key) {
      return this.getDefectQueryNavItems().findIndex((item) => item.key === key)
    },
    /** 鼠标/Tab 聚焦某一查询项时同步进入键盘导航态（与 ⌘+S 行为一致） */
    ensureDefectQueryNavFromFocus(target) {
      if (!target || !this.showSearch) return
      const itemEl = target.closest && target.closest('.defect-list-query-nav-item[data-query-key]')
      if (!itemEl) return
      const form = this.$refs.queryForm && this.$refs.queryForm.$el
      if (!form || !form.contains(itemEl)) return
      const key = itemEl.getAttribute('data-query-key')
      const index = this.getDefectQueryNavIndexByKey(key)
      if (index < 0) return
      this.exitDefectTabKeyboardNav()
      this.exitDefectRightToolbarNav()
      this.exitDefectStatisticKeyboardNav()
      this.defectQueryNavActive = true
      this.defectQueryNavIndex = index
      this.syncDefectQueryNavFocusClass()
      this.$_attachDefectQueryNavListeners()
    },
    getDefectQueryNavItems() {
      return [
        { key: 'defectType' },
        { key: 'defectState' },
        { key: 'handleBy' },
        { key: 'nameVersionKeyword' }
      ]
    },
    getDefectQueryNavItemEl(key) {
      const form = this.$refs.queryForm && this.$refs.queryForm.$el
      if (!form || !key) return null
      return form.querySelector(`.defect-list-query-nav-item[data-query-key="${String(key)}"]`)
    },
    getDefectQueryNavFocusEl(key) {
      const itemEl = this.getDefectQueryNavItemEl(key)
      if (!itemEl) return null
      if (key === 'defectType') {
        const host = this.getDefectQueryNavItemEl('defectType')
        return getSplitDropdownFocusTarget(host) || host
      }
      if (key === 'defectState') {
        return itemEl.querySelector('.defect-state-select .el-input__inner') ||
          itemEl.querySelector('.el-select .el-input__inner')
      }
      if (key === 'handleBy') {
        return itemEl.querySelector('.select-project-member-input')
      }
      if (key === 'nameVersionKeyword') {
        return itemEl.querySelector('input.el-input__inner')
      }
      return itemEl.querySelector('input, button, [tabindex]')
    },
    clearDefectQueryNavFocusMarks() {
      const form = this.$refs.queryForm && this.$refs.queryForm.$el
      if (!form) return
      form.querySelectorAll('.defect-list-query-nav-item.defect-query-nav-focused').forEach((el) => {
        el.classList.remove('defect-query-nav-focused')
      })
    },
    syncDefectQueryNavFocusClass() {
      this.clearDefectQueryNavFocusMarks()
      if (!this.defectQueryNavActive || this.defectQueryNavIndex < 0) return
      const items = this.getDefectQueryNavItems()
      const item = items[this.defectQueryNavIndex]
      if (!item) return
      const itemEl = this.getDefectQueryNavItemEl(item.key)
      if (itemEl) {
        itemEl.classList.add('defect-query-nav-focused')
      }
    },
    focusDefectQueryNavDom(index) {
      const items = this.getDefectQueryNavItems()
      const item = items[index]
      if (!item) return
      if (item.key === 'handleBy') {
        const comp = this.$refs.defectQueryHandleBy
        if (comp) {
          comp.searchInputActive = false
          if (typeof comp.focus === 'function') {
            comp.focus()
          } else {
            const shell = this.getDefectQueryNavFocusEl(item.key)
            if (shell) shell.focus()
          }
          this.$nextTick(() => {
            this.syncDefectQueryNavFocusClass()
          })
          return
        }
      }
      const focusEl = this.getDefectQueryNavFocusEl(item.key)
      if (focusEl) {
        focusEl.focus()
      }
      this.$nextTick(() => {
        this.syncDefectQueryNavFocusClass()
      })
    },
    applyDefectQueryNavFocus(index) {
      const items = this.getDefectQueryNavItems()
      if (index < 0 || index >= items.length) return
      const prevIndex = this.defectQueryNavIndex
      if (prevIndex >= 0 && prevIndex !== index) {
        const prevItem = items[prevIndex]
        if (prevItem) {
          this.releaseDefectQueryNavItemFocus(prevItem.key)
        }
      }
      this.defectQueryNavSuppressBlur = true
      this.defectQueryNavIndex = index
      this.$nextTick(() => {
        this.focusDefectQueryNavDom(index)
        this.$nextTick(() => {
          this.defectQueryNavSuppressBlur = false
        })
      })
    },
    /** 离开某一查询项时关闭下拉并 blur，避免 is-focus 视觉残留 */
    releaseDefectQueryNavItemFocus(key) {
      if (!key) return
      if (key === 'defectType') {
        const dropdown = this.$refs.defectTypeDropdown
        if (dropdown && dropdown.visible && typeof dropdown.hide === 'function') {
          dropdown.hide()
        }
        const caret = this.getDefectQueryNavFocusEl('defectType')
        if (caret && typeof caret.blur === 'function') {
          caret.blur()
        }
        return
      }
      if (key === 'defectState') {
        const select = this.$refs.defectQueryStateSelect
        if (select && typeof select.blur === 'function') {
          select.blur()
        } else {
          const el = this.getDefectQueryNavFocusEl('defectState')
          if (el && typeof el.blur === 'function') {
            el.blur()
          }
        }
        return
      }
      if (key === 'handleBy') {
        const comp = this.$refs.defectQueryHandleBy
        if (comp) {
          comp.searchInputActive = false
          const inputComp = comp.$refs && comp.$refs.selectProjectMemberInput
          const inner = inputComp && inputComp.$el && inputComp.$el.querySelector('input')
          if (inner && typeof inner.blur === 'function') {
            inner.blur()
          }
        }
        const shell = this.getDefectQueryNavFocusEl('handleBy')
        if (shell && typeof shell.blur === 'function') {
          shell.blur()
        }
        return
      }
      if (key === 'nameVersionKeyword') {
        const el = this.getDefectQueryNavFocusEl('nameVersionKeyword')
        if (el && typeof el.blur === 'function') {
          el.blur()
        }
      }
    },
    enterDefectQueryKeyboardNav() {
      this.exitDefectTabKeyboardNav()
      this.exitDefectRightToolbarNav()
      this.exitDefectStatisticKeyboardNav()
      this.showSearch = true
      const items = this.getDefectQueryNavItems()
      if (!items.length) return
      const idx = items.length - 1
      this.defectQueryNavActive = true
      this.defectQueryNavIndex = idx
      this.$nextTick(() => {
        this.applyDefectQueryNavFocus(idx)
        this.$nextTick(() => {
          this.$_attachDefectQueryNavListeners()
        })
      })
    },
    exitDefectQueryKeyboardNav() {
      if (!this.defectQueryNavActive && !this.$_defectQueryNavListenersBound) return
      const items = this.getDefectQueryNavItems()
      if (this.defectQueryNavIndex >= 0 && items[this.defectQueryNavIndex]) {
        this.releaseDefectQueryNavItemFocus(items[this.defectQueryNavIndex].key)
      }
      this.defectQueryNavActive = false
      this.defectQueryNavIndex = -1
      this.clearDefectQueryNavFocusMarks()
      const form = this.$refs.queryForm && this.$refs.queryForm.$el
      if (form && form.contains(document.activeElement)) {
        document.activeElement.blur()
      }
      this.$_detachDefectQueryNavListeners()
    },
    moveDefectQueryNav(delta) {
      const items = this.getDefectQueryNavItems()
      const next = this.defectQueryNavIndex + delta
      if (next < 0 || next >= items.length) return
      this.applyDefectQueryNavFocus(next)
    },
    bridgeDefectToolbarToQuery() {
      this.exitDefectRightToolbarNav()
      const items = this.getDefectQueryNavItems()
      if (!items.length) return
      const idx = items.length - 1
      this.defectQueryNavActive = true
      this.defectQueryNavIndex = idx
      this.$nextTick(() => {
        this.applyDefectQueryNavFocus(idx)
        this.$nextTick(() => this.$_attachDefectQueryNavListeners())
      })
    },
    isDefectQueryNavItemFocused(key) {
      const itemEl = this.getDefectQueryNavItemEl(key)
      const active = document.activeElement
      return !!(itemEl && active && itemEl.contains(active))
    },
    isDefectTypeDropdownOpen() {
      const dd = this.$refs.defectTypeDropdown
      return !!(dd && dd.visible)
    },
    closeDefectTypeDropdown() {
      const dd = this.$refs.defectTypeDropdown
      if (dd && dd.$el) closeSplitDropdown(dd.$el)
    },
    openDefectTypeDropdown() {
      const dropdown = this.$refs.defectTypeDropdown
      if (!dropdown || dropdown.visible) return
      const host = dropdown.$el
      bindSplitDropdownHost(host)
      const caret = this.getDefectQueryNavFocusEl('defectType')
      if (caret && document.activeElement !== caret) {
        caret.focus()
      }
      this.defectTypeDropdownKeyboardOpen = true
      this.clearDefectQueryNavFocusMarks()
      if (typeof dropdown.show === 'function') {
        dropdown.show()
      } else if (caret) {
        caret.click()
      }
      const state = createDropdownMenuKeyboardState()
      this.$nextTick(() => {
        focusInitialDropdownMenuItem(host, dropdown, state)
        this.defectTypeDropdownKeyboardOpen = false
      })
    },
    onDefectTypeDropdownVisibleChange(visible) {
      if (visible) {
        if (this.defectTypeDropdownKeyboardOpen) {
          this.clearDefectQueryNavFocusMarks()
          this.defectTypeDropdownKeyboardOpen = false
        }
        return
      }
      this.defectTypeDropdownKeyboardOpen = false
      if (!this.defectQueryNavActive) return
      const typeIndex = this.getDefectQueryNavIndexByKey('defectType')
      if (this.defectQueryNavIndex !== typeIndex) return
      this.$nextTick(() => {
        this.syncDefectQueryNavFocusClass()
        const caret = this.getDefectQueryNavFocusEl('defectType')
        if (caret && !this.isFocusInDefectQueryOverlay(document.activeElement)) {
          caret.focus()
        }
      })
    },
    isDefectQueryNavDropdownOpen() {
      const typeDd = this.$refs.defectTypeDropdown
      if (typeDd && typeDd.visible) return true
      const stateSelect = this.$refs.defectQueryStateSelect
      if (stateSelect && stateSelect.visible) return true
      const handleBy = this.$refs.defectQueryHandleBy
      if (handleBy && handleBy.popoverVisible) return true
      return false
    },
    isFocusInDefectQueryOverlay(target) {
      if (!target) return false
      const typeDd = this.$refs.defectTypeDropdown
      if (typeDd && typeDd.visible) {
        const menu = typeDd.dropdownElm || typeDd.popperElm
        if (menu && menu.contains(target)) return true
      }
      const stateSelect = this.$refs.defectQueryStateSelect
      if (stateSelect && stateSelect.visible) {
        const popper = stateSelect.$refs.popper && stateSelect.$refs.popper.$el
        if (popper && popper.contains(target)) return true
      }
      const handleBy = this.$refs.defectQueryHandleBy
      if (handleBy && handleBy.popoverVisible) {
        const popovers = document.querySelectorAll('.select-project-member-popover')
        for (let i = 0; i < popovers.length; i++) {
          if (popovers[i].contains(target)) return true
        }
      }
      return false
    },
    $_attachDefectQueryNavListeners() {
      if (this.$_defectQueryNavListenersBound) return
      this.$_defectQueryNavListenersBound = true
      this.$_onDefectQueryNavKeydown = (e) => {
        if (e.key === 'ArrowDown' && this.isDefectQueryNavItemFocused('defectType')) {
          const typeDd = this.$refs.defectTypeDropdown
          if (typeDd && typeDd.visible) {
            return
          }
          if (!this.defectQueryNavActive) {
            this.ensureDefectQueryNavFromFocus(document.activeElement)
          } else {
            const typeIndex = this.getDefectQueryNavIndexByKey('defectType')
            if (typeIndex >= 0) {
              this.defectQueryNavIndex = typeIndex
              this.syncDefectQueryNavFocusClass()
            }
          }
          e.preventDefault()
          e.stopPropagation()
          this.openDefectTypeDropdown()
          return
        }
        if (!this.defectQueryNavActive) return
        const items = this.getDefectQueryNavItems()
        const item = items[this.defectQueryNavIndex]
        if (this.isDefectQueryNavDropdownOpen()) return
        if (e.key === 'ArrowLeft') {
          e.preventDefault()
          e.stopPropagation()
          this.moveDefectQueryNav(-1)
          return
        }
        if (e.key === 'ArrowRight') {
          e.preventDefault()
          e.stopPropagation()
          if (this.defectQueryNavIndex === items.length - 1) {
            const toolbarItems = this.getDefectRightToolbarNavItems()
            if (toolbarItems.length) {
              this.enterDefectRightToolbarNav(toolbarItems[0].key)
              return
            }
          }
          this.moveDefectQueryNav(1)
          return
        }
        if (e.key === 'Escape') {
          e.preventDefault()
          e.stopPropagation()
          this.exitDefectQueryKeyboardNav()
        }
      }
      this.$_onDefectQueryNavFocusOut = (e) => {
        if (!this.defectQueryNavActive || this.defectQueryNavSuppressBlur) return
        const form = this.$refs.queryForm && this.$refs.queryForm.$el
        if (form && e.relatedTarget && form.contains(e.relatedTarget)) return
        if (this.isFocusInDefectQueryOverlay(e.relatedTarget)) return
        setTimeout(() => {
          if (!this.defectQueryNavActive || this.defectQueryNavSuppressBlur) return
          if (this.isFocusInDefectQueryOverlay(document.activeElement)) return
          if (!form || !form.contains(document.activeElement)) {
            this.exitDefectQueryKeyboardNav()
          }
        }, 0)
      }
      document.addEventListener('keydown', this.$_onDefectQueryNavKeydown, true)
      const form = this.$refs.queryForm && this.$refs.queryForm.$el
      if (form) {
        form.addEventListener('focusout', this.$_onDefectQueryNavFocusOut, true)
      }
    },
    $_detachDefectQueryNavListeners() {
      if (!this.$_defectQueryNavListenersBound) return
      this.$_defectQueryNavListenersBound = false
      document.removeEventListener('keydown', this.$_onDefectQueryNavKeydown, true)
      const form = this.$refs.queryForm && this.$refs.queryForm.$el
      if (form && this.$_onDefectQueryNavFocusOut) {
        form.removeEventListener('focusout', this.$_onDefectQueryNavFocusOut, true)
      }
    },
    $_bindDefectQueryNavFocusIn() {
      if (this.$_defectQueryNavFocusInBound) return
      const form = this.$refs.queryForm && this.$refs.queryForm.$el
      if (!form) return
      this.$_defectQueryNavFocusInBound = true
      this.$_onDefectQueryNavFocusIn = (e) => {
        this.ensureDefectQueryNavFromFocus(e.target)
      }
      form.addEventListener('focusin', this.$_onDefectQueryNavFocusIn, true)
    },
    $_unbindDefectQueryNavFocusIn() {
      if (!this.$_defectQueryNavFocusInBound) return
      this.$_defectQueryNavFocusInBound = false
      const form = this.$refs.queryForm && this.$refs.queryForm.$el
      if (form && this.$_onDefectQueryNavFocusIn) {
        form.removeEventListener('focusin', this.$_onDefectQueryNavFocusIn, true)
      }
      this.$_onDefectQueryNavFocusIn = null
    },
    ensureDefectToolbarNavFromFocus(target) {
      if (!target) return
      const items = this.getDefectRightToolbarNavItems()
      for (let i = 0; i < items.length; i++) {
        const host = this.getDefectRightToolbarNavEl(items[i].key)
        const focusEl = this.getDefectRightToolbarFocusEl(items[i].key) || host
        if (!focusEl && !host) continue
        if ((focusEl && (focusEl === target || focusEl.contains(target))) ||
          (host && host.contains(target))) {
          this.exitDefectTabKeyboardNav()
          this.exitDefectQueryKeyboardNav()
          this.exitDefectStatisticKeyboardNav()
          this.defectToolbarNavActive = true
          this.defectToolbarNavIndex = i
          this.syncDefectRightToolbarNavFocusClass()
          this.$_attachDefectToolbarNavListeners()
          return
        }
      }
    },
    $_bindDefectToolbarNavFocusIn() {
      if (this.$_defectToolbarNavFocusInBound) return
      const root = this.$el
      if (!root) return
      this.$_defectToolbarNavFocusInBound = true
      this.$_onDefectToolbarNavFocusIn = (e) => {
        this.ensureDefectToolbarNavFromFocus(e.target)
      }
      root.addEventListener('focusin', this.$_onDefectToolbarNavFocusIn, true)
    },
    $_unbindDefectToolbarNavFocusIn() {
      if (!this.$_defectToolbarNavFocusInBound) return
      this.$_defectToolbarNavFocusInBound = false
      const root = this.$el
      if (root && this.$_onDefectToolbarNavFocusIn) {
        root.removeEventListener('focusin', this.$_onDefectToolbarNavFocusIn, true)
      }
      this.$_onDefectToolbarNavFocusIn = null
    },
    /** Cmd/Ctrl+J：直接切换到下一项（含添加按钮，不进入焦点导航） */
    shortcutSwitchTab() {
      this.exitDefectTabKeyboardNav()
      const items = this.getDefectTabNavItems()
      if (!items.length) return
      let idx
      if (this.defectShortcutTabAtAdd) {
        idx = items.findIndex((it) => it.type === 'add')
      } else {
        const cur = String(this.activeDefectTabName)
        idx = items.findIndex((it) => it.type === 'tab' && it.name === cur)
      }
      const next = items[(idx < 0 ? 0 : idx + 1) % items.length]
      if (next.type === 'add') {
        this.defectShortcutTabAtAdd = true
        this.$nextTick(() => this.syncDefectShortcutAddHighlight())
        return
      }
      this.defectShortcutTabAtAdd = false
      this.clearDefectShortcutAddHighlight()
      this.activeDefectTabName = next.name
      this.selectDefectTabHandle({ name: next.name })
    },
    syncDefectShortcutAddHighlight() {
      this.clearDefectShortcutAddHighlight()
      const addBtn = this.getDefectTabAddButtonEl()
      if (addBtn) addBtn.classList.add('defect-tab-shortcut-active')
    },
    clearDefectShortcutAddHighlight() {
      const root = this.$refs.defectToolsTab
      if (!root) return
      root.querySelectorAll('.defect-tab-add-btn.defect-tab-shortcut-active').forEach((el) => {
        el.classList.remove('defect-tab-shortcut-active')
      })
    },
    /** Cmd/Ctrl+L：打开左上角项目切换 */
    shortcutSwitchProject() {
      const label = this.$refs.defectProjectLabel
      if (label && typeof label.openProjectSelect === 'function') {
        label.openProjectSelect()
      }
    },
    /** Cmd/Ctrl+G：统计模块键盘导航（左右选择，Delete 移除） */
    shortcutStatisticNav() {
      if (!this.defectStatisticNavAvailable) return
      this.enterDefectStatisticKeyboardNav()
    },
    onDefectStatisticListChange(list) {
      this.defectStatisticItemCount = Array.isArray(list) ? list.length : 0
      if (!this.defectStatisticNavAvailable && this.defectStatisticNavActive) {
        this.exitDefectStatisticKeyboardNav()
      }
      this.registerDefectShortcuts()
    },
    syncDefectStatisticItemCount() {
      const items = this.getDefectStatisticNavItems()
      this.defectStatisticItemCount = items.length
      if (!this.defectStatisticNavAvailable && this.defectStatisticNavActive) {
        this.exitDefectStatisticKeyboardNav()
      }
    },
    getDefectStatisticNavItems() {
      const stat = this.$refs.defectStatistic
      if (!stat || !stat.list) return []
      return Array.isArray(stat.list) ? stat.list : []
    },
    getDefectStatisticItemEls() {
      const stat = this.$refs.defectStatistic
      const viewport = stat && stat.$refs && stat.$refs.viewport
      if (!viewport) return []
      return Array.from(viewport.querySelectorAll('.statistic-item'))
    },
    clearDefectStatisticNavFocusMarks() {
      const stat = this.$refs.defectStatistic
      const root = stat && stat.$el
      if (!root) return
      root.querySelectorAll('.statistic-item.defect-statistic-nav-focused').forEach((el) => {
        el.classList.remove('defect-statistic-nav-focused')
        el.tabIndex = -1
      })
    },
    syncDefectStatisticNavFocusClass() {
      this.clearDefectStatisticNavFocusMarks()
      if (!this.defectStatisticNavActive || this.defectStatisticNavIndex < 0) return
      const els = this.getDefectStatisticItemEls()
      const el = els[this.defectStatisticNavIndex]
      if (el) {
        el.classList.add('defect-statistic-nav-focused')
        el.tabIndex = 0
      }
    },
    scrollDefectStatisticItemIntoView(el) {
      if (!el || typeof el.scrollIntoView !== 'function') return
      el.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' })
    },
    focusDefectStatisticNavDom(index) {
      const els = this.getDefectStatisticItemEls()
      const el = els[index]
      if (el) {
        el.tabIndex = 0
        el.focus()
        this.scrollDefectStatisticItemIntoView(el)
      }
      this.$nextTick(() => {
        this.syncDefectStatisticNavFocusClass()
      })
    },
    applyDefectStatisticNavFocus(index) {
      const items = this.getDefectStatisticNavItems()
      if (!items.length || index < 0 || index >= items.length) return
      this.defectStatisticNavSuppressBlur = true
      this.defectStatisticNavIndex = index
      this.$nextTick(() => {
        this.focusDefectStatisticNavDom(index)
        this.$nextTick(() => {
          this.defectStatisticNavSuppressBlur = false
        })
      })
    },
    enterDefectStatisticKeyboardNav() {
      this.exitDefectTabKeyboardNav()
      this.exitDefectQueryKeyboardNav()
      this.exitDefectRightToolbarNav()
      const items = this.getDefectStatisticNavItems()
      if (!items.length) return
      this.defectStatisticNavActive = true
      this.defectStatisticNavIndex = 0
      this.$nextTick(() => {
        this.applyDefectStatisticNavFocus(0)
        this.$nextTick(() => {
          this.$_attachDefectStatisticNavListeners()
        })
      })
    },
    exitDefectStatisticKeyboardNav() {
      if (!this.defectStatisticNavActive && !this.$_defectStatisticNavListenersBound) return
      this.defectStatisticNavActive = false
      this.defectStatisticNavIndex = -1
      this.clearDefectStatisticNavFocusMarks()
      const root = this.$refs.defectStatisticWrap
      if (root && root.contains(document.activeElement)) {
        document.activeElement.blur()
      }
      this.$_detachDefectStatisticNavListeners()
    },
    moveDefectStatisticNav(delta) {
      const items = this.getDefectStatisticNavItems()
      const next = this.defectStatisticNavIndex + delta
      if (next < 0 || next >= items.length) return
      this.applyDefectStatisticNavFocus(next)
    },
    removeFocusedDefectStatistic() {
      const items = this.getDefectStatisticNavItems()
      const sc = items[this.defectStatisticNavIndex]
      const stat = this.$refs.defectStatistic
      if (!sc || !stat || typeof stat.removeStatistic !== 'function') return
      const removeIndex = this.defectStatisticNavIndex
      stat.removeStatistic(sc)
      this.$nextTick(() => {
        const nextItems = this.getDefectStatisticNavItems()
        if (!nextItems.length) {
          this.exitDefectStatisticKeyboardNav()
          return
        }
        const idx = Math.min(removeIndex, nextItems.length - 1)
        this.applyDefectStatisticNavFocus(idx)
      })
    },
    /** 空格/回车：触发当前聚焦统计模块的主点击（筛选、打开设置等） */
    activateFocusedDefectStatisticClick() {
      const els = this.getDefectStatisticItemEls()
      const el = els[this.defectStatisticNavIndex]
      const items = this.getDefectStatisticNavItems()
      const sc = items[this.defectStatisticNavIndex]
      if (!el || !sc || !sc.name) return false
      return activateStatisticItemClick(el, sc.name)
    },
    $_attachDefectStatisticNavListeners() {
      if (this.$_defectStatisticNavListenersBound) return
      this.$_defectStatisticNavListenersBound = true
      this.$_onDefectStatisticNavKeydown = (e) => {
        if (!this.defectStatisticNavActive) return
        if (findTopFormDrawerVm()) return
        if (e.key === 'ArrowLeft') {
          e.preventDefault()
          e.stopPropagation()
          this.moveDefectStatisticNav(-1)
          return
        }
        if (e.key === 'ArrowRight') {
          e.preventDefault()
          e.stopPropagation()
          this.moveDefectStatisticNav(1)
          return
        }
        const key = e.key
        if (key === 'Enter' || key === ' ' || key === 'Spacebar' || e.code === 'Space') {
          e.preventDefault()
          e.stopPropagation()
          if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation()
          this.activateFocusedDefectStatisticClick()
          return
        }
        if (e.key === 'Delete' || e.key === 'Backspace') {
          e.preventDefault()
          e.stopPropagation()
          this.removeFocusedDefectStatistic()
          return
        }
        if (e.key === 'Escape') {
          e.preventDefault()
          e.stopPropagation()
          this.exitDefectStatisticKeyboardNav()
        }
      }
      this.$_onDefectStatisticNavFocusOut = (e) => {
        if (!this.defectStatisticNavActive || this.defectStatisticNavSuppressBlur) return
        const root = this.$refs.defectStatisticWrap
        if (root && e.relatedTarget && root.contains(e.relatedTarget)) return
        setTimeout(() => {
          if (!this.defectStatisticNavActive || this.defectStatisticNavSuppressBlur) return
          if (!root || !root.contains(document.activeElement)) {
            this.exitDefectStatisticKeyboardNav()
          }
        }, 0)
      }
      document.addEventListener('keydown', this.$_onDefectStatisticNavKeydown, true)
      const root = this.$refs.defectStatisticWrap
      if (root) {
        root.addEventListener('focusout', this.$_onDefectStatisticNavFocusOut, true)
      }
    },
    $_detachDefectStatisticNavListeners() {
      if (!this.$_defectStatisticNavListenersBound) return
      this.$_defectStatisticNavListenersBound = false
      document.removeEventListener('keydown', this.$_onDefectStatisticNavKeydown, true)
      const root = this.$refs.defectStatisticWrap
      if (root && this.$_onDefectStatisticNavFocusOut) {
        root.removeEventListener('focusout', this.$_onDefectStatisticNavFocusOut, true)
      }
    },
    /** Cmd/Ctrl+E：打开新建缺陷下拉，左右键在右侧工具栏按钮间切换 */
    shortcutOpenAddMenu() {
      if (!this.defectAddToolbarVisible) return
      this.exitDefectTabKeyboardNav()
      this.exitDefectQueryKeyboardNav()
      this.exitDefectStatisticKeyboardNav()
      const items = this.getDefectRightToolbarNavItems()
      const idx = items.findIndex((it) => it.key === 'addMenu')
      if (idx < 0) return
      this.defectToolbarNavActive = true
      this.defectToolbarNavIndex = idx
      this.openDefectAddDropdown()
      this.$nextTick(() => {
        this.syncDefectRightToolbarNavFocusClass()
        this.$_attachDefectToolbarNavListeners()
      })
    },
    getDefectAddDropdownCaretEl() {
      const host = this.getDefectRightToolbarNavEl('addMenu')
      return host && host.querySelector('.el-dropdown__caret-button')
    },
    getDefectRightToolbarNavItems() {
      const items = []
      const content = this.$refs.defectContentComponent
      if (!content || !content.$el) return items
      if (this.defectContentComponent === 'DefectExcel') {
        if (content.$el.querySelector('.defect-list-hint-excel-refresh')) {
          items.push({ key: 'excelRefresh' })
        }
        if (content.$el.querySelector('.defect-list-hint-excel-columns')) {
          items.push({ key: 'excelColumns' })
        }
      } else if (this.defectContentComponent === 'DefectTable') {
        if (content.$el.querySelector('.defect-list-hint-columns')) {
          items.push({ key: 'columns' })
        }
      }
      if (this.defectAddToolbarVisible && this.$el.querySelector('.defect-list-hint-add-menu')) {
        items.push({ key: 'addMenu' })
      }
      return items
    },
    getDefectRightToolbarNavEl(key) {
      const content = this.$refs.defectContentComponent
      const root = this.$el
      if (!root || !key) return null
      if (key === 'columns') {
        return content && content.$el && content.$el.querySelector('.defect-list-hint-columns')
      }
      if (key === 'excelRefresh') {
        return content && content.$el && content.$el.querySelector('.defect-list-hint-excel-refresh')
      }
      if (key === 'excelColumns') {
        return content && content.$el && content.$el.querySelector('.defect-list-hint-excel-columns')
      }
      if (key === 'addMenu') {
        return root.querySelector('.defect-list-hint-add-menu')
      }
      return null
    },
    getDefectRightToolbarFocusEl(key) {
      const host = this.getDefectRightToolbarNavEl(key)
      if (!host) return null
      if (key === 'addMenu') {
        const host = this.getDefectRightToolbarNavEl('addMenu')
        return getSplitDropdownFocusTarget(host) ||
          host.querySelector('.el-button-group > .el-button--primary') ||
          host.querySelector('.el-button-group > .el-button:first-child')
      }
      return host
    },
    clearDefectRightToolbarNavFocusMarks() {
      const root = this.$el
      if (!root) return
      root.querySelectorAll('.defect-toolbar-nav-focused').forEach((el) => {
        el.classList.remove('defect-toolbar-nav-focused')
      })
      const content = this.$refs.defectContentComponent
      if (content && content.$el) {
        content.$el.querySelectorAll('.defect-toolbar-nav-focused').forEach((el) => {
          el.classList.remove('defect-toolbar-nav-focused')
        })
      }
    },
    syncDefectRightToolbarNavFocusClass() {
      this.clearDefectRightToolbarNavFocusMarks()
      if (!this.defectToolbarNavActive || this.defectToolbarNavIndex < 0) return
      const items = this.getDefectRightToolbarNavItems()
      const item = items[this.defectToolbarNavIndex]
      if (!item) return
      const el = this.getDefectRightToolbarFocusEl(item.key) || this.getDefectRightToolbarNavEl(item.key)
      if (el) el.classList.add('defect-toolbar-nav-focused')
    },
    isDefectAddDropdownOpen() {
      const dd = this.$refs.defectAddDropdown
      return !!(dd && dd.visible)
    },
    isFocusInDefectAddDropdownOverlay(target) {
      if (!target) return false
      const dd = this.$refs.defectAddDropdown
      if (dd && dd.visible) {
        const menu = dd.dropdownElm || dd.popperElm
        if (menu && menu.contains(target)) return true
      }
      return false
    },
    isDefectRightToolbarOnAddMenu() {
      if (!this.defectToolbarNavActive || this.defectToolbarNavIndex < 0) return false
      const items = this.getDefectRightToolbarNavItems()
      const item = items[this.defectToolbarNavIndex]
      return !!(item && item.key === 'addMenu')
    },
    openDefectAddDropdown() {
      shortcutOpenSplitDropdown(this.$el, '.defect-add-dropdown.cat2bug-split-dropdown-kbd')
    },
    closeDefectAddDropdown() {
      const dd = this.$refs.defectAddDropdown
      if (dd && dd.$el) closeSplitDropdown(dd.$el)
    },
    closeDefectTableColumnPicker() {
      const btn = this.getDefectRightToolbarNavEl('columns')
      if (!btn) return
      let vm = btn.__vue__
      while (vm) {
        if (vm.$options && vm.$options.name === 'ElPopover') {
          if (typeof vm.doClose === 'function') vm.doClose()
          vm.showPopper = false
          return
        }
        vm = vm.$parent
      }
    },
    closeDefectExcelColumnPicker() {
      const content = this.$refs.defectContentComponent
      if (content && this.defectContentComponent === 'DefectExcel') {
        content.excelToolbarColumnPickerVisible = false
      }
    },
    closeDefectColumnPicker() {
      this.closeDefectTableColumnPicker()
      this.closeDefectExcelColumnPicker()
      this.defectColumnPickerMenuIndex = 0
      this.clearDefectColumnPickerFocusMarks()
      this.$_detachDefectColumnPickerMenuListeners()
    },
    getDefectColumnPickerToolbarKey() {
      const content = this.$refs.defectContentComponent
      if (!content || !content.$el) return null
      if (this.defectContentComponent === 'DefectExcel') {
        return content.$el.querySelector('.defect-list-hint-excel-columns') ? 'excelColumns' : null
      }
      if (this.defectContentComponent === 'DefectTable') {
        return content.$el.querySelector('.defect-list-hint-columns') ? 'columns' : null
      }
      return null
    },
    getDefectColumnPickerPopoverVm() {
      const key = this.getDefectColumnPickerToolbarKey()
      if (!key) return null
      const btn = this.getDefectRightToolbarNavEl(key)
      if (!btn) return null
      let vm = btn.__vue__
      while (vm) {
        if (vm.$options && vm.$options.name === 'ElPopover') return vm
        vm = vm.$parent
      }
      return null
    },
    isDefectColumnPickerOpen() {
      if (this.defectContentComponent === 'DefectExcel') {
        const content = this.$refs.defectContentComponent
        return !!(content && content.excelToolbarColumnPickerVisible)
      }
      const pop = this.getDefectColumnPickerPopoverVm()
      return !!(pop && pop.showPopper)
    },
    getDefectColumnPickerPopperEl() {
      if (this.defectContentComponent === 'DefectExcel') {
        return document.querySelector('.defect-excel-column-picker-popover')
      }
      const byClass = document.querySelector('.defect-column-picker-popover')
      if (byClass) return byClass
      const pop = this.getDefectColumnPickerPopoverVm()
      return (pop && pop.$refs && pop.$refs.popper) || null
    },
    getDefectColumnPickerCheckboxEls() {
      const popper = this.getDefectColumnPickerPopperEl()
      if (!popper) return []
      return Array.from(popper.querySelectorAll('.defect-column-picker .el-checkbox:not(.is-disabled)'))
    },
    isFocusInDefectColumnPickerOverlay(target) {
      if (!target) return false
      const popper = this.getDefectColumnPickerPopperEl()
      return !!(popper && popper.contains(target))
    },
    isDefectRightToolbarOnColumnPicker() {
      if (!this.defectToolbarNavActive || this.defectToolbarNavIndex < 0) return false
      const items = this.getDefectRightToolbarNavItems()
      const item = items[this.defectToolbarNavIndex]
      return !!(item && (item.key === 'columns' || item.key === 'excelColumns'))
    },
    clearDefectColumnPickerFocusMarks() {
      const popper = this.getDefectColumnPickerPopperEl()
      if (!popper) return
      popper.querySelectorAll('.el-checkbox.defect-column-picker-item-focused').forEach((el) => {
        el.classList.remove('defect-column-picker-item-focused')
      })
    },
    syncDefectColumnPickerFocusClass() {
      this.clearDefectColumnPickerFocusMarks()
      const items = this.getDefectColumnPickerCheckboxEls()
      if (!items.length) return
      const idx = this.getDefectColumnPickerMenuIndex()
      const el = items[idx]
      if (el) el.classList.add('defect-column-picker-item-focused')
    },
    focusDefectColumnPickerItem(index = 0) {
      const items = this.getDefectColumnPickerCheckboxEls()
      if (!items.length) return false
      const i = Math.max(0, Math.min(index, items.length - 1))
      const el = items[i]
      el.setAttribute('tabindex', '0')
      el.focus()
      if (typeof el.scrollIntoView === 'function') {
        el.scrollIntoView({ block: 'nearest' })
      }
      this.defectColumnPickerMenuIndex = i
      this.syncDefectColumnPickerFocusClass()
      return true
    },
    getDefectColumnPickerMenuIndex() {
      const items = this.getDefectColumnPickerCheckboxEls()
      if (!items.length) return 0
      const active = document.activeElement
      let node = active
      while (node) {
        const idx = items.indexOf(node)
        if (idx >= 0) return idx
        node = node.parentElement
      }
      return Math.min(this.defectColumnPickerMenuIndex || 0, items.length - 1)
    },
    moveDefectColumnPickerItem(delta) {
      const items = this.getDefectColumnPickerCheckboxEls()
      if (!items.length) return false
      const cur = this.getDefectColumnPickerMenuIndex()
      const next = cur + delta
      if (next < 0 || next >= items.length) return false
      return this.focusDefectColumnPickerItem(next)
    },
    toggleDefectColumnPickerItem(index) {
      const items = this.getDefectColumnPickerCheckboxEls()
      if (!items[index]) return
      const input = items[index].querySelector('input[type="checkbox"]')
      if (input) {
        input.click()
      } else {
        items[index].click()
      }
      this.$nextTick(() => {
        this.focusDefectColumnPickerItem(index)
      })
    },
    openDefectColumnPicker() {
      if (this.isDefectColumnPickerOpen()) {
        this.focusDefectColumnPickerItem(0)
        this.$_attachDefectColumnPickerMenuListeners()
        return
      }
      this.defectColumnPickerKeyboardOpen = true
      const key = this.getDefectColumnPickerToolbarKey()
      if (key === 'excelColumns') {
        const content = this.$refs.defectContentComponent
        if (content) content.excelToolbarColumnPickerVisible = true
      } else if (key === 'columns') {
        const btn = this.getDefectRightToolbarNavEl('columns')
        let vm = btn && btn.__vue__
        while (vm) {
          if (vm.$options && vm.$options.name === 'ElPopover') {
            if (typeof vm.doShow === 'function') vm.doShow()
            else vm.showPopper = true
            break
          }
          vm = vm.$parent
        }
      }
      const tryFocus = (attempt = 0) => {
        if (this.focusDefectColumnPickerItem(0)) {
          this.defectColumnPickerKeyboardOpen = false
          this.$_attachDefectColumnPickerMenuListeners()
          return
        }
        if (attempt < 8) {
          setTimeout(() => tryFocus(attempt + 1), 40)
        } else {
          this.defectColumnPickerKeyboardOpen = false
          if (this.isDefectColumnPickerOpen()) {
            this.$_attachDefectColumnPickerMenuListeners()
          }
        }
      }
      this.$nextTick(() => tryFocus())
    },
    isDefectRightToolbarOverlayOpen() {
      if (this.isDefectAddDropdownOpen()) return true
      if (this.isDefectColumnPickerOpen()) return true
      return false
    },
    releaseDefectRightToolbarFocus(key) {
      if (!key) return
      if (key === 'addMenu') this.closeDefectAddDropdown()
      if (key === 'columns' || key === 'excelColumns') this.closeDefectColumnPicker()
      const el = this.getDefectRightToolbarFocusEl(key)
      if (el && typeof el.blur === 'function') el.blur()
    },
    focusDefectRightToolbarDom(index) {
      const items = this.getDefectRightToolbarNavItems()
      const item = items[index]
      if (!item) return
      const focusEl = this.getDefectRightToolbarFocusEl(item.key)
      if (focusEl && typeof focusEl.focus === 'function') {
        focusEl.focus()
      }
      this.$nextTick(() => {
        this.syncDefectRightToolbarNavFocusClass()
      })
    },
    applyDefectRightToolbarNavFocus(index, opts = {}) {
      const items = this.getDefectRightToolbarNavItems()
      if (index < 0 || index >= items.length) return
      const prevIndex = this.defectToolbarNavIndex
      if (prevIndex >= 0 && prevIndex !== index) {
        const prevItem = items[prevIndex]
        if (prevItem) this.releaseDefectRightToolbarFocus(prevItem.key)
      }
      this.defectToolbarNavSuppressBlur = true
      this.defectToolbarNavIndex = index
      const item = items[index]
      this.$nextTick(() => {
        if (item && item.key === 'addMenu' && opts.openAddMenu) {
          this.openDefectAddDropdown()
        } else if (item && (item.key === 'columns' || item.key === 'excelColumns') && opts.openColumnPicker) {
          this.openDefectColumnPicker()
        } else {
          this.focusDefectRightToolbarDom(index)
        }
        this.$nextTick(() => {
          this.defectToolbarNavSuppressBlur = false
        })
      })
    },
    enterDefectRightToolbarNav(focusKey, opts = {}) {
      this.exitDefectTabKeyboardNav()
      this.exitDefectQueryKeyboardNav()
      this.exitDefectStatisticKeyboardNav()
      const items = this.getDefectRightToolbarNavItems()
      if (!items.length) return
      let idx = items.findIndex((it) => it.key === focusKey)
      if (idx < 0) idx = items.length - 1
      this.defectToolbarNavActive = true
      this.defectToolbarNavIndex = idx
      this.$nextTick(() => {
        this.applyDefectRightToolbarNavFocus(idx, opts)
        this.$nextTick(() => {
          this.$_attachDefectToolbarNavListeners()
        })
      })
    },
    exitDefectRightToolbarNav() {
      if (!this.defectToolbarNavActive && !this.$_defectToolbarNavListenersBound) return
      const items = this.getDefectRightToolbarNavItems()
      if (this.defectToolbarNavIndex >= 0 && items[this.defectToolbarNavIndex]) {
        this.releaseDefectRightToolbarFocus(items[this.defectToolbarNavIndex].key)
      }
      this.defectToolbarNavActive = false
      this.defectToolbarNavIndex = -1
      this.clearDefectRightToolbarNavFocusMarks()
      this.$_detachDefectToolbarNavListeners()
    },
    moveDefectRightToolbarNav(delta) {
      const items = this.getDefectRightToolbarNavItems()
      const next = this.defectToolbarNavIndex + delta
      if (next < 0 || next >= items.length) return
      this.applyDefectRightToolbarNavFocus(next)
    },
    onDefectColumnPickerVisibleChange(visible) {
      if (visible) {
        this.$_attachDefectColumnPickerMenuListeners()
        if (this.defectColumnPickerKeyboardOpen || this.defectToolbarNavActive) {
          this.$nextTick(() => {
            if (!this.focusDefectColumnPickerItem(0)) {
              setTimeout(() => this.focusDefectColumnPickerItem(0), 60)
            }
            this.defectColumnPickerKeyboardOpen = false
          })
        }
        return
      }
      this.defectColumnPickerKeyboardOpen = false
      this.defectColumnPickerMenuIndex = 0
      this.clearDefectColumnPickerFocusMarks()
      this.$_detachDefectColumnPickerMenuListeners()
      if (!this.defectToolbarNavActive) return
      const key = this.getDefectColumnPickerToolbarKey()
      this.$nextTick(() => {
        this.syncDefectRightToolbarNavFocusClass()
        const btn = key && this.getDefectRightToolbarFocusEl(key)
        if (btn && !this.isFocusInDefectColumnPickerOverlay(document.activeElement)) {
          btn.focus()
        }
      })
    },
    $_attachDefectColumnPickerMenuListeners() {
      if (this.$_defectColumnPickerMenuListenersBound) return
      this.$_defectColumnPickerMenuListenersBound = true
      this.$_onDefectColumnPickerMenuKeydown = (e) => {
        if (!this.isDefectColumnPickerOpen()) return
        const key = e.key
        const focusedIn = this.isFocusInDefectColumnPickerOverlay(document.activeElement)
        if (key === 'ArrowDown' || key === 'Down') {
          e.preventDefault()
          e.stopPropagation()
          if (!focusedIn) {
            this.focusDefectColumnPickerItem(0)
          } else if (!this.moveDefectColumnPickerItem(1)) {
            this.focusDefectColumnPickerItem(this.getDefectColumnPickerCheckboxEls().length - 1)
          }
          return
        }
        if (key === 'ArrowUp' || key === 'Up') {
          e.preventDefault()
          e.stopPropagation()
          if (!focusedIn) {
            this.focusDefectColumnPickerItem(0)
          } else if (this.getDefectColumnPickerMenuIndex() <= 0) {
            this.closeDefectColumnPicker()
          } else {
            this.moveDefectColumnPickerItem(-1)
          }
          return
        }
        if (key === ' ' || key === 'Spacebar' || e.code === 'Space' || key === 'Enter') {
          e.preventDefault()
          e.stopPropagation()
          this.toggleDefectColumnPickerItem(this.getDefectColumnPickerMenuIndex())
          return
        }
        if (key === 'Escape' || key === 'Esc') {
          e.preventDefault()
          e.stopPropagation()
          this.closeDefectColumnPicker()
        }
      }
      document.addEventListener('keydown', this.$_onDefectColumnPickerMenuKeydown, true)
    },
    $_detachDefectColumnPickerMenuListeners() {
      if (!this.$_defectColumnPickerMenuListenersBound) return
      this.$_defectColumnPickerMenuListenersBound = false
      document.removeEventListener('keydown', this.$_onDefectColumnPickerMenuKeydown, true)
      this.$_onDefectColumnPickerMenuKeydown = null
    },
    onDefectAddDropdownVisibleChange(visible) {
      if (!visible && this.defectToolbarNavActive) {
        this.$nextTick(() => {
          this.syncDefectRightToolbarNavFocusClass()
          const btn = this.getDefectRightToolbarFocusEl('addMenu')
          if (btn && !this.isFocusInDefectAddDropdownOverlay(document.activeElement)) {
            btn.focus()
          }
        })
      }
    },
    $_attachDefectToolbarNavListeners() {
      if (this.$_defectToolbarNavListenersBound) return
      this.$_defectToolbarNavListenersBound = true
      this.$_onDefectToolbarNavKeydown = (e) => {
        if (!this.defectToolbarNavActive) return
        if (this.isDefectAddDropdownOpen()) {
          return
        }
        if (this.isDefectColumnPickerOpen()) {
          return
        }
        if (e.key === 'ArrowDown' && this.isDefectRightToolbarOnAddMenu()) {
          e.preventDefault()
          e.stopPropagation()
          this.openDefectAddDropdown()
          return
        }
        if (e.key === 'ArrowDown' && this.isDefectRightToolbarOnColumnPicker()) {
          e.preventDefault()
          e.stopPropagation()
          this.openDefectColumnPicker()
          return
        }
        if (e.key === 'ArrowLeft') {
          e.preventDefault()
          e.stopPropagation()
          if (this.defectToolbarNavIndex === 0) {
            this.bridgeDefectToolbarToQuery()
            return
          }
          if (this.isDefectRightToolbarOverlayOpen()) {
            this.closeDefectAddDropdown()
            this.closeDefectColumnPicker()
          }
          this.moveDefectRightToolbarNav(-1)
          return
        }
        if (e.key === 'ArrowRight') {
          e.preventDefault()
          e.stopPropagation()
          if (this.isDefectRightToolbarOverlayOpen()) {
            this.closeDefectAddDropdown()
            this.closeDefectColumnPicker()
          }
          this.moveDefectRightToolbarNav(1)
          return
        }
        if (e.key === 'Escape') {
          e.preventDefault()
          e.stopPropagation()
          if (this.isDefectColumnPickerOpen()) {
            this.closeDefectColumnPicker()
            return
          }
          if (this.isDefectAddDropdownOpen()) {
            this.closeDefectAddDropdown()
            return
          }
          this.exitDefectRightToolbarNav()
        }
      }
      this.$_onDefectToolbarNavFocusOut = (e) => {
        if (!this.defectToolbarNavActive || this.defectToolbarNavSuppressBlur) return
        const within = this.isFocusInDefectRightToolbar(e.relatedTarget)
        if (within) return
        if (this.isDefectRightToolbarOverlayOpen()) return
        setTimeout(() => {
          if (!this.defectToolbarNavActive || this.defectToolbarNavSuppressBlur) return
          if (this.isDefectRightToolbarOverlayOpen()) return
          if (!this.isFocusInDefectRightToolbar(document.activeElement)) {
            this.exitDefectRightToolbarNav()
          }
        }, 0)
      }
      document.addEventListener('keydown', this.$_onDefectToolbarNavKeydown, true)
      const root = this.$el
      if (root) {
        root.addEventListener('focusout', this.$_onDefectToolbarNavFocusOut, true)
      }
      const content = this.$refs.defectContentComponent
      if (content && content.$el) {
        content.$el.addEventListener('focusout', this.$_onDefectToolbarNavFocusOut, true)
      }
    },
    $_detachDefectToolbarNavListeners() {
      if (!this.$_defectToolbarNavListenersBound) return
      this.$_defectToolbarNavListenersBound = false
      document.removeEventListener('keydown', this.$_onDefectToolbarNavKeydown, true)
      const root = this.$el
      if (root && this.$_onDefectToolbarNavFocusOut) {
        root.removeEventListener('focusout', this.$_onDefectToolbarNavFocusOut, true)
      }
      const content = this.$refs.defectContentComponent
      if (content && content.$el && this.$_onDefectToolbarNavFocusOut) {
        content.$el.removeEventListener('focusout', this.$_onDefectToolbarNavFocusOut, true)
      }
    },
    isFocusInDefectRightToolbar(target) {
      if (!target) return false
      if (this.isFocusInDefectAddDropdownOverlay(target)) return true
      if (this.isFocusInDefectColumnPickerOverlay(target)) return true
      const items = this.getDefectRightToolbarNavItems()
      for (let i = 0; i < items.length; i++) {
        const host = this.getDefectRightToolbarNavEl(items[i].key)
        if (host && host.contains(target)) return true
      }
      return false
    },
    /** Cmd/Ctrl+D：显示/隐藏左侧交付物列表（仅表格视图） */
    shortcutToggleModuleTree() {
      if (this.defectContentComponent !== 'DefectTable') return
      const content = this.$refs.defectContentComponent
      if (content && typeof content.toggleModuleTreeVisible === 'function') {
        content.toggleModuleTreeVisible()
      }
    },
    /** Cmd/Ctrl+O：在表格 / Excel 显示模式间切换 */
    shortcutSwitchView() {
      const next = this.defectContentComponent === 'DefectExcel' ? 'DefectTable' : 'DefectExcel'
      this.defectContentComponent = next
      this.handleDefectContentChange()
    },
    getDefectTabNavItems() {
      const items = []
      if (this.config && this.config.tabs) {
        this.config.tabs.forEach((t) => {
          items.push({ type: 'tab', name: String(t.tabId) })
        })
      }
      items.push({ type: 'tab', name: this.allTab })
      items.push({ type: 'tab', name: this.deletedTab })
      items.push({ type: 'add' })
      return items
    },
    getDefectTabItemEl(name) {
      const root = this.$refs.defectToolsTab
      if (!root || name == null) return null
      return root.querySelector(`[id="tab-${String(name)}"]`)
    },
    getDefectTabAddButtonEl() {
      const root = this.$refs.defectToolsTab
      return root && root.querySelector(`#tab-${this.defectAddTabPaneName} .defect-tab-add-btn`)
    },
    clearDefectTabNavFocusMarks() {
      const root = this.$refs.defectToolsTab
      if (!root) return
      root.querySelectorAll('.el-tabs__item.defect-tab-nav-focused').forEach((el) => {
        el.classList.remove('defect-tab-nav-focused')
      })
      root.querySelectorAll('.defect-tab-add-btn.defect-tab-nav-focused').forEach((el) => {
        el.classList.remove('defect-tab-nav-focused')
      })
      root.querySelectorAll('.el-tabs__item[tabindex="0"]').forEach((el) => {
        el.tabIndex = -1
      })
      root.querySelectorAll('.defect-tab-add-btn[tabindex="0"]').forEach((el) => {
        el.tabIndex = -1
      })
    },
    syncDefectTabNavItemFocusClass() {
      this.clearDefectTabNavFocusMarks()
      if (!this.defectTabNavActive || !this.defectTabNavFocusedName) return
      if (this.defectTabNavFocusedName === this.defectAddTabPaneName) {
        const addBtn = this.getDefectTabAddButtonEl()
        if (addBtn) {
          addBtn.classList.add('defect-tab-nav-focused')
          addBtn.tabIndex = 0
        }
        return
      }
      const tabEl = this.getDefectTabItemEl(this.defectTabNavFocusedName)
      if (tabEl) {
        tabEl.classList.add('defect-tab-nav-focused')
        tabEl.tabIndex = 0
      }
    },
    getDefectTabNavFocusEl(item) {
      if (!item) return null
      if (item.type === 'add') {
        return this.getDefectTabAddButtonEl()
      }
      return this.getDefectTabItemEl(item.name)
    },
    focusDefectTabNavDom(item) {
      const focusEl = this.getDefectTabNavFocusEl(item)
      if (focusEl) {
        focusEl.tabIndex = 0
        focusEl.focus()
      }
      this.$nextTick(() => {
        this.syncDefectTabNavItemFocusClass()
      })
    },
    applyDefectTabNavFocus(index, options = {}) {
      const activate = options.activate !== false
      const items = this.getDefectTabNavItems()
      const item = items[index]
      if (!item) return

      this.defectTabNavSuppressBlur = true
      this.defectTabNavIndex = index
      this.defectTabNavFocusedName = item.type === 'add'
        ? this.defectAddTabPaneName
        : item.name

      if (item.type === 'add') {
        this.$nextTick(() => {
          this.focusDefectTabNavDom(item)
          this.$nextTick(() => {
            this.defectTabNavSuppressBlur = false
          })
        })
        return
      }

      const activateTab = activate && String(this.activeDefectTabName) !== item.name
      if (activateTab) {
        this.activeDefectTabName = item.name
        this.selectDefectTabHandle({ name: item.name })
      }
      this.$nextTick(() => {
        this.$nextTick(() => {
          this.focusDefectTabNavDom(item)
          this.defectTabNavSuppressBlur = false
        })
      })
    },
    enterDefectTabKeyboardNav() {
      this.exitDefectQueryKeyboardNav()
      this.exitDefectRightToolbarNav()
      this.exitDefectStatisticKeyboardNav()
      const items = this.getDefectTabNavItems()
      if (!items.length) return
      const cur = String(this.activeDefectTabName)
      let idx = items.findIndex((it) => it.type === 'tab' && it.name === cur)
      if (idx < 0) idx = 0
      this.defectTabNavActive = true
      this.defectTabNavIndex = idx
      this.$nextTick(() => {
        this.applyDefectTabNavFocus(idx, { activate: false })
        this.$nextTick(() => {
          this.$_attachDefectTabNavListeners()
        })
      })
    },
    exitDefectTabKeyboardNav() {
      if (!this.defectTabNavActive && !this.$_defectTabNavListenersBound) return
      this.defectShortcutTabAtAdd = false
      this.clearDefectShortcutAddHighlight()
      this.defectTabNavActive = false
      this.defectTabNavIndex = -1
      this.defectTabNavFocusedName = null
      this.clearDefectTabNavFocusMarks()
      const root = this.$refs.defectToolsTab
      if (root && root.contains(document.activeElement)) {
        document.activeElement.blur()
      }
      this.$_detachDefectTabNavListeners()
    },
    moveDefectTabNav(delta) {
      const items = this.getDefectTabNavItems()
      const next = this.defectTabNavIndex + delta
      if (next < 0 || next >= items.length) return
      this.applyDefectTabNavFocus(next, { activate: true })
    },
    $_attachDefectTabNavListeners() {
      if (this.$_defectTabNavListenersBound) return
      this.$_defectTabNavListenersBound = true
      this.$_onDefectTabNavKeydown = (e) => {
        if (!this.defectTabNavActive) return
        if (e.key === 'ArrowLeft') {
          e.preventDefault()
          e.stopPropagation()
          this.moveDefectTabNav(-1)
          return
        }
        if (e.key === 'ArrowRight') {
          e.preventDefault()
          e.stopPropagation()
          this.moveDefectTabNav(1)
          return
        }
        if (e.key === 'Enter') {
          const items = this.getDefectTabNavItems()
          const item = items[this.defectTabNavIndex]
          if (item && item.type === 'add') {
            e.preventDefault()
            e.stopPropagation()
            this.addDefectTabHandle()
            this.exitDefectTabKeyboardNav()
          }
          return
        }
        if (e.key === 'Escape') {
          e.preventDefault()
          e.stopPropagation()
          this.exitDefectTabKeyboardNav()
        }
      }
      this.$_onDefectTabNavFocusOut = (e) => {
        if (!this.defectTabNavActive || this.defectTabNavSuppressBlur) return
        const root = this.$refs.defectToolsTab
        if (root && e.relatedTarget && root.contains(e.relatedTarget)) return
        setTimeout(() => {
          if (!this.defectTabNavActive || this.defectTabNavSuppressBlur) return
          if (!root || !root.contains(document.activeElement)) {
            this.exitDefectTabKeyboardNav()
          }
        }, 0)
      }
      document.addEventListener('keydown', this.$_onDefectTabNavKeydown, true)
      const root = this.$refs.defectToolsTab
      if (root) {
        root.addEventListener('focusout', this.$_onDefectTabNavFocusOut, true)
      }
    },
    $_detachDefectTabNavListeners() {
      if (!this.$_defectTabNavListenersBound) return
      this.$_defectTabNavListenersBound = false
      document.removeEventListener('keydown', this.$_onDefectTabNavKeydown, true)
      const root = this.$refs.defectToolsTab
      if (root && this.$_onDefectTabNavFocusOut) {
        root.removeEventListener('focusout', this.$_onDefectTabNavFocusOut, true)
      }
    },
    /** 翻页（delta: -1 上一页 / 1 下一页） */
    shortcutChangePage(delta) {
      const cur = this.queryParams.pageNum || 1
      const target = Math.max(1, cur + delta)
      if (target === cur && delta < 0) return
      this.queryParams.pageNum = target
      this.handleRefreshQuery()
    },
    /**
     * 统一列表查询入口：通用条件与扩展条件分离，避免扩展键在统计块切换时残留。
     * @param {Object} [options]
     * @param {Object} [options.common] 合并进 queryParams 的通用字段
     * @param {Object} [options.extension] 写入 queryParams.params 的扩展字段
     * @param {boolean} [options.stack=true] false 时整页重建（我的参与专用）
     */
    searchQuery({ common, extension, stack = true } = {}) {
      if (!stack) {
        this.reset()
        this.activeDefectTypeName = 'defect.all-type'
        if (hasParticipationExtension(extension) && this.activeDefectTabName !== this.allTab) {
          this.activeDefectTabName = this.allTab
          this.$cache.local.set(DEFECT_TAB_CACHE_KEY, this.allTab)
        }
        clearExtensionParams(this.queryParams, this)
        if (common) {
          this._setProperty(this.queryParams, common)
        }
        this.applyQueryExtension(extension)
        this.handleQuery()
        return
      }
      clearExtensionParams(this.queryParams, this)
      if (common) {
        this._setProperty(this.queryParams, common)
      }
      this.applyQueryExtension(extension)
      this.handleQuery()
    },
    applyQueryExtension(extension) {
      if (!extension) {
        return
      }
      if (!this.queryParams.params) {
        this.$set(this.queryParams, 'params', {})
      }
      Object.keys(extension).forEach(key => {
        this.$set(this.queryParams.params, key, extension[key])
      })
    },
    /** Tab config.customFieldFilters → queryParams.params.customFieldFilters */
    applyTabCustomFieldFilters(tabConfig) {
      if (!tabConfig || !Array.isArray(tabConfig.customFieldFilters) || !tabConfig.customFieldFilters.length) {
        return
      }
      if (!this.queryParams.params) {
        this.$set(this.queryParams, 'params', {})
      }
      this.$set(this.queryParams.params, 'customFieldFilters', tabConfig.customFieldFilters)
    },
    /** 按当前 Tab 恢复基准查询并清除扩展参数（工具栏无独立重置按钮时，类型下拉主按钮等同此逻辑） */
    resetQueryByCurrentTab() {
      clearExtensionParams(this.queryParams, this)
      this.selectDefectTabHandle({ name: this.activeDefectTabName })
    },
    /** @deprecated 请使用 searchQuery({ common: params }) */
    search(params) {
      this.searchQuery({ common: params })
    },
    /** 热力图点击：委托 searchQuery，不叠加其它条件 */
    searchByParticipation(participationLogDate, participationUserId) {
      this.searchQuery({
        stack: false,
        extension: { participationLogDate, participationUserId },
        common: {
          params: { defectStates: [], delFlag: '0' }
        }
      })
    },
    /** 设置查询属性 */
    _setProperty(parent, obj) {
      if (obj && Array.isArray(obj)) {
        parent = obj
        return parent
      } else if (obj && typeof obj === 'object') {
        for (const key in obj) {
          if (parent[key] && Array.isArray(obj[key])) {
            this.$set(parent, key, obj[key])
          } else if (parent[key] && typeof obj[key] === 'object') {
            this.$set(parent, key, this._setProperty(parent[key], obj[key]))
          } else {
            this.$set(parent, key, obj[key])
          }
        }
        return parent
      } else {
        return obj
      }
    },
    /** 获取当前用户缺陷配置 */
    getDefectConfig() {
      configDefect().then(res => {
        this.config = res.data
        // 如果页面传的参数有tab，代表有临时查询
        const tab = getDefectTempTab() // 获取临时缓存tab
        if (tab) {
          removeDefectTempTab()
          this.config.tabs = this.config.tabs || []
          this.config.tabs.unshift(tab)
          this.activeDefectTabName = tab.tabId + ''
        } else if (this.config.tabs) {
          let cachedTab = this.$cache.local.get(DEFECT_TAB_CACHE_KEY)
          if (cachedTab === this.deletedTab) {
            cachedTab = this.allTab
            this.$cache.local.set(DEFECT_TAB_CACHE_KEY, this.allTab)
          }
          const resolved = resolveDefectTabFromCache(cachedTab)
          if (resolved) {
            this.activeDefectTabName = resolved
          }
          // 查看所有页标签里是否保护激活页标签，如果没有，设置页标签为"全部"
          if (!this.activeDefectTabName ||
            this.activeDefectTabName === this.defectAddTabPaneName ||
            (this.activeDefectTabName !== this.allTab &&
              this.activeDefectTabName !== this.deletedTab &&
              this.config.tabs.filter(t => t.tabId + '' == this.activeDefectTabName).length == 0)) {
            this.activeDefectTabName = this.allTab
          }
        }
        // 执行激活页标签方法
        this.selectDefectTabHandle()
      })
    },
    /** 查找缺陷状态改变的处理 */
    defectTypeChangeHandle(defectType) {
      if (defectType) {
        this.activeDefectTypeName = defectType
      } else {
        this.activeDefectTypeName = 'defect.all-type'
      }
      this.queryParams.defectType = defectType
      this.handleQuery()
    },
    /** 打开缺陷浏览的处理 */
    handleDefectClick(defect) {
      this.$refs.editDefectForm.open(defect.defectId)
    },
    /** 获取项目id */
    getProjectId() {
      return this.$route.query.projectId ? parseInt(this.$route.query.projectId) : parseInt(this.$store.state.user.config.currentProjectId)
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.queryParams.pageSize = 10
      this.handleRefreshQuery()
    },
    /** 搜索操作 */
    handleRefreshQuery() {
      this.queryParams.projectId = this.projectId
      this.$refs.defectContentComponent.search(this.queryParams)
      const statistic = this.$refs.defectStatistic
      if (statistic && typeof statistic.refreshData === 'function') {
        statistic.refreshData()
      }
      this.$forceUpdate()
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.$refs.addDefectForm.open()
    },
    /** 添加统计操作 */
    addStatisticHandle() {
      this.$router.push({ name: 'DefectStatisticTemplate' })
    },
    /** 统计显示切换操作 */
    statisticPanelHandle() {
      this.statisticPanelVisible = !this.statisticPanelVisible
      this.$cache.local.set(CACHE_KEY_STATISTIC_PANEL_VISIBLE, this.statisticPanelVisible + '')
    },
    /** 重制查询条件 */
    reset() {
      this.queryParams = {
        pageNum: 1,
        pageSize: 10,
        orderByColumn: 'updateTime',
        isAsc: 'desc',
        defectType: null,
        defectName: null,
        nameVersionKeyword: null,
        projectId: this.projectId,
        testPlanId: null,
        caseId: null,
        dataSources: null,
        dataSourcesParams: null,
        moduleId: null,
        moduleVersion: null,
        createBy: null,
        updateTime: null,
        createTime: null,
        updateBy: null,
        defectState: null,
        handleBy: null,
        handleTime: null,
        defectLevel: null,
        params: {
          defectStates: [],
          delFlag: '0'
        }
      }
    },
    /** 切换缺陷内容组件改变的处理 */
    handleDefectContentChange() {
      const c = this.defectContentComponent
      if (DEFECT_CONTENT_VIEW_ALLOWED.includes(c)) {
        this.$cache.local.set(DEFECT_CONTENT_VIEW_CACHE_KEY, c)
      }
      this.$nextTick(() => {
        const child = this.$refs.defectContentComponent
        child.init()
        child.search(this.queryParams)
        if (child && typeof child.setDragComponentSize === 'function') {
          child.setDragComponentSize()
        }
      })
    },
    /** ============================================
     * 页签处理方法
     * ============================================ /

    /** 切换页标签；tab-click 触发时 v-model 尚未更新，须用事件参数 tab.name */
    selectDefectTabHandle(tab) {
      this.defectShortcutTabAtAdd = false
      this.clearDefectShortcutAddHighlight()
      const activeName = tab && tab.name != null ? String(tab.name) : String(this.activeDefectTabName)
      if (activeName === this.defectAddTabPaneName) {
        const restoreTab = resolveDefectTabFromCache(this.$cache.local.get(DEFECT_TAB_CACHE_KEY)) || this.allTab
        this.activeDefectTabName = restoreTab
        return
      }
      clearExtensionParams(this.queryParams, this)
      if (activeName === this.allTab) {
        this.reset()
        if (!this.queryParams.params) {
          this.$set(this.queryParams, 'params', {})
        }
        this.$set(this.queryParams.params, 'delFlag', '0')
        this.scheduleTabDataLoad()
        this.$cache.local.set(DEFECT_TAB_CACHE_KEY, activeName)
        return
      }
      if (activeName === this.deletedTab) {
        this.reset()
        if (!this.queryParams.params) {
          this.$set(this.queryParams, 'params', {})
        }
        this.$set(this.queryParams.params, 'delFlag', '2')
        this.scheduleTabDataLoad()
        // 不缓存「已删除」页签，避免刷新后仍停留在此且工具栏无新建按钮
        return
      }
      if (this.config && this.config.tabs) {
        const tabItem = this.config.tabs.find(t => String(t.tabId) === activeName)
        if (tabItem && tabItem.config) {
          tabItem.config.isAsc = 'desc'
          tabItem.config.orderByColumn = 'updateTime'
          if (!tabItem.config.params) {
            tabItem.config.params = {
              defectStates: [],
              delFlag: '0'
            }
          }
          if (tabItem.config.params.delFlag == null || tabItem.config.params.delFlag === '') {
            this.$set(tabItem.config.params, 'delFlag', '0')
          }
          const tc = tabItem.config
          const tp = tc.params
          if (tp.nameVersionKeyword != null && String(tp.nameVersionKeyword).trim() !== '') {
            if (tc.nameVersionKeyword == null || String(tc.nameVersionKeyword).trim() === '') {
              tc.nameVersionKeyword = String(tp.nameVersionKeyword).trim()
            }
            this.$delete(tp, 'nameVersionKeyword')
          }
          const kwEmpty = tc.nameVersionKeyword == null || String(tc.nameVersionKeyword).trim() === ''
          const hasDn = tc.defectName != null && String(tc.defectName).trim() !== ''
          const hasMv = tc.moduleVersion != null && String(tc.moduleVersion).trim() !== ''
          if (kwEmpty && ((hasDn && !hasMv) || (!hasDn && hasMv))) {
            tc.nameVersionKeyword = hasDn ? String(tc.defectName).trim() : String(tc.moduleVersion).trim()
            tc.defectName = null
            tc.moduleVersion = null
          }
          this.queryParams = tabItem.config
          clearExtensionParams(this.queryParams, this)
          this.applyTabCustomFieldFilters(tabItem.config)
        } else {
          this.reset()
        }
        this.scheduleTabDataLoad()
      }
      this.$cache.local.set(DEFECT_TAB_CACHE_KEY, activeName)
    },
    /** Tab 快速切换时合并查询，只加载最终停留页签的数据 */
    scheduleTabDataLoad() {
      const content = this.$refs.defectContentComponent
      if (content && Object.prototype.hasOwnProperty.call(content, 'loading')) {
        content.loading = true
      }
      if (this._tabDataLoadTimer) {
        clearTimeout(this._tabDataLoadTimer)
      }
      this._tabDataLoadTimer = setTimeout(() => {
        this._tabDataLoadTimer = null
        this.handleQuery()
      }, 120)
    },
    clearTabDataLoadTimer() {
      if (this._tabDataLoadTimer) {
        clearTimeout(this._tabDataLoadTimer)
        this._tabDataLoadTimer = null
      }
    },
    /** 打开添加缺陷页签对话框 */
    addDefectTabHandle() {
      const restoreTab =
        this.activeDefectTabName !== this.defectAddTabPaneName
          ? this.activeDefectTabName
          : (resolveDefectTabFromCache(this.$cache.local.get(DEFECT_TAB_CACHE_KEY)) || this.allTab)
      this.$refs.defectTabDialog.open()
      this.$nextTick(() => {
        if (this.activeDefectTabName === this.defectAddTabPaneName) {
          this.activeDefectTabName = restoreTab
        }
      })
    },
    /** 添加页签处理 */
    tabAddHandle(tab) {
      this.config.tabs.push(tab)
      this.activeDefectTabName = tab.tabId + ''
      this.selectDefectTabHandle()
    },
    /** 移除页签处理 */
    removeDefectTabHandle(tabId) {
      if (!tabId) return
      this.$modal.confirm(
        this.$i18n.t('defect.delete-defect-tab'),
        this.$i18n.t('prompted').toString(),
        {
          confirmButtonText: i18n.t('delete').toString(),
          cancelButtonText: i18n.t('cancel').toString(),
          confirmButtonClass: 'delete-button',
          type: 'warning'
        }).then(function() {
        return delTabs(tabId)
      }).then(() => {
        let activeName = this.activeDefectTabName
        if (this.activeDefectTabName == tabId) {
          this.config.tabs.forEach((tab, index) => {
            if (tab.tabId + '' == tabId) {
              const nextTab = this.config.tabs[index + 1] || this.config.tabs[index - 1]
              if (nextTab) {
                activeName = nextTab.tabId + ''
              } else {
                activeName = ALL_TAB_NAME
              }
            }
          })
        }
        this.activeDefectTabName = activeName
        this.config.tabs = this.config.tabs.filter(t => (t.tabId + '') != tabId)
        this.selectDefectTabHandle()
        this.$modal.msgSuccess(this.$i18n.t('delete.success'))
      }).catch(() => {})
    },
    /** 打开导入缺陷对话框 */
    handleImport() {
      this.$refs.defectImportDialog.open()
    },
    /** 导出按钮操作 */
    async handleExport() {
      const host = resolveExportAssetHost()
      const payload = { ...this.queryParams }
      if (host) {
        payload.params = { ...(payload.params || {}), host }
      }
      const c = this.$refs.defectContentComponent
      const columns = await resolveDefectExportColumns({
        projectId: this.projectId,
        cache: this.$cache.local,
        tableRef: c && c.$refs ? c.$refs.cat2BugTable : null,
        excelRef: c
      })
      appendExportColumnParams(payload, columns, 'data', 'defect')
      this.download('system/defect/export', payload, `${this.$i18n.t('defect.export-file-name')}_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
<style lang="scss" scoped>
/* AppMain 首子列内占位；与用例页同级，勿对 Excel 使用 flex-end 以免顶栏下留白过大 */
.defect-route-host {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
}
/*
 * 高度与滚动：
 * - 表格 / Excel：内层 .defect-page 在 host 内 flex:1 铺满；Excel 根 height:100%，内滚动在 excel 组件。
 */
.app-container.case-body.defect-page {
  /* 与用例页 .case-body 一致：顶左右 20px，底边交给内容区 / safe-area */
  padding: 20px 20px 0;
  box-sizing: border-box;
}
.defect-page {
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  overflow: hidden;
}
/* Excel：与 defectPageRootStyle 一致，避免仅靠内联时 scoped 的 visible 参与级联 */
.defect-page.defect-page--excel-view {
  overflow: hidden;
}
.defect-page:not(.defect-page--excel-view) {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
}
.defect-page.defect-page--excel-view {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
}
.defect-page .defect-content-slot {
  flex: 0 1 auto;
  display: flex;
  flex-direction: column;
  padding-top: 0;
  box-sizing: border-box;
}
.defect-page:not(.defect-page--excel-view) .defect-content-slot:not(.defect-content-slot--excel) {
  flex: 1 1 0%;
  min-height: 0;
}
.defect-page.defect-page--excel-view .defect-project-label,
.defect-page.defect-page--excel-view .defect-tools-tab,
.defect-page.defect-page--excel-view .defect-tools-statistic-wrap {
  flex-shrink: 0;
}
.defect-page.defect-page--excel-view .defect-content-slot.defect-content-slot--excel {
  flex: 1 1 0%;
  min-height: 0;
  /* 滚动在 .defect-excel-context；此处勿 hidden，否则工具栏搜索框顶部 focus 环被裁切 */
  overflow: visible;
  display: flex;
  flex-direction: column;
}
.defect-page.defect-page--excel-view .defect-tools-search {
  overflow: visible !important;
}
.defect-page.defect-page--excel-view .defect-view-toolbar.defect-excel-tools {
  overflow: visible;
  padding-top: 2px;
  box-sizing: border-box;
}
.defect-page.defect-page--excel-view .defect-tools-search ::v-deep .defect-list-query-nav-item.defect-query-nav-focused {
  position: relative;
  z-index: 2;
  overflow: visible !important;
}
.defect-page ::v-deep h3.defect-project-label {
  margin-top: 0;
  margin-bottom: 0;
}
/*
 * 查询条：全宽保留（不再在窄屏隐藏）。宽屏与工具栏同一行时由父级 flex 让右侧先换行；
 * 表单项过多时左侧内部 flex-wrap；≤576px 为手机排布（切换+类型一行，其余与新建各占行）。
 */
.defect-tools-search {
  flex: 1 1 auto;
  min-width: 0;
  /* 勿设 width:100%：与工具栏同行时 flex 子项会按父宽 100% 占位，右侧 .table-tools 会被挤换行 */
  max-width: 100%;
  box-sizing: border-box;
  ::v-deep .el-form.el-form--inline {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    align-content: flex-start;
    row-gap: var(--cat2bug-toolbar-row-gap, 8px);
    column-gap: var(--cat2bug-toolbar-item-gap, 10px);
    /* 相对 .defect-tools-search 盒宽铺满；父级勿 width:100% 占满整行 flex */
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
  }
  ::v-deep .el-form--inline .el-form-item {
    margin-right: 0;
    margin-bottom: 0;
    display: flex;
    align-items: flex-start;
    vertical-align: top;
  }
  ::v-deep .el-form--inline .el-form-item__label {
    display: none;
  }
  ::v-deep .el-form--inline .el-form-item__content {
    display: inline-flex !important;
    align-items: flex-start !important;
    line-height: 1 !important;
    vertical-align: top !important;
    min-height: var(--cat2bug-toolbar-control-height, 32px);
  }
  @media screen and (max-width: 576px) {
    width: 100%;
    max-width: 100%;
    ::v-deep .el-form.el-form--inline {
      display: grid;
      width: 100%;
      max-width: 100%;
      /* minmax(0,1fr) 避免第二列被内容最小宽度卡住无法铺满 */
      grid-template-columns: auto minmax(0, 1fr);
      column-gap: 8px;
      row-gap: 8px;
      align-items: start;
    }
    ::v-deep .el-form--inline .el-form-item:nth-child(1) {
      grid-column: 1;
      grid-row: 1;
      align-self: start;
      align-items: flex-start;
    }
    ::v-deep .el-form--inline .el-form-item:nth-child(1) .el-form-item__content {
      line-height: 1;
      margin-top: 0;
      padding-top: 0;
    }
    ::v-deep .el-form--inline .el-form-item:nth-child(2) {
      grid-column: 2;
      grid-row: 1;
      min-width: 0;
      width: 100%;
      max-width: 100%;
      justify-self: stretch;
      display: flex;
      align-items: flex-start;
    }
    /* Element：控件在 .el-form-item__content 内，勿用 > .el-dropdown */
    ::v-deep .el-form--inline .el-form-item:nth-child(2) .el-form-item__content {
      flex: 1 1 auto;
      min-width: 0;
      width: 100%;
      max-width: 100%;
      display: block;
      margin-top: 0;
      padding-top: 0;
      line-height: 1;
    }
    ::v-deep .el-form--inline .el-form-item:nth-child(2) .el-form-item__content .el-dropdown {
      min-width: 0;
      width: 100% !important;
      max-width: 100%;
      display: block !important;
    }
    ::v-deep .el-form--inline .el-form-item:nth-child(2) .el-button-group {
      width: 100%;
      max-width: 100%;
      display: flex;
      flex: 1 1 auto;
      min-width: 0;
    }
    ::v-deep .el-form--inline .el-form-item:nth-child(2) .el-button:first-child {
      flex: 1 1 auto;
      min-width: 0;
    }
    ::v-deep .el-form--inline .el-form-item:nth-child(2) .el-button:last-child {
      flex-shrink: 0;
    }
    ::v-deep .el-form--inline .el-form-item:nth-child(3),
    ::v-deep .el-form--inline .el-form-item:nth-child(4),
    ::v-deep .el-form--inline .el-form-item:nth-child(5) {
      grid-column: 1 / -1;
      width: 100%;
      max-width: 100%;
      min-width: 0;
      display: flex;
      align-items: stretch;
    }
    ::v-deep .el-form--inline .el-form-item:nth-child(3) .el-form-item__content,
    ::v-deep .el-form--inline .el-form-item:nth-child(4) .el-form-item__content,
    ::v-deep .el-form--inline .el-form-item:nth-child(5) .el-form-item__content {
      display: block;
      width: 100%;
      max-width: 100%;
      min-width: 0;
      box-sizing: border-box;
    }
    ::v-deep .el-form--inline .el-form-item:nth-child(3) .el-form-item__content > *,
    ::v-deep .el-form--inline .el-form-item:nth-child(4) .el-form-item__content > *,
    ::v-deep .el-form--inline .el-form-item:nth-child(5) .el-form-item__content > * {
      display: block;
      min-width: 0;
      width: 100% !important;
      max-width: 100%;
      box-sizing: border-box;
    }
    ::v-deep .el-form--inline .el-form-item:nth-child(3) .el-select,
  ::v-deep .el-form--inline .el-form-item:nth-child(3) .el-select.defect-state-select {
      display: block;
    }
    ::v-deep .el-form--inline .el-form-item:nth-child(3) .el-select,
    ::v-deep .el-form--inline .el-form-item:nth-child(3) .el-input {
      width: 100% !important;
      max-width: 100%;
    }
    ::v-deep .el-form--inline .el-form-item:nth-child(3) .el-input__inner {
      width: 100% !important;
      box-sizing: border-box;
    }
    ::v-deep .el-form--inline .el-form-item:nth-child(4) .select-project-member-input {
      width: 100% !important;
      max-width: 100%;
      box-sizing: border-box;
    }
    ::v-deep .el-form--inline .el-form-item:nth-child(5) .el-input {
      width: 100% !important;
      max-width: 100%;
    }
    ::v-deep .el-form--inline .el-form-item:nth-child(5) .el-input__inner {
      width: 100% !important;
      box-sizing: border-box;
    }
  }
  /* 键盘导航：聚焦环由全站 :focus / :focus-within 统一绘制，此处仅抬升层级避免被裁切 */
  &.defect-query-keyboard-nav ::v-deep .defect-list-query-nav-item.defect-query-nav-focused {
    position: relative;
    z-index: 2;
    overflow: visible !important;
  }
  .select-header-icon {
    margin-left: 5px;
    font-size: 14px;
  }
}
.row {
  display: flex;
  flex-direction: row;
  align-items: center;
  > * {
    margin: 0px 5px 0px 0px;
  }
}
/* Tab 行：顶/底间距由 .defect-page gap 承担；裁切 el-tabs 空 content 溢出 */
.defect-tools-tab {
  position: relative;
  z-index: 30;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  margin: 0;
  height: 40px;
  min-width: 0;
  flex-shrink: 0;
  overflow: visible;
  box-sizing: border-box;
  /* 伪元素底线与 active-bar 同处 bottom:0，避免 border-bottom 挤占内容区导致蓝条偏高 */
  &::after {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 2px;
    background-color: #ebeef5;
    pointer-events: none;
    z-index: 1;
  }
  /* 全宽底线覆盖右侧统计按钮；避免与 el-tabs 内置 nav-wrap 底线叠成双线 */
  ::v-deep .el-tabs__nav-wrap::after {
    display: none !important;
  }
  /* el-tabs 默认表头 margin-bottom:15px + 空 content，会在 Tab 与下方查询条之间多出一段空白 */
  ::v-deep .el-tabs__header {
    margin-bottom: 0 !important;
    height: 100%;
    display: flex;
    align-items: stretch;
    box-sizing: border-box;
  }
  ::v-deep .el-tabs__nav-wrap {
    flex: 1;
    min-height: 100%;
    margin-bottom: 0 !important;
    overflow: visible !important;
  }
  ::v-deep .el-tabs__nav-scroll {
    overflow: visible !important;
  }
  ::v-deep .el-tabs__nav {
    overflow: visible !important;
  }
  ::v-deep .el-tabs__active-bar {
    height: 2px !important;
    bottom: 0 !important;
    z-index: 2;
  }
  ::v-deep .el-tabs__content {
    display: none !important;
    height: 0 !important;
    min-height: 0 !important;
    margin: 0 !important;
    padding: 0 !important;
    overflow: hidden !important;
  }
  .el-tabs {
    flex: 1 1 auto;
    min-width: 0;
    display: flex;
    flex-direction: column;
    height: 100%;
    max-height: 100%;
    overflow: visible;
  }
  ::v-deep .el-tabs__item:not(#tab-__defect_add_tab__) {
    max-width: 200px;
    overflow: visible !important;
  }
  ::v-deep #tab-__defect_add_tab__.el-tabs__item {
    display: inline-flex !important;
    align-items: center;
    justify-content: center;
    height: 40px;
    line-height: normal !important;
    padding: 0 4px;
    cursor: default;
    color: #303133;
    &:hover {
      color: #303133;
    }
  }
  ::v-deep .defect-tab-add-btn.svg-icon {
    vertical-align: middle !important;
  }
  ::v-deep .defect-tab-add-btn.defect-tab-shortcut-active {
    position: relative;
    z-index: 4;
    box-shadow: 0 0 0 1px var(--cat2bug-field-focus-color);
  }
  ::v-deep .defect-tab-add-btn {
    flex-shrink: 0;
    width: 15px !important;
    height: 15px !important;
    box-sizing: border-box;
    padding: 2px !important;
    border: 1px solid #c0c4cc;
    border-radius: var(--cat2bug-border-radius, 4px);
    background: #fff;
    color: #606266;
    display: inline-block;
    line-height: 0;
    vertical-align: middle !important;
    overflow: visible !important;
    cursor: pointer;
    transition: color 0.2s, border-color 0.2s, background-color 0.2s;
    &:hover {
      color: #409eff;
      border-color: #c6e2ff;
      background-color: #ecf5ff;
    }
  }
  ::v-deep .el-tabs__item.is-active {
    overflow: visible !important;
  }
  /* 键盘导航：聚焦环贴合 label 内容宽度，底边与 Tab 灰线重叠 */
  &.defect-tab-keyboard-nav ::v-deep .el-tabs__item.defect-tab-nav-focused:not(#tab-__defect_add_tab__) {
    position: relative;
    z-index: 4;
    overflow: visible !important;
    outline: none;
    box-shadow: none !important;

    .defect-tab-label {
      position: relative;
      height: 100%;
      box-sizing: border-box;
      padding: 0 8px;

      &::after {
        content: '';
        position: absolute;
        left: 0;
        right: 0;
        top: 5px;
        bottom: 0;
        box-sizing: border-box;
        border: var(--cat2bug-field-focus-ring-width, 2px) solid var(--cat2bug-field-focus-color);
        border-radius: var(--cat2bug-border-radius, 4px) var(--cat2bug-border-radius, 4px) 0 0;
        pointer-events: none;
      }
    }
  }
  &.defect-tab-keyboard-nav ::v-deep .el-tabs__item.defect-tab-nav-focused:focus,
  &.defect-tab-keyboard-nav ::v-deep .el-tabs__item.defect-tab-nav-focused:focus-visible {
    outline: none !important;
  }
  &.defect-tab-keyboard-nav ::v-deep .defect-tab-add-btn.defect-tab-nav-focused {
    position: relative;
    z-index: 4;
    outline: none;
    box-shadow: 0 0 0 1px var(--cat2bug-field-focus-color);
  }
  &.defect-tab-keyboard-nav ::v-deep .defect-tab-add-btn.defect-tab-nav-focused:focus,
  &.defect-tab-keyboard-nav ::v-deep .defect-tab-add-btn.defect-tab-nav-focused:focus-visible {
    outline: none !important;
  }
  ::v-deep .el-tabs__item:focus,
  ::v-deep .el-tabs__item:focus-visible {
    outline: none !important;
  }
  ::v-deep .el-tabs__header,
  ::v-deep .el-tabs__nav-wrap {
    overflow: visible !important;
  }
  .defect-tools-tab-right {
    position: relative;
    z-index: 31;
    flex: 0 0 auto;
    display: flex;
    justify-content: center;
    align-items: center;
    padding-left: 8px;
    overflow: visible;
  }
}
.defect-page .defect-tools-statistic-wrap {
  position: relative;
  overflow: visible;
  flex-shrink: 0;
  min-width: 0;
  &.defect-statistic-keyboard-nav ::v-deep .statistic-item.defect-statistic-nav-focused {
    position: relative;
    z-index: 2;
    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      top: 0;
      bottom: 0;
      box-sizing: border-box;
      border: var(--cat2bug-field-focus-ring-width, 2px) solid var(--cat2bug-field-focus-color);
      border-radius: var(--cat2bug-border-radius, 4px);
      pointer-events: none;
    }
  }
  &.defect-statistic-keyboard-nav ::v-deep .statistic-item.defect-statistic-nav-focused:focus,
  &.defect-statistic-keyboard-nav ::v-deep .statistic-item.defect-statistic-nav-focused:focus-visible {
    outline: none;
  }
  .defect-list-hint-stat-nav {
    position: absolute;
    right: -2px;
    bottom: -4px;
    width: 16px;
    height: 16px;
    pointer-events: none;
    z-index: 5;
  }
}
.defect-page .defect-tools-statistic {
  position: relative;
  z-index: 1;
}
.defect-tools-button {
  cursor: pointer;
  color: #606266;
  margin: 0px 5px;

  @at-root html.dark & {
    color: var(--text-color-regular);
  }
}
.defect-tools-button:hover {
  color: #409EFF;

  @at-root html.dark & {
    color: #FFC107 !important;
  }
}
.defect-tab-label {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  min-width: 0;
  overflow: visible !important;
  ::v-deep .defect-tab-icon {
    font-size: 14px;
    flex-shrink: 0;
    vertical-align: middle;
  }
  .el-icon-close {
    flex-shrink: 0;
  }
}
.defect-tab-text {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.defect-tools-tab-right {
  position: relative;
  .defect-list-hint-statistic-panel {
    position: absolute;
    right: 2px;
    bottom: -2px;
    width: 14px;
    height: 14px;
    pointer-events: none;
    z-index: 5;
  }
}
.defect-add-toolbar-kbd-wrap {
  position: relative;
  display: inline-flex;
  vertical-align: middle;
}
.defect-add-kbd-anchor-host {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 0;
  overflow: visible;
  pointer-events: none;
  z-index: 6;
  .defect-list-hint-import {
    position: absolute;
    right: 36px;
    bottom: -2px;
    width: 12px;
    height: 12px;
  }
  .defect-list-hint-export {
    position: absolute;
    right: -2px;
    bottom: -2px;
    width: 12px;
    height: 12px;
  }
}
.defect-add-dropdown {
  margin-right: 0px;
  position: relative;
  ::v-deep .el-button-group {
    display: flex;
    flex-direction: row;
    flex-wrap: nowrap;
  }
  ::v-deep button {
    color: #ffffff;
    background: #409eff;
    border-color: #409eff;
  }
  ::v-deep button:hover,
  ::v-deep button:focus {
    background: #66b1ff;
    border-color: #66b1ff;
    color: #ffffff;
  }
  ::v-deep .el-dropdown__caret-button::before {
    background-color: rgba(255, 255, 255, 0.45);

    @at-root html.dark & {
      background-color: #141414 !important;
    }
  }

  @at-root html.dark & {
    ::v-deep button {
      color: #141414 !important;
      background: #ffc107 !important;
      border-color: #ffc107 !important;
    }
    ::v-deep button:hover,
    ::v-deep button:focus {
      color: #141414 !important;
      background: #ffd54f !important;
      border-color: #ffd54f !important;
    }
  }
}
.defect-add-dropdown-menu {
  min-width: 120px;
}
.defect-add-dropdown-divider {
  margin-top: 5px;
  margin-bottom: 5px;
}
/* 右侧工具栏键盘导航：聚焦环由全站 :focus 绘制，此处保证不被裁切 */
.defect-page ::v-deep .defect-toolbar-nav-focused {
  position: relative;
  z-index: 2;
}
.defect-content-view-switch {
  display: inline-flex;
  vertical-align: top;
  flex-shrink: 0;
  /* 三等分格：等宽等高，避免 table/date/excel2 矢量留白不同导致「格与格」观感不一 */
  ::v-deep .el-radio-button__inner {
    padding: 0 !important;
    width: 36px !important;
    /* 与右侧「全部类型」(el-dropdown split-button size=small) 高度对齐 */
    height: 32px !important;
    line-height: 1;
    box-sizing: border-box;
    display: inline-flex !important;
    align-items: center !important;
    justify-content: center !important;
  }
  .defect-content-view-switch-inner {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 18px;
    height: 18px;
    flex-shrink: 0;
    line-height: 0;
  }
  .defect-content-view-switch-inner ::v-deep .svg-icon {
    width: 18px !important;
    height: 18px !important;
    vertical-align: top;
  }
}
</style>

<!-- 三视图共用：工具栏与下方主体间距、与右侧 table-tools 对齐（不受子组件 scoped 限制） -->
<style lang="scss">
.defect-add-dropdown-menu .el-dropdown-menu__item.cat2bug-split-dropdown-item-focused,
.defect-add-dropdown-menu .el-dropdown-menu__item:focus {
  outline: none !important;
  box-shadow: none !important;
  background-color: #ecf5ff;
  color: inherit;
}
html.dark .defect-add-dropdown-menu .el-dropdown-menu__item.cat2bug-split-dropdown-item-focused,
html.dark .defect-add-dropdown-menu .el-dropdown-menu__item:focus {
  outline: none !important;
  box-shadow: none !important;
  background-color: #3a3a42;
  color: inherit;
}
/* 显示字段：键盘导航高亮，与成员下拉 is-keyboard-active 一致 */
.defect-column-picker-popover,
.defect-excel-column-picker-popover {
  padding: 8px 6px;
  box-sizing: border-box;
}
.defect-column-picker .el-checkbox,
.defect-column-picker-popover .defect-column-picker .el-checkbox,
.defect-excel-column-picker-popover .defect-column-picker .el-checkbox {
  display: flex;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
  margin-right: 0;
  padding: 4px 8px;
  border-radius: 5px;
  outline: none !important;
  box-shadow: none !important;
}
.defect-column-picker .el-checkbox:focus,
.defect-column-picker-popover .defect-column-picker .el-checkbox:focus,
.defect-excel-column-picker-popover .defect-column-picker .el-checkbox:focus {
  outline: none !important;
  box-shadow: none !important;
}
.defect-column-picker .el-checkbox.defect-column-picker-item-focused,
.defect-column-picker-popover .defect-column-picker .el-checkbox.defect-column-picker-item-focused,
.defect-excel-column-picker-popover .defect-column-picker .el-checkbox.defect-column-picker-item-focused {
  background-color: #ecf5ff;
}
html.dark .defect-column-picker .el-checkbox.defect-column-picker-item-focused,
html.dark .defect-column-picker-popover .defect-column-picker .el-checkbox.defect-column-picker-item-focused,
html.dark .defect-excel-column-picker-popover .defect-column-picker .el-checkbox.defect-column-picker-item-focused {
  background-color: #3a3a42;
}
/* 取消勾选时禁用过渡，避免钩子在 scaleY 动画中先闪白再消失 */
.defect-column-picker-popover .defect-column-picker .el-checkbox__inner,
.defect-excel-column-picker-popover .defect-column-picker .el-checkbox__inner {
  transition: none !important;
}
.defect-column-picker-popover .defect-column-picker .el-checkbox__inner::after,
.defect-excel-column-picker-popover .defect-column-picker .el-checkbox__inner::after {
  transition: none !important;
}
.defect-column-picker-popover .defect-column-picker .el-checkbox__input:not(.is-checked) .el-checkbox__inner::after,
.defect-excel-column-picker-popover .defect-column-picker .el-checkbox__input:not(.is-checked) .el-checkbox__inner::after {
  transform: rotate(45deg) scaleY(0) !important;
  border-color: transparent !important;
}
html.dark .defect-column-picker-popover .defect-column-picker .el-checkbox__input.is-checked .el-checkbox__inner::after,
html.dark .defect-excel-column-picker-popover .defect-column-picker .el-checkbox__input.is-checked .el-checkbox__inner::after {
  border-color: #141414 !important;
}
.defect-page .defect-view-toolbar {
  box-sizing: border-box;
  flex-shrink: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  align-content: flex-start;
  /* space-between：同行时左右拉开；右侧单独换行时该行仅一项，会靠主轴起点（左） */
  justify-content: space-between;
  column-gap: var(--cat2bug-toolbar-section-gap, 10px);
  row-gap: var(--cat2bug-toolbar-row-gap, 8px);
  margin-top: 0;
  margin-bottom: var(--project-list-section-v-gap, 10px);
  padding-top: 0;
  padding-bottom: 0;
}
/* 左侧（查询区 / Excel 左槽）优先占行宽；右侧为第二子节点时先被挤到下一行 */
.defect-page .defect-view-toolbar > *:first-child {
  flex: 1 1 auto;
  min-width: 0;
}
.defect-page .defect-view-toolbar > *:nth-child(2) {
  flex: 0 0 auto;
  display: inline-flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: flex-start;
}
@media screen and (max-width: 576px) {
  .defect-page .defect-view-toolbar > *:nth-child(2) {
    flex: 1 1 100%;
    width: 100%;
    max-width: 100%;
    justify-content: flex-start;
    flex-wrap: nowrap !important;
  }
  /* 列选择 + 新建：同一行，新建 flex:1 占满剩余宽 */
  .defect-page .defect-view-toolbar .table-tools {
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
    display: flex !important;
    flex-direction: row !important;
    flex-wrap: nowrap !important;
    align-items: center !important;
    column-gap: 8px;
  }
  .defect-page .defect-view-toolbar .table-tools > *:first-child {
    flex-shrink: 0;
  }
  .defect-page .defect-view-toolbar .table-tools .defect-add-dropdown {
    flex: 1 1 auto;
    min-width: 0;
    width: auto !important;
    max-width: none;
    box-sizing: border-box;
  }
  .defect-page .defect-view-toolbar .defect-add-dropdown .el-button-group {
    width: 100%;
    display: flex;
  }
  .defect-page .defect-view-toolbar .defect-add-dropdown .el-button-group > .el-button:first-child {
    flex: 1 1 auto;
    min-width: 0;
  }
  /* Excel 视图右侧：刷新/列选择与新建同一行，新建占满剩余宽 */
  .defect-page .defect-view-toolbar.defect-excel-tools > .defect-excel-tools-right {
    display: flex !important;
    flex-direction: row !important;
    flex-wrap: nowrap !important;
    align-items: center !important;
    flex: 1 1 auto;
    min-width: 0;
    width: 100%;
    max-width: 100%;
    column-gap: 8px;
  }
  .defect-page .defect-view-toolbar.defect-excel-tools > .defect-excel-tools-right > *:not(.defect-add-dropdown) {
    flex-shrink: 0;
  }
  .defect-page .defect-view-toolbar.defect-excel-tools > .defect-excel-tools-right .defect-add-dropdown {
    flex: 1 1 auto;
    min-width: 0;
    width: auto !important;
    max-width: none;
    box-sizing: border-box;
  }
}
.defect-page .defect-view-toolbar .table-tools {
  padding-top: 0 !important;
  align-items: flex-start !important;
  display: inline-flex;
  flex-wrap: wrap;
  row-gap: 8px;
  justify-content: flex-start;
}
.defect-page .defect-view-toolbar .table-tools.row > * {
  margin-bottom: 0 !important;
}

/* 缺陷列表页快捷键锚点布局（徽标样式见 assets/styles/page-kbd-hints.scss） */
.defect-page .defect-list-hint-statistic {
  display: inline-flex;
  position: relative;
}
.defect-page .defect-list-hint-tabs .el-tabs__item.is-active {
  overflow: visible !important;
}
.defect-page .defect-list-hint-tabs .el-tabs__header,
.defect-page .defect-list-hint-tabs .el-tabs__nav-wrap,
.defect-page .defect-list-hint-tabs .el-tabs__nav-scroll,
.defect-page .defect-list-hint-tabs .el-tabs__nav {
  overflow: visible !important;
}

</style>
