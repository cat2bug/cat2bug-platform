<template>
  <div class="markdown-it-vue md-body" v-html="html" @error.capture="handleImageErrorEvent" />
</template>

<script>
import MarkdownIt from 'markdown-it'
import { sanitizeHtml } from '@/utils/sanitize-html'
import { handleImageErrorEvent } from '@/utils/upload-asset'
import { loadMarkdownExtrasCss } from '@/components/Markdown/load-markdown-extras-css'

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

export default {
  name: 'MarkdownContent',
  props: {
    content: {
      type: String,
      default: ''
    }
  },
  created() {
    loadMarkdownExtrasCss()
  },
  computed: {
    html() {
      return sanitizeHtml(getMarkdownIt().render(this.content || ''))
    }
  },
  methods: {
    handleImageErrorEvent
  }
}
</script>
