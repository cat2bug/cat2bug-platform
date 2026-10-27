<template>
  <div
    class="cat2bug-loading"
    :class="[
      `cat2bug-loading--${resolvedVariant}`,
      { 'cat2bug-loading--reduced': reduceMotion }
    ]"
    :style="rootStyle"
  >
    <div class="cat2bug-loading__runner">
      <span class="cat2bug-loading__dust" aria-hidden="true">
        <i /><i /><i /><i />
      </span>
      <div class="cat2bug-loading__cat-wrap">
        <div class="cat2bug-loading__stage">
          <div class="cat2bug-loading__run" aria-hidden="true" />
        </div>
        <span class="cat2bug-loading__shade" aria-hidden="true" />
      </div>
    </div>
    <p class="cat2bug-loading__label">{{ displayText }}</p>
  </div>
</template>

<script>
import sheetLight from '@/assets/images/loading/run-sheet.png'
import sheetDark from '@/assets/images/loading/run-sheet-dark.png'
import { useSettingsStore } from '@/stores/settings'

/** 帧画布 382×280；6 帧；默认 130ms/帧（见预览 v8） */
const FRAME_W = 382
const FRAME_H = 280
const FRAME_N = 6
const FRAME_MS = 130

/** 加载猫高度标准（px），与 docs/04-UI规范/组件与交互约定.md 一致；enhance 与直挂组件共用 */
export const CAT2BUG_LOADING_SIZE = Object.freeze({
  /** 列表页 / 表格 / 区域默认 */
  list: 44,
  /** 统计卡、工具条 KPI 等矮格 */
  stat: 24
})

/**
 * Cat2Bug 品牌加载：奔跑猫轮廓 + 文案。
 * props: text / size(高度 px) / variant(light|dark|auto)
 */
export default {
  name: 'Cat2BugLoading',
  props: {
    text: {
      type: String,
      default: undefined
    },
    size: {
      type: Number,
      default: CAT2BUG_LOADING_SIZE.list
    },
    variant: {
      type: String,
      default: 'auto',
      validator: (v) => ['light', 'dark', 'auto'].includes(v)
    }
  },
  data() {
    return {
      reduceMotion: false,
      motionMq: null
    }
  },
  computed: {
    displayText() {
      if (this.text != null && String(this.text).length) {
        return this.text
      }
      return this.$t('loading')
    },
    themeMode() {
      try {
        return useSettingsStore().themeMode
      } catch (_e) {
        return document.documentElement.classList.contains('dark') ? 'dark' : 'light'
      }
    },
    resolvedVariant() {
      if (this.variant === 'light' || this.variant === 'dark') {
        return this.variant
      }
      return this.themeMode === 'dark' ? 'dark' : 'light'
    },
    catH() {
      const n = Number(this.size)
      return Number.isFinite(n) && n > 0 ? n : CAT2BUG_LOADING_SIZE.list
    },
    rootStyle() {
      const sheet = this.resolvedVariant === 'dark' ? sheetDark : sheetLight
      const h = this.catH
      // 小尺寸时收紧间距与字号，适配统计卡片等窄容器
      const gap = Math.max(4, Math.round(h * 0.22))
      const labelSize = Math.max(11, Math.min(14, Math.round(h * 0.42)))
      return {
        '--cat-h': `${h}px`,
        '--cat-gap': `${gap}px`,
        '--cat-label-size': `${labelSize}px`,
        '--fw': String(FRAME_W),
        '--fh': String(FRAME_H),
        '--n': String(FRAME_N),
        '--ms': String(FRAME_MS),
        '--cycle': `${FRAME_MS * FRAME_N}ms`,
        '--sheet': `url(${sheet})`
      }
    }
  },
  mounted() {
    if (typeof window === 'undefined' || !window.matchMedia) return
    this.motionMq = window.matchMedia('(prefers-reduced-motion: reduce)')
    this.reduceMotion = !!this.motionMq.matches
    this._onMotion = () => {
      this.reduceMotion = !!this.motionMq.matches
    }
    if (this.motionMq.addEventListener) {
      this.motionMq.addEventListener('change', this._onMotion)
    } else if (this.motionMq.addListener) {
      this.motionMq.addListener(this._onMotion)
    }
  },
  beforeUnmount() {
    if (!this.motionMq || !this._onMotion) return
    if (this.motionMq.removeEventListener) {
      this.motionMq.removeEventListener('change', this._onMotion)
    } else if (this.motionMq.removeListener) {
      this.motionMq.removeListener(this._onMotion)
    }
  }
}
</script>

<style lang="scss" scoped>
.cat2bug-loading {
  --dust: rgba(96, 98, 102, 0.45);
  --shade: rgba(96, 98, 102, 0.35);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--cat-gap, 12px);
  box-sizing: border-box;
}

.cat2bug-loading--dark {
  --dust: rgba(255, 255, 255, 0.35);
  --shade: rgba(255, 255, 255, 0.28);
}

