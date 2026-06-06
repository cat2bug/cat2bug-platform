<template>
  <el-popover
    popper-class="select-project-member-popover"
    placement="bottom-start"
    width="350"
    v-model="popoverVisible"
    @show="popoverShowHandle"
    @hide="popoverHideHandle"
    trigger="click">
    <template #reference>
    <div ref="inputBox" :style="inputBoxStyle" :class="'el-input__inner select-project-member-input cat2bug-combo-focus-target select-project-member-input-'+size" tabindex="0" @focus="onOuterFocus" @keydown="searchKeyDownHandle">
      <i :class="icon" v-if="icon" class="select-project-member-input__prefix-icon"></i>
      <div ref="tagContent" class="selectProjectMemberInput_content" :class="{'has-members': selectMembers.size > 0, 'is-search-active': searchInputActive}">
        <div ref="tagMeasure" class="select-project-member-tags-measure" aria-hidden="true">
          <el-tag
            class="select-project-member-tag"
            size="mini"
            closable
            v-for="member in selectMemberList()"
            :key="'measure-' + member.userId + '-' + (optionsChecks.get(member.userId) || 0)"
            :type="tagType(member)">
            <span class="select-project-member-tag__text">{{ member.nickName }}</span>
          </el-tag>
          <span ref="moreBadgeMeasure" class="select-project-member-tag-more-measure">+99</span>
        </div>
        <span ref="tagsRow" class="select-project-member-tags-row">
          <span ref="tagsWrap" class="select-project-member-tags">
            <el-tag
              ref="memberTags"
              class="select-project-member-tag"
              size="mini"
              v-for="(member, index) in selectMemberList()"
              :key="getMemberTagKey(member)"
              v-show="index < visibleTagCount"
              closable
              @close="clickMenuHandle(member,$event)"
              :type="tagType(member)">
              <span class="select-project-member-tag__text" :title="member.nickName">{{ member.nickName }}</span>
            </el-tag>
          </span>
          <span v-if="hiddenMemberCount > 0" class="select-project-member-tag-more" :title="hiddenMemberTitle">+{{ hiddenMemberCount }}</span>
        </span>
        <el-input ref="selectProjectMemberInput" class="select-project-member-search-input" :size="size" :placeholder="selectMembers.size>0?'':$t(placeholder)" v-model="queryMember.params.search" @input="searchChangeHandle" @focus.native="onSearchInputFocus" @blur.native="onSearchInputBlur" @keydown.native.stop="searchKeyDownHandle"></el-input>
      </div>
      <i class="select-project-member-input__icon el-icon-arrow-up" :class="{'is-open': popoverVisible}" v-show="isClearButtonVisible==false" @mouseenter="showClearButtonHandle(true)"></i>
      <i class="select-project-member-input__icon el-icon-circle-close" v-show="isClearButtonVisible==true" @mouseleave="showClearButtonHandle(false)" @click="clearSelectMembersHandle"></i>
    </div>
    </template>
    <el-tabs v-if="roleGroup" ref="tabs" class="select-project-member-tabs" v-model="queryMember.roleId" @tab-click="getMemberList">
      <el-tab-pane ref="tabAll" :label="$i18n.t('all')" name="0"></el-tab-pane>
      <el-tab-pane v-for="(role,roleIndex) in roleList" :key="roleIndex" :label="role.roleNameI18nKey?$i18n.t(role.roleNameI18nKey):role.roleName" :name="role.roleId+''"></el-tab-pane>
    </el-tabs>

    <el-row ref="memberMenu" v-if="options && options.length>0" class="select-project-member-menu">
      <el-col :span="24" v-for="(item,itemIndex) in options" :key="itemIndex" @click.native="clickMenuHandle(item,$event)" :active="optionsChecks.get(item.userId)?'true':'false'" :class="{'is-keyboard-active': itemIndex===activeIndex}">
        <member-nameplate :member="item"></member-nameplate>
        <el-tag v-if="isHead" size="mini"  @click.native="setMasterHandle($event,item)" :type="tagType(item)">
          {{optionsChecks.get(item.userId)===1?$i18n.t('head'):$i18n.t('assistant')}}
        </el-tag>
        <i v-else class="el-icon-check"></i>
      </el-col>
    </el-row>
    <el-empty v-else :description="$i18n.t('no-data')"></el-empty>
    <el-pagination
      small
      :hide-on-single-page="true"
      layout="prev, next"
      :current-page.sync="queryMember.pageNum"
      :page-size.sync="queryMember.pageSize"
      @current-change="currentPageChangeHandle"
      :total="total">
    </el-pagination>
  </el-popover>
</template>

