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
    <project-label class="defect-project-label" />
    <!-- 缺陷页标签-->
    <div class="defect-tools-tab" ref="defectToolsTab">
      <el-tabs ref="defectTabs" v-model="activeDefectTabName" @tab-click="selectDefectTabHandle">
        <el-tab-pane v-for="tab in config.tabs" :key="tab.tabId+''" :name="tab.tabId+''">
          <span slot="label" class="defect-tab-label">
            <svg-icon icon-class="list2" class="defect-tab-icon" />
            <span class="defect-tab-text" :title="tab.tabName">{{ tab.tabName }}</span>
            <i style="width: 14px;" class="el-icon-close" @click.stop="removeDefectTabHandle(tab.tabId)" />
          </span>
        </el-tab-pane>
        <el-tab-pane key="all-tab" :name="allTab">
          <span slot="label" class="defect-tab-label">
            <svg-icon icon-class="all" class="defect-tab-icon" />
            <span class="defect-tab-text" :title="$t('defect.all-defect')">{{ $t('defect.all-defect') }}</span>
          </span>
        </el-tab-pane>
        <el-tab-pane key="deleted-tab" :name="deletedTab">
          <span slot="label" class="defect-tab-label">
            <svg-icon icon-class="delete" class="defect-tab-icon" />
            <span class="defect-tab-text" :title="$t('defect.deleted-defect')">{{ $t('defect.deleted-defect') }}</span>
          </span>
        </el-tab-pane>
        <el-tab-pane :name="defectAddTabPaneName" disabled class="defect-tab-add-pane">
          <svg-icon
            slot="label"
            icon-class="add-tab"
            class-name="defect-tab-label defect-tab-add-btn"
            :title="$t('defect.tab')"
            @click.stop="addDefectTabHandle"
          />
        </el-tab-pane>
      </el-tabs>
      <div class="defect-tools-tab-right">
        <svg-icon v-show="statisticPanelVisible" class="defect-tools-button" icon-class="view-statistic" @click.native="addStatisticHandle" />
      </div>
    </div>
    <!-- 统计板块-->
    <cat2-bug-statistic v-show="statisticPanelVisible" class="defect-tools-statistic" :params="{}" :draggable="true" />
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
    >
      <template slot="left-tools">
        <!-- 搜索-->
        <div class="defect-tools-search">
          <el-form v-show="showSearch" ref="queryForm" :model="queryParams" size="small" :inline="true" label-width="0">
            <el-form-item>
              <!-- 缺陷显示模式切换 -->
              <el-radio-group v-model="defectContentComponent" class="defect-content-view-switch" size="mini" @input="handleDefectContentChange">
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
            <el-form-item prop="defectType">
              <el-dropdown split-button size="small" @command="defectTypeChangeHandle" @click="selectDefectTabHandle">
                {{ $i18n.t(activeDefectTypeName) }}
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item command="">{{ $i18n.t('defect.all-type') }}</el-dropdown-item>
                  <el-dropdown-item v-for="type in config.types" :key="'type_'+type.key" :command="type.value">{{ $i18n.t(type.value) }}</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </el-form-item>
            <el-form-item prop="defectState">
              <el-select
                v-model="queryParams.params.defectStates"
                class="defect-state-select"
                size="small"
                multiple
                collapse-tags
                clearable
                :placeholder="$t('defect.select-state')"
                @change="handleQuery()"
              >
                <template v-slot:prefix>
                  <i class="select-header-icon el-icon-finished"></i>
                </template>
                <el-option
                  v-for="state in config.states"
                  :key="state.key"
                  :label="$i18n.t(state.value)"
                  :value="state.key"
                />
              </el-select>
            </el-form-item>
            <el-form-item prop="handleBy">
              <select-project-member
                v-model="queryParams.handleBy"
                :project-id="projectId"
                placeholder="defect.select-handle-by"
                :is-head="false"
                size="small"
                icon="el-icon-user"
                @input="handleQuery()"
              />
            </el-form-item>
            <el-form-item prop="nameVersionKeyword">
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
        <el-dropdown
          v-if="defectAddToolbarVisible"
          class="defect-add-dropdown"
          split-button
          size="small"
          type="primary"
          @click="handleAdd"
        >
          <i class="el-icon-plus" />{{ $i18n.t('defect.create') }}
          <el-dropdown-menu slot="dropdown" class="defect-add-dropdown-menu">
            <el-dropdown-item @click.native="handleAdd"><i class="el-icon-plus" />{{ $i18n.t('defect.create') }}</el-dropdown-item>
            <el-divider class="defect-add-dropdown-divider" />
            <el-dropdown-item @click.native="handleImport"><i class="el-icon-upload2" />{{ $t('defect.import') }}</el-dropdown-item>
            <el-dropdown-item @click.native="handleExport"><i class="el-icon-download" />{{ $t('defect.export') }}</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
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
  DEFECT_FIELD_LIST_KEY,
  getColumnsFromCat2BugTable,
  mergeTableColumns
} from '@/utils/excel-export-columns'
import { TableOptions } from '@/views/system/defect/list/table-options'
import store from '@/store'
import DefectTable from './list/table'
import DefectExcel from './list/excel'

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
    }
  },
  watch: {
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
  // 移除滚动条监听
  destroyed() {
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
    },
    /** 查询缺陷 */
    search(params) {
      this._setProperty(this.queryParams, params)
      this.handleQuery()
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
      const activeName = tab && tab.name != null ? String(tab.name) : String(this.activeDefectTabName)
      if (activeName === this.defectAddTabPaneName) {
        return
      }
      if (activeName === this.allTab) {
        this.reset()
        if (!this.queryParams.params) {
          this.$set(this.queryParams, 'params', {})
        }
        this.$set(this.queryParams.params, 'delFlag', '0')
        this.handleQuery()
        this.$cache.local.set(DEFECT_TAB_CACHE_KEY, activeName)
        return
      }
      if (activeName === this.deletedTab) {
        this.reset()
        if (!this.queryParams.params) {
          this.$set(this.queryParams, 'params', {})
        }
        this.$set(this.queryParams.params, 'delFlag', '2')
        this.handleQuery()
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
        } else {
          this.reset()
        }
        this.handleQuery()
      }
      this.$cache.local.set(DEFECT_TAB_CACHE_KEY, activeName)
    },
    /** 打开添加缺陷页签对话框 */
    addDefectTabHandle() {
      this.$refs.defectTabDialog.open()
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
    handleExport() {
      const host = resolveExportAssetHost()
      const payload = { ...this.queryParams }
      if (host) {
        payload.params = { ...(payload.params || {}), host }
      }
      const c = this.$refs.defectContentComponent
      let columns = null
      if (c && c.$refs && c.$refs.cat2BugTable) {
        columns = getColumnsFromCat2BugTable(c.$refs.cat2BugTable)
      }
      if (!columns) {
        columns = mergeTableColumns(TableOptions, DEFECT_FIELD_LIST_KEY, this.$cache.local)
      }
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
  /* 查询条上、下与 Tab / 主内容区的留白一致（子树继承） */
  --defect-toolbar-v-gap: 8px;
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
  /* 与 Tab/统计区之间的纵向留白改由 .defect-view-toolbar 的 margin-top 承担，避免 padding+margin 叠在不同盒子上导致观感不对称 */
  padding-top: 0;
  box-sizing: border-box;
}
.defect-page:not(.defect-page--excel-view) .defect-content-slot:not(.defect-content-slot--excel) {
  flex: 1 1 0%;
  min-height: 0;
}
.defect-page.defect-page--excel-view .defect-project-label,
.defect-page.defect-page--excel-view .defect-tools-tab,
.defect-page.defect-page--excel-view .defect-tools-statistic {
  flex-shrink: 0;
}
.defect-page.defect-page--excel-view .defect-content-slot.defect-content-slot--excel {
  flex: 1 1 0%;
  min-height: 0;
  overflow: hidden;
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
    align-items: center;
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
    align-items: center;
    vertical-align: middle;
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
  .select-header-icon {
    margin-left: 5px;
    color: var(--text-color-placeholder);
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
/* 与用例页：h3 下 10px + 本行上 8px（flex 子项不折叠 margin） */
.defect-tools-tab {
  position: relative;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  margin-top: var(--defect-toolbar-v-gap, 8px);
  height: 40px;
  min-width: 0;
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
  }
  ::v-deep .el-tabs__nav-wrap {
    flex: 1;
    min-height: 100%;
    margin-bottom: 0 !important;
  }
  ::v-deep .el-tabs__active-bar {
    height: 2px !important;
    bottom: 0 !important;
    z-index: 2;
  }
  ::v-deep .el-tabs__content {
    display: none !important;
  }
  .el-tabs {
    flex: 1 1 auto;
    min-width: 0;
    display: flex;
    flex-direction: column;
    height: 100%;
  }
  ::v-deep .el-tabs__item:not(#tab-__defect_add_tab__) {
    max-width: 200px;
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
  ::v-deep .defect-tab-add-btn {
    position: relative;
    top: 1px;
    flex-shrink: 0;
    width: 15px !important;
    height: 15px !important;
    box-sizing: border-box;
    padding: 2px !important;
    border: 1px solid #c0c4cc;
    border-radius: 2px;
    background: #fff;
    color: #606266;
    display: inline-block;
    line-height: 0;
    cursor: pointer;
    vertical-align: middle !important;
    overflow: visible !important;
    transition: color 0.2s, border-color 0.2s, background-color 0.2s;
    &:hover {
      color: #409eff;
      border-color: #c6e2ff;
      background-color: #ecf5ff;
    }
  }
  .defect-tools-tab-right {
    flex: 0 0 auto;
    display: flex;
    justify-content: center;
    align-items: center;
    padding-left: 8px;
  }
}
.defect-tools-button {
  cursor: pointer;
  color: #606266;
  margin: 0px 5px;
}
.defect-tools-button:hover {
  color: #409EFF;
}
.defect-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  min-width: 0;
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
.defect-add-dropdown {
  margin-right: 0px;
  ::v-deep .el-button-group {
    display: flex;
    flex-direction: row;
    flex-wrap: nowrap;
  }
  ::v-deep button {
    color: #1890ff;
    background: #e8f4ff;
    border-color: #a3d3ff;
  }
  ::v-deep button:hover {
    background: #1890ff;
    border-color: #1890ff;
    color: #FFFFFF;
  }
  ::v-deep .el-dropdown__caret-button::before {
      background-color: #a3d3ff;
  }
}
.defect-add-dropdown-menu {
  min-width: 120px;
}
.defect-add-dropdown-divider {
  margin-top: 5px;
  margin-bottom: 5px;
}
.defect-content-view-switch {
  vertical-align: middle;
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
.defect-page .defect-view-toolbar {
  box-sizing: border-box;
  flex-shrink: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  align-content: flex-start;
  /* space-between：同行时左右拉开；右侧单独换行时该行仅一项，会靠主轴起点（左） */
  justify-content: space-between;
  column-gap: var(--cat2bug-toolbar-section-gap, 10px);
  row-gap: var(--cat2bug-toolbar-row-gap, 8px);
  margin-top: var(--defect-toolbar-v-gap, 8px);
  margin-bottom: var(--defect-toolbar-v-gap, 8px);
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
  align-items: center;
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
  align-items: center !important;
  display: inline-flex;
  flex-wrap: wrap;
  row-gap: 8px;
  justify-content: flex-start;
}
.defect-page .defect-view-toolbar .table-tools.row > * {
  margin-bottom: 0 !important;
}

</style>
