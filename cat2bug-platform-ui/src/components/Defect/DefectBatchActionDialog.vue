<template>
  <el-dialog
    :title="dialogTitle"
    v-model="dialogVisible"
    :close-on-press-escape="!submitting"
    :close-on-click-modal="false"
    :before-close="onBeforeClose"
    append-to-body
    width="780px"
    class="defect-batch-action-dialog"
  >
    <div class="defect-batch-bulk">
      <div v-if="needsMember" class="defect-batch-bulk-row">
        <span class="defect-batch-bulk-label">{{ memberFieldLabel }}</span>
        <select-project-member
          :key="'bulk-member-' + action + '-' + openSeq"
          ref="bulkMember"
          v-model:memberId="bulkReceiveBy"
          :project-id="projectId"
          :is-head="false"
          :multiple="memberMultiple"
          size="small"
        />
        <el-button size="small" @click="applyMemberToAll">{{ $t('defect.batch-apply-all') }}</el-button>
      </div>
      <div class="defect-batch-bulk-row">
        <span class="defect-batch-bulk-label">{{ $t('describe') }}</span>
        <el-input
          v-model="bulkDescribe"
          type="textarea"
          :rows="2"
          maxlength="255"
          show-word-limit
          :placeholder="$t('defect.enter-describe')"
          class="defect-batch-bulk-describe"
        />
        <el-button size="small" @click="applyDescribeToAll">{{ $t('defect.batch-apply-all') }}</el-button>
      </div>
    </div>

    <el-table
      :data="rows"
      border
      size="small"
      max-height="360"
      class="defect-batch-rows-table"
      row-key="defectId"
    >
      <el-table-column label="#" width="72" align="center">
        <template #default="{ row }">#{{ row.projectNum }}</template>
      </el-table-column>
      <el-table-column :label="$t('defect.name')" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.defectName }}</template>
      </el-table-column>
      <el-table-column v-if="needsMember" :label="memberFieldLabel" min-width="180">
        <template #default="{ row }">
          <select-project-member
            :key="'row-member-' + action + '-' + row.defectId + '-' + openSeq"
            v-model:memberId="row.receiveBy"
            :project-id="projectId"
            :is-head="false"
            :multiple="memberMultiple"
            size="small"
          />
        </template>
      </el-table-column>
      <el-table-column :label="$t('describe')" min-width="200">
        <template #default="{ row }">
          <el-input
            v-model="row.defectLogDescribe"
            type="textarea"
            :rows="2"
            maxlength="255"
            show-word-limit
            :placeholder="$t('defect.enter-describe')"
          />
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <div class="dialog-footer">
        <el-button :disabled="submitting" @click="requestClose">{{ $t('cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ $t('submit') }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import SelectProjectMember from '@/components/Project/SelectProjectMember'
import { assign, reject, repair, pass, close, open } from '@/api/system/defect'
import { listLog } from '@/api/system/log'
import { strFormat } from '@/utils/index'

const ACTION_META = {
  assign: {
    titleKey: 'assign',
    member: true,
    memberMultiple: true,
    memberLabelKey: 'defect.assigned-to',
    describeRequired: false,
  },
  repair: {
    titleKey: 'repair',
    member: true,
    memberMultiple: false,
    memberLabelKey: 'defect.reviewed-by',
    describeRequired: false,
  },
  reject: {
    titleKey: 'reject',
    member: false,
    describeRequired: true,
  },
  pass: {
    titleKey: 'pass',
    member: false,
    describeRequired: false,
  },
  close: {
    titleKey: 'close',
    member: false,
    describeRequired: true,
  },
  open: {
    titleKey: 'open',
    member: false,
    describeRequired: true,
  },
}

export default {
  name: 'DefectBatchActionDialog',
  emits: ['done'],
  components: { SelectProjectMember },
  props: {
    projectId: {
      type: Number,
      default: null,
    },
  },
  data() {
    return {
      dialogVisible: false,
      submitting: false,
      action: null,
      rows: [],
      bulkReceiveBy: [],
      bulkDescribe: '',
      openSeq: 0,
    }
  },
  computed: {
    meta() {
      return (this.action && ACTION_META[this.action]) || {}
    },
    dialogTitle() {
      if (!this.meta.titleKey) return this.$t('defect.batch-action')
      return `${this.$t('defect.batch-action')} · ${this.$t(this.meta.titleKey)}`
    },
    needsMember() {
      return !!this.meta.member
    },
    memberMultiple() {
      return this.meta.memberMultiple !== false
    },
    memberFieldLabel() {
      return this.meta.memberLabelKey ? this.$t(this.meta.memberLabelKey) : ''
    },
  },
  methods: {
    open(action, defects) {
      if (!action || !ACTION_META[action] || !defects || !defects.length) return
      this.action = action
      this.openSeq += 1
      this.bulkReceiveBy = []
      this.bulkDescribe = ''
      this.rows = defects.map((d) => ({
        defectId: d.defectId,
        projectNum: d.projectNum,
        defectName: d.defectName,
        receiveBy: [],
        defectLogDescribe: '',
      }))
      this.dialogVisible = true
      if (action === 'repair') {
        this.prefetchRepairReviewers()
      }
    },
    prefetchRepairReviewers() {
      this.rows.forEach((row) => {
        listLog({
          pageNum: 1,
          pageSize: 1,
          defectId: row.defectId,
          orderByColumn: 'createTime',
          isAsc: 'desc',
        }).then((res) => {
          const log = res.rows && res.rows[0]
          if (log && log.createBy != null && log.createBy !== '') {
            row.receiveBy = [parseInt(log.createBy, 10)]
          }
        }).catch(() => {})
      })
    },
    applyMemberToAll() {
      const ids = Array.isArray(this.bulkReceiveBy)
        ? [...this.bulkReceiveBy]
        : this.bulkReceiveBy != null && this.bulkReceiveBy !== ''
          ? [this.bulkReceiveBy]
          : []
      this.rows.forEach((row) => {
        row.receiveBy = [...ids]
      })
      this.openSeq += 1
    },
    applyDescribeToAll() {
      const text = this.bulkDescribe == null ? '' : String(this.bulkDescribe)
      this.rows.forEach((row) => {
        row.defectLogDescribe = text
      })
    },
    validateRows() {
      for (const row of this.rows) {
        if (this.needsMember) {
          const rb = row.receiveBy
          const empty = !rb || (Array.isArray(rb) && rb.length === 0)
          if (empty) {
            this.$modal.msgWarning(
              strFormat(
                this.$t('defect.batch-row-member-required'),
                row.projectNum
              )
            )
            return false
          }
        }
        if (this.meta.describeRequired) {
          const desc = row.defectLogDescribe
          if (desc == null || String(desc).trim() === '') {
            this.$modal.msgWarning(
              strFormat(
                this.$t('defect.batch-row-describe-required'),
                row.projectNum
              )
            )
            return false
          }
        }
      }
      return true
    },
    buildPayload(row) {
      const payload = {
        defectId: row.defectId,
        defectLogDescribe: row.defectLogDescribe || '',
      }
      if (this.needsMember) {
        let rb = row.receiveBy
        if (!Array.isArray(rb)) {
          rb = rb != null && rb !== '' ? [rb] : []
        }
        payload.receiveBy = rb
      }
      return payload
    },
    callApi(row) {
      const payload = this.buildPayload(row)
      const id = row.defectId
      switch (this.action) {
        case 'assign':
          return assign(id, payload)
        case 'repair':
          return repair(id, payload)
        case 'reject':
          return reject(id, payload)
        case 'pass':
          return pass(id, payload)
        case 'close':
          return close(id, payload)
        case 'open':
          return open(id, payload)
        default:
          return Promise.reject(new Error('unknown action'))
      }
    },
    async onSubmit() {
      if (this.submitting) return
      if (!this.validateRows()) return
      this.submitting = true
      let ok = 0
      const failures = []
      for (const row of this.rows) {
        try {
          await this.callApi(row)
          ok += 1
        } catch (e) {
          const msg =
            (e && e.message) ||
            (e && e.msg) ||
            String(e || this.$t('fail'))
          failures.push(`#${row.projectNum}: ${msg}`)
        }
      }
      this.submitting = false
      this.dialogVisible = false
      this.$emit('done', { ok, fail: failures.length, failures, action: this.action })
      if (failures.length === 0) {
        this.$modal.msgSuccess(
          strFormat(this.$t('defect.batch-result-all-ok'), ok)
        )
      } else {
        this.$modal.msgWarning(
          strFormat(this.$t('defect.batch-result-partial'), ok, failures.length) +
            (failures.length ? `\n${failures.slice(0, 5).join('\n')}` : '')
        )
      }
    },
    requestClose() {
      if (this.submitting) return
      this.dialogVisible = false
    },
    onBeforeClose(done) {
      if (this.submitting) return
      done()
    },
  },
}
</script>

<style lang="scss" scoped>
.defect-batch-bulk {
  margin-bottom: 12px;
  padding: 10px 12px;
  background: var(--bg-color-base, #f5f7fa);
  border: 1px solid var(--border-color-lighter, #ebeef5);
  border-radius: 4px;
}
.defect-batch-bulk-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
  &:last-child {
    margin-bottom: 0;
  }
}
.defect-batch-bulk-label {
  flex: 0 0 72px;
  line-height: 32px;
  color: var(--form-label-color, var(--text-color-regular));
  font-size: 13px;
}
.defect-batch-bulk-describe {
  flex: 1;
  min-width: 0;
}
.defect-batch-rows-table {
  width: 100%;
  :deep(.el-table__cell) {
    vertical-align: top;
  }
}
</style>
