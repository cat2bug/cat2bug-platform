<template>
  <el-select v-model="value" placeholder="请选择" @change="changeHandle">
    <el-option
      v-for="item in maxLevel"
      :key="item"
      :label="item.toString()"
      :value="item">
      <cat2-bug-level :level="item" />
    </el-option>
  </el-select>
</template>

<script>
import Cat2BugLevel from "@/components/Cat2BugLevel";
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
    changeHandle() {
      this.$emit('change',this.value)
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
