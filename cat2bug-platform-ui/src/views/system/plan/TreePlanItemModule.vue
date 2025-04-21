<template>
  <div class="tree">
    <div class="tree-tools">
      <el-tabs v-model="activeName" @tab-click="handleTabClick">
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
    }
  },
  computed: {
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
.tree-tools {
  width: 100%;
  height: 55px;
  line-height: 55px;
  border-bottom: 1px solid #dfe6ec;
  background-color: #f8f8f9;
  padding: 0px 10px;
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 500;
  color: #515a6e;
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
.el-tree {
  overflow-y: hidden;
  overflow-x: auto;
  width: 100%;
  ::v-deep > div[role="treeitem"]:first-child{
    .el-checkbox {
      display: none;
    }
  }
}
</style>
