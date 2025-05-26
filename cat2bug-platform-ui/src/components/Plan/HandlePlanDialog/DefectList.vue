<template>
  <div class="plan-item-content">
    <div class="plan-item-query">
      <div class="row">
        <slot name="query"></slot>
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="0">
          <el-form-item prop="defectName">
            <el-input
              size="small"
              v-model="queryParams.defectName"
              :placeholder="$t('defect.enter-name')"
              prefix-icon="el-icon-search"
              clearable
              @input="search()"
            />
          </el-form-item>
          <el-form-item prop="defectState">
            <el-select size="small" v-model="queryParams.params.defectStates" multiple collapse-tags clearable :placeholder="$t('defect.select-state')" @change="search()">
              <el-option
                v-for="state in defectStates"
                :key="state.key"
                :label="$i18n.t(state.value)"
                :value="state.key">
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item prop="handleBy" class="padding-top-3px">
            <select-project-member
              v-model="queryParams.handleBy"
              :project-id="projectId"
              placeholder="defect.select-handle-by"
              :is-head="false"
              size="small"
              icon="el-icon-user"
              @input="search()"
            />
          </el-form-item>
        </el-form>
      </div>
      <div class="handle-plan-tools-right">
        <el-popover
          placement="top"
          trigger="click">
          <div class="row">
            <i class="el-icon-s-fold"></i>
            <h4>{{$t('defect.display-field')}}</h4>
          </div>
          <el-divider class="defect-field-divider"></el-divider>
          <el-checkbox-group v-model="tableShowFieldList" class="col" @change="checkedFieldListChange">
            <el-checkbox v-for="field in tableAllFieldList" :label="field" :key="field">{{$t(field)}}</el-checkbox>
          </el-checkbox-group>
          <el-button
            style="padding: 9px;"
            plain
            slot="reference"
            icon="el-icon-s-fold"
            size="mini"
          ></el-button>
        </el-popover>
      </div>
    </div>
    <el-table ref="defectTable" :key="tableKey" v-loading="loading" style="width:100%;" :data="defectList"
              @selection-change="handleSelectionChange"
              @sort-change="sortChangeHandle"
              @row-click="handleClickTableRow"
              @mousedown.native="handleTableMouseDown"
              @mouseup.native="handleTableMouseUp"
              @mousemove.native="handleTableMouseMove">
      <el-table-column v-if="showField('id')" :label="$t('id')" :key="$t('id')" align="left" prop="projectNum" width="80" sortable="custom" fixed >
        <template slot-scope="scope">
          <span>{{ '#' + scope.row.projectNum }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('type')" :label="$t('type')" :key="$t('type')" align="left" prop="defectTypeName" width="100" sortable="custom" fixed>
        <template slot-scope="scope">
          <defect-type-flag :defect="scope.row" />
        </template>
      </el-table-column>
      <el-table-column v-if="showField('defect.name')" :label="$t('defect.name')" :key="$t('defect.name')" align="left" prop="defectName" width="300" sortable="custom" fixed>
        <template slot-scope="scope">
          <div class="table-defect-title">
            <focus-member-list
              v-show="scope.row.focusList && scope.row.focusList.length>0"
              v-model="scope.row.focusList"
              module-name="defect"
              :data-id="scope.row.defectId" />
            <el-link type="primary" @click="handleClickTableRow(scope.row)">{{ scope.row.defectName }}</el-link>
            <div class="defect-statistics">
              <div>
                <i class="el-icon-time"></i>
                <span>{{ $t('defect.life-time') }}:</span>
                <span class="defect-statistics-value">{{defectLife(scope.row)}}</span>
              </div>
              <div>
                <i class="el-icon-document-delete"></i>
                <span>{{$i18n.t('reject')}}:</span>
                <span class="defect-statistics-value">{{scope.row.rejectCount}}</span>
              </div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('priority')" :label="$t('priority')" :key="$t('priority')" align="center" prop="defectLevel" width="100" sortable="custom" >
        <template slot-scope="scope">
          <level-tag :options="dict.type.defect_level" :value="scope.row.defectLevel"/>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('state')" :label="$t('state')" :key="$t('state')" align="center" prop="defectState" width="120" sortable="custom">
        <template slot-scope="scope">
          <defect-state-flag :defect="scope.row" />
        </template>
      </el-table-column>
      <el-table-column v-if="showField('module')" :label="$t('module')" :key="$t('module')" align="left" prop="moduleName" min-width="200" sortable="custom" />
      <el-table-column v-if="showField('version')" :label="$t('version')" :key="$t('version')" align="left" prop="moduleVersion" width="100" sortable="custom" />
      <el-table-column v-if="showField('image')" :label="$t('image')" :key="$t('image')" align="center" prop="imgUrls" width="80">
        <template slot-scope="scope">
          <cat2-bug-preview-image :images="getUrl(scope.row.imgUrls)" />
        </template>
      </el-table-column>
      <el-table-column v-if="showField('annex')" :label="$t('annex')" :key="$t('annex')" align="left" prop="annexUrls" min-width="300">
        <template slot-scope="scope">
          <div class="annex-list">
            <cat2-bug-text :content="file" type="down" :tooltip="file" v-for="(file,index) in getUrl(scope.row.annexUrls)" :key="index" />
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('update-time')" :label="$t('update-time')" :key="$t('update-time')" align="left" prop="updateTime" width="160" sortable="custom" >
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('plan-start-time')" :label="$t('plan-start-time')" :key="$t('plan-start-time')" align="left" prop="planStartTime" width="160" sortable="custom" >
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.planStartTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('plan-end-time')" :label="$t('plan-end-time')" :key="$t('plan-end-time')" align="left" prop="planEndTime" width="160" sortable="custom" >
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.planEndTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('createBy')" :label="$t('createBy')" :key="$t('createBy')" align="center" min-width="160" prop="createMember">
        <template slot-scope="scope">
          <row-list-member :members="[scope.row.createMember]"></row-list-member>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('handle-by')" :label="$t('handle-by')" :key="$t('handle-by')" align="center" min-width="160" prop="handleBy">
        <template slot-scope="scope">
          <row-list-member :members="scope.row.handleByList"></row-list-member>
        </template>
      </el-table-column>
      <el-table-column :label="$t('operate')" align="left" class-name="small-padding fixed-width" min-width="250" fixed="right">
        <template slot-scope="scope">
          <defect-tools class="defect-row-tools" :exclusions="defectToolsExclusions" :is-text="true" :defect="scope.row" size="mini" :is-show-icon="true" @delete="handleDelete" @update="search" @log="search"></defect-tools>
        </template>
      </el-table-column>
    </el-table>
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="search(queryParams)"
    />
  </div>
</template>

<script>
import LevelTag from "@/components/LevelTag";
import Cat2BugText from "@/components/Cat2BugText";
import RowListMember from "@/components/RowListMember";
import Cat2BugPreviewImage from "@/components/Cat2BugPreviewImage";
import FocusMemberList from "@/components/FocusMemberList";
import DefectTypeFlag from "@/components/Defect/DefectTypeFlag";
import DefectStateFlag from "@/components/Defect/DefectStateFlag";
import DefectTools from "@/components/Defect/DefectTools";
import SelectModule from "@/components/Module/SelectModule";
import SelectProjectMember from "@/components/Project/SelectProjectMember";
import {parseTime} from "@/utils/ruoyi";
import {lifeTime} from "@/utils/defect";
import {listDefect} from "@/api/system/defect";
import {listDefectOfPlan} from "@/api/system/plan";
import {getDefectState} from "@/api/system/DefectState";

/** 需要显示的缺陷字段列表在缓存的key值 */
const DEFECT_TABLE_FIELD_LIST_CACHE_KEY='plan-defect-table-field-list';
/** 用例表排序的列 */
const DEFECT_TABLE_SORT_COLUMN = 'plan-defect_table_sort_column_key';
/** 用例表排序的类型（正序、倒叙） */
const DEFECT_TABLE_SORT_TYPE = 'plan-defect_table_sort_type_key';
export default {
  name: "DefectList",
  dicts: ['defect_level'],
  components: { LevelTag, Cat2BugText, RowListMember, Cat2BugPreviewImage, FocusMemberList, DefectTypeFlag, DefectStateFlag, DefectTools, SelectModule, SelectProjectMember },
  data() {
    return {
      // 鼠标是否点击
      mouseFlag: false,
      // 鼠标移动的偏移量
      mouseOffset: 0,
      loading: false,
      defectToolsExclusions: ['delete'],
      plan: {},
      planItem: {},
      // 是否选择了所有
      isCheckAll: false,
      // 全选组件的状态
      isIndeterminate: false,
      // 勾选的缺陷ID列表
      checkedDefectList: [],
      tableKey: (new Date()).getMilliseconds(),
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 总条数
      total: 0,
      // 缺陷表格数据
      defectList: [],
      // 缺陷状态数组
      defectStates: [],
      // 表格组件相关配置
      // 选中的表格列数据集合
      tableShowFieldList: [],
      // 表格里全部列数据集合
      tableAllFieldList: [
        'id','type','defect.name','priority','state','module','version','image','annex','update-time','plan-start-time','plan-end-time','createBy','handle-by'
      ],
      // 查询条件
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        orderByColumn: 'projectNum',
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
          defectStates: null
        }
      }
    }
  },
  watch: {
    "$i18n.locale": function (newVal, oldVal) {
      this.refreshShowFields();
    },
  },
  directives: {
    resize: {
      // 指令的名称
      bind(el, binding) {
        // el为绑定的元素，binding为绑定给指令的对象
        let width = ''
        let height = ''
        function isResize() {
          const style = document.defaultView.getComputedStyle(el);
          if (width !== style.width || height !== style.height) {
            binding.value({ width: style.width, height: style.height }) // 关键(这传入的是函数,所以执行此函数)
          }
          width = style.width
          height = style.height
        }
        el.__vueSetInterval__ = setInterval(isResize, 300)
      },
      unbind(el) {
        clearInterval(el.__vueSetInterval__)
      }
    }
  },
  computed: {
    /** 项目ID */
    projectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 缺陷批量工具是否可用 */
    batchToolsDisabled: function () {
      return function (defect) {
        return this.checkedDefectList.filter(d=>d==defect.defectId).length==0;
      }
    },
    /** 缺陷的存活时间 */
    defectLife: function () {
      return function (defect) {
        return lifeTime(defect);
      }
    },
    /** 字段是否显示 */
    showField: function () {
      return function (field) {
        return this.tableShowFieldList.filter(f=>f==field).length>0;
      }
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
    },
  },
  created() {
    this.initDefectState();
  },
  methods: {
    parseTime,
    /** 初始化缺陷状态 */
    initDefectState() {
      getDefectState().then(res=>{
        this.defectStates = res.data;
      });
    },
    setDragComponentSize() {
      // 组件尺寸改变
      this.$emit('resize');
    },
    initFloatMenu() {
      this.$emit('init-float-menu');
    },
    open(planId, projectId, query) {
      this.queryParams.projectId = projectId;
      this.queryParams = {...this.queryParams, ...query}
      this.queryParams.planId = planId;
      if(!this.queryParams.params) {
        this.queryParams.params = {};
      }
      this.init();
      this.search(this.queryParams);
    },
    init() {
      this.refreshShowFields();
      this.initSort();
    },
    /** 初始化排序数据 */
    initSort() {
      this.queryParams.isAsc = this.$cache.local.get(DEFECT_TABLE_SORT_TYPE)||null;
      this.queryParams.orderByColumn = this.$cache.local.get(DEFECT_TABLE_SORT_COLUMN)||null;
      this.$nextTick(()=>{
        this.$refs.defectTable.sort(this.queryParams.orderByColumn, this.queryParams.isAsc);
      });
    },
    /** 保存表格显示哪些属性 */
    saveShowFields(field) {
      this.$cache.local.setJSON(DEFECT_TABLE_FIELD_LIST_CACHE_KEY,field);
    },
    /** 获取表格显示哪些属性 */
    getShowFields() {
      return this.$cache.local.getJSON(DEFECT_TABLE_FIELD_LIST_CACHE_KEY);
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.defectId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 缺陷列表属性字段改变操作 */
    checkedFieldListChange(field) {
      this.saveShowFields(field);
      this.refreshShowFields();
    },
    /** 设置列表显示的属性字段 */
    refreshShowFields() {
      const fieldList = this.getShowFields();
      if(fieldList && fieldList.length>0) {
        this.tableShowFieldList = fieldList;
      } else {
        this.tableShowFieldList = [];
        this.tableAllFieldList.forEach(f=>{
          this.tableShowFieldList.push(f);
        });
      }
      this.$nextTick(()=>{
        this.$refs.defectTable.doLayout();
      });
    },
    /** 排序改变的处理 */
    sortChangeHandle(e) {
      this.$cache.local.set(DEFECT_TABLE_SORT_COLUMN, e.prop);
      this.$cache.local.set(DEFECT_TABLE_SORT_TYPE, e.order);
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
        this.queryParams.isAsc=e.order;
      } else {
        this.queryParams.orderByColumn=null;
        this.queryParams.isAsc=null;
      }
      this.queryParams.pageNum = 1;
      this.search(this.queryParams);
    },
    /** 查询缺陷列表 */
    search(params) {
      this.loading = true;
      this.queryParams = params||this.queryParams;
      listDefectOfPlan(this.queryParams.planId, this.queryParams).then(response => {
        this.loading = false;
        this.defectList = response.rows;
        this.total = response.total;
      });
    },
    /** 处理删除缺陷 */
    handleDelete() {
      this.search();
      this.$emit('delete');
    },
    /** 显示图片操作 */
    clickImageHandle(event){
      event.stopPropagation();
    },
    /** 处理点击了表格中的某一行 */
    handleClickTableRow(defect) {
      this.$emit('defect-click',defect);
    },
    /** 下载附件操作 */
    handleDown(event, file) {
      const a = document.createElement("a");
      const e = new MouseEvent("click");
      a.href = file;
      a.dispatchEvent(e);
      event.stopPropagation();
    },
    /** 全选组件勾选状态的改变处理 */
    handleCheckAllChange(value) {
      this.checkedDefectList = value ? this.defectList.map(d=>d.defectId) : [];
      this.isIndeterminate = false;
    },
    /** 勾选某个缺陷的改变处理 */
    handleCheckedDefectChange(value) {
      let checkedCount = value.length;
      this.checkAll = checkedCount === this.defectList.length;
      this.isIndeterminate = checkedCount > 0 && checkedCount < this.defectList.length;

    },
    /** 阻止冒泡事件传递处理 */
    handleStopPropagation(event) {
      event.stopPropagation();
    },

    /** 处理鼠标在表格点下事件 */
    handleTableMouseDown(e) {
      this.mouseOffset = e.clientX;
      this.mouseFlag = true;
    },
    /** 处理鼠标在表格点起事件 */
    handleTableMouseUp(e) {
      this.mouseFlag = false;
    },
    /** 处理鼠标在表格移动事件 */
    handleTableMouseMove(e) {
      // 这里面需要注意，通过ref需要那个那个包含table元素的父元素
      let tableBody = this.$refs.defectTable.bodyWrapper;
      if (this.mouseFlag) {
        // 设置水平方向的元素的位置
        tableBody.scrollLeft -= (- this.mouseOffset + (this.mouseOffset = e.clientX));
      }
    },
  }
}
</script>

<style lang="scss" scoped>
.plan-item-content {
  flex-grow: 1;
  overflow:hidden;
  height: 100%;
  padding-bottom: 30px;
}
.plan-item-query {
  display: inline-flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: flex-start;
  width: 100%;
  .el-form-item {
    margin-bottom: 5px;
  }
}
.handle-plan-tools-right {
  display: inline-flex;
  flex-direction: row;
  gap: 10px;
}
.plan-item-field-divider {
  margin: 8px 0px;
}
.row {
  display: flex;
  flex-direction: row;
  align-items: center;
  > * {
    margin: 0px 5px 0px 0px;
  }
}
.col {
  display: flex;
  flex-direction: column;
}
.defect-field-divider {
  margin: 8px 0px;
}
</style>
