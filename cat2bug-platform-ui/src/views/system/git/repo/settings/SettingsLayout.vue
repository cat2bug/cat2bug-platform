<template>
  <div class="app-container git-settings-page" v-loading="loading">
    <git-repo-shell
      v-if="accessAllowed"
      :repository-id="repositoryId"
      :project-id="projectId"
      :repo-name="repoName"
      :visibility="visibility"
      :show-settings="true"
      active-tab="settings"
    >
      <div class="git-settings-layout">
        <aside class="git-settings-sidebar" aria-label="settings">
          <div class="git-settings-group">
            <div class="git-settings-group__title">
              {{ $t('git.repo.settings.nav.group-repo') }}
            </div>
            <nav class="git-settings-nav">
              <button
                type="button"
                class="git-settings-nav__item"
                :class="{ 'is-active': isGeneral }"
                @click="goGeneral"
              >
                {{ $t('git.repo.settings.nav.general') }}
              </button>
              <button
                type="button"
                class="git-settings-nav__item"
                :class="{ 'is-active': isDanger }"
                @click="goDanger"
              >
                {{ $t('git.repo.settings.nav.danger') }}
              </button>
            </nav>
          </div>

          <div class="git-settings-group">
            <div class="git-settings-group__title">
              {{ $t('git.repo.settings.nav.group-access') }}
            </div>
            <nav class="git-settings-nav">
              <button
                type="button"
                class="git-settings-nav__item"
                :class="{ 'is-active': isMembers }"
                @click="goMembers"
              >
                {{ $t('git.repo.settings.nav.members') }}
              </button>
              <button
                type="button"
                class="git-settings-nav__item"
                :class="{ 'is-active': isWebhooks }"
                @click="goWebhooks"
              >
                {{ $t('git.repo.settings.nav.webhooks') }}
              </button>
            </nav>
          </div>
        </aside>

        <section class="git-settings-main">
          <div class="git-settings-main__inner">
            <router-view />
          </div>
        </section>
      </div>
    </git-repo-shell>
  </div>
</template>

<script>
import { getGitRepo, getGitRepoMyAccess } from '@/api/system/gitRepo'
import {
  gitRepoBrowseRoute,
  gitRepoSettingsDangerRoute,
  gitRepoSettingsGeneralRoute,
  gitRepoSettingsMembersRoute,
  gitRepoSettingsWebhooksRoute
} from '@/utils/git-repo-routes'
import { checkPermi } from '@/utils/permission'
import { getCurrentProjectId } from '@/utils/store-context'
import GitRepoShell from '../GitRepoShell.vue'

