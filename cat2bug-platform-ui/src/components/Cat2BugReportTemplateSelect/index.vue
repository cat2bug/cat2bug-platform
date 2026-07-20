<template>
  <div :class="[hostClass, { 'is-popover-open': popoverOpen }]">
    <el-popover
      ref="templatePopover"
      placement="bottom"
      width="640"
      trigger="hover"
      :popper-class="popperClass">
      <div class="template-header">
        <span>{{$t('report.select-template')}}</span>
      </div>
      <div class="template-picker-body" ref="templatePickerBody">
        <section
          v-for="group in pickerGroups"
          :key="group.code"
          class="template-group">
          <div class="template-group-header">
            <span class="template-group-title">{{ groupTitle(group) }}</span>
            <div
              v-if="group.code === 'custom'"
              class="report-template-add-link-wrap">
              <el-link
                ref="addTemplateLink"
                class="report-template-add-link"
                :icon="$legacyIcon('el-icon-plus')"
                @click="addReportTemplate">{{$t('report.add-template')}}</el-link>
            </div>
          </div>
          <div class="template-list" ref="templateList">
            <div
              v-for="(t, index) in group.items"
              class="template-list-item"
              :key="templateCardKey(t, group.code, index)"
              role="button"
              :aria-label="t.templateTitle"
              @click="createReportHandle(t)">
              <cat2-bug-image fit="contain" :src="iconUrl(t)" />
              <span>{{ t.templateTitle }}</span>
              <div class="menu">
                <el-button link size="small" class="report-template-card-copy" @click="copyReportTemplate($event,t)">
                  <svg-icon icon-class="copy"/>
                </el-button>
                <el-button
                  v-if="isCustomTemplate(t)"
                  link
                  size="small"
                  class="report-template-card-edit"
                  @click="goReportTemplateView($event,t.templateId)">
                  <svg-icon icon-class="edit"/>
                </el-button>
                <el-button
                  v-if="isCustomTemplate(t)"
                  link
                  size="small"
                  class="report-template-card-delete"
                  @click="deleteReport($event,t.templateId, group.code, index)">
                  <svg-icon icon-class="close"/>
                </el-button>
              </div>
            </div>
          </div>
          <div
            v-if="group.code === 'custom' && customTotal > query.pageSize"
            class="template-list-pagination report-template-pagination">
            <el-pagination
              size="small"
              layout="prev, pager, next"
              v-model:current-page="query.pageNum"
              v-model:page-size="query.pageSize"
              :total="customTotal"
              @size-change="onCustomPageSizeChange"
              @current-change="onTemplatePageChange">
            </el-pagination>
          </div>
        </section>
      </div>
      <template #reference>
        <el-button
          ref="triggerButton"
          class="report-hint-create"
          type="primary"
          plain
          :icon="$legacyIcon('el-icon-plus')"
          :size="controlSize"
        >{{$t('report.make')}}</el-button>
      </template>
    </el-popover>
  </div>
</template>

<script>
import {
  addReportTemplate,
  copyPresetTemplate,
  delTemplate,
  listReportTemplate,
  fetchPresetPreviewBlob,
  pickerReportTemplates,
  presetTemplatePreviewPath,
  REPORT_TEMPLATE_PICKER_GROUP_ORDER
} from "@/api/system/ReportTemplate";
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
import emptyImg from '@/assets/images/empty.png'
import { mapElementPlusSize } from '@/utils/element-plus-size'
import Cat2BugImage from '@/components/Cat2BugImage'
import { resolveUploadUrl } from '@/utils/upload-asset'

const EMPTY_PICKER_GROUPS = REPORT_TEMPLATE_PICKER_GROUP_ORDER.map((code) => ({
  code,
  title: null,
  items: []
}))

