/**
 * Phase 1 build shim — html-docx-js uses legacy `with` statements that Rollup cannot parse.
 * Word export is restored in Phase 3c (frontend-lib-migration).
 */
export default {
  asBlob(html, options) {
    void options
    const body = typeof html === 'string' ? html : String(html ?? '')
    return new Blob([body], {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    })
  }
}
