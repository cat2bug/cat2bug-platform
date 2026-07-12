<template>
  <div class="member-invite-link-panel">
    <el-form ref="linkForm" :model="linkForm" label-width="120px">
      <el-form-item :label="$t('role')">
        <el-select
          v-model="linkForm.roleIds"
          class="invite-role-select"
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
      <el-form-item v-if="linkForm.roleIds.length > 0" :label="$t('invite.link-url')">
        <el-input
          v-model="generatedUrl"
          class="invite-link-input"
          readonly
          style="width: 100%"
          :placeholder="generating ? $t('loading') : ''"
        >
          <el-button
            slot="append"
            :icon="$legacyIcon('el-icon-document-copy')"
            :disabled="!generatedUrl || generating"
            v-clipboard:copy="generatedUrl"
            v-clipboard:success="onCopySuccess"
          ></el-button>
        </el-input>
        <p class="invite-link-hint">{{ $t('invite.link-validity-hint') }}</p>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import {
  buildInviteUrl,
  createTeamInviteLink,
  createProjectInviteLink
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
      generating: false,
      generateRequestId: 0
    }
  },
  watch: {
    'linkForm.roleIds': {
      handler() {
        this.generateInviteLink()
      },
      deep: true
    },
    scopeId() {
      this.resetPanel()
    }
  },
  methods: {
    resetPanel() {
      this.generateRequestId += 1
      this.linkForm = { roleIds: [] }
      this.generatedUrl = ''
      this.generating = false
    },
    onActivated() {
      this.generateInviteLink()
    },
    resolveInviteUrl(row) {
      if (row.token) {
        return buildInviteUrl(row.token)
      }
      if (row.inviteUrl || row.url) {
        return row.inviteUrl || row.url
      }
      return ''
    },
    generateInviteLink() {
      if (!this.scopeId) {
        return
      }
      const roleIds = this.linkForm.roleIds
      if (!roleIds || roleIds.length === 0) {
        this.generatedUrl = ''
        this.generating = false
        return
      }
      const requestId = ++this.generateRequestId
      this.generating = true
      const createFn = this.scopeType === 'team' ? createTeamInviteLink : createProjectInviteLink
      createFn(this.scopeId, { roleIds }).then(res => {
        if (requestId !== this.generateRequestId) {
          return
        }
        const data = res.data || {}
        this.generatedUrl = this.resolveInviteUrl(data)
      }).catch(() => {
        if (requestId === this.generateRequestId) {
          this.generatedUrl = ''
        }
      }).finally(() => {
        if (requestId === this.generateRequestId) {
          this.generating = false
        }
      })
    },
    onCopySuccess() {
      this.$modal.msgSuccess(this.$t('invite.copy-success'))
    }
  }
}
</script>

<style lang="scss" scoped>
.member-invite-link-panel {
  overflow: visible;

  ::v-deep .el-form-item__content {
    overflow: visible;
  }

  ::v-deep .invite-role-select {
    width: 100%;
    overflow: visible;

    .el-input {
      overflow: visible;
    }
  }

  ::v-deep .invite-link-input.el-input-group .el-input__inner {
    border-top-right-radius: 0 !important;
    border-bottom-right-radius: 0 !important;
  }

  .invite-link-hint {
    margin: 6px 0 0;
    font-size: 12px;
    color: #909399;
    line-height: 1.5;
  }
}
</style>

<!-- 避免 Tab 区域 overflow 裁切 el-select 焦点环（box-shadow） -->
<style lang="scss">
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
</style>
