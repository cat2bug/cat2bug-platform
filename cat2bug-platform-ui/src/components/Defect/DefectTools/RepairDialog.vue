<template>
  <el-dialog :title="$i18n.t('repair')" :visible.sync="dialogVisible" :close-on-press-escape="false" :before-close="onToolDialogBeforeClose" append-to-body @opened="onToolDialogOpened" width="30%">
    <el-form ref="form" :model="form" :rules="rules" label-width="120px">
      <el-form-item :label="$i18n.t('defect.reviewed-by')" prop="receiveBy">
        <select-project-member ref="selectProjectMember" v-model="form.receiveBy" :project-id="projectId" :is-head="false" :multiple="false" />
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
import SelectProjectMember from "@/components/Project/SelectProjectMember";
import { repair } from "@/api/system/defect";
import {listLog} from "@/api/system/log";
import defectToolDialogKbd from '@/mixins/defect-tool-dialog-kbd'
export default {
  name: "RepairDialog",
  mixins: [defectToolDialogKbd],
  components: { SelectProjectMember },
  data() {
    return {
      dialogVisible: false,
      deferToolDialogCloseBaseline: true,
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
      // 查询缺陷日志找到最新的一条，默认将上一条日志创建人设置为待处理的人
      listLog({
        pageNum: 1,
        pageSize: 1,
        defectId: this.defectId,
        orderByColumn: 'createTime',
        isAsc: 'desc',
      }).then(res=>{
        res.rows.forEach(l=>{
          this.$set(this.form, 'receiveBy',[parseInt(l.createBy)])
        });
      }).finally(() => {
        this.$nextTick(() => this.captureToolDialogCloseBaseline());
      });
      this.dialogVisible = true;
    },
    onSubmit() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.defectId = this.defectId;
          repair(this.defectId, this.form).then(res => {
            this.$modal.msgSuccess(this.$i18n.t('defect.repair-success'));
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
