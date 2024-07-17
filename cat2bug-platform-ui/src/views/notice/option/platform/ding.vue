<template>
  <el-form ref="form" :model="form" label-width="120px">
    <el-form-item label="钉钉机器人">
      <el-switch v-model="form.switch" @change="handleChange"></el-switch>
    </el-form-item>
    <el-form-item label="关键词">
      <el-input v-model="form.key" @input="handleChange"></el-input>
    </el-form-item>
    <el-form-item label="Hook地址">
      <el-input v-model="form.hook" @input="handleChange"></el-input>
    </el-form-item>
  </el-form>
</template>

<script>
export default {
  name: "DingDingNoticePlatform",
  model: {
    prop: 'ding',
    event: 'change'
  },
  data() {
    return {
      form: this.ding,
    }
  },
  props: {
    ding: {
      type: Object,
      default: ()=>{
        return {}
      }
    }
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
