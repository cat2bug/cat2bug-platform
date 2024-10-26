<template>
  <el-drawer
    size="50%"
    :visible.sync="visible"
    direction="rtl"
    :append-to-body="appendToBody"
    :before-close="closeDefectDrawer"
    @closed="cancel">
    <template slot="title">
      <div class="case-add-header">
        <div class="case-add-title">
          <i class="el-icon-arrow-left" @click="cancel"></i>
          <focus-member-list
            v-model="form.focusList"
            module-name="case"
            :data-id="form.caseId"
            :tooltip="false"
          />
          <h3>{{title}}</h3>
        </div>
        <div>
          <el-button @click="cancel" icon="el-icon-close" :class="isAddMode?'':'green-button'" size="mini">{{$t('close')}}</el-button>
          <el-button v-if="isAddMode" v-hasPermi="['system:case:add']" type="primary" icon="el-icon-finished" @click="submitForm" size="mini">{{$t('create')}}</el-button>
          <el-button v-else v-hasPermi="['system:case:edit']" type="success" icon="el-icon-finished" @click="submitForm" size="mini">{{$t('modify')}}</el-button>
        </div>
      </div>
    </template>
    <div class="app-container">

      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item>
          <el-checkbox v-if="isAddMode" class="create-next-case" v-model="isCreateNextCase">{{ $t('case.create-next-case') }}</el-checkbox>
        </el-form-item>
        <el-form-item :label="$t('case.name')" prop="caseName">
          <el-input type="textarea" v-model="form.caseName" :placeholder="$t('case.please-enter-title')" rows="3" maxlength="255" show-word-limit />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item :label="$t('module')" prop="moduleId">
              <select-module size="medium" v-model="form.moduleId" :project-id="projectId"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('level')" prop="caseLevel">
              <cat2-bug-select-level v-model="form.caseLevel" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('preconditions')" prop="casePreconditions">
          <el-input type="textarea" v-model="form.casePreconditions" :placeholder="$t('case.please-enter-prerequisite')" maxlength="255" rows="3" show-word-limit />
        </el-form-item>
        <el-form-item :label="$t('expect')" prop="caseExpect">
          <el-input type="textarea" v-model="form.caseExpect" :placeholder="$t('case.please-enter-expectations')" maxlength="255" rows="3" show-word-limit />
        </el-form-item>
        <el-form-item :label="$t('data')" prop="caseData">
          <el-input type="textarea" v-model="form.caseData" :placeholder="$t('case.please-enter-data')" maxlength="10000" rows="3" show-word-limit />
        </el-form-item>
        <el-form-item :label="$t('step')" prop="caseStep">
          <template slot="label">
            <div class="form-item-case-step">
              <el-radio-group v-model="caseStepSwitchType" @input="caseStepPanelTypeChangeHandle">
                <el-radio-button label=""><svg-icon icon-class="option" /></el-radio-button>
                <el-radio-button label="list"><svg-icon icon-class="list2" /></el-radio-button>
                <el-radio-button label="input"><svg-icon icon-class="textarea" /></el-radio-button>
              </el-radio-group>
              <label>{{ $t('step') }}</label>
            </div>
          </template>
          <case-step-panel ref="caseStepPanel" v-model="form.caseStep" :panel-type="caseStepSwitchType" />
        </el-form-item>
        <el-form-item :label="$t('remark')" prop="remark">
          <el-input type="textarea" v-model="form.remark" :placeholder="$t('please-enter-remark')" maxlength="255" rows="6" show-word-limit />
        </el-form-item>
        <el-form-item :label="$t('image')" prop="imgUrls">
          <image-upload v-model="form.imgUrls" :limit="9"></image-upload>
        </el-form-item>
      </el-form>
    </div>
  </el-drawer>
</template>

<script>
import {addCase, closeEditWindow, getCase, updateCase} from "@/api/system/case";
import CaseStepPanel from "./CaseStepPanel"
import SelectModule from "@/components/Module/SelectModule"
import Cat2BugSelectLevel from "@/components/Cat2BugSelectLevel";
import Label from "@/components/Cat2BugStatistic/Components/Label";
import FocusMemberList from "@/components/FocusMemberList";

const CASE_STEP_PANEL_TYPE_CACHE_KEY = 'case-step-panel-type';

