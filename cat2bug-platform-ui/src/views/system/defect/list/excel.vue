<template>
  <div class="defect-excel-root">
    <div class="defect-excel-tools defect-table-tools-bar defect-view-toolbar">
      <div class="defect-excel-tools-left">
        <slot name="left-tools" />
      </div>
      <div class="defect-excel-tools-right table-tools row">
        <el-tooltip :content="$t('refresh')" placement="top">
          <el-button
            icon="el-icon-refresh"
            size="small"
            circle
            plain
            :loading="loading"
            @click="handleExcelToolbarRefresh"
          />
        </el-tooltip>
        <el-popover
          v-model="excelToolbarColumnPickerVisible"
          placement="bottom-end"
          trigger="click"
          popper-class="defect-excel-column-picker-popover"
        >
          <div class="defect-excel-picker-head">
            <i class="el-icon-s-fold"></i>
            <h4>{{ $t("display-field") }}</h4>
          </div>
          <el-divider class="defect-field-divider" />
          <el-checkbox-group
            v-model="excelColumnPickerCheckedKeys"
            class="defect-column-picker"
            @change="onExcelColumnPickerChange"
          >
            <el-checkbox v-for="c in excelTableColumnPickerOptions" :key="c.key" :label="c.key">{{
              $t(c.key)
            }}</el-checkbox>
          </el-checkbox-group>
          <el-button slot="reference" style="padding: 9px" plain size="small" icon="el-icon-s-fold" />
        </el-popover>
        <slot name="right-tools" />
      </div>
    </div>

    <div
      ref="defectExcelContext"
      v-loading="loading"
      class="defect-excel-context"
      :class="{ 'defect-excel-col-dragging': excelColDragActive }"
      @mousedown.capture="onDefectExcelCellInteractiveMousedownCapture"
      @click.capture="onDefectExcelCellInteractiveClickCapture"
      @paste.capture="onDefectExcelPasteCapture"
    >
      <vue-excel-editor
        ref="excelEditor"
        v-model="sheetRows"
        class="defect-vue-excel-editor"
        width="100%"
        :height="excelEditorHeightPx + 'px'"
        :header-label="defectExcelHeaderLabel"
        :cell-style="defectExcelCellStyle"
        :localized-label="excelEditorLocalizedLabel"
        no-paging
        no-mouse-scroll
        no-header-edit
        @update="handleSheetUpdate"
        @setting="onExcelEditorSettingFromLibrary"
        @delete-selected="handleExcelDeleteSelectedRows"
      >
        <vue-excel-column
          field="defectId"
          label="defectId"
          :pos="0"
          invisible
          readonly
          key-field
          type="string"
          width="1px"
          :sort="excelColumnNoSortFn"
        />
        <vue-excel-column
          field="projectNum"
          :label="columnLabel(projectNumColumnDef)"
          :pos="1"
          :invisible="!excelColumnVisible('projectNum')"
          type="string"
          readonly
          :width="projectNumColumnDef.width + 'px'"
          :init-style="projectNumCellStyle"
          :to-text="projectNumToText"
          :sort="excelColumnNoSortFn"
        />
        <!-- 列显隐与库内 PanelSetting 一致：始终注册全部列，用 invisible + 原地改 fields[].invisible（见 applyExcelColumnVisibilityToEditorFields） -->
        <vue-excel-column
          v-for="c in colsWithoutProjectNum"
          :key="c.key"
          v-bind="excelColumnValueBind(c)"
          :field="c.key"
          :pos="excelSheetDataColumnPos(c)"
          :invisible="!excelColumnVisible(c.key)"
          :label="columnLabel(c)"
          :type="columnFieldType(c)"
          :readonly="columnCellReadonly(c)"
          :allow-keys="columnAllowKeys(c)"
          :width="c.width + 'px'"
          :options="columnOptions(c)"
          :auto-fill-width="c.key === 'defectName' || c.key === 'defectDescribe'"
          :cell-html="excelColumnCellHtml(c)"
          :to-text="excelColumnToText(c)"
          :sort="excelColumnNoSortFn"
        />
      </vue-excel-editor>
    </div>

    <!-- 贴在单元格下方，focus 后直接弹出 Element 日期时间下拉面板（非对话框） -->
    <!-- 交付物列右侧三角：固定层内嵌 SelectModule（与计划时间列日期面板同理） -->
    <div
      v-if="excelModulePickerVisible"
      ref="excelModulePickerAnchor"
      class="defect-excel-module-picker-anchor"
      :style="excelModulePickerAnchorStyle"
      @mousedown.stop
    >
      <select-module
        :key="
          'excel-mod-' +
          (excelModulePickerRow && excelModulePickerRow.$id != null ? String(excelModulePickerRow.$id) : '0') +
          '-' +
          (excelModulePickerModuleId == null || excelModulePickerModuleId === '' ? 'x' : String(excelModulePickerModuleId))
        "
        ref="excelModuleSelect"
        :module-id="excelModulePickerModuleId"
        :project-id="excelModulePickerProjectId"
        :is-edit="true"
        :clearable="true"
        :direct-menu="true"
        popper-extra-class="defect-excel-module-picker-popper"
        size="small"
        @input="onExcelModuleSelectInput"
      />
    </div>

    <div
      v-show="planTimePickerVisible"
      class="defect-excel-plan-time-picker-anchor"
      :style="planTimePickerAnchorStyle"
    >
      <el-date-picker
        ref="planTimeDatePicker"
        v-model="planTimeDialogValue"
        type="datetime"
        value-format="yyyy-MM-dd HH:mm:ss"
        :placeholder="planTimeDialogPlaceholder"
        :editable="false"
        popper-class="defect-excel-plan-datetime-panel"
        default-time="12:00:00"
        @change="onPlanTimePickerChange"
        @blur="onPlanTimePickerBlur"
      />
    </div>

    <!-- 与 Cat2BugPreviewImage / el-image 一致：Element 大图预览（缩放、旋转、左右切换） -->
    <el-image-viewer
      v-if="excelImageViewerVisible"
      :key="excelImageViewerKey"
      :url-list="excelImageViewerUrls"
      :initial-index="excelImageViewerIndex"
      :z-index="3000"
      :on-close="closeExcelImageViewer"
    />
    <!-- Excel 图片/附件列内联上传：由 cellHtml 内按钮触发 click() -->
    <input
      ref="excelInlineImgFileInput"
      type="file"
      class="defect-excel-inline-file-input"
      accept="image/png,image/jpeg,image/jpg"
      multiple
      @change="onExcelInlineImgFileChange"
    />
    <input ref="excelInlineAnnexFileInput" type="file" class="defect-excel-inline-file-input" multiple @change="onExcelInlineAnnexFileChange" />
    <!-- 表头列拖曳：位置/内容由脚本直写 DOM，避免 mousemove 触发整组件重绘卡顿 -->
    <div ref="excelColDragGhostEl" v-show="excelColDragGhost.show" class="defect-excel-col-drag-ghost">
      <div ref="excelColDragGhostInnerEl" class="defect-excel-col-drag-ghost-inner"></div>
    </div>
    <!-- 拖入高亮：盖在表头格上方，不改 th DOM/CSS，避免列宽与 colgroup 错位 -->
    <div ref="excelColDropHighlightOverlay" class="defect-excel-col-drop-highlight-overlay" aria-hidden="true" />
  </div>
</template>

<script>
import ElImageViewer from "element-ui/packages/image/src/image-viewer.vue";
import SelectModule from "@/components/Module/SelectModule/index.vue";
import { listDefect, updateDefect, addDefect, getDefect, delDefect } from "@/api/system/defect";
import { listMemberOfProject } from "@/api/system/project";
import { upload } from "@/api/common/upload";
import { strFormat } from "@/utils";
import { checkPermi } from "@/utils/permission";
import errorCode from "@/utils/errorCode";
import { TableOptions } from "@/views/system/defect/list/table-options";

/**
 * 与 list/table.vue 中 cat2-bug-table 的 cache-key="defect-table" 及 Cat2BugTable columnsStorageKey()
 *（cacheKey + 'defect-table-field-list'）完全一致，列显隐共用一份本地缓存。
 */
const DEFECT_LIST_TABLE_CACHE_KEY = "defect-table";
const DEFECT_TABLE_FIELD_LIST_STORAGE_KEY =
  DEFECT_LIST_TABLE_CACHE_KEY + "defect-table-field-list";
/** 缺陷 Excel 视图列宽（field.name -> \"NNNpx\"），与表格列缓存分开存储 */
const DEFECT_EXCEL_COLUMN_WIDTH_STORAGE_KEY = DEFECT_LIST_TABLE_CACHE_KEY + "-excel-col-widths";
/** 表头拖列：超过该位移(px)才视为拖曳，避免误触 */
const EXCEL_HEADER_COL_DRAG_THRESHOLD_PX = 8;
/** 首行表头固定高度(px)，与样式中 thead 首行一致；calCellTop2 与库筛选行对齐也用此值 */
const EXCEL_HEAD_ROW_PX = 32;

/** 非空 options 使 vue-excel-editor 给 td 加 .select（与类型/优先级列相同的右侧三角）；计划列用三角热区 + 弹层选时间 */
const PLAN_TIME_CELL_OPTIONS_MARKER = Object.freeze({ __excelPlanTimeMarker: "" });

/** 与 AddDefect / EditDefectDialog 中 ImageUpload、FileUpload 的 limit 一致 */
const EXCEL_INLINE_IMG_LIMIT = 9;
const EXCEL_INLINE_ANNEX_LIMIT = 9;
const EXCEL_INLINE_IMG_FILE_TYPES = Object.freeze(["png", "jpg", "jpeg"]);
const EXCEL_INLINE_IMG_MAX_MB = 5;
const EXCEL_INLINE_ANNEX_MAX_MB = 30;

/**
 * 与 node_modules/vue-excel-editor/src/VueExcelEditor.vue 中
 * `.systable tbody td.select:not(.readonly)` 的 data URL 逐字一致。
 * 曾误删 base64 中一段（SUExUR 后少了 dnZ2），PNG 解码失败会导致三角整块不显示。
 */
const PLAN_TIME_SELECT_TRIANGLE_BG = Object.freeze({
  backgroundImage:
    'url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABIAAAASCAMAAABhEH5lAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAASUExURQAAANra2tfX19ra2tnZ2dnZ2c8lDs8AAAAFdFJOUwAwQL/PKlwehgAAAAlwSFlzAAAXEQAAFxEByibzPwAAAEdJREFUKFNdyskBACAIA8F49d+yiBEh+9rHYC5poPGiDmUDUGZI2EHCHBV2UWFEiT2UWKBgHwVLiCwjsoKcVeRMkDFFxoiADtH4AyvGhvOPAAAAAElFTkSuQmCC")',
  backgroundRepeat: "no-repeat",
  backgroundSize: "8px 8px",
  /* 与库内 td.select 一致：三角垂直居中、靠右 */
  backgroundPosition: "right 6px center",
  paddingRight: "18px",
  position: "relative",
});

const COLS = [
  { key: "projectNum", titleKey: "id", width: 90, editable: false, showInColumnPicker: false },
  { key: "defectType", titleKey: "type", width: 120, editable: true, fieldType: "map" },
  { key: "defectName", titleKey: "defect.name", width: 260, editable: true, required: true },
  { key: "defectLevel", titleKey: "priority", width: 100, editable: true, fieldType: "map" },
  /** map：成员 userId 字符串 -> 昵称；Excel 中只编辑首位处理人，保存为 handleBy: [id] */
  { key: "excelHandleByMemberId", titleKey: "handle-by", width: 180, editable: true, fieldType: "map", required: true },
  /** 格内存状态枚举序号字符串 "0".."4"；占位行排队/进行中仍为哨兵串 */
  { key: "defectStateText", titleKey: "state", width: 100, editable: true, fieldType: "map" },
  { key: "moduleName", titleKey: "module", width: 180, editable: true },
  { key: "moduleVersion", titleKey: "version", width: 120, editable: true },
  { key: "defectDescribe", titleKey: "describe", width: 200, editable: true },
  /** 与 table.vue 中 imgUrls 列同源；由 cellHtml 平铺缩略图；不落库文本，改图走上传/剪贴板文件 */
  { key: "excelImgUrlsText", titleKey: "image", width: 220, editable: true },
  /** cellHtml 附件行；不落库文本，改附件走上传/剪贴板文件 */
  { key: "excelAnnexUrlsText", titleKey: "annex", width: 400, editable: true },
  /** columnOptions 带三角；可键盘改时间，清空走 params.clearPlan* */
  { key: "planStartTime", titleKey: "plan-start-time", width: 170, editable: true },
  { key: "planEndTime", titleKey: "plan-end-time", width: 170, editable: true },
  /** 创建人由后台在创建时写入，仅只读展示 */
  { key: "createByText", titleKey: "createBy", width: 120, editable: false },
  { key: "updateTime", titleKey: "update-time", width: 170, editable: false },
];

const KEYS_IN_EXCEL_FOR_PICKER = new Set(
  COLS.filter((c) => c.key !== "projectNum").map((c) => String(c.titleKey))
);
/** 编号列在 TableOptions 中为 key `id`，与 COLS.projectNum.titleKey 一致 */
KEYS_IN_EXCEL_FOR_PICKER.add("id");

/** 优先级下拉顺序：与 sys_dict_data.defect_level 值（急/高/中/低）一致 */
const DEFECT_LEVEL_DICT_VALUE_ORDER = Object.freeze(["urgent", "height", "middle", "low"]);
/** 类型/优先级留空时的持久化默认值（与后端 DefectDefaults 一致） */
const DEFAULT_DEFECT_TYPE = "BUG";
const DEFAULT_DEFECT_LEVEL = "middle";

/** 与 Excel「显示字段」勾选列表一致（含编号 id ↔ projectNum） */
function isExcelPickerTableOption(c) {
  return (
    c &&
    c.showInColumnPicker !== false &&
    KEYS_IN_EXCEL_FOR_PICKER.has(String(c.key))
  );
}

/** 表头拖列只重排数据列，不把「编号」并入可拖动的中间块（避免写缓存时把 id 挤到末尾） */
function isExcelDragReorderTableOption(c) {
  return isExcelPickerTableOption(c) && String(c.key) !== "id";
}

/** 必填列被清空时的提示文案（与 i18n 中 defect.*-cannot-empty 一致；勿误用单一「处理人」文案） */
const EXCEL_REQUIRED_EMPTY_MSG_I18N = Object.freeze({
  defectName: "defect.defect-name-cannot-empty",
  excelHandleByMemberId: "defect.handle-by-cannot-empty",
});

/** vue-excel-editor 列头点击会触发排序；恒返回 0 表示不重排（等价于关闭排序） */
const excelColumnNoSort = () => 0;

/** 至少展示的行数（含数据行），模拟 Excel 下方大量空行 */
const MIN_SHEET_ROWS = 100;
/** 在数据行之后再保留的空行数量下限 */
const TAIL_EMPTY_ROWS = 120;
/** Excel 视图 listDefect 每页条数（与缺陷列表页 query 默认 pageSize 解耦，仅本组件请求使用） */
const EXCEL_DEFECT_PAGE_SIZE = 20;
/** 仅 Excel 视图 listDefect：固定按编号升序（与父级缺陷列表 query 排序无关，search/loadMore 内会覆盖） */
const EXCEL_LIST_ORDER_BY_COLUMN = "projectNum";
const EXCEL_LIST_IS_ASC = "asc";
/** 距表格内容区底部多少 px 内触发加载下一页（物理底部） */
const EXCEL_SCROLL_LOAD_MORE_THRESHOLD_PX = 120;
/** 最后一条已加载数据行距可视区底边在该距离内也触发加载（下方有大量占位行时仍要能拉最后一页） */
const EXCEL_LAST_DATA_ROW_NEAR_BOTTOM_PX = 160;
/** 用于判断「首屏数据行高度是否明显不足一屏」的估算行高(px)；与库内 tbody td 约 24px 对齐 */
const EXCEL_DATA_ROW_ESTIMATE_PX = 26;
/** 表格可视区底部相对 .defect-excel-context 内底的留白(px)；高度优先用 context.clientHeight − gap */
const EXCEL_VIEWPORT_BOTTOM_GAP = 20;
/** 底部自定义横条 fallback(px)；与 ::v-deep .footer 高度一致 */
const VUE_EXCEL_EDITOR_FOOTER_BAR_PX = 8;
/** 占位行状态列：批量创建队列中的展示（仅存于 defectStateText，非接口值） */
const EXCEL_CREATE_ROW_QUEUED = "__CAT2BUG_EXCEL_CREATE_QUEUED__";
const EXCEL_CREATE_ROW_RUNNING = "__CAT2BUG_EXCEL_CREATE_RUNNING__";

