<template>
  <el-card class="box-card">
    <div slot="header" class="clearfix">
      <i class="el-icon-s-flag"></i>
      <div>
        <span>{{$t('project.other-system')}}</span>
        <span>{{$t('project.other-system-describe')}}</span>
      </div>
    </div>
    <router-link to="ding" v-hasPermi="['ding:list']"><el-link>{{$t('ding')}}</el-link></router-link>
    <router-link to="enterprise-wechat" v-hasPermi="['wechat:list']"><el-link>{{$t('enterprise-wechat')}}</el-link></router-link>
  </el-card>
</template>

<script>
import {strFormat} from "@/utils";
import {delProject} from "@/api/system/project";
import i18n from "@/utils/i18n/i18n";
import store from "@/store";

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
