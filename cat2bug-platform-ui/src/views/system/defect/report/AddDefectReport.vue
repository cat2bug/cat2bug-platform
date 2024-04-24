<template>
  <div class="app-container">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('defect.statistic-template')">
      </el-page-header>
    </el-row>
    <cat2-bug-markdown />
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

export default {
  name: "AddDefectReport",
  components: {Cat2BugMarkdown, mavonEditor},
  data() {
    return {
      content: '',
      html: '',
    }
  },
  methods: {
    /** 返回 */
    goBack() {
      this.$router.back();
    },
    change(value, render) {
      // render 为 markdown 解析后的结果
      this.html = render;
    },
  }
}
</script>

<style scoped>
.app-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  position: absolute;
  top: 0px;
  bottom: 0px;
}
</style>
