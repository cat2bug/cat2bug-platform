<template>
  <div class="invite-page">
    <div class="invite-card">
      <div class="invite-header">
        <span class="brand-name">
          <span class="brand-name-cat2">Cat2</span><span class="brand-name-bug">Bug</span>
        </span>
        <lang-select class="lang-select" />
      </div>

      <div v-loading="loading" class="invite-body">
        <template v-if="loadError">
          <div class="invite-outcome invite-outcome--error">
            <div class="invite-outcome-icon" aria-hidden="true">
              <legacy-icon icon="el-icon-close"></legacy-icon>
            </div>
            <p class="invite-outcome-title">{{ $t('invite.status.invalid') }}</p>
            <p v-if="loadError" class="invite-outcome-desc">{{ loadError }}</p>
            <div class="invite-outcome-actions">
              <el-button type="primary" @click="goLogin">{{ $t('login') }}</el-button>
            </div>
          </div>
        </template>

        <template v-else-if="preview">
          <h2 class="invite-title">{{ pageTitle }}</h2>

          <dl class="invite-details">
            <div class="invite-detail-row">
              <dt>{{ $t('invite.inviter') }}</dt>
              <dd>{{ preview.inviterName || '-' }}</dd>
            </div>
            <div v-if="isRepoInvite" class="invite-detail-row">
              <dt>{{ $t('invite.target-repository') }}</dt>
              <dd>{{ preview.repositoryName || '-' }}</dd>
            </div>
            <div v-if="isProjectInvite" class="invite-detail-row">
              <dt>{{ $t('invite.target-project') }}</dt>
              <dd>{{ preview.projectName || '-' }}</dd>
            </div>
            <div class="invite-detail-row">
              <dt>{{ $t('invite.target-team') }}</dt>
              <dd>{{ preview.teamName || '-' }}</dd>
            </div>
            <div class="invite-detail-row">
              <dt>{{ $t('invite.roles') }}</dt>
              <dd>{{ roleNamesText }}</dd>
            </div>
            <div class="invite-detail-row">
              <dt>{{ $t('invite.valid-until') }}</dt>
              <dd>{{ formatExpiresAt(preview.expiresAt) }}</dd>
            </div>
          </dl>

          <el-alert
            v-if="previewStatus !== 'valid'"
            :title="statusAlertTitle"
            :description="statusAlertDescription"
            :type="statusAlertType"
            :closable="false"
            show-icon
            class="invite-status-alert"
          />

          <template v-if="acceptState === 'success'">
            <div class="invite-outcome invite-outcome--success">
              <div class="invite-outcome-icon" aria-hidden="true">
                <legacy-icon icon="el-icon-check"></legacy-icon>
              </div>
              <p class="invite-outcome-title">{{ $t('invite.accept-success') }}</p>
              <div class="invite-outcome-actions">
                <el-button type="primary" @click="goAfterAccept">{{ primaryActionLabel }}</el-button>
              </div>
            </div>
          </template>

          <template v-else-if="acceptState === 'already-member' || showAlreadyMember">
            <div class="invite-outcome invite-outcome--info">
              <div class="invite-outcome-icon" aria-hidden="true">
                <legacy-icon icon="el-icon-info"></legacy-icon>
              </div>
              <p class="invite-outcome-title">{{ alreadyMemberText }}</p>
              <div class="invite-outcome-actions">
                <el-button type="primary" @click="goAfterAccept">{{ primaryActionLabel }}</el-button>
              </div>
            </div>
          </template>

          <template v-else-if="previewStatus === 'valid'">
            <p class="invite-hint">{{ $t('invite.join-hint') }}</p>
            <div class="invite-actions">
              <el-button
                v-if="isLoggedIn"
                class="invite-join-btn"
                type="primary"
                :loading="accepting"
                @click="handleAccept"
              >{{ $t('invite.join') }}</el-button>
              <el-button v-else class="invite-join-btn" type="primary" @click="goLogin">{{ $t('invite.login-to-join') }}</el-button>
            </div>
          </template>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import LangSelect from '@/components/LangSelect'
