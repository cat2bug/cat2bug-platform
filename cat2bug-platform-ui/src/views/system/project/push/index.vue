<template>
  <div class="app-container" ref="projectOptionSubMain">
    <el-row class="project-add-page-header project-option-sub-hint-back">
      <el-page-header @back="goBack" :content="$t('project.push')">
      </el-page-header>
    </el-row>
    <el-alert
      class="project-push-hint"
      type="info"
      :closable="false"
      show-icon
      :title="$t('project.push.package-hint')"
    />
    <el-alert
      v-if="restored"
      class="project-push-hint"
      type="success"
      :closable="false"
      show-icon
      :title="$t('project.push.restored')"
    />
    <el-form ref="form" class="project-push-form" :model="form" :rules="rules" label-width="120px">
      <el-form-item :label="$t('project.push.key')" prop="pullKey">
        <el-input
          v-model="form.pullKey"
          clearable
          :disabled="submitting || selectionLocked"
          :placeholder="$t('project.push.key-placeholder')"
        />
        <div class="project-push-key-hint">{{ $t('project.push.key-hint') }}</div>
      </el-form-item>
      <el-form-item :label="$t('project.push.include')">
        <push-selection
          :loading="estimateLoading"
          :error="estimateError"
          :locked="selectionLocked"
          :required-items="estimate.required"
          :repositories="estimate.repositories"
          :documents="estimate.documents"
          v-model:checked-git-slugs="checkedGitSlugs"
          v-model:checked-doc-ids="checkedDocIds"
          :total-bytes="totalBytes"
          :max-total-bytes="estimate.maxTotalBytes"
          :over-limit="overLimit"
          :required-over-limit="requiredOverLimit"
          :can-remove-largest="canRemoveLargest"
          @remove-largest="onRemoveLargest"
          @reload="loadEstimate"
        />
      </el-form-item>
      <el-form-item v-if="jobVisible" :label="$t('project.push.progress')">
        <div class="project-push-progress">
          <div class="project-push-progress__meta">
            <span>{{ job.phase || $t('project.push.submitting') }}</span>
            <span v-if="job.parts && job.parts.length">{{ donePartCount }}/{{ job.parts.length }}</span>
          </div>
          <el-progress
            :percentage="displayPercent"
            :status="progressStatus"
            :stroke-width="8"
          />
          <el-alert
            v-if="jobFailed && failureReason"
            class="project-push-error"
            type="error"
            :closable="false"
            show-icon
            :title="failureReason"
          />
          <push-part-progress
            class="project-push-parts"
            :parts="job.parts || []"
            :pending="jobRunning"
            :can-retry="canRetry"
            :retrying="submitting && jobFailed"
            @retry="onRetryFailed"
          />
        </div>
      </el-form-item>
      <el-form-item v-if="jobVisible" :label="$t('project.push.logs')">
        <pre ref="logPanel" class="project-push-logs">{{ logText }}</pre>
      </el-form-item>
      <el-form-item class="page-form-actions">
        <div class="page-form-actions__buttons">
          <el-button @click="goBack" :disabled="submitting">{{ $t('cancel') }}</el-button>
          <el-button
            class="defect-kbd-hint-host"
            type="primary"
            :loading="submitting && !job.jobId"
            :disabled="syncDisabled"
            @click="onSubmit"
          >
            {{ syncButtonLabel }}
            <span v-show="fieldHintsActive && !syncDisabled" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
          </el-button>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { estimatePush, createPushJob, getPushJobById, retryFailedPushJob, getActivePushJob } from '@/api/system/project'
import projectOptionSubFormKbd from '@/mixins/project-option-sub-form-kbd'
import PushSelection from './PushSelection.vue'
import PushPartProgress from './PushPartProgress.vue'
import {
  DEFAULT_MAX_TOTAL_BYTES,
  REQUIRED_KEYS,
  normalizeEstimate,
  normalizeJob,
  selectedTotal,
  removeLargestOptional,
  selectableCheckedDocIds,
  buildPushJobBody,
  isRunningJob,
  isUnfinishedJob,
  jobHasFailedParts,
  progressPercent
} from './push-estimate'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'

