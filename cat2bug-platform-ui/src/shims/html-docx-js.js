/**
 * Word export entry — loads real html-docx-js UMD at runtime
 * (bundlers cannot parse its legacy `with` templates).
 */
import { htmlDocxAsBlob } from '@/utils/html-docx-runtime'

export default {
  /**
   * @param {string} html
   * @param {object} [options]
   * @returns {Promise<Blob>}
   */
  asBlob(html, options) {
    return htmlDocxAsBlob(html, options)
  }
}
