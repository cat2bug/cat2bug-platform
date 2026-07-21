<template>
  <div class="app-container project-form-page">
    <el-row class="project-add-page-header project-option-sub-hint-back">
      <el-page-header @back="goBack" :content="$t('project.info')">
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
            <el-form-item :label="$t('project.path-share')" v-if="projectSharePath">
              <el-input :model-value="projectSharePath" readonly>
                <template #append>
                  <el-button
                    v-clipboard:copy="projectShareUrl"
                    v-clipboard:success="clipboardShareSuccess"
                    v-clipboard:error="clipboardShareError"
                  >{{ $t('copy') }}</el-button>
                </template>
              </el-input>
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
            <el-form-item class="page-form-actions">
              <div class="page-form-actions__buttons">
                <el-button @click="goBack">{{$t('cancel')}}</el-button>
                <el-button class="defect-kbd-hint-host" type="primary" @click="onSubmit">
                  {{$t('update')}}
                  <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
                </el-button>
              </div>
            </el-form-item>
          </el-col>
          <!--          项目图标-->
          <el-col :xs="24" :sm="24" :md="8" :lg="6" :xl="6" class="step2">
            <cat2-bug-image
              style="width: 150px; height: 150px; border-radius: 4px; overflow: hidden;"
              :key="projectIconPreviewUrl"
              :src="projectIconPreviewUrl"
              fit="cover"></cat2-bug-image>
            <div :class="[PROJECT_ICON_POPOVER_HOST_CLASS, { 'is-popover-open': projectIconPopperVisible }]">
              <el-popover
                ref="projectIconPopover"
                v-model:visible="projectIconPopperVisible"
                popper-class="project-icon-popper"
                placement="bottom"
                :width="560"
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
                    <image-upload v-model="projectIcon" :limit="1" :file-type="[]" :is-show-tip="false" :is-show-clipboard-button="false" buttonStyle="width: 130px; height: 130px;" @input="handleSelectSelfImage" />
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
import {addProject, getProject, listProjectRole, updateProject} from "@/api/system/project";
import { listMember } from "@/api/system/team";
import MemberNameplate from "@/components/MemberNameplate"
import { resolveProjectIconUrl } from '@/utils/upload-asset'
import { getDefaultProjectIconUrl, getProjectIconStoragePath } from '@/utils/project-default-icons'
import projectOptionSubFormKbd from '@/mixins/project-option-sub-form-kbd'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { PROJECT_OPTION_KBD_SCOPE } from '@/mixins/project-option-sub-kbd'
import {
  PROJECT_ICON_POPOVER_HOST_CLASS,
  destroyProjectIconPopoverKbd,
  initProjectIconPopoverKbd,
  shortcutOpenProjectIconPopover
} from '@/utils/project-icon-popover-kbd'
import { storeToRefs } from 'pinia';
import { useUserStore } from '@/stores/user'
import {
  buildProjectPathSegment,
  buildProjectShareUrl,
  confirmPathKeyRename,
  createPathKeyValidator
} from '@/utils/path-key'

