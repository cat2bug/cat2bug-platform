<template>
  <aside class="git-overview-aside">
    <!-- 三格统计：扁平分隔，无卡片底 -->
    <section class="git-aside-section git-aside-stats" v-loading="statsLoading">
      <h4 class="git-aside-section__title">{{ $t('git.repo.overview.achievements') }}</h4>
      <div class="git-aside-stats__row">
        <div class="git-aside-stat">
          <span class="git-aside-stat__value">{{ statsLoading ? '-' : overview.repoCount }}</span>
          <span class="git-aside-stat__label">{{ $t('git.repo.overview.repo') }}</span>
        </div>
        <div class="git-aside-stat">
          <span class="git-aside-stat__value">{{ statsLoading ? '-' : overview.openPrCount }}</span>
          <span class="git-aside-stat__label">{{ $t('git.repo.overview.pr') }}</span>
        </div>
        <div class="git-aside-stat">
          <span class="git-aside-stat__value">{{ statsLoading ? '-' : overview.collectCount }}</span>
          <span class="git-aside-stat__label">{{ $t('git.repo.overview.collect') }}</span>
        </div>
      </div>
    </section>

    <!-- 语言：顶部色带 + 色点列表（GitHub 风格） -->
    <section class="git-aside-section git-aside-languages" v-loading="langLoading">
      <h4 class="git-aside-section__title">{{ $t('git.repo.overview.languages') }}</h4>
      <template v-if="!langLoading && languages.length">
        <div class="git-lang-spectrum" aria-hidden="true">
          <span
            v-for="(lang, index) in languages"
            :key="'seg-' + lang.name + '-' + index"
            class="git-lang-spectrum__seg"
            :style="{
              width: clampPercent(lang.percent) + '%',
              background: languageColor(lang.name, index)
            }"
          />
        </div>
        <ul class="git-lang-list">
          <li v-for="(lang, index) in languages" :key="lang.name + '-' + index" class="git-lang-row">
            <span class="git-lang-row__dot" :style="{ background: languageColor(lang.name, index) }" />
            <span class="git-lang-row__name" :title="lang.name">{{ lang.name }}</span>
            <span class="git-lang-row__percent">{{ formatPercent(lang.percent) }}%</span>
          </li>
        </ul>
      </template>
      <p v-else-if="!langLoading" class="git-aside-empty">{{ $t('git.repo.overview.languages-empty') }}</p>
    </section>

    <!-- 源码仓库 ACL 成员预览（跨仓去重；无仓库时为空） -->
    <section class="git-aside-section git-aside-members" v-loading="membersLoading">
      <h4 class="git-aside-section__title">{{ $t('git.repo.overview.members', { n: memberTotal }) }}</h4>
      <div v-if="!membersLoading && displayMembers.length" class="git-member-wall">
        <div
          v-for="(m, index) in displayMembers"
          :key="memberKey(m, index)"
          class="git-member-cell"
          :title="memberName(m)"
        >
          <cat2-bug-avatar class="git-member-cell__avatar" :member="m" :size="40" />
          <span class="git-member-cell__name">{{ memberName(m) }}</span>
        </div>
        <div v-if="memberOverflow > 0" class="git-member-cell git-member-cell--more" :title="'+' + memberOverflow">
          <span class="git-member-cell__more">+{{ memberOverflow }}</span>
        </div>
      </div>
      <p v-else-if="!membersLoading" class="git-aside-empty">{{ $t('git.repo.overview.members-empty') }}</p>
    </section>
  </aside>
</template>

<script>
import Cat2BugAvatar from '@/components/Cat2BugAvatar'
import { getGitRepoProjectOverview, listGitRepoProjectMembers } from '@/api/system/gitRepo'

/** 侧栏成员预览：展示上限（超出以 “+N” 折叠） */
const MEMBERS_DISPLAY_LIMIT = 15

/** 仓库语言色板（按列表下标取色，超出循环） */
const LANGUAGE_PALETTE = [
  '#5070dd',
  '#b6d634',
  '#505372',
  '#ff994d',
  '#0ca8df',
  '#ffd10a',
  '#fb628b',
  '#785db0',
  '#3fbe95'
]
const LANGUAGE_OTHER_COLOR = '#8b9199'

