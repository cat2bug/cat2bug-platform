import Cookies from 'js-cookie'

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
