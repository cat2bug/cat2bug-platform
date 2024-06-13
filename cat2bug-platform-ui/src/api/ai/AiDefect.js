import request from "@/utils/request";

/** 制造缺陷 */
export function makeDefect(data) {
  return request({
    url: '/ai/defect',
    method: 'post',
    data: data,
    timeout: 300000
  })
}

/** 制造缺陷标题 */
export function makeDefectTitle(describe) {
  return request({
    url: '/ai/defect/title',
    method: 'post',
    data: describe,
    timeout: 60000
  })
}

/** 制造缺陷关联交付物 */
export function makeDefectModule(describe) {
  return request({
    url: '/ai/defect/module',
    method: 'post',
    data: describe,
    timeout: 60000
  })
}
