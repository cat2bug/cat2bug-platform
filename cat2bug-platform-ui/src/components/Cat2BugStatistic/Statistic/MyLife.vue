<template>
  <div class="my-life-statistic-root">
    <cat2-bug-card :style="lifeCardStyle" :title="$i18n.t('defect.my-life').toString()" v-loading="loading" :tools="tools" @tools-click="toolsHandle">
      <template slot="content">
        <div class="life-text" @click="openEditHandle">
          <div ref="lifeBody" class="life-body">{{ myLife }}</div>
        </div>
      </template>
    </cat2-bug-card>
    <el-dialog
    :title="$t('defect.my-life')"
    :visible.sync="dialogVisible"
    width="480px"
    append-to-body
    custom-class="my-life-dialog"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :before-close="onToolDialogBeforeClose"
    @opened="onToolDialogOpened"
    @closed="handleClose"
  >
    <el-alert
      class="my-life-dialog__tip"
      :title="$t('defect.my-life.title')"
      type="success"
      :closable="false"
      show-icon
    />
    <el-form ref="form" class="my-life-dialog__form" :model="form" label-width="80px">
      <el-form-item :label="$t('font-size')">
        <el-radio-group v-model="form.fontSize" class="my-life-dialog__font-size">
          <el-radio label="45px" border>{{ $t('font-size.medium') }}</el-radio>
          <el-radio label="35px" border>{{ $t('font-size.small0') }}</el-radio>
          <el-radio label="20px" border>{{ $t('font-size.mini') }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="$t('font-color')">
        <el-color-picker v-model="form.color" class="my-life-dialog__color-picker" popper-class="my-life-dialog__color-dropdown" />
      </el-form-item>
      <el-form-item :label="$t('defect.my-life.content')" class="my-life-dialog__content-item">
        <el-input
          v-model="form.lifeContent"
          type="textarea"
          :placeholder="$t('defect.my-life.input-content')"
          class="my-life-dialog__textarea"
          :autosize="{ minRows: 4, maxRows: 4 }"
          maxlength="32"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="requestCloseToolDialog">{{ $t('cancel') }}</el-button>
      <el-button class="defect-kbd-hint-host" type="primary" @click="onSubmit">
        {{ $t('update') }}
        <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
      </el-button>
    </span>
  </el-dialog>
  </div>
</template>

<script>
import Cat2BugCard from "../Components/Card"
import statisticDialogKbd from '@/mixins/statistic-dialog-kbd'
import { updateConfig } from "@/api/system/user-config";

export default {
  name: "MyLife",
  mixins: [statisticDialogKbd],
  components: { Cat2BugCard },
  data() {
    return {
      loading: false,
      lifeContent: 'defect.my-life.click-input-content',
      dialogVisible: false,
      form: {
        color: '#303133',
        fontSize: '35px',
        lifeContent: null
      }
    }
  },
  props: {
    params: {
      type: Object,
      default: () => {}
    },
    tools: {
      type: Array,
      default: () => []
    },
    parent: {
      type: Object,
      default: () => {}
    },
    read: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    currentProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    myLife() {
      return this.form.lifeContent || this.$i18n.t(this.lifeContent);
    },
    lifeCardStyle() {
      return {
        '--color': this.lifeTextColor,
        '--fontSize': this.form.fontSize
      };
    },
    lifeTextColor() {
      const color = this.form.color || '#303133';
      if (this.$store.state.settings.themeMode === 'dark' && this.isColorTooDarkOnDarkBg(color)) {
        return '#f5f7fa';
      }
      return color;
    },
  },
  created() {
    if (this.$store.state.user.config.lifeContent) {
      this.form = JSON.parse(this.$store.state.user.config.lifeContent);
    } else if (this.$store.state.settings.themeMode === 'dark') {
      this.form.color = '#f5f7fa';
    }
  },
  mounted() {
    this.$nextTick(() => this.fitLifeBodyLayout());
    if (typeof ResizeObserver !== 'undefined') {
      this._lifeBodyRo = new ResizeObserver(() => this.fitLifeBodyLayout());
      const el = this.$refs.lifeBody;
      if (el) this._lifeBodyRo.observe(el);
    }
  },
  beforeDestroy() {
    if (this._lifeBodyRo) {
      this._lifeBodyRo.disconnect();
      this._lifeBodyRo = null;
    }
  },
  watch: {
    myLife() {
      this.$nextTick(() => this.fitLifeBodyLayout());
    },
    'form.fontSize'() {
      this.$nextTick(() => this.fitLifeBodyLayout());
    },
  },
  methods: {
    /** 固定高度内：优先用用户字号，放不下则逐步缩小直至换行后仍能完整展示 */
    fitLifeBodyLayout() {
      const el = this.$refs.lifeBody;
      if (!el) return;
      const basePx = parseFloat(String(this.form.fontSize || '35px'));
      const base = Number.isFinite(basePx) ? basePx : 35;
      el.style.fontSize = base + 'px';
      let size = base;
      const min = 12;
      while (size > min && (el.scrollHeight > el.clientHeight + 1 || el.scrollWidth > el.clientWidth + 1)) {
        size -= 1;
        el.style.fontSize = size + 'px';
      }
    },
    isColorTooDarkOnDarkBg(color) {
      const hex = String(color || '').trim().toLowerCase();
      if (!hex.startsWith('#')) {
        return false;
      }
      let value = hex.slice(1);
      if (value.length === 3) {
        value = value.split('').map(ch => ch + ch).join('');
      }
      if (value.length !== 6 || !/^[0-9a-f]{6}$/.test(value)) {
        return false;
      }
      const r = parseInt(value.slice(0, 2), 16);
      const g = parseInt(value.slice(2, 4), 16);
      const b = parseInt(value.slice(4, 6), 16);
      const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
      return luminance < 0.55;
    },
    toolsHandle(e, tool) {
      this.$emit('tools-click', tool);
    },
    handleClose() {
      this.dialogVisible = false;
    },
    shortcutSave() {
      this.onSubmit()
    },
    getFieldHintContainer() {
      const form = this.$refs.form
      return (form && form.$el) || this.$el
    },
    onSubmit() {
      const param = {
        lifeContent: JSON.stringify(this.form)
      };
      updateConfig(param).then(() => {
        this.lifeContent = this.form.lifeContent;
        this.$message.success(this.$i18n.t('update.success').toString());
        this.toolDialogCloseBaseline = null;
        this.dialogVisible = false;
      });
    },
    openEditHandle() {
      if (this.read) return;
      this.dialogVisible = true;
    }
  }
}
</script>

<style lang="scss" scoped>
.my-life-statistic-root {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  width: 100%;
  box-sizing: border-box;

  ::v-deep .statistic-box {
    flex: 1 1 auto;
    height: 100%;
    max-height: 100%;
    min-height: 0;
    width: 100%;
  }

  ::v-deep .statistic-box-body {
    flex: 1 1 auto;
    min-height: 0;
    overflow: hidden;
  }
}

.life-text {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}

.life-body {
  width: 100%;
  max-width: 100%;
  flex: 1 1 auto;
  min-height: 0;
  max-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  overflow: hidden;
  color: var(--color);
  font-size: var(--fontSize);
  text-align: center;
  line-height: 1.3;
  word-break: break-word;
  overflow-wrap: anywhere;
  white-space: normal;
  hyphens: auto;
}

.life-body:hover {
  cursor: pointer;
}
</style>

<style lang="scss">
.my-life-dialog {
  .el-dialog__header {
    padding: 16px 20px 12px;
  }

  .el-dialog__body {
    padding: 0 20px 16px;
  }

  .el-dialog__footer {
    padding: 12px 20px 16px;
  }

  .my-life-dialog__tip {
    margin: 0 0 16px;
  }

  .my-life-dialog__tip.el-alert {
    border-radius: var(--cat2bug-border-radius, 4px);
  }

  .my-life-dialog__form {
    .el-form-item {
      margin-bottom: 16px;
    }

    .my-life-dialog__content-item {
      margin-bottom: 0;
    }
  }

  .my-life-dialog__font-size {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;

    .el-radio.is-bordered {
      margin: 0 !important;
      padding: 0 8px;
      height: 28px;
      line-height: 26px;
      border-radius: var(--cat2bug-border-radius, 4px);
    }

    .el-radio.is-bordered + .el-radio.is-bordered {
      margin-left: 0 !important;
    }

    .el-radio__label {
      font-size: 12px;
      padding-left: 4px;
    }

    .el-radio__inner {
      width: 12px;
      height: 12px;
    }
  }

  .my-life-dialog__color-picker.el-color-picker {
    .el-color-picker__trigger {
      width: 32px;
      height: 32px;
      padding: 3px;
      border: 1px solid var(--border-color-base, #dcdfe6);
      border-radius: var(--cat2bug-border-radius, 4px);
      background-color: var(--input-bg, #fff);
    }

    .el-color-picker__color {
      border: none;
      border-radius: 2px;
      overflow: hidden;
    }

    .el-color-picker__icon {
      color: var(--text-color-secondary, #909399);
    }
  }

  .my-life-dialog__textarea .el-textarea__inner {
    resize: none;
  }
}
</style>
