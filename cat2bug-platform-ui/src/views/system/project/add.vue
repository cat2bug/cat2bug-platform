<template>
  <div class="app-container project-form-page" ref="projectCreateMain">
    <el-row class="project-add-page-header project-create-hint-back">
      <el-page-header @back="goBack" :content="$t('project.create-project')">
      </el-page-header>
    </el-row>
    <el-row class="project-add-page-container">
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
<!--        基础信息-->
        <el-row :gutter="24" class="step1">
          <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="12">
            <el-form-item :label="$t('project.name')" prop="projectName">
              <el-input v-model="form.projectName" maxlength="64" :placeholder="$t('project.enter-project-name')"></el-input>
            </el-form-item>
            <el-form-item :label="$t('project.path-key')" prop="projectKey">
              <el-input v-model="form.projectKey" maxlength="100" :placeholder="$t('project.path-key-placeholder')"></el-input>
              <div class="form-tip">{{ $t('project.path-key-hint') }}</div>
            </el-form-item>
            <el-form-item :label="$t('project.introduction')">
              <el-input
                type="textarea"
                :placeholder="$t('enter-content')"
                :rows="5"
                v-model="form.projectIntroduce"
                maxlength="255"
                show-word-limit
              >
              </el-input>
            </el-form-item>
            <el-form-item :label="$t('project.member')">
              <el-row>
                <el-col :span="24">
                  <el-switch
                    v-model="projectMemberSwitch"
                    active-color="#13ce66"
                    inactive-color="#ff4949">
                  </el-switch>
                </el-col>
                <el-col :span="24" v-if="projectMemberSwitch">
<!--                  添加成员按钮-->
                  <el-popover
                    v-model:visible="memberPopoverVisible"
                    placement="bottom"
                    width="400"
                    @show="getMemberList"
                    @hide="resetMember"
                    trigger="click">
                    <el-input :prefix-icon="$legacyIcon('el-icon-search')" :placeholder="$t('team.search-placeholder')" v-model="memberParams.params.search" @input="getMemberList" size="small"></el-input>
