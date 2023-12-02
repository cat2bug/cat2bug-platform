<template>
  <el-dialog :title="$i18n.t('open')" :visible.sync="dialogVisible" append-to-body width="30%">
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
      <el-form-item>
        <el-button type="primary" @click="onSubmit">{{$i18n.t('submit')}}</el-button>
        <el-button @click="close">{{$i18n.t('cancel')}}</el-button>
      </el-form-item>
    </el-form>
  </el-dialog>
</template>

<script>
import SelectProjectMember from "@/components/SelectProjectMember";
import {reject} from "@/api/system/defect";
export default {
  name: "OpenDialog",
  components: { SelectProjectMember },
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
    close() {
      this.dialogVisible = false;
    },
    onSubmit() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.defectId = this.defectId;
          open(this.defectId, this.form).then(res => {
            this.$modal.msgSuccess(this.$i18n.t('defect.reject-success'));
            this.close();
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
