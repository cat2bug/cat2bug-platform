<template>
  <div class="app-container" ref="projectOptionSubMain">
    <el-row class="project-add-page-header project-add-page-header--with-filter project-option-sub-hint-back">
      <el-page-header @back="goBack" :content="$t('project.api-key')">
      </el-page-header>
    </el-row>
    <div class="api-tools project-option-list-tools">
      <el-form
        :model="queryParams"
        ref="queryForm"
        size="small"
        :inline="true"
        v-show="showSearch"
        label-width="68px"
        class="left"
        :class="{ 'list-query-keyboard-nav': listQueryNavActive }"
      >
        <el-form-item label="" prop="apiName" class="list-query-nav-item project-api-hint-query" data-query-key="apiName">
          <el-input
            v-model="queryParams.apiName"
            size="small"
            prefix-icon="el-icon-s-flag"
            :placeholder="$t('project.api.please-enter-name')"
            clearable
            @input="handleQuery"
          />
        </el-form-item>
      </el-form>

      <div ref="apiToolsRight" class="project-option-list-tools-right">
        <el-button
          class="project-api-hint-create"
          type="primary"
          plain
          icon="el-icon-plus"
          size="small"
          @click="handleAdd"
          v-hasPermi="['system:api:add']"
        >{{ $t('project.api.create-key') }}</el-button>
      </div>
    </div>
    <el-table v-loading="loading" :data="apiList" @selection-change="handleSelectionChange">
      <el-table-column :label="$t('project.api.name')" align="center" prop="apiName" />
      <el-table-column :label="$t('project.api.key')" align="center" prop="apiId">
        <template slot-scope="scope">
          <span :style="apiKeyVisibility[scope.row.apiId] ? 'font-family: monospace;' : 'font-family: monospace;'">
            {{ apiKeyVisibility[scope.row.apiId] ? scope.row.apiId : '•'.repeat(scope.row.apiId.length) }}
          </span>
          <el-button
            type="text"
            :icon="apiKeyVisibility[scope.row.apiId] ? 'el-icon-view' : 'el-icon-view'"
            @click="toggleApiKeyVisibility(scope.row.apiId)"
            style="margin-left: 8px;"
          ></el-button>
          <el-button
            type="text"
            icon="el-icon-document-copy"
            @click="copyApiKey(scope.row.apiId)"
            style="margin-left: 4px;"
          ></el-button>
        </template>
      </el-table-column>
