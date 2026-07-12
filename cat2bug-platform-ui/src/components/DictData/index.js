import DataDict from '@/utils/dict'
import { getDicts as getDicts } from '@/api/system/dict/data'
import { pinia } from '@/store/pinia.js'
import { useDictStore } from '@/stores/dict'

function searchDictByKey(dict, key) {
  if (key == null && key == "") {
    return null
  }
  try {
    for (let i = 0; i < dict.length; i++) {
      if (dict[i].key == key) {
        return dict[i].value
      }
    }
  } catch (e) {
    return null
  }
}

function install(app) {
  const dictStore = useDictStore(pinia)
  DataDict(app, {
    metas: {
      '*': {
        labelField: 'dictLabel',
        valueField: 'dictValue',
        request(dictMeta) {
          const storeDict = searchDictByKey(dictStore.dict, dictMeta.type)
          if (storeDict) {
            return new Promise(resolve => { resolve(storeDict) })
          }
          return new Promise((resolve, reject) => {
            getDicts(dictMeta.type).then(res => {
              dictStore.setDict({ key: dictMeta.type, value: res.data })
              resolve(res.data)
            }).catch(error => {
              reject(error)
            })
          })
        },
      },
    },
  })
}

export default {
  install,
}
