<template>
  <div class="defect-field-default-value">
    <el-input
      v-if="fieldType === 'string'"
      :value="stringValue"
      clearable
      :maxlength="maxLength || 512"
      :placeholder="$t('defect.custom-field.default-value-placeholder')"
      @input="onString"
    />
    <el-input-number
      v-else-if="fieldType === 'number'"
      :value="numberValue"
      controls-position="right"
      class="defect-field-default-value-number"
      @input="onNumber"
    />
    <el-switch
      v-else-if="fieldType === 'boolean'"
      :value="booleanValue"
      @change="onBoolean"
    />
    <el-date-picker
      v-else-if="fieldType === 'datetime'"
      :value="stringValue"
      type="datetime"
      value-format="yyyy-MM-dd HH:mm:ss"
      class="defect-field-default-value-datetime"
      clearable
      @input="onString"
    />
    <el-select
      v-else-if="fieldType === 'enum'"
      :value="stringValue"
      clearable
      class="defect-field-default-value-enum defect-enum-select"
      :style="enumSelectCssVars({ typeConfig }, stringValue)"
      :placeholder="$t('defect.custom-field.default-value-placeholder')"
      @change="onString"
    >
      <el-option
        v-for="opt in enumOptions"
        :key="opt.key"
        :label="opt.label || opt.key"
        :value="opt.key"
      >
        <span class="defect-custom-field-enum-dot" :style="{ background: opt.color || '#909399' }" />
        <span :style="enumOptionTextStyle(opt)">{{ opt.label || opt.key }}</span>
      </el-option>
    </el-select>
    <el-input
      v-else-if="fieldType === 'object'"
      :value="objectText"
      type="textarea"
      :rows="3"
      :placeholder="$t('defect.custom-field.object-placeholder')"
      @input="onObjectText"
      @blur="syncObject"
    />
    <image-upload
      v-else-if="fieldType === 'image'"
      v-model="imageFileValue"
      :limit="imageLimit"
      :is-show-clipboard-button="false"
    />
    <file-upload
      v-else-if="fieldType === 'file'"
      v-model="fileValue"
      :limit="fileLimit"
      :file-type="[]"
    />
    <el-input
      v-else-if="fieldType === 'array'"
      :value="arrayText"
      type="textarea"
      :rows="3"
      :placeholder="$t('defect.custom-field.default-value-array-placeholder')"
      @input="onArrayText"
      @blur="syncArray"
    />
    <span v-else class="defect-field-default-value-unsupported">—</span>
  </div>
</template>

<script>
import ImageUpload from '@/components/ImageUpload'
import FileUpload from '@/components/FileUpload'
import {
  enumOptionTextStyle,
  enumOptions,
  enumSelectCssVars,
  parseTypeConfig,
  urlListFromCustomFieldValue
} from './format'

export default {
  name: 'DefectFieldDefaultValueInput',
  components: { ImageUpload, FileUpload },
  props: {
    fieldType: { type: String, default: 'string' },
    typeConfig: { type: [Object, String], default: () => ({}) },
    maxLength: { type: Number, default: null },
    value: { default: null }
  },
  data() {
    return {
      objectText: '',
      arrayText: ''
    }
  },
  computed: {
    cfg() {
      return parseTypeConfig({ typeConfig: this.typeConfig })
    },
    enumOptions() {
      return enumOptions({ typeConfig: this.typeConfig })
    },
    stringValue() {
      return this.value == null || this.value === '' ? null : String(this.value)
    },
    numberValue() {
      return typeof this.value === 'number' ? this.value : (this.value == null || this.value === '' ? undefined : Number(this.value))
    },
    booleanValue() {
      return this.value === true || this.value === 'true'
    },
    imageLimit() {
      return this.cfg.maxCount || 9
    },
    fileLimit() {
      return this.cfg.maxCount || 9
    },
    imageFileValue: {
      get() {
        const arr = urlListFromCustomFieldValue(this.value)
        return arr.length ? arr.join(',') : ''
      },
      set(v) {
        const urls = urlListFromCustomFieldValue(v)
        this.$emit('input', urls.length ? urls : null)
      }
    },
    fileValue: {
      get() {
        const arr = urlListFromCustomFieldValue(this.value)
        return arr.length ? arr.join(',') : ''
      },
      set(v) {
        const urls = urlListFromCustomFieldValue(v)
        this.$emit('input', urls.length ? urls : null)
      }
    }
  },
  watch: {
    value: {
      immediate: true,
      handler(v) {
        if (this.fieldType === 'object') {
          this.objectText = v == null ? '' : (typeof v === 'string' ? v : JSON.stringify(v, null, 2))
        } else if (this.fieldType === 'array') {
          this.arrayText = v == null ? '' : (Array.isArray(v) ? JSON.stringify(v) : String(v))
        }
      }
    },
    fieldType() {
      this.$emit('input', null)
      this.objectText = ''
      this.arrayText = ''
    }
  },
  methods: {
    enumOptionTextStyle,
    enumSelectCssVars,
    onString(v) {
      this.$emit('input', v == null || v === '' ? null : v)
    },
    onNumber(v) {
      this.$emit('input', v == null || v === '' ? null : v)
    },
    onBoolean(v) {
      this.$emit('input', v)
    },
    onObjectText(text) {
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
    onArrayText(text) {
      this.arrayText = text
    },
    syncArray() {
      const text = (this.arrayText || '').trim()
      if (!text) {
        this.$emit('input', null)
        return
      }
      try {
        const parsed = JSON.parse(text)
        if (!Array.isArray(parsed)) {
          this.$message.warning(this.$t('defect.custom-field.default-value-array-invalid'))
          return
        }
        this.$emit('input', parsed)
      } catch (e) {
        this.$message.warning(this.$t('defect.custom-field.invalid-json'))
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.defect-field-default-value-number,
.defect-field-default-value-datetime,
.defect-field-default-value-enum {
  width: 100%;
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
.defect-field-default-value-unsupported {
  color: #909399;
}
</style>
