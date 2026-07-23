<template>
  <el-popover
    placement="bottom-start"
    v-model:visible="popoverVisible"
    :width="360"
    popper-class="git-repo-select-popover"
    trigger="click"
    @show="onPopoverShow"
    @hide="onPopoverHide"
  >
    <template #reference>
      <div class="git-repo-select-trigger" role="button" tabindex="0">
        <star-switch
          class="git-repo-select-star"
          :model-value="!!currentRepo.collect"
          @change="onCurrentCollectChange"
          @click.stop
        />
        <span class="git-repo-select-name" :title="displayName">{{ displayName }}</span>
        <el-tag
          v-if="visibilityLabel"
          size="small"
          effect="plain"
          class="git-repo-select-visibility"
          :type="visibilityTagType(currentVisibility)"
          :class="visibilityTagClass(currentVisibility)"
        >{{ visibilityLabel }}</el-tag>
        <legacy-icon icon="el-icon-arrow-down" class="git-repo-select-caret" />
      </div>
    </template>

    <div class="git-repo-select-panel">
      <h3 class="git-repo-select-title">
        <img class="git-repo-select-title-icon" src="@/assets/icons/qiehuan.png" alt="" />
        {{ $t('git.repo.select') }}
      </h3>
      <div class="git-repo-select-toolbar">
        <el-input
          v-model="keyword"
          clearable
          size="small"
          class="git-repo-select-search"
          :prefix-icon="Search"
          :placeholder="$t('git.repo.select-search-placeholder')"
          @input="onSearchInput"
        />
        <el-button
          v-if="canManageGitRepo"
          class="git-repo-select-add"
          type="primary"
          plain
          size="small"
          :icon="Plus"
          :title="$t('git.repo.new')"
          @click.stop="openCreateDialog"
        />
      </div>
      <div v-loading="loading" class="git-repo-select-list">
        <div
          v-for="item in pagedList"
          :key="item.repositoryId"
          class="git-repo-select-item"
          :class="{ 'is-active': isActive(item) }"
          @click="handleSelect(item)"
        >
          <star-switch
            class="git-repo-select-item-star"
            :model-value="!!item.collect"
            @change="(val) => onItemCollectChange(val, item)"
            @click.stop
          />
          <div class="git-repo-select-item-main">
            <span class="git-repo-select-item-name" :title="item.name">{{ item.name || item.slug || '—' }}</span>
            <span class="git-repo-select-item-remark" :title="itemRemark(item)">{{ itemRemark(item) }}</span>
          </div>
          <el-tag
            size="small"
            effect="plain"
            class="git-repo-select-item-vis"
            :type="visibilityTagType(item.visibility)"
            :class="visibilityTagClass(item.visibility)"
          >{{ itemVisibilityLabel(item) }}</el-tag>
        </div>
        <div v-if="!loading && !filteredList.length" class="git-repo-select-empty">
          {{ $t('git.repo.select-empty') }}
        </div>
      </div>
      <template v-if="filteredTotal > pageSize">
        <el-divider class="git-repo-select-pager-divider" />
        <el-pagination
          class="git-repo-select-pager"
          size="small"
          layout="prev, next"
          :hide-on-single-page="true"
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="filteredTotal"
        />
      </template>
    </div>
  </el-popover>

  <git-repo-create-dialog
    v-model="createOpen"
    :project-id="resolvedProjectId"
    :team-key="pathTeamKey"
    :project-key="pathProjectKey"
    @success="onCreateSuccess"
  />
</template>

<script>
import { markRaw } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import StarSwitch from '@/components/StarSwitch'
import GitRepoCreateDialog from '@/components/Git/GitRepoCreateDialog'
import { listGitRepo, collectGitRepo, getGitRepo } from '@/api/system/gitRepo'
import { getCurrentProjectId } from '@/utils/store-context'
import { checkPermi } from '@/utils/permission'

function normalizeCollect(item) {
  return !!(item && (item.collect === true || item.collect === 1 || item.collect === '1'))
}

