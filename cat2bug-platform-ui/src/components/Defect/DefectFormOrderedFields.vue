<template>
  <div class="defect-form-ordered-fields">
    <template v-for="item in orderedFormFields">
      <el-form-item
        v-if="item.kind === 'builtin' && item.formKey === 'defectName'"
        :key="'builtin-' + item.formKey"
        :label="$t('defect.name')"
        prop="defectName"
      >
        <el-input
          ref="defectNameInput"
          v-model="form.defectName"
          :placeholder="$t('defect.enter-name')"
          maxlength="128"
        />
      </el-form-item>

      <el-form-item
        v-else-if="item.kind === 'builtin' && item.formKey === 'defectType'"
        :key="'builtin-' + item.formKey"
        :label="$t('type')"
        prop="defectType"
      >
        <el-select v-model="form.defectType" clearable :placeholder="$t('defect.select-type')">
          <el-option
            v-for="type in config.types"
            :key="type.value || type.key"
            :label="$t(type.value)"
            :value="type.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item
        v-else-if="item.kind === 'builtin' && item.formKey === 'defectLevel'"
        :key="'builtin-' + item.formKey"
        :label="$t('priority')"
        prop="defectLevel"
      >
        <el-select v-model="form.defectLevel" clearable :placeholder="$t('defect.select-level')">
          <el-option
            v-for="dict in dict.type.defect_level"
            :key="dict.value"
            :label="$t(dict.value) ? $t(dict.value) : dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item
        v-else-if="item.kind === 'builtin' && item.formKey === 'handleBy'"
        :key="'builtin-' + item.formKey"
        :label="$t('handle-by')"
        prop="handleBy"
      >
        <select-project-member v-model="form.handleBy" :project-id="projectId" />
      </el-form-item>

      <el-form-item
        v-else-if="item.kind === 'builtin' && item.formKey === 'moduleVersion'"
        :key="'builtin-' + item.formKey"
        :label="$t('version')"
        prop="moduleVersion"
      >
        <el-input
          v-model="form.moduleVersion"
          :placeholder="$t('defect.enter-version')"
          maxlength="128"
          style="max-width: 300px;"
        />
      </el-form-item>

      <el-form-item
        v-else-if="item.kind === 'builtin' && item.formKey === 'moduleId'"
        :key="'builtin-' + item.formKey"
        :label="$t('module')"
        prop="moduleId"
      >
        <select-module
          ref="selectModule"
          v-model="form.moduleId"
          :project-id="projectId"
          @input="onModuleChange"
        />
      </el-form-item>

      <el-form-item
        v-else-if="item.kind === 'builtin' && item.formKey === 'planTime'"
        :key="'builtin-' + item.formKey"
        :label="$t('plan-time')"
        prop="planEndTime"
      >
        <el-date-picker
          class="defect-plan-datetimerange"
          :value="planTimeRange"
          type="datetimerange"
          :range-separator="$t('time-to')"
          :start-placeholder="$t('plan-start-time')"
          :end-placeholder="$t('plan-end-time')"
          value-format="yyyy-MM-dd HH:mm:ss"
          :placeholder="$t('defect.please-select-end-time')"
          @input="$emit('update:planTimeRange', $event)"
        />
      </el-form-item>

      <el-form-item
        v-else-if="item.kind === 'builtin' && item.formKey === 'caseId'"
        :key="'builtin-' + item.formKey"
        :label="$t('case')"
        prop="caseId"
      >
        <select-case
          ref="selectCase"
          v-model="form.caseId"
          :module-id="form.moduleId"
          :step-index="form.caseStepId"
          @step-change="$emit('step-change', $event)"
        />
      </el-form-item>

      <el-form-item
        v-else-if="item.kind === 'builtin' && item.formKey === 'defectDescribe'"
        :key="'builtin-' + item.formKey"
        :label="$t('describe')"
        prop="defectDescribe"
      >
        <cat2-bug-textarea
          ref="cat2bugTextarea"
          :name="$t('describe').toString()"
          :placeholder="$t('defect.enter-markdown-describe').toString()"
          :tools="describeTools"
          v-model="form.defectDescribe"
          maxlength="65536"
          rows="8"
          show-word-limit
          show-tools
        >
          <template v-slot:tools>
            <slot name="describe-tools" />
          </template>
        </cat2-bug-textarea>
      </el-form-item>

      <el-form-item
        v-else-if="item.kind === 'builtin' && item.formKey === 'imgUrls'"
        :key="'builtin-' + item.formKey"
        :label="$t('image')"
        prop="imgUrls"
      >
        <image-upload v-model="form.imgUrls" :limit="9" />
      </el-form-item>

      <el-form-item
        v-else-if="item.kind === 'builtin' && item.formKey === 'annexUrls'"
        :key="'builtin-' + item.formKey"
        :label="$t('annex')"
        prop="annexUrls"
      >
        <file-upload v-model="form.annexUrls" :limit="9" :file-type="[]" />
      </el-form-item>

      <defect-custom-field-form-item
        v-else-if="item.kind === 'custom'"
        :key="'custom-' + item.fieldKey"
        :field="item.meta"
        :value="customFieldValue(item.fieldKey)"
        @input="setCustomFieldValue(item.fieldKey, $event)"
      />
    </template>
  </div>
</template>

