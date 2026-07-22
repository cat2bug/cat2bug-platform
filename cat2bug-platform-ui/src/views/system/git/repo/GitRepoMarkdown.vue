<template>
  <div
    class="git-repo-markdown"
    @click="onClick"
  >
    <markdown-renderer
      :key="renderKey"
      :content="rewrittenContent"
    />
  </div>
</template>

<script>
import { MarkdownRenderer } from '@/components/Markdown'
import { fetchGitRepoRawBlob } from '@/api/system/gitRepo'
import {
  parseGitMarkdownNavHref,
  parseGitMarkdownRawPathFromImg,
  repoFileDir,
  rewriteGitMarkdownSource
} from '@/utils/git-markdown-assets'

export default {
  name: 'GitRepoMarkdown',
  components: { MarkdownRenderer },
  props: {
    content: {
      type: String,
      default: ''
    },
    /** Absolute path of the markdown file within the repository */
    filePath: {
      type: String,
      default: ''
    },
    repositoryId: {
      type: [Number, String],
      required: true
    },
    gitRef: {
      type: String,
      default: ''
    }
  },
  emits: ['navigate'],
  data() {
    return {
      objectUrls: []
    }
  },
  computed: {
    baseDir() {
      return repoFileDir(this.filePath)
    },
    rewrittenContent() {
      return rewriteGitMarkdownSource(this.content || '', {
        baseDir: this.baseDir
      })
    },
    renderKey() {
      return `${this.filePath}:${this.gitRef}:${(this.rewrittenContent || '').length}`
    }
  },
  watch: {
    rewrittenContent() {
      this.$nextTick(() => this.hydrateImages())
    },
    gitRef() {
      this.$nextTick(() => this.hydrateImages())
    },
    repositoryId() {
      this.$nextTick(() => this.hydrateImages())
    }
  },
  mounted() {
    this.$nextTick(() => this.hydrateImages())
  },
  beforeUnmount() {
    this.revokeObjectUrls()
  },
  beforeDestroy() {
    this.revokeObjectUrls()
  },
  methods: {
    revokeObjectUrls() {
      ;(this.objectUrls || []).forEach((url) => {
        try {
          URL.revokeObjectURL(url)
        } catch (e) { /* ignore */ }
      })
      this.objectUrls = []
    },
    async hydrateImages() {
      const root = this.$el
      if (!root || !this.repositoryId) return
      const imgs = root.querySelectorAll('img[data-git-raw], img[src^="git-raw://"]')
      if (!imgs.length) return

      this.revokeObjectUrls()
      const nextUrls = []
      await Promise.all(Array.from(imgs).map(async (img) => {
        const path = parseGitMarkdownRawPathFromImg(img)
        if (!path) return
        const blob = await fetchGitRepoRawBlob(this.repositoryId, this.gitRef, path)
        if (!blob) return
        const url = URL.createObjectURL(blob)
        nextUrls.push(url)
        img.src = url
        img.removeAttribute('data-git-raw')
      }))
      this.objectUrls = nextUrls
    },
    onClick(e) {
      const a = e.target && e.target.closest ? e.target.closest('a[href]') : null
      if (!a || !this.$el.contains(a)) return
      const path = parseGitMarkdownNavHref(a.getAttribute('href'))
      if (!path) return
      e.preventDefault()
      e.stopPropagation()
      this.$emit('navigate', path)
    }
  }
}
</script>
