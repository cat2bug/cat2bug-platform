<template>
  <div class="app-container">
    <el-row>
      <el-page-header @back="goBack" :content="$t('team.manage')">
      </el-page-header>
    </el-row>
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="" prop="teamName">
        <el-input
          v-model="queryParams.teamName"
          :placeholder="$t('team.please-enter-name')"
          clearable
          @input="handleQuery"
        />
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="teamList">
      <el-table-column :label="$t('team.icon')" align="center" prop="teamIcon" width="100">
        <template #default="scope">
          <image-preview :src="scope.row.teamIcon" :width="50" :height="50"/>
        </template>
      </el-table-column>
      <el-table-column :label="$t('team.name')" align="left" prop="teamName" />
      <el-table-column :label="$t('createBy')" align="left" prop="createBy" width="100"  />
      <el-table-column :label="$t('createTime')" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('project.count')" align="center" prop="projectCount" width="130">
        <template #default="scope">
          <el-link type="primary" @click="handleProjectListClick(scope.row)">{{ scope.row.projectCount }}</el-link>
        </template>
      </el-table-column>
      <el-table-column :label="$t('member.count')" align="center" prop="memberCount" width="130" />
      <el-table-column :label="$t('admin.team-plan')" align="left" prop="planName" width="140">
        <template #default="scope">
          {{ scope.row.planName || '—' }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('team.introduce')" align="left" prop="introduce" />
      <el-table-column :label="$t('operate')" align="center" class-name="small-padding" width="200">
        <template #default="scope">
          <el-button
            size="small"
            link
            :icon="$legacyIcon('el-icon-s-goods')"
            @click="handleBindPlan(scope.row)"
            v-hasPermi="['admin:team:plan']"
          >{{ $t('admin.team.bind-plan') }}</el-button>
          <el-button
            size="small"
            link
            :icon="$legacyIcon(scope.row.lock?'el-icon-unlock':'el-icon-lock')"
            @click="handleLock(scope.row)"
            v-hasPermi="['admin:team:lock']"
          >{{scope.row.lock?$t('unlock'):$t('lock')}}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
    <team-lock-dialog ref="teamLockDialog" @change="handleQuery"></team-lock-dialog>
    <el-dialog :title="$t('admin.team.bind-plan')" v-model="planDialogVisible" width="420px" append-to-body>
      <el-select v-model="bindPlanId" style="width: 100%" :placeholder="$t('admin.team-plan')">
        <el-option
          v-for="p in planOptions"
          :key="p.planId"
          :label="p.planName"
          :value="p.planId"
        />
      </el-select>
      <template #footer>
        <el-button @click="planDialogVisible = false">{{ $t('cancel') }}</el-button>
        <el-button type="primary" @click="submitBindPlan">{{ $t('confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import TeamLockDialog from './lock'
import { listTeam } from "@/api/admin/team";
import { listTeamPlan, bindTeamPlan } from "@/api/admin/team-plan";

export default {
  name: "AdminTeam",
  components: { TeamLockDialog },
  data() {
    return {
      planDialogVisible: false,
      planOptions: [],
      bindPlanId: null,
      bindTeamId: null,
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 团队表格数据
      teamList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        teamName: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 返回 */
    goBack() {
      this.$router.back();
    },
    handleBindPlan(row) {
      this.bindTeamId = row.teamId
      this.bindPlanId = row.planId || null
      listTeamPlan().then(res => {
        this.planOptions = res.data || []
        this.planDialogVisible = true
      })
    },
    submitBindPlan() {
      if (!this.bindPlanId) {
        this.$modal.msgError(this.$t('admin.team-plan.name-required'))
        return
      }
      bindTeamPlan(this.bindTeamId, this.bindPlanId).then(() => {
        this.$modal.msgSuccess(this.$t('success'))
        this.planDialogVisible = false
        this.getList()
      })
    },
    /** 查询团队列表 */
    getList() {
      this.loading = true;
      listTeam(this.queryParams).then(response => {
        this.teamList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
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
    handleLock(team) {
      this.$refs.teamLockDialog.open(team);
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    handleProjectListClick(team) {
      this.$router.push({path:'/admin/project', query: {
          teamName: team.teamName
        }})
    }
  }
};
</script>