export default {
  name: "Cat2BugReportTemplateSelect",
  components: { Cat2BugImage },
  data() {
    return {
      hostClass: REPORT_TEMPLATE_HOST_CLASS,
      popperClass: REPORT_TEMPLATE_POPPER_CLASS,
      popoverOpen: false,
      pickerGroups: EMPTY_PICKER_GROUPS.map((g) => ({ ...g, items: [] })),
      customTotal: 0,
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
      /** path → object URL；预制预览需带 token 拉取 */
      previewBlobUrls: {}
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
    controlSize() {
      return mapElementPlusSize(this.size)
    },
    flatTemplateList() {
      const list = []
      this.pickerGroups.forEach((group) => {
        (group.items || []).forEach((item) => list.push(item))
      })
      return list
    },
    customGroupItems() {
      const custom = this.pickerGroups.find((g) => g.code === 'custom')
      return custom && custom.items ? custom.items : []
    },
  },
  mounted() {
    this.loadPicker();
    initReportTemplatePopoverKbd(this);
    this.$nextTick(() => this.bindPopoverOpenWatch());
  },
  beforeDestroy() {
    destroyReportTemplatePopoverKbd(this);
    this.unbindPopoverOpenWatch();
    this.closeTemplatePopover();
    this.revokePreviewBlobs();
  },
  beforeUnmount() {
    destroyReportTemplatePopoverKbd(this);
    this.unbindPopoverOpenWatch();
    this.closeTemplatePopover();
    this.revokePreviewBlobs();
  },
  deactivated() {
    this.closeTemplatePopover();
  },
  methods: {
    getReportTemplateFlatList() {
      return this.flatTemplateList
    },
    isCustomTemplate(template) {
      if (!template) return false
      if (template.source === 'preset') return false
      if (template.source === 'custom') return true
      return template.templateId != null
    },
    isPresetTemplate(template) {
      return !this.isCustomTemplate(template)
    },
    isReportTemplateEditableAtIndex(index) {
      const template = this.flatTemplateList[index]
      return this.isCustomTemplate(template)
    },
    isReportTemplateDeletableAtIndex(index) {
      return this.isReportTemplateEditableAtIndex(index)
    },
    groupTitle(group) {
      if (!group) return ''
      const i18nKey = `report.template-group.${group.code}`
      const translated = this.$t(i18nKey)
      if (translated !== i18nKey) return translated
      return group.title || group.code
    },
    templateCardKey(template, groupCode, index) {
      if (template && template.templateId != null) {
        return `custom-${template.templateId}`
      }
      const key = template && (template.templateKey || template.key)
      if (key) return `preset-${groupCode}-${key}`
      return `${groupCode}-${index}`
    },
    closeTemplatePopover() {
      closeReportTemplatePopover(this);
      this.popoverOpen = false;
    },
    iconUrl(template) {
      const cacheKey = this.previewCacheKey(template)
      if (cacheKey && this.previewBlobUrls[cacheKey]) {
        return this.previewBlobUrls[cacheKey]
      }
      // 预制：等带 token 的 blob 就绪再显示，避免 <img> 命中浏览器里旧的黑底缓存
      if (this.isPresetTemplate(template)) {
        return emptyImg
      }
      if (template && template.templateIconUrl) {
        return resolveUploadUrl(template.templateIconUrl, process.env.VUE_APP_BASE_API);
      }
      return emptyImg;
    },
    previewCacheKey(template) {
      if (!template) return ''
      if (this.isPresetTemplate(template)) {
        return presetTemplatePreviewPath(
          template.groupCode,
          template.templateKey || template.key,
          template.presetFolder || template.folder
        ) || ''
      }
      return template.templateIconUrl || ''
    },
    revokePreviewBlobs() {
      Object.keys(this.previewBlobUrls || {}).forEach((key) => {
        const url = this.previewBlobUrls[key]
        if (url && typeof URL !== 'undefined' && url.indexOf('blob:') === 0) {
          try { URL.revokeObjectURL(url) } catch (e) { /* ignore */ }
        }
      })
      this.previewBlobUrls = {}
    },
    hydratePresetPreviewBlobs() {
      this.revokePreviewBlobs()
      const tasks = []
      this.pickerGroups.forEach((group) => {
        (group.items || []).forEach((item) => {
          if (!this.isPresetTemplate(item)) return
          const cacheKey = this.previewCacheKey(item)
          if (!cacheKey) return
          const groupCode = item.groupCode || group.code
          const key = item.templateKey || item.key
          const folder = item.presetFolder || item.folder
          tasks.push(
            fetchPresetPreviewBlob(groupCode, key, folder).then((blob) => {
              if (!blob) return
              const objectUrl = URL.createObjectURL(blob)
              this.$set
                ? this.$set(this.previewBlobUrls, cacheKey, objectUrl)
                : (this.previewBlobUrls = { ...this.previewBlobUrls, [cacheKey]: objectUrl })
            })
          )
        })
      })
      return Promise.all(tasks)
    },
    bindPopoverOpenWatch() {
      const pop = this.$refs.templatePopover;
      if (!pop || this._popoverOpenWatch) return;
      this.popoverOpen = !!pop.showPopper;
      this._popoverOpenWatch = pop.$watch('showPopper', (visible) => {
        this.popoverOpen = !!visible;
        if (visible) {
          this.loadPicker()
        }
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
    flatIndexForCustomItem(groupCode, indexInGroup) {
      if (groupCode !== 'custom') return -1
      let offset = 0
      for (const group of this.pickerGroups) {
        if (group.code === 'custom') {
          return offset + indexInGroup
        }
        offset += (group.items || []).length
      }
      return offset + indexInGroup
    },
    createReportFromCardIndex(index) {
      const template = this.flatTemplateList[index];
      if (!template) return;
      this.createReportHandle(template);
      closeReportTemplatePopover(this);
    },
    copyReportTemplateAtIndex(index) {
      const template = this.flatTemplateList[index];
      if (!template) return;
      this.copyReportTemplate({ stopPropagation() {} }, template);
    },
    editReportTemplateAtIndex(index) {
      if (!this.isReportTemplateEditableAtIndex(index)) return
      const template = this.flatTemplateList[index];
      if (!template) return;
      this.goReportTemplateView({ stopPropagation() {} }, template.templateId);
    },
    deleteReportTemplateAtIndex(index) {
      if (!this.isReportTemplateDeletableAtIndex(index)) return
      const template = this.flatTemplateList[index];
      if (!template) return;
      this.deleteReport({ stopPropagation() {} }, template.templateId, 'custom', this.customGroupItems.indexOf(template));
    },
    findTemplateIndexById(templateId) {
      return this.flatTemplateList.findIndex((t) => t.templateId === templateId);
    },
    focusTemplateCardAfterDelete(flatIndex) {
      if (!this.popoverOpen) return;
      const count = this.flatTemplateList.length;
      if (!count) {
        clearReportTemplatePopoverFocus(this);
        return;
      }
      const idx = flatIndex >= 0 ? flatIndex : 0;
      const nextIndex = Math.min(idx, count - 1);
      focusReportTemplateCardWhenReady(this, nextIndex);
    },
    onCustomPageSizeChange() {
      this.query.pageNum = 1
      this.onTemplatePageChange()
    },
    onTemplatePageChange() {
      this.loadPicker();
      this.$nextTick(() => {
        if (this.flatTemplateList.length && this.popoverOpen) {
          syncReportTemplateCardFocus(this, 0);
        }
      });
    },
    normalizePickerItem(item, groupCode) {
      const normalized = { ...item }
      if (!normalized.groupCode) normalized.groupCode = groupCode
      if (!normalized.source) {
        normalized.source = groupCode === 'custom' ? 'custom' : 'preset'
      }
      if (normalized.key && !normalized.templateKey) {
        normalized.templateKey = normalized.key
      }
      if (groupCode === 'custom') {
        if (normalized.editable == null) normalized.editable = true
        if (normalized.deletable == null) normalized.deletable = true
      } else if (normalized.editable == null) {
        normalized.editable = false
        normalized.deletable = false
      }
      return normalized
    },
    mergeOrderedPickerGroups(apiGroups) {
      const byCode = {}
      ;(apiGroups || []).forEach((group) => {
        if (group && group.code) byCode[group.code] = group
      })
      return REPORT_TEMPLATE_PICKER_GROUP_ORDER.map((code) => {
        const fromApi = byCode[code] || {}
        const items = (fromApi.items || fromApi.rows || []).map((item) =>
          this.normalizePickerItem(item, code)
        )
        return {
          code,
          title: fromApi.title || null,
          items,
          total: fromApi.total
        }
      })
    },
    applyCustomListFallback(res) {
      const rows = res.rows || []
      this.pickerGroups = this.mergeOrderedPickerGroups([
        {
          code: 'custom',
          items: rows.map((row) => this.normalizePickerItem(row, 'custom'))
        }
      ])
      this.customTotal = res.total != null ? res.total : rows.length
    },
    loadPicker() {
      this.query.projectId = this.projectId;
      if (this.projectId == null) {
        this.pickerGroups = EMPTY_PICKER_GROUPS.map((g) => ({ ...g, items: [] }))
        this.customTotal = 0
        return Promise.resolve()
      }
      return pickerReportTemplates(this.projectId, {
        pageNum: this.query.pageNum,
        pageSize: this.query.pageSize
      }).then((res) => {
        const payload = res && res.data != null ? res.data : res
        const groups = payload.groups || payload.groupList || []
        this.pickerGroups = this.mergeOrderedPickerGroups(groups)
        const customGroup = this.pickerGroups.find((g) => g.code === 'custom')
        const totalFromGroup = customGroup && customGroup.total
        const totalFromPayload = payload.customTotal != null ? payload.customTotal : payload.total
        this.customTotal = totalFromGroup != null
          ? totalFromGroup
          : (totalFromPayload != null ? totalFromPayload : (customGroup.items || []).length)
        this.hydratePresetPreviewBlobs()
        return res
      }).catch(() => {
        return listReportTemplate(this.query).then((res) => {
          this.applyCustomListFallback(res)
          return res
        })
      })
    },
    goReportTemplateView(event,templateId) {
      this.$router.push({name:'AddDefectReport', query:{templateId: templateId}});
      if(event) {
        event.stopPropagation();
      }
    },
    deleteReport(event, templateId, groupCode, indexInGroup) {
      const flatBefore = this.flatIndexForCustomItem(groupCode, indexInGroup)
      const removedIndex = flatBefore >= 0
        ? flatBefore
        : this.findTemplateIndexById(templateId);
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
        return this.loadPicker().then(() => removedIndex);
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
    copyReportTemplate(event, template) {
      if (this.isPresetTemplate(template)) {
        const group = template.groupCode
        const key = template.templateKey || template.key
        const folder = template.presetFolder || template.folder
        copyPresetTemplate(group, key, this.projectId, folder).then(res => {
          if (res.data) {
            this.$message.success(this.$i18n.t('report.copy-template-success').toString());
            this.goReportTemplateView(null, res.data.templateId);
          } else {
            this.$message.error(this.$i18n.t('report.create-template-fail').toString());
          }
        })
        event.stopPropagation();
        return
      }
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
        projectId: template.projectId || this.projectId,
        templateContent: template.templateContent,
        templateIconUrl: template.templateIconUrl,
        majorVersion: 1,
        minorVersion: 0,
      }
    },
    buildAddReportParams(template) {
      const params = {
        reportTitle: template.templateTitle,
        reportDescription: template.templateContent || '',
        projectId: this.projectId,
        params: {
          vueAppBaseApi: process.env.VUE_APP_BASE_API
        }
      }
      if (this.isPresetTemplate(template)) {
        const presetGroup = template.groupCode
        const presetKey = template.templateKey || template.key
        const presetFolder = template.presetFolder || template.folder
        params.source = 'preset'
        params.presetGroup = presetGroup
        params.presetKey = presetKey
        if (presetFolder) params.presetFolder = presetFolder
        Object.assign(params.params, {
          source: 'preset',
          presetGroup,
          presetKey
        })
        if (presetFolder) params.params.presetFolder = presetFolder
      }
      return params
    },
    createReportFromCopiedPreset(template, baseParams) {
      const params = {
        ...baseParams,
        source: undefined,
        presetGroup: undefined,
        presetKey: undefined,
        presetFolder: undefined,
        reportDescription: template.templateContent || baseParams.reportDescription,
        params: {
          vueAppBaseApi: process.env.VUE_APP_BASE_API
        }
      }
      return addReport(params)
    },
    createReportHandle(template) {
      const params = this.buildAddReportParams(template)
      addReport(params).then(res=>{
        this.$message.success(strFormat(this.$i18n.t('report.create-success'), params.reportTitle));
        this.$emit('create', params);
      }).catch(() => {
        if (!this.isPresetTemplate(template)) return
        const group = template.groupCode
        const key = template.templateKey || template.key
        const folder = template.presetFolder || template.folder
        return copyPresetTemplate(group, key, this.projectId, folder).then((copyRes) => {
          if (!copyRes.data) return Promise.reject(copyRes)
          return this.createReportFromCopiedPreset(copyRes.data, params).then(() => {
            this.$message.success(strFormat(this.$i18n.t('report.create-success'), params.reportTitle));
            this.$emit('create', params);
          })
        })
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
}
.template-picker-body {
  max-height: 460px;
  overflow-y: auto;
  overflow-x: hidden;
  width: 100%;
  padding-right: 4px;
}
.template-group-header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-top: 12px;
  .template-group-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-color-primary);
  }
  .el-link {
    color: #1890ff;
  }
}
.template-group:first-child .template-group-header {
  margin-top: 4px;
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
  width: 100%;
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
      z-index: 2;
      padding: 1px;
      border-radius: 4px;
      /* 预览图固定白底，工具条需自带衬底与深色图标，深/浅主题都可读 */
      background: rgba(255, 255, 255, 0.92);
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.18);
      button {
        padding: 5px;
        margin: 0px;
        color: #606266;
      }
      .svg-icon {
        color: #606266;
      }
      button:hover {
        background-color: rgba(0, 0, 0, 0.06);
        color: #303133;
        .svg-icon {
          color: #303133;
        }
      }
    }
    .cat2-bug-image,
    .el-image {
      width: 180px;
      height: 120px;
      /* 固定浅底：深色主题下也能看清白底预览与占位 */
      background-color: #f5f6f8;
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
  padding-top: 8px;
}
</style>
