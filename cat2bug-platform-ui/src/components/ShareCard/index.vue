<template>
  <el-popover
    placement="top"
    v-model="visible">
    <div class="shard-context" :loading="apiLoading">
      <el-form :inline="true" :model="form">
        <el-form-item :label="$t('shard.aging-hour')">
          <el-select v-model="form.agingHour" size="mini" @change="refresh">
            <el-option :label="$t('shard.1hour')" value="1"></el-option>
            <el-option :label="$t('shard.1day')" value="24"></el-option>
            <el-option :label="$t('shard.7day')" value="168"></el-option>
            <el-option :label="$t('shard.permanent')" value="0"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('password')">
          <el-input v-model="form.password" size="mini" clearable :placeholder="$t('shard.please-enter-password')" minlength="4" maxlength="4" @change="refresh"></el-input>
        </el-form-item>
      </el-form>
      <div class="title">
        <label>{{ $t('shard.click-prompt') }}</label>
      </div>
      <div class="share-list">
        <component
          v-loading="loading[index]"
          :agingHour="form.agingHour"
          :password="form.password"
          :element-loading-text="$t('click-copy')"
          element-loading-background="rgba(255, 255, 255, 0.5)"
          element-loading-spinner="el-icon-document-copy"
          class="share-card-loading"
          ref="shareComponent"
          :is="sc.name"
          :params="params"
          v-for="(sc,index) in shareCardList"
          :key="index"
          @click.native="copy(index)"
          @mouseenter.native="onMouseEnterHandle(index)"
          @mouseleave.native="onMouseLeaveHandle(index)"
          @copy="copyHandle"
        />
      </div>
    </div>
    <svg-icon class="button" icon-class="shard" slot="reference"
              @click="clickHandle"></svg-icon>
  </el-popover>
</template>

<script>
import {getShard, shardDefect} from "@/api/system/DefectShard";

const path = require('path');
const files = require.context('./DefectShareCard/', true, /\.vue$/);
const modules = {};

// 动态加载组件
files.keys().forEach(key=>{
  const name = path.basename(key,'.vue');
  modules[name] = files(key).default||files(key)
});

export default {
  name: "ShareCard",
  components: modules,
  data() {
    return {
      apiLoading: false,
      form: {
        defectId: this.params.defectId,
        agingHour: '1',
        password: null
      },
      shard: null,
      visible: false,
      loading: Object.values(modules).map(l=>false),
      shareCardList: Object.values(modules)
    }
  },
  props: {
    params: {
      type: Object,
      default: ()=> {}
    }
  },
  watch: {
    visible: function (n) {
      if(n) {
        this.refresh();
      }
    }
  },
  created() {
    this.getShardInfo(this.params.defectId);
  },
  methods: {
    /** 刷新 */
    refresh() {
      this.form.defectId = this.params.defectId;
      shardDefect(this.form).then(res=>{
        this.shard = res.data;
        this.refreshComponents();
      });
    },
    /** 刷新分享组件 */
    refreshComponents() {
      this.$refs.shareComponent.forEach(c=>{
        c.refresh(this.shard);
      });
    },
    /** 复制数据到剪贴板 */
    copy(index) {
      let self = this;
      setTimeout( ()=>{
        self.$refs.shareComponent[index].copy(self.shard, self.form.agingHour, self.form.password);
        self.visible = false;
      },0);
    },
    /** 复制剪贴板成功处理事件 */
    copyHandle() {
      this.$message.success(this.$i18n.t('copy-success').toString());
    },
    /** 点击分享按钮事件 */
    clickHandle(e) {
      e.stopPropagation();
    },
    /** 鼠标进入分享组件效果 */
    onMouseEnterHandle(index) {
      this.$set(this.loading,index,true);
    },
    /** 鼠标离开分享组件效果 */
    onMouseLeaveHandle(index) {
      this.$set(this.loading,index,false);
    },
    /** 获取共享信息 */
    getShardInfo(defectId) {
      if(defectId) {
        getShard(defectId).then(res=>{
          if(res.data) {
            this.form = res.data;
            this.form.agingHour = this.form.agingHour?this.form.agingHour + '':'1';
          }
        });
      }
    }
  }
}
</script>
<style>
.share-card-loading .el-icon-document-copy {
  font-size: 40px;
}
</style>
<style lang="scss" scoped>
  .shard-context {
    > * {
      margin: 0 5px 5px 5px;
    }
    > .title {
      background-color: #f4f4f4;
      border-radius: 3px;
      padding: 3px 5px;
    }
  }
  .share-list {
    display: inline-flex;
    flex-direction: row;
  }
  .share-list > * {
    position: relative;
    margin-left: 5px;
    overflow: hidden;
  }
  .share-list > *:first-child {
    margin-left: 0px;
  }
  .button:hover {
    cursor: pointer;
  }
  .button {
    color: #8c8c8c;
  }
  .el-form-item {
    margin-bottom: 0px;
  }
</style>
