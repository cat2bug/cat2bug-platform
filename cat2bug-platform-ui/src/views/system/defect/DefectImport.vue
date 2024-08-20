<template>
  <!-- 用户导入对话框 -->
  <el-dialog :title="upload.title" :visible.sync="upload.open" width="400px" append-to-body>
    <el-upload
      ref="upload"
      :limit="1"
      accept=".xlsx, .xls"
      :headers="upload.headers"
      :action="upload.url + '?updateSupport=' + upload.updateSupport"
      :disabled="upload.isUploading"
      :on-progress="handleFileUploadProgress"
      :on-success="handleFileSuccess"
      :auto-upload="false"
      drag
    >
      <i class="el-icon-upload"></i>
      <div class="el-upload__text">{{ $t('defect.import-prompt') }}<em>{{$t('char-span')}} {{ $t('click.upload') }}</em></div>
      <div class="el-upload__tip text-center" slot="tip">
        <span>{{ strFormat($t('defect.import-file-format'), 'xls、xlsx') }}</span>
        <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;" @click="importTemplate">{{$t('download.template')}}</el-link>
      </div>
    </el-upload>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="submitFileForm">{{ $t('ok') }}</el-button>
      <el-button @click="upload.open = false">{{ $t('cancel') }}</el-button>
    </div>
  </el-dialog>
</template>

<script>
import {getToken} from "@/utils/auth";
import {strFormat} from "@/utils";

export default {
  name: "DefectImport",
  data() {
    return {
      // 缺陷导入参数
      upload: {
        // 是否显示弹出层（用户导入）
        open: false,
        // 弹出层标题（用户导入）
        title: "",
        // 是否禁用上传
        isUploading: false,
        // 是否更新已经存在的用户数据
        updateSupport: 0,
        // 设置上传的请求头部
        headers: { Authorization: "Bearer " + getToken() },
        // 上传的地址
        url: process.env.VUE_APP_BASE_API + "/system/defect/import"
      },
    }
  },
  props: {
    projectId: {
      type: Number,
      default: null
    }
  },
  methods: {
    strFormat,
    /** 导入按钮操作 */
    open() {
      this.upload.title = "缺陷导入";
      this.upload.open = true;
    },
    /** 下载模板操作 */
    importTemplate() {
      this.download('system/defect/importTemplate', {
      }, `defect_template_${new Date().getTime()}.xlsx`)
    },
    // 文件上传中处理
    handleFileUploadProgress(event, file, fileList) {
      this.upload.isUploading = true;
    },
    // 文件上传成功处理
    handleFileSuccess(response, file, fileList) {
      this.upload.open = false;
      this.upload.isUploading = false;
      this.$refs.upload.clearFiles();
      this.$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", this.$i18n.t('defect.import-result').toString(), { dangerouslyUseHTMLString: true });
      this.$emit("upload",response);
    },
    // 提交上传文件
    submitFileForm() {
      this.$refs.upload.submit();
    }
  }
}
</script>

<style scoped>

</style>