export default {
  name: "ProjectEdit",
  mixins: [projectOptionSubFormKbd],
  components:{ MemberNameplate },
  setup() {
    const userStore = useUserStore()
    const { config } = storeToRefs(userStore)
    return { config }
  },
  data() {
    return {
      PROJECT_ICON_POPOVER_HOST_CLASS,
      memberParams: {
        params: {
          excludeMembers: [],
          search: null
        }
      },                                // 用户查询条件
      roleOptions: [],                  // 角色选项
      memberList:[],                    // 成员列表
      projectMemberSwitch: false,       // 项目成员开关
      projectIcon: null,
      form:{
        teamId: this.getTeamId(),  // 团队id
        projectName: null,              // 项目名称
        projectIntroduce: null,         // 项目介绍
        projectIcon: null,              // 项目图标
        members: []                     // 项目成员
      },                                // 提交的表单
      projectIconPopperVisible: false,  // 是否显示项目图标弹窗
      activeProjectIconIndex: 1,        // 当前选中的项目图标索引
      originalProjectKey: null,
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
    projectSharePath() {
      return buildProjectPathSegment(this.form.teamKey, this.form.projectKey)
    },
    projectShareUrl() {
      return buildProjectShareUrl(this.form.teamKey, this.form.projectKey)
    },
    /** 计算当前项目图标地址 */
    activeProjectIconUrl() {
      return (index) => getDefaultProjectIconUrl(index)
    },
    projectIconPreviewUrl() {
      return resolveProjectIconUrl(this.form.projectIcon)
    },
    changeIconHintLetter() {
      return shortcutStore.getLetter(`action.${PROJECT_OPTION_KBD_SCOPE}.changeIcon`, 'I')
    }
  },
  created() {
    this.getProject();
    this.getRoleList();
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
    getProjectOptionSubRegisterActions() {
      return [
        {
          key: 'changeIcon',
          defaultLetter: 'I',
          titleKey: 'keyboard.act.project-change-icon',
          run: () => this.shortcutOpenProjectIconPopover()
        }
      ]
    },
    getProjectOptionSubActionHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${PROJECT_OPTION_KBD_SCOPE}.${key}`, def)
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
        letter: shortcutStore.getLetter(`action.${PROJECT_OPTION_KBD_SCOPE}.changeIcon`, 'I'),
        injectBadge: false,
        onActivate: () => this.shortcutOpenProjectIconPopover()
      }]
    },
    shortcutOpenProjectIconPopover() {
      shortcutOpenProjectIconPopover(this)
    },
    shouldProjectOptionSubEscGoBack() {
      return !this.projectIconPopperVisible
    },
    shortcutSave() {
      this.onSubmit()
    },
    serializePageFormCloseState() {
      return JSON.stringify({
        form: { ...this.form },
        activeProjectIconIndex: this.activeProjectIconIndex,
        projectIcon: this.projectIcon
      })
    },
    getProjectId() {
      return parseInt(this.config.currentProjectId);
    },
    getProject() {
      getProject(this.getProjectId()).then(res=>{
        this.form = res.data;
        this.originalProjectKey = res.data && res.data.projectKey ? res.data.projectKey : null;
        this.$nextTick(() => this.capturePageFormCloseBaseline());
      });
    },
    getRoleList() {
      listProjectRole(0).then(res => {
        this.roleOptions = res.rows?res.rows.map(r=>{
          r.roleName = r.roleNameI18nKey?this.$t(r.roleNameI18nKey):r.roleName;
          return r;
        }):[];
        this.getMemberList();
        this.dialogVisible = true;
        this.$nextTick(() => initProjectIconPopoverKbd(this));
      });
    },
    /** 获取待添加的成员 */
    getMemberList(){
      this.memberParams.params.excludeMembers = this.form.members?this.form.members.map(m=>m.userId):[];
      listMember(this.getTeamId(),this.memberParams).then(res=>{
        // 赋值成员列表
        this.memberList = res.rows.map(m=> {
          // 设置不选中
          m.isSelect=false;
          // 设置默认角色
          if(this.roleOptions && this.roleOptions.length>0){
            m.roleIds=[this.roleOptions[0].roleId];
          } else {
            m.roleIds=[];
          }
          return m;
        });
      });
    },
    /** 获取团队id */
    getTeamId() {
      return this.config.currentTeamId;
    },
    /** 删除选中成员 */
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
    /** 选中添加的成员 */
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
    /** 点击选中的项目图标处理方法 */
    clickProjectIconHandle(index) {
      this.activeProjectIconIndex = index;
      this.form.projectIcon = getProjectIconStoragePath(index);
      this.projectIconPopperVisible = false;
    },
    /** 点击选择自定义图片处理方法 */
    handleSelectSelfImage(url) {
      if(url) {
        this.form.projectIcon = process.env.VUE_APP_BASE_API + url;                              // 赋值项目图标
        this.projectIconPopperVisible = false;
      } else {
        this.clickProjectIconHandle(1);
      }
    },
    /** 提交按钮 */
    onSubmit() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          confirmPathKeyRename(this, this.originalProjectKey, this.form.projectKey, () => {
            updateProject(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('update.success'));
              this.originalProjectKey = this.form.projectKey;
              this.pageFormCloseBaseline = null;
              this.goBack();
            });
          });
        }
      });
    },
    clipboardShareSuccess() {
      this.$modal.msgSuccess(this.$t('project.path-share-copied'));
    },
    clipboardShareError() {
      this.$modal.msgError(this.$t('git.repo.copy-failed'));
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
      display: -webkit-flex; /* Safari */
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
    :deep(.el-col[select="true"]){
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
