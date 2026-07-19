import { expandTableCellDslText } from '@/components/ReportTemplateEditor/milkdown/remark-cat2bug-dsl-transform'

describe('expandTableCellDslText', () => {
  it('promotes $image tokens in table cells to cat2bugDynamicImage nodes', () => {
    const raw = '$image["style":"width:100px;height:100px;"]{api.project.projectIcon}'
    const children = expandTableCellDslText(raw)
    expect(children).toEqual([
      {
        type: 'cat2bugDynamicImage',
        style: 'width:100px;height:100px;',
        path: 'api.project.projectIcon',
        raw
      }
    ])
  })

  it('keeps plain text and promotes ${} variables together with images', () => {
    const children = expandTableCellDslText(
      'icon:$image{api.project.projectIcon} name:${api.project.projectName}'
    )
    expect(children).toEqual([
      { type: 'text', value: 'icon:' },
      {
        type: 'cat2bugDynamicImage',
        style: '',
        path: 'api.project.projectIcon',
        raw: '$image{api.project.projectIcon}'
      },
      { type: 'text', value: ' name:' },
      {
        type: 'cat2bugVariable',
        path: 'api.project.projectName',
        raw: '${api.project.projectName}'
      }
    ])
  })

  it('promotes $image after leading spaces (common GFM cell padding)', () => {
    const raw = '$image["style":"width:100px;height:100px;"]{api.project.projectIcon}'
    const children = expandTableCellDslText(`  ${raw}  `)
    expect(children?.some((n) => n.type === 'cat2bugDynamicImage')).toBe(true)
  })
})
