/**
 * 项目/团队设置枢纽页：每张功能卡内可见入口（链接/按钮）分配快捷键徽标。
 */

import { assignRowHintLetters } from '@/utils/defect-row-kbd-hints'
import {
  PROJECT_OPTION_CARD_CATALOG,
  TEAM_OPTION_CARD_CATALOG,
  getOptionCardCatalogDefaultLetter,
  lookupOptionCardCatalogEntry
} from '@/utils/option-card-kbd-catalog'

export {
  PROJECT_OPTION_CARD_CATALOG,
  TEAM_OPTION_CARD_CATALOG,
  PROJECT_OPTION_ACTION_DEFAULTS,
  TEAM_OPTION_ACTION_DEFAULTS
} from '@/utils/option-card-kbd-catalog'

const OPTION_CARD_CATALOG_BY_SCOPE = {
  'project-option': PROJECT_OPTION_CARD_CATALOG,
  'team-option': TEAM_OPTION_CARD_CATALOG
}

function isVisibleEl(el) {
  return !!(el && el.offsetParent !== null)
}

function sanitizeOptionActionKeyPart(value) {
  return String(value || '')
    .trim()
    .replace(/[^\w\u4e00-\u9fff-]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 48)
}

function readExplicitOptionActionKey(el) {
  if (!el || !el.getAttribute) return ''
  const self = el.getAttribute('data-cat2bug-kbd-key')
  if (self) return String(self).trim()
  const host = el.closest('[data-cat2bug-kbd-key]')
  if (host) return String(host.getAttribute('data-cat2bug-kbd-key') || '').trim()
  return ''
}

/** href → 稳定 route key（与卡片 catalog 对齐） */
export function hrefToOptionActionRouteKey(href) {
  if (!href) return null
  let path = String(href).trim()
  if (path.startsWith('#')) path = path.slice(1)
  path = path.split('?')[0]
  path = path.replace(/^\/project\/option\/?/, '')
  path = path.replace(/^\/project\//, '')
  path = path.replace(/^\/team\/option\/?/, '')
  path = path.replace(/^\/team\//, '')
  path = path.replace(/^\//, '')
  const part = sanitizeOptionActionKeyPart(path)
  return part ? `route-${part}` : null
}

/** 为设置项生成稳定 binding key（data 属性 > 路由 > 文案） */
export function resolveOptionActionKey(el, cardIndex, actionIndex) {
  if (!el) return `card${cardIndex}-action${actionIndex}`
  const explicit = readExplicitOptionActionKey(el)
  if (explicit) return explicit
  const anchor = el.closest('a[href]') || el
  const href = anchor.getAttribute && anchor.getAttribute('href')
  const routeKey = hrefToOptionActionRouteKey(href)
  if (routeKey) return routeKey
  const text = sanitizeOptionActionKeyPart(el.textContent)
  if (text) return `text-${text}`
  return `card${cardIndex}-action${actionIndex}`
}

export function lookupOptionCardActionMeta(key, scopeKey) {
  return lookupOptionCardCatalogEntry(key, scopeKey)
}

/** 卡片 body 内每个可见功能入口（按 DOM 顺序，去重 router-link 内外层） */
export function collectActionElementsFromCardBody(body) {
  if (!body) return []
  const items = []
  Array.from(body.children).forEach((child) => {
    if (!isVisibleEl(child)) return
    if (child.matches('a[href]')) {
      items.push(child)
      return
    }
    if (child.matches('.el-link')) {
      items.push(child)
      return
    }
    const innerLink = child.querySelector('a[href]')
    if (innerLink && isVisibleEl(innerLink)) {
      items.push(innerLink)
      return
    }
    const innerElLink = child.querySelector('.el-link:not(a .el-link)')
    if (innerElLink && isVisibleEl(innerElLink) && !innerElLink.closest('a[href]')) {
      items.push(innerElLink)
    }
  })
  return items
}

/** 按 DOM 顺序收集所有可见卡片内的功能入口 */
export function collectVisibleOptionCardActions(container) {
  if (!container || typeof container.querySelectorAll !== 'function') return []
  const items = []
  collectVisibleOptionCards(container).forEach((card, cardIndex) => {
    const body = card.querySelector('.el-card__body')
    collectActionElementsFromCardBody(body).forEach((el, actionIndex) => {
      items.push({
        card,
        cardIndex,
        actionIndex,
        el,
        key: resolveOptionActionKey(el, cardIndex, actionIndex)
      })
    })
  })
  return items
}

/** 卡片内第一个可见链接（跳过 header） */
export function findCardPrimaryLink(card) {
  const body = card && card.querySelector('.el-card__body')
  const first = collectActionElementsFromCardBody(body)[0]
  return first || null
}

export function activateOptionCardAction(el) {
  if (!el) return
  const clickTarget = el.closest('a[href]') || el
  if (clickTarget && typeof clickTarget.click === 'function') {
    clickTarget.click()
  }
}

export function activateOptionCardLink(card) {
  activateOptionCardAction(findCardPrimaryLink(card))
}

/** 按 DOM 顺序收集可见设置卡片 */
export function collectVisibleOptionCards(container) {
  if (!container || typeof container.querySelectorAll !== 'function') return []
  const cards = []
  container.querySelectorAll('.box-card').forEach((card) => {
    if (!isVisibleEl(card)) return
    const col = card.closest('.el-col')
    if (col && !isVisibleEl(col)) return
    cards.push(card)
  })
  return cards
}

const OPTION_CARD_FLOAT = { placement: 'bottom-right-outset', outset: 2, anchorTextBounds: true }

function buildOptionCardEntries({ container, letterForKey, usedLetters = new Set(), scopeKey }) {
  const actions = collectVisibleOptionCardActions(container)
  const letters = assignRowHintLetters(actions.length, usedLetters)
  return actions.map((item, i) => {
    const meta = scopeKey ? lookupOptionCardActionMeta(item.key, scopeKey) : null
    const catalogDefault = scopeKey ? getOptionCardCatalogDefaultLetter(scopeKey, item.key) : ''
    const fallback = catalogDefault || letters[i]
    if (!fallback) return null
    const letter = letterForKey(item.key, fallback)
    if (!letter) return null
    return {
      key: item.key,
      titleKey: meta && meta.titleKey,
      letter,
      defaultLetter: fallback,
      anchor: item.el,
      floatOffset: OPTION_CARD_FLOAT,
      skipViewportCheck: true,
      run: () => activateOptionCardAction(item.el)
    }
  }).filter(Boolean)
}

/**
 * 构建 page-action-hints 动态项（徽标挂在各功能入口上）。
 */
export function buildOptionCardActionHints({ container, letterForKey, usedLetters, scopeKey }) {
  return buildOptionCardEntries({ container, letterForKey, usedLetters, scopeKey })
}

/**
 * 构建 registerPage 动作列表（与动态徽标同一套顺序与键名）。
 */
export function buildOptionCardRegisterActions({ container, letterForKey, usedLetters, scopeKey }) {
  return buildOptionCardEntries({ container, letterForKey, usedLetters, scopeKey }).map((item) => ({
    key: item.key,
    titleKey: item.titleKey,
    defaultLetter: item.defaultLetter,
    run: item.run
  }))
}

export { getOptionCardCatalogDefaultLetter }
