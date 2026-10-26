<template>
  <div class="defect-log-create">
    <defect-log-user size="medium" :name="log.createByName" :avatar="log.createByAvatar" />
    <span class="state">{{$i18n.t('create')}},</span>
    <span class="state">{{ $i18n.t('defect.assigned-to') }}</span>
    <row-list-member
      v-if="log.receiveByList && log.receiveByList.length > 1"
      :members="log.receiveByList"
      size="small"
    />
    <defect-log-user v-else-if="log.receiveByList && log.receiveByList.length" :member="log.receiveByList[0]" />
    <span v-if="resolvedDescribe">,</span>
    <div v-if="resolvedDescribe" class="defect-log-create__desc">
      <div class="defect-log-create__label">{{$i18n.t('describe')}}</div>
      <markdown-content class="defect-log-create__md markdown-rhythm" :content="resolvedDescribe" />
    </div>
  </div>
</template>

<script>
import DefectLogUser from './DefectLogUser'
import RowListMember from '@/components/RowListMember'
import MarkdownContent from '@/components/Markdown/MarkdownContent.vue'

export default {
  name: "CREATE",
  components: { DefectLogUser, RowListMember, MarkdownContent },
  props:{
    log: {
      type: Object,
      default: ()=>({})
    },
    /** 历史 CREATE 日志可能未落库描述，用缺陷正文兜底 */
    fallbackDescribe: {
      type: String,
      default: ''
    }
  },
  computed: {
    resolvedDescribe() {
      const fromLog = this.log && this.log.defectLogDescribe
      if (fromLog != null && String(fromLog).trim()) {
        return String(fromLog).trim()
      }
      const fb = this.fallbackDescribe
      return fb != null && String(fb).trim() ? String(fb).trim() : ''
    }
  }
}
</script>

<style lang="scss" scoped>
.defect-log-create {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  flex-direction: row;
  flex-wrap: wrap;
  font-size: 14px;
  gap: 2px 0;
  min-width: 0;

  > span,
  > :deep(.defect-log-user),
  > :deep(.row-list-member) {
    align-self: center;
  }

  span {
    padding-left: 2px;
    padding-right: 2px;
  }

  .state {
    font-size: 14px;
    color: var(--cat2bug-primary, #1890ff);
  }
}

.defect-log-create__desc {
  display: flex;
  flex-direction: column;
  flex: 1 1 100%;
  gap: 8px;
  min-width: 0;
  margin-top: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  background: var(--el-fill-color-light, rgba(0, 0, 0, 0.04));
  box-sizing: border-box;
}

.defect-log-create__label {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 650;
  line-height: 1.4;
  color: var(--text-color-secondary, #909399);
  letter-spacing: 0.02em;
}

.defect-log-create__md {
  min-width: 0;
  max-height: 9.5em;
  overflow: hidden;
  font-size: 14px;
  line-height: 1.65;
  font-weight: 500;
  color: var(--text-color-primary, #303133);

  :deep(*) {
    color: inherit;
  }
  :deep(ul),
  :deep(ol) {
    padding-left: 1.25em;
  }
}
</style>
