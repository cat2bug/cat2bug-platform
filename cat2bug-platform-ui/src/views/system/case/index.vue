<template>
  <div class="app-container">
    <project-label />
    <div class="case-tools">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item prop="caseName">
          <el-input
            v-model="queryParams.caseName"
            placeholder="请输入用例名称"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
<!--        <el-form-item label="模块id" prop="moduleId">-->
<!--          <el-input-->
<!--            v-model="queryParams.moduleId"-->
<!--            placeholder="请输入模块id"-->
<!--            clearable-->
<!--            @keyup.enter.native="handleQuery"-->
<!--          />-->
<!--        </el-form-item>-->
        <el-form-item prop="caseLevel">
          <el-input
            v-model="queryParams.caseLevel"
            placeholder="请输入用例级别"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="caseNum">
          <el-input
            v-model="queryParams.caseNum"
            placeholder="请输入用例号码"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
<!--        <el-form-item>-->
<!--          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>-->
<!--          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>-->
<!--        </el-form-item>-->
      </el-form>

      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAdd"
            v-hasPermi="['system:case:add']"
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
            v-hasPermi="['system:case:edit']"
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
            v-hasPermi="['system:case:remove']"
          >删除</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="warning"
            plain
            icon="el-icon-download"
            size="mini"
            @click="handleExport"
            v-hasPermi="['system:case:export']"
          >导出</el-button>
        </el-col>
      </el-row>
    </div>
    <el-table v-loading="loading" :data="caseList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="测试用例" align="center" prop="caseId" />
      <el-table-column label="用例名称" align="center" prop="caseName" />
      <el-table-column label="模块id" align="center" prop="moduleId" />
      <el-table-column label="用例类型" align="center" prop="caseType" />
      <el-table-column label="预期" align="center" prop="caseExpect" />
      <el-table-column label="步骤" align="center" prop="caseStep" />
      <el-table-column label="用例级别" align="center" prop="caseLevel" />
      <el-table-column label="前置条件" align="center" prop="casePreconditions" />
      <el-table-column label="用例号码" align="center" prop="caseNum" />
      <el-table-column label="项目编号" align="center" prop="projectId" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:case:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:case:remove']"
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

    <!-- 添加或修改测试用例对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用例名称" prop="caseName">
          <el-input v-model="form.caseName" placeholder="请输入用例名称" />
        </el-form-item>
        <el-form-item label="模块id" prop="moduleId">
          <el-input v-model="form.moduleId" placeholder="请输入模块id" />
        </el-form-item>
        <el-form-item label="预期" prop="caseExpect">
          <el-input v-model="form.caseExpect" placeholder="请输入预期" />
        </el-form-item>
        <el-form-item label="用例级别" prop="caseLevel">
          <el-input v-model="form.caseLevel" placeholder="请输入用例级别" />
        </el-form-item>
        <el-form-item label="前置条件" prop="casePreconditions">
          <el-input v-model="form.casePreconditions" placeholder="请输入前置条件" />
        </el-form-item>
        <el-form-item label="用例号码" prop="caseNum">
          <el-input v-model="form.caseNum" placeholder="请输入用例号码" />
        </el-form-item>
        <el-form-item label="项目编号" prop="projectId">
          <el-input v-model="form.projectId" placeholder="请输入项目编号" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import ProjectLabel from "@/components/ProjectLabel";
import { listCase, getCase, delCase, addCase, updateCase } from "@/api/system/case";

export default {
  name: "Case",
  components: {ProjectLabel},
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
      // 测试用例表格数据
      caseList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        caseName: null,
        moduleId: null,
        caseType: null,
        caseExpect: null,
        caseLevel: null,
        casePreconditions: null,
        caseNum: null,
        projectId: null
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
    /** 查询测试用例列表 */
    getList() {
      this.loading = true;
      listCase(this.queryParams).then(response => {
        this.caseList = response.rows;
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
        caseId: null,
        caseName: null,
        moduleId: null,
        caseType: null,
        caseExpect: null,
        caseStep: null,
        caseLevel: null,
        casePreconditions: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        caseNum: null,
        projectId: null
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
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.caseId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加测试用例";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const caseId = row.caseId || this.ids
      getCase(caseId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改测试用例";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.caseId != null) {
            updateCase(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addCase(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const caseIds = row.caseId || this.ids;
      this.$modal.confirm('是否确认删除测试用例编号为"' + caseIds + '"的数据项？').then(function() {
        return delCase(caseIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/case/export', {
        ...this.queryParams
      }, `case_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
<style scoped lang="scss">
  .case-tools {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    margin-top: 10px;
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
</style>
