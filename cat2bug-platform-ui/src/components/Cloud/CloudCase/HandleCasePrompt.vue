<template>
  <el-dialog
    :title="$t('case.edit-input-prompt')"
    :visible.sync="dialogVisible"
    :append-to-body="true"
    width="70%"
    :before-close="close">
    <el-form ref="form" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="" prop="casePromptContent">
        <el-input
          type="textarea"
          :autosize="{ minRows: 10, maxRows: 15}"
          :placeholder="$t('case.add-prompt-placeholder')"
          v-model="form.casePromptContent"></el-input>
      </el-form-item>
      <el-form-item label="" v-for="(v, index) in form.variableList" :key="index"
                    :prop="`variableList.${index}.replaceContent`"
                    :rules="{ required: true, message: $t('case.prompt-content-cannot-empty'), trigger: 'input' }">
        <div class="variable-set-row">
          <div class="variable-number">#{{ index+1 }}</div>
          <label class="variable-name"><el-tag>{{ v.variableName }}</el-tag></label>
          <el-input type="textarea" :autosize="{minRows:2, maxRows: 5}" v-model="v.replaceContent" :placeholder="strFormat($t('case.please-enter-replace-text'),v.variableName)"></el-input>
        </div>
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
    <el-button @click="close">{{ $t('cancel') }}</el-button>
    <el-button type="primary" @click="submit">{{ $t('submit') }}</el-button>
  </span>
  </el-dialog>
</template>

<script>
import Label from "@/components/Cat2BugStatistic/Components/Label";
import {strFormat} from "@/utils";
const PATTERN = /\$\{\s*[0-9a-zA-z]{1,255}\s*\}/g;
export default {
  name: "HandleCasePrompt",
  components: {Label},
  data() {
    return {
      dialogVisible: false,
      form: {
        variableList: [],
      },
      rules: {
        casePromptContent:[
          { required: true, message: this.$i18n.t('case.prompt-content-cannot-empty'), trigger: "input" }
        ],
        variableReplaceText:[
          { required: true, message: this.$i18n.t('case.prompt-content-cannot-empty'), trigger: "input" }
        ]
      }
    }
  },
  methods: {
    strFormat,
    open(prompt) {
      this.dialogVisible = true;
      this.reset();
      this.form = {...{variableList:[]},...prompt}
      const matches = prompt.casePromptContent.match(PATTERN);
      if(matches && matches.length>0) {
        this.form.variableList = matches.map(m=>{
          return {
            variableName: m,
            replaceContent: null,
          }
        })
      }
    },
    close() {
      this.dialogVisible = false;
    },
    /** 检测是否通过 */
    check(prompt) {
      const matches = prompt.casePromptContent.match(PATTERN);
      return !matches || matches.length==0;
    },
    reset() {
      this.form = {
        casePromptContent: null,
        variableReplaceText: null,
        variableList: [],
      };
      this.resetForm("form");
    },
    submit() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          let content=this.form.casePromptContent;
          this.form.variableList.forEach(t=>{
            if(t.replaceContent) {
              content = content.replace(t.variableName, t.replaceContent);
            }
          })
          this.close();
          this.$emit('submit', content);
        }
      });
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-form-item__content {
  margin-left: 0px !important;
  width: 100%;
}
.variable-set-row {
  display: inline-flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  width: 100%;
  gap: 10px;
  > .el-input {
    flex: 1;
  }
  ::v-deep +.el-form-item__error{
    margin-left: 167px;
  }
  .variable-name {
    width: 120px;
  }
  .variable-number {
    width: 50px;
    text-align: center;
  }
}
</style>
