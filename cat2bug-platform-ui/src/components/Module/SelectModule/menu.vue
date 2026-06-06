<template>
  <div class="module-menu-column">
    <div class="module-menu-scroll-region">
      <button
        v-show="scrollHasUp"
        type="button"
        class="module-menu-scroll-btn module-menu-scroll-up"
        :title="$t('module.scroll-up')"
        :aria-label="$t('module.scroll-up')"
        @click.stop="scrollMenuStep(-1)">
        <i class="el-icon-arrow-up"></i>
      </button>
      <div ref="menuScroller" class="module-menu-scroller" @scroll="syncVerticalScrollIndicators">
        <el-row class="module-menu">
          <el-col v-if="loading" class="module-menu-item module-menu-loading" :span="24">
            <div v-loading="loading" style="width: 100%;height: 200px;"></div>
          </el-col>
          <el-col
            class="module-menu-item module-menu-row"
            :span="24"
            v-for="(module,index) in moduleList"
            :key="module.moduleId"
            :class="{
              'is-keyboard-active': keyboardActiveIndex === index && keyboardActivePart === 'main',
              'is-path-active': isPathActiveRow(module)
            }"
            @click.native="showSubMenuHandle(module)">
            <el-tooltip
              :content="module.moduleName"
              placement="top"
              :open-delay="400"
              :disabled="!labelNeedsTooltip(module.moduleName)">
              <span class="module-menu-row-label">{{ formatMenuLabel(module.moduleName) }}</span>
            </el-tooltip>
            <i v-if="moduleHasChildren(module)" class="el-icon-arrow-right module-menu-row-arrow"></i>
            <el-button
              v-else-if="isEdit"
              type="text"
              size="mini"
              class="module-menu-row-side-add"
              :class="{'is-keyboard-active': keyboardActiveIndex === index && keyboardActivePart === 'side'}"
              @click="addSubMenuHandle($event, module)"><i class="el-icon-plus"></i></el-button>
          </el-col>
        </el-row>
      </div>
      <button
        v-show="scrollHasDown"
        type="button"
        class="module-menu-scroll-btn module-menu-scroll-down"
        :title="$t('module.scroll-down')"
        :aria-label="$t('module.scroll-down')"
        @click.stop="scrollMenuStep(1)">
        <i class="el-icon-arrow-down"></i>
      </button>
    </div>
    <div v-if="isEdit" class="module-menu-add-footer">
      <div class="module-menu-footer-divider" aria-hidden="true"></div>
      <div
        class="module-menu-add-wrap"
        :class="{'is-keyboard-active': keyboardActiveIndex === moduleList.length && keyboardActivePart === 'new'}">
        <add-module-menu-item
          ref="addModuleMenu"
          v-model="moduleName"
          :project-id="projectId"
          :module-pid="modulePid"
          @added="addModuleHandle"
          @input-blur="$emit('addInputBlur')"
          @input-escape="$emit('addInputEscape')" />
      </div>
    </div>
  </div>
</template>

<script>
import AddModuleMenuItem from "@/components/Module/SelectModule/add";
import {listModule} from "@/api/system/module";
import {
  estimateCharsForPixelWidth,
  formatDeliverableMenuLabel
} from '@/utils/deliverable-path-display';

