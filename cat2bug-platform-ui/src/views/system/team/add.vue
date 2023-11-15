<template>
  <div class="app-container">
    <el-form ref="form" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="团队名称" prop="teamName">
        <el-input v-model="form.teamName" placeholder="请输入团队名称" maxlength="64" />
      </el-form-item>
      <el-form-item label="团队图标" prop="teamIcon">
        <image-upload v-model="form.teamIcon"/>
      </el-form-item>
      <el-form-item label="团队介绍" prop="introduce">
        <el-input v-model="form.introduce" placeholder="请输入团队介绍" type="textarea" maxlength="255" show-word-limit />
      </el-form-item>
    </el-form>
    <el-row>
      <el-col span="24">
        <el-button type="primary" @click="submitForm">创建团队</el-button>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import {addTeam, listTeam, updateTeam} from "@/api/system/team";

export default {
  name: "TeamAdd",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 表单参数
      form: {},
      // 表单校验
      rules: {
      }
    };
  },
  methods: {
    // 表单重置
    reset() {
      this.form = {
        teamId: null,
        teamName: null,
        teamIcon: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        introduce: null,
        isDel: null
      };
      this.resetForm("form");
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.teamId != null) {
            updateTeam(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addTeam(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
  }
}
</script>

<style lang="scss" scoped>
  .el-button {
    width: 100%;
  }
</style>
