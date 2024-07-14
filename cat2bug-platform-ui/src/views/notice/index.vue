<template>
  <div class="app-container">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('notice')">
      </el-page-header>
    </el-row>
    <div class="tabs-tools-row">
      <el-tabs v-model="activeTabName" @tab-click="handleQuery">
        <el-tab-pane v-for="(group,index) in groupList" :key="index" :name="group.groupName">
          <span slot="label" class="row">
            <span>{{ tabName(group.groupName) }}</span>
            <span v-if="group.notReadCount>0" class="number">{{ group.notReadCount }}</span>
          </span>
        </el-tab-pane>
      </el-tabs>
      <div class="right-tools">
        <svg-icon class="tools-button" icon-class="add-tab" @click.native="addNoticeTabHandle" />
        <svg-icon class="tools-button" icon-class="config" @click="setNoticeOptionHandle" />
      </div>
    </div>
    <div class="notice-tools">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="" prop="noticeTitle">
          <el-input
            prefix-icon="el-icon-document"
            v-model="queryParams.noticeTitle"
            placeholder="请输入通知标题"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
  <!--      <el-form-item label="" prop="noticeType">-->
  <!--        <el-select v-model="queryParams.noticeType" placeholder="公告类型" clearable>-->
  <!--          <el-option-->
  <!--            v-for="dict in dict.type.sys_notice_type"-->
  <!--            :key="dict.value"-->
  <!--            :label="dict.label"-->
  <!--            :value="dict.value"-->
  <!--          />-->
  <!--        </el-select>-->
  <!--      </el-form-item>-->
  <!--      <el-form-item>-->
  <!--        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>-->
  <!--        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>-->
  <!--      </el-form-item>-->
      </el-form>

      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button
            type="danger"
            plain
            icon="el-icon-delete"
            size="mini"
            :disabled="multiple"
            @click="handleDelete($event,{})"
            v-hasPermi="['system:notice:remove']"
          >{{ $t('batch-delete') }}</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="danger"
            plain
            icon="el-icon-delete"
            size="mini"
            :disabled="multiple"
            @click="handleDelete($event,{})"
            v-hasPermi="['system:notice:send']"
          >{{ $t('notice.send') }}</el-button>
        </el-col>
      </el-row>
    </div>
    <el-table v-loading="loading" :data="noticeList" @row-click="handleNotice" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column :label="$t('create-time')" align="left" prop="createTime" width="170">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('notice.team')"
        prop="teamName"
        align="left" width="150"/>
      <el-table-column
        :label="$t('notice.project')"
        prop="projectName"
        align="left" width="300"/>
      <el-table-column
        :label="$t('notice.group')"
        align="left"
        :show-overflow-tooltip="true"
      >
        <template slot-scope="scope">
          <el-tag v-if="scope.row.isRead===false" size="mini" type="danger" effect="dark" class="span font-size9">NEW</el-tag>
          <span>{{scope.row.noticeTitle}}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('operate')" align="center" class-name="small-padding fixed-width" width="100">
        <template slot-scope="scope">
          <el-button
            class="red-button"
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete($event,scope.row)"
            v-hasPermi="['system:notice:remove']"
          >{{$t('delete')}}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
    <view-notice ref="viewNotice" @read="getList"></view-notice>
  </div>
</template>

<script>
import {delNotice, groupStatisticsNotice, listNotice} from "@/api/system/notice";
import ViewNotice from "@/components/Notice/ViewNotice";

export default {
  name: "Notice",
  components: { ViewNotice },
  dicts: ['sys_notice_status', 'sys_notice_type'],
  data () {
    return {
      groupList: [],
      activeNoticeName: '',
      activeTabName: 'all',
      noticeTabs:['', 'defect', 'report'],
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 公告表格数据
      noticeList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        groupName: '',
        noticeTitle: undefined,
        createBy: undefined,
        status: undefined,
        projectId: this.getProjectId(),
        receiverId: this.getUserId()
      },
    }
  },
  computed: {
    tabName: function () {
      return function (name) {
        return this.$i18n.t(name)+this.$i18n.t('char-span')+this.$i18n.t('notice');
      }
    }
  },
  created() {
    this.getGroupStatisticsNotice();
  },
  methods: {
    /** 获取项目id */
    getProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 获取用户id */
    getUserId() {
      return this.$store.state.user.id;
    },
    /** 设置通知配置 */
    setNoticeOptionHandle() {

    },
    /** 添加通知标签 */
    addNoticeTabHandle() {

    },
    /** 返回 */
    goBack() {
      this.$router.back();
    },
    getGroupStatisticsNotice() {
      this.loading = true;
      groupStatisticsNotice().then(res=>{
        let all = {
          groupName: 'all',
          notReadCount: 0,
          readCount: 0
        }
        res.rows.forEach(g=>{
          all.notReadCount += g.notReadCount;
          all.readCount += g.readCount;
        })
        this.groupList = [...[all],...res.rows];
        this.handleQuery();
      })
    },
    /** 查询公告列表 */
    getList() {
      this.loading = true;
      listNotice(this.queryParams).then(response => {
        this.noticeList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.queryParams.groupName = this.activeTabName=='all'?null:this.activeTabName;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 删除按钮操作 */
    handleDelete(event,row) {
      const noticeIds = row.noticeId || this.ids
      this.$modal.confirm(this.$i18n.t('notice.is-delete')).then(function() {
        return delNotice(noticeIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess(this.$i18n.t('delete.success'));
      }).catch(() => {});
      event.stopPropagation();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.noticeId)
      this.single = selection.length!=1
      this.multiple = !selection.length
    },
    handleNotice(notice) {
      this.$refs.viewNotice.open(notice.noticeId);
    }
  }
}
</script>

<style lang="scss" scoped>
.font-size9 {
  font-size: 9px;
}
.span {
  margin-right: 5px;
}
.row {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
}
.number {
  background-color: #f56c6c;
  color: #FFFFFF;
  font-size: 0.7rem;
  height: 16px;
  line-height: 10px;
  padding: 3px 7px;
  margin: 0 3px;
  border-radius: 10px;
}
.notice-tools {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  > * {
    display: inline-block;
    justify-content: flex-start;
    margin-bottom: 0px;
    ::v-deep .el-form-item {
      margin-bottom: 0px;
    }
  }
}
</style>
