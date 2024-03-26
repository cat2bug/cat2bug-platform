<template>
  <div class="app-container">
    <project-label />
    <div class="defect-tools-tab">
      <el-tabs v-model="activeDefectTabName" @tab-click="selectDefectTabHandle">
        <el-tab-pane :label="$t('defect.my-participated-in')" :name="$t('defect.my-participated-in')"></el-tab-pane>
        <el-tab-pane :label="$t('defect.my-following')" :name="$t('defect.my-following')"></el-tab-pane>
        <el-tab-pane :label="$t('defect.all-defect')" :name="$t('defect.all-defect')"></el-tab-pane>
        <!--      <el-tab-pane :label="$t('project.archived-project')" :name="$t('project.archived-project')"></el-tab-pane>-->
      </el-tabs>
      <div class="defect-tools-tab-right">
        <svg-icon class="defect-tools-button" icon-class="add-statistic" @click="addStatisticHandle" />
        <svg-icon class="defect-tools-button" v-show="statisticPanelVisible" icon-class="view-statistic" @click="statisticPanelHandle" />
        <svg-icon class="defect-tools-button" v-show="!statisticPanelVisible" icon-class="not-view-statistic" @click="statisticPanelHandle" />
      </div>
    </div>
    <cat2-bug-statistic :params="{}" v-show="statisticPanelVisible" :draggable="true" />
    <div class="defect-tools">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="0">
        <el-form-item prop="defectType">
          <el-dropdown split-button size="small" @command="defectTypeChangeHandle" @click="selectDefectTabHandle">
            {{$i18n.t(activeDefectTypeName)}}
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="">{{$i18n.t('defect.all-type')}}</el-dropdown-item>
              <el-dropdown-item v-for="type in config.types" :command="type.value" :key="'type_'+type.key">{{$i18n.t(type.value)}}</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </el-form-item>
        <el-form-item prop="defectState">
          <el-select v-model="queryParams.params.defectStates" multiple collapse-tags :placeholder="$t('defect.select-state')" @change="defectStateChangeHandle">
            <el-option
              v-for="state in config.states"
              :key="state.key"
              :label="$i18n.t(state.value)"
              :value="state.key">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item prop="defectName">
          <el-input
            v-model="queryParams.defectName"
            :placeholder="$t('defect.enter-name')"
            prefix-icon="el-icon-search"
            clearable
            @input="selectDefectTabHandle()"
          />
        </el-form-item>
        <el-form-item>
          <select-module v-model="queryParams.moduleId" :project-id="queryParams.projectId" :is-edit="false" size="small" icon="el-icon-files" @input="selectDefectTabHandle()" />
        </el-form-item>
        <el-form-item prop="moduleVersion">
          <el-input
            v-model="queryParams.moduleVersion"
            prefix-icon="el-icon-discount"
            :placeholder="$t('defect.enter-version')"
            clearable
            @input="selectDefectTabHandle()"
          />
        </el-form-item>
        <el-form-item prop="handleBy">
          <select-project-member
            v-model="queryParams.handleBy"
            :project-id="queryParams.projectId"
            :placeholder="$t('defect.select-handle-by').toString()"
            :is-head="false"
            size="small"
            icon="el-icon-user"
            @input="selectDefectTabHandle()"
          />
        </el-form-item>
      </el-form>
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-popover
            placement="top"
            trigger="click">
            <div class="row">
              <i class="el-icon-s-fold"></i>
              <h4>显示字段</h4>
            </div>
            <el-divider class="defect-field-divider"></el-divider>
            <el-checkbox-group v-model="checkedFieldList" class="col" @change="checkedFieldListChange">
              <el-checkbox v-for="field in fieldList" :label="field" :key="field">{{$t(field)}}</el-checkbox>
            </el-checkbox-group>
            <el-button
              style="padding: 7px;"
              plain
              slot="reference"
              icon="el-icon-s-fold"
              size="mini"
            ></el-button>
          </el-popover>
        </el-col>
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
      </el-row>
    </div>
    <!-- 缺陷列表-->
    <el-table :key="tableKey" v-loading="loading" style="width:100%;" :data="defectList" @selection-change="handleSelectionChange" @sort-change="sortChangeHandle" @row-click="editDefectHandle">
      <el-table-column v-if="showField('id')" :label="$t('id')" :key="$t('id')" align="left" prop="projectNum" width="80" sortable >
        <template slot-scope="scope">
          <span>{{ '#' + scope.row.projectNum }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('type')" :label="$t('type')" :key="$t('type')" align="left" prop="defectTypeName" width="100" sortable>
        <template slot-scope="scope">
          <defect-type-flag :defect="scope.row" />
        </template>
      </el-table-column>
      <el-table-column v-if="showField('title')" :label="$t('title')" :key="$t('title')" align="left" prop="defectName" min-width="200" sortable >
        <template slot-scope="scope">
          <div class="table-defect-title">
            <focus-member-list
              v-show="scope.row.focusList && scope.row.focusList.length>0"
              v-model="scope.row.focusList"
              module-name="defect"
              :data-id="scope.row.defectId" />
            <el-link type="primary" @click="editDefectHandle(scope.row)">{{ scope.row.defectName }}</el-link>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('level')" :label="$t('level')" :key="$t('level')" align="left" prop="defectLevel" width="100" sortable >
        <template slot-scope="scope">
          <level-tag :options="dict.type.defect_level" :value="scope.row.defectLevel"/>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('state')" :label="$t('state')" :key="$t('state')" align="left" prop="defectStateName" width="120" sortable>
        <template slot-scope="scope">
          <defect-state-flag :defect="scope.row" />
        </template>
      </el-table-column>
      <el-table-column v-if="showField('module')" :label="$t('module')" :key="$t('module')" align="left" prop="moduleName" min-width="100" sortable />
      <el-table-column v-if="showField('version')" :label="$t('version')" :key="$t('version')" align="left" prop="moduleVersion" width="100" sortable />
      <el-table-column v-if="showField('update-time')" :label="$t('update-time')" :key="$t('update-time')" align="left" prop="updateTime" width="160" sortable >
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('handle-by')" :label="$t('handle-by')" :key="$t('handle-by')" align="left" prop="handleBy">
        <template slot-scope="scope">
          <row-list-member :members="scope.row.handleByList"></row-list-member>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('image')" :label="$t('image')" :key="$t('image')" align="left" prop="imgUrls">
        <template slot-scope="scope">
          <el-image
            @click="clickImageHandle"
            v-for="(img,index) in getUrl(scope.row.imgUrls)"
            :key="index"
            style="width: 50px; height: 50px"
            :src="img"
            :preview-src-list="[img]"
            fit="contain"></el-image>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('annex')" :label="$t('annex')" :key="$t('annex')" align="left" prop="annexUrls">
        <template slot-scope="scope">
          <el-link type="primary" v-for="(file,index) in getUrl(scope.row.annexUrls)" :key="index" :href="file">{{getFileName(file)}}</el-link>
        </template>
      </el-table-column>
      <el-table-column :label="$t('operate')" align="left" class-name="small-padding fixed-width" width="200">
        <template slot-scope="scope">
          <defect-tools :is-text="true" :defect="scope.row" size="mini" :is-show-icon="true" @delete="getList(queryParams.params)" @update="getList(queryParams.params)" @log="getList(queryParams.params)"></defect-tools>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList(queryParams.params)"
    />
    <!-- 添加或修改缺陷对话框 -->
    <add-defect ref="addDefectForm" :project-id="getProjectId()" @added="getList(queryParams.params)" />
    <handle-defect ref="editDefectForm" :project-id="getProjectId()" @change="getList(queryParams.params)" @delete="getList(queryParams.params)" />
  </div>
</template>

<script>
import {listDefect, getDefect, delDefect, configDefect} from "@/api/system/defect";
import RowListMember from "@/components/RowListMember";
import LevelTag from "@/components/LevelTag";
import AddDefect from "@/components/Defect/AddDefect"
import HandleDefect from "@/components/Defect/HandleDefect"
import SelectModule from "@/components/Module/SelectModule";
import SelectProjectMember from "@/components/Project/SelectProjectMember";
import ProjectLabel from "@/components/Project/ProjectLabel";
import DefectTypeFlag from "@/components/Defect/DefectTypeFlag";
import DefectStateFlag from "@/components/Defect/DefectStateFlag";
import DefectTools from "@/components/Defect/DefectTools";
import Cat2BugStatistic from "@/components/Cat2BugStatistic"
import FocusMemberList from "@/components/FocusMemberList";
import { checkPermi } from "@/utils/permission";

/** 需要显示的缺陷字段列表在缓存的key值 */
const DEFECT_TABLE_FIELD_LIST_CACHE_KEY='defect-table-field-list';
/** 记录Tab标签选项 */
const DEFECT_TAB_CACHE_KEY='defect-tab';

/** 记录分析模版是否显示的缓存变量名 */
const CACHE_KEY_STATISTIC_PANEL_VISIBLE = 'defect.statisticPanelVisible';
export default {
  name: "Defect",
  components: {SelectModule, RowListMember, AddDefect, HandleDefect, LevelTag, SelectProjectMember,ProjectLabel,DefectTypeFlag, DefectStateFlag, DefectTools, Cat2BugStatistic, FocusMemberList },
  dicts: ['defect_level'],
  data() {
    return {
      tableKey: (new Date()).getMilliseconds(),
      checkedFieldList: [],
      fieldList: [],
      // 分析图表列表
      statisticList:[],
      // 查询中缺陷类型的名称
      activeDefectTypeName: 'defect.all-type',
      // 查询中缺陷状态的名称
      activeDefectStateName: 'defect.all-state',
      // 缺陷配置
      config:{},
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
      // 是否显示统计面板
      statisticPanelVisible: this.$cache.local.get(CACHE_KEY_STATISTIC_PANEL_VISIBLE)=='false'?false:true,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        orderByColumn: 'createTime',
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
        defectLevel: null,
        params:{}
      },
    };
  },
  computed: {
    /** 字段是否显示 */
    showField: function () {
      return function (field) {
        return this.checkedFieldList.filter(f=>f==field).length>0;
      }
    },
    /** 获取当前用户id */
    currentUserId: function() {
      return this.$store.state.user.id;
    },
    getUrl: function () {
      return function (urls){
        let imgs = urls?urls.split(','):[];
        return imgs.map(i=>{
          return process.env.VUE_APP_BASE_API + i;
        })
      }
    },
    getFileName: function () {
      return function (url) {
        if(!url) return null;
        let arr = url.split('\/');
        return arr[arr.length-1];
      }
    }
  },
  watch: {
    "$i18n.locale": function (newVal, oldVal) {
      this.setFieldList();
    },
    "queryParams.defectType": function (newVal, oldVal) {
      if( newVal!=oldVal) {
        this.defectTypeChangeHandle(newVal);
      }
    },
    "queryParams.defectState": function (newVal, oldVal) {
      if( newVal!=oldVal) {
        this.defectStateChangeHandle(newVal);
      }
    },
  },
  created() {
    // 设置缺陷列表显示哪些列属性
    this.setFieldList();
    // 设置tab标签选项
    const tabActive = this.$cache.local.get(DEFECT_TAB_CACHE_KEY);
    this.activeDefectTabName = tabActive?tabActive:this.$i18n.t('defect.my-participated-in');

    this.getDefectConfig();
    this.selectDefectTabHandle();
  },
  mounted() {
  },
  methods: {
    checkPermi,
    /** 设置列表显示的属性字段 */
    setFieldList() {
      this.fieldList = [
        'id','type','title','level','state','module','version','update-time','handle-by','image','annex'
      ];

      const fieldList = this.$cache.local.get(DEFECT_TABLE_FIELD_LIST_CACHE_KEY);
      if(fieldList) {
        this.checkedFieldList = JSON.parse(fieldList);
      } else {
        this.checkedFieldList = [];
        this.fieldList.forEach(f=>{
          this.checkedFieldList.push(f);
        });
      }
    },
    search(params) {
      this._setProperty(this.queryParams, params);
      this.selectDefectTabHandle();
    },
    _setProperty(parent,obj) {
      if(obj && Array.isArray(obj)) {
        parent = obj;
        return parent
      } else if(obj && typeof obj == 'object') {
        for (let key in obj) {
          if (parent[key] && typeof obj[key] == 'object') {
            this.$set(parent,key,this._setProperty(parent[key], obj[key]))
          } else {
            this.$set(parent,key,obj[key])
          }
        }
        return parent
      } else {
        return obj;
      }
    },
    getDefectConfig() {
      configDefect().then(res=>{
        this.config = res.data;
      })
    },
    /** 查找缺陷状态改变的处理 */
    defectStateChangeHandle(defectState) {
      this.selectDefectTabHandle();
    },
    /** 查找缺陷状态改变的处理 */
    defectTypeChangeHandle(defectType) {
      if(defectType) {
        this.activeDefectTypeName = defectType;
      } else {
        this.activeDefectTypeName = 'defect.all-type';
      }
      this.queryParams.defectType= defectType;
      this.selectDefectTabHandle();
    },
    /** 打开编辑界面的处理 */
    editDefectHandle(defect) {
      this.$refs.editDefectForm.open(defect.defectId);
    },
    /** 排序改变的处理 */
    sortChangeHandle(e) {
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
      this.selectDefectTabHandle();
    },
    /** 切换页标签 */
    selectDefectTabHandle() {
      switch (this.activeDefectTabName) {
        case this.$i18n.t('defect.my-following'):
          this.handleQuery({
            collect: 1
          });
          break;
        case this.$i18n.t('defect.my-participated-in'):
          this.queryParams.params.userId = this.getUserId();
          this.queryParams.params.collect = null;
          this.handleQuery();
          break;
        default:
          this.queryParams.params.userId = null;
          this.queryParams.params.collect = null;
          this.handleQuery();
          break;
      }
      this.$cache.local.set(DEFECT_TAB_CACHE_KEY,this.activeDefectTabName);
    },
    /** 获取用户id */
    getUserId() {
      return this.$store.state.user.id;
    },
    /** 获取项目id */
    getProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 获取团队id */
    getTeamId() {
      return this.$store.state.user.config.currentTeamId;
    },
    /** 查询缺陷列表 */
    getList(params) {
      this.loading = true;
      if(params)
        this.queryParams.params = params;

      if(!this.queryParams.params) {
        this.queryParams.params={};
      }
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
        this.loading = false;
        this.defectList = response.rows;
        this.total = response.total;
      });
    },
    /** 搜索按钮操作 */
    handleQuery(params) {
      this.queryParams.pageNum = 1;
      this.getList(params);
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
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/defect/export', {
        ...this.queryParams
      }, `defect_${new Date().getTime()}.xlsx`)
    },
    /** 显示图片操作 */
    clickImageHandle(event){
      event.stopPropagation();
    },
    /** 添加统计操作 */
    addStatisticHandle() {
      this.$router.push({name:'DefectStatisticTemplate'})
    },
    /** 统计显示切换操作 */
    statisticPanelHandle() {
      this.statisticPanelVisible = !this.statisticPanelVisible;
      this.$cache.local.set(CACHE_KEY_STATISTIC_PANEL_VISIBLE, this.statisticPanelVisible+'');
    },
    /** 缺陷列表属性字段改变操作 */
    checkedFieldListChange(field) {
      this.$cache.local.set(DEFECT_TABLE_FIELD_LIST_CACHE_KEY,JSON.stringify(field));
    },
  }
};
</script>
<style lang="scss" scoped>
.col {
  display: flex;
  flex-direction: column;
}
.row {
  display: flex;
  flex-direction: row;
  align-items: center;
  > * {
    margin: 0px 5px 0px 0px;
  }
}
.defect-field-divider {
  margin: 8px 0px;
}
.defect-tools-tab {
  margin-top: -10px;
  position: relative;
  height: 50px;
  .el-tabs {
    position: absolute;
    width: 100%;
  }
  .defect-tools-tab-right {
    position: absolute;
    right: 0px;
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    padding-bottom: 5px;
  }
}
.defect-tools {
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
.defect-tools-button {
  cursor: pointer;
  color: #606266;
  margin: 0px 5px;
}
.defect-tools-button:hover {
  color: #409EFF;
}
.table-defect-title {
  display: inline-flex;
  flex-direction: column;
  justify-content: flex-start;
  .el-link {
    flex: 1;
  }
}
.el-table {
  ::v-deep table {
    width: 100% !important;
  }
}
</style>
