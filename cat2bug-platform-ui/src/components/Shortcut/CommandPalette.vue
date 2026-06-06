<template>
  <transition name="cmdp-fade">
    <div v-if="palette.open" class="cmdp-mask" @mousedown.self="onMaskClick">
      <div class="cmdp" role="dialog" aria-modal="true">
        <div class="cmdp__head">
          <div class="cmdp__crumbs">
            <span
              v-for="(lv, i) in palette.stack"
              :key="i"
              class="cmdp__crumb"
              :class="{ 'is-current': i === palette.stack.length - 1 }"
            >
              <span v-if="i > 0" class="cmdp__crumb-sep">/</span>{{ lv.title }}
            </span>
          </div>
          <div class="cmdp__search" :class="{ 'is-active': searchFocused }">
            <i class="el-icon-search" />
            <input
              ref="search"
              v-model="query"
              type="text"
              :placeholder="$t('keyboard.palette.search-hint')"
              @focus="searchFocused = true"
              @blur="searchFocused = false"
            >
          </div>
        </div>

        <div class="cmdp__body">
          <div v-if="filtered.length === 0" class="cmdp__empty">
            {{ $t('keyboard.palette.no-result') }}
          </div>
          <ul v-else class="cmdp__list">
            <li
              v-for="(item, idx) in filtered"
              :key="item.bindingId || item.title"
              class="cmdp__item"
              :class="{ 'is-active': idx === highlight }"
              @mouseenter="highlight = idx"
              @click="activate(item)"
            >
              <shortcut-badge :letter="item.letter" />
              <span class="cmdp__title" v-html="renderTitle(item.title)" />
              <i v-if="item.type === 'dropdown'" class="el-icon-arrow-right cmdp__more" />
            </li>
          </ul>
        </div>

        <div class="cmdp__foot">
          <span><shortcut-badge letter="↑" /><shortcut-badge letter="↓" /> {{ $t('keyboard.palette.tip-move') }}</span>
          <span><shortcut-badge letter="↵" /> {{ $t('keyboard.palette.tip-enter') }}</span>
          <span><shortcut-badge letter="/" /> {{ $t('keyboard.palette.tip-search') }}</span>
          <span><shortcut-badge letter="Esc" /> {{ $t('keyboard.palette.tip-esc') }}</span>
        </div>
      </div>
    </div>
  </transition>
</template>

<script>
import ShortcutBadge from './ShortcutBadge.vue'
import { shortcutService } from '@/plugins/shortcut'
import { normalizeKey } from '@/plugins/shortcut/keymap'

const I18N_LOCALE_KEY = 'i18n-locale'

/** 子序列模糊匹配，返回 { ok, score }，score 越小越优 */
function fuzzy(query, text) {
  const q = String(query || '').toLowerCase().trim()
  const t = String(text || '').toLowerCase()
  if (!q) return { ok: true, score: 0 }
  const direct = t.indexOf(q)
  if (direct >= 0) return { ok: true, score: direct }
  let qi = 0
  let lastIdx = -1
  let gaps = 0
  for (let i = 0; i < t.length && qi < q.length; i++) {
    if (t[i] === q[qi]) {
      if (lastIdx >= 0) gaps += i - lastIdx - 1
      lastIdx = i
      qi++
    }
  }
  if (qi === q.length) return { ok: true, score: 1000 + gaps }
  return { ok: false, score: Infinity }
}

