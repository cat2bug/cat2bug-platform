<template>
  <div class="document-folder-tree">
    <div class="tree-tools" :style="treeToolsBarStyle">
      <span class="tree-tools-title">
        <legacy-icon icon="el-icon-folder" />
        {{ $t('doc.folder-tree') }}
      </span>
      <el-tooltip v-if="showSidebarToggle" :content="$t('doc.hide-folder-tree')" placement="bottom">
        <span
          class="tree-sidebar-collapse-toggle"
          role="button"
          tabindex="0"
          @click.stop="handleSidebarToggle"
          @keyup.enter.stop.prevent="handleSidebarToggle"
        >
          <legacy-icon icon="el-icon-d-arrow-left" />
        </span>
      </el-tooltip>
    </div>
    <div class="tree-search">
      <el-input
        v-model="filterText"
        clearable
        size="small"
        :prefix-icon="$legacyIcon('el-icon-search')"
        :placeholder="$t('doc.folder-tree-search')"
        @input="onFilterInput"
      />
    </div>
    <el-tree
      v-if="!searchMode"
      :key="'lazy-' + treeEpoch"
      ref="folderTree"
      class="folder-tree"
      :highlight-current="true"
      :props="treeProps"
      :lazy="true"
      :load="loadNode"
      node-key="docId"
      :default-expanded-keys="[0]"
      :expand-on-click-node="false"
      @node-click="handleNodeClick"
    >
      <template #default="{ node, data }">
        <span class="tree-node">
          <span class="tree-node-label" :title="nodeLabel(data)">
            <svg-icon class="tree-node-icon" icon-class="dir" />
            {{ nodeLabel(data) }}
          </span>
          <el-button
            v-if="editVisible"
            link
            size="small"
            class="tree-node-add"
            v-hasPermi="['system:document:add']"
            :title="$t('doc.create-folder')"
            @click.stop="append($event, data)"
          >
            <legacy-icon icon="el-icon-plus" />
          </el-button>
        </span>
      </template>
    </el-tree>
    <el-tree
      v-else
      :key="'search-' + treeEpoch"
      ref="folderTree"
      class="folder-tree"
      :highlight-current="true"
      :data="searchTreeData"
      :props="treeProps"
      node-key="docId"
      :default-expand-all="true"
      :expand-on-click-node="false"
      @node-click="handleNodeClick"
    >
      <template #default="{ node, data }">
        <span class="tree-node">
          <span class="tree-node-label" :title="nodeLabel(data)">
            <svg-icon class="tree-node-icon" icon-class="dir" />
            {{ nodeLabel(data) }}
          </span>
          <el-button
            v-if="editVisible"
            link
            size="small"
            class="tree-node-add"
            v-hasPermi="['system:document:add']"
            :title="$t('doc.create-folder')"
            @click.stop="append($event, data)"
          >
            <legacy-icon icon="el-icon-plus" />
          </el-button>
        </span>
      </template>
    </el-tree>
  </div>
</template>

<script>
import { listDocument } from '@/api/system/document'

const ROOT_ID = 0

