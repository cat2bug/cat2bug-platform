<template>
  <div ref="container" class="mouse-runner-container" tabindex="0">
    <canvas ref="canvas" class="mouse-canvas" />
  </div>
</template>

<script>
/** 横向奔跑精灵：1 行×8 列，朝右；朝左由 flipX 镜像 */
const RUN_SPRITE_COLS = 8
const RUN_SPRITE_FRAME_W = 214
const RUN_SPRITE_FRAME_H = 96
const RUN_SPRITE_DISPLAY_SCALE = 0.42
/** 整体显示缩放（相对默认尺寸再小 1/4） */
const MOUSE_DISPLAY_SCALE = 0.75
/** 脚底阴影（不随 MOUSE_DISPLAY_SCALE 同比缩小，避免暗色背景下看不见） */
const SHADOW_RX_RATIO = 0.38
const SHADOW_RY_RATIO = 0.14
const SHADOW_MIN_RX = 18
const SHADOW_MIN_RY = 6
const SHADOW_Y_OFFSET = 4
const SHADOW_ALPHA_DARK = 0.52
const SHADOW_ALPHA_LIGHT = 0.16
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
const FORM_BARRIER_OFFSET = 0
const FORM_BARRIER_Y_EPS = 8
const Z_MOUSE_ABOVE_FORM = 4
const Z_MOUSE_BEHIND_FORM = 1

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
      reducedMotionMq: null
    }
  },
  mounted() {
    this.initAnimState()
    this.initCanvas()
    this.loadSprite()
    this.setupReducedMotion()
    this.resetAutoRun(true)
    this.$nextTick(() => {
      const canvas = this.$refs.canvas
      if (canvas) this.syncPlayBounds(canvas)
    })
    document.addEventListener('contextmenu', this.onContextMenu)
    document.addEventListener('mousedown', this.onDocumentMouseDown)
    document.addEventListener('mousemove', this.onDocumentMouseMove)
    document.addEventListener('mouseup', this.onDocumentMouseUp)
    document.addEventListener('keydown', this.onKeyDown)
    document.addEventListener('keyup', this.onKeyUp)
    window.addEventListener('resize', this.handleResize)
    this.lastTime = performance.now()
    this.animationFrame = requestAnimationFrame(this.update)
  },
  beforeDestroy() {
    document.removeEventListener('contextmenu', this.onContextMenu)
    document.removeEventListener('mousedown', this.onDocumentMouseDown)
    document.removeEventListener('mousemove', this.onDocumentMouseMove)
    document.removeEventListener('mouseup', this.onDocumentMouseUp)
    document.removeEventListener('keydown', this.onKeyDown)
    document.removeEventListener('keyup', this.onKeyUp)
    window.removeEventListener('resize', this.handleResize)
    if (this.reducedMotionMq) {
      this.reducedMotionMq.removeEventListener('change', this.onReducedMotionChange)
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
    onKeyDown(e) {
      if (!MOVE_KEY_CODES.has(e.key) || this.isFormField(e.target)) return
      e.preventDefault()
      this.keysDown.add(e.key)
      if (this.rmbDown) return
      this.farSpawn = false
      this.state = 'KEYBOARD_CONTROL'
    },
    onKeyUp(e) {
      if (!MOVE_KEY_CODES.has(e.key)) return
      this.keysDown.delete(e.key)
      if (this.rmbDown) return
      if (!this.hasMovementKeys() && this.state === 'KEYBOARD_CONTROL') {
        this.enterSitLook()
      }
    },
    initCanvas() {
      const canvas = this.$refs.canvas
      const container = this.$refs.container
      if (!canvas || !container) return
      canvas.width = container.clientWidth
      canvas.height = container.clientHeight
      this.ctx = canvas.getContext('2d')
      this.syncPlayBounds(canvas)
    },
    handleResize() {
      const canvas = this.$refs.canvas
      const container = this.$refs.container
      if (!canvas || !container) return
      canvas.width = container.clientWidth
      canvas.height = container.clientHeight
      this.syncPlayBounds(canvas)
    },
    syncPlayBounds(canvas) {
      const canvasRect = canvas.getBoundingClientRect()
      const form = document.querySelector('.logo-page .login-form')
      let minY = Math.max(24, canvas.height * 0.2)
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
      this.maxMouseY = canvas.height - MOUSE_MAX_Y_INSET
      this.groundY = Math.min(canvas.height * 0.88, this.maxMouseY)
      this.minMouseY = minY
      if (this.mouseY < this.minMouseY) this.mouseY = this.minMouseY
      if (this.mouseY > this.maxMouseY) this.mouseY = this.maxMouseY
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
    clampManualPosition(canvas) {
      this.mouseX = Math.max(EDGE_PAD, Math.min(canvas.width - EDGE_PAD, this.mouseX))
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
      this.mouseX = this.direction > 0 ? -margin : canvas.width + margin
      this.farSpawn = !initial && exitY == null && Math.random() < FAR_SPAWN_CHANCE
      if (this.farSpawn) {
        this.farSpawnT = 0
        const skyY = canvas.height * (0.35 + Math.random() * 0.2)
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
      this.clampManualPosition(canvas)
      this.spriteRow = this.direction > 0 ? SPRITE_ROW_RIGHT : SPRITE_ROW_LEFT
      this.flipX = this.direction < 0
      this.frameIndex = 0
      this.frameTimer = 0
    },
    depthScale(canvas) {
      const t = Math.max(0, Math.min(1, this.mouseY / canvas.height))
      return 0.55 + t * 0.4
    },
    pixelScale(canvas) {
      const d = this.depthScale(canvas)
      return d * 1.15
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
      if (this.direction > 0 && this.mouseX > canvas.width + margin) {
        this.mouseX = canvas.width + margin
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
      const fps = 3 // 降低晃头动画的帧率，使其更悠闲
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
      let tx = this.pointerX
      let ty = Math.max(this.minMouseY, Math.min(this.maxMouseY, this.pointerY))
      if (this.isOnFormBarrierLine()) {
        ty = Math.max(ty, this.formBarrierY)
      }
      const lerp = 1 - Math.exp(-USER_LERP * dt)
      const prevX = this.mouseX
      const prevY = this.mouseY
      this.mouseX += (tx - this.mouseX) * lerp
      this.mouseY += (ty - this.mouseY) * lerp
      this.clampManualPosition(canvas)
      
      const actualDx = this.mouseX - prevX
      const actualDy = this.mouseY - prevY
      const dist = Math.hypot(actualDx, actualDy)
      const moving = dist > 0.4
      
      if (moving) {
        // 使用实际移动的轨迹来决定朝向。这样当撞到上下边界只能横向滑动时，动画会变成直走
        this.pickUserSpriteRow(actualDx, actualDy)
        
        const speedMul = 0.7 + this.depthScale(canvas) * 0.5
        this.speed = AUTO_SPEED * speedMul
        this.createDust(actualDx / dist, actualDy / dist, canvas)
      }
      this.advanceRunFrames(dt, moving)
    },
    updateKeyboardControl(dt, canvas) {
      const { dx, dy } = this.getKeyMovement()
      if (dx === 0 && dy === 0) return

      const prevX = this.mouseX
      const prevY = this.mouseY
      const speedMul = 0.7 + this.depthScale(canvas) * 0.5
      const speed = AUTO_SPEED * speedMul
      this.mouseX += dx * speed * dt
      this.mouseY += dy * speed * dt
      this.clampManualPosition(canvas)
      
      const actualDx = this.mouseX - prevX
      const actualDy = this.mouseY - prevY
      const dist = Math.hypot(actualDx, actualDy)
      const moving = dist > 0.01

      if (moving) {
        // 使用实际移动的轨迹来决定朝向。这样当撞到上下边界只能横向滑动时，动画会变成直走
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
    drawMouse(ctx, canvas) {
      if (!this.isSpriteLoaded) return

      const row = this.spriteRow
      const horizontal = this.isHorizontalRunRow(row)
      const topLeft = this.isTopLeftRunRow(row)
      const bottomLeft = this.isBottomLeftRunRow(row)
      const top = this.isTopRunRow(row)
      const bottom = this.isBottomRunRow(row)
      const sit = this.isSitRow(row)
      const pScale = this.pixelScale(canvas)
      const sizeScale = (horizontal ? RUN_SPRITE_DISPLAY_SCALE : (topLeft ? RUN_TL_SPRITE_DISPLAY_SCALE : (bottomLeft ? RUN_BL_SPRITE_DISPLAY_SCALE : (top ? RUN_T_SPRITE_DISPLAY_SCALE : (bottom ? RUN_B_SPRITE_DISPLAY_SCALE : SIT_SPRITE_DISPLAY_SCALE))))) * MOUSE_DISPLAY_SCALE
      const baseW = horizontal ? RUN_SPRITE_FRAME_W : (topLeft ? RUN_TL_SPRITE_FRAME_W : (bottomLeft ? RUN_BL_SPRITE_FRAME_W : (top ? RUN_T_SPRITE_FRAME_W : (bottom ? RUN_B_SPRITE_FRAME_W : SIT_SPRITE_FRAME_W))))
      const baseH = horizontal ? RUN_SPRITE_FRAME_H : (topLeft ? RUN_TL_SPRITE_FRAME_H : (bottomLeft ? RUN_BL_SPRITE_FRAME_H : (top ? RUN_T_SPRITE_FRAME_H : (bottom ? RUN_B_SPRITE_FRAME_H : SIT_SPRITE_FRAME_H))))
      const drawW = baseW * pScale * sizeScale
      const drawH = baseH * pScale * sizeScale
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
      const airLift = Math.max(0, (this.groundY || this.mouseY) - this.mouseY)
      const shadowScale = 1 - Math.min(0.35, airLift / (drawH * 2))

      const isDark = document.documentElement.classList.contains('dark')
      const shadowRx = Math.max(SHADOW_MIN_RX, drawW * SHADOW_RX_RATIO * shadowScale)
      const shadowRy = Math.max(SHADOW_MIN_RY, drawH * SHADOW_RY_RATIO * shadowScale)
      const shadowAlpha = (isDark ? SHADOW_ALPHA_DARK : SHADOW_ALPHA_LIGHT) * shadowScale
      ctx.beginPath()
      ctx.ellipse(
        Math.round(this.mouseX),
        Math.round(this.mouseY + SHADOW_Y_OFFSET),
        Math.round(shadowRx),
        Math.round(shadowRy),
        0, 0, Math.PI * 2
      )
      ctx.fillStyle = `rgba(0,0,0,${shadowAlpha})`
      ctx.fill()

      ctx.save()
      ctx.imageSmoothingEnabled = true
      if (flip) {
        ctx.translate(Math.round(dx + drawW), Math.round(dy))
        ctx.scale(-1, 1)
        ctx.drawImage(img, sx, sy, sw, sh, 0, 0, drawW, drawH)
      } else {
        ctx.drawImage(
          img, sx, sy, sw, sh,
          Math.round(dx), Math.round(dy), drawW, drawH
        )
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

      ctx.clearRect(0, 0, canvas.width, canvas.height)

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
