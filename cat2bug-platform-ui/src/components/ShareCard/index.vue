<template>
  <el-popover
    ref="sharePopover"
    class="share-card-popover"
    placement="left-start"
    trigger="click"
    :teleported="true"
    popper-class="share-card-popper"
    :width="sharePopoverWidth"
    :popper-options="sharePopperOptions"
    v-model:visible="visible">
    <div v-if="visible" class="shard-context" v-loading="apiLoading">
      <div class="shard-context__toolbar">
        <el-form :inline="true" :model="form">
          <el-form-item :label="$t('shard.aging-hour')">
            <el-select
              v-model="form.agingHour"
              class="share-aging-select"
              size="small"
              :disabled="apiLoading"
              @change="refresh"
            >
              <el-option :label="$t('shard.1hour')" :value="'1'" />
              <el-option :label="$t('shard.1day')" :value="'24'" />
              <el-option :label="$t('shard.7day')" :value="'168'" />
              <el-option :label="$t('shard.permanent')" :value="'0'" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('password')">
            <el-input
              v-model="form.password"
              class="share-password-input"
              size="small"
              clearable
              :disabled="apiLoading"
              :placeholder="$t('shard.please-enter-password')"
              minlength="4"
              maxlength="4"
              @change="refresh"
            />
          </el-form-item>
        </el-form>
      </div>
      <div class="title">
        <label>{{ $t('shard.click-prompt') }}</label>
      </div>
      <!-- 必须用 v-if：加载中不要挂两张重卡片，否则主线程卡死、奔跑猫画不出来 -->
      <div v-if="!apiLoading && shard" class="share-list">
        <span
          v-for="(sc,index) in shareCardList"
          :key="index"
          class="share-card-item"
          :class="{
            'is-copying': copyingIndex === index,
            'is-snapshot-ready': !!snapshotReadyMap[index]
          }"
          @click="copy(index)"
        >
          <span class="share-card-item__mask" aria-hidden="true">{{ $t('click-copy') }}</span>
          <component
            ref="shareComponent"
            :is="sc.name"
            :params="params"
            :agingHour="form.agingHour"
            :password="form.password"
            :resolved-image-urls="resolvedImageUrls"
            @copy="copyHandle"
            @snapshot-ready="(ready) => onSnapshotReady(index, ready)"
          />
        </span>
      </div>
      <div v-else class="share-list share-list--placeholder" aria-hidden="true" />
    </div>
    <template #reference>
      <span class="share-card-trigger" @click="clickHandle">
        <svg-icon class="button" icon-class="shard"></svg-icon>
      </span>
    </template>
  </el-popover>
</template>

<script>
import {getShard, shardDefect} from "@/api/system/DefectShard";
import {
  loadEnabledImageFields,
  resolveShareCardImageUrls
} from '@/utils/share-card-images'

const shareCardModules = import.meta.glob('./DefectShareCard/*.vue', { eager: true })
const modules = {}

for (const [filePath, mod] of Object.entries(shareCardModules)) {
  const name = filePath.split('/').pop().replace(/\.vue$/, '')
  modules[name] = mod.default || mod
}

/** 固定顺序：图卡在左、文本在右 */
const SHARE_CARD_ORDER = ['DefaultDefectShareCard', 'TextDefectShareCard']

