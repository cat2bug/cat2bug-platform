<template>
  <el-dialog
    :title="$t('git.repo.settings.members.invite.dialog-title')"
    v-model="dialogVisible"
    width="640px"
    append-to-body
    :close-on-click-modal="false"
    custom-class="git-repo-invite-member-dialog"
    @closed="onClosed"
  >
    <el-tabs v-model="activeTab" class="member-invite-dialog-tabs" @tab-change="onTabChange">
      <!-- 链接邀请 -->
      <el-tab-pane :label="$t('git.repo.settings.members.invite.tab-link')" name="link">
        <el-form label-width="110px" @submit.prevent>
          <el-form-item :label="$t('git.repo.settings.members.role')">
            <el-select
              v-model="linkForm.role"
              class="invite-role-select"
              :placeholder="$t('git.repo.settings.members.role-placeholder')"
              @change="onLinkRoleChange"
            >
              <el-option
                v-for="opt in roleOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('git.repo.settings.members.invite.link-url')">
            <el-input
              v-model="linkForm.url"
              class="invite-link-input"
              readonly
              :placeholder="linkLoading ? $t('loading') : ''"
            >
              <template #append>
                <el-button
                  :icon="$legacyIcon('el-icon-document-copy')"
                  :disabled="!linkForm.url || linkLoading"
                  v-clipboard:copy="linkForm.url"
                  v-clipboard:success="onCopySuccess"
                />
              </template>
            </el-input>
            <p class="invite-link-hint">
              {{ $t('git.repo.settings.members.invite.link-validity-hint') }}
            </p>
          </el-form-item>
          <el-form-item v-if="linkForm.qrDataUrl" :label="$t('git.repo.settings.members.invite.qrcode')">
            <img
              class="invite-qr"
              :src="linkForm.qrDataUrl"
              :alt="$t('git.repo.settings.members.invite.qrcode')"
            >
          </el-form-item>
          <el-form-item>
            <el-button
              size="small"
              :loading="linkRegenLoading"
              :disabled="!linkForm.inviteId || linkLoading"
              @click="regenerateLink"
            >
              {{ $t('git.repo.settings.members.invite.regenerate') }}
            </el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 直接添加 -->
      <el-tab-pane :label="$t('git.repo.settings.members.invite.tab-direct')" name="direct">
        <el-form
          ref="directForm"
          :model="directForm"
          :rules="directRules"
          label-width="110px"
          @submit.prevent
        >
          <el-form-item :label="$t('git.repo.settings.members.user')" prop="userId">
            <el-select
              v-model="directForm.userId"
              class="member-direct-select"
              filterable
              remote
              clearable
              reserve-keyword
              :remote-method="searchUsers"
              :loading="userSearchLoading"
              :placeholder="$t('git.repo.settings.members.user-placeholder')"
              style="width: 100%"
            >
              <el-option
                v-for="item in userOptions"
                :key="item.userId"
                :label="userOptionLabel(item)"
                :value="item.userId"
                :disabled="isAlreadyMember(item.userId)"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('git.repo.settings.members.role')" prop="role">
            <el-select
              v-model="directForm.role"
              class="invite-role-select"
              :placeholder="$t('git.repo.settings.members.role-placeholder')"
            >
              <el-option
                v-for="opt in roleOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 通过仓库邀请 -->
      <el-tab-pane :label="$t('git.repo.settings.members.invite.tab-from-repo')" name="fromRepo">
        <el-form
          ref="fromRepoForm"
          :model="fromRepoForm"
          :rules="fromRepoRules"
          label-width="110px"
          @submit.prevent
        >
          <el-form-item :label="$t('git.repo.settings.members.invite.source-repo')" prop="sourceRepositoryId">
            <el-select
              v-model="fromRepoForm.sourceRepositoryId"
              class="invite-role-select"
              filterable
              clearable
              :loading="sourcesLoading"
              :placeholder="$t('git.repo.settings.members.invite.source-repo-placeholder')"
              style="width: 100%"
              @change="onSourceRepoChange"
            >
              <el-option
                v-for="repo in sourceRepos"
                :key="repo.repositoryId"
                :label="sourceRepoLabel(repo)"
                :value="repo.repositoryId"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('git.repo.settings.members.invite.source-members')" prop="userIds">
            <el-select
              v-model="fromRepoForm.userIds"
              class="member-direct-select"
              multiple
              filterable
              clearable
              collapse-tags
              :loading="sourceMembersLoading"
              :disabled="!fromRepoForm.sourceRepositoryId"
              :placeholder="$t('git.repo.settings.members.invite.source-members-placeholder')"
              style="width: 100%"
            >
              <el-option
                v-for="item in sourceMembers"
                :key="item.userId"
                :label="userOptionLabel(item)"
                :value="item.userId"
                :disabled="isAlreadyMember(item.userId)"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('git.repo.settings.members.role')" prop="role">
            <el-select
              v-model="fromRepoForm.role"
              class="invite-role-select"
              :placeholder="$t('git.repo.settings.members.role-placeholder')"
            >
              <el-option
                v-for="opt in roleOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <div class="dialog-footer">
        <el-button
          v-if="activeTab === 'direct'"
          type="primary"
          :loading="directSubmitting"
          @click="submitDirect"
        >
          {{ $t('git.repo.settings.members.add') }}
        </el-button>
        <el-button
          v-if="activeTab === 'fromRepo'"
          type="primary"
          :loading="fromRepoSubmitting"
          @click="submitFromRepo"
        >
          {{ $t('git.repo.settings.members.invite.from-repo-submit') }}
        </el-button>
        <el-button @click="dialogVisible = false">{{ $t('cancel') }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import QRCode from 'qrcode'
import {
  addGitRepoMember,
  addGitRepoMembersFromRepo,
  createGitRepoInvite,
  listGitRepoInviteSourceMembers,
  listGitRepoInviteSources,
  listGitRepoInvites,
  revokeGitRepoInvite
} from '@/api/system/gitRepo'
import { buildInviteUrl } from '@/api/system/memberInvite'
import { listUser } from '@/api/system/user'

const ROLE_VALUES = ['read', 'write', 'admin']

export default {
  name: 'InviteMemberDialog',
  props: {
    repositoryId: {
      type: Number,
      default: 0
    },
    /** Existing ACL member userIds (for disabling already-members) */
    memberIds: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      dialogVisible: false,
      activeTab: 'link',
      linkLoading: false,
      linkRegenLoading: false,
      linkRequestId: 0,
      linkForm: {
        role: 'write',
        inviteId: null,
        token: '',
        url: '',
        qrDataUrl: ''
      },
      directSubmitting: false,
      userSearchLoading: false,
      userOptions: [],
      directForm: {
        userId: null,
        role: 'write'
      },
      directRules: {
        userId: [{
          required: true,
          message: this.$t('git.repo.settings.members.user-required'),
          trigger: 'change'
        }],
        role: [{
          required: true,
          message: this.$t('git.repo.settings.members.role-required'),
          trigger: 'change'
        }]
      },
      sourcesLoading: false,
      sourceMembersLoading: false,
      fromRepoSubmitting: false,
      sourceRepos: [],
      sourceMembers: [],
      fromRepoForm: {
        sourceRepositoryId: null,
        userIds: [],
        role: 'write'
      },
      fromRepoRules: {
        sourceRepositoryId: [{
          required: true,
          message: this.$t('git.repo.settings.members.invite.source-repo-required'),
          trigger: 'change'
        }],
        userIds: [{
          type: 'array',
          required: true,
          min: 1,
          message: this.$t('git.repo.settings.members.invite.source-members-required'),
          trigger: 'change'
        }],
        role: [{
          required: true,
          message: this.$t('git.repo.settings.members.role-required'),
          trigger: 'change'
        }]
      }
    }
  },
  computed: {
    roleOptions() {
      return ROLE_VALUES.map(value => ({
        value,
        label: this.$t(`git.repo.settings.members.role-${value}`)
      }))
    },
    memberIdSet() {
      return new Set((this.memberIds || []).map(id => Number(id)).filter(id => Number.isFinite(id)))
    }
  },
  methods: {
    open() {
      this.resetAll()
      this.activeTab = 'link'
      this.dialogVisible = true
      this.$nextTick(() => {
        this.ensureLinkInvite()
      })
    },
    onClosed() {
      this.resetAll()
    },
    resetAll() {
      this.linkRequestId += 1
      this.linkLoading = false
      this.linkRegenLoading = false
      this.linkForm = {
        role: 'write',
        inviteId: null,
        token: '',
        url: '',
        qrDataUrl: ''
      }
      this.directSubmitting = false
      this.userSearchLoading = false
      this.userOptions = []
      this.directForm = { userId: null, role: 'write' }
      this.sourcesLoading = false
      this.sourceMembersLoading = false
      this.fromRepoSubmitting = false
      this.sourceRepos = []
      this.sourceMembers = []
      this.fromRepoForm = { sourceRepositoryId: null, userIds: [], role: 'write' }
      this.$nextTick(() => {
        if (this.$refs.directForm) this.$refs.directForm.clearValidate()
        if (this.$refs.fromRepoForm) this.$refs.fromRepoForm.clearValidate()
      })
    },
    normalizeRole(role) {
      const r = role != null ? String(role).toLowerCase() : ''
      return ROLE_VALUES.includes(r) ? r : 'read'
    },
    isAlreadyMember(userId) {
      return this.memberIdSet.has(Number(userId))
    },
    userOptionLabel(item) {
      const nick = (item && (item.nickName || item.userName)) || ''
      const name = (item && item.userName) || ''
      if (nick && name && nick !== name) return `${nick} (@${name})`
      return nick || name || String((item && item.userId) || '')
    },
    sourceRepoLabel(repo) {
      if (!repo) return ''
      const name = repo.name || ''
      const slug = repo.slug || ''
      if (name && slug && name !== slug) return `${name} (${slug})`
      return name || slug || String(repo.repositoryId)
    },
    onTabChange(name) {
      if (name === 'link') {
        this.ensureLinkInvite()
      } else if (name === 'fromRepo') {
        this.loadInviteSources()
      }
    },
    onLinkRoleChange() {
      this.ensureLinkInvite()
    },
    resolveInviteUrl(row) {
      if (!row) return ''
      if (row.token) return buildInviteUrl(row.token)
      return row.inviteUrl || row.url || ''
    },
    applyInvite(row) {
      const token = (row && row.token) || ''
      const url = this.resolveInviteUrl(row)
      this.linkForm.inviteId = row && row.inviteId != null ? row.inviteId : null
      this.linkForm.token = token
      this.linkForm.url = url
      this.renderQr(url)
    },
    clearInviteDisplay() {
      this.linkForm.inviteId = null
      this.linkForm.token = ''
      this.linkForm.url = ''
      this.linkForm.qrDataUrl = ''
    },
    renderQr(url) {
      if (!url) {
        this.linkForm.qrDataUrl = ''
        return
      }
      QRCode.toDataURL(url, { width: 160, margin: 1 }).then(dataUrl => {
        this.linkForm.qrDataUrl = dataUrl
      }).catch(() => {
        this.linkForm.qrDataUrl = ''
      })
    },
    ensureLinkInvite() {
      const id = this.repositoryId
      if (!Number.isFinite(id) || id <= 0) {
        this.clearInviteDisplay()
        return
      }
      const role = this.normalizeRole(this.linkForm.role)
      const requestId = ++this.linkRequestId
      this.linkLoading = true
      listGitRepoInvites(id).then(res => {
        if (requestId !== this.linkRequestId) return
        const list = (res && res.data) || []
        const rows = Array.isArray(list) ? list : []
        const existing = rows.find(r => this.normalizeRole(r && r.role) === role)
        if (existing) {
          this.applyInvite(existing)
          return null
        }
        return createGitRepoInvite(id, { role }).then(createRes => {
          if (requestId !== this.linkRequestId) return
          this.applyInvite((createRes && createRes.data) || {})
        })
      }).catch(err => {
        if (requestId !== this.linkRequestId) return
        this.clearInviteDisplay()
        const msg = (err && (err.msg || err.message)) || this.$t('git.repo.settings.members.invite.link-load-failed')
        this.$modal.msgError(msg)
      }).finally(() => {
        if (requestId === this.linkRequestId) {
          this.linkLoading = false
        }
      })
    },
    regenerateLink() {
      const id = this.repositoryId
      const inviteId = this.linkForm.inviteId
      const role = this.normalizeRole(this.linkForm.role)
      if (!Number.isFinite(id) || id <= 0 || inviteId == null) return
      const requestId = ++this.linkRequestId
      this.linkRegenLoading = true
      revokeGitRepoInvite(id, inviteId).then(() => {
        return createGitRepoInvite(id, { role })
      }).then(res => {
        if (requestId !== this.linkRequestId) return
        this.applyInvite((res && res.data) || {})
        this.$modal.msgSuccess(this.$t('git.repo.settings.members.invite.regenerate-success'))
      }).catch(err => {
        if (requestId !== this.linkRequestId) return
        const msg = (err && (err.msg || err.message)) || this.$t('git.repo.settings.members.invite.regenerate-failed')
        this.$modal.msgError(msg)
      }).finally(() => {
        if (requestId === this.linkRequestId) {
          this.linkRegenLoading = false
        }
      })
    },
    onCopySuccess() {
      this.$modal.msgSuccess(this.$t('git.repo.settings.members.invite.copy-success'))
    },
    searchUsers(query) {
      const q = (query || '').trim()
      if (!q) {
        this.userOptions = []
        return
      }
      this.userSearchLoading = true
      listUser({
        pageNum: 1,
        pageSize: 20,
        userName: q
      }).then(res => {
        const rows = (res && res.rows) || []
        this.userOptions = Array.isArray(rows) ? rows : []
      }).catch(() => {
        this.userOptions = []
      }).finally(() => {
        this.userSearchLoading = false
      })
    },
    submitDirect() {
      this.$refs.directForm.validate(valid => {
        if (!valid) return
        const id = this.repositoryId
        if (!Number.isFinite(id) || id <= 0) return
        this.directSubmitting = true
        addGitRepoMember(id, {
          userId: this.directForm.userId,
          role: this.normalizeRole(this.directForm.role)
        }).then(() => {
          this.$modal.msgSuccess(this.$t('git.repo.settings.members.add-success'))
          this.directForm.userId = null
          this.directForm.role = 'write'
          this.userOptions = []
          this.$nextTick(() => {
            if (this.$refs.directForm) this.$refs.directForm.clearValidate()
          })
          this.$emit('added')
          this.dialogVisible = false
        }).catch(err => {
          const msg = (err && (err.msg || err.message)) || this.$t('git.repo.settings.members.add-failed')
          this.$modal.msgError(msg)
        }).finally(() => {
          this.directSubmitting = false
        })
      })
    },
    loadInviteSources() {
      const id = this.repositoryId
      if (!Number.isFinite(id) || id <= 0) {
        this.sourceRepos = []
        return
      }
      if (this.sourceRepos.length) return
      this.sourcesLoading = true
      listGitRepoInviteSources(id).then(res => {
        const list = (res && res.data) || []
        this.sourceRepos = Array.isArray(list) ? list : []
      }).catch(err => {
        this.sourceRepos = []
        const msg = (err && (err.msg || err.message)) || this.$t('git.repo.settings.members.invite.sources-load-failed')
        this.$modal.msgError(msg)
      }).finally(() => {
        this.sourcesLoading = false
      })
    },
    onSourceRepoChange(sourceId) {
      this.fromRepoForm.userIds = []
      this.sourceMembers = []
      if (this.$refs.fromRepoForm) this.$refs.fromRepoForm.clearValidate('userIds')
      const id = this.repositoryId
      if (!Number.isFinite(id) || id <= 0 || sourceId == null) return
      this.sourceMembersLoading = true
      listGitRepoInviteSourceMembers(id, sourceId).then(res => {
        const list = (res && res.data) || []
        this.sourceMembers = Array.isArray(list) ? list : []
      }).catch(err => {
        this.sourceMembers = []
        const msg = (err && (err.msg || err.message)) || this.$t('git.repo.settings.members.invite.source-members-load-failed')
        this.$modal.msgError(msg)
      }).finally(() => {
        this.sourceMembersLoading = false
      })
    },
    submitFromRepo() {
      this.$refs.fromRepoForm.validate(valid => {
        if (!valid) return
        const id = this.repositoryId
        if (!Number.isFinite(id) || id <= 0) return
        this.fromRepoSubmitting = true
        addGitRepoMembersFromRepo(id, {
          sourceRepositoryId: this.fromRepoForm.sourceRepositoryId,
          userIds: this.fromRepoForm.userIds,
          role: this.normalizeRole(this.fromRepoForm.role)
        }).then(res => {
          const data = (res && res.data) || {}
          const added = Number(data.added) || 0
          const skipped = Number(data.skipped) || 0
          this.$modal.msgSuccess(this.$t('git.repo.settings.members.invite.from-repo-result', {
            added,
            skipped
          }))
          this.$emit('added')
          this.dialogVisible = false
        }).catch(err => {
          const msg = (err && (err.msg || err.message)) || this.$t('git.repo.settings.members.invite.from-repo-failed')
          this.$modal.msgError(msg)
        }).finally(() => {
          this.fromRepoSubmitting = false
        })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.invite-link-hint {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-color-secondary, #909399);
}

.invite-qr {
  display: block;
  width: 160px;
  height: 160px;
  border-radius: 4px;
  background: #fff;
}

.invite-role-select,
.member-direct-select {
  width: 100%;
  max-width: 420px;
}

:deep(.invite-link-input.el-input-group .el-input__inner) {
  border-top-right-radius: 0 !important;
  border-bottom-right-radius: 0 !important;
}
</style>

<style lang="scss">
.git-repo-invite-member-dialog {
  .member-invite-dialog-tabs {
    .el-tabs__content,
    .el-tab-pane {
      overflow: visible !important;
    }

    .el-form-item__content {
      overflow: visible;
    }

    .member-direct-select,
    .invite-role-select {
      width: 100%;
      overflow: visible;

      .el-input {
        overflow: visible;
      }
    }
  }
}
</style>
