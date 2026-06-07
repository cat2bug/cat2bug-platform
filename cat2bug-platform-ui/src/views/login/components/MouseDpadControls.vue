<template>
  <div class="mouse-dpad-root">
    <!-- 专用键盘焦点：macOS 上 ⌘+方向键的 keyup 只有聚焦元素能稳定收到 -->
    <div
      ref="keyboardSink"
      class="mouse-dpad-keyboard-sink"
      tabindex="-1"
      aria-hidden="true"
      @keydown="onSinkKeyDown"
      @keyup="onSinkKeyUp"
      @focusout="onKeyboardSinkFocusOut"
    />
    <div
      v-show="metaHeld"
      class="mouse-dpad"
      role="group"
      :aria-label="$t('login.mouse-dpad')"
    >
      <button
        type="button"
        class="mouse-dpad__btn mouse-dpad__btn--up"
        :class="{ 'is-active': activeKeys.ArrowUp }"
        :aria-label="$t('login.mouse-dpad-up')"
        @mousedown.prevent
        @pointerdown.prevent="onPointerPress('ArrowUp', $event)"
        @pointerup.prevent="onPointerRelease('ArrowUp')"
        @pointercancel.prevent="onPointerRelease('ArrowUp')"
        @pointerleave="onPointerLeave('ArrowUp', $event)"
      >
        <i class="el-icon-arrow-up" />
        <span class="mouse-dpad__hint" aria-hidden="true">↑</span>
      </button>
      <button
        type="button"
        class="mouse-dpad__btn mouse-dpad__btn--left"
        :class="{ 'is-active': activeKeys.ArrowLeft }"
        :aria-label="$t('login.mouse-dpad-left')"
        @mousedown.prevent
        @pointerdown.prevent="onPointerPress('ArrowLeft', $event)"
        @pointerup.prevent="onPointerRelease('ArrowLeft')"
        @pointercancel.prevent="onPointerRelease('ArrowLeft')"
        @pointerleave="onPointerLeave('ArrowLeft', $event)"
      >
        <i class="el-icon-arrow-left" />
        <span class="mouse-dpad__hint" aria-hidden="true">←</span>
      </button>
      <button
        type="button"
        class="mouse-dpad__btn mouse-dpad__btn--down"
        :class="{ 'is-active': activeKeys.ArrowDown }"
        :aria-label="$t('login.mouse-dpad-down')"
        @mousedown.prevent
        @pointerdown.prevent="onPointerPress('ArrowDown', $event)"
        @pointerup.prevent="onPointerRelease('ArrowDown')"
        @pointercancel.prevent="onPointerRelease('ArrowDown')"
        @pointerleave="onPointerLeave('ArrowDown', $event)"
      >
        <i class="el-icon-arrow-down" />
        <span class="mouse-dpad__hint" aria-hidden="true">↓</span>
      </button>
      <button
        type="button"
        class="mouse-dpad__btn mouse-dpad__btn--right"
        :class="{ 'is-active': activeKeys.ArrowRight }"
        :aria-label="$t('login.mouse-dpad-right')"
        @mousedown.prevent
        @pointerdown.prevent="onPointerPress('ArrowRight', $event)"
        @pointerup.prevent="onPointerRelease('ArrowRight')"
        @pointercancel.prevent="onPointerRelease('ArrowRight')"
        @pointerleave="onPointerLeave('ArrowRight', $event)"
      >
        <i class="el-icon-arrow-right" />
        <span class="mouse-dpad__hint" aria-hidden="true">→</span>
      </button>
    </div>
  </div>
</template>

<script>
import {
  ARROW_KEYS,
  OPPOSITE_ARROW_KEYS,
  createArrowKeyReleasePoller,
  lockArrowKeys,
  markArrowEventHandled,
  resolveArrowKey,
  unlockArrowKeys
} from './mouse-arrow-keys'

const RUNNER_KEYS_EVENT = 'cat2bug-mouse-runner-keys'

function getMouseRunner() {
  if (typeof window === 'undefined') return null
  return window.__cat2bugMouseRunner || null
}

