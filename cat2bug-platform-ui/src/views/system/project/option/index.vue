<template>
  <div class="app-container">
    <project-label />
    <el-row :gutter="20">
      <el-col :span="6" v-for="(m, index) in itemList" :key="index">
        <component :is="m.name" v-model="project"></component>
      </el-col>
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
    i {
      color: #409EFF;
      margin-right: 10px;
      margin-top: 3px;
    }
  }
  ::v-deep .el-card__body {
    min-height: 150px;
    display: flex;
    align-items: flex-start;
    flex-direction: column;
    > * {
      margin-left: 20px;
      margin-bottom: 15px;
    }
  }
</style>
