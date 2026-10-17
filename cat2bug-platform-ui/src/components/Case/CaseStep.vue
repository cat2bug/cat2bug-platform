<template>
  <div class="step">
    <div
      class="step-row"
      v-for="(step,index) in steps"
      :key="'step-' + index + '-' + (step && step.state)"
      :class="{ pointer: stateVisible && edit }"
      :state="stateVisible ? step.state : ''"
      @click.stop="onStepRowClick($event, index)"
      @mousedown.stop="onStepRowMouseDown"
    >
      <p>{{ index + 1 }}</p>
      <span>{{ step.stepDescribe }}</span>
      <span>{{ step.stepExpect }}</span>
      <div v-show="stateVisible" class="step-state" :state="step.state">{{ stateName(step) }}</div>
    </div>
    <div v-show="stateVisible" class="step-tools">
      <el-link v-show="isShowFoldButton" :underline="false" type="primary" @click="foldChangedHandle"><svg-icon :icon-class="foldState?'up':'down'" />{{ foldButtonName }}</el-link>
    </div>
  </div>
</template>

<script>
// 没有测试
const NOT_TEST_KEY = 'case.not-tested';
// 通过测试
const PASS_TEST_KEY = 'case.pass-tested';
// 未通过
const FAILED_TEST_KEY = 'case.failed-tested';
export default {
  emits: ['update:stepIndex', 'change'],
  name: "step",
  data() {
    return {
      foldState: false,
      /** 点击后立刻用于渲染，避免等父级回写 */
      localFailIndex: null,
    }
  },
  props: {
    caseSteps: {
      type: Array,
      default: ()=>[]
    },
    stepIndex: {
      type: Number,
      default: null
    },
    edit: {
      type: Boolean,
      default: false
    },
    stateVisible: {
      type: Boolean,
      default: false
    },
    maxShowCaseCount: {
      type: Number,
      default: 3
    }
  },
  computed: {
    foldButtonName: function (){
      if(this.foldState) {
        return this.$i18n.t('case.fold-button-off');
      } else {
        return this.$i18n.t('case.fold-button-on');
      }
    },
    failAtIndex() {
      if (this.localFailIndex != null && Number.isFinite(Number(this.localFailIndex))) {
        return Number(this.localFailIndex)
      }
      if (this.stepIndex != null && Number.isFinite(Number(this.stepIndex))) {
        return Number(this.stepIndex)
      }
      return null
    },
    /** 显示的步骤（带原始下标，折叠过滤后点击仍写回正确 caseStepId） */
    steps: function() {
      const raw = Array.isArray(this.caseSteps) ? this.caseSteps : [];
      let ret = raw.map((s, originalIndex) => {
        const row = s && typeof s === 'object' ? { ...s } : { stepDescribe: s };
        row._originalIndex = originalIndex;
        return row;
      });
      const failAt = this.failAtIndex;
      if (failAt != null) {
        for (let i = 0; i < failAt && i < ret.length; i++) {
          ret[i].state = PASS_TEST_KEY;
        }
        if (failAt >= 0 && failAt < ret.length) {
          ret[failAt].state = FAILED_TEST_KEY;
          for (let i = failAt + 1; i < ret.length; i++) {
            ret[i].state = NOT_TEST_KEY;
          }
        }
      }

      if (!this.stateVisible || raw.length <= this.maxShowCaseCount || this.foldState) {
        return ret;
      }
      const tempArr = ret.filter((c) => c.state === PASS_TEST_KEY || c.state === FAILED_TEST_KEY);
      return [
        ...tempArr,
        ...ret
          .filter((c) => !c.state || c.state === NOT_TEST_KEY)
          .filter((c, i) => i < Math.max(this.maxShowCaseCount - tempArr.length, 1))
      ];
    },
    isShowFoldButton: function () {
      if (!Array.isArray(this.caseSteps) || this.caseSteps.length <= this.maxShowCaseCount) {
        return false;
      }
      if (this.failAtIndex == null) {
        return this.caseSteps.length > this.maxShowCaseCount;
      }
      return (this.caseSteps.length - this.failAtIndex - 1) > 1;
    },
    /** 状态名 */
    stateName: function (){
      return function (step){
        if(step.state) {
          return this.$i18n.t(step.state);
        } else {
          return this.$i18n.t(NOT_TEST_KEY);
        }
      }
    },
  },
  watch: {
    stepIndex: {
      immediate: true,
      handler(v) {
        if (v == null || v === '' || !Number.isFinite(Number(v))) {
          this.localFailIndex = null
        } else {
          this.localFailIndex = Number(v)
        }
      }
    }
  },
  methods: {
    onStepRowMouseDown() {
      /* 阻止外层 SelectCase / Popover 抢点击 */
    },
    onStepRowClick(e, index) {
      if (!this.edit || !this.stateVisible) return;
      const row = this.steps[index];
      const originalIndex = row && row._originalIndex != null ? row._originalIndex : index;
      this.clickStepHandle(e, originalIndex);
    },
    clickStepHandle(e, index) {
      if (!this.stateVisible) return;
      const next = Number(index);
      if (!Number.isFinite(next)) return;
      this.localFailIndex = next;
      this.$emit('update:stepIndex', next);
      this.$emit('change', next);
    },
    foldChangedHandle(e) {
      this.foldState = !this.foldState;
      if (e && typeof e.stopPropagation === 'function') {
        e.stopPropagation();
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.step {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  /* 勿 overflow:auto，否则下拉里会出现右侧空滚动条槽 */
  overflow: visible;
  gap: 2px;
}
.step-row {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 10px;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  padding: 8px 4px 8px 8px;
  margin: 0;
  border: none;
  border-radius: 0;
  border-left: 2px solid transparent;
  background: transparent;
  color: var(--text-color-regular, #606266);

  /* 序号：随状态变色，未测为中性灰 */
  p {
    flex: 0 0 20px;
    width: 20px;
    height: 20px;
    margin: 0;
    line-height: 20px;
    text-align: center;
    font-size: 12px;
    font-weight: 600;
    border-radius: 50%;
    background-color: var(--text-color-secondary, #909399);
    color: #fff;
  }

  > span {
    flex: 1 1 120px;
    min-width: 120px;
    max-width: 100%;
    overflow-wrap: break-word;
    word-break: break-word;
    text-align: left;
    font-size: 13px;
    line-height: 1.5;
    margin: 0;
    color: inherit;
  }

  > span + span {
    padding-left: 10px;
    border-left: 1px solid var(--border-color-lighter, #ebeef5);
  }

  /* 状态：轻量描边标签，避免大色块 */
  .step-state {
    flex: 0 0 auto;
    min-width: 52px;
    height: 22px;
    line-height: 20px;
    margin: 0;
    padding: 0 8px;
    border-radius: 11px;
    text-align: center;
    font-size: 12px;
    font-weight: 500;
    box-sizing: border-box;
    border: 1px solid var(--border-color-base, #dcdfe6);
    background: transparent;
    color: var(--text-color-secondary, #909399);
  }
  .step-state[state='case.not-tested'] {
    border-color: var(--border-color-base, #dcdfe6);
    color: var(--text-color-secondary, #909399);
    background: transparent;
  }
  .step-state[state='case.pass-tested'] {
    border-color: rgba(103, 194, 58, 0.55);
    color: #67c23a;
    background: rgba(103, 194, 58, 0.08);
  }
  .step-state[state='case.failed-tested'] {
    border-color: rgba(245, 108, 108, 0.55);
    color: #f56c6c;
    background: rgba(245, 108, 108, 0.08);
  }
}

.step-row.pointer {
  cursor: pointer;
  border-radius: 4px;
  &:hover {
    background-color: var(--table-row-hover-bg, rgba(24, 144, 255, 0.06));
  }
}

.step-row[state='case.pass-tested'] {
  border-left-color: #67c23a;
  background: transparent;
  color: var(--text-color-regular, #606266);

  p {
    background-color: #67c23a;
  }
}

.step-row[state='case.failed-tested'] {
  border-left-color: #f56c6c;
  background: rgba(245, 108, 108, 0.06);
  color: var(--text-color-regular, #606266);

  p {
    background-color: #f56c6c;
  }
}

.step-row[state='case.not-tested'] {
  border-left-color: var(--border-color-light, #e4e7ed);

  p {
    background-color: var(--text-color-secondary, #909399);
  }
}

html.dark .step-row.pointer:hover {
  background-color: rgba(105, 177, 255, 0.08);
}
html.dark .step-row[state='case.failed-tested'] {
  background: rgba(245, 108, 108, 0.1);
}
html.dark .step-row > span + span {
  border-left-color: var(--border-color-light);
}

.step-tools {
  display: flex;
  justify-content: center;
  width: 100%;
  flex: 1;
  font-size: 14px;
  padding-top: 4px;
}
</style>
