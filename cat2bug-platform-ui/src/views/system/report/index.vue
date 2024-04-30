<template>
  <div class="app-container">
    <project-label />
    <div class="report-tools">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="" prop="reportTitle">
          <el-input
            v-model="queryParams.reportTitle"
            :placeholder="$t('report.please-enter-title')"
            prefix-icon="el-icon-tickets"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="">
          <el-date-picker
            v-model="daterangeReportTime"
            style="width: 240px"
            value-format="yyyy-MM-dd"
            type="daterange"
            range-separator="-"
            :start-placeholder="$t('start-time')"
            :end-placeholder="$t('end-time')"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="" prop="createById">
          <el-input
            v-model="queryParams.createById"
            :placeholder="$t('report.please-enter-pusher')"
            prefix-icon="el-icon-user"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
      </el-form>
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <cat2-bug-report-template-select
            v-hasPermi="['system:report:add']"
            @create="createReportHandle"
            :project-id="projectId" />

<!--          <el-dropdown size="mini" split-button type="primary" v-hasPermi="['system:report:add']" @command="handleCommand">-->
<!--            生成报告-->
<!--            <el-dropdown-menu slot="dropdown" class="list">-->
<!--              <el-dropdown-item v-for="(t,index) in templateList" :command="t.templateId" :key="index">-->
<!--                -->
<!--              </el-dropdown-item>-->
<!--              <el-dropdown-item divided command="add">-->
<!--                <svg-icon icon-class="add-tab"/>编辑报告模版-->
<!--              </el-dropdown-item>-->
<!--            </el-dropdown-menu>-->
<!--          </el-dropdown>-->
        </el-col>
      </el-row>
    </div>
    <el-table v-loading="loading" :data="reportList" @row-click="rowClickHandle">
      <el-table-column :label="$t('report.type')" align="center" prop="reportTime" width="120">
        <template slot-scope="scope">
          <report-type-flag :report="scope.row" />
        </template>
      </el-table-column>
      <el-table-column :label="$t('report.title')" align="start" prop="reportTitle">
        <template slot-scope="scope">
          <div class="table-report-title">
            <focus-member-list
              v-model="scope.row.focusList"
              module-name="report"
              :data-id="scope.row.reportId" />
            <span>{{ scope.row.reportTitle }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('report.time')" align="center" prop="reportTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.reportTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('report.source')" align="center" prop="reportSource"  width="200"/>
      <el-table-column :label="$t('report.create-by')" align="center" prop="createBy"  width="150"/>
      <el-table-column :label="$t('operate')" align="start" class-name="small-padding fixed-width" width="90">
        <template slot-scope="scope">
          <report-tools :report="scope.row" :is-text="true" :is-show-icon="true" @delete="getList" />
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

    <!-- 添加或修改报告对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="报告标题" prop="reportTitle">
          <el-input v-model="form.reportTitle" placeholder="请输入报告标题" />
        </el-form-item>
        <el-form-item label="报告时间" prop="reportTime">
          <el-date-picker clearable
            v-model="form.reportTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择报告时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="报告描述" prop="reportDescription">
          <el-input v-model="form.reportDescription" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label=" 推送人ID" prop="createById">
          <el-input v-model="form.createById" placeholder="请输入 推送人ID" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
    <view-report ref="viewReport" :project-id="projectId" />
  </div>
</template>

<script>
import { listReport, getReport, delReport, addReport, updateReport } from "@/api/system/report";
import ProjectLabel from "@/components/Project/ProjectLabel";
import ViewReport from "@/components/Report/ViewReport";
import Step from "@/components/Case/CaseStep";
import ReportTools from "@/components/Report/ReportTools";
import FocusMemberList from "@/components/FocusMemberList";
import ReportTypeFlag from "@/components/Report/ReportTypeFlag";
import Cat2BugReportTemplateSelect from "@/components/Cat2BugReportTemplateSelect";

export default {
  name: "Report",
  components: { Step, ProjectLabel, ViewReport, ReportTools, FocusMemberList, ReportTypeFlag, Cat2BugReportTemplateSelect },
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
      // 报告表格数据
      reportList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      //  推送人ID时间范围
      daterangeReportTime: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        reportTitle: null,
        reportTime: null,
        reportDataType: null,
        createById: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        reportTitle: [
          { required: true, message: "报告标题不能为空", trigger: "blur" }
        ],
      },
    };
  },
  computed: {
    /** 获取项目id */
    projectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询报告列表 */
    getList() {
      this.loading = true;
      this.queryParams.projectId = this.projectId;
      this.queryParams.params = {};
      if (null != this.daterangeReportTime && '' != this.daterangeReportTime) {
        this.queryParams.params["beginReportTime"] = this.daterangeReportTime[0];
        this.queryParams.params["endReportTime"] = this.daterangeReportTime[1];
      }
      listReport(this.queryParams).then(response => {
        this.reportList = response.rows;
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
        reportId: null,
        reportTitle: null,
        reportTime: null,
        reportDescription: null,
        reportDataType: null,
        reportData: null,
        createById: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.daterangeReportTime = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    rowClickHandle(report) {
      this.$refs.viewReport.open(report.reportId);
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加报告";
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.reportId != null) {
            updateReport(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addReport(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleCommand(cmd) {
      switch (cmd){
        case 'add':
          this.handleReport();
          break;
        default:
          break;
      }
    },
    createReportHandle(template) {
      this.getList();
      this.$message.success('创建报告['+template.templateTitle+']成功');
    }
  }
};
</script>
<style>
.list {
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  /*width: 500px;*/
}
</style>
<style lang="scss" scoped>
.report-tools {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  > * {
    display: inline-block;
    justify-content: flex-start;
    margin-bottom: 0px;
    ::v-deep .el-form-item {
      margin-bottom: 0px;
    }
  }
}
.table-report-title {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
}
</style>
