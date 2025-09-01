import request from '@/utils/request'

// 查询屏幕尺寸列表
export function listScreenSize(query) {
  return request({
    url: '/system/ScreenSize/list',
    method: 'get',
    params: query
  })
}

// 查询屏幕尺寸详细
export function getScreenSize(screenSizeId) {
  return request({
    url: '/system/ScreenSize/' + screenSizeId,
    method: 'get'
  })
}

// 新增屏幕尺寸
export function addScreenSize(data) {
  return request({
    url: '/system/ScreenSize',
    method: 'post',
    data: data
  })
}

// 修改屏幕尺寸
export function updateScreenSize(data) {
  return request({
    url: '/system/ScreenSize',
    method: 'put',
    data: data
  })
}

// 删除屏幕尺寸
export function delScreenSize(screenSizeId) {
  return request({
    url: '/system/ScreenSize/' + screenSizeId,
    method: 'delete'
  })
}
