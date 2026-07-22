<template>
    <div :class="{'has-logo':showLogo, 'sidebar-shell--dark': isSidebarDark}" :style="{ backgroundColor: sidebarShellBackground }">
        <team-select v-if="showLogo" :collapse="isCollapse" :dark="isSidebarDark"></team-select>
        <el-scrollbar :class="scrollbarThemeClass" wrap-class="scrollbar-wrapper">
          <el-menu
            v-show="!teamLock && teamId && teamMember && teamRouters.length > 0"
            :default-active="activeMenu"
            :collapse="isCollapse"
            :background-color="menuBackgroundColor"
            :text-color="menuTextColor"
            :unique-opened="true"
            :active-text-color="menuActiveTextColor"
            :collapse-transition="false"
            mode="vertical"
          >
            <sidebar-item
              v-for="(route, index) in teamRouters"
              :key="route.path  + index"
              :item="route"
              :base-path="'team/'+route.path"
            />
          </el-menu>
          <div v-show="!teamLock && teamId && teamMember && teamRouters.length > 0" class="sidebar-divider">
            <el-divider></el-divider>
          </div>
          <el-menu
              v-show="!teamLock && teamId && !projectLock && projectId && projectRouters.length > 0 && !routesRefreshing"
              :default-active="activeMenu"
              :collapse="isCollapse"
              :background-color="menuBackgroundColor"
              :text-color="menuTextColor"
              :unique-opened="true"
              :active-text-color="menuActiveTextColor"
              :collapse-transition="false"
              mode="vertical"
          >
              <sidebar-item
                  v-for="(route, index) in projectRouters"
                  :key="route.path  + index"
                  :item="route"
                  :base-path="projectMenuBasePath(route)"
              />
          </el-menu>
          <div v-show="!teamLock && teamId && !projectLock && projectId && projectRouters.length > 0 && !routesRefreshing" class="sidebar-divider">
            <el-divider></el-divider>
          </div>
          <el-menu
            v-show="teamId && teamMember && teamOptionRouters.length > 0"
            :default-active="activeMenu"
            :collapse="isCollapse"
            :background-color="menuBackgroundColor"
            :text-color="menuTextColor"
            :unique-opened="true"
            :active-text-color="menuActiveTextColor"
            :collapse-transition="false"
            mode="vertical"
          >
            <sidebar-item
              v-for="(route, index) in teamOptionRouters"
              :key="route.path  + index"
              :item="route"
              :base-path="'team-options/'+route.path"
            />
          </el-menu>
          <div v-show="teamId && teamMember && teamOptionRouters.length > 0" class="sidebar-divider">
            <el-divider></el-divider>
          </div>
<!--          <el-menu-->
<!--            :default-active="activeMenu"-->
<!--            :collapse="isCollapse"-->
<!--            :background-color="settings.sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground"-->
<!--            :text-color="settings.sideTheme === 'theme-dark' ? variables.menuColor : variables.menuLightColor"-->
<!--            :unique-opened="true"-->
<!--            :active-text-color="menuActiveTextColor"-->
<!--            :collapse-transition="false"-->
<!--            mode="vertical"-->
<!--          >-->
<!--            <sidebar-item-->
<!--              v-for="(route, index) in filterSidebarRouters('system')"-->
<!--              :key="route.path  + index"-->
<!--              :item="route"-->
<!--              :base-path="'system/'+route.path"-->
<!--            />-->
<!--          </el-menu>-->
        </el-scrollbar>
      <div class="sidebar-admin-panel" v-show="adminOptionRouters.length>0" >
        <div class="sidebar-divider">
          <el-divider></el-divider>
        </div>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :background-color="menuBackgroundColor"
          :text-color="menuTextColor"
          :unique-opened="true"
          :active-text-color="menuActiveTextColor"
          :collapse-transition="false"
          mode="vertical"
        >
          <sidebar-item
            v-for="(route, index) in adminOptionRouters"
            :key="route.path  + index"
            :item="route"
            :base-path="'admin/'+route.path"
          />
        </el-menu>
      </div>
    </div>
</template>

<script>
import { storeToRefs } from 'pinia'
import { useSettingsStore } from '@/stores/settings'
import { usePermissionStore } from '@/stores/permission'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import TeamSelect from "./TeamSelect"
import SidebarItem from "./SidebarItem";
import variables from "@/assets/styles/variables.js";
import { resolveProjectScopedActiveMenu } from '@/utils/project-module-path'

