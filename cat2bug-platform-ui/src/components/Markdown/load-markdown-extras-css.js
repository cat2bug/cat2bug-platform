/**
 * Font Awesome / KaTeX 样式原捆在 markdown-it-vue.css 主包内。
 * 仅在 Markdown 渲染组件挂载时按需拉取，避免拖大首屏 CSS。
 */
let loadPromise = null

export function loadMarkdownExtrasCss() {
  if (!loadPromise) {
    loadPromise = Promise.all([
      import('@/assets/styles/markdown-it-vue-fa.css'),
      import('@/assets/styles/markdown-it-vue-katex.css')
    ]).then(() => undefined)
  }
  return loadPromise
}
