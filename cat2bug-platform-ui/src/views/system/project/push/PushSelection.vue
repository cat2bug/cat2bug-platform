<template>
  <div class="push-selection" v-loading="loading">
    <el-alert
      v-if="error"
      type="error"
      :closable="false"
      show-icon
      :title="error"
    >
      <el-button link type="primary" @click="$emit('reload')">{{ $t('project.push.reload-estimate') }}</el-button>
    </el-alert>

    <section class="push-selection__block" :aria-label="$t('project.push.required')">
      <h3 class="push-selection__title">{{ $t('project.push.required') }}</h3>
      <ul class="push-required">
        <li v-for="item in requiredItems" :key="item.key" class="push-choice">
          <el-checkbox :model-value="true" disabled>
            <span class="push-choice__name">{{ $t(labelOf(item.key)) }}</span>
            <span class="push-choice__lock">{{ $t('project.push.required-locked') }}</span>
          </el-checkbox>
          <span class="push-choice__size">{{ approx(item.bytes) }}</span>
        </li>
      </ul>
    </section>

    <section class="push-selection__block" :aria-label="$t('project.push.git')">
      <h3 class="push-selection__title">{{ $t('project.push.git') }}</h3>
      <p class="push-selection__hint">{{ $t('project.push.git-hint') }}</p>
      <p v-if="!repositories.length" class="push-selection__empty">{{ $t('project.push.git-empty') }}</p>
      <el-checkbox-group
        v-else
        class="push-git"
        :model-value="checkedGitSlugs"
        @change="onGitChange"
      >
        <div v-for="repo in repositories" :key="repo.slug" class="push-choice">
          <el-checkbox :value="repo.slug" :disabled="locked">
            <span class="push-choice__name">{{ repo.name || repo.slug }}</span>
          </el-checkbox>
          <span class="push-choice__size">{{ approx(repo.bytes) }}</span>
        </div>
      </el-checkbox-group>
    </section>

    <section class="push-selection__block" :aria-label="$t('project.push.documents')">
      <h3 class="push-selection__title">{{ $t('project.push.documents') }}</h3>
      <p v-if="!documents.length" class="push-selection__empty">{{ $t('project.push.documents-empty') }}</p>
      <el-tree
        v-else
        ref="docTree"
        class="push-docs"
        :class="{ 'is-locked': locked }"
        :data="documents"
        node-key="id"
        show-checkbox
        default-expand-all
        :props="treeProps"
        @check="onTreeCheck"
      >
        <template #default="{ data }">
          <span class="push-doc-node">
            <span class="push-doc-node__name" :title="data.name">{{ data.name }}</span>
            <span v-if="data.localMissing" class="push-doc-node__missing">{{ $t('project.push.local-missing') }}</span>
            <span v-else class="push-doc-node__size">{{ approx(data.bytes) }}</span>
          </span>
        </template>
      </el-tree>
    </section>

    <div class="push-total" :class="{ 'is-over': overLimit }" role="status">
      <span>{{ $t('project.push.total') }}</span>
      <strong>{{ $t('project.push.total-of', { used: formatByteSize(totalBytes), max: formatByteSize(maxTotalBytes) }) }}</strong>
    </div>
    <p class="push-selection__hint">{{ $t('project.push.approx-note') }}</p>
    <el-alert
      v-if="overLimit"
      class="push-total__alert"
      type="warning"
      :closable="false"
      show-icon
      :title="requiredOverLimit ? $t('project.push.required-over-limit') : $t('project.push.over-limit')"
    />
    <el-button
      v-if="overLimit"
      :disabled="!canRemoveLargest"
      @click="$emit('remove-largest')"
    >{{ $t('project.push.remove-largest') }}</el-button>
  </div>
</template>

<script>
import { formatByteSize, selectableCheckedDocIds } from './push-estimate'

const LABEL_KEYS = {
  defect: 'defect',
  case: 'case',
  plan: 'plan',
  module: 'module'
}

