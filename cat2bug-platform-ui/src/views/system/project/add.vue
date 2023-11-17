<template>
  <div class="app-container">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('project.create-project')">
      </el-page-header>
    </el-row>
    <el-row class="project-add-page-container">
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
<!--        基础信息-->
        <el-row :gutter="100" class="step1">
          <el-col :span="10">
            <el-form-item :label="$t('project.name')">
              <el-input v-model="form.projectName" maxlength="64" :placeholder="$t('project.enter-project-name')"></el-input>
            </el-form-item>
            <el-form-item :label="$t('project.introduction')">
              <el-input
                type="textarea"
                :placeholder="$t('enter-content')"
                v-model="form.projectIntroduce"
                maxlength="255"
                show-word-limit
              >
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onSubmit">{{$t('finish')}}</el-button>
              <el-button @click="goBack">{{$t('cancel')}}</el-button>
            </el-form-item>
          </el-col>
<!--          项目图标-->
          <el-col :span="8" class="step2">
            <el-image
              style="width: 150px; height: 150px"
              :src="activeProjectIconUrl(activeProjectIconIndex)"
              fit="cover"></el-image>
            <el-popover
              v-model="projectIconPopperVisible"
              popper-class="project-icon-popper"
              placement="bottom"
              trigger="click">
              <el-row class="project-icon-popper">
                <el-col :span="6" v-for="i in 10">
                  <el-image
                    @click="clickProjectIconHandle(i)"
                    :src="activeProjectIconUrl(i)"
                    fit="cover"
                  ></el-image>
                </el-col>
              </el-row>
              <el-button slot="reference" size="mini">{{$t('project.change-icon')}}</el-button>
            </el-popover>
          </el-col>
        </el-row>
      </el-form>
    </el-row>
  </div>
</template>

<script>
import {addProject, updateProject} from "@/api/system/project";

export default {
  name: "ProjectAdd",
  data() {
    return {
      form:{
        teamId: this.$store.state.user.config.currentTeamId,
        projectName: null,
        projectIntroduce: null,
        projectIcon: null
      },                          // 提交的表单
      projectIconPopperVisible: false,  // 是否显示项目图标弹窗
      activeProjectIconIndex: 1,        // 当前选中的项目图标索引
      // 表单校验
      rules: {
        projectName: [
          { required: true, message: this.$i18n.t('project.project-name-cannot-empty'), trigger: "blur" }
        ],
      }
    }
  },
  computed: {
    /** 计算当前项目图标地址 */
    activeProjectIconUrl: function () {
      return function (index){
        return require('@/assets/images/project/project_icon'+index+'.svg')
      }
    }
  },
  methods: {
    /** 返回 */
    goBack() {
      this.$router.back();
    },
    /** 点击选中的项目图标处理方法 */
    clickProjectIconHandle(index) {
      this.activeProjectIconIndex = index;                      // 赋值当前所选索引
      this.form.projectIcon = this.activeProjectIconUrl(index); // 赋值项目图标
      this.projectIconPopperVisible = false;                    // 隐藏图标选择组件
    },
    /** 提交按钮 */
    onSubmit() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          addProject(this.form).then(response => {
            this.$modal.msgSuccess(this.$i18n.t('add-success'));
            this.goBack();
          });
        }
      });
    },
  }
}
</script>

<style lang="scss" scoped>
  .project-add-page-header {
    padding-bottom: 20px;
    border-bottom: #EBEEF5 1px solid;
  }
  .project-add-page-container {
    margin-top: 40px;
    .step2 {
      display: flex;
      display: -webkit-flex; /* Safari */
      flex-direction: column;
      column-gap: 10px;
      row-gap: 10px;
      border-left: #EBEEF5 1px solid;
      :first-child {
        margin-bottom: 10px;
      }
      :last-child {
        width: 150px;
      }
    }
  }
  .project-icon-popper {
    width: 500px;
    .el-image:hover {
      cursor: pointer;
    }
  }
</style>
