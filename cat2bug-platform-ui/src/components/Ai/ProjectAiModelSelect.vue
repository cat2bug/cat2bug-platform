<template>
  <div
    v-if="projectId"
    class="c2b-model-select-wrap"
    :class="{ 'c2b-model-select-wrap--combo': embedAiButton }"
  >
    <el-tooltip
      v-if="embedAiButton"
      class="item c2b-model-select__robot-tooltip"
      effect="dark"
      :content="aiTooltip"
      placement="top"
    >
      <el-button
        native-type="button"
        type="text"
        class="c2b-model-select__robot"
        :handle="aiLoading ? 'true' : 'false'"
        :disabled="aiLoading"
        @click.stop="onAiClick"
      >
        <svg-icon icon-class="robot" />
        <span v-show="aiLoading">分析中...</span>
      </el-button>
    </el-tooltip>
    <div
      class="c2b-model-select__trigger-shell"
      :class="{ 'c2b-model-select__trigger-shell--combo': embedAiButton }"
    >
      <el-select
        ref="elModelSelect"
        class="c2b-model-select"
        :class="{ 'c2b-model-select--in-combo': embedAiButton }"
        v-model="innerModelId"
        size="mini"
        popper-class="c2b-model-select-dropdown"
        placeholder=""
        :loading="loading"
        @visible-change="onSelectVisibleChange"
        @change="onChange"
      >
        <el-option-group
          v-for="group in groups"
          :key="group.key"
          :label="group.label"
        >
          <el-option
            v-for="item in group.options"
            :key="group.key + '-' + item.key"
            :label="modelSelectInputLabel"
            :value="item.key"
          >
            <span class="c2b-model-select-option__cell">
              <i
                class="el-icon-check c2b-model-select-option__tick"
                :class="{ 'c2b-model-select-option__tick--on': keyEquals(innerModelId, item.key) }"
                aria-hidden="true"
              />
              <span class="c2b-model-select-option__name">{{ item.label }}</span>
            </span>
          </el-option>
        </el-option-group>
        <template slot="empty">
          <div v-if="loading" class="c2b-model-select-dropdown__loading">
            <i class="el-icon-loading c2b-model-select-dropdown__loading-icon"></i>
            <span class="c2b-model-select-dropdown__loading-text">{{ loadingHintText }}</span>
          </div>
          <p v-else class="el-select-dropdown__empty">{{ noDataHintText }}</p>
        </template>
      </el-select>
    </div>
  </div>
</template>

<script>
import { projectAiModelOptions } from '@/api/system/ai';

const DEFECT_AI_MODEL_ID_PREFIX = 'defect_ai_model_id_';
/** 收起态输入框不展示模型名；下拉列表用 option 默认插槽展示 label */
const MODEL_SELECT_INPUT_LABEL = '\u200b';

