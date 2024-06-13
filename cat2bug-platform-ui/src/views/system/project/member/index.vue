<template>
  <div class="app-container">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('project.member-manage')">
      </el-page-header>
    </el-row>
    <el-row :gutter="20">
      <!--用户数据-->
      <el-col :span="24">
        <el-row :gutter="10" class="mb8" type="flex" justify="space-between">
          <el-col :span="8">
            <el-input
              v-model="queryParams.params.search"
              prefix-icon="el-icon-search"
              :placeholder="$t('member.search-placeholder')"
              clearable
              style="width: 320px"
              size="mini"
              @input="memberSearchHandle"
            />
          </el-col>
          <el-col :span="8" class="member-tools">
            <el-button
              type="primary"
              style="float: right;"
              plain
              icon="el-icon-user-solid"
              size="mini"
              @click="addMemberHandle"
              v-hasPermi="['system:project:list']"
            >{{$t('project.add-member')}}</el-button>
          </el-col>
        </el-row>

        <el-table v-loading="loading" :data="memberList">
          <el-table-column :label="$t('member.name')" align="left" key="nickName" prop="nickName" :show-overflow-tooltip="true">
            <template slot-scope="scope">
              <member-nameplate :member="scope.row"></member-nameplate>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('role')"
            align="left"
            width="280"
          >
            <template slot-scope="scope" v-if="scope.row.userId !== 1">
              <el-select class="member-operate"
                         v-model="scope.row.roleIds"
                         multiple
                         :placeholder="$t('member.please-select-role')"
                         :disabled="isMemberDisabled(scope.row)"
                         @change="roleChangeHandle(scope.row)"
              >
                <el-option
                  v-for="item in selectRoleOptions(scope.row)"
                  :key="item.roleId"
                  :label="item.roleName"
                  :value="item.roleId"
                  :disabled="item.status == 1"
                ></el-option>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column :label="$t('email')" align="left" key="email" prop="email" :show-overflow-tooltip="true" width="300">
            <template slot-scope="scope">
              <span :teamLock="scope.row.status">{{ scope.row.email }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('phone-number')" align="left" key="phoneNumber" prop="phoneNumber" width="150">
            <template slot-scope="scope">
              <span :teamLock="scope.row.status">{{ scope.row.phoneNumber }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('register-time')" align="left" key="createTime" prop="createTime" width="180">
            <template slot-scope="scope">
              <div teamLock="scope.row.status">
                <span :teamLock="scope.row.status">{{ scope.row.createTime }}</span>
              </div>

            </template>
          </el-table-column>
          <el-table-column
            :label="$t('operate')"
            align="center"
            width="160"
            class-name="small-padding fixed-width"
          >
            <template slot-scope="scope" v-if="scope.row.userId !== 1">
              <el-button
                size="mini"
                type="text"
                icon="el-icon-delete"
                @click="deleteHandle(scope.row)"
                v-show="visibleDelete(scope.row)"
              >{{$t('delete')}}</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination
          v-show="total>0"
          :total="total"
          :page.sync="queryParams.pageNum"
          :limit.sync="queryParams.pageSize"
          @pagination="getMemberList"
        />
      </el-col>
    </el-row>
    <add-project-member ref="addProjectMemberDialog" @invite="getMemberList" />
  </div>
</template>

<script>
import {updateMemberTeamRole} from "@/api/system/team";
import AddProjectMember from "@/views/system/project/member/AddProjectMember";
import MemberNameplate from "@/components/MemberNameplate";
import {
  delMemberOfProject,
  listMemberOfProject,
  listProjectRole, updateMemberRoleOfProject,
} from "@/api/system/project";
import {strFormat} from "@/utils";
import i18n from "@/utils/i18n/i18n";
import {checkPermi} from "@/utils/permission";

export default {
  name: "ProjectMemberManage",
  components: { AddProjectMember, MemberNameplate },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 总条数
      total: 0,
      // 显示搜索条件
      showSearch: true,
      memberSearch: null,
      // 成员列表
      memberList:[],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        params: {},
      },
      // 角色选项
      roleOptions: [],
    }
  },
  created() {
    this.getRoleList();
  },
  computed: {
    selectRoleOptions: function () {
      return function (member) {
        if(member.roles && member.roles.filter(r=>r.projectCreateBy).length>0){
          return this.roleOptions.filter(r=>r.projectCreateBy);
        } else {
          return this.roleOptions.filter(r=>r.projectCreateBy==false);
        }
      }
    },
    /** 获取当前用户id */
    currentUserId: function () {
      return this.$store.state.user.id;
    },
    /** 获取项目id */
    currentProjectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 成员是否禁用 */
    isMemberDisabled: function () {
      return function (member) {
        return member.status==1 || (member.roles && member.roles.filter(r=>r.projectCreateBy).length>0);
      }
    },
    /** 是否显示删除按钮 */
    visibleDelete() {
      return function (member) {
        return checkPermi(['system:project:member:remove']) && member.roles.filter(r=>r.projectCreateBy).length==0;
      }
    },
  },
  methods: {
    /** 获取角色 */
    getRoleList() {
      listProjectRole(0).then(res => {
        this.roleOptions = res.rows?res.rows.map(r=>{
          r.roleName = r.roleNameI18nKey?this.$t(r.roleNameI18nKey):r.roleName;
          return r;
        }):[];
        this.getMemberList();
      });
    },

    /** 搜索用户 */
    memberSearchHandle(e){
      this.queryParams.params.search=e;
      this.getMemberList();
    },
    /** 获取团队成员列表 */
    getMemberList() {
      this.loading = true;
      listMemberOfProject(this.currentProjectId,this.queryParams).then(res => {
        this.loading = false;
        this.memberList = res.rows;
        this.total = res.total;
      });
    },
    /** 更新用户权限 */
    roleChangeHandle(member) {
      updateMemberRoleOfProject(
        this.currentProjectId,
        member.userId,
        member.roleIds
      ).then(()=>{
        this.$message.success(this.$i18n.t('update.success').toString());
        this.getMemberList();
      }).catch(()=>{
        this.getMemberList();
      });
    },
    /** 返回 */
    goBack() {
      this.$router.back();
    },
    /** 添加成员按钮操作 */
    addMemberHandle() {
      this.$refs.addProjectMemberDialog.open();
    },
    /** 移除成员按钮操作 */
    deleteHandle(member) {
      let _this = this;
      this.$modal.confirm(this.$i18n.t('project.is-delete-member'),
        this.$i18n.t('prompted').toString(),
        {
          confirmButtonText: i18n.t('delete').toString(),
          cancelButtonText: i18n.t('cancel').toString(),
          confirmButtonClass: 'delete-button',
          type: "warning",
        }).then(() => {
          delMemberOfProject(_this.currentProjectId,member.userId).then(()=>{
            _this.$message.success(this.$i18n.t('delete.success').toString());
            _this.getMemberList();
          })
      }).catch(() => {
      });
    },
  }
}
</script>

<style lang="scss" scoped>
  .member-tools>* {
    margin-left: 10px;
  }
  ::v-deep .member-operate .el-input__inner, ::v-deep .member-operate .el-select__tags {
    min-width: 250px;
    display: flex;
    justify-content: center;
    background-color: #00000000;
    border-width: 0;
  }
  ::v-deep .member-operate .el-select__tags {
    flex-wrap: inherit !important;
    overflow-x: auto !important;
    > span {
      display: flex;
      margin-right: auto;
    }
  }
  ::v-deep .el-table {
    span[teamLock="1"] {
      text-decoration: line-through;
      color: #DCDFE6;
    }
  }
</style>
<style>
.delete-button {
  color: #fff;
  background-color: #f56c6c !important;
  border-color: #f56c6c !important;
}
.delete-button:hover {
  background: #f78989 !important;
  border-color: #f78989 !important;
  color: #fff;
}
</style>
