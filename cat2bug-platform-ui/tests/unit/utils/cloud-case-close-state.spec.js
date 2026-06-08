import { serializeCloudCaseDrawerCloseState } from '@/utils/cloud-case-close-state'

describe('cloud-case-close-state', () => {
  it('treats rowCount string and number as equal after normalize', () => {
    const a = serializeCloudCaseDrawerCloseState({ prompt: null, modelId: 'm1', rowCount: '5' }, [])
    const b = serializeCloudCaseDrawerCloseState({ prompt: null, modelId: 'm1', rowCount: 5 }, [])
    expect(a).toBe(b)
  })

  it('detects modelId change as dirty', () => {
    const baseline = serializeCloudCaseDrawerCloseState({ prompt: null, modelId: null, rowCount: 5 }, [])
    const current = serializeCloudCaseDrawerCloseState({ prompt: null, modelId: 'default-model', rowCount: 5 }, [])
    expect(baseline).not.toBe(current)
  })

  it('ignores whitespace-only prompt', () => {
    const a = serializeCloudCaseDrawerCloseState({ prompt: '   ', modelId: null, rowCount: 5 }, [])
    const b = serializeCloudCaseDrawerCloseState({ prompt: null, modelId: null, rowCount: 5 }, [])
    expect(a).toBe(b)
  })
})
