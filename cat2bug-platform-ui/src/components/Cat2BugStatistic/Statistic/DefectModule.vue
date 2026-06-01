<template>
  <cat2-bug-card :title="$i18n.t('defect.module-ranking').toString()" :tools="tools" @tools-click="toolsHandle">
    <template slot="content">
      <div class="defect-module-panel">
      <div v-if="!showModuleList || showModuleList.length==0">
        <div @click="getStatisticModule" class="defect-module-row" v-for="n in 3" :key="n">
          <h5 class="skeleton">{{ n+1 }}</h5><div class="skeleton width100"></div><div class="skeleton width120"></div><div class="skeleton width30"></div>
        </div>
      </div>
      <div v-else>
        <div @click="clickHandle(m)" class="defect-module-row" v-for="(m,index) in showModuleList" :key="index">
          <h5 :style="`background-color:${flagColor(index)}`">{{ rankingNumber(index) }}</h5><h4>{{ $te(m.k)?$t(m.k):m.k }}</h4><el-progress :percentage="percentage(m)" :format="format" :color="customColors"></el-progress><span>{{ m.f }}/{{ m.a }}</span>
        </div>
        <el-pagination
          class="statistic-pagination"
          small
          :hide-on-single-page="total<formParams.pageSize"
          layout="prev, next"
          :current-page.sync="formParams.pageNum"
          :page-size.sync="formParams.pageSize"
          :total="total"
          @current-change="getStatisticModule">
        </el-pagination>
      </div>
      </div>

    </template>
  </cat2-bug-card>
</template>

<script>
import Cat2BugCard from "../Components/Card"
import {statisticModule} from "@/api/system/statistic/defect";
export default {
  name: "DefectModule",
  components: {Cat2BugCard},
  data() {
    return {
      total:0,
      prevClickModuleId: null,
      loading: false,
      moduleList:[],
      customColors: [
        {color: '#6f7ad3', percentage: 20},
        {color: 'rgb(245, 108, 108)', percentage: 40},
        {color: 'rgb(251, 177, 63)', percentage: 60},
        {color: '#1989fa', percentage: 80},
        {color: 'rgb(103, 194, 58)', percentage: 100},
      ],
      formParams: {
        pageNum: 1,
        pageSize: 3,
      }
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
    /** 排行号 */
    rankingNumber () {
      return function (index) {
        return (this.formParams.pageNum - 1) * this.formParams.pageSize + index + 1;
      }
    },
    percentage: function (){
      return function (m) {
        return parseInt(m.f/m.a*100)||0;
      }
    },
    showModuleList: function (){
      let ret = [];
      for(let i=0;i<3 && i<this.moduleList.length;i++){
        ret.push(this.moduleList[i])
      }
      return ret
    },
    flagColor: function (){
      return function (index) {
        switch (index%5) {
          case 0:
            return 'rgb(245, 108, 108)';
          case 1:
            return 'rgb(251, 177, 63)';
          case 2:
            return 'rgb(103, 194, 58)';
          case 3:
            return '#1989fa';
          case 4:
            return '#6f7ad3';
        }
      }
    },
    currentProjectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
  },
  mounted() {
    this.getStatisticModule();
  },
  methods: {
    format(percentage) {
      return ``;
    },
    getStatisticModule() {
      this.loading = true;
      statisticModule(this.currentProjectId,this.formParams).then(res=>{
        this.loading = false;
        this.moduleList = res.rows;
        this.total = res.total;
      })
    },
    clickHandle(m) {
      if(this.read) return;
      let id = this.prevClickModuleId==m.id?null:m.id;
      this.parent.searchQuery({
        common: { moduleId: id }
      });
      this.prevClickModuleId = id;
    },
    toolsHandle(e,tool) {
      this.$emit('tools-click',tool);
    }
  }
}
</script>

<style scoped>
  .defect-module-panel {
    display: flex;
    flex-direction: column;
    flex: 1 1 auto;
    min-height: 0;
    max-height: 100%;
    overflow-x: hidden;
    overflow-y: auto;
  }
  .el-progress {
    flex: 0 0 72px;
    width: 72px;
    min-width: 72px;
    max-width: 72px;
    margin-right: 4px;
  }
  .width30 {
    width: 32px;
  }
  .width100 {
    width: 72px;
  }
  .width120 {
    width: 72px;
  }
  h5 {
    border-radius: 3px;
    color: #ffffff;
    font-size: 10px;
    font-weight: 600;
    flex-shrink: 0;
    width: 14px;
    height: 14px;
    line-height: 14px;
    text-align: center;
    margin: 0;
  }
  h4 {
    font-size: 12px;
    font-weight: 500;
    margin: 0 6px 0 4px;
    flex: 0 1 72px;
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    color: var(--text-color-regular, #606266);
  }
  span {
    font-size: 11px;
    font-variant-numeric: tabular-nums;
    flex: 0 0 32px;
    min-width: 32px;
    text-align: right;
    color: var(--text-color-secondary, #909399);
  }
  .defect-module-row {
    display: flex;
    flex-direction: row;
    align-items: center;
    min-height: var(--statistic-module-row-min-height, 17px);
    margin: 0;
    padding: var(--statistic-module-row-padding-y, 1px) 5px;
    cursor: pointer;
    gap: 2px;
    border-radius: 2px;
    transition: background-color 0.15s;
  }
  .defect-module-row + .defect-module-row {
    border-top: 1px dashed var(--border-color-lighter, #f2f6fc);
  }
  .defect-module-row:hover {
    background-color: rgba(64, 158, 255, 0.06);
  }
  .defect-module-row:hover h4 {
    color: #409eff;
  }
  ::v-deep .el-progress-bar {
    width: 100%;
    margin-right: 0;
    padding-right: 0;
  }
  ::v-deep .el-progress__text {
    display: none;
  }
  ::v-deep .el-progress-bar__outer {
    height: 4px !important;
    border-radius: 2px !important;
    overflow: hidden;
    background-color: var(--progress-track-bg, #ebeef5) !important;
    border: none !important;
  }
  ::v-deep .el-progress-bar__inner {
    height: 100% !important;
    line-height: 1 !important;
    border-radius: 0 !important;
    vertical-align: top;
  }
  .skeleton {
    height: 12px;
    background-color: var(--border-color-lighter, #e4e7ed);
    border-radius: 3px;
    margin-left: 6px;
  }
  .el-pagination {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    margin-top: 2px;
    padding-top: 2px;
    line-height: 1;
  }
  ::v-deep .el-pagination .el-icon {
    font-size: 10px !important;
  }
  ::v-deep .el-pagination button {
    min-width: calc(var(--statistic-pagination-control-height, 15px) + 5px) !important;
    height: var(--statistic-pagination-control-height, 15px) !important;
    line-height: var(--statistic-pagination-control-height, 15px) !important;
  }
</style>
