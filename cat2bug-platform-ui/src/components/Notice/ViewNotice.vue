<template>
  <el-drawer
    size="35%"
    :visible.sync="visible"
    direction="rtl"
    :before-close="closeDefectDrawer">
    <template slot="title">
      <div class="notice-header">
        <div class="notice-header-title">
          <i class="el-icon-arrow-left" @click="cancel"></i>
          <h4 class="notice-header-title-name">{{notice.noticeTitle}}</h4>
        </div>
      </div>
    </template>
    <div class="app-container notice-body" v-loading="loading">
      <el-collapse v-model="activeNames">
        <el-collapse-item :title="$i18n.t('base-info')" name="base">
          <el-row class="notice-header-body-base" :gutter="20">
            <el-col :span="12">
              <label>{{$t('notice.team')}}:</label>
              <span>{{notice.teamName}}</span>
            </el-col>
            <el-col :span="12">
              <label>{{$t('notice.project')}}:</label>
              <span>{{notice.projectName}}</span>
            </el-col>
            <el-col :span="12">
              <label>{{$t('notice.group')}}:</label>
              <span>{{notice.groupName}}</span>
            </el-col>
            <el-col :span="12">
              <label>{{$t('create-time')}}:</label>
              <span>{{notice.createTime}}</span>
            </el-col>
          </el-row>
        </el-collapse-item>
        <el-collapse-item :title="$i18n.t('notice.content')" name="content">
          <markdown-it-vue :content="notice.noticeContent+''" />
        </el-collapse-item>
      </el-collapse>
      <div slot="footer" class="dialog-footer"></div>
    </div>
  </el-drawer>
</template>

<script>
import {getNotice} from "@/api/system/notice";
import MarkdownItVue from "markdown-it-vue"

export default {
  name: "ViewNotice",
  components: { MarkdownItVue },
  data() {
    return {
      activeNames: ['base','content'],
      loading: false,
      // 显示窗口
      visible: false,
      // 缺陷对象
      notice:{},
    }
  },
  methods:{
    /** 初始化浮动菜单 */
    initFloatMenu() {
      this.$floatMenu.windowsInit(document.querySelector('.main-container'));
      this.$floatMenu.resetMenus([{
        id: 'closeViewNotice',
        name: 'close',
        visible: true,
        plain: true,
        type: '',
        icon: 'close',
        prompt: 'close',
        click : this.cancel
      }]);
    },
    // 获取缺陷信息
    getNoticeInfo(noticeId) {
      this.loading = true;
      getNotice(noticeId).then(res=>{
        this.loading = false;
        this.notice = res.data;
        this.$emit('read',this.notice);
      }).catch(e=>{
        this.loading = false;
      });
    },
    // 打开操作
    open(noticeId) {
      this.visible = true;
      // this.reset();
      this.getNoticeInfo(noticeId);
      this.initFloatMenu();
    },
    // 取消按钮
    cancel() {
      this.$emit('close');
      this.visible = false;
      // this.reset();
    },
    /** 关闭缺陷抽屉窗口 */
    closeDefectDrawer(done) {
      done();
      this.cancel();
    },
    handleByChangeHandle(members) {
      console.log(members, this.form.handleBy);
    },
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-drawer {
  border-left: 3px solid #ff4949;
  .el-drawer__header {
    margin-bottom: 0px;
  }
}
.notice-header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-direction: row;
  flex-wrap: wrap;
  .notice-header-title {
    display: inline-block;
    display: inline-flex;
    justify-content: flex-start;
    align-items: center;
    flex-direction: row;
    overflow: hidden;
    > * {
      float:left;
    }
    .el-icon-arrow-left {
      font-size: 22px;
    }
    .el-icon-arrow-left:hover {
      cursor: pointer;
      color: #909399;
    }
    .notice-header-title-name {
      flex: 1;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }
    .notice-header-title-num, .notice-header-title-name {
      font-size: 20px;
      color: #303133;
      font-weight: 500;
      margin-top: 10px;
      margin-bottom: 10px;
    }
    > * {
      margin-right: 10px;
    }
  }
}
.notice-body {
  padding-left: 30px;
  padding-right: 30px;
  ::v-deep .el-collapse {
    .el-collapse-item__header {
      font-size: 16px;
    }
    border-width: 0px;
    .el-collapse-item:last-child {
      .el-collapse-item__wrap {
        border-width: 0px;
      }
    }
  }
  h5 {
    font-size: 18px;
    color: #303133;
    margin: 10px 0px;
  }
  .notice-header-body-base {
    .el-col {
      margin-bottom: 20px;
      label {
        font-size: 14px;
        color: #303133;
        width: 120px;
        display: inline-block;
        justify-content: flex-start;
        margin-right: 10px;
        text-align: right;
      }
    }
  }
}
</style>
