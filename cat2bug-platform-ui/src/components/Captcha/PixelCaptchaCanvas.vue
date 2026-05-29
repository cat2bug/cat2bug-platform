<template>
  <canvas
    ref="canvas"
    class="pixel-captcha-canvas"
    :width="width"
    :height="height"
    :title="$t('verification-code')"
    @click="$emit('refresh')"
  />
</template>

<script>
const C = {
  skyTop: '#E86A5A',
  skyMid: '#FF8F5A',
  skyLow: '#FFC145',
  sun: '#FFD54F',
  sunEdge: '#FFB300',
  sunCore: '#FFF8E1',
  horizonGlow: '#FFAB40',
  ground: '#5A5A59',
  groundHi: '#6D6E71',
  grass: '#7CB342',
  grassDark: '#558B2F',
  flowerA: '#FF8CB0',
  flowerB: '#FF6F9B',
  flowerCore: '#FFD54F',
  flowerC: '#80DEEA',
  flowerCDark: '#00ACC1',
  stem: '#4C8259',
  leaf: '#66BB6A',
  snailBody: '#FFC8A2',
  snailShadow: '#E09A76',
  snailHi: '#FFE5D9',
  shell: '#B04A33',
  shellHi: '#E57F67',
  shellSpiral: '#6A2417',
  eyeWhite: '#FFFDF0',
  textFill: 'rgba(90,90,89,0.42)',
  textStroke: 'rgba(255,243,214,0.72)',
  ink: '#2E1E1C'
}

const SNAIL_UNIT = 1.5

const SNAIL_A = [
  '..............kkkk...kkkk...',
  '.............kWWek..kWWek...',
  '.............kWWek..kWWek...',
  '..............kkkk...kkkk...',
  '......kkkkkk..kBBk...kBBk...',
  '....kkhhhhhhkkkBBk...kBBk...',
  '...khhSSSSSShhkkBBkkkBBk....',
  '..khhSsssssSShhkkBBkBBk.....',
  '..khSSsSSSssSShkkkBBk.......',
  '.kHSSssSShhSsSSkkBBBBk......',
  '.kHSSsSShhhSsSSkBBBBBBk.....',
  '.kHSSsSSsssSsSSkBBBBBBk.....',
  '.khSSssSSssSShkkBBBBBBk.....',
  '..khhSSssssSShkBBBBBBk......',
  '...kkSShhhSSkkkBBBBBBk......',
  '.....kkkkkkkkkkbBbbBkk......',
  '...kbbBBBBBBBBBBBBBBBBBBk...',
  '....kkkkkkkkkkkkkkkkkkkk....'
]

const SNAIL_B = [
  '...............kkkk...kkkk..',
  '..............kWWek..kWWek..',
  '..............kWWek..kWWek..',
  '...............kkkk...kkkk..',
  '......kkkkkk...kBBk...kBBk..',
  '....kkhhhhhhkk..kBBk...kBBk.',
  '...khhSSSSSShhkkBBkkkBBk....',
  '..khhSsssssSShhkkBBkBBk.....',
  '..khSSsSSSssSShkkkBBk.......',
  '.kHSSssSShhSsSSkkBBBBk......',
  '.kHSSsSShhhSsSSkBBBBBBk.....',
  '.kHSSsSSsssSsSSkBBBBBBk.....',
  '.khSSssSSssSShkkBBBBBBk.....',
  '..khhSSssssSShkBBBBBBk......',
  '...kkSShhhSSkkkBBBBBBk......',
  '.....kkkkkkkkkkbBbbBkk......',
  '...kbbBBBBBBBBBBBBBBBBBBk...',
  '....kkkkkkkkkkkkkkkkkkkk....'
]

const SNAIL_MAP = {
  'k': C.ink,
  'W': C.eyeWhite,
  'e': C.ink,
  'B': C.snailBody,
  'b': C.snailShadow,
  'H': C.snailHi,
  'S': C.shell,
  's': C.shellSpiral,
  'h': C.shellHi
}

const FLORA_MAP = {
  '.': null,
  's': C.stem,
  'L': C.leaf,
  'g': C.grassDark,
  'G': C.grass,
  'w': '#A2D149',
  'a': C.flowerA,
  'A': C.flowerB,
  'y': C.flowerCore,
  'r': '#E53935',
  'R': '#B71C1C',
  'c': C.flowerC,
  'C': C.flowerCDark
}

const FLOWER_SPRITES = [
  // 雏菊 (Pink Daisy)
  [
    '..aaa...',
    '.aayya..',
    'aayyyya.',
    'aayyyya.',
    '.aayya..',
    '..aaa...',
    '...s....',
    '..Lss...',
    '...s....',
    '...s....'
  ],
  // 郁金香 (Red Tulip)
  [
    '.r..r...',
    'rRr.rRr.',
    'RRRRRRR.',
    'rRRRRRr.',
    '.rRRRr..',
    '..ss....',
    '..ssL...',
    '..ss....',
    '..ss....'
  ],
  // 蓝铃花 (Bluebell)
  [
    '..cc....',
    '.cCCc...',
    'cCCCCc..',
    'cCCCCc..',
    '.cCCc...',
    '..s.....',
    '..s.....',
    '.Ls.....',
    '..s.....'
  ]
]

