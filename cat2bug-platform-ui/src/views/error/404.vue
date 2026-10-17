<template>
  <error-state-shell
    code="404"
    illustration="404"
    :title="$t('error.404.title')"
    :description="$t('error.404.description')"
  >
    <template #actions>
      <el-button type="primary" @click="goHome">{{ $t('error.404.home') }}</el-button>
    </template>
  </error-state-shell>
</template>

<script>
import ErrorStateShell from '@/components/ErrorStateShell'

export default {
  name: 'Page404',
  components: { ErrorStateShell },
  methods: {
    goHome() {
      this.$router.push({ path: '/' })
    }
  }
}
</script>
