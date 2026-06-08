<template>
  <div class="app-container notice-page" ref="noticeMain">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('notice')">
      </el-page-header>
    </el-row>
    <div class="tabs-tools-row">
      <el-tabs v-model="activeTabName" class="notice-hint-tabs" @tab-click="handleQuery">
        <el-tab-pane v-for="(group,index) in groupList" :key="index" :name="group.groupName">
          <span slot="label" class="row notice-tab-label">
            <span>{{ tabName(group.groupName) }}</span>
            <span v-if="group.notReadCount>0" class="number">{{ group.notReadCount }}</span>
          </span>
        </el-tab-pane>
      </el-tabs>
      <div class="right-tools">
<!--        <svg-icon class="tools-button" icon-class="add-tab" @click.native="addNoticeTabHandle" />-->
        <svg-icon class="tools-button notice-hint-config" icon-class="config" @click="setNoticeOptionHandle" />
      </div>
    </div>
    <div class="notice-tools">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px" :class="{ 'list-query-keyboard-nav': listQueryNavActive }">
        <el-form-item label="" prop="noticeTitle" class="notice-hint-query list-query-nav-item" data-query-key="noticeTitle">
          <el-input
            prefix-icon="el-icon-document"
            v-model="queryParams.noticeTitle"
            :placeholder="$t('notice.please-enter-title')"
            clearable
            @input="handleQuery"
          />
        </el-form-item>
      </el-form>

      <el-row ref="noticeToolsRight" :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button
            type="danger"
            plain
            icon="el-icon-delete"
            size="mini"
            :disabled="multiple"
            @click="handleDelete($event,{})"
            v-hasPermi="['notice:remove']"
          >{{ $t('batch-delete') }}</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            class="notice-hint-send"
            type="primary"
            plain
            icon="el-icon-s-promotion"
            size="mini"
            @click="handleSendNotice"
            v-hasPermi="['notice:send']"
          >{{ $t('notice.send') }}</el-button>
        </el-col>
      </el-row>
    </div>
    <el-table
      ref="noticeTable"
      class="notice-list-table"
      v-loading="loading"
      :data="noticeList"
      @row-click="handleNotice"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column :label="$t('create-time')" align="left" prop="createTime" width="170">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('notice.team')"
        prop="teamName"
        align="left" width="200"/>
      <el-table-column
        :label="$t('notice.project')"
        prop="projectName"
        align="left" width="300"/>
      <el-table-column
        :label="$t('notice.title')"
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
            v-hasPermi="['notice:remove']"
          >{{$t('delete')}}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      class="notice-table-pagination"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
    <view-notice ref="viewNotice" @read="refreshData"></view-notice>
    <option-notice ref="noticeOption" :member-id="getUserId()" />
    <send-notice-dialog ref="sendNoticeDialog" @send="refreshData" />
  </div>
</template>

<script>
import {delNotice, groupStatisticsNotice, listNotice} from "@/api/system/notice";
import OptionNotice from "./option/index"
import SendNoticeDialog from "./send/index"
import ViewNotice from "@/components/Notice/ViewNotice";
import pageActionHints from '@/mixins/page-action-hints'
import listQueryKeyboardNav from '@/mixins/list-query-keyboard-nav'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { checkPermi } from '@/utils/permission'
import {
  assignRowHintLetters,
  collectHintLettersFromToolbar,
  getDefectTableScrollBody,
  isRowIntersectingContainer,
  resolveNoticeTableCheckboxAnchor
} from '@/utils/defect-row-kbd-hints'

const NOTICE_KBD_SCOPE = 'notice'

