<template>
  <error-state-shell
    code="401"
    illustration="401"
    :title="$t('error.401.title')"
    :description="$t('error.401.description')"
  >
    <template #actions>
      <el-button @click="back">{{ $t('error.401.back') }}</el-button>
      <el-button type="primary" @click="goHome">{{ $t('error.401.home') }}</el-button>
    </template>
  </error-state-shell>
</template>

<script>
import ErrorStateShell from '@/components/ErrorStateShell'

export default {
  name: 'Page401',
  components: { ErrorStateShell },
  methods: {
    back() {
      if (this.$route.query.noGoBack) {
        this.$router.push({ path: '/' })
      } else {
        this.$router.go(-1)
      }
    },
    goHome() {
      this.$router.push({ path: '/' })
    }
  }
}
</script>
