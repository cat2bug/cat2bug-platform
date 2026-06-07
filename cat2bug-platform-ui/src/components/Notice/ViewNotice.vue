<template>
  <el-drawer
    size="50%"
    :visible.sync="visible"
    direction="rtl"
    :before-close="closeDefectDrawer">
    <template slot="title">
      <div class="notice-header">
        <div class="notice-header-title">
          <i class="el-icon-arrow-left" @click="cancel"></i>
          <h4 class="notice-header-title-name">{{ notice.noticeTitle }}</h4>
        </div>
      </div>
    </template>
    <div class="app-container notice-body" v-loading="loading">
      <el-collapse v-model="activeNames">
        <el-collapse-item name="base">
          <template slot="title">
            <span class="notice-collapse-title">{{ $t('base-info') }}</span>
          </template>
          <el-row class="notice-header-body-base" :gutter="16">
            <el-col :span="12">
              <label>{{ $t('notice.team') }}:</label>
              <span>{{ notice.teamName }}</span>
            </el-col>
            <el-col :span="12">
              <label>{{ $t('notice.project') }}:</label>
              <span>{{ notice.projectName }}</span>
            </el-col>
            <el-col :span="12">
              <label>{{ $t('notice.group') }}:</label>
              <span>{{ notice.groupName }}</span>
            </el-col>
            <el-col :span="12">
              <label>{{ $t('create-time') }}:</label>
              <span>{{ notice.createTime }}</span>
            </el-col>
          </el-row>

          <template v-if="defectNoticeLoading">
            <div class="notice-defect-loading" v-loading="true"></div>
          </template>
          <template v-else-if="defectNotice">
            <div class="notice-defect-block">
              <div class="notice-defect-title">
                <span class="notice-defect-title-num">#{{ defectNotice.projectNum }}</span>
                {{ defectNotice.defectName }}
              </div>
              <p v-if="defectViewHref" class="notice-defect-view-link">
                <a :href="defectViewHref" @click.prevent="openDefectPage">{{ $t('defect.click-view') }}</a>
              </p>
              <defect-custom-fields-display
                class="notice-defect-fields"
                :project-id="defectNotice.projectId || notice.projectId"
                :defect="defectNotice"
                :defect-case="defectCase"
                :custom-fields="defectNotice.customFields"
              />
            </div>
          </template>
        </el-collapse-item>

        <el-collapse-item v-if="!defectNotice && !defectNoticeLoading" name="content">
          <template slot="title">
            <span class="notice-collapse-title">{{ $t('notice.content') }}</span>
          </template>
          <markdown-it-vue :content="notice.noticeContent + ''" />
        </el-collapse-item>
      </el-collapse>
      <div slot="footer" class="dialog-footer"></div>
    </div>
  </el-drawer>
</template>

<script>
import { getNotice } from '@/api/system/notice'
import { getDefect } from '@/api/system/defect'
import { getCase } from '@/api/system/case'
import MarkdownItVue from 'markdown-it-vue'
import DefectCustomFieldsDisplay from '@/components/DefectCustomField/DefectCustomFieldsDisplay'
import {
  isDefectGroupNotice,
  parseDefectIdFromNoticeContent
} from '@/utils/notice-defect'

