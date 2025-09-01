<template>
  <h3 class="row">
    <span>{{$t('project')}}:</span>
    <project-select class="margin-left-10" :team-id="currentTeamId" :project-id="currentProjectId" :member-id="currentMemberId" />
  </h3>
</template>

<script>
import {getProject} from "@/api/system/project";
import ProjectSelect from "@/components/Project/ProjectSelect";
export default {
  name: "ProjectLabel",
  components: {ProjectSelect},
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
  computed: {
    currentProjectId: function () {
      return this.projectId==0?parseInt(this.$store.state.user.config.currentProjectId):this.projectId;
    },
    /** 获取当前成员id */
    currentMemberId: function() {
      return this.$store.state.user.id;
    },
    /** 获取团队id */
    currentTeamId() {
      return this.$store.state.user.config.currentTeamId;
    },
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
  .margin-left-10 {
    margin-left: 10px;
  }
  h3 {
    margin-top: 0px;
    margin-bottom: 20px;
  }
  .row {
    display: inline-flex;
    flex-direction: row;
    align-items: center;
  }
</style>
