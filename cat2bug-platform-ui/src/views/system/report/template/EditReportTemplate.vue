<template>
  <div class="app-container">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('defect.statistic-template')">
        <template v-slot:content>
          <el-input type="text" v-model="template.templateTitle" class="report-name" size="small" maxlength="255" @input="changeHandle(500)"></el-input>
          <div class="time" v-if="template.updateTime">
            <svg-icon icon-class="md-create-time" />
            <span>{{$t('update-time')}}:{{template.updateTime}}</span>
          </div>
        </template>
      </el-page-header>
    </el-row>
    <cat2-bug-markdown ref="cat2bugMarkdown" v-model="template.templateContent" :placeholder="$t('mk.start-edit').toString()" :template="template" @input="changeHandle(5000)" />
<!--    <mavon-editor-->
<!--      v-model="content"-->
<!--      ref="md"-->
<!--      @change="change"-->
<!--      style="min-height: 200px"-->
<!--    />-->
  </div>
</template>

<script>
import { mavonEditor } from 'mavon-editor';
import 'mavon-editor/dist/css/index.css';
import Cat2BugMarkdown from "@/components/Cat2BugMarkdown";
import {getReportTemplate, updateTemplate} from "@/api/system/ReportTemplate";
import {upload} from "@/api/common/upload";
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts';
import pageFormClose from '@/mixins/page-form-close';

export default {
  name: "AddDefectReport",
  mixins: [dialogFormShortcuts, pageFormClose],
  components: {Cat2BugMarkdown, mavonEditor},
  data() {
    return {
      visible: true,
      template: {
        templateTitle: '',
        moduleType: null,
        projectId: null,
        templateContent: '',
        majorVersion: 1,
        minorVersion: 0,
      },
      pushId: null,
      content: '',
      html: '',
    }
  },
  created() {
    const id = this.$route.query.templateId;
    if(id) {
      this.getReportTemplate(id);
    } else {
      this.$nextTick(() => this.capturePageFormCloseBaseline());
    }
  },
  mounted() {
    this.$nextTick(() => this.focusMarkdownEditor());
  },
  methods: {
    focusMarkdownEditor() {
      const md = this.$refs.cat2bugMarkdown;
      if (md && typeof md.focusEditor === 'function') {
        md.focusEditor();
      }
    },
    serializePageFormCloseState() {
      return JSON.stringify({
        templateTitle: this.template.templateTitle || '',
        templateContent: this.template.templateContent || '',
        templateIconUrl: this.template.templateIconUrl || ''
      });
    },
    onPageFormShortcutClose() {
      this.closeReportTemplatePage();
    },
    async closeReportTemplatePage() {
      if (this._closingTemplatePage) return;
      this._closingTemplatePage = true;
      try {
        if (this.pushId) {
          clearTimeout(this.pushId);
          this.pushId = null;
        }
        if (this.isPageFormCloseDirty()) {
          await this.saveTemplateNow();
        }
        this.pageFormCloseBaseline = null;
        this.$router.back();
      } catch (e) {
        this._closingTemplatePage = false;
      }
    },
    async saveTemplateNow() {
      const md = this.$refs.cat2bugMarkdown;
      if (md && typeof md.getMarkdownImage === 'function') {
        const blob = await md.getMarkdownImage();
        if (blob && blob.type === 'image/png') {
          const formData = new FormData();
          formData.append('file', blob);
          const uploadRes = await upload(formData);
          this.template.templateIconUrl = uploadRes.fileName;
        }
      }
      const res = await updateTemplate(this.template);
      this.template = res.data;
      await this.$nextTick();
      this.capturePageFormCloseBaseline();
      return res;
    },
    getReportTemplate(id) {
      getReportTemplate(id).then(res=>{
        this.template = res.data;
        this.$nextTick(() => {
          this.capturePageFormCloseBaseline();
          this.focusMarkdownEditor();
        });
      });
    },
    /** 返回 */
    goBack() {
      this.onPageFormShortcutClose();
    },
    change(value, render) {
      // render 为 markdown 解析后的结果
      this.html = render;
    },
    changeHandle(time) {
      if (!this.pushId) {
        const delay = time ? time : 1000;
        this.pushId = setTimeout(() => {
          this.pushId = null;
          this.saveTemplateNow().catch(() => {});
        }, delay);
      }
    },
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-page-header {
  display: inline-flex;
  flex-direction: row;
  width: 100%;
  .el-page-header__content {
    flex: 1;
    display: inline-flex;
    flex-direction: row;
    .el-input {
      flex: 1;
    }
  }
}
.app-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  position: absolute;
  top: 0px;
  bottom: 0px;
}
::v-deep .report-name > .el-input__inner {
  border: none;
  border-radius: 0px;
  font-size: 16px;
  padding: 0px;
  height: 18px;
  line-height: 18px;
}
.time {
  font-size: 12px;
  border-radius: 12px;
  padding: 0px 10px;
  background-color: #EBEEF5;
  margin-left: 15px;
  gap: 5px;
  display: inline-flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
}
</style>
