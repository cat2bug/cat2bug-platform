<template>
  <div class="report-milkdown-editor">
    <Milkdown />
  </div>
</template>

<script>
import { defineComponent, watch } from 'vue'
import { Milkdown, useEditor } from '@milkdown/vue'
import { Editor, rootCtx, defaultValueCtx, editorViewCtx } from '@milkdown/core'
import { commonmark } from '@milkdown/preset-commonmark'
import { gfm } from '@milkdown/preset-gfm'
import { listener, listenerCtx } from '@milkdown/plugin-listener'
import 'prosemirror-view/style/prosemirror.css'

export default defineComponent({
  name: 'MilkdownEditorInner',
  components: { Milkdown },
  props: {
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue', 'change'],
  setup(props, { emit, expose }) {
    const { get } = useEditor((root) => {
      return Editor.make()
        .config((ctx) => {
          ctx.set(rootCtx, root)
          ctx.set(defaultValueCtx, props.modelValue || '')
        })
        .config((ctx) => {
          ctx.get(listenerCtx).markdownUpdated((_ctx, markdown) => {
            emit('update:modelValue', markdown)
            emit('change', markdown)
          })
        })
        .use(commonmark)
        .use(gfm)
        .use(listener)
    })

    watch(
      () => props.modelValue,
      (value) => {
        const editor = get()
        if (!editor) return
        editor.action((ctx) => {
          const current = ctx.get(defaultValueCtx)
          if (current !== value) {
            ctx.set(defaultValueCtx, value || '')
          }
        })
      }
    )

    expose({
      focus() {
        get()?.action((ctx) => {
          ctx.get(editorViewCtx)?.focus()
        })
      }
    })

    return {}
  }
})
</script>

<style scoped>
.report-milkdown-editor {
  min-height: 120px;
  padding: 8px 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background: var(--el-bg-color);
}

.report-milkdown-editor :deep(.ProseMirror) {
  min-height: 100px;
  outline: none;
  line-height: 1.6;
}

.report-milkdown-editor :deep(.ProseMirror p) {
  margin: 0.5em 0;
}
</style>
