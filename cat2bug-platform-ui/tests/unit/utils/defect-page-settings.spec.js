import {
  defaultDefectPageSettings,
  enabledDefectViews,
  lifeTimeMetricColor,
  rejectCountMetricColor,
  resolveDefectContentView
} from '@/utils/defect-page-settings'

describe('defect-page-settings', () => {
  it('resolves cached disabled view to table', () => {
    const settings = defaultDefectPageSettings()
    settings.views.DefectKanban = false
    expect(resolveDefectContentView('DefectKanban', settings)).toBe('DefectTable')
  })

  it('enabled views omit disabled', () => {
    const settings = defaultDefectPageSettings()
    settings.views.DefectGantt = false
    expect(enabledDefectViews(settings)).toEqual([
      'DefectTable',
      'DefectExcel',
      'DefectKanban'
    ])
  })

  it('life time color bands', () => {
    const s = defaultDefectPageSettings()
    const day = 24 * 60 * 60 * 1000
    expect(lifeTimeMetricColor(s, 2 * day)).toBe('#67C23A')
    expect(lifeTimeMetricColor(s, 5 * day)).toBe('#E6A23C')
    expect(lifeTimeMetricColor(s, 10 * day)).toBe('#F56C6C')
  })

  it('reject count color bands', () => {
    const s = defaultDefectPageSettings()
    expect(rejectCountMetricColor(s, 1)).toBe('#67C23A')
    expect(rejectCountMetricColor(s, 4)).toBe('#E6A23C')
    expect(rejectCountMetricColor(s, 8)).toBe('#F56C6C')
  })
})
