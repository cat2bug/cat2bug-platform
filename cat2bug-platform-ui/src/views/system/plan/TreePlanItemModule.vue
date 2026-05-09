<template>
  <div class="tree">
    <div class="tree-tools" :style="planTreeHeaderSyncStyle">
      <div v-if="showSidebarToggle" class="tree-plan-item-sidebar-toggle-wrap">
        <el-tooltip :content="$t('case.hide-module-tree')" placement="bottom">
          <span
            class="tree-sidebar-collapse-toggle"
            role="button"
            tabindex="0"
            @click.stop="handleSidebarToggle"
            @keyup.enter.stop.prevent="handleSidebarToggle"
          >
            <i class="el-icon-d-arrow-left" />
          </span>
        </el-tooltip>
      </div>
      <el-tabs v-model="activeName" stretch class="tree-plan-item-tabs" @tab-click="handleTabClick">
        <el-tab-pane :label="$t('module.list')" name="module">
          <el-tree v-loading="loading" :highlight-current="true" ref="moduleTree" :show-checkbox="checkVisible" :props="props" :lazy="true" :data="tree" :load="loadNode" node-key="(id,pid)" @node-click="handleNodeClick" @check-change="handleCheckChange">
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
                  v-show="removeNodeButtonVisible(data)"
                  @click="remove($event, node, data)">
                  <i class="el-icon-close" />
                </el-button>
              </span>
            </span>
          </el-tree>
        </el-tab-pane>
        <el-tab-pane :label="$t('defect.level-list')" name="defect-level">
          <el-tree v-loading="loading" :highlight-current="true" ref="levelTree" :show-checkbox="checkVisible" :props="props" :data="defectLevelTree" node-key="id" @node-click="handleLevelNodeClick" @check-change="handleLevelCheckChange">
          </el-tree>
        </el-tab-pane>
      </el-tabs>
    </div>

    <module-dialog ref="moduleDialog" :project-id="projectId" @added="batchAddNode" @updated="batchAddNode" />
  </div>
</template>