export default {
  name: 'DocumentFolderTree',
  props: {
    projectId: {
      type: [Number, String],
      default: null
    },
    /** 当前选中的文件夹 docId；0/null 表示根 */
    currentDocId: {
      type: [Number, String],
      default: 0
    },
    editVisible: {
      type: Boolean,
      default: true
    },
    showSidebarToggle: {
      type: Boolean,
      default: false
    },
    toolbarSyncHeight: {
      type: Number,
      default: null
    }
  },
  data() {
    return {
      filterText: '',
      searchMode: false,
      searchTreeData: [],
      allFoldersCache: null,
      treeEpoch: 0,
      filterTimer: null,
      treeProps: {
        label: 'docName',
        children: 'children',
        isLeaf: 'leaf'
      }
    }
  },
  computed: {
    treeToolsBarStyle() {
      const h = this.toolbarSyncHeight
      if (h == null || !(h > 0)) {
        return {}
      }
      return {
        height: `${h}px`,
        minHeight: `${h}px`,
        lineHeight: 'normal',
        boxSizing: 'border-box'
      }
    }
  },
  watch: {
    currentDocId() {
      this.$nextTick(() => this.syncCurrentHighlight())
    },
    projectId() {
      this.reload()
    }
  },
  beforeUnmount() {
    if (this.filterTimer) {
      clearTimeout(this.filterTimer)
    }
  },
  methods: {
    handleSidebarToggle() {
      this.$emit('toggle-sidebar')
    },
    nodeLabel(data) {
      if (!data || data.docId === ROOT_ID || data.docId === -1) {
        return this.$t('doc.root-folder')
      }
      return data.docName || ''
    },
    rootNode() {
      return {
        docId: ROOT_ID,
        docPid: 0,
        docName: this.$t('doc.root-folder'),
        docType: 0,
        leaf: false
      }
    },
    loadNode(node, resolve) {
      if (node.level === 0) {
        resolve([this.rootNode()])
        // 懒加载下 default-expanded-keys 常不生效，根节点出现后主动展开
        this.$nextTick(() => this.expandRootNode())
        return
      }
      const parentId = node.data && node.data.docId != null ? node.data.docId : ROOT_ID
      const pid = parentId > 0 ? parentId : 0
      listDocument({
        pageNum: 1,
        pageSize: 9999,
        docType: 0,
        projectId: this.projectId,
        docPid: pid
      }).then(response => {
        const rows = (response && response.rows) || []
        resolve(rows.map(r => ({
          ...r,
          leaf: !!r.leaf
        })))
        this.$nextTick(() => this.syncCurrentHighlight())
      }).catch(() => resolve([]))
    },
    expandRootNode() {
      const tree = this.$refs.folderTree
      if (!tree || typeof tree.getNode !== 'function') return
      const rootNode = tree.getNode(ROOT_ID)
      if (!rootNode) return
      rootNode.data.leaf = false
      rootNode.isLeaf = false
      if (!rootNode.expanded && typeof rootNode.expand === 'function') {
        rootNode.expand()
      }
      this.syncCurrentHighlight()
    },
    handleNodeClick(data) {
      if (!data) return
      const folder = data.docId === ROOT_ID || data.docId === -1
        ? null
        : {
            docId: data.docId,
            docPid: data.docPid != null ? data.docPid : 0,
            docName: data.docName || '',
            docType: 0
          }
      this.$emit('node-click', folder)
    },
    append(e, data) {
      const parent = !data || data.docId === ROOT_ID || data.docId === -1
        ? { docId: 0, docPid: 0, docName: '', docType: 0 }
        : {
            docId: data.docId,
            docPid: data.docPid != null ? data.docPid : 0,
            docName: data.docName || '',
            docType: 0
          }
      this.$emit('create-folder', parent)
      if (e && e.stopPropagation) e.stopPropagation()
    },
    onFilterInput() {
      if (this.filterTimer) clearTimeout(this.filterTimer)
      this.filterTimer = setTimeout(() => this.applyFilter(), 280)
    },
    applyFilter() {
      const kw = (this.filterText || '').trim()
      if (!kw) {
        this.searchMode = false
        this.searchTreeData = []
        this.treeEpoch += 1
        this.$nextTick(() => this.syncCurrentHighlight())
        return
      }
      this.loadAllFolders().then(list => {
        const lower = kw.toLowerCase()
        const matched = list.filter(f => (f.docName || '').toLowerCase().includes(lower))
        const keepIds = new Set(matched.map(f => f.docId))
        // 保留命中节点的祖先，便于树路径可读
        const byId = new Map(list.map(f => [f.docId, f]))
        matched.forEach(f => {
          let pid = f.docPid
          while (pid && pid > 0 && byId.has(pid) && !keepIds.has(pid)) {
            keepIds.add(pid)
            pid = byId.get(pid).docPid
          }
        })
        const subset = list.filter(f => keepIds.has(f.docId))
        this.searchTreeData = [{
          ...this.rootNode(),
          children: this.buildTree(subset),
          leaf: subset.length === 0
        }]
        this.searchMode = true
        this.treeEpoch += 1
        this.$nextTick(() => this.syncCurrentHighlight())
      })
    },
    loadAllFolders() {
      if (this.allFoldersCache) {
        return Promise.resolve(this.allFoldersCache)
      }
      // 不传 docPid → 全项目文件夹
      return listDocument({
        pageNum: 1,
        pageSize: 9999,
        docType: 0,
        projectId: this.projectId
      }).then(response => {
        this.allFoldersCache = (response && response.rows) || []
        return this.allFoldersCache
      })
    },
    buildTree(flatList) {
      const map = new Map()
      flatList.forEach(f => {
        map.set(f.docId, { ...f, children: [], leaf: true })
      })
      const roots = []
      map.forEach(node => {
        const pid = node.docPid
        if (pid && pid > 0 && map.has(pid)) {
          const parent = map.get(pid)
          parent.children.push(node)
          parent.leaf = false
        } else {
          roots.push(node)
        }
      })
      return roots
    },
    syncCurrentHighlight() {
      const tree = this.$refs.folderTree
      if (!tree || typeof tree.setCurrentKey !== 'function') return
      const key = this.currentDocId != null && this.currentDocId !== '' ? Number(this.currentDocId) : ROOT_ID
      const id = Number.isFinite(key) && key > 0 ? key : ROOT_ID
      try {
        tree.setCurrentKey(id)
      } catch (e) {
        /* 节点尚未加载时忽略 */
      }
    },
    /** 展开并高亮到指定文件夹（列表导航后调用） */
    ensureFolderVisible(folder) {
      this.$nextTick(() => {
        this.syncCurrentHighlight()
        if (this.searchMode || !folder || !folder.docId) return
        const tree = this.$refs.folderTree
        if (!tree) return
        // 懒加载树：逐级展开路径需已知祖链；此处尽力 setCurrentKey，父级已展开时可高亮
        try {
          tree.setCurrentKey(folder.docId)
        } catch (e) { /* ignore */ }
      })
    },
    reload() {
      this.allFoldersCache = null
      this.filterText = ''
      this.searchMode = false
      this.searchTreeData = []
      this.treeEpoch += 1
      this.$nextTick(() => this.syncCurrentHighlight())
    },
    clearFolderCache() {
      this.allFoldersCache = null
    },
    /** 搜索中：重跑过滤；懒加载树：勿整树 remount（会收起根） */
    invalidateAndRefresh() {
      this.allFoldersCache = null
      if (this.searchMode && (this.filterText || '').trim()) {
        this.applyFilter()
      }
      // 懒加载模式由 addFolderNode / removeFolderNodes / reloadChildren 局部更新
    },
    parentKey(docPid) {
      const pid = docPid != null ? Number(docPid) : 0
      return Number.isFinite(pid) && pid > 0 ? pid : ROOT_ID
    },
    /** 新建文件夹：挂到父节点下并展开，不 remount */
    addFolderNode(folder) {
      this.clearFolderCache()
      if (!folder || !folder.docId) return
      if (this.searchMode && (this.filterText || '').trim()) {
        this.applyFilter()
        return
      }
      const tree = this.$refs.folderTree
      if (!tree) return
      const pKey = this.parentKey(folder.docPid)
      let parentNode = tree.getNode(pKey)
      if (!parentNode) {
        return
      }
      const child = {
        docId: folder.docId,
        docPid: folder.docPid != null ? folder.docPid : 0,
        docName: folder.docName || '',
        docType: 0,
        leaf: true
      }
      if (parentNode.loaded) {
        if (typeof tree.append === 'function') {
          tree.append(child, parentNode)
        }
        parentNode.data.leaf = false
        parentNode.isLeaf = false
        if (!parentNode.expanded && typeof parentNode.expand === 'function') {
          parentNode.expand()
        }
      } else {
        parentNode.data.leaf = false
        parentNode.isLeaf = false
        if (typeof parentNode.expand === 'function') {
          parentNode.expand()
        }
      }
      this.$nextTick(() => this.syncCurrentHighlight())
    },
    /** 删除文件夹节点（含子树由后端级联，前端只删树上对应节点） */
    removeFolderNodes(docIds) {
      this.clearFolderCache()
      const ids = (Array.isArray(docIds) ? docIds : [docIds]).filter(id => id != null && id > 0)
      if (!ids.length) return
      if (this.searchMode && (this.filterText || '').trim()) {
        this.applyFilter()
        return
      }
      const tree = this.$refs.folderTree
      if (!tree) return
      ids.forEach(id => {
        const node = tree.getNode(id)
        if (!node) return
        const parent = node.parent
        if (typeof tree.remove === 'function') {
          tree.remove(node)
        }
        if (parent && parent.data) {
          const empty = !(parent.childNodes && parent.childNodes.length)
          // 根节点始终可展开
          if (parent.data.docId !== ROOT_ID) {
            parent.data.leaf = empty
            parent.isLeaf = empty
          }
        }
      })
      this.$nextTick(() => this.syncCurrentHighlight())
    },
    /** 重命名已加载的文件夹节点 */
    renameFolderNode(folder) {
      this.clearFolderCache()
      if (!folder || !folder.docId) return
      if (this.searchMode && (this.filterText || '').trim()) {
        this.applyFilter()
        return
      }
      const tree = this.$refs.folderTree
      if (!tree) return
      const node = tree.getNode(folder.docId)
      if (node && node.data) {
        node.data.docName = folder.docName || node.data.docName
      }
    },
    /**
     * 重载某个父夹的子节点（移动后用）；保持其它分支展开状态
     * @param {number|number[]} parentIds docPid，0 表示根
     */
    reloadChildren(parentIds) {
      this.clearFolderCache()
      if (this.searchMode && (this.filterText || '').trim()) {
        this.applyFilter()
        return
      }
      const tree = this.$refs.folderTree
      if (!tree) return
      const keys = (Array.isArray(parentIds) ? parentIds : [parentIds]).map(p => this.parentKey(p))
      const uniq = [...new Set(keys)]
      uniq.forEach(pKey => {
        const parentNode = tree.getNode(pKey)
        if (!parentNode) return
        const pid = pKey > 0 ? pKey : 0
        listDocument({
          pageNum: 1,
          pageSize: 9999,
          docType: 0,
          projectId: this.projectId,
          docPid: pid
        }).then(response => {
          const rows = (response && response.rows) || []
          const children = [...(parentNode.childNodes || [])]
          children.forEach(c => {
            if (typeof tree.remove === 'function') tree.remove(c)
          })
          rows.forEach(r => {
            if (typeof tree.append === 'function') {
              tree.append({ ...r, leaf: !!r.leaf }, parentNode)
            }
          })
          if (pKey !== ROOT_ID) {
            parentNode.data.leaf = rows.length === 0
            parentNode.isLeaf = rows.length === 0
          } else {
            parentNode.data.leaf = false
            parentNode.isLeaf = false
          }
          parentNode.loaded = true
          if (!parentNode.expanded && typeof parentNode.expand === 'function') {
            parentNode.expand()
          }
          this.$nextTick(() => this.syncCurrentHighlight())
        }).catch(() => {})
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.document-folder-tree {
  height: 100%;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
.tree-tools {
  flex-shrink: 0;
  width: 100%;
  height: 48px;
  line-height: 48px;
  border-bottom: 1px solid var(--border-color-light);
  background-color: var(--table-header-bg);
  padding: 0 10px;
  font-size: 13px;
  font-weight: 500;
  color: var(--table-header-color);
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
}
.tree-tools-title {
  display: inline-flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  gap: 8px;
}
.tree-search {
  flex-shrink: 0;
  padding: 8px 10px;
  border-bottom: 1px solid var(--border-color-lighter, var(--border-color-light));
}
.folder-tree {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  width: 100%;
  padding: 4px 0;
}
.tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 6px;
  min-width: 0;
}
.tree-node-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tree-node-icon {
  flex-shrink: 0;
  width: 14px;
  height: 14px;
}
.tree-node-add {
  flex-shrink: 0;
  padding: 0 4px !important;
}
</style>
