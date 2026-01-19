<template>
  <div class="wscn-http404-container">
    <div class="wscn-http404">
      <div class="pic-404">
        <img class="pic-404__parent" src="@/assets/images/lock_flag.png" alt="lock">
        <img class="pic-404__child left" src="@/assets/404_images/404_cloud.png" alt="404">
        <img class="pic-404__child mid" src="@/assets/404_images/404_cloud.png" alt="404">
        <img class="pic-404__child right" src="@/assets/404_images/404_cloud.png" alt="404">
      </div>
      <div class="bullshit">
        <div class="bullshit__oops">{{ $t('error.team-lock.title') }}</div>
        <div class="bullshit__headline">{{ $t('error.team-lock.subtitle') }}</div>
        <div class="bullshit__info" style="margin-bottom: 10px;">{{ lockRemark }}</div>
        <div class="bullshit__info">{{ $t('error.team-lock.attention') }}</div>
        <!-- 切换团队下拉框-->
        <el-popover
          placement="bottom"
          width="300"
          trigger="hover">
          <div class="team-menu">
            <div class="team-row" :style="{cursor:team.lock?'not-allowed':'pointer'}" v-for="(team, teamIndex) in teamList" :key="teamIndex" @click="handleTeamClick(team)">
              <el-avatar class="team-row-icon" shape="square" v-if="team.teamIcon" :src="iconUrl(team)" fit="cover"></el-avatar>
              <el-avatar class="team-row-icon" shape="square" v-else>{{team.teamName}}</el-avatar>
              <p>
                {{ team.teamName }}
                <el-tag v-if="team.lock" type="danger" effect="dark" size="mini">{{ $t('locked') }}</el-tag>
              </p>
            </div>
          </div>
          <el-button slot="reference" type="primary" round>{{ $t('team.select') }}</el-button>
        </el-popover>
      </div>
    </div>
  </div>
</template>

<script>
import {myListTeam} from "@/api/system/team";
import {updateConfig} from "@/api/system/user-config";
import store from "@/store";

export default {
  name: "TeamLockError",
  data() {
    return {
      teamList:[],
    }
  },
  computed: {
    lockRemark() {
      return this.$store.state.user.config.currentTeamLockRemark;
    },
    iconUrl: function (){
      return function (team){
        return process.env.VUE_APP_BASE_API + team.teamIcon
      }
    },
    teamId() {
      return this.$store.state.user.config.currentTeamId;
    },
  },
  mounted() {
    this.getTeamList();
  },
  methods: {
    /** 获取当前人所能访问的团队列表 */
    getTeamList() {
      myListTeam().then(res=>{
        this.teamList = res.rows;
      });
    },
    handleTeamClick(team) {
      if(team.teamId == this.teamId) return;
      updateConfig({
        currentTeamId: team.teamId,
        currentProjectId: 0
      }).then(res => {
        store.dispatch('GetInfo').then((res1) => {
          this.$router.push({path: '/team/project-list'});
        });
      });
    }
  }
}
</script>

<style lang="scss" scoped>
.team-menu {
  width: 100%;
  display: inline-flex;
  flex-direction: column;
  gap: 5px;
}
.team-row {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  padding: 5px 10px;
  border-radius: 5px;
  &:first-child {
    flex-shrink: 0;
  }
  &:last-child {
    flex: 1;
  }
  .team-row-icon {
    flex-shrink: 0;
  }
}
.team-row:hover {
  background-color: rgb(236, 245, 255);
}
.wscn-http404-container{
  transform: translate(-50%,-50%);
  position: absolute;
  top: 40%;
  left: 50%;
}
.wscn-http404 {
  position: relative;
  width: 1200px;
  padding: 0 50px;
  overflow: hidden;
.pic-404 {
  position: relative;
  float: left;
  width: 600px;
  overflow: hidden;
&__parent {
   width: 100%;
 }
&__child {
   position: absolute;
&.left {
   width: 80px;
   top: 17px;
   left: 220px;
   opacity: 0;
   animation-name: cloudLeft;
   animation-duration: 2s;
   animation-timing-function: linear;
   animation-fill-mode: forwards;
   animation-delay: 1s;
 }
&.mid {
   width: 46px;
   top: 10px;
   left: 420px;
   opacity: 0;
   animation-name: cloudMid;
   animation-duration: 2s;
   animation-timing-function: linear;
   animation-fill-mode: forwards;
   animation-delay: 1.2s;
 }
&.right {
   width: 62px;
   top: 100px;
   left: 500px;
   opacity: 0;
   animation-name: cloudRight;
   animation-duration: 2s;
   animation-timing-function: linear;
   animation-fill-mode: forwards;
   animation-delay: 1s;
 }
@keyframes cloudLeft {
  0% {
    top: 17px;
    left: 220px;
    opacity: 0;
  }
  20% {
    top: 33px;
    left: 188px;
    opacity: 1;
  }
  80% {
    top: 81px;
    left: 92px;
    opacity: 1;
  }
  100% {
    top: 97px;
    left: 60px;
    opacity: 0;
  }
}
@keyframes cloudMid {
  0% {
    top: 10px;
    left: 420px;
    opacity: 0;
  }
  20% {
    top: 40px;
    left: 360px;
    opacity: 1;
  }
  70% {
    top: 130px;
    left: 180px;
    opacity: 1;
  }
  100% {
    top: 160px;
    left: 120px;
    opacity: 0;
  }
}
@keyframes cloudRight {
  0% {
    top: 100px;
    left: 500px;
    opacity: 0;
  }
  20% {
    top: 120px;
    left: 460px;
    opacity: 1;
  }
  80% {
    top: 180px;
    left: 340px;
    opacity: 1;
  }
  100% {
    top: 200px;
    left: 300px;
    opacity: 0;
  }
}
}
}
.bullshit {
  position: relative;
  float: left;
  width: 350px;
  padding: 30px 0;
  overflow: hidden;
&__oops {
   font-size: 32px;
   font-weight: bold;
   line-height: 40px;
   color: #1482f0;
   opacity: 0;
   margin-bottom: 20px;
   animation-name: slideUp;
   animation-duration: 0.5s;
   animation-fill-mode: forwards;
 }
&__headline {
   font-size: 20px;
   line-height: 24px;
   color: #222;
   font-weight: bold;
   opacity: 0;
   margin-bottom: 10px;
   animation-name: slideUp;
   animation-duration: 0.5s;
   animation-delay: 0.1s;
   animation-fill-mode: forwards;
 }
&__info {
   font-size: 13px;
   line-height: 21px;
   color: grey;
   opacity: 0;
   margin-bottom: 30px;
   animation-name: slideUp;
   animation-duration: 0.5s;
   animation-delay: 0.2s;
   animation-fill-mode: forwards;
 }
&__return-home {
   display: block;
   float: left;
   width: 110px;
   height: 36px;
   background: #1482f0;
   border-radius: 100px;
   text-align: center;
   color: #ffffff;
   opacity: 0;
   font-size: 14px;
   line-height: 36px;
   cursor: pointer;
   animation-name: slideUp;
   animation-duration: 0.5s;
   animation-delay: 0.3s;
   animation-fill-mode: forwards;
 }
@keyframes slideUp {
  0% {
    transform: translateY(60px);
    opacity: 0;
  }
  100% {
    transform: translateY(0);
    opacity: 1;
  }
}
}
}
</style>
