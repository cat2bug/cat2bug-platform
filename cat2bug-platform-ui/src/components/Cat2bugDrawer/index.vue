<template>
  <el-drawer
    :size="width"
    :visible.sync="drawer"
    :direction="direction">
    <template slot="title">
      <h1 class="title">{{title}}</h1>
      <div class="title-tools">
        <slot name="tools"></slot>
        <el-dropdown @command="windowSizeChangedHandle">
          <span class="el-dropdown-link">
            <svg-icon icon-class="link" />
          </span>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="30%">30%</el-dropdown-item>
            <el-dropdown-item command="50%">50%</el-dropdown-item>
            <el-dropdown-item command="80%">80%</el-dropdown-item>
            <el-dropdown-item command="100%">100%</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
        <el-dropdown @command="windowPositionChangedHandle">
          <span class="el-dropdown-link">
            <svg-icon icon-class="frame-top" />
          </span>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="ttb"><svg-icon icon-class="frame-top" class="marginRight5" />居上</el-dropdown-item>
            <el-dropdown-item command="rtl"><svg-icon icon-class="frame-right" class="marginRight5" />居右</el-dropdown-item>
            <el-dropdown-item command="btt"><svg-icon icon-class="frame-bottom" class="marginRight5" />居下</el-dropdown-item>
            <el-dropdown-item command="ltr"><svg-icon icon-class="frame-left" class="marginRight5" />居左</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
        <svg-icon icon-class="close" @click="closeHandle" />
      </div>
    </template>
    <slot name="content"></slot>
  </el-drawer>
</template>

<script>
export default {
  name: "Cat2bugDrawer",
  data() {
    return {
      drawer: false,
      direction:'ltr',
      width:'30%',
    }
  },
  props: {
    title: {
      type: String,
      default: null
    }
  },
  methods: {
    closeHandle() {
      this.drawer = false;
    },
    open() {
      this.drawer = true;
    },
    windowSizeChangedHandle(e){
      this.width = e;
    },
    windowPositionChangedHandle(e){
      this.direction = e;
    }
  }
}
</script>

<style lang="scss" scoped>
  .title {
    font-size: 26px;
    margin-top: 10px;
    margin-bottom: 10px;
    text-align: left;
  }
  ::v-deep .el-drawer__close-btn {
    display: none;
  }
  ::v-deep .el-drawer__header {
    padding-top: 10px;
    margin-bottom: 10px;
  }
  .title-tools {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    flex-direction: row;
    > * {
      cursor: pointer;
      margin-right: 5px;
      .svg-icon {
        margin-right: 5px;
      }
    }
  }
  .marginRight5 {
    margin-right: 5px;
  }
  ::v-deep .el-drawer__body {
    padding: 10px 15px;
  }
</style>
