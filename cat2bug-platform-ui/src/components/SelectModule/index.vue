<template>
  <el-popover
    placement="bottom-start"
    v-model="popoverVisible"
    @show="popoverShowHandle"
    @hide="popoverHideHandle"
    popper-class="select-module-popover"
    trigger="click">
    <div slot="reference" :class="'el-input__inner select-module-input select-module-input-'+size" @mouseenter="showClearButtonHandle(true)" @mouseleave="showClearButtonHandle(false)">
      <i :class="icon" v-if="icon" style="margin: 0px 0px 0px 10px; color: #C0C4CC;"></i>
      <div class="selectProjectMemberInput_content">
        <el-input ref="selectProjectModuleInput" :class="icon?'padding-left-8':''" readonly :placeholder="placeholder" v-model="queryMember.params.search" @input="searchChangeHandle"></el-input>
      </div>
      <i class="select-module-input__icon el-icon-arrow-up" v-show="isClearButtonVisible==false"></i>
      <i class="select-module-input__icon el-icon-circle-close" v-show="isClearButtonVisible==true" @click="clearSelectModuleHandle"></i>
    </div>
    <div class="select-module-menu">
      <module-menu v-for="(moduleId, index) in activeModuleIds"
                   :key="moduleId"
                   :module-pid="moduleId"
                   :project-id="projectId"
                   :is-edit="isEdit && checkPermi(['system:module:add'])"
                   @clickMenu="clickMenuHandle($event,index)"
                   @clickDirectory="clickDirectoryHandle($event,index)"
                   @clickAddSubMenu="clickAddSubMenuHandle($event,index)"
      ></module-menu>
    </div>
  </el-popover>
</template>

<script>
import i18n from "@/utils/i18n/i18n";
import { checkPermi } from "@/utils/permission";
import MemberNameplate from "@/components/MemberNameplate";
import ModuleMenu from "@/components/SelectModule/menu";
import {getModule} from "@/api/system/module";

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
      default: i18n.t('module.select-module-name')
    },
    clearable: {
      type: Boolean,
      default: true
    },
    isEdit: {
      type: Boolean,
      default: true
    },
    size: {
      type: String,
      default: 'default'
    },
    icon: {
      type: String,
      default: null
    }
  },
  watch: {
    moduleId: function (newVal, oldVal) {
      if(newVal!=oldVal) {
        if(newVal) {
          getModule(newVal).then(res=>{
            this.clickMenuHandle(res.data);
          });
        } else {
          this.clearSelectModuleHandle();
        }
      }
    }
  },
  created() {
  },
  methods: {
    checkPermi,
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
      if(event) {
        this.$emit('input',null);
        // event.stopPropagation();
      }
    },
    resetMenu() {
      this.activeModuleIds = [0];
      this.$forceUpdate();
    }
  }
}
</script>
<style>
  .select-module-popover {
    padding-top: 0px;
  }
</style>
<style lang="scss" scoped>
  ::v-deep .select-module-input {
    display: inline-flex;
    flex-direction: row;
    justify-content: flex-start;
    max-width: 300px;
    width: 100%;
    height: auto;
    line-height: 0;
    align-items: center;
    padding-left: 0px;
    .selectProjectMemberInput_content {
      display: inline-flex;
      flex-direction: row;
      justify-content: flex-start;
      align-items: center;
      flex-wrap: wrap;
      flex-grow: 1;
      overflow: hidden;
      min-height: 34px;
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
      .padding-left-8 input {
        padding-left: 8px;
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
    justify-content: flex-start;
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
  ::v-deep .select-module-input-medium {
    .selectProjectMemberInput_content {
      min-height: 28px;
    }
  }

  ::v-deep .select-module-input-small {
    .selectProjectMemberInput_content {
      min-height: 24px;
    }
  }
  ::v-deep .select-module-input-mini {
    .selectProjectMemberInput_content {
      min-height: 20px;
    }
  }
</style>
