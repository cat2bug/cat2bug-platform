<template>
  <el-drawer
    :visible.sync="visible"
    :direction="direction"
    size="90%"
    :before-close="handleClose">
<!--    标题-->
    <template slot="title">
      <div class="case-search-header">
        <div class="case-search-title">
          <i class="el-icon-arrow-left" @click="close"></i>
          <h4 class="case-search-title-name"><svg-icon icon-class="robot" style="margin-right: 10px;" />{{ $t('case.ai-create') }}</h4>
        </div>
        <div>
        </div>
      </div>
    </template>
    <div class="case-content">
      <div ref="chat" class="chat">
        <chat-message v-for="(c,index) in chatList"
           :message="c"
           :ref="c.ref"
           :key="index"
           @change="toChatBottom"
           @submit="handleSubmit" />
      </div>
      <div class="send">
        <el-input
          type="textarea"
          placeholder="请输入内容"
          v-model="query.query"
          maxlength="65535"
          :rows="5"
          show-word-limit />
        <el-button @click="handle">创建用例描述</el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script>
import ChatMessage from "@/components/Cloud/CloudCase2/chat/ChatMessage";
export default {
  name: "index",
  components: { ChatMessage },
  data() {
    return {
      visible: false,
      query: {query: '登陆页面'},
      chatList: []
    }
  },
  computed: {
  },
  props: {
    direction: {
      type: String,
      default: 'rtl'
    }
  },
  methods: {
    open() {
      this.visible = true;
    },
    close() {
      this.visible = false;
      this.$emit('close')
    },
    handleClose(done) {
      // done();
    },
    handleAddUserChat() {
      const newChat = {
        component: 'Text',
        ref: 'Text'+(this.chatList.length+1),
        data: null,
        member: this.$store.state.user,
        direction: 'right'
      };
      this.chatList.push(newChat);
      this.toChatBottom();
    },
    handle() {
      this.handleAddUserChat();

      const newChat = {
        component: 'CaseDemand',
        ref: 'CaseDemand'+(this.chatList.length+1),
        data: null,
        member: {
          nickName: 'AI'
        },
        direction: 'left'
      };
      this.chatList.push(newChat);
      this.toChatBottom();
    },
    toChatBottom() {
      this.$nextTick(()=>{
        const chat = this.$refs.chat;
        chat.scrollTop = chat.scrollHeight;
      });
    },
    addCaseMind(data) {
      const newChat = {
        component: 'CaseMind',
        ref: 'CaseMind'+(this.chatList.length+1),
        data: data,
        member: {
          nickName: 'AI'
        },
        direction: 'left'
      };
      this.chatList.push(newChat);
      this.toChatBottom();
    },
    addCaseList(data) {
      const newChat = {
        component: 'CaseList',
        ref: 'CaseList'+(this.chatList.length+1),
        data: data,
        member: {
          nickName: 'AI'
        },
        direction: 'left'
      };
      this.chatList.push(newChat);
      this.toChatBottom();
    },
    handleSubmit(type, data) {
      switch (type) {
        case 'CaseDemand':
          this.addCaseMind(data);
          break;
        case 'CaseMind':
          this.addCaseList(data);
          break;
        case 'CaseList':
          this.$emit('added');
          break;
      }
    }
  }
}
</script>
<style lang="scss" scoped>
.case-search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-direction: row;
  .case-search-title {
    display: inline-flex;
    justify-content: flex-start;
    align-items: center;
    flex-direction: row;
    .el-icon-arrow-left {
      font-size: 22px;
    }
    .el-icon-arrow-left:hover {
      cursor: pointer;
      color: #909399;
    }
    .case-search-title-name {
      max-width: 400px;
      overflow: hidden;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      font-size: 20px;
      color: #303133;
      font-weight: 500;
      margin-top: 10px;
      margin-bottom: 10px;
    }
    > * {
      margin-right: 10px;
    }
  }
}
.case-content {
  position: relative;
  width: 100%;
  height: 100%;
}
.send {
  z-index: 9;
  bottom: 20px;
  position: absolute;
  width: calc(100% - 60px);
  display: inline-flex;
  flex-direction: row;
  margin: 10px 30px;
  border: 1px solid #DCDFE6;
  border-radius: 4px;
  .el-input {
    flex: 1;
  }
  ::v-deep textarea {
    border-width: 0px;
  }
  .el-button {
    flex-shrink: 0;
    border-width: 0px;
    border-left: 1px solid #DCDFE6;
  }
}
.chat {
  position: absolute;
  overflow-y: auto;
  width: 100%;
  height: calc(100% - 0px);
  top: 0px;
  bottom: 0px;
  padding: 10px 20% 200px 20%;
  display: inline-flex;
  flex-direction: column;
  gap: 80px;
  > * {
    flex-shrink: 0;
  }
}
</style>
