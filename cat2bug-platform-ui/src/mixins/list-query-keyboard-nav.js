import { useListQueryKeyboardNav } from '@/composables/keyboard/useListQueryKeyboardNav'
export default {
  setup() { return useListQueryKeyboardNav() },
  methods: {
    getListQueryNavFormRef() { return 'queryForm' },
    getListQueryNavToolbarRef() { return null },
    getListQueryNavItems() { return [] },
    getListQueryNavDefaultIndex() { return 0 },
  }
}
