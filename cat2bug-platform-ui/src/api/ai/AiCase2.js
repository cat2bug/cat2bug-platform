import request from "@/utils/request";

/** 制造缺陷关联版本 */
export function makeCaseDemand(data) {
  return request({
    url: '/cat2ai/api/case/demand',
    method: 'post',
    data: data,
    timeout: 300000
  })
}
