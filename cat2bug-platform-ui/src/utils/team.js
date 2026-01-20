import store from "@/store";

/** 团队锁定时是否锁定当前页面 */
export function isLockPath (path){
  const lockTeamErrorPath = '/error/team-lock';  // 锁定团队的页面路径
  // 如果路径是报错路径或项目没被禁用，跳到报错界面
  if (!path || path === lockTeamErrorPath || !store.state.user.config.currentTeamLock) {
    return false;
  }

  const paths = path.split('/');
  const basePath = paths[0]+(paths.length>1?paths[1]:'');
  return basePath==='system' || basePath==='team';
}
