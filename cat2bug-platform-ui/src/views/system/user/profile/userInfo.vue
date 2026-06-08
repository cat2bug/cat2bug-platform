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
      <el-button class="defect-kbd-hint-host" type="primary" size="mini" @click="submit">
        {{ $t('save') }}
        <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
      </el-button>
      <el-button type="danger" size="mini" @click="close">{{ $t('close') }}</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { updateUserProfile } from "@/api/system/user";
import { optionalPhoneRule, optionalEmailRule, normalizeContactFields } from "@/utils/user-contact-rules";
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import formFieldHints from '@/mixins/form-field-hints'
import pageFormClose from '@/mixins/page-form-close'

export default {
  mixins: [dialogFormShortcuts, formFieldHints, pageFormClose],
  props: {
    user: {
      type: Object
    },
    formActive: {
      type: Boolean,
      default: true
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
  computed: {
    visible() {
      return this.formActive
    }
  },
  watch: {
    formActive(val) {
      if (val) {
        this.$nextTick(() => {
          this.capturePageFormCloseBaseline()
          this.$_syncFieldHintListeners(true)
        })
      } else {
        this.$_syncFieldHintListeners(false)
      }
    },
    user: {
      deep: true,
      handler() {
        if (this.formActive && !this.pageFormCloseBaseline) {
          this.$nextTick(() => this.capturePageFormCloseBaseline())
        }
      }
    }
  },
  mounted() {
    if (this.formActive) {
      this.$nextTick(() => {
        this.capturePageFormCloseBaseline()
        this.$_syncFieldHintListeners(true)
      })
    }
  },
  methods: {
    serializePageFormCloseState() {
      return JSON.stringify({
        nickName: this.user.nickName,
        phoneNumber: this.user.phoneNumber,
        email: this.user.email
      })
    },
    onPageFormShortcutClose() {
      this.requestClosePageForm({ onClose: () => this.$tab.closePage() })
    },
    shortcutSave() {
      this.submit()
    },
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
      this.onPageFormShortcutClose()
    }
  }
};
</script>
