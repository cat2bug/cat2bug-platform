<template>
  <div>
    <el-dialog :title="$t('mk.add-link')" :visible.sync="dialogFormVisible" width="500px">
      <el-form :model="form">
        <el-form-item :label="$t('mk.link-describe')" label-width="120">
          <el-input v-model="form.describe" autocomplete="off" maxlength="255"></el-input>
        </el-form-item>
        <el-form-item :label="$t('mk.link-url')" label-width="120">
          <el-input v-model="form.url" autocomplete="off" maxlength="255"></el-input>
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
export default {
  name: "LinkTool",
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
      const url = this.form.url?this.form.url:'';
      this.view.insertText(`[${desc}](${url})`);
      this.dialogFormVisible = false;
    }
  }
}

</script>

<style scoped>

</style>
