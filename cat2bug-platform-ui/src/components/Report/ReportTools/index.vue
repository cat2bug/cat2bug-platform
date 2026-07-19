<template>
  <div class="report-tools report-tools__bar">
    <slot name="left"></slot>
    <el-button
      v-show="deleteVisible"
      data-report-tool="delete"
      :icon="isIconOnly ? undefined : $legacyIcon(isShowIcon ? 'el-icon-delete' : '')"
      :size="controlSize"
      :link="!!isText"
      :type="isText ? undefined : 'danger'"
      :class="[isText ? 'red' : '', isIconOnly ? 'report-tools__icon-btn' : '']"
      :plain="isIconOnly || (!isShowIcon && !isText)"
      :aria-label="$i18n.t('delete')"
      :title="isIconOnly ? $i18n.t('delete') : undefined"
      @click="handleDelete"
    >
      <legacy-icon v-if="isIconOnly" icon="el-icon-delete" />
      <template v-else>{{ $i18n.t('delete') }}</template>
    </el-button>
    <slot name="right"></slot>
  </div>
</template>

<script>
import {checkPermi} from "@/utils/permission";
import i18n from "@/utils/i18n/i18n";
import {delReport} from "@/api/system/report";
import { mapElementPlusSize } from '@/utils/element-plus-size'
import { storeToRefs } from "pinia";
import { useUserStore } from "@/stores/user";

export default {
  emits: ['update:report', 'update'],
  name: "ReportTools",
  components: { },
  setup() {
    const userStore = useUserStore()
    const { id } = storeToRefs(userStore)
    return { id }
  },
  props:{
    report: {
      type: Object,
      default: {}
    },
    isText: {
      type: Boolean,
      default: null
    },
    isShowIcon: {
      type: Boolean,
      default: null
    },
    size: {
      type: String,
      default: 'mini'
    }
  },
  computed: {
    controlSize() {
      return mapElementPlusSize(this.size)
    },
    isIconOnly() {
      return !!this.isShowIcon && !this.isText
    },
    /** 获取当前用户id */
    currentUserId: function() {
      return this.id;
    },
    deleteVisible: function () {
      return this.report.createById==this.currentUserId || checkPermi(['system:report:remove']);
    },
  },
  methods:{
    /** 删除按钮操作 */
    handleDelete(event) {
      const reportIds = this.report.reportId;
      this.$modal.confirm(
        this.$i18n.t('report.delete-report'),
        this.$i18n.t('prompted').toString(),
        {
          confirmButtonText: i18n.t('delete').toString(),
          cancelButtonText: i18n.t('cancel').toString(),
          confirmButtonClass: 'delete-button',
          type: "warning"
        }).then(function() {
        return delReport(reportIds);
      }).then(() => {
        this.$emit('delete',this.report);
        this.$modal.msgSuccess(this.$i18n.t('delete.success'));
      }).catch(() => {});
      event.stopPropagation();
    },
  }
}
</script>

<style lang="scss" scoped>
  .report-tools {
    display: inline-flex;
    flex-wrap: nowrap;
    flex-direction: row;
    column-gap: var(--cat2bug-operate-tools-gap, 10px);
    row-gap: var(--cat2bug-operate-tools-row-gap, 0);
    align-items: center;
    justify-content: flex-start;
    font-size: 12px;
    margin-bottom: 0px !important;
    max-width: 100%;
    min-width: 0;
    box-sizing: border-box;
    > * {
      margin: 0;
      flex-shrink: 0;
    }
  }
  .report-tools__bar {
    overflow: visible;
  }
  .red {
    color: #f56c6c;
  }
</style>
