<template>
  <div>
    <div class="defect-table-tools">
      <slot name="left-tools"></slot>
      <div class="table-tools row">
        <el-popover placement="top" trigger="click">
          <div class="row">
            <i class="el-icon-s-fold"></i>
            <h4>{{ $t('display-field') }}</h4>
          </div>
          <el-divider class="defect-field-divider"></el-divider>
          <el-checkbox-group v-model="columnPickerCheckedKeys" class="defect-column-picker" @change="onColumnPickerChange">
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
        <div>
          <slot name="right-tools"></slot>
        </div>
      </div>
    </div>
    <cat2-bug-table
      ref="cat2BugTable"
      cache-key="defect-table"
      :columns="tableColumnDefaults"
      :data="defectList"
      :loading="loading"
      @sort-change="sortChangeHandle"
      @columns-change="onTableColumnsChange">
      <template #columns="{ scope, column }">
        <span v-if="column.prop==='projectNum'" >{{ '#' + scope.row[column.prop] }}</span>
        <defect-type-flag v-else-if="column.prop==='defectTypeName'" :defect="scope.row" />
        <div v-else-if="column.prop==='defectName'" class="table-defect-title">
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
        <level-tag v-else-if="column.prop==='defectLevel'" :options="dict.type.defect_level" :value="scope.row.defectLevel"/>
        <defect-state-flag v-else-if="column.prop==='defectState'" :defect="scope.row" />
        <cat2-bug-preview-image v-else-if="column.prop==='imgUrls'" :images="getUrl(scope.row.imgUrls)" />
        <div v-else-if="column.prop==='annexUrls'" class="annex-list">
          <cat2-bug-text :content="file" type="down" :tooltip="file" v-for="(file,index) in getUrl(scope.row.annexUrls)" :key="index" />
        </div>
        <span v-else-if="column.prop==='updateTime'">{{ parseTime(scope.row.updateTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        <span v-else-if="column.prop==='planStartTime'">{{ parseTime(scope.row.planStartTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        <span v-else-if="column.prop==='planEndTime'">{{ parseTime(scope.row.planEndTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        <row-list-member v-else-if="column.prop==='createMember'" :members="[scope.row.createMember]"></row-list-member>
        <row-list-member v-else-if="column.prop==='handleBy'" :members="scope.row.handleByList"></row-list-member>
        <span v-else>{{ scope.row[column.prop] }}</span>
      </template>
      <template #append>
        <el-table-column :label="$t('operate')" align="center" class-name="no-drag small-padding fixed-width" min-width="250" fixed="right">
          <template slot-scope="scope">
            <div class="row">
              <defect-tools class="defect-row-tools" :is-text="true" :defect="scope.row" size="mini" :is-show-icon="true" @view="handleClickTableRow" @delete="refreshSearch" @update="refreshSearch" @log="refreshSearch"></defect-tools>
            </div>
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
import {listDefect} from "@/api/system/defect";
import {lifeTime} from "@/utils/defect";
import { TableOptions } from "@/views/system/defect/list/table-options";

export default {
  name: "DefectTable",
  dicts: ['defect_level'],
  components: {RowListMember, Cat2BugPreviewImage, LevelTag, FocusMemberList, DefectTypeFlag, DefectStateFlag, DefectTools, Cat2BugText, Cat2BugTable },
  data() {
    return {
      mouseFlag: false,
      mouseOffset: 0,
      mouseClickTime: 0,
      isCheckAll: false,
      isIndeterminate: false,
      checkedDefectList: [],
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      total: 0,
      defectList: [],
      queryParams: this.query,
      /** 缺陷列表表格默认列（克隆自 table-options，避免与全局常量引用互相污染） */
      tableColumnDefaults: TableOptions.map(c => ({ ...c })),
      columnPickerCheckedKeys: [],
    }
  },
  props: {
    query: {
      type: Object,
      default: () => {
        return {
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
          params:{}
        }
      }
    }
  },
  computed: {
    defectColumnPickerOptions() {
      return TableOptions.filter(c => c.showInColumnPicker !== false);
    },
    defectLife: function () {
      return function (defect) {
        return lifeTime(defect);
      }
    },
    getUrl: function () {
      return function (urls){
        if(urls) {
          let files = urls ? urls.split(',') : [];
          return files.map(i => {
            return process.env.VUE_APP_BASE_API + i;
          })
        } else {
          return [];
        }
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
  methods: {
    init() {},
    onTableColumnsChange(columns) {
      this.columnPickerCheckedKeys = columns.filter(c => c.visible && c.showInColumnPicker !== false).map(c => c.key);
    },
    onColumnPickerChange(keys) {
      this.$refs.cat2BugTable && this.$refs.cat2BugTable.setColumnsVisible(keys);
    },
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
    search(params) {
      this.loading = true;
      this.queryParams = params;
      listDefect(params).then(response => {
        this.loading = false;
        this.defectList = response.rows;
        this.total = response.total;
      });
    },
    refreshSearch() {
      this.$emit('refresh');
    },
    clickImageHandle(event){
      event.stopPropagation();
    },
    handleClickTableRow(defect) {
      if(defect && defect.defectId)
        this.$emit('defect-click',defect);
    },
    handleDown(event, file) {
      const a = document.createElement("a");
      const e = new MouseEvent("click");
      a.href = file;
      a.dispatchEvent(e);
      event.stopPropagation();
    },
  }
}
</script>

<style lang="scss" scoped>
@media screen and (max-width: 980px) {
  .defect-table-tools {
    justify-content: flex-end;
  }
}
@media screen and (min-width: 980px) {
  .defect-table-tools {
    justify-content: space-between;
  }
}
.defect-table-tools {
  display: flex;
  flex-direction: row;
  align-items: center;
  > * {
    display: inline-block;
    justify-content: flex-start;
    margin-bottom: 0px;
    ::v-deep .el-form-item {
      margin-bottom: 0px;
    }
  }
  .table-tools {
    align-items: flex-start;
    padding-top: 3px;
    > * {
      margin-bottom: 10px;
    }
  }
}
.defect-field-divider {
  margin: 8px 0px;
}
/** 显示字段：勿限制 checkbox-group 宽高（原先 15px 会导致选项重叠错乱） */
.defect-column-picker {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 220px;
  max-height: 380px;
  overflow-y: auto;
  padding-right: 4px;
}
.defect-column-picker ::v-deep .el-checkbox {
  display: flex;
  align-items: center;
  margin-right: 0;
  white-space: nowrap;
}
.defect-column-picker ::v-deep .el-checkbox__input {
  flex-shrink: 0;
}
.defect-column-picker ::v-deep .el-checkbox__label {
  line-height: 1.4;
  padding-left: 8px;
}
.row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  > * {
    margin: 0px;
  }
}
.table-defect-title {
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  .el-link {
    flex: 1;
    padding-left: 5px;
  }
}
.el-table {
  ::v-deep table {
    width: 100% !important;
  }
}
.defect-statistics {
  display: inline-flex;
  flex-direction: row;
  gap: 10px;
  font-size: 10px;
  > div {
    padding: 0px 5px;
    border-radius: 3px;
    background-color: #f9fbff;
  }
  i {
    margin-right: 2px;
  }
  .defect-statistics-value {
    padding-left: 3px;
    font-size: 11px;
    color: #303133;
  }
}
.annex-link {
  white-space: normal;
  text-align: center;
  word-break: break-all;
}
.defect-row-tools {
  margin-left: 10px;
}
.annex-list {
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  > *:last-child {
    border-bottom: 0px;
  }
  > * {
    border-bottom: 1px dashed #E4E7ED;
  }
}
</style>
