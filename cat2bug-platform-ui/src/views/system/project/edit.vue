<template>
  <div class="app-container">
    <el-page-header @back="goBack" :content="$t('project.info')">
    </el-page-header>
    <el-row class="project-add-page-container">
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
<!--        基础信息-->
        <el-row :gutter="100" class="step1">
          <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="12">
            <el-form-item :label="$t('project.name')" prop="projectName">
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
          </el-col>
          <!--          项目图标-->
          <el-col :xs="24" :sm="24" :md="8" :lg="6" :xl="6" class="step2">
            <el-image
              style="width: 150px; height: 150px"
              :src="form.projectIcon"
              fit="cover"></el-image>
            <el-popover
              v-model="projectIconPopperVisible"
              popper-class="project-icon-popper"
              placement="bottom"
              trigger="click">
              <el-row class="project-icon-popper">
                <el-col :span="6" v-for="i in 10" :key="i">
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
          <!--            保存取消按钮-->
          <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="12">
            <el-form-item>
              <el-button type="primary" @click="onSubmit">{{$t('update')}}</el-button>
              <el-button @click="goBack">{{$t('cancel')}}</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-row>
  </div>
</template>

<script>
import {addProject, getProject, listProjectRole, updateProject} from "@/api/system/project";
import { listMember } from "@/api/system/team";
import MemberNameplate from "@/components/MemberNameplate"

export default {
  name: "ProjectAdd",
  components:{ MemberNameplate },
  data() {
    return {
      memberParams: {
        params: {
          excludeMembers: [],
          search: null
        }
      },                                // 用户查询条件
      roleOptions: [],                  // 角色选项
      memberList:[],                    // 成员列表
      projectMemberSwitch: false,       // 项目成员开关
      form:{
        teamId: this.getTeamId(),  // 团队id
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
        roleIds: [
          { type: 'array', required: true, message: this.$i18n.t('member.please-select-role'), trigger: 'change'}
        ]
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
  created() {
    this.getProject();
    this.getRoleList();
  },
  methods: {
    getProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    getProject() {
      getProject(this.getProjectId()).then(res=>{
        this.form = res.data;
      });
    },
    getRoleList() {
      listProjectRole(0).then(res => {
        this.roleOptions = res.rows?res.rows.map(r=>{
          r.roleName = r.roleNameI18nKey?this.$t(r.roleNameI18nKey):r.roleName;
          return r;
        }):[];
        this.getMemberList();
        this.dialogVisible = true;
      });
    },
    /** 获取待添加的成员 */
    getMemberList(){
      this.memberParams.params.excludeMembers = this.form.members.map(m=>m.userId);
      listMember(this.getTeamId(),this.memberParams).then(res=>{
        // 赋值成员列表
        this.memberList = res.rows.map(m=> {
          // 设置不选中
          m.isSelect=false;
          // 设置默认角色
          if(this.roleOptions && this.roleOptions.length>0){
            m.roleIds=[this.roleOptions[0].roleId];
          } else {
            m.roleIds=[];
          }
          return m;
        });
      });
    },
    /** 获取团队id */
    getTeamId() {
      return this.$store.state.user.config.currentTeamId;
    },
    /** 删除选中成员 */
    deleteMemberHandle(member){
      member.isSelect = false;
      this.form.members = this.form.members.filter(m=>m.isSelect);
    },
    resetMember() {
      this.memberParams = {
        params: {
          excludeMembers: [],
          search: null
        }
      };
    },
    /** 选中添加的成员 */
    selectAddMemberHandle(member) {
      member.isSelect = !member.isSelect;
      if(member.isSelect){
        this.form.members.push(member);
        let membersSet = new Set(this.form.members);
        this.form.members = Array.from(membersSet);
      } else {
        this.form.members = this.form.members.filter(m=>m.isSelect);
      }
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
          updateProject(this.form).then(response => {
            this.$modal.msgSuccess(this.$i18n.t('update.success'));
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
  .project-members {
    margin-top: 10px;
    .el-col {
      padding: 0 5px;
      display: flex;
      align-items: center;
      flex-direction: row;
      flex-wrap: nowrap;
      justify-content: flex-start;
    }
    .el-col:hover {
      background-color: #F2F6FC;
      border-radius: 5px;
      cursor: pointer;
    }
    ::v-deep .el-col[select="true"] {
      .member-nameplate-content {
        p {
          color: #1890ff;
        }
        span {
          color: #909399;
        }
      }
    }
    .el-col[select="true"]:after {
      position: absolute;
      right: 20px;
      font-family: "element-icons";
      content: "";
      font-size: 12px;
      -webkit-font-smoothing: antialiased;
      color: #1890ff;
    }
  }
  ::v-deep .el-table {
    .el-form-item__error {
      display: none;
    }
    .el-input__inner {
      border-width: 0px;
    }
    .el-input__inner::placeholder {
      color: red;
    }
  }
</style>
