/**
 * 工作台角色预设（v3：rows）
 * quick-create 走工具条，不进 rows
 */

import {
  CONFIG_VERSION,
  normalizeV3Config,
  isV3Config,
  rowWithTypes,
  rowsFromWidgetTypes,
  normalizeRows
} from './row-layout'

/** 个人工作台首次种 / 编辑套用（含 custom） */
export const PRESET_IDS = ['tester', 'developer', 'manager', 'outsider', 'custom']

/** 加号创建可选 */
export const CREATE_PRESET_IDS = ['manager', 'tester', 'developer', 'document']

/** 编辑套用可选 */
export const APPLY_PRESET_IDS = ['manager', 'tester', 'developer', 'outsider', 'custom']

function workTab(rows) {
  return { rows: rows.filter(Boolean) }
}

function blankRows() {
  return [
    {
      id: 'r-blank',
      height: { mode: 'row' },
      cols: [{ id: 'c-blank', ratio: 24, height: null, widget: null }]
    }
  ]
}

export const WORKBENCH_PRESETS = {
  tester: {
    id: 'tester',
    titleKey: 'workbench.preset.tester',
    tabs: {
      work: workTab([
        rowWithTypes([{ type: 'my-kpi', ratio: 24 }], { mode: 'content' }),
        rowWithTypes(
          [
            { type: 'my-defects', ratio: 12 },
            { type: 'my-documents', ratio: 12 }
          ],
          { mode: 'row' }
        )
      ])
    }
  },
  developer: {
    id: 'developer',
    titleKey: 'workbench.preset.developer',
    tabs: {
      work: workTab([
        rowWithTypes([{ type: 'my-kpi', ratio: 24 }], { mode: 'content' }),
        rowWithTypes(
          [
            { type: 'defects-pending-repair', ratio: 14 },
            { type: 'recent-commits', ratio: 10 }
          ],
          { mode: 'row' }
        ),
        rowWithTypes([{ type: 'my-documents', ratio: 24 }], { mode: 'row' })
      ])
    }
  },
  manager: {
    id: 'manager',
    titleKey: 'workbench.preset.manager',
    tabs: {
      work: workTab([
        rowWithTypes(
          [
            { type: 'team-weekly-report', ratio: 8 },
            { type: 'team-type-kpi', ratio: 16 }
          ],
          { mode: 'content' }
        ),
        rowWithTypes(
          [
            { type: 'team-defect-trend', ratio: 14 },
            { type: 'plan-burndown', ratio: 10 }
          ],
          { mode: 'fixed', px: 240 }
        ),
        rowWithTypes(
          [
            { type: 'team-defects', ratio: 14 },
            {
              ratio: 10,
              stack: [{ type: 'git-repo-stats' }, { type: 'document-list' }]
            }
          ],
          { mode: 'row' }
        )
      ])
    }
  },
  outsider: {
    id: 'outsider',
    titleKey: 'workbench.preset.outsider',
    tabs: {
      work: workTab([
        rowWithTypes([{ type: 'my-kpi', ratio: 24 }], { mode: 'content' }),
        rowWithTypes(
          [
            { type: 'my-documents', ratio: 12 },
            { type: 'document-list', ratio: 12 }
          ],
          { mode: 'row' }
        )
      ])
    }
  },
  document: {
    id: 'document',
    titleKey: 'workbench.preset.document',
    tabs: {
      work: workTab([
        rowWithTypes(
          [
            { type: 'document-list', ratio: 12 },
            { type: 'my-documents', ratio: 12 }
          ],
          { mode: 'row' }
        ),
        rowWithTypes([{ type: 'document-pin', ratio: 24 }], { mode: 'content' })
      ])
    }
  },
  custom: {
    id: 'custom',
    titleKey: 'workbench.preset.custom',
    tabs: {
      work: { rows: blankRows() }
    }
  },
  /** @deprecated 兼容旧 blank 文案；等同 custom */
  blank: {
    id: 'blank',
    titleKey: 'workbench.preset.blank',
    tabs: {
      work: { rows: blankRows() }
    }
  }
}

/** 项目角色 role_key → 预设（管理优先） */
export function presetIdFromRoleKeys(roleKeys = []) {
  const keys = Array.isArray(roleKeys) ? roleKeys : []
  if (keys.some(k => k === 'project.create-by' || k === 'project.admin')) {
    return 'manager'
  }
  if (keys.some(k => k === 'project.develop' || k === 'project.developer')) {
    return 'developer'
  }
  if (keys.some(k => k === 'project.tester')) {
    return 'tester'
  }
  return 'outsider'
}

export function rowsFromPreset(presetId) {
  const preset = WORKBENCH_PRESETS[presetId] || WORKBENCH_PRESETS.tester
  return JSON.parse(JSON.stringify(preset.tabs.work.rows || []))
}

/** 个人工作台完整 config（无 board） */
export function buildConfigFromPreset(presetId) {
  const preset = WORKBENCH_PRESETS[presetId] || WORKBENCH_PRESETS.tester
  const id = preset.id === 'blank' ? 'custom' : preset.id
  return {
    version: CONFIG_VERSION,
    preset: id,
    tabs: {
      work: {
        rows: JSON.parse(JSON.stringify(preset.tabs.work.rows))
      }
    }
  }
}

/** 自定义 tab 存储形态：{ version, preset, rows } */
export function buildTabConfigFromPreset(presetId) {
  const preset = WORKBENCH_PRESETS[presetId] || WORKBENCH_PRESETS.custom
  const id = preset.id === 'blank' ? 'custom' : preset.id
  return {
    version: CONFIG_VERSION,
    preset: id,
    rows: normalizeRows(JSON.parse(JSON.stringify(preset.tabs.work.rows)))
  }
}

export function parseTabConfig(raw) {
  if (!raw || typeof raw !== 'object') return null
  if (Array.isArray(raw.rows)) {
    return {
      version: CONFIG_VERSION,
      preset: raw.preset || 'custom',
      rows: normalizeRows(raw.rows)
    }
  }
  if (Array.isArray(raw.tabs?.work?.rows)) {
    return {
      version: CONFIG_VERSION,
      preset: raw.preset || 'custom',
      rows: normalizeRows(raw.tabs.work.rows)
    }
  }
  return null
}

/**
 * 读入个人工作台配置：仅接受 v3 work.rows；忽略 board
 * @returns {{ config: object, changed: boolean }}
 */
export function alignLegacyConfig(raw, fallbackPresetId) {
  const v3 = normalizeV3Config(raw)
  if (v3) {
    let changed = Number(raw?.version) !== CONFIG_VERSION
    if (raw?.tabs?.board != null) {
      changed = true
    }
    return { config: v3, changed }
  }
  const known =
    raw &&
    typeof raw === 'object' &&
    raw.preset &&
    (PRESET_IDS.includes(raw.preset) ||
      CREATE_PRESET_IDS.includes(raw.preset) ||
      raw.preset === 'blank')
  const presetId = fallbackPresetId || (known ? raw.preset : 'tester')
  return { config: buildConfigFromPreset(presetId), changed: true }
}

/** @deprecated 兼容旧 import；等价 normalizeV3Config */
export function normalizeConfig(raw) {
  return normalizeV3Config(raw)
}

export { isV3Config, rowsFromWidgetTypes }
