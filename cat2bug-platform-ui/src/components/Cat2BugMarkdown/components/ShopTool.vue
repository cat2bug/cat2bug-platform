<template>
  <el-drawer
    title="我是标题"
    :visible.sync="drawer"
    :direction="direction"
    :before-close="handleClose">
    <template slot="title">
      <div class="report-shop-header">
        <i class="el-icon-arrow-left" @click="cancel"></i>
        <h4>{{$t('report.template-shop')}}</h4>
      </div>
    </template>
    <div class="report-shop-body" v-loading="loading">
      <div>
        <div v-for="(t,index) in templateList" :key="index">
          <el-image :src="imgUrl(t)" />
          <span>{{t.templateTitle}}</span>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script>
import {listReportTemplate} from "@/api/system/ReportTemplate";

export default {
  name: "ShopTool",
  data() {
    return {
      loading: false,
      view: null,
      drawer: false,
      direction: 'rtl',
      templateList: [],
      query: {
        projectId: this.projectId,
        pageNum: 1,
        pageSize: 9,
        isShop: true
      },
    };
  },
  computed: {
    imgUrl() {
      return function (template) {
        return process.env.VUE_APP_BASE_API + template.templateIconUrl;
      }
    }
  },
  methods: {
    /** 组件执行 */
    run(view,tools,tool) {
      this.drawer = true;
      this.view = view;
      this.getReportTemplate();
    },
    /** 取消按钮 */
    cancel() {
      this.drawer = false;
    },
    toolHandle() {
      const desc = this.form.describe?this.form.describe:'';
      const url = this.form.url?this.form.url:'';
      this.view.insertText(`[${desc}](${url})`);
      this.dialogFormVisible = false;
    },
    handleClose(done) {
      done();
    },
    getReportTemplate() {
      this.loading = true;
      listReportTemplate(this.query).then(res=>{
        this.loading = false;
        this.templateList = res.rows;
      });
    }
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
::v-deep .el-drawer__close-btn {
  display: none;
}
.report-shop-header {
  width: 100%;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  flex-direction: row;
  flex-wrap: wrap;
  .el-icon-arrow-left {
    font-size: 22px;
  }
  .el-icon-arrow-left:hover {
    cursor: pointer;
    color: #909399;
  }
  h4 {
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
.report-shop-body {
  padding: 20px 30px;
}
</style>
