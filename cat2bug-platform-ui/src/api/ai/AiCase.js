import request from "@/utils/request";

/** 制造缺陷关联版本 */
export function makeCaseList(data) {
  return request({
    url: '/ai/case/list',
    method: 'post',
    data: data,
    timeout: 300000
  })
}