export default {
  name: 'ProjectAiModelSelect',
  props: {
    projectId: {
      type: Number,
      default: null
    },
    value: {
      type: String,
      default: null
    },
    /** 为 true 时在下拉框左侧内嵌机器人（AI）按钮 */
    embedAiButton: {
      type: Boolean,
      default: false
    },
    aiLoading: {
      type: Boolean,
      default: false
    },
    aiTooltip: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      loading: false,
      innerModelId: this.value,
      modelSelectInputLabel: MODEL_SELECT_INPUT_LABEL,
      _stripComboT: null,
      groups: [
        { key: 'ollama', label: this.$i18n.t('project.ai-model-manager'), options: [] },
        { key: 'openai', label: this.$i18n.t('case.openai-service'), options: [] }
      ]
    };
  },
  computed: {
    loadingHintText() {
      const k = 'el.select.loading';
      const t = this.$t(k);
      return typeof t === 'string' && t && t !== k ? t : '加载中';
    },
    noDataHintText() {
      const k = 'el.select.noData';
      const t = this.$t(k);
      return typeof t === 'string' && t && t !== k ? t : '无数据';
    }
  },
  watch: {
    innerModelId() {
      this.scheduleStripComboWidth();
    },
    value(val) {
      this.innerModelId = val;
    },
    projectId() {
      this.reload();
    },
    '$i18n.locale'() {
      this.groups[0].label = this.$i18n.t('project.ai-model-manager');
      this.groups[1].label = this.$i18n.t('case.openai-service');
    },
    loading(val) {
      if (!val) {
        this.scheduleStripComboWidth();
      }
    }
  },
  mounted() {
    this.reload();
    this.scheduleStripComboWidth();
  },
  updated() {
    this.scheduleStripComboWidth();
  },
  beforeDestroy() {
    if (this._stripComboT != null) {
      clearTimeout(this._stripComboT);
      this._stripComboT = null;
    }
  },
  methods: {
    scheduleStripComboWidth() {
      if (!this.embedAiButton) {
        return;
      }
      if (this._stripComboT != null) {
        clearTimeout(this._stripComboT);
      }
      this._stripComboT = setTimeout(() => {
        this._stripComboT = null;
        this.$nextTick(() => {
          requestAnimationFrame(() => {
            this.stripComboSelectInlineWidth();
            requestAnimationFrame(() => this.stripComboSelectInlineWidth());
          });
        });
      }, 0);
    },
    onAiClick() {
      this.$emit('ai-click');
    },
    /** 组合模式：Popper 锚在 el-input 上偏右，将下拉整体平移使水平中心与机器人按钮中心对齐 */
    onSelectVisibleChange(visible) {
      if (!this.embedAiButton) {
        return;
      }
      const selectVm = this.$refs.elModelSelect;
      if (!selectVm || !selectVm.$refs.popper || !selectVm.$refs.popper.$el) {
        return;
      }
      const popperVm = selectVm.$refs.popper;
      const popperEl = popperVm.$el;
      if (!visible) {
        popperEl.style.marginLeft = '';
        return;
      }
      popperEl.style.marginLeft = '';
      this.$nextTick(() => {
        if (typeof popperVm.updatePopper === 'function') {
          popperVm.updatePopper();
        }
        this.$nextTick(() => {
          requestAnimationFrame(() => {
            requestAnimationFrame(() => {
              const robot =
                (this.$el && this.$el.querySelector('.c2b-model-select-wrap--combo .c2b-model-select__robot .svg-icon')) ||
                (this.$el && this.$el.querySelector('.c2b-model-select-wrap--combo .c2b-model-select__robot'));
              if (!robot) {
                return;
              }
              const pr = popperEl.getBoundingClientRect();
              const rr = robot.getBoundingClientRect();
              const robotCx = rr.left + rr.width / 2;
              const popperCx = pr.left + pr.width / 2;
              const adjust = robotCx - popperCx;
              if (Math.abs(adjust) > 0.5) {
                popperEl.style.marginLeft = `${adjust}px`;
              }
            });
          });
        });
      });
    },
    /** Element 会在 el-input__inner 上写内联 min-width；壳宽改由 flex 均分，不再写死像素 */
    stripComboSelectInlineWidth() {
      if (!this.embedAiButton || !this.$el) {
        return;
      }
      const root = this.$el.querySelector('.c2b-model-select__trigger-shell--combo');
      if (!root) {
        return;
      }
      root.style.removeProperty('width');
      root.style.removeProperty('max-width');
      root.style.removeProperty('flex');
      root.style.removeProperty('overflow');

      const sel = root.querySelector('.el-select');
      if (sel) {
        sel.style.setProperty('min-width', '0', 'important');
        sel.style.setProperty('width', '100%', 'important');
        sel.style.setProperty('max-width', '100%', 'important');
        sel.style.removeProperty('overflow');
      }
      const inp = root.querySelector('.el-input');
      if (inp) {
        inp.style.setProperty('min-width', '0', 'important');
        inp.style.setProperty('width', '100%', 'important');
        inp.style.setProperty('max-width', '100%', 'important');
        inp.style.removeProperty('overflow');
      }
      const inner = root.querySelector('.el-input__inner');
      if (inner) {
        inner.style.setProperty('min-width', '0', 'important');
        inner.style.setProperty('width', '100%', 'important');
        inner.style.setProperty('max-width', '100%', 'important');
      }
    },
    storageKey() {
      const pid = this.projectId;
      return DEFECT_AI_MODEL_ID_PREFIX + (Number.isFinite(pid) ? pid : '0');
    },
    getStoredModelId() {
      const raw = this.$cache.local.get(this.storageKey());
      if (raw === undefined || raw === null || raw === '') {
        return null;
      }
      return String(raw);
    },
    persistModelId(modelId) {
      if (modelId === null || modelId === undefined || modelId === '') {
        return;
      }
      this.$cache.local.set(this.storageKey(), String(modelId));
    },
    keyEquals(a, b) {
      return String(a) === String(b);
    },
    reload() {
      if (!this.projectId) {
        return;
      }
      this.loading = true;
      projectAiModelOptions(this.projectId)
        .then((res) => {
          const data = res.data || {};
          const ollama = data.ollama || [];
          const openai = data.openai || [];
          this.groups[0].options = ollama.map((m) => ({
            label: m.label || m.key,
            key: m.key
          }));
          this.groups[1].options = openai.map((m) => ({
            label: m.label || m.key,
            key: m.key
          }));
          const ollamaOpts = this.groups[0].options;
          const openaiOpts = this.groups[1].options;
          const allOpts = [...ollamaOpts, ...openaiOpts];
          const pickDefault = () => {
            if (ollamaOpts.length > 0) {
              return ollamaOpts[0].key;
            }
            if (openaiOpts.length > 0) {
              return openaiOpts[0].key;
            }
            return null;
          };
          const stored = this.getStoredModelId();
          const storedOpt = stored ? allOpts.find((o) => this.keyEquals(o.key, stored)) : null;
          let next = this.value;
          if (next && allOpts.some((o) => this.keyEquals(o.key, next))) {
            /* keep parent value */
          } else if (storedOpt) {
            next = storedOpt.key;
          } else {
            next = pickDefault();
          }
          this.innerModelId = next;
          this.$emit('input', next);
          this.emitServiceType(next);
        })
        .catch(() => {
          this.groups[0].options = [];
          this.groups[1].options = [];
        })
        .finally(() => {
          this.loading = false;
          this.scheduleStripComboWidth();
        });
    },
    serviceTypeForModelId(modelId) {
      if (!modelId) {
        return null;
      }
      const inOllama = this.groups[0].options.some((o) => this.keyEquals(o.key, modelId));
      return inOllama ? 'ollama' : 'openai';
    },
    emitServiceType(modelId) {
      this.$emit('service-type', this.serviceTypeForModelId(modelId));
    },
    onChange(val) {
      this.$emit('input', val);
      this.persistModelId(val);
      this.emitServiceType(val);
    }
  }
};
</script>

