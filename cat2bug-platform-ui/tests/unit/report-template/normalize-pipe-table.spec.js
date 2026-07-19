import {
  normalizePipeTableMarkdown,
  splitPipeTableCells,
  formatPipeSeparator,
  cleanPipeTableCell,
  stripMilkdownTableCellBreaks,
  isIrregularPipeTable,
  isPipeSeparatorRow
} from '@/components/ReportTemplateEditor/dsl/normalize-pipe-table'

describe('normalize-pipe-table', () => {
  it('pads columns and inserts a GFM separator after the header', () => {
    const out = normalizePipeTableMarkdown('|a|b|\n|1|\n|x|y|z|')
    const lines = out.split('\n')
    // Header width wins — do not inflate to the longest row
    expect(splitPipeTableCells(lines[0])).toEqual(['a', 'b'])
    expect(lines[1]).toBe(formatPipeSeparator(2))
    expect(splitPipeTableCells(lines[2])).toEqual(['1', ''])
    expect(splitPipeTableCells(lines[3])).toEqual(['x', 'y'])
  })

  it('keeps dash separators from being treated as empty data rows', () => {
    const out = normalizePipeTableMarkdown('|h1|h2|\n|-|-|\n|a|b|')
    expect(out.split('\n')).toEqual(['|h1|h2|', '|-|-|', '|a|b|'])
  })

  it('does not treat all-empty pipe rows as separators', () => {
    expect(isPipeSeparatorRow('|||||')).toBe(false)
    expect(isPipeSeparatorRow('|-|-|')).toBe(true)
  })

  it('strips Milkdown empty-cell <br /> markers but keeps intentional in-cell breaks', () => {
    expect(cleanPipeTableCell('<br />')).toBe('')
    expect(cleanPipeTableCell(' <br/> ')).toBe('')
    expect(cleanPipeTableCell('hello<br />')).toBe('hello<br />')
    const src = '|a|<br />|\n|-|-|\n| <br /> |b|'
    expect(stripMilkdownTableCellBreaks(src)).toBe('|a||\n|-|-|\n||b|')
    expect(stripMilkdownTableCellBreaks('|a<br>b|\n|-|\n|c|')).toBe('|a<br>b|\n|-|\n|c|')
  })

  it('does not inflate column count when a polluted row has many pipes', () => {
    const polluted = [
      '|a|b|',
      '|-|-|',
      '|ok|',
      '|x|y|z|extra|extra|extra|extra|extra|'
    ].join('\n')
    const out = normalizePipeTableMarkdown(polluted)
    const cols = splitPipeTableCells(out.split('\n')[0]).length
    expect(cols).toBe(2)
    expect(out.includes('|||||')).toBe(false)
  })

  it('detects irregular tables vs stable GFM', () => {
    expect(isIrregularPipeTable('|a|b|\n|1|')).toBe(true)
    expect(isIrregularPipeTable('|a|b|\n|-|-|\n|1|2|')).toBe(false)
    expect(isIrregularPipeTable('|a|b|\n|-|-|\n|1|')).toBe(true)
  })
})
