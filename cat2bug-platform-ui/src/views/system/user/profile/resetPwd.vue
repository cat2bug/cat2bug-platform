<template>
  <el-form ref="form" :model="user" :rules="rules" label-width="150px">
    <el-form-item :label="$t('old-password')" prop="oldPassword">
      <el-input v-model="user.oldPassword" :placeholder="$t('member.please-enter-old-password')" type="password" show-password maxlength="20" minlength="6" />
    </el-form-item>
    <el-form-item :label="$t('new-password')" prop="newPassword">
      <el-input v-model="user.newPassword" :placeholder="$t('member.please-enter-new-password')" type="password" show-password maxlength="20" minlength="6" />
    </el-form-item>
    <el-form-item :label="$t('confirm-password')" prop="confirmPassword">
      <el-input v-model="user.confirmPassword" :placeholder="$t('member.please-enter-new-password')" type="password" show-password maxlength="20" minlength="6" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" size="mini" @click="submit">{{ $t('save') }}</el-button>
      <el-button type="danger" size="mini" @click="close">{{ $t('close') }}</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { updateUserPwd } from "@/api/system/user";

export default {
  data() {
    const equalToPassword = (rule, value, callback) => {
      if (this.user.newPassword !== value) {
        callback(new Error(this.$i18n.t('entered-passwords-differ').toString()));
      } else {
        callback();
      }
    };
    return {
      user: {
        oldPassword: undefined,
        newPassword: undefined,
        confirmPassword: undefined
      },
      // 表单校验
      rules: {
        oldPassword: [
          { required: true, message: this.$i18n.t('member.old-password-cannot-empty'), trigger: "blur" }
        ],
        newPassword: [
          { required: true, message: this.$i18n.t('member.new-password-cannot-empty'), trigger: "blur" },
          { min: 6, max: 20, message: this.$i18n.t('member.input-size-exception'), trigger: "blur" }
        ],
        confirmPassword: [
          { required: true, message: this.$i18n.t('member.confirm-password-cannot-empty'), trigger: "blur" },
          { required: true, validator: equalToPassword, trigger: "blur" }
        ]
      }
    };
  },
  methods: {
    submit() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          updateUserPwd(this.user.oldPassword, this.user.newPassword).then(response => {
            this.$modal.msgSuccess(this.$i18n.t('modify-success'));
          });
        }
      });
    },
    close() {
      this.$tab.closePage();
    }
  }
};
</script>
