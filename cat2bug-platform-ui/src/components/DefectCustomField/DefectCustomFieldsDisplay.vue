<template>
  <div v-if="rows.length" class="defect-custom-fields-display">
    <el-row v-if="gridRows.length" :gutter="20">
      <el-col v-for="row in gridRows" :key="row.fieldKey" :span="12" class="defect-custom-field-display-col">
        <label>{{ row.fieldLabel }}:</label>
        <span v-if="row.fieldType === 'enum'" class="defect-custom-field-enum-tag" :style="{ color: row.color }">
          {{ row.display }}
        </span>
        <el-tooltip
          v-else-if="row.displayAsDeliverablePath && row.fullPath"
          :content="row.fullPath"
          placement="top"
          :disabled="row.fullPath === row.display"
        >
          <span class="deliverable-path-text">{{ row.display || $t('no-data') }}</span>
        </el-tooltip>
        <span v-else>{{ row.display || $t('no-data') }}</span>
      </el-col>
    </el-row>
    <el-collapse
      v-if="accordionRows.length"
      v-model="accordionActiveNames"
      class="defect-custom-field-accordions"
    >
      <el-collapse-item
        v-for="row in accordionRows"
        :key="row.fieldKey"
        :title="row.fieldLabel"
        :name="row.fieldKey"
      >
        <div v-if="row.fieldType === 'image' && row.urls.length" class="defect-image-tiles">
          <cat2-bug-image
            v-for="(img, index) in row.urls"
            :key="index"
            :src="img"
            :preview-src-list="row.urls"
            fit="contain"
          />
        </div>
        <el-empty
          v-else-if="row.fieldType === 'image'"
          :description="$t('no-data')"
        />
        <div v-else-if="row.fieldType === 'object'" class="defect-custom-field-object">
          <el-tooltip
            v-if="row.displayAsDeliverablePath && row.fullPath"
            :content="row.fullPath"
            placement="top"
            :disabled="row.fullPath === row.display"
          >
            <span class="deliverable-path-text">{{ row.display || $t('no-data') }}</span>
          </el-tooltip>
          <pre v-else class="defect-custom-field-json">{{ row.display || $t('no-data') }}</pre>
        </div>
        <div v-else-if="row.fieldType === 'file' && row.urls.length" class="annex-list">
          <cat2-bug-text
            v-for="(file, index) in row.urls"
            :key="index"
            class="annex-list-item"
            :content="file"
            type="down"
            :tooltip="file"
          />
        </div>
        <el-empty
          v-else-if="row.fieldType === 'file'"
          :description="$t('no-data')"
        />
      </el-collapse-item>
    </el-collapse>
  </div>
  <el-empty v-else :description="$t('no-data')" />
</template>

<script>
import Cat2BugImage from '@/components/Cat2BugImage'
import Cat2BugText from '@/components/Cat2BugText'
import { listModulePath } from '@/api/system/module'
import { TableOptions } from '@/views/system/defect/list/table-options'
import { TABLE_KEY_TO_BUILTIN_FIELD_KEY } from '@/utils/defect-field-layout'
import {
  buildDefectTableColumnDefaults,
  loadDefectColumnLayout
} from '@/utils/defect-custom-field-columns'
import { resolveDefectMergedColumns } from '@/utils/defect-display-field'
import {
  DEFECT_DETAIL_EXTRA_ROWS,
  resolveOrderedVisibleDetailColumns
} from '@/utils/defect-detail-fields'
import { formatCustomFieldValue, normalizeFieldRow, enumOptions } from './format'
import { formatDeliverablePath, looksLikeDeliverablePath } from '@/utils/deliverable-path-display'
import { parseTime } from '@/utils/ruoyi'
import { lifeTime } from '@/utils/defect'

/** 基础信息内以风箱展示的类型及排序：图片 → 对象 → 附件 */
const ACCORDION_FIELD_TYPES = Object.freeze(['image', 'object', 'file'])
const ACCORDION_TYPE_ORDER = Object.freeze({ image: 0, object: 1, file: 2 })

