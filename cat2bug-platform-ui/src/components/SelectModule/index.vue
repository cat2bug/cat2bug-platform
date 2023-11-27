<template>
  <el-popover
    placement="bottom-start"
    v-model="popoverVisible"
    @show="popoverShowHandle"
    @hide="popoverHideHandle"
    trigger="click">
    <div slot="reference" class="select-module-input el-input__inner">
      <div class="selectProjectMemberInput_content">
        <el-input ref="selectProjectModuleInput" readonly :placeholder="placeholder" v-model="queryMember.params.search" @input="searchChangeHandle"></el-input>
      </div>
      <i class="select-module-input__icon el-icon-arrow-up" v-show="isClearButtonVisible==false" @mouseenter="showClearButtonHandle(true)"></i>
      <i class="select-module-input__icon el-icon-circle-close" v-show="isClearButtonVisible==true" @mouseleave="showClearButtonHandle(false)" @click="clearSelectModuleHandle"></i>
    </div>
    <div class="select-module-menu">
      <module-menu v-for="(moduleId, index) in activeModuleIds"
                   :key="moduleId"
                   :module-pid="moduleId"
                   :project-id="projectId"
                   @clickMenu="clickMenuHandle($event,index)"
                   @clickDirectory="clickDirectoryHandle($event,index)"
                   @clickAddSubMenu="clickAddSubMenuHandle($event,index)"
      ></module-menu>
    </div>
  </el-popover>
</template>

<script>
import i18n from "@/utils/i18n/i18n";
import {listMemberOfProject, listProjectRole} from "@/api/system/project";
import MemberNameplate from "@/components/MemberNameplate";
import ModuleMenu from "@/components/SelectModule/menu";

export default {
  name: "SelectModule",
  model: {
    prop: 'moduleId',
    event: 'input'
  },
  components: { MemberNameplate, ModuleMenu },
  data() {
    return {
      selectModule: null,
      activeModuleIds: [0],
      // 是否显示清除按钮
      isClearButtonVisible: false,
      // 是否显示成员下拉列表
      popoverVisible: false,
      // 成员总数
      total: 0,
      queryMember: {
        params: {}
      },
      addModule: null
    }
  },
  props: {
    moduleId: {
      type: Number,
      default: null
    },
    projectId: {
      type: Number,
      default: null
    },
    multiple: {
      type: Boolean,
      default: true
    },
    placeholder: {
      type: String,
      default: i18n.t('member.please-select-member')
    },
    clearable: {
      type: Boolean,
      default: true
    },
  },
  computed: {
  },
  created() {
  },
  methods: {
    /** 搜索成员事件 */
    searchChangeHandle() {
      this.$emit('input',this.selectModule.moduleId);
    },
    /** 弹窗显示事件 */
    popoverShowHandle() {
      this.$refs.selectProjectModuleInput.focus();
    },
    /** 弹窗隐藏事件 */
    popoverHideHandle() {
      this.$refs.selectProjectModuleInput.blur();
    },
    /** 点击目录事件的处理 */
    clickDirectoryHandle(module,index) {
      let arr = [];
      for(let i=0;i<=index;i++) {
        arr.push(this.activeModuleIds[i])
      }
      if(this.activeModuleIds.length-1==index) {
        arr.push(module.moduleId);
      }
      arr[index+1] = module.moduleId;
      this.activeModuleIds=arr;
      this.$forceUpdate();
    },
    /** 点击菜单事件的处理 */
    clickMenuHandle(module,index){
      this.selectModule = module;
      this.queryMember.params.search=this.selectModule.moduleName;
      this.popoverVisible=false;
      this.$emit('input',this.selectModule.moduleId);
      this.resetMenu();
    },
    /** 点击菜单添加按钮的事件处理 */
    clickAddSubMenuHandle(module,index) {
      this.clickDirectoryHandle(module,index);
    },
    /** 显示或隐藏清除按钮 */
    showClearButtonHandle(visible) {
      if(this.clearable==false) return;
      if(visible && this.queryMember.params.search) {
        this.isClearButtonVisible = true;
      } else {
        this.isClearButtonVisible = false;
      }
    },
    /** 清除选择的成员 */
    clearSelectModuleHandle(event) {
      this.queryMember.params.search=null;
      this.selectModule=null;
      this.popoverVisible = false;
      this.$forceUpdate();
      event.stopPropagation();
    },
    resetMenu() {
      this.activeModuleIds = [0];
      this.$forceUpdate();
    }
  }
}
</script>

<style lang="scss" scoped>
  ::v-deep .select-module-input {
    display: inline-flex;
    flex-direction: row;
    width: 300px;
    height: auto;
    line-height: 0;
    align-items: center;
    padding-left: 0px;
    .selectProjectMemberInput_content {
      display: inline-flex;
      flex-direction: row;
      align-items: center;
      flex-wrap: wrap;
      flex-grow: 1;
      overflow: hidden;
      min-height: 28px;
      margin: 3px 10px 3px 0px;
      .el-input {
        flex-grow: 1;
        width: 0.1%;
        height: 22px;
        .el-input__inner {
          border-width: 0px;
          height: 22px;
          line-height: 22px;
          display: inline;
        }
      }
    }
    .select-module-input__icon {
      display: inline;
      color: #C0C4CC;
      font-size: 14px;
      transition: transform 0.3s, -webkit-transform 0.3s;
      -webkit-transform: rotateZ(180deg);
      transform: rotateZ(180deg);
      cursor: pointer;
    }
  }
  .select-module-input:focus {
    .select-module-input__icon {
      transform: rotateZ(0deg);
    }
  }
  .select-module-menu {
    display: inline-flex;
    flex-direction: row;
    justify-content: start;
    .module-menu:first-child {
      padding-left: 0px;
      border-left-width: 0;
    }
    .module-menu:last-child {
      padding-right: 0px;
    }
    .module-menu {
      border-left: #f1f1f1 1px solid;
      padding-left: 10px;
      padding-right: 10px;
    }
  }
</style>
