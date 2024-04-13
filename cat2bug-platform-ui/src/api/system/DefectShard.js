import request from '@/utils/request'

// 查询分享缺陷关联详细
export function getShardDefect(defectId,defectShardId, data) {
  return request({
    url: '/system/defect/' + defectId + '/shard/'+defectShardId,
    method: 'get',
    params: data
  })
}

// 查询分享缺陷关联详细
export function getShard(defectId) {
  return request({
    url: '/system/defect/' + defectId + '/shard',
    method: 'get'
  })
}

// 分享缺陷关联
export function shardDefect(data) {
  return request({
    url: '/system/defect/'+data.defectId+'/shard',
    method: 'post',
    data: data
  })
}
