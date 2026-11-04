import request from '@/utils/request'

export function startCaseReview(projectId, caseIds) {
  return request({
    url: '/system/case-review',
    method: 'post',
    data: { projectId, caseIds },
    silentError: true
  })
}

/**
 * @param {number|string} projectId
 * @param {object} [query] status, createTimeBegin, createTimeEnd, createBy, pageNum, pageSize
 */
export function listCaseReviews(projectId, query = {}) {
  const params = { ...(query || {}) }
  if (projectId != null && projectId !== '') {
    params.projectId = projectId
  }
  return request({
    url: '/system/case-review',
    method: 'get',
    params,
    // 工作台小组件拉取失败不弹全局「接口不存在」
    silentError: true
  })
}

/** 兼容 TableDataInfo.rows 与旧版 data[] */
export function extractCaseReviewList(res) {
  if (!res) return { rows: [], total: 0 }
  if (Array.isArray(res.rows)) {
    return { rows: res.rows, total: Number(res.total) || res.rows.length }
  }
  const data = res.data
  if (Array.isArray(data)) {
    return { rows: data, total: data.length }
  }
  if (data && typeof data === 'object' && Array.isArray(data.rows)) {
    return { rows: data.rows, total: Number(data.total) || data.rows.length }
  }
  return { rows: [], total: 0 }
}

export function submitCaseReview(projectId, items) {
  return request({
    url: '/system/case-review/submit',
    method: 'post',
    data: { projectId, items }
  })
}

export function getCaseReview(reviewId) {
  return request({
    url: '/system/case-review/' + reviewId,
    method: 'get'
  })
}

export function commentCaseReview(reviewId, item) {
  return request({
    url: '/system/case-review/' + reviewId + '/item',
    method: 'put',
    data: item
  })
}

export function completeCaseReview(reviewId) {
  return request({
    url: '/system/case-review/' + reviewId + '/complete',
    method: 'post'
  })
}

export function updateCaseReviewReport(reviewId, reportMarkdown) {
  return request({
    url: '/system/case-review/' + reviewId + '/report',
    method: 'put',
    data: { reportMarkdown }
  })
}
