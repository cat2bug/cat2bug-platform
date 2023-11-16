import cache from '@/plugins/cache.js'
/**
 * 我的通用方法
 */
export default {
  setCurrentTeam(team) {
    cache.local.setJSON('currentTeam', team);
  }
}
