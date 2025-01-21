<template>
  <div v-loading="loading" class="defect-state-chart" :style="{height:height,width:width}">
    <div class="title">
      <h1>{{ $t('defect') }}</h1>
      <span>{{ defectStatistics.total }}</span>
    </div>
    <div class="data">
      <div>
        <svg-icon icon-class="pending-processing" :style="`color: ${iconColor(0)||'#606266'};`" />
        <span> {{ $t('PENDING') }} {{ defectStatistics.pendingCount }}</span>
      </div>
      <div>
        <svg-icon icon-class="pending-processing" :style="`color: ${iconColor(1)||'#606266'};`" />
        <span> {{ $t('AUDIT') }} {{ defectStatistics.verifyCount }}</span>
      </div>
      <div>
        <svg-icon icon-class="pending-processing" :style="`color: ${iconColor(2)||'#606266'};`" />
        <span> {{ $t('CLOSED') }} {{ defectStatistics.closedCount }}</span>
      </div>
    </div>
  </div>
</template>

<script>
// 用例统计

import {defectStatistics} from "@/api/system/dashboard";

export default {
  name: "DefectStatisticsChart",
  props: {
    width: {
      type: String,
      default: '100%'
    },
    height: {
      type: String,
      default: '100px'
    },
  },
  data() {
    return {
      loading: false,
      chart: null,
      defectStatistics: {}
    }
  },
  computed: {
    /** 获取项目id */
    projectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
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
    this.getDefectStatistics();
  },
  methods: {
    getDefectStatistics() {
      this.loading=true;
      defectStatistics(this.projectId).then(res=>{
        this.loading=false;
        this.defectStatistics = res.data;
      }).catch(e=>this.loading=false);
    }
  }
}
</script>

<style lang="scss" scoped>
.defect-state-chart {
  position: relative;
  .title {
    width: 100%;
    display: inline-flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    > h1 {
      flex-shrink: 0;
    }
    > span {
      flex: 1;
      font-size: 3rem;
      text-align: center;
    }
  }
  .data {
    display: inline-flex;
    flex-direction: row;
    flex-wrap: wrap;
    > * {
      font-size: 0.8rem;
      width: 50%;
      padding-bottom: 5px;
    }
  }
}
.time-type {
  position: absolute;
  z-index: 9;
  top: 7px;
  right: 40px;
  ::v-deep .el-radio-button__inner {
    padding: 3px 5px;
  }
}
</style>
