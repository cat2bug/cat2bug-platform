<template>
  <el-popover
    ref="modulePopover"
    placement="bottom-start"
    v-model="popoverVisible"
    @show="popoverShowHandle"
    @after-enter="onModulePopoverAfterEnter"
    @hide="popoverHideHandle"
    :popper-class="popoverPopperClass"
    :visible-arrow="popoverVisibleArrow"
    :boundaries-padding="16"
    :popper-options="modulePopoverPopperOptions"
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
      ref="inputBox"
      :class="'el-input__inner select-module-input cat2bug-combo-focus-target select-module-input-'+size"
      tabindex="0"
      @focus="onOuterFocus"
      @keydown="onShellKeydownForModule"
      @mouseenter="showClearButtonHandle(true)"
      @mouseleave="showClearButtonHandle(false)"
    >
      <i :class="icon" v-if="icon" class="select-module-input__prefix-icon"></i>
      <el-tooltip
        :content="moduleFullPath"
        placement="top"
        :disabled="!moduleFullPath || moduleFullPath === queryMember.params.search"
      >
        <div class="selectProjectMemberInput_content">
          <el-input ref="selectProjectModuleInput" class="select-module-display-input" :size="size" :class="icon?'padding-left-8':''" readonly :placeholder="$t(placeholder)" v-model="queryMember.params.search" @input="searchChangeHandle" @keydown.native.stop="onShellKeydownForModule"></el-input>
        </div>
      </el-tooltip>
      <i class="select-module-input__icon el-icon-arrow-up" :class="{'is-open': popoverVisible}" v-show="!readonly && isClearButtonVisible==false"></i>
      <i class="select-module-input__icon el-icon-circle-close" v-show="!readonly && isClearButtonVisible==true" @click="clearSelectModuleHandle"></i>
    </div>
    <div class="select-module-menu-outer" :class="{'has-menu-scroll-left': menuScrollHasLeft}">
      <button
        v-show="menuScrollHasLeft"
        type="button"
        class="select-module-menu-prev"
        :title="$t('module.prev-column')"
        :aria-label="$t('module.prev-column')"
        @click.stop="scrollMenuColumnsPrev">
        <span
          v-if="menuHiddenParentLevelCount > 0"
          class="select-module-menu-prev-level">{{ $t('module.hidden-parent-levels', [menuHiddenParentLevelCount]) }}</span>
        <i class="el-icon-arrow-left"></i>
      </button>
    <div ref="moduleMenuScroller" class="select-module-menu" @scroll="syncMenuScrollIndicators">
      <module-menu v-for="(moduleId, index) in activeModuleIds"
                   ref="moduleMenu"
                   :key="moduleId + '-' + index"
                   :module-pid="moduleId"
                   :project-id="projectId"
                   :is-edit="isEdit && checkPermi(['system:module:add'])"
                   :keyboard-active-index="keyboardActiveIndexForColumn(index)"
                   :keyboard-active-part="keyboardActivePartForColumn(index)"
                   :path-highlight-module-id="pathHighlightModuleIdForColumn(index)"
                   :label-max-length-override="menuLabelLimits[index]"
                   @clickMenu="clickMenuHandle($event,index)"
                   @clickDirectory="clickDirectoryHandle($event,index)"
                   @clickAddSubMenu="clickAddSubMenuHandle($event,index)"
                   @addInputBlur="onAddModuleInputBlur"
                   @addInputEscape="handleAddModuleInputEscape"
                   @column-ready="onModuleMenuColumnReady"
                   @module-added="onModuleMenuAdded($event, index)"
      ></module-menu>
    </div>
    </div>
  </el-popover>
</template>

