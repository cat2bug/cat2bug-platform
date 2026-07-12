<template>
  <el-scrollbar ref="scrollContainer" :vertical="false" class="scroll-container" @wheel.prevent="handleScroll">
    <slot />
  </el-scrollbar>
</template>

<script>
const tagAndTagSpacing = 4 // tagAndTagSpacing

export default {
  name: 'ScrollPane',
  data() {
    return {
      left: 0
    }
  },
  computed: {
    scrollWrapper() {
      return this.$refs.scrollContainer?.wrapRef
    }
  },
  mounted() {
    this.scrollWrapper?.addEventListener('scroll', this.emitScroll, true)
  },
  beforeUnmount() {
    this.scrollWrapper?.removeEventListener('scroll', this.emitScroll)
  },
  methods: {
    handleScroll(e) {
      const eventDelta = e.wheelDelta || -e.deltaY * 40
      const $scrollWrapper = this.scrollWrapper
      if (!$scrollWrapper) return
      $scrollWrapper.scrollLeft = $scrollWrapper.scrollLeft + eventDelta / 4
    },
    emitScroll() {
      this.$emit('scroll')
    },
    moveToTarget(currentTag) {
      const $container = this.$refs.scrollContainer?.$el
      const $scrollWrapper = this.scrollWrapper
      if (!$container || !$scrollWrapper) return

      const $containerWidth = $container.offsetWidth
      const tagList = this.$parent.$refs.tag

      let firstTag = null
      let lastTag = null

      if (tagList.length > 0) {
        firstTag = tagList[0]
        lastTag = tagList[tagList.length - 1]
      }

      const tagEl = currentTag?.$el

      if (firstTag === currentTag) {
        $scrollWrapper.scrollLeft = 0
      } else if (lastTag === currentTag) {
        $scrollWrapper.scrollLeft = $scrollWrapper.scrollWidth - $containerWidth
      } else if (tagEl) {
        const currentIndex = tagList.findIndex(item => item === currentTag)
        const prevTag = tagList[currentIndex - 1]
        const nextTag = tagList[currentIndex + 1]

        const afterNextTagOffsetLeft = nextTag.$el.offsetLeft + nextTag.$el.offsetWidth + tagAndTagSpacing
        const beforePrevTagOffsetLeft = prevTag.$el.offsetLeft - tagAndTagSpacing

        if (afterNextTagOffsetLeft > $scrollWrapper.scrollLeft + $containerWidth) {
          $scrollWrapper.scrollLeft = afterNextTagOffsetLeft - $containerWidth
        } else if (beforePrevTagOffsetLeft < $scrollWrapper.scrollLeft) {
          $scrollWrapper.scrollLeft = beforePrevTagOffsetLeft
        }
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.scroll-container {
  white-space: nowrap;
  position: relative;
  overflow: hidden;
  width: 100%;
  :deep(.el-scrollbar__bar) {
    bottom: 0px;
  }
  :deep(.el-scrollbar__wrap) {
    height: 49px;
  }
}
</style>
