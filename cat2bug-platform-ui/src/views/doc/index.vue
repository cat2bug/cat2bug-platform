<template>
  <div class="doc-viewer-wrapper">
    <div class="doc-header project-add-page-header">
      <el-page-header class="doc-hint-back" @back="handleBack">
        <template slot="content">
          <span v-html="formattedTitle"></span>
        </template>
      </el-page-header>
    </div>
    <div class="doc-viewer">
      <div class="doc-sidebar">
        <div class="search-box">
          <el-input
            v-model="searchText"
            class="doc-hint-search"
            :placeholder="$t('search')"
            prefix-icon="el-icon-search"
            clearable
            @input="handleSearch"
          />
        </div>
        <div class="doc-tree doc-hint-tree">
          <el-tree
            ref="docTree"
            :data="filteredDocs"
            :props="treeProps"
            node-key="label"
            :default-expanded-keys="expandedKeys"
            :highlight-current="true"
            @node-click="handleNodeClick"
          >
            <span class="custom-tree-node" slot-scope="{ node, data }">
              <svg-icon v-if="data.icon && !data.icon.startsWith('el-icon')" :icon-class="data.icon" />
              <i v-else :class="data.icon"></i>
              <span v-html="highlightText(node.label)"></span>
            </span>
          </el-tree>
        </div>
      </div>
      <div class="doc-main" ref="docScroll" @scroll.passive="onDocContentScroll">
        <div class="doc-content">
          <div class="markdown-body" ref="markdownBody" v-html="renderedContent"></div>
          <el-backtop
            target=".doc-main"
            :visibility-height="300"
            :right="40"
            :bottom="40"
          >
            <div class="backtop-button">
              <i class="el-icon-top"></i>
            </div>
          </el-backtop>
        </div>
        <doc-page-outline
          class="doc-hint-outline"
          :title="$t('doc.on-this-page')"
          :print-label="$t('doc.print')"
          :items="outlineItems"
          :active-id="activeOutlineId"
          :kbd-focus-id="docKbdRegion === 'outline' ? docKbdOutlineFocusId : ''"
          @click="scrollToOutlineHeading"
          @print="printCurrentDoc"
        />
      </div>
    </div>
  </div>
</template>

<script>
import MarkdownIt from 'markdown-it'
import MarkdownItContainer from 'markdown-it-container'
import MarkdownItAnchor from 'markdown-it-anchor'
import { setHeader } from '@/utils/request'
import {
  preprocessCodeTabs,
  initCodeTabs,
  safeHighlight,
  resolveApiDocBaseUrls,
  applyApiDocPlaceholders
} from '@/utils/doc-code-tabs'
import 'highlight.js/styles/github-gist.css'
import DocPageOutline from '@/components/Doc/DocPageOutline.vue'
import pageActionHints from '@/mixins/page-action-hints'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import {
  clampDocIndex,
  findDocLeafIndex,
  flattenDocTreeLeaves
} from '@/utils/doc-page-kbd'

const DOC_KBD_SCOPE = 'system-doc'

