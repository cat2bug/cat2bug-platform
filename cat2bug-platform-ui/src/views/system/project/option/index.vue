<template>
  <div class="app-container">
    <project-label />
    <el-row :gutter="20">
      <component :is="m.name" v-for="(m, index) in itemList" :key="index" v-model="project"></component>
    </el-row>
  </div>
</template>

<script>
import {getProject} from "@/api/system/project";
import ProjectLabel from "@/components/Project/ProjectLabel";

const path = require('path');
const files = require.context('./item/', true, /\.vue$/);
const modules = {ProjectLabel};
const moduleList = [];
// 动态加载组件
files.keys().forEach(key=>{
  const name = path.basename(key,'.vue');
  moduleList.push({
    name: name,
  });
  modules[name] = files(key).default||files(key)
});
export default {
  name: "ProjectOption",
  components: modules,
  data() {
    return {
      project: {},
      itemList: moduleList
    };
  },
  mounted() {
    this.getProject();
  },
  // 移除滚动条监听
  destroyed() {
  },
  methods: {
    getProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    getProject() {
      getProject(this.getProjectId()).then(res=>{
        this.project = res.data;
      });
    },
  }
};
</script>
<style lang="scss" scoped>
  $project-option-card-gap: 20px;

  /* gutter 仅控制列间距，补相同 row-gap 使行列间隔一致 */
  ::v-deep .el-row {
    margin-bottom: -$project-option-card-gap;
  }

  ::v-deep .el-col {
    margin-bottom: $project-option-card-gap;
  }

  ::v-deep .el-card__header {
    font-weight: 600;
    div:first-child {
      display: flex;
      flex-direction: row;
      width: calc(100% - 30px);
      div {
        display: flex;
        flex-direction: column;
        width: 100%;
        span {
          margin-bottom: 5px;
        }
        span:first-child, span:last-child {
          text-overflow: ellipsis;
          overflow: hidden;
          white-space: nowrap;
        }
        span:last-child {
          line-height: 12px;
          font-size: 12px;
          color: #C0C4CC;
        }
      }
    }
    i, .svg-icon {
      color: #409EFF;
      margin-right: 10px;
      margin-top: 3px;
      
      @at-root html.dark & {
        color: #FFC107 !important;
      }
    }
  }
  ::v-deep .el-card__body {
    min-height: 180px;
    display: flex;
    align-items: flex-start;
    flex-direction: column;
    > * {
      margin-left: 20px;
      margin-bottom: 15px;
    }
  }
</style>
