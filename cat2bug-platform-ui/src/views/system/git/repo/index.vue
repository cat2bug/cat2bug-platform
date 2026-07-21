<template>
  <div class="app-container case-body git-repo-page git-repo-list-layout project-list-page-host">
    <project-label class="git-repo-project-label" />

    <div v-loading="loading" class="git-repo-body">
      <!-- 关注仓库 -->
      <section v-if="featuredRepos.length && !queryParams.keyword" class="git-featured">
        <h3 class="git-section-title">{{ $t('git.repo.featured') }}</h3>
        <div class="git-featured-grid">
          <div
            v-for="item in featuredRepos"
            :key="'feat-' + item.repositoryId"
            class="git-featured-card"
            @click="handleBrowse(item)"
          >
            <div class="git-featured-card__head">
              <el-icon class="git-repo-icon"><FolderOpened /></el-icon>
              <a class="git-repo-name" href="javascript:;" @click.stop="handleBrowse(item)">{{ item.name }}</a>
              <star-switch
                class="git-featured-star"
                v-model="item.collect"
                @change="clickCollectHandle($event, item)"
              />
            </div>
            <p class="git-featured-card__desc">{{ item.remark || $t('git.repo.no-description') }}</p>
            <div class="git-featured-card__foot">
              <span class="git-lang-badge">Git</span>
              <div class="git-stat-row" @click.stop>
                <span class="git-stat" :title="$t('git.repo.browse')" @click="handleBrowse(item)">
                  <el-icon><View /></el-icon>
                </span>
                <span class="git-stat" :title="$t('git.repo.copy-clone-url')" @click="copyCloneUrl(item)">
                  <el-icon><CopyDocument /></el-icon>
                </span>
                <span
                  v-hasPermi="['system:git:repo:manage']"
                  class="git-stat"
                  :title="$t('git.repo.settings')"
                  @click="handleUpdate(item)"
                >
                  <el-icon><Setting /></el-icon>
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 搜索 / 排序（对齐 plan-tools / document-tools 全站列表工具栏规范） -->
      <div class="git-tools">
        <el-form class="left" :model="queryParams" size="small" :inline="true" label-width="0">
          <el-form-item label="" prop="keyword" class="git-hint-query">
            <el-input
              v-model="queryParams.keyword"
              clearable
              size="small"
              class="git-search"
              :placeholder="$t('git.repo.quick-search')"
              :prefix-icon="$legacyIcon('el-icon-search')"
              @keyup.enter="noop"
              @clear="noop"
            />
          </el-form-item>
        </el-form>
        <div class="git-tools-right">
          <el-select v-model="queryParams.sort" size="small" class="git-filter">
            <el-option :label="$t('git.repo.sort-updated')" value="updated" />
            <el-option :label="$t('git.repo.sort-name')" value="name" />
            <el-option :label="$t('git.repo.sort-created')" value="created" />
          </el-select>
          <el-button
            v-hasPermi="['system:git:repo:manage']"
            type="primary"
            plain
            size="small"
            :icon="$legacyIcon('el-icon-plus')"
            @click="handleAdd"
          >{{ $t('git.repo.new') }}</el-button>
        </div>
      </div>

      <!-- 列表 -->
      <div v-if="displayList.length" class="git-repo-list">
        <div v-for="item in displayList" :key="item.repositoryId" class="git-repo-item">
          <div class="git-repo-item__main">
            <div class="git-repo-item__title-row">
              <star-switch
                v-model="item.collect"
                @change="clickCollectHandle($event, item)"
              />
              <el-icon class="git-visibility-icon" :title="visibilityTitle(item)">
                <Unlock v-if="item.visibility === 'public'" />
                <Lock v-else />
              </el-icon>
              <a class="git-repo-name" href="javascript:;" @click="handleBrowse(item)">{{ item.name }}</a>
              <span class="git-meta-tag"><el-icon><Document /></el-icon> Git</span>
            </div>
            <p class="git-repo-item__desc">{{ item.remark || $t('git.repo.no-description') }}</p>
            <div class="git-repo-item__meta">
              <span>{{ $t('git.repo.updated-ago', { time: relativeTime(item.updateTime || item.createTime) }) }}</span>
              <a
                v-hasPermi="['system:git:repo:manage']"
                class="git-settings-link"
                href="javascript:;"
                @click="handleUpdate(item)"
              >{{ $t('git.repo.settings') }}</a>
            </div>
          </div>
          <div class="git-repo-item__stats">
            <span class="git-stat" :title="$t('git.repo.browse')" @click="handleBrowse(item)">
              <el-icon><View /></el-icon>
            </span>
            <span class="git-stat" :title="$t('git.repo.copy-clone-url')" @click="copyCloneUrl(item)">
              <el-icon><CopyDocument /></el-icon>
            </span>
            <span
              v-hasPermi="['system:git:repo:manage']"
              class="git-stat git-stat--danger"
              :title="$t('delete')"
              @click="handleDelete(item)"
            >
              <el-icon><component :is="$legacyIcon('el-icon-delete')" /></el-icon>
            </span>
          </div>
        </div>
      </div>
      <el-empty v-else-if="!loading" :description="$t('git.repo.empty')" />
    </div>

    <el-dialog
      :title="title"
      v-model="open"
      width="560px"
      append-to-body
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item :label="$t('git.repo.name')" prop="name">
          <el-input
            v-model="form.name"
            maxlength="100"
            :placeholder="$t('git.repo.name')"
            @input="onRepoNameInput"
          />
        </el-form-item>
        <el-form-item :label="$t('git.repo.slug')" prop="slug">
          <el-input
            v-model="form.slug"
            style="max-width: 600px"
            maxlength="100"
            :placeholder="$t('git.repo.slug-placeholder')"
            @input="onRepoSlugInput"
          >
            <template #prepend>{{ pathPrefixDisplay }}</template>
          </el-input>
          <div v-if="slugRepoAddressPreview" class="form-tip git-repo-address-tip">
            {{ $t('git.repo.repo-address') }}{{ slugRepoAddressPreview }}
          </div>
          <div class="form-tip">
            {{ $t('git.repo.slug-format-hint') }}
            <span v-if="form.repositoryId">，{{ $t('git.repo.slug-readonly-hint') }}</span>
          </div>
        </el-form-item>
        <el-form-item :label="$t('git.repo.visibility')" prop="visibility">
          <el-radio-group v-model="form.visibility">
            <el-radio label="public">{{ $t('git.repo.visibility-public') }}</el-radio>
            <el-radio label="project">{{ $t('git.repo.visibility-project') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('git.repo.default-branch')" prop="defaultBranch">
          <el-input v-model="form.defaultBranch" maxlength="100" placeholder="master" />
        </el-form-item>
        <el-form-item :label="$t('git.repo.remark')" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item v-if="form.repositoryId" :label="$t('git.repo.clone-url')">
          <div class="clone-url-row">
            <el-input :model-value="fullCloneUrl(form)" readonly size="small" />
            <el-button size="small" :icon="$legacyIcon('el-icon-document-copy')" @click="copyCloneUrl(form)">{{ $t('git.repo.copy-clone-url') }}</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" size="small" @click="submitForm">{{ $t('ok') }}</el-button>
          <el-button size="small" @click="cancel">{{ $t('cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { FolderOpened, View, CopyDocument, Setting, Lock, Unlock, Document } from '@element-plus/icons-vue'
import { listGitRepo, addGitRepo, updateGitRepo, delGitRepo, collectGitRepo } from '@/api/system/gitRepo'
import { getProject } from '@/api/system/project'
import ProjectLabel from '@/components/Project/ProjectLabel'
import StarSwitch from '@/components/StarSwitch'
import { getCurrentProjectId } from '@/utils/store-context'
import { formatTime } from '@/utils/index'
import { parseTime } from '@/utils/ruoyi'

const SLUG_PATTERN = /^[a-zA-Z0-9]([a-zA-Z0-9._-]*[a-zA-Z0-9])?$/

export default {
  name: 'GitRepo',
  components: { ProjectLabel, StarSwitch, FolderOpened, View, CopyDocument, Setting, Lock, Unlock, Document },
  data() {
    return {
      loading: true,
      repoList: [],
      open: false,
      title: '',
      form: {},
      originalSlug: null,
      slugManual: false,
      pathTeamKey: '',
      pathProjectKey: '',
      queryParams: {
        keyword: '',
        sort: 'updated'
      },
      rules: {
        name: [{ required: true, message: this.$t('git.repo.name-required'), trigger: 'blur' }],
        slug: [
          { required: true, message: this.$t('git.repo.slug-required'), trigger: 'blur' },
          { pattern: SLUG_PATTERN, message: this.$t('git.repo.slug-format-hint'), trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    pathPrefixDisplay() {
      if (this.pathTeamKey && this.pathProjectKey) {
        return `/${this.pathTeamKey}/${this.pathProjectKey}/code/`
      }
      return '/…/…/code/'
    },
    /** 路径名称下方的仓库地址预览（随 slug / 团队项目 key 变化） */
    slugRepoAddressPreview() {
      const slug = (this.form && this.form.slug || '').trim()
      if (!slug || !this.pathTeamKey || !this.pathProjectKey) {
        return ''
      }
      return `${window.location.origin}/${this.pathTeamKey}/${this.pathProjectKey}/code/${slug}`
    },
    sortedList() {
      const list = [...this.repoList]
      const sort = this.queryParams.sort
      list.sort((a, b) => {
        if (sort === 'name') {
          return String(a.name || '').localeCompare(String(b.name || ''), 'zh')
        }
        if (sort === 'created') {
          return this.timeValue(b.createTime) - this.timeValue(a.createTime)
        }
        return this.timeValue(b.updateTime || b.createTime) - this.timeValue(a.updateTime || a.createTime)
      })
      return list
    },
    displayList() {
      const kw = (this.queryParams.keyword || '').trim().toLowerCase()
      if (!kw) return this.sortedList
      return this.sortedList.filter(item => {
        const hay = [item.name, item.slug, item.remark, item.defaultBranch]
          .filter(Boolean)
          .join(' ')
          .toLowerCase()
        return hay.includes(kw)
      })
    },
    featuredRepos() {
      return this.sortedList.filter(item => !!item.collect)
    }
  },
  created() {
    this.getList()
  },
  methods: {
    noop() {},
    /** 关注 / 取消关注 */
    clickCollectHandle(collect, item) {
      item.collect = !!collect
      collectGitRepo(item.repositoryId, collect).then(() => {
        if (collect) {
          this.$message.success(this.$t('collect-success'))
        } else {
          this.$message.success(this.$t('cancel-success'))
        }
      }).catch(() => {
        item.collect = !collect
      })
    },
    visibilityTitle(item) {
      return item && item.visibility === 'public'
        ? this.$t('git.repo.visibility-public')
        : this.$t('git.repo.visibility-project')
    },
    /** 由仓库名称推导路径名称（仅保留 slug 允许字符） */
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
    onRepoNameInput() {
      if (this.form.repositoryId || this.slugManual) return
      this.form.slug = this.suggestSlugFromName(this.form.name)
      // 程序化回填不会触发 slug 的 blur 校验，需主动清除/重校，否则会残留「不能为空」
      this.$nextTick(() => {
        const form = this.$refs.form
        if (!form) return
        if (this.form.slug) {
          form.clearValidate('slug')
        } else {
          form.validateField('slug')
        }
      })
    },
    onRepoSlugInput() {
      if (!(this.form.slug || '').trim()) {
        this.slugManual = false
        return
      }
      this.slugManual = true
    },
    getProjectId() {
      return getCurrentProjectId()
    },
    timeValue(t) {
      if (!t) return 0
      const n = new Date(t).getTime()
      return Number.isNaN(n) ? 0 : n
    },
    relativeTime(t) {
      if (!t) return '-'
      const ms = this.timeValue(t)
      if (!ms) return parseTime(t) || '-'
      return formatTime(ms)
    },
    fullCloneUrl(row) {
      if (!row) return ''
      const path = row.cloneUrl
        || (row.teamKey && row.projectKey && row.slug
          ? `/${row.teamKey}/${row.projectKey}/code/${row.slug}.git`
          : (row.projectId && row.slug ? `/git/p/${row.projectId}/${row.slug}.git` : ''))
      if (!path) return ''
      return window.location.origin + path
    },
    copyCloneUrl(row) {
      const text = this.fullCloneUrl(row)
      if (!text) {
        this.$modal.msgError(this.$t('git.repo.copy-empty'))
        return
      }
      const onOk = () => this.$modal.msgSuccess(this.$t('git.repo.copy-success'))
      const onFail = () => this.$modal.msgError(this.$t('git.repo.copy-failed'))
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(onOk).catch(() => {
          if (this.copyTextFallback(text)) onOk()
          else onFail()
        })
        return
      }
      if (this.copyTextFallback(text)) onOk()
      else onFail()
    },
    copyTextFallback(text) {
      try {
        const input = document.createElement('textarea')
        input.value = text
        input.setAttribute('readonly', 'readonly')
        input.style.position = 'fixed'
        input.style.left = '-9999px'
        document.body.appendChild(input)
        input.select()
        const ok = document.execCommand('copy')
        document.body.removeChild(input)
        return ok
      } catch (e) {
        return false
      }
    },
    getList() {
      const projectId = this.getProjectId()
      if (!projectId) {
        this.repoList = []
        this.loading = false
        return
      }
      this.loading = true
      listGitRepo({ projectId }).then(response => {
        const data = response.data
        this.repoList = (Array.isArray(data) ? data : (response.rows || [])).map(item => ({
          ...item,
          collect: !!(item && (item.collect === true || item.collect === 1 || item.collect === '1'))
        }))
        this.applyPathKeysFromRepos(this.repoList)
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    applyPathKeysFromRepos(list) {
      const hit = (list || []).find(r => r && r.teamKey && r.projectKey)
      if (hit) {
        this.pathTeamKey = hit.teamKey
        this.pathProjectKey = hit.projectKey
      }
    },
    ensurePathKeys() {
      if (this.pathTeamKey && this.pathProjectKey) {
        return Promise.resolve()
      }
      const fromList = (this.repoList || []).find(r => r && r.teamKey && r.projectKey)
      if (fromList) {
        this.pathTeamKey = fromList.teamKey
        this.pathProjectKey = fromList.projectKey
        return Promise.resolve()
      }
      const projectId = this.getProjectId()
      if (!projectId) return Promise.resolve()
      return getProject(projectId).then(res => {
        const d = res.data || {}
        this.pathTeamKey = d.teamKey || ''
        this.pathProjectKey = d.projectKey || ''
      }).catch(() => {})
    },
    reset() {
      this.form = {
        repositoryId: null,
        projectId: this.getProjectId(),
        name: null,
        slug: null,
        defaultBranch: 'master',
        visibility: 'project',
        remark: null
      }
      this.originalSlug = null
      this.slugManual = false
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.reset()
    },
    handleAdd() {
      this.reset()
      this.ensurePathKeys().finally(() => {
        this.open = true
        this.title = this.$t('git.repo.new')
      })
    },
    handleUpdate(row) {
      this.reset()
      if (row.teamKey && row.projectKey) {
        this.pathTeamKey = row.teamKey
        this.pathProjectKey = row.projectKey
      }
      this.form = {
        repositoryId: row.repositoryId,
        projectId: row.projectId || this.getProjectId(),
        name: row.name,
        slug: row.slug,
        defaultBranch: row.defaultBranch || 'master',
        visibility: row.visibility === 'public' ? 'public' : 'project',
        remark: row.remark,
        cloneUrl: row.cloneUrl,
        teamKey: row.teamKey,
        projectKey: row.projectKey
      }
      this.originalSlug = row.slug
      this.slugManual = true
      this.ensurePathKeys().finally(() => {
        this.open = true
        this.title = this.$t('modify')
        this.$nextTick(() => {
          if (this.$refs.form) this.$refs.form.clearValidate()
        })
      })
    },
    handleBrowse(row) {
      this.$router.push({
        path: '/project/git-repo/browse/' + row.repositoryId,
        query: { name: row.name, slug: row.slug }
      })
    },
    submitForm() {
      if (!this.form.repositoryId && !(this.form.slug || '').trim()) {
        this.form.slug = this.suggestSlugFromName(this.form.name)
      }
      this.$refs.form.validate(valid => {
        if (!valid) return
        const isEdit = !!this.form.repositoryId
        const slugChanged = isEdit && this.originalSlug != null && this.form.slug !== this.originalSlug
        if (slugChanged) {
          this.$modal.confirm(
            this.$t('git.repo.slug-change-warning-content'),
            this.$t('git.repo.slug-change-warning-title'),
            { confirmButtonText: this.$t('ok'), cancelButtonText: this.$t('cancel'), type: 'warning' }
          ).then(() => this.doSubmit(isEdit)).catch(() => {})
        } else {
          this.doSubmit(isEdit)
        }
      })
    },
    doSubmit(isEdit) {
      const payload = {
        repositoryId: this.form.repositoryId,
        projectId: this.form.projectId || this.getProjectId(),
        name: this.form.name,
        slug: this.form.slug,
        defaultBranch: this.form.defaultBranch,
        visibility: this.form.visibility === 'public' ? 'public' : 'project',
        remark: this.form.remark
      }
      const request = isEdit ? updateGitRepo(payload) : addGitRepo(payload)
      request.then(response => {
        this.$modal.msgSuccess(isEdit ? this.$t('modify-success') : this.$t('create-success'))
        if (isEdit && response && response.cloneUrlChanged) {
          this.$modal.msgWarning(response.message || this.$t('git.repo.slug-changed-hint'))
        }
        this.open = false
        this.getList()
      })
    },
    handleDelete(row) {
      this.$modal.confirm(this.$t('git.repo.delete-confirm') + ' ' + this.$t('git.repo.delete-wipe-disk-hint'))
        .then(() => delGitRepo(row.repositoryId, true))
        .then(() => {
          this.getList()
          this.$modal.msgSuccess(this.$t('delete-success'))
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped lang="scss">
/* 使用 theme-dark.scss 全局语义色：--text-color-* / --card-bg / --border-color-* */
.git-repo-page {
  color: var(--text-color-regular);
  padding-bottom: 12px;
}

.git-repo-project-label {
  flex-shrink: 0;
}

.git-repo-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
}

.git-section-title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.git-featured {
  margin-bottom: 20px;
}

.git-featured-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 320px));
  gap: 16px;
  justify-content: start;
}

.git-featured-card {
  width: 100%;
  max-width: 320px;
  padding: 16px 18px;
  border: 1px solid var(--border-color-light);
  border-radius: 8px;
  background: var(--card-bg);
  color: var(--text-color-regular);
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    border-color: var(--el-color-primary);
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.18);
  }

  &__head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
  }

  .git-featured-star {
    margin-left: auto;
    flex-shrink: 0;
  }

  &__desc {
    margin: 0 0 14px;
    min-height: 40px;
    font-size: 13px;
    line-height: 1.5;
    color: var(--text-color-secondary);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}

.git-repo-icon {
  font-size: 18px;
  color: var(--text-color-secondary);
}

.git-repo-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-color-primary);
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.git-lang-badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: 12px;
  color: var(--text-color-secondary);
  background: var(--bg-color-block);
  border: 1px solid var(--border-color-light);
  border-radius: 4px;
}

.git-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cat2bug-toolbar-section-gap, 10px);
  width: 100%;
  margin-bottom: 0;
  padding: 0 0 8px;
  border-bottom: 1px solid var(--border-color-light);
  flex-wrap: wrap;
  box-sizing: border-box;
}

.git-tools > .left {
  flex: 1 1 auto;
  min-width: 0;
  max-width: 100%;
  width: auto;
}

.git-tools-right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  flex: 0 0 auto;
  gap: var(--cat2bug-toolbar-item-gap, 10px);
}

.git-hint-query {
  margin-right: 0 !important;
  margin-bottom: 0 !important;
}

.git-search {
  width: 320px;
  min-width: 240px;
  max-width: 100%;
}

.git-filter {
  width: 160px;
  min-width: 140px;
}

.git-repo-list {
  border-top: 1px solid var(--border-color-light);
}

.git-repo-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 4px;
  border-bottom: 1px solid var(--border-color-light);

  &__main {
    min-width: 0;
    flex: 1;
  }

  &__title-row {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    margin-bottom: 6px;
  }

  &__desc {
    margin: 0 0 8px;
    font-size: 13px;
    line-height: 1.5;
    color: var(--text-color-secondary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 12px;
    color: var(--text-color-placeholder);
  }

  &__stats {
    display: flex;
    align-items: center;
    gap: 14px;
    flex-shrink: 0;
    padding-top: 2px;
  }
}

.git-visibility-icon {
  font-size: 14px;
  color: var(--text-color-secondary);
}

.git-meta-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-color-secondary);
}

.git-settings-link {
  color: var(--el-color-primary);
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.git-stat-row,
.git-stat {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.git-stat {
  gap: 4px;
  color: var(--text-color-secondary);
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  width: 20px;
  height: 20px;
  justify-content: center;
  flex-shrink: 0;

  .el-icon {
    display: inline-flex;
    width: 1em;
    height: 1em;
  }

  &:hover {
    color: var(--el-color-primary);
  }

  &--danger {
    color: var(--cat2bug-delete-color, #e64545);

    &:hover {
      color: var(--cat2bug-delete-color-hover, #f56c6c);
    }
  }
}

.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-color-secondary);
  line-height: 1.4;
}

.git-repo-address-tip {
  color: var(--text-color-regular);
  word-break: break-all;
}

.clone-url-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

@media (max-width: 768px) {
  .git-featured-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .git-featured-card {
    max-width: none;
  }

  .git-tools {
    flex-direction: column;
    align-items: stretch;
  }

  .git-tools > .left {
    width: 100%;
  }

  .git-hint-query {
    width: 100% !important;
  }

  .git-search {
    width: 100% !important;
    min-width: 0;
  }

  .git-tools-right {
    width: 100%;
    justify-content: flex-start;
  }

  .git-filter {
    flex: 1 1 auto;
    width: auto;
    min-width: 140px;
  }
}
</style>