import { getToken } from '@/utils/auth'
import { parseTime } from '@/utils/ruoyi'
import { previewInvite, acceptInvite } from '@/api/system/memberInvite'

export default {
  name: 'InviteAccept',
  components: { LangSelect },
  data() {
    return {
      loading: false,
      accepting: false,
      preview: null,
      loadError: '',
      acceptState: '',
      acceptResult: null
    }
  },
  computed: {
    token() {
      return this.$route.params.token
    },
    isLoggedIn() {
      return !!getToken()
    },
    inviteScope() {
      const scope = this.preview && this.preview.scope
      return scope ? String(scope).toUpperCase() : ''
    },
    isProjectInvite() {
      return this.inviteScope === 'PROJECT'
    },
    isRepoInvite() {
      return this.inviteScope === 'REPO'
    },
    previewStatus() {
      if (!this.preview) {
        return ''
      }
      if (this.preview.status) {
        return this.preview.status
      }
      if (this.preview.revoked) {
        return 'revoked'
      }
      if (this.preview.expiresAt && new Date(this.preview.expiresAt).getTime() <= Date.now()) {
        return 'expired'
      }
      return 'valid'
    },
    pageTitle() {
      if (this.isRepoInvite) {
        return this.$t('invite.repo-invite')
      }
      return this.isProjectInvite ? this.$t('invite.project-invite') : this.$t('invite.team-invite')
    },
    roleNamesText() {
      const names = (this.preview && this.preview.roleNames) || []
      if (!names.length) {
        return '-'
      }
      return names.join('、')
    },
    statusAlertTitle() {
      const map = {
        expired: this.$t('invite.status.expired'),
        revoked: this.$t('invite.status.revoked'),
        invalid: this.$t('invite.status.invalid')
      }
      return map[this.previewStatus] || this.$t('invite.status.invalid')
    },
    statusAlertDescription() {
      if (this.previewStatus === 'expired' || this.previewStatus === 'revoked') {
        return this.$t('invite.invalid-contact-inviter')
      }
      return ''
    },
    statusAlertType() {
      return this.previewStatus === 'revoked' ? 'warning' : 'error'
    },
    showAlreadyMember() {
      return !!(this.preview && this.preview.alreadyMember)
    },
    alreadyMemberText() {
      if (this.isRepoInvite) {
        return this.$t('invite.already-repo-member')
      }
      if (this.isProjectInvite) {
        return this.$t('invite.already-project-member')
      }
      return this.$t('invite.already-team-member')
    },
    canGoToRepo() {
      const result = this.acceptResult
      if (!result) {
        return false
      }
      return !!(result.redirectPath || result.repositoryId)
    },
    primaryActionLabel() {
      return this.canGoToRepo ? this.$t('invite.go-repo') : this.$t('invite.go-home')
    }
  },
  watch: {
    '$route.params.token'() {
      this.resetAndLoad()
    }
  },
  created() {
    this.resetAndLoad()
  },
  methods: {
    resetAndLoad() {
      this.preview = null
      this.loadError = ''
      this.acceptState = ''
      this.acceptResult = null
      this.loadPreview()
    },
    formatExpiresAt(expiresAt) {
      if (!expiresAt) {
        return '-'
      }
      return parseTime(expiresAt, '{y}-{m}-{d} {h}:{i}')
    },
    loadPreview() {
      if (!this.token) {
        this.loadError = this.$t('invite.status.invalid')
        return
      }
      this.loading = true
      previewInvite(this.token).then(res => {
        this.preview = res.data || res
        if (this.preview.alreadyMember) {
          this.acceptState = 'already-member'
        }
      }).catch(err => {
        this.loadError = (err && err.msg) || (err && err.message) || this.$t('invite.status.invalid')
      }).finally(() => {
        this.loading = false
      })
    },
    handleAccept() {
      if (!this.token || this.previewStatus !== 'valid') {
        return
      }
      this.accepting = true
      acceptInvite(this.token).then(res => {
        const data = res.data || {}
        this.acceptResult = data
        if (data.alreadyMember === true || data.alreadyMember === 'true') {
          this.acceptState = 'already-member'
        } else {
          this.acceptState = 'success'
          // Stay on success UI; user clicks「进入仓库」. Auto-push used to race
          // route guards before ACL/project context settled (and /invite was
          // mis-parsed as /{teamKey}/{projectKey} before reserved-word fix).
        }
      }).catch(err => {
        const msg = (err && err.msg) || (err && err.message) || ''
        if (msg.includes('已是') || msg.includes('already') || (err && err.code === 601)) {
          this.acceptState = 'already-member'
        } else if (msg.includes('过期') || msg.includes('expired')) {
          this.$modal.msgError(this.$t('invite.status.expired'))
        } else if (msg.includes('撤销') || msg.includes('revoked')) {
          this.$modal.msgError(this.$t('invite.status.revoked'))
        } else {
          this.$modal.msgError(msg || this.$t('exception'))
        }
      }).finally(() => {
        this.accepting = false
      })
    },
    /**
     * Prefer backend redirectPath; fall back to legacy browse-by-id route.
     * @returns {boolean} whether navigation was attempted
     */
    navigateAfterAccept(result) {
      if (!result) {
        return false
      }
      const path = result.redirectPath
      if (path) {
        this.navigateToPath(path)
        return true
      }
      if (result.repositoryId) {
        this.$router.push('/project/git-repo/browse/' + result.repositoryId)
        return true
      }
      return false
    },
    navigateToPath(path) {
      if (/^https?:\/\//i.test(path)) {
        window.location.href = path
        return
      }
      this.$router.push(path)
    },
    goAfterAccept() {
      if (this.navigateAfterAccept(this.acceptResult)) {
        return
      }
      this.goHome()
    },
    goLogin() {
      const redirect = this.$route.fullPath
      this.$router.push({ path: '/login', query: { redirect } })
    },
    goHome() {
      this.$router.push({ path: '/' })
    }
  }
}
</script>

