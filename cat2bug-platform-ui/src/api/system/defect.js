import request from '@/utils/request'

// 查询缺陷列表
export function configDefect() {
  return request({
    url: '/system/defect/config',
    method: 'get'
  })
}


// 查询缺陷列表
export function listDefect(query) {
  return request({
    url: '/system/defect/list',
    method: 'get',
    params: query
  })
}

// 查询缺陷详细
export function getDefect(defectId) {
  return request({
    url: '/system/defect/' + defectId,
    method: 'get'
  })
}

/**
 * 新增缺陷
 * @param {object} data 请求体
 * @param {object} [axiosConfig] 透传给 axios（如 Excel 批量创建：`{ headers: { repeatSubmit: false } }` 关闭 1s 内同 body 拦截，避免多行相同内容被判重复提交）
 */
export function addDefect(data, axiosConfig) {
  data.srcHost = `${window.location.protocol}//${window.location.host}`;
  return request({
    url: '/system/defect',
    method: 'post',
    data: data,
    ...(axiosConfig || {}),
  })
}

export function closeEditWindow(defectId) {
  return request({
    url: '/system/defect/'+defectId+'/close-edit-window',
    method: 'post',
  })
}

// 修改缺陷
export function updateDefect(data, axiosConfig) {
  data.srcHost = `${window.location.protocol}//${window.location.host}`;
  return request({
    url: '/system/defect',
    method: 'put',
    data: data,
    ...(axiosConfig || {}),
  })
}

// 指派
export function assign(defectId, data) {
  data.srcHost = `${window.location.protocol}//${window.location.host}`;
  return request({
    url: '/system/defect/'+defectId+'/assign',
    method: 'post',
    data: data
  })
}

// 拒绝
export function reject(defectId, data) {
  data.srcHost = `${window.location.protocol}//${window.location.host}`;
  return request({
    url: '/system/defect/'+defectId+'/reject',
    method: 'post',
    data: data
  })
}

// 修复
export function repair(defectId, data) {
  data.srcHost = `${window.location.protocol}//${window.location.host}`;
  return request({
    url: '/system/defect/'+defectId+'/repair',
    method: 'post',
    data: data
  })
}

// 修复
export function pass(defectId, data) {
  data.srcHost = `${window.location.protocol}//${window.location.host}`;
  return request({
    url: '/system/defect/'+defectId+'/pass',
    method: 'post',
    data: data
  })
}

// 开启
export function open(defectId, data) {
  data.srcHost = `${window.location.protocol}//${window.location.host}`;
  return request({
    url: '/system/defect/'+defectId+'/open',
    method: 'post',
    data: data
  })
}

// 关闭
export function close(defectId, data) {
  data.srcHost = `${window.location.protocol}//${window.location.host}`;
  return request({
    url: '/system/defect/'+defectId+'/close',
    method: 'post',
    data: data
  })
}

// 删除缺陷
export function delDefect(defectId) {
  return request({
    url: '/system/defect/' + defectId,
    method: 'delete'
  })
}

// 恢复已删除缺陷
export function restoreDefect(defectId) {
  return request({
    url: '/system/defect/' + defectId + '/restore',
    method: 'put'
  })
}

// 修改用户缺陷
export function updateUserDefect(defectId, data) {
  data.srcHost = `${window.location.protocol}//${window.location.host}`;
  return request({
    url: '/system/defect/'+defectId,
    method: 'put',
    data: data
  })
}

/** 甘特二期：依赖边 + 基线（可按缺陷 ID 过滤） */
export function getDefectPlanGanttMeta(projectId, defectIds) {
  const params = { projectId }
  if (Array.isArray(defectIds) && defectIds.length) {
    params.defectIds = defectIds.join(',')
  }
  return request({
    url: '/system/defect/plan-gantt-meta',
    method: 'get',
    params
  })
}

/** 甘特计划节点写（起止/处理人/交付物），权限 system:defect:gantt:plan */
export function updateDefectPlanNode(data) {
  return request({
    url: '/system/defect/plan-node',
    method: 'put',
    data
  })
}

export function createDefectPlanLink(data) {
  return request({
    url: '/system/defect/plan-link',
    method: 'post',
    data
  })
}

export function deleteDefectPlanLink(linkId) {
  return request({
    url: '/system/defect/plan-link/' + linkId,
    method: 'delete'
  })
}

export function freezeDefectPlanBaseline(projectId) {
  return request({
    url: '/system/defect/plan-baseline/freeze',
    method: 'post',
    data: { projectId }
  })
}

export function clearDefectPlanBaseline(projectId) {
  return request({
    url: '/system/defect/plan-baseline',
    method: 'delete',
    params: { projectId }
  })
}