export default {
  name: "ModuleMenu",
  components: { AddModuleMenuItem },
  data() {
    return {
      loading: false,
      moduleName: null,
      moduleList: [],
      scrollHasUp: false,
      scrollHasDown: false,
      /** 菜单列宽换算的可显示字符数 */
      labelMaxLength: 16,
      params: {
        pageIndex: 1,
          pageSize: 9999,
          modulePid: 0,
          projectId: null
      },
    }
  },
  props: {
    projectId: {
      type: Number,
      default: null
    },
    modulePid: {
      type: Number,
      default: 0
    },
    isEdit: {
      type: Boolean,
      default: true
    },
    /** 键盘上下导航时高亮的行索引（-1 表示无；等于 moduleList.length 时为底部新建行） */
    keyboardActiveIndex: {
      type: Number,
      default: -1
    },
    /** 当前行键盘焦点部分：main | side | new */
    keyboardActivePart: {
      type: String,
      default: 'main'
    },
    /** 父级按可视宽度压缩的非末列 label 上限（null 表示仅按列宽） */
    labelMaxLengthOverride: {
      type: Number,
      default: null
    },
    /** 选中路径上本列应高亮的父级 moduleId */
    pathHighlightModuleId: {
      type: Number,
      default: null
    }
  },
  computed: {
    getCurrentProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    effectiveLabelMaxLength() {
      if (this.labelMaxLengthOverride != null && this.labelMaxLengthOverride > 0) {
        return Math.min(this.labelMaxLength, this.labelMaxLengthOverride);
      }
      return this.labelMaxLength;
    }
  },
  created() {
    this.getModuleList();
  },
  mounted() {
    this.$nextTick(() => {
      this.updateLabelMaxLength();
      this.attachMenuScrollerListener();
      this.syncVerticalScrollIndicators();
    });
  },
  updated() {
    this.updateLabelMaxLength();
    this.$nextTick(() => this.syncVerticalScrollIndicators());
  },
  beforeDestroy() {
    if (this._scrollIndicatorRaf) {
      cancelAnimationFrame(this._scrollIndicatorRaf);
      this._scrollIndicatorRaf = null;
    }
    this.detachMenuScrollerListener();
  },
  watch: {
    keyboardActiveIndex(val) {
      if (this.keyboardActivePart !== 'main') return;
      if (val === 0) {
        this.$nextTick(() => this.resetMenuScrollToTop());
        return;
      }
      const lastMainIdx = (this.moduleList || []).length - 1;
      if (lastMainIdx >= 0 && val === lastMainIdx) {
        this.$nextTick(() => this.resetMenuScrollToBottom());
      }
    }
  },
  methods: {
    attachMenuScrollerListener() {
      const scroller = this.$refs.menuScroller;
      if (!scroller || this._menuVerticalScrollBound) return;
      this._onMenuVerticalScroll = () => this.syncVerticalScrollIndicators();
      scroller.addEventListener('scroll', this._onMenuVerticalScroll, { passive: true });
      this._menuVerticalScrollBound = true;
    },
    detachMenuScrollerListener() {
      const scroller = this.$refs.menuScroller;
      if (scroller && this._onMenuVerticalScroll) {
        scroller.removeEventListener('scroll', this._onMenuVerticalScroll);
      }
      this._menuVerticalScrollBound = false;
      this._onMenuVerticalScroll = null;
    },
    syncColumnScrollIndicators() {
      this.syncVerticalScrollIndicators();
    },
    syncVerticalScrollIndicators() {
      const scroller = this.$refs.menuScroller;
      if (!scroller) {
        this.scrollHasUp = false;
        this.scrollHasDown = false;
        return;
      }
      const apply = () => {
        const scrollTop = Math.max(0, scroller.scrollTop);
        const clientHeight = scroller.clientHeight;
        const scrollHeight = scroller.scrollHeight;
        const maxScroll = Math.max(0, scrollHeight - clientHeight);
        const edge = 2;
        const hasOverflow = maxScroll > edge;
        const atTop = scrollTop <= edge;
        const atBottom = Math.ceil(scrollTop + clientHeight) >= scrollHeight - edge;
        this.scrollHasUp = hasOverflow && !atTop;
        this.scrollHasDown = hasOverflow && !atBottom;
      };
      apply();
      // 上下箭头显隐会改变 scroller 高度，连续两帧再量一次避免边界仍显示箭头
      if (this._scrollIndicatorRaf) {
        cancelAnimationFrame(this._scrollIndicatorRaf);
      }
      this._scrollIndicatorRaf = requestAnimationFrame(() => {
        apply();
        this._scrollIndicatorRaf = requestAnimationFrame(() => {
          this._scrollIndicatorRaf = null;
          apply();
        });
      });
    },
    scrollMenuStep(dir) {
      const scroller = this.$refs.menuScroller;
      if (!scroller) return;
      const step = Math.max(120, Math.floor(scroller.clientHeight * 0.72));
      const maxTop = Math.max(0, scroller.scrollHeight - scroller.clientHeight);
      let nextTop = Math.max(0, Math.min(scroller.scrollTop + dir * step, maxTop));
      if (dir < 0 && nextTop <= 1) {
        nextTop = 0;
      }
      if (dir > 0 && maxTop > 0 && nextTop >= maxTop - 1) {
        nextTop = maxTop;
      }
      scroller.scrollTop = nextTop;
      this.syncVerticalScrollIndicators();
    },
    resetMenuScrollToTop() {
      const scroller = this.$refs.menuScroller;
      if (!scroller) return;
      scroller.scrollTop = 0;
      this.syncVerticalScrollIndicators();
    },
    resetMenuScrollToBottom() {
      const scroller = this.$refs.menuScroller;
      if (!scroller) return;
      const maxTop = Math.max(0, scroller.scrollHeight - scroller.clientHeight);
      scroller.scrollTop = maxTop;
      this.syncVerticalScrollIndicators();
    },
    updateLabelMaxLength() {
      const root = this.$el;
      if (!root) return;
      const colWidth = root.clientWidth || 200;
      const textPx = Math.max(48, colWidth - 40);
      const next = estimateCharsForPixelWidth(textPx);
      if (next !== this.labelMaxLength) {
        this.labelMaxLength = next;
      }
    },
    formatMenuLabel(name) {
      return formatDeliverableMenuLabel(name, this.effectiveLabelMaxLength);
    },
    labelNeedsTooltip(name) {
      const full = (name == null ? '' : String(name)).trim();
      return full && this.formatMenuLabel(name) !== full;
    },
    isPathActiveRow(module) {
      if (this.pathHighlightModuleId == null || !module || module.moduleId == null) {
        return false;
      }
      return Number(module.moduleId) === Number(this.pathHighlightModuleId);
    },
    getModuleList() {
      this.params.projectId=this.projectId||this.getCurrentProjectId;
      this.params.modulePid=this.modulePid;
      this.loading = true;
      return listModule(this.params).then(res=>{
        this.loading = false;
        this.moduleList = res.data;
        if (this.$refs.addModuleMenu) {
          this.$refs.addModuleMenu.setFormVisible(this.moduleList.length==0);
        }
        this.$nextTick(() => {
          this.$emit('column-ready');
          this.syncVerticalScrollIndicators();
        });
        return res.data;
      }).catch(e=>{
        this.loading = false;
        this.$nextTick(() => {
          this.$emit('column-ready');
          this.syncVerticalScrollIndicators();
        });
        return [];
      })
    },
    addModuleHandle(payload) {
      const info = (payload && typeof payload === 'object')
        ? payload
        : { moduleName: payload };
      this.getModuleList().then(() => {
        this.$nextTick(() => this.$emit('module-added', info));
      });
    },
    /** 新建子级成功后，将父级 childrenCount +1，使 + 变为展开箭头 */
    bumpModuleChildrenCount(moduleId) {
      if (moduleId == null) return;
      const list = this.moduleList || [];
      const idx = list.findIndex((m) => Number(m.moduleId) === Number(moduleId));
      if (idx < 0) return;
      const row = list[idx];
      const next = (Number(row.childrenCount) || 0) + 1;
      this.$set(row, 'childrenCount', next);
    },
    open(projectId,modulePid) {
      if(projectId){
        this.params.projectId=projectId;
      }
      if(modulePid){
        this.params.modulePid=modulePid;
      }
      this.getModuleList();
    },
    moduleHasChildren (module) {
      const c = module && module.childrenCount
      return c != null && Number(c) > 0
    },
    showSubMenuHandle(module) {
      if (this.moduleHasChildren(module)) {
        this.$emit('clickDirectory', module);
      } else {
        this.$emit('clickMenu', module);
      }
    },
    addSubMenuHandle(event, module) {
      this.$emit('clickAddSubMenu', module);
      event.stopPropagation();
    }
  }
}
</script>

