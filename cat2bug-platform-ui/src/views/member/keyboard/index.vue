<template>
  <div class="app-container kbd-settings">
    <el-card shadow="never" class="kbd-settings__intro">
      <div slot="header" class="kbd-settings__header">
        <span>{{ $t('keyboard.title') }}</span>
        <div class="kbd-settings__header-ops">
          <el-switch
            v-model="enabled"
            :active-text="$t('keyboard.enabled')"
          />
          <el-button size="mini" @click="resetAll">{{ $t('keyboard.reset-all') }}</el-button>
        </div>
      </div>
      <p class="kbd-settings__desc" v-html="introHtml" />
      <div class="kbd-settings__leaders">
        <span class="kbd-settings__leader">
          {{ $t('keyboard.leader-nav') }}
          <shortcut-badge :letter="leaders.nav" large />
        </span>
        <span class="kbd-settings__leader">
          {{ $t('keyboard.leader-action') }}
          <shortcut-badge :letter="leaders.action" large />
        </span>
      </div>
    </el-card>

    <el-card
      v-for="section in sections"
      :key="section.id"
      shadow="never"
      class="kbd-settings__section"
    >
      <div slot="header">{{ section.title }}</div>
      <el-table :data="section.items" size="small" :show-header="true">
        <el-table-column :label="$t('keyboard.col-action')" prop="title" min-width="200" />
        <el-table-column :label="$t('keyboard.col-shortcut')" width="160">
          <template slot-scope="{ row }">
            <el-input
              v-model="row.letter"
              maxlength="1"
              size="mini"
              class="kbd-settings__input"
              :class="{ 'is-conflict': row.conflict }"
              @input="(v) => onLetterInput(row, v)"
            />
            <span v-if="row.conflict" class="kbd-settings__conflict">
              {{ $t('keyboard.conflict') }}
            </span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('keyboard.col-default')" width="100">
          <template slot-scope="{ row }">
            <shortcut-badge :letter="row.defaultLetter" />
          </template>
        </el-table-column>
        <el-table-column :label="$t('keyboard.col-op')" width="100">
          <template slot-scope="{ row }">
            <el-button
              type="text"
              size="mini"
              :disabled="!isOverridden(row)"
              @click="resetBinding(row, section)"
            >
              {{ $t('keyboard.reset') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import ShortcutBadge from '@/components/Shortcut/ShortcutBadge.vue'
import { shortcutStore } from '@/plugins/shortcut'
import { buildNavItems } from '@/plugins/shortcut/nav-source'
import { DEFECT_ACTION_DEFAULTS, normalizeKey } from '@/plugins/shortcut/keymap'

export default {
  name: 'KeyboardSettings',
  components: { ShortcutBadge },
  data() {
    return {
      sections: []
    }
  },
  computed: {
    enabled: {
      get() {
        return shortcutStore.isEnabled()
      },
      set(v) {
        shortcutStore.setEnabled(v)
      }
    },
    leaders() {
      return shortcutStore.state.leaders
    },
    introHtml() {
      return this.$t('keyboard.intro', {
        nav: shortcutStore.getLeader('nav'),
        action: shortcutStore.getLeader('action')
      })
    }
  },
  created() {
    this.rebuild()
  },
  methods: {
    rebuild() {
      const nav = buildNavItems()
      const menu = nav.filter((it) => it.bindingId.indexOf('nav.menu.') === 0)
      const top = nav.filter((it) => it.bindingId.indexOf('nav.top.') === 0)

      const sections = []
      if (menu.length) {
        sections.push({ id: 'menu', title: this.$t('keyboard.sec-menu'), items: this.toRows(menu) })
      }
      sections.push({ id: 'top', title: this.$t('keyboard.sec-top'), items: this.toRows(top) })

      // 下拉二级项
      top.forEach((item) => {
        if (item.children && item.children.length) {
          sections.push({
            id: item.bindingId,
            title: this.$t('keyboard.sec-sub', { name: item.title }),
            items: this.toRows(item.children)
          })
        }
      })

      // 缺陷页动作
      const defectRows = DEFECT_ACTION_DEFAULTS.map((a) => ({
        bindingId: `action.defect.${a.key}`,
        title: this.$t(a.titleKey),
        defaultLetter: a.defaultLetter,
        letter: shortcutStore.getLetter(`action.defect.${a.key}`, a.defaultLetter),
        conflict: false
      }))
      sections.push({ id: 'defect', title: this.$t('keyboard.sec-defect'), items: defectRows })

      sections.forEach((s) => this.markConflicts(s))
      this.sections = sections
    },
    toRows(items) {
      return items.map((it) => ({
        bindingId: it.bindingId,
        title: it.title,
        defaultLetter: it.defaultLetter || '',
        letter: shortcutStore.getLetter(it.bindingId, it.defaultLetter),
        conflict: false
      }))
    },
    markConflicts(section) {
      const counts = {}
      section.items.forEach((r) => {
        const l = normalizeKey(r.letter)
        if (l) counts[l] = (counts[l] || 0) + 1
      })
      section.items.forEach((r) => {
        const l = normalizeKey(r.letter)
        r.conflict = !!l && counts[l] > 1
      })
    },
    onLetterInput(row, value) {
      const norm = normalizeKey(value)
      row.letter = norm
      shortcutStore.setOverride(row.bindingId, norm, row.defaultLetter)
      const section = this.sections.find((s) => s.items.includes(row))
      if (section) this.markConflicts(section)
    },
    isOverridden(row) {
      return normalizeKey(row.letter) !== normalizeKey(row.defaultLetter)
    },
    resetBinding(row, section) {
      shortcutStore.resetBinding(row.bindingId)
      row.letter = normalizeKey(row.defaultLetter)
      this.markConflicts(section)
    },
    resetAll() {
      this.$confirm(this.$t('keyboard.reset-all-confirm'), this.$t('prompted'), {
        confirmButtonText: this.$t('ok'),
        cancelButtonText: this.$t('cancel'),
        type: 'warning'
      })
        .then(() => {
          shortcutStore.resetAll()
          this.rebuild()
        })
        .catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.kbd-settings__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.kbd-settings__header-ops {
  display: flex;
  align-items: center;
  gap: 16px;
}
.kbd-settings__desc {
  color: var(--el-text-color-secondary, #909399);
  font-size: 13px;
  line-height: 1.7;
  margin: 0 0 12px;
}
.kbd-settings__leaders {
  display: flex;
  gap: 28px;
}
.kbd-settings__leader {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.kbd-settings__section {
  margin-top: 16px;
}
.kbd-settings__input {
  width: 70px;
  text-transform: uppercase;
}
.kbd-settings__input.is-conflict ::v-deep .el-input__inner {
  border-color: var(--el-color-danger, #f56c6c);
}
.kbd-settings__conflict {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-color-danger, #f56c6c);
}
</style>
