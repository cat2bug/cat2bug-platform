/** 源码管理（Git 仓库）列表动态列 */
export const GitRepoTableColumnDefaults = [
  { key: 'git.repo.name', prop: 'name', fixed: true, visible: true, align: 'left', minWidth: 140 },
  { key: 'git.repo.slug', prop: 'slug', fixed: false, visible: true, align: 'left', minWidth: 120 },
  { key: 'git.repo.default-branch', prop: 'defaultBranch', fixed: false, visible: true, align: 'center', width: 120 },
  { key: 'git.repo.clone-url', prop: 'cloneUrl', fixed: false, visible: true, align: 'left', minWidth: 280 },
  { key: 'git.repo.remark', prop: 'remark', fixed: false, visible: true, align: 'left', minWidth: 140 }
]
