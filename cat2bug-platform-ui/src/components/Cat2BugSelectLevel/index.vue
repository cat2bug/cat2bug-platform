<template>
  <el-select
    popper-class="case-level"
    class="case-level-input"
    v-model="value"
    :placeholder="$t('defect.select-level')"
    size="small"
    @change="changeHandle"
    :disabled="readonly"
    :clearable="clearable">
    <template #prefix>
      <el-icon v-if="iconComponent" class="select-header-icon"><component :is="iconComponent" /></el-icon>
    </template>
    <el-option
      v-for="item in maxLevel"
      :key="item"
      :label="getLevelName(item)"
      :value="item">
      <cat2-bug-level :level="item" />
      <span class="level-explain">{{ getLevelExplain(item) }}</span>
    </el-option>
  </el-select>
</template>

<script>
import Cat2BugLevel from "@/components/Cat2BugLevel";
import {getLevelName, getLevelExplain, MAX_LEVEL_INDEX} from "@/utils/case";
export default {
  emits: ['update:level', 'change'],
  name: "index",
  components: {Cat2BugLevel},
  data() {
    return {
      value: this.level,
    }
  },
  watch: {
    level:function (n) {
      this.value = n;
    }
  },
  props: {
    clearable: {
      type: Boolean,
      default: false
    },
    icon: {
      type: [String, Object],
      default: null
    },
    level: {
      type: [Number, String],
      default: null,
    },
    maxLevel: {
      type: Number,
      default: MAX_LEVEL_INDEX
    },
    readonly: {
      type: Boolean,
      default: false
    },
  },
  computed: {
    iconComponent() {
      if (!this.icon) return null
      if (typeof this.icon === 'string') return this.$legacyIcon(this.icon)
      return this.icon
    }
  },
  methods: {
    getLevelName,
    getLevelExplain,
    changeHandle() {
      this.$emit('update:level', this.value); this.$emit('change', this.value)
    }
  }
}
</script>
<style>
.case-level .el-select-dropdown__item {
  height: 100%;
  line-height: 100%;
  padding: 5px;
  margin: 5px 10px;
  max-width: 200px;
  overflow-wrap: break-word;
}
.case-level > .el-scrollbar > .el-select-dropdown__wrap {
  max-height: 500px;
}
</style>
<style lang="scss" scoped>
.case-level-input {
  min-width: 180px;
}
.select-header-icon {
  margin-left: 2px;
  font-size: 14px;
}
.level-explain {
  font-size: 0.7rem;
  color: #C0C4CC;
  width: 100% !important;
  float: left !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  white-space: normal !important;
  margin-top: 5px;
}
:deep(.el-select.case-level-input.is-disabled .el-select__wrapper) {
  background-color: white;
  cursor: default;
  color: #303133;
}
:deep(.el-select.case-level-input.is-disabled .el-select__suffix) {
  display: none;
}
</style>
