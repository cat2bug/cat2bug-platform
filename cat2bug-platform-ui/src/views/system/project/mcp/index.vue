<template>
  <div class="app-container mcp-settings-page">
    <el-row class="project-add-page-header project-option-sub-hint-back">
      <el-page-header @back="goBack" :content="$t('project-mcp.title')" />
    </el-row>
    <el-form v-loading="loading" label-width="110px" @submit.prevent>
      <el-form-item :label="$t('project-mcp.switch')">
        <div class="mcp-page__switch">
          <el-switch v-model="form.enabled" :disabled="!settings.editable" />
          <div class="mcp-page__warn">{{ $t('project-mcp.intro') }}</div>
        </div>
      </el-form-item>
      <el-form-item :label="$t('project-mcp.modules')">
        <el-checkbox-group v-model="form.modules" :disabled="!settings.editable || !form.enabled">
          <el-checkbox v-for="key in moduleKeys" :key="key" :value="key">
            {{ $t('project-mcp.module.' + key) }}
          </el-checkbox>
        </el-checkbox-group>
        <div class="mcp-page__hint">{{ $t('project-mcp.modules-hint') }}</div>
      </el-form-item>
      <el-form-item :label="$t('project-mcp.mcp-url')">
        <el-input :model-value="displayMcpUrl" readonly>
          <template #append>
            <el-button :icon="CopyDocument" :title="$t('project-mcp.copy')" @click="copy(displayMcpUrl)" />
          </template>
        </el-input>
        <div v-if="!settings.publicUrlConfigured" class="mcp-page__warn">{{ $t('project-mcp.public-url-hint') }}</div>
      </el-form-item>
    </el-form>

    <div class="mcp-page__clients">
      <div class="mcp-page__clients-title">{{ $t('project-mcp.clients') }}</div>
      <div class="mcp-page__hint">{{ $t('project-mcp.login-hint') }}</div>

      <div class="mcp-page__picker" role="listbox" :aria-label="$t('project-mcp.clients')">
        <div v-for="group in clientGroups" :key="group.key" class="mcp-page__row">
          <span class="mcp-page__row-label">{{ group.label }}</span>
          <div class="mcp-page__chips">
            <button
              v-for="c in group.items"
              :key="c.key"
              type="button"
              class="mcp-page__chip"
              :class="{ 'is-active': clientKey === c.key }"
              role="option"
              :aria-selected="clientKey === c.key"
              @click="clientKey = c.key"
            >{{ c.label }}</button>
          </div>
        </div>
      </div>

      <template v-if="activeClient">
        <div class="mcp-page__hint mcp-page__client-hint">{{ activeClient.hint }}</div>
        <div class="mcp-page__snippet-wrap">
          <el-button
            class="mcp-page__snippet-copy"
            link
            :icon="CopyDocument"
            :title="$t('project-mcp.copy')"
            @click="copy(activeClient.snippet)"
          />
          <pre class="mcp-page__snippet">{{ activeClient.snippet }}</pre>
        </div>
      </template>
    </div>

    <div class="mcp-page__actions">
      <span v-if="settings.updateTime" class="mcp-page__updated">
        {{ $t('project-mcp.updated', { by: settings.updateBy || '-', time: settings.updateTime }) }}
      </span>
      <el-button @click="goBack">{{ $t('cancel') }}</el-button>
      <el-button v-if="settings.editable" type="primary" :loading="saving" @click="save">
        {{ $t('save') }}
      </el-button>
    </div>
  </div>
</template>

<script>
import { markRaw } from 'vue'
import { CopyDocument } from '@element-plus/icons-vue'
import { getProjectMcp, updateProjectMcp } from '@/api/system/project-mcp'
import { useUserStore } from '@/stores/user'

/**
 * 未配置 cat2bug.public-url 时，展示地址与当前网页协议/主机对齐。
 * 反代漏传 X-Forwarded-Proto 时后端常推导成 http，而用户实际在 https 页面上打开。
 */
const LOOPBACK = new Set(['localhost', '127.0.0.1', '[::1]', '::1'])

