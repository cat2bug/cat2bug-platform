<template>
  <h3>{{$t('project')}}:<span class="project-label-title">{{project.projectName}}</span></h3>
</template>

<script>
import {getProject} from "@/api/system/project";

export default {
  name: "ProjectLabel",
  data() {
    return {
      project:{}
    }
  },
  created() {
    this.getProjectInfo();
  },
  methods: {
    getProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    getProjectInfo() {
      getProject(this.getProjectId()).then(res=>{
        this.project = res.data;
      });
    }
  }
}
</script>

<style scoped>
  h3 {
    margin-top: 0px;
    margin-bottom: 10px;
  }
  .project-label-title {
    margin-left: 10px;
    color: #303133;
  }
</style>
