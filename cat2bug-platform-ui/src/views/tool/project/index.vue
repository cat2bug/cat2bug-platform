<template>
  <div class="app-container">
    <router-link to="/tools/browser" target="_blank">
      <el-button icon="el-icon-eleme">{{$t('browser')}}</el-button>
    </router-link>

  </div>
</template>

<script>
export default {
  name: "ProjectTools",
  components: { },
  data() {
    return {
    };
  },
};
</script>
<style lang="scss" scoped>

</style>