<script>
import i18n from "@/utils/i18n/i18n";
import {listMemberOfProject, listProjectRole} from "@/api/system/project";
import MemberNameplate from "@/components/MemberNameplate"
import { observeComboFocusTarget } from '@/utils/combo-focus-tab'
export default {
  name: "SelectProjectMember",
  model: {
    prop: 'memberId',
    event: 'input'
  },
  components: { MemberNameplate },
  data() {
    return {
      // 查询的角色列表
      roleList: [],
      // 查询的成员列表
      options: [],
      // 列表中成员选择的优先级顺序
      optionsChecks: new Map(),
      // 当前成员id
      currentMemberId: null,
      // 当前选择的角色标签
      // 选择的成员
      selectMembers: new Map(),
      updateInt: 0,
      // 是否显示清除按钮
      isClearButtonVisible: false,
      // 是否显示成员下拉列表
      popoverVisible: false,
      // 键盘上下导航高亮的成员索引（-1 表示无）
      activeIndex: -1,
      // 单行展示：可见 tag 数量与折叠的剩余人数
      visibleTagCount: 1,
      hiddenMemberCount: 0,
      // 锁定输入框宽度，避免选中成员后撑开
      lockedInputWidth: 0,
      // 是否展开搜索 input（有成员时默认收起，避免占位与光标干扰 tag 展示）
      searchInputActive: false,
      // 成员查询参数
      queryMember: {
        pageNum: 1,
        pageSize: 10,
        roleId: null,
        params: {
          search: null
        }
      },
      // 成员总数
      total: 0
    }
  },
  props: {
    memberId: {
      type: Array,
      default: null,
    },
    isHead: {
      type: Boolean,
      default: true
    },
    projectId: {
      type: Number,
      default: null
    },
    roleId: {
      type: Number,
      default: null
    },
    multiple: {
      type: Boolean,
      default: true
    },
    roleGroup: {
      type: Boolean,
      default: true
    },
    placeholder: {
      type: String,
      default: 'member.please-select-member'
    },
    clearable: {
      type: Boolean,
      default: true
    },
    size: {
      type: String,
      default: 'medium'
    },
    icon: {
      type: String,
      default: null
    }
  },
  computed: {
    tagType: function () {
      return function (member) {
        if(this.isHead) {
          return this.optionsChecks.get(member.userId)===1?'':'success';
        } else {
          return 'info';
        }
      }
    },
    selectMemberList: function() {
      return function () {
        const arr = Array.from(this.selectMembers.values());
        return arr.sort((a, b) => (this.optionsChecks.get(a.userId) || 0) - (this.optionsChecks.get(b.userId) || 0));
      }
    },
    hiddenMemberTitle() {
      const list = this.selectMemberList();
      if (this.hiddenMemberCount <= 0) return '';
      return list.slice(this.visibleTagCount).map(m => m.nickName).join(', ');
    },
    inputBoxStyle() {
      if (!this.lockedInputWidth) return null;
      const w = this.lockedInputWidth + 'px';
      return { width: w, minWidth: w, maxWidth: w };
    }
  },
  watch: {
    popoverVisible(val) {
      this.scheduleTagLayout();
      if (val) {
        this.$_onPopoverEscKeydown = (e) => {
          if (e.key !== 'Escape') return;
          // 单独 Esc 关闭成员下拉；Esc 关闭抽屉由 defect-drawer-shortcuts 处理
          e.preventDefault();
          e.stopPropagation();
          if (this.popoverVisible) this.closePopoverKeepFocus();
        };
        document.addEventListener('keydown', this.$_onPopoverEscKeydown, true);
      } else {
        this.detachPopoverEscListener();
      }
    },
    memberId: function (newVal, oldVal) {
      if(newVal!=oldVal) {
        if (!newVal || newVal.length==0) {
          this.clear();
        } else if(this.selectMembers.size!=newVal.length || Array.from(this.selectMembers.keys()).filter(k=>newVal.includes(parseInt(k))).length!=newVal.length)  {
          this.clearSelectMembersHandle();
          this.getMemberList(newVal);
        }
      }
    },
  },
  created() {
    this.queryMember.roleId = this.roleId?this.roleId+'':'0';
    this.getRoleList();
    this.getMemberList(this.memberId);
  },
  mounted() {
    this.$nextTick(() => {
      this.lockInputWidth();
      this.scheduleTagLayout();
      const shell = this.$refs.inputBox;
      if (shell) {
        shell.__cat2bugOnTabAway = () => {
          if (this.popoverVisible) this.popoverVisible = false;
          this.searchInputActive = false;
        };
        observeComboFocusTarget(shell);
      }
    });
    if (typeof ResizeObserver !== 'undefined') {
      this.$_tagResizeObs = new ResizeObserver(() => this.scheduleTagLayout());
      this.$nextTick(() => {
        if (this.$refs.tagContent) {
          this.$_tagResizeObs.observe(this.$refs.tagContent);
        }
        if (this.$refs.tagsRow) {
          this.$_tagResizeObs.observe(this.$refs.tagsRow);
        }
      });
    }
  },
  beforeDestroy() {
    if (this.$_tagLayoutTimer) {
      clearTimeout(this.$_tagLayoutTimer);
      this.$_tagLayoutTimer = null;
    }
    if (this.$_tagResizeObs) {
      this.$_tagResizeObs.disconnect();
      this.$_tagResizeObs = null;
    }
    this.detachPopoverEscListener();
    const shell = this.$refs.inputBox;
    if (shell) shell.__cat2bugOnTabAway = null;
  },
  methods: {
    detachPopoverEscListener() {
      if (this.$_onPopoverEscKeydown) {
        document.removeEventListener('keydown', this.$_onPopoverEscKeydown, true);
        this.$_onPopoverEscKeydown = null;
      }
    },
    /** 负责人/协助顺序变化后刷新 input 区 tag 样式（Map 变更 Vue2 不自动追踪） */
    refreshMemberTags() {
      this.updateInt = (this.updateInt + 1) % 999;
      this.resetTagLayoutPessimistic();
      this.scheduleTagLayout();
    },
    getMemberTagKey(member) {
      const rank = this.optionsChecks.get(member.userId) || 0;
      return member.userId + '-' + rank + '-' + this.updateInt;
    },
    /** 关闭下拉并恢复输入框焦点（Esc / 避免 popover 关闭后焦点落到 body） */
    closePopoverKeepFocus() {
      if (!this.popoverVisible) return;
      this.popoverVisible = false;
      this.searchInputActive = false;
      this.$nextTick(() => {
        setTimeout(() => this.focusOuterShell(), 150);
      });
    },
    clear() {
      this.clearSelectMembersHandle();
    },
    /** 对外暴露：聚焦外框（快捷键/表单跳转用） */
    focus() {
      this.focusOuterShell();
    },
    focusOuterShell() {
      const outer = this.$refs.inputBox;
      if (outer && typeof outer.focus === 'function') {
        outer.focus();
      }
    },
    onOuterFocus(e) {
      if (e && e.type === 'focus' && !e.isTrusted) {
        this.searchInputActive = false;
        return;
      }
      if (this.searchInputActive) {
        this.forwardFocusToInput(e);
      }
    },
    onSearchInputFocus(e) {
      if (e && e.isTrusted) {
        this.searchInputActive = true;
        this.scheduleTagLayout();
      }
    },
    onSearchInputBlur() {
      this.$nextTick(() => {
        const comp = this.$refs.selectProjectMemberInput;
        const inner = comp && comp.$el && comp.$el.querySelector('input');
        const outer = this.$refs.inputBox;
        if (document.activeElement !== inner && document.activeElement !== outer) {
          this.searchInputActive = false;
          this.scheduleTagLayout();
        }
      });
    },
    /** 搜索态下把焦点传递到内部 input */
    forwardFocusToInput(e) {
      if (e && e.type === 'focus' && !e.isTrusted) {
        this.searchInputActive = false;
        return;
      }
      if (!this.searchInputActive) return;
      const focusInput = () => {
        const comp = this.$refs.selectProjectMemberInput;
        if (comp && comp.$el) {
          const inner = comp.$el.querySelector('input');
          if (inner && typeof inner.focus === 'function') {
            inner.focus();
            return;
          }
        }
        if (comp && typeof comp.focus === 'function') {
          comp.focus();
        }
      };
      this.$nextTick(focusInput);
      setTimeout(focusInput, 0);
      setTimeout(focusInput, 120);
    },
    /** 成员变更后先保守折叠，避免布局重算前 tag 与 +N 重叠 */
    resetTagLayoutPessimistic() {
      const total = this.selectMembers.size;
      if (total <= 0) {
        this.visibleTagCount = 0;
        this.hiddenMemberCount = 0;
        return;
      }
      this.visibleTagCount = 1;
      this.hiddenMemberCount = total - 1;
    },
    /** 成员 tag 变更或容器尺寸变化时，重新计算单行可见数量 */
    scheduleTagLayout() {
      if (this.$_tagLayoutTimer) clearTimeout(this.$_tagLayoutTimer);
      this.$_tagLayoutTimer = setTimeout(() => {
        this.$_tagLayoutTimer = null;
        this.lockInputWidth();
        this.$nextTick(() => this.recalcVisibleTags());
      }, 0);
    },
    /** 锁定输入框宽度；抽屉展开后变宽时允许更新，避免预算长期偏小 */
    lockInputWidth() {
      const box = this.$refs.inputBox;
      if (!box) return;
      const w = box.offsetWidth;
      if (w > 0 && (this.lockedInputWidth === 0 || w > this.lockedInputWidth)) {
        this.lockedInputWidth = w;
      }
    },
    /** tag 之间的 flex gap（与样式一致） */
    getTagGap() {
      return 6;
    },
    /** 标签行左侧内边距（offsetWidth 不含 padding） */
    getTagsLeading() {
      return 6;
    },
    /** 搜索框占用宽度 */
    getSearchInputWidth() {
      if (this.selectMembers.size > 0 && !this.searchInputActive) return 0;
      if (this.selectMembers.size > 0) return 44;
      const searchEl = this.$refs.selectProjectMemberInput && this.$refs.selectProjectMemberInput.$el;
      return searchEl && searchEl.offsetWidth > 0 ? Math.min(searchEl.offsetWidth, 44) : 28;
    },
    /** 标签行右边界（tags-row + +N 不可超过此线） */
    getTagsAllowedRight() {
      const content = this.$refs.tagContent;
      if (!content) return 0;
      const rect = content.getBoundingClientRect();
      return rect.right - this.getSearchInputWidth() - 4;
    },
    /** 标签行可用宽度（优先用 tags-row 实际宽度，与 flex 布局一致） */
    getTagsBudget() {
      const row = this.$refs.tagsRow;
      if (row && row.clientWidth > 0) return row.clientWidth;
      const content = this.$refs.tagContent;
      if (!content || content.clientWidth <= 0) return 0;
      return Math.max(0, content.clientWidth - this.getSearchInputWidth() - 4);
    },
    measureMoreBadgeWidth(count) {
      if (count <= 0) return 0;
      const probe = this.$refs.moreBadgeMeasure;
      if (probe) {
        const prev = probe.textContent;
        probe.textContent = '+' + count;
        const w = probe.getBoundingClientRect().width || probe.offsetWidth;
        probe.textContent = prev;
        if (w > 0) return w;
      }
      return 28 + String(count).length * 8;
    },
    /** 从离屏测量区读取 tag 宽度（不受 overflow 裁切影响） */
    readMeasureTagWidths(expectedCount) {
      const measure = this.$refs.tagMeasure;
      if (!measure) return null;
      const nodes = measure.querySelectorAll('.select-project-member-tag');
      if (nodes.length !== expectedCount) return null;
      const widths = Array.from(nodes).map(el => el.getBoundingClientRect().width || el.offsetWidth);
      if (widths.some(w => w <= 0)) return null;
      return widths;
    },
    /** 渲染后修正：禁止 tag 互相重叠、或与 +N 重叠、或被容器裁切 */
    trimPartialVisibleTags(total, depth = 0) {
      if (depth > total + 2) return;
      const allowedRight = this.getTagsAllowedRight();
      const wrap = this.$refs.tagsWrap;
      const row = this.$refs.tagsRow;
      if (!wrap || !row || allowedRight <= 0) return;

      const tags = Array.from(wrap.querySelectorAll('.select-project-member-tag')).filter(el => el.offsetWidth > 0);
      const moreEl = row.querySelector('.select-project-member-tag-more');
      const rowGap = this.getTagGap();
      let reduceTo = -1;

      if (moreEl && moreEl.offsetWidth > 0 && tags.length > 0) {
        const moreLeft = moreEl.getBoundingClientRect().left;
        for (let i = 0; i < tags.length; i++) {
          if (tags[i].getBoundingClientRect().right > moreLeft - rowGap + 0.5) {
            reduceTo = Math.max(1, i);
            break;
          }
        }
      }

      if (reduceTo < 0) {
        for (let i = 1; i < tags.length; i++) {
          const prevRight = tags[i - 1].getBoundingClientRect().right;
          const curLeft = tags[i].getBoundingClientRect().left;
          if (curLeft < prevRight - 0.5) {
            reduceTo = Math.max(1, i);
            break;
          }
        }
      }

      if (reduceTo < 0) {
        for (let i = 0; i < tags.length; i++) {
          if (tags[i].getBoundingClientRect().right > allowedRight + 0.5) {
            reduceTo = Math.max(1, i);
            break;
          }
        }
      }

      const moreClipped = moreEl && moreEl.offsetWidth > 0 && moreEl.getBoundingClientRect().right > allowedRight + 0.5;
      if (reduceTo < 0 && moreClipped && this.visibleTagCount > 1) {
        reduceTo = this.visibleTagCount - 1;
      }

      if (reduceTo >= 0 && reduceTo !== this.visibleTagCount) {
        this.visibleTagCount = reduceTo;
        this.hiddenMemberCount = total - reduceTo;
        this.$nextTick(() => this.trimPartialVisibleTags(total, depth + 1));
      }
    },
    /** 计算展示 count 个 tag（及可选 +N）所需总宽度 */
    measureTagsRowWidth(widths, visibleCount, hiddenCount) {
      const gap = this.getTagGap();
      const leading = this.getTagsLeading();
      let sum = leading;
      for (let i = 0; i < visibleCount && i < widths.length; i++) {
        sum += widths[i];
        if (i < visibleCount - 1) sum += gap;
      }
      if (hiddenCount > 0) {
        sum += gap + this.measureMoreBadgeWidth(hiddenCount);
      }
      return sum;
    },
    applyVisibleTagLayout(total, widths, tagsBudget) {
      const budget = Math.max(0, tagsBudget - 2);
      let bestVisible = 1;
      for (let k = total; k >= 1; k--) {
        const hidden = total - k;
        if (this.measureTagsRowWidth(widths, k, hidden) <= budget) {
          bestVisible = k;
          break;
        }
      }
      bestVisible = Math.max(1, Math.min(bestVisible, total));
      this.visibleTagCount = bestVisible;
      this.hiddenMemberCount = total - bestVisible;
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          this.trimPartialVisibleTags(total);
        });
      });
    },
    recalcVisibleTags(retry = 0) {
      const members = this.selectMemberList();
      const total = members.length;
      if (total === 0) {
        this.visibleTagCount = 0;
        this.hiddenMemberCount = 0;
        return;
      }
      this.lockInputWidth();
      const tagsBudget = this.getTagsBudget();
      if (tagsBudget <= 0 && retry < 8) {
        setTimeout(() => this.recalcVisibleTags(retry + 1), 50);
        return;
      }
      this.$nextTick(() => {
        const widths = this.readMeasureTagWidths(total);
        if (!widths) {
          if (retry < 12) {
            setTimeout(() => this.recalcVisibleTags(retry + 1), 30);
          }
          return;
        }
        this.applyVisibleTagLayout(total, widths, tagsBudget);
      });
    },
    /** 搜索成员事件 */
    searchChangeHandle() {
      this.searchInputActive = true;
      this.popoverVisible = true;
      this.getMemberList();
    },
    /** 输入字符事件（外框与内部 input 共用） */
    searchKeyDownHandle(e) {
      const key = e.key;
      const outer = this.$refs.inputBox;
      const onOuter = outer && document.activeElement === outer;
      if (
        onOuter &&
        key.length === 1 &&
        !e.metaKey &&
        !e.ctrlKey &&
        !e.altKey &&
        key !== ' '
      ) {
        this.searchInputActive = true;
        this.popoverVisible = true;
        const cur = this.queryMember.params.search || '';
        this.queryMember.params.search = cur + key;
        this.getMemberList();
        this.$nextTick(() => this.forwardFocusToInput());
        e.preventDefault();
        return;
      }
      if (!this.popoverVisible) {
        // 下拉未展开：上/下/回车 → 展开（并阻止默认滚动，不滚动外层抽屉滚动条）
        if (key === 'ArrowDown' || key === 'ArrowUp' || key === 'Enter') {
          e.preventDefault();
          this.popoverVisible = true;
          return;
        }
      } else {
        // 下拉已展开：上/下 选择成员列表，左/右 切换角色 tab，回车 选中，Esc 关闭
        if (key === 'ArrowDown') { e.preventDefault(); this.moveActive(1); return; }
        if (key === 'ArrowUp') { e.preventDefault(); this.moveActive(-1); return; }
        if (key === 'ArrowLeft') { e.preventDefault(); this.moveTab(-1); return; }
        if (key === 'ArrowRight') { e.preventDefault(); this.moveTab(1); return; }
        if (key === 'Enter' || key === ' ' || key === 'Spacebar' || e.code === 'Space') { e.preventDefault(); this.selectActive(); return; }
        if (key === 'Escape') {
          if (!e.metaKey && !e.ctrlKey &&
            !(typeof e.getModifierState === 'function' &&
              (e.getModifierState('Meta') || e.getModifierState('Control')))) {
            e.preventDefault();
            this.closePopoverKeepFocus();
          }
          return;
        }
      }
      // Tab：关闭下拉，交回浏览器原生 Tab 切换到下一个字段（不阻止默认）
      if (key === 'Tab') {
        if (this.popoverVisible) {
          this.popoverVisible = false;
        }
        return;
      }
      // 如果按了退格键，并且input中没有字符了，就删除最后一个选中的成员
      if (e.keyCode === 8 && !this.queryMember.params.search) {
        e.preventDefault();
        let maxIndexId = null;
        let maxIndexIndex = 0;
        for (let [k, value] of this.optionsChecks) {
          if (maxIndexIndex < value) {
            maxIndexIndex = value;
            maxIndexId = k;
          }
        }
        if (maxIndexId) {
          this.optionsChecks.set(maxIndexId, 0);
          this.selectMembers.delete(maxIndexId);
          this.updateMembers();
          this.$forceUpdate();
          // 删除后（含删光最后一个）保持输入框焦点，避免焦点环消失
          this.searchInputActive = false;
          this.$nextTick(() => this.focusOuterShell());
        }
      }
    },
    /** 查询角色集合 */
    getRoleList() {
      listProjectRole(this.projectId).then(res=>{
        this.roleList = res.rows;
      });
    },
    /** 查询成员集合 */
    getMemberList(memberIds) {
      this.queryMember.projectId = this.projectId;
      return listMemberOfProject(this.projectId, this.queryMember).then(res => {
        res.rows.forEach(m => {
          if (!this.optionsChecks.get(m.userId)) {
            this.optionsChecks.set(m.userId, 0);
          }
        });
        this.options = res.rows;
        this.total = res.total;
        if (memberIds && memberIds.length > 0) {
          this.options.forEach(m => {
            if (memberIds.includes(m.userId)) {
              this.clickMenuHandle(m);
            }
          });
        }
        this.$forceUpdate();
        this.scheduleTagLayout();
        return res;
      });
    },
    /** 选择成员变化的处理 */
    selectMemberChangeHandle(id) {
      this.currentMemberId=id;
    },
    /** 弹窗显示事件 */
    popoverShowHandle() {
      // 展开时默认高亮第一个成员，便于键盘上下选择
      this.activeIndex = 0;
      // 设置打开时tab下面的横条宽度
      this.$nextTick(_ => this.updateTabBar());
      this.getMemberList();
      this.$nextTick(() => this.focusOuterShell());
    },
    /** 更新角色 tab 下方高亮横条的位置与宽度 */
    updateTabBar() {
      if (this.$refs.tabs && this.$refs.tabs.$el) {
        const activeTab = this.$refs.tabs.$el.querySelector('.el-tabs__item.is-active');
        const activeBar = this.$refs.tabs.$el.querySelector('.el-tabs__active-bar');
        if (activeTab && activeBar) {
          activeBar.style.width = activeTab.clientWidth - 16 + 'px';
          activeBar.style.transform = `translateX(${activeTab.offsetLeft + 8}px)`;
        }
      }
    },
    hasPrevMemberPage() {
      return this.queryMember.pageNum > 1;
    },
    hasNextMemberPage() {
      const pageSize = this.queryMember.pageSize || 10;
      return this.queryMember.pageNum * pageSize < this.total;
    },
    /** 键盘翻页后定位高亮项 */
    goMemberPage(pageNum, activePos) {
      this.queryMember.pageNum = pageNum;
      return this.getMemberList().then(() => {
        if (activePos === 'last') {
          this.activeIndex = Math.max(0, this.options.length - 1);
        } else {
          this.activeIndex = 0;
        }
        this.$nextTick(() => this.scrollActiveIntoView());
      });
    },
    /** 键盘上/下移动成员列表高亮；首尾项再按上/下则翻页 */
    moveActive(dir) {
      if (!this.options || this.options.length === 0) {
        if (dir > 0 && this.hasNextMemberPage()) {
          this.goMemberPage(this.queryMember.pageNum + 1, 0);
        } else if (dir < 0 && this.hasPrevMemberPage()) {
          this.goMemberPage(this.queryMember.pageNum - 1, 'last');
        }
        return;
      }
      let i = this.activeIndex;
      if (i < 0) i = 0;
      i += dir;
      if (i < 0) {
        if (this.hasPrevMemberPage()) {
          this.goMemberPage(this.queryMember.pageNum - 1, 'last');
        } else if (this.activeIndex <= 0) {
          this.closePopoverKeepFocus();
        } else {
          this.activeIndex = 0;
        }
        return;
      }
      if (i > this.options.length - 1) {
        if (this.hasNextMemberPage()) {
          this.goMemberPage(this.queryMember.pageNum + 1, 0);
        } else {
          this.activeIndex = this.options.length - 1;
        }
        return;
      }
      this.activeIndex = i;
      this.$nextTick(() => this.scrollActiveIntoView());
    },
    /** 让当前高亮的成员行滚动到可视区域 */
    scrollActiveIntoView() {
      const menu = this.$refs.memberMenu && this.$refs.memberMenu.$el;
      if (!menu) return;
      const cols = menu.querySelectorAll('.el-col');
      const el = cols[this.activeIndex];
      if (el && typeof el.scrollIntoView === 'function') {
        el.scrollIntoView({ block: 'nearest' });
      }
    },
    /** 键盘左/右切换角色 tab（循环） */
    moveTab(dir) {
      if (!this.roleGroup) return;
      const names = ['0'];
      this.roleList.forEach(r => names.push(r.roleId + ''));
      if (names.length <= 1) return;
      let idx = names.indexOf(this.queryMember.roleId + '');
      if (idx < 0) idx = 0;
      idx = (idx + dir + names.length) % names.length;
      this.queryMember.roleId = names[idx];
      this.queryMember.pageNum = 1;
      this.activeIndex = 0;
      this.getMemberList();
      this.$nextTick(() => this.updateTabBar());
    },
    /** 回车选中当前高亮的成员 */
    selectActive() {
      if (this.activeIndex >= 0 && this.activeIndex < this.options.length) {
        this.clickMenuHandle(this.options[this.activeIndex], true);
      }
    },
    /** 弹窗隐藏事件 */
    popoverHideHandle() {
      this.activeIndex = -1;
      this.searchInputActive = false;
      this.scheduleTagLayout();
      this.setMemberIdEvent();
    },
    /** 设置主负责人：重排 rank 并同步 v-model 顺序（负责人始终在首位） */
    setMasterHandle(event, member) {
      const newHeadRank = this.optionsChecks.get(member.userId);
      if (!newHeadRank) {
        if (event) event.stopPropagation();
        return;
      }
      if (newHeadRank === 1) {
        if (event) event.stopPropagation();
        return;
      }
      const others = this.selectMemberList().filter(m => m.userId !== member.userId);
      this.optionsChecks.set(member.userId, 1);
      others.forEach((m, index) => {
        this.optionsChecks.set(m.userId, index + 2);
      });
      this.refreshMemberTags();
      this.setMemberIdEvent();
      this.$forceUpdate();
      if (event) {
        event.stopPropagation();
      }
    },
    /** 设置菜单索引顺序 */
    clickMenuHandle(member,event){
      if(this.optionsChecks.get(member.userId)){
        for (let [key, value] of this.optionsChecks) {
          if(value>this.optionsChecks.get(member.userId)) {
            this.optionsChecks.set(key,value-1);
          }
        }
        this.optionsChecks.set(member.userId,0);
        this.selectMembers.delete(member.userId);
      } else {
        let count = 0;
        for (let [key, value] of this.optionsChecks) {
          if(value>0) {
            count++;
          }
        }
        this.optionsChecks.set(member.userId,count+1);
        this.selectMembers.set(member.userId,member);
      }
      if (event) {
        this.updateMembers();
        this.refreshMemberTags();
        this.$forceUpdate();
      } else {
        this.scheduleTagLayout();
      }
    },
    updateMembers(){
      this.setMemberIdEvent();
      this.resetTagLayoutPessimistic();
      this.scheduleTagLayout();
    },
    /** 显示或隐藏清除按钮 */
    showClearButtonHandle(visible) {
      if(this.clearable==false) return;
      if(visible && (this.selectMembers.size>0 || this.queryMember.params.search)) {
        this.isClearButtonVisible = true;
      } else {
        this.isClearButtonVisible = false;
      }
    },
    /** 清除选择的成员 */
    clearSelectMembersHandle(event) {
      this.optionsChecks.clear();
      this.selectMembers.clear();
      this.updateInt = (this.updateInt+1)%999;
      this.queryMember.roleId = this.roleId?this.roleId+'':'0';
      this.queryMember.params.search=null;
      this.queryMember.pageNum=1;
      this.popoverVisible = false;

      if(event) {
        this.updateMembers();
        this.getMemberList();
        event.stopPropagation();
      } else {
        this.scheduleTagLayout();
      }

      this.$forceUpdate();
    },
    /** 翻页处理 */
    currentPageChangeHandle(val){
      this.queryMember.pageNum = val;
      this.getMemberList();
    },
    setMemberIdEvent() {
      let arr = Array.from(this.selectMembers.values());
      arr = arr.sort((a, b) => (this.optionsChecks.get(a.userId) || 0) - (this.optionsChecks.get(b.userId) || 0));
      let values = [];
      for(let i in arr){
        values.push(arr[i].userId);
      }
      this.$emit('input', values);
    }
  }
}
</script>
<style>
  .select-project-member-tabs .el-tabs__nav-prev, .select-project-member-tabs .el-tabs__nav-next {
    line-height: 27px;
  }
