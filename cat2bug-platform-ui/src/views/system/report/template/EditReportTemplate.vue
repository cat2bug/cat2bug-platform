<template>
  <div class="app-container">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('defect.statistic-template')">
        <template v-slot:content>
          <el-input type="text" v-model="template.templateTitle" class="report-name" maxlength="255" @input="changeHandle(500)"></el-input>
          <div class="time" v-if="template.updateTime">
            <svg-icon icon-class="md-create-time" />
            <span>{{$t('update-time')}}:{{template.updateTime}}</span>
          </div>
        </template>
      </el-page-header>
    </el-row>
    <report-template-editor
      ref="templateEditor"
      v-model:content="template.templateContent"
      :placeholder="$t('mk.start-edit').toString()"
      :template="template"
      @input="changeHandle(5000)"
      @save="onToolbarSave"
    />
  </div>
</template>

<script>
import { getReportTemplate, updateTemplate } from '@/api/system/ReportTemplate'
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import pageFormClose from '@/mixins/page-form-close'
import { defineAsyncComponent } from 'vue'
import { useUserStore } from '@/stores/user'

const ReportTemplateEditor = defineAsyncComponent(() =>
  import('@/components/ReportTemplateEditor').then((m) => m.ReportTemplateEditor)
)

export default {
  name: 'AddDefectReport',
  mixins: [dialogFormShortcuts, pageFormClose],
  components: { ReportTemplateEditor },
  data() {
    return {
      visible: true,
      template: {
        templateTitle: '',
        moduleType: null,
        projectId: null,
        templateContent: '',
        majorVersion: 1,
        minorVersion: 0
      },
      pushId: null,
      content: '',
      html: ''
    }
  },
  created() {
    const id = this.$route.query.templateId
    if (id) {
      this.getReportTemplate(id)
    } else {
      const store = useUserStore()
      const currentProjectId = store.config?.currentProjectId
      if (currentProjectId) {
        this.template.projectId = currentProjectId
      }
      this.$nextTick(() => this.capturePageFormCloseBaseline())
    }
  },
  mounted() {
    this.$nextTick(() => this.focusMarkdownEditor())
  },
  methods: {
    focusMarkdownEditor() {
      this.$refs.templateEditor?.focusEditor?.()
    },
    serializePageFormCloseState() {
      return JSON.stringify({
        templateTitle: this.template.templateTitle || '',
        templateContent: this.template.templateContent || '',
        templateIconUrl: this.template.templateIconUrl || ''
      })
    },
    onPageFormShortcutClose() {
      this.closeReportTemplatePage()
    },
    async closeReportTemplatePage() {
      if (this._closingTemplatePage) return
      this._closingTemplatePage = true
      try {
        if (this.pushId) {
          clearTimeout(this.pushId)
          this.pushId = null
        }
        if (this.isPageFormCloseDirty()) {
          await this.saveTemplateNow()
        }
        this.pageFormCloseBaseline = null
        this.$router.back()
      } catch (e) {
        this._closingTemplatePage = false
      }
    },
    async saveTemplateNow(notifySuccess = false) {
      const res = await updateTemplate(this.template)
      this.template = res.data
      await this.$nextTick()
      this.capturePageFormCloseBaseline()
      if (notifySuccess) {
        this.$message.success(this.$t('save.success').toString())
      }
      return res
    },
    onToolbarSave() {
      this.saveTemplateNow(true).catch(() => {})
    },
    getReportTemplate(id) {
      getReportTemplate(id).then((res) => {
        this.template = res.data
        this.$nextTick(() => {
          this.capturePageFormCloseBaseline()
          this.focusMarkdownEditor()
        })
      })
    },
    goBack() {
      this.onPageFormShortcutClose()
    },
    change(value, render) {
      this.html = render
    },
    changeHandle(time) {
      if (!this.pushId) {
        const delay = time ? time : 1000
        this.pushId = setTimeout(() => {
          this.pushId = null
          this.saveTemplateNow().catch(() => {})
        }, delay)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-page-header {
  display: inline-flex;
  flex-direction: row;
  width: 100%;
  .el-page-header__content {
    flex: 1;
    display: inline-flex;
    flex-direction: row;
    .el-input {
      flex: 1;
    }
  }
}
.app-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  position: absolute;
  top: 0px;
  bottom: 0px;

  > .project-add-page-header + .report-template-editor {
    margin-top: 0;
    flex: 1;
    min-height: 0;
  }
}
:deep(.report-name.el-input) {
  .el-input__wrapper {
    border: none !important;
    box-shadow: none !important;
    background-color: transparent !important;
    padding: 0;
  }

  &.is-focus .el-input__wrapper,
  .el-input__wrapper.is-focus,
  &:hover .el-input__wrapper {
    border: none !important;
    box-shadow: none !important;
  }

  .el-input__inner {
    border: none;
    border-radius: 0;
    font-size: 18px;
    font-weight: 600;
    padding: 0;
    height: auto;
    line-height: 1.4;
    background-color: transparent;
  }
}
.time {
  font-size: 12px;
  border-radius: 12px;
  padding: 0px 10px;
  background-color: #EBEEF5;
  margin-left: 15px;
  gap: 5px;
  display: inline-flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
}
</style>
