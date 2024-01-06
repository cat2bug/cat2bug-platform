<template>
    <div :class="{'has-logo':showLogo}" :style="{ backgroundColor: settings.sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground }">
        <team-select v-if="showLogo" :collapse="isCollapse" v-model="teamId"></team-select>
        <el-scrollbar :class="settings.sideTheme" wrap-class="scrollbar-wrapper">
          <el-menu
            v-show="teamId"
            :default-active="activeMenu"
            :collapse="isCollapse"
            :background-color="settings.sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground"
            :text-color="settings.sideTheme === 'theme-dark' ? variables.menuColor : variables.menuLightColor"
            :unique-opened="true"
            :active-text-color="settings.theme"
            :collapse-transition="false"
            mode="vertical"
          >
            <sidebar-item
              v-for="(route, index) in filterSidebarRouters('team')"
              :key="route.path  + index"
              :item="route"
              :base-path="'team/'+route.path"
            />
          </el-menu>
          <div v-show="teamId" class="sidebar-divider">
            <el-divider></el-divider>
          </div>
          <el-menu
              v-show="teamId && isShowProjectMenu"
              :default-active="activeMenu"
              :collapse="isCollapse"
              :background-color="settings.sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground"
              :text-color="settings.sideTheme === 'theme-dark' ? variables.menuColor : variables.menuLightColor"
              :unique-opened="true"
              :active-text-color="settings.theme"
              :collapse-transition="false"
              mode="vertical"
          >
              <sidebar-item
                  v-for="(route, index) in filterSidebarRouters('project')"
                  :key="route.path  + index"
                  :item="route"
                  :base-path="'project/'+route.path"
              />
          </el-menu>
          <div v-show="teamId && isShowProjectMenu" class="sidebar-divider">
            <el-divider></el-divider>
          </div>
          <el-menu
            v-show="teamId"
            :default-active="activeMenu"
            :collapse="isCollapse"
            :background-color="settings.sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground"
            :text-color="settings.sideTheme === 'theme-dark' ? variables.menuColor : variables.menuLightColor"
            :unique-opened="true"
            :active-text-color="settings.theme"
            :collapse-transition="false"
            mode="vertical"
          >
            <sidebar-item
              v-for="(route, index) in filterSidebarRouters('team-options')"
              :key="route.path  + index"
              :item="route"
              :base-path="'team-options/'+route.path"
            />
          </el-menu>
          <div class="sidebar-divider">
            <el-divider></el-divider>
          </div>
          <el-menu
            :default-active="activeMenu"
            :collapse="isCollapse"
            :background-color="settings.sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground"
            :text-color="settings.sideTheme === 'theme-dark' ? variables.menuColor : variables.menuLightColor"
            :unique-opened="true"
            :active-text-color="settings.theme"
            :collapse-transition="false"
            mode="vertical"
          >
            <sidebar-item
              v-for="(route, index) in filterSidebarRouters('system')"
              :key="route.path  + index"
              :item="route"
              :base-path="'system/'+route.path"
            />
          </el-menu>
        </el-scrollbar>
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import TeamSelect from "./TeamSelect"
import SidebarItem from "./SidebarItem";
import variables from "@/assets/styles/variables.scss";

export default {
    components: { SidebarItem, TeamSelect },
    data() {
      return {
        // teamId: null,
      }
    },
    computed: {
        ...mapState(["settings"]),
        ...mapGetters(["sidebarRouters", "sidebar"]),
      teamId() {
          return this.$store.state.user.config.currentTeamId;
      },
      isShowProjectMenu() {
        return this.$store.state.user.config.currentProjectId
      },
      filterSidebarRouters() {
        return function (name){
          for(let i in this.sidebarRouters) {
            if(this.sidebarRouters[i].name && this.sidebarRouters[i].name.toLowerCase()==name.toLowerCase()){
              return this.sidebarRouters[i].children;
            }
          }
          return [];
        }
      },
      activeMenu() {
          const route = this.$route;
          const { meta, path } = route;
          // if set path, the sidebar will highlight the path you set
          if (meta.activeMenu) {
              return meta.activeMenu;
          }
          return path;
      },
      showLogo() {
          return this.$store.state.settings.sidebarLogo;
      },
      variables() {
          return variables;
      },
      isCollapse() {
          return !this.sidebar.opened;
      }
    }
};
</script>
<style lang="scss" scoped>
  .sidebar-divider {
    padding: 0px 15px;
    .el-divider {
      margin: 7px 0px;
      background-color: #EBEEF5;
    }
  }
</style>
