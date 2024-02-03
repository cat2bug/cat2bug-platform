<template>
  <el-dialog :title="title" :visible.sync="visible" width="600px" append-to-body>
    <el-form ref="form" rules="rules" :model="form" :rules="rules" label-width="150px">
      <el-form-item :label="$t('module.parent-module')" prop="modulePid">
          <treeselect v-model="form.modulePid" :options="moduleOptions" :normalizer="normalizer" :placeholder="$t('module.please-select-parent-module')" />
      </el-form-item>
      <el-form-item prop="moduleName" v-show="form.moduleId || addModuleMode">
        <template slot="label">
          <div class="module-switch-title">
            <label>{{ $t('module.name') }}</label>
            <el-switch
              v-show="!form.moduleId"
              v-model="addModuleMode"
              active-color="#13ce66"
              inactive-color="#ff4949">
            </el-switch>
          </div>
        </template>
        <el-input v-model="form.moduleName" :placeholder="$t('module.enter-module-name')" maxlength="128" />
      </el-form-item>
      <el-form-item prop="batchModuleNames" v-show="!form.moduleId && !addModuleMode">
        <template slot="label">
          <div class="module-switch-title">
            <label>{{ $t('module.names') }}</label>
            <el-switch
              v-model="addModuleMode"
              active-color="#13ce66"
              inactive-color="#ff4949">
            </el-switch>
          </div>
        </template>
        <el-input type="textarea" rows="8" v-model="strAddModules" :placeholder="$t('module.enter-module-names')" maxlength="65536" show-word-limit />
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
      <el-button type="primary" @click="submitForm">{{ $t('ok') }}</el-button>
      <el-button @click="cancel">{{ $t('cancel') }}</el-button>
    </div>
  </el-dialog>
</template>

<script>
import {addModule, getModule, listModule, updateModule} from "@/api/system/module";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import Label from "@/components/Cat2BugStatistic/Components/Label";
import {strFormat} from "@/utils";
export default {
  name: "ModuleDialog",
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
  methods: {
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
        this.title = this.$i18n.t('module.add');
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
            updateModule(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('modify-success'));
              this.visible = false;
              this.$emit('updated',this.form)
            });
          } else {
            if(this.addModuleMode==false && this.strAddModules) {
              this.form.batchModuleNames = this.strAddModules.split('\n').filter(m=>m);
            }
            addModule(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('add-success'));
              this.visible = false;
              this.$emit('added',response.data)
            });
          }
        }
      });
    },
    // 取消按钮
    cancel() {
      this.visible = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        moduleId: null,
        modulePid: null,
        moduleName: null,
        batchModuleNames: [],
        remark: null,
        projectId: this.projectId
      };
      this.strAddModules = null;
      this.resetForm("form");
    },
  }
}
</script>

<style lang="scss" scoped>
.module-switch-title {
  display: inline-grid;
  flex-direction: column;
  justify-content: center;
  align-items: flex-end;
  .el-switch {
    display: inline;
  }
}
</style>
