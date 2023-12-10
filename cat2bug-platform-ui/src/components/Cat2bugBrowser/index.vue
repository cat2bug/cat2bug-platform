<template>
  <div class="cat2bug-browser">
    <div></div>
    <iframe id="main-iframe" :src="url" class="cat2bug-browser-iframe" security="restricted"> </iframe>
  </div>
</template>

<script>
export default {
  name: "Cat2bugBrowser",
  props: {
  },
  data() {
    return {
      // 访问的网址
      url: null
    }
  },
  computed:{
    screen: function (){
      return document.querySelector('.cat2bug-browser-iframe')
    }
  },
  methods: {
    /** 测试屏幕尺寸改变的处理 */
    setScreenSize(width,height){




      this.screen.style.transform = 'scale(0.5)';
      this.screen.style.width = width;
      this.screen.style.height = height;
    },
    open(url, width, height){
      this.url = url;
      if(width && height) {
        this.setScreenSize(width,height);
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.cat2bug-browser {
  position: absolute;
  width: 100%;
  height: 100%;
  position: absolute;
  z-index: 998;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #606266;
  overflow: auto;
}
.cat2bug-browser-iframe {
  z-index: 998;
  width: 100%;
  height: 100%;
  border-width: 0px;
}
</style>
