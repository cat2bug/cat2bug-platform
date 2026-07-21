import { useLegacyKeyboardMixin } from '@/composables/keyboard/useLegacyKeyboardMixin'
import formFieldHintsMixin from '@/mixins/form-field-hints'

/**
 * Composition API bridge for {@link formFieldHintsMixin}.
 */
export function useFormFieldHints() {
  return useLegacyKeyboardMixin(formFieldHintsMixin)
}
