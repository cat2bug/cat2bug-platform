<template>
  <el-popover
    placement="bottom-start"
    v-model="popoverVisible"
    @show="popoverShowHandle"
    @hide="popoverHideHandle"
    :popper-class="popoverPopperClass"
    :visible-arrow="popoverVisibleArrow"
    :trigger="popoverTriggerMode">
    <!-- directMenu：仅作定位锚点，不展示「请选择交付物」输入条（缺陷 Excel 三角直达下层菜单） -->
    <div
      v-if="directMenu"
      slot="reference"
      class="select-module-ref-minimal"
      tabindex="-1"
      aria-hidden="true"
    />
    <div
      v-else
      slot="reference"
      :class="'el-input__inner select-module-input select-module-input-'+size"
      @mouseenter="showClearButtonHandle(true)"
      @mouseleave="showClearButtonHandle(false)"
    >
      <i :class="icon" v-if="icon" style="margin: 0px 0px 0px 10px; color: #C0C4CC;"></i>
      <div class="selectProjectMemberInput_content">
        <el-input ref="selectProjectModuleInput" :size="size" :class="icon?'padding-left-8':''" readonly :placeholder="$t(placeholder)" v-model="queryMember.params.search" @input="searchChangeHandle"></el-input>
      </div>
      <i class="select-module-input__icon el-icon-arrow-up" v-show="!readonly && isClearButtonVisible==false"></i>
      <i class="select-module-input__icon el-icon-circle-close" v-show="!readonly && isClearButtonVisible==true" @click="clearSelectModuleHandle"></i>
    </div>
    <div class="select-module-menu">
      <module-menu v-for="(moduleId, index) in activeModuleIds"
                   ref="moduleMenu"
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
import { checkPermi } from "@/utils/permission";
import MemberNameplate from "@/components/MemberNameplate";
import ModuleMenu from "@/components/Module/SelectModule/menu";
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
      default: 'module.select-module-name'
    },
    clearable: {
      type: Boolean,
      default: true
    },
    isEdit: {
      type: Boolean,
      default: true
    },
    readonly: {
      type: Boolean,
      default: false
    },
    size: {
      type: String,
      default: 'medium'
    },
    icon: {
      type: String,
      default: null
    },
    /** 为 true 时不展示 reference 里的伪输入框，由父组件调 openDirectMenu() 只弹出下层模块树（如缺陷 Excel 三角） */
    directMenu: {
      type: Boolean,
      default: false
    },
    /** 追加到 el-popover 的 popper-class（如缺陷 Excel 用独立类名做点击外部判断，避免页面上多个 SelectModule 时 querySelector 取错） */
    popperExtraClass: {
      type: String,
      default: ''
    }
  },
  computed: {
    popoverPopperClass () {
      const base = 'select-module-popover'
      const extra = (this.popperExtraClass || '').trim()
      return extra ? `${base} ${extra}` : base
    },
    /** directMenu 时默认关箭头（窄锚点）；带 popperExtraClass 时（如缺陷 Excel）再打开，顶侧气泡尖角由父级样式微调位置 */
    popoverVisibleArrow () {
      if (!this.directMenu) return true
      return !!(this.popperExtraClass && String(this.popperExtraClass).trim())
    },
    popoverTriggerMode () {
      if (this.readonly) return 'manual'
      if (this.directMenu) return 'manual'
      return 'click'
    }
  },
  watch: {
    moduleId: function (newVal, oldVal) {
      if(newVal!=oldVal) {
        if(newVal) {
          getModule(newVal).then(res=>{
            if (this.directMenu) {
              this.applyModuleDataWithoutClosingPopover(res.data);
            } else {
              this.clickMenuHandle(res.data);
            }
          });
        } else {
          this.clearSelectModuleHandle();
        }
      }
    }
  },
  mounted() {
    if(this.moduleId) {
      getModule(this.moduleId).then(res => {
        if (this.directMenu) {
          this.applyModuleDataWithoutClosingPopover(res.data);
        } else {
          this.clickMenuHandle(res.data);
        }
      });
    }
  },
  methods: {
    checkPermi,
    /** directMenu：同步当前交付物文案与内部状态，不关 popover、不 emit（避免与 openDirectMenu 抢状态导致闪烁） */
    applyModuleDataWithoutClosingPopover (module) {
      if (!module) return
      this.selectModule = module
      this.queryMember.params.search = module.moduleName != null ? String(module.moduleName) : ''
    },
    /** 缺陷 Excel 等场景：不点伪输入，直接打开 popover 并刷新首级交付物菜单 */
    openDirectMenu () {
      if (!this.directMenu) return
      this.popoverVisible = true
      this.$nextTick(() => {
        const menus = this.$refs.moduleMenu
        const m0 = Array.isArray(menus) ? menus[0] : menus
        if (m0 && typeof m0.open === 'function') {
          m0.open(this.projectId, 0)
        }
      })
    },
    reset() {
      this.$refs['moduleMenu'][0].open(this.projectId, 0); // 刷新第一级交付物菜单数据
      this.resetMenu();
    },
    /** 搜索成员事件 */
    searchChangeHandle() {
      this.$emit('input',this.selectModule.moduleId);
    },
    /** 弹窗显示事件 */
    popoverShowHandle() {
      if (this.directMenu) return
      const inp = this.$refs.selectProjectModuleInput
      if (inp && typeof inp.focus === 'function') inp.focus()
    },
    /** 弹窗隐藏事件 */
    popoverHideHandle() {
      if (this.directMenu) return
      const inp = this.$refs.selectProjectModuleInput
      if (inp && typeof inp.blur === 'function') inp.blur()
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
      this.$emit('input',this.selectModule.moduleId,this.selectModule);
      this.resetMenu();
    },
    /** 点击菜单添加按钮的事件处理 */
    clickAddSubMenuHandle(module,index) {
      this.clickDirectoryHandle(module,index);
    },
    /** 显示或隐藏清除按钮 */
    showClearButtonHandle(visible) {
      if(this.readonly || this.clearable==false) return;
      if(visible && this.queryMember.params.search) {
        this.isClearButtonVisible = true;
      } else {
        this.isClearButtonVisible = false;
      }
    },
    /** 清除选择的交付物 */
    clearSelectModuleHandle(event) {
      this.queryMember.params.search=null;
      this.selectModule=null;
      this.popoverVisible = false;
      this.$forceUpdate();
      if(event) {
        this.$emit('input',null);
        event.stopPropagation();
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
  /* 须压过 .el-popover.el-popper（双类），否则自定义 padding 不生效，仍用主题默认 12px，且与箭头叠加后上下观感不一致 */
  /* 外层不可 overflow:hidden/auto，否则 Element 画在顶缘外的 .popper__arrow 会被裁掉，小三角不显示 */
  .el-popover.el-popper.select-module-popover {
    padding: 10px 10px;
    box-sizing: border-box;
    overflow: visible !important;
    display: inline-flex;
    max-height: none;
  }
  .el-popover.el-popper.select-module-popover > .select-module-menu {
    max-height: 80vh;
    min-height: 0;
    overflow-x: auto;
    overflow-y: auto;
  }
  /* directMenu：占位锚点，不占视觉高度，供 popover 对齐 */
  .select-module-ref-minimal {
    display: block;
    width: 100%;
    height: 0;
    min-height: 0;
    padding: 0;
    margin: 0;
    border: none !important;
    outline: none;
    box-shadow: none !important;
    overflow: hidden;
    opacity: 0;
    pointer-events: none;
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
      min-height: 30px;
      margin: 0px 10px 0px 0px;
      .el-input {
        flex-grow: 1;
        width: 0.1%;
        height: 26px;
        .el-input__inner {
          border-width: 0px;
          height: 26px;
          line-height: 26px;
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
    /* 菜单区相对 popper 上下对称留白 */
    padding: 4px 0;
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
      min-height: 34px;
    }
  }

  ::v-deep .select-module-input-small {
    .selectProjectMemberInput_content {
      min-height: 30px;
    }
  }
  ::v-deep .select-module-input-mini {
    .selectProjectMemberInput_content {
      min-height: 26px;
    }
  }
</style>
