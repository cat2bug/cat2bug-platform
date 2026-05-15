<template>
  <el-card class="box-card">
    <div slot="header" class="clearfix">
      <i class="el-icon-s-flag"></i>
      <div>
        <span>{{$t('project.ai-manager')}}</span>
        <span>{{$t('project.ai-manager-describe')}}</span>
      </div>
    </div>
    <router-link to="ollama" v-hasPermi="['system:ai:list']"><el-link>{{$t('project.ai-model-manager')}}</el-link></router-link>
    <router-link to="openai" v-hasPermi="['ai:account:list']"><el-link>{{$t('project.ai-account-manager')}}</el-link></router-link>
  </el-card>
</template>

<script>

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
  methods: {

  }
}
</script>
