<template>
  <div class="app-container">
    <el-row>
      <el-col :span="24">
        <el-page-header @back="goBack" :content="$t('project.create-project')">
        </el-page-header>
      </el-col>
    </el-row>
    <el-form ref="form" :model="form" label-width="120px">
      <el-row :gutter="50">
        <el-col :span="8">
          <el-form-item :label="$t('project.name')">
            <el-input v-model="form.projectName" maxlength="64"></el-input>
          </el-form-item>
          <el-form-item :label="$t('project.introduction')">
            <el-input
              type="textarea"
              placeholder="请输入内容"
              v-model="form.projectIntroduce"
              maxlength="255"
              show-word-limit
            >
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onSubmit">{{$t('finish')}}</el-button>
            <el-button>{{$t('cancel')}}</el-button>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-image
            style="width: 100px; height: 100px"
            :src="form.projectIcon"
            fit="cover"></el-image>
          <el-popover
            placement="bottom"
            trigger="click">
            <el-image
              style="width: 50px; height: 50px"
              :src="form.projectIcon"
              fit="cover"></el-image>
            <el-button slot="reference" size="mini">更改封面</el-button>
          </el-popover>
        </el-col>
      </el-row>
    </el-form>
  </div>
</template>

<script>
export default {
  name: "ProjectAdd",
  data() {
    return {
      form:{}
    }
  },
  methods: {
    goBack() {
      this.$router.back();
    }
  }
}
</script>

<style scoped>

</style>
