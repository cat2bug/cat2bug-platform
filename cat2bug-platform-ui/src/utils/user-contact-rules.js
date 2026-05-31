import { validEmail } from '@/utils/validate'

const CN_MOBILE_PATTERN = /^1[3-9]\d{9}$/

/**
 * Element UI：手机号选填，有值时校验长度与格式。
 */
export function optionalPhoneRule(getMessage) {
  return {
    validator: (rule, value, callback) => {
      const trimmed = (value || '').trim()
      if (!trimmed) {
        callback()
        return
      }
      if (trimmed.length < 11 || trimmed.length > 16) {
        callback(new Error(getMessage('phone-size-exception') || getMessage('member.phone-number-format-exception')))
        return
      }
      if (!CN_MOBILE_PATTERN.test(trimmed)) {
        callback(new Error(getMessage('member.phone-number-format-exception')))
        return
      }
      callback()
    },
    trigger: 'blur'
  }
}

/**
 * Element UI：邮箱选填，有值时校验格式。
 */
export function optionalEmailRule(getMessage) {
  return {
    validator: (rule, value, callback) => {
      const trimmed = (value || '').trim()
      if (!trimmed) {
        callback()
        return
      }
      if (!validEmail(trimmed)) {
        callback(new Error(getMessage('member.email-format-exception')))
        return
      }
      callback()
    },
    trigger: ['blur', 'change']
  }
}

/** 提交前将空字符串规范为 null，便于后端写入 NULL */
export function normalizeContactFields(target, fields = ['phoneNumber', 'email']) {
  if (!target) {
    return target
  }
  fields.forEach(field => {
    if (Object.prototype.hasOwnProperty.call(target, field)) {
      const trimmed = (target[field] || '').trim()
      target[field] = trimmed || null
    }
  })
  return target
}

/** 展示用：空值显示「未设置」 */
export function formatContactDisplay(value, notSetLabel) {
  const trimmed = (value || '').trim()
  return trimmed || notSetLabel
}
