<template>
  <el-dialog v-model="open" :title="$t('case.review.title')" width="80%" append-to-body class="case-review-dialog">
    <el-steps :active="step" finish-status="success" align-center class="case-review-steps">
      <el-step>
        <template #title>
          <span class="case-review-steps__title" @click="goStep(0)">{{ $t('case.review.step-cases') }}</span>
        </template>
      </el-step>
      <el-step>
        <template #title>
          <span class="case-review-steps__title" :class="{ 'is-disabled': !completed }" @click="goStep(1)">{{ $t('case.review.step-report') }}</span>
        </template>
      </el-step>
    </el-steps>
    <el-alert v-if="skippedText" :title="skippedText" type="warning" :closable="false" show-icon class="case-review-alert" />
    <div v-show="step === 0">
      <div v-if="!completed" class="case-review-batch">
        <span>{{ $t('case.review.batch') }}</span>
        <el-radio-group v-model="batchVerdict" @change="applyBatch">
          <el-radio label="passed">{{ $t('case.review.passed') }}</el-radio>
          <el-radio label="changes">{{ $t('case.review.changes') }}</el-radio>
        </el-radio-group>
      </div>
      <el-table :data="items">
        <el-table-column :label="$t('id')" prop="caseNum" width="80" />
        <el-table-column :label="$t('title')" prop="caseName" min-width="160" />
        <el-table-column :label="$t('case.type')" prop="caseTypeName" width="120" />
        <el-table-column :label="$t('case.review.verdict')" min-width="220">
          <template #default="scope">
            <el-radio-group v-model="scope.row.verdict" :disabled="completed" @change="syncBatch">
              <el-radio label="passed">{{ $t('case.review.passed') }}</el-radio>
              <el-radio label="changes">{{ $t('case.review.changes') }}</el-radio>
            </el-radio-group>
          </template>
        </el-table-column>
        <el-table-column :label="$t('case.review.comment')" min-width="200">
          <template #default="scope">
            <el-input v-model="scope.row.comment" size="small" :disabled="completed" />
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div v-show="step === 1" class="case-review-minutes">
      <cat2-bug-editor v-model="report" :paper="false" :min-height="280" />
    </div>
    <template #footer>
      <div class="case-review-footer">
        <template v-if="step === 0 && !completed">
          <el-button type="primary" :loading="submitting" @click="complete">{{ $t('case.review.complete') }}</el-button>
          <el-button @click="open = false">{{ $t('close') }}</el-button>
        </template>
        <template v-else>
          <el-button type="primary" :loading="savingDoc" :disabled="!completed" @click="openSaveDoc">{{ $t('case.review.save-to-doc') }}</el-button>
          <el-button @click="open = false">{{ $t('close') }}</el-button>
        </template>
      </div>
    </template>

    <el-dialog
      v-model="saveDocVisible"
      :title="$t('case.review.save-doc-title')"
      width="520px"
      append-to-body
      destroy-on-close
    >
      <el-form label-width="100px" @submit.prevent>
        <el-form-item :label="$t('case.review.save-doc-folder')" required>
          <div class="case-review-save-folder">
            <el-tree
              ref="folderTree"
              lazy
              :load="loadFolderNode"
              :props="folderTreeProps"
              highlight-current
              node-key="docId"
              :expand-on-click-node="false"
              @node-click="onFolderClick"
            >
              <template #default="{ data }">
                <span class="case-review-save-folder__node">
                  <svg-icon icon-class="dir" />
                  <span>{{ data.docName }}</span>
                </span>
              </template>
            </el-tree>
            <el-button size="small" @click="createFolder">{{ $t('doc.create-folder') }}</el-button>
          </div>
        </el-form-item>
        <el-form-item :label="$t('case.review.save-doc-name')" required>
          <el-input v-model="saveDocName" maxlength="255" :placeholder="$t('doc.please-enter-name')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="case-review-footer">
          <el-button type="primary" :loading="savingDoc" @click="confirmSaveDoc">{{ $t('ok') }}</el-button>
          <el-button @click="saveDocVisible = false">{{ $t('cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script>
import { commentCaseReview, completeCaseReview, getCaseReview, listCaseReviews, submitCaseReview, updateCaseReviewReport } from '@/api/system/case-review'
import { addDocument, listDocument } from '@/api/system/document'
import { upload } from '@/api/common/upload'
import { appMessageError } from '@/utils/app-message'
import Cat2BugEditor from '@/components/Cat2BugEditor/index.vue'

export default {
  name: 'CaseReviewDialog',
  components: { Cat2BugEditor },
  data() {
    return {
      open: false,
      step: 0,
      projectId: null,
      reviewId: null,
      items: [],
      status: 'draft',
      report: '',
      batchVerdict: '',
      skippedText: '',
      submitting: false,
      savingDoc: false,
      saveDocVisible: false,
      saveDocName: '',
      saveDocPid: 0,
      folderSelected: false,
      folderTreeProps: {
        label: 'docName',
        children: 'children',
        isLeaf: 'leaf'
      }
    }
  },
  computed: {
    completed() {
      return this.status === 'completed'
    }
  },
  methods: {
    goStep(index) {
      if (index === 1 && !this.completed) {
        return
      }
      this.step = index
    },
    start(projectId, rows) {
      const list = Array.isArray(rows) ? rows : []
      const skipped = list.filter(row => row && row.reviewStatus === 'in_review')
      const ready = list.filter(row => row && row.reviewStatus !== 'in_review')
      if (!ready.length) {
        appMessageError(this.$t('case.review.all-in-review'))
        return
      }
      this.projectId = projectId
      this.reviewId = null
      this.status = 'draft'
      this.step = 0
      this.report = ''
      this.batchVerdict = ''
      this.submitting = false
      this.items = ready.map(row => ({
        caseId: row.caseId,
        caseNum: row.caseNum,
        caseName: row.caseName,
        caseTypeName: row.caseTypeName,
        verdict: '',
        comment: ''
      }))
      this.skippedText = skipped.length
        ? this.$t('case.review.skip-in-review') + skipped.map(row => '#' + row.caseNum).join('、')
        : ''
      this.open = true
    },
    applyBatch(value) {
      if (value !== 'passed' && value !== 'changes') {
        return
      }
      this.items.forEach(item => {
        item.verdict = value
      })
    },
    syncBatch() {
      const first = this.items.length ? this.items[0].verdict : ''
      this.batchVerdict = this.items.every(item => item.verdict === first) ? first : ''
    },
    complete() {
      if (this.items.some(item => item.verdict !== 'passed' && item.verdict !== 'changes')) {
        appMessageError(this.$t('case.review.need-verdict'))
        return
      }
      this.submitting = true
      const done = (res) => {
        const review = res.data || {}
        const extra = review.skippedCaseNums || []
        if (extra.length) {
          const line = this.$t('case.review.skip-in-review') + extra.map(num => '#' + num).join('、')
          this.skippedText = this.skippedText ? this.skippedText + '；' + line : line
        }
        this.reviewId = review.reviewId
        this.report = review.reportMarkdown || ''
        this.status = 'completed'
        this.step = 1
        this.$emit('changed')
      }
      const request = this.reviewId
        ? this.items.reduce((chain, item) => chain.then(() => commentCaseReview(this.reviewId, {
          caseId: item.caseId,
          verdict: item.verdict,
          comment: item.comment || ''
        })), Promise.resolve()).then(() => completeCaseReview(this.reviewId))
        : submitCaseReview(this.projectId, this.items.map(item => ({
          caseId: item.caseId,
          verdict: item.verdict,
          comment: item.comment || ''
        })))
      request.then(done).finally(() => {
        this.submitting = false
      })
    },
    openCase(projectId, caseId) {
      this.openExisting(projectId, caseId).then(opened => {
        if (!opened) {
          appMessageError(this.$t('case.review.all-in-review'))
        }
      })
    },
    openExisting(projectId, caseId) {
      return listCaseReviews(projectId).then(res => {
        const opens = (res.data || []).filter(item => item.status === 'open')
        const next = (index) => {
          if (index >= opens.length) {
            return false
          }
          return getCaseReview(opens[index].reviewId).then(detail => {
            const review = detail.data || {}
            const found = (review.items || []).some(item => Number(item.caseId) === Number(caseId))
            if (!found) {
              return next(index + 1)
            }
            this.projectId = projectId
            this.reviewId = review.reviewId
            this.items = review.items || []
            this.report = review.reportMarkdown || ''
            this.status = review.status === 'completed' ? 'completed' : 'open'
            this.step = this.status === 'completed' ? 1 : 0
            this.batchVerdict = ''
            this.skippedText = ''
            this.open = true
            return true
          })
        }
        return next(0)
      })
    },
    defaultDocName() {
      const now = new Date()
      const pad = (n) => String(n).padStart(2, '0')
      const stamp = [
        now.getFullYear(),
        pad(now.getMonth() + 1),
        pad(now.getDate())
      ].join('-') + ' ' + [
        pad(now.getHours()),
        pad(now.getMinutes()),
        pad(now.getSeconds())
      ].join(':')
      return this.$t('case.review.title') + '-' + stamp
    },
    openSaveDoc() {
      if (!this.completed) {
        return
      }
      this.saveDocName = this.defaultDocName()
      this.saveDocPid = 0
      this.folderSelected = true
      this.saveDocVisible = true
    },
    folderTreeKey(docId) {
      return docId != null && Number(docId) > 0 ? Number(docId) : -1
    },
    folderDocPid(docId) {
      return docId != null && Number(docId) > 0 ? Number(docId) : 0
    },
    loadFolderNode(node, resolve) {
      if (node.level === 0) {
        resolve([{
          docId: -1,
          docName: this.$t('doc.root-folder'),
          docType: 0,
          leaf: false
        }])
        this.$nextTick(() => {
          if (this.$refs.folderTree) {
            this.$refs.folderTree.setCurrentKey(-1)
          }
        })
        return
      }
      listDocument({
        pageNum: 1,
        pageSize: 9999,
        docType: 0,
        projectId: this.projectId,
        docPid: this.folderDocPid(node.data && node.data.docId)
      }).then(response => {
        const rows = (response.rows || []).map(row => ({
          ...row,
          leaf: false
        }))
        resolve(rows)
      }).catch(() => resolve([]))
    },
    onFolderClick(data) {
      this.saveDocPid = this.folderDocPid(data && data.docId)
      this.folderSelected = true
    },
    createFolder() {
      this.$modal.prompt(this.$t('doc.folder-name'), this.$t('doc.create-folder'), {
        confirmButtonText: this.$t('ok'),
        cancelButtonText: this.$t('cancel'),
        inputPattern: /\S+/,
        inputErrorMessage: this.$t('doc.name-cannot-empty'),
        appendTo: 'body'
      }).then(({ value }) => {
        const name = String(value || '').trim()
        if (!name) return
        const parentId = this.saveDocPid || 0
        return addDocument({
          projectId: this.projectId,
          docName: name,
          docType: 0,
          docPid: parentId
        }).then(() => {
          this.$modal.msgSuccess(this.$t('create-success'))
          const tree = this.$refs.folderTree
          const node = tree && tree.getNode(this.folderTreeKey(parentId))
          if (node) {
            node.loaded = false
            node.expand()
          }
        })
      }).catch(() => {})
    },
    confirmSaveDoc() {
      const name = (this.saveDocName || '').trim()
      if (!this.folderSelected) {
        appMessageError(this.$t('case.review.select-folder'))
        return
      }
      if (!name) {
        appMessageError(this.$t('case.review.doc-name-required'))
        return
      }
      this.savingDoc = true
      const markdown = this.report || ''
      const fileName = (name.toLowerCase().endsWith('.md') ? name : (name + '.md')).replace(/[\\/:*?"<>|]/g, '-')
      const blob = new Blob([markdown], { type: 'text/markdown;charset=utf-8' })
      const file = new File([blob], fileName, { type: 'text/markdown' })
      const formData = new FormData()
      formData.append('file', file)
      const persistReport = this.reviewId
        ? updateCaseReviewReport(this.reviewId, markdown).then(res => {
          this.report = (res.data && res.data.reportMarkdown) || markdown
        })
        : Promise.resolve()
      persistReport
        .then(() => upload(formData))
        .then(res => {
          const fileUrl = res && (res.fileName || (res.data && res.data.fileName))
          if (!fileUrl) {
            throw new Error('upload failed')
          }
          return addDocument({
            projectId: this.projectId,
            docName: name.replace(/\.md$/i, ''),
            docType: 1,
            docPid: this.saveDocPid || 0,
            fileUrl,
            fileExtension: (res.fileExtension || 'md')
          })
        })
        .then(() => {
          this.$modal.msgSuccess(this.$t('case.review.save-doc-success'))
          this.saveDocVisible = false
          this.open = false
        })
        .catch(() => {})
        .finally(() => {
          this.savingDoc = false
        })
    }
  }
}
</script>

<style scoped>
.case-review-steps {
  margin-bottom: 16px;
}
.case-review-steps__title {
  cursor: pointer;
}
.case-review-steps__title.is-disabled {
  cursor: not-allowed;
}
.case-review-alert {
  margin-bottom: 12px;
}
.case-review-batch {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.case-review-minutes {
  min-height: 0;
}
.case-review-footer {
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: nowrap;
  gap: 10px;
}
.case-review-dialog :deep(.el-dialog__footer) {
  padding-top: 12px;
}
.case-review-save-folder {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.case-review-save-folder :deep(.el-tree) {
  max-height: 240px;
  overflow: auto;
  border: 1px solid var(--border-color-light);
  border-radius: var(--cat2bug-border-radius, 4px);
  padding: 8px;
  background: var(--bg-color-base);
}
.case-review-save-folder__node {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
</style>
