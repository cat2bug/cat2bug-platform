import request from '@/utils/request'

export function startCaseReview(projectId, caseIds) {
  return request({
    url: '/system/case-review',
    method: 'post',
    data: { projectId, caseIds },
    silentError: true
  })
}

export function listCaseReviews(projectId) {
  return request({
    url: '/system/case-review',
    method: 'get',
    params: { projectId },
    // 工作台小组件拉取失败不弹全局「接口不存在」
    silentError: true
  })
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
