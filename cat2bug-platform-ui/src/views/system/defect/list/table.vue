<template>
  <div>
    <!--查询-->
    <div class="defect-table-tools">
      <slot name="left-tools"></slot>
      <div class="table-tools row">
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
        <div>
          <slot name="right-tools"></slot>
        </div>
      </div>
    </div>
    <!--表格-->
    <el-table ref="defectTable" :key="tableKey" v-loading="loading" style="width:100%;" :data="defectList" @selection-change="handleSelectionChange" @sort-change="sortChangeHandle" @row-click="handleClickTableRow">
<!--      多选项，后续版本开放 -->
<!--      <el-table-column width="50" align="start">-->
<!--        <template #header>-->
<!--          <el-checkbox key="allCheck" :indeterminate="isIndeterminate" v-model="isCheckAll" @change="handleCheckAllChange"></el-checkbox>-->
<!--        </template>-->
<!--        <template slot-scope="scope">-->
<!--          <div class="row">-->
<!--            <el-checkbox-group v-model="checkedDefectList" @change="handleCheckedDefectChange">-->
<!--              <el-checkbox :label="scope.row.defectId" :key="scope.row.defectId" @click.native="handleStopPropagation">{{''}}</el-checkbox>-->
<!--            </el-checkbox-group>-->
<!--            <div class="defect-check-tools">-->
<!--              <el-tooltip class="item" effect="dark" content="合并缺陷" placement="right">-->
<!--                <svg-icon icon-class="booknail" :disabled="batchToolsDisabled(scope.row)" />-->
<!--              </el-tooltip>-->
<!--              <el-tooltip class="item" effect="dark" content="合并缺陷" placement="right">-->
<!--                <el-button round type="warning" :disabled="batchToolsDisabled(scope.row)"></el-button>-->
<!--              </el-tooltip>-->
<!--              <el-tooltip class="item" effect="dark" content="批量删除" placement="right">-->
<!--                <el-button round type="danger" :disabled="batchToolsDisabled(scope.row)"></el-button>-->
<!--              </el-tooltip>-->
<!--            </div>-->
<!--          </div>-->
<!--        </template>-->
<!--      </el-table-column>-->
      <el-table-column v-if="showField('id')" :label="$t('id')" :key="$t('id')" align="left" prop="projectNum" width="80" sortable fixed >
        <template slot-scope="scope">
          <span>{{ '#' + scope.row.projectNum }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('type')" :label="$t('type')" :key="$t('type')" align="left" prop="defectTypeName" width="100" sortable fixed>
        <template slot-scope="scope">
          <defect-type-flag :defect="scope.row" />
        </template>
      </el-table-column>
      <el-table-column v-if="showField('defect.name')" :label="$t('defect.name')" :key="$t('defect.name')" align="left" prop="defectName" min-width="300" sortable fixed>
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
      <el-table-column v-if="showField('priority')" :label="$t('priority')" :key="$t('priority')" align="center" prop="defectLevel" width="100" sortable >
        <template slot-scope="scope">
          <level-tag :options="dict.type.defect_level" :value="scope.row.defectLevel"/>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('state')" :label="$t('state')" :key="$t('state')" align="center" prop="defectStateName" width="120" sortable>
        <template slot-scope="scope">
          <defect-state-flag :defect="scope.row" />
        </template>
      </el-table-column>
      <el-table-column v-if="showField('module')" :label="$t('module')" :key="$t('module')" align="left" prop="moduleName" min-width="200" sortable />
      <el-table-column v-if="showField('version')" :label="$t('version')" :key="$t('version')" align="left" prop="moduleVersion" width="100" sortable />
      <el-table-column v-if="showField('image')" :label="$t('image')" :key="$t('image')" align="left" prop="imgUrls">
        <template slot-scope="scope">
          <cat2-bug-preview-image :images="getUrl(scope.row.imgUrls)" />
        </template>
      </el-table-column>
      <el-table-column v-if="showField('annex')" :label="$t('annex')" :key="$t('annex')" align="left" prop="annexUrls">
        <template slot-scope="scope">
          <el-button class="annex-link" type="text"  v-for="(file,index) in getUrl(scope.row.annexUrls)" :key="index" @click="handleDown($event, file)">{{getFileName(file)}}</el-button>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('update-time')" :label="$t('update-time')" :key="$t('update-time')" align="left" prop="updateTime" width="160" sortable >
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('plan-start-time')" :label="$t('plan-start-time')" :key="$t('plan-start-time')" align="left" prop="planStartTime" width="160" sortable >
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.planStartTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('plan-end-time')" :label="$t('plan-end-time')" :key="$t('plan-end-time')" align="left" prop="planEndTime" width="160" sortable >
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
          <defect-tools class="defect-row-tools" :is-text="true" :defect="scope.row" size="mini" :is-show-icon="true" @delete="refreshSearch" @update="refreshSearch" @log="refreshSearch"></defect-tools>
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
import RowListMember from "@/components/RowListMember";
import Cat2BugPreviewImage from "@/components/Cat2BugPreviewImage";
import FocusMemberList from "@/components/FocusMemberList";
import DefectTypeFlag from "@/components/Defect/DefectTypeFlag";
import DefectStateFlag from "@/components/Defect/DefectStateFlag";
import DefectTools from "@/components/Defect/DefectTools";
import {listDefect} from "@/api/system/defect";
import {lifeTime} from "@/utils/defect";

/** 需要显示的缺陷字段列表在缓存的key值 */
const DEFECT_TABLE_FIELD_LIST_CACHE_KEY='defect-table-field-list';

export default {
  name: "DefectTable",
  dicts: ['defect_level'],
  components: {RowListMember, Cat2BugPreviewImage, LevelTag, FocusMemberList, DefectTypeFlag, DefectStateFlag, DefectTools},
  data() {
    return {
      // 是否选择了所有
      isCheckAll: false,
      // 全选组件的状态
      isIndeterminate: false,
      // 勾选的缺陷ID列表
      checkedDefectList: [],
      tableKey: (new Date()).getMilliseconds(),
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
      // 表格组件相关配置
      // 选中的表格列数据集合
      tableShowFieldList: [],
      // 表格里全部列数据集合
      tableAllFieldList: [
        'id','type','defect.name','priority','state','module','version','image','annex','update-time','plan-start-time','plan-end-time','createBy','handle-by'
      ],
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
  watch: {
    "$i18n.locale": function (newVal, oldVal) {
      this.refreshShowFields();
    },
  },
  computed: {
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
  methods: {
    init() {
      this.refreshShowFields();
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
      if(fieldList) {
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
</style>
