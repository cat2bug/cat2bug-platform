<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <project-base-info-card v-model="project" />
      </el-col>
      <el-col :span="6">
        <project-member-card v-model="project" />
      </el-col>
    </el-row>
  </div>
</template>

<script>
import ProjectBaseInfoCard from './item/base'
import ProjectMemberCard from './item/member'
import {getProject} from "@/api/system/project";

export default {
  name: "ProjectOption",
  components: { ProjectBaseInfoCard, ProjectMemberCard },
  data() {
    return {
      project: {}
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
