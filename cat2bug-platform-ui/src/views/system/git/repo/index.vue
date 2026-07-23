<template>
  <div class="app-container case-body git-repo-page git-repo-list-layout project-list-page-host">
    <project-label class="git-repo-project-label" />

    <!-- 关注仓库：整行区块在双栏之上；内部仍为卡片网格 -->
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
            <span class="git-visibility-icon" :title="visibilityTitle(item)">
              <git-open-source-icon v-if="item.visibility === 'public'" />
              <el-icon v-else><Lock /></el-icon>
            </span>
            <a class="git-repo-name" href="javascript:;" @click.stop="handleBrowse(item)">{{ item.name }}</a>
            <star-switch
              class="git-featured-star"
              v-model="item.collect"
              @change="clickCollectHandle($event, item)"
            />
          </div>
          <p class="git-featured-card__desc">{{ item.remark || $t('git.repo.no-description') }}</p>
          <div class="git-featured-card__foot">
            <div class="git-repo-metrics">
              <span class="git-repo-metric" :title="$t('git.repo.metric-collect')">
                <el-icon><Star /></el-icon>
                <span>{{ metricCollect(item) }}</span>
              </span>
              <span class="git-repo-metric" :title="$t('git.repo.metric-branch')">
                <svg-icon icon-class="git-branch" class="git-repo-metric__icon" />
                <span>{{ metricBranch(item) }}</span>
              </span>
            </div>
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

    <!-- 列表 + 右侧统计：同一双栏块 -->
    <div v-loading="loading" class="git-repo-layout">
      <div class="git-repo-main">
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

        <div class="git-repo-list-body">
          <!-- 列表 -->
          <div v-if="displayList.length" class="git-repo-list">
            <div v-for="item in displayList" :key="item.repositoryId" class="git-repo-item">
              <div class="git-repo-item__main">
                <div class="git-repo-item__title-row">
                  <span class="git-visibility-icon" :title="visibilityTitle(item)">
                    <git-open-source-icon v-if="item.visibility === 'public'" />
                    <el-icon v-else><Lock /></el-icon>
                  </span>
                  <a class="git-repo-name" href="javascript:;" @click="handleBrowse(item)">{{ item.name }}</a>
                  <span class="git-repo-metrics git-repo-metrics--inline">
                    <span class="git-repo-metric" :title="$t('git.repo.metric-collect')">
                      <el-icon><Star /></el-icon>
                      <span>{{ metricCollect(item) }}</span>
                    </span>
                    <span class="git-repo-metric" :title="$t('git.repo.metric-branch')">
                      <svg-icon icon-class="git-branch" class="git-repo-metric__icon" />
                      <span>{{ metricBranch(item) }}</span>
                    </span>
                  </span>
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
                <star-switch
                  class="git-repo-stat-star"
                  v-model="item.collect"
                  @change="clickCollectHandle($event, item)"
                />
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

        <div v-show="filteredTotal > 0" class="git-repo-pagination-band table-pagination-band">
          <pagination
            class="git-repo-pagination"
            :total="filteredTotal"
            v-model:page="pageNum"
            v-model:limit="pageSize"
            :auto-scroll="false"
          />
        </div>
      </div>

      <!-- 右侧概览栏：≥1200px 展示，与列表同属一块双栏 -->
      <git-repo-overview-aside
        ref="overviewAside"
        class="git-repo-aside"
        :project-id="listProjectId"
      />
    </div>

    <!-- 新建 / 导入（共享对话框） -->
    <git-repo-create-dialog
      v-model="createOpen"
      :project-id="listProjectId"
      :team-key="pathTeamKey"
      :project-key="pathProjectKey"
      @success="onCreateSuccess"
    />

  </div>
</template>

<script>
import { View, CopyDocument, Setting, Lock, Star } from '@element-plus/icons-vue'
import { listGitRepo, delGitRepo, collectGitRepo } from '@/api/system/gitRepo'
import { getProject } from '@/api/system/project'
import ProjectLabel from '@/components/Project/ProjectLabel'
import StarSwitch from '@/components/StarSwitch'
import GitRepoCreateDialog from '@/components/Git/GitRepoCreateDialog'
import GitRepoOverviewAside from './GitRepoOverviewAside'
import GitOpenSourceIcon from './GitOpenSourceIcon'
import { getCurrentProjectId } from '@/utils/store-context'
import { formatTime } from '@/utils/index'
import { parseTime } from '@/utils/ruoyi'
import { gitRepoSettingsGeneralRoute } from '@/utils/git-repo-routes'

