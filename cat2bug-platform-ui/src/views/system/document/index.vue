<template>
  <div class="app-container case-body document-page document-list-layout project-list-page-host">
    <project-label class="document-project-label" />
    <div ref="documentTools" class="document-tools" :class="{ 'wrapped-tools': documentToolsWrapped }">
      <el-form class="left" :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="120px">
        <el-form-item label="" prop="docName">
          <el-input
            v-model="queryParams.docName"
            :placeholder="$t('doc.please-enter-name')"
            clearable
            size="small"
            @input="handleQuery"
          />
        </el-form-item>
      </el-form>

      <div ref="documentToolsRight" class="document-tools-right">
          <el-popover placement="top" trigger="click">
            <div class="doc-picker-head row">
              <i class="el-icon-s-fold"></i>
              <h4>{{ $t('display-field') }}</h4>
            </div>
            <el-divider class="doc-picker-divider"></el-divider>
            <el-checkbox-group
              :key="'doc-colpick-' + docColumnPickerRev"
              v-model="columnPickerCheckedKeys"
              class="doc-picker-col"
              @change="onDocColumnPickerChange"
            >
              <el-checkbox v-for="c in documentColumnPickerOptions" :key="c.key" :label="c.key">{{ $t(c.key) }}</el-checkbox>
            </el-checkbox-group>
            <el-button style="padding: 9px;" plain slot="reference" icon="el-icon-s-fold" size="small"></el-button>
          </el-popover>
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="small"
            @click="handleAddDir"
            v-hasPermi="['system:document:add']"
          >{{ $t('doc.create-folder') }}</el-button>
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="small"
            @click="handleAddFile"
            v-hasPermi="['system:document:add']"
          >{{ $t('doc.create-file') }}</el-button>
      </div>
    </div>

    <div ref="documentListBody" class="document-list-body">
      <div class="document-table-x-scroll">
    <cat2-bug-table
      ref="cat2BugTable"
      cache-key="document-table"
      :persist-sort="false"
      :columns="documentTableColumnDefaults"
      :data="documentList"
      :loading="loading"
      :table-max-height="documentTableBodyMaxHeight"
      @selection-change="handleSelectionChange"
      @columns-change="onDocTableColumnsChange"
    >
      <template #columns="{ scope, column }">
        <template v-if="column.prop==='docName'">
          <div class="document-title">
            <svg-icon class="file-icon" :icon-class="fileIcon(scope.row)" />
            <el-link v-if="scope.row.docType<=0" @click="goFolder(scope.row)" type="primary">
              <template v-if="scope.row.docType===-1">{{ '... ' + $t('doc.upper-level-dir') }}</template>
              <template v-else>{{ scope.row.docName }}</template>
            </el-link>
            <el-link v-else @click="handleView(scope.row)" type="primary">{{ scope.row.docName }}</el-link>
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
          <template slot-scope="scope">
            <div v-show="scope.row.docType>-1" class="cat2bug-operate-tools">
              <el-button
                v-if="scope.row.docType==1"
                size="mini"
                type="text"
                icon="el-icon-edit"
                @click="handleDownload(scope.row)"
                v-hasPermi="['system:document:export']"
              >{{ $t('download') }}</el-button>
              <el-button
                size="mini"
                type="text"
                @click="handleOpenMoveDialog(scope.row)"
                v-hasPermi="['system:document:edit']"
              ><svg-icon class="button-icon" icon-class="file-move"></svg-icon>{{ $t('move') }}</el-button>
              <el-button
                size="mini"
                type="text"
                icon="el-icon-edit"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['system:document:edit']"
              >{{ $t('update') }}</el-button>
              <el-button
                size="mini"
                type="text"
                icon="el-icon-delete"
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

    <div v-show="total>0" ref="documentPaginationBand" class="document-table-pagination-band">
      <pagination
        class="document-table-pagination"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </div>
    </div>

    <!-- 添加或修改文档对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="fileFieldName" prop="docName">
          <el-input ref="docNameInput" v-model="form.docName" :placeholder="$t('doc.please-enter-name')" maxlength="255" />
        </el-form-item>
