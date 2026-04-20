<template>
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
          node-key="path"
          :default-expanded-keys="expandedKeys"
          :highlight-current="true"
          @node-click="handleNodeClick"
        >
          <span class="custom-tree-node" slot-scope="{ node, data }">
            <svg-icon v-if="data.icon && !data.icon.startsWith('el-icon')" :icon-class="data.icon" />
            <i v-else :class="data.icon"></i>
            <span>{{ node.label }}</span>
          </span>
        </el-tree>
      </div>
    </div>
    <div class="doc-content">
      <div class="doc-header">
        <h2>{{ currentDocTitle }}</h2>
      </div>
      <div class="markdown-body" v-html="renderedContent"></div>
    </div>
  </div>
</template>

<script>
import MarkdownIt from 'markdown-it'

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
      docs: [
        {
          label: '系统文档',
          path: 'README.md',
          icon: 'el-icon-document'
        },
        {
          label: '快速开始',
          path: 'quick-start.md',
          icon: 'el-icon-video-play'
        },
        {
          label: '用户指南',
          icon: 'el-icon-reading',
          children: [
            {
              label: '团队管理',
              path: 'team-manage.md',
              icon: 'el-icon-office-building'
            },
            {
              label: '项目管理',
              path: 'project-manage.md',
              icon: 'el-icon-folder-opened'
            },
            {
              label: '当前项目',
              icon: 'el-icon-folder',
              children: [
                {
                  label: '缺陷管理',
                  path: 'defect.md',
                  icon: 'bug'
                },
                {
                  label: '测试用例',
                  path: 'case.md',
                  icon: 'case'
                },
                {
                  label: '测试计划',
                  path: 'plan.md',
                  icon: 'date'
                },
                {
                  label: '交付物管理',
                  path: 'module.md',
                  icon: 'cascader'
                },
                {
                  label: '报告管理',
                  path: 'report.md',
                  icon: 'chart'
                },
                {
                  label: '文档管理',
                  path: 'document.md',
                  icon: 'education'
                },
                {
                  label: '团队设置',
                  path: 'team-setting.md',
                  icon: 'el-icon-setting'
                }
              ]
            }
          ]
        },
        {
          label: '管理员指南',
          path: 'admin-guide.md',
          icon: 'el-icon-s-tools'
        },
        {
          label: 'API 文档',
          icon: 'el-icon-connection',
          children: [
            {
              label: 'API 介绍',
              path: 'api-intro.md',
              icon: 'el-icon-info'
            },
            {
              label: '交付物接口',
              path: 'api-deliverable.md',
              icon: 'el-icon-files'
            },
            {
              label: '测试用例接口',
              path: 'api-case.md',
              icon: 'case'
            },
            {
              label: '缺陷接口',
              path: 'api-defect.md',
              icon: 'bug'
            },
            {
              label: '报告接口',
              path: 'api-report-defect.md',
              icon: 'chart'
            },
            {
              label: '成员接口',
              path: 'api-member.md',
              icon: 'el-icon-user'
            },
            {
              label: '文件接口',
              path: 'api-file.md',
              icon: 'el-icon-upload'
            },
            {
              label: '项目接口',
              path: 'api-project.md',
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
      const keyword = this.searchText.toLowerCase()
      return this.docs.filter(doc =>
        doc.label.toLowerCase().includes(keyword)
      )
    }
  },
  mounted() {
    this.md = new MarkdownIt()
    this.loadDoc('README.md')
  },
  methods: {
    handleSearch() {
      // 搜索功能已通过 computed 实现
    },
    handleNodeClick(data) {
      // 只有叶子节点（有 path 属性）才加载文档
      if (data.path) {
        this.loadDoc(data.path)
        this.currentDocTitle = data.label
      }
    },
    async loadDoc(docPath) {
      try {
        const response = await fetch(`/docs/${docPath}`)
        if (!response.ok) {
          throw new Error('文档加载失败')
        }
        const markdown = await response.text()
        let html = this.md.render(markdown)
        // 修复图片路径：将相对路径转换为绝对路径
        html = html.replace(/src="\.\/img\//g, 'src="/docs/img/')
        html = html.replace(/src="img\//g, 'src="/docs/img/')
        this.renderedContent = html
        this.currentDoc = docPath
      } catch (error) {
        this.$message.error('加载文档失败: ' + error.message)
        this.renderedContent = '<p>文档加载失败，请稍后重试。</p>'
      }
    }
  }
}
</script>

<style scoped lang="scss">
@import '~@/assets/styles/markdown.css';

.doc-viewer {
  display: flex;
  height: calc(100vh - 84px);
  background: #fff;

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
      }
    }
  }

  .doc-content {
    flex: 1;
    overflow-y: auto;
    padding: 24px 48px;

    .doc-header {
      margin-bottom: 24px;
      padding-bottom: 16px;
      border-bottom: 1px solid #e8e8e8;

      h2 {
        margin: 0;
        font-size: 28px;
        font-weight: 600;
        color: #303133;
      }
    }
  }
}

/* markdown-body 样式已在 @import 的 CSS 文件中定义 */
</style>