export default {
  name: 'DocViewer',
  mixins: [pageActionHints],
  components: { DocPageOutline },
  data() {
    return {
      searchText: '',
      currentDoc: 'README.md',
      currentDocTitle: '系统文档',
      renderedContent: '',
      expandedKeys: ['README.md'],
      md: null,
      treeProps: {
        children: 'children',
        label: 'label'
      },
      docContents: {}, // 缓存文档内容
      docHistory: [], // 文档浏览历史记录
      outlineItems: [],
      activeOutlineId: '',
      outlineScrollRaf: null,
      docKbdRegion: '',
      docKbdTreeIndex: -1,
      docKbdOutlineIndex: -1,
      docs: [
        {
          label: '系统介绍',
          path: 'README.md',
          icon: 'el-icon-document'
        },
        {
          label: '快速开始',
          path: 'quick-start.md',
          icon: 'el-icon-video-play'
        },
        {
          label: '角色介绍',
          icon: 'peoples',
          children: [
            {
              label: '团队创建人',
              path: 'role-guide/team-creator.md',
              icon: 'point'
            },
            {
              label: '团队管理员',
              path: 'role-guide/team-admin.md',
              icon: 'point'
            },
            {
              label: '团队普通人员',
              path: 'role-guide/team-member.md',
              icon: 'point'
            },
            {
              label: '项目创建人',
              path: 'role-guide/project-creator.md',
              icon: 'point'
            },
            {
              label: '项目管理员',
              path: 'role-guide/project-admin.md',
              icon: 'point'
            },
            {
              label: '开发',
              path: 'role-guide/developer.md',
              icon: 'point'
            },
            {
              label: '测试',
              path: 'role-guide/tester.md',
              icon: 'point'
            },
            {
              label: '外部人员',
              path: 'role-guide/external.md',
              icon: 'point'
            },
            {
              label: '系统管理员',
              path: 'role-guide/system-admin.md',
              icon: 'point'
            }
          ]
        },
        {
          label: '用户指南',
          icon: 'user',
          children: [
            {
              label: '用户管理',
              icon: 'user',
              children: [
                {
                  label: '登录',
                  path: 'user-guide/user-management/user-login.md',
                  icon: 'login'
                },
                {
                  label: '注册',
                  path: 'user-guide/user-management/user-register.md',
                  icon: 'el-icon-edit'
                },
                {
                  label: '个人中心',
                  path: 'user-guide/user-management/user-profile.md',
                  icon: 'user_center'
                },
                {
                  label: '通知',
                  icon: 'notice',
                  children: [
                    {
                      label: '通知列表',
                      path: 'user-guide/user-management/notification/notification-list.md',
                      icon: 'list'
                    },
                    {
                      label: '发送选项',
                      path: 'user-guide/user-management/notification/send-options.md',
                      icon: 'el-icon-setting'
                    },
                    {
                      label: '接收平台',
                      icon: 'mk-card',
                      children: [
                        {
                          label: '系统内部通知',
                          path: 'user-guide/user-management/notification/system-notification.md',
                          icon: 'server'
                        },
                        {
                          label: '电子邮件',
                          path: 'user-guide/user-management/notification/email-notification.md',
                          icon: 'el-icon-message'
                        },
                        {
                          label: '钉钉',
                          path: 'user-guide/user-management/notification/dingtalk-notification.md',
                          icon: 'dingding'
                        },
                        {
                          label: '飞书',
                          path: 'user-guide/user-management/notification/feishu-notification.md',
                          icon: 'feishu'
                        },
                        {
                          label: '企业微信',
                          path: 'user-guide/user-management/notification/wecom-notification.md',
                          icon: 'wechat'
                        }
                      ]
                    }
                  ]
                }
              ]
            },
            {
              label: '团队列表',
              path: 'user-guide/team-manage.md',
              icon: 'el-icon-office-building'
            },
            {
              label: '项目管理',
              path: 'user-guide/project-manage.md',
              icon: 'el-icon-folder-opened'
            },
            {
              label: '当前项目',
              icon: 'el-icon-folder',
              children: [
                {
                  label: '仪表盘',
                  path: 'user-guide/current-project/dashboard.md',
                  icon: 'dashboard'
                },
                {
                  label: '测试用例',
                  icon: 'case',
                  children: [
                    {
                      label: '用例介绍',
                      path: 'user-guide/current-project/case.md',
                      icon: 'el-icon-document'
                    },
                    {
                      label: '新建用例',
                      path: 'user-guide/current-project/case/case-create.md',
                      icon: 'el-icon-plus'
                    },
                    {
                      label: '修改用例',
                      path: 'user-guide/current-project/case/case-edit.md',
                      icon: 'el-icon-edit'
                    },
                    {
                      label: '删除用例',
                      path: 'user-guide/current-project/case/case-delete.md',
                      icon: 'el-icon-delete'
                    },
                    {
                      label: '导入用例',
                      path: 'user-guide/current-project/case/case-import.md',
                      icon: 'el-icon-upload2'
                    },
                    {
                      label: '导出用例',
                      path: 'user-guide/current-project/case/case-export.md',
                      icon: 'el-icon-download'
                    },
                    {
                      label: 'AI用例生成',
                      path: 'user-guide/current-project/case/case-ai.md',
                      icon: 'robot'
                    }
                  ]
                },
                {
                  label: '测试计划',
                  icon: 'date',
                  children: [
                    {
                      label: '计划介绍',
                      path: 'user-guide/current-project/plan.md',
                      icon: 'el-icon-document'
                    },
                    {
                      label: '新建计划',
                      path: 'user-guide/current-project/plan/plan-create.md',
                      icon: 'el-icon-plus'
                    },
                    {
                      label: '复制计划',
                      path: 'user-guide/current-project/plan/plan-copy.md',
                      icon: 'el-icon-document-copy'
                    },
                    {
                      label: '执行计划',
                      path: 'user-guide/current-project/plan/plan-execute.md',
                      icon: 'el-icon-video-play'
                    },
                    {
                      label: '修改计划',
                      path: 'user-guide/current-project/plan/plan-edit.md',
                      icon: 'el-icon-edit'
                    },
                    {
                      label: '删除计划',
                      path: 'user-guide/current-project/plan/plan-delete.md',
                      icon: 'el-icon-delete'
                    }
                  ]
                },
                {
                  label: '缺陷管理',
                  icon: 'bug',
                  children: [
                    {
                      label: '缺陷介绍',
                      path: 'user-guide/current-project/defect.md',
                      icon: 'el-icon-document'
                    },
                    {
                      label: '页标签',
                      path: 'user-guide/current-project/defect/defect-tabs.md',
                      icon: 'el-icon-collection-tag'
                    },
                    {
                      label: '数据统计',
                      path: 'user-guide/current-project/defect/defect-statistics.md',
                      icon: 'el-icon-data-line'
                    },
                    {
                      label: '缺陷显示模式',
                      icon: 'eye-open',
                      children: [
                        {
                          label: 'Table模式',
                          icon: 'table',
                          children: [
                            {
                              label: 'Table模式介绍',
                              path: 'user-guide/current-project/defect/display-mode/table-mode/table-mode-intro.md',
                              icon: 'el-icon-document'
                            },
                            {
                              label: '新建缺陷',
                              path: 'user-guide/current-project/defect/display-mode/table-mode/defect-create.md',
                              icon: 'el-icon-plus'
                            },
                            {
                              label: '修改缺陷',
                              path: 'user-guide/current-project/defect/display-mode/table-mode/defect-edit.md',
                              icon: 'el-icon-edit'
                            },
                            {
                              label: '指派缺陷',
                              path: 'user-guide/current-project/defect/display-mode/table-mode/defect-assign.md',
                              icon: 'el-icon-user'
                            },
                            {
                              label: '修复缺陷',
                              path: 'user-guide/current-project/defect/display-mode/table-mode/defect-repair.md',
                              icon: 'el-icon-check'
                            },
                            {
                              label: '驳回缺陷',
                              path: 'user-guide/current-project/defect/display-mode/table-mode/defect-reject.md',
                              icon: 'el-icon-close'
                            },
                            {
                              label: '通过缺陷',
                              path: 'user-guide/current-project/defect/display-mode/table-mode/defect-pass.md',
                              icon: 'el-icon-circle-check'
                            },
                            {
                              label: '开启缺陷',
                              path: 'user-guide/current-project/defect/display-mode/table-mode/defect-reopen.md',
                              icon: 'el-icon-refresh'
                            },
                            {
                              label: '关闭缺陷',
                              path: 'user-guide/current-project/defect/display-mode/table-mode/defect-close.md',
                              icon: 'el-icon-turn-off'
                            },
                            {
                              label: '删除缺陷',
                              path: 'user-guide/current-project/defect/display-mode/table-mode/defect-delete.md',
                              icon: 'el-icon-delete'
                            },
                            {
                              label: '新建评论',
                              path: 'user-guide/current-project/defect/display-mode/table-mode/defect-comment.md',
                              icon: 'el-icon-chat-line-round'
                            }
                          ]
                        },
                        {
                          label: 'Excel模式',
                          icon: 'excel2',
                          children: [
                            {
                              label: 'Excel模式介绍',
                              path: 'user-guide/current-project/defect/display-mode/excel-mode/excel-mode-intro.md',
                              icon: 'el-icon-document'
                            },
                            {
                              label: '新建缺陷',
                              path: 'user-guide/current-project/defect/display-mode/excel-mode/defect-create.md',
                              icon: 'el-icon-plus'
                            },
                            {
                              label: '修改缺陷',
                              path: 'user-guide/current-project/defect/display-mode/excel-mode/defect-edit.md',
                              icon: 'el-icon-edit'
                            },
                            {
                              label: '删除缺陷',
                              path: 'user-guide/current-project/defect/display-mode/excel-mode/defect-delete.md',
                              icon: 'el-icon-delete'
                            }
                          ]
                        }
                      ]
                    },
                    {
                      label: 'Excel导入',
                      path: 'user-guide/current-project/defect/defect-import.md',
                      icon: 'el-icon-upload2'
                    },
                    {
                      label: 'Excel导出',
                      path: 'user-guide/current-project/defect/defect-export.md',
                      icon: 'el-icon-download'
                    }
                  ]
                },
                {
                  label: '交付物管理',
                  icon: 'cascader',
                  children: [
                    {
                      label: '交付物介绍',
                      path: 'user-guide/current-project/module.md',
                      icon: 'el-icon-document'
                    },
                    {
                      label: '新建交付物',
                      path: 'user-guide/current-project/module/module-create.md',
                      icon: 'el-icon-plus'
                    },
                    {
                      label: '修改交付物',
                      path: 'user-guide/current-project/module/module-edit.md',
                      icon: 'el-icon-edit'
                    },
                    {
                      label: '删除交付物',
                      path: 'user-guide/current-project/module/module-delete.md',
                      icon: 'el-icon-delete'
                    }
                  ]
                },
                {
                  label: '报告管理',
                  icon: 'chart',
                  children: [
                    {
                      label: '报告介绍',
                      path: 'user-guide/current-project/report.md',
                      icon: 'el-icon-document'
                    },
                    {
                      label: '添加模版',
                      path: 'user-guide/current-project/report/template-create.md',
                      icon: 'el-icon-circle-plus'
                    },
                    {
                      label: '生成报告',
                      icon: 'el-icon-plus',
                      children: [
                        {
                          label: '通过API创建',
                          path: 'user-guide/current-project/report/report-create-api.md',
                          icon: 'el-icon-connection'
                        },
                        {
                          label: '通过模版创建',
                          path: 'user-guide/current-project/report/report-create-template.md',
                          icon: 'el-icon-document-copy'
                        }
                      ]
                    },
                    {
                      label: '查看报告',
                      path: 'user-guide/current-project/report/report-view.md',
                      icon: 'el-icon-view'
                    },
                    {
                      label: '导出报告',
                      path: 'user-guide/current-project/report/report-export.md',
                      icon: 'el-icon-download'
                    },
                    {
                      label: '删除报告',
                      path: 'user-guide/current-project/report/report-delete.md',
                      icon: 'el-icon-delete'
                    }
                  ]
                },
                {
                  label: '文档管理',
                  icon: 'education',
                  children: [
                    {
                      label: '文档介绍',
                      path: 'user-guide/current-project/document.md',
                      icon: 'el-icon-document'
                    },
                    {
                      label: '新建文件夹',
                      path: 'user-guide/current-project/document/folder-create.md',
                      icon: 'el-icon-folder-add'
                    },
                    {
                      label: '新建文档',
                      path: 'user-guide/current-project/document/document-create.md',
                      icon: 'el-icon-document-add'
                    },
                    {
                      label: '移动文件夹/文档',
                      path: 'user-guide/current-project/document/document-move.md',
                      icon: 'el-icon-rank'
                    },
                    {
                      label: '删除文件夹/文档',
                      path: 'user-guide/current-project/document/document-delete.md',
                      icon: 'el-icon-delete'
                    },
                    {
                      label: '更新文件夹',
                      path: 'user-guide/current-project/document/folder-update.md',
                      icon: 'el-icon-folder-opened'
                    },
                    {
                      label: '更新文档',
                      path: 'user-guide/current-project/document/document-update.md',
                      icon: 'el-icon-document-checked'
                    },
                    {
                      label: '下载文档',
                      path: 'user-guide/current-project/document/document-download.md',
                      icon: 'el-icon-download'
                    }
                  ]
                },
                {
                  label: '项目设置',
                  icon: 'el-icon-setting',
                  children: [
                    {
                      label: '项目信息',
                      icon: 'el-icon-info',
                      children: [
                        {
                          label: '基本信息',
                          path: 'user-guide/current-project/project-setting/project-info/base-info.md',
                          icon: 'el-icon-edit'
                        },
                        {
                          label: 'API KEY',
                          path: 'user-guide/current-project/project-setting/project-info/api-key.md',
                          icon: 'el-icon-key'
                        },
                        {
                          label: '同步到云端',
                          path: 'user-guide/current-project/project-setting/project-info/push.md',
                          icon: 'el-icon-upload'
                        },
                        {
                          label: '删除项目',
                          path: 'user-guide/current-project/project-setting/project-info/delete.md',
                          icon: 'el-icon-delete'
                        }
                      ]
                    },
                    {
                      label: 'AI管理',
                      icon: 'robot',
                      children: [
                        {
                          label: 'Ollama模型',
                          path: 'user-guide/current-project/project-setting/project-ai/ollama.md',
                          icon: 'robot'
                        },
                        {
                          label: 'OpenAI账号管理',
                          path: 'user-guide/current-project/project-setting/project-ai/openai-account.md',
                          icon: 'el-icon-user'
                        }
                      ]
                    },
                    {
                      label: '第三方应用',
                      icon: 'el-icon-connection',
                      children: [
                        {
                          label: '钉钉',
                          path: 'user-guide/current-project/project-setting/project-third-party/ding.md',
                          icon: 'el-icon-message'
                        },
                        {
                          label: '飞书',
                          path: 'user-guide/current-project/project-setting/project-third-party/feishu.md',
                          icon: 'el-icon-chat-line-square'
                        },
                        {
                          label: '企业微信',
                          path: 'user-guide/current-project/project-setting/project-third-party/enterprise-wechat.md',
                          icon: 'el-icon-chat-dot-square'
                        }
                      ]
                    },
                    {
                      label: '组织和成员',
                      icon: 'mk-member',
                      children: [
                        {
                          label: '成员管理',
                          path: 'user-guide/current-project/project-setting/project-member/member-manage.md',
                          icon: 'peoples'
                        }
                      ]
                    }
                  ]
                }
              ]
            },
            {
              label: '团队设置',
              icon: 'team-option',
              children: [
                {
                  label: '团队信息',
                  path: 'user-guide/team-setting/team-info/base-info.md',
                  icon: 'el-icon-info',
                  children: [
                    {
                      label: '基本信息',
                      path: 'user-guide/team-setting/team-info/base-info.md',
                      icon: 'el-icon-edit'
                    },
                    {
                      label: '删除团队',
                      path: 'user-guide/team-setting/team-info/delete.md',
                      icon: 'el-icon-delete'
                    }
                  ]
                },
                {
                  label: '组织和成员',
                  path: 'user-guide/team-setting/team-member/member-manage.md',
                  icon: 'mk-member',
                  children: [
                    {
                      label: '成员管理',
                      path: 'user-guide/team-setting/team-member/member-manage.md#成员管理',
                      icon: 'peoples'
                    }
                  ]
                }
              ]
            }
          ]
        },
        {
          label: '管理员指南',
          icon: 'admin',
          children: [
            {
              label: '安装向导',
              path: 'admin-guide/install-wizard.md',
              icon: 'el-icon-setting'
            },
            {
              label: '团队管理',
              path: 'admin-guide/admin-team.md',
              icon: 'el-icon-office-building'
            },
            {
              label: '项目管理',
              path: 'admin-guide/admin-project.md',
              icon: 'el-icon-folder-opened'
            },
            {
              label: '角色管理',
              path: 'admin-guide/admin-role.md',
              icon: 'role'
            },
            {
              label: '成员管理',
              path: 'admin-guide/admin-member.md',
              icon: 'peoples'
            }
          ]
        },
        {
          label: 'AI 指南',
          icon: 'robot',
          children: [
            {
              label: 'MCP 接入',
              path: 'ai-guide/mcp.md',
              icon: 'el-icon-connection'
            }
          ]
        },
        {
          label: 'API 文档',
          icon: 'el-icon-connection',
          children: [
            {
              label: 'API 介绍',
              path: 'api/api-intro.md',
              icon: 'el-icon-info'
            },
            {
              label: '交付物接口',
              path: 'api/api-deliverable.md',
              icon: 'el-icon-files'
            },
            {
              label: '测试用例接口',
              path: 'api/api-case.md',
              icon: 'case'
            },
            {
              label: '缺陷接口',
              path: 'api/api-defect.md',
              icon: 'bug'
            },
            {
              label: '报告接口',
              path: 'api/api-report-defect.md',
              icon: 'chart'
            },
            {
              label: '成员接口',
              path: 'api/api-member.md',
              icon: 'el-icon-user'
            },
            {
              label: '文件接口',
              path: 'api/api-file.md',
              icon: 'el-icon-upload'
            },
            {
              label: '项目接口',
              path: 'api/api-project.md',
              icon: 'el-icon-folder-opened'
            }
          ]
        },
        {
          label: '常见问题',
          path: 'faq.md',
          icon: 'el-icon-question'
        }
      ]
    }
  },
  computed: {
    filteredDocs() {
      if (!this.searchText) {
        return this.docs
      }
      // 将搜索文本按空格分割成多个关键词
      const keywords = this.searchText.toLowerCase().trim().split(/\s+/).filter(k => k)
      if (keywords.length === 0) {
        return this.docs
      }
      return this.filterDocsRecursive(this.docs, keywords)
    },
    formattedTitle() {
      if (!this.currentDocTitle) {
        return '系统文档'
      }
      const parts = this.currentDocTitle.split(' / ')
      if (parts.length === 1) {
        return `<span style="color: #303133;">${parts[0]}</span>`
      }
      const grayParts = parts.slice(0, -1).map(part =>
        `<span style="color: #909399;">${part}</span>`
      ).join('<span style="color: #909399;"> / </span>')
      const lastPart = `<span style="color: #303133;">${parts[parts.length - 1]}</span>`
      return `${grayParts}<span style="color: #909399;"> / </span>${lastPart}`
    },
    docKbdOutlineFocusId() {
      const items = this.outlineItems || []
      if (!items.length || this.docKbdOutlineIndex < 0) return ''
      const item = items[this.docKbdOutlineIndex]
      return item ? item.id : ''
    }
  },
  beforeDestroy() {
    this.exitDocKbdRegion()
    if (this.$shortcut) this.$shortcut.unregisterPage(DOC_KBD_SCOPE)
    if (this.outlineScrollRaf) {
      cancelAnimationFrame(this.outlineScrollRaf)
      this.outlineScrollRaf = null
    }
  },
  mounted() {
    this.md = new MarkdownIt({
      html: true,
      highlight: (str, lang) => {
        const highlighted = safeHighlight(str, lang)
        return `<pre class="hljs"><code>${highlighted}</code></pre>`
      }
    })

    // 配置 markdown-it-anchor 插件，直接使用标题文本作为 ID
    this.md.use(MarkdownItAnchor, {
      permalink: false,
      slugify: (s) => s.trim()
    })

    // 配置 tip 容器，使用橙色块风格
    this.md.use(MarkdownItContainer, 'tip', {
      validate: function(params) {
        return params.trim().match(/^tip\s*(.*)$/)
      },
      render: function (tokens, idx) {
        const m = tokens[idx].info.trim().match(/^tip\s*(.*)$/)
        if (tokens[idx].nesting === 1) {
          // 开始标签
          const title = m && m[1] ? m[1] : 'TIP'
          return `<div class="custom-block tip"><p class="custom-block-title">${title}</p>\n`
        } else {
          // 结束标签
          return '</div>\n'
        }
      }
    })

    this.loadDoc('README.md')
    this.initExpandedKeys()
    this.preloadAllDocs()
    this.registerDocShortcuts()
  },
  activated() {
    this.registerDocShortcuts()
  },
  deactivated() {
    this.exitDocKbdRegion()
    if (this.$shortcut) this.$shortcut.unregisterPage(DOC_KBD_SCOPE)
  },
  methods: {
    registerDocShortcuts() {
      if (!this.$shortcut) return
      this.$shortcut.registerPage(DOC_KBD_SCOPE, [
        { key: 'query', defaultLetter: 'S', run: () => this.shortcutFocusQuery() },
        { key: 'back', defaultLetter: 'B', run: () => this.handleBack() },
        { key: 'treeNav', defaultLetter: 'L', run: () => this.shortcutTreeNav() },
        { key: 'outlineNav', defaultLetter: 'D', run: () => this.shortcutOutlineNav() },
        { key: 'print', defaultLetter: 'P', run: () => this.printCurrentDoc() }
      ])
    },
    getPageActionHintContainer() {
      return this.$el
    },
    getPageActionHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${DOC_KBD_SCOPE}.${key}`, def)
      return [
        {
          key: 'query',
          letter: L('query', 'S'),
          badgeSelector: '.doc-hint-search .el-input__inner',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.shortcutFocusQuery()
        },
        {
          key: 'back',
          letter: L('back', 'B'),
          badgeSelector: '.doc-hint-back .el-page-header__left',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.handleBack()
        },
        {
          key: 'treeNav',
          letter: L('treeNav', 'L'),
          badgeSelector: '.doc-hint-tree',
          floatOffset: { placement: 'center-left-inset', outset: 8, dx: 4 },
          run: () => this.shortcutTreeNav()
        },
        {
          key: 'outlineNav',
          letter: L('outlineNav', 'D'),
          badgeSelector: '.doc-hint-outline-title',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.shortcutOutlineNav(),
          visible: () => this.outlineItems.length >= 2
        },
        {
          key: 'print',
          letter: L('print', 'P'),
          badgeSelector: '.doc-hint-print',
          floatOffset: { placement: 'center-right', outset: 4, dx: 2 },
          run: () => this.printCurrentDoc(),
          visible: () => this.outlineItems.length >= 2
        }
      ]
    },
    getPageActionHintScrollRoots() {
      const tree = this.$el && this.$el.querySelector('.doc-tree')
      const main = this.$refs.docScroll
      return [tree, main].filter(Boolean)
    },
    shortcutFocusQuery() {
      this.exitDocKbdRegion()
      const input = this.$el && this.$el.querySelector('.doc-hint-search input')
      if (input && typeof input.focus === 'function') {
        input.focus()
        if (typeof input.select === 'function') input.select()
      }
    },
    shortcutTreeNav() {
      const leaves = flattenDocTreeLeaves(this.filteredDocs)
      if (!leaves.length) return
      let idx = findDocLeafIndex(leaves, this.currentDoc)
      if (idx < 0) idx = 0
      this.docKbdTreeIndex = idx
      this.enterDocKbdRegion('tree')
    },
    shortcutOutlineNav() {
      if (this.outlineItems.length < 2) return
      let idx = this.outlineItems.findIndex((item) => item.id === this.activeOutlineId)
      if (idx < 0) idx = 0
      this.docKbdOutlineIndex = idx
      this.enterDocKbdRegion('outline')
    },
    enterDocKbdRegion(region) {
      this.docKbdRegion = region
      this.syncDocKbdRegionHighlight()
      if (!this.$_docKbdKeydown) {
        this.$_docKbdKeydown = (e) => this.onDocKbdKeydown(e)
        document.addEventListener('keydown', this.$_docKbdKeydown, true)
      }
    },
    exitDocKbdRegion() {
      this.docKbdRegion = ''
      this.docKbdTreeIndex = -1
      this.docKbdOutlineIndex = -1
      if (this.$_docKbdKeydown) {
        document.removeEventListener('keydown', this.$_docKbdKeydown, true)
        this.$_docKbdKeydown = null
      }
    },
    onDocKbdKeydown(e) {
      if (!this.docKbdRegion || e.isComposing) return
      if (e.metaKey || e.ctrlKey || e.altKey) return
      const key = e.key
      if (key === 'Escape') {
        e.preventDefault()
        this.exitDocKbdRegion()
        return
      }
      if (key === 'ArrowUp' || key === 'ArrowDown') {
        e.preventDefault()
        const delta = key === 'ArrowUp' ? -1 : 1
        if (this.docKbdRegion === 'tree') {
          const leaves = flattenDocTreeLeaves(this.filteredDocs)
          this.docKbdTreeIndex = clampDocIndex(this.docKbdTreeIndex + delta, leaves.length)
          this.syncDocKbdRegionHighlight()
        } else if (this.docKbdRegion === 'outline') {
          this.docKbdOutlineIndex = clampDocIndex(
            this.docKbdOutlineIndex + delta,
            this.outlineItems.length
          )
          this.syncDocKbdRegionHighlight()
        }
        return
      }
      if (key === 'Enter') {
        e.preventDefault()
        this.activateDocKbdRegion()
      }
    },
    syncDocKbdRegionHighlight() {
      if (this.docKbdRegion === 'tree') {
        const leaves = flattenDocTreeLeaves(this.filteredDocs)
        const entry = leaves[this.docKbdTreeIndex]
        const tree = this.$refs.docTree
        if (!entry || !tree) return
        tree.setCurrentKey(entry.data.label)
        this.$nextTick(() => {
          const node = tree.getNode(entry.data.label)
          if (node && node.data && node.data.path) {
            this.expandNodeParents(node.data.path)
          }
          const currentEl = tree.$el && tree.$el.querySelector('.el-tree-node.is-current')
          if (currentEl && typeof currentEl.scrollIntoView === 'function') {
            currentEl.scrollIntoView({ block: 'nearest' })
          }
        })
      } else if (this.docKbdRegion === 'outline') {
        const item = this.outlineItems[this.docKbdOutlineIndex]
        if (item) {
          this.activeOutlineId = item.id
        }
      }
    },
    activateDocKbdRegion() {
      if (this.docKbdRegion === 'tree') {
        const leaves = flattenDocTreeLeaves(this.filteredDocs)
        const entry = leaves[this.docKbdTreeIndex]
        const tree = this.$refs.docTree
        if (!entry || !tree) return
        const node = tree.getNode(entry.data.label)
        if (node) {
          this.handleNodeClick(entry.data, node)
        }
        this.exitDocKbdRegion()
      } else if (this.docKbdRegion === 'outline') {
        const item = this.outlineItems[this.docKbdOutlineIndex]
        if (item) {
          this.scrollToOutlineHeading(item.id)
        }
        this.exitDocKbdRegion()
      }
    },
    highlightText(text) {
      if (!this.searchText) {
        return text
      }
      const keywords = this.searchText.trim().split(/\s+/).filter(k => k)
      let result = text
      keywords.forEach(keyword => {
        if (keyword) {
          const regex = new RegExp(`(${this.escapeRegExp(keyword)})`, 'gi')
          result = result.replace(regex, '<mark>$1</mark>')
        }
      })
      return result
    },
    filterDocsRecursive(docs, keywords) {
      const result = []
      docs.forEach(doc => {
        // 检查当前节点标题是否匹配所有关键词
        const labelLower = doc.label.toLowerCase()
        const titleMatches = keywords.every(keyword => labelLower.includes(keyword))

        // 检查文档内容是否匹配（如果有path）
        let contentMatches = false
        if (doc.path && this.docContents[doc.path]) {
          const contentLower = this.docContents[doc.path].toLowerCase()
          contentMatches = keywords.every(keyword => contentLower.includes(keyword))
        }

        // 递归过滤子节点
        let filteredChildren = []
        if (doc.children && doc.children.length > 0) {
          filteredChildren = this.filterDocsRecursive(doc.children, keywords)
        }

        // 如果当前节点标题匹配、内容匹配或有匹配的子节点，则保留
        if (titleMatches || contentMatches || filteredChildren.length > 0) {
          result.push({
            ...doc,
            children: filteredChildren.length > 0 ? filteredChildren : doc.children
          })
        }
      })
      return result
    },
    handleBack() {
      this.exitDocKbdRegion()
      // 如果有文档浏览历史，返回上一个文档
      if (this.docHistory.length > 0) {
        const previousDoc = this.docHistory.pop()
        this.currentDocTitle = previousDoc.title
        this.loadDoc(previousDoc.path, false) // false 表示不添加到历史记录
      } else {
        // 如果没有文档历史，按照浏览器记录返回
        this.$router.go(-1)
      }
    },
    /** 与 axios 一致，为 fetch 附加 Token 等头信息（否则 /docs 会 401） */
    docFetchHeaders(urlPath) {
      const headers = {}
      setHeader(urlPath, headers)
      return headers
    },
    initExpandedKeys() {
      // 收集所有一级目录的key（有children的节点）
      const keys = []
      this.docs.forEach(doc => {
        if (doc.children && doc.children.length > 0) {
          // 使用label作为key，因为一级目录没有path
          keys.push(doc.label)
        }
      })
      this.expandedKeys = keys
    },
    async preloadAllDocs() {
      // 递归收集所有文档路径
      const collectPaths = (docs) => {
        const paths = []
        docs.forEach(doc => {
          if (doc.path) {
            paths.push(doc.path)
          }
          if (doc.children && doc.children.length > 0) {
            paths.push(...collectPaths(doc.children))
          }
        })
        return paths
      }

      const allPaths = collectPaths(this.docs)

      // 预加载所有文档内容
      for (const path of allPaths) {
        try {
          const urlPath = `/docs/${path}`
          const response = await fetch(urlPath, { headers: this.docFetchHeaders(urlPath) })
          if (response.ok) {
            const content = await response.text()
            this.docContents[path] = content
          }
        } catch (error) {
          console.error(`Failed to load ${path}:`, error)
        }
      }
    },
    handleSearch() {
      // 搜索功能已通过 computed 实现
    },
    handleNodeClick(data, node) {
      this.exitDocKbdRegion()
      // 只有叶子节点（有 path 属性）才加载文档
      if (data.path) {
        // 将当前文档添加到历史记录
        if (this.currentDoc && this.currentDoc !== data.path) {
          this.docHistory.push({
            path: this.currentDoc,
            title: this.currentDocTitle
          })
        }
        this.loadDoc(data.path)
        // 构建面包屑路径
        this.currentDocTitle = this.buildBreadcrumb(node)
      }
    },
    buildBreadcrumb(node) {
      // 从当前节点向上遍历，构建面包屑
      const breadcrumbs = []
      let currentNode = node

      while (currentNode) {
        breadcrumbs.unshift(currentNode.label)
        currentNode = currentNode.parent
      }

      // 第一级前不要有 /，用 / 连接面包屑
      return breadcrumbs.join(' / ')
    },
    /** 将「当前文档路径 + 相对链接」解析为 docs 树中的标准 path（处理 ../、./） */
    resolveDocRelativePath(currentDoc, relativePath) {
      const rel = (relativePath || '').replace(/^\.\//, '')
      const dir = currentDoc && currentDoc.includes('/')
        ? currentDoc.substring(0, currentDoc.lastIndexOf('/'))
        : ''
      const segments = []
      if (dir) {
        segments.push(...dir.split('/').filter(Boolean))
      }
      segments.push(...rel.split('/').filter(Boolean))
      const out = []
      for (const s of segments) {
        if (s === '..') {
          if (out.length) out.pop()
        } else if (s !== '.' && s !== '') {
          out.push(s)
        }
      }
      return out.join('/')
    },
    /** 仅根据 path 在 docs 树中还原面包屑（用于 Markdown 内链跳转，无 Tree 节点上下文时） */
    buildBreadcrumbByPath(docPath) {
      const findTrail = (nodes, targetPath, trail = []) => {
        for (const node of nodes) {
          const next = [...trail, node.label]
          if (node.path === targetPath) {
            return next
          }
          if (node.children && node.children.length) {
            const hit = findTrail(node.children, targetPath, next)
            if (hit) return hit
          }
        }
        return null
      }
      const labels = findTrail(this.docs, docPath)
      return labels ? labels.join(' / ') : ''
    },
    async loadDoc(docPath, addToHistory = true) {
      try {
        const urlPath = `/docs/${docPath}`
        const response = await fetch(urlPath, { headers: this.docFetchHeaders(urlPath) })
        if (!response.ok) {
          throw new Error('文档加载失败')
        }
        const markdown = await response.text()
        const isApiDoc = /^api\//.test(docPath)
        let md = markdown
        let tabOptions = {}
        if (isApiDoc) {
          const urls = resolveApiDocBaseUrls()
          tabOptions = { baseUrl: urls.baseUrl, apiKey: '' }
          md = applyApiDocPlaceholders(markdown, urls)
        }
        const processed = preprocessCodeTabs(md, tabOptions)
        let html = this.md.render(processed)
        // 修复图片路径：将相对路径转换为绝对路径
        html = html.replace(/src="(\.\.\/)+images\//g, 'src="/docs/images/')
        html = html.replace(/src="\.\/images\//g, 'src="/docs/images/')
        html = html.replace(/src="images\//g, 'src="/docs/images/')

        // 如果有搜索关键词，高亮显示
        if (this.searchText) {
          const keywords = this.searchText.trim().split(/\s+/).filter(k => k)
          keywords.forEach(keyword => {
            if (keyword) {
              // 使用正则表达式进行全局替换，忽略大小写
              const regex = new RegExp(`(${this.escapeRegExp(keyword)})`, 'gi')
              html = html.replace(regex, '<mark>$1</mark>')
            }
          })
        }

        this.renderedContent = html
        this.currentDoc = docPath

        const crumb = this.buildBreadcrumbByPath(docPath)
        if (crumb) {
          this.currentDocTitle = crumb
        }

        // 设置左侧目录树的当前激活节点，并展开其父节点
        this.$nextTick(() => {
          if (this.$refs.docTree) {
            // 先展开包含该节点的所有父节点
            this.expandNodeParents(docPath)
            // 然后通过label找到并激活该节点
            this.setCurrentNodeByPath(docPath)
          }
        })

        // 等待DOM更新后，为文档内的链接添加点击事件处理，并滚动到顶部
        this.$nextTick(() => {
          this.handleDocLinks()
          initCodeTabs(this.$refs.markdownBody)
          this.buildOutline()
          const scrollEl = this.$refs.docScroll
          if (scrollEl) {
            scrollEl.scrollTop = 0
          }
          this.updateActiveOutline()
        })
      } catch (error) {
        this.$message.error('加载文档失败: ' + error.message)
        this.renderedContent = '<p>文档加载失败，请稍后重试。</p>'
      }
    },
    setCurrentNodeByPath(docPath) {
      // 通过path查找节点的label，然后设置为当前节点
      const findLabelByPath = (nodes, targetPath) => {
        for (let node of nodes) {
          if (node.path === targetPath) {
            return node.label
          }
          if (node.children && node.children.length > 0) {
            const result = findLabelByPath(node.children, targetPath)
            if (result) return result
          }
        }
        return null
      }

      const label = findLabelByPath(this.docs, docPath)
      if (label && this.$refs.docTree) {
        this.$refs.docTree.setCurrentKey(label)
      }
    },
    expandNodeParents(docPath) {
      // 递归查找节点并收集所有父节点的label（因为父节点没有path）
      const findNodeAndParents = (nodes, targetPath, parents = []) => {
        for (let node of nodes) {
          if (node.path === targetPath) {
            // 找到目标节点，返回所有父节点的label
            return parents
          }
          if (node.children && node.children.length > 0) {
            // 将当前节点的label加入父节点列表
            const newParents = [...parents, node.label]
            const result = findNodeAndParents(node.children, targetPath, newParents)
            if (result) {
              return result
            }
          }
        }
        return null
      }

      // 在原始docs中查找所有父节点
      const parentLabels = findNodeAndParents(this.docs, docPath)

      // 展开所有父节点
      if (parentLabels && this.$refs.docTree) {
        // 先将所有父节点label添加到expandedKeys中
        parentLabels.forEach(label => {
          if (!this.expandedKeys.includes(label)) {
            this.expandedKeys.push(label)
          }
        })

        // 然后通过tree组件展开节点
        this.$nextTick(() => {
          parentLabels.forEach(label => {
            const node = this.$refs.docTree.getNode(label)
            if (node && !node.expanded) {
              node.expand()
            }
          })
        })
      }
    },
    escapeRegExp(string) {
      // 转义正则表达式特殊字符
      return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    },
    buildOutline() {
      const root = this.$refs.markdownBody
      if (!root) {
        this.outlineItems = []
        this.activeOutlineId = ''
        return
      }
      const headings = root.querySelectorAll('h2, h3')
      const items = []
      headings.forEach((heading) => {
        const text = (heading.textContent || '').trim()
        if (!text) return
        let id = heading.id
        if (!id) {
          id = text
          heading.id = id
        }
        const level = parseInt(heading.tagName.charAt(1), 10)
        items.push({ id, text, level })
      })
      this.outlineItems = items
      this.activeOutlineId = items.length ? items[0].id : ''
    },
    onDocContentScroll() {
      if (!this.outlineItems.length) return
      if (this.outlineScrollRaf) return
      this.outlineScrollRaf = requestAnimationFrame(() => {
        this.outlineScrollRaf = null
        this.updateActiveOutline()
      })
    },
    updateActiveOutline() {
      const container = this.$refs.docScroll
      if (!container || !this.outlineItems.length) return

      const containerTop = container.getBoundingClientRect().top
      const threshold = 72
      let active = this.outlineItems[0].id

      for (const item of this.outlineItems) {
        const el = document.getElementById(item.id)
        if (!el) continue
        const offset = el.getBoundingClientRect().top - containerTop
        if (offset <= threshold) {
          active = item.id
        }
      }
      this.activeOutlineId = active
    },
    scrollToOutlineHeading(id) {
      const container = this.$refs.docScroll
      const el = document.getElementById(id)
      if (!container || !el) return

      const containerTop = container.getBoundingClientRect().top
      const elTop = el.getBoundingClientRect().top
      const scrollTop = container.scrollTop + (elTop - containerTop) - 16
      container.scrollTo({ top: Math.max(0, scrollTop), behavior: 'smooth' })
      this.activeOutlineId = id
    },
    printCurrentDoc() {
      const bodyEl = this.$refs.markdownBody
      if (!bodyEl) return

      const titlePart = (this.currentDocTitle || '').split(' / ').pop()
      const title = titlePart || this.$t('system-doc')

      const iframe = document.createElement('iframe')
      iframe.setAttribute('aria-hidden', 'true')
      Object.assign(iframe.style, {
        position: 'fixed',
        width: '0',
        height: '0',
        border: '0',
        visibility: 'hidden'
      })
      document.body.appendChild(iframe)

      const win = iframe.contentWindow
      const doc = win.document
      doc.open()
      doc.write('<!DOCTYPE html><html><head><meta charset="utf-8"></head><body></body></html>')
      doc.close()
      doc.title = title

      const printStyle = doc.createElement('style')
      printStyle.textContent = `
        @page { margin: 16mm; }
        body { margin: 0; padding: 0; background: #fff; }
        .doc-print-body { max-width: none !important; margin: 0 !important; }
        img { max-width: 100%; }
        pre { white-space: pre-wrap; word-break: break-word; }
      `
      doc.head.appendChild(printStyle)

      const styleNeedle = ['markdown-body', '.hljs', 'custom-block', 'doc-code-tabs']
      document.querySelectorAll('style').forEach((node) => {
        const text = node.textContent || ''
        if (!styleNeedle.some((key) => text.includes(key))) return
        const clone = doc.createElement('style')
        // 去掉 Vue scoped 的 data-v 选择器，以便在独立打印文档中生效
        clone.textContent = text.replace(/\s*\[data-v-[a-f0-9]+\]/g, '')
        doc.head.appendChild(clone)
      })

      const container = doc.createElement('div')
      container.className = 'markdown-body doc-print-body'
      container.innerHTML = bodyEl.innerHTML
      doc.body.appendChild(container)

      const cleanup = () => {
        if (iframe.parentNode) {
          iframe.parentNode.removeChild(iframe)
        }
      }

      const doPrint = () => {
        try {
          win.focus()
          win.print()
        } finally {
          setTimeout(cleanup, 500)
        }
      }

      win.addEventListener('afterprint', cleanup, { once: true })
      setTimeout(doPrint, 300)
    },
    checkPermission(route) {
      // 检查用户是否有访问该路由的权限
      const permissions = this.$store.getters && this.$store.getters.permissions
      const all_permission = "*:*:*"

      // 如果用户有超级权限，允许访问所有路由
      if (permissions && permissions.includes(all_permission)) {
        return true
      }

      // 从 store 中获取所有路由
      const routes = this.$store.getters && this.$store.getters.sidebarRouters

      if (!routes || routes.length === 0) {
        return false // 如果没有路由信息，默认不允许访问
      }

      // 递归查找路由及其权限
      const findRoute = (routes, path, parentPath = '') => {
        for (let route of routes) {
          // 构建完整路径
          let fullPath = route.path
          if (parentPath && !route.path.startsWith('/')) {
            fullPath = parentPath + '/' + route.path
          }

          // 匹配完整路径
          if (fullPath === path || route.path === path) {
            return route
          }

          // 递归查找子路由
          if (route.children && route.children.length > 0) {
            const found = findRoute(route.children, path, fullPath)
            if (found) return found
          }
        }
        return null
      }

      const targetRoute = findRoute(routes, route)

      // 如果找不到路由，默认不允许访问
      if (!targetRoute) {
        return false
      }

      // 检查路由的 perms 字段（后端返回的权限字段）
      const requiredPermissions = targetRoute.perms || (targetRoute.meta && targetRoute.meta.permissions)

      // 如果路由没有设置权限要求，允许访问
      if (!requiredPermissions || requiredPermissions.length === 0) {
        return true
      }

      // 检查用户是否有所需权限
      return permissions && permissions.some(permission => {
        return requiredPermissions.includes(permission)
      })
    },
    handleDocLinks() {
      const content = document.querySelector('.markdown-body')
      if (!content) return

      const links = content.querySelectorAll('a')
      links.forEach(link => {
        const href = link.getAttribute('href')
        if (!href) return

        // 移除旧的事件监听器（如果存在）
        const oldHandler = link._clickHandler
        if (oldHandler) {
          link.removeEventListener('click', oldHandler)
        }

        // 处理页面内锚点链接（以 # 开头，但不包含路径）
        if (href.startsWith('#') && !href.includes('/')) {
          const handler = (e) => {
            e.preventDefault()
            e.stopPropagation()

            // 获取锚点 ID（去掉 # 号）并解码
            const targetId = decodeURIComponent(href.substring(1))
            const target = document.getElementById(targetId)

            if (target) {
              // 滚动到目标位置
              target.scrollIntoView({ behavior: 'smooth', block: 'start' })
            }
          }
          link.addEventListener('click', handler)
          link._clickHandler = handler
          return
        }

        // 处理相对路径的.md文件链接
        if (href.endsWith('.md') || href.includes('.md#')) {
          const handler = (e) => {
            e.preventDefault()

            // 解析链接路径和锚点
            const [path, hash] = href.split('#')

            // 计算完整路径（解析 ./、../，与侧栏 path 一致）
            let fullPath = path
            if (path.startsWith('/')) {
              fullPath = path.replace(/^\/?docs\//, '')
            } else {
              fullPath = this.resolveDocRelativePath(this.currentDoc, path)
            }

            // 将当前文档添加到历史记录
            if (this.currentDoc && this.currentDoc !== fullPath) {
              this.docHistory.push({
                path: this.currentDoc,
                title: this.currentDocTitle
              })
            }

            // 加载文档
            this.loadDoc(fullPath)

            // 如果有锚点，等待文档加载后滚动到对应位置
            if (hash) {
              this.$nextTick(() => {
                const target = document.getElementById(hash)
                if (target) {
                  target.scrollIntoView({ behavior: 'smooth' })
                }
              })
            }
          }
          link.addEventListener('click', handler)
          link._clickHandler = handler
        } else if (href.startsWith('/')) {
          // 处理系统内部路由链接
          // 检查用户是否有访问该路由的权限
          const hasPermission = this.checkPermission(href)

          if (!hasPermission) {
            // 如果没有权限，移除链接，只保留文本
            const text = link.textContent
            const textNode = document.createTextNode(text)
            link.parentNode.replaceChild(textNode, link)
            return
          }

          const handler = (e) => {
            e.preventDefault()
            e.stopPropagation()
            this.$router.push(href).catch(err => {
              console.error('Navigation error:', err)
              this.$message.error('页面跳转失败: ' + err.message)
            })
          }
          link.addEventListener('click', handler)
          link._clickHandler = handler
        }
      })
    }
  }
}
</script>

<style scoped lang="scss">
@import '~@/assets/styles/markdown.css';

.doc-viewer-wrapper {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: calc(100vh - 50px);
  background-color: var(--table-bg, #fff);

  .doc-header {
    padding: 20px 24px 0;
    background-color: var(--table-bg, #fff);

    > .el-page-header {
      background-color: transparent;
    }
  }

  .doc-viewer {
    display: flex;
    flex: 1;
    width: 100%;
    overflow: hidden;
    background-color: var(--table-bg, #fff);

    .doc-sidebar {
      width: 280px;
      box-sizing: border-box;
      padding-left: 24px;
      border-right: 1px solid #e8e8e8;
      background-color: #fff;
      display: flex;
      flex-direction: column;

      .search-box {
        padding: 16px 16px 16px 0;
        border-bottom: 1px solid #e8e8e8;
      }

      .doc-tree {
        flex: 1;
        overflow-y: auto;
        padding: 16px 8px 16px 4px;

        ::v-deep .el-tree-node__content {
          border-radius: 3px;
          margin-right: 4px;
        }

        ::v-deep .el-tree-node.is-current > .el-tree-node__content {
          background-color: #ecf5ff;
          color: #409eff;
        }

        ::v-deep .el-tree-node__content:hover {
          border-radius: 3px;
        }

        .custom-tree-node {
          display: flex;
          align-items: center;

          i, .svg-icon {
            margin-right: 8px;
            font-size: 16px;
            color: #409eff;
          }

          .svg-icon {
            width: 16px;
            height: 16px;
          }

          ::v-deep mark {
            background-color: #ffeb3b;
            padding: 2px 4px;
            border-radius: 2px;
            font-weight: 500;
          }
        }
      }
    }

    .doc-main {
      display: flex;
      flex: 1;
      min-width: 0;
      align-items: flex-start;
      overflow-x: hidden;
      overflow-y: auto;
      background-color: var(--table-bg, #fff);
    }

    .doc-content {
      flex: 1;
      min-width: 0;
      padding: 0 48px 24px;
      position: relative;
      background-color: var(--table-bg, #fff);

      .markdown-body {
        max-width: 1000px;
        margin: 0 auto;

        ::v-deep mark {
          background-color: #ffeb3b;
          padding: 2px 4px;
          border-radius: 2px;
          font-weight: 500;
        }
      }

      .backtop-button {
        width: 40px;
        height: 40px;
        background-color: #409eff;
        color: #fff;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        cursor: pointer;
        box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
        transition: all 0.3s;

        &:hover {
          background-color: #66b1ff;
        }
      }
    }
  }
}

/* markdown-body 样式已在 @import 的 CSS 文件中定义 */
</style>
