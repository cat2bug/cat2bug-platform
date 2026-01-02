<template>
  <div class="app-container">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('project.ai-account-manager')">
      </el-page-header>
    </el-row>
    <div class="ai-account-tools">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
        <el-form-item label="" prop="accountName">
          <el-input
            prefix-icon="el-icon-setting"
            v-model="queryParams.accountName"
            placeholder="请输入配置名称"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="" prop="modelName">
          <el-input
            prefix-icon="el-icon-coin"
            v-model="queryParams.modelName"
            placeholder="请输入模型名称"
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
            v-hasPermi="['ai:account:add']"
          >{{ $t('project.ai.create') }}</el-button>
        </el-col>
<!--        <el-col :span="1.5">-->
<!--          <el-button-->
<!--            type="success"-->
<!--            plain-->
<!--            icon="el-icon-edit"-->
<!--            size="mini"-->
<!--            :disabled="single"-->
<!--            @click="handleUpdate"-->
<!--            v-hasPermi="['ai:account:edit']"-->
<!--          >修改</el-button>-->
<!--        </el-col>-->
<!--        <el-col :span="1.5">-->
<!--          <el-button-->
<!--            type="danger"-->
<!--            plain-->
<!--            icon="el-icon-delete"-->
<!--            size="mini"-->
<!--            :disabled="multiple"-->
<!--            @click="handleDelete"-->
<!--            v-hasPermi="['ai:account:remove']"-->
<!--          >删除</el-button>-->
<!--        </el-col>-->
<!--        <el-col :span="1.5">-->
<!--          <el-button-->
<!--            type="warning"-->
<!--            plain-->
<!--            icon="el-icon-download"-->
<!--            size="mini"-->
<!--            @click="handleExport"-->
<!--            v-hasPermi="['ai:account:export']"-->
<!--          >导出</el-button>-->
<!--        <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>-->
      </el-row>
    </div>
    <el-table v-loading="loading" :data="accountList" @selection-change="handleSelectionChange">
<!--      <el-table-column type="selection" width="55" align="center" />-->
      <el-table-column :label="$t('project.ai.account-name')" align="center" prop="accountName" width="200" />
      <el-table-column :label="$t('project.ai.url')" align="center" prop="aiUrl" />
      <el-table-column :label="$t('project.ai.model-name')" align="center" prop="modelName" width="200" />
      <el-table-column :label="$t('project.ai.max-token')" align="center" prop="maxCompletionTokens" width="100" />
      <el-table-column :label="$t('project.ai.app-key')" align="center" prop="apiKey" width="300"/>
      <el-table-column :label="$t('operate')" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['ai:account:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['ai:account:remove']"
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

    <!-- 添加或修改OpenAI账号对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="$t('project.ai.account-name')" prop="accountName">
          <el-input v-model="form.accountName" placeholder="请输入账号名称" :maxlength="64" />
        </el-form-item>
        <el-form-item :label="$t('project.ai.url')" prop="aiUrl">
          <el-input v-model="form.aiUrl" placeholder="请输入AI服务网址" type="textarea" :rows="2" :maxlength="255"/>
        </el-form-item>
        <el-form-item :label="$t('project.ai.model-name')" prop="modelName">
          <el-input v-model="form.modelName" placeholder="请输入模型名称" :maxlength="255" />
        </el-form-item>
        <el-form-item :label="$t('project.ai.max-token')" prop="maxCompletionTokens">
          <el-input-number v-model="form.maxCompletionTokens" :min="1" :max="65536" label="请输入最大Token"></el-input-number>
        </el-form-item>
        <el-form-item :label="$t('project.ai.app-key')" prop="apiKey">
          <el-input v-model="form.apiKey" placeholder="请输入密钥" :maxlength="255" show-password />
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
import { listAccount, getAccount, delAccount, addAccount, updateAccount } from "@/api/ai/AIAccount";

export default {
  name: "Account",
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
      // OpenAI账号表格数据
      accountList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        modelName: null,
        projectId: null,
        accountName: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        aiUrl: [
          { required: true, message: "AI服务网址不能为空", trigger: "blur" }
        ],
        modelName: [
          { required: true, message: "模型名称不能为空", trigger: "blur" }
        ],
        apiKey: [
          { required: true, message: "密钥不能为空", trigger: "blur" }
        ],
        projectId: [
          { required: true, message: "关联项目ID不能为空", trigger: "blur" }
        ],
        accountName: [
          { required: true, message: "账号名称不能为空", trigger: "blur" }
        ]
      }
    };
  },
  computed: {
    currentProjectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
  },
  created() {
    this.getList();
  },
  methods: {
    /** 返回上一页 */
    goBack() {
      this.$router.back();
    },
    /** 查询OpenAI账号列表 */
    getList() {
      this.loading = true;
      listAccount(this.queryParams).then(response => {
        this.accountList = response.rows;
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
        accountId: null,
        aiUrl: null,
        modelName: null,
        maxCompletionTokens: null,
        apiKey: null,
        createBy: null,
        projectId: null,
        accountName: null
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
      this.ids = selection.map(item => item.accountId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加OpenAI账号";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const accountId = row.accountId || this.ids
      getAccount(accountId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改OpenAI账号";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.accountId != null) {
            updateAccount(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('modify-success'));
              this.open = false;
              this.getList();
            });
          } else {
            this.form.projectId = this.currentProjectId;
            addAccount(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('create-success'));
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const accountIds = row.accountId || this.ids;
      this.$modal.confirm('是否确认删除OpenAI账号编号为"' + accountIds + '"的数据项？').then(function() {
        return delAccount(accountIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('ai/account/export', {
        ...this.queryParams
      }, `account_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
<style lang="scss" scoped>
.ai-account-tools {
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
</style>
