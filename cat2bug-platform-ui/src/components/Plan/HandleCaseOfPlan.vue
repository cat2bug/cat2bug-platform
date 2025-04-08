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
        <div class="tools-row">
          <el-button v-if="!isAddMode"@click="prevCase" type="text" icon="el-icon-arrow-left" size="mini"></el-button>
          <el-button v-if="!isAddMode"@click="nextCase" type="text" icon="el-icon-arrow-right" size="mini"></el-button>
          <el-button @click="cancel" icon="el-icon-close" :class="isAddMode?'':'green-button'" size="mini">{{$t('close')}}</el-button>
          <plan-item-tools v-model="planItem" :plan="plan" type="primary" :project-id="projectId" @change="handlePlanItemChange" @close="initFloatMenu" />
        </div>
      </div>
    </template>
    <div class="app-container" v-loading="loading">
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item :label="$t('case.number')" prop="caseNum">
          <span>#{{ form.caseNum }}</span>
        </el-form-item>
        <el-form-item :label="$t('case.name')" prop="caseName">
          <el-input type="textarea" v-model="form.caseName" :placeholder="$t('empty-data')" rows="4" maxlength="255" readonly />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item :label="$t('module')" prop="moduleId">
              <select-module ref="selectModule" size="medium" v-model="form.moduleId" :project-id="projectId" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('level')" prop="caseLevel">
              <cat2-bug-select-level v-model="form.caseLevel" readonly />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('preconditions')" prop="casePreconditions">
          <el-input type="textarea" v-model="form.casePreconditions" :placeholder="$t('empty-data')" maxlength="65535" rows="3" readonly />
        </el-form-item>
        <el-form-item :label="$t('step')" prop="caseStep">
          <case-step ref="caseStep" class="step" :case-steps="form.caseStep" />
        </el-form-item>
        <el-form-item :label="$t('data')" prop="caseData">
          <el-input type="textarea" v-model="form.caseData" :placeholder="$t('empty-data')" maxlength="65535" rows="3" readonly />
        </el-form-item>
        <el-form-item :label="$t('expect')" prop="caseExpect">
          <el-input type="textarea" v-model="form.caseExpect" :placeholder="$t('empty-data')" maxlength="65535" rows="3" readonly />
        </el-form-item>
        <el-form-item :label="$t('remark')" prop="remark">
          <el-input type="textarea" v-model="form.remark" :placeholder="$t('empty-data')" maxlength="255" rows="6" readonly />
        </el-form-item>
        <el-form-item :label="$t('image')" prop="imgUrls">
          <div class="case-image">
            <el-image
              v-for="(img,index) in getUrl(form.imgUrls)"
              :key="index"
              :src="img"
              :preview-src-list="getUrl(form.imgUrls)"
              fit="contain"></el-image>
          </div>
        </el-form-item>
        <el-form-item :label="$t('annex')" prop="annexUrls">
          <el-link type="primary" v-for="(file,index) in getUrl(form.annexUrls)" :key="index" :href="file">{{getFileName(file)}}</el-link>
        </el-form-item>
      </el-form>
    </div>
  </el-drawer>
</template>

<script>
import {addCase, closeEditWindow, getCase, getNextCase, getPrevCase, updateCase} from "@/api/system/case";
import CaseStepPanel from "@/components/Case/CaseStepPanel";
import SelectModule from "@/components/Module/SelectModule"
import Cat2BugSelectLevel from "@/components/Cat2BugSelectLevel";
import Label from "@/components/Cat2BugStatistic/Components/Label";
import FocusMemberList from "@/components/FocusMemberList";
import PlanItemTools from "@/components/Plan/PlanItemTools";
import CaseStep from "@/components/Case/CaseStep";
import {getNextPlanItem, getPrevPlanItem, getPrevPlanItemId} from "@/api/system/PlanItem";
const CASE_STEP_PANEL_TYPE_CACHE_KEY = 'case-step-panel-type';

export default {
  name: "HandleCaseOfPlan",
  components: {Label,CaseStepPanel,SelectModule,Cat2BugSelectLevel,FocusMemberList,PlanItemTools,CaseStep},
  model: {
    prop: 'planItem',
    event: 'change'
  },
  data() {
    return {
      loading: false,
      caseStepSwitchType: '',
      // 是否显示创建组件
      visible: false,
      // 是否连续创建用例
      isCreateNextCase: true,
      // 搜索条件
      params: null,
      // 测试计划
      plan: {},
      planItem: {},
      // 表单参数
      form: {
        caseStep:[{}],
        moduleId: 0,
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
      return this.$i18n.t('case.test');
    },
    getUrl: function () {
      return function (urls){
        let imgs = urls?urls.split(','):[];
        return imgs.map(i=>{
          return process.env.VUE_APP_BASE_API + i;
        })
      }
    },
    getFileName: function () {
      return function (url) {
        if(!url) return null;
        let arr = url.split('\/');
        return arr[arr.length-1];
      }
    },
  },
  methods: {
    /** 初始化浮动菜单 */
    initFloatMenu() {
      this.$floatMenu.windowsInit(document.querySelector('.main-container'));
      let tools = [];
      this.$floatMenu.resetMenus(tools);
    },
    open(plan, planItem, caseId, params) {
      this.plan = plan;
      this.planItem = planItem;
      this.params = params;
      this.reset();
      if(caseId) {
        this.loading = true;
        getCase(caseId).then(response => {
          this.loading = false;
          this.visible = true;
          this.form = response.data;
          this.initFloatMenu();
        }).catch(()=>this.loading = true);
      } else {
        this.visible = true;
        this.initFloatMenu();
      }
    },
    /** 关闭缺陷抽屉窗口 */
    closeDefectDrawer(done) {
      // 如果是编辑，点击背景关闭
      // if(this.isAddMode==false){
      //   done();
      //   return;
      // }
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
      this.$nextTick(()=>{
        this.$refs.selectModule && this.$refs.selectModule.reset();
      })
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
    },
    prevCase() {
      this.loading = true;
      getPrevPlanItem(this.planItem.planItemId, this.params).then(res=>{
        this.loading = false;
        if(res.data) {
          this.form = res.data;
          this.planItem = res.data;
          this.visible = true;
        }
      }).catch(()=>this.loading = false);
    },
    nextCase() {
      this.loading = true;
      getNextPlanItem(this.planItem.planItemId, this.params).then(res=>{
        this.loading = false;
        if(res.data) {
          this.form = res.data;
          this.planItem = res.data;
          this.visible = true;
        }
      }).catch(()=>this.loading = false);
    },
    handlePlanItemChange() {
      this.$emit('change');
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
.case-image {
  display: inline-flex;
  flex-direction: row;
  gap: 10px;
  flex-wrap: wrap;
  > .el-image {
    width: 150px;
    height: 150px;
  }
}
.tools-row {
  display: inline-flex;
  flex-direction: row;
  gap: 10px;
}
</style>