export default {
  name: "AddCase",
  components: {Label,CaseStepPanel,SelectModule,Cat2BugSelectLevel,FocusMemberList},
  data() {
    return {
      caseStepSwitchType: '',
      // 是否显示创建组件
      visible: false,
      // 是否连续创建用例
      isCreateNextCase: true,
      // 表单参数
      form: {
        caseStep:[{}],
        caseLevel: 1
      },
      // 表单校验
      rules: {
        caseName: [
          { required: true, message: this.$i18n.t('case.name-cannot-empty'), trigger: "input" }
        ],
        caseLevel: [
          { required: true, message: this.$i18n.t('case.level-cannot-empty'), trigger: "input" }
        ],
        moduleId: [
          { required: true, message: this.$i18n.t('case.module-cannot-empty'), trigger: "input" }
        ],
        caseExpect: [
          { required: true, message: this.$i18n.t('case.expect-cannot-empty'), trigger: "input" }
        ],
      }
    }
  },
  props: {
    moduleId: {
      type: Number,
      default: null,
    },
    appendToBody: {
      type: Boolean,
      default: false
    }
  },
  watch: {
    moduleId: function (n) {
      this.form.moduleId = n;
    }
  },
  computed: {
    /** 获取当前用户id */
    userId: function () {
      return this.$store.state.user.id;
    },
    /** 获取项目id */
    projectId: function() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 是否是添加模式 */
    isAddMode: function (){
      return this.form.caseId == null
    },
    title: function () {
      if(this.isAddMode) {
        return this.$i18n.t('case.create');
      } else {
        return this.$i18n.t('case.modify');
      }
    }
  },
  methods: {
    /** 初始化浮动菜单 */
    initFloatMenu() {
      this.$floatMenu.windowsInit(document.querySelector('.main-container'));
      let tools = [{
        id: 'closeAddCase',
        name: 'close',
        visible: true,
        plain: true,
        type: '',
        icon: 'close',
        prompt: 'close',
        click : this.cancel
      }];
      if(this.isAddMode) {
        tools.push({
          id: 'saveAddCase',
          name: 'create',
          visible: true,
          plain: false,
          type: 'primary',
          icon: 'finish',
          prompt: 'create',
          permissions: ['system:case:add'],
          click : this.submitForm
        })
      } else {
        tools.push({
          id: 'editAddCase',
          name: 'modify',
          visible: true,
          plain: false,
          type: 'success',
          icon: 'finish',
          prompt: 'modify',
          permissions: ['system:case:edit'],
          click : this.submitForm
        })
      }
      this.$floatMenu.resetMenus(tools);
    },
    open(caseId) {
      let self = this;
      this.reset();
      if(caseId) {
        getCase(caseId).then(response => {
          this.form = response.data;
          this.visible = true;
          this.$nextTick(()=>{
            self.$refs['caseStepPanel'].reset();
          });
          this.initFloatMenu();
        });
      } else {
        this.visible = true;
        this.$nextTick(()=>{
          self.$refs['caseStepPanel'].reset();
        });
        this.initFloatMenu();
      }
    },
    /** 关闭缺陷抽屉窗口 */
    closeDefectDrawer(done) {
      // 如果是编辑，点击背景关闭
      if(this.isAddMode==false){
        done();
        return;
      }
      closeEditWindow();
    },
    // 取消按钮
    cancel() {
      this.$emit('close');
      this.visible = false;
      this.reset();
    },
    /** 循环时的重设 */
    loopReset() {
      let self = this;
      this.form = {
        caseId: null,
        caseName: null,
        caseData: null,
        moduleId: this.form.moduleId,
        caseType: null,
        caseExpect: null,
        caseStep: [{}],
        caseLevel: this.form.caseLevel,
        casePreconditions: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        caseNum: null,
        projectId: this.projectId
      };
      this.resetForm("form");
      this.caseStepSwitchType = this.getCaseStepSwitchType();
      this.$nextTick(()=>{
        self.$refs['caseStepPanel'].reset();
      })
    },
    // 表单重置
    reset() {
      this.form = {
        caseId: null,
        caseName: null,
        caseData: null,
        moduleId: this.moduleId,
        caseType: null,
        caseExpect: null,
        caseStep: [{}],
        caseLevel: 1,
        casePreconditions: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        caseNum: null,
        projectId: this.projectId
      };
      this.resetForm("form");
      this.caseStepSwitchType = this.getCaseStepSwitchType();
    },
    /** 获取用例步骤面板类型 */
    getCaseStepSwitchType() {
      return this.$cache.session.get(CASE_STEP_PANEL_TYPE_CACHE_KEY) || '';
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.caseStep = this.form.caseStep.filter(c=>c.stepDescribe || c.stepExpect);
          if (this.isAddMode) {
            addCase(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('create-success'));
              if(this.isCreateNextCase==false) {
                this.visible = false;
                this.reset();
              } else {
                this.loopReset();
              }
              this.$emit('added');
            });
          } else {
            updateCase(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('modify-success'));
              this.reset();
              this.visible = false;
              this.$emit('added');
            });
          }
        }
      });
    },
    caseStepPanelTypeChangeHandle() {
      this.$cache.session.set(CASE_STEP_PANEL_TYPE_CACHE_KEY,this.caseStepSwitchType);
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-drawer {
  border-left: 3px solid #13ce66;
}
::v-deep .el-drawer__header {
  margin-bottom: 0px;
}
::v-deep .el-drawer__close-btn {
  display: none;
}
.case-add-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-direction: row;
  .case-add-title {
    display: inline-flex;
    justify-content: flex-start;
    align-items: center;
    flex-direction: row;
    overflow: hidden;
    gap: 10px;
    > * {
      float: left;
    }
    .el-icon-arrow-left {
      font-size: 22px;
    }
    .el-icon-arrow-left:hover {
      cursor: pointer;
      color: #909399;
    }
  }
}
.create-next-case {
  left: -25px;
}
.form-item-case-step {
  display:flex;
  flex-direction:row;
  align-items: center;
  justify-content: flex-end;
  > * {
    margin-left: 5px;
  }
  label {
    line-height: 24px;
  }
  ::v-deep .el-radio-button__inner {
    padding: 0px;
  }
}
.green-button:hover {
  background-color: #f0f9eb;
  border: 1px solid #c2e7b0;
  color: #67c23a;
}
</style>
