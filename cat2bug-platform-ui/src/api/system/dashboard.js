import request from '@/utils/request'

// 查询测试用例列表
export function defectLine(projectId, query) {
  return request({
    url: '/system/dashboard/'+projectId+'/defectLine',
    method: 'get',
    params: query
  })
}
