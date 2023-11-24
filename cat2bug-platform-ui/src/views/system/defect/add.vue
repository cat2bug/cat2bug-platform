<template>
  <div class="app-container">
    <el-form ref="form" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="处理人" prop="handleBy">
        <select-project-member v-model="form.handleBy" :project-id="projectId" />
      </el-form-item>
      <el-form-item label="缺陷标题" prop="defectName">
        <el-input v-model="form.defectName" placeholder="请输入缺陷标题" />
      </el-form-item>
      <el-form-item label="缺陷描述">
        <editor v-model="form.defectDescribe" :min-height="192"/>
      </el-form-item>
      <el-form-item label="附件" prop="annexUrls">
        <file-upload v-model="form.annexUrls"/>
      </el-form-item>
      <el-form-item label="测试用例id" prop="caseId">
        <el-input v-model="form.caseId" placeholder="请输入测试用例id" />
      </el-form-item>
      <el-form-item label="数据来源" prop="dataSources">
        <el-input v-model="form.dataSources" placeholder="请输入数据来源" />
      </el-form-item>
      <el-form-item label="数据来源相关参数" prop="dataSourcesParams">
        <el-input v-model="form.dataSourcesParams" placeholder="请输入数据来源相关参数" />
      </el-form-item>
      <el-form-item label="测试模块id" prop="moduleId">
        <el-input v-model="form.moduleId" placeholder="请输入测试模块id" />
      </el-form-item>
      <el-form-item label="版本" prop="moduleVersion">
        <el-input v-model="form.moduleVersion" placeholder="请输入版本" />
      </el-form-item>
      <el-form-item label="处理时间" prop="handleTime">
        <el-date-picker clearable
                        v-model="form.handleTime"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择处理时间">
        </el-date-picker>
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="cancel">取 消</el-button>
    </div>
  </div>
</template>

<script>
import {addDefect, updateDefect} from "@/api/system/defect";
import SelectProjectMember from "@/components/SelectProjectMember"

export default {
  name: "AddDefect",
  components: { SelectProjectMember },
  data() {
    return {
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        defectType: [
          { required: true, message: "缺陷类型不能为空", trigger: "change" }
        ],
        defectName: [
          { required: true, message: "缺陷标题不能为空", trigger: "blur" }
        ],
        defectDescribe: [
          { required: true, message: "缺陷描述不能为空", trigger: "blur" }
        ],
        projectId: [
          { required: true, message: "项目id不能为空", trigger: "change" }
        ],
        defectState: [
          { required: true, message: "缺陷状态不能为空", trigger: "change" }
        ],
      }
    }
  },
  props: {
    projectId: {
      type: Number,
      default: null
    }
  },
  methods:{
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        defectId: null,
        defectType: null,
        defectName: null,
        defectDescribe: null,
        annexUrls: null,
        projectId: null,
        testPlanId: null,
        caseId: null,
        dataSources: null,
        dataSourcesParams: null,
        moduleId: null,
        moduleVersion: null,
        createBy: null,
        updateTime: null,
        createTime: null,
        updateBy: null,
        defectState: null,
        caseStepId: null,
        handleBy: null,
        handleTime: null,
        defectLevel: null
      };
      this.resetForm("form");
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.defectId != null) {
            updateDefect(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addDefect(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
  }
}
</script>

<style scoped>

</style>