export default {
  name: 'GitRepoSettingsLayout',
  components: { GitRepoShell },
  data() {
    return {
      loading: false,
      accessAllowed: false,
      repositoryId: Number(this.$route.params.repositoryId) || null,
      projectId: getCurrentProjectId(),
      repoName: String(this.$route.params.slug || ''),
      visibility: 'project'
    }
  },
  computed: {
    isGeneral() {
      return this.$route.name === 'GitRepoSettingsGeneral'
    },
    isDanger() {
      return this.$route.name === 'GitRepoSettingsDanger'
    },
    isMembers() {
      return this.$route.name === 'GitRepoSettingsMembers'
    },
    isWebhooks() {
      return this.$route.name === 'GitRepoSettingsWebhooks'
    }
  },
  watch: {
    '$route.params.repositoryId'(id) {
      const next = Number(id)
      if (Number.isFinite(next) && next > 0 && next !== this.repositoryId) {
        this.repositoryId = next
        this.bootstrap()
      }
    }
  },
  created() {
    this.bootstrap()
  },
  methods: {
    /**
     * Effective manage for settings content: trust my-access.canManage when the API
     * responds; on failure fall back to system:git:repo:manage for compat.
     */
    async resolveCanManage(repositoryId) {
      try {
        const res = await getGitRepoMyAccess(repositoryId)
        const access = (res && res.data) || {}
        return !!access.canManage
      } catch (e) {
        return checkPermi(['system:git:repo:manage'])
      }
    },
    async bootstrap() {
      const id = Number(this.$route.params.repositoryId)
      if (!Number.isFinite(id) || id <= 0) {
        this.accessAllowed = false
        return
      }
      this.repositoryId = id
      this.loading = true
      try {
        const allowed = await this.resolveCanManage(id)
        if (!allowed) {
          this.accessAllowed = false
          this.$router.replace({
            ...gitRepoBrowseRoute(this.$route),
            replace: true
          }).catch(() => {})
          return
        }
        this.accessAllowed = true
        await this.loadRepo()
      } finally {
        this.loading = false
      }
    },
    loadRepo() {
      const id = Number(this.$route.params.repositoryId)
      if (!Number.isFinite(id) || id <= 0) return Promise.resolve()
      this.repositoryId = id
      return getGitRepo(id).then(res => {
        const data = (res && res.data) || {}
        this.repoName = data.name || this.repoName || String(this.$route.params.slug || '')
        this.projectId = data.projectId != null ? data.projectId : (this.projectId || getCurrentProjectId())
        this.visibility = data.visibility === 'public' ? 'public' : 'project'
      }).catch(() => {})
    },
    goGeneral() {
      if (this.isGeneral) return
      this.$router.push(gitRepoSettingsGeneralRoute(this.$route)).catch(() => {})
    },
    goDanger() {
      if (this.isDanger) return
      this.$router.push(gitRepoSettingsDangerRoute(this.$route)).catch(() => {})
    },
    goMembers() {
      if (this.isMembers) return
      this.$router.push(gitRepoSettingsMembersRoute(this.$route)).catch(() => {})
    },
    goWebhooks() {
      if (this.isWebhooks) return
      this.$router.push(gitRepoSettingsWebhooksRoute(this.$route)).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.git-settings-page.app-container {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  padding: 12px 20px 0;
  box-sizing: border-box;
  color: var(--text-color-regular);
}

/* Shell fills remaining viewport so the settings panel can pin to the bottom */
.git-settings-page :deep(.git-repo-shell) {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  height: 100%;
}

.git-settings-page :deep(.git-repo-shell__body) {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
}

/* Split pane: sidebar | content — flush to bottom of page */
.git-settings-layout {
  display: flex;
  align-items: stretch;
  flex: 1 1 auto;
  min-height: 0;
  height: 100%;
  border: 1px solid var(--border-color-light);
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
  background: var(--card-bg);
  overflow: hidden;
}

.git-settings-sidebar {
  flex: 0 0 280px;
  width: 280px;
  padding: 20px 12px;
  box-sizing: border-box;
  background: var(--card-bg);
  border-right: 1px solid var(--sidebar-divider-color, var(--border-color-light));
  overflow-y: auto;
}

.git-settings-group + .git-settings-group {
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color-lighter, var(--border-color-light));
}

/* 组名 = 分类标签：小字、次要色、不可点；与下方功能项刻意拉开层级 */
.git-settings-group__title {
  margin: 0 0 6px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
  line-height: 1.4;
  color: var(--text-color-secondary);
  user-select: none;
  pointer-events: none;
}

.git-settings-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  /* 相对组名向右缩进，形成「组 → 子项」层级（不用左侧竖线） */
  padding-left: 10px;
}

.git-settings-nav__item {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  min-height: 38px;
  padding: 8px 12px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--text-color-primary);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  text-align: left;
  cursor: pointer;
  box-sizing: border-box;
  transition: color 0.15s ease, background-color 0.15s ease;
}

.git-settings-nav__item:hover:not(.is-disabled):not(.is-active) {
  background: var(--el-fill-color-light, rgba(0, 0, 0, 0.04));
}

.git-settings-nav__item.is-active {
  color: var(--el-color-primary);
  background: color-mix(in srgb, var(--el-color-primary) 14%, transparent);
  font-weight: 600;
  box-shadow: none;
}

@supports not (background: color-mix(in srgb, red 10%, white)) {
  .git-settings-nav__item.is-active {
    background: var(--el-color-primary-light-9, rgba(64, 158, 255, 0.12));
  }
}

.git-settings-nav__item.is-disabled {
  color: var(--text-color-placeholder, var(--text-color-secondary));
  opacity: 0.85;
  cursor: not-allowed;
}

.git-settings-nav__label {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.git-settings-nav__badge {
  flex: 0 0 auto;
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
  line-height: 16px;
  color: var(--text-color-secondary);
  background: var(--border-color-lighter, rgba(144, 147, 153, 0.18));
  white-space: nowrap;
}

.git-settings-main {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
  overflow: auto;
  background: var(--card-bg);
}

.git-settings-main__inner {
  width: 100%;
  max-width: none;
  padding: 20px 28px 28px;
  box-sizing: border-box;
}

@media (max-width: 860px) {
  .git-settings-page.app-container {
    height: auto;
    overflow: auto;
    padding-bottom: 12px;
  }

  .git-settings-layout {
    flex-direction: column;
    min-height: 0;
    height: auto;
    border-bottom: 1px solid var(--border-color-light);
    border-radius: 8px;
  }

  .git-settings-sidebar {
    flex: 0 0 auto;
    width: 100%;
    border-right: 0;
    border-bottom: 1px solid var(--sidebar-divider-color, var(--border-color-light));
  }

  .git-settings-nav {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .git-settings-nav__item {
    width: auto;
  }

  .git-settings-main__inner {
    max-width: none;
    padding: 16px 16px 24px;
  }
}
</style>
