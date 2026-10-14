<template>
  <div class="comment-view">
    <div class="comment-view-title">
      <cat2-bug-avatar size="small" :member="comment.createMember" />
      <div class="comment-view-title-info">
        <span class="name">{{ comment.createMember.nickName }}</span>
        <span class="time">{{ parseTime(comment.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
      </div>
      <div>
        <el-button v-if="showDeleteButton(comment)" :icon="$legacyIcon('el-icon-delete')" size="small" link @click="deleteHandle(comment)"></el-button>
      </div>
    </div>
    <div class="comment-view-content" v-html="toHtml(comment.commentContent)"></div>
  </div>
</template>

<script>
import Cat2BugAvatar from "@/components/Cat2BugAvatar";
import {delComment} from "@/api/system/comment";
import { defaultEmojiMap } from '@/utils/emoji-assets'
import { commentToHtml } from '@/utils/comment-html'
import { pinia } from '@/store/pinia.js'
import { useUserStore } from '@/stores/user'

const imgMap = defaultEmojiMap
const userStore = useUserStore(pinia)
export default {
  name: "CommentView",
  components: { Cat2BugAvatar },
  data() {
    return {

    }
  },
  props: {
    comment: {
      type: Object,
      default: {}
    }
  },
  computed: {
    showDeleteButton: function (){
      return function (comment) {
        return userStore.id == comment.createById;
      }
    },
  },
  methods: {
    toHtml(str) {
      return commentToHtml(str, imgMap);
    },
    deleteHandle(comment){
      const self = this;
      this.$modal.confirm(this.$i18n.t('comment.is-delete')).then(function() {
        delComment(comment.commentId).then(res=>{
          self.$modal.msgSuccess(self.$i18n.t('delete.success'));
          self.$emit('delete',comment);
        });
      });
    }
  }
}
</script>

<style lang="scss" scoped>
.comment-view {
  padding-left: 15px;
  margin-bottom: 10px;
  width: 100%;
}
.comment-view-title {
  width: 100%;
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  .comment-view-title-info {
    display: flex;
    flex-direction: column;
    flex: 1;
    > * {
      margin-left: 5px;
      height: 18px;
    }
    .name {
      font-weight: 500;
    }
    .time {
      flex-shrink: 0;
      font-size: 12px;
      color: #909399;
    }
  }
}
.comment-view-content {
  padding-left: 0px;
  display: flex;
  flex-direction: column;
}
</style>
