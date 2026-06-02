<template>
  <el-col v-if="cardVisible" :span="6">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <i class="el-icon-s-flag"></i>
        <div>
          <span>{{$t('project.info')}}</span>
          <span>{{$t('project.base-info-describe')}}</span>
        </div>
      </div>
      <router-link v-if="canEditProject" to="project-base-info"><el-link>{{$t('project.base-info')}}</el-link></router-link>
      <router-link v-if="canManageApi" to="project-api"><el-link>{{$t('project.api-key')}}</el-link></router-link>
      <router-link v-if="canPushProject" to="/project/push"><el-link>{{$t('project.push')}}</el-link></router-link>
      <el-link v-if="canRemoveProject" @click="handleDelete" type="danger">{{$t('project.delete')}}</el-link>
    </el-card>
  </el-col>
</template>

<script>
import {strFormat} from "@/utils";
import {delProject} from "@/api/system/project";
import i18n from "@/utils/i18n/i18n";
import store from "@/store";
import { hasAnyPermi } from '@/utils/project-option-card'

export default {
  name: "ProjectBaseInfoCard",
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
    canEditProject() {
      return hasAnyPermi(['system:project:edit'])
    },
    canManageApi() {
      return hasAnyPermi(['system:api:list'])
    },
    canPushProject() {
      return hasAnyPermi(['system:project:push'])
    },
    canRemoveProject() {
      return hasAnyPermi(['system:project:remove'])
    },
    cardVisible() {
      return hasAnyPermi([
        'system:project:edit',
        'system:api:list',
        'system:project:push',
        'system:project:remove'
      ])
    }
  },
  methods: {
    /** 删除按钮操作 */
    handleDelete() {
      let msg = this.$i18n.t('project.is-delete-project');
      let _this=this;
      this.$modal.prompt(strFormat(msg,'[ '+this.project.projectName+' ]'),
        i18n.t('prompted').toString(),
        {
          confirmButtonText: i18n.t('delete').toString(),
          cancelButtonText: i18n.t('cancel').toString(),
          inputPlaceholder: i18n.t('please-enter-your-password').toString(),
          confirmButtonClass: 'delete-button',
          inputType: 'password',
          type: "warning",
      }).then(function(res) {
        delProject(_this.project.projectId, res.value).then(()=>{
          _this.$modal.msgSuccess(_this.$i18n.t('delete-success'));
          store.dispatch('GetInfo').then(() => {
            _this.$router.push({path:'/team/index'});
          });
        });
      }).catch(() => {});
    },
  }
}
</script>