export default {
  name: 'PushSelection',
  props: {
    loading: { type: Boolean, default: false },
    error: { type: String, default: '' },
    locked: { type: Boolean, default: false },
    requiredItems: { type: Array, default: () => [] },
    repositories: { type: Array, default: () => [] },
    documents: { type: Array, default: () => [] },
    checkedGitSlugs: { type: Array, default: () => [] },
    checkedDocIds: { type: Array, default: () => [] },
    totalBytes: { type: Number, default: 0 },
    maxTotalBytes: { type: Number, default: 0 },
    overLimit: { type: Boolean, default: false },
    requiredOverLimit: { type: Boolean, default: false },
    canRemoveLargest: { type: Boolean, default: false }
  },
  emits: ['update:checkedGitSlugs', 'update:checkedDocIds', 'remove-largest', 'reload'],
  data() {
    return {
      treeProps: { label: 'name', children: 'children', disabled: 'disabled' }
    }
  },
  watch: {
    documents() {
      this.syncTree()
    },
    checkedDocIds() {
      this.syncTree()
    }
  },
  mounted() {
    this.syncTree()
  },
  methods: {
    formatByteSize,
    approx(bytes) {
      if (bytes == null || bytes === '') return '—'
      return this.$t('project.push.size-approx', { size: formatByteSize(bytes) })
    },
    labelOf(key) {
      return LABEL_KEYS[key] || key
    },
    onGitChange(values) {
      if (this.locked) return
      this.$emit('update:checkedGitSlugs', values)
    },
    onTreeCheck() {
      const tree = this.$refs.docTree
      if (!tree) return
      if (this.locked) {
        this.syncTree()
        return
      }
      const checked = tree.getCheckedNodes(true) || []
      const ids = selectableCheckedDocIds(this.documents, checked.map((node) => node.id))
      if (ids.length !== checked.length) {
        tree.setCheckedKeys(ids)
      }
      this.$emit('update:checkedDocIds', ids)
    },
    syncTree() {
      this.$nextTick(() => {
        const tree = this.$refs.docTree
        if (!tree) return
        const next = selectableCheckedDocIds(this.documents, this.checkedDocIds)
        const current = (tree.getCheckedKeys(true) || []).map(String).sort().join(',')
        if (current !== next.slice().sort().join(',')) {
          tree.setCheckedKeys(next)
        }
      })
    }
  }
}
</script>

<style scoped>
.push-selection__block {
  margin-top: 16px;
}
.push-selection__title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
}
.push-selection__hint,
.push-selection__empty {
  margin: 0 0 8px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary, #909399);
}
.push-choice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  max-width: 720px;
}
.push-git :deep(.el-checkbox) {
  height: auto;
  margin-right: 0;
  padding: 4px 0;
}
.push-required {
  margin: 0;
  padding: 0;
  list-style: none;
}
.push-choice__lock,
.push-choice__size,
.push-doc-node__size,
.push-doc-node__missing {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
  font-variant-numeric: tabular-nums;
}
.push-choice__lock {
  color: var(--el-text-color-placeholder, #a8abb2);
}
.push-required :deep(.el-checkbox.is-disabled.is-checked .el-checkbox__inner) {
  background-color: var(--el-color-primary);
  border-color: var(--el-color-primary);
}
.push-required :deep(.el-checkbox.is-disabled.is-checked .el-checkbox__inner::after) {
  border-color: #fff;
}
.push-required :deep(.el-checkbox.is-disabled .el-checkbox__label) {
  color: var(--el-text-color-primary, #303133);
}
.push-docs {
  max-width: 720px;
}
.push-docs.is-locked :deep(.el-checkbox) {
  pointer-events: none;
}
.push-docs :deep(.el-tree-node__content) {
  height: auto;
  min-height: 26px;
}
.push-doc-node {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  padding-right: 8px;
}
.push-doc-node__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.push-doc-node__missing {
  color: var(--el-color-warning);
  white-space: nowrap;
}
.push-total {
  display: flex;
  align-items: baseline;
  gap: 12px;
  max-width: 720px;
  margin-top: 16px;
  font-size: 14px;
}
.push-total.is-over strong {
  color: var(--el-color-danger);
}
.push-total__alert {
  max-width: 720px;
  margin: 8px 0;
}
</style>
