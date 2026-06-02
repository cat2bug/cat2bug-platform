<template>
  <div v-if="fieldList.length" class="defect-custom-fields-section">
    <el-form-item
      v-for="field in fieldList"
      :key="field.fieldKey"
      :label="field.fieldLabel"
      :required="field.required"
    >
      <el-input
        v-if="field.fieldType === 'string'"
        v-model="localValues[field.fieldKey]"
        :maxlength="field.maxLength || 512"
        clearable
      />
      <el-input-number
        v-else-if="field.fieldType === 'number'"
        v-model="localValues[field.fieldKey]"
        controls-position="right"
        class="defect-custom-field-number"
      />
      <el-switch
        v-else-if="field.fieldType === 'boolean'"
        v-model="localValues[field.fieldKey]"
      />
      <el-date-picker
        v-else-if="field.fieldType === 'datetime'"
        v-model="localValues[field.fieldKey]"
        type="datetime"
        value-format="yyyy-MM-dd HH:mm:ss"
        class="defect-custom-field-datetime"
      />
      <el-select
        v-else-if="field.fieldType === 'enum'"
        v-model="localValues[field.fieldKey]"
        clearable
        class="defect-custom-field-enum defect-enum-select"
        :style="enumSelectCssVars(field, localValues[field.fieldKey])"
      >
        <el-option
          v-for="opt in enumOptions(field)"
          :key="opt.key"
          :label="opt.label || opt.key"
          :value="opt.key"
        >
          <span class="defect-custom-field-enum-dot" :style="{ background: opt.color || '#909399' }" />
          <span :style="enumOptionTextStyle(opt)">{{ opt.label || opt.key }}</span>
        </el-option>
      </el-select>
      <el-input
        v-else-if="field.fieldType === 'object'"
        v-model="objectText[field.fieldKey]"
        type="textarea"
        :rows="4"
        :placeholder="$t('defect.custom-field.object-placeholder')"
        @blur="syncObject(field.fieldKey)"
      />
      <image-upload
        v-else-if="field.fieldType === 'image'"
        v-model="localValues[field.fieldKey]"
        :limit="imageLimit(field)"
      />
      <file-upload
        v-else-if="field.fieldType === 'file'"
        v-model="localValues[field.fieldKey]"
        :limit="fileLimit(field)"
        :file-type="[]"
      />
      <div v-else-if="field.fieldType === 'array'" class="defect-custom-field-array">
        <div v-for="(item, idx) in arrayValues(field.fieldKey)" :key="idx" class="defect-custom-field-array-row">
          <el-input
            v-if="arrayItemType(field) === 'string'"
            v-model="arrayValues(field.fieldKey)[idx]"
            size="small"
          />
          <el-input-number
            v-else-if="arrayItemType(field) === 'number'"
            v-model="arrayValues(field.fieldKey)[idx]"
            size="small"
            controls-position="right"
          />
          <el-switch
            v-else-if="arrayItemType(field) === 'boolean'"
            v-model="arrayValues(field.fieldKey)[idx]"
          />
          <el-date-picker
            v-else-if="arrayItemType(field) === 'datetime'"
            v-model="arrayValues(field.fieldKey)[idx]"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            size="small"
          />
          <el-input
            v-else
            v-model="arrayValues(field.fieldKey)[idx]"
            type="textarea"
            :rows="2"
            size="small"
          />
          <el-button type="text" icon="el-icon-delete" @click="removeArrayItem(field.fieldKey, idx)" />
        </div>
        <el-button type="text" icon="el-icon-plus" @click="addArrayItem(field.fieldKey)">
          {{ $t('defect.custom-field.add-item') }}
        </el-button>
      </div>
    </el-form-item>
  </div>
</template>

<script>
import { listEnabledFields } from '@/api/system/defect-field'
import ImageUpload from '@/components/ImageUpload'
import FileUpload from '@/components/FileUpload'
import {
  cloneDefaultFieldValue,
  enumOptionTextStyle,
  enumOptions,
  enumSelectCssVars,
  normalizeCustomFieldsForSubmit,
  normalizeFieldRow,
  parseTypeConfig,
  supportsFieldDefaultValue,
  urlListFromCustomFieldValue
} from './format'

