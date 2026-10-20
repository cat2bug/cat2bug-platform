<template>
  <div class="app-container markdown-doc-page" v-loading="loading">
    <div class="markdown-doc-page__header">
      <el-page-header @back="goBack">
        <template #content>
          <span class="markdown-doc-page__title">{{ pageTitle }}</span>
          <el-button
            v-if="canEdit && doc"
            link
            type="primary"
            class="markdown-doc-page__rename"
            :icon="$legacyIcon('el-icon-edit')"
            :title="$t('doc.rename')"
            @click="editTitle"
          />
          <el-tag v-if="!canEdit" size="small" type="info" class="markdown-doc-page__mode">{{ $t('preview') }}</el-tag>
        </template>
      </el-page-header>
    </div>
    <cat2-bug-editor
      v-if="doc"
      v-model="content"
      :editable="canEdit"
      :paper="true"
      :flat="true"
      :show-save="canEdit"
      :save-disabled="!dirty"
      :save-loading="saving"
      :min-height="480"
      class="markdown-doc-page__editor"
      @save="save"
    />
    <cat2-bug-empty v-else-if="!loading" :description="$t('no-data')" />
  </div>
</template>

<script>
import Cat2BugEditor from '@/components/Cat2BugEditor/index.vue'
import Cat2BugEmpty from '@/components/Cat2BugEmpty'
import { fetchDocumentMarkdown, getDocument, updateDocument } from '@/api/system/document'
import { deleteFileBestEffort, upload } from '@/api/common/upload'
import { checkPermi } from '@/utils/permission'
import { appMessageError } from '@/utils/app-message'
import { getCurrentProjectId } from '@/utils/store-context'
import { setDocumentReturnFolder } from '@/utils/document-folder-state'

