<template>
  <el-dialog :title="$i18n.t('repair')" :visible.sync="dialogVisible" append-to-body @close="close" width="30%">
    <el-form ref="form" :model="form" :rules="rules" label-width="120px">
      <el-form-item :label="$i18n.t('defect.reviewed-by')" prop="receiveBy">
        <select-project-member ref="selectProjectMember" v-model="form.receiveBy" :project-id="projectId" :role-id="8" :is-head="false" :multiple="false" />
      </el-form-item>
      <el-form-item :label="$i18n.t('describe')">
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
import SelectProjectMember from "@/components/Project/SelectProjectMember";
import { repair } from "@/api/system/defect";
export default {
  name: "RepairDialog",
  components: { SelectProjectMember },
  data() {
    return {
      dialogVisible: false,
      rules: {
        receiveBy: [
          {required: true, message: this.$t('defect.handle-by-cannot-empty'), trigger: "change"},
        ],
      },
      form: {
        receiveBy: [],
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
        receiveBy: [],
        remark: null
      }
      this.resetForm("form");
      this.$refs.selectProjectMember.clear();
    },
    open() {
      this.dialogVisible = true;
    },
    close() {
      this.dialogVisible = false;
      this.reset();
    },
    onSubmit() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.defectId = this.defectId;
          repair(this.defectId, this.form).then(res => {
            this.$modal.msgSuccess(this.$i18n.t('defect.repair-success'));
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
