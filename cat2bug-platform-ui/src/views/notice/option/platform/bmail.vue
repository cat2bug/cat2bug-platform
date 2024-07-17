<template>
  <el-form ref="form" :model="form" label-width="120px">
    <el-form-item label="电子邮件">
      <el-switch v-model="form.switch" @change="handleChange"></el-switch>
    </el-form-item>
    <el-form-item label="发送人邮箱">
      <el-input v-model="form.sender" @input="handleChange"></el-input>
    </el-form-item>
  </el-form>
</template>

<script>
export default {
  name: "EMailNoticePlatform",
  model: {
    prop: 'mail',
    event: 'change'
  },
  data() {
    return {
      form: this.mail,
    }
  },
  props: {
    mail: {
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