export default {
  name: 'DefectCustomFieldsDisplay',
  dicts: ['defect_level'],
  components: { Cat2BugImage, Cat2BugText },
  props: {
    projectId: { type: Number, default: null },
    /** 完整缺陷对象（详情抽屉传入） */
    defect: { type: Object, default: null },
    /** 关联用例（展示测试用例名称） */
    defectCase: { type: Object, default: null },
    customFields: { type: Object, default: () => ({}) }
  },
  data() {
    return {
      layout: null,
      mergedColumns: [],
      accordionActiveNames: [],
      modulePathById: {}
    }
  },
  computed: {
    rows() {
      const defect = this.defect
      if (!defect || !this.mergedColumns.length) {
        return []
      }
      const cols = resolveOrderedVisibleDetailColumns(this.mergedColumns, this.layout || {})
      const list = cols.map((col) => this.buildRowFromColumn(col, defect)).filter(Boolean)
      DEFECT_DETAIL_EXTRA_ROWS.forEach((extra) => {
        list.push(this.buildExtraRow(extra, defect))
      })
      return list
    },
    gridRows() {
      return this.rows.filter((row) => !ACCORDION_FIELD_TYPES.includes(row.fieldType))
    },
    accordionRows() {
      const indexed = this.rows
        .map((row, index) => ({ row, index }))
        .filter(({ row }) => ACCORDION_FIELD_TYPES.includes(row.fieldType))
      indexed.sort((a, b) => {
        const typeDiff =
          (ACCORDION_TYPE_ORDER[a.row.fieldType] ?? 99) -
          (ACCORDION_TYPE_ORDER[b.row.fieldType] ?? 99)
        return typeDiff !== 0 ? typeDiff : a.index - b.index
      })
      return indexed.map(({ row }) => row)
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler(id) {
        this.reloadLayout(id)
        this.reloadModulePathMap(id)
      }
    },
    accordionRows: {
      immediate: true,
      handler(rows) {
        this.accordionActiveNames = rows
          .filter((row) => this.accordionRowHasContent(row))
          .map((row) => row.fieldKey)
      }
    }
  },
  methods: {
    applyDeliverablePathDisplay(row, fullPath, maxLength = 32) {
      const formatted = formatDeliverablePath(fullPath, maxLength)
      row.fullPath = formatted.full
      row.display = formatted.short || formatted.full
      row.displayAsDeliverablePath = !!formatted.full
      return row
    },
    accordionRowHasContent(row) {
      if (row.fieldType === 'image' || row.fieldType === 'file') {
        return row.urls && row.urls.length > 0
      }
      if (row.fieldType === 'object') {
        return !!(row.display && String(row.display).trim())
      }
      return false
    },
    resolveModuleFullPath(defect) {
      if (!defect) return ''
      if (defect.modulePath) {
        return String(defect.modulePath).trim()
      }
      const moduleId = defect.moduleId
      if (moduleId != null && this.modulePathById[moduleId]) {
        return String(this.modulePathById[moduleId]).trim()
      }
      return defect.moduleName ? String(defect.moduleName).trim() : ''
    },
    reloadModulePathMap(projectId) {
      const pid = Number(projectId)
      if (!pid) {
        this.modulePathById = {}
        return
      }
      listModulePath(pid).then((res) => {
        const map = {}
        ;(res.data || []).forEach((m) => {
          if (m && m.moduleId != null && m.modulePath) {
            map[m.moduleId] = m.modulePath
          }
        })
        this.modulePathById = map
      }).catch(() => {
        this.modulePathById = {}
      })
    },
    reloadLayout(projectId) {
      const pid = Number(projectId)
      if (!pid) {
        this.layout = null
        this.mergedColumns = []
        return
      }
      loadDefectColumnLayout(pid).then((layout) => {
        this.layout = layout
        const defaults = buildDefectTableColumnDefaults(
          TableOptions.map((c) => ({ ...c })),
          layout
        )
        this.mergedColumns = resolveDefectMergedColumns(this.$cache.local, defaults)
      }).catch(() => {
        this.layout = null
        this.mergedColumns = []
      })
    },
    columnLabel(col) {
      if (col.isCustomField && col.fieldLabel) {
        return String(col.fieldLabel)
      }
      const builtinKey = TABLE_KEY_TO_BUILTIN_FIELD_KEY[col.key]
      if (builtinKey) {
        const i18nKey = 'defect.builtin-field.' + builtinKey
        if (this.$te(i18nKey)) return this.$t(i18nKey)
      }
      if (col.label) return String(col.label)
      if (col.key && this.$te(String(col.key))) return this.$t(col.key)
      return String(col.key || '')
    },
    formatMembers(users) {
      if (!users || !users.length) return ''
      return users
        .map((u) => (u && (u.nickName || u.userName)) || '')
        .filter(Boolean)
        .join(', ')
    },
    formatDefectLevel(val) {
      if (val == null || val === '') return ''
      const items = (this.dict && this.dict.type && this.dict.type.defect_level) || []
      const hit = items.find((d) => String(d.value) === String(val))
      return hit ? hit.label : String(val)
    },
    buildExtraRow(extra, defect) {
      let display = ''
      if (extra.fieldKey === '__lifeTime') {
        display = lifeTime(defect) || ''
      } else if (extra.fieldKey === '__rejectCount') {
        display =
          defect.rejectCount != null && defect.rejectCount !== ''
            ? String(defect.rejectCount)
            : '0'
      }
      return {
        fieldKey: extra.fieldKey,
        fieldLabel: this.$t(extra.labelKey),
        fieldType: 'string',
        display,
        urls: [],
        color: null
      }
    },
    buildRowFromColumn(col, defect) {
      const cf = defect.customFields || this.customFields || {}
      const fk = col.customFieldKey || (String(col.key).startsWith('custom:') ? String(col.key).slice(7) : null)
      if (fk || col.isCustomField) {
        const meta = col.customFieldMeta
          ? normalizeFieldRow(col.customFieldMeta)
          : normalizeFieldRow({ fieldKey: fk, fieldLabel: col.fieldLabel, fieldType: col.fieldType, typeConfig: col.typeConfig })
        const val = cf[fk]
        const row = {
          fieldKey: fk || col.key,
          fieldLabel: this.columnLabel(col),
          fieldType: meta.fieldType,
          display: formatCustomFieldValue(meta, val),
          urls: [],
          color: null
        }
        if (meta.fieldType === 'enum') {
          const opt = enumOptions(meta).find((o) => o.key === val)
          row.color = opt && opt.color ? opt.color : '#606266'
        }
        if (meta.fieldType === 'image' || meta.fieldType === 'file') {
          const arr = Array.isArray(val) ? val : val ? String(val).split(',') : []
          row.urls = arr.filter(Boolean).map((u) =>
            u.startsWith('http') ? u : process.env.VUE_APP_BASE_API + u
          )
        }
        if (meta.fieldType === 'object' && looksLikeDeliverablePath(row.display)) {
          this.applyDeliverablePathDisplay(row, row.display)
        }
        return row
      }

      const label = this.columnLabel(col)
      const prop = col.prop
      let display = ''
      let fieldType = 'string'
      const urls = []
      let color = null

      switch (prop) {
        case 'defectTypeName':
          display = defect.defectTypeName ? this.$t(defect.defectTypeName) : ''
          break
        case 'defectState':
          display = defect.defectStateName ? this.$t(defect.defectStateName) : ''
          break
        case 'defectLevel':
          display = this.formatDefectLevel(defect.defectLevel)
          break
        case 'handleBy':
          display = this.formatMembers(defect.handleByList)
          break
        case 'createMember':
          display =
            defect.createMemberName ||
            (defect.createMember && (defect.createMember.nickName || defect.createMember.userName)) ||
            ''
          break
        case 'moduleName': {
          const fullPath = this.resolveModuleFullPath(defect)
          const row = {
            fieldKey: col.key,
            fieldLabel: label,
            fieldType: 'string',
            display: '',
            urls: [],
            color: null,
            fullPath: '',
            displayAsDeliverablePath: false
          }
          if (fullPath) {
            this.applyDeliverablePathDisplay(row, fullPath)
          }
          return row
        }
        case 'moduleVersion':
          display = defect.moduleVersion || ''
          break
        case 'projectNum':
          display = defect.projectNum != null ? '#' + defect.projectNum : ''
          break
        case 'caseId':
          display =
            (this.defectCase && this.defectCase.caseName) ||
            (defect.caseName || '') ||
            (defect.caseId != null ? String(defect.caseId) : '')
          break
        case 'updateTime':
        case 'planStartTime':
        case 'planEndTime':
          display = defect[prop] ? parseTime(defect[prop], '{y}-{m}-{d} {h}:{i}:{s}') : ''
          fieldType = 'datetime'
          break
        case 'imgUrls':
        case 'annexUrls': {
          const raw = defect[prop]
          const arr = raw ? String(raw).split(',').filter(Boolean) : []
          fieldType = prop === 'imgUrls' ? 'image' : 'file'
          urls.push(...arr.map((u) => (u.startsWith('http') ? u : process.env.VUE_APP_BASE_API + u)))
          display = arr.length ? '' : ''
          break
        }
        case 'defectDescribe':
          display = defect.defectDescribe || ''
          fieldType = 'text'
          break
        default:
          display = defect[prop] != null ? String(defect[prop]) : ''
      }

      return {
        fieldKey: col.key,
        fieldLabel: label,
        fieldType,
        display,
        urls,
        color
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.deliverable-path-text {
  cursor: default;
  word-break: break-all;
}
.defect-custom-field-display-col {
  margin-bottom: 8px;
  label {
    color: var(--text-color-secondary, #909399);
    margin-right: 6px;
  }
}
.defect-custom-field-json {
  margin: 0;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
.defect-custom-field-accordions {
  margin-top: 12px;
  border-top: none;
  border-bottom: none;

  ::v-deep .el-collapse-item__header {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-color-regular, #606266);
    border-bottom-color: var(--border-color-lighter, #ebeef5);
  }

  ::v-deep .el-collapse-item__content {
    padding-top: 12px;
    padding-bottom: 8px;
  }

  ::v-deep .el-collapse-item:last-child .el-collapse-item__wrap {
    border-bottom: none !important;
  }
}
.annex-list {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;

  ::v-deep .annex-list-item {
    display: block;
    width: 100%;
    border-bottom: none !important;
  }

  ::v-deep .annex-list-item:last-child {
    border-bottom: none !important;
    margin-bottom: 0;
    padding-bottom: 0;
  }
}
.defect-image-tiles {
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 10px;

  ::v-deep .el-image {
    width: 150px;
    height: 150px;
    border-radius: var(--cat2bug-border-radius, 4px);
    overflow: hidden;
  }
}
</style>
