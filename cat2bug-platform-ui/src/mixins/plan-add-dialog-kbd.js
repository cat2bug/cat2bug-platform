import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import defectToolDialogClose from '@/mixins/defect-tool-dialog-close'
import formFieldHints from '@/mixins/form-field-hints'
import { usePlanAddDialogKbd } from '@/composables/keyboard/usePlanAddDialogKbd'

export default {
  mixins: [dialogFormShortcuts, defectToolDialogClose, formFieldHints],
  setup() {
    return usePlanAddDialogKbd()
  }
}
