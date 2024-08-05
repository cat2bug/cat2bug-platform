<template>
  <div class="app-container">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('project.api-key')">
      </el-page-header>
    </el-row>
    <div class="api-tools">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="" prop="apiName">
          <el-input
            v-model="queryParams.apiName"
            size="small"
            prefix-icon="el-icon-s-flag"
            :placeholder="$t('project.api.please-enter-name')"
            clearable
            @input="handleQuery"
          />
        </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="small"
            @click="handleAdd"
            v-hasPermi="['system:api:add']"
          >{{ $t('project.api.create-key') }}</el-button>
        </el-col>
      </el-row>
    </div>
    <el-table v-loading="loading" :data="apiList" @selection-change="handleSelectionChange">
      <el-table-column :label="$t('project.api.name')" align="center" prop="apiName" />
      <el-table-column :label="$t('project.api.key')" align="center" prop="apiId" />
<!--      <el-table-column :label="$t('project.api.white-list')" align="center" prop="whiteList" />-->
      <el-table-column :label="$t('project.api.expire-time')" align="center" prop="expireTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.expireTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('remark')" align="center" prop="remark" />
      <el-table-column :label="$t('operate')" align="center" width="120" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:api:edit']"
          >{{ $t('modify') }}</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:api:remove']"
          >{{ $t('delete') }}</el-button>
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

    <!-- 添加或修改项目API对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="$t('project.api.name')" prop="apiName">
          <el-input v-model="form.apiName" :placeholder="$t('project.api.please-enter-name')" maxlength="30" />
        </el-form-item>
<!--        <el-form-item :label="$t('project.api.white-list')" prop="whiteList">-->
<!--          <el-input v-model="form.whiteList" :placeholder="$t('project.api.please-enter-white-list')" />-->
<!--        </el-form-item>-->
        <el-form-item :label="$t('project.api.expire-time')" prop="expireTime">
          <el-date-picker clearable
            v-model="form.expireTime"
            type="date"
            value-format="yyyy-MM-dd"
            :placeholder="$t('project.api.please-select-expire-time')">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input type="textarea"
                    v-model="form.remark"
                    :placeholder="$t('please-enter-remark')"
                    maxlength="255"
                    rows="3"
                    show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">{{ $t('ok') }}</el-button>
        <el-button @click="cancel">{{ $t('cancel') }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import ProjectLabel from "@/components/Project/ProjectLabel";
import { listApi, getApi, delApi, addApi, updateApi } from "@/api/system/api";

export default {
  name: "Api",
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
      // 项目API表格数据
      apiList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        projectId: null,
        userId: null,
        expireTime: null,
        apiName: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        apiName: [
          { required: true, message: this.$i18n.t('project.api.name--cannot-empty'), trigger: "blur" }
        ]
      }
    };
  },
  computed: {
    /** 获取项目id操作 */
    projectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
  },
  created() {
    this.getList();
  },
  methods: {
    /** 返回 */
    goBack() {
      this.$router.back();
    },
    /** 查询项目API列表 */
    getList() {
      this.loading = true;
      this.queryParams.projectId = this.projectId;
      listApi(this.queryParams).then(response => {
        this.apiList = response.rows;
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
        apiId: null,
        projectId: this.projectId,
        userId: null,
        whiteList: null,
        expireTime: null,
        remark: null,
        apiName: null
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
      this.ids = selection.map(item => item.apiId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加项目API";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const apiId = row.apiId || this.ids
      getApi(apiId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改项目API";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.apiId != null) {
            updateApi(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('modify-success'));
              this.open = false;
              this.getList();
            });
          } else {
            this.form.projectId = this.projectId;
            addApi(this.form).then(response => {
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
      const apiIds = row.apiId || this.ids;
      this.$modal.confirm(this.$i18n.t('project.api.is-delete')).then(function() {
        return delApi(apiIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess(this.$i18n.t('delete.success'));
      }).catch(() => {});
    },
  }
};
</script>
<style scoped lang="scss">
.api-tools {
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