const GRASS_SPRITES = [
  // 高挺野草
  [
    '....g...',
    '...gG...',
    '..gG....',
    '..gG....',
    '.gG.....',
    '.gg.....',
    'ggG.....',
    'ggg.....'
  ],
  // 丛生草簇
  [
    '.g...g..',
    '.g..gg..',
    'gg.gGg..',
    'gGggGg.g',
    'gGgGgggg',
    'gggggggg'
  ],
  // 小三叶草/小苗
  [
    '...g....',
    '..gG....',
    '.gGGg...',
    'gGGGg...',
    'ggggg...'
  ]
]

/** 沿条带预置不规则花草（确定性随机） */
function buildFloraStrip(length) {
  const items = []
  let x = 0
  let seed = 7919
  const rnd = () => {
    seed = (seed * 1103515245 + 12345) & 0x7fffffff
    return seed / 0x7fffffff
  }
  while (x < length) {
    const isFlower = rnd() < 0.52
    items.push({
      x,
      type: isFlower ? 'flower' : 'grass',
      yOff: Math.floor(rnd() * 5) - 2,
      variant: Math.floor(rnd() * 3),
      scale: isFlower ? 1 + (rnd() > 0.6 ? 1 : 0) : 1
    })
    x += (isFlower ? 14 : 7) + Math.floor(rnd() * 16)
  }
  return items
}

const FLORA_STRIP = buildFloraStrip(520)

