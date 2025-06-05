<template>
  <div>
    <el-dialog :title="$t('mk.add-image')" :visible.sync="dialogFormVisible" append-to-body width="510px">
      <el-form :model="form">
        <el-form-item :label="$t('mk.image-describe')" label-width="120">
          <el-input v-model="form.describe" autocomplete="off" maxlength="255"></el-input>
        </el-form-item>
        <el-form-item :label="$t('mk.select-image')" label-width="120">
          <image-upload v-model="form.urls" :limit="9" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">{{$t('cancel')}}</el-button>
        <el-button type="primary" @click="toolHandle">{{$t('ok')}}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import ImageUpload from "@/components/ImageUpload";

export default {
  name: "UploadImageTool",
  components: {ImageUpload},
  data() {
    return {
      dialogFormVisible: false,
      view: null,
      form: {
        url: null,
        describe: null
      }
    }
  },
  methods: {
    run(view,tools,tool) {
      this.dialogFormVisible = true;
      this.view = view;
    },
    toolHandle() {
      const desc = this.form.describe?this.form.describe:'';
      const urls = this.form.urls?this.form.urls.split(','):[''];
      let text = '';
      urls.forEach(url=>{
        let baseUrl = location.protocol + '//' + location.hostname + (location.port ? ':' + location.port : '');
        text += `\n![${desc}](${baseUrl + process.env.VUE_APP_BASE_API + url})\n`;
      })
      this.view.insertText(text);
      this.dialogFormVisible = false;
    }
  }
}
</script>

<style scoped>

</style>
