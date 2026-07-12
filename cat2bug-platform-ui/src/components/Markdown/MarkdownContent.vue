<template>
  <div class="markdown-it-vue md-body" v-html="html" />
</template>

<script>
import MarkdownIt from 'markdown-it'

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
  computed: {
    html() {
      return getMarkdownIt().render(this.content || '')
    }
  }
}
</script>
