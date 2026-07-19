import { commandsCtx, editorViewCtx } from '@milkdown/kit/core'
import {
  paragraphSchema,
  clearTextInCurrentBlockCommand,
  setBlockTypeCommand,
  headingSchema,
  blockquoteSchema,
  wrapInBlockTypeCommand,
  hrSchema,
  addBlockTypeCommand,
  bulletListSchema,
  orderedListSchema,
  codeBlockSchema,
  selectTextNearPosCommand
} from '@milkdown/kit/preset/commonmark'
import { createTable } from '@milkdown/kit/preset/gfm'
import { imageBlockSchema } from '@milkdown/kit/component/image-block'
import { requestLinkInsertMarkdown } from './link-insert-bridge'

function runCommands(ctx, ...steps) {
  const commands = ctx.get(commandsCtx)
  for (const step of steps) {
    commands.call(step.key, step.payload)
  }
}

/**
 * Default Crepe-like block menu groups for the report block-add overlay.
 * @returns {{ key: string, label: string, items: { key: string, label: string, onSelect: (ctx: import('@milkdown/ctx').Ctx) => void }[] }[]}
 */
export function buildDefaultBlockMenuGroups() {
  return [
    {
      key: 'text',
      label: '文本',
      items: [
        {
          key: 'text',
          label: '正文',
          onSelect: (ctx) => {
            runCommands(ctx, {
              key: clearTextInCurrentBlockCommand.key
            }, {
              key: setBlockTypeCommand.key,
              payload: { nodeType: paragraphSchema.type(ctx) }
            })
          }
        },
        {
          key: 'h1',
          label: '标题 1',
          onSelect: (ctx) => {
            runCommands(ctx, {
              key: clearTextInCurrentBlockCommand.key
            }, {
              key: setBlockTypeCommand.key,
              payload: { nodeType: headingSchema.type(ctx), attrs: { level: 1 } }
            })
          }
        },
        {
          key: 'h2',
          label: '标题 2',
          onSelect: (ctx) => {
            runCommands(ctx, {
              key: clearTextInCurrentBlockCommand.key
            }, {
              key: setBlockTypeCommand.key,
              payload: { nodeType: headingSchema.type(ctx), attrs: { level: 2 } }
            })
          }
        },
        {
          key: 'h3',
          label: '标题 3',
          onSelect: (ctx) => {
            runCommands(ctx, {
              key: clearTextInCurrentBlockCommand.key
            }, {
              key: setBlockTypeCommand.key,
              payload: { nodeType: headingSchema.type(ctx), attrs: { level: 3 } }
            })
          }
        },
        {
          key: 'quote',
          label: '引用',
          onSelect: (ctx) => {
            runCommands(ctx, {
              key: clearTextInCurrentBlockCommand.key
            }, {
              key: wrapInBlockTypeCommand.key,
              payload: { nodeType: blockquoteSchema.type(ctx) }
            })
          }
        },
        {
          key: 'divider',
          label: '分割线',
          onSelect: (ctx) => {
            runCommands(ctx, {
              key: clearTextInCurrentBlockCommand.key
            }, {
              key: addBlockTypeCommand.key,
              payload: { nodeType: hrSchema.type(ctx) }
            })
          }
        }
      ]
    },
    {
      key: 'list',
      label: '列表',
      items: [
        {
          key: 'bullet-list',
          label: '无序列表',
          onSelect: (ctx) => {
            runCommands(ctx, {
              key: clearTextInCurrentBlockCommand.key
            }, {
              key: wrapInBlockTypeCommand.key,
              payload: { nodeType: bulletListSchema.type(ctx) }
            })
          }
        },
        {
          key: 'ordered-list',
          label: '有序列表',
          onSelect: (ctx) => {
            runCommands(ctx, {
              key: clearTextInCurrentBlockCommand.key
            }, {
              key: wrapInBlockTypeCommand.key,
              payload: { nodeType: orderedListSchema.type(ctx) }
            })
          }
        }
      ]
    },
    {
      key: 'advanced',
      label: '高级',
      // Order matches Crepe official slash menu: Image → Code → Table
      items: [
        {
          key: 'image',
          label: '图片',
          onSelect: (ctx) => {
            const imageBlock = imageBlockSchema.type(ctx)
            if (!imageBlock) {
              return
            }
            runCommands(ctx, {
              key: clearTextInCurrentBlockCommand.key
            }, {
              key: addBlockTypeCommand.key,
              payload: { nodeType: imageBlock }
            })
          }
        },
        {
          key: 'code',
          label: '代码块',
          onSelect: (ctx) => {
            runCommands(ctx, {
              key: clearTextInCurrentBlockCommand.key
            }, {
              key: setBlockTypeCommand.key,
              payload: { nodeType: codeBlockSchema.type(ctx) }
            })
          }
        },
        {
          key: 'table',
          label: '表格',
          onSelect: (ctx) => {
            const commands = ctx.get(commandsCtx)
            const view = ctx.get(editorViewCtx)
            commands.call(clearTextInCurrentBlockCommand.key)
            const { from } = view.state.selection
            commands.call(addBlockTypeCommand.key, {
              nodeType: createTable(ctx, 3, 3)
            })
            commands.call(selectTextNearPosCommand.key, { pos: from })
          }
        }
      ]
    }
  ]
}

