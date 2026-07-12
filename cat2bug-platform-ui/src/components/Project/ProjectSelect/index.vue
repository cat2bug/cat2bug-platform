<template>
  <el-popover
    placement="bottom-start"
    v-model:visible="popoverVisible"
    @show="popoverShowHandle"
    @hide="popoverHideHandle"
    trigger="hover">
    <template #reference>
      <div :class="triggerClass">
        <legacy-icon icon="el-icon-star-on" class="yellow start-switch project-select-star" :collect="currentProjectDisplay.collect ? 'true' : 'false'" @click.stop="handleProjectCollect(currentProjectDisplay, $event)"></legacy-icon>
        <p class="prefix-project-name">{{ currentProjectDisplay.projectName || '' }}</p>
        <legacy-icon icon="el-icon-arrow-down" class="project-select-caret" />
      </div>
    </template>
    <h3 class="title"><legacy-icon icon="el-icon-refresh" /> {{ $t('project.select') }}</h3>
    <el-divider></el-divider>
    <div ref="projectList" class="project-select-list">
      <div
        v-for="(item,index) in options"
        :key="index"
        class="col click item"
        :class="{ 'is-keyboard-active': index === activeIndex }"
        @click="handleProjectChange(item)"
      >
        <div class="row">
          <legacy-icon icon="el-icon-star-on" class="yellow start-switch" :collect="item.collect?'true':'false'" @click="handleProjectCollect(item, $event)"></legacy-icon>
<!--        <el-image :src="item.projectIcon" />-->
          <p class="prefix-project-name">{{item.projectName}}</p>
        </div>
      </div>
    </div>
    <el-divider v-if="total>queryParams.pageSize"></el-divider>
    <el-pagination
      style="float: right;"
      size="small"
      :hide-on-single-page="true"
      layout="prev, next"
      v-model:current-page="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      @current-change="getProjectList">
    </el-pagination>
  </el-popover>
</template>

<script>
import {collectProject, getProject, listProject} from "@/api/system/project";
import { listProject as listAdminProject } from "@/api/admin/project";
import { myListTeam } from "@/api/system/team";
import store from "@/store";
import Label from "@/components/Cat2BugStatistic/Components/Label";

