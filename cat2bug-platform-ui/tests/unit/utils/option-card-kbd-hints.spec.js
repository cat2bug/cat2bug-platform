/** @jest-environment jsdom */

import {
  collectVisibleOptionCardActions,
  resolveOptionActionKey
} from '@/utils/option-card-kbd-hints'

describe('option-card-kbd-hints', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
  })

  it('collects each visible action in card body order', () => {
    document.body.innerHTML = `
      <div id="root">
        <div class="el-col">
          <div class="box-card">
            <div class="el-card__header">Header</div>
            <div class="el-card__body">
              <a href="#/project-base-info"><span class="el-link">基本信息</span></a>
              <a href="#/project-api"><span class="el-link">API</span></a>
              <span class="el-link">删除</span>
            </div>
          </div>
        </div>
      </div>
    `
    const root = document.getElementById('root')
    const actions = collectVisibleOptionCardActions(root)
    expect(actions).toHaveLength(3)
    expect(actions[0].key).toBe('route-project-base-info')
    expect(actions[1].key).toBe('route-project-api')
    expect(actions[2].key).toMatch(/^text-/)
  })

  it('resolveOptionActionKey falls back to card index', () => {
    const el = document.createElement('span')
    expect(resolveOptionActionKey(el, 2, 1)).toBe('card2-action1')
  })
})
