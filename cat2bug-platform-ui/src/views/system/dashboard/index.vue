<template>
  <div class="app-container workbench-page">
    <div class="workbench-header">
      <div class="workbench-header__bar">
        <project-label class="workbench-header__project" />
      </div>

      <div class="workbench-hello" aria-label="greeting">
        <cat2-bug-avatar class="workbench-hello__avatar" :member="currentMember" :size="44" />
        <div class="workbench-hello__copy">
          <div class="workbench-hello__line">
            <span class="workbench-hello__salute">{{ greetingSalute }}</span>
            <span class="workbench-hello__name" :title="displayName">{{ displayName }}</span>
          </div>
          <div class="workbench-hello__date">{{ greetingMeta }}</div>
        </div>
      </div>
    </div>

    <div class="workbench-tools-tab">
      <el-tabs
        v-if="tabsReady"
        v-model="activeTab"
        class="workbench-tabs"
        @tab-change="onTabChange"
        @tab-click="onTabClick"
      >
        <el-tab-pane
          v-for="entry in orderedNavEntries"
          :key="entry.key"
          :name="entry.name"
        >
          <template #label>
            <span class="wb-tab-label">
              <el-icon
                v-if="entry.kind === 'work'"
                class="wb-tab-kind is-private"
                :title="$t('workbench.tab-dialog.private')"
              >
                <LockIcon />
              </el-icon>
              <el-icon
                v-else-if="entry.tab && entry.tab.visibility === 'project'"
                class="wb-tab-kind is-shared"
                :title="$t('workbench.tab-dialog.shared')"
              >
                <ShareIcon />
              </el-icon>
              <el-icon
                v-else
                class="wb-tab-kind is-private"
                :title="$t('workbench.tab-dialog.private')"
              >
                <LockIcon />
              </el-icon>
              <span
                class="wb-tab-text"
                :title="entry.kind === 'work' ? $t('workbench.tab.work') : entry.tab.tabName"
              >{{ entry.kind === 'work' ? $t('workbench.tab.work') : entry.tab.tabName }}</span>
              <el-dropdown
                v-if="!editing && (entry.kind === 'work' || canEditTab(entry.tab))"
                trigger="click"
                class="wb-tab-menu"
                @command="onTabMenuCommand"
                @pointerdown.stop
                @mousedown.stop
                @click.stop
              >
                <span class="wb-tab-menu-trigger" @pointerdown.stop @mousedown.stop @click.stop>
                  <el-icon><MoreFilledIcon /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      :command="'edit:' + entry.name"
                      :icon="EditPenIcon"
                    >{{ $t('modify') }}</el-dropdown-item>
                    <el-dropdown-item
                      v-if="canDeleteMoreTabs"
                      :command="'delete:' + entry.name"
                      :icon="DeleteIcon"
                      divided
                      class="wb-tab-menu-danger"
                    >{{ $t('delete') }}</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane v-if="!editing" :name="addTabPaneName">
          <template #label>
            <svg-icon
              icon-class="add-tab"
              class-name="wb-tab-add-btn"
              :title="$t('workbench.tab-add')"
              @click.stop="openAddTab"
              @mousedown.stop.prevent
            />
          </template>
        </el-tab-pane>
      </el-tabs>
      <div v-if="editing" class="workbench-tools-tab-right">
        <div v-if="!isWorkSurface" class="workbench-tab-rename">
          <span class="workbench-tab-rename__label">{{ $t('workbench.tab-dialog.name') }}</span>
          <el-input
            v-model="draftTabName"
            size="small"
            maxlength="12"
            show-word-limit
            clearable
            class="workbench-tab-rename__input"
            :placeholder="$t('workbench.tab-dialog.name-ph')"
          />
        </div>
        <el-dropdown trigger="click" @command="onPresetCommand">
          <el-button size="small" :icon="ResetIcon">
            {{ $t('workbench.preset-label') }}
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="id in applyPresetIds" :key="id" :command="'apply:' + id">
                {{ $t('workbench.preset.' + id) }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button size="small" :icon="CloseIcon" @click="cancelEdit">
          {{ $t('cancel') }}
        </el-button>
        <el-button
          size="small"
          type="primary"
          :icon="saving ? undefined : CheckIcon"
          :loading="saving"
          @click="saveEdit"
        >
          {{ $t('save') }}
        </el-button>
      </div>
    </div>

    <div class="list-page-toolbar" v-if="!editing && isWorkSurface">
      <div class="list-page-toolbar__filters stats-inline">
        {{ toolbarHint }}
      </div>
      <div class="list-page-toolbar__actions">
        <quick-create />
      </div>
    </div>

    <div v-if="editing" class="workbench-edit-hint stats-inline">
      {{ $t('workbench.edit-hint-rows') }}
    </div>

    <workbench-row-editor
      v-if="editing && canEditCurrentSurface"
      v-loading="loadingConfig"
      :rows="draftRows"
      :title-of="widgetTitleOf"
      :props-of="widgetPropsOf"
      :component-of="widgetComponentOf"
      @update:rows="onDraftRows"
    />
    <workbench-rows
      v-else
      v-loading="loadingConfig"
      :rows="viewRows"
      :title-of="widgetTitleOf"
      :meta-of="widgetMetaOf"
      :props-of="widgetPropsOf"
      :component-of="widgetComponentOf"
    >
      <template #empty>
        <cat2-bug-empty :description="$t('workbench.empty-tab')" />
      </template>
    </workbench-rows>

    <workbench-tab-dialog
      ref="tabDialog"
      :project-id="projectId"
      :can-manage-shared="canManageShared"
      :shared-count="sharedCount"
      :shared-limit="sharedLimit"
      @created="onTabCreated"
    />
  </div>
</template>

<script>
import { markRaw } from 'vue'
import {
  ArrowDown,
  Check,
  Close,
  Delete,
  EditPen,
  Lock as LockIcon,
  MoreFilled,
  RefreshRight,
  Share as ShareIcon
} from '@element-plus/icons-vue'
import Sortable from 'sortablejs'
import ProjectLabel from '@/components/Project/ProjectLabel'
import Cat2BugAvatar from '@/components/Cat2BugAvatar'
import { useUserStore } from '@/stores/user'
import { getCurrentProjectId } from '@/utils/store-context'
import {
  getWorkbenchLayout,
  saveWorkbenchLayout,
  listWorkbenchTabs,
  updateWorkbenchTab,
  deleteWorkbenchTab,
  sortWorkbenchTabs
} from '@/api/system/workbench'
import QuickCreate from './workbench/widgets/QuickCreate.vue'
import WorkbenchRows from './workbench/WorkbenchRows.vue'
import WorkbenchRowEditor from './workbench/WorkbenchRowEditor.vue'
import WorkbenchTabDialog from './workbench/WorkbenchTabDialog.vue'
import {
  APPLY_PRESET_IDS,
  WORKBENCH_PRESETS,
  buildConfigFromPreset,
  buildTabConfigFromPreset,
  parseTabConfig,
  presetIdFromRoleKeys,
  alignLegacyConfig,
  rowsFromPreset
} from './workbench/presets'
import { normalizeRows, CONFIG_VERSION } from './workbench/row-layout'
import { getWidgetDef } from './workbench/widget-registry'

const ADD_TAB_PANE = '__workbench_add_tab__'
const WORK_TAB_KEY = 'work'
const TAB_CACHE_PREFIX = 'workbench-tab:'
const WORK_HIDDEN_PREFIX = 'workbench-work-hidden:'
/** 个人页签条完整顺序（含工作台与共享/私有交织），按项目存本地 */
const TAB_ORDER_PREFIX = 'workbench-tab-order:'
/** :icon 传入的组件勿进 data 响应式，否则 Vue 会告警 */
const ResetIcon = markRaw(RefreshRight)
const CloseIcon = markRaw(Close)
const CheckIcon = markRaw(Check)
const MoreFilledIcon = markRaw(MoreFilled)
const EditPenIcon = markRaw(EditPen)
const DeleteIcon = markRaw(Delete)

function readTabCache(projectId) {
  try {
    const raw = localStorage.getItem(TAB_CACHE_PREFIX + projectId)
    if (!raw || raw === ADD_TAB_PANE) return null
    return String(raw)
  } catch (e) {
    return null
  }
}

function writeTabCache(projectId, tabId) {
  try {
    if (!projectId) return
    if (!tabId || tabId === ADD_TAB_PANE) {
      localStorage.removeItem(TAB_CACHE_PREFIX + projectId)
      return
    }
    localStorage.setItem(TAB_CACHE_PREFIX + projectId, String(tabId))
  } catch (e) {
    /* ignore */
  }
}

function readWorkHidden(projectId) {
  try {
    return localStorage.getItem(WORK_HIDDEN_PREFIX + projectId) === '1'
  } catch (e) {
    return false
  }
}

function writeWorkHidden(projectId, hidden) {
  try {
    if (!projectId) return
    if (hidden) localStorage.setItem(WORK_HIDDEN_PREFIX + projectId, '1')
    else localStorage.removeItem(WORK_HIDDEN_PREFIX + projectId)
  } catch (e) {
    /* ignore */
  }
}

function readTabOrder(projectId) {
  try {
    const raw = localStorage.getItem(TAB_ORDER_PREFIX + projectId)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed.map(String) : null
  } catch (e) {
    return null
  }
}

function writeTabOrder(projectId, order) {
  try {
    if (!projectId) return
    localStorage.setItem(TAB_ORDER_PREFIX + projectId, JSON.stringify(order || []))
  } catch (e) {
    /* ignore */
  }
}

/** 默认：工作台 → 共享(tabSort desc) → 私有(tabSort desc) */
function defaultTabOrder(workVisible, customTabs) {
  const list = customTabs || []
  const bySort = (a, b) => Number(b.tabSort || 0) - Number(a.tabSort || 0)
  const shared = list.filter(t => t.visibility === 'project').slice().sort(bySort)
  const priv = list.filter(t => t.visibility === 'private').slice().sort(bySort)
  return [
    ...(workVisible ? [WORK_TAB_KEY] : []),
    ...shared.map(t => String(t.tabId)),
    ...priv.map(t => String(t.tabId))
  ]
}

function mergeTabOrder(saved, workVisible, customTabs) {
  const valid = new Set([
    ...(workVisible ? [WORK_TAB_KEY] : []),
    ...(customTabs || []).map(t => String(t.tabId))
  ])
  const out = []
  for (const id of saved || []) {
    const s = String(id)
    if (valid.has(s) && !out.includes(s)) out.push(s)
  }
  for (const id of defaultTabOrder(workVisible, customTabs)) {
    if (!out.includes(id)) out.push(id)
  }
  return out.filter(id => valid.has(id))
}

export default {
  name: 'ProjectWorkbench',
  components: {
    ProjectLabel,
    Cat2BugAvatar,
    QuickCreate,
    ArrowDown,
    ShareIcon,
    LockIcon,
    MoreFilledIcon,
    WorkbenchRows,
    WorkbenchRowEditor,
    WorkbenchTabDialog
  },
  setup() {
    return { ResetIcon, CloseIcon, CheckIcon, EditPenIcon, DeleteIcon }
  },
  data() {
    return {
      projectId: null,
      activeTab: 'work',
      /** 页签列表与 activeTab 就绪后再挂载 el-tabs，避免先亮「工作台」再滑到缓存项 */
      tabsReady: false,
      addTabPaneName: ADD_TAB_PANE,
      loadingConfig: false,
      saving: false,
      editing: false,
      config: null,
      draft: null,
      draftRowsLocal: [],
      customTabs: [],
      canManageShared: false,
      sharedCount: 0,
      sharedLimit: 24,
      projectRoleKeys: [],
      applyPresetIds: APPLY_PRESET_IDS,
      tabSortable: null,
      tabSortSaving: false,
      draftTabName: '',
      /** 个人「工作台」页签是否显示；删除后本地隐藏，布局数据仍保留 */
      workTabVisible: true,
      /** 页签条完整顺序键：'work' | tabId 字符串 */
      navTabOrder: []
    }
  },
  watch: {
    /** 仅页签增删/可见性变化时重绑；纯 tabSort 更新不销毁 Sortable，避免拖坏 Vue/i18n 节点 */
    tabNavBindKey() {
      if (this.tabSortSaving) return
      this.$nextTick(() => this.bindTabSortable())
    },
    editing(val) {
      if (val) this.destroyTabSortable()
      else this.$nextTick(() => this.bindTabSortable())
    }
  },
  mounted() {
    this.$nextTick(() => this.bindTabSortable())
  },
  beforeUnmount() {
    this.destroyTabSortable()
  },
  computed: {
    tabNavBindKey() {
      const ids = (this.customTabs || []).map(t => `${t.tabId}:${t.visibility}`).join('|')
      return `${ids}#${this.canManageShared ? 1 : 0}#${this.workTabVisible ? 1 : 0}#${(this.navTabOrder || []).join(',')}`
    },
    sharedTabs() {
      return (this.customTabs || []).filter(t => t.visibility === 'project')
    },
    privateTabs() {
      return (this.customTabs || []).filter(t => t.visibility === 'private')
    },
    /** 按个人顺序渲染的页签条（工作台可插在任意位置） */
    orderedNavEntries() {
      const byId = new Map((this.customTabs || []).map(t => [String(t.tabId), t]))
      const entries = []
      for (const key of this.navTabOrder || []) {
        if (key === WORK_TAB_KEY) {
          if (this.workTabVisible) {
            entries.push({ key: 'nav-work', name: WORK_TAB_KEY, kind: 'work', tab: null })
          }
          continue
        }
        const tab = byId.get(String(key))
        if (tab) {
          entries.push({
            key: 'nav-' + tab.tabId,
            name: String(tab.tabId),
            kind: 'custom',
            tab
          })
        }
      }
      return entries
    },
    /** 可见页签数（工作台 + 自定义）；仅剩 1 个时禁止删除 */
    visibleTabCount() {
      return (this.workTabVisible ? 1 : 0) + (this.customTabs || []).length
    },
    canDeleteMoreTabs() {
      return this.visibleTabCount > 1
    },
    isWorkSurface() {
      return this.activeTab === 'work'
    },
    currentCustomTab() {
      if (this.activeTab === 'work' || this.activeTab === ADD_TAB_PANE) return null
      return (this.customTabs || []).find(t => String(t.tabId) === String(this.activeTab)) || null
    },
    canEditCurrentSurface() {
      if (this.isWorkSurface) return true
      const tab = this.currentCustomTab
      if (!tab) return false
      return this.canEditTab(tab)
    },
    currentMember() {
      const u = useUserStore()
      return {
        avatar: u.avatar,
        nickName: u.nickName,
        userName: u.name,
        name: u.nickName || u.name
      }
    },
    displayName() {
      const u = useUserStore()
      return u.nickName || u.name || ''
    },
    greetingSalute() {
      const hour = new Date().getHours()
      let key = 'workbench.greeting-salute-day'
      if (hour < 6) key = 'workbench.greeting-salute-night'
      else if (hour < 12) key = 'workbench.greeting-salute-morning'
      else if (hour < 18) key = 'workbench.greeting-salute-afternoon'
      else key = 'workbench.greeting-salute-evening'
      return this.$t(key)
    },
    greetingMeta() {
      const now = new Date()
      const y = now.getFullYear()
      const m = String(now.getMonth() + 1).padStart(2, '0')
      const d = String(now.getDate()).padStart(2, '0')
      const weekKeys = [
        'workbench.weekday.sun',
        'workbench.weekday.mon',
        'workbench.weekday.tue',
        'workbench.weekday.wed',
        'workbench.weekday.thu',
        'workbench.weekday.fri',
        'workbench.weekday.sat'
      ]
      const week = this.$t(weekKeys[now.getDay()])
      return this.$t('workbench.greeting-meta', [`${y}-${m}-${d}`, week])
    },
    toolbarHint() {
      if (this.config?.preset === 'manager') return this.$t('workbench.hint-work-manager')
      return this.$t('workbench.hint-work')
    },
    weekRangeLabel() {
      const now = new Date()
      const start = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      const day = start.getDay()
      const diff = day === 0 ? -6 : 1 - day
      start.setDate(start.getDate() + diff)
      const end = new Date(start)
      end.setDate(start.getDate() + 6)
      const fmt = d => {
        const m = String(d.getMonth() + 1).padStart(2, '0')
        const dd = String(d.getDate()).padStart(2, '0')
        return `${m}-${dd}`
      }
      return this.$t('workbench.weekly.range', [fmt(start), fmt(end)])
    },
    viewRows() {
      if (this.isWorkSurface) return this.config?.tabs?.work?.rows || []
      const tab = this.currentCustomTab
      if (!tab) return []
      let raw = tab.configJson
      if (typeof raw === 'string') {
        try {
          raw = JSON.parse(raw)
        } catch (e) {
          return []
        }
      }
      const parsed = parseTabConfig(raw)
      return parsed?.rows || []
    },
    draftRows() {
      return this.draftRowsLocal || []
    }
  },
  created() {
    this.bootstrap()
  },
  methods: {
    onTabClick(pane) {
      const name = pane?.paneName ?? pane?.props?.name ?? pane?.name
      if (name === ADD_TAB_PANE) {
        this.openAddTab()
      }
    },
    onTabChange(name) {
      if (name === ADD_TAB_PANE) {
        const restore = readTabCache(this.projectId) || 'work'
        this.$nextTick(() => {
          if (this.activeTab === ADD_TAB_PANE) {
            this.activeTab = this.resolveActiveTab(restore)
          }
        })
        return
      }
      if (this.editing) {
        this.cancelEdit()
      }
      writeTabCache(this.projectId, name)
    },
    openAddTab() {
      if (this.editing) return
      const restore = this.activeTab !== ADD_TAB_PANE ? this.activeTab : 'work'
      this.$refs.tabDialog?.open()
      this.$nextTick(() => {
        if (this.activeTab === ADD_TAB_PANE) this.activeTab = restore
      })
    },
    resolveActiveTab(candidate) {
      const firstNav = (this.navTabOrder || []).find(id => {
        if (id === WORK_TAB_KEY) return this.workTabVisible
        return (this.customTabs || []).some(t => String(t.tabId) === String(id))
      })
      const firstCustom = this.customTabs?.length ? String(this.customTabs[0].tabId) : null
      const fallback = firstNav || (this.workTabVisible ? WORK_TAB_KEY : firstCustom || WORK_TAB_KEY)
      if (!candidate || candidate === ADD_TAB_PANE) return fallback
      if (candidate === WORK_TAB_KEY) return this.workTabVisible ? WORK_TAB_KEY : fallback
      if ((this.customTabs || []).some(t => String(t.tabId) === String(candidate))) {
        return String(candidate)
      }
      return fallback
    },
    ensureAtLeastOneTab() {
      if (this.visibleTabCount > 0) return
      this.workTabVisible = true
      writeWorkHidden(this.projectId, false)
    },
    /** 合并本地顺序与当前可见页签；新 tab 追加到末尾 */
    ensureNavTabOrder() {
      const merged = mergeTabOrder(
        readTabOrder(this.projectId) || this.navTabOrder,
        this.workTabVisible,
        this.customTabs || []
      )
      this.navTabOrder = merged
      if (this.projectId) writeTabOrder(this.projectId, merged)
    },
    setWorkTabVisible(visible) {
      this.workTabVisible = !!visible
      writeWorkHidden(this.projectId, !this.workTabVisible)
      this.ensureAtLeastOneTab()
      this.ensureNavTabOrder()
    },
    widgetTitleOf(widget) {
      const type = typeof widget === 'string' ? widget : widget?.type
      if (this.config?.preset === 'developer' && type === 'recent-commits') {
        return this.$t('workbench.widget.my-commits')
      }
      const def = getWidgetDef(type)
      return def ? this.$t(def.titleKey) : type
    },
    widgetMetaOf(widget) {
      const type = typeof widget === 'string' ? widget : widget?.type
      if (type === 'team-weekly-report') return this.weekRangeLabel
      return ''
    },
    widgetComponentOf(type) {
      return getWidgetDef(type)?.component || null
    },
    widgetPropsOf(widget, editing = false) {
      const type = typeof widget === 'string' ? widget : widget?.type
      const def = getWidgetDef(type)
      if (!def) return {}
      const props = {
        widgetId: (typeof widget === 'object' && widget?.instanceId) || type,
        ...(def.props || {}),
        ...((typeof widget === 'object' && widget?.props) || {})
      }
      if (def.boardChart) props.boardChart = def.boardChart
      if (this.config?.preset === 'developer' && type === 'recent-commits') {
        props.scope = 'me'
      }
      if (type === 'team-weekly-report') {
        props.showRange = false
      }
      if (type === 'document-pin') {
        props.docId = (typeof widget === 'object' && widget?.props?.docId) ?? null
        props.editing = !!editing
      }
      return props
    },
    async bootstrap() {
      this.projectId = getCurrentProjectId()
      const storeRoles = useUserStore().roles || []
      let fallback = presetIdFromRoleKeys(storeRoles)
      this.tabsReady = false
      if (!this.projectId) {
        this.config = buildConfigFromPreset(fallback)
        this.workTabVisible = true
        this.ensureNavTabOrder()
        this.activeTab = 'work'
        this.tabsReady = true
        return
      }
      this.loadingConfig = true
      try {
        const [layoutRes, tabsRes] = await Promise.all([
          getWorkbenchLayout(this.projectId),
          listWorkbenchTabs(this.projectId).catch(() => ({ data: {} }))
        ])
        const tabsData = tabsRes.data || tabsRes || {}
        this.customTabs = tabsData.tabs || []
        this.sharedCount = Number(tabsData.sharedCount) || 0
        this.sharedLimit = Number(tabsData.sharedLimit) || 24
        this.canManageShared = !!tabsData.canManageShared
        this.projectRoleKeys = tabsData.projectRoleKeys || []
        if (this.projectRoleKeys.length) {
          fallback = presetIdFromRoleKeys(this.projectRoleKeys)
        }

        const data = layoutRes.data || layoutRes || {}
        let raw = null
        if (data.configJson) {
          try {
            raw = typeof data.configJson === 'string' ? JSON.parse(data.configJson) : data.configJson
          } catch (e) {
            raw = null
          }
        }
        const { config, changed } = alignLegacyConfig(raw, fallback)
        this.config = config
        if (changed && config) {
          saveWorkbenchLayout(this.projectId, config).catch(() => {})
        }

        this.workTabVisible = !readWorkHidden(this.projectId)
        this.ensureAtLeastOneTab()
        this.ensureNavTabOrder()
        const cached = readTabCache(this.projectId)
        this.activeTab = this.resolveActiveTab(cached)
        writeTabCache(this.projectId, this.activeTab)
      } catch (e) {
        this.config = buildConfigFromPreset(fallback)
        this.workTabVisible = true
        this.ensureNavTabOrder()
        this.activeTab = 'work'
      } finally {
        this.loadingConfig = false
        this.tabsReady = true
        this.$nextTick(() => this.bindTabSortable())
      }
    },
    async refreshTabs() {
      if (!this.projectId) return
      try {
        const res = await listWorkbenchTabs(this.projectId)
        const data = res.data || res || {}
        this.customTabs = data.tabs || []
        this.sharedCount = Number(data.sharedCount) || 0
        this.sharedLimit = Number(data.sharedLimit) || 24
        this.canManageShared = !!data.canManageShared
        this.projectRoleKeys = data.projectRoleKeys || []
        this.ensureAtLeastOneTab()
        this.ensureNavTabOrder()
        this.activeTab = this.resolveActiveTab(this.activeTab)
      } catch (e) {
        /* keep */
      }
    },
    onTabCreated(tab) {
      this.refreshTabs().then(() => {
        const id = String(tab.tabId)
        if (!this.navTabOrder.includes(id)) {
          this.navTabOrder = [...this.navTabOrder, id]
          writeTabOrder(this.projectId, this.navTabOrder)
        }
        this.activeTab = id
        writeTabCache(this.projectId, id)
        this.startEdit()
      })
    },
    canEditTab(tab) {
      if (!tab) return false
      if (tab.visibility === 'private') return true
      return this.canManageShared
    },
    destroyTabSortable() {
      if (this.tabSortable) {
        const nav = this.tabSortable.el
        if (nav && nav.removeEventListener) {
          nav.removeEventListener('selectstart', this.onTabNavSelectStart)
        }
        this.tabSortable.destroy()
        this.tabSortable = null
      }
    },
    tabNavItemEl(el) {
      if (!el || el.nodeType !== 1) return null
      if (el.classList && el.classList.contains('el-tabs__item')) return el
      return typeof el.closest === 'function' ? el.closest('.el-tabs__item') : null
    },
    tabNameFromNavEl(el) {
      const item = this.tabNavItemEl(el)
      if (!item || !item.id) return null
      const name = String(item.id).replace(/^tab-/, '')
      if (!name || name === ADD_TAB_PANE) return null
      return name
    },
    tabFromNavEl(el) {
      const name = this.tabNameFromNavEl(el)
      if (!name || name === WORK_TAB_KEY) return null
      return (this.customTabs || []).find(t => String(t.tabId) === String(name)) || null
    },
    isTabNavDraggable(el) {
      if (this.editing || this.tabSortSaving) return false
      if (this.visibleTabCount < 2) return false
      const name = this.tabNameFromNavEl(el)
      if (!name) return false
      // 工作台、任意可见自定义页签均可拖（个人条顺序）；加号除外
      if (name === WORK_TAB_KEY) return this.workTabVisible
      return (this.customTabs || []).some(t => String(t.tabId) === String(name))
    },
    /** 把 nav DOM 恢复成当前 Vue 个人顺序，避免 Sortable 挪节点后与 VNode/i18n 错位 */
    syncTabNavDomToData(nav) {
      if (!nav) return
      const expected = [
        ...(this.navTabOrder || []),
        ...(this.editing ? [] : [ADD_TAB_PANE])
      ]
      const map = new Map()
      for (const el of [...nav.querySelectorAll('.el-tabs__item')]) {
        map.set(String(el.id || '').replace(/^tab-/, ''), el)
      }
      const bar = nav.querySelector('.el-tabs__active-bar')
      for (const name of expected) {
        const el = map.get(name)
        if (el) nav.appendChild(el)
      }
      if (bar) nav.insertBefore(bar, nav.firstChild)
    },
    onTabNavSelectStart(evt) {
      evt.preventDefault()
    },
    /** 给可拖页签打标记：光标用 */
    refreshTabSortableClasses(nav) {
      if (!nav) return
      for (const el of nav.querySelectorAll('.el-tabs__item')) {
        if (this.isTabNavDraggable(el)) el.classList.add('is-wb-sortable')
        else el.classList.remove('is-wb-sortable')
      }
    },
    bindTabSortable() {
      this.destroyTabSortable()
      if (this.editing || !this.projectId || this.tabSortSaving) return
      const root = this.$el
      if (!root || !root.querySelector) return
      const nav = root.querySelector('.workbench-tabs .el-tabs__nav')
      if (!nav) return
      const self = this
      this.refreshTabSortableClasses(nav)
      nav.addEventListener('selectstart', this.onTabNavSelectStart)
      this.tabSortable = Sortable.create(nav, {
        animation: 150,
        forceFallback: true,
        fallbackOnBody: true,
        fallbackTolerance: 0,
        direction: 'horizontal',
        swapThreshold: 0.65,
        supportPointer: false,
        draggable: '.el-tabs__item',
        filter(evt, target) {
          const origin = evt && (evt.target || evt.srcElement)
          if (origin && typeof origin.closest === 'function' && origin.closest('.wb-tab-menu')) {
            return true
          }
          const item = self.tabNavItemEl(target) || self.tabNavItemEl(origin)
          return !self.isTabNavDraggable(item)
        },
        preventOnFilter: false,
        onStart() {
          try {
            window.getSelection && window.getSelection().removeAllRanges()
          } catch (e) { /* ignore */ }
        },
        onMove(evt) {
          const relatedName = self.tabNameFromNavEl(evt.related)
          // 不可插到加号上；其余（工作台 / 共享 / 私有）均可互插
          if (!relatedName || relatedName === ADD_TAB_PANE) return false
          const dragName = self.tabNameFromNavEl(evt.dragged)
          if (!dragName || dragName === ADD_TAB_PANE) return false
          return true
        },
        onEnd(evt) {
          self.onTabSortEnd(evt)
        }
      })
    },
    async onTabSortEnd(evt) {
      if (this.tabSortSaving) return
      const nav = evt.to || evt.from
      if (!nav) return
      const order = [...nav.querySelectorAll('.el-tabs__item')]
        .map(el => String(el.id || '').replace(/^tab-/, ''))
        .filter(id => id && id !== ADD_TAB_PANE)
      this.destroyTabSortable()
      this.navTabOrder = order
      writeTabOrder(this.projectId, order)
      this.syncTabNavDomToData(nav)

      const byId = new Map((this.customTabs || []).map(t => [String(t.tabId), t]))
      const privateOrder = order.map(id => byId.get(id)).filter(t => t && t.visibility === 'private')
      const sharedOrder = order.map(id => byId.get(id)).filter(t => t && t.visibility === 'project')

      const relativeChanged = (group, prevList) => {
        if (group.length === 0) return false
        if (group.length !== prevList.length) return true
        return group.some((t, i) => String(t.tabId) !== String(prevList[i].tabId))
      }

      const payloads = []
      const base = Date.now()
      let offset = 0
      const pushGroup = (group, prevList, allowed) => {
        if (!allowed || !relativeChanged(group, prevList)) return
        for (let i = 0; i < group.length; i++) {
          payloads.push({ tabId: group[i].tabId, tabSort: base - offset - i, _vis: group[i].visibility })
        }
        offset += group.length
      }
      pushGroup(privateOrder, this.privateTabs, true)
      pushGroup(sharedOrder, this.sharedTabs, this.canManageShared)

      if (payloads.length === 0) {
        this.$nextTick(() => this.bindTabSortable())
        return
      }

      const sortMap = new Map(payloads.map(p => [String(p.tabId), p.tabSort]))
      this.tabSortSaving = true
      this.customTabs = (this.customTabs || []).map(t => {
        const s = sortMap.get(String(t.tabId))
        return s == null ? t : { ...t, tabSort: s }
      })
      try {
        const privPayload = payloads
          .filter(p => p._vis === 'private')
          .map(({ tabId, tabSort }) => ({ tabId, tabSort }))
        const sharePayload = payloads
          .filter(p => p._vis === 'project')
          .map(({ tabId, tabSort }) => ({ tabId, tabSort }))
        // 后端禁止单次请求跨 visibility，分组提交
        if (privPayload.length) await sortWorkbenchTabs(this.projectId, privPayload)
        if (sharePayload.length) await sortWorkbenchTabs(this.projectId, sharePayload)
      } catch (e) {
        await this.refreshTabs()
      } finally {
        this.tabSortSaving = false
        this.$nextTick(() => this.bindTabSortable())
      }
    },
    onTabMenuCommand(command) {
      const raw = String(command || '')
      const sep = raw.indexOf(':')
      if (sep < 0) return
      const action = raw.slice(0, sep)
      const tabKey = raw.slice(sep + 1)
      if (action === 'edit') {
        this.editTabSurface(tabKey)
        return
      }
      if (action === 'delete') {
        if (tabKey === 'work') this.deleteWorkTab()
        else this.deleteTabById(tabKey)
      }
    },
    editTabSurface(tabKey) {
      if (this.editing) return
      const key = String(tabKey || 'work')
      if (key !== 'work') {
        const tab = (this.customTabs || []).find(t => String(t.tabId) === key)
        if (!tab || !this.canEditTab(tab)) return
      }
      this.activeTab = key
      writeTabCache(this.projectId, key)
      this.$nextTick(() => this.startEdit())
    },
    startEdit() {
      if (!this.canEditCurrentSurface) return
      if (this.isWorkSurface) {
        this.draft = JSON.parse(JSON.stringify(this.config || buildConfigFromPreset('tester')))
        this.draft.version = CONFIG_VERSION
        if (!this.draft.tabs) this.draft.tabs = { work: { rows: [] } }
        if (!this.draft.tabs.work) this.draft.tabs.work = { rows: [] }
        this.draftRowsLocal = normalizeRows(this.draft.tabs.work.rows)
        this.draftTabName = ''
      } else {
        const tab = this.currentCustomTab
        let raw = tab?.configJson
        if (typeof raw === 'string') {
          try {
            raw = JSON.parse(raw)
          } catch (e) {
            raw = null
          }
        }
        const parsed = parseTabConfig(raw) || buildTabConfigFromPreset(tab?.preset || 'custom')
        this.draft = { ...parsed }
        this.draftRowsLocal = normalizeRows(parsed.rows)
        this.draftTabName = String(tab?.tabName || '').slice(0, 12)
      }
      this.editing = true
    },
    cancelEdit() {
      this.editing = false
      this.draft = null
      this.draftRowsLocal = []
      this.draftTabName = ''
    },
    normalizeDraftTabName() {
      const name = String(this.draftTabName || '').trim()
      if (!name) {
        this.$modal.msgError(this.$t('workbench.tab-dialog.name-required'))
        return null
      }
      if (name.length > 12) {
        this.$modal.msgError(this.$t('workbench.tab-dialog.name-max'))
        return null
      }
      return name
    },
    onDraftRows(rows) {
      this.draftRowsLocal = normalizeRows(rows)
      if (this.isWorkSurface && this.draft?.tabs?.work) {
        this.draft.tabs.work.rows = this.draftRowsLocal
      } else if (this.draft) {
        this.draft.rows = this.draftRowsLocal
      }
    },
    async saveEdit() {
      if (!this.projectId || !this.draft) return
      this.saving = true
      try {
        const rows = normalizeRows(this.draftRowsLocal)
        if (this.isWorkSurface) {
          const payload = {
            version: CONFIG_VERSION,
            preset: this.draft.preset || this.config?.preset || 'tester',
            tabs: { work: { rows } }
          }
          await saveWorkbenchLayout(this.projectId, payload)
          this.config = payload
        } else {
          const tab = this.currentCustomTab
          const tabName = this.normalizeDraftTabName()
          if (!tabName) return
          const payload = {
            version: CONFIG_VERSION,
            preset: this.draft.preset || tab?.preset || 'custom',
            rows
          }
          const res = await updateWorkbenchTab(this.projectId, tab.tabId, {
            tabName,
            configJson: payload
          })
          const updated = res.data || res
          const idx = this.customTabs.findIndex(t => String(t.tabId) === String(tab.tabId))
          if (idx >= 0) {
            this.customTabs.splice(idx, 1, {
              ...this.customTabs[idx],
              ...updated,
              tabName,
              configJson: payload
            })
          }
        }
        this.editing = false
        this.draft = null
        this.draftRowsLocal = []
        this.draftTabName = ''
        this.$modal.msgSuccess(this.$t('workbench.saved'))
      } catch (e) {
        this.$modal.msgError(this.$t('workbench.save-failed'))
      } finally {
        this.saving = false
      }
    },
    onPresetCommand(cmd) {
      if (!cmd || !cmd.startsWith('apply:')) return
      if (!this.canEditCurrentSurface) return
      const presetId = cmd.slice(6)
      this.$confirm(this.$t('workbench.preset-confirm'), this.$t('workbench.preset-label'), {
        type: 'warning'
      })
        .then(async () => {
          const rows = rowsFromPreset(presetId)
          if (this.isWorkSurface) {
            const next = buildConfigFromPreset(presetId)
            next.tabs.work.rows = rows
            if (this.projectId) await saveWorkbenchLayout(this.projectId, next)
            this.config = next
          } else {
            const tab = this.currentCustomTab
            const payload = buildTabConfigFromPreset(presetId)
            payload.rows = rows
            payload.preset = presetId
            await updateWorkbenchTab(this.projectId, tab.tabId, {
              preset: presetId,
              configJson: payload
            })
            await this.refreshTabs()
          }
          this.editing = false
          this.draft = null
          this.draftRowsLocal = []
          this.$modal.msgSuccess(this.$t('workbench.saved'))
        })
        .catch(() => {})
    },
    deleteWorkTab() {
      if (!this.canDeleteMoreTabs || !this.workTabVisible) return
      this.$confirm(this.$t('workbench.tab-delete-confirm'), this.$t('workbench.tab-delete'), {
        type: 'warning'
      })
        .then(() => {
          this.setWorkTabVisible(false)
          if (this.activeTab === 'work') {
            this.activeTab = this.resolveActiveTab(null)
            writeTabCache(this.projectId, this.activeTab)
          }
          this.$modal.msgSuccess(this.$t('workbench.saved'))
        })
        .catch(() => {})
    },
    deleteTabById(tabId) {
      const tab = (this.customTabs || []).find(t => String(t.tabId) === String(tabId))
      if (!tab || !this.canEditTab(tab) || !this.canDeleteMoreTabs) return
      this.$confirm(this.$t('workbench.tab-delete-confirm'), this.$t('workbench.tab-delete'), {
        type: 'warning'
      })
        .then(async () => {
          await deleteWorkbenchTab(this.projectId, tab.tabId)
          await this.refreshTabs()
          if (String(this.activeTab) === String(tab.tabId)) {
            this.activeTab = this.resolveActiveTab(null)
            writeTabCache(this.projectId, this.activeTab)
          }
          this.$modal.msgSuccess(this.$t('workbench.saved'))
        })
        .catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
@import './workbench/workbench-proto.scss';

.workbench-page {
  min-height: 100%;
}
.workbench-header {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 12px;
}
.workbench-header__bar {
  display: flex;
  align-items: center;
  min-width: 0;
}
.workbench-header__project {
  margin: 0;
  min-width: 0;
}
.workbench-hello {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  min-width: 0;
  margin: 0;
  padding: 12px 16px;
  box-sizing: border-box;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter, rgba(255, 255, 255, 0.08));
  background: var(--el-bg-color, #141414);
}
html:not(.dark) .workbench-hello {
  background: #fff;
  border-color: var(--el-border-color-lighter, #ebeef5);
}
.workbench-hello__avatar {
  flex-shrink: 0;
  border-radius: 50%;
}
.workbench-hello__copy {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.workbench-hello__line {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}
.workbench-hello__salute {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-color-secondary);
}
.workbench-hello__name {
  min-width: 0;
  font-size: 17px;
  font-weight: 650;
  color: var(--text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.workbench-hello__date {
  font-size: 12px;
  color: var(--text-color-secondary);
}
/* 对齐缺陷页 .defect-tools-tab：整行 40px + 底线贯通到右侧按钮下 */
.workbench-tools-tab {
  position: relative;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  min-width: 0;
  height: 40px;
  margin-bottom: 8px;
  box-sizing: border-box;
  &::after {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 2px;
    background-color: var(--el-border-color-light, #ebeef5);
    pointer-events: none;
    z-index: 1;
  }
}
.workbench-tabs {
  flex: 1 1 auto;
  min-width: 0;
  height: 100%;
  margin-bottom: 0;
}
.workbench-tabs :deep(.el-tabs__header) {
  margin-bottom: 0 !important;
  height: 100%;
}
.workbench-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none !important;
}
.workbench-tabs :deep(.el-tabs__nav-wrap),
.workbench-tabs :deep(.el-tabs__nav-scroll),
.workbench-tabs :deep(.el-tabs__nav) {
  height: 100%;
}
.workbench-tabs :deep(.el-tabs__item) {
  height: 40px;
  line-height: 40px;
  user-select: none;
  -webkit-user-select: none;
}
.workbench-tabs :deep(.el-tabs__item.is-wb-sortable) {
  cursor: grab;
}
.workbench-tabs :deep(.el-tabs__item.is-wb-sortable.sortable-chosen),
.workbench-tabs :deep(.el-tabs__item.is-wb-sortable.sortable-drag) {
  cursor: grabbing;
}
.workbench-tabs :deep(.el-tabs__item.sortable-ghost) {
  opacity: 0.35;
}
.workbench-tabs :deep(.wb-tab-text),
.workbench-tabs :deep(.wb-tab-label) {
  user-select: none;
  -webkit-user-select: none;
}
.workbench-tabs :deep(.el-tabs__active-bar) {
  z-index: 2;
  /* 避免切换/首屏激活条滑动动画 */
  transition: none !important;
}
.workbench-tabs :deep(.el-tabs__content) {
  display: none;
}
.workbench-tabs :deep(#tab-__workbench_add_tab__.el-tabs__item) {
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  cursor: default;
}
.workbench-tools-tab-right {
  position: relative;
  z-index: 2;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 8px;
  height: 100%;
  box-sizing: border-box;
}
.workbench-tab-rename {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-right: 4px;
}
.workbench-tab-rename__label {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-color-secondary);
  white-space: nowrap;
}
.workbench-tab-rename__input {
  width: 176px;
  height: 28px;
}
.workbench-tab-rename__input :deep(.el-input__wrapper) {
  height: 28px;
  min-height: 28px;
  padding: 0 8px;
  box-shadow: 0 0 0 1px var(--el-border-color) inset;
}
.workbench-tab-rename__input :deep(.el-input__inner) {
  height: 26px;
  line-height: 26px;
  font-size: 13px;
}
.workbench-tab-rename__input :deep(.el-input__count) {
  height: 20px;
  line-height: 20px;
  font-size: 11px;
}
.workbench-tab-rename__input :deep(.el-input__suffix) {
  display: inline-flex;
  align-items: center;
}
.workbench-tools-tab-right :deep(.el-button + .el-button) {
  margin-left: 0;
}
.workbench-tools-tab-right :deep(.el-button) {
  min-height: 28px;
  height: 28px;
  padding: 5px 11px;
  font-size: 13px;
}
.wb-tab-add-btn {
  width: 15px;
  height: 15px;
  vertical-align: middle;
  cursor: pointer;
}
.wb-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 200px;
  vertical-align: bottom;
}
.wb-tab-kind {
  flex: 0 0 auto;
  width: 14px;
  height: 14px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  &.is-shared {
    color: var(--el-color-primary);
  }
  &.is-private {
    color: var(--el-text-color-secondary);
  }
}
/* 激活页签：图标跟文字同色（主色），覆盖私有默认灰 */
.workbench-tabs :deep(.el-tabs__item.is-active) .wb-tab-kind,
.workbench-tabs :deep(.el-tabs__item.is-active) .wb-tab-kind.is-private,
.workbench-tabs :deep(.el-tabs__item.is-active) .wb-tab-kind.is-shared {
  color: var(--el-color-primary);
}
.wb-tab-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.wb-tab-menu {
  display: none;
  flex: 0 0 auto;
  align-items: center;
  vertical-align: middle;
  line-height: 1;
}
.workbench-tabs :deep(.el-tabs__item:hover) .wb-tab-menu,
.wb-tab-menu:has([aria-expanded='true']) {
  display: inline-flex;
}
.wb-tab-menu-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  width: 18px;
  height: 18px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  border: 1px solid var(--el-border-color);
  border-radius: var(--cat2bug-border-radius, 4px);
  background: var(--el-bg-color);
  outline: none;
  &:hover {
    color: var(--el-color-primary);
    border-color: var(--el-color-primary);
  }
}
.wb-tab-menu-danger {
  color: var(--el-color-danger);
  :deep(.el-icon) {
    color: var(--el-color-danger);
  }
}
.workbench-page > .list-page-toolbar {
  margin-top: 0;
  margin-bottom: var(--project-list-section-v-gap, 10px);
}
.stats-inline {
  font-size: 12px;
  color: var(--text-color-secondary);
}
.workbench-edit-hint {
  margin-bottom: 10px;
}
</style>
