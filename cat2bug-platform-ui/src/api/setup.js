import request from '@/utils/request'

/**
 * @typedef {'h2'|'mysql'} SetupDatabaseType
 * @typedef {'new'|'existing'} SetupDatabaseMode
 */

/**
 * POST /setup/test/database
 * @typedef {Object} SetupDatabaseTestRequest
 * @property {SetupDatabaseType} databaseType
 * @property {string} [database] - H2/MySQL 库名（默认 cat2bug；H2 对应 ./data/{name}.mv.db）
 * @property {string} [host] - MySQL 主机
 * @property {number} [port] - MySQL 端口
 * @property {string} [username] - MySQL 用户名
 * @property {string} [password] - MySQL 密码
 */

/**
 * @typedef {Object} SetupDatabaseTestResponse
 * @property {boolean} success
 * @property {string} [message]
 * @property {SetupDatabaseMode} [databaseMode] - 库/文件是否已存在
 */

/**
 * POST /setup/submit
 * @typedef {Object} SetupSubmitRequest
 * @property {SetupDatabaseType} databaseType
 * @property {SetupDatabaseMode} databaseMode - 须与最近一次成功测试连接结果一致
 * @property {string} [h2Database] - H2 库名（databaseType=h2）
 * @property {string} [mysqlHost]
 * @property {number} [mysqlPort]
 * @property {string} [mysqlDatabase]
 * @property {string} [mysqlUsername]
 * @property {string} [mysqlPassword]
 * @property {string} cacheType
 * @property {string} profile
 * @property {string} [temp]
 * @property {string} logPath
 * @property {boolean} [aiEnabled]
 * @property {string} [aiHost]
 * @property {string} adminUsername
 * @property {string} adminPassword
 * @property {boolean} [registerUser]
 * @property {boolean} [captchaEnabled]
 */

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
