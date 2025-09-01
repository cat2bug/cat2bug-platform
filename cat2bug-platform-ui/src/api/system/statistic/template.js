import request from '@/utils/request'

// 查询用户统计模版列表
export function listStatistic(query) {
  return request({
    url: '/system/statistic/list',
    method: 'get',
    params: query
  })
}

// 查询用户统计模版详细
export function getStatistic(statisticTemplateId) {
  return request({
    url: '/system/statistic/' + statisticTemplateId,
    method: 'get'
  })
}

// 新增用户统计模版
export function addStatistic(data) {
  return request({
    url: '/system/statistic',
    method: 'post',
    data: data
  })
}

// 修改用户统计模版
export function updateStatistic(data) {
  return request({
    url: '/system/statistic',
    method: 'put',
    data: data
  })
}

// 删除用户统计模版
export function delStatistic(statisticTemplateId) {
  return request({
    url: '/system/statistic/' + statisticTemplateId,
    method: 'delete'
  })
}
