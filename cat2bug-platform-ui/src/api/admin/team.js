import request from '@/utils/request'

// 查询团队列表
export function listTeam(query) {
  return request({
    url: '/admin/team/list',
    method: 'get',
    params: query
  })
}

// 禁用或解锁团队
export function lockTeam(teamId, data) {
  return request({
    url: '/admin/team/'+teamId+'/lock',
    method: 'put',
    data: data
  })
}
