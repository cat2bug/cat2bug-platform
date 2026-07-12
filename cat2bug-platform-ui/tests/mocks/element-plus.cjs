module.exports = {
  ElNotification: { error: jest.fn() },
  ElMessageBox: { confirm: jest.fn(() => Promise.resolve()) },
  ElMessage: Object.assign(jest.fn(), { error: jest.fn() }),
  ElLoading: { service: jest.fn(() => ({ close: jest.fn() })) }
}
