<template>
  <div class="select-module-add-root">
    <el-button v-if="!formVisible" type="text" icon="el-icon-plus" class="select-module-add-full select-module-add-button" @click="openForm">{{$t('module.new')}}</el-button>
    <el-form v-show="formVisible"
             ref="form"
             :model="form"
             :rules="rules"
             label-width="0"
             class="select-module-add"
             @keydown.enter.native='addProjectModule'
             @submit.native.prevent>
      <el-form-item prop="moduleName" label-width="0" class="select-module-add-item">
        <div class="select-module-add-combo">
          <el-input
            ref="moduleNameInput"
            class="select-module-add-input"
            :placeholder="$t('module.enter-module-name')"
            v-model="name"
            size="small"
            @input="nameChangeHandle"
            @keydown.native.esc.stop="onInputEscape">
            <el-button slot="append" icon="el-icon-plus" size="mini"
                       class="select-module-add-submit"
                       @mousedown.native.prevent
                       @click.stop="addProjectModule"></el-button>
          </el-input>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import {addModule} from "@/api/system/module";
import { suppressDropdownBlurClose } from '@/utils/dropdown-blur-close';

export default {
  name: "AddModuleMenuItem",
  model: {
    prop: 'moduleName',
    event: 'input'
  },
  data() {
    return {
      name: null,
      formVisible: false,
      // 模块表单
      form: {},
      // 表单校验
      rules: {
        moduleName: [
          { required: true, message: this.$i18n.t('module.name-cannot-empty'), trigger: "blur" }
        ]
      }
    }
  },
  props: {
    moduleName: {
      type: String,
      default: null
    },
    projectId: {
      type: Number,
      default: null
    },
    modulePid: {
      type: Number,
      default: 0
    },
  },
  methods: {
    reset() {
      this.form={};
      this.name=null;
    },
    openForm() {
      suppressDropdownBlurClose();
      this.setFormVisible(true, true);
    },
    setFormVisible(visible, focus = false) {
      this.formVisible = visible;
      if (visible && focus) {
        this.$nextTick(() => this.focusInput());
      }
    },
    focusInput() {
      const ref = this.$refs.moduleNameInput;
      const input = ref && ref.$refs && ref.$refs.input;
      if (input && typeof input.focus === 'function') {
        input.focus();
      }
    },
    onInputEscape() {
      this.$emit('input-escape');
    },
    nameChangeHandle(val){
      this.$emit('input',val);
    },
    addProjectModule(){
      const that = this;
      suppressDropdownBlurClose(500);
      this.form = {
        modulePid: this.modulePid||0,
        moduleName: this.name,
        projectId: this.projectId
      }
      this.$nextTick(()=>{
        this.$refs["form"].validate(valid => {
          if (valid) {
            addModule(this.form).then(res=>{
              suppressDropdownBlurClose(500);
              that.$emit('added', {
                module: res.data,
                moduleName: that.name
              });
              that.formVisible = false;
              that.reset();
              that.$message.success(this.$i18n.t('module.create-success').toString());
            })
          }
        });
      })
    }
  }
}
</script>

<style lang="scss" scoped>
  .select-module-add-root {
    width: 100%;
    box-sizing: border-box;
    overflow: visible;
  }
  .select-module-add {
    width: 100%;
    box-sizing: border-box;
    overflow: visible;
    .select-module-add-item {
      margin-top: 6px;
      margin-bottom: 0;
    }
    ::v-deep .el-form-item__content {
      line-height: normal;
      overflow: visible;
    }
    .select-module-add-combo {
      width: 100%;
      box-sizing: border-box;
      border-radius: 4px;
      overflow: visible;

      &:focus-within {
        box-shadow: inset 0 0 0 1px var(--cat2bug-field-focus-color, #1890ff);
      }
    }
    ::v-deep .select-module-add-input.el-input-group {
      width: 100%;
      vertical-align: middle;
      table-layout: fixed;

      .el-input__inner {
        border-top-right-radius: 0 !important;
        border-bottom-right-radius: 0 !important;

        &:focus {
          border-color: var(--border-color-base, #dcdfe6) !important;
          box-shadow: none !important;
        }
      }

      &:focus-within .el-input__inner {
        border-color: var(--border-color-base, #dcdfe6) !important;
      }

      .el-input-group__append {
        width: 36px;
        min-width: 36px;
        border-top-left-radius: 0 !important;
        border-bottom-left-radius: 0 !important;
        padding: 0;
        vertical-align: middle;

        .select-module-add-submit.el-button {
          width: 100%;
          min-width: 36px;
          height: 100%;
          min-height: 30px;
          padding: 0 8px;
          border-top-left-radius: 0 !important;
          border-bottom-left-radius: 0 !important;
          border-left: none !important;
          margin: 0;
          color: var(--text-color-regular, #606266);

          &:hover,
          &:focus {
            color: var(--cat2bug-primary, #409eff);
            outline: none !important;
            box-shadow: none !important;
          }

          @at-root html.dark & {
            color: #e5eaf3;

            &:hover,
            &:focus {
              color: #ffc107;
            }
          }
        }
      }

      &:focus-within .el-input-group__append {
        border-color: var(--border-color-base, #dcdfe6) !important;
      }
    }
    ::v-deep .el-form-item__error {
      position: relative;
      padding-top: 2px;
      line-height: 1.2;
      white-space: normal;
      word-break: break-all;
    }
  }
  .select-module-add-full {
    width: 100%;
  }
  .select-module-add-button {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    width: 100%;
    min-height: calc(1.4em + 10px);
    padding: 5px 10px !important;
    margin: 0 !important;
    line-height: 1.4;
    box-sizing: border-box;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    border-radius: 5px;
  }
  .select-module-add-button:hover {
    background-color: #F2F6FC;
    border-radius: 5px;
    cursor: pointer;
  }
</style>
