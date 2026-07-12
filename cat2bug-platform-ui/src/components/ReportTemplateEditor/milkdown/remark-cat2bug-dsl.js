import { visit } from 'unist-util-visit'
import { createTimer } from '@milkdown/ctx'
import { InitReady, remarkPluginsCtx, schemaTimerCtx } from '@milkdown/kit/core'
import {
  TABLE_TOKEN_RE,
  CARD_TOKEN_RE,
  IMAGE_TOKEN_RE
} from '../dsl/grammar'
import { parseTemplate } from '../dsl/parse'
import { normalizeDslSource } from '../dsl/normalize'
import { remarkPreserveRawBlocks } from './remark-preserve-raw-blocks'

export const Cat2bugRawRemarkReady = createTimer('Cat2bugRawRemarkReady')
export const Cat2bugDslRemarkReady = createTimer('Cat2bugDslRemarkReady')

function paragraphText(node) {
  if (!node.children?.length) {
    return ''
  }
  return node.children
    .filter((child) => child.type === 'text')
    .map((child) => child.value)
    .join('')
}

function isBlockToken(text) {
  const trimmed = normalizeDslSource(text || '').trim()
  if (!trimmed) {
    return null
  }
  TABLE_TOKEN_RE.lastIndex = 0
  CARD_TOKEN_RE.lastIndex = 0
  IMAGE_TOKEN_RE.lastIndex = 0
  const table = TABLE_TOKEN_RE.exec(trimmed)
  if (table && table[0] === trimmed) {
    return { kind: 'defectTable', segment: parseTemplate(trimmed)[0] }
  }
  const card = CARD_TOKEN_RE.exec(trimmed)
  if (card && card[0] === trimmed) {
    return { kind: 'caseCard', segment: parseTemplate(trimmed)[0] }
  }
  const image = IMAGE_TOKEN_RE.exec(trimmed)
  if (image && image[0] === trimmed) {
    return { kind: 'dynamicImage', segment: parseTemplate(trimmed)[0] }
  }
  return null
}

function segmentToMdast(segment) {
  switch (segment.type) {
    case 'text':
      return segment.value ? [{ type: 'text', value: segment.value }] : []
    case 'variable':
      return [{ type: 'cat2bugVariable', path: segment.path, raw: segment.raw }]
    case 'planVariable':
      return [{
        type: 'cat2bugPlanVariable',
        path: segment.path,
        planIndex: segment.planIndex,
        raw: segment.raw
      }]
    case 'defectTable':
      return [{
        type: 'cat2bugDefectTable',
        dataSource: segment.dataSource,
        columns: segment.columns,
        raw: segment.raw
      }]
    case 'caseCard':
      return [{
        type: 'cat2bugCaseCard',
        dataSource: segment.dataSource,
        raw: segment.raw
      }]
    case 'dynamicImage':
      return [{
        type: 'cat2bugDynamicImage',
        style: segment.style,
        path: segment.path,
        raw: segment.raw
      }]
    default:
      return segment.raw ? [{ type: 'text', value: segment.raw }] : []
  }
}

function blockMdastFromSegment(segment) {
  const nodes = segmentToMdast(segment)
  const block = nodes[0]
  if (!block || block.type === 'text') {
    return {
      type: 'paragraph',
      children: nodes.length ? nodes : [{ type: 'text', value: segment.raw || '' }]
    }
  }
  return block
}

function mergeSplitBlockDslParagraphs(children) {
  if (!children?.length) {
    return children || []
  }

  const merged = []
  let index = 0

  while (index < children.length) {
    const node = children[index]
    if (node.type !== 'paragraph') {
      merged.push(node)
      index += 1
      continue
    }

    let text = paragraphText(node)
    const normalized = normalizeDslSource(text).trim()
    const startsBlockDsl = /^\$(?:table|card|image|img)/.test(normalized)

    if (startsBlockDsl && !isBlockToken(text)) {
      const parts = [text]
      let nextIndex = index + 1

      while (nextIndex < children.length) {
        const nextNode = children[nextIndex]
        if (nextNode.type !== 'paragraph') {
          break
        }
        parts.push(paragraphText(nextNode))
        const combined = normalizeDslSource(parts.join('\n'))
        if (isBlockToken(combined)) {
          nextIndex += 1
          break
        }
        nextIndex += 1
      }

      const combined = normalizeDslSource(parts.join('\n')).trim()
      if (isBlockToken(combined)) {
        merged.push({
          type: 'paragraph',
          children: [{ type: 'text', value: combined }]
        })
        index = nextIndex
        continue
      }
    }

    merged.push(node)
    index += 1
  }

  return merged
}

