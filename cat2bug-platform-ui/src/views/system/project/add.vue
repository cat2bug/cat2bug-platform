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
            <el-form-item :label="$t('project.introduction')">
              <el-input
                type="textarea"
                :placeholder="$t('enter-content')"
                rows="5"
                v-model="form.projectIntroduce"
                maxlength="255"
                show-word-limit
              >
              </el-input>
            </el-form-item>
          </el-col>
          <!--          项目图标-->
          <el-col :xs="24" :sm="24" :md="8" :lg="6" :xl="6" class="step2">
            <cat2-bug-image
              style="width: 150px; height: 150px; border-radius: 4px; overflow: hidden;"
              :src="projectIconPreviewUrl"
              fit="cover"></cat2-bug-image>
            <div :class="[PROJECT_ICON_POPOVER_HOST_CLASS, { 'is-popover-open': projectIconPopperVisible }]">
              <el-popover
                ref="projectIconPopover"
                v-model="projectIconPopperVisible"
                popper-class="project-icon-popper"
                placement="bottom"
                trigger="click">
                <div class="project-icon-popper">
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
                  <div class="project-icon-item" role="button" :aria-label="$t('upload.upload')" tabindex="-1">
                    <image-upload v-model="projectIcon" :limit="1" :file-type="[]" :is-show-tip="false" :is-show-clipboard-button="false" buttonStyle="width: 130px; height: 130px;" @input="handleSelectSelfImage" />
                  </div>
                </div>
                <el-button
                  ref="projectIconTrigger"
                  slot="reference"
                  size="mini"
                  class="project-icon-hint-change defect-kbd-hint-host"
                >{{$t('project.change-icon')}}
                  <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint" aria-hidden="true">{{ changeIconHintLetter }}</span>
                </el-button>
              </el-popover>
            </div>
          </el-col>
          <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="12">
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
                    v-model="memberPopoverVisible"
                    placement="bottom"
                    width="400"
                    @show="getMemberList"
                    @hide="resetMember"
                    trigger="click">
                    <el-input prefix-icon="el-icon-search" :placeholder="$t('team.search-placeholder')" v-model="memberParams.params.search" @input="getMemberList" size="mini"></el-input>
<!--                    成员列表-->
                      <el-row v-if="memberList && memberList.length>0" class="project-members">
                        <el-col :select="member.isSelect" :span="24" v-for="member in memberList" :key="member.userId" @click.native="selectAddMemberHandle(member)">
                          <member-nameplate :member="member"></member-nameplate>
                        </el-col>
                      </el-row>
                    <el-empty v-else :description="$t('no-data')"></el-empty>
                    <el-button slot="reference" size="mini" icon="el-icon-user-solid">{{$t('project.add-member')}}</el-button>
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
                      <template slot-scope="scope">
                        <member-nameplate :member="scope.row"></member-nameplate>
                      </template>
                    </el-table-column>
                    <el-table-column
                      prop="roleIds"
                      :label="$t('role')"
                      width="230">
                      <template slot-scope="scope">
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
                      <template slot-scope="scope">
                        <el-button
                          v-if="scope.row.userId!=currentUserId"
                          size="mini"
                          type="text"
                          icon="el-icon-delete"
                          @click="deleteMemberHandle(scope.row)"
                          v-hasPermi="['system:project:remove']"
                        >{{$t('delete')}}</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </el-col>
              </el-row>
            </el-form-item>
<!--            保存取消按钮-->
            <el-form-item class="page-form-actions">
              <div class="page-form-actions__buttons">
                <el-button @click="goBack">{{$t('cancel')}}</el-button>
                <el-button class="defect-kbd-hint-host" type="primary" @click="onSubmit">
                  {{$t('finish')}}
                  <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
                </el-button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-row>
  </div>
</template>

<script>
import {addProject, listProjectRole} from "@/api/system/project";
import { listMember } from "@/api/system/team";
import MemberNameplate from "@/components/MemberNameplate"
import { resolveProjectIconUrl } from '@/utils/upload-asset'
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