export default {
  name: 'DefectCustomFieldsSection',
  components: { ImageUpload, FileUpload },
  props: {
    projectId: { type: Number, default: null },
    value: { type: Object, default: () => ({}) }
  },
  data() {
    return {
      fieldList: [],
      localValues: {},
      objectText: {}
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler(id) {
        this.loadFields(id)
      }
    },
    value: {
      deep: true,
      handler(v) {
        this.applyValue(v)
      }
    },
    localValues: {
      deep: true,
      handler(v) {
        this.$emit('input', normalizeCustomFieldsForSubmit(v, this.fieldList))
      }
    }
  },
  methods: {
    enumOptions,
    enumOptionTextStyle,
    enumSelectCssVars,
    parseTypeConfig,
    loadFields(projectId) {
      if (!projectId) {
        this.fieldList = []
        return
      }
      listEnabledFields(projectId).then(res => {
        this.fieldList = (res.data || []).map(normalizeFieldRow)
        this.initDefaults()
        this.applyValue(this.value)
      })
    },
    initDefaults() {
      const src = this.value || {}
      this.fieldList.forEach(f => {
        const key = f.fieldKey
        if (src[key] !== undefined) {
          return
        }
        if (!supportsFieldDefaultValue(f.fieldType)) {
          return
        }
        const fromDefault = cloneDefaultFieldValue(f, f.defaultValue)
        if (fromDefault !== undefined) {
          this.$set(this.localValues, key, fromDefault)
          if (f.fieldType === 'object') {
            this.$set(
              this.objectText,
              key,
              typeof fromDefault === 'string' ? fromDefault : JSON.stringify(fromDefault, null, 2)
            )
          }
          return
        }
        if (this.localValues[key] === undefined) {
          if (f.fieldType === 'boolean') {
            this.$set(this.localValues, key, false)
          } else if (f.fieldType === 'array') {
            this.$set(this.localValues, key, [])
          } else if (f.fieldType === 'image' || f.fieldType === 'file') {
            this.$set(this.localValues, key, '')
          } else {
            this.$set(this.localValues, key, null)
          }
        }
      })
    },
    applyValue(v) {
      const src = v || {}
      this.fieldList.forEach(f => {
        const key = f.fieldKey
        if (f.fieldType === 'object') {
          const val = src[key]
          this.$set(this.objectText, key, val == null ? '' : (typeof val === 'string' ? val : JSON.stringify(val, null, 2)))
          this.$set(this.localValues, key, val)
        } else if (src[key] !== undefined) {
          if (f.fieldType === 'image' || f.fieldType === 'file') {
            const urls = urlListFromCustomFieldValue(src[key])
            this.$set(this.localValues, key, urls.length ? urls.join(',') : '')
          } else {
            this.$set(this.localValues, key, src[key])
          }
        }
      })
      this.initDefaults()
    },
    syncObject(key) {
      const text = (this.objectText[key] || '').trim()
      if (!text) {
        this.$set(this.localValues, key, null)
        return
      }
      try {
        this.$set(this.localValues, key, JSON.parse(text))
      } catch (e) {
        this.$message.warning(this.$t('defect.custom-field.invalid-json'))
      }
    },
    arrayItemType(field) {
      return parseTypeConfig(field).itemType || 'string'
    },
    arrayValues(key) {
      if (!Array.isArray(this.localValues[key])) {
        this.$set(this.localValues, key, [])
      }
      return this.localValues[key]
    },
    addArrayItem(key) {
      const arr = this.arrayValues(key)
      arr.push(null)
    },
    removeArrayItem(key, idx) {
      this.arrayValues(key).splice(idx, 1)
    },
    imageLimit(field) {
      return parseTypeConfig(field).maxCount || 9
    },
    fileLimit(field) {
      return parseTypeConfig(field).maxCount || 9
    }
  }
}
</script>

<style lang="scss" scoped>
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
.defect-custom-field-array-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.defect-custom-field-datetime {
  width: 100%;
}
</style>
