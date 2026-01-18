<template>
  <div>
    <!--查询-->
    <div class="defect-table-tools">
      <slot name="left-tools"></slot>
      <div class="table-tools row">
        <el-popover
          v-show="$refs.defectTable"
          placement="top"
          trigger="click">
          <div class="row">
            <i class="el-icon-s-fold"></i>
            <h4>{{$t('defect.display-field')}}</h4>
          </div>
          <el-divider class="defect-field-divider"></el-divider>
          <el-checkbox-group v-model="tableShowFieldList" class="col" @change="checkedFieldListChange">
            <el-checkbox v-for="field in tableFieldList" :label="field.key" :key="field.key">{{$t(field.key)}}</el-checkbox>
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
    <!--表格-->
    <cat2-bug-table
      ref="defectTable"
      cache-key="defect-table"
      :columns="tableFieldList"
      :data="defectList"
      @columns-change="handleTableColumnsChange"
      @sort-change="sortChangeHandle">
      <template v-slot:columns="{scope, column}">
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
          <defect-type-flag v-else-if="column.prop==='defectTypeName'" :defect="scope.row" />
          <span v-else-if="column.prop==='updateTime'">{{ parseTime(scope.row.updateTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
          <span v-else-if="column.prop==='planStartTime'">{{ parseTime(scope.row.planStartTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
          <span v-else-if="column.prop==='planEndTime'">{{ parseTime(scope.row.planEndTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
          <row-list-member v-else-if="column.prop==='createMember'" :members="[scope.row.createMember]"></row-list-member>
          <row-list-member v-else-if="column.prop==='handleBy'" :members="scope.row.handleByList"></row-list-member>
          <span v-else>{{ scope.row[column.prop] }}</span>
      </template>
      <el-table-column :label="$t('operate')" align="left" class-name="no-drag small-padding fixed-width" min-width="250" fixed="right">
        <template slot-scope="scope">
          <defect-tools class="defect-row-tools" :is-text="true" :defect="scope.row" size="mini" :is-show-icon="true" @delete="refreshSearch" @update="refreshSearch" @log="refreshSearch"></defect-tools>
        </template>
      </el-table-column>
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
import {TableOptions} from "@/views/system/defect/list/table-options";

export default {
  name: "DefectTable",
  dicts: ['defect_level'],
  components: {RowListMember, Cat2BugPreviewImage, LevelTag, FocusMemberList, DefectTypeFlag, DefectStateFlag, DefectTools, Cat2BugText, Cat2BugTable },
  data() {
    return {
      // 鼠标是否点击
      mouseFlag: false,
      // 鼠标移动的偏移量
      mouseOffset: 0,
      // 鼠标点击时间
      mouseClickTime: 0,
      // 是否选择了所有
      isCheckAll: false,
      // 全选组件的状态
      isIndeterminate: false,
      // 勾选的缺陷ID列表
      checkedDefectList: [],
      // 遮罩层
      loading: true,
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
      // 查询参数
      queryParams: this.query,
      // 表格列表
      tableFieldList: TableOptions,
      // 选中的表格列数据集合
      tableShowFieldList: [],
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
    /** 缺陷的存活时间 */
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
    /** 缺陷列表属性字段改变操作 */
    checkedFieldListChange(fields) {
      this.$refs.defectTable.setColumnsVisible(fields);
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
        // this.queryParams.isAsc=e.order=='ascending'?"asc":'desc';
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
      this.queryParams = params;
      listDefect(params).then(response => {
        this.loading = false;
        this.defectList = response.rows;
        this.total = response.total;
      });
    },
    /** 刷新查询 */
    refreshSearch() {
      this.$emit('refresh');
    },
    /** 显示图片操作 */
    clickImageHandle(event){
      event.stopPropagation();
    },
    /** 处理点击了表格中的某一行 */
    handleClickTableRow(defect) {
      if(defect && defect.defectId)
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
    /** 表格列改变事件 */
    handleTableColumnsChange(columns) {
      this.tableFieldList = columns;
      this.tableShowFieldList = columns.filter(c=>c.visible).map(c=>c.key);
    }
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

.table-row {
  width: 100%;
  display: inline-flex;
  justify-content: flex-start;
  align-items: center;
  flex-direction: row;
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
.el-checkbox-group {
  width: 15px;
  height: 15px;
  line-height: 15px;
}
.defect-check-tools {
  display: flex;
  flex-direction: column;
  gap: 3px;
  > * {
    font-size: 0.7rem;
  }
  ::v-deep .el-button {
    width: 8px;
    height: 8px;
    margin: 0px;
    padding: 0px;
    border-width: 0px;
  }
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
.col {
  display: flex;
  flex-direction: column;
  height: auto;
}
.defect-field-divider {
  margin: 8px 0px;
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
