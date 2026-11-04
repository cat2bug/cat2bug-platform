<template>
  <el-drawer
    ref="mergeDrawer"
    v-model="visible"
    class="defect-drawer-accent merge-defect-drawer"
    size="85%"
    direction="rtl"
    :append-to-body="true"
    :show-close="false"
    :close-on-press-escape="false"
    :before-close="closeDrawer"
  >
    <template #header>
      <div class="merge-defect-drawer__header">
        <div class="merge-defect-drawer__title">
          <legacy-icon icon="el-icon-arrow-left" @click="requestClose" />
          <h3>{{ $t('defect.merge-title') }}</h3>
        </div>
        <div>
          <el-button size="small" :icon="$legacyIcon('el-icon-close')" @click="requestClose">
            {{ $t('cancel') }}
          </el-button>
          <el-button
            type="primary"
            size="small"
            :icon="$legacyIcon('el-icon-finished')"
            :disabled="!canSubmit || submitting"
            :loading="submitting"
            @click="submitMerge"
          >
            {{ $t('defect.merge') }}
          </el-button>
        </div>
      </div>
    </template>

    <div v-loading="loadingSources" class="merge-defect-drawer__body">
      <multipane
        layout="vertical"
        class="custom-resizer merge-defect-multipane"
        @paneResizeStop="onMergePaneResizeStop"
      >
        <section
          class="merge-defect-drawer__left"
          aria-label="merge result form"
          :style="leftPaneStyle"
        >
          <p v-if="!masterDefectId" class="merge-defect-drawer__hint">
            {{ $t('defect.merge-no-master-hint') }}
          </p>
          <div
            ref="leftFormWrap"
            class="merge-defect-drawer__form-wrap"
            @focusin.capture="onLeftFormFocusIn"
          >
            <el-form ref="form" :model="form" :rules="rules" label-width="120px" size="small">
              <defect-form-ordered-fields
                v-if="projectId"
                ref="orderedFields"
                :project-id="projectId"
                :form="form"
                :config="config"
                v-model:plan-time-range="planTimeRange"
                :describe-tools="describeTools"
                :defect-field-layout="defectFieldLayout"
                :is-builtin-defect-field-visible="isBuiltinDefectFieldVisible"
                @module-change="moduleChangeHandle"
                @step-change="stepChangeHandle"
              />
            </el-form>
          </div>
        </section>

        <multipane-resizer
          ref="paneResizer"
          class="merge-defect-multipane__resizer"
          :style="mergeResizerStyle"
        />

        <section class="merge-defect-drawer__right" aria-label="source defects">
          <ul class="merge-defect-source-list">
            <li
              v-for="item in sourceList"
              :key="item.defectId"
              class="merge-defect-source-card"
              :class="{ 'is-master': item.defectId === masterDefectId }"
            >
              <div class="merge-defect-source-card__head">
                <span class="merge-defect-source-card__num">#{{ item.projectNum }}</span>
                <defect-state-flag :defect="item" />
                <div class="merge-defect-source-card__actions">
                  <el-button
                    type="primary"
                    link
                    size="small"
                    :icon="$legacyIcon('el-icon-d-arrow-left')"
                    @click="onSelectMaster(item.defectId)"
                  >
                    {{ $t('defect.master-defect') }}
                  </el-button>
                  <el-button
                    class="red"
                    link
                    size="small"
                    :icon="$legacyIcon('el-icon-remove-outline')"
                    :disabled="!canRemoveFromMerge"
                    @click="onRemoveFromMerge(item.defectId)"
                  >
                    {{ $t('defect.remove-from-merge') }}
                  </el-button>
                </div>
              </div>
              <div class="merge-defect-source-card__title" :title="item.defectName">
                {{ item.defectName || '—' }}
              </div>
              <div
                v-if="focusFieldKey"
                class="merge-defect-source-card__field"
                :class="{ 'is-focused-field': true }"
              >
                <span class="merge-defect-source-card__field-label">{{ focusFieldLabel }}</span>
                <span class="merge-defect-source-card__field-value">{{ fieldPreview(item, focusFieldKey) }}</span>
                <el-button
                  type="primary"
                  link
                  size="small"
                  :icon="$legacyIcon('el-icon-arrow-left')"
                  @click="applyFieldFromSource(item.defectId, focusFieldKey)"
                >
                  {{ $t('defect.use-this-field') }}
                </el-button>
              </div>
            </li>
          </ul>
        </section>
      </multipane>
    </div>
  </el-drawer>
