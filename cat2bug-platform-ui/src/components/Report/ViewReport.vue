<template>
  <el-drawer
    size="90%"
    :visible.sync="visible"
    direction="rtl"
    :before-close="closeReportDrawer">
    <template slot="title">
      <div class="defect-edit-header">
        <div class="defect-edit-title">
          <i class="el-icon-arrow-left" @click="cancel"></i>
          <h4 class="defect-edit-title-num">{{report.reportTitle}}</h4>
        </div>
        <div class="defect-edit-tools">

        </div>
      </div>
    </template>
    <div class="app-container" v-loading="loading">

      <markdown-it-vue class="md" :content="report.reportDescription+''" />
    </div>
  </el-drawer>
</template>

<script>
import MarkdownItVue from "markdown-it-vue"
import 'markdown-it-vue/dist/markdown-it-vue.css'

import {getReport} from "@/api/system/report";

export default {
  name: "ViewReport",
  components: { MarkdownItVue },
  data() {
    return {
      loading: false,
      // 显示窗口
      visible: false,
      report: {},
    }
  },
  props: {
    projectId: {
      type: Number,
      default: null
    },
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
  methods:{
    // 获取缺陷信息
    getReportInfo(reportId) {
      this.loading = true;
      getReport(reportId).then(res=>{
        this.loading = false;
        this.report = res.data;
      }).catch(e=>{
        this.loading = false;
      });
    },
    // 打开操作
    open(reportId) {
      this.visible = true;
      this.reset();
      this.reportId = reportId;
      this.getReportInfo(reportId);
      this.$nextTick(()=>{

      });
    },
    // 取消按钮
    cancel() {
      this.visible = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        defectId: null,
        defectType: null,
        defectName: null,
        defectDescribe: null,
        annexUrls: null,
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
        caseStepId: null,
        handleBy: null,
        handleTime: null,
        defectLevel: 'middle'
      };
      this.resetForm("form");
    },
    /** 关闭缺陷抽屉窗口 */
    closeReportDrawer(done) {
      done();
    },
    handleByChangeHandle(members) {
      console.log(members, this.form.handleBy);
    },
    deleteHandle() {
      this.$emit('delete',this.defect);
      this.cancel();
    },
  }
}
</script>

<style lang="scss" scoped>
  ::v-deep .el-drawer {
    border-left: 3px solid #ffbe00;
  }
  ::v-deep .el-drawer__header {
    margin-bottom: 0px;
    max-width: calc(100vw);
  }
  .defect-edit-header {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-direction: row;
    flex-wrap: wrap;
    .defect-edit-tools {
      padding: 5px 0px;
      width: 100%;
      overflow: auto;
    }
    .defect-edit-title {
      display: inline-block;
      display: inline-flex;
      justify-content: flex-start;
      align-items: center;
      flex-direction: row;
      overflow: hidden;
      > * {
        float:left;
      }
      .el-icon-arrow-left {
        font-size: 22px;
      }
      .el-icon-arrow-left:hover {
        cursor: pointer;
        color: #909399;
      }
      .defect-edit-title-name {
        flex: 1;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
      }
      .defect-edit-title-num, .defect-edit-title-name {
        font-size: 20px;
        color: #303133;
        font-weight: 500;
        margin-top: 10px;
        margin-bottom: 10px;
      }
      > * {
        margin-right: 10px;
      }
    }
  }
  ::v-deep .el-drawer__close-btn {
    display: none;
  }
  ::v-deep .md {
    margin: 0 10px;
    span {
      display: inline-block;
      word-wrap: break-word;
      white-space: normal;
    }
  }

</style>
