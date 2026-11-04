/**
 * 用例评审导出：基于落库 reportMarkdown 快照，不重渲模板。
 * PDF 首版用浏览器打印（window.print），避免引入 html2canvas+jspdf 体积与排版风险。
 */
import { saveAs } from 'file-saver'
import MarkdownIt from 'markdown-it'
import { sanitizeHtml } from '@/utils/sanitize-html'
import htmlDocx from 'html-docx-js'

let mdInstance

function getMarkdownIt() {
  if (!mdInstance) {
    mdInstance = new MarkdownIt({
      html: true,
      linkify: true,
      breaks: true
    })
  }
  return mdInstance
}

function markdownToHtml(markdown) {
  return sanitizeHtml(getMarkdownIt().render(String(markdown || '')))
}

function exportBaseName(review) {
  const id = review && review.reviewId != null ? review.reviewId : 'review'
  return `case-review-${id}`
}

/**
 * @param {{ status?: string, reportMarkdown?: string }|null|undefined} review
 */
export function canExportCaseReview(review) {
  if (!review || review.status !== 'completed') return false
  return String(review.reportMarkdown || '').trim().length > 0
}

export function exportCaseReviewMarkdown(review) {
  if (!canExportCaseReview(review)) return
  const blob = new Blob([review.reportMarkdown], { type: 'text/markdown;charset=utf-8' })
  saveAs(blob, `${exportBaseName(review)}.md`)
}

export async function exportCaseReviewDoc(review) {
  if (!canExportCaseReview(review)) return
  const bodyHtml = markdownToHtml(review.reportMarkdown)
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8"></head><body>${bodyHtml}</body></html>`
  const blob = await htmlDocx.asBlob(html)
  saveAs(blob, `${exportBaseName(review)}.docx`)
}

export function exportCaseReviewPdf(review) {
  if (!canExportCaseReview(review)) return
  const bodyHtml = markdownToHtml(review.reportMarkdown)
  const win = window.open('', '_blank', 'noopener,noreferrer')
  if (!win) return
  win.document.open()
  win.document.write(`<!DOCTYPE html><html><head><meta charset="utf-8"><title>${exportBaseName(review)}</title>
<style>
body { font-family: system-ui, sans-serif; padding: 24px; color: #24292e; line-height: 1.6; }
img { max-width: 100%; }
table { border-collapse: collapse; }
th, td { border: 1px solid #ddd; padding: 6px 10px; }
</style></head><body class="markdown-body">${bodyHtml}</body></html>`)
  win.document.close()
  win.focus()
  const print = () => {
    try {
      win.print()
    } catch (_) {
      /* 用户可手动打印 */
    }
  }
  if (win.document.readyState === 'complete') {
    setTimeout(print, 300)
  } else {
    win.onload = () => setTimeout(print, 300)
  }
}
