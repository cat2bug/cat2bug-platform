<template>
  <el-popover
    placement="bottom-start"
    v-model:visible="popoverVisible"
    :width="360"
    popper-class="project-select-popover"
    trigger="click"
    @show="popoverShowHandle"
    @hide="popoverHideHandle"
  >
    <template #reference>
      <div
        :class="triggerClass"
        class="project-select-trigger"
        role="button"
        tabindex="0"
      >
        <star-switch
          class="project-select-star"
          :model-value="!!currentProjectDisplay.collect"
          @change="onCurrentCollectChange"
          @click.stop
        />
        <span class="project-select-name" :title="displayName">{{ displayName }}</span>
        <legacy-icon icon="el-icon-arrow-down" class="project-select-caret" />
      </div>
    </template>

    <div class="project-select-panel">
      <h3 class="project-select-title">
        <img class="project-select-title-icon" src="@/assets/icons/qiehuan.png" alt="" />
        {{ $t('project.select') }}
      </h3>
      <div class="project-select-toolbar">
        <el-input
          v-model="keyword"
          clearable
          size="small"
          class="project-select-search"
          :prefix-icon="Search"
          :placeholder="$t('project.select-search-placeholder')"
          @input="onSearchInput"
          @clear="onSearchInput"
        />
        <el-button
          v-if="canAddProject"
          class="project-select-add"
          type="primary"
          plain
          size="small"
          :icon="Plus"
          :title="$t('project.create-project')"
          @click.stop="openCreateProject"
        />
      </div>
      <div ref="projectList" v-loading="loading" class="project-select-list">
        <div
          v-for="(item, index) in options"
          :key="item.projectId || index"
          class="project-select-item"
          :class="{
            'is-active': isCurrentProject(item),
            'is-keyboard-active': index === activeIndex
          }"
          @click="handleProjectChange(item)"
        >
          <star-switch
            class="project-select-item-star"
            :model-value="!!item.collect"
            @change="(val) => onItemCollectChange(val, item)"
            @click.stop
          />
          <div class="project-select-item-main">
            <span class="project-select-item-name" :title="item.projectName">{{ item.projectName || '—' }}</span>
            <span class="project-select-item-remark" :title="itemRemark(item)">{{ itemRemark(item) }}</span>
          </div>
        </div>
        <div v-if="!loading && !options.length" class="project-select-empty">
          {{ $t('project.select-empty') }}
        </div>
      </div>
      <template v-if="total > queryParams.pageSize">
        <el-divider class="project-select-pager-divider" />
        <el-pagination
          class="project-select-pager"
          size="small"
          layout="prev, next"
          :hide-on-single-page="true"
          v-model:current-page="queryParams.pageNum"
          :page-size="queryParams.pageSize"
          :total="total"
          @current-change="getProjectList"
        />
      </template>
    </div>
  </el-popover>
</template>

<script>
import { markRaw } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import StarSwitch from '@/components/StarSwitch'
import { collectProject, getProject, listProject } from '@/api/system/project'
import { myListTeam } from '@/api/system/team'
import { useUserStore } from '@/stores/user'
import { checkPermi } from '@/utils/permission'

