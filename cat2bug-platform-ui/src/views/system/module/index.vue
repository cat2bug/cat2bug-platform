<template>
  <div class="app-container case-body module-page">
    <project-label class="module-project-label" />
    <div ref="moduleTools" class="module-tools" :class="{ 'wrapped-tools': moduleToolsWrapped }">
      <el-form class="left" :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="120px">
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

      <div ref="moduleToolsRight" class="module-tools-right">
          <el-button
            type="info"
            plain
            icon="el-icon-sort"
            size="small"
            @click="toggleExpandAll"
          >{{ $t('module.expand-collapse') }}</el-button>
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="small"
            @click="handleAdd"
            v-hasPermi="['system:module:add']"
          >{{ $t('module.new') }}</el-button>
      </div>
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
            class="red"
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
      moduleToolsWrapped: false,
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
    this.$nextTick(() => {
      this.syncModuleToolsWrapped();
    });
    window.addEventListener("resize", this.syncModuleToolsWrapped);
  },
  destroyed() {
    this.$floatMenu.windowsDestory();
    window.removeEventListener("resize", this.syncModuleToolsWrapped);
  },
  methods: {
    syncModuleToolsWrapped() {
      const measure = () => {
        const tools = this.$refs.moduleTools;
        const left = tools && tools.querySelector(".left");
        const right = this.$refs.moduleToolsRight;
        if (!tools || !left || !right) {
          this.moduleToolsWrapped = false;
          return;
        }
        this.moduleToolsWrapped = right.offsetTop - left.offsetTop > 4;
      };
      this.$nextTick(() => {
        if (this.moduleToolsWrapped) {
          this.moduleToolsWrapped = false;
          this.$nextTick(measure);
          return;
        }
        measure();
      });
    },
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
        name: 'module.new',
        visible: true,
        plain: true,
        type: 'primary',
        icon: 'add-tab',
        prompt: 'module.new',
        permissions: ['system:module:add'],
        click : this.handleAdd
      }]);
    },
    /** 查询模块列表 */
    getList() {
      this.loading = true;
      listModule(this.queryParams).then(response => {
        this.moduleList = this.handleTree(response.data, "moduleId", "modulePid");
        this.loading = false;
        this.syncModuleToolsWrapped();
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
.app-container.case-body.module-page {
  padding: 20px 20px 0;
  box-sizing: border-box;
}
.module-page {
  --case-toolbar-v-gap: 8px;
}
.module-page ::v-deep h3.module-project-label {
  margin-top: 0;
  margin-bottom: 10px;
}
.module-tools {
  width: 100%;
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: center;
  align-content: flex-start;
  column-gap: 12px;
  row-gap: 8px;
  margin-top: var(--case-toolbar-v-gap, 8px);
  margin-bottom: var(--case-toolbar-v-gap, 8px);
  .el-form-item {
    margin-bottom: 0;
  }
}
.module-tools > .left {
  flex: 1 1 auto;
  min-width: 0;
  max-width: 100%;
  width: auto;
  display: inline-flex;
  flex-wrap: nowrap;
  align-items: center;
  row-gap: 8px;
  column-gap: 8px;
  box-sizing: border-box;
  ::v-deep .el-form-item {
    flex: 0 0 auto;
    min-width: 0;
    margin-right: 0;
  }
  ::v-deep .el-form-item .el-form-item__content,
  ::v-deep .el-form-item .el-input {
    width: auto;
  }
}
.module-tools-right {
  flex: 0 0 auto;
  display: inline-flex;
  flex-wrap: nowrap;
  justify-content: flex-start;
  align-items: center;
  gap: 10px;
  > * {
    flex: 0 0 auto;
    width: auto;
    min-width: 0;
    margin-left: 0;
  }
}
.module-tools.wrapped-tools {
  > .left {
    flex: 1 1 100%;
    width: 100%;
    max-width: 100%;
    display: inline-flex;
    flex-wrap: wrap;
    box-sizing: border-box;
  }
  > .left ::v-deep .el-form-item {
    display: inline-flex;
    flex: 1 1 0;
    min-width: 220px;
    margin-right: 0;
    margin-bottom: 0;
  }
  > .left ::v-deep .el-form-item .el-form-item__content,
  > .left ::v-deep .el-form-item .el-input {
    width: 100% !important;
    max-width: 100%;
    box-sizing: border-box;
  }
  > .module-tools-right {
    margin-left: 0 !important;
    flex: 1 1 100%;
    width: 100%;
    display: inline-flex;
    flex-wrap: wrap;
    gap: 10px;
    justify-content: flex-start;
    align-items: center;
  }
}
</style>
