/**
 * @deprecated Use {@link useProjectOptionSubKbd} from `@/composables/keyboard/useProjectOptionSubKbd`.
 */
import { useProjectOptionSubKbd, PROJECT_OPTION_KBD_SCOPE } from '@/composables/keyboard/useProjectOptionSubKbd'

export { PROJECT_OPTION_KBD_SCOPE }

export default {
  setup() {
    return useProjectOptionSubKbd()
  }
}
