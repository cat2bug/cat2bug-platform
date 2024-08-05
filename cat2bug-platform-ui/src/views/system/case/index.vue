<template>
  <div class="app-container case-body">
    <project-label />
    <div class="case-tools">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item prop="caseNum">
          <el-input
            v-model="queryParams.caseNum"
            size="small"
            prefix-icon="el-icon-s-flag"
            :placeholder="$t('case.please-enter-id')"
            clearable
            @input="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="caseName">
          <el-input
            v-model="queryParams.caseName"
            size="small"
            prefix-icon="el-icon-files"
            :placeholder="$t('case.please-enter-title')"
            clearable
            @input="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="caseLevel">
          <el-input
            v-model="queryParams.caseLevel"
            size="small"
            prefix-icon="el-icon-s-data"
            :placeholder="$t('case.please-enter-level')"
            clearable
            @input="handleQuery"
          />
        </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-popover
            placement="top"
            trigger="click">
            <div class="row">
              <i class="el-icon-s-fold"></i>
              <h4>{{$t('defect.display-field')}}</h4>
            </div>
            <el-divider class="case-field-divider"></el-divider>
            <el-checkbox-group v-model="checkedFieldList" class="col" @change="checkedFieldListChange">
              <el-checkbox v-for="field in fieldList" :label="field" :key="field">{{$t(field)}}</el-checkbox>
            </el-checkbox-group>
            <el-button
              style="padding: 9px;"
              plain
              slot="reference"
              icon="el-icon-s-fold"
              size="small"
            ></el-button>
          </el-popover>
        </el-col>
        <el-col :span="1.5">
          <el-dropdown class="case-add-dropdown"
                       split-button
                       size="small"
                       type="primary"
                       v-hasPermi="['system:case:add']"
                       @click="handleAdd">
            <div class="title">
              <i class="el-icon-plus" />
              <span>{{$i18n.t('case.create')}}</span>
            </div>
            <el-dropdown-menu slot="dropdown" class="case-add-dropdown-menu">
              <el-dropdown-item @click.native="handleAdd"><i class="el-icon-plus" />{{$i18n.t('case.create')}}</el-dropdown-item>
              <el-divider class="case-add-dropdown-divider"></el-divider>
              <el-dropdown-item @click.native="handleImport"><i class="el-icon-upload2" />{{ $t('case.import') }}</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="success"
            size="small"
            @click="handleCloudCaseAdd"
            v-hasPermi="['system:case:add']"
          ><svg-icon icon-class="robot" />
            {{ $t('case.ai-create') }}</el-button>
        </el-col>
<!--        <el-col :span="1.5">-->
<!--          <el-button-->
<!--            type="warning"-->
<!--            plain-->
<!--            icon="el-icon-download"-->
<!--            size="small"-->
<!--            @click="handleExport"-->
<!--            v-hasPermi="['system:case:export']"-->
<!--          >导出</el-button>-->
<!--        </el-col>-->
      </el-row>
    </div>
<!--    模块树和用例列表区域-->
    <multipane layout="vertical" ref="multiPane" class="custom-resizer" @pane-resize-start="dragStopHandle">
<!--      树形模块选择组件-->
      <div class="tree-module" ref="treeModule" :style="treeModuleStyle">
        <tree-module :project-id="projectId" @node-click="moduleClickHandle"  v-resize="setDragComponentSize" />
      </div>
      <multipane-resizer :style="multipaneStyle"></multipane-resizer>