export default {
  name: 'GitRepo',
  components: {
    ProjectLabel,
    StarSwitch,
    GitRepoCreateDialog,
    GitRepoOverviewAside,
    GitOpenSourceIcon,
    View,
    CopyDocument,
    Setting,
    Lock,
    Star
  },
  data() {
    return {
      loading: true,
      repoList: [],
      createOpen: false,
      pathTeamKey: '',
      pathProjectKey: '',
      queryParams: {
        keyword: '',
        sort: 'updated'
      },
      pageNum: 1,
      pageSize: 10
    }
  },
  computed: {
    listProjectId() {
      return getCurrentProjectId()
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
    filteredList() {
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
    filteredTotal() {
      return this.filteredList.length
    },
    displayList() {
      const size = this.pageSize || 10
      const maxPage = Math.max(1, Math.ceil(this.filteredTotal / size) || 1)
      const page = Math.min(Math.max(1, this.pageNum || 1), maxPage)
      const start = (page - 1) * size
      return this.filteredList.slice(start, start + size)
    },
    featuredRepos() {
      return this.sortedList.filter(item => !!item.collect)
    }
  },
  watch: {
    'queryParams.keyword'() {
      this.pageNum = 1
    },
    'queryParams.sort'() {
      this.pageNum = 1
    },
    filteredTotal(n) {
      const maxPage = Math.max(1, Math.ceil((n || 0) / (this.pageSize || 10)) || 1)
      if (this.pageNum > maxPage) this.pageNum = maxPage
    }
  },
  created() {
    this.getList()
  },
  methods: {
    noop() {},
    /** 关注 / 取消关注 */
    clickCollectHandle(collect, item) {
      const prev = !!item.collect
      item.collect = !!collect
      collectGitRepo(item.repositoryId, collect).then(() => {
        if (collect) {
          this.$message.success(this.$t('collect-success'))
        } else {
          this.$message.success(this.$t('cancel-success'))
        }
        if (prev !== !!collect) {
          const next = Math.max(0, this.metricCollect(item) + (collect ? 1 : -1))
          item.collectCount = next
          if (this.$refs.overviewAside) {
            this.$refs.overviewAside.adjustCollectCount(collect ? 1 : -1)
          }
        }
      }).catch(() => {
        item.collect = !collect
      })
    },
    metricCollect(item) {
      const n = item && item.collectCount
      return Number.isFinite(Number(n)) ? Number(n) : 0
    },
    metricBranch(item) {
      const n = item && item.branchCount
      return Number.isFinite(Number(n)) ? Number(n) : 0
    },
    visibilityTitle(item) {
      return item && item.visibility === 'public'
        ? this.$t('git.repo.visibility-public')
        : this.$t('git.repo.visibility-project')
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
    handleAdd() {
      this.ensurePathKeys().finally(() => {
        this.createOpen = true
      })
    },
    onCreateSuccess() {
      this.getList()
    },
    /** Open shell settings (preferred editor); featured card + list share this entry. */
    handleUpdate(row) {
      const teamKey = row.teamKey || this.pathTeamKey
      const projectKey = row.projectKey || this.pathProjectKey
      const slug = row.slug
      if (teamKey && projectKey && slug) {
        this.$router.push(gitRepoSettingsGeneralRoute({ teamKey, projectKey, slug })).catch(() => {})
        return
      }
      this.ensurePathKeys().finally(() => {
        const tk = row.teamKey || this.pathTeamKey
        const pk = row.projectKey || this.pathProjectKey
        if (tk && pk && row.slug) {
          this.$router.push(gitRepoSettingsGeneralRoute({
            teamKey: tk,
            projectKey: pk,
            slug: row.slug
          })).catch(() => {})
          return
        }
        this.$modal.msgError(this.$t('git.repo.settings.nav-failed'))
      })
    },
    handleBrowse(row) {
      if (row.teamKey && row.projectKey && row.slug) {
        this.$router.push({
          name: 'GitRepoBrowse',
          params: {
            teamKey: row.teamKey,
            projectKey: row.projectKey,
            slug: row.slug
          }
        })
        return
      }
      // Fallback: legacy id path (redirects to slug when resolvable)
      this.$router.push({
        path: '/project/git-repo/browse/' + row.repositoryId,
        query: row.slug ? { slug: row.slug } : {}
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
.git-repo-page.git-repo-list-layout {
  color: var(--text-color-regular);
  padding: 20px 20px 12px;
  box-sizing: border-box;
  min-height: 0;
  flex: 1 1 auto;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: stretch;
}

/* 与用例页一致：项目条下边距清零，与下方区块间距仅由 --project-list-section-v-gap（父级 gap）承担 */
.git-repo-page :deep(h3.git-repo-project-label) {
  margin-top: 0;
  margin-bottom: 0;
}

.git-repo-project-label {
  flex-shrink: 0;
}

/* 双栏布局：列表与右侧统计同一滚动条一起滚（关注区在其上方整行） */
.git-repo-layout {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  flex: 1 1 0%;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.git-repo-main {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.git-repo-list-body {
  min-width: 0;
}

.git-repo-aside {
  flex: 0 0 280px;
  width: 280px;
  padding-top: 4px;
}

@media (max-width: 1199px) {
  .git-repo-aside {
    display: none;
  }
}

.git-section-title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.git-featured {
  flex-shrink: 0;
  width: 100%;
  /* 与 .git-tools 的 padding-bottom（查询框→分割线 8px）对齐，抵消父级 section gap */
  margin-bottom: calc(8px - var(--project-list-section-v-gap, 10px));
}

.git-featured-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 10px;
  width: 100%;
}

.git-featured-card {
  width: 100%;
  padding: 16px 18px;
  border: 1px solid var(--border-color-light);
  border-radius: 8px;
  background: var(--card-bg);
  color: var(--text-color-regular);
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  box-sizing: border-box;

  &:hover {
    border-color: var(--el-color-primary);
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.18);
  }

  &__head {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    margin-bottom: 10px;

    .git-visibility-icon,
    .git-featured-star {
      /* 与标题首行视觉对齐（15px 字号约一线高） */
      margin-top: 2px;
    }

    .git-repo-name {
      flex: 1 1 auto;
      min-width: 0;
      line-height: 1.35;
      word-break: break-word;
    }
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
    color: var(--text-color-placeholder, var(--text-color-secondary));
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

  /* 卡片内图标略放大 */
  .git-visibility-icon {
    font-size: 17px;

    .el-icon,
    :deep(svg) {
      width: 17px;
      height: 17px;
      font-size: 17px;
    }
  }

  .git-featured-star {
    font-size: 17px;
  }

  .git-repo-metric {
    font-size: 13px;

    .el-icon,
    .git-repo-metric__icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
    }
  }

  .git-stat {
    font-size: 18px;
    width: 22px;
    height: 22px;
  }
}

.git-repo-metrics {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.git-repo-metrics--inline {
  margin-left: 4px;
}

.git-repo-metric {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  line-height: 1;
  color: var(--text-color-regular);
  font-variant-numeric: tabular-nums;

  .el-icon,
  .git-repo-metric__icon {
    font-size: 14px;
    width: 14px;
    height: 14px;
    color: var(--text-color-secondary);
  }
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
  flex-shrink: 0;
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

.git-repo-pagination-band {
  flex-shrink: 0;
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
    align-items: flex-start;
    gap: 8px;
    flex-wrap: wrap;
    margin-bottom: 6px;

    .git-visibility-icon {
      margin-top: 2px;
    }
  }

  &__desc {
    margin: 0 0 8px;
    font-size: 13px;
    line-height: 1.5;
    color: var(--text-color-placeholder, var(--text-color-secondary));
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 12px;
    color: var(--text-color-regular);
  }

  &__stats {
    display: flex;
    align-items: center;
    gap: 14px;
    flex-shrink: 0;
    padding-top: 2px;
  }
}

.git-repo-stat-star {
  flex-shrink: 0;
  line-height: 1;
}

.git-visibility-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  line-height: 1;
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