export default {
  name: 'GitRepoSelect',
  components: { StarSwitch, GitRepoCreateDialog, Plus },
  props: {
    repositoryId: {
      type: [Number, String],
      default: null
    },
    projectId: {
      type: [Number, String],
      default: null
    },
    /** Fallback display name before list/meta loads */
    repoName: {
      type: String,
      default: ''
    },
    /** public | project — optional override for current tag */
    visibility: {
      type: String,
      default: ''
    }
  },
  emits: ['change', 'collect-change'],
  data() {
    return {
      Search: markRaw(Search),
      Plus: markRaw(Plus),
      popoverVisible: false,
      loading: false,
      keyword: '',
      searchTimer: null,
      pageNum: 1,
      pageSize: 10,
      repoList: [],
      currentRepo: {},
      createOpen: false,
      pathTeamKey: '',
      pathProjectKey: ''
    }
  },
  computed: {
    canManageGitRepo() {
      return checkPermi(['system:git:repo:manage'])
    },
    resolvedProjectId() {
      const fromProp = Number(this.projectId)
      if (Number.isFinite(fromProp) && fromProp > 0) return fromProp
      const fromCurrent = Number(getCurrentProjectId())
      return Number.isFinite(fromCurrent) && fromCurrent > 0 ? fromCurrent : null
    },
    displayName() {
      return this.currentRepo.name || this.repoName || this.currentRepo.slug || '—'
    },
    visibilityLabel() {
      return this.currentVisibility === 'public'
        ? this.$t('git.repo.visibility-public')
        : this.$t('git.repo.visibility-project')
    },
    /** Prefer list/detail on currentRepo so header matches dropdown rows */
    currentVisibility() {
      const fromRepo = this.normalizeVisibility(this.currentRepo && this.currentRepo.visibility)
      if (fromRepo) return fromRepo
      return this.normalizeVisibility(this.visibility) || 'project'
    },
    filteredList() {
      const kw = (this.keyword || '').trim().toLowerCase()
      const list = Array.isArray(this.repoList) ? this.repoList : []
      const sorted = [...list].sort((a, b) => {
        const ac = normalizeCollect(a) ? 0 : 1
        const bc = normalizeCollect(b) ? 0 : 1
        if (ac !== bc) return ac - bc
        return String(a.name || '').localeCompare(String(b.name || ''), 'zh')
      })
      if (!kw) return sorted
      return sorted.filter(item => {
        const hay = [item.name, item.slug, item.remark]
          .filter(Boolean)
          .join(' ')
          .toLowerCase()
        return hay.includes(kw)
      })
    },
    filteredTotal() {
      return this.filteredList.length
    },
    pagedList() {
      const size = this.pageSize || 10
      const maxPage = Math.max(1, Math.ceil(this.filteredTotal / size) || 1)
      const page = Math.min(Math.max(1, this.pageNum || 1), maxPage)
      const start = (page - 1) * size
      return this.filteredList.slice(start, start + size)
    }
  },
  watch: {
    repositoryId: {
      immediate: true,
      handler() {
        this.syncCurrentRepo()
      }
    },
    projectId() {
      this.syncCurrentRepo()
    },
    resolvedProjectId() {
      this.syncCurrentRepo()
    },
    filteredTotal(n) {
      const maxPage = Math.max(1, Math.ceil((n || 0) / (this.pageSize || 10)) || 1)
      if (this.pageNum > maxPage) this.pageNum = maxPage
    }
  },
  beforeUnmount() {
    window.clearTimeout(this.searchTimer)
  },
  methods: {
    normalizeVisibility(value) {
      if (value === 'public' || value === 'project') return value
      return null
    },
    visibilityTagClass(visibility) {
      return this.normalizeVisibility(visibility) === 'public' ? 'is-public' : 'is-project'
    },
    /** Element Plus type so dark theme does not force default plain tags to gold */
    visibilityTagType(visibility) {
      return this.normalizeVisibility(visibility) === 'public' ? 'success' : 'warning'
    },
    itemVisibilityLabel(item) {
      const vis = this.normalizeVisibility(item && item.visibility) || 'project'
      return vis === 'public'
        ? this.$t('git.repo.visibility-public')
        : this.$t('git.repo.visibility-project')
    },
    itemRemark(item) {
      const remark = item && item.remark != null ? String(item.remark).trim() : ''
      return remark || this.$t('git.repo.no-description')
    },
    isActive(item) {
      return item && Number(item.repositoryId) === Number(this.repositoryId)
    },
    onPopoverShow() {
      this.keyword = ''
      this.pageNum = 1
      this.loadList()
    },
    onPopoverHide() {
      this.keyword = ''
      this.pageNum = 1
    },
    onSearchInput() {
      window.clearTimeout(this.searchTimer)
      this.searchTimer = window.setTimeout(() => {
        this.pageNum = 1
      }, 0)
    },
    normalizeRepoItem(item) {
      return {
        ...item,
        collect: normalizeCollect(item),
        visibility: this.normalizeVisibility(item && item.visibility) || 'project'
      }
    },
    loadList() {
      const projectId = this.resolvedProjectId
      if (!projectId) {
        this.repoList = []
        return Promise.resolve()
      }
      this.loading = true
      return listGitRepo({ projectId }).then(response => {
        const list = response.data || []
        this.repoList = (Array.isArray(list) ? list : []).map(item => this.normalizeRepoItem(item))
        this.applyPathKeysFromRepos(this.repoList)
        this.syncCurrentFromList()
      }).catch(() => {
        this.repoList = []
      }).finally(() => {
        this.loading = false
      })
    },
    syncCurrentFromList() {
      const id = Number(this.repositoryId)
      if (!Number.isFinite(id) || id <= 0) return
      const found = this.repoList.find(item => Number(item.repositoryId) === id)
      if (found) {
        this.currentRepo = { ...found }
      }
    },
    /**
     * 初始展示需带 collect：详情接口未必返回关注态，优先用项目仓库列表对齐。
     */
    syncCurrentRepo() {
      const id = Number(this.repositoryId)
      if (!Number.isFinite(id) || id <= 0) {
        this.currentRepo = { name: this.repoName, collect: false }
        return
      }
      const found = this.repoList.find(item => Number(item.repositoryId) === id)
      if (found) {
        this.currentRepo = { ...found }
        return
      }
      const projectId = this.resolvedProjectId
      if (!projectId) {
        this.loadCurrentFromDetail(id)
        return
      }
      listGitRepo({ projectId }).then(response => {
        const list = response.data || []
        this.repoList = (Array.isArray(list) ? list : []).map(item => this.normalizeRepoItem(item))
        const row = this.repoList.find(item => Number(item.repositoryId) === id)
        if (row) {
          this.currentRepo = { ...row }
          return
        }
        return this.loadCurrentFromDetail(id)
      }).catch(() => this.loadCurrentFromDetail(id))
    },
    loadCurrentFromDetail(id) {
      return getGitRepo(id).then(response => {
        const repo = response.data || {}
        this.currentRepo = {
          ...this.normalizeRepoItem(repo),
          name: repo.name || this.repoName
        }
      }).catch(() => {
        this.currentRepo = { name: this.repoName, collect: false, repositoryId: id }
      })
    },
    onCurrentCollectChange(collect) {
      const id = this.currentRepo.repositoryId || this.repositoryId
      if (!id) return
      const prev = !!this.currentRepo.collect
      this.currentRepo = { ...this.currentRepo, collect: !!collect }
      collectGitRepo(id, collect).then(() => {
        this.$message.success(this.$t(collect ? 'collect-success' : 'cancel-success'))
        const row = this.repoList.find(item => Number(item.repositoryId) === Number(id))
        if (row) row.collect = !!collect
        this.$emit('collect-change', { repositoryId: id, collect: !!collect })
      }).catch(() => {
        this.currentRepo = { ...this.currentRepo, collect: prev }
      })
    },
    onItemCollectChange(collect, item) {
      if (!item || !item.repositoryId) return
      const prev = !!item.collect
      item.collect = !!collect
      if (Number(item.repositoryId) === Number(this.repositoryId)) {
        this.currentRepo = { ...this.currentRepo, collect: !!collect }
      }
      collectGitRepo(item.repositoryId, collect).then(() => {
        this.$message.success(this.$t(collect ? 'collect-success' : 'cancel-success'))
        this.$emit('collect-change', { repositoryId: item.repositoryId, collect: !!collect })
      }).catch(() => {
        item.collect = prev
        if (Number(item.repositoryId) === Number(this.repositoryId)) {
          this.currentRepo = { ...this.currentRepo, collect: prev }
        }
      })
    },
    handleSelect(item) {
      if (!item || !item.slug) return
      const repositoryId = Number(item.repositoryId)
      if (!Number.isFinite(repositoryId) || repositoryId <= 0) return
      if (this.isActive(item)) {
        this.popoverVisible = false
        return
      }
      const { teamKey, projectKey } = this.$route.params
      const params = {
        teamKey: item.teamKey || teamKey,
        projectKey: item.projectKey || projectKey,
        slug: item.slug
      }
      if (!params.teamKey || !params.projectKey || !params.slug) {
        this.$router.push({
          path: '/project/git-repo/browse/' + repositoryId,
          query: item.slug ? { slug: item.slug } : {}
        }).catch(() => {})
      } else {
        const routeName = this.routeNameForSwitch()
        // Drop stale ?ref= from previous repo so the new default branch loads
        this.$router.push({ name: routeName, params, query: {} }).catch(() => {})
      }
      this.popoverVisible = false
      this.$emit('change', item)
    },
    routeNameForSwitch() {
      const name = this.$route && this.$route.name
      if (name === 'GitRepoBranches') return 'GitRepoBranches'
      if (name === 'GitRepoPullRequests') return 'GitRepoPullRequests'
      if (
        name === 'GitRepoSettings'
        || name === 'GitRepoSettingsGeneral'
        || name === 'GitRepoSettingsDanger'
        || name === 'GitRepoSettingsMembers'
        || name === 'GitRepoSettingsWebhooks'
      ) {
        return name === 'GitRepoSettingsMembers' || name === 'GitRepoSettingsWebhooks'
          ? name
          : 'GitRepoSettingsGeneral'
      }
      return 'GitRepoBrowse'
    },
    applyPathKeysFromRepos(list) {
      const hit = (list || []).find(r => r && r.teamKey && r.projectKey)
      if (hit) {
        this.pathTeamKey = hit.teamKey
        this.pathProjectKey = hit.projectKey
      }
    },
    openCreateDialog() {
      if (!this.canManageGitRepo) return
      this.popoverVisible = false
      const { teamKey, projectKey } = (this.$route && this.$route.params) || {}
      if (!this.pathTeamKey && teamKey) this.pathTeamKey = String(teamKey)
      if (!this.pathProjectKey && projectKey) this.pathProjectKey = String(projectKey)
      this.createOpen = true
    },
    onCreateSuccess(item) {
      const normalized = this.normalizeRepoItem(item || {})
      if (normalized.teamKey) this.pathTeamKey = normalized.teamKey
      if (normalized.projectKey) this.pathProjectKey = normalized.projectKey
      if (normalized.repositoryId && normalized.slug) {
        this.handleSelect(normalized)
      } else {
        this.loadList()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.git-repo-select-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  min-width: 0;
  cursor: pointer;
  user-select: none;
  border: none;
  background: transparent;
  padding: 0;
  color: var(--text-color-primary);
}

.git-repo-select-star {
  flex-shrink: 0;
  font-size: 16px;
}

.git-repo-select-name {
  flex: 0 1 auto;
  min-width: 0;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.git-repo-select-caret {
  flex-shrink: 0;
  font-size: 12px !important;
  width: 12px;
  height: 12px;
  color: var(--text-color-regular, #606266);
}

.git-repo-select-panel {
  margin: -4px 0;
}

.git-repo-select-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 5px 0 15px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.git-repo-select-title-icon {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  object-fit: contain;
  display: block;
}

@at-root html.dark .git-repo-select-title-icon {
  /* 原图为黑色描线，暗色下反相为浅色 */
  filter: invert(1);
}

.git-repo-select-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.git-repo-select-search {
  flex: 1 1 auto;
  min-width: 0;

  /* popover 不在全站 form/dialog 高度 mixin 内，需显式对齐 --cat2bug-control-height */
  :deep(.el-input__wrapper) {
    height: var(--cat2bug-control-height, 32px) !important;
    min-height: var(--cat2bug-control-height, 32px) !important;
    box-sizing: border-box;
  }
}

.git-repo-select-add {
  flex-shrink: 0;
  width: var(--cat2bug-control-height, 32px) !important;
  height: var(--cat2bug-control-height, 32px) !important;
  min-height: var(--cat2bug-control-height, 32px) !important;
  padding: 0 !important;
  box-sizing: border-box;
}

.git-repo-select-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: hidden;
  min-height: 48px;
}

.git-repo-select-pager-divider {
  margin: 8px 0 6px;

  :deep(.el-divider--horizontal),
  &.el-divider--horizontal {
    border-top-color: var(--border-color-lighter, #f0f2f5);
  }
}

.git-repo-select-pager {
  display: flex;
  justify-content: flex-end;
  margin: 0 0 2px;
  clear: both;
}

.git-repo-select-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 6px;
  border-radius: 6px;
  cursor: pointer;

  &:hover,
  &.is-active {
    background: var(--table-row-hover-bg, rgba(0, 0, 0, 0.04));
  }
}

.git-repo-select-item-star {
  flex-shrink: 0;
  font-size: 15px;
}

.git-repo-select-item-main {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.git-repo-select-item-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.git-repo-select-item-remark {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--text-color-secondary);
}

.git-repo-select-empty {
  padding: 16px 8px;
  text-align: center;
  font-size: 13px;
  color: var(--text-color-placeholder);
}
</style>

<!-- Unscoped: el-tag is a child component + popover teleports; dark theme gold default needs type + overrides -->
<style lang="scss">
.git-repo-select-visibility,
.git-repo-select-item-vis {
  flex-shrink: 0;
  height: auto !important;
  min-height: 22px;
  padding: 2px 10px !important;
  line-height: 18px !important;
  border-radius: 4px;
  font-size: 12px;
  box-sizing: border-box;
  white-space: nowrap;
  max-width: none;
}

.git-repo-select-visibility.is-public,
.git-repo-select-item-vis.is-public,
.git-repo-select-visibility.el-tag--success,
.git-repo-select-item-vis.el-tag--success {
  color: var(--el-color-success) !important;
  border-color: var(--el-color-success-light-5, var(--el-color-success)) !important;
  background: var(--el-color-success-light-9, rgba(103, 194, 58, 0.12)) !important;
}

.git-repo-select-visibility.is-project,
.git-repo-select-item-vis.is-project,
.git-repo-select-visibility.el-tag--warning,
.git-repo-select-item-vis.el-tag--warning {
  color: var(--el-color-warning) !important;
  border-color: var(--el-color-warning-light-5, var(--el-color-warning)) !important;
  background: var(--el-color-warning-light-9, rgba(230, 162, 60, 0.12)) !important;
}

html.dark {
  .git-repo-select-visibility.is-public,
  .git-repo-select-item-vis.is-public,
  .git-repo-select-visibility.el-tag--success,
  .git-repo-select-item-vis.el-tag--success {
    color: #85ce61 !important;
    border-color: rgba(103, 194, 58, 0.45) !important;
    background: rgba(103, 194, 58, 0.14) !important;
  }

  .git-repo-select-visibility.is-project,
  .git-repo-select-item-vis.is-project,
  .git-repo-select-visibility.el-tag--warning,
  .git-repo-select-item-vis.el-tag--warning {
    color: #eebe77 !important;
    border-color: rgba(230, 162, 60, 0.45) !important;
    background: rgba(230, 162, 60, 0.14) !important;
  }
}
</style>
