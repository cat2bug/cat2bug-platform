import request from '@/utils/request'

// 项目 MCP 设置（开关 + 开放模块）
export function getProjectMcp(projectId) {
  return request({
    url: '/system/project/' + projectId + '/mcp',
    method: 'get'
  })
}

export function updateProjectMcp(projectId, data) {
  return request({
    url: '/system/project/' + projectId + '/mcp',
    method: 'put',
    data
  })
}
