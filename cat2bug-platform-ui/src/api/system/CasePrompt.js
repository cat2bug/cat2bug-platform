import request from '@/utils/request'

// 查询测试用例提示词列表
export function listCasePrompt(query) {
  return request({
    url: '/system/CasePrompt/list',
    method: 'get',
    params: query
  })
}

// 查询测试用例提示词详细
export function getCasePrompt(casePromptId) {
  return request({
    url: '/system/CasePrompt/' + casePromptId,
    method: 'get'
  })
}

// 新增测试用例提示词
export function addCasePrompt(data) {
  return request({
    url: '/system/CasePrompt',
    method: 'post',
    data: data
  })
}

// 修改测试用例提示词
export function updateCasePrompt(data) {
  return request({
    url: '/system/CasePrompt',
    method: 'put',
    data: data
  })
}

// 删除测试用例提示词
export function delCasePrompt(casePromptId) {
  return request({
    url: '/system/CasePrompt/' + casePromptId,
    method: 'delete'
  })
}