<!--                    成员列表-->
                      <el-row v-if="memberList && memberList.length>0" class="project-members">
                        <el-col :select="member.isSelect" :span="24" v-for="member in memberList" :key="member.userId" @click="selectAddMemberHandle(member)">
                          <member-nameplate :member="member"></member-nameplate>
                        </el-col>
                      </el-row>
                    <cat2-bug-empty v-else size="small" :description="$t('no-data')" />
                    <template #reference>
                      <el-button :icon="$legacyIcon('el-icon-user-solid')">{{$t('project.add-member')}}</el-button>
                    </template>
                  </el-popover>
                </el-col>
                <el-col :span="24" v-if="projectMemberSwitch">
                  <el-table
                    :data="form.members"
                    style="width: 100%">
                    <el-table-column
                      prop="nickName"
                      :label="$t('name')"
                      min-width="200"
                    >
                      <template #default="scope">
                        <member-nameplate :member="scope.row"></member-nameplate>
                      </template>
                    </el-table-column>
                    <el-table-column
                      prop="roleIds"
                      :label="$t('role')"
                      width="230">
                      <template #default="scope">
                        <el-form-item :prop="`members.${scope.$index}.roleIds`" :rules="rules.roleIds">
                          <el-select v-model="scope.row.roleIds" :placeholder="$t('member.please-select-role')" multiple collapse-tags :disabled="scope.row.userId==currentUserId">
                            <el-option
                              v-for="role in selectRoleOptions(scope.row)"
                              :key="role.roleId"
                              :label="role.roleName"
                              :value="role.roleId">
                            </el-option>
                          </el-select>
                        </el-form-item>
                      </template>
                    </el-table-column>
                    <el-table-column
                      width="80"
                      :label="$t('operate')">
                      <template #default="scope">
                        <el-button
                          v-if="scope.row.userId!=currentUserId"
                          link
                          :icon="$legacyIcon('el-icon-delete')"
                          @click="deleteMemberHandle(scope.row)"
                          v-hasPermi="['system:project:remove']"
                        >{{$t('delete')}}</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </el-col>
              </el-row>
            </el-form-item>
            <el-form-item :label="$t('project.copy.switch')">
              <el-switch v-model="copyEnabled" />
              <div v-if="copyEnabled" class="project-copy-panel">
                <div
                  class="project-copy-source-wrap"
                  :class="{ 'is-open': copySourceCascaderOpen, 'has-value': !!copySourceDisplay }"
                >
                  <!-- 唯一可见选择框：始终自绘，级联仅用于弹出面板 -->
                  <div
                    class="project-copy-source-face"
                    :title="copySourceDisplayTitle"
                    role="button"
                    tabindex="0"
                    @click="openCopySourceCascader"
                    @keydown.enter.prevent="openCopySourceCascader"
                  >
                    <template v-if="copySourceDisplay">
                      <span class="project-copy-source-face__body">
                        <span class="project-copy-source-face__part">
                          <el-avatar
                            class="project-copy-source-avatar"
                            :size="22"
                            shape="square"
                            :src="copySourceDisplay.teamIconUrl || undefined"
                            fit="cover"
                          >{{ copySourceDisplay.teamName }}</el-avatar>
                          <span class="project-copy-source-face__name">{{ copySourceDisplay.teamName }}</span>
                        </span>
                        <span class="project-copy-source-face__sep">/</span>
                        <span class="project-copy-source-face__part">
                          <el-avatar
                            class="project-copy-source-avatar"
                            :size="22"
                            shape="square"
                            :src="copySourceDisplay.projectIconUrl || undefined"
                            fit="cover"
                          >{{ copySourceDisplay.projectName }}</el-avatar>
                          <span class="project-copy-source-face__name">{{ copySourceDisplay.projectName }}</span>
                        </span>
                      </span>
                    </template>
                    <span v-else class="project-copy-source-face__placeholder">
                      {{ $t('project.copy.source-placeholder') }}
                    </span>
                    <span class="project-copy-source-face__actions">
                      <el-icon
                        v-if="copySourceDisplay"
                        class="project-copy-source-face__clear"
                        @click.stop="clearCopySource"
                      ><CircleClose /></el-icon>
                      <el-icon
                        class="project-copy-source-face__arrow"
                        :class="{ 'is-reverse': copySourceCascaderOpen }"
                      ><ArrowDown /></el-icon>
                    </span>
                  </div>
                  <el-cascader
                    ref="copySourceCascader"
                    v-model="copySourcePath"
                    class="project-copy-source-cascader-hidden"
                    :options="copySourceCascaderOptions"
                    :props="copySourceCascaderProps"
                    :show-all-levels="true"
                    separator=" / "
                    popper-class="project-copy-source-cascader-popper"
                    @change="onCopySourcePathChange"
                    @visible-change="onCopySourceVisibleChange"
                  >
                    <template #default="{ data }">
                      <span class="project-copy-source-node">
                        <el-avatar
                          class="project-copy-source-avatar"
                          :size="22"
                          shape="square"
                          :src="data.iconUrl || undefined"
                          fit="cover"
                        >{{ data.label }}</el-avatar>
                        <span class="project-copy-source-label" :title="data.label">{{ data.label }}</span>
                      </span>
                    </template>
                  </el-cascader>
                </div>
                <template v-if="copySelection.sourceProjectId">
                  <el-tabs v-model="copyAssetTab" class="project-copy-tabs" type="border-card">
                    <el-tab-pane name="git">
                      <template #label>
                        <span class="project-copy-tab-label">
                          <svg-icon :icon-class="copyTabIcon('git')" class="project-copy-tab-icon" />
                          <span>{{ copyTabLabel('git') }}</span>
                        </span>
                      </template>
                      <div class="project-copy-tab-toolbar">
                        <el-checkbox
                          :model-value="copyGitAll"
                          :indeterminate="copyGitIndeterminate"
                          @change="onCopyGitAllChange"
                        >{{ $t('project.copy.select-all') }}</el-checkbox>
                        <el-input
                          v-model="copySearch.git"
                          clearable
                          :placeholder="$t('project.copy.search')"
                          class="project-copy-search"
                        />
                      </div>
                      <el-checkbox-group v-model="copySelection.gitSlugs" class="project-copy-pick-list" @change="onCopyGitIdsChange">
                        <el-checkbox v-for="repo in filteredCopyGit" :key="repo.id" :label="repo.id">
                          {{ repo.name }}
                          <span v-if="repo.bytes != null" class="project-copy-size">{{ formatCopyBytes(repo.bytes) }}</span>
                        </el-checkbox>
                        <div v-if="!filteredCopyGit.length" class="project-copy-empty">{{ $t('project.copy.empty') }}</div>
                      </el-checkbox-group>
                    </el-tab-pane>
                    <el-tab-pane name="cases">
                      <template #label>
                        <span class="project-copy-tab-label">
                          <svg-icon :icon-class="copyTabIcon('cases')" class="project-copy-tab-icon" />
                          <span>{{ copyTabLabel('cases') }}</span>
                        </span>
                      </template>
                      <div class="project-copy-tab-toolbar">
                        <el-checkbox
                          :model-value="copySelection.copyAllCases"
                          :indeterminate="copyCasesIndeterminate"
                          @change="onCopyCasesAllChange"
                        >{{ $t('project.copy.select-all') }}</el-checkbox>
                        <el-input
                          v-model="copySearch.cases"
                          clearable
                          :placeholder="$t('project.copy.search')"
                          class="project-copy-search"
                        />
                      </div>
                      <el-checkbox-group
                        v-model="copySelection.caseIds"
                        class="project-copy-pick-list"
                        :disabled="copySelection.copyAllCases"
                        @change="onCopyCaseIdsChange"
                      >
                        <el-checkbox v-for="row in filteredCopyCases" :key="row.id" :label="row.id">{{ row.name }}</el-checkbox>
                        <div v-if="!filteredCopyCases.length" class="project-copy-empty">{{ $t('project.copy.empty') }}</div>
                      </el-checkbox-group>
                    </el-tab-pane>
                    <el-tab-pane name="modules">
                      <template #label>
                        <span class="project-copy-tab-label">
                          <svg-icon :icon-class="copyTabIcon('modules')" class="project-copy-tab-icon" />
                          <span>{{ copyTabLabel('modules') }}</span>
                        </span>
                      </template>
                      <div class="project-copy-tab-toolbar">
                        <el-checkbox
                          :model-value="copySelection.copyAllModules"
                          :indeterminate="copyModulesIndeterminate"
                          @change="onCopyModulesAllChange"
                        >{{ $t('project.copy.select-all') }}</el-checkbox>
                        <el-input
                          v-model="copySearch.modules"
                          clearable
                          :placeholder="$t('project.copy.search')"
                          class="project-copy-search"
                          @input="onCopyModuleSearch"
                        />
                      </div>
                      <el-tree
                        v-if="copyModuleTree.length"
                        ref="copyModuleTree"
                        class="project-copy-module-tree"
                        :class="{ 'is-locked': copySelection.copyAllModules }"
                        :data="copyModuleTree"
                        node-key="id"
                        show-checkbox
                        default-expand-all
                        :props="copyModuleTreeProps"
                        :filter-node-method="filterCopyModuleNode"
                        @check="onCopyModuleTreeCheck"
                      />
                      <div v-else class="project-copy-empty">{{ $t('project.copy.empty') }}</div>
                    </el-tab-pane>
                    <el-tab-pane name="documents">
                      <template #label>
                        <span class="project-copy-tab-label">
                          <svg-icon :icon-class="copyTabIcon('documents')" class="project-copy-tab-icon" />
                          <span>{{ copyTabLabel('documents') }}</span>
                        </span>
                      </template>
                      <div class="project-copy-tab-toolbar">
                        <el-checkbox
                          :model-value="copySelection.copyAllDocuments"
                          :indeterminate="copyDocumentsIndeterminate"
                          @change="onCopyDocumentsAllChange"
                        >{{ $t('project.copy.select-all') }}</el-checkbox>
                        <el-input
                          v-model="copySearch.documents"
                          clearable
                          :placeholder="$t('project.copy.search')"
                          class="project-copy-search"
                          @input="onCopyDocumentSearch"
                        />
                      </div>
                      <el-tree
                        v-if="copyDocumentTree.length"
                        ref="copyDocumentTree"
                        class="project-copy-document-tree"
                        :class="{ 'is-locked': copySelection.copyAllDocuments }"
                        :data="copyDocumentTree"
                        node-key="id"
                        show-checkbox
                        default-expand-all
                        :props="copyDocumentTreeProps"
                        :filter-node-method="filterCopyDocumentNode"
                        @check="onCopyDocumentTreeCheck"
                      >
                        <template #default="{ data }">
                          <span class="project-copy-doc-node">
                            <svg-icon
                              class="project-copy-doc-node__icon"
                              :icon-class="data.folder ? 'dir' : 'doc'"
                            />
                            <span class="project-copy-doc-node__name" :title="data.name">{{ data.name }}</span>
                          </span>
                        </template>
                      </el-tree>
                      <div v-else class="project-copy-empty">{{ $t('project.copy.empty') }}</div>
                    </el-tab-pane>
                    <el-tab-pane name="members">
                      <template #label>
                        <span class="project-copy-tab-label">
                          <svg-icon :icon-class="copyTabIcon('members')" class="project-copy-tab-icon" />
                          <span>{{ copyTabLabel('members') }}</span>
                        </span>
                      </template>
                      <div class="project-copy-tab-toolbar">
                        <el-checkbox
                          :model-value="copySelection.copyAllMembers"
                          :indeterminate="copyMembersIndeterminate"
                          @change="onCopyMembersAllChange"
                        >{{ $t('project.copy.select-all') }}</el-checkbox>
                        <el-input
                          v-model="copySearch.members"
                          clearable
                          :placeholder="$t('project.copy.search')"
                          class="project-copy-search"
                        />
                      </div>
                      <el-checkbox-group
                        v-model="copySelection.memberIds"
                        class="project-copy-pick-list project-copy-member-list"
                        :disabled="copySelection.copyAllMembers"
                        @change="onCopyMemberIdsChange"
                      >
                        <el-checkbox
                          v-for="row in filteredCopyMembers"
                          :key="row.id"
                          :label="row.id"
                          :disabled="isCopyCurrentMember(row)"
                          class="project-copy-member-check"
                        >
                          <div class="project-copy-member-row">
                            <member-nameplate :member="row" size="default" />
                            <div v-if="row.roles && row.roles.length" class="project-copy-member-roles">
                              <el-tag
                                v-for="role in row.roles"
                                :key="role.roleId || role.roleName"
                                size="small"
                                type="info"
                                effect="plain"
                              >{{ role.roleName || role }}</el-tag>
                            </div>
                          </div>
                        </el-checkbox>
                        <div v-if="!filteredCopyMembers.length" class="project-copy-empty">{{ $t('project.copy.empty') }}</div>
                      </el-checkbox-group>
                    </el-tab-pane>
                    <el-tab-pane name="ai">
                      <template #label>
                        <span class="project-copy-tab-label">
                          <svg-icon :icon-class="copyTabIcon('ai')" class="project-copy-tab-icon" />
                          <span>{{ copyTabLabel('ai') }}</span>
                        </span>
                      </template>
                      <div class="project-copy-ai-options">
                        <div class="project-copy-ai-option">
                          <el-checkbox v-model="copySelection.copyAi" @change="refreshCopyEstimate">
                            {{ $t('project.copy.openai') }}
                          </el-checkbox>
                          <div class="project-copy-tab-hint">{{ $t('project.copy.hint.openai') }}</div>
                        </div>
                        <div class="project-copy-ai-option">
                          <el-checkbox v-model="copySelection.copyMcp" @change="refreshCopyEstimate">
                            {{ $t('project.copy.mcp') }}
                          </el-checkbox>
                          <div class="project-copy-tab-hint">{{ $t('project.copy.hint.mcp') }}</div>
                        </div>
                      </div>
                    </el-tab-pane>
                    <el-tab-pane name="im">
                      <template #label>
                        <span class="project-copy-tab-label">
                          <svg-icon :icon-class="copyTabIcon('im')" class="project-copy-tab-icon" />
                          <span>{{ copyTabLabel('im') }}</span>
                        </span>
                      </template>
                      <el-checkbox-group
                        v-model="copySelection.imSystemCodes"
                        class="project-copy-im-options"
                        @change="refreshCopyEstimate"
                      >
                        <el-checkbox label="ding">{{ $t('ding') }}</el-checkbox>
                        <el-checkbox label="feishu">{{ $t('feishu') }}</el-checkbox>
                        <el-checkbox label="wechat">{{ $t('enterprise-wechat') }}</el-checkbox>
                      </el-checkbox-group>
                      <div class="project-copy-tab-hint">{{ $t('project.copy.hint.im') }}</div>
                    </el-tab-pane>
                  </el-tabs>
                  <div class="project-copy-estimate" :class="{ 'is-over': copyOverQuota }">
                    {{ $t('project.copy.estimate', { used: formatCopyBytes(copyEstimatedBytes), max: formatCopyBytes(copyMaxBytes) }) }}
                  </div>
                  <el-alert
                    v-if="copyOverQuota"
                    type="warning"
                    :closable="false"
                    show-icon
                    :title="$t('project.copy.over-quota')"
                  />
                </template>
              </div>
            </el-form-item>
            <el-form-item class="page-form-actions">
              <div class="page-form-actions__buttons">
                <el-button @click="goBack">{{$t('cancel')}}</el-button>
                <el-button class="defect-kbd-hint-host" type="primary" :disabled="copyEnabled && copyOverQuota" @click="onSubmit">
                  {{$t('finish')}}
                  <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
                </el-button>
              </div>
            </el-form-item>
            <el-dialog
              v-model="copyProgressVisible"
              :title="$t('project.copy.progress')"
              width="480px"
              :close-on-click-modal="false"
              :show-close="copyProgressDone"
              @close="onCopyProgressClose"
            >
              <el-progress :percentage="copyJobPercent" :status="copyProgressStatus" />
              <ul v-if="copyJobItems && copyJobItems.length" class="project-copy-items">
                <li v-for="(it, idx) in copyJobItems" :key="idx">
                  {{ it.key || it.name || it.step || ('#' + (idx + 1)) }}
                  — {{ it.state || it.status || '' }}
                  <span v-if="it.error || it.message">：{{ it.error || it.message }}</span>
                </li>
              </ul>
              <p v-if="copyJobError">{{ copyJobError }}</p>
              <template #footer>
                <el-button v-if="copyProgressDone" type="primary" @click="onCopyProgressClose">{{ $t('confirm') }}</el-button>
              </template>
            </el-dialog>
          </el-col>
          <!--          项目图标-->
          <el-col :xs="24" :sm="24" :md="8" :lg="6" :xl="6" class="step2">
            <cat2-bug-image
              style="width: 96px; height: 96px; border-radius: 4px; overflow: hidden;"
              :key="projectIconPreviewUrl"
              :src="projectIconPreviewUrl"
              fit="cover"></cat2-bug-image>
            <div :class="[PROJECT_ICON_POPOVER_HOST_CLASS, { 'is-popover-open': projectIconPopperVisible }]">
              <el-popover
                ref="projectIconPopover"
                v-model:visible="projectIconPopperVisible"
                popper-class="project-icon-popper"
                placement="bottom"
                :width="348"
                trigger="click">
                <template #reference>
                  <el-button
                    ref="projectIconTrigger"
                    class="project-icon-hint-change defect-kbd-hint-host"
                  >{{$t('project.change-icon')}}
                    <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint" aria-hidden="true">{{ changeIconHintLetter }}</span>
                  </el-button>
                </template>
                <div class="project-icon-popper__grid">
                  <div
                    class="project-icon-item"
                    v-for="i in 10"
                    :key="i"
                    role="button"
                    :aria-label="$t('project.change-icon') + ' ' + i"
                    tabindex="-1"
                  >
                    <el-image
                      @click="clickProjectIconHandle(i)"
                      :src="activeProjectIconUrl(i)"
                      fit="cover"
                    ></el-image>
                  </div>
                  <div class="project-icon-item" role="button" :aria-label="$t('upload')" tabindex="-1">
                    <image-upload v-model="projectIcon" :limit="1" :file-type="[]" :is-show-tip="false" :is-show-clipboard-button="false" buttonStyle="width: 72px; height: 72px; font-size: 22px;" @input="handleSelectSelfImage" />
                  </div>
                </div>
              </el-popover>
            </div>
          </el-col>
        </el-row>
      </el-form>
    </el-row>
  </div>
