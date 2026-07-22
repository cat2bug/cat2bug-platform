<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="640px"
    append-to-body
    :close-on-click-modal="false"
    destroy-on-close
    class="git-repo-create-dialog"
    @update:model-value="onVisibleChange"
    @closed="onClosed"
  >
    <el-tabs v-model="mode" class="git-repo-create-tabs" @tab-change="onModeChange">
      <el-tab-pane :label="$t('git.repo.import.mode-empty')" name="empty" />
      <el-tab-pane :label="$t('git.repo.import.mode-url')" name="url" />
      <el-tab-pane :label="$t('git.repo.import.mode-github')" name="github" />
      <el-tab-pane :label="$t('git.repo.import.mode-gitlab')" name="gitlab" />
      <el-tab-pane :label="$t('git.repo.import.mode-gitee')" name="gitee" />
    </el-tabs>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="git-repo-create-form">
      <!-- Mode-specific: URL -->
      <template v-if="mode === 'url'">
        <el-form-item :label="$t('git.repo.import.source-url')" prop="sourceUrl" :rules="sourceUrlRules">
          <el-input
            v-model="form.sourceUrl"
            maxlength="500"
            :placeholder="$t('git.repo.import.source-url-placeholder')"
          />
        </el-form-item>
        <el-form-item :label="$t('git.repo.import.username')" prop="username">
          <el-input
            v-model="form.username"
            maxlength="200"
            clearable
            autocomplete="off"
            :placeholder="$t('git.repo.import.username-placeholder')"
          />
        </el-form-item>
        <el-form-item :label="$t('git.repo.import.token')" prop="token">
          <el-input
            v-model="form.token"
            type="password"
            show-password
            maxlength="500"
            clearable
            autocomplete="new-password"
            :placeholder="$t('git.repo.import.token-optional-placeholder')"
          />
        </el-form-item>
      </template>

      <!-- Mode-specific: GitHub / GitLab / Gitee -->
      <template v-else-if="isProviderMode">
        <el-form-item
          v-if="mode === 'gitlab'"
          :label="$t('git.repo.import.host')"
          prop="host"
        >
          <el-input
            v-model="form.host"
            maxlength="300"
            clearable
            :placeholder="$t('git.repo.import.host-placeholder')"
          />
        </el-form-item>
        <el-form-item :label="$t('git.repo.import.token')" prop="token" :rules="tokenRequiredRules">
          <div class="git-repo-create-token-row">
            <el-input
              v-model="form.token"
              type="password"
              show-password
              maxlength="500"
              clearable
              autocomplete="new-password"
              :placeholder="$t('git.repo.import.token-placeholder')"
            />
            <el-button
              type="primary"
              plain
              size="small"
              :loading="remoteLoading"
              :disabled="!form.token"
              @click="loadRemoteRepos"
            >{{ $t('git.repo.import.load-repos') }}</el-button>
          </div>
        </el-form-item>
        <el-form-item :label="$t('git.repo.import.remote-repo')" required>
          <div class="git-repo-create-remote">
            <el-input
              v-model="remoteKeyword"
              clearable
              size="small"
              class="git-repo-create-remote-search"
              :placeholder="$t('git.repo.import.remote-search-placeholder')"
              :disabled="!remoteList.length"
            />
            <div v-loading="remoteLoading" class="git-repo-create-remote-list">
              <div
                v-for="item in filteredRemoteList"
                :key="remoteKey(item)"
                class="git-repo-create-remote-item"
                :class="{ 'is-active': isRemoteSelected(item) }"
                @click="selectRemote(item)"
              >
                <div class="git-repo-create-remote-item-main">
                  <span class="git-repo-create-remote-name">{{ item.fullPath || item.name }}</span>
                  <span v-if="item.cloneUrl" class="git-repo-create-remote-url">{{ item.cloneUrl }}</span>
                </div>
                <el-tag v-if="item.defaultBranch" size="small" effect="plain">{{ item.defaultBranch }}</el-tag>
              </div>
              <div v-if="!remoteLoading && remoteList.length && !filteredRemoteList.length" class="git-repo-create-remote-empty">
                {{ $t('git.repo.import.remote-empty-filter') }}
              </div>
              <div v-else-if="!remoteLoading && !remoteList.length" class="git-repo-create-remote-empty">
                {{ $t('git.repo.import.remote-empty') }}
              </div>
            </div>
            <div v-if="form.sourceUrl" class="git-repo-create-form-tip">
              {{ $t('git.repo.import.selected-url') }}{{ form.sourceUrl }}
            </div>
          </div>
        </el-form-item>
      </template>

      <!-- Shared fields -->
      <el-form-item :label="$t('git.repo.name')" prop="name">
        <el-input
          v-model="form.name"
          maxlength="100"
          :placeholder="$t('git.repo.name')"
          @input="onNameInput"
        />
      </el-form-item>
      <el-form-item :label="$t('git.repo.slug')" prop="slug">
        <el-input
          v-model="form.slug"
          maxlength="100"
          :placeholder="$t('git.repo.slug-placeholder')"
          @input="onSlugInput"
        >
          <template #prepend>{{ pathPrefixDisplay }}</template>
        </el-input>
        <div v-if="slugRepoAddressPreview" class="git-repo-create-form-tip git-repo-create-address-tip">
          {{ $t('git.repo.repo-address') }}{{ slugRepoAddressPreview }}
        </div>
        <div class="git-repo-create-form-tip">{{ $t('git.repo.slug-format-hint') }}</div>
      </el-form-item>
      <el-form-item :label="$t('git.repo.visibility')" prop="visibility">
        <el-radio-group v-model="form.visibility">
          <el-radio label="public">{{ $t('git.repo.visibility-public') }}</el-radio>
          <el-radio label="project">{{ $t('git.repo.visibility-project') }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="$t('git.repo.default-branch')" prop="defaultBranch">
        <el-input
          v-model="form.defaultBranch"
          maxlength="100"
          placeholder="master"
          @input="onDefaultBranchInput"
        />
      </el-form-item>
      <el-form-item :label="$t('git.repo.remark')" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button size="small" @click="close">{{ $t('cancel') }}</el-button>
      <el-button type="primary" size="small" :loading="submitting" @click="submit">
        {{ submitLabel }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import { getProject } from '@/api/system/project'
import { addGitRepo, importGitRepo, listRemoteGitRepos } from '@/api/system/gitRepo'
import { getCurrentProjectId } from '@/utils/store-context'

const SLUG_PATTERN = /^[a-zA-Z0-9]([a-zA-Z0-9._-]*[a-zA-Z0-9])?$/
const MODES = ['empty', 'url', 'github', 'gitlab', 'gitee']
const PROVIDER_MODES = ['github', 'gitlab', 'gitee']
const DEFAULT_GITLAB_HOST = 'https://gitlab.com'

function emptyForm(projectId) {
  return {
    projectId: projectId || null,
    name: '',
    slug: '',
    visibility: 'project',
    defaultBranch: 'master',
    remark: '',
    sourceUrl: '',
    username: '',
    token: '',
    host: DEFAULT_GITLAB_HOST
  }
}

export default {
  name: 'GitRepoCreateDialog',
  props: {
    /** Dialog visibility (v-model) */
    modelValue: {
      type: Boolean,
      default: false
    },
    projectId: {
      type: [Number, String],
      default: null
    },
    teamKey: {
      type: String,
      default: ''
    },
    projectKey: {
      type: String,
      default: ''
    },
    title: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue', 'success', 'closed'],
  data() {
    return {
      mode: 'empty',
      submitting: false,
      remoteLoading: false,
      remoteList: [],
      remoteKeyword: '',
      selectedRemote: null,
      slugManual: false,
      nameManual: false,
      defaultBranchManual: false,
      resolvedTeamKey: '',
      resolvedProjectKey: '',
      form: emptyForm()
    }
  },
  computed: {
    dialogTitle() {
      return this.title || this.$t('git.repo.new')
    },
    submitLabel() {
      return this.mode === 'empty'
        ? this.$t('ok')
        : this.$t('git.repo.import.submit')
    },
    isProviderMode() {
      return PROVIDER_MODES.includes(this.mode)
    },
    resolvedProjectId() {
      const fromProp = Number(this.projectId)
      if (Number.isFinite(fromProp) && fromProp > 0) return fromProp
      const fromCurrent = Number(getCurrentProjectId())
      return Number.isFinite(fromCurrent) && fromCurrent > 0 ? fromCurrent : null
    },
    pathPrefixDisplay() {
      if (this.resolvedTeamKey && this.resolvedProjectKey) {
        return `/${this.resolvedTeamKey}/${this.resolvedProjectKey}/code/`
      }
      return '/…/…/code/'
    },
    slugRepoAddressPreview() {
      const slug = (this.form.slug || '').trim()
      if (!slug || !this.resolvedTeamKey || !this.resolvedProjectKey) return ''
      return `${window.location.origin}/${this.resolvedTeamKey}/${this.resolvedProjectKey}/code/${slug}.git`
    },
    filteredRemoteList() {
      const list = Array.isArray(this.remoteList) ? this.remoteList : []
      const kw = (this.remoteKeyword || '').trim().toLowerCase()
      if (!kw) return list
      return list.filter(item => {
        const hay = [item.name, item.fullPath, item.cloneUrl, item.defaultBranch]
          .filter(Boolean)
          .join(' ')
          .toLowerCase()
        return hay.includes(kw)
      })
    },
    rules() {
      return {
        name: [{ required: true, message: this.$t('git.repo.name-required'), trigger: 'blur' }],
        slug: [
          { required: true, message: this.$t('git.repo.slug-required'), trigger: 'blur' },
          { pattern: SLUG_PATTERN, message: this.$t('git.repo.slug-format-hint'), trigger: 'blur' }
        ]
      }
    },
    sourceUrlRules() {
      return [
        { required: true, message: this.$t('git.repo.import.source-url-required'), trigger: 'blur' }
      ]
    },
    tokenRequiredRules() {
      return [
        { required: true, message: this.$t('git.repo.import.token-required'), trigger: 'blur' }
      ]
    }
  },
  watch: {
    modelValue(open) {
      if (open) {
        this.bootstrapOpen()
      }
    },
    teamKey: {
      immediate: true,
      handler(val) {
        if (val) this.resolvedTeamKey = val
      }
    },
    projectKey: {
      immediate: true,
      handler(val) {
        if (val) this.resolvedProjectKey = val
      }
    }
  },
  methods: {
    onVisibleChange(val) {
      this.$emit('update:modelValue', val)
    },
    close() {
      this.$emit('update:modelValue', false)
    },
    onClosed() {
      this.resetAll()
      this.$emit('closed')
    },
    bootstrapOpen() {
      this.mode = 'empty'
      this.resetImportSecrets()
      this.form = emptyForm(this.resolvedProjectId)
      this.slugManual = false
      this.nameManual = false
      this.defaultBranchManual = false
      this.submitting = false
      if (this.teamKey) this.resolvedTeamKey = this.teamKey
      if (this.projectKey) this.resolvedProjectKey = this.projectKey
      this.ensurePathKeys().finally(() => {
        this.$nextTick(() => {
          if (this.$refs.formRef) this.$refs.formRef.clearValidate()
        })
      })
    },
    resetAll() {
      this.mode = 'empty'
      this.resetImportSecrets()
      this.form = emptyForm(this.resolvedProjectId)
      this.slugManual = false
      this.nameManual = false
      this.defaultBranchManual = false
      this.submitting = false
      this.remoteLoading = false
    },
    resetImportSecrets() {
      this.form.sourceUrl = ''
      this.form.username = ''
      this.form.token = ''
      this.form.host = DEFAULT_GITLAB_HOST
      this.remoteList = []
      this.remoteKeyword = ''
      this.selectedRemote = null
    },
    ensurePathKeys() {
      if (this.resolvedTeamKey && this.resolvedProjectKey) {
        return Promise.resolve()
      }
      if (this.teamKey && this.projectKey) {
        this.resolvedTeamKey = this.teamKey
        this.resolvedProjectKey = this.projectKey
        return Promise.resolve()
      }
      const { teamKey, projectKey } = (this.$route && this.$route.params) || {}
      if (teamKey && projectKey) {
        this.resolvedTeamKey = String(teamKey)
        this.resolvedProjectKey = String(projectKey)
        return Promise.resolve()
      }
      const projectId = this.resolvedProjectId
      if (!projectId) return Promise.resolve()
      return getProject(projectId).then(res => {
        const d = res.data || {}
        this.resolvedTeamKey = d.teamKey || this.resolvedTeamKey || ''
        this.resolvedProjectKey = d.projectKey || this.resolvedProjectKey || ''
      }).catch(() => {})
    },
    suggestSlugFromName(name) {
      if (!name) return ''
      let s = String(name).trim().replace(/[^a-zA-Z0-9._-]+/g, '-')
      s = s.replace(/[-._]{2,}/g, '-')
      s = s.replace(/^[^a-zA-Z0-9]+/, '').replace(/[^a-zA-Z0-9]+$/, '')
      if (s.length > 100) {
        s = s.slice(0, 100).replace(/[^a-zA-Z0-9]+$/, '')
      }
      return s
    },
    onNameInput() {
      this.nameManual = !!(this.form.name || '').trim()
      if (this.slugManual) return
      this.form.slug = this.suggestSlugFromName(this.form.name)
      this.$nextTick(() => {
        const form = this.$refs.formRef
        if (!form) return
        if (this.form.slug) form.clearValidate('slug')
        else form.validateField('slug')
      })
    },
    onSlugInput() {
      if (!(this.form.slug || '').trim()) {
        this.slugManual = false
        return
      }
      this.slugManual = true
    },
    onDefaultBranchInput() {
      this.defaultBranchManual = !!(this.form.defaultBranch || '').trim()
    },
    onModeChange(nextMode) {
      if (!MODES.includes(nextMode)) {
        this.mode = 'empty'
      }
      // Clear secrets & remote selection; keep shared name/slug/visibility/remark/defaultBranch
      const kept = {
        name: this.form.name,
        slug: this.form.slug,
        visibility: this.form.visibility,
        defaultBranch: this.form.defaultBranch,
        remark: this.form.remark,
        projectId: this.form.projectId || this.resolvedProjectId
      }
      this.form = {
        ...emptyForm(kept.projectId),
        ...kept,
        sourceUrl: '',
        username: '',
        token: '',
        host: DEFAULT_GITLAB_HOST
      }
      this.remoteList = []
      this.remoteKeyword = ''
      this.selectedRemote = null
      this.$nextTick(() => {
        if (this.$refs.formRef) this.$refs.formRef.clearValidate()
      })
    },
    remoteKey(item) {
      if (!item) return ''
      return item.cloneUrl || item.fullPath || item.name || ''
    },
    isRemoteSelected(item) {
      if (!item || !this.selectedRemote) return false
      return this.remoteKey(item) === this.remoteKey(this.selectedRemote)
    },
    selectRemote(item) {
      if (!item) return
      this.selectedRemote = item
      this.form.sourceUrl = item.cloneUrl || ''
      const remoteName = item.name || (item.fullPath ? String(item.fullPath).split('/').pop() : '')
      if ((!this.form.name || !this.nameManual) && remoteName) {
        this.form.name = remoteName
        this.nameManual = false
      }
      if ((!this.form.slug || !this.slugManual) && remoteName) {
        this.form.slug = this.suggestSlugFromName(remoteName)
      }
      if (item.defaultBranch && (!this.form.defaultBranch || !this.defaultBranchManual)) {
        this.form.defaultBranch = item.defaultBranch
        this.defaultBranchManual = false
      }
      this.$nextTick(() => {
        const form = this.$refs.formRef
        if (!form) return
        if (this.form.name) form.clearValidate('name')
        if (this.form.slug) form.clearValidate('slug')
      })
    },
    loadRemoteRepos() {
      if (!this.isProviderMode) return
      const token = (this.form.token || '').trim()
      if (!token) {
        this.$modal.msgError(this.$t('git.repo.import.token-required'))
        return
      }
      const payload = {
        provider: this.mode,
        token
      }
      if (this.mode === 'gitlab') {
        payload.host = (this.form.host || '').trim() || DEFAULT_GITLAB_HOST
      }
      this.remoteLoading = true
      this.selectedRemote = null
      this.form.sourceUrl = ''
      listRemoteGitRepos(payload).then(response => {
        const list = (response && response.data) || []
        this.remoteList = Array.isArray(list) ? list : []
        this.remoteKeyword = ''
        if (!this.remoteList.length) {
          this.$modal.msgWarning(this.$t('git.repo.import.remote-empty'))
        }
      }).catch(() => {
        this.remoteList = []
      }).finally(() => {
        this.remoteLoading = false
      })
    },
    submit() {
      if (!(this.form.slug || '').trim()) {
        this.form.slug = this.suggestSlugFromName(this.form.name)
      }
      this.$refs.formRef.validate(valid => {
        if (!valid) return
        const projectId = this.resolvedProjectId
        if (!projectId) {
          this.$modal.msgError(this.$t('defect.statistic.no-project'))
          return
        }
        if (this.mode === 'empty') {
          this.submitEmpty(projectId)
          return
        }
        this.submitImport(projectId)
      })
    },
    sharedPayload(projectId) {
      return {
        projectId,
        name: this.form.name,
        slug: this.form.slug,
        defaultBranch: this.form.defaultBranch || 'master',
        visibility: this.form.visibility === 'public' ? 'public' : 'project',
        remark: this.form.remark
      }
    },
    submitEmpty(projectId) {
      this.submitting = true
      addGitRepo(this.sharedPayload(projectId)).then(response => {
        this.$modal.msgSuccess(this.$t('create-success'))
        const created = (response && response.data) || {}
        this.emitSuccess(created, this.sharedPayload(projectId))
        this.close()
      }).finally(() => {
        this.submitting = false
      })
    },
    submitImport(projectId) {
      const sourceUrl = (this.form.sourceUrl || '').trim()
      if (!sourceUrl) {
        this.$modal.msgError(
          this.isProviderMode
            ? this.$t('git.repo.import.remote-required')
            : this.$t('git.repo.import.source-url-required')
        )
        return
      }
      const payload = {
        ...this.sharedPayload(projectId),
        sourceUrl
      }
      const username = (this.form.username || '').trim()
      const token = (this.form.token || '').trim()
      if (username) payload.username = username
      if (token) payload.token = token

      this.submitting = true
      importGitRepo(payload).then(response => {
        this.$modal.msgSuccess(this.$t('git.repo.import.success'))
        const created = (response && response.data) || {}
        this.emitSuccess(created, payload)
        this.close()
      }).finally(() => {
        this.submitting = false
      })
    },
    emitSuccess(created, fallback) {
      const item = {
        ...fallback,
        ...created,
        name: (created && created.name) || fallback.name,
        slug: (created && created.slug) || fallback.slug,
        teamKey: (created && created.teamKey) || this.resolvedTeamKey,
        projectKey: (created && created.projectKey) || this.resolvedProjectKey,
        visibility: (created && created.visibility) || fallback.visibility
      }
      this.$emit('success', item)
    }
  }
}
</script>

<style lang="scss" scoped>
.git-repo-create-tabs {
  margin-bottom: 8px;

  :deep(.el-tabs__header) {
    margin-bottom: 12px;
  }

  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
  }
}

.git-repo-create-token-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;

  .el-input {
    flex: 1 1 auto;
    min-width: 0;
  }

  .el-button {
    flex-shrink: 0;
  }
}

.git-repo-create-remote {
  width: 100%;
}

.git-repo-create-remote-search {
  margin-bottom: 8px;
}

.git-repo-create-remote-list {
  max-height: 180px;
  min-height: 64px;
  overflow-y: auto;
  border: 1px solid var(--border-color-lighter, var(--el-border-color-lighter, #ebeef5));
  border-radius: 4px;
}

.git-repo-create-remote-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  cursor: pointer;
  box-sizing: border-box;

  &:hover {
    background: var(--table-row-hover-bg, rgba(0, 0, 0, 0.04));
  }

  /* inset 避免被列表 overflow 裁掉左右边（outline 会被裁） */
  &.is-active {
    background: var(--el-color-primary-light-9, rgba(64, 158, 255, 0.1));
    box-shadow: inset 0 0 0 1px var(--el-color-primary-light-5, var(--el-color-primary));
  }
}

.git-repo-create-remote-item-main {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.git-repo-create-remote-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.git-repo-create-remote-url {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--text-color-secondary);
}

.git-repo-create-remote-empty {
  padding: 16px 8px;
  text-align: center;
  font-size: 13px;
  color: var(--text-color-placeholder);
}

.git-repo-create-form-tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--text-color-secondary);
}

.git-repo-create-address-tip {
  word-break: break-all;
}
</style>
