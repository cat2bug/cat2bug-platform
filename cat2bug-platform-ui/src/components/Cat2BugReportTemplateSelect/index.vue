<template>
  <el-popover
    placement="bottom"
    width="613"
    trigger="hover">
    <div class="template-header">
      <span>请选择报告模版</span>
      <div>
        <el-link icon="el-icon-edit" @click="handleReport">编辑模版</el-link>
      </div>
    </div>
    <div class="template-list">
      <div v-for="(t,index) in templateList" class="template-list-item" :key="index" @click="createReportHandle(t)">
        <el-image :src="iconUrl(t)" />
        <span>{{t.templateTitle}}</span>
      </div>
    </div>
    <div class="template-list-pagination">
      <el-pagination
        small
        layout="prev, pager, next"
        :current-page.sync="query.pageNum"
        :page-size.sync="query.pageSize"
        :total="total"
        @size-change="getTemplateList"
        @current-change="getTemplateList">
      </el-pagination>
    </div>
    <el-button
      type="primary"
      slot="reference"
      plain
      icon="el-icon-plus"
      size="mini"
    >生成报告</el-button>
  </el-popover>
</template>

<script>
import {listReportTemplate} from "@/api/system/ReportTemplate";
import {addReport} from "@/api/system/report";

export default {
  name: "Cat2BugReportTemplateSelect",
  data() {
    return {
      templateList: [],
      total: 0,
      query: {
        projectId: this.projectId,
        pageNum: 1,
        pageSize: 9
      }
    }
  },
  props: {
    projectId: {
      type: Number,
      default: null
    }
  },
  computed: {
    iconUrl() {
      return function (template) {
        return process.env.VUE_APP_BASE_API + template.templateIconUrl;
      }
    },
  },
  mounted() {
    this.getTemplateList();
  },
  methods: {
    /** 跳转到报表操作 */
    handleReport() {
      this.$router.push({name:'ReportTemplate'})
    },
    getTemplateList() {
      this.query.projectId = this.projectId;
      listReportTemplate(this.query).then(res=>{
        this.templateList = res.rows;
        this.total = res.total;
      })
    },
    createReportHandle(template) {
      let params = {
        reportTitle: template.templateTitle,
        reportDescription: template.templateContent,
        projectId: this.projectId,
      }
      addReport(params).then(res=>{
        this.$emit('create', params);
      });
    },
  }
}
</script>

<style lang="scss" scoped>
.template-header {
  display: inline-flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  span {
    font-size: 16px;
  }
  .el-link {
    color: #1890ff;
  }
}
.template-list {
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 5px;
  padding-top: 10px;
  .template-list-item:hover {
    cursor: pointer;
    box-shadow: 0 3px 5px rgba(0, 0, 0, 0.1);
  }
  .template-list-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 5px;
    border-radius: 5px;
    border: 1px solid #E4E7ED;
    .el-image {
      width: 180px;
      height: 120px;
    }
    span {
      width: 180px;
      display: -webkit-box;
      -webkit-line-clamp: 1;
      -webkit-box-orient: vertical;
      text-align: center;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: normal;
      padding-top: 10px;
    }
  }
}
.template-list-pagination {
  display: inline-flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  width: 100%;
}
</style>
