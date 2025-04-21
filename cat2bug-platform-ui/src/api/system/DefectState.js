import request from '@/utils/request'

// 查询缺陷状态
export function getDefectState() {
  return request({
    url: '/system/defect/state',
    method: 'get'
  })
}
