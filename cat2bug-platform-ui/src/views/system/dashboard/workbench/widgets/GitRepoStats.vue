<template>
  <div class="git-repo-stats" v-loading="loading">
    <div class="git-repo-stats__row">
      <button type="button" class="git-repo-stats__cell" @click="goCode">
        <span class="n">{{ overview.repoCount }}</span>
        <span class="l">{{ $t('git.repo.overview.repo') }}</span>
      </button>
      <button type="button" class="git-repo-stats__cell" @click="goCode">
        <span class="n">{{ overview.openPrCount }}</span>
        <span class="l">{{ $t('git.repo.overview.pr') }}</span>
      </button>
      <button type="button" class="git-repo-stats__cell" @click="goCode">
        <span class="n">{{ overview.collectCount }}</span>
        <span class="l">{{ $t('git.repo.overview.collect') }}</span>
      </button>
    </div>
    <div v-if="languages.length" class="git-repo-stats__langs">
      <span
        v-for="(lang, index) in languages.slice(0, 4)"
        :key="lang.name + '-' + index"
        class="lang-chip"
      >
        <i class="lang-dot" :style="{ background: langColor(lang, index) }" aria-hidden="true" />
        {{ lang.name }} {{ formatPercent(lang.percent) }}%
      </span>
    </div>
  </div>
</template>

<script>
import { getGitRepoProjectOverview } from '@/api/system/gitRepo'
import { getCurrentProjectId } from '@/utils/store-context'
import { buildProjectModulePath } from '@/utils/project-module-path'

/** 常见语言固定色；其余按序号轮换 */
const LANG_COLOR_MAP = {
  JavaScript: '#f1e05a',
  TypeScript: '#3178c6',
  Java: '#b07219',
  Python: '#3572A5',
  Vue: '#41b883',
  HTML: '#e34c26',
  CSS: '#563d7c',
  SCSS: '#c6538c',
  Go: '#00ADD8',
  Rust: '#dea584',
  Kotlin: '#A97BFF',
  Swift: '#F05138',
  C: '#555555',
  'C++': '#f34b7d',
  'C#': '#178600',
  PHP: '#4F5D95',
  Ruby: '#701516',
  Shell: '#89e051',
  Dart: '#00B4AB',
  Markdown: '#083fa1'
}
const LANG_FALLBACK = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#B37FEB']

export default {
  name: 'GitRepoStats',
  data() {
    return {
      loading: false,
      overview: { repoCount: 0, openPrCount: 0, collectCount: 0 },
      languages: []
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      const projectId = getCurrentProjectId()
      if (!projectId) return
      this.loading = true
      try {
        const res = await getGitRepoProjectOverview(projectId)
        const data = (res && res.data) || {}
        this.overview = {
          repoCount: Number(data.repoCount || 0),
          openPrCount: Number(data.openPrCount || 0),
          collectCount: Number(data.collectCount || 0)
        }
        this.languages = Array.isArray(data.languages) ? data.languages : []
      } catch (e) {
        this.overview = { repoCount: 0, openPrCount: 0, collectCount: 0 }
        this.languages = []
      } finally {
        this.loading = false
      }
    },
    langColor(lang, index) {
      const name = String(lang?.name || '').trim()
      if (name && LANG_COLOR_MAP[name]) return LANG_COLOR_MAP[name]
      // 忽略大小写匹配
      const hit = Object.keys(LANG_COLOR_MAP).find(k => k.toLowerCase() === name.toLowerCase())
      if (hit) return LANG_COLOR_MAP[hit]
      return LANG_FALLBACK[index % LANG_FALLBACK.length]
    },
    formatPercent(percent) {
      const n = Number(percent)
      if (!Number.isFinite(n)) return '0'
      return (Math.round(n * 10) / 10).toString()
    },
    goCode() {
      const path = buildProjectModulePath(this.$route.params.teamKey, this.$route.params.projectKey, 'code')
      if (path) this.$router.push(path)
    },
    refresh() {
      return this.load()
    }
  }
}
</script>

<style scoped lang="scss">
.git-repo-stats {
  display: flex;
  flex-direction: column;
  justify-content: center;
  flex: 1;
  min-height: 120px;
}
.git-repo-stats__row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}
.git-repo-stats__cell {
  border: 0;
  background: transparent;
  padding: 4px 6px;
  min-width: 0;
  text-align: center;
  cursor: pointer;
  color: inherit;
  box-sizing: border-box;
}
.git-repo-stats__cell .n {
  display: block;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--text-color-primary);
  font-variant-numeric: tabular-nums;
}
.git-repo-stats__cell .l {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.git-repo-stats__cell:hover .n {
  color: var(--cat2bug-primary);
}
.git-repo-stats__langs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}
.lang-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  padding: 2px 8px 2px 6px;
  border-radius: 999px;
  border: 1px solid var(--border-color-light, #414243);
  color: var(--text-color-secondary);
}
.lang-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