export default {
  name: 'ProjectSelect',
  components: { StarSwitch, Plus },
  setup() {
    const userStore = useUserStore()
    return { userStore }
  },
  data() {
    return {
      Search: markRaw(Search),
      Plus: markRaw(Plus),
      popoverVisible: false,
      loading: false,
      options: [],
      currentProject: {},
      total: 0,
      activeIndex: -1,
      keyword: '',
      searchTimer: null,
      queryParams: {
        teamId: this.teamId,
        projectName: undefined,
        params: {
          userId: this.memberId
        },
        pageNum: 1,
        pageSize: 10
      }
    }
  },
  props: {
    teamId: {
      type: Number,
      default: 0
    },
    projectId: {
      type: Number,
      default: 0
    },
    memberId: {
      type: Number,
      default: 0
    },
    size: {
      type: String,
      default: 'default'
    }
  },
  computed: {
    currentProjectDisplay() {
      return this.currentProject && typeof this.currentProject === 'object' ? this.currentProject : {}
    },
    displayName() {
      return this.currentProjectDisplay.projectName || ''
    },
    canAddProject() {
      return checkPermi(['system:project:add'])
    },
    /** 与 Vue2 一致：保留键盘提示锚点类名 */
    triggerClass() {
      return [
        'project-select-kbd-hint-anchor'
      ]
    }
  },
  watch: {
    teamId: {
      immediate: true,
      handler() {
        this.queryParams.teamId = this.teamId
        this.getProjectList()
      }
    },
    memberId(val) {
      if (!this.queryParams.params) {
        this.queryParams.params = {}
      }
      this.queryParams.params.userId = val
      this.getProjectList()
    },
    projectId() {
      this.getCurrentProjectInfo()
    }
  },
  mounted() {
    this.getCurrentProjectInfo()
    this.getProjectList()
  },
  beforeUnmount() {
    window.clearTimeout(this.searchTimer)
    this.detachProjectSelectKeyboardListener()
  },
  methods: {
    /** 快捷键 / 外部调用：打开项目切换浮层 */
    openPopover() {
      return this.getProjectList().then(() => {
        this.popoverVisible = true
      })
    },
    attachProjectSelectKeyboardListener() {
      if (this.$_projectSelectKeydownBound) return
      this.$_projectSelectKeydownBound = true
      this.$_onProjectSelectKeydown = (e) => {
        if (!this.popoverVisible) return
        this.onProjectSelectKeydown(e)
      }
      document.addEventListener('keydown', this.$_onProjectSelectKeydown, true)
    },
    detachProjectSelectKeyboardListener() {
      if (!this.$_projectSelectKeydownBound) return
      this.$_projectSelectKeydownBound = false
      document.removeEventListener('keydown', this.$_onProjectSelectKeydown, true)
      this.$_onProjectSelectKeydown = null
    },
    isTypingInSearch(e) {
      const t = e && e.target
      if (!t || typeof t.closest !== 'function') return false
      return !!t.closest('.project-select-search')
    },
    onProjectSelectKeydown(e) {
      const key = e.key
      if (this.isTypingInSearch(e)) {
        if (key === 'Escape' || key === 'Esc') {
          if (e.metaKey || e.ctrlKey) return
          if (typeof e.getModifierState === 'function' &&
            (e.getModifierState('Meta') || e.getModifierState('Control'))) return
          e.preventDefault()
          e.stopPropagation()
          this.closePopover()
        }
        return
      }
      if (key === 'ArrowDown' || key === 'Down') {
        e.preventDefault()
        e.stopPropagation()
        this.moveActive(1)
        return
      }
      if (key === 'ArrowUp' || key === 'Up') {
        e.preventDefault()
        e.stopPropagation()
        this.moveActive(-1)
        return
      }
      if (key === 'ArrowLeft' || key === 'Left') {
        e.preventDefault()
        e.stopPropagation()
        this.movePage(-1)
        return
      }
      if (key === 'ArrowRight' || key === 'Right') {
        e.preventDefault()
        e.stopPropagation()
        this.movePage(1)
        return
      }
      if (key === 'Enter' || key === ' ' || key === 'Spacebar' || e.code === 'Space') {
        e.preventDefault()
        e.stopPropagation()
        this.selectActive()
        return
      }
      if (key === 'Escape' || key === 'Esc') {
        if (e.metaKey || e.ctrlKey) return
        if (typeof e.getModifierState === 'function' &&
          (e.getModifierState('Meta') || e.getModifierState('Control'))) return
        e.preventDefault()
        e.stopPropagation()
        this.closePopover()
      }
    },
    closePopover() {
      this.popoverVisible = false
    },
    hasPrevPage() {
      return this.queryParams.pageNum > 1
    },
    hasNextPage() {
      const pageSize = this.queryParams.pageSize || 10
      return this.queryParams.pageNum * pageSize < this.total
    },
    goPage(pageNum, activePos) {
      this.queryParams.pageNum = pageNum
      return this.getProjectList().then(() => {
        if (activePos === 'last') {
          this.activeIndex = Math.max(0, this.options.length - 1)
        } else {
          this.activeIndex = 0
        }
        this.$nextTick(() => this.scrollActiveIntoView())
      })
    },
    /** 键盘焦点优先落在当前项目，找不到则落在第一项 */
    indexOfCurrentOrFirst() {
      if (!this.options || !this.options.length) return -1
      const idx = this.options.findIndex((item) => this.isCurrentProject(item))
      return idx >= 0 ? idx : 0
    },
    movePage(dir) {
      if (dir < 0 && this.hasPrevPage()) {
        this.goPage(this.queryParams.pageNum - 1, 'last')
      } else if (dir > 0 && this.hasNextPage()) {
        this.goPage(this.queryParams.pageNum + 1, 0)
      }
    },
    moveActive(dir) {
      if (!this.options || !this.options.length) {
        if (dir > 0 && this.hasNextPage()) {
          this.goPage(this.queryParams.pageNum + 1, 0)
        } else if (dir < 0 && this.hasPrevPage()) {
          this.goPage(this.queryParams.pageNum - 1, 'last')
        } else if (dir < 0) {
          this.closePopover()
        }
        return
      }
      let i = this.activeIndex
      if (i < 0) i = 0
      i += dir
      if (i < 0) {
        if (this.hasPrevPage()) {
          this.goPage(this.queryParams.pageNum - 1, 'last')
        } else if (this.activeIndex <= 0) {
          this.closePopover()
        } else {
          this.activeIndex = 0
        }
        return
      }
      if (i > this.options.length - 1) {
        if (this.hasNextPage()) {
          this.goPage(this.queryParams.pageNum + 1, 0)
        } else {
          this.activeIndex = this.options.length - 1
        }
        return
      }
      this.activeIndex = i
      this.$nextTick(() => this.scrollActiveIntoView())
    },
    scrollActiveIntoView() {
      const root = this.$refs.projectList
      if (!root) return
      const items = root.querySelectorAll('.project-select-item')
      const el = items[this.activeIndex]
      if (el && typeof el.scrollIntoView === 'function') {
        el.scrollIntoView({ block: 'nearest' })
      }
    },
    selectActive() {
      if (this.activeIndex >= 0 && this.activeIndex < this.options.length) {
        this.handleProjectChange(this.options[this.activeIndex])
      }
    },
    popoverShowHandle() {
      this.keyword = ''
      this.queryParams.projectName = undefined
      this.queryParams.pageNum = 1
      this.activeIndex = -1
      this.getProjectList().then(() => {
        this.activeIndex = this.indexOfCurrentOrFirst()
        this.$nextTick(() => this.scrollActiveIntoView())
      })
      this.attachProjectSelectKeyboardListener()
    },
    popoverHideHandle() {
      this.keyword = ''
      this.queryParams.projectName = undefined
      this.activeIndex = -1
      this.detachProjectSelectKeyboardListener()
    },
    onSearchInput() {
      window.clearTimeout(this.searchTimer)
      this.searchTimer = window.setTimeout(() => {
        const kw = (this.keyword || '').trim()
        this.queryParams.projectName = kw || undefined
        this.queryParams.pageNum = 1
        this.getProjectList().then(() => {
          this.activeIndex = this.indexOfCurrentOrFirst()
        })
      }, 200)
    },
    itemRemark(item) {
      const intro = item && item.projectIntroduce != null ? String(item.projectIntroduce).trim() : ''
      if (intro) return intro
      const key = item && item.projectKey != null ? String(item.projectKey).trim() : ''
      return key || this.$t('project.introduction-not-filled')
    },
    isCurrentProject(item) {
      return item && Number(item.projectId) === Number(this.projectId)
    },
    getCurrentProjectInfo() {
      const projectId = parseInt(this.projectId, 10)
      if (!Number.isFinite(projectId) || projectId <= 0) {
        return
      }
      getProject(projectId).then(res => {
        if (res && res.data) {
          this.currentProject = res.data
        }
      })
    },
    applyProjectListResponse(res) {
      this.options = (res && res.rows) ? res.rows : []
      this.total = (res && res.total != null) ? res.total : this.options.length
      if (this.popoverVisible && this.activeIndex >= this.options.length) {
        this.activeIndex = Math.max(0, this.options.length - 1)
      }
    },
    resolveTeamIdFromMyList() {
      return myListTeam().then((res) => {
        const first = res && res.rows && res.rows[0]
        const tid = first && Number(first.teamId)
        return (Number.isFinite(tid) && tid > 0) ? tid : null
      }).catch(() => null)
    },
    resolveTeamIdFromConfig() {
      const direct = Number(this.teamId)
      if (Number.isFinite(direct) && direct > 0) {
        return Promise.resolve(direct)
      }
      const ctx = this.userStore.contextTeam
      if (ctx && ctx.teamId) {
        const tid = Number(ctx.teamId)
        if (Number.isFinite(tid) && tid > 0) {
          return Promise.resolve(tid)
        }
      }
      return this.resolveTeamIdFromMyList()
    },
    /** 优先当前项目所属团队，避免 config.currentTeamId 与当前项目不一致导致列表为空 */
    resolveEffectiveTeamId() {
      const projectId = Number(this.projectId)
      if (Number.isFinite(projectId) && projectId > 0) {
        const cached = this.currentProject && Number(this.currentProject.projectId) === projectId
          ? Number(this.currentProject.teamId)
          : NaN
        if (Number.isFinite(cached) && cached > 0) {
          return Promise.resolve(cached)
        }
        return getProject(projectId).then((res) => {
          const tid = res && res.data && Number(res.data.teamId)
          if (Number.isFinite(tid) && tid > 0) {
            return tid
          }
          return this.resolveTeamIdFromConfig()
        }).catch(() => this.resolveTeamIdFromConfig())
      }
      return this.resolveTeamIdFromConfig()
    },
    getProjectList() {
      if (this.memberId) {
        if (!this.queryParams.params) {
          this.queryParams.params = {}
        }
        this.queryParams.params.userId = this.memberId
      }
      this.loading = true
      return this.resolveEffectiveTeamId().then((teamId) => {
        if (!teamId) {
          this.options = []
          this.total = 0
          this.loading = false
          return
        }
        this.queryParams.teamId = teamId
        return listProject(this.queryParams).then((res) => {
          this.applyProjectListResponse(res)
          this.loading = false
        }).catch(() => {
          this.options = []
          this.total = 0
          this.loading = false
        })
      })
    },
    handleProjectChange(project) {
      this.detachProjectSelectKeyboardListener()
      if (!project || !project.projectId) return
      const currentPath = this.$route && this.$route.path
      this.popoverVisible = false
      this.userStore.SwitchCurrentProject(project.projectId, {
        rewriteUrl: true,
        currentPath
      }).then(() => {
        this.currentProject = project
      })
    },
    onCurrentCollectChange(collect) {
      this.setProjectCollect(this.currentProjectDisplay, collect)
    },
    onItemCollectChange(collect, item) {
      this.setProjectCollect(item, collect)
    },
    setProjectCollect(project, collect) {
      if (!project || !project.projectId) return
      const next = !!collect
      const prev = !!project.collect
      project.collect = next
      collectProject(project.projectId, project).then(() => {
        if (Number(project.projectId) === Number(this.currentProject.projectId)) {
          this.currentProject = { ...this.currentProject, collect: next }
        }
        if (next) {
          this.$message.success(this.$i18n.t('collect-success').toString())
        } else {
          this.$message.success(this.$i18n.t('cancel-success').toString())
        }
        this.getProjectList()
      }).catch(() => {
        project.collect = prev
      })
    },
    openCreateProject() {
      if (!this.canAddProject) return
      this.popoverVisible = false
      this.$router.push({ name: 'ProjectAdd' })
    }
  }
}
</script>

