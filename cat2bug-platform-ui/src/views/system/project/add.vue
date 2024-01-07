<template>
  <div class="app-container">
    <el-page-header @back="goBack" :content="$t('project.create-project')">
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
              :src="activeProjectIconUrl(activeProjectIconIndex)"
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
          <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="12">
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
                    @show="getMemberList"
                    @hide="resetMember"
                    trigger="click">
                    <el-input prefix-icon="el-icon-search" :placeholder="$t('team.search-placeholder')" v-model="memberParams.params.search" @input="getMemberList" size="mini"></el-input>
<!--                    成员列表-->
                      <el-row v-if="memberList && memberList.length>0" class="project-members">
                        <el-col :select="member.isSelect" :span="24" v-for="member in memberList" :key="member.userId" @click.native="selectAddMemberHandle(member)">
                          <member-nameplate :member="member"></member-nameplate>
                        </el-col>
                      </el-row>
                    <el-empty v-else :description="$t('no-data')"></el-empty>
                    <el-button slot="reference" size="mini" icon="el-icon-user-solid">{{$t('project.add-member')}}</el-button>
                  </el-popover>
                </el-col>
                <el-col :span="24" v-if="projectMemberSwitch">
                  <el-table
                    :data="form.members"
                    style="width: 100%">
                    <el-table-column
                      prop="nickName"
                      :label="$t('name')"
                      min-width="200"
                    >
                      <template slot-scope="scope">
                        <member-nameplate :member="scope.row"></member-nameplate>
                      </template>
                    </el-table-column>
                    <el-table-column
                      prop="roleIds"
                      :label="$t('role')"
                      width="230">
                      <template slot-scope="scope">
                        <el-form-item :prop="`members.${scope.$index}.roleIds`" :rules="rules.roleIds">
                          <el-select v-model="scope.row.roleIds" :placeholder="$t('member.please-select-role')" multiple collapse-tags :disabled="scope.row.userId==getCurrentUserId()">
                            <el-option
                              v-for="role in selectRoleOptions(scope.row)"
                              :key="role.roleId"
                              :label="role.roleName"
                              :value="role.roleId">
                            </el-option>
                          </el-select>
                        </el-form-item>
                      </template>
                    </el-table-column>
                    <el-table-column
                      width="80"
                      :label="$t('operate')">
                      <template slot-scope="scope">
                        <el-button
                          v-if="scope.row.userId!=getCurrentUserId()"
                          size="mini"
                          type="text"
                          icon="el-icon-delete"
                          @click="deleteMemberHandle(scope.row)"
                          v-hasPermi="['system:project:remove']"
                        >{{$t('delete')}}</el-button>
                      </template>
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
        </el-row>
      </el-form>
    </el-row>
  </div>
</template>

<script>
import {addProject, listProjectRole} from "@/api/system/project";
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
    },
    selectRoleOptions: function (){
      return function (member){
        if(this.getCurrentUserId() == member.userId){
          return this.roleOptions.filter(r=>r.projectCreateBy);
        } else {
          console.log(member.roleIds, this.roleOptions.filter(r=>r.projectCreateBy==false));
          return this.roleOptions.filter(r=>r.projectCreateBy==false);
        }
      }
    }
  },
  created() {
    this.form.projectIcon = this.activeProjectIconUrl(1);
    this.getRoleList();
  },
  methods: {
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
            m.roleIds=[this.roleOptions.filter(r=>r.projectCreateBy==false)[0].roleId];
          } else {
            m.roleIds=[];
          }
          if(this.getCurrentUserId() == m.userId && this.form.members.length == 0) {
            m.roleIds = this.roleOptions.filter(r=>r.projectCreateBy).map(r=>r.roleId);
            this.form.members.push(m);
          }
          return m;
        });
      });
    },
    /** 获取当前用户id */
    getCurrentUserId() {
      return this.$store.state.user.id;
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
