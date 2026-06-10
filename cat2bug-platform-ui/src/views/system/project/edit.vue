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
                <!--              选择项目图标按钮-->
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
          <!--            保存取消按钮-->
          <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="12">
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
import projectOptionSubFormKbd from '@/mixins/project-option-sub-form-kbd'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { PROJECT_OPTION_KBD_SCOPE } from '@/mixins/project-option-sub-kbd'
import {
  PROJECT_ICON_POPOVER_HOST_CLASS,
  destroyProjectIconPopoverKbd,
  initProjectIconPopoverKbd,
  shortcutOpenProjectIconPopover
} from '@/utils/project-icon-popover-kbd'

export default {
  name: "ProjectEdit",
  mixins: [projectOptionSubFormKbd],
  components:{ MemberNameplate },
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
      // 表单校验
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
    /** 计算当前项目图标地址 */
    activeProjectIconUrl: function () {
      return function (index){
        return require('@/assets/images/project/project_icon'+index+'.svg')
      }
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
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    getProject() {
      getProject(this.getProjectId()).then(res=>{
        this.form = res.data;
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
      return this.$store.state.user.config.currentTeamId;
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
      this.activeProjectIconIndex = index;                      // 赋值当前所选索引
      this.form.projectIcon = this.activeProjectIconUrl(index); // 赋值项目图标
      this.projectIconPopperVisible = false;                    // 隐藏图标选择组件
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
          updateProject(this.form).then(response => {
            this.$modal.msgSuccess(this.$i18n.t('update.success'));
            this.goBack();
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
      display: -webkit-flex; /* Safari */
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
  ::v-deep .project-icon-hint-change.defect-kbd-hint-host .defect-kbd-hint {
    width: max-content !important;
    min-width: 16px;
    padding: 0 2px;
    box-sizing: border-box;
    transform: translate(50%, calc(50% + 10px));
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
