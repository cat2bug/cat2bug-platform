<template>
  <div class="report-detail-history" v-loading="loading">
    <p v-if="!items.length" class="report-detail-history-empty">
      {{ $t('report.history-empty') }}
    </p>

    <div
      v-for="group in groupedItems"
      :key="group.dateKey"
      class="report-history-group"
    >
      <div class="report-history-group-date">
        <legacy-icon class="report-history-group-date-icon" icon="el-icon-date" />
        <span>{{ group.dateLabel }}</span>
      </div>

      <div
        v-for="item in group.items"
        :key="item.historyId"
        class="report-history-item"
      >
        <div class="report-history-meta">
          <cat2-bug-avatar size="small" :member="item.createMember" />
          <div class="report-history-meta-main">
            <div class="report-history-meta-title">
              <span class="name">{{ memberName(item.createMember) }}</span>
              <span class="time">{{ parseTime(item.createTime, '{h}:{i}:{s}') }}</span>
              <el-dropdown
                v-if="canOperate"
                trigger="click"
                class="report-history-more"
                @command="(cmd) => onHistoryCommand(cmd, item)"
                @click.stop
              >
                <el-button
                  class="report-history-more-btn"
                  link
                  size="small"
                  :icon="$legacyIcon('el-icon-more')"
                  :title="$t('report.history-actions')"
                  @click.stop
                ></el-button>
                <template #dropdown>
                  <el-dropdown-menu class="report-history-actions-menu">
                    <el-dropdown-item command="restore">
                      <legacy-icon icon="el-icon-refresh-left" class="report-history-action-icon" />
                      {{ $t('report.history-restore') }}
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <legacy-icon icon="el-icon-delete" class="report-history-action-icon" />
                      {{ $t('delete') }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <div class="report-history-meta-hint">{{ $t('report.history-edited') }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Cat2BugAvatar from '@/components/Cat2BugAvatar'

export default {
  name: 'ReportDetailHistory',
  components: { Cat2BugAvatar },
  props: {
    items: { type: Array, default: () => [] },
    canOperate: { type: Boolean, default: false },
    loading: { type: Boolean, default: false }
  },
  emits: ['restore', 'delete'],
  computed: {
    groupedItems() {
      const groups = []
      const indexByKey = Object.create(null)
      ;(this.items || []).forEach(item => {
        const dateKey = this.parseTime(item.createTime, '{y}-{m}-{d}') || ''
        const dateLabel = dateKey || '--'
        if (indexByKey[dateKey] == null) {
          indexByKey[dateKey] = groups.length
          groups.push({ dateKey, dateLabel, items: [] })
        }
        groups[indexByKey[dateKey]].items.push(item)
      })
      return groups
    }
  },
  methods: {
    memberName(member) {
      return member && member.nickName ? member.nickName : ''
    },
    onHistoryCommand(command, item) {
      if (command === 'restore') {
        this.$emit('restore', item)
      } else if (command === 'delete') {
        this.$emit('delete', item)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.report-detail-history {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 0 12px 16px;
}
.report-detail-history-empty {
  padding: 24px 4px;
  font-size: 12px;
  text-align: center;
  color: var(--el-text-color-placeholder, #c0c4cc);
}
.report-history-group {
  margin-bottom: 12px;
}
.report-history-group-date {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px 10px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--el-text-color-regular, #606266);
  letter-spacing: 0.02em;

  .report-history-group-date-icon {
    flex: 0 0 auto;
    font-size: 16px;
    color: var(--el-text-color-secondary, #909399);
  }
}
.report-history-item {
  padding: 10px 8px;
  border-radius: 6px;
  margin-bottom: 4px;
  border: 1px solid transparent;

  &:hover {
    background-color: var(--el-fill-color-light, #f5f7fa);
  }
}
.report-history-meta {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}
.report-history-meta-main {
  flex: 1;
  min-width: 0;
}
.report-history-meta-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  .name {
    flex: 1 1 auto;
    min-width: 0;
    font-weight: 500;
    font-size: 13px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .time {
    flex: 0 0 auto;
    font-size: 12px;
    font-variant-numeric: tabular-nums;
    color: var(--el-text-color-secondary, #909399);
    white-space: nowrap;
  }
}
.report-history-more {
  flex: 0 0 auto;
  line-height: 1;
}
.report-history-more-btn {
  padding: 2px !important;
  min-height: 0 !important;
  height: auto !important;
  color: var(--el-text-color-secondary, #909399) !important;
}
.report-history-more-btn:hover {
  color: var(--el-color-primary, #409eff) !important;
}
.report-history-action-icon {
  margin-right: 6px;
  font-size: 14px;
  vertical-align: -2px;
}
.report-history-meta-hint {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
}

html.dark .report-detail-history {
  .report-detail-history-empty,
  .report-history-meta-hint,
  .report-history-group-date,
  .report-history-group-date .report-history-group-date-icon,
  .report-history-meta-title .time {
    color: #c8ccd3;
  }
  .report-history-item:hover {
    background-color: #2a2a2c;
  }
  .report-history-meta-title .name {
    color: #e5eaf3;
  }
  .report-history-more-btn {
    color: #c8ccd3 !important;
  }
  .report-history-more-btn:hover {
    color: var(--crepe-color-primary, #f4bd6f) !important;
  }
}
</style>
