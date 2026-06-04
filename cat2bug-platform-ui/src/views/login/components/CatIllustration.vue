<template>
  <div class="cat-illustration">
    <img
      class="cat-scene-shot"
      :src="sceneImage"
      alt=""
      draggable="false"
    />
    <div class="zzz-anchor" aria-hidden="true">
      <span class="zzz-bubble zzz-bubble--small">z</span>
      <span class="zzz-bubble zzz-bubble--large">Z</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CatIllustration',
  data() {
    return {
      isDark: document.documentElement.classList.contains('dark'),
      themeObserver: null
    }
  },
  computed: {
    sceneImage() {
      return this.isDark
        ? require('@/assets/images/login-cat-scene-dark.jpg')
        : require('@/assets/images/login-cat-scene-light.jpg')
    }
  },
  mounted() {
    this.themeObserver = new MutationObserver(() => {
      this.isDark = document.documentElement.classList.contains('dark')
    })
    this.themeObserver.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['class']
    })
  },
  beforeDestroy() {
    if (this.themeObserver) {
      this.themeObserver.disconnect()
      this.themeObserver = null
    }
  }
}
</script>

<style lang="scss" scoped>
.cat-illustration {
  position: relative;
  width: 100%;
  max-width: 550px;
  display: flex;
  justify-content: center;
  align-items: flex-end;
  margin: 0 auto -1px;
  z-index: 2;
  pointer-events: none;
}

.cat-scene-shot {
  width: 100%;
  max-width: 550px;
  height: auto;
  display: block;
  object-fit: contain;
  border-radius: 16px 16px 0 0;
  user-select: none;
}

.zzz-anchor {
  position: absolute;
  left: 34%;
  top: 40%;
  z-index: 3;
  pointer-events: none;
}

.zzz-bubble {
  position: absolute;
  color: var(--login-zzz-color, #e5e7eb);
  font-weight: bold;
  font-family: 'Comic Sans MS', 'Chalkboard SE', cursive, sans-serif;
  animation: zzzRise 2.4s infinite ease-out;
}

html:not(.dark) .zzz-bubble {
  color: #6b7280;
}

.zzz-bubble--small {
  font-size: 14px;
  top: 8px;
}

.zzz-bubble--large {
  font-size: 22px;
  left: 10px;
  top: -6px;
  animation-delay: 0.55s;
}

@keyframes zzzRise {
  0% {
    transform: translate(0, 0);
    opacity: 0;
  }
  15% {
    opacity: 1;
  }
  100% {
    transform: translate(6px, -40px);
    opacity: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .zzz-bubble {
    animation: none;
    opacity: 0.75;
  }
}
</style>
