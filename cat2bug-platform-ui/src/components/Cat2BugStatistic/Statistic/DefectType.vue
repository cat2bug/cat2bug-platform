<template>
  <cat2-bug-card :title="$i18n.t('defect.type-statistics').toString()" v-loading="loading" :tools="tools" @tools-click="toolsHandle">
    <template slot="content">
      <div class="defect-type-panel">
        <cat2-but-label class="defect-type-label" @click.native="clickHandle(type)" v-for="type in typeList" :key="type.label"
                        :icon="type.icon" :icon-color="type.color" :label="$t(type.label).toString()" :content="type.value" />
      </div>
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
      this.parent.searchQuery({ common: { defectType: type ? type.id : null } })
    },
    toolsHandle(e,tool) {
      this.$emit('tools-click',tool);
    }
  }
}
</script>

<style lang="scss" scoped>
.defect-type-panel {
  display: flex;
  flex-direction: column;
  gap: 0;
  flex: 1 1 auto;
  min-height: 0;
  max-height: 100%;
  overflow-x: hidden;
  overflow-y: auto;
}

.defect-type-panel ::v-deep .defect-type-label {
  display: grid;
  grid-template-columns: 16px minmax(48px, 1fr) 28px;
  column-gap: 6px;
  align-items: center;
  cursor: pointer;
  padding-left: 5px;
  padding-right: 5px;
  border-radius: 2px;
  transition: background-color 0.15s;

  &:hover {
    background-color: rgba(64, 158, 255, 0.06);
  }

  &:hover .cat2bug-statistic-label__name {
    color: #409eff;
  }
}

.defect-type-panel ::v-deep .defect-type-label + .defect-type-label {
  border-top: 1px dashed var(--border-color-lighter, #f2f6fc);
}
</style>
