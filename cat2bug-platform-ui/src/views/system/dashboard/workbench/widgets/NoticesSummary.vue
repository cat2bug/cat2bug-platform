<template>
  <div class="wb-list" v-loading="loading">
    <div class="notice-summary">
      <span class="n">{{ unread }}</span>
      <span class="l">{{ $t('workbench.unread-notices') }}</span>
      <el-button link type="primary" @click="goNotice">{{ $t('workbench.go') }}</el-button>
    </div>
  </div>
</template>

<script>
import { groupStatisticsNotice } from '@/api/system/notice'

export default {
  name: 'NoticesSummary',
  data() {
    return { loading: false, unread: 0 }
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      this.loading = true
      try {
        const res = await groupStatisticsNotice()
        const data = res.data || res || {}
        this.unread = Number(data.unread || data.unRead || data.total || 0)
      } catch (e) {
        this.unread = 0
      } finally {
        this.loading = false
      }
    },
    goNotice() {
      this.$router.push('/notice/index')
    }
  }
}
</script>

<style scoped lang="scss">
.notice-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 48px;
}
.notice-summary .n {
  font-size: 22px;
  font-weight: 600;
  color: var(--cat2bug-primary);
}
.notice-summary .l {
  flex: 1;
  color: var(--text-color-secondary);
  font-size: 13px;
}
</style>
