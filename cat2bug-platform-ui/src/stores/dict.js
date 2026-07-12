import { defineStore } from 'pinia'

export const useDictStore = defineStore('dict', {
  state: () => ({
    dict: []
  }),
  actions: {
    setDict(data) {
      const { key, value } = data
      if (key !== null && key !== '') {
        this.dict.push({ key, value })
      }
    },
    removeDict(key) {
      try {
        for (let i = 0; i < this.dict.length; i++) {
          if (this.dict[i].key == key) {
            this.dict.splice(i, 1)
            return true
          }
        }
      } catch (e) {
        // ignore
      }
    },
    cleanDict() {
      this.dict = []
    }
  }
})
