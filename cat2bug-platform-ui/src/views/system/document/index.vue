<template>
  <div class="app-container case-body document-page document-list-layout project-list-page-host" ref="documentMain">
    <project-label class="document-project-label" />
    <div ref="documentTools" class="document-tools" :class="{ 'wrapped-tools': documentToolsWrapped }">
      <el-form class="left" :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="120px" :class="{ 'list-query-keyboard-nav': listQueryNavActive }">
        <el-form-item label="" prop="docName" class="document-hint-query list-query-nav-item" data-query-key="docName">
          <el-input
            v-model="queryParams.docName"
            :placeholder="$t('doc.please-enter-name')"
            :prefix-icon="$legacyIcon('el-icon-search')"
            clearable
            size="small"
            @input="handleQuery"
          />
        </el-form-item>
      </el-form>

      <div ref="documentToolsRight" class="document-tools-right">
          <el-popover
          :width="240"
            placement="top"
            trigger="click"
            popper-class="defect-column-picker-popover"
            v-model:visible="docColumnPickerVisible"
            @show="onColumnPickerPopoverShow"
            @hide="onColumnPickerPopoverHide"
          >
            <div class="defect-column-picker-head">
              <el-icon class="legacy-icon-inline"><component :is="$legacyIcon('el-icon-s-fold')" /></el-icon>
              <h4>{{ $t('display-field') }}</h4>
            </div>
            <el-divider class="defect-field-divider"></el-divider>
            <el-checkbox-group
              :key="'doc-colpick-' + docColumnPickerRev"
              v-model="columnPickerCheckedKeys"
              class="defect-column-picker"
              @change="onDocColumnPickerChange"
            >
              <el-checkbox v-for="c in documentColumnPickerOptions" :key="c.key" :value="c.key">{{ $t(c.key) }}</el-checkbox>
            </el-checkbox-group>
            <template #reference>
              <el-button class="document-list-hint-columns" style="padding: 9px;" plain :icon="$legacyIcon('el-icon-s-fold')" size="small"></el-button>
            </template>
          </el-popover>
          <el-button
            class="document-hint-batch-move"
            type="primary"
            plain
            size="small"
            :disabled="multiple"
            @click="handleBatchMove"
            v-hasPermi="['system:document:edit']"
          ><svg-icon class="button-icon" icon-class="file-move" />{{ $t('doc.batch-move') }}</el-button>
          <el-button
            class="document-hint-batch-delete"
            type="danger"
            plain
            size="small"
            :icon="$legacyIcon('el-icon-delete')"
            :disabled="multiple"
            @click="handleBatchDelete"
          >{{ $t('batch-delete') }}</el-button>
          <el-button
            class="document-hint-create-folder"
            type="primary"
            plain
            :icon="$legacyIcon('el-icon-plus')"
            size="small"
            @click="handleAddDir()"
            v-hasPermi="['system:document:add']"
          >{{ $t('doc.create-folder') }}</el-button>
          <span v-hasPermi="['system:document:add']" class="document-create-dropdown-wrap">
            <el-dropdown
              class="document-hint-create-file document-create-dropdown cat2bug-split-dropdown-kbd"
              split-button
              trigger="click"
              size="small"
              type="primary"
              :disabled="creatingMarkdown"
              @click="handleCreateMarkdown"
            >
              <span><legacy-icon icon="el-icon-plus" />{{ $t('doc.create-file') }}</span>
              <template #dropdown>
                <el-dropdown-menu class="document-create-dropdown-menu">
                  <el-dropdown-item :disabled="creatingMarkdown" @click="handleCreateMarkdown">
                    <legacy-icon icon="el-icon-plus" />{{ $t('doc.create-file') }}
                  </el-dropdown-item>
                  <el-dropdown-item @click="handleAddFile">
                    <legacy-icon icon="el-icon-upload2" />{{ $t('doc.upload-file') }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </span>
      </div>
    </div>

    <div ref="documentListBody" class="document-list-body">
      <multipane
        layout="vertical"
        ref="multiPane"
        class="custom-resizer document-multipane"
        :class="{ 'custom-resizer--tree-hidden': !showFolderTree }"
        @paneResizeStop="dragStopHandle"
      >
        <div v-if="showFolderTree" ref="treeModule" class="document-tree-module" :style="treeModuleStyle">
          <document-folder-tree
            ref="documentFolderTree"
            :project-id="getProjectId()"
            :current-doc-id="currentFolderDocId"
            :show-sidebar-toggle="true"
            :toolbar-sync-height="documentTreeToolbarHeight"
            @toggle-sidebar="toggleFolderTreeVisible"
            @node-click="onFolderTreeNodeClick"
            @create-folder="handleAddDir"
            v-resize="setDragComponentSize"
          />
        </div>
        <multipane-resizer ref="paneResizer" v-show="showFolderTree" :style="[multipaneStyle, paneResizerRuleVars]" />
        <div ref="documentTableContext" class="document-table-context">
          <div class="document-table-x-scroll document-table-root">
            <cat2-bug-table
              ref="cat2BugTable"
              cache-key="document-table"
              sort-column-cache-key="document_table_sort_column_key"
              sort-type-cache-key="document_table_sort_type_key"
              :operate-column-max-width="320"
              :columns="documentTableColumnDefaults"
              :data="documentList"
              :loading="loading"
              :table-max-height="documentTableBodyMaxHeight"
              v-resize="setDragComponentSize"
              @selection-change="handleSelectionChange"
              @columns-change="onDocTableColumnsChange"
              @sort-change="handleSortChange"
            >
              <template #prepend>
                <el-table-column
                  v-if="!showFolderTree"
                  key="document-sidebar-expand-col"
                  fixed
                  width="30"
                  align="center"
                  label-class-name="tree-sidebar-expand-header-cell"
                  class-name="tree-sidebar-expand-body-cell"
                >
                  <template #header>
                    <el-tooltip :content="$t('doc.show-folder-tree')" placement="bottom">
                      <span
                        class="tree-sidebar-expand-trigger"
                        role="button"
                        tabindex="0"
                        @click.stop="toggleFolderTreeVisible"
                        @keyup.enter.stop.prevent="toggleFolderTreeVisible"
                      >
                        <svg-icon icon-class="menu" class-name="tree-sidebar-expand-svg" />
                      </span>
                    </el-tooltip>
                  </template>
                  <template #default>
                    <span class="tree-sidebar-expand-body-placeholder" />
                  </template>
                </el-table-column>
                <el-table-column type="selection" width="50" align="center" fixed :selectable="rowSelectable" />
              </template>
              <template #columns="{ scope, column }">
                <template v-if="column.prop==='docName'">
                  <div class="document-title">
                    <svg-icon class="file-icon" :icon-class="fileIcon(scope.row)" />
                    <el-link v-if="scope.row.docType<=0" @click="goFolder(scope.row)" type="primary" :title="scope.row.docType===-1 ? $t('doc.upper-level-dir') : scope.row.docName">
                      <template v-if="scope.row.docType===-1">{{ '... ' + $t('doc.upper-level-dir') }}</template>
                      <template v-else>{{ scope.row.docName }}</template>
                    </el-link>
                    <el-link v-else @click="handleView(scope.row)" type="primary" :title="scope.row.docName">{{ scope.row.docName }}</el-link>
                  </div>
                </template>
                <template v-else-if="column.prop==='fileExtension'">
                  <span v-if="scope.row.docType===0">{{ $t('folder') }}</span>
                  <span v-else>{{ scope.row.fileExtension }}</span>
                </template>
                <template v-else-if="column.prop==='updateByName'">
                  <el-tooltip v-if="scope.row.updateBy" class="item" effect="dark" :content="scope.row.updateBy" placement="top">
                    <cat2-bug-avatar :member="member(scope.row)" />
                  </el-tooltip>
                </template>
                <span v-else>{{ scope.row[column.prop] }}</span>
              </template>
              <template #append>
                <el-table-column :label="$t('operate')" align="left" class-name="no-drag cat2bug-operate-column" fixed="right">
                  <template #default="scope">
                    <div v-show="scope.row.docType>-1" class="cat2bug-operate-tools">
                      <el-button
                        v-if="scope.row.docType==1"
                        size="small"
                        link
                        :icon="$legacyIcon('el-icon-edit')"
                        @click="handleDownload(scope.row)"
                        v-hasPermi="['system:document:export']"
                      >{{ $t('download') }}</el-button>
                      <el-button
                        size="small"
                        link
                        @click="handleOpenMoveDialog(scope.row)"
                        v-hasPermi="['system:document:edit']"
                      ><svg-icon class="button-icon" icon-class="file-move"></svg-icon>{{ $t('move') }}</el-button>
                      <el-button
                        size="small"
                        link
                        class="green"
                        :icon="$legacyIcon('el-icon-edit')"
                        @click="handleUpdate(scope.row)"
                        v-hasPermi="['system:document:edit']"
                      >{{ $t('update') }}</el-button>
                      <el-button
                        size="small"
                        link
                        :icon="$legacyIcon('el-icon-delete')"
                        class="red"
                        @click="handleDelete(scope.row)"
                        v-if="hasDeletePermi(scope.row)"
                      >{{ $t('delete') }}</el-button>
                    </div>
                  </template>
                </el-table-column>
              </template>
            </cat2-bug-table>
          </div>
          <div v-show="total>0" ref="documentPaginationBand" class="document-table-pagination-band table-pagination-band">
            <pagination
              class="document-table-pagination"
              :total="total"
              v-model:page="queryParams.pageNum"
              v-model:limit="queryParams.pageSize"
              @pagination="onPagination"
            />
          </div>
        </div>
      </multipane>
    </div>

    <!-- 添加或修改文档对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body :close-on-press-escape="false" :before-close="onToolDialogBeforeClose" @opened="onCreateDialogOpened">
      <!-- 单输入框回车会触发表单原生 submit → 整页刷新；须 prevent 并改走 submitForm -->
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" @submit.prevent="submitForm">
        <el-form-item :label="fileFieldName" prop="docName">
          <el-input
            ref="docNameInput"
            v-model="form.docName"
            :placeholder="$t('doc.please-enter-name')"
            maxlength="255"
            @keyup.enter="submitForm"
          />
        </el-form-item>
<!--        <el-form-item label="备注" prop="docRemark">-->
<!--          <el-input v-model="form.docRemark" placeholder="请输入备注" />-->
<!--        </el-form-item>-->
        <el-form-item v-if="form.docType==1" prop="fileUrl" class="cat2bug-form-item-upload">
          <template #label>{{ $t('file') }}</template>
          <file-upload v-model="form.fileUrl" :drag="true" :file-size="500" :limit="1" :file-type="[]" @upload="fileUploadHandle"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
        <el-button class="defect-kbd-hint-host" type="primary" native-type="button" @click="submitForm">
          {{ $t('ok') }}
          <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
        </el-button>
        <el-button native-type="button" @click="requestCloseToolDialog">{{ $t('cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 移动文档对话框 -->
    <el-dialog :title="$t('doc.move-to')" v-model="moveDialogVisible" width="500px" append-to-body :close-on-press-escape="false" :before-close="onToolDialogBeforeClose" @opened="onMoveDialogOpened">
      <el-form ref="moveForm" :model="folderForm" :rules="rules" label-width="0px" @submit.prevent>
        <el-form-item class="margin-bottom-0" label="" prop="docId">
          <el-tree
            lazy
            :load="loadNode"
            :props="defaultProps"
            accordion
            @node-click="handleNodeClick"
            node-key="docId">
            <template #default="{ data }">
            <div class="span__" style="padding-left:6px;">
              <svg-icon icon-class="dir"></svg-icon>
              <span class="tree-node-text">{{ data.docName }}</span>
            </div>
            </template>
          </el-tree>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
        <el-button class="defect-kbd-hint-host" type="primary" native-type="button" :disabled="!isClickFolder" @click="handleMoveFolder">
          {{ $t('ok') }}
          <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
        </el-button>
        <el-button native-type="button" @click="requestCloseToolDialog">{{ $t('cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import ProjectLabel from "@/components/Project/ProjectLabel";
import Cat2BugAvatar from "@/components/Cat2BugAvatar";
import Cat2BugTable from "@/components/Cat2BugTable";
import DocumentFolderTree from "@/components/Document/DocumentFolderTree";
import { Multipane, MultipaneResizer } from "vue-multipane";
import { DocumentTableColumnDefaults } from "@/views/system/document/document-table-options";
import { listDocument, getDocument, delDocument, addDocument, updateDocument } from "@/api/system/document";
import {strFormat} from "@/utils";
import {toBase64} from "js-base64";
import {checkPermi} from "@/utils/permission";
import pageActionHints from '@/mixins/page-action-hints'
import listQueryKeyboardNav from '@/mixins/list-query-keyboard-nav'
import columnPickerPopoverKbd from '@/mixins/column-picker-popover-kbd'
import defectToolDialogKbd from '@/mixins/defect-tool-dialog-kbd'
import paneResizerHandleViewport from '@/mixins/paneResizerHandleViewport'
import multipaneTreeTableHeightSync from '@/mixins/multipaneTreeTableHeightSync'
import { serializeToolDialogFormCloseState } from '@/utils/defect-tool-dialog-close-state'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { getCurrentProjectId, getCurrentUserId } from '@/utils/store-context'
import { consumeDocumentReturnFolder } from '@/utils/document-folder-state'

const DOCUMENT_KBD_SCOPE = 'document'
const DOCUMENT_PAGE_SIZE_CACHE_KEY = 'document.pageSize'
const DOCUMENT_FOLDER_TREE_VISIBLE_CACHE_KEY = 'document_folder_tree_visible'
const DOCUMENT_FOLDER_TREE_WIDTH_CACHE_KEY = 'document_folder_tree_width'

export default {
  name: "Document",
  mixins: [
    pageActionHints,
    listQueryKeyboardNav,
    columnPickerPopoverKbd,
    defectToolDialogKbd,
    paneResizerHandleViewport,
    multipaneTreeTableHeightSync
  ],
  components: {
    ProjectLabel,
    Cat2BugAvatar,
    Cat2BugTable,
    DocumentFolderTree,
    Multipane,
    MultipaneResizer
  },
  directives: {
    resize: {
      mounted(el, binding) {
        let width = ""
        let height = ""
        function isResize() {
          const style = document.defaultView.getComputedStyle(el)
          if (width !== style.width || height !== style.height) {
            binding.value({ width: style.width, height: style.height })
          }
          width = style.width
          height = style.height
        }
        el.__vueSetInterval__ = setInterval(isResize, 300)
      },
      unmounted(el) {
        clearInterval(el.__vueSetInterval__)
      }
    }
  },
  data() {
    return {
      // 移动对话框是否显示
      moveDialogVisible: false,
      // 在移动时，记录是否点击了要移动的文件夹
      isClickFolder: false,
      /** 单条或批量移动的目标 id 列表 */
      moveTargetIds: [],
      // 文件夹表单
      folderForm: {},
      folderTree: [],
      currentFolder: null,
      showFolderTree: true,
      /** 与右侧列表表头行同高（px），测完后传给左侧树标题栏 */
      documentTreeToolbarHeight: null,
      treeModuleStyle: { "--treeModuleWidth": "280px" },
      multipaneStyle: {},
      defaultProps: {
        children: 'children',
        label: 'docName',
        isLeaf: 'leaf'
      },
      defaultExpandedKeys: [],
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 文档表格数据
      documentList: [],
      creatingMarkdown: false,
      documentTableColumnDefaults: DocumentTableColumnDefaults.map(c => ({ ...c })),
      columnPickerCheckedKeys: [],
      docColumnPickerVisible: false,
      docColumnPickerRev: 0,
      docPickerColumnList: null,
      documentToolsWrapped: false,
      documentTableBodyMaxHeight: null,
      // 弹出层标题
      title: "",
      fileFieldName: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        projectId: this.getProjectId(),
        docName: null,
        docType: null,
        fileExtension: null,
        createById: null,
        updateById: null,
        docPid: 0,
        orderByColumn: null,
        isAsc: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        docName: [
          { required: true, message: this.$i18n.t('doc.name-cannot-empty'), trigger: "change" }
        ],
        fileUrl: [
          { required: true, message: this.$i18n.t('doc.file-cannot-empty'), trigger: "change" }
        ]
      },
      // 文件扩展名，对应icon图标名
      fileExtensions: ['bmp','gif','jpg','pm4','pdf','doc','docx','xls','xlsx']
    };
  },
  computed: {
    $_formShortcutSurfaceVisible() {
      return this.open === true || this.moveDialogVisible === true
    },
    /** multipaneTreeTableHeightSync 约定名 */
    showModuleTree() {
      return this.showFolderTree
    },
    currentFolderDocId() {
      return this.currentFolder && this.currentFolder.docId > 0 ? this.currentFolder.docId : 0
    },
    paneResizerRuleVars() {
      const h = this.documentTreeToolbarHeight
      return {
        '--multipane-header-rule-offset': h != null && h > 0 ? `${h}px` : '48px'
      }
    },
    documentColumnPickerOptions() {
      const ordered = this.docPickerColumnList;
      if (ordered && ordered.length) {
        return ordered.map(c => ({ ...c }));
      }
      return DocumentTableColumnDefaults.filter(c => c.showInColumnPicker !== false);
    },
  },
  watch: {
    open(val) {
      this.$_syncFormShortcutBinding(!!val)
    },
    moveDialogVisible(val) {
      this.$_syncFormShortcutBinding(!!val)
    }
  },
  created() {
    this.queryParams.orderByColumn = this.$cache.local.get('document_table_sort_column_key') || null
    this.queryParams.isAsc = this.$cache.local.get('document_table_sort_type_key') || null
    const cachedSize = this.$cache.local.get(DOCUMENT_PAGE_SIZE_CACHE_KEY)
    if (cachedSize != null && cachedSize !== '') {
      const n = Number(cachedSize)
      if (Number.isFinite(n) && n > 0) {
        this.queryParams.pageSize = n
      }
    }
    const treeVis = this.$cache.local.get(DOCUMENT_FOLDER_TREE_VISIBLE_CACHE_KEY)
    this.showFolderTree = !(treeVis === '0' || treeVis === 'false')
    this.getFolderTreeWidth()
    this.restoreFolderThenList()
  },
  mounted() {
    this.$nextTick(() => {
      this.syncDocumentToolsWrapped();
      this.initDocumentListBodyResizeObserver();
      this.syncDocumentTableBodyMaxHeight();
      this.setDragComponentSize();
      this.initDocumentTableHeaderHeightSync();
    });
    window.addEventListener("resize", this.syncDocumentToolsWrapped);
    window.addEventListener("resize", this.syncDocumentTableBodyMaxHeight);
    this.registerDocumentShortcuts();
  },
  activated() {
    this.registerDocumentShortcuts();
  },
  deactivated() {
    if (this.$shortcut) this.$shortcut.unregisterPage(DOCUMENT_KBD_SCOPE);
  },
  // 移除滚动条监听
  destroyed() {
    window.removeEventListener("resize", this.syncDocumentToolsWrapped);
    window.removeEventListener("resize", this.syncDocumentTableBodyMaxHeight);
    this.destroyDocumentListBodyResizeObserver();
    this.destroyDocumentTableHeaderHeightSync();
    if (this.$shortcut) this.$shortcut.unregisterPage(DOCUMENT_KBD_SCOPE);
  },
  beforeDestroy() {
    this.destroyDocumentTableHeaderHeightSync();
    if (this.$shortcut) this.$shortcut.unregisterPage(DOCUMENT_KBD_SCOPE);
  },
  methods: {
    registerDocumentShortcuts() {
      if (!this.$shortcut) return
      this.$shortcut.registerPage(DOCUMENT_KBD_SCOPE, [
        { key: 'createFolder', defaultLetter: 'O', run: () => this.shortcutCreateFolder() },
        { key: 'createFile', defaultLetter: 'I', run: () => this.shortcutCreateFile() }
      ])
    },
    getPageActionHintContainer() {
      return this.$refs.documentMain || this.$el
    },
    getPageActionHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${DOCUMENT_KBD_SCOPE}.${key}`, def)
      return [
        {
          key: 'createFolder',
          letter: L('createFolder', 'O'),
          badgeSelector: '.document-hint-create-folder',
          floatOffset: { placement: 'bottom-right-outset', outset: 2, dy: 5 },
          run: () => this.shortcutCreateFolder(),
          visible: () => checkPermi(['system:document:add'])
        },
        {
          key: 'createFile',
          letter: L('createFile', 'I'),
          badgeSelector: '.document-hint-create-file',
          floatOffset: { placement: 'bottom-right-outset', outset: 2, dy: 5 },
          run: () => this.shortcutCreateFile(),
          visible: () => checkPermi(['system:document:add'])
        }
      ]
    },
    shortcutFocusQuery() {
      this.enterListQueryKeyboardNav()
    },
    getListQueryNavItems() {
      return [{ key: 'docName' }]
    },
    getListQueryNavToolbarRef() {
      return 'documentToolsRight'
    },
    getColumnPickerTriggerEl() {
      const root = this.getPageActionHintContainer()
      return root && root.querySelector('.document-list-hint-columns')
    },
    closeColumnPickerPopoverKbd() {
      this.docColumnPickerVisible = false
    },
    shortcutCreateFolder() {
      if (!checkPermi(['system:document:add'])) return
      this.handleAddDir()
    },
    shortcutCreateFile() {
      if (!checkPermi(['system:document:add'])) return
      this.handleCreateMarkdown()
    },
    shortcutGoUp() {
      if (!this.currentFolder || !this.currentFolder.docId) return
      this.goFolder({ docId: this.currentFolder.docPid })
    },
    shortcutChangePage(delta) {
      const root = this.getPageActionHintContainer()
      if (!root || typeof root.querySelector !== 'function') return
      const btn = root.querySelector(
        delta < 0 ? '.document-table-pagination .btn-prev' : '.document-table-pagination .btn-next'
      )
      if (btn && !btn.classList.contains('disabled') && typeof btn.click === 'function') {
        btn.click()
      }
    },
    initDocumentListBodyResizeObserver() {
      if (typeof ResizeObserver === 'undefined') return;
      this.destroyDocumentListBodyResizeObserver();
      const el = this.$refs.documentListBody;
      if (!el) return;
      this._documentListBodyResizeObserver = new ResizeObserver(() => {
        this.syncDocumentTableBodyMaxHeight();
      });
      this._documentListBodyResizeObserver.observe(el);
    },
    destroyDocumentListBodyResizeObserver() {
      if (this._documentListBodyResizeObserver) {
        this._documentListBodyResizeObserver.disconnect();
        this._documentListBodyResizeObserver = null;
      }
    },
    syncDocumentTableBodyMaxHeight() {
      this.$nextTick(() => {
        const body = this.$refs.documentListBody;
        if (!body || !body.clientHeight) return;
        const pagEl = this.$refs.documentPaginationBand;
        let reserveBelowTable = 0;
        if (this.total > 0 && pagEl && pagEl.offsetParent !== null) {
          const st = window.getComputedStyle(pagEl);
          reserveBelowTable =
            pagEl.offsetHeight +
            parseFloat(st.marginTop || "0") +
            parseFloat(st.marginBottom || "0");
        }
        const next = Math.max(120, Math.floor(body.clientHeight - reserveBelowTable - 2));
        if (this.documentTableBodyMaxHeight !== next) {
          this.documentTableBodyMaxHeight = next;
          this.$nextTick(() => {
            const tbl = this.$refs.cat2BugTable;
            if (tbl && typeof tbl.doLayout === "function") tbl.doLayout();
          });
        }
      });
    },
    syncDocumentToolsWrapped() {
      const measure = () => {
        const tools = this.$refs.documentTools;
        const left = tools && tools.querySelector(".left");
        const right = this.$refs.documentToolsRight;
        if (!tools || !left || !right) {
          this.documentToolsWrapped = false;
          return;
        }
        this.documentToolsWrapped = right.offsetTop - left.offsetTop > 4;
      };
      this.$nextTick(() => {
        if (this.documentToolsWrapped) {
          this.documentToolsWrapped = false;
          this.$nextTick(measure);
          return;
        }
        measure();
      });
    },
    /** 获取项目ID */
    getProjectId() {
      return getCurrentProjectId();
    },
    member(doc) {
      return {
        nickName: doc.updateBy,
        avatar: doc.updateByAvatar
      };
    },
    fileIcon(doc) {
      if (doc.docType === -1) {
        return 'root-dir';
      }
      if (doc.docType === 0) {
        return 'dir';
      }
      if (doc.fileExtension && this.fileExtensions.indexOf(doc.fileExtension) !== -1) {
        return doc.fileExtension;
      }
      return 'unknown-file';
    },
    hasDeletePermi(doc) {
      return doc.createById == getCurrentUserId() || checkPermi(['system:document:remove']);
    },
    onDocTableColumnsChange(columns) {
      this.docColumnPickerRev += 1;
      const picker = columns.filter(c => c.showInColumnPicker !== false).map(c => ({ ...c }));
      this.$set(this, 'docPickerColumnList', picker);
      this.columnPickerCheckedKeys = columns.filter(c => c.visible && c.showInColumnPicker !== false).map(c => c.key);
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout();
        this.syncDocumentTableBodyMaxHeight();
        this.syncDocumentTreeToolbarWithTableHeader();
      });
    },
    onDocColumnPickerChange(keys) {
      this.$refs.cat2BugTable && this.$refs.cat2BugTable.setColumnsVisible(keys);
    },
    /**
     * 仅从 MD 编辑「一次性返回标记」或本次路由 query.docPid 恢复文件夹；
     * 侧栏普通进入文档管理不带这些信息 → 根目录。
     */
    restoreFolderThenList() {
      const projectId = this.getProjectId()
      const once = consumeDocumentReturnFolder(projectId)
      if (once && once.docId > 0) {
        this.applyFolder(once)
        this.getList()
        return
      }
      const q = this.$route.query && this.$route.query.docPid
      const fromQuery = q != null && q !== '' ? Number(q) : NaN
      if (Number.isFinite(fromQuery) && fromQuery > 0) {
        getDocument(fromQuery).then(res => {
          const folder = res && res.data
          if (folder && folder.docId) {
            this.applyFolder(folder)
          }
          this.getList()
        }).catch(() => {
          this.getList()
        })
        return
      }
      this.applyFolder(null)
      this.getList()
    },
    applyFolder(folder) {
      if (!folder || !folder.docId || folder.docId <= 0) {
        this.currentFolder = null
        this.queryParams.docPid = 0
        this.$nextTick(() => {
          const tree = this.$refs.documentFolderTree
          if (tree && typeof tree.ensureFolderVisible === 'function') {
            tree.ensureFolderVisible(null)
          }
        })
        return
      }
      this.currentFolder = {
        docId: folder.docId,
        docPid: folder.docPid != null ? folder.docPid : 0,
        docName: folder.docName || '',
        docType: folder.docType != null ? folder.docType : 0
      }
      this.queryParams.docPid = folder.docId
      this.$nextTick(() => {
        const tree = this.$refs.documentFolderTree
        if (tree && typeof tree.ensureFolderVisible === 'function') {
          tree.ensureFolderVisible(this.currentFolder)
        }
      })
    },
    onFolderTreeNodeClick(folder) {
      this.reset()
      this.queryParams.docType = null
      this.queryParams.pageNum = 1
      this.applyFolder(folder)
      this.getList()
    },
    toggleFolderTreeVisible() {
      this.showFolderTree = !this.showFolderTree
      this.$cache.local.set(DOCUMENT_FOLDER_TREE_VISIBLE_CACHE_KEY, this.showFolderTree ? '1' : '0')
      this.$nextTick(() => {
        this.setDragComponentSize()
        if (this.$refs.cat2BugTable) {
          this.$refs.cat2BugTable.doLayout()
        }
        this.$nextTick(() => this.syncDocumentTreeToolbarWithTableHeader())
      })
    },
    syncDocumentTreeToolbarWithTableHeader() {
      this.$nextTick(() => {
        const cat = this.$refs.cat2BugTable
        const elTable = cat && cat.$refs && cat.$refs.elTable
        if (!elTable || !elTable.$el) {
          return
        }
        const headerWrap = elTable.$el.querySelector('.el-table__header-wrapper')
        if (!headerWrap) {
          return
        }
        if (
          this._documentTableHeaderObservedEl &&
          this._documentTableHeaderObservedEl !== headerWrap
        ) {
          this.destroyDocumentTableHeaderHeightSync()
          this.initDocumentTableHeaderHeightSync()
          return
        }
        const tr = headerWrap.querySelector('thead tr')
        if (!tr) {
          return
        }
        const rect = tr.getBoundingClientRect()
        const h = Math.round(rect.height || tr.offsetHeight || 0)
        if (h > 0 && h !== this.documentTreeToolbarHeight) {
          this.documentTreeToolbarHeight = h
        }
      })
    },
    initDocumentTableHeaderHeightSync() {
      this.destroyDocumentTableHeaderHeightSync()
      const cat = this.$refs.cat2BugTable
      const elTable = cat && cat.$refs && cat.$refs.elTable
      if (!elTable || !elTable.$el) {
        this.syncDocumentTreeToolbarWithTableHeader()
        return
      }
      const headerWrap = elTable.$el.querySelector('.el-table__header-wrapper')
      if (!headerWrap) {
        this.syncDocumentTreeToolbarWithTableHeader()
        return
      }
      this._documentTableHeaderObservedEl = headerWrap
      this.syncDocumentTreeToolbarWithTableHeader()
      if (typeof ResizeObserver === 'undefined') {
        return
      }
      this._documentTableHeaderResizeObserver = new ResizeObserver(() => {
        this.syncDocumentTreeToolbarWithTableHeader()
      })
      this._documentTableHeaderResizeObserver.observe(headerWrap)
    },
    destroyDocumentTableHeaderHeightSync() {
      if (this._documentTableHeaderResizeObserver) {
        this._documentTableHeaderResizeObserver.disconnect()
        this._documentTableHeaderResizeObserver = null
      }
      this._documentTableHeaderObservedEl = null
    },
    getFolderTreeWidth() {
      const w = this.$cache.session.get(DOCUMENT_FOLDER_TREE_WIDTH_CACHE_KEY)
      this.treeModuleStyle['--treeModuleWidth'] = (w ? w : 280) + 'px'
    },
    cacheFolderTreeWidth() {
      if (this.$refs.treeModule) {
        this.$cache.session.set(DOCUMENT_FOLDER_TREE_WIDTH_CACHE_KEY, this.$refs.treeModule.clientWidth)
      }
    },
    dragStopHandle() {
      if (this.showFolderTree) {
        this.cacheFolderTreeWidth()
      }
      this.setDragComponentSize()
    },
    setDragComponentSize() {
      this.syncDocumentTableBodyMaxHeight()
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout && this.$refs.cat2BugTable.doLayout()
        this.syncMultipaneTreeTableHeight('cat2BugTable')
        this.syncDocumentTreeToolbarWithTableHeader()
      })
    },
    onPagination() {
      this.$cache.local.set(DOCUMENT_PAGE_SIZE_CACHE_KEY, String(this.queryParams.pageSize))
      this.getList()
    },
    rowSelectable(row) {
      return row && row.docType > -1
    },
    folderTreeRef() {
      return this.$refs.documentFolderTree
    },
    /** @deprecated 全量刷新会收起树；优先用局部方法 */
    refreshFolderTree() {
      const tree = this.folderTreeRef()
      if (tree && typeof tree.invalidateAndRefresh === 'function') {
        tree.invalidateAndRefresh()
      }
    },
    notifyFolderCreated(folder) {
      const tree = this.folderTreeRef()
      if (tree && typeof tree.addFolderNode === 'function') {
        tree.addFolderNode(folder)
      }
    },
    notifyFoldersRemoved(docIds) {
      const tree = this.folderTreeRef()
      if (tree && typeof tree.removeFolderNodes === 'function') {
        tree.removeFolderNodes(docIds)
      }
    },
    notifyFolderRenamed(folder) {
      const tree = this.folderTreeRef()
      if (tree && typeof tree.renameFolderNode === 'function') {
        tree.renameFolderNode(folder)
      }
    },
    notifyFolderParentsReloaded(parentIds) {
      const tree = this.folderTreeRef()
      if (tree && typeof tree.reloadChildren === 'function') {
        tree.reloadChildren(parentIds)
      }
    },
    /** 查询文档列表 */
    getList() {
      this.loading = true;
      listDocument(this.queryParams).then(response => {
        const rows = response.rows;
        if(this.currentFolder && this.currentFolder.docId>0) {
          this.documentList = [...[{
            docId: this.currentFolder.docPid,
            docName: '',
            docType: -1
          }],...rows];
        } else {
          this.documentList = rows;
        }
        this.total = response.total;
        this.loading = false;
        this.syncDocumentToolsWrapped();
        this.$nextTick(() => {
          this.syncDocumentTableBodyMaxHeight();
          this.syncDocumentTreeToolbarWithTableHeader();
          this.initDocumentTableHeaderHeightSync();
        });
      });
    },
    /** 取消按钮 */
    cancel() {
      this.requestCloseToolDialog();
    },
    getActiveToolDialogForm() {
      if (this.moveDialogVisible) return this.folderForm
      return this.form
    },
    captureToolDialogCloseBaseline() {
      this.toolDialogCloseBaseline = serializeToolDialogFormCloseState(this.getActiveToolDialogForm())
    },
    isToolDialogCloseDirty() {
      if (!this.toolDialogCloseBaseline) return false
      return serializeToolDialogFormCloseState(this.getActiveToolDialogForm()) !== this.toolDialogCloseBaseline
    },
    doCloseToolDialog() {
      if (this.moveDialogVisible) {
        this.moveDialogVisible = false
        this.isClickFolder = false
      } else {
        this.open = false
        if (typeof this.reset === 'function') this.reset()
      }
      this.toolDialogCloseBaseline = null
    },
    onCreateDialogOpened() {
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          this.captureToolDialogCloseBaseline()
          this.$_focusToolDialogFirstField()
        })
      })
    },
    onMoveDialogOpened() {
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          this.captureToolDialogCloseBaseline()
        })
      })
    },
    shortcutSave() {
      if (this.moveDialogVisible) {
        if (this.isClickFolder) this.handleMoveFolder()
      } else if (this.open) {
        this.submitForm()
      }
    },
    getFieldHintContainer() {
      if (this.moveDialogVisible) {
        const form = this.$refs.moveForm
        return (form && form.$el) || this.$el
      }
      const form = this.$refs.form
      return (form && form.$el) || this.$el
    },
    /** 表单重置 */
    reset() {
      this.form = {
        docId: null,
        projectId: this.getProjectId(),
        docName: null,
        docType: null,
        fileExtension: null,
        createTime: null,
        createById: null,
        updateTime: null,
        updateById: null,
        fileVersion: null,
        docPid: 0,
        docRemark: null,
        fileUrl: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.queryParams.projectId = this.getProjectId();
      this.getList();
    },
    handleSortChange(column) {
      this.queryParams.orderByColumn = column.order ? column.prop : null
      this.queryParams.isAsc = column.order
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 多选框选中数据（排除「上级目录」行） */
    handleSelectionChange(selection) {
      const rows = (selection || []).filter(item => item && item.docType > -1)
      this.ids = rows.map(item => item.docId)
      this.single = rows.length !== 1
      this.multiple = !rows.length
    },
    handleView(doc) {
      const ext = String((doc && doc.fileExtension) || '').toLowerCase()
      if (ext === 'md' || ext === 'markdown') {
        const folderId = this.currentFolder && this.currentFolder.docId ? this.currentFolder.docId : 0
        this.$router.push({
          name: 'HandleMarkdownDocument',
          params: { docId: String(doc.docId) },
          query: folderId > 0 ? { docPid: String(folderId) } : {}
        })
        return
      }
      this.handleDownload(doc);
      // window.open('http://127.0.0.1:8012/onlinePreview?url='+encodeURIComponent(toBase64(doc.fileUrl)));
    },
    /** 新增文件夹；可从树节点传入父夹 */
    handleAddDir(parentFolder) {
      this.reset();
      this.form.docType = 0;
      if (parentFolder && parentFolder.docId != null && parentFolder.docId > 0) {
        this.form.docPid = parentFolder.docId;
      } else if (this.currentFolder && this.currentFolder.docId > 0) {
        this.form.docPid = this.currentFolder.docId;
      } else {
        this.form.docPid = 0;
      }
      this.open = true;
      this.title = this.$i18n.t('doc.create-folder');
      this.fileFieldName = this.$i18n.t('doc.folder-name');
      this.$nextTick(() => {
        this.$refs.docNameInput && this.$refs.docNameInput.focus();
      });
    },
    /** 上传文档（原新建上传对话框） */
    handleAddFile() {
      this.reset();
      this.form.docType=1;
      this.open = true;
      this.title = this.$i18n.t('doc.upload-file');
      this.fileFieldName = this.$i18n.t('doc.file-name');
      this.$nextTick(() => {
        this.$refs.docNameInput.focus();
      });
    },
    /** 在线新建空 Markdown 并进入编辑页 */
    handleCreateMarkdown() {
      if (!checkPermi(['system:document:add']) || this.creatingMarkdown) return
      const projectId = this.getProjectId()
      const docPid = this.currentFolder && this.currentFolder.docId ? this.currentFolder.docId : 0
      this.creatingMarkdown = true
      this.resolveDefaultMarkdownName(projectId, docPid).then(docName => {
        return             addDocument({
          projectId,
          docPid,
          docName,
          docType: 1,
          fileExtension: 'md',
          fileUrl: null
        }).then(response => {
          const docId = response && response.data
          if (!docId) {
            this.$modal.msgError(this.$t('exception'))
            return
          }
          this.$router.push({
            name: 'HandleMarkdownDocument',
            params: { docId: String(docId) },
            query: docPid > 0 ? { docPid: String(docPid) } : {}
          })
        })
      }).catch(() => {}).finally(() => {
        this.creatingMarkdown = false
      })
    },
    /** 默认名：新建文档 / 新建文档2 / 新建文档3… */
    resolveDefaultMarkdownName(projectId, docPid) {
      const base = this.$t('doc.default-md-name')
      return listDocument({
        projectId,
        docPid,
        pageNum: 1,
        pageSize: 9999
      }).then(response => {
        const rows = (response && response.rows) || []
        const names = new Set(rows.map(r => r && r.docName).filter(Boolean))
        if (!names.has(base)) {
          return base
        }
        let n = 2
        while (names.has(base + n)) {
          n += 1
        }
        return base + n
      })
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const docId = row.docId || this.ids
      getDocument(docId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = this.$i18n.t('doc.modify-file');
      });
    },
    /** 下载文件处理 */
    handleDownload(row) {
      this.$download.resource(row.fileUrl);
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // doCloseToolDialog → reset 会清空 form，须先记下
          const isFolder = this.form.docType === 0 || this.form.docType === '0'
          const snapshot = {
            docId: this.form.docId,
            docName: this.form.docName,
            docPid: this.form.docPid != null ? this.form.docPid : 0
          }
          if (this.form.docId != null) {
            updateDocument(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('modify-success'));
              this.doCloseToolDialog();
              this.getList();
              if (isFolder) {
                this.notifyFolderRenamed(snapshot);
              }
            });
          } else {
            if (this.form.docPid == null) {
              this.form.docPid = this.currentFolder && this.currentFolder.docId > 0
                ? this.currentFolder.docId
                : 0;
            }
            snapshot.docPid = this.form.docPid
            addDocument(this.form).then(response => {
              const newId = response && response.data
              this.$modal.msgSuccess(this.$i18n.t('create-success'));
              this.doCloseToolDialog();
              this.getList();
              if (isFolder && newId) {
                this.notifyFolderCreated({
                  docId: newId,
                  docName: snapshot.docName,
                  docPid: snapshot.docPid,
                  docType: 0,
                  leaf: true
                });
              }
            });
          }
        }
      });
    },
    /** 文件上传 */
    fileUploadHandle(res) {
      if(!res) return;
      if(!this.form.docName) {
        this.form.docName = res.originalFilename;
      }
      this.form.fileExtension = res.fileExtension;
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const docIds = row.docId || this.ids;
      const folderIds = row && row.docType == 0
        ? [row.docId]
        : (this.documentList || [])
            .filter(d => d && d.docType == 0 && (Array.isArray(docIds) ? docIds.includes(d.docId) : d.docId === docIds))
            .map(d => d.docId)
      let msg = null;
      if (row && row.docType == 0) {
        msg = strFormat(this.$i18n.t('doc.is-delete-dir'), row.docName);
      } else if (row && row.docName) {
        msg = strFormat(this.$i18n.t('doc.is-delete-file'), row.docName);
      } else {
        msg = this.$i18n.t('doc.is-batch-delete');
      }
      this.$modal.confirm(msg).then(function() {
        return delDocument(docIds);
      }).then(() => {
        this.getList();
        if (folderIds.length) {
          this.notifyFoldersRemoved(folderIds);
        }
        this.$modal.msgSuccess(this.$i18n.t('delete-success'));
      }).catch(() => {});
    },
    handleBatchDelete() {
      if (!this.ids || !this.ids.length) return
      const can = this.ids.every(id => {
        const row = this.documentList.find(d => d.docId === id)
        return row && this.hasDeletePermi(row)
      })
      if (!can) {
        this.$modal.msgWarning(this.$t('doc.batch-delete-no-permi'))
        return
      }
      const folderIds = (this.documentList || [])
        .filter(d => d && d.docType == 0 && this.ids.includes(d.docId))
        .map(d => d.docId)
      const ids = [...this.ids]
      this.$modal.confirm(this.$t('doc.is-batch-delete')).then(() => {
        return delDocument(ids)
      }).then(() => {
        this.getList()
        if (folderIds.length) {
          this.notifyFoldersRemoved(folderIds)
        }
        this.$modal.msgSuccess(this.$t('delete-success'))
      }).catch(() => {})
    },
    handleBatchMove() {
      if (!this.ids || !this.ids.length) return
      this.moveTargetIds = [...this.ids]
      this.folderForm = { docPid: null }
      this.isClickFolder = false
      this.moveDialogVisible = true
    },
    /** 跳转文件夹按钮操作 */
    goFolder(dir) {
      this.reset();
      this.queryParams.docType = null;
      this.queryParams.pageNum = 1;
      if (dir && dir.docType === -1) {
        if (dir.docId && dir.docId > 0) {
          getDocument(dir.docId).then(res => {
            const folder = res && res.data
            this.applyFolder(folder && folder.docId ? folder : { docId: dir.docId })
            this.getList()
          }).catch(() => {
            this.applyFolder({ docId: dir.docId })
            this.getList()
          })
        } else {
          this.applyFolder(null)
          this.getList()
        }
        return
      }
      if (dir && dir.docId) {
        this.applyFolder(dir)
      } else {
        this.applyFolder(null)
      }
      this.getList();
    },
    /** 加载文件夹树节点 */
    loadNode(node, resolve) {
      if(node && node.data) {
        this.handleNodeClick(node.data);
      }
      if (node.level === 0) {
        return resolve([{
          docId: -1,
          docName: this.$i18n.t('doc.root-folder'),
          docType: 0,
          isLeaf: true
        }]);
      }
      let params = {
        pageNum: 1,
        pageSize: 9999,
        docType: 0,
        projectId: this.getProjectId(),
        docPid: node.data.docId>-1?node.data.docId:0
      }
      listDocument(params).then(response => {
        resolve(response.rows.filter(d=>d.docId!=this.folderForm.docId));
      });
    },
    /** 编辑c2d文档 */
    handleCat2Doc(doc) {
      this.$router.push({name: 'HandleCat2Document', params: {docId: doc.docId}})
    },
    /** 打开移动对话框 */
    handleOpenMoveDialog(doc) {
      this.moveDialogVisible = true;
      this.folderForm = { ...doc };
      this.moveTargetIds = doc && doc.docId ? [doc.docId] : [];
      this.isClickFolder = false;
    },
    /** 取消移动对话框 */
    cancelFolderDialog() {
      this.requestCloseToolDialog();
    },
    /** 移动文件夹（单条或批量） */
    handleMoveFolder() {
      const targetPid = this.folderForm.docPid;
      if (targetPid == null) return;
      const ids = (this.moveTargetIds && this.moveTargetIds.length)
        ? this.moveTargetIds
        : (this.folderForm.docId ? [this.folderForm.docId] : []);
      if (!ids.length) return;
      // 不能移动到自己头上
      if (ids.some(id => id === targetPid)) {
        return;
      }
      const projectId = this.getProjectId();
      const fromPids = ids.map(id => {
        const row = this.documentList.find(d => d.docId === id)
        if (row && row.docPid != null) return row.docPid
        if (this.currentFolder && this.currentFolder.docId) return this.currentFolder.docId
        return 0
      })
      const movedFolderIds = ids.filter(id => {
        const row = this.documentList.find(d => d.docId === id)
        return row && row.docType == 0
      })
      Promise.all(ids.map(docId => updateDocument({
        docId,
        docPid: targetPid > 0 ? targetPid : 0,
        projectId
      }))).then(() => {
        this.$modal.msgSuccess(this.$i18n.t('doc.move-success'));
        this.doCloseToolDialog();
        this.moveTargetIds = [];
        this.getList();
        if (movedFolderIds.length) {
          // 源父夹去掉节点，目标父夹重载子列表（其它展开分支不动）
          this.notifyFoldersRemoved(movedFolderIds)
          this.notifyFolderParentsReloaded([targetPid, ...fromPids])
        }
      });
    },
    /** 移动树节点点击 */
    handleNodeClick(doc) {
      // 根节点 docId=-1 → 落到根 docPid=0
      this.folderForm.docPid = doc.docId > 0 ? doc.docId : 0;
      this.isClickFolder = true;
    }
  }
};
</script>
<style lang="scss" scoped>
@import "~@/assets/styles/multipane-resizer-grip.scss";

.app-container.case-body {
  padding: 20px 20px 0;
  box-sizing: border-box;
}
.document-page.document-list-layout {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  min-height: 0;
  flex: 1 1 auto;
  width: 100%;
  box-sizing: border-box;
}
.document-list-body {
  flex: 1 1 0%;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.custom-resizer.multipane.document-multipane {
  flex: 1 1 0%;
  width: 100%;
  min-width: 0;
  min-height: 0;
  height: auto;
  align-items: stretch;
  user-select: none;
}
.custom-resizer--tree-hidden > .document-table-context {
  flex: 1 1 100%;
  width: 100%;
}
.custom-resizer > .multipane-resizer {
  flex-shrink: 0;
  margin: 0 0 0 -8px;
  left: 4px;
  width: 8px;
  cursor: col-resize;
  position: relative;
  box-sizing: border-box;
  z-index: 350;
  background-image: linear-gradient(#dfe6ec, #dfe6ec);
  background-size: 100% 1px;
  background-position: 0 calc(var(--multipane-header-rule-offset, 48px) - 1px);
  background-repeat: no-repeat;
  @include multipane-resizer-vertical-appearance;
}
.document-tree-module {
  width: var(--treeModuleWidth);
  max-width: 75%;
  flex-shrink: 0;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  min-height: 0;
  box-sizing: border-box;
}
.document-tree-module :deep(> .document-folder-tree) {
  flex: 1 1 0%;
  min-height: 0;
}
.document-table-context {
  flex: 1 1 0%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
}
.document-table-x-scroll {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow-x: hidden;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
}
.document-table-root {
  width: 100%;
  max-width: 100%;
  min-width: 0;
}
.document-table-root :deep(.cat2-bug-table-wrap){
  width: 100%;
  max-width: 100%;
  min-width: 0;
}
.document-table-root :deep(.cat2-bug-table-wrap .el-table){
  width: 100% !important;
}
.document-table-root :deep(.el-table__body-wrapper),
.document-table-root :deep(.el-table__header-wrapper){
  overflow-x: hidden !important;
}
.document-table-root :deep(th.doc-name-col .cell){
  text-align: left !important;
  justify-content: flex-start !important;
}
.document-table-root :deep(th.doc-name-col .table-header),
.document-table-root :deep(th.doc-name-col .header-title){
  text-align: left !important;
  justify-content: flex-start !important;
}
.document-table-root :deep(td.doc-name-col .cell){
  display: flex !important;
  flex-direction: row !important;
  align-items: center !important;
  justify-content: flex-start !important;
  text-align: left !important;
}
.document-page :deep(h3.document-project-label){
  margin-top: 0;
  margin-bottom: 0;
}
.document-tools {
  width: 100%;
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: center;
  align-content: flex-start;
  column-gap: var(--cat2bug-toolbar-section-gap, 10px);
  row-gap: var(--cat2bug-toolbar-row-gap, 8px);
  margin-top: 0;
  margin-bottom: 0;
  .el-form-item {
    margin-bottom: 0;
  }
}
.document-tools > .left {
  flex: 1 1 auto;
  min-width: 0;
  max-width: 100%;
  width: auto;
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  row-gap: 8px;
  column-gap: var(--cat2bug-toolbar-item-gap, 10px);
  box-sizing: border-box;
  :deep(.el-form-item){
    margin-right: 0;
  }
}
.document-tools-right {
  flex: 0 0 auto;
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: center;
  gap: var(--cat2bug-toolbar-item-gap, 10px);
  margin-left: auto;
  >* {
    margin-left: 0;
  }
}
.document-tools.wrapped-tools {
  > .left {
    flex: 1 1 100%;
    width: 100%;
    max-width: 100%;
    display: block;
    box-sizing: border-box;
  }
  > .left :deep(.el-form-item){
    display: block;
    width: 100% !important;
    margin-right: 0;
    margin-bottom: 8px;
  }
  > .left :deep(.el-form-item:last-child){
    margin-bottom: 0;
  }
  > .left :deep(.el-form-item .el-form-item__content),
  > .left :deep(.el-form-item .el-input){
    width: 100% !important;
    max-width: 100%;
    box-sizing: border-box;
  }
  > .document-tools-right {
    margin-left: 0;
    flex: 1 1 100%;
    width: 100%;
    justify-content: flex-start;
  }
}
.file-icon {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
}
.red {
  color: #f56c6c;
}
.button-icon {
  margin-right: 3px;
}
.tree-node-text {
  margin-left: 5px;
}
.margin-bottom-0 {
  margin-bottom: 0px;
}
.document-title {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  gap: var(--cat2bug-toolbar-item-gap, 10px);
  width: 100%;
  max-width: 100%;
  min-width: 0;
  text-align: left;

  .file-icon {
    flex-shrink: 0;
  }

  .el-link {
    flex: 1 1 auto;
    min-width: 0;
    max-width: 100%;
    display: block;
    text-align: left !important;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    word-break: normal;
    -webkit-box-pack: start;
    -webkit-box-align: start;

    :deep(.el-link--inner){
      display: block;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      text-align: left;
    }
  }
}
.document-table-pagination-band {
  flex-shrink: 0;
}
/* 操作列：与缺陷/用例页对齐，避免 Safari 下表头与按钮被裁切 */
.document-table-root :deep(th.cat2bug-operate-column .cell){
  box-sizing: border-box;
  overflow: visible !important;
  flex-wrap: nowrap;
  white-space: nowrap;
  width: 100%;
  max-width: 100%;
  color: var(--table-header-color, inherit);
}
.document-table-root :deep(.cat2-bug-table-wrap:not([data-operate-at-cap='true']) td.cat2bug-operate-column .cell){
  box-sizing: border-box;
  overflow: hidden;
  flex-wrap: nowrap;
  white-space: nowrap;
}
.document-table-root :deep(td.cat2bug-operate-column .cat2bug-operate-tools){
  display: inline-flex;
  flex-direction: row;
  flex-wrap: nowrap !important;
  align-items: center;
  white-space: nowrap !important;
  box-sizing: border-box;
  width: auto !important;
  max-width: 100% !important;
}
.document-hint-create-folder,
.document-hint-create-file {
  position: relative;
  overflow: visible !important;
}
.document-create-dropdown-wrap {
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
}
.document-create-dropdown-menu :deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
