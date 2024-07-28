<template>
  <div class="app-container">
    <project-label />
    <!-- 缺陷页标签-->
    <div class="defect-tools-tab">
      <el-tabs v-model="activeDefectTabName" @tab-click="selectDefectTabHandle">
        <el-tab-pane v-for="tab in config.tabs" :name="tab.tabId+''" :key="tab.tabId+''">
          <span slot="label">{{ tab.tabName }}
            <i style="width: 14px;" class="el-icon-close" @click.stop="removeDefectTabHandle(tab.tabId)"></i>
          </span>
        </el-tab-pane>
        <el-tab-pane key="all-tab" :name="allTab">
          <span slot="label">{{ $t('defect.all-defect') }}</span>
        </el-tab-pane>
      </el-tabs>
      <div class="defect-tools-tab-right">
        <svg-icon class="defect-tools-button" icon-class="add-tab" @click.native="addDefectTabHandle" />
        <svg-icon v-show="statisticPanelVisible" icon-class="view-statistic" @click="addStatisticHandle" />
      </div>
    </div>
    <!-- 统计板块-->
    <cat2-bug-statistic class="defect-tools-statistic" :params="{}" v-show="statisticPanelVisible" :draggable="true" />
    <!-- 动态缺陷显示组件-->
<!--    <keep-alive>-->
    <component
      ref="defectContentComponent"
      :is="defectContentComponent"
      @defect-click="handleDefectClick"
      @refresh="handleRefreshQuery"
      >
      <template slot="left-tools">
        <!-- 搜索-->
        <div class="defect-tools-search">
          <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="0">
            <el-form-item>
              <el-radio-group v-model="defectContentComponent" @input="handleDefectContentChange">
                <el-radio-button label="DefectTable">{{ $t('table') }}</el-radio-button>
                <el-radio-button label="DefectCalendar">{{ $t('calendar') }}</el-radio-button>
              </el-radio-group>
            </el-form-item>
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
              <el-select v-model="queryParams.params.defectStates" multiple collapse-tags clearable :placeholder="$t('defect.select-state')" @change="handleQuery()">
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
                @input="handleQuery()"
              />
            </el-form-item>
            <el-form-item>
              <select-module v-model="queryParams.moduleId" :project-id="queryParams.projectId" :is-edit="false" size="small" icon="el-icon-files" @input="handleQuery()" />
            </el-form-item>
            <el-form-item prop="moduleVersion">
              <el-input
                v-model="queryParams.moduleVersion"
                prefix-icon="el-icon-discount"
                :placeholder="$t('defect.enter-version')"
                clearable
                @input="handleQuery()"
              />
            </el-form-item>
            <el-form-item prop="handleBy">
              <select-project-member
                v-model="queryParams.handleBy"
                :project-id="queryParams.projectId"
                placeholder="defect.select-handle-by"
                :is-head="false"
                size="small"
                icon="el-icon-user"
                @input="handleQuery()"
              />
            </el-form-item>
          </el-form>
        </div>
      </template>
      <template slot="right-tools">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="small"
          @click="handleAdd"
          v-hasPermi="['system:defect:add']"
        >{{$i18n.t('defect.create')}}</el-button>
      </template>
    </component>
<!--    </keep-alive>-->
    <!-- 缺陷日历-->
<!--    <defect-calendar v-else-if="defectContentId=='calendar'" ref="defectCalendar" />-->
    <!-- 添加或修改缺陷对话框 -->
    <add-defect ref="addDefectForm" :project-id="getProjectId()" @added="search(queryParams)" />
    <!-- 浏览缺陷对话框 -->
    <handle-defect ref="editDefectForm" :project-id="getProjectId()" @change="handleRefreshQuery" @delete="handleRefreshQuery" />
    <!-- 添加页签对话框 -->
    <defect-tab-dialog ref="defectTabDialog" :project-id="getProjectId()" :member-id="userId" @add="tabAddHandle" />
  </div>
</template>

<script>
import { checkPermi } from "@/utils/permission";
import { delTabs } from "@/api/system/DefectTabs";
import { configDefect } from "@/api/system/defect";
import AddDefect from "@/components/Defect/AddDefect"
import HandleDefect from "@/components/Defect/HandleDefect"
import SelectModule from "@/components/Module/SelectModule";
import SelectProjectMember from "@/components/Project/SelectProjectMember";
import ProjectLabel from "@/components/Project/ProjectLabel";
import Cat2BugStatistic from "@/components/Cat2BugStatistic"
import DefectTabDialog from "@/views/system/defect/DefectTabDialog";
import i18n from "@/utils/i18n/i18n";
import {lifeTime} from "@/utils/defect";
import store from "@/store";
import DefectTable from "./list/table"
import DefectCalendar from "./list/calendar"

