<template>
  <div class="wb-list wb-repair" v-loading="loading">
    <div v-if="!rows.length && !loading" class="wb-empty">{{ $t('workbench.empty-list') }}</div>
    <div v-for="row in rows" :key="row.defectId" class="wb-repair__row">
      <div
        class="wb-repair__thumb"
        :class="{ 'is-empty': !rowImages(row).length }"
        @click.stop="onThumbClick(row)"
      >
        <cat2-bug-preview-image
          v-if="rowImages(row).length"
          class="wb-repair__preview"
          :images="rowImages(row)"
          :width="52"
          :height="52"
        />
        <span
          v-if="rowImages(row).length > 1"
          class="wb-repair__count"
        >+{{ rowImages(row).length }}</span>
        <div v-else-if="!rowImages(row).length" class="wb-repair__empty" :title="$t('workbench.no-image')">
          <span class="wb-repair__empty-mark" aria-hidden="true" />
          <span class="wb-repair__empty-text">{{ $t('workbench.no-image') }}</span>
        </div>
      </div>

      <div class="wb-repair__body" @click="openDefect(row)">
        <div class="wb-repair__title">
          <span class="id">#{{ row.projectNum || row.defectId }}</span>
          <span class="desc" :title="row.defectName">{{ row.defectName }}</span>
        </div>
        <div class="wb-repair__meta">
          <defect-state-flag :defect="stateDefect(row)" />
          <span class="chip level" v-if="isHigh(row)">{{ levelLabel(row) }}</span>
          <span class="meta-text">{{ $t('workbench.meta.assigned-me') }}</span>
        </div>
      </div>

      <div class="wb-repair__actions">
        <button
          v-if="canRepair"
          type="button"
          class="wb-op orange"
          @click.stop="openDefect(row)"
        >{{ $t('workbench.repair') }}</button>
        <button
          v-if="canAssign"
          type="button"
          class="wb-op orange"
          @click.stop="openDefect(row)"
        >{{ $t('workbench.assign') }}</button>
        <button type="button" class="wb-op muted" @click.stop="openDefect(row)">{{ $t('detail') }}</button>
      </div>
    </div>
    <div class="wb-more" v-if="total > rows.length">
      <el-button link type="primary" @click="goDefectList">{{ $t('workbench.view-more') }}</el-button>
    </div>
    <handle-defect ref="handleDefect" :project-id="projectId" @change="load" @delete="load" />
  </div>
</template>

<script>
import { listDefect } from '@/api/system/defect'
import { listEnabledFields } from '@/api/system/defect-field'
import { urlListFromCustomFieldValue } from '@/components/DefectCustomField/format'
import { getCurrentProjectId, getCurrentUserId } from '@/utils/store-context'
import { buildProjectModulePath } from '@/utils/project-module-path'
import { checkPermi } from '@/utils/permission'
import { resolveCommaSeparatedUrls } from '@/utils/upload-asset'
import HandleDefect from '@/components/Defect/HandleDefect'
import DefectStateFlag from '@/components/Defect/DefectStateFlag'
import Cat2BugPreviewImage from '@/components/Cat2BugPreviewImage'

const STATE_NAME = { 0: 'PROCESSING', 1: 'AUDIT', 2: 'CLOSED', 3: 'REJECTED' }
const IMG_CACHE = new WeakMap()

