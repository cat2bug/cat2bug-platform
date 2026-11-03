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
    <el-form ref="form" class="project-push-form" :model="form" :rules="rules" label-width="120px">
      <el-form-item :label="$t('project.push.key')" prop="pullKey">
        <el-input
          v-model="form.pullKey"
          clearable
          :disabled="submitting"
          :placeholder="$t('project.push.key-placeholder')"
        />
        <div class="project-push-key-hint">{{ $t('project.push.key-hint') }}</div>
      </el-form-item>
      <el-form-item v-if="jobVisible" :label="$t('project.push.progress')">
        <div class="project-push-progress">
          <div class="project-push-progress__meta">
            <span>{{ job.phase || $t('project.push.submitting') }}</span>
          </div>
          <el-progress
            :percentage="job.percent || 0"
            :status="progressStatus"
            :stroke-width="8"
          />
          <el-alert
            v-if="job.state === 'FAILED' && failureReason"
            class="project-push-error"
            type="error"
            :closable="false"
            show-icon
            :title="failureReason"
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
            :loading="submitting"
            :disabled="syncDisabled"
            @click="onSubmit"
          >
            {{ submitting ? $t('project.push.submitting') : $t('sync') }}
            <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
          </el-button>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { pullProject, getPushJob } from "@/api/system/project";
import projectOptionSubFormKbd from '@/mixins/project-option-sub-form-kbd'
import { storeToRefs } from 'pinia';
import { useUserStore } from '@/stores/user'

export default {
  name: "index",
  mixins: [projectOptionSubFormKbd],
  setup() {
    const userStore = useUserStore()
    const { config } = storeToRefs(userStore)
    return { config }
  },
  data() {
    return {
      submitting: false,
      jobVisible: false,
      pollTimer: null,
      job: {
        jobId: '',
        state: '',
        percent: 0,
        phase: '',
        logs: [],
        error: '',
        message: ''
      },
      form: {
        pullKey: '',
      },
      rules: {
        pullKey: [
          {required: true, message: this.$i18n.t('project.push.key-cannot-empty'), trigger: "change"}
        ],
      }
    }
  },
  computed: {
    teamId() {
      return this.config.currentTeamId;
    },
    projectId() {
      return this.config.currentProjectId
    },
    logText() {
      const logs = this.job.logs || []
      return logs.map(row => {
        const time = (row.time || '').replace('T', ' ').replace(/\.\d+Z?$/, '')
        return `[${time}] ${row.level || 'INFO'} ${row.message || ''}`
      }).join('\n')
    },
    progressStatus() {
      if (this.job.state === 'SUCCESS') return 'success'
      if (this.job.state === 'FAILED') return 'exception'
      return undefined
    },
    failureReason() {
      const text = (this.job.error || this.job.message || '').trim()
      return text || ''
    },
    /** 成功后禁止再点；失败仍可重试 */
    syncDisabled() {
      return this.submitting || this.job.state === 'SUCCESS'
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
      this.onSubmit()
    },
    serializePageFormCloseState() {
      return JSON.stringify({ form: { ...this.form } })
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
        if (el) {
          el.scrollTop = el.scrollHeight
        }
      })
    },
    applyJob(data) {
      if (!data || typeof data !== 'object') return
      this.job = {
        jobId: data.jobId || this.job.jobId,
        state: data.state || '',
        percent: data.percent || 0,
        phase: data.phase || '',
        logs: Array.isArray(data.logs) ? data.logs : [],
        error: data.error || '',
        message: data.message || ''
      }
      this.scrollLogs()
    },
    pollJob(jobId) {
      this.clearPoll()
      this.pollTimer = setInterval(() => {
        getPushJob(this.projectId, jobId).then(res => {
          const data = res.data || res
          this.applyJob(data)
          if (data.state === 'SUCCESS') {
            this.clearPoll()
            this.submitting = false
            this.$message.success(data.message || this.$t('project.push.success'))
          } else if (data.state === 'FAILED') {
            this.clearPoll()
            this.submitting = false
            this.$message.error(data.error || this.$t('project.push.failed'))
          }
        }).catch(err => {
          this.clearPoll()
          this.submitting = false
          this.job.state = 'FAILED'
          this.job.error = (err && err.message) || this.$t('project.push.failed')
          this.$message.error(this.job.error)
        })
      }, 800)
    },
    /** 提交项目同步操作（异步打包上传，轮询进度） */
    onSubmit() {
      this.$refs["form"].validate(valid => {
        if (!valid || this.submitting || this.job.state === 'SUCCESS') {
          return
        }
        this.submitting = true
        this.jobVisible = true
        this.job = {
          jobId: '',
          state: 'RUNNING',
          percent: 0,
          phase: this.$t('project.push.submitting'),
          logs: [],
          error: '',
          message: ''
        }
        pullProject(this.projectId, this.form).then((res) => {
          const data = res.data || res
          if (!data || !data.jobId) {
            // 兼容旧接口：无 jobId 时视为同步完成
            this.submitting = false
            this.job.state = 'SUCCESS'
            this.job.percent = 100
            this.$message.success(this.$t('project.push.success'))
            return
          }
          this.applyJob(data)
          this.pollJob(data.jobId)
        }).catch(() => {
          this.submitting = false
          this.job.state = 'FAILED'
        })
      });
    },
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
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--el-text-color-regular, #606266);
}
.project-push-error {
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
