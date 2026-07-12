import { SAVE_SHORTCUT_LABEL, useDialogFormShortcuts } from '@/composables/keyboard/useDialogFormShortcuts'

export { SAVE_SHORTCUT_LABEL }

export default {
  setup() {
    return useDialogFormShortcuts()
  }
}
