<template>
  <el-dialog
    :title="$t('notice.option')"
    :visible.sync="dialogVisible"
    width="60%"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :before-close="onToolDialogBeforeClose"
    @opened="onToolDialogOpened">
    <el-tabs v-model="activeTabName" class="notice-option-hint-tabs" v-loading="loading">
      <el-tab-pane name="type">
        <span slot="label" class="notice-option-main-tab-type">{{ $t('notice.send-option') }}</span>
        <div v-for="(m,index) in moduleList">
          <component
            v-model="option.config.modules[m.name]"
            :is="m.name"
            :key="index"
          />
          <el-divider></el-divider>
        </div>
      </el-tab-pane>
      <el-tab-pane name="platform">
        <span slot="label" class="notice-option-main-tab-platform">{{ $t('notice.receive-platform-option') }}</span>
        <el-alert
          v-if="contactProfileHintVisible"
          type="info"
          :closable="false"
          show-icon
          class="notice-contact-hint"
          :title="$t('notice.contact-profile-hint')"
        />
        <el-tabs v-model="activePlatformName" class="notice-option-platform-hint-tabs" type="border-card">
          <el-tab-pane name="asystem">
            <span slot="label" class="notice-option-platform-tab-asystem">{{ $t('system.internal-notification') }}</span>
            <component
              ref="platform-asystem"
              v-model="option.config.platforms.asystem"
              is="asystem"
            />
          </el-tab-pane>
          <el-tab-pane name="bmail">
            <span slot="label" class="notice-option-platform-tab-bmail">{{ $t('email') }}</span>
            <component
              ref="platform-bmail"
              v-model="option.config.platforms.bmail"
              is="bmail"
            />
          </el-tab-pane>
          <el-tab-pane v-for="(p,index) in externalPlatformList" :key="index" :name="p.name">
            <span slot="label" :class="'notice-option-platform-tab-' + p.name">{{ getPlatformLabel(p.name) }}</span>
            <component
              :ref="`platform-${p.name}`"
              v-model="option.config.platforms[p.name]"
              :is="p.name"
            />
          </el-tab-pane>
        </el-tabs>
      </el-tab-pane>
    </el-tabs>

    <span slot="footer" class="dialog-footer">
    <el-button @click="requestCloseToolDialog">{{ $t('cancel') }}</el-button>
    <el-button class="defect-kbd-hint-host" type="primary" @click="handleSaveOption">
      {{ $t('save') }}
      <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
    </el-button>
  </span>
  </el-dialog>
</template>

<script>

import {getIMConfig, saveConfig} from "@/api/im/config";
import { getUserProfile } from "@/api/system/user";

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

import noticeOptionDialogKbd from '@/mixins/notice-option-dialog-kbd'

export default {
  name: "OptionNotice",
  mixins: [noticeOptionDialogKbd],
  components: modules,
  data() {
    return {
      loading: false,
      dialogVisible: false,
      activeTabName: 'type',
      activePlatformName: 'asystem',
      option: {
        config: {
          modules: {},
          platforms: {}
        }
      },
      moduleList: allModuleList,
      platformList: allPlatformList,
      profileContact: {
        phoneNumber: '',
        email: ''
      }
    }
  },
  computed: {
    externalPlatformList() {
      return this.platformList.filter(p => p.name !== 'asystem' && p.name !== 'bmail');
    },
    contactProfileHintVisible() {
      const phone = (this.profileContact.phoneNumber || '').trim();
      const email = (this.profileContact.email || '').trim();
      return !phone || !email;
    }
  },
  mounted() {
    // this.getConfig();
  },
  methods: {
    /** 重制表单 */
    reset() {
      this.activeTabName = 'type';
      this.activePlatformName = 'asystem';
      // 重制子组件表单
      this.platformList.forEach(p=>{
        let com = this.$refs['platform-'+p.name];
        if (com && com[0]) {
          com[0].resetFields();
        }
      });
    },
    /** 获取平台标签名称 */
    getPlatformLabel(name) {
      const labels = {
        'ding': this.$i18n.t('ding'),
        'feishu': this.$i18n.t('feishu'),
        'wechat': this.$i18n.t('enterprise-wechat')
      };
      return labels[name] || name;
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
        this.$nextTick(() => this.captureToolDialogCloseBaseline());
      }).catch(e=>{
        this.loading = false;
      });
    },
    loadProfileContact() {
      getUserProfile().then(res => {
        const user = (res && res.data) ? res.data : {};
        this.profileContact = {
          phoneNumber: user.phoneNumber || '',
          email: user.email || ''
        };
      }).catch(() => {});
    },
    /** 打开配置窗口 */
    open() {
      this.dialogVisible = true;
      this.$nextTick(()=>{
        this.reset();
        this.loadProfileContact();
        this.getConfig();
      });
    },
    /** 处理保存配置操作 */
    async handleSaveOption() {
      const validatePromises = [];

      // 验证 asystem 和 bmail（在 system 标签页中）
      const asystemCom = this.$refs['platform-asystem'];
      if (asystemCom && asystemCom.validate) {
        validatePromises.push(asystemCom.validate());
      }

      const bmailCom = this.$refs['platform-bmail'];
      if (bmailCom && bmailCom.validate) {
        validatePromises.push(bmailCom.validate());
      }

      // 验证其他外部平台
      this.externalPlatformList.forEach(v => {
        let com = this.$refs['platform-'+v.name];
        if (com && com[0] && com[0].validate) {
          validatePromises.push(com[0].validate());
        }
      });

      Promise.all(validatePromises).then(res => {
        if (res.every(v => v)) {
          saveConfig(this.option).then(res=>{
            this.$message.success(this.$i18n.t('save-success').toString());
            this.toolDialogCloseBaseline = null;
            this.dialogVisible = false;
            this.reset();
          })
        } else {
          this.$message.error(this.$i18n.t('notice.option.save-error').toString());
        }
      }).catch(() => {
        this.$alert('请稍后重试', '提示', {
          dangerouslyUseHTMLString: true
        });
      });
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
.notice-contact-hint {
  margin-bottom: 12px;
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
.notice-option-hint-tabs ::v-deep .el-tabs__item,
.notice-option-platform-hint-tabs ::v-deep .el-tabs__item {
  position: relative;
  overflow: visible !important;
}
.notice-option-hint-tabs ::v-deep .el-tabs__header,
.notice-option-platform-hint-tabs ::v-deep .el-tabs__header {
  overflow: visible !important;
}
</style>
