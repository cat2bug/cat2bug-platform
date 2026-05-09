/**
 * 替换 element-ui/src/utils/scrollbar-width。
 * Element 用单一 gutterWidth 同时预留纵向条宽度与横向条高度；原实现只测纵向且 overlay 常为 0，
 * 会导致固定列盖住滚动条。此处双轴测量并取 max，与 el-table-scrollbar.scss 中 14px 厚度对齐。
 */
import Vue from 'vue'

let cached

const MIN_THEME = 14

export default function scrollbarWidth() {
  if (Vue.prototype.$isServer) return 0
  if (cached !== undefined) return cached

  const outer = document.createElement('div')
  outer.className = 'el-scrollbar__wrap cat2bug-scrollbar-measure-host'
  Object.assign(outer.style, {
    visibility: 'hidden',
    position: 'absolute',
    top: '-9999px',
    width: '100px',
    height: '100px',
    overflow: 'scroll',
    boxSizing: 'border-box',
  })
  const inner = document.createElement('div')
  inner.style.width = '200px'
  inner.style.height = '200px'
  outer.appendChild(inner)
  document.body.appendChild(outer)

  const vertical = outer.offsetWidth - outer.clientWidth
  const horizontal = outer.offsetHeight - outer.clientHeight

  outer.parentNode.removeChild(outer)

  let v = Math.max(vertical || 0, horizontal || 0, MIN_THEME)
  v = Math.ceil(v) + 2

  cached = v
  return cached
}
