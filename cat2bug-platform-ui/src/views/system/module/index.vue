<template>
  <div class="app-container">
    <project-label />
    <div class="module-tools">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="120px">
        <el-form-item prop="moduleName">
          <el-input
            v-model="queryParams.moduleName"
            size="small"
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
            type="info"
            plain
            icon="el-icon-sort"
            size="small"
            @click="toggleExpandAll"
          >{{ $t('module.expand-collapse') }}</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="small"
            @click="handleAdd"
            v-hasPermi="['system:module:add']"
          >{{ $t('module.create') }}</el-button>
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
      <el-table-column :label="$t('module.name')" align="start" prop="moduleName" />
      <el-table-column :label="$t('annex')" align="left" prop="annexUrls">
        <template slot-scope="scope">
          <div>
            <cat2-bug-text :content="file" type="down" :tooltip="file" v-for="(file,index) in getUrl(scope.row.annexUrls)" :key="index" />
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('remark')" align="center" prop="remark" />
      <el-table-column :label="$t('operate')" align="center" class-name="small-padding fixed-width" width="200">
        <template slot-scope="scope">
          <el-button
            size="small"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:module:edit']"
          >{{ $t('modify') }}</el-button>
          <el-button
            size="small"
            type="text"
            icon="el-icon-plus"
            @click="handleAdd(scope.row)"
            v-hasPermi="['system:module:add']"
          >{{ $t('add') }}</el-button>
          <el-button
            size="small"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:module:remove']"
          >{{ $t('delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改模块对话框 -->
    <module-dialog ref="moduleDialog" :project-id="getProjectId()" @added="getList" @updated="getList" />
  </div>
</template>

<script>
import { listModule, getModule, delModule, addModule, updateModule } from "@/api/system/module";
import ModuleDialog from "@/components/Module/ModuleDialog";
import ProjectLabel from "@/components/Project/ProjectLabel";
import Cat2BugText from "@/components/Cat2BugText";

export default {
  name: "Module",
  components: {
    ProjectLabel,
    ModuleDialog,
    Cat2BugText
  },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 模块表格数据
      moduleList: [],
      // 是否展开，默认全部展开
      isExpandAll: true,
      // 重新渲染表格状态
      refreshTable: true,
      // 查询参数
      queryParams: {
        modulePid: null,
        moduleName: null,
        projectId: this.getProjectId(),
        pageNum: 1,
        pageSize: 10
      },
    };
  },
  computed: {
    getUrl: function () {
      return function (urls){
        if(urls) {
          let files = urls ? urls.split(',') : [];
          return files.map(i => {
            return process.env.VUE_APP_BASE_API + i;
          })
        } else {
          return [];
        }
      }
    },
  },
  created() {
    this.getList();
  },
  mounted() {
    // 初始化浮动菜单
    this.initFloatMenu();
  },
  methods: {
    /** 初始化浮动菜单 */
    initFloatMenu() {
      this.$floatMenu.windowsInit(document.querySelector('.main-container'));
      this.$floatMenu.resetMenus([{
        id: 'moduleExpandAll',
        name: 'module.expand-collapse',
        visible: true,
        plain: true,
        type: 'info',
        icon: 'up_down_switch',
        prompt: 'module.expand-collapse',
        click : this.toggleExpandAll
      },{
        id: 'moduleExpandAll',
        name: 'module.create',
        visible: true,
        plain: true,
        type: 'primary',
        icon: 'add-tab',
        prompt: 'module.create',
        permissions: ['system:module:add'],
        click : this.handleAdd
      }]);
    },
    // 移除滚动条监听
    destroyed() {
      this.$floatMenu.windowsDestory();
    },
    /** 查询模块列表 */
    getList() {
      this.loading = true;
      listModule(this.queryParams).then(response => {
        this.moduleList = this.handleTree(response.data, "moduleId", "modulePid");
        this.loading = false;
      });
    },
    /** 获取项目id操作 */
    getProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 新增按钮操作 */
    handleAdd(row) {
      let modulePid = null;
      if (row != null && row.moduleId) {
        modulePid = row.moduleId;
      } else {
        modulePid = 0;
      }
      this.$refs.moduleDialog.open(null,modulePid);
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
      this.$refs.moduleDialog.open(row.moduleId,row.modulePid);
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
