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
          <el-checkbox-group
            :key="'plan-defect-colpick-' + planDefectColumnPickerRev"
            v-model="columnPickerCheckedKeys"
            class="col"
            @change="onColumnPickerChange"
          >
            <el-checkbox v-for="c in defectColumnPickerOptions" :key="c.key" :label="c.key">{{ $t(c.key) }}</el-checkbox>
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
    <cat2-bug-table
      ref="cat2BugTable"
      field-list-cache-key="plan-defect-table-field-list"
      :sort-column-cache-key="planDefectSortColumnKey"
      :sort-type-cache-key="planDefectSortTypeKey"
      :columns="planDefectTableColumns"
      :data="defectList"
      :loading="loading"
      @sort-change="sortChangeHandle"
      @selection-change="handleSelectionChange"
      @row-click="handleClickTableRow"
      @columns-change="onTableColumnsChange"
      @native-mousedown="handleTableMouseDown"
      @native-mouseup="handleTableMouseUp"
      @native-mousemove="handleTableMouseMove"
    >
      <template #columns="{ scope, column }">
        <span v-if="column.prop === 'projectNum'">{{ '#' + scope.row[column.prop] }}</span>
        <defect-type-flag v-else-if="column.prop === 'defectTypeName'" :defect="scope.row" />
        <div v-else-if="column.prop === 'defectName'" class="table-defect-title">
          <focus-member-list
            v-show="scope.row.focusList && scope.row.focusList.length > 0"
            v-model="scope.row.focusList"
            module-name="defect"
            :data-id="scope.row.defectId"
          />
          <el-link type="primary" @click="handleClickTableRow(scope.row)">{{ scope.row.defectName }}</el-link>
          <div class="defect-statistics">
            <div>
              <i class="el-icon-time"></i>
              <span>{{ $t('defect.life-time') }}:</span>
              <span class="defect-statistics-value">{{ defectLife(scope.row) }}</span>
            </div>
            <div>
              <i class="el-icon-document-delete"></i>
              <span>{{ $i18n.t('reject') }}:</span>
              <span class="defect-statistics-value">{{ scope.row.rejectCount }}</span>
            </div>
          </div>
        </div>
        <level-tag v-else-if="column.prop === 'defectLevel'" :options="dict.type.defect_level" :value="scope.row.defectLevel" />
        <defect-state-flag v-else-if="column.prop === 'defectState'" :defect="scope.row" />
        <cat2-bug-preview-image v-else-if="column.prop === 'imgUrls'" :images="getUrl(scope.row.imgUrls)" />
        <div v-else-if="column.prop === 'annexUrls'" class="annex-list">
          <cat2-bug-text :content="file" type="down" :tooltip="file" v-for="(file, index) in getUrl(scope.row.annexUrls)" :key="index" />
        </div>
        <span v-else-if="column.prop === 'updateTime'">{{ parseTime(scope.row.updateTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        <span v-else-if="column.prop === 'planStartTime'">{{ parseTime(scope.row.planStartTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        <span v-else-if="column.prop === 'planEndTime'">{{ parseTime(scope.row.planEndTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        <row-list-member v-else-if="column.prop === 'createMember'" :members="[scope.row.createMember]" />
        <row-list-member v-else-if="column.prop === 'handleBy'" :members="scope.row.handleByList" />
        <span v-else>{{ scope.row[column.prop] }}</span>
      </template>
      <template #append>
        <el-table-column :label="$t('operate')" align="left" class-name="no-drag small-padding fixed-width" min-width="250" fixed="right">
          <template slot-scope="scope">
            <defect-tools
              class="defect-row-tools"
              :exclusions="defectToolsExclusions"
              :is-text="true"
              :defect="scope.row"
              size="mini"
              :is-show-icon="true"
              @delete="handleDelete"
              @update="search"
              @log="search"
            />
          </template>
        </el-table-column>
      </template>
    </cat2-bug-table>
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
import Cat2BugTable from "@/components/Cat2BugTable";
import SelectModule from "@/components/Module/SelectModule";
import SelectProjectMember from "@/components/Project/SelectProjectMember";
import { TableOptions } from "@/views/system/defect/list/table-options";
import {parseTime} from "@/utils/ruoyi";
import {lifeTime} from "@/utils/defect";
import {listDefectOfPlan} from "@/api/system/plan";
import {getDefectState} from "@/api/system/DefectState";

/** 用例表排序的列 */
const DEFECT_TABLE_SORT_COLUMN = 'plan-defect_table_sort_column_key';
/** 用例表排序的类型（正序、倒叙） */
const DEFECT_TABLE_SORT_TYPE = 'plan-defect_table_sort_type_key';
export default {
  name: "DefectList",
  dicts: ['defect_level'],
  components: { LevelTag, Cat2BugText, RowListMember, Cat2BugPreviewImage, FocusMemberList, DefectTypeFlag, DefectStateFlag, DefectTools, Cat2BugTable, SelectModule, SelectProjectMember },
  data() {
    return {
      planDefectSortColumnKey: DEFECT_TABLE_SORT_COLUMN,
      planDefectSortTypeKey: DEFECT_TABLE_SORT_TYPE,
      planDefectTableColumns: TableOptions.map((c) => ({ ...c })),
      columnPickerCheckedKeys: [],
      planDefectColumnPickerRev: 0,
      defectPickerColumnList: null,
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
    "$i18n.locale": function () {
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout();
      });
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
    defectColumnPickerOptions() {
      const ordered = this.defectPickerColumnList;
      if (ordered && ordered.length) {
        return ordered.map((c) => ({ ...c }));
      }
      return TableOptions.filter((c) => c.showInColumnPicker !== false);
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
      this.$nextTick(() => {
        this.initSort();
      });
    },
    /** 初始化排序数据 */
    initSort() {
      this.queryParams.isAsc = this.$cache.local.get(DEFECT_TABLE_SORT_TYPE) || null;
      this.queryParams.orderByColumn = this.$cache.local.get(DEFECT_TABLE_SORT_COLUMN) || null;
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.sort(this.queryParams.orderByColumn, this.queryParams.isAsc);
      });
    },
    onTableColumnsChange(columns) {
      this.planDefectColumnPickerRev += 1;
      const picker = columns.filter((c) => c.showInColumnPicker !== false).map((c) => ({ ...c }));
      this.$set(this, 'defectPickerColumnList', picker);
      this.columnPickerCheckedKeys = columns
        .filter((c) => c.visible && c.showInColumnPicker !== false)
        .map((c) => c.key);
    },
    onColumnPickerChange(keys) {
      this.$refs.cat2BugTable && this.$refs.cat2BugTable.setColumnsVisible(keys);
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.defectId)
      this.single = selection.length!==1
      this.multiple = !selection.length
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
      const elTable = this.$refs.cat2BugTable && this.$refs.cat2BugTable.$refs.elTable;
      if (!elTable) return;
      let tableBody = elTable.bodyWrapper;
      if (this.mouseFlag) {
        // 设置水平方向的元素的位置
        tableBody.scrollLeft -= (- this.mouseOffset + (this.mouseOffset = e.clientX));
      }
    },
    /** 处理计划项状态搜索事件 */
    handlePlanItemStateSearch(state) {

    }
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
