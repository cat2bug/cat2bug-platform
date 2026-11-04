<template>
  <div class="app-container">
    <el-row>
      <el-page-header @back="goBack" :content="$t('admin.team-plan')" />
    </el-row>
    <el-row class="mb8" style="margin-top: 12px;">
      <el-button
        type="primary"
        :icon="$legacyIcon('el-icon-plus')"
        size="small"
        @click="handleAdd"
        v-hasPermi="['admin:team-plan:add']"
      >{{ $t('add') }}</el-button>
    </el-row>
    <el-table v-loading="loading" :data="planList">
      <el-table-column :label="$t('admin.team-plan.name')" prop="planName" min-width="140" />
      <el-table-column :label="$t('admin.team-plan.max-members')" prop="maxMembers" width="120" align="center" />
      <el-table-column :label="$t('admin.team-plan.max-projects')" prop="maxProjects" width="120" align="center" />
      <el-table-column :label="$t('admin.team-plan.max-project-space')" width="160" align="center">
        <template #default="scope">
          {{ formatBytes(scope.row.maxProjectBytes) }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('admin.team-plan.system-default')" width="100" align="center">
        <template #default="scope">
          <el-tag v-if="scope.row.systemDefault" type="success" size="small">{{ $t('yes') }}</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('operate')" width="100" align="center">
        <template #default="scope">
          <el-button
            link
            size="small"
            :icon="$legacyIcon('el-icon-edit')"
            @click="handleEdit(scope.row)"
            v-hasPermi="['admin:team-plan:edit']"
          >{{ $t('modify') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="title" v-model="open" width="480px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="140px">
        <el-form-item :label="$t('admin.team-plan.name')" prop="planName">
          <el-input v-model="form.planName" maxlength="64" />
        </el-form-item>
        <el-form-item :label="$t('admin.team-plan.max-members')" prop="maxMembers">
          <el-input-number v-model="form.maxMembers" :min="1" :max="100000" />
        </el-form-item>
        <el-form-item :label="$t('admin.team-plan.max-projects')" prop="maxProjects">
          <el-input-number v-model="form.maxProjects" :min="1" :max="100000" />
        </el-form-item>
        <el-form-item :label="$t('admin.team-plan.max-project-space-gb')" prop="maxProjectGb">
          <el-input-number v-model="form.maxProjectGb" :min="0.1" :max="1024" :step="0.5" :precision="1" />
        </el-form-item>
        <el-form-item :label="$t('admin.team-plan.system-default')">
          <el-switch v-model="form.systemDefault" />
        </el-form-item>
        <el-form-item :label="$t('remark')">
          <el-input v-model="form.remark" type="textarea" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">{{ $t('cancel') }}</el-button>
        <el-button type="primary" @click="submitForm">{{ $t('confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listTeamPlan, addTeamPlan, updateTeamPlan } from '@/api/admin/team-plan'

const GIB = 1073741824

export default {
  name: 'AdminTeamPlan',
  data() {
    return {
      loading: true,
      planList: [],
      open: false,
      title: '',
      form: {},
      rules: {
        planName: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
        maxMembers: [{ required: true, message: '必填', trigger: 'change' }],
        maxProjects: [{ required: true, message: '必填', trigger: 'change' }],
        maxProjectGb: [{ required: true, message: '必填', trigger: 'change' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    goBack() {
      this.$router.back()
    },
    formatBytes(bytes) {
      if (bytes == null) return '—'
      const gib = bytes / GIB
      if (gib >= 1) return gib.toFixed(gib % 1 === 0 ? 0 : 1) + ' GB'
      const mib = bytes / (1024 * 1024)
      return mib.toFixed(mib % 1 === 0 ? 0 : 1) + ' MB'
    },
    getList() {
      this.loading = true
      listTeamPlan().then(res => {
        this.planList = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    reset() {
      this.form = {
        planId: null,
        planName: '',
        maxMembers: 10,
        maxProjects: 20,
        maxProjectGb: 1,
        systemDefault: false,
        remark: ''
      }
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = this.$t('admin.team-plan.add')
    },
    handleEdit(row) {
      this.reset()
      this.form = {
        planId: row.planId,
        planName: row.planName,
        maxMembers: row.maxMembers,
        maxProjects: row.maxProjects,
        maxProjectGb: row.maxProjectBytes ? Number((row.maxProjectBytes / GIB).toFixed(1)) : 1,
        systemDefault: !!row.systemDefault,
        remark: row.remark
      }
      this.open = true
      this.title = this.$t('admin.team-plan.edit')
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const payload = {
          planId: this.form.planId,
          planName: this.form.planName,
          maxMembers: this.form.maxMembers,
          maxProjects: this.form.maxProjects,
          maxProjectBytes: Math.round(this.form.maxProjectGb * GIB),
          systemDefault: this.form.systemDefault,
          remark: this.form.remark
        }
        const req = this.form.planId ? updateTeamPlan(payload) : addTeamPlan(payload)
        req.then(() => {
          this.$modal.msgSuccess(this.$t('success'))
          this.open = false
          this.getList()
        })
      })
    }
  }
}
</script>
