<template>
  <el-drawer
    ref="defectFormDrawer"
    custom-class="defect-drawer-accent"
    size="65%"
    :visible.sync="visible"
    direction="rtl"
    :append-to-body="appendToBody"
    :close-on-press-escape="false"
    :before-close="closeDefectDrawer">
    <template slot="title">
      <div class="defect-edit-form-header">
        <div class="defect-edit-form-title">
          <i class="el-icon-arrow-left" @click="requestCloseDefectFormDrawer"></i>
          <h3>
            {{ $t('defect.modify') }}
            <span v-if="form.projectNum" class="defect-edit-form-num">#{{ form.projectNum }}</span>
          </h3>
        </div>
        <div>
          <el-button class="defect-kbd-hint-host" @click="requestCloseDefectFormDrawer" icon="el-icon-close" size="small">
            {{ $t('close') }}
            <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint" aria-hidden="true">{{ dialogCloseShortcutLabel }}</span>
          </el-button>
          <el-button class="defect-kbd-hint-host" type="primary" icon="el-icon-finished" @click="submitForm" size="small">
            {{ $t('defect.save') }}
            <span class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
          </el-button>
        </div>
      </div>
    </template>
    <div class="app-container defect-edit-body">
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <defect-form-ordered-fields
          v-if="projectId"
          ref="orderedFields"
          :project-id="projectId"
          :form="form"
          :config="config"
          :plan-time-range.sync="planTimeRange"
          :describe-tools="describeTools"
          :defect-field-layout="defectFieldLayout"
          :is-builtin-defect-field-visible="isBuiltinDefectFieldVisible"
          @module-change="moduleChangeHandle"
          @step-change="stepChangeHandle"
        >
          <template #describe-tools>
            <project-ai-model-select
              v-if="projectId"
              :project-id="projectId"
              v-model="defectAiModelId"
              embed-ai-button
              :ai-loading="aiButtonLoading"
              :ai-tooltip="$t('defect.ai-filling-in').toString()"
              @service-type="defectAiServiceType = $event"
              @ai-click="createDefectByAiHandle"
            />
          </template>
        </defect-form-ordered-fields>
      </el-form>
    </div>
  </el-drawer>
</template>

<script>
import { configDefect, getDefect, updateDefect } from '@/api/system/defect'
import ProjectAiModelSelect from '@/components/Ai/ProjectAiModelSelect.vue'
import DefectFormOrderedFields from '@/components/Defect/DefectFormOrderedFields'
import {
  makeDefectMember,
  makeDefectModule,
  makeDefectTitle,
  makeDefectType,
  makeDefectVersion
} from '@/api/ai/AiDefect'
import { strFormat } from '@/utils'
import { normalizeDefectTypeAndLevel } from '@/utils/defect-defaults'
import defectBuiltinFieldVisibility from '@/mixins/defect-builtin-field-visibility'
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import defectFormDrawerClose from '@/mixins/defect-form-drawer-close'
import formFieldHints from '@/mixins/form-field-hints'

