<template>
  <cat2-bug-card :title="title" v-loading="loading" :tools="tools" @tools-click="toolsHandle">
    <template slot="content">
      <cat2-but-label @click.native="clickHandle" class="defect-state-label" icon="all" icon-color="#409EFF" :label="$t('all')" content="12">
        <template slot="content">
          <span>{{ total }}</span>
          <span class="width50">{{ $t('today') }}</span>
          <span class="width50">{{ $t('this-week') }}</span>
        </template>
      </cat2-but-label>
      <cat2-but-label @click.native="clickHandle($event,state)"
                      class="defect-state-label"
                      icon="pending-processing"
                      :icon-color="iconColor(index)"
                      v-for="(state,index) in stateList"
                      :key="index"
                      :label="$t(state.k).toString()">
        <template slot="content">
          <span>{{ state.a }}</span>
          <span class="width50" :flag="flag(state.d)">{{ flag(state.d) + state.d }}</span>
          <span class="width50" :flag="flag(state.w)">{{ flag(state.w) + state.w }}</span>
        </template>
      </cat2-but-label>
    </template>
  </cat2-bug-card>
</template>

<script>
import Cat2BugCard from "../Components/Card"
import Cat2ButLabel from "../Components/Label"
import {statisticDefectState} from "@/api/system/statistic/defect";

export default {
  name: "DefectState",
  components: {Cat2ButLabel,Cat2BugCard},
  data() {
    return {
      loading: false,
      title: this.$i18n.t('defect.status-statistics'),
      stateList: [],
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
  },
  computed: {
    flag: function () {
      return function (num) {
        if(num>0) {
          return '+';
        } if(num<0) {
          return '-';
        } else {
          return '';
        }
      }
    },
    currentProjectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    total: function () {
      let all = 0;
      this.stateList.forEach(s=>all+=s.a);
      return all;
    },
    iconColor: function () {
      return function (index) {
        switch (index%3) {
          case 0:
            return '#F56C6C'
          case 1:
            return '#FBB13F'
          case 2:
            return '#67C23A'
        }
      }
    }
  },
  mounted() {
    this.getStatisticDefectState();
  },
  methods: {
    clickHandle(event, state) {
      this.parent.search({
        params: {
          defectStates: state?JSON.parse(JSON.stringify(state.id)):null
        }
      });
      event.stopPropagation();
    },
    getStatisticDefectState() {
      this.loading = true;
      statisticDefectState(this.currentProjectId).then(res=>{
        this.loading = false;
        // 合并处理中和驳回的
        let a=0,d=0,w=0;
        res.data.forEach(s=>{
          if(s.k=='PROCESSING' || s.k=='REJECTED') {
            a+=s.a;
            d+=s.d;
            w+=s.w;
          }
        });
        let ret = [{
          id: [0,3],
          k:'PENDING',
          a:a,
          d:d,
          w:w
        }]
        // 过滤只显示待审核、已审核的
        ret = ret.concat( res.data.filter(s=>s.k == 'AUDIT').map(s=>{
          return {
            id: [s.id],
            k:s.k,
            a:s.a,
            d:s.d,
            w:s.w
          }
        }));
        ret = ret.concat( res.data.filter(s=>s.k=='CLOSED').map(s=>{
          return {
            id: [s.id],
            k:s.k,
            a:s.a,
            d:s.d,
            w:s.w
          }
        }))
        this.stateList =ret;
      })
    },
    toolsHandle(e,tool) {
      this.$emit('tools-click',tool);
    }
  }
}
</script>

<style lang="scss" scoped>
  .width50 {
    width: 50px;
    min-width: 50px;
  }
  .cat2bug-statistic-label {
    span[flag="+"] {
      color: orangered;
    }
    span[flag="-"] {
      color: #67C23A;
    }
  }
  .defect-state-label {
    cursor: pointer;
  }
  .defect-state-label:hover {
    color: #409EFF;
  }
</style>
