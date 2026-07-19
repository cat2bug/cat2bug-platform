import {
  markdownSemanticallyEqual,
  shouldApplyExternalModelSync
} from '@/components/ReportTemplateEditor/milkdown/model-sync'

describe('model-sync', () => {
  describe('shouldApplyExternalModelSync', () => {
    it('skips external sync while visual editor is focused', () => {
      expect(shouldApplyExternalModelSync({ force: false, editorFocused: true })).toBe(false)
    })

    it('applies forced sync even when focused', () => {
      expect(shouldApplyExternalModelSync({ force: true, editorFocused: true })).toBe(true)
    })

    it('applies sync when editor is not focused', () => {
      expect(shouldApplyExternalModelSync({ force: false, editorFocused: false })).toBe(true)
    })
  })

  describe('markdownSemanticallyEqual', () => {
    it('treats trailing empty paragraph newlines as equivalent', () => {
      expect(
        markdownSemanticallyEqual(
          '描述：${api.project.projectIntroduce}\n',
          '描述：${api.project.projectIntroduce}\n\n'
        )
      ).toBe(true)
    })

    it('detects real content differences', () => {
      expect(markdownSemanticallyEqual('行一\n', '行一\n行二\n')).toBe(false)
    })

    it('treats empty table cells and Milkdown <br /> cells as equivalent', () => {
      expect(
        markdownSemanticallyEqual(
          '|a||\n|-|-|\n||b|',
          '|a|<br />|\n|-|-|\n| <br /> |b|'
        )
      ).toBe(true)
    })
  })
})
