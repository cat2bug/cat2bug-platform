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
              v-model="queryParams.userName"
              prefix-icon="el-icon-search"
              :placeholder="$t('team.search-placeholder')"
              clearable
              style="width: 320px"
              size="mini"
              @keyup.enter.native="handleQuery"
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
          <el-table-column :label="$t('member.name')" align="left" key="nickName" prop="nickName" :show-overflow-tooltip="true" />
          <el-table-column :label="$t('email')" align="left" key="email" prop="email" :show-overflow-tooltip="true" width="300"/>
          <el-table-column :label="$t('phone-number')" align="left" key="phoneNumber" prop="phoneNumber" width="120" />
          <el-table-column
            :label="$t('operate')"
            align="center"
            width="160"
            class-name="small-padding fixed-width"
          >
            <template slot-scope="scope" v-if="scope.row.userId !== 1">
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
  </div>
</template>

<script>
import {getMemberByTeam} from "@/api/system/team";

export default {
  name: "TeamMemberManage",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 总条数
      total: 0,
      // 显示搜索条件
      showSearch: true,
      // 成员列表
      memberList:[],
      // 查询参数
      queryParams: {
        teamId: this.$store.state.user.config.currentTeamId,  // 团队id
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        phonenumber: undefined,
        status: undefined,
        deptId: undefined
      },
    }
  },
  created() {
    this.getMemberList();
  },
  methods: {
    getMemberList() {
      this.loading = true;
      getMemberByTeam(this.queryParams.teamId).then(res => {
        this.loading = false;
        this.memberList = res.rows;
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
      this.$router.push({name:'TeamAdd'})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/user/export', {
        ...this.queryParams
      }, `user_${new Date().getTime()}.xlsx`)
    },
  }
}
</script>

<style lang="scss" scoped>
  .member-tools>* {
    margin-left: 10px;
  }
</style>
