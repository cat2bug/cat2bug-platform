<template>
  <div class="app-container project-manage-page" ref="projectManageMain">
    <el-tabs v-model="activeProjectTabName" @tab-click="selectProjectTabHandle">
      <el-tab-pane :label="$t('project.my-participated-in')" :name="$t('project.my-participated-in')"></el-tab-pane>
<!--      <el-tab-pane :label="$t('project.my-manage')" :name="$t('project.my-manage')"></el-tab-pane>-->
      <el-tab-pane :label="$t('project.all-project')" name=""></el-tab-pane>
<!--      <el-tab-pane :label="$t('project.archived-project')" :name="$t('project.archived-project')"></el-tab-pane>-->
    </el-tabs>
<!--    收藏列表-->
    <h4 v-show="collectList.length>0">{{$t('project.collect-project')}}</h4>
    <el-row v-show="collectList.length>0" class="project-collects" :gutter="10">
      <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6" v-for="project in collectList" :key="project.projectId">
        <el-card class="box-card project-block" @click="clickProject(project)">
          <template #header>
            <div class="clearfix project-collects-card-header">
              <project-nameplate :project="project"></project-nameplate>
              <star-switch v-model="project.collect" @change="clickCollectHandle($event, project, true)"></star-switch>
            </div>
          </template>
          <div class="project-collects-card-tools">
            <template v-if="collectShortcuts(project).length">
              <svg-icon
                v-for="item in collectShortcuts(project)"
                :key="item.module"
                :icon-class="item.icon"
                :title="$t(item.titleKey)"
                @click="goCollectShortcut($event, project, item)"
              />
            </template>
            <span v-else class="project-collect-no-permission">{{ $t('project.collect-no-permission') }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
<!--    项目列表-->
    <h4>{{$t('project.project-list')}}</h4>
    <div class="project-manage-tools">
      <el-form
        :model="queryParams"
        ref="queryForm"
        size="small"
        :inline="true"
        v-show="showSearch"
        label-width="0"
        class="left"
        :class="{ 'list-query-keyboard-nav': listQueryNavActive }"
      >
        <el-form-item prop="defectName" class="project-manage-hint-query list-query-nav-item" data-query-key="projectName">
          <el-input
            v-model="queryParams.projectName"
            :placeholder="$t('project.enter-project-name')"
            :prefix-icon="$legacyIcon('el-icon-search')"
            clearable
            size="small"
            @input="handleQuery"
          />
        </el-form-item>
      </el-form>
      <div ref="projectManageToolsRight" class="project-manage-tools-right">
        <el-button
          class="project-manage-hint-create"
          type="primary"
          plain
          :icon="$legacyIcon('el-icon-plus')"
          size="small"
          @click="handleAdd"
          v-hasPermi="['system:project:add']"
        >{{$t("project.create-project")}}</el-button>
      </div>
    </div>
    <el-table
      ref="projectTable"
      class="project-manage-table"
      v-loading="loading"
      :data="projectList"
      style="width: 100%"
    >
      <el-table-column
        :label="$t('project.name')"
        align="left"
        prop="projectName"
        class-name="project-name-col"
        min-width="280"
      >
        <template #default="scope">
          <span
            class="project-row-kbd-hint-anchor project-list-title"
            :class="{ 'is-project-link': canAccessProject(scope.row) }"
            @click="clickProject(scope.row)"
          >
            <project-nameplate :project="scope.row" />
          </span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('update-time')" align="left" prop="updateTime" width="140" min-width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('member')" align="center" prop="members" width="160" min-width="120">
        <template #default="scope">
          <row-list-member :members="scope.row.listShowMembers" />
        </template>
      </el-table-column>
      <!-- 勿用 cat2bug-operate-column：那是 Cat2BugTable 测宽专用，裸 el-table 会锁成 88px 挡住操作 -->
      <el-table-column
        :label="$t('operate')"
        align="left"
        class-name="no-drag project-manage-operate-col"
        width="200"
        fixed="right"
      >
        <template #default="scope">
          <div class="cat2bug-operate-tools">
            <star-switch
              v-if="visibleDelete(scope.row)"
              v-model="scope.row.collect"
              @change="clickCollectHandle($event, scope.row, false)"
            />
            <el-button
              v-if="visibleEdit(scope.row)"
              size="small"
              link
              class="green"
              :icon="$legacyIcon('el-icon-edit')"
              @click="handleEditBaseInfo($event, scope.row)"
            >{{ $t('modify') }}</el-button>
            <el-button
              v-if="visibleDelete(scope.row)"
              size="small"
              link
              class="red"
              :icon="$legacyIcon('el-icon-delete')"
              @click="handleDelete($event, scope.row)"
            >{{ $t('delete') }}</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      class="project-manage-table-pagination"
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改项目对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="form.projectName" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目图标" prop="projectIcon">
          <el-input v-model="form.projectIcon" placeholder="请输入项目图标地址" />
        </el-form-item>
        <el-form-item label="项目介绍" prop="projectIntroduce">
          <el-input v-model="form.projectIntroduce" placeholder="请输入项目介绍" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {listProject, getProject, delProject, addProject, updateProject, collectProject} from "@/api/system/project";
import { listProject as listAdminProject } from "@/api/admin/project";
import ProjectNameplate from "@/components/Project/ProjectNameplate";
import StarSwitch from "@/components/StarSwitch";
import RowListMember from "@/components/RowListMember";
import { strFormat } from "@/utils/index"
import i18n from "@/utils/i18n/i18n";
import { storeToRefs } from "pinia";
import { usePermissionStore } from "@/stores/permission";
import { useUserStore } from "@/stores/user";
import {checkPermi} from "@/utils/permission";
import pageActionHints from '@/mixins/page-action-hints'
import listQueryKeyboardNav from '@/mixins/list-query-keyboard-nav'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import {
  assignRowHintLetters,
  collectHintLettersFromToolbar,
  getDefectTableScrollBody,
  isRowIntersectingContainer,
  resolveElTableRowData,
  resolveTableRowFirstColumnHintAnchor,
  resolveTableRowFirstColumnKbdBadgeLayout
} from '@/utils/defect-row-kbd-hints'
import {
  canUseAdminProjectList,
  getCurrentTeamId,
  getCurrentUserId,
  resolveEffectiveTeamId
} from '@/utils/store-context'
import { buildProjectModulePath } from '@/utils/project-module-path'
import { projectOptionHref } from '@/utils/project-option-card'

const PROJECT_MANAGE_KBD_SCOPE = 'project-manage'

/** 收藏卡片快捷入口：与项目菜单 order_num 一致 */
const COLLECT_SHORTCUTS = Object.freeze([
  { module: 'dashboard', perms: ['system:dashboard:query'], icon: 'dashboard', titleKey: 'dashboard', fallbackName: 'ProjectDashboard' },
  { module: 'code', perms: ['system:git:repo:list'], icon: 'code', titleKey: 'git.repo.menu', fallbackName: 'ProjectGitRepo' },
  { module: 'case', perms: ['system:case:list'], icon: 'case', titleKey: 'case', fallbackName: 'ProjectCase' },
  { module: 'plan', perms: ['system:plan:list'], icon: 'time', titleKey: 'plan', fallbackName: 'ProjectPlan' },
  { module: 'defect', perms: ['system:defect:list'], icon: 'bug', titleKey: 'defect.manage', fallbackName: 'ProjectDefect' },
  { module: 'module', perms: ['system:module:list'], icon: 'cascader', titleKey: 'module', fallbackName: 'ProjectModule' },
  { module: 'document', perms: ['system:document:list'], icon: 'library', titleKey: 'doc', fallbackName: 'ProjectDocument' },
  { module: 'option', perms: ['system:project:option'], icon: 'system', titleKey: 'project.option', fallbackName: 'ProjectOption' }
])

export default {
  name: "Project",
  mixins: [pageActionHints, listQueryKeyboardNav],
  components: { ProjectNameplate, RowListMember,StarSwitch },
  setup() {
    const permissionStore = usePermissionStore()
    const { sidebarRouters } = storeToRefs(permissionStore)
    return { sidebarRouters }
  },
  data() {
    return {
      activeProjectTabName: this.$i18n.t('project.my-participated-in'),
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
      // 收藏的项目集合
      collectList: [],
      // 项目表格集合
      projectList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        params: {
          userId: null
        },
        /** 我参与的：后端按成员关系过滤（memberOnly=true） */
        memberOnly: true,
        pageNum: 1,
        pageSize: 10,
        projectName: null,
        createBy: null,
        teamId: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        projectName: [
          { required: true, message: "项目名称不能为空", trigger: "blur" }
        ],
      },
      /** 列表请求序号：忽略过期的 system/admin 交错响应，避免 total 与 rows 错位 */
      _listReqId: 0
    };
  },
  computed: {
    collect: function() {
      return function (project) {
        return project.collect?'true':'false';
      }
    },
    /** 获取当前用户id */
    currentUserId() {
      return getCurrentUserId();
    },
    /** 获取团队id */
    currentTeamId() {
      return getCurrentTeamId();
    },
    /** 是否显示添加按钮 */
    visibleAdd() {
      return function (project) {
        return checkPermi(['system:project:add']) && (parseInt(project.createBy) === this.currentUserId || project.members.filter(m=>m.userId == this.currentUserId).length>0)
      }
    },
    /** 是否显示删除按钮 */
    visibleDelete() {
      return function (project) {
        return checkPermi(['system:project:remove']) && this.isProjectMemberOrCreator(project)
      }
    },
    /** 是否显示编辑（跳转项目基本信息） */
    visibleEdit() {
      return function (project) {
        return checkPermi(['system:project:edit']) && this.isProjectMemberOrCreator(project)
      }
    },
    /** 是否可切换进入该项目（名称链接） */
    canAccessProject() {
      return function (project) {
        return this.hasCollectShortcut(project)
      }
    },
    /** 收藏卡片可见的快捷入口（按菜单顺序，有权限才显示） */
    collectShortcuts() {
      return function (project) {
        if (!this.isProjectMemberOrCreator(project)) return []
        return COLLECT_SHORTCUTS.filter(item => checkPermi(item.perms))
      }
    },
    /** 是否至少有一个收藏卡片快捷入口权限 */
    hasCollectShortcut() {
      return function (project) {
        return this.collectShortcuts(project).length > 0
      }
    }
  },
  created() {
    this.selectProjectTabHandle();
    this.getCollectList();
  },
  mounted() {
    this.registerProjectManageShortcuts();
    this.$nextTick(() => {
      this.$_bindListQueryNavFocusIn();
      this.$_bindListQueryNavToolbarFocusIn();
    });
  },
  activated() {
    this.registerProjectManageShortcuts();
    this.$nextTick(() => {
      this.$_bindListQueryNavFocusIn();
      this.$_bindListQueryNavToolbarFocusIn();
    });
  },
  deactivated() {
    if (this.$shortcut) this.$shortcut.unregisterPage(PROJECT_MANAGE_KBD_SCOPE);
  },
  beforeDestroy() {
    if (this.$shortcut) this.$shortcut.unregisterPage(PROJECT_MANAGE_KBD_SCOPE);
  },
  methods: {
    registerProjectManageShortcuts() {
      if (!this.$shortcut) return
      this.$shortcut.registerPage(PROJECT_MANAGE_KBD_SCOPE, [
        { key: 'create', defaultLetter: 'E', run: () => this.shortcutCreateProject() }
      ])
    },
    getPageActionHintContainer() {
      return this.$refs.projectManageMain || this.$el
    },
    getPageActionHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${PROJECT_MANAGE_KBD_SCOPE}.${key}`, def)
      return [
        {
          key: 'create',
          letter: L('create', 'E'),
          badgeSelector: '.project-manage-hint-create',
          floatOffset: { placement: 'bottom-right-outset', outset: 2, dy: 5 },
          run: () => this.shortcutCreateProject(),
          visible: () => checkPermi(['system:project:add'])
        }
      ]
    },
    getPageDynamicActionHints(ctx) {
      const used = (ctx && ctx.usedLetters) ? new Set(ctx.usedLetters) : new Set()
      collectHintLettersFromToolbar(this.getPageActionHints()).forEach((ch) => used.add(ch))
      return this.buildProjectTableRowActionHints(used)
    },
    getPageActionHintScrollRoots() {
      const table = this.$refs.projectTable
      if (!table || !table.$el) return []
      const bodyWrap = getDefectTableScrollBody(table.$el)
      return bodyWrap ? [bodyWrap] : []
    },
    buildProjectTableRowActionHints(usedLetters) {
      if (this.loading || this.open) return []
      const table = this.$refs.projectTable
      if (!table || !table.$el) return []
      const bodyWrap = getDefectTableScrollBody(table.$el)
      if (!bodyWrap) return []
      const tableRoot = table.$el
      const list = this.projectList || []
      const seen = new Set()
      const anchors = []
      bodyWrap.querySelectorAll('tbody tr.el-table__row').forEach((tr, rowIndex) => {
        if (!isRowIntersectingContainer(tr, bodyWrap)) return
        const row = resolveElTableRowData(tr) || list[rowIndex]
        if (!row || row.projectId == null) return
        if (!this.hasCollectShortcut(row)) return
        const projectId = String(row.projectId)
        if (seen.has(projectId)) return
        seen.add(projectId)
        const anchor = tr.querySelector('.project-row-kbd-hint-anchor') || resolveTableRowFirstColumnHintAnchor(tr)
        if (!anchor) return
        const layout = resolveTableRowFirstColumnKbdBadgeLayout(tr, tableRoot)
        if (!anchor || !layout.rect) return
        anchors.push({
          anchor,
          getAnchorRect: () => layout.rect,
          floatOffset: layout.floatOffset,
          skipViewportCheck: true,
          run: () => this.clickProject(row)
        })
      })
      const letters = assignRowHintLetters(anchors.length, usedLetters)
      return anchors.map((item, i) => ({
        ...item,
        letter: letters[i],
        key: `row-${i}`
      })).filter((item) => item.letter)
    },
    getListQueryNavItems() {
      return [{ key: 'projectName' }]
    },
    getListQueryNavToolbarRef() {
      return 'projectManageToolsRight'
    },
    shortcutFocusQuery() {
      this.enterListQueryKeyboardNav()
    },
    shortcutCreateProject() {
      if (!checkPermi(['system:project:add'])) return
      this.handleAdd()
    },
    switchTabParticipated() {
      this.activeProjectTabName = this.$i18n.t('project.my-participated-in')
      this.selectProjectTabHandle()
    },
    switchTabAll() {
      this.activeProjectTabName = ''
      this.selectProjectTabHandle()
    },
    shortcutChangePage(delta) {
      const root = this.getPageActionHintContainer()
      if (!root || typeof root.querySelector !== 'function') return
      const btn = root.querySelector(
        delta < 0 ? '.project-manage-table-pagination .btn-prev' : '.project-manage-table-pagination .btn-next'
      )
      if (btn && !btn.classList.contains('disabled') && typeof btn.click === 'function') {
        btn.click()
      }
    },
    /** 选择项目分组 */
    selectProjectTabHandle(){
      this.reset();
      switch (this.activeProjectTabName) {
        case this.$i18n.t('project.my-participated-in'):
          this.queryParams.params.userId = getCurrentUserId();
          this.queryParams.memberOnly = true;
          break;
        default:
          this.queryParams.params.userId = null;
          this.queryParams.memberOnly = false;
      }
      this.getList();
    },
    normalizeProjectRows(rows) {
      const list = rows || [];
      list.forEach(p => {
        if (!p.members) {
          p.listShowMembers = [];
        } else {
          p.listShowMembers = p.members;
        }
      });
      return list;
    },
    /** 原子写入列表与分页总数，避免 total/rows 来自不同请求 */
    applyProjectListResult(res) {
      this.projectList = this.normalizeProjectRows(res && res.rows);
      this.total = Number(res && res.total) || 0;
    },
    tryAdminProjectList(reqId, teamId) {
      if (!canUseAdminProjectList()) {
        if (reqId === this._listReqId) {
          this.projectList = [];
          this.total = 0;
        }
        return Promise.resolve();
      }
      const params = {
        pageNum: this.queryParams.pageNum,
        pageSize: this.queryParams.pageSize,
        projectName: this.queryParams.projectName,
        params: this.queryParams.params,
      };
      // 有团队上下文时必须带 teamId，禁止用全站 admin 总数污染当前团队分页
      if (teamId) {
        params.teamId = teamId;
      }
      return listAdminProject(params).then(res => {
        if (reqId !== this._listReqId) return;
        this.applyProjectListResult(res);
      });
    },
    /** 查询项目列表 */
    getList() {
      const reqId = ++this._listReqId;
      this.loading = true;
      resolveEffectiveTeamId().then((teamId) => {
        if (reqId !== this._listReqId) return;
        if (!teamId) {
          // 无团队时仅管理员可看全站；普通用户空列表
          return this.tryAdminProjectList(reqId, null);
        }
        this.queryParams.teamId = teamId;
        return listProject(this.queryParams).then(res => {
          if (reqId !== this._listReqId) return;
          // 团队作用域下以 system list 为准（含空列表），不再静默换成全站 admin list
          this.applyProjectListResult(res);
        }).catch(() => {
          if (reqId !== this._listReqId) return;
          this.projectList = [];
          this.total = 0;
        });
      }).catch(() => {
        if (reqId !== this._listReqId) return;
        return this.tryAdminProjectList(reqId, null);
      }).finally(() => {
        if (reqId === this._listReqId) {
          this.loading = false;
        }
      });
    },
    /** 查询收藏的项目列表 */
    getCollectList() {
      const params = {
        params: {
          userId: getCurrentUserId(),
        },
        collect: true,
        pageNum: 1,
        pageSize: 99
      };
      resolveEffectiveTeamId().then((teamId) => {
        if (!teamId) {
          if (!canUseAdminProjectList()) {
            this.collectList = [];
            return Promise.resolve();
          }
          params.teamId = null;
          return listAdminProject(params).then(res => {
            this.collectList = res.rows || [];
          });
        }
        params.teamId = teamId;
        return listProject(params).then(res => {
          this.collectList = res.rows || [];
        }).catch(() => {
          this.collectList = [];
        });
      }).catch(() => {
        this.collectList = [];
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        projectId: null,
        projectName: null,
        projectIcon: null,
        projectIntroduce: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
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
      this.ids = selection.map(item => item.projectId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.$router.push({name:'ProjectAdd'})
    },
    isProjectMemberOrCreator(project) {
      if (!project) return false
      if (parseInt(project.createBy) === this.currentUserId) return true
      const members = project.members || []
      return members.some(m => m.userId == this.currentUserId)
    },
    /** 修改按钮操作（旧对话框，保留备用） */
    handleUpdate(row) {
      this.reset();
      const projectId = row.projectId || this.ids
      getProject(projectId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改项目";
      });
    },
    /** 编辑：切换当前项目并进入项目基本信息 */
    handleEditBaseInfo(e, project) {
      if (e && e.stopPropagation) e.stopPropagation()
      if (!project || !project.projectId) return
      useUserStore().SwitchCurrentProject(project.projectId).then(() => {
        this.$router.push(projectOptionHref('project-base-info'))
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.projectId != null) {
            updateProject(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addProject(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(e,row) {
      let msg = this.$i18n.t('project.is-delete-project');
      let _this=this;
      this.$modal.prompt(strFormat(msg,'[ '+row.projectName+' ]'),
        i18n.t('prompted').toString(),
        {
          confirmButtonText: i18n.t('delete').toString(),
          cancelButtonText: i18n.t('cancel').toString(),
          inputPlaceholder: i18n.t('please-enter-your-password').toString(),
          confirmButtonClass: 'delete-button',
          inputType: 'password',
          type: "warning",
        }).then(function(res) {
        delProject(row.projectId, res.value).then(()=>{
          _this.$modal.msgSuccess(_this.$i18n.t('delete-success'));
          _this.refreshSidebar();
        });
      }).catch(() => {});
    },
    /** 点击收藏操作 */
    clickCollectHandle(collect, project,isRefreshList) {
      this.$set(project, 'collect', collect);
      // 保存收藏状态
      collectProject(project.projectId, project).then(res=>{
        // 如果collect参数存在，表明是点击头部的收藏按钮触发
        if(isRefreshList) {
          this.getList();
        }
        this.getCollectList();
        if(collect) {
          this.$message.success(this.$i18n.t('collect-success').toString());
        } else {
          this.$message.success(this.$i18n.t('cancel-success').toString());
        }
      });
    },
    /** Prefer namespaced module path when project has path keys. */
    pushProjectModule(project, module, fallbackName) {
      const namespaced = project && project.teamKey && project.projectKey
        ? buildProjectModulePath(project.teamKey, project.projectKey, module)
        : ''
      if (namespaced) {
        this.$router.push(namespaced)
      } else if (fallbackName) {
        this.$router.push({ name: fallbackName })
      }
    },
    /** 点击项目跳转：进入首个有权限的模块 */
    clickProject(project) {
      const shortcuts = this.collectShortcuts(project)
      if (!shortcuts.length) {
        this.$message.warning(this.$i18n.t('project.no-permission-access-project').toString())
        return
      }
      this.goCollectShortcut(null, project, shortcuts[0])
    },
    /** 收藏卡片快捷入口跳转 */
    goCollectShortcut(e, project, item) {
      if (e && e.stopPropagation) e.stopPropagation()
      if (!project || !project.projectId || !item) return
      useUserStore().SwitchCurrentProject(project.projectId).then(() => {
        this.pushProjectModule(project, item.module, item.fallbackName)
      })
    }
  }
}
</script>
<style lang="scss" scoped>
.project-manage-page {
  /* 与缺陷等页签一致：按文字自然宽度，勿强制 min-width / 过大左右 padding */
  :deep(.el-tabs__item) {
    min-width: 0 !important;
    padding: 0 20px !important;
  }

  .project-manage-table {
    width: 100%;
  }

  :deep(.project-manage-operate-col .cell) {
    overflow: visible;
  }
}

h4 {
  margin: 5px 0px 10px 0px;
}
  .cell {
    > i {
      margin-right: 10px;
    }
  }
  .el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell .el-avatar {
    border: #F5F7FA 3px solid;
  }
  .el-avatar--medium {
    line-height: 31px;
  }
  :deep(.project-collects){
    .el-card__body {
      padding: 10px 20px;
    }
    .project-collects-card-header {
      display: flex;
      flex-direction: row;
      justify-content: space-between;
      align-items: center;
      > i {
        margin-left: auto;
        margin-right: 5px;
      }
    }
    .project-collects-card-tools {
      display: inline-flex;
      flex-direction: row;
      flex-wrap: wrap;
      align-items: center;
      gap: var(--cat2bug-operate-tools-gap, 10px);
      > .svg-icon {
        padding: 2px;
        color: #909399;
        font-size: 18px;
        width: 1em;
        height: 1em;
      }
      > .svg-icon:hover {
        cursor: pointer;
        color: #606266;
      }
      .project-collect-no-permission {
        color: #909399;
        font-size: 12px;
        line-height: 1.4;
        cursor: default;
      }
    }
    .el-col {
      margin-bottom: 10px;
    }
  }
  .project-list-title:hover, .project-block:hover {
    cursor: pointer;
  }
  /* 有权限：名称链接色，对齐缺陷列表标题 */
  .project-list-title.is-project-link {
    cursor: pointer;

    :deep(.project-nameplate-content p) {
      color: var(--cat2bug-operate-text-primary, var(--cat2bug-primary, #1890ff));
    }

    &:hover :deep(.project-nameplate-content p) {
      text-decoration: underline;
      text-underline-offset: 3px;
    }
  }
  .project-list-title:not(.is-project-link) {
    cursor: default;
  }
  .project-manage-tools {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-direction: row;
    margin-bottom: 10px;
    :deep(.el-form-item){
      margin-bottom: 0px;
    }
  }
</style>
