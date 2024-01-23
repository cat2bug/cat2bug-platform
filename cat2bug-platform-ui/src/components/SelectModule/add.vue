<template>
  <div>
    <el-button v-show="!formVisible" type="text" icon="el-icon-plus" class="select-module-add-full select-module-add-button" @click="formVisible=!formVisible">{{$t('module.create')}}</el-button>
    <el-form v-show="formVisible" ref="form" :model="form" :rules="rules" label-width="0" class="select-module-add">
      <el-form-item prop="moduleName" label-width="0">
        <el-input
          :placeholder="$t('module.enter-module-name')"
          v-model="name" @input="nameChangeHandle">
          <el-button slot="append" icon="el-icon-plus" size="mini"
                     @click="addProjectModule"></el-button>
        </el-input>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import {addModule} from "@/api/system/module";

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
    setFormVisible(visible) {
      this.formVisible = visible;
    },
    nameChangeHandle(val){
      this.$emit('input',val);
    },
    addProjectModule(){
      console.log('------',this.name)
      const that = this;
      this.form = {
        modulePid: this.modulePid||0,
        moduleName: this.name,
        projectId: this.projectId
      }
      this.$nextTick(()=>{
        this.$refs["form"].validate(valid => {
          if (valid) {
            addModule(this.form).then(res=>{
              that.$emit('added',this.name);
              that.formVisibmpe = false;
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
  .select-module-add {
    .el-form-item {
      margin-top: 10px;
      margin-bottom: 0px;
    }
  }
  .select-module-add-full {
    width: 100%;
  }
  .select-module-add-button {
    padding: 5px 0px;
  }
  .select-module-add-button:hover {
    background-color: #F2F6FC;
    border-radius: 5px;
    cursor: pointer;
  }
</style>
