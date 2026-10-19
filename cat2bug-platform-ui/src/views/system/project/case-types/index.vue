<template>
  <div class="app-container project-list-page-host">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('case.type.manage')" />
    </el-row>
    <div class="list-page-toolbar">
      <div class="list-page-toolbar__filters" />
      <div class="list-page-toolbar__actions">
        <el-button v-hasPermi="['system:project:case-type:add']" type="primary" size="small" :icon="$legacyIcon('el-icon-plus')" @click="handleAdd">{{ $t('case.type.add') }}</el-button>
      </div>
    </div>
    <el-table v-loading="loading" :data="types">
      <el-table-column :label="$t('case.type.name')" prop="typeName" />
      <el-table-column :label="$t('case.type.used')" prop="caseCount" width="120" />
      <el-table-column :label="$t('operate')" width="180">
        <template #default="scope">
          <el-button v-hasPermi="['system:project:case-type:edit']" link size="small" @click="handleRename(scope.row)">{{ $t('modify') }}</el-button>
          <el-button v-hasPermi="['system:project:case-type:remove']" link size="small" class="red" @click="handleDelete(scope.row)">{{ $t('delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import { addCaseType, deleteCaseType, listCaseTypes, updateCaseType } from '@/api/system/case-type'
import { useUserStore } from '@/stores/user'

export default {
  name: 'ProjectCaseTypes',
  data() {
    return {
      loading: false,
      types: []
    }
  },
  computed: {
    projectId() {
      return parseInt(useUserStore().config.currentProjectId)
    }
  },
  created() {
    this.load()
  },
  methods: {
    goBack() {
      this.$router.push('/project/option')
    },
    load() {
      this.loading = true
      listCaseTypes(this.projectId).then(res => {
        this.types = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleAdd() {
      this.$prompt(this.$t('case.type.name'), this.$t('case.type.add'), {
        inputPattern: /\S+/,
        inputErrorMessage: this.$t('case.type.name')
      }).then(({ value }) => addCaseType(this.projectId, value.trim())).then(() => this.load())
    },
    handleRename(row) {
      this.$prompt(this.$t('case.type.name'), this.$t('modify'), {
        inputValue: row.typeName,
        inputPattern: /\S+/,
        inputErrorMessage: this.$t('case.type.name')
      }).then(({ value }) => updateCaseType(this.projectId, row.typeId, value.trim())).then(() => this.load())
    },
    handleDelete(row) {
      this.$modal.confirm(this.$t('delete')).then(() => deleteCaseType(this.projectId, row.typeId)).then(() => this.load())
    }
  }
}
</script>
