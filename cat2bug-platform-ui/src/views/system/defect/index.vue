<template>
  <div class="app-container">
    <el-tabs v-model="activeDefectTabName" @tab-click="selectDefectTabHandle">
      <el-tab-pane :label="$t('project.my-participated-in')" :name="$t('project.my-participated-in')"></el-tab-pane>
      <!--      <el-tab-pane :label="$t('project.my-manage')" :name="$t('project.my-manage')"></el-tab-pane>-->
            <el-tab-pane :label="$t('project.all-project')" :name="$t('project.all-project')"></el-tab-pane>
      <!--      <el-tab-pane :label="$t('project.archived-project')" :name="$t('project.archived-project')"></el-tab-pane>-->
    </el-tabs>
    <div class="defect-tools">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="0">
        <el-form-item prop="defectName">
          <el-input
            v-model="queryParams.defectName"
            :placeholder="$t('defect.enter-name')"
            prefix-icon="el-icon-search"
            clearable
            @input="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <select-module v-model="queryParams.moduleId" :project-id="queryParams.projectId" :is-edit="false" size="small" icon="el-icon-files" @input="handleQuery" />
        </el-form-item>
        <el-form-item prop="moduleVersion">
          <el-input
            v-model="queryParams.moduleVersion"
            prefix-icon="el-icon-discount"
            :placeholder="$t('defect.enter-version')"
            clearable
            @input="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="handleBy">
          <el-input
            v-model="queryParams.handleBy"
            placeholder="请输入处理人"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAdd"
            v-hasPermi="['system:defect:add']"
          >{{$i18n.t('defect.create')}}</el-button>
        </el-col>
