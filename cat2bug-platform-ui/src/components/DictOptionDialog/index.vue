<template>
  <el-dialog
    :title="title"
    :visible.sync="visible"
    width="500"
    :close-on-click-modal="false"
    :before-close="close">
    <div>
      <el-table :data="dictItems">
        <el-table-column
          align="center"
          prop="dictSort"
          label="排序"
          width="50">
        </el-table-column>
        <el-table-column
          prop="dictLabel"
          label="名称">
          <template slot-scope="scope">
            <el-input v-model="scope.row.dictLabel" />
          </template>
        </el-table-column>
        <el-table-column
          prop="dictValue"
          label="值">
          <template slot-scope="scope">
            <el-input v-model="scope.row.dictValue" />
          </template>
        </el-table-column>
        <el-table-column
          align="center"
          prop="cssClass"
          label="颜色"
          width="80">
          <template slot-scope="scope">
            <el-color-picker v-model="scope.row.cssClass"></el-color-picker>
          </template>
        </el-table-column>
        <el-table-column
          align="center"
          prop="isDefault"
          label="默认选项"
          width="80">
          <template slot-scope="scope">
            <el-radio v-model="isDefaultId" :label="scope.row.planItemId">{{ ' ' }}</el-radio>
          </template>
        </el-table-column>
        <el-table-column
          align="center"
          prop="status"
          label="禁用"
          width="80">
          <template slot-scope="scope">
            <el-switch
              v-model="scope.row.status"
              active-color="#13ce66"
              inactive-color="#ff4949">
            </el-switch>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <span slot="footer" class="dialog-footer">
      <el-button @click="handleAdd">新 增</el-button>
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="primary" @click="visible = false">确 定</el-button>
    </span>
  </el-dialog>
</template>

<script>
export default {
  name: "DictOptionDialog",
  data() {
    return {
      visible: false,
      dictItems: [],
      isDefaultId: null
    }
  },
  props: {
    title: {
      type: String,
      default: ''
    },
    dictType: {
      type: Array,
      default: []
    }
  },
  methods: {
    open() {
      this.dictItems = this.dictType.map(d=>d.raw);
      this.visible = true;
    },
    close(done) {
      if(done)
        done();
      this.visible = false;
    },
    handleAdd() {
      this.dictItems.push({
        dictLabel: null,
        dictValue: null,
        dictSort: this.dictItems.length,
        isDefault: false,
        status: 0,
        cssClass: '#606266',
      })
    }
  }
}
</script>

<style scoped>

</style>
