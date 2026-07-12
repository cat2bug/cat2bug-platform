<template>
  <el-card class="box-card">
    <template #header>
      <div class="clearfix">
        <legacy-icon icon="el-icon-s-flag"></legacy-icon>
        <div>
          <span>{{$t('team.info')}}</span>
          <span>{{$t('team.base-info-describe')}}</span>
        </div>
      </div>
    </template>
    <router-link to="team-base-info" v-hasPermi="['system:team:edit']"><el-link>{{$t('team.base-info')}}</el-link></router-link>
    <el-link data-cat2bug-kbd-key="delete-team" @click="handleDelete" type="danger" v-hasPermi="['system:team:remove']">{{$t('team.delete')}}</el-link>
  </el-card>
</template>

<script>
import {strFormat} from "@/utils";
import i18n from "@/utils/i18n/i18n";
import {delProject} from "@/api/system/project";
import {delTeam} from "@/api/system/team";
import { storeToRefs } from 'pinia';
import { useUserStore } from '@/stores/user'

export default {
  name: "TeamBaseInfoCard",
  setup() {
    const userStore = useUserStore()
    const { config } = storeToRefs(userStore)
    return { config }
  },
  computed: {
  },
  methods: {
    /** 获取团队id */
    getTeamId() {
      return this.config.currentTeamId;
    },
    /** 删除按钮操作 */
    handleDelete() {
      let _this=this;
      this.$modal.prompt(
        this.$i18n.t('team.is-delete-team'),
        i18n.t('prompted').toString(),
      {
        confirmButtonText: i18n.t('delete').toString(),
        cancelButtonText: i18n.t('cancel').toString(),
        inputPlaceholder: i18n.t('please-enter-your-password').toString(),
        confirmButtonClass: 'delete-button',
        inputType: 'password',
        type: "warning",
        }).then(function(res) {
        delTeam(_this.getTeamId()).then(()=>{
          _this.$modal.msgSuccess(_this.$i18n.t('delete-success'));
          useUserStore().GetInfo().then(() => {
            _this.$router.push({path:'/team/index'});
          });
        });
      }).catch(() => {});
    },
  }
}
</script>
