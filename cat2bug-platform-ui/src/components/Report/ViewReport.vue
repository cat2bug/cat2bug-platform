<template>
  <el-drawer
    ref="viewReportDrawer"
    class="report-drawer-accent"
    size="90%"
    v-model="visible"
    direction="rtl"
    destroy-on-close
    :show-close="false"
    :close-on-press-escape="false"
    :before-close="closeReportDrawer"
    @closed="onReportDrawerClosed">
    <template #header>
      <div class="report-edit-header">
        <div class="report-edit-title">
          <legacy-icon icon="el-icon-arrow-left" class="report-view-back" @click="cancel"></legacy-icon>
          <focus-member-list
            v-model:members="report.focusList"
            module-name="report"
            :data-id="report.reportId"
            :tooltip="false"
          />
          <div class="report-edit-title-wrap">
            <template v-if="!isEditingTitle">
              <h4 class="report-edit-title-num" :title="report.reportTitle">{{ report.reportTitle }}</h4>
              <el-button
                v-if="canEdit"
                class="report-edit-title-btn"
                link
                size="small"
                :icon="$legacyIcon('el-icon-edit')"
                :title="$t('report.edit-title')"
                @click.stop="startEditTitle"
              ></el-button>
            </template>
            <el-input
              v-else
              ref="titleInput"
              v-model="titleDraft"
              class="report-edit-title-input"
              size="small"
              maxlength="255"
              :placeholder="$t('report.please-enter-title')"
              @keyup.enter="commitEditTitle"
              @keydown.esc.prevent="cancelEditTitle"
              @blur="commitEditTitle"
            />
          </div>
        </div>
        <div class="report-edit-tools">
          <report-tools :report="report" :is-show-icon="true" size="small" @delete="handleDelete" />
          <el-dropdown
            ref="exportDropdown"
            class="report-view-export cat2bug-split-dropdown-kbd"
            data-report-tool="export"
            @command="handleExport">
            <el-button size="small" plain>
              <legacy-icon icon="el-icon-download"></legacy-icon>
            </el-button>
            <template #dropdown>
          <el-dropdown-menu>
              <el-dropdown-item command="word"><svg-icon icon-class="docx" />Word 导出</el-dropdown-item>
              <el-dropdown-item command="pdf"><svg-icon icon-class="pdf" />PDF 导出</el-dropdown-item>
            </el-dropdown-menu>
        </template>
          </el-dropdown>
        </div>
      </div>
    </template>
    <div
      v-if="visible"
      class="report-detail-shell"
      @mousedown="handleShellMouseDown"
      @scroll.capture="onAnnotationOverlayScroll"
    >
      <div class="report-detail-body" :class="{ 'is-pane-resizing': paneResizing }">
      <report-detail-toc
        v-show="!tocCollapsed"
        class="report-detail-toc--resizable"
        :style="{ width: tocWidth + 'px' }"
        :headings="headings"
        :active-id="activeHeadingId"
        :reorderable="isEditing"
        @select="handleTocSelect"
        @reorder="handleTocReorder"
        @toggle-collapse="tocCollapsed = true"
      />
      <div
        v-show="!tocCollapsed"
        class="report-pane-resizer"
        role="separator"
        aria-orientation="vertical"
        :title="$t('report.resize-toc')"
        @mousedown.prevent="startPaneResize('toc', $event)"
      ></div>
      <div ref="reportMainScroll" class="report-detail-main">
        <report-content-toolbar
          v-if="isEditing"
          :dirty="isDirty"
          :saving="saving"
          :status-text="saveStatusText"
          @insert="onToolbarInsert"
          @insert-tool="onToolbarInsertTool"
          @save="saveReportContent()"
          @clear="onToolbarClear"
        />
        <div class="report-pane-edge-bar" aria-hidden="false">
          <el-button
            v-if="tocCollapsed"
            class="report-pane-edge-btn report-pane-edge-btn--left"
            link
            size="small"
            :icon="$legacyIcon('el-icon-notebook-2')"
            :title="$t('report.expand-toc')"
            @click="tocCollapsed = false"
          ></el-button>
          <el-button
            v-if="commentsCollapsed"
            class="report-pane-edge-btn report-pane-edge-btn--right"
            link
            size="small"
            :icon="$legacyIcon('el-icon-chat-line-round')"
            :title="$t('report.expand-comments')"
            @click="commentsCollapsed = false"
          ></el-button>
        </div>
        <div
          ref="report-data"
          id="report-data"
          class="app-container"
          :class="{ 'is-editing': isEditing }"
          v-loading="loading"
          @scroll="onMainScroll"
          @mouseup="handleBodyMouseUp"
          @mousedown="handleBodyMouseDown"
          @mousemove="handleBodyMouseMove"
          @mouseleave="handleBodyMouseLeave"
          @click="handleBodyClick">
          <div
            v-if="isEditing"
            class="report-template-editor report-body-visual-editor"
            :class="{ 'report-template-editor--dark': isEditorDark }"
          >
            <milkdown-editor
              ref="reportEditor"
              class="report-edit-pane"
              :project-id="report.projectId || projectId"
              :enable-dsl="false"
              :enable-selection-comment="true"
              v-model="editDraft"
              @change="onEditDraftChange"
              @selection-comment="handleEditorSelectionComment"
            />
          </div>
          <markdown-it-vue
            v-show="!isEditing"
            ref="markdownView"
            class="md markdown-body"
            :options="options"
            :content="displayReportDescription" />
        </div>
        <div
          v-if="!isEditing && lineButton.visible"
          class="report-line-comment-button"
          :style="{ top: lineButton.top + 'px', left: lineButton.left + 'px' }"
          :title="$t('report.line-comment')"
          @mousedown.stop.prevent
          @click="handleAddLineComment">
          <legacy-icon icon="el-icon-chat-line-round"></legacy-icon>
        </div>
      </div>
      <div
        v-show="!commentsCollapsed"
        class="report-pane-resizer"
        role="separator"
        aria-orientation="vertical"
        :title="$t('report.resize-comments')"
        @mousedown.prevent="startPaneResize('side', $event)"
      ></div>
      <report-detail-side
        ref="reportSide"
        v-show="!commentsCollapsed"
        class="report-detail-side--resizable"
        :style="{ width: sideWidth + 'px' }"
        v-model:side-tab="sideTab"
        :threads="threads"
        :active-thread-id="activeThreadId"
        :draft="draftAnnotation"
        :draft-top="draftTop"
        :thread-tops="threadTops"
        :comment-canvas-height="commentCanvasHeight"
        :current-user-id="currentUserId"
        :can-manage="isSuperAdmin"
        :comments-loading="commentsLoading"
        :history-items="historyItems"
        :history-loading="historyLoading"
        :can-edit-history="canEdit"
        @toggle-collapse="commentsCollapsed = true"
        @focus-thread="handleFocusThread"
        @submit-draft="submitDraft"
        @cancel-draft="cancelDraft"
        @reply="submitReply"
        @delete="handleDeleteComment"
        @history-restore="handleRestoreHistory"
        @history-delete="handleDeleteHistory"
        @comments-scroll="onCommentsScroll"
        @layout-request="scheduleCommentLayout"
      />
      </div>
      <div
        v-if="(isEditing && bodyHighlightRects.length) || annotationConnector"
        class="report-annotation-overlay"
        aria-hidden="true"
      >
        <div
          v-for="(r, i) in bodyHighlightRects"
          :key="'ahl-' + i"
          class="report-annotation-overlay__rect"
          :style="{
            top: r.top + 'px',
            left: r.left + 'px',
            width: r.width + 'px',
            height: r.height + 'px'
          }"
        ></div>
        <svg
          v-if="annotationConnector"
          class="report-annotation-overlay__connector"
          xmlns="http://www.w3.org/2000/svg"
        >
          <path
            class="report-annotation-overlay__connector-path"
            :d="annotationConnector.d"
            fill="none"
          />
        </svg>
      </div>
      <div class="report-status-bar">
        <div class="report-status-bar__item">
          <legacy-icon icon="el-icon-document" class="report-status-bar__icon"></legacy-icon>
          <span class="report-status-bar__label">{{ $t('report.type') }}</span>
          <span class="report-status-bar__value">{{ reportTypeLabel }}</span>
        </div>
        <div class="report-status-bar__item">
          <legacy-icon icon="el-icon-user" class="report-status-bar__icon"></legacy-icon>
          <span class="report-status-bar__label">{{ $t('pusher') }}</span>
          <span class="report-status-bar__value" :title="report.createBy || ''">{{ report.createBy || '—' }}</span>
        </div>
        <div class="report-status-bar__item">
          <legacy-icon icon="el-icon-time" class="report-status-bar__icon"></legacy-icon>
          <span class="report-status-bar__label">{{ $t('report.time') }}</span>
          <span class="report-status-bar__value">{{ reportTimeLabel }}</span>
        </div>
        <div class="report-status-bar__item">
          <legacy-icon icon="el-icon-monitor" class="report-status-bar__icon"></legacy-icon>
          <span class="report-status-bar__label">{{ $t('report.source') }}</span>
          <span class="report-status-bar__value" :title="report.reportSource || ''">{{ report.reportSource || '—' }}</span>
        </div>
        <div class="report-status-bar__item report-status-bar__item--chars">
          <legacy-icon icon="el-icon-edit-outline" class="report-status-bar__icon"></legacy-icon>
          <span class="report-status-bar__label">{{ $t('report.char-count') }}</span>
          <span class="report-status-bar__value">{{ reportCharCount }}</span>
        </div>
      </div>
      <report-selection-toolbar
        :visible="!isEditing && selectionToolbar.visible"
        :top="selectionToolbar.top"
        :left="selectionToolbar.left"
        @add-comment="handleAddSelectionComment"
      />
    </div>
  </el-drawer>
</template>

<script>
import { MarkdownRenderer as MarkdownItVue } from '@/components/Markdown'
import multimdTable from 'markdown-it-multimd-table'
import FileSaver from "file-saver";
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import {closeEditWindow, getReport, updateReport, listReportHistory, restoreReportHistory, delReportHistory} from "@/api/system/report";
import {listComment, addComment, delComment} from "@/api/system/comment";
import ReportTools from "./ReportTools/index"
import FocusMemberList from "@/components/FocusMemberList";
import ReportDetailToc from "@/components/Report/ReportDetailToc.vue";
import ReportDetailSide from "@/components/Report/ReportDetailSide.vue";
import ReportSelectionToolbar from "@/components/Report/ReportSelectionToolbar.vue";
import MilkdownEditor from "@/components/ReportTemplateEditor/MilkdownEditor.vue";
import ReportContentToolbar from "@/components/Report/ReportContentToolbar.vue";
import { resolveToolContent } from "@/components/ReportTemplateEditor/composables/useReportTemplateTools";
import { normalizeDslSource } from "@/components/ReportTemplateEditor/dsl/normalize";
import { rewriteReportSnapshotImageSrcs } from "@/components/ReportTemplateEditor/milkdown/rewrite-report-snapshot-images";
import {extractHeadings, createScrollSpy, reorderMarkdownHeadingBlocks, splitMarkdownHeadingBlocks} from "@/components/Report/annotation/report-toc";
import { countReportChars } from "@/components/Report/annotation/report-char-count";
import "@/components/ReportTemplateEditor/milkdown-crepe-theme.scss";
import { isReportEditorDarkMode } from "@/components/ReportTemplateEditor/report-editor-theme";
import { useSettingsStore } from "@/stores/settings";
import {
  ANNOTATABLE_SELECTOR,
  getBlockText,
  rematchAnnotation,
  rematchAnchor,
  buildPlainTextIndex,
  rangeFromPlainOffsets,
  extractSelectionContext,
  clearAnnotationHighlights,
  highlightQuoteInElement
} from "@/components/Report/annotation/report-anchor-rematch";
import {checkPermi, checkRole} from "@/utils/permission";
import {pinia} from "@/store/pinia.js";
import {useUserStore} from "@/stores/user";
import {getToken} from "@/utils/auth";
import {setHeader} from "@/utils/request";
import '@/assets/font/simsun-normal';
import {listDefect} from "@/api/system/defect";
import {TablePlugin} from "@/components/Cat2BugMarkdown/plugins/TablePlugin";
import {CardPlugin} from "@/components/Cat2BugMarkdown/plugins/CardPlugin";
import {listCase} from "@/api/system/case";
import {getProject} from "@/api/system/project";
import {ImagePlugin} from "@/components/Cat2BugMarkdown/plugins/ImagePlugin";
import {VariablePlugin} from "@/components/Cat2BugMarkdown/plugins/VariablePlugin";
import {markdownData} from "@/api/markdown/markdown";
import {CaseCardPlugin} from "@/components/Cat2BugMarkdown/plugins/CaseCardPlugin";
import {LeftPointPlugin} from "@/components/Cat2BugMarkdown/plugins/LeftPointPlugin";
import {RightPointPlugin} from "@/components/Cat2BugMarkdown/plugins/RightPointPlugin";
import {CenterPointPlugin} from "@/components/Cat2BugMarkdown/plugins/CenterPointPlugin";
import {PlanVariablePlugin} from "@/components/Cat2BugMarkdown/plugins/PlanVariablePlugin";
import viewReportKbd from '@/mixins/view-report-kbd'
import {
  isReportContentDirty,
  REPORT_AUTOSAVE_INTERVAL_MS
} from '@/components/Report/annotation/report-edit-autosave'
import {
  REPORT_EXPORT_CONTENT_WIDTH,
  createReportMarkdownExportHost,
  readMarkdownPreviewHtml
} from '@/components/Report/annotation/report-export-surface'
import {
  prepareWordExportHtml,
  buildWordExportDocument
} from '@/components/Report/annotation/report-word-export'
import { htmlDocxAsBlob } from '@/utils/html-docx-runtime'
import { scrollTargetToPeerHeight } from '@/components/Report/annotation/report-scroll-align'
import {
  resolveCommentCollisions,
  resolveDraftTopAmongComments,
  findElementByQuote,
  getContentOffsetTop,
  clampCommentCardHeight
} from '@/components/Report/annotation/report-comment-layout'

