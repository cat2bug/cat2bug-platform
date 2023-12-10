<template>
  <div></div>
</template>

<script>
export default {
  name: "HotKey",
  data() {
    return {
      hotKeys: null,
      keyList: [],
      timer: null
    }
  },
  props: {
    iframeIds: {
      type: Array,
      default: ()=>[]
    }
  },
  /** 页面加载完成 */
  mounted() {
    this.hotKeys = new Map();
    this.hotKeys.set('screenShot',[16,65])
    // 添加事件
    document.addEventListener('keydown', this.onKeyDown);
    document.addEventListener('keyup', this.onKeyUp);
    this.resetFrameListener();
  },
  /** 页面释放 */
  beforeDestroy() {
    document.removeEventListener('keydown', this.onKeyDown);
    document.removeEventListener('keyup', this.onKeyUp);
    this.removeFrameListener();
  },
  methods: {
    removeFrameListener() {
      this.iframeIds.forEach(frameId=>{
        let frame = document.getElementById(frameId);
        if(frame){
          frame.contentWindow.document.removeEventListener('keydown', this.onKeyDown);
          frame.contentWindow.document.removeEventListener('keyup', this.onKeyUp);
        }
      })
    },
    resetFrameListener() {
      this.removeFrameListener();
      this.iframeIds.forEach(frameId=>{
        let frame = document.getElementById(frameId);
        if(frame){
          frame.contentWindow.document.addEventListener('keydown', this.onKeyDown);
          frame.contentWindow.document.addEventListener('keyup', this.onKeyUp);
        }
      })
    },
    onKeyDown(event){
      const e = event || window.event;
      let key = e.keyCode;
      if(!this.keyList.includes(key)) {
        this.keyList.push(key);
        event.preventDefault();
        this.downKeyHandle().then(res=>{
          alert(res)
        })
      }

      console.log(key,this.keyList)
    },
    onKeyUp(event){
      const e = event || window.event;
      let key = e.keyCode;
      if(this.keyList.indexOf(key)!==-1) {
        this.keyList.splice(this.keyList.indexOf(key),1);
        this.timer = null;
      }
    },
    downKeyHandle() {
      let _this = this;
      return new Promise((resolve=>{

        for(let [key,value] of _this.hotKeys){
          if(_this.checkValue(value,_this.keyList)) {
            if(_this.timer!=null) {
              // 防止多次触发
              return;
            }
            _this.timer = setTimeout(()=>{
              _this.timer = null;
            },5000)

            resolve(key);
          }
        }
      }))
    },
    checkValue(list,list2){
      if(list.length!=list2.length){
        console.log(list,list2,list.length,list2.length)
        return false;
      }
      console.log('---------')
      for(let i=0;i<list.length;i++){
        if(list[i]!=list2[i]){
          return false;
        }
      }
      return true;
    }
  }
}
</script>

<style scoped>

</style>
