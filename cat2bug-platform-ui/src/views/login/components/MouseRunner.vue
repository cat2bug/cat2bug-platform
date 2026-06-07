<template>
  <div ref="container" class="mouse-runner-container" tabindex="0">
    <canvas ref="canvas" class="mouse-canvas" />
  </div>
</template>

<script>
import { markArrowEventHandled, resolveArrowKey } from './mouse-arrow-keys'

/** 横向奔跑精灵：1 行×8 列，朝右；朝左由 flipX 镜像 */
const RUN_SPRITE_COLS = 8
const RUN_SPRITE_FRAME_W = 214
const RUN_SPRITE_FRAME_H = 96
const RUN_SPRITE_DISPLAY_SCALE = 0.42
/** 整体显示缩放（相对默认尺寸再小 1/4） */
const MOUSE_DISPLAY_SCALE = 0.75
/** 脚底阴影：宽高与偏移均按当前帧 drawW/drawH 同比缩放 */
const SHADOW_RX_RATIO = 0.38
const SHADOW_RY_RATIO = 0.14
const SHADOW_Y_OFFSET_RATIO = 0.045
const SHADOW_ALPHA_DARK = 0.52
const SHADOW_ALPHA_LIGHT = 0.16
const SHADOW_ALPHA_SIZE_FLOOR = 0.55
const SHADOW_ALPHA_SIZE_CEIL = 1
const RUN_SPRITE_FRAME_FOOT_Y = [1, 1, 1, 1, 1, 1, 1, 1]
const RUN_TL_SPRITE_COLS = 8
const RUN_TL_SPRITE_FRAME_W = 203
const RUN_TL_SPRITE_FRAME_H = 117
const RUN_TL_SPRITE_DISPLAY_SCALE = 0.4
const RUN_TL_SPRITE_FRAME_FOOT_Y = [0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9]
const RUN_BL_SPRITE_COLS = 8
const RUN_BL_SPRITE_FRAME_W = 196
const RUN_BL_SPRITE_FRAME_H = 156
const RUN_BL_SPRITE_DISPLAY_SCALE = 0.3
const RUN_BL_SPRITE_FRAME_FOOT_Y = [0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9]
const RUN_T_SPRITE_COLS = 8
const RUN_T_SPRITE_FRAME_W = 113
const RUN_T_SPRITE_FRAME_H = 286
const RUN_T_SPRITE_DISPLAY_SCALE = 0.25
const RUN_T_SPRITE_FRAME_FOOT_Y = [0.7, 0.7, 0.7, 0.7, 0.7, 0.7, 0.7, 0.7]
const RUN_B_SPRITE_COLS = 8
const RUN_B_SPRITE_FRAME_W = 94
const RUN_B_SPRITE_FRAME_H = 211
const RUN_B_SPRITE_DISPLAY_SCALE = 0.25
const RUN_B_SPRITE_FRAME_FOOT_Y = [0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9]

const SIT_SPRITE_COLS = 8
const SIT_SPRITE_FRAME_W = 179
const SIT_SPRITE_FRAME_H = 191
const SIT_SPRITE_DISPLAY_SCALE = 0.22
const SIT_SPRITE_FRAME_FOOT_Y = [0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9]

const SPRITE_ROW_RIGHT = 2
const SPRITE_ROW_LEFT = 6
const SPRITE_ROW_UP_RIGHT = 1
const SPRITE_ROW_UP_LEFT = 7
const SPRITE_ROW_DOWN_RIGHT = 3
const SPRITE_ROW_DOWN_LEFT = 5
const SPRITE_ROW_SIT = 8
const SIT_DURATION = 2.8
const AUTO_SPEED = 330
const USER_LERP = 15
const RUN_FPS = 14
const FAR_SPAWN_CHANCE = 0.3
const AUTO_TURN_WAIT_MIN = 5
const AUTO_TURN_WAIT_MAX = 10
const AUTO_TURN_Y_JITTER = 50
const EDGE_PAD = 56
const MOUSE_MAX_Y_INSET = 50
const MOVE_KEY_CODES = new Set([
  'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight',
  'w', 'a', 's', 'd', 'W', 'A', 'S', 'D'
])
/** 同轴反向键：避免左右/上下同时残留导致 dx/dy 抵消、老鼠不动 */
const OPPOSITE_AXIS_KEYS = {
  ArrowLeft: ['ArrowRight', 'd', 'D'],
  ArrowRight: ['ArrowLeft', 'a', 'A'],
  ArrowUp: ['ArrowDown', 's', 'S'],
  ArrowDown: ['ArrowUp', 'w', 'W'],
  a: ['ArrowRight', 'd', 'D'],
  A: ['ArrowRight', 'd', 'D'],
  d: ['ArrowLeft', 'a', 'A'],
  D: ['ArrowLeft', 'a', 'A'],
  w: ['ArrowDown', 's', 'S'],
  W: ['ArrowDown', 's', 'S'],
  s: ['ArrowUp', 'w', 'W'],
  S: ['ArrowUp', 'w', 'W']
}
const FORM_BARRIER_OFFSET = 0
const FORM_BARRIER_Y_EPS = 8
const Z_MOUSE_ABOVE_FORM = 4
const Z_MOUSE_BEHIND_FORM = 1
/** Retina 屏 Canvas 按 DPR 放大位图，避免 CSS 拉伸导致块状/发糊 */
const MAX_DEVICE_PIXEL_RATIO = 2
/** 朝下精灵纵向占比大，略缩小以与横向/斜向视觉一致（不随 Y 变化，保证越往下越大） */
const DOWN_RUN_SIZE_FACTOR = 0.84
/** 纵深：顶小底大；最底相对原 0.95 缩小 1/3 */
const DEPTH_SCALE_MIN = 0.55
const DEPTH_SCALE_TOP_RANGE = 0.4
const DEPTH_SCALE_MAX = (DEPTH_SCALE_MIN + DEPTH_SCALE_TOP_RANGE) * (2 / 3)
/** 贴底时左右奔跑、坐立动画在通用贴底缩放基础上再放大约 1/3 */
const BOTTOM_POSE_SIZE_BOOST = 4 / 3
const BOTTOM_POSE_BOOST_START = 0.7

