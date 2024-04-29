<template>
  <div class="app-container">
    <el-row>
      <el-page-header @back="goBack" :content="$t('report.template')">
      </el-page-header>
    </el-row>
    <div class="row">
      <div class="template" v-for="(t, index) in templateList" :key="index" @click="goReportTemplateView(t.templateId)">
        <el-image fit="contain" :src="iconUrl(t)" />
        <span>{{ t.templateTitle }}</span>
      </div>
      <div class="add" @click="addReportTemplate">
        <svg-icon icon-class="add-statistic"/>
        <span>添加空白模版</span>
      </div>
    </div>
  </div>
</template>

<script>

import {addReportTemplate, listReportTemplate} from "@/api/system/ReportTemplate";

export default {
  name: "ReportTemplate",
  data() {
    return {
      templateList: [],
      template: {
        templateTitle: '新建报告',
        moduleType: null,
        projectId: null,
        templateContent: '',
        majorVersion: 1,
        minorVersion: 0,
      },
    }
  },
  computed: {
    iconUrl() {
      return function (template) {
        return process.env.VUE_APP_BASE_API + template.templateIconUrl;
      }
    },
    projectId() {
      return this.$store.state.user.config.currentProjectId
    },
  },
  created() {
    this.getTemplateList();
  },
  methods: {
    /** 返回 */
    goBack() {
      this.$router.back();
    },
    addReportTemplate() {
      this.template.projectId = this.projectId;
      addReportTemplate(this.template).then(res=>{
        if(res.data) {
          this.goReportTemplateView(res.data.templateId);
        } else {
          this.$message.error('创建模版失败');
        }
      });
    },
    goReportTemplateView(templateId) {
      this.$router.push({name:'AddDefectReport', query:{templateId: templateId}});
    },
    getTemplateList() {
      let query = {
        pageNum: 1,
        pageSize: 9999
      }
      listReportTemplate(query).then(res=>{
        this.templateList = res.rows;
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  position: absolute;
  top: 0px;
  bottom: 0px;
  .el-page-header {
    margin-bottom: 10px;
  }
}
.row {
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 10px;
  overflow-y: auto;
  padding: 2px;
}
.add {
  border: 2px dashed #E4E7ED;
  .svg-icon {
    color: #C0C4CC;
    width: 300px;
    height: 300px;
    padding: 20px;
    border-radius: 5px;
    background-color: #F5F7FA;
  }
}
.template {
  .el-image {
    width: 100%;
    border-radius: 5px;
  }
  span {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: normal;
  }
}
.add, .template {
  width: 400px;
  height: 360px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border: 1px solid #E4E7ED;
  border-radius: 5px;
  padding: 15px;
  span {
    margin-top: 10px;
  }
}
.add:hover, .template:hover {
  transform: scale(1.01, 1.01);
  box-shadow: 0 3px 5px rgba(0, 0, 0, 0.1);
  cursor: pointer;
}
</style>