export default {
  name: 'MouseDpadControls',
  data() {
    return {
      metaHeld: false,
      arrowKeysHeld: new Set(),
      pointerKeysDown: new Set(),
      activeKeys: {
        ArrowUp: false,
        ArrowDown: false,
        ArrowLeft: false,
        ArrowRight: false
      },
      pointerCapture: new Map()
    }
  },
  mounted() {
    this.$_arrowReleasePoller = createArrowKeyReleasePoller({
      getHeldKeys: () => this.arrowKeysHeld,
      onRelease: (key) => this.handleArrowKeyUp(key)
    })
    this.$_onMetaSync = (e) => {
      // 方向键 keyup 先于 syncActiveHighlights 处理，避免 ⌘ 仍按住时高亮残留
      if (e.type === 'keyup') {
        const arrowKey = resolveArrowKey(e)
        if (arrowKey && (e.metaKey || e.ctrlKey)) {
          this.handleArrowKeyUp(arrowKey)
        }
      }
      const next = !!(e.metaKey || e.ctrlKey)
      if (this.metaHeld && !next) {
        this.clearMovementState()
        unlockArrowKeys()
      } else if (!this.metaHeld && next) {
        this.resetForMetaOpen()
        this.focusKeyboardSink()
        lockArrowKeys()
      }
      this.metaHeld = next
      this.syncActiveHighlights()
    }
    this.$_onArrowKeyDown = (e) => this.handleArrowKeyDown(e)
    this.$_onArrowKeyUp = (e) => this.handleArrowKeyUpEvent(e)
    this.$_onWindowBlur = () => {
      this.metaHeld = false
      this.clearMovementState()
      unlockArrowKeys()
    }
    this.$_arrowKeyBound = [
      [window, 'keydown', this.$_onMetaSync, true],
      [window, 'keydown', this.$_onArrowKeyDown, true],
      [window, 'keyup', this.$_onMetaSync, true],
      [window, 'keyup', this.$_onArrowKeyUp, true]
    ]
    this.$_arrowKeyBound.forEach(([target, type, handler, capture]) => {
      target.addEventListener(type, handler, capture)
    })
    window.addEventListener('blur', this.$_onWindowBlur)
    if (typeof window !== 'undefined') {
      window.__cat2bugMouseDpad = this
    }
  },
  beforeDestroy() {
    this.releaseAllPointerKeys()
    unlockArrowKeys()
    if (this.$_arrowReleasePoller) {
      this.$_arrowReleasePoller.reset()
      this.$_arrowReleasePoller = null
    }
    if (this.$_arrowKeyBound) {
      this.$_arrowKeyBound.forEach(([target, type, handler, capture]) => {
        target.removeEventListener(type, handler, capture)
      })
      this.$_arrowKeyBound = null
    }
    window.removeEventListener('blur', this.$_onWindowBlur)
    if (typeof window !== 'undefined' && window.__cat2bugMouseDpad === this) {
      delete window.__cat2bugMouseDpad
    }
  },
  methods: {
    isKeyboardSinkFocused() {
      const sink = this.$refs.keyboardSink
      return !!(sink && document.activeElement === sink)
    },
    focusKeyboardSink() {
      this.$nextTick(() => {
        const el = this.$refs.keyboardSink
        if (!el || !this.metaHeld) return
        try {
          el.focus({ preventScroll: true })
        } catch (err) {
          el.focus()
        }
      })
    },
    onKeyboardSinkFocusOut() {
      if (!this.metaHeld) return
      this.focusKeyboardSink()
    },
    blurKeyboardSink() {
      const el = this.$refs.keyboardSink
      if (el && document.activeElement === el) el.blur()
    },
    touchArrowKeyActivity(key, isRepeat) {
      if (this.$_arrowReleasePoller) {
        this.$_arrowReleasePoller.touch(key, isRepeat)
      }
    },
    handleArrowKeyDown(e) {
      if (!this.metaHeld) return
      if (this.isKeyboardSinkFocused()) return
      const key = resolveArrowKey(e)
      if (!key) return
      this.touchArrowKeyActivity(key, e.repeat)
      this.pressArrowKeyHeld(key)
      this.pressRunnerKey(key)
    },
    handleArrowKeyUpEvent(e) {
      if (markArrowEventHandled(e)) return
      const key = resolveArrowKey(e)
      if (!key) return
      this.handleArrowKeyUp(key)
    },
    handleArrowKeyUp(key) {
      if (!ARROW_KEYS.includes(key)) return
      this.$_arrowReleasePoller.clear(key)
      this.releaseArrowKeyHeld(key, { syncRunner: true })
    },
    onSinkKeyDown(e) {
      if (!this.metaHeld || e.isComposing) return
      const key = resolveArrowKey(e)
      if (!key) return
      e.preventDefault()
      this.touchArrowKeyActivity(key, e.repeat)
      this.pressArrowKeyHeld(key)
      this.pressRunnerKey(key)
    },
    onSinkKeyUp(e) {
      if (!this.metaHeld) return
      const key = resolveArrowKey(e)
      if (!key) return
      e.preventDefault()
      if (markArrowEventHandled(e)) return
      this.handleArrowKeyUp(key)
    },
    clearOppositeArrowHeld(key) {
      const opposites = OPPOSITE_ARROW_KEYS[key]
      if (!opposites) return
      opposites.forEach((k) => {
        this.arrowKeysHeld.delete(k)
        this.$_arrowReleasePoller.clear(k)
        this.setKeyActive(k, false)
      })
    },
    pressArrowKeyHeld(key) {
      if (!ARROW_KEYS.includes(key) || !this.metaHeld) return
      this.clearOppositeArrowHeld(key)
      this.arrowKeysHeld.add(key)
      this.syncActiveHighlights()
    },
    releaseArrowKeyHeld(key, opts = {}) {
      if (!ARROW_KEYS.includes(key)) return
      const syncRunner = opts.syncRunner !== false
      this.arrowKeysHeld.delete(key)
      this.$_arrowReleasePoller.clear(key)
      this.setKeyActive(key, false)
      if (syncRunner) this.releaseRunnerKey(key)
      this.syncActiveHighlights()
    },
    releaseAllArrowKeysHeld() {
      ARROW_KEYS.forEach((key) => {
        if (this.arrowKeysHeld.has(key)) {
          this.releaseArrowKeyHeld(key, { syncRunner: true })
        }
      })
    },
    resetForMetaOpen() {
      this.arrowKeysHeld.clear()
      this.pointerKeysDown.clear()
      this.pointerCapture.clear()
      if (this.$_arrowReleasePoller) this.$_arrowReleasePoller.reset()
      const runner = getMouseRunner()
      if (runner && typeof runner.clearMoveKeys === 'function') {
        runner.clearMoveKeys()
      }
      this.clearActiveHighlights()
    },
    clearMovementState() {
      this.arrowKeysHeld.clear()
      this.pointerKeysDown.clear()
      this.pointerCapture.clear()
      if (this.$_arrowReleasePoller) this.$_arrowReleasePoller.reset()
      const runner = getMouseRunner()
      if (runner && typeof runner.clearMoveKeys === 'function') {
        runner.clearMoveKeys()
      }
      this.clearActiveHighlights()
      this.blurKeyboardSink()
    },
    syncActiveHighlights() {
      if (!this.metaHeld) {
        this.clearActiveHighlights()
        return
      }
      ARROW_KEYS.forEach((key) => {
        this.setKeyActive(key, this.arrowKeysHeld.has(key))
      })
    },
    clearActiveHighlights() {
      ARROW_KEYS.forEach((key) => this.setKeyActive(key, false))
    },
    setKeyActive(key, active) {
      if (!ARROW_KEYS.includes(key)) return
      this.$set(this.activeKeys, key, active)
    },
    pressRunnerKey(key) {
      const runner = getMouseRunner()
      if (runner && typeof runner.pressMoveKey === 'function') {
        runner.pressMoveKey(key)
      }
    },
    releaseRunnerKey(key) {
      const runner = getMouseRunner()
      if (runner && typeof runner.releaseMoveKey === 'function') {
        runner.releaseMoveKey(key)
      }
    },
    releaseAllPointerKeys() {
      ARROW_KEYS.forEach((key) => this.releasePointerKey(key))
    },
    releasePointerKey(key) {
      const cap = this.pointerCapture.get(key)
      if (cap && cap.el && cap.el.releasePointerCapture) {
        try {
          cap.el.releasePointerCapture(cap.pointerId)
        } catch (err) {
          /* ignore */
        }
      }
      this.pointerCapture.delete(key)
      if (!this.pointerKeysDown.has(key)) return
      this.pointerKeysDown.delete(key)
      this.releaseArrowKeyHeld(key, { syncRunner: true })
    },
    onPointerPress(key, e) {
      if (this.pointerKeysDown.has(key)) return
      if (e.currentTarget && e.pointerId != null && e.currentTarget.setPointerCapture) {
        try {
          e.currentTarget.setPointerCapture(e.pointerId)
          this.pointerCapture.set(key, { el: e.currentTarget, pointerId: e.pointerId })
        } catch (err) {
          /* ignore */
        }
      }
      this.pressArrowKeyHeld(key)
      this.pointerKeysDown.add(key)
      this.pressRunnerKey(key)
    },
    onPointerRelease(key) {
      this.releasePointerKey(key)
    },
    onPointerLeave(key, e) {
      if (e.buttons !== 0) {
        this.releasePointerKey(key)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.mouse-dpad-keyboard-sink {
  position: fixed;
  left: 0;
  top: 0;
  width: 1px;
  height: 1px;
  opacity: 0;
  overflow: hidden;
  outline: none;
  z-index: 0;
}

.mouse-dpad {
  position: fixed;
  left: 20px;
  bottom: 20px;
  z-index: 6;
  display: grid;
  grid-template-columns: 50px 50px 50px;
  grid-template-rows: 50px 50px 50px;
  gap: 6px;
  pointer-events: auto;
  user-select: none;
  -webkit-user-select: none;
}

.mouse-dpad__btn {
  width: 50px;
  height: 50px;
  margin: 0;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 10px;
  background: rgba(30, 30, 32, 0.38);
  color: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  cursor: pointer;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.18);
  transition: background 0.15s ease, border-color 0.15s ease, transform 0.08s ease;

  i {
    font-size: 18px;
    font-weight: 700;
    line-height: 1;
  }

  &:hover {
    background: rgba(40, 40, 44, 0.52);
    border-color: rgba(255, 255, 255, 0.42);
  }

  &.is-active {
    background: rgba(255, 193, 7, 0.28);
    border-color: rgba(255, 193, 7, 0.65);
    color: #ffc107;
    transform: scale(0.96);
  }
}

html.dark .mouse-dpad__btn.is-active .mouse-dpad__hint {
  opacity: 1;
  color: #ffc107;
}

.mouse-dpad__hint {
  font-size: 10px;
  line-height: 1;
  opacity: 0.75;
  font-weight: 600;
}

.mouse-dpad__btn--up {
  grid-column: 2;
  grid-row: 1;
}

.mouse-dpad__btn--left {
  grid-column: 1;
  grid-row: 2;
}

.mouse-dpad__btn--down {
  grid-column: 2;
  grid-row: 2;
}

.mouse-dpad__btn--right {
  grid-column: 3;
  grid-row: 2;
}

html:not(.dark) .mouse-dpad__btn {
  border-color: rgba(0, 0, 0, 0.12);
  background: rgba(255, 255, 255, 0.55);
  color: rgba(48, 49, 51, 0.92);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);

  &:hover {
    background: rgba(255, 255, 255, 0.72);
    border-color: rgba(0, 0, 0, 0.18);
  }

  &.is-active {
    background: rgba(64, 158, 255, 0.28);
    border-color: rgba(64, 158, 255, 0.55);
  }
}
</style>
