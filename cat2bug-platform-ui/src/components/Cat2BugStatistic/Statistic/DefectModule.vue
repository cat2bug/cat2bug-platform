<template>
  <cat2-bug-card :title="title">
    <template slot="content">
      <div class="defect-module-row">
        <h5 :style="`background-color:${flagColor(0)}`">1</h5><label>登陆界面</label><el-progress :percentage="20" :format="format" :color="customColors"></el-progress><span>12/232</span>
      </div>
      <div class="defect-module-row">
        <h5 :style="`background-color:${flagColor(1)}`">2</h5><label>项目列表</label><el-progress :percentage="60" :format="format" :color="customColors"></el-progress>
      </div>
      <div class="defect-module-row">
        <h5 :style="`background-color:${flagColor(2)}`">3</h5><label>团队列表团队列表团队列表团队列表</label><el-progress :percentage="90" :format="format" :color="customColors"></el-progress>
      </div>
    </template>
  </cat2-bug-card>
</template>

<script>
import Cat2BugCard from "../Components/Card"
export default {
  name: "DefectModule",
  components: {Cat2BugCard},
  data() {
    return {
      title: "模块排行",
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
    }
  },
  methods: {
    format(percentage) {
      return ``;
    }
  }
}
</script>

<style scoped>
  .el-progress {
    width: 150px;
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
  label {
    font-size: 13px;
    font-weight: 500;
    margin-right: 10px;
    margin-left: 5px;
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
    margin-top: 5px;
    margin-bottom: 5px;
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
</style>