export default {
  name: 'PixelCaptchaCanvas',
  props: {
    expression: { type: String, default: '' },
    width: { type: Number, default: 150 },
    height: { type: Number, default: 38 }
  },
  data() {
    return {
      frame: 0,
      bgScroll: 0,
      animId: null
    }
  },
  watch: {
    expression() {
      this.resetMotion()
    }
  },
  mounted() {
    this.resetMotion()
    this.startLoop()
  },
  beforeDestroy() {
    this.stopLoop()
  },
  methods: {
    groundY() {
      return this.height - 4
    },
    snailAnchorX() {
      return 6
    },
    resetMotion() {
      this.frame = 0
      this.bgScroll = 0
    },
    startLoop() {
      this.stopLoop()
      const tick = () => {
        this.frame += 1
        this.bgScroll += 0.22
        this.draw()
        this.animId = window.requestAnimationFrame(tick)
      }
      this.animId = window.requestAnimationFrame(tick)
    },
    stopLoop() {
      if (this.animId != null) {
        window.cancelAnimationFrame(this.animId)
        this.animId = null
      }
    },
    draw() {
      const canvas = this.$refs.canvas
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      if (!ctx) return
      ctx.imageSmoothingEnabled = false
      const w = this.width
      const h = this.height
      const groundY = this.groundY()

      this.drawSunsetSky(ctx, w, groundY)
      this.drawSettingSun(ctx, w, groundY)
      this.drawGround(ctx, w, h, groundY)
      this.drawExpression(ctx, w, groundY) // 先画验证码文字
      this.drawScrollingFlora(ctx, w, groundY) // 后画滚动花草，遮挡和重叠在文字前面，增强机器识别干扰和画面层次感
      const bob = this.frame % 30 < 15 ? 0 : 1
      this.drawSnail(ctx, this.snailAnchorX(), groundY + bob, this.frame)
    },
    drawSunsetSky(ctx, w, groundY) {
      const bands = [C.skyTop, C.skyMid, C.skyLow]
      const bandH = 3
      for (let y = 0; y < groundY; y += bandH) {
        ctx.fillStyle = bands[Math.floor(y / bandH) % bands.length]
        ctx.fillRect(0, y, w, bandH)
      }
    },
    drawSettingSun(ctx, w, groundY) {
      const cx = w - 22
      const cy = groundY
      const r = 15
      const block = 2

      for (let dy = -r; dy < 0; dy += block) {
        const rowY = cy + dy
        const halfChord = Math.sqrt(Math.max(0, r * r - dy * dy))
        const rowW = Math.floor(halfChord * 2)
        const startX = cx - Math.floor(halfChord)
        const t = (dy + r) / r
        ctx.fillStyle = t < 0.28 ? C.sunCore : t < 0.68 ? C.sun : C.sunEdge
        ctx.fillRect(startX, rowY, rowW, block)
      }

      ctx.fillStyle = C.horizonGlow
      ctx.fillRect(cx - r - 6, cy - 1, r * 2 + 12, 2)
      ctx.fillStyle = C.ink
      for (let dx = -r; dx <= r; dx += block) {
        const topDy = -Math.sqrt(Math.max(0, r * r - dx * dx))
        ctx.fillRect(cx + dx, cy + topDy - block, block, 1)
      }
    },
    drawGround(ctx, w, h, groundY) {
      ctx.fillStyle = C.ground
      ctx.fillRect(0, groundY, w, h - groundY)
      ctx.fillStyle = C.groundHi
      for (let x = 0; x < w; x += 5) {
        ctx.fillRect(x, groundY, 2, 1)
      }
    },
    drawScrollingFlora(ctx, w, groundY) {
      const scroll = this.bgScroll % 520
      for (const item of FLORA_STRIP) {
        let drawX = item.x - scroll
        while (drawX < -24) drawX += 520
        while (drawX > w + 24) drawX -= 520
        if (drawX < -24 || drawX > w + 24) continue
        
        // 统一对齐到地面(groundY)，没有浮空或过度下沉
        const gy = groundY
        const variant = item.variant % 3
        const px = 1.2 // 花草的精细像素单位

        if (item.type === 'flower') {
          const sprite = FLOWER_SPRITES[variant]
          const spriteH = sprite.length
          const baseY = gy - spriteH * px
          for (let r = 0; r < spriteH; r++) {
            const line = sprite[r]
            for (let col = 0; col < line.length; col++) {
              const ch = line[col]
              if (ch === '.') continue
              ctx.fillStyle = FLORA_MAP[ch] || C.stem
              ctx.fillRect(Math.round(drawX) + col * px, baseY + r * px, px, px)
            }
          }
        } else {
          const sprite = GRASS_SPRITES[variant]
          const spriteH = sprite.length
          const baseY = gy - spriteH * px
          for (let r = 0; r < spriteH; r++) {
            const line = sprite[r]
            for (let col = 0; col < line.length; col++) {
              const ch = line[col]
              if (ch === '.') continue
              ctx.fillStyle = FLORA_MAP[ch] || C.grass
              ctx.fillRect(Math.round(drawX) + col * px, baseY + r * px, px, px)
            }
          }
        }
      }
    },
    drawExpression(ctx, w, groundY) {
      if (!this.expression) return
      let fontSize = 23
      ctx.font = `bold ${fontSize}px monospace`
      while (ctx.measureText(this.expression).width > w - 4 && fontSize > 12) {
        fontSize -= 1
        ctx.font = `bold ${fontSize}px monospace`
      }
      
      const chars = this.expression.split('')
      const totalW = ctx.measureText(this.expression).width
      // 蜗牛在最左，夕阳在最右，黄金居中偏移
      const startX = Math.round(w * 0.58) - totalW / 2
      const centerY = groundY / 2 + 1

      ctx.textBaseline = 'middle'
      ctx.textAlign = 'center'
      ctx.lineWidth = 3
      ctx.strokeStyle = C.textStroke
      ctx.fillStyle = C.textFill

      let currentX = startX
      for (let i = 0; i < chars.length; i++) {
        const char = chars[i]
        const charW = ctx.measureText(char).width

        // 对每一个字符赋予活泼的微调偏移与微小倾角，打破机械对齐：
        // 1. 垂直波浪跳动 (根据字符索引产生高低参差效果，并随帧产生极轻柔的浮动)
        const waveY = Math.sin(i * 1.5 + this.frame * 0.05) * 1.8
        // 2. 倾角参差（正负旋转交替，让每个字符都有独特的形态）
        const angle = Math.sin(i * 2.3) * 0.14

        ctx.save()
        // 移动到每个字符的中心点进行渲染
        ctx.translate(currentX + charW / 2, centerY + waveY)
        ctx.rotate(angle)

        // 绘制描边与文字
        ctx.strokeText(char, 0, 0)
        ctx.fillText(char, 0, 0)

        ctx.restore()

        currentX += charW
      }
    },
    /** 参考图结构：圆壳在背、竖颈+双眼柄在右、腹足平贴（肉色身体，朝右看） */
    drawSnail(ctx, sx, gy, frame) {
      const px = SNAIL_UNIT
      const rows = frame % 24 < 12 ? SNAIL_A : SNAIL_B
      const rowsH = rows.length
      const baseY = gy - rowsH * px

      for (let r = 0; r < rows.length; r++) {
        // 直接使用预设计好的朝右像素行，无需水平翻转
        const line = rows[r]
        for (let col = 0; col < line.length; col++) {
          const ch = line[col]
          if (ch === '.') continue
          ctx.fillStyle = SNAIL_MAP[ch] || C.snailBody
          ctx.fillRect(sx + col * px, baseY + r * px, px, px)
        }
      }
    }
  }
}
</script>

<style scoped>
.pixel-captcha-canvas {
  width: 100%;
  height: 100%;
  display: block;
  cursor: pointer;
  image-rendering: pixelated;
  image-rendering: crisp-edges;
}
</style>
