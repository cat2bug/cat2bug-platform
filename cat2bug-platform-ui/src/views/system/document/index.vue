<template>
  <div class="app-container">
    <project-label />
    <div class="document-tools">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="120px">
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

      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-popover placement="top" trigger="click">
            <div class="doc-picker-head row">
              <i class="el-icon-s-fold"></i>
              <h4>{{ $t('display-field') }}</h4>
            </div>
            <el-divider class="doc-picker-divider"></el-divider>
            <el-checkbox-group v-model="columnPickerCheckedKeys" class="doc-picker-col" @change="onDocColumnPickerChange">
              <el-checkbox v-for="c in documentColumnPickerOptions" :key="c.key" :label="c.key">{{ $t(c.key) }}</el-checkbox>
            </el-checkbox-group>
            <el-button style="padding: 9px;" plain slot="reference" icon="el-icon-s-fold" size="small"></el-button>
          </el-popover>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="small"
            @click="handleAddDir"
            v-hasPermi="['system:document:add']"
          >{{ $t('doc.create-folder') }}</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="small"
            @click="handleAddFile"
            v-hasPermi="['system:document:add']"
          >{{ $t('doc.create-file') }}</el-button>
        </el-col>
      </el-row>
    </div>

    <cat2-bug-table
      ref="cat2BugTable"
      cache-key="document-table"
      :persist-sort="false"
      :columns="documentTableColumnDefaults"
      :data="documentList"
      :loading="loading"
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
        <el-table-column :label="$t('operate')" align="center" class-name="small-padding fixed-width no-drag" width="200" fixed="right">
          <template slot-scope="scope">
            <div v-show="scope.row.docType>-1">
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

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

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
    this.initFloatMenu();
  },
  // 移除滚动条监听
  destroyed() {
    this.$floatMenu.windowsDestory();
  },
  methods: {
    /** 初始化浮动菜单 */
    initFloatMenu() {
      this.$floatMenu.windowsInit(document.querySelector('.main-container'));
      this.$floatMenu.resetMenus([{
        id: 'addDocumentDir',
        name: 'doc.create-folder',
        visible: true,
        plain: true,
        type: 'primary',
        icon: 'add_folder',
        prompt: 'doc.create-folder',
        permissions: ['system:document:add'],
        click : this.handleAddDir
      },{
        id: 'addDocumentFile',
        name: 'doc.create-file',
        visible: true,
        plain: true,
        type: 'primary',
        icon: 'add_file',
        prompt: 'doc.create-file',
        permissions: ['system:document:add'],
        click : this.handleAddFile
      }]);
    },
    /** 获取项目ID */
    getProjectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    onDocTableColumnsChange(columns) {
      this.columnPickerCheckedKeys = columns.filter(c => c.visible && c.showInColumnPicker !== false).map(c => c.key);
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
.doc-picker-head.row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
}
.doc-picker-divider {
  margin: 8px 0;
}
.doc-picker-col {
  display: flex;
  flex-direction: column;
}
.document-tools {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  > * {
    display: inline-block;
    justify-content: flex-start;
    margin-bottom: 0px;
    ::v-deep .el-form-item {
      margin-bottom: 0px;
    }
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
  gap: 10px;
}
</style>
