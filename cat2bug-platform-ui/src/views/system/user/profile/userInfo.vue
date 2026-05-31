<template>
  <el-form ref="form" :model="user" :rules="rules" label-width="220px">
    <el-form-item :label="$t('name')" prop="nickName">
      <el-input v-model="user.nickName" maxlength="30" />
    </el-form-item>
    <el-form-item :label="$t('phone-number')" prop="phoneNumber">
      <el-input v-model="user.phoneNumber" maxlength="11" :placeholder="$t('member.phone-number-optional-placeholder')" />
    </el-form-item>
    <el-form-item :label="$t('email')" prop="email">
      <el-input v-model="user.email" maxlength="50" :placeholder="$t('member.email-optional-placeholder')" />
    </el-form-item>
<!--    <el-form-item :label="$t('ding')+' User ID'" prop="email">-->
<!--      <el-input v-model="user.dingUserId" maxlength="32" />-->
<!--    </el-form-item>-->
<!--    <el-form-item :label="$t('enterprise-wechat')+' User ID'" prop="email">-->
<!--      <el-input v-model="user.wechatUserId" maxlength="32" />-->
<!--    </el-form-item>-->
    <el-form-item>
      <el-button type="primary" size="mini" @click="submit">{{ $t('save') }}</el-button>
      <el-button type="danger" size="mini" @click="close">{{ $t('close') }}</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { updateUserProfile } from "@/api/system/user";
import { optionalPhoneRule, optionalEmailRule, normalizeContactFields } from "@/utils/user-contact-rules";

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
          optionalEmailRule(key => this.$i18n.t(key))
        ],
        phoneNumber: [
          optionalPhoneRule(key => this.$i18n.t(key))
        ]
      }
    };
  },
  methods: {
    submit() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          const payload = { ...this.user };
          normalizeContactFields(payload);
          updateUserProfile(payload).then(response => {
            this.$modal.msgSuccess(this.$i18n.t('modify-success'));
            Object.assign(this.user, payload);
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
