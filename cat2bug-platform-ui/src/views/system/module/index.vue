<template>
  <div class="app-container">
    <div class="module-tools">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="120px">
        <el-form-item prop="moduleName">
          <el-input
            v-model="queryParams.moduleName"
            :placeholder="$t('module.enter-module-name')"
            prefix-icon="el-icon-files"
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
            size="mini"
            @click="handleAdd"
            v-hasPermi="['system:module:add']"
          >{{ $t('add') }}</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="info"
            plain
            icon="el-icon-sort"
            size="mini"
            @click="toggleExpandAll"
          >{{ $t('module.expand-collapse') }}</el-button>
        </el-col>
      </el-row>
    </div>
    <el-table
      v-if="refreshTable"
      v-loading="loading"
      :data="moduleList"
      row-key="moduleId"
      :default-expand-all="isExpandAll"
      :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
    >
      <el-table-column :label="$t('module.name')" align="center" prop="moduleName" width="300" />
      <el-table-column :label="$t('remark')" align="center" prop="remark" />
      <el-table-column :label="$t('operate')" align="center" class-name="small-padding fixed-width" width="200">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:module:edit']"
          >{{ $t('modify') }}</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-plus"
            @click="handleAdd(scope.row)"
            v-hasPermi="['system:module:add']"
          >{{ $t('add') }}</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:module:remove']"
          >{{ $t('delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改模块对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" rules="rules" :model="form" :rules="rules" label-width="120px">
        <el-form-item :label="$t('module.parent-module')" prop="modulePid">
          <treeselect v-model="form.modulePid" :options="moduleOptions" :normalizer="normalizer" :placeholder="$t('module.please-select-parent-module')" />
        </el-form-item>
        <el-form-item :label="$t('module.name')" prop="moduleName">
          <el-input v-model="form.moduleName" :placeholder="$t('module.enter-module-name')" min="1" max="" />
        </el-form-item>
        <el-form-item :label="$t('remark')" prop="remark">
          <el-input v-model="form.remark" :placeholder="$t('please-enter-remark')" />
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
import { listModule, getModule, delModule, addModule, updateModule } from "@/api/system/module";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";

export default {
  name: "Module",
  components: {
    Treeselect
  },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 模块表格数据
      moduleList: [],
      // 模块树选项
      moduleOptions: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 是否展开，默认全部展开
      isExpandAll: true,
      // 重新渲染表格状态
      refreshTable: true,
      // 查询参数
      queryParams: {
        modulePid: null,
        moduleName: null,
        projectId: this.getProjectId()
      },
      // 表单参数
      form: {
        projectId: this.getProjectId()
      },
      // 表单校验
      rules: {
        moduleName: [
          { required: true, message: this.$t('module.name-cannot-empty'), trigger: "blur" },
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询模块列表 */
    getList() {
      this.loading = true;
      listModule(this.queryParams).then(response => {
        this.moduleList = this.handleTree(response.data, "moduleId", "modulePid");
        this.loading = false;
      });
    },
    /** 转换模块数据结构 */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children;
      }
      return {
        id: node.moduleId,
        label: node.moduleName,
        children: node.children
      };
    },
	/** 查询模块下拉树结构 */
    getTreeselect() {
      let params = {
        projectId: this.getProjectId()
      }
      listModule(params).then(response => {
        this.moduleOptions = [];
        const data = { moduleId: 0, moduleName: this.$i18n.t('module.root-node'), children: [] };
        data.children = this.handleTree(response.data, "moduleId", "modulePid");
        this.moduleOptions.push(data);
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
        moduleId: null,
        modulePid: null,
        moduleName: null,
        remark: null,
        projectId: this.getProjectId()
      };
      this.resetForm("form");
    },
    /** 获取项目id操作 */
    getProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 搜索按钮操作 */
    handleQuery() {
      console.log('--------')
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 新增按钮操作 */
    handleAdd(row) {
      this.reset();
      this.getTreeselect();
      if (row != null && row.moduleId) {
        this.form.modulePid = row.moduleId;
      } else {
        this.form.modulePid = 0;
      }
      this.open = true;
      this.title = this.$i18n.t('module.add');
    },
    /** 展开/折叠操作 */
    toggleExpandAll() {
      this.refreshTable = false;
      this.isExpandAll = !this.isExpandAll;
      this.$nextTick(() => {
        this.refreshTable = true;
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      this.getTreeselect();
      if (row != null) {
        this.form.modulePid = row.modulePid;
      }
      getModule(row.moduleId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = this.$i18n.t('module.modify');
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.moduleId != null) {
            updateModule(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('modify-success'));
              this.open = false;
              this.getList();
            });
          } else {
            addModule(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('add-success'));
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.$modal.confirm(this.$i18n.t('module.is-delete-module')).then(function() {
        return delModule(row.moduleId);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess(this.$i18n.t('delete-success'));
      }).catch(() => {});
    }
  }
};
</script>
<style lang="scss" scoped>
.module-tools {
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
