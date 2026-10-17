/**
 * @jest-environment jsdom
 */
import { extractHeadings } from '@/utils/markdown-toc'

describe('markdown-toc extractHeadings', () => {
  it('assigns md-heading ids and data-md-toc-id', () => {
    document.body.innerHTML = `
      <div id="root">
        <h1>Hello World</h1>
        <h2>Section Two</h2>
      </div>
    `
    const root = document.getElementById('root')
    const toc = extractHeadings(root)
    expect(toc).toHaveLength(2)
    expect(toc[0].id).toMatch(/^md-heading-/)
    expect(root.querySelector('h1').getAttribute('data-md-toc-id')).toBe(toc[0].id)
  })
})
