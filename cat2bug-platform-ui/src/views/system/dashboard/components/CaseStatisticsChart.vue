<template>
  <div v-loading="loading" class="defect-state-chart" :style="{height:height,width:width}">
    <div class="title">
      <h1>{{ $t('case') }}</h1>
      <span>{{ caseStatistics.total }}</span>
    </div>
  </div>
</template>

<script>
// 用例统计

import {caseStatistics} from "@/api/system/dashboard";

export default {
  name: "CaseStatisticsChart",
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
      caseStatistics:{}
    }
  },
  computed: {
    /** 获取项目id */
    projectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
  },
  mounted() {
    this.getCaseStatistics();
  },
  methods: {
    getCaseStatistics() {
      this.loading = true;
      caseStatistics(this.projectId).then(res=>{
        this.loading = false;
        this.caseStatistics = res.data;
      }).catch(e=>this.loading = false);
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
