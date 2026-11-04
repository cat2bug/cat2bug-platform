<template>
  <el-dialog
    :title="$t('project.add-member')"
    v-model="dialogVisible"
    width="720px"
    append-to-body
    :close-on-click-modal="false"
    custom-class="cat2bug-form-shortcut-dialog project-add-member-dialog"
    :close-on-press-escape="false"
    :before-close="onToolDialogBeforeClose"
    @opened="onToolDialogOpened"
  >
    <el-tabs v-model="activeTab" class="member-invite-dialog-tabs" @tab-change="onTabChange">
      <el-tab-pane :label="$t('member.invite-team')" name="team">
        <el-form ref="teamForm" :model="teamForm" :rules="teamRules" label-width="120px">
          <el-form-item :label="$t('role')" prop="roleIds">
            <el-select
              v-model="teamForm.roleIds"
              class="member-direct-select"
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
          <!-- 筛选框不能放在 prop=memberIds 的 form-item 内，否则输入会误触发「请选择成员」 -->
          <el-form-item :label="$t('member')" required>
            <div class="team-add-body">
              <div class="team-add-toolbar">
                <el-input
                  v-model="teamSearchInput"
                  clearable
                  size="small"
                  :prefix-icon="$legacyIcon('el-icon-search')"
                  :placeholder="$t('member.team-add-search-placeholder')"
                  @input="onTeamSearchInput"
                  @clear="onTeamSearchInput('')"
                />
                <div class="team-add-toolbar__actions">
                  <el-button size="small" link type="primary" @click="selectAllTeamPage">
                    {{ $t('member.team-add-select-all-page') }}
                  </el-button>
                  <el-button size="small" link @click="clearTeamSelection">
                    {{ $t('member.team-add-clear') }}
                  </el-button>
                  <span class="team-add-selected-count">
                    {{ $t('member.team-add-selected-count', { count: teamForm.memberIds.length }) }}
                  </span>
                </div>
              </div>
              <el-form-item prop="memberIds" label-width="0" class="team-add-field">
                <div v-loading="teamLoading" class="team-add-list">
                  <cat2-bug-empty
                    v-if="!teamLoading && !teamOptions.length"
                    size="small"
                    :description="$t('member.team-add-empty')"
                  />
                  <el-checkbox-group v-else v-model="teamForm.memberIds" class="team-add-checkbox-group">
                    <el-checkbox
                      v-for="item in teamOptions"
                      :key="item.userId"
                      :label="item.userId"
                      class="team-add-checkbox-row"
                    >
                      <member-nameplate :member="item" size="default" />
                    </el-checkbox>
                  </el-checkbox-group>
                </div>
                <div v-if="teamTotal > 0" class="team-add-pagination">
                  <el-pagination
                    small
                    layout="prev, pager, next, total"
                    :total="teamTotal"
                    v-model:current-page="teamPageNum"
                    :page-size="TEAM_PAGE_SIZE"
                    @current-change="onTeamPageChange"
                  />
                </div>
              </el-form-item>
            </div>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane :label="$t('member.invite-direct')" name="direct">
    <el-form ref="form" :model="form" :rules="rules" label-width="120px">
      <el-row>
        <el-col :span="24">
          <el-form-item :label="$t('role')" prop="roleIds">
            <el-select v-model="form.roleIds" class="member-direct-select" multiple :placeholder="$t('member.please-select-role')" style="width: 100%">
              <el-option
                v-for="item in roleOptions"
                :key="item.roleId"
                :label="item.roleName"
                :value="item.roleId"
                :disabled="item.status == 1"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('member')" prop="memberIds">
            <el-select
              class="select-member member-direct-select"
              v-model="form.memberIds"
              multiple
              filterable
              remote
              reserve-keyword
              :automatic-dropdown="false"
              popper-class="project-add-member-select-dropdown"
              :placeholder="$t('member.add-search-placeholder')"
              :remote-method="searchMemberHandle"
              :loading="loading && memberPageNum === 1"
              style="width: 100%"
              @visible-change="onMemberSelectVisible"
            >
              <el-option-group
                v-for="group in memberOptionGroups"
                :key="group.key"
                :label="group.label"
              >
                <el-option
                  v-for="item in group.options"
                  :key="item.userId"
                  :label="item.nickName"
                  :value="item.userId">
                  <member-nameplate :member="item" size="default" />
                </el-option>
              </el-option-group>
              <el-option
                v-if="memberLoadHint"
                :value="MEMBER_HINT_VALUE"
                disabled
                class="add-member-load-hint"
                :label="memberLoadHint"
              >
                {{ memberLoadHint }}
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
      </el-tab-pane>
      <el-tab-pane :label="$t('member.invite-link')" name="link">
        <member-invite-link-panel
          ref="inviteLinkPanel"
          scope-type="project"
          :scope-id="getProjectId()"
          :role-options="roleOptions"
        />
      </el-tab-pane>
    </el-tabs>
    <template #footer>
      <div class="dialog-footer">
      <el-button
        v-if="activeTab === 'team' || activeTab === 'direct'"
        class="defect-kbd-hint-host"
        type="primary"
        @click="submitForm"
      >
        {{$t('submit')}}
        <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
      </el-button>
      <el-button @click="requestCloseToolDialog">{{$t('cancel')}}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import {
  addProjectMembers,
  listNotMemberOfProject,
  listProjectRole,
  listTeamNotMemberOfProject,
} from "@/api/system/project";
import MemberNameplate from "@/components/MemberNameplate";
import MemberInviteLinkPanel from "@/components/MemberInviteLinkPanel";
import Cat2BugEmpty from "@/components/Cat2BugEmpty";
import defectToolDialogKbd from '@/mixins/defect-tool-dialog-kbd'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'

