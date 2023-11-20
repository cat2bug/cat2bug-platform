<template>
  <div class="app-container">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('member.manage')">
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
              :placeholder="$t('team.search-placeholder')"
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
              icon="el-icon-plus"
              size="mini"
              @click="inviteMemberHandle"
              v-hasPermi="['system:user:add']"
            >{{$t('team.invite-members')}}</el-button>
            <el-button
              type="primary"
              style="float: right;"
              plain
              icon="el-icon-plus"
              size="mini"
              @click="createMemberHandle"
              v-hasPermi="['system:user:add']"
            >{{$t('member.create')}}</el-button>
          </el-col>
        </el-row>

        <el-table v-loading="loading" :data="memberList">
          <el-table-column :label="$t('member.name')" align="left" key="nickName" prop="nickName" :show-overflow-tooltip="true">
            <template slot-scope="scope">
              <div class="member-name">
                <el-avatar v-if="scope.row.avatar" size="small" :src="scope.row.avatar"></el-avatar>
                <el-avatar v-else size="small">{{ scope.row.userName }}</el-avatar>
                <span :teamLock="scope.row.status">{{ scope.row.nickName }}</span>
              </div>
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
                         :disabled="scope.row.status==1"
                         @change="roleChangeHandle(scope.row)"
              >
                <el-option
                  v-for="item in roleOptions"
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
                icon="el-icon-lock"
                @click="lockMemberHandle(scope.row)"
                v-hasPermi="['system:user:edit']"
                v-if="scope.row.status=='0'"
              >锁定</el-button>
              <el-button
                size="mini"
                type="text"
                icon="el-icon-unlock"
                @click="unlockMemberHandle(scope.row)"
                v-hasPermi="['system:user:edit']"
                v-else-if="scope.row.status=='1'"
              >解锁</el-button>
<!--              <el-popover-->
<!--                placement="bottom"-->
<!--                width="200"-->
<!--                trigger="click">-->
<!--                <el-row>-->
<!--                  <el-col :span="24" v-for="item in roleOptions">-->
<!--                    <el-link :underline="false">{{item.roleName}}<i class="el-icon-check"></i></el-link>-->
<!--                  </el-col>-->
<!--                </el-row>-->
<!--              </el-popover>-->
<!--              <el-button-->
<!--                size="mini"-->
<!--                type="text"-->
<!--                icon="el-icon-edit"-->
<!--                @click="handleUpdate(scope.row)"-->
<!--                v-hasPermi="['system:user:edit']"-->
<!--              >修改</el-button>-->
<!--              <el-button-->
<!--                size="mini"-->
<!--                type="text"-->
<!--                icon="el-icon-delete"-->
<!--                @click="handleDelete(scope.row)"-->
<!--                v-hasPermi="['system:user:remove']"-->
<!--              >删除</el-button>-->
<!--              <el-dropdown size="mini" @command="(command) => handleCommand(command, scope.row)" v-hasPermi="['system:user:resetPwd', 'system:user:edit']">-->
<!--                <el-button size="mini" type="text" icon="el-icon-d-arrow-right">更多</el-button>-->
<!--                <el-dropdown-menu slot="dropdown">-->
<!--                  <el-dropdown-item command="handleResetPwd" icon="el-icon-key"-->
<!--                                    v-hasPermi="['system:user:resetPwd']">重置密码</el-dropdown-item>-->
<!--                  <el-dropdown-item command="handleAuthRole" icon="el-icon-circle-check"-->
<!--                                    v-hasPermi="['system:user:edit']">分配角色</el-dropdown-item>-->
<!--                </el-dropdown-menu>-->
<!--              </el-dropdown>-->
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
    <create-team-member ref="createTeamMemberDialog" @create="getMemberList" />
  </div>
</template>

<script>
import {getMemberByTeam, listMember, updateMemberTeamRole, updateMemberTeamRoleIds} from "@/api/system/team";
import CreateTeamMember from "@/views/system/team/option/team/CreateTeamMember";
import {getUser} from "@/api/system/user";

export default {
  name: "TeamMemberManage",
  components: { CreateTeamMember },
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
    this.init();
    this.getMemberList();
  },
  methods: {
    /** 初始化数据 */
    init() {
      // 获取当前用户信息
      getUser().then(res => {
        this.roleOptions = res.roles?res.roles.filter(r=>r.isTeamRole).map(r=>{
          r.roleName = r.roleNameI18nKey?this.$t(r.roleNameI18nKey):r.roleName;
          return r;
        }):[];
      });
    },
    /** 获取团队id */
    getTeamId() {
      return this.$store.state.user.config.currentTeamId;
    },
    /** 搜索用户 */
    memberSearchHandle(e){
      this.queryParams.params.search=e;
      this.getMemberList();
    },
    /** 获取团队成员列表 */
    getMemberList() {
      this.loading = true;
      listMember(this.getTeamId(),this.queryParams).then(res => {
        this.loading = false;
        this.memberList = res.rows;
      });
    },
    /** 更新用户权限 */
    roleChangeHandle(member) {
      updateMemberTeamRoleIds(
        this.getTeamId(),
        member.userId,
        member.roleIds
      ).then(res=>{
        this.$message.success(this.$i18n.t('update.success'));
        this.getMemberList();
      });
    },
    /** 返回 */
    goBack() {
      this.$router.back();
    },
    /** 邀请按钮操作 */
    inviteMemberHandle() {
      this.$router.push({name:'TeamAdd'})
    },
    /** 新增按钮操作 */
    createMemberHandle() {
      // this.$router.push({path:'/team-option/create-member'})
      this.$refs.createTeamMemberDialog.open();
    },
    /** 更新用户操作 */
    updateMemberRole(memberRole){
      updateMemberTeamRole(memberRole.teamId,memberRole.userId,memberRole).then(res=>{
        this.$message.success(this.$i18n.t('update.success'));
        this.getMemberList();
      });
    },
    /** 锁定按钮操作 */
    lockMemberHandle(member) {
      this.updateMemberRole({
        teamId: this.getTeamId(),
        userId: member.userId,
        teamLock: 1
      });
    },
    /** 解锁按钮操作 */
    unlockMemberHandle(member) {
      this.updateMemberRole({
        teamId: this.getTeamId(),
        userId: member.userId,
        teamLock: 0
      });
    },
  }
}
</script>

<style lang="scss" scoped>
  .member-tools>* {
    margin-left: 10px;
  }
  .member-name {
    display: flex;
    flex-direction: row;
    justify-content: start;
    align-items: center;
    >* {
      margin-right: 10px;
    }
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
