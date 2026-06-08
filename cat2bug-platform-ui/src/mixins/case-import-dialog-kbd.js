/**
 * 用例页 Excel 导入弹框：Cmd/Ctrl+Enter 提交、Esc 关闭、Cmd/Ctrl+F 选文件。
 */
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import formFieldHints from '@/mixins/form-field-hints'
import { beginNativeFilePickerSession } from '@/utils/native-file-picker'
import { observeUploadFocusTarget, patchUploadFocusTarget, scheduleUploadShellRefocusAfterPicker } from '@/utils/upload-focus-tab'
import {
  UPLOAD_KBD_LIST,
  UPLOAD_KBD_SHELL,
  applyUploadListKeyboardFocus,
  clearUploadListKeyboardFocus,
  handleUploadListKeydown
} from '@/utils/upload-file-list-kbd'

const CASE_IMPORT_FILE_LIST_SELECTOR = '.el-upload-list__item'

export default {
  mixins: [dialogFormShortcuts, formFieldHints],
  data() {
    return {
      caseImportKeyboardZone: UPLOAD_KBD_SHELL,
      caseImportKeyboardFocusIndex: 0
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.upload.open
      },
      set(val) {
        this.upload.open = val
      }
    }
  },
  watch: {
    'upload.open'(val) {
      if (val) {
        this.$nextTick(() => {
          this.$_patchCaseImportUploadFocus()
        })
      } else {
        this.$_closeCaseImportFilePickerSession()
        this.caseImportKeyboardZone = UPLOAD_KBD_SHELL
        this.caseImportKeyboardFocusIndex = 0
        this.$_clearCaseImportListKeyboardFocus()
      }
    }
  },
  beforeDestroy() {
    this.$_closeCaseImportFilePickerSession()
  },
  methods: {
    shortcutSave() {
      if (typeof this.submitFileForm === 'function') this.submitFileForm()
    },
    shortcutClose(e) {
      if (e) {
        e.preventDefault()
        e.stopPropagation()
        if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation()
      }
      this.onCaseImportDialogBeforeClose(() => {
        this.upload.open = false
      })
    },
    onCaseImportDialogBeforeClose(done) {
      const uploadRef = this.$refs.upload
      const hasFiles = uploadRef && uploadRef.uploadFiles && uploadRef.uploadFiles.length > 0
      if (!hasFiles) {
        done()
        return
      }
      this.$modal.confirm(
        this.$i18n.t('case.import-close-confirm').toString(),
        this.$i18n.t('prompted').toString(),
        {
          confirmButtonText: this.$i18n.t('ok').toString(),
          cancelButtonText: this.$i18n.t('cancel').toString(),
          type: 'warning'
        }
      ).then(() => {
        if (uploadRef && typeof uploadRef.clearFiles === 'function') uploadRef.clearFiles()
        done()
      }).catch(() => {})
    },
    getFixedFieldHints() {
      return [
        {
          letter: 'F',
          badgeSelector: '.case-import-upload-focus-target',
          onActivate: () => this.openCaseImportFilePicker()
        }
      ]
    },
    getFieldHintContainer() {
      const shell = this.$refs.caseImportUploadFocus
      if (shell && shell.closest) {
        const body = shell.closest('.el-dialog__body')
        if (body) return body
      }
      const dialogBody = document.querySelector('.case-import-dialog .el-dialog__body')
      return dialogBody || this.$el
    },
    $_patchCaseImportUploadFocus() {
      const shell = this.$refs.caseImportUploadFocus
      if (!shell) return
      observeUploadFocusTarget(shell)
      patchUploadFocusTarget(shell)
    },
    prepareCaseImportFilePicker() {
      beginNativeFilePickerSession()
      const shell = this.$refs.caseImportUploadFocus
      if (shell) scheduleUploadShellRefocusAfterPicker(shell)
      if (shell && typeof shell.blur === 'function') shell.blur()
    },
    $_closeCaseImportFilePickerSession() {
      // 保留钩子供弹框关闭时调用；resume 由 scheduleUploadShellRefocusAfterPicker 自行清理
    },
    openCaseImportFilePicker() {
      if (this.upload && this.upload.isUploading) return
      this.prepareCaseImportFilePicker()
      const upload = this.$refs.upload
      if (!upload || !upload.$el) return
      const input = upload.$el.querySelector('input[type="file"]')
      if (input && typeof input.click === 'function') input.click()
    },
    $_getCaseImportFileCount() {
      const uploadRef = this.$refs.upload
      return uploadRef && uploadRef.uploadFiles ? uploadRef.uploadFiles.length : 0
    },
    onCaseImportUploadFocus() {
      if (this.caseImportKeyboardZone !== UPLOAD_KBD_LIST || !this.$_getCaseImportFileCount()) {
        this.caseImportKeyboardZone = UPLOAD_KBD_SHELL
        this.caseImportKeyboardFocusIndex = 0
      }
      this.$nextTick(() => this.$_applyCaseImportListKeyboardFocus())
    },
    onCaseImportUploadBlur() {
      this.$_clearCaseImportListKeyboardFocus()
    },
    $_clearCaseImportListKeyboardFocus() {
      const shell = this.$refs.caseImportUploadFocus
      if (shell) clearUploadListKeyboardFocus(shell)
    },
    $_applyCaseImportListKeyboardFocus() {
      const shell = this.$refs.caseImportUploadFocus
      if (!shell) return
      const next = applyUploadListKeyboardFocus(
        shell,
        this.caseImportKeyboardZone,
        this.caseImportKeyboardFocusIndex,
        CASE_IMPORT_FILE_LIST_SELECTOR
      )
      this.caseImportKeyboardZone = next.zone
      this.caseImportKeyboardFocusIndex = next.index
    },
    $_deleteCaseImportKeyboardFile(index) {
      const uploadRef = this.$refs.upload
      if (!uploadRef || !uploadRef.uploadFiles || !uploadRef.uploadFiles[index]) return
      const file = uploadRef.uploadFiles[index]
      if (typeof uploadRef.handleRemove === 'function') {
        uploadRef.handleRemove(file)
      }
      this.$nextTick(() => {
        const count = this.$_getCaseImportFileCount()
        if (!count) {
          this.caseImportKeyboardZone = UPLOAD_KBD_SHELL
          this.caseImportKeyboardFocusIndex = 0
        } else if (this.caseImportKeyboardFocusIndex >= count) {
          this.caseImportKeyboardFocusIndex = count - 1
        }
        this.$_applyCaseImportListKeyboardFocus()
      })
    },
    onCaseImportUploadKeydown(e) {
      const result = handleUploadListKeydown(
        e,
        { zone: this.caseImportKeyboardZone, index: this.caseImportKeyboardFocusIndex },
        {
          fileCount: this.$_getCaseImportFileCount(),
          onOpenPicker: () => this.openCaseImportFilePicker(),
          onDeleteFile: (index) => this.$_deleteCaseImportKeyboardFile(index)
        }
      )
      if (!result.handled) return
      this.caseImportKeyboardZone = result.state.zone
      this.caseImportKeyboardFocusIndex = result.state.index
      this.$_applyCaseImportListKeyboardFocus()
    }
  }
}
