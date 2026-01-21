import request from '@/utils/request'

// 查询项目列表
export function listProject(query) {
  return request({
    url: '/admin/project/list',
    method: 'get',
    params: query
  })
}

// 禁用或解锁项目
export function lockProject(projectId, data) {
  return request({
    url: '/admin/project/'+projectId+'/lock',
    method: 'put',
    data: data
  })
}
