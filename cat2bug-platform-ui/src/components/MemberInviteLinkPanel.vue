<template>
  <div class="member-invite-link-panel">
    <el-form ref="linkForm" :model="linkForm" label-width="120px">
      <el-form-item :label="$t('role')">
        <el-select
          v-model="linkForm.roleIds"
          multiple
          :placeholder="$t('member.please-select-role')"
          style="width: 100%"
        >
          <el-option
            v-for="item in roleOptions"
            :key="item.roleId"
            :label="item.roleName"
            :value="item.roleId"
            :disabled="item.status == 1"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="generating" @click="handleGenerate">
          {{ $t('invite.generate-link') }}
        </el-button>
      </el-form-item>
      <el-form-item v-if="generatedUrl" :label="$t('invite.link-url')">
        <el-input :value="generatedUrl" readonly>
          <el-button
            slot="append"
            v-clipboard:copy="generatedUrl"
            v-clipboard:success="onCopySuccess"
          >{{ $t('copy') }}</el-button>
        </el-input>
      </el-form-item>
    </el-form>

    <div v-loading="listLoading" class="active-links-section">
      <div class="active-links-header">
        <span>{{ $t('invite.active-links') }}</span>
        <el-button type="text" icon="el-icon-refresh" @click="loadActiveLinks" />
      </div>
      <el-empty v-if="!listLoading && activeLinks.length === 0" :description="$t('invite.no-active-links')" />
      <el-table v-else :data="activeLinks" size="small">
        <el-table-column :label="$t('invite.link-url')" min-width="200" show-overflow-tooltip>
          <template slot-scope="scope">
            {{ resolveInviteUrl(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('invite.expires-at')" width="160">
          <template slot-scope="scope">
            {{ formatExpiresAt(scope.row.expiresAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('operate')" width="120" align="center">
          <template slot-scope="scope">
            <el-button
              type="text"
              size="mini"
              icon="el-icon-document-copy"
              v-clipboard:copy="resolveInviteUrl(scope.row)"
              v-clipboard:success="onCopySuccess"
            />
            <el-button
              type="text"
              size="mini"
              icon="el-icon-delete"
              :disabled="scope.row.revoked"
              @click="handleRevoke(scope.row)"
            />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
import { parseTime } from '@/utils/ruoyi'
import {
  buildInviteUrl,
  createTeamInviteLink,
  listTeamInviteLinks,
  revokeTeamInviteLink,
  createProjectInviteLink,
  listProjectInviteLinks,
  revokeProjectInviteLink
} from '@/api/system/memberInvite'

export default {
  name: 'MemberInviteLinkPanel',
  props: {
    scopeType: {
      type: String,
      required: true,
      validator: v => ['team', 'project'].includes(v)
    },
    scopeId: {
      type: Number,
      required: true
    },
    roleOptions: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      linkForm: {
        roleIds: []
      },
      generatedUrl: '',
      activeLinks: [],
      generating: false,
      listLoading: false
    }
  },
  watch: {
    scopeId() {
      this.resetPanel()
      this.loadActiveLinks()
    }
  },
  methods: {
    resetPanel() {
      this.linkForm = { roleIds: [] }
      this.generatedUrl = ''
      this.activeLinks = []
    },
    onActivated() {
      this.loadActiveLinks()
    },
    resolveInviteUrl(row) {
      if (row.inviteUrl || row.url) {
        return row.inviteUrl || row.url
      }
      if (row.token) {
        return buildInviteUrl(row.token)
      }
      return ''
    },
    formatExpiresAt(expiresAt) {
      if (!expiresAt) {
        return '-'
      }
      return parseTime(expiresAt, '{y}-{m}-{d} {h}:{i}')
    },
    loadActiveLinks() {
      if (!this.scopeId) {
        return
      }
      this.listLoading = true
      const listFn = this.scopeType === 'team' ? listTeamInviteLinks : listProjectInviteLinks
      listFn(this.scopeId).then(res => {
        const rows = res.data || res.rows || []
        this.activeLinks = rows.filter(item => !item.revoked && !this.isExpired(item))
      }).finally(() => {
        this.listLoading = false
      })
    },
    isExpired(row) {
      if (!row.expiresAt) {
        return false
      }
      return new Date(row.expiresAt).getTime() <= Date.now()
    },
    handleGenerate() {
      if (!this.linkForm.roleIds || this.linkForm.roleIds.length === 0) {
        this.$modal.msgWarning(this.$t('invite.please-select-role'))
        return
      }
      this.generating = true
      const createFn = this.scopeType === 'team' ? createTeamInviteLink : createProjectInviteLink
      createFn(this.scopeId, { roleIds: this.linkForm.roleIds }).then(res => {
        const data = res.data || {}
        this.generatedUrl = this.resolveInviteUrl(data)
        this.$modal.msgSuccess(this.$t('invite.generate-success'))
        this.loadActiveLinks()
      }).finally(() => {
        this.generating = false
      })
    },
    handleRevoke(row) {
      const inviteId = row.inviteId || row.id
      this.$modal.confirm(this.$t('invite.revoke-confirm')).then(() => {
        const revokeFn = this.scopeType === 'team' ? revokeTeamInviteLink : revokeProjectInviteLink
        return revokeFn(this.scopeId, inviteId)
      }).then(() => {
        this.$modal.msgSuccess(this.$t('invite.revoke-success'))
        if (this.generatedUrl && this.resolveInviteUrl(row) === this.generatedUrl) {
          this.generatedUrl = ''
        }
        this.loadActiveLinks()
      }).catch(() => {})
    },
    onCopySuccess() {
      this.$modal.msgSuccess(this.$t('invite.copy-success'))
    }
  }
}
</script>

<style lang="scss" scoped>
.member-invite-link-panel {
  .active-links-section {
    margin-top: 8px;
  }
  .active-links-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
    font-weight: 500;
  }
}
</style>
