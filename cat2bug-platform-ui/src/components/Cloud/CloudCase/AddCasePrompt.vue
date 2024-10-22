<template>
  <div>
    <el-form class="add-case-prompt-form" ref="form" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="" prop="casePromptContent">
        <el-input
          type="textarea"
          :autosize="{ minRows: 10, maxRows: 15}"
          :placeholder="$t('case.add-prompt-placeholder')"
          v-model="form.casePromptContent"></el-input>
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer add-case-prompt-dialog-footer">
      <el-button size="mini" @click="cancel">{{ $t('cancel') }}</el-button>
      <el-button size="mini" type="primary" @click="addPrompt">{{ $t('add') }}</el-button>
    </span>
  </div>
</template>

<script>
import {addCasePrompt} from "@/api/system/CasePrompt";

export default {
  name: "AddCasePrompt",
  data() {
    return {
      form: {},
      rules: {
        casePromptContent:[
        { required: true, message: this.$i18n.t('case.prompt-content-cannot-empty'), trigger: "input" }
      ]}
    }
  },
  props: {
    projectId: {
      type: Number,
      default: null
    }
  },
  methods: {
    cancel() {
      this.$emit('cancel');
    },
    reset() {
      this.form = {}
      this.resetForm("form");
    },
    addPrompt() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.projectId = this.projectId;
          addCasePrompt(this.form).then(res => {
            this.$message.success(this.$i18n.t('save.success').toString());
            this.$emit('added', this.form);
          });
        }
      });
    }
  }
}
</script>
<style>
.add-case-prompt-form > .el-form-item > .el-form-item__content {
  margin-left: 0px !important;
}
</style>
<style scoped>
.add-case-prompt-dialog-footer {
  display: inline-flex;
  flex-direction: row;
  justify-content: flex-end;
  align-items: center;
  width: 100%;
}
</style>