<style lang="scss" scoped>
/* 略小于 Cat2BugTextarea 工具条 24px；非组合 select 高度 */
$c2b-tool-h: 22px;
/* 组合模式：内容区水平宽度；整体高度略小以减少上下空隙 */
$c2b-combo-inner-w: 30px;
$c2b-combo-height: 24px;
$c2b-combo-hover-bg: #f0f7ff;
$c2b-tool-fs: 12px;
$c2b-border: #f0f2f5;
$c2b-border-hover: #e8eaed;
$c2b-focus: #1890ff;
$c2b-focus-soft: #ecf5ff;
$c2b-text: #606266;
$c2b-muted: #909399;
$c2b-radius: 3px;
$c2b-caret-side: 4px;
/* 组合条左侧空隙为右侧（三角侧留白）的两倍 */
$c2b-combo-pad-left: 2 * $c2b-caret-side;
$c2b-caret-fs: 12px;
/* 组合内 CSS 小三角：更淡的灰、不随下拉展开旋转 */
$c2b-combo-caret-triangle: #e0e2e6;
/* 机器人区域与下拉三角之间的水平间距 */
$c2b-combo-robot-caret-gap: 4px;

.c2b-model-select-wrap {
  display: inline-block;
  vertical-align: middle;
}

.c2b-model-select-wrap--combo {
  display: inline-flex;
  align-items: stretch;
  gap: $c2b-combo-robot-caret-gap;
  box-sizing: border-box;
  width: $c2b-combo-inner-w + $c2b-combo-pad-left + $c2b-caret-side;
  min-width: $c2b-combo-inner-w + $c2b-combo-pad-left + $c2b-caret-side;
  max-width: $c2b-combo-inner-w + $c2b-combo-pad-left + $c2b-caret-side;
  height: $c2b-combo-height;
  min-height: $c2b-combo-height;
  max-height: $c2b-combo-height;
  padding: 0 $c2b-caret-side 0 $c2b-combo-pad-left;
  border: none;
  border-radius: $c2b-radius;
  background-color: transparent;
  overflow: visible;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.c2b-model-select-wrap--combo:hover,
.c2b-model-select-wrap--combo:focus-within {
  background-color: $c2b-combo-hover-bg;
  outline: none;
}

.c2b-model-select-wrap--combo .c2b-model-select__robot-tooltip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  align-self: stretch;
  flex: 1 1 0;
  min-width: 0;
  overflow: visible;
}

