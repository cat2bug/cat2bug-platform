import { serializeDefectFormCloseState } from '@/utils/defect-form-close-state'
import {
  applyMergeFieldToForm,
  createEmptyMergeForm,
  isMergeFormEffectivelyEmpty,
  shouldConfirmMergeMasterOverwrite
} from '@/utils/defect-merge-form'

describe('shouldConfirmMergeMasterOverwrite', () => {
  const emptyBaseline = serializeDefectFormCloseState(createEmptyMergeForm(), [])

  it('does not confirm on empty left before first master', () => {
    const form = createEmptyMergeForm()
    expect(
      shouldConfirmMergeMasterOverwrite(null, form, [], emptyBaseline, serializeDefectFormCloseState)
    ).toBe(false)
  })

  it('does not confirm when only custom-field defaults differ from baseline', () => {
    const form = createEmptyMergeForm()
    form.projectId = 58
    form.customFields = {
      bool_a: false,
      text_b: '',
      list_c: [],
      num_d: null
    }
    const staleBaseline = serializeDefectFormCloseState(createEmptyMergeForm(), [])
    expect(
      shouldConfirmMergeMasterOverwrite(
        null,
        form,
        [],
        staleBaseline,
        serializeDefectFormCloseState
      )
    ).toBe(false)
  })

  it('confirms when master already selected', () => {
    const form = createEmptyMergeForm()
    expect(
      shouldConfirmMergeMasterOverwrite(12, form, [], emptyBaseline, serializeDefectFormCloseState)
    ).toBe(true)
  })

  it('confirms when user edited left before choosing master', () => {
    const form = { ...createEmptyMergeForm(), defectName: 'edited' }
    expect(
      shouldConfirmMergeMasterOverwrite(null, form, [], emptyBaseline, serializeDefectFormCloseState)
    ).toBe(true)
  })
})

describe('isMergeFormEffectivelyEmpty', () => {
  it('treats default level and empty custom defaults as empty', () => {
    const form = createEmptyMergeForm()
    form.projectId = 1
    form.customFields = { flag: false, name: '' }
    expect(isMergeFormEffectivelyEmpty(form, [])).toBe(true)
  })

  it('detects filled name', () => {
    const form = { ...createEmptyMergeForm(), defectName: 'x' }
    expect(isMergeFormEffectivelyEmpty(form, [])).toBe(false)
  })
})

describe('applyMergeFieldToForm', () => {
  it('replaces a single field without merging lists', () => {
    const form = createEmptyMergeForm()
    form.annexUrls = ['a.pdf']
    const planTimeRange = []
    const source = {
      annexUrls: ['b.pdf'],
      defectName: 'from source'
    }
    applyMergeFieldToForm(form, planTimeRange, 'annexUrls', source)
    expect(form.annexUrls).toEqual(['b.pdf'])
    expect(form.defectName).toBeNull()
  })
})
