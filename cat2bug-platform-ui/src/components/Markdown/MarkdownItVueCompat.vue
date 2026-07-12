<template>
  <div class="markdown-it-vue md-body" v-html="html" />
</template>

<script>
import MarkdownIt from 'markdown-it'

export default {
  name: 'MarkdownItVueCompat',
  props: {
    content: {
      type: String,
      default: ''
    },
    options: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      md: null,
      html: ''
    }
  },
  created() {
    const markdownItOpts = this.options?.markdownIt || {}
    this.md = new MarkdownIt({
      html: true,
      linkify: true,
      breaks: true,
      ...markdownItOpts
    })
    this.applyLinkAttributes()
    this.render()
  },
  watch: {
    content() {
      this.render()
    },
    options: {
      deep: true,
      handler() {
        this.applyLinkAttributes()
        this.render()
      }
    }
  },
  methods: {
    use(plugin, pluginOptions) {
      if (this.md && plugin) {
        this.md.use(plugin, pluginOptions)
        this.render()
      }
      return this
    },
    applyLinkAttributes() {
      const attrs = this.options?.linkAttributes?.attrs
      if (!attrs || !this.md) return
      const defaultRender = this.md.renderer.rules.link_open
        || ((tokens, idx, opts, env, self) => self.renderToken(tokens, idx, opts))
      this.md.renderer.rules.link_open = (tokens, idx, opts, env, self) => {
        Object.entries(attrs).forEach(([key, value]) => {
          const attrIndex = tokens[idx].attrIndex(key)
          if (attrIndex < 0) {
            tokens[idx].attrPush([key, value])
          } else {
            tokens[idx].attrs[attrIndex][1] = value
          }
        })
        return defaultRender(tokens, idx, opts, env, self)
      }
    },
    render() {
      if (!this.md) return
      try {
        this.html = this.md.render(this.content || '')
      } catch (e) {
        this.html = `<pre>${(this.content || '').replace(/</g, '&lt;')}</pre>`
      }
    }
  }
}
</script>
