<template>
  <el-col v-if="cardVisible" :span="6">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <i class="el-icon-cpu"></i>
        <div>
          <span>{{$t('project.ai-manager')}}</span>
          <span>{{$t('project.ai-manager-describe')}}</span>
        </div>
      </div>
      <router-link v-if="canManageAiModel" to="ollama"><el-link>{{$t('project.ai-model-manager')}}</el-link></router-link>
      <router-link v-if="canManageAiAccount" to="openai"><el-link>{{$t('project.ai-account-manager')}}</el-link></router-link>
    </el-card>
  </el-col>
</template>

<script>
import { projectAiModelOptions } from "@/api/system/ai";
import { hasAnyPermi } from '@/utils/project-option-card'

export default {
  name: "AICard",
  model: {
    prop: 'project'
  },
  props: {
    project: {
      type: Object,
      default: {}
    }
  },
  data() {
    return {
      aiEnabled: false
    };
  },
  computed: {
    projectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    canManageAiModel() {
      return this.aiEnabled && hasAnyPermi(['system:ai:list'])
    },
    canManageAiAccount() {
      return hasAnyPermi(['ai:account:list'])
    },
    cardVisible() {
      return this.canManageAiModel || this.canManageAiAccount
    }
  },
  created() {
    this.checkAiEnabled();
  },
  methods: {
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
