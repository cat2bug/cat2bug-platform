<template>
  <div class="app-container" ref="projectOptionSubMain">
    <el-row class="project-add-page-header project-add-page-header--with-filter project-option-sub-hint-back">
      <el-page-header @back="goBack" :content="$t('project.ai-account-manager')">
      </el-page-header>
    </el-row>
    <div class="ai-account-tools project-option-list-tools">
      <el-form
        :model="queryParams"
        ref="queryForm"
        size="small"
        :inline="true"
        v-show="showSearch"
        label-width="80px"
        class="left"
        :class="{ 'list-query-keyboard-nav': listQueryNavActive }"
      >
        <el-form-item label="" prop="accountName" class="list-query-nav-item project-ai-account-hint-query-name" data-query-key="accountName">
          <el-input
            prefix-icon="el-icon-setting"
            v-model="queryParams.accountName"
            placeholder="请输入配置名称"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="" prop="modelName" class="list-query-nav-item" data-query-key="modelName">
          <el-input
            prefix-icon="el-icon-coin"
            v-model="queryParams.modelName"
            placeholder="请输入模型名称"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
      </el-form>
      <div ref="aiAccountToolsRight" class="project-option-list-tools-right">
        <el-button
          class="project-ai-account-hint-create"
          type="primary"
          plain
          icon="el-icon-plus"
          size="small"
          @click="handleAdd"
          v-hasPermi="['ai:account:add']"
        >{{ $t('project.ai.new-account') }}</el-button>
      </div>
    </div>
    <el-table ref="aiAccountTable" v-loading="loading" :data="accountList" @selection-change="handleSelectionChange">
<!--      <el-table-column type="selection" width="55" align="center" />-->
      <el-table-column :label="$t('project.ai.account-name')" align="center" prop="accountName" width="200">
        <template slot-scope="scope">
          <span class="ai-account-row-kbd-hint-anchor">{{ scope.row.accountName }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('project.ai.url')" align="center" prop="aiUrl" />
      <el-table-column :label="$t('project.ai.model-name')" align="center" prop="modelName" width="200" />
      <el-table-column :label="$t('project.ai.max-token')" align="center" prop="maxCompletionTokens" width="100" />
      <el-table-column :label="$t('operate')" align="center" class-name="small-padding fixed-width" width="220">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-video-play"
            :loading="scope.row._testing === true"
            @click="handleTest(scope.row)"
            v-hasPermi="['ai:account:edit']"
          >{{ $t('project.ai.test') }}</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['ai:account:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['ai:account:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      class="project-ai-account-table-pagination"
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新建或修改 OpenAI 账号对话框 -->
    <el-dialog
      :title="title"
      :visible.sync="open"
      width="500px"
      append-to-body
      custom-class="cat2bug-form-shortcut-dialog"
      :close-on-press-escape="false"
      :before-close="onToolDialogBeforeClose"
      @opened="onToolDialogOpened"
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" class="project-ai-account-dialog-form">
        <el-form-item :label="$t('project.ai.account-name')" prop="accountName">
          <el-input v-model="form.accountName" placeholder="请输入账号名称" :maxlength="64" />
        </el-form-item>
        <el-form-item :label="$t('project.ai.url')" prop="aiUrl">
          <el-input v-model="form.aiUrl" placeholder="请输入AI服务网址" type="textarea" :rows="2" :maxlength="255"/>
        </el-form-item>
        <el-form-item :label="$t('project.ai.model-name')" prop="modelName">
          <el-input v-model="form.modelName" placeholder="请输入模型名称" :maxlength="255" />
        </el-form-item>
        <el-form-item :label="$t('project.ai.max-token')" prop="maxCompletionTokens">
          <el-input-number v-model="form.maxCompletionTokens" :min="1" :max="65536" label="请输入最大Token"></el-input-number>
        </el-form-item>
        <el-form-item :label="$t('project.ai.app-key')" prop="apiKey">
          <el-input v-model="form.apiKey" placeholder="请输入密钥" :maxlength="255" show-password />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button class="defect-kbd-hint-host" type="primary" @click="submitForm">
          确 定
          <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
        </el-button>
        <el-button @click="requestCloseToolDialog">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAccount, getAccount, delAccount, addAccount, updateAccount, testAccount } from "@/api/ai/AIAccount";
import projectOptionSubListKbd from '@/mixins/project-option-sub-list-kbd'
import defectToolDialogKbd from '@/mixins/defect-tool-dialog-kbd'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { PROJECT_OPTION_KBD_SCOPE } from '@/mixins/project-option-sub-kbd'
import { checkPermi } from '@/utils/permission'
import {
  assignRowHintLetters,
  collectHintLettersFromToolbar,
  getDefectTableScrollBody,
  isRowIntersectingContainer,
  resolveElTableRowData,
  resolveTableRowFirstColumnHintAnchor,
  resolveTableRowFirstColumnKbdBadgeLayout
} from '@/utils/defect-row-kbd-hints'

function validateAiUrl(rule, value, callback) {
  if (value === null || value === undefined || String(value).trim() === "") {
    callback();
    return;
  }
  const text = String(value).trim();
  try {
    const parsed = new URL(text);
    if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
      callback(new Error("AI服务网址须以 http:// 或 https:// 开头"));
      return;
    }
    if (!parsed.host) {
      callback(new Error("AI服务网址格式不正确"));
      return;
    }
    callback();
  } catch (e) {
    callback(new Error("AI服务网址格式不正确，请输入有效的 URL"));
  }
}

