<template>
  <el-col v-if="cardVisible" :span="6">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <i class="el-icon-connection"></i>
        <div>
          <span>{{$t('project.other-system')}}</span>
          <span>{{$t('project.other-system-describe')}}</span>
        </div>
      </div>
      <router-link v-if="canManageDing" to="ding"><el-link>{{$t('ding')}}</el-link></router-link>
      <router-link v-if="canManageFeishu" to="feishu"><el-link>{{$t('feishu')}}</el-link></router-link>
      <router-link v-if="canManageWechat" to="enterprise-wechat"><el-link>{{$t('enterprise-wechat')}}</el-link></router-link>
    </el-card>
  </el-col>
</template>

<script>
import { hasAnyPermi } from '@/utils/project-option-card'

export default {
  name: "OtherSystemCard",
  model: {
    prop: 'project'
  },
  props: {
    project: {
      type: Object,
      default: {}
    }
  },
  computed: {
    canManageDing() {
      return hasAnyPermi(['ding:list'])
    },
    canManageFeishu() {
      return hasAnyPermi(['feishu:list'])
    },
    canManageWechat() {
      return hasAnyPermi(['wechat:list'])
    },
    cardVisible() {
      return hasAnyPermi(['ding:list', 'feishu:list', 'wechat:list'])
    }
  }
}
</script>
