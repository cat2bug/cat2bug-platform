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
          <el-result icon="error" :title="$t('invite.status.invalid')" :sub-title="loadError">
            <template slot="extra">
              <el-button type="primary" @click="goLogin">{{ $t('login') }}</el-button>
            </template>
          </el-result>
        </template>

        <template v-else-if="preview">
          <h2 class="invite-title">{{ pageTitle }}</h2>
          <p class="invite-target-name">{{ targetName }}</p>

          <el-alert
            v-if="previewStatus !== 'valid'"
            :title="statusAlertTitle"
            :type="statusAlertType"
            :closable="false"
            show-icon
            class="invite-status-alert"
          />

          <template v-if="acceptState === 'success'">
            <el-result icon="success" :title="$t('invite.accept-success')">
              <template slot="extra">
                <el-button type="primary" @click="goHome">{{ $t('invite.go-home') }}</el-button>
              </template>
            </el-result>
          </template>

          <template v-else-if="acceptState === 'already-member'">
            <el-result icon="info" :title="$t('invite.already-member')">
              <template slot="extra">
                <el-button type="primary" @click="goHome">{{ $t('invite.go-home') }}</el-button>
              </template>
            </el-result>
          </template>

          <template v-else-if="previewStatus === 'valid'">
            <p class="invite-hint">{{ $t('invite.valid-hint') }}</p>
            <div class="invite-actions">
              <el-button
                v-if="isLoggedIn"
                type="primary"
                :loading="accepting"
                @click="handleAccept"
              >{{ $t('invite.accept') }}</el-button>
              <el-button v-else type="primary" @click="goLogin">{{ $t('invite.login-to-accept') }}</el-button>
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
      autoAcceptAttempted: false
    }
  },
  computed: {
    token() {
      return this.$route.params.token
    },
    isLoggedIn() {
      return !!getToken()
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
      const scope = (this.preview && this.preview.scope) || ''
      if (scope === 'PROJECT') {
        return this.$t('invite.project-invite')
      }
      return this.$t('invite.team-invite')
    },
    targetName() {
      if (!this.preview) {
        return ''
      }
      if (this.preview.scope === 'PROJECT') {
        return this.preview.projectName || ''
      }
      return this.preview.teamName || ''
    },
    statusAlertTitle() {
      const map = {
        expired: this.$t('invite.status.expired'),
        revoked: this.$t('invite.status.revoked'),
        invalid: this.$t('invite.status.invalid')
      }
      return map[this.previewStatus] || this.$t('invite.status.invalid')
    },
    statusAlertType() {
      return this.previewStatus === 'revoked' ? 'warning' : 'error'
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
      this.autoAcceptAttempted = false
      this.loadPreview()
    },
    loadPreview() {
      if (!this.token) {
        this.loadError = this.$t('invite.status.invalid')
        return
      }
      this.loading = true
      previewInvite(this.token).then(res => {
        this.preview = res.data || res
        if (this.previewStatus === 'valid' && this.isLoggedIn) {
          this.tryAutoAccept()
        }
      }).catch(err => {
        this.loadError = (err && err.msg) || (err && err.message) || this.$t('invite.status.invalid')
      }).finally(() => {
        this.loading = false
      })
    },
    tryAutoAccept() {
      if (this.autoAcceptAttempted) {
        return
      }
      this.autoAcceptAttempted = true
      this.handleAccept(true)
    },
    handleAccept(silent) {
      if (!this.token || this.previewStatus !== 'valid') {
        return
      }
      this.accepting = true
      acceptInvite(this.token).then(res => {
        const data = res.data || {}
        if (data.alreadyMember === true || data.alreadyMember === 'true') {
          this.acceptState = 'already-member'
        } else {
          this.acceptState = 'success'
        }
      }).catch(err => {
        const msg = (err && err.msg) || (err && err.message) || ''
        if (msg.includes('已是') || msg.includes('already') || (err && err.code === 601)) {
          this.acceptState = 'already-member'
        } else if (!silent) {
          this.$modal.msgError(msg || this.$t('exception'))
        }
      }).finally(() => {
        this.accepting = false
      })
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

<style lang="scss" scoped>
.invite-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
  padding: 24px;
}

.invite-card {
  width: 100%;
  max-width: 520px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  padding: 32px;
}

.invite-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.brand-name-cat2 {
  color: #303133;
  font-weight: 700;
  font-size: 20px;
}

.brand-name-bug {
  color: #409eff;
  font-weight: 700;
  font-size: 20px;
}

.invite-title {
  margin: 0 0 8px;
  font-size: 22px;
  color: #303133;
}

.invite-target-name {
  margin: 0 0 20px;
  font-size: 16px;
  color: #606266;
}

.invite-status-alert {
  margin-bottom: 16px;
}

.invite-hint {
  color: #909399;
  margin-bottom: 20px;
}

.invite-actions {
  display: flex;
  justify-content: center;
}
</style>
