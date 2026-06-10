<template>
  <el-dialog :title="title" :visible.sync="visible" width="600px" append-to-body :close-on-press-escape="false" :before-close="onToolDialogBeforeClose" @opened="onToolDialogOpened">
    <el-form ref="form" rules="rules" :model="form" :rules="rules" label-width="150px" class="module-dialog-form">
      <el-form-item :label="$t('module.parent-module')" prop="modulePid">
          <treeselect
            v-model="form.modulePid"
            :options="moduleOptions"
            :normalizer="normalizer"
            :searchable="false"
            :placeholder="$t('module.please-select-parent-module')"
          />
      </el-form-item>
      <el-form-item :prop="moduleNameFormProp" class="module-name-row cat2bug-field-hint-exclude is-no-asterisk">
        <template slot="label">
          <div class="module-switch-title">
            <label class="module-name-label-hint">
              <span v-if="!form.moduleId" class="module-name-required" aria-hidden="true">*</span>{{ moduleNameLabelText }}
            </label>
            <span v-show="!form.moduleId" class="module-mode-switch-hint">
              <el-switch
                v-model="addModuleMode"
                active-color="#13ce66"
                inactive-color="#ff4949">
              </el-switch>
            </span>
          </div>
        </template>
        <div class="module-name-hint-host">
          <el-input
            v-if="form.moduleId || addModuleMode"
            v-model="form.moduleName"
            :placeholder="$t('module.enter-module-name')"
            maxlength="128"
          />
          <el-input
            v-else
            type="textarea"
            rows="8"
            v-model="strAddModules"
            :placeholder="$t('module.enter-module-names')"
            maxlength="65536"
            show-word-limit
          />
        </div>
      </el-form-item>
      <el-form-item v-show="addModuleMode" :label="$t('annex')" prop="annexUrls">
        <file-upload v-model="form.annexUrls" :limit="9" :file-type="[]"/>
      </el-form-item>
      <el-form-item :label="$t('remark')" prop="remark">
        <el-input v-model="form.remark"
                  :placeholder="$t('please-enter-remark')"
                  type="textarea"
                  maxlength="255"
                  rows="5"
                  show-word-limit
        />
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button class="defect-kbd-hint-host" type="primary" @click="submitForm">
        {{ $t('ok') }}
        <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
      </el-button>
      <el-button @click="requestCloseToolDialog">{{ $t('cancel') }}</el-button>
    </div>
  </el-dialog>
</template>

