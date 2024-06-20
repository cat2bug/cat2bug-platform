import request from '@/utils/request'

// 查询文档列表
export function listDocument(query) {
  return request({
    url: '/system/document/list',
    method: 'get',
    params: query
  })
}

// 查询文档详细
export function getDocument(docId) {
  return request({
    url: '/system/document/' + docId,
    method: 'get'
  })
}

// 新增文档
export function addDocument(data) {
  return request({
    url: '/system/document',
    method: 'post',
    data: data
  })
}

// 修改文档
export function updateDocument(data) {
  return request({
    url: '/system/document',
    method: 'put',
    data: data
  })
}

// 删除文档
export function delDocument(docId) {
  return request({
    url: '/system/document/' + docId,
    method: 'delete'
  })
}
