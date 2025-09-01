<template>
  <div class="browser-input">
    <div class="browser-input-body">
      <h1 class="browser-input-title">Cat2Bug-BrowserTest</h1>
      <div class="browser-input-content">
        <span class="browser-input-url">{{$i18n.t('browser.website')}}</span>
        <el-popover
          v-model="urlPopoverVisible"
          placement="bottom-start"
          popper-class="browser-input-url-input-list"
          trigger="click">
          <el-row>
            <el-col :span="24" v-for="(url,index) in oldUrlOptions" :key="index" @click.native="clickUrlHandle(url)">
              <div v-html="html(url)">{{url}}</div>
            </el-col>
          </el-row>
          <el-input slot="reference" v-model="selectUrl"
                    @click="urlChangedHandle"
                    @input="urlChangedHandle"
                    @keyup.enter.native="inputHandle"
                    :placeholder="$i18n.t('browser.please-enter-testing-website')"></el-input>
        </el-popover>
        <el-button :disabled="!selectUrl" type="primary" @click="inputHandle">{{$i18n.t('browser.access')}}</el-button>
      </div>
      <p class="browser-input-prompt">{{$i18n.t('browser.please-enter-testing-website-prompt')}}</p>
    </div>
  </div>
</template>

<script>
export default {
  name: "BrowserInput",
  model: {
    prop: 'url',
    event: 'input'
  },
  props: {
    url: {
      type: String,
      default: null
    },
    maxUrlCount: {
      type: Number,
      default: 20
    }
  },
  computed: {
    oldUrlOptions: function (){
      let list = this.getBrowserUrlList();
      if(this.selectUrl){
        return list.filter(url=>url && url.toLowerCase().indexOf(this.selectUrl.toLowerCase())>-1);
      } else {
        return list;
      }
    },
    html: function() {
      return function (value){
        if(!this.selectUrl || !value){
          return value;
        }
        let posList = [];
        let len = value.length;
        let pos = 0;
        while (pos < len) {
          pos = value.toLowerCase().indexOf(this.selectUrl.toLowerCase(), pos);
          if (pos === -1)
          break;
          posList.push(pos);
          pos = pos + 1;
        }
        let html = [];
        let index = 0;
        if(posList.length>0){
          for(let i=0;i<posList.length;i++){
            html.push('<span>'+value.substring(index,posList[i])+'</span>');
            html.push('<span style="color:red;">'+value.substring(posList[i],posList[i]+this.selectUrl.length)+'</span>');
            index=posList[i];
          }
          html.push('<span>'+value.substring(posList[posList.length-1]+this.selectUrl.length)+'</span>');
          return html.join('');
        } else {
          return value;
        }
      }
    }
  },
  data() {
    return {
      selectUrl: this.getLastBrowserUrl() || this.url,
      urlPopoverVisible: false,
    }
  },
  mounted() {
    window.addEventListener('beforeunload', e => this.beforeunloadHandler(e))
  },
  methods: {
    beforeunloadHandler(e) {
      this.saveLastBrowserUrl(this.selectUrl);
    },
    /** 输入网址改变的处理 */
    urlChangedHandle(e){
      this.urlPopoverVisible = this.oldUrlOptions.length>0;
    },
    /** 获取当前页面最后一次访问的页面 */
    getLastBrowserUrl(){
      return  this.$cache.session.get('toolsBrowserLastUrl');
    },
    /** 保存当前页面最后一次访问的页面 */
    saveLastBrowserUrl(url) {
      this.$cache.session.set('toolsBrowserLastUrl', url);
    },
    /** 获取浏览器网址列表 */
    getBrowserUrlList(){
      let list = this.$cache.local.getJSON('toolsBrowserUrlList');
      if(!list) {
        list = [];
      }
      return list;
    },
    /** 保存浏览器网址列表 */
    saveBrowserUrlList(list){
      this.$cache.local.setJSON('toolsBrowserUrlList', list)
    },
    /** 点击网址的处理 */
    clickUrlHandle(url){
      this.urlPopoverVisible = false;
      this.selectUrl = url;
      this.inputHandle();
    },
    /** 输入网址的处理 */
    inputHandle(e){
      let list = this.getBrowserUrlList();
      list = list.filter(u=>u!=this.selectUrl);
      list.unshift(this.selectUrl);
      if(list.length>this.maxUrlCount){
        list.splice(this.maxUrlCount-1,list.length-this.maxUrlCount);
      }

      this.saveBrowserUrlList(list);
      this.saveLastBrowserUrl(this.selectUrl);
      this.$emit('input',this.selectUrl);
    }
  }
}
</script>

<style lang="scss" scoped>
.browser-input {
  position: absolute;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  width: 100%;
  .browser-input-body {
    position: absolute;
    display: flex;
    justify-content: flex-start;
    align-items: flex-start;
    flex-direction: column;
    width: 50%;
    margin-bottom: 150px;
    .browser-input-content{
      display: flex;
      justify-content: center;
      align-items: center;
      flex-direction: row;
      padding: 0px 0px 0px 15px;
      border: #DCDFE6 1px solid;
      border-radius: 5px;
      width: 100%;
      .browser-input-url {
        flex-shrink: 0;
        margin-right: 10px;
        color: #606266;
        border-right: #DCDFE6 1px solid;
        padding-right: 15px;
      }
      > *:nth-child(2) {
        flex-shrink: 1;
        width: 100%;
      }
      .el-button {
        border-radius: 0px 5px 5px 0px;
      }
      ::v-deep input {
        border-width: 0px;
        padding-right: 0px;
      }
    }
    .browser-input-content:hover {
      border-color: #C0C4CC;
    }
    .browser-input-prompt {
      font-size: 12px;
      color: #909399;
    }
    .browser-input-title {

    }
  }
}
</style>
<style lang="scss">
.browser-input-url-input-list {
  max-width: 60%;
  .el-col {
    padding: 5px 10px;
  }
  .el-col:hover {
    background-color: #F2F6FC;
    cursor: pointer;
  }
}
</style>
