/** 官网客服建议邮箱（顶栏 mailto） */
export const FEEDBACK_MAIL_TO = 'service@cat2bug.com'

const UA_MAX = 120

/**
 * @param {object} [opts]
 * @param {string} [opts.subject]
 * @param {string} [opts.bodyPrompt]
 * @param {string} [opts.locale]
 * @param {string} [opts.nickName]
 * @param {string|number} [opts.projectId]
 * @param {string} [opts.projectName]
 * @param {string} [opts.appVersion]
 * @param {string} [opts.userAgent]
 * @returns {string} mailto URL
 */
export function buildFeedbackMailto(opts = {}) {
  const subject = String(opts.subject || 'Cat2Bug feedback').trim() || 'Cat2Bug feedback'
  const lines = []
  const prompt = String(opts.bodyPrompt || '').trim()
  if (prompt) lines.push(prompt, '')
  lines.push('---')
  if (opts.appVersion) lines.push(`App: ${opts.appVersion}`)
  if (opts.locale) lines.push(`Locale: ${opts.locale}`)
  if (opts.nickName) lines.push(`User: ${opts.nickName}`)
  if (opts.projectName || opts.projectId) {
    const name = opts.projectName ? String(opts.projectName) : ''
    const id = opts.projectId != null && opts.projectId !== '' ? String(opts.projectId) : ''
    lines.push(`Project: ${[name, id && `#${id}`].filter(Boolean).join(' ')}`.trim())
  }
  const ua = String(opts.userAgent || '').trim()
  if (ua) {
    lines.push(`UA: ${ua.length > UA_MAX ? `${ua.slice(0, UA_MAX)}…` : ua}`)
  }
  lines.push('---', '')
  const body = lines.join('\n')
  return `mailto:${FEEDBACK_MAIL_TO}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`
}

/**
 * 打开系统邮件客户端撰写建议信。
 * @param {string} mailtoUrl
 */
export function openFeedbackMailto(mailtoUrl) {
  if (!mailtoUrl || typeof mailtoUrl !== 'string') return
  window.location.href = mailtoUrl
}
