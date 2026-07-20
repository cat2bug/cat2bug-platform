import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export const REPO_ROOT = path.resolve(__dirname, '../..');

export const SKILLS_ROOT = path.join(
  REPO_ROOT,
  'readme/development/skills/report-templates'
);

export const RESOURCES_ROOT = path.join(
  REPO_ROOT,
  'cat2bug-platform-admin/src/main/resources/报告/模版'
);

export const UI_NODE_MODULES = path.join(REPO_ROOT, 'cat2bug-platform-ui/node_modules');

/** @type {{ relDest: string, globDir: string, exclude: Set<string> }[]} */
export const COPY_SOURCES = [
  {
    relDest: '29119-3',
    globDir: path.join(SKILLS_ROOT, '29119-3'),
    exclude: new Set(['README.md']),
  },
  {
    relDest: '9001/manuals',
    globDir: path.join(SKILLS_ROOT, '9001/manuals'),
    exclude: new Set(['README.md']),
  },
  {
    relDest: '9001/records',
    globDir: path.join(SKILLS_ROOT, '9001/records'),
    exclude: new Set(['README.md']),
  },
];

export const MANIFEST_GROUPS = [
  { code: 'iso-29119-3', title: 'ISO/IEC/IEEE 29119-3:2021', relPrefix: '29119-3' },
  { code: 'iso-9001', title: 'ISO 9001', relPrefixes: ['9001/manuals', '9001/records'] },
];
