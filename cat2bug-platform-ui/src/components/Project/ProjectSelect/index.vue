<template>
  <el-popover
    placement="bottom-start"
    v-model="popoverVisible"
    @show="popoverShowHandle"
    @hide="popoverHideHandle"
    trigger="hover">
    <div slot="reference" :class="['between-row-1', 'click', 'project-select-kbd-hint-anchor', 'select-project-member-input-' + size]">
      <i class="yellow el-icon-star-on start-switch" :collect="currentProject.collect?'true':'false'" @click="handleProjectCollect(currentProject)"></i>
<!--      <el-image :src="currentProject.projectIcon" />-->
      <p class="prefix-project-name">{{currentProject.projectName}}</p>
      <i class="el-icon-arrow-down" />
    </div>
    <h3 class="title"><i class="el-icon-refresh"/> {{ $t('project.select') }}</h3>
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
          <i class="yellow el-icon-star-on start-switch" :collect="item.collect?'true':'false'" @click="handleProjectCollect(item, $event)"></i>
<!--        <el-image :src="item.projectIcon" />-->
          <p class="prefix-project-name">{{item.projectName}}</p>
        </div>
      </div>
    </div>
    <el-divider v-if="total>queryParams.pageSize"></el-divider>
    <el-pagination
      style="float: right;"
      small
      :hide-on-single-page="true"
      layout="prev, next"
      :current-page.sync="queryParams.pageNum"
      :page-size.sync="queryParams.pageSize"
      :total="total"
      @current-change="getProjectList">
    </el-pagination>
  </el-popover>
</template>

<script>
import {collectProject, getProject, listProject} from "@/api/system/project";
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
  created() {
  },
  mounted() {
    this.getCurrentProjectInfo();
    this.getProjectList();
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
      getProject(this.projectId).then(res=>{
        this.currentProject = res.data;
      })
    },
    /** 查询收藏的项目列表 */
    getProjectList() {
      return listProject(this.queryParams).then(res => {
        this.options = res.rows;
        this.total = res.total;
        if (this.popoverVisible && this.activeIndex >= this.options.length) {
          this.activeIndex = Math.max(0, this.options.length - 1);
        }
      });
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
      this.$set(project, 'collect', !project.collect);
      // 保存收藏状态
      collectProject(project.projectId, project).then(res=>{
        if(project.projectId == this.currentProject.projectId) {
          this.$set(this.currentProject, 'collect', project.collect);
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
  .el-icon-arrow-down {
    flex-shrink: 0;
    color: #909399;
  }
  .start-switch {
    flex-shrink: 0;
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
.click:hover,
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

@at-root html.dark .project-select-list .item.is-keyboard-active {
  background-color: rgba(255, 193, 7, 0.12);
}
</style>
