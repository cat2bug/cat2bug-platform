<template>
  <div class="app-container">
    <el-tabs v-model="activeProjectTabName" @tab-click="selectProjectTabHandle">
      <el-tab-pane :label="$t('project.my-participated-in')" :name="$t('project.my-participated-in')"></el-tab-pane>
<!--      <el-tab-pane :label="$t('project.my-manage')" :name="$t('project.my-manage')"></el-tab-pane>-->
      <el-tab-pane :label="$t('project.all-project')" name=""></el-tab-pane>
<!--      <el-tab-pane :label="$t('project.archived-project')" :name="$t('project.archived-project')"></el-tab-pane>-->
    </el-tabs>
<!--    收藏列表-->
    <h4 v-show="collectList.length>0">{{$t('project.collect-project')}}</h4>
    <el-row v-show="collectList.length>0" class="project-collects" :gutter="10">
      <el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4" v-for="project in collectList" :key="project.projectId">
        <el-card class="box-card project-block" @click.native="clickProject(project)">
          <div slot="header" class="clearfix project-collects-card-header">
            <project-nameplate :project="project"></project-nameplate>
            <star-switch v-model="project.collect" @change="clickCollectHandle($event, project, true)"></star-switch>
          </div>
          <div class="project-collects-card-tools">
<!--            <svg-icon icon-class="nested"-->
<!--                      @click="goCaseHandle($event, project)"-->
<!--                      v-hasPermi="['system:case:list']"></svg-icon>-->
            <svg-icon icon-class="bug"
               @click="goDefectHandle($event, project)"
               v-hasPermi="['system:defect:list']"></svg-icon>
            <svg-icon icon-class="system"
                @click="goProjectOptionHandle($event, project)"
                v-hasPermi="['system:project:option']"></svg-icon>
<!--            <i class="el-icon-s-platform"></i>-->
<!--            <i class="el-icon-s-operation"></i>-->
            <i class="el-icon-delete"
               @click="handleDelete($event, project)"
               v-hasPermi="['system:project:remove']"></i>
          </div>
        </el-card>
      </el-col>
    </el-row>
<!--    项目列表-->
    <h4>{{$t('project.project-list')}}</h4>
    <div class="project-tools">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="0">
        <el-form-item prop="defectName">
          <el-input
            v-model="queryParams.projectName"
            :placeholder="$t('project.enter-project-name')"
            prefix-icon="el-icon-search"
            clearable
            size="small"
            @input="handleQuery"
          />
        </el-form-item>
      </el-form>
      <div>
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="small"
          @click="handleAdd"
          v-hasPermi="['system:project:add']"
        >{{$t("project.create-project")}}</el-button>
      </div>
    </div>
    <el-table v-loading="loading" :data="projectList">
      <el-table-column :label="$t('project.name')" align="left" prop="projectName">
        <template slot-scope="scope">
          <project-nameplate class="project-list-title" :project="scope.row" @click.native="clickProject(scope.row)"></project-nameplate>
        </template>
      </el-table-column>
      <el-table-column :label="$t('update-time')" align="left" prop="updateTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('member')" align="center" prop="members" width="200">
        <template slot-scope="scope">
          <row-list-member :members="scope.row.listShowMembers" />
        </template>
      </el-table-column>
      <el-table-column :label="$t('operate')" align="left" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <star-switch v-if="visibleDelete(scope.row)" v-model="scope.row.collect" @change="clickCollectHandle($event, scope.row, false)"></star-switch>
<!--          <el-button-->
<!--            size="small"-->
<!--            type="text"-->
<!--            icon="el-icon-edit"-->
<!--            @click="handleUpdate(scope.row)"-->
<!--            v-hasPermi="['system:project:edit']"-->
<!--          >修改</el-button>-->
          <el-button
            size="small"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete($event, scope.row)"
            v-if="visibleDelete(scope.row)"
          >{{$t('delete')}}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改项目对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
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
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {listProject, getProject, delProject, addProject, updateProject, collectProject} from "@/api/system/project";
import ProjectNameplate from "@/components/Project/ProjectNameplate";
import StarSwitch from "@/components/StarSwitch";
import RowListMember from "@/components/RowListMember";
import { strFormat } from "@/utils/index"
import store from "@/store";
import i18n from "@/utils/i18n/i18n";
import {mapGetters, mapState} from "vuex";
import {checkPermi} from "@/utils/permission";
import router from "@/router";

