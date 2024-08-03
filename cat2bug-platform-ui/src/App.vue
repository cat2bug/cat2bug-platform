<template>
  <div id="app">
    <router-view />
    <theme-picker />
  </div>
</template>

<script>
import ThemePicker from "@/components/ThemePicker";

export default {
  name: "App",
  components: { ThemePicker },
  metaInfo() {
      return {
        taskId:null,
        title: this.$store.state.settings.dynamicTitle && this.$store.state.settings.title,
        titleTemplate: title => {
            return title ? `${title} - ${this.$i18n.t("system-name")}` : this.$i18n.t("system-name")
        }
      }
  },
  data() {
    return {
      wsUrl: null
    }
  },
  created() {
    this.welcome();
    this.initWebSocket();
  },
  methods: {
    /**
     * 欢迎语
     */
    welcome() {
      console.log(this.$i18n.t('welcome'));
    },
    /**
     * 初始化websocket
     */
    initWebSocket() {
      this.taskId = setInterval(()=>{
        const userId = this.$store.state.user.id;
        if(userId){
          this.wsUrl = (window.location.protocol === 'https:' ? `wss://${location.host}${process.env.VUE_APP_BASE_WEBSOCKET}/websocket/${userId}/message` : `ws://${location.host}${process.env.VUE_APP_BASE_WEBSOCKET}/websocket/${userId}/message`)
          this.$connect(this.wsUrl, { format: 'json' })
          this.$socket.onopen = this.webSocketOnOpen;//连接成功方法
          this.$socket.onerror = this.webSocketOnError;//报错方法
          this.$socket.onmessage = this.webSocketOnMessage;// 接收端返回或推送信息的方法
          this.$socket.onclose = this.webSocketClose;//关闭

          clearInterval(this.taskId);
        }
      },1000);

    },
    /**
     * 链接ws服务器，e.target.readyState = 0/1/2/3   0 CONNECTING ,1 OPEN, 2 CLOSING, 3 CLOSED
     * @param e
     */
    webSocketOnOpen(e) {
      // console.log('WebSocket连接成功', e);
    },
    /**
     * 接收端发送过来的信息，整个项目接收信息的唯一入口
     * @param e
     */
    webSocketOnMessage(e) {
      if (!e.data) {return;}
      let res = JSON.parse(e.data);
      // 端返回成功信息
      if (res.code == 200) {
        this.$topic.publish(res.action,res);
      }
    },
    webSocketOnError(e) {
      console.error(e);
    },
    webSocketClose(e) {
      console.log(e)
      this.initWebSocket();
    }
  }
};
</script>
<style scoped>
#app .theme-picker {
  display: none;
}
</style>
