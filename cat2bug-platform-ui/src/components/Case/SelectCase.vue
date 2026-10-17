<template>
  <el-popover
    class="select-case"
    placement="bottom-start"
    trigger="manual"
    @show="popoverShowHandle"
    @hide="popoverHideHandle"
    popper-class="cat2bug-select-case"
    :width="selectCasePopoverWidth"
    v-model:visible="popoverVisible"
    ref="popover"
  >
    <template #reference>
      <div
        class="select-case-input cat2bug-combo-focus-target"
        tabindex="0"
        @mouseenter="showClearButtonHandle(true)"
        @mouseleave="showClearButtonHandle(false)"
        @click="onSelectCaseInputClick"
        @keydown.enter.prevent="toggleCasePopover"
        @keydown.space.prevent="toggleCasePopover"
      >
        <i :class="icon" v-if="icon" class="select-case-input__prefix-icon"></i>
        <case-card
          ref="caseCard"
          class="select-case-input_content"
          :case-model="currentCase"
          :state-visible="true"
          :edit="true"
          :step-index="effectiveStepIndex"
          @change="stepChangeHandle"
          @update:step-index="onStepIndexUpdate"
        />
        <legacy-icon icon="el-icon-arrow-up" class="select-case-input__icon" v-show="isClearButtonVisible==false"></legacy-icon>
        <legacy-icon icon="el-icon-circle-close" class="select-case-input__icon" v-show="isClearButtonVisible==true" @click.stop="clearSelectModuleHandle"></legacy-icon>
      </div>
    </template>
    <div class="case-list">
      <div class="case-list-item" v-for="(c,index) in caseList" :key="index" @click="clickCaseItemHandle(c)">
        <case-card :case-model="c" />
      </div>
    </div>
  </el-popover>
</template>

<script>
import { getCase, listCase } from "@/api/system/case";
import CaseCard from "@/components/Case/CaseCard";
import Label from "@/components/Cat2BugStatistic/Components/Label";
import { storeToRefs } from "pinia";
import { useUserStore } from "@/stores/user";

