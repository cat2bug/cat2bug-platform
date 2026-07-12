const emojiModules = import.meta.glob('@/assets/emoji/default/emoji*.png', {
  eager: true,
  import: 'default'
})

const emojiMap = new Map()

for (const [path, url] of Object.entries(emojiModules)) {
  const match = path.match(/emoji(\d+)\.png$/)
  if (match) {
    emojiMap.set(`emoji${match[1]}`, url)
  }
}

/**
 * @param {number | string} nameOrIndex emoji 序号（1-18）或名称（emoji1）
 * @returns {string | undefined}
 */
export function getEmojiUrl(nameOrIndex) {
  if (typeof nameOrIndex === 'number') {
    return emojiMap.get(`emoji${nameOrIndex}`)
  }
  return emojiMap.get(String(nameOrIndex))
}

export { emojiMap as defaultEmojiMap }
