<template>
  <div class="access-token-panel">
    <el-alert type="info" :closable="false" class="access-token-panel__intro">
      <template #title>{{ $t('access-token.intro') }}</template>
    </el-alert>

    <div class="access-token-panel__mcp">
      <span class="access-token-panel__label">{{ $t('access-token.mcp-url') }}</span>
      <el-input :model-value="mcpUrl" readonly size="small" class="access-token-panel__mcp-input">
        <template #append>
          <el-button size="small" @click="copy(mcpUrl)">{{ $t('access-token.copy') }}</el-button>
        </template>
      </el-input>
    </div>
    <div v-if="!publicUrlConfigured" class="access-token-panel__hint">{{ $t('access-token.public-url-hint') }}</div>

    <div class="access-token-panel__toolbar">
      <el-button type="primary" size="small" @click="openCreate"><el-icon class="el-icon--left"><Plus /></el-icon>{{ $t('access-token.create') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="tokens" size="small" :empty-text="$t('access-token.empty')">
      <el-table-column :label="$t('access-token.name')" min-width="150">
        <template #default="{ row }">
          <span>{{ row.name }}</span>
          <el-tag v-if="row.source !== 'manual'" size="small" type="success" class="access-token-panel__tag">OAuth</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('access-token.prefix')" width="150">
        <template #default="{ row }"><code>{{ row.tokenPrefix }}…</code></template>
      </el-table-column>
      <el-table-column :label="$t('create-time')" prop="createTime" width="155" />
      <el-table-column :label="$t('access-token.expires-at')" width="155">
        <template #default="{ row }">{{ row.expiresAt || $t('access-token.never') }}</template>
      </el-table-column>
      <el-table-column :label="$t('access-token.last-used')" min-width="190">
        <template #default="{ row }">
          <span v-if="row.lastUsedAt">{{ row.lastUsedAt }}<span v-if="row.lastUsedIp" class="access-token-panel__ip"> · {{ row.lastUsedIp }}</span></span>
          <span v-else class="access-token-panel__muted">{{ $t('access-token.never-used') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('operate')" width="80" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" size="small" @click="revoke(row)">{{ $t('access-token.revoke') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createVisible" :title="$t('access-token.create')" width="520px" append-to-body
               :close-on-click-modal="!createdToken" @closed="onDialogClosed">
      <template v-if="!createdToken">
        <el-form ref="createForm" :model="form" :rules="rules" label-width="100px" @submit.prevent>
          <el-form-item :label="$t('access-token.name')" prop="name">
            <el-input v-model="form.name" maxlength="128" :placeholder="$t('access-token.name-placeholder')" />
          </el-form-item>
          <el-form-item :label="$t('access-token.expires-at')" prop="expireDays">
            <el-select v-model="form.expireDays" style="width: 100%">
              <el-option v-for="opt in expireOptions" :key="String(opt.value)" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
        </el-form>
      </template>
      <template v-else>
        <el-alert type="warning" :closable="false" :title="$t('access-token.copy-once')" />
        <el-input :model-value="createdToken" readonly class="access-token-panel__created">
          <template #append>
            <el-button @click="copy(createdToken)">{{ $t('access-token.copy') }}</el-button>
          </template>
        </el-input>
      </template>
      <template #footer>
        <template v-if="!createdToken">
          <el-button size="small" @click="createVisible = false">{{ $t('cancel') }}</el-button>
          <el-button type="primary" size="small" :loading="creating" @click="submitCreate">{{ $t('access-token.generate') }}</el-button>
        </template>
        <el-button v-else type="primary" size="small" @click="createVisible = false">{{ $t('access-token.done') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Plus } from '@element-plus/icons-vue'
import { listAccessTokens, createAccessToken, revokeAccessToken } from '@/api/system/access-token'

export default {
  name: 'AccessToken',
  components: { Plus },
  props: {
    active: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      loading: false,
      loaded: false,
      tokens: [],
      mcpUrl: '',
      publicUrlConfigured: true,
      createVisible: false,
      creating: false,
      createdToken: '',
      form: { name: '', expireDays: 90 },
      rules: {
        name: [{ required: true, message: this.$t('access-token.name-required'), trigger: 'blur' }]
      }
    }
  },
  computed: {
    expireOptions() {
      return [30, 90, 180, 365].map(days => ({ value: days, label: this.$t('access-token.days', { n: days }) }))
        .concat([{ value: null, label: this.$t('access-token.never') }])
    }
  },
  watch: {
    active: {
      immediate: true,
      handler(val) {
        if (val && !this.loaded) this.load()
      }
    }
  },
  methods: {
    load() {
      this.loading = true
      listAccessTokens().then(res => {
        this.tokens = res.data || []
        this.mcpUrl = res.mcpUrl || ''
        this.publicUrlConfigured = res.publicUrlConfigured !== false
        this.loaded = true
      }).finally(() => {
        this.loading = false
      })
    },
    openCreate() {
      this.form = { name: '', expireDays: 90 }
      this.createdToken = ''
      this.createVisible = true
    },
    submitCreate() {
      this.$refs.createForm.validate(valid => {
        if (!valid) return
        this.creating = true
        createAccessToken({ name: this.form.name.trim(), expireDays: this.form.expireDays }).then(res => {
          this.createdToken = res.data.token
          this.load()
        }).finally(() => {
          this.creating = false
        })
      })
    },
    onDialogClosed() {
      this.createdToken = ''
    },
    revoke(row) {
      const message = row.source === 'manual'
        ? this.$t('access-token.revoke-confirm', { name: row.name })
        : this.$t('access-token.revoke-oauth-confirm', { name: row.name })
      this.$modal.confirm(message).then(() => revokeAccessToken(row.tokenId)).then(() => {
        this.$modal.msgSuccess(this.$t('access-token.revoked'))
        this.load()
      }).catch(() => {})
    },
    copy(text) {
      if (!text) return
      const done = () => this.$modal.msgSuccess(this.$t('access-token.copied'))
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).then(done).catch(() => this.fallbackCopy(text, done))
      } else {
        this.fallbackCopy(text, done)
      }
    },
    fallbackCopy(text, done) {
      const el = document.createElement('textarea')
      el.value = text
      el.setAttribute('readonly', '')
      el.style.position = 'fixed'
      el.style.opacity = '0'
      document.body.appendChild(el)
      el.select()
      try {
        document.execCommand('copy')
        done()
      } finally {
        document.body.removeChild(el)
      }
    }
  }
}
</script>

<style scoped>
.access-token-panel__intro {
  margin-bottom: 12px;
}
.access-token-panel__mcp {
  display: flex;
  align-items: center;
  gap: 8px;
}
.access-token-panel__label {
  white-space: nowrap;
  font-size: 13px;
}
.access-token-panel__mcp-input {
  max-width: 520px;
}
.access-token-panel__hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-color-warning);
}
.access-token-panel__toolbar {
  margin: 12px 0 8px;
}
.access-token-panel__tag {
  margin-left: 6px;
}
.access-token-panel__ip,
.access-token-panel__muted {
  color: var(--el-text-color-secondary);
}
.access-token-panel__created {
  margin-top: 12px;
}
</style>
