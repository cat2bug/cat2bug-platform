<template>
  <el-dialog
    title="通知设置"
    :visible.sync="dialogVisible"
    width="40%"
    :close-on-click-modal="false"
    :before-close="handleClose">
    <el-tabs v-model="activeTabName" v-loading="loading">
      <el-tab-pane label="发送选项" name="type">
        <div v-for="(m,index) in moduleList">
          <component
            v-model="option.config.modules[m.name]"
            :is="m.name"
            :key="index"
          />
          <el-divider></el-divider>
        </div>
      </el-tab-pane>
      <el-tab-pane label="接收平台" name="platform">
        <div class="option-body">
          <div v-for="(p,index) in platformList">
            <component
              v-model="option.config.platforms[p.name]"
              :is="p.name"
              :key="index"
            />
            <el-divider></el-divider>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <span slot="footer" class="dialog-footer">
    <el-button @click="dialogVisible = false">{{ $t('cancel') }}</el-button>
    <el-button type="primary" @click="handleSaveOption">{{ $t('save') }}</el-button>
  </span>
  </el-dialog>
</template>

<script>

import {getIMConfig, saveConfig} from "@/api/im/config";

const path = require('path');
const moduleFiles = require.context('./module/', true, /\.vue$/);
const modules = {};
const allModuleList = [];
// 动态加载组件
moduleFiles.keys().forEach(key=>{
  const name = path.basename(key,'.vue');
  allModuleList.push({
    name: name,
  });
  modules[name] = moduleFiles(key).default||moduleFiles(key)
});

const platformFiles = require.context('./platform/', true, /\.vue$/);
const allPlatformList = [];
// 动态加载组件
platformFiles.keys().forEach(key=>{
  const name = path.basename(key,'.vue');
  allPlatformList.push({
    name: name,
  });
  modules[name] = platformFiles(key).default||platformFiles(key)
});

export default {
  name: "OptionNotice",
  components: modules,
  data() {
    return {
      loading: false,
      dialogVisible: false,
      activeTabName: 'type',
      option: {
        config: {
          modules: {},
          platforms: {}
        }
      },
      moduleList: allModuleList,
      platformList: allPlatformList,
    }
  },
  mounted() {
    this.getConfig();
  },
  methods: {
    reset() {
      this.activeTabName = 'type';
    },
    /** 获取项目ID */
    getProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 获取用户id */
    getUserId() {
      return this.$store.state.user.id;
    },
    /** 获取配置 */
    getConfig() {
      this.loading = true;
      getIMConfig(this.getProjectId(), this.getUserId()).then(res=>{
        this.loading = false;
        this.option = res.data;
      }).catch(e=>{
        this.loading = false;
      });
    },
    /** 打开配置窗口 */
    open() {
      this.dialogVisible = true;
    },
    /** 处理关闭窗口时的操作 */
    handleClose(done) {
      done();
    },
    /** 处理保存配置操作 */
    handleSaveOption() {
      saveConfig(this.option).then(res=>{
        this.$message.success(this.$i18n.t('save-success').toString());
        this.dialogVisible = false;
        this.reset();
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.option-body {
  padding: 0px 10px;
  .svg-icon {
    margin-right: 5px;
  }
  .option-item {
    span {
      margin-right: 10px;
    }
  }
}
.el-divider {
  margin: 10px 0px 15px 0px;
}
.row {
  display: inline-flex;
  flex-direction: row;
  gap: 10px;
}
.col {
  display: inline-flex;
  flex-direction: column;
}
.margin-right-30 {
  margin-right: 30px;
}
</style>
