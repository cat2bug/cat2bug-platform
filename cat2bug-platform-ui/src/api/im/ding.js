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

// 群发测试钉钉通知
export function groupTestDingNotice(data) {
  return request({
    url: '/im/project/ding/config/group-test',
    method: 'post',
    data: data
  })
}

// 单发测试钉钉通知
export function singleTestDingNotice(data) {
  return request({
    url: '/im/project/ding/config/single-test',
    method: 'post',
    data: data
  })
}
