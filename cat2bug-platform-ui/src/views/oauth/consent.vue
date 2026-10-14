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
              <dd>{{ userName }}</dd>
            </div>
            <div class="oauth-consent-row">
              <dt>{{ $t('oauth.redirect-host') }}</dt>
              <dd>{{ client.redirectHost }}<span v-if="isLoopback" class="oauth-consent-muted">（{{ $t('oauth.local-app') }}）</span></dd>
            </div>
          </dl>
          <ul class="oauth-consent-scopes">
            <li>{{ $t('oauth.scope-mcp') }}</li>
            <li>{{ $t('oauth.scope-same-permission') }}</li>
            <li>{{ $t('oauth.scope-revoke') }}</li>
          </ul>
          <div class="oauth-consent-actions">
            <el-button :disabled="submitting" @click="decide(false)">{{ $t('oauth.deny') }}</el-button>
            <el-button type="primary" :loading="submitting" @click="decide(true)">{{ $t('oauth.allow') }}</el-button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import LangSelect from '@/components/LangSelect'
import { checkOAuthAuthorize, decideOAuthAuthorize } from '@/api/system/access-token'
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
      loadError: '',
      client: null
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
    }).catch(err => {
      this.loadError = (err && (err.msg || err.message)) || this.$t('oauth.invalid-request')
    }).finally(() => {
      this.loading = false
    })
  },
  methods: {
    decide(approve) {
      this.submitting = true
      decideOAuthAuthorize({ ...this.authorizeParams, approve }).then(res => {
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
  --consent-accent: #409eff;
  --consent-text-primary: #303133;
  --consent-text-secondary: #909399;
  --consent-details-bg: #f5f7fa;

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
  --consent-accent: #ffc107;
  --consent-text-primary: #ffffff;
  --consent-text-secondary: #9ca3af;
  --consent-details-bg: #141416;
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

.oauth-consent-muted {
  color: var(--consent-text-secondary);
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
