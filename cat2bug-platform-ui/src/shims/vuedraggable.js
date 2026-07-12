/**
 * Vue 3 + Vite shim for vue-excel-editor PanelSetting (legacy vuedraggable@2 API).
 * Nested vuedraggable@2 only ships UMD without ESM default export; v4 requires #item slot.
 */
import Sortable from 'sortablejs'
import { defineComponent, h, onBeforeUnmount, onMounted, ref } from 'vue'

function resolveList(props) {
  if (props.list != null) return props.list
  if (props.modelValue != null) return props.modelValue
  if (props.value != null) return props.value
  return []
}

function emitListUpdate(emit, list) {
  emit('update:modelValue', list)
  emit('input', list)
  emit('change', list)
}

/** Sortable / vuedraggable 配置项，不能落到 DOM 根节点 */
const SORTABLE_OPTION_ATTRS = new Set([
  'draggable',
  'animation',
  'group',
  'handle',
  'ghostClass',
  'ghost-class',
  'filter',
  'sort',
  'delay',
  'delayOnTouchStart',
  'touchStartThreshold',
  'forceFallback',
  'fallbackClass',
  'dragClass',
  'chosenClass',
  'swapThreshold',
  'invertSwap',
  'invertedSwapThreshold',
  'direction',
  'scroll',
  'scrollSensitivity',
  'scrollSpeed',
  'bubbleScroll',
  'emptyInsertThreshold',
  'disabled',
  'list',
  'modelValue',
  'value',
  'tag'
])

function pickDomAttrs(attrs) {
  const dom = {}
  Object.keys(attrs).forEach((key) => {
    if (SORTABLE_OPTION_ATTRS.has(key)) return
    if (key.startsWith('on')) return
    dom[key] = attrs[key]
  })
  return dom
}

export default defineComponent({
  name: 'Draggable',
  inheritAttrs: false,
  props: {
    list: {
      type: Array,
      default: null
    },
    modelValue: {
      type: Array,
      default: null
    },
    value: {
      type: Array,
      default: null
    },
    tag: {
      type: String,
      default: 'div'
    }
  },
  emits: ['update:modelValue', 'input', 'change'],
  setup(props, { slots, attrs, emit }) {
    const rootRef = ref(null)
    let sortable = null

    onMounted(() => {
      const root = rootRef.value
      if (!root) return
      const draggableSelector = typeof attrs.draggable === 'string' && attrs.draggable
        ? attrs.draggable
        : '> *'
      sortable = Sortable.create(root, {
        draggable: draggableSelector,
        animation: 150,
        onEnd(evt) {
          const { oldIndex, newIndex } = evt
          if (oldIndex == null || newIndex == null || oldIndex === newIndex) return
          const next = resolveList(props).slice()
          const [moved] = next.splice(oldIndex, 1)
          next.splice(newIndex, 0, moved)
          emitListUpdate(emit, next)
        }
      })
    })

    onBeforeUnmount(() => {
      sortable?.destroy()
      sortable = null
    })

    return () => h(props.tag, { ...pickDomAttrs(attrs), ref: rootRef }, slots.default?.())
  }
})