export default {
  name: "ShareCard",
  components: modules,
  data() {
    return {
      apiLoading: false,
      copyingIndex: -1,
      /** 各卡片预截图是否已就绪（图卡点复制才接近瞬时） */
      snapshotReadyMap: {},
      form: {
        defectId: this.params.defectId,
        agingHour: '1',
        password: null
      },
      shard: null,
      visible: false,
      resolvedImageUrls: [],
      shareCardList: SHARE_CARD_ORDER
        .filter((name) => modules[name])
        .map((name) => modules[name])
    }
  },
  props: {
    params: {
      type: Object,
      default: ()=> {}
    }
  },
  computed: {
    /** 两项横排：两卡约 300×2 + 间距；窄屏再压到视口内 */
    sharePopoverWidth() {
      if (typeof window === 'undefined') return 640
      return Math.min(640, Math.max(320, window.innerWidth - 24))
    },
    sharePopperOptions() {
      return {
        strategy: 'fixed',
        modifiers: [
          {
            name: 'offset',
            options: { offset: [0, 10] }
          },
          {
            name: 'flip',
            options: {
              fallbackPlacements: ['left', 'left-end', 'right-start', 'bottom-start', 'top-start']
            }
          },
          {
            name: 'preventOverflow',
            options: {
              padding: 12,
              boundary: 'viewport',
              altAxis: true
            }
          }
        ]
      }
    }
  },
  watch: {
    visible: function (n) {
      if (n) {
        this.normalizeAgingHour()
        // 先亮 loading，双 rAF 后再拉数据，保证奔跑猫先画出来
        this.apiLoading = true
        this.shard = null
        this.resolvedImageUrls = []
        this.copyingIndex = -1
        this.snapshotReadyMap = {}
        this.schedulePopperUpdate()
        this.$nextTick(() => {
          requestAnimationFrame(() => {
            requestAnimationFrame(() => {
              this.refresh()
            })
          })
        })
      } else {
        this.copyingIndex = -1
      }
    }
  },
  methods: {
    normalizeAgingHour() {
      const allowed = new Set(['0', '1', '24', '168'])
      const raw = this.form.agingHour
      const s = raw == null || raw === '' ? '1' : String(raw)
      this.form.agingHour = allowed.has(s) ? s : '1'
    },
    /** 内容高度变化后重算弹层，避免 placement=top 时顶出屏幕 */
    schedulePopperUpdate() {
      const run = () => {
        const pop = this.$refs.sharePopover
        if (pop && typeof pop.updatePopper === 'function') {
          pop.updatePopper()
        }
      }
      this.$nextTick(() => {
        run()
        requestAnimationFrame(() => {
          run()
          setTimeout(run, 50)
          setTimeout(run, 200)
        })
      })
    },
    /** 等浏览器至少画完一帧（loading 可见） */
    waitPaint() {
      return new Promise((resolve) => {
        this.$nextTick(() => {
          requestAnimationFrame(() => requestAnimationFrame(resolve))
        })
      })
    },
    /** 刷新 */
    async refresh() {
      this.normalizeAgingHour()
      this.form.defectId = this.params && this.params.defectId;
      if (!this.form.defectId) {
        this.apiLoading = false;
        this.schedulePopperUpdate();
        return;
      }
      this.apiLoading = true;
      this.schedulePopperUpdate();
      await this.waitPaint()
      // 取图与分享 API 并行，缩短卡住时间
      const imagePromise = (async () => {
        const fields = await loadEnabledImageFields(this.params && this.params.projectId)
        return resolveShareCardImageUrls(this.params, fields)
      })()
      try {
        const [res, urls] = await Promise.all([
          shardDefect(this.form),
          imagePromise
        ])
        this.shard = res.data
        this.resolvedImageUrls = urls || []
      } catch (_e) {
        /* 错误提示由 request 拦截器处理 */
      } finally {
        // 先关 loading、挂载卡片，再刷内容（此时主线程已让出过）
        this.apiLoading = false
        this.schedulePopperUpdate()
        await this.waitPaint()
        await this.refreshComponents()
      }
    },
    /** 刷新分享组件 */
    async refreshComponents() {
      await this.$nextTick()
      const refs = this.$refs.shareComponent
      if (!refs) {
        this.schedulePopperUpdate()
        return
      }
      const list = Array.isArray(refs) ? refs : [refs]
      await Promise.all(
        list.map((c) => {
          if (c && typeof c.refresh === 'function') {
            return Promise.resolve(c.refresh(this.shard, this.resolvedImageUrls))
          }
          return Promise.resolve()
        })
      )
      this.schedulePopperUpdate()
    },
    onSnapshotReady(index, ready) {
      this.snapshotReadyMap = { ...this.snapshotReadyMap, [index]: !!ready }
    },
    /** 复制数据到剪贴板：必须等截图/写入完成后再关弹层，否则图卡 DOM 已被销毁 */
    async copy(index) {
      if (this.copyingIndex >= 0) return
      const refs = this.$refs.shareComponent
      const comp = Array.isArray(refs) ? refs[index] : refs
      if (!comp || typeof comp.copy !== 'function') return
      this.copyingIndex = index
      try {
        await comp.copy(this.shard, this.form.agingHour, this.form.password)
        this.visible = false
      } catch (_e) {
        this.$message.error(this.$i18n.t('copy-fail'))
      } finally {
        this.copyingIndex = -1
      }
    },
    /** 复制剪贴板成功处理事件 */
    copyHandle() {
      this.$message.success(this.$i18n.t('copy-success').toString());
    },
    /** 点击分享按钮事件 */
    clickHandle(e) {
      e.stopPropagation();
    },
    /** 获取共享信息 */
    getShardInfo(defectId) {
      if(defectId) {
        getShard(defectId).then(res=>{
          if(res.data) {
            this.form = res.data;
            this.normalizeAgingHour()
          }
        });
      }
    }
  }
}
</script>
<style>
.share-card-loading .el-icon-document-copy {
  font-size: 40px;
}
html.dark .share-card-loading .el-loading-mask {
  background-color: rgba(0, 0, 0, 0.45) !important;
}
.el-popover.el-popper.share-card-popper {
  box-sizing: border-box;
  max-width: min(640px, calc(100vw - 24px)) !important;
  max-height: min(80vh, 720px);
  padding: 10px 8px !important;
  overflow-x: hidden;
  overflow-y: auto;
}
</style>
<style lang="scss" scoped>
  .shard-context {
    max-width: 100%;
    min-height: 240px;
    box-sizing: border-box;
    position: relative;
  }
  .shard-context__toolbar {
    margin: 0 0 8px 0;
  }
  .share-aging-select {
    width: 128px;
  }
  .share-password-input {
    width: 120px;
  }
  .title {
    margin: 0 0 8px 0;
    background-color: var(--share-card-title-bg, #f4f4f4);
    border-radius: 3px;
    padding: 3px 5px;
    color: var(--text-color-regular);

    label {
      color: inherit;
    }
  }
  .share-list {
    display: flex;
    flex-direction: row;
    flex-wrap: nowrap;
    gap: 8px;
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
  }
  .share-list--placeholder {
    min-height: 180px;
  }
  .share-list > .share-card-item {
    position: relative;
    overflow: hidden;
    display: flex;
    flex: 1 1 0;
    min-width: 0;
    max-width: 100%;
    cursor: pointer;
    border-radius: 5px;
  }
  .share-card-item__mask {
    position: absolute;
    inset: 0;
    z-index: 2;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(255, 255, 255, 0.72);
    color: var(--text-color-primary, #303133);
    font-size: 16px;
    font-weight: 600;
    opacity: 0;
    pointer-events: none;
    /* 不用 transition：悬停时若顺带跑截图会卡住过渡，看起来像「等一秒才出提示」 */
  }
  html.dark .share-card-item__mask {
    background: rgba(0, 0, 0, 0.55);
    color: var(--text-color-primary, #e5eaf3);
  }
  .share-card-item:hover .share-card-item__mask,
  .share-card-item.is-copying .share-card-item__mask {
    opacity: 1;
  }
  .share-card-trigger {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 auto;
    width: 14px;
    max-width: 14px;
    min-width: 0;
    overflow: hidden;
    line-height: 1;
    vertical-align: middle;
    cursor: pointer;
  }

  .button {
    color: var(--text-color-secondary, #8c8c8c);
    font-size: 14px;
  }
  .el-form-item {
    margin-bottom: 0px;
  }
</style>
