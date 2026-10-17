/**
 * Element UI 字符串图标（el-icon-*）→ Element Plus @element-plus/icons-vue 组件。
 * 模板用法：:icon="$legacyIcon('el-icon-delete')"
 * 只注册源码里出现过的图标，避免整包 icons-vue 打进产物。
 */
import { markRaw } from 'vue'
import {
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  ArrowUp,
  Box,
  Calendar,
  CaretBottom,
  CaretRight,
  CaretTop,
  ChatLineRound,
  Check,
  CircleCheck,
  CircleClose,
  CirclePlus,
  Close,
  Cloudy,
  CoffeeCup,
  Coin,
  Collection,
  Connection,
  CopyDocument,
  Cpu,
  DArrowLeft,
  DArrowRight,
  DataLine,
  Delete,
  Discount,
  Document,
  DocumentAdd,
  DocumentChecked,
  DocumentCopy,
  DocumentDelete,
  Download,
  Edit,
  EditPen,
  Eleme,
  Files,
  Finished,
  Flag,
  Fold,
  Folder,
  HomeFilled,
  InfoFilled,
  Iphone,
  Key,
  Link,
  List,
  Loading,
  Lock,
  Magnet,
  Menu,
  Minus,
  Monitor,
  More,
  Notebook,
  Odometer,
  Operation,
  Paperclip,
  Phone,
  Picture,
  PieChart,
  Platform,
  Plus,
  Pointer,
  Promotion,
  QuestionFilled,
  Refresh,
  RefreshLeft,
  RefreshRight,
  Remove,
  Right,
  Scissor,
  Search,
  Select,
  Setting,
  Sort,
  Star,
  StarFilled,
  SuccessFilled,
  TakeawayBox,
  Tickets,
  Timer,
  Unlock,
  Upload,
  User,
  UserFilled,
  VideoPlay,
  View,
  WarningFilled
} from '@element-plus/icons-vue'

/** 无法靠 kebab→Pascal 自动推断的遗留图标 */
const LEGACY_ICON_MAP = {
  'el-icon-s-fold': 'Fold',
  'el-icon-s-home': 'HomeFilled',
  'el-icon-s-order': 'List',
  'el-icon-s-operation': 'Operation',
  'el-icon-s-platform': 'Platform',
  'el-icon-s-data': 'DataLine',
  'el-icon-s-flag': 'Flag',
  'el-icon-s-promotion': 'Promotion',
  'el-icon-s-check': 'Select',
  'el-icon-d-arrow-left': 'DArrowLeft',
  'el-icon-d-arrow-right': 'DArrowRight',
  'el-icon-circle-plus-outline': 'CirclePlus',
  'el-icon-remove-outline': 'Remove',
  'el-icon-picture-outline': 'Picture',
  'el-icon-phone-outline': 'Phone',
  'el-icon-user-solid': 'UserFilled',
  'el-icon-upload2': 'Upload',
  'el-icon-star-on': 'StarFilled',
  'el-icon-star-off': 'Star',
  'el-icon-success': 'SuccessFilled',
  'el-icon-error': 'CircleClose',
  'el-icon-attract': 'Magnet',
  'el-icon-time': 'Timer',
  'el-icon-thumb': 'Pointer',
  'el-icon-receiving': 'Box',
  'el-icon-date': 'Calendar',
  'el-icon-notebook-2': 'Notebook',
  'el-icon-scissors': 'Scissor',
  'el-icon-copy-document': 'CopyDocument',
  'el-icon-document-add': 'DocumentAdd',
  'el-icon-chat-line-round': 'ChatLineRound',
  'el-icon-caret-bottom': 'CaretBottom',
  'el-icon-caret-right': 'CaretRight',
  'el-icon-caret-top': 'CaretTop',
  'el-icon-arrow-down': 'ArrowDown',
  'el-icon-arrow-left': 'ArrowLeft',
  'el-icon-arrow-right': 'ArrowRight',
  'el-icon-arrow-up': 'ArrowUp',
  'el-icon-circle-close': 'CircleClose',
  'el-icon-circle-check': 'CircleCheck',
  'el-icon-circle-plus': 'CirclePlus',
  'el-icon-refresh-left': 'RefreshLeft',
  'el-icon-refresh-right': 'RefreshRight',
  'el-icon-takeaway-box': 'TakeawayBox',
  'el-icon-document-checked': 'DocumentChecked',
  'el-icon-document-copy': 'DocumentCopy',
  'el-icon-document-delete': 'DocumentDelete',
  'el-icon-pie-chart': 'PieChart',
  'el-icon-video-play': 'VideoPlay',
  'el-icon-info': 'InfoFilled',
  'el-icon-question': 'QuestionFilled',
  'el-icon-warning': 'WarningFilled',
  'el-icon-loading': 'Loading',
  'el-icon-edit-outline': 'EditPen',
  'el-icon-mobile': 'Iphone',
  'el-icon--right': null,
  'el-icon-wrapper': null
}

