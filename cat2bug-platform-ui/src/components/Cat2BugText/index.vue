<template>
  <div>
    <el-tooltip class="item" effect="dark" :placement="tooltipPosition">
      <div slot="content"><div v-html="text(tooltip)"></div></div>
      <el-link v-if="type=='link'" v-html="text(content)" @click="handleClick" type="primary" ></el-link>
      <div v-else-if="type=='text'" class="cat2bug-text-content" v-html="text(content)"></div>
    </el-tooltip>
  </div>
</template>

<script>
export default {
  name: "Cat2BugText",
  model: {
    prop: 'content',
    event: 'click'
  },
  props: {
    content: {
      type: String,
      default: null
    },
    tooltip: {
      type: String,
      default: null
    },
    tooltipPosition: {
      type: String,
      default: 'top-start'
    },
    type: {
      type: String,
      default: 'text'
    }
  },
  computed: {
    text: function () {
      return function (value) {
        return value.replace('\n', '<br />');
      }
    }
  },
  methods: {
    handleClick(e) {
      this.$emit('click');
      e.stopPropagation();
    }
  }
}
</script>

<style scoped>
.cat2bug-text-content {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
