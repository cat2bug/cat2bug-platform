<template>
  <div class="doc-viewer-wrapper">
    <div class="doc-header">
      <el-page-header @back="handleBack">
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
            :placeholder="$t('search')"
            prefix-icon="el-icon-search"
            clearable
            @input="handleSearch"
          />
        </div>
        <div class="doc-tree">
          <el-tree
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
      <div class="doc-content" ref="docContent">
        <div class="markdown-body" v-html="renderedContent"></div>
        <el-backtop target=".doc-content" :visibility-height="300" :right="40" :bottom="40">
          <div class="backtop-button">
            <i class="el-icon-top"></i>
          </div>
        </el-backtop>
      </div>
    </div>
  </div>
</template>

<script>
import MarkdownIt from 'markdown-it'
import MarkdownItContainer from 'markdown-it-container'
import MarkdownItAnchor from 'markdown-it-anchor'

export default {
  name: 'DocViewer',
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
              label: '团队管理',
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
                  path: 'user-guide/current-project/case.md',
                  icon: 'case'
                },
                {
                  label: '测试计划',
                  path: 'user-guide/current-project/plan.md',
                  icon: 'date'
                },
                {
                  label: '缺陷管理',
                  path: 'user-guide/current-project/defect.md',
                  icon: 'bug'
                },
                {
                  label: '交付物管理',
                  path: 'user-guide/current-project/module.md',
                  icon: 'cascader'
                },
                {
                  label: '报告管理',
                  path: 'user-guide/current-project/report.md',
                  icon: 'chart'
                },
                {
                  label: '文档管理',
                  path: 'user-guide/current-project/document.md',
                  icon: 'education'
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
                          label: 'AI大模型',
                          path: 'user-guide/current-project/project-setting/project-ai/ai-model.md',
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
    }
  },
  mounted() {
    this.md = new MarkdownIt()

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
  },
  methods: {
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
          const response = await fetch(`/docs/${path}`)
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
    async loadDoc(docPath, addToHistory = true) {
      try {
        const response = await fetch(`/docs/${docPath}`)
        if (!response.ok) {
          throw new Error('文档加载失败')
        }
        const markdown = await response.text()
        let html = this.md.render(markdown)
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

        // 等待DOM更新后，为文档内的链接添加点击事件处理，并滚动到顶部
        this.$nextTick(() => {
          this.handleDocLinks()
          // 滚动到文档内容区域顶部
          const docContent = document.querySelector('.doc-content')
          if (docContent) {
            docContent.scrollTop = 0
          }
        })
      } catch (error) {
        this.$message.error('加载文档失败: ' + error.message)
        this.renderedContent = '<p>文档加载失败，请稍后重试。</p>'
      }
    },
    escapeRegExp(string) {
      // 转义正则表达式特殊字符
      return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
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

            // 计算完整路径
            let fullPath = path
            if (path.startsWith('./')) {
              // 相对于当前文档的路径
              const currentDir = this.currentDoc.substring(0, this.currentDoc.lastIndexOf('/'))
              fullPath = currentDir ? `${currentDir}/${path.substring(2)}` : path.substring(2)
            } else if (!path.startsWith('/')) {
              // 相对路径，需要基于当前文档目录
              const currentDir = this.currentDoc.substring(0, this.currentDoc.lastIndexOf('/'))
              fullPath = currentDir ? `${currentDir}/${path}` : path
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
  height: calc(100vh - 50px);

  .doc-header {
    padding: 10px 24px 0 24px;
    >.el-page-header {
      margin: 0;
      padding-bottom: 10px;
    }
  }

  .doc-viewer {
    display: flex;
    flex: 1;
    overflow: hidden;

    .doc-sidebar {
      width: 280px;
      border-right: 1px solid #e8e8e8;
      display: flex;
      flex-direction: column;

      .search-box {
        padding: 16px;
        border-bottom: 1px solid #e8e8e8;
      }

      .doc-tree {
        flex: 1;
        overflow-y: auto;
        padding: 16px 8px;

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

    .doc-content {
      flex: 1;
      overflow-y: auto;
      padding: 0px 48px 24px 48px;
      position: relative;

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
