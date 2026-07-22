<template>
  <el-breadcrumb class="app-breadcrumb" separator="/">
    <transition-group name="breadcrumb">
      <el-breadcrumb-item v-for="(item, index) in levelList" :key="crumbKey(item, index)">
        <span v-if="item.redirect === 'noRedirect' || index == levelList.length - 1" class="no-redirect">{{ titleName(item) }}</span>
        <a v-else @click.prevent="handleLink(item)">{{ titleName(item) }}</a>
      </el-breadcrumb-item>
    </transition-group>
  </el-breadcrumb>
</template>

<script>
export default {
  data() {
    return {
      levelList: null
    }
  },
  watch: {
    $route(route) {
      // if you go to the redirect page, do not update the breadcrumbs
      if (route.path.startsWith('/redirect/')) {
        return
      }
      this.getBreadcrumb()
    }
  },
  computed: {
    titleName: function (){
      return function (menu) {
        return menu.meta.titleI18nKey?this.$i18n.t(menu.meta.titleI18nKey):menu.meta.title;
      }
    }
  },
  created() {
    this.getBreadcrumb()
  },
  methods: {
    /** TransitionGroup 要求稳定唯一 key；breadcrumbParents 可能只有 name、无 path */
    crumbKey(item, index) {
      if (!item) return `crumb-${index}`
      const metaKey = item.meta && (item.meta.titleI18nKey || item.meta.title)
      return `${index}-${item.path || item.name || metaKey || 'crumb'}`
    },
    getBreadcrumb() {
      // only show routes with meta.title / titleI18nKey
      let matched = this.$route.matched.filter(item => item.meta && (item.meta.title || item.meta.titleI18nKey))
      const leaf = matched.length ? matched[matched.length - 1] : null
      // 隐藏详情页等可通过 meta.breadcrumbParents 补齐上级（如：项目管理 / 源码管理 / 源码库）
      if (leaf && Array.isArray(leaf.meta.breadcrumbParents) && leaf.meta.breadcrumbParents.length) {
        matched = leaf.meta.breadcrumbParents.concat(matched)
      }
      const first = matched[0]

      if (!this.isDashboard(first)) {
        matched = [{ path: '/index', meta: { titleI18nKey:'home', title: this.$i18n.t('home') }}].concat(matched)
      }

      this.levelList = matched.filter(item => item.meta && (item.meta.title || item.meta.titleI18nKey) && item.meta.breadcrumb !== false)
    },
    isDashboard(route) {
      const name = route && route.name
      if (!name) {
        return false
      }
      return name.trim() === 'Index'
    },
    handleLink(item) {
      const { redirect, path, name } = item
      if (redirect === 'noRedirect') {
        return
      }
      if (redirect) {
        this.$router.push(redirect)
        return
      }
      if (name) {
        const params = { ...(this.$route.params || {}) }
        // repositoryId is attached by beforeEnter, not a path param — never push it
        delete params.repositoryId
        // Drop deep-link params when jumping to a parent named route
        if (name === 'GitRepoBrowse' || name === 'GitRepoList') {
          delete params.commitId
          delete params.releaseId
          delete params.number
        }
        if (
          name === 'GitRepoSettings'
          || name === 'GitRepoSettingsGeneral'
          || name === 'GitRepoSettingsDanger'
          || name === 'GitRepoSettingsMembers'
          || name === 'GitRepoSettingsWebhooks'
        ) {
          delete params.commitId
          delete params.releaseId
          delete params.number
        }
        if (name === 'GitRepoList') {
          delete params.slug
        }
        this.$router.push({ name, params }).catch(() => {})
        return
      }
      if (path) {
        this.$router.push(path)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.app-breadcrumb.el-breadcrumb {
  display: inline-block;
  font-size: 14px;
  line-height: 50px;
  margin-left: 8px;

  .no-redirect {
    color: #97a8be;
    cursor: text;
  }
}
</style>