.c2b-model-select-wrap--combo .c2b-model-select__robot-tooltip .el-button {
  height: 100%;
  min-height: 100%;
}

.c2b-model-select-wrap--combo .c2b-model-select__robot-tooltip > span {
  display: inline-flex !important;
  align-items: center;
  align-self: stretch;
  height: 100%;
}

.c2b-model-select__robot {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 0 2px;
  height: 100%;
  min-height: 100%;
  border: none;
  border-radius: 0;
  color: $c2b-focus;
  line-height: 1;
  box-sizing: border-box;
  font-size: 12px;
}

.c2b-model-select__robot:hover,
.c2b-model-select__robot:focus {
  color: $c2b-focus;
  background-color: transparent !important;
}

.c2b-model-select__robot:disabled {
  color: #8cc5fc;
  opacity: 1;
}

.c2b-model-select__robot span {
  margin-left: 2px;
  font-size: 11px;
  color: $c2b-muted;
  line-height: 1;
}

.c2b-model-select__robot ::v-deep .svg-icon {
  width: 14px;
  height: 14px;
  display: block;
}

/* 非组合：与原先一致；组合：由 --combo 壳承担固定宽度与裁切 */
.c2b-model-select__trigger-shell {
  display: inline-block;
  vertical-align: middle;
}

.c2b-model-select__trigger-shell--combo {
  flex: 1 1 0;
  min-width: 0;
  width: auto;
  max-width: none;
  height: auto;
  align-self: stretch;
  overflow: visible;
  display: flex;
  align-items: center;
}

.c2b-model-select__trigger-shell--combo ::v-deep .c2b-model-select--in-combo.el-select {
  width: 100% !important;
  min-width: 0 !important;
  max-width: 100% !important;
  height: 100% !important;
  min-height: 0 !important;
  overflow: visible;
}

.c2b-model-select__trigger-shell--combo ::v-deep .c2b-model-select--in-combo .el-input {
  width: 100% !important;
  min-width: 0 !important;
  max-width: 100% !important;
  height: 100% !important;
  min-height: 0 !important;
  display: flex;
  align-items: center;
  position: relative;
  overflow: visible;
}

.c2b-model-select__trigger-shell--combo ::v-deep .c2b-model-select--in-combo .el-input__inner {
  height: 100% !important;
  min-height: 0 !important;
  line-height: 1.2 !important;
  padding: 0;
  min-width: 0 !important;
  width: 100% !important;
  max-width: 100% !important;
  font-size: $c2b-tool-fs;
  font-weight: 400;
  letter-spacing: 0.01em;
  border: none;
  border-radius: 0;
  background: transparent;
  box-sizing: border-box;
}

.c2b-model-select__trigger-shell--combo ::v-deep .c2b-model-select--in-combo .el-input.is-focus .el-input__inner {
  border: none;
  box-shadow: none;
}