<script>
import {addModule, getModule, listModule, updateModule} from "@/api/system/module";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import Label from "@/components/Cat2BugStatistic/Components/Label";
import {strFormat} from "@/utils";
import defectToolDialogKbd from '@/mixins/defect-tool-dialog-kbd'
export default {
  name: "ModuleDialog",
  mixins: [defectToolDialogKbd],
  components: {Label, Treeselect},
  data() {
    return {
      // 添加模型的模式（true=单个添加；false=多个添加）
      addModuleMode: true,
      strAddModules: '',
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      visible: false,
      // 模块树选项
      moduleOptions: [],
      // 表单参数
      form: {
        projectId: this.projectId
      },
      // 表单校验
      rules: {
        moduleName: [
          { required: false, trigger: "blur",
            validator: (rule, value, callback) => {
              if(this.addModuleMode && !value) {
                callback(new Error(this.$t('module.name-cannot-empty').toString())); // 空异常提示
                return;
              }
              callback();
            }
          },
        ],
        batchModuleNames: [
          {
            required: !this.addModuleMode,
            trigger: "blur",
            validator: (rule, value, callback) => {
              if(this.addModuleMode==false) {
                if(!this.strAddModules) {
                  callback(new Error(this.$t('module.name-cannot-empty').toString())); // 空异常提示
                  return;
                }
                let arr = this.strAddModules.split('\n');
                let lines = [];
                for(let i=0;i<arr.length;i++) {
                  if(arr[i].length>128) {
                    lines.push(i);
                  }
                }
                if(lines.length>0) {
                  callback(new Error(strFormat(this.$t('module.names-length-exception').toString(),[lines.join(','),128]))); // 行名称超过128个字符提示
                  return;
                }
              }
              callback();
            }
          }
        ]
      }
    }
  },
  props: {
    projectId: {
      type: Number,
      default: null
    }
  },
  computed: {
    moduleNameFormProp() {
      return (this.form.moduleId || this.addModuleMode) ? 'moduleName' : 'batchModuleNames'
    },
    moduleNameLabelText() {
      return (this.form.moduleId || this.addModuleMode)
        ? this.$t('module.name')
        : this.$t('module.names')
    }
  },
  methods: {
    getFixedFieldHints() {
      const hints = [
        {
          letter: 'F',
          badgeSelector: this.$_moduleNameHintBadgeSelector(),
          onActivate: () => this.$_focusModuleNameField()
        }
      ]
      if (!this.form.moduleId) {
        hints.push({
          letter: 'M',
          badgeSelector: this.$_moduleModeSwitchBadgeSelector(),
          onActivate: () => this.$_toggleModuleAddMode()
        })
      }
      return hints
    },
    $_moduleNameHintBadgeSelector() {
      return '.module-name-row .module-name-label-hint'
    },
    $_moduleModeSwitchBadgeSelector() {
      return '.module-name-row .module-mode-switch-hint'
    },
    $_focusModuleNameField() {
      const container = this.getFieldHintContainer()
      if (!container) return
      const host = container.querySelector('.module-name-row .module-name-hint-host')
      const input = host && (host.querySelector('textarea') || host.querySelector('input:not([type="hidden"])'))
      if (input && typeof this.$_focusControl === 'function') {
        this.$_focusControl(input)
      }
    },
    $_toggleModuleAddMode() {
      const container = this.getFieldHintContainer()
      if (!container) return
      const switchRoot = container.querySelector('.module-name-row .module-mode-switch-hint .el-switch')
      const switchCore = switchRoot && switchRoot.querySelector('.el-switch__core')
      if (switchCore) {
        switchCore.click()
      } else {
        this.addModuleMode = !this.addModuleMode
      }
      this.$_fieldHintMap = null
      this.$_fieldHintPending = null
      this.$_hideFieldHintBadges()
      this.$nextTick(() => {
        const nextInput = container.querySelector('.module-name-row .module-mode-switch-hint .el-switch__input')
        if (nextInput && typeof this.$_focusControl === 'function') {
          this.$_focusControl(nextInput)
        }
        if (this.$_modifierHeld) {
          this.$_refreshFieldHintsForViewport(true)
        }
      })
    },
    open(moduleId, modulePid) {
      this.getTreeselect();
      this.reset();
      if(moduleId) {
        getModule(moduleId).then(response => {
          this.form = response.data;
          this.visible = true;
          this.title = this.$i18n.t('module.modify');
        });
      } else {
        this.title = this.$i18n.t('module.new');
        this.form.modulePid = modulePid;
        this.visible = true;
      }
    },
    /** 转换模块数据结构 */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children;
      }
      return {
        id: node.moduleId,
        label: node.moduleName,
        children: node.children
      };
    },
    /** 查询模块下拉树结构 */
    getTreeselect() {
      let params = {
        projectId: this.projectId
      }
      listModule(params).then(response => {
        this.moduleOptions = [];
        const data = { moduleId: 0, moduleName: this.$i18n.t('module.root-node'), children: [] };
        data.children = this.handleTree(response.data, "moduleId", "modulePid");
        this.moduleOptions.push(data);
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.moduleId != null) {
            console.log(this.form, 'form')
            updateModule(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('modify-success'));
              this.doCloseToolDialog();
              this.$emit('updated',this.form)
            });
          } else {
            if(this.addModuleMode==false && this.strAddModules) {
              this.form.batchModuleNames = this.strAddModules.split('\n').filter(m=>m);
            }
            addModule(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('add-success'));
              this.doCloseToolDialog();
              this.$emit('added',response.data)
            });
          }
        }
      });
    },
    shortcutSave() {
      this.submitForm();
    },
    captureToolDialogCloseBaseline() {
      this.toolDialogCloseBaseline = JSON.stringify({
        form: { ...(this.form || {}) },
        addModuleMode: this.addModuleMode,
        strAddModules: this.strAddModules || ''
      })
    },
    isToolDialogCloseDirty() {
      if (!this.toolDialogCloseBaseline) return false
      return JSON.stringify({
        form: { ...(this.form || {}) },
        addModuleMode: this.addModuleMode,
        strAddModules: this.strAddModules || ''
      }) !== this.toolDialogCloseBaseline
    },
    doCloseToolDialog() {
      this.visible = false;
      this.toolDialogCloseBaseline = null;
      if (typeof this.reset === 'function') this.reset();
    },
    // 表单重置
    reset() {
      this.addModuleMode = true
      this.form = {
        moduleId: null,
        modulePid: null,
        moduleName: null,
        batchModuleNames: [],
        annexUrls: null,
        remark: null,
        projectId: this.projectId
      };
      this.strAddModules = '';
      this.resetForm("form");
    },
  }
}
</script>

<style lang="scss" scoped>
.module-name-row.el-form-item {
  display: flex;
  align-items: flex-start;

  &::before,
  &::after {
    display: none;
  }

  :deep(.el-form-item__label) {
    float: none;
    flex: 0 0 150px;
    width: 150px !important;
    display: flex;
    align-items: flex-start;
    justify-content: flex-end;
    padding-top: 0;
    line-height: 20px;
  }

  :deep(.el-form-item__content) {
    margin-left: 0 !important;
    flex: 1 1 auto;
    min-width: 0;
  }
}
.module-switch-title {
  box-sizing: border-box;
  width: 100%;
  display: flex;
  flex-flow: column nowrap;
  align-items: stretch;
  gap: 6px;
}
.module-name-label-hint,
.module-mode-switch-hint {
  position: relative;
  display: block;
  box-sizing: border-box;
  width: calc(100% + 12px);
  max-width: calc(100% + 12px);
  margin-right: -12px;
  padding-right: 12px;
  text-align: right;
}
.module-name-label-hint {
  margin: 0;
  padding-right: 12px;
  white-space: nowrap;
  line-height: 20px;
}
.module-name-required {
  color: #f56c6c;
  margin-right: 4px;
}
.module-mode-switch-hint {
  margin: 0 -12px 0 0;
  padding: 0 12px 0 0;
  line-height: 1;
}
.module-mode-switch-hint .el-switch {
  vertical-align: top;
}
</style>