<script>
import SelectProjectMember from '@/components/Project/SelectProjectMember'
import SelectModule from '@/components/Module/SelectModule'
import SelectCase from '@/components/Case/SelectCase'
import ImageUpload from '@/components/ImageUpload'
import FileUpload from '@/components/FileUpload'
import Cat2BugTextarea from '@/components/Cat2BugTextarea'
import DefectCustomFieldFormItem from '@/components/DefectCustomField/DefectCustomFieldFormItem'
import {
  cloneDefaultFieldValue,
  normalizeCustomFieldsForSubmit,
  supportsFieldDefaultValue
} from '@/components/DefectCustomField/format'
import { resolveOrderedDefectFormFields } from '@/utils/defect-form-field-order'

export default {
  name: 'DefectFormOrderedFields',
  dicts: ['defect_level'],
  components: {
    SelectProjectMember,
    SelectModule,
    SelectCase,
    ImageUpload,
    FileUpload,
    Cat2BugTextarea,
    DefectCustomFieldFormItem
  },
  props: {
    projectId: { type: Number, default: null },
    form: { type: Object, required: true },
    config: { type: Object, default: () => ({}) },
    planTimeRange: { type: Array, default: () => [] },
    describeTools: { type: Array, default: () => [] },
    defectFieldLayout: { type: Object, default: null },
    isBuiltinDefectFieldVisible: { type: Function, required: true }
  },
  data() {
    return {
      orderedFormFields: []
    }
  },
  watch: {
    defectFieldLayout: {
      deep: true,
      handler(val) {
        this.rebuildOrderedFormFields()
        if (val) {
          this.$nextTick(() => this.applyCustomFieldDefaults(this.orderedFormFields))
        }
      }
    },
    projectId() {
      this.rebuildOrderedFormFields()
    }
  },
  created() {
    this.rebuildOrderedFormFields()
  },
  methods: {
    rebuildOrderedFormFields() {
      this.orderedFormFields = resolveOrderedDefectFormFields(
        this.$cache && this.$cache.local,
        this.defectFieldLayout,
        { isBuiltinVisible: this.isBuiltinDefectFieldVisible }
      )
    },
    /** 表单重置后补全自定义字段默认值（由 AddDefect / EditDefectDialog 在 open 时调用） */
    refreshCustomFieldDefaults() {
      this.applyCustomFieldDefaults(this.orderedFormFields)
    },
    focusDefectName() {
      const ref = this.$refs.defectNameInput
      const input = Array.isArray(ref) ? ref[0] : ref
      input && input.focus && input.focus()
    },
    getSelectCaseRef() {
      const ref = this.$refs.selectCase
      return Array.isArray(ref) ? ref[0] : ref
    },
    getSelectModuleRef() {
      const ref = this.$refs.selectModule
      return Array.isArray(ref) ? ref[0] : ref
    },
    onModuleChange() {
      this.$emit('module-change')
    },
    customFieldValue(fieldKey) {
      const cf = this.form.customFields || {}
      return cf[fieldKey]
    },
    setCustomFieldValue(fieldKey, value) {
      if (!this.form.customFields) {
        this.$set(this.form, 'customFields', {})
      }
      const meta = (this.orderedFormFields || []).find(
        f => f.kind === 'custom' && f.fieldKey === fieldKey
      )
      const nextValue =
        meta && meta.meta && meta.meta.fieldType === 'enum'
          ? (value == null || value === '' ? value : String(value))
          : value
      this.$set(this.form.customFields, fieldKey, nextValue)
      this.syncNormalizedCustomFields()
    },
    applyCustomFieldDefaults(fields) {
      if (!this.form) return
      if (!this.form.customFields) {
        this.$set(this.form, 'customFields', {})
      }
      const customFields = (fields || []).filter(f => f.kind === 'custom').map(f => f.meta)
      customFields.forEach(f => {
        const key = f.fieldKey
        if (this.form.customFields[key] !== undefined) return
        if (!supportsFieldDefaultValue(f.fieldType)) return
        const fromDefault = cloneDefaultFieldValue(f, f.defaultValue)
        if (fromDefault !== undefined) {
          this.$set(this.form.customFields, key, fromDefault)
          return
        }
        if (f.fieldType === 'boolean') {
          this.$set(this.form.customFields, key, false)
        } else if (f.fieldType === 'array') {
          this.$set(this.form.customFields, key, [])
        } else if (f.fieldType === 'image' || f.fieldType === 'file') {
          this.$set(this.form.customFields, key, '')
        } else {
          this.$set(this.form.customFields, key, null)
        }
      })
    },
    syncNormalizedCustomFields() {
      const customFields = (this.orderedFormFields || [])
        .filter(f => f.kind === 'custom')
        .map(f => f.meta)
      if (!customFields.length || !this.form.customFields) return
      const normalized = normalizeCustomFieldsForSubmit(this.form.customFields, customFields)
      Object.keys(normalized).forEach(key => {
        if (this.form.customFields[key] !== normalized[key]) {
          this.$set(this.form.customFields, key, normalized[key])
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.defect-form-ordered-fields ::v-deep .defect-plan-datetimerange.el-date-editor--datetimerange.el-input__inner {
  width: 100% !important;
  max-width: 100%;
  min-width: 0 !important;
  box-sizing: border-box;
}
</style>
