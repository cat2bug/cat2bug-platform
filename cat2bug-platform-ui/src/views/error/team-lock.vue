<template>
  <error-state-shell
    code="TEAM"
    illustration="disabled"
    :title="$t('error.team-lock.title')"
  >
    <template #description>
      <p>{{ $t('error.team-lock.subtitle') }}</p>
      <p v-if="lockRemark">{{ lockRemark }}</p>
      <p>{{ $t('error.team-lock.attention') }}</p>
    </template>
    <template #actions>
      <el-popover placement="bottom" width="300" trigger="hover">
        <div class="team-menu">
          <div
            v-for="(team, teamIndex) in teamList"
            :key="teamIndex"
            class="team-row"
            :style="{ cursor: team.lock ? 'not-allowed' : 'pointer' }"
            @click="handleTeamClick(team)"
          >
            <el-avatar
              v-if="team.teamIcon"
              class="team-row-icon"
              shape="square"
              :src="teamAvatarSrc(team)"
              fit="cover"
              @error="onTeamAvatarError(team)"
            />
            <el-avatar v-else class="team-row-icon" shape="square">{{ team.teamName }}</el-avatar>
            <p>
              {{ team.teamName }}
              <el-tag v-if="team.lock" type="danger" effect="dark" size="small">{{ $t('locked') }}</el-tag>
            </p>
          </div>
        </div>
        <template #reference>
          <el-button type="primary">{{ $t('team.select') }}</el-button>
        </template>
      </el-popover>
    </template>
  </error-state-shell>
</template>

<script>
import { myListTeam } from '@/api/system/team'
import { updateConfig } from '@/api/system/user-config'
import { resolveTeamIconUrl } from '@/utils/upload-asset'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'
import ErrorStateShell from '@/components/ErrorStateShell'

export default {
  name: 'TeamLockError',
  components: { ErrorStateShell },
  setup() {
    const userStore = useUserStore()
    const { config } = storeToRefs(userStore)
    return { config }
  },
  data() {
    return {
      teamList: [],
      failedTeamIcons: {}
    }
  },
  computed: {
    lockRemark() {
      return this.config.currentTeamLockRemark
    },
    teamId() {
      return this.config.currentTeamId
    }
  },
  mounted() {
    this.getTeamList()
  },
  methods: {
    teamAvatarSrc(team) {
      if (!team || !team.teamIcon) {
        return ''
      }
      if (this.failedTeamIcons[team.teamId]) {
        return resolveTeamIconUrl(null)
      }
      return resolveTeamIconUrl(team.teamIcon)
    },
    onTeamAvatarError(team) {
      if (team && team.teamId != null) {
        this.failedTeamIcons = { ...this.failedTeamIcons, [team.teamId]: true }
      }
    },
    getTeamList() {
      myListTeam().then((res) => {
        this.teamList = res.rows
      })
    },
    handleTeamClick(team) {
      if (team.teamId == this.teamId) return
      if (team.lock) return
      updateConfig({
        currentTeamId: team.teamId,
        currentProjectId: 0
      }).then(() => {
        const userStore = useUserStore()
        userStore.GetInfo().then(() => {
          this.$router.push({ path: '/team/project-list' })
        })
      })
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

  p {
    margin: 0;
  }
}
.team-row:hover {
  background-color: var(--el-fill-color-light, rgb(236, 245, 255));
}
</style>