.cat2bug-loading__runner {
  position: relative;
  display: inline-flex;
  align-items: flex-end;
  justify-content: center;
  height: var(--cat-h);
  /* 左右对称留白：尘土占左侧，右侧等宽，整体几何中心对齐猫身 */
  padding-left: calc(var(--cat-h) * 0.55);
  padding-right: calc(var(--cat-h) * 0.55);
  overflow: visible;
}

.cat2bug-loading__cat-wrap {
  position: relative;
  z-index: 2;
  width: calc(var(--cat-h) * var(--fw) / var(--fh));
  height: var(--cat-h);
}

.cat2bug-loading__stage {
  position: relative;
  z-index: 2;
  width: 100%;
  height: 100%;
  overflow: hidden;
  /* 促进合成层，避免缺陷列表等重渲染时主线程堵死帧动画 */
  transform: translateZ(0);
}

.cat2bug-loading__shade {
  position: absolute;
  left: 50%;
  top: calc(var(--cat-h) * 0.91);
  z-index: 1;
  width: 86%;
  height: calc(var(--cat-h) * 0.11);
  border-radius: 50%;
  background: var(--shade);
  pointer-events: none;
  transform-origin: center center;
  transform: translate3d(-50%, 0, 0) scale(1, 1);
  animation: cat2bug-shade-pulse var(--cycle) infinite;
  will-change: transform;
}

/*
 * 用 transform 抽帧，不用 background-position：
 * 后者走主线程 paint，缺陷列表大数据渲染时猫会「卡住不跑」。
 */
.cat2bug-loading__run {
  width: calc(100% * var(--n));
  height: 100%;
  background-image: var(--sheet);
  background-repeat: no-repeat;
  background-size: 100% 100%;
  background-position: 0 0;
  transform: translate3d(0, 0, 0);
  animation: cat2bug-run-steps var(--cycle) steps(6) infinite;
  will-change: transform;
}

.cat2bug-loading__dust {
  position: absolute;
  left: 0;
  bottom: calc(var(--cat-h) * 0.12);
  width: calc(var(--cat-h) * 0.55);
  height: calc(var(--cat-h) * 0.45);
  pointer-events: none;
  z-index: 1;

  i {
    position: absolute;
    display: block;
    border-radius: 50%;
    background: var(--dust);
    animation: cat2bug-dust-puff 480ms ease-out infinite;
  }

  i:nth-child(1) {
    width: 5px;
    height: 5px;
    left: 55%;
    bottom: 20%;
    animation-delay: 0ms;
  }
  i:nth-child(2) {
    width: 3.5px;
    height: 3.5px;
    left: 40%;
    bottom: 35%;
    animation-delay: 160ms;
  }
  i:nth-child(3) {
    width: 4px;
    height: 4px;
    left: 48%;
    bottom: 8%;
    animation-delay: 320ms;
  }
  i:nth-child(4) {
    width: 2.5px;
    height: 2.5px;
    left: 30%;
    bottom: 25%;
    animation-delay: 240ms;
  }
}

.cat2bug-loading__label {
  margin: 0;
  font-size: var(--cat-label-size, 14px);
  line-height: 1.4;
  text-align: center;
  width: calc(var(--cat-h) * var(--fw) / var(--fh));
  white-space: nowrap;
  color: var(--el-text-color-regular, #606266);
}

.cat2bug-loading--dark .cat2bug-loading__label {
  color: rgba(255, 255, 255, 0.85);
}

.cat2bug-loading--reduced {
  .cat2bug-loading__run,
  .cat2bug-loading__shade,
  .cat2bug-loading__dust i {
    animation: none !important;
  }
}

@keyframes cat2bug-run-steps {
  from {
    transform: translate3d(0, 0, 0);
  }
  to {
    /* 条带宽 = 6 帧；steps(6) 停在第 0～5 帧 */
    transform: translate3d(-100%, 0, 0);
  }
}

@keyframes cat2bug-shade-pulse {
  0%,
  16.666% {
    transform: translate3d(-50%, 0, 0) scale(1, 1);
  }
  16.667%,
  33.333% {
    transform: translate3d(-50%, 0, 0) scale(0.82, 0.92);
  }
  33.334%,
  50% {
    transform: translate3d(-50%, 0, 0) scale(0.95, 1.05);
  }
  50.001%,
  66.666% {
    transform: translate3d(-50%, 0, 0) scale(0.77, 0.85);
  }
  66.667%,
  83.333% {
    transform: translate3d(-50%, 0, 0) scale(0.95, 0.95);
  }
  83.334%,
  100% {
    transform: translate3d(-50%, 0, 0) scale(0.76, 0.88);
  }
}

@keyframes cat2bug-dust-puff {
  0% {
    opacity: 0;
    transform: translate(0, 0) scale(0.4);
  }
  25% {
    opacity: 0.7;
    transform: translate(-2px, -1px) scale(1);
  }
  100% {
    opacity: 0;
    transform: translate(calc(var(--cat-h) * -0.45), calc(var(--cat-h) * -0.2)) scale(1.6);
  }
}
</style>
