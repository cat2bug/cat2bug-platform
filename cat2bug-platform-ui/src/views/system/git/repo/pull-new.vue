<template>
  <div class="app-container git-pr-new" v-loading="loading">
    <git-repo-shell :repository-id="repositoryId" :repo-name="repoName" active-tab="pulls">
      <el-alert
        v-if="!canPush"
        type="warning"
        :title="$t('git.repo.pr.no-push-permission')"
        show-icon
        :closable="false"
        class="git-pr-new__alert"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="git-pr-new__form">
        <el-form-item :label="$t('git.repo.pr.form.source')" prop="sourceBranch">
          <el-select v-model="form.sourceBranch" filterable :disabled="!canPush" :placeholder="$t('git.repo.select-branch')">
            <el-option v-for="branch in branches" :key="branch.name" :label="branch.name" :value="branch.name" />
          </el-select>
        </el-form-item>
        <div class="git-pr-new__arrow">→</div>
        <el-form-item :label="$t('git.repo.pr.form.target')" prop="targetBranch">
          <el-select v-model="form.targetBranch" filterable :disabled="!canPush" :placeholder="$t('git.repo.select-branch')">
            <el-option v-for="branch in branches" :key="branch.name" :label="branch.name" :value="branch.name" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('git.repo.pr.form.title')" prop="title" class="git-pr-new__field--wide">
          <el-input v-model="form.title" :disabled="!canPush" :placeholder="$t('git.repo.pr.form.title-placeholder')" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item :label="$t('git.repo.pr.form.body')" class="git-pr-new__field--wide">
          <el-input v-model="form.body" type="textarea" :rows="8" :disabled="!canPush" :placeholder="$t('git.repo.pr.form.body-placeholder')" />
        </el-form-item>
        <div class="git-pr-new__actions">
          <el-button @click="goBack">{{ $t('cancel') }}</el-button>
          <el-button type="primary" :disabled="!canPush" :loading="submitting" @click="submit">
            {{ $t('git.repo.pr.create') }}
          </el-button>
        </div>
      </el-form>
    </git-repo-shell>
  </div>
</template>

<script>
import { createGitPullRequest } from '@/api/system/gitPullRequest'
import { getGitRepo, listGitRepoBranches } from '@/api/system/gitRepo'
import { checkPermi } from '@/utils/permission'
import { gitRepoPullDetailRoute, gitRepoPullsRoute } from '@/utils/git-repo-routes'
import GitRepoShell from './GitRepoShell.vue'

export default {
  name: 'GitRepoPullRequestNew',
  components: { GitRepoShell },
  data() {
    return {
      repositoryId: Number(this.$route.params.repositoryId),
      repoName: String(this.$route.params.slug || ''),
      branches: [],
      loading: false,
      submitting: false,
      form: {
        sourceBranch: String(this.$route.query.source || ''),
        targetBranch: String(this.$route.query.target || ''),
        title: '',
        body: ''
      },
      rules: {
        sourceBranch: [{ required: true, message: this.$t('git.repo.pr.form.source-required'), trigger: 'change' }],
        targetBranch: [{ required: true, message: this.$t('git.repo.pr.form.target-required'), trigger: 'change' }],
        title: [{ required: true, message: this.$t('git.repo.pr.form.title-required'), trigger: 'blur' }]
      }
    }
  },
  computed: {
    canPush() {
      return checkPermi(['system:git:repo:push'])
    }
  },
  created() {
    this.loadRepo()
    this.loadBranches()
  },
  methods: {
    goBack() {
      this.$router.push(gitRepoPullsRoute(this.$route)).catch(() => {})
    },
    loadRepo() {
      getGitRepo(this.repositoryId).then(res => {
        this.repoName = (res.data && res.data.name) || this.repoName
      }).catch(() => {})
    },
    loadBranches() {
      this.loading = true
      listGitRepoBranches(this.repositoryId).then(res => {
        this.branches = res.data || []
        const defaultBranch = this.branches.find(branch => branch.default)
        if (!this.form.targetBranch && defaultBranch) this.form.targetBranch = defaultBranch.name
        if (!this.form.sourceBranch && this.branches.length) this.form.sourceBranch = this.branches[0].name
      }).finally(() => {
        this.loading = false
      })
    },
    submit() {
      if (!this.canPush) return
      this.$refs.formRef.validate(valid => {
        if (!valid) return
        if (this.form.sourceBranch === this.form.targetBranch) {
          this.$modal.msgWarning(this.$t('git.repo.pr.form.same-branch'))
          return
        }
        this.submitting = true
        createGitPullRequest(this.repositoryId, {
          title: this.form.title.trim(),
          body: this.form.body,
          sourceBranch: this.form.sourceBranch,
          targetBranch: this.form.targetBranch
        }).then(res => {
          const pull = res.data || {}
          this.$modal.msgSuccess(this.$t('git.repo.pr.create-success'))
          this.$router.replace(gitRepoPullDetailRoute(this.$route, pull.number)).catch(() => {})
        }).finally(() => {
          this.submitting = false
        })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.git-pr-new {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 20px 20px 16px;
  box-sizing: border-box;
  color: var(--text-color-regular);
}

.git-pr-new__alert {
  max-width: 860px;
  margin-bottom: 16px;
}

.git-pr-new__form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  gap: 0 12px;
  max-width: 860px;
  padding: 20px;
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  background: var(--card-bg);
}

.git-pr-new__form :deep(.el-select) {
  width: 100%;
}

.git-pr-new__arrow {
  padding-top: 35px;
  color: var(--text-color-secondary);
}

.git-pr-new__field--wide,
.git-pr-new__actions {
  grid-column: 1 / -1;
}

.git-pr-new__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 4px;
}

@media (max-width: 640px) {
  .git-pr-new__form {
    grid-template-columns: 1fr;
  }

  .git-pr-new__arrow {
    display: none;
  }
}
</style>
