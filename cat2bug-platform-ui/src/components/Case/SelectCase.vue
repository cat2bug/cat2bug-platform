<template>
  <el-popover
    placement="bottom-start"
    trigger="click"
    @show="popoverShowHandle"
    @hide="popoverHideHandle"
    popper-class="cat2bug-select-case"
    v-model="popoverVisible"
    ref="popover"
  >
    <div slot="reference" class="el-input__inner select-case-input" @mouseenter="showClearButtonHandle(true)" @mouseleave="showClearButtonHandle(false)">
      <i :class="icon" v-if="icon" style="margin: 0px 0px 0px 10px; color: #C0C4CC;"></i>
      <case-card ref="caseCard" class="select-case-input_content" :case-model="currentCase" :state-visible="true" :edit="true" v-bind="$attrs" @change="stepChangeHandle" />
      <i class="select-case-input__icon el-icon-arrow-up" v-show="isClearButtonVisible==false"></i>
      <i class="select-case-input__icon el-icon-circle-close" v-show="isClearButtonVisible==true" @click="clearSelectModuleHandle"></i>
    </div>
    <div class="case-list">
      <div class="case-list-item" v-for="(c,index) in caseList" :key="index" @click="clickCaseItemHandle(c)">
        <case-card :case-model="c" />
      </div>
    </div>
  </el-popover>
</template>

<script>
import {listCase} from "@/api/system/case";
import CaseCard from "@/components/Case/CaseCard";
import Label from "@/components/Cat2BugStatistic/Components/Label";
export default {
  name: "SelectCase",
  components: {Label, CaseCard},
  model: {
    prop: 'caseId',
    event: 'change'
  },
  data() {
    return {
      popoverVisible:false,
      currentCase: {},
      currentCaseId: null,
      caseList: [],
      // 是否显示清除按钮
      isClearButtonVisible: false,
    }
  },
  props: {
    caseId: {
      type: Number,
      default: null
    },
    moduleId: {
      type: Number,
      default: null
    },
    clearable: {
      type: Boolean,
      default: true
    },
    icon: {
      type: String,
      default: null
    },
  },
  watch: {
    moduleId: function (n,o) {
      if(n && n!=o) {
        this.search(n);
      }
    },
    caseId: function (n,o) {
      if(n && this.caseList && this.caseList.length>0 && n!=o) {
        this.refreshCaseId();
      }
    },
  },
  methods: {
    refresh(moduleId) {
      this.moduleId = moduleId;
      this.search(this.moduleId);
    },
    reset() {
      this.caseList=[];
    },
    refreshCaseId() {
      for(let i=0;i<this.caseList.length;i++){
        let c=this.caseList[i];
        if(this.caseId==c.caseId) {
          this.setCaseId(c);
          // this.$refs.caseCard.refresh()
          return;
        }
      }
      // 如果新获取的用例没有之前选择的，就设置空用例
      this.setCaseId({});
    },
    search(moduleId) {
      this.reset();
      if(!moduleId){
        return;
      }
      let query = {
        params: {
          pageSize: 999,
          pageNum: 1,
          modulePid: moduleId
        }
      }
      listCase(query).then(res=>{
        this.caseList = res.rows;
        this.refreshCaseId();
      })
    },
    setCaseId(val) {
      this.currentCase = val;
      this.$emit('change',val.caseId);
    },
    /** 显示或隐藏清除按钮 */
    showClearButtonHandle(visible) {
      if(this.clearable==false) return;
      // if(visible && this.queryMember.params.search) {
      //   this.isClearButtonVisible = true;
      // } else {
      //   this.isClearButtonVisible = false;
      // }
    },
    /** 清除选择的成员 */
    clearSelectModuleHandle(event) {
      // this.queryMember.params.search=null;
      // this.currentCase=null;
      this.setCaseId({});
      this.popoverVisible = false;
      this.$forceUpdate();
      if(event) {
        this.$emit('input',null);
        // event.stopPropagation();
      }
    },
    /** 点击用例选项处理操作 */
    clickCaseItemHandle(c) {
      this.setCaseId(c);
      this.popoverVisible = false;
    },
    /** 弹窗显示事件 */
    popoverShowHandle() {
      this.$refs.popover.updatePopper()
      // this.$refs.selectProjectModuleInput.focus();
    },
    /** 弹窗隐藏事件 */
    popoverHideHandle() {
      // this.$refs.selectProjectModuleInput.blur();
    },
    stepChangeHandle(index) {
      this.$emit('step-change', index);
    }
  }
}
</script>
<style>
.cat2bug-select-case {
  max-width: calc(50% - 1px);
  min-width: 500px;
}
</style>
<style lang="scss" scoped>
  .select-case {
  }
  .select-case-input {
    display: inline-flex;
    flex-direction: row;
    justify-content: flex-start;
    //max-width: 300px;
    overflow:hidden;
    width: 100%;
    height: auto;
    align-items: center;
    padding: 10px 15px;
    .select-case-input_content {
      flex-wrap: wrap;
      flex-grow: 1;
      min-height: 30px;
      padding: 3px 10px 3px 0px;
      width: calc(100% - 100px);
    }
    .select-case-input__icon {
      display: inline;
      color: #C0C4CC;
      font-size: 14px;
      transition: transform 0.3s, -webkit-transform 0.3s;
      -webkit-transform: rotateZ(180deg);
      transform: rotateZ(180deg);
      cursor: pointer;
    }
  }
  .case-list {
    overflow-y: auto;
    width: 100%;
    max-height: 500px;
  }
  .case-list-item {
    padding: 10px 0px;
  }
  .case-list-item:hover {
    background-color: #F2F6FC;
    border-radius: 5px;
    cursor: pointer;
  }
</style>
