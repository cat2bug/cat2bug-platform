import request from '@/utils/request'

// 查询OpenAI账号列表
export function listAccount(query) {
  return request({
    url: '/system/ai/account/list',
    method: 'get',
    params: query
  })
}

// 查询OpenAI账号详细
export function getAccount(accountId) {
  return request({
    url: '/system/ai/account/' + accountId,
    method: 'get'
  })
}

// 新增OpenAI账号
export function addAccount(data) {
  return request({
    url: '/system/ai/account',
    method: 'post',
    data: data
  })
}

// 修改OpenAI账号
export function updateAccount(data) {
  return request({
    url: '/system/ai/account',
    method: 'put',
    data: data
  })
}

// 删除OpenAI账号
export function delAccount(accountId) {
  return request({
    url: '/system/ai/account/' + accountId,
    method: 'delete'
  })
}
