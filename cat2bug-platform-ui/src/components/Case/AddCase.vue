<template>
  <el-drawer
    size="50%"
    :visible.sync="visible"
    direction="rtl"
    :before-close="closeDefectDrawer">
    <template slot="title">
      <div class="case-add-header">
        <h3>{{title}}</h3>
        <div>
          <el-button @click="cancel" icon="el-icon-close" size="mini">{{$t('close')}}</el-button>
          <el-button v-if="isAddMode" type="primary" icon="el-icon-finished" @click="submitForm" size="mini">{{$t('create')}}</el-button>
          <el-button v-else type="success" icon="el-icon-finished" @click="submitForm" size="mini">{{$t('modify')}}</el-button>
        </div>
      </div>
    </template>
    <div class="app-container">
      <el-checkbox v-if="isAddMode" class="create-next-case" v-model="isCreateNextCase">{{ $t('case.create-next-case') }}</el-checkbox>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item :label="$t('title')" prop="caseName">
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
        <el-form-item :label="$t('step')" prop="caseStep">
          <case-step-panel v-model="form.caseStep" />
        </el-form-item>
        <el-form-item :label="$t('remark')" prop="remark">
          <el-input type="textarea" v-model="form.remark" :placeholder="$t('please-enter-remark')" maxlength="255" rows="6" show-word-limit />
        </el-form-item>
      </el-form>
    </div>
  </el-drawer>
</template>

<script>
import {addCase, getCase, updateCase} from "@/api/system/case";
import CaseStepPanel from "./CaseStepPanel"
import SelectModule from "@/components/SelectModule"
import Cat2BugSelectLevel from "@/components/Cat2BugSelectLevel";
import Label from "@/components/Cat2BugStatistic/Components/Label";

export default {
  name: "AddCase",
  components: {Label,CaseStepPanel,SelectModule,Cat2BugSelectLevel},
  data() {
    return {
      // 是否显示创建组件
      visible: false,
      // 是否连续创建用例
      isCreateNextCase: true,
      // 表单参数
      form: {
        caseStep:[],
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
    open(caseId) {
      this.reset();
      if(caseId) {
        getCase(caseId).then(response => {
          this.form = response.data;
          this.visible = true;
          // this.title = "修改测试用例";
        });
      } else {
        this.visible = true;
      }
      // this.title = "添加测试用例";
    },
    /** 关闭缺陷抽屉窗口 */
    closeDefectDrawer(done) {
      // 如果是编辑，点击背景关闭
      if(this.isAddMode==false){
        done();
      }
    },
    // 取消按钮
    cancel() {
      this.visible = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        caseId: null,
        caseName: null,
        moduleId: null,
        caseType: null,
        caseExpect: null,
        caseStep: [],
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
    },

    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.isAddMode) {
            addCase(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('create-success'));
              if(this.isCreateNextCase==false) {
                this.visible = false;
              }
              this.reset();
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
  }
}
</script>

<style lang="scss" scoped>
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
}
.create-next-case {
  margin: 0px 10px 20px 40px;
}
</style>
