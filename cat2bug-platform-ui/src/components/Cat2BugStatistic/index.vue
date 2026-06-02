<template>
  <div
    class="statistic-panel"
    :class="{ 'is-scrollable': scrollOverflow && !wrap, 'is-wrap': wrap }"
    :style="emptyHiddenStyle"
  >
    <div class="statistic-scroll-wrap">
      <button
        v-show="scrollOverflow"
        type="button"
        class="statistic-scroll-btn statistic-scroll-btn--prev"
        :disabled="!canScrollLeft"
        @click="scrollPrev"
      >
        <i class="el-icon-arrow-left" />
      </button>
      <div ref="viewport" class="statistic-scroll-viewport" @scroll="updateScrollState">
      <draggable
        v-if="draggable"
        tag="div"
        v-model="list"
        class="statistic-tools"
        @change="updateStatisticPanel"
      >
        <div
          v-for="(sc,index) in list"
          :key="sc.name + '_' + index"
          class="statistic-item"
          :class="'statistic-item--' + sc.name"
        >
          <component
            :is="sc.name"
            ref="statisticItem"
            :read="read"
            :tools="tools"
            :params="sc.params"
            :parent="parent"
            @click.native="clickHandle($event,sc)"
            @tools-click="toolsHandle($event,sc)"
          />
        </div>
      </draggable>
      <div v-else class="statistic-tools">
        <div
          v-for="(sc,index) in list"
          :key="sc.name + '_' + index"
          class="statistic-item"
          :class="'statistic-item--' + sc.name"
        >
          <component
            :is="sc.name"
            ref="statisticItem"
            :read="read"
            :tools="tools"
            :params="sc.params"
            :parent="parent"
            @click.native="clickHandle($event,sc)"
            @tools-click="toolsHandle($event,sc)"
          />
        </div>
      </div>
      </div>
      <button
        v-show="scrollOverflow"
        type="button"
        class="statistic-scroll-btn statistic-scroll-btn--next"
        :disabled="!canScrollRight"
        @click="scrollNext"
      >
        <i class="el-icon-arrow-right" />
      </button>
    </div>
  </div>
</template>

<script>
import Cat2ButTitle from "./Components/Title"
import draggable from 'vuedraggable'
import {addStatistic, listStatistic} from "@/api/system/statistic/template";
import { resolveCurrentProjectId } from './utils/project-id'

const path = require('path');
const files = require.context('./Statistic/', true, /\.vue$/);
const modules = {Cat2ButTitle,draggable};
const allStatisticList = [];
// 动态加载组件
files.keys().forEach(key=>{
  const name = path.basename(key,'.vue');
  allStatisticList.push({
    name: name,
  });
  modules[name] = files(key).default||files(key)
});

/** 无统计模板时缺陷页默认统计块（含 ECharts 待办块） */
const DEFAULT_DEFECT_STATISTIC_TEMPLATE = [
  { name: 'DefectMemberOnline' },
  { name: 'DefectModule' },
  { name: 'DefectState' },
  { name: 'DefectType' },
  { name: 'MyOpenTodoGauge' },
  { name: 'TeamOpenWorkloadBar' },
  { name: 'MyLife' }
]

/** 模版选择区：个人 / 团队分组 */
export const PERSONAL_STATISTIC_NAMES = [
  'MyOpenTodoGauge',
  'MyLife',
  'MyParticipationHeatmap',
  'PersonalRemindTimer'
]
export const TEAM_STATISTIC_NAMES = [
  'DefectMemberOnline',
  'DefectModule',
  'DefectState',
  'DefectType',
  'TeamOpenWorkloadBar',
  'TeamPlanBurndown',
  'TeamPlanMetricsRadar',
  'TeamPlanCountdown',
  'TeamDefectStateTrend',
  'TeamMemberDefectTrend'
]