export default {
  name: "ProjectSelect",
  components: {Label},
  data() {
    return {
      popoverVisible: false,
      options: [],
      currentProject: {},
      total: 0,
      activeIndex: -1,
      queryParams: {
        teamId: this.teamId,
        params: {
          userId: this.memberId,
        },
        pageNum: 1,
        pageSize: 10
      }
    }
  },
  props: {
    teamId : {
      type: Number,
      default: 0
    },
    projectId : {
      type: Number,
      default: 0
    },
    memberId : {
      type: Number,
      default: 0
    },
    size: {
      type: String,
      default: 'default'
    },
  },
  computed: {
    currentProjectDisplay() {
      return this.currentProject && typeof this.currentProject === 'object' ? this.currentProject : {}
    },
    /** 与 Vue2 一致：仅 size 后缀类名，不用 select-project-member-input 外框 */
    triggerClass() {
      return [
        'between-row-1',
        'click',
        'project-select-trigger',
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
    projectId(val) {
      this.getCurrentProjectInfo()
    }
  },
  created() {
  },
  mounted() {
    this.getCurrentProjectInfo();
  },
  beforeDestroy() {
    this.detachProjectSelectKeyboardListener();
  },
  methods: {
    /** 快捷键 / 外部调用：打开项目切换浮层 */
    openPopover() {
      return this.getProjectList().then(() => {
        this.popoverVisible = true;
      });
    },
    attachProjectSelectKeyboardListener() {
      if (this.$_projectSelectKeydownBound) return;
      this.$_projectSelectKeydownBound = true;
      this.$_onProjectSelectKeydown = (e) => {
        if (!this.popoverVisible) return;
        this.onProjectSelectKeydown(e);
      };
      document.addEventListener('keydown', this.$_onProjectSelectKeydown, true);
    },
    detachProjectSelectKeyboardListener() {
      if (!this.$_projectSelectKeydownBound) return;
      this.$_projectSelectKeydownBound = false;
      document.removeEventListener('keydown', this.$_onProjectSelectKeydown, true);
      this.$_onProjectSelectKeydown = null;
    },
    onProjectSelectKeydown(e) {
      const key = e.key;
      if (key === 'ArrowDown' || key === 'Down') {
        e.preventDefault();
        e.stopPropagation();
        this.moveActive(1);
        return;
      }
      if (key === 'ArrowUp' || key === 'Up') {
        e.preventDefault();
        e.stopPropagation();
        this.moveActive(-1);
        return;
      }
      if (key === 'ArrowLeft' || key === 'Left') {
        e.preventDefault();
        e.stopPropagation();
        this.movePage(-1);
        return;
      }
      if (key === 'ArrowRight' || key === 'Right') {
        e.preventDefault();
        e.stopPropagation();
        this.movePage(1);
        return;
      }
      if (key === 'Enter' || key === ' ' || key === 'Spacebar' || e.code === 'Space') {
        e.preventDefault();
        e.stopPropagation();
        this.selectActive();
        return;
      }
      if (key === 'Escape' || key === 'Esc') {
        if (e.metaKey || e.ctrlKey) return;
        if (typeof e.getModifierState === 'function' &&
          (e.getModifierState('Meta') || e.getModifierState('Control'))) return;
        e.preventDefault();
        e.stopPropagation();
        this.closePopover();
      }
    },
    closePopover() {
      this.popoverVisible = false;
    },
    hasPrevPage() {
      return this.queryParams.pageNum > 1;
    },
    hasNextPage() {
      const pageSize = this.queryParams.pageSize || 10;
      return this.queryParams.pageNum * pageSize < this.total;
    },
    goPage(pageNum, activePos) {
      this.queryParams.pageNum = pageNum;
      return this.getProjectList().then(() => {
        if (activePos === 'last') {
          this.activeIndex = Math.max(0, this.options.length - 1);
        } else {
          this.activeIndex = 0;
        }
        this.$nextTick(() => this.scrollActiveIntoView());
      });
    },
    movePage(dir) {
      if (dir < 0 && this.hasPrevPage()) {
        this.goPage(this.queryParams.pageNum - 1, 'last');
      } else if (dir > 0 && this.hasNextPage()) {
        this.goPage(this.queryParams.pageNum + 1, 0);
      }
    },
    moveActive(dir) {
      if (!this.options || !this.options.length) {
        if (dir > 0 && this.hasNextPage()) {
          this.goPage(this.queryParams.pageNum + 1, 0);
        } else if (dir < 0 && this.hasPrevPage()) {
          this.goPage(this.queryParams.pageNum - 1, 'last');
        } else if (dir < 0) {
          this.closePopover();
        }
        return;
      }
      let i = this.activeIndex;
      if (i < 0) i = 0;
      i += dir;
      if (i < 0) {
        if (this.hasPrevPage()) {
          this.goPage(this.queryParams.pageNum - 1, 'last');
        } else if (this.activeIndex <= 0) {
          this.closePopover();
        } else {
          this.activeIndex = 0;
        }
        return;
      }
      if (i > this.options.length - 1) {
        if (this.hasNextPage()) {
          this.goPage(this.queryParams.pageNum + 1, 0);
        } else {
          this.activeIndex = this.options.length - 1;
        }
        return;
      }
      this.activeIndex = i;
      this.$nextTick(() => this.scrollActiveIntoView());
    },
    scrollActiveIntoView() {
      const root = this.$refs.projectList;
      if (!root) return;
      const items = root.querySelectorAll('.item');
      const el = items[this.activeIndex];
      if (el && typeof el.scrollIntoView === 'function') {
        el.scrollIntoView({ block: 'nearest' });
      }
    },
    selectActive() {
      if (this.activeIndex >= 0 && this.activeIndex < this.options.length) {
        this.handleProjectChange(this.options[this.activeIndex]);
      }
    },
    /** 弹窗显示事件 */
    popoverShowHandle() {
      this.activeIndex = 0;
      this.getProjectList().then(() => {
        this.$nextTick(() => this.scrollActiveIntoView());
      });
      this.attachProjectSelectKeyboardListener();
    },
    /** 弹窗隐藏事件 */
    popoverHideHandle() {
      this.activeIndex = -1;
      this.detachProjectSelectKeyboardListener();
    },
    /** 获取当前项目信息 */
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
    canUseAdminProjectList() {
      const perms = store.state.user.permissions
      return Array.isArray(perms) && (perms.includes('*:*:*') || perms.includes('admin:project:list'))
    },
    resolveTeamIdFromMyList() {
      return myListTeam().then((res) => {
        const first = res && res.rows && res.rows[0]
        const tid = first && Number(first.teamId)
        return (Number.isFinite(tid) && tid > 0) ? tid : null
      }).catch(() => null)
    },
    /** currentTeamId 为空时，从当前项目 / 上下文团队 / 我的团队推断 teamId */
    resolveEffectiveTeamId() {
      const direct = Number(this.teamId)
      if (Number.isFinite(direct) && direct > 0) {
        return Promise.resolve(direct)
      }
      const ctx = store.state.user.contextTeam
      if (ctx && ctx.teamId) {
        const tid = Number(ctx.teamId)
        if (Number.isFinite(tid) && tid > 0) {
          return Promise.resolve(tid)
        }
      }
      const projectId = Number(this.projectId)
      if (Number.isFinite(projectId) && projectId > 0) {
        return getProject(projectId).then((res) => {
          const tid = res && res.data && Number(res.data.teamId)
          if (Number.isFinite(tid) && tid > 0) {
            return tid
          }
          return this.resolveTeamIdFromMyList()
        }).catch(() => this.resolveTeamIdFromMyList())
      }
      return this.resolveTeamIdFromMyList()
    },
    tryAdminProjectList() {
      if (!this.canUseAdminProjectList()) {
        this.options = []
        this.total = 0
        return Promise.resolve()
      }
      return listAdminProject({
        pageNum: this.queryParams.pageNum,
        pageSize: this.queryParams.pageSize,
      }).then((res) => {
        this.applyProjectListResponse(res)
      }).catch(() => {
        this.options = []
        this.total = 0
      })
    },
    /** 查询收藏的项目列表 */
    getProjectList() {
      if (this.memberId) {
        if (!this.queryParams.params) {
          this.queryParams.params = {}
        }
        this.queryParams.params.userId = this.memberId
      }
      return this.resolveEffectiveTeamId().then((teamId) => {
        if (!teamId) {
          return this.tryAdminProjectList()
        }
        this.queryParams.teamId = teamId
        return listProject(this.queryParams).then((res) => {
          if (res && res.rows && res.rows.length) {
            this.applyProjectListResponse(res)
            return
          }
          return this.tryAdminProjectList()
        }).catch(() => this.tryAdminProjectList())
      })
    },
    /** 选择项目 */
    handleProjectChange(project) {
      this.detachProjectSelectKeyboardListener();
      store.dispatch('UpdateCurrentProjectId', project.projectId).then(() => {
        store.dispatch('GetInfo').then(() => {
          // 设置缺陷列表显示哪些列属性
          window.location.reload();
        });
      });
    },
    /** 设置当前项目是否收藏 */
    handleProjectCollect(project, event) {
      if (!project || !project.projectId) {
        return
      }
      project.collect = !project.collect
      // 保存收藏状态
      collectProject(project.projectId, project).then(res=>{
        if(project.projectId == this.currentProject.projectId) {
          this.currentProject = { ...this.currentProject, collect: project.collect }
        }
        if(project.collect) {
          this.$message.success(this.$i18n.t('collect-success').toString());
        } else {
          this.$message.success(this.$i18n.t('cancel-success').toString());
        }
        // 重新获取项目列表
        this.getProjectList();
      });
      if(event) {
        event.stopPropagation();
      }
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

.el-divider {
  background-color: var(--border-color-light);
  margin: 10px 0px;
}
.project-select-list {
  max-height: 280px;
  overflow-y: auto;
}
.between-row-1 {
  display: inline-flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: space-between;
  align-items: center;
  gap: 5px;
  max-width: 100%;
  .prefix-project-name {
    flex: 1 1 auto;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .start-switch {
    flex-shrink: 0;
  }
}

.project-select-trigger.between-row-1 {
  border: none !important;
  background: transparent !important;
  box-shadow: none !important;
  outline: none !important;

  &:focus,
  &:focus-within,
  &:focus-visible {
    border: none !important;
    box-shadow: none !important;
    outline: none !important;
  }
}

.project-select-caret {
  flex-shrink: 0;
  font-size: 10px !important;
  width: 10px;
  height: 10px;
  line-height: 1;
  color: var(--cat2bug-input-icon-color, #909399);

  :deep(svg) {
    width: 10px !important;
    height: 10px !important;
  }
}
  .prefix-project-name {
    margin: 5px 0px;
  }
.row {
  display: inline-flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: center;
  gap: 5px;
  max-width: 100%;
  .prefix-project-name {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
.col {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}
.yellow {
  color: #f7ba2a;
}
.block {
  display: inline-block;
}
.click {
  cursor: pointer;
  padding: 2px 10px;
  border-radius: 3px;
}

.project-select-trigger.click:hover {
  background-color: #e8f4ff;
}
.item.click {
  padding: 2px 10px;
  border-radius: 3px;
}
.item.click:hover,
.item.is-keyboard-active {
  background-color: #e8f4ff;
}
.item {
  font-size: 1rem;
}
.start-switch[collect="true"] {
  color: var(--star-switch-on-color, rgb(247, 186, 42));
  -webkit-text-stroke: 3px transparent;
  background: var(--star-switch-on-fill, rgb(247, 186, 42)) top left / 100% 100%;
  -webkit-background-clip: text;
}
.start-switch {
  color: var(--star-switch-off-color, #ffffff);
  -webkit-text-stroke: 3px transparent;
  background: var(--star-switch-off-fill, rgb(198, 209, 222)) top left / 100% 100%;
  -webkit-background-clip: text;
}
.title {
  margin: 5px 5px 15px 10px;
}
.el-image {
  width: 30px;
  height: 30px;
}

@at-root html.dark .project-select-trigger.click:hover {
  background-color: var(--table-row-hover-bg) !important;
}

@at-root html.dark .project-select-list .item.is-keyboard-active {
  background-color: rgba(255, 193, 7, 0.12);
}
</style>
