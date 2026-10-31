import { listColumnLayout } from '@/api/system/defect-field'
import { isBuiltinFormFieldVisible, normalizeEnabledBuiltinKeys } from '@/utils/defect-field-layout'

/**
 * 新增/编辑缺陷表单：按项目内置字段启用配置控制表单项显隐。
 */
export default {
  data() {
    return {
      defectBuiltinFieldKeys: null,
      defectFieldLayout: null,
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
        this.defectFieldLayout = null
        return
      }
      this._defectBuiltinLayoutLoading = true
      listColumnLayout(pid)
        .then(res => {
          const data = res.data || {}
          const keys = data.enabledBuiltinFieldKeys
          this.defectFieldLayout = data
          this.defectBuiltinFieldKeys = normalizeEnabledBuiltinKeys(keys)
          this.applyDefectFormRulesForBuiltinLayout && this.applyDefectFormRulesForBuiltinLayout()
        })
        .catch(() => {
          /* 失败勿回退「全部启用」，否则会一直像默认属性 */
          this.defectFieldLayout = null
          this.defectBuiltinFieldKeys = normalizeEnabledBuiltinKeys([])
        })
        .finally(() => {
          this._defectBuiltinLayoutLoading = false
        })
    },
    isBuiltinDefectFieldVisible(fieldKey) {
      if (this.defectBuiltinFieldKeys == null) {
        /* 布局尚未返回：先不展示可关掉的内置项，避免闪一下全是默认字段 */
        return isBuiltinFormFieldVisible(fieldKey, normalizeEnabledBuiltinKeys([]))
      }
      return isBuiltinFormFieldVisible(fieldKey, this.defectBuiltinFieldKeys)
    }
  }
}
