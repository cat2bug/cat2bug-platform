<template>
  <div>
    <el-form class="add-case-prompt-form" ref="form" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="" prop="casePromptContent">
        <el-input
          type="textarea"
          :autosize="autoSize"
          :placeholder="$t('case.add-prompt-placeholder')"
          v-model="form.casePromptContent"></el-input>
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer add-case-prompt-dialog-footer">
      <el-button size="mini" @click="cancel">{{ $t('cancel') }}</el-button>
      <el-button size="mini" type="primary" @click="submit">{{ $t(isAdd?'add':'update') }}</el-button>
    </span>
  </div>
</template>

<script>
import {addCasePrompt, updateCasePrompt} from "@/api/system/CasePrompt";

export default {
  name: "AddCasePrompt",
  data() {
    return {
      form: this.casePrompt,
      rules: {
        casePromptContent:[
        { required: true, message: this.$i18n.t('case.prompt-content-cannot-empty'), trigger: "input" }
      ]}
    }
  },
  props: {
    autoSize: {
      type: Object,
      default: ()=>{
        return { minRows: 5, maxRows: 15}
      }
    },
    projectId: {
      type: Number,
      default: null
    },
    casePrompt: {
      type: Object,
      default: ()=>{
        return {
          casePromptContent: null
        }
      }
    },
    isAdd: {
      type: Boolean,
      default: true
    }
  },
  watch: {
    casePrompt: function (n) {
      this.form = n;
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
    openEdit(casePrompt) {
      this.form = casePrompt;
    },
    submit() {
      if(this.isAdd) {
        this.addPrompt();
      } else {
        this.editPrompt();
      }
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
    },
    editPrompt() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          updateCasePrompt(this.form).then(res => {
            this.$message.success(this.$i18n.t('update.success').toString());
            this.$emit('updated', this.form);
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