export default {
  name: "Project",
  components: { ProjectNameplate, RowListMember,StarSwitch },
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
      }
    };
  },
  computed: {
    ...mapGetters(["sidebarRouters"]),
    collect: function() {
      return function (project) {
        return project.collect?'true':'false';
      }
    },
    /** 获取当前用户id */
    currentUserId: function() {
      return this.$store.state.user.id;
    },
    /** 获取团队id */
    currentTeamId() {
      return this.$store.state.user.config.currentTeamId;
    },
    /** 是否显示添加按钮 */
    visibleAdd() {
      return function (project) {
        return checkPermi(['system:project:add']) && project.members.filter(m=>m.userId == this.currentUserId).length>0
      }
    },
    /** 是否显示删除按钮 */
    visibleDelete() {
      return function (project) {
        return checkPermi(['system:project:remove']) && project.members.filter(m=>m.userId == this.currentUserId).length>0
      }
    },
    /** 是否可以访问缺陷页面 */
    isViewDefect() {
      return function (project) {
        return checkPermi(['system:defect:list']) && project.members.filter(m=>m.userId == this.currentUserId).length>0
      }
    }
  },
  created() {
    this.selectProjectTabHandle();
    this.getCollectList();
  },
  mounted() {
    this.initFloatMenu();
  },
  methods: {
    /** 初始化浮动菜单 */
    initFloatMenu() {
      this.$floatMenu.windowsInit(document.querySelector('.main-container'));
      this.$floatMenu.resetMenus([{
        id: 'addProject',
        name: 'project.create-project',
        visible: true,
        plain: true,
        type: 'primary',
        icon: 'add-tab',
        prompt: 'project.create-project',
        permissions: ['system:project:add'],
        click : this.handleAdd
      }]);
    },
    /** 选择项目分组 */
    selectProjectTabHandle(){
      this.reset();
      switch (this.activeProjectTabName) {
        case this.$i18n.t('project.my-participated-in'):
          this.queryParams.params.userId = this.$store.state.user.id;
          break;
        default:
          this.queryParams.params.userId = null;
      }
      this.getList();
    },
    /** 查询项目列表 */
    getList() {
      this.queryParams.teamId = this.currentTeamId;
      this.loading = true;
      listProject(this.queryParams).then(res => {
        res.rows.forEach(p=>{
          if(!p.members){
            p.listShowMembers = [];
          } else if(p.members.length<=3) {
            p.listShowMembers = p.members;
          } else {
            let ms = p.members;
            p.listShowMembers = [{
              userName: p.members.length,
              nickName: '共'+p.members.length+"人",
              isStatistics: true
            },ms[1],ms[0]]
          }
        });
        this.projectList = res.rows;
        this.total = res.total;
        this.loading = false;
      });
    },
    /** 查询收藏的项目列表 */
    getCollectList() {
      let params = {
        teamId: this.currentTeamId,
        params: {
          userId: this.currentUserId,
        },
        collect: true,
        pageNum: 1,
        pageSize: 99
      }
      listProject(params).then(res => {
        this.collectList = res.rows;
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
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const projectId = row.projectId || this.ids
      getProject(projectId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改项目";
      });
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
    /** 点击项目跳转 */
    clickProject(project) {
      let _this = this;
      if(this.isViewDefect(project)) {
        store.dispatch('UpdateCurrentProjectId', project.projectId).then(() => {
          store.dispatch('GetInfo').then(() => {
            store.dispatch('GenerateRoutes').then(accessRoutes => {
              router.addRoutes(accessRoutes) // 动态添加可访问路由表
              _this.$router.push({name:'Defect', params: { projectId: project.projectId }})
            });
          });
        });
      } else {
        this.$message.warning(this.$i18n.t('project.no-permission-access-project').toString());
      }
    },
    /** 跳转到缺陷管理 */
    goDefectHandle(e, project) {
      this.clickProject(project);
      e.stopPropagation();
    },
    /** 跳转到项目设置 */
    goProjectOptionHandle(e,project) {
      let _this = this;
      store.dispatch('UpdateCurrentProjectId', project.projectId).then(() => {
        store.dispatch('GetInfo').then(() => {
          _this.$router.push({name:'Option', params: { projectId: project.projectId }})
        });
      });
      e.stopPropagation();
    },
    /** 跳转到测试用例 */
    goCaseHandle(e,project) {
      let _this = this;
      store.dispatch('UpdateCurrentProjectId', project.projectId).then(() => {
        store.dispatch('GetInfo').then(() => {
          _this.$router.push({name: 'Case', params: {projectId: project.projectId}})
        });
      });
    }
  }
}
</script>
<style lang="scss" scoped>
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
  ::v-deep .project-collects {
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
      gap: 5px;
      >* {
        padding: 1px;
        color: #909399;
      }
      >*:hover {
        cursor: pointer;
        color: #606266;
      }
    }
    .el-col {
      margin-bottom: 10px;
    }
  }
  .project-list-title:hover, .project-block:hover {
    cursor: pointer;
  }
  .project-tools {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-direction: row;
    margin-bottom: 10px;
    ::v-deep .el-form-item {
      margin-bottom: 0px;
    }
  }
</style>