</template>

<script>
import {addProject, listProjectRole, listCopySources, listCopySourceItems, estimateProjectCopy, getProjectCopyJob} from "@/api/system/project";
import { listMember, getTeamQuota } from "@/api/system/team";
import MemberNameplate from "@/components/MemberNameplate"
import { resolveProjectIconUrl, resolveTeamIconUrl } from '@/utils/upload-asset'
import { getDefaultProjectIconUrl, getProjectIconStoragePath, projectIconThemeKey } from '@/utils/project-default-icons'
import projectCreateFormKbd from '@/mixins/project-create-form-kbd'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { PROJECT_CREATE_KBD_SCOPE } from '@/mixins/project-create-kbd'
import {
  PROJECT_ICON_POPOVER_HOST_CLASS,
  destroyProjectIconPopoverKbd,
  initProjectIconPopoverKbd,
  isAnyProjectIconPopoverOpenInDom,
  shortcutOpenProjectIconPopover
} from '@/utils/project-icon-popover-kbd'
import { isEscapeCloseKey } from '@/utils/defect-drawer-shortcuts'
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'
import { createPathKeyValidator } from '@/utils/path-key'
import { checkPermi } from '@/utils/permission'

import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'
import { ArrowDown, CircleClose } from '@element-plus/icons-vue'

