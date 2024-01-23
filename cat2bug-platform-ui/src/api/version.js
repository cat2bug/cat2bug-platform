import request from '@/utils/request'

// 获取系统版本
export const getVersion = () => {
  return request({
    url: '/version',
    method: 'get'
  })
}
