<template>
  <cat2-bug-card
    :title="$i18n.t('defect.team-open-workload').toString()"
    v-loading="loading"
    :tools="tools"
    @tools-click="toolsHandle"
  >
    <template slot="content">
      <div class="team-workload-panel">
        <div v-if="isEmpty" class="team-workload-empty">{{ $t('no-data') }}</div>
        <template v-else>
          <div
            v-for="(item, index) in workloadList"
            :key="item.userId"
            class="team-workload-row"
            @click="clickHandle(item)"
          >
            <h5 :style="`background-color:${flagColor(index)}`">{{ rankingNumber(index) }}</h5>
            <h4 :title="item.nickName">{{ formatName(item.nickName) }}</h4>
            <span class="team-workload-count">{{ item.total }}</span>
          </div>
          <el-pagination
            class="statistic-pagination"
            small
            :hide-on-single-page="total <= formParams.pageSize"
            layout="prev, next"
            :current-page.sync="formParams.pageNum"
            :page-size.sync="formParams.pageSize"
            :total="total"
            @current-change="getStatistic"
          />
        </template>
      </div>
    </template>
  </cat2-bug-card>
</template>

<script>
import Cat2BugCard from '../Components/Card'
import { statisticOpenWorkload } from '@/api/system/statistic/defect'

const OPEN_DEFECT_STATE_IDS = [0, 1, 3]

export default {
  name: 'TeamOpenWorkloadBar',
  components: { Cat2BugCard },
  data() {
    return {
      loading: false,
      total: 0,
      workloadList: [],
      formParams: {
        pageNum: 1,
        pageSize: 3
      }
    }
  },
  props: {
    params: { type: Object, default: () => ({}) },
    tools: { type: Array, default: () => [] },
    parent: { type: Object, default: () => ({}) },
    read: { type: Boolean, default: false }
  },
  computed: {
    currentProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId)
    },
    isEmpty() {
      return this.total === 0
    },
    rankingNumber() {
      return (index) => (this.formParams.pageNum - 1) * this.formParams.pageSize + index + 1
    }
  },
  mounted() {
    this.getStatistic()
  },
  methods: {
    getStatistic() {
      this.loading = true
      statisticOpenWorkload(this.currentProjectId, this.formParams).then(res => {
        this.workloadList = (res.rows || []).map(item => ({
          userId: item.userId,
          nickName: item.nickName || item.userName || '-',
          total: item.total || 0
        }))
        this.total = res.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    formatName(name) {
      const raw = (name || '').trim()
      if (raw.length <= 6) return raw || '-'
      return raw.slice(0, 6) + '…'
    },
    flagColor(index) {
      switch (index % 5) {
        case 0: return 'rgb(245, 108, 108)'
        case 1: return 'rgb(251, 177, 63)'
        case 2: return 'rgb(103, 194, 58)'
        case 3: return '#1989fa'
        case 4: return '#6f7ad3'
        default: return '#909399'
      }
    },
    clickHandle(item) {
      if (this.read || !item || !item.userId) return
      this.parent.searchQuery({
        common: {
          handleBy: [item.userId],
          params: { defectStates: OPEN_DEFECT_STATE_IDS }
        }
      })
    },
    refreshData() {
      this.getStatistic()
    },
    toolsHandle(e, tool) {
      this.$emit('tools-click', tool)
    }
  }
}
</script>

<style scoped>
.team-workload-panel {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  max-height: 100%;
  overflow-x: hidden;
  overflow-y: auto;
}

.team-workload-empty {
  font-size: 12px;
  color: var(--text-color-secondary, #909399);
}

.team-workload-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  min-height: var(--statistic-module-row-min-height, 17px);
  margin: 0;
  padding: var(--statistic-module-row-padding-y, 1px) 5px;
  cursor: pointer;
  gap: 4px;
  border-radius: 2px;
  transition: background-color 0.15s;
}

.team-workload-row + .team-workload-row {
  border-top: 1px dashed var(--border-color-lighter, #f2f6fc);
}

.team-workload-row:hover {
  background-color: rgba(64, 158, 255, 0.06);
}

.team-workload-row:hover h4 {
  color: #409eff;
}

h5 {
  border-radius: 3px;
  color: #ffffff;
  font-size: 10px;
  font-weight: 600;
  flex-shrink: 0;
  width: 14px;
  height: 14px;
  line-height: 14px;
  text-align: center;
  margin: 0;
}

h4 {
  font-size: 12px;
  font-weight: 500;
  margin: 0;
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--text-color-regular, #606266);
}

.team-workload-count {
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
  min-width: 16px;
  text-align: right;
  color: var(--text-color-primary, #303133);
}

.el-pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 2px;
  padding-top: 2px;
  line-height: 1;
}

::v-deep .el-pagination .el-icon {
  font-size: 10px !important;
}

::v-deep .el-pagination button {
  min-width: calc(var(--statistic-pagination-control-height, 15px) + 5px) !important;
  height: var(--statistic-pagination-control-height, 15px) !important;
  line-height: var(--statistic-pagination-control-height, 15px) !important;
}
</style>
