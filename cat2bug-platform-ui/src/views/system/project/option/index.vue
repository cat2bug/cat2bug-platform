<template>
  <div class="app-container project-option-page" ref="optionMain">
    <project-label />
    <el-row :gutter="20">
      <component :is="m.name" v-for="(m, index) in itemList" :key="index" :project="project"></component>
    </el-row>
  </div>
</template>

<script>
import {getProject} from "@/api/system/project";
import ProjectLabel from "@/components/Project/ProjectLabel";
import pageActionHints from '@/mixins/page-action-hints'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import {
  buildOptionCardPageActionHints,
  buildOptionCardRegisterActions
} from '@/utils/option-card-kbd-hints'
import { storeToRefs } from 'pinia';
import { useUserStore } from '@/stores/user'

const itemModules = import.meta.glob('./item/*.vue', { eager: true });
const modules = { ProjectLabel };
const moduleList = [];
// 动态加载组件（Vite: import.meta.glob 替代 webpack require.context）
Object.entries(itemModules).forEach(([key, mod]) => {
  const name = key.replace(/^\.\/item\/(.+)\.vue$/, '$1');
  moduleList.push({ name });
  modules[name] = mod.default || mod;
});

const PROJECT_OPTION_KBD_SCOPE = 'project-option'

export default {
  name: "ProjectOption",
  mixins: [pageActionHints],
  components: modules,
  setup() {
    const userStore = useUserStore()
    const { config } = storeToRefs(userStore)
    return { config }
  },
  data() {
    return {
      project: {},
      itemList: moduleList
    };
  },
  computed: {
  },
  mounted() {
    this.getProject();
    this.registerProjectOptionShortcuts();
  },
  activated() {
    this.registerProjectOptionShortcuts();
  },
  deactivated() {
    if (this.$shortcut) this.$shortcut.unregisterPage(PROJECT_OPTION_KBD_SCOPE);
  },
  beforeDestroy() {
    if (this.$shortcut) this.$shortcut.unregisterPage(PROJECT_OPTION_KBD_SCOPE);
  },
  methods: {
    registerProjectOptionShortcuts() {
      if (!this.$shortcut) return
      const container = this.getPageActionHintContainer()
      const L = (key, def) => shortcutStore.getLetter(`action.${PROJECT_OPTION_KBD_SCOPE}.${key}`, def)
      const actions = buildOptionCardRegisterActions({
        container,
        letterForKey: L,
        scopeKey: PROJECT_OPTION_KBD_SCOPE
      })
      this.$shortcut.registerPage(PROJECT_OPTION_KBD_SCOPE, actions)
    },
    getPageActionHintContainer() {
      return this.$refs.optionMain || this.$el
    },
    getPageActionHints() {
      const container = this.getPageActionHintContainer()
      const L = (key, def) => shortcutStore.getLetter(`action.${PROJECT_OPTION_KBD_SCOPE}.${key}`, def)
      return buildOptionCardPageActionHints({
        container,
        letterForKey: L,
        scopeKey: PROJECT_OPTION_KBD_SCOPE
      })
    },
    getProjectId() {
      return parseInt(this.config.currentProjectId);
    },
    getProject() {
      getProject(this.getProjectId()).then(res=>{
        this.project = res.data;
        this.$nextTick(() => this.registerProjectOptionShortcuts())
      });
    },
  }
};
</script>
<style lang="scss" scoped>
  $project-option-card-gap: 20px;

  /* gutter 仅控制列间距，补相同 row-gap 使行列间隔一致 */
  :deep(.el-row){
    margin-bottom: -$project-option-card-gap;
  }

  :deep(.el-col){
    margin-bottom: $project-option-card-gap;
  }

  :deep(.el-card__header){
    font-weight: 600;
    div:first-child {
      display: flex;
      flex-direction: row;
      width: calc(100% - 30px);
      div {
        display: flex;
        flex-direction: column;
        width: 100%;
        span {
          margin-bottom: 5px;
        }
        span:first-child, span:last-child {
          text-overflow: ellipsis;
          overflow: hidden;
          white-space: nowrap;
        }
        span:last-child {
          line-height: 12px;
          font-size: 12px;
          color: #C0C4CC;
        }
      }
    }
    i, .svg-icon {
      color: var(--cat2bug-option-card-icon-color, #1890ff);
      fill: currentColor;
      margin-right: 10px;
      margin-top: 3px;
      flex-shrink: 0;
    }
    .svg-icon {
      font-size: 16px;
      width: 1em;
      height: 1em;
    }
  }
  :deep(.el-card__body){
    min-height: 180px;
    display: flex;
    align-items: flex-start;
    flex-direction: column;
    > * {
      margin-left: 20px;
      margin-bottom: 15px;
      width: fit-content;
      max-width: calc(100% - 20px);
    }
    /* el-link ::after 会按元素宽度画线；入口文字较短时避免下划线撑满卡片 */
    .el-link {
      width: fit-content;
      max-width: 100%;

      &::after,
      &:hover::after {
        display: none !important;
      }

      &:hover .el-link__inner {
        text-decoration: underline;
        text-underline-offset: 3px;
      }
    }
  }
  :deep(.el-card__header){
    position: relative;
    overflow: visible !important;
  }
</style>
