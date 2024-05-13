import request from '@/utils/request'

/** 获取markdown用数据 */
export function markdownData(projectId) {
  return request({
    url: '/markdown/'+projectId,
    method: 'get',
  });
}
