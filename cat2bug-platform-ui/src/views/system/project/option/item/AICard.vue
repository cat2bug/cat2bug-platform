<template>
  <el-col v-if="cardVisible" :span="6">
    <el-card class="box-card">
      <template #header>
        <div class="clearfix">
          <legacy-icon icon="el-icon-cpu"></legacy-icon>
          <div>
            <span>{{$t('project.ai-manager')}}</span>
            <span>{{$t('project.ai-manager-describe')}}</span>
          </div>
        </div>
      </template>
      <router-link v-if="canManageAiModel" :to="href('ollama')"><el-link>{{$t('project.ai-model-manager')}}</el-link></router-link>
      <router-link v-if="canManageAiAccount" :to="href('openai')"><el-link>{{$t('project.ai-account-manager')}}</el-link></router-link>
      <el-link v-if="canManageMcp" @click="mcpVisible = true">{{$t('project-mcp.entry')}}</el-link>
    </el-card>
    <mcp-settings-dialog v-if="canManageMcp" v-model="mcpVisible" :project-id="projectId" />
  </el-col>
</template>

<script>
import { projectAiModelOptions } from "@/api/system/ai";
import { hasAnyPermi, projectOptionHref } from '@/utils/project-option-card'
import { storeToRefs } from 'pinia';
import { useUserStore } from '@/stores/user'
import McpSettingsDialog from '../dialog/McpSettingsDialog.vue'

export default {
  name: "AICard",
  components: { McpSettingsDialog },
  props: {
    project: {
      type: Object,
      default: {}
    }
  },
  setup() {
    const userStore = useUserStore()
    const { config } = storeToRefs(userStore)
    return { config }
  },
  data() {
    return {
      aiEnabled: false,
      mcpVisible: false
    };
  },
  computed: {
    projectId() {
      return parseInt(this.config.currentProjectId);
    },
    canManageAiModel() {
      return this.aiEnabled && hasAnyPermi(['system:ai:list'])
    },
    canManageAiAccount() {
      return hasAnyPermi(['ai:account:list'])
    },
    canManageMcp() {
      return hasAnyPermi(['system:project:query'])
    },
    cardVisible() {
      return this.canManageAiModel || this.canManageAiAccount || this.canManageMcp
    }
  },
  created() {
    this.checkAiEnabled();
  },
  methods: {
    href: projectOptionHref,
    checkAiEnabled() {
      if (this.projectId) {
        projectAiModelOptions(this.projectId).then(res => {
          const data = res.data || {};
          this.aiEnabled = data.aiEnabled === true;
        }).catch(() => {
          this.aiEnabled = false;
        }).finally(() => {
          this.refreshProjectOptionShortcuts()
        });
      } else {
        this.aiEnabled = false;
        this.refreshProjectOptionShortcuts()
      }
    },
    refreshProjectOptionShortcuts() {
      let parent = this.$parent
      while (parent) {
        if (typeof parent.registerProjectOptionShortcuts === 'function') {
          parent.$nextTick(() => parent.registerProjectOptionShortcuts())
          return
        }
        parent = parent.$parent
      }
    }
  }
}
</script>