export default {
  name: "Notice",
  mixins: [pageActionHints, listQueryKeyboardNav],
  components: { ViewNotice, OptionNotice, SendNoticeDialog },
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
  mounted() {
    this.registerNoticeShortcuts()
    /** 设置指定消息已读 */
    if(this.$route.query.noticeId) {
      this.handleNotice(this.$route.query);
    }
  },
  activated() {
    this.registerNoticeShortcuts()
  },
  deactivated() {
    if (this.$shortcut) this.$shortcut.unregisterPage(NOTICE_KBD_SCOPE)
  },
  beforeDestroy() {
    if (this.$shortcut) this.$shortcut.unregisterPage(NOTICE_KBD_SCOPE)
  },
  methods: {
    registerNoticeShortcuts() {
      if (!this.$shortcut) return
      this.$shortcut.registerPage(NOTICE_KBD_SCOPE, [
        { key: 'query', defaultLetter: 'S', run: () => this.shortcutFocusQuery() },
        { key: 'switchTab', defaultLetter: 'J', run: () => this.shortcutSwitchTab() },
        { key: 'config', defaultLetter: 'G', run: () => this.shortcutOpenConfig() },
        { key: 'send', defaultLetter: 'E', run: () => this.shortcutSendNotice() },
        { key: 'prevPage', defaultLetter: 'B', run: () => this.shortcutChangePage(-1) },
        { key: 'nextPage', defaultLetter: 'P', run: () => this.shortcutChangePage(1) }
      ])
    },
    getPageActionHintContainer() {
      return this.$refs.noticeMain || this.$el
    },
    getPageActionHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${NOTICE_KBD_SCOPE}.${key}`, def)
      return [
        {
          key: 'query',
          letter: L('query', 'S'),
          badgeSelector: '.notice-hint-query',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.shortcutFocusQuery()
        },
        {
          key: 'switchTab',
          letter: L('switchTab', 'J'),
          badgeSelector: '.notice-hint-tabs .el-tabs__item.is-active .notice-tab-label',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.shortcutSwitchTab()
        },
        {
          key: 'config',
          letter: L('config', 'G'),
          badgeSelector: '.notice-hint-config',
          floatOffset: { placement: 'bottom-right-outset', outset: 2, dx: 5, dy: 5 },
          run: () => this.shortcutOpenConfig()
        },
        {
          key: 'send',
          letter: L('send', 'E'),
          badgeSelector: '.notice-hint-send',
          floatOffset: { placement: 'bottom-right-outset', outset: 2, dy: 5 },
          run: () => this.shortcutSendNotice(),
          visible: () => checkPermi(['notice:send'])
        },
        {
          key: 'prevPage',
          letter: L('prevPage', 'B'),
          badgeSelector: '.notice-table-pagination .btn-prev',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.shortcutChangePage(-1),
          visible: () => this.total > 0
        },
        {
          key: 'nextPage',
          letter: L('nextPage', 'P'),
          badgeSelector: '.notice-table-pagination .btn-next',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.shortcutChangePage(1),
          visible: () => this.total > 0
        }
      ]
    },
    /** ⌘ 按住：表格可见行勾选列动态徽标（1–9 优先） */
    getPageDynamicActionHints(ctx) {
      const used = (ctx && ctx.usedLetters) ? new Set(ctx.usedLetters) : new Set()
      collectHintLettersFromToolbar(this.getPageActionHints()).forEach((ch) => used.add(ch))
      return this.buildNoticeTableRowActionHints(used)
    },
    getPageActionHintScrollRoots() {
      const table = this.$refs.noticeTable
      if (!table || !table.$el) return []
      const bodyWrap = getDefectTableScrollBody(table.$el)
      return bodyWrap ? [bodyWrap] : []
    },
    buildNoticeTableRowActionHints(usedLetters) {
      const table = this.$refs.noticeTable
      if (!table || !table.$el) return []
      const bodyWrap = getDefectTableScrollBody(table.$el)
      if (!bodyWrap) return []
      const list = this.noticeList || []
      const seen = new Set()
      const anchors = []
      bodyWrap.querySelectorAll('tbody tr.el-table__row').forEach((tr, rowIndex) => {
        if (!isRowIntersectingContainer(tr, bodyWrap)) return
        const row = list[rowIndex]
        if (!row || row.noticeId == null) return
        const noticeId = String(row.noticeId)
        if (seen.has(noticeId)) return
        seen.add(noticeId)
        const anchor = resolveNoticeTableCheckboxAnchor(tr)
        if (!anchor) return
        anchors.push({
          anchor,
          skipViewportCheck: true,
          run: () => this.handleNotice(row)
        })
      })
      const letters = assignRowHintLetters(anchors.length, usedLetters)
      const rowFloat = { placement: 'bottom-right-outset', outset: 2 }
      return anchors.map((item, i) => ({
        ...item,
        letter: letters[i],
        floatOffset: rowFloat,
        key: `row-${i}`
      })).filter((item) => item.letter)
    },
    shortcutFocusQuery() {
      this.enterListQueryKeyboardNav()
    },
    getListQueryNavItems() {
      return [{ key: 'noticeTitle' }]
    },
    getListQueryNavToolbarRef() {
      return 'noticeToolsRight'
    },
    shortcutSwitchTab() {
      const tabs = this.groupList || []
      if (!tabs.length) return
      const idx = tabs.findIndex((t) => t.groupName === this.activeTabName)
      const next = tabs[(idx < 0 ? 0 : idx + 1) % tabs.length]
      this.activeTabName = next.groupName
      this.handleQuery()
    },
    shortcutOpenConfig() {
      this.setNoticeOptionHandle()
    },
    shortcutSendNotice() {
      if (!checkPermi(['notice:send'])) return
      this.handleSendNotice()
    },
    shortcutChangePage(delta) {
      const root = this.getPageActionHintContainer()
      if (!root || typeof root.querySelector !== 'function') return
      const btn = root.querySelector(
        delta < 0 ? '.notice-table-pagination .btn-prev' : '.notice-table-pagination .btn-next'
      )
      if (btn && !btn.classList.contains('disabled') && typeof btn.click === 'function') {
        btn.click()
      }
    },
    /** 获取项目id */
    getProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId || 0);
    },
    /** 获取用户id */
    getUserId() {
      return this.$store.state.user.id;
    },
    /** 设置通知配置 */
    setNoticeOptionHandle() {
      this.$refs.noticeOption.open();
    },
    /** 添加通知标签 */
    addNoticeTabHandle() {

    },
    /** 返回 */
    goBack() {
      this.$router.back();
    },
    /** 获取页签数据 */
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
    /** 刷新数据 */
    refreshData() {
      this.getList();
      this.getGroupStatisticsNotice();
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
      const noticeIds = row?row.noticeId || this.ids:this.ids;
      if(noticeIds.length==0) {
        this.$message.error(this.$i18n.t('notice.batch-delete-except').toString());
        return;
      }
      this.$modal.confirm(this.$i18n.t('notice.is-delete')).then(function() {
        return delNotice(noticeIds);
      }).then(() => {
        this.refreshData();
        this.$modal.msgSuccess(this.$i18n.t('delete.success'));
      }).catch(() => {});
      if(event) {
        event.stopPropagation();
      }
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.noticeId)
      this.single = selection.length!=1
      this.multiple = !selection.length
    },
    /** 显示通知详情操作 */
    handleNotice(notice) {
      this.$refs.viewNotice.open(notice.noticeId);
    },
    /** 显示发送通知对话框操作 */
    handleSendNotice() {
      this.$refs.sendNoticeDialog.open();
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
.notice-page {
  .notice-list-table ::v-deep td.el-table-column--selection .cell {
    overflow: visible !important;
  }
  .tabs-tools-row {
    margin-bottom: 10px;
  }
  .notice-hint-tabs ::v-deep .el-tabs__header {
    margin-bottom: 0;
  }
  .notice-hint-tabs .el-tabs__item.is-active {
    position: relative;
    overflow: visible !important;
  }
  .notice-hint-config {
    position: relative;
    overflow: visible !important;
  }
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
