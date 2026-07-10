import request from '@/utils/request'

/** Build full invite URL for hash-mode router */
export function buildInviteUrl(token) {
  let path = window.location.pathname || '/'
  // dev / 部分部署会把入口挂在 /index，避免生成 .../index#/invite/...
  path = path.replace(/\/index\.html$/i, '').replace(/\/index$/i, '')
  path = path.replace(/\/+$/, '')
  if (!path) {
    return `${window.location.origin}/#/invite/${token}`
  }
  return `${window.location.origin}${path}/#/invite/${token}`
}

// --- Team invite links ---

export function createTeamInviteLink(teamId, data) {
  return request({
    url: `/system/team/${teamId}/invite-link`,
    method: 'post',
    data
  })
}

export function listTeamInviteLinks(teamId) {
  return request({
    url: `/system/team/${teamId}/invite-links`,
    method: 'get'
  })
}

export function revokeTeamInviteLink(teamId, inviteId) {
  return request({
    url: `/system/team/${teamId}/invite-link/${inviteId}`,
    method: 'delete'
  })
}

// --- Project invite links ---

export function createProjectInviteLink(projectId, data) {
  return request({
    url: `/system/project/${projectId}/invite-link`,
    method: 'post',
    data
  })
}

export function listProjectInviteLinks(projectId) {
  return request({
    url: `/system/project/${projectId}/invite-links`,
    method: 'get'
  })
}

export function revokeProjectInviteLink(projectId, inviteId) {
  return request({
    url: `/system/project/${projectId}/invite-link/${inviteId}`,
    method: 'delete'
  })
}

// --- Public invite ---

export function previewInvite(token) {
  return request({
    url: `/invite/${token}/preview`,
    method: 'get'
  })
}

export function acceptInvite(token) {
  return request({
    url: `/invite/${token}/accept`,
    method: 'post'
  })
}
