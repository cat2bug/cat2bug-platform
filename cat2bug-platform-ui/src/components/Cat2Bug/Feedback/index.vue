<template>
  <div>
    <svg-icon icon-class="email" @click="openSuggestMail" />
  </div>
</template>

<script>
import pkg from '../../../../package.json'
import { useUserStore } from '@/stores/user'
import { getCurrentProjectId } from '@/utils/store-context'
import { buildFeedbackMailto, openFeedbackMailto } from '@/utils/feedback-mailto'

export default {
  name: 'Cat2BugFeedback',
  methods: {
    openSuggestMail() {
      const user = useUserStore()
      const locale = (this.$i18n && this.$i18n.locale) || ''
      const projectId = getCurrentProjectId()
      const mailto = buildFeedbackMailto({
        subject: String(this.$t('feedback.mail-subject')),
        bodyPrompt: String(this.$t('feedback.mail-body-prompt')),
        locale,
        nickName: user.nickName || user.name || '',
        projectId: projectId > 0 ? projectId : '',
        appVersion: (pkg && pkg.version) || '',
        userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : ''
      })
      openFeedbackMailto(mailto)
    }
  }
}
</script>
