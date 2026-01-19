<template>
  <el-dialog :title="(form.lock?$t('lock'):$t('unlock')) + $t('team')" :visible.sync="visible" width="600px" append-to-body>
    <el-form ref="form" :model="form" :rules="rules" label-width="140px">
      <el-form-item :label="$t('team.name')">
        <span>{{form.teamName}}</span>
      </el-form-item>
      <el-form-item :label="form.lock?$t('team.unlock-remark'):$t('team.lock-remark')" prop="lockRemark" required>
        <el-input
          v-model="form.lockRemark"
          type="textarea"
          :rows="7"
          :placeholder="form.lock?$t('team.please-enter-unlock-remark'):$t('team.please-enter-lock-remark')"
          maxlength="255" />
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="visible=false">{{ $t('cancel') }}</el-button>
      <el-button type="primary" @click="handleLock">{{ form.lock?$t('lock'):$t('unlock') }}</el-button>
    </div>
  </el-dialog>
</template>

<script>
import {lockTeam} from "@/api/system/team";

export default {
  name: "TeamLockDialog",
  data() {
    return {
      visible: false,
      form: {},
      rules: {
        lockRemark: [
          { required: true, message: this.$i18n.t('case.name-cannot-empty'), trigger: "input" }
        ],
      }
    }
  },
  computed: {
    lockRemarkLabel: function () {
      return this.form.lock?this.$i18n.t('unlock'):this.$i18n.t('lock')+this.$i18n.t('remark');
    }
  },
  methods: {
    open(team) {
      this.reset();
      this.form = {
        ...team,
        ...{
          lockRemark: null,
          lock: !team.lock
        }
      };
      this.visible = true;
    },
    // 表单重置
    reset() {
      this.form = {
        teamId: null,
        teamName: null,
        teamIcon: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        introduce: null,
        isDel: null
      };
      this.resetForm("form");
    },
    /** 处理禁用或解锁 */
    handleLock() {
      this.$refs.form.validate(valid => {
        if (valid) {
          lockTeam(this.form.teamId, this.form).then(res => {
            this.$message.success(this.$i18n.t('modify-success').toString())
            this.visible = false;
            this.$emit('change', this.form);
          });
        }
      });
    }
  }
}
</script>

<style scoped>

</style>
