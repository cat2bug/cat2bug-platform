<template>
  <div v-if="fields.length" class="custom-field-filter-builder">
    <el-divider content-position="left">{{ $t('defect.custom-field-filters') }}</el-divider>
    <div v-for="field in fields" :key="field.fieldKey" class="custom-field-filter-row">
      <el-form-item :label="field.fieldLabel" label-width="120px">
        <div class="custom-field-filter-controls">
          <el-select
            v-model="filterState[field.fieldKey].op"
            size="small"
            class="custom-field-filter-op"
            :placeholder="$t('defect.custom-field-filter-op')"
            clearable
            @change="onOpChange(field.fieldKey)"
          >
            <el-option
              v-for="op in opsForField(field)"
              :key="op.value"
              :label="$t(op.labelKey)"
              :value="op.value"
            />
          </el-select>
          <template v-if="needsValue(filterState[field.fieldKey].op)">
            <el-input
              v-if="isTextOp(field, filterState[field.fieldKey].op)"
              v-model="filterState[field.fieldKey].value"
              size="small"
              clearable
              class="custom-field-filter-value"
              :placeholder="field.fieldLabel"
            />
            <el-input-number
              v-else-if="field.fieldType === 'number' && filterState[field.fieldKey].op === 'eq'"
              v-model="filterState[field.fieldKey].value"
              size="small"
              class="custom-field-filter-value"
              controls-position="right"
            />
            <template v-else-if="field.fieldType === 'number' && filterState[field.fieldKey].op === 'between'">
              <el-input-number
                v-model="filterState[field.fieldKey].value[0]"
                size="small"
                class="custom-field-filter-between"
                controls-position="right"
              />
              <span class="custom-field-filter-sep">~</span>
              <el-input-number
                v-model="filterState[field.fieldKey].value[1]"
                size="small"
                class="custom-field-filter-between"
                controls-position="right"
              />
            </template>
            <el-select
              v-else-if="field.fieldType === 'boolean'"
              v-model="filterState[field.fieldKey].value"
              size="small"
              class="custom-field-filter-value"
              clearable
            >
              <el-option :label="$t('defect.custom-field.yes')" :value="true" />
              <el-option :label="$t('defect.custom-field.no')" :value="false" />
            </el-select>
            <el-select
              v-else-if="field.fieldType === 'enum' && filterState[field.fieldKey].op === 'in'"
              v-model="filterState[field.fieldKey].value"
              size="small"
              class="custom-field-filter-value defect-enum-select"
              multiple
              collapse-tags
              clearable
            >
              <el-option
                v-for="opt in enumOptions(field)"
                :key="opt.key"
                :label="opt.label"
                :value="opt.key"
              >
                <span class="defect-custom-field-enum-dot" :style="{ background: opt.color || '#909399' }" />
                <span :style="enumOptionTextStyle(opt)">{{ opt.label || opt.key }}</span>
              </el-option>
            </el-select>
            <el-select
              v-else-if="field.fieldType === 'enum'"
              v-model="filterState[field.fieldKey].value"
              size="small"
              class="custom-field-filter-value defect-enum-select"
              :style="enumSelectCssVars(field, filterState[field.fieldKey].value)"
              clearable
            >
              <el-option
                v-for="opt in enumOptions(field)"
                :key="opt.key"
                :label="opt.label"
                :value="opt.key"
              >
                <span class="defect-custom-field-enum-dot" :style="{ background: opt.color || '#909399' }" />
                <span :style="enumOptionTextStyle(opt)">{{ opt.label || opt.key }}</span>
              </el-option>
            </el-select>
            <el-date-picker
              v-else-if="field.fieldType === 'datetime' && filterState[field.fieldKey].op === 'between'"
              v-model="filterState[field.fieldKey].value"
              type="datetimerange"
              size="small"
              class="custom-field-filter-value custom-field-filter-daterange"
              value-format="yyyy-MM-dd HH:mm:ss"
              :start-placeholder="$t('start-time')"
              :end-placeholder="$t('end-time')"
            />
          </template>
        </div>
      </el-form-item>
    </div>
  </div>
</template>

<script>
import { enumOptionTextStyle, enumOptions as getEnumOptions, enumSelectCssVars } from './format'

