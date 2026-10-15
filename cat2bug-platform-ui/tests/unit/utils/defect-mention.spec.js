import {
  DEFECT_MENTION_HASH_PREFIX,
  linkDefectMentionsInMarkdown,
  parseDefectMentionHref,
  splitDefectMentions
} from '@/utils/defect-mention'

describe('defect-mention', () => {
  it('links #num outside fences', () => {
    const out = linkDefectMentionsInMarkdown('修复缺陷 #61 与 #7')
    expect(out).toContain(`[#61](#${DEFECT_MENTION_HASH_PREFIX}61)`)
    expect(out).toContain(`[#7](#${DEFECT_MENTION_HASH_PREFIX}7)`)
  })

  it('does not link inside fenced code', () => {
    const out = linkDefectMentionsInMarkdown('见代码：\n```\nconst x = "#61"\n```\n外面 #62')
    expect(out).toContain('const x = "#61"')
    expect(out).not.toContain(`[#61](#${DEFECT_MENTION_HASH_PREFIX}61)`)
    expect(out).toContain(`[#62](#${DEFECT_MENTION_HASH_PREFIX}62)`)
  })

  it('splits title into mention segments', () => {
    const segs = splitDefectMentions('fix: 交付物路径（#61）')
    expect(segs.some((s) => s.type === 'mention' && s.num === 61)).toBe(true)
    expect(segs.filter((s) => s.type === 'text').length).toBeGreaterThan(0)
  })

  it('parses mention href', () => {
    expect(parseDefectMentionHref(`#${DEFECT_MENTION_HASH_PREFIX}61`)).toBe(61)
    expect(parseDefectMentionHref('https://example.com')).toBeNull()
  })
})