export default {
  name: "Cat2BugStatistic",
  model: {
    prop: 'statisticComponents',
    event: 'change'
  },
  components: modules,
  data() {
    return {
      activeNames: null,
      statisticList: [],
      canScrollLeft: false,
      canScrollRight: false,
      scrollOverflow: false,
      // 统计工具
      statisticToolsList: [{
        icon: 'close'
      }],
    }
  },
  props: {
    statisticComponents: {
      type: Array,
      default: null
    },
    /** 显示类型，member=根据成员自己设置的模版显示；all=显示所有模版 */
    showType: {
      type: String,
      default: 'member'
    },
    statisticTools: {
      type: Array,
      default: null
    },
    draggable: {
      type: Boolean,
      default: false
    },
    params: {
      type: Object,
      default: ()=> {}
    },
    read: {
      type: Boolean,
      default: false
    },
    /** 为 true 时统计块自动换行（用于模版选择区）；默认单行横向滚动 */
    wrap: {
      type: Boolean,
      default: false
    },
    /** 模版选择区分组：personal | team；为空则展示全部 */
    templateGroup: {
      type: String,
      default: null
    }
  },
  computed: {
    parent: function() {
      return this;
    },
    draggableList: {
      get(){
        return this.draggable?this.list:null;
      },
      set(val) {
        if(this.draggable)
          this.list = val;
      }
    },
    list: {
      get(){
        return this.statisticComponents || this.statisticList;
      },
      set(val) {
        if(this.statisticComponents) {
          this.$emit('change', val);
        }
        this.statisticList = val;
      }
    },
    tools: function () {
      return this.statisticTools || this.statisticToolsList;
    },
    /** 获取项目id */
    projectId() {
      return resolveCurrentProjectId(this)
    },
    memberId() {
      return parseInt(this.$store.state.user.id);
    },
    /** 无统计块时不占位（配合缺陷页 .defect-tools-statistic flex gap） */
    emptyHiddenStyle() {
      return this.list && this.list.length > 0 ? {} : { display: 'none' };
    }
  },
  watch: {
    list: {
      handler() {
        this.$nextTick(this.updateScrollState);
      },
      deep: true
    }
  },
  mounted() {
    switch (this.showType) {
      case 'member':
        this.getStatisticList();
        break;
      case 'all':
        this.getAllStatisticList();
        break;
    }
    this.$nextTick(this.initScrollObserver);
  },
  created() {
  },
  beforeDestroy() {
    if (this._scrollResizeObserver) {
      this._scrollResizeObserver.disconnect();
      this._scrollResizeObserver = null;
    }
  },
  methods: {
    /** 保存单个统计块的 params（报时规则、计划选择等） */
    saveStatisticItemParams(name, paramsPatch) {
      if (this.read) {
        return
      }
      const list = (this.list || []).map(item => {
        if (item.name !== name) {
          return item
        }
        return {
          ...item,
          params: { ...(item.params || {}), ...paramsPatch }
        }
      })
      if (this.statisticComponents) {
        this.$emit('change', list)
        return
      }
      this.statisticList = list
      const body = {
        userId: this.memberId,
        projectId: this.projectId,
        moduleType: 1,
        statisticTemplatConfig: JSON.stringify(list)
      }
      addStatistic(body).then(() => {
        this.$emit('change', list)
      })
    },
    /** 更新统计面板 */
    updateStatisticPanel: function (){
      let params = {
        userId: this.memberId,
        projectId: this.projectId,
        moduleType: 1,
        statisticTemplatConfig: JSON.stringify(this.list)
      }
      addStatistic(params).then(res => {
        this.$message.success(this.$i18n.t('defect.statistic-save-success').toString());
      });
    },
    inputChanged(value) {
      this.activeNames = value;
    },
    getComponentData() {
      return {
        on: {
          change: this.updateStatisticPanel,
          input: this.inputChanged
        },
        attrs:{
          wrap: true
        },
        props: {
          value: this.activeNames
        }
      };
    },
    refresh() {
      this.getStatisticList();
    },
    /** 缺陷变更后刷新各统计块数据 */
    refreshData() {
      const refs = this.$refs.statisticItem
      if (!refs) {
        return
      }
      const items = Array.isArray(refs) ? refs : [refs]
      const refreshMethods = [
        'refreshData',
        'getStatistic',
        'getParticipation',
        'getPlanMetrics',
        'getStatisticModule',
        'getStatisticDefectState',
        'getStatisticDefectType',
        'getMemberList'
      ]
      items.forEach(comp => {
        if (!comp) {
          return
        }
        for (const method of refreshMethods) {
          if (typeof comp[method] === 'function') {
            comp[method]()
            break
          }
        }
      })
    },
    searchQuery(opts) {
      if (this.$parent && typeof this.$parent.searchQuery === 'function') {
        this.$parent.searchQuery(opts);
      }
    },
    search(params) {
      this.searchQuery({ common: params });
    },
    searchByParticipation(participationLogDate, participationUserId) {
      this.searchQuery({
        stack: false,
        extension: { participationLogDate, participationUserId },
        common: { params: { defectStates: [], delFlag: '0' } }
      });
    },
    /** 获取分析模块 */
    getStatisticList() {
      if(!this.statisticComponents) {
        let params = {
          userId: this.memberId,
          projectId: this.projectId,
          moduleType: 1
        }
        listStatistic(params).then(res=>{
          let statisticList=[];
          if(res.rows.length>0) {
            statisticList = JSON.parse(res.rows[0].statisticTemplatConfig);
          } else {
            statisticList = DEFAULT_DEFECT_STATISTIC_TEMPLATE.map(item => ({ ...item }));
          }
          this.statisticList = statisticList;
          this.$emit('change',statisticList);
        })
      }
    },
    getAllStatisticList() {
      let list = allStatisticList
      if (this.templateGroup === 'personal') {
        list = allStatisticList.filter(item => PERSONAL_STATISTIC_NAMES.includes(item.name))
      } else if (this.templateGroup === 'team') {
        list = allStatisticList.filter(item => TEAM_STATISTIC_NAMES.includes(item.name))
      }
      this.statisticList = list
    },
    clickHandle(e,sc){
      e.stopPropagation();
      this.$emit('click-template-node',e,sc);
    },
    toolsHandle(tool,sc) {
      switch (tool.icon) {
        case 'close':
          this.removeStatistic(sc);
          break;
      }
    },
    removeStatistic(sc) {
      let list = this.list.filter(s=>s.name!=sc.name);
      let params = {
        userId: this.memberId,
        projectId: this.projectId,
        moduleType: 1,
        statisticTemplatConfig: JSON.stringify(list)
      }
      addStatistic(params).then(res=>{
        this.statisticList = list;
        this.$message.success(this.$i18n.t('delete.success').toString());
        this.$emit('change',list);
      });
    },
    mouseEnterHandle(e){
      e.stopPropagation();
    },
    mouseDownHandle(e) {
      e.stopPropagation();
    },
    initScrollObserver() {
      this.updateScrollState();
      if (typeof ResizeObserver === 'undefined' || !this.$refs.viewport) {
        return;
      }
      this._scrollResizeObserver = new ResizeObserver(() => {
        this.updateScrollState();
      });
      this._scrollResizeObserver.observe(this.$refs.viewport);
    },
    updateScrollState() {
      if (this.wrap) {
        this.scrollOverflow = false;
        this.canScrollLeft = false;
        this.canScrollRight = false;
        return;
      }
      const el = this.$refs.viewport;
      if (!el) {
        return;
      }
      const maxScroll = el.scrollWidth - el.clientWidth;
      this.scrollOverflow = maxScroll > 1;
      this.canScrollLeft = el.scrollLeft > 1;
      this.canScrollRight = maxScroll > 1 && el.scrollLeft < maxScroll - 1;
    },
    scrollPrev() {
      const el = this.$refs.viewport;
      if (!el) {
        return;
      }
      const target = this.getAdjacentScrollTarget('prev');
      el.scrollTo({ left: target, behavior: 'smooth' });
      window.setTimeout(this.updateScrollState, 320);
    },
    scrollNext() {
      const el = this.$refs.viewport;
      if (!el) {
        return;
      }
      const target = this.getAdjacentScrollTarget('next');
      el.scrollTo({ left: target, behavior: 'smooth' });
      window.setTimeout(this.updateScrollState, 320);
    },
    /** 按相邻统计块计算滚动目标位置 */
    getAdjacentScrollTarget(direction) {
      const el = this.$refs.viewport;
      if (!el) {
        return 0;
      }
      const items = Array.from(el.querySelectorAll('.statistic-item'));
      if (!items.length) {
        return 0;
      }
      const scrollLeft = el.scrollLeft;
      const maxScroll = Math.max(0, el.scrollWidth - el.clientWidth);
      if (direction === 'next') {
        const next = items.find(item => item.offsetLeft > scrollLeft + 4);
        return next ? Math.min(next.offsetLeft, maxScroll) : maxScroll;
      }
      let prev = null;
      items.forEach(item => {
        if (item.offsetLeft < scrollLeft - 4) {
          prev = item;
        }
      });
      return prev ? prev.offsetLeft : 0;
    }
  }
}
</script>