<style lang="scss">
.invite-page {
  --invite-page-bg-start: #f5f7fa;
  --invite-page-bg-end: #e4e7ed;
  --invite-card-bg: #ffffff;
  --invite-card-shadow: rgba(0, 0, 0, 0.08);
  --invite-card-border: transparent;
  --invite-accent: #1890ff;
  --invite-accent-hover: #66b1ff;
  --invite-accent-active: #3a8ee6;
  --invite-on-accent: #ffffff;
  --invite-text-primary: #303133;
  --invite-text-secondary: #909399;
  --invite-details-bg: #f5f7fa;
  --invite-outcome-bg: #f5f7fa;
  --invite-outcome-success-icon: #67c23a;
  --invite-outcome-success-ring: rgba(103, 194, 58, 0.18);
  --invite-outcome-info-icon: #909399;
  --invite-outcome-info-ring: rgba(144, 147, 153, 0.2);
  --invite-outcome-error-icon: #f56c6c;
  --invite-outcome-error-ring: rgba(245, 108, 108, 0.18);

  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--invite-page-bg-start) 0%, var(--invite-page-bg-end) 100%);
  padding: 24px;
  color: var(--invite-text-primary);
}

html.dark .invite-page {
  --invite-page-bg-start: #17181a;
  --invite-page-bg-end: #1e1e20;
  --invite-card-bg: #1f2023;
  --invite-card-shadow: rgba(0, 0, 0, 0.4);
  --invite-card-border: #2a2a2c;
  --invite-accent: #69b1ff;
  --invite-accent-hover: #4ea8ff;
  --invite-accent-active: #096dd9;
  --invite-on-accent: #ffffff;
  --invite-text-primary: #ffffff;
  --invite-text-secondary: #9ca3af;
  --invite-details-bg: #141416;
  --invite-outcome-bg: #252529;
  --invite-outcome-success-icon: #85ce61;
  --invite-outcome-success-ring: rgba(133, 206, 97, 0.2);
  --invite-outcome-info-icon: #69b1ff;
  --invite-outcome-info-ring: rgba(105, 177, 255, 0.18);
  --invite-outcome-error-icon: #f78989;
  --invite-outcome-error-ring: rgba(247, 137, 137, 0.2);
}

