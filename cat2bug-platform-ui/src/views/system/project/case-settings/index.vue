<template>
  <div class="app-container project-list-page-host">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('case.review.template')" />
    </el-row>
    <cat2-bug-editor v-model="template" :editable="canEdit" :min-height="360" />
    <div v-if="canEdit" class="case-settings-tools">
      <el-button type="primary" size="small" @click="saveTemplate">{{ $t('save') }}</el-button>
    </div>
  </div>
</template>

<script>
import { getCaseReviewTemplate, saveCaseReviewTemplate } from '@/api/system/case-type'
import { checkPermi } from '@/utils/permission'
import { useUserStore } from '@/stores/user'
import Cat2BugEditor from '@/components/Cat2BugEditor/index.vue'

export default {
  name: 'ProjectCaseSettings',
  components: { Cat2BugEditor },
  data() {
    return {
      template: ''
    }
  },
  computed: {
    projectId() {
      return parseInt(useUserStore().config.currentProjectId)
    },
    canEdit() {
      return checkPermi(['system:project:case-type:edit'])
    }
  },
  created() {
    this.load()
  },
  methods: {
    goBack() {
      this.$router.push('/project/option')
    },
    load() {
      getCaseReviewTemplate(this.projectId).then(res => {
        this.template = (res.data && res.data.template) || ''
      }).catch(() => {})
    },
    saveTemplate() {
      saveCaseReviewTemplate(this.projectId, this.template).then(() => {
        this.$modal.msgSuccess(this.$t('modify-success'))
      })
    }
  }
}
</script>
