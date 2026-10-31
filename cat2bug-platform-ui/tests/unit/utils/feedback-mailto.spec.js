import { buildFeedbackMailto, FEEDBACK_MAIL_TO } from '@/utils/feedback-mailto'

describe('feedback-mailto', () => {
  it('builds mailto to service inbox with encoded subject and body', () => {
    const url = buildFeedbackMailto({
      subject: 'Cat2Bug 产品建议',
      bodyPrompt: '请写下建议：',
      locale: 'zh_CN',
      nickName: 'Tester',
      projectId: 12,
      appVersion: '1.2.0',
      userAgent: 'Mozilla/5.0'
    })
    expect(url.startsWith(`mailto:${FEEDBACK_MAIL_TO}?`)).toBe(true)
    expect(url).toContain('subject=' + encodeURIComponent('Cat2Bug 产品建议'))
    const body = decodeURIComponent(url.split('body=')[1])
    expect(body).toContain('请写下建议：')
    expect(body).toContain('Locale: zh_CN')
    expect(body).toContain('User: Tester')
    expect(body).toContain('Project: #12')
    expect(body).toContain('App: 1.2.0')
  })

  it('truncates long userAgent', () => {
    const ua = 'x'.repeat(200)
    const url = buildFeedbackMailto({ subject: 's', userAgent: ua })
    const body = decodeURIComponent(url.split('body=')[1])
    expect(body).toContain('UA: ' + 'x'.repeat(120) + '…')
    expect(body).not.toContain('x'.repeat(121))
  })
})
