<template>
  <el-drawer
    :visible.sync="visible"
    :direction="direction"
    size="90%"
    :before-close="handleClose">
<!--    标题-->
    <template slot="title">
      <div class="case-search-header">
        <div class="case-search-title">
          <i class="el-icon-arrow-left" @click="close"></i>
          <h4 class="case-search-title-name"><svg-icon icon-class="robot" style="margin-right: 10px;" />{{ $t('case.ai-create') }}</h4>
        </div>
        <div>
        </div>
      </div>
    </template>
    <div class="case" v-loading="loading" :element-loading-text="$t('case.load-prompted')">
      <div ref="caseHistory" v-show="promptHistoryList && promptHistoryList.length>0" class="case-history">
        <el-row class="case-history-row" v-for="(prompt,index) in promptHistoryList" :key="index" @click.native="selectHistoryHandle(prompt)">
          <el-tag type="success" effect="dark">{{$t('DEMAND') + ' ' + (index+1).toString().padStart(2,'0')}}</el-tag>
          <span class="case-history-prompt" v-html="prompt.prompt"></span>
        </el-row>
      </div>
      <div ref="caseSearch" class="case-search">
          <cat2-bug-textarea
            :readonly="loading"
            v-model="prompt.prompt"
            maxlength="65536"
            rows="5"
            :show-default-tools="false"
            show-word-limit
            show-tools
            @change="handlePromptChange"
            :placeholder="$t('case.ai-search-describe').toString()">
            <template v-slot:tools>
              <div class="cloud-case-input-tools">
                <el-tooltip class="item" effect="dark" :content="$t('case.create-row-count')" placement="top">
                  <el-input-number class="cloud-case-prompt-input-number" size="mini" :min="1" :max="100" v-model="prompt.rowCount" @change="setDefaultRowCount(prompt.rowCount)"></el-input-number>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" :content="$t('clear')" placement="top">
                  <el-button class="cat2-bug-textarea-button" type="text" @click="handleClearPromptContent"><svg-icon icon-class="delete"></svg-icon></el-button>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" :content="$t('case.prompt-tooltip')" placement="top">
                  <el-popover
                    placement="bottom"
                    :title="$t('case.prompt')"
                    popper-class="cloud-case-prompt-popover"
                    ref="cloudCasePromptPopover"
                    v-model="casePromptVisible"
                    @show="handlePromptChange"
                    trigger="click">
                    <div class="cloud-case-prompt">
                      <div v-show="addCasePromptVisible">
                        <h4><i class="el-icon-plus"></i>{{ $t('case.add-prompt') }}</h4>
                        <add-case-prompt ref="addCasePrompt" :project-id="projectId" @added="handleAddCasePrompt" @cancel="addCasePromptVisible=false" />
                        <el-divider></el-divider>
                      </div>
                      <div class="cloud-case-prompt-tools">
                        <div>
                          <h4><i class="el-icon-tickets"></i>{{ $t('case.prompt-list') }}</h4>
                        </div>
                        <div>
                          <el-button icon="el-icon-plus" v-show="!addCasePromptVisible" size="mini" @click="handleOpenAddCasePrompt">{{ $t('case.add-prompt') }}</el-button>
                          <el-pagination
                            style="float: right"
                            :current-page.sync="casePromptQuery.pageNum"
                            :page-size="casePromptQuery.pageSize"
                            layout="prev, pager, next"
                            :total="casePromptTotal"
                            @size-change="getCasePrompt"
                            @current-change="getCasePrompt">
                          </el-pagination>
                        </div>
                      </div>
                      <div v-if="casePromptList && casePromptList.length>0">
                        <el-table
                          :data="casePromptList"
                          :show-header="false"
                          @row-click="handleCasePromptClick"
                          style="width: 100%">
                          <el-table-column
                            prop="casePromptContent">
                            <template slot-scope="scope">
                              <div v-show="!scope.row.isEditVisible" class="case-prompt-content" v-html="renderCasePromptContent(scope.row.casePromptContent)"></div>
                              <add-case-prompt v-show="scope.row.isEditVisible" :is-add="false" :case-prompt="scope.row" :auto-size="{ minRows: 1, maxRows: 15 }" @click.native="handleEditCasePromptClick" @updated="getCasePrompt" @cancel="scope.row.isEditVisible=false" />
                            </template>
                          </el-table-column>
                          <el-table-column
                            prop="operate"
                            width="120">
                            <template slot-scope="scope">
                              <el-button
                                size="mini"
                                type="text"
                                icon="el-icon-edit"
                                v-hasPermi="['system:case:add']"
                                @click="handlePromptEdit($event, scope.row)"
                              >{{ $t('modify') }}</el-button>
                              <el-button
                                size="mini"
                                type="text"
                                class="red"
                                icon="el-icon-close"
                                v-hasPermi="['system:case:add']"
                                @click="handlePromptDelete($event, scope.row)"
                              >{{ $t('delete') }}</el-button>
                            </template>
                          </el-table-column>
                        </el-table>
                        <el-pagination
                          style="float: right; margin-top: 10px;"
                          :current-page.sync="casePromptQuery.pageNum"
                          :page-size="casePromptQuery.pageSize"
                          layout="total, prev, pager, next"
                          :total="casePromptTotal"
                          @size-change="getCasePrompt"
                          @current-change="getCasePrompt">
                        </el-pagination>
                      </div>
                      <el-empty v-if="!casePromptList || casePromptList.length==0" :description="$t('case.empty-prompt')"></el-empty>
                    </div>
                    <el-button slot="reference" class="cat2-bug-textarea-button" type="text"><svg-icon icon-class="tips"></svg-icon></el-button>
                  </el-popover>
                </el-tooltip>
              </div>
            </template>
          </cat2-bug-textarea>
        <el-dropdown split-button type="success" :disabled="loading" @click="searchHandle" @command="searchCommandHandle">
          {{ $t('case.add-ai-case-prompt') }}
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="add">{{ $t('case.add-ai-case-prompt') }}</el-dropdown-item>
            <el-dropdown-item command="new">{{ $t('case.new-ai-case-prompt') }}</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
      <div class="case-data">
        <div class="case-table">
          <div class="case-table-tools">
            <div>
              <el-button :disabled="ids.size==0" class="el-icon-delete" type="danger" size="mini" @click="batchRemoveCase">
                {{ $t('batch-delete') }}</el-button>
              <el-button :disabled="ids.size==0" class="el-icon-download" type="primary" size="mini" @click="batchImportCase">{{ $t('batch-import') }}</el-button>
            </div>
            <div>
              <el-pagination
                style="float: right"
                :current-page.sync="pageIndex"
                :page-size="pageSize"
                layout="total, prev, pager, next"
                :total="caseTotal">
              </el-pagination>
            </div>
          </div>
          <el-table ref="caseTable" :data="pageList" @row-click="handleUpdate" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="50" align="center" />
            <el-table-column :label="$t('case')" align="center" >
              <template slot-scope="scope">
                <div class="case-info">
                  <div class="case-base">
                    <div>
                      <label>{{ $t('title') }}</label>
                      <span>{{ scope.row.caseName }}</span>
                    </div>
                    <div>
                      <label>{{ $t('module') }}</label>
                      <span>{{ scope.row.moduleName }}</span>
                    </div>
                    <div>
                      <label>{{ $t('level') }}</label>
                      <cat2-bug-level :level="scope.row.caseLevel" />
                    </div>
                    <div>
                      <label>{{ $t('preconditions') }}</label>
                      <span>{{ scope.row.casePreconditions }}</span>
                    </div>
                    <div>
                      <label>{{ $t('expect') }}</label>
                      <span>{{ scope.row.caseExpect }}</span>
                    </div>
                    <div>
                      <label>{{ $t('step') }}</label>
                      <step :steps="scope.row.caseStep" />
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="$t('state')" align="center" prop="createTime" width="100">
              <template slot-scope="scope">
                <div class="case-state">
                <el-tag :type="importStateType(scope.row)" size="mini" >{{ importStateName(scope.row) }}</el-tag>
                <span>{{
                    parseTime(scope.row.searchTime, '{h}:{i}:{s}')
                  }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="$t('operate')" align="center" class-name="small-padding fixed-width" width="120">
              <template slot-scope="scope">
                <el-button
                  size="mini"
                  type="text"
                  class="red"
                  icon="el-icon-download"
                  @click="handleImport($event,scope.row)"
                  v-hasPermi="['system:case:import']"
                >{{ $t('import') }}</el-button>
                <el-button
                  size="mini"
                  type="text"
                  class="red"
                  icon="el-icon-delete"
                  @click="handleDelete($event,scope.row)"
                >{{ $t('delete') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            style="float: right; margin-top: 5px"
            :current-page.sync="pageIndex"
            :page-size="pageSize"
            layout="total, prev, pager, next"
            :total="caseTotal">
          </el-pagination>
        </div>
        <div ref="caseContext" class="case-context" v-show="caseContextVisible">
          <case-form class="case-form" ref="caseForm" :form.sync="currentCase" :style="caseFormStyle" />
        </div>
      </div>
    </div>
    <!-- 将用例导入到系统对话框 -->
    <el-dialog :title="$t('import')" :visible.sync="importDialogVisible" width="500px" append-to-body>
      <el-form ref="form" :model="importForm" label-width="100px">
        <el-form-item :label="$t('module')" prop="teamName">
          <div class="import-dialog-module-form-item">
            <select-module v-model="importForm.moduleId" :project-id="projectId" @input="importFormModuleChangeHandle" />
            <span>{{$t('case.batch-import-module-describe')}}</span>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitImportForm">{{ $t('ok') }}</el-button>
        <el-button @click="cancelImportForm">{{ $t('cancel') }}</el-button>
      </div>
    </el-dialog>
    <handle-case-prompt ref="handCasePrompt" @submit="handleCasePromptCorrect" />
  </el-drawer>
</template>

<script>
import Cat2BugLevel from "@/components/Cat2BugLevel";
import Cat2BugTextarea from "@/components/Cat2BugTextarea";
import CaseForm from "@/components/Case/CaseForm";
import Step from "@/views/system/case/components/step";
import CaseCard from "@/components/Case/CaseCard";
import SelectModule from "@/components/Module/SelectModule";
import AddCasePrompt from "@/components/Cloud/CloudCase/AddCasePrompt";
import HandleCasePrompt from "@/components/Cloud/CloudCase/HandleCasePrompt";
import { Multipane, MultipaneResizer } from 'vue-multipane';
import Label from "@/components/Cat2BugStatistic/Components/Label";
import {parseTime} from "@/utils/ruoyi"
import {addCase, batchAddCase, updateCase} from "@/api/system/case";
import {strFormat} from "@/utils";
import {makeCaseList} from "@/api/ai/AiCase";
import i18n from "@/utils/i18n/i18n";
import {delCasePrompt, listCasePrompt} from "@/api/system/CasePrompt";

const DEFAULT_ROW_COUNT_KEY = 'case_default_row_count';
const PATTERN = /\$\{\s*[0-9a-zA-z]{1,255}\s*\}/g;

export default {
  name: "index",
  components: { Label, Cat2BugLevel, Step, Multipane, MultipaneResizer, CaseForm, CaseCard, SelectModule, Cat2BugTextarea, AddCasePrompt, HandleCasePrompt },
  data() {
    return {
      prompt: {
        prompt: null,
        context: null,
        rowCount: this.getDefaultRowCount()
      },
      // 是否查询用例时保存提示
      isSearchAndSavePrompt: true,
      promptHistoryList: [],
      ids:new Set(),
      multipaneStyle: {'--marginTop':'0px'},
      visible: false,
      loading:false,
      caseList:[],
      caseContextVisible: true,
      pageIndex:1,
      pageSize:10,
      currentCase: {
        projectId: this.projectId
      },
      caseFormStyle:null,
      importDialogVisible: false,
      importForm: {},
      casePromptVisible: false,
      casePromptTotal: 0,
      casePromptQuery: {
        pageSize: 10,
        pageNum: 1,
        projectId: null,
        casePromptContent: null
      },
      casePromptList: [],
      addCasePromptVisible: false,
    }
  },
  props: {
    direction: {
      type: String,
      default: 'rtl'
    }
  },
  computed: {
    renderCasePromptContent: function () {
      return function (content) {
        if(!content) return null;
        const matches = content.match(PATTERN);
        if(matches && matches.length>0) {
          let html = content;
          matches.forEach(m=>{
            html = html.replace(m,`<span style="
                background-color: #ecf5ff;
                display: inline-block;
                height: 26px;
                padding: 0 10px;
                margin: 0 5px;
                line-height: 24px;
                font-size: 12px;
                color: #409eff;
                border: 1px solid #d9ecff;
                border-radius: 4px;
                box-sizing: border-box;
                white-space: nowrap;">${m}</span>`);
          });
          return html;
        }
        return content;
      }
    },
    importStateName() {
      return function (c) {
        return c.isImport===true?this.$i18n.t('case.imported'):this.$i18n.t('case.not-imported');
      }
    },
    importStateType() {
      return function (c) {
        return c.isImport?'info':'danger';
      }
    },
    projectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    caseTotal: function () {
      return this.caseList.length;
    },
    pageList: function () {
      return this.caseList.filter((c,index)=>index>=(this.pageIndex-1)*this.pageSize && index<this.pageIndex*this.pageSize);
    },
  },
  methods: {
    setDefaultRowCount(row) {
      this.$cache.local.set(DEFAULT_ROW_COUNT_KEY, row);
    },
    getDefaultRowCount() {
      return this.$cache.local.get(DEFAULT_ROW_COUNT_KEY) || 5;
    },
    parseTime,
    /** 初始化浮动菜单 */
    initFloatMenu() {
      this.$floatMenu.windowsInit(document.querySelector('.main-container'));
      this.$floatMenu.resetMenus();
    },
    bodyScrollHandle(event) {
      const scrollTop = event.target.scrollTop;
      const height = this.$refs.caseSearch.offsetHeight + this.$refs.caseHistory.offsetHeight + 30;
      if(scrollTop>height) {
        this.caseFormStyle = 'position: fixed;top: 70px;';
      } else {
        this.caseFormStyle = '';
      }
    },
    open() {
      this.visible = true;
      this.$nextTick(()=>{
        const container = document.querySelector('.el-drawer__body');
        container.addEventListener('scroll',this.bodyScrollHandle);
      });
      this.initFloatMenu();
    },
    close() {
      this.$emit('close')
      const container = document.querySelector('.el-drawer__body');
      container.removeEventListener('scroll',this.bodyScrollHandle);
      this.visible = false;
      this.resetPrompt();
    },
    handleClose(done) {
      // done();
    },
    selectHistoryHandle(prompt) {
      this.caseList = prompt.list;
    },
    searchCommandHandle(command) {
      switch (command) {
        case 'add':
          this.searchHandle();
          break;
        case 'new':
          this.newPrompt();
          break;
      }
    },
    resetPrompt() {
      this.promptHistoryList = [];
      this.caseList = [];
      this.prompt = {
        prompt: null,
        context: null,
        rowCount: this.getDefaultRowCount()
      };
      this.currentCase={
        projectId: this.projectId
      };
      this.casePromptQuery = {
        pageSize: 10,
        pageNum: 1,
        projectId: null,
        casePromptContent: null
      }
      this.casePromptList = [];
      this.$refs['caseForm'].reset();
    },
    /** 新话题 */
    newPrompt() {
      const self = this;
      this.$modal.confirm(
        this.$i18n.t('case.new-ai-case-prompt-alert'),
        this.$i18n.t('prompted').toString(),
        {
          confirmButtonText: i18n.t('ok').toString(),
          cancelButtonText: i18n.t('cancel').toString(),
          type: "warning"
        }).then(function() {
          self.resetPrompt();
        }).catch(() => {});
    },
    handlePromptChange() {
      if(this.prompt.prompt && this.prompt.prompt.length>30) {
        return;
      }
      this.casePromptVisible = true;
      this.addCasePromptVisible = false;
      this.$refs.addCasePrompt.reset();
      this.casePromptQuery.pageNum = 1;
      this.getCasePrompt();
    },
    /** 搜索用例 */
    searchHandle() {
      if(!this.prompt.prompt) {
        this.$message.error(this.$i18n.t('case.prompt-content-not-empty').toString())
        return;
      }
      let startSeconds = new Date().getTime();
      this.loading = true;
      this.caseList = [];
      makeCaseList(this.prompt).then(res=>{
        this.loading = false;
        if(res.data && res.data.cases.length>0) {
          // 添加之前创建的用例列表，目前注释掉
          // this.caseList = [...res.data.cases.map(c => {
          //   c.projectId = this.projectId;
          //   c.searchTime = new Date();
          //   return c;
          // }), ...this.caseList];
          this.promptHistoryList.push({
            prompt: this.prompt.prompt.split('\n').join('<br />'),
            context: this.prompt.context
          });
          this.$nextTick(() => {
            const container = this.$refs.caseHistory;
            container.scrollTop = container.scrollHeight;
          });
          this.caseList = res.data.cases.map(c => {
            c.projectId = this.projectId;
            c.searchTime = new Date();
            return c;
          });
          this.promptHistoryList[this.promptHistoryList.length-1].list = this.caseList;
          this.prompt.context = res.data.context;
          this.prompt.prompt = null;
          this.refreshCaseList();
          this.$message.success(strFormat(this.$i18n.t('case.ai-search-success-result'),Math.floor((new Date().getTime()-startSeconds)/1000),res.data.cases.length))
        } else {
          this.$message.warning(this.$i18n.t('case.ai-search-fail-result').toString())
        }
      }).catch(e=>{
        this.loading = false;
      });
      // 保存提示词
      // if(this.isSearchAndSavePrompt) {
      //   let prompt = {
      //     casePromptContent: this.prompt.prompt,
      //     projectId: this.projectId
      //   }
      //   addCasePrompt(prompt).then(res=>{});
      // }
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      if(this.caseContextVisible) {
        this.currentCase = row;
        this.$refs.caseForm.setCase(row);
      }
    },
    validateCase(row) {
      return true;
    },
    handleImport(e,row) {
      if (this.validateCase(row)) {
        if (!row.caseId) {
          addCase(row).then(response => {
            this.$modal.msgSuccess(this.$i18n.t('import-success'));
            row.caseId = response.data.caseId;
            this.$set(row,'isImport',true);
            this.$emit('added');
          });
        } else {
          updateCase(row).then(response => {
            this.$modal.msgSuccess(this.$i18n.t('re-import-success'));
            this.$set(row,'isImport',true);
            this.$emit('added');
          });
        }
      }
      e.stopPropagation();
    },
    addDefectHandle(e,row) {

    },
    /** 删除用例 */
    handleDelete(e,row) {
      this.caseList = this.caseList.filter(c => c !== row);
      e.stopPropagation();
    },
    /** 编辑提示词 */
    handlePromptEdit(e, row) {
      row.isEditVisible = true;
      e.stopPropagation();
    },
    /** 删除提示词 */
    handlePromptDelete(e,row) {
      if(!row) return;
      delCasePrompt(row.casePromptId).then(res=>{
        this.$modal.msgSuccess(this.$i18n.t('delete.success'));
        this.getCasePrompt();
      });
      e.stopPropagation();
    },
    /** 设置模块与用例列表中间拖动块的尺寸 */
    setDragComponentSize() {

    },
    /** 拖动事件完成 */
    dragStopHandle(pane, container, size) {
    },
    /** 批量删除 */
    batchRemoveCase() {
      let self = this;
      this.$modal.confirm(this.$i18n.t('case.is-batch-delete')).then(function() {
        self.caseList = self.caseList.filter((item) => !self.ids.has(item.index));
        self.ids = new Set();
        self.refreshCaseList();
        self.$modal.msgSuccess(self.$i18n.t('delete.success'));
      });
    },
    /** 批量导入 */
    batchImportCase() {
      this.importForm = {};
      this.importDialogVisible = true;
    },
    refreshCaseList() {
      this.caseList = this.caseList.map((c,index)=>{
        c.index = index;
        return c;
      })
    },
    /** 获取表格中选中的用例 */
    handleSelectionChange(selection) {
      this.ids = new Set(selection.map(c=>c.index));
    },
    submitImportForm() {
      let self = this;
      let importCaseList = self.caseList.filter((item) => self.ids.has(item.index));
      if(this.importForm.moduleId) {
        importCaseList.forEach(c=>{
          if(!c.moduleId) {
            if(this.importForm.moduleId) {
              c.moduleId = this.importForm.moduleId;
            }
            if(this.importForm.moduleName) {
              self.$set(c,'moduleName',this.importForm.moduleName);
            }
          }
        });
      }
      batchAddCase(importCaseList).then(res=>{
        if(importCaseList.length == res.data.length){
          for(let i=0;i<importCaseList.length;i++){
            importCaseList[i].caseId = res.data[i].caseId;
            self.$set(importCaseList[i],'isImport',true);
          }
        }
        self.importDialogVisible=false;
        self.$modal.msgSuccess(self.$i18n.t('import.success'));
        self.$emit('added');
      })
    },
    cancelImportForm() {
      this.importDialogVisible = false;
    },
    importFormModuleChangeHandle(moduleId,module) {
      this.importForm.moduleName = module.moduleName;
    },
    /** 获取提示 */
    getCasePrompt() {
      this.casePromptQuery.projectId = this.projectId;
      this.casePromptQuery.casePromptContent = this.prompt.prompt;
      listCasePrompt(this.casePromptQuery).then(res=>{
        this.casePromptList = res.rows.map(p=>{
          p.isEditVisible = false;
          return p;
        });
        this.casePromptTotal = res.total;
        this.handleCasePromptOpened();
      })
    },
    handleCasePromptOpened() {
      this.$nextTick(()=>{
        const rect = this.$refs.caseSearch.getBoundingClientRect();
        let el = document.querySelector('.cloud-case-prompt-popover');
        el.style.left = rect.x+'px';
        el.style.width = rect.width+'px';
        el.style.maxHeight = (document.body.clientHeight - parseFloat(el.style.top) - 20) + 'px';
        el.style.overflow = 'auto';
      })
    },
    handleCasePromptClick(row, column, event) {
      this.casePromptVisible = false;
      if(this.$refs.handCasePrompt.check(row)) {
        this.prompt.prompt = row.casePromptContent;
      } else {
        this.$refs.handCasePrompt.open(row);
      }
    },
    handleEditCasePromptClick(e) {
      e.stopPropagation();
    },
    handleOpenAddCasePrompt() {
      this.addCasePromptVisible = true;
      this.$refs.addCasePrompt.reset();
    },
    handleAddCasePrompt() {
      this.addCasePromptVisible = false;
      this.getCasePrompt();
    },
    handleCasePromptCorrect(promptContent) {
      this.prompt.prompt = promptContent;
    },
    handleClearPromptContent() {
      this.prompt.prompt = null;
    }
  }
}
</script>
<style>
  .cloud-case-prompt-input-number {
    line-height: 22px;
    width: 80px;
  }
  .cloud-case-prompt-input-number>.el-input-number__decrease {
    border-radius: 2px 0 0 2px;
  }
  .cloud-case-prompt-input-number>.el-input-number__increase {
    border-radius: 0 2px 2px 0;
  }
  .cloud-case-prompt-input-number>.el-input-number__decrease, .cloud-case-prompt-input-number>.el-input-number__increase {
    color: #67c23a;
    background: #f0f9eb;
    border-color: #c2e7b0;
    width: 22px;
    height: 20px;
    line-height: 20px;
  }
  .cloud-case-prompt-input-number>.el-input-number__decrease:hover,
  .cloud-case-prompt-input-number>.el-input-number__increase:hover,
  .el-input-number__increase:hover:not(.is-disabled) ~ .el-input .el-input__inner:not(.is-disabled),
  .el-input-number__decrease:hover:not(.is-disabled) ~ .el-input .el-input__inner:not(.is-disabled) {
    border-color: #67c23a;
  }
  .cloud-case-prompt-input-number>div>.el-input__inner {
    height: 22px;
    line-height: 22px;
    border-radius: 2px;
    padding: 0 20px;
    border-color: #c2e7b0;
    color: #67c23a;
  }
  .cloud-case-prompt {
    display: inline-flex;
    flex-direction: column;
    gap: 5px;
    width: 100%;
  }
  .cloud-case-prompt > * {
    width: 100%;
  }
  .cloud-case-prompt-tools {
    width: 100%;
    display: inline-flex;
    justify-content: space-between;
    flex-direction: row;
    align-items: center;
  }
  .cloud-case-prompt-tools h4 > i {
    margin-right: 5px;
  }
  .case-prompt-content {
    display: inline-block;
    width: 100%;
    white-space: pre;
  }
</style>
<style lang="scss" scoped>
.case {
  display: flex;
  flex-direction: column;
  padding: 20px;
  width: 100%;
  .case-history {
    max-height: 150px;
    overflow-y: auto;
    margin-bottom: 10px;
    .case-history-row {
      padding: 5px 15px;
      display: flex;
      flex-direction: row;
      align-items: center;
      .case-history-prompt {
        padding-left: 20px;
        font-size: 15px;
        color: #606266;
      }
    }
    .case-history-row:hover {
      background-color: #f6f9fe;
      border-radius: 5px;
      cursor: pointer;
    }
  }
  .case-search {
    display: inline-flex;
    width: 100%;
    margin-bottom: 20px;
    .cat2-bug-textarea-button {
      color: #67c23a;
      background-color: white;
      padding: 3px;
      border-radius: 2px;
    }
    .cat2-bug-textarea-button:hover {
      background-color: #f0f9eb;
      border:1px solid #67c23a;
    }
    .cat2bug-textarea {
      flex: 1;
      ::v-deep textarea {
        height: 100%;
        border-radius: 5px 0 0 5px;
        padding-left: 45px;
        border-color: #67c23a;
        border-width: 1px 0px 1px 1px ;
      }
      ::v-deep textarea:hover {
        border-color: #85ce61;
      }
    }
    .cat2bug-textarea:before {
      content: '';
      display: block;
      background-size: cover; /* 调整大小以适应容器 */
      width: 20px;
      height: 20px;
      background-image: url("../../../assets/images/keyborad.png");
      position: absolute;
      top: 7px;
      left: 15px;
    }
    ::v-deep .el-button-group {
      height: 100%;
      .el-button {
        height: 100%;
      }
      .el-button:first-child {
        border-radius: 0px;
      }
    }
    ::v-deep .el-button-group {
      margin: 0px;
      border-radius: 0px 5px 5px 0px;
    }
  }
  .case-data {
    display: flex;
    flex-direction: row;
    width: 100%;
    .case-table {
      flex: 1;
      .el-table, .case-table-tools {
        width: 100%;
      }
      .case-table-tools {
        display: flex;
        flex-direction: row;
        justify-content: space-between;
        margin-bottom: 10px;
      }
    }
    .case-context {
      right: 20px;
      border-left: 1px solid #EBEEF5;
      margin-left: 20px;
    }
  }
}
.case-form, .case-context {
  width: 800px;
}
.case-info {
  .case-base {
    > div {
      display: flex;
      flex-direction: row;
      label {
        margin-right: 10px;
        min-width: 80px;
        text-align: right;
      }
      label:after {
        content: ':';
      }
      span {
        flex: 1;
        text-align: left;
      }
    }
  }
}
.case-state {
  display: flex;
  flex-direction: column;
  justify-content: center;
  > * {
    margin-bottom: 5px;
  }
}
.case-search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-direction: row;
  .case-search-title {
    display: inline-flex;
    justify-content: flex-start;
    align-items: center;
    flex-direction: row;
    .el-icon-arrow-left {
      font-size: 22px;
    }
    .el-icon-arrow-left:hover {
      cursor: pointer;
      color: #909399;
    }
    .case-search-title-name {
      max-width: 400px;
      overflow: hidden;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      font-size: 20px;
      color: #303133;
      font-weight: 500;
      margin-top: 10px;
      margin-bottom: 10px;
    }
    > * {
      margin-right: 10px;
    }
  }
}
.import-dialog-module-form-item {
  display: flex;
  flex-direction: column;
  span {
    color: #F56C6C;
  }
}
::v-deep .el-drawer {
  border-left: 3px solid #67c23a;
}
::v-deep .el-drawer__header {
  margin-bottom: 0px;
}
::v-deep .el-drawer__close-btn {
  display: none;
}
::v-deep .el-loading-spinner {
  circle {
    stroke: #13ce66;
  }
  .el-loading-text {
    color: #13ce66;
    font-size: 18px;
  }
}
.cloud-case-input-tools {
  display: inline-flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  gap: 5px;
}
</style>
