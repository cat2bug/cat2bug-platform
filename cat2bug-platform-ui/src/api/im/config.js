import request from '@/utils/request'

// 查询用户消息配置详细
export function getIMConfig(projectId, memberId) {
  return request({
    url:`/im/config?projectId=${projectId}&memberId=${memberId}`,
    method: 'get'
  })
}

// 新增用户消息配置
export function saveConfig(data) {
  return request({
    url: '/im/config',
    method: 'post',
    data: data
  })
}

// 删除用户消息配置
export function delConfig(imConfigId) {
  return request({
    url: '/im/config/' + imConfigId,
    method: 'delete'
  })
}
