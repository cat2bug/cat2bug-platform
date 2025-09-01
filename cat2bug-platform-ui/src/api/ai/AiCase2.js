import request from "@/utils/request";

/** 制造缺陷描述 */
export function makeCaseDemand(data) {
  return request({
    url: '/cat2ai/api/case/demand',
    method: 'post',
    data: data,
    timeout: 300000
  })
}
/** 制造缺陷脑图 */
export function makeCaseMind(data) {
  return request({
    url: '/cat2ai/api/case/mind',
    method: 'post',
    data: data,
    timeout: 300000
  })
}
/** 制造缺陷列表 */
export function makeCaseList(data) {
  return request({
    url: '/cat2ai/api/case/list',
    method: 'post',
    data: data,
    timeout: 900000
  })
}