export default {
  name: 'DefectsPendingRepair',
  components: { HandleDefect, DefectStateFlag, Cat2BugPreviewImage },
  data() {
    return {
      loading: false,
      rows: [],
      total: 0,
      projectId: null,
      imageFieldKeys: [],
      _imageKeysProjectId: null
    }
  },
  computed: {
    canRepair() { return checkPermi(['system:defect:repair']) },
    canAssign() { return checkPermi(['system:defect:assign']) }
  },
  mounted() {
    this.projectId = getCurrentProjectId()
    this.load()
  },
  methods: {
    async ensureImageFieldKeys(projectId) {
      if (!projectId) {
        this.imageFieldKeys = []
        this._imageKeysProjectId = null
        return
      }
      if (this._imageKeysProjectId === projectId) return
      try {
        const res = await listEnabledFields(projectId)
        const list = res.data || res.rows || []
        this.imageFieldKeys = (Array.isArray(list) ? list : [])
          .filter((f) => f && f.fieldType === 'image' && f.fieldKey)
          .slice()
          .sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0))
          .map((f) => f.fieldKey)
        this._imageKeysProjectId = projectId
      } catch (_) {
        this.imageFieldKeys = []
        this._imageKeysProjectId = projectId
      }
    },
    rowImages(row) {
      if (!row) return []
      const cf = row.customFields && typeof row.customFields === 'object' ? row.customFields : null
      const customParts = []
      ;(this.imageFieldKeys || []).forEach((key) => {
        if (!cf || cf[key] == null || cf[key] === '') return
        customParts.push(...urlListFromCustomFieldValue(cf[key]))
      })
      const cacheKey = `${row.imgUrls || ''}|${customParts.join(',')}`
      const hit = IMG_CACHE.get(row)
      if (hit && hit.key === cacheKey) return hit.urls
      // 顺序：内置图片 → 自定义图片字段（与表单字段顺序一致）
      const merged = []
      const seen = new Set()
      ;[...(row.imgUrls ? [row.imgUrls] : []), ...customParts].forEach((raw) => {
        resolveCommaSeparatedUrls(raw).forEach((url) => {
          if (!url || seen.has(url)) return
          seen.add(url)
          merged.push(url)
        })
      })
      IMG_CACHE.set(row, { key: cacheKey, urls: merged })
      return merged
    },
    onThumbClick(row) {
      if (!this.rowImages(row).length) this.openDefect(row)
    },
    stateDefect(row) {
      const name = row.defectStateName || STATE_NAME[Number(row.defectState)] || 'PROCESSING'
      return { ...row, defectStateName: name }
    },
    isHigh(row) {
      const lv = row.defectLevel
      return lv === 'high' || lv === 'urgent' || lv === 2 || lv === 3 || lv === '2' || lv === '3'
    },
    levelLabel(row) {
      const lv = row.defectLevel
      if (lv === 'urgent' || lv === 3 || lv === '3') return this.$t('workbench.level-urgent')
      return this.$t('workbench.level-high')
    },
    async load() {
      const projectId = getCurrentProjectId()
      const userId = getCurrentUserId()
      this.projectId = projectId
      if (!projectId || !userId) return
      this.loading = true
      try {
        await this.ensureImageFieldKeys(projectId)
        const res = await listDefect({
          projectId,
          pageNum: 1,
          pageSize: 8,
          handleBy: [userId],
          defectState: 0,
          params: { delFlag: '0' }
        })
        this.rows = res.rows || []
        this.total = Number(res.total || 0)
      } finally {
        this.loading = false
      }
    },
    openDefect(row) {
      if (this.$refs.handleDefect && row?.defectId) {
        this.$refs.handleDefect.open(row.defectId)
        return
      }
      const path = buildProjectModulePath(this.$route.params.teamKey, this.$route.params.projectKey, 'defect')
      if (path) this.$router.push({ path, query: { defectId: row.defectId } })
    },
    goDefectList() {
      const path = buildProjectModulePath(this.$route.params.teamKey, this.$route.params.projectKey, 'defect')
      if (path) this.$router.push(path)
    },
    refresh() { return this.load() }
  }
}
</script>

<style scoped lang="scss">
@import './wb-list.scss';

.wb-repair {
  padding: 4px 0 0;
}

.wb-repair__row {
  display: grid;
  grid-template-columns: 60px minmax(0, 1fr) auto;
  align-items: center;
  column-gap: 12px;
  min-height: 72px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--border-color-lighter);
  box-sizing: border-box;
}
.wb-repair__row:last-child {
  border-bottom: 0;
}
.wb-repair__row:hover {
  background: var(--table-row-hover-bg, rgba(255, 255, 255, 0.03));
}