<style lang="scss" scoped>
  .module-menu-column {
    display: flex;
    flex-direction: column;
    width: 200px;
    min-width: 200px;
    max-width: 200px;
    max-height: min(80vh, 520px);
    flex-shrink: 0;
    box-sizing: border-box;
    padding-left: 10px;
    padding-right: 10px;
  }
  .module-menu-scroll-region {
    display: flex;
    flex-direction: column;
    flex: 1;
    min-height: 0;
  }
  .module-menu-scroller {
    flex: 1;
    min-height: 0;
    overflow-x: hidden;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
    box-sizing: border-box;
  }
  .module-menu-scroll-btn {
    flex-shrink: 0;
    width: 100%;
    height: 26px;
    margin: 0;
    padding: 0;
    border: none;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-color-secondary, #909399);
    &:hover {
      color: var(--cat2bug-primary, #409eff);
    }
    .el-icon-arrow-up,
    .el-icon-arrow-down {
      font-size: 14px;
      font-weight: bold;
    }
  }
  .module-menu-scroll-up {
    border-radius: 4px 4px 0 0;
    background: linear-gradient(to bottom, #fff 55%, rgba(255, 255, 255, 0));
    @at-root html.dark & {
      background: linear-gradient(to bottom, #252529 55%, rgba(37, 37, 41, 0));
    }
  }
  .module-menu-scroll-down {
    border-radius: 0;
    background: linear-gradient(to top, #fff 55%, rgba(255, 255, 255, 0));
    @at-root html.dark & {
      background: linear-gradient(to top, #252529 55%, rgba(37, 37, 41, 0));
    }
  }
  .module-menu-add-footer {
    flex-shrink: 0;
    width: 100%;
    box-sizing: border-box;
    padding-top: 10px;
  }
  .module-menu-footer-divider {
    height: 0;
    border-top: 1px solid var(--border-color-light, #e4e7ed);
    margin: 0 0 10px;
  }
  .module-menu {
    width: 100%;
    min-width: 0;
    max-width: 100%;
    max-height: none;
    overflow: visible;
    margin-left: 0 !important;
    margin-right: 0 !important;
    > .el-col {
      float: none;
    }
    > .el-col.module-menu-item.module-menu-row {
      padding: 5px 10px !important;
    }
    > .el-col.module-menu-loading {
      padding: 0 !important;
    }
  }
  .module-menu-add-wrap {
    margin-top: 0;
    padding: 0;
    box-sizing: border-box;
    min-width: 0;
    width: 100%;
    overflow: visible;
    flex-shrink: 0;
  }
  .module-menu-item.module-menu-row {
    min-height: 0;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 4px;
    box-sizing: border-box;
    ::v-deep .el-tooltip {
      flex: 1;
      min-width: 0;
      overflow: hidden;
    }
  }
  .module-menu-loading {
    padding: 0;
  }
  .module-menu-item:not(.module-menu-row) {
    padding: 0;
  }
  .module-menu-row {
    min-width: 0;
    overflow: hidden;
    &.is-path-active {
      padding: 5px 10px !important;
      background-color: #F0F7FF;
      border-radius: 5px;
      @at-root html.dark & {
        background-color: #2e2e33;
      }
    }
    &.is-keyboard-active {
      padding: 5px 10px !important;
      background-color: #ECF5FF;
      border-radius: 5px;
      @at-root html.dark & {
        background-color: #3a3a42;
      }
    }
  }
  .module-menu-row-label {
    flex: 1;
    min-width: 0;
    max-width: 100%;
    overflow: hidden;
    white-space: nowrap;
    border-radius: 5px;
    padding: 0;
    margin: 0;
    line-height: 1.4;
    box-sizing: border-box;
    &:focus {
      outline: none;
    }
  }
  .module-menu-row-arrow {
    flex-shrink: 0;
  }
  .module-menu-row-side-add {
    flex-shrink: 0;
    margin: 0;
    padding: 4px 6px;
    color: var(--text-color-secondary, #909399);
  }
  .module-menu-row-side-add:hover {
    color: var(--cat2bug-primary, #409eff);
  }
  .module-menu-item.module-menu-row:hover {
    padding: 5px 10px !important;
    background-color: #F2F6FC;
    border-radius: 5px;
    cursor: pointer;
    @at-root html.dark & {
      background-color: #3a3a42;
    }
  }
  .module-menu-row-side-add.is-keyboard-active {
    padding: 5px 10px;
    background-color: #ECF5FF;
    border-radius: 5px;
    @at-root html.dark & {
      background-color: #3a3a42;
    }
  }
  .module-menu-add-wrap.is-keyboard-active ::v-deep .select-module-add-button {
    background-color: #ECF5FF;
    border-radius: 5px;
    @at-root html.dark & {
      background-color: #3a3a42;
    }
  }
</style>
