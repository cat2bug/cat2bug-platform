<template>
  <el-drawer
    size="90%"
    v-loading="loading"
    :visible.sync="visible"
    direction="rtl"
    :before-close="closeReportDrawer">
    <template slot="title">
      <div class="report-edit-header">
        <div class="report-edit-title">
          <i class="el-icon-arrow-left" @click="cancel"></i>
          <h4 class="report-edit-title-num">{{report.reportTitle}}</h4>
        </div>
        <div class="report-edit-tools">
          <report-tools :report="report" @delete="cancel" />
          <el-dropdown @command="handleExport">
            <el-button size="mini">
              <i class="el-icon-download"></i>
            </el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="word"><svg-icon icon-class="docx" />Word 导出</el-dropdown-item>
              <el-dropdown-item command="pdf"><svg-icon icon-class="pdf" />PDF 导出</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </div>
    </template>
    <div ref="report-data" id="report-data" class="app-container" v-loading="loading">
      <markdown-it-vue class="md" :content="report.reportDescription+''" />
    </div>
  </el-drawer>
</template>

<script>
import MarkdownItVue from "markdown-it-vue"
import 'markdown-it-vue/dist/markdown-it-vue.css'
import FileSaver from "file-saver";
import htmlDocx from "html-docx-js/dist/html-docx";
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import {getReport} from "@/api/system/report";
import ReportTools from "./ReportTools/index"
require('@/assets/font/simsun-normal');

export default {
  name: "ViewReport",
  components: { MarkdownItVue,ReportTools },
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
    /** 获取指定id下的所有canvas图片 */
    convertCanvasesToImages(containerId) {
      let container = document.getElementById(containerId);
      let canvases = container.getElementsByTagName('canvas');
      let images = [];
      for (let i=0;i<canvases.length;i++) {
        let canvas = canvases[i];
        let image = document.createElement('img');
        image.src = canvas.toDataURL("image/png");
        image.setAttribute('width', canvas.style.width?parseInt(canvas.style.width)+'':'500');
        image.setAttribute('height', canvas.style.height?parseInt(canvas.style.height)+'':'200');
        // canvas.parentNode.replaceChild(image, canvas);
        canvas.insertAdjacentElement('afterend', image);
        images.push(image);
      }
      return images;
    },
    /** 导出PDF */
    exportPDF() {
      this.loading = true;
      const content = this.$refs['report-data'];
      html2canvas(content).then(canvas => {
        const imgData = canvas.toDataURL('image/png');
        const pdf = new jsPDF({
          orientation: 'portrait',
          unit: 'px',
          format: 'a4'
        });
        const imgProps= pdf.getImageProperties(imgData);
        const pdfWidth = pdf.internal.pageSize.getWidth();
        pdf.setFont("simsun");
        const pdfHeight = (imgProps.height * pdfWidth) / imgProps.width;
        pdf.addImage(imgData, 'PNG', 0, 0, pdfWidth, pdfHeight);
        pdf.save(this.report.reportTitle+'.pdf');
        this.loading = false;
      });
    },
    setHide(ele) {
      ele.setAttribute('cat2bug-temp-display', ele.style.display?ele.style.display:'inline-flex');
      ele.style.display = 'none';
    },
    removeHide(ele) {
      if(ele.getAttribute('cat2bug-temp-display')) {
        ele.style.display = ele.getAttribute('cat2bug-temp-display');
      }
    },
    /** 根据样式类查找元素并转为图片 */
    async classToImages(className) {
      const contents = document.getElementsByClassName(className);
      let images = [];
      for(let i=0;i<contents.length;i++) {
        await html2canvas(contents[i]).then(canvas=>{
          let image = this.canvasToImage(canvas);
          contents[i].classList.add('print-hide')
          this.setHide(contents[i]);
          contents[i].insertAdjacentElement('afterend', image);
          images.push(image);
        });
      }
      return images;
    },
    /** 移除元素的指定样式 */
    removeClass(className) {
      const contents = document.getElementsByClassName(className);
      while (contents.length>0) {
        this.removeHide(contents[0]);
        contents[0].classList.remove(className);
      }
    },
    /** 画板转图片 */
    canvasToImage(canvas) {
      let image = document.createElement('img');
      image.src = canvas.toDataURL("image/png");
      image.setAttribute('width', canvas.style.width?parseInt(canvas.style.width)+'':'500');
      image.setAttribute('height', canvas.style.height?parseInt(canvas.style.height)+'':'200');
      canvas.insertAdjacentElement('afterend', image);
      return image;
    },
    /** 导出Word */
    async exportWord() {
      this.loading = true;
      let id = "report-data";
      // 将canvas转为图片
      let tempImages = this.convertCanvasesToImages(id);
      // 根据样式获取元素并转为图片
      const classes = ['markdown-it-vue-alter'];
      for(let i=0;i<classes.length;i++){
        let imgs = await this.classToImages(classes[i]);
        tempImages = [...tempImages,...imgs];
      }
      // 整合为一个导出页面
      let contentHtml = document.getElementById(id).innerHTML;
      let content = `
    	 	<!DOCTYPE html><html>
            	<head>
	                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	            </head>
	            <body>
	                <div>
                  		${contentHtml}
                	</div>
	            </body>
            </html>`;
      // 导出文件
      let converted = htmlDocx.asBlob(content);
      FileSaver.saveAs(converted, this.report.reportTitle + ".docx");
      // 移除临时图片
      for(let i=0;i<tempImages.length;i++){
        tempImages[i].remove();
      }
      // 移除因导出打印而设置为print-hide的元素样式
      this.removeClass('print-hide');
      this.loading = false;
    },
    /** 获取报告信息 */
    getReportInfo(reportId) {
      this.loading = true;
      getReport(reportId).then(res=>{
        this.loading = false;
        this.report = res.data;
      }).catch(e=>{
        this.loading = false;
      });
    },
    /** 打开操作 */
    open(reportId) {
      this.visible = true;
      this.reset();
      this.reportId = reportId;
      this.getReportInfo(reportId);
      this.$nextTick(()=>{

      });
    },
    /** 取消返回 */
    cancel() {
      this.visible = false;
      this.reset();
    },
    /** 表单重置 */
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
    /** 导出 */
    handleExport(command) {
      switch (command) {
        case 'word':
          this.exportWord();
          break;
        case 'pdf':
          this.exportPDF();
          break;
      }
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
  .report-edit-header {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-direction: row;
    flex-wrap: wrap;
    .report-edit-tools {
      padding: 5px 0px;
      display: inline-flex;
      gap: 10px;
    }
    .report-edit-title {
      display: inline-block;
      display: inline-flex;
      justify-content: flex-start;
      align-items: center;
      flex-direction: row;
      overflow: hidden;
      flex: 1;
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
      .report-edit-title-name {
        flex: 1;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
      }
      .report-edit-title-num, .report-edit-title-name {
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
  ::v-deep .el-dropdown-menu__item {
    > * {
      margin-right: 5px;
    }
  }
</style>
