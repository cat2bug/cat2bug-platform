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
              v-for="(route, index) in teamRouters"
              :key="route.path  + index"
              :item="route"
              :base-path="'team/'+route.path"
            />
          </el-menu>
          <div v-show="teamId && teamRouters" class="sidebar-divider">
            <el-divider></el-divider>
          </div>
          <el-menu
              v-show="teamId && projectId"
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
                  v-for="(route, index) in projectRouters"
                  :key="route.path  + index"
                  :item="route"
                  :base-path="'project/'+route.path"
              />
          </el-menu>
          <div v-show="teamId && projectId && projectRouters" class="sidebar-divider">
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
              v-for="(route, index) in teamOptionRouters"
              :key="route.path  + index"
              :item="route"
              :base-path="'team-options/'+route.path"
            />
          </el-menu>
          <div v-show="teamId && teamOptionRouters" class="sidebar-divider">
            <el-divider></el-divider>
          </div>
<!--          <el-menu-->
<!--            :default-active="activeMenu"-->
<!--            :collapse="isCollapse"-->
<!--            :background-color="settings.sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground"-->
<!--            :text-color="settings.sideTheme === 'theme-dark' ? variables.menuColor : variables.menuLightColor"-->
<!--            :unique-opened="true"-->
<!--            :active-text-color="settings.theme"-->
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
      <div style="bottom: 10px;position: absolute;height: auto;width: 100%;" v-show="adminOptionRouters.length>0" >
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
import { mapGetters, mapState } from "vuex";
import TeamSelect from "./TeamSelect"
import SidebarItem from "./SidebarItem";
import variables from "@/assets/styles/variables.scss";

export default {
    components: { SidebarItem, TeamSelect },
    data() {
      return {
      }
    },
    computed: {
        ...mapState(["settings"]),
        ...mapGetters(["sidebarRouters", "sidebar"]),
      teamId() {
          return this.$store.state.user.config.currentTeamId;
      },
      projectId() {
        return this.$store.state.user.config.currentProjectId
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
