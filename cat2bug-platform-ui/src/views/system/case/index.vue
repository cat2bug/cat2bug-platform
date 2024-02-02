<template>
  <div class="app-container case-body">
    <project-label />
    <div class="case-tools">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item prop="caseNum">
          <el-input
            v-model="queryParams.caseNum"
            prefix-icon="el-icon-s-flag"
            :placeholder="$t('case.please-enter-id')"
            clearable
            @input="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="caseName">
          <el-input
            v-model="queryParams.caseName"
            prefix-icon="el-icon-files"
            :placeholder="$t('case.please-enter-title')"
            clearable
            @input="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="caseLevel">
          <el-input
            v-model="queryParams.caseLevel"
            prefix-icon="el-icon-s-data"
            :placeholder="$t('case.please-enter-level')"
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
            icon="el-icon-download"
            size="mini"
            @click="handleImport"
            v-hasPermi="['system:case:import']"
          >{{ $t('import') }}</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAdd"
            v-hasPermi="['system:case:add']"
          >{{ $t('case.create') }}</el-button>
        </el-col>
<!--        <el-col :span="1.5">-->
<!--          <el-button-->
<!--            type="warning"-->
<!--            plain-->
<!--            icon="el-icon-download"-->
<!--            size="mini"-->
<!--            @click="handleExport"-->
<!--            v-hasPermi="['system:case:export']"-->
<!--          >导出</el-button>-->
<!--        </el-col>-->
      </el-row>
    </div>
    <multipane layout="vertical" class="custom-resizer" @pane-resize-start="multipaneResizeStopHandle">
<!--      树形模块选择组件-->
      <div class="tree-module" :style="treeModuleStyle" ref="treeModule">
        <tree-module :project-id="projectId" @node-click="moduleClickHandle" />
      </div>
      <multipane-resizer :style="multipaneStyle"></multipane-resizer>
