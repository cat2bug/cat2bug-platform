<template>
  <div class="tree">
    <div class="tree-tools">
      <i class="el-icon-menu" />
      {{ $t('module.list') }}
    </div>
    <el-tree :highlight-current="true" ref="moduleTree" :props="props" :lazy="true" :data="tree" :load="loadNode" node-key="id" @node-click="handleNodeClick">
      <span class="tree-node" slot-scope="{ node, data }">
        <span v-if="node.label!=$t('module.all-module')">{{ node.label }}</span>
        <span v-else><< {{ node.label }} >></span>
        <span>
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
    }
  },
  computed: {
    removeNodeButtonVisible: function () {
      return function (node) {
        return node.leaf && checkPermi(['system:module:remove']) && node.name!=this.$i18n.t('module.all-module');
      }
    }
  },
  created() {
    this.getModuleList();
  },
  methods: {
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
        let data = res.data.map(m=>{
          return {
            id: m.moduleId,
            name: m.moduleName,
            leaf: m.childrenCount===0
          }
        });
        if(modulePid==0){
          data = [...[{
            name: this.$i18n.t('module.all-module'),
            leaf: true
          }],...data];
        }
        if(resolve){
          resolve(data);
        }
      }).catch(e=>{
        this.loading = false;
      })
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
}
</style>