const userStore = useUserStore(pinia)

const REPORT_PANE_LAYOUT_KEY = 'report-detail-pane-layout'
const TOC_WIDTH_DEFAULT = 220
const SIDE_WIDTH_DEFAULT = 320
const TOC_WIDTH_MIN = 160
const TOC_WIDTH_MAX = 420
const SIDE_WIDTH_MIN = 260
const SIDE_WIDTH_MAX = 560

function clampPaneWidth(value, min, max, fallback) {
  const n = Number(value)
  if (!Number.isFinite(n)) {
    return fallback
  }
  return Math.min(max, Math.max(min, Math.round(n)))
}

export default {
  name: "ViewReport",
  mixins: [viewReportKbd],
  components: { MarkdownItVue,ReportTools,FocusMemberList,ReportDetailToc,ReportDetailSide,ReportSelectionToolbar,MilkdownEditor,ReportContentToolbar },
  data() {
    return {
      loading: false,
      // 显示窗口
      visible: false,
      markdownPluginsReady: false,
      report: {},
      options: {
        markdownIt: {
          html: true,
          linkify: true
        },
      },
      // 目录（TOC）
      tocCollapsed: false,
      tocWidth: TOC_WIDTH_DEFAULT,
      headings: [],
      activeHeadingId: null,
      // 评论/批注 / 历史
      commentsCollapsed: false,
      sideWidth: SIDE_WIDTH_DEFAULT,
      paneResizing: false,
      sideTab: 'comments',
      comments: [],
      commentsLoading: false,
      rematchMap: {},
      activeThreadId: null,
      /** 编辑态高亮遮罩（不改 ProseMirror DOM，避免重绘丢 class） */
      bodyHighlightRects: [],
      /** 正文高亮 ↔ 右侧评论块连线（视口坐标 SVG path） */
      annotationConnector: null,
      draftAnnotation: null,
      draftTop: null,
      threadTops: {},
      commentCanvasHeight: 0,
      commentLayoutTimer: null,
      syncingScroll: false,
      historyItems: [],
      historyLoading: false,
      // 正文编辑与自动保存
      isEditing: false,
      editDraft: '',
      savedDescription: '',
      saving: false,
      lastSavedAt: null,
      autosaveTimer: null,
      // 标题就地编辑
      isEditingTitle: false,
      titleDraft: '',
      titleSaving: false,
      // 选区浮条与整行批注入口
      selectionToolbar: { visible: false, top: 0, left: 0 },
      pendingSelectionRange: null,
      lineButton: { visible: false, top: 0, left: 0 },
      hoverLineBlock: null,
      reportBodyObserver: null,
      scrollSpyHandle: null,
      refreshDetailTimer: null,
      applyingHighlights: false,
      applyingToc: false
    }
  },
  props: {
    projectId: {
      type: Number,
      default: null
    },
  },
  deactivated() {
    if (this.visible) {
      this.flushSaveOnLeave(true)
      this.visible = false
    }
  },
  beforeUnmount() {
    this.teardownAutosave()
    this.unbindLeaveGuards()
    if (this.visible) {
      this.flushSaveOnLeave(true)
      this.visible = false
    }
    this.teardownReportDetailObservers();
  },
  computed: {
    getUrl: function () {
      return function (urls){
        let imgs = urls?urls.split(','):[];
        return imgs.map(i=>{
          return process.env.VUE_APP_BASE_API + i;
        })
      }
    },
    currentUserId() {
      return userStore.id;
    },
    isSuperAdmin() {
      return checkRole(['admin']);
    },
    canEdit() {
      return checkPermi(['system:report:edit']);
    },
    isEditorDark() {
      const settingsStore = useSettingsStore(pinia);
      return settingsStore.themeMode === 'dark' || isReportEditorDarkMode();
    },
    /** markdown-it 预览：把 /static/img 项目图标解析成可加载 URL（不写回正文） */
    displayReportDescription() {
      return rewriteReportSnapshotImageSrcs((this.report.reportDescription || '') + '')
    },
    isDirty() {
      return isReportContentDirty(this.editDraft, this.savedDescription);
    },
    saveStatusText() {
      if (this.saving) {
        return this.$t('report.saving');
      }
      if (this.isDirty) {
        return this.$t('report.unsaved');
      }
      if (this.lastSavedAt) {
        return this.$t('report.saved');
      }
      return '';
    },
    reportTypeLabel() {
      return this.report && this.report.reportKey
        ? this.$t('report.type-runtime')
        : this.$t('report.type-text');
    },
    reportTimeLabel() {
      if (!this.report || !this.report.reportTime) {
        return '—';
      }
      return this.parseTime(this.report.reportTime, '{y}-{m}-{d} {h}:{i}:{s}') || '—';
    },
    /** 底部字数：编辑态跟草稿，预览态跟当前报告正文 */
    reportCharCount() {
      const src = this.isEditing
        ? this.editDraft
        : (this.editDraft || (this.report && this.report.reportDescription) || '');
      return countReportChars(src);
    },
    /** 按 parentId 归并为线程（主评 + 扁平回复），并附上锚点重匹配结果 */
    threads() {
      const roots = this.comments.filter(c => !c.parentId);
      const repliesByParent = new Map();
      this.comments.forEach(c => {
        if (c.parentId) {
          if (!repliesByParent.has(c.parentId)) {
            repliesByParent.set(c.parentId, []);
          }
          repliesByParent.get(c.parentId).push(c);
        }
      });
      return roots
        .map(root => {
          const replies = (repliesByParent.get(root.commentId) || [])
            .slice()
            .sort((a, b) => new Date(a.createTime) - new Date(b.createTime));
          const rematch = this.rematchMap[String(root.commentId)] || this.rematchMap[root.commentId] || { matched: false, ambiguous: false, element: null };
          return {
            root,
            replies,
            matched: rematch.matched,
            ambiguous: rematch.ambiguous,
            element: rematch.element
          };
        })
        .sort((a, b) => new Date(b.root.createTime) - new Date(a.root.createTime));
    },
  },
  watch: {
    sideTab(val) {
      this.savePaneLayout();
      if (val === 'history' && this.report.reportId) {
        this.loadHistory();
      }
      if (val === 'comments') {
        this.scheduleCommentLayout();
      }
    },
    isDirty(val) {
      if (val) {
        this.setupAutosave();
      }
    },
    isEditing() {
      this.$nextTick(() => {
        // 进入/退出编辑时切换观察目标（Milkdown ↔ markdown-it），并刷新目录
        this.setupReportDetailObservers();
        this.scheduleCommentLayout();
        if (this.comments.length) {
          this.refreshAnchors();
        }
      });
    },
    threads() {
      this.$nextTick(() => this.scheduleCommentLayout());
    },
    tocCollapsed() {
      this.savePaneLayout();
    },
    commentsCollapsed(val) {
      this.savePaneLayout();
      if (!val) {
        this.scheduleCommentLayout();
      }
    }
  },
  methods:{
    /**
     * Export capture: render current content via markdown-it, then mount a
     * fixed-width light print host (A4 column). Never screenshot Milkdown.
     * @param {(ctx: { element: HTMLElement, htmlRoot: HTMLElement }) => Promise<void>|void} fn
     */
    async withExportCapture(fn) {
      let hostPack = null;
      try {
        await this.withMarkdownExportPreview(async () => {
          const previewRoot = this.$refs['report-data'];
          // 批注 <mark> 在页面上有自定义样式；导出离线容器会落到浏览器默认黄底
          if (previewRoot) {
            clearAnnotationHighlights(previewRoot);
          }
          const html = readMarkdownPreviewHtml(previewRoot);
          if (!html || !String(html).trim()) {
            throw new Error('export-empty');
          }
          hostPack = createReportMarkdownExportHost({
            html,
            width: REPORT_EXPORT_CONTENT_WIDTH
          });
          if (!hostPack) {
            throw new Error('export-empty');
          }
          await this.$nextTick();
          // allow images to decode in the off-screen host
          await new Promise((resolve) => setTimeout(resolve, 120));
          await fn({ element: hostPack.element, htmlRoot: hostPack.body || hostPack.element });
        });
      } finally {
        if (hostPack) {
          hostPack.destroy();
        }
      }
    },
    /**
     * Temporarily switch Milkdown → markdown-it preview so plugins render static HTML.
     * @param {() => Promise<void>|void} fn
     */
    async withMarkdownExportPreview(fn) {
      const wasEditing = this.isEditing;
      if (wasEditing) {
        this.report = {
          ...this.report,
          reportDescription: this.editDraft == null ? '' : String(this.editDraft)
        };
        this.isEditing = false;
        await this.$nextTick();
        // wait for markdown-it / table / chart plugins
        await new Promise((resolve) => setTimeout(resolve, 400));
      }
      try {
        return await fn();
      } finally {
        if (wasEditing) {
          this.isEditing = true;
          await this.$nextTick();
        }
      }
    },
    /** 带超时的 html2canvas，避免一直转圈 */
    html2canvasWithTimeout(element, options = {}, timeoutMs = 60000) {
      return Promise.race([
        html2canvas(element, {
          useCORS: true,
          allowTaint: true,
          logging: false,
          backgroundColor: '#ffffff',
          scale: 2,
          ...options
        }),
        new Promise((_, reject) => {
          setTimeout(() => reject(new Error('export-timeout')), timeoutMs);
        })
      ]);
    },
    /** 导出 PDF：浅色固定栏宽 markdown 预览 */
    async exportPDF() {
      this.loading = true;
      try {
        await this.withExportCapture(async ({ element }) => {
          const canvas = await this.html2canvasWithTimeout(element);
          const imgData = canvas.toDataURL('image/png');
          const pdf = new jsPDF({
            orientation: 'portrait',
            unit: 'px',
            format: 'a4'
          });
          const imgProps = pdf.getImageProperties(imgData);
          const pdfWidth = pdf.internal.pageSize.getWidth();
          const pageHeight = pdf.internal.pageSize.getHeight();
          const pdfHeight = (imgProps.height * pdfWidth) / imgProps.width;
          try {
            pdf.setFont('simsun');
          } catch (_e) {
            // 字体可选
          }
          let heightLeft = pdfHeight;
          let position = 0;
          pdf.addImage(imgData, 'PNG', 0, position, pdfWidth, pdfHeight);
          heightLeft -= pageHeight;
          while (heightLeft > 0) {
            position = heightLeft - pdfHeight;
            pdf.addPage();
            pdf.addImage(imgData, 'PNG', 0, position, pdfWidth, pdfHeight);
            heightLeft -= pageHeight;
          }
          pdf.save((this.report.reportTitle || 'report') + '.pdf');
        });
      } catch (e) {
        const msg = e && e.message === 'export-timeout'
          ? (this.$t('report.export-timeout') || '导出超时，请稍后重试')
          : (this.$t('report.export-failed') || '导出失败，请稍后重试');
        this.$message.error(String(msg));
      } finally {
        this.loading = false;
      }
    },
    setHide(ele) {
      ele.setAttribute('cat2bug-temp-display', ele.style.display?ele.style.display:'inline-flex');
      ele.style.display = 'none';
    },
    removeHide(ele) {
      if(ele.getAttribute('cat2bug-temp-display')) {
        ele.style.display = ele.getAttribute('cat2bug-temp-display');
      }
    },
    /** 根据样式类查找元素并转为图片（限定在导出根节点内） */
    async classToImages(className, rootEl) {
      const scope = rootEl || document;
      const contents = scope.getElementsByClassName
        ? scope.getElementsByClassName(className)
        : document.getElementsByClassName(className);
      let images = [];
      for(let i=0;i<contents.length;i++) {
        await this.html2canvasWithTimeout(contents[i]).then(canvas=>{
          let image = this.canvasToImage(canvas);
          contents[i].classList.add('print-hide')
          this.setHide(contents[i]);
          contents[i].insertAdjacentElement('afterend', image);
          images.push(image);
        });
      }
      return images;
    },
    /** 移除元素的指定样式 */
    removeClass(className) {
      const contents = document.getElementsByClassName(className);
      while (contents.length>0) {
        this.removeHide(contents[0]);
        contents[0].classList.remove(className);
      }
    },
    /** 画板转图片 */
    canvasToImage(canvas) {
      let image = document.createElement('img');
      image.src = canvas.toDataURL("image/png");
      image.setAttribute('width', canvas.style.width?parseInt(canvas.style.width)+'':'500');
      image.setAttribute('height', canvas.style.height?parseInt(canvas.style.height)+'':'200');
      canvas.insertAdjacentElement('afterend', image);
      return image;
    },
    convertCanvasesToImagesIn(rootEl) {
      if (!rootEl) {
        return [];
      }
      let canvases = rootEl.getElementsByTagName('canvas');
      let images = [];
      for (let i=0;i<canvases.length;i++) {
        let canvas = canvases[i];
        let image = document.createElement('img');
        try {
          image.src = canvas.toDataURL("image/png");
        } catch (_e) {
          continue;
        }
        image.setAttribute('width', canvas.style.width?parseInt(canvas.style.width)+'':'500');
        image.setAttribute('height', canvas.style.height?parseInt(canvas.style.height)+'':'200');
        canvas.insertAdjacentElement('afterend', image);
        images.push(image);
      }
      return images;
    },
    /** 导出 Word：真实 docx + 图片内联 + 表格限宽 */
    async exportWord() {
      this.loading = true;
      try {
        await this.withExportCapture(async ({ htmlRoot }) => {
          let tempImages = this.convertCanvasesToImagesIn(htmlRoot);
          const classes = ['markdown-it-vue-alter'];
          for (let i = 0; i < classes.length; i++) {
            let imgs = await this.classToImages(classes[i], htmlRoot);
            tempImages = [...tempImages, ...imgs];
          }
          const bodyHtml = await prepareWordExportHtml(htmlRoot);
          const content = buildWordExportDocument(bodyHtml, {
            title: this.report.reportTitle || 'report'
          });
          const converted = await htmlDocxAsBlob(content);
          FileSaver.saveAs(converted, (this.report.reportTitle || 'report') + '.docx');
          for (let i = 0; i < tempImages.length; i++) {
            tempImages[i].remove();
          }
          this.removeClass('print-hide');
        });
      } catch (e) {
        const msg = e && e.message === 'export-timeout'
          ? (this.$t('report.export-timeout') || '导出超时，请稍后重试')
          : (this.$t('report.export-failed') || '导出失败，请稍后重试');
        this.$message.error(String(msg));
        try {
          this.removeClass('print-hide');
        } catch (_e) {
          // ignore
        }
      } finally {
        this.loading = false;
      }
    },
    /** 获取报告信息 */
    getReportInfo(reportId) {
      this.loading = true;
      getReport(reportId).then(res=>{
        this.loading = false;
        res.data.reportDescription = (res.data.reportDescription || '').replace(' \n ','\n');
        this.report = res.data;
        const description = normalizeDslSource(res.data.reportDescription || '');
        this.savedDescription = description;
        this.editDraft = description;
        // 有编辑权限时直接进入正文编辑（无顶部编辑按钮）
        this.isEditing = this.canEdit;
        this.loadComments();
        this.loadHistory();
        this.bindLeaveGuards();
        if (this.isEditing) {
          this.setupAutosave();
        }
        this.$nextTick(() => {
          this.setupReportDetailObservers();
        });
      }).catch(e=>{
        this.loading = false;
      });
    },
    /** 打开操作 */
    open(reportId) {
      this.visible = true;
      this.loadPaneLayout();
      this.$nextTick(()=>{
        this.initMarkdownPlug(reportId);
      })
    },
    loadPaneLayout() {
      const narrowDefault = typeof window !== 'undefined' && window.innerWidth < 992;
      try {
        const cached = this.$cache && this.$cache.local
          ? this.$cache.local.getJSON(REPORT_PANE_LAYOUT_KEY)
          : null;
        // 兼容旧 key（仅宽度）
        const legacy = (!cached && this.$cache && this.$cache.local)
          ? this.$cache.local.getJSON('report-detail-pane-widths')
          : null;
        const source = cached || legacy;
        if (source && typeof source === 'object') {
          this.tocWidth = clampPaneWidth(source.tocWidth, TOC_WIDTH_MIN, TOC_WIDTH_MAX, TOC_WIDTH_DEFAULT);
          this.sideWidth = clampPaneWidth(source.sideWidth, SIDE_WIDTH_MIN, SIDE_WIDTH_MAX, SIDE_WIDTH_DEFAULT);
          this.tocCollapsed = typeof source.tocCollapsed === 'boolean' ? source.tocCollapsed : false;
          this.commentsCollapsed = typeof source.commentsCollapsed === 'boolean'
            ? source.commentsCollapsed
            : narrowDefault;
          this.sideTab = source.sideTab === 'history' ? 'history' : 'comments';
          return;
        }
      } catch (e) {
        // ignore corrupt cache
      }
      this.tocWidth = TOC_WIDTH_DEFAULT;
      this.sideWidth = SIDE_WIDTH_DEFAULT;
      this.tocCollapsed = false;
      this.commentsCollapsed = narrowDefault;
      this.sideTab = 'comments';
    },
    savePaneLayout() {
      if (!this.visible || !this.$cache || !this.$cache.local) {
        return;
      }
      this.$cache.local.setJSON(REPORT_PANE_LAYOUT_KEY, {
        tocWidth: this.tocWidth,
        sideWidth: this.sideWidth,
        tocCollapsed: !!this.tocCollapsed,
        commentsCollapsed: !!this.commentsCollapsed,
        sideTab: this.sideTab === 'history' ? 'history' : 'comments'
      });
    },
    startPaneResize(which, event) {
      if (which !== 'toc' && which !== 'side') {
        return;
      }
      const startX = event.pageX;
      const startToc = this.tocWidth;
      const startSide = this.sideWidth;
      this.paneResizing = true;
      const onMove = (ev) => {
        const delta = ev.pageX - startX;
        if (which === 'toc') {
          this.tocWidth = clampPaneWidth(startToc + delta, TOC_WIDTH_MIN, TOC_WIDTH_MAX, TOC_WIDTH_DEFAULT);
        } else {
          // 分隔条在侧栏左侧：向右拖 → 主栏变宽、侧栏变窄
          this.sideWidth = clampPaneWidth(startSide - delta, SIDE_WIDTH_MIN, SIDE_WIDTH_MAX, SIDE_WIDTH_DEFAULT);
        }
      };
      const onUp = () => {
        window.removeEventListener('mousemove', onMove);
        window.removeEventListener('mouseup', onUp);
        this.paneResizing = false;
        this.savePaneLayout();
        this.scheduleCommentLayout();
      };
      window.addEventListener('mousemove', onMove);
      window.addEventListener('mouseup', onUp);
    },
    /** 取消返回 */
    async cancel() {
      await this.flushSaveOnLeave(false);
      this.visible = false;
    },
    onReportDrawerClosed() {
      this.teardownAutosave();
      this.unbindLeaveGuards();
      this.markdownPluginsReady = false;
      this.report = {};
      this.loading = false;
      this.isEditing = false;
      this.editDraft = '';
      this.savedDescription = '';
      this.saving = false;
      this.lastSavedAt = null;
      this.isEditingTitle = false;
      this.titleDraft = '';
      this.titleSaving = false;
      this.teardownReportDetailObservers();
      this.headings = [];
      this.activeHeadingId = null;
      this.comments = [];
      this.rematchMap = {};
      this.activeThreadId = null;
      this.bodyHighlightRects = [];
      this.annotationConnector = null;
      this.draftAnnotation = null;
      this.draftTop = null;
      this.threadTops = {};
      this.commentCanvasHeight = 0;
      this.historyItems = [];
      this.selectionToolbar = { visible: false, top: 0, left: 0 };
      this.pendingSelectionRange = null;
      this.lineButton = { visible: false, top: 0, left: 0 };
      this.hoverLineBlock = null;
    },
    handleDelete() {
      this.$emit('delete');
      this.cancel();
    },
    startEditTitle() {
      if (!this.canEdit || !this.report.reportId) {
        return;
      }
      this.titleDraft = this.report.reportTitle || '';
      this.isEditingTitle = true;
      this.$nextTick(() => {
        const input = this.$refs.titleInput;
        if (input && typeof input.focus === 'function') {
          input.focus();
        }
        if (input && typeof input.select === 'function') {
          input.select();
        } else {
          const native = input && input.$el && input.$el.querySelector('input');
          if (native && typeof native.select === 'function') {
            native.select();
          }
        }
      });
    },
    cancelEditTitle() {
      this.isEditingTitle = false;
      this.titleDraft = '';
    },
    commitEditTitle() {
      if (!this.isEditingTitle || this.titleSaving) {
        return;
      }
      const next = (this.titleDraft || '').trim();
      if (!next) {
        this.$message.warning(this.$t('report.please-enter-title').toString());
        this.$nextTick(() => {
          const input = this.$refs.titleInput;
          if (input && typeof input.focus === 'function') {
            input.focus();
          }
        });
        return;
      }
      if (next === (this.report.reportTitle || '')) {
        this.cancelEditTitle();
        return;
      }
      this.titleSaving = true;
      updateReport({
        reportId: this.report.reportId,
        reportTitle: next
      }).then(() => {
        this.report = { ...this.report, reportTitle: next };
        this.isEditingTitle = false;
        this.titleDraft = '';
        this.$message.success(this.$t('report.save-success').toString());
        this.$emit('update', this.report);
      }).catch(() => {}).finally(() => {
        this.titleSaving = false;
      });
    },
    /** 表单重置 */
    reset() {
      this.form = {
        defectId: null,
        defectType: null,
        defectName: null,
        defectDescribe: null,
        annexUrls: null,
        projectId: null,
        testPlanId: null,
        caseId: null,
        dataSources: null,
        dataSourcesParams: null,
        moduleId: null,
        moduleVersion: null,
        createBy: null,
        updateTime: null,
        createTime: null,
        updateBy: null,
        defectState: null,
        caseStepId: null,
        handleBy: null,
        handleTime: null,
        defectLevel: 'middle'
      };
      this.resetForm("form");
    },
    /** 关闭缺陷抽屉窗口 */
    async closeReportDrawer(done) {
      try {
        await this.flushSaveOnLeave(false);
      } catch (e) {
        // 仍允许关闭，避免卡住抽屉
      }
      done();
      closeEditWindow();
    },
    /** 导出 */
    handleExport(command) {
      switch (command) {
        case 'word':
          this.exportWord();
          break;
        case 'pdf':
          this.exportPDF();
          break;
      }
    },
    /** 初始化markdown插件 */
    initMarkdownPlug(reportId) {
      const loadReport = () => {
        this.reportId = reportId;
        this.getReportInfo(reportId);
      };
      if (this.markdownPluginsReady) {
        loadReport();
        return;
      }
      const markdownView = this.$refs.markdownView;
      if (!markdownView) {
        this.$nextTick(() => this.initMarkdownPlug(reportId));
        return;
      }
      // 可以合并行列的表格
      markdownView.use(multimdTable, {
        multiline:  true,
        rowspan:    true,
        headerless: true,
        multibody:  true,
        aotolabel:  true,
      });
      markdownView.use(LeftPointPlugin);
      markdownView.use(RightPointPlugin);
      markdownView.use(CenterPointPlugin);
      // 缺陷操作符
      const defectList = listDefect().then(res=>{
        markdownView.use(TablePlugin,{
          name: 'api.defect.list',
          value: res.rows
        });
        markdownView.use(CardPlugin,{
          name: 'api.defect.list',
          value: res.rows
        });
        return res;
      });
      // 测试用例操作符
      const caseList = listCase().then(res=>{
        markdownView.use(TablePlugin,{
          name: 'api.case.list',
          value: res.rows
        });
        return res;
      });
      // 项目操作符
      const project = getProject(this.projectId).then(res=>{
        markdownView.use(ImagePlugin,{
          name: 'api.project',
          value: res.data
        });
        markdownView.use(VariablePlugin,{
          name: 'api.project',
          value: res.data
        });
        return res;
      });
      const data = markdownData(this.projectId).then(res=>{
        markdownView.use(CaseCardPlugin,{
          name: 'api.case.list',
          value: res.data.case?res.data.case.list:[]
        });
        markdownView.use(VariablePlugin,{
          name: 'api.case.total',
          value: res.data.case?res.data.case.total:0
        });
        markdownView.use(PlanVariablePlugin,{
          name: 'api.plan',
          value: res.data.plan?res.data.plan.list:[]
        });
      });
      Promise.all([defectList,caseList,project,data]).then(()=>{
        this.markdownPluginsReady = true;
        loadReport();
      }).catch(e=>{
        this.$message.error(e);
      })
    },

    // ===================== 目录（TOC）与 scroll-spy =====================
    /** 监听正文 DOM 变化（渲染完成/内容编辑后）以刷新目录与批注锚点 */
    setupReportDetailObservers() {
      this.teardownReportDetailObservers();
      const shell = this.$refs['report-data'];
      // 编辑态只观察可见编辑器，避免扫到 v-show 隐藏的 markdown-it 标题
      const container = this.isEditing
        ? (this.getAnnotationContentRoot() || shell)
        : shell;
      if (!container || typeof MutationObserver === 'undefined') {
        this.refreshDetail();
        return;
      }
      this.reportBodyObserver = new MutationObserver(() => {
        if (this.applyingHighlights || this.applyingToc) {
          return;
        }
        // Milkdown 异步挂载后，从壳层切到 ProseMirror 再观察
        if (this.isEditing) {
          const liveRoot = this.getAnnotationContentRoot();
          if (liveRoot && liveRoot !== container && liveRoot.classList
            && liveRoot.classList.contains('ProseMirror')) {
            this.setupReportDetailObservers();
            return;
          }
        }
        this.scheduleRefreshDetail();
      });
      this.reportBodyObserver.observe(container, { childList: true, subtree: true, characterData: true });
      this.refreshDetail();
    },
    teardownReportDetailObservers() {
      if (this.reportBodyObserver) {
        this.reportBodyObserver.disconnect();
        this.reportBodyObserver = null;
      }
      if (this.scrollSpyHandle) {
        this.scrollSpyHandle.disconnect();
        this.scrollSpyHandle = null;
      }
      if (this.refreshDetailTimer) {
        clearTimeout(this.refreshDetailTimer);
        this.refreshDetailTimer = null;
      }
      if (this.commentLayoutTimer) {
        clearTimeout(this.commentLayoutTimer);
        this.commentLayoutTimer = null;
      }
    },
    scheduleRefreshDetail() {
      if (this.applyingHighlights || this.applyingToc) {
        return;
      }
      if (this.refreshDetailTimer) {
        clearTimeout(this.refreshDetailTimer);
      }
      this.refreshDetailTimer = setTimeout(() => this.refreshDetail(), 150);
    },
    scheduleCommentLayout() {
      if (this.commentLayoutTimer) {
        clearTimeout(this.commentLayoutTimer);
      }
      this.commentLayoutTimer = setTimeout(() => this.layoutCommentsByAnchor(), 80);
    },
    getCommentsScrollElement() {
      const side = this.$refs.reportSide;
      return side && typeof side.getCommentsScrollElement === 'function'
        ? side.getCommentsScrollElement()
        : null;
    },
    /** 正文滚动容器（编辑态滚动在 #report-data，预览态同） */
    getMainScrollElement() {
      return this.$refs['report-data'] || this.$refs.reportMainScroll || null;
    },
    /**
     * 批注匹配 / 高亮的实际内容根。
     * 编辑态与预览态并存于 #report-data（markdown 用 v-show 隐藏），必须限定到当前可见根，
     * 否则会匹配到隐藏正文，右侧点选后中间看不到高亮。
     */
    getAnnotationContentRoot() {
      const container = this.$refs['report-data'];
      if (!container) {
        return null;
      }
      if (this.isEditing) {
        return (
          container.querySelector('.report-body-visual-editor .ProseMirror') ||
          container.querySelector('.report-body-visual-editor') ||
          container
        );
      }
      return container.querySelector('.markdown-body') || container;
    },
    onMainScroll() {
      if (this.activeThreadId != null) {
        if (this.isEditing) {
          this.syncEditModeHighlightOverlay();
        } else {
          this.syncAnnotationConnector();
        }
      }
      if (this.syncingScroll || this.sideTab !== 'comments') {
        return;
      }
      const main = this.getMainScrollElement();
      const comments = this.getCommentsScrollElement();
      if (!main || !comments) {
        return;
      }
      this.syncingScroll = true;
      comments.scrollTop = main.scrollTop;
      this.$nextTick(() => { this.syncingScroll = false; });
    },
    onCommentsScroll() {
      if (this.syncingScroll || this.sideTab !== 'comments') {
        return;
      }
      const main = this.getMainScrollElement();
      const comments = this.getCommentsScrollElement();
      if (!main || !comments) {
        return;
      }
      this.syncingScroll = true;
      main.scrollTop = comments.scrollTop;
      this.$nextTick(() => { this.syncingScroll = false; });
    },
    /**
     * 编辑态：正文是 Milkdown，无法按 markdown 锚点对齐已有评论。
     * 已有评论自上而下堆叠；草稿按正文选区 Y 落位，并避开已有卡片。
     */
    layoutCommentsStacked() {
      if (this.sideTab !== 'comments') {
        return;
      }
      const provisional = {};
      let top = 8;
      this.threads.forEach(thread => {
        const id = String(thread.root.commentId);
        provisional[id] = top;
        top += 96;
      });
      this.threadTops = provisional;
      this.commentCanvasHeight = Math.max(top, 1);

      this.$nextTick(() => {
        let y = 8;
        const tops = {};
        let maxBottom = 1;
        this.threads.forEach(thread => {
          const id = String(thread.root.commentId);
          tops[id] = y;
          const el = typeof document !== 'undefined'
            ? document.querySelector(`[data-report-thread-id="${id}"]`)
            : null;
          const h = clampCommentCardHeight(el ? el.offsetHeight : 88);
          y += h + 8;
          maxBottom = Math.max(maxBottom, y);
        });
        this.threadTops = tops;
        if (this.draftAnnotation) {
          this.updateDraftTop();
          maxBottom = Math.max(maxBottom, (this.draftTop || 0) + 160);
        }
        this.commentCanvasHeight = maxBottom;
      });
    },
    /** 按正文锚点 Y 排布右侧评论；每条优先对齐自己的锚点，仅重叠时轻微下推 */
    layoutCommentsByAnchor() {
      if (this.sideTab !== 'comments') {
        return;
      }
      // 编辑态：已有评论堆叠；草稿仍按选区实际位置落位
      if (this.isEditing) {
        this.layoutCommentsStacked();
        return;
      }
      const scrollRoot = this.getMainScrollElement();
      const commentsBody = this.getCommentsScrollElement();
      const reportData = this.$refs['report-data'];
      if (!scrollRoot || !reportData) {
        return;
      }
      const viewportDelta = commentsBody
        ? scrollRoot.getBoundingClientRect().top - commentsBody.getBoundingClientRect().top
        : 0;

      // 布局时重新匹配锚点，避免 MutationObserver 后仍握着已脱离 DOM 的 element（top 会算成同一个值）
      const matchedItems = [];
      const orphanThreads = [];
      this.threads.forEach(thread => {
        const id = String(thread.root.commentId);
        const result = rematchAnnotation(reportData, thread.root);
        if (result.matched && result.element && scrollRoot.contains(result.element)) {
          const offset = getContentOffsetTop(scrollRoot, result.element);
          if (offset != null) {
            matchedItems.push({
              id,
              top: Math.max(0, offset + viewportDelta),
              height: 88
            });
            return;
          }
        }
        orphanThreads.push(thread);
      });

      // 先按锚点落位（不做碰撞），保证每条先对齐自己的正文
      const provisional = {};
      matchedItems.forEach(it => { provisional[it.id] = it.top; });
      this.threadTops = provisional;
      const contentHeight = Math.max(
        reportData.scrollHeight || 0,
        scrollRoot.scrollHeight || 0,
        scrollRoot.clientHeight || 0
      );
      this.commentCanvasHeight = Math.max(contentHeight, ...matchedItems.map(it => it.top + 120), 1);

      this.$nextTick(() => {
        matchedItems.forEach(it => {
          const el = typeof document !== 'undefined'
            ? document.querySelector(`[data-report-thread-id="${it.id}"]`)
            : null;
          const raw = el
            ? (el.getBoundingClientRect ? el.getBoundingClientRect().height : el.offsetHeight)
            : 88;
          it.height = clampCommentCardHeight(raw);
        });

        const resolved = resolveCommentCollisions(matchedItems, 8);
        const tops = {};
        let maxBottom = contentHeight;
        resolved.forEach(r => {
          tops[String(r.id)] = r.top;
          const src = matchedItems.find(i => String(i.id) === String(r.id));
          maxBottom = Math.max(maxBottom, r.top + (src ? src.height : 88) + 24);
        });

        // 原文已变更：放到正文末尾之后，不要跟在第一条评论下面
        let orphanTop = Math.max(contentHeight + 16, maxBottom + 16);
        orphanThreads.forEach(t => {
          const id = String(t.root.commentId);
          tops[id] = orphanTop;
          const el = typeof document !== 'undefined'
            ? document.querySelector(`[data-report-thread-id="${id}"]`)
            : null;
          const h = clampCommentCardHeight(el ? el.offsetHeight : 88);
          orphanTop += h + 8;
          maxBottom = Math.max(maxBottom, orphanTop);
        });

        this.threadTops = tops;
        if (this.draftAnnotation) {
          this.updateDraftTop();
          maxBottom = Math.max(maxBottom, (this.draftTop || 0) + 160);
        }
        this.commentCanvasHeight = maxBottom;
      });
    },
    /** 收集已有评论卡片占用区间（用于草稿避让） */
    getOccupiedCommentSlots() {
      return this.threads.map(thread => {
        const id = String(thread.root.commentId);
        const top = Object.prototype.hasOwnProperty.call(this.threadTops || {}, id)
          ? this.threadTops[id]
          : 0;
        const el = typeof document !== 'undefined'
          ? document.querySelector(`[data-report-thread-id="${id}"]`)
          : null;
        const height = clampCommentCardHeight(el ? el.offsetHeight : 88);
        return { id, top, height };
      });
    },
    updateDraftTop() {
      const scrollRoot = this.getMainScrollElement();
      const commentsBody = this.getCommentsScrollElement();
      if (!scrollRoot || !this.draftAnnotation) {
        this.draftTop = null;
        return;
      }
      const viewportDelta = commentsBody
        ? scrollRoot.getBoundingClientRect().top - commentsBody.getBoundingClientRect().top
        : 0;
      const rootRect = scrollRoot.getBoundingClientRect();
      let desired = null;

      // 1) 打开草稿时记录的选区/块矩形（最准）
      const rect = this.draftAnnotation.anchorRect;
      if (rect && typeof rect.top === 'number') {
        desired = rect.top - rootRect.top + scrollRoot.scrollTop + viewportDelta;
      }

      // 2) 悬停块 / 选区公共祖先
      if (desired == null) {
        let el = this.hoverLineBlock;
        if (!el && this.pendingSelectionRange) {
          try {
            const node = this.pendingSelectionRange.commonAncestorContainer;
            el = node && node.nodeType === 1 ? node : (node && node.parentElement);
          } catch (e) {
            el = null;
          }
        }
        if (el && typeof el.getBoundingClientRect === 'function') {
          const elRect = el.getBoundingClientRect();
          desired = elRect.top - rootRect.top + scrollRoot.scrollTop + viewportDelta;
        }
      }

      // 3) 用 quote 在可见正文中回找
      if (desired == null && this.draftAnnotation.quote) {
        const found = findElementByQuote(
          this.getAnnotationContentRoot() || this.$refs['report-data'],
          this.draftAnnotation.quote
        );
        if (found) {
          const offset = getContentOffsetTop(scrollRoot, found);
          if (offset != null) {
            desired = offset + viewportDelta;
          }
        }
      }

      if (desired == null) {
        desired = 8;
      }

      const draftEl = typeof document !== 'undefined'
        ? document.querySelector('.report-detail-comments-draft')
        : null;
      const draftHeight = draftEl ? draftEl.offsetHeight : 140;
      this.draftTop = resolveDraftTopAmongComments(
        desired,
        this.getOccupiedCommentSlots(),
        draftHeight,
        8
      );
    },
    /** 重新扫描标题生成目录 + scroll-spy；预览态同时重匹配批注锚点 */
    refreshDetail() {
      const container = this.getAnnotationContentRoot() || this.$refs['report-data'];
      if (!container) {
        return;
      }
      this.applyingToc = true;
      try {
        this.headings = extractHeadings(container);
        if (this.activeHeadingId && !this.headings.some(h => h.id === this.activeHeadingId)) {
          this.activeHeadingId = this.headings.length ? this.headings[0].id : null;
        }
        if (this.scrollSpyHandle) {
          this.scrollSpyHandle.disconnect();
        }
        this.scrollSpyHandle = createScrollSpy({
          root: this.getMainScrollElement(),
          headings: this.headings,
          onActiveChange: (id) => { this.activeHeadingId = id; }
        });
      } finally {
        this.$nextTick(() => {
          this.applyingToc = false;
        });
      }
      // 编辑态输入频繁，仅刷新目录；批注锚点在进出编辑 / 显式操作时再匹配
      if (!this.isEditing) {
        this.refreshAnchors();
        this.scheduleCommentLayout();
      }
    },
    /** 点击目录项：滚动正文到对应标题 */
    handleTocSelect(id) {
      const heading = this.headings.find(h => h.id === id);
      if (heading && heading.el && heading.el.scrollIntoView) {
        heading.el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        this.activeHeadingId = id;
      }
    },
    /**
     * 目录拖拽重排 → 同步中间 Markdown 正文（Milkdown 经 v-model 实时替换）。
     * 每个目录项对应「该标题 + 下一标题前的正文」整块移动。
     */
    handleTocReorder({ oldIndex, newIndex, headings: nextHeadings }) {
      if (!this.isEditing || !Number.isInteger(oldIndex) || !Number.isInteger(newIndex)) {
        return;
      }
      if (oldIndex === newIndex) {
        return;
      }
      const { headingBlocks } = splitMarkdownHeadingBlocks(this.editDraft || '');
      // 目录与 Markdown 标题数不一致时拒绝，避免错位改写
      if (headingBlocks.length !== (this.headings || []).length) {
        return;
      }
      const next = reorderMarkdownHeadingBlocks(this.editDraft || '', oldIndex, newIndex);
      if (next === (this.editDraft || '')) {
        return;
      }
      // 先更新本地目录顺序，正文 replaceAll 后再由 observer 精刷
      if (Array.isArray(nextHeadings) && nextHeadings.length) {
        this.headings = nextHeadings.map(h => ({
          id: h.id,
          text: h.text,
          level: h.level,
          el: h.el
        }));
      }
      this.editDraft = next;
      this.onEditDraftChange(next);
    },

    // ===================== 批注锚点重匹配与高亮 =====================
    /** 用当前正文重新匹配所有主评的锚点，未命中标记为 orphaned（不自动删除） */
    refreshAnchors() {
      const shell = this.$refs['report-data'];
      const container = this.getAnnotationContentRoot();
      if (!shell || !container) {
        return;
      }
      // 先清掉旧 mark / class（整壳清理，避免隐藏预览树残留）
      this.applyingHighlights = true;
      clearAnnotationHighlights(shell);
      const map = {};
      this.comments.filter(c => !c.parentId).forEach(root => {
        map[String(root.commentId)] = rematchAnnotation(container, root);
      });
      this.rematchMap = map;
      this.applyHighlightClasses();
      this.applyingHighlights = false;
      this.scheduleCommentLayout();
    },
    applyHighlightClasses() {
      const shell = this.$refs['report-data'];
      const container = this.getAnnotationContentRoot();
      if (!shell || !container) {
        return;
      }
      this.applyingHighlights = true;
      clearAnnotationHighlights(shell);
      const nextMap = { ...this.rematchMap };
      // rematch 可能握着已清掉的旧节点，按当前可见根再匹配一次
      Object.keys(nextMap).forEach(id => {
        const comment = this.comments.find(c => String(c.commentId) === String(id));
        if (comment) {
          nextMap[id] = rematchAnnotation(container, comment);
        }
      });
      // 编辑态：ProseMirror 会重绘丢掉 class，只更新 rematchMap，视觉交给遮罩层
      if (this.isEditing) {
        Object.entries(nextMap).forEach(([id, r]) => {
          if (!r || !r.matched || !r.element) {
            return;
          }
          const comment = this.comments.find(c => String(c.commentId) === String(id));
          const anchorKind = (r.anchorKind || (comment && comment.anchorKind) || 'line');
          nextMap[id] = { ...r, highlightEl: r.element, anchorKind };
        });
        this.rematchMap = nextMap;
        this.applyingHighlights = false;
        if (this.activeThreadId != null) {
          this.$nextTick(() => this.syncEditModeHighlightOverlay());
        } else {
          this.clearEditModeHighlightOverlay();
        }
        return;
      }
      Object.entries(nextMap).forEach(([id, r]) => {
        if (!r || !r.matched || !r.element) {
          return;
        }
        const comment = this.comments.find(c => String(c.commentId) === String(id));
        const anchorKind = (r.anchorKind || (comment && comment.anchorKind) || 'line');
        const active = this.activeThreadId != null && String(this.activeThreadId) === String(id);
        if (anchorKind === 'selection' && comment) {
          const mark = highlightQuoteInElement(r.element, comment, { active });
          if (mark) {
            nextMap[id] = { ...r, highlightEl: mark, anchorKind };
            return;
          }
        }
        r.element.classList.add('report-annotation-highlight');
        if (active) {
          r.element.classList.add('is-active');
        }
        nextMap[id] = { ...r, highlightEl: r.element, anchorKind };
      });
      this.rematchMap = nextMap;
      this.applyingHighlights = false;
      this.clearEditModeHighlightOverlay();
    },
    /** 从右侧线程点击：高亮正文对应片段，并滚到与该评论相同的屏幕高度 */
    handleFocusThread(threadId) {
      this.activeThreadId = threadId;
      this.sideTab = 'comments';
      if (this.commentsCollapsed) {
        this.commentsCollapsed = false;
      }
      if (this.isEditing) {
        // 编辑态只用遮罩；refreshAnchors 里的 class 会被 ProseMirror 立刻冲掉
        this.$nextTick(() => {
          this.syncEditModeHighlightOverlay();
          const container = this.getAnnotationContentRoot();
          const comment = this.comments.find(c => String(c.commentId) === String(threadId));
          if (!container || !comment) {
            this.syncAnnotationConnector();
            return;
          }
          const rematch = rematchAnnotation(container, comment);
          if (!rematch.matched || !rematch.element || !rematch.element.isConnected) {
            this.syncAnnotationConnector();
            return;
          }
          const scrollRoot = this.getMainScrollElement();
          const peerEl = this.findThreadPeerElement(threadId);
          scrollTargetToPeerHeight(scrollRoot, rematch.element, peerEl);
          this.$nextTick(() => {
            this.syncEditModeHighlightOverlay();
            this.syncAnnotationConnector();
          });
        });
        return;
      }
      this.refreshAnchors();
      const result = this.findRematchResult(threadId);
      if (!result || !result.matched) {
        this.syncAnnotationConnector();
        return;
      }
      const targetEl = result.highlightEl || result.element;
      if (!targetEl) {
        this.syncAnnotationConnector();
        return;
      }
      this.$nextTick(() => {
        const scrollRoot = this.getMainScrollElement();
        const peerEl = this.findThreadPeerElement(threadId);
        scrollTargetToPeerHeight(scrollRoot, targetEl, peerEl);
        targetEl.classList.remove('is-flash');
        void targetEl.offsetWidth;
        targetEl.classList.add('is-flash');
        this.syncAnnotationConnector();
      });
    },
    clearActiveThread() {
      if (this.activeThreadId == null && !this.bodyHighlightRects.length && !this.annotationConnector) {
        return;
      }
      this.activeThreadId = null;
      this.clearEditModeHighlightOverlay();
      this.annotationConnector = null;
      if (!this.isEditing) {
        this.refreshAnchors();
      }
    },
    clearEditModeHighlightOverlay() {
      this.bodyHighlightRects = [];
    },
    /**
     * 编辑态：用绝对定位遮罩高亮选区/块，避免给 ProseMirror DOM 加 class（会被文档重绘清掉）。
     */
    syncEditModeHighlightOverlay() {
      if (!this.isEditing || this.activeThreadId == null) {
        this.clearEditModeHighlightOverlay();
        this.annotationConnector = null;
        return;
      }
      const scrollRoot = this.getMainScrollElement();
      const container = this.getAnnotationContentRoot();
      if (!scrollRoot || !container) {
        this.clearEditModeHighlightOverlay();
        this.annotationConnector = null;
        return;
      }
      const comment = this.comments.find(c => String(c.commentId) === String(this.activeThreadId));
      if (!comment || comment.parentId) {
        this.clearEditModeHighlightOverlay();
        this.annotationConnector = null;
        return;
      }
      const rematch = rematchAnnotation(container, comment);
      if (!rematch.matched || !rematch.element || !rematch.element.isConnected) {
        this.clearEditModeHighlightOverlay();
        this.annotationConnector = null;
        return;
      }

      let clientRects = [];
      const anchorKind = rematch.anchorKind || comment.anchorKind || 'line';
      if (anchorKind === 'selection' && comment.quote) {
        const index = buildPlainTextIndex(rematch.element);
        const hit = rematchAnchor(comment, index);
        if (hit.status === 'matched' && hit.start != null && hit.end != null) {
          const range = rangeFromPlainOffsets(index, hit.start, hit.end);
          if (range && !range.collapsed) {
            clientRects = Array.from(range.getClientRects());
          }
        }
      }
      if (!clientRects.length) {
        clientRects = Array.from(rematch.element.getClientRects());
      }

      // 视口坐标 + fixed 遮罩，避免嵌套 overflow / transform 导致定位失效
      this.bodyHighlightRects = clientRects
        .filter(r => r.width > 0 && r.height > 0)
        .map(r => ({
          top: r.top,
          left: r.left,
          width: r.width,
          height: r.height
        }));

      // 同步 rematchMap，便于后续 focus / 侧栏 orphan 状态
      this.rematchMap = {
        ...this.rematchMap,
        [String(comment.commentId)]: {
          ...rematch,
          highlightEl: rematch.element,
          anchorKind
        }
      };
      this.syncAnnotationConnector();
    },
    /**
     * 正文高亮区域与右侧评论卡片之间画连线（视口坐标）。
     */
    syncAnnotationConnector() {
      if (this.activeThreadId == null) {
        this.annotationConnector = null;
        return;
      }
      const peer = this.findThreadPeerElement(this.activeThreadId);
      if (!peer || !peer.isConnected) {
        this.annotationConnector = null;
        return;
      }
      const peerRect = peer.getBoundingClientRect();
      if (peerRect.width <= 0 || peerRect.height <= 0) {
        this.annotationConnector = null;
        return;
      }

      let from = null;
      if (this.bodyHighlightRects && this.bodyHighlightRects.length) {
        // 取最靠右的高亮条，连线从右侧中点出发
        from = this.bodyHighlightRects.reduce((best, r) => {
          const right = r.left + r.width;
          if (!best || right > best.right) {
            return { left: r.left, top: r.top, width: r.width, height: r.height, right };
          }
          return best;
        }, null);
      } else {
        const result = this.findRematchResult(this.activeThreadId);
        const el = result && (result.highlightEl || result.element);
        if (el && el.isConnected) {
          const b = el.getBoundingClientRect();
          if (b.width > 0 && b.height > 0) {
            from = { left: b.left, top: b.top, width: b.width, height: b.height, right: b.right };
          }
        }
      }
      if (!from) {
        this.annotationConnector = null;
        return;
      }

      const x1 = from.right;
      const y1 = from.top + from.height / 2;
      const x2 = peerRect.left;
      const y2 = peerRect.top + Math.min(20, peerRect.height / 2);
      const dx = Math.max(40, Math.abs(x2 - x1) * 0.45);
      this.annotationConnector = {
        d: `M ${x1.toFixed(1)} ${y1.toFixed(1)} C ${(x1 + dx).toFixed(1)} ${y1.toFixed(1)}, ${(x2 - dx).toFixed(1)} ${y2.toFixed(1)}, ${x2.toFixed(1)} ${y2.toFixed(1)}`
      };
    },
    onAnnotationOverlayScroll() {
      if (this.activeThreadId == null) {
        return;
      }
      if (this._annotationOverlayRaf) {
        return;
      }
      this._annotationOverlayRaf = requestAnimationFrame(() => {
        this._annotationOverlayRaf = 0;
        if (this.isEditing) {
          this.syncEditModeHighlightOverlay();
        } else {
          this.syncAnnotationConnector();
        }
      });
    },
    findRematchResult(threadId) {
      if (threadId == null) {
        return null;
      }
      const direct = this.rematchMap[threadId];
      if (direct) {
        return direct;
      }
      return this.rematchMap[String(threadId)] || null;
    },
    findThreadPeerElement(threadId) {
      if (typeof document === 'undefined' || threadId == null) {
        return null;
      }
      return document.querySelector(`[data-report-thread-id="${threadId}"]`);
    },
    /** 从正文高亮点击：反向定位到右侧线程 */
    handleBodyClick(e) {
      const target = e.target && e.target.closest
        ? e.target.closest('.report-annotation-highlight, mark.report-annotation-highlight-mark')
        : null;
      if (!target) {
        return;
      }
      const entry = Object.entries(this.rematchMap).find(([, r]) => {
        return r && (r.highlightEl === target || r.element === target || (r.highlightEl && r.highlightEl.contains && r.highlightEl.contains(target)));
      });
      if (entry) {
        this.handleFocusThread(entry[0]);
      }
    },

    // ===================== 选区浮条（选中文字批注） =====================
    handleShellMouseDown(e) {
      // 任意新的鼠标操作先隐藏行批注按钮，避免与选区浮条打架
      this.lineButton.visible = false;
      // 点击非评论块区域 → 取消评论焦点与连线/高亮
      if (this.activeThreadId == null) {
        return;
      }
      const target = e && e.target;
      if (!target || typeof target.closest !== 'function') {
        return;
      }
      if (target.closest('.report-thread')) {
        return;
      }
      if (target.closest('.report-detail-comments-draft')) {
        return;
      }
      // 回复输入框挂在线程内，已由 .report-thread 覆盖
      this.clearActiveThread();
    },
    handleBodyMouseUp() {
      if (this.isEditing) {
        return;
      }
      this.$nextTick(() => this.updateSelectionToolbar());
    },
    updateSelectionToolbar() {
      if (this.isEditing) {
        this.selectionToolbar.visible = false;
        this.pendingSelectionRange = null;
        return;
      }
      const container = this.$refs['report-data'];
      const selection = typeof window !== 'undefined' ? window.getSelection() : null;
      if (!container || !selection || selection.rangeCount === 0 || selection.isCollapsed) {
        this.selectionToolbar.visible = false;
        this.pendingSelectionRange = null;
        return;
      }
      const range = selection.getRangeAt(0);
      if (!container.contains(range.commonAncestorContainer)) {
        this.selectionToolbar.visible = false;
        this.pendingSelectionRange = null;
        return;
      }
      const text = selection.toString().trim();
      if (!text) {
        this.selectionToolbar.visible = false;
        this.pendingSelectionRange = null;
        return;
      }
      const rect = range.getBoundingClientRect();
      this.selectionToolbar = {
        visible: true,
        top: rect.top,
        left: rect.left + rect.width / 2
      };
      this.pendingSelectionRange = range.cloneRange();
    },
    handleAddSelectionComment() {
      const container = this.$refs['report-data'];
      const range = this.pendingSelectionRange;
      if (!container || !range) {
        return;
      }
      const quote = range.toString().trim().slice(0, 400);
      if (!quote) {
        return;
      }
      const context = extractSelectionContext(container, range, 60);
      let anchorRect = null;
      try {
        const rect = range.getBoundingClientRect();
        anchorRect = { top: rect.top, bottom: rect.bottom, left: rect.left, right: rect.right };
      } catch (e) {
        anchorRect = null;
      }
      this.openDraft({
        anchorKind: 'selection',
        quote,
        contextBefore: context.before,
        contextAfter: context.after,
        anchorHint: this.nearestHeadingHint(range.startContainer),
        anchorRect
      });
      this.selectionToolbar.visible = false;
      this.pendingSelectionRange = null;
      if (typeof window !== 'undefined' && window.getSelection) {
        window.getSelection().removeAllRanges();
      }
    },
    /** 编辑态：Crepe 选区工具条「批注」 */
    handleEditorSelectionComment(payload) {
      const quote = payload && payload.quote ? String(payload.quote).trim().slice(0, 400) : '';
      if (!quote) {
        return;
      }
      this.openDraft({
        anchorKind: 'selection',
        quote,
        contextBefore: (payload && payload.contextBefore) || '',
        contextAfter: (payload && payload.contextAfter) || '',
        anchorHint: null,
        anchorRect: (payload && payload.anchorRect) || null
      });
    },

    // ===================== 整行 / 块级批注入口 =====================
    handleBodyMouseDown() {
      this.lineButton.visible = false;
    },
    handleBodyMouseMove(e) {
      if (this.isEditing) {
        return;
      }
      const container = this.$refs['report-data'];
      if (!container || this.selectionToolbar.visible) {
        return;
      }
      const block = e.target && e.target.closest ? e.target.closest(ANNOTATABLE_SELECTOR) : null;
      if (!block || !container.contains(block)) {
        if (this.lineButton.visible) {
          this.lineButton.visible = false;
          this.hoverLineBlock = null;
        }
        return;
      }
      if (block === this.hoverLineBlock) {
        return;
      }
      this.hoverLineBlock = block;
      const rect = block.getBoundingClientRect();
      this.lineButton = { visible: true, top: Math.max(rect.top, 0) + 2, left: Math.max(rect.left - 26, 0) };
    },
    handleBodyMouseLeave() {
      this.lineButton.visible = false;
      this.hoverLineBlock = null;
    },
    handleAddLineComment() {
      const block = this.hoverLineBlock;
      if (!block) {
        return;
      }
      const quote = getBlockText(block).slice(0, 400);
      if (!quote) {
        return;
      }
      let anchorRect = null;
      try {
        const rect = block.getBoundingClientRect();
        anchorRect = { top: rect.top, bottom: rect.bottom, left: rect.left, right: rect.right };
      } catch (e) {
        anchorRect = null;
      }
      this.openDraft({
        anchorKind: 'line',
        quote,
        contextBefore: '',
        contextAfter: '',
        anchorHint: this.nearestHeadingHint(block),
        anchorRect
      });
      this.lineButton.visible = false;
    },
    /** 就近向上查找所属标题，作为辅助定位提示（anchor_hint，非强匹配依据） */
    nearestHeadingHint(node) {
      const container = this.$refs['report-data'];
      let el = node && node.nodeType === 1 ? node : (node && node.parentElement);
      while (el && el !== container) {
        if (/^H[1-6]$/.test(el.tagName || '')) {
          return el.getAttribute('data-report-toc-id') || null;
        }
        el = el.previousElementSibling || el.parentElement;
      }
      return null;
    },

    // ===================== 批注草稿 / 提交 / 回复 / 删除 =====================
    openDraft(anchor) {
      this.draftAnnotation = anchor;
      this.sideTab = 'comments';
      if (this.commentsCollapsed) {
        this.commentsCollapsed = false;
      }
      this.$nextTick(() => {
        this.updateDraftTop();
        this.scheduleCommentLayout();
      });
    },
    cancelDraft() {
      this.draftAnnotation = null;
      this.draftTop = null;
    },
    /** 加载报告的全部批注（moduleType=report，correlationId=reportId） */
    loadComments() {
      if (!this.report.reportId) {
        return;
      }
      this.commentsLoading = true;
      listComment({
        moduleType: 'report',
        correlationId: this.report.reportId,
        pageNum: 1,
        pageSize: 1000
      }).then(res => {
        this.comments = res.rows || [];
        this.$nextTick(() => this.refreshAnchors());
      }).finally(() => {
        this.commentsLoading = false;
      });
    },
    submitDraft(content) {
      if (!this.draftAnnotation || !content) {
        return;
      }
      addComment({
        commentContent: content,
        moduleType: 'report',
        correlationId: this.report.reportId,
        anchorKind: this.draftAnnotation.anchorKind,
        quote: this.draftAnnotation.quote,
        contextBefore: this.draftAnnotation.contextBefore,
        contextAfter: this.draftAnnotation.contextAfter,
        anchorHint: this.draftAnnotation.anchorHint
      }).then(() => {
        this.$message.success(this.$i18n.t('comment.submit-success').toString());
        this.draftAnnotation = null;
        this.loadComments();
      });
    },
    submitReply({ threadId, content }) {
      if (!content) {
        return;
      }
      addComment({
        commentContent: content,
        moduleType: 'report',
        correlationId: this.report.reportId,
        parentId: threadId
      }).then(() => {
        this.$message.success(this.$i18n.t('comment.submit-success').toString());
        this.loadComments();
      });
    },
    handleDeleteComment(comment) {
      const isRoot = !comment.parentId;
      const confirmText = isRoot
        ? this.$i18n.t('report.delete-annotation-confirm').toString()
        : this.$i18n.t('report.delete-reply-confirm').toString();
      this.$modal.confirm(confirmText).then(() => {
        delComment(comment.commentId).then(() => {
          this.$modal.msgSuccess(this.$i18n.t('delete.success').toString());
          if (String(this.activeThreadId) === String(comment.commentId) || String(comment.parentId) === String(this.activeThreadId)) {
            this.clearActiveThread();
          }
          this.loadComments();
        });
      }).catch(() => {});
    },

    // ===================== 正文编辑与自动保存 =====================
    onToolbarInsert(content) {
      if (!content || !this.isEditing) {
        return;
      }
      const editor = this.$refs.reportEditor;
      if (editor && typeof editor.insertMarkdown === 'function') {
        editor.insertMarkdown(content);
        return;
      }
      const prefix = this.editDraft && !this.editDraft.endsWith('\n') ? '\n' : '';
      this.editDraft = `${this.editDraft || ''}${prefix}${content}\n`;
    },
    onToolbarInsertTool(tool) {
      if (!tool || !this.isEditing) {
        return;
      }
      const content = resolveToolContent(tool);
      if (content) {
        this.onToolbarInsert(content);
      }
    },
    onToolbarClear() {
      if (!this.isEditing) {
        return;
      }
      this.$modal.confirm('确定清空报告正文吗？').then(() => {
        this.editDraft = '';
      }).catch(() => {});
    },
    onEditDraftChange(val) {
      const next = normalizeDslSource(val == null ? '' : val);
      this.editDraft = next;
      // 草稿变更时同步左侧目录（MutationObserver 的兜底，覆盖部分非 DOM 感知路径）
      this.scheduleRefreshDetail();
    },
    setupAutosave() {
      if (this.autosaveTimer || !this.canEdit) {
        return;
      }
      this.autosaveTimer = setInterval(() => {
        // 内容未变（!isDirty）则跳过，后端也不会写历史
        if (this.isDirty && this.visible && !this.saving) {
          this.saveReportContent({ silent: true });
        }
      }, REPORT_AUTOSAVE_INTERVAL_MS);
    },
    teardownAutosave() {
      if (this.autosaveTimer) {
        clearInterval(this.autosaveTimer);
        this.autosaveTimer = null;
      }
    },
    bindLeaveGuards() {
      this.unbindLeaveGuards();
      this._onBeforeUnload = (e) => {
        if (!this.isDirty) {
          return;
        }
        this.flushSaveOnLeave(true);
        e.preventDefault();
        e.returnValue = '';
      };
      this._onPageHide = () => {
        if (this.isDirty) {
          this.flushSaveOnLeave(true);
        }
      };
      if (typeof window !== 'undefined') {
        window.addEventListener('beforeunload', this._onBeforeUnload);
        window.addEventListener('pagehide', this._onPageHide);
      }
    },
    unbindLeaveGuards() {
      if (typeof window !== 'undefined') {
        if (this._onBeforeUnload) {
          window.removeEventListener('beforeunload', this._onBeforeUnload);
        }
        if (this._onPageHide) {
          window.removeEventListener('pagehide', this._onPageHide);
        }
      }
      this._onBeforeUnload = null;
      this._onPageHide = null;
    },
    /** 保存正文；silent 时不弹成功提示（定时自动保存） */
    saveReportContent(options = {}) {
      const silent = !!options.silent;
      // 无实质变更：不请求、不写历史
      if (!this.canEdit || !this.report.reportId || !this.isDirty || this.saving) {
        return Promise.resolve(false);
      }
      const nextDescription = normalizeDslSource(this.editDraft == null ? '' : this.editDraft);
      if (!isReportContentDirty(nextDescription, this.savedDescription)) {
        this.editDraft = nextDescription;
        this.savedDescription = nextDescription;
        return Promise.resolve(false);
      }
      const payload = {
        reportId: this.report.reportId,
        reportDescription: nextDescription,
        projectId: this.report.projectId,
        reportTitle: this.report.reportTitle
      };
      this.saving = true;
      return updateReport(payload).then(() => {
        this.editDraft = nextDescription;
        this.savedDescription = nextDescription;
        this.report = { ...this.report, reportDescription: nextDescription };
        this.lastSavedAt = Date.now();
        if (!silent) {
          this.$message.success(this.$t('report.save-success').toString());
        }
        this.loadHistory();
        return true;
      }).catch(() => false).finally(() => {
        this.saving = false;
      });
    },
    /**
     * 离开/刷新前尽量落盘。keepalive 用于页面卸载场景。
     * @param {boolean} useKeepalive
     */
    flushSaveOnLeave(useKeepalive) {
      if (!this.canEdit || !this.report.reportId || !this.isDirty) {
        return Promise.resolve(false);
      }
      if (useKeepalive && typeof fetch === 'function') {
        const body = {
          reportId: this.report.reportId,
          reportDescription: this.editDraft,
          projectId: this.report.projectId,
          reportTitle: this.report.reportTitle,
          srcHost: `${window.location.protocol}//${window.location.host}`
        };
        const headers = { 'Content-Type': 'application/json;charset=utf-8' };
        try {
          setHeader('/system/report', headers);
        } catch (e) {
          if (getToken()) {
            headers.Authorization = 'Bearer ' + getToken();
          }
        }
        const base = process.env.VUE_APP_BASE_API || '';
        try {
          fetch(base + '/system/report', {
            method: 'PUT',
            headers,
            body: JSON.stringify(body),
            keepalive: true,
            credentials: 'include'
          });
          this.savedDescription = this.editDraft;
          return Promise.resolve(true);
        } catch (e) {
          return this.saveReportContent({ silent: true });
        }
      }
      return this.saveReportContent({ silent: true });
    },

    // ===================== 编辑历史 =====================
    loadHistory() {
      if (!this.report.reportId) {
        return;
      }
      this.historyLoading = true;
      listReportHistory(this.report.reportId).then(res => {
        this.historyItems = res.data || [];
      }).finally(() => {
        this.historyLoading = false;
      });
    },
    handleRestoreHistory(item) {
      if (!item || !item.historyId || !this.canEdit) {
        return;
      }
      const doRestore = () => {
        restoreReportHistory(item.historyId).then(() => {
          return getReport(this.report.reportId);
        }).then(res => {
          const next = res.data || {};
          const description = normalizeDslSource(next.reportDescription == null ? '' : String(next.reportDescription));
          this.report = {
            ...this.report,
            ...next,
            reportDescription: description,
            focusList: this.report.focusList
          };
          this.editDraft = description;
          this.savedDescription = description;
          this.isEditing = true;
          this.lastSavedAt = Date.now();
          this.$message.success(this.$t('report.history-restore-success').toString());
          this.loadHistory();
          this.$nextTick(() => this.refreshDetail());
        }).catch(() => {});
      };
      const confirmText = this.isDirty
        ? this.$t('report.history-restore-dirty-confirm').toString()
        : this.$t('report.history-restore-confirm').toString();
      this.$modal.confirm(confirmText).then(doRestore).catch(() => {});
    },
    handleDeleteHistory(item) {
      if (!item || !item.historyId || !this.canEdit) {
        return;
      }
      this.$modal.confirm(this.$t('report.history-delete-confirm').toString()).then(() => {
        return delReportHistory(item.historyId);
      }).then(() => {
        this.$modal.msgSuccess(this.$t('delete.success').toString());
        this.loadHistory();
      }).catch(() => {});
    },
  }
}
</script>
<style lang="scss" scoped>
@import "~@/assets/styles/multipane-resizer-grip.scss";

  .report-edit-header {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-direction: row;
    flex-wrap: wrap;
    .report-edit-tools {
      padding: 0;
      overflow: visible;
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }
    .report-save-status {
      font-size: 12px;
      color: var(--el-text-color-secondary, #909399);
      white-space: nowrap;
    }
    .report-edit-title {
      display: inline-flex;
      justify-content: flex-start;
      align-items: center;
      flex-direction: row;
      overflow: hidden;
      flex: 1;
      min-width: 0;
      > * {
        float:left;
      }
      .report-view-back {
        font-size: 22px;
        color: var(--el-text-color-primary, #303133);
        flex-shrink: 0;
      }
      .report-view-back:hover {
        cursor: pointer;
        color: var(--el-text-color-secondary, #909399);
      }
      .report-edit-title-wrap {
        display: inline-flex;
        align-items: center;
        min-width: 0;
        flex: 1;
        gap: 4px;
      }
      .report-edit-title-name {
        flex: 1;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
      }
      .report-edit-title-num, .report-edit-title-name {
        font-size: 20px;
        color: var(--el-text-color-primary, #303133);
        font-weight: 500;
        margin-top: 10px;
        margin-bottom: 10px;
      }
      .report-edit-title-num {
        min-width: 0;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
      }
      .report-edit-title-btn {
        flex-shrink: 0;
        color: var(--el-text-color-secondary, #909399) !important;
        padding: 4px !important;
        height: auto !important;
        min-height: 0 !important;
      }
      .report-edit-title-btn:hover {
        color: var(--el-color-primary, #409eff) !important;
      }
      .report-edit-title-input {
        flex: 1;
        min-width: 160px;
        max-width: 480px;
      }
      .report-edit-title-input :deep(.el-input__wrapper) {
        background-color: var(--el-fill-color-blank, #ffffff);
        box-shadow: 0 0 0 1px var(--el-border-color, #dcdfe6) inset;
      }
      .report-edit-title-input :deep(.el-input__inner) {
        color: var(--el-text-color-primary, #303133);
      }
      > * {
        margin-right: 10px;
      }
    }
  }
  ::v-deep .md {
    margin: 0 24px;
    span {
      display: inline-block;
      word-wrap: break-word;
      white-space: normal;
    }
    img {
      max-width: 100% !important;
      height: auto !important;
      object-fit: contain;
      vertical-align: middle;
      display: inline-block !important;
      margin-left: 0 !important;
      margin-right: 0 !important;
    }
    table {
      display: table !important;
      width: 100% !important;
      max-width: 100%;
      table-layout: fixed;
      border-collapse: collapse;
      overflow: visible;
    }
    table th,
    table td {
      border: 1px solid var(--el-border-color, #dcdfe6);
      padding: 8px 12px;
      word-break: break-word;
      overflow-wrap: anywhere;
      white-space: normal;
      min-width: 0;
    }
    table th {
      background-color: var(--el-fill-color-light, #f5f7fa);
      font-weight: 600;
    }
  }
  ::v-deep .el-dropdown-menu__item {
    > * {
      margin-right: 5px;
    }
  }
  .report-detail-shell {
    display: flex;
    flex-direction: column;
    flex: 1;
    align-items: stretch;
    min-height: 0;
    overflow: hidden;
    position: relative;
    background-color: var(--el-fill-color-light, #f5f7fa);
  }
  .report-detail-body {
    display: flex;
    flex: 1;
    align-items: stretch;
    min-height: 0;
    overflow: hidden;

    &.is-pane-resizing {
      cursor: col-resize;
      user-select: none;
    }
  }
  .report-pane-resizer {
    position: relative;
    z-index: 12;
    flex: 0 0 10px;
    width: 10px;
    margin: 0 -5px;
    cursor: col-resize;
    @include multipane-resizer-vertical-appearance;
  }
  .report-detail-body :deep(.report-detail-toc--resizable) {
    border-right: none;
  }
  .report-detail-body :deep(.report-detail-side--resizable) {
    border-left: none;
  }
  .report-status-bar {
    flex-shrink: 0;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px 20px;
    min-height: 32px;
    padding: 6px 14px;
    box-sizing: border-box;
    border-top: 1px solid var(--el-border-color-lighter, #ebeef5);
    background-color: var(--el-fill-color-blank, #fafafa);
    color: var(--el-text-color-regular, #606266);
    font-size: 12px;
    line-height: 1.4;
    border-radius: 0;
  }
  .report-status-bar__item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
    max-width: 100%;
  }
  .report-status-bar__item--chars {
    margin-left: auto;
  }
  .report-status-bar__icon {
    flex-shrink: 0;
    font-size: 14px;
    color: var(--el-text-color-secondary, #909399);
  }
  .report-status-bar__label {
    flex-shrink: 0;
    color: var(--el-text-color-secondary, #909399);
  }
  .report-status-bar__label::after {
    content: ':';
  }
  .report-status-bar__value {
    min-width: 0;
    color: var(--el-text-color-primary, #303133);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .report-detail-main {
    flex: 1;
    min-width: 0;
    min-height: 0;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    position: relative;
    background-color: var(--el-bg-color, #ffffff);
    box-shadow: 0 0 0 1px var(--el-border-color-lighter, #ebeef5);
  }
  .report-detail-main :deep(.report-content-toolbar) {
    flex-shrink: 0;
  }
  .report-detail-main #report-data.app-container {
    position: relative;
    flex: 1;
    min-height: 0;
    overflow-x: hidden;
    overflow-y: auto;
    padding-left: 40px;
    padding-right: 40px;
    box-sizing: border-box;

    &.is-editing {
      display: flex;
      flex-direction: column;
      padding-top: 0;
      padding-bottom: 0;
      padding-left: 0;
      padding-right: 0;
    }
  }
  .report-annotation-overlay {
    position: fixed;
    inset: 0;
    z-index: 3100;
    pointer-events: none;
    overflow: visible;
  }
  .report-annotation-overlay__rect {
    position: absolute;
    border-radius: 2px;
    background-color: rgba(245, 108, 108, 0.28);
    box-shadow: inset 0 -2px 0 0 #f56c6c;
  }
  .report-annotation-overlay__connector {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    overflow: visible;
  }
  .report-annotation-overlay__connector-path {
    stroke: #f56c6c;
    stroke-width: 1.5;
    stroke-linecap: round;
    opacity: 0.85;
  }
  .report-edit-pane {
    flex: 1;
    min-height: 0;
    width: 100%;
    display: flex;
    flex-direction: column;
  }
  .report-body-visual-editor.report-template-editor {
    flex: 1;
    min-height: 0;
    height: auto;
    display: flex;
    flex-direction: column;
    box-shadow: none;
    border-radius: 0;
    overflow: hidden;
  }
  .report-body-visual-editor :deep(.report-milkdown-host),
  .report-body-visual-editor :deep(.report-milkdown-editor) {
    flex: 1 1 auto;
    min-height: 0;
    height: auto;
    overflow: auto;
  }
  .report-body-visual-editor :deep(.report-milkdown-editor.milkdown) {
    overflow: auto;
  }
  .report-pane-edge-bar {
    position: sticky;
    top: 0;
    z-index: 20;
    height: 0;
    pointer-events: none;
  }
  .report-detail-main:has(.report-content-toolbar) .report-pane-edge-bar {
    top: 44px;
  }
  .report-pane-edge-btn {
    pointer-events: auto;
    position: absolute;
    top: 8px;
    width: 28px;
    height: 28px;
    margin: 0 !important;
    padding: 0 !important;
    background-color: var(--el-bg-color, #ffffff);
    color: var(--el-color-primary, #409eff);
    border: 1px solid var(--el-border-color, #dcdfe6);
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);

    &:hover {
      color: var(--el-color-primary, #409eff);
      background-color: var(--el-fill-color-light, #f5f7fa);
      border-color: var(--el-color-primary-light-5, #a0cfff);
    }
  }
  /* 贴边一侧直角，内侧圆角 */
  .report-pane-edge-btn--left {
    left: 0 !important;
    border-left: none !important;
    border-radius: 0 4px 4px 0 !important;
  }
  .report-pane-edge-btn--right {
    right: 0 !important;
    border-right: none !important;
    border-radius: 4px 0 0 4px !important;
  }
  .report-line-comment-button {
    position: fixed;
    z-index: 2500;
    width: 22px;
    height: 22px;
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: var(--el-color-primary, #409eff);
    color: #ffffff;
    font-size: 13px;
    cursor: pointer;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);

    &:hover {
      opacity: 0.85;
    }
  }
  ::v-deep .report-annotation-highlight,
  ::v-deep mark.report-annotation-highlight-mark {
    background-color: transparent;
    color: inherit;
    padding: 0;
    border-radius: 2px;
    box-decoration-break: clone;
    -webkit-box-decoration-break: clone;
    text-decoration-line: underline;
    text-decoration-style: wavy;
    text-decoration-color: #f56c6c;
    text-decoration-thickness: 1px;
    text-underline-offset: 2px;
    transition: background-color 0.2s ease, text-decoration-color 0.2s ease;
  }
  ::v-deep mark.report-annotation-highlight-mark {
    padding: 0 1px;
  }
  ::v-deep .report-annotation-highlight.is-active,
  ::v-deep mark.report-annotation-highlight-mark.is-active {
    background-color: rgba(245, 108, 108, 0.22);
    text-decoration-color: #f56c6c;
    outline: none;
    color: inherit;
  }
  ::v-deep .report-annotation-highlight.is-flash,
  ::v-deep mark.report-annotation-highlight-mark.is-flash {
    animation: report-annotation-flash 0.9s ease;
  }
  @keyframes report-annotation-flash {
    0% { background-color: rgba(245, 108, 108, 0.4); }
    100% { background-color: rgba(245, 108, 108, 0.22); }
  }
</style>

<style lang="scss">
/* 报告抽屉 header / body：teleport 到 body，须非 scoped */
.el-drawer.report-drawer-accent {
  --el-drawer-padding-primary: 0;
}

.el-drawer.report-drawer-accent > .el-drawer__header {
  margin-bottom: 0 !important;
  max-width: calc(100vw);
  background-color: var(--el-bg-color, #ffffff) !important;
  color: var(--el-text-color-primary, #303133) !important;
  border-bottom: 1px solid var(--el-border-color, #dcdfe6);
  padding: var(--cat2bug-drawer-header-padding-y) var(--cat2bug-drawer-header-padding-x) !important;
}

.el-drawer.report-drawer-accent > .el-drawer__body {
  padding: 0 !important;
  margin: 0 !important;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  flex: 1;
}

.el-drawer.report-drawer-accent .report-detail-shell {
  flex: 1;
  width: 100%;
  min-height: 0;
  margin: 0;
  padding: 0;
}

/* 贴边展开钮：贴边一侧直角（覆盖 EP button 默认圆角） */
.el-drawer.report-drawer-accent .report-pane-edge-btn.report-pane-edge-btn--left {
  left: 0 !important;
  border-left: none !important;
  border-radius: 0 4px 4px 0 !important;
}
.el-drawer.report-drawer-accent .report-pane-edge-btn.report-pane-edge-btn--right {
  right: 0 !important;
  border-right: none !important;
  border-radius: 4px 0 0 4px !important;
}

html.dark .el-drawer.report-drawer-accent > .el-drawer__header {
  /* 与正文底板同色，靠底部分割线区分 */
  background-color: #1e1e20 !important;
  color: #e5eaf3 !important;
  border-bottom: 1px solid #3a3a3e !important;
}

html.dark .el-drawer.report-drawer-accent .report-edit-header {
  .report-save-status {
    color: rgba(229, 234, 243, 0.72);
  }
  .report-edit-title {
    .report-view-back {
      color: #e5eaf3;
    }
    .report-view-back:hover {
      color: rgba(229, 234, 243, 0.75);
    }
    .report-edit-title-num,
    .report-edit-title-name {
      color: #e5eaf3;
    }
    .report-edit-title-btn {
      color: rgba(229, 234, 243, 0.72) !important;
    }
    .report-edit-title-btn:hover {
      color: #ffffff !important;
    }
    .report-edit-title-input .el-input__wrapper {
      background-color: rgba(255, 255, 255, 0.08);
      box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.22) inset;
    }
    .report-edit-title-input .el-input__inner {
      color: #e5eaf3;
    }
  }
}

/* 报告详情三栏暗色：侧栏与正文底板统一 */
html.dark .report-detail-shell {
  background-color: #1e1e20;
}
html.dark .report-pane-resizer {
  &:before {
    background-color: #3a3a3e;
  }
  &:after {
    border-color: #4a4a4e;
    background: linear-gradient(180deg, #2b2b2c 0%, #252529 48%, #252529 52%, #2b2b2c 100%);
    box-shadow:
      0 0 0 1px rgba(0, 0, 0, 0.5),
      0 1px 4px rgba(0, 0, 0, 0.4);
  }
  &:hover:before {
    background-color: #f4bd6f;
  }
  &:hover:after {
    border-color: #f4bd6f;
    background: linear-gradient(180deg, #333336 0%, #2a2a2c 50%, #333336 100%);
    box-shadow:
      0 0 0 1px rgba(0, 0, 0, 0.55),
      0 2px 8px rgba(244, 189, 111, 0.25);
  }
}
html.dark .report-status-bar {
  background-color: #252529;
  border-top-color: #3a3a3e;
  color: #a3a6ad;
}
html.dark .report-status-bar__label {
  color: #6b6e76;
}
html.dark .report-status-bar__icon {
  color: #6b6e76;
}
html.dark .report-status-bar__value {
  color: #e5eaf3;
}
html.dark .report-detail-main {
  background-color: #1e1e20;
  color: #e5eaf3;
  box-shadow: none;
}
html.dark .report-detail-main .report-pane-edge-btn {
  background-color: rgba(37, 37, 41, 0.92);
  color: var(--crepe-color-primary, #f4bd6f);
  border: 1px solid #3a3a3e;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.35);

  &:hover {
    color: #ffffff;
    background-color: #333336;
    border-color: #4a4a4e;
  }
}
html.dark .report-detail-main .report-pane-edge-btn--left {
  border-left: none !important;
  border-radius: 0 4px 4px 0 !important;
}
html.dark .report-detail-main .report-pane-edge-btn--right {
  border-right: none !important;
  border-radius: 4px 0 0 4px !important;
}
html.dark .report-detail-main .markdown-body,
html.dark .report-detail-main .md {
  color: #e5eaf3;
}
html.dark .report-detail-main .markdown-body h1,
html.dark .report-detail-main .markdown-body h2,
html.dark .report-detail-main .markdown-body h3,
html.dark .report-detail-main .markdown-body h4,
html.dark .report-detail-main .markdown-body h5,
html.dark .report-detail-main .markdown-body h6 {
  color: #e5eaf3;
  border-bottom-color: #3a3a3e;
}
html.dark .report-detail-main .markdown-body table,
html.dark .report-detail-main .md table {
  display: table !important;
  width: 100% !important;
  max-width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  background: #1e1e20;
}
html.dark .report-detail-main .markdown-body table th,
html.dark .report-detail-main .markdown-body table td,
html.dark .report-detail-main .md table th,
html.dark .report-detail-main .md table td {
  border-color: #3a3a3e;
  word-break: break-word;
  overflow-wrap: anywhere;
  min-width: 0;
}
html.dark .report-detail-main .markdown-body table th,
html.dark .report-detail-main .md table th {
  background-color: #252529;
}
html.dark .report-detail-main .markdown-body table tr,
html.dark .report-detail-main .md table tr {
  background-color: #1e1e20;
}
html.dark .report-detail-main .markdown-body table tr:nth-child(2n),
html.dark .report-detail-main .md table tr:nth-child(2n) {
  background-color: #252529;
}

/* 暗色：波折线用主题金，激活态半透明底 + 继承正文色（避免黄底白字） */
html.dark .report-detail-main .report-annotation-highlight,
html.dark .report-detail-main mark.report-annotation-highlight-mark {
  background-color: transparent !important;
  text-decoration-color: var(--crepe-color-primary, #f4bd6f) !important;
}
html.dark .report-detail-main .report-annotation-highlight.is-active,
html.dark .report-detail-main mark.report-annotation-highlight-mark.is-active {
  background-color: rgba(244, 189, 111, 0.2) !important;
  text-decoration-color: var(--crepe-color-primary, #f4bd6f) !important;
  color: inherit !important;
}
html.dark .report-detail-main .report-annotation-highlight.is-flash,
html.dark .report-detail-main mark.report-annotation-highlight-mark.is-flash {
  animation: report-annotation-flash-dark 0.9s ease;
}
html.dark .report-annotation-overlay__rect {
  background-color: rgba(244, 189, 111, 0.32);
  box-shadow: inset 0 -2px 0 0 var(--crepe-color-primary, #f4bd6f);
}
html.dark .report-annotation-overlay__connector-path {
  stroke: var(--crepe-color-primary, #f4bd6f);
  opacity: 0.9;
}
@keyframes report-annotation-flash-dark {
  0% { background-color: rgba(244, 189, 111, 0.35); }
  100% { background-color: rgba(244, 189, 111, 0.2); }
}
</style>
<style>
/* 引入统一的 Markdown 样式 */
@import '~@/assets/styles/markdown.css';
</style>