const MEMBER_HINT_VALUE = '__member_list_hint__'
const MEMBER_PAGE_SIZE = 20
const TEAM_PAGE_SIZE = 5

export default {
  name: "AddProjectMember",
  mixins: [defectToolDialogKbd],
  components: { MemberNameplate, MemberInviteLinkPanel, Cat2BugEmpty },
  setup() {
    const userStore = useUserStore()
    const { config } = storeToRefs(userStore)
    return { config }
  },
  data() {
    return {
      MEMBER_HINT_VALUE,
      TEAM_PAGE_SIZE,
      activeTab: 'team',
      loading: false,
      selectMemberList: [],
      memberOptions: [],
      memberSearchQuery: '',
      memberPageNum: 1,
      memberTotal: 0,
      memberLoadingMore: false,
      memberDropdownWrap: null,
      dialogVisible: false,
      form: {
        projectId: this.getProjectId(),
        memberIds: [],
        roleIds: []
      },
      rules: {
        roleIds: [
          { type: 'array', required: true, min: 1, message: () => this.$t('member.please-select-role'), trigger: 'change' }
        ],
        memberIds: [
          { type: 'array', required: true, min: 1, message: () => this.$t('member.please-select-member'), trigger: 'change' }
        ]
      },
      teamForm: {
        memberIds: [],
        roleIds: []
      },
      teamRules: {
        roleIds: [
          { type: 'array', required: true, min: 1, message: () => this.$t('member.please-select-role'), trigger: 'change' }
        ],
        memberIds: [
          { type: 'array', required: true, min: 1, message: () => this.$t('member.please-select-member'), trigger: 'change' }
        ]
      },
      teamOptions: [],
      teamSearchInput: '',
      teamSearchQuery: '',
      teamPageNum: 1,
      teamTotal: 0,
      teamLoading: false,
      teamSearchTimer: null,
      roleOptions: [],
      initPassword: null,
    }
  },
  computed: {
    memberHasMore() {
      return this.memberOptions.length < this.memberTotal
    },
    memberLoadHint() {
      if (!this.memberOptions.length) return ''
      if (this.memberLoadingMore) {
        return this.$t('member.search-loading-more')
      }
      if (this.memberHasMore) {
        return this.$t('member.search-scroll-more', {
          loaded: this.memberOptions.length,
          total: this.memberTotal
        })
      }
      if (this.memberTotal > MEMBER_PAGE_SIZE) {
        return this.$t('member.search-all-loaded', { total: this.memberTotal })
      }
      return ''
    },
    memberOptionGroups() {
      const team = []
      const others = []
      for (const item of this.memberOptions || []) {
        if (this.isTeamMember(item)) {
          team.push(item)
        } else {
          others.push(item)
        }
      }
      const groups = []
      if (team.length) {
        groups.push({
          key: 'team',
          label: this.$t('member.team-member-group'),
          options: team
        })
      }
      if (others.length) {
        groups.push({
          key: 'other',
          label: this.$t('member.other-user-group'),
          options: others
        })
      }
      return groups
    }
  },
  beforeUnmount() {
    this.unbindMemberDropdownScroll()
    if (this.teamSearchTimer) {
      clearTimeout(this.teamSearchTimer)
      this.teamSearchTimer = null
    }
  },
  methods: {
    isTeamMember(item) {
      if (!item) return false
      const v = item.teamMember
      return v === true || v === 1 || v === '1'
    },
    open(){
      this.activeTab = 'team';
      this.reset();
      this.getRoleList();
    },
    getRoleList() {
      listProjectRole(0).then(res => {
        this.roleOptions = res.rows?res.rows.filter(r=>r.projectCreateBy==false).map(r=>{
          r.roleName = r.roleNameI18nKey?this.$t(r.roleNameI18nKey):r.roleName;
          return r;
        }):[];
        this.memberOptions = [];
        this.loading = false;
        this.dialogVisible = true;
        this.$nextTick(() => {
          this.loadTeamNotMembers()
          if (this.$refs.inviteLinkPanel) {
            this.$refs.inviteLinkPanel.onActivated();
          }
        });
      });
    },
    getProjectId() {
      return parseInt(this.config.currentProjectId);
    },
    onTabChange(name) {
      if (name === 'team') {
        this.loadTeamNotMembers()
      }
    },
    onTeamSearchInput(val) {
      const next = val !== undefined && val !== null ? String(val) : (this.teamSearchInput || '')
      this.teamSearchInput = next
      if (this.teamSearchTimer) clearTimeout(this.teamSearchTimer)
      this.teamSearchTimer = setTimeout(() => {
        this.teamSearchQuery = next.trim()
        this.teamPageNum = 1
        this.loadTeamNotMembers()
      }, 280)
    },
    onTeamPageChange(page) {
      this.teamPageNum = page
      this.loadTeamNotMembers()
    },
    loadTeamNotMembers() {
      this.teamLoading = true
      const keyword = (this.teamSearchQuery || '').trim()
      const query = {
        pageNum: this.teamPageNum,
        pageSize: TEAM_PAGE_SIZE,
        params: {}
      }
      if (keyword) {
        query.params.search = keyword
        query.search = keyword
      }
      listTeamNotMemberOfProject(this.getProjectId(), query).then(res => {
        this.teamOptions = res.rows || []
        this.teamTotal = Number(res.total) || 0
      }).catch(() => {
        this.teamOptions = []
        this.teamTotal = 0
      }).finally(() => {
        this.teamLoading = false
      })
    },
    selectAllTeamPage() {
      const ids = new Set(this.teamForm.memberIds || [])
      for (const m of this.teamOptions || []) {
        if (m && m.userId != null) ids.add(m.userId)
      }
      this.teamForm.memberIds = Array.from(ids)
    },
    clearTeamSelection() {
      this.teamForm.memberIds = []
    },
    searchMemberHandle(query) {
      const keyword = query && String(query).trim()
      if (!keyword) {
        this.loading = false
        this.memberOptions = []
        this.memberSearchQuery = ''
        this.memberPageNum = 1
        this.memberTotal = 0
        return
      }
      this.memberSearchQuery = keyword
      this.memberPageNum = 1
      this.fetchMemberPage({ append: false })
    },
    fetchMemberPage({ append }) {
      if (!this.memberSearchQuery) return
      if (append) {
        if (this.memberLoadingMore || !this.memberHasMore) return
        this.memberLoadingMore = true
      } else {
        this.loading = true
      }
      const pageNum = append ? this.memberPageNum + 1 : 1
      listNotMemberOfProject(this.getProjectId(), {
        pageNum,
        pageSize: MEMBER_PAGE_SIZE,
        params: { search: this.memberSearchQuery }
      }).then(res => {
        const rows = res.rows || []
        this.memberTotal = Number(res.total) || rows.length
        this.memberPageNum = pageNum
        if (append) {
          const seen = new Set(this.memberOptions.map(m => m.userId))
          this.memberOptions = this.memberOptions.concat(rows.filter(r => !seen.has(r.userId)))
        } else {
          this.memberOptions = rows
        }
      }).catch(() => {
        if (!append) {
          this.memberOptions = []
          this.memberTotal = 0
        }
      }).finally(() => {
        this.loading = false
        this.memberLoadingMore = false
      })
    },
    onMemberSelectVisible(visible) {
      if (visible) {
        this.$nextTick(() => this.bindMemberDropdownScroll())
      } else {
        this.unbindMemberDropdownScroll()
      }
    },
    bindMemberDropdownScroll() {
      this.unbindMemberDropdownScroll()
      const wrap = document.querySelector(
        '.project-add-member-select-dropdown .el-select-dropdown__wrap'
      )
      if (!wrap) return
      this.memberDropdownWrap = wrap
      wrap.addEventListener('scroll', this.onMemberDropdownScroll, { passive: true })
    },
    unbindMemberDropdownScroll() {
      if (this.memberDropdownWrap) {
        this.memberDropdownWrap.removeEventListener('scroll', this.onMemberDropdownScroll)
        this.memberDropdownWrap = null
      }
    },
    onMemberDropdownScroll(e) {
      const el = e.target
      if (!el || this.memberLoadingMore || !this.memberHasMore) return
      if (el.scrollTop + el.clientHeight >= el.scrollHeight - 24) {
        this.fetchMemberPage({ append: true })
      }
    },
    submitForm() {
      if (this.activeTab === 'team') {
        this.submitTeamForm()
        return
      }
      this.$refs["form"].validate(valid => {
        if (valid) {
          const payload = {
            ...this.form,
            memberIds: (this.form.memberIds || []).filter(id => id !== MEMBER_HINT_VALUE)
          }
          addProjectMembers(this.getProjectId(), payload).then(res => {
            this.$modal.msgSuccess(this.$i18n.t('add-success'));
            this.doCloseToolDialog();
            this.$emit("invite", res);
          });
        }
      });
    },
    submitTeamForm() {
      this.$refs.teamForm.validate(valid => {
        if (!valid) return
        const payload = {
          projectId: this.getProjectId(),
          memberIds: this.teamForm.memberIds || [],
          roleIds: this.teamForm.roleIds || []
        }
        addProjectMembers(this.getProjectId(), payload).then(res => {
          this.$modal.msgSuccess(this.$i18n.t('add-success'))
          this.doCloseToolDialog()
          this.$emit('invite', res)
        })
      })
    },
    shortcutSave() {
      this.submitForm();
    },
    cancel() {
      this.requestCloseToolDialog();
    },
    reset() {
      this.unbindMemberDropdownScroll()
      this.form = {
        projectId: this.getProjectId(),
        memberIds: [],
        roleIds: []
      };
      this.teamForm = {
        memberIds: [],
        roleIds: []
      }
      this.memberOptions = []
      this.memberSearchQuery = ''
      this.memberPageNum = 1
      this.memberTotal = 0
      this.memberLoadingMore = false
      this.teamOptions = []
      this.teamSearchInput = ''
      this.teamSearchQuery = ''
      this.teamPageNum = 1
      this.teamTotal = 0
      this.resetForm("form");
      this.resetForm("teamForm");
    },
  }
}
</script>

