import request from '@/utils/request'

export function caseStatistics(projectId) {
  return request({
    url: '/system/dashboard/'+projectId+'/case',
    method: 'get',
  })
}

export function defectStatistics(projectId) {
  return request({
    url: '/system/dashboard/'+projectId+'/defect',
    method: 'get',
  })
}

export function moduleStatistics(projectId) {
  return request({
    url: '/system/dashboard/'+projectId+'/module',
    method: 'get',
  })
}

export function reportStatistics(projectId) {
  return request({
    url: '/system/dashboard/'+projectId+'/report',
    method: 'get',
  })
}

export function documentStatistics(projectId) {
  return request({
    url: '/system/dashboard/'+projectId+'/document',
    method: 'get',
  })
}

export function memberStatistics(projectId) {
  return request({
    url: '/system/dashboard/'+projectId+'/member',
    method: 'get',
  })
}

// 查询测试用例列表
export function defectLine(projectId, query) {
  return request({
    url: '/system/dashboard/'+projectId+'/defect-line',
    method: 'get',
    params: query
  })
}

export function actionList(projectId, query) {
  return request({
    url: '/system/dashboard/'+projectId+'/actions',
    method: 'get',
    params: query
  })
}

export function planBurndown(projectId, planId) {
  return request({
    url: '/system/dashboard/'+projectId+'/plan/'+planId,
    method: 'get',
  })
}

export function memberRankOfDefects(projectId) {
  return request({
    url: '/system/dashboard/'+projectId+'/member-defect',
    method: 'get',
  })
}

export function memberLineOfDefects(projectId, query) {
  return request({
    url: '/system/dashboard/'+projectId+'/member-defect-line',
    method: 'get',
    params: query
  })
}
