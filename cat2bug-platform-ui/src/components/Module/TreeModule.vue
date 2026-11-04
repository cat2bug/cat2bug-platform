<template>
  <div class="tree">
    <div class="tree-tools" :style="treeToolsBarStyle">
      <span class="tree-tools-title">
        <legacy-icon icon="el-icon-menu" />
        {{ $t('module.list') }}
      </span>
      <el-tooltip v-if="showSidebarToggle" :content="$t('case.hide-module-tree')" placement="bottom">
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
    <el-tree :highlight-current="true" ref="moduleTree" :show-checkbox="checkVisible" :props="props" :lazy="true" :data="tree" :load="loadNode" node-key="id" @node-click="handleNodeClick" @check-change="handleCheckChange">
      <template #default="{ node, data }">
        <span class="tree-node">
          <span class="tree-node-label">
            <span v-if="node.label !== $t('module.all-module')">{{ node.label }}</span>
            <span v-else><< {{ node.label }} >></span>
          </span>
          <span v-if="editVisible" class="tree-node-actions">
            <el-button
              link
              size="small"
              v-hasPermi="['system:module:add']"
              @click.stop="append($event, data)">
              <legacy-icon icon="el-icon-plus" />
            </el-button>
            <el-button
              link
              size="small"
              class="tree-node-remove-btn"
              v-show="removeNodeButtonVisible(data)"
              @click.stop="remove($event, node, data)">
              <legacy-icon icon="el-icon-close" />
            </el-button>
          </span>
        </span>
      </template>
    </el-tree>
    <module-dialog ref="moduleDialog" :project-id="projectId" @added="batchAddNode" @updated="batchAddNode" />
  </div>
</template>

<script>
import {delModule, listModule} from "@/api/system/module";
import {checkPermi} from "@/utils/permission";
import ModuleDialog from "@/components/Module/ModuleDialog";
export default {
  name: "TreeModule",
  components: {ModuleDialog},
  data(){
    return {
      firstLoad: true,
      currentNode: null,
      tree:[],
      props: {
        label: 'name',
        children: 'children',
        isLeaf: 'leaf'
      },
    }
  },
  props: {
    projectId: {
      type: [Number,String],
      default: null
    },
    editVisible: {
      type: Boolean,
      default: true
    },
    checkVisible: {
      type: Boolean,
      default: false
    },
    /** 标题栏右侧：收起左侧栏按钮（用例页交付物展开时） */
    showSidebarToggle: {
      type: Boolean,
      default: false
    },
    /**
     * 与右侧表格表头行同高（px）。由父级测量传入；未传时沿用样式默认高度。
     */
    toolbarSyncHeight: {
      type: Number,
      default: null
    },
    /** 收起按钮额外 class（缺陷页 ⌘ 快捷键徽标锚点） */
    sidebarToggleHintClass: {
      type: String,
      default: ''
    }
  },
  computed: {
    treeToolsBarStyle() {
      const h = this.toolbarSyncHeight;
      if (h == null || !(h > 0)) {
        return {};
      }
      return {
        height: `${h}px`,
        minHeight: `${h}px`,
        lineHeight: "normal",
        boxSizing: "border-box",
      };
    },
    removeNodeButtonVisible: function () {
      return function (node) {
        return node.leaf && checkPermi(['system:module:remove']) && node.name!=this.$i18n.t('module.all-module');
      }
    }
  },
  methods: {
    handleSidebarToggle() {
      this.$emit('toggle-sidebar');
    },
    reloadData() {
      this.currentNode = null;
      this.tree = [];
      if(!this.firstLoad) {
        this.getModuleList(0);
      }
    },
    loadNode(node, resolve) {
      let modulePid = 0;
      if(node.level > 0) {
        modulePid = node.data.id;
      }
      this.getModuleList(modulePid,resolve);
    },
    /** 获取模块 */
    getModuleList(modulePid,resolve) {
      let params = {
        projectId:this.projectId,
        modulePid:modulePid
      }
      listModule(params).then(res=>{
        this.firstLoad = false;
        let data = res.data.map(m=>{
          return {
            id: m.moduleId,
            name: m.moduleName,
            leaf: m.childrenCount===0
          }
        });
        // 添加一个显示所有数据的节点
        if(modulePid==0){
          data = [...[{
            name: this.$i18n.t('module.all-module'),
            leaf: true
          }],...data];
        }
        if(resolve){
          resolve(data);
        } else {
          // 刷新节点
          let { nodesMap } = this.$refs.moduleTree.root.store;
          nodesMap = {};
          this.$refs.moduleTree.root.setData(data);
        }
      }).catch(e=>{
        this.loading = false;
      })
    },
    handleCheckChange(data, checked, indeterminate) {
      this.$emit('check-change', data, checked, indeterminate);
    },
    handleNodeClick(node){
      this.currentNode = node;
      this.$emit('node-click',node.id);
    },
    append(e, data) {
      const parentId = data && data.id != null ? data.id : 0;
      this.$refs.moduleDialog.open(null, parentId);
      e.stopPropagation();
    },
    /** 批量添加节点（兼容单条对象或数组；根级 modulePid=0 挂到树根）——局部插入，不 reload 整树 */
    batchAddNode(modules) {
      if (!modules) return;
      const list = Array.isArray(modules) ? modules : [modules];
      if (!list.length) return;
      const tree = this.$refs.moduleTree;
      if (!tree) return;
      const pid = list[0].modulePid;
      let parentNode = null;
      if (pid != null && pid !== '' && Number(pid) !== 0) {
        parentNode = tree.getNode(pid) || tree.getNode({ id: Number(pid) });
        /* 父节点尚未出现在树中：不挂到根，等用户展开路径时懒加载即可 */
        if (!parentNode) return;
        if (parentNode.data) parentNode.data.leaf = false;
        parentNode.isLeaf = false;
        /*
         * 父节点还没懒加载过：只露出 ›，不要 append + loaded=true，
         * 否则会当成「已加载且仅有新建那一项」，其余子节点再也出不来。
         */
        if (!parentNode.loaded) {
          return;
        }
      }
      for (let i = 0; i < list.length; i++) {
        const module = list[i];
        if (!module || module.moduleId == null) continue;
        const exist = tree.getNode(module.moduleId) || tree.getNode({ id: module.moduleId });
        if (exist) continue;
        tree.append({
          id: module.moduleId,
          leaf: true,
          name: module.moduleName,
        }, parentNode || undefined);
      }
      if (parentNode) {
        this.ensureTreeNodeNonLeafExpanded(parentNode);
      }
    },
    /** 父节点已有子节点时保持非叶子并展开，不改 loaded、不递归污染根节点 */
    ensureTreeNodeNonLeafExpanded(node) {
      if (!node || !node.data || node.level === 0) return;
      node.data.leaf = false;
      node.isLeaf = false;
      if (typeof node.expand === 'function') {
        node.expand();
      }
    },
    /** 删除后刷新：只修正本节点/父节点 leaf，避免 loaded=true 吞掉未加载的子级 */
    refreshTreeNode(node) {
      if (!node || !node.data || node.level === 0) return;
      const kids = node.childNodes || [];
      const isLeaf = kids.length === 0;
      node.data.leaf = isLeaf;
      node.isLeaf = isLeaf;
      if (!isLeaf && typeof node.expand === 'function') {
        node.expand();
      }
      const parent = node.parent;
      if (parent && parent.data && parent.level > 0) {
        const pkids = parent.childNodes || [];
        parent.data.leaf = pkids.length === 0;
        parent.isLeaf = pkids.length === 0;
      }
    },
    remove(e, node, data) {
      this.$modal.confirm(this.$i18n.t('module.is-delete-module')).then(function() {
        return delModule(data.id);
      }).then(() => {
        this.$modal.msgSuccess(this.$i18n.t('delete-success'));
        let parentNode = node.parent;
        node.remove();
        if(parentNode) {
          this.refreshTreeNode(parentNode);
        }
      }).catch(() => {});
      e.stopPropagation();
    },
  }
}
</script>

