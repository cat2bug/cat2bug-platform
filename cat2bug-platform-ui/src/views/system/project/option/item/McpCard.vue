<template>
  <el-col v-if="cardVisible" :span="6">
    <el-card class="box-card">
      <template #header>
        <div class="clearfix">
          <legacy-icon icon="el-icon-connection"></legacy-icon>
          <div>
            <span>{{ $t('project-mcp.title') }}</span>
            <span>{{ $t('project-mcp.describe') }}</span>
          </div>
        </div>
      </template>
      <div class="mcp-card__status">
        <el-tag v-if="settings.enabled" type="success" size="small">{{ $t('project-mcp.enabled') }}</el-tag>
        <el-tag v-else type="info" size="small">{{ $t('project-mcp.disabled') }}</el-tag>
        <span v-if="settings.enabled" class="mcp-card__count">
          {{ $t('project-mcp.module-count', { n: settings.modules.length }) }}
        </span>
      </div>
      <el-link @click="openDialog">{{ $t('project-mcp.open-settings') }}</el-link>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="$t('project-mcp.title')" width="680px" append-to-body>
      <el-alert type="info" :closable="false" class="mcp-card__intro">
        <template #title>{{ $t('project-mcp.intro') }}</template>
      </el-alert>
      <el-form label-width="110px" @submit.prevent>
        <el-form-item :label="$t('project-mcp.switch')">
          <el-switch v-model="form.enabled" :disabled="!settings.editable" />
        </el-form-item>
        <el-form-item :label="$t('project-mcp.modules')">
          <el-checkbox-group v-model="form.modules" :disabled="!settings.editable || !form.enabled">
            <el-checkbox v-for="key in settings.availableModules" :key="key" :value="key">
              {{ $t('project-mcp.module.' + key) }}
            </el-checkbox>
          </el-checkbox-group>
          <div class="mcp-card__hint">{{ $t('project-mcp.modules-hint') }}</div>
        </el-form-item>
        <el-form-item :label="$t('access-token.mcp-url')">
          <el-input :model-value="settings.mcpUrl" readonly>
            <template #append>
              <el-button @click="copy(settings.mcpUrl)">{{ $t('access-token.copy') }}</el-button>
            </template>
          </el-input>
          <div v-if="!settings.publicUrlConfigured" class="mcp-card__warn">{{ $t('access-token.public-url-hint') }}</div>
        </el-form-item>
        <el-form-item :label="$t('project-mcp.project-ref')">
          <code>{{ projectRef }}</code>
          <span class="mcp-card__hint mcp-card__hint--inline">{{ $t('project-mcp.project-ref-hint') }}</span>
        </el-form-item>
      </el-form>

      <div class="mcp-card__clients">
        <div class="mcp-card__clients-title">{{ $t('project-mcp.clients') }}</div>
        <el-tabs v-model="clientTab">
          <el-tab-pane v-for="c in clients" :key="c.key" :label="c.label" :name="c.key">
            <div class="mcp-card__hint">{{ c.hint }}</div>
            <pre class="mcp-card__snippet">{{ c.snippet }}</pre>
            <el-button size="small" @click="copy(c.snippet)">{{ $t('access-token.copy') }}</el-button>
          </el-tab-pane>
        </el-tabs>
        <router-link :to="{ path: '/member/profile', query: { tab: 'tokens' } }" @click="dialogVisible = false">
          <el-link type="primary">{{ $t('project-mcp.go-tokens') }}</el-link>
        </router-link>
      </div>

      <template #footer>
        <span v-if="settings.updateTime" class="mcp-card__updated">
          {{ $t('project-mcp.updated', { by: settings.updateBy || '-', time: settings.updateTime }) }}
        </span>
        <el-button size="small" @click="dialogVisible = false">{{ $t('cancel') }}</el-button>
        <el-button v-if="settings.editable" type="primary" size="small" :loading="saving" @click="save">
          {{ $t('save') }}
        </el-button>
      </template>
    </el-dialog>
  </el-col>
</template>

<script>
import { getProjectMcp, updateProjectMcp } from '@/api/system/project-mcp'
import { hasAnyPermi } from '@/utils/project-option-card'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'

export default {
  name: 'McpCard',
  props: {
    project: {
      type: Object,
      default: () => ({})
    }
  },
  setup() {
    const userStore = useUserStore()
    const { config } = storeToRefs(userStore)
    return { config }
  },
  data() {
    return {
      settings: { enabled: false, modules: [], availableModules: [], editable: false, mcpUrl: '', publicUrlConfigured: true },
      form: { enabled: false, modules: [] },
      dialogVisible: false,
      saving: false,
      clientTab: 'cursor'
    }
  },
  computed: {
    projectId() {
      return parseInt(this.config.currentProjectId)
    },
    cardVisible() {
      return hasAnyPermi(['system:project:query'])
    },
    projectRef() {
      const { teamKey, projectKey } = this.settings
      return teamKey && projectKey ? `${teamKey}/${projectKey}` : String(this.projectId || '')
    },
    clients() {
      const url = this.settings.mcpUrl || 'https://<host>/mcp'
      const token = '<c2b_pat_...>'
      const header = { Authorization: `Bearer ${token}` }
      return [
        {
          key: 'cursor',
          label: 'Cursor',
          hint: this.$t('project-mcp.client.cursor'),
          snippet: JSON.stringify({ mcpServers: { cat2bug: { url, headers: header } } }, null, 2)
        },
        {
          key: 'claude-code',
          label: 'Claude Code',
          hint: this.$t('project-mcp.client.claude-code'),
          snippet: `claude mcp add --transport http cat2bug ${url} --header "Authorization: Bearer ${token}"`
        },
        {
          key: 'codex',
          label: 'Codex',
          hint: this.$t('project-mcp.client.codex'),
          snippet: `[mcp_servers.cat2bug]\nurl = "${url}"\nbearer_token_env_var = "CAT2BUG_TOKEN"`
        },
        {
          key: 'claude',
          label: 'Claude Desktop / Web',
          hint: this.$t('project-mcp.client.claude'),
          snippet: url
        }
      ]
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      if (!this.projectId) return
      getProjectMcp(this.projectId).then(res => {
        const data = res.data || {}
        this.settings = {
          ...this.settings,
          ...data,
          modules: data.modules || [],
          availableModules: data.availableModules || []
        }
      }).catch(() => {})
    },
    openDialog() {
      this.form = { enabled: !!this.settings.enabled, modules: [...this.settings.modules] }
      this.dialogVisible = true
    },
    save() {
      this.saving = true
      updateProjectMcp(this.projectId, { enabled: this.form.enabled, modules: this.form.modules }).then(() => {
        this.$modal.msgSuccess(this.$t('modify-success'))
        this.dialogVisible = false
        this.load()
      }).finally(() => {
        this.saving = false
      })
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
.mcp-card__status {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mcp-card__count,
.mcp-card__hint,
.mcp-card__updated {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.mcp-card__hint--inline {
  margin-left: 8px;
}
.mcp-card__warn {
  font-size: 12px;
  color: var(--el-color-warning);
}
.mcp-card__intro {
  margin-bottom: 12px;
}
.mcp-card__updated {
  margin-right: 12px;
}
.mcp-card__clients-title {
  font-weight: 600;
  margin: 4px 0;
}
.mcp-card__snippet {
  background: var(--el-fill-color-light);
  padding: 8px 10px;
  border-radius: 4px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 6px 0;
}
</style>
