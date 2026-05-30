<template>
  <el-card class="box-card">
    <div slot="header" class="clearfix">
      <i class="el-icon-s-flag"></i>
      <div>
        <span>{{$t('project.ai-manager')}}</span>
        <span>{{$t('project.ai-manager-describe')}}</span>
      </div>
    </div>
    <router-link v-if="aiEnabled" to="ollama" v-hasPermi="['system:ai:list']"><el-link>{{$t('project.ai-model-manager')}}</el-link></router-link>
    <router-link to="openai" v-hasPermi="['ai:account:list']"><el-link>{{$t('project.ai-account-manager')}}</el-link></router-link>
  </el-card>
</template>

<script>
import { projectAiModelOptions } from "@/api/system/ai";

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
      aiEnabled: true
    };
  },
  computed: {
    projectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    }
  },
  created() {
    this.checkAiEnabled();
  },
  methods: {
    checkAiEnabled() {
      if (this.projectId) {
        projectAiModelOptions(this.projectId).then(res => {
          this.aiEnabled = res.aiEnabled !== false;
        }).catch(() => {
          this.aiEnabled = true;
        });
      }
    }
  }
}
</script>
