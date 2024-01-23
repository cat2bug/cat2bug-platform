<template>
  <cat2-bug-card :title="title">
    <template slot="content">
      <div v-if="!showModuleList || showModuleList.length==0">
        <div @click="getStatisticModule" class="defect-module-row" v-for="n in 4" :key="n">
          <h5 class="skeleton">{{ n+1 }}</h5><div class="skeleton width100"></div><div class="skeleton width120"></div><div class="skeleton width30"></div>
        </div>
      </div>
      <div v-else>
        <div @click="clickHandle(m)" class="defect-module-row" v-for="(m,index) in showModuleList" :key="index">
          <h5 :style="`background-color:${flagColor(index)}`">{{ index+1 }}</h5><h4>{{ $t(m.k) }}</h4><el-progress :percentage="percentage(m)" :format="format" :color="customColors"></el-progress><span>{{ m.f }}/{{ m.a }}</span>
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
      prevClickModuleId: null,
      loading: false,
      title: this.$i18n.t('defect.module-ranking'),
      moduleList:[],
      customColors: [
        {color: '#6f7ad3', percentage: 20},
        {color: 'rgb(245, 108, 108)', percentage: 40},
        {color: 'rgb(251, 177, 63)', percentage: 60},
        {color: '#1989fa', percentage: 80},
        {color: 'rgb(103, 194, 58)', percentage: 100},
      ]
    }
  },
  props: {
    params: {
      type: Object,
      default: ()=>[]
    }
  },
  computed: {
    percentage: function (){
      return function (m) {
        return parseInt(m.f/m.a*100)||0;
      }
    },
    showModuleList: function (){
      let ret = [];
      for(let i=0;i<4 && i<this.moduleList.length;i++){
        ret.push(this.moduleList[i])
      }
      console.log(ret);
      return ret;
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
      statisticModule(this.currentProjectId).then(res=>{
        this.loading = false;
        this.moduleList = res.data;
      })
    },
    clickHandle(m) {
      let id = this.prevClickModuleId==m.id?null:m.id;
      this.$parent.search({
        moduleId: id
      });
      this.prevClickModuleId = id;
    }
  }
}
</script>

<style scoped>
  .el-progress {
    width: 150px;
  }
  .width30 {
    width: 30px;
  }
  .width100 {
    width: 100px;
  }
  .width120 {
    width: 120px;
  }
  h5 {
    border-radius: 5px;
    color: #FFFFFF;
    font-size: 12px;
    font-weight: 500;
    float: left;
    width: 16px;
    height: 16px;
    line-height: 16px;
    text-align: center;
    margin-top: 0px;
    margin-bottom: 0px;
  }
  h4 {
    font-size: 13px;
    font-weight: 500;
    margin: 0 10px 0 5px;
    width: 100px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
  span {
    font-size: 13px;
  }
  .el-carousel {
    width: 300px;
  }
  .defect-module-row {
    display: flex;
    flex-direction: row;
    align-items: center;
    margin-top: 5px;
    margin-bottom: 5px;
    cursor: pointer;
  }
  .defect-module-row:hover {
    color: #409EFF;
  }
  .el-progress {
    margin-right: 5px;
  }
  ::v-deep .el-progress-bar {
    margin-right: 10px;
    padding-right: 0px;
  }
  ::v-deep .el-progress__text {
    display: none;
  }
  .skeleton {
    height: 16px;
    background-color: #E4E7ED;
    border-radius: 5px;
    margin-left: 10px;
  }
</style>
