import {
  isNonRepoMarkdownHref,
  resolveRepoRelativePath,
  repoFileDir,
  rewriteGitMarkdownSource,
  gitMarkdownNavHref,
  gitMarkdownRawHref,
  gitMarkdownRawImgHtml,
  GIT_RAW_PLACEHOLDER_SRC,
  parseGitMarkdownNavHref,
  parseGitMarkdownRawHref,
  parseGitMarkdownRawPathFromImg
} from '@/utils/git-markdown-assets'

describe('git-markdown-assets', () => {
  describe('resolveRepoRelativePath', () => {
    it('resolves relative paths from base dir', () => {
      expect(resolveRepoRelativePath('', 'docs/a.md')).toBe('docs/a.md')
      expect(resolveRepoRelativePath('docs', 'img/logo.png')).toBe('docs/img/logo.png')
      expect(resolveRepoRelativePath('docs/guide', '../img/a.png')).toBe('docs/img/a.png')
      expect(resolveRepoRelativePath('', './readme-assets/x.png')).toBe('readme-assets/x.png')
    })

    it('treats leading slash as repo root', () => {
      expect(resolveRepoRelativePath('docs', '/images/a.png')).toBe('images/a.png')
    })

    it('rejects escape above repo root', () => {
      expect(resolveRepoRelativePath('', '../secret')).toBeNull()
      expect(resolveRepoRelativePath('docs', '../../x')).toBeNull()
    })

    it('ignores external and special hrefs', () => {
      expect(resolveRepoRelativePath('', 'https://example.com/a.png')).toBeNull()
      expect(resolveRepoRelativePath('', 'mailto:a@b.c')).toBeNull()
      expect(resolveRepoRelativePath('', '#section')).toBeNull()
      expect(isNonRepoMarkdownHref('#section')).toBe(true)
    })

    it('strips query and hash from path part', () => {
      expect(resolveRepoRelativePath('', 'docs/a.md?x=1#y')).toBe('docs/a.md')
    })
  })

  describe('repoFileDir', () => {
    it('returns parent directory', () => {
      expect(repoFileDir('README.md')).toBe('')
      expect(repoFileDir('docs/guide.md')).toBe('docs')
    })
  })

  describe('rewriteGitMarkdownSource', () => {
    it('rewrites relative images to placeholder + data-git-raw (not git-raw:// src)', () => {
      const md = 'See ![logo](./img/a.png) and [guide](docs/b.md) plus [toc](#toc) and [ext](https://ex.com).'
      const out = rewriteGitMarkdownSource(md, { baseDir: '' })
      expect(out).toContain('data-git-raw="img%2Fa.png"')
      expect(out).toContain(`src="${GIT_RAW_PLACEHOLDER_SRC}"`)
      expect(out).not.toContain('git-raw://')
      expect(out).toContain(`[guide](${gitMarkdownNavHref('docs/b.md')})`)
      expect(out).toContain('[toc](#toc)')
      expect(out).toContain('[ext](https://ex.com)')
    })

    it('resolves against nested baseDir', () => {
      const out = rewriteGitMarkdownSource('![](../shared/x.png)', { baseDir: 'docs/guide' })
      expect(out).toBe(gitMarkdownRawImgHtml('docs/shared/x.png'))
    })

    it('rewrites HTML img/a relative attributes', () => {
      const out = rewriteGitMarkdownSource('<img src="./a.png"><a href="docs/b.md">x</a>', {
        baseDir: ''
      })
      expect(out).toContain(`src="${GIT_RAW_PLACEHOLDER_SRC}"`)
      expect(out).toContain('data-git-raw="a.png"')
      expect(out).not.toContain('git-raw://')
      expect(out).toContain(`href="${gitMarkdownNavHref('docs/b.md')}"`)
    })
  })

  describe('markers', () => {
    it('round-trips nav and raw paths', () => {
      expect(parseGitMarkdownNavHref(gitMarkdownNavHref('docs/a.md'))).toBe('docs/a.md')
      expect(parseGitMarkdownRawHref(gitMarkdownRawHref('readme/images/a.png'))).toBe('readme/images/a.png')
      expect(parseGitMarkdownRawHref(encodeURIComponent('readme/images/a.png'))).toBe('readme/images/a.png')
    })

    it('reads path from img data-git-raw', () => {
      const img = {
        getAttribute: (name) => (name === 'data-git-raw' ? encodeURIComponent('readme/images/a.png') : null)
      }
      expect(parseGitMarkdownRawPathFromImg(img)).toBe('readme/images/a.png')
    })
  })
})
