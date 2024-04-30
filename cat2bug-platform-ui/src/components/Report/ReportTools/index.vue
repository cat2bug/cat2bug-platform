<template>
  <div class="report-tools">
    <slot name="left"></slot>
    <el-button v-show="deleteVisible" :icon="isShowIcon?'el-icon-delete':''" :size="size" :type="isText?'text':'danger'" :class="isText?'red':''" :plain="!isShowIcon" @click="handleDelete">{{$i18n.t('delete')}}</el-button>
    <slot name="right"></slot>
  </div>
</template>

<script>
import {checkPermi} from "@/utils/permission";
import i18n from "@/utils/i18n/i18n";
import {delReport} from "@/api/system/report";

export default {
  name: "ReportTools",
  components: { },
  model: {
    prop: 'report',
    event: 'update'
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
    /** 获取当前用户id */
    currentUserId: function() {
      return this.$store.state.user.id;
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
    display: flex;
    flex-wrap: wrap;
    flex-direction: row;
    gap: 10px;
    align-items: center;
    justify-content: flex-start !important;
    font-size: 12px;
    margin-bottom: 0px !important;
    > * {
      margin: 0;
    }
  }
  .red {
    color: #f56c6c;
  }
</style>
