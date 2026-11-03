import {
  DEFAULT_MAX_TOTAL_BYTES,
  buildPushJobBody,
  formatByteSize,
  isUnfinishedJob,
  normalizeEstimate,
  normalizeJob,
  removeLargestOptional,
  selectableCheckedDocIds,
  selectedTotal
} from '@/views/system/project/push/push-estimate'

const estimatePayload = {
  code: 200,
  data: {
    maxTotalBytes: 1000,
    required: [
      { key: 'defects', bytes: 100 },
      { key: 'case', estimatedBytes: 80 },
      { key: 'plans', size: 20 },
      { key: 'deliverable', bytes: 50 }
    ],
    git: [
      { gitSlug: 'app', repoName: 'App', bytes: 600 },
      { slug: 'web', name: 'Web', bytes: 200 }
    ],
    documents: [
      {
        id: 1,
        name: 'specs',
        folder: true,
        children: [
          { docId: 2, docName: 'a.pdf', type: 'file', bytes: 300, localMissing: false },
          { id: 3, name: 'gone.docx', type: 'file', bytes: 900, readable: false }
        ]
      }
    ]
  }
}

describe('push-estimate', () => {
  it('normalizes required modules, git and missing local files', () => {
    const estimate = normalizeEstimate(estimatePayload)
    expect(estimate.maxTotalBytes).toBe(1000)
    expect(estimate.required.map((item) => item.bytes)).toEqual([100, 80, 20, 50])
    expect(estimate.repositories.map((repo) => repo.slug)).toEqual(['app', 'web'])
    const folder = estimate.documents[0]
    expect(folder.folder).toBe(true)
    expect(folder.disabled).toBe(false)
    expect(folder.bytes).toBe(300)
    expect(folder.children[1].disabled).toBe(true)
    expect(folder.children[1].localMissing).toBe(true)
  })

  it('disables a folder when every file is missing locally', () => {
    const estimate = normalizeEstimate({
      documents: [{
        id: 'dir',
        name: 'empty',
        children: [{ id: 'x', name: 'x', localMissing: true, bytes: 10 }]
      }]
    })
    expect(estimate.documents[0].disabled).toBe(true)
    expect(estimate.maxTotalBytes).toBe(DEFAULT_MAX_TOTAL_BYTES)
  })

  it('sums only selected optional items and ignores missing files', () => {
    const estimate = normalizeEstimate(estimatePayload)
    const total = selectedTotal({
      ...estimate,
      checkedGitSlugs: ['web'],
      checkedDocIds: ['2', '3']
    })
    expect(total).toBe(100 + 80 + 20 + 50 + 200 + 300)
    expect(selectableCheckedDocIds(estimate.documents, ['2', '3', '1'])).toEqual(['2'])
  })

  it('removes the single largest optional item and prefers git on a tie', () => {
    const estimate = normalizeEstimate(estimatePayload)
    const once = removeLargestOptional({
      ...estimate,
      checkedGitSlugs: ['app', 'web'],
      checkedDocIds: ['2']
    })
    expect(once.removed).toEqual({ kind: 'git', id: 'app', bytes: 600 })
    expect(once.checkedGitSlugs).toEqual(['web'])
    expect(once.checkedDocIds).toEqual(['2'])

    const tied = removeLargestOptional({
      repositories: [{ slug: 'a', name: 'a', bytes: 10 }],
      documents: [{ id: 'd', name: 'd', folder: false, bytes: 10, localMissing: false, disabled: false, children: [] }],
      checkedGitSlugs: ['a'],
      checkedDocIds: ['d']
    })
    expect(tied.removed.kind).toBe('git')
  })

  it('builds the job body and reads job progress', () => {
    expect(buildPushJobBody({
      pullKey: ' k ',
      gitSlugs: ['app'],
      documentIds: ['12', 'note']
    })).toEqual({
      pullKey: 'k',
      gitSlugs: ['app'],
      documentIds: [12, 'note']
    })
    const job = normalizeJob({
      code: 200,
      data: {
        jobId: 'j1',
        state: 'FAILED',
        parts: [{ partId: 'core', type: 'core', state: 'OK', bytes: 10 }, { partId: 'git-app', type: 'git', state: 'FAILED', error: 'timeout' }],
        selection: { gitSlugs: ['app'], documentIds: [2] }
      }
    })
    expect(job.parts[1].error).toBe('timeout')
    expect(job.selection.documentIds).toEqual(['2'])
    expect(isUnfinishedJob(job)).toBe(true)
    expect(isUnfinishedJob({ jobId: 'j2', state: 'SUCCESS' })).toBe(false)
    expect(normalizeJob({ code: 200, data: null })).toBe(null)
  })

  it('formats unknown and byte sizes', () => {
    expect(formatByteSize(null)).toBe('—')
    expect(formatByteSize(0)).toBe('0 B')
    expect(formatByteSize(1536)).toBe('1.5 KB')
    expect(formatByteSize(524288000)).toBe('500 MB')
  })
})
