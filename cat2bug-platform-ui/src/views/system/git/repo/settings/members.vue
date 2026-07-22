<template>
  <div class="git-settings-members" v-loading="loading">
    <div class="git-settings-members__header">
      <div>
        <h3 class="git-settings-members__title">{{ $t('git.repo.settings.nav.members') }}</h3>
        <p class="git-settings-members__desc">{{ $t('git.repo.settings.members.desc') }}</p>
      </div>
      <el-button type="primary" plain size="small" @click="openInviteDialog">
        {{ $t('git.repo.settings.members.add') }}
      </el-button>
    </div>

    <el-table
      class="git-settings-members__table"
      :data="members"
      size="small"
      empty-text="—"
    >
      <el-table-column
        :label="$t('git.repo.settings.members.user')"
        min-width="280"
      >
        <template #default="{ row }">
          <div class="git-settings-members__user">
            <cat2-bug-avatar :member="row" :size="40" />
            <div class="git-settings-members__user-meta">
              <div class="git-settings-members__user-title">
                <span class="git-settings-members__nick">{{ displayName(row) }}</span>
                <el-tag
                  v-if="isRepoOwner(row)"
                  size="small"
                  effect="plain"
                  type="primary"
                  class="git-settings-members__tag"
                >
                  {{ $t('git.repo.settings.members.tag-owner') }}
                </el-tag>
                <el-tag
                  v-if="isMyself(row)"
                  size="small"
                  effect="plain"
                  type="success"
                  class="git-settings-members__tag"
                >
                  {{ $t('git.repo.settings.members.tag-myself') }}
                </el-tag>
              </div>
              <div class="git-settings-members__user-sub">
                <template v-if="row.userName">{{ row.userName }}</template>
                <template v-if="row.userName && row.email"> </template>
                <template v-if="row.email">({{ row.email }})</template>
              </div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('git.repo.settings.members.role')" width="160">
        <template #default="{ row }">
          <el-select
            :model-value="normalizeRole(row.role)"
            size="small"
            class="git-settings-members__role-select"
            :disabled="roleUpdatingId === row.userId"
            @change="val => onRoleChange(row, val)"
          >
            <el-option
              v-for="opt in roleOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column :label="$t('operate')" width="100" align="center">
        <template #default="{ row }">
          <el-button
            link
            size="small"
            class="red"
            :icon="Delete"
            :loading="removingId === row.userId"
            @click="removeMember(row)"
          >
            {{ $t('git.repo.settings.members.remove') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <invite-member-dialog
      ref="inviteDialog"
      :repository-id="repositoryId"
      :member-ids="memberUserIds"
      @added="loadMembers"
    />
  </div>
</template>

<script>
import { Delete } from '@element-plus/icons-vue'
import { markRaw } from 'vue'
import {
  delGitRepoMember,
  getGitRepo,
  listGitRepoMembers,
  updateGitRepoMember
} from '@/api/system/gitRepo'
import Cat2BugAvatar from '@/components/Cat2BugAvatar'
import { pinia } from '@/store/pinia.js'
import { useUserStore } from '@/stores/user'
import InviteMemberDialog from './components/InviteMemberDialog.vue'

const ROLE_VALUES = ['read', 'write', 'admin']

export default {
  name: 'GitRepoSettingsMembers',
  components: { Cat2BugAvatar, Delete, InviteMemberDialog },
  data() {
    return {
      Delete: markRaw(Delete),
      loading: false,
      members: [],
      roleUpdatingId: null,
      removingId: null,
      /** 仓库创建者用户名（sys_git_repository.create_by） */
      repoCreateBy: null
    }
  },
  computed: {
    repositoryId() {
      return Number(this.$route.params.repositoryId)
    },
    currentUserId() {
      return Number(useUserStore(pinia).id)
    },
    roleOptions() {
      return ROLE_VALUES.map(value => ({
        value,
        label: this.$t(`git.repo.settings.members.role-${value}`)
      }))
    },
    memberUserIds() {
      return this.members.map(m => m.userId).filter(id => id != null)
    }
  },
  watch: {
    '$route.params.repositoryId'() {
      this.bootstrap()
    }
  },
  created() {
    this.bootstrap()
  },
  methods: {
    normalizeRole(role) {
      const r = role != null ? String(role).toLowerCase() : ''
      return ROLE_VALUES.includes(r) ? r : 'read'
    },
    displayName(row) {
      return (row && (row.nickName || row.userName)) || String((row && row.userId) || '')
    },
    isMyself(row) {
      const uid = Number(row && row.userId)
      return Number.isFinite(uid) && uid > 0 && uid === this.currentUserId
    },
    isRepoOwner(row) {
      if (!this.repoCreateBy || !row) return false
      const by = String(this.repoCreateBy)
      if (row.userName && String(row.userName) === by) return true
      // create_by 偶发存 userId
      return row.userId != null && String(row.userId) === by
    },
    openInviteDialog() {
      if (this.$refs.inviteDialog) {
        this.$refs.inviteDialog.open()
      }
    },
    bootstrap() {
      this.loadRepoMeta()
      this.loadMembers()
    },
    loadRepoMeta() {
      const id = this.repositoryId
      if (!Number.isFinite(id) || id <= 0) {
        this.repoCreateBy = null
        return
      }
      getGitRepo(id).then(res => {
        const repo = (res && res.data) || {}
        this.repoCreateBy = repo.createBy || null
      }).catch(() => {
        this.repoCreateBy = null
      })
    },
    loadMembers() {
      const id = this.repositoryId
      if (!Number.isFinite(id) || id <= 0) {
        this.members = []
        return
      }
      this.loading = true
      listGitRepoMembers(id).then(res => {
        const list = (res && res.data) || []
        this.members = Array.isArray(list) ? list.map(m => ({
          ...m,
          role: this.normalizeRole(m.role)
        })) : []
      }).catch(() => {
        this.members = []
      }).finally(() => {
        this.loading = false
      })
    },
    onRoleChange(row, role) {
      const id = this.repositoryId
      const userId = row && row.userId
      const nextRole = this.normalizeRole(role)
      if (!Number.isFinite(id) || id <= 0 || userId == null) return
      if (this.normalizeRole(row.role) === nextRole) return
      const prev = row.role
      row.role = nextRole
      this.roleUpdatingId = userId
      updateGitRepoMember(id, userId, { role: nextRole }).then(() => {
        this.$modal.msgSuccess(this.$t('git.repo.settings.members.update-success'))
      }).catch(err => {
        row.role = prev
        const msg = (err && (err.msg || err.message)) || this.$t('git.repo.settings.members.update-failed')
        this.$modal.msgError(msg)
      }).finally(() => {
        this.roleUpdatingId = null
      })
    },
    removeMember(row) {
      const id = this.repositoryId
      const userId = row && row.userId
      if (!Number.isFinite(id) || id <= 0 || userId == null) return
      const name = this.displayName(row)
      this.$modal.confirm(
        this.$t('git.repo.settings.members.remove-confirm', { name }),
        this.$t('git.repo.settings.nav.members'),
        { confirmButtonText: this.$t('ok'), cancelButtonText: this.$t('cancel'), type: 'warning' }
      ).then(() => {
        this.removingId = userId
        return delGitRepoMember(id, userId).then(() => {
          this.$modal.msgSuccess(this.$t('git.repo.settings.members.remove-success'))
          this.loadMembers()
        })
      }).catch(err => {
        if (err === 'cancel' || err === 'close') return
        const msg = (err && (err.msg || err.message)) || this.$t('git.repo.settings.members.remove-failed')
        this.$modal.msgError(msg)
      }).finally(() => {
        this.removingId = null
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.git-settings-members {
  color: var(--text-color-regular);
}

.git-settings-members__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.git-settings-members__title {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--text-color-primary);
}

.git-settings-members__desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-color-secondary);
}

.git-settings-members__role-select {
  width: 100%;
  max-width: 180px;
}

.git-settings-members__table {
  width: 100%;
}

.git-settings-members__user {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  padding: 4px 0;
}

.git-settings-members__user-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.git-settings-members__user-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.git-settings-members__nick {
  color: var(--el-color-primary);
  font-weight: 500;
  line-height: 1.3;
}

.git-settings-members__tag {
  flex-shrink: 0;
}

.git-settings-members__user-sub {
  font-size: 12px;
  line-height: 1.3;
  color: var(--text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
