<template>
  <div class="app-container project-path-entry">
    <el-skeleton :rows="3" animated />
  </div>
</template>

<script>
import { getProjectByPathKeys } from '@/api/system/project'
import { useUserStore } from '@/stores/user'
import { isPathKeyFormatValid, isPathKeyReserved } from '@/utils/path-key'

export default {
  name: 'ProjectPathShareEntry',
  created() {
    this.enterProjectFromPath()
  },
  methods: {
    enterProjectFromPath() {
      const { teamKey, projectKey } = this.$route.params
      if (!teamKey || !projectKey
        || isPathKeyReserved(teamKey)
        || !isPathKeyFormatValid(projectKey)) {
        this.$router.replace('/404')
        return
      }
      getProjectByPathKeys(teamKey, projectKey).then(res => {
        const project = res && res.data
        if (!project || !project.projectId) {
          this.$router.replace('/404')
          return
        }
        return useUserStore().SwitchCurrentProject(project.projectId).then(() => {
          this.$router.replace({ name: 'Dashboard' })
        })
      }).catch(err => {
        const code = err && typeof err === 'object' ? err.code : undefined
        if (code === 403 || code === 401) {
          this.$router.replace('/401')
        } else {
          this.$router.replace('/404')
        }
      })
    }
  }
}
</script>
