import {
  destroySplitDropdownKbd,
  initSplitDropdownKbd
} from '@/utils/split-dropdown-kbd'

/** 页面根节点内 el-dropdown 键盘绑定（全局 DropdownKbdPlugin 已覆盖，此处用于 keep-alive 激活时刷新） */
export default {
  mounted() {
    this.$nextTick(() => this.$_refreshSplitDropdownKbd())
  },
  updated() {
    this.$nextTick(() => initSplitDropdownKbd(this.getSplitDropdownKbdRoot()))
  },
  activated() {
    this.$nextTick(() => this.$_refreshSplitDropdownKbd())
  },
  beforeDestroy() {
    this.$_destroySplitDropdownKbd()
  },
  deactivated() {
    this.$_destroySplitDropdownKbd()
  },
  methods: {
    getSplitDropdownKbdRoot() {
      return this.$el
    },
    $_refreshSplitDropdownKbd() {
      this.$_destroySplitDropdownKbd()
      initSplitDropdownKbd(this.getSplitDropdownKbdRoot())
    },
    $_destroySplitDropdownKbd() {
      destroySplitDropdownKbd(this.getSplitDropdownKbdRoot())
    }
  }
}