export default {
  name: 'GitRepoOverviewAside',
  components: { Cat2BugAvatar },
  props: {
    projectId: {
      type: [String, Number],
      default: null
    }
  },
  data() {
    return {
      statsLoading: true,
      langLoading: true,
      membersLoading: true,
      overview: {
        repoCount: 0,
        openPrCount: 0,
        collectCount: 0
      },
      languages: [],
      members: [],
      memberTotal: 0
    }
  },
  computed: {
    displayMembers() {
      return (this.members || []).slice(0, MEMBERS_DISPLAY_LIMIT)
    },
    memberOverflow() {
      const loaded = (this.members || []).length
      const shown = this.displayMembers.length
      if (loaded <= shown) return 0
      return Math.max(0, (this.memberTotal || loaded) - shown)
    }
  },
  watch: {
    projectId(val, old) {
      if (val === old) return
      this.loadAll()
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    loadAll() {
      this.loadOverview()
      this.loadMembers()
    },
    /** 统计三格 + 语言分布（同一接口，语言区块仍独立 loading 展示） */
    loadOverview() {
      if (!this.projectId) {
        this.overview = { repoCount: 0, openPrCount: 0, collectCount: 0 }
        this.languages = []
        this.statsLoading = false
        this.langLoading = false
        return
      }
      this.statsLoading = true
      this.langLoading = true
      getGitRepoProjectOverview(this.projectId).then(res => {
        const data = res.data || {}
        this.overview = {
          repoCount: data.repoCount || 0,
          openPrCount: data.openPrCount || 0,
          collectCount: data.collectCount || 0
        }
        this.languages = Array.isArray(data.languages) ? data.languages : []
        this.statsLoading = false
        this.langLoading = false
      }).catch(() => {
        this.statsLoading = false
        this.langLoading = false
      })
    },
    /** 源码仓库 ACL 成员去重预览（非项目成员）；无仓库时为空 */
    loadMembers() {
      if (!this.projectId) {
        this.members = []
        this.memberTotal = 0
        this.membersLoading = false
        return
      }
      this.membersLoading = true
      listGitRepoProjectMembers(this.projectId).then(res => {
        const rows = res.data || res.rows || []
        this.members = Array.isArray(rows) ? rows : []
        this.memberTotal = this.members.length
        this.membersLoading = false
      }).catch(() => {
        this.members = []
        this.memberTotal = 0
        this.membersLoading = false
      })
    },
    memberKey(member, index) {
      if (member && member.userId != null) return member.userId
      return 'member-' + index
    },
    memberName(member) {
      if (!member) return ''
      return member.nickName || member.userName || member.name || ''
    },
    clampPercent(percent) {
      const n = Number(percent)
      if (!Number.isFinite(n)) return 0
      return Math.max(0, Math.min(100, n))
    },
    formatPercent(percent) {
      const n = Number(percent)
      if (!Number.isFinite(n)) return 0
      const rounded = Math.round(n * 10) / 10
      return Number.isInteger(rounded) ? rounded : rounded.toFixed(1)
    },
    languageColor(name, index) {
      if (name === 'Other' || name === '其他') return LANGUAGE_OTHER_COLOR
      const i = Number(index)
      const safeIndex = Number.isFinite(i) && i >= 0 ? i : 0
      return LANGUAGE_PALETTE[safeIndex % LANGUAGE_PALETTE.length]
    },
    /** 关注切换成功后本地 ±1，避免整卡重拉，交互更跟手；由父级 index.vue 通过 ref 调用 */
    adjustCollectCount(delta) {
      const next = (this.overview.collectCount || 0) + Number(delta || 0)
      this.overview.collectCount = Math.max(0, next)
    },
    /** 兜底：需要整体刷新时（如新建/导入仓库后）由父级调用 */
    refresh() {
      this.loadAll()
    }
  }
}
</script>

<style scoped lang="scss">
/* 覆盖全局 aside { background:#eef1f6; padding… }，暗色下也不要白块/卡片底 */
.git-overview-aside {
  display: flex;
  flex-direction: column;
  gap: 0;
  min-width: 0;
  margin: 0;
  padding: 4px 0 0;
  border-radius: 0;
  line-height: inherit;
  font-size: inherit;
  font-family: inherit;
  color: var(--text-color-regular);
  background: transparent;
}

.git-aside-section {
  padding: 0 0 16px;
  margin: 0 0 16px;
  border: none;
  border-bottom: 1px solid var(--border-color-lighter, var(--border-color-light));
  border-radius: 0;
  background: transparent;
  box-sizing: border-box;

  &:last-child {
    margin-bottom: 0;
    padding-bottom: 0;
    border-bottom: none;
  }
}

.git-aside-section__title {
  margin: 0 0 12px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--text-color-regular);
}

/* 三格统计：标题 + 扁平居中三格 */
.git-aside-stats__row {
  display: flex;
  align-items: stretch;
  gap: 0;
}

.git-aside-stat {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 0 8px;
  text-align: center;

  &:not(:last-child) {
    border-right: 1px solid var(--border-color-lighter, var(--border-color-light));
  }

  &__value {
    font-size: 22px;
    font-weight: 600;
    line-height: 1.15;
    color: var(--text-color-primary);
    font-variant-numeric: tabular-nums;
  }

  &__label {
    font-size: 12px;
    color: var(--text-color-regular);
  }
}

/* 语言色带 + 色点列表 */
.git-lang-spectrum {
  display: flex;
  width: 100%;
  height: 8px;
  margin-bottom: 12px;
  border-radius: 4px;
  overflow: hidden;
  background: var(--bg-color-block);
}

.git-lang-spectrum__seg {
  display: block;
  height: 100%;
  min-width: 0;
}

.git-lang-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.git-lang-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-color-regular);

  &__dot {
    flex: 0 0 8px;
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }

  &__name {
    flex: 1 1 auto;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-weight: 500;
    color: var(--text-color-primary);
  }

  &__percent {
    flex: 0 0 auto;
    color: var(--text-color-regular);
    font-variant-numeric: tabular-nums;
  }
}

/* 成员：无胶囊底，头像墙（每行 5 个） */
.git-member-wall {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px 6px;
}

.git-member-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 0;
  text-align: center;

  &__avatar {
    flex-shrink: 0;
  }

  &__name {
    width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 10px;
    line-height: 1.2;
    color: var(--text-color-regular);
  }

  &__more {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    border: 1px dashed var(--border-color-light);
    font-size: 11px;
    font-weight: 600;
    color: var(--text-color-regular);
  }

  &--more {
    justify-content: flex-start;
    padding-top: 0;
  }
}

.git-aside-empty {
  margin: 0;
  font-size: 12px;
  color: var(--text-color-secondary);
}
</style>
