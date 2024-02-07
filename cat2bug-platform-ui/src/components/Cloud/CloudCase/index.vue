<template>
  <el-drawer
    :visible.sync="visible"
    :direction="direction"
    size="90%"
    :before-close="handleClose">
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
    <div class="case">
      <div class="case-search">
        <el-input type="textarea"
                  :readonly="loading"
                  maxlength="255"
                  rows="5"
                  show-word-limit v-model="searchData"
                  :placeholder="$t('case.ai-search-describe')">
          <svg-icon slot="prefix" icon-class="robot" style="margin-right: 10px;" />
        </el-input>
        <el-button type="success" :disabled="loading" @click="searchHandle">{{ $t('search') }}</el-button>
      </div>
      <div class="case-data" v-loading="loading" :element-loading-text="$t('case.load-prompted')">
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
            style="float: right"
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
      <el-form ref="form" :model="importForm" label-width="80px">
        <el-form-item :label="$t('module')" prop="teamName">
          <div class="import-dialog-module-form-item">
            <select-module v-model="importForm.moduleId" :project-id="projectId" @input="importFormModuleChangeHandle" />
            <span>{{$t('case.batch-import-module-describe')}}</span>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitImportForm">确 定</el-button>
        <el-button @click="cancelImportForm">取 消</el-button>
      </div>
    </el-dialog>
  </el-drawer>

</template>

<script>
import {listCase} from "@/api/cloud/case";
import {checkPermi} from "@/utils/permission";
import Cat2BugLevel from "@/components/Cat2BugLevel";
import CaseForm from "@/components/Case/CaseForm";
import Step from "@/views/system/case/components/step";
import CaseCard from "@/components/Case/CaseCard";
import SelectModule from "@/components/Module/SelectModule";
import { Multipane, MultipaneResizer } from 'vue-multipane';
import Label from "@/components/Cat2BugStatistic/Components/Label";
import {parseTime} from "@/utils/ruoyi"
import {addCase, batchAddCase, updateCase} from "@/api/system/case";
import {strFormat} from "@/utils";
import {delUser} from "@/api/system/user";
export default {
  name: "index",
  components: {Label, Cat2BugLevel,Step,Multipane,MultipaneResizer,CaseForm,CaseCard,SelectModule},
  data() {
    return {
      ids:new Set(),
      multipaneStyle: {'--marginTop':'0px'},
      visible: false,
      searchData: null,
      loading:false,
      caseList:[],
      caseContextVisible: true,
      pageIndex:1,
      pageSize:10,
      currentCase: {},
      caseFormStyle:null,
      importDialogVisible: false,
      importForm: {},
    }
  },
  props: {
    direction: {
      type: String,
      default: 'rtl'
    }
  },
  computed: {
    importStateName() {
      return function (c) {
        return c.isImport===true?'已导入':'未导入';
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
    parseTime,
    bodyScrollHandle(event) {
      const scrollTop = event.target.scrollTop;
      if(scrollTop>150) {
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
    },
    close() {
      this.visible = false;
    },
    handleClose(done) {
      // done();
    },
    searchHandle() {
      this.loading = true;
      listCase(this.searchData).then(res=>{
        this.loading = false;
        if(res.data && res.data.length>0) {
          this.caseList = [...res.data.map(c => {
            c.projectId = this.projectId;
            c.searchTime = new Date();
            return c;
          }), ...this.caseList];
          this.refreshCaseList();
          this.$message.success(strFormat(this.$i18n.t('case.ai-search-success-result'),res.data.length))
        } else {
          this.$message.warning(this.$i18n.t('case.ai-search-fail-result').toString())
        }
      }).catch(e=>{
        this.loading = false;
      })
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
            this.$modal.msgSuccess(this.$i18n.t('create-success'));
            row.caseId = response.data.caseId;
            this.$set(row,'isImport',true);
            this.$emit('added');
          });
        } else {
          updateCase(row).then(response => {
            this.$modal.msgSuccess(this.$i18n.t('modify-success'));
            this.$set(row,'isImport',true);
            this.$emit('added');
          });
        }
      }
      e.stopPropagation();
    },
    addDefectHandle(e,row) {

    },
    handleDelete(e,row) {
      this.caseList = this.caseList.filter(c => c !== row);
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
    }
  }
}
</script>

<style lang="scss" scoped>
.case {
  display: flex;
  flex-direction: column;
  padding: 20px;
  width: 100%;
  .case-search {
    display: inline-flex;
    width: 100%;
    margin-bottom: 20px;
    .el-textarea {
      flex: 1;
      ::v-deep .el-textarea__inner {
        height: 100%;
        border-radius: 5px 0 0 5px;
        padding-left: 45px;
        border-color: #67c23a;
      }
      ::v-deep .el-textarea__inner:hover {
        border-color: #85ce61;
      }
    }
    .el-textarea:before {
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
    .el-button {
      border-radius: 0 5px 5px 0;
      min-width: 80px;
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
</style>