const ICONS = {
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  ArrowUp,
  Box,
  Calendar,
  CaretBottom,
  CaretRight,
  CaretTop,
  ChatLineRound,
  Check,
  CircleCheck,
  CircleClose,
  CirclePlus,
  Close,
  Cloudy,
  CoffeeCup,
  Coin,
  Collection,
  Connection,
  CopyDocument,
  Cpu,
  DArrowLeft,
  DArrowRight,
  DataLine,
  Delete,
  Discount,
  Document,
  DocumentAdd,
  DocumentChecked,
  DocumentCopy,
  DocumentDelete,
  Download,
  Edit,
  EditPen,
  Eleme,
  Files,
  Finished,
  Flag,
  Fold,
  Folder,
  HomeFilled,
  InfoFilled,
  Iphone,
  Key,
  Link,
  List,
  Loading,
  Lock,
  Magnet,
  Menu,
  Minus,
  Monitor,
  More,
  Notebook,
  Odometer,
  Operation,
  Paperclip,
  Phone,
  Picture,
  PieChart,
  Platform,
  Plus,
  Pointer,
  Promotion,
  QuestionFilled,
  Refresh,
  RefreshLeft,
  RefreshRight,
  Remove,
  Right,
  Scissor,
  Search,
  Select,
  Setting,
  Sort,
  Star,
  StarFilled,
  SuccessFilled,
  TakeawayBox,
  Tickets,
  Timer,
  Unlock,
  Upload,
  User,
  UserFilled,
  VideoPlay,
  View,
  WarningFilled
}

function legacyKeyToEpName(legacy) {
  if (Object.prototype.hasOwnProperty.call(LEGACY_ICON_MAP, legacy)) {
    return LEGACY_ICON_MAP[legacy]
  }
  const raw = legacy.replace(/^el-icon-/, '')
  if (!raw) return null
  return raw
    .split('-')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join('')
}

/**
 * @param {string | import('vue').Component | null | undefined} icon
 * @returns {import('vue').Component | undefined}
 */
export function resolveLegacyIcon(icon) {
  if (icon == null || icon === '') return undefined
  if (typeof icon !== 'string') return icon
  const trimmed = icon.trim()
  if (!trimmed.startsWith('el-icon-')) return icon
  const epName = legacyKeyToEpName(trimmed)
  if (!epName) return undefined
  const resolved = ICONS[epName]
  return resolved ? markRaw(resolved) : undefined
}

/** Element Plus 图标 path，供 cellHtml 等无法使用 Vue 组件的场景 */
const LEGACY_INLINE_SVG_PATH = {
  'el-icon-close':
    'M764.288 214.592 512 466.88 259.712 214.592a31.936 31.936 0 0 0-45.12 45.12L466.752 512 214.528 764.224a31.936 31.936 0 1 0 45.12 45.184L512 557.184l252.288 252.288a31.936 31.936 0 0 0 45.12-45.12L557.12 512.064l252.288-252.352a31.936 31.936 0 1 0-45.12-45.184z',
  'el-icon-upload2':
    'M160 832h704a32 32 0 1 1 0 64H160a32 32 0 1 1 0-64m384-578.304V704h-64V247.296L237.248 490.048 192 444.8 508.8 128l316.8 316.8-45.312 45.248z',
  'el-icon-paperclip':
    'M602.496 240.448A192 192 0 1 1 874.048 512l-316.8 316.8A256 256 0 0 1 195.2 466.752L602.496 59.456l45.248 45.248L240.448 512A192 192 0 0 0 512 783.552l316.8-316.8a128 128 0 1 0-181.056-181.056L353.6 579.904a32 32 0 1 0 45.248 45.248l294.144-294.144 45.312 45.248L444.096 670.4a96 96 0 1 1-135.744-135.744z'
}

/**
 * 将 el-icon-* 渲染为内联 SVG 字符串（用于 Excel cellHtml 等原生 HTML 注入）。
 * @param {string} legacy
 * @param {{ className?: string }} [options]
 * @returns {string}
 */
export function legacyIconInlineHtml(legacy, options = {}) {
  const d = LEGACY_INLINE_SVG_PATH[legacy]
  if (!d) return ''
  const extraClass = options.className ? ` ${options.className}` : ''
  return (
    `<svg class="cat2bug-inline-svg-icon${extraClass}" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" aria-hidden="true">` +
    `<path fill="currentColor" d="${d}"/></svg>`
  )
}
