import request from '@/utils/request'

// 查询AI模型配置列表
export function listAi(query) {
  return request({
    url: '/system/ai/list',
    method: 'get',
    params: query
  })
}

/** 项目内 AI 模型选项：已下载 Ollama + OpenAI 账号（分组下拉用） */
export function projectAiModelOptions(projectId) {
  return request({
    url: '/system/ai/project-model-options',
    method: 'get',
    params: { projectId }
  })
}

// 查询AI默认模型列表
export function defaultListAiModelList() {
  return request({
    url: '/system/ai/model/list/default',
    method: 'get'
  })
}

// 查询AI模型配置详细
export function getAi(aiId) {
  return request({
    url: '/system/ai/' + aiId,
    method: 'get'
  })
}

// 新增AI模型配置
export function downloadModel(data) {
  return request({
    url: '/system/ai/model',
    method: 'post',
    data: data
  })
}

// 删除AI模型配置
export function delModel(data) {
  return request({
    url: '/system/ai/model',
    method: 'delete',
    data: data
  })
}

// 新增AI模型配置
export function addAi(data) {
  return request({
    url: '/system/ai',
    method: 'post',
    data: data
  })
}

// 修改AI模型配置
export function updateProjectModelOption(data) {
  return request({
    url: '/system/ai',
    method: 'put',
    data: data
  })
}

// 删除AI模型配置
export function delAi(aiId) {
  return request({
    url: '/system/ai/' + aiId,
    method: 'delete'
  })
}
