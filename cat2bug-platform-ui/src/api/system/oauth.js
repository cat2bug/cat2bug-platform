import request from '@/utils/request'

// OAuth 授权确认页：校验请求并取可选团队 / 项目
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
