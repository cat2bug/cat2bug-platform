import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import planHandleDrawerClose from '@/mixins/plan-handle-drawer-close'
import { usePlanHandleDrawerKbd } from '@/composables/keyboard/usePlanHandleDrawerKbd'

export default {
  mixins: [dialogFormShortcuts, planHandleDrawerClose],
  setup() {
    return usePlanHandleDrawerKbd()
  }
}
