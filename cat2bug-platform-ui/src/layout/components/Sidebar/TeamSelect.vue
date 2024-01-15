<template>
  <div>
    <el-select class="team-select" :collapse="collapse" v-model="currentTeamId" :popper-append-to-body="false" @change="selectTeamChangedHandle">
      <template slot="prefix">
        <el-avatar :size="collapse?'small':'medium'"  shape="square" v-if="!currentTeam" :src="imgUrl('add.svg')" fit="cover"></el-avatar>
        <el-avatar :size="collapse?'small':'medium'"  shape="square" v-else-if="currentTeam.teamIcon" :src="iconUrl(currentTeam)" fit="cover"></el-avatar>
        <el-avatar :size="collapse?'small':'medium'" shape="square" v-else>{{currentTeam.name}}</el-avatar>
        <p class="prefix-team-name" v-if="!collapse">{{currentTeam?currentTeam.teamName: $t('team.create')}}</p>
      </template>
      <el-option
        style="height:80px;"
        v-for="team in teamList"
        :key="team.teamId"
        :label="team.teamName"
        :value="team.teamId">
        <el-avatar shape="square" v-if="team.teamIcon" :src="iconUrl(team)" fit="cover"></el-avatar>
        <el-avatar shape="square" v-else>{{team.teamName}}</el-avatar>
        <p>{{ team.teamName }}</p>
      </el-option>
      <el-option class="team-select-footer"
                 key="team-select-footer-create-team"
                 value="team-select-footer-create-team"
      >
        <el-button icon="el-icon-plus" style="width: 100%;" @click="addTeamHandle">{{$t('team.create')}}</el-button>
      </el-option>
    </el-select>
  </div>
</template>

<script>
import { myListTeam } from "@/api/system/team";
import { updateConfig } from "@/api/system/user-config";
import store from "@/store";

export default {
  name: "TeamSelect",
  data() {
    return {
      teamList:[],
      currentTeamId:null,
    }
  },
  props:{
    collapse: {
      type: Boolean,
      default: false
    },
  },
  watch: {
    '$store.state.user.config.currentTeamId': function() {
      this.getTeamList();
    }
  },
  computed:{
    teamId: function (){
      return this.$store.state.user.config.currentTeamId;
    },
    currentTeam: function() {
      for(let i in this.teamList){
        if(this.teamList[i].teamId==this.teamId) {
          return this.teamList[i];
        }
      }
      return null;
    },
    iconUrl: function (){
      return function (team){
        return process.env.VUE_APP_BASE_API + team.teamIcon
      }
    },
    imgUrl: function () {
      return function (index){
        return require('@/assets/images/'+index)
      }
    }
  },
  mounted() {
    this.getTeamList();
  },
  methods:{
    /** 获取当前人所能访问的团队列表 */
    getTeamList() {
      myListTeam().then(res=>{
        this.teamList = res.rows;
        this.selectTeam(this.teamId,false);
      });
    },
    addTeamHandle(e) {
      this.$router.push({name:'TeamAdd'});
    },
    /** 选择团队变化的处理 */
    selectTeamChangedHandle(currentTeamId,isRefresh){
      switch (currentTeamId){
        case 'team-select-footer-create-team':
          break;
        default:
          this.selectTeam(currentTeamId,true);
      }
    },
    /** 选择团队 */
    selectTeam(currentTeamId,isRefresh){
      if(currentTeamId == this.teamId) return;
      let _this=this;
      if(isRefresh) {
        updateConfig({
          currentTeamId: currentTeamId,
          currentProjectId: 0
        }).then(res => {
          let path = '/team/project';
          store.dispatch('GetInfo').then(() => {
            if (_this.$router.currentRoute.path == path) {
              _this.$router.go(0);
            } else {
              _this.$router.push({path: path});
            }
          });
        });
      }
    }
  }
}
</script>

<style lang="scss" scoped>
  ::v-deep .team-select {
    cursor: pointer;
    .el-input--prefix {
      margin: 10px;
      width: calc(100% - 20px);
      border-radius: 5px;

      .el-input__inner {
        background-color: #00000000;
        border-width: 0;
        height: 60px;
        visibility: hidden;
      }

      .el-input__prefix {
        display: flex;
        justify-content: flex-start;
        align-items: center;
        column-gap: 10px;
      }

      .prefix-team-name {
        text-overflow: ellipsis;
        overflow: hidden;
        white-space: nowrap;
        width: 100px;
        color: #303133;
        font-size: 16px;
        text-align: left;
      }
    }
    .el-input--prefix:hover {
      background-color: #88888833;
    }
  }

  ::v-deep .team-select[collapse="true"] {
    .el-input--prefix {
      height: 40px;
      width: 40px;
      margin-left: 7px;
      margin-right: 7px;
    }
    .el-input__inner {
      height: 40px;
      width: 40px;
    }
    .el-input__prefix {
      left: 6px;
    }
    .el-input__suffix {
      display: none;
    }
  }

  .el-select-dropdown__item {
    height: 80px;
    display: flex;
    justify-content: flex-start;
    align-items: center;
    column-gap: 10px;
  }
  .team-select-footer {
    height: 60px;
    border-top: #EBEEF5 1px solid;
    padding-top: 5px;
    background-color: #FFFFFF;
  }
  ::v-deep .el-select-dropdown__wrap {
    max-height: 100%;
  }
</style>
