<template>
  <div class="top-right-btn" :style="style">
    <el-row>
      <el-tooltip class="item" effect="dark" :content="showSearch ? $t('hide-search') : $t('show-search')" placement="top" v-if="search">
        <el-button size="small" circle @click="toggleSearch()">
          <el-icon><Search /></el-icon>
        </el-button>
      </el-tooltip>
      <el-tooltip class="item" effect="dark" :content="$t('refresh')" placement="top">
        <el-button size="small" circle @click="refresh()">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </el-tooltip>
      <el-tooltip class="item" effect="dark" :content="$t('display-columns')" placement="top" v-if="columns">
        <el-button size="small" circle @click="showColumn()">
          <el-icon><Menu /></el-icon>
        </el-button>
      </el-tooltip>
    </el-row>
    <el-dialog :title="title" v-model="open" append-to-body>
      <el-transfer
        :titles="[$t('show'), $t('hide')]"
        v-model="value"
        :data="columns"
        @change="dataChange"
      ></el-transfer>
    </el-dialog>
  </div>
</template>
<script>
import { Menu, Refresh, Search } from '@element-plus/icons-vue'

export default {
  name: "RightToolbar",
  components: { Menu, Refresh, Search },
  data() {
    return {
      value: [],
      title: this.$t('show') + '/' + this.$t('hide'),
      open: false,
    };
  },
  props: {
    showSearch: {
      type: Boolean,
      default: true,
    },
    columns: {
      type: Array,
    },
    search: {
      type: Boolean,
      default: true,
    },
    gutter: {
      type: Number,
      default: 10,
    },
  },
  computed: {
    style() {
      const ret = {};
      if (this.gutter) {
        ret.marginRight = `${this.gutter / 2}px`;
      }
      return ret;
    }
  },
  created() {
    for (let item in this.columns) {
      if (this.columns[item].visible === false) {
        this.value.push(parseInt(item));
      }
    }
  },
  methods: {
    toggleSearch() {
      this.$emit("update:showSearch", !this.showSearch);
    },
    refresh() {
      this.$emit("queryTable");
    },
    dataChange(data) {
      for (let item in this.columns) {
        const key = this.columns[item].key;
        this.columns[item].visible = !data.includes(key);
      }
    },
    showColumn() {
      this.open = true;
    },
  },
};
</script>
<style lang="scss" scoped>
:deep(.el-transfer__button) {
  border-radius: 50%;
  padding: 12px;
  display: block;
  margin-left: 0px;
}
:deep(.el-transfer__button:first-child) {
  margin-bottom: 10px;
}
</style>