.c2b-model-select__trigger-shell--combo ::v-deep .c2b-model-select--in-combo .el-input__suffix {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: auto;
  margin: auto 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  transform: none;
  padding: 0 $c2b-caret-side;
  box-sizing: border-box;
  z-index: 2;
}

.c2b-model-select__trigger-shell--combo ::v-deep .c2b-model-select--in-combo .el-input__suffix-inner {
  display: flex;
  align-items: center;
  line-height: 1;
}
::v-deep .c2b-model-select:not(.c2b-model-select--in-combo) .el-input__inner {
  height: $c2b-tool-h;
  line-height: $c2b-tool-h;
  padding: 0 $c2b-caret-side 0 7px;
  font-size: $c2b-tool-fs;
  border-radius: $c2b-radius;
  border: 1px solid $c2b-border;
  box-sizing: border-box;
}

::v-deep .c2b-model-select:not(.c2b-model-select--in-combo) .el-input.is-focus .el-input__inner {
  border-color: $c2b-focus;
}

::v-deep .c2b-model-select:not(.c2b-model-select--in-combo):hover .el-input__inner {
  border-color: $c2b-border-hover;
}

::v-deep .c2b-model-select .el-input__icon {
  line-height: $c2b-tool-h;
}

.c2b-model-select__trigger-shell--combo ::v-deep .c2b-model-select--in-combo .el-input__icon {
  line-height: 1 !important;
}

::v-deep .c2b-model-select .el-input__suffix {
  right: 0;
  display: flex;
  align-items: center;
  padding: 0 $c2b-caret-side;
  box-sizing: border-box;
}

::v-deep .c2b-model-select .el-input__suffix-inner .el-select__caret {
  font-size: $c2b-caret-fs;
  line-height: 1;
}

/*
 * 组合模式壳子很窄，Element 图标字在部分环境下会裁切/不渲染；用 CSS 三角替代 ::before 字形。
 * 三角始终朝下，取消 Element 在展开态对 .el-select__caret 的旋转。is-loading 时不改后缀，保留转圈。
 */
.c2b-model-select__trigger-shell--combo
  ::v-deep
  .c2b-model-select--in-combo.el-select:not(.is-loading)
  .el-input__suffix-inner
  .el-select__caret {
  position: relative;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  width: 10px;
  min-width: 10px;
  height: 10px;
  font-size: 0 !important;
  line-height: 1 !important;
  color: transparent !important;
  -webkit-text-fill-color: transparent !important;
  transform: none !important;
  transition: none !important;
}

.c2b-model-select__trigger-shell--combo
  ::v-deep
  .c2b-model-select--in-combo.el-select:not(.is-loading)
  .el-input__suffix-inner
  .el-select__caret::before {
  display: none !important;
}

.c2b-model-select__trigger-shell--combo
  ::v-deep
  .c2b-model-select--in-combo.el-select:not(.is-loading)
  .el-input__suffix-inner
  .el-select__caret::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  margin-top: 0.5px;
  transform: translate(-50%, -50%);
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 4px 3.5px 0 3.5px;
  border-color: $c2b-combo-caret-triangle transparent transparent transparent;
  pointer-events: none;
}

::v-deep .c2b-model-select.el-select--mini .el-input__inner {
  font-size: $c2b-tool-fs;
}

::v-deep .c2b-model-select:not(.c2b-model-select--in-combo) {
  min-width: 34px;
}

::v-deep .c2b-model-select .el-input {
  font-size: $c2b-tool-fs;
}

/* 收起态始终隐藏输入框内的选中/回退文案（Element 在选项未就绪时会写 String(value)） */
::v-deep .c2b-model-select .el-input__inner {
  color: transparent !important;
  caret-color: transparent;
}

/* 非组合：占位符可见；组合：不展示任何占位（避免 Element 默认「请选择」与首屏闪现） */
::v-deep .c2b-model-select:not(.c2b-model-select--in-combo) .el-input__inner::placeholder {
  color: #c0c4cc;
  opacity: 1;
}

::v-deep .c2b-model-select:not(.c2b-model-select--in-combo) .el-input__inner::-webkit-input-placeholder {
  color: #c0c4cc;
  opacity: 1;
}

