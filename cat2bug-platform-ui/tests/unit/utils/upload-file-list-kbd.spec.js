import {
  UPLOAD_KBD_LIST,
  UPLOAD_KBD_SHELL,
  createUploadListKbdState,
  handleUploadListKeydown
} from '@/utils/upload-file-list-kbd'

describe('upload-file-list-kbd', () => {
  it('creates default shell zone state', () => {
    expect(createUploadListKbdState()).toEqual({
      zone: UPLOAD_KBD_SHELL,
      index: 0
    })
  })

  it('moves from shell to first file on ArrowDown', () => {
    const state = createUploadListKbdState()
    const e = { key: 'ArrowDown', preventDefault: jest.fn(), isComposing: false }
    const result = handleUploadListKeydown(e, state, { fileCount: 2 })
    expect(e.preventDefault).toHaveBeenCalled()
    expect(result.handled).toBe(true)
    expect(result.state).toEqual({ zone: UPLOAD_KBD_LIST, index: 0 })
  })

  it('returns to shell from first file on ArrowUp', () => {
    const state = { zone: UPLOAD_KBD_LIST, index: 0 }
    const e = { key: 'ArrowUp', preventDefault: jest.fn(), isComposing: false }
    const result = handleUploadListKeydown(e, state, { fileCount: 2 })
    expect(result.state).toEqual({ zone: UPLOAD_KBD_SHELL, index: 0 })
  })

  it('calls onDeleteFile in list zone', () => {
    const onDeleteFile = jest.fn()
    const state = { zone: UPLOAD_KBD_LIST, index: 1 }
    const e = { key: 'Delete', preventDefault: jest.fn(), isComposing: false }
    handleUploadListKeydown(e, state, { fileCount: 3, onDeleteFile })
    expect(onDeleteFile).toHaveBeenCalledWith(1)
  })
})
