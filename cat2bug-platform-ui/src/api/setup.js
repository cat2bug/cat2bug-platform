import request from '@/utils/request'

const setupHeaders = {
  isToken: false,
  repeatSubmit: false
}

/** 查询是否已完成首次安装 */
export function getSetupStatus() {
  return request({
    url: '/setup/status',
    headers: setupHeaders,
    method: 'get',
    silentError: true
  })
}

/** 测试数据库连接 */
export function testDatabase(data) {
  return request({
    url: '/setup/test/database',
    headers: setupHeaders,
    method: 'post',
    data,
    silentError: true
  })
}

/** 测试 Redis 连接 */
export function testRedis(data) {
  return request({
    url: '/setup/test/redis',
    headers: setupHeaders,
    method: 'post',
    data,
    silentError: true
  })
}

/** 测试 Ollama 连接 */
export function testOllama(data) {
  return request({
    url: '/setup/test/ollama',
    headers: setupHeaders,
    method: 'post',
    data,
    silentError: true
  })
}

/** 测试路径可写 */
export function testPath(data) {
  return request({
    url: '/setup/test/path',
    headers: setupHeaders,
    method: 'post',
    data,
    silentError: true
  })
}

/** 提交安装配置 */
export function submitSetup(data) {
  return request({
    url: '/setup/submit',
    headers: setupHeaders,
    method: 'post',
    data,
    timeout: 300000
  })
}
