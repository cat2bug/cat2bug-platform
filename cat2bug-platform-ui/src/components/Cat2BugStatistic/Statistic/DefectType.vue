<template>
  <cat2-bug-card :title="$i18n.t('defect.type-statistics').toString()" v-loading="loading" :tools="tools" @tools-click="toolsHandle">
    <template slot="content">
      <cat2-but-label class="defect-type-label" @click.native="clickHandle(type)" v-for="type in typeList" :key="type.label"
                      :icon="type.icon" :icon-color="type.color" :label="$t(type.label).toString()" :content="type.value" />
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
      typeList: [],
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
            id: t.k,
            color: color,
            icon: icon,
            label: t.k,
            value: t.v
          }
        });
        ts.unshift({
          id: null,
          color: '409EFF',
          icon: 'all',
          label: 'all',
          value: total
        })
        this.typeList = ts;
      });
    },
    clickHandle(type) {
      if(this.read) return;
      this.parent.search({defectType: type?type.id:null})
    },
    toolsHandle(e,tool) {
      this.$emit('tools-click',tool);
    }
  }
}
</script>

<style scoped>
.defect-type-label, ::v-deep .defect-type-label label {
  cursor: pointer;
}
.defect-type-label:hover {
  color: #409EFF;
}
</style>