</template>

<script>
import { Multipane, MultipaneResizer } from 'vue-multipane'
import { configDefect, getDefect, mergeDefects } from '@/api/system/defect'
import DefectFormOrderedFields from '@/components/Defect/DefectFormOrderedFields'
import DefectStateFlag from '@/components/Defect/DefectStateFlag'
import {
  buildRequiredCustomFieldFormRules,
  normalizeFieldRow
} from '@/components/DefectCustomField/format'
import defectBuiltinFieldVisibility from '@/mixins/defect-builtin-field-visibility'
import { normalizeDefectTypeAndLevel } from '@/utils/defect-defaults'
import { serializeDefectFormCloseState } from '@/utils/defect-form-close-state'
import {
  applyMergeFieldToForm,
  createEmptyMergeForm,
  defectToMergeForm,
  planTimeRangeFromDefect,
  resolveFocusFieldKeyFromOrderedField,
  shouldConfirmMergeMasterOverwrite
} from '@/utils/defect-merge-form'
import { computeResizerHandleTopPx } from '@/utils/multipaneResizerViewport'

const MERGE_LEFT_PANE_DEFAULT_WIDTH = '62%'

export default {
  name: 'MergeDefectDrawer',
  dicts: ['defect_level'],
  mixins: [defectBuiltinFieldVisibility],
  components: { DefectFormOrderedFields, DefectStateFlag, Multipane, MultipaneResizer },
  props: {
    projectId: {
      type: Number,
      default: null
    }
  },
  data() {
    return {
      visible: false,
      loadingSources: false,
      submitting: false,
      describeTools: [],
      config: {},
      planTimeRange: [],
      form: createEmptyMergeForm(),
      rules: {},
      masterDefectId: null,
      sourceDefectIds: [],
      sourceList: [],
      sourceById: {},
      focusFieldKey: null,
      emptyFormBaseline: null,
      leftPaneWidth: MERGE_LEFT_PANE_DEFAULT_WIDTH,
      mergeResizerStyle: { '--resizerHandleTop': '50%' },
      _mergeResizerRaf: null,
      _onMergeResizerViewport: null
    }
  },
  computed: {
    leftPaneStyle() {
      return {
        width: this.leftPaneWidth,
        minWidth: '320px',
        maxWidth: '80%'
      }
    },
    canSubmit() {
      return this.masterDefectId != null && this.sourceDefectIds.length >= 2
    },
    /** 至少保留 1 条，避免右侧空列表；剩 1 条时顶部合并本就不能点 */
    canRemoveFromMerge() {
      return this.sourceList.length > 1
    },
    focusFieldLabel() {
      return this.fieldLabelForKey(this.focusFieldKey)
    }
  },
  watch: {
    defectFieldLayout() {
      if (!this.visible || this.masterDefectId != null) return
      this.$nextTick(() => this.resyncEmptyFormBaseline())
    },
    visible(val) {
      if (val) {
        this.$nextTick(() => {
          this.scheduleSyncMergeResizerHandle()
          this.bindMergeResizerViewport()
        })
      } else {
        this.unbindMergeResizerViewport()
      }
    }
  },
  beforeUnmount() {
    this.unbindMergeResizerViewport()
  },
  methods: {
    bindMergeResizerViewport() {
      this.unbindMergeResizerViewport()
      this._onMergeResizerViewport = () => this.scheduleSyncMergeResizerHandle()
      window.addEventListener('resize', this._onMergeResizerViewport)
      window.addEventListener('scroll', this._onMergeResizerViewport, true)
    },
    unbindMergeResizerViewport() {
      if (this._onMergeResizerViewport) {
        window.removeEventListener('resize', this._onMergeResizerViewport)
        window.removeEventListener('scroll', this._onMergeResizerViewport, true)
        this._onMergeResizerViewport = null
      }
      if (this._mergeResizerRaf != null) {
        cancelAnimationFrame(this._mergeResizerRaf)
        this._mergeResizerRaf = null
      }
    },
    scheduleSyncMergeResizerHandle() {
      if (this._mergeResizerRaf != null) return
      this._mergeResizerRaf = requestAnimationFrame(() => {
        this._mergeResizerRaf = null
        this.syncMergeResizerHandleTop()
      })
    },
    syncMergeResizerHandleTop() {
      if (!this.visible) return
      const comp = this.$refs.paneResizer
      const el = comp && (comp.$el || comp)
      if (!el || typeof el.getBoundingClientRect !== 'function') return
      const h = el.getBoundingClientRect().height
      const px = computeResizerHandleTopPx(el, h > 1 ? h : 0)
      this.mergeResizerStyle = {
        '--resizerHandleTop': px != null && !Number.isNaN(px) ? `${Math.round(px * 100) / 100}px` : '50%'
      }
    },
    onMergePaneResizeStop(pane) {
      if (pane && pane.style && pane.style.width) {
        this.leftPaneWidth = pane.style.width
      }
      this.scheduleSyncMergeResizerHandle()
    },
    open(defects) {
      const list = (defects || []).filter((d) => d && d.defectId != null)
      if (list.length < 2) return
      this.resetState()
      this.sourceDefectIds = list.map((d) => d.defectId)
      this.visible = true
      this.getDefectConfig()
      this.loadSources()
      this.$nextTick(() => this.scheduleSyncMergeResizerHandle())
    },
    resetState() {
      this.masterDefectId = null
      this.focusFieldKey = null
      this.planTimeRange = []
      this.leftPaneWidth = MERGE_LEFT_PANE_DEFAULT_WIDTH
      this.form = createEmptyMergeForm()
      if (this.projectId) {
        this.form.projectId = this.projectId
      }
      this.applyDefectFormRulesForBuiltinLayout()
      this.$nextTick(() => {
        this.resyncEmptyFormBaseline()
        this.$refs.form && this.$refs.form.clearValidate && this.$refs.form.clearValidate()
      })
    },
    /** 在自定义字段默认值写入后再拍基线，避免首次「主缺陷」误弹覆盖确认 */
    resyncEmptyFormBaseline() {
      if (this.masterDefectId != null) return
      const orderedFields = this.$refs.orderedFields
      if (orderedFields && typeof orderedFields.refreshCustomFieldDefaults === 'function') {
        orderedFields.refreshCustomFieldDefaults()
      }
      this.emptyFormBaseline = serializeDefectFormCloseState(this.form, this.planTimeRange)
    },
    getDefectConfig() {
      configDefect().then((res) => {
        this.config = res.data
      })
    },
    async loadSources() {
      this.loadingSources = true
      this.sourceById = {}
      try {
        const results = await Promise.all(
          this.sourceDefectIds.map((id) =>
            getDefect(id).then((res) => res.data).catch(() => null)
          )
        )
        this.sourceList = results.filter(Boolean)
        results.forEach((d) => {
          if (d && d.defectId != null) this.sourceById[d.defectId] = d
        })
      } finally {
        this.loadingSources = false
        this.$nextTick(() => this.scheduleSyncMergeResizerHandle())
      }
    },
    applyDefectFormRulesForBuiltinLayout() {
      const rules = {}
      if (this.isBuiltinDefectFieldVisible('defectName')) {
        /* EP2 el-input 只在 change/blur 上报校验；trigger:'input' 会导致提交报错后改值仍不消红 */
        rules.defectName = [
          {
            required: true,
            message: this.$i18n.t('defect.defect-name-cannot-empty'),
            trigger: ['change', 'blur']
          }
        ]
      }
      const customFields = ((this.defectFieldLayout && this.defectFieldLayout.customFields) || [])
        .filter((f) => f && f.enabled !== 0 && f.enabled !== false)
        .map(normalizeFieldRow)
      Object.assign(
        rules,
        buildRequiredCustomFieldFormRules(customFields, (f) =>
          this.$t('defect.custom-field.value-required', { label: f.fieldLabel || f.fieldKey })
        )
      )
      this.rules = rules
    },
    onLeftFormFocusIn(e) {
      const root = this.$refs.orderedFields && this.$refs.orderedFields.$el
      if (!root || !root.contains(e.target)) return
      const items = root.querySelectorAll('.el-form-item')
      let hitIndex = -1
      items.forEach((el, i) => {
        if (el.contains(e.target)) hitIndex = i
      })
      if (hitIndex < 0) return
      const fields = (this.$refs.orderedFields && this.$refs.orderedFields.orderedFormFields) || []
      const field = fields[hitIndex]
      this.focusFieldKey = resolveFocusFieldKeyFromOrderedField(field)
    },
    fieldLabelForKey(fieldKey) {
      if (!fieldKey) return ''
      if (fieldKey === 'planTime') return this.$t('plan-time').toString()
      if (fieldKey.startsWith('custom:')) {
        const key = fieldKey.slice(7)
        const meta = ((this.defectFieldLayout && this.defectFieldLayout.customFields) || []).find(
          (f) => f && f.fieldKey === key
        )
        return (meta && meta.fieldLabel) || key
      }
      const map = {
        defectName: 'defect.name',
        defectType: 'type',
        defectLevel: 'priority',
        handleBy: 'handle-by',
        moduleVersion: 'version',
        moduleId: 'module',
        caseId: 'case',
        defectDescribe: 'describe',
        imgUrls: 'image',
        annexUrls: 'annex'
      }
      const i18nKey = map[fieldKey]
      return i18nKey ? this.$t(i18nKey).toString() : fieldKey
    },
    fieldPreview(defect, fieldKey) {
      if (!defect || !fieldKey) return '—'
      if (fieldKey === 'planTime') {
        const r = planTimeRangeFromDefect(defect)
        return r.length ? r.join(' ~ ') : '—'
      }
      if (fieldKey.startsWith('custom:')) {
        const key = fieldKey.slice(7)
        const v = defect.customFields && defect.customFields[key]
        if (v == null || v === '') return '—'
        if (Array.isArray(v)) return v.length ? v.join(', ') : '—'
        return String(v)
      }
      const v = defect[fieldKey]
      if (fieldKey === 'handleBy') {
        if (!Array.isArray(v) || !v.length) return '—'
        return this.$t('defect.merge-handle-by-count', { count: v.length }).toString()
      }
      if (fieldKey === 'defectDescribe') {
        if (!v) return '—'
        const plain = String(v).replace(/\s+/g, ' ').trim()
        return plain.length > 80 ? `${plain.slice(0, 80)}…` : plain
      }
      if (v == null || v === '') return '—'
      if (Array.isArray(v)) return v.length ? `${v.length}` : '—'
      return String(v)
    },
    async onSelectMaster(defectId) {
      const source = this.sourceById[defectId]
      if (!source) return
      const needConfirm = shouldConfirmMergeMasterOverwrite(
        this.masterDefectId,
        this.form,
        this.planTimeRange,
        this.emptyFormBaseline,
        serializeDefectFormCloseState
      )
      if (needConfirm) {
        try {
          await this.$modal.confirm(
            this.$t('defect.merge-overwrite-confirm').toString(),
            this.$t('prompted').toString(),
            {
              confirmButtonText: this.$t('ok').toString(),
              cancelButtonText: this.$t('cancel').toString(),
              type: 'warning'
            }
          )
        } catch {
          return
        }
      }
      this.applyMasterFromSource(source)
    },
    onRemoveFromMerge(defectId) {
      if (!this.canRemoveFromMerge || defectId == null) return
      const id = Number(defectId)
      this.sourceList = (this.sourceList || []).filter((d) => Number(d.defectId) !== id)
      this.sourceDefectIds = (this.sourceDefectIds || []).filter((x) => Number(x) !== id)
      if (this.sourceById && this.sourceById[defectId] != null) {
        const next = { ...this.sourceById }
        delete next[defectId]
        delete next[id]
        this.sourceById = next
      }
      if (this.masterDefectId != null && Number(this.masterDefectId) === id) {
        this.masterDefectId = null
      }
    },
    applyMasterFromSource(source) {
      this.form = defectToMergeForm(source)
      if (this.projectId) {
        this.form.projectId = this.projectId
      }
      this.masterDefectId = source.defectId
      this.planTimeRange = planTimeRangeFromDefect(source)
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
        /* 整表替换不会走 el-input change，需清掉提交留下的红字 */
        this.$refs.form && this.$refs.form.clearValidate && this.$refs.form.clearValidate()
      })
    },
    /** 右侧「使用此字段」对应的 el-form prop */
    formPropForFocusFieldKey(fieldKey) {
      if (!fieldKey || fieldKey === 'planTime') return null
      if (fieldKey.startsWith('custom:')) return `customFields.${fieldKey.slice(7)}`
      return fieldKey
    },
    applyFieldFromSource(defectId, fieldKey) {
      const source = this.sourceById[defectId]
      if (!source || !fieldKey) return
      applyMergeFieldToForm(this.form, this.planTimeRange, fieldKey, source)
      if (fieldKey === 'moduleId') {
        this.moduleChangeHandle()
      }
      const prop = this.formPropForFocusFieldKey(fieldKey)
      if (!prop) return
      this.$nextTick(() => {
        const formRef = this.$refs.form
        if (!formRef) return
        /* 程序化赋值后按现值重校，有内容则消红，仍空则保留必填提示 */
        if (typeof formRef.validateField === 'function') {
          formRef.validateField(prop, () => {})
        } else if (typeof formRef.clearValidate === 'function') {
          formRef.clearValidate(prop)
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
    submitMerge() {
      if (!this.canSubmit || this.submitting) return
      this.$refs.form.validate((valid) => {
        if (!valid) return
        normalizeDefectTypeAndLevel(this.form)
        if (this.planTimeRange.length > 1) {
          this.form.planStartTime = this.planTimeRange[0]
          this.form.planEndTime = this.planTimeRange[1]
        } else {
          this.form.planStartTime = null
          this.form.planEndTime = null
        }
        this.form.defectId = this.masterDefectId
        this.submitting = true
        mergeDefects({
          projectId: this.projectId,
          masterDefectId: this.masterDefectId,
          sourceDefectIds: [...this.sourceDefectIds],
          defect: this.form
        })
          .then(() => {
            this.$modal.msgSuccess(this.$t('defect.merge-success').toString())
            this.doClose()
            this.$emit('merged')
          })
          .catch((e) => {
            const msg = (e && e.message) || (e && e.msg) || this.$t('fail').toString()
            this.$modal.msgError(msg)
          })
          .finally(() => {
            this.submitting = false
          })
      })
    },
    requestClose() {
      this.doClose()
    },
    closeDrawer(done) {
      this.doClose()
      done && done()
    },
    doClose() {
      this.visible = false
      this.resetState()
      this.sourceList = []
      this.sourceDefectIds = []
      this.sourceById = {}
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/multipane-resizer-grip.scss";

:deep(.el-drawer__header) {
  margin-bottom: 0;
}
:deep(.el-drawer__close-btn) {
  display: none;
}
/* 铺满抽屉剩余高度，避免 calc(100vh-120px) 偏矮在底部留空缝 */
:deep(.el-drawer__body) {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
  padding-top: 10px;
  padding-bottom: 0;
}

.merge-defect-drawer__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.merge-defect-drawer__title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 500;
    color: var(--text-color-primary);
  }
  .el-icon-arrow-left {
    font-size: 22px;
    cursor: pointer;
    color: var(--text-color-secondary);
  }
}
.merge-defect-drawer__body {
  flex: 1 1 auto;
  min-height: 0;
  min-width: 0;
  max-width: 100%;
  height: 100%;
  overflow: hidden;
}
.merge-defect-multipane.custom-resizer {
  display: flex;
  flex-direction: row;
  align-items: stretch;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: transparent;
}
.merge-defect-multipane > .multipane-resizer,
.merge-defect-multipane__resizer {
  margin: 0;
  left: 0;
  position: relative;
  width: 12px;
  min-width: 12px;
  flex: 0 0 12px;
  cursor: col-resize;
  z-index: 2;
  /* 覆盖 grip mixin 的 height:auto，保证与抽屉可视区同高，手柄才能垂直居中 */
  align-self: stretch !important;
  height: 100% !important;
  min-height: 100% !important;
  /* 与弹框同色：勿铺 border-color 灰条（暗色下会整条发灰） */
  background: transparent !important;
  background-image: none !important;
  @include multipane-resizer-vertical-appearance;
  &:after {
    top: var(--resizerHandleTop, 50%) !important;
    transform: translate(-50%, -50%);
  }
}
.merge-defect-drawer__left {
  min-width: 0;
  align-self: stretch;
  height: auto;
  max-height: 100%;
  overflow: auto;
  overflow-x: hidden;
  padding: 0 8px 10px 0;
  box-sizing: border-box;
  background: transparent;
}
.merge-defect-drawer__form-wrap {
  min-width: 0;
  max-width: 100%;
  overflow-x: hidden;

  :deep(.el-form-item__content) {
    min-width: 0;
    max-width: 100%;
  }

  /* 分栏比单栏窄：外层限宽防撑破；卡片本身保持 148px，勿 width:100%，
   * 否则预览独占一行、+ / 剪贴板被挤到下一行（看起来像排列错乱）。 */
  :deep(.component-upload-image),
  :deep(.upload-file) {
    display: block;
    width: 100%;
    max-width: 100%;
  }
  :deep(.component-upload-image-focus-target),
  :deep(.flex-row) {
    max-width: 100% !important;
    box-sizing: border-box;
  }
  :deep(.cat2bug-upload .el-upload-list--picture-card) {
    width: auto !important;
    max-width: 100% !important;
    flex-wrap: wrap !important;
  }
  /* 触发器（+ / 剪贴板）保持内容宽度，与已上传卡片横排；窄栏不够时由 list wrap */
  :deep(.cat2bug-upload .el-upload-list--picture-card > .el-upload--picture-card),
  :deep(.cat2bug-upload > .el-upload--picture-card) {
    width: max-content !important;
    max-width: 100% !important;
    flex-wrap: nowrap !important;
  }
}
.merge-defect-drawer__right {
  flex: 1;
  min-width: 240px;
  align-self: stretch;
  height: auto;
  max-height: 100%;
  overflow: auto;
  padding: 0 0 10px 4px;
  box-sizing: border-box;
  background: transparent;
}
.merge-defect-drawer__hint {
  margin: 0 0 12px;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-color-secondary);
  background: var(--bg-color-block);
  border: 1px solid var(--border-color-light);
  border-radius: 4px;
}
.merge-defect-source-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.merge-defect-source-card {
  padding: 12px;
  background: var(--bg-color-block);
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  color: var(--text-color-primary);
}
.merge-defect-source-card.is-master {
  border-color: var(--cat2bug-primary, var(--el-color-primary));
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--cat2bug-primary, var(--el-color-primary)) 25%, transparent);
}
.merge-defect-source-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}
.merge-defect-source-card__num {
  font-weight: 600;
  color: var(--text-color-primary);
}
.merge-defect-source-card__actions {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;

  /* 与缺陷工具条删除一致：link + .red 统一危险色 */
  :deep(.el-button.is-link.red:not(.is-disabled)) {
    color: var(--cat2bug-operate-text-danger, #f56c6c);
  }
  :deep(.el-button.is-link.red:not(.is-disabled):hover),
  :deep(.el-button.is-link.red:not(.is-disabled):focus) {
    color: var(--cat2bug-operate-text-danger-hover, #f78989);
  }
}
.merge-defect-source-card__title {
  font-size: 14px;
  line-height: 1.4;
  color: var(--text-color-regular);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 8px;
}
.merge-defect-source-card__field {
  margin-top: 8px;
  padding: 8px;
  border-radius: 4px;
  background: var(--bg-color-base);
  border: 1px solid var(--border-color-lighter);
  font-size: 13px;
}
.merge-defect-source-card__field.is-focused-field {
  border-color: var(--cat2bug-primary, var(--el-color-primary));
}
.merge-defect-source-card__field-label {
  display: block;
  font-weight: 500;
  color: var(--text-color-secondary);
  margin-bottom: 4px;
}
.merge-defect-source-card__field-value {
  display: block;
  color: var(--text-color-primary);
  margin-bottom: 6px;
  word-break: break-word;
}
</style>