const OPS_BY_TYPE = {
  string: [
    { value: 'contains', labelKey: 'defect.custom-field-op-contains' },
    { value: 'eq', labelKey: 'defect.custom-field-op-eq' },
    { value: 'isEmpty', labelKey: 'defect.custom-field-op-isEmpty' },
    { value: 'isNotEmpty', labelKey: 'defect.custom-field-op-isNotEmpty' }
  ],
  number: [
    { value: 'eq', labelKey: 'defect.custom-field-op-eq' },
    { value: 'between', labelKey: 'defect.custom-field-op-between' },
    { value: 'isEmpty', labelKey: 'defect.custom-field-op-isEmpty' },
    { value: 'isNotEmpty', labelKey: 'defect.custom-field-op-isNotEmpty' }
  ],
  boolean: [{ value: 'eq', labelKey: 'defect.custom-field-op-eq' }],
  datetime: [
    { value: 'between', labelKey: 'defect.custom-field-op-between' },
    { value: 'isEmpty', labelKey: 'defect.custom-field-op-isEmpty' },
    { value: 'isNotEmpty', labelKey: 'defect.custom-field-op-isNotEmpty' }
  ],
  enum: [
    { value: 'in', labelKey: 'defect.custom-field-op-in' },
    { value: 'eq', labelKey: 'defect.custom-field-op-eq' },
    { value: 'isEmpty', labelKey: 'defect.custom-field-op-isEmpty' },
    { value: 'isNotEmpty', labelKey: 'defect.custom-field-op-isNotEmpty' }
  ],
  object: [
    { value: 'isEmpty', labelKey: 'defect.custom-field-op-isEmpty' },
    { value: 'isNotEmpty', labelKey: 'defect.custom-field-op-isNotEmpty' }
  ],
  image: [
    { value: 'isEmpty', labelKey: 'defect.custom-field-op-isEmpty' },
    { value: 'isNotEmpty', labelKey: 'defect.custom-field-op-isNotEmpty' }
  ],
  file: [
    { value: 'isEmpty', labelKey: 'defect.custom-field-op-isEmpty' },
    { value: 'isNotEmpty', labelKey: 'defect.custom-field-op-isNotEmpty' }
  ],
  array: [
    { value: 'contains', labelKey: 'defect.custom-field-op-contains' },
    { value: 'isEmpty', labelKey: 'defect.custom-field-op-isEmpty' },
    { value: 'isNotEmpty', labelKey: 'defect.custom-field-op-isNotEmpty' }
  ]
}

const NO_VALUE_OPS = new Set(['isEmpty', 'isNotEmpty', 'hasValue'])

function defaultValueForOp(fieldType, op) {
  if (NO_VALUE_OPS.has(op)) return null
  if (op === 'between') {
    return fieldType === 'datetime' ? [] : [null, null]
  }
  if (op === 'in') return []
  return ''
}

export default {
  name: 'CustomFieldFilterBuilder',
  props: {
    fields: {
      type: Array,
      default: () => []
    },
    value: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      filterState: {}
    }
  },
  watch: {
    fields: {
      immediate: true,
      handler() {
        this.syncFromValue()
      }
    },
    value: {
      deep: true,
      handler() {
        this.syncFromValue()
      }
    },
    filterState: {
      deep: true,
      handler() {
        this.emitFilters()
      }
    }
  },
  methods: {
    opsForField(field) {
      return OPS_BY_TYPE[field.fieldType] || OPS_BY_TYPE.string
    },
    enumOptions(field) {
      return getEnumOptions(field)
    },
    enumOptionTextStyle,
    enumSelectCssVars,
    needsValue(op) {
      return op && !NO_VALUE_OPS.has(op)
    },
    isTextOp(field, op) {
      if (op === 'contains') return true
      if (field.fieldType === 'string' && op === 'eq') return true
      if (field.fieldType === 'array' && op === 'contains') return true
      return false
    },
    onOpChange(fieldKey) {
      const field = this.fields.find(f => f.fieldKey === fieldKey)
      if (!field) return
      const op = this.filterState[fieldKey].op
      this.$set(this.filterState[fieldKey], 'value', defaultValueForOp(field.fieldType, op))
    },
    syncFromValue() {
      const next = {}
      const byKey = {}
      ;(this.value || []).forEach(f => {
        if (f && f.fieldKey) byKey[f.fieldKey] = f
      })
      this.fields.forEach(field => {
        const existing = byKey[field.fieldKey]
        const op = existing && existing.op ? existing.op : ''
        let val = existing ? existing.value : null
        if (op === 'between' && (!Array.isArray(val) || val.length < 2)) {
          val = field.fieldType === 'datetime' ? [] : [null, null]
        }
        if (op === 'in' && !Array.isArray(val)) {
          val = val != null && val !== '' ? [val] : []
        }
        next[field.fieldKey] = { op, value: val }
      })
      this.filterState = next
    },
    emitFilters() {
      const out = []
      this.fields.forEach(field => {
        const st = this.filterState[field.fieldKey]
        if (!st || !st.op) return
        const entry = { fieldKey: field.fieldKey, op: st.op }
        if (!NO_VALUE_OPS.has(st.op)) {
          if (st.op === 'between') {
            const arr = Array.isArray(st.value) ? st.value : []
            if (arr.length < 2 || arr[0] == null || arr[1] == null || arr[0] === '' || arr[1] === '') {
              return
            }
            entry.value = arr
          } else if (st.op === 'in') {
            if (!Array.isArray(st.value) || !st.value.length) return
            entry.value = st.value
          } else if (st.value === '' || st.value == null) {
            return
          } else {
            entry.value = st.value
          }
        }
        out.push(entry)
      })
      this.$emit('input', out)
    }
  }
}
</script>

<style scoped>
.custom-field-filter-controls {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.custom-field-filter-op {
  width: 140px;
}
.custom-field-filter-value {
  flex: 1;
  min-width: 160px;
}
.custom-field-filter-between {
  width: 120px;
}
.custom-field-filter-daterange {
  max-width: 360px;
}
.custom-field-filter-sep {
  color: #909399;
}
.defect-custom-field-enum-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}
.defect-enum-select ::v-deep .el-input__inner {
  color: var(--defect-enum-color, inherit) !important;
}
</style>
