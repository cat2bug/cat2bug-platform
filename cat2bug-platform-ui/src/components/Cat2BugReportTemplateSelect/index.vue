<template>
  <div :class="[hostClass, { 'is-popover-open': popoverOpen }]">
    <el-popover
      ref="templatePopover"
      placement="bottom"
      width="613"
      trigger="hover"
      :popper-class="popperClass">
      <div class="template-header">
        <span>{{$t('report.select-template')}}</span>
        <div class="report-template-add-link-wrap">
          <el-link
            ref="addTemplateLink"
            class="report-template-add-link"
            icon="el-icon-plus"
            @click="addReportTemplate">{{$t('report.add-template')}}</el-link>
        </div>
      </div>
      <div class="template-list" ref="templateList">
        <div
          v-for="(t,index) in templateList"
          class="template-list-item"
          :key="t.templateId != null ? t.templateId : index"
          role="button"
          :aria-label="t.templateTitle"
          @click="createReportHandle(t)">
          <el-image fit="contain" :src="iconUrl(t)" />
          <span>{{t.templateTitle}}</span>
          <div class="menu">
            <el-button type="text" size="mini" class="report-template-card-copy" @click="copyReportTemplate($event,t)">
              <svg-icon icon-class="copy"/>
            </el-button>
            <el-button type="text" size="mini" class="report-template-card-edit" @click="goReportTemplateView($event,t.templateId)">
              <svg-icon icon-class="edit"/>
            </el-button>
            <el-button type="text" size="mini" class="report-template-card-delete" @click="deleteReport($event,t.templateId,index)">
              <svg-icon icon-class="close"/>
            </el-button>
          </div>
        </div>
      </div>
      <div class="template-list-pagination report-template-pagination">
        <el-pagination
          small
          layout="prev, pager, next"
          :current-page.sync="query.pageNum"
          :page-size.sync="query.pageSize"
          :total="total"
          @size-change="getTemplateList"
          @current-change="onTemplatePageChange">
        </el-pagination>
      </div>
      <el-button
        ref="triggerButton"
        class="report-hint-create"
        type="primary"
        slot="reference"
        plain
        icon="el-icon-plus"
        :size="size"
      >{{$t('report.make')}}</el-button>
    </el-popover>
  </div>
</template>

<script>
import {addReportTemplate, delTemplate, listReportTemplate} from "@/api/system/ReportTemplate";
import {addReport} from "@/api/system/report";
import i18n from "@/utils/i18n/i18n";
import {strFormat} from "@/utils";
import {
  REPORT_TEMPLATE_HOST_CLASS,
  REPORT_TEMPLATE_POPPER_CLASS,
  closeReportTemplatePopover,
  clearReportTemplatePopoverFocus,
  destroyReportTemplatePopoverKbd,
  initReportTemplatePopoverKbd,
  shortcutOpenReportTemplatePopover,
  syncReportTemplateCardFocus,
  focusReportTemplateCardWhenReady
} from '@/utils/report-template-popover-kbd'