<!--        <el-col :span="1.5">-->
<!--          <el-button-->
<!--            type="warning"-->
<!--            plain-->
<!--            icon="el-icon-download"-->
<!--            size="mini"-->
<!--            @click="handleExport"-->
<!--            v-hasPermi="['system:defect:export']"-->
<!--          >导出</el-button>-->
<!--        </el-col>-->
      </el-row>
    </div>
    <el-table v-loading="loading" :data="defectList" @selection-change="handleSelectionChange" @sort-change="sortChangeHandle">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column :label="$t('id')" align="left" prop="projectNum" width="80" sortable >
        <template slot-scope="scope">
          <span>{{ '#' + scope.row.projectNum }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('type')" align="left" prop="defectTypeName" width="80" sortable />
      <el-table-column :label="$t('title')" align="left" prop="defectName" sortable />
      <el-table-column :label="$t('level')" align="left" prop="defectLevel" width="100" sortable >
        <template slot-scope="scope">
          <level-tag :options="dict.type.defect_level" :value="scope.row.defectLevel"/>
        </template>
      </el-table-column>
      <el-table-column :label="$t('state')" align="left" prop="defectStateName" width="120" sortable />
      <el-table-column :label="$t('module')" align="left" prop="moduleName" width="150" sortable />
      <el-table-column :label="$t('version')" align="left" prop="moduleVersion" width="100" sortable />
      <el-table-column :label="$t('update-time')" align="left" prop="updateTime" width="180" sortable >
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('handle-by')" align="left" prop="handleBy">
        <template slot-scope="scope">
          <row-list-member :members="scope.row.handleByList"></row-list-member>
        </template>
      </el-table-column>
      <el-table-column :label="$t('image')" align="left" prop="imgUrls">
        <template slot-scope="scope">
          <el-image
            v-for="(img,index) in getImg(scope.row)"
            :key="index"
            style="width: 50px; height: 50px"
            :src="img"
            :preview-src-list="[img]"
            fit="contain"></el-image>
        </template>
      </el-table-column>
      <el-table-column :label="$t('annex')" align="left" prop="annexUrls" />
      <el-table-column :label="$t('operate')" align="left" class-name="small-padding fixed-width" width="120">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:defect:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:defect:remove']"
          >删除</el-button>
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

    <!-- 添加或修改缺陷对话框 -->
    <add-defect ref="addDefectForm" :project-id="22" @added="getList" />
  </div>
</template>

<script>
import { listDefect, getDefect, delDefect, addDefect, updateDefect } from "@/api/system/defect";
import RowListMember from "@/components/RowListMember";
import LevelTag from "@/components/LevelTag";
import AddDefect from "./add.vue"
import SelectModule from "@/components/SelectModule";
export default {
  name: "Defect",
  components: {SelectModule, RowListMember, AddDefect, LevelTag },
  dicts: ['defect_level'],
  data() {
    return {
      // 当前缺陷的tab页名
      activeDefectTabName: this.$i18n.t('project.my-participated-in'),
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
      // 缺陷表格数据
      defectList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 缺陷等级时间范围
      daterangeUpdateTime: [],
      // 缺陷等级时间范围
      daterangeCreateTime: [],
      // 缺陷等级时间范围
      daterangeHandleTime: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        orderByColumn: 'updateTime',
        isAsc: 'desc',
        defectType: null,
        defectName: null,
        projectId: null,
        testPlanId: null,
        caseId: null,
        dataSources: null,
        dataSourcesParams: null,
        moduleId: null,
        moduleVersion: null,
        createBy: null,
        updateTime: null,
        createTime: null,
        updateBy: null,
        defectState: null,
        handleBy: null,
        handleTime: null,
        defectLevel: null
      },

    };
  },
  computed: {
    getImg: function () {
      return function (defect){
        let imgs = defect.imgUrls?defect.imgUrls.split(','):[];
        return imgs.map(i=>{
          return process.env.VUE_APP_BASE_API + i;
        })
      }
    }
  },
  created() {
    this.getList();
  },
  methods: {
    sortChangeHandle(e) {
      console.log(e)
      if(e.order){
        switch (e.prop) {
          case 'defectStateName':
            this.queryParams.orderByColumn='defectState';
            break;
          case 'defectTypeName':
            this.queryParams.orderByColumn='defectType';
            break;
          default:
            this.queryParams.orderByColumn=e.prop;
            break;
        }
        this.queryParams.isAsc=e.order=='ascending'?"asc":'desc';
      } else {
        this.queryParams.orderByColumn=null;
        this.queryParams.isAsc=null;
      }
      this.getList();
    },
    /** 切换页标签 */
    selectDefectTabHandle() {

    },
    getProjectId() {
      return 22;
    },
    /** 获取团队id */
    getTeamId() {
      return this.$store.state.user.config.currentTeamId;
    },
    /** 查询缺陷列表 */
    getList() {
      this.loading = true;
      this.queryParams.params = {};
      if (null != this.daterangeUpdateTime && '' != this.daterangeUpdateTime) {
        this.queryParams.params["beginUpdateTime"] = this.daterangeUpdateTime[0];
        this.queryParams.params["endUpdateTime"] = this.daterangeUpdateTime[1];
      }
      if (null != this.daterangeCreateTime && '' != this.daterangeCreateTime) {
        this.queryParams.params["beginCreateTime"] = this.daterangeCreateTime[0];
        this.queryParams.params["endCreateTime"] = this.daterangeCreateTime[1];
      }
      if (null != this.daterangeHandleTime && '' != this.daterangeHandleTime) {
        this.queryParams.params["beginHandleTime"] = this.daterangeHandleTime[0];
        this.queryParams.params["endHandleTime"] = this.daterangeHandleTime[1];
      }
      this.queryParams.projectId = this.getProjectId();
      listDefect(this.queryParams).then(response => {
        this.defectList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },

    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.daterangeUpdateTime = [];
      this.daterangeCreateTime = [];
      this.daterangeHandleTime = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.defectId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.$refs.addDefectForm.open();
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const defectId = row.defectId || this.ids
      getDefect(defectId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改缺陷";
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const defectIds = row.defectId || this.ids;
      this.$modal.confirm('是否确认删除缺陷编号为"' + defectIds + '"的数据项？').then(function() {
        return delDefect(defectIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/defect/export', {
        ...this.queryParams
      }, `defect_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
<style lang="scss" scoped>
.defect-tools {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  > * {
    display: inline-block;
    margin-bottom: 0px;
    ::v-deep .el-form-item {
      margin-bottom: 0px;
    }
  }
}
</style>
