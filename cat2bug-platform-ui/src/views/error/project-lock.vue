<template>
  <error-state-shell
    code="PROJECT"
    illustration="disabled"
    :title="$t('error.project-lock.title')"
  >
    <template #description>
      <p>{{ $t('error.project-lock.subtitle') }}</p>
      <p v-if="lockRemark">{{ lockRemark }}</p>
      <p>{{ $t('error.project-lock.attention') }}</p>
    </template>
    <template #actions>
      <el-button type="primary" @click="handleSelectProjectClick">{{ $t('project.select') }}</el-button>
    </template>
  </error-state-shell>
</template>

<script>
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'
import ErrorStateShell from '@/components/ErrorStateShell'

export default {
  name: 'ProjectLockError',
  components: { ErrorStateShell },
  setup() {
    const userStore = useUserStore()
    const { config } = storeToRefs(userStore)
    return { config }
  },
  computed: {
    lockRemark() {
      return this.config.currentProjectLockRemark
    }
  },
  methods: {
    handleSelectProjectClick() {
      this.$router.push({ path: '/team/project-list' })
    }
  }
}
</script>
