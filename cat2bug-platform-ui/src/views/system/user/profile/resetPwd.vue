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
      <el-button class="defect-kbd-hint-host" type="primary" size="mini" @click="submit">
        {{ $t('save') }}
        <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
      </el-button>
      <el-button type="danger" size="mini" @click="close">{{ $t('close') }}</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { updateUserPwd } from "@/api/system/user";
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import formFieldHints from '@/mixins/form-field-hints'
import pageFormClose from '@/mixins/page-form-close'

export default {
  mixins: [dialogFormShortcuts, formFieldHints, pageFormClose],
  props: {
    formActive: {
      type: Boolean,
      default: true
    }
  },
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
        oldPassword: this.user.oldPassword,
        newPassword: this.user.newPassword,
        confirmPassword: this.user.confirmPassword
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
          updateUserPwd(this.user.oldPassword, this.user.newPassword).then(response => {
            this.$modal.msgSuccess(this.$i18n.t('modify-success'));
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
