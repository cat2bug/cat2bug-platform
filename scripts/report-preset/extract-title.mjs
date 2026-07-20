import fs from 'fs';

/**
 * @param {string} mdPath
 * @returns {string}
 */
export function titleFromMarkdownFile(mdPath) {
  const text = fs.readFileSync(mdPath, 'utf8');
  const line = text.split(/\r?\n/).find((l) => /^#\s+/.test(l));
  if (line) {
    return line.replace(/^#\s+/, '').trim();
  }
  const base = mdPath.split(/[/\\]/).pop() || 'template';
  return base.replace(/\.md$/i, '');
}
