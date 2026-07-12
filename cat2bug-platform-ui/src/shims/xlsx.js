/**
 * Default export shim for SheetJS xlsx (vue-excel-editor uses `import XLSX from 'xlsx'`).
 * Package ESM build only exposes named exports.
 */
import * as XLSX from '../../node_modules/xlsx/xlsx.mjs'

export default XLSX