.invite-card {
  width: 100%;
  max-width: 520px;
  background: var(--invite-card-bg);
  border: 1px solid var(--invite-card-border);
  border-radius: 12px;
  box-shadow: 0 8px 32px var(--invite-card-shadow);
  padding: 32px;
}

.invite-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;

  .lang .dropdown-title {
    color: var(--invite-text-primary);

    .svg-icon {
      color: var(--invite-text-primary);
    }

    i {
      color: var(--invite-text-secondary);
    }
  }
}

.brand-name-cat2 {
  color: var(--invite-text-primary);
  font-weight: 700;
  font-size: 20px;
}

.brand-name-bug {
  color: var(--invite-accent);
  font-weight: 700;
  font-size: 20px;
}

.invite-title {
  margin: 0 0 16px;
  font-size: 22px;
  color: var(--invite-text-primary);
}

.invite-details {
  margin: 0 0 20px;
  padding: 16px;
  background: var(--invite-details-bg);
  border-radius: 8px;
}

.invite-detail-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 10px;

  &:last-child {
    margin-bottom: 0;
  }

  dt {
    flex: 0 0 88px;
    margin: 0;
    color: var(--invite-text-secondary);
    font-size: 14px;
    line-height: 1.6;
  }

  dd {
    flex: 1;
    margin: 0;
    color: var(--invite-text-primary);
    font-size: 14px;
    line-height: 1.6;
    word-break: break-word;
  }
}

.invite-status-alert {
  margin-bottom: 16px;
}

.invite-hint {
  color: var(--invite-text-secondary);
  margin-bottom: 20px;
  text-align: center;
}

.invite-outcome {
  margin-top: 8px;
  padding: 24px 20px;
  text-align: center;
  background: var(--invite-outcome-bg);
  border-radius: 10px;
}

.invite-outcome-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  margin: 0 auto 14px;
  border-radius: 50%;
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
}

.invite-outcome--success .invite-outcome-icon {
  color: var(--invite-outcome-success-icon);
  background: var(--invite-outcome-success-ring);
}

.invite-outcome--info .invite-outcome-icon {
  color: var(--invite-outcome-info-icon);
  background: var(--invite-outcome-info-ring);
}

.invite-outcome--error .invite-outcome-icon {
  color: var(--invite-outcome-error-icon);
  background: var(--invite-outcome-error-ring);
}

.invite-outcome-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.5;
  color: var(--invite-text-primary);
}

.invite-outcome-desc {
  margin: 0 0 16px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--invite-text-secondary);
}

.invite-outcome-actions {
  margin-top: 16px;
  display: flex;
  justify-content: center;

  .el-button--primary {
    width: 100%;
    min-height: 40px;
    padding: 10px 24px;
  }
}

.invite-body {
  .el-button--primary {
    background-color: var(--invite-accent) !important;
    border-color: var(--invite-accent) !important;
    color: var(--invite-on-accent) !important;

    &:hover,
    &:focus {
      background-color: var(--invite-accent-hover) !important;
      border-color: var(--invite-accent-hover) !important;
      color: var(--invite-on-accent) !important;
    }

    &:active {
      background-color: var(--invite-accent-active) !important;
      border-color: var(--invite-accent-active) !important;
      color: var(--invite-on-accent) !important;
    }
  }
}

.invite-actions {
  display: flex;
  justify-content: center;

  .invite-join-btn.el-button--primary,
  .el-button--primary {
    width: 100%;
    min-height: 40px;
    padding: 10px 24px;
  }
}
</style>
