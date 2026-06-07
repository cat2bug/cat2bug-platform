<template>
  <el-form-item
    :label="field.fieldLabel"
    :required="field.required"
  >
    <el-input
      v-if="field.fieldType === 'string'"
      v-model="fieldValue"
      :maxlength="field.maxLength || 512"
      clearable
    />
    <el-input-number
      v-else-if="field.fieldType === 'number'"
      v-model="fieldValue"
      controls-position="right"
      class="defect-custom-field-number"
    />
    <el-switch
      v-else-if="field.fieldType === 'boolean'"
      v-model="fieldValue"
    />
    <el-date-picker
      v-else-if="field.fieldType === 'datetime'"
      v-model="fieldValue"
      type="datetime"
      value-format="yyyy-MM-dd HH:mm:ss"
      class="defect-custom-field-datetime"
    />
    <el-select
      v-else-if="field.fieldType === 'enum'"
      v-model="fieldValue"
      clearable
      class="defect-custom-field-enum defect-enum-select"
      :style="enumSelectCssVars(field, fieldValue)"
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
      :value="objectText"
      type="textarea"
      :rows="4"
      :placeholder="$t('defect.custom-field.object-placeholder')"
      @input="onObjectTextInput"
      @blur="syncObject"
    />
    <image-upload
      v-else-if="field.fieldType === 'image'"
      :value="imageFileValue"
      :limit="imageLimit(field)"
      @input="onImageInput"
    />
    <file-upload
      v-else-if="field.fieldType === 'file'"
      :value="imageFileValue"
      :limit="fileLimit(field)"
      :file-type="[]"
      @input="onImageInput"
    />
    <div v-else-if="field.fieldType === 'array'" class="defect-custom-field-array">
      <div v-for="(item, idx) in arrayValues" :key="idx" class="defect-custom-field-array-row">
        <el-input
          v-if="arrayItemType(field) === 'string'"
          :value="arrayValues[idx]"
          size="small"
          @input="updateArrayItem(idx, $event)"
        />
        <el-input-number
          v-else-if="arrayItemType(field) === 'number'"
          :value="arrayValues[idx]"
          size="small"
          controls-position="right"
          @input="updateArrayItem(idx, $event)"
        />
        <el-switch
          v-else-if="arrayItemType(field) === 'boolean'"
          :value="arrayValues[idx]"
          @input="updateArrayItem(idx, $event)"
        />
        <el-date-picker
          v-else-if="arrayItemType(field) === 'datetime'"
          :value="arrayValues[idx]"
          type="datetime"
          value-format="yyyy-MM-dd HH:mm:ss"
          size="small"
          @input="updateArrayItem(idx, $event)"
        />
        <el-input
          v-else
          :value="arrayValues[idx]"
          type="textarea"
          :rows="2"
          size="small"
          @input="updateArrayItem(idx, $event)"
        />
        <el-button type="text" icon="el-icon-delete" @click="removeArrayItem(idx)" />
      </div>
      <el-button type="text" icon="el-icon-plus" @click="addArrayItem">
        {{ $t('defect.custom-field.add-item') }}
      </el-button>
    </div>
  </el-form-item>
</template>

<script>
import ImageUpload from '@/components/ImageUpload'
import FileUpload from '@/components/FileUpload'
import {
  enumOptionTextStyle,
  enumOptions,
  enumSelectCssVars,
  normalizeEnumFieldValue,
  parseTypeConfig,
  urlListFromCustomFieldValue
} from './format'

export default {
  name: 'DefectCustomFieldFormItem',
  components: { ImageUpload, FileUpload },
  props: {
    field: { type: Object, required: true },
    value: { default: null }
  },
  data() {
    return {
      objectText: ''
    }
  },
  computed: {
    fieldValue: {
      get() {
        if (this.field.fieldType === 'enum') {
          return normalizeEnumFieldValue(this.value)
        }
        return this.value
      },
      set(v) {
        if (this.field.fieldType === 'enum') {
          this.$emit('input', normalizeEnumFieldValue(v))
          return
        }
        this.$emit('input', v)
      }
    },
    imageFileValue() {
      const urls = urlListFromCustomFieldValue(this.value)
      return urls.length ? urls.join(',') : ''
    },
    arrayValues() {
      return Array.isArray(this.value) ? this.value : []
    }
  },
  watch: {
    value: {
      immediate: true,
      handler(val) {
        if (this.field.fieldType !== 'object') return
        this.objectText = val == null ? '' : (typeof val === 'string' ? val : JSON.stringify(val, null, 2))
      }
    }
  },
  methods: {
    enumOptions,
    enumOptionTextStyle,
    enumSelectCssVars,
    arrayItemType(field) {
      return parseTypeConfig(field).itemType || 'string'
    },
    imageLimit(field) {
      return parseTypeConfig(field).maxCount || 9
    },
    fileLimit(field) {
      return parseTypeConfig(field).maxCount || 9
    },
    onObjectTextInput(text) {
      this.objectText = text
    },
    syncObject() {
      const text = (this.objectText || '').trim()
      if (!text) {
        this.$emit('input', null)
        return
      }
      try {
        this.$emit('input', JSON.parse(text))
      } catch (e) {
        this.$message.warning(this.$t('defect.custom-field.invalid-json'))
      }
    },
    onImageInput(val) {
      this.$emit('input', val || '')
    },
    updateArrayItem(idx, val) {
      const next = [...this.arrayValues]
      next[idx] = val
      this.$emit('input', next)
    },
    addArrayItem() {
      this.$emit('input', [...this.arrayValues, null])
    },
    removeArrayItem(idx) {
      const next = [...this.arrayValues]
      next.splice(idx, 1)
      this.$emit('input', next)
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