<script>
import { checkPermi } from "@/utils/permission";
import MemberNameplate from "@/components/MemberNameplate";
import ModuleMenu from "@/components/Module/SelectModule/menu";
import { getModule, listModulePath } from "@/api/system/module";
import {
  estimateCharsForPixelWidth,
  formatDeliverablePath
} from "@/utils/deliverable-path-display";
import { observeComboFocusTarget } from '@/utils/combo-focus-tab'

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
        params: {
          search: null
        }
      },
      addModule: null,
      /** 列数超出可视宽度时，各列菜单项 label 最大字符数（null 表示按列宽自动） */
      menuLabelLimits: [],
      /** 左侧有被隐藏的父级列时显示层数与 < 翻页 */
      menuScrollHasLeft: false,
      /** 视口左侧被隐藏的父级菜单列数 */
      menuHiddenParentLevelCount: 0,
      moduleFullPath: '',
      modulePathById: {},
      // 键盘导航：当前列与行（列对应父/子级联菜单）
      activeColumnIndex: 0,
      activeRowIndex: -1,
      /** main=菜单行 | side=行内右侧添加子级 | new=底部新建交付物 */
      activeRowPart: 'main',
      /** 忽略过期的 getModule 回调（表单重置与快速切换 moduleId 时） */
      _moduleLoadSeq: 0,
      /** 行内 + 打开的子级新建列索引（Esc 时收起该列回到父菜单） */
      _openedForAddSubColumnIndex: null
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
    },
    modulePopoverPopperOptions () {
      return {
        gpuAcceleration: false,
        boundariesElement: 'viewport',
        boundariesPadding: 16,
        /* 展开子级列后不因 flip 改到 top，避免抽屉底部输入框旁的下拉跳到表单中部 */
        modifiersIgnored: ['flip']
      }
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler(id) {
        this.reloadModulePathMap(id)
      }
    },
    moduleId(newVal, oldVal) {
      if (newVal === oldVal) return
      if (newVal) {
        this.loadModuleForProp(newVal)
      } else {
        this.clearSelectModuleState(false)
      }
    },
    popoverVisible(val) {
      if (val) {
        this.attachModulePopoverKeyboardListener();
        this.attachModulePopoverScrollSync();
        this.$nextTick(() => {
          this.fitModulePopoverLayout();
          this._onModulePopoverResize = () => this.fitModulePopoverLayout();
          window.addEventListener('resize', this._onModulePopoverResize);
        });
      } else {
        this.detachModulePopoverKeyboardListener();
        this.detachModulePopoverScrollSync();
        if (this._onModulePopoverResize) {
          window.removeEventListener('resize', this._onModulePopoverResize);
          this._onModulePopoverResize = null;
        }
      }
    }
  },
  mounted() {
    this.$nextTick(() => {
      const shell = this.$refs.inputBox;
      if (shell) {
        shell.__cat2bugOnTabAway = () => {
          if (this.popoverVisible) this.popoverVisible = false;
        };
        observeComboFocusTarget(shell);
      }
    });
    if (this.moduleId) {
      this.loadModuleForProp(this.moduleId)
    }
  },
  beforeDestroy() {
    const shell = this.$refs.inputBox;
    if (shell) shell.__cat2bugOnTabAway = null;
    this.detachModulePopoverKeyboardListener();
    this.detachModulePopoverScrollSync();
    this.detachMenuScrollerListener();
  },
  methods: {
    checkPermi,
    keyboardActiveIndexForColumn(index) {
      return index === this.activeColumnIndex ? this.activeRowIndex : -1;
    },
    keyboardActivePartForColumn(index) {
      return index === this.activeColumnIndex ? this.activeRowPart : 'main';
    },
    /** 当前选中路径上，本列应对应高亮的父级 moduleId（仅 index < activeColumnIndex） */
    pathHighlightModuleIdForColumn(index) {
      if (index >= this.activeColumnIndex) {
        return null;
      }
      const id = this.activeModuleIds[index + 1];
      if (id == null || Number(id) === 0) {
        return null;
      }
      return Number(id);
    },
    /** 定位锚点：el-popover 的 reference（展开子级后须刷新，否则 Popper 仍绑旧 DOM） */
    getModulePopoverReferenceEl() {
      if (this.directMenu) {
        return (this.$el && this.$el.querySelector('.select-module-ref-minimal')) || null;
      }
      return this.$refs.inputBox || null;
    },
    refreshModulePopoverReference() {
      const popover = this.$refs.modulePopover;
      const ref = this.getModulePopoverReferenceEl();
      if (!popover || !ref) return;
      popover.referenceElm = ref;
      if (popover.popperJS && popover.popperJS._reference) {
        popover.popperJS._reference = ref;
      }
    },
    attachModulePopoverScrollSync() {
      this.detachModulePopoverScrollSync();
      this._modulePopoverScrollSync = () => {
        if (!this.popoverVisible) return;
        this.refreshModulePopoverReference();
        const popover = this.$refs.modulePopover;
        if (popover && typeof popover.updatePopper === 'function') {
          popover.updatePopper();
        }
      };
      this._modulePopoverScrollNodes = [];
      const ref = this.getModulePopoverReferenceEl();
      let node = ref;
      while (node && node !== document.documentElement) {
        if (node === document.body) break;
        try {
          const style = window.getComputedStyle(node);
          const oy = style.overflowY;
          const ox = style.overflowX;
          const scrollable = /auto|scroll|overlay/.test(oy) || /auto|scroll|overlay/.test(ox);
          if (scrollable && (node.scrollHeight > node.clientHeight || node.scrollWidth > node.clientWidth)) {
            node.addEventListener('scroll', this._modulePopoverScrollSync, { passive: true });
            this._modulePopoverScrollNodes.push(node);
          }
        } catch (err) { /* ignore */ }
        node = node.parentElement;
      }
      window.addEventListener('scroll', this._modulePopoverScrollSync, { passive: true });
      this._modulePopoverScrollWindowBound = true;
    },
    detachModulePopoverScrollSync() {
      if (this._modulePopoverScrollNodes && this._modulePopoverScrollSync) {
        this._modulePopoverScrollNodes.forEach((node) => {
          node.removeEventListener('scroll', this._modulePopoverScrollSync);
        });
      }
      if (this._modulePopoverScrollWindowBound && this._modulePopoverScrollSync) {
        window.removeEventListener('scroll', this._modulePopoverScrollSync);
      }
      this._modulePopoverScrollNodes = null;
      this._modulePopoverScrollWindowBound = false;
      this._modulePopoverScrollSync = null;
    },
    /** 子级菜单列数据加载完成后重新贴合 reference */
    onModuleMenuColumnReady() {
      if (!this.popoverVisible) return;
      this.$nextTick(() => this.fitModulePopoverLayout());
    },
    /** 本列新建交付物成功后：选中新建项并显示键盘高亮背景，下拉保持展开 */
    onModuleMenuAdded(payload, columnIndex) {
      this.popoverVisible = true;
      if (columnIndex > 0) {
        this.bumpParentChildrenCount(columnIndex, payload);
      }
      if (columnIndex !== this.activeColumnIndex) return;
      this.waitForSubMenuColumnReady(columnIndex, (menu) => {
        const list = menu.moduleList || [];
        if (!list.length) return;
        let idx = -1;
        const created = payload && payload.module;
        if (created && created.moduleId != null) {
          idx = list.findIndex((m) => Number(m.moduleId) === Number(created.moduleId));
        }
        if (idx < 0 && payload && payload.moduleName) {
          const name = String(payload.moduleName).trim();
          idx = list.findIndex((m) => String(m.moduleName).trim() === name);
        }
        if (idx < 0) idx = list.length - 1;
        this.activeRowIndex = idx;
        this.activeRowPart = 'main';
        this._openedForAddSubColumnIndex = null;
        this.hideActiveAddModuleForm();
        const module = list[idx];
        if (module) {
          this.selectModule = module;
          const pathOverride = this.buildModulePathFromOpenColumns(module, columnIndex);
          this.syncModuleDisplay(module, pathOverride);
          this.$emit('input', module.moduleId, module);
          this.reloadModulePathMap(this.projectId || module.projectId);
        }
        this.$nextTick(() => {
          this.popoverVisible = true;
          this.scrollActiveModuleIntoView();
          this.focusActiveMenuOption();
        });
      });
    },
    /** 子列新建成功后，更新父列对应项的 childrenCount */
    bumpParentChildrenCount(childColumnIndex, payload) {
      if (childColumnIndex <= 0) return;
      let parentModuleId = this.activeModuleIds[childColumnIndex];
      const created = payload && payload.module;
      if ((parentModuleId == null || Number(parentModuleId) === 0) &&
        created && created.modulePid != null) {
        parentModuleId = created.modulePid;
      }
      if (parentModuleId == null) return;
      const menus = this.getModuleMenus();
      const parentMenu = menus[childColumnIndex - 1];
      if (parentMenu && typeof parentMenu.bumpModuleChildrenCount === 'function') {
        parentMenu.bumpModuleChildrenCount(parentModuleId);
      }
    },
    attachModulePopoverKeyboardListener() {
      this.detachModulePopoverKeyboardListener();
      this.$_onModulePopoverKeydown = (e) => {
        if (!this.popoverVisible || this.readonly || this.directMenu) return;
        const t = e.target;
        const inAddInput = t && t.closest && t.closest('.select-module-add') &&
          (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA');
        if (inAddInput && e.key === 'Escape') {
          e.preventDefault();
          e.stopPropagation();
          this.handleAddModuleInputEscape(t);
          return;
        }
        if (e.key === 'Escape') {
          if (e.metaKey || e.ctrlKey) return;
          if (typeof e.getModifierState === 'function' &&
            (e.getModifierState('Meta') || e.getModifierState('Control'))) return;
          e.preventDefault();
          e.stopPropagation();
          this.closePopoverKeepFocus();
          return;
        }
        if (inAddInput) {
          return;
        }
        const navKeys = ['ArrowDown', 'ArrowUp', 'ArrowLeft', 'ArrowRight', 'Enter', ' ', 'Tab'];
        if (navKeys.indexOf(e.key) < 0 && e.code !== 'Space') return;
        this.moduleKeyDownHandle(e);
        if (e.key !== 'Tab') {
          e.preventDefault();
          e.stopPropagation();
        }
      };
      document.addEventListener('keydown', this.$_onModulePopoverKeydown, true);
    },
    detachModulePopoverKeyboardListener() {
      if (this.$_onModulePopoverKeydown) {
        document.removeEventListener('keydown', this.$_onModulePopoverKeydown, true);
        this.$_onModulePopoverKeydown = null;
      }
    },
    /** 限制 popover 不超出视口/抽屉，宽度随菜单列内容收缩（列内超长文字省略） */
    fitModulePopoverLayout() {
      const apply = () => {
        const scroller = this.$refs.moduleMenuScroller;
        if (!scroller) return;
        const popper = scroller.closest('.select-module-popover');
        if (!popper) return;

        const pad = 16;
        const vw = window.innerWidth;
        let maxWidth = vw - pad * 2;

        const ref = this.directMenu
          ? (this.$el && this.$el.querySelector('.select-module-ref-minimal'))
          : this.$refs.inputBox;
        let refRect = null;
        if (ref && typeof ref.getBoundingClientRect === 'function') {
          refRect = ref.getBoundingClientRect();
          maxWidth = Math.min(maxWidth, vw - refRect.left - pad);
        }

        const drawer = ref && ref.closest('.el-drawer');
        if (drawer && typeof drawer.getBoundingClientRect === 'function') {
          const dr = drawer.getBoundingClientRect();
          if (refRect) {
            maxWidth = Math.min(maxWidth, dr.right - refRect.left - pad);
          } else {
            maxWidth = Math.min(maxWidth, dr.width - pad * 2);
          }
        }

        maxWidth = Math.max(160, Math.floor(maxWidth));
        popper.style.setProperty('max-width', `${maxWidth}px`, 'important');
        popper.style.boxSizing = 'border-box';

        const colW = 200;
        const colCount = this.activeModuleIds.length;
        const popoverPadding = 20;
        const contentW = colCount * colW + popoverPadding;
        if (contentW > maxWidth) {
          popper.style.setProperty('width', `${maxWidth}px`, 'important');
        } else {
          popper.style.removeProperty('width');
        }

        const outer = scroller.parentElement;
        if (outer && outer.classList.contains('select-module-menu-outer')) {
          outer.style.removeProperty('width');
          outer.style.maxWidth = '100%';
        }
        scroller.style.removeProperty('width');
        scroller.style.maxWidth = '100%';

        this.updateMenuLabelLimits(scroller, maxWidth);

        this.refreshModulePopoverReference();
        const popover = this.$refs.modulePopover;
        if (popover && typeof popover.updatePopper === 'function') {
          popover.updatePopper();
        }
        this.syncMenuScrollIndicators();
        this.syncAllMenuColumnScrollIndicators();
      };
      this.$nextTick(() => {
        apply();
        requestAnimationFrame(apply);
      });
    },
    /** 多列超出可视宽度时，压缩非末列 label，为末列录入区留出空间 */
    updateMenuLabelLimits(scroller, popoverMaxWidth) {
      const colCount = this.activeModuleIds.length;
      const limits = new Array(colCount).fill(null);
      if (colCount <= 1) {
        this.menuLabelLimits = limits;
        return;
      }
      const viewW = (scroller && scroller.clientWidth) || popoverMaxWidth - 20 || 0;
      const colW = 200;
      if (!viewW || colCount * colW <= viewW) {
        this.menuLabelLimits = limits;
        return;
      }
      const lastIdx = colCount - 1;
      const otherCols = colCount - 1;
      const budgetOthers = Math.max(otherCols * 72, viewW - colW);
      const perCol = Math.floor(budgetOthers / otherCols);
      const maxChars = estimateCharsForPixelWidth(Math.max(28, perCol - 44));
      for (let i = 0; i < lastIdx; i++) {
        limits[i] = maxChars;
      }
      this.menuLabelLimits = limits;
    },
    onModulePopoverAfterEnter() {
      this.attachMenuScrollerListener();
      this.fitModulePopoverLayout();
      this.scrollMenuColumnIntoView();
      this.$nextTick(() => this.syncAllMenuColumnScrollIndicators());
    },
    onShellKeydownForModule(e) {
      if (this.popoverVisible) return;
      this.moduleKeyDownHandle(e);
    },
    getModuleMenus() {
      const menus = this.$refs.moduleMenu;
      if (!menus) return [];
      return Array.isArray(menus) ? menus : [menus];
    },
    focusOuterShell() {
      const outer = this.$refs.inputBox;
      if (outer && typeof outer.focus === 'function') {
        outer.focus();
      }
    },
    /** Esc 退出新建录入框后，焦点回到当前高亮菜单行的文字选项（非 + 按钮） */
    focusActiveMenuOption(attempt = 0) {
      if (!this.popoverVisible) return;
      const menu = this.getActiveMenu();
      if (!menu || !menu.$el) {
        if (attempt < 8) {
          this.$nextTick(() => this.focusActiveMenuOption(attempt + 1));
        }
        return;
      }
      let rowIndex = this.activeRowIndex;
      if (this.isNewRowIndex(menu, rowIndex)) {
        const n = (menu.moduleList || []).length;
        rowIndex = n > 0 ? n - 1 : -1;
      }
      if (rowIndex < 0) {
        if (attempt < 8) {
          this.$nextTick(() => this.focusActiveMenuOption(attempt + 1));
        }
        return;
      }
      const rows = menu.$el.querySelectorAll('.module-menu-row');
      const row = rows[rowIndex];
      const el = (row && row.querySelector('.module-menu-row-label')) || row;
      if (!el) {
        if (attempt < 8) {
          this.$nextTick(() => this.focusActiveMenuOption(attempt + 1));
        }
        return;
      }
      if (!el.hasAttribute('tabindex')) {
        el.setAttribute('tabindex', '-1');
      }
      try {
        el.focus({ preventScroll: false });
      } catch (err) {
        if (typeof el.focus === 'function') el.focus();
      }
      this.scrollActiveModuleIntoView();
    },
    closePopoverKeepFocus() {
      if (!this.popoverVisible) return;
      this.popoverVisible = false;
      this.$nextTick(() => {
        setTimeout(() => this.focusOuterShell(), 150);
      });
    },
    getActiveMenu() {
      const menus = this.getModuleMenus();
      return menus[this.activeColumnIndex];
    },
    getMenuRowCount(menu) {
      if (!menu) return 0;
      const n = (menu.moduleList || []).length;
      return menu.isEdit ? n + 1 : n;
    },
    isNewRowIndex(menu, rowIndex) {
      if (!menu || !menu.isEdit) return false;
      return rowIndex === (menu.moduleList || []).length;
    },
    menuRowHasSideAdd(menu, rowIndex) {
      if (!menu || !menu.isEdit) return false;
      const list = menu.moduleList || [];
      const module = list[rowIndex];
      if (!module) return false;
      return !menu.moduleHasChildren(module);
    },
    /** 键盘：外框与内部 input 共用 */
    moduleKeyDownHandle(e) {
      if (this.readonly || this.directMenu) return;
      const t = e.target;
      if (t && t.closest && t.closest('.select-module-add') &&
        (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA')) {
        return;
      }
      const key = e.key;
      if (!this.popoverVisible) {
        if (key === 'ArrowDown' || key === 'ArrowUp' || key === 'Enter') {
          e.preventDefault();
          this.popoverVisible = true;
        }
        return;
      }
      if (key === 'ArrowDown') { e.preventDefault(); this.moveModuleRow(1); return; }
      if (key === 'ArrowUp') { e.preventDefault(); this.moveModuleRow(-1); return; }
      if (key === 'ArrowLeft') { e.preventDefault(); this.moveModuleHorizontal(-1); return; }
      if (key === 'ArrowRight') { e.preventDefault(); this.moveModuleHorizontal(1); return; }
      if (key === 'Enter' || key === ' ' || key === 'Spacebar' || e.code === 'Space') {
        e.preventDefault();
        this.selectActiveModule();
        return;
      }
      if (key === 'Escape') {
        if (t && t.closest && t.closest('.select-module-add') &&
          (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA')) {
          e.preventDefault();
          this.handleAddModuleInputEscape(t);
          return;
        }
        if (!e.metaKey && !e.ctrlKey &&
          !(typeof e.getModifierState === 'function' &&
            (e.getModifierState('Meta') || e.getModifierState('Control')))) {
          e.preventDefault();
          this.closePopoverKeepFocus();
        }
        return;
      }
      if (key === 'Tab' && this.popoverVisible) {
        this.popoverVisible = false;
      }
    },
    moveModuleRow(dir) {
      const menu = this.getActiveMenu();
      if (!menu || menu.loading) return;
      const rowCount = this.getMenuRowCount(menu);
      if (!rowCount) return;
      let i = this.activeRowIndex < 0 ? 0 : this.activeRowIndex;
      const prev = i;
      i += dir;
      if (i < 0) {
        if (prev <= 0 && this.activeColumnIndex <= 0) {
          this.closePopoverKeepFocus();
          return;
        }
        i = 0;
      }
      if (i >= rowCount) i = rowCount - 1;
      this.activeRowIndex = i;
      this.activeRowPart = this.isNewRowIndex(menu, i) ? 'new' : 'main';
      this.$nextTick(() => this.scrollActiveModuleIntoView());
    },
    moveModuleHorizontal(dir) {
      const menu = this.getActiveMenu();
      if (!menu || menu.loading) return;
      const list = menu.moduleList || [];
      const idx = this.activeRowIndex;

      if (this.activeRowPart === 'new' || this.isNewRowIndex(menu, idx)) {
        if (dir < 0) this.focusParentMenuFromNewRow();
        return;
      }

      if (idx < 0 || idx >= list.length) return;
      const module = list[idx];

      if (dir > 0) {
        if (this.activeRowPart === 'main') {
          if (this.menuRowHasSideAdd(menu, idx)) {
            this.activeRowPart = 'side';
            this.$nextTick(() => this.scrollActiveModuleIntoView());
          } else if (menu.moduleHasChildren(module)) {
            this.moveModuleColumn(1);
          }
        }
        return;
      }

      if (this.activeRowPart === 'side') {
        this.activeRowPart = 'main';
        return;
      }
      if (this.activeColumnIndex > 0) {
        this.moveModuleColumn(-1);
      }
    },
    /** 「新建交付物」行按 ← / Esc：回到父列并高亮展开子列的那一项 */
    returnToParentMenuFromChildColumn(options = {}) {
      const { fitLayout = false } = options;
      if (this.activeColumnIndex <= 0) return false;
      const childCol = this.activeColumnIndex;
      const openedByModuleId = this.activeModuleIds[childCol];
      this._openedForAddSubColumnIndex = null;
      this.hideActiveAddModuleForm();
      this.activeColumnIndex = childCol - 1;
      this.collapseModuleColumnsToActiveIndex();
      const parentMenu = this.getActiveMenu();
      const list = (parentMenu && parentMenu.moduleList) || [];
      const rowIdx = list.findIndex((m) => Number(m.moduleId) === Number(openedByModuleId));
      if (rowIdx >= 0) {
        this.activeRowIndex = rowIdx;
        this.activeRowPart = 'main';
      } else {
        this.clampActiveRowIndex();
        this.activeRowPart = 'main';
      }
      this.$nextTick(() => {
        if (fitLayout) this.fitModulePopoverLayout();
        this.scrollMenuColumnIntoView();
        this.scrollActiveModuleIntoView();
        if (fitLayout) this.syncMenuScrollIndicators();
        this.focusActiveMenuOption();
      });
      return true;
    },
    focusParentMenuFromNewRow() {
      this.returnToParentMenuFromChildColumn();
    },
    moveModuleColumn(dir) {
      if (dir < 0) {
        this.returnToParentMenuFromChildColumn();
        return;
      }
      const menus = this.getModuleMenus();
      const menu = menus[this.activeColumnIndex];
      if (!menu || menu.loading) return;
      const list = menu.moduleList || [];
      const module = list[this.activeRowIndex];
      if (!module || !menu.moduleHasChildren(module)) return;
      this.clickDirectoryHandle(module, this.activeColumnIndex);
      const childCol = this.activeColumnIndex;
      this.waitForSubMenuColumnReady(childCol, () => {
        const menus = this.getModuleMenus();
        this.activeColumnIndex = Math.min(childCol, menus.length - 1);
        this.activeRowIndex = 0;
        this.activeRowPart = 'main';
        this.fitModulePopoverLayout();
        this.scrollMenuColumnIntoView();
        this.scrollActiveModuleIntoView();
      });
    },
    clampActiveRowIndex() {
      const menu = this.getActiveMenu();
      const rowCount = this.getMenuRowCount(menu);
      if (!rowCount) {
        this.activeRowIndex = -1;
        this.activeRowPart = 'main';
        return;
      }
      if (this.activeRowIndex < 0 || this.activeRowIndex >= rowCount) {
        this.activeRowIndex = 0;
        this.activeRowPart = 'main';
      }
    },
    /** 收起当前列右侧的子菜单列（← 返回父级时） */
    collapseModuleColumnsToActiveIndex() {
      const keep = this.activeColumnIndex + 1;
      if (this.activeModuleIds.length > keep) {
        this.activeModuleIds = this.activeModuleIds.slice(0, keep);
      }
    },
    scrollMenuColumnIntoView() {
      this.fitModulePopoverLayout();
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          const scroller = this.$refs.moduleMenuScroller;
          const menus = this.getModuleMenus();
          const col = menus[this.activeColumnIndex];
          if (!scroller || !col || !col.$el) return;
          const colEl = this.getMenuColumnEl(col);
          if (!colEl) return;

          const colLeft = colEl.offsetLeft;
          const colRight = colLeft + colEl.offsetWidth;
          const viewLeft = scroller.scrollLeft;
          const viewRight = viewLeft + scroller.clientWidth;
          const edgePad = 8;
          let target = scroller.scrollLeft;

          if (colRight > viewRight - edgePad) {
            target = colRight - scroller.clientWidth + edgePad;
          } else if (colLeft < viewLeft + edgePad) {
            target = colLeft - edgePad;
          }

          const maxScroll = Math.max(0, scroller.scrollWidth - scroller.clientWidth);
          scroller.scrollLeft = Math.max(0, Math.min(target, maxScroll));
          this.syncMenuScrollIndicators();
        });
      });
    },
    attachMenuScrollerListener() {
      this.$nextTick(() => {
        const scroller = this.$refs.moduleMenuScroller;
        if (!scroller || this._menuScrollerScrollBound) return;
        this._menuScrollerOnScroll = () => this.syncMenuScrollIndicators();
        scroller.addEventListener('scroll', this._menuScrollerOnScroll, { passive: true });
        this._menuScrollerScrollBound = true;
        this.syncMenuScrollIndicators();
      });
    },
    detachMenuScrollerListener() {
      const scroller = this.$refs.moduleMenuScroller;
      if (scroller && this._menuScrollerOnScroll) {
        scroller.removeEventListener('scroll', this._menuScrollerOnScroll);
      }
      this._menuScrollerOnScroll = null;
      this._menuScrollerScrollBound = false;
      this.menuScrollHasLeft = false;
      this.menuHiddenParentLevelCount = 0;
    },
    getMenuColumnEl(menu) {
      if (!menu || !menu.$el) return null;
      const root = menu.$el;
      if (root.classList && root.classList.contains('module-menu-column')) return root;
      if (root.classList && root.classList.contains('module-menu')) return root;
      return root.querySelector('.module-menu-column') || root.querySelector('.module-menu') || root;
    },
    syncAllMenuColumnScrollIndicators() {
      this.getModuleMenus().forEach((menu) => {
        if (menu && typeof menu.syncColumnScrollIndicators === 'function') {
          menu.syncColumnScrollIndicators();
        }
      });
    },
    countHiddenParentColumns(scroller) {
      const menus = this.getModuleMenus();
      if (!scroller || !menus.length) return 0;
      const viewLeft = scroller.scrollLeft + 4;
      let hidden = 0;
      for (let i = 0; i < menus.length; i++) {
        const colEl = this.getMenuColumnEl(menus[i]);
        if (!colEl) continue;
        const colRight = colEl.offsetLeft + colEl.offsetWidth;
        if (colRight <= viewLeft) {
          hidden++;
        } else {
          break;
        }
      }
      return hidden;
    },
    syncMenuScrollIndicators() {
      const scroller = this.$refs.moduleMenuScroller;
      if (!scroller) {
        this.menuScrollHasLeft = false;
        this.menuHiddenParentLevelCount = 0;
        return;
      }
      const edgePad = 4;
      const hasOverflow = scroller.scrollWidth > scroller.clientWidth + edgePad;
      this.menuHiddenParentLevelCount = this.countHiddenParentColumns(scroller);
      this.menuScrollHasLeft = hasOverflow && (
        this.menuHiddenParentLevelCount > 0 || scroller.scrollLeft > edgePad
      );
    },
    scrollMenuColumnsPrev() {
      const scroller = this.$refs.moduleMenuScroller;
      const edgePad = 4;
      if (this.activeColumnIndex > 0) {
        this.moveModuleColumn(-1);
        return;
      }
      if (scroller && scroller.scrollLeft > edgePad) {
        scroller.scrollLeft = Math.max(0, scroller.scrollLeft - 200);
        this.syncMenuScrollIndicators();
      }
    },
    scrollActiveModuleIntoView() {
      const menu = this.getActiveMenu();
      if (!menu || !menu.$el) return;
      if (this.isNewRowIndex(menu, this.activeRowIndex)) {
        if (typeof menu.resetMenuScrollToBottom === 'function') {
          menu.resetMenuScrollToBottom();
        }
        return;
      }
      if (this.activeRowPart === 'side') {
        const sideBtn = menu.$el.querySelectorAll('.module-menu-row-side-add')[this.activeRowIndex];
        if (sideBtn && typeof sideBtn.scrollIntoView === 'function') {
          sideBtn.scrollIntoView({ block: 'nearest' });
          return;
        }
      }
      if (this.activeRowIndex === 0 && this.activeRowPart === 'main') {
        if (typeof menu.resetMenuScrollToTop === 'function') {
          menu.resetMenuScrollToTop();
        }
        return;
      }
      const listLen = (menu.moduleList || []).length;
      const lastMainIdx = listLen - 1;
      if (lastMainIdx >= 0 && this.activeRowIndex === lastMainIdx && this.activeRowPart === 'main') {
        if (typeof menu.resetMenuScrollToBottom === 'function') {
          menu.resetMenuScrollToBottom();
        }
        return;
      }
      const rows = menu.$el.querySelectorAll('.module-menu-row');
      const row = rows[this.activeRowIndex];
      const label = row && row.querySelector('.module-menu-row-label');
      const el = label || row;
      if (el && typeof el.scrollIntoView === 'function') {
        el.scrollIntoView({ block: 'nearest' });
      }
      this.$nextTick(() => this.syncAllMenuColumnScrollIndicators());
    },
    activateNewModuleRow(menu) {
      const addComp = menu.$refs.addModuleMenu;
      if (!addComp) return;
      if (!addComp.formVisible) {
        addComp.setFormVisible(true);
      }
      this.focusAddModuleInput(addComp);
    },
    focusAddModuleInput(addComp, attempt = 0) {
      const tryFocus = () => {
        let input = null;
        if (addComp.$refs.moduleNameInput && addComp.$refs.moduleNameInput.$refs &&
          addComp.$refs.moduleNameInput.$refs.input) {
          input = addComp.$refs.moduleNameInput.$refs.input;
        }
        if (!input && addComp.$el) {
          input = addComp.$el.querySelector('.select-module-add input');
        }
        if (input && typeof input.focus === 'function') {
          input.focus();
          return true;
        }
        return false;
      };
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          if (!tryFocus() && attempt < 12) {
            setTimeout(() => this.focusAddModuleInput(addComp, attempt + 1), 50);
          }
        });
      });
    },
    hideActiveAddModuleForm() {
      const menu = this.getActiveMenu();
      const addComp = menu && menu.$refs && menu.$refs.addModuleMenu;
      if (!addComp) return;
      if (addComp.$refs && addComp.$refs.form) {
        addComp.$refs.form.clearValidate();
      }
      addComp.setFormVisible(false);
      addComp.reset();
    },
    handleAddModuleInputEscape(inputEl) {
      if (!inputEl) {
        const menu = this.getActiveMenu();
        const addComp = menu && menu.$refs && menu.$refs.addModuleMenu;
        const inputRef = addComp && addComp.$refs && addComp.$refs.moduleNameInput;
        inputEl = inputRef && inputRef.$refs && inputRef.$refs.input;
      }
      if (inputEl && typeof inputEl.blur === 'function') {
        inputEl.blur();
      }
      this.hideActiveAddModuleForm();

      const menu = this.getActiveMenu();
      const onNewRow = menu && this.isNewRowIndex(menu, this.activeRowIndex);
      if (onNewRow && this.activeColumnIndex > 0) {
        this.returnToParentMenuFromChildColumn({ fitLayout: true });
        return;
      }

      if (onNewRow) {
        const list = menu.moduleList || [];
        if (list.length > 0) {
          this.activeRowIndex = list.length - 1;
        }
        this.activeRowPart = 'main';
      }
      this.$nextTick(() => {
        this.scrollActiveModuleIntoView();
        this.focusActiveMenuOption();
      });
    },
    blurAddModuleInput(inputEl) {
      this.handleAddModuleInputEscape(inputEl);
    },
    onAddModuleInputBlur() {
      this.$nextTick(() => {
        this.focusOuterShell();
      });
    },
    selectActiveModule() {
      const menu = this.getActiveMenu();
      if (!menu || menu.loading) return;
      const list = menu.moduleList || [];
      const idx = this.activeRowIndex;

      if (this.isNewRowIndex(menu, idx)) {
        const addComp = menu.$refs.addModuleMenu;
        if (addComp && addComp.formVisible) {
          const input = addComp.$refs.moduleNameInput &&
            addComp.$refs.moduleNameInput.$refs &&
            addComp.$refs.moduleNameInput.$refs.input;
          if (input && document.activeElement === input) {
            addComp.addProjectModule();
          } else {
            this.activateNewModuleRow(menu);
          }
        } else {
          this.activateNewModuleRow(menu);
        }
        return;
      }

      if (idx < 0 || idx >= list.length) return;
      const module = list[idx];

      if (this.activeRowPart === 'side') {
        this.clickAddSubMenuHandle(module, this.activeColumnIndex);
        return;
      }

      if (menu.moduleHasChildren(module)) {
        this.moveModuleColumn(1);
      } else {
        this.clickMenuHandle(module, this.activeColumnIndex);
      }
    },
    reloadModulePathMap(projectId) {
      const pid = Number(projectId)
      if (!pid) {
        this.modulePathById = {}
        return
      }
      listModulePath(pid).then((res) => {
        const map = {}
        ;(res.data || []).forEach((m) => {
          if (m && m.moduleId != null && m.modulePath) {
            map[m.moduleId] = m.modulePath
          }
        })
        this.modulePathById = map
        if (this.selectModule) {
          this.syncModuleDisplay(this.selectModule)
        }
      }).catch(() => {
        this.modulePathById = {}
      })
    },
    resolveModuleFullPath(module) {
      if (!module) return ''
      if (module.modulePath) {
        return String(module.modulePath).trim()
      }
      const id = module.moduleId
      if (id != null && this.modulePathById[id]) {
        return String(this.modulePathById[id]).trim()
      }
      return module.moduleName != null ? String(module.moduleName).trim() : ''
    },
    /** 从当前已展开的各级菜单拼出全路径（选中瞬间 modulePath 可能尚未入库） */
    buildModulePathFromOpenColumns(module, columnIndex) {
      if (!module || columnIndex == null || columnIndex < 0) return '';
      const menus = this.getModuleMenus();
      const parts = [];
      for (let col = 0; col < columnIndex; col++) {
        const targetId = this.activeModuleIds[col + 1];
        if (targetId == null || Number(targetId) === 0) continue;
        const menu = menus[col];
        const row = menu && (menu.moduleList || []).find(
          (m) => Number(m.moduleId) === Number(targetId)
        );
        const name = row && row.moduleName != null ? String(row.moduleName).trim() : '';
        if (name) parts.push(name);
      }
      const leaf = module.moduleName != null ? String(module.moduleName).trim() : '';
      if (leaf) parts.push(leaf);
      return parts.join('/');
    },
    setDisplaySearch(value) {
      this.$set(this.queryMember.params, 'search', value);
    },
    applyDisplaySearchFormatted(full) {
      const apply = () => {
        const formatted = formatDeliverablePath(full, this.getDisplayMaxLength());
        this.setDisplaySearch(formatted.short || formatted.full || '');
      };
      apply();
      this.$nextTick(() => {
        apply();
        requestAnimationFrame(apply);
      });
    },
    syncModuleDisplay(module, pathOverride) {
      if (!module) {
        this.moduleFullPath = '';
        this.setDisplaySearch(null);
        return;
      }
      const override = pathOverride != null ? String(pathOverride).trim() : '';
      const full = override || this.resolveModuleFullPath(module);
      this.moduleFullPath = full;
      this.applyDisplaySearchFormatted(full);
    },
    /** 按输入框可视宽度估算可展示字符数 */
    getDisplayMaxLength() {
      const input = this.$refs.selectProjectModuleInput
      const inner = input && input.$el ? input.$el.querySelector('.el-input__inner') : null
      if (inner && inner.clientWidth > 0) {
        const style = window.getComputedStyle(inner)
        const fontSize = parseFloat(style.fontSize) || 14
        return Math.max(10, Math.floor(inner.clientWidth / (fontSize * 0.58)))
      }
      return 20
    },
    /** directMenu：同步当前交付物文案与内部状态，不关 popover、不 emit（避免与 openDirectMenu 抢状态导致闪烁） */
    applyModuleDataWithoutClosingPopover (module) {
      if (!module) return
      this.selectModule = module
      this.syncModuleDisplay(module)
    },
    /** 缺陷 Excel 等场景：不点伪输入，直接打开 popover 并刷新首级交付物菜单 */
    openDirectMenu () {
      if (!this.directMenu) return
      this.popoverVisible = true
      this.$nextTick(() => {
        this.fitModulePopoverLayout()
        const menus = this.$refs.moduleMenu
        const m0 = Array.isArray(menus) ? menus[0] : menus
        if (m0 && typeof m0.open === 'function') {
          m0.open(this.projectId, 0)
        }
      })
    },
    refreshRootMenu() {
      this.$nextTick(() => {
        const menus = this.getModuleMenus()
        const m0 = menus[0]
        if (m0 && typeof m0.open === 'function') {
          m0.open(this.projectId, 0)
        }
      })
    },
    loadModuleForProp(moduleId) {
      const id = Number(moduleId)
      if (!id) return
      const seq = ++this._moduleLoadSeq
      getModule(id).then((res) => {
        if (seq !== this._moduleLoadSeq || Number(this.moduleId) !== id) return
        if (!res || !res.data) return
        if (this.directMenu) {
          this.applyModuleDataWithoutClosingPopover(res.data)
        } else {
          this.selectModule = res.data
          this.syncModuleDisplay(res.data)
        }
      })
    },
    /** 表单打开/重置时与 v-model 强制对齐（含 moduleId 未变化的情况） */
    resetFromForm(moduleId) {
      ++this._moduleLoadSeq
      this.resetMenu()
      this.refreshRootMenu()
      if (moduleId) {
        this.loadModuleForProp(moduleId)
      } else {
        this.clearSelectModuleState(false)
      }
    },
    clearSelectModuleState(emitInput) {
      ++this._moduleLoadSeq
      this.setDisplaySearch(null)
      this.selectModule = null
      this.moduleFullPath = ''
      this.popoverVisible = false
      this.resetMenu()
      this.refreshRootMenu()
      this.$forceUpdate()
      if (emitInput) {
        this.$emit('input', null)
      }
    },
    reset() {
      this.refreshRootMenu()
      this.resetMenu()
    },
    /** 搜索成员事件 */
    searchChangeHandle() {
      this.$emit('input',this.selectModule.moduleId);
    },
    /** 弹窗显示事件 */
    popoverShowHandle() {
      if (this.directMenu) return
      this.activeColumnIndex = Math.max(0, this.activeModuleIds.length - 1);
      this.activeRowIndex = 0;
      this.activeRowPart = 'main';
      this.$nextTick(() => {
        this.attachMenuScrollerListener();
        this.fitModulePopoverLayout();
        this.clampActiveRowIndex();
        this.focusOuterShell();
        this.scrollMenuColumnIntoView();
        this.scrollActiveModuleIntoView();
      });
    },
    /** 弹窗隐藏事件 */
    popoverHideHandle() {
      if (this.directMenu) return
      this.detachMenuScrollerListener()
      this.resetMenu()
      this.refreshRootMenu()
      this.activeColumnIndex = 0
      this.activeRowIndex = -1
      this.activeRowPart = 'main'
      this.detachModulePopoverKeyboardListener()
    },
    /** 点击目录事件的处理 */
    clickDirectoryHandle(module, index) {
      let arr = [];
      for (let i = 0; i <= index; i++) {
        arr.push(this.activeModuleIds[i]);
      }
      if (this.activeModuleIds.length - 1 === index) {
        arr.push(module.moduleId);
      }
      arr[index + 1] = module.moduleId;
      this.activeModuleIds = arr;
      this.activeColumnIndex = index + 1;
      this.activeRowIndex = 0;
      this.activeRowPart = 'main';
      if (this._openedForAddSubColumnIndex != null && this.activeColumnIndex !== this._openedForAddSubColumnIndex) {
        this._openedForAddSubColumnIndex = null;
      }
      this.$nextTick(() => this.scrollMenuColumnIntoView());
    },
    /** 点击菜单事件的处理 */
    clickMenuHandle(module, index) {
      this.selectModule = module;
      const pathOverride = this.buildModulePathFromOpenColumns(module, index);
      this.syncModuleDisplay(module, pathOverride);
      this.popoverVisible = false;
      this.$emit('input', this.selectModule.moduleId, this.selectModule);
      this.resetMenu();
    },
    /** 点击菜单添加按钮的事件处理 */
    clickAddSubMenuHandle(module, index) {
      this.clickDirectoryHandle(module, index);
      this.activeColumnIndex = index + 1;
      this._openedForAddSubColumnIndex = index + 1;
      this.activeRowPart = 'main';
      this.waitForSubMenuColumnReady(this.activeColumnIndex, (menu) => {
        const n = (menu.moduleList || []).length;
        if (menu.isEdit) {
          this.activeRowIndex = n;
          this.activeRowPart = 'new';
          this.activateNewModuleRow(menu);
        } else {
          this.activeRowIndex = 0;
        }
        this.fitModulePopoverLayout();
        this.scrollMenuColumnIntoView();
        this.scrollActiveModuleIntoView();
      });
    },
    /** 等待子级菜单列加载完成 */
    waitForSubMenuColumnReady(columnIndex, callback, attempt = 0) {
      this.$nextTick(() => {
        const menus = this.getModuleMenus();
        const menu = menus[columnIndex];
        if (!menu) {
          if (attempt < 24) {
            setTimeout(() => this.waitForSubMenuColumnReady(columnIndex, callback, attempt + 1), 50);
          }
          return;
        }
        if (menu.loading) {
          if (attempt < 48) {
            setTimeout(() => this.waitForSubMenuColumnReady(columnIndex, callback, attempt + 1), 50);
          }
          return;
        }
        if (typeof callback === 'function') callback(menu);
      });
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
      this.clearSelectModuleState(!!event)
      if (event) event.stopPropagation()
    },
    resetMenu() {
      this.activeModuleIds = [0];
      this.activeColumnIndex = 0;
      this.activeRowIndex = -1;
      this.activeRowPart = 'main';
      this.menuLabelLimits = [];
      this._openedForAddSubColumnIndex = null;
      this.$forceUpdate();
    },
    /** 对外暴露的聚焦方法：聚焦外框（Tab 单停靠点） */
    focus() {
      const outer = this.$refs.inputBox;
      if (outer && typeof outer.focus === 'function') {
        outer.focus();
      }
    },
    /** Tab/快捷键聚焦外框时不转发到内部 readonly input */
    onOuterFocus(e) {
      if (e && e.type === 'focus' && !e.isTrusted) {
        return;
      }
    },
  }
}
</script>
<style>
  /* 须压过 .el-popover.el-popper（双类），否则自定义 padding 不生效，仍用主题默认 12px，且与箭头叠加后上下观感不一致 */
  /* 外层不可 overflow:hidden/auto，否则 Element 画在顶缘外的 .popper__arrow 会被裁掉，小三角不显示 */
  .el-popover.el-popper.select-module-popover {
    padding: 10px 0 !important;
    box-sizing: border-box;
    overflow: visible !important;
    display: flex;
    flex-direction: column;
    align-items: stretch;
    line-height: normal;
    width: auto;
    max-width: calc(100vw - 32px) !important;
    max-height: none;
  }
  .el-popover.el-popper.select-module-popover > .select-module-menu-outer {
    display: flex;
    flex-direction: row;
    align-items: stretch;
    box-sizing: border-box;
    max-width: 100%;
    overflow: hidden;
    position: relative;
    line-height: normal;
    margin: 0;
    padding: 0;
  }
  .el-popover.el-popper.select-module-popover .select-module-menu-prev {
    position: relative;
    flex-shrink: 0;
    align-self: stretch;
    left: auto;
    top: auto;
    bottom: auto;
    z-index: 3;
    min-width: 44px;
    width: auto;
    padding: 0 6px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    margin: 0;
    border: none;
    border-right: 1px solid var(--border-color-light, #e4e7ed);
    border-radius: 0;
    cursor: pointer;
    color: var(--text-color-secondary, #909399);
    background: linear-gradient(to right, var(--dropdown-bg, #fff) 50%, rgba(255, 255, 255, 0));
  }
  html.dark .el-popover.el-popper.select-module-popover .select-module-menu-prev {
    color: #a8abb2;
    background: linear-gradient(to right, var(--dropdown-bg, #252529) 50%, rgba(37, 37, 41, 0));
  }
  .el-popover.el-popper.select-module-popover .select-module-menu-prev:hover {
    color: var(--cat2bug-primary, #409eff);
  }
  .el-popover.el-popper.select-module-popover .select-module-menu-prev-level {
    font-size: 11px;
    line-height: 1.2;
    font-weight: 600;
    white-space: nowrap;
    user-select: none;
    padding: 2px 6px;
    border-radius: 10px;
    background: var(--bg-color-block, #f2f6fc);
    color: var(--text-color-regular, #606266);
  }
  html.dark .el-popover.el-popper.select-module-popover .select-module-menu-prev-level {
    background: #3a3a42;
    color: #e5eaf3;
  }
  .el-popover.el-popper.select-module-popover .select-module-menu-prev .el-icon-arrow-left {
    font-size: 14px;
    font-weight: bold;
  }
  .el-popover.el-popper.select-module-popover .select-module-menu {
    display: inline-flex;
    flex: 1;
    flex-direction: row;
    flex-wrap: nowrap;
    align-items: stretch;
    box-sizing: border-box;
    width: max-content;
    max-width: 100%;
    min-width: 0;
    max-height: min(80vh, 560px);
    min-height: 0;
    min-width: 0;
    overflow-x: auto;
    overflow-y: hidden;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: thin;
  }
  .el-popover.el-popper.select-module-popover .module-menu-column + .module-menu-column {
    border-left: 1px solid var(--border-color-light, #e4e7ed);
  }
  .el-popover.el-popper.select-module-popover .select-module-menu::-webkit-scrollbar {
    height: 8px;
  }
  .el-popover.el-popper.select-module-popover .select-module-menu::-webkit-scrollbar-thumb {
    background: rgba(144, 147, 153, 0.55);
    border-radius: 4px;
  }
  .el-popover.el-popper.select-module-popover .module-menu-add-footer {
    padding-top: 10px !important;
    overflow: visible;
  }
  .el-popover.el-popper.select-module-popover .module-menu-footer-divider {
    margin-bottom: 10px !important;
  }
  .el-popover.el-popper.select-module-popover .select-module-add-button {
    display: flex !important;
    align-items: center;
    justify-content: flex-start;
    width: 100% !important;
    min-height: calc(1.4em + 10px);
    padding: 5px 10px !important;
    margin: 0 !important;
    line-height: 1.4;
    box-sizing: border-box;
    border-radius: 5px;
  }
  .el-popover.el-popper.select-module-popover .select-module-add-root,
  .el-popover.el-popper.select-module-popover .select-module-add {
    width: 100%;
    box-sizing: border-box;
    overflow: visible;
  }
  .el-popover.el-popper.select-module-popover .module-menu > .el-col.module-menu-item.module-menu-row,
  .el-popover.el-popper.select-module-popover .module-menu-item.module-menu-row.is-keyboard-active,
  .el-popover.el-popper.select-module-popover .module-menu-item.module-menu-row.is-path-active,
  .el-popover.el-popper.select-module-popover .module-menu-item.module-menu-row:hover {
    padding-left: 10px !important;
    padding-right: 10px !important;
    box-sizing: border-box;
  }
  .el-popover.el-popper.select-module-popover .module-menu-scroll-btn {
    color: var(--text-color-secondary, #909399);
  }
  html.dark .el-popover.el-popper.select-module-popover .module-menu-scroll-btn {
    color: #a8abb2;
  }
  .el-popover.el-popper.select-module-popover .module-menu-scroll-btn:hover {
    color: var(--cat2bug-primary, #409eff);
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
    overflow: hidden;
    .selectProjectMemberInput_content {
      display: inline-flex;
      flex-direction: row;
      justify-content: flex-start;
      align-items: center;
      flex-wrap: nowrap;
      flex-grow: 1;
      flex-shrink: 1;
      min-width: 0;
      overflow: hidden;
      min-height: 30px;
      margin: 0px 10px 0px 0px;
      .select-module-display-input {
        flex-grow: 1;
        flex-shrink: 1;
        width: 0;
        min-width: 0;
        height: 26px;
        ::v-deep .el-input__inner:focus,
        ::v-deep .el-input__inner:focus-visible {
          outline: none !important;
          box-shadow: none !important;
          border-color: transparent !important;
        }
      }
      .el-input {
        flex-grow: 1;
        flex-shrink: 1;
        width: 0;
        min-width: 0;
        height: 26px;
        .el-input__inner {
          border-width: 0px;
          height: 26px;
          line-height: 26px;
          display: block;
          width: 100%;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
      .padding-left-8 input {
        padding-left: 8px;
      }
    }
    .select-module-input__prefix-icon {
      margin: 0 0 0 10px;
      flex-shrink: 0;
    }
    .select-module-input__icon {
      display: inline;
      color: var(--cat2bug-input-icon-color, #c0c4cc);
      font-size: 14px;
      transition: transform 0.3s, -webkit-transform 0.3s;
      -webkit-transform: rotateZ(180deg);
      transform: rotateZ(180deg);
      cursor: pointer;
    }
  }
  .select-module-input:focus,
  .select-module-input:focus-within {
    .select-module-input__icon {
      transform: rotateZ(0deg);
    }
  }
  ::v-deep .select-module-input .select-module-input__icon.is-open {
    transform: rotateZ(0deg);
  }
  .select-module-menu {
    justify-content: flex-start;
    padding: 0;
    .module-menu-column:first-child {
      border-left-width: 0;
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
