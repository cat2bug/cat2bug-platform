<template>
  <div class="app-container case-body defect-field-page defect-field-list-layout project-list-page-host" ref="projectOptionSubMain">
    <el-row class="project-add-page-header project-add-page-header--with-filter project-option-sub-hint-back">
      <el-page-header @back="goBack" :content="$t('defect.custom-field.title')">
      </el-page-header>
    </el-row>

    <div v-if="canAddDefectField" ref="defectFieldTools" class="defect-field-tools">
      <el-button
        class="project-defect-field-hint-add"
        type="primary"
        icon="el-icon-plus"
        size="small"
        @click="handleAdd"
      >{{ $t('defect.custom-field.add-field') }}</el-button>
    </div>

    <div ref="defectFieldListBody" class="defect-field-list-body">
      <div class="defect-field-table-x-scroll">
    <el-table
      ref="defectFieldTable"
      class="defect-field-list-table"
      v-loading="loading"
      :data="paginatedFieldList"
      :row-key="rowKey"
      :max-height="tableBodyMaxHeight"
    >
      <el-table-column
        :label="$t('defect.custom-field.field-source')"
        prop="_sourceOrder"
        :width="sourceColumnWidth"
        align="center"
        class-name="defect-field-source-col"
      >
        <template slot-scope="scope">
          <el-tag
            v-if="isBuiltinField(scope.row)"
            size="small"
            type="info"
            :effect="sourceTagEffect"
            class="defect-field-source-tag"
            :class="sourceTagSystemClass"
          >{{ $t('defect.custom-field.source-system') }}</el-tag>
          <el-tag
            v-else
            size="small"
            :effect="sourceTagEffect"
            class="defect-field-source-tag"
          >{{ $t('defect.custom-field.source-custom') }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('defect.custom-field.field-key')"
        prop="fieldKey"
        min-width="120"
        show-overflow-tooltip
      />
      <el-table-column
        :label="$t('defect.custom-field.field-label')"
        prop="_displayLabel"
        min-width="120"
        show-overflow-tooltip
      >
        <template slot-scope="scope">
          <span class="defect-field-row-kbd-hint-anchor">{{ displayFieldLabel(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('defect.custom-field.field-type')"
        prop="fieldType"
        width="116"
        align="center"
      >
        <template slot-scope="scope">
          {{ fieldTypeLabel(scope.row.fieldType) }}
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('defect.custom-field.max-length')"
        prop="maxLength"
        width="108"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.maxLength != null ? scope.row.maxLength : '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('defect.custom-field.default-value')"
        prop="defaultValue"
        min-width="120"
        show-overflow-tooltip
      >
        <template slot-scope="scope">
          <span>{{ formatDefaultValueDisplay(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('defect.custom-field.required')"
        prop="required"
        width="80"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ isTruthy(scope.row.required) ? $t('defect.custom-field.yes') : $t('defect.custom-field.no') }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('defect.custom-field.nullable')"
        prop="nullable"
        width="80"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ isTruthy(scope.row.nullable) ? $t('defect.custom-field.yes') : $t('defect.custom-field.no') }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('defect.custom-field.enabled')"
        prop="enabled"
        width="90"
        align="center"
      >
        <template slot-scope="scope">
          <el-switch
            v-if="canEditDefectField && !isBuiltinEnabledLocked(scope.row)"
            :value="isTruthy(scope.row.enabled)"
            :disabled="scope.row._toggleLoading"
            @change="val => handleEnabledChange(scope.row, val)"
          />
          <span v-else-if="!isBuiltinEnabledLocked(scope.row)">
            {{ isTruthy(scope.row.enabled) ? $t('defect.custom-field.yes') : $t('defect.custom-field.no') }}
          </span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('defect.custom-field.sort-order')"
        prop="sortOrder"
        width="100"
        align="center"
        class-name="defect-field-sort-col"
      >
        <template slot-scope="scope">
          <div v-if="canEditDefectField" class="sort-order-arrows" @click.stop>
            <i
              class="el-icon-caret-top sort-arrow"
              :class="{ 'is-disabled': !canMoveSort(scope.row, -1) }"
              :title="$t('defect.custom-field.sort-up')"
              @click.stop="handleSortMove(scope.row, -1)"
            />
            <i
              class="el-icon-caret-bottom sort-arrow"
              :class="{ 'is-disabled': !canMoveSort(scope.row, 1) }"
              :title="$t('defect.custom-field.sort-down')"
              @click.stop="handleSortMove(scope.row, 1)"
            />
          </div>
          <span v-else>{{ scope.row.sortOrder != null ? scope.row.sortOrder : 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column
        v-if="canShowOperateColumn"
        :label="$t('operate')"
        align="left"
        :width="operateColumnWidth"
        class-name="cat2bug-operate-column small-padding"
      >
        <template slot-scope="scope">
          <div v-if="!isBuiltinField(scope.row)" class="cat2bug-operate-tools defect-field-operate-tools">
            <el-button
              v-if="canEditDefectField"
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleUpdate(scope.row)"
            >{{ $t('modify') }}</el-button>
            <el-button
              v-if="canRemoveDefectField"
              size="mini"
              type="text"
              icon="el-icon-delete"
              class="red"
              @click="handleDelete(scope.row)"
            >{{ $t('delete') }}</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
      </div>

    <div v-show="total > 0" ref="defectFieldPaginationBand" class="defect-field-table-pagination-band table-pagination-band">
      <pagination
        class="defect-field-table-pagination"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="handlePagination"
      />
    </div>
    </div>

    <el-dialog
      :title="title"
      :visible.sync="open"
      width="720px"
      append-to-body
      :close-on-click-modal="false"
      custom-class="cat2bug-form-shortcut-dialog defect-field-form-dialog"
      :close-on-press-escape="false"
      :before-close="onToolDialogBeforeClose"
      @opened="onToolDialogOpened"
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="120px" class="project-defect-field-dialog-form">
        <el-form-item :label="$t('defect.custom-field.field-key')" prop="fieldKey">
          <el-input
            v-model="form.fieldKey"
            class="defect-field-readonly"
            :disabled="!!form.fieldId"
            :placeholder="$t('defect.custom-field.field-key-placeholder')"
            maxlength="64"
          />
          <div v-if="form.fieldId" class="form-tip">{{ $t('defect.custom-field.field-key-readonly-hint') }}</div>
        </el-form-item>
        <el-form-item :label="$t('defect.custom-field.field-label')" prop="fieldLabel">
          <el-input
            v-model="form.fieldLabel"
            class="defect-field-label-input"
            :placeholder="$t('defect.custom-field.field-label-placeholder')"
            maxlength="6"
            show-word-limit
          />
        </el-form-item>
        <el-form-item :label="$t('defect.custom-field.field-type')" prop="fieldType">
          <el-select
            v-model="form.fieldType"
            class="defect-field-readonly"
            :disabled="!!form.fieldId"
            :placeholder="$t('defect.custom-field.field-type-placeholder')"
            style="width: 100%"
          >
            <el-option
              v-for="item in fieldTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.fieldType === 'enum'" :label="$t('defect.custom-field.enum-options')">
          <el-table :data="enumOptions" size="small" border class="enum-options-table defect-field-enum-table">
            <el-table-column :label="$t('defect.custom-field.option-key')" min-width="120">
              <template slot-scope="scope">
                <el-input v-model="scope.row.key" size="small" maxlength="64" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('defect.custom-field.option-label')" min-width="120">
              <template slot-scope="scope">
                <el-input v-model="scope.row.label" size="small" maxlength="128" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('defect.custom-field.option-color')" width="90" align="center">
              <template slot-scope="scope">
                <el-color-picker
                  v-model="scope.row.color"
                  size="small"
                  popper-class="cat2bug-color-picker-dropdown"
                />
              </template>
            </el-table-column>
            <el-table-column :label="$t('operate')" width="70" align="center">
              <template slot-scope="scope">
                <el-button type="text" icon="el-icon-delete" class="red" @click="removeEnumOption(scope.$index)" />
              </template>
            </el-table-column>
          </el-table>
          <el-button type="text" icon="el-icon-plus" @click="addEnumOption" style="margin-top: 8px;">
            {{ $t('defect.custom-field.add-option') }}
          </el-button>
        </el-form-item>
        <el-form-item
          v-if="supportsFieldDefaultValue(form.fieldType)"
          :label="$t('defect.custom-field.default-value')"
        >
          <defect-field-default-value-input
            v-model="form.defaultValue"
            :field-type="form.fieldType"
            :type-config="form.typeConfig || enumTypeConfigForDefault"
            :max-length="form.maxLength"
          />
          <div class="form-tip">{{ $t('defect.custom-field.default-value-hint') }}</div>
        </el-form-item>
        <el-form-item v-if="supportsFieldMaxLength(form.fieldType)" :label="$t('defect.custom-field.max-length')" prop="maxLength">
          <el-input-number
            v-model="form.maxLength"
            :min="1"
            :max="65535"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="$t('defect.custom-field.required')" prop="required">
          <el-switch v-model="form.required" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item :label="$t('defect.custom-field.nullable')" prop="nullable">
          <el-switch
            v-model="form.nullable"
            :active-value="1"
            :inactive-value="0"
            :disabled="isTruthy(form.required)"
          />
        </el-form-item>
        <el-form-item :label="$t('defect.custom-field.enabled')" prop="enabled">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item :label="$t('defect.custom-field.sort-order')" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button class="defect-kbd-hint-host" type="primary" @click="submitForm">
          {{ $t('ok') }}
          <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
        </el-button>
        <el-button @click="requestCloseToolDialog">{{ $t('cancel') }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listFields,
  listEnabledFields,
  addField,
  updateField,
  deleteField,
  updateBuiltinFieldLayout
} from '@/api/system/defect-field'
import {
  buildDefectTableColumnDefaults,
  clearCustomFieldColumnsCache,
  loadDefectColumnLayout,
  pruneDefectTableColumnCacheFromColumns,
  syncNewDefectTableColumnsIntoFieldListCache
} from '@/utils/defect-custom-field-columns'
import { TableOptions } from '@/views/system/defect/list/table-options'
import { isBuiltinFieldAlwaysEnabled } from '@/utils/defect-field-layout'
import { mapGetters } from 'vuex'
import DefectFieldDefaultValueInput from '@/components/DefectCustomField/DefectFieldDefaultValueInput'
import {
  enumOptionsFromTypeConfig,
  formatCustomFieldValue,
  parseTypeConfigRaw,
  supportsFieldDefaultValue,
  supportsFieldMaxLength
} from '@/components/DefectCustomField/format'
import { hasAnyPermi } from '@/utils/project-option-card'
import projectOptionSubKbd, { PROJECT_OPTION_KBD_SCOPE } from '@/mixins/project-option-sub-kbd'
import defectToolDialogKbd from '@/mixins/defect-tool-dialog-kbd'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import {
  assignRowHintLetters,
  collectHintLettersFromToolbar,
  getDefectTableScrollBody,
  isRowIntersectingContainer,
  resolveElTableRowData,
  resolveTableRowFirstColumnHintAnchor,
  resolveTableRowFirstColumnKbdBadgeLayout
} from '@/utils/defect-row-kbd-hints'

const FIELD_TYPES = ['string', 'number', 'boolean', 'datetime', 'enum', 'object', 'image', 'file', 'array']
const FIELD_KEY_PATTERN = /^[a-z][a-z0-9_]{0,63}$/

export default {
  name: 'ProjectDefectFields',
  mixins: [projectOptionSubKbd, defectToolDialogKbd],
  components: { DefectFieldDefaultValueInput },
  data() {
    return {
      loading: false,
      total: 0,
      fieldList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10
      },
      tableBodyMaxHeight: null,
      operateColumnWidth: 120,
      sourceColumnWidth: 128,
      _suppressEnumWatch: false,
      open: false,
      title: '',
      enumOptions: [],
      form: {},
      rules: {
        fieldKey: [{ required: true, validator: this.validateFieldKeyRule, trigger: 'blur' }],
        fieldLabel: [{ required: true, validator: this.validateFieldLabelRule, trigger: 'blur' }],
        fieldType: [{ required: true, message: this.$i18n.t('defect.custom-field.field-type-required'), trigger: 'change' }]
      }
    }
  },
  computed: {
    ...mapGetters(['themeMode']),
    /** 浅色主题用 Element 默认 light；深色主题系统灰、自定义蓝（dark） */
    sourceTagEffect() {
      return this.themeMode === 'dark' ? 'dark' : 'light'
    },
    sourceTagSystemClass() {
      return this.themeMode === 'dark' ? 'defect-field-source-tag--system' : ''
    },
    projectId() {
      return parseInt(this.$store.state.user.config.currentProjectId)
    },
    canAddDefectField() {
      return hasAnyPermi(['system:project:defect-field:add'])
    },
    canEditDefectField() {
      return hasAnyPermi(['system:project:defect-field:edit'])
    },
    canRemoveDefectField() {
      return hasAnyPermi(['system:project:defect-field:remove'])
    },
    canShowOperateColumn() {
      return this.canEditDefectField || this.canRemoveDefectField
    },
    fieldTypeOptions() {
      return FIELD_TYPES.map(value => ({
        value,
        label: this.$t('defect.custom-field.type.' + value)
      }))
    },
    paginatedFieldList() {
      const pageNum = this.queryParams.pageNum || 1
      const pageSize = this.queryParams.pageSize || 10
      const start = (pageNum - 1) * pageSize
      return this.fieldList.slice(start, start + pageSize)
    },
    enumTypeConfigForDefault() {
      if (this.form.fieldType !== 'enum') {
        return {}
      }
      return {
        options: this.enumOptions
          .filter(opt => opt.key || opt.label)
          .map(opt => ({
            key: (opt.key || '').trim(),
            label: (opt.label || '').trim(),
            color: opt.color
          }))
      }
    },
    /** 供弹框快捷键 mixin 识别 open 状态 */
    dialogVisible: {
      get() {
        return this.open
      },
      set(val) {
        this.open = val
      }
    }
  },
  watch: {
    open(val) {
      if (typeof this.$_syncFormShortcutBinding === 'function') {
        this.$_syncFormShortcutBinding(!!val)
      }
    },
    '$i18n.locale'() {
      this.$nextTick(() => this.syncDefectFieldAdaptiveColumnWidths())
    },
    'form.required'(val) {
      if (this.isTruthy(val)) {
        this.form.nullable = 0
      }
    },
    'form.fieldType'(val) {
      if (this._suppressEnumWatch) {
        return
      }
      if (!supportsFieldMaxLength(val)) {
        this.form.maxLength = null
      } else if (this.form.maxLength == null) {
        this.form.maxLength = 255
      }
      if (val !== 'enum') {
        this.enumOptions = []
      } else if (!this.enumOptions.length) {
        this.loadEnumOptionsFromTypeConfig(this.form.typeConfig)
        if (!this.enumOptions.length) {
          this.addEnumOption()
        }
      }
      if (!supportsFieldDefaultValue(val)) {
        this.form.defaultValue = null
      } else if (!this.form.fieldId) {
        this.form.defaultValue = null
      }
    }
  },
  created() {
    this.getList()
  },
  mounted() {
    this.$nextTick(() => {
      this.initDefectFieldListBodyResizeObserver()
      this.syncDefectFieldTableBodyMaxHeight()
      this.syncDefectFieldAdaptiveColumnWidths()
    })
    window.addEventListener('resize', this.onDefectFieldWindowResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.onDefectFieldWindowResize)
    this.destroyDefectFieldListBodyResizeObserver()
  },
  methods: {
    shouldProjectOptionSubEscGoBack() {
      return !this.open
    },
    shortcutSave() {
      this.submitForm()
    },
    doCloseToolDialog() {
      this.open = false
      this.toolDialogCloseBaseline = null
      this.reset()
    },
    serializeDefectFieldDialogCloseState() {
      return JSON.stringify({
        form: this.form,
        enumOptions: this.enumOptions
      })
    },
    captureToolDialogCloseBaseline() {
      this.toolDialogCloseBaseline = this.serializeDefectFieldDialogCloseState()
    },
    isToolDialogCloseDirty() {
      if (!this.toolDialogCloseBaseline) return false
      return this.serializeDefectFieldDialogCloseState() !== this.toolDialogCloseBaseline
    },
    getFieldHintScrollContainer() {
      const form = this.$refs.form
      if (form && form.$el) {
        const body = form.$el.closest('.el-dialog__body')
        if (body) return body
      }
      return typeof this.getFieldHintContainer === 'function' ? this.getFieldHintContainer() : null
    },
    getProjectOptionSubPaginationSelector() {
      return '.defect-field-table-pagination'
    },
    getProjectOptionSubRegisterActions() {
      return [
        { key: 'create', defaultLetter: 'E', run: () => this.shortcutAddDefectField() }
      ]
    },
    getProjectOptionSubActionHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${PROJECT_OPTION_KBD_SCOPE}.${key}`, def)
      return [
        {
          key: 'create',
          letter: L('create', 'E'),
          badgeSelector: '.project-defect-field-hint-add',
          floatOffset: { placement: 'bottom-right-outset', outset: 2, dy: 5 },
          run: () => this.shortcutAddDefectField(),
          visible: () => this.canAddDefectField
        }
      ]
    },
    shortcutAddDefectField() {
      if (!this.canAddDefectField) return
      this.handleAdd()
    },
    /** ⌘ 按住：表格可见自定义字段行「字段名称」列动态徽标（1–9 优先，字母补位） */
    getPageDynamicActionHints(ctx) {
      const used = (ctx && ctx.usedLetters) ? new Set(ctx.usedLetters) : new Set()
      collectHintLettersFromToolbar(this.getPageActionHints()).forEach((ch) => used.add(ch))
      return this.buildDefectFieldTableRowActionHints(used)
    },
    getPageActionHintScrollRoots() {
      const table = this.$refs.defectFieldTable
      if (!table || !table.$el) return []
      const bodyWrap = getDefectTableScrollBody(table.$el)
      return bodyWrap ? [bodyWrap] : []
    },
    buildDefectFieldTableRowActionHints(usedLetters) {
      if (this.loading || this.open || !this.canEditDefectField) return []
      const table = this.$refs.defectFieldTable
      if (!table || !table.$el) return []
      const bodyWrap = getDefectTableScrollBody(table.$el)
      if (!bodyWrap) return []
      const tableRoot = table.$el
      const list = this.paginatedFieldList || []
      const seen = new Set()
      const anchors = []
      bodyWrap.querySelectorAll('tbody tr.el-table__row').forEach((tr, rowIndex) => {
        if (!isRowIntersectingContainer(tr, bodyWrap)) return
        const row = resolveElTableRowData(tr) || list[rowIndex]
        if (!row || this.isBuiltinField(row) || row.fieldId == null) return
        const fieldId = String(row.fieldId)
        if (seen.has(fieldId)) return
        seen.add(fieldId)
        const rowKey = { field: 'fieldId', value: fieldId }
        const layout = resolveTableRowFirstColumnKbdBadgeLayout(
          tr, tableRoot, '.defect-field-row-kbd-hint-anchor', rowKey
        )
        const anchor = tr.querySelector('.defect-field-row-kbd-hint-anchor') ||
          resolveTableRowFirstColumnHintAnchor(tr, tableRoot, '.defect-field-row-kbd-hint-anchor', rowKey)
        if (!anchor || !layout.rect) return
        anchors.push({
          anchor,
          getAnchorRect: () => layout.rect,
          floatOffset: layout.floatOffset,
          skipViewportCheck: true,
          run: () => {
            if (!this.canEditDefectField) return
            this.handleUpdate(row)
          }
        })
      })
      const letters = assignRowHintLetters(anchors.length, usedLetters)
      return anchors.map((item, i) => ({
        ...item,
        letter: letters[i],
        key: `row-${i}`
      })).filter((item) => item.letter)
    },
    initDefectFieldListBodyResizeObserver() {
      if (typeof ResizeObserver === 'undefined') return
      this.destroyDefectFieldListBodyResizeObserver()
      const el = this.$refs.defectFieldListBody
      if (!el) return
      this._defectFieldListBodyResizeObserver = new ResizeObserver(() => {
        this.syncDefectFieldTableBodyMaxHeight()
      })
      this._defectFieldListBodyResizeObserver.observe(el)
    },
    destroyDefectFieldListBodyResizeObserver() {
      if (this._defectFieldListBodyResizeObserver) {
        this._defectFieldListBodyResizeObserver.disconnect()
        this._defectFieldListBodyResizeObserver = null
      }
    },
    onDefectFieldWindowResize() {
      this.syncDefectFieldTableBodyMaxHeight()
      this.syncDefectFieldAdaptiveColumnWidths()
    },
    syncDefectFieldTableBodyMaxHeight() {
      this.$nextTick(() => {
        const body = this.$refs.defectFieldListBody
        if (!body || !body.clientHeight) return
        const pagEl = this.$refs.defectFieldPaginationBand
        let reserveBelowTable = 0
        if (this.total > 0 && pagEl && pagEl.offsetParent !== null) {
          const st = window.getComputedStyle(pagEl)
          reserveBelowTable =
            pagEl.offsetHeight +
            parseFloat(st.marginTop || '0') +
            parseFloat(st.marginBottom || '0')
        }
        const next = Math.max(120, Math.floor(body.clientHeight - reserveBelowTable - 2))
        if (this.tableBodyMaxHeight !== next) {
          this.tableBodyMaxHeight = next
          this.$nextTick(() => {
            const table = this.$refs.defectFieldTable
            if (table && typeof table.doLayout === 'function') {
              table.doLayout()
            }
            this.syncDefectFieldAdaptiveColumnWidths()
          })
        }
      })
    },
    /** 来源 tag、操作按钮等列按当前语言内容自适应宽度 */
    syncDefectFieldAdaptiveColumnWidths() {
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          const table = this.$refs.defectFieldTable
          if (!table || !table.$el) return
          const paddingX = 16
          let changed = false

          const measureEl = (el) => Math.max(el.getBoundingClientRect().width, el.scrollWidth || 0)

          let maxSourceTag = 0
          table.$el.querySelectorAll('td.defect-field-source-col .defect-field-source-tag').forEach((el) => {
            maxSourceTag = Math.max(maxSourceTag, measureEl(el))
          })
          const sourceHeader = table.$el.querySelector('th.defect-field-source-col .cell')
          const sourceHeaderW = sourceHeader ? measureEl(sourceHeader) : 0
          const nextSource = Math.max(80, Math.ceil(Math.max(maxSourceTag, sourceHeaderW) + paddingX))
          if (this.sourceColumnWidth !== nextSource) {
            this.sourceColumnWidth = nextSource
            changed = true
          }

          let maxOperate = 0
          table.$el.querySelectorAll('td.cat2bug-operate-column .cat2bug-operate-tools').forEach((el) => {
            maxOperate = Math.max(maxOperate, measureEl(el))
          })
          const operateHeader = table.$el.querySelector('th.cat2bug-operate-column .cell')
          const operateHeaderW = operateHeader ? measureEl(operateHeader) : 0
          const nextOperate = Math.min(
            450,
            Math.max(100, Math.ceil(Math.max(maxOperate, operateHeaderW) + paddingX))
          )
          if (this.operateColumnWidth !== nextOperate) {
            this.operateColumnWidth = nextOperate
            changed = true
          }

          if (changed && typeof table.doLayout === 'function') {
            table.doLayout()
          }
        })
      })
    },
    normalizeLabelForCompare(label) {
      return String(label || '').trim().toLowerCase()
    },
    isFieldKeyDuplicated(key, excludeFieldId) {
      const normalizedKey = String(key || '').trim()
      if (!normalizedKey) {
        return false
      }
      return this.fieldList.some(row => {
        if (!this.isBuiltinField(row) && excludeFieldId && row.fieldId === excludeFieldId) {
          return false
        }
        return String(row.fieldKey || '').trim() === normalizedKey
      })
    },
    isFieldLabelDuplicated(label, excludeFieldId) {
      const normalizedLabel = this.normalizeLabelForCompare(label)
      if (!normalizedLabel) {
        return false
      }
      return this.fieldList.some(row => {
        if (!this.isBuiltinField(row) && excludeFieldId && row.fieldId === excludeFieldId) {
          return false
        }
        return this.normalizeLabelForCompare(this.displayFieldLabel(row)) === normalizedLabel
      })
    },
    validateFieldKeyRule(rule, value, callback) {
      if (!value) {
        callback(new Error(this.$t('defect.custom-field.field-key-required')))
        return
      }
      if (!FIELD_KEY_PATTERN.test(value)) {
        callback(new Error(this.$t('defect.custom-field.field-key-format')))
        return
      }
      if (!this.form.fieldId && this.isFieldKeyDuplicated(value, null)) {
        callback(new Error(this.$t('defect.custom-field.field-key-duplicate')))
        return
      }
      callback()
    },
    validateFieldLabelRule(rule, value, callback) {
      if (!value || !String(value).trim()) {
        callback(new Error(this.$t('defect.custom-field.field-label-required')))
        return
      }
      if (String(value).trim().length > 6) {
        callback(new Error(this.$t('defect.custom-field.field-label-max-length')))
        return
      }
      if (this.isFieldLabelDuplicated(value, this.form.fieldId || null)) {
        callback(new Error(this.$t('defect.custom-field.field-label-duplicate')))
        return
      }
      callback()
    },
    isTruthy(val) {
      return val === true || val === 1 || val === '1'
    },
    rowKey(row) {
      return row.systemBuiltin ? `builtin:${row.fieldKey}` : `custom:${row.fieldId}`
    },
    isBuiltinField(row) {
      return row && (row.systemBuiltin === true || row.systemBuiltin === 1)
    },
    isBuiltinEnabledLocked(row) {
      return this.isBuiltinField(row) && isBuiltinFieldAlwaysEnabled(row.fieldKey)
    },
    displayFieldLabel(row) {
      if (this.isBuiltinField(row)) {
        const key = 'defect.builtin-field.' + row.fieldKey
        return this.$te(key) ? this.$t(key) : (row.fieldLabel || row.fieldKey)
      }
      return row.fieldLabel
    },
    builtinLayoutPayload(row, patch) {
      return [{
        systemBuiltin: true,
        fieldKey: row.fieldKey,
        enabled: patch.enabled != null ? patch.enabled : row.enabled,
        sortOrder: patch.sortOrder != null ? patch.sortOrder : row.sortOrder
      }]
    },
    fieldTypeLabel(type) {
      if (!type) {
        return '—'
      }
      const key = 'defect.custom-field.type.' + type
      return this.$te(key) ? this.$t(key) : type
    },
    supportsFieldDefaultValue,
    supportsFieldMaxLength,
    formatDefaultValueDisplay(row) {
      if (this.isBuiltinField(row) || !supportsFieldDefaultValue(row.fieldType)) {
        return '—'
      }
      if (row.defaultValue === undefined || row.defaultValue === null || row.defaultValue === '') {
        return '—'
      }
      return formatCustomFieldValue(row, row.defaultValue) || '—'
    },
    parseTypeConfig(typeConfig) {
      return parseTypeConfigRaw(typeConfig)
    },
    getList() {
      this.loading = true
      listFields(this.projectId).then(response => {
        const data = response.data
        const rows = Array.isArray(data) ? data : (response.rows || [])
        this.fieldList = rows.map(row => this.decorateFieldRow(row))
        this.sortFieldList()
        this.syncPaginationTotal()
        this.loading = false
        this.$nextTick(() => {
          this.syncDefectFieldTableBodyMaxHeight()
          this.syncDefectFieldAdaptiveColumnWidths()
        })
      }).catch(() => {
        this.loading = false
      })
    },
    decorateFieldRow(row) {
      const builtin = this.isBuiltinField(row)
      return {
        ...row,
        _sourceOrder: builtin ? 1 : 0,
        _displayLabel: this.displayFieldLabel(row)
      }
    },
    syncPaginationTotal() {
      this.total = this.fieldList.length
      const pageSize = this.queryParams.pageSize || 10
      const maxPage = Math.max(1, Math.ceil(this.total / pageSize) || 1)
      if (this.queryParams.pageNum > maxPage) {
        this.queryParams.pageNum = maxPage
      }
    },
    handlePagination() {
      this.syncPaginationTotal()
      this.$nextTick(() => {
        this.syncDefectFieldTableBodyMaxHeight()
        this.syncDefectFieldAdaptiveColumnWidths()
      })
    },
    /** 默认：系统与自定义共用 sortOrder 排序 */
    sortFieldList() {
      this.fieldList.sort((a, b) => this.compareByDefaultOrder(a, b))
    },
    compareByDefaultOrder(a, b) {
      const sa = a.sortOrder != null ? a.sortOrder : 0
      const sb = b.sortOrder != null ? b.sortOrder : 0
      if (sa !== sb) return sa - sb
      return this.compareText(a.fieldKey, b.fieldKey)
    },
    nextUnifiedSortOrder() {
      return this.fieldList.length
    },
    compareText(a, b) {
      return String(a == null ? '' : a).localeCompare(String(b == null ? '' : b), undefined, { sensitivity: 'base' })
    },
    findRowIndex(row) {
      const key = this.rowKey(row)
      return this.fieldList.findIndex(r => this.rowKey(r) === key)
    },
    /** 参与排序的全部字段（系统 + 自定义共用一套 sortOrder） */
    getSortablePeers() {
      return this.fieldList
    },
    buildPatchPayload(row, patch) {
      return {
        projectId: row.projectId || this.projectId,
        fieldId: row.fieldId,
        fieldKey: row.fieldKey,
        fieldLabel: row.fieldLabel,
        fieldType: row.fieldType,
        maxLength: row.maxLength,
        required: row.required,
        nullable: row.nullable,
        enabled: row.enabled,
        sortOrder: row.sortOrder,
        typeConfig: this.parseTypeConfig(row.typeConfig),
        defaultValue: row.defaultValue != null ? row.defaultValue : null,
        ...patch
      }
    },
    handleEnabledChange(row, val) {
      if (this.isBuiltinEnabledLocked(row) && !val) {
        return
      }
      const enabled = val ? 1 : 0
      this.$set(row, '_toggleLoading', true)
      const request = this.isBuiltinField(row)
        ? updateBuiltinFieldLayout(this.projectId, this.builtinLayoutPayload(row, { enabled }))
        : updateField(this.projectId, row.fieldId, this.buildPatchPayload(row, { enabled }))
      request
        .then(() => {
          row.enabled = enabled
          clearCustomFieldColumnsCache()
          return loadDefectColumnLayout(this.projectId, { force: true })
        })
        .then(layout => {
          if (!layout) return
          const cols = buildDefectTableColumnDefaults(TableOptions.map(c => ({ ...c })), layout)
          pruneDefectTableColumnCacheFromColumns(this.$cache.local, cols)
          if (enabled) {
            syncNewDefectTableColumnsIntoFieldListCache(this.$cache.local, cols)
          }
        })
        .catch(() => {})
        .finally(() => {
          this.$set(row, '_toggleLoading', false)
        })
    },
    canMoveSort(row, delta) {
      if (row && row._toggleLoading) return false
      const peers = this.getSortablePeers()
      const gi = peers.findIndex(r => this.rowKey(r) === this.rowKey(row))
      if (gi < 0) return false
      const target = gi + delta
      return target >= 0 && target < peers.length
    },
    /** 上移/下移：在整表内交换位置，并按 0..n-1 写回 sortOrder（非表头查询排序） */
    handleSortMove(row, delta) {
      if (!this.canMoveSort(row, delta)) {
        return
      }
      const peers = this.getSortablePeers()
      const gi = peers.findIndex(r => this.rowKey(r) === this.rowKey(row))
      const reordered = peers.slice()
      const tmp = reordered[gi]
      reordered[gi] = reordered[gi + delta]
      reordered[gi + delta] = tmp

      const changes = []
      reordered.forEach((r, index) => {
        if (r.sortOrder !== index) {
          changes.push({ row: r, sortOrder: index })
        }
      })
      if (!changes.length) {
        return
      }

      changes.forEach(c => this.$set(c.row, '_toggleLoading', true))
      Promise.all(changes.map(c => {
        const r = c.row
        return this.isBuiltinField(r)
          ? updateBuiltinFieldLayout(this.projectId, this.builtinLayoutPayload(r, { sortOrder: c.sortOrder }))
          : updateField(this.projectId, r.fieldId, this.buildPatchPayload(r, { sortOrder: c.sortOrder }))
      }))
        .then(() => {
          changes.forEach(c => {
            c.row.sortOrder = c.sortOrder
          })
          this.sortFieldList()
          clearCustomFieldColumnsCache()
        })
        .catch(() => {
          this.getList()
          this.$modal.msgError(this.$t('defect.custom-field.sort-move-fail'))
        })
        .finally(() => {
          changes.forEach(c => this.$set(c.row, '_toggleLoading', false))
        })
    },
    reset() {
      this.form = {
        fieldId: null,
        projectId: this.projectId,
        fieldKey: null,
        fieldLabel: null,
        fieldType: 'string',
        maxLength: 255,
        required: 0,
        nullable: 1,
        enabled: 1,
        sortOrder: this.nextUnifiedSortOrder(),
        typeConfig: null,
        defaultValue: null
      }
      this.enumOptions = []
      this.resetForm('form')
    },
    cancel() {
      this.requestCloseToolDialog()
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = this.$t('defect.custom-field.add-field')
      this.$nextTick(() => {
        const btn = this.$el && this.$el.querySelector('.project-defect-field-hint-add')
        if (btn && typeof btn.blur === 'function') btn.blur()
      })
    },
    loadEnumOptionsFromTypeConfig(typeConfig) {
      this.enumOptions = enumOptionsFromTypeConfig(typeConfig)
    },
    handleUpdate(row) {
      const typeConfig = this.parseTypeConfig(row.typeConfig)
      this._suppressEnumWatch = true
      if (row.fieldType === 'enum') {
        this.loadEnumOptionsFromTypeConfig(typeConfig)
        if (!this.enumOptions.length) {
          this.addEnumOption()
        }
      } else {
        this.enumOptions = []
      }
      this.form = {
        fieldId: row.fieldId,
        projectId: row.projectId || this.projectId,
        fieldKey: row.fieldKey,
        fieldLabel: row.fieldLabel,
        fieldType: row.fieldType,
        maxLength: row.maxLength,
        required: this.isTruthy(row.required) ? 1 : 0,
        nullable: this.isTruthy(row.nullable) ? 1 : 0,
        enabled: this.isTruthy(row.enabled) ? 1 : 0,
        sortOrder: row.sortOrder != null ? row.sortOrder : 0,
        typeConfig: typeConfig,
        defaultValue: row.defaultValue != null ? row.defaultValue : null
      }
      this.open = true
      this.title = this.$t('defect.custom-field.edit-field')
      this.$nextTick(() => {
        this._suppressEnumWatch = false
        if (this.$refs.form) {
          this.$refs.form.clearValidate()
        }
      })
    },
    addEnumOption() {
      this.enumOptions.push({
        key: '',
        label: '',
        color: '#409EFF'
      })
    },
    removeEnumOption(index) {
      this.enumOptions.splice(index, 1)
    },
    buildSubmitPayload() {
      const payload = { ...this.form }
      delete payload.typeConfig
      if (payload.fieldType === 'enum') {
        payload.typeConfig = {
          options: this.enumOptions
            .filter(opt => opt.key || opt.label)
            .map(opt => ({
              key: (opt.key || '').trim(),
              label: (opt.label || '').trim(),
              color: opt.color || undefined
            }))
        }
      } else if (payload.fieldType === 'string' && payload.maxLength == null) {
        payload.maxLength = 255
      } else if (payload.fieldType !== 'string') {
        payload.maxLength = null
      }
      if (!supportsFieldDefaultValue(payload.fieldType)) {
        payload.defaultValue = null
      } else if (payload.defaultValue === '' || payload.defaultValue === undefined) {
        payload.defaultValue = null
      }
      return payload
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        if (this.form.fieldType === 'enum') {
          const validOptions = this.enumOptions.filter(opt => opt.key && opt.label)
          if (!validOptions.length) {
            this.$modal.msgError(this.$t('defect.custom-field.enum-options-required'))
            return
          }
          const keys = validOptions.map(opt => opt.key.trim())
          if (new Set(keys).size !== keys.length) {
            this.$modal.msgError(this.$t('defect.custom-field.enum-option-key-duplicate'))
            return
          }
        }
        const payload = this.buildSubmitPayload()
        const request = payload.fieldId
          ? updateField(this.projectId, payload.fieldId, payload)
          : addField(this.projectId, payload)
        request.then(() => {
          this.$modal.msgSuccess(payload.fieldId ? this.$t('modify-success') : this.$t('create-success'))
          this.open = false
          clearCustomFieldColumnsCache()
          if (this.isTruthy(payload.enabled)) {
            return loadDefectColumnLayout(this.projectId, { force: true }).then(layout => {
              const cols = buildDefectTableColumnDefaults(TableOptions.map(c => ({ ...c })), layout)
              pruneDefectTableColumnCacheFromColumns(this.$cache.local, cols)
              syncNewDefectTableColumnsIntoFieldListCache(this.$cache.local, cols)
            })
          }
        }).then(() => {
          this.getList()
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm(this.$t('defect.custom-field.delete-confirm'))
        .then(() => deleteField(this.projectId, row.fieldId))
        .then(() => {
          clearCustomFieldColumnsCache()
          return listEnabledFields(this.projectId)
        })
        .then(() => loadDefectColumnLayout(this.projectId, { force: true }))
        .then(layout => {
          const cols = buildDefectTableColumnDefaults(TableOptions.map(c => ({ ...c })), layout)
          pruneDefectTableColumnCacheFromColumns(this.$cache.local, cols)
        })
        .then(() => {
          this.getList()
          this.$modal.msgSuccess(this.$t('delete-success'))
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped lang="scss">
.app-container.case-body {
  padding: 20px 20px 0;
  box-sizing: border-box;
}
.defect-field-page.defect-field-list-layout {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  min-height: 0;
  flex: 1 1 auto;
  width: 100%;
  box-sizing: border-box;
}
.defect-field-list-body {
  flex: 1 1 0%;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.defect-field-table-x-scroll {
  width: 100%;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
}
.defect-field-tools {
  flex-shrink: 0;
  width: 100%;
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
  gap: var(--cat2bug-toolbar-item-gap, 10px);
  margin: 0;
}
.defect-field-table-pagination-band {
  flex-shrink: 0;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  margin-top: 4px;
}
.enum-options-table {
  width: 100%;
}
/* 只读/禁用：浅色主题下与可编辑项区分 */
.defect-field-readonly.el-input.is-disabled .el-input__inner,
.defect-field-readonly .el-input.is-disabled .el-input__inner {
  background-color: #f5f7fa;
  border-color: #e4e7ed;
  color: #909399;
  cursor: not-allowed;
}
html.dark .defect-field-source-tag--system {
  border: none;
  background-color: #525252;
  border-color: #525252;
  color: #f5f5f5;
}

html.dark .defect-field-page {
  .defect-field-list-table.el-table {
    th.el-table__cell {
      background-color: var(--table-header-bg, #2b2b2c) !important;
      color: var(--table-header-color, #e5eaf3) !important;
    }
    td.el-table__cell {
      background-color: var(--cat2bug-table-row-bg, #1d1d1d) !important;
      color: var(--text-color-regular, #cfd3dc) !important;
      border-bottom-color: var(--border-color-light, #414243) !important;
    }
    .el-table__row:hover > td.el-table__cell {
      background-color: var(--table-row-hover-bg, #262727) !important;
    }
  }
}

html.dark .defect-field-form-dialog {
  .defect-field-label-input.el-input {
    .el-input__inner {
      background-color: var(--input-bg, #141414) !important;
      border-color: var(--input-border, #4c4d4f) !important;
      color: var(--input-color, #e5eaf3) !important;

      &::placeholder {
        color: var(--text-color-placeholder, #6c6e72) !important;
      }

      &:focus {
        border-color: var(--cat2bug-field-focus-color, #ffc107) !important;
      }
    }

    .el-input__count,
    .el-input__count-inner {
      background: transparent !important;
      color: var(--text-color-secondary, #a3a6ad) !important;
    }

    &.is-exceed .el-input__count,
    &.is-exceed .el-input__count-inner {
      color: var(--color-danger, #f56c6c) !important;
    }
  }

  .defect-field-readonly.el-input.is-disabled .el-input__inner,
  .defect-field-readonly .el-input.is-disabled .el-input__inner {
    background-color: rgba(255, 255, 255, 0.06) !important;
    border-color: var(--border-color-base, #4c4d4f) !important;
    color: var(--text-color-secondary, #a3a6ad) !important;
    -webkit-text-fill-color: var(--text-color-secondary, #a3a6ad) !important;
    box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.15);
  }

  .defect-field-enum-table.el-table--border {
    th.el-table__cell {
      background-color: var(--table-header-bg, #2b2b2c) !important;
      color: var(--table-header-color, #e5eaf3) !important;
    }
    td.el-table__cell {
      background-color: var(--cat2bug-table-row-bg, #1d1d1d) !important;
      border-color: var(--border-color-light, #414243) !important;
    }
    .el-input__inner {
      background-color: var(--input-bg, #141414) !important;
      border-color: var(--input-border, #4c4d4f) !important;
      color: var(--input-color, #e5eaf3) !important;
    }
  }
}

html.dark .defect-field-page .sort-order-arrows .sort-arrow.is-disabled {
  color: var(--text-color-placeholder, #6c6e72);
}
.defect-field-list-table {
  /* 覆盖 ruoyi 表头 word-break: break-word，避免「最大长度」等标题被挤成两行 */
  ::v-deep .el-table__header-wrapper th.el-table__cell,
  ::v-deep .el-table__fixed-header-wrapper th.el-table__cell {
    word-break: keep-all;
    > .cell {
      white-space: nowrap;
      flex-wrap: nowrap;
    }
  }
  ::v-deep td.defect-field-source-col .cell,
  ::v-deep th.defect-field-source-col .cell {
    overflow: visible;
    text-overflow: clip;
  }
  ::v-deep .defect-field-source-tag {
    display: inline-flex;
    max-width: none;
    white-space: nowrap;
    vertical-align: middle;
  }
  ::v-deep td.cat2bug-operate-column .cell,
  ::v-deep th.cat2bug-operate-column .cell {
    overflow: visible;
  }
  ::v-deep .defect-field-operate-tools {
    display: inline-flex;
    flex-wrap: nowrap;
    white-space: nowrap;
    width: max-content;
    max-width: none;
    .el-button--mini {
      width: auto;
      padding-left: 0;
      padding-right: 0;
      white-space: nowrap;
      flex-shrink: 0;
    }
  }
}
.defect-field-operate-tools {
  column-gap: 4px;
  flex-wrap: nowrap;
  white-space: nowrap;
}
.sort-order-arrows {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 2px;
  .sort-arrow {
    font-size: 18px;
    line-height: 1;
    color: #409eff;
    cursor: pointer;
    user-select: none;
    &:hover:not(.is-disabled) {
      color: #66b1ff;
    }
    &.is-disabled {
      color: #c0c4cc;
      cursor: not-allowed;
      pointer-events: none;
    }
  }
}
</style>