<style lang="scss" scoped>
.member-tools>* {
  margin-left: 10px;
}
.team-add-body {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}
.team-add-field {
  margin-bottom: 0;
  width: 100%;
  :deep(.el-form-item__content) {
    display: flex;
    flex-direction: column;
    margin-left: 0 !important;
  }
}
.team-add-toolbar {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.team-add-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.team-add-selected-count {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-color-secondary);
}
.team-add-list {
  width: 100%;
  min-height: 120px;
  /* 每页 5 条，高度跟内容走，尽量不出现列表滚动条 */
  max-height: none;
  overflow: visible;
  border: 1px solid var(--border-color-light);
  border-radius: 4px;
  padding: 8px 10px;
  background: var(--bg-color-base);
}
.team-add-checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}
.team-add-checkbox-row {
  display: flex;
  align-items: center;
  height: auto;
  margin-right: 0;
  width: 100%;
  padding: 4px 0;
  :deep(.el-checkbox__input) {
    align-self: center;
  }
  :deep(.el-checkbox__label) {
    flex: 1;
    min-width: 0;
    white-space: normal;
    line-height: normal;
  }
  :deep(.member-nameplate) {
    align-items: center;
  }
}
.team-add-pagination {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}
</style>

<style lang="scss">
/* 下拉挂到 body，须非 scoped */
.project-add-member-select-dropdown.el-select-dropdown {
  min-width: 520px !important;
}
/* 抬高可视区：少量结果不必滚；多结果在此高度内滚动并触底加载 */
.project-add-member-select-dropdown .el-select-dropdown__wrap {
  max-height: 420px !important;
}
.project-add-member-select-dropdown.el-select-dropdown.is-multiple .el-select-dropdown__item {
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
.project-add-member-select-dropdown.el-select-dropdown.is-multiple .el-select-dropdown__item.add-member-load-hint,
.project-add-member-select-dropdown .el-select-dropdown__item.add-member-load-hint {
  min-height: 32px;
  height: 32px;
  justify-content: center;
  color: var(--text-color-secondary);
  font-size: 12px;
  cursor: default;
}
.project-add-member-select-dropdown .el-select-group__title {
  padding: 8px 12px 4px;
  font-size: 12px;
  color: var(--text-color-secondary);
  line-height: 1.4;
}
.project-add-member-select-dropdown .member-nameplate {
  width: 100%;
  padding: 2px 0;
}
.project-add-member-select-dropdown .member-nameplate-content p {
  margin: 0;
  font-size: 14px;
  color: var(--text-color-primary);
}
.project-add-member-select-dropdown .member-nameplate-content span {
  font-size: 12px;
  line-height: 16px;
  height: auto;
  color: var(--text-color-secondary);
}
</style>
