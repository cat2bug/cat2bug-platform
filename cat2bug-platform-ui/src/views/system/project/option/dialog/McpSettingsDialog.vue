<template>
  <el-dialog :model-value="modelValue" :title="$t('project-mcp.title')" width="920px" append-to-body
             class="mcp-settings-dialog"
             @update:model-value="$emit('update:modelValue', $event)" @open="load">
    <el-alert type="info" :closable="false" class="mcp-dialog__intro">
      <template #title>{{ $t('project-mcp.intro') }}</template>
    </el-alert>
    <el-form v-loading="loading" label-width="110px" @submit.prevent>
      <el-form-item :label="$t('project-mcp.switch')">
        <el-switch v-model="form.enabled" :disabled="!settings.editable" />
      </el-form-item>
      <el-form-item :label="$t('project-mcp.modules')">
        <el-checkbox-group v-model="form.modules" :disabled="!settings.editable || !form.enabled">
          <el-checkbox v-for="key in moduleKeys" :key="key" :value="key">
            {{ $t('project-mcp.module.' + key) }}
          </el-checkbox>
        </el-checkbox-group>
        <div class="mcp-dialog__hint">{{ $t('project-mcp.modules-hint') }}</div>
      </el-form-item>
      <el-form-item :label="$t('project-mcp.mcp-url')">
        <el-input :model-value="displayMcpUrl" readonly>
          <template #append>
            <el-button :icon="CopyDocument" :title="$t('project-mcp.copy')" @click="copy(displayMcpUrl)" />
          </template>
        </el-input>
        <div v-if="!settings.publicUrlConfigured" class="mcp-dialog__warn">{{ $t('project-mcp.public-url-hint') }}</div>
      </el-form-item>
    </el-form>

    <div class="mcp-dialog__clients">
      <div class="mcp-dialog__clients-title">{{ $t('project-mcp.clients') }}</div>
      <div class="mcp-dialog__hint">{{ $t('project-mcp.login-hint') }}</div>

      <div class="mcp-dialog__picker" role="listbox" :aria-label="$t('project-mcp.clients')">
        <div v-for="group in clientGroups" :key="group.key" class="mcp-dialog__row">
          <span class="mcp-dialog__row-label">{{ group.label }}</span>
          <div class="mcp-dialog__chips">
            <button
              v-for="c in group.items"
              :key="c.key"
              type="button"
              class="mcp-dialog__chip"
              :class="{ 'is-active': clientKey === c.key }"
              role="option"
              :aria-selected="clientKey === c.key"
              @click="clientKey = c.key"
            >{{ c.label }}</button>
          </div>
        </div>
      </div>

      <template v-if="activeClient">
        <div class="mcp-dialog__hint mcp-dialog__client-hint">{{ activeClient.hint }}</div>
        <div class="mcp-dialog__snippet-wrap">
          <el-button
            class="mcp-dialog__snippet-copy"
            link
            :icon="CopyDocument"
            :title="$t('project-mcp.copy')"
            @click="copy(activeClient.snippet)"
          />
          <pre class="mcp-dialog__snippet">{{ activeClient.snippet }}</pre>
        </div>
      </template>
    </div>

    <template #footer>
      <span v-if="settings.updateTime" class="mcp-dialog__updated">
        {{ $t('project-mcp.updated', { by: settings.updateBy || '-', time: settings.updateTime }) }}
      </span>
      <el-button size="small" @click="$emit('update:modelValue', false)">{{ $t('cancel') }}</el-button>
      <el-button v-if="settings.editable" type="primary" size="small" :loading="saving" @click="save">
        {{ $t('save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import { markRaw } from 'vue'
import { CopyDocument } from '@element-plus/icons-vue'
import { getProjectMcp, updateProjectMcp } from '@/api/system/project-mcp'

/** 回环地址在展示时与当前网页主机名对齐（localhost ↔ 127.0.0.1 ↔ ::1） */
const LOOPBACK = new Set(['localhost', '127.0.0.1', '[::1]', '::1'])

function alignMcpUrlWithPage(rawUrl) {
  if (!rawUrl || typeof window === 'undefined') {
    return rawUrl || ''
  }
  try {
    const u = new URL(rawUrl)
    const pageHost = window.location.hostname
    if (LOOPBACK.has(u.hostname) && LOOPBACK.has(pageHost) && u.hostname !== pageHost) {
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
  name: 'McpSettingsDialog',
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    projectId: {
      type: Number,
      default: null
    }
  },
  emits: ['update:modelValue'],
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
    moduleKeys() {
      return (this.settings.availableModules || []).map(m => (typeof m === 'string' ? m : m.key))
    },
    displayMcpUrl() {
      return alignMcpUrlWithPage(this.settings.mcpUrl || '')
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
  methods: {
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
        this.$emit('update:modelValue', false)
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
.mcp-dialog__hint,
.mcp-dialog__updated {
  font-size: 12px;
  color: var(--text-color-secondary);
}
.mcp-dialog__warn {
  font-size: 12px;
  color: var(--el-color-warning);
}
.mcp-dialog__intro {
  margin-bottom: 12px;
}
.mcp-dialog__updated {
  margin-right: 12px;
}
.mcp-dialog__clients-title {
  font-weight: 600;
  margin: 4px 0;
}
.mcp-dialog__picker {
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border-color-light);
  border-radius: var(--cat2bug-border-radius, 4px);
  background: var(--bg-color-block);
}
.mcp-dialog__row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.mcp-dialog__row + .mcp-dialog__row {
  margin-top: 8px;
}
.mcp-dialog__row-label {
  flex: 0 0 36px;
  padding-top: 8px;
  font-size: 12px;
  line-height: 1;
  color: var(--text-color-secondary);
  white-space: nowrap;
}
.mcp-dialog__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.mcp-dialog__chip {
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
.mcp-dialog__chip:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}
.mcp-dialog__chip.is-active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary);
  color: var(--bg-color-base);
  font-weight: 600;
}
html:not(.dark) .mcp-dialog__chip.is-active {
  color: #fff;
}
.mcp-dialog__client-hint {
  margin-top: 12px;
}
.mcp-dialog__snippet-wrap {
  position: relative;
  margin: 6px 0 0;
}
.mcp-dialog__snippet-copy {
  position: absolute;
  top: 6px;
  right: 6px;
  z-index: 1;
  color: var(--text-color-secondary);
}
.mcp-dialog__snippet-copy:hover {
  color: var(--el-color-primary);
}
.mcp-dialog__snippet {
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
</style>
