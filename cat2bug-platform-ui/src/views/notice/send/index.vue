<template>
  <el-dialog
    :title="$t('notice.send')"
    :visible.sync="visible"
    width="40%"
    custom-class="cat2bug-form-shortcut-dialog send-notice-dialog"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :before-close="onSendDialogBeforeClose"
    @opened="onSendDialogOpened">
    <el-form ref="form" :model="form" :rules="rules" label-width="130px">
      <el-form-item :label="$t('notice.receiver')" prop="receiveIds">
        <select-project-member
          :placeholder="$t('notice.please-select-receiver')"
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
    <el-button @click="requestCloseSendDialog">{{ $t('cancel') }}</el-button>
    <el-button class="defect-kbd-hint-host" type="primary" @click="handleSend">
      {{ $t('ok') }}
      <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
    </el-button>
  </span>
  </el-dialog>
</template>

<script>
import SelectProjectMember from "@/components/Project/SelectProjectMember";
import {sendNotice} from "@/api/system/notice";
import sendNoticeDialogKbd from '@/mixins/send-notice-dialog-kbd'

export default {
  name: "SendNoticeDialog",
  mixins: [sendNoticeDialogKbd],
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
    onSendDialogBeforeClose(done) {
      this.requestCloseSendDialog({ done })
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
