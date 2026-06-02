import { listColumnLayout } from '@/api/system/defect-field'
import { isBuiltinFormFieldVisible, normalizeEnabledBuiltinKeys } from '@/utils/defect-field-layout'

/**
 * 新增/编辑缺陷表单：按项目内置字段启用配置控制表单项显隐。
 */
export default {
  data() {
    return {
      defectBuiltinFieldKeys: null,
      _defectBuiltinLayoutLoading: false
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler(val) {
        this.loadDefectBuiltinFieldLayout(val)
      }
    }
  },
  methods: {
    loadDefectBuiltinFieldLayout(projectId) {
      const pid = Number(projectId)
      if (!pid) {
        this.defectBuiltinFieldKeys = null
        return
      }
      this._defectBuiltinLayoutLoading = true
      listColumnLayout(pid)
        .then(res => {
          const data = res.data || {}
          const keys = data.enabledBuiltinFieldKeys
          this.defectBuiltinFieldKeys = normalizeEnabledBuiltinKeys(keys)
          this.applyDefectFormRulesForBuiltinLayout && this.applyDefectFormRulesForBuiltinLayout()
        })
        .catch(() => {
          this.defectBuiltinFieldKeys = normalizeEnabledBuiltinKeys(null)
        })
        .finally(() => {
          this._defectBuiltinLayoutLoading = false
        })
    },
    isBuiltinDefectFieldVisible(fieldKey) {
      if (this.defectBuiltinFieldKeys == null) {
        return true
      }
      return isBuiltinFormFieldVisible(fieldKey, this.defectBuiltinFieldKeys)
    }
  }
}
