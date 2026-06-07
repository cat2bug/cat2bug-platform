/**
 * 快捷键引擎：全局监听引导键并控制命令面板状态。
 *
 * 职责划分：
 *   - 本服务：面板关闭时检测引导键（g / Space），打开对应命令面板；维护面板响应式状态。
 *   - CommandPalette 组件：面板打开后接管按键（字母直达 / 搜索 / 方向键 / Esc），
 *     并通过 executor 执行与 Vue 上下文相关的副作用（路由跳转、主题、语言、登出等）。
 */
import Vue from 'vue'
import i18n from '@/utils/i18n/i18n'
import { shortcutStore } from './shortcut-store'
import { pageRegistry } from './registry'
import { buildNavItems, navLeader, actionLeader } from './nav-source'
import {
  DEFECT_ACTION_DEFAULTS,
  LOGIN_ACTION_DEFAULTS,
  REGISTER_ACTION_DEFAULTS,
  NOTICE_ACTION_DEFAULTS,
  STATISTIC_TEMPLATE_ACTION_DEFAULTS,
  normalizeKey,
  assignLetters
} from './keymap'

const palette = Vue.observable({
  open: false,
  mode: '', // 'nav' | 'action'
  stack: [], // [{ title, items: [...] }]
  query: ''
})

let executor = null
let started = false

function isEditableElement(el) {
  if (!el || el === document.body) return false
  const tag = el.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return true
  if (el.isContentEditable) return true
  return false
}

/** 是否处于输入态：聚焦输入控件 / IME 组合输入中 */
function inEditableContext(event) {
  if (event && event.isComposing) return true
  if (isEditableElement(document.activeElement)) return true
  return false
}

/** 是否聚焦在原生用 Space/Enter 触发的交互元素上（避免动作引导键劫持按钮/链接） */
function onInteractiveElement() {
  const el = document.activeElement
  if (!el || el === document.body) return false
  const tag = el.tagName
  if (tag === 'BUTTON' || tag === 'A') return true
  const role = el.getAttribute && el.getAttribute('role')
  if (role === 'button' || role === 'link' || role === 'menuitem') return true
  return false
}

function isVisibleLayer(el) {
  if (!el || !el.getBoundingClientRect) return false
  if (el.getAttribute('aria-hidden') === 'true') return false
  const style = window.getComputedStyle(el)
  if (style.display === 'none' || style.visibility === 'hidden') return false
  const rect = el.getBoundingClientRect()
  return rect.width > 0 && rect.height > 0
}

/**
 * 是否存在遮挡层（弹框 / 抽屉 / 下拉浮层等）。
 * 缺陷页「页面动作」仅在一级列表、无任何遮罩时响应空格。
 */
const BLOCKING_UI_LAYER_SELECTORS = [
  '.el-dialog__wrapper',
  '.el-drawer__wrapper',
  '.el-message-box__wrapper',
  '.cmdp-mask',
  '.select-project-member-popover',
  '.select-module-popover',
  '.el-select-dropdown',
  '.el-picker-panel',
  '.el-dropdown-menu',
  '.el-autocomplete-suggestion',
  '.el-cascader-menus',
  '.el-color-picker__panel',
  '.defect-column-picker-popover',
  '.defect-excel-column-picker-popover',
  /* 缺陷页统计模块 G 导航：空格用于触发模块点击，勿打开动作面板 */
  '.defect-statistic-keyboard-nav',
  /* 统计模版页键盘导航：空格用于添加到预览区，勿打开动作面板 */
  '.statistic-template-kbd-nav'
]

/** 新建/编辑缺陷表单抽屉（Esc 关闭时不应把自身算作遮挡层） */
function isDefectFormDrawerWrapper(el) {
  if (!el || !el.querySelector) return false
  if (!el.classList || !el.classList.contains('el-drawer__wrapper')) return false
  return !!(el.querySelector('.defect-add-header, .defect-edit-form-header') && isVisibleLayer(el))
}

/** 处理缺陷详情抽屉（工具弹框 Esc 取消时，下层抽屉不算遮挡层） */
function isHandleDefectDrawerWrapper(el) {
  if (!el || !el.querySelector) return false
  if (!el.classList || !el.classList.contains('el-drawer__wrapper')) return false
  return !!(el.querySelector('.defect-edit-header') && isVisibleLayer(el))
}

const DEFECT_TOOL_DIALOG_NAMES = new Set([
  'AssignDialog',
  'RepairDialog',
  'RejectDialog',
  'PassDialog',
  'CloseDialog',
  'OpenDialog'
])

const STATISTIC_DIALOG_NAMES = new Set([
  'PersonalRemindTimer',
  'MyLife'
])

const FORM_SHORTCUT_DIALOG_NAMES = new Set([
  ...DEFECT_TOOL_DIALOG_NAMES,
  ...STATISTIC_DIALOG_NAMES
])

/** 缺陷工具栏操作弹框（Esc 关闭时不应把自身算作遮挡层） */
function isDefectToolDialogWrapper(el) {
  if (!el || !el.classList || !el.classList.contains('el-dialog__wrapper')) return false
  if (!isVisibleLayer(el)) return false
  // Element UI 2.x：AssignDialog 等挂在 wrapper.__vue__ 链上，.el-dialog 节点无 __vue__
  let vm = el.__vue__
  while (vm) {
    const name = vm.$options && vm.$options.name
    if (FORM_SHORTCUT_DIALOG_NAMES.has(name)) return true
    vm = vm.$parent
  }
  return false
}

