<template>
  <el-empty
    class="cat2bug-empty"
    :class="sizeClass"
    :style="rootStyle"
    :description="description"
    :image-size="resolvedImageSize"
  >
    <template #image>
      <div class="cat2bug-empty__stage" :style="stageStyle">
        <span class="cat2bug-empty__shadow" aria-hidden="true" />
        <img
          class="cat2bug-empty__art"
          :src="emptyBoxImage"
          alt=""
          draggable="false"
        >
      </div>
    </template>
    <template v-if="$slots.description" #description>
      <slot name="description" />
    </template>
    <slot />
  </el-empty>
</template>

<script>
import emptyBoxImage from '@/assets/images/cat2bug-empty-box.png'

/** Element 风格三档：同时驱动插画与说明文字 */
const SIZE_PRESETS = {
  small: { image: 80, font: 12 },
  default: { image: 120, font: 14 },
  large: { image: 260, font: 14 }
}

/**
 * Cat2Bug 统一空状态（包 el-empty）。
 * - description：底部文案
 * - size：small / default / large（图+字一起变）
 * - imageSize：数字覆盖图尺寸（兼容旧调用）
 * 箱子缓慢上下浮；上移时阴影略小，下移时阴影略大。
 */
export default {
  name: 'Cat2BugEmpty',
  props: {
    description: {
      type: String,
      default: ''
    },
    /** Element 风格：small | default | large */
    size: {
      type: String,
      default: 'default',
      validator: (v) => ['small', 'default', 'large'].includes(v)
    },
    /** 可选：精确覆盖插画边长（px），优先于 size */
    imageSize: {
      type: Number,
      default: undefined
    }
  },
  data() {
    return { emptyBoxImage }
  },
  computed: {
    preset() {
      return SIZE_PRESETS[this.size] || SIZE_PRESETS.default
    },
    resolvedImageSize() {
      const n = Number(this.imageSize)
      if (Number.isFinite(n) && n > 0) {
        return Math.max(48, n)
      }
      return this.preset.image
    },
    sizeClass() {
      return `cat2bug-empty--${this.size}`
    },
    rootStyle() {
      return {
        '--cat2bug-empty-font': `${this.preset.font}px`
      }
    },
    stageStyle() {
      const size = this.resolvedImageSize
      return {
        width: `${size}px`,
        height: `${size}px`
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.cat2bug-empty {
  padding: 8px 0 4px;

  :deep(.el-empty__image) {
    margin-bottom: 4px;
    filter: none !important;
    opacity: 1 !important;
    width: auto !important;
  }

  :deep(.el-empty__description) {
    margin-top: 10px;
    line-height: 1.55;
    color: var(--text-color-secondary);
    font-size: var(--cat2bug-empty-font, 14px);
  }

  :deep(.el-empty__description p) {
    margin: 0;
    color: var(--text-color-secondary) !important;
    font-size: inherit;
  }
}

.cat2bug-empty--small {
  :deep(.el-empty__description) {
    margin-top: 6px;
  }
}

.cat2bug-empty--large {
  :deep(.el-empty__description) {
    margin-top: 12px;
  }
}

.cat2bug-empty__stage {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
}

.cat2bug-empty__shadow {
  position: absolute;
  left: 22%;
  right: 22%;
  bottom: 7%;
  height: 11%;
  border-radius: 50%;
  background: radial-gradient(
    ellipse 100% 100% at 50% 50%,
    color-mix(in srgb, var(--text-color-secondary) 20%, transparent) 0%,
    transparent 72%
  );
  pointer-events: none;
  z-index: 0;
  transform-origin: center center;
  animation: cat2bug-empty-shadow 5.6s ease-in-out infinite;
}

.cat2bug-empty__art {
  position: relative;
  z-index: 1;
  width: 88%;
  height: auto;
  display: block;
  object-fit: contain;
  transform-origin: center bottom;
  animation: cat2bug-empty-float 5.6s ease-in-out infinite;
  will-change: transform;
}

html.dark {
  .cat2bug-empty__shadow {
    background: radial-gradient(
      ellipse 100% 100% at 50% 50%,
      rgba(0, 0, 0, 0.42) 0%,
      transparent 72%
    );
  }

  /* 暗色再压一档，贴近页面底，避免浅灰箱子过亮 */
  .cat2bug-empty__art {
    opacity: 0.78;
    filter: brightness(0.58) contrast(0.95);
  }
}

/* 箱上移 → 影略小略淡；箱略下 → 影略大略实 */
@keyframes cat2bug-empty-float {
  0%,
  100% {
    transform: translateY(0);
  }
  45% {
    transform: translateY(-7px);
  }
  70% {
    transform: translateY(2px);
  }
}

@keyframes cat2bug-empty-shadow {
  0%,
  100% {
    transform: scaleX(1) scaleY(1);
    opacity: 1;
  }
  45% {
    transform: scaleX(0.82) scaleY(0.78);
    opacity: 0.72;
  }
  70% {
    transform: scaleX(1.1) scaleY(1.12);
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .cat2bug-empty__art,
  .cat2bug-empty__shadow {
    animation: none;
  }
}

@supports not (background: color-mix(in srgb, #000 10%, transparent)) {
  .cat2bug-empty__shadow {
    background: radial-gradient(
      ellipse 100% 100% at 50% 50%,
      rgba(144, 147, 153, 0.2) 0%,
      transparent 72%
    );
  }
}
</style>
