<template>
  <div>
    <el-row v-for="(log,logIndex) in logList" :key="logIndex+'-'+log.defectLogId">
      <el-col class="list-defect-log-row" :span="24">
        <component :is="logComponentType(log)" :log="log"></component>
        <div>
          <el-button v-if="commentPermi" icon="el-icon-chat-line-round" size="mini" type="text" @click="showCommentInputHandle(log,logIndex)">{{$i18n.t('comment')}}</el-button>
          <span class="time">{{ parseTime(log.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </div>
      </el-col>
      <comment-input ref="myCommentInput" v-show="log.visibleCommentInput" show-word-limit @submit="commentSubmitHandle(log,$event)" />
      <el-col v-if="showComment" class="list-comment-row" :key="comment.commentId" :span="24" v-for="comment in log.commentList">
        <comment-view :comment="comment" @delete="getLog" />
      </el-col>
    </el-row>
  </div>
</template>

<script>
import {listLog} from "@/api/system/log";
import CREATE from "@/components/ListDefectLog/create";
import IMPORT from "@/components/ListDefectLog/import"
import ASSIGN from "@/components/ListDefectLog/assign"
import REJECTED from "@/components/ListDefectLog/reject";
import REPAIR from "@/components/ListDefectLog/repair";
import PASS from "@/components/ListDefectLog/pass";
import CLOSED from "@/components/ListDefectLog/close";
import OPEN from "@/components/ListDefectLog/open";
import UPDATE from "@/components/ListDefectLog/update";
import DELETE from "@/components/ListDefectLog/delete";
import RESTORE from "@/components/ListDefectLog/restore";
import Cat2BugAvatar from "@/components/Cat2BugAvatar";
import CommentInput from "@/components/Comment/CommentInput";
import CommentView from "@/components/Comment/CommentView";
import {addComment} from "@/api/system/comment";
import {checkPermi} from "@/utils/permission";

/** 与后端 SysDefectLogStateEnum.ordinal() 一致，兼容接口返回枚举序号而非名称的情况 */
const DEFECT_LOG_TYPE_ORDER = [
  'CREATE', 'ASSIGN', 'REJECT', 'PASS', 'REJECTED', 'REPAIR', 'CLOSED', 'OPEN', 'UPDATE', 'IMPORT', 'DELETE', 'RESTORE'
];

export default {
  name: "ListDefectLog",
  components:{ CREATE,IMPORT,ASSIGN,REJECTED,REPAIR,PASS,CLOSED,OPEN,UPDATE,DELETE,RESTORE,Cat2BugAvatar, CommentInput, CommentView },
  data() {
    return {
      defectId: null,
      logList: [],
      commentList: [],
    }
  },
  props: {
    pageNum: {
      type: Number,
      default: 1
    },
    pageSize: {
      type: Number,
      default: 10
    },
    showComment: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    commentPermi: function () {
      return this.showComment && checkPermi(['system:defect:list']);
    },
    commentParse: function () {
      return function (content) {
        if(this.$refs.commentInputOfLog){
          // return this.$refs.commentInputOfLog.toHtml(content);
          return content;
        } else {
          return content;
        }
      }
    },
    member: function () {
      return function (log) {
        return {
          nickName: log.createByName,
          avatar: log.createByAvatar
        }
      }
    }
  },
  methods: {
    logComponentType(log) {
      const raw = log && log.defectLogType;
      if (raw === null || raw === undefined) {
        return 'CREATE';
      }
      if (typeof raw === 'number' && raw >= 0 && raw < DEFECT_LOG_TYPE_ORDER.length) {
        return DEFECT_LOG_TYPE_ORDER[raw];
      }
      if (typeof raw === 'string') {
        return raw;
      }
      if (typeof raw === 'object' && raw.name) {
        return raw.name;
      }
      return 'CREATE';
    },
    open(defectId) {
      this.defectId = defectId;
      this.getLog();
    },
    refresh(logs) {
      this.logList = logs.map(l=>{
        l.visibleCommentInput = false;
        return l;
      });
    },
    getLog() {
      listLog({
        pageNum: this.pageNum,
        pageSize: this.pageSize,
        defectId: this.defectId
      }).then(res=>{
        this.logList = res.rows.map(l=>{
          l.visibleCommentInput = false;
          return l;
        });
      })
    },
    addLog(log) {
      this.logList.unshift(log);
      this.$set(this,'logList',this.logList);
    },
    /** 提交评论 */
    commentSubmitHandle(log,content) {
      addComment({
        commentContent:content,
        moduleType: 'defect_log',
        correlationId: log.defectLogId
      }).then(res=>{
        this.$message.success(this.$i18n.t('comment.submit-success').toString());
        this.getLog();
      })
    },
    showCommentInputHandle(log,logIndex) {
      log.visibleCommentInput=!log.visibleCommentInput;
      if(log.visibleCommentInput) {
        this.$refs['myCommentInput'][logIndex].reset();
        this.$refs['myCommentInput'][logIndex].focus();
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.list-defect-log-row {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 5px;

  /* 左侧日志正文占满剩余宽度，右侧「评论 + 时间」固定在同一行顶端（与单行日志一致） */
  > :first-child {
    flex: 1 1 auto;
    min-width: 0;
  }

  > div:last-child {
    flex-shrink: 0;
    display: inline-flex;
    flex-direction: row;
    align-items: center;
    white-space: nowrap;
    margin-left: 8px;
    padding-top: 1px;
  }

  .time {
    margin-left: 10px;
    flex-shrink: 0;
    font-size: 12px;
    color: #909399;
  }
}
.list-comment-row {
  padding-left: 20px;
}
</style>
