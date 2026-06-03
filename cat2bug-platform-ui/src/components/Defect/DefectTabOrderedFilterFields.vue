<template>
  <div v-if="orderedFilterFields.length" class="defect-tab-ordered-filters">
    <template v-for="item in orderedFilterFields">
      <el-form-item
        v-if="item.kind === 'builtin' && item.formKey === 'defectType'"
        :key="'builtin-' + item.formKey"
        :label="$t('defect.type')"
        prop="defectType"
      >
        <el-select
          v-model="config.defectType"
          clearable
          :placeholder="$t('defect.select-type')"
        >
          <el-option
            v-for="type in defectTypes"
            :key="type.key || type.value"
            :label="$i18n.t(type.value)"
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
        <el-select
          v-model="config.defectLevel"
          clearable
          :placeholder="$t('defect.select-level')"
        >
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
        <select-project-member
          v-model="config.handleBy"
          :project-id="projectId"
          :placeholder="$t('defect.select-handle-by').toString()"
          :is-head="false"
          size="medium"
        />
      </el-form-item>

      <el-form-item
        v-else-if="item.kind === 'builtin' && item.formKey === 'moduleVersion'"
        :key="'builtin-' + item.formKey"
        :label="$t('version')"
        prop="moduleVersion"
      >
        <el-input
          v-model="config.moduleVersion"
          :placeholder="$t('defect.enter-version')"
          maxlength="128"
          clearable
        />
      </el-form-item>

      <el-form-item
        v-else-if="item.kind === 'builtin' && item.formKey === 'moduleId'"
        :key="'builtin-' + item.formKey"
        :label="$t('module')"
        prop="moduleId"
      >
        <select-module
          v-model="config.moduleId"
          :project-id="projectId"
          :is-edit="false"
          size="medium"
        />
      </el-form-item>

      <el-form-item
        v-else-if="item.kind === 'builtin' && item.formKey === 'caseId'"
        :key="'builtin-' + item.formKey"
        :label="$t('case')"
        prop="caseId"
      >
        <select-case
          v-model="config.caseId"
          :module-id="config.moduleId"
        />
      </el-form-item>

      <el-form-item
        v-else-if="item.kind === 'builtin' && item.formKey === 'defectState'"
        :key="'builtin-' + item.formKey"
        :label="$t('defect.state')"
        prop="defectState"
      >
        <el-select
          v-model="defectStates"
          clearable
          multiple
          collapse-tags
          :placeholder="$t('defect.select-state')"
        >
          <el-option
            v-for="state in defectStatesOptions"
            :key="state.key"
            :label="$i18n.t(state.value)"
            :value="state.key"
          />
        </el-select>
      </el-form-item>

      <el-form-item
        v-else-if="item.kind === 'builtin' && item.formKey === 'createMember'"
        :key="'builtin-' + item.formKey"
        :label="$t('create-by')"
        prop="createByIds"
      >
        <select-project-member
          v-model="config.createByIds"
          :project-id="projectId"
          :placeholder="$t('defect.select-create-by').toString()"
          :is-head="false"
          size="medium"
        />
      </el-form-item>

      <custom-field-filter-row
        v-else-if="item.kind === 'custom'"
        :key="'custom-' + item.fieldKey"
        :field="item.meta"
        :filter="customFilterEntry(item.fieldKey)"
        @change="setCustomFilterEntry(item.fieldKey, $event)"
      />
    </template>
  </div>
</template>

<script>
import SelectProjectMember from '@/components/Project/SelectProjectMember'
import SelectModule from '@/components/Module/SelectModule'
import SelectCase from '@/components/Case/SelectCase'
import CustomFieldFilterRow from '@/components/DefectCustomField/CustomFieldFilterRow'
import {
  isBuiltinFormFieldVisible,
  normalizeEnabledBuiltinKeys
} from '@/utils/defect-field-layout'
import { resolveOrderedDefectTabFilterFields } from '@/utils/defect-tab-filter-fields'

export default {
  name: 'DefectTabOrderedFilterFields',
  dicts: ['defect_level'],
  components: { SelectProjectMember, SelectModule, SelectCase, CustomFieldFilterRow },
  props: {
    projectId: { type: Number, default: 0 },
    config: { type: Object, required: true },
    fieldLayout: { type: Object, default: null },
    defectTypes: { type: Array, default: () => [] },
    defectStatesOptions: { type: Array, default: () => [] }
  },
  computed: {
    orderedFilterFields() {
      return resolveOrderedDefectTabFilterFields(
        this.$cache && this.$cache.local,
        this.fieldLayout,
        { isBuiltinVisible: this.isBuiltinFieldVisible }
      )
    },
    defectStates: {
      get() {
        const params = this.config.params || {}
        return params.defectStates || []
      },
      set(val) {
        if (!this.config.params) {
          this.$set(this.config, 'params', {})
        }
        this.$set(this.config.params, 'defectStates', val || [])
      }
    }
  },
  methods: {
    isBuiltinFieldVisible(fieldKey) {
      if (!this.fieldLayout) return true
      return isBuiltinFormFieldVisible(
        fieldKey,
        normalizeEnabledBuiltinKeys(this.fieldLayout.enabledBuiltinFieldKeys)
      )
    },
    customFilterEntry(fieldKey) {
      const list = this.config.customFieldFilters || []
      return list.find(f => f && f.fieldKey === fieldKey) || null
    },
    setCustomFilterEntry(fieldKey, entry) {
      const list = [...(this.config.customFieldFilters || [])]
      const idx = list.findIndex(f => f && f.fieldKey === fieldKey)
      if (!entry) {
        if (idx >= 0) list.splice(idx, 1)
      } else if (idx >= 0) {
        list.splice(idx, 1, entry)
      } else {
        list.push(entry)
      }
      this.$set(this.config, 'customFieldFilters', list)
    }
  }
}
</script>
