/**
 * @deprecated Use {@link useProjectCreateKbd} from `@/composables/keyboard/useProjectCreateKbd`.
 */
import { useProjectCreateKbd, PROJECT_CREATE_KBD_SCOPE } from '@/composables/keyboard/useProjectCreateKbd'

export { PROJECT_CREATE_KBD_SCOPE }

export default {
  setup() {
    return useProjectCreateKbd()
  }
}
