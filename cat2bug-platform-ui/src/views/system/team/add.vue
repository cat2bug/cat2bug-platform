<template>
  <div class="app-container">
    <el-form ref="form" :model="form" :rules="rules" label-width="120px">
      <el-form-item :label="$t('team.name')" prop="teamName">
        <el-input v-model="form.teamName" :placeholder="$t('team.please-enter-name')" maxlength="64" />
      </el-form-item>
      <el-form-item :label="$t('team.icon')" prop="teamIcon">
        <image-upload v-model="form.teamIcon" :limit="1"/>
      </el-form-item>
      <el-form-item :label="$t('team.introduce')" prop="introduce">
        <el-input v-model="form.introduce" :placeholder="$t('team.please-enter-introduce')" type="textarea" rows="5" maxlength="255" show-word-limit />
      </el-form-item>
    </el-form>
    <el-row>
      <el-col :span="24">
        <el-button type="primary" @click="submitForm">{{ $t('team.create') }}</el-button>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import {addTeam, listTeam, updateTeam} from "@/api/system/team";
import {updateConfig} from "@/api/system/user-config";
import {removeCurrentProjectId} from "@/utils/project";
import store from "@/store";

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
        teamName: [
          { required: true, message: this.$t('team.name-cannot-empty'), trigger: "blur" }
        ],
        teamIcon: [
          { required: true, message: this.$t('team.icon-cannot-empty'), trigger: "blur" }
        ],
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
      let _this = this;
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.teamId != null) {
            updateTeam(this.form).then(response => {
              _this.$modal.msgSuccess(this.$i18n.t('modify-success'));
              _this.open = false;
              _this.getList();
            });
          } else {
            addTeam(this.form).then(response => {
              _this.$modal.msgSuccess(this.$i18n.t('create-success'));
              updateConfig({
                currentTeamId: response.data.teamId
              }).then(res => {
                store.dispatch('GetInfo').then(() => {
                  _this.$router.push({path:'/team/project'});
                });
              });
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
