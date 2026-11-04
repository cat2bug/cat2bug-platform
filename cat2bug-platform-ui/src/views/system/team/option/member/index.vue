<template>
  <div
    class="app-container case-body team-member-page team-member-list-layout project-list-page-host"
    ref="teamMemberMain"
  >
    <el-row class="project-add-page-header project-add-page-header--with-filter team-member-hint-back">
      <el-page-header @back="goBack" :content="$t('team.member')">
      </el-page-header>
    </el-row>

    <div ref="teamMemberTools" class="team-member-tools project-option-list-tools">
      <el-form
        ref="queryForm"
        size="small"
        :inline="true"
        class="left"
        :class="{ 'list-query-keyboard-nav': listQueryNavActive }"
      >
        <el-form-item class="list-query-nav-item team-member-hint-query" data-query-key="search">
          <el-input
            v-model="queryParams.params.search"
            :prefix-icon="$legacyIcon('el-icon-search')"
            :placeholder="$t('team.search-placeholder')"
            clearable
            style="width: 320px"
            size="small"
            @input="memberSearchHandle"
          />
        </el-form-item>
      </el-form>
      <div ref="teamMemberToolsRight" class="project-option-list-tools-right">
        <el-button
          class="team-member-hint-create"
          type="primary"
          plain
          :icon="$legacyIcon('el-icon-plus')"
          size="small"
          @click="createMemberHandle"
          v-hasPermi="['system:team:member:create']"
        >{{$t('member.create')}}</el-button>
        <el-button
          class="team-member-hint-invite cat2bug-btn-outline"
          type="primary"
          plain
          :icon="$legacyIcon('el-icon-plus')"
          size="small"
          @click="inviteMemberHandle"
          v-hasPermi="['system:team:member:invite']"
        >{{$t('team.invite-members')}}</el-button>
      </div>
    </div>

    <div ref="teamMemberListBody" class="team-member-list-body">
      <div class="team-member-table-x-scroll">
        <el-table
          ref="teamMemberTable"
          class="team-member-list-table"
          v-loading="loading"
          :data="memberList"
          :max-height="tableBodyMaxHeight"
        >
          <el-table-column :label="$t('member.name')" align="left" key="nickName" prop="nickName" min-width="260" :show-overflow-tooltip="true">
            <template #default="{ row }">
              <member-nameplate :member="row"></member-nameplate>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('role')"
            align="left"
            min-width="220"
          >
            <template #default="{ row }">
              <member-role-select
                v-if="row.userId !== 1"
                v-model="row.roleIds"
                :options="selectRoleOptions(row)"
                :placeholder="$t('member.please-select-role')"
                :disabled="isCreateBy(row)"
                @change="(ids) => roleChangeHandle(row, ids)"
              />
            </template>
          </el-table-column>
          <el-table-column :label="$t('email')" align="left" key="email" prop="email" :show-overflow-tooltip="true" width="300">
            <template #default="{ row }">
              <span>{{ row.email }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('phone-number')" align="left" key="phoneNumber" prop="phoneNumber" width="150">
            <template #default="{ row }">
              <span>{{ row.phoneNumber }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('register-time')" align="left" key="createTime" prop="createTime" width="180">
            <template #default="{ row }">
              <span>{{ row.createTime }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('operate')"
            align="center"
            width="160"
            class-name="small-padding fixed-width"
          >
            <template #default="{ row }">
              <!-- 转让：仅创建人本人可见；其他人看创建人行无操作 -->
              <el-button
                v-if="canTransferCreator(row)"
                size="small"
                link
                :icon="$legacyIcon('el-icon-s-custom')"
                @click="transferCreatorHandle(row)"
              >{{$t('team.transfer-creator')}}</el-button>
              <el-button
                v-else-if="!isCreateBy(row)"
                size="small"
                link
                type="danger"
                :icon="$legacyIcon('el-icon-delete')"
                @click="removeMemberHandle(row)"
                v-hasPermi="['system:team:edit']"
              >{{$t('delete')}}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div
        v-show="total>0"
        ref="teamMemberPaginationBand"
        class="team-member-table-pagination-band table-pagination-band"
      >
        <pagination
          class="team-member-table-pagination"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getMemberList"
        />
      </div>
    </div>

    <create-team-member ref="createTeamMemberDialog" @create="getMemberList" />
    <invite-team-member ref="inviteTeamMemberDialog" @invite="getMemberList" />
    <el-dialog
      v-model="transferOpen"
      :title="$t('team.transfer-creator-title')"
      width="480px"
      append-to-body
    >
      <el-form label-width="100px" size="small">
        <el-form-item :label="$t('team.transfer-target')">
          <el-select
            v-model="transferTargetUserId"
            filterable
            clearable
            style="width: 100%"
            :filter-method="filterTransferCandidate"
            popper-class="team-transfer-member-select-dropdown"
            :placeholder="$t('team.transfer-target-placeholder')"
          >
            <el-option
              v-for="m in filteredTransferCandidates"
              :key="m.userId"
              :label="m.nickName || m.userName || String(m.userId)"
              :value="m.userId"
            >
              <member-nameplate :member="m" size="default" />
            </el-option>
          </el-select>
        </el-form-item>
        <p class="team-transfer-hint">{{ $t('team.transfer-creator-hint') }}</p>
      </el-form>
      <template #footer>
        <el-button size="small" @click="transferOpen = false">{{ $t('cancel') }}</el-button>
        <el-button type="primary" size="small" :loading="transferSubmitting" @click="confirmTransfer">
          {{ $t('confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {
  listMember,
  listTeamRole,
  updateMemberTeamRoleIds,
  removeTeamMember,
  transferTeamCreator
} from "@/api/system/team";
import CreateTeamMember from "@/views/system/team/option/member/CreateTeamMember";
import InviteTeamMember from "@/views/system/team/option/member/InviteTeamMember";
import MemberNameplate from "@/components/MemberNameplate";
import MemberRoleSelect from "@/components/MemberRoleSelect";
import pageActionHints from '@/mixins/page-action-hints'
import listQueryKeyboardNav from '@/mixins/list-query-keyboard-nav'
import teamMemberListKbd from '@/mixins/team-member-list-kbd'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { checkPermi } from '@/utils/permission'
import {
  buildProjectOptionSubPaginationShortcuts,
  clickProjectOptionSubPagination
} from '@/utils/project-option-sub-list-kbd'
import { storeToRefs } from 'pinia';
import { useUserStore } from '@/stores/user'

const TEAM_MEMBER_KBD_SCOPE = 'team-member'

export default {
  name: "TeamMemberManage",
  mixins: [pageActionHints, listQueryKeyboardNav, teamMemberListKbd],
  components: { CreateTeamMember, InviteTeamMember, MemberNameplate, MemberRoleSelect },
  setup() {
    const userStore = useUserStore()
    const { config, id: currentUserId } = storeToRefs(userStore)
    return { config, userStore, currentUserId }
  },
  data() {
    return {
      loading: true,
      total: 0,
      showSearch: true,
      memberSearch: null,
      memberList:[],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        params: {},
      },
      roleOptions: [],
      transferOpen: false,
      transferTargetUserId: null,
      transferSubmitting: false,
      transferFilterKeyword: '',
      tableBodyMaxHeight: null,
    }
  },
  computed: {
    transferCandidates() {
      return (this.memberList || []).filter(m => m && m.userId != null && !this.isCreateBy(m) && m.userId !== 1)
    },
    filteredTransferCandidates() {
      const kw = (this.transferFilterKeyword || '').trim().toLowerCase()
      if (!kw) return this.transferCandidates
      return this.transferCandidates.filter(m => {
        const parts = [m.nickName, m.userName, m.phoneNumber, m.email]
        return parts.some(v => v != null && String(v).toLowerCase().includes(kw))
      })
    },
  },
  created() {
    this.init();
    this.getMemberList();
  },
  mounted() {
    this.registerTeamMemberShortcuts();
    this.$nextTick(() => {
      this.$_bindListQueryNavFocusIn()
      this.$_bindListQueryNavToolbarFocusIn()
      this.initTeamMemberListBodyResizeObserver()
      this.syncTeamMemberTableBodyMaxHeight()
    })
    window.addEventListener('resize', this.onTeamMemberWindowResize)
  },
  activated() {
    this.registerTeamMemberShortcuts();
    this.$nextTick(() => {
      this.$_bindListQueryNavFocusIn()
      this.$_bindListQueryNavToolbarFocusIn()
      this.syncTeamMemberTableBodyMaxHeight()
    })
  },
  deactivated() {
    if (this.$shortcut) this.$shortcut.unregisterPage(TEAM_MEMBER_KBD_SCOPE);
  },
  beforeDestroy() {
    if (this.$shortcut) this.$shortcut.unregisterPage(TEAM_MEMBER_KBD_SCOPE);
    window.removeEventListener('resize', this.onTeamMemberWindowResize)
    this.destroyTeamMemberListBodyResizeObserver()
  },
  methods: {
    registerTeamMemberShortcuts() {
      if (!this.$shortcut) return
      const pagination = buildProjectOptionSubPaginationShortcuts(this)
      this.$shortcut.registerPage(TEAM_MEMBER_KBD_SCOPE, [
        { key: 'create', defaultLetter: 'E', run: () => this.shortcutCreateMember() },
        { key: 'invite', defaultLetter: 'I', run: () => this.shortcutInviteMember() },
        { key: 'back', defaultLetter: 'B', run: () => this.goBack() },
        ...pagination.register
      ])
    },
    getPageActionHintContainer() {
      return this.$refs.teamMemberMain || this.$el
    },
    getPageActionHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${TEAM_MEMBER_KBD_SCOPE}.${key}`, def)
      const pagination = buildProjectOptionSubPaginationShortcuts(this)
      return [
        {
          key: 'create',
          letter: L('create', 'E'),
          badgeSelector: '.team-member-hint-create',
          floatOffset: { placement: 'bottom-right-outset', outset: 2, dy: 5 },
          run: () => this.shortcutCreateMember(),
          visible: () => checkPermi(['system:team:member:create'])
        },
        {
          key: 'invite',
          letter: L('invite', 'I'),
          badgeSelector: '.team-member-hint-invite',
          floatOffset: { placement: 'bottom-right-outset', outset: 2, dy: 5 },
          run: () => this.shortcutInviteMember(),
          visible: () => checkPermi(['system:team:member:invite'])
        },
        {
          key: 'back',
          letter: L('back', 'B'),
          badgeSelector: '.team-member-hint-back .el-page-header__left',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.goBack()
        },
        ...pagination.hints
      ]
    },
    getListQueryNavItems() {
      return [{ key: 'search' }]
    },
    getListQueryNavToolbarRef() {
      return 'teamMemberToolsRight'
    },
    getProjectOptionSubKbdScope() {
      return TEAM_MEMBER_KBD_SCOPE
    },
    getProjectOptionSubPaginationSelector() {
      return '.team-member-table-pagination'
    },
    shortcutChangePage(delta) {
      clickProjectOptionSubPagination(this, delta)
    },
    shortcutFocusQuery() {
      this.enterListQueryKeyboardNav()
    },
    shortcutCreateMember() {
      if (!checkPermi(['system:team:member:create'])) return
      this.createMemberHandle()
    },
    shortcutInviteMember() {
      if (!checkPermi(['system:team:member:invite'])) return
      this.inviteMemberHandle()
    },
    initTeamMemberListBodyResizeObserver() {
      if (typeof ResizeObserver === 'undefined') return
      this.destroyTeamMemberListBodyResizeObserver()
      const el = this.$refs.teamMemberListBody
      if (!el) return
      this._teamMemberListBodyResizeObserver = new ResizeObserver(() => {
        this.syncTeamMemberTableBodyMaxHeight()
      })
      this._teamMemberListBodyResizeObserver.observe(el)
    },
    destroyTeamMemberListBodyResizeObserver() {
      if (this._teamMemberListBodyResizeObserver) {
        this._teamMemberListBodyResizeObserver.disconnect()
        this._teamMemberListBodyResizeObserver = null
      }
    },
    onTeamMemberWindowResize() {
      this.syncTeamMemberTableBodyMaxHeight()
    },
    syncTeamMemberTableBodyMaxHeight() {
      this.$nextTick(() => {
        const body = this.$refs.teamMemberListBody
        if (!body || !body.clientHeight) return
        const pagEl = this.$refs.teamMemberPaginationBand
        let reserveBelowTable = 0
        if (this.total > 0 && pagEl && pagEl.offsetParent !== null) {
          const st = window.getComputedStyle(pagEl)
          reserveBelowTable =
            pagEl.offsetHeight +
            parseFloat(st.marginTop || '0') +
            parseFloat(st.marginBottom || '0')
        }
        const next = Math.max(120, Math.floor(body.clientHeight - reserveBelowTable - 2))
        if (this.tableBodyMaxHeight !== next) {
          this.tableBodyMaxHeight = next
          this.$nextTick(() => {
            const table = this.$refs.teamMemberTable
            if (table && typeof table.doLayout === 'function') {
              table.doLayout()
            }
          })
        }
      })
    },
    init() {
      listTeamRole(this.getTeamId()).then(res => {
        this.roleOptions = res.data?res.data.filter(r=>r.isTeamRole).map(r=>{
          r.roleName = r.roleNameI18nKey?this.$t(r.roleNameI18nKey):r.roleName;
          return r;
        }):[];
      });
    },
    getTeamId() {
      return this.config.currentTeamId;
    },
    isCreateBy(member) {
      return member.admin || (this.roleOptions.filter(r => r.teamCreateBy && member.roleIds && member.roleIds.indexOf(r.roleId) > -1).length > 0);
    },
    /** 仅团队创建人本人可转让（管理员等其它角色不可见） */
    canTransferCreator(row) {
      if (!this.isCreateBy(row) || row == null || row.userId == null) {
        return false
      }
      return Number(row.userId) === Number(this.currentUserId)
    },
    selectRoleOptions(member) {
      if (this.isCreateBy(member)) {
        return this.roleOptions.filter(r => r.teamCreateBy);
      }
      return this.roleOptions.filter(r => r.teamCreateBy == false);
    },
    filterTransferCandidate(query) {
      this.transferFilterKeyword = query == null ? '' : String(query)
    },
    memberSearchHandle(e){
      this.queryParams.params.search=e;
      this.getMemberList();
    },
    getMemberList() {
      this.loading = true;
      return listMember(this.getTeamId(),this.queryParams).then(res => {
        this.loading = false;
        this.memberList = (res.rows || []).map(row => {
          const ids = Array.isArray(row.roleIds)
            ? row.roleIds
            : (row.roles || []).map(r => r.roleId).filter(id => id != null)
          return { ...row, roleIds: ids }
        });
        this.total = res.total;
        this.syncTeamMemberTableBodyMaxHeight()
      }).catch(() => {
        this.loading = false;
      });
    },
    roleChangeHandle(member, roleIds) {
      const ids = Array.isArray(roleIds) ? roleIds : (member.roleIds || [])
      if (!ids.length) {
        this.$modal.msgError(this.$t('member.please-select-role'))
        this.getMemberList()
        return
      }
      member.roleIds = ids
      updateMemberTeamRoleIds(
        this.getTeamId(),
        member.userId,
        ids
      ).then(res=>{
        this.$message.success(this.$i18n.t('update.success'));
        this.getMemberList();
      }).catch(() => {
        this.getMemberList()
      });
    },
    goBack() {
      this.$router.back();
    },
    inviteMemberHandle() {
      this.$refs.inviteTeamMemberDialog.open();
    },
    createMemberHandle() {
      this.$refs.createTeamMemberDialog.open();
    },
    removeMemberHandle(member) {
      const name = member.nickName || member.userName || member.userId
      this.$modal.confirm(this.$t('team.remove-member-confirm', { name })).then(() => {
        return removeTeamMember(this.getTeamId(), member.userId)
      }).then(() => {
        this.$message.success(this.$t('team.remove-member-success'))
        this.getMemberList()
      }).catch(() => {})
    },
    transferCreatorHandle() {
      const self = (this.memberList || []).find(m => Number(m.userId) === Number(this.currentUserId))
      if (!self || !this.canTransferCreator(self)) {
        this.$modal.msgError(this.$t('team.transfer-creator-only'))
        return
      }
      this.transferTargetUserId = null
      this.transferFilterKeyword = ''
      this.transferOpen = true
    },
    confirmTransfer() {
      const self = (this.memberList || []).find(m => Number(m.userId) === Number(this.currentUserId))
      if (!self || !this.canTransferCreator(self)) {
        this.$modal.msgError(this.$t('team.transfer-creator-only'))
        return
      }
      if (!this.transferTargetUserId) {
        this.$modal.msgError(this.$t('team.transfer-target-placeholder'))
        return
      }
      this.transferSubmitting = true
      transferTeamCreator(this.getTeamId(), this.transferTargetUserId).then(() => {
        this.$message.success(this.$t('team.transfer-creator-success'))
        this.transferOpen = false
        // 原创建人已变为管理员：立刻刷新登录态角色/权限与菜单，并重拉成员列表
        return this.userStore.GetInfo().then(() => {
          return import('@/stores/permission').then(({ usePermissionStore }) => {
            const permissionStore = usePermissionStore()
            permissionStore.SetRoutesRefreshing(true)
            return permissionStore.GenerateRoutes().finally(() => {
              permissionStore.SetRoutesRefreshing(false)
            })
          })
        }).catch(() => {}).then(() => this.getMemberList())
      }).catch(() => {
        this.$modal.msgError(this.$t('team.transfer-creator-fail'))
      }).finally(() => {
        this.transferSubmitting = false
      })
    },
  }
}
</script>

<style lang="scss" scoped>
.app-container.case-body {
  padding: 20px 20px 0;
  box-sizing: border-box;
}
.team-member-page.team-member-list-layout {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  min-height: 0;
  flex: 1 1 auto;
  width: 100%;
  box-sizing: border-box;
}
.team-member-tools {
  flex-shrink: 0;
  width: 100%;
  margin-bottom: 0;
}
.team-member-list-body {
  flex: 1 1 0%;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.team-member-table-x-scroll {
  width: 100%;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
}
.team-member-table-pagination-band {
  flex-shrink: 0;
}
.team-transfer-hint {
  margin: 0 0 0 100px;
  color: var(--el-text-color-secondary, #909399);
  font-size: 12px;
  line-height: 1.5;
}
</style>

<style lang="scss">
/* 下拉挂到 body，须非 scoped */
.team-transfer-member-select-dropdown.el-select-dropdown {
  min-width: 420px !important;
}
.team-transfer-member-select-dropdown .el-select-dropdown__wrap {
  max-height: 360px !important;
}
.team-transfer-member-select-dropdown .el-select-dropdown__item {
  height: auto;
  min-height: 60px;
  padding-top: 6px;
  padding-bottom: 6px;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  flex-direction: row;
  flex-wrap: nowrap;
  line-height: normal;
}
.team-transfer-member-select-dropdown .member-nameplate {
  width: 100%;
  padding: 2px 0;
}
.team-transfer-member-select-dropdown .member-nameplate-content p {
  margin: 0;
  font-size: 14px;
  color: var(--text-color-primary);
}
.team-transfer-member-select-dropdown .member-nameplate-content span {
  font-size: 12px;
  line-height: 16px;
  height: auto;
  color: var(--text-color-secondary);
}
</style>
