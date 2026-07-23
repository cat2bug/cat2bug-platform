<template>
  <h3 class="row">
    <span>{{$t('project')}}:</span>
    <project-select
      ref="projectSelect"
      class="margin-left-10"
      :team-id="currentTeamId"
      :project-id="currentProjectId"
      :member-id="currentMemberId"
    />
  </h3>
</template>

<script>
import {getProject} from "@/api/system/project";
import ProjectSelect from "@/components/Project/ProjectSelect";
import { storeToRefs } from "pinia";
import { useUserStore } from "@/stores/user";
export default {
  name: "ProjectLabel",
  components: {ProjectSelect},
  setup() {
    const userStore = useUserStore()
    const { id, config } = storeToRefs(userStore)
    return { id, config }
  },
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
      return this.projectId==0?parseInt(this.config.currentProjectId):this.projectId;
    },
    /** 获取当前成员id */
    currentMemberId: function() {
      return this.id;
    },
    /** 获取团队id（优先当前项目所属团队，避免 config.currentTeamId 与当前项目不一致） */
    currentTeamId() {
      const projectTeamId = this.project && this.project.teamId
      if (projectTeamId) {
        return projectTeamId
      }
      return this.config.currentTeamId
    },
  },
  methods: {
    getProjectId() {
      return this.projectId==0?parseInt(this.config.currentProjectId):this.projectId;
    },
    getProjectInfo(projectId) {
      getProject(projectId).then(res=>{
        this.project = res.data;
      });
    },
    /** 打开项目切换浮层（供页面快捷键调用） */
    openProjectSelect() {
      const select = this.$refs.projectSelect
      if (select && typeof select.openPopover === 'function') {
        select.openPopover()
      }
    }
  }
}
</script>

<style scoped>
  .margin-left-10 {
    margin-left: 10px;
    /* 长项目名时由 ProjectSelect 内省略，避免把「项目:」与选择器折成两行 */
    min-width: 0;
    flex: 0 1 auto;
    max-width: 100%;
  }
  h3 {
    margin-top: 0px;
    margin-bottom: 20px;
  }
  .row {
    display: inline-flex;
    flex-direction: row;
    flex-wrap: nowrap;
    align-items: center;
    gap: 10px;
    max-width: 100%;
    white-space: nowrap;
  }

  /* gap 已承担间距，避免与 margin-left 叠成 20px */
  .row > .margin-left-10 {
    margin-left: 0;
  }
</style>
