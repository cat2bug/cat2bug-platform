import {
  buildProjectOptionSubPaginationShortcuts,
  clickProjectOptionSubPagination
} from '@/utils/project-option-sub-list-kbd'

describe('project-option-sub-list-kbd', () => {
  it('builds U/P pagination shortcuts when selector is set', () => {
    const vm = {
      total: 20,
      getProjectOptionSubPaginationSelector: () => '.demo-pagination',
      getProjectOptionSubKbdScope: () => 'project-option',
      shortcutChangePage: jest.fn()
    }
    const built = buildProjectOptionSubPaginationShortcuts(vm)
    expect(built.register).toHaveLength(2)
    expect(built.register[0].defaultLetter).toBe('U')
    expect(built.register[1].defaultLetter).toBe('P')
    expect(built.hints[0].badgeSelector).toBe('.demo-pagination .btn-prev')
  })

  it('clicks pagination button when enabled', () => {
    const btn = { classList: { contains: () => false }, click: jest.fn() }
    const vm = {
      getPageActionHintContainer: () => ({
        querySelector: (sel) => (sel.includes('btn-next') ? btn : null)
      }),
      getProjectOptionSubPaginationSelector: () => '.demo-pagination'
    }
    clickProjectOptionSubPagination(vm, 1)
    expect(btn.click).toHaveBeenCalled()
  })
})
