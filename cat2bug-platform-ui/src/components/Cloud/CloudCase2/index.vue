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
           :key="index">
          <template #content>
            <component
              :is="c.component"
              :ref="c.ref"
              :data="c.data"
              @change="toChatBottom"
              @submit="handleSubmit" />
          </template>
        </chat-message>
      </div>
      <div class="send">
        <div>
          <el-input
            type="textarea"
            placeholder="请输入创建测试用例需求,描述越详细，创建的用例越匹配，需求格式如下:
## 需求
一个网页系统的登陆页面
## 页面元素
1. 登陆名称(3-32个英文字符)
2. 登陆密码(3-32个数字或英文字符)
3. 手机验证码(4个英文字符)
4. 登陆按钮
## 测试场景
1. 验证每个元素的输入限制，每个元素的验证生成至少一条用例
2. 验证正常登陆流程
3. 验证异常登陆后是否出现提示信息
## 测试数据
1. 登陆名称: admin
2. 登陆密码: admin123
3. 手机验证码: abcd
## 限制
1. 不要生成（测试场景）之外的测试用例
2. 不要出现（页面元素）之外的元素
3. 测试用例数量不要少于3条
              "
            v-model="query.query"
            maxlength="65535"
            :rows="9"
            show-word-limit />
          <el-button type="success" @click="handleCreateCaseDescribe">创建用例描述</el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script>
import ChatMessage from "@/components/Cloud/CloudCase2/chat/ChatMessage";
import CaseDemand from "@/components/Cloud/CloudCase2/chat/CaseDemand";
import CaseMind from "@/components/Cloud/CloudCase2/chat/CaseMind";
import CaseList from "@/components/Cloud/CloudCase2/chat/CaseList";
import CaseHelloWorld from "@/components/Cloud/CloudCase2/chat/CaseHelloWorld";
import TextMessage from "@/components/Cloud/CloudCase2/chat/TextMessage";
const AI_CASE_QUERY_KEY = 'ai_case2_query_key';
export default {
  name: "index",
  components: { CaseDemand, CaseMind, CaseList, TextMessage, ChatMessage, CaseHelloWorld },
  data() {
    return {
      visible: false,
      query: {query: ''},
      chatList: [{
        component: 'CaseHelloWorld',
        ref: 'CaseHelloWorld0',
        member: {
          name: '用例机器人'
        },
        direction: 'left'
      }]
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
      this.readQuery();
    },
    close() {
      this.visible = false;
      this.$emit('close')
    },
    readQuery() {
      this.query.query = this.$cache.local.get(AI_CASE_QUERY_KEY)||'';
    },
    handleClose(done) {
      done();
    },
    handleAddUserChat() {
      const newChat = {
        component: 'TextMessage',
        ref: 'TextMessage'+(this.chatList.length+1),
        data: this.query.query,
        member: this.$store.state.user,
        direction: 'right'
      };
      this.chatList.push(newChat);
    },
    handleCreateCaseDescribe() {
      if(!this.query.query) {
        this.$message.error("请输入创建用例的需求后，在点击按钮创建用例描述!")
        return;
      }
      this.$cache.local.set(AI_CASE_QUERY_KEY, this.query.query);
      this.handleAddUserChat();
      const newChat = {
        component: 'CaseDemand',
        ref: 'CaseDemand'+(this.chatList.length+1),
        data: this.query.query,
        member: {
          name: '用例机器人'
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
          name: '用例机器人'
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
          name: '用例机器人'
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
  bottom: 0px;
  position: absolute;
  width: 100%;
  margin: 0px;
  background-color: white;
  > div {
    display: inline-flex;
    flex-direction: row;
    width: calc(100% - 60px);
    margin: 10px 30px 20px 30px;
    border: 1px solid #DCDFE6;
    border-radius: 4px;
  }
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
  gap: 50px;
  > * {
    flex-shrink: 0;
    border-bottom: 1px solid #F2F6FC;
    padding-bottom: 50px;
  }
  > *:last-child {
    flex-shrink: 0;
    border-bottom: 0px;
  }
}
</style>