export default {
  name: 'CommandPalette',
  components: { ShortcutBadge },
  data() {
    return {
      palette: shortcutService.palette,
      highlight: 0,
      searchFocused: false
    }
  },
  computed: {
    query: {
      get() {
        return this.palette.query
      },
      set(v) {
        shortcutService.setQuery(v)
      }
    },
    items() {
      return shortcutService.currentItems()
    },
    filtered() {
      const q = this.query
      if (!q) return this.items
      return this.items
        .map((it) => ({ it, m: fuzzy(q, it.title) }))
        .filter((x) => x.m.ok)
        .sort((a, b) => a.m.score - b.m.score)
        .map((x) => x.it)
    }
  },
  watch: {
    'palette.open'(open) {
      if (open) {
        this.highlight = 0
        this.searchFocused = false
        document.addEventListener('keydown', this.onKeydown, true)
      } else {
        document.removeEventListener('keydown', this.onKeydown, true)
      }
    },
    'palette.stack'() {
      this.highlight = 0
    },
    query() {
      this.highlight = 0
    }
  },
  created() {
    shortcutService.setExecutor(this.execute)
  },
  beforeDestroy() {
    document.removeEventListener('keydown', this.onKeydown, true)
  },
  methods: {
    onMaskClick() {
      shortcutService.close()
    },
    onKeydown(event) {
      if (!this.palette.open) return
      if (event.isComposing) return
      const focused = document.activeElement === this.$refs.search

      if (event.key === 'Escape') {
        event.preventDefault()
        event.stopPropagation()
        if (focused && this.query) {
          this.query = ''
          return
        }
        if (this.searchFocused) this.$refs.search.blur()
        shortcutService.back()
        return
      }
      if (event.key === 'ArrowDown') {
        event.preventDefault()
        this.move(1)
        return
      }
      if (event.key === 'ArrowUp') {
        event.preventDefault()
        this.move(-1)
        return
      }
      if (event.key === 'Enter') {
        event.preventDefault()
        const item = this.filtered[this.highlight]
        if (item) this.activate(item)
        return
      }
      if (event.key === 'Backspace' && !focused && !this.query) {
        event.preventDefault()
        shortcutService.back()
        return
      }
      if (event.key === '/' && !focused) {
        event.preventDefault()
        this.$nextTick(() => this.$refs.search && this.$refs.search.focus())
        return
      }
      // 未聚焦搜索框：字母 / 数字直达
      if (!focused) {
        const letter = normalizeKey(event.key)
        if (letter) {
          if (shortcutService.selectLetter(letter)) {
            event.preventDefault()
            event.stopPropagation()
          }
        }
      }
    },
    move(delta) {
      const n = this.filtered.length
      if (!n) return
      this.highlight = (this.highlight + delta + n) % n
    },
    activate(item) {
      shortcutService.activate(item)
    },
    renderTitle(title) {
      const q = (this.query || '').trim()
      const safe = this.escapeHtml(title)
      if (!q) return safe
      const idx = safe.toLowerCase().indexOf(q.toLowerCase())
      if (idx < 0) return safe
      return (
        safe.slice(0, idx) +
        '<mark class="cmdp__hl">' +
        safe.slice(idx, idx + q.length) +
        '</mark>' +
        safe.slice(idx + q.length)
      )
    },
    escapeHtml(s) {
      return String(s == null ? '' : s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
    },

    /** 执行被激活的叶子项（与 Vue 上下文相关的副作用集中在此） */
    execute(item) {
      if (!item) return
      if (typeof item.run === 'function') {
        item.run()
        return
      }
      if (item.type === 'route' && item.to) {
        if (this.$route.path !== item.to) this.$router.push(item.to).catch(() => {})
        return
      }
      if (item.action) this.runAction(item.action)
    },
    runAction(action) {
      if (action.indexOf('lang:') === 0) {
        this.applyLang(action.slice(5))
      } else if (action === 'theme:light' || action === 'theme:dark') {
        this.$store.dispatch('settings/changeThemeMode', action.split(':')[1])
      } else if (action === 'logout') {
        this.logout()
      } else if (action === 'open:site') {
        window.open('https://www.cat2bug.com')
      } else if (action === 'open:git') {
        window.open('https://gitee.com/cat2bug/cat2bug-platform')
      }
    },
    applyLang(lang) {
      this.$i18n.locale = lang
      try {
        this.$cache.local.set(I18N_LOCALE_KEY, lang)
      } catch (e) {
        localStorage.setItem(I18N_LOCALE_KEY, lang)
      }
    },
    logout() {
      this.$confirm(
        this.$i18n.t('sure-logout-system').toString(),
        this.$i18n.t('prompted').toString(),
        {
          confirmButtonText: this.$i18n.t('ok'),
          cancelButtonText: this.$i18n.t('cancel'),
          type: 'warning'
        }
      )
        .then(() => {
          this.$store.dispatch('LogOut').then(() => {
            location.href = '/index'
          })
        })
        .catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.cmdp-fade-enter-active,
.cmdp-fade-leave-active {
  transition: opacity 0.15s ease;
}
.cmdp-fade-enter,
.cmdp-fade-leave-to {
  opacity: 0;
}

$cmdp-bg: #1f1f23;
$cmdp-bg-elev: #2a2a2f;
$cmdp-border: rgba(255, 255, 255, 0.1);
$cmdp-border-soft: rgba(255, 255, 255, 0.07);
$cmdp-text: #e8e8ea;
$cmdp-text-dim: #9a9aa2;
$cmdp-accent: #ffce3a;

.cmdp-mask {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 12vh;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(3px);
}

.cmdp {
  width: 560px;
  max-width: calc(100vw - 32px);
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  background: $cmdp-bg;
  color: $cmdp-text;
  border: 1px solid $cmdp-border;
  border-radius: 12px;
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.55);
  overflow: hidden;
}

.cmdp__head {
  padding: 14px 16px 10px;
  border-bottom: 1px solid $cmdp-border-soft;
}
.cmdp__crumbs {
  font-size: 12px;
  color: $cmdp-text-dim;
  margin-bottom: 8px;
}
.cmdp__crumb.is-current {
  color: $cmdp-text;
  font-weight: 600;
}
.cmdp__crumb-sep {
  margin: 0 6px;
  opacity: 0.5;
}
.cmdp__search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid $cmdp-border;
  border-radius: 8px;
  transition: border-color 0.15s;
  i {
    color: $cmdp-text-dim;
  }
  input {
    flex: 1;
    border: none;
    outline: none;
    background: transparent;
    font-size: 14px;
    color: $cmdp-text;
    &::placeholder {
      color: $cmdp-text-dim;
    }
  }
  &.is-active {
    border-color: $cmdp-accent;
  }
}

.cmdp__body {
  flex: 1;
  overflow-y: auto;
  padding: 6px;
  &::-webkit-scrollbar {
    width: 8px;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.14);
    border-radius: 4px;
  }
}
.cmdp__empty {
  padding: 32px 0;
  text-align: center;
  color: $cmdp-text-dim;
  font-size: 13px;
}
.cmdp__list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.cmdp__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border-radius: 8px;
  cursor: pointer;
  &.is-active {
    background: $cmdp-bg-elev;
    box-shadow: inset 2px 0 0 $cmdp-accent;
  }
}
.cmdp__title {
  flex: 1;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
::v-deep .cmdp__hl {
  background: rgba(255, 206, 58, 0.28);
  color: $cmdp-accent;
  border-radius: 2px;
  padding: 0 1px;
}
.cmdp__more {
  color: $cmdp-text-dim;
}

.cmdp__foot {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  padding: 10px 16px;
  border-top: 1px solid $cmdp-border-soft;
  font-size: 12px;
  color: $cmdp-text-dim;
  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}
</style>
