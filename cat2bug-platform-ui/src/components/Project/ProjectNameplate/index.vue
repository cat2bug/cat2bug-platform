<template>
  <div class="project-nameplate">
    <cat2-bug-image
      style="width: 50px; height: 50px;flex-basis:auto;"
      :src="projectIconUrl"
      fit="cover"
    ></cat2-bug-image>
    <div class="project-nameplate-content">
      <p>{{ project.projectName }}</p>
      <span v-if="project.projectIntroduce">{{ project.projectIntroduce }}</span>
      <span v-else style="color: #DCDFE6;">{{ project.projectIntroduce?project.projectIntroduce:$t('project.introduction-not-filled') }}</span>
    </div>
  </div>
</template>

<script>
import Cat2BugImage from '@/components/Cat2BugImage'
import { resolveProjectIconUrl } from '@/utils/upload-asset'

export default {
  name: "ProjectNameplate",
  components: { Cat2BugImage },
  props: {
    project: {
      type: Object,
      default: {}
    }
  },
  computed: {
    projectIconUrl() {
      return resolveProjectIconUrl(this.project && this.project.projectIcon)
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
      border-radius: 4px;
      overflow: hidden;
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
