<template>
  <div class="app-container">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('team.info')">
      </el-page-header>
    </el-row>
    <el-row>
      <el-col :span="16">
        <el-skeleton :loading="loading" animated>
          <template slot="template">
            <div style="padding: 14px;">
              <el-skeleton-item variant="h3" />
            </div>
            <el-skeleton-item
              variant="image"
              style="width: 150px; height: 150px;"
            />
            <div style="padding: 14px;">
              <el-skeleton-item variant="h3" />
              <el-skeleton-item variant="h3" />
            </div>
          </template>
          <template>
            <el-form ref="form" :model="form" :rules="rules" label-width="120px">
              <el-form-item :label="$t('member.name')" prop="teamName">
                <el-input v-model="form.teamName" placeholder="请输入团队名称" maxlength="64" />
              </el-form-item>
              <el-form-item :label="$t('member.icon')" prop="teamIcon">
                <image-upload v-model="form.teamIcon" :limit="1"/>
              </el-form-item>
              <el-form-item :label="$t('member.introduce')" prop="introduce">
                <el-input v-model="form.introduce" :placeholder="$t('member.please-enter-introduce')" type="textarea" maxlength="255" rows="5" show-word-limit />
              </el-form-item>
            </el-form>
            <el-row>
              <el-col :span="24">
                <el-button type="primary" @click="submitForm">{{$t('save')}}</el-button>
              </el-col>
            </el-row>
          </template>
        </el-skeleton>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import {addTeam, getTeam, listTeam, updateTeam} from "@/api/system/team";

export default {
  name: "TeamBaseInfo",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        teamName: [
          { required: true, message: this.$t('member.name-cannot-empty'), trigger: "blur" }
        ],
        teamIcon: [
          { required: true, message: this.$t('member.icon-cannot-empty'), trigger: "blur" }
        ],
      }
    };
  },
  mounted() {
    this.getTeamInfo();
  },
  methods: {
    /** 获取团队id */
    getTeamId() {
      return this.$store.state.user.config.currentTeamId;
    },
    /** 获取团队 */
    getTeamInfo(){
      this.loading = true;
      getTeam(this.getTeamId()).then(res=>{
        this.loading = false;
        this.form = res.data;
      });
    },
    /** 返回 */
    goBack() {
      this.$router.back();
    },
    /** 表单重置 */
    reset() {
      this.form = {
        teamId: this.getTeamId(),
        teamName: null,
        teamIcon: null,
        introduce: null
      };
      this.resetForm("form");
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          updateTeam(this.form).then(response => {
            this.$modal.msgSuccess(this.$t('save.success'));
            this.$router.go(0);
          });
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
