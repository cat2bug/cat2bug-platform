import request from '@/utils/request'

// 查询项目通知配置详细
export function getDingConfig(projectId) {
  return request({
    url: '/im/project/ding/config?projectId=' + projectId,
    method: 'get'
  })
}

// 新增项目通知配置
export function saveDingConfig(data) {
  return request({
    url: '/im/project/ding/config',
    method: 'post',
    data: data
  })
}