function alignMcpUrlWithPage(rawUrl, publicUrlConfigured) {
  if (!rawUrl || typeof window === 'undefined') {
    return rawUrl || ''
  }
  try {
    const u = new URL(rawUrl)
    const page = window.location
    const pageHost = page.hostname
    if (!publicUrlConfigured) {
      if (page.protocol === 'https:' || page.protocol === 'http:') {
        u.protocol = page.protocol
      }
      const sameHost = u.hostname === pageHost
      const bothLoopback = LOOPBACK.has(u.hostname) && LOOPBACK.has(pageHost)
      if (sameHost || bothLoopback) {
        u.hostname = pageHost
        u.port = page.port || ''
      }
    } else if (LOOPBACK.has(u.hostname) && LOOPBACK.has(pageHost) && u.hostname !== pageHost) {
      u.hostname = pageHost
    }
    let out = u.toString()
    if (out.endsWith('/') && !rawUrl.endsWith('/')) {
      out = out.slice(0, -1)
    }
    return out
  } catch {
    return rawUrl
  }
}

export default {
  name: 'ProjectMcpSettings',
  data() {
    return {
      CopyDocument: markRaw(CopyDocument),
      settings: { enabled: false, modules: [], availableModules: [], editable: false, mcpUrl: '', publicUrlConfigured: true },
      form: { enabled: false, modules: [] },
      loading: false,
      saving: false,
      clientKey: 'cursor'
    }
  },
  computed: {
    projectId() {
      return parseInt(useUserStore().config.currentProjectId)
    },
    moduleKeys() {
      return (this.settings.availableModules || []).map(m => (typeof m === 'string' ? m : m.key))
    },
    displayMcpUrl() {
      return alignMcpUrlWithPage(this.settings.mcpUrl || '', !!this.settings.publicUrlConfigured)
    },
    clients() {
      const url = this.displayMcpUrl || 'https://<host>/mcp'
      const mcpServersJson = JSON.stringify({ mcpServers: { cat2bug: { url } } }, null, 2)
      const httpTypeJson = JSON.stringify({
        mcpServers: {
          cat2bug: { type: 'http', url }
        }
      }, null, 2)
      const deepseekYaml = [
        '- insert:',
        '  - id: mcp-cat2bug',
        "    name: '@deepseek-ai/dsh-mcp-client'",
        '    config:',
        '      serverName: cat2bug',
        '      transport: streamable-http',
        `      url: ${url}`
      ].join('\n')
      return [
        { key: 'trae', label: 'Trae', group: 'domestic', hint: this.$t('project-mcp.client.trae'), snippet: mcpServersJson },
        { key: 'codebuddy', label: 'CodeBuddy', group: 'domestic', hint: this.$t('project-mcp.client.codebuddy'), snippet: httpTypeJson },
        { key: 'deepseek', label: 'DeepSeek', group: 'domestic', hint: this.$t('project-mcp.client.deepseek'), snippet: deepseekYaml },
        { key: 'lingma', label: '通义灵码', group: 'domestic', hint: this.$t('project-mcp.client.lingma'), snippet: url },
        { key: 'yuanqi', label: '腾讯元器', group: 'domestic', hint: this.$t('project-mcp.client.yuanqi'), snippet: url },
        { key: 'cursor', label: 'Cursor', group: 'international', hint: this.$t('project-mcp.client.cursor'), snippet: mcpServersJson },
        { key: 'vscode', label: 'VS Code', group: 'international', hint: this.$t('project-mcp.client.vscode'), snippet: httpTypeJson },
        { key: 'claude-code', label: 'Claude Code', group: 'international', hint: this.$t('project-mcp.client.claude-code'), snippet: `claude mcp add --transport http cat2bug ${url}` },
        { key: 'windsurf', label: 'Windsurf', group: 'international', hint: this.$t('project-mcp.client.windsurf'), snippet: mcpServersJson },
        { key: 'cline', label: 'Cline', group: 'international', hint: this.$t('project-mcp.client.cline'), snippet: mcpServersJson },
        { key: 'continue', label: 'Continue', group: 'international', hint: this.$t('project-mcp.client.continue'), snippet: mcpServersJson },
        { key: 'codex', label: 'Codex', group: 'international', hint: this.$t('project-mcp.client.codex'), snippet: `[mcp_servers.cat2bug]\nurl = "${url}"\n\n# codex mcp login cat2bug` },
        { key: 'claude', label: 'Claude', group: 'international', hint: this.$t('project-mcp.client.claude'), snippet: url }
      ]
    },
    clientGroups() {
      return [
        {
          key: 'domestic',
          label: this.$t('project-mcp.client-group.domestic'),
          items: this.clients.filter(c => c.group === 'domestic')
        },
        {
          key: 'international',
          label: this.$t('project-mcp.client-group.international'),
          items: this.clients.filter(c => c.group === 'international')
        }
      ]
    },
    activeClient() {
      return this.clients.find(c => c.key === this.clientKey) || this.clients[0] || null
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
      if (!this.projectId) return
      this.loading = true
      getProjectMcp(this.projectId).then(res => {
        const data = res.data || {}
        this.settings = {
          ...this.settings,
          ...data,
          modules: data.modules || [],
          availableModules: data.availableModules || []
        }
        this.form = { enabled: !!this.settings.enabled, modules: [...this.settings.modules] }
      }).catch(() => {}).finally(() => {
        this.loading = false
      })
    },
    save() {
      this.saving = true
      updateProjectMcp(this.projectId, { enabled: this.form.enabled, modules: this.form.modules }).then(() => {
        this.$modal.msgSuccess(this.$t('modify-success'))
        this.load()
      }).finally(() => {
        this.saving = false
      })
    },
    copy(text) {
      if (!text) return
      const done = () => this.$modal.msgSuccess(this.$t('project-mcp.copied'))
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
.mcp-settings-page {
  max-width: 960px;
}
.mcp-page__hint,
.mcp-page__updated {
  font-size: 12px;
  color: var(--text-color-secondary);
}
.mcp-page__switch {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  width: 100%;
}
.mcp-page__warn {
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-color-warning);
}
.mcp-page__switch .mcp-page__warn {
  margin-top: 4px;
}
.mcp-page__clients-title {
  font-weight: 600;
  margin: 4px 0;
}
.mcp-page__picker {
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border-color-light);
  border-radius: var(--cat2bug-border-radius, 4px);
  background: var(--bg-color-block);
}
.mcp-page__row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.mcp-page__row + .mcp-page__row {
  margin-top: 8px;
}
.mcp-page__row-label {
  flex: 0 0 36px;
  padding-top: 8px;
  font-size: 12px;
  line-height: 1;
  color: var(--text-color-secondary);
  white-space: nowrap;
}
.mcp-page__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.mcp-page__chip {
  height: var(--cat2bug-control-height, 32px);
  padding: 0 12px;
  border: 1px solid var(--border-color-light);
  border-radius: var(--cat2bug-border-radius, 4px);
  background: var(--bg-color-base);
  color: var(--text-color-regular);
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  white-space: nowrap;
}
.mcp-page__chip:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}
.mcp-page__chip.is-active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary);
  color: var(--bg-color-base);
  font-weight: 600;
}
html:not(.dark) .mcp-page__chip.is-active {
  color: #fff;
}
.mcp-page__client-hint {
  margin-top: 12px;
}
.mcp-page__snippet-wrap {
  position: relative;
  margin: 6px 0 0;
}
.mcp-page__snippet-copy {
  position: absolute;
  top: 6px;
  right: 6px;
  z-index: 1;
  color: var(--text-color-secondary);
}
.mcp-page__snippet-copy:hover {
  color: var(--el-color-primary);
}
.mcp-page__snippet {
  min-height: 96px;
  max-height: 220px;
  overflow: auto;
  background: var(--bg-color-base);
  border: 1px solid var(--border-color-light);
  color: var(--text-color-primary);
  padding: 10px 36px 10px 10px;
  border-radius: 4px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
.mcp-page__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}
.mcp-page__updated {
  margin-right: auto;
}
</style>
