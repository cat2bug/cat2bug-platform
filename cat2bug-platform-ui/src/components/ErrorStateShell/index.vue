<template>
  <div class="error-state-shell">
    <div class="error-state-shell__inner">
      <img
        class="error-state-shell__art"
        :src="artSrc"
        alt=""
        draggable="false"
      >
      <div
        class="error-state-shell__code"
        :class="{ 'error-state-shell__code--word': isWordCode }"
      >
        {{ code }}
      </div>
      <h1 v-if="title" class="error-state-shell__title">{{ title }}</h1>
      <div v-if="description || $slots.description" class="error-state-shell__desc">
        <slot name="description">
          <p>{{ description }}</p>
        </slot>
      </div>
      <div v-if="$slots.actions" class="error-state-shell__actions">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script>
import { storeToRefs } from 'pinia'
import { useSettingsStore } from '@/stores/settings'
import art404Light from '@/assets/images/error/404-light.png'
import art404Dark from '@/assets/images/error/404-dark.png'
import art401Light from '@/assets/images/error/401-light.png'
import art401Dark from '@/assets/images/error/401-dark.png'
import artDisabledLight from '@/assets/images/error/disabled-light.png'
import artDisabledDark from '@/assets/images/error/disabled-dark.png'

const ART = {
  '404': { light: art404Light, dark: art404Dark },
  '401': { light: art401Light, dark: art401Dark },
  disabled: { light: artDisabledLight, dark: artDisabledDark }
}

function readDomDark() {
  if (typeof document === 'undefined') return false
  return document.documentElement.classList.contains('dark')
}

export default {
  name: 'ErrorStateShell',
  props: {
    /** 展示用状态码或词，如 404 / 401 / TEAM / PROJECT */
    code: {
      type: String,
      required: true
    },
    title: {
      type: String,
      default: ''
    },
    description: {
      type: String,
      default: ''
    },
    /** 插画变体：404 | 401 | disabled */
    illustration: {
      type: String,
      required: true,
      validator: (v) => ['404', '401', 'disabled'].includes(v)
    }
  },
  setup() {
    const settingsStore = useSettingsStore()
    const { themeMode } = storeToRefs(settingsStore)
    return { themeMode }
  },
  data() {
    return {
      domDark: readDomDark()
    }
  },
  computed: {
    isWordCode() {
      return !/^\d+$/.test(String(this.code || '').trim())
    },
    artSrc() {
      const pack = ART[this.illustration] || ART['404']
      // 与 html.dark / --bg-color-page 同源；依赖 domDark 以便 class 变化时重算
      return this.domDark ? pack.dark : pack.light
    }
  },
  watch: {
    themeMode() {
      this.syncDomDark()
    }
  },
  mounted() {
    this.syncDomDark()
    this._themeObserver = new MutationObserver(() => {
      this.syncDomDark()
    })
    this._themeObserver.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['class']
    })
  },
  beforeUnmount() {
    if (this._themeObserver) {
      this._themeObserver.disconnect()
      this._themeObserver = null
    }
  },
  methods: {
    syncDomDark() {
      this.domDark = readDomDark()
    }
  }
}
</script>

<style lang="scss" scoped>
.error-state-shell {
  box-sizing: border-box;
  /* 挂在 Layout / app-main 内：铺满主内容区，保留侧栏与顶栏，避免整屏空洞 */
  width: 100%;
  height: 100%;
  min-height: calc(100vh - 84px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 20px 64px;
  background: transparent;
}

.error-state-shell__inner {
  width: 100%;
  max-width: 320px;
  text-align: center;
}

.error-state-shell__art {
  display: block;
  width: auto;
  height: auto;
  max-width: 220px;
  max-height: 180px;
  margin: 0 auto 12px;
  object-fit: contain;
  user-select: none;
  pointer-events: none;
}

.error-state-shell__code {
  margin: 0 0 8px;
  font-size: 48px;
  line-height: 1.05;
  font-weight: 700;
  color: var(--el-color-primary, #1890ff);
}

.error-state-shell__code--word {
  font-size: 28px;
  letter-spacing: 0.06em;
}

.error-state-shell__title {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--text-color-primary, #303133);
}

.error-state-shell__desc {
  margin: 0 0 20px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--text-color-secondary, #909399);

  p {
    margin: 0 0 8px;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.error-state-shell__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  align-items: center;
}
</style>
