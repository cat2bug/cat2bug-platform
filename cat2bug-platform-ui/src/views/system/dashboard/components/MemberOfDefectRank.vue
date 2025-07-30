<template>
  <div :style="{width:width}">
    <h1>{{ $t('dashboard.member-Defect-rank.title') }}</h1>
    <div v-for="(m,index) in memberList" :key="index" class="member-row" @click="handleClick(m)">
      <cat2-bug-avatar :member="m" />
      <div class="line">
        <div>
          <span>{{ $t('total') }}</span>
          <el-progress :percentage="percentageTotal(m.defectTotal)"></el-progress>
          <span>{{ m.defectTotal }}</span>
        </div>
        <div>
          <span>{{ $t('today') }}</span>
          <el-progress :percentage="percentageTodayCount(m.defectTodayCount)" status="success"></el-progress>
          <span>{{ m.defectTodayCount }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
// 成员处理缺陷排行

import {memberRankOfDefects} from "@/api/system/dashboard";
import Cat2BugAvatar from "@/components/Cat2BugAvatar";
import {setDefectTempTab} from "@/utils/defect";

export default {
  name: "MemberOfDefectRank",
  components: { Cat2BugAvatar },
  props: {
    width: {
      type: String,
      default: '100%'
    },
  },
  data() {
    return {
      loading: false,
      memberList: [],
      maxTotal: 0,
      maxCount: 0,
      query: {
        timeType: 'day'
      }
    }
  },
  computed: {
    /** 获取项目id */
    projectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    percentageTotal: function () {
      return function (val) {
        return this.maxTotal?parseInt(val/this.maxTotal*100):0;
      }
    },
    percentageTodayCount: function () {
      return function (val) {
        return this.maxCount?parseInt(val/this.maxCount*100):0;
      }
    }
  },
  mounted() {
    this.getMemberRankOfDefectList();
  },
  methods: {
    handleClick(member) {
      const params = {
        tabId: new Date().getMilliseconds(),
        tabName: member.nickName || member.userName,
        config: {
          handleBy: [member.userId]
        }}
      setDefectTempTab(params);
      const targetRoute = this.$router.resolve({ name:'Defect', params: {}});
      window.open(targetRoute.href, '_blank');
    },
    getMemberRankOfDefectList() {
      this.loading = true;
      memberRankOfDefects(this.projectId).then(res=>{
        this.loading = false
        res.data.forEach(d=>{
          this.maxCount = Math.max(this.maxCount, d.defectTodayCount);
          this.maxTotal = Math.max(this.maxTotal, d.defectTotal);
        })
        this.memberList = res.data;
      }).catch(e=>this.loading = false);
    }
  }
}
</script>

<style lang="scss" scoped>
.member-row {
  width: 100%;
  display: inline-flex;
  flex-direction: row;
  justify-content: center;
  >*:first-child {
    flex-shrink: 0;
  }
  >*:last-child {
    flex: 1;
  }
}
.line {
  padding-left: 5px;
  > div {
    width: 100%;
    display: inline-flex;
    flex-direction: row;
    align-items: center;
    justify-content: center;
    gap: 3px;
    > span {
      min-width: 30px;
      flex-shrink: 0;
      font-size: 0.8rem;
    }
    > span:last-child {
      min-width: 30px;
    }
    > .el-progress {
      flex: 1;
    }
  }
  ::v-deep .el-progress {
    > .el-progress-bar {
      padding-right: 5px;
    }
    > .el-progress__text {
      display: none;
    }
  }
}
</style>
