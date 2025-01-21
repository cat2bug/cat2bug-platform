<template>
  <div v-loading="loading" class="defect-state-chart" :style="{height:height,width:width}">
    <div class="title">
      <h1>{{ $t('member') }}</h1>
      <span>{{ memberStatistics.total }}</span>
    </div>
    <div class="data">
      <div>
        <svg-icon icon-class="user" :style="`color: ${iconColor(0)||'#606266'};`" />
        <span> {{ $t('project.admin') }} {{ memberStatistics.adminCount }}</span>
      </div>
      <div>
        <svg-icon icon-class="user" :style="`color: ${iconColor(1)||'#606266'};`" />
        <span> {{ $t('project.tester') }} {{ memberStatistics.testCount }}</span>
      </div>
      <div>
        <svg-icon icon-class="user" :style="`color: ${iconColor(2)||'#606266'};`" />
        <span> {{ $t('project.develop') }} {{ memberStatistics.developmentCount }}</span>
      </div>
      <div>
        <svg-icon icon-class="user" :style="`color: ${iconColor(3)||'#606266'};`" />
        <span> {{ $t('project.outsider') }} {{ memberStatistics.outsideCount }}</span>
      </div>
    </div>
  </div>
</template>

<script>
// 用例统计

import {memberStatistics} from "@/api/system/dashboard";

export default {
  name: "MemberStatisticsChart",
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
      memberStatistics: {}
    }
  },
  computed: {
    /** 获取项目id */
    projectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    iconColor: function () {
      return function (index) {
        switch (index) {
          case 0:
            return '#F56C6C'
          case 1:
            return '#FBB13F'
          case 2:
            return '#67C23A'
          default:
            return '#909399'
        }
      }
    }
  },
  mounted() {
    this.getMemberStatistics();
  },
  methods: {
    getMemberStatistics() {
      this.loading = true;
      memberStatistics(this.projectId).then(res=>{
        this.loading = false;
        this.memberStatistics = res.data;
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
