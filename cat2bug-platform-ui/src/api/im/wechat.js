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
