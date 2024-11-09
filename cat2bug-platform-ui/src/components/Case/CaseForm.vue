<template>
  <el-form ref="form" :model="form" :rules="rules" label-width="120px">
    <el-form-item :label="$t('title')" prop="caseName">
      <el-input type="textarea" v-model="form.caseName" :placeholder="$t('case.please-enter-title')" rows="3" maxlength="255" show-word-limit />
    </el-form-item>
    <el-row>
      <el-col :span="12">
        <el-form-item :label="$t('module')" prop="moduleId">
          <select-module size="medium" v-model="form.moduleId" :project-id="form.projectId"  @input="moduleChangeHandle"/>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item :label="$t('level')" prop="caseLevel">
          <cat2-bug-select-level v-model="form.caseLevel" />
        </el-form-item>
      </el-col>
    </el-row>
    <el-form-item :label="$t('preconditions')" prop="casePreconditions">
      <el-input type="textarea" v-model="form.casePreconditions" :placeholder="$t('case.please-enter-prerequisite')" maxlength="65535" rows="3" show-word-limit />
    </el-form-item>
    <el-form-item :label="$t('expect')" prop="caseExpect">
      <el-input type="textarea" v-model="form.caseExpect" :placeholder="$t('case.please-enter-expectations')" maxlength="65535" rows="3" show-word-limit />
    </el-form-item>
    <el-form-item :label="$t('step')" prop="caseStep">
      <template slot="label">
        <div class="form-item-case-form-step">
          <el-radio-group v-model="caseStepSwitchType" @input="caseStepPanelTypeChangeHandle($event)">
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
  </el-form>
</template>

<script>
import CaseStepPanel from "./CaseStepPanel"
import SelectModule from "@/components/Module/SelectModule"
import Cat2BugSelectLevel from "@/components/Cat2BugSelectLevel";
const CASE_STEP_PANEL_TYPE_CACHE_KEY = 'case-step-panel-type';
export default {
  name: "CaseForm",
  components: {Cat2BugSelectLevel,CaseStepPanel,SelectModule},
  data() {
    return {
      caseStepSwitchType: '',
      // 表单参数
      // form: {
      //   caseStep:[{}],
      //   caseLevel: 1
      // },
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
    form: {
      type: Object,
      default: {
        caseStep:[{}],
        caseLevel: 1
      }
    }
  },
  methods: {
    setCase(caseModel) {
      let self = this;
      // this.form = caseModel;
      // this.form = {
      //   caseId: caseModel.caseId,
      //   caseName: caseModel.caseName,
      //   moduleId: caseModel.moduleId,
      //   caseType: caseModel.caseType,
      //   caseExpect: caseModel.caseExpect,
      //   caseStep: caseModel.caseStep,
      //   caseLevel: caseModel.caseLevel,
      //   casePreconditions: caseModel.casePreconditions,
      //   createBy: caseModel.createBy,
      //   createTime: caseModel.createTime,
      //   updateBy: caseModel.updateTime,
      //   updateTime: caseModel.updateTime,
      //   caseNum: caseModel.caseNum,
      //   projectId: caseModel.projectId
      // };
      // this.resetForm("form");
      this.caseStepSwitchType = this.getCaseStepSwitchType();
      this.$nextTick(()=>{
        self.$refs['caseStepPanel'].reset();
      });
    },
    // 表单重置
    reset() {
      this.setCase({
        caseId: null,
        caseName: null,
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
      });
    },
    /** 获取用例步骤面板类型 */
    getCaseStepSwitchType() {
      let caseStepPanelType = this.$cache.session.get(CASE_STEP_PANEL_TYPE_CACHE_KEY);

      return caseStepPanelType?caseStepPanelType:'';
    },
    caseStepPanelTypeChangeHandle(e) {
      this.$cache.session.set(CASE_STEP_PANEL_TYPE_CACHE_KEY,this.caseStepSwitchType);
    },
    /** 选中模块改变的处理操作 */
    moduleChangeHandle(moduleId,module) {
      this.form.moduleName = module.moduleName;
    }
  }
}
</script>

<style lang="scss" scoped>
.form-item-case-form-step {
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
</style>
