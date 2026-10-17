<template>
  <div class="oauth-consent-page">
    <div class="oauth-consent-card">
      <div class="oauth-consent-header">
        <span>
          <span class="brand-name-cat2">Cat2</span><span class="brand-name-bug">Bug</span>
        </span>
        <lang-select class="lang-select" />
      </div>

      <div v-loading="loading" class="oauth-consent-body">
        <template v-if="loadError">
          <h2 class="oauth-consent-title">{{ $t('oauth.invalid-request') }}</h2>
          <p class="oauth-consent-error">{{ loadError }}</p>
        </template>

        <template v-else-if="client">
          <h2 class="oauth-consent-title">{{ $t('oauth.title', { client: client.clientName }) }}</h2>
          <dl class="oauth-consent-details">
            <div class="oauth-consent-row">
              <dt>{{ $t('oauth.account') }}</dt>
              <dd class="oauth-consent-account">
                <span>{{ userName }}</span>
                <button type="button" class="oauth-consent-switch" :disabled="switching" @click="switchAccount">
                  {{ $t('oauth.switch-account') }}
                </button>
              </dd>
            </div>
            <div class="oauth-consent-row">
              <dt>{{ $t('oauth.redirect-host') }}</dt>
              <dd>{{ client.redirectHost }}<span v-if="isLoopback" class="oauth-consent-muted">（{{ $t('oauth.local-app') }}）</span></dd>
            </div>
          </dl>
          <div class="oauth-consent-project">
            <div class="oauth-consent-project__title">{{ $t('oauth.select-project') }}</div>
            <template v-if="teams.length">
              <div class="oauth-consent-project__list" role="radiogroup" :aria-label="$t('oauth.select-project')">
                <div v-for="t in teams" :key="String(t.teamId)" class="oauth-consent-project__group">
                  <div class="oauth-consent-project__team">{{ t.teamName || $t('oauth.no-team') }}</div>
                  <button
                    v-for="p in t.projects || []"
                    :key="p.projectId"
                    type="button"
                    role="radio"
                    :aria-checked="projectId === p.projectId"
                    class="oauth-consent-project__item"
                    :class="{ 'is-active': projectId === p.projectId }"
                    @click="projectId = p.projectId"
                  >
                    <span class="oauth-consent-project__radio" />
                    <span class="oauth-consent-project__name">{{ p.projectName }}</span>
                    <span v-if="t.teamKey && p.projectKey" class="oauth-consent-project__key">{{ t.teamKey }}/{{ p.projectKey }}</span>
                  </button>
                </div>
              </div>
              <p class="oauth-consent-muted oauth-consent-project__hint">{{ $t('oauth.project-hint') }}</p>
            </template>
            <p v-else class="oauth-consent-warn">{{ $t('oauth.no-project') }}</p>
          </div>
          <ul class="oauth-consent-scopes">
            <li>{{ $t('oauth.scope-mcp') }}</li>
            <li>{{ $t('oauth.scope-same-permission') }}</li>
          </ul>
          <div class="oauth-consent-actions">
            <el-button :disabled="submitting" @click="decide(false)">{{ $t('oauth.deny') }}</el-button>
            <el-button type="primary" :loading="submitting" :disabled="!projectId" @click="decide(true)">{{ $t('oauth.allow') }}</el-button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import LangSelect from '@/components/LangSelect'
import { checkOAuthAuthorize, decideOAuthAuthorize } from '@/api/system/oauth'
import { useUserStore } from '@/stores/user'

const PARAMS = ['response_type', 'client_id', 'redirect_uri', 'code_challenge', 'code_challenge_method', 'state', 'resource']
const LOOPBACK = ['127.0.0.1', 'localhost', '[::1]', '::1']

function isFramed() {
  try {
    return window.self !== window.top
  } catch (e) {
    return true
  }
}

export default {
  name: 'OAuthConsent',
  components: { LangSelect },
  data() {
    return {
      loading: true,
      submitting: false,
      switching: false,
      loadError: '',
      client: null,
      teams: [],
      projectId: null
    }
  },
  computed: {
    userName() {
      return useUserStore().name || ''
    },
    isLoopback() {
      return !!this.client && LOOPBACK.includes(String(this.client.redirectHost || '').toLowerCase())
    },
    authorizeParams() {
      const query = this.$route.query || {}
      const params = {}
      PARAMS.forEach(key => {
        if (query[key] !== undefined && query[key] !== null && query[key] !== '') params[key] = String(query[key])
      })
      return params
    }
  },
  created() {
    if (isFramed()) {
      this.loadError = this.$t('oauth.framed')
      this.loading = false
      return
    }
    checkOAuthAuthorize(this.authorizeParams).then(res => {
      this.client = res.data
      this.teams = (res.data && res.data.teams) || []
      this.selectDefault()
    }).catch(err => {
      this.loadError = (err && (err.msg || err.message)) || this.$t('oauth.invalid-request')
    }).finally(() => {
      this.loading = false
    })
  },
  methods: {
    /** 只有一个项目时直接选中；多个项目时预选网页当前所在项目，否则留给用户自己选 */
    selectDefault() {
      const projects = this.teams.flatMap(t => t.projects || [])
      if (projects.length === 1) {
        this.projectId = projects[0].projectId
        return
      }
      const current = Number(useUserStore().config?.currentProjectId)
      if (projects.some(p => p.projectId === current)) this.projectId = current
    },
    switchAccount() {
      if (this.switching) return
      this.switching = true
      const redirect = this.$route.fullPath
      const store = useUserStore()
      const goLogin = () => {
        this.$router.replace({ path: '/login', query: { redirect } })
      }
      store.LogOut().catch(() => {
        store.FedLogOut()
      }).then(goLogin)
    },
    decide(approve) {
      this.submitting = true
      const data = { ...this.authorizeParams, approve }
      if (approve) data.project_id = this.projectId
      decideOAuthAuthorize(data).then(res => {
        window.location.href = res.data.redirect
      }).catch(() => {
        this.submitting = false
      })
    }
  }
}
</script>