<!--      用例列表-->
      <div ref="caseContext" class="case-context">
        <el-table v-loading="loading" :data="caseList" @row-click="handleUpdate"  v-resize="setDragComponentSize">
          <el-table-column v-if="showField('id')" :label="$t('id')" align="left" prop="caseNum" width="80" sortable>
            <template slot-scope="scope">
              <span>{{ caseNumber(scope.row) }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="showField('title')" :label="$t('title')" align="left" prop="caseName" sortable>
            <template slot-scope="scope">
              <div class="table-case-title">
                <focus-member-list
                  v-model="scope.row.focusList"
                  module-name="case"
                  :data-id="scope.row.caseId" />
                <span>{{ scope.row.caseName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column v-if="showField('module')" :label="$t('module')" align="left" prop="moduleName" sortable />
          <el-table-column v-if="showField('level')" :label="$t('level')" align="left" prop="caseLevel" sortable width="80">
            <template slot-scope="scope">
              <cat2-bug-level :level="scope.row.caseLevel" />
            </template>
          </el-table-column>
          <el-table-column v-if="showField('preconditions')" :label="$t('preconditions')" align="left" prop="casePreconditions" sortable />
          <el-table-column v-if="showField('expect')" :label="$t('expect')" align="left" prop="caseExpect" sortable />
          <el-table-column v-if="showField('step')" :label="$t('step')" align="left" prop="caseStep" sortable>
            <template slot-scope="scope">
              <step :steps="scope.row.caseStep" />
            </template>
          </el-table-column>
          <el-table-column v-if="showField('image')" :label="$t('image')" :key="$t('image')" align="left" prop="imgUrls">
            <template slot-scope="scope">
              <cat2-bug-preview-image :images="getUrl(scope.row.imgUrls)" />
            </template>
          </el-table-column>
          <el-table-column v-if="showField('update-time')" :label="$t('update-time')" align="left" prop="updateTime" width="150" sortable>
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('operate')" align="center" class-name="small-padding fixed-width" width="150">
            <template slot-scope="scope">
              <el-button
                size="small"
                type="text"
                @click="addDefectHandle($event,scope.row)"
                v-hasPermi="['system:defect:add']"
              ><svg-icon icon-class="bug"></svg-icon>
                {{ $t('defect.create') }}</el-button>
              <el-button
                size="small"
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
    <cloud-case ref="cloudCaseDialog" @added="getList" />
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
        <div class="el-upload__text">{{ $t('case.import-prompt') }}<em> {{ $t('click.upload') }}</em></div>
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
import CloudCase from "@/components/Cloud/CloudCase";
import FocusMemberList from "@/components/FocusMemberList";
import Cat2BugPreviewImage from "@/components/Cat2BugPreviewImage";
import {checkPermi} from "@/utils/permission";
import {strFormat} from "@/utils";
import {getToken} from "@/utils/auth";

const TREE_MODULE_WIDTH_CACHE_KEY = 'case_tree_module_width';
/** 需要显示的测试用例字段列表在缓存的key值 */
const CASE_TABLE_FIELD_LIST_CACHE_KEY='case-table-field-list';

export default {
  name: "Case",
  components: {ProjectLabel,AddCase,Cat2BugLevel,Step,TreeModule,Multipane,MultipaneResizer,AddDefect,CloudCase,FocusMemberList,Cat2BugPreviewImage},
  data() {
    return {
      multipaneStyle: {'--marginTop':'0px'},
      treeModuleStyle: {'--treeModuleWidth':'300px'},
      // 表格中可以显示的字段列表
      checkedFieldList: [],
      // 所有属性类型
      fieldList: [],
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
      observer: null,
    };
  },
  watch: {
    "$i18n.locale": function (newVal, oldVal) {
      this.setFieldList();
    },
  },
  computed: {
    /** 字段是否显示 */
    showField: function () {
      return function (field) {
        return this.checkedFieldList.filter(f=>f==field).length>0;
      }
    },
    /** 用于显示的用例编号 */
    caseNumber: function () {
      return function (val) {
        return '#'+val.caseNum;
      }
    },
    /** 项目ID */
    projectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 字符转url数组 */
    getUrl: function () {
      return function (urls){
        let imgs = urls?urls.split(','):[];
        return imgs.map(i=>{
          return process.env.VUE_APP_BASE_API + i;
        })
      }
    },
  },
  created() {
    // 设置缺陷列表显示哪些列属性
    this.setFieldList();
  },
  mounted() {
    this.queryParams.projectId=this.projectId;
    this.getTreeModuleWidth();
    this.getList();
  },
  directives: {
    resize: {
      // 指令的名称
      bind(el, binding) {
        // el为绑定的元素，binding为绑定给指令的对象
        let width = ''
        let height = ''
        function isResize() {
          const style = document.defaultView.getComputedStyle(el)
          if (width !== style.width || height !== style.height) {
            binding.value({ width: style.width, height: style.height }) // 关键(这传入的是函数,所以执行此函数)
          }
          width = style.width
          height = style.height
        }
        el.__vueSetInterval__ = setInterval(isResize, 300)
      },
      unbind(el) {
        clearInterval(el.__vueSetInterval__)
      }
    }
  },
  methods: {
    strFormat,
    /** 设置列表显示的属性字段 */
    setFieldList() {
      this.fieldList = [
        'id','title','module','level', 'preconditions','expect','step','image','update-time'
      ];

      const fieldList = this.$cache.local.get(CASE_TABLE_FIELD_LIST_CACHE_KEY);
      if(fieldList) {
        this.checkedFieldList = JSON.parse(fieldList);
      } else {
        this.checkedFieldList = [];
        this.fieldList.forEach(f=>{
          this.checkedFieldList.push(f);
        });
      }
    },
    /** 测试用例列表属性字段改变操作 */
    checkedFieldListChange(field) {
      this.$cache.local.set(CASE_TABLE_FIELD_LIST_CACHE_KEY,JSON.stringify(field));
    },
    /** 获取树模型宽度 */
    getTreeModuleWidth() {
      let treeModuleWidth = this.$cache.session.get(TREE_MODULE_WIDTH_CACHE_KEY);
      this.treeModuleStyle['--treeModuleWidth'] = (treeModuleWidth?treeModuleWidth:300)+'px';
    },
    /** 设置树模型宽度到本地缓存 */
    cacheTreeModuleWidth() {
      this.$cache.session.set(TREE_MODULE_WIDTH_CACHE_KEY,this.$refs.treeModule.clientWidth);
    },
    /** 拖动事件完成 */
    dragStopHandle(pane, container, size) {
      this.cacheTreeModuleWidth();
    },
    /** 设置模块与用例列表中间拖动块的尺寸 */
    setDragComponentSize() {
      this.multipaneStyle['--marginTop'] = '0px';
      this.$nextTick(()=> {
        let pageHeight = Math.max(this.$refs.treeModule.scrollHeight || 0, this.$refs.caseContext.scrollHeight || 0, document.body.scrollHeight - 170)
        this.multipaneStyle['--marginTop'] = pageHeight + 'px';
      })
    },
    /** 查询测试用例列表 */
    getList() {
      this.loading = true;
      listCase(this.queryParams).then(response => {
        this.caseList = response.rows;
        this.total = response.total;
        this.loading = false;
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
    handleCloudCaseAdd() {
      this.$refs.cloudCaseDialog.open();
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
    /** 文件上传中处理 */
    handleFileUploadProgress(event, file, fileList) {
      this.upload.isUploading = true;
    },
    /** 文件上传成功处理 */
    handleFileSuccess(response, file, fileList) {
      this.upload.open = false;
      this.upload.isUploading = false;
      this.$refs.upload.clearFiles();
      let html = "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>"
        + "<label>"+response.data.message +"</label>";
      response.data.rows.forEach(r=>{
        html+='<div style="display: flex;flex-direction: row; align-items: center; padding: 5px 0px; border-top:1px solid #EBEEF5"><h4 style="margin: 0px 10px 0px 0px;">'+strFormat(this.$i18n.t('line'),r.rowNum+'')+'</h4><div style="display:flex;flex-direction:column;flex:1;overflow: hidden;">'
        r.messages.forEach(m=>{
          html+='<span>'+m+'</span>'
        });
        html+='</div></div>'
      });
      html+="</div>"
      this.$alert(html, this.$i18n.t('case.import-result').toString(), { dangerouslyUseHTMLString: true, customClass: 'case-upload-alert' });
      this.getList();
    },
    // 提交上传文件
    submitFileForm() {
      this.$refs.upload.submit();
    }
  }
};
</script>
<style>
  .case-upload-alert {
    width: auto;
  }
</style>
<style scoped lang="scss">
  .col {
    display: flex;
    flex-direction: column;
  }
  .row {
    display: flex;
    flex-direction: row;
    align-items: center;
    > * {
      margin: 0px 5px 0px 0px;
    }
  }
  .case-field-divider {
    margin: 8px 0px;
  }
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
  .table-case-title {
    display: inline-flex;
    flex-direction: column;
  }
  .case-add-dropdown {
    .title {
      display: inline-flex;
      flex-direction: row;
      gap: 5px;
      > * {
        margin: 0px;
      }
    }
    ::v-deep .el-button-group {
      display: flex;
      flex-direction: row;
      flex-wrap: nowrap;
    }
    ::v-deep button {
      color: #1890ff;
      background: #e8f4ff;
      border-color: #a3d3ff;
    }
    ::v-deep button:hover {
      background: #1890ff;
      border-color: #1890ff;
      color: #FFFFFF;
    }
    ::v-deep .el-dropdown__caret-button::before {
      background-color: #a3d3ff;
    }
  }
  .case-add-dropdown-menu {
    min-width: 120px;
  }
  .case-add-dropdown-divider {
    margin-top: 5px;
    margin-bottom: 5px;
    background-color: #E4E7ED;
    padding: 0px 5px;
  }
</style>
