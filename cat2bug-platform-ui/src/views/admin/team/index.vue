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
        <template slot-scope="scope">
          <image-preview :src="scope.row.teamIcon" :width="50" :height="50"/>
        </template>
      </el-table-column>
      <el-table-column :label="$t('team.name')" align="left" prop="teamName" />
      <el-table-column :label="$t('createBy')" align="left" prop="createBy" width="100"  />
      <el-table-column :label="$t('createTime')" align="center" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('project.count')" align="center" prop="projectCount" width="130" />
      <el-table-column :label="$t('member.count')" align="center" prop="memberCount" width="130" />
      <el-table-column :label="$t('team.introduce')" align="left" prop="introduce" />
      <el-table-column :label="$t('operate')" align="center" class-name="small-padding" width="120">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            :icon="scope.row.lock?'el-icon-unlock':'el-icon-lock'"
            @click="handleLock(scope.row)"
            v-hasPermi="['system:team:lock']"
          >{{scope.row.lock?$t('unlock'):$t('lock')}}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
    <team-lock-dialog ref="teamLockDialog" @change="handleQuery"></team-lock-dialog>
  </div>
</template>

<script>
import TeamLockDialog from './lock'
import { listTeam } from "@/api/admin/team";

export default {
  name: "AdminTeam",
  components: { TeamLockDialog },
  data() {
    return {
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
  }
};
</script>
