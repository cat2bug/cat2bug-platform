<template>
  <div class="cat2bug-defect-table-block">
    <div class="cat2bug-defect-table-block__header">
      <svg-icon icon-class="bug" />
      <span>{{ $t('defect.table') }}</span>
      <el-popover placement="bottom" :width="360" trigger="click">
        <template #reference>
          <el-button size="small" text type="primary">{{ $t('edit') }}</el-button>
        </template>
        <div class="cat2bug-defect-table-block__columns">
          <el-checkbox-group v-model="selectedFields" @change="onColumnsChange">
            <el-checkbox
              v-for="field in allFields"
              :key="field"
              :label="field"
            >
              {{ field }}
            </el-checkbox>
          </el-checkbox-group>
        </div>
      </el-popover>
      <el-button size="small" text type="danger" @click="$emit('remove')">
        {{ $t('delete') }}
      </el-button>
    </div>
    <div class="cat2bug-defect-table-block__preview">
      <code>{{ previewToken }}</code>
    </div>
  </div>
</template>

<script>
import { DEFECT_TABLE_DEFAULT_COLUMNS, formatTableColumns } from '../dsl/grammar'

export default {
  name: 'DefectTableBlock',
  props: {
    dataSource: {
      type: String,
      default: 'api.defect.list'
    },
    columns: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:columns', 'remove'],
  data() {
    return {
      allFields: [...DEFECT_TABLE_DEFAULT_COLUMNS],
      selectedFields: []
    }
  },
  computed: {
    previewToken() {
      const cols = this.columns.length ? this.columns : DEFECT_TABLE_DEFAULT_COLUMNS.map((f) => ({ field: f, label: null }))
      return `$table{${this.dataSource}[${formatTableColumns(cols)}]}`
    }
  },
  watch: {
    columns: {
      immediate: true,
      handler(cols) {
        this.selectedFields = (cols.length ? cols : DEFECT_TABLE_DEFAULT_COLUMNS.map((f) => ({ field: f })))
          .map((c) => c.field)
      }
    }
  },
  methods: {
    onColumnsChange(fields) {
      const ordered = this.allFields.filter((f) => fields.includes(f))
      const columns = ordered.map((field) => {
        const prev = this.columns.find((c) => c.field === field)
        return prev ? { ...prev } : { field, label: null }
      })
      this.$emit('update:columns', columns)
    }
  }
}
</script>

<style scoped>
.cat2bug-defect-table-block {
  border: 1px dashed var(--el-color-primary-light-5);
  border-radius: 6px;
  padding: 8px 12px;
  margin: 8px 0;
  background: var(--el-fill-color-lighter);
}

.cat2bug-defect-table-block__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.cat2bug-defect-table-block__preview code {
  font-size: 12px;
  word-break: break-all;
  color: var(--el-text-color-secondary);
}

.cat2bug-defect-table-block__columns {
  max-height: 280px;
  overflow: auto;
}

.cat2bug-defect-table-block__columns .el-checkbox {
  display: block;
  margin: 4px 0;
}
</style>