export default {
  name: "ProjectAdd",
  mixins: [projectCreateFormKbd],
  components:{ MemberNameplate, ArrowDown, CircleClose },
  setup() {
    const userStore = useUserStore()
    const { id: userId, config: userConfig } = storeToRefs(userStore)
    return { userId, userConfig }
  },
  data() {
    return {
      PROJECT_ICON_POPOVER_HOST_CLASS,
      memberParams: {
        params: {
          excludeMembers: [],
          search: null
        }
      },
      roleOptions: [],
      memberList:[],
      projectMemberSwitch: false,
      projectIcon: null,
      memberPopoverVisible: false,
      form:{
        teamId: this.currentTeamId,
        projectName: null,
        projectKey: null,
        projectIntroduce: null,
        projectIcon: null,
        members: []
      },
      projectIconPopperVisible: false,
      activeProjectIconIndex: 1,
      copyEnabled: false,
      copySources: [],
      copySourcePath: [],
      copySourceCascaderProps: {
        value: 'value',
        label: 'label',
        children: 'children',
        emitPath: true,
        expandTrigger: 'click'
      },
      copyAssetTab: 'git',
      copySourceCascaderOpen: false,
      copySearch: { git: '', cases: '', modules: '', documents: '', members: '' },
      copyItems: { git: [], cases: [], modules: [], documents: [], members: [] },
      copyModuleTreeProps: { label: 'name', children: 'children', disabled: 'disabled' },
      copyDocumentTreeProps: { label: 'name', children: 'children', disabled: 'disabled' },
      copyGitAll: false,
      copySelection: {
        sourceProjectId: null,
        copyAllCases: false,
        copyAllModules: false,
        copyAllDocuments: false,
        copyAllMembers: false,
        caseIds: [],
        moduleIds: [],
        documentIds: [],
        memberIds: [],
        copyAi: false,
        copyMcp: false,
        imSystemCodes: [],
        gitSlugs: []
      },
      copyEstimatedBytes: 0,
      copyMaxBytes: 1073741824,
      copyOverQuota: false,
      copyEstimateTimer: null,
      copyProgressVisible: false,
      copyProgressDone: false,
      copyJobId: null,
      copyJobPercent: 0,
      copyJobItems: [],
      copyJobError: null,
      copyPollTimer: null,
      rules: {
        projectName: [
          { required: true, message: this.$i18n.t('project.project-name-cannot-empty'), trigger: "blur" }
        ],
        projectKey: [
          { required: true, message: this.$i18n.t('project.path-key-required'), trigger: 'blur' },
          { validator: createPathKeyValidator({
            required: this.$i18n.t('project.path-key-required'),
            reserved: this.$i18n.t('path-key.reserved'),
            format: this.$i18n.t('path-key.format-invalid')
          }), trigger: 'blur' }
        ],
        roleIds: [
          { type: 'array', required: true, message: this.$i18n.t('member.please-select-role'), trigger: 'change'}
        ]
      }
    }
  },
  computed: {
    activeProjectIconUrl() {
      projectIconThemeKey()
      return (index) => getDefaultProjectIconUrl(index)
    },
    selectRoleOptions: function (){
      return function (member){
        if(this.currentUserId == member.userId){
          return this.roleOptions.filter(r=>r.projectCreateBy);
        } else {
          return this.roleOptions.filter(r=>r.projectCreateBy==false);
        }
      }
    },
    currentUserId: function () {
      return this.userId;
    },
    currentTeamId: function () {
      return this.userConfig.currentTeamId;
    },
    copyProgressStatus() {
      if (!this.copyProgressDone) return undefined
      if (this.copyJobError) return 'exception'
      return 'success'
    },
    /** 复制源：团队 → 项目二级；节点带图标 */
    copySourceCascaderOptions() {
      const teams = new Map()
      for (const p of this.copySources || []) {
        const teamId = p.teamId != null ? p.teamId : 0
        let team = teams.get(teamId)
        if (!team) {
          const teamIcon = p.teamIcon
          team = {
            value: `team-${teamId}`,
            label: p.teamName || String(teamId),
            iconUrl: teamIcon ? resolveTeamIconUrl(teamIcon) : '',
            children: []
          }
          teams.set(teamId, team)
        }
        team.children.push({
          value: p.projectId,
          label: p.projectName || String(p.projectId),
          iconUrl: resolveProjectIconUrl(p.projectIcon),
          leaf: true
        })
      }
      return Array.from(teams.values())
    },
    /** 已选源：展示「团队图标+名 / 项目图标+名」 */
    copySourceDisplay() {
      const projectId = this.copySelection && this.copySelection.sourceProjectId
      if (projectId == null) return null
      const p = (this.copySources || []).find(x => x.projectId === projectId)
      if (!p) return null
      return {
        teamName: p.teamName || '',
        projectName: p.projectName || '',
        teamIconUrl: p.teamIcon ? resolveTeamIconUrl(p.teamIcon) : '',
        projectIconUrl: resolveProjectIconUrl(p.projectIcon)
      }
    },
    copySourceDisplayTitle() {
      const d = this.copySourceDisplay
      if (!d) return ''
      return `${d.teamName} / ${d.projectName}`
    },
    filteredCopyGit() {
      return this.filterCopyItems(this.copyItems.git, this.copySearch.git)
    },
    filteredCopyCases() {
      return this.filterCopyItems(this.copyItems.cases, this.copySearch.cases)
    },
    filteredCopyMembers() {
      const list = this.copyItems.members || []
      const q = (this.copySearch.members || '').trim().toLowerCase()
      if (!q) return list
      return list.filter(row => {
        const roles = (row.roles || []).map(r => String(r.roleName || r || '')).join(' ')
        return [row.name, row.nickName, row.userName, row.phoneNumber, row.email, row.id, roles]
          .some(v => String(v || '').toLowerCase().includes(q))
      })
    },
    /** 可勾选复制的成员（排除当前用户，创建人必进新项目） */
    copyableCopyMembers() {
      return (this.copyItems.members || []).filter(row => !this.isCopyCurrentMember(row))
    },
    copyModuleTree() {
      return this.buildCopyModuleTree(this.copyItems.modules || [])
    },
    copyDocumentTree() {
      return this.buildCopyDocumentTree(this.copyItems.documents || [])
    },
    copyGitIndeterminate() {
      const n = (this.copySelection.gitSlugs || []).length
      const total = (this.copyItems.git || []).length
      return n > 0 && n < total
    },
    copyCasesIndeterminate() {
      if (this.copySelection.copyAllCases) return false
      const n = (this.copySelection.caseIds || []).length
      const total = (this.copyItems.cases || []).length
      return n > 0 && n < total
    },
    copyModulesIndeterminate() {
      if (this.copySelection.copyAllModules) return false
      const n = (this.copySelection.moduleIds || []).length
      const total = (this.copyItems.modules || []).length
      return n > 0 && n < total
    },
    copyDocumentsIndeterminate() {
      if (this.copySelection.copyAllDocuments) return false
      const n = (this.copySelection.documentIds || []).length
      const total = (this.copyItems.documents || []).length
      return n > 0 && n < total
    },
    copyMembersIndeterminate() {
      if (this.copySelection.copyAllMembers) return false
      const n = (this.copySelection.memberIds || []).length
      const total = this.copyableCopyMembers.length
      return n > 0 && n < total
    },
    projectIconPreviewUrl() {
      return resolveProjectIconUrl(this.form.projectIcon)
    },
    changeIconHintLetter() {
      return shortcutStore.getLetter(`action.${PROJECT_CREATE_KBD_SCOPE}.changeIcon`, 'I')
    }
  },
  created() {
    // 与创建按钮、POST /system/project、copy-sources 一致；路由未挂权限时防直链误入
    if (!checkPermi(['system:project:add'])) {
      this.$modal.msgError(this.$t('project.collect-no-permission'))
      this.$router.replace({ path: '/index' }).catch(() => { this.$router.back() })
      return
    }
    this.form.projectIcon = getProjectIconStoragePath(1);
    this.getRoleList();
    this.$nextTick(() => this.capturePageFormCloseBaseline())
  },
  mounted() {
    this.$nextTick(() => initProjectIconPopoverKbd(this))
  },
  activated() {
    this.$nextTick(() => initProjectIconPopoverKbd(this))
  },
  beforeDestroy() {
    destroyProjectIconPopoverKbd(this)
  },
  methods: {
    getProjectCreateRegisterActions() {
      return [{
        key: 'changeIcon',
        defaultLetter: 'I',
        titleKey: 'keyboard.act.project-change-icon',
        run: () => this.shortcutOpenProjectIconPopover()
      }]
    },
    getProjectCreateActionHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${PROJECT_CREATE_KBD_SCOPE}.${key}`, def)
      return [{
        key: 'changeIcon',
        letter: L('changeIcon', 'I'),
        badgeSelector: '.project-icon-hint-change',
        floatOffset: { placement: 'bottom-right-outset', outset: 2 },
        run: () => this.shortcutOpenProjectIconPopover()
      }]
    },
    getFixedFieldHints() {
      return [{
        letter: shortcutStore.getLetter(`action.${PROJECT_CREATE_KBD_SCOPE}.changeIcon`, 'I'),
        injectBadge: false,
        onActivate: () => this.shortcutOpenProjectIconPopover()
      }]
    },
    shortcutOpenProjectIconPopover() {
      shortcutOpenProjectIconPopover(this)
    },
    shouldProjectCreateEscGoBack() {
      return !this.projectIconPopperVisible && !this.memberPopoverVisible
    },
    $_canCloseDrawerByShortcut(e) {
      if (this.memberPopoverVisible) return false
      if (!this.$_formShortcutSurfaceVisible) return false
      if (!isEscapeCloseKey(e)) return false
      if (isAnyProjectIconPopoverOpenInDom()) return false
      return !hasBlockingUiLayer({
        excludeDefectFormDrawer: true,
        excludeHandleDefectDrawer: true,
        excludeViewReportDrawer: true,
        excludeDefectToolDialog: true,
        excludeCaseImportDialog: true
      })
    },
    shortcutSave() {
      this.onSubmit()
    },
    serializePageFormCloseState() {
      return JSON.stringify({
        form: { ...this.form },
        activeProjectIconIndex: this.activeProjectIconIndex,
        projectIcon: this.projectIcon,
        projectMemberSwitch: this.projectMemberSwitch
      })
    },
    getRoleList() {
      listProjectRole(0).then(res => {
        this.roleOptions = res.rows?res.rows.map(r=>{
          r.roleName = r.roleNameI18nKey?this.$t(r.roleNameI18nKey):r.roleName;
          return r;
        }):[];
        this.getMemberList();
        this.$nextTick(() => {
          initProjectIconPopoverKbd(this)
          this.capturePageFormCloseBaseline()
        })
      });
    },
    getMemberList(){
      const excludeIds = new Set((this.form.members || []).map(m => m.userId));
      this.memberParams.params.excludeMembers = Array.from(excludeIds);
      listMember(this.currentTeamId,this.memberParams).then(res=>{
        const rows = (res.rows || []).map(m => {
          m.isSelect = false;
          if (this.roleOptions && this.roleOptions.length > 0) {
            m.roleIds = [this.roleOptions.filter(r => r.projectCreateBy == false)[0].roleId];
          } else {
            m.roleIds = [];
          }
          return m;
        });
        // 新建时默认把当前用户加入项目成员，并不再出现在待选下拉中
        if (this.form.members.length === 0) {
          const me = rows.find(m => m.userId == this.currentUserId);
          if (me) {
            me.isSelect = true;
            me.roleIds = this.roleOptions.filter(r => r.projectCreateBy).map(r => r.roleId);
            this.form.members.push(me);
            excludeIds.add(me.userId);
          }
        }
        this.memberList = rows.filter(m => !excludeIds.has(m.userId));
      });
    },
    deleteMemberHandle(member){
      member.isSelect = false;
      this.form.members = this.form.members.filter(m=>m.isSelect);
    },
    resetMember() {
      this.memberParams = {
        params: {
          excludeMembers: [],
          search: null
        }
      };
    },
    selectAddMemberHandle(member) {
      member.isSelect = true;
      if (!this.form.members.some(m => m.userId === member.userId)) {
        this.form.members.push(member);
      }
      // 已选成员从下拉中移除
      this.memberList = this.memberList.filter(m => m.userId !== member.userId);
    },
    clickProjectIconHandle(index) {
      this.activeProjectIconIndex = index;
      this.form.projectIcon = getProjectIconStoragePath(index);
      this.projectIconPopperVisible = false;
    },
    handleSelectSelfImage(url) {
      if(url) {
        this.form.projectIcon = process.env.VUE_APP_BASE_API + url;
        this.projectIconPopperVisible = false;
      } else {
        this.clickProjectIconHandle(1);
      }
    },
    loadCopySources() {
      if (!checkPermi(['system:project:add'])) {
        this.copySources = []
        return
      }
      listCopySources().then(res => {
        this.copySources = res.data || []
      }).catch(() => { this.copySources = [] })
      getTeamQuota(this.currentTeamId).then(res => {
        const q = res.data || {}
        if (q.maxProjectBytes) this.copyMaxBytes = q.maxProjectBytes
      }).catch(() => {})
    },
    filterCopyItems(items, search) {
      const list = items || []
      const q = (search || '').trim().toLowerCase()
      if (!q) return list
      return list.filter(row => String(row.name || '').toLowerCase().includes(q)
        || String(row.id || '').toLowerCase().includes(q))
    },
    /** 与侧栏菜单 sys_menu.icon 对齐；AI 用机器人 */
    copyTabIcon(type) {
      return ({
        git: 'code',
        cases: 'case',
        modules: 'cascader',
        documents: 'library',
        members: 'peoples',
        ai: 'robot',
        im: 'option'
      })[type] || 'option'
    },
    copyTabLabel(type) {
      const total = (this.copyItems[type] || []).length
      let selected = 0
      if (type === 'git') selected = (this.copySelection.gitSlugs || []).length
      else if (type === 'cases') {
        selected = this.copySelection.copyAllCases ? total : (this.copySelection.caseIds || []).length
      } else if (type === 'modules') {
        selected = this.copySelection.copyAllModules ? total : (this.copySelection.moduleIds || []).length
      } else if (type === 'documents') {
        selected = this.copySelection.copyAllDocuments ? total : (this.copySelection.documentIds || []).length
      } else if (type === 'members') {
        const copyable = this.copyableCopyMembers.length
        selected = this.copySelection.copyAllMembers ? copyable : (this.copySelection.memberIds || []).length
      } else if (type === 'ai') {
        selected = (this.copySelection.copyAi ? 1 : 0) + (this.copySelection.copyMcp ? 1 : 0)
      } else if (type === 'im') {
        selected = (this.copySelection.imSystemCodes || []).length
      }
      const title = this.$t(`project.copy.tab.${type}`)
      return selected > 0 ? `${title} (${selected})` : title
    },
    resetCopyAssetSelection() {
      this.copyGitAll = false
      this.copySelection.gitSlugs = []
      this.copySelection.copyAllCases = false
      this.copySelection.copyAllModules = false
      this.copySelection.copyAllDocuments = false
      this.copySelection.copyAllMembers = false
      this.copySelection.caseIds = []
      this.copySelection.moduleIds = []
      this.copySelection.documentIds = []
      this.copySelection.memberIds = []
      this.copySelection.copyAi = false
      this.copySelection.copyMcp = false
      this.copySelection.imSystemCodes = []
      this.copySearch = { git: '', cases: '', modules: '', documents: '', members: '' }
      this.copyItems = { git: [], cases: [], modules: [], documents: [], members: [] }
      this.copyAssetTab = 'git'
    },
    loadCopyAssetItems() {
      const sourceId = this.copySelection.sourceProjectId
      if (!sourceId) {
        this.copyItems = { git: [], cases: [], modules: [], documents: [], members: [] }
        return
      }
      const types = ['git', 'cases', 'modules', 'documents', 'members']
      Promise.all(types.map(type =>
        listCopySourceItems(sourceId, { type }).then(res => ({ type, items: res.data || [] })).catch(() => ({ type, items: [] }))
      )).then(rows => {
        const next = { git: [], cases: [], modules: [], documents: [], members: [] }
        rows.forEach(({ type, items }) => { next[type] = items })
        this.copyItems = next
        this.copySelection.memberIds = this.sanitizeCopyMemberIds(this.copySelection.memberIds)
        this.syncCopyModuleTreeChecked()
        this.onCopyModuleSearch(this.copySearch.modules)
        this.syncCopyDocumentTreeChecked()
        this.onCopyDocumentSearch(this.copySearch.documents)
      })
    },
    hasCopyAssetSelection() {
      const s = this.copySelection || {}
      return !!(
        (s.gitSlugs && s.gitSlugs.length) ||
        s.copyAllCases || (s.caseIds && s.caseIds.length) ||
        s.copyAllModules || (s.moduleIds && s.moduleIds.length) ||
        s.copyAllDocuments || (s.documentIds && s.documentIds.length) ||
        s.copyAllMembers || (s.memberIds && s.memberIds.length) ||
        s.copyAi || s.copyMcp || (s.imSystemCodes && s.imSystemCodes.length)
      )
    },
    buildCopySourcePath(projectId) {
      if (projectId == null) return []
      const p = (this.copySources || []).find(x => x.projectId === projectId)
      if (!p) return []
      const teamId = p.teamId != null ? p.teamId : 0
      return [`team-${teamId}`, projectId]
    },
    applyCopySourceProject(nextId) {
      this.copySelection.sourceProjectId = nextId || null
      this.copySourcePath = this.buildCopySourcePath(nextId)
      this.resetCopyAssetSelection()
      this.loadCopyAssetItems()
      this.refreshCopyEstimate()
    },
    onCopySourceVisibleChange(visible) {
      this.copySourceCascaderOpen = !!visible
    },
    openCopySourceCascader() {
      const cascader = this.$refs.copySourceCascader
      if (!cascader) return
      const open = () => {
        if (typeof cascader.togglePopperVisible === 'function') {
          cascader.togglePopperVisible(true)
        } else {
          const root = cascader.$el || cascader
          const input = root && root.querySelector && root.querySelector('input, .el-input__wrapper')
          if (input) input.click()
        }
      }
      open()
      // display/布局刚就绪时补一次定位，避免偶发落在 (0,0)
      this.$nextTick(() => {
        const tip = cascader.tooltipRef || cascader.popperRef
        if (tip && typeof tip.updatePopper === 'function') tip.updatePopper()
        else if (cascader.$refs && cascader.$refs.popper && typeof cascader.$refs.popper.updatePopper === 'function') {
          cascader.$refs.popper.updatePopper()
        }
      })
    },
    clearCopySource() {
      this.copySourceCascaderOpen = false
      this.applyCopySourceProject(null)
    },
    onCopySourcePathChange(path) {
      const nextId = Array.isArray(path) && path.length ? path[path.length - 1] : null
      const prevId = this.copySelection.sourceProjectId
      if (nextId === prevId) return
      const apply = () => this.applyCopySourceProject(nextId)
      if (prevId && this.hasCopyAssetSelection()) {
        this.$modal.confirm(this.$t('project.copy.switch-source-confirm')).then(() => {
          apply()
        }).catch(() => {
          this.copySourcePath = this.buildCopySourcePath(prevId)
        })
        return
      }
      apply()
    },
    onCopyGitAllChange(val) {
      this.copyGitAll = !!val
      this.copySelection.gitSlugs = val ? (this.copyItems.git || []).map(r => r.id) : []
      this.refreshCopyEstimate()
    },
    onCopyGitIdsChange(ids) {
      const total = (this.copyItems.git || []).length
      this.copyGitAll = total > 0 && (ids || []).length === total
      this.refreshCopyEstimate()
    },
    onCopyCasesAllChange(val) {
      this.copySelection.copyAllCases = !!val
      if (val) {
        this.copySelection.caseIds = []
        this.copySelection.copyAllModules = true
        this.copySelection.moduleIds = []
      }
      this.refreshCopyEstimate()
    },
    onCopyCaseIdsChange(ids) {
      this.copySelection.copyAllCases = false
      if ((ids || []).length && !this.copySelection.copyAllModules && !(this.copySelection.moduleIds || []).length) {
        this.copySelection.copyAllModules = true
      }
      this.refreshCopyEstimate()
    },
    isCopyCurrentMember(row) {
      if (!row) return false
      const id = row.id != null ? row.id : row.userId
      return id != null && String(id) === String(this.currentUserId)
    },
    sanitizeCopyMemberIds(ids) {
      const me = this.currentUserId
      return (ids || []).filter(id => id != null && String(id) !== String(me))
    },
    onCopyMembersAllChange(val) {
      this.copySelection.copyAllMembers = !!val
      if (val) this.copySelection.memberIds = []
      this.refreshCopyEstimate()
    },
    onCopyMemberIdsChange(ids) {
      this.copySelection.copyAllMembers = false
      this.copySelection.memberIds = this.sanitizeCopyMemberIds(ids)
      this.refreshCopyEstimate()
    },
    buildCopyModuleTree(flat) {
      const nodes = new Map()
      const roots = []
      ;(flat || []).forEach(row => {
        nodes.set(row.id, {
          id: row.id,
          name: row.name,
          parentId: row.parentId || null,
          disabled: !!this.copySelection.copyAllModules,
          children: []
        })
      })
      nodes.forEach(node => {
        const pid = node.parentId
        if (pid != null && nodes.has(pid)) {
          nodes.get(pid).children.push(node)
        } else {
          roots.push(node)
        }
      })
      const sortRec = list => {
        list.sort((a, b) => String(a.name || '').localeCompare(String(b.name || ''), 'zh'))
        list.forEach(n => {
          if (n.children && n.children.length) sortRec(n.children)
          else delete n.children
        })
      }
      sortRec(roots)
      return roots
    },
    filterCopyModuleNode(value, data) {
      if (!value) return true
      return String(data.name || '').toLowerCase().includes(String(value).toLowerCase())
    },
    onCopyModuleSearch(val) {
      this.$nextTick(() => {
        const tree = this.$refs.copyModuleTree
        if (tree && tree.filter) tree.filter(val || '')
      })
    },
    syncCopyModuleTreeChecked() {
      this.$nextTick(() => {
        const tree = this.$refs.copyModuleTree
        if (!tree || !tree.setCheckedKeys) return
        if (this.copySelection.copyAllModules) {
          const allIds = (this.copyItems.modules || []).map(m => m.id)
          tree.setCheckedKeys(allIds)
        } else {
          tree.setCheckedKeys(this.copySelection.moduleIds || [])
        }
      })
    },
    onCopyModulesAllChange(val) {
      this.copySelection.copyAllModules = !!val
      if (val) {
        this.copySelection.moduleIds = []
      }
      this.syncCopyModuleTreeChecked()
      this.refreshCopyEstimate()
    },
    onCopyModuleTreeCheck() {
      const tree = this.$refs.copyModuleTree
      if (!tree) return
      this.copySelection.copyAllModules = false
      const checked = tree.getCheckedKeys(false) || []
      const half = tree.getHalfCheckedKeys ? (tree.getHalfCheckedKeys() || []) : []
      this.copySelection.moduleIds = [...new Set([...checked, ...half])]
      const total = (this.copyItems.modules || []).length
      if (total > 0 && this.copySelection.moduleIds.length === total) {
        this.copySelection.copyAllModules = true
        this.copySelection.moduleIds = []
      }
      this.refreshCopyEstimate()
    },
    onCopyModuleIdsChange() {
      this.copySelection.copyAllModules = false
      this.refreshCopyEstimate()
    },
    buildCopyDocumentTree(flat) {
      const nodes = new Map()
      const roots = []
      ;(flat || []).forEach(row => {
        nodes.set(row.id, {
          id: row.id,
          name: row.name,
          parentId: row.parentId || null,
          folder: !!row.folder,
          disabled: !!this.copySelection.copyAllDocuments,
          children: []
        })
      })
      nodes.forEach(node => {
        const pid = node.parentId
        if (pid != null && nodes.has(pid)) {
          nodes.get(pid).children.push(node)
        } else {
          roots.push(node)
        }
      })
      const sortRec = list => {
        list.sort((a, b) => {
          if (!!a.folder !== !!b.folder) return a.folder ? -1 : 1
          return String(a.name || '').localeCompare(String(b.name || ''), 'zh')
        })
        list.forEach(n => {
          if (n.children && n.children.length) sortRec(n.children)
          else delete n.children
        })
      }
      sortRec(roots)
      return roots
    },
    filterCopyDocumentNode(value, data) {
      if (!value) return true
      return String(data.name || '').toLowerCase().includes(String(value).toLowerCase())
    },
    onCopyDocumentSearch(val) {
      this.$nextTick(() => {
        const tree = this.$refs.copyDocumentTree
        if (tree && tree.filter) tree.filter(val || '')
      })
    },
    syncCopyDocumentTreeChecked() {
      this.$nextTick(() => {
        const tree = this.$refs.copyDocumentTree
        if (!tree || !tree.setCheckedKeys) return
        if (this.copySelection.copyAllDocuments) {
          const allIds = (this.copyItems.documents || []).map(d => d.id)
          tree.setCheckedKeys(allIds)
        } else {
          tree.setCheckedKeys(this.copySelection.documentIds || [])
        }
      })
    },
    onCopyDocumentsAllChange(val) {
      this.copySelection.copyAllDocuments = !!val
      if (val) this.copySelection.documentIds = []
      this.syncCopyDocumentTreeChecked()
      this.refreshCopyEstimate()
    },
    onCopyDocumentTreeCheck() {
      const tree = this.$refs.copyDocumentTree
      if (!tree) return
      this.copySelection.copyAllDocuments = false
      const checked = tree.getCheckedKeys(false) || []
      const half = tree.getHalfCheckedKeys ? (tree.getHalfCheckedKeys() || []) : []
      this.copySelection.documentIds = [...new Set([...checked, ...half])]
      const total = (this.copyItems.documents || []).length
      if (total > 0 && this.copySelection.documentIds.length === total) {
        this.copySelection.copyAllDocuments = true
        this.copySelection.documentIds = []
      }
      this.refreshCopyEstimate()
    },
    onCopyDocumentIdsChange() {
      this.copySelection.copyAllDocuments = false
      this.refreshCopyEstimate()
    },
    formatCopyBytes(bytes) {
      const n = Number(bytes) || 0
      if (n >= 1073741824) return (n / 1073741824).toFixed(n % 1073741824 === 0 ? 0 : 1) + ' GB'
      if (n >= 1048576) return (n / 1048576).toFixed(n % 1048576 === 0 ? 0 : 1) + ' MB'
      if (n >= 1024) return (n / 1024).toFixed(0) + ' KB'
      return n + ' B'
    },
    refreshCopyEstimate() {
      if (!this.copyEnabled || !this.copySelection.sourceProjectId) {
        this.copyEstimatedBytes = 0
        this.copyOverQuota = false
        return
      }
      clearTimeout(this.copyEstimateTimer)
      this.copyEstimateTimer = setTimeout(() => {
        const body = this.buildCopySelectionPayload()
        if (!body) {
          this.copyEstimatedBytes = 0
          this.copyOverQuota = false
          return
        }
        estimateProjectCopy(body).then(res => {
          const data = res.data || {}
          this.copyEstimatedBytes = Number(data.estimatedBytes) || 0
          if (data.maxProjectBytes) this.copyMaxBytes = Number(data.maxProjectBytes)
          this.copyOverQuota = !!data.overQuota
        }).catch(() => {})
      }, 200)
    },
    buildCopySelectionPayload() {
      if (!this.copyEnabled || !this.copySelection.sourceProjectId) return null
      const s = {
        ...this.copySelection,
        targetTeamId: this.currentTeamId,
        caseIds: this.copySelection.copyAllCases ? [] : [...(this.copySelection.caseIds || [])],
        moduleIds: this.copySelection.copyAllModules ? [] : [...(this.copySelection.moduleIds || [])],
        documentIds: this.copySelection.copyAllDocuments ? [] : [...(this.copySelection.documentIds || [])],
        memberIds: this.copySelection.copyAllMembers
          ? []
          : this.sanitizeCopyMemberIds(this.copySelection.memberIds),
        imSystemCodes: [...(this.copySelection.imSystemCodes || [])],
        gitSlugs: [...(this.copySelection.gitSlugs || [])]
      }
      const hasCases = s.copyAllCases || s.caseIds.length
      const hasModules = s.copyAllModules || s.moduleIds.length
      const hasDocs = s.copyAllDocuments || s.documentIds.length
      const hasMembers = s.copyAllMembers || s.memberIds.length
      const has =
        hasCases || hasModules || hasDocs || hasMembers ||
        s.copyAi || s.copyMcp || s.imSystemCodes.length || s.gitSlugs.length
      return has ? s : null
    },
    startCopyPoll(jobId) {
      this.copyJobId = jobId
      this.copyProgressVisible = true
      this.copyProgressDone = false
      this.copyJobPercent = 0
      this.copyJobItems = []
      this.copyJobError = null
      const tick = () => {
        getProjectCopyJob(jobId).then(res => {
          const job = res.data || {}
          this.copyJobPercent = Number(job.percent) || 0
          this.copyJobItems = job.items || []
          this.copyJobError = job.errorMessage || null
          const state = (job.state || '').toUpperCase()
          if (state === 'SUCCESS' || state === 'FAILED' || state === 'PARTIAL') {
            this.copyProgressDone = true
            clearInterval(this.copyPollTimer)
            this.copyPollTimer = null
            if (state === 'SUCCESS') this.$modal.msgSuccess(this.$t('project.copy.done'))
            else this.$modal.msgError(this.$t('project.copy.failed'))
          }
        }).catch(() => {})
      }
      tick()
      this.copyPollTimer = setInterval(tick, 1500)
    },
    onCopyProgressClose() {
      this.copyProgressVisible = false
      if (this.copyPollTimer) {
        clearInterval(this.copyPollTimer)
        this.copyPollTimer = null
      }
      this.pageFormCloseBaseline = null
      this.$router.back()
    },
    onSubmit() {
      this.form.teamId = this.currentTeamId;
      if (this.copyEnabled && this.copyOverQuota) {
        this.$modal.msgError(this.$t('project.copy.over-quota'))
        return
      }
      this.$refs["form"].validate(valid => {
        if (valid) {
          const payload = { ...this.form }
          const copySelection = this.buildCopySelectionPayload()
          if (copySelection) payload.copySelection = copySelection
          addProject(payload).then((res) => {
            const data = res.data
            const jobId = data && data.copyJobId
            if (jobId) {
              this.$modal.msgSuccess(this.$i18n.t('add-success'));
              this.startCopyPoll(jobId)
            } else {
              this.$modal.msgSuccess(this.$i18n.t('add-success'));
              this.pageFormCloseBaseline = null
              this.$router.back();
            }
          });
        }
      });
    },
  },
  watch: {
    copyEnabled(val) {
      if (val) this.loadCopySources()
    },
    copyModuleTree() {
      this.syncCopyModuleTreeChecked()
    },
    copyDocumentTree() {
      this.syncCopyDocumentTreeChecked()
    },
    'copySelection.copyAllModules'() {
      this.syncCopyModuleTreeChecked()
    },
    'copySelection.copyAllDocuments'() {
      this.syncCopyDocumentTreeChecked()
    },
    copyAssetTab(val) {
      if (val === 'modules') {
        this.syncCopyModuleTreeChecked()
        this.onCopyModuleSearch(this.copySearch.modules)
      } else if (val === 'documents') {
        this.syncCopyDocumentTreeChecked()
        this.onCopyDocumentSearch(this.copySearch.documents)
      }
    }
  },
  beforeUnmount() {
    clearTimeout(this.copyEstimateTimer)
    if (this.copyPollTimer) clearInterval(this.copyPollTimer)
  }
}
</script>

<style lang="scss" scoped>
  .project-add-page-container {
    width: 100%;
    max-width: 100%;
    overflow-x: hidden;
    .step2 {
      display: flex;
      display: -webkit-flex;
      flex-direction: column;
      column-gap: 10px;
      row-gap: 10px;
      border-left: var(--border-color-light) 1px solid;
      > :first-child {
        margin-bottom: 10px;
      }
    }
  }
  :deep(.el-table__cell){
    padding: 0;
  }
  .project-copy-panel {
    margin-top: 8px;
    width: 100%;
  }
  .project-copy-source-wrap {
    position: relative;
    width: 100%;
  }
  .project-copy-source-face {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    min-height: 40px;
    padding: 0 11px;
    box-sizing: border-box;
    border: 1px solid var(--el-border-color, #dcdfe6);
    border-radius: var(--el-input-border-radius, var(--el-border-radius-base, 4px));
    background: var(--el-fill-color-blank, var(--el-bg-color, #fff));
    cursor: pointer;
    transition: border-color 0.2s;
    &:hover {
      border-color: var(--el-border-color-hover, var(--el-color-primary));
    }
    &:focus {
      outline: none;
      border-color: var(--el-color-primary);
    }
  }
  .project-copy-source-wrap.is-open .project-copy-source-face,
  .project-copy-source-wrap.has-value .project-copy-source-face:hover {
    border-color: var(--el-color-primary);
  }
  .project-copy-source-face__body {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
    flex: 1 1 auto;
    overflow: hidden;
  }
  .project-copy-source-face__part {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
    flex: 1 1 0;
    max-width: calc(50% - 10px);
  }
  .project-copy-source-face__name {
    min-width: 0;
    flex: 1 1 auto;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 14px;
    line-height: 22px;
    color: var(--el-text-color-regular, #606266);
  }
  .project-copy-source-face__sep {
    flex-shrink: 0;
    color: var(--el-text-color-secondary, #909399);
  }
  .project-copy-source-face__placeholder {
    flex: 1 1 auto;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 14px;
    color: var(--el-text-color-placeholder, #a8abb2);
  }
  .project-copy-source-face__actions {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;
    color: var(--el-text-color-placeholder, #a8abb2);
  }
  .project-copy-source-face__clear {
    font-size: 14px;
    cursor: pointer;
    &:hover {
      color: var(--el-text-color-secondary, #909399);
    }
  }
  .project-copy-source-face__arrow {
    font-size: 14px;
    transition: transform 0.2s;
    &.is-reverse {
      transform: rotate(180deg);
    }
  }
  /* 不可 display:none：popper 需要真实锚点尺寸，否则会飞到左上角 */
  :deep(.project-copy-source-cascader-hidden) {
    position: absolute !important;
    inset: 0;
    z-index: 0;
    width: 100% !important;
    height: 100% !important;
    margin: 0 !important;
    opacity: 0 !important;
    pointer-events: none !important;
    overflow: hidden !important;
  }
  :deep(.project-copy-source-cascader-hidden .el-input),
  :deep(.project-copy-source-cascader-hidden .el-input__wrapper) {
    width: 100% !important;
    height: 100% !important;
    min-height: 0 !important;
    box-shadow: none !important;
  }
  .project-copy-source-node {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    max-width: 100%;
    vertical-align: middle;
  }
  .project-copy-source-avatar {
    flex-shrink: 0;
    font-size: 11px;
  }
  .project-copy-source-label {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .project-copy-tabs {
    margin-top: 10px;
    margin-bottom: 10px;
    :deep(.el-tabs__content) {
      padding: 10px 12px;
    }
    :deep(.el-tabs__item) {
      padding: 0 14px;
    }
  }
  .project-copy-tab-label {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }
  .project-copy-tab-icon {
    font-size: 14px;
    flex-shrink: 0;
  }
  .project-copy-tab-toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;
    flex-wrap: wrap;
  }
  .project-copy-search {
    flex: 1;
    min-width: 160px;
    max-width: 280px;
  }
  .project-copy-pick-list {
    display: flex;
    flex-direction: column;
    gap: 4px;
    max-height: 220px;
    overflow: auto;
    padding-right: 4px;
  }
  .project-copy-module-tree,
  .project-copy-document-tree {
    max-height: 260px;
    overflow: auto;
    background: transparent;
  }
  .project-copy-module-tree.is-locked,
  .project-copy-document-tree.is-locked {
    opacity: 0.85;
    pointer-events: none;
  }
  .project-copy-module-tree :deep(.el-tree-node__content),
  .project-copy-document-tree :deep(.el-tree-node__content) {
    height: 30px;
  }
  .project-copy-doc-node {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
  }
  .project-copy-doc-node__icon {
    flex-shrink: 0;
    font-size: 14px;
  }
  .project-copy-doc-node__name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .project-copy-empty {
    color: var(--text-color-secondary, #909399);
    font-size: 13px;
    padding: 8px 0;
  }
  .project-copy-tab-hint {
    margin-top: 8px;
    color: var(--text-color-secondary, #909399);
    font-size: 13px;
    line-height: 1.5;
  }
  .project-copy-ai-options {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }
  .project-copy-ai-option {
    .project-copy-tab-hint {
      margin-top: 4px;
      margin-left: 22px;
    }
  }
  .project-copy-im-options {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .project-copy-member-list {
    gap: 8px;
  }
  .project-copy-member-check {
    display: flex;
    align-items: flex-start;
    height: auto;
    margin-right: 0;
    width: 100%;
    padding: 4px 0;
    white-space: normal;
    :deep(.el-checkbox__input) {
      margin-top: 12px;
    }
    :deep(.el-checkbox__label) {
      flex: 1;
      min-width: 0;
      line-height: normal;
      white-space: normal;
    }
  }
  .project-copy-member-row {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 0;
  }
  .project-copy-member-roles {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    padding-left: 2px;
  }
  .project-copy-size {
    margin-left: 8px;
    color: var(--text-color-secondary, #909399);
    font-size: 12px;
  }
  .project-copy-estimate {
    margin-top: 6px;
    font-size: 13px;
  }
  .project-copy-estimate.is-over {
    color: var(--el-color-warning);
  }
  .project-copy-items {
    margin-top: 12px;
    padding-left: 18px;
    font-size: 13px;
  }
  .project-members {
    margin-top: 10px;
    /* 成员多时在浮层内滚动，避免撑出视口 */
    max-height: min(420px, 50vh);
    overflow-x: hidden;
    overflow-y: auto;
    .el-col {
      position: relative;
      padding: 0 5px;
      display: flex;
      align-items: center;
      flex-direction: row;
      flex-wrap: nowrap;
      justify-content: flex-start;
    }
    .el-col:hover {
      background-color: var(--table-row-hover-bg, #f2f6fc);
      border-radius: 5px;
      cursor: pointer;
    }
    :deep(.el-col[select="true"]){
      .member-nameplate-content {
        p {
          color: var(--cat2bug-primary, #1890ff);
        }
        span {
          color: var(--text-color-secondary, #909399);
        }
      }
    }
    .el-col[select="true"]:after {
      position: absolute;
      right: 20px;
      font-family: "element-icons";
      content: "";
      font-size: 12px;
      -webkit-font-smoothing: antialiased;
      color: var(--cat2bug-primary, #1890ff);
    }
  }
  :deep(.el-table){
    .el-form-item__error {
      display: none;
    }
    .el-input__inner {
      border-width: 0px;
    }
    .el-input__inner::placeholder {
      color: red;
    }
  }
</style>

<style lang="scss">
  /* 透明占位锚点：与 face 同框，供 cascader popper 定位（勿用 display:none） */
  .project-copy-source-cascader-hidden {
    position: absolute !important;
    inset: 0;
    z-index: 0;
    width: 100% !important;
    height: 100% !important;
    margin: 0 !important;
    opacity: 0 !important;
    pointer-events: none !important;
    overflow: hidden !important;
  }
  .project-copy-source-cascader-hidden .el-input,
  .project-copy-source-cascader-hidden .el-input__wrapper {
    width: 100% !important;
    height: 100% !important;
    min-height: 0 !important;
    box-shadow: none !important;
  }
  /* cascader 下拉挂到 body，需非 scoped */
  .project-copy-source-cascader-popper {
    .el-cascader-menu {
      min-width: 280px;
    }
    .el-cascader-menu__wrap.el-scrollbar__wrap {
      /* EP 默认固定 204px，选项少时会留大块空区 */
      height: auto !important;
      max-height: 260px;
    }
    .el-cascader-menu__list {
      min-height: auto !important;
    }
    .project-copy-source-node {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      max-width: 360px;
      vertical-align: middle;
    }
    .project-copy-source-avatar {
      flex-shrink: 0;
      font-size: 11px;
    }
    .project-copy-source-label {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
</style>
