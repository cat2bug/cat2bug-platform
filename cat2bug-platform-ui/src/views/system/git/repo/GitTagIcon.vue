<template>
  <span class="git-tag-icon" aria-hidden="true" />
</template>

<script>
/** 发行版 Tag：产品 tag-o 线框吊牌（尖角朝下、顶部圆孔），PNG mask 保真着色 */
export default {
  name: 'GitTagIcon'
}
</script>

<style lang="scss" scoped>
.git-tag-icon {
  display: inline-block;
  width: 1em;
  height: 1em;
  flex-shrink: 0;
  vertical-align: middle;
  background-color: currentColor;
  -webkit-mask: url('@/assets/icons/git/tag-o.png') center / contain no-repeat;
  mask: url('@/assets/icons/git/tag-o.png') center / contain no-repeat;
}
</style>