<style lang="scss" scoped>
:deep(.el-popover__reference),
:deep(.el-popover__reference-wrapper) {
  display: inline-flex;
  width: auto;
  max-width: 100%;
}

.project-select-trigger {
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

.project-select-star {
  flex-shrink: 0;
  font-size: 16px;
}

.project-select-name {
  flex: 0 1 auto;
  min-width: 0;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: inherit;
  font-weight: inherit;
  color: var(--text-color-primary);
}

.project-select-caret {
  flex-shrink: 0;
  font-size: 12px !important;
  width: 12px;
  height: 12px;
  line-height: 1;
  color: var(--text-color-regular, #606266);

  :deep(.el-icon),
  :deep(svg) {
    width: 12px !important;
    height: 12px !important;
    color: inherit;
  }
}

.project-select-panel {
  margin: -4px 0;
}

.project-select-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 5px 0 15px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.project-select-title-icon {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  object-fit: contain;
  display: block;
}

@at-root html.dark .project-select-title-icon {
  filter: invert(1);
}

.project-select-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.project-select-search {
  flex: 1 1 auto;
  min-width: 0;

  :deep(.el-input__wrapper) {
    height: var(--cat2bug-control-height, 32px) !important;
    min-height: var(--cat2bug-control-height, 32px) !important;
    box-sizing: border-box;
  }
}

.project-select-add {
  flex-shrink: 0;
  width: var(--cat2bug-control-height, 32px) !important;
  height: var(--cat2bug-control-height, 32px) !important;
  min-height: var(--cat2bug-control-height, 32px) !important;
  padding: 0 !important;
  box-sizing: border-box;
}

.project-select-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: hidden;
  min-height: 48px;
  max-height: 280px;
  overflow-y: auto;
}

.project-select-pager-divider {
  margin: 8px 0 6px;

  :deep(.el-divider--horizontal),
  &.el-divider--horizontal {
    border-top-color: var(--border-color-lighter, #f0f2f5);
  }
}

.project-select-pager {
  display: flex;
  justify-content: flex-end;
  margin: 0 0 2px;
  clear: both;
}

.project-select-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 6px;
  border-radius: 6px;
  cursor: pointer;

  &:hover,
  &.is-keyboard-active {
    background: var(--table-row-hover-bg, rgba(0, 0, 0, 0.04));
  }

  /* 当前项目：主色字重 + 浅底，与键盘高亮区分 */
  &.is-active {
    background: var(--table-row-hover-bg, rgba(0, 0, 0, 0.04));

    .project-select-item-name {
      font-weight: 600;
      color: var(--cat2bug-primary, var(--el-color-primary, #1890ff));
    }
  }

  &.is-active.is-keyboard-active {
    background: var(--el-color-primary-light-9, rgba(24, 144, 255, 0.08));
  }
}

.project-select-item-star {
  flex-shrink: 0;
  font-size: 15px;
}

.project-select-item-main {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.project-select-item-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 400;
  color: var(--text-color-primary);
}

.project-select-item-remark {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--text-color-secondary);
}

.project-select-empty {
  padding: 16px 8px;
  text-align: center;
  font-size: 13px;
  color: var(--text-color-placeholder);
}
</style>

<style lang="scss">
/* popover 挂载到 body：固定宽度，与源码库下拉一致 */
.project-select-popover.el-popover {
  width: 360px !important;
  max-width: min(360px, 50vw) !important;
  min-width: 0;
  box-sizing: border-box;
}
</style>
