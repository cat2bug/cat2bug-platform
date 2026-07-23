<template>
  <span class="git-open-source-icon" aria-hidden="true" />
</template>

<script>
/** 开源可见性：OSI 风格圆环，PNG mask 随 currentColor 着色 */
export default {
  name: 'GitOpenSourceIcon'
}
</script>

<style lang="scss" scoped>
.git-open-source-icon {
  display: inline-block;
  width: 1em;
  height: 1em;
  flex-shrink: 0;
  vertical-align: middle;
  background-color: currentColor;
  -webkit-mask: url('@/assets/icons/git/open-source.png') center / contain no-repeat;
  mask: url('@/assets/icons/git/open-source.png') center / contain no-repeat;
}
</style>
