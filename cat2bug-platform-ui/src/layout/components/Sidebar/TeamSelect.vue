<template>
  <div>
    <el-select class="team-select" :collapse="collapse" v-model="currentTeamId" placeholder="请选择" @change="selectTeamChangedHandle">
      <template slot="prefix">
          <el-avatar :size="collapse?'small':'medium'"  shape="square" v-if="currentTeam.teamIcon" :src="iconUrl(currentTeam)" fit="cover"></el-avatar>
          <el-avatar :size="collapse?'small':'medium'" shape="square" v-else>{{currentTeam.name}}</el-avatar>
          <p class="prefix-team-name" v-if="!collapse">{{currentTeam.teamName}}</p>
      </template>
      <el-option
        v-for="team in teamList"
        :key="team.teamId"
        :label="team.teamName"
        :value="team.teamId">
        <el-avatar shape="square" v-if="team.teamIcon" :src="iconUrl(team)" fit="cover"></el-avatar>
        <el-avatar shape="square" v-else>{{team.teamName}}</el-avatar>
        <p>{{ team.teamName }}</p>
      </el-option>
    </el-select>
  </div>
</template>

<script>
import { myListTeam } from "@/api/system/team";
import { updateConfig } from "@/api/system/user-config";

export default {
  name: "TeamSelect",
  data() {
    return {
      teamList:[],
      currentTeamId: null,
      currentTeam: {},
    }
  },
  props:{
    collapse: {
      type: Boolean,
      default: false
    }
  },
  computed:{
    iconUrl: function (){
      return function (team){
        return process.env.VUE_APP_BASE_API + team.teamIcon
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
        // 如果登陆数据中存在当前团队id，默认选择登陆数据中的团队id；否则选择已有团队的第一个
        if(this.$store.state.user.config && this.$store.state.user.config.currentTeamId){
          this.currentTeamId = this.$store.state.user.config.currentTeamId;
        } else if(this.teamList && this.teamList.length>0) {
          this.currentTeamId = res.rows[0].teamId;
        }
        this.selectTeamChangedHandle(this.currentTeamId);
      });
    },
    /** 选择团队变化的处理 */
    selectTeamChangedHandle(currentTeamId){
      for(let i in this.teamList){
        if(this.teamList[i].teamId==currentTeamId) {
          this.currentTeam=this.teamList[i];
          // 存储到远程服务器
          updateConfig({
            currentTeamId: currentTeamId
          }).then(res=>{});
          break;
        }
      }
    }
  }
}
</script>

<style lang="scss" scoped>
  ::v-deep .team-select {
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
</style>
