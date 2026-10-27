<template>
  <div class="mwr" v-loading="loading">
    <div v-if="rangeLabel" class="mwr__range">{{ rangeLabel }}</div>
    <div v-if="!loading && !members.length" class="mwr__empty">
      {{ $t('workbench.weekly-rank.empty') }}
    </div>
    <ul v-else class="mwr__list">
      <li
        v-for="(m, index) in members"
        :key="m.userId || index"
        class="mwr__row"
      >
        <cat2-bug-avatar :member="avatarMember(m)" :size="36" />
        <div class="mwr__body">
          <div class="mwr__name" :title="displayName(m)">{{ displayName(m) }}</div>
          <div
            v-for="item in metricItems(m)"
            :key="item.key"
            class="mwr__line"
          >
            <span class="mwr__lab" :title="item.label">{{ item.label }}</span>
            <el-progress
              :percentage="item.pct"
              :status="item.status"
              :stroke-width="4"
            />
            <span class="mwr__n">{{ item.value }}</span>
          </div>
        </div>
      </li>
    </ul>
  </div>
</template>

<script>
import Cat2BugAvatar from '@/components/Cat2BugAvatar'
import { memberWeeklyRank } from '@/api/system/dashboard'
import { getCurrentProjectId } from '@/utils/store-context'

export default {
  name: 'MemberWeeklyRank',
  components: { Cat2BugAvatar },
  data() {
    return {
      loading: false,
      weekStart: '',
      weekEnd: '',
      members: [],
      maxDefect: 0,
      maxCommit: 0,
      maxFile: 0
    }
  },
  computed: {
    rangeLabel() {
      if (!this.weekStart || !this.weekEnd) return ''
      const fmt = (s) => {
        const parts = String(s).split('-')
        if (parts.length >= 3) return `${parts[1]}-${parts[2]}`
        return s
      }
      return this.$t('workbench.weekly.range', [fmt(this.weekStart), fmt(this.weekEnd)])
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    displayName(m) {
      return m.nickName || m.userName || String(m.userId || '')
    },
    avatarMember(m) {
      return {
        userId: m.userId,
        nickName: m.nickName,
        userName: m.userName,
        avatar: m.avatar
      }
    },
    pctOf(value, max) {
      const n = Number(value) || 0
      const m = Number(max) || 0
      if (m <= 0) return 0
      return Math.max(0, Math.min(100, Math.round((n / m) * 100)))
    },
    metricItems(m) {
      return [
        {
          key: 'defect',
          label: this.$t('workbench.weekly-rank.defect'),
          value: Number(m.defectCount) || 0,
          pct: this.pctOf(m.defectCount, this.maxDefect),
          status: undefined
        },
        {
          key: 'commit',
          label: this.$t('workbench.weekly-rank.commit'),
          value: Number(m.commitCount) || 0,
          pct: this.pctOf(m.commitCount, this.maxCommit),
          status: 'success'
        },
        {
          key: 'file',
          label: this.$t('workbench.weekly-rank.file'),
          value: Number(m.fileCount) || 0,
          pct: this.pctOf(m.fileCount, this.maxFile),
          status: 'warning'
        }
      ]
    },
    async load() {
      const projectId = getCurrentProjectId()
      if (!projectId) return
      this.loading = true
      try {
        const res = await memberWeeklyRank(projectId)
        const data = res.data || res || {}
        this.weekStart = data.weekStart || ''
        this.weekEnd = data.weekEnd || ''
        const list = Array.isArray(data.members) ? data.members : []
        let maxDefect = 0
        let maxCommit = 0
        let maxFile = 0
        for (const row of list) {
          maxDefect = Math.max(maxDefect, Number(row.defectCount) || 0)
          maxCommit = Math.max(maxCommit, Number(row.commitCount) || 0)
          maxFile = Math.max(maxFile, Number(row.fileCount) || 0)
        }
        this.maxDefect = maxDefect
        this.maxCommit = maxCommit
        this.maxFile = maxFile
        this.members = list
      } catch (e) {
        this.members = []
        this.weekStart = ''
        this.weekEnd = ''
        this.maxDefect = 0
        this.maxCommit = 0
        this.maxFile = 0
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.mwr {
  width: 100%;
  padding: 0;
  background: transparent;
}
.mwr__range {
  margin: 0 2px 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.mwr__empty {
  padding: 12px 8px;
  text-align: center;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  background: transparent;
}
.mwr__list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: transparent;
}
.mwr__row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 8px;
  border-radius: 8px;
  border: 1px solid var(--border-color-light, var(--el-border-color, #414243));
  background: transparent;
  box-shadow: none;
  box-sizing: border-box;
  transition: background 0.15s ease, border-color 0.15s ease;
  &:hover {
    background: var(--table-row-hover-bg, rgba(255, 255, 255, 0.03));
    border-color: var(--el-border-color, #606266);
  }
}
.mwr__body {
  flex: 1;
  min-width: 0;
  line-height: 1.15;
  overflow: hidden;
}
.mwr__name {
  position: relative;
  z-index: 1;
  margin-bottom: 2px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mwr__line {
  display: grid;
  grid-template-columns: 5.75rem minmax(0, 1fr) 1.25rem;
  align-items: center;
  column-gap: 6px;
  width: 100%;
  min-width: 0;
  min-height: 12px;
  & + & {
    margin-top: 0;
  }
}
.mwr__lab {
  min-width: 0;
  font-size: 11px;
  line-height: 1.15;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mwr__n {
  text-align: right;
  font-size: 11px;
  line-height: 1.15;
  font-variant-numeric: tabular-nums;
  color: var(--el-text-color-regular);
}
.mwr__line :deep(.el-progress) {
  width: 100%;
  min-width: 0;
  line-height: 1;
  .el-progress-bar {
    padding-right: 0;
    margin-right: 0;
  }
  .el-progress-bar__outer {
    height: 4px !important;
  }
  .el-progress__text {
    display: none;
  }
}
</style>
