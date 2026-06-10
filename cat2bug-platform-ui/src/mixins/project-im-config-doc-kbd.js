/**
 * 钉钉 / 飞书 / 企业微信等项目 IM 配置页：右侧配置说明区 ⌘/Ctrl + ↑/↓ 滚动。
 * 模板须在 `.doc` 列上设置 ref="projectImConfigDoc"。
 */
export default {
  methods: {
    getFieldHintArrowScrollContainer() {
      const ref = this.$refs.projectImConfigDoc
      const el = ref && (ref.$el || ref)
      if (!el || typeof el.scrollHeight !== 'number') return null
      if (el.scrollHeight <= el.clientHeight + 2) return null
      return el
    }
  }
}