export default {
    components: { SidebarItem, TeamSelect },
    setup() {
      const settingsStore = useSettingsStore()
      const permissionStore = usePermissionStore()
      const appStore = useAppStore()
      const userStore = useUserStore()
      const { themeMode, sideTheme, theme, sidebarLogo } = storeToRefs(settingsStore)
      const { sidebarRouters, routesRefreshing } = storeToRefs(permissionStore)
      const { sidebar } = storeToRefs(appStore)
      const { config, teamMember } = storeToRefs(userStore)
      return {
        settingsStore,
        themeMode,
        sideTheme,
        theme,
        sidebarLogo,
        sidebarRouters,
        routesRefreshing,
        sidebar,
        userConfig: config,
        userTeamMember: teamMember
      }
    },
    computed: {
        settings() {
          return this.settingsStore
        },
      /** 侧栏菜单背景：随 themeMode / sideTheme 切换 */
      menuBackgroundColor() {
        if (this.settings?.themeMode === 'dark') {
          return 'var(--sidebar-bg)'
        }
        return this.settings?.sideTheme === 'theme-dark'
          ? variables.menuBackground
          : variables.menuLightBackground
      },
      sidebarShellBackground() {
        if (this.settings?.themeMode === 'dark') {
          return 'var(--sidebar-bg)'
        }
        return this.settings?.sideTheme === 'theme-dark'
          ? variables.menuBackground
          : variables.menuLightBackground
      },
      scrollbarThemeClass() {
        if (this.settings?.themeMode === 'dark') {
          return 'theme-dark'
        }
        return this.settings?.sideTheme
      },
      menuTextColor() {
        if (this.settings?.themeMode === 'dark') {
          return 'var(--sidebar-text)'
        }
        return this.settings?.sideTheme === 'theme-dark'
          ? variables.menuColor
          : variables.menuLightColor
      },
      teamId() {
          return this.userConfig && this.userConfig.currentTeamId
      },
      teamLock() {
        return this.userConfig && this.userConfig.currentTeamLock
      },
      projectId() {
        return this.userConfig && this.userConfig.currentProjectId
      },
      projectLock() {
        return this.userConfig && this.userConfig.currentProjectLock
      },
      teamMember() {
        return this.userTeamMember !== false
      },
      projectRouters() {
          return this.filterSidebarRouters('project');
      },
      teamRouters() {
        return this.filterSidebarRouters('team');
      },
      teamOptionRouters() {
        return this.filterSidebarRouters('team-options');
      },
      adminOptionRouters() {
        return this.filterSidebarRouters('admin');
      },
      filterSidebarRouters() {
        return function (name){
          for(let i in this.sidebarRouters) {
            if(this.sidebarRouters[i].name && this.sidebarRouters[i].name.toLowerCase()==name.toLowerCase()){
              return this.sidebarRouters[i].children || []
            }
          }
          return [];
        }
      },
      /** Absolute namespaced menu paths use themselves as base; legacy keep `project/{seg}`. */
      projectMenuBasePath() {
        return function (route) {
          const p = route && route.path != null ? String(route.path) : ''
          if (p.startsWith('/')) return p
          return 'project/' + p
        }
      },
      activeMenu() {
          const route = this.$route;
          const { meta, path } = route;
          return resolveProjectScopedActiveMenu(path, meta && meta.activeMenu);
      },
      showLogo() {
          return this.sidebarLogo
      },
      variables() {
          return variables;
      },
      isCollapse() {
          return !this.sidebar.opened;
      },
      isSidebarDark() {
        return this.settings?.themeMode === 'dark' || this.settings?.sideTheme === 'theme-dark'
      },
      menuActiveTextColor() {
        if (this.isSidebarDark) {
          return 'var(--sidebar-active-text, #FFC107)'
        }
        return this.settings?.theme
      }
    }
};
</script>
<style lang="scss" scoped>
  .sidebar-divider {
    padding: 0px 15px;
    .el-divider {
      margin: 8px 0px;
    }
    .el-divider--horizontal {
      border-top-color: var(--sidebar-divider-color, #ebeef5) !important;
    }
  }

  .sidebar-admin-panel {
    bottom: 10px;
    position: absolute;
    height: auto;
    width: 100%;
    background-color: inherit;
  }
</style>