.wb-repair__thumb {
  position: relative;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-sizing: border-box;
  overflow: visible;
}
.wb-repair__thumb.is-empty {
  cursor: pointer;
}

/* 自绘数量角标，避免依赖组件内部 popover 结构被裁切 */
.wb-repair__count {
  position: absolute;
  right: 2px;
  bottom: 2px;
  z-index: 4;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 3px;
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
  color: #fff;
  background-color: rgba(0, 0, 0, 0.62);
  pointer-events: none;
  box-sizing: border-box;
}

/* 与缺陷列表同一组件；只约束缩略图 */
.wb-repair__preview {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: visible;
  :deep(.cat2bug-preview-image) {
    overflow: visible;
  }
  :deep(.preview) {
    width: 60px !important;
    height: 60px !important;
    overflow: visible !important;
  }
  :deep(.preview > .el-image),
  :deep(.preview .cat2bug-image),
  :deep(.preview img),
  :deep(.cat2bug-preview-image > div > .el-image),
  :deep(.cat2bug-preview-image > div > .cat2bug-image),
  :deep(.cat2bug-preview-image > div img) {
    width: 60px !important;
    height: 60px !important;
  }
  :deep(.el-image) {
    padding: 0 !important;
    box-sizing: border-box;
  }
  :deep(.click:hover) {
    scale: 1.05;
  }
  /* 与自绘角标二选一，避免叠两个 */
  :deep(.number) {
    display: none !important;
  }
}

/* 无图：刻意不同于 default-image.svg（山景占位） */
.wb-repair__empty {
  width: 60px;
  height: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border-radius: var(--cat2bug-border-radius, 4px);
  border: 1px dashed var(--border-color-light, #dcdfe6);
  background: var(--bg-color-block, #f5f7fa);
  box-sizing: border-box;
  color: var(--text-color-secondary);
}
.wb-repair__empty-mark {
  width: 18px;
  height: 14px;
  border: 1.5px solid currentColor;
  border-radius: 2px;
  opacity: 0.55;
  position: relative;
}
.wb-repair__empty-mark::after {
  content: '';
  position: absolute;
  left: -3px;
  right: -3px;
  top: 50%;
  height: 1.5px;
  background: currentColor;
  transform: rotate(-28deg);
  opacity: 0.85;
}
.wb-repair__empty-text {
  font-size: 11px;
  line-height: 1;
  opacity: 0.75;
}
.wb-repair__thumb.is-empty:hover .wb-repair__empty {
  border-color: var(--cat2bug-primary);
  color: var(--cat2bug-primary);
}

.wb-repair__body {
  min-width: 0;
  cursor: pointer;
}

.wb-repair__title {
  display: flex;
  align-items: baseline;
  gap: 6px;
  min-width: 0;
  font-size: 14px;
  line-height: 1.35;
  color: var(--text-color-primary);
}
.wb-repair__title .id {
  flex-shrink: 0;
  font-weight: 500;
  color: var(--cat2bug-accent, var(--cat2bug-primary));
}
.wb-repair__title .desc {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wb-repair__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-color-secondary);
}
.wb-repair__meta .meta-text {
  color: var(--text-color-secondary);
}
.wb-repair__meta .chip.level {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 6px;
  border-radius: 2px;
  font-size: 12px;
  background: transparent;
  border: 1px solid var(--border-color-light, #414243);
  color: var(--text-color-secondary);
}

.wb-repair__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-shrink: 0;
}

.wb-op {
  border: 0;
  background: transparent;
  padding: 0;
  font-size: 13px;
  cursor: pointer;
  color: var(--cat2bug-operate-text-primary, var(--cat2bug-primary));
}
.wb-op.orange { color: #e6a23c; }
.wb-op.muted { color: var(--text-color-regular); }
.wb-op.muted:hover { color: var(--cat2bug-primary); }
</style>
