<template>
  <div v-loading="loading" class="defect-state-chart" :style="{height:height,width:width}">
    <div class="title" @click="handleClick">
      <cat2-bug-block color="rgb(251, 177, 63)" />
      <h1>{{ $t('document') }}</h1>
      <span>{{ documentStatistics.total }}</span>
    </div>
  </div>
</template>

<script>
// 用例统计
import Cat2BugBlock from "@/components/Cat2BugBlock";
import {documentStatistics} from "@/api/system/dashboard";

export default {
  name: "DocumentStatisticsChart",
  components: { Cat2BugBlock },
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
      documentStatistics: {}
    }
  },
  computed: {
    /** 获取项目id */
    projectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
  },
  mounted() {
    this.getDocumentStatistics();
  },
  methods: {
    handleClick() {
      const targetRoute = this.$router.resolve({ name:'Document', params: {}});
      window.open(targetRoute.href, '_blank');
    },
    getDocumentStatistics() {
      this.loading=true;
      documentStatistics(this.projectId).then(res=>{
        this.loading = false;
        this.documentStatistics = res.data;
      }).catch(e=>this.loading=false);
    }
  }
}
</script>

<style lang="scss" scoped>
.defect-state-chart {
  position: relative;
  .title:hover {
    cursor: pointer;
  }
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
