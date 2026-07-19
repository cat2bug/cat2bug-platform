import { visit } from 'unist-util-visit'
import { createTimer } from '@milkdown/ctx'
import { InitReady, remarkPluginsCtx, schemaTimerCtx } from '@milkdown/kit/core'
import { parseTemplate } from '../dsl/parse'
import { normalizeDslSource } from '../dsl/normalize'
import { remarkPreserveRawBlocks } from './remark-preserve-raw-blocks'
import {
  blockMdastFromSegment,
  expandTableCellDslText,
  isBlockToken,
  segmentToMdast
} from './remark-cat2bug-dsl-transform'

export const Cat2bugRawRemarkReady = createTimer('Cat2bugRawRemarkReady')
export const Cat2bugDslRemarkReady = createTimer('Cat2bugDslRemarkReady')

export { expandTableCellDslText, isBlockToken, segmentToMdast, blockMdastFromSegment }

function paragraphText(node) {
  if (!node.children?.length) {
    return ''
  }
  return node.children
    .filter((child) => child.type === 'text')
    .map((child) => child.value)
    .join('')
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

    const text = paragraphText(node)
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
        if (segment.type === 'defectTable' || segment.type === 'caseCard') {
          return [{ type: 'text', value: segment.raw }]
        }
        return segmentToMdast(segment)
      })

      node.children = nextChildren.length ? nextChildren : node.children
    })

    // GFM table cells hold phrasing nodes — promote ${} / $image so Milkdown can edit them in-cell.
    visit(tree, 'tableCell', (node) => {
      const text = (node.children || [])
        .map((child) => (child.type === 'text' ? child.value : ''))
        .join('')
      const nextChildren = expandTableCellDslText(text)
      if (nextChildren) {
        node.children = nextChildren
      }
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
