<template>
  <div class="app-container">
    <el-tabs v-model="activeProjectTabName" @tab-click="selectProjectTabHandle">
      <el-tab-pane :label="$t('project.my-participated-in')" :name="$t('project.my-participated-in')"></el-tab-pane>
<!--      <el-tab-pane :label="$t('project.my-manage')" :name="$t('project.my-manage')"></el-tab-pane>-->
<!--      <el-tab-pane :label="$t('project.all-project')" :name="$t('project.all-project')"></el-tab-pane>-->
<!--      <el-tab-pane :label="$t('project.archived-project')" :name="$t('project.archived-project')"></el-tab-pane>-->
    </el-tabs>
    <h4 v-show="collectList.length>0">{{$t('project.collect-project')}}</h4>
    <el-row v-show="collectList.length>0" class="project-collects" :gutter="10">
      <el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4" v-for="project in collectList" :key="project.projectId">
        <el-card class="box-card">
          <div slot="header" class="clearfix project-collects-card-header">
            <project-nameplate :project="project"></project-nameplate>
            <star-switch v-model="project.collect" @change="clickCollectHandle(project, true, $event)"></star-switch>
          </div>
          <div class="project-collects-card-tools">
            <i class="el-icon-s-platform"></i>
            <i class="el-icon-s-operation"></i>
            <i class="el-icon-delete"
               @click="handleDelete(project)"
               v-hasPermi="['system:project:remove']"></i>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <h4>{{$t('project.project-list')}}</h4>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:project:add']"
        >{{$t("create")}}</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="projectList" @selection-change="handleSelectionChange">
      <el-table-column :label="$t('project.name')" align="left" prop="projectName">
        <template slot-scope="scope">
          <project-nameplate :project="scope.row"></project-nameplate>
        </template>
      </el-table-column>
      <el-table-column :label="$t('update-time')" align="left" prop="updateTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('member')" align="left" prop="members" width="200">
        <template slot-scope="scope">
          <div class="project-member-icons">
            <el-tooltip class="item" effect="dark" v-for="member in scope.row.listShowMembers" :key="member.userId" :content="member.nickName" placement="top">
              <el-avatar
                :isStatistics="member.isStatistics?'true':'false'"
                :src="member.avatar?member.avatar:''"
                fit="cover" size="medium">
                {{member.avatar?'':member.userName}}
              </el-avatar>
            </el-tooltip>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('operate')" align="left" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <star-switch v-model="scope.row.collect" @change="clickCollectHandle(scope.row, false, $event)"></star-switch>
<!--          <el-button-->
<!--            size="mini"-->
<!--            type="text"-->
<!--            icon="el-icon-edit"-->
<!--            @click="handleUpdate(scope.row)"-->
<!--            v-hasPermi="['system:project:edit']"-->
<!--          >修改</el-button>-->
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:project:remove']"
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
import ProjectNameplate from "@/components/ProjectNameplate";
import StarSwitch from "@/components/StarSwitch";
import { strFormat } from "@/utils/index"

export default {
  name: "Project",
  components: { ProjectNameplate, StarSwitch },
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
    collect: function() {
      return function (project) {
        return project.collect?'true':'false';
      }
    },
  },
  created() {
    this.getList();
    this.getCollectList();
  },
  methods: {
    /** 选择项目分组 */
    selectProjectTabHandle(){

    },
    /** 查询项目列表 */
    getList() {
      this.queryParams.teamId = this.getTeamId();
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
    /** 获取团队id */
    getTeamId() {
      return this.$store.state.user.config.currentTeamId;
    },
    /** 查询收藏的项目列表 */
    getCollectList() {
      let params = {
        teamId: this.getTeamId(),
        collect: true
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
    handleDelete(row) {
      let msg = this.$i18n.t('project.is-delete-project');
      this.$modal.confirm(strFormat(msg,'[ '+row.projectName+' ]')).then(function() {
        return delProject(row.projectId);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 点击收藏操作 */
    clickCollectHandle(project,isRefreshList,collect) {
      this.$set(project,'collect',collect);
      // 保存收藏状态
      collectProject(project.projectId, project).then(res=>{
        // 如果collect参数存在，表明是点击头部的收藏按钮触发
        if(isRefreshList) {
          this.getList();
        }
        this.getCollectList();
        if(collect) {
          this.$message.success(this.$i18n.t('project.collect-success'));
        } else {
          this.$message.success(this.$i18n.t('project.cancel-success'));}
      });
    }
  }
};
</script>
<style lang="scss" scoped>
  .project-member-icons {
    display: flex;
    display: -webkit-flex; /* Safari */
    flex-direction: row-reverse;
    flex-wrap: nowrap;
    justify-content: flex-end;
    align-items: center;
    .el-avatar {
      margin-right:-8px;
      border: #FFF 3px solid;
    }
    .el-avatar[isStatistics='true'] {
      background-color: #E4E7ED;
      color: #909399;
    }
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
      i {
        padding: 5px;
        color: #909399;
      }
      i:hover {
        cursor: pointer;
        color: #606266;
      }
    }
    .el-col {
      margin-bottom: 10px;
    }
  }


</style>