export default {
  name: 'MouseRunner',
  data() {
    return {
      state: 'AUTO_RUN',
      animationFrame: null,
      ctx: null,
      runSpriteImage: null,
      sitSpriteImage: null,
      isSpriteLoaded: false,
      rmbDown: false,
      reducedMotion: false,
      reducedMotionMq: null,
      boundsResizeObserver: null,
      playWidth: 0,
      playHeight: 0,
      canvasDpr: 1
    }
  },
  mounted() {
    this.initAnimState()
    this.initCanvas()
    this.loadSprite()
    this.setupReducedMotion()
    this.resetAutoRun(true)
    this.scheduleBoundsRefresh()
    this.setupLayoutObserver()
    document.addEventListener('contextmenu', this.onContextMenu)
    document.addEventListener('mousedown', this.onDocumentMouseDown)
    document.addEventListener('mousemove', this.onDocumentMouseMove)
    document.addEventListener('mouseup', this.onDocumentMouseUp)
    window.addEventListener('keydown', this.onKeyDown, true)
    window.addEventListener('keyup', this.onKeyUp, true)
    window.addEventListener('resize', this.handleResize)
    this.lastTime = performance.now()
    this.animationFrame = requestAnimationFrame(this.update)
    if (typeof window !== 'undefined') {
      window.__cat2bugMouseRunner = this
    }
  },
  beforeDestroy() {
    if (typeof window !== 'undefined' && window.__cat2bugMouseRunner === this) {
      delete window.__cat2bugMouseRunner
    }
    document.removeEventListener('contextmenu', this.onContextMenu)
    document.removeEventListener('mousedown', this.onDocumentMouseDown)
    document.removeEventListener('mousemove', this.onDocumentMouseMove)
    document.removeEventListener('mouseup', this.onDocumentMouseUp)
    window.removeEventListener('keydown', this.onKeyDown, true)
    window.removeEventListener('keyup', this.onKeyUp, true)
    window.removeEventListener('resize', this.handleResize)
    if (this.reducedMotionMq) {
      this.reducedMotionMq.removeEventListener('change', this.onReducedMotionChange)
    }
    if (this.boundsResizeObserver) {
      this.boundsResizeObserver.disconnect()
      this.boundsResizeObserver = null
    }
    cancelAnimationFrame(this.animationFrame)
  },
  methods: {
    initAnimState() {
      Object.assign(this, {
        mouseX: 0,
        mouseY: 0,
        direction: 1,
        speed: AUTO_SPEED,
        groundY: 0,
        maxMouseY: 0,
        minMouseY: 0,
        frameIndex: 0,
        frameTimer: 0,
        flipX: false,
        spriteRow: 0,
        particles: [],
        lastTime: 0,
        pointerX: 0,
        pointerY: 0,
        sitElapsed: 0,
        farSpawn: false,
        farSpawnT: 0,
        farStartY: 0,
        keysDown: new Set(),
        minMouseX: EDGE_PAD,
        maxMouseX: EDGE_PAD,
        formLeft: null,
        formRight: null,
        formBottom: null,
        formBarrierY: null,
        autoTurnWaitElapsed: 0,
        autoTurnWaitDuration: 0,
        pendingDirection: 1,
        pendingExitY: 0
      })
    },
    isFormField(el) {
      if (!el || !el.tagName) return false
      const tag = el.tagName.toLowerCase()
      return tag === 'input' || tag === 'textarea' || tag === 'select' || el.isContentEditable
    },
    hasMovementKeys() {
      for (const key of this.keysDown) {
        if (MOVE_KEY_CODES.has(key)) return true
      }
      return false
    },
    isWithinFormX() {
      return this.formLeft != null
        && this.mouseX >= this.formLeft
        && this.mouseX <= this.formRight
    },
    isOnFormBarrierLine() {
      return this.formBarrierY != null
        && this.isWithinFormX()
        && Math.abs(this.mouseY - this.formBarrierY) <= FORM_BARRIER_Y_EPS
    },
    getKeyMovement() {
      let dx = 0
      let dy = 0
      const keys = this.keysDown
      if (keys.has('ArrowLeft') || keys.has('a') || keys.has('A')) dx -= 1
      if (keys.has('ArrowRight') || keys.has('d') || keys.has('D')) dx += 1
      if (keys.has('ArrowUp') || keys.has('w') || keys.has('W')) dy -= 1
      if (keys.has('ArrowDown') || keys.has('s') || keys.has('S')) dy += 1
      if (this.isOnFormBarrierLine() && dy < 0) dy = 0
      if (dx === 0 && dy === 0) return { dx: 0, dy: 0 }
      const len = Math.hypot(dx, dy)
      return { dx: dx / len, dy: dy / len }
    },
    updateMouseLayer() {
      const el = this.$refs.container
      if (!el) return
      if (this.formBottom == null) {
        el.style.zIndex = String(Z_MOUSE_BEHIND_FORM)
        return
      }
      // Y 大于 form 底边：在表单前；Y 小于等于 form 底边：在表单后
      const aboveForm = this.mouseY > this.formBottom - FORM_BARRIER_OFFSET
      el.style.zIndex = String(aboveForm ? Z_MOUSE_ABOVE_FORM : Z_MOUSE_BEHIND_FORM)
    },
    resolveMoveKey(e) {
      const arrow = resolveArrowKey(e)
      if (arrow) return arrow
      if (!e || !MOVE_KEY_CODES.has(e.key)) return null
      return e.key
    },
    onKeyDown(e) {
      const key = this.resolveMoveKey(e)
      if (!key) return
      const isArrow = key.startsWith('Arrow')
      if (isArrow) {
        const inForm = this.isFormField(e.target)
        const withModifier = e.metaKey || e.ctrlKey
        if (inForm && !withModifier) return
        if (inForm) e.preventDefault()
      } else if (this.isFormField(e.target)) {
        return
      } else {
        e.preventDefault()
      }
      this.pressMoveKey(key)
      const dpad = typeof window !== 'undefined' ? window.__cat2bugMouseDpad : null
      if (dpad && dpad.metaHeld && key.startsWith('Arrow') && typeof dpad.pressArrowKeyHeld === 'function') {
        dpad.pressArrowKeyHeld(key)
        if (typeof dpad.touchArrowKeyActivity === 'function') {
          dpad.touchArrowKeyActivity(key, e.repeat)
        }
      }
    },
    onKeyUp(e) {
      if (markArrowEventHandled(e)) return
      const key = this.resolveMoveKey(e)
      if (!key) return
      this.releaseMoveKey(key)
      const dpad = typeof window !== 'undefined' ? window.__cat2bugMouseDpad : null
      if (dpad && key.startsWith('Arrow') && typeof dpad.handleArrowKeyUp === 'function') {
        dpad.handleArrowKeyUp(key)
      }
    },
    notifyMoveKeysChanged() {
      if (typeof window === 'undefined') return
      window.dispatchEvent(new CustomEvent('cat2bug-mouse-runner-keys'))
    },
    exitKeyboardControlIfIdle() {
      if (!this.hasMovementKeys() && this.state === 'KEYBOARD_CONTROL') {
        this.enterSitLook()
      }
    },
    clearOppositeAxisKeys(key) {
      const opposites = OPPOSITE_AXIS_KEYS[key]
      if (!opposites) return
      opposites.forEach((k) => this.keysDown.delete(k))
    },
    /** 供左下角方向键控件调用 */
    pressMoveKey(key) {
      if (!MOVE_KEY_CODES.has(key)) return
      this.clearOppositeAxisKeys(key)
      this.keysDown.add(key)
      if (this.rmbDown) {
        this.notifyMoveKeysChanged()
        return
      }
      this.farSpawn = false
      this.state = 'KEYBOARD_CONTROL'
      this.notifyMoveKeysChanged()
    },
    releaseMoveKey(key) {
      if (!MOVE_KEY_CODES.has(key)) return
      this.keysDown.delete(key)
      if (this.rmbDown) {
        this.notifyMoveKeysChanged()
        return
      }
      this.exitKeyboardControlIfIdle()
      this.notifyMoveKeysChanged()
    },
    clearMoveKeys() {
      this.keysDown.clear()
      if (!this.rmbDown) {
        this.exitKeyboardControlIfIdle()
      }
      this.notifyMoveKeysChanged()
    },
    resizeCanvas() {
      const canvas = this.$refs.canvas
      const container = this.$refs.container
      if (!canvas || !container) return
      const dpr = Math.min(window.devicePixelRatio || 1, MAX_DEVICE_PIXEL_RATIO)
      const w = Math.max(1, container.clientWidth)
      const h = Math.max(1, container.clientHeight)
      canvas.width = Math.round(w * dpr)
      canvas.height = Math.round(h * dpr)
      canvas.style.width = `${w}px`
      canvas.style.height = `${h}px`
      const ctx = canvas.getContext('2d')
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      ctx.imageSmoothingEnabled = true
      if (typeof ctx.imageSmoothingQuality !== 'undefined') {
        ctx.imageSmoothingQuality = 'high'
      }
      this.ctx = ctx
      this.canvasDpr = dpr
      this.playWidth = w
      this.playHeight = h
      this.syncPlayBounds(canvas)
    },
    initCanvas() {
      this.resizeCanvas()
    },
    handleResize() {
      this.resizeCanvas()
    },
    refreshPlayBounds() {
      const canvas = this.$refs.canvas
      if (canvas) this.syncPlayBounds(canvas)
    },
    scheduleBoundsRefresh() {
      const tick = () => this.refreshPlayBounds()
      this.$nextTick(tick)
      requestAnimationFrame(() => requestAnimationFrame(tick))
      setTimeout(tick, 100)
      setTimeout(tick, 500)
    },
    setupLayoutObserver() {
      if (typeof ResizeObserver === 'undefined') return
      this.boundsResizeObserver = new ResizeObserver(() => this.refreshPlayBounds())
      this.$nextTick(() => {
        const form = document.querySelector('.logo-page .login-form')
        const body = document.querySelector('.logo-page .login-body')
        if (form) this.boundsResizeObserver.observe(form)
        if (body) this.boundsResizeObserver.observe(body)
        const catImg = document.querySelector('.logo-page .cat-scene-shot')
        if (catImg && !catImg.complete) {
          catImg.addEventListener('load', this.refreshPlayBounds, { once: true })
        }
      })
    },
    syncPlayBounds(canvas) {
      const canvasRect = canvas.getBoundingClientRect()
      const form = document.querySelector('.logo-page .login-form')
      const ph = this.playHeight || canvasRect.height
      let minY = Math.max(24, ph * 0.2)
      if (form) {
        const fr = form.getBoundingClientRect()
        const formHalfY = fr.top + fr.height / 2 - canvasRect.top
        minY = Math.max(24, formHalfY)
        this.formLeft = fr.left - canvasRect.left
        this.formRight = fr.right - canvasRect.left
        this.formBottom = fr.bottom - canvasRect.top
        this.formBarrierY = this.formBottom - FORM_BARRIER_OFFSET
      } else {
        this.formLeft = null
        this.formRight = null
        this.formBottom = null
        this.formBarrierY = null
      }
      this.maxMouseY = ph - MOUSE_MAX_Y_INSET
      this.groundY = Math.min(ph * 0.88, this.maxMouseY)
      this.minMouseY = minY
      this.refreshHorizontalBounds()
      if (this.mouseY < this.minMouseY) this.mouseY = this.minMouseY
      if (this.mouseY > this.maxMouseY) this.mouseY = this.maxMouseY
      if (this.isManualControlState()) this.clampManualPosition()
    },
    canvasPointFromEvent(e) {
      const canvas = this.$refs.canvas
      if (!canvas) return null
      const rect = canvas.getBoundingClientRect()
      const x = e.clientX - rect.left
      const y = e.clientY - rect.top
      if (x < 0 || x > rect.width || y < 0 || y > rect.height) return null
      return { x, y }
    },
    isManualControlState() {
      return this.state === 'KEYBOARD_CONTROL'
        || this.state === 'USER_CONTROL'
        || this.state === 'SIT_LOOK_AROUND'
    },
    /** 各朝向精灵在当前纵深下的最大半宽，保证贴边时整只老鼠仍在画面内 */
    maxSpriteHalfWidth() {
      const rows = [
        SPRITE_ROW_RIGHT,
        SPRITE_ROW_LEFT,
        SPRITE_ROW_UP_RIGHT,
        SPRITE_ROW_UP_LEFT,
        SPRITE_ROW_DOWN_RIGHT,
        SPRITE_ROW_DOWN_LEFT,
        0,
        4,
        SPRITE_ROW_SIT
      ]
      let maxHalf = EDGE_PAD
      for (const row of rows) {
        const { drawW } = this.getDrawDimensions(row)
        maxHalf = Math.max(maxHalf, drawW / 2)
      }
      return maxHalf
    },
    refreshHorizontalBounds() {
      const pw = this.playWidth || 0
      const half = this.maxSpriteHalfWidth()
      this.minMouseX = half
      this.maxMouseX = Math.max(half, pw - half)
    },
    clampManualPosition() {
      this.refreshHorizontalBounds()
      const minX = this.minMouseX ?? EDGE_PAD
      const maxX = this.maxMouseX ?? Math.max(minX, (this.playWidth || 0) - EDGE_PAD)
      this.mouseX = Math.max(minX, Math.min(maxX, this.mouseX))
      this.mouseY = Math.max(this.minMouseY, Math.min(this.maxMouseY, this.mouseY))
    },
    /** 折返开跑时：在基准 Y 基础上随机 ±AUTO_TURN_Y_JITTER */
    jitterMouseYForTurn(baseY) {
      const jitter = (Math.random() * 2 - 1) * AUTO_TURN_Y_JITTER
      return Math.max(this.minMouseY, Math.min(this.maxMouseY, baseY + jitter))
    },
    setupReducedMotion() {
      if (typeof window === 'undefined' || !window.matchMedia) return
      this.reducedMotionMq = window.matchMedia('(prefers-reduced-motion: reduce)')
      this.reducedMotion = this.reducedMotionMq.matches
      this.onReducedMotionChange = () => {
        this.reducedMotion = this.reducedMotionMq.matches
      }
      this.reducedMotionMq.addEventListener('change', this.onReducedMotionChange)
    },
    loadSprite() {
      let pending = 6
      const onReady = () => {
        pending -= 1
        if (pending <= 0) this.isSpriteLoaded = true
      }
      this.runSpriteImage = new Image()
      this.runTlSpriteImage = new Image()
      this.runBlSpriteImage = new Image()
      this.runTSpriteImage = new Image()
      this.runBSpriteImage = new Image()
      this.sitSpriteImage = new Image()
      this.runSpriteImage.src = require('@/assets/images/mouse_sprite_run_horizontal_v2.png')
      this.runTlSpriteImage.src = require('@/assets/images/mouse_sprite_run_top_left.png')
      this.runBlSpriteImage.src = require('@/assets/images/mouse_sprite_run_bottom_left.png')
      this.runTSpriteImage.src = require('@/assets/images/mouse_sprite_run_top.png')
      this.runBSpriteImage.src = require('@/assets/images/mouse_sprite_run_bottom.png')
      this.sitSpriteImage.src = require('@/assets/images/mouse_sprite_sit_v3.png')
      this.runSpriteImage.onload = onReady
      this.runTlSpriteImage.onload = onReady
      this.runBlSpriteImage.onload = onReady
      this.runTSpriteImage.onload = onReady
      this.runBSpriteImage.onload = onReady
      this.sitSpriteImage.onload = onReady
    },
    isHorizontalRunRow(row) {
      return row === SPRITE_ROW_RIGHT || row === SPRITE_ROW_LEFT
    },
    isTopLeftRunRow(row) {
      return row === SPRITE_ROW_UP_LEFT || row === SPRITE_ROW_UP_RIGHT
    },
    isBottomLeftRunRow(row) {
      return row === SPRITE_ROW_DOWN_LEFT || row === SPRITE_ROW_DOWN_RIGHT
    },
    isTopRunRow(row) {
      return row === 0 // SPRITE_ROW_UP
    },
    isBottomRunRow(row) {
      return row === 4 // SPRITE_ROW_DOWN
    },
    isDownFacingRunRow(row) {
      return this.isBottomRunRow(row) || this.isBottomLeftRunRow(row)
    },
    isSitRow(row) {
      return row === SPRITE_ROW_SIT
    },
    onContextMenu(e) {
      if (this.isFormField(e.target)) return
      if (!this.canvasPointFromEvent(e)) return
      e.preventDefault()
    },
    onDocumentMouseDown(e) {
      if (e.button !== 2 || this.isFormField(e.target)) return
      const pt = this.canvasPointFromEvent(e)
      if (!pt) return
      e.preventDefault()
      this.rmbDown = true
      this.farSpawn = false
      this.state = 'USER_CONTROL'
      this.pointerX = pt.x
      this.pointerY = pt.y
    },
    onDocumentMouseMove(e) {
      if (!this.rmbDown || this.state !== 'USER_CONTROL') return
      const pt = this.canvasPointFromEvent(e)
      if (!pt) return
      this.pointerX = pt.x
      this.pointerY = pt.y
    },
    onDocumentMouseUp(e) {
      if (e.button !== 2) return
      if (this.rmbDown) this.endUserControl()
    },
    enterSitLook() {
      this.state = 'SIT_LOOK_AROUND'
      this.sitElapsed = 0
      this.sitDuration = 3 + Math.random() * 2 // 3 to 5 seconds
      this.spriteRow = SPRITE_ROW_SIT
      this.frameIndex = 0
      this.frameTimer = 0
      this.particles = []
    },
    endUserControl() {
      if (!this.rmbDown) return
      this.rmbDown = false
      if (this.hasMovementKeys()) {
        this.state = 'KEYBOARD_CONTROL'
        return
      }
      this.enterSitLook()
    },
    randomDirection() {
      return Math.random() < 0.5 ? -1 : 1
    },
    resetAutoRun(initial = false, exitY = null) {
      const canvas = this.$refs.canvas
      if (!canvas) return
      this.state = 'AUTO_RUN'
      this.direction = this.randomDirection()
      const margin = 100
      this.mouseX = this.direction > 0 ? -margin : this.playWidth + margin
      this.farSpawn = !initial && exitY == null && Math.random() < FAR_SPAWN_CHANCE
      if (this.farSpawn) {
        this.farSpawnT = 0
        const skyY = this.playHeight * (0.35 + Math.random() * 0.2)
        this.farStartY = Math.max(this.minMouseY, skyY)
        this.mouseY = this.farStartY
        this.speed = AUTO_SPEED * 0.75
      } else if (exitY != null) {
        const jitter = Math.random() * 100 - 50
        this.mouseY = Math.max(this.minMouseY, Math.min(this.maxMouseY, exitY + jitter))
        this.speed = AUTO_SPEED
      } else {
        this.mouseY = this.groundY
        this.speed = AUTO_SPEED
      }
      this.spriteRow = this.direction > 0 ? SPRITE_ROW_RIGHT : SPRITE_ROW_LEFT
      this.flipX = this.direction < 0
      this.frameIndex = 0
      this.frameTimer = 0
    },
    resumeAutoRunFromCurrent() {
      const canvas = this.$refs.canvas
      if (!canvas) return
      this.state = 'AUTO_RUN'
      this.direction = this.randomDirection()
      this.farSpawn = false
      this.speed = AUTO_SPEED
      this.clampManualPosition()
      this.spriteRow = this.direction > 0 ? SPRITE_ROW_RIGHT : SPRITE_ROW_LEFT
      this.flipX = this.direction < 0
      this.frameIndex = 0
      this.frameTimer = 0
    },
    playDepthT() {
      const span = Math.max(1, (this.maxMouseY ?? 0) - (this.minMouseY ?? 0))
      return Math.max(0, Math.min(1, (this.mouseY - (this.minMouseY ?? 0)) / span))
    },
    depthScale() {
      const t = this.playDepthT()
      return DEPTH_SCALE_MIN + t * (DEPTH_SCALE_MAX - DEPTH_SCALE_MIN)
    },
    pixelScale() {
      const d = this.depthScale()
      return d * 1.15
    },
    /** 贴底段放大系数（左右奔跑、坐立） */
    bottomPoseSizeBoost() {
      const t = this.playDepthT()
      if (t <= BOTTOM_POSE_BOOST_START) return 1
      const ease = (t - BOTTOM_POSE_BOOST_START) / (1 - BOTTOM_POSE_BOOST_START)
      return 1 + ease * (BOTTOM_POSE_SIZE_BOOST - 1)
    },
    /** 贴底横向奔跑时的参考 visualMax，用于阴影与老鼠大小同比 */
    referenceHorizontalVisualMax() {
      const refMax = RUN_SPRITE_FRAME_W * RUN_SPRITE_DISPLAY_SCALE * MOUSE_DISPLAY_SCALE
      return refMax * DEPTH_SCALE_MAX * 1.15 * BOTTOM_POSE_SIZE_BOOST
    },
    /** 阴影尺寸/透明度随当前帧绘制大小实时变化 */
    computeShadowMetrics(drawW, drawH) {
      const ref = Math.max(1, this.referenceHorizontalVisualMax())
      const sizeMul = Math.min(1.15, Math.max(drawW, drawH) / ref)
      let rx = drawW * SHADOW_RX_RATIO
      let ry = drawH * SHADOW_RY_RATIO
      const yOffset = drawH * SHADOW_Y_OFFSET_RATIO
      const alphaBase = document.documentElement.classList.contains('dark')
        ? SHADOW_ALPHA_DARK
        : SHADOW_ALPHA_LIGHT
      let alpha = alphaBase * (
        SHADOW_ALPHA_SIZE_FLOOR + sizeMul * (SHADOW_ALPHA_SIZE_CEIL - SHADOW_ALPHA_SIZE_FLOOR)
      )
      if (this.farSpawn) {
        const airLift = Math.max(0, (this.groundY || this.mouseY) - this.mouseY)
        const airFade = Math.min(0.28, airLift / Math.max(drawH * 2.5, 90))
        const fade = 1 - airFade
        rx *= fade
        ry *= fade
        alpha *= fade
      }
      return { rx, ry, yOffset, alpha, sizeMul }
    },
    pickUserSpriteRow(dx, dy) {
      if (dx === 0 && dy === 0) return
      let a = Math.atan2(dy, dx)
      if (a < 0) a += 2 * Math.PI
      const octant = Math.round(a / (Math.PI / 4)) % 8
      // Assuming rows: 0:Up, 1:Up-Right, 2:Right, 3:Down-Right, 4:Down, 5:Down-Left, 6:Left, 7:Up-Left
      // Octant 0 (Right) -> 2, Octant 1 (Down-Right) -> 3, Octant 2 (Down) -> 4, Octant 3 (Down-Left) -> 5
      // Octant 4 (Left) -> 6, Octant 5 (Up-Left) -> 7, Octant 6 (Up) -> 0, Octant 7 (Up-Right) -> 1
      const rowMap = [2, 3, 4, 5, 6, 7, 0, 1]
      this.spriteRow = rowMap[octant]
      this.flipX = this.spriteRow === SPRITE_ROW_LEFT || this.spriteRow === SPRITE_ROW_UP_RIGHT || this.spriteRow === SPRITE_ROW_DOWN_RIGHT
    },
    rowFrameCount() {
      if (this.isHorizontalRunRow(this.spriteRow)) return RUN_SPRITE_COLS
      if (this.isTopLeftRunRow(this.spriteRow)) return RUN_TL_SPRITE_COLS
      if (this.isBottomLeftRunRow(this.spriteRow)) return RUN_BL_SPRITE_COLS
      if (this.isTopRunRow(this.spriteRow)) return RUN_T_SPRITE_COLS
      if (this.isBottomRunRow(this.spriteRow)) return RUN_B_SPRITE_COLS
      if (this.isSitRow(this.spriteRow)) return SIT_SPRITE_COLS
      return RUN_SPRITE_COLS
    },
    createDust(dx, dy, canvas) {
      if (this.state === 'SIT_LOOK_AROUND' || this.state === 'AUTO_TURN_WAIT') return
      if (Math.random() > 0.4) return
      const isDark = document.documentElement.classList.contains('dark')
      this.particles.push({
        x: this.mouseX - dx * 10 + (Math.random() * 6 - 3),
        y: this.mouseY - dy * 10 + (Math.random() * 4 - 2) - 5,
        life: 1,
        vx: -dx * 16 + (Math.random() * 6 - 3),
        vy: -dy * 16 + (Math.random() * 4 - 2),
        color: isDark ? 'rgba(156,163,175,' : 'rgba(107,114,128,'
      })
    },
    updateParticles(dt) {
      for (let i = this.particles.length - 1; i >= 0; i--) {
        const p = this.particles[i]
        p.life -= dt * 2.2
        p.x += p.vx * dt
        p.y += p.vy * dt
        if (p.life <= 0) this.particles.splice(i, 1)
      }
    },
    advanceRunFrames(dt, moving) {
      if (!moving) {
        this.frameIndex = 0
        return
      }
      const fps = this.reducedMotion ? RUN_FPS * 0.35 : RUN_FPS
      const frameDur = 1 / fps
      this.frameTimer += dt
      let steps = 0
      while (this.frameTimer >= frameDur && steps < 4) {
        const maxF = this.rowFrameCount()
        this.frameIndex = (this.frameIndex + 1) % maxF
        this.frameTimer -= frameDur
        steps += 1
      }
    },
    beginAutoTurnWait(exitY) {
      this.state = 'AUTO_TURN_WAIT'
      this.autoTurnWaitElapsed = 0
      this.autoTurnWaitDuration = AUTO_TURN_WAIT_MIN + Math.random() * (AUTO_TURN_WAIT_MAX - AUTO_TURN_WAIT_MIN)
      this.pendingDirection = -this.direction
      this.pendingExitY = exitY
      this.farSpawn = false
      this.frameIndex = 0
      this.frameTimer = 0
      this.particles = []
    },
    exitScreenAndTurnBack(canvas) {
      const margin = 100
      if (this.direction > 0 && this.mouseX > this.playWidth + margin) {
        this.mouseX = this.playWidth + margin
        this.beginAutoTurnWait(this.mouseY)
      } else if (this.direction < 0 && this.mouseX < -margin) {
        this.mouseX = -margin
        this.beginAutoTurnWait(this.mouseY)
      }
    },
    updateAutoTurnWait(dt, canvas) {
      this.autoTurnWaitElapsed += dt
      this.spriteRow = this.direction > 0 ? SPRITE_ROW_RIGHT : SPRITE_ROW_LEFT
      this.frameIndex = 0
      if (this.autoTurnWaitElapsed >= this.autoTurnWaitDuration) {
        this.direction = this.pendingDirection
        this.mouseY = this.jitterMouseYForTurn(this.pendingExitY)
        this.state = 'AUTO_RUN'
        this.speed = AUTO_SPEED
        this.spriteRow = this.direction > 0 ? SPRITE_ROW_RIGHT : SPRITE_ROW_LEFT
        this.flipX = this.direction < 0
        this.frameIndex = 0
        this.frameTimer = 0
      }
    },
    updateSit(dt) {
      this.sitElapsed += dt

      this.frameTimer += dt
      const fps = 3
      if (this.frameTimer > 1 / fps) {
        this.frameTimer = 0
        this.frameIndex = (this.frameIndex + 1) % SIT_SPRITE_COLS
      }

      if (this.sitElapsed >= this.sitDuration) {
        this.resumeAutoRunFromCurrent()
      }
    },
    updateAutoRun(dt, canvas) {
      if (this.reducedMotion) return

      if (this.farSpawn) {
        this.farSpawnT = Math.min(1, this.farSpawnT + dt * 0.45)
        this.mouseY = this.farStartY + (this.groundY - this.farStartY) * this.farSpawnT
      }

      const dx = this.direction
      const dy = 0
      this.mouseX += dx * this.speed * dt
      this.spriteRow = dx > 0 ? SPRITE_ROW_RIGHT : SPRITE_ROW_LEFT
      this.flipX = dx < 0
      this.advanceRunFrames(dt, true)
      this.createDust(dx, dy, canvas)
      this.exitScreenAndTurnBack(canvas)
    },
    updateUserControl(dt, canvas) {
      this.refreshHorizontalBounds()
      let tx = Math.max(this.minMouseX, Math.min(this.maxMouseX, this.pointerX))
      let ty = Math.max(this.minMouseY, Math.min(this.maxMouseY, this.pointerY))
      if (this.isOnFormBarrierLine()) {
        ty = Math.max(ty, this.formBarrierY)
      }
      const lerp = 1 - Math.exp(-USER_LERP * dt)
      const prevX = this.mouseX
      const prevY = this.mouseY
      this.mouseX += (tx - this.mouseX) * lerp
      this.mouseY += (ty - this.mouseY) * lerp
      this.clampManualPosition()

      const actualDx = this.mouseX - prevX
      const actualDy = this.mouseY - prevY
      const dist = Math.hypot(actualDx, actualDy)
      const moving = dist > 0.4

      if (moving) {
        this.pickUserSpriteRow(actualDx, actualDy)

        const speedMul = 0.7 + this.depthScale() * 0.5
        this.speed = AUTO_SPEED * speedMul
        this.createDust(actualDx / dist, actualDy / dist, canvas)
      }
      this.advanceRunFrames(dt, moving)
    },
    updateKeyboardControl(dt, canvas) {
      const { dx, dy } = this.getKeyMovement()
      if (dx === 0 && dy === 0) {
        this.exitKeyboardControlIfIdle()
        return
      }

      const prevX = this.mouseX
      const prevY = this.mouseY
      const speedMul = 0.7 + this.depthScale() * 0.5
      const speed = AUTO_SPEED * speedMul
      this.mouseX += dx * speed * dt
      this.mouseY += dy * speed * dt
      this.clampManualPosition()

      const actualDx = this.mouseX - prevX
      const actualDy = this.mouseY - prevY
      const dist = Math.hypot(actualDx, actualDy)
      const moving = dist > 0.01

      if (moving) {
        this.pickUserSpriteRow(actualDx, actualDy)
        this.createDust(actualDx / dist, actualDy / dist, canvas)
      }
      this.advanceRunFrames(dt, true)

      if (!moving) {
        this.frameIndex = 0
      }
    },
    spriteSourceRect(col, row) {
      if (this.isHorizontalRunRow(row)) {
        return {
          sx: col * RUN_SPRITE_FRAME_W,
          sy: 0,
          sw: RUN_SPRITE_FRAME_W,
          sh: RUN_SPRITE_FRAME_H
        }
      }
      if (this.isTopLeftRunRow(row)) {
        return {
          sx: col * RUN_TL_SPRITE_FRAME_W,
          sy: 0,
          sw: RUN_TL_SPRITE_FRAME_W,
          sh: RUN_TL_SPRITE_FRAME_H
        }
      }
      if (this.isBottomLeftRunRow(row)) {
        return {
          sx: col * RUN_BL_SPRITE_FRAME_W,
          sy: 0,
          sw: RUN_BL_SPRITE_FRAME_W,
          sh: RUN_BL_SPRITE_FRAME_H
        }
      }
      if (this.isTopRunRow(row)) {
        return {
          sx: col * RUN_T_SPRITE_FRAME_W,
          sy: 0,
          sw: RUN_T_SPRITE_FRAME_W,
          sh: RUN_T_SPRITE_FRAME_H
        }
      }
      if (this.isBottomRunRow(row)) {
        return {
          sx: col * RUN_B_SPRITE_FRAME_W,
          sy: 0,
          sw: RUN_B_SPRITE_FRAME_W,
          sh: RUN_B_SPRITE_FRAME_H
        }
      }
      return {
        sx: col * SIT_SPRITE_FRAME_W,
        sy: 0,
        sw: SIT_SPRITE_FRAME_W,
        sh: SIT_SPRITE_FRAME_H
      }
    },
    getDrawDimensions(spriteRow = this.spriteRow) {
      const row = spriteRow
      const horizontal = this.isHorizontalRunRow(row)
      const topLeft = this.isTopLeftRunRow(row)
      const bottomLeft = this.isBottomLeftRunRow(row)
      const top = this.isTopRunRow(row)
      const bottom = this.isBottomRunRow(row)
      const sit = this.isSitRow(row)
      const pScale = this.pixelScale()
      const baseW = horizontal ? RUN_SPRITE_FRAME_W : (topLeft ? RUN_TL_SPRITE_FRAME_W : (bottomLeft ? RUN_BL_SPRITE_FRAME_W : (top ? RUN_T_SPRITE_FRAME_W : (bottom ? RUN_B_SPRITE_FRAME_W : SIT_SPRITE_FRAME_W))))
      const baseH = horizontal ? RUN_SPRITE_FRAME_H : (topLeft ? RUN_TL_SPRITE_FRAME_H : (bottomLeft ? RUN_BL_SPRITE_FRAME_H : (top ? RUN_T_SPRITE_FRAME_H : (bottom ? RUN_B_SPRITE_FRAME_H : SIT_SPRITE_FRAME_H))))
      const displayMul = MOUSE_DISPLAY_SCALE
      const dpr = this.canvasDpr || 1
      let drawW
      let drawH
      const poseBoost = this.bottomPoseSizeBoost()
      if (sit) {
        const sizeScale = SIT_SPRITE_DISPLAY_SCALE * displayMul * poseBoost
        drawW = Math.round(baseW * pScale * sizeScale * dpr) / dpr
        drawH = Math.round(baseH * pScale * sizeScale * dpr) / dpr
      } else {
        const refW = RUN_SPRITE_FRAME_W * RUN_SPRITE_DISPLAY_SCALE * displayMul
        const refH = RUN_SPRITE_FRAME_H * RUN_SPRITE_DISPLAY_SCALE * displayMul
        const refMax = Math.max(refW, refH)
        const spriteMax = Math.max(baseW, baseH)
        let fitScale = refMax / Math.max(spriteMax, 1)
        if (this.isHorizontalRunRow(row)) {
          fitScale *= poseBoost
        }
        if (this.isDownFacingRunRow(row)) {
          fitScale *= DOWN_RUN_SIZE_FACTOR
        }
        drawW = Math.round(baseW * pScale * fitScale * dpr) / dpr
        drawH = Math.round(baseH * pScale * fitScale * dpr) / dpr
      }
      return {
        spriteRow: row,
        drawW,
        drawH,
        visualMax: Math.max(drawW, drawH),
        visualArea: drawW * drawH,
        pScale
      }
    },
    drawMouse(ctx, canvas) {
      if (!this.isSpriteLoaded) return

      const row = this.spriteRow
      const horizontal = this.isHorizontalRunRow(row)
      const topLeft = this.isTopLeftRunRow(row)
      const bottomLeft = this.isBottomLeftRunRow(row)
      const top = this.isTopRunRow(row)
      const bottom = this.isBottomRunRow(row)
      const sit = this.isSitRow(row)
      const { drawW, drawH } = this.getDrawDimensions(row)
      const maxCol = horizontal ? RUN_SPRITE_COLS : (topLeft ? RUN_TL_SPRITE_COLS : (bottomLeft ? RUN_BL_SPRITE_COLS : (top ? RUN_T_SPRITE_COLS : (bottom ? RUN_B_SPRITE_COLS : SIT_SPRITE_COLS))))
      const col = this.frameIndex % maxCol
      const footYRatio = horizontal
        ? (RUN_SPRITE_FRAME_FOOT_Y[col] || RUN_SPRITE_FRAME_FOOT_Y[0])
        : (topLeft ? (RUN_TL_SPRITE_FRAME_FOOT_Y[col] || RUN_TL_SPRITE_FRAME_FOOT_Y[0]) : (bottomLeft ? (RUN_BL_SPRITE_FRAME_FOOT_Y[col] || RUN_BL_SPRITE_FRAME_FOOT_Y[0]) : (top ? (RUN_T_SPRITE_FRAME_FOOT_Y[col] || RUN_T_SPRITE_FRAME_FOOT_Y[0]) : (bottom ? (RUN_B_SPRITE_FRAME_FOOT_Y[col] || RUN_B_SPRITE_FRAME_FOOT_Y[0]) : (SIT_SPRITE_FRAME_FOOT_Y[col] || SIT_SPRITE_FRAME_FOOT_Y[0])))))
      const { sx, sy, sw, sh } = this.spriteSourceRect(col, row)
      const img = horizontal ? this.runSpriteImage : (topLeft ? this.runTlSpriteImage : (bottomLeft ? this.runBlSpriteImage : (top ? this.runTSpriteImage : (bottom ? this.runBSpriteImage : this.sitSpriteImage))))
      const flip = horizontal ? row === SPRITE_ROW_LEFT : (topLeft ? row === SPRITE_ROW_UP_LEFT : (bottomLeft ? row === SPRITE_ROW_DOWN_RIGHT : (sit ? false : this.flipX)))
      const dx = this.mouseX - drawW / 2
      const dy = this.mouseY - drawH * footYRatio
      const { rx: shadowRx, ry: shadowRy, yOffset: shadowYOffset, alpha: shadowAlpha } =
        this.computeShadowMetrics(drawW, drawH)
      ctx.beginPath()
      ctx.ellipse(
        this.mouseX,
        this.mouseY + shadowYOffset,
        shadowRx,
        shadowRy,
        0, 0, Math.PI * 2
      )
      ctx.fillStyle = `rgba(0,0,0,${shadowAlpha})`
      ctx.fill()

      ctx.save()
      ctx.imageSmoothingEnabled = true
      if (typeof ctx.imageSmoothingQuality !== 'undefined') {
        ctx.imageSmoothingQuality = 'high'
      }
      if (flip) {
        ctx.translate(dx + drawW, dy)
        ctx.scale(-1, 1)
        ctx.drawImage(img, sx, sy, sw, sh, 0, 0, drawW, drawH)
      } else {
        ctx.drawImage(img, sx, sy, sw, sh, dx, dy, drawW, drawH)
      }
      ctx.restore()
    },
    update(time) {
      const dt = Math.min((time - this.lastTime) / 1000, 0.05)
      this.lastTime = time

      const canvas = this.$refs.canvas
      const ctx = this.ctx
      if (!canvas || !ctx) {
        this.animationFrame = requestAnimationFrame(this.update)
        return
      }

      ctx.clearRect(0, 0, this.playWidth, this.playHeight)

      if (this.state === 'AUTO_RUN') {
        this.updateAutoRun(dt, canvas)
      } else if (this.state === 'AUTO_TURN_WAIT') {
        this.updateAutoTurnWait(dt, canvas)
      } else if (this.state === 'KEYBOARD_CONTROL') {
        this.updateKeyboardControl(dt, canvas)
      } else if (this.state === 'USER_CONTROL') {
        this.updateUserControl(dt, canvas)
      } else if (this.state === 'SIT_LOOK_AROUND') {
        this.updateSit(dt)
      }

      this.updateParticles(dt)

      for (const p of this.particles) {
        ctx.beginPath()
        ctx.arc(p.x, p.y, 2.5 * (1.2 - p.life * 0.4), 0, Math.PI * 2)
        ctx.fillStyle = p.color + (p.life * 0.45) + ')'
        ctx.fill()
      }

      this.updateMouseLayer()
      this.drawMouse(ctx, canvas)
      this.animationFrame = requestAnimationFrame(this.update)
    }
  }
}
</script>

<style scoped>
.mouse-runner-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
  outline: none;
}

.mouse-canvas {
  width: 100%;
  height: 100%;
  display: block;
  background: transparent;
}
</style>
