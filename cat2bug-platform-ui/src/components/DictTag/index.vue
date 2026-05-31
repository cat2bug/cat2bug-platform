<template>
  <div>
    <template v-for="(item, index) in options">
      <template v-if="values.includes(item.value)">
        <span
          v-if="(item.raw.listClass == 'default' || item.raw.listClass == '') && (item.raw.cssClass == '' || item.raw.cssClass == null)"
          :key="item.value"
          :index="index"
          :class="['dict-status-text', 'dict-status-' + item.value]"
          >{{ item.label + " " }}</span
        >
        <el-tag
          v-else
          :disable-transitions="true"
          :key="item.value"
          :index="index"
          :type="item.raw.listClass == 'primary' ? '' : item.raw.listClass"
          :effect="tagEffect"
          :class="['dict-status-tag', item.raw.cssClass, 'dict-status-' + item.value]"
        >
          {{ $te(item.value) ? $t(item.value):item.label + " " }}
        </el-tag>
      </template>
    </template>
    <template v-if="unmatch && showValue">
      {{ unmatchArray | handleArray }}
    </template>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';

export default {
  name: "DictTag",
  props: {
    options: {
      type: Array,
      default: null,
    },
    value: [Number, String, Array],
    // 当未找到匹配的数据时，显示value
    showValue: {
      type: Boolean,
      default: true,
    }
  },
  data() {
    return {
      unmatchArray: [], // 记录未匹配的项
    }
  },
  computed: {
    ...mapGetters(['themeMode']),
    tagEffect() {
      return this.themeMode === 'dark' ? 'plain' : 'light';
    },
    values() {
      if (this.value !== null && typeof this.value !== "undefined") {
        return Array.isArray(this.value) ? this.value : [String(this.value)];
      } else {
        return [];
      }
    },
    unmatch() {
      this.unmatchArray = [];
      if (this.value !== null && typeof this.value !== "undefined") {
        // 传入值为非数组
        if (!Array.isArray(this.value)) {
          if (this.options.some((v) => v.value == this.value)) return false;
          this.unmatchArray.push(this.value);
          return true;
        }
        // 传入值为Array
        this.value.forEach((item) => {
          if (!this.options.some((v) => v.value == item))
            this.unmatchArray.push(item);
        });
        return true;
      }
      // 没有value不显示
      return false;
    },
  },
  filters: {
    handleArray(array) {
      if (array.length === 0) return "";
      return array.reduce((pre, cur) => {
        return pre + " " + cur;
      })
    }
  }
};
</script>
<style scoped>
.el-tag + .el-tag {
  margin-left: 10px;
}
</style>