function emptyEstimate() {
  return {
    maxTotalBytes: DEFAULT_MAX_TOTAL_BYTES,
    required: REQUIRED_KEYS.map((key) => ({ key, bytes: null })),
    repositories: [],
    documents: []
  }
}

function emptyJob() {
  return {
    jobId: '',
    state: '',
    percent: 0,
    phase: '',
    logs: [],
    error: '',
    message: '',
    parts: [],
    selection: null
  }
}

export default {
  name: 'ProjectPush',
  components: { PushSelection, PushPartProgress },
  mixins: [projectOptionSubFormKbd],
  setup() {
    const userStore = useUserStore()
    const { config } = storeToRefs(userStore)
    return { config }
  },
  data() {
    return {
      estimateLoading: false,
      estimateError: '',
      estimate: emptyEstimate(),
      checkedGitSlugs: [],
      checkedDocIds: [],
      restored: false,
      loadSeq: 0,
      submitting: false,
      syncSucceeded: false,
      jobVisible: false,
      pollTimer: null,
      job: emptyJob(),
      form: { pullKey: '' },
      rules: {
        pullKey: [
          { required: true, message: this.$i18n.t('project.push.key-cannot-empty'), trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    projectId() {
      return this.config.currentProjectId
    },
    totalBytes() {
      return selectedTotal({
        required: this.estimate.required,
        silentBytes: this.estimate.silentBytes,
        repositories: this.estimate.repositories,
        documents: this.estimate.documents,
        checkedGitSlugs: this.checkedGitSlugs,
        checkedDocIds: this.checkedDocIds
      })
    },
    requiredBytes() {
      return (this.estimate.required || []).reduce((sum, item) => sum + (Number(item.bytes) || 0), 0)
    },
    overLimit() {
      return this.totalBytes > this.estimate.maxTotalBytes
    },
    requiredOverLimit() {
      return this.requiredBytes > this.estimate.maxTotalBytes
    },
    canRemoveLargest() {
      return this.overLimit && !this.selectionLocked && (this.checkedGitSlugs.length > 0 || this.checkedDocIds.length > 0)
    },
    jobFailed() {
      const state = String(this.job.state || '').toUpperCase()
      return state === 'FAILED' || state === 'PARTIAL' || jobHasFailedParts(this.job)
    },
    jobRunning() {
      return isRunningJob(this.job)
    },
    selectionLocked() {
      return this.submitting || (this.jobVisible && !!this.job.jobId && !this.jobSucceeded)
    },
    logText() {
      return (this.job.logs || []).map((row) => {
        const time = (row.time || '').replace('T', ' ').replace(/\.\d+Z?$/, '')
        return '[' + time + '] ' + (row.level || 'INFO') + ' ' + (row.message || '')
      }).join('\n')
    },
    jobSucceeded() {
      return this.syncSucceeded || ['SUCCESS', 'SUCCEEDED', 'COMPLETED', 'DONE'].includes(String(this.job.state || '').toUpperCase())
    },
    displayPercent() {
      return progressPercent(this.job)
    },
    donePartCount() {
      return (this.job.parts || []).filter((part) => ['OK', 'SUCCESS', 'SUCCEEDED'].includes(String(part.state || '').toUpperCase())).length
    },
    progressStatus() {
      if (this.jobSucceeded) return 'success'
      if (String(this.job.state || '').toUpperCase() === 'FAILED' || (this.jobFailed && !isRunningJob(this.job))) return 'exception'
      return undefined
    },
    failureReason() {
      return (this.job.error || this.job.message || '').trim()
    },
    canRetry() {
      return this.jobVisible && !!this.job.jobId && !this.submitting && !isRunningJob(this.job) && !this.jobSucceeded
        && (jobHasFailedParts(this.job) || String(this.job.state || '').toUpperCase() === 'FAILED')
    },
    syncDisabled() {
      return !this.projectId || this.submitting || this.jobSucceeded || this.estimateLoading || !!this.estimateError
        || this.overLimit || this.selectionLocked
    },
    syncButtonLabel() {
      if (this.submitting) return this.$t('project.push.submitting')
      if (this.jobSucceeded) return this.$t('project.push.done')
      return this.$t('sync')
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler(id) {
        if (id) this.bootstrap()
      }
    }
  },
  mounted() {
    this.$nextTick(() => this.capturePageFormCloseBaseline())
  },
  beforeUnmount() {
    this.clearPoll()
  },
  methods: {
    shortcutSave() {
      if (this.syncDisabled) return
      this.onSubmit()
    },
    serializePageFormCloseState() {
      return JSON.stringify({ form: { ...this.form } })
    },
    bootstrap() {
      this.clearPoll()
      this.submitting = false
      this.syncSucceeded = false
      this.jobVisible = false
      this.restored = false
      this.checkedGitSlugs = []
      this.checkedDocIds = []
      this.job = emptyJob()
      const seq = ++this.loadSeq
      this.loadEstimate(seq)
    },
    loadEstimate(seq) {
      const token = seq == null ? this.loadSeq : seq
      this.estimateLoading = true
      this.estimateError = ''
      estimatePush(this.projectId).then((res) => {
        if (token !== this.loadSeq) return
        this.estimate = normalizeEstimate(res)
        this.estimateLoading = false
        this.restoreActiveJob(token)
      }).catch((err) => {
        if (token !== this.loadSeq) return
        this.estimate = emptyEstimate()
        this.estimateError = (err && (err.msg || err.message)) || this.$t('project.push.estimate-failed')
        this.estimateLoading = false
        this.restoreActiveJob(token)
      })
    },
    restoreActiveJob(seq) {
      getActivePushJob(this.projectId).then((res) => {
        if (seq !== this.loadSeq) return
        const job = normalizeJob(res)
        if (!job || !isUnfinishedJob(job)) return
        this.restored = true
        this.jobVisible = true
        this.applyJob(job)
        if (job.selection) {
          this.checkedGitSlugs = job.selection.gitSlugs.slice()
          this.checkedDocIds = selectableCheckedDocIds(this.estimate.documents, job.selection.documentIds)
        }
        if (isRunningJob(job)) {
          this.submitting = true
          this.pollJob(job.jobId)
        }
      }).catch(() => {})
    },
    onRemoveLargest() {
      const next = removeLargestOptional({
        repositories: this.estimate.repositories,
        documents: this.estimate.documents,
        checkedGitSlugs: this.checkedGitSlugs,
        checkedDocIds: this.checkedDocIds
      })
      this.checkedGitSlugs = next.checkedGitSlugs
      this.checkedDocIds = next.checkedDocIds
    },
    clearPoll() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },
    scrollLogs() {
      this.$nextTick(() => {
        const el = this.$refs.logPanel
        if (el) el.scrollTop = el.scrollHeight
      })
    },
    applyJob(data) {
      if (!data) return
      const state = data.state != null ? String(data.state) : (this.job.state || '')
      this.job = {
        jobId: data.jobId || this.job.jobId,
        state,
        percent: data.percent != null ? data.percent : this.job.percent,
        phase: data.phase || this.job.phase || '',
        logs: Array.isArray(data.logs) ? data.logs : (this.job.logs || []),
        error: data.error || '',
        message: data.message || '',
        parts: data.parts == null ? (this.job.parts || []) : data.parts,
        selection: data.selection || this.job.selection
      }
      if (['SUCCESS', 'SUCCEEDED', 'COMPLETED', 'DONE'].includes(String(state).toUpperCase())) {
        this.syncSucceeded = true
      }
      this.scrollLogs()
    },
    markSyncSuccess(message) {
      this.syncSucceeded = true
      this.submitting = false
      this.job.state = 'SUCCESS'
      this.job.percent = 100
      if (!this.job.phase) this.job.phase = this.$t('project.push.done')
      this.$message.success(message || this.$t('project.push.success'))
    },
    pollJob(jobId) {
      this.clearPoll()
      this.pollTimer = setInterval(() => {
        getPushJobById(jobId).then((res) => {
          const data = normalizeJob(res)
          if (!data) return
          this.applyJob(data)
          const state = String(data.state || '').toUpperCase()
          if (['SUCCESS', 'SUCCEEDED', 'COMPLETED', 'DONE'].includes(state)) {
            this.clearPoll()
            this.markSyncSuccess(data.message)
          } else if (!isRunningJob(data) && (state === 'FAILED' || state === 'PARTIAL' || jobHasFailedParts(data))) {
            this.clearPoll()
            this.submitting = false
            this.$message.error(data.error || this.$t('project.push.failed'))
          }
        }).catch((err) => {
          this.clearPoll()
          this.submitting = false
          this.job.state = 'FAILED'
          this.job.error = (err && (err.msg || err.message)) || this.$t('project.push.failed')
          this.$message.error(this.job.error)
        })
      }, 800)
    },
    onSubmit() {
      this.$refs.form.validate((valid) => {
        if (!valid || this.syncDisabled) return
        const docIds = selectableCheckedDocIds(this.estimate.documents, this.checkedDocIds)
        this.checkedDocIds = docIds
        this.submitting = true
        this.syncSucceeded = false
        this.restored = false
        this.jobVisible = true
        this.job = {
          ...emptyJob(),
          state: 'RUNNING',
          phase: this.$t('project.push.submitting')
        }
        createPushJob(this.projectId, buildPushJobBody({
          pullKey: this.form.pullKey,
          gitSlugs: this.checkedGitSlugs,
          documentIds: docIds
        })).then((res) => {
          this.followJob(normalizeJob(res))
        }).catch(() => {
          this.submitting = false
          if (!this.job.jobId) {
            this.jobVisible = false
            this.job = emptyJob()
          }
        })
      })
    },
    onRetryFailed() {
      if (!this.canRetry) return
      this.submitting = true
      this.job.phase = this.$t('project.push.retrying')
      retryFailedPushJob(this.job.jobId).then((res) => {
        this.followJob(normalizeJob(res))
      }).catch(() => {
        this.submitting = false
      })
    },
    followJob(data) {
      if (!data || !data.jobId) {
        this.submitting = false
        this.jobVisible = false
        this.job = emptyJob()
        this.$message.error(this.$t('project.push.failed'))
        return
      }
      this.applyJob(data)
      const state = String(data.state || '').toUpperCase()
      if (['SUCCESS', 'SUCCEEDED', 'COMPLETED', 'DONE'].includes(state)) {
        this.markSyncSuccess(data.message)
        return
      }
      if (isRunningJob(data) || !state) {
        this.pollJob(data.jobId)
        return
      }
      this.submitting = false
      if (state === 'FAILED' || state === 'PARTIAL' || jobHasFailedParts(data)) {
        this.$message.error(data.error || this.$t('project.push.failed'))
      }
    }
  }
}
</script>

<style scoped>
.project-push-hint {
  margin-top: 10px !important;
  margin-bottom: 10px !important;
}
.project-push-form {
  margin-top: 0;
}
.project-push-form :deep(.el-form-item__label) {
  padding-right: 24px;
}
.project-push-key-hint {
  margin-top: 12px;
  line-height: 1.5;
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
}
.project-push-progress {
  width: 100%;
  max-width: 720px;
}
.project-push-progress__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--el-text-color-regular, #606266);
}
.project-push-error,
.project-push-parts {
  margin-top: 10px;
}
.project-push-logs {
  margin: 0;
  width: 100%;
  max-width: 720px;
  max-height: 280px;
  overflow: auto;
  padding: 10px 12px;
  border-radius: 4px;
  border: 1px solid var(--el-border-color, #dcdfe6);
  background: var(--el-fill-color-blank, #fff);
  color: var(--el-text-color-primary, #303133);
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
</style>
