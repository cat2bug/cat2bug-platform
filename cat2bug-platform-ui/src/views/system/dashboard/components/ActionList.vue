<template>
  <div class="defect-state-chart" :style="{width:width}">
    <h1>{{ $t('dashboard.action.title') }}</h1>
    <div v-for="actionTime in sortActionTimes">
      <h4>{{formatDate(actionTime)}}</h4>
      <div v-for="action in  actionList[actionTime]" class="action-row">
        <el-tag v-if="action.type==='report'" effect="dark">{{ $t(action.type) }}</el-tag>
        <el-tag v-else-if="action.type==='document'" type="info" effect="dark">{{ $t(action.type) }}</el-tag>
        <el-tag v-else-if="action.type==='defect'" type="danger" effect="dark">{{ $t(action.type) }}</el-tag>
        <el-tag v-else-if="action.type==='case'" type="success" effect="dark">{{ $t(action.type) }}</el-tag>
        <el-tag v-else-if="action.type==='plan'" type="warning" effect="dark">{{ $t(action.type) }}</el-tag>
        <el-tag v-else type="info" effect="dark">{{ $t(action.type) }}</el-tag>
        <cat2-bug-text :content="action.title" :tooltip="action.title" type="link" />
        <span>{{ action.time }}</span>
        <span v-if="action.state && $te(action.state)">({{ $t(action.state) }})</span>
        <cat2-bug-avatar :member="action" />
      </div>
    </div>
  </div>
</template>

<script>
// 活动列表
import Cat2BugText from "@/components/Cat2BugText";
import Cat2BugAvatar from "@/components/Cat2BugAvatar";
import {actionList} from "@/api/system/dashboard";
import {strFormat} from "@/utils";

export default {
  name: "ActionList",
  components: { Cat2BugText, Cat2BugAvatar },
  props: {
    width: {
      type: String,
      default: '100%'
    },
    height: {
      type: String,
      default: '100px'
    },
  },
  data() {
    return {
      loading: false,
      actionList: {},
      query: {
        type: '1'
      }
    }
  },
  computed: {
    /** 获取项目id */
    projectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    formatDate: function () {
      return function (time) {
        const date = new Date(parseInt(time));
        return `${strFormat(this.$i18n.t('year-month-day'),date.getFullYear(), date.getMonth()+1, date.getDate())} ${this.$i18n.t('week'+date.getDay())}`
      }
    },
    sortActionTimes() {
      return Object.keys(this.actionList).sort(function(a,b){return parseInt(b)-parseInt(a)});
    }
  },
  mounted() {
    this.getActionList();
  },
  methods: {
    getActionList() {
      this.loading = true;
      actionList(this.projectId, this.query).then(res=>{
        this.loading = false;
        this.actionList = res.data.actions;
      }).catch(e=>this.loading=false);
    }
  }
}
</script>

<style lang="scss" scoped>
.defect-state-chart {
  position: relative;
  h4 {
    margin: 10px 0px;
  }
}
.time-type {
  position: absolute;
  z-index: 9;
  top: 7px;
  right: 40px;
  ::v-deep .el-radio-button__inner {
    padding: 3px 5px;
  }
}
.action-row:nth-child(even):hover {
  background-color: #EBEEF5;
}
.action-row:nth-child(even) {
  background-color: #F2F6FC;
}
.action-row {
  width: 100%;
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  padding: 5px 15px;
  border-radius: 5px;
  border-bottom: 1px solid #EBEEF5;
  > span {
    font-size: 0.8rem;
  }
  > ::v-deep .member-avatar {
    margin-left: auto;
  }
}
</style>
