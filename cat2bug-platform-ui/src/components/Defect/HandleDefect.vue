<template>
  <el-drawer
    ref="handleDefectDrawer"
    custom-class="defect-drawer-accent"
    size="55%"
    :visible.sync="visible"
    direction="rtl"
    :append-to-body="appendToBody"
    :before-close="closeDefectDrawer"
    @opened="loadDefectLogs">
    <template slot="title">
      <div class="defect-edit-header">
        <div class="defect-edit-title">
          <i class="el-icon-arrow-left" @click="cancel"></i>
          <div style="line-height: 36px;">
            <span class="defect-edit-title-num">#{{defect.projectNum}}</span>
            <span class="defect-edit-title-content">
                <defect-type-flag :defect="defect" />
                <defect-state-flag :defect="defect" />
                <focus-member-list
                  v-model="defect.focusList"
                  module-name="defect"
                  :data-id="defect.defectId"
                  :tooltip="false"
                />
                {{defect.defectName}}
            </span>
          </div>
        </div>
        <div class="report-edit-tools">
          <defect-tools :exclusions="defectToolsExclusions" :defect="defect" size="mini" @delete="deleteHandle" @restore="restoreHandle" @log="logHandle"></defect-tools>
        </div>
      </div>
    </template>
    <div ref="handleDefectBody" class="app-container defect-edit-body" v-loading="loading">
      <div class="defect-edit-body-log-first">
        <list-defect-log ref="defectLogFirst" :pageSize="1" />
      </div>
      <el-collapse v-model="activeNames">
        <el-collapse-item name="defectDescribe">
          <template slot="title">
            <span class="handle-defect-collapse-title" data-collapse-name="defectDescribe">{{ $i18n.t('describe') }}</span>
          </template>
          <markdown-it-vue v-if="defect.defectDescribe" :content="defect.defectDescribe+''" />
          <el-empty v-else :description="$t('no-data')"></el-empty>
        </el-collapse-item>
        <el-collapse-item name="customFields">
          <template slot="title">
            <span class="handle-defect-collapse-title" data-collapse-name="customFields">{{ $i18n.t('defect.base-info') }}</span>
          </template>
          <defect-custom-fields-display
            ref="customFieldsDisplay"
            :project-id="defect.projectId"
            :defect="defect"
            :defect-case="defectCase"
            :custom-fields="defect.customFields"
            @accordion-rows-change="onCustomFieldAccordionRowsChange"
          />
        </el-collapse-item>
        <el-collapse-item
          v-for="row in customFieldAccordionRows"
          :key="row.fieldKey"
          :name="customFieldAccordionCollapseName(row.fieldKey)"
        >
          <template slot="title">
            <span
              class="handle-defect-collapse-title"
              :data-collapse-name="customFieldAccordionCollapseName(row.fieldKey)"
            >{{ row.fieldLabel }}</span>
          </template>
          <defect-custom-field-accordion-body :row="row" />
        </el-collapse-item>
        <el-collapse-item v-if="defectCase && defectCase.caseId" name="caseId">
          <template slot="title">
            <span class="handle-defect-collapse-title" data-collapse-name="caseId">{{ $i18n.t('case') }}</span>
          </template>
          <case-card :case-model="defectCase" :state-visible="true" :step-index.sync="defect.caseStepId" :edit="false" />
        </el-collapse-item>
        <el-collapse-item name="log">
          <template slot="title">
            <span class="handle-defect-collapse-title" data-collapse-name="log">{{ $i18n.t('log') }}</span>
          </template>
          <list-defect-log ref="defectLog" :pageSize="5" show-comment />
        </el-collapse-item>
      </el-collapse>
      <div slot="footer" class="dialog-footer"></div>
    </div>
  </el-drawer>
</template>

<script>
import {addDefect, closeEditWindow, getDefect, updateDefect} from "@/api/system/defect";
import SelectProjectMember from "@/components/Project/SelectProjectMember"
import SelectModule from "@/components/Module/SelectModule"
import ListDefectLog from "@/components/ListDefectLog";
import DefectTools from "@/components/Defect/DefectTools";
import DefectTypeFlag from "@/components/Defect/DefectTypeFlag";
import DefectStateFlag from "@/components/Defect/DefectStateFlag";
import CaseCard from "@/components/Case/CaseCard";
import FocusMemberList from "@/components/FocusMemberList";
import {getCase} from "@/api/system/case";
import MarkdownItVue from "markdown-it-vue"
import 'markdown-it-vue/dist/markdown-it-vue.css'
import {normalizeDefectTypeAndLevel} from "@/utils/defect-defaults";
import handleDefectKbdHints from '@/mixins/handle-defect-kbd-hints'
import {
  accordionRowHasContent,
  customFieldAccordionCollapseName,
  isCustomFieldAccordionCollapseName
} from '@/components/DefectCustomField/defect-custom-field-accordion'