<style lang="scss">
.oauth-consent-page {
  --consent-bg-start: #f5f7fa;
  --consent-bg-end: #e4e7ed;
  --consent-card-bg: #ffffff;
  --consent-card-shadow: rgba(0, 0, 0, 0.08);
  --consent-card-border: transparent;
  --consent-accent: #1890ff;
  --consent-text-primary: #303133;
  --consent-text-secondary: #909399;
  --consent-details-bg: #f5f7fa;
  --consent-item-border: #dcdfe6;

  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, var(--consent-bg-start) 0%, var(--consent-bg-end) 100%);
  color: var(--consent-text-primary);
}

html.dark .oauth-consent-page {
  --consent-bg-start: #17181a;
  --consent-bg-end: #1e1e20;
  --consent-card-bg: #1f2023;
  --consent-card-shadow: rgba(0, 0, 0, 0.4);
  --consent-card-border: #2a2a2c;
  --consent-accent: #69b1ff;
  --consent-text-primary: #ffffff;
  --consent-text-secondary: #9ca3af;
  --consent-details-bg: #141416;
  --consent-item-border: #2a2a2c;
}

.oauth-consent-card {
  width: 100%;
  max-width: 480px;
  background: var(--consent-card-bg);
  border: 1px solid var(--consent-card-border);
  border-radius: 12px;
  box-shadow: 0 8px 32px var(--consent-card-shadow);
  padding: 32px;
}

.oauth-consent-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;

  .lang .dropdown-title {
    color: var(--consent-text-primary);

    .svg-icon {
      color: var(--consent-text-primary);
    }

    i {
      color: var(--consent-text-secondary);
    }
  }
}

.oauth-consent-page .brand-name-cat2 {
  color: var(--consent-text-primary);
  font-weight: 700;
  font-size: 20px;
}

.oauth-consent-page .brand-name-bug {
  color: var(--consent-accent);
  font-weight: 700;
  font-size: 20px;
}

.oauth-consent-body {
  min-height: 160px;
}

.oauth-consent-title {
  margin: 0 0 16px;
  font-size: 18px;
  color: var(--consent-text-primary);
  word-break: break-word;
}

.oauth-consent-error {
  color: var(--el-color-danger);
  word-break: break-word;
}

.oauth-consent-details {
  margin: 0 0 16px;
  padding: 12px 16px;
  border-radius: 8px;
  background: var(--consent-details-bg);
}

.oauth-consent-row {
  display: flex;
  gap: 12px;
  padding: 4px 0;

  dt {
    flex: 0 0 88px;
    color: var(--consent-text-secondary);
  }

  dd {
    flex: 1;
    margin: 0;
    color: var(--consent-text-primary);
    word-break: break-all;
  }
}

.oauth-consent-account {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.oauth-consent-switch {
  flex: none;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--consent-accent);
  font-size: 13px;
  cursor: pointer;

  &:disabled {
    cursor: default;
    opacity: 0.6;
  }
}

.oauth-consent-muted {
  color: var(--consent-text-secondary);
}

.oauth-consent-project {
  margin: 0 0 16px;
}

.oauth-consent-project__title {
  margin-bottom: 8px;
  color: var(--consent-text-primary);
  font-weight: 600;
}

.oauth-consent-project__list {
  max-height: 240px;
  overflow-y: auto;
  margin-bottom: 8px;
}

.oauth-consent-project__group + .oauth-consent-project__group {
  margin-top: 12px;
}

.oauth-consent-project__team {
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--consent-text-secondary);
}

.oauth-consent-project__item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  margin-bottom: 6px;
  border: 1px solid var(--consent-item-border);
  border-radius: 8px;
  background: var(--consent-details-bg);
  color: var(--consent-text-primary);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s;

  &:hover,
  &:focus-visible {
    border-color: var(--consent-accent);
    outline: none;
  }

  &.is-active {
    border-color: var(--consent-accent);
  }
}

.oauth-consent-project__radio {
  flex: 0 0 14px;
  height: 14px;
  border: 1px solid var(--consent-text-secondary);
  border-radius: 50%;

  .is-active > & {
    border: 4px solid var(--consent-accent);
  }
}

.oauth-consent-project__name {
  flex: 1;
  word-break: break-word;
}

.oauth-consent-project__key {
  font-size: 12px;
  color: var(--consent-text-secondary);
}

.oauth-consent-project__hint {
  margin: 0;
  font-size: 12px;
}

.oauth-consent-warn {
  margin: 0;
  color: var(--el-color-warning);
  word-break: break-word;
}

.oauth-consent-scopes {
  padding-left: 20px;
  line-height: 1.9;
  color: var(--consent-text-primary);
}

.oauth-consent-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 24px;
}
</style>
