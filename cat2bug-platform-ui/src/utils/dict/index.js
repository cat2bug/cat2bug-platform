import Dict from './Dict'
import { mergeOptions } from './DictOptions'

export default function(app, options) {
  mergeOptions(options)
  app.mixin({
    data() {
      if (this.$options === undefined || this.$options.dicts === undefined || this.$options.dicts === null) {
        return {}
      }
      const dict = new Dict()
      dict.owner = this
      return {
        dict
      }
    },
    created() {
      if (!(this.dict instanceof Dict)) {
        return
      }
      options.onCreated && options.onCreated(this.dict)
      this.dict.init(this.$options.dicts).then(() => {
        options.onReady && options.onReady(this.dict)
        this.$nextTick(() => {
          // 全局 mixin 未声明 emits，勿 $emit，避免 Vue3 控制台告警；业务侧用 onDictReady 钩子
          if (this.$options.methods && this.$options.methods.onDictReady instanceof Function) {
            this.$options.methods.onDictReady.call(this, this.dict)
          }
        })
      })
    },
  })
}