function flattenBlockDslParagraphs(tree) {
  if (tree?.children) {
    tree.children = mergeSplitBlockDslParagraphs(tree.children)
  }
}

function promoteBlockSegment(segment) {
  if (
    segment?.type === 'defectTable' ||
    segment?.type === 'caseCard' ||
    segment?.type === 'dynamicImage'
  ) {
    return blockMdastFromSegment(segment)
  }
  return null
}

function remarkCat2bugDsl() {
  return (tree) => {
    flattenBlockDslParagraphs(tree)

    visit(tree, 'code', (node, index, parent) => {
      if (!parent || index == null) {
        return
      }
      const text = normalizeDslSource(node.value || '')
      const blockHit = isBlockToken(text)
      if (blockHit) {
        parent.children[index] = blockMdastFromSegment(blockHit.segment)
        return
      }
      const segments = parseTemplate(text.trim())
      const promoted = segments.length === 1 ? promoteBlockSegment(segments[0]) : null
      if (promoted) {
        parent.children[index] = promoted
      }
    })

    visit(tree, 'paragraph', (node, index, parent) => {
      if (!parent || index == null) {
        return
      }

      const text = normalizeDslSource(paragraphText(node))
      const blockHit = isBlockToken(text)
      if (blockHit) {
        parent.children[index] = blockMdastFromSegment(blockHit.segment)
        return
      }

      const trimmed = text.trim()
      const singleSegments = parseTemplate(trimmed)
      if (singleSegments.length === 1) {
        const promoted = promoteBlockSegment(singleSegments[0])
        if (promoted) {
          parent.children[index] = promoted
          return
        }
      }

      const hasInlineDsl = /\$\{|\$table\{|\$card\{|\$image|\$img/.test(text)
      if (!hasInlineDsl) {
        return
      }

      const segments = parseTemplate(text)
      if (segments.length === 1 && segments[0].type === 'text') {
        return
      }

      const nextChildren = segments.flatMap((segment) => {
        if (segment.type === 'defectTable' || segment.type === 'caseCard' || segment.type === 'dynamicImage') {
          return [{ type: 'text', value: segment.raw }]
        }
        return segmentToMdast(segment)
      })

      node.children = nextChildren.length ? nextChildren : node.children
    })
  }
}

/**
 * Register raw-block preservation before DSL transforms.
 */
export function registerCat2bugRawRemarkPlugin(ctx) {
  ctx.record(Cat2bugRawRemarkReady)
  ctx.update(schemaTimerCtx, (timers) => (
    timers.includes(Cat2bugRawRemarkReady) ? timers : [...timers, Cat2bugRawRemarkReady]
  ))

  return async () => {
    await ctx.wait(InitReady)
    const entry = { plugin: remarkPreserveRawBlocks, options: {} }
    ctx.update(remarkPluginsCtx, (plugins) => [entry, ...plugins])
    ctx.done(Cat2bugRawRemarkReady)
    return () => {
      ctx.update(remarkPluginsCtx, (plugins) => plugins.filter((item) => item !== entry))
      ctx.clearTimer(Cat2bugRawRemarkReady)
    }
  }
}

/**
 * Register remark DSL transform before schema builds the remark processor.
 * $remark() waits for InitReady and can lose the race with internal schema plugin.
 */
export function registerCat2bugDslRemarkPlugin(ctx) {
  ctx.record(Cat2bugDslRemarkReady)
  ctx.update(schemaTimerCtx, (timers) => (
    timers.includes(Cat2bugDslRemarkReady) ? timers : [...timers, Cat2bugDslRemarkReady]
  ))

  return async () => {
    await ctx.wait(InitReady)
    const entry = { plugin: remarkCat2bugDsl, options: {} }
    ctx.update(remarkPluginsCtx, (plugins) => [...plugins, entry])
    ctx.done(Cat2bugDslRemarkReady)
    return () => {
      ctx.update(remarkPluginsCtx, (plugins) => plugins.filter((item) => item !== entry))
      ctx.clearTimer(Cat2bugDslRemarkReady)
    }
  }
}
