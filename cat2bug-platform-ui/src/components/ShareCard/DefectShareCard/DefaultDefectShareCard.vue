<template>
  <div>
  <div ref="share" class="share">
    <div class="header">
      <row-list-member :members="params.handleByList" />
      <h4 class="defect-title-num">#{{params.projectNum}}</h4>
      <h4 class="defect-title-name">{{params.defectName}}</h4>
    </div>
    <div class="body">
      <div>
        <i class="gitee"></i>
        <label>{{$t('shard.aging-hour')}}:</label>
        <span>{{defectAgingHour()}}</span>
      </div>
      <div v-if="password">
        <label>{{$t('password')}}:</label><span>{{password}}</span>
      </div>
    </div>
    <div class="body">
      <div v-show="displayImageUrls.length" class="img-list">
        <div
          v-for="(img,index) in displayImageUrls"
          :key="index"
          class="share-img-wrap"
          :class="{ 'share-img-wrap--hero': index === 0 }"
        >
          <canvas
            ref="canvas"
            :class="`defectSvg${index}`"
            :index="index"
          ></canvas>
        </div>
      </div>

      <div class="flag">
        <defect-type-flag :defect="params" class="font-mini" />
        <defect-state-flag :defect="params" class="font-mini" />
      </div>
      <div class="describe">
        {{params.defectDescribe}}
      </div>
    </div>
    <div class="footer">
      <div class="qr-wrap">
        <canvas ref="qrcode" class="qr" id="defect-qr-canvas"></canvas>
      </div>
      <span>{{$t('defect.shard.click-qrcode-info')}}</span>
    </div>
    <div class="link">
      <el-link>{{getDefectUrl(shard.defectShardId)}}</el-link>
    </div>
    <img ref="myImg" class="myImg" :src="shardBase64Img || ''" />
  </div>
  </div>
</template>

<script>
import DefectTypeFlag from "@/components/Defect/DefectTypeFlag";
import DefectStateFlag from "@/components/Defect/DefectStateFlag";
import RowListMember from "@/components/RowListMember";
import QRCode from 'qrcode'
import html2canvas from 'html2canvas';
import Label from "@/components/Cat2BugStatistic/Components/Label";
import {
  loadEnabledImageFields,
  resolveShareCardImageUrls
} from '@/utils/share-card-images'

/** 跨弹层开关保留预截图，避免「每次打开再点」都重跑 html2canvas */
const SNAPSHOT_CACHE_MAX = 8
const SNAPSHOT_CACHE_VER = 'v3-square' // 改截图策略时 bump，丢掉旧白边缓存
const snapshotCache = new Map()

function snapshotCacheKey(shardId, agingHour, password, urls) {
  const list = Array.isArray(urls) ? urls.join(',') : ''
  return `${SNAPSHOT_CACHE_VER}|${shardId || ''}|${agingHour ?? ''}|${password || ''}|${list}`
}

/** 把任意 css 颜色落到不透明 rgb，供 canvas 铺底 */
function opaqueCssColor(css, fallback = '#1e1e1e') {
  const raw = (css || '').trim()
  if (!raw || raw === 'transparent') return fallback
  const probe = document.createElement('div')
  probe.style.cssText = `position:fixed;left:-9999px;top:0;background:${raw}`
  document.body.appendChild(probe)
  const computed = getComputedStyle(probe).backgroundColor
  document.body.removeChild(probe)
  const m = /^rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)$/.exec(computed || '')
  if (!m) return fallback
  const a = m[4] == null ? 1 : Number(m[4])
  if (!(a > 0.01)) return fallback
  return `rgb(${m[1]}, ${m[2]}, ${m[3]})`
}

/** 先铺底色再叠截图层，杜绝 PNG 透明角在 IM 里变白 */
function flattenOpaqueCanvas(source, bgCss) {
  const out = document.createElement('canvas')
  out.width = source.width
  out.height = source.height
  const ctx = out.getContext('2d')
  ctx.fillStyle = bgCss
  ctx.fillRect(0, 0, out.width, out.height)
  ctx.drawImage(source, 0, 0)
  return out
}

