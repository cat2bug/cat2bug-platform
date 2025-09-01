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

/** 制造缺陷名称 */
export function makeDefectTitle(data) {
  return request({
    url: '/ai/defect/title',
    method: 'post',
    data: data,
    timeout: 300000
  })
}

/** 制造缺陷关联交付物 */
export function makeDefectModule(data) {
  return request({
    url: '/ai/defect/module',
    method: 'post',
    data: data,
    timeout: 300000
  })
}

/** 制造缺陷关联类型 */
export function makeDefectType(data) {
  return request({
    url: '/ai/defect/type',
    method: 'post',
    data: data,
    timeout: 300000
  })
}

/** 制造缺陷关联成员 */
export function makeDefectMember(data) {
  return request({
    url: '/ai/defect/member',
    method: 'post',
    data: data,
    timeout: 300000
  })
}

/** 制造缺陷关联版本 */
export function makeDefectVersion(data) {
  return request({
    url: '/ai/defect/version',
    method: 'post',
    timeout: 300000,
    data: data
  })
}
