<template>
  <div class="app-container">
    <el-page-header @back="goBack" :content="$t('project.create-project')">
    </el-page-header>
<!--    <el-row class="project-add-page-header">-->
<!--      <el-page-header @back="goBack" :content="$t('project.create-project')">-->
<!--      </el-page-header>-->
<!--    </el-row>-->
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
                rows="5"
                v-model="form.projectIntroduce"
                maxlength="255"
                show-word-limit
              >
              </el-input>
            </el-form-item>
            <el-form-item :label="$t('project.member')">
              <el-row>
                <el-col :span="24">
                  <el-switch
                    v-model="projectMemberSwitch"
                    active-color="#13ce66"
                    inactive-color="#ff4949">
                  </el-switch>
                </el-col>
                <el-col :span="24" v-if="projectMemberSwitch">
<!--                  添加成员按钮-->
                  <el-popover
                    placement="bottom"
                    width="400"
                    trigger="click">
                    <el-input placeholder="请输入内容" v-model="userParams.params.search" class="input-with-select" size="mini">
                      <el-button slot="append" icon="el-icon-search"></el-button>
                    </el-input>
<!--                    成员列表-->
                    <div v-for="member in memberList" :key="member.userId">
                      <span>{{member.name}}</span>
                      <el-avatar src="member.icon" fit="cover" size="medium"></el-avatar>
                      <el-avatar size="medium">member.name</el-avatar>
                    </div>
                    <el-empty description="暂无数据"></el-empty>
                    <el-button slot="reference" size="mini" icon="el-icon-user-solid">{{$t('project.add-user')}}</el-button>
                  </el-popover>
                </el-col>
                <el-col :span="24" v-if="projectMemberSwitch">
                  <el-table
                    :data="form.members"
                    style="width: 100%">
                    <el-table-column
                      prop="date"
                      :label="$t('name')">
                    </el-table-column>
                    <el-table-column
                      prop="name"
                      :label="$t('project.set-role-group')"
                      width="180">
                    </el-table-column>
                    <el-table-column
                      prop="address"
                      width="180"
                      :label="$t('operate')">
                    </el-table-column>
                  </el-table>
                </el-col>
              </el-row>
            </el-form-item>
<!--            保存取消按钮-->
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
<!--              选择项目图标按钮-->
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
import {getMemberByTeam} from "@/api/system/team";

export default {
  name: "ProjectAdd",
  data() {
    return {
      userParams: {
        params: {}
      },                                // 用户查询条件
      memberList:[],                    // 成员列表
      projectMemberSwitch: false,       // 项目成员开关
      form:{
        teamId: this.$store.state.user.config.currentTeamId,  // 团队id
        projectName: null,              // 项目名称
        projectIntroduce: null,         // 项目介绍
        projectIcon: null,              // 项目图标
        members: []                     // 项目成员
      },                                // 提交的表单
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
  mounted() {
    this.getMemberList();
  },
  methods: {
    getMemberList(){
      getMemberByTeam(this.form.teamId).then(res=>{
        this.memberList = res.rows;
      });
    },
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
  .project-add-page-container {
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
  ::v-deep .el-table__cell {
    padding: 0;
  }
</style>