<style lang="scss" scoped>
  .statistic-panel {
    min-width: 0;
    width: 100%;
  }

  .statistic-scroll-wrap {
    position: relative;
    min-width: 0;
    width: 100%;
  }

  .statistic-scroll-viewport {
    width: 100%;
    min-width: 0;
    overflow-x: auto;
    overflow-y: hidden;
    scroll-behavior: smooth;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
      width: 0;
      height: 0;
    }
  }

  .statistic-scroll-btn {
    position: absolute;
    top: 50%;
    z-index: 2;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    padding: 0;
    margin: 0;
    border: none;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.45);
    color: #fff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.18);
    cursor: pointer;
    opacity: 0;
    visibility: hidden;
    pointer-events: none;
    transform: translateY(-50%);
    transition: opacity 0.2s ease, visibility 0.2s ease, background-color 0.2s ease, transform 0.2s ease;

    &--prev {
      left: 6px;
    }

    &--next {
      right: 6px;
    }

    i {
      font-size: 14px;
      line-height: 1;
      font-weight: 600;
    }

    &:hover:not(:disabled) {
      background: rgba(0, 0, 0, 0.62);
      transform: translateY(-50%) scale(1.05);
    }

    &:disabled {
      opacity: 0 !important;
      visibility: hidden !important;
      pointer-events: none !important;
    }
  }

  .statistic-panel.is-scrollable:hover .statistic-scroll-btn:not(:disabled) {
    opacity: 1;
    visibility: visible;
    pointer-events: auto;
  }

  .statistic-tools {
    display: flex;
    flex-direction: row;
    flex-wrap: nowrap;
    align-items: flex-start;
    gap: var(--statistic-tools-gap, 10px);
    width: max-content;
    border-width: 0;

    > .statistic-item {
      margin-bottom: 0;
    }
  }

  .statistic-item {
    flex: 0 0 auto;
    align-self: flex-start;
    display: flex;
    flex-direction: column;
    box-sizing: border-box;
    min-width: var(--statistic-item-min-width, 0);
    width: var(--statistic-item-width, auto);
    max-width: var(--statistic-item-max-width, none);
    height: var(--statistic-card-height, var(--statistic-life-card-height, 115px));
    max-height: var(--statistic-card-height, var(--statistic-life-card-height, 115px));

    ::v-deep .statistic-box {
      flex: 1 1 auto;
      height: 100%;
      max-height: 100%;
      min-height: 0;
      width: 100%;
    }
  }

  .statistic-item--DefectMemberOnline {
    width: max-content;
    --statistic-item-width: max-content;
    --statistic-item-min-width: var(--statistic-item-width-member-min, 108px);
    --statistic-item-max-width: var(--statistic-item-width-member-max, 228px);
  }

  .statistic-item--DefectModule {
    width: max-content;
    --statistic-item-width: max-content;
    --statistic-item-min-width: var(--statistic-item-width-module, 248px);
  }

  .statistic-item--DefectState {
    width: max-content;
    --statistic-item-width: max-content;
    --statistic-item-min-width: var(--statistic-item-width-state, 272px);
  }

  .statistic-item--DefectType {
    width: max-content;
    --statistic-item-width: max-content;
    --statistic-item-min-width: var(--statistic-item-width-type, 208px);
  }

  .statistic-item--MyLife {
    width: max-content;
    --statistic-item-width: max-content;
    --statistic-item-min-width: var(--statistic-item-width-life-min, var(--statistic-item-width-life, 236px));
    --statistic-item-max-width: var(--statistic-item-width-life-max, 360px);
  }

  .statistic-item--MyOpenTodoGauge {
    width: max-content;
    --statistic-item-width: max-content;
    --statistic-item-min-width: 168px;
  }

  .statistic-item--TeamOpenWorkloadBar {
    width: max-content;
    --statistic-item-width: max-content;
    --statistic-item-min-width: 168px;
  }

  .statistic-item--MyParticipationHeatmap {
    width: max-content;
    --statistic-item-width: max-content;
    --statistic-item-min-width: 0;
    --statistic-item-max-width: none;
  }

  .statistic-item--TeamPlanBurndown {
    width: max-content;
    --statistic-item-width: max-content;
    --statistic-item-min-width: 320px;
  }

  .statistic-item--TeamPlanMetricsRadar {
    width: max-content;
    --statistic-item-width: max-content;
    --statistic-item-min-width: 168px;
    --statistic-item-max-width: 168px;
    overflow: visible;
  }

  .statistic-item--PersonalRemindTimer {
    width: max-content;
    --statistic-item-width: max-content;
    --statistic-item-min-width: 280px;
    --statistic-item-max-width: 280px;
  }

  .statistic-item--TeamPlanCountdown {
    width: max-content;
    --statistic-item-width: max-content;
    --statistic-item-min-width: 0;
    --statistic-item-max-width: none;
    overflow: visible;

    ::v-deep .statistic-box {
      width: max-content;
      max-width: 100%;
      flex: 0 1 auto;
    }
  }

  .statistic-item--TeamDefectStateTrend,
  .statistic-item--TeamMemberDefectTrend {
    width: max-content;
    --statistic-item-width: max-content;
    --statistic-item-min-width: 320px;
    overflow: visible;
  }

  .statistic-panel.is-wrap {
    .statistic-scroll-viewport {
      overflow: visible;
    }

    .statistic-tools {
      flex-wrap: wrap;
      width: 100%;
      row-gap: var(--statistic-tools-gap, 10px);
      column-gap: var(--statistic-tools-gap, 10px);
    }

    .statistic-tools > .statistic-item {
      margin-bottom: 0;
      height: var(--statistic-card-height, var(--statistic-life-card-height, 115px));
      max-height: var(--statistic-card-height, var(--statistic-life-card-height, 115px));
    }
  }
</style>
