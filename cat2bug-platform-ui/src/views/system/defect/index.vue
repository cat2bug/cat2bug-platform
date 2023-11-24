<template>
  <div class="app-container">
    <el-tabs v-model="activeDefectTabName" @tab-click="selectDefectTabHandle">
      <el-tab-pane :label="$t('project.my-participated-in')" :name="$t('project.my-participated-in')"></el-tab-pane>
      <!--      <el-tab-pane :label="$t('project.my-manage')" :name="$t('project.my-manage')"></el-tab-pane>-->
            <el-tab-pane :label="$t('project.all-project')" :name="$t('project.all-project')"></el-tab-pane>
      <!--      <el-tab-pane :label="$t('project.archived-project')" :name="$t('project.archived-project')"></el-tab-pane>-->
    </el-tabs>
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="120px">
      <el-form-item label="缺陷标题" prop="defectName">
        <el-input
          v-model="queryParams.defectName"
          placeholder="请输入缺陷标题"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="版本" prop="moduleVersion">
        <el-input
          v-model="queryParams.moduleVersion"
          placeholder="请输入版本"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="处理人" prop="handleBy">
        <el-input
          v-model="queryParams.handleBy"
          placeholder="请输入处理人"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:defect:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:defect:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:defect:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:defect:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="defectList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="缺陷类型" align="center" prop="defectType" />
      <el-table-column label="缺陷标题" align="center" prop="defectName" />
      <el-table-column label="附件" align="center" prop="annexUrls" />
      <el-table-column label="项目" align="center" prop="projectId" />
      <el-table-column label="数据来源" align="center" prop="dataSources" />
      <el-table-column label="版本" align="center" prop="moduleVersion" />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="缺陷状态" align="center" prop="defectState" />
      <el-table-column label="处理人" align="center" prop="handleBy" />
      <el-table-column label="处理时间" align="center" prop="handleTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.handleTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="缺陷等级" align="center" prop="defectLevel" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:defect:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:defect:remove']"
          >删除</el-button>
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

    <!-- 添加或修改缺陷对话框 -->
    <el-drawer
      size="50%"
      title="新建缺陷"
      :visible.sync="open"
      direction="rtl"
      :before-close="closeDefectDrawer">
      <add-defect :project-id="22" />
    </el-drawer>
  </div>
</template>

<script>
import { listDefect, getDefect, delDefect, addDefect, updateDefect } from "@/api/system/defect";
import AddDefect from "./add.vue"
export default {
  name: "Defect",
  components: { AddDefect },
  data() {
    return {
      // 当前缺陷的tab页名
      activeDefectTabName: this.$i18n.t('project.my-participated-in'),
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
      // 缺陷表格数据
      defectList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 缺陷等级时间范围
      daterangeUpdateTime: [],
      // 缺陷等级时间范围
      daterangeCreateTime: [],
      // 缺陷等级时间范围
      daterangeHandleTime: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        defectType: null,
        defectName: null,
        projectId: null,
        testPlanId: null,
        caseId: null,
        dataSources: null,
        dataSourcesParams: null,
        moduleId: null,
        moduleVersion: null,
        createBy: null,
        updateTime: null,
        createTime: null,
        updateBy: null,
        defectState: null,
        handleBy: null,
        handleTime: null,
        defectLevel: null
      },

    };
  },
  created() {
    this.getList();
  },
  methods: {
    closeDefectDrawer(done) {
      done();
    },
    /** 切换页标签 */
    selectDefectTabHandle() {

    },
    /** 获取团队id */
    getTeamId() {
      return this.$store.state.user.config.currentTeamId;
    },
    /** 查询缺陷列表 */
    getList() {
      this.loading = true;
      this.queryParams.params = {};
      if (null != this.daterangeUpdateTime && '' != this.daterangeUpdateTime) {
        this.queryParams.params["beginUpdateTime"] = this.daterangeUpdateTime[0];
        this.queryParams.params["endUpdateTime"] = this.daterangeUpdateTime[1];
      }
      if (null != this.daterangeCreateTime && '' != this.daterangeCreateTime) {
        this.queryParams.params["beginCreateTime"] = this.daterangeCreateTime[0];
        this.queryParams.params["endCreateTime"] = this.daterangeCreateTime[1];
      }
      if (null != this.daterangeHandleTime && '' != this.daterangeHandleTime) {
        this.queryParams.params["beginHandleTime"] = this.daterangeHandleTime[0];
        this.queryParams.params["endHandleTime"] = this.daterangeHandleTime[1];
      }
      listDefect(this.queryParams).then(response => {
        this.defectList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },

    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.daterangeUpdateTime = [];
      this.daterangeCreateTime = [];
      this.daterangeHandleTime = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.defectId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.open = true;
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const defectId = row.defectId || this.ids
      getDefect(defectId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改缺陷";
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const defectIds = row.defectId || this.ids;
      this.$modal.confirm('是否确认删除缺陷编号为"' + defectIds + '"的数据项？').then(function() {
        return delDefect(defectIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/defect/export', {
        ...this.queryParams
      }, `defect_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
