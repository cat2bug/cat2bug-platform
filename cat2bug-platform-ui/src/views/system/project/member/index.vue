<template>
  <div class="app-container" ref="projectOptionSubMain">
    <el-row class="project-add-page-header project-add-page-header--with-filter project-option-sub-hint-back">
      <el-page-header @back="goBack" :content="$t('project.member-manage')">
      </el-page-header>
    </el-row>
    <el-row :gutter="20">
      <!--用户数据-->
      <el-col :span="24">
        <div class="project-member-tools project-option-list-tools mb8">
          <el-form
            ref="queryForm"
            size="small"
            :inline="true"
            class="left"
            :class="{ 'list-query-keyboard-nav': listQueryNavActive }"
          >
            <el-form-item class="list-query-nav-item project-member-hint-query" data-query-key="search">
              <el-input
                v-model="queryParams.params.search"
                :prefix-icon="$legacyIcon('el-icon-search')"
                :placeholder="$t('member.search-placeholder')"
                clearable
                style="width: 320px"
                size="small"
                @input="memberSearchHandle"
              />
            </el-form-item>
          </el-form>
          <div ref="memberToolsRight" class="project-option-list-tools-right member-tools">
            <el-button
              class="project-member-hint-add"
              type="primary"
              plain
              :icon="$legacyIcon('el-icon-user-solid')"
              size="small"
              @click="addMemberHandle"
              v-hasPermi="['system:project:list']"
            >{{$t('project.add-member')}}</el-button>
          </div>
        </div>

        <el-table v-loading="loading" :data="memberList">
          <el-table-column :label="$t('member.name')" align="left" key="nickName" prop="nickName" min-width="200" :show-overflow-tooltip="true">
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
                :disabled="isMemberDisabled(row)"
                @change="(ids) => roleChangeHandle(row, ids)"
              />
            </template>
          </el-table-column>
          <el-table-column :label="$t('email')" align="left" key="email" prop="email" :show-overflow-tooltip="true" width="300">
            <template #default="{ row }">
              <span :teamLock="row.status">{{ row.email }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('phone-number')" align="left" key="phoneNumber" prop="phoneNumber" width="150">
            <template #default="{ row }">
              <span :teamLock="row.status">{{ row.phoneNumber }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('register-time')" align="left" key="createTime" prop="createTime" width="180">
            <template #default="{ row }">
              <div :teamLock="row.status">
                <span :teamLock="row.status">{{ row.createTime }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('operate')"
            align="center"
            width="160"
            class-name="small-padding fixed-width"
          >
            <template #default="{ row }">
              <el-button
                v-if="canTransferCreator(row)"
                size="small"
                link
                :icon="$legacyIcon('el-icon-s-custom')"
                @click="transferCreatorHandle()"
              >{{$t('project.transfer-creator')}}</el-button>
              <el-button
                v-else-if="row.userId !== 1"
                size="small"
                link
                :icon="$legacyIcon('el-icon-delete')"
                @click="deleteHandle(row)"
                v-show="visibleDelete(row)"
              >{{$t('delete')}}</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination
          class="project-member-table-pagination"
          v-show="total>0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getMemberList"
        />
      </el-col>
    </el-row>
    <add-project-member ref="addProjectMemberDialog" @invite="getMemberList" />

    <el-dialog
      v-model="handoffDialogVisible"
      :title="$t('project.remove-member-handoff-title')"
      width="520px"
      append-to-body
      :close-on-click-modal="false"
      @closed="resetHandoffDialog"
    >
      <p class="project-member-handoff-hint">{{ $t('project.remove-member-handoff-hint') }}</p>
      <div
        v-if="handoffPreview && handoffPreview.soleHandlerDefects && handoffPreview.soleHandlerDefects.length"
        class="project-member-handoff-block"
      >
        <div class="project-member-handoff-block__title">
          {{ $t('project.remove-member-sole-defects', { count: handoffPreview.soleHandlerDefects.length }) }}
        </div>
        <ul class="project-member-handoff-list">
          <li v-for="d in handoffPreview.soleHandlerDefects.slice(0, 20)" :key="d.defectId">
            #{{ d.projectNum }} {{ d.defectName }}
          </li>
          <li v-if="handoffPreview.soleHandlerDefects.length > 20">…</li>
        </ul>
      </div>
      <div
        v-if="handoffPreview && handoffPreview.lastAdminRepositories && handoffPreview.lastAdminRepositories.length"
        class="project-member-handoff-block"
      >
        <div class="project-member-handoff-block__title">
          {{ $t('project.remove-member-last-admin-repos', { count: handoffPreview.lastAdminRepositories.length }) }}
        </div>
        <ul class="project-member-handoff-list">
          <li v-for="r in handoffPreview.lastAdminRepositories" :key="r.repositoryId">
            {{ r.name || r.slug }}
          </li>
        </ul>
      </div>
      <p
        v-if="handoffPreview && handoffPreview.multiHandlerDefectCount > 0"
        class="project-member-handoff-multi"
      >
        {{ $t('project.remove-member-multi-hint', { count: handoffPreview.multiHandlerDefectCount }) }}
      </p>
      <el-form ref="handoffForm" :model="handoffForm" :rules="handoffRules" label-width="90px">
        <el-form-item :label="$t('project.remove-member-handoff-user')" prop="handoffUserIds">
          <select-project-member
            ref="handoffMemberSelect"
            v-model:memberId="handoffForm.handoffUserIds"
            :project-id="currentProjectId"
            :multiple="false"
            :is-head="false"
            :enabled-member-ids="handoffEnabledMemberIds"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handoffDialogVisible = false">{{ $t('cancel') }}</el-button>
        <el-button type="danger" :loading="handoffSubmitting" @click="submitHandoffRemove">
          {{ $t('project.remove-member-confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="transferOpen"
      :title="$t('project.transfer-creator-title')"
      width="480px"
      append-to-body
    >
      <el-form label-width="100px" size="small">
        <el-form-item :label="$t('project.transfer-target')">
          <el-select
            v-model="transferTargetUserId"
            filterable
            clearable
            style="width: 100%"
            :filter-method="filterTransferCandidate"
            popper-class="project-transfer-member-select-dropdown"
            :placeholder="$t('project.transfer-target-placeholder')"
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
        <p class="project-transfer-hint">{{ $t('project.transfer-creator-hint') }}</p>
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
import AddProjectMember from "@/views/system/project/member/AddProjectMember";
import MemberNameplate from "@/components/MemberNameplate";
import MemberRoleSelect from "@/components/MemberRoleSelect";
import SelectProjectMember from "@/components/Project/SelectProjectMember";
import {
  delMemberOfProject,
  listMemberOfProject,
  listProjectRole,
  previewRemoveMemberOfProject,
  transferProjectCreator,
  updateMemberRoleOfProject,
} from "@/api/system/project";
import i18n from "@/utils/i18n/i18n";
import {checkPermi} from "@/utils/permission";
import projectOptionSubListKbd from '@/mixins/project-option-sub-list-kbd'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { PROJECT_OPTION_KBD_SCOPE } from '@/mixins/project-option-sub-kbd'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'

export default {
  name: "ProjectMemberManage",
  mixins: [projectOptionSubListKbd],
  components: { AddProjectMember, MemberNameplate, MemberRoleSelect, SelectProjectMember },
  setup() {
    const userStore = useUserStore()
    const { id, config } = storeToRefs(userStore)
    return { id, config, userStore }
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
      handoffDialogVisible: false,
      handoffSubmitting: false,
      handoffPreview: null,
      handoffTargetMember: null,
      handoffForm: {
        handoffUserIds: [],
      },
      handoffRules: {
        handoffUserIds: [
          {
            type: 'array',
            required: true,
            min: 1,
            message: i18n.t('project.remove-member-handoff-required').toString(),
            trigger: 'change',
          },
        ],
      },
      transferOpen: false,
      transferSubmitting: false,
      transferTargetUserId: null,
      transferFilterKeyword: '',
    }
  },
  created() {
    this.getRoleList();
  },
  computed: {
    currentUserId() {
      return this.id;
    },
    currentProjectId() {
      return parseInt(this.config.currentProjectId);
    },
    handoffEnabledMemberIds() {
      const excludeId = this.handoffTargetMember && this.handoffTargetMember.userId;
      return (this.memberList || [])
        .map((m) => m.userId)
        .filter((id) => id != null && id !== excludeId);
    },
    filteredTransferCandidates() {
      const selfId = Number(this.currentUserId)
      const kw = (this.transferFilterKeyword || '').trim().toLowerCase()
      return (this.memberList || []).filter((m) => {
        if (m == null || m.userId == null || Number(m.userId) === selfId) return false
        if (!kw) return true
        const nick = (m.nickName || '').toLowerCase()
        const name = (m.userName || '').toLowerCase()
        const phone = (m.phoneNumber || '').toLowerCase()
        const email = (m.email || '').toLowerCase()
        return nick.includes(kw) || name.includes(kw) || phone.includes(kw) || email.includes(kw)
      })
    },
  },
  methods: {
    isProjectCreator(member) {
      return member && member.roles && member.roles.filter(r => r.projectCreateBy).length > 0
    },
    /** 仅项目创建人本人可转让 */
    canTransferCreator(row) {
      if (!this.isProjectCreator(row) || row == null || row.userId == null) {
        return false
      }
      return Number(row.userId) === Number(this.currentUserId)
    },
    selectRoleOptions(member) {
      if (this.isProjectCreator(member)) {
        return this.roleOptions.filter(r => r.projectCreateBy);
      }
      return this.roleOptions.filter(r => r.projectCreateBy == false);
    },
    isMemberDisabled(member) {
      return member.status == 1 || this.isProjectCreator(member);
    },
    visibleDelete(member) {
      return checkPermi(['system:project:member:remove']) && !this.isProjectCreator(member);
    },
    filterTransferCandidate(query) {
      this.transferFilterKeyword = query == null ? '' : String(query)
    },
    shouldProjectOptionSubEscGoBack() {
      if (this.handoffDialogVisible || this.transferOpen) return false
      const dlg = this.$refs.addProjectMemberDialog
      return !(dlg && dlg.dialogVisible)
    },
    getListQueryNavItems() {
      return [{ key: 'search' }]
    },
    getListQueryNavToolbarRef() {
      return 'memberToolsRight'
    },
    getProjectOptionSubPaginationSelector() {
      return '.project-member-table-pagination'
    },
    getProjectOptionSubRegisterActions() {
      return [
        { key: 'create', defaultLetter: 'E', run: () => this.shortcutAddMember() }
      ]
    },
    getProjectOptionSubActionHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${PROJECT_OPTION_KBD_SCOPE}.${key}`, def)
      return [
        {
          key: 'create',
          letter: L('create', 'E'),
          badgeSelector: '.project-member-hint-add',
          floatOffset: { placement: 'bottom-right-outset', outset: 2, dy: 5 },
          run: () => this.shortcutAddMember(),
          visible: () => checkPermi(['system:project:list'])
        }
      ]
    },
    shortcutAddMember() {
      if (!checkPermi(['system:project:list'])) return
      this.addMemberHandle()
    },
    getRoleList() {
      listProjectRole(0).then(res => {
        this.roleOptions = res.rows?res.rows.map(r=>{
          r.roleName = r.roleNameI18nKey?this.$t(r.roleNameI18nKey):r.roleName;
          return r;
        }):[];
        this.getMemberList();
      });
    },
    memberSearchHandle(e){
      this.queryParams.params.search=e;
      this.getMemberList();
    },
    getMemberList() {
      this.loading = true;
      listMemberOfProject(this.currentProjectId,this.queryParams).then(res => {
        this.loading = false;
        this.memberList = (res.rows || []).map(row => {
          const ids = Array.isArray(row.roleIds)
            ? row.roleIds
            : (row.roles || []).map(r => r.roleId).filter(id => id != null)
          return { ...row, roleIds: ids }
        });
        this.total = res.total;
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
      updateMemberRoleOfProject(
        this.currentProjectId,
        member.userId,
        ids
      ).then(()=>{
        this.$message.success(this.$i18n.t('update.success').toString());
        this.getMemberList();
      }).catch(()=>{
        this.getMemberList();
      });
    },
    addMemberHandle() {
      this.$refs.addProjectMemberDialog.open();
    },
    deleteHandle(member) {
      previewRemoveMemberOfProject(this.currentProjectId, member.userId).then((res) => {
        const preview = res.data || res;
        if (preview && preview.requiresHandoff) {
          this.handoffTargetMember = member;
          this.handoffPreview = preview;
          this.handoffForm.handoffUserIds = [];
          this.handoffDialogVisible = true;
          return;
        }
        this.$modal.confirm(this.$i18n.t('project.is-delete-member'),
          this.$i18n.t('prompted').toString(),
          {
            confirmButtonText: i18n.t('delete').toString(),
            cancelButtonText: i18n.t('cancel').toString(),
            confirmButtonClass: 'delete-button',
            type: "warning",
          }).then(() => {
            return delMemberOfProject(this.currentProjectId, member.userId);
          }).then(()=>{
            this.$message.success(this.$i18n.t('delete.success').toString());
            this.getMemberList();
          }).catch(() => {});
      }).catch(() => {});
    },
    resetHandoffDialog() {
      this.handoffPreview = null;
      this.handoffTargetMember = null;
      this.handoffForm.handoffUserIds = [];
      this.handoffSubmitting = false;
      this.$refs.handoffMemberSelect?.clear?.();
      this.$refs.handoffForm?.clearValidate?.();
    },
    submitHandoffRemove() {
      this.$refs.handoffForm.validate((valid) => {
        if (!valid) return;
        const handoffUserId = Array.isArray(this.handoffForm.handoffUserIds)
          ? this.handoffForm.handoffUserIds[0]
          : this.handoffForm.handoffUserIds;
        if (!handoffUserId || !this.handoffTargetMember) return;
        this.handoffSubmitting = true;
        delMemberOfProject(this.currentProjectId, this.handoffTargetMember.userId, handoffUserId)
          .then(() => {
            this.$message.success(this.$i18n.t('delete.success').toString());
            this.handoffDialogVisible = false;
            this.getMemberList();
          })
          .catch(() => {})
          .finally(() => {
            this.handoffSubmitting = false;
          });
      });
    },
    transferCreatorHandle() {
      const self = (this.memberList || []).find(m => Number(m.userId) === Number(this.currentUserId))
      if (!self || !this.canTransferCreator(self)) {
        this.$modal.msgError(this.$t('project.transfer-creator-only'))
        return
      }
      this.transferTargetUserId = null
      this.transferFilterKeyword = ''
      this.transferOpen = true
    },
    confirmTransfer() {
      const self = (this.memberList || []).find(m => Number(m.userId) === Number(this.currentUserId))
      if (!self || !this.canTransferCreator(self)) {
        this.$modal.msgError(this.$t('project.transfer-creator-only'))
        return
      }
      if (!this.transferTargetUserId) {
        this.$modal.msgError(this.$t('project.transfer-target-placeholder'))
        return
      }
      this.transferSubmitting = true
      transferProjectCreator(this.currentProjectId, this.transferTargetUserId).then(() => {
        this.$message.success(this.$t('project.transfer-creator-success'))
        this.transferOpen = false
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
        this.$modal.msgError(this.$t('project.transfer-creator-fail'))
      }).finally(() => {
        this.transferSubmitting = false
      })
    },
  }
}
</script>

<style lang="scss" scoped>
  .member-tools>* {
    margin-left: 10px;
  }
  :deep(.el-table){
    span[teamLock="1"] {
      text-decoration: line-through;
      color: #DCDFE6;
    }
  }
  .project-member-handoff-hint,
  .project-transfer-hint {
    margin: 0 0 12px;
    color: var(--text-color-regular);
    line-height: 1.5;
  }
  .project-member-handoff-block {
    margin-bottom: 12px;
    padding: 8px 10px;
    background: var(--bg-color-base);
    border: 1px solid var(--border-color-light);
    border-radius: 4px;
  }
  .project-member-handoff-block__title {
    font-weight: 600;
    color: var(--text-color-primary);
    margin-bottom: 6px;
  }
  .project-member-handoff-list {
    margin: 0;
    padding-left: 18px;
    color: var(--text-color-regular);
    max-height: 160px;
    overflow: auto;
  }
  .project-member-handoff-multi {
    margin: 0 0 12px;
    color: var(--text-color-secondary);
    font-size: 13px;
  }
</style>
<style>
.delete-button {
  color: #fff;
  background-color: #f56c6c !important;
  border-color: #f56c6c !important;
}
.delete-button:hover {
  background: #f78989 !important;
  border-color: #f78989 !important;
  color: #fff;
}
</style>

<style lang="scss">
/* 下拉挂到 body，须非 scoped；铭牌两行需撑高 option */
.project-transfer-member-select-dropdown.el-select-dropdown {
  min-width: 420px !important;
}
.project-transfer-member-select-dropdown .el-select-dropdown__wrap {
  max-height: 360px !important;
}
.project-transfer-member-select-dropdown .el-select-dropdown__item {
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
.project-transfer-member-select-dropdown .member-nameplate {
  width: 100%;
  padding: 2px 0;
}
.project-transfer-member-select-dropdown .member-nameplate-content p {
  margin: 0;
  font-size: 14px;
  color: var(--text-color-primary);
}
.project-transfer-member-select-dropdown .member-nameplate-content span {
  font-size: 12px;
  line-height: 16px;
  height: auto;
  color: var(--text-color-secondary);
}
</style>
