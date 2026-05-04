<template>
  <el-row class="module-menu">
    <el-col v-show="loading" class="module-menu-item" :span="24">
      <div v-loading="loading" style="width: 100%;height: 200px;"></div>
    </el-col>
    <el-col class="module-menu-item" :span="24" v-for="(module,index) in moduleList" :key="module.moduleId"
      @click.native="showSubMenuHandle(module)"
      @mouseenter.native="mouseEnterHandle(index)"
      @mouseleave.native="mouseLeaveHandle(index)">
      <span>{{module.moduleName}}</span>
      <i v-if="moduleHasChildren(module)" class="el-icon-arrow-right"></i>
      <el-button v-else-if="isEdit" type="text" size="mini" v-show="addButtonVisibleList[index]" @click="addSubMenuHandle($event, module)"><i class="el-icon-plus"></i></el-button>
    </el-col>
    <el-col :span="24" v-if="isEdit" class="module-menu-add-wrap">
      <add-module-menu-item ref="addModuleMenu" v-model="moduleName" :project-id="projectId" :module-pid="modulePid" @added="addModuleHandle" />
    </el-col>
  </el-row>
</template>

<script>
import AddModuleMenuItem from "@/components/Module/SelectModule/add";
import {listModule} from "@/api/system/module";

export default {
  name: "ModuleMenu",
  components: { AddModuleMenuItem },
  data() {
    return {
      loading: false,
      moduleName: null,
      moduleList: [],
      params: {
        pageIndex: 1,
          pageSize: 9999,
          modulePid: 0,
          projectId: null
      },
      addButtonVisibleList: [],
    }
  },
  props: {
    projectId: {
      type: Number,
      default: null
    },
    modulePid: {
      type: Number,
      default: 0
    },
    isEdit: {
      type: Boolean,
      default: true
    }
  },
  computed: {
    getCurrentProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
  },
  created() {
    this.getModuleList();
  },
  methods: {
    getModuleList() {
      this.params.projectId=this.projectId||this.getCurrentProjectId;
      this.params.modulePid=this.modulePid;
      this.loading = true;
      listModule(this.params).then(res=>{
        this.loading = false;
        this.moduleList = res.data;
        let visibleList = [];
        res.data.forEach(m=>{
          visibleList.push(false);
        })
        this.addButtonVisibleList = visibleList;
        this.$refs.addModuleMenu.setFormVisible(this.moduleList.length==0);
      }).catch(e=>{
        this.loading = false;
      })
    },
    addModuleHandle(modules) {
      this.getModuleList();
      this.$forceUpdate();
    },
    open(projectId,modulePid) {
      if(projectId){
        this.params.projectId=projectId;
      }
      if(modulePid){
        this.params.modulePid=modulePid;
      }
      this.getModuleList();
    },
    moduleHasChildren (module) {
      const c = module && module.childrenCount
      return c != null && Number(c) > 0
    },
    showSubMenuHandle(module) {
      if (this.moduleHasChildren(module)) {
        this.$emit('clickDirectory', module);
      } else {
        this.$emit('clickMenu', module);
      }
    },
    addSubMenuHandle(event, module) {
      this.$emit('clickAddSubMenu', module);
      event.stopPropagation();
    },
    mouseEnterHandle(index) {
      this.addButtonVisibleList[index]=true;
      this.$forceUpdate();
    },
    mouseLeaveHandle(index) {
      this.addButtonVisibleList[index]=false;
      this.$forceUpdate();
    }
  }
}
</script>

<style lang="scss" scoped>
  .module-menu {
    width: 200px;
  }
  .module-menu-add-wrap {
    margin-top: 0;
    padding-top: 6px;
  }
  .module-menu-item {
    padding: 5px 20px;
    min-height: 38px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .module-menu-item:hover {
    background-color: #F2F6FC;
    border-radius: 5px;
    cursor: pointer;
  }
</style>