export default {
  name: 'ViewNotice',
  components: {
    MarkdownItVue,
    DefectCustomFieldsDisplay
  },
  data() {
    return {
      activeNames: ['base'],
      loading: false,
      defectNoticeLoading: false,
      visible: false,
      notice: {},
      defectNotice: null,
      defectCase: null
    }
  },
  computed: {
    defectViewHref() {
      if (!this.defectNotice || this.defectNotice.defectId == null) return ''
      const projectId = this.defectNotice.projectId || this.notice.projectId
      const base = `${window.location.origin}${window.location.pathname}`
      const query = projectId != null
        ? `?projectId=${projectId}&defectId=${this.defectNotice.defectId}`
        : `?defectId=${this.defectNotice.defectId}`
      return `${base}#/project/defect${query}`
    }
  },
  methods: {
    resetDefectNoticeState() {
      this.defectNotice = null
      this.defectCase = null
      this.defectNoticeLoading = false
    },
    getNoticeInfo(noticeId) {
      this.loading = true
      this.resetDefectNoticeState()
      getNotice(noticeId).then((res) => {
        this.notice = res.data || {}
        this.loading = false
        this.activeNames = ['base']
        if (isDefectGroupNotice(this.notice)) {
          return this.loadDefectNoticeContent(this.notice).finally(() => {
            this.$emit('read', this.notice)
          })
        }
        this.activeNames = ['base', 'content']
        this.$emit('read', this.notice)
      }).catch(() => {
        this.loading = false
      })
    },
    loadDefectNoticeContent(notice) {
      const defectId = parseDefectIdFromNoticeContent(notice.noticeContent)
      if (!defectId) return Promise.resolve()
      this.defectNoticeLoading = true
      return getDefect(defectId).then((res) => {
        this.defectNotice = res.data || null
        const caseId = this.defectNotice && this.defectNotice.caseId
        if (!caseId) return
        return getCase(caseId).then((caseRes) => {
          this.defectCase = caseRes.data || null
        }).catch(() => {
          this.defectCase = null
        })
      }).catch(() => {
        this.defectNotice = null
        this.defectCase = null
      }).finally(() => {
        this.defectNoticeLoading = false
      })
    },
    openDefectPage() {
      if (!this.defectViewHref) return
      window.open(this.defectViewHref, '_blank')
    },
    open(noticeId) {
      this.visible = true
      this.getNoticeInfo(noticeId)
    },
    cancel() {
      this.$emit('close')
      this.visible = false
    },
    closeDefectDrawer(done) {
      done()
      this.cancel()
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-drawer {
  border-left: 3px solid #ff4949;
  .el-drawer__header {
    margin-bottom: 0;
    padding-bottom: 12px;
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
    display: inline-flex;
    justify-content: flex-start;
    align-items: center;
    flex-direction: row;
    overflow: hidden;
    .el-icon-arrow-left {
      font-size: 22px;
      margin-right: 10px;
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
      font-size: 20px;
      color: #303133;
      font-weight: 500;
      margin: 0;
    }
  }
}
.notice-body {
  padding: 12px 30px 20px;
  ::v-deep .el-collapse {
    border-width: 0;
    .el-collapse-item__header {
      font-size: 16px;
      height: 42px;
      line-height: 42px;
      overflow: visible;
    }
    .el-collapse-item__content {
      padding-top: 10px;
      padding-bottom: 6px;
      > :first-child {
        margin-top: 0;
      }
    }
    > .el-collapse-item > .el-collapse-item__wrap {
      border-bottom: 1px solid var(--border-color-light, #ebeef5);
    }
    .el-collapse-item:last-child .el-collapse-item__wrap {
      border-width: 0;
    }
  }
  .notice-collapse-title {
    display: inline-block;
    line-height: 1.4;
  }
  .notice-header-body-base {
    .el-col {
      margin-bottom: 8px;
      line-height: 1.5;
      label {
        font-size: 14px;
        color: #909399;
        width: 88px;
        display: inline-block;
        margin-right: 8px;
        text-align: right;
        vertical-align: top;
      }
      span {
        color: #303133;
      }
    }
  }
  .notice-defect-block {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid var(--border-color-light, #ebeef5);
  }
  .notice-defect-loading {
    min-height: 60px;
    margin-top: 12px;
  }
  .notice-defect-title {
    font-size: 16px;
    font-weight: 500;
    line-height: 1.5;
    margin-bottom: 6px;
    color: #303133;
    .notice-defect-title-num {
      margin-right: 6px;
    }
  }
  .notice-defect-view-link {
    margin: 0 0 10px;
    line-height: 1.5;
    a {
      color: #409eff;
    }
  }
  ::v-deep .notice-defect-fields {
    .defect-custom-field-display-col {
      margin-bottom: 4px;
      line-height: 1.5;
      label {
        color: var(--text-color-secondary, #909399);
        margin-right: 6px;
      }
    }
    .el-row {
      margin-bottom: 0;
    }
  }
}
</style>