</style>
<style lang="scss" scoped>
  ::v-deep .el-popover__reference-wrapper {
    display: block;
    max-width: 100%;
    min-width: 0;
  }
  ::v-deep .select-project-member-input {
    display: inline-flex;
    flex-direction: row;
    max-width: 300px;
    width: 100%;
    min-width: 0;
    flex: 0 0 auto;
    box-sizing: border-box;
    line-height: 1;
    align-items: center;
    padding-top: 0;
    padding-bottom: 0;
    padding-left: 5px;
    vertical-align: middle;
    .selectProjectMemberInput_content {
      position: relative;
      display: inline-flex;
      flex-direction: row;
      align-items: center;
      flex-wrap: nowrap;
      flex-grow: 1;
      min-width: 0;
      overflow: hidden;
      min-height: 0;
      height: 100%;
      padding: 0 6px 0 0;
      gap: 4px;
      .select-project-member-tags-measure {
        position: fixed;
        left: -10000px;
        top: 0;
        visibility: hidden;
        pointer-events: none;
        z-index: -1;
        display: inline-flex;
        flex-wrap: nowrap;
        white-space: nowrap;
        gap: 6px;
        padding-left: 6px;
      }
      .select-project-member-tags-row {
        display: inline-flex;
        flex-direction: row;
        align-items: center;
        flex: 1 1 auto;
        min-width: 0;
        max-width: 100%;
        overflow: hidden;
        gap: 6px;
      }
      .select-project-member-tags {
        display: inline-flex;
        flex-direction: row;
        align-items: center;
        flex-wrap: nowrap;
        flex: 1 1 auto;
        min-width: 0;
        max-width: 100%;
        overflow: hidden;
        gap: 6px;
        padding-left: 6px;
      }
      ::v-deep .select-project-member-tag.el-tag {
        margin: 0;
        flex-shrink: 0;
        max-width: 96px;
        overflow: visible;
        display: inline-flex;
        align-items: center;
        justify-content: flex-start;
        gap: 2px;
        height: 20px;
        line-height: 20px;
        padding: 0 4px 0 6px;
        box-sizing: border-box;
        vertical-align: middle;
        font-size: 12px;
        .el-tag__close,
        .el-icon-close {
          flex-shrink: 0;
          position: static;
          top: auto !important;
          right: auto !important;
          margin: 0 0 0 2px !important;
          transform: none !important;
          width: 14px;
          height: 14px;
          line-height: 14px;
          font-size: 12px;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          &::before {
            display: block;
            line-height: 14px;
          }
        }
      }
      .select-project-member-tag-more-measure {
        display: inline-block;
        flex-shrink: 0;
        margin: 0;
        padding: 0 6px;
        min-width: 28px;
        font-size: 12px;
        line-height: 20px;
        white-space: nowrap;
      }
      .select-project-member-tag__text {
        display: inline-flex;
        align-items: center;
        flex: 1 1 auto;
        min-width: 0;
        max-width: 56px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        line-height: 20px;
        height: 20px;
        padding: 0;
      }
      .select-project-member-tag-more {
        flex-shrink: 0;
        margin: 0;
        padding: 0 6px;
        min-width: 28px;
        font-size: 12px;
        line-height: 20px;
        color: var(--text-color-secondary, #909399);
        background: var(--bg-color-block, #f4f4f5);
        border-radius: 4px;
        white-space: nowrap;
        overflow: visible;
        @at-root html.dark & {
          background: var(--table-row-hover-bg, #2d2d31);
          color: var(--text-color-regular, #e5eaf3);
        }
      }
      .select-project-member-search-input {
        flex: 1 1 auto;
        min-width: 28px;
        min-height: 0;
        height: 100%;
        ::v-deep .el-input__inner:focus,
        ::v-deep .el-input__inner:focus-visible {
          outline: none !important;
          box-shadow: none !important;
          border-color: transparent !important;
        }
      }
      &.has-members:not(.is-search-active) .select-project-member-search-input {
        flex: 0 0 0;
        width: 0;
        min-width: 0;
        max-width: 0;
        overflow: hidden;
        opacity: 0;
        pointer-events: none;
        ::v-deep .el-input__inner {
          padding: 0;
          width: 0;
          caret-color: transparent;
        }
      }
      &.has-members.is-search-active .select-project-member-search-input {
        flex: 0 0 44px;
        width: 44px;
        min-width: 44px;
        max-width: 44px;
        opacity: 1;
        pointer-events: auto;
      }
      .el-input {
        flex: 1 1 auto;
        min-width: 0;
        min-height: 0;
        height: 100%;
        .el-input__inner {
          padding-left: 10px;
          padding-right: 0px;
          padding-top: 0;
          padding-bottom: 0;
          border-width: 0px;
          height: 100%;
          min-height: 0;
          line-height: 1;
          display: block;
        }
      }
    }
    .select-project-member-input__prefix-icon {
      margin: 0 0 0 5px;
      flex-shrink: 0;
    }
    .select-project-member-input__icon {
      display: inline;
      color: var(--cat2bug-input-icon-color, #c0c4cc);
      font-size: 14px;
      transition: transform 0.3s, -webkit-transform 0.3s;
      -webkit-transform: rotateZ(180deg);
      transform: rotateZ(180deg);
      cursor: pointer;
    }
  }
  ::v-deep .select-project-member-input-medium {
    height: 36px;
    min-height: 36px;
    max-height: 36px;
  }

  ::v-deep .select-project-member-input-small {
    height: 32px;
    min-height: 32px;
    max-height: 32px;
  }
  ::v-deep .select-project-member-input-mini {
    height: 28px;
    min-height: 28px;
    max-height: 28px;
  }

  /* 下拉三角仅在下拉展开时朝上；获取焦点本身不翻转（保持朝下） */
  ::v-deep .select-project-member-input .select-project-member-input__icon.is-open {
    -webkit-transform: rotateZ(0deg);
    transform: rotateZ(0deg);
  }
  .select-project-member-menu {
    .el-col {
      display: inline-flex;
      flex-shrink: 0;
      justify-content: space-between;
      align-items: center;
      i {
        margin-right: 15px;
      }
      .el-tag {
        margin-right: 5px;
      }
      .el-tag, i {
        display: none;
      }
    }
    .el-col[active='true'] {
      .el-tag, i {
        display: inline-block;
      }
      ::v-deep .member-nameplate-content p, i {
        color: #409eff;
      }
    }
    .el-col:hover {
      background-color: #F2F6FC;
      border-radius: 5px;
      cursor: pointer;
      @at-root html.dark & {
        background-color: var(--table-row-hover-bg, #2d2d31);
      }
    }
    /* 键盘上下导航高亮 */
    .el-col.is-keyboard-active {
      background-color: #ECF5FF;
      border-radius: 5px;
      cursor: pointer;
      @at-root html.dark & {
        background-color: #3a3a42;
      }
    }
  }
  ::v-deep .el-tabs__item {
    height: 27px !important;
    line-height: 27px !important;
    font-size: 12px;
    padding: 0 8px !important;
  }
  ::v-deep .el-tabs__nav-next, .el-tabs__nav-prev {
    line-height: 30px;
  }
  .el-pagination {
    float: right;
  }
  ::v-deep .el-popover__reference-wrapper {
    display: inline-flex;
    align-items: center;
    vertical-align: middle;
    line-height: 0;
  }
</style>
