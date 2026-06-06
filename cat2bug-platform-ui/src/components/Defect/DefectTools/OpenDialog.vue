<template>
  <el-dialog :title="$i18n.t('open')" :visible.sync="dialogVisible" :close-on-press-escape="false" :before-close="onToolDialogBeforeClose" append-to-body @opened="onToolDialogOpened" width="30%">
    <el-form ref="form" :model="form" :rules="rules" label-width="120px">
      <el-form-item :label="$i18n.t('describe')" prop="defectLogDescribe">
        <el-input type="textarea"
                  rows="5"
                  v-model="form.defectLogDescribe"
                  maxlength="255"
                  :placeholder="$i18n.t('defect.enter-describe')"
                  show-word-limit
        ></el-input>
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="requestCloseToolDialog">{{$i18n.t('cancel')}}</el-button>
      <el-button class="defect-kbd-hint-host" type="primary" @click="onSubmit">
        {{$i18n.t('submit')}}
        <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import {open} from "@/api/system/defect";
import defectToolDialogKbd from '@/mixins/defect-tool-dialog-kbd'
export default {
  name: "OpenDialog",
  mixins: [defectToolDialogKbd],
  data() {
    return {
      dialogVisible: false,
      rules: {
        defectLogDescribe: [
          {required: true, message: this.$t('defect.describe-cannot-empty'), trigger: "change"},
        ],
      },
      form: {
        remark: null
      }
    }
  },
  props: {
    projectId: {
      type: Number,
      default: null
    },
    defectId: {
      type: Number,
      default: null
    }
  },
  methods:{
    reset() {
      this.form = {
        remark: null
      }
      this.resetForm("form");
    },
    open() {
      this.reset();
      this.dialogVisible = true;
    },
    onSubmit() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.defectId = this.defectId;
          open(this.defectId, this.form).then(res => {
            this.$modal.msgSuccess(this.$i18n.t('defect.start-success'));
            this.doCloseToolDialog();
            this.$emit('log', res.data);
          });
        }
      });

    }
  }
}
</script>

<style scoped>

</style>