export default {
  name: 'HandleMarkdownDocument',
  components: { Cat2BugEditor, Cat2BugEmpty },
  data() {
    return {
      loading: false,
      saving: false,
      doc: null,
      content: '',
      savedContent: '',
      leaveConfirmed: false
    }
  },
  computed: {
    canEdit() {
      return checkPermi(['system:document:edit'])
    },
    dirty() {
      return this.content !== this.savedContent
    },
    pageTitle() {
      if (!this.doc) {
        return this.$t('doc.md-page-title')
      }
      const name = this.doc.docName || ''
      const ext = this.doc.fileExtension ? ('.' + this.doc.fileExtension) : ''
      return name + (name.toLowerCase().endsWith('.md') ? '' : ext)
    }
  },
  watch: {
    '$route.params.docId': {
      immediate: true,
      handler(docId) {
        if (docId) {
          this.load(docId)
        }
      }
    }
  },
  mounted() {
    window.addEventListener('beforeunload', this.onBeforeUnload)
  },
  beforeUnmount() {
    window.removeEventListener('beforeunload', this.onBeforeUnload)
  },
  async beforeRouteLeave() {
    // Vue Router 4：返回 true/false，勿 next(true)（会导致导航卡住）
    if (this.leaveConfirmed) {
      await this.rememberListFolder()
      return true
    }
    const ok = await this.confirmLeave()
    if (ok) {
      this.leaveConfirmed = true
      await this.rememberListFolder()
    }
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
    /** 一次性记下所属文件夹；列表页消费后清除，侧栏再进仍是根目录 */
    async rememberListFolder() {
      const projectId = (this.doc && this.doc.projectId) || getCurrentProjectId()
      const fromQuery = this.$route.query && this.$route.query.docPid
      const folderId = Number(fromQuery) || (this.doc && this.doc.docPid) || 0
      if (!folderId || folderId <= 0) {
        setDocumentReturnFolder(projectId, null)
        return
      }
      try {
        const res = await getDocument(folderId)
        const folder = res && res.data
        if (folder && folder.docId) {
          setDocumentReturnFolder(projectId, folder)
          return
        }
      } catch (_) { /* fall through */ }
      setDocumentReturnFolder(projectId, { docId: folderId, docPid: 0, docName: '' })
    },
    async goBack() {
      if (!this.leaveConfirmed) {
        const ok = await this.confirmLeave()
        if (!ok) return
        this.leaveConfirmed = true
      }
      await this.rememberListFolder()
      this.$router.back()
    },
    editTitle() {
      if (!this.canEdit || !this.doc) return
      const current = (this.doc.docName || '').replace(/\.md$/i, '')
      this.$modal.prompt(this.$t('doc.file-name'), this.$t('doc.rename'), {
        confirmButtonText: this.$t('ok'),
        cancelButtonText: this.$t('cancel'),
        inputValue: current,
        inputPattern: /\S+/,
        inputErrorMessage: this.$t('doc.name-cannot-empty')
      }).then(({ value }) => {
        const name = String(value || '').trim().replace(/\.md$/i, '')
        if (!name || name === current) return
        return updateDocument({
          ...this.doc,
          docName: name
        }).then(() => {
          this.doc = { ...this.doc, docName: name }
          this.$modal.msgSuccess(this.$t('modify-success'))
        })
      }).catch(() => {})
    },
    load(docId) {
      this.loading = true
      this.doc = null
      this.content = ''
      this.savedContent = ''
      getDocument(docId).then(res => {
        const doc = res.data
        if (!doc) {
          appMessageError(this.$t('no-data'))
          return
        }
        const ext = String(doc.fileExtension || '').toLowerCase()
        if (ext && ext !== 'md' && ext !== 'markdown') {
          appMessageError(this.$t('doc.md-only'))
          return
        }
        this.doc = doc
        if (!doc.fileUrl) {
          this.content = ''
          this.savedContent = ''
          return
        }
        return fetchDocumentMarkdown(doc.fileUrl).then(text => {
          this.content = text || ''
          this.savedContent = this.content
        })
      }).catch(() => {
        this.doc = null
      }).finally(() => {
        this.loading = false
      })
    },
    save() {
      if (!this.canEdit || !this.doc || this.saving || !this.dirty) {
        return
      }
      this.saving = true
      const oldUrl = this.doc.fileUrl
      const baseName = (this.doc.docName || 'document').replace(/\.md$/i, '')
      const fileName = (baseName + '.md').replace(/[\\/:*?"<>|]/g, '-')
      const blob = new Blob([this.content || ''], { type: 'text/markdown;charset=utf-8' })
      const file = new File([blob], fileName, { type: 'text/markdown' })
      const formData = new FormData()
      formData.append('file', file)
      upload(formData).then(res => {
        const fileUrl = res && (res.fileName || (res.data && res.data.fileName))
        if (!fileUrl) {
          throw new Error('upload failed')
        }
        const payload = {
          ...this.doc,
          fileUrl,
          fileExtension: res.fileExtension || 'md'
        }
        return updateDocument(payload).then(() => {
          this.doc = { ...this.doc, fileUrl, fileExtension: payload.fileExtension }
          this.savedContent = this.content
          this.$modal.msgSuccess(this.$t('modify-success'))
          if (oldUrl && oldUrl !== fileUrl) {
            deleteFileBestEffort({ fileName: oldUrl })
          }
        })
      }).catch(() => {}).finally(() => {
        this.saving = false
      })
    }
  }
}
</script>

<style scoped>
.markdown-doc-page {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 120px);
}
.markdown-doc-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.markdown-doc-page__title {
  font-size: 16px;
  font-weight: 600;
  margin-right: 4px;
  vertical-align: middle;
}
.markdown-doc-page__rename {
  vertical-align: middle;
  padding: 0 4px;
  height: auto;
  min-height: 0;
}
.markdown-doc-page__mode {
  vertical-align: middle;
  margin-left: 6px;
}
.markdown-doc-page__editor {
  flex: 1;
  min-height: 0;
}
</style>
