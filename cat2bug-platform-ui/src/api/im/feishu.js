import request from '@/utils/request'

// 查询项目飞书通知配置详细
export function getFeishuConfig(projectId) {
  return request({
    url: '/im/project/feishu/config?projectId=' + projectId,
    method: 'get'
  })
}

// 新增项目飞书通知配置
export function saveFeishuConfig(data) {
  return request({
    url: '/im/project/feishu/config',
    method: 'post',
    data: data
  })
}

// 群发测试飞书通知
export function groupTestFeishuNotice(data) {
  return request({
    url: '/im/project/feishu/config/group-test',
    method: 'post',
    data: data
  })
}

// 单发测试飞书通知
export function singleTestFeishuNotice(data) {
  return request({
    url: '/im/project/feishu/config/single-test',
    method: 'post',
    data: data
  })
}
