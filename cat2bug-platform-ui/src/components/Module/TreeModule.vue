<template>
  <div class="tree">
    <div class="tree-tools">
      <i class="el-icon-menu" />
      {{ $t('module.list') }}
    </div>
    <el-tree :highlight-current="true" ref="moduleTree" :props="props" :lazy="true" :data="tree" :load="loadNode" node-key="id" @node-click="handleNodeClick">
      <span class="tree-node" slot-scope="{ node, data }">
        <span>{{ node.label }}</span>
        <span>
          <el-button
            type="text"
            size="mini"
            @click="() => append(data)">
            <i class="el-icon-plus" />
          </el-button>
          <el-button
            type="text"
            size="mini"
            v-show="removeNodeButtonVisible(data)"
            @click="() => remove(node, data)">
            <i class="el-icon-close" />
          </el-button>
        </span>
      </span>
    </el-tree>
    <module-dialog ref="moduleDialog" :project-id="projectId" @added="refreshNode" @updated="refreshNode" />
  </div>
</template>

<script>
import {delModule, listModule} from "@/api/system/module";
import {checkPermi} from "@/utils/permission";
import ModuleDialog from "@/views/system/case/components/ModuleDialog";
export default {
  name: "TreeModule",
  components: {ModuleDialog},
  data(){
    return {
      currentNode: null,
      tree:[],
      props: {
        label: 'name',
        children: 'zones',
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
        return node.leaf && checkPermi(['system:module:remove']);
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
    append(data) {
      this.$refs.moduleDialog.open(null,data.id);
    },
    refreshNode(module) {
      let node = this.$refs.moduleTree.getNode({ id: module.modulePid })
      node.loaded = false;
      node.expand();
    },
    remove(node, data) {
      this.$modal.confirm(this.$i18n.t('module.is-delete-module')).then(function() {
        return delModule(data.id);
      }).then(() => {
        this.$modal.msgSuccess(this.$i18n.t('delete-success'));
        node.remove();
      }).catch(() => {});
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
</style>
