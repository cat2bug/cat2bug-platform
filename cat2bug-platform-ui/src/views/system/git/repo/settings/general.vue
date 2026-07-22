<template>
  <div class="git-settings-general" v-loading="loading">
    <h3 class="git-settings-general__title">{{ $t('git.repo.settings.nav.general') }}</h3>
    <el-form
      ref="form"
      class="git-settings-general__form"
      :model="form"
      :rules="rules"
      label-width="110px"
    >
      <el-form-item :label="$t('git.repo.name')" prop="name">
        <el-input
          v-model="form.name"
          maxlength="100"
          :placeholder="$t('git.repo.name')"
        />
      </el-form-item>
      <el-form-item :label="$t('git.repo.slug')" prop="slug">
        <el-input
          v-model="form.slug"
          maxlength="100"
          :placeholder="$t('git.repo.slug-placeholder')"
        >
          <template #prepend>{{ pathPrefixDisplay }}</template>
        </el-input>
        <div v-if="slugRepoAddressPreview" class="form-tip">
          {{ $t('git.repo.repo-address') }}{{ slugRepoAddressPreview }}
        </div>
        <div class="form-tip">
          {{ $t('git.repo.slug-format-hint') }}
          <span>，{{ $t('git.repo.slug-readonly-hint') }}</span>
        </div>
      </el-form-item>
      <el-form-item :label="$t('git.repo.visibility')" prop="visibility">
        <el-radio-group v-model="form.visibility">
          <el-radio value="public">{{ $t('git.repo.visibility-public') }}</el-radio>
          <el-radio value="project">{{ $t('git.repo.visibility-project') }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="$t('git.repo.default-branch')" prop="defaultBranch">
        <el-select
          v-model="form.defaultBranch"
          class="git-settings-general__branch"
          filterable
          clearable
          :loading="branchesLoading"
          :disabled="!branchOptions.length && !branchesLoading"
          :placeholder="branchSelectPlaceholder"
        >
          <el-option
            v-for="name in branchOptions"
            :key="name"
            :label="name"
            :value="name"
          />
        </el-select>
        <div v-if="!branchesLoading && !branchOptions.length" class="form-tip">
          {{ $t('git.repo.empty-repository') }}
        </div>
      </el-form-item>
      <el-form-item :label="$t('git.repo.remark')" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" :loading="saving" @click="submitForm">
          {{ $t('save') }}
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { getGitRepo, listGitRepoBranches, updateGitRepo } from '@/api/system/gitRepo'
import { gitRepoSettingsGeneralRoute } from '@/utils/git-repo-routes'

const SLUG_PATTERN = /^[a-zA-Z0-9]([a-zA-Z0-9._-]*[a-zA-Z0-9])?$/

export default {
  name: 'GitRepoSettingsGeneral',
  data() {
    return {
      loading: false,
      saving: false,
      branchesLoading: false,
      branchNames: [],
      originalSlug: null,
      form: {
        repositoryId: null,
        projectId: null,
        name: null,
        slug: null,
        defaultBranch: null,
        visibility: 'project',
        remark: null,
        teamKey: null,
        projectKey: null
      },
      rules: {
        name: [{ required: true, message: this.$t('git.repo.name-required'), trigger: 'blur' }],
        slug: [
          { required: true, message: this.$t('git.repo.slug-required'), trigger: 'blur' },
          { pattern: SLUG_PATTERN, message: this.$t('git.repo.slug-format-hint'), trigger: 'blur' }
        ],
        defaultBranch: [{
          validator: (rule, value, callback) => {
            if (!this.branchOptions.length) {
              callback()
              return
            }
            if (!(value || '').trim()) {
              callback(new Error(this.$t('git.repo.select-branch')))
              return
            }
            callback()
          },
          trigger: 'change'
        }]
      }
    }
  },
  computed: {
    pathTeamKey() {
      return this.form.teamKey || this.$route.params.teamKey || ''
    },
    pathProjectKey() {
      return this.form.projectKey || this.$route.params.projectKey || ''
    },
    pathPrefixDisplay() {
      if (this.pathTeamKey && this.pathProjectKey) {
        return `/${this.pathTeamKey}/${this.pathProjectKey}/code/`
      }
      return '/…/…/code/'
    },
    slugRepoAddressPreview() {
      const slug = (this.form.slug || '').trim()
      if (!slug || !this.pathTeamKey || !this.pathProjectKey) return ''
      return `${window.location.origin}/${this.pathTeamKey}/${this.pathProjectKey}/code/${slug}.git`
    },
    branchOptions() {
      const names = [...this.branchNames]
      const current = (this.form.defaultBranch || '').trim()
      if (current && !names.includes(current)) {
        names.unshift(current)
      }
      return names
    },
    branchSelectPlaceholder() {
      if (this.branchesLoading) return this.$t('loading')
      if (!this.branchOptions.length) return this.$t('git.repo.branches.no-default')
      return this.$t('git.repo.select-branch')
    }
  },
  watch: {
    '$route.params.repositoryId'() {
      this.loadRepo()
    },
    '$route.params.slug'() {
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
        const row = (res && res.data) || {}
        this.form = {
          repositoryId: row.repositoryId,
          projectId: row.projectId,
          name: row.name,
          slug: row.slug,
          defaultBranch: row.defaultBranch || null,
          visibility: row.visibility === 'public' ? 'public' : 'project',
          remark: row.remark,
          teamKey: row.teamKey || this.$route.params.teamKey || null,
          projectKey: row.projectKey || this.$route.params.projectKey || null
        }
        this.originalSlug = row.slug
        this.$nextTick(() => {
          if (this.$refs.form) this.$refs.form.clearValidate()
        })
        this.loadBranches(row.repositoryId || id)
      }).catch(() => {}).finally(() => {
        this.loading = false
      })
    },
    loadBranches(repositoryId) {
      const id = Number(repositoryId || this.form.repositoryId || this.$route.params.repositoryId)
      if (!Number.isFinite(id) || id <= 0) {
        this.branchNames = []
        return
      }
      this.branchesLoading = true
      listGitRepoBranches(id).then(res => {
        const list = (res && res.data) || []
        this.branchNames = list.map(b => b && b.name).filter(Boolean)
        const liveDefault = list.find(b => b && b.default)
        if (liveDefault && liveDefault.name) {
          this.form.defaultBranch = liveDefault.name
        } else if (this.form.defaultBranch && !this.branchNames.includes(this.form.defaultBranch)) {
          // keep metadata value only if still present; otherwise pick first branch
          if (this.branchNames.length) {
            this.form.defaultBranch = this.branchNames[0]
          }
        } else if (!this.form.defaultBranch && this.branchNames.length) {
          this.form.defaultBranch = this.branchNames[0]
        }
      }).catch(() => {
        this.branchNames = []
      }).finally(() => {
        this.branchesLoading = false
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const slugChanged = this.originalSlug != null && this.form.slug !== this.originalSlug
        if (slugChanged) {
          this.$modal.confirm(
            this.$t('git.repo.slug-change-warning-content'),
            this.$t('git.repo.slug-change-warning-title'),
            { confirmButtonText: this.$t('ok'), cancelButtonText: this.$t('cancel'), type: 'warning' }
          ).then(() => this.doSubmit(true)).catch(() => {})
        } else {
          this.doSubmit(false)
        }
      })
    },
    doSubmit(slugChanged) {
      const payload = {
        repositoryId: this.form.repositoryId,
        projectId: this.form.projectId,
        name: this.form.name,
        slug: this.form.slug,
        defaultBranch: this.form.defaultBranch,
        visibility: this.form.visibility === 'public' ? 'public' : 'project',
        remark: this.form.remark
      }
      this.saving = true
      updateGitRepo(payload).then(response => {
        this.$modal.msgSuccess(this.$t('modify-success'))
        if (response && response.cloneUrlChanged) {
          this.$modal.msgWarning(response.message || this.$t('git.repo.slug-changed-hint'))
        }
        const newSlug = this.form.slug
        this.originalSlug = newSlug
        if (slugChanged && newSlug) {
          this.replaceToNewSlug(newSlug)
        } else {
          this.loadRepo()
        }
      }).finally(() => {
        this.saving = false
      })
    },
    replaceToNewSlug(newSlug) {
      const ctx = {
        teamKey: this.pathTeamKey || this.$route.params.teamKey,
        projectKey: this.pathProjectKey || this.$route.params.projectKey,
        slug: newSlug
      }
      this.$router.replace(gitRepoSettingsGeneralRoute(ctx)).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.git-settings-general {
  color: var(--text-color-regular);
}

.git-settings-general__title {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--text-color-primary);
}

.git-settings-general__form {
  margin-top: 18px;
  max-width: 720px;
}

.git-settings-general__form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.git-settings-general__form :deep(.el-form-item__label) {
  color: var(--text-color-regular);
}

.git-settings-general__branch {
  width: 100%;
  max-width: 320px;
}

.form-tip {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-color-secondary);
}
</style>
