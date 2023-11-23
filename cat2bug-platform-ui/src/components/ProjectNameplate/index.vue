<template>
  <div class="project-nameplate">
    <el-image
      style="width: 50px; height: 50px;flex-basis:auto;"
      :src="project.projectIcon"
      fit="cover"
    ></el-image>
    <div class="project-nameplate-content">
      <p>{{ project.projectName }}</p>
      <span v-if="project.projectIntroduce">{{ project.projectIntroduce }}</span>
      <span v-else style="color: #DCDFE6;">{{ project.projectIntroduce?project.projectIntroduce:$t('project.introduction-not-filled') }}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: "MemberNameplate",
  props: {
    project: {
      type: Object,
      default: {}
    }
  }
}
</script>

<style lang="scss" scoped>
  .project-nameplate {
    display: flex;
    align-items: center;
    flex-wrap: nowrap;
    padding: 5px;
    overflow: hidden;
    .el-image {
      flex-shrink: 0;
    }
    > * {
      margin-right: 10px;
    }
    .project-nameplate-content {
      display: flex;
      align-items: flex-start;
      flex-direction: column;
      width: 100%;
      overflow: hidden;
      p {
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
        margin: 0px;
        font-size: 16px;
        width: 100%;
      }
      span {
        font-size: 12px;
        color: #C0C4CC;
        line-height: 12px;
        height: 12px;
        margin-top: 3px;
        width: 100%;
      }
    }
  }
</style>
