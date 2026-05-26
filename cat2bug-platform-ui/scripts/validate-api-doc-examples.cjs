#!/usr/bin/env node
/**
 * 校验 readme/production/api/*.md 中每个 HTTP 接口小节是否包含完整六语言 code-tabs
 */
const fs = require('fs')
const path = require('path')

const API_DIR = path.join(__dirname, '../../readme/production/api')
const REQUIRED_TITLES = ['cURL', 'Java', 'Python', 'Node.js', 'PHP', 'C#']
const SKIP_SECTIONS = [
  'creator 创建人对象',
  '缺陷状态与缺陷日志状态对应关系',
  'API 授权配置',
  '接口调用',
  '接口列表',
  '服务地址',
  '示例代码环境要求'
]

function isHttpSection(body) {
  return /\*\*方法\*\*/.test(body) && /\*\*路径\*\*/.test(body)
}

function parseSections(content) {
  const parts = content.split(/^## /m)
  const sections = []
  for (let i = 1; i < parts.length; i++) {
    const chunk = parts[i]
    const nl = chunk.indexOf('\n')
    const title = nl >= 0 ? chunk.slice(0, nl).trim() : chunk.trim()
    const body = nl >= 0 ? chunk.slice(nl + 1) : ''
    sections.push({ title, body })
  }
  return sections
}

function validateCodeTabs(body, sectionTitle) {
  const blockMatch = body.match(/::: code-tabs\n([\s\S]*?)\n:::/)
  if (!blockMatch) {
    return { ok: false, error: '缺少 ::: code-tabs 块' }
  }
  const inner = blockMatch[1]
  const titles = []
  const re = /```\w+\s+title=([^\n]+)/g
  let m
  while ((m = re.exec(inner)) !== null) {
    titles.push(m[1].trim())
  }
  for (const req of REQUIRED_TITLES) {
    if (!titles.includes(req)) {
      return { ok: false, error: `缺少语言: ${req}，已有: ${titles.join(', ')}` }
    }
  }
  if (titles.length < REQUIRED_TITLES.length) {
    return { ok: false, error: `围栏数量不足: ${titles.length}` }
  }
  if (!body.includes('${baseUrl}')) {
    return { ok: false, error: '示例中未使用 ${baseUrl}' }
  }
  return { ok: true }
}

function shouldSkip(title) {
  return SKIP_SECTIONS.some((s) => title.includes(s) || title === s)
}

function main() {
  const files = fs.readdirSync(API_DIR).filter((f) => f.startsWith('api-') && f.endsWith('.md'))
  let errors = []

  for (const file of files) {
    if (file === 'api-intro.md') continue
    const content = fs.readFileSync(path.join(API_DIR, file), 'utf8')
    const sections = parseSections(content)
    for (const { title, body } of sections) {
      if (shouldSkip(title)) continue
      if (!isHttpSection(body)) continue
      const result = validateCodeTabs(body, title)
      if (!result.ok) {
        errors.push(`${file} → 「${title}」: ${result.error}`)
      }
    }
  }

  if (errors.length) {
    console.error('API 文档 code-tabs 校验失败:\n')
    errors.forEach((e) => console.error('  -', e))
    process.exit(1)
  }
  console.log(`校验通过: ${files.length} 个 api-*.md 文件的 HTTP 接口均已包含六语言 code-tabs`)
}

main()
