<template>
  <div class="doc-pin" v-loading="loading">
    <template v-if="editing && !docId">
      <div class="doc-pin__pick">
        <div class="doc-pin__pick-title">{{ $t('workbench.doc-pin.pick-title') }}</div>
        <el-select
          filterable
          clearable
          :placeholder="$t('workbench.doc-pin.pick-placeholder')"
          :model-value="docId || undefined"
          style="width: 100%"
          @update:model-value="onPick"
        >
          <el-option
            v-for="d in mdOptions"
            :key="d.docId"
            :label="d.docName || ('#' + d.docId)"
            :value="d.docId"
          />
        </el-select>
        <div v-if="!mdOptions.length && !loading" class="doc-pin__hint">
          {{ $t('workbench.doc-pin.empty-md') }}
        </div>
      </div>
    </template>
    <template v-else-if="!doc || loadError">
      <div class="doc-pin__empty">
        <div>{{ $t('workbench.doc-pin.missing') }}</div>
        <el-button v-if="editing" link type="primary" @click="reload">{{ $t('workbench.doc-pin.reselect') }}</el-button>
      </div>
    </template>
    <div v-else class="doc-pin__panel" :class="{ 'is-clickable': !editing }" @click="openDoc">
      <div class="doc-pin__hd">
        <div class="doc-pin__hd-main">
          <span class="doc-pin__badge">Markdown</span>
          <span class="doc-pin__name" :title="doc.docName">{{ doc.docName || ('#' + doc.docId) }}</span>
        </div>
        <div class="doc-pin__meta">
          <span v-if="doc.updateTime">{{ doc.updateTime }}</span>
          <span v-else-if="doc.createTime">{{ doc.createTime }}</span>
        </div>
      </div>
      <div class="doc-pin__body markdown-body">
        <markdown-content v-if="mdText" :content="mdText" />
        <div v-else-if="!loading" class="doc-pin__body-empty">{{ $t('workbench.doc-pin.no-content') }}</div>
      </div>
      <div v-if="editing" class="doc-pin__edit-actions" @click.stop>
        <el-button link type="primary" size="small" @click="showRepick = !showRepick">
          {{ $t('workbench.doc-pin.change') }}
        </el-button>
        <el-select
          v-if="showRepick"
          filterable
          size="small"
          :placeholder="$t('workbench.doc-pin.pick-placeholder')"
          :model-value="docId"
          style="width: 100%; margin-top: 6px"
          @update:model-value="onPick"
        >
          <el-option
            v-for="d in mdOptions"
            :key="d.docId"
            :label="d.docName || ('#' + d.docId)"
            :value="d.docId"
          />
        </el-select>
      </div>
    </div>
  </div>
</template>

<script>
import { listDocument, getDocument, fetchDocumentMarkdown } from '@/api/system/document'
import { getCurrentProjectId } from '@/utils/store-context'
import MarkdownContent from '@/components/Markdown/MarkdownContent.vue'

function isMarkdown(d) {
  const ext = String(d?.fileExtension || '').toLowerCase()
  return ext === 'md' || ext === 'markdown'
}