export default {
  name: "Account",
  mixins: [projectOptionSubListKbd, defectToolDialogKbd],
  data() {
    return {
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
      // OpenAI账号表格数据
      accountList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        modelName: null,
        projectId: null,
        accountName: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        aiUrl: [
          { required: true, message: "AI服务网址不能为空", trigger: "blur" },
          { validator: validateAiUrl, trigger: "blur" }
        ],
        modelName: [
          { required: true, message: "模型名称不能为空", trigger: "blur" }
        ],
        apiKey: [
          { required: true, message: "密钥不能为空", trigger: "blur" }
        ],
        projectId: [
          { required: true, message: "关联项目ID不能为空", trigger: "blur" }
        ],
        accountName: [
          { required: true, message: "账号名称不能为空", trigger: "blur" }
        ]
      }
    };
  },
  computed: {
    currentProjectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 供弹框快捷键 mixin 识别 open 状态 */
    dialogVisible: {
      get() {
        return this.open
      },
      set(val) {
        this.open = val
      }
    }
  },
  created() {
    this.getList();
  },
  watch: {
    open(val) {
      if (typeof this.$_syncFormShortcutBinding === 'function') {
        this.$_syncFormShortcutBinding(!!val)
      }
    }
  },
  methods: {
    shouldProjectOptionSubEscGoBack() {
      return !this.open
    },
    shortcutSave() {
      this.submitForm()
    },
    doCloseToolDialog() {
      this.open = false
      this.toolDialogCloseBaseline = null
      this.reset()
    },
    getFieldHintScrollContainer() {
      const form = this.$refs.form
      if (form && form.$el) {
        const body = form.$el.closest('.el-dialog__body')
        if (body) return body
      }
      return typeof this.getFieldHintContainer === 'function' ? this.getFieldHintContainer() : null
    },
    getListQueryNavItems() {
      return [
        { key: 'accountName' },
        { key: 'modelName' }
      ]
    },
    getListQueryNavToolbarRef() {
      return 'aiAccountToolsRight'
    },
    getProjectOptionSubPaginationSelector() {
      return '.project-ai-account-table-pagination'
    },
    getProjectOptionSubRegisterActions() {
      return [
        { key: 'create', defaultLetter: 'E', run: () => this.shortcutCreateAccount() }
      ]
    },
    getProjectOptionSubActionHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${PROJECT_OPTION_KBD_SCOPE}.${key}`, def)
      return [
        {
          key: 'create',
          letter: L('create', 'E'),
          badgeSelector: '.project-ai-account-hint-create',
          floatOffset: { placement: 'bottom-right-outset', outset: 2, dy: 5 },
          run: () => this.shortcutCreateAccount(),
          visible: () => checkPermi(['ai:account:add'])
        }
      ]
    },
    /** ⌘ 按住：表格可见行账号名称列动态徽标（1–9 优先，字母补位） */
    getPageDynamicActionHints(ctx) {
      const used = (ctx && ctx.usedLetters) ? new Set(ctx.usedLetters) : new Set()
      collectHintLettersFromToolbar(this.getPageActionHints()).forEach((ch) => used.add(ch))
      return this.buildAiAccountTableRowActionHints(used)
    },
    getPageActionHintScrollRoots() {
      const table = this.$refs.aiAccountTable
      if (!table || !table.$el) return []
      const bodyWrap = getDefectTableScrollBody(table.$el)
      return bodyWrap ? [bodyWrap] : []
    },
    buildAiAccountTableRowActionHints(usedLetters) {
      if (this.loading || this.open || !checkPermi(['ai:account:edit'])) return []
      const table = this.$refs.aiAccountTable
      if (!table || !table.$el) return []
      const bodyWrap = getDefectTableScrollBody(table.$el)
      if (!bodyWrap) return []
      const tableRoot = table.$el
      const list = this.accountList || []
      const seen = new Set()
      const anchors = []
      bodyWrap.querySelectorAll('tbody tr.el-table__row').forEach((tr, rowIndex) => {
        if (!isRowIntersectingContainer(tr, bodyWrap)) return
        const row = resolveElTableRowData(tr) || list[rowIndex]
        if (!row || row.accountId == null) return
        const accountId = String(row.accountId)
        if (seen.has(accountId)) return
        seen.add(accountId)
        const rowKey = { field: 'accountId', value: accountId }
        const layout = resolveTableRowFirstColumnKbdBadgeLayout(
          tr, tableRoot, '.ai-account-row-kbd-hint-anchor', rowKey
        )
        const anchor = tr.querySelector('.ai-account-row-kbd-hint-anchor') ||
          resolveTableRowFirstColumnHintAnchor(tr, tableRoot, '.ai-account-row-kbd-hint-anchor', rowKey)
        if (!anchor || !layout.rect) return
        anchors.push({
          anchor,
          getAnchorRect: () => layout.rect,
          floatOffset: layout.floatOffset,
          skipViewportCheck: true,
          run: () => {
            if (!checkPermi(['ai:account:edit'])) return
            this.handleUpdate(row)
          }
        })
      })
      const letters = assignRowHintLetters(anchors.length, usedLetters)
      return anchors.map((item, i) => ({
        ...item,
        letter: letters[i],
        key: `row-${i}`
      })).filter((item) => item.letter)
    },
    shortcutCreateAccount() {
      if (!checkPermi(['ai:account:add'])) return
      this.handleAdd()
    },
    /** 查询OpenAI账号列表 */
    getList() {
      this.loading = true;
      listAccount(this.queryParams).then(response => {
        this.accountList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮（保留供其他调用；弹框内请用 requestCloseToolDialog）
    cancel() {
      this.requestCloseToolDialog()
    },
    // 表单重置
    reset() {
      this.form = {
        accountId: null,
        aiUrl: null,
        modelName: null,
        maxCompletionTokens: 65536,
        apiKey: null,
        createBy: null,
        projectId: null,
        accountName: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.accountId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = this.$t("project.ai.new-account");
      this.$nextTick(() => {
        const btn = this.$el && this.$el.querySelector('.project-ai-account-hint-create')
        if (btn && typeof btn.blur === 'function') btn.blur()
      })
    },
    /** 测试 OpenAI 接口 */
    handleTest(row) {
      this.$set(row, "_testing", true);
      const showTestFail = (detail) => {
        const name =
          row.accountName && String(row.accountName).trim()
            ? row.accountName
            : String(this.$t("project.ai.test-account-id", { id: row.accountId }));
        this.$modal.msgError(
          String(this.$t("project.ai.test-failed-msg", { name, detail }))
        );
      };
      testAccount(row.accountId)
        .then((response) => {
          this.$modal.msgSuccess(response.msg || this.$t("project.ai.test-success"));
        })
        .catch((err) => {
          const detail =
            err && err.msg
              ? err.msg
              : err && err.message
                ? err.message
                : String(this.$t("project.ai.test-failed-unknown"));
          showTestFail(detail);
        })
        .finally(() => {
          this.$set(row, "_testing", false);
        });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const accountId = row.accountId || this.ids
      getAccount(accountId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改OpenAI账号";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.accountId != null) {
            updateAccount(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('modify-success'));
              this.open = false;
              this.getList();
            });
          } else {
            this.form.projectId = this.currentProjectId;
            addAccount(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('create-success'));
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const accountIds = row.accountId || this.ids;
      this.$modal.confirm('是否确认删除OpenAI账号编号为"' + accountIds + '"的数据项？').then(function() {
        return delAccount(accountIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('ai/account/export', {
        ...this.queryParams
      }, `account_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
<style lang="scss" scoped>
.ai-account-tools {
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
</style>
