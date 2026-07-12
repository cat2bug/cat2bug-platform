/**
 * Vendored vue-excel-editor@1.5.23 (MIT) with Cat2Bug patches — Vue 3 plugin entry.
 * @see patches/README.md (historical) and src/vendor/vue-excel-editor/
 */
import VueExcelEditor from './VueExcelEditor.vue'
import VueExcelColumn from './VueExcelColumn.vue'

export { VueExcelEditor, VueExcelColumn }

export default {
  install(app) {
    app.component('vue-excel-editor', VueExcelEditor)
    app.component('vue-excel-column', VueExcelColumn)
  }
}