export default {
  name: "Cat2BugReportTemplateSelect",
  data() {
    return {
      hostClass: REPORT_TEMPLATE_HOST_CLASS,
      popperClass: REPORT_TEMPLATE_POPPER_CLASS,
      popoverOpen: false,
      templateList: [],
      total: 0,
      query: {
        projectId: this.projectId,
        pageNum: 1,
        pageSize: 9
      },
      template: {
        templateTitle: null,
        moduleType: null,
        projectId: null,
        templateContent: '',
        majorVersion: 1,
        minorVersion: 0,
      },
    }
  },
  props: {
    projectId: {
      type: Number,
      default: null
    },
    size: {
      type: String,
      default: 'small'
    }
  },
  computed: {
    iconUrl() {
      return function (template) {
        if(template.templateIconUrl) {
          return process.env.VUE_APP_BASE_API + template.templateIconUrl;
        } else {
          return require('@/assets/images/empty.png')
        }
      }
    },
  },
  mounted() {
    this.getTemplateList();
    initReportTemplatePopoverKbd(this);
    this.$nextTick(() => this.bindPopoverOpenWatch());
  },
  beforeDestroy() {
    destroyReportTemplatePopoverKbd(this);
    this.unbindPopoverOpenWatch();
  },
  methods: {
    bindPopoverOpenWatch() {
      const pop = this.$refs.templatePopover;
      if (!pop || this._popoverOpenWatch) return;
      this.popoverOpen = !!pop.showPopper;
      this._popoverOpenWatch = pop.$watch('showPopper', (visible) => {
        this.popoverOpen = !!visible;
      });
    },
    unbindPopoverOpenWatch() {
      if (this._popoverOpenWatch) {
        this._popoverOpenWatch();
        this._popoverOpenWatch = null;
      }
    },
    openTemplatePopover() {
      shortcutOpenReportTemplatePopover(this);
    },
    createReportFromCardIndex(index) {
      const template = this.templateList[index];
      if (!template) return;
      this.createReportHandle(template);
      closeReportTemplatePopover(this);
    },
    copyReportTemplateAtIndex(index) {
      const template = this.templateList[index];
      if (!template) return;
      this.copyReportTemplate({ stopPropagation() {} }, template);
    },
    editReportTemplateAtIndex(index) {
      const template = this.templateList[index];
      if (!template) return;
      this.goReportTemplateView({ stopPropagation() {} }, template.templateId);
    },
    deleteReportTemplateAtIndex(index) {
      const template = this.templateList[index];
      if (!template) return;
      this.deleteReport({ stopPropagation() {} }, template.templateId, index);
    },
    findTemplateIndexById(templateId) {
      return this.templateList.findIndex((t) => t.templateId === templateId);
    },
    focusTemplateCardAfterDelete(deletedIndex) {
      if (!this.popoverOpen) return;
      const count = this.templateList.length;
      if (!count) {
        clearReportTemplatePopoverFocus(this);
        return;
      }
      const idx = deletedIndex >= 0 ? deletedIndex : 0;
      const nextIndex = Math.min(idx, count - 1);
      focusReportTemplateCardWhenReady(this, nextIndex);
    },
    onTemplatePageChange() {
      this.getTemplateList();
      this.$nextTick(() => {
        if (this.templateList.length && this.popoverOpen) {
          syncReportTemplateCardFocus(this, 0);
        }
      });
    },
    getTemplateList() {
      this.query.projectId = this.projectId;
      return listReportTemplate(this.query).then(res => {
        this.templateList = res.rows;
        this.total = res.total;
        return res;
      });
    },
    goReportTemplateView(event,templateId) {
      this.$router.push({name:'AddDefectReport', query:{templateId: templateId}});
      if(event) {
        event.stopPropagation();
      }
    },
    deleteReport(event, templateId, deletedIndex = -1) {
      const removedIndex = deletedIndex >= 0 ? deletedIndex : this.findTemplateIndexById(templateId);
      this.$modal.confirm(
        this.$i18n.t('report.delete-template'),
        this.$i18n.t('prompted').toString(),
        {
          confirmButtonText: i18n.t('delete').toString(),
          cancelButtonText: i18n.t('cancel').toString(),
          confirmButtonClass: 'delete-button',
          type: "warning"
        }).then(function() {
        return delTemplate(templateId);
      }).then(() => {
        return this.getTemplateList().then(() => removedIndex);
      }).then((idx) => {
        this.$modal.msgSuccess(this.$i18n.t('delete.success'));
        this.focusTemplateCardAfterDelete(idx);
      }).catch(() => {});
      if (event && typeof event.stopPropagation === 'function') {
        event.stopPropagation();
      }
    },
    addReportTemplate() {
      this.template.projectId = this.projectId;
      this.template.templateTitle = this.$i18n.t('report.default-title');
      addReportTemplate(this.template).then(res=>{
        if(res.data) {
          this.$message.success(this.$i18n.t('report.create-template-success').toString());
          this.goReportTemplateView(null, res.data.templateId);
        } else {
          this.$message.error(this.$i18n.t('report.create-template-fail').toString());
        }
      });
    },
    copyReportTemplate(event,template) {
      const param = this.copyTemplateObj(template);
      addReportTemplate(param).then(res=>{
        if(res.data) {
          this.$message.success(this.$i18n.t('report.copy-template-success').toString());
          this.goReportTemplateView(null, res.data.templateId);
        } else {
          this.$message.error(this.$i18n.t('report.create-template-fail').toString());
        }
      });
      event.stopPropagation();
    },
    copyTemplateObj(template) {
      return {
        templateTitle: template.templateTitle+'-'+this.$i18n.t('copy'),
        moduleType: template.moduleType,
        projectId: template.projectId,
        templateContent: template.templateContent,
        templateIconUrl: template.templateIconUrl,
        majorVersion: 1,
        minorVersion: 0,
      }
    },
    createReportHandle(template) {
      let params = {
        reportTitle: template.templateTitle,
        reportDescription: template.templateContent,
        projectId: this.projectId,
        params: {
          vueAppBaseApi: process.env.VUE_APP_BASE_API
        }
      }
      addReport(params).then(res=>{
        this.$message.success(strFormat(this.$i18n.t('report.create-success'),params.reportTitle));
        this.$emit('create', params);
      });
    },
  }
}
</script>

<style lang="scss" scoped>
.template-header {
  display: inline-flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  span {
    font-size: 16px;
    color: var(--text-color-primary);
  }
  .el-link {
    color: #1890ff;
  }
}
.report-template-add-link-wrap {
  position: relative;
  overflow: visible;
}
.template-list {
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 5px;
  padding-top: 10px;
  .template-list-item:hover {
    cursor: pointer;
    box-shadow: 0 3px 5px rgba(0, 0, 0, 0.1);
  }
  .template-list-item {
    display: flex;
    position: relative;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 5px;
    border-radius: 5px;
    border: 1px solid var(--border-color-light);
    background-color: var(--bg-color-block);
    outline: none;
    .menu {
      display: inline-flex;
      flex-direction: row;
      gap: 0px;
      position: absolute;
      right: 5px;
      top: 5px;
      button {
        padding: 5px;
        margin: 0px;
      }
      button:hover {
        background-color: var(--table-row-hover-bg);
      }
    }
    .el-image {
      width: 180px;
      height: 120px;
      background-color: var(--input-bg);
      border-radius: 4px;
      overflow: hidden;
    }
    span {
      width: 180px;
      display: -webkit-box;
      -webkit-line-clamp: 1;
      -webkit-box-orient: vertical;
      text-align: center;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: normal;
      padding-top: 10px;
      color: var(--text-color-regular);
    }
  }
}
.template-list-pagination {
  display: inline-flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  width: 100%;
}
</style>
