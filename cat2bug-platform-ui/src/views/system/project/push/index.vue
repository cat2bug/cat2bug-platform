<template>
  <div class="app-container">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('project.push')">
      </el-page-header>
    </el-row>
    <el-form ref="form" :model="form" label-width="80px">
      <el-form-item :label="$t('project.push.key')">
        <el-input v-model="form.pullKey"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSubmit">{{ $t('sync') }}</el-button>
        <el-button>{{ $t('cancel') }}</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import {pullProject} from "@/api/system/project";

export default {
  name: "index",
  data() {
    return {
      form: {
        pullKey: '',
      }
    }
  },
  computed: {
    teamId() {
      return this.$store.state.user.config.currentTeamId;
    },
    projectId() {
      return this.$store.state.user.config.currentProjectId
    },
  },
  methods: {
    /** 返回 */
    goBack() {
      this.$router.back();
    },
    /** 提交项目同步操作 */
    onSubmit() {
      pullProject(this.projectId, this.form).then(res=>{
        this.$message.success('同步成功');
      });
    },
  }
}
</script>

<style scoped>

</style>