::v-deep .c2b-model-select:not(.c2b-model-select--in-combo) .el-input__inner:-ms-input-placeholder {
  color: #c0c4cc;
}

::v-deep .c2b-model-select:not(.c2b-model-select--in-combo) .el-input__inner::-ms-input-placeholder {
  color: #c0c4cc;
}

::v-deep .c2b-model-select--in-combo .el-input__inner::placeholder {
  color: transparent !important;
  opacity: 0 !important;
}

::v-deep .c2b-model-select--in-combo .el-input__inner::-webkit-input-placeholder {
  color: transparent !important;
  opacity: 0 !important;
  -webkit-text-fill-color: transparent;
}

::v-deep .c2b-model-select--in-combo .el-input__inner:-ms-input-placeholder {
  color: transparent !important;
}

::v-deep .c2b-model-select--in-combo .el-input__inner::-ms-input-placeholder {
  color: transparent !important;
}
</style>

<style lang="scss">
/* 下拉层面板（挂载到 body）：组名为描边胶囊，选项块保持层次 */
.el-select-dropdown.c2b-model-select-dropdown {
  margin-top: 4px;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
  padding: 6px 0 4px;
  box-sizing: border-box;
}

.el-select-dropdown.c2b-model-select-dropdown .el-select-group {
  margin: 0;
  padding: 0;
}

.el-select-dropdown.c2b-model-select-dropdown .el-select-group__wrap:not(:last-of-type) {
  padding-bottom: 12px;
  margin-bottom: 2px;
}

.el-select-dropdown.c2b-model-select-dropdown .el-select-group__wrap:not(:last-of-type)::after {
  left: 12px;
  right: 12px;
  bottom: 0;
  height: 1px;
  background: #f0f2f5;
}

/* 组名：白底描边小胶囊，与下方列表行区分开 */
.el-select-dropdown.c2b-model-select-dropdown .el-select-group__title {
  display: inline-block;
  margin: 4px 12px 8px;
  padding: 4px 11px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #1890ff;
  line-height: 1.35;
  background: #fff;
  border: 1px solid rgba(24, 144, 255, 0.35);
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(24, 144, 255, 0.06);
  box-sizing: border-box;
}

.el-select-dropdown.c2b-model-select-dropdown .el-select-group .el-select-dropdown__item {
  margin: 0 8px 2px;
  padding: 0 12px 0 10px !important;
  height: 30px;
  line-height: 30px;
  font-size: 12px;
  color: #303133;
  border-radius: 4px;
  box-sizing: border-box;
}

.el-select-dropdown.c2b-model-select-dropdown .el-select-group .el-select-dropdown__item.hover,
.el-select-dropdown.c2b-model-select-dropdown .el-select-group .el-select-dropdown__item:hover {
  background-color: #f0f7ff !important;
  color: #1890ff;
}

.el-select-dropdown.c2b-model-select-dropdown .el-select-group .el-select-dropdown__item.selected {
  background-color: #e6f7ff !important;
  color: #1890ff !important;
  font-weight: 600;
}

.el-select-dropdown.c2b-model-select-dropdown .el-select-group .el-select-dropdown__item.selected.hover,
.el-select-dropdown.c2b-model-select-dropdown .el-select-group .el-select-dropdown__item.selected:hover {
  background-color: #d6efff !important;
}

.c2b-model-select-option__cell {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
}

.c2b-model-select-option__tick {
  flex: 0 0 16px;
  width: 16px;
  font-size: 13px;
  line-height: 1;
  color: #1890ff;
  visibility: hidden;
}

.c2b-model-select-option__tick--on {
  visibility: visible;
}

.c2b-model-select-option__name {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.c2b-model-select-dropdown__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 14px 16px 12px;
  box-sizing: border-box;
  color: #909399;
}

.c2b-model-select-dropdown__loading-icon {
  font-size: 20px;
  line-height: 1;
  margin-bottom: 6px;
}

.c2b-model-select-dropdown__loading-text {
  font-size: 11px;
  line-height: 1.3;
  color: #909399;
}
</style>
