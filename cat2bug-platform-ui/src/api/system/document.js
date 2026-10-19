import request from '@/utils/request'
import { blobValidate } from '@/utils/ruoyi'
import errorCode from '@/utils/errorCode'

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

/** 读取文档关联的 Markdown/文本资源内容 */
export function fetchDocumentMarkdown(fileUrl) {
  return request({
    url: '/common/download/resource',
    method: 'get',
    params: { resource: fileUrl },
    responseType: 'blob',
    silentError: true
  }).then(async (data) => {
    if (!data) {
      return Promise.reject(new Error('empty'))
    }
    if (!blobValidate(data)) {
      const text = await data.text()
      let msg = 'error'
      try {
        const rsp = JSON.parse(text)
        msg = errorCode[rsp.code] || rsp.msg || msg
      } catch (e) {
        msg = text || msg
      }
      return Promise.reject(new Error(msg))
    }
    return data.text()
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