export default {
  name: 'DocumentPin',
  components: { MarkdownContent },
  props: {
    docId: { type: [Number, String], default: null },
    editing: { type: Boolean, default: false }
  },
  emits: ['update:docId'],
  data() {
    return {
      loading: false,
      doc: null,
      mdText: '',
      loadError: false,
      mdOptions: [],
      showRepick: false
    }
  },
  watch: {
    docId: {
      immediate: true,
      handler() {
        this.load()
      }
    },
    editing(v) {
      if (v) this.loadOptions()
    }
  },
  mounted() {
    if (this.editing) this.loadOptions()
  },
  methods: {
    async load() {
      if (!this.docId) {
        this.doc = null
        this.mdText = ''
        this.loadError = false
        return
      }
      this.loading = true
      this.loadError = false
      this.mdText = ''
      try {
        const res = await getDocument(this.docId)
        const data = res.data || res
        if (!data || !isMarkdown(data)) {
          this.doc = null
          this.loadError = true
          return
        }
        this.doc = data
        if (data.fileUrl) {
          try {
            this.mdText = (await fetchDocumentMarkdown(data.fileUrl)) || ''
          } catch (e) {
            this.mdText = ''
          }
        }
      } catch (e) {
        this.doc = null
        this.mdText = ''
        this.loadError = true
      } finally {
        this.loading = false
      }
    },
    async loadOptions() {
      const projectId = getCurrentProjectId()
      if (!projectId) return
      try {
        const res = await listDocument({ projectId, pageNum: 1, pageSize: 100 })
        this.mdOptions = (res.rows || []).filter(d => Number(d.docType) > 0 && isMarkdown(d))
      } catch (e) {
        this.mdOptions = []
      }
    },
    onPick(val) {
      if (val == null || val === '') {
        this.$emit('update:docId', null)
        return
      }
      const hit = this.mdOptions.find(d => String(d.docId) === String(val))
      if (hit && !isMarkdown(hit)) {
        this.$modal?.msgWarning?.(this.$t('workbench.doc-pin.only-md'))
        return
      }
      this.showRepick = false
      this.$emit('update:docId', Number(val) || val)
    },
    openDoc() {
      if (this.editing) return
      if (!this.doc?.docId) return
      this.$router.push({
        name: 'HandleMarkdownDocument',
        params: { docId: String(this.doc.docId) }
      })
    },
    reload() {
      this.loadOptions()
      this.showRepick = true
    },
    refresh() {
      return this.load()
    }
  }
}
</script>

<style scoped lang="scss">
.doc-pin {
  height: 100%;
  min-height: 120px;
  box-sizing: border-box;
}
.doc-pin__panel {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 120px;
  margin: 0;
  padding: 0;
  text-align: left;
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 10px;
  background: var(--el-bg-color, #fff);
  color: var(--text-color-primary);
  box-sizing: border-box;
  overflow: hidden;
}
.doc-pin__panel.is-clickable {
  cursor: pointer;
}
.doc-pin__panel.is-clickable:hover {
  border-color: var(--el-color-primary-light-5, #a0cfff);
}
.doc-pin__hd {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px 8px;
  border-bottom: 1px solid var(--el-border-color-lighter, #ebeef5);
  background: var(--el-fill-color-blank, #fafafa);
}
.doc-pin__hd-main {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.doc-pin__badge {
  flex-shrink: 0;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--el-color-primary);
}
.doc-pin__name {
  min-width: 0;
  font-size: 13px;
  font-weight: 650;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc-pin__meta {
  font-size: 11px;
  color: var(--text-color-secondary);
}
.doc-pin__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  padding: 10px 12px 12px;
  font-size: 13px;
  line-height: 1.55;
  box-sizing: border-box;
}
.doc-pin__body-empty {
  color: var(--text-color-secondary);
  font-size: 12px;
}
.doc-pin__body :deep(h1),
.doc-pin__body :deep(h2),
.doc-pin__body :deep(h3) {
  margin: 0.6em 0 0.35em;
  font-size: 1.05em;
  line-height: 1.35;
}
.doc-pin__body :deep(h1:first-child),
.doc-pin__body :deep(h2:first-child),
.doc-pin__body :deep(h3:first-child),
.doc-pin__body :deep(p:first-child) {
  margin-top: 0;
}
.doc-pin__body :deep(p) {
  margin: 0.4em 0;
}
.doc-pin__body :deep(img) {
  max-width: 100%;
  height: auto;
}
.doc-pin__body :deep(pre) {
  max-width: 100%;
  overflow: auto;
  font-size: 12px;
}
.doc-pin__pick,
.doc-pin__empty {
  padding: 14px;
  border: 1px dashed var(--el-border-color, #dcdfe6);
  border-radius: 10px;
  background: var(--el-fill-color-blank, #fff);
  color: var(--text-color-secondary);
  font-size: 13px;
  box-sizing: border-box;
  min-height: 88px;
}
.doc-pin__pick-title {
  margin-bottom: 8px;
  font-weight: 600;
  color: var(--text-color-primary);
}
.doc-pin__hint {
  margin-top: 8px;
  font-size: 12px;
}
.doc-pin__edit-actions {
  flex: 0 0 auto;
  width: 100%;
  padding: 6px 12px 10px;
  border-top: 1px solid var(--el-border-color-lighter, #ebeef5);
  box-sizing: border-box;
}
</style>
