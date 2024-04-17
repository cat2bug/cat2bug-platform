import request from "@/utils/request";
// 上传截图
export function uploadScreenShot( data) {
  return request({
    url: '/common/upload/screen-shot',
    method: 'post',
    data: data
  })
}

export function upload(data) {
  return request({
    url: '/common/upload',
    method: 'post',
    data: data,
    headers: {'Content-Type': 'multipart/form-data'}
  })
}
