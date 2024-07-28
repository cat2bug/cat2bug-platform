<template>
  <el-form ref="form" :model="user" :rules="rules" label-width="220px">
    <el-form-item :label="$t('name')" prop="nickName">
      <el-input v-model="user.nickName" maxlength="30" />
    </el-form-item>
    <el-form-item :label="$t('phone-number')" prop="phoneNumber">
      <el-input v-model="user.phoneNumber" maxlength="11" />
    </el-form-item>
    <el-form-item :label="$t('email')" prop="email">
      <el-input v-model="user.email" maxlength="50" />
    </el-form-item>
    <el-form-item :label="$t('ding')+' User ID'" prop="email">
      <el-input v-model="user.dingUserId" maxlength="32" />
    </el-form-item>
    <el-form-item :label="$t('enterprise-wechat')+' User ID'" prop="email">
      <el-input v-model="user.wechatUserId" maxlength="32" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" size="mini" @click="submit">{{ $t('save') }}</el-button>
      <el-button type="danger" size="mini" @click="close">{{ $t('close') }}</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { updateUserProfile } from "@/api/system/user";

export default {
  props: {
    user: {
      type: Object
    }
  },
  data() {
    return {
      // 表单校验
      rules: {
        nickName: [
          { required: true, message: this.$i18n.t('member.name-cannot-empty'), trigger: "blur" }
        ],
        email: [
          { required: true, message: this.$i18n.t('member.email-cannot-empty'), trigger: "blur" },
          {
            type: "email",
            message: this.$i18n.t('member.email-format-exception'),
            trigger: ["blur", "change"]
          }
        ],
        phoneNumber: [
          { required: true, message: this.$i18n.t('member.phone-number-cannot-empty'), trigger: "blur" },
          {
            pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
            message: this.$i18n.t('member.phone-number-format-exception'),
            trigger: "blur"
          }
        ]
      }
    };
  },
  methods: {
    submit() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          updateUserProfile(this.user).then(response => {
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
