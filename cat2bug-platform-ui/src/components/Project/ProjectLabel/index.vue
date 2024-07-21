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
  props: {
    projectId : {
      type: Number,
      default: 0
    }
  },
  watch: {
    projectId: function (n,o) {
      if(n!=o) {
        this.getProjectInfo(n);
      }
    }
  },
  created() {
    this.getProjectInfo(this.getProjectId());
  },
  methods: {
    getProjectId() {
      return this.projectId==0?parseInt(this.$store.state.user.config.currentProjectId):this.projectId;
    },
    getProjectInfo(projectId) {
      getProject(projectId).then(res=>{
        this.project = res.data;
      });
    }
  }
}
</script>

<style scoped>
  h3 {
    margin-top: 0px;
    margin-bottom: 20px;
  }
  .project-label-title {
    margin-left: 10px;
    color: #303133;
  }
</style>
