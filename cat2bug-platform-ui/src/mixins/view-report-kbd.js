import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import { useViewReportKbd } from '@/composables/keyboard/useViewReportKbd'

export default {
  mixins: [dialogFormShortcuts],
  setup() {
    return useViewReportKbd()
  }
}
