<template>
  <div>
    <draggable v-if="draggable" tag="div" v-model="list" @change="updateStatisticPanel" class="statistic-tools">
      <component
        @click.native="clickHandle($event,sc)"
        @tools-click="toolsHandle($event,sc)"
        :read="read"
        :tools="tools"
        :is="sc.name"
        :params="sc.params"
        :parent="parent"
        v-for="(sc,index) in list"
        :key="index"
      />
    </draggable>
    <div v-else class="statistic-tools">
      <component
        @click.native="clickHandle($event,sc)"
        @tools-click="toolsHandle($event,sc)"
        :read="read"
        :tools="tools"
        :is="sc.name"
        :params="sc.params"
        v-for="(sc,index) in list"
        :key="index"
      />
    </div>
  </div>
</template>

<script>
import Cat2ButTitle from "./Components/Title"
import draggable from 'vuedraggable'
import {addStatistic, listStatistic} from "@/api/system/statistic/template";

const path = require('path');
const files = require.context('./Statistic/', true, /\.vue$/);
const modules = {Cat2ButTitle,draggable};
const allStatisticList = [];
// 动态加载组件
files.keys().forEach(key=>{
  const name = path.basename(key,'.vue');
  allStatisticList.push({
    name: name,
  });
  modules[name] = files(key).default||files(key)
});

export default {
  name: "Cat2BugStatistic",
  model: {
    prop: 'statisticComponents',
    event: 'change'
  },
  components: modules,
  data() {
    return {
      activeNames: null,
      statisticList: [],
      // 统计工具
      statisticToolsList: [{
        icon: 'close'
      }],
    }
  },
  props: {
    statisticComponents: {
      type: Array,
      default: null
    },
    /** 显示类型，member=根据成员自己设置的模版显示；all=显示所有模版 */
    showType: {
      type: String,
      default: 'member'
    },
    statisticTools: {
      type: Array,
      default: null
    },
    draggable: {
      type: Boolean,
      default: false
    },
    params: {
      type: Object,
      default: ()=> {}
    },
    read: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    parent: function() {
      return this;
    },
    draggableList: {
      get(){
        return this.draggable?this.list:null;
      },
      set(val) {
        if(this.draggable)
          this.list = val;
      }
    },
    list: {
      get(){
        return this.statisticComponents || this.statisticList;
      },
      set(val) {
        if(this.statisticComponents) {
          this.$emit('change', val);
        }
        this.statisticList = val;
      }
    },
    tools: function () {
      return this.statisticTools || this.statisticToolsList;
    },
    /** 获取项目id */
    projectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    memberId() {
      return parseInt(this.$store.state.user.id);
    }
  },
  mounted() {
    switch (this.showType) {
      case 'member':
        this.getStatisticList();
        break;
      case 'all':
        this.getAllStatisticList();
        break;
    }
  },
  created() {
  },
  methods: {
    /** 更新统计面板 */
    updateStatisticPanel: function (){
      let params = {
        userId: this.memberId,
        projectId: this.projectId,
        moduleType: 1,
        statisticTemplatConfig: JSON.stringify(this.list)
      }
      addStatistic(params).then(res => {
        this.$message.success(this.$i18n.t('defect.statistic-save-success').toString());
      });
    },
    inputChanged(value) {
      this.activeNames = value;
    },
    getComponentData() {
      return {
        on: {
          change: this.updateStatisticPanel,
          input: this.inputChanged
        },
        attrs:{
          wrap: true
        },
        props: {
          value: this.activeNames
        }
      };
    },
    refresh() {
      this.getStatisticList();
    },
    search(params) {
      this.$parent.search && this.$parent.search(params);
    },
    /** 获取分析模块 */
    getStatisticList() {
      if(!this.statisticComponents) {
        let params = {
          userId: this.memberId,
          projectId: this.projectId,
          moduleType: 1
        }
        listStatistic(params).then(res=>{
          let statisticList=[];
          if(res.rows.length>0) {
            statisticList = JSON.parse(res.rows[0].statisticTemplatConfig);
          }
          this.statisticList = statisticList;
          this.$emit('change',statisticList);
        })
      }
    },
    getAllStatisticList() {
      this.statisticList = allStatisticList;
    },
    clickHandle(e,sc){
      e.stopPropagation();
      this.$emit('click-template-node',e,sc);
    },
    toolsHandle(tool,sc) {
      switch (tool.icon) {
        case 'close':
          this.removeStatistic(sc);
          break;
      }
    },
    removeStatistic(sc) {
      let list = this.list.filter(s=>s.name!=sc.name);
      let params = {
        userId: this.memberId,
        projectId: this.projectId,
        moduleType: 1,
        statisticTemplatConfig: JSON.stringify(list)
      }
      addStatistic(params).then(res=>{
        this.statisticList = list;
        this.$message.success(this.$i18n.t('delete.success').toString());
        this.$emit('change',list);
      });
    },
    mouseEnterHandle(e){
      e.stopPropagation();
    },
    mouseDownHandle(e) {
      e.stopPropagation();
    }
  }
}
</script>

<div  style="width:20px;position: absolute;right:5px;top:28px;border:1px solid green;display: flex;flex-direction: column;justify-content: center;">
<div :style="{width:'8px', height:'8px',borderRadius:'100%',backgroundColor:item.lamplightColour }" v-for="item in cabinetAreaLight" :key="item" style="margin:3px 0px;"/>
</div>

<style lang="scss" scoped>
  .statistic-tools {
    display: flex;
    justify-content: flex-start;
    align-content: center;
    flex-direction: row;
    flex-wrap: wrap;
    gap:15px;
    border-width: 0px;
    > * {
      margin-bottom: 10px;
    }
  }
</style>