/**
 * Insert Crepe image-block at the current selection (toolbar「图片」).
 * @param {import('@milkdown/ctx').Ctx} ctx
 */
export function insertImageBlockAtSelection(ctx) {
  const imageBlock = imageBlockSchema.type(ctx)
  if (!imageBlock) {
    return
  }
  runCommands(ctx, {
    key: clearTextInCurrentBlockCommand.key
  }, {
    key: addBlockTypeCommand.key,
    payload: { nodeType: imageBlock }
  })
}

function contentNeedsCaretPreserve(content) {
  return /\$\{|\$image|\$img|\$table|\$card/.test(String(content || ''))
}

/**
 * @param {{ key: string, label: string, items: { key: string, label: string, content: string }[] }[]} businessGroups
 * @param {(ctx: import('@milkdown/ctx').Ctx, content: string) => void} onInsert
 */
export function businessGroupsToOverlayGroups(businessGroups, onInsert) {
  return (businessGroups || []).map((group) => ({
    key: group.key,
    label: group.label,
    items: (group.items || []).map((item) => ({
      key: item.key,
      label: item.label,
      // Keep pre-+ caret; do not jump to the ephemeral empty paragraph first.
      skipSelectionRestore: true,
      onSelect: (ctx) => onInsert(ctx, item.content)
    }))
  }))
}

/**
 * Convert toolbar-aligned add-menu groups into overlay menu groups.
 * @param {{ key: string, label: string, items: { key: string, label: string, content?: string, insertMode?: string }[] }[]} groups
 * @param {{ onInsertContent: (ctx: import('@milkdown/ctx').Ctx, content: string) => void, onInsertImageBlock?: (ctx: import('@milkdown/ctx').Ctx) => void }} handlers
 */
export function toolbarAddMenuToOverlayGroups(groups, handlers) {
  const onInsertContent = handlers?.onInsertContent
  const onInsertImageBlock = handlers?.onInsertImageBlock || insertImageBlockAtSelection
  return (groups || []).map((group) => ({
    key: group.key,
    label: group.label,
    items: (group.items || []).map((item) => {
      if (item.insertMode === 'image-block') {
        return {
          key: item.key,
          label: item.label,
          onSelect: (ctx) => onInsertImageBlock(ctx)
        }
      }
      if (item.insertMode === 'link') {
        return {
          key: item.key,
          label: item.label,
          skipSelectionRestore: true,
          onSelect: (ctx) => {
            requestLinkInsertMarkdown().then((markdown) => {
              if (markdown) {
                onInsertContent?.(ctx, markdown)
              }
            })
          }
        }
      }
      return {
        key: item.key,
        label: item.label,
        skipSelectionRestore: contentNeedsCaretPreserve(item.content),
        onSelect: (ctx) => onInsertContent?.(ctx, item.content)
      }
    })
  }))
}