function getCachedSnapshot(key) {
  if (!key || !snapshotCache.has(key)) return null
  const blob = snapshotCache.get(key)
  // LRU：读后挪到末尾
  snapshotCache.delete(key)
  snapshotCache.set(key, blob)
  return blob
}

function setCachedSnapshot(key, blob) {
  if (!key || !blob) return
  if (snapshotCache.has(key)) snapshotCache.delete(key)
  snapshotCache.set(key, blob)
  while (snapshotCache.size > SNAPSHOT_CACHE_MAX) {
    const oldest = snapshotCache.keys().next().value
    snapshotCache.delete(oldest)
  }
}

export default {
  name: "DefaultDefectShareCard",
  components:{Label, DefectTypeFlag, DefectStateFlag, RowListMember},
  props: {
    params: {
      type: Object,
      default: ()=>{}
    },
    agingHour: {
      type: [String, Number],
      default: '1'
    },
    password: {
      type: String,
      default: null
    },
    /** 父级已探测好的图片 URL；传入则跳过二次探测 */
    resolvedImageUrls: {
      type: Array,
      default: null
    }
  },
  data() {
    return {
      shardBase64Img: null,
      displayImageUrls: [],
      /** 预截图缓存：打开/刷新后后台生成，点击复制时直接写剪贴板 */
      cachedPngBlob: null,
      snapshotPromise: null,
      snapshotToken: 0,
      snapshotReady: false,
      shard: {
        defectShardId: 'XXXXXXXXXX',
        agingTime: new Date(),
      }
    }
  },
  computed: {
    getDefectUrl: function () {
      return function (id) {
        const base = (process.env.BASE_URL || '/').replace(/\/$/, '')
        return `${window.location.origin}${base}/shard/defect?id=${id}`;
      }
    },
    defectAgingHour: function () {
      return function () {
        switch (parseInt(this.agingHour)) {
          case 1:
            return this.$i18n.t('shard.1hour');
          case 24:
            return this.$i18n.t('shard.1day');
          case 168:
            return this.$i18n.t('shard.7day');
          case 0:
            return this.$i18n.t('shard.permanent');
        }
      }
    },
    isSnapshotReady() {
      return this.snapshotReady && !!this.cachedPngBlob
    }
  },
  methods: {
    paintCanvasFromUrl(canvas, src) {
      if (!canvas || !src) return Promise.resolve()
      const ctx = canvas.getContext('2d')
      canvas.width = canvas.clientWidth || canvas.width
      canvas.height = canvas.clientHeight || canvas.height
      return new Promise((resolve) => {
        const img = new Image()
        img.onload = () => {
          let sourceX = 0
          let sourceY = 0
          let sourceWidth = img.width
          let sourceHeight = img.height
          const destX = 0
          const destY = 0
          const destWidth = canvas.width
          const destHeight = canvas.height
          if ((img.width / img.height) < (canvas.width / canvas.height)) {
            sourceX = 0
            sourceWidth = img.width
            sourceY = (img.height - img.width / (canvas.width / canvas.height)) / 2
            sourceHeight = img.width / (canvas.width / canvas.height)
          } else {
            sourceX = (img.width - img.height * (canvas.width / canvas.height)) / 2
            sourceWidth = img.height * (canvas.width / canvas.height)
            sourceY = 0
            sourceHeight = img.height
          }
          ctx.drawImage(
            img,
            sourceX, sourceY, sourceWidth, sourceHeight,
            destX, destY, destWidth, destHeight
          )
          resolve()
        }
        img.onerror = () => resolve()
        img.src = src
      })
    },
    async drawImage() {
      const imgs = this.displayImageUrls || []
      await this.$nextTick()
      const refs = this.$refs.canvas
      if (!refs || !imgs.length) return
      const canvases = Array.isArray(refs) ? refs : [refs]
      await Promise.all(
        imgs.map((src, i) => this.paintCanvasFromUrl(canvases[i], src))
      )
    },
    createQRCode (url) {
      return new Promise((resolve) => {
        if (!this.$refs.qrcode) {
          resolve()
          return
        }
        QRCode.toCanvas(this.$refs.qrcode, url, (error) => {
          if (error) {
            console.error(error)
          }
          resolve()
        })
      })
    },
    invalidateSnapshot() {
      this.snapshotToken += 1
      this.cachedPngBlob = null
      this.snapshotPromise = null
      this.shardBase64Img = null
      this.snapshotReady = false
      this.$emit('snapshot-ready', false)
    },
    /** 立刻预热（不等 idle），让点击尽量只写剪贴板 */
    kickPrewarm() {
      const token = this.snapshotToken
      const run = () => {
        if (token !== this.snapshotToken) return
        this.ensureSnapshot().catch(() => {})
      }
      // 空闲再截：避免刚打开弹层悬停「点击复制」时主线程被 html2canvas 占住
      this.$nextTick(() => {
        if (typeof requestIdleCallback === 'function') {
          requestIdleCallback(run, { timeout: 900 })
        } else {
          setTimeout(run, 120)
        }
      })
    },
    async renderPngBlob() {
      const el = this.$refs.share
      if (!el) {
        throw new Error('share card not ready')
      }
      await this.$nextTick()
      // 外层圆角在 html2canvas 里会抗锯齿出白边；截图时强制直角，并去掉父级圆角裁剪
      const parent = el.parentElement
      const prev = {
        radius: el.style.borderRadius,
        overflow: el.style.overflow,
        parentRadius: parent ? parent.style.borderRadius : '',
        parentOverflow: parent ? parent.style.overflow : ''
      }
      el.classList.add('share--capturing')
      el.style.setProperty('border-radius', '0', 'important')
      el.style.overflow = 'hidden'
      if (parent) {
        parent.style.setProperty('border-radius', '0', 'important')
        parent.style.overflow = 'visible'
      }
      const bg = opaqueCssColor(getComputedStyle(el).backgroundColor, '#1e1e1e')
      const w = el.offsetWidth
      const h = el.offsetHeight
      // 等样式真正生效后再截（否则仍可能截到圆角）
      await new Promise((r) => requestAnimationFrame(() => requestAnimationFrame(r)))
      try {
        const canvas = await html2canvas(el, {
          useCORS: true,
          allowTaint: true,
          backgroundColor: bg,
          scale: 1,
          logging: false,
          imageTimeout: 0,
          width: w,
          height: h,
          windowWidth: w,
          windowHeight: h,
          scrollX: 0,
          scrollY: 0,
          ignoreElements: (node) =>
            !!(node && node.classList && node.classList.contains('myImg')),
          // html2canvas 在克隆文档里渲染：必须在这里再清一次圆角才可靠
          onclone: (_doc, cloned) => {
            if (!cloned || !cloned.style) return
            cloned.style.setProperty('border-radius', '0px', 'important')
            cloned.style.setProperty('overflow', 'hidden', 'important')
            cloned.style.setProperty('background-color', bg, 'important')
            cloned.classList.add('share--capturing')
          }
        })
        const flat = flattenOpaqueCanvas(canvas, bg)
        const blob = await new Promise((resolve, reject) => {
          flat.toBlob((b) => {
            if (b) resolve(b)
            else reject(new Error('toBlob failed'))
          }, 'image/png')
        })
        return blob
      } finally {
        el.classList.remove('share--capturing')
        if (prev.radius) el.style.borderRadius = prev.radius
        else el.style.removeProperty('border-radius')
        el.style.overflow = prev.overflow
        if (parent) {
          if (prev.parentRadius) parent.style.borderRadius = prev.parentRadius
          else parent.style.removeProperty('border-radius')
          parent.style.overflow = prev.parentOverflow
        }
      }
    },
    currentSnapshotKey() {
      const id = this.shard && this.shard.defectShardId
      return snapshotCacheKey(
        id,
        this.agingHour,
        this.password,
        this.displayImageUrls
      )
    },
    markSnapshotReady(blob) {
      this.cachedPngBlob = blob
      this.snapshotReady = true
      this.$emit('snapshot-ready', true)
      setCachedSnapshot(this.currentSnapshotKey(), blob)
    },
    ensureSnapshot() {
      if (this.cachedPngBlob) {
        return Promise.resolve(this.cachedPngBlob)
      }
      const fromLru = getCachedSnapshot(this.currentSnapshotKey())
      if (fromLru) {
        this.markSnapshotReady(fromLru)
        return Promise.resolve(fromLru)
      }
      if (this.snapshotPromise) {
        return this.snapshotPromise
      }
      const token = this.snapshotToken
      this.snapshotPromise = this.renderPngBlob()
        .then((blob) => {
          if (token !== this.snapshotToken) {
            throw new Error('snapshot stale')
          }
          this.markSnapshotReady(blob)
          this.snapshotPromise = null
          return blob
        })
        .catch((err) => {
          if (token === this.snapshotToken) {
            this.snapshotPromise = null
            this.snapshotReady = false
            this.$emit('snapshot-ready', false)
          }
          throw err
        })
      return this.snapshotPromise
    },
    async refresh(shard, preResolvedUrls) {
      this.invalidateSnapshot()
      this.shard = shard;
      if (Array.isArray(preResolvedUrls)) {
        this.displayImageUrls = preResolvedUrls
      } else if (Array.isArray(this.resolvedImageUrls)) {
        this.displayImageUrls = this.resolvedImageUrls
      } else {
        const fields = await loadEnabledImageFields(this.params && this.params.projectId)
        this.displayImageUrls = await resolveShareCardImageUrls(this.params, fields)
      }
      // 同缺陷/时效/图源已截过：直接命中，无需再等 html2canvas
      const hit = getCachedSnapshot(this.currentSnapshotKey())
      if (hit) {
        this.markSnapshotReady(hit)
      }
      await this.createQRCode(this.getDefectUrl(this.shard.defectShardId));
      await this.drawImage();
      if (!this.cachedPngBlob) {
        this.kickPrewarm()
      }
    },
    async copy(shard) {
      this.shard = shard
      if (!this.$refs.share) {
        throw new Error('share card not ready')
      }
      // 若预截图未完成，复用同一 Promise，避免重复 html2canvas
      const blob = await this.ensureSnapshot()

      if (navigator.clipboard && window.ClipboardItem) {
        await navigator.clipboard.write([
          new ClipboardItem({ 'image/png': blob })
        ])
        this.$emit('copy')
        return
      }
      if (document.execCommand && document.queryCommandSupported('copy') && this.$refs.myImg) {
        // 仅旧浏览器兜底才造 dataURL
        if (!this.shardBase64Img) {
          this.shardBase64Img = await new Promise((resolve, reject) => {
            const reader = new FileReader()
            reader.onload = () => resolve(reader.result)
            reader.onerror = reject
            reader.readAsDataURL(blob)
          })
        }
        this.getSelect(this.$refs.myImg)
        document.execCommand('copy')
        window.getSelection().removeAllRanges()
        this.$emit('copy')
        return
      }
      throw new Error('clipboard unavailable')
    },
    getSelect(targetNode) {
      if (window.getSelection) {
        //chrome等主流浏览器
        let selection = window.getSelection();
        let range = document.createRange();
        range.selectNode(targetNode);
        // range.comparePoint(targetNode,0)
        // range.setStart(targetNode,0)
        // range.setEnd(targetNode,0);
        // range.insertNode(targetNode)
        selection.removeAllRanges();
        selection.addRange(range);
        // targetNode.focus();
      }
    },
    base64toFile (dataBase64, filename = "file") {
      const arr = dataBase64.split(",");
      const mime = arr[0].match(/:(.*?);/)[1]; // 获取file文件流的type名称
      const suffix = mime.split("/")[1]; // 获取文件类型
      const bstr = window.atob(arr[1]);
      let n = bstr.length;
      const u8arr = new Uint8Array(n);
      while (n--) {
        u8arr[n] = bstr.charCodeAt(n);
      }
      return new File([u8arr], `${filename}.${suffix}`, {
        type: mime
      });
    },
    imageBase64(img) {
      let canvas = document.createElement("canvas");
      canvas.width = img.width;
      canvas.height = img.height;
      let ctx = canvas.getContext("2d");
      ctx.drawImage(img, 0, 0, img.width, img.height);
      let dataURL = canvas.toDataURL("image/png");
      return dataURL;
    },
    base64ToBlob(b64Data, contentType, sliceSize) {
      contentType = contentType || "";
      sliceSize = sliceSize || 512;
      let byteCharacters = window.atob(b64Data);
      // var byteCharacters  = b64Data;
      // 该atob函数将base64编码的字符串解码为一个新字符串，其中包含二进制数据每个字节的字符。
      let byteArrays = [];
      for (let offset = 0; offset < byteCharacters.length; offset += sliceSize) {
        let slice = byteCharacters.slice(offset, offset + sliceSize);
        let byteNumbers = new Array(slice.length);
        // 通过使用.charCodeAt字符串中每个字符的方法应用它来创建一个新的数组。
        for (let i = 0; i < slice.length; i++) {
          byteNumbers[i] = slice.charCodeAt(i);
        }
        // 将这个数组转换为实际类型的数组，方法是将其传递给Uint8Array构造函数。
        let byteArray = new Uint8Array(byteNumbers);
        byteArrays.push(byteArray);
      }
      // 创建一个blob：包含这条数据的URL，返回去。
      let blob = new Blob(byteArrays, { type: contentType });
      return blob;
    },
  }
}
</script>