export default {
  name: "EditDefect",
  mixins: [handleDefectKbdHints],
  dicts: ['defect_level'],
  components: {
    SelectProjectMember,
    SelectModule,
    ListDefectLog,
    DefectTools,
    DefectTypeFlag,
    DefectStateFlag,
    CaseCard,
    MarkdownItVue,
    FocusMemberList,
    DefectCustomFieldsDisplay: () => import('@/components/DefectCustomField/DefectCustomFieldsDisplay'),
    DefectCustomFieldAccordionBody: () => import('@/components/DefectCustomField/DefectCustomFieldAccordionBody')
  },
  data() {
    return {
      loading: false,
      defectId: null,
      activeNames: [],
      customFieldAccordionRows: [],
      // 显示窗口
      visible: false,
      // 缺陷对象
      defect:{},
      // 操作工具中过滤的功能
      exclusions:['view'],
      // 用例
      defectCase: {},
      // 表单参数
      form: {
        defectLevel: 'middle'
      },
      // 表单校验
      rules: {
        handleBy: [
          { required: true, message: this.$i18n.t('defect.handle-by-cannot-empty'), trigger: "input" }
        ],
        defectName: [
          { required: true, message: this.$i18n.t('defect.defect-name-cannot-empty'), trigger: "input" }
        ],
        defectDescribe: [
          { required: true, message: this.$i18n.t('defect.describe-cannot-empty'), trigger: "input" }
        ],
      },
      options: {
        markdownIt: {
          linkify: true
        },
        linkAttributes: {
          attrs: {
            target: '_blank',
            rel: 'noopener'
          }
        }
      }
    }
  },
  props: {
    projectId: {
      type: Number,
      default: null
    },
    appendToBody: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    defectToolsExclusions() {
      const f = this.defect && this.defect.delFlag;
      if (f === '2' || f === 2) {
        /* 抽屉内已是查看态，仅保留恢复；「查看」仅在列表操作列展示 */
        return ['view', 'assign', 'repair', 'reject', 'pass', 'open', 'close', 'edit', 'delete'];
      }
      return this.exclusions;
    },
  },
  // 移除滚动条监听
  destroyed() {
  },
  watch: {
    defectId(id) {
      if (id != null && this.visible) {
        this.$nextTick(() => this.loadDefectLogs());
      }
    },
  },
  methods:{
    customFieldAccordionCollapseName,
    onCustomFieldAccordionRowsChange(rows) {
      const nextRows = rows || []
      this.customFieldAccordionRows = nextRows
      const validNames = new Set(
        nextRows.map((row) => customFieldAccordionCollapseName(row.fieldKey))
      )
      this.activeNames = this.activeNames.filter(
        (name) => !isCustomFieldAccordionCollapseName(name) || validNames.has(name)
      )
      nextRows.forEach((row) => {
        if (!accordionRowHasContent(row)) return
        const name = customFieldAccordionCollapseName(row.fieldKey)
        if (!this.activeNames.includes(name)) {
          this.activeNames.push(name)
        }
      })
    },
    // 获取缺陷信息
    getDefectInfo(defectId) {
      this.loading = true;
      this.activeNames = ['log', 'customFields']
      getDefect(defectId).then(res=>{
        this.loading = false;
        this.defect = res.data;
        if(this.defect.defectDescribe) {
          this.activeNames.push('defectDescribe');
        }
        if(this.defect){
          this.activeNames.push('caseId');
          this.getCase(this.defect.caseId);
        }
      }).catch(e=>{
        this.loading = false;
        this.visible = false;
      });
    },
    // 获取用例
    getCase(caseId) {
      if(caseId) {
        getCase(caseId).then(res=>{
          this.defectCase = res.data;
        })
      } else {
        this.defectCase = {};
      }
    },
    /** 刷新顶部与折叠内的缺陷日志（抽屉动画结束后也会触发一次） */
    loadDefectLogs() {
      if (!this.defectId || !this.visible) {
        return;
      }
      const id = this.defectId;
      const first = this.$refs.defectLogFirst;
      const list = this.$refs.defectLog;
      if (first && typeof first.open === 'function') {
        first.open(id);
      }
      if (list && typeof list.open === 'function') {
        list.open(id);
      }
    },
    // 打开操作
    open(defectId) {
      this.visible = true;
      this.reset();
      this.defectId = defectId;
      this.getDefectInfo(defectId);
    },
    // 取消按钮
    cancel() {
      this.$emit('close')
      this.visible = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        defectId: null,
        defectType: null,
        defectName: null,
        defectDescribe: null,
        annexUrls: null,
        projectId: null,
        testPlanId: null,
        caseId: null,
        dataSources: null,
        dataSourcesParams: null,
        moduleId: null,
        moduleVersion: null,
        createBy: null,
        updateTime: null,
        createTime: null,
        updateBy: null,
        defectState: null,
        caseStepId: null,
        handleBy: null,
        handleTime: null,
        defectLevel: 'middle'
      };
      this.activeNames = ['log', 'customFields']
      this.customFieldAccordionRows = []
      this.resetForm("form");
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          normalizeDefectTypeAndLevel(this.form);
          this.form.projectId = this.projectId;
          if (this.form.defectId != null) {
            updateDefect(this.form).then(res => {
              this.$modal.msgSuccess("修改成功");
              this.cancel();
              this.$emit('added',res)
            });
          } else {
            addDefect(this.form).then(res => {
              this.$modal.msgSuccess("新增成功");
              this.cancel();
              this.$emit('added',res)
            });
          }
        }
      });
    },
    /** 关闭缺陷抽屉窗口 */
    closeDefectDrawer(done) {
      done();
      closeEditWindow(this.defectId);
      this.cancel();
    },
    handleByChangeHandle(members) {
      console.log(members, this.form.handleBy);
    },
    logHandle(log) {
      this.getDefectInfo(this.defectId);
      this.$nextTick(() => this.loadDefectLogs());
      this.$emit('change', this.defect);
    },
    deleteHandle() {
      this.$emit('delete',this.defect);
      this.cancel();
    },
    restoreHandle() {
      this.$emit('change', this.defect);
      this.cancel();
    },
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-drawer {
  border-left: 3px solid #ff4949;
}
::v-deep .el-drawer__header {
  margin-bottom: 0px;
  max-width: calc(100vw);
}
.defect-edit-header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-direction: row;
  flex-wrap: wrap;
  .report-edit-tools {
    padding: 5px 0px;
    overflow: visible;
  }
  .defect-edit-title {
    display: inline-flex;
    justify-content: flex-start;
    align-items: center;
    flex-direction: row;
    overflow: hidden;
    > * {
      float:left;
    }
    .el-icon-arrow-left {
      font-size: 22px;
    }
    .el-icon-arrow-left:hover {
      cursor: pointer;
      color: #909399;
    }
    .defect-edit-title-name {
      //flex: 1;
      //overflow: hidden;
      //white-space: nowrap;
      //text-overflow: ellipsis;
    }
    .defect-edit-title-content {
      display: inline-flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 5px;
      font-size: 20px;
      color: #303133;
      font-weight: 500;
      line-height: 36px;
      vertical-align: middle;

      ::v-deep .defect-type-tag,
      ::v-deep .project-member-icons,
      ::v-deep .defect-state-tag,
      ::v-deep .el-tag--dark.el-tag--danger {
        float: none;
        margin: 0;
      }
    }
    .defect-edit-title-num, .defect-edit-title-name {
      font-size: 20px;
      color: #303133;
      font-weight: 500;
      float: left;
      margin-right:10px;
    }
    > * {
      margin-right:10px;
    }
  }
}
.defect-edit-body {
  padding-left: 30px;
  padding-right: 30px;
  .defect-edit-body-log-first {
    margin-bottom: 20px;
  }
  ::v-deep .el-collapse {
    .el-collapse-item__header {
      font-size: 16px;
      overflow: visible;
      position: relative;
    }
    .handle-defect-collapse-title {
      position: relative;
      display: inline-block;
      padding-right: 4px;
    }
    .el-collapse-item__content {
      padding-top: 20px;
      padding-bottom: 0;

      > :first-child {
        margin-top: 0;
      }
    }
    > .el-collapse-item > .el-collapse-item__wrap {
      border-bottom: 1px solid var(--border-color-light, #ebeef5);
    }
    border-width: 0px;
    .el-collapse-item:last-child {
      .el-collapse-item__wrap {
        border-width: 0px;
      }
    }
  }
  h5 {
    font-size: 18px;
    color: #303133;
    margin: 10px 0px;
  }
  .defect-edit-body-base {
    .el-col {
      margin-bottom: 20px;
      label {
        font-size: 14px;
        color: #303133;
        width: 120px;
        display: inline-block;
        justify-content: flex-start;
        margin-right: 10px;
        text-align: right;
      }
    }
  }
}
::v-deep .el-drawer__close-btn {
  display: none;
}
::v-deep.el-tag{
  font-size: 13px !important;
  vertical-align: 1px;
}
</style>

<style lang="scss">
  .defect-drawer-accent .defect-tools__bar [data-defect-tool].cat2bug-field-hint-host {
    position: relative;
    overflow: visible !important;
  }
  .defect-drawer-accent .defect-tools__bar .handle-defect-kbd-hint {
    right: 0;
    bottom: 0;
    transform: translate(50%, 50%);
  }
  .defect-drawer-accent .handle-defect-collapse-title .handle-defect-kbd-hint {
    right: -2px;
    bottom: -2px;
    transform: none;
  }
  .defect-drawer-accent .el-collapse-item__header.handle-defect-collapse-flash {
    animation: handleDefectCollapseFlash 1.2s ease forwards;
  }
  @keyframes handleDefectCollapseFlash {
    0%, 100% { background-color: transparent; }
    15%, 45% { background-color: rgba(64, 158, 255, 0.12); }
  }
  html.dark .defect-drawer-accent .el-collapse-item__header.handle-defect-collapse-flash {
    animation-name: handleDefectCollapseFlashDark;
  }
  @keyframes handleDefectCollapseFlashDark {
    0%, 100% { background-color: transparent; }
    15%, 45% { background-color: rgba(255, 206, 58, 0.15); }
  }
</style>
