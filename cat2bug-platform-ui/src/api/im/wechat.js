import request from '@/utils/request'

// 查询项目通知配置详细
export function getWeChatConfig(projectId) {
  return request({
    url: '/im/project/wechat/config?projectId=' + projectId,
    method: 'get'
  })
}

// 新增项目通知配置
export function saveWeChatConfig(data) {
  return request({
    url: '/im/project/wechat/config',
    method: 'post',
    data: data
  })
}

// 群发测试企业微信通知
export function groupTestWeChatNotice(data) {
  return request({
    url: '/im/project/wechat/config/group-test',
    method: 'post',
    data: data
  })
}

// 单发测试企业微信通知
export function singleTestWeChatNotice(data) {
  return request({
    url: '/im/project/wechat/config/single-test',
    method: 'post',
    data: data
  })
}
