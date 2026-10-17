<template>
  <div class="case-card" :class="$attrs.class">
    <div class="case-card-body" v-if="caseModel && caseModel.caseId">
      <div class="case-card-tools" v-if="_toolsVisible(caseModel)">
        <el-popover
          placement="left-start"
          trigger="hover"
          width="auto"
          popper-class="case-card-media-popover">
          <div style="display: inline-flex; flex-direction: row; gap: 5px; max-width: 900px">
            <cat2-bug-image
              v-for="(img,index) in list(caseModel.imgUrls)"
              :key="index"
              style="width: 100px; height: 100px"
              :src="img"
              class="image-click"
              :preview-src-list="list(caseModel.imgUrls)"
              fit="cover"></cat2-bug-image>
          </div>
          <template #reference>
            <el-badge :value="num(caseModel.imgUrls)" class="item" v-if="num(caseModel.imgUrls)">
              <el-button :icon="$legacyIcon('el-icon-picture-outline')" size="small"></el-button>
            </el-badge>
          </template>
        </el-popover>
        <el-popover
          placement="left-start"
          trigger="hover"
          :width="280"
          popper-class="case-card-media-popover">
          <div style="display: inline-flex; flex-direction: column; align-items: start;">
            <cat2-bug-text type="down" :content="file" :tooltip="file" v-for="(file,index) in list(caseModel.annexUrls)" :key="index"/>
          </div>
          <template #reference>
            <el-badge :value="num(caseModel.annexUrls)" class="item" v-if="num(caseModel.annexUrls)">
              <el-button :icon="$legacyIcon('el-icon-paperclip')" size="small"></el-button>
            </el-badge>
          </template>
        </el-popover>
      </div>
      <div class="case-card-content">
        <div class="case-card-header">
          <div class="row">
            <label>{{ $t('title') }}</label><span>{{ caseModel.caseName }}</span>
          </div>
          <div class="row">
            <label>{{ $t('preconditions') }}</label><span>{{ caseModel.casePreconditions }}</span>
          </div>
          <div class="row">
            <label>{{ $t('expect') }}</label><span>{{ caseModel.caseExpect }}</span>
          </div>
        </div>
        <div class="row footer">
          <label>{{ $t('step') }}</label>
          <div class="case-card-step-wrap">
            <case-step
              ref="caseStep"
              :case-steps="caseModel.caseStep"
              :edit="edit"
              :state-visible="stateVisible"
              :step-index="stepIndex"
              @update:step-index="onStepIndexUpdate"
              @change="onStepChange"
            />
          </div>
        </div>
      </div>
    </div>
    <div v-else class="case-card-empty">
      <span>{{ $t('empty-data') }}</span>
    </div>

  </div>
</template>

<script>
import CaseStep from "@/components/Case/CaseStep";
import Cat2BugText from "@/components/Cat2BugText";

export default {
  emits: ['update:stepIndex', 'change'],
  name: "CaseCard",
  components: {CaseStep, Cat2BugText},
  inheritAttrs: false,
  data() {
    return {
    }
  },
  props: {
    caseModel: {
      type: Object,
      default: {}
    },
    toolsVisible: {
      type: Boolean,
      default: true
    },
    /** 是否可点选失败步骤（缺陷表单选中用例后） */
    edit: {
      type: Boolean,
      default: false
    },
    /** 是否展示步骤通过/未通过状态 */
    stateVisible: {
      type: Boolean,
      default: false
    },
    /** 未通过步骤下标（之前的步骤视为通过） */
    stepIndex: {
      type: Number,
      default: null
    }
  },
  computed: {
    list: function () {
      return function (str) {
        return str? str.split(',').map(i => process.env.VUE_APP_BASE_API + i):[];
      }
    },
    num: function () {
      return function (strArray) {
        return strArray? strArray.split(',').length:0;
      }
    },
    _toolsVisible: function () {
      return function (model) {
        return this.toolsVisible && model && model.imgUrls && model.annexUrls && (this.num(model.imgUrls) + this.num(model.annexUrls))>0;
      }
    },
    getFileName: function () {
      return function (url) {
        if(!url) return null;
        let arr = url.split('\/');
        return arr[arr.length-1];
      }
    },
  },
  methods: {
    onStepIndexUpdate(index) {
      this.$emit('update:stepIndex', index)
    },
    onStepChange(index) {
      this.$emit('change', index)
    },
    /** 下载附件操作 */
    handleDown(event, file) {
      const a = document.createElement("a");
      const e = new MouseEvent("click");
      a.href = file;
      a.dispatchEvent(e);
      event.stopPropagation();
    },
  }
}
</script>

<style lang="scss" scoped>
.case-card {
  position: relative;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  padding: 4px 0;
  border: none;
  background: transparent;
  box-shadow: none;
  .case-card-body {
    width: 100%;
    min-width: 0;
    display: flex;
    flex-direction: row;
    align-items: flex-start;
    gap: 8px;
  }
  .case-card-tools {
    display: flex;
    flex-direction: column;
    flex: 0 0 auto;
    gap: 10px;
    > span {
      flex: 0;
    }
    .el-button {
      padding: 5px;
      margin: 0;
    }
    .el-badge > :deep(.el-badge__content) {
      font-size: 0.6rem;
      height: 15px;
      width: 15px;
      line-height: 13px;
      padding: 0;
    }
  }
  .case-card-content {
    flex: 1 1 auto;
    min-width: 0;
  }
  .case-card-header {
    width: 100%;
    min-width: 0;
  }
  .case-card-empty {
    width: 100%;
    min-height: 48px;
    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
    background-color: var(--bg-color-block, #fbfdff);
    border-radius: 5px;
    border: 1px dashed var(--border-color-base, #c0ccdc);
    color: var(--text-color-secondary, #909399);
    > span {
      flex: none;
      font-size: 14px;
    }
  }
  /* 固定标签列宽，标题/前置条件/预期左缘对齐 */
  label {
    flex: 0 0 4.5em;
    width: 4.5em;
    min-width: 4.5em;
    max-width: 4.5em;
    margin: 0;
    padding: 0;
    text-align: right;
    font-size: 12px;
    line-height: 1.5;
    color: var(--text-color-secondary, #909399);
  }
  .case-card-header > .row > span {
    flex: 1 1 auto;
    min-width: 0;
    font-size: 13px;
    line-height: 1.5;
    word-break: break-word;
    color: var(--text-color-primary, #303133);
  }
  /* 同组件内包一层，保证步骤区吃满剩余宽度（scoped 无法直接 flex 子组件根节点） */
  .case-card-step-wrap {
    flex: 1 1 auto;
    min-width: 0;
    width: 0; /* 配合 flex:1，强制按剩余空间计算宽度，避免被内容撑成极窄列 */
  }
  .footer {
    width: 100%;
    min-width: 0;
    align-items: flex-start;
    padding-top: 8px;
    margin-top: 6px;
    border-top: 1px dashed var(--border-color-light, #e4e7ed);
  }
}
.row {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
  min-width: 0;
  margin-bottom: 6px;
  &:last-child {
    margin-bottom: 0;
  }
}
.image-click {
  border-radius: 5px;
  border: 1px solid var(--border-color-lighter, #f2f6fc);
}
.image-click:hover {
  scale: 1.1;
  z-index: 999;
  border: 1px solid var(--border-color-base, #dcdfe6);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.3);
}
</style>
<style>
.el-popover.el-popper.case-card-media-popover {
  width: auto !important;
  min-width: 120px !important;
  max-width: min(900px, calc(100vw - 32px)) !important;
  box-sizing: border-box;
}
</style>