function hasBlockingUiLayer(options = {}) {
  const {
    excludeDefectFormDrawer = false,
    excludeDefectToolDialog = false,
    excludeHandleDefectDrawer = false
  } = options
  return BLOCKING_UI_LAYER_SELECTORS.some((sel) =>
    Array.from(document.querySelectorAll(sel)).some((el) => {
      if (!isVisibleLayer(el)) return false
      if (excludeDefectFormDrawer && isDefectFormDrawerWrapper(el)) return false
      if (excludeHandleDefectDrawer && isHandleDefectDrawerWrapper(el)) return false
      if (excludeDefectToolDialog && isDefectToolDialogWrapper(el)) return false
      return true
    })
  )
}

export { hasBlockingUiLayer }

/** 缺陷页空格打开「页面动作」的前置条件 */
function canOpenDefectPageActions() {
  if (!pageRegistry.hasActions()) return false
  if (hasBlockingUiLayer()) return false
  if (onInteractiveElement()) return false
  return true
}

function findPageActionDefault(key, scopeKey) {
  if (scopeKey === 'login') {
    return LOGIN_ACTION_DEFAULTS.find((d) => d.key === key)
  }
  if (scopeKey === 'register') {
    return REGISTER_ACTION_DEFAULTS.find((d) => d.key === key)
  }
  if (scopeKey === 'statistic-template') {
    return STATISTIC_TEMPLATE_ACTION_DEFAULTS.find((d) => d.key === key)
  }
  if (scopeKey === 'notice') {
    return NOTICE_ACTION_DEFAULTS.find((d) => d.key === key)
  }
  return DEFECT_ACTION_DEFAULTS.find((d) => d.key === key)
}

/** 缺陷等页面注册的动作 → 解析为带唯一字母的面板项 */
function buildActionItems() {
  const registered = pageRegistry.getActiveActions()
  if (!registered.length) return []
  const list = registered.map((a) => {
    const def = findPageActionDefault(a.key, a.scopeKey)
    const titleKey = a.titleKey || (def && def.titleKey)
    const bindingId = `action.${a.scopeKey}.${a.key}`
    return {
      bindingId,
      type: 'action',
      title: a.title || (titleKey ? i18n.t(titleKey) : a.key),
      run: a.run,
      preferred: shortcutStore.getLetter(bindingId, a.defaultLetter || (def && def.defaultLetter)),
      defaultLetter: a.defaultLetter || (def && def.defaultLetter)
    }
  })
  assignLetters(list)
  return list
}

function currentLevel() {
  return palette.stack.length ? palette.stack[palette.stack.length - 1] : null
}

export const shortcutService = {
  palette,

  start() {
    if (started) return
    started = true
    document.addEventListener('keydown', this._onLeaderKeydown, true)
  },

  stop() {
    if (!started) return
    started = false
    document.removeEventListener('keydown', this._onLeaderKeydown, true)
  },

  setExecutor(fn) {
    executor = fn
  },

  getExecutor() {
    return executor
  },

  _onLeaderKeydown(event) {
    if (!shortcutStore.isEnabled()) return
    if (palette.open) return // 打开后由组件接管
    if (inEditableContext(event)) return
    if (event.metaKey || event.ctrlKey || event.altKey) return

    const key = event.key
    const nav = navLeader()
    const action = actionLeader()

    // 导航引导键（默认 g）
    if (nav && key && key.toLowerCase() === String(nav).toLowerCase()) {
      event.preventDefault()
      event.stopPropagation()
      shortcutService.openNav()
      return
    }
    // 动作引导键（默认 Space）
    const isActionLeader =
      (action === 'Space' && (key === ' ' || key === 'Spacebar')) ||
      (action && key && key.toLowerCase() === String(action).toLowerCase())
    if (isActionLeader && canOpenDefectPageActions()) {
      event.preventDefault()
      event.stopPropagation()
      shortcutService.openAction()
    }
  },

  openNav() {
    palette.mode = 'nav'
    palette.query = ''
    palette.stack = [{ title: i18n.t('keyboard.palette.go-to'), items: buildNavItems() }]
    palette.open = true
  },

  openAction() {
    const items = buildActionItems()
    if (!items.length) return
    palette.mode = 'action'
    palette.query = ''
    palette.stack = [{ title: i18n.t('keyboard.palette.actions'), items }]
    palette.open = true
  },

  /** 供外部按钮/帮助调用 */
  open(mode) {
    if (mode === 'action') this.openAction()
    else this.openNav()
  },

  close() {
    palette.open = false
    palette.stack = []
    palette.query = ''
    palette.mode = ''
  },

  back() {
    if (palette.stack.length > 1) {
      palette.stack = palette.stack.slice(0, -1)
      palette.query = ''
    } else {
      this.close()
    }
  },

  setQuery(q) {
    palette.query = q
  },

  currentItems() {
    const level = currentLevel()
    return level ? level.items : []
  },

  /** 按字母选择当前层某项 */
  selectLetter(rawLetter) {
    const letter = normalizeKey(rawLetter)
    if (!letter) return false
    const items = this.currentItems()
    const item = items.find((it) => it.letter === letter)
    if (!item) return false
    this.activate(item)
    return true
  },

  /** 激活某项：下拉进二级，叶子执行并关闭 */
  activate(item) {
    if (!item) return
    if (item.type === 'dropdown' && item.children && item.children.length) {
      palette.stack = [...palette.stack, { title: item.title, items: item.children }]
      palette.query = ''
      return
    }
    if (executor) {
      try {
        executor(item)
      } catch (e) {
        /* 执行错误不应卡住面板 */
      }
    }
    this.close()
  }
}
