<template>
  <el-popover
    class="share-card-popover"
    placement="top"
    trigger="click"
    :teleported="true"
    popper-class="share-card-popper"
    :width="420"
    v-model:visible="visible">
    <div v-if="visible" class="shard-context" v-loading="apiLoading">
      <el-form :inline="true" :model="form">
        <el-form-item :label="$t('shard.aging-hour')">
          <el-select v-model="form.agingHour" size="small" @change="refresh">
            <el-option :label="$t('shard.1hour')" value="1"></el-option>
            <el-option :label="$t('shard.1day')" value="24"></el-option>
            <el-option :label="$t('shard.7day')" value="168"></el-option>
            <el-option :label="$t('shard.permanent')" value="0"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('password')">
          <el-input v-model="form.password" size="small" clearable :placeholder="$t('shard.please-enter-password')" minlength="4" maxlength="4" @change="refresh"></el-input>
        </el-form-item>
      </el-form>
      <el-button link :icon="$legacyIcon('el-icon-close')"></el-button>
      <div class="title">
        <label>{{ $t('shard.click-prompt') }}</label>
      </div>
      <div class="share-list">
        <span
          v-for="(sc,index) in shareCardList"
          :key="index"
          v-loading="loading[index]"
          :element-loading-text="$t('click-copy')"
          element-loading-background="rgba(255, 255, 255, 0.5)"
          class="share-card-loading"
          @click="copy(index)"
          @mouseenter="onMouseEnterHandle(index)"
          @mouseleave="onMouseLeaveHandle(index)"
        >
          <component
            ref="shareComponent"
            :is="sc.name"
            :params="params"
            :agingHour="form.agingHour"
            :password="form.password"
            @copy="copyHandle"
          />
        </span>
      </div>
    </div>
    <template #reference>
      <span class="share-card-trigger" @click="clickHandle">
        <svg-icon class="button" icon-class="shard"></svg-icon>
      </span>
    </template>
  </el-popover>
</template>

<script>
import {getShard, shardDefect} from "@/api/system/DefectShard";

const shareCardModules = import.meta.glob('./DefectShareCard/*.vue', { eager: true })
const modules = {}

for (const [filePath, mod] of Object.entries(shareCardModules)) {
  const name = filePath.split('/').pop().replace(/\.vue$/, '')
  modules[name] = mod.default || mod
}

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
  methods: {
    /** 刷新 */
    refresh() {
      this.apiLoading = true;
      this.form.defectId = this.params.defectId;
      shardDefect(this.form).then(res=>{
        this.shard = res.data;
        this.refreshComponents();
        this.apiLoading = false;
      }).catch(()=>this.apiLoading = true);
    },
    /** 刷新分享组件 */
    refreshComponents() {
      const refs = this.$refs.shareComponent
      if (!refs) return
      const list = Array.isArray(refs) ? refs : [refs]
      list.forEach((c) => {
        if (c && typeof c.refresh === 'function') {
          c.refresh(this.shard)
        }
      })
    },
    /** 复制数据到剪贴板 */
    copy(index) {
      const refs = this.$refs.shareComponent
      const comp = Array.isArray(refs) ? refs[index] : refs
      if (!comp || typeof comp.copy !== 'function') return
      setTimeout(() => {
        comp.copy(this.shard, this.form.agingHour, this.form.password)
        this.visible = false
      }, 0)
    },
    /** 复制剪贴板成功处理事件 */
    copyHandle() {
      this.$message.success(this.$i18n.t('copy-success').toString());
    },
    /** 点击分享按钮事件 */
    clickHandle(e) {
      // this.getShardInfo(this.params.defectId);
      e.stopPropagation();
    },
    /** 鼠标进入分享组件效果 */
    onMouseEnterHandle(index) {
      this.loading = this.loading.map((v, i) => (i === index ? true : v));
    },
    /** 鼠标离开分享组件效果 */
    onMouseLeaveHandle(index) {
      this.loading = this.loading.map((v, i) => (i === index ? false : v));
    },
    /** 获取共享信息 */
    getShardInfo(defectId) {
      if(defectId) {
        getShard(defectId).then(res=>{
          // this.apiLoading = false;
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
html.dark .share-card-loading .el-loading-mask {
  background-color: rgba(0, 0, 0, 0.45) !important;
}
.el-popover.el-popper.share-card-popper {
  width: 420px !important;
  min-width: 320px !important;
  max-width: min(480px, calc(100vw - 32px)) !important;
  box-sizing: border-box;
}
</style>
<style lang="scss" scoped>
  .shard-context {
    > * {
      margin: 0 5px 5px 5px;
    }
    > .title {
      background-color: var(--share-card-title-bg, #f4f4f4);
      border-radius: 3px;
      padding: 3px 5px;
      color: var(--text-color-regular);

      label {
        color: inherit;
      }
    }
  }
  .share-list {
    display: inline-flex;
    flex-direction: row;
  }
  .share-list > span {
    position: relative;
    margin-left: 5px;
    overflow: hidden;
    display: inline-flex;
  }
  .share-list > span:first-child {
    margin-left: 0px;
  }
  .share-card-trigger {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 auto;
    width: 14px;
    max-width: 14px;
    min-width: 0;
    overflow: hidden;
    line-height: 1;
    vertical-align: middle;
    cursor: pointer;
  }

  .button {
    color: var(--text-color-secondary, #8c8c8c);
    font-size: 14px;
  }
  .el-form-item {
    margin-bottom: 0px;
  }
</style>
