import request from '@/utils/request'

// 当前用户的访问令牌（含 OAuth 授权条目）
export function listAccessTokens() {
  return request({
    url: '/system/user/profile/tokens',
    method: 'get'
  })
}

export function createAccessToken(data) {
  return request({
    url: '/system/user/profile/tokens',
    method: 'post',
    data
  })
}

export function revokeAccessToken(tokenId) {
  return request({
    url: '/system/user/profile/tokens/' + tokenId,
    method: 'delete'
  })
}

// OAuth 授权确认页
export function checkOAuthAuthorize(data) {
  return request({
    url: '/oauth/authorize/check',
    method: 'post',
    data,
    silentError: true
  })
}

export function decideOAuthAuthorize(data) {
  return request({
    url: '/oauth/authorize/decision',
    method: 'post',
    data
  })
}
