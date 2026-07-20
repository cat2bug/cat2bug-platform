import fs from 'fs';
import path from 'path';
import {
  COPY_SOURCES,
  MANIFEST_GROUPS,
  RESOURCES_ROOT,
} from './paths.mjs';
import { titleFromMarkdownFile } from './extract-title.mjs';
import { generatePreviews } from './generate-previews.mjs';

/**
 * @param {string} dir
 * @param {Set<string>} exclude
 * @returns {string[]}
 */
function listMarkdownFiles(dir, exclude) {
  if (!fs.existsSync(dir)) {
    throw new Error(`Missing skills directory: ${dir}`);
  }
  return fs
    .readdirSync(dir)
    .filter((name) => name.endsWith('.md') && !exclude.has(name))
    .sort((a, b) => a.localeCompare(b));
}

function copySources() {
  /** @type {{ key: string, title: string, contentPath: string, previewPath: string, mdPath: string, pngPath: string, groupCode: string }[]} */
  const allItems = [];

  for (const src of COPY_SOURCES) {
    const files = listMarkdownFiles(src.globDir, src.exclude);
    const destDir = path.join(RESOURCES_ROOT, src.relDest);
    fs.mkdirSync(destDir, { recursive: true });

    for (const file of files) {
      const stem = file.replace(/\.md$/i, '');
      const from = path.join(src.globDir, file);
      const to = path.join(destDir, file);
      fs.copyFileSync(from, to);

      const contentPath = `${src.relDest}/${file}`.replace(/\\/g, '/');
      const previewPath = `${src.relDest}/${stem}.png`.replace(/\\/g, '/');
      const title = titleFromMarkdownFile(from);

      let groupCode = 'iso-9001';
      let key = stem;
      if (src.relDest === '29119-3') {
        groupCode = 'iso-29119-3';
      } else if (src.relDest === '9001/manuals') {
        key = `manuals/${stem}`;
      } else if (src.relDest === '9001/records') {
        key = `records/${stem}`;
      }

      allItems.push({
        key,
        title,
        contentPath,
        previewPath,
        mdPath: to,
        pngPath: path.join(RESOURCES_ROOT, previewPath),
        groupCode,
      });
    }
  }

  return allItems;
}

/**
 * @param {{ key: string, title: string, contentPath: string, previewPath: string, groupCode: string }[]} allItems
 */
function writeManifest(allItems) {
  const groups = MANIFEST_GROUPS.map((def) => {
    let items;
    if ('relPrefix' in def && def.relPrefix) {
      items = allItems
        .filter((i) => i.groupCode === def.code)
        .map(({ key, title, contentPath, previewPath }) => ({
          key,
          title,
          contentPath,
          previewPath,
        }));
    } else {
      items = allItems
        .filter((i) => i.groupCode === def.code)
        .map(({ key, title, contentPath, previewPath }) => ({
          key,
          title,
          contentPath,
          previewPath,
        }));
    }
    items.sort((a, b) => a.key.localeCompare(b.key));
    return { code: def.code, title: def.title, items };
  });

  const manifestPath = path.join(RESOURCES_ROOT, 'manifest.json');
  fs.writeFileSync(manifestPath, `${JSON.stringify({ groups }, null, 2)}\n`, 'utf8');
}

async function main() {
  fs.mkdirSync(RESOURCES_ROOT, { recursive: true });
  fs.mkdirSync(path.join(RESOURCES_ROOT, '29119-3'), { recursive: true });
  fs.mkdirSync(path.join(RESOURCES_ROOT, '9001/manuals'), { recursive: true });
  fs.mkdirSync(path.join(RESOURCES_ROOT, '9001/records'), { recursive: true });

  const allItems = copySources();
  const previewItems = allItems.map(({ mdPath, pngPath, title }) => ({
    mdPath,
    pngPath,
    title,
  }));

  const { usePlaywright, ok, failed } = await generatePreviews(previewItems);
  writeManifest(allItems);

  const mdCount = allItems.length;
  const pngCount = allItems.filter((i) => fs.existsSync(i.pngPath)).length;

  console.log(`Synced ${mdCount} markdown file(s) → ${RESOURCES_ROOT}`);
  console.log(`Preview PNGs: ${pngCount}/${mdCount} (playwright=${usePlaywright})`);
  if (failed > 0) {
    console.warn(`Preview generation had ${failed} failure(s); placeholders may have been written.`);
  }
  process.exit(failed > mdCount ? 1 : 0);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