<!--        <el-form-item label="备注" prop="docRemark">-->
<!--          <el-input v-model="form.docRemark" placeholder="请输入备注" />-->
<!--        </el-form-item>-->
        <el-form-item v-if="form.docType==1" :label="$t('file')" prop="fileUrl">
          <file-upload v-model="form.fileUrl" :drag="true" :file-size="500" :limit="1" :file-type="[]" @upload="fileUploadHandle"/>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">{{ $t('ok') }}</el-button>
        <el-button @click="cancel">{{ $t('cancel') }}</el-button>
      </div>
    </el-dialog>

    <!-- 移动文档对话框 -->
    <el-dialog :title="$t('doc.move-to')" :visible.sync="moveDialogVisible" width="500px" append-to-body>
      <el-form ref="form" :model="folderForm" :rules="rules" label-width="0px">
        <el-form-item class="margin-bottom-0" label="" prop="docId">
          <el-tree
            lazy
            :load="loadNode"
            :props="defaultProps"
            accordion
            @node-click="handleNodeClick"
            node-key="docId">
            <div slot-scope="{ data }" class="span__" style="padding-left:6px;">
              <svg-icon icon-class="dir"></svg-icon>
              <span class="tree-node-text">{{ data.docName }}</span>
            </div>
          </el-tree>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :disabled="!isClickFolder" @click="handleMoveFolder">{{ $t('ok') }}</el-button>
        <el-button @click="cancelFolderDialog">{{ $t('cancel') }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import ProjectLabel from "@/components/Project/ProjectLabel";
import Cat2BugAvatar from "@/components/Cat2BugAvatar";
import Cat2BugTable from "@/components/Cat2BugTable";
import { DocumentTableColumnDefaults } from "@/views/system/document/document-table-options";
import { listDocument, getDocument, delDocument, addDocument, updateDocument } from "@/api/system/document";
import {strFormat} from "@/utils";
import {toBase64} from "js-base64";
import {checkPermi} from "@/utils/permission";

export default {
  name: "Document",
  components: { ProjectLabel, Cat2BugAvatar, Cat2BugTable },
  data() {
    return {
      // 移动对话框是否显示
      moveDialogVisible: false,
      // 在移动时，记录是否点击了要移动的文件夹
      isClickFolder: false,
      // 文件夹表单
      folderForm: {},
      folderTree: [],
      currentFolder: null,
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
      documentTableColumnDefaults: DocumentTableColumnDefaults.map(c => ({ ...c })),
      columnPickerCheckedKeys: [],
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
    documentColumnPickerOptions() {
      const ordered = this.docPickerColumnList;
      if (ordered && ordered.length) {
        return ordered.map(c => ({ ...c }));
      }
      return DocumentTableColumnDefaults.filter(c => c.showInColumnPicker !== false);
    },
    member: function () {
      return function (doc) {
        return {
          nickName: doc.updateBy,
          avatar: doc.updateByAvatar
        }
      }
    },
    fileIcon: function () {
      return function (doc) {
        if(doc.docType===-1) {
          return 'root-dir'
        } else if(doc.docType===0) {
          return 'dir'
        } else if (doc.fileExtension && this.fileExtensions.indexOf(doc.fileExtension)!==-1) {
          return doc.fileExtension;
        } else {
          return 'unknown-file';
        }
      }
    },
    hasDeletePermi: function() {
      return function (doc) {
        return doc.createById==this.$store.state.user.id || checkPermi(['system:document:remove']);
      }
    }
  },
  created() {
    this.getList();
  },
  mounted() {
    this.$nextTick(() => {
      this.syncDocumentToolsWrapped();
      this.initDocumentListBodyResizeObserver();
      this.syncDocumentTableBodyMaxHeight();
    });
    window.addEventListener("resize", this.syncDocumentToolsWrapped);
    window.addEventListener("resize", this.syncDocumentTableBodyMaxHeight);
  },
  // 移除滚动条监听
  destroyed() {
    window.removeEventListener("resize", this.syncDocumentToolsWrapped);
    window.removeEventListener("resize", this.syncDocumentTableBodyMaxHeight);
    this.destroyDocumentListBodyResizeObserver();
  },
  methods: {
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
    getProjectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    onDocTableColumnsChange(columns) {
      this.docColumnPickerRev += 1;
      const picker = columns.filter(c => c.showInColumnPicker !== false).map(c => ({ ...c }));
      this.$set(this, 'docPickerColumnList', picker);
      this.columnPickerCheckedKeys = columns.filter(c => c.visible && c.showInColumnPicker !== false).map(c => c.key);
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout();
        this.syncDocumentTableBodyMaxHeight();
      });
    },
    onDocColumnPickerChange(keys) {
      this.$refs.cat2BugTable && this.$refs.cat2BugTable.setColumnsVisible(keys);
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
        });
      });
    },
    /** 取消按钮 */
    cancel() {
      this.open = false;
      this.reset();
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
      this.queryParams.projectId = this.projectId;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.docId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    handleView(doc) {
      this.handleDownload(doc);
      // window.open('http://127.0.0.1:8012/onlinePreview?url='+encodeURIComponent(toBase64(doc.fileUrl)));
    },
    /** 新增文件夹按钮操作 */
    handleAddDir() {
      this.reset();
      this.form.docType=0;
      this.open = true;
      this.title = this.$i18n.t('doc.create-folder');
      this.fileFieldName = this.$i18n.t('doc.folder-name');
      this.$nextTick(() => {
        this.$refs.docNameInput.focus();
      });
    },
    /** 新增文件按钮操作 */
    handleAddFile() {
      this.reset();
      this.form.docType=1;
      this.open = true;
      this.title = this.$i18n.t('doc.create-file');
      this.fileFieldName = this.$i18n.t('doc.file-name');
      this.$nextTick(() => {
        this.$refs.docNameInput.focus();
      });
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
          if (this.form.docId != null) {
            updateDocument(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('modify-success'));
              this.open = false;
              this.getList();
            });
          } else {
            if(this.currentFolder) {
              this.form.docPid = this.currentFolder.docId;
            }
            addDocument(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('create-success'));
              this.open = false;
              this.getList();
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
      let msg = null;
      if(row.docType==0) {
        msg = strFormat(this.$i18n.t('doc.is-delete-dir'),row.docName);
      } else {
        msg = strFormat(this.$i18n.t('doc.is-delete-file'),row.docName);
      }
      this.$modal.confirm(msg).then(function() {
        return delDocument(docIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess(this.$i18n.t('delete-success'));
      }).catch(() => {});
    },
    /** 跳转文件夹按钮操作 */
    goFolder(dir) {
      this.reset();
      this.queryParams.docPid = dir.docId || 0;
      this.queryParams.docType = null;
      if(dir.docId) {
        this.currentFolder = dir;
      } else {
        this.currentFolder = null;
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
      this.folderForm = doc;
      this.isClickFolder = false;
    },
    /** 取消移动对话框 */
    cancelFolderDialog() {
      this.moveDialogVisible = false;
    },
    /** 移动文件夹 */
    handleMoveFolder() {
      // 不能移动到自己头上
      if(this.folderForm.docId==this.folderForm.docPid) {
        return;
      }
      updateDocument(this.folderForm).then(response => {
        this.$modal.msgSuccess(strFormat(this.$i18n.t('doc.move-success'),));
        this.moveDialogVisible = false;
        this.isClickFolder = false;
        this.getList();
      });
    },
    /** 移动树节点点击 */
    handleNodeClick(doc) {
      this.folderForm.docPid = doc.docId;
      this.isClickFolder = true;
    }
  }
};
</script>
<style lang="scss" scoped>
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
  --case-toolbar-v-gap: 8px;
}
.document-list-body {
  flex: 1 1 0%;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  --defect-pagination-v-gap: 28px;
}
.document-table-x-scroll {
  width: 100%;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
}
.doc-picker-head.row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: var(--cat2bug-toolbar-item-gap, 10px);

  > * {
    margin: 0;
  }
}
.doc-picker-head h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.3;
}
.doc-picker-divider {
  margin: 8px 0;
}
/** 与缺陷列表「显示字段」同款：用 gap 控制间距，去掉 checkbox 默认大块外边距 */
.doc-picker-col {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 220px;
  max-height: 380px;
  overflow-y: auto;
  padding-right: 4px;
}
.doc-picker-col ::v-deep .el-checkbox {
  display: flex;
  align-items: center;
  margin-right: 0;
  margin-bottom: 0;
  height: auto;
  line-height: 1.4;
  white-space: nowrap;
}
.doc-picker-col ::v-deep .el-checkbox__input {
  flex-shrink: 0;
}
.doc-picker-col ::v-deep .el-checkbox__label {
  line-height: 1.4;
  padding-left: 8px;
}
.document-page ::v-deep h3.document-project-label {
  margin-top: 0;
  margin-bottom: 10px;
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
  margin-top: var(--case-toolbar-v-gap, 8px);
  margin-bottom: var(--case-toolbar-v-gap, 8px);
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
  ::v-deep .el-form-item {
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
  > .left ::v-deep .el-form-item {
    display: block;
    width: 100% !important;
    margin-right: 0;
    margin-bottom: 8px;
  }
  > .left ::v-deep .el-form-item:last-child {
    margin-bottom: 0;
  }
  > .left ::v-deep .el-form-item .el-form-item__content,
  > .left ::v-deep .el-form-item .el-input {
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
  display: inline-flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  gap: var(--cat2bug-toolbar-item-gap, 10px);
}
/* 与缺陷列表 .defect-table-pagination-band 一致 */
.document-table-pagination-band {
  flex-shrink: 0;
  margin-top: var(--defect-pagination-v-gap);
  margin-bottom: calc(40px + env(safe-area-inset-bottom, 0px));
}
::v-deep .document-table-pagination.pagination-container {
  margin-top: 0 !important;
  margin-bottom: 0 !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  padding-left: 0 !important;
  padding-right: 0 !important;
}
</style>
