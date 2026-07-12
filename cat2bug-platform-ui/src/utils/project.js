import Cookies from 'js-cookie'
import { pinia } from '@/store/pinia.js'
import { useUserStore } from '@/stores/user'

const projectIdKey = 'Current-Project-Id'

export function getCurrentProjectId() {
  return Cookies.get(projectIdKey)
}

export function setCurrentProjectId(projectId) {
  return Cookies.set(projectIdKey, projectId)
}

export function removeCurrentProjectId() {
  return Cookies.remove(projectIdKey)
}

/** 项目锁定时是否锁定当前页面 */
export function isLockProjectPath (path){
  const lockTeamErrorPath = '/error/project-lock';  // 锁定团队的页面路径
  // 如果路径是报错路径或项目没被禁用，跳到报错界面
  const userStore = useUserStore(pinia)
  if (!path || path === lockTeamErrorPath || !userStore.config.currentProjectLock) {
    return false;
  }
  const paths = path.split('/');
  const basePath = paths[0]+(paths.length>1?paths[1]:'');
  return basePath==='project';
}

