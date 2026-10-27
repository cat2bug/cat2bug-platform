<template>
  <div class="app-container project-list-page-host case-settings-page">
    <el-row class="project-add-page-header team-option-sub-hint-back">
      <el-page-header @back="goBack" :content="$t('case.review.template')" />
    </el-row>
    <cat2-bug-editor
      v-model="template"
      :editable="canEdit"
      :paper="true"
      :flat="true"
      :show-save="canEdit"
      :save-disabled="!dirty"
      :save-loading="saving"
      :min-height="360"
      class="case-settings-page__editor"
      @save="saveTemplate"
    />
  </div>
</template>

<script>
import { getCaseReviewTemplate, saveCaseReviewTemplate } from '@/api/system/case-type'
import { checkPermi } from '@/utils/permission'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'
import Cat2BugEditor from '@/components/Cat2BugEditor/index.vue'

export default {
  name: 'TeamCaseSettings',
  components: { Cat2BugEditor },
  setup() {
    const userStore = useUserStore()
    const { config } = storeToRefs(userStore)
    return { config }
  },
  data() {
    return {
      template: '',
      savedTemplate: '',
      saving: false,
      leaveConfirmed: false
    }
  },
  computed: {
    teamId() {
      return parseInt(this.config.currentTeamId)
    },
    canEdit() {
      return checkPermi(['system:team:case-type:edit'])
    },
    dirty() {
      return this.template !== this.savedTemplate
    }
  },
  created() {
    this.load()
  },
  mounted() {
    window.addEventListener('beforeunload', this.onBeforeUnload)
  },
  beforeUnmount() {
    window.removeEventListener('beforeunload', this.onBeforeUnload)
  },
  async beforeRouteLeave() {
    if (this.leaveConfirmed) return true
    const ok = await this.confirmLeave()
    if (ok) this.leaveConfirmed = true
    return ok
  },
  methods: {
    onBeforeUnload(e) {
      if (!this.dirty) return
      e.preventDefault()
      e.returnValue = ''
    },
    confirmLeave() {
      if (!this.dirty) {
        return Promise.resolve(true)
      }
      return this.$modal.confirm(
        this.$i18n.t('defect.unsaved-close-confirm').toString(),
        this.$i18n.t('prompted').toString(),
        {
          confirmButtonText: this.$i18n.t('ok').toString(),
          cancelButtonText: this.$i18n.t('cancel').toString(),
          type: 'warning'
        }
      ).then(() => true).catch(() => false)
    },
    async goBack() {
      if (!this.leaveConfirmed) {
        const ok = await this.confirmLeave()
        if (!ok) return
        this.leaveConfirmed = true
      }
      this.$router.push('/team-options/team-option')
    },
    load() {
      getCaseReviewTemplate(this.teamId).then(res => {
        this.template = (res.data && res.data.template) || ''
        this.savedTemplate = this.template
      }).catch(() => {})
    },
    saveTemplate() {
      if (!this.canEdit || this.saving || !this.dirty) return
      this.saving = true
      saveCaseReviewTemplate(this.teamId, this.template).then(() => {
        this.savedTemplate = this.template
        this.$modal.msgSuccess(this.$t('modify-success'))
      }).finally(() => {
        this.saving = false
      })
    }
  }
}
</script>

<style scoped>
.case-settings-page {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 120px);
}
.case-settings-page__editor {
  flex: 1;
  min-height: 0;
}
</style>