<style lang="scss" scoped>
 .share {
   width: 100%;
   min-width: 0;
   max-width: 100%;
   box-sizing: border-box;
   display: inline-flex;
   flex-direction: column;
   justify-content: flex-start;
   background-color: var(--share-card-bg, #f4f4f4);
   color: var(--text-color-primary);
   padding: 0px 10px 10px 10px;
   border-radius: 5px;
   position: relative;
 }
 /* 仅截图瞬间使用：去掉外层圆角，避免导出图四角白边 */
 .share.share--capturing {
   border-radius: 0 !important;
 }
  .header {
    display: inline-flex;
    flex-direction: row;
    justify-content: flex-start;
    align-items: center;
    gap: 10px;
    .defect-title-num,
    .defect-title-name {
      color: var(--text-color-primary);
    }
    .defect-title-num {
      margin-left: 5px;
    }
    .defect-title-name {
      flex: 1;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }
  }
  .body {
    padding: 10px 10px;
    background-color: var(--share-card-surface-bg, #ffffff);
    color: var(--text-color-regular);
    border-radius: 5px;
    margin-bottom: 10px;
    font-size: 12px;

    label {
      color: var(--text-color-secondary);
    }
  }
 .img-list {
    width: 100%;
    display: inline-flex;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 10px;
  }
  .share-img-wrap {
    flex: 1;
    width: 0;
    height: 50px;
    border-radius: var(--cat2bug-border-radius, 4px);
    overflow: hidden;

    &--hero {
      width: 100%;
      height: 150px;
      flex: auto;
    }

    canvas {
      display: block;
      width: 100%;
      height: 100%;
    }
  }
  .footer {
    display: inline-flex;
    flex-direction: row;
    justify-content: flex-start;
    align-items: center;
    gap: 5px;
    padding: 15px 10px;
    background-color: var(--share-card-surface-bg, #ffffff);
    color: var(--text-color-regular);
    border-radius: 5px;
    width: 100%;
    overflow: hidden;
    box-sizing: border-box;
  }
  .qr-wrap {
    flex-shrink: 0;
    width: 100px;
    height: 100px;
    border-radius: var(--cat2bug-border-radius, 4px);
    overflow: hidden;

    .qr {
      display: block;
      width: 100% !important;
      height: 100% !important;
    }
  }
  .footer > span {
    flex: 1;
    min-width: 0;
  }
  .describe {
    margin-top: 10px;
    -webkit-line-clamp: 2;
    overflow: hidden;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 4;
    text-overflow: ellipsis;
  }
  .font-mini {
    font-size: 10px;
    margin-right: 5px;
    padding: 0 5px;
    height: 22px;
    line-height: 20px;
  }
  .flag {
    display: inline-flex;
    flex-direction: row;
    align-items: center;
    justify-content: flex-start;
  }
  .link {
    margin-top: 10px;
    padding: 0px 10px;
    width: 100%;
    word-break:break-all;
    white-space: normal;
  }
  label {
    padding-right: 10px;
  }
  .el-link {
    color: var(--share-card-link-color, #1890ff);
    text-decoration: underline;
    text-decoration-color: var(--share-card-link-color, #1890ff);
  }
  .myImg {
    opacity: 0;
    position: absolute;
  }
</style>
