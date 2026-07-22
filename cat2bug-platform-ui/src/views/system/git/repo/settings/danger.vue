<template>
  <div class="git-settings-danger" v-loading="loading">
    <h3 class="git-settings-danger__title">{{ $t('git.repo.settings.nav.danger') }}</h3>
    <p class="git-settings-danger__desc">{{ $t('git.repo.settings.danger.warning') }}</p>

    <div class="git-settings-danger__box">
      <div class="git-settings-danger__repo">
        <span class="git-settings-danger__label">{{ $t('git.repo.name') }}</span>
        <span class="git-settings-danger__value">{{ repoName || '—' }}</span>
      </div>
      <div class="git-settings-danger__repo">
        <span class="git-settings-danger__label">{{ $t('git.repo.slug') }}</span>
        <span class="git-settings-danger__value">{{ repoSlug || '—' }}</span>
      </div>

      <el-checkbox v-model="wipeDisk" class="git-settings-danger__wipe">
        {{ $t('git.repo.delete-wipe-disk') }}
      </el-checkbox>
      <p class="git-settings-danger__hint">{{ $t('git.repo.delete-wipe-disk-hint') }}</p>

      <el-form label-position="top" class="git-settings-danger__confirm-form" @submit.prevent>
        <el-form-item :label="$t('git.repo.settings.danger.confirm-slug')">
          <el-input
            v-model="confirmSlug"
            maxlength="100"
            :placeholder="$t('git.repo.settings.danger.confirm-slug-placeholder', { slug: repoSlug || '' })"
          />
        </el-form-item>
      </el-form>

      <el-button
        type="danger"
        class="git-settings-danger__delete-btn"
        :loading="deleting"
        :disabled="!canDelete"
        @click="handleDelete"
      >
        {{ $t('git.repo.settings.danger.delete-button') }}
      </el-button>
    </div>
  </div>
</template>

<script>
import { getGitRepo, delGitRepo } from '@/api/system/gitRepo'

export default {
  name: 'GitRepoSettingsDanger',
  data() {
    return {
      loading: false,
      deleting: false,
      repositoryId: null,
      repoName: '',
      repoSlug: '',
      teamKey: '',
      projectKey: '',
      wipeDisk: true,
      confirmSlug: ''
    }
  },
  computed: {
    canDelete() {
      return !!this.repositoryId
        && !!this.repoSlug
        && String(this.confirmSlug).trim() === String(this.repoSlug)
    }
  },
  watch: {
    '$route.params.repositoryId'() {
      this.loadRepo()
    }
  },
  created() {
    this.loadRepo()
  },
  methods: {
    loadRepo() {
      const id = Number(this.$route.params.repositoryId)
      if (!Number.isFinite(id) || id <= 0) return
      this.loading = true
      getGitRepo(id).then(res => {
        const data = (res && res.data) || {}
        this.repositoryId = data.repositoryId || id
        this.repoName = data.name || ''
        this.repoSlug = data.slug || String(this.$route.params.slug || '')
        this.teamKey = data.teamKey || this.$route.params.teamKey || ''
        this.projectKey = data.projectKey || this.$route.params.projectKey || ''
        this.confirmSlug = ''
      }).catch(() => {}).finally(() => {
        this.loading = false
      })
    },
    handleDelete() {
      if (!this.canDelete) return
      this.$modal.confirm(
        this.$t('git.repo.delete-confirm') + ' ' + this.$t('git.repo.delete-wipe-disk-hint')
      ).then(() => {
        this.deleting = true
        return delGitRepo(this.repositoryId, this.wipeDisk !== false)
      }).then(() => {
        this.$modal.msgSuccess(this.$t('delete-success'))
        this.goRepoList()
      }).catch(() => {}).finally(() => {
        this.deleting = false
      })
    },
    goRepoList() {
      const teamKey = this.teamKey || this.$route.params.teamKey
      const projectKey = this.projectKey || this.$route.params.projectKey
      if (teamKey && projectKey) {
        this.$router.push({
          name: 'GitRepoList',
          params: { teamKey, projectKey }
        }).catch(() => {})
        return
      }
      this.$router.push({ path: '/project/git-repo' }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.git-settings-danger {
  color: var(--text-color-regular);
}

.git-settings-danger__title {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--text-color-primary);
}

.git-settings-danger__desc {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-color-secondary);
}

.git-settings-danger__box {
  max-width: 560px;
  padding: 16px 18px;
  border: 1px solid var(--el-color-danger-light-5, rgba(245, 108, 108, 0.35));
  border-radius: 6px;
  background: var(--card-bg);
}

.git-settings-danger__repo {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 13px;
}

.git-settings-danger__label {
  flex: 0 0 88px;
  color: var(--text-color-secondary);
}

.git-settings-danger__value {
  color: var(--text-color-primary);
  font-weight: 600;
  word-break: break-all;
}

.git-settings-danger__wipe {
  margin: 14px 0 6px;
}

.git-settings-danger__hint {
  margin: 0 0 14px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-color-secondary);
}

.git-settings-danger__confirm-form {
  margin-bottom: 12px;
}

.git-settings-danger__confirm-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.git-settings-danger__delete-btn {
  height: var(--cat2bug-control-height, 32px);
  min-height: var(--cat2bug-control-height, 32px);
  padding: 0 15px;
  line-height: 1;
  box-sizing: border-box;
}
</style>
