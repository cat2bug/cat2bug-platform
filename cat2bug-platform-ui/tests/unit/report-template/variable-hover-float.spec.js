/** @jest-environment jsdom */

import {
  createVariableHoverFloatController,
  createVariableUnitDomShell,
  VAR_HOVER_CHROME_H,
  VAR_HOVER_PAD_X,
  VAR_HOVER_PAD_Y
} from '@/components/ReportTemplateEditor/milkdown/variable-hover-float'

describe('variable-hover-float', () => {
  it('floats frame/chrome only and leaves body in document flow', () => {
    const { dom, frame, chrome, body } = createVariableUnitDomShell()
    document.body.appendChild(dom)
    body.textContent = 'Cat2Bug'
    body.getBoundingClientRect = () => ({
      top: 100,
      left: 200,
      width: 80,
      height: 16,
      right: 280,
      bottom: 116,
      x: 200,
      y: 100,
      toJSON: () => ({})
    })

    const hover = createVariableHoverFloatController(dom, { frame, chrome, body })
    hover.activate()

    expect(dom.classList.contains('is-hovered')).toBe(true)
    expect(frame.style.top).toBe(`${100 - VAR_HOVER_PAD_Y}px`)
    expect(frame.style.left).toBe(`${200 - VAR_HOVER_PAD_X}px`)
    expect(frame.style.width).toBe(`${80 + VAR_HOVER_PAD_X * 2}px`)
    expect(chrome.style.top).toBe(`${100 - VAR_HOVER_PAD_Y - VAR_HOVER_CHROME_H}px`)
    expect(chrome.style.left).toBe(`${200 - VAR_HOVER_PAD_X}px`)
    // Body must stay in-flow — no fixed lift / spacer
    expect(body.style.position).toBe('')
    expect(dom.querySelector('.cat2bug-dsl-var__spacer')).toBeNull()

    hover.destroy()
    expect(dom.classList.contains('is-hovered')).toBe(false)
    dom.remove()
  })
})
