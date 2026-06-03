<template>
  <el-form-item
    :label="field.fieldLabel"
    label-width="120px"
    :class="{ 'custom-field-filter-row--boolean': field.fieldType === 'boolean' }"
  >
    <div class="custom-field-filter-controls">
      <template v-if="field.fieldType === 'boolean'">
        <el-switch
          :value="booleanSwitchValue"
          @input="onBooleanInput"
        />
      </template>
      <template v-else>
        <el-select
          v-model="localOp"
          size="small"
          class="custom-field-filter-op"
          :placeholder="$t('defect.custom-field-filter-op')"
          clearable
          @change="onOpChange"
        >
          <el-option
            v-for="op in opsForField(field)"
            :key="op.value"
            :label="$t(op.labelKey)"
            :value="op.value"
          />
        </el-select>
        <template v-if="needsValue(localOp)">
          <el-input
            v-if="isTextOp(field, localOp)"
            v-model="localValue"
            size="small"
            clearable
            class="custom-field-filter-value"
            :placeholder="field.fieldLabel"
            @input="emitChange"
          />
          <el-input-number
            v-else-if="field.fieldType === 'number' && localOp === 'eq'"
            v-model="localValue"
            size="small"
            class="custom-field-filter-value"
            controls-position="right"
            @input="emitChange"
          />
          <template v-else-if="field.fieldType === 'number' && localOp === 'between'">
            <el-input-number
              v-model="betweenValue[0]"
              size="small"
              class="custom-field-filter-between"
              controls-position="right"
              @input="emitBetween"
            />
            <span class="custom-field-filter-sep">~</span>
            <el-input-number
              v-model="betweenValue[1]"
              size="small"
              class="custom-field-filter-between"
              controls-position="right"
              @input="emitBetween"
            />
          </template>
          <el-select
            v-else-if="field.fieldType === 'enum' && localOp === 'in'"
            v-model="localValue"
            size="small"
            class="custom-field-filter-value defect-enum-select"
            multiple
            collapse-tags
            clearable
            @change="emitChange"
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
            v-model="localValue"
            size="small"
            class="custom-field-filter-value defect-enum-select"
            :style="enumSelectCssVars(field, localValue)"
            clearable
            @change="emitChange"
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
            v-else-if="field.fieldType === 'datetime' && localOp === 'between'"
            v-model="localValue"
            type="datetimerange"
            size="small"
            class="custom-field-filter-value custom-field-filter-daterange"
            value-format="yyyy-MM-dd HH:mm:ss"
            :start-placeholder="$t('start-time')"
            :end-placeholder="$t('end-time')"
            @change="emitChange"
          />
        </template>
      </template>
    </div>
  </el-form-item>
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
  name: 'CustomFieldFilterRow',
  props: {
    field: { type: Object, required: true },
    filter: { type: Object, default: null }
  },
  data() {
    return {
      localOp: '',
      localValue: null,
      betweenValue: [null, null],
      suppressEmit: false
    }
  },
  computed: {
    hasBooleanFilter() {
      return !!(this.filter && this.filter.op === 'eq')
    },
    booleanSwitchValue() {
      if (!this.hasBooleanFilter) return false
      return !!this.filter.value
    }
  },
  watch: {
    filter: {
      immediate: true,
      deep: true,
      handler(entry) {
        this.syncFromFilter(entry)
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
    syncFromFilter(entry) {
      this.suppressEmit = true
      if (!entry || !entry.op) {
        this.localOp = ''
        this.localValue = null
        this.betweenValue = [null, null]
      } else if (entry.op === 'between') {
        this.localOp = 'between'
        const arr = Array.isArray(entry.value) ? entry.value : []
        if (this.field.fieldType === 'datetime') {
          this.localValue = arr
        } else {
          this.betweenValue = [arr[0] != null ? arr[0] : null, arr[1] != null ? arr[1] : null]
          this.localValue = this.betweenValue
        }
      } else if (entry.op === 'in') {
        this.localOp = 'in'
        this.localValue = Array.isArray(entry.value) ? entry.value : []
      } else {
        this.localOp = entry.op
        this.localValue = entry.value
      }
      this.$nextTick(() => {
        this.suppressEmit = false
      })
    },
    onOpChange() {
      this.localValue = defaultValueForOp(this.field.fieldType, this.localOp)
      if (this.localOp === 'between' && this.field.fieldType !== 'datetime') {
        this.betweenValue = [null, null]
        this.localValue = this.betweenValue
      }
      this.emitChange()
    },
    emitBetween() {
      this.localValue = [...this.betweenValue]
      this.emitChange()
    },
    emitChange() {
      if (this.suppressEmit) return
      if (!this.localOp) {
        this.$emit('change', null)
        return
      }
      const entry = { fieldKey: this.field.fieldKey, op: this.localOp }
      if (!NO_VALUE_OPS.has(this.localOp)) {
        if (this.localOp === 'between') {
          const arr = Array.isArray(this.localValue) ? this.localValue : []
          if (arr.length < 2 || arr[0] == null || arr[1] == null || arr[0] === '' || arr[1] === '') {
            this.$emit('change', null)
            return
          }
          entry.value = arr
        } else if (this.localOp === 'in') {
          if (!Array.isArray(this.localValue) || !this.localValue.length) {
            this.$emit('change', null)
            return
          }
          entry.value = this.localValue
        } else if (this.localValue === '' || this.localValue == null) {
          this.$emit('change', null)
          return
        } else {
          entry.value = this.localValue
        }
      }
      this.$emit('change', entry)
    },
    onBooleanInput(val) {
      this.$emit('change', {
        fieldKey: this.field.fieldKey,
        op: 'eq',
        value: !!val
      })
    }
  }
}
</script>

<style scoped>
.custom-field-filter-row--boolean ::v-deep .el-form-item__label {
  line-height: 32px;
}

.custom-field-filter-row--boolean ::v-deep .el-form-item__content {
  display: flex;
  align-items: center;
  min-height: 32px;
  line-height: 32px;
}

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
