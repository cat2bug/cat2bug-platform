/**
 * 项目设置列表类子页：查询区 ←/→ 导航 + 工具栏桥接 + 分页快捷键。
 */
import projectOptionSubKbd from '@/mixins/project-option-sub-kbd'
import listQueryKeyboardNav from '@/mixins/list-query-keyboard-nav'

export default {
  mixins: [projectOptionSubKbd, listQueryKeyboardNav],
  mounted() {
    this.$nextTick(() => {
      this.$_bindListQueryNavFocusIn()
      this.$_bindListQueryNavToolbarFocusIn()
    })
  },
  activated() {
    this.$nextTick(() => {
      this.$_bindListQueryNavFocusIn()
      this.$_bindListQueryNavToolbarFocusIn()
    })
  },
  methods: {
    shortcutFocusQuery() {
      if (typeof this.enterListQueryKeyboardNav === 'function') {
        this.enterListQueryKeyboardNav()
      }
    }
  }
}
