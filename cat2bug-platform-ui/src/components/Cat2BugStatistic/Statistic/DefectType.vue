<template>
  <cat2-bug-card :title="title" v-loading="loading">
    <template slot="content">
      <cat2-but-label v-for="type in typeList" :key="type.label" :icon="type.icon" :icon-color="type.color" :label="type.label" :content="type.value" />
    </template>
  </cat2-bug-card>
</template>

<script>
import Cat2BugCard from "../Components/Card"
import Cat2ButLabel from "../Components/Label"
import {statisticDefectType} from "@/api/system/statistic/defect";
export default {
  name: "DefectType",
  components: {Cat2ButLabel,Cat2BugCard},
  data() {
    return {
      loading: false,
      title: this.$i18n.t('defect.type-statistics'),
      typeList: [],
    }
  },
  props: {
    params: {
      type: Object,
      default: ()=>[]
    }
  },
  computed: {
    currentProjectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
  },
  mounted() {
    this.getStatisticDefectType();
  },
  methods: {
    getStatisticDefectType: function (){
      this.loading = true;
      statisticDefectType(this.currentProjectId).then(res=>{
        this.loading = false;
        let total = 0;
        let ts = res.data.map(t=>{
          total += t.v;
          let color;
          let icon;
          switch (t.k) {
            case 'BUG':
              color = '#F56C6C';
              icon = 'bug';
              break;
            case 'TASK':
              color = '#67C23A';
              icon = 'task';
              break;
            case 'DEMAND':
              color = '#FBB13F';
              icon = 'demand';
              break;
          }
          return {
            color: color,
            icon: icon,
            label: this.$i18n.t(t.k),
            value: t.v
          }
        });
        ts.unshift({
          color: '409EFF',
          icon: 'all',
          label: 'ALL',
          value: total
        })
        this.typeList = ts;
      });
    }
  }
}
</script>

<style scoped>

</style>
