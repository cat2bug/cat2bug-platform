<template>
  <div
    class="list-defect-log"
    v-loading="loading"
    :element-loading-text="$t('loading')"
  >
    <div class="list-defect-log-panel">
    <el-row
      v-for="(log,logIndex) in logList"
      :key="logIndex+'-'+log.defectLogId"
      class="list-defect-log-item"
      :class="{ 'list-defect-log-item--divider': logIndex < logList.length - 1 }"
    >
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
    <div v-if="pagerVisible" class="list-defect-log-pager">
      <el-pagination
        class="statistic-pagination"
        small
        layout="prev, next"
        :hide-on-single-page="true"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        @current-change="handlePageChange"
      />
    </div>
    </div>
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
  components:{ CREATE,IMPORT,ASSIGN,REJECTED,REPAIR,PASS,CLOSED,OPEN,UPDATE,DELETE,RESTORE, CommentInput, CommentView },
  data() {
    return {
      defectId: null,
      logList: [],
      commentList: [],
      currentPage: 1,
      total: 0,
      loading: false,
    }
  },
  props: {
    pageNum: {
      type: Number,
      default: 1
    },
    pageSize: {
      type: Number,
      default: 5
    },
    showComment: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    pagerVisible() {
      return this.pageSize > 1;
    },
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
      this.currentPage = this.pageNum || 1;
      this.getLog();
    },
    refresh(logs) {
      this.logList = logs.map(l=>{
        l.visibleCommentInput = false;
        return l;
      });
    },
    async getLog() {
      if (!this.defectId) {
        return;
      }
      this.loading = true;
      try {
        const fetchPage = async (pageNum) => {
          const res = await listLog({
            pageNum,
            pageSize: this.pageSize,
            defectId: this.defectId
          });
          return res;
        };
        let res = await fetchPage(this.currentPage);
        this.total = res.total || 0;
        const maxPage = Math.max(1, Math.ceil(this.total / this.pageSize));
        if (this.currentPage > maxPage) {
          this.currentPage = maxPage;
          res = await fetchPage(this.currentPage);
        }
        this.logList = (res.rows || []).map(l => {
          l.visibleCommentInput = false;
          return l;
        });
      } finally {
        this.loading = false;
      }
    },
    handlePageChange(page) {
      this.currentPage = page;
      this.getLog();
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
.list-defect-log {
  position: relative;
  min-height: 48px;
}
.list-defect-log-panel {
  border-radius: var(--defect-log-panel-radius, 5px);
  padding: 0;
  box-sizing: border-box;
}
.list-defect-log-item--divider {
  padding-bottom: 10px;
  margin-bottom: 10px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}
.list-defect-log-row {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;

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
.list-defect-log-pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
  padding-top: 4px;

  ::v-deep .el-pagination.statistic-pagination {
    padding: 0;

    button,
    .btn-prev,
    .btn-next {
      padding: 0 6px;
      min-width: 28px;
      background: transparent !important;
      background-color: transparent !important;
      border: none !important;
      box-shadow: none !important;
    }

    .btn-prev.disabled,
    .btn-next.disabled,
    button:disabled {
      background: transparent !important;
      background-color: transparent !important;
    }
  }
}
</style>
