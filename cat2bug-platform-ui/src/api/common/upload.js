import request from "@/utils/request";
// 上传截图
export function uploadScreenShot( data) {
  return request({
    url: '/common/upload/screen-shot',
    method: 'post',
    data: data
  })
}
// 上传文件
export function upload(data) {
  return request({
    url: '/common/upload',
    method: 'post',
    data: data,
    headers: {'Content-Type': 'multipart/form-data'}
  })
}
// 删除文件
export function deleteFile(data) {
  return request({
    url: '/common/delete',
    method: 'delete',
    data: data,
  })
}

/** 尽力删除：物理文件不存在或已删除时仍视为成功，便于仅更新业务数据 */
export function deleteFileBestEffort(data) {
  return deleteFile(data).catch(() => ({ code: 200 }))
}

