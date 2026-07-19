import { rewriteReportSnapshotImageSrcs } from '@/components/ReportTemplateEditor/milkdown/rewrite-report-snapshot-images'

jest.mock('@/utils/project-default-icons', () => ({
  parseProjectIconIndex: (path) => {
    const m = String(path || '').match(/project_icon(\d+)/i)
    return m ? Number(m[1]) : null
  },
  getDefaultProjectIconUrl: (index) => `/vite-asset/project_icon${index}.svg`
}))

jest.mock('@/utils/upload-asset', () => ({
  resolveProjectIconUrl: (path) => {
    const m = String(path || '').match(/project_icon(\d+)/i)
    return m ? `/vite-asset/project_icon${m[1]}.svg` : path
  },
  resolveUploadUrl: (path) => {
    const base = '/dev-api'
    const trimmed = String(path || '')
    if (trimmed.startsWith('/static/img/')) {
      return base + trimmed
    }
    return trimmed
  }
}))

describe('rewriteReportSnapshotImageSrcs', () => {
  it('rewrites preset project icon img src to vite asset url', () => {
    const input =
      '<img src="/static/img/project_icon1.204904ae.svg" alt="projectIcon" style="width:100px;height:100px;" />'
    const out = rewriteReportSnapshotImageSrcs(input)
    expect(out).toContain('src="/vite-asset/project_icon1.svg"')
    expect(out).toContain('style="width:100px;height:100px;"')
  })
})