<script>
import {delModule} from "@/api/system/module";
import {listPlanItemModule} from "@/api/system/PlanItem";
import {checkPermi} from "@/utils/permission";
import ModuleDialog from "@/components/Module/ModuleDialog";
import {getLevelName, MAX_LEVEL_INDEX} from "@/utils/case";
export default {
  name: "TreePlanItemModule",
  components: {ModuleDialog},
  data(){
    return {
      loading: false,
      activeName: 'module',
      firstLoad: true,
      currentNode: null,
      tree:[],
      defectLevelTree: [{
        label: `<< ${this.$i18n.t('defect.all-level')} >>`,
        isLeaf: true
      }],
      props: {
        label: 'label',
        children: 'children',
        isLeaf: 'leaf'
      },
      menuItemStatisticType: this.statisticType
    }
  },
  watch: {
    "$i18n.locale": function (newVal, oldVal) {
      this.defectLevelTree = [{
        label: `<< ${this.$i18n.t('defect.all-level')} >>`,
        isLeaf: true
      }];
      this.initLevelTree();
    },
    statisticType: function (n,o) {
      if(n!=o) {
        this.menuItemStatisticType = n;
        this.reloadData();
      }
    }
  },
  props: {
    /** 统计类型:case、defect */
    statisticType: {
      type: String,
      default: 'case'
    },
    projectId: {
      type: [Number,String],
      default: null
    },
    planId: {
      type: [Number,String],
      default: null
    },
    /** 是否显示编辑组件 */
    editVisible: {
      type: Boolean,
      default: true
    },
    /** 是否显示多选框 */
    checkVisible: {
      type: Boolean,
      default: false
    },
    /** 是否显示全部交付物 */
    showAll: {
      type: Boolean,
      default: false
    },
    /** 是否显示交付物中包含的用例数量 */
    showCount: {
      type: Boolean,
      default: true
    },
    /** 标题栏右侧：收起左侧栏（与 TreeModule 一致，由父级 multipane 隐藏树列） */
    showSidebarToggle: {
      type: Boolean,
      default: false
    },
    /** 与右侧表格表头行同高（px），用于 Tab 标题行与收起按钮行对齐 */
    toolbarSyncHeight: {
      type: Number,
      default: null
    }
  },
  computed: {
    /** 与 ruoyi.scss 表头 th 默认 48px 对齐；测量到位后用右侧 thead tr 实际高度 */
    planTreeHeaderSyncStyle() {
      const h = this.toolbarSyncHeight;
      const px = h != null && h > 0 ? `${h}px` : '48px';
      return { '--plan-tree-tabs-sync-height': px };
    },
    removeNodeButtonVisible: function () {
      return function (node) {
        return node.leaf && checkPermi(['system:module:remove']) && node.label!=this.$i18n.t('module.all-module');
      }
    },
  },
  created() {
    this.initLevelTree();
  },
  methods: {
    initLevelTree() {
      for(let i = 1; i<=MAX_LEVEL_INDEX;i++) {
        this.defectLevelTree.push({
          label:  getLevelName(i),
          id: i,
          isLeaf: true
        })
      }
    },
    handleTabClick(tab, event) {
      // console.log(tab, event);
    },
    handleSidebarToggle() {
      this.$emit('toggle-sidebar');
    },
    reloadData() {
      this.currentNode = null;
      this.tree = [];
      this.getModuleList(0);
    },
    loadNode(node, resolve) {
      let modulePid = 0;
      if(node.level > 0) {
        modulePid = node.data.id;
      }
      this.getModuleList(modulePid,resolve);
    },
    /** 获取模块下的用例数量 */
    getItemCount(modules,index) {
      const module = modules[index];
      let count = 0;
      switch (this.menuItemStatisticType) {
        case 'case':
          count = module.itemCount||0;
          break;
        case 'defect':
          count = module.defectCount||0;
          break;
        default:
          count = module.itemCount||0;
          break;
      }

      if(module.childrenCount===0)
        return count;
      for(let i; i<modules.length;i++) {
        if(modules[i].modulePid = module.moduleId) {
          count += this.getItemCount(modules, i);
        }
      }
      return count;
    },
    /** 获取模块 */
    getModuleList(modulePid,resolve) {
      let params = {
        projectId:this.projectId,
        planId: this.planId,
        modulePid:modulePid
      }
      this.loading = true;
      listPlanItemModule(params).then(res=>{
        this.firstLoad = false;
        this.loading = false;
        let data = res.data.map((m,index)=>{
          const count = this.getItemCount(res.data, index);
          return {
            id: m.moduleId,
            pid: m.modulePid,
            label: m.moduleName + (this.showCount?`(${count})`:''),
            count: count,
            leaf: m.childrenCount===0
          }
        })
        data = this.showAll?data:data.filter(m=>m.count);
        if(modulePid==0){
          data = [...[{
            label: this.$i18n.t('module.all-module'),
            leaf: true
          }],...data];
        }
        if(resolve){
          resolve(data);
        } else {
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
    handleLevelCheckChange(data, checked, indeterminate) {
      this.$emit('level-check-change', data, checked, indeterminate);
    },
    handleLevelNodeClick(node){
      this.currentNode = node;
      this.$emit('level-node-click',node.id);
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
            label: module.moduleName,
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
/* 与缺陷页 defect-tree-module + TreeModule 一致：侧栏纵向撑满、树区可滚动 */
.tree {
  height: 100%;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
.tree-tools {
  position: relative;
  width: 100%;
  flex: 1 1 0%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-bottom: none;
  background-color: transparent;
  padding: 0;
  margin-bottom: 0;
  font-size: 13px;
  font-weight: 500;
  color: #515a6e;
  box-sizing: border-box;
  /* 外层左右 5px：父级 AddPlanDialog / HandlePlanDialog .custom-resizer；树/优先级列表在 Tab 内容区内再留 5px，避免贴边 */
  ::v-deep .el-tabs {
    display: flex;
    flex-direction: column;
    flex: 1 1 0%;
    min-height: 0;
  }
  ::v-deep .el-tabs__header {
    flex-shrink: 0;
    margin: 0;
    padding: 0;
    box-sizing: border-box;
    background-color: #f8f8f9;
  }
  /* 与右侧 el-table 表头同色同高：固定 height（不只 min-height），避免 Tab 默认行高低于 th */
  ::v-deep .tree-plan-item-tabs > .el-tabs__header {
    padding-right: 28px;
    box-sizing: border-box;
    height: var(--plan-tree-tabs-sync-height, 48px);
    min-height: var(--plan-tree-tabs-sync-height, 48px);
    display: flex;
    align-items: stretch;
    border-bottom: 1px solid #dfe6ec;
  }
  ::v-deep .tree-plan-item-tabs > .el-tabs__header .el-tabs__nav-wrap {
    flex: 1;
    min-height: 100%;
    display: flex;
    align-items: stretch;
    box-sizing: border-box;
    /* Element 默认 ::after 高度 2px，与下方 header 的 1px 底边叠加会「一段粗一段细」 */
    margin-bottom: 0 !important;
  }
  ::v-deep .tree-plan-item-tabs > .el-tabs__header .el-tabs__nav-wrap::after {
    display: none !important;
  }
  ::v-deep .el-tabs__nav-wrap {
    padding-left: 5px;
    padding-right: 5px;
    box-sizing: border-box;
  }
  /*
   * Element 默认 TabBar 用 JS 算宽度/位移，在 stretch + 侧栏拖拽变宽时经常错位。
   * 隐藏 TabBar，用每个 tab 格子的 border-bottom 作为选中指示线，宽度随 flex 自动正确。
   */
  ::v-deep .el-tabs__active-bar {
    display: none !important;
  }
  /* stretch 下 nav 已 flex；item 默认 inline-block 会让文案视觉上偏左，改为 flex 居中 */
  ::v-deep .tree-plan-item-tabs .el-tabs__nav {
    width: 100%;
    float: none;
  }
  /* Tab 格纵向铺满标题栏高度，文案 flex 居中（与表头 th 内 .cell 观感一致） */
  ::v-deep .tree-plan-item-tabs .el-tabs__item {
    display: flex !important;
    align-items: center;
    justify-content: center;
    align-self: stretch;
    text-align: center;
    line-height: 1.25;
    padding-left: 8px;
    padding-right: 8px;
    border-bottom: none !important;
    box-sizing: border-box;
  }
  ::v-deep .tree-plan-item-tabs .el-tabs__item.is-active {
    color: #1890ff;
    position: relative;
  }
  ::v-deep .tree-plan-item-tabs .el-tabs__item.is-active::after {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 1px;
    background-color: #1890ff;
    z-index: 2;
  }
  ::v-deep .el-tabs__content {
    flex: 1 1 0%;
    min-height: 0;
    overflow: hidden;
    padding: 10px 5px 0;
    box-sizing: border-box;
    background-color: #fff;
  }
  ::v-deep .el-tab-pane {
    height: 100%;
  }
}
.tree-plan-item-sidebar-toggle-wrap {
  position: absolute;
  top: 0;
  right: 5px;
  z-index: 4;
  height: var(--plan-tree-tabs-sync-height, 48px);
  display: flex;
  align-items: center;
}
.tree-plan-item-sidebar-toggle-wrap .tree-sidebar-collapse-toggle {
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
.tree-plan-item-sidebar-toggle-wrap .tree-sidebar-collapse-toggle:hover {
  color: #409eff;
  background-color: #ecf5ff;
}
.tree-plan-item-sidebar-toggle-wrap .tree-sidebar-collapse-toggle:focus-visible {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.35);
}
.tree-plan-item-sidebar-toggle-wrap .tree-sidebar-collapse-toggle > i {
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
.active {
  background-color: #00afff;
}
/* 可横/纵滚动，滚动条隐藏（触控板、Shift+滚轮、拖拽仍可滚） */
.el-tree {
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  overflow-x: auto;
  width: 100%;
  box-sizing: border-box;
  scrollbar-width: none;
  -ms-overflow-style: none;
  &::-webkit-scrollbar {
    width: 0;
    height: 0;
  }
  ::v-deep > div[role="treeitem"]:first-child{
    .el-checkbox {
      display: none;
    }
  }
}
</style>
