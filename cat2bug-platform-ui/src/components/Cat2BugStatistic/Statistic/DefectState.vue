<template>
  <cat2-bug-card :title="$i18n.t('defect.status-statistics').toString()" v-loading="loading" :tools="tools" @tools-click="toolsHandle">
    <template slot="content">
      <div class="defect-state-panel">
      <el-tooltip
        effect="dark"
        placement="top"
        :open-delay="200"
        :content="formatStateTooltip($t('all'), total, stateDeltaTotal('d'), stateDeltaTotal('w'))"
      >
        <cat2-but-label @click.native="clickHandle" class="defect-state-label" icon="all" icon-color="#409EFF" :label="$t('all').toString()" content="12">
          <template slot="content">
            <span>{{ total }}</span>
            <span>{{ stateDeltaTotal('d') >= 0 ? '+' + stateDeltaTotal('d') : stateDeltaTotal('d') }}</span>
            <span>{{ stateDeltaTotal('w') >= 0 ? '+' + stateDeltaTotal('w') : stateDeltaTotal('w') }}</span>
          </template>
        </cat2-but-label>
      </el-tooltip>
      <el-tooltip
        v-for="(state,index) in stateList"
        :key="index"
        effect="dark"
        placement="top"
        :open-delay="200"
        :content="formatStateTooltip($t(state.k), state.a, state.d, state.w)"
      >
        <cat2-but-label @click.native="clickHandle($event,state)"
                        class="defect-state-label"
                        icon="pending-processing"
                        :icon-color="iconColor(index)"
                        :label="$t(state.k).toString()">
          <template slot="content">
            <span>{{ state.a }}</span>
            <span :flag="flag(state.d)">{{ flag(state.d) + state.d }}</span>
            <span :flag="flag(state.w)">{{ flag(state.w) + state.w }}</span>
          </template>
        </cat2-but-label>
      </el-tooltip>
      </div>
    </template>
  </cat2-bug-card>
</template>

<script>
import Cat2BugCard from "../Components/Card"
import Cat2ButLabel from "../Components/Label"
import {statisticDefectState} from "@/api/system/statistic/defect";

export default {
  name: "DefectState",
  components: {Cat2ButLabel,Cat2BugCard},
  data() {
    return {
      loading: false,
      stateList: [],
    }
  },
  props: {
    params: {
      type: Object,
      default: ()=>{}
    },
    tools: {
      type: Array,
      default: ()=>[]
    },
    parent: {
      type: Object,
      default: ()=>{}
    },
    read: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    flag: function () {
      return function (num) {
        if(num>0) {
          return '+';
        } if(num<0) {
          return '-';
        } else {
          return '';
        }
      }
    },
    currentProjectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    total: function () {
      let all = 0;
      this.stateList.forEach(s=>all+=s.a);
      return all;
    },
    stateDeltaTotal() {
      return function (field) {
        let sum = 0;
        this.stateList.forEach(s => { sum += (s[field] || 0); });
        return sum;
      };
    },
    iconColor: function () {
      return function (index) {
        switch (index%3) {
          case 0:
            return '#F56C6C'
          case 1:
            return '#FBB13F'
          case 2:
            return '#67C23A'
        }
      }
    }
  },
  mounted() {
    this.getStatisticDefectState();
  },
  methods: {
    clickHandle(event, state) {
      if(this.read) return;
      this.parent.search({
        params: {
          defectStates: state?JSON.parse(JSON.stringify(state.id)):null
        }
      });
      event.stopPropagation();
    },
    getStatisticDefectState() {
      this.loading = true;
      statisticDefectState(this.currentProjectId).then(res=>{
        this.loading = false;
        // 合并处理中和驳回的
        let a=0,d=0,w=0;
        res.data.forEach(s=>{
          if(s.k=='PROCESSING' || s.k=='REJECTED') {
            a+=s.a;
            d+=s.d;
            w+=s.w;
          }
        });
        let ret = [{
          id: [0,3],
          k:'PENDING',
          a:a,
          d:d,
          w:w
        }]
        // 过滤只显示待验证、已审核的
        ret = ret.concat( res.data.filter(s=>s.k == 'AUDIT').map(s=>{
          return {
            id: [s.id],
            k:s.k,
            a:s.a,
            d:s.d,
            w:s.w
          }
        }));
        ret = ret.concat( res.data.filter(s=>s.k=='CLOSED').map(s=>{
          return {
            id: [s.id],
            k:s.k,
            a:s.a,
            d:s.d,
            w:s.w
          }
        }))
        this.stateList = ret;
      })
    },
    toolsHandle(e,tool) {
      this.$emit('tools-click',tool);
    },
    formatDelta(num) {
      const value = num || 0;
      if (value > 0) {
        return '+' + value;
      }
      if (value < 0) {
        return String(value);
      }
      return '0';
    },
    formatStateTooltip(label, total, dayDelta, weekDelta) {
      return `${label}：${total} / ${this.$t('today')}：${this.formatDelta(dayDelta)} / ${this.$t('this-week')}：${this.formatDelta(weekDelta)}`;
    }
  }
}
</script>

<style lang="scss" scoped>
  .defect-state-panel {
    display: flex;
    flex-direction: column;
    gap: 0;
    flex: 1 1 auto;
    min-height: 0;
    max-height: 100%;
    overflow-x: hidden;
    overflow-y: auto;

    ::v-deep .el-tooltip {
      display: block;
      width: 100%;
    }
  }

  .defect-state-panel ::v-deep .el-tooltip.defect-state-label {
    display: grid;
    grid-template-columns: 16px minmax(48px, 1fr) 28px 36px 36px;
    column-gap: 6px;
    align-items: center;
    cursor: pointer;
    padding-left: 5px;
    padding-right: 5px;
    border-radius: 2px;
    transition: background-color 0.15s;

    &:hover {
      background-color: rgba(64, 158, 255, 0.06);
    }

    &:hover .cat2bug-statistic-label__name {
      color: #409eff;
    }
  }

  .defect-state-panel ::v-deep span[flag="+"] {
    color: #f56c6c;
  }

  .defect-state-panel ::v-deep span[flag="-"] {
    color: #67c23a;
  }

  .defect-state-panel ::v-deep .el-tooltip.defect-state-label + .el-tooltip.defect-state-label {
    border-top: 1px dashed var(--border-color-lighter, #f2f6fc);
  }
</style>
