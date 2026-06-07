/** 系统文档页：侧栏树叶子节点扁平化与索引 */

export function flattenDocTreeLeaves(docs, trail = []) {
  const out = []
  for (const item of docs || []) {
    const nextTrail = [...trail, item]
    if (item.path) {
      out.push({ data: item, trail: nextTrail })
    }
    if (item.children && item.children.length) {
      out.push(...flattenDocTreeLeaves(item.children, nextTrail))
    }
  }
  return out
}

export function findDocLeafIndex(leaves, path) {
  if (!path || !leaves || !leaves.length) return -1
  return leaves.findIndex((entry) => entry.data.path === path)
}

export function clampDocIndex(index, length) {
  if (!length) return -1
  if (index < 0) return 0
  if (index >= length) return length - 1
  return index
}