export default {
  name: "DefectExcel",
  components: {
    ElImageViewer,
    SelectModule,
  },
  dicts: ["defect_level"],
  props: {
    query: {
      type: Object,
      default: () => ({
        pageNum: 1,
        pageSize: 10,
        orderByColumn: "projectNum",
        isAsc: "asc",
        projectId: 0,
        params: {},
      }),
    },
    projectId: {
      type: Number,
      default: null,
    },
    /** 与缺陷页 config.types 一致，用于「类型」下拉 */
    defectTypeOptions: {
      type: Array,
      default: () => [],
    },
    /** 表格可视区底部留白(px)；与容器 clientHeight 一起决定 excelEditorHeightPx，默认 20 */
    viewportBottomGap: {
      type: Number,
      default: EXCEL_VIEWPORT_BOTTOM_GAP,
    },
  },
  data() {
    return {
      /** 供 vue-excel-column :sort，关闭列头点击重排 */
      excelColumnNoSortFn: excelColumnNoSort,
      /** map 列放开 readonly 后用于禁止键盘输入，仅保留右侧点击打开下拉（与库内 25px 热区一致） */
      excelMapBlockKeys: [],
      /** 编号列样式：库默认 td 为 vertical-align: bottom */
      projectNumCellStyle: { verticalAlign: "middle", textAlign: "center" },
      loading: false,
      defectList: [],
      sheetRows: [],
      queryParams: this.query,
      syncing: false,
      /** 已删行编辑提示节流（毫秒时间戳） */
      _deletedDefectEditWarnAt: 0,
      /** 与 vue-excel-column 的 options 引用保持一致；字典/配置异步到达后再写入，避免 map 列一直用空表 */
      defectTypeSelectMap: {},
      defectLevelSelectMap: {},
      /** 缺陷状态 0..4 -> 展示文案，与 SysDefectStateEnum 序一致 */
      defectStateSelectMap: {},
      /** 项目成员 userId -> 展示名，供处理人下拉 */
      memberSelectMap: {},
      /** 由表格区域容器 ResizeObserver 测量，使 vue-excel-editor 铺满剩余高度 */
      excelEditorHeightPx: 480,
      planTimePickerVisible: false,
      planTimePickerAnchorStyle: {},
      planTimeDialogRow: null,
      planTimeDialogField: "",
      planTimeDialogValue: null,
      planTimeDialogOldValue: "",
      /** 延后打开日期面板，避免 vue-excel-editor 内 inputBox.focus 抢走焦点 */
      planTimeOpenTimer: null,
      /** 占位行（无 defectId）正在提交新增，避免重复请求 */
      excelPlaceholderRowSubmitLock: {},
      /** 占位行批量新增：FIFO 队列（$id 字符串），串行 addDefect 避免「请勿重复提交」 */
      excelDefectCreateQueueIds: [],
      /** 队列 drain 执行中，防止并发 drain */
      excelDefectCreateQueueDraining: false,
      /** 计划时间保存中，防止 @change 与自定义「清除」等路径重复打 updateDefect */
      planTimePickerSaving: false,
      /** 工具栏「显示字段」el-popover；不因表格 scroll 关闭（见 dismissExcelFloatingPanelsOnTableScroll） */
      excelToolbarColumnPickerVisible: false,
      /** 交付物 SelectModule：三角打开，与计划时间锚层互斥 */
      excelModulePickerVisible: false,
      excelModulePickerAnchorStyle: {},
      excelModulePickerRow: null,
      excelModulePickerModuleId: null,
      excelModulePickerProjectId: null,
      excelModulePickerSaving: false,
      _excelModulePickerOutsideClose: null,
      /** 图片列缩略图点击：Element el-image-viewer 全屏预览（与 el-image preview-src-list 一致） */
      excelImageViewerVisible: false,
      excelImageViewerUrls: [],
      excelImageViewerIndex: 0,
      excelImageViewerKey: 0,
      /** 内联上传：最近一次点击的上传按钮上下文（change 时消费） */
      excelInlineUploadTarget: null,
      /** 与表格视图一致：存 TableOptions 的 key（如 type、defect.name），非 COLS 字段名 */
      excelColumnPickerCheckedKeys: [],
      /** scheduleExcelEditorLayoutFix 防抖 timer，避免与列头点击、refreshPageSize 多次交错 */
      excelLayoutFixDebounceTimer: null,
      /** 监听库延迟写回 th 行高，异常时再次夹紧 */
      excelHeaderMutationObs: null,
      excelHeaderClampTimer: null,
      /** sync 内延迟夹紧的 timer，销毁时清理（勿用 _ 前缀：Vue2 不代理到 this） */
      excelLateClampTimers: [],
      /** 库内 fields 深监听会频繁 @setting，合并后再写缓存与 excelColumnPickerCheckedKeys */
      excelSettingSyncTimer: null,
      /** 数据列顺序（不含 projectNum）；null 表示与 COLS 默认一致 */
      excelDataColumnKeyOrder: null,
      /** 与 $cache 列顺序联动：递增后强制重算 excelTableColumnPickerOptions（local 非响应式） */
      excelColumnPickerOrderRev: 0,
      /** 表头拖列调整顺序：移动中用于光标与样式 */
      excelColDragActive: false,
      _excelColDrag: null,
      _excelColDragBoundMove: null,
      _excelColDragBoundUp: null,
      /** 列头拖曳幽灵：仅 show 走 Vue，位置与 innerHTML 在拖动中直写 DOM */
      excelColDragGhost: {
        show: false,
      },
      /** 计划时间 / 交付物「三角热区」mousedown（capture），beforeDestroy 里按此解绑 */
      _planTimeTriZoneMouseDown: null,
      _planTimeExcelEditorRootEl: null,
      /** Excel 列表分页：接口 total、已请求页码、触底加载中（与父级 queryParams.pageSize 无关） */
      defectTotal: 0,
      excelListPageNum: 1,
      excelListLoadingMore: false,
      excelListRequestGen: 0,
      _excelScrollBound: null,
      _excelTableContentEl: null,
      _excelScrollLoadDebounceTimer: null,
      /** 首屏/追加后若仍无纵向滚动条，自动再拉页的上限，防止死循环 */
      excelViewportAutoFetchCount: 0,
    };
  },
  computed: {
    /** vue-excel-editor map：key 为接口值，value 为展示文案 */
    defectTypeMap() {
      const m = {};
      (this.defectTypeOptions || []).forEach((t) => {
        const k = t.value;
        if (k == null || k === "") return;
        m[k] = String(this.$t(k) || t.key || k);
      });
      return m;
    },
    defectLevelMap() {
      const m = {};
      const list = [...(this.dict.type.defect_level || [])];
      const pri = DEFECT_LEVEL_DICT_VALUE_ORDER;
      list.sort((a, b) => {
        const av = String(a.value);
        const bv = String(b.value);
        const ia = pri.indexOf(av);
        const ib = pri.indexOf(bv);
        const ra = ia >= 0 ? ia : 100;
        const rb = ib >= 0 ? ib : 100;
        if (ra !== rb) return ra - rb;
        const as = a.raw && (a.raw.dictSort != null ? a.raw.dictSort : a.raw.dict_sort);
        const bs = b.raw && (b.raw.dictSort != null ? b.raw.dictSort : b.raw.dict_sort);
        return (Number(as) || 0) - (Number(bs) || 0);
      });
      list.forEach((item) => {
        const k = item.value;
        if (k == null || k === "") return;
        m[k] = String(this.$t(item.value) || item.label || item.value);
      });
      return m;
    },
    /**
     * 与 SysDefectStateEnum 序一致：PROCESSING=0 … CLOSED=4。
     * RESOLVED(2) 已弃用：与 /system/defect/config 中 states 一致，不提供下拉选项；历史行仍用 excelDefectStateToText 展示。
     */
    defectStateMap() {
      const keys = ["PROCESSING", "AUDIT", "RESOLVED", "REJECTED", "CLOSED"];
      const m = {};
      keys.forEach((k, i) => {
        if (k === "RESOLVED") return;
        m[String(i)] = String(this.$t(k) || k);
      });
      return m;
    },
    projectNumColumnDef() {
      return COLS.find((c) => c.key === "projectNum") || COLS[0];
    },
    colsWithoutProjectNum() {
      const base = COLS.filter((c) => c.key !== "projectNum");
      const order = this.excelDataColumnKeyOrder;
      if (!order || !Array.isArray(order) || !order.length) return base;
      const byKey = new Map(base.map((c) => [c.key, c]));
      const out = [];
      const seen = new Set();
      order.forEach((k) => {
        const c = byKey.get(k);
        if (c) {
          out.push(c);
          seen.add(k);
        }
      });
      base.forEach((c) => {
        if (!seen.has(c.key)) out.push(c);
      });
      return out;
    },
    /**
     * 与 table.vue / Cat2BugTable 共用 DEFECT_TABLE_FIELD_LIST_STORAGE_KEY 中的列顺序；
     * 含编号 id（对应 Excel 的 projectNum 列）。
     */
    excelTableColumnPickerOptions() {
      void this.excelColumnPickerOrderRev;
      const defaults = TableOptions.map((d) => ({ ...d }));
      const merged = this.mergeTableOptionsWithCache(defaults, DEFECT_TABLE_FIELD_LIST_STORAGE_KEY);
      const out = [];
      const seen = new Set();
      merged.forEach((m) => {
        if (!isExcelPickerTableOption(m)) return;
        const base = TableOptions.find((x) => String(x.key) === String(m.key));
        if (!base) return;
        out.push({ ...base, fixed: !!m.fixed, visible: m.visible !== false });
        seen.add(String(m.key));
      });
      TableOptions.filter(isExcelPickerTableOption).forEach((base) => {
        if (!seen.has(String(base.key))) out.push({ ...base });
      });
      return out;
    },
    defaultExcelColumnPickerKeys() {
      return TableOptions.filter(isExcelPickerTableOption).map((c) => c.key);
    },
    /** 内置 PanelSetting 标题与按钮文案（与 vue3-excel-editor 默认键一致） */
    excelEditorLocalizedLabel() {
      return {
        tableSetting: String(this.$t("display-field")),
        exportExcel: `${String(this.$t("export"))} Excel`,
        importExcel: `${String(this.$t("defect.import"))} Excel`,
        back: String(this.$t("back")),
        reset: "Default",
        footerLeft: (top, bottom, total) => `Record ${top} to ${bottom} of ${total}`,
        first: "First",
        previous: "Previous",
        next: "Next",
        last: "Last",
        footerRight: {
          selected: "Selected:",
          filtered: "Filtered:",
          loaded: "Loaded:",
        },
        processing: "Processing",
        sortingAndFiltering: "Sorting And Filtering",
        sortAscending: "Sort Ascending",
        sortDescending: "Sort Descending",
        near: "≒ Near",
        exactMatch: "= Exact Match",
        notMatch: "≠ Not Match",
        greaterThan: "&gt; Greater Than",
        greaterThanOrEqualTo: "≥ Greater Than or Equal To",
        lessThan: "&lt; Less Than",
        lessThanOrEqualTo: "≤ Less Than Or Equal To",
        regularExpression: "~ Regular Expression",
        customFilter: "Custom Filter",
        listFirstNValuesOnly: (n) => `List first ${n} values only`,
        apply: "Apply",
        noRecordIsRead: "No record is read",
        readonlyColumnDetected: "Readonly column detected",
        columnHasValidationError: (name, err) => `Column ${name} has validation error: ${err}`,
        rowHasValidationError: (row, name, err) =>
          `Row ${row} has validation error for column ${name}: ${err}`,
        noMatchedColumnName: "No matched column name",
        invalidInputValue: "Invalid input value",
        missingKeyColumn: "Missing key column",
        noRecordIndicator: "No record",
      };
    },
    planTimeDialogPlaceholder() {
      if (this.planTimeDialogField === "planEndTime") return String(this.$t("defect.please-select-end-time"));
      return String(this.$t("defect.please-select-start-time"));
    },
  },
  watch: {
    defectTypeMap: {
      handler(map) {
        this.syncExcelOptionMap(this.defectTypeSelectMap, map);
        this.refreshExcelEditorView();
      },
      immediate: true,
    },
    defectLevelMap: {
      handler(map) {
        this.syncExcelOptionMap(this.defectLevelSelectMap, map);
        this.refreshExcelEditorView();
      },
      immediate: true,
    },
    defectStateMap: {
      handler(map) {
        this.syncExcelOptionMap(this.defectStateSelectMap, map);
        this.refreshExcelEditorView();
      },
      immediate: true,
    },
    projectId() {
      this.refreshProjectMembers();
    },
    "queryParams.projectId"() {
      this.refreshProjectMembers();
    },
    "$i18n.locale"() {
      this.$nextTick(() => {
        this.applyExcelEditorI18nHeaders();
        this.scheduleExcelEditorLayoutFix();
      });
    },
    loading(val) {
      if (val === false) {
        this.$nextTick(() => this.scheduleExcelEditorLayoutFix());
      }
    },
  },
  created() {
    this.initExcelColumnPickerFromCache();
    this.initExcelColumnOrderFromCache();
  },
  mounted() {
    this.bindExcelLayoutObserver();
    this.refreshProjectMembers();
    this.$nextTick(() => {
      this.applyExcelColumnVisibilityToEditorFields();
      this.applyExcelColumnOrderToEditorFields();
      this.applyPersistedExcelColumnWidthsToEditor();
      this.maybeRunInitialExcelColumnFill();
      this.scheduleExcelEditorLayoutFix();
      this.bindPlanTimeTriZoneMouseDown();
    });
  },
  beforeDestroy() {
    this.closeExcelModulePicker();
    this.unbindPlanTimeTriZoneMouseDown();
    this.teardownExcelEditorCalStickyPatch();
    if (this.planTimeOpenTimer) clearTimeout(this.planTimeOpenTimer);
    this.clearExcelEditorLayoutFixTimers();
    this.disconnectExcelHeaderLayoutObserver();
    if (this.excelHeaderClampTimer) {
      clearTimeout(this.excelHeaderClampTimer);
      this.excelHeaderClampTimer = null;
    }
    if (Array.isArray(this.excelLateClampTimers)) {
      this.excelLateClampTimers.forEach((id) => clearTimeout(id));
      this.excelLateClampTimers = [];
    }
    if (this.excelSettingSyncTimer) {
      clearTimeout(this.excelSettingSyncTimer);
      this.excelSettingSyncTimer = null;
    }
    this.teardownExcelColumnHeaderDragSession();
    this.unbindExcelLayoutObserver();
    this.teardownExcelScrollLoadMoreListener();
  },
  methods: {
    /** 库在拖拽列宽时仍 lazy 调 calStickyLeft，其末尾 $forceUpdate 会用 fields[].width 重绑 col，与 DOM 上正在拖的宽度冲突导致左右跳 */
    isExcelEditorColSepDragging() {
      const ed = this.$refs.excelEditor;
      return !!(ed && ed.sep && ed.sep.curCol);
    },
    ensureExcelEditorCalStickySafeDuringColDrag() {
      const ed = this.$refs.excelEditor;
      if (!ed || ed._cat2bugStickyPatch) return;
      const orig = ed.calStickyLeft;
      if (typeof orig !== "function") return;
      ed._cat2bugCalStickyOriginal = orig;
      ed._cat2bugStickyPatch = true;
      /* 库内 lazy() 用 setTimeout(() => p()) 调用，无 this；必须用 ed 判断 sep 并以 ed 为 this 调原版 */
      ed.calStickyLeft = function cat2bugCalStickyGuarded() {
        if (ed.sep && ed.sep.curCol) return;
        return ed._cat2bugCalStickyOriginal.apply(ed, arguments);
      };
    },
    teardownExcelEditorCalStickyPatch() {
      const ed = this.$refs.excelEditor;
      if (!ed || !ed._cat2bugCalStickyOriginal) return;
      ed.calStickyLeft = ed._cat2bugCalStickyOriginal;
      delete ed._cat2bugCalStickyOriginal;
      delete ed._cat2bugStickyPatch;
    },
    bindExcelLayoutObserver() {
      const run = () => this.updateExcelEditorHeight();
      if (typeof ResizeObserver !== "undefined") {
        this._excelLayoutObs = new ResizeObserver(run);
        this.$nextTick(() => {
          const el = this.$refs.defectExcelContext;
          if (el) this._excelLayoutObs.observe(el);
          run();
          setTimeout(run, 100);
          setTimeout(run, 400);
        });
      } else {
        this._excelLayoutOnResize = run;
        window.addEventListener("resize", run);
        this.$nextTick(run);
      }
    },
    unbindExcelLayoutObserver() {
      if (this._excelLayoutObs) {
        this._excelLayoutObs.disconnect();
        this._excelLayoutObs = null;
      }
      if (this._excelLayoutOnResize) {
        window.removeEventListener("resize", this._excelLayoutOnResize);
        this._excelLayoutOnResize = null;
      }
    },
    updateExcelEditorHeight() {
      const wrap = this.$refs.defectExcelContext;
      if (!wrap) return;
      const gap = Math.max(0, Number(this.viewportBottomGap) || 0);
      const bottomChrome = this.measureExcelEditorBottomChromePx();
      /*
       * 优先用 flex 已分配给 .defect-excel-context 的 clientHeight，避免用「视口/缺陷页底 − top」在
       * 主内容区高于 .defect-page 底、或首轮 layout 未稳时算矮，导致表格下方大块留白。
       */
      const ch = wrap.clientHeight;
      let space;
      if (ch >= 1) {
        space = Math.max(0, Math.floor(ch - gap));
      } else {
        const top = wrap.getBoundingClientRect().top;
        let bottomBoundary = window.innerHeight;
        if (typeof wrap.closest === "function") {
          const pageEl = wrap.closest(".defect-page");
          if (pageEl && typeof pageEl.getBoundingClientRect === "function") {
            const pb = pageEl.getBoundingClientRect().bottom;
            /* 整页滚动时 .defect-page 底边常在视口下方，不能与视口底取较大值，否则 Excel 高度会被算成超大 */
            bottomBoundary = Math.min(pb, window.innerHeight);
          }
        }
        space = Math.max(0, Math.floor(bottomBoundary - top - gap));
      }
      /* 为底部 .footer 横条 + 编辑器/component-content 外框留出高度，避免被 .defect-excel-context overflow:hidden 裁切 */
      this.excelEditorHeightPx = Math.max(120, space - bottomChrome);
      const h = this.excelEditorHeightPx;
      this.$nextTick(() => {
        const ed = this.$refs.excelEditor;
        if (ed && ed.tableContent) {
          ed.tableContent.style.height = h + "px";
          if (!this.isExcelEditorColSepDragging()) this.scheduleExcelEditorLayoutFix();
        }
      });
    },
    /** table-content 之外占用的高度：footer 横条 + 编辑器/component-content 上下边框，避免底部滚动条被裁切 */
    measureExcelEditorBottomChromePx() {
      const ed = this.$refs.excelEditor;
      if (!ed || !ed.$el) {
        return VUE_EXCEL_EDITOR_FOOTER_BAR_PX + 3;
      }

      /* 优先量 table-content 底到编辑器外壳底，含 footer + 外框，避免固定常量和 border-box 估算偏差 */
      if (ed.tableContent && typeof ed.tableContent.getBoundingClientRect === "function") {
        const tcBottom = ed.tableContent.getBoundingClientRect().bottom;
        const shellBottom = ed.$el.getBoundingClientRect().bottom;
        const measured = shellBottom - tcBottom;
        if (measured > 0) {
          return Math.ceil(measured) + 2;
        }
      }

      let chrome = VUE_EXCEL_EDITOR_FOOTER_BAR_PX;
      const footer = ed.$refs && ed.$refs.footer;
      if (footer && typeof footer.getBoundingClientRect === "function") {
        const fh = footer.getBoundingClientRect().height;
        if (fh > 0) chrome = fh;
      }

      if (ed.$el) {
        const shellStyle = window.getComputedStyle(ed.$el);
        const shellBorderY =
          (parseFloat(shellStyle.borderTopWidth) || 0) + (parseFloat(shellStyle.borderBottomWidth) || 0);
        if (shellBorderY > 0) {
          chrome += shellBorderY;
        } else {
          const cc = ed.$el.querySelector(".component-content");
          if (cc) {
            const ccStyle = window.getComputedStyle(cc);
            chrome +=
              (parseFloat(ccStyle.borderTopWidth) || 0) + (parseFloat(ccStyle.borderBottomWidth) || 0);
          }
        }
      }

      return Math.ceil(chrome) + 2;
    },
    /**
     * 用内联 !important 锁死首行表头高度（压过 vue-excel-editor mounted 里异步写回的 height），
     * 再刷新滚动/列宽；最后挂上 MutationObserver，在库再次写入异常高度时重复夹紧。
     */
    syncExcelEditorInnerLayout() {
      if (this.isExcelEditorColSepDragging()) return;
      this.disconnectExcelHeaderLayoutObserver();
      const ed = this.$refs.excelEditor;
      if (!ed || !ed.labelTr || !ed.labelTr.children || !ed.labelTr.children.length) return;
      this.clampExcelEditorHeaderRow();
      /* 勿调 refreshPageSize：其内部会 columnFillWidth，用旧 fields[].width 覆盖 col 上刚拖好的宽度导致弹回 */
      this.refreshExcelEditorHorizontalLayout(ed);
      if (typeof ed.calStickyLeft === "function") ed.calStickyLeft();
      /* calStickyLeft 会改 th 的 :style(left)，可能冲掉 height，需再夹紧 */
      this.clampExcelEditorHeaderRow();
      try {
        if (typeof ed.calVScroll === "function") ed.calVScroll();
      } catch (e) {
        /* 列刚变更时 vScroller 可能尚未就绪 */
      }
      /* 勿 scrollTo(0, …)：会把横向滚动条打回第一列；保留 scrollLeft，宽度变化后夹紧到可滚范围 */
      if (ed.tableContent && typeof ed.tableContent.scrollTo === "function") {
        try {
          const tc = ed.tableContent;
          const maxLeft = Math.max(0, tc.scrollWidth - tc.clientWidth);
          const left = Math.min(Math.max(0, tc.scrollLeft), maxLeft);
          tc.scrollTo(left, tc.scrollTop);
        } catch (e) {
          /* ignore */
        }
      }
      this.clampExcelEditorHeaderRow();
      this.$nextTick(() => this.ensureExcelHeaderLayoutObserver());
      /* refreshPageSize 末尾会 setTimeout(calVScroll)，晚一拍再夹紧，避免异步布局把行高顶回去 */
      if (Array.isArray(this.excelLateClampTimers)) {
        this.excelLateClampTimers.forEach((id) => clearTimeout(id));
        this.excelLateClampTimers = [];
      }
      [150, 400].forEach((delay) => {
        const tid = setTimeout(() => this.clampExcelEditorHeaderRow(), delay);
        this.excelLateClampTimers.push(tid);
      });
    },
    clampExcelEditorHeaderRow() {
      const ed = this.$refs.excelEditor;
      if (!ed || !ed.labelTr) return;
      const row = ed.labelTr;
      const px = `${EXCEL_HEAD_ROW_PX}px`;
      const imp = "important";
      row.style.setProperty("height", px, imp);
      row.style.setProperty("max-height", px, imp);
      row.style.setProperty("min-height", px, imp);
      Array.from(row.querySelectorAll("th")).forEach((th) => {
        th.style.setProperty("height", px, imp);
        th.style.setProperty("max-height", px, imp);
        th.style.setProperty("min-height", px, imp);
      });
      ed.calCellTop2 = EXCEL_HEAD_ROW_PX;
    },
    disconnectExcelHeaderLayoutObserver() {
      if (this.excelHeaderMutationObs) {
        try {
          this.excelHeaderMutationObs.disconnect();
        } catch (e) {
          /* ignore */
        }
        this.excelHeaderMutationObs = null;
      }
    },
    ensureExcelHeaderLayoutObserver() {
      if (typeof MutationObserver === "undefined") return;
      const ed = this.$refs.excelEditor;
      if (!ed || !ed.labelTr) return;
      if (this.excelHeaderMutationObs) return;
      const row = ed.labelTr;
      const maxPx = EXCEL_HEAD_ROW_PX + 2;
      this.excelHeaderMutationObs = new MutationObserver(() => {
        const editor = this.$refs.excelEditor;
        const r = editor && editor.labelTr;
        if (!r) return;
        for (let i = 0; i < r.children.length; i++) {
          const el = r.children[i];
          if (!el || el.tagName !== "TH") continue;
          const inline = parseFloat(el.style.height);
          if (Number.isFinite(inline) && inline > maxPx) {
            this.queueExcelHeaderClamp();
            return;
          }
          const h = el.getBoundingClientRect().height;
          if (Number.isFinite(h) && h > EXCEL_HEAD_ROW_PX + 6) {
            this.queueExcelHeaderClamp();
            return;
          }
        }
      });
      this.excelHeaderMutationObs.observe(row, {
        attributes: true,
        attributeFilter: ["style"],
        subtree: true,
      });
    },
    queueExcelHeaderClamp() {
      if (this.excelHeaderClampTimer) clearTimeout(this.excelHeaderClampTimer);
      this.excelHeaderClampTimer = setTimeout(() => {
        this.excelHeaderClampTimer = null;
        this.disconnectExcelHeaderLayoutObserver();
        this.clampExcelEditorHeaderRow();
        this.$nextTick(() => this.ensureExcelHeaderLayoutObserver());
      }, 32);
    },
    clearExcelEditorLayoutFixTimers() {
      if (this.excelLayoutFixDebounceTimer != null) {
        clearTimeout(this.excelLayoutFixDebounceTimer);
        this.excelLayoutFixDebounceTimer = null;
      }
    },
    /**
     * 仅同步底部横向滚动条占位（摘自 vue-excel-editor refreshPageSize 前段），不触发 columnFillWidth。
     */
    refreshExcelEditorHorizontalLayout(ed) {
      if (!ed || !ed.$refs || !ed.systable || !ed.tableContent) return;
      const hScroll = ed.$refs.hScroll;
      if (!hScroll) return;
      try {
        const fullWidth = ed.systable.getBoundingClientRect().width;
        const viewWidth = ed.tableContent.getBoundingClientRect().width;
        if (!(fullWidth > 0) || !(viewWidth > 0)) return;
        if (!ed.hScroller) ed.hScroller = {};
        ed.hScroller.tableUnseenWidth = fullWidth - viewWidth;
        hScroll.style.width = 100 * (viewWidth / fullWidth) + "%";
        const scrollerWidth = hScroll.getBoundingClientRect().width;
        if (ed.footer && typeof ed.footer.getBoundingClientRect === "function") {
          ed.hScroller.scrollerUnseenWidth = ed.footer.getBoundingClientRect().width - 40 - scrollerWidth;
        }
      } catch (e) {
        /* ignore */
      }
    },
    normalizeExcelColWidthCss(w) {
      const s = String(w == null ? "" : w).trim();
      if (!s) return "";
      const n = parseFloat(s.replace(/px$/i, ""));
      if (!Number.isFinite(n)) return "";
      const clamped = Math.min(800, Math.max(20, Math.round(n)));
      return `${clamped}px`;
    },
    /** 将 getSetting().fields 里的宽度写回编辑器 fields，与 colgroup 一致，避免 Vue 重渲染把 col 绑回旧 width */
    applyExcelEditorWidthsFromSettingToFields(payload) {
      const ed = this.$refs.excelEditor;
      if (!ed || !Array.isArray(ed.fields) || !payload || !Array.isArray(payload.fields)) return;
      const byName = {};
      payload.fields.forEach((sf) => {
        if (!sf || !sf.name) return;
        const nw = this.normalizeExcelColWidthCss(sf.width);
        if (nw) byName[sf.name] = nw;
      });
      ed.fields.forEach((f) => {
        if (!f || !f.name) return;
        const nw = byName[f.name];
        if (nw) f.width = nw;
      });
      this.$nextTick(() => {
        const e = this.$refs.excelEditor;
        if (!e) return;
        this.refreshExcelEditorHorizontalLayout(e);
        if (typeof e.calStickyLeft === "function") e.calStickyLeft();
      });
    },
    persistExcelColumnWidthsToCache(payload) {
      if (!payload || !Array.isArray(payload.fields)) return;
      let prev = {};
      try {
        prev = this.$cache.local.getJSON(DEFECT_EXCEL_COLUMN_WIDTH_STORAGE_KEY) || {};
      } catch (e) {
        prev = {};
      }
      const next = { ...prev };
      payload.fields.forEach((sf) => {
        if (!sf || !sf.name) return;
        const nw = this.normalizeExcelColWidthCss(sf.width);
        if (nw) next[sf.name] = nw;
      });
      this.$cache.local.setJSON(DEFECT_EXCEL_COLUMN_WIDTH_STORAGE_KEY, next);
    },
    applyPersistedExcelColumnWidthsToEditor() {
      const ed = this.$refs.excelEditor;
      if (!ed || !Array.isArray(ed.fields)) return;
      let saved = null;
      try {
        saved = this.$cache.local.getJSON(DEFECT_EXCEL_COLUMN_WIDTH_STORAGE_KEY);
      } catch (e) {
        saved = null;
      }
      if (!saved || typeof saved !== "object") return;
      ed.fields.forEach((f) => {
        if (!f || !f.name) return;
        const nw = this.normalizeExcelColWidthCss(saved[f.name]);
        if (nw) f.width = nw;
      });
      this.$nextTick(() => {
        if (typeof ed.calStickyLeft === "function") ed.calStickyLeft();
        if (ed.$forceUpdate) ed.$forceUpdate();
      });
    },
    maybeRunInitialExcelColumnFill() {
      const ed = this.$refs.excelEditor;
      if (!ed) return;
      let saved = null;
      try {
        saved = this.$cache.local.getJSON(DEFECT_EXCEL_COLUMN_WIDTH_STORAGE_KEY);
      } catch (e) {
        saved = null;
      }
      const hasSaved = saved && typeof saved === "object" && Object.keys(saved).length > 0;
      if (hasSaved) return;
      this.$nextTick(() => {
        if (typeof ed.columnFillWidth === "function") ed.columnFillWidth();
        if (typeof ed.calStickyLeft === "function") ed.calStickyLeft();
      });
    },
    /** nextTick 一次 + 防抖一次，避免 ResizeObserver / 列操作与库内 setTimeout(calVScroll) 交错产生不固定行高 */
    scheduleExcelEditorLayoutFix() {
      this.ensureExcelEditorCalStickySafeDuringColDrag();
      this.clearExcelEditorLayoutFixTimers();
      const run = () => {
        if (this.isExcelEditorColSepDragging()) return;
        this.syncExcelEditorInnerLayout();
      };
      this.$nextTick(run);
      this.excelLayoutFixDebounceTimer = setTimeout(() => {
        this.excelLayoutFixDebounceTimer = null;
        run();
      }, 220);
    },
    syncExcelOptionMap(target, map) {
      Object.keys(target).forEach((k) => this.$delete(target, k));
      if (map && typeof map === "object") {
        Object.keys(map).forEach((k) => this.$set(target, k, map[k]));
      }
    },
    isDeletedDefectRow(row) {
      if (!row) return false;
      const f = row.delFlag;
      return f === "2" || f === 2;
    },
    notifyDeletedDefectCannotEdit() {
      const now = Date.now();
      if (now - (this._deletedDefectEditWarnAt || 0) < 800) return;
      this._deletedDefectEditWarnAt = now;
      this.$modal.msgWarning(String(this.$t("defect.deleted-cannot-edit")));
    },
    refreshExcelEditorView() {
      this.$nextTick(() => {
        const ed = this.$refs.excelEditor;
        if (!ed) return;
        if (this.isExcelEditorColSepDragging()) return;
        ed.$forceUpdate();
        /* cell-html（图片/附件）高度变化后，库内 inputSquare 仍用旧 getBoundingClientRect；须重算焦点框与拖选 overlay */
        this.$nextTick(() => {
          requestAnimationFrame(() => {
            this.repositionExcelEditorFocusChrome();
          });
        });
      });
    },
    /** 在 DOM 布局稳定后，让 vue-excel-editor 按当前格尺寸重画活动格（inputSquare）与矩形选区 overlay */
    repositionExcelEditorFocusChrome() {
      const ed = this.$refs.excelEditor;
      if (!ed || this.isExcelEditorColSepDragging()) return;
      /*
       * 库内 moveInputSquare 会清空 autocompleteInputs（三角点开 map 下拉后若再调一次，下拉一闪即关）。
       * 计划时间 / 库内日期面板打开时也不要重算活动格，避免抢焦点或误关面板。
       */
      const overlayOnly = () => {
        this.$nextTick(() => {
          const ed2 = this.$refs.excelEditor;
          if (!ed2 || typeof ed2.updateCellRangeSelectionOverlay !== "function") return;
          ed2.updateCellRangeSelectionOverlay();
        });
      };
      if (this.planTimePickerVisible) {
        overlayOnly();
        return;
      }
      const acOpen = ed.autocompleteInputs && ed.autocompleteInputs.length > 0;
      const libDateOpen = !!ed.showDatePicker;
      if (acOpen || libDateOpen) {
        overlayOnly();
        return;
      }
      if (ed.focused && typeof ed.moveInputSquare === "function" && ed.currentRowPos >= 0 && ed.currentColPos >= 0) {
        ed.moveInputSquare(ed.currentRowPos, ed.currentColPos);
      }
      overlayOnly();
    },
    /** 与 Cat2BugTable mergeCachedColumns 逻辑一致，便于读写同一份列配置 */
    mergeTableOptionsWithCache(list, storageKey) {
      if (!storageKey || !list.length) return list;
      let cached = this.$cache.local.getJSON(storageKey);
      if (cached == null) {
        const raw = this.$cache.local.get(storageKey);
        if (raw) {
          try {
            cached = typeof raw === "string" ? JSON.parse(raw) : raw;
          } catch (e) {
            cached = null;
          }
        }
      }
      if (!cached || !Array.isArray(cached) || cached.length === 0) {
        return list;
      }
      if (typeof cached[0] === "string") {
        const visibleKeys = new Set(cached);
        return list.map((d) => ({ ...d, visible: visibleKeys.has(d.key) }));
      }
      const defaultByKey = {};
      list.forEach((d) => {
        defaultByKey[d.key] = d;
      });
      const merged = [];
      const used = new Set();
      cached.forEach((c) => {
        const base = defaultByKey[c.key];
        if (!base) return;
        merged.push({
          ...base,
          fixed: !!c.fixed,
          visible: c.visible !== false,
        });
        used.add(c.key);
      });
      list.forEach((d) => {
        if (!used.has(d.key)) merged.push({ ...d });
      });
      return merged;
    },
    initExcelColumnOrderFromCache() {
      const allowed = COLS.filter((c) => c.key !== "projectNum").map((c) => c.key);
      const defaults = TableOptions.map((d) => ({ ...d }));
      const merged = this.mergeTableOptionsWithCache(defaults, DEFECT_TABLE_FIELD_LIST_STORAGE_KEY);
      const keys = [];
      const seen = new Set();
      merged.forEach((m) => {
        const tk = String(m.key);
        const col = COLS.find(
          (c) =>
            c.key !== "projectNum" &&
            c.titleKey != null &&
            c.titleKey !== "" &&
            String(c.titleKey) === tk
        );
        if (col && !seen.has(col.key)) {
          keys.push(col.key);
          seen.add(col.key);
        }
      });
      allowed.forEach((k) => {
        if (!seen.has(k)) keys.push(k);
      });
      const isDefault = keys.length === allowed.length && keys.every((k, i) => k === allowed[i]);
      this.excelDataColumnKeyOrder = isDefault ? null : keys;
    },
    /**
     * 将 Excel 数据列顺序写回与 Cat2BugTable 共用的 field-list 缓存，使表格视图 / 列选择器 / Excel 勾选列表顺序一致。
     */
    syncMergedTableFieldListOrderFromExcel(excelFieldKeysOrder) {
      const storageKey = DEFECT_TABLE_FIELD_LIST_STORAGE_KEY;
      const defaults = TableOptions.map((d) => ({ ...d }));
      const merged = this.mergeTableOptionsWithCache(defaults, storageKey);
      const pickSet = new Set(TableOptions.filter(isExcelDragReorderTableOption).map((c) => String(c.key)));

      const mergedByKey = {};
      merged.forEach((m) => {
        mergedByKey[String(m.key)] = { ...m };
      });

      const excelTableKeyOrder = (excelFieldKeysOrder || [])
        .map((ek) => {
          const col = COLS.find((c) => c.key === String(ek) && c.key !== "projectNum");
          return col && col.titleKey != null && col.titleKey !== "" ? String(col.titleKey) : null;
        })
        .filter(Boolean);

      const firstIdx = merged.findIndex((m) => pickSet.has(String(m.key)));
      if (firstIdx < 0) return;

      let lastIdx = -1;
      merged.forEach((m, i) => {
        if (pickSet.has(String(m.key))) lastIdx = i;
      });

      const before = merged.slice(0, firstIdx).map((m) => ({ ...m }));
      const after = merged.slice(lastIdx + 1).map((m) => ({ ...m }));

      const seenTk = new Set();
      const excelMerged = [];
      excelTableKeyOrder.forEach((tk) => {
        if (seenTk.has(tk)) return;
        seenTk.add(tk);
        const row = mergedByKey[tk];
        if (row) excelMerged.push({ ...row });
        else {
          const base = TableOptions.find((x) => String(x.key) === tk);
          if (base && pickSet.has(tk)) excelMerged.push({ ...base, visible: true, fixed: !!base.fixed });
        }
      });
      merged.forEach((m) => {
        const k = String(m.key);
        if (pickSet.has(k) && !seenTk.has(k)) {
          excelMerged.push({ ...m });
          seenTk.add(k);
        }
      });

      const newMerged = [...before, ...excelMerged, ...after];
      this.$cache.local.setJSON(storageKey, newMerged);
      this.excelColumnPickerOrderRev = (this.excelColumnPickerOrderRev || 0) + 1;
    },
    /**
     * 将宿主期望的数据列顺序同步到库内 fields[]（表头/表体/colgroup 均按 fields 渲染）。
     * 固定前导列为 defectId、projectNum，其余按 colsWithoutProjectNum。
     */
    applyExcelColumnOrderToEditorFields() {
      const ed = this.$refs.excelEditor;
      if (!ed || !Array.isArray(ed.fields)) return;
      const headNames = ["defectId", "projectNum"];
      const head = [];
      const rest = [];
      ed.fields.forEach((f) => {
        if (!f || !f.name) return;
        if (headNames.includes(f.name)) head.push(f);
        else rest.push(f);
      });
      head.sort((a, b) => headNames.indexOf(a.name) - headNames.indexOf(b.name));
      const order = this.colsWithoutProjectNum.map((c) => c.key);
      rest.sort((a, b) => {
        const ia = order.indexOf(a.name);
        const ib = order.indexOf(b.name);
        const ra = ia < 0 ? 9999 : ia;
        const rb = ib < 0 ? 9999 : ib;
        if (ra !== rb) return ra - rb;
        return String(a.name).localeCompare(String(b.name));
      });
      const next = [...head, ...rest];
      if (next.length !== ed.fields.length) return;
      ed.fields.splice(0, ed.fields.length, ...next);
      ed.fields.forEach((field) => {
        if (!field) return;
        if (field.name === "defectId") field.pos = 0;
        else if (field.name === "projectNum") field.pos = 1;
        else {
          const oi = order.indexOf(field.name);
          field.pos = oi >= 0 ? 10 + oi : 500;
        }
      });
      setTimeout(() => {
        const editor = this.$refs.excelEditor;
        if (!editor) return;
        if (typeof editor.calStickyLeft === "function") editor.calStickyLeft();
        this.scheduleExcelEditorLayoutFix();
        editor.$forceUpdate();
      }, 0);
    },
    findDefectExcelLabelHeaderTh(target) {
      const root = this.$refs.excelEditor && this.$refs.excelEditor.$el;
      if (!root || !target || typeof target.closest !== "function") return null;
      const el = target.closest("th");
      if (!el || el.tagName !== "TH") return null;
      if (!root.contains(el)) return null;
      const table = el.closest("table.systable");
      if (!table || !root.contains(table)) return null;
      const thead = el.closest("thead");
      if (!thead) return null;
      const firstTr = thead.querySelector("tr:first-child");
      const tr = el.closest("tr");
      if (!firstTr || tr !== firstTr) return null;
      if (el.classList.contains("first-col")) return null;
      return el;
    },
    getExcelHeaderFieldNameForTh(th) {
      const ed = this.$refs.excelEditor;
      if (!ed || !ed.labelTr || !th) return null;
      const tr = ed.labelTr;
      if (th.parentElement !== tr) return null;
      const idx = Array.from(tr.children).indexOf(th);
      if (idx <= 0) return null;
      const p = idx - 1;
      const f = ed.fields && ed.fields[p];
      return f && f.name ? String(f.name) : null;
    },
    excelReorderableFieldKeys() {
      return this.colsWithoutProjectNum.map((c) => c.key);
    },
    teardownExcelColumnHeaderDragSession() {
      const dragSession = this._excelColDrag;
      if (dragSession && dragSession._colDragHighlightRaf != null) {
        cancelAnimationFrame(dragSession._colDragHighlightRaf);
        dragSession._colDragHighlightRaf = null;
      }
      if (this._excelColDragBoundMove) {
        window.removeEventListener("mousemove", this._excelColDragBoundMove, true);
        this._excelColDragBoundMove = null;
      }
      if (this._excelColDragBoundUp) {
        window.removeEventListener("mouseup", this._excelColDragBoundUp, true);
        this._excelColDragBoundUp = null;
      }
      this._excelColDrag = null;
      this.excelColDragActive = false;
      const inner = this.$refs.excelColDragGhostInnerEl;
      if (inner) inner.innerHTML = "";
      this.excelColDragGhost = { show: false };
      this.clearExcelColumnDropThHighlight();
    },
    clearExcelColumnDropThHighlight() {
      this._excelColDropOverlayRectKey = null;
      const ov = this.$refs.excelColDropHighlightOverlay;
      if (ov) ov.style.display = "none";
    },
    /** 用 fixed 蒙层对齐目标 th 的 getBoundingClientRect，不修改表格内节点，列宽不受影响 */
    positionExcelColumnDropHighlightOverlay(th) {
      const ov = this.$refs.excelColDropHighlightOverlay;
      if (!ov || !th || typeof th.getBoundingClientRect !== "function") return;
      const r = th.getBoundingClientRect();
      const key = `${Math.round(r.left)},${Math.round(r.top)},${Math.round(r.width)},${Math.round(r.height)}`;
      if (this._excelColDropOverlayRectKey === key) return;
      this._excelColDropOverlayRectKey = key;
      ov.style.display = "block";
      ov.style.position = "fixed";
      ov.style.left = `${Math.round(r.left)}px`;
      ov.style.top = `${Math.round(r.top)}px`;
      ov.style.width = `${Math.max(0, Math.round(r.width))}px`;
      ov.style.height = `${Math.max(0, Math.round(r.height))}px`;
      ov.style.zIndex = "3998";
      ov.style.pointerEvents = "none";
      ov.style.boxSizing = "border-box";
      /* 半透明叠在表头上，标题/红字仍可读；勿再叠一层 opacity 否则字被糊住 */
      ov.style.background = "rgba(217, 236, 255, 0.58)";
      ov.style.opacity = "1";
      ov.style.boxShadow = "none";
      ov.style.border = "none";
      ov.style.borderRadius = "0";
    },
    /**
     * 将 fromKey 移到 hoverKey 的左侧(insertAfter=false)或右侧(insertAfter=true)，与竖条语义一致。
     */
    reorderExcelColumnKeys(keys, fromKey, hoverKey, insertAfter) {
      const from = keys.indexOf(fromKey);
      const to = keys.indexOf(hoverKey);
      if (from < 0 || to < 0) return null;
      const next = keys.slice();
      next.splice(from, 1);
      let to2 = to;
      if (from < to) to2 = to - 1;
      let ins = insertAfter ? to2 + 1 : to2;
      if (ins < 0) ins = 0;
      if (ins > next.length) ins = next.length;
      next.splice(ins, 0, fromKey);
      return next;
    },
    /** 鼠标指针下的可拖数据列表头 th 浅蓝高亮（与 Cat2BugTable table-header-ghost 一致）；同时写入 drop 语义供 mouseup */
    updateExcelColumnDropThHighlight(e, d) {
      const allowed = new Set(this.excelReorderableFieldKeys());
      const raw = document.elementFromPoint(e.clientX, e.clientY);
      const th = raw && typeof raw.closest === "function" ? this.findDefectExcelLabelHeaderTh(raw) : null;
      if (!th) {
        this.clearExcelColumnDropThHighlight();
        d.dropHoverKey = null;
        d.dropInsertAfter = false;
        return;
      }
      const hoverKey = this.getExcelHeaderFieldNameForTh(th);
      if (!hoverKey || !allowed.has(hoverKey)) {
        this.clearExcelColumnDropThHighlight();
        d.dropHoverKey = null;
        d.dropInsertAfter = false;
        return;
      }
      const rect = th.getBoundingClientRect();
      d.dropHoverKey = hoverKey;
      d.dropInsertAfter = e.clientX >= rect.left + rect.width / 2;
      this.positionExcelColumnDropHighlightOverlay(th);
    },
    applyExcelColDragGhostLayout(e, d) {
      const el = this.$refs.excelColDragGhostEl;
      if (!el) return;
      const w = d.ghostWidth != null ? d.ghostWidth : 100;
      const ox = d.ghostOffsetX != null ? d.ghostOffsetX : 0;
      const oy = d.ghostOffsetY != null ? d.ghostOffsetY : 0;
      el.style.left = Math.round(e.clientX - ox) + "px";
      el.style.top = Math.round(e.clientY - oy) + "px";
      el.style.width = w + "px";
      el.style.minWidth = w + "px";
      el.style.maxWidth = w + "px";
    },
    scheduleExcelColumnDropHighlightRaf(e, d) {
      d._lastHighlightMoveEvent = e;
      if (d._colDragHighlightRaf != null) return;
      d._colDragHighlightRaf = requestAnimationFrame(() => {
        d._colDragHighlightRaf = null;
        const ev = d._lastHighlightMoveEvent;
        if (!this._excelColDrag || !this._excelColDrag.dragging || !ev) return;
        this.updateExcelColumnDropThHighlight(ev, this._excelColDrag);
      });
    },
    onExcelColumnHeaderDragMove(e) {
      const d = this._excelColDrag;
      if (!d) return;
      const dx = e.clientX - d.startX;
      const dy = e.clientY - d.startY;
      if (!d.dragging && dx * dx + dy * dy >= EXCEL_HEADER_COL_DRAG_THRESHOLD_PX * EXCEL_HEADER_COL_DRAG_THRESHOLD_PX) {
        d.dragging = true;
        this.excelColDragActive = true;
        this.excelColDragGhost = { show: true };
        d._ghostDomInited = false;
      }
      if (d.dragging) {
        if (!d._ghostDomInited) {
          d._ghostDomInited = true;
          this.$nextTick(() => {
            if (!this._excelColDrag || !this._excelColDrag.dragging) return;
            const d2 = this._excelColDrag;
            const inner = this.$refs.excelColDragGhostInnerEl;
            if (inner) inner.innerHTML = d2.ghostHtml || "";
            this.applyExcelColDragGhostLayout(e, d2);
            this.updateExcelColumnDropThHighlight(e, d2);
          });
          this.applyExcelColDragGhostLayout(e, d);
          this.updateExcelColumnDropThHighlight(e, d);
        } else {
          this.applyExcelColDragGhostLayout(e, d);
          this.scheduleExcelColumnDropHighlightRaf(e, d);
        }
      }
    },
    onExcelColumnHeaderDragUp(e) {
      const d = this._excelColDrag;
      const dropHoverKey = d && d.dropHoverKey;
      const dropInsertAfter = d && !!d.dropInsertAfter;
      const fromKey = d && d.fromKey;
      const dragging = d && d.dragging;
      this.teardownExcelColumnHeaderDragSession();
      if (!dragging || !fromKey) return;
      const allowed = new Set(this.excelReorderableFieldKeys());
      let hoverKey = dropHoverKey;
      let insertAfter = dropInsertAfter;
      if (!hoverKey || !allowed.has(hoverKey)) {
        const raw = document.elementFromPoint(e.clientX, e.clientY);
        const th = raw && typeof raw.closest === "function" ? this.findDefectExcelLabelHeaderTh(raw) : null;
        hoverKey = th ? this.getExcelHeaderFieldNameForTh(th) : null;
        if (th && hoverKey && allowed.has(hoverKey) && hoverKey !== fromKey) {
          const rect = th.getBoundingClientRect();
          insertAfter = e.clientX >= rect.left + rect.width / 2;
        } else {
          hoverKey = null;
        }
      }
      if (!hoverKey || !allowed.has(hoverKey) || hoverKey === fromKey) return;
      const keys = this.colsWithoutProjectNum.map((c) => c.key);
      const nextKeys = this.reorderExcelColumnKeys(keys, fromKey, hoverKey, insertAfter);
      if (!nextKeys || nextKeys.join("\0") === keys.join("\0")) return;
      this.excelDataColumnKeyOrder = nextKeys;
      this.syncMergedTableFieldListOrderFromExcel(nextKeys);
      this.$nextTick(() => {
        this.applyExcelColumnOrderToEditorFields();
        this.applyExcelColumnVisibilityToEditorFields();
      });
    },
    tryBeginExcelColumnHeaderDrag(e) {
      if (e.button !== 0) return false;
      if (e.target && typeof e.target.closest === "function" && e.target.closest(".col-sep")) return false;
      const th = this.findDefectExcelLabelHeaderTh(e.target);
      if (!th) return false;
      const fromKey = this.getExcelHeaderFieldNameForTh(th);
      const allowed = this.excelReorderableFieldKeys();
      if (!fromKey || !allowed.includes(fromKey)) return false;
      e.preventDefault();
      e.stopPropagation();
      this.teardownExcelColumnHeaderDragSession();
      const ghostHtml = this.defectExcelHeaderLabel("", { name: fromKey });
      const thRect = typeof th.getBoundingClientRect === "function" ? th.getBoundingClientRect() : null;
      const ghostWidth = thRect && thRect.width > 0 ? Math.max(40, Math.round(thRect.width)) : 100;
      /* 按下点相对表头格左上角：拖动时保持该点与鼠标重合 */
      let ghostOffsetX = 0;
      let ghostOffsetY = 0;
      if (thRect && thRect.width > 0 && thRect.height > 0) {
        ghostOffsetX = e.clientX - thRect.left;
        ghostOffsetY = e.clientY - thRect.top;
      } else {
        ghostOffsetX = Math.round(ghostWidth / 2);
        ghostOffsetY = 16;
      }
      this._excelColDrag = {
        fromKey,
        startX: e.clientX,
        startY: e.clientY,
        dragging: false,
        ghostHtml,
        ghostWidth,
        ghostOffsetX,
        ghostOffsetY,
        dropHoverKey: null,
        dropInsertAfter: false,
      };
      this._excelColDragBoundMove = this.onExcelColumnHeaderDragMove.bind(this);
      this._excelColDragBoundUp = this.onExcelColumnHeaderDragUp.bind(this);
      window.addEventListener("mousemove", this._excelColDragBoundMove, true);
      window.addEventListener("mouseup", this._excelColDragBoundUp, true);
      return true;
    },
    initExcelColumnPickerFromCache() {
      const merged = this.mergeTableOptionsWithCache(
        TableOptions.map((d) => ({ ...d })),
        DEFECT_TABLE_FIELD_LIST_STORAGE_KEY
      );
      const pickerKeys = new Set(this.defaultExcelColumnPickerKeys.map((k) => String(k)));
      const checked = merged
        .filter((c) => c.visible && pickerKeys.has(String(c.key)))
        .map((c) => String(c.key));
      const all = this.defaultExcelColumnPickerKeys.map((k) => String(k));
      this.excelColumnPickerCheckedKeys = checked.length ? checked : [...all];
    },
    /** key 为 COLS 字段名（如 defectType），勾选存的是 TableOptions.key（如 type）；projectNum 由「编号」id 控制 */
    excelColumnVisible(excelFieldKey) {
      const col = COLS.find((c) => c.key === excelFieldKey);
      if (!col) return true;
      if (col.key === "projectNum") {
        const picked = this.excelColumnPickerCheckedKeys || [];
        return picked.some((k) => String(k) === "id");
      }
      const tk = col.titleKey;
      if (tk == null || tk === "") return true;
      const need = String(tk);
      const picked = this.excelColumnPickerCheckedKeys || [];
      return picked.some((k) => String(k) === need);
    },
    /**
     * 与 vue-excel-editor 内置 `PanelSetting.columnLabelClick` 相同：直接修改编辑器 `fields` 里每项的 `invisible`，
     * 再 `calStickyLeft`（库内用 setTimeout 包一层）。VueExcelColumn 只在 created 里把 invisible 快照进 fields，
     * 仅靠模板改 `:invisible` 不会同步到内部 field，列选择器变更必须走本方法。
     */
    applyExcelColumnVisibilityToEditorFields() {
      const ed = this.$refs.excelEditor;
      if (!ed || !Array.isArray(ed.fields)) return;
      ed.fields.forEach((f) => {
        if (!f || !f.name) return;
        if (f.name === "defectId") {
          f.invisible = true;
          return;
        }
        if (f.name === "projectNum") {
          const picked = this.excelColumnPickerCheckedKeys || [];
          f.invisible = !picked.some((k) => String(k) === "id");
          return;
        }
        const col = COLS.find((c) => c.key === f.name);
        if (!col) return;
        const tk = col.titleKey;
        if (tk == null || tk === "") {
          f.invisible = false;
          return;
        }
        const need = String(tk);
        const picked = this.excelColumnPickerCheckedKeys || [];
        f.invisible = !picked.some((k) => String(k) === need);
      });
      /* 与 PanelSetting.columnLabelClick 一致，仅 calStickyLeft；布局夹紧仍由 scheduleExcelEditorLayoutFix 负责 */
      setTimeout(() => {
        const editor = this.$refs.excelEditor;
        if (!editor) return;
        if (typeof editor.calStickyLeft === "function") editor.calStickyLeft();
        this.scheduleExcelEditorLayoutFix();
      }, 0);
    },
    /**
     * vue-excel-editor 按 pos 插入 fields；全部默认 0 时新列会追加到末尾，fields 顺序与表头 DOM 不一致会错位/“列不显示”。
     * 与 COLS 中数据列顺序一致（不含编号列）。
     */
    excelSheetDataColumnPos(c) {
      const idx = this.colsWithoutProjectNum.findIndex((x) => x.key === c.key);
      return 10 + (idx < 0 ? 0 : idx);
    },
    persistDefectTableFieldListFromExcelPicker(nextVisibleTableKeys) {
      const storageKey = DEFECT_TABLE_FIELD_LIST_STORAGE_KEY;
      const defaults = TableOptions.map((d) => ({ ...d }));
      const merged = this.mergeTableOptionsWithCache(defaults, storageKey);
      const pickSet = new Set(this.excelTableColumnPickerOptions.map((c) => String(c.key)));
      const next = (nextVisibleTableKeys || []).map((k) => String(k));
      const nextSet = new Set(next);
      const updated = merged.map((col) => {
        if (pickSet.has(String(col.key))) {
          return { ...col, visible: nextSet.has(String(col.key)) };
        }
        return { ...col };
      });
      this.$cache.local.setJSON(storageKey, updated);
    },
    onExcelColumnPickerChange(keys) {
      const all = this.defaultExcelColumnPickerKeys.map((k) => String(k));
      const next = (keys || []).map((k) => String(k)).filter((k) => all.includes(k));
      if (!next.length) {
        this.$modal.msgWarning(String(this.$t("defect.excel-column-at-least-one")));
        this.$nextTick(() => {
          this.excelColumnPickerCheckedKeys = [...all];
          this.applyExcelColumnVisibilityToEditorFields();
        });
        return;
      }
      this.excelColumnPickerCheckedKeys = next;
      this.persistDefectTableFieldListFromExcelPicker(next);
      this.$nextTick(() => {
        this.applyExcelColumnVisibilityToEditorFields();
        this.updateExcelEditorHeight();
      });
    },
    /**
     * vue-excel-editor 在 fields 变更、列宽拖拽结束等时机 $emit('setting', getSetting())；
     * 把 fields[].invisible 同步回与表格视图共用的列缓存（同 vue3-excel-editor PanelSetting 行为）。
     */
    onExcelEditorSettingFromLibrary(payload) {
      if (!payload || !Array.isArray(payload.fields)) return;
      /* 列宽拖拽结束库先 emit：立刻写回 fields 并落盘，避免随后 layout 用旧 width 绑回 col 导致弹回 */
      this.applyExcelEditorWidthsFromSettingToFields(payload);
      this.persistExcelColumnWidthsToCache(payload);
      if (this.excelSettingSyncTimer) clearTimeout(this.excelSettingSyncTimer);
      this.excelSettingSyncTimer = setTimeout(() => {
        this.excelSettingSyncTimer = null;
        this.syncExcelColumnPickerFromEditorSetting(payload);
      }, 60);
      /* 拖拽期间跳过的 layout，在 sep 已清除、宽度已写回 fields 后再跑一次 */
      this.$nextTick(() => this.scheduleExcelEditorLayoutFix());
    },
    syncExcelColumnPickerFromEditorSetting(payload) {
      const list = payload.fields;
      const all = this.defaultExcelColumnPickerKeys.map((k) => String(k));
      const pickerKeySet = new Set(all);
      const visibleTk = [];
      (list || []).forEach((sf) => {
        if (!sf) return;
        const name = sf.name;
        if (name === "defectId") return;
        /* projectNum 对应显示字段里的「编号」TableOptions.key === id，不能跳过否则勾选会被 @setting 同步冲掉 */
        if (name === "projectNum") {
          if (!sf.invisible && pickerKeySet.has("id")) visibleTk.push("id");
          return;
        }
        if (sf.invisible) return;
        const col = COLS.find((c) => c.key === name);
        if (!col || col.titleKey == null || col.titleKey === "") return;
        const tk = String(col.titleKey);
        if (pickerKeySet.has(tk)) visibleTk.push(tk);
      });
      const vis = new Set(visibleTk);
      const nextKeys = all.filter((k) => vis.has(k));
      const finalKeys = nextKeys.length ? nextKeys : [...all];
      const norm = (arr) =>
        [...(arr || [])]
          .map(String)
          .sort()
          .join("|");
      if (norm(finalKeys) === norm(this.excelColumnPickerCheckedKeys)) return;
      this.excelColumnPickerCheckedKeys = finalKeys;
      this.persistDefectTableFieldListFromExcelPicker(finalKeys);
    },
    handleExcelToolbarRefresh() {
      this.search(this.queryParams);
    },
    init() {
      /* 数据由父组件 search(queryParams) 拉取，与表格视图一致 */
    },
    columnLabel(c) {
      return String(this.$t(c.titleKey));
    },
    /** 展示为 #编号，底层仍为数字便于与接口一致 */
    projectNumToText(val) {
      if (val == null || val === "") return "";
      return "#" + String(val);
    },
    /** 状态列：批量创建排队/进行中哨兵转文案；其余原样展示 */
    defectStateTextToText(val) {
      if (val === EXCEL_CREATE_ROW_QUEUED) return String(this.$t("defect.excel-create-queued"));
      if (val === EXCEL_CREATE_ROW_RUNNING) return String(this.$t("defect.excel-create-running"));
      if (val == null || val === "") return "";
      return String(val);
    },
    /**
     * map 列若用原样 to-text，格内会显示 id/字典 value 而非文案；区域复制也会带上 id，粘贴时与 toValue(按标签反查) 对不上。
     */
    excelColumnToText(c) {
      if (c && c.key === "defectStateText") return this.excelDefectStateCellToText;
      if (c && c.key === "defectType") return this.excelDefectTypeToText;
      if (c && c.key === "defectLevel") return this.excelDefectLevelToText;
      if (c && c.key === "excelHandleByMemberId") return this.excelHandleByMemberIdToText;
      return this.excelColumnToTextPassthrough;
    },
    excelColumnToTextPassthrough(v) {
      return v == null ? "" : v;
    },
    excelDefectTypeToText(v) {
      if (v == null || v === "") return "";
      const m = this.defectTypeSelectMap;
      if (m && Object.prototype.hasOwnProperty.call(m, v)) return String(m[v]);
      const sk = String(v);
      if (m && Object.prototype.hasOwnProperty.call(m, sk)) return String(m[sk]);
      return String(v);
    },
    excelDefectLevelToText(v) {
      if (v == null || v === "") return "";
      const m = this.defectLevelSelectMap;
      if (m && Object.prototype.hasOwnProperty.call(m, v)) return String(m[v]);
      const sk = String(v);
      if (m && Object.prototype.hasOwnProperty.call(m, sk)) return String(m[sk]);
      return String(v);
    },
    excelHandleByMemberIdToText(v) {
      if (v == null || v === "") return "";
      const m = this.memberSelectMap;
      const sk = String(v);
      if (m && Object.prototype.hasOwnProperty.call(m, sk)) return String(m[sk]);
      return String(v);
    },
    excelDefectStateCellToText(val) {
      if (val === EXCEL_CREATE_ROW_QUEUED || val === EXCEL_CREATE_ROW_RUNNING) return this.defectStateTextToText(val);
      return this.excelDefectStateToText(val);
    },
    excelDefectStateToText(v) {
      if (v == null || v === "") return "";
      const sk = String(v);
      if (sk === "2") return String(this.$t("RESOLVED") || "RESOLVED");
      const m = this.defectStateSelectMap;
      if (m && Object.prototype.hasOwnProperty.call(m, sk)) return String(m[sk]);
      return String(v);
    },
    /** 仅 map 列覆盖 toValue：支持粘贴展示名或 key(id)；非 map 不传以保持库默认 */
    excelColumnValueBind(c) {
      if (!c || c.fieldType !== "map") return {};
      if (c.key === "defectLevel") return { toValue: this.excelToValueDefectLevel };
      if (c.key === "excelHandleByMemberId") return { toValue: this.excelToValueHandleByMemberId };
      if (c.key === "defectType") return { toValue: this.excelToValueDefectType };
      if (c.key === "defectStateText") return { toValue: this.excelToValueDefectState };
      return {};
    },
    excelToValueDefectLevel(text) {
      return this.excelMapFieldToValue(text, this.defectLevelSelectMap, (k) => this.coerceDefectLevelValue(k));
    },
    excelToValueHandleByMemberId(text) {
      return this.excelMapFieldToValue(text, this.memberSelectMap, (k) => (k == null || k === "" ? "" : String(k)));
    },
    excelToValueDefectType(text) {
      return this.excelMapFieldToValue(text, this.defectTypeSelectMap, (k) => k);
    },
    excelToValueDefectState(text) {
      const raw = text == null ? "" : String(text).trim();
      if (raw === "2" || raw === "RESOLVED" || raw === String(this.$t("RESOLVED"))) {
        return "2";
      }
      return this.excelMapFieldToValue(text, this.defectStateSelectMap, (k) => (k == null || k === "" ? "" : String(k)));
    },
    /**
     * 将粘贴文本解析为 map 存库 key：先按 options 文案精确匹配，再按 key 本身（与复制出的 id 一致）。
     */
    excelMapFieldToValue(text, optionsObj, normalizeKey) {
      const raw = text == null ? "" : String(text).trim();
      if (raw === "") return "";
      const opts = optionsObj || {};
      const keys = Object.keys(opts);
      for (let i = 0; i < keys.length; i++) {
        const k = keys[i];
        const label = opts[k];
        if (label != null && String(label) === raw) {
          return normalizeKey ? normalizeKey(k) : k;
        }
      }
      if (Object.prototype.hasOwnProperty.call(opts, raw)) {
        return normalizeKey ? normalizeKey(raw) : raw;
      }
      const rawStr = String(raw);
      for (let j = 0; j < keys.length; j++) {
        const k2 = keys[j];
        if (String(k2) === rawStr) {
          return normalizeKey ? normalizeKey(k2) : k2;
        }
      }
      return raw;
    },
    defectExcelCellStyle(record, item) {
      if (!item || !record) return {};
      if (item.name === "planStartTime" || item.name === "planEndTime" || item.name === "moduleName") {
        return Object.assign({}, PLAN_TIME_SELECT_TRIANGLE_BG);
      }
      return {};
    },
    bindPlanTimeTriZoneMouseDown() {
      this.unbindPlanTimeTriZoneMouseDown();
      const root = this.$refs.excelEditor && this.$refs.excelEditor.$el;
      if (!root || typeof root.addEventListener !== "function") return;
      this._planTimeExcelEditorRootEl = root;
      this._planTimeTriZoneMouseDown = this.onPlanTimeTriZoneMouseDown.bind(this);
      root.addEventListener("mousedown", this._planTimeTriZoneMouseDown, true);
    },
    unbindPlanTimeTriZoneMouseDown() {
      const root = this._planTimeExcelEditorRootEl || (this.$refs.excelEditor && this.$refs.excelEditor.$el);
      if (root && this._planTimeTriZoneMouseDown) {
        root.removeEventListener("mousedown", this._planTimeTriZoneMouseDown, true);
      }
      this._planTimeTriZoneMouseDown = null;
      this._planTimeExcelEditorRootEl = null;
    },
    /**
     * 与 vue-excel-editor `tdDropdownHotZoneHit` 一致：仅右侧三角热区打开计划时间弹层；
     * capture 阶段命中三角时 stopPropagation，避免库内 mousedown 抢焦点。
     */
    onPlanTimeTriZoneMouseDown(e) {
      let t = e.target;
      while (t && t.nodeType !== 1) t = t.parentNode;
      if (!t || t.tagName !== "TD" || !t.id) return;
      if (t.classList.contains("first-col")) return;
      let fieldName = null;
      if (t.id.endsWith("-planStartTime")) fieldName = "planStartTime";
      else if (t.id.endsWith("-planEndTime")) fieldName = "planEndTime";
      else if (t.id.endsWith("-moduleName")) fieldName = "moduleName";
      else return;
      const ed = this.$refs.excelEditor;
      if (!ed || typeof ed.tdDropdownHotZoneHit !== "function" || !ed.tdDropdownHotZoneHit(t, e.clientX)) return;
      const suf = `-${fieldName}`;
      if (!t.id.startsWith("id-") || !t.id.endsWith(suf)) return;
      const rowId = t.id.slice(3, t.id.length - suf.length);
      const rec = (this.sheetRows || []).find((r) => String(r.$id) === rowId);
      if (!rec) return;
      const field = ed.fields && ed.fields.find((f) => f.name === fieldName);
      if (!field) return;
      e.preventDefault();
      e.stopPropagation();
      if (fieldName === "moduleName") {
        this.openExcelModulePickerFromTriangle(rec);
        return;
      }
      const text = rec[fieldName];
      this.handlePlanTimeCellClick(text, rec, 0, 0, field);
    },
    /** 点击计划时间列右侧三角热区：在格下方展示选择器并 focus，弹出 Element 日期时间下拉 */
    handlePlanTimeCellClick(text, record, _rowPos, _colPos, field) {
      if (!record || !field) return;
      this.closeExcelModulePicker();
      if (this.planTimeOpenTimer) clearTimeout(this.planTimeOpenTimer);
      this.planTimeDialogRow = record;
      this.planTimeDialogField = field.name;
      const raw = text == null ? "" : String(text).trim();
      this.planTimeDialogValue = raw || null;
      this.planTimeDialogOldValue = raw;
      this.positionPlanTimePickerNearCell(record, field.name);
      this.planTimePickerVisible = true;
      /* excel 在 mouseDown 里 moveInputSquare → inputBox.focus，另有 setTimeout(0) 再 focus；须在其后 blur 再打开面板 */
      this.$nextTick(() => {
        this.planTimeOpenTimer = setTimeout(() => {
          this.planTimeOpenTimer = null;
          if (!this.planTimePickerVisible) return;
          const ed = this.$refs.excelEditor;
          if (ed && ed.inputBox) ed.inputBox.blur();
          const dp = this.$refs.planTimeDatePicker;
          if (dp && typeof dp.focus === "function") {
            dp.focus();
            setTimeout(() => this.attachPlanTimePanelFooterClear(), 280);
          }
        }, 150);
      });
    },
    /** 下拉面板挂到 body，在底部栏插入「清除」（与 Element 此刻/确定并列） */
    attachPlanTimePanelFooterClear() {
      if (!this.planTimePickerVisible) return;
      const footer = document.querySelector(
        ".defect-excel-plan-datetime-panel .el-picker-panel__footer"
      );
      if (!footer || footer.querySelector(".defect-excel-plan-footer-clear")) return;
      const wrap = document.createElement("span");
      wrap.className = "defect-excel-plan-footer-clear";
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "el-button el-button--text el-button--mini";
      btn.textContent = String(this.$t("clear"));
      btn.addEventListener("click", (ev) => {
        ev.preventDefault();
        ev.stopPropagation();
        if (this.planTimePickerSaving) return;
        /* 只改 v-model，保存走 @change → onPlanTimePickerChange；勿再手动 await 一次，否则会二次 updateDefect 触发防重复提交 */
        this.planTimeDialogValue = null;
        this.$nextTick(() => {
          const dp = this.$refs.planTimeDatePicker;
          if (dp) {
            if (typeof dp.handleClose === "function") dp.handleClose();
            else dp.pickerVisible = false;
          }
        });
      });
      wrap.appendChild(btn);
      footer.insertBefore(wrap, footer.firstChild);
    },
    positionPlanTimePickerNearCell(record, fieldName) {
      const id = `id-${record.$id}-${fieldName}`;
      const el = document.getElementById(id);
      const pad = 2;
      let top = "120px";
      let left = "120px";
      if (el && el.getBoundingClientRect) {
        const rect = el.getBoundingClientRect();
        top = `${rect.bottom + pad}px`;
        left = `${rect.left}px`;
      }
      this.planTimePickerAnchorStyle = {
        position: "fixed",
        top,
        left,
        zIndex: 5000,
      };
    },
    positionExcelModulePickerNearCell(record) {
      const id = `id-${record.$id}-moduleName`;
      const el = document.getElementById(id);
      const pad = 2;
      let top = "120px";
      let left = "120px";
      if (el && el.getBoundingClientRect) {
        const rect = el.getBoundingClientRect();
        top = `${rect.bottom + pad}px`;
        left = `${rect.left}px`;
      }
      this.excelModulePickerAnchorStyle = {
        position: "fixed",
        top,
        left,
        zIndex: 5001,
        minWidth: "220px",
      };
    },
    removeExcelModulePickerOutsideClose() {
      if (this._excelModulePickerOutsideClose) {
        document.removeEventListener("mousedown", this._excelModulePickerOutsideClose, true);
        this._excelModulePickerOutsideClose = null;
      }
    },
    closeExcelModulePicker() {
      this.removeExcelModulePickerOutsideClose();
      this.excelModulePickerVisible = false;
      this.excelModulePickerRow = null;
      this.excelModulePickerModuleId = null;
      this.excelModulePickerProjectId = null;
      this.excelModulePickerAnchorStyle = {};
    },
    attachExcelModulePickerOutsideClose() {
      this.removeExcelModulePickerOutsideClose();
      this._excelModulePickerOutsideClose = (ev) => {
        if (!this.excelModulePickerVisible) return;
        const anchor = this.$refs.excelModulePickerAnchor;
        const t = ev.target;
        /* 勿用 querySelector('.select-module-popover')：页面上另有交付物下拉时取到的是第一个 DOM，误判为外部点击，mousedown 捕获阶段即关掉弹层，有子节点时无法展开 */
        if (t && typeof t.closest === "function" && t.closest(".defect-excel-module-picker-popper")) return;
        if (anchor && anchor.contains(t)) return;
        this.closeExcelModulePicker();
      };
      document.addEventListener("mousedown", this._excelModulePickerOutsideClose, true);
    },
    openExcelModulePickerFromTriangle(record) {
      if (!record) return;
      this.planTimePickerVisible = false;
      this.resetPlanTimePickerState();
      this.closeExcelModulePicker();
      this.excelModulePickerRow = record;
      const pid =
        record.projectId != null && record.projectId !== ""
          ? Number(record.projectId)
          : this.queryParams && this.queryParams.projectId != null
            ? Number(this.queryParams.projectId)
            : null;
      this.excelModulePickerProjectId = Number.isFinite(pid) ? pid : null;
      if (this.excelModulePickerProjectId == null || this.excelModulePickerProjectId <= 0) return;
      const mid =
        record.moduleId != null && record.moduleId !== "" && Number.isFinite(Number(record.moduleId))
          ? Number(record.moduleId)
          : null;
      this.excelModulePickerModuleId = mid;
      this.positionExcelModulePickerNearCell(record);
      this.excelModulePickerVisible = true;
      const ed = this.$refs.excelEditor;
      if (ed && ed.inputBox) ed.inputBox.blur();
      this.$nextTick(() => {
        /* 勿延迟：有 moduleId 时 mounted 里 getModule 若晚于 click 曾会把 popover 关掉造成闪烁；见 SelectModule applyModuleDataWithoutClosingPopover */
        setTimeout(() => {
          const sm = this.$refs.excelModuleSelect;
          if (!sm) return;
          if (typeof sm.openDirectMenu === "function") sm.openDirectMenu();
          else {
            const el = sm.$el && sm.$el.querySelector(".select-module-input");
            if (el) el.click();
          }
          this.attachExcelModulePickerOutsideClose();
        }, 0);
      });
    },
    onExcelModuleSelectInput(a, b) {
      if (this.excelModulePickerSaving) return;
      if (b != null && typeof b === "object") {
        void this.applyExcelModulePickFromPicker(a, b);
        return;
      }
      if (arguments.length === 1 && (a == null || a === "")) {
        void this.applyExcelModulePickFromPicker(null, null);
      }
    },
    async applyExcelModulePickFromPicker(moduleId, mod) {
      const row = this.excelModulePickerRow;
      if (!row) {
        this.closeExcelModulePicker();
        return;
      }
      if (this.excelModulePickerSaving) return;
      if (this.isDeletedDefectRow(row)) {
        this.notifyDeletedDefectCannotEdit();
        this.closeExcelModulePicker();
        return;
      }
      const newId =
        moduleId != null && moduleId !== "" && Number.isFinite(Number(moduleId)) ? Number(moduleId) : null;
      const newName = mod && mod.moduleName != null ? String(mod.moduleName) : "";
      const oldId =
        row.moduleId != null && row.moduleId !== "" && Number.isFinite(Number(row.moduleId))
          ? Number(row.moduleId)
          : null;
      const oldName = row.moduleName != null ? String(row.moduleName) : "";
      if (oldId === newId && oldName === newName) {
        this.closeExcelModulePicker();
        return;
      }
      this.$set(row, "moduleId", newId);
      this.$set(row, "moduleName", newName);
      this.closeExcelModulePicker();
      if (!row.defectId) {
        this.refreshExcelEditorView();
        this.$modal.msgWarning(String(this.$t("defect.excel-plan-time-staged-row")));
        return;
      }
      this.excelModulePickerSaving = true;
      this.syncing = true;
      try {
        const payload = {
          defectId: row.defectId,
          projectId: row.projectId,
        };
        if (newId != null) payload.moduleId = newId;
        else payload.params = { clearModuleId: true };
        await updateDefect(payload);
        this.touchRowUpdateTime(row);
        this.$modal.msgSuccess(this.$t("defect.excel-save-success").toString());
        this.refreshExcelEditorView();
      } catch (e) {
        this.$set(row, "moduleId", oldId);
        this.$set(row, "moduleName", oldName);
        this.$modal.msgError(this.$t("defect.excel-save-failed").toString());
      } finally {
        this.excelModulePickerSaving = false;
        this.syncing = false;
      }
    },
    resetPlanTimePickerState() {
      if (this.planTimeOpenTimer) {
        clearTimeout(this.planTimeOpenTimer);
        this.planTimeOpenTimer = null;
      }
      this.planTimeDialogRow = null;
      this.planTimeDialogField = "";
      this.planTimeDialogValue = null;
      this.planTimeDialogOldValue = "";
      this.planTimePickerAnchorStyle = {};
    },
    onPlanTimePickerBlur() {
      this.planTimePickerVisible = false;
      this.$nextTick(() => {
        if (!this.syncing) this.resetPlanTimePickerState();
      });
    },
    /** 面板关闭且值相对打开时有变化时由 Element 触发（与 EditDefect 一致） */
    async onPlanTimePickerChange(val) {
      const row = this.planTimeDialogRow;
      const key = this.planTimeDialogField;
      const oldVal = this.planTimeDialogOldValue;
      if (!row || !key) {
        this.planTimePickerVisible = false;
        this.resetPlanTimePickerState();
        return;
      }
      const nv =
        val != null && val !== "" ? String(val).trim() : "";
      if (nv === oldVal) {
        this.planTimePickerVisible = false;
        this.resetPlanTimePickerState();
        return;
      }
      if (this.planTimePickerSaving) return;
      if (this.isDeletedDefectRow(row)) {
        this.notifyDeletedDefectCannotEdit();
        this.planTimePickerVisible = false;
        this.resetPlanTimePickerState();
        return;
      }
      this.planTimePickerSaving = true;
      this.syncing = true;
      try {
        if (!row.defectId) {
          /* 占位行无缺陷 ID：仅更新表格展示，刷新页面前不会落库 */
          this.$set(row, key, nv);
          this.refreshExcelEditorView();
          this.$modal.msgWarning(String(this.$t("defect.excel-plan-time-staged-row")));
        } else {
          const payload = {
            defectId: row.defectId,
            projectId: row.projectId,
          };
          if (nv) {
            payload[key] = nv;
          } else {
            /* 后端动态 SQL 对 null 不 SET 计划时间列，须用 params 显式清空 */
            payload.params = {};
            if (key === "planStartTime") payload.params.clearPlanStartTime = true;
            else if (key === "planEndTime") payload.params.clearPlanEndTime = true;
          }
          await updateDefect(payload);
          this.$set(row, key, nv);
          this.touchRowUpdateTime(row);
          this.refreshExcelEditorView();
          this.$modal.msgSuccess(this.$t("defect.excel-save-success").toString());
        }
      } catch (e) {
        this.$modal.msgError(this.$t("defect.excel-save-failed").toString());
        this.planTimeDialogValue = oldVal || null;
      } finally {
        this.syncing = false;
        this.planTimePickerSaving = false;
        this.planTimePickerVisible = false;
        this.resetPlanTimePickerState();
      }
    },
    /**
     * 表头文案始终按列 field.name → COLS.titleKey 用当前语言 $t；
     * vue-excel-editor 把 label 快照在 fields 里，切换语言后需配合 applyExcelEditorI18nHeaders 刷新。
     */
    defectExcelHeaderLabel(label, field) {
      const esc = (s) =>
        String(s == null ? "" : s)
          .replace(/&/g, "&amp;")
          .replace(/</g, "&lt;")
          .replace(/>/g, "&gt;")
          .replace(/"/g, "&quot;");
      const name = field && field.name;
      const col = name ? COLS.find((c) => c.key === name) : null;
      const text = col ? String(this.$t(col.titleKey)) : String(label == null ? "" : label);
      if (!col || !col.required) return esc(text);
      return `<span style="color:#f56c6c">${esc(text)}</span>`;
    },
    /** 同步 vue-excel-editor 内部 fields[].label（组件只在 created 里 register 一次） */
    applyExcelEditorI18nHeaders() {
      const ed = this.$refs.excelEditor;
      if (!ed || !Array.isArray(ed.fields)) return;
      ed.fields.forEach((f) => {
        const col = COLS.find((c) => c.key === f.name);
        if (col) f.label = String(this.$t(col.titleKey));
      });
      ed.$forceUpdate();
    },
    columnFieldType(c) {
      return c.fieldType === "map" ? "map" : "string";
    },
    /** 可编辑 map 列不能 readonly；下拉由库内右侧三角热区与 input 区右侧点击打开 */
    columnCellReadonly(c) {
      return !c.editable;
    },
    columnAllowKeys(c) {
      if (c.key === "moduleName" && c.editable) return this.excelMapBlockKeys;
      if (c.fieldType === "map" && c.editable) return this.excelMapBlockKeys;
      return null;
    },
    columnOptions(c) {
      if (c.key === "defectType") return this.defectTypeSelectMap;
      if (c.key === "defectLevel") return this.defectLevelSelectMap;
      if (c.key === "defectStateText") return this.defectStateSelectMap;
      if (c.key === "excelHandleByMemberId") return this.memberSelectMap;
      if (c.key === "planStartTime" || c.key === "planEndTime") return PLAN_TIME_CELL_OPTIONS_MARKER;
      if (c.key === "moduleName") return PLAN_TIME_CELL_OPTIONS_MARKER;
      return null;
    },
    refreshProjectMembers() {
      const pid =
        (this.queryParams && this.queryParams.projectId) ||
        this.projectId ||
        0;
      if (!pid) {
        Object.keys(this.memberSelectMap).forEach((k) => this.$delete(this.memberSelectMap, k));
        this.refreshExcelEditorView();
        return;
      }
      listMemberOfProject(pid, { pageNum: 1, pageSize: 2000 }).then((res) => {
        const m = {};
        (res.rows || []).forEach((mem) => {
          const id = mem.userId;
          if (id == null || id === "") return;
          m[String(id)] = String(mem.nickName || mem.userName || mem.memberName || id);
        });
        this.syncExcelOptionMap(this.memberSelectMap, m);
        this.refreshExcelEditorView();
      });
    },
    /** 多人处理时取首位用于 Excel 下拉；保存后变为单人列表 */
    firstHandleByMemberId(row) {
      if (Array.isArray(row.handleBy) && row.handleBy.length) return String(row.handleBy[0]);
      const list = row && row.handleByList;
      if (Array.isArray(list) && list.length && list[0] && list[0].userId != null) return String(list[0].userId);
      return "";
    },
    formatCreateByText(row) {
      const m = row && row.createMember ? row.createMember : null;
      if (m) return String(m.nickName || m.userName || m.memberName || "");
      if (row && row.createMemberName) return String(row.createMemberName);
      if (row && row.createBy) return String(row.createBy);
      return "";
    },
    /** 图片/附件列：vue-excel-editor 补丁 cellHtml */
    excelColumnCellHtml(c) {
      if (!c) return null;
      if (c.key === "excelImgUrlsText") return (record) => this.buildExcelImageGalleryHtml(record);
      if (c.key === "excelAnnexUrlsText") return (record) => this.buildExcelAnnexUrlsHtml(record);
      return null;
    },
    buildExcelImageGalleryHtml(record) {
      if (!record || !record.defectId) return "";
      const escAttr = (s) =>
        String(s)
          .replace(/&/g, "&amp;")
          .replace(/"/g, "&quot;")
          .replace(/</g, "&lt;")
          .replace(/>/g, "&gt;");
      const rawParts = String(record.imgUrls || "")
        .split(",")
        .map((s) => s.trim())
        .filter(Boolean);
      const baseApi = String(process.env.VUE_APP_BASE_API || "");
      const removeTitle = escAttr(String(this.$t("delete")));
      const tiles = rawParts.length
        ? rawParts
            .map((pathRel) => {
              const full = baseApi + pathRel;
              const escPath = escAttr(pathRel);
              return (
                `<span class="defect-excel-img-wrap">` +
                `<img class="defect-excel-img-tile" src="${escAttr(full)}" alt="" loading="lazy" />` +
                `<span class="defect-excel-img-remove" data-defect-id="${escAttr(String(record.defectId))}" data-img-path="${escPath}" title="${removeTitle}" role="button" tabindex="0"><i class="el-icon-close"></i></span>` +
                `</span>`
              );
            })
            .join("")
        : "";
      const rowInner = tiles
        ? `<span class="defect-excel-img-row">${tiles}</span>`
        : `<span class="defect-excel-img-row defect-excel-img-row--empty"></span>`;
      const uploadTitle = escAttr(String(this.$t("defect.excel-upload-image")));
      const btn = `<span class="defect-excel-inline-upload-btn" data-upload-kind="img" data-defect-id="${escAttr(
        String(record.defectId)
      )}" data-project-id="${escAttr(String(record.projectId != null ? record.projectId : ""))}" title="${uploadTitle}" role="button" tabindex="0"><i class="el-icon-upload2"></i></span>`;
      return `<div class="defect-excel-attach-cell">${rowInner}${btn}</div>`;
    },
    isDefectExcelPreviewThumbImg(node) {
      return (
        node &&
        node.nodeType === 1 &&
        node.tagName === "IMG" &&
        node.classList.contains("defect-excel-img-tile") &&
        this.$refs.defectExcelContext &&
        this.$refs.defectExcelContext.contains(node)
      );
    },
    isDefectExcelAnnexLine(node) {
      return (
        node &&
        node.nodeType === 1 &&
        node.classList.contains("defect-excel-annex-line") &&
        node.getAttribute("data-url") &&
        this.$refs.defectExcelContext &&
        this.$refs.defectExcelContext.contains(node)
      );
    },
    /** 与 Cat2BugText fileName 一致：去路径、decode、弱化随机后缀 */
    annexFileDisplayNameFromPath(path) {
      const seg = String(path).replace(/\\/g, "/").split("/");
      let name = seg[seg.length - 1] || path;
      try {
        name = decodeURIComponent(name);
      } catch (e) {
        /* ignore */
      }
      return String(name).replace(/_[0-9a-zA-Z]+\./g, ".");
    },
    /** 与 Cat2BugText handleClickDown 一致：fetch blob 后本地下载 */
    downloadDefectExcelAnnexUrl(fullUrl) {
      if (!fullUrl) return;
      const link = document.createElement("a");
      const display = this.annexFileDisplayNameFromPath(fullUrl);
      fetch(fullUrl)
        .then((res) => {
          if (!res.ok) throw new Error(String(res.status));
          return res.blob();
        })
        .then((blob) => {
          const href = URL.createObjectURL(blob);
          link.href = href;
          link.download = display || "download";
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          URL.revokeObjectURL(href);
        })
        .catch(() => {
          try {
            this.$modal.msgError(String(this.$t("download.error")));
          } catch (e2) {
            /* ignore */
          }
        });
    },
    buildExcelAnnexUrlsHtml(record) {
      if (!record || !record.defectId) return "";
      const base = String(process.env.VUE_APP_BASE_API || "");
      const parts = String(record.annexUrls || "")
        .split(",")
        .map((s) => s.trim())
        .filter(Boolean);
      const escAttr = (s) =>
        String(s)
          .replace(/&/g, "&amp;")
          .replace(/"/g, "&quot;")
          .replace(/</g, "&lt;")
          .replace(/>/g, "&gt;");
      const escText = (s) =>
        String(s)
          .replace(/&/g, "&amp;")
          .replace(/</g, "&lt;")
          .replace(/>/g, "&gt;");
      const removeTitle = escAttr(String(this.$t("delete")));
      const lines = parts.length
        ? parts
            .map((path) => {
              const full = base + path;
              const label = this.annexFileDisplayNameFromPath(path);
              const escPath = escAttr(path);
              return (
                `<span class="defect-excel-annex-item">` +
                `<span class="defect-excel-annex-line" data-url="${escAttr(full)}" data-annex-path="${escPath}" title="${escAttr(
                  label
                )}"><i class="el-icon-paperclip"></i><span class="defect-excel-annex-label">${escText(label)}</span></span>` +
                `<span class="defect-excel-annex-remove" data-defect-id="${escAttr(String(record.defectId))}" data-annex-path="${escPath}" title="${removeTitle}" role="button" tabindex="0"><i class="el-icon-close"></i></span>` +
                `</span>`
              );
            })
            .join("")
        : "";
      const stackInner = lines
        ? lines
        : `<span class="defect-excel-annex-line defect-excel-annex-line--placeholder">&nbsp;</span>`;
      const uploadTitle = escAttr(String(this.$t("defect.excel-upload-annex")));
      const btn = `<span class="defect-excel-inline-upload-btn" data-upload-kind="annex" data-defect-id="${escAttr(
        String(record.defectId)
      )}" data-project-id="${escAttr(String(record.projectId != null ? record.projectId : ""))}" title="${uploadTitle}" role="button" tabindex="0"><i class="el-icon-upload2"></i></span>`;
      return `<div class="defect-excel-attach-cell"><div class="defect-excel-annex-stack">${stackInner}</div>${btn}</div>`;
    },
    defectExcelClosestInlineUploadBtn(node) {
      const root = this.$refs.defectExcelContext;
      if (!node || !root || typeof node.closest !== "function") return null;
      const btn = node.closest(".defect-excel-inline-upload-btn");
      if (!btn || !root.contains(btn)) return null;
      return btn;
    },
    defectExcelClosestImgRemoveBtn(node) {
      const root = this.$refs.defectExcelContext;
      if (!node || !root || typeof node.closest !== "function") return null;
      const btn = node.closest(".defect-excel-img-remove");
      if (!btn || !root.contains(btn)) return null;
      return btn;
    },
    defectExcelClosestAnnexRemoveBtn(node) {
      const root = this.$refs.defectExcelContext;
      if (!node || !root || typeof node.closest !== "function") return null;
      const btn = node.closest(".defect-excel-annex-remove");
      if (!btn || !root.contains(btn)) return null;
      return btn;
    },
    excelCommaUrlParts(str) {
      return String(str || "")
        .split(",")
        .map((s) => s.trim())
        .filter(Boolean);
    },
    excelMergeCommaUrls(existingStr, newRelativePaths) {
      const merged = [...this.excelCommaUrlParts(existingStr), ...newRelativePaths.map((p) => String(p).trim()).filter(Boolean)];
      return merged.join(",");
    },
    validateExcelInlineImageFile(file) {
      const types = EXCEL_INLINE_IMG_FILE_TYPES;
      let ext = "";
      if (file && file.name && file.name.lastIndexOf(".") > -1) {
        ext = file.name.slice(file.name.lastIndexOf(".") + 1).toLowerCase();
      }
      const okType = types.some((t) => {
        if (file.type && String(file.type).toLowerCase().indexOf(String(t).toLowerCase()) > -1) return true;
        return ext && ext === String(t).toLowerCase();
      });
      if (!okType) {
        this.$modal.msgError(strFormat(this.$t("upload.file-format-is-incorrect"), types.join("/")));
        return false;
      }
      if (file.size / 1024 / 1024 >= EXCEL_INLINE_IMG_MAX_MB) {
        this.$modal.msgError(strFormat(this.$t("upload.img-size-exceeds-range"), EXCEL_INLINE_IMG_MAX_MB));
        return false;
      }
      return true;
    },
    validateExcelInlineAnnexFile(file) {
      if (file.size / 1024 / 1024 >= EXCEL_INLINE_ANNEX_MAX_MB) {
        this.$modal.msgError(strFormat(this.$t("upload.size-exceeds-range"), EXCEL_INLINE_ANNEX_MAX_MB));
        return false;
      }
      return true;
    },
    onExcelImageRemoveClick(removeEl) {
      if (!removeEl || typeof removeEl.getAttribute !== "function") return;
      const defectId = removeEl.getAttribute("data-defect-id");
      const pathRel = removeEl.getAttribute("data-img-path");
      if (!defectId || pathRel == null || pathRel === "") return;
      this.$modal
        .confirm(String(this.$t("is-delete-img")), String(this.$t("prompted")), {
          confirmButtonText: String(this.$t("delete")),
          cancelButtonText: String(this.$t("cancel")),
          type: "warning",
        })
        .then(() => this.executeExcelInlineImageRemove(defectId, pathRel))
        .catch(() => {});
    },
    async executeExcelInlineImageRemove(defectId, pathRel) {
      const row = this.sheetRows.find((r) => String(r.defectId) === String(defectId));
      if (!row || this.isDeletedDefectRow(row)) {
        if (row) this.notifyDeletedDefectCannotEdit();
        return;
      }
      const parts = this.excelCommaUrlParts(row.imgUrls);
      const next = parts.filter((p) => String(p) !== String(pathRel));
      if (next.length === parts.length) return;
      const merged = next.join(",");
      const projectId = row.projectId != null && row.projectId !== "" ? row.projectId : this.queryParams.projectId;
      this.syncing = true;
      this.$modal.loading(String(this.$t("upload.img-deleting")));
      try {
        await updateDefect({
          defectId: row.defectId,
          projectId,
          imgUrls: merged,
        });
        this.$set(row, "imgUrls", merged);
        this.touchRowUpdateTime(row);
        this.refreshExcelEditorView();
        this.$modal.msgSuccess(this.$t("upload.img-delete-success").toString());
      } catch (e) {
        this.$modal.msgError(this.$t("defect.excel-save-failed").toString());
      } finally {
        this.$modal.closeLoading();
        this.syncing = false;
      }
    },
    onExcelAnnexRemoveClick(removeEl) {
      if (!removeEl || typeof removeEl.getAttribute !== "function") return;
      const defectId = removeEl.getAttribute("data-defect-id");
      const pathRel = removeEl.getAttribute("data-annex-path");
      if (!defectId || pathRel == null || pathRel === "") return;
      const row = this.sheetRows.find((r) => String(r.defectId) === String(defectId));
      const display = row ? this.annexFileDisplayNameFromPath(pathRel) : pathRel;
      this.$modal
        .confirm(String(strFormat(this.$t("doc.is-delete-file"), display)), String(this.$t("prompted")), {
          confirmButtonText: String(this.$t("delete")),
          cancelButtonText: String(this.$t("cancel")),
          type: "warning",
        })
        .then(() => this.executeExcelInlineAnnexRemove(defectId, pathRel))
        .catch(() => {});
    },
    async executeExcelInlineAnnexRemove(defectId, pathRel) {
      const row = this.sheetRows.find((r) => String(r.defectId) === String(defectId));
      if (!row || this.isDeletedDefectRow(row)) {
        if (row) this.notifyDeletedDefectCannotEdit();
        return;
      }
      const parts = this.excelCommaUrlParts(row.annexUrls);
      const next = parts.filter((p) => String(p) !== String(pathRel));
      if (next.length === parts.length) return;
      const merged = next.join(",");
      const projectId = row.projectId != null && row.projectId !== "" ? row.projectId : this.queryParams.projectId;
      this.syncing = true;
      this.$modal.loading(String(this.$t("upload.file-deleting")));
      try {
        await updateDefect({
          defectId: row.defectId,
          projectId,
          annexUrls: merged,
        });
        this.$set(row, "annexUrls", merged);
        this.touchRowUpdateTime(row);
        this.refreshExcelEditorView();
        this.$modal.msgSuccess(this.$t("upload.file-delete-success").toString());
      } catch (e) {
        this.$modal.msgError(this.$t("defect.excel-save-failed").toString());
      } finally {
        this.$modal.closeLoading();
        this.syncing = false;
      }
    },
    async onExcelInlineImgFileChange(ev) {
      const input = ev.target;
      const files = Array.from((input && input.files) || []);
      if (input) input.value = "";
      const tgt = this.excelInlineUploadTarget;
      this.excelInlineUploadTarget = null;
      if (!tgt || tgt.kind !== "img" || !tgt.defectId || !files.length) return;
      const row = this.sheetRows.find((r) => String(r.defectId) === String(tgt.defectId));
      if (!row) return;
      await this.excelInlineUploadImagesForRow(row, files);
    },
    /** 从剪贴板收集图片文件（截图、复制图片等） */
    collectClipboardImageFiles(e) {
      const cd = e.clipboardData;
      if (!cd) return [];
      const out = [];
      if (cd.items && cd.items.length) {
        for (let i = 0; i < cd.items.length; i++) {
          const it = cd.items[i];
          if (it.kind !== "file") continue;
          const t = String(it.type || "").toLowerCase();
          if (t.indexOf("image/") !== 0) continue;
          const f = it.getAsFile();
          if (f) out.push(f);
        }
      }
      if (!out.length && cd.files && cd.files.length) {
        for (let i = 0; i < cd.files.length; i++) {
          const f = cd.files[i];
          if (f && String(f.type || "").toLowerCase().indexOf("image/") === 0) out.push(f);
        }
      }
      return out;
    },
    /** 剪贴板中的文件（附件列：任意类型文件，含从资源管理器复制等） */
    collectClipboardFiles(e) {
      const cd = e.clipboardData;
      if (!cd) return [];
      const out = [];
      if (cd.items && cd.items.length) {
        for (let i = 0; i < cd.items.length; i++) {
          const it = cd.items[i];
          if (it.kind !== "file") continue;
          const f = it.getAsFile();
          if (f) out.push(f);
        }
      }
      if (!out.length && cd.files && cd.files.length) {
        for (let i = 0; i < cd.files.length; i++) {
          if (cd.files[i]) out.push(cd.files[i]);
        }
      }
      const seen = new Set();
      const deduped = [];
      for (let i = 0; i < out.length; i++) {
        const f = out[i];
        const k = `${f.name}|${f.size}|${f.lastModified}`;
        if (seen.has(k)) continue;
        seen.add(k);
        deduped.push(f);
      }
      return deduped;
    },
    /**
     * 图片/附件列聚焦时 Ctrl/Cmd+V：剪贴板为文件则走上传；纯文本粘贴阻止写入格内（改图/附件只走上传）。
     * capture：在事件到达隐藏 textarea 前拦截。
     */
    onDefectExcelPasteCapture(e) {
      const ed = this.$refs.excelEditor;
      if (!ed || (!ed.focused && !ed.mousein)) return;
      if (ed.inputBoxShow) return;
      const field = ed.currentField;
      if (!field) return;
      const rec = ed.currentRecord;
      if (!rec || rec.defectId == null || rec.defectId === "") return;
      const row = this.sheetRows.find((r) => String(r.defectId) === String(rec.defectId));
      if (!row) return;

      if (field.name === "excelImgUrlsText") {
        const files = this.collectClipboardImageFiles(e);
        if (files.length) {
          e.preventDefault();
          e.stopPropagation();
          if (this.syncing) return;
          void this.excelInlineUploadImagesForRow(row, files);
          return;
        }
        e.preventDefault();
        e.stopPropagation();
        return;
      }
      if (field.name === "excelAnnexUrlsText") {
        const files = this.collectClipboardFiles(e);
        if (files.length) {
          e.preventDefault();
          e.stopPropagation();
          if (this.syncing) return;
          void this.excelInlineUploadAnnexesForRow(row, files);
          return;
        }
        e.preventDefault();
        e.stopPropagation();
        return;
      }
    },
    /** 内联「添加图片」与剪贴板粘贴共用：校验、上传、updateDefect、刷新单元格 */
    async excelInlineUploadImagesForRow(row, files) {
      if (!row || !files || !files.length) return;
      if (this.isDeletedDefectRow(row)) {
        this.notifyDeletedDefectCannotEdit();
        return;
      }
      const existing = this.excelCommaUrlParts(row.imgUrls);
      if (existing.length + files.length > EXCEL_INLINE_IMG_LIMIT) {
        this.$modal.msgError(strFormat(this.$t("upload.number-exceeds-range"), EXCEL_INLINE_IMG_LIMIT));
        return;
      }
      for (let i = 0; i < files.length; i++) {
        if (!this.validateExcelInlineImageFile(files[i])) return;
      }
      const projectId = row.projectId != null && row.projectId !== "" ? row.projectId : this.queryParams.projectId;
      this.syncing = true;
      this.$modal.loading(this.$t("upload.img-loading").toString());
      try {
        const newNames = [];
        for (let i = 0; i < files.length; i++) {
          const fd = new FormData();
          fd.append("file", files[i]);
          const res = await upload(fd);
          if (!res || res.code !== 200 || !res.fileName) {
            throw new Error((res && res.msg) || "upload");
          }
          newNames.push(res.fileName);
        }
        const merged = this.excelMergeCommaUrls(row.imgUrls, newNames);
        await updateDefect({
          defectId: row.defectId,
          projectId,
          imgUrls: merged,
        });
        this.$set(row, "imgUrls", merged);
        this.touchRowUpdateTime(row);
        this.refreshExcelEditorView();
        this.$modal.msgSuccess(this.$t("defect.excel-save-success").toString());
      } catch (e) {
        this.$modal.msgError(this.$t("upload.img-fail").toString());
      } finally {
        this.$modal.closeLoading();
        this.syncing = false;
      }
    },
    async onExcelInlineAnnexFileChange(ev) {
      const input = ev.target;
      const files = Array.from((input && input.files) || []);
      if (input) input.value = "";
      const tgt = this.excelInlineUploadTarget;
      this.excelInlineUploadTarget = null;
      if (!tgt || tgt.kind !== "annex" || !tgt.defectId || !files.length) return;
      const row = this.sheetRows.find((r) => String(r.defectId) === String(tgt.defectId));
      if (!row) return;
      await this.excelInlineUploadAnnexesForRow(row, files);
    },
    /** 内联「上传附件」与剪贴板粘贴共用 */
    async excelInlineUploadAnnexesForRow(row, files) {
      if (!row || !files || !files.length) return;
      if (this.isDeletedDefectRow(row)) {
        this.notifyDeletedDefectCannotEdit();
        return;
      }
      const existing = this.excelCommaUrlParts(row.annexUrls);
      if (existing.length + files.length > EXCEL_INLINE_ANNEX_LIMIT) {
        this.$modal.msgError(strFormat(this.$t("upload.number-exceeds-range"), EXCEL_INLINE_ANNEX_LIMIT));
        return;
      }
      for (let i = 0; i < files.length; i++) {
        if (!this.validateExcelInlineAnnexFile(files[i])) return;
      }
      const projectId = row.projectId != null && row.projectId !== "" ? row.projectId : this.queryParams.projectId;
      this.syncing = true;
      this.$modal.loading(this.$t("upload.uploading").toString());
      try {
        const newNames = [];
        for (let i = 0; i < files.length; i++) {
          const fd = new FormData();
          fd.append("file", files[i]);
          const res = await upload(fd);
          if (!res || res.code !== 200 || !res.fileName) {
            throw new Error((res && res.msg) || "upload");
          }
          newNames.push(res.fileName);
        }
        const merged = this.excelMergeCommaUrls(row.annexUrls, newNames);
        await updateDefect({
          defectId: row.defectId,
          projectId,
          annexUrls: merged,
        });
        this.$set(row, "annexUrls", merged);
        this.touchRowUpdateTime(row);
        this.refreshExcelEditorView();
        this.$modal.msgSuccess(this.$t("defect.excel-save-success").toString());
      } catch (e) {
        this.$modal.msgError(this.$t("upload.file-fail").toString());
      } finally {
        this.$modal.closeLoading();
        this.syncing = false;
      }
    },
    onDefectExcelCellInteractiveMousedownCapture(e) {
      if (this.tryBeginExcelColumnHeaderDrag(e)) return;
      if (this.defectExcelClosestInlineUploadBtn(e.target)) {
        e.preventDefault();
        e.stopPropagation();
        return;
      }
      if (this.defectExcelClosestImgRemoveBtn(e.target)) {
        e.preventDefault();
        e.stopPropagation();
        return;
      }
      if (this.defectExcelClosestAnnexRemoveBtn(e.target)) {
        e.preventDefault();
        e.stopPropagation();
        return;
      }
      if (this.isDefectExcelPreviewThumbImg(e.target) || this.isDefectExcelAnnexLine(e.target)) {
        e.preventDefault();
        e.stopPropagation();
      }
    },
    onDefectExcelCellInteractiveClickCapture(e) {
      const uploadBtn = this.defectExcelClosestInlineUploadBtn(e.target);
      if (uploadBtn) {
        e.preventDefault();
        e.stopPropagation();
        const kind = uploadBtn.getAttribute("data-upload-kind");
        const defectId = uploadBtn.getAttribute("data-defect-id");
        if (!defectId) return;
        this.excelInlineUploadTarget = { kind, defectId };
        this.$nextTick(() => {
          if (kind === "img") {
            const el = this.$refs.excelInlineImgFileInput;
            if (el) el.click();
          } else {
            const el = this.$refs.excelInlineAnnexFileInput;
            if (el) el.click();
          }
        });
        return;
      }
      const imgRemove = this.defectExcelClosestImgRemoveBtn(e.target);
      if (imgRemove) {
        e.preventDefault();
        e.stopPropagation();
        this.onExcelImageRemoveClick(imgRemove);
        return;
      }
      const annexRemove = this.defectExcelClosestAnnexRemoveBtn(e.target);
      if (annexRemove) {
        e.preventDefault();
        e.stopPropagation();
        this.onExcelAnnexRemoveClick(annexRemove);
        return;
      }
      if (this.isDefectExcelPreviewThumbImg(e.target)) {
        e.preventDefault();
        e.stopPropagation();
        this.openDefectExcelImageViewer(e.target);
        return;
      }
      const annexLine =
        typeof e.target.closest === "function" ? e.target.closest(".defect-excel-annex-line") : null;
      if (
        annexLine &&
        this.$refs.defectExcelContext &&
        this.$refs.defectExcelContext.contains(annexLine) &&
        annexLine.getAttribute("data-url") &&
        !annexLine.classList.contains("defect-excel-annex-line--placeholder")
      ) {
        e.preventDefault();
        e.stopPropagation();
        const u = annexLine.getAttribute("data-url");
        if (u) this.downloadDefectExcelAnnexUrl(u);
      }
    },
    openDefectExcelImageViewer(clickedImg) {
      const wrap = clickedImg.closest(".defect-excel-img-row");
      if (!wrap) return;
      const imgs = wrap.querySelectorAll("img.defect-excel-img-tile");
      const urls = Array.from(imgs)
        .map((im) => im.getAttribute("src"))
        .filter(Boolean);
      if (!urls.length) return;
      const clickedSrc = clickedImg.getAttribute("src");
      let idx = urls.indexOf(clickedSrc);
      if (idx < 0) idx = 0;
      this.excelImageViewerKey += 1;
      this.excelImageViewerUrls = urls;
      this.excelImageViewerIndex = idx;
      this.excelImageViewerVisible = true;
    },
    closeExcelImageViewer() {
      this.excelImageViewerVisible = false;
      this.excelImageViewerUrls = [];
      this.excelImageViewerIndex = 0;
    },
    search(params) {
      this.loading = true;
      this.queryParams = params;
      this.refreshProjectMembers();
      const gen = ++this.excelListRequestGen;
      this.excelListPageNum = 1;
      this.excelViewportAutoFetchCount = 0;
      const requestParams = {
        ...(params || {}),
        pageNum: 1,
        pageSize: EXCEL_DEFECT_PAGE_SIZE,
        orderByColumn: EXCEL_LIST_ORDER_BY_COLUMN,
        isAsc: EXCEL_LIST_IS_ASC,
      };
      listDefect(requestParams)
        .then((response) => {
          if (gen !== this.excelListRequestGen) return;
          this.loading = false;
          const rows = response.rows || [];
          this.defectTotal =
            response.total != null && response.total !== ""
              ? Number(response.total)
              : rows.length;
          this.defectList = rows.map((r) => this.mapDefectRowForExcelSheet(r));
          this.sheetRows = this.buildSheetRows(this.defectList);
          this.$nextTick(() => {
            this.applyExcelColumnVisibilityToEditorFields();
            this.applyExcelColumnOrderToEditorFields();
            this.applyPersistedExcelColumnWidthsToEditor();
            this.scheduleExcelEditorLayoutFix();
            this.ensureExcelScrollLoadMoreListener();
            this.maybeFillExcelViewportUntilScrollable();
          });
        })
        .catch(() => {
          if (gen !== this.excelListRequestGen) return;
          this.loading = false;
        });
    },
    /**
     * 占位行使 tableContent 总有很大 scrollHeight，不能用「整表是否可滚」判断首屏是否要预拉。
     * 用「已加载数据行估算高度」是否仍明显矮于可视区。
     */
    maybeFillExcelViewportUntilScrollable() {
      if (this.loading || this.excelListLoadingMore || this.syncing) return;
      if (this.excelViewportAutoFetchCount >= 15) return;
      const loaded = Array.isArray(this.defectList) ? this.defectList.length : 0;
      if (this.defectTotal != null && Number.isFinite(this.defectTotal) && loaded >= this.defectTotal) return;
      const tc =
        this._excelTableContentEl ||
        (this.$refs.excelEditor && this.$refs.excelEditor.$refs && this.$refs.excelEditor.$refs.tableContent);
      if (!tc) return;
      const estimatedDataH = loaded * EXCEL_DATA_ROW_ESTIMATE_PX;
      if (estimatedDataH >= tc.clientHeight + 60) return;
      this.tryLoadMoreDefectsForExcel({ fromViewportFill: true });
    },
    /**
     * 数据行下方仍有大量空白行时，scroll 接近「物理底部」不等于滚到了「最后一条缺陷」。
     * 用最后一条真实数据行与可视区底边的距离判断是否应请求下一页。
     */
    isExcelLastDataRowNearViewportBottom(tableContentEl, thresholdPx) {
      const n = Array.isArray(this.defectList) ? this.defectList.length : 0;
      if (!n || !tableContentEl) return false;
      const ed = this.$refs.excelEditor;
      const rb = ed && ed.recordBody;
      if (!rb || !rb.children || !rb.children.length) return false;
      const idx = Math.min(n, rb.children.length) - 1;
      if (idx < 0) return false;
      const lastRow = rb.children[idx];
      if (!lastRow || lastRow.tagName !== "TR") return false;
      const tcRect = tableContentEl.getBoundingClientRect();
      const rowRect = lastRow.getBoundingClientRect();
      const th = thresholdPx != null ? thresholdPx : EXCEL_LAST_DATA_ROW_NEAR_BOTTOM_PX;
      return rowRect.bottom <= tcRect.bottom + th;
    },
    ensureExcelScrollLoadMoreListener() {
      const ed = this.$refs.excelEditor;
      const tc = ed && ed.$refs && ed.$refs.tableContent;
      if (!tc || typeof tc.addEventListener !== "function") return;
      if (this._excelTableContentEl === tc && this._excelScrollBound) return;
      this.teardownExcelScrollLoadMoreListener();
      this._excelTableContentEl = tc;
      this._excelScrollBound = this.onExcelTableScroll.bind(this);
      tc.addEventListener("scroll", this._excelScrollBound, { passive: true });
    },
    teardownExcelScrollLoadMoreListener() {
      if (this._excelScrollLoadDebounceTimer) {
        clearTimeout(this._excelScrollLoadDebounceTimer);
        this._excelScrollLoadDebounceTimer = null;
      }
      if (this._excelTableContentEl && this._excelScrollBound) {
        this._excelTableContentEl.removeEventListener("scroll", this._excelScrollBound);
      }
      this._excelTableContentEl = null;
      this._excelScrollBound = null;
    },
    onExcelTableScroll() {
      this.dismissExcelFloatingPanelsOnTableScroll();
      if (this._excelScrollLoadDebounceTimer) {
        clearTimeout(this._excelScrollLoadDebounceTimer);
      }
      this._excelScrollLoadDebounceTimer = setTimeout(() => {
        this._excelScrollLoadDebounceTimer = null;
        this.tryLoadMoreDefectsForExcel();
      }, 200);
    },
    /** 表格 tableContent 上下/左右滚动时收起本页自定义浮层（交付物、计划时间、缩略图预览）。
     * 「显示字段」popover 不在此关闭：勾选会触发布局并可能产生 scroll，否则弹层会立刻消失、无法连续勾选。
     */
    dismissExcelFloatingPanelsOnTableScroll() {
      if (this.excelModulePickerVisible) this.closeExcelModulePicker();
      if (this.planTimePickerVisible) {
        this.planTimePickerVisible = false;
        const dp = this.$refs.planTimeDatePicker;
        if (dp) {
          if (typeof dp.handleClose === "function") dp.handleClose();
          else if (dp.pickerVisible !== undefined) dp.pickerVisible = false;
        }
        this.resetPlanTimePickerState();
      }
      if (this.excelImageViewerVisible) this.closeExcelImageViewer();
    },
    /**
     * @param {object} [opts]
     * @param {boolean} [opts.fromViewportFill] 为 true 时不校验「接近底部」，用于首屏高度不足时链式拉页；此时受 excelViewportAutoFetchCount 上限约束
     */
    tryLoadMoreDefectsForExcel(opts) {
      if (this.loading || this.excelListLoadingMore || this.syncing) return;
      const fromViewportFill = !!(opts && opts.fromViewportFill);
      if (fromViewportFill && this.excelViewportAutoFetchCount >= 15) return;
      const loaded = Array.isArray(this.defectList) ? this.defectList.length : 0;
      if (this.defectTotal != null && Number.isFinite(this.defectTotal) && loaded >= this.defectTotal) return;
      const tc =
        this._excelTableContentEl ||
        (this.$refs.excelEditor && this.$refs.excelEditor.$refs && this.$refs.excelEditor.$refs.tableContent);
      if (!tc) return;
      if (!fromViewportFill) {
        const { scrollTop, scrollHeight, clientHeight } = tc;
        const nearPhysicalBottom =
          scrollHeight - scrollTop - clientHeight <= EXCEL_SCROLL_LOAD_MORE_THRESHOLD_PX;
        const nearLastDataBottom = this.isExcelLastDataRowNearViewportBottom(
          tc,
          EXCEL_LAST_DATA_ROW_NEAR_BOTTOM_PX
        );
        if (!nearPhysicalBottom && !nearLastDataBottom) return;
      } else {
        const estimatedDataH = loaded * EXCEL_DATA_ROW_ESTIMATE_PX;
        if (estimatedDataH >= tc.clientHeight + 60) return;
      }
      const gen = this.excelListRequestGen;
      if (fromViewportFill) this.excelViewportAutoFetchCount += 1;
      this.excelListLoadingMore = true;
      const nextPage = this.excelListPageNum + 1;
      const requestParams = {
        ...(this.queryParams || {}),
        pageNum: nextPage,
        pageSize: EXCEL_DEFECT_PAGE_SIZE,
        orderByColumn: EXCEL_LIST_ORDER_BY_COLUMN,
        isAsc: EXCEL_LIST_IS_ASC,
      };
      listDefect(requestParams)
        .then((response) => {
          this.excelListLoadingMore = false;
          if (gen !== this.excelListRequestGen) return;
          const rows = response.rows || [];
          if (!rows.length) {
            this.defectTotal = loaded;
            return;
          }
          this.excelListPageNum = nextPage;
          const mapped = rows.map((r) => this.mapDefectRowForExcelSheet(r));
          this.defectList = [...this.defectList, ...mapped];
          this.sheetRows = this.buildSheetRows(this.defectList);
          if (response.total != null && response.total !== "") {
            this.defectTotal = Number(response.total);
          }
          this.$nextTick(() => {
            this.scheduleExcelEditorLayoutFix();
            this.maybeFillExcelViewportUntilScrollable();
            /* 追加后若最后一条数据仍贴底，继续拉一页（避免须再微滚一下才触发 scroll） */
            requestAnimationFrame(() => {
              if (this.loading || this.excelListLoadingMore || this.syncing) return;
              const tc2 =
                this._excelTableContentEl ||
                (this.$refs.excelEditor &&
                  this.$refs.excelEditor.$refs &&
                  this.$refs.excelEditor.$refs.tableContent);
              const ld = Array.isArray(this.defectList) ? this.defectList.length : 0;
              if (
                !tc2 ||
                this.defectTotal == null ||
                !Number.isFinite(this.defectTotal) ||
                ld >= this.defectTotal
              ) {
                return;
              }
              if (this.isExcelLastDataRowNearViewportBottom(tc2, EXCEL_LAST_DATA_ROW_NEAR_BOTTOM_PX)) {
                this.tryLoadMoreDefectsForExcel();
              }
            });
          });
        })
        .catch(() => {
          this.excelListLoadingMore = false;
        });
    },
    /** 数据行 + 空白占位行（无 defectId，仅展示，保存时会跳过） */
    buildSheetRows(dataRows) {
      const rows = (dataRows || []).map((r) => ({ ...r }));
      const target = Math.max(MIN_SHEET_ROWS, rows.length + TAIL_EMPTY_ROWS);
      while (rows.length < target) {
        rows.push(this.createEmptySheetRow());
      }
      return rows;
    },
    createEmptySheetRow() {
      const row = {
        projectId: this.queryParams && this.queryParams.projectId,
      };
      COLS.forEach((c) => {
        row[c.key] = "";
      });
      row.moduleId = null;
      return row;
    },
    handleSheetUpdate(buf) {
      if (!Array.isArray(buf) || this.syncing) return;
      const items = [];
      const touchedPlaceholderIds = new Set();
      let deletedEditAttempted = false;
      for (const rec of buf) {
        if (rec.err) continue;
        const col = COLS.find((c) => c.key === rec.name);
        if (!col || !col.editable) continue;
        if (col.key === "excelImgUrlsText" || col.key === "excelAnnexUrlsText") continue;
        const nv = rec.newVal == null ? "" : String(rec.newVal);
        const ov = rec.oldVal == null ? "" : String(rec.oldVal);
        if (nv === ov) continue;
        const row = this.sheetRows.find((r) => r.$id === rec.$id);
        if (!row) continue;
        if (this.isDeletedDefectRow(row)) {
          deletedEditAttempted = true;
          this.syncing = true;
          this.$set(row, rec.name, rec.oldVal == null ? "" : rec.oldVal);
          this.$nextTick(() => {
            this.syncing = false;
          });
          continue;
        }
        if (!row.defectId) {
          touchedPlaceholderIds.add(rec.$id);
          if (col.required && (!nv || nv === "")) {
            const msgKey = EXCEL_REQUIRED_EMPTY_MSG_I18N[col.key] || "defect.excel-handle-by-required";
            this.$modal.msgError(this.$t(msgKey).toString());
            this.syncing = true;
            this.$set(row, col.key, ov);
            this.$nextTick(() => {
              this.syncing = false;
            });
          }
          continue;
        }
        if (col.required && (!nv || nv === "")) {
          const msgKey = EXCEL_REQUIRED_EMPTY_MSG_I18N[col.key] || "defect.excel-handle-by-required";
          this.$modal.msgError(this.$t(msgKey).toString());
          this.syncing = true;
          this.$set(row, col.key, ov);
          this.$nextTick(() => {
            this.syncing = false;
          });
          continue;
        }
        if (col.key === "defectStateText" && (!nv || String(nv).trim() === "")) {
          this.syncing = true;
          this.$set(row, col.key, ov);
          this.$nextTick(() => {
            this.syncing = false;
          });
          continue;
        }
        items.push({ row, key: col.key, nv });
      }
      if (deletedEditAttempted) this.notifyDeletedDefectCannotEdit();
      if (!items.length && touchedPlaceholderIds.size === 0) return;
      const run = async () => {
        try {
          if (items.length) await this.persistSheetCellsAsync(items);
          const toEnqueue = [];
          for (const rid of touchedPlaceholderIds) {
            const row = this.sheetRows.find((r) => r.$id === rid);
            if (!row || row.defectId) continue;
            if (this.excelPlaceholderRowAllRequiredFilled(row)) toEnqueue.push(row);
          }
          if (!toEnqueue.length) return;
          /* 先于 enqueue 置 syncing，避免 persist 结束到 drain 之间的空隙又进 handleSheetUpdate；与 axios 同 body 防重复无关 */
          this.syncing = true;
          const added = this.enqueueExcelDefectCreates(toEnqueue);
          if (added) await this.runExcelDefectCreateQueueDrain();
          else this.syncing = false;
        } catch (e) {
          /* persist / create 内已提示 */
          this.syncing = false;
        }
      };
      void run();
    },
    /**
     * 行号列多选后 Del/Backspace：确认后调接口删已落库缺陷，并从当前表移除占位行/数据行。
     */
    async handleExcelDeleteSelectedRows(rows) {
      if (!Array.isArray(rows) || !rows.length) return;
      const persisted = rows.filter((r) => r && r.defectId);
      if (!checkPermi(["system:defect:remove"])) {
        const currentUserId = this.$store.state.user.id;
        const allPersistedOwnedByCurrentUser = persisted.every((r) => r.createById == currentUserId);
        if (!allPersistedOwnedByCurrentUser) {
          this.$modal.msgError(errorCode["403"]);
          return;
        }
      }
      const remove$ids = new Set(rows.map((r) => String(r.$id)));
      try {
        await this.$modal.confirm(
          String(strFormat(this.$t("defect.excel-delete-rows-confirm-any"), rows.length)),
          String(this.$t("prompted")),
          {
            confirmButtonText: String(this.$t("delete")),
            cancelButtonText: String(this.$t("cancel")),
            type: "warning",
          }
        );
      } catch {
        return;
      }
      this.syncing = true;
      try {
        for (let i = 0; i < persisted.length; i++) {
          await delDefect(persisted[i].defectId);
        }
        const kept = (this.sheetRows || []).filter((r) => !remove$ids.has(String(r.$id)));
        const dataRows = kept.filter((r) => r.defectId);
        this.defectList = dataRows.map((r) => ({ ...r }));
        const dec = persisted.length;
        if (this.defectTotal != null && Number.isFinite(Number(this.defectTotal))) {
          this.defectTotal = Math.max(0, Number(this.defectTotal) - dec);
        }
        this.sheetRows = this.buildSheetRows(this.defectList);
        this.$nextTick(() => {
          const ed = this.$refs.excelEditor;
          if (ed && typeof ed.clearAllSelected === "function") ed.clearAllSelected();
          this.refreshExcelEditorView();
          this.scheduleExcelEditorLayoutFix();
        });
        this.$modal.msgSuccess(this.$t("delete.success").toString());
      } catch (e) {
        this.$modal.msgError(this.$t("defect.excel-save-failed").toString());
      } finally {
        this.syncing = false;
      }
    },
    /** 与 listDefect 行映射一致，供 search 与占位行新增后合并 */
    mapDefectRowForExcelSheet(r) {
      if (!r) return {};
      const stateKeys = ["PROCESSING", "AUDIT", "RESOLVED", "REJECTED", "CLOSED"];
      let defectStateCell = "";
      if (r.defectState != null && r.defectState !== "") {
        if (typeof r.defectState === "number" && Number.isFinite(r.defectState)) {
          defectStateCell = String(r.defectState);
        } else {
          const num = Number(r.defectState);
          if (Number.isFinite(num) && num >= 0 && num <= 4) defectStateCell = String(num);
          else {
            const name = r.defectStateName != null ? String(r.defectStateName) : String(r.defectState);
            const idx = stateKeys.indexOf(name);
            if (idx >= 0) defectStateCell = String(idx);
          }
        }
      }
      return {
        ...r,
        defectStateText: defectStateCell,
        excelHandleByMemberId: this.firstHandleByMemberId(r),
        createByText: this.formatCreateByText(r),
        excelImgUrlsText: "",
        excelAnnexUrlsText: "",
        planStartTime: r.planStartTime ? this.parseTime(r.planStartTime, "{y}-{m}-{d} {h}:{i}:{s}") : "",
        planEndTime: r.planEndTime ? this.parseTime(r.planEndTime, "{y}-{m}-{d} {h}:{i}:{s}") : "",
        updateTime: r.updateTime ? this.parseTime(r.updateTime, "{y}-{m}-{d} {h}:{i}:{s}") : "",
      };
    },
    /** 无 defectId 的占位行：COLS 中 required+editable 均已非空则可新建 */
    excelPlaceholderRowAllRequiredFilled(row) {
      if (!row || row.defectId) return false;
      for (let i = 0; i < COLS.length; i++) {
        const c = COLS[i];
        if (!c.required || !c.editable) continue;
        const v = row[c.key];
        if (v == null || String(v).trim() === "") return false;
      }
      return true;
    },
    /**
     * 将多行占位创建加入 FIFO 队列，状态列显示「排队」。
     * @returns {boolean} 是否新加入了至少一条（未入队去重）
     */
    enqueueExcelDefectCreates(rows) {
      if (!rows || !rows.length) return false;
      let added = false;
      for (let i = 0; i < rows.length; i++) {
        const row = rows[i];
        if (!row || row.defectId || !this.excelPlaceholderRowAllRequiredFilled(row)) continue;
        const idStr = row.$id != null && row.$id !== "" ? String(row.$id) : null;
        if (!idStr) continue;
        if (this.excelDefectCreateQueueIds.includes(idStr)) continue;
        this.excelDefectCreateQueueIds.push(idStr);
        if (row.defectStateText !== EXCEL_CREATE_ROW_RUNNING) {
          this.$set(row, "defectStateText", EXCEL_CREATE_ROW_QUEUED);
        }
        added = true;
      }
      if (added) this.refreshExcelEditorView();
      return added;
    },
    async runExcelDefectCreateQueueDrain() {
      if (!this.excelDefectCreateQueueIds.length) return;
      if (this.excelDefectCreateQueueDraining) return;
      this.excelDefectCreateQueueDraining = true;
      this.syncing = true;
      let ok = 0;
      try {
        while (this.excelDefectCreateQueueIds.length) {
          const idStr = this.excelDefectCreateQueueIds.shift();
          const row = this.sheetRows.find((r) => String(r.$id) === idStr);
          if (!row || row.defectId) continue;
          this.$set(row, "defectStateText", EXCEL_CREATE_ROW_RUNNING);
          this.refreshExcelEditorView();
          const success = await this.createDefectFromExcelPlaceholderRow(row, {
            queueMode: true,
            queueSilent: true,
          });
          if (success) ok++;
        }
      } finally {
        this.excelDefectCreateQueueDraining = false;
        this.syncing = false;
        this.refreshExcelEditorView();
      }
      if (ok > 0) {
        this.$modal.msgSuccess(String(strFormat(this.$t("defect.excel-create-batch-success"), ok)));
      }
    },
    /**
     * 占位行提交 addDefect。
     * @param {object} opts.queueMode 由队列串行调用：不抢全局 syncing、不弹单条成功
     * @param {object} opts.queueSilent 不弹 loading、不 toast 单条成功（失败仍提示）
     * @returns {Promise<boolean>} 是否创建成功
     */
    async createDefectFromExcelPlaceholderRow(row, opts) {
      const queueMode = !!(opts && opts.queueMode);
      const queueSilent = !!(opts && opts.queueSilent);
      if (!row || row.defectId) return false;
      const lockKey = row.$id != null && row.$id !== "" ? String(row.$id) : `idx-${this.sheetRows.indexOf(row)}`;
      if (this.excelPlaceholderRowSubmitLock[lockKey]) return false;
      if (!checkPermi(["system:defect:add"])) {
        this.$modal.msgError(this.$t("defect.excel-save-failed").toString());
        this.$set(row, "defectStateText", "");
        return false;
      }
      const projectId = row.projectId != null && row.projectId !== "" ? row.projectId : this.queryParams.projectId;
      if (projectId == null || projectId === "") {
        this.$modal.msgError(this.$t("defect.excel-save-failed").toString());
        this.$set(row, "defectStateText", "");
        return false;
      }
      this.$set(this.excelPlaceholderRowSubmitLock, lockKey, true);
      if (!queueMode) this.syncing = true;
      if (!queueSilent) this.$modal.loading(this.$t("upload.uploading").toString());
      try {
        const typeRaw = row.defectType == null ? "" : String(row.defectType).trim();
        const payload = {
          projectId: Number(projectId),
          defectType: typeRaw !== "" ? row.defectType : DEFAULT_DEFECT_TYPE,
          defectName: String(row.defectName).trim(),
          defectLevel: this.coerceDefectLevelValue(row.defectLevel),
          handleBy: row.excelHandleByMemberId ? [Number(row.excelHandleByMemberId)] : [],
        };
        if (row.defectDescribe != null && String(row.defectDescribe).trim() !== "")
          payload.defectDescribe = String(row.defectDescribe).trim();
        const mid = row.moduleId;
        if (mid != null && mid !== "" && Number.isFinite(Number(mid))) payload.moduleId = Number(mid);
        const mv = row.moduleVersion != null && String(row.moduleVersion).trim() !== "";
        if (mv) payload.moduleVersion = String(row.moduleVersion).trim();
        /* 关闭 axios 1s 内「同 URL + 同 body」防重复：多行粘贴相同缺陷字段时 body 会相同，串行仍会被误拦 */
        const res = await addDefect(payload, {
          headers: { repeatSubmit: false },
        });
        if (!res || res.code !== 200 || !res.data) {
          throw new Error((res && res.msg) || "addDefect");
        }
        let detail = res.data;
        const newId = detail.defectId;
        if (newId != null && newId !== "") {
          try {
            const detRes = await getDefect(newId);
            if (detRes && detRes.code === 200 && detRes.data) detail = detRes.data;
          } catch (_) {
            /* 保留 add 返回体，创建人/状态文案可能不完整 */
          }
        }
        const sheet = this.mapDefectRowForExcelSheet(detail);
        const keep$Id = row.$id;
        Object.keys(sheet).forEach((k) => {
          if (k === "$id") return;
          this.$set(row, k, sheet[k]);
        });
        if (keep$Id != null) this.$set(row, "$id", keep$Id);
        const mappedList = this.mapDefectRowForExcelSheet(detail);
        const idx = (this.defectList || []).findIndex((d) => d && d.defectId === row.defectId);
        if (idx >= 0) this.$set(this.defectList, idx, mappedList);
        else if (Array.isArray(this.defectList)) this.defectList.push(mappedList);
        this.refreshExcelEditorView();
        if (!queueSilent) this.$modal.msgSuccess(this.$t("create-success").toString());
        return true;
      } catch (e) {
        this.$modal.msgError(this.$t("defect.excel-save-failed").toString());
        this.$set(row, "defectStateText", "");
        return false;
      } finally {
        if (!queueSilent) this.$modal.closeLoading();
        if (!queueMode) this.syncing = false;
        this.$delete(this.excelPlaceholderRowSubmitLock, lockKey);
      }
    },
    persistSheetCells(items) {
      return this.persistSheetCellsAsync(items);
    },
    async persistSheetCellsAsync(items) {
      if (!items || !items.length) return;
      this.syncing = true;
      let ok = 0;
      try {
        for (let i = 0; i < items.length; i++) {
          const it = items[i];
          if (this.isDeletedDefectRow(it.row)) continue;
          try {
            const payload = {
              defectId: it.row.defectId,
              projectId: it.row.projectId,
            };
            if (it.key === "defectType") {
              const typeRaw = String(it.nv != null ? it.nv : "").trim();
              payload.defectType = typeRaw !== "" ? it.nv : DEFAULT_DEFECT_TYPE;
            } else if (it.key === "defectLevel") {
              payload.defectLevel = this.coerceDefectLevelValue(it.nv);
            } else if (it.key === "excelHandleByMemberId") {
              payload.handleBy = it.nv ? [Number(it.nv)] : [];
            } else if (it.key === "defectStateText") {
              const raw = String(it.nv != null ? it.nv : "").trim();
              if (raw === "") continue;
              if (raw === EXCEL_CREATE_ROW_QUEUED || raw === EXCEL_CREATE_ROW_RUNNING) continue;
              const n = Number(raw);
              if (!Number.isFinite(n) || n < 0 || n > 4) continue;
              payload.defectState = n;
              this.$set(it.row, "defectState", n);
            } else if (it.key === "planStartTime" || it.key === "planEndTime") {
              const nv = String(it.nv != null ? it.nv : "").trim();
              if (nv) {
                payload[it.key] = nv;
              } else {
                payload.params = {};
                if (it.key === "planStartTime") payload.params.clearPlanStartTime = true;
                else payload.params.clearPlanEndTime = true;
              }
            } else if (it.key === "moduleName") {
              /* 交付物仅通过三角 SelectModule 与 moduleId 同步，忽略单元格纯文本回写 */
              continue;
            } else {
              payload[it.key] = it.nv;
            }
            await updateDefect(payload);
            this.touchRowUpdateTime(it.row);
            ok++;
          } catch (e) {
            this.$modal.msgError(this.$t("defect.excel-save-failed").toString());
            return;
          }
        }
        if (ok > 0) {
          this.$modal.msgSuccess(this.$t("defect.excel-save-success").toString());
          this.refreshExcelEditorView();
        }
      } finally {
        this.syncing = false;
      }
    },
    /** 与字典项 value 类型对齐，避免 number / string 混用导致后端校验失败 */
    coerceDefectLevelValue(v) {
      const raw = v == null ? "" : String(v).trim();
      if (raw === "") return DEFAULT_DEFECT_LEVEL;
      const list = this.dict.type.defect_level || [];
      const hit = list.find((d) => String(d.value) === raw);
      if (hit) return hit.value;
      const num = Number(raw);
      return Number.isFinite(num) ? num : raw;
    },
    /** 与表格视图接口一致；不在此触发父级全量 search，避免权限/列表/成员接口连打 */
    refreshSearch() {
      this.refreshExcelEditorView();
    },
    touchRowUpdateTime(row) {
      if (!row || !row.defectId || typeof this.parseTime !== "function") return;
      this.$set(row, "updateTime", this.parseTime(new Date(), "{y}-{m}-{d} {h}:{i}:{s}"));
    },
  },
};
</script>

<style lang="scss" scoped>
.defect-excel-root {
  width: 100%;
  flex: 1 1 0%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
/* 与 .defect-vue-excel-editor .systable thead tr:first-child th 首行数据列表头一致（浅灰底、细边框、32px 高） */
.defect-excel-col-drag-ghost {
  position: fixed;
  z-index: 4000;
  pointer-events: none;
  box-sizing: border-box;
  height: 32px;
  min-height: 32px;
  max-height: 32px;
  padding: 2px 6px !important;
  margin: 0;
  background-color: #f5f7fa !important;
  border: 1px solid #ebeef5;
  border-radius: 0;
  font-size: 12px;
  font-weight: 500;
  color: #606266;
  line-height: 1.15;
  vertical-align: middle;
  text-align: center;
  text-shadow: none;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  opacity: 0.72;
}
.defect-excel-col-drag-ghost-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-width: 0;
  height: 100%;
  box-sizing: border-box;
  padding-right: 8px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #606266;
}
.defect-excel-inline-file-input {
  position: fixed;
  left: -9999px;
  width: 0;
  height: 0;
  opacity: 0;
  overflow: hidden;
}
.defect-excel-tools {
  flex-shrink: 0;
  width: 100%;
  /* 行内 flex 排布由父页 .defect-page .defect-view-toolbar 统一控制，右侧先换行 */
}
.defect-excel-tools-left {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  align-items: center;
}
.defect-excel-tools-right {
  flex: 0 0 auto;
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  > * {
    margin: 0 !important;
  }
}
.defect-excel-context {
  /* 缺陷页 Excel 模式：父级 defect-page 给 min-height + flex，此处撑满剩余高度 */
  flex: 1 1 0%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: transparent;
  padding-bottom: 2px;
  box-sizing: border-box;
}
/* 去掉外层边框，只保留库内 .component-content 一层 1px，避免「双边框」显粗 */
.defect-vue-excel-editor {
  flex: 0 0 auto;
  min-width: 0;
  max-width: 100%;
  border: none;
  align-self: stretch;
  box-sizing: border-box;
  display: block !important;
}
/* 纵向由 .table-content 滚动；隐藏库右侧假纵向条，避免误以为可上下拖 */
.defect-vue-excel-editor ::v-deep .v-scroll {
  display: none !important;
  pointer-events: none !important;
}
/* 与系统 Element 表格色系一致：浅边框 + 浅表头，避免过重 */
.defect-vue-excel-editor ::v-deep .component-content {
  border: 1px solid #dcdfe6;
  border-radius: 4px 4px 0 0;
  overflow: hidden;
  /* 勿 transform：会破坏库内 .autocomplete-results(position:fixed) 相对视口定位，下拉会跑偏 */
  max-width: 100%;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
}
/* 矩形选区 / 行选 / 填充预览：z 在序号列(11)之上、表头(16)之下；背景须透明，否则会盖住只读/交付物/序号列的格线 */
.defect-vue-excel-editor ::v-deep .cell-range-overlay,
.defect-vue-excel-editor ::v-deep .fill-drag-overlay {
  z-index: 13 !important;
  border: 2px solid #1890ff !important;
  box-shadow: none !important;
  outline: none !important;
  background: transparent !important;
}
.defect-vue-excel-editor ::v-deep .fill-drag-overlay {
  /* 填充预览略铺淡色即可，勿铺实底挡格线 */
  background: rgba(24, 144, 255, 0.06) !important;
}
/*
 * 活动格须高于选区(13)，且须低于吸顶表头(16)，滚动时不得盖住标题；
 * 编辑态仍用库内边框（input-square--editing），非编辑态矩形选区用 inset 与 overlay 一致。
 */
.defect-vue-excel-editor ::v-deep .input-square {
  z-index: 14 !important;
}
/* 勿再画蓝边：与 cell-range-overlay 叠成双框；库内已 border/background transparent，仅保留透明+无阴影 */
.defect-vue-excel-editor ::v-deep .input-square.input-square--range-suppress:not(.input-square--editing) {
  border-color: transparent !important;
  background: transparent !important;
  box-shadow: none !important;
  outline: none !important;
}
.defect-vue-excel-editor ::v-deep .table-content {
  text-shadow: none;
  font-size: 12px;
}
/* 库底部横向拖条为自定义 .h-scroll（table-content 原生条宽高为 0）；与 Cat2BugTable .cat2bug-custom-xbar 共用 token */
.defect-vue-excel-editor ::v-deep .footer {
  height: 8px !important;
  min-height: 8px !important;
  line-height: 1 !important;
  box-sizing: border-box;
  background-color: var(--cat2bug-xbar-track) !important;
  border-top: 1px solid var(--border-color-light) !important;
  overflow: visible !important;
}
.defect-vue-excel-editor ::v-deep .footer .left-block {
  height: 100% !important;
  max-height: 7px;
  box-sizing: border-box;
  background-color: var(--table-header-bg) !important;
  border-right: 1px solid var(--border-color-light) !important;
}
.defect-vue-excel-editor ::v-deep .footer .h-scroll {
  z-index: 2;
  height: 100% !important;
  max-height: 7px;
  box-sizing: border-box;
  background-color: var(--cat2bug-xbar-thumb) !important;
  border-radius: 3px !important;
}
.defect-vue-excel-editor ::v-deep .footer .h-scroll:hover,
.defect-vue-excel-editor ::v-deep .footer .h-scroll.focus,
.defect-vue-excel-editor ::v-deep .footer:hover .h-scroll {
  background-color: var(--cat2bug-xbar-thumb-hover) !important;
}
/* no-paging 时底部仍渲染英文 Selected|Filtered|Loaded，与本页无关，隐藏仅保留横条 */
.defect-vue-excel-editor ::v-deep .footer > span:last-child {
  display: none !important;
}
/* 库内 systable 为 z-index:-1，会把整块表压在选区 overlay 之下，格线像「消失」；提到 0 与 overlay 同属一层叠上下文后再靠透明 overlay 露格线 */
.defect-vue-excel-editor ::v-deep table.systable {
  z-index: 0 !important;
}
/* 仅表头需要高于选区 overlay(13)；tbody 勿写 z-index（无效且易误导） */
.defect-vue-excel-editor ::v-deep .systable thead th,
.defect-vue-excel-editor ::v-deep .systable thead td {
  border-color: #ebeef5 !important;
  z-index: 16 !important;
}
.defect-vue-excel-editor ::v-deep .systable th:not(:last-child) {
  border-color: #ebeef5 !important;
}
.defect-vue-excel-editor ::v-deep .systable tbody td {
  border-color: #ebeef5 !important;
}
.defect-vue-excel-editor ::v-deep .systable thead th {
  background-color: #f5f7fa !important;
  font-weight: 500;
  color: #606266;
  /* 库默认 s-resize（上下），本页不能纵向拖表头，改为默认；列宽仍靠 .col-sep 的 col-resize */
  cursor: default !important;
}
/* 数据列表头：左右拖调整列顺序（行号列、列宽条除外） */
.defect-vue-excel-editor ::v-deep .systable thead tr:first-child th:not(.first-col) {
  cursor: grab !important;
}
.defect-excel-context.defect-excel-col-dragging .defect-vue-excel-editor ::v-deep .systable thead tr:first-child th {
  cursor: grabbing !important;
}
/* 拖入列高亮蒙层（见 positionExcelColumnDropHighlightOverlay），默认隐藏 */
.defect-excel-col-drop-highlight-overlay {
  display: none;
  position: fixed;
  z-index: 3998;
  pointer-events: none;
  box-sizing: border-box;
  background: rgba(217, 236, 255, 0.58);
  opacity: 1;
  box-shadow: none;
  border: none;
}
/* 固定首行表头高度并水平居中：用 !important 压过库内写在内联上的 height，避免点击列头时 offsetHeight 反馈随机撑高 */
.defect-vue-excel-editor ::v-deep .systable thead tr:first-child {
  height: 32px !important;
  max-height: 32px !important;
}
.defect-vue-excel-editor ::v-deep .systable thead tr:first-child th {
  height: 32px !important;
  max-height: 32px !important;
  min-height: 32px !important;
  vertical-align: middle !important;
  text-align: center !important;
  box-sizing: border-box !important;
  line-height: 1.15;
  padding: 2px 6px !important;
  overflow: hidden;
  /* 须与库内一致为 sticky：勿用 relative，否则会取消表头吸顶且左上角无法横向冻结 */
  position: sticky !important;
  top: 0 !important;
}
.defect-vue-excel-editor ::v-deep .systable thead tr:first-child th.first-col {
  border-top-left-radius: 4px;
  top: 0 !important;
}
.defect-vue-excel-editor ::v-deep .systable thead tr:first-child th:last-child {
  border-top-right-radius: 4px;
}
.defect-vue-excel-editor ::v-deep .systable thead tr:first-child th > div:first-child {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  box-sizing: border-box;
  padding-right: 8px;
  text-align: center;
}
.defect-vue-excel-editor ::v-deep .systable thead tr:first-child th .table-col-header {
  text-align: center !important;
}
/* 左上角行号格：库内为 th > span，无包裹 div */
.defect-vue-excel-editor ::v-deep .systable thead tr:first-child th.first-col > span {
  display: flex;
  align-items: center;
  justify-content: center;
}
/* 与首行 th 同高（勿写死 26px）；库内 .col-sep 已为 position:absolute;height:100% */
.defect-vue-excel-editor ::v-deep .systable thead tr:first-child th .col-sep {
  height: 100% !important;
  max-height: none !important;
  min-height: 0;
  box-sizing: border-box !important;
  cursor: col-resize !important;
}
/* 左上角行号表头：无菜单/全选，默认光标；筛选行首格仍可点清除筛选 */
.defect-vue-excel-editor ::v-deep .systable thead th.first-col {
  background-color: #f5f7fa !important;
  color: #606266;
  cursor: default !important;
  position: sticky !important;
  left: 0 !important;
  /* 左上角：同时 sticky top+left，须高于同行其它 th，避免横向滚动时被列头压住 */
  z-index: 18 !important;
}
.defect-vue-excel-editor ::v-deep .systable thead td.first-col {
  background-color: #f5f7fa !important;
  color: #606266;
  cursor: pointer !important;
  position: sticky !important;
  left: 0 !important;
  z-index: 18 !important;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.first-col {
  background-color: #f5f7fa !important;
  color: #909399;
  cursor: pointer !important;
  user-select: none;
  position: sticky !important;
  left: 0 !important;
  z-index: 11 !important;
  /* 覆盖库内 th,td vertical-align: bottom 与 tbody tr text-align */
  vertical-align: middle !important;
  text-align: center !important;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.first-col > span {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  box-sizing: border-box;
  text-align: center;
}
.defect-vue-excel-editor ::v-deep .systable tbody tr:hover td.first-col {
  background-color: #f0f2f5 !important;
}
.defect-vue-excel-editor ::v-deep .systable tbody tr:hover td {
  background-color: #fafafa;
}
/* 整行选中：蓝色描边由库内 cell-range-overlay（TD 并集 bbox）绘制；勿对 tr 用 outline（高行时底边会短） */
.defect-vue-excel-editor ::v-deep .systable tbody tr.select td,
.defect-vue-excel-editor ::v-deep .systable tbody tr.select td.first-col {
  background-color: #ecf5ff !important;
}
.defect-vue-excel-editor ::v-deep .systable tbody tr.select:hover td,
.defect-vue-excel-editor ::v-deep .systable tbody tr.select:hover td.first-col {
  background-color: #d9ecff !important;
}
/* 只读数据格：浅红斜线底纹（行号列除外；::before 在内容之下，不挡三角/文字） */
.defect-vue-excel-editor ::v-deep .systable tbody td.readonly:not(.first-col) {
  position: relative;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.readonly:not(.first-col)::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  pointer-events: none;
  box-sizing: border-box;
  /* 仅用斜线，不用整块底色，避免盖住 td 上的计划时间下拉三角等 background-image */
  background-image: repeating-linear-gradient(
    -45deg,
    rgba(245, 108, 108, 0.14) 0,
    rgba(245, 108, 108, 0.14) 1px,
    transparent 1px,
    transparent 7px
  );
}
.defect-vue-excel-editor ::v-deep .systable tbody tr.select td.readonly:not(.first-col)::before {
  background-image: repeating-linear-gradient(
    -45deg,
    rgba(245, 108, 108, 0.1) 0,
    rgba(245, 108, 108, 0.1) 1px,
    transparent 1px,
    transparent 7px
  );
}
.defect-vue-excel-editor ::v-deep .systable tbody td.readonly:not(.first-col) .vue-excel-cell-html {
  position: relative;
  z-index: 1;
}
/* 覆盖 vue-excel-editor 默认 td vertical-align: bottom */
.defect-vue-excel-editor ::v-deep .systable tbody td[id$="-projectNum"] {
  vertical-align: middle !important;
  text-align: center;
}
/* 除行号、# 编号列外，数据格垂直居中（下拉/日期/cellHtml 见下条专门覆盖） */
.defect-vue-excel-editor ::v-deep .systable tbody td:not(.first-col):not([id$="-projectNum"]) {
  vertical-align: middle !important;
}
/* 纯文本列：单元格内保留 \\n 换行，行高随内容增高（缺陷名单独见下条；map 三角/日期/cellHtml 仍单行） */
.defect-vue-excel-editor ::v-deep .systable tbody td:not(.first-col):not([id$="-projectNum"]):not([id$="-defectName"]):not([id$="-moduleName"]):not([id$="-moduleVersion"]):not([id$="-defectDescribe"]):not([id$="-createByText"]):not([id$="-updateTime"]):not(.cell-html):not(.select):not(.datepick) {
  height: auto !important;
  min-height: 24px;
  white-space: pre-wrap !important;
  word-break: break-word;
  overflow-x: hidden;
  overflow-y: visible;
  text-overflow: clip;
  vertical-align: top !important;
}
/* 缺陷名称：换行与行高同其它纯文本列；不设 flex（会破坏 td 表格对齐），仅用 vertical-align 做格内上下居中 */
.defect-vue-excel-editor ::v-deep .systable tbody td[id$="-defectName"]:not(.first-col) {
  height: auto !important;
  min-height: 24px;
  white-space: pre-wrap !important;
  word-break: break-word;
  overflow-x: hidden;
  overflow-y: visible;
  text-overflow: clip;
  vertical-align: middle !important;
  box-sizing: border-box;
}
/* 交付物(moduleName)、版本(moduleVersion)：与缺陷名称一致，格内上下居中 */
.defect-vue-excel-editor ::v-deep .systable tbody td[id$="-moduleName"]:not(.first-col),
.defect-vue-excel-editor ::v-deep .systable tbody td[id$="-moduleVersion"]:not(.first-col) {
  height: auto !important;
  min-height: 24px;
  white-space: pre-wrap !important;
  word-break: break-word;
  overflow-x: hidden;
  overflow-y: visible;
  text-overflow: clip;
  vertical-align: middle !important;
  box-sizing: border-box;
}
.defect-vue-excel-editor ::v-deep .systable tbody td[id$="-defectDescribe"]:not(.first-col) {
  height: auto !important;
  min-height: 24px;
  white-space: pre-wrap !important;
  word-break: break-word;
  overflow-x: hidden;
  overflow-y: visible;
  text-overflow: clip;
  vertical-align: middle !important;
  box-sizing: border-box;
}
/* 创建人、更新时间：只读单行为主，行变高时格内上下居中 */
.defect-vue-excel-editor ::v-deep .systable tbody td[id$="-createByText"]:not(.first-col),
.defect-vue-excel-editor ::v-deep .systable tbody td[id$="-updateTime"]:not(.first-col) {
  height: auto !important;
  min-height: 24px;
  white-space: nowrap;
  overflow-x: hidden;
  overflow-y: visible;
  text-overflow: ellipsis;
  vertical-align: middle !important;
  box-sizing: border-box;
}
/* 内联编辑框：库默认 nowrap 会压扁换行观感，改为与单元格一致的 pre-wrap */
.defect-vue-excel-editor ::v-deep textarea.input-box {
  white-space: pre-wrap !important;
  word-break: break-word;
  overflow-x: hidden !important;
  overflow-y: auto !important;
}
/* 与补丁 .input-square--defect-name 一致：宿主提高优先级，凡可编辑非日期列编辑时文本在格内上下居中 */
.defect-vue-excel-editor ::v-deep .input-square.input-square--defect-name > div:first-child {
  display: flex !important;
  flex-direction: column !important;
  justify-content: center !important;
  align-items: stretch !important;
  min-height: 0;
  box-sizing: border-box;
  padding: 2px 0 1px 0 !important;
}
.defect-vue-excel-editor ::v-deep .input-square.input-square--defect-name textarea.input-box {
  width: 100% !important;
  max-height: 100% !important;
  flex: 0 1 auto;
  box-sizing: border-box;
  line-height: 1.45;
  padding: 0.2rem 0.3rem !important;
}
.defect-vue-excel-editor ::v-deep .input-square.input-square--defect-name textarea.input-box:focus,
.defect-vue-excel-editor ::v-deep .input-square.input-square--defect-name textarea.input-box:active {
  box-shadow: none !important;
}
/* 库内 .input-square--defect-name + syncDefectNameInputBoxLayout(scrollHeight) */
/* 图片列 cellHtml：多图横向平铺，突破库 tbody td 单行与固定行高 */
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html {
  height: auto !important;
  min-height: 32px;
  white-space: normal !important;
  overflow-x: auto !important;
  overflow-y: visible !important;
  line-height: 0;
  padding-top: 2px !important;
  padding-bottom: 2px !important;
  text-align: left !important;
}
/* 两列 Grid：避免 flex-wrap 把上传钮挤到「下一行」贴底；右侧钮与左侧内容垂直居中 */
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-attach-cell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-attach-cell .defect-excel-img-row,
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-attach-cell .defect-excel-annex-stack {
  min-width: 0;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-inline-upload-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  /* 无按钮框：仅图标；字号与库内 td.select 三角（background-size: 8px）视觉重量对齐，el-icon 不宜再用 8px 字否则显小 */
  width: auto;
  height: auto;
  min-width: 18px;
  min-height: 18px;
  margin-top: 0;
  padding: 2px;
  border: none;
  border-radius: 0;
  color: #909399;
  cursor: pointer;
  font-size: 12px;
  line-height: 1;
  background: transparent;
  box-sizing: border-box;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-inline-upload-btn i {
  font-size: 12px;
  line-height: 1;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-inline-upload-btn:hover {
  color: #409eff;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-img-row {
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-start;
  gap: 4px;
  max-width: 100%;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-img-wrap {
  position: relative;
  flex: 0 0 auto;
  width: 60px;
  height: 60px;
  line-height: 0;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-img-remove {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  margin: 0;
  padding: 0;
  line-height: 1;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 0 2px 0 3px;
  cursor: pointer;
  box-sizing: border-box;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-img-remove:hover {
  background: #f56c6c;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-img-remove .el-icon-close {
  font-size: 11px;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-img-row--empty {
  min-height: 24px;
  min-width: 1px;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-img-tile {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 3px;
  flex: 0 0 auto;
  border: 1px solid #ebeef5;
  box-sizing: border-box;
  cursor: pointer;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-img-wrap .defect-excel-img-tile {
  display: block;
  width: 100%;
  height: 100%;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-annex-stack {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  line-height: 1.35;
  font-size: 12px;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-annex-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  min-width: 0;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-annex-item .defect-excel-annex-line {
  flex: 1 1 auto;
  min-width: 0;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-annex-remove {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  margin: 0;
  padding: 0;
  line-height: 1;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  border-radius: 2px;
  cursor: pointer;
  box-sizing: border-box;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-annex-remove:hover {
  background: #f56c6c;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-annex-remove .el-icon-close {
  font-size: 11px;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-annex-line {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
  cursor: pointer;
  color: #409eff;
  text-align: left;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-annex-line .el-icon-paperclip {
  flex-shrink: 0;
  font-size: 12px;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-annex-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-annex-line:hover .defect-excel-annex-label {
  text-decoration: underline;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-annex-line--placeholder {
  cursor: default;
  color: transparent;
  text-decoration: none;
  min-height: 1em;
}
.defect-vue-excel-editor ::v-deep .systable tbody td.cell-html .defect-excel-annex-line--placeholder:hover {
  text-decoration: none;
}
/* 列头仍可能进入 sort-asc / sort-des 态，去掉排序箭头提示 */
.defect-vue-excel-editor ::v-deep .systable thead th.sort-asc-sign,
.defect-vue-excel-editor ::v-deep .systable thead th.sort-des-sign {
  background-image: none !important;
}
.table-tools.row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8px;
}
.defect-excel-picker-head {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8px;
}
.defect-excel-picker-head h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
}
.defect-field-divider {
  margin: 8px 0;
}
.defect-column-picker {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 220px;
  max-height: 380px;
  overflow-y: auto;
  padding-right: 4px;
}
.defect-column-picker ::v-deep .el-checkbox {
  display: flex;
  align-items: center;
  margin-right: 0;
  white-space: nowrap;
}
.defect-column-picker ::v-deep .el-checkbox__input {
  flex-shrink: 0;
}
.defect-column-picker ::v-deep .el-checkbox__label {
  line-height: 1.4;
  padding-left: 8px;
}
/* 内置 Table Setting：Default 会 resetColumn([]) 清空 fields，与声明式列不兼容，隐藏该按钮；面板内导入/导出与本页流程无关，一并隐藏 */
.defect-vue-excel-editor ::v-deep #panelModal .panel-footer > button.panel-button:first-of-type {
  display: none !important;
}
.defect-vue-excel-editor ::v-deep #panelModal .panel-content > .panel-action {
  display: none !important;
}
/* 交付物：三角直达菜单时锚点无伪输入框，勿画边框（否则格与 popover 之间会出现一条灰线） */
.defect-excel-module-picker-anchor {
  padding: 0;
  margin: 0;
  box-sizing: border-box;
  background: transparent;
  border: none;
  box-shadow: none;
}
.defect-excel-module-picker-anchor ::v-deep .select-module-input {
  border: none;
  min-height: 32px;
}
/* 锚点完全不可见（含前缀时钟图标），仅保留 DOM 供程序 focus() 打开面板 */
.defect-excel-plan-time-picker-anchor {
  padding: 0;
  margin: 0;
  background: transparent;
  border: none;
  box-shadow: none;
  box-sizing: border-box;
  width: 1px;
  height: 1px;
  min-width: 0 !important;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
}
.defect-excel-plan-time-picker-anchor ::v-deep .el-date-editor.el-input {
  width: 1px;
  min-width: 0;
  height: 1px;
}
.defect-excel-plan-time-picker-anchor ::v-deep .el-date-editor .el-input__inner {
  width: 1px;
  min-width: 0;
  height: 1px;
  padding: 0;
  margin: 0;
  border: none;
  opacity: 0;
  pointer-events: none;
}
.defect-excel-plan-time-picker-anchor ::v-deep .el-date-editor .el-input__prefix,
.defect-excel-plan-time-picker-anchor ::v-deep .el-date-editor .el-input__suffix {
  display: none !important;
}
</style>

<!-- 下拉面板 append-to-body：底部栏布局与自定义「清除」对齐 -->
<style lang="scss">
/* 子组件内部 td 无宿主 scoped 属性：用根类压过 vue-excel-editor 默认 bottom，统一垂直居中 */
.defect-excel-root .defect-vue-excel-editor table.systable tbody td.first-col {
  vertical-align: middle !important;
  text-align: center !important;
}
.defect-excel-root .defect-vue-excel-editor table.systable tbody td:not(.first-col):not([id$="-projectNum"]) {
  vertical-align: middle !important;
}
.defect-excel-root .defect-vue-excel-editor table.systable tbody td:not(.first-col):not([id$="-projectNum"]):not([id$="-defectName"]):not([id$="-moduleName"]):not([id$="-moduleVersion"]):not([id$="-defectDescribe"]):not([id$="-createByText"]):not([id$="-updateTime"]):not(.cell-html):not(.select):not(.datepick) {
  height: auto !important;
  min-height: 24px;
  white-space: pre-wrap !important;
  word-break: break-word;
  overflow-x: hidden;
  text-overflow: clip;
  vertical-align: top !important;
}
.defect-excel-root .defect-vue-excel-editor table.systable tbody td[id$="-defectName"]:not(.first-col) {
  height: auto !important;
  min-height: 24px;
  white-space: pre-wrap !important;
  word-break: break-word;
  overflow-x: hidden;
  overflow-y: visible;
  text-overflow: clip;
  vertical-align: middle !important;
  box-sizing: border-box;
}
.defect-excel-root .defect-vue-excel-editor table.systable tbody td[id$="-moduleName"]:not(.first-col),
.defect-excel-root .defect-vue-excel-editor table.systable tbody td[id$="-moduleVersion"]:not(.first-col) {
  height: auto !important;
  min-height: 24px;
  white-space: pre-wrap !important;
  word-break: break-word;
  overflow-x: hidden;
  overflow-y: visible;
  text-overflow: clip;
  vertical-align: middle !important;
  box-sizing: border-box;
}
.defect-excel-root .defect-vue-excel-editor table.systable tbody td[id$="-defectDescribe"]:not(.first-col) {
  height: auto !important;
  min-height: 24px;
  white-space: pre-wrap !important;
  word-break: break-word;
  overflow-x: hidden;
  overflow-y: visible;
  text-overflow: clip;
  vertical-align: middle !important;
  box-sizing: border-box;
}
.defect-excel-root .defect-vue-excel-editor table.systable tbody td[id$="-createByText"]:not(.first-col),
.defect-excel-root .defect-vue-excel-editor table.systable tbody td[id$="-updateTime"]:not(.first-col) {
  height: auto !important;
  min-height: 24px;
  white-space: nowrap;
  overflow-x: hidden;
  overflow-y: visible;
  text-overflow: ellipsis;
  vertical-align: middle !important;
  box-sizing: border-box;
}
.defect-excel-root .defect-vue-excel-editor textarea.input-box {
  white-space: pre-wrap !important;
  word-break: break-word;
  overflow-x: hidden !important;
  overflow-y: auto !important;
}
.defect-excel-root .defect-vue-excel-editor .input-square.input-square--defect-name > div:first-child {
  display: flex !important;
  flex-direction: column !important;
  justify-content: center !important;
  align-items: stretch !important;
  min-height: 0;
  box-sizing: border-box;
  padding: 2px 0 1px 0 !important;
}
.defect-excel-root .defect-vue-excel-editor .input-square.input-square--defect-name textarea.input-box {
  width: 100% !important;
  max-height: 100% !important;
  flex: 0 1 auto;
  box-sizing: border-box;
  line-height: 1.45;
  padding: 0.2rem 0.3rem !important;
}
.defect-excel-root .defect-vue-excel-editor .input-square.input-square--defect-name textarea.input-box:focus,
.defect-excel-root .defect-vue-excel-editor .input-square.input-square--defect-name textarea.input-box:active {
  box-shadow: none !important;
}
.defect-excel-root .defect-vue-excel-editor table.systable tbody td.select:not(.readonly),
.defect-excel-root .defect-vue-excel-editor table.systable tbody td.datepick:not(.readonly) {
  vertical-align: middle !important;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='%23909399' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><polyline points='6 9 12 15 18 9'></polyline></svg>") !important;
  background-repeat: no-repeat !important;
  background-size: 12px 12px !important;
  background-position: right 6px center !important;
}
.defect-excel-root .defect-vue-excel-editor table.systable tbody td.cell-html {
  vertical-align: middle !important;
  text-align: left !important;
}
/* 计划开始/结束、交付物：与库内 td.select 相同三角；三角在 padding 区内，横向溢出仍裁切 */
.defect-excel-root .defect-vue-excel-editor table.systable tbody td[id$="-planStartTime"]:not(.first-col),
.defect-excel-root .defect-vue-excel-editor table.systable tbody td[id$="-planEndTime"]:not(.first-col),
.defect-excel-root .defect-vue-excel-editor table.systable tbody td[id$="-moduleName"]:not(.first-col) {
  position: relative !important;
  overflow-x: hidden !important;
  padding-right: 18px !important;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='%23909399' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><polyline points='6 9 12 15 18 9'></polyline></svg>") !important;
  background-repeat: no-repeat !important;
  background-size: 12px 12px !important;
  background-position: right 6px center !important;
  vertical-align: middle !important;
}
.defect-excel-plan-datetime-panel .el-picker-panel__footer {
  display: flex !important;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
  gap: 8px;
  line-height: normal;
  /* 覆盖主题里 text-align / font-size:0，否则「清除」与内置按钮间距不一致 */
  text-align: initial !important;
  font-size: 12px !important;
}
.defect-excel-plan-datetime-panel .el-picker-panel__footer > * {
  margin-left: 0 !important;
  margin-right: 0 !important;
  float: none !important;
}
.defect-excel-plan-datetime-panel .defect-excel-plan-footer-clear {
  display: inline-flex;
  align-items: center;
  line-height: 1;
}
.defect-excel-plan-datetime-panel .defect-excel-plan-footer-clear .el-button {
  vertical-align: middle;
  padding-left: 8px;
  padding-right: 8px;
}
/* 交付物三角弹层：顶侧气泡小尖角略靠左（相对居中再往「前」/单元格方向移） */
.defect-excel-module-picker-popper.el-popper[x-placement^="bottom"] .popper__arrow {
  left: calc(50% - 44px) !important;
  margin-right: 0 !important;
  margin-left: -6px !important;
}
</style>