/** 记录Tab标签选项 */
const DEFECT_TAB_CACHE_KEY='defect-tab';
/** 名称等于所有选项的name */
const ALL_TAB_NAME = 'all-tab';

/** 记录分析模版是否显示的缓存变量名 */
const CACHE_KEY_STATISTIC_PANEL_VISIBLE = 'defect.statisticPanelVisible';
export default {
  name: "Defect",
  components: {SelectModule, AddDefect, HandleDefect, SelectProjectMember,ProjectLabel, Cat2BugStatistic, DefectTabDialog, DefectTable, DefectCalendar },
  data() {
    return {
      // 当前缺陷面板的类型
      defectContentId: 'list',
      defectContentComponent: 'DefectTable',
      // tab相关配置
      // 所有tab的名称
      allTab: ALL_TAB_NAME,
      // 当前缺陷的tab页名
      activeDefectTabName: ALL_TAB_NAME, // this.$i18n.t('project.my-participated-in'),

      // 显示搜索条件
      showSearch: true,
      // 分析图表列表
      statisticList:[],
      // 查询中缺陷类型的名称
      activeDefectTypeName: 'defect.all-type',
      // 查询中缺陷状态的名称
      activeDefectStateName: 'defect.all-state',
      // 缺陷配置
      config:{},

      // 是否显示统计面板
      statisticPanelVisible: this.$cache.local.get(CACHE_KEY_STATISTIC_PANEL_VISIBLE)=='false'?false:true,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        orderByColumn: 'updateTime',
        isAsc: 'desc',
        defectType: null,
        defectName: null,
        projectId: 0,
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
        params:{
          defectStates: []
        }
      },
    };
  },
  computed: {
    /** 获取当前用户id */
    currentUserId: function() {
      return this.$store.state.user.id;
    },
    /** 获取项目id */
    projectId: function() {
      return this.$route.query.projectId?parseInt(this.$route.query.projectId):parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 获取用户id */
    userId: function() {
      return Number(this.$store.state.user.id);
    },
  },
  watch: {
    "queryParams.defectType": function (newVal, oldVal) {
      if( newVal!=oldVal) {
        // this.defectTypeChangeHandle(newVal);
      }
    },
    "queryParams.defectState": function (newVal, oldVal) {
      if( newVal!=oldVal) {
        // this.handleQuery();
      }
    },
  },
  mounted() {
    /** 根据项目ID跳转 */
    if(this.$route.query.projectId) {
      let _this = this;
      store.dispatch('UpdateCurrentProjectId', this.$route.query.projectId).then(() => {
        store.dispatch('GetInfo').then(() => {
          _this.init();
        });
      });
    } else {
      this.init();
    }
  },
  methods: {
    checkPermi,
    init() {
      // 初始化对象
      this.$refs.defectContentComponent.init();
      // 获取缺陷配置
      this.getDefectConfig();
      // 显示指定缺陷信息
      if(this.$route.query.defectId) {
        this.$refs.editDefectForm.open(this.$route.query.defectId);
      }
    },
    /** 查询缺陷 */
    search(params) {
      this._setProperty(this.queryParams, params);
      this.handleQuery();
    },
    /** 设置查询属性 */
    _setProperty(parent,obj) {
      if(obj && Array.isArray(obj)) {
        parent = obj;
        return parent
      } else if(obj && typeof obj == 'object') {
        for (let key in obj) {
          if (parent[key] && Array.isArray(obj[key])) {
            this.$set(parent,key,obj[key])
          } else if (parent[key] && typeof obj[key] == 'object') {
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
    /** 获取当前用户缺陷配置 */
    getDefectConfig() {
      configDefect().then(res=>{
        this.config = res.data;
        // 获取选中的缺陷页签名称
        if(this.config.tabs) {
          // 从本地缓存取激活的页标签名称
          this.activeDefectTabName = this.$cache.local.get(DEFECT_TAB_CACHE_KEY); // || ALL_TAB_NAME;
          // 查看所有页标签里是否保护激活页标签，如果没有，设置页标签为"全部"
          if(!this.activeDefectTabName || this.config.tabs.filter(t=>t.tabId+''==this.activeDefectTabName).length==0) {
            this.activeDefectTabName = this.allTab;
          }
        }
        // 执行激活页标签方法
        this.selectDefectTabHandle();
      })
    },
    /** 查找缺陷状态改变的处理 */
    defectTypeChangeHandle(defectType) {
      if(defectType) {
        this.activeDefectTypeName = defectType;
      } else {
        this.activeDefectTypeName = 'defect.all-type';
      }
      this.queryParams.defectType= defectType;
      this.handleQuery();
    },
    /** 打开缺陷浏览的处理 */
    handleDefectClick(defect) {
      this.$refs.editDefectForm.open(defect.defectId);
    },
    /** 获取项目id */
    getProjectId() {
      return this.$route.query.projectId?parseInt(this.$route.query.projectId):parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.handleRefreshQuery();
    },
    /** 搜索操作 */
    handleRefreshQuery() {
      this.queryParams.projectId = this.getProjectId();
      this.$refs.defectContentComponent.search(this.queryParams);
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.$refs.addDefectForm.open();
    },
    ///** 导出按钮操作 */
    // handleExport() {
    //   this.download('system/defect/export', {
    //     ...this.queryParams
    //   }, `defect_${new Date().getTime()}.xlsx`)
    // },
    /** 添加统计操作 */
    addStatisticHandle() {
      this.$router.push({name:'DefectStatisticTemplate'})
    },
    /** 统计显示切换操作 */
    statisticPanelHandle() {
      this.statisticPanelVisible = !this.statisticPanelVisible;
      this.$cache.local.set(CACHE_KEY_STATISTIC_PANEL_VISIBLE, this.statisticPanelVisible+'');
    },
    /** 重制查询条件 */
    reset() {
      this.queryParams = {
        pageNum: 1,
        pageSize: 10,
        orderByColumn: 'updateTime',
        isAsc: 'desc',
        defectType: null,
        defectName: null,
        projectId: 0,
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
        params:{
          defectStates: []
        }
      }
    },
    /** 切换缺陷内容组件改变的处理 */
    handleDefectContentChange() {
      this.$nextTick(()=>{
        this.$refs.defectContentComponent.init();
        this.$refs.defectContentComponent.search(this.queryParams);
      });
    },
    /** ============================================
     * 页签处理方法
     * ============================================ /

    /** 切换页标签 */
    selectDefectTabHandle() {
      if(this.config && this.config.tabs) {
        let tab = this.config.tabs.find(t=>t.tabId==this.activeDefectTabName);
        if(tab && tab.config) {
          if(!tab.config.params) {
            tab.config.params = {
              defectStates: []
            };
          }
          this.queryParams = tab.config;
        } else {
          this.reset();
        }
        this.handleQuery();
      }
      this.$cache.local.set(DEFECT_TAB_CACHE_KEY,this.activeDefectTabName);
    },
    /** 打开添加缺陷页签对话框 */
    addDefectTabHandle() {
      this.$refs.defectTabDialog.open();
    },
    /** 添加页签处理 */
    tabAddHandle(tab) {
      this.config.tabs.push(tab);
      this.activeDefectTabName = tab.tabId+'';
      this.selectDefectTabHandle();
    },
    /** 移除页签处理 */
    removeDefectTabHandle(tabId) {
      if(!tabId) return;
      this.$modal.confirm(
        this.$i18n.t('defect.delete-defect-tab'),
        this.$i18n.t('prompted').toString(),
        {
          confirmButtonText: i18n.t('delete').toString(),
          cancelButtonText: i18n.t('cancel').toString(),
          confirmButtonClass: 'delete-button',
          type: "warning"
        }).then(function() {
        return delTabs(tabId);
      }).then(() => {
        let activeName = this.activeDefectTabName;
        if (this.activeDefectTabName == tabId) {
          this.config.tabs.forEach((tab, index) => {
            if (tab.tabId+'' == tabId) {
              let nextTab = this.config.tabs[index + 1] || this.config.tabs[index - 1];
              if (nextTab) {
                activeName = nextTab.tabId + '';
              } else {
                activeName = ALL_TAB_NAME;
              }
            }
          });
        }
        this.activeDefectTabName = activeName;
        this.config.tabs=this.config.tabs.filter(t=>(t.tabId+'')!=tabId);
        this.selectDefectTabHandle();
        this.$modal.msgSuccess(this.$i18n.t('delete.success'));
      }).catch(() => {});
    },
  }
};
</script>
<style lang="scss" scoped>
.row {
  display: flex;
  flex-direction: row;
  align-items: center;
  > * {
    margin: 0px 5px 0px 0px;
  }
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
.defect-tools-search {
  display: inline-flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  > * {
    display: inline-block;
    justify-content: flex-start;
    margin-bottom: 0px;
    ::v-deep .el-form-item {
      margin-bottom: 10px;
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
</style>
