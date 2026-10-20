<template>
  <div class="app-container project-list-page-host">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('case.type.manage')" />
    </el-row>
    <div class="list-page-toolbar">
      <div class="list-page-toolbar__filters" />
      <div class="list-page-toolbar__actions">
        <el-button
          v-hasPermi="['system:project:case-type:add']"
          type="primary"
          plain
          class="cat2bug-btn-outline"
          :icon="$legacyIcon('el-icon-plus')"
          @click="handleAdd"
        >{{ $t('case.type.add') }}</el-button>
      </div>
    </div>
    <cat2-bug-table
      ref="cat2BugTable"
      cache-key="case-types-table"
      row-key="typeId"
      :columns="caseTypeTableColumns"
      :data="types"
      :loading="loading"
      :enable-header-sort="false"
      :operate-column-max-width="200"
    >
      <template #columns="{ scope, column }">
        <el-tooltip
          v-if="column.prop === 'enabled'"
          :content="usedTooltip(scope.row)"
          placement="top"
        >
          <el-switch
            :model-value="isEnabled(scope.row)"
            :disabled="!canEdit"
            @change="(val) => handleEnabledChange(scope.row, val)"
            @click.stop
          />
        </el-tooltip>
        <span v-else>{{ scope.row[column.prop] }}</span>
      </template>
      <template #append>
        <el-table-column
          :label="$t('operate')"
          align="left"
          class-name="no-drag cat2bug-operate-column"
        >
          <template #default="scope">
            <div class="cat2bug-operate-tools" @click.stop>
              <el-button
                v-hasPermi="['system:project:case-type:edit']"
                link
                size="small"
                class="green"
                :icon="$legacyIcon('el-icon-edit')"
                @click.stop="handleRename(scope.row)"
              >{{ $t('modify') }}</el-button>
              <el-button
                v-hasPermi="['system:project:case-type:remove']"
                link
                size="small"
                class="red"
                :icon="$legacyIcon('el-icon-delete')"
                @click.stop="handleDelete(scope.row)"
              >{{ $t('delete') }}</el-button>
            </div>
          </template>
        </el-table-column>
      </template>
    </cat2-bug-table>
  </div>
</template>

<script>
import { ElMessageBox } from 'element-plus'
import Cat2BugTable from '@/components/Cat2BugTable'
import { addCaseType, deleteCaseType, listCaseTypes, updateCaseType, updateCaseTypeEnabled } from '@/api/system/case-type'
import { checkPermi } from '@/utils/permission'
import { useUserStore } from '@/stores/user'

const CASE_TYPE_TABLE_COLUMNS = [
  {
    key: 'case.type.name',
    prop: 'typeName',
    fixed: false,
    visible: true,
    align: 'left',
    minWidth: 200,
    pinFixedToggle: false,
    sortable: false
  },
  {
    key: 'case.type.used',
    prop: 'enabled',
    fixed: false,
    visible: true,
    align: 'center',
    width: 120,
    pinFixedToggle: false,
    sortable: false
  }
]

export default {
  name: 'ProjectCaseTypes',
  components: { Cat2BugTable },
  data() {
    return {
      loading: false,
      types: [],
      caseTypeTableColumns: CASE_TYPE_TABLE_COLUMNS.map((c) => ({ ...c }))
    }
  },
  computed: {
    projectId() {
      return parseInt(useUserStore().config.currentProjectId)
    },
    canEdit() {
      return checkPermi(['system:project:case-type:edit'])
    }
  },
  created() {
    this.load()
  },
  methods: {
    goBack() {
      this.$router.push('/project/option')
    },
    isEnabled(row) {
      return row != null && Number(row.enabled) === 1
    },
    usedTooltip(row) {
      const n = row && row.caseCount ? Number(row.caseCount) : 0
      return this.$t('case.type.used-count', { count: n })
    },
    load() {
      this.loading = true
      listCaseTypes(this.projectId).then(res => {
        this.types = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleEnabledChange(row, enabled) {
      const prev = row.enabled
      const next = enabled ? 1 : 0
      row.enabled = next
      updateCaseTypeEnabled(this.projectId, row.typeId, next).catch(() => {
        row.enabled = prev
      })
    },
    handleAdd() {
      ElMessageBox.prompt(this.$t('case.type.name'), this.$t('case.type.add'), {
        inputPattern: /\S+/,
        inputErrorMessage: this.$t('case.type.name'),
        closeOnClickModal: false
      }).then(({ value }) => addCaseType(this.projectId, value.trim())).then(() => this.load()).catch(() => {})
    },
    handleRename(row) {
      ElMessageBox.prompt(this.$t('case.type.name'), this.$t('modify'), {
        inputValue: row.typeName,
        inputPattern: /\S+/,
        inputErrorMessage: this.$t('case.type.name'),
        closeOnClickModal: false
      }).then(({ value }) => updateCaseType(this.projectId, row.typeId, value.trim())).then(() => this.load()).catch(() => {})
    },
    handleDelete(row) {
      this.$modal.confirm(this.$t('delete')).then(() => deleteCaseType(this.projectId, row.typeId)).then(() => this.load()).catch(() => {})
    }
  }
}
</script>