<style lang="scss" scoped>
.tree {
  height: 100%;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
/* 与全局 .el-table th 一致（ruoyi.scss：表头行 48px / font-size 13px），与含 padding 的表头视觉同高 */
.tree-tools {
  flex-shrink: 0;
  width: 100%;
  height: 48px;
  line-height: 48px;
  border-bottom: 1px solid var(--border-color-light);
  background-color: var(--table-header-bg);
  padding: 0px 10px;
  margin-bottom: 0;
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
  > i {
    margin-right: 8px;
    flex-shrink: 0;
  }
}
.tree-node {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 4px;
  font-size: 14px;
  padding-right: 8px;
  box-sizing: border-box;
}
.tree-node-label {
  flex: 1 1 auto;
  min-width: 0;
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
  line-height: 1.4;
  padding: 1px 0;
}
.tree-node-actions {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  margin-top: -1px;
}
.tree-node-remove-btn {
  color: #f56c6c !important;
  &:hover,
  &:focus {
    color: #f78989 !important;
  }
}
.active {
  background-color: #00afff;
}
.el-tree {
  overflow-y: auto;
  overflow-x: hidden;
  width: 100%;
  /* 长名称换行：覆盖 EP 默认固定行高 + nowrap */
  :deep(.el-tree-node__content) {
    height: auto !important;
    min-height: 26px;
    white-space: normal;
    align-items: flex-start;
    padding-top: 4px;
    padding-bottom: 4px;
  }
  :deep(.el-tree-node__expand-icon) {
    padding-top: 2px;
  }
  :deep(> div[role="treeitem"]:first-child){
    margin-top: 5px;
    .el-checkbox {
      display: none;
    }
  }
}
</style>
