import request from "@/utils/request";

export function listCase(content) {
  return request({
    url: '/cloud/api/case/list?content='+content,
    method: 'get',
    timeout: 60000
  })
}
