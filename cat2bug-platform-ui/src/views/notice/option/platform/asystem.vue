<template>
  <el-form ref="form" :model="form" label-width="120px">
    <el-form-item label="系统内部通知">
      <el-switch v-model="form.switch" @change="handleChange"></el-switch>
    </el-form-item>
    <el-form-item label="选项">
      <el-checkbox label="开启通知音效" v-model="form.backgroundMusic" @change="handleChange"></el-checkbox>
      <el-checkbox label="在右上角实时显示通知窗口" v-model="form.panel" @change="handleChange"></el-checkbox>
    </el-form-item>
  </el-form>
</template>

<script>
export default {
  name: "SystemNoticePlatform",
  model: {
    prop: 'system',
    event: 'change'
  },
  data() {
    return {
      form: this.system,
    }
  },
  props: {
    system: {
      type: Object,
      default: ()=>{
        return {}
      }
    }
  },
  mounted() {
  },
  methods: {
    /** 操作改变 */
    handleChange() {
      this.$emit('change', this.form);
    },
  }
}
</script>

<style scoped>

</style>
