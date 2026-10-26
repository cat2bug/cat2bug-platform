<template>
  <div class="board-chart-host" :class="{ 'is-embedded': embedded }">
    <component
      :is="chartComponent"
      v-if="chartComponent"
      v-bind="chartBind"
    />
  </div>
</template>

<script>
import CaseStatisticsChart from '@/views/system/dashboard/components/CaseStatisticsChart'
import DefectStatisticsChart from '@/views/system/dashboard/components/DefectStatisticsChart'
import ModuleStatisticsChart from '@/views/system/dashboard/components/ModuleStatisticsChart'
import DocumentStatisticsChart from '@/views/system/dashboard/components/DocumentStatisticsChart'
import MemberStatisticsChart from '@/views/system/dashboard/components/MemberStatisticsChart'
import DefectStateChart from '@/views/system/dashboard/components/DefectStateChart'
import MemberOfDefectLine from '@/views/system/dashboard/components/MemberOfDefectLine'
import PlanStatisticsChart from '@/views/system/dashboard/components/PlanStatisticsChart'
import ActionList from '@/views/system/dashboard/components/ActionList'
import MemberOfDefectRank from '@/views/system/dashboard/components/MemberOfDefectRank'
import PlanBurndownChart from '@/views/system/dashboard/components/PlanBurndownChart'

const CHARTS = {
  CaseStatisticsChart,
  DefectStatisticsChart,
  ModuleStatisticsChart,
  DocumentStatisticsChart,
  MemberStatisticsChart,
  DefectStateChart,
  MemberOfDefectLine,
  PlanStatisticsChart,
  ActionList,
  MemberOfDefectRank,
  PlanBurndownChart
}

export default {
  name: 'BoardChartHost',
  props: {
    widgetId: { type: String, default: '' },
    boardChart: { type: String, required: true },
    /** 工作台内嵌：压缩高度、隐藏组件自带标题（外层已有 widget 标题） */
    embedded: { type: Boolean, default: false }
  },
  computed: {
    chartComponent() {
      return CHARTS[this.boardChart] || null
    },
    chartBind() {
      if (this.boardChart === 'PlanBurndownChart' && this.embedded) {
        return { embedded: true, height: '220px' }
      }
      return {}
    }
  }
}
</script>

<style scoped lang="scss">
.board-chart-host {
  width: 100%;
  min-height: 0;
}
.board-chart-host.is-embedded {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.board-chart-host.is-embedded :deep(.plan-burndown-chart) {
  flex: 1;
  min-height: 0;
}
</style>
