/** 与登录页输入框 maxlength 及后端 UserConstants 校验范围对齐 */
export const LOGIN_USERNAME_MIN_LENGTH = 2
export const LOGIN_USERNAME_MAX_LENGTH = 30
export const LOGIN_PASSWORD_MIN_LENGTH = 5
export const LOGIN_PASSWORD_MAX_LENGTH = 30

/**
 * 登录/升级管理员验证共用的用户名、密码表单规则。
 * @param {(key: string) => string} t i18n 翻译函数
 */
export function buildLoginCredentialRules(t) {
  return {
    username: [
      { required: true, trigger: 'blur', message: t('please-enter-your-account') },
      {
        min: LOGIN_USERNAME_MIN_LENGTH,
        max: LOGIN_USERNAME_MAX_LENGTH,
        message: t('account-size-exception'),
        trigger: 'blur'
      }
    ],
    password: [
      { required: true, trigger: 'blur', message: t('please-enter-your-password') },
      {
        min: LOGIN_PASSWORD_MIN_LENGTH,
        max: LOGIN_PASSWORD_MAX_LENGTH,
        message: t('password-size-exception'),
        trigger: 'blur'
      }
    ]
  }
}

/** 升级/安装向导中 prop 名为 adminUsername / adminPassword 时的规则映射 */
export function buildAdminCredentialRules(t) {
  const rules = buildLoginCredentialRules(t)
  return {
    adminUsername: rules.username,
    adminPassword: rules.password
  }
}
