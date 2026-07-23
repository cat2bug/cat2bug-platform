<template>
  <span class="git-release-icon" aria-hidden="true" />
</template>

<script>
/** 发行版：纸飞机线框图标（PNG mask，随 currentColor 着色） */
export default {
  name: 'GitReleaseIcon'
}
</script>

<style lang="scss" scoped>
.git-release-icon {
  display: inline-block;
  width: 1em;
  height: 1em;
  flex-shrink: 0;
  vertical-align: middle;
  background-color: currentColor;
  -webkit-mask: url('@/assets/icons/git/release.png') center / contain no-repeat;
  mask: url('@/assets/icons/git/release.png') center / contain no-repeat;
}
</style>