<!--      用例列表-->
      <div ref="caseContext" class="case-context">
        <el-table v-loading="loading" :data="caseList" @row-click="handleUpdate">
          <el-table-column :label="$t('id')" align="center" prop="caseNum" width="80" sortable>
            <template slot-scope="scope">
              <span>{{ caseNumber(scope.row) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('title')" align="center" prop="caseName" sortable />
          <el-table-column :label="$t('module')" align="center" prop="moduleName" sortable />
          <el-table-column :label="$t('level')" align="center" prop="caseLevel" sortable width="80">
            <template slot-scope="scope">
              <cat2-bug-level :level="scope.row.caseLevel" />
            </template>
          </el-table-column>
          <el-table-column :label="$t('preconditions')" align="center" prop="casePreconditions" sortable />
          <el-table-column :label="$t('expect')" align="center" prop="caseExpect" sortable />
          <el-table-column :label="$t('step')" align="center" prop="caseStep" sortable>
            <template slot-scope="scope">
              <step :steps="scope.row.caseStep" />
            </template>
          </el-table-column>
          <el-table-column :label="$t('update-time')" align="left" prop="updateTime" width="150" sortable>
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('operate')" align="center" class-name="small-padding fixed-width" width="150">
            <template slot-scope="scope">
              <el-button
                size="mini"
                type="text"
                @click="addDefectHandle($event,scope.row)"
                v-hasPermi="['system:defect:add']"
              ><svg-icon icon-class="bug"></svg-icon>
                {{ $t('defect.create') }}</el-button>
              <el-button
                size="mini"
                type="text"
                class="red"
                icon="el-icon-delete"
                @click="handleDelete($event,scope.row)"
                v-hasPermi="['system:case:remove']"
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
      </div>
    </multipane>
    <add-case ref="addCaseDialog" :module-id="queryParams.params.modulePid" @added="getList" />
    <add-defect ref="addDefect" :project-id="projectId" />

    <!-- 用户导入对话框 -->
    <el-dialog :title="upload.title" :visible.sync="upload.open" width="400px" append-to-body>
      <el-upload
        ref="upload"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="upload.url + '?projectId=' + projectId"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :auto-upload="false"
        drag
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">{{ $t('case.import-prompt') }}<em>{{ $t('click.upload') }}</em></div>
        <div class="el-upload__tip text-center" slot="tip">
          <span>{{ strFormat($t('case.import-file-format'), 'xls、xlsx') }}</span>
          <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;" @click="importTemplate">
            {{ $t('download.template') }}</el-link>
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">{{ $t('import' )}}</el-button>
        <el-button @click="upload.open = false">{{ $t('cancel') }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import ProjectLabel from "@/components/Project/ProjectLabel";
import Cat2BugLevel from "@/components/Cat2BugLevel";
import Step from "@/views/system/case/components/step";
import TreeModule from "@/components/Module/TreeModule";
import { Multipane, MultipaneResizer } from 'vue-multipane';
import { listCase, delCase } from "@/api/system/case";
import AddCase from "@/components/Case/AddCase";
import AddDefect from "@/components/Defect/AddDefect";
import {checkPermi} from "@/utils/permission";
import {strFormat} from "@/utils";
import {getToken} from "@/utils/auth";

const TREE_MODULE_WIDTH_CACHE_KEY = 'case_tree_module_width';
export default {
  name: "Case",
  components: {ProjectLabel,AddCase,Cat2BugLevel,Step,TreeModule,Multipane,MultipaneResizer,AddDefect},
  data() {
    return {
      multipaneStyle: {'--marginTop':'0px'},
      treeModuleStyle: {'--treeModuleWidth':'300px'},
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
        projectId: this.projectId,
        params:{}
      },
      // 用例导入参数
      upload: {
        // 是否显示弹出层（用户导入）
        open: false,
        // 弹出层标题（用户导入）
        title: "",
        // 是否禁用上传
        isUploading: false,
        // 设置上传的请求头部
        headers: { Authorization: "Bearer " + getToken() },
        // 上传的地址
        url: process.env.VUE_APP_BASE_API + "/system/case/importData"
      },
    };
  },
  computed: {
    caseNumber: function () {
      return function (val) {
        return '#'+val.caseNum;
      }
    },
    projectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
  },
  created() {
  },
  mounted() {
    window.onresize=this.resize;
    this.queryParams.projectId=this.projectId;
    this.getTreeModuleWidth();
    this.getList();
  },
  methods: {
    strFormat,
    getTreeModuleWidth() {
      let treeModuleWidth = this.$cache.session.get(TREE_MODULE_WIDTH_CACHE_KEY);
      this.treeModuleStyle['--treeModuleWidth'] = (treeModuleWidth?treeModuleWidth:300)+'px';
    },
    /** 设置树模型宽度到本地缓存 */
    cacheTreeModuleWidth() {
      this.$cache.session.set(TREE_MODULE_WIDTH_CACHE_KEY,this.$refs.treeModule.clientWidth);
    },
    /** 拖动事件完成 */
    multipaneResizeStopHandle(pane, container, size) {
      this.cacheTreeModuleWidth();
    },
    resize() {
      let elContent = document.querySelector('.custom-resizer').getBoundingClientRect()
      let scrollY = document.documentElement.scrollTop || document.body.scrollTop
      let y = elContent.y + scrollY
      let pageHeight = Math.max(document.body.scrollHeight-y-20,this.$refs.caseContext.scrollHeight);
      this.multipaneStyle['--marginTop'] = pageHeight+'px';
    },
    /** 查询测试用例列表 */
    getList() {
      this.loading = true;
      listCase(this.queryParams).then(response => {
        this.caseList = response.rows;
        this.total = response.total;
        this.loading = false;
        this.multipaneStyle['--marginTop'] = '0px';
        this.$nextTick(this.resize)
      });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.$refs.addCaseDialog.open();
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      if(checkPermi(['system:case:edit'])) {
        this.$refs.addCaseDialog.open(row.caseId);
      }
    },
    /** 删除按钮操作 */
    handleDelete(e,row) {
      this.$modal.confirm(strFormat(this.$i18n.t('case.is-delete').toString(),this.caseNumber(row))).then(function() {
        return delCase(row.caseId);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess(this.$i18n.t('delete-success'));
      }).catch(() => {});
      e.stopPropagation();
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/case/export', {
        ...this.queryParams
      }, `case_${new Date().getTime()}.xlsx`)
    },
    /** 点击模块树中的某个模块操作 */
    moduleClickHandle(moduleId) {
      this.queryParams.params.modulePid = moduleId;
      this.handleQuery();
    },
    addDefectHandle(e,caseObj){
      this.$refs.addDefect.openByCase(caseObj);
      e.stopPropagation();
    },
    /** 导入按钮操作 */
    handleImport() {
      this.upload.title = this.$i18n.t('case.import');
      this.upload.open = true;
    },
    /** 下载模板操作 */
    importTemplate() {
      this.download('system/case/importTemplate?projectId='+this.projectId, {
      }, this.$i18n.t('case.template-file-name')+`${new Date().getTime()}.xlsx`)
    },
    // 文件上传中处理
    handleFileUploadProgress(event, file, fileList) {
      this.upload.isUploading = true;
    },
    // 文件上传成功处理
    handleFileSuccess(response, file, fileList) {
      this.upload.open = false;
      this.upload.isUploading = false;
      this.$refs.upload.clearFiles();
      this.$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", this.$i18n.t('case.import-result').toString(), { dangerouslyUseHTMLString: true });
      this.getList();
    },
    // 提交上传文件
    submitFileForm() {
      this.$refs.upload.submit();
    }
  }
};
</script>
<style scoped lang="scss">
  .tree-module {
    width: var(--treeModuleWidth);
    max-width: 50%;
  }
  .case-context {
    flex-grow: 1;
    overflow:hidden;
  }
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
  .red {
    color: #f56c6c;
  }
  .custom-resizer {
    width: 100%;
    height: auto;
  }
  .custom-resizer > .multipane-resizer {
    margin: 0; left: 0;
    display: flex;
    justify-content: center;
    align-items: flex-start;
    height: 100%;
    text-align: center;
    &:before {
      display: block;
      content: "";
      width: 3px;
      height: var(--marginTop);
      margin-top: 0px;
      margin-left: -1.5px;
      border-left: 1px solid #DCDFE6;
      border-right: 1px solid #DCDFE6;
    }
    &:hover {
      &:before {
        border-color: #CCC;
      }
    }
  }
</style>
