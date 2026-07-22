<template>
  <div class="git-settings-webhooks" v-loading="loading">
    <div class="git-settings-webhooks__header">
      <div>
        <h3 class="git-settings-webhooks__title">{{ $t('git.repo.settings.nav.webhooks') }}</h3>
        <p class="git-settings-webhooks__desc">{{ $t('git.repo.settings.webhooks.desc') }}</p>
      </div>
      <el-button type="primary" plain size="small" @click="openCreateDialog">
        {{ $t('git.repo.settings.webhooks.add') }}
      </el-button>
    </div>

    <el-table
      class="git-settings-webhooks__table"
      :data="webhooks"
      size="small"
      empty-text="—"
    >
      <el-table-column :label="$t('git.repo.settings.webhooks.column-name')" min-width="240">
        <template #default="{ row }">
          <div class="git-settings-webhooks__name">
            <span v-if="row.alias" class="git-settings-webhooks__alias">{{ row.alias }}</span>
            <span class="git-settings-webhooks__url">{{ row.url }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('git.repo.settings.webhooks.column-events')" min-width="200">
        <template #default="{ row }">
          <el-tag
            v-for="ev in normalizeEvents(row.events)"
            :key="ev"
            size="small"
            effect="plain"
            class="git-settings-webhooks__event-tag"
          >
            {{ eventLabel(ev) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('git.repo.settings.webhooks.column-active')" width="100" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="!!row.active"
            :loading="activeUpdatingId === row.webhookId"
            @change="val => onActiveChange(row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column :label="$t('git.repo.settings.webhooks.column-actions')" width="360" align="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" :icon="Edit" @click="openEditDialog(row)">
            {{ $t('modify') }}
          </el-button>
          <el-button
            link
            type="primary"
            size="small"
            :icon="RefreshRight"
            :loading="regeneratingId === row.webhookId"
            @click="regenerateSecret(row)"
          >
            {{ $t('git.repo.settings.webhooks.regenerate') }}
          </el-button>
          <el-button link type="primary" size="small" :icon="Tickets" @click="openDeliveries(row)">
            {{ $t('git.repo.settings.webhooks.deliveries') }}
          </el-button>
          <el-button
            link
            type="primary"
            size="small"
            :icon="VideoPlay"
            :loading="testingId === row.webhookId"
            @click="testWebhook(row)"
          >
            {{ $t('git.repo.settings.webhooks.test') }}
          </el-button>
          <el-button
            link
            size="small"
            class="red"
            :icon="Delete"
            :loading="deletingId === row.webhookId"
            @click="deleteWebhook(row)"
          >
            {{ $t('git.repo.settings.webhooks.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="formVisible"
      :title="formMode === 'create' ? $t('git.repo.settings.webhooks.create-title') : $t('git.repo.settings.webhooks.edit-title')"
      width="520px"
      append-to-body
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px" size="default">
        <el-form-item :label="$t('git.repo.settings.webhooks.url')" prop="url">
          <el-input v-model="form.url" :placeholder="$t('git.repo.settings.webhooks.url-placeholder')" />
        </el-form-item>
        <el-form-item :label="$t('git.repo.settings.webhooks.alias')" prop="alias">
          <el-input v-model="form.alias" :placeholder="$t('git.repo.settings.webhooks.alias-placeholder')" />
        </el-form-item>
        <el-form-item
          v-if="formMode === 'create'"
          :label="$t('git.repo.settings.webhooks.secret')"
          prop="secret"
        >
          <el-input
            v-model="form.secret"
            :placeholder="$t('git.repo.settings.webhooks.secret-auto')"
            show-password
            autocomplete="new-password"
          />
          <p class="git-settings-webhooks__field-hint">{{ $t('git.repo.settings.webhooks.secret-hint-create') }}</p>
        </el-form-item>
        <el-form-item :label="$t('git.repo.settings.webhooks.events')" prop="events">
          <el-checkbox-group v-model="form.events">
            <el-checkbox value="push">{{ $t('git.repo.settings.webhooks.event-push') }}</el-checkbox>
            <el-checkbox value="tag">{{ $t('git.repo.settings.webhooks.event-tag') }}</el-checkbox>
            <el-checkbox value="pull_request">{{ $t('git.repo.settings.webhooks.event-pull-request') }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item :label="$t('git.repo.settings.webhooks.active')">
          <el-switch v-model="form.active" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">{{ $t('cancel') }}</el-button>
        <el-button type="primary" :loading="formSubmitting" @click="submitForm">
          {{ $t('ok') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="secretRevealVisible"
      :title="$t('git.repo.settings.webhooks.secret-reveal-title')"
      width="520px"
      append-to-body
      :close-on-click-modal="false"
      @closed="revealedSecret = ''"
    >
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        class="git-settings-webhooks__secret-alert"
      >
        {{ $t('git.repo.settings.webhooks.secret-reveal-warning') }}
      </el-alert>
      <div class="git-settings-webhooks__secret-row">
        <el-input :model-value="revealedSecret" readonly />
        <el-button
          plain
          size="small"
          :icon="CopyDocument"
          :title="$t('git.repo.settings.webhooks.secret-copy')"
          v-clipboard:copy="revealedSecret"
          v-clipboard:success="onSecretCopySuccess"
        />
      </div>
      <template #footer>
        <el-button type="primary" @click="secretRevealVisible = false">{{ $t('ok') }}</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="deliveriesVisible"
      :title="deliveriesTitle"
      size="480px"
      append-to-body
      destroy-on-close
      @closed="onDeliveriesClosed"
    >
      <div v-loading="deliveriesLoading" class="git-settings-webhooks__deliveries">
        <el-table
          v-if="deliveries.length"
          :data="deliveries"
          size="small"
          empty-text="—"
        >
          <el-table-column :label="$t('git.repo.settings.webhooks.delivery-time')" width="150">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column :label="$t('git.repo.settings.webhooks.delivery-event')" prop="event" width="100" />
          <el-table-column :label="$t('git.repo.settings.webhooks.delivery-status')" min-width="120">
            <template #default="{ row }">
              <el-tag
                size="small"
                :type="row.success ? 'success' : 'danger'"
                effect="plain"
              >
                {{ row.success ? $t('git.repo.settings.webhooks.delivery-success') : $t('git.repo.settings.webhooks.delivery-failed') }}
              </el-tag>
              <span v-if="row.responseStatus != null" class="git-settings-webhooks__status-code">
                {{ row.responseStatus }}
              </span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('git.repo.settings.webhooks.delivery-duration')" width="80">
            <template #default="{ row }">
              <span v-if="row.durationMs != null">{{ row.durationMs }}ms</span>
              <span v-else>—</span>
            </template>
          </el-table-column>
        </el-table>
        <p v-else class="git-settings-webhooks__deliveries-empty">
          {{ $t('git.repo.settings.webhooks.deliveries-empty') }}
        </p>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { CopyDocument, Delete, Edit, RefreshRight, Tickets, VideoPlay } from '@element-plus/icons-vue'
import { markRaw } from 'vue'
import {
  createGitRepoWebhook,
  delGitRepoWebhook,
  listGitRepoWebhookDeliveries,
  listGitRepoWebhooks,
  regenerateGitRepoWebhookSecret,
  testGitRepoWebhook,
  updateGitRepoWebhook
} from '@/api/system/gitRepo'
import { parseTime } from '@/utils/ruoyi'

const EVENT_VALUES = ['push', 'tag', 'pull_request']

export default {
  name: 'GitRepoSettingsWebhooks',
  components: { CopyDocument, Delete, Edit, RefreshRight, Tickets, VideoPlay },
  data() {
    return {
      CopyDocument: markRaw(CopyDocument),
      Delete: markRaw(Delete),
      Edit: markRaw(Edit),
      RefreshRight: markRaw(RefreshRight),
      Tickets: markRaw(Tickets),
      VideoPlay: markRaw(VideoPlay),
      loading: false,
      webhooks: [],
      activeUpdatingId: null,
      regeneratingId: null,
      testingId: null,
      deletingId: null,
      formVisible: false,
      formMode: 'create',
      formSubmitting: false,
      editingWebhookId: null,
      form: {
        url: '',
        alias: '',
        secret: '',
        events: ['push'],
        active: true
      },
      secretRevealVisible: false,
      revealedSecret: '',
      deliveriesVisible: false,
      deliveriesLoading: false,
      deliveriesWebhook: null,
      deliveries: []
    }
  },
  computed: {
    repositoryId() {
      const id = Number(this.$route.params.repositoryId)
      return Number.isFinite(id) && id > 0 ? id : null
    },
    formRules() {
      return {
        url: [{ required: true, message: this.$t('git.repo.settings.webhooks.url-required'), trigger: 'blur' }],
        events: [{
          type: 'array',
          required: true,
          min: 1,
          message: this.$t('git.repo.settings.webhooks.events-required'),
          trigger: 'change'
        }]
      }
    },
    deliveriesTitle() {
      const row = this.deliveriesWebhook
      const label = row && (row.alias || row.url)
      if (label) {
        return `${this.$t('git.repo.settings.webhooks.deliveries-title')} — ${label}`
      }
      return this.$t('git.repo.settings.webhooks.deliveries-title')
    }
  },
  watch: {
    '$route.params.repositoryId'() {
      this.loadWebhooks()
    }
  },
  created() {
    this.loadWebhooks()
  },
  methods: {
    normalizeEvents(events) {
      if (!Array.isArray(events)) return []
      return events.filter(ev => EVENT_VALUES.includes(ev))
    },
    eventLabel(ev) {
      const map = {
        push: 'git.repo.settings.webhooks.event-push',
        tag: 'git.repo.settings.webhooks.event-tag',
        pull_request: 'git.repo.settings.webhooks.event-pull-request'
      }
      const key = map[ev]
      return key ? this.$t(key) : ev
    },
    formatTime(t) {
      if (!t) return '—'
      return parseTime(t, '{y}-{m}-{d} {h}:{i}:{s}') || '—'
    },
    loadWebhooks() {
      const id = this.repositoryId
      if (!id) {
        this.webhooks = []
        return
      }
      this.loading = true
      listGitRepoWebhooks(id).then(res => {
        const list = (res && res.data) || []
        this.webhooks = Array.isArray(list) ? list : []
      }).catch(() => {
        this.webhooks = []
        this.$modal.msgError(this.$t('git.repo.settings.webhooks.load-failed'))
      }).finally(() => {
        this.loading = false
      })
    },
    openCreateDialog() {
      this.formMode = 'create'
      this.editingWebhookId = null
      this.form = {
        url: '',
        alias: '',
        secret: '',
        events: ['push'],
        active: true
      }
      this.formVisible = true
    },
    openEditDialog(row) {
      this.formMode = 'edit'
      this.editingWebhookId = row.webhookId
      this.form = {
        url: row.url || '',
        alias: row.alias || '',
        secret: '',
        events: this.normalizeEvents(row.events).length
          ? [...this.normalizeEvents(row.events)]
          : ['push'],
        active: row.active !== false
      }
      this.formVisible = true
    },
    resetForm() {
      this.formSubmitting = false
      this.editingWebhookId = null
    },
    submitForm() {
      const id = this.repositoryId
      if (!id || !this.$refs.formRef) return
      this.$refs.formRef.validate(valid => {
        if (!valid) return
        this.formSubmitting = true
        const payload = {
          url: String(this.form.url || '').trim(),
          alias: String(this.form.alias || '').trim() || undefined,
          events: this.normalizeEvents(this.form.events),
          active: !!this.form.active
        }
        if (this.formMode === 'create') {
          const secret = String(this.form.secret || '').trim()
          if (secret) payload.secret = secret
          createGitRepoWebhook(id, payload).then(res => {
            const data = (res && res.data) || {}
            this.formVisible = false
            this.$modal.msgSuccess(this.$t('git.repo.settings.webhooks.create-success'))
            this.loadWebhooks()
            const plain = data.secret != null ? String(data.secret) : ''
            if (plain) this.showSecretReveal(plain)
          }).catch(err => {
            // HTTP 层错误已由 request 拦截器提示；此处仅展示业务 msg
            const msg = err && err.msg
            if (msg) this.$modal.msgError(msg)
          }).finally(() => {
            this.formSubmitting = false
          })
          return
        }
        updateGitRepoWebhook(id, this.editingWebhookId, payload).then(() => {
          this.formVisible = false
          this.$modal.msgSuccess(this.$t('git.repo.settings.webhooks.update-success'))
          this.loadWebhooks()
        }).catch(err => {
          const msg = err && err.msg
          if (msg) this.$modal.msgError(msg)
        }).finally(() => {
          this.formSubmitting = false
        })
      })
    },
    onActiveChange(row, active) {
      const id = this.repositoryId
      const webhookId = row && row.webhookId
      if (!id || webhookId == null) return
      const prev = !!row.active
      row.active = !!active
      this.activeUpdatingId = webhookId
      updateGitRepoWebhook(id, webhookId, {
        url: row.url,
        alias: row.alias,
        events: this.normalizeEvents(row.events),
        active: !!active
      }).catch(err => {
        row.active = prev
        const msg = (err && (err.msg || err.message)) || this.$t('git.repo.settings.webhooks.toggle-active-failed')
        this.$modal.msgError(msg)
      }).finally(() => {
        this.activeUpdatingId = null
      })
    },
    showSecretReveal(secret) {
      this.revealedSecret = secret
      this.secretRevealVisible = true
    },
    onSecretCopySuccess() {
      this.$modal.msgSuccess(this.$t('git.repo.settings.webhooks.secret-copy-success'))
    },
    regenerateSecret(row) {
      const id = this.repositoryId
      const webhookId = row && row.webhookId
      if (!id || webhookId == null) return
      this.$modal.confirm(
        this.$t('git.repo.settings.webhooks.regenerate-confirm'),
        this.$t('git.repo.settings.webhooks.regenerate'),
        { confirmButtonText: this.$t('ok'), cancelButtonText: this.$t('cancel'), type: 'warning' }
      ).then(() => {
        this.regeneratingId = webhookId
        return regenerateGitRepoWebhookSecret(id, webhookId).then(res => {
          const data = (res && res.data) || {}
          const plain = data.secret != null ? String(data.secret) : ''
          this.$modal.msgSuccess(this.$t('git.repo.settings.webhooks.regenerate-success'))
          if (plain) this.showSecretReveal(plain)
        })
      }).catch(err => {
        if (err === 'cancel' || err === 'close') return
        const msg = (err && (err.msg || err.message)) || this.$t('git.repo.settings.webhooks.regenerate-failed')
        this.$modal.msgError(msg)
      }).finally(() => {
        this.regeneratingId = null
      })
    },
    testWebhook(row) {
      const id = this.repositoryId
      const webhookId = row && row.webhookId
      if (!id || webhookId == null) return
      this.testingId = webhookId
      testGitRepoWebhook(id, webhookId).then(() => {
        this.$modal.msgSuccess(this.$t('git.repo.settings.webhooks.test-success'))
      }).catch(err => {
        const msg = (err && (err.msg || err.message)) || this.$t('git.repo.settings.webhooks.test-failed')
        this.$modal.msgError(msg)
      }).finally(() => {
        this.testingId = null
      })
    },
    deleteWebhook(row) {
      const id = this.repositoryId
      const webhookId = row && row.webhookId
      if (!id || webhookId == null) return
      const label = row.alias || row.url || String(webhookId)
      this.$modal.confirm(
        this.$t('git.repo.settings.webhooks.delete-confirm', { name: label }),
        this.$t('git.repo.settings.webhooks.delete'),
        { confirmButtonText: this.$t('ok'), cancelButtonText: this.$t('cancel'), type: 'warning' }
      ).then(() => {
        this.deletingId = webhookId
        return delGitRepoWebhook(id, webhookId).then(() => {
          this.$modal.msgSuccess(this.$t('git.repo.settings.webhooks.delete-success'))
          this.loadWebhooks()
        })
      }).catch(err => {
        if (err === 'cancel' || err === 'close') return
        const msg = (err && (err.msg || err.message)) || this.$t('git.repo.settings.webhooks.delete-failed')
        this.$modal.msgError(msg)
      }).finally(() => {
        this.deletingId = null
      })
    },
    openDeliveries(row) {
      this.deliveriesWebhook = row
      this.deliveries = []
      this.deliveriesVisible = true
      this.loadDeliveries()
    },
    onDeliveriesClosed() {
      this.deliveriesWebhook = null
      this.deliveries = []
    },
    loadDeliveries() {
      const id = this.repositoryId
      const row = this.deliveriesWebhook
      const webhookId = row && row.webhookId
      if (!id || webhookId == null) return
      this.deliveriesLoading = true
      listGitRepoWebhookDeliveries(id, webhookId).then(res => {
        const list = (res && res.data) || []
        this.deliveries = Array.isArray(list) ? list : []
      }).catch(() => {
        this.deliveries = []
        this.$modal.msgError(this.$t('git.repo.settings.webhooks.load-failed'))
      }).finally(() => {
        this.deliveriesLoading = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.git-settings-webhooks {
  color: var(--text-color-regular);
}

.git-settings-webhooks__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.git-settings-webhooks__title {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--text-color-primary);
}

.git-settings-webhooks__desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-color-secondary);
}

.git-settings-webhooks__table {
  width: 100%;
}

.git-settings-webhooks__name {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.git-settings-webhooks__alias {
  font-weight: 500;
  color: var(--text-color-primary);
}

.git-settings-webhooks__url {
  font-size: 12px;
  color: var(--text-color-secondary);
  word-break: break-all;
}

.git-settings-webhooks__event-tag + .git-settings-webhooks__event-tag {
  margin-left: 4px;
}

.git-settings-webhooks__field-hint {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.4;
  color: var(--text-color-secondary);
}

.git-settings-webhooks__secret-alert {
  margin-bottom: 16px;
}

.git-settings-webhooks__secret-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.git-settings-webhooks__secret-row .el-input {
  flex: 1 1 auto;
}

.git-settings-webhooks__deliveries {
  min-height: 120px;
}

.git-settings-webhooks__deliveries-empty {
  margin: 24px 0;
  text-align: center;
  color: var(--text-color-secondary);
  font-size: 13px;
}

.git-settings-webhooks__status-code {
  margin-left: 6px;
  font-size: 12px;
  color: var(--text-color-secondary);
}
</style>
