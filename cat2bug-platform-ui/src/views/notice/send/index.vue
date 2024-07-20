<template>
  <el-dialog
    :title="$t('notice.send')"
    :visible.sync="visible"
    width="40%"
    :before-close="handleClose">
    <el-form ref="form" :model="form" :rules="rules" label-width="130px">
      <el-form-item :label="$t('notice.receiver')" prop="receiveIds">
        <select-project-member
          :placeholder="$t('notice.please-select-receiver').toString()"
          v-model="form.receiveIds"
          :project-id="getProjectId()"
          :is-head="false"
        />
      </el-form-item>
      <el-form-item :label="$t('notice.title')" prop="title">
        <el-input v-model="form.title" maxlength="255" :placeholder="$t('notice.please-enter-title')"></el-input>
      </el-form-item>
      <el-form-item :label="$t('notice.content')" prop="content">
        <el-input type="textarea"
                  :rows="8"
                  maxlength="65536"
                  show-word-limit
                  :placeholder="$t('notice.please-enter-content')"
                  v-model="form.content"></el-input>
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
    <el-button @click="visible = false">{{ $t('cancel') }}</el-button>
    <el-button type="primary" @click="handleSend">{{ $t('ok') }}</el-button>
  </span>
  </el-dialog>
</template>

<script>
import SelectProjectMember from "@/components/Project/SelectProjectMember";
import {sendNotice} from "@/api/system/notice";
export default {
  name: "SendNoticeDialog",
  components: { SelectProjectMember },
  data() {
    return {
      visible: false,
      form: {},
      rules: {
        receiveIds: [
          { required: true, message: this.$i18n.t('notice.receiver-not-empty'), trigger: "input" }
        ],
        title: [
          { required: true, message: this.$i18n.t('notice.title-not-empty'), trigger: 'input' },
        ],
        content: [
          { required: true, message: this.$i18n.t('notice.content-not-empty'), trigger: 'input' },
        ]
      }
    }
  },
  methods: {
    /** 打开窗口 */
    open() {
      this.visible = true;
      if(this.$refs.form) {
        this.$refs.form.resetFields();
      }
    },
    /** 关闭窗口的处理 */
    handleClose(done) {
      done();
    },
    /** 获取项目ID */
    getProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 发送通知 */
    handleSend() {
      this.$refs['form'].validate((valid) => {
        if (valid) {
          this.form.projectId = this.getProjectId();
          this.form.src = `${window.location.protocol}//${window.location.host}`;
          this.form.groupName = 'artificial'; // 人工通知
          sendNotice(this.form).then(res=>{
            this.visible = false;
            this.$message.success(this.$i18n.t('notice.send-success').toString());
            this.$emit('send', this.form)
          })
        }
      });
    }
  }
}
</script>

<style scoped>

</style>
