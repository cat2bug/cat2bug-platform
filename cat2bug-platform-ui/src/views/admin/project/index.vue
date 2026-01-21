<template>
  <div class="app-container">
    <el-row>
      <el-page-header @back="goBack" :content="$t('project.manage')">
      </el-page-header>
    </el-row>
    <!--项目列表-->
    <div class="project-tools">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="0">
        <el-form-item prop="teamName">
          <el-input
            v-model="queryParams.teamName"
            :placeholder="$t('team.please-enter-name')"
            clearable
            @input="handleQuery"
          />
        </el-form-item>
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
    </div>
    <el-table v-loading="loading" :data="projectList">
      <el-table-column :label="$t('team.name')" align="left" prop="teamName">
        <template slot-scope="scope">
          <team-nameplate class="project-list-title" :team="scope.row" @click.native="clickProject(scope.row)"></team-nameplate>
        </template>
      </el-table-column>
      <el-table-column :label="$t('project.name')" align="left" prop="projectName">
        <template slot-scope="scope">
          <project-nameplate class="project-list-title" :project="scope.row" @click.native="clickProject(scope.row)"></project-nameplate>
        </template>
      </el-table-column>
      <el-table-column :label="$t('create-time')" align="left" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('member')" align="center" prop="members" width="200">
        <template slot-scope="scope">
          <span>{{ scope.row.listShowMembers.length }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('project.introduction')" align="center" prop="projectIntroduce"></el-table-column>
      <el-table-column :label="$t('operate')" align="left" class-name="small-padding" width="130">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            :icon="scope.row.lock?'el-icon-unlock':'el-icon-lock'"
            @click="handleLock(scope.row)"
            v-hasPermi="['admin:project:lock']"
          >{{scope.row.lock?$t('unlock'):$t('lock')}}</el-button>
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
    <project-lock-dialog ref="projectLockDialog" @change="handleQuery"></project-lock-dialog>
  </div>
</template>

<script>
import {listProject} from "@/api/admin/project";
import ProjectNameplate from "@/components/Project/ProjectNameplate";
import TeamNameplate from "@/components/team/TeamNameplate";
import ProjectLockDialog from './lock'
import {mapGetters, mapState} from "vuex";

export default {
  name: "Project",
  components: { TeamNameplate, ProjectNameplate, ProjectLockDialog },
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
  },
  created() {
    this.queryParams.teamName = this.$route.query.teamName;
    this.getList();
  },
  mounted() {
  },
  methods: {
    /** 返回 */
    goBack() {
      this.$router.back();
    },
    /** 查询项目列表 */
    getList() {
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
    /** 点击项目跳转 */
    clickProject(project) {
    },
    handleLock(project) {
      this.$refs.projectLockDialog.open(project);
    },
  }
}
</script>
<style lang="scss" scoped>
  .cell {
    > i {
      margin-right: 10px;
    }
  }
  .el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell .el-avatar {
    border: #F5F7FA 3px solid;
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