<!--      <el-table-column :label="$t('project.api.white-list')" align="center" prop="whiteList" />-->
      <el-table-column :label="$t('project.api.enable-status')" align="center" prop="enabled" width="100">
        <template slot-scope="scope">
          <el-tag :type="(scope.row.enabled === true || scope.row.enabled === 1) ? 'success' : 'info'" size="small">
            {{ (scope.row.enabled === true || scope.row.enabled === 1) ? $t('enable') : $t('close') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('project.api.expire-time')" align="center" prop="expireTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.expireTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('remark')" align="center" prop="remark" />
      <el-table-column :label="$t('operate')" align="center" width="120" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:api:edit']"
          >{{ $t('modify') }}</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:api:remove']"
          >{{ $t('delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      class="project-api-table-pagination"
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改项目API对话框 -->
    <el-dialog
      :title="title"
      :visible.sync="open"
      width="800px"
      append-to-body
      custom-class="cat2bug-form-shortcut-dialog"
      :close-on-press-escape="false"
      :before-close="onToolDialogBeforeClose"
      @opened="onToolDialogOpened"
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" class="project-api-dialog-form">
        <el-form-item :label="$t('project.api.name')" prop="apiName">
          <el-input v-model="form.apiName" :placeholder="$t('project.api.please-enter-name')" maxlength="30" />
        </el-form-item>
        <el-form-item :label="$t('project.api.enable-status')" prop="enabled">
          <el-switch v-model="form.enabled"></el-switch>
          <span style="margin-left: 10px;">{{ form.enabled ? $t('enable') : $t('close') }}</span>
        </el-form-item>
<!--        <el-form-item :label="$t('project.api.white-list')" prop="whiteList">-->
<!--          <el-input v-model="form.whiteList" :placeholder="$t('project.api.please-enter-white-list')" />-->
<!--        </el-form-item>-->
        <el-form-item :label="$t('project.api.expire-time')" prop="expireTime">
          <el-date-picker clearable
            v-model="form.expireTime"
            type="date"
            value-format="yyyy-MM-dd"
            :placeholder="$t('project.api.please-select-expire-time')">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input type="textarea"
                    v-model="form.remark"
                    :placeholder="$t('please-enter-remark')"
                    maxlength="255"
                    rows="3"
                    show-word-limit />
        </el-form-item>

        <!-- 权限配置面板 -->
        <el-form-item :label="$t('project.api.permissions-config')" prop="permissions">
          <div class="permissions-config">
            <!-- 缺陷管理权限 -->
            <div class="permission-group">
              <div class="permission-group-header">
                <el-checkbox
                  v-model="checkAll.defect"
                  @change="handleCheckAllChange('defect', $event)"
                  :indeterminate="isIndeterminate('defect')">
                  {{ $t('project.api.defect-management') }}
                </el-checkbox>
              </div>
              <el-checkbox-group v-model="permissions.defect" class="permission-options">
                <el-checkbox label="list">{{ $t('project.api.permission.list') }}</el-checkbox>
                <el-checkbox label="query">{{ $t('project.api.permission.query') }}</el-checkbox>
                <el-checkbox label="create">{{ $t('project.api.permission.create') }}</el-checkbox>
                <el-checkbox label="update">{{ $t('project.api.permission.update') }}</el-checkbox>
                <el-checkbox label="repair">{{ $t('project.api.permission.repair') }}</el-checkbox>
                <el-checkbox label="assign">{{ $t('project.api.permission.assign') }}</el-checkbox>
                <el-checkbox label="reject">{{ $t('project.api.permission.reject') }}</el-checkbox>
                <el-checkbox label="pass">{{ $t('project.api.permission.pass') }}</el-checkbox>
                <el-checkbox label="close">{{ $t('project.api.permission.close') }}</el-checkbox>
                <el-checkbox label="open">{{ $t('project.api.permission.open') }}</el-checkbox>
              </el-checkbox-group>
            </div>

            <!-- 测试用例管理权限 -->
            <div class="permission-group">
              <div class="permission-group-header">
                <el-checkbox
                  v-model="checkAll.case"
                  @change="handleCheckAllChange('case', $event)"
                  :indeterminate="isIndeterminate('case')">
                  {{ $t('project.api.case-management') }}
                </el-checkbox>
              </div>
              <el-checkbox-group v-model="permissions.case" class="permission-options">
                <el-checkbox label="list">{{ $t('project.api.permission.list') }}</el-checkbox>
                <el-checkbox label="query">{{ $t('project.api.permission.query') }}</el-checkbox>
                <el-checkbox label="create">{{ $t('project.api.permission.create') }}</el-checkbox>
                <el-checkbox label="update">{{ $t('project.api.permission.update') }}</el-checkbox>
                <el-checkbox label="delete">{{ $t('project.api.permission.delete') }}</el-checkbox>
              </el-checkbox-group>
            </div>

            <!-- 交付物管理权限 -->
            <div class="permission-group">
              <div class="permission-group-header">
                <el-checkbox
                  v-model="checkAll.deliverable"
                  @change="handleCheckAllChange('deliverable', $event)"
                  :indeterminate="isIndeterminate('deliverable')">
                  {{ $t('project.api.deliverable-management') }}
                </el-checkbox>
              </div>
              <el-checkbox-group v-model="permissions.deliverable" class="permission-options">
                <el-checkbox label="list">{{ $t('project.api.permission.list') }}</el-checkbox>
                <el-checkbox label="query">{{ $t('project.api.permission.query') }}</el-checkbox>
                <el-checkbox label="create">{{ $t('project.api.permission.create') }}</el-checkbox>
              </el-checkbox-group>
            </div>

            <!-- 文件管理权限 -->
            <div class="permission-group">
              <div class="permission-group-header">
                <span class="permission-group-title">{{ $t('project.api.file-management') }}</span>
              </div>
              <el-checkbox-group v-model="permissions.file" class="permission-options">
                <el-checkbox label="upload">{{ $t('project.api.permission.upload') }}</el-checkbox>
              </el-checkbox-group>
            </div>

            <!-- 项目信息权限 -->
            <div class="permission-group">
              <div class="permission-group-header">
                <el-checkbox
                  v-model="checkAll.project"
                  @change="handleCheckAllChange('project', $event)"
                  :indeterminate="isIndeterminate('project')">
                  {{ $t('project.api.project-info') }}
                </el-checkbox>
              </div>
              <el-checkbox-group v-model="permissions.project" class="permission-options">
                <el-checkbox label="info">{{ $t('project.api.permission.info') }}</el-checkbox>
                <el-checkbox label="members">{{ $t('project.api.permission.members') }}</el-checkbox>
              </el-checkbox-group>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button class="defect-kbd-hint-host" type="primary" @click="submitForm">
          {{ $t('ok') }}
          <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
        </el-button>
        <el-button @click="requestCloseToolDialog">{{ $t('cancel') }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import ProjectLabel from "@/components/Project/ProjectLabel";
import { listApi, getApi, delApi, addApi, updateApi } from "@/api/system/api";
import projectOptionSubListKbd from '@/mixins/project-option-sub-list-kbd'
import defectToolDialogKbd from '@/mixins/defect-tool-dialog-kbd'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { PROJECT_OPTION_KBD_SCOPE } from '@/mixins/project-option-sub-kbd'
import { checkPermi } from '@/utils/permission'

export default {
  name: "Api",
  mixins: [projectOptionSubListKbd, defectToolDialogKbd],
  components: {ProjectLabel},
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
      // 项目API表格数据
      apiList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // API KEY 显示状态映射
      apiKeyVisibility: {},
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        projectId: null,
        userId: null,
        expireTime: null,
        apiName: null
      },
      // 表单参数
      form: {},
      // 权限常量定义
      defectActions: ['list', 'query', 'create', 'update', 'repair', 'assign', 'reject', 'pass', 'close', 'open'],
      caseActions: ['list', 'query', 'create', 'update', 'delete'],
      deliverableActions: ['list', 'query', 'create'],
      projectActions: ['info', 'members'],
      // 权限配置
      permissions: {
        defect: [],
        case: [],
        deliverable: [],
        file: [],
        project: []
      },
      // 全选状态
      checkAll: {
        defect: false,
        case: false,
        deliverable: false,
        project: false
      },
      // 表单校验
      rules: {
        apiName: [
          { required: true, message: this.$i18n.t('project.api.name--cannot-empty'), trigger: "blur" }
        ]
      }
    };
  },
  computed: {
    /** 获取项目id操作 */
    projectId() {
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
  watch: {
    open(val) {
      if (typeof this.$_syncFormShortcutBinding === 'function') {
        this.$_syncFormShortcutBinding(!!val)
      }
    },
    'permissions.defect': {
      handler() {
        this.updateCheckAllState('defect');
      },
      deep: true
    },
    'permissions.case': {
      handler() {
        this.updateCheckAllState('case');
      },
      deep: true
    },
    'permissions.deliverable': {
      handler() {
        this.updateCheckAllState('deliverable');
      },
      deep: true
    },
    'permissions.project': {
      handler() {
        this.updateCheckAllState('project');
      },
      deep: true
    }
  },
  created() {
    this.getList();
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
    serializeApiDialogCloseState() {
      return JSON.stringify({
        form: this.form,
        permissions: this.permissions
      })
    },
    captureToolDialogCloseBaseline() {
      this.toolDialogCloseBaseline = this.serializeApiDialogCloseState()
    },
    isToolDialogCloseDirty() {
      if (!this.toolDialogCloseBaseline) return false
      return this.serializeApiDialogCloseState() !== this.toolDialogCloseBaseline
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
      return [{ key: 'apiName' }]
    },
    getListQueryNavToolbarRef() {
      return 'apiToolsRight'
    },
    getProjectOptionSubPaginationSelector() {
      return '.project-api-table-pagination'
    },
    getProjectOptionSubRegisterActions() {
      return [
        { key: 'create', defaultLetter: 'E', run: () => this.shortcutCreateApi() }
      ]
    },
    getProjectOptionSubActionHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${PROJECT_OPTION_KBD_SCOPE}.${key}`, def)
      return [
        {
          key: 'create',
          letter: L('create', 'E'),
          badgeSelector: '.project-api-hint-create',
          floatOffset: { placement: 'bottom-right-outset', outset: 2, dy: 5 },
          run: () => this.shortcutCreateApi(),
          visible: () => checkPermi(['system:api:add'])
        }
      ]
    },
    shortcutCreateApi() {
      if (!checkPermi(['system:api:add'])) return
      this.handleAdd()
    },
    /** 查询项目API列表 */
    getList() {
      this.loading = true;
      this.queryParams.projectId = this.projectId;
      listApi(this.queryParams).then(response => {
        this.apiList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.requestCloseToolDialog()
    },
    // 表单重置
    reset() {
      this.form = {
        apiId: null,
        projectId: this.projectId,
        userId: null,
        whiteList: null,
        expireTime: null,
        remark: null,
        apiName: null,
        enabled: true,
        features: null
      };
      this.permissions = {
        defect: [],
        case: [],
        deliverable: [],
        file: [],
        project: []
      };
      this.checkAll = {
        defect: false,
        case: false,
        deliverable: false,
        project: false
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
      this.ids = selection.map(item => item.apiId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = this.$t('project.api.create-key');
      this.$nextTick(() => {
        const btn = this.$el && this.$el.querySelector('.project-api-hint-create')
        if (btn && typeof btn.blur === 'function') btn.blur()
      })
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const apiId = row.apiId || this.ids
      getApi(apiId).then(response => {
        this.form = response.data;
        // 处理 enabled 字段，确保为布尔类型
        if (this.form.enabled === null || this.form.enabled === undefined) {
          this.form.enabled = true;
        } else {
          // 确保转换为布尔类型（处理 0/1 或 true/false）
          this.form.enabled = this.form.enabled === true || this.form.enabled === 1 || this.form.enabled === '1';
        }
        // 加载权限配置
        this.loadPermissions();
        this.open = true;
        this.title = this.$t('modify');
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // 序列化权限配置
          this.form.features = this.buildFeaturesJson();

          if (this.form.apiId != null) {
            updateApi(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('modify-success'));
              this.open = false;
              this.getList();
            });
          } else {
            this.form.projectId = this.projectId;
            addApi(this.form).then(response => {
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
      const apiIds = row.apiId || this.ids;
      this.$modal.confirm(this.$i18n.t('project.api.is-delete')).then(function() {
        return delApi(apiIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess(this.$i18n.t('delete.success'));
      }).catch(() => {});
    },
    /** 切换 API KEY 显示/隐藏 */
    toggleApiKeyVisibility(apiId) {
      this.$set(this.apiKeyVisibility, apiId, !this.apiKeyVisibility[apiId]);
    },
    /** 复制 API KEY 到剪贴板 */
    copyApiKey(apiId) {
      const textarea = document.createElement('textarea');
      textarea.value = apiId;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.select();
      try {
        document.execCommand('copy');
        this.$modal.msgSuccess(this.$i18n.t('project.api.copy-success'));
      } catch (err) {
        this.$modal.msgError(this.$i18n.t('project.api.copy-failed'));
      }
      document.body.removeChild(textarea);
    },
    /** 处理全选操作 */
    handleCheckAllChange(module) {
      const actionsMap = {
        defect: this.defectActions,
        case: this.caseActions,
        deliverable: this.deliverableActions,
        project: this.projectActions
      };
      this.permissions[module] = this.checkAll[module] ? actionsMap[module] : [];
    },
    /** 更新全选状态 */
    updateCheckAllState(module) {
      const actionsMap = {
        defect: this.defectActions,
        case: this.caseActions,
        deliverable: this.deliverableActions,
        project: this.projectActions
      };
      const actions = actionsMap[module];
      this.checkAll[module] = this.permissions[module].length === actions.length;
    },
    /** 判断半选状态 */
    isIndeterminate(module) {
      const actionsMap = {
        defect: this.defectActions,
        case: this.caseActions,
        deliverable: this.deliverableActions,
        project: this.projectActions
      };
      const actions = actionsMap[module];
      const checkedCount = this.permissions[module].length;
      return checkedCount > 0 && checkedCount < actions.length;
    },
    /** 序列化权限为 JSON */
    buildFeaturesJson() {
      const features = {
        defect: this.permissions.defect,
        case: this.permissions.case,
        deliverable: this.permissions.deliverable,
        file: this.permissions.file,
        project: this.permissions.project
      };

      return JSON.stringify(features);
    },
    /** 反序列化 JSON 为权限选项 */
    loadPermissions() {
      if (!this.form.features) {
        return;
      }

      try {
        const features = JSON.parse(this.form.features);

        // 辅助函数：兼容数组和对象格式
        const loadModulePermissions = (module, actions) => {
          if (!features[module]) {
            return [];
          }
          // 如果是数组格式，直接返回
          if (Array.isArray(features[module])) {
            return features[module];
          }
          // 如果是对象格式，过滤出值为 true 的键
          return actions.filter(action => features[module][action]);
        };

        // 加载各模块权限
        this.permissions.defect = loadModulePermissions('defect', this.defectActions);
        this.permissions.case = loadModulePermissions('case', this.caseActions);
        this.permissions.deliverable = loadModulePermissions('deliverable', this.deliverableActions);
        this.permissions.file = loadModulePermissions('file', ['upload']);
        this.permissions.project = loadModulePermissions('project', this.projectActions);

        // 更新全选状态
        ['defect', 'case', 'deliverable', 'project'].forEach(module => {
          this.updateCheckAllState(module);
        });
      } catch (e) {
        console.error('Failed to parse features JSON:', e);
      }
    },
  }
};
</script>
<style scoped lang="scss">
.api-tools {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
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

.permissions-config {
  .permission-group {
    background-color: #F5F7FA;
    border: 1px solid #EBEEF5;
    border-radius: 4px;
    padding: 8px 12px;
    margin-bottom: 8px;

    &:last-child {
      margin-bottom: 0;
    }

    .permission-group-header {
      margin-bottom: 6px;
      font-weight: 500;
    }

    .permission-options {
      padding-left: 24px;
      display: flex;
      flex-wrap: wrap;
      gap: 4px 8px;

      ::v-deep .el-checkbox {
        margin-right: 0;
        height: 24px;
        line-height: 24px;
      }
    }
  }
}

.api-key-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;

  .api-key-text {
    font-family: monospace;
    font-size: 13px;
  }

  .api-key-btn {
    cursor: pointer;
    color: #409EFF;
    font-size: 16px;
    transition: color 0.3s;

    &:hover {
      color: #66B1FF;
    }

    &:active {
      color: #3A8EE6;
    }
  }
}
</style>
