<template>
  <cat2-bug-card :title="$i18n.t('defect.online-member').toString()" v-loading="loading" :tools="tools" @tools-click="toolsHandle">
    <template slot="content">
      <div class="member-list">
        <cat2-bug-avatar class="click" :member="member" :online="true" v-for="member in memberList" :key="member.userId" @click.native="clickHandle(member)" />
      </div>
    </template>
  </cat2-bug-card>
</template>

<script>
import Cat2BugCard from "../Components/Card"
import Cat2BugAvatar from "@/components/Cat2BugAvatar";

import {listMemberOfProject} from "@/api/system/project";

export default {
  name: "DefectMemberOnline",
  components: {Cat2BugCard,Cat2BugAvatar},
  data() {
    return {
      onlineTopicId: null,
      offlineTopicId: null,
      loading: false,
      memberList: [],
      // 成员查询参数
      queryMember: {
        pageNum: 1,
        pageSize: 99999,
        roleId: null,
        params: {
          search: null
        }
      },
    }
  },
  props: {
    params: {
      type: Object,
      default: ()=>{}
    },
    tools: {
      type: Array,
      default: ()=>[]
    },
    parent: {
      type: Object,
      default: ()=>{}
    },
    read: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    /** 获取项目id */
    projectId: function() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
  },
  created() {
    this.getMemberList();
    // 订阅成员离线消息
    this.offlineTopicId = this.$topic.subscribe(this.$topic.MEMBER_OFFLINE_TOPIC, (name, data) => {
      let memberId = data.data;
      for(let key in this.memberList){
        if(this.memberList[key].userId == memberId) {
          this.memberList[key].online = false;
          this.$set(this.memberList[key],'online',false);
          break;
        }
      }
    });
    // 订阅成员在线消息
    this.onlineTopicId = this.$topic.subscribe(this.$topic.MEMBER_ONLINE_TOPIC, (name, data) => {
      let memberId = data.data;
      for(let key in this.memberList){
        if(this.memberList[key].userId == memberId) {
          this.memberList[key].online = true;
          this.$set(this.memberList[key],'online',true);
          break;
        }
      }
    });
  },
  beforeDestroy() {
    // 取消用户活动焦点的订阅
    this.$topic.unsubscribe(this.onlineTopicId);
    this.$topic.unsubscribe(this.offlineTopicId);
    this.onlineTopicId = null;
    this.offlineTopicId = null;
  },
  methods:{
    clickHandle(member) {
      if(this.read) return;
      this.parent.search({handleBy: [member.userId]})
    },
    toolsHandle(e,tool) {
      this.$emit('tools-click',tool);
    },
    /** 查询成员集合 */
    getMemberList() {
      this.loading = true;
      this.queryMember.projectId = this.projectId;
      listMemberOfProject(this.projectId,this.queryMember).then(res=>{
        this.loading = false;
        this.memberList=res.rows;
      });
    },
  }
}
</script>

<style lang="scss" scoped>
.click:hover {
  cursor: pointer;
}
.member-list {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  max-width: 280px;
  min-width: 230px;
  overflow-x: hidden;
  overflow-y: auto;
}
</style>