export default {
  emits: ['update:caseId', 'change', 'input', 'step-change', 'update:stepIndex'],
  name: "SelectCase",
  components: {Label, CaseCard},
  inheritAttrs: false,
  setup() {
    const userStore = useUserStore()
    const { config } = storeToRefs(userStore)
    return { config }
  },
  data() {
    return {
      popoverVisible:false,
      currentCase: {},
      currentCaseId: null,
      caseList: [],
      // 是否显示清除按钮
      isClearButtonVisible: false,
      _caseLoadSeq: 0,
      /** 本地失败步骤下标，点击后立刻反馈，并与 props.stepIndex 同步 */
      localStepIndex: null,
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
    projectId: {
      type: Number,
      default: null
    },
    /** 未通过步骤下标，写入缺陷 caseStepId */
    stepIndex: {
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
  computed: {
    effectiveProjectId() {
      const fromProp = Number(this.projectId)
      if (Number.isFinite(fromProp) && fromProp > 0) return fromProp
      const fromConfig = this.config && parseInt(this.config.currentProjectId, 10)
      return Number.isFinite(fromConfig) && fromConfig > 0 ? fromConfig : null
    },
    /** 用例卡片文字多，浮层加宽；EP 默认 150 会压扁 */
    selectCasePopoverWidth() {
      if (typeof window === 'undefined') return 720
      const vw = window.innerWidth || 1200
      return Math.max(480, Math.min(800, Math.floor(vw * 0.72) - 24))
    },
    effectiveStepIndex() {
      if (this.localStepIndex != null && Number.isFinite(Number(this.localStepIndex))) {
        return Number(this.localStepIndex)
      }
      if (this.stepIndex != null && Number.isFinite(Number(this.stepIndex))) {
        return Number(this.stepIndex)
      }
      return null
    }
  },
  watch: {
    moduleId: function (n, o) {
      if (n && n != o) {
        this.search(n);
      } else if (!n) {
        this.reset();
        if (this.currentCase && this.currentCase.caseId) {
          this.setCaseId({});
        }
      }
    },
    caseId: function (n, o) {
      if (n && n != o) {
        this.refreshCaseId({ clearIfMissing: false });
      } else if (!n && o) {
        this.currentCase = {};
      }
    },
    projectId() {
      if (this.moduleId) {
        this.search(this.moduleId);
      }
    },
    stepIndex: {
      immediate: true,
      handler(v) {
        if (v == null || v === '' || !Number.isFinite(Number(v))) {
          this.localStepIndex = null
        } else {
          this.localStepIndex = Number(v)
        }
      }
    }
  },
  methods: {
    refresh(moduleId) {
      this.search(moduleId != null ? moduleId : this.moduleId);
    },
    reset() {
      this.caseList=[];
    },
    /**
     * @param {{ clearIfMissing?: boolean }} [options]
     * clearIfMissing=true：列表已按交付物筛完仍找不到时清空（切换交付物）
     * clearIfMissing=false：按 id 拉详情回显（编辑回填）
     */
    refreshCaseId(options = {}) {
      const clearIfMissing = !!(options && options.clearIfMissing);
      if (!this.caseId) {
        this.currentCase = {};
        return;
      }
      for (let i = 0; i < this.caseList.length; i++) {
        let c = this.caseList[i];
        if (this.caseId == c.caseId) {
          this.currentCase = c;
          return;
        }
      }
      if (clearIfMissing) {
        this.setCaseId({});
        return;
      }
      this.loadCaseById(this.caseId);
    },
    loadCaseById(caseId) {
      const id = Number(caseId)
      if (!Number.isFinite(id) || id <= 0) {
        this.currentCase = {}
        return
      }
      const seq = ++this._caseLoadSeq
      getCase(id).then(res => {
        if (seq !== this._caseLoadSeq) return
        if (Number(this.caseId) !== id) return
        this.currentCase = (res && res.data) || {}
      }).catch(() => {
        if (seq !== this._caseLoadSeq) return
        this.currentCase = {}
      })
    },
    search(moduleId) {
      this.reset();
      if (!moduleId) {
        return;
      }
      const projectId = this.effectiveProjectId
      // 后端 restrictToModuleTree 依赖 projectId + params[modulePid]；缺 projectId 时子树为空，用例不显示
      if (!projectId) {
        return;
      }
      const seq = ++this._caseLoadSeq
      let query = {
        pageSize: 9999,
        pageNum: 1,
        projectId,
        params: {
          modulePid: moduleId
        }
      }
      listCase(query).then(res=>{
        if (seq !== this._caseLoadSeq) return
        this.caseList = (res && res.rows) || [];
        this.refreshCaseId({ clearIfMissing: true });
      })
    },
    setCaseId(val) {
      this.currentCase = val || {};
      const id = val && val.caseId != null ? val.caseId : null;
      this.$emit('update:caseId', id);
      this.$emit('change', id);
    },
    /** 显示或隐藏清除按钮 */
    showClearButtonHandle(visible) {
      if(this.clearable==false) return;
      if(visible && this.currentCase && this.currentCase.caseId) {
        this.isClearButtonVisible = true;
      } else {
        this.isClearButtonVisible = false;
      }
    },
    /** 清除选择的成员 */
    clearSelectModuleHandle(event) {
      this.setCaseId({});
      this.popoverVisible = false;
      this.$forceUpdate();
      if(event) {
        this.$emit('input',null);
        event.stopPropagation();
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
    /** 点步骤区只改失败步骤，不打开用例下拉 */
    onSelectCaseInputClick(e) {
      const t = e && e.target
      if (t && t.closest && t.closest('.step-row')) {
        return
      }
      if (t && t.closest && t.closest('.select-case-input__icon')) {
        // 清除按钮自己处理；箭头用于开关
        if (t.closest('.el-icon-circle-close') || (t.classList && t.classList.contains('el-icon-circle-close'))) {
          return
        }
      }
      this.toggleCasePopover()
    },
    toggleCasePopover() {
      this.popoverVisible = !this.popoverVisible
    },
    applyFailedStep(index) {
      const next = Number(index)
      if (!Number.isFinite(next)) return
      this.localStepIndex = next
      this.$emit('update:stepIndex', next)
      this.$emit('step-change', next)
    },
    stepChangeHandle(index) {
      this.applyFailedStep(index)
    },
    onStepIndexUpdate(index) {
      this.applyFailedStep(index)
    }
  }
}
</script>
<style>
/* 覆盖 EP 默认 inline width:150；仅靠 min-width 在部分环境下仍会被压扁 */
.el-popover.el-popper.cat2bug-select-case {
  min-width: 480px !important;
  max-width: min(800px, calc(100vw - 32px)) !important;
  box-sizing: border-box;
  padding: 6px 8px !important;
}
.el-popover.el-popper.cat2bug-select-case .case-list {
  overflow-x: hidden;
  overflow-y: auto;
  width: 100%;
  /* 文字多时加高可视区域，超出再滚动 */
  max-height: min(720px, 75vh);
  scrollbar-gutter: auto;
}
.el-popover.el-popper.cat2bug-select-case .case-list-item {
  padding: 4px 2px;
  border-bottom: 1px solid var(--border-color-lighter, #ebeef5);
  border-radius: 4px;
  box-sizing: border-box;
}
.el-popover.el-popper.cat2bug-select-case .case-list-item:last-child {
  border-bottom: none;
}
.el-popover.el-popper.cat2bug-select-case .case-list-item:hover {
  background-color: var(--table-row-hover-bg, #f2f6fc);
  cursor: pointer;
}
</style>
<style lang="scss" scoped>
  .select-case-input {
    display: flex;
    flex-direction: row;
    justify-content: flex-start;
    overflow: hidden;
    width: 100%;
    max-width: 100%;
    min-height: var(--cat2bug-control-height, 32px);
    height: auto;
    align-items: flex-start;
    padding: 8px 12px;
    box-sizing: border-box;
    cursor: pointer;
    /* 边框/圆角与全站 input 一致（见 cat2bug.scss），此处保证多行内容时高度自适应 */
    border-radius: var(--cat2bug-border-radius, 4px);
    border: 1px solid var(--border-color-base, var(--el-input-border-color, #dcdfe6));
    background-color: var(--el-input-bg-color, var(--input-bg, #fff));
    .select-case-input_content {
      flex: 1 1 auto;
      flex-wrap: wrap;
      min-width: 0;
      width: 0;
      min-height: 30px;
      padding: 0 8px 0 0;
      box-sizing: border-box;
    }
    .select-case-input__prefix-icon {
      margin: 0 0 0 4px;
      flex-shrink: 0;
    }
    .select-case-input__icon {
      flex-shrink: 0;
      margin-top: 4px;
      color: var(--cat2bug-input-icon-color, #c0c4cc);
      font-size: 14px;
      transition: transform 0.3s, -webkit-transform 0.3s;
      transform: rotateZ(180deg);
      cursor: pointer;
    }
  }
  .case-list {
    overflow-x: hidden;
    overflow-y: auto;
    width: 100%;
    max-height: min(720px, 75vh);
  }
  .case-list-item {
    padding: 4px 2px;
  }
</style>
