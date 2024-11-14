<template>
  <cat2bug-drawer
    :title="$i18n.t('defect.manage').toString()"
    ref="cat2bugDrawer">
    <template slot="content">
      <el-table v-loading="loading" :data="defectList" @sort-change="sortChangeHandle" @row-click="editDefectHandle">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column :label="$t('id')" align="left" prop="projectNum" width="80" sortable >
          <template slot-scope="scope">
            <span>{{ '#' + scope.row.projectNum }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('type')" align="left" prop="defectTypeName" width="80" sortable>
          <template slot-scope="scope">
            <defect-type-flag :defect="scope.row" />
          </template>
        </el-table-column>
        <el-table-column :label="$t('defect.name')" align="left" prop="defectName" sortable >
          <template slot-scope="scope">
            <el-link type="primary" @click="editDefectHandle(scope.row)">{{ scope.row.defectName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column :label="$t('priority')" align="left" prop="defectLevel" width="100" sortable >
          <template slot-scope="scope">
            <level-tag :options="dict.type.defect_level" :value="scope.row.defectLevel"/>
          </template>
        </el-table-column>
        <el-table-column :label="$t('state')" align="left" prop="defectStateName" width="120" sortable />
        <el-table-column :label="$t('module')" align="left" prop="moduleName" width="150" sortable />
        <el-table-column :label="$t('handle-by')" align="left" prop="handleBy">
          <template slot-scope="scope">
            <row-list-member :members="scope.row.handleByList"></row-list-member>
          </template>
        </el-table-column>
        <el-table-column :label="$t('image')" align="left" prop="imgUrls">
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
        <el-table-column :label="$t('operate')" align="left" class-name="small-padding fixed-width" width="150">
          <template slot-scope="scope">
            <defect-tools :is-text="true" :defect="scope.row" size="mini" :is-show-icon="true" @log="getList" />
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
    </template>
  </cat2bug-drawer>
</template>

<script>
import Cat2bugDrawer from "@/components/Cat2bugDrawer";
import {listDefect, getDefect, delDefect, addDefect, updateDefect, configDefect} from "@/api/system/defect";
import RowListMember from "@/components/RowListMember";
import LevelTag from "@/components/LevelTag";
import AddDefect from "@/components/Defect/AddDefect"
import SelectModule from "@/components/Module/SelectModule";
import SelectProjectMember from "@/components/Project/SelectProjectMember";
import ProjectLabel from "@/components/Project/ProjectLabel";
import DefectTypeFlag from "@/components/Defect/DefectTypeFlag";
import DefectTools from "@/components/Defect/DefectTools";

export default {
  name: "ListDefect",
  components: {Cat2bugDrawer,SelectModule, RowListMember, AddDefect, LevelTag, SelectProjectMember,ProjectLabel,DefectTypeFlag, DefectTools },
  dicts: ['defect_level'],
  data() {
    return {
      drawer: false,
      // 缺陷配置
      config:{},
      // 遮罩层
      loading: true,
      // 总条数
      total: 0,
      // 缺陷表格数据
      defectList: [],
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
        defectLevel: null
      },
    }
  },
  props: {
  },
  computed: {
    getUrl: function () {
      return function (urls){
        let imgs = urls?urls.split(','):[];
        return imgs.map(i=>{
          return process.env.VUE_APP_BASE_API + i;
        })
      }
    },
  },
  methods: {
    open() {
      this.$refs.cat2bugDrawer.open();
      this.getDefectConfig();
      this.getList();
    },
    getDefectConfig() {
      configDefect().then(res=>{
        this.config = res.data;
      })
    },
    /** 获取项目id */
    getProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
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
      this.getList();
    },
    /** 查询缺陷列表 */
    getList(params) {
      this.loading = true;
      if(params)
        this.queryParams.params = params;
      else
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
    /** 打开编辑界面的处理 */
    editDefectHandle(defect) {
      // this.$refs.editDefectForm.open(defect.defectId);
    },
    clickImageHandle(event){
      event.stopPropagation();
    }
  }
}
</script>

<style scoped>

</style>