export default {
  name: 'EditDefectDialog',
  dicts: ['defect_level'],
  mixins: [defectBuiltinFieldVisibility, dialogFormShortcuts, defectFormDrawerClose, formFieldHints],
  components: { ProjectAiModelSelect, DefectFormOrderedFields },
  data() {
    return {
      describeTools: [],
      aiButtonLoading: false,
      defectAiModelId: null,
      defectAiServiceType: 'ollama',
      planTimeRange: [],
      config: {},
      visible: false,
      form: {
        defectLevel: 'middle',
        customFields: {}
      },
      rules: {
        handleBy: [
          { required: true, message: this.$i18n.t('defect.handle-by-cannot-empty'), trigger: 'input' }
        ],
        defectName: [
          { required: true, message: this.$i18n.t('defect.defect-name-cannot-empty'), trigger: 'input' }
        ]
      }
    }
  },
  props: {
    projectId: {
      type: Number,
      default: null
    },
    defectId: {
      type: Number,
      default: null
    },
    appendToBody: {
      type: Boolean,
      default: true
    }
  },
  methods: {
    getFieldHintContainer() {
      return (this.$refs.form && this.$refs.form.$el) || this.$el
    },
    applyDefectFormRulesForBuiltinLayout() {
      const rules = {}
      if (this.isBuiltinDefectFieldVisible('handleBy')) {
        rules.handleBy = [
          { required: true, message: this.$i18n.t('defect.handle-by-cannot-empty'), trigger: 'input' }
        ]
      }
      if (this.isBuiltinDefectFieldVisible('defectName')) {
        rules.defectName = [
          { required: true, message: this.$i18n.t('defect.defect-name-cannot-empty'), trigger: 'input' }
        ]
      }
      this.rules = rules
    },
    getDefectConfig() {
      configDefect().then(res => {
        this.config = res.data
      })
    },
    getDefectInfo(defectId) {
      return getDefect(defectId).then(res => {
        this.form = res.data
        this.planTimeRange = []
        if (this.form.planStartTime) {
          this.planTimeRange.push(this.form.planStartTime)
        }
        if (this.form.planEndTime) {
          this.planTimeRange.push(this.form.planEndTime)
        }
      })
    },
    open(defectId) {
      const id = defectId != null ? defectId : this.defectId
      if (id == null) return
      this.reset()
      this.getDefectConfig()
      this.visible = true
      this.getDefectInfo(id).then(() => {
        this.$nextTick(() => {
          const orderedFields = this.$refs.orderedFields
          if (orderedFields) {
            orderedFields.refreshCustomFieldDefaults()
            const selectModule = orderedFields.getSelectModuleRef && orderedFields.getSelectModuleRef()
            if (selectModule && typeof selectModule.resetFromForm === 'function') {
              selectModule.resetFromForm(this.form.moduleId)
            }
            const selectCase = orderedFields.getSelectCaseRef && orderedFields.getSelectCaseRef()
            if (this.form.moduleId && selectCase) {
              selectCase.search(this.form.moduleId)
            }
          }
          this.captureDefectFormCloseBaseline()
        })
      })
    },
    doCloseDefectFormDrawer() {
      this.visible = false
      this.defectFormCloseBaseline = null
      this.reset()
    },
    closeDefectDrawer(done) {
      this.requestCloseDefectFormDrawer({ done })
    },
    reset() {
      this.planTimeRange = []
      this.defectAiModelId = null
      this.defectAiServiceType = 'ollama'
      this.form = {
        defectId: null,
        defectType: null,
        defectName: null,
        defectDescribe: null,
        annexUrls: null,
        imgUrls: null,
        projectId: null,
        projectNum: null,
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
        defectLevel: 'middle',
        customFields: {}
      }
      this.resetForm('form')
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          normalizeDefectTypeAndLevel(this.form)
          if (this.planTimeRange.length > 1) {
            this.form.planStartTime = this.planTimeRange[0]
            this.form.planEndTime = this.planTimeRange[1]
          }
          updateDefect(this.form).then(() => {
            this.$modal.msgSuccess(this.$i18n.t('modify-success'))
            this.doCloseDefectFormDrawer()
            this.$emit('log', this.form)
          })
        }
      })
    },
    moduleChangeHandle() {
      const selectCase = this.$refs.orderedFields && this.$refs.orderedFields.getSelectCaseRef()
      selectCase && selectCase.search(this.form.moduleId)
    },
    stepChangeHandle(index) {
      this.form.caseStepId = index
    },
    defectHandleByNeedsAiFill() {
      const hb = this.form.handleBy
      if (hb == null) return true
      return Array.isArray(hb) && hb.length === 0
    },
    createDefectByAiHandle() {
      if (this.aiButtonLoading) {
        this.$message.error(this.$i18n.t('defect.ai-re-run').toString())
        return
      }
      const startSeconds = new Date().getTime()
      if (!this.form.defectDescribe) {
        this.$message.error(this.$i18n.t('defect.describe-cannot-empty').toString())
        return
      }
      if (!this.defectAiModelId) {
        this.$message.error(this.$i18n.t('case.model-content-not-empty').toString())
        return
      }
      this.aiButtonLoading = true
      let makeTitle
      let makeModule
      let makeType
      let makeMember
      let makeVersion
      const params = {
        projectId: this.projectId,
        describe: this.form.defectDescribe,
        modelId: this.defectAiModelId != null ? String(this.defectAiModelId) : null,
        serviceType: this.defectAiServiceType || 'ollama'
      }
      if (!this.form.defectName) {
        makeTitle = makeDefectTitle(params).then(res => {
          if (!this.form.defectName && res.code === 200 && res.data.title) {
            this.form.defectName = res.data.title
            this.$message.success(strFormat(this.$i18n.t('fill-finish'), this.$i18n.t('title').toString()))
          }
        })
      }
      if (!this.form.moduleId) {
        makeModule = makeDefectModule(params).then(res => {
          if (!this.form.moduleId && res.code === 200 && res.data && res.data.moduleId) {
            this.form.moduleId = res.data.moduleId
            this.$message.success(strFormat(this.$i18n.t('fill-finish'), this.$i18n.t('module').toString()))
          }
        })
      }
      if (!this.form.defectType) {
        makeType = makeDefectType(params).then(res => {
          if (!this.form.defectType && res.code === 200 && res.data.type) {
            this.form.defectType = res.data.type
            this.$message.success(strFormat(this.$i18n.t('fill-finish'), this.$i18n.t('type').toString()))
          }
        })
      }
      if (this.defectHandleByNeedsAiFill()) {
        makeMember = makeDefectMember(params).then(res => {
          if (this.defectHandleByNeedsAiFill() && res.code === 200 && res.data.memberId) {
            this.$set(this.form, 'handleBy', [res.data.memberId])
            this.$message.success(strFormat(this.$i18n.t('fill-finish'), this.$i18n.t('handle-by').toString()))
          }
        })
      }
      if (!this.form.moduleVersion) {
        makeVersion = makeDefectVersion(params).then(res => {
          if (this.form.moduleVersion || res.code !== 200 || !res.data) {
            return
          }
          const raw = res.data
          const v =
            raw.version != null && String(raw.version).trim() !== ''
              ? String(raw.version).trim()
              : raw.moduleVersion != null && String(raw.moduleVersion).trim() !== ''
                ? String(raw.moduleVersion).trim()
                : null
          if (!v) {
            return
          }
          this.$set(this.form, 'moduleVersion', v)
          this.$message.success(
            strFormat(this.$i18n.t('fill-finish'), this.$i18n.t('version').toString())
          )
        })
      }
      if (!makeTitle && !makeModule && !makeType && !makeMember && !makeVersion) {
        this.aiButtonLoading = false
        this.$message.error(this.$i18n.t('defect.no-fields-analyze').toString())
        return
      }
      Promise.all([makeTitle, makeModule, makeType, makeMember, makeVersion])
        .then(() => {
          const self = this
          setTimeout(() => {
            self.aiButtonLoading = false
            self.$message({
              message: strFormat(
                self.$i18n.t('defect.analyze-finish'),
                Math.floor((new Date().getTime() - startSeconds) / 1000)
              ),
              type: 'success',
              duration: 5000
            })
          }, 1000)
        })
        .catch(e => {
          this.aiButtonLoading = false
          this.$message.error(e)
        })
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-drawer__header {
  margin-bottom: 0;
  overflow: visible;
}
::v-deep .el-drawer__close-btn {
  display: none;
}
.defect-edit-form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  overflow: visible;
  > div:last-child {
    overflow: visible;
  }
}
.defect-edit-form-title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  overflow: hidden;
  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 500;
    line-height: 1.4;
  }
  .defect-edit-form-num {
    margin-left: 6px;
    font-weight: 600;
    color: #909399;
  }
  .el-icon-arrow-left {
    font-size: 22px;
    cursor: pointer;
    flex-shrink: 0;
  }
  .el-icon-arrow-left:hover {
    color: #909399;
  }
}
::v-deep .defect-edit-body {
  .defect-plan-datetimerange.el-date-editor--datetimerange.el-input__inner {
    width: 100% !important;
    max-width: 100%;
    min-width: 0 !important;
    box-sizing: border-box;
  }
}
.cat2-bug-textarea-button {
  span {
    margin-left: 3px;
    font-size: 12px;
  }
}
.cat2-bug-textarea-button[handle='true'] {
  background-color: rgb(236, 245, 255);
  border-radius: 15px;
}
</style>
