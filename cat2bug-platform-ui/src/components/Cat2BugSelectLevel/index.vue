<template>
  <el-select popper-class="case-level" v-model="value" placeholder="请选择" @change="changeHandle">
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
import { getLevelName, getLevelExplain } from "@/utils/case";
export default {
  name: "index",
  model:{
    prop: 'level',
    event: 'change'
  },
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
    level: {
      type: Number,
      default: 1,
    },
    maxLevel: {
      type: Number,
      default: 5
    }
  },
  methods: {
    getLevelName,
    getLevelExplain,
    changeHandle() {
      this.$emit('change',this.value)
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
</style>
