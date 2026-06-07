<template>
  <div class="tree">
    <div class="tree-tools" :style="treeToolsBarStyle">
      <span class="tree-tools-title">
        <i class="el-icon-menu" />
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
          <i class="el-icon-d-arrow-left" :class="sidebarToggleHintClass" />
        </span>
      </el-tooltip>
    </div>
    <el-tree :highlight-current="true" ref="moduleTree" :show-checkbox="checkVisible" :props="props" :lazy="true" :data="tree" :load="loadNode" node-key="id" @node-click="handleNodeClick" @check-change="handleCheckChange">
      <span class="tree-node" slot-scope="{ node, data }">
        <span v-if="node.label!=$t('module.all-module')">{{ node.label }}</span>
        <span v-else><< {{ node.label }} >></span>
        <span v-if="editVisible">
          <el-button
            type="text"
            size="mini"
            v-hasPermi="['system:module:add']"
            @click="append($event, data)">
            <i class="el-icon-plus" />
          </el-button>
          <el-button
            type="text"
            size="mini"
            class="tree-node-remove-btn"
            v-show="removeNodeButtonVisible(data)"
            @click="remove($event, node, data)">
            <i class="el-icon-close" />
          </el-button>
        </span>
      </span>
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
      this.$refs.moduleDialog.open(null,data.id);
      e.stopPropagation();
    },
    /** 批量添加节点 */
    batchAddNode(modules) {
      if(!modules) return;
      if(modules instanceof Array) {
        let parentNode = this.$refs.moduleTree.getNode({ id: modules[0].modulePid });
        for(let i=0;i<modules.length;i++){
          let module = modules[i];
          this.$refs.moduleTree.append({
            id: module.moduleId,
            leaf: true,
            name: module.moduleName,
          }, parentNode);
        }
        if(parentNode) {
          this.refreshTreeNode(parentNode);
        }
      }
    },
    /** 刷新树节点 */
    refreshTreeNode(node) {
      if(!node) return;
      node.data.leaf=(node.childNodes==0);
      node.isLeft=node.data.leaf;
      node.loaded=true;
      node.expand()
      this.refreshTreeNode(node.parent)
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
.tree-sidebar-collapse-toggle {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  box-sizing: border-box;
  cursor: pointer;
  color: #909399;
  font-size: 12px;
  line-height: 1;
  border-radius: 4px;
  border: none;
  outline: none;
  background: transparent;
}
.tree-sidebar-collapse-toggle:hover {
  color: #409eff;
  background-color: #ecf5ff;
}
.tree-sidebar-collapse-toggle:focus-visible {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.35);
}
.tree-sidebar-collapse-toggle > i {
  font-size: 12px;
  transform: scale(0.92);
}
.tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
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
  overflow-y: hidden;
  overflow-x: auto;
  width: 100%;
  ::v-deep > div[role="treeitem"]:first-child{
    margin-top: 5px;
    .el-checkbox {
      display: none;
    }
  }
}
</style>
