import { useModulePageSettingsStore } from '@/stores/modulePageSettings'
import { isTeamModuleEnabled } from '@/utils/module-page-settings'

/** 项目页读取团队模块闸门。模块关闭时交付物树视为收起。 */
export default {
  computed: {
    deliverableModuleOn() {
      const settings = useModulePageSettingsStore().settings
      return isTeamModuleEnabled(settings, 'deliverable')
    },
    casePlanModuleOn() {
      const settings = useModulePageSettingsStore().settings
      return isTeamModuleEnabled(settings, 'case_plan')
    },
    defectModuleOn() {
      const settings = useModulePageSettingsStore().settings
      return isTeamModuleEnabled(settings, 'defect')
    },
    moduleTreeOpen() {
      return this.deliverableModuleOn && !!this.showModuleTree
    }
  }
}