export default {
  name: "ProjectAdd",
  mixins: [projectCreateFormKbd],
  components:{ MemberNameplate },
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
        projectIntroduce: null,
        projectIcon: null,
        members: []
      },
      projectIconPopperVisible: false,
      activeProjectIconIndex: 1,
      rules: {
        projectName: [
          { required: true, message: this.$i18n.t('project.project-name-cannot-empty'), trigger: "blur" }
        ],
        roleIds: [
          { type: 'array', required: true, message: this.$i18n.t('member.please-select-role'), trigger: 'change'}
        ]
      }
    }
  },
  computed: {
    activeProjectIconUrl: function () {
      return function (index){
        return require('@/assets/images/project/project_icon'+index+'.svg')
      }
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
      return this.$store.state.user.id;
    },
    currentTeamId: function () {
      return this.$store.state.user.config.currentTeamId;
    },
    projectIconPreviewUrl() {
      return resolveProjectIconUrl(this.form.projectIcon)
    },
    changeIconHintLetter() {
      return shortcutStore.getLetter(`action.${PROJECT_CREATE_KBD_SCOPE}.changeIcon`, 'I')
    }
  },
  created() {
    this.form.projectIcon = this.activeProjectIconUrl(1);
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
      this.memberParams.params.excludeMembers = this.form.members.map(m=>m.userId);
      listMember(this.currentTeamId,this.memberParams).then(res=>{
        this.memberList = res.rows.map(m=> {
          m.isSelect=false;
          if(this.roleOptions && this.roleOptions.length>0){
            m.roleIds=[this.roleOptions.filter(r=>r.projectCreateBy==false)[0].roleId];
          } else {
            m.roleIds=[];
          }
          if(this.currentUserId == m.userId && this.form.members.length == 0) {
            m.isSelect = true;
            m.roleIds = this.roleOptions.filter(r=>r.projectCreateBy).map(r=>r.roleId);
            this.form.members.push(m);
          }
          return m;
        });
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
      member.isSelect = !member.isSelect;
      if(member.isSelect){
        this.form.members.push(member);
        let membersSet = new Set(this.form.members);
        this.form.members = Array.from(membersSet);
      } else {
        this.form.members = this.form.members.filter(m=>m.isSelect);
      }
    },
    clickProjectIconHandle(index) {
      this.activeProjectIconIndex = index;
      this.form.projectIcon = this.activeProjectIconUrl(index);
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
    onSubmit() {
      this.form.teamId = this.currentTeamId;
      this.$refs["form"].validate(valid => {
        if (valid) {
          addProject(this.form).then(() => {
            this.$modal.msgSuccess(this.$i18n.t('add-success'));
            this.pageFormCloseBaseline = null
            this.$router.back();
          });
        }
      });
    },
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
      :first-child {
        margin-bottom: 10px;
      }
      :last-child {
        width: 150px;
      }
    }
  }
  .project-icon-popper {
    width: 550px;
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    padding: 0;
    margin: 0;
    .project-icon-item {
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 0;
      margin: 0;
    }
    .el-image {
      width: 130px;
      height: 130px;
      border-radius: 4px;
      cursor: pointer;
    }
  }
  ::v-deep .el-table__cell {
    padding: 0;
  }
  .project-members {
    margin-top: 10px;
    .el-col {
      padding: 0 5px;
      display: flex;
      align-items: center;
      flex-direction: row;
      flex-wrap: nowrap;
      justify-content: flex-start;
    }
    .el-col:hover {
      background-color: #F2F6FC;
      border-radius: 5px;
      cursor: pointer;
    }
    ::v-deep .el-col[select="true"] {
      .member-nameplate-content {
        p {
          color: #1890ff;
        }
        span {
          color: #909399;
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
      color: #1890ff;
    }
  }
  ::v-deep .el-table {
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
