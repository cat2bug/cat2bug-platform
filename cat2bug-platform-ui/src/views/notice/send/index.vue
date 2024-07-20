<template>
  <el-dialog
    :title="$t('notice.send')"
    :visible.sync="visible"
    width="40%"
    :before-close="handleClose">
    <el-form ref="form" :model="form" label-width="120px">
      <el-form-item :label="$t('notice.receiver')" prop="receiveIds">
        <select-project-member
          v-model="form.receiveIds"
          :project-id="getProjectId()"
          :is-head="false"
        />
      </el-form-item>
      <el-form-item :label="$t('notice.title')" prop="title">
        <el-input v-model="form.title" maxlength="255"></el-input>
      </el-form-item>
      <el-form-item :label="$t('notice.content')" prop="content">
        <el-input type="textarea"
                  :rows="8"
                  maxlength="65536"
                  show-word-limit
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
      form: {}
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
      this.form.projectId = this.getProjectId();
      this.form.src = `${window.location.protocol}//${window.location.host}`;
      this.form.groupName = 'artificial'; // 人工通知
      sendNotice(this.form).then(res=>{
        this.visible = false;
        this.$message.success(this.$i18n.t('notice.send-success').toString());
        this.$emit('send', this.form)
      })
    }
  }
}
</script>

<style scoped>

</style>
