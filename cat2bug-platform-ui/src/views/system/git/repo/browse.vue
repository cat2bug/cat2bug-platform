<template>
  <div class="app-container git-browse-page" v-loading="pageLoading">
    <git-repo-shell
      :repository-id="repositoryId"
      :project-id="repoProjectId"
      :repo-name="repoName"
      :visibility="repoVisibility"
      :active-tab="shellActiveTab"
      @tab-click="onShellTabClick"
    >
    <div v-if="selectedCommitId" class="git-browse-commits-pane">
      <git-repo-commit-detail-panel
        :key="'commit-' + selectedCommitId"
        embedded
        :repository-id="repositoryId"
        :commit-id="String(selectedCommitId)"
        :repo-name="repoName"
        :project-members="projectMembers"
        :branch="currentBrowseBranch() || selectedBranchName || ''"
        @back="clearCommitFocus"
        @browse-files="browseFilesAtCommit"
      />
    </div>

    <div v-else-if="activeTab === 'commits'" class="git-browse-commits-pane">
      <div class="git-browse-commits-toolbar">
        <el-form
          class="git-browse-commits-query"
          :inline="true"
          size="small"
          @submit.prevent
        >
          <el-form-item>
            <el-select
              v-model="currentRef"
              size="small"
              class="git-browse-branch-select"
              :placeholder="$t('git.repo.select-branch')"
              :loading="branchesLoading"
              @change="onCommitFilterRefChange"
            >
              <template #prefix>
                <el-icon class="git-browse-branch-icon">
                  <git-branch-icon />
                </el-icon>
              </template>
              <el-option
                v-for="b in branches"
                :key="b.name"
                :label="b.name"
                :value="b.name"
              >
                <span class="git-browse-branch-option">
                  <el-icon class="git-browse-branch-icon">
                    <git-branch-icon />
                  </el-icon>
                  <span>{{ b.name }}</span>
                </span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select
              v-model="commitFilter.author"
              size="small"
              clearable
              filterable
              class="git-browse-commits-filter-control"
              :placeholder="$t('git.repo.commit.filter.all-authors')"
              :loading="commitFilterContributorsLoading"
              @change="onCommitFilterChange"
            >
              <el-option
                v-for="(c, idx) in commitFilterContributors"
                :key="contributorKey(c, idx)"
                :label="c.displayName || c.email || '-'"
                :value="contributorFilterValue(c)"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-date-picker
              v-model="commitFilter.dateRange"
              type="daterange"
              size="small"
              clearable
              value-format="YYYY-MM-DD"
              range-separator="-"
              class="git-browse-commits-filter-daterange"
              :start-placeholder="$t('git.repo.commit.filter.since')"
              :end-placeholder="$t('git.repo.commit.filter.until')"
              @change="onCommitFilterChange"
            />
          </el-form-item>
        </el-form>
      </div>
      <div v-loading="commitsLoading" class="git-browse-commits">
        <div
          v-for="group in commitsByDate"
          :key="group.dateKey"
          class="git-browse-commit-group"
        >
          <div v-if="group.label" class="git-browse-commit-group-title">{{ group.label }}</div>
          <div v-for="c in group.items" :key="c.id" class="git-browse-commit-item">
            <cat2-bug-avatar :member="memberForAuthor(c)" :size="32" />
            <div class="git-browse-commit-item-body">
              <div class="git-browse-commit-item-msg">
                <a
                  href="javascript:;"
                  class="is-link"
                  @click.prevent="openCommitDetail(c.id || c.shortId)"
                >{{ c.message }}</a>
              </div>
              <div class="git-browse-commit-item-meta">
                <span>{{ c.authorName }}</span>
                <span>{{ relativeTime(c.commitTime) }}</span>
                <a
                  href="javascript:;"
                  class="git-browse-commit-hash is-link"
                  @click.prevent="openCommitDetail(c.id || c.shortId)"
                >{{ c.shortId }}</a>
              </div>
            </div>
          </div>
        </div>
        <div v-if="!commitsLoading && !commits.length" class="git-browse-empty">{{ $t('git.repo.empty-repository') }}</div>
      </div>
    </div>

    <div
      v-show="!selectedCommitId && activeTab === 'code'"
      class="git-browse-layout"
      :class="{ 'has-side-tree': showSideFileTree }"
    >
      <!-- 左侧目录树：仅进入子目录后显示；分支选择在「文件」标题下 -->
      <aside v-if="showSideFileTree" class="git-browse-file-tree">
        <div class="git-browse-file-tree__head">
          <span class="git-browse-file-tree__title">
            <el-icon class="git-browse-file-tree__title-icon"><FolderOpened /></el-icon>
            {{ $t('git.repo.files') }}
          </span>
          <el-dropdown trigger="click" @command="onFileTreeAddCommand">
            <el-button
              class="git-browse-file-tree__add"
              size="small"
              link
              :icon="Plus"
              :title="$t('git.repo.files-add')"
            />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="new-file">
                  <el-icon><Document /></el-icon>
                  <span>{{ $t('git.repo.new-file') }}</span>
                </el-dropdown-item>
                <el-dropdown-item command="new-folder">
                  <el-icon><Folder /></el-icon>
                  <span>{{ $t('git.repo.new-folder') }}</span>
                </el-dropdown-item>
                <el-dropdown-item command="upload-file" divided>
                  <el-icon><Upload /></el-icon>
                  <span>{{ $t('git.repo.upload-file') }}</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="git-browse-file-tree__branch">
          <el-select
            :key="'side-branch-' + (branchSelectValue || '') + '-' + branches.length"
            :model-value="branchSelectValue"
            class="git-browse-branch-select git-browse-branch-select--side"
            :placeholder="$t('git.repo.select-branch')"
            :loading="branchesLoading"
            @change="onBranchSelect"
          >
            <template #prefix>
              <el-icon class="git-browse-branch-icon">
                <git-branch-icon />
              </el-icon>
            </template>
            <el-option
              v-for="b in branches"
              :key="b.name"
              :label="b.name"
              :value="b.name"
            >
              <span class="git-browse-branch-option">
                <el-icon class="git-browse-branch-icon">
                  <git-branch-icon />
                </el-icon>
                <span>{{ b.name }}</span>
              </span>
            </el-option>
          </el-select>
        </div>
        <div class="git-browse-file-tree__body" v-loading="sideTreeBootLoading">
          <el-tree
            v-if="currentRef"
            :key="'side-tree-' + currentRef"
            ref="sideFileTree"
            class="git-browse-file-tree__el"
            node-key="path"
            lazy
            highlight-current
            :load="loadSideTreeNode"
            :props="sideTreeProps"
            :expand-on-click-node="false"
            @node-click="onSideTreeNodeClick"
          >
            <template #default="{ data }">
              <span
                class="git-browse-file-tree__node"
                :class="{ 'is-active': isSideTreeNodeActive(data) }"
              >
                <el-icon class="git-browse-file-tree__icon" :class="{ 'is-dir': data.type === 'tree' }">
                  <Folder v-if="data.type === 'tree'" />
                  <Document v-else />
                </el-icon>
                <span class="git-browse-file-tree__label" :title="data.name">{{ data.name }}</span>
              </span>
            </template>
          </el-tree>
          <div v-else class="git-browse-file-tree__empty">{{ $t('git.repo.select-branch') }}</div>
        </div>
      </aside>

      <div class="git-browse-main" :class="{ 'git-browse-main--blob': viewMode === 'blob' }">
        <!-- 工具栏：根目录显示分支；子目录显示路径面包屑 -->
        <div class="git-browse-toolbar">
          <div class="git-browse-toolbar-left">
            <template v-if="showSideFileTree">
              <div class="git-browse-breadcrumb git-browse-breadcrumb--toolbar">
                <el-breadcrumb separator="/">
                  <el-breadcrumb-item>
                    <a href="javascript:;" @click="navigateToPath('')">{{ repoName || $t('git.repo.path-root') }}</a>
                  </el-breadcrumb-item>
                  <el-breadcrumb-item v-for="(seg, idx) in breadcrumbSegments" :key="idx">
                    <a
                      v-if="idx < breadcrumbSegments.length - 1 || viewMode === 'blob'"
                      href="javascript:;"
                      @click="navigateToPath(breadcrumbPathAt(idx))"
                    >{{ seg }}</a>
                    <span v-else>{{ seg }}</span>
                  </el-breadcrumb-item>
                </el-breadcrumb>
                <el-button
                  class="git-browse-path-copy"
                  size="small"
                  link
                  :icon="CopyDocument"
                  :title="$t('git.repo.copy-path')"
                  @click="copyCurrentPath"
                />
              </div>
            </template>
            <template v-else>
              <el-select
                :key="'toolbar-branch-' + (branchSelectValue || '') + '-' + branches.length"
                :model-value="branchSelectValue"
                size="small"
                class="git-browse-branch-select"
                :placeholder="$t('git.repo.select-branch')"
                :loading="branchesLoading"
                @change="onBranchSelect"
              >
                <template #prefix>
                  <el-icon class="git-browse-branch-icon">
                    <git-branch-icon />
                  </el-icon>
                </template>
                <el-option
                  v-for="b in branches"
                  :key="b.name"
                  :label="b.name"
                  :value="b.name"
                >
                  <span class="git-browse-branch-option">
                    <el-icon class="git-browse-branch-icon">
                      <git-branch-icon />
                    </el-icon>
                    <span>{{ b.name }}</span>
                  </span>
                </el-option>
              </el-select>
              <el-button plain size="small" class="git-browse-meta-chip" @click="openBranchesPage">
                <el-icon class="git-browse-meta-chip-icon">
                  <git-branch-icon />
                </el-icon>
                <span>{{ $t('git.repo.branch') }}</span>
              </el-button>
              <el-button plain size="small" class="git-browse-meta-chip" @click="openTagsPage">
                <git-tag-icon class="git-browse-meta-chip-icon" />
                <span>{{ $t('git.repo.tags.title') }}</span>
              </el-button>
            </template>
          </div>
          <div class="git-browse-toolbar-right">
            <el-dropdown
              v-if="!showSideFileTree"
              trigger="click"
              @command="onFileTreeAddCommand"
            >
              <el-button
                class="git-browse-toolbar-add"
                size="small"
                plain
                :icon="Plus"
                :title="$t('git.repo.files-add')"
              />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="new-file">
                    <el-icon><Document /></el-icon>
                    <span>{{ $t('git.repo.new-file') }}</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="new-folder">
                    <el-icon><Folder /></el-icon>
                    <span>{{ $t('git.repo.new-folder') }}</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="upload-file" divided>
                    <el-icon><Upload /></el-icon>
                    <span>{{ $t('git.repo.upload-file') }}</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-dropdown trigger="click" :hide-on-click="false">
              <el-button type="primary" plain size="small" class="git-browse-clone-btn">
                {{ $t('git.repo.clone-download') }}
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <div class="git-browse-clone-panel" @click.stop>
                  <div class="git-browse-clone-proto-row">
                    <div class="git-browse-clone-proto">HTTPS</div>
                    <el-button
                      class="git-browse-clone-zip"
                      type="primary"
                      link
                      :loading="archiveDownloading"
                      :disabled="!repositoryId || !archiveRef"
                      @click="downloadSourceZip"
                    >
                      <el-icon class="el-icon--left"><Download /></el-icon>
                      {{ $t('git.repo.download-source-zip') }}
                    </el-button>
                  </div>
                  <div class="git-browse-clone-row">
                    <el-input :model-value="fullCloneUrl" readonly class="git-browse-clone-url" />
                    <el-button
                      class="git-browse-clone-copy"
                      :icon="CopyDocument"
                      :title="$t('git.repo.copy')"
                      @click="copyCloneUrl"
                    />
                  </div>

                  <div class="git-browse-clone-tips">
                    <div class="git-browse-clone-tips-title">{{ $t('git.repo.clone-tips') }}</div>

                    <p class="git-browse-clone-tips-desc">{{ $t('git.repo.clone-cmd-hint') }}</p>
                    <div class="git-browse-clone-code">
                      <pre>{{ gitCloneCommand }}</pre>
                      <el-button
                        size="small"
                        link
                        :icon="CopyDocument"
                        :title="$t('git.repo.copy')"
                        @click="copyText(gitCloneCommand)"
                      />
                    </div>

                    <p class="git-browse-clone-tips-desc">{{ $t('git.repo.clone-config-hint') }}</p>
                    <div class="git-browse-clone-code">
                      <pre>{{ gitConfigCommands }}</pre>
                      <el-button
                        size="small"
                        link
                        :icon="CopyDocument"
                        :title="$t('git.repo.copy')"
                        @click="copyText(gitConfigCommands)"
                      />
                    </div>

                    <p class="git-browse-clone-tips-desc">{{ $t('git.repo.clone-https-auth-hint') }}</p>
                    <div class="git-browse-clone-code git-browse-clone-code--example">
                      <pre>{{ gitHttpsAuthExample }}</pre>
                    </div>
                  </div>
                </div>
              </template>
            </el-dropdown>
          </div>
        </div>

        <!-- 最近提交条 -->
        <div v-if="latestCommit && viewMode !== 'blob'" class="git-browse-commit-bar">
          <div class="git-browse-commit-bar-left">
            <cat2-bug-avatar :member="memberForAuthor(latestCommit)" :size="28" />
            <span class="git-browse-author">{{ latestCommit.authorName || '-' }}</span>
            <a
              href="javascript:;"
              class="git-browse-commit-msg is-link"
              :title="latestCommit.message"
              @click.prevent="openCommitDetail(latestCommit.id || latestCommit.shortId)"
            >{{ latestCommit.message }}</a>
            <a
              href="javascript:;"
              class="git-browse-commit-hash is-link"
              @click.prevent="openCommitDetail(latestCommit.id || latestCommit.shortId)"
            >{{ latestCommit.shortId }}</a>
            <span class="git-browse-commit-time">{{ relativeTime(latestCommit.commitTime) }}</span>
          </div>
          <a class="git-browse-commit-total" href="javascript:;" @click.prevent="openCommitsList">
            {{ $t('git.repo.commit-count', { n: commitTotal }) }}
            <el-icon><component :is="ArrowRightIcon" /></el-icon>
          </a>
        </div>

        <!-- 根目录不展示仅仓库名的面包屑；进入子路径后由工具栏/侧栏承担 -->
        <div v-if="!showSideFileTree && breadcrumbSegments.length" class="git-browse-breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>
              <a href="javascript:;" @click="navigateToPath('')">{{ repoName || $t('git.repo.path-root') }}</a>
            </el-breadcrumb-item>
            <el-breadcrumb-item v-for="(seg, idx) in breadcrumbSegments" :key="idx">
              <a
                v-if="idx < breadcrumbSegments.length - 1 || viewMode === 'blob'"
                href="javascript:;"
                @click="navigateToPath(breadcrumbPathAt(idx))"
              >{{ seg }}</a>
              <span v-else>{{ seg }}</span>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <!-- 文件树 -->
        <div v-if="viewMode === 'tree'" v-loading="treeLoading" class="git-browse-tree-card">
          <div
            v-for="row in treeEntries"
            :key="row.path"
            class="git-browse-tree-row"
            @click="handleTreeRowClick(row)"
          >
            <div class="git-browse-tree-name">
              <el-icon class="git-browse-file-icon" :class="{ 'is-dir': row.type === 'tree' }">
                <Folder v-if="row.type === 'tree'" />
                <Document v-else />
              </el-icon>
              <span>{{ row.name }}</span>
            </div>
            <div class="git-browse-tree-msg" :title="row.lastCommitMessage">{{ row.lastCommitMessage || '—' }}</div>
            <div class="git-browse-tree-time">{{ relativeTime(row.lastCommitTime) }}</div>
          </div>
          <div v-if="!treeLoading && !treeEntries.length" class="git-browse-empty">{{ $t('git.repo.empty-repository') }}</div>
        </div>

        <!-- 根目录 README / LICENSE（有正文才显示） -->
        <div
          v-if="showRootDocs"
          v-loading="rootDocsLoading"
          class="git-browse-root-docs"
        >
          <el-tabs
            v-model="activeRootDocTab"
            class="git-browse-root-docs-tabs"
            @tab-click="onRootDocTabClick"
          >
            <el-tab-pane
              v-for="tab in displayRootDocTabs"
              :key="tab.key"
              :name="tab.key"
            >
              <template #label>
                <span class="git-browse-root-doc-tab-label">
                  <el-icon v-if="tab.key === 'readme'" class="git-browse-root-doc-tab-icon">
                    <Document />
                  </el-icon>
                  <el-icon v-else class="git-browse-root-doc-tab-icon">
                    <svg
                      viewBox="0 0 24 24"
                      width="1em"
                      height="1em"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      aria-hidden="true"
                    >
                      <path d="m16 16 3-8 3 8c-.87.65-1.92 1-3 1s-2.13-.35-3-1Z" />
                      <path d="m2 16 3-8 3 8c-.87.65-1.92 1-3 1s-2.13-.35-3-1Z" />
                      <path d="M7 21h10" />
                      <path d="M12 3v18" />
                      <path d="M3 7h2c2 0 5-1 7-2 2 1 5 2 7 2h2" />
                    </svg>
                  </el-icon>
                  {{ tab.label }}
                </span>
              </template>
            </el-tab-pane>
          </el-tabs>
          <div v-if="activeRootDoc && hasRootDocBody" class="git-browse-root-docs-panel">
            <div v-if="activeRootDocState.loading" class="git-browse-empty">{{ $t('loading') }}</div>
            <div v-else class="git-browse-root-docs-content" :class="{ 'has-toc': rootDocToc.length > 0 }">
              <aside
                v-if="rootDocToc.length"
                class="git-browse-doc-toc"
                :style="{ width: rootDocTocWidth + 'px' }"
              >
                <div class="git-browse-doc-toc__title">{{ $t('git.repo.doc-toc') }}</div>
                <ul class="git-browse-doc-toc__list">
                  <li
                    v-for="item in rootDocToc"
                    :key="item.id"
                    class="git-browse-doc-toc__item"
                    :class="'is-level-' + Math.min(6, item.level || 1)"
                    :style="{ paddingLeft: ((Math.max(1, item.level || 1) - 1) * 6 + 2) + 'px' }"
                    :title="item.text"
                    @click="scrollToRootDocHeading(item)"
                  >
                    <span class="git-browse-doc-toc__dot" aria-hidden="true" />
                    <span class="git-browse-doc-toc__text">{{ item.text }}</span>
                  </li>
                </ul>
              </aside>
              <div
                v-if="rootDocToc.length"
                class="git-browse-doc-toc-resizer"
                :title="$t('git.repo.resize-toc')"
                @mousedown.prevent="startRootDocTocResize"
              />
              <div ref="rootDocMarkdown" class="git-browse-root-docs-body markdown-body">
                <git-repo-markdown
                  :key="activeRootDoc.path + ':' + (activeRootDocState.content || '').length"
                  :content="activeRootDocState.content"
                  :file-path="activeRootDoc.path"
                  :repository-id="repositoryId"
                  :git-ref="currentRef"
                  @navigate="openRepoPathFromMarkdown"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 文件内容 -->
        <div v-if="viewMode === 'blob'" v-loading="blobLoading" class="git-browse-blob-card">
          <template v-if="blob">
            <header class="git-browse-blob-header">
              <div class="git-browse-blob-header-left">
                <el-icon class="git-browse-blob-file-icon" aria-hidden="true"><Document /></el-icon>
                <span class="git-browse-blob-file-name">{{ blobFileName }}</span>
                <span v-if="blob.size != null && blob.size !== ''" class="git-browse-blob-file-size">
                  {{ fileSizeUnit(blob.size) }}
                </span>
              </div>
              <div class="git-browse-blob-actions">
                <el-button size="small" plain @click="copyText(blob.content)">
                  {{ $t('git.repo.blob.copy-all') }}
                </el-button>
                <el-button size="small" plain @click="onBlobFeatureWip">
                  {{ $t('git.repo.blob.edit') }}
                </el-button>
                <el-button size="small" plain :loading="blobDownloading" @click="downloadBlobFile">
                  {{ $t('git.repo.blob.download') }}
                </el-button>
                <el-button size="small" plain @click="openCommitsList">
                  {{ $t('git.repo.blob.history') }}
                </el-button>
                <el-button
                  v-if="canManageGitRepo"
                  size="small"
                  plain
                  :icon="DeleteIcon"
                  :title="$t('git.repo.blob.delete')"
                  :aria-label="$t('git.repo.blob.delete')"
                  @click="onBlobFeatureWip"
                />
              </div>
            </header>
            <div v-if="latestCommit" class="git-browse-blob-commit">
              <span class="git-browse-blob-commit-text">
                {{
                  $t('git.repo.blob.committed-by', {
                    name: latestCommit.authorName || '-',
                    time: relativeTime(latestCommit.commitTime)
                  })
                }}
              </span>
              <span class="git-browse-blob-commit-sep" aria-hidden="true">·</span>
              <a
                href="javascript:;"
                class="git-browse-blob-commit-link is-link"
                @click.prevent="openCommitsList"
              >{{ $t('git.repo.blob.all-updates') }}</a>
            </div>
            <div class="git-browse-blob-body">
              <div v-if="blob.binary" class="git-browse-empty">{{ $t('git.repo.file-binary') }}</div>
              <div v-else-if="!blob.content && blob.content !== 0" class="git-browse-empty">
                {{ $t('git.repo.file-empty') }}
              </div>
              <template v-else>
                <div v-if="blob.truncated" class="git-browse-truncated">{{ $t('git.repo.file-truncated') }}</div>
                <div class="git-browse-blob-code">
                  <div class="git-browse-blob-gutter" aria-hidden="true">
                    <pre class="git-browse-blob-ln-pre">{{ blobLineNumbersText }}</pre>
                  </div>
                  <pre class="git-browse-code git-browse-blob-code-pre"><code>{{ blob.content }}</code></pre>
                </div>
              </template>
            </div>
          </template>
        </div>
      </div>

      <!-- 右侧栏：仅仓库根目录概览显示；进入文件/文件夹后隐藏 -->
      <aside v-if="!showSideFileTree" class="git-browse-aside">
        <section class="git-aside-block">
          <h4 class="git-aside-title">{{ $t('git.repo.aside.intro') }}</h4>
          <p class="git-aside-desc">{{ repoRemark || $t('git.repo.no-description') }}</p>
          <ul class="git-aside-links">
            <li>
              <el-icon><Share /></el-icon>
              <span>{{ $t('git.repo.default-branch') }}: {{ defaultBranch || currentRef || '-' }}</span>
            </li>
            <li>
              <el-icon><Link /></el-icon>
              <a href="javascript:;" @click="copyCloneUrl">{{ $t('git.repo.clone-url') }}</a>
            </li>
          </ul>
        </section>

        <section class="git-aside-block">
          <div class="git-aside-title-row">
            <h4 class="git-aside-title">{{ $t('git.repo.aside.releases') }}</h4>
            <el-button
              v-if="canManageGitRepo"
              link
              size="small"
              class="git-aside-action"
              @click="openCreateRelease"
            >{{ $t('git.repo.release.create') }}</el-button>
          </div>
          <div v-loading="releasesLoading" class="git-aside-releases-wrap">
          <ul v-if="!releasesLoading && releases.length" class="git-aside-releases">
            <li v-for="r in releasesAsidePreview" :key="r.releaseId" class="git-aside-release-item">
              <a
                href="javascript:;"
                class="git-aside-release-title"
                :title="r.title"
                @click.prevent="openReleaseDetail(r)"
              >
                <git-release-icon class="git-aside-release-icon" />
                <span class="git-aside-release-title-text">{{ r.title || r.tagName }}</span>
              </a>
              <div class="git-aside-release-meta">
                <span class="git-aside-release-tag">{{ r.tagName }}</span>
                <span class="git-aside-release-sep">·</span>
                <span class="git-aside-release-time">{{ releaseRelativeTime(r) }}</span>
                <el-tag v-if="r.draft === 1" size="small" type="info" effect="plain">Draft</el-tag>
              </div>
            </li>
          </ul>
          <div v-else-if="!releasesLoading" class="git-aside-empty">
            {{ $t('git.repo.release.empty') }}
          </div>
          </div>
        </section>

        <section v-if="contributorCount > 0" class="git-aside-block">
          <div class="git-aside-title-row">
            <h4 class="git-aside-title">{{ $t('git.repo.aside.contributors', { n: contributorCount }) }}</h4>
            <el-button link size="small" class="git-aside-action" @click="openContributorsDialog">
              {{ $t('git.repo.contributors.all') }}
            </el-button>
          </div>
          <div class="git-aside-contributors">
            <cat2-bug-avatar
              v-for="(c, idx) in contributorsAsidePreview"
              :key="contributorKey(c, idx)"
              :member="memberForContributor(c)"
              :size="32"
            />
          </div>
        </section>

        <section class="git-aside-block">
          <h4 class="git-aside-title">{{ $t('git.repo.aside.activity') }}</h4>
          <div v-if="recentActivity.length" class="git-aside-activity">
            <div v-for="c in recentActivity" :key="c.id" class="git-aside-activity-item">
              <cat2-bug-avatar :member="memberForAuthor(c)" :size="32" />
              <div class="git-aside-activity-body">
                <div class="git-aside-activity-text">
                  <span class="git-aside-activity-time">{{ relativeTime(c.commitTime) }}</span>
                  <strong>{{ c.authorName || '-' }}</strong>
                  {{ $t('git.repo.aside.pushed') }}
                  <a
                    href="javascript:;"
                    class="git-aside-hash is-link"
                    @click.prevent="openCommitDetail(c.id || c.shortId)"
                  >{{ c.shortId }}</a>
                </div>
                <a
                  href="javascript:;"
                  class="git-aside-activity-msg is-link"
                  :title="c.message"
                  @click.prevent="openCommitDetail(c.id || c.shortId)"
                >{{ c.message }}</a>
              </div>
            </div>
          </div>
          <div v-else class="git-aside-empty">{{ $t('git.repo.empty-repository') }}</div>
        </section>
      </aside>
    </div>

    <div v-show="!selectedCommitId && activeTab === 'releases'" class="git-browse-releases">
      <div class="git-browse-releases__toolbar">
        <h2 class="git-browse-releases__heading">
          <git-release-icon class="git-browse-releases__heading-icon" />
          {{ $t('git.repo.aside.releases') }}
        </h2>
        <el-button
          v-if="canManageGitRepo"
          type="primary"
          plain
          size="small"
          :icon="$legacyIcon('el-icon-plus')"
          @click="openCreateRelease"
        >{{ $t('git.repo.release.create-dialog-title') }}</el-button>
      </div>

      <div v-loading="releasesLoading" class="git-browse-releases__timeline">
        <git-repo-release-detail-panel
          v-for="r in releasesTimeline"
          :id="'git-release-item-' + r.releaseId"
          :key="r.releaseId + '-' + releasesRefreshToken"
          :repository-id="repositoryId"
          :release-id="r.releaseId"
          :is-latest="isLatestRelease(r)"
          :highlighted="false"
          :project-members="projectMembers"
          @deleted="onReleaseDeleted"
          @edit="openEditRelease"
        />
        <cat2-bug-empty
          v-if="!releasesLoading && !releasesTimeline.length"
          class="git-releases-empty"
        >
          <template #description>
            <p class="git-releases-empty__title">{{ $t('git.repo.release.empty') }}</p>
            <p class="git-releases-empty__hint">{{ $t('git.repo.release.empty-hint') }}</p>
          </template>
        </cat2-bug-empty>
      </div>
    </div>

    </git-repo-shell>

    <el-dialog
      v-model="contributorsDialogVisible"
      :title="$t('git.repo.contributors.title')"
      width="480px"
      append-to-body
      class="git-contributors-dialog"
    >
      <div v-loading="contributorsLoading" class="git-contributors-list">
        <div v-for="(c, idx) in contributors" :key="contributorKey(c, idx)" class="git-contributors-list-item">
          <cat2-bug-avatar :member="memberForContributor(c)" :size="36" />
          <div class="git-contributors-list-body">
            <div class="git-contributors-list-name">{{ c.displayName || '-' }}</div>
            <div v-if="c.email" class="git-contributors-list-email">{{ c.email }}</div>
          </div>
          <span class="git-contributors-list-count">{{ $t('git.repo.contributors.commits', { n: c.commitCount }) }}</span>
        </div>
      </div>
    </el-dialog>

    <el-dialog
      v-model="createReleaseVisible"
      :title="editingReleaseId
        ? $t('git.repo.release.edit-dialog-title')
        : $t('git.repo.release.create-dialog-title')"
      width="960px"
      append-to-body
      class="git-create-release-dialog"
      @closed="resetCreateReleaseForm"
    >
      <el-form ref="createReleaseFormRef" :model="createReleaseForm" label-width="100px" size="default">
        <el-form-item :label="$t('git.repo.release.form.tag')" required>
          <el-input
            v-model="createReleaseForm.tagName"
            placeholder="v1.0.0"
            :disabled="!!editingReleaseId"
          />
        </el-form-item>
        <el-form-item :label="$t('git.repo.release.form.title')" required>
          <el-input v-model="createReleaseForm.title" />
        </el-form-item>
        <el-form-item v-if="!editingReleaseId" :label="$t('git.repo.release.form.target-ref')">
          <el-select
            v-model="createReleaseForm.targetRef"
            filterable
            style="width: 100%"
            :loading="branchesLoading"
            :placeholder="$t('git.repo.select-branch')"
          >
            <el-option
              v-for="b in branches"
              :key="b.name"
              :label="b.name"
              :value="b.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('git.repo.release.form.body')" class="git-create-release-body-item">
          <div class="git-create-release-markdown">
            <cat2-bug-markdown
              v-if="createReleaseVisible"
              :key="'release-body-' + createReleaseVisible + '-' + (editingReleaseId || 'new')"
              v-model:content="createReleaseForm.body"
              :placeholder="$t('git.repo.release.form.body-placeholder')"
              :exclude-tools="createReleaseMarkdownExcludeTools"
              :show-preview="false"
            />
          </div>
        </el-form-item>
        <el-form-item v-if="!editingReleaseId" :label="$t('git.repo.release.form.assets')">
          <el-upload
            ref="createReleaseUpload"
            class="git-create-release-upload"
            action="#"
            drag
            :auto-upload="false"
            :limit="CREATE_RELEASE_MAX_ASSETS"
            :file-list="createReleaseAssetList"
            :on-change="onCreateReleaseAssetChange"
            :on-remove="onCreateReleaseAssetRemove"
            :on-exceed="onCreateReleaseAssetExceed"
            multiple
          >
            <el-icon class="git-create-release-upload__icon"><Upload /></el-icon>
            <div class="el-upload__text">
              {{ $t('git.repo.release.form.assets-drag') }}
              <em>{{ $t('git.repo.release.form.assets-select') }}</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">{{ $t('git.repo.release.form.assets-tip') }}</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item :label="$t('git.repo.release.form.draft')">
          <el-switch v-model="createReleaseForm.draft" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createReleaseVisible = false">{{ $t('cancel') }}</el-button>
        <el-button type="primary" :loading="createReleaseSubmitting" @click="submitCreateRelease">
          {{ $t('ok') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { h, markRaw } from 'vue'
import { FolderOpened, Folder, Document, Share, ArrowDown, ArrowRight, Link, CopyDocument, Plus, Upload, Download, Delete } from '@element-plus/icons-vue'
import {
  getGitRepo,
  listGitRepoBranches,
  listGitRepoTree,
  getGitRepoBlob,
  listGitRepoCommits,
  getGitRepoCommitCount,
  gitRepoCommitDetailRoute,
  listGitRepoContributors,
  listGitRepoReleases,
  createGitRepoRelease,
  updateGitRepoRelease,
  uploadGitRepoReleaseAsset,
  resolveGitRepoByProjectAndSlug,
  downloadGitRepoArchive,
  fetchGitRepoRawBlob
} from '@/api/system/gitRepo'
import { saveAs } from 'file-saver'
import { getCurrentProjectId } from '@/utils/store-context'
import { groupCommitsByDate } from '@/utils/git-commits-by-date'
import { applyBrowseRefState, detachedBrowseRefOption, resolveBrowseBranchName } from '@/utils/git-browse-ref'
import {
  gitRepoBrowseRoute,
  gitRepoBranchesRoute,
  gitRepoReleaseDetailRoute,
  gitRepoTagsRoute,
  resolveBrowseActiveTab
} from '@/utils/git-repo-routes'
import { listMemberOfProject } from '@/api/system/project'
import Cat2BugAvatar from '@/components/Cat2BugAvatar'
import Cat2BugMarkdown from '@/components/Cat2BugMarkdown'
import GitRepoMarkdown from './GitRepoMarkdown.vue'
import GitRepoReleaseDetailPanel from './release-detail.vue'
import GitRepoCommitDetailPanel from './commit-detail.vue'
import GitRepoShell from './GitRepoShell.vue'
import GitTagIcon from './GitTagIcon.vue'
import GitReleaseIcon from './GitReleaseIcon.vue'
import { extractHeadings } from '@/utils/markdown-toc'
import { useUserStore } from '@/stores/user'
import { checkPermi } from '@/utils/permission'
import { formatTime, fileSizeUnit } from '@/utils/index'
import { parseTime } from '@/utils/ruoyi'

const CREATE_RELEASE_MAX_ASSETS = 20
const CREATE_RELEASE_MAX_ASSET_BYTES = 100 * 1024 * 1024

const README_NAMES = new Set(['readme.md'])
const LICENSE_NAMES = new Set(['license', 'license.md', 'licence', 'licence.md'])
const ROOT_DOC_TOC_WIDTH_DEFAULT = 200
const ROOT_DOC_TOC_WIDTH_MIN = 140
const ROOT_DOC_TOC_WIDTH_MAX = 360

function clampRootDocTocWidth(width) {
  const n = Number(width)
  if (!Number.isFinite(n)) return ROOT_DOC_TOC_WIDTH_DEFAULT
  return Math.min(ROOT_DOC_TOC_WIDTH_MAX, Math.max(ROOT_DOC_TOC_WIDTH_MIN, Math.round(n)))
}

/** 分支图标（render，避免 runtime template 在 Vite 下不编译导致空白） */
const GitBranchIcon = {
  name: 'GitBranchIcon',
  render() {
    return h(
      'svg',
      {
        viewBox: '0 0 24 24',
        width: '1em',
        height: '1em',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '2',
        'stroke-linecap': 'round',
        'stroke-linejoin': 'round',
        'aria-hidden': 'true'
      },
      [
        h('circle', { cx: '6', cy: '5', r: '2.5' }),
        h('circle', { cx: '6', cy: '19', r: '2.5' }),
        h('circle', { cx: '18', cy: '7', r: '2.5' }),
        h('path', { d: 'M6 7.5v9' }),
        h('path', { d: 'M8.5 5.5h2a5 5 0 0 1 5 5' })
      ]
    )
  }
}

export default {
  name: 'GitRepoBrowse',
  components: {
    FolderOpened,
    Folder,
    Document,
    Share,
    ArrowDown,
    Link,
    CopyDocument,
    Plus,
    Upload,
    Download,
    Cat2BugAvatar,
    Cat2BugMarkdown,
    GitRepoMarkdown,
    GitRepoReleaseDetailPanel,
    GitRepoCommitDetailPanel,
    GitRepoShell,
    GitBranchIcon,
    GitTagIcon,
    GitReleaseIcon
  },
  data() {
    return {
      // :icon 需要实例属性；markRaw 避免图标组件被做成响应式
      CopyDocument: markRaw(CopyDocument),
      Plus: markRaw(Plus),
      DeleteIcon: markRaw(Delete),
      ArrowRightIcon: markRaw(ArrowRight),
      repositoryId: null,
      /** Bumps on each init so stale branch/tree responses are ignored after repo switch */
      loadSeq: 0,
      repoName: '',
      repoRemark: '',
      repoVisibility: 'project',
      repoSlug: '',
      repoProjectId: null,
      repoTeamKey: '',
      repoProjectKey: '',
      cloneUrlPath: '',
      defaultBranch: '',
      branches: [],
      branchesLoading: false,
      currentRef: null,
      /** Branch shown in select while currentRef may be a commit SHA */
      selectedBranchName: null,
      activeTab: resolveBrowseActiveTab(this.$route),
      selectedReleaseId: (this.$route && this.$route.params.releaseId) || null,
      selectedCommitId: (this.$route && this.$route.params.commitId) || null,
      currentPath: '',
      viewMode: 'tree',
      treeEntries: [],
      treeLoading: false,
      blob: null,
      blobLoading: false,
      blobDownloading: false,
      commits: [],
      commitsLoading: false,
      /** Full reachable commit count for currentRef (not capped by list limit) */
      commitTotal: 0,
      commitFilter: {
        author: '',
        dateRange: null
      },
      commitFilterContributors: [],
      commitFilterContributorsLoading: false,
      pageLoading: false,
      archiveDownloading: false,
      projectMembers: [],
      /** 根目录文档 tab：[{ key, name, path }] */
      rootDocTabs: [],
      activeRootDocTab: '',
      /** 用户是否手动点过根文档 tab；未点过时始终优先 README */
      rootDocTabUserSelected: false,
      /** path -> { content, loading, binary } */
      rootDocCache: {},
      rootDocToc: [],
      rootDocTocWidth: ROOT_DOC_TOC_WIDTH_DEFAULT,
      rootDocTocResizing: false,
      sideTreeBootLoading: false,
      sideTreeProps: {
        label: 'name',
        children: 'children',
        isLeaf: 'isLeaf'
      },
      releases: [],
      releasesLoading: false,
      contributors: [],
      contributorsLoading: false,
      contributorsDialogVisible: false,
      createReleaseVisible: false,
      createReleaseSubmitting: false,
      createReleaseAssetList: [],
      editingReleaseId: null,
      releasesRefreshToken: 0,
      createReleaseMarkdownExcludeTools: ['siding-9899', 'project', 'case', 'defect', 'member', 'save', 'plan'],
      createReleaseForm: {
        tagName: '',
        title: '',
        body: '',
        targetRef: '',
        draft: 0
      },
      CREATE_RELEASE_MAX_ASSETS
    }
  },
  computed: {
    /** 当前浏览路径（相对仓库根）：目录或文件 */
    currentBrowsePath() {
      if (this.viewMode === 'blob' && this.blob && this.blob.path) {
        return this.blob.path
      }
      return this.currentPath || ''
    },
    /** 左侧树高亮路径：目录用 currentPath，文件用 blob.path */
    sideTreeActivePath() {
      return this.currentBrowsePath
    },
    /** 进入子目录，或打开任意文件时显示左侧树；仓库根目录概览隐藏 */
    showSideFileTree() {
      if (this.viewMode === 'blob') return true
      return !!(this.currentPath && String(this.currentPath).trim())
    },
    /** Primary shell tabs: commits list still highlights「代码」 */
    shellActiveTab() {
      return this.activeTab === 'releases' ? 'releases' : 'code'
    },
    showRootDocs() {
      if (this.viewMode !== 'tree' || this.currentPath) return false
      if (this.rootDocsLoading) return this.rootDocTabs.length > 0
      return this.visibleRootDocTabs.length > 0
    },
    rootDocsLoading() {
      if (!this.rootDocTabs.length) return false
      return this.rootDocTabs.some(tab => {
        const state = this.rootDocCache[tab.path]
        return !state || state.loading
      })
    },
    /** 加载中展示候选 tab；加载完只保留有正文的 */
    displayRootDocTabs() {
      return this.rootDocsLoading ? this.rootDocTabs : this.visibleRootDocTabs
    },
    /** 仅展示有正文的文档 tab（空文件 / 二进制不占位） */
    visibleRootDocTabs() {
      return this.rootDocTabs.filter(tab => this.isRootDocContentVisible(this.rootDocCache[tab.path]))
    },
    hasRootDocBody() {
      const state = this.activeRootDocState
      return !!(state && (state.loading || this.isRootDocContentVisible(state)))
    },
    activeRootDoc() {
      const tabs = this.displayRootDocTabs
      return tabs.find(t => t.key === this.activeRootDocTab) || tabs[0] || null
    },
    activeRootDocState() {
      if (!this.activeRootDoc) {
        return { content: '', loading: false, binary: false }
      }
      return this.rootDocState(this.activeRootDoc.path)
    },
    breadcrumbSegments() {
      const path = this.viewMode === 'blob' ? this.blob && this.blob.path : this.currentPath
      if (!path) return []
      return path.split('/').filter(Boolean)
    },
    blobFileName() {
      const path = this.blob && this.blob.path
      if (!path) return ''
      const parts = String(path).split('/').filter(Boolean)
      return parts.length ? parts[parts.length - 1] : path
    },
    blobCodeLines() {
      if (!this.blob || this.blob.binary) return []
      const content = this.blob.content
      if (content == null || content === '') return ['']
      return String(content).split(/\r?\n/)
    },
    blobLineNumbersText() {
      const n = this.blobCodeLines.length
      if (!n) return '1'
      return Array.from({ length: n }, (_, i) => i + 1).join('\n')
    },
    latestCommit() {
      return this.commits.length ? this.commits[0] : null
    },
    recentActivity() {
      return this.commits.slice(0, 5)
    },
    canManageGitRepo() {
      return checkPermi(['system:git:repo:manage'])
    },
    contributorCount() {
      return this.contributors.length
    },
    contributorsAsidePreview() {
      return this.contributors.slice(0, 5)
    },
    releasesAsidePreview() {
      return this.releases.slice(0, 5)
    },
    /** 侧栏点进单条时只渲染该发行版；点顶部 Tab 清空 selectedReleaseId 后显示全部 */
    releasesTimeline() {
      if (this.selectedReleaseId == null || this.selectedReleaseId === '') {
        return this.releases
      }
      const id = Number(this.selectedReleaseId)
      return this.releases.filter(r => r && Number(r.releaseId) === id)
    },
    latestReleaseId() {
      const latest = this.releases.find(r => r && r.draft !== 1)
      return latest ? latest.releaseId : null
    },
    fullCloneUrl() {
      const path = this.cloneUrlPath
        || (this.repoTeamKey && this.repoProjectKey && this.repoSlug
          ? `/${this.repoTeamKey}/${this.repoProjectKey}/code/${this.repoSlug}.git`
          : (this.repoProjectId && this.repoSlug ? `/git/p/${this.repoProjectId}/${this.repoSlug}.git` : ''))
      if (!path) return ''
      return window.location.origin + path
    },
    /** Current browse ref for archive download (branch / commit) */
    archiveRef() {
      return this.currentRef || this.defaultBranch || ''
    },
    /** Commit/tag ref not in branch list */
    detachedRefOption() {
      return detachedBrowseRefOption(this.currentRef, this.branches)
    },
    /** Branch select: real branch name even when tree is pinned to a commit */
    branchSelectValue() {
      if (!this.detachedRefOption) {
        return this.currentRef || undefined
      }
      return resolveBrowseBranchName([
        this.selectedBranchName,
        this.$route.query && this.$route.query.branch,
        this.defaultBranch
      ], this.branches) || undefined
    },
    gitCloneCommand() {
      return this.fullCloneUrl ? `git clone ${this.fullCloneUrl}` : ''
    },
    gitAuthorName() {
      const user = useUserStore()
      return (user.nickName || user.name || '').trim() || 'Your Name'
    },
    gitAuthorEmail() {
      const user = useUserStore()
      return (user.email || '').trim() || 'you@example.com'
    },
    gitConfigCommands() {
      return [
        `git config --global user.name '${this.gitAuthorName}'`,
        `git config --global user.email '${this.gitAuthorEmail}'`
      ].join('\n')
    },
    gitHttpsAuthExample() {
      const user = useUserStore()
      const username = (user.name || 'username').trim() || 'username'
      let protocol = window.location.protocol || 'http:'
      let host = window.location.host || 'localhost'
      try {
        const url = new URL(this.fullCloneUrl || window.location.origin)
        protocol = url.protocol
        host = url.host
      } catch (e) { /* keep defaults */ }
      const origin = `${protocol}//${host}`
      return [
        `Username for '${origin}': ${username}`,
        `Password for '${protocol}//${username}@${host}': ${this.$t('git.repo.clone-https-password-placeholder')}`
      ].join('\n')
    },
    commitsByDate() {
      const locale = (this.$i18n && this.$i18n.locale) || 'zh-CN'
      return groupCommitsByDate(this.commits, locale)
    }
  },
  watch: {
    activeRootDocTab() {
      this.scheduleRefreshRootDocToc()
    },
    activeRootDocState: {
      deep: true,
      handler() {
        this.scheduleRefreshRootDocToc()
      }
    },
    '$route.params.slug'() {
      this.bindRouteContext(true)
    },
    '$route.params.repositoryId'(id) {
      // beforeEach may attach id after slug-only navigation; reload when it arrives
      if (this.parseRepositoryId(id) && this.parseRepositoryId(id) !== this.repositoryId) {
        this.bindRouteContext(true)
      }
    },
    '$route.params.releaseId'() {
      this.syncReleaseRouteFromParams()
    },
    '$route.params.commitId'() {
      this.syncCommitRouteFromParams()
    },
    '$route.name'() {
      this.syncReleaseRouteFromParams()
      this.syncCommitRouteFromParams()
      this.syncBrowseQueryFromRoute()
    },
    '$route.query.tab'() {
      this.syncBrowseQueryFromRoute()
    },
    '$route.query.ref'(ref) {
      if (this.$route.name !== 'GitRepoBrowse') return
      if (!this.parseRepositoryId(this.repositoryId)) return
      const next = ref ? String(ref) : null
      if (!next || next === this.currentRef) return
      this.currentRef = next
      const qBranch = this.$route.query.branch ? String(this.$route.query.branch) : ''
      if (qBranch) {
        this.selectedBranchName = qBranch
      } else if (this.branches && this.branches.length) {
        this.selectedBranchName = resolveBrowseBranchName(
          [this.selectedBranchName, this.defaultBranch],
          this.branches
        )
      }
      this.onRefChange()
    }
  },
  beforeUnmount() {
    this.stopRootDocTocResize()
  },
  created() {
    this.bindRouteContext(true)
  },
  methods: {
    fileSizeUnit,
    onBlobFeatureWip() {
      this.$modal.msgWarning(this.$t('git.repo.blob.feature-wip'))
    },
    downloadBlobFile() {
      if (!this.repositoryId || !this.blob || !this.blob.path || this.blobDownloading) return
      const fileName = this.blobFileName || 'download'
      this.blobDownloading = true
      fetchGitRepoRawBlob(this.repositoryId, this.currentRef, this.blob.path).then(data => {
        if (data) {
          saveAs(data, fileName)
          return
        }
        if (this.blob.content != null && this.blob.content !== '') {
          saveAs(new Blob([this.blob.content], { type: 'text/plain;charset=utf-8' }), fileName)
          return
        }
        this.$modal.msgError(this.$t('git.repo.blob.download-failed'))
      }).catch(() => {
        this.$modal.msgError(this.$t('git.repo.blob.download-failed'))
      }).finally(() => {
        this.blobDownloading = false
      })
    },
    parseRepositoryId(value) {
      const n = Number(value)
      return Number.isFinite(n) && n > 0 ? n : null
    },
    async bindRouteContext(reload) {
      const slug = this.$route.params.slug ? String(this.$route.params.slug) : ''
      if (slug) this.repoName = slug
      this.syncReleaseRouteFromParams()
      this.syncCommitRouteFromParams()
      const qBranchEarly = this.$route.query.branch ? String(this.$route.query.branch) : ''
      if (qBranchEarly) {
        this.selectedBranchName = qBranchEarly
      }

      let nextId = this.parseRepositoryId(this.$route.params.repositoryId)
      // Same-route slug switches skip route beforeEnter; resolve from slug if needed
      if (!nextId && slug) {
        try {
          const projectId = getCurrentProjectId() || this.repoProjectId
          const repo = await resolveGitRepoByProjectAndSlug(projectId, slug)
          nextId = this.parseRepositoryId(repo && repo.repositoryId)
          if (nextId && this.$route.params) {
            this.$route.params.repositoryId = String(nextId)
          }
        } catch (e) {
          nextId = null
        }
      }

      if (!nextId) {
        this.repositoryId = null
        this.resetBrowseContent()
        return
      }
      const changed = Number(this.repositoryId) !== nextId
      this.repositoryId = nextId
      // After repositoryId is ready: apply tab/ref. Avoid onRefChange here when init() will load.
      const willInit = reload || changed
      this.syncBrowseQueryFromRoute({ skipLoad: willInit })
      if (willInit) {
        this.init()
      }
    },
    resetBrowseContent() {
      this.currentPath = ''
      this.viewMode = 'tree'
      this.currentRef = null
      this.selectedBranchName = null
      this.branches = []
      this.treeEntries = []
      this.blob = null
      this.commits = []
      this.clearRootDocs()
      this.sideTreeBootLoading = false
    },
    openBranchesPage() {
      if (!this.repositoryId) return
      this.$router.push(gitRepoBranchesRoute(this.$route)).catch(() => {})
    },
    openTagsPage() {
      if (!this.repositoryId) return
      this.$router.push(gitRepoTagsRoute(this.$route)).catch(() => {})
    },
    /** Remembered / displayed branch (never silently snap to default) */
    currentBrowseBranch() {
      return resolveBrowseBranchName([
        this.selectedBranchName,
        this.$route.query && this.$route.query.branch,
        !this.detachedRefOption ? this.currentRef : null,
        this.defaultBranch
      ], this.branches)
    },
    /** 代码页「N 次提交」→ 提交列表（顶部无提交记录 Tab） */
    openCommitsList() {
      this.selectedCommitId = null
      this.activeTab = 'commits'
      this.resetCommitFilterDraft()
      const branch = this.currentBrowseBranch()
      if (branch) {
        this.selectedBranchName = branch
        this.currentRef = branch
      }
      const query = { tab: 'commits' }
      if (branch) query.branch = branch
      this.$router.replace(gitRepoBrowseRoute(this.$route, query)).catch(() => {})
      this.loadCommitFilterContributors()
      this.loadCommits()
    },
    /** 提交详情入口（代码页最近提交 / 侧栏活动 / 提交列表） */
    openCommitDetail(commitId) {
      if (!commitId || !this.repositoryId) return
      this.selectedCommitId = commitId
      const branch = this.currentBrowseBranch()
      if (branch) {
        this.selectedBranchName = branch
      }
      const route = gitRepoCommitDetailRoute(this.$route, commitId, branch ? { branch } : undefined)
      this.$router.push(route).catch(() => {})
    },
    /**
     * Sync browse tab + optional ?ref= / ?branch= from the route.
     * @param {{ skipLoad?: boolean }} [opts] skipLoad: only update state (init/loadBranches will fetch)
     */
    syncBrowseQueryFromRoute(opts) {
      if (this.$route.name !== 'GitRepoBrowse') return
      const skipLoad = !!(opts && opts.skipLoad)
      const nextTab = resolveBrowseActiveTab(this.$route)
      const enteringCommits = nextTab === 'commits' && this.activeTab !== 'commits'
      this.activeTab = nextTab
      const qBranch = this.$route.query.branch ? String(this.$route.query.branch) : ''
      if (qBranch) {
        this.selectedBranchName = qBranch
      }
      const qRef = this.$route.query.ref ? String(this.$route.query.ref) : ''
      if (qRef && qRef !== this.currentRef) {
        this.currentRef = qRef
        if (!skipLoad && this.parseRepositoryId(this.repositoryId)) {
          this.onRefChange()
        }
      } else if (!qRef && qBranch && qBranch !== this.currentRef) {
        // History / commits list: keep API ref on the selected branch, not the default
        this.currentRef = qBranch
      }
      if (enteringCommits && !skipLoad && this.parseRepositoryId(this.repositoryId)) {
        this.resetCommitFilterDraft()
        this.loadCommitFilterContributors()
        this.loadCommits()
      }
      this.maybeOpenCreateReleaseFromQuery()
    },
    /**
     * Tags page → browse with ?createRelease=1&tagName=… opens release create dialog prefilled.
     */
    maybeOpenCreateReleaseFromQuery() {
      if (this.$route.name !== 'GitRepoBrowse') return
      const q = this.$route.query || {}
      const createFlag = q.createRelease === '1' || q.createRelease === 'true'
      const tagName = q.tagName ? String(q.tagName).trim() : ''
      if (!createFlag || !tagName) return
      if (!this.canManageGitRepo) return

      const openDialog = () => {
        this.editingReleaseId = null
        this.createReleaseForm = {
          tagName,
          title: tagName,
          body: '',
          // Must be a real branch (dropdown options), never the tag name
          targetRef: this.resolveReleaseTargetBranch(),
          draft: 0
        }
        this.createReleaseAssetList = []
        this.createReleaseVisible = true
        const nextQuery = { ...this.$route.query }
        delete nextQuery.createRelease
        delete nextQuery.tagName
        if (!nextQuery.tab) nextQuery.tab = 'releases'
        this.$router.replace(gitRepoBrowseRoute(this.$route, nextQuery)).catch(() => {})
      }

      if ((this.branches || []).length) {
        openDialog()
        return
      }
      listGitRepoBranches(this.repositoryId).then(response => {
        this.branches = response.data || []
      }).catch(() => {
        this.branches = []
      }).finally(() => {
        openDialog()
      })
    },
    /** Prefer current/default branch when it exists in branch list; else first branch. */
    resolveReleaseTargetBranch() {
      const list = this.branches || []
      const names = list.map(b => b && b.name).filter(Boolean)
      const preferred = [this.currentRef, this.defaultBranch]
        .map(v => (v == null ? '' : String(v).trim()))
        .filter(Boolean)
      for (const name of preferred) {
        if (names.includes(name)) return name
      }
      const liveDefault = list.find(b => b && b.default)
      if (liveDefault && liveDefault.name) return liveDefault.name
      return names[0] || ''
    },
    syncReleaseRouteFromParams() {
      const releaseId = this.$route.params.releaseId
      if (releaseId) {
        this.selectedReleaseId = releaseId
        this.activeTab = 'releases'
        return
      }
      if (this.$route.name === 'GitRepoBrowse') {
        this.selectedReleaseId = null
      }
    },
    syncCommitRouteFromParams() {
      const commitId = this.$route.params.commitId
      if (commitId) {
        this.selectedCommitId = commitId
        return
      }
      if (this.$route.name === 'GitRepoBrowse') {
        this.selectedCommitId = null
      }
    },
    /** Shell tab click: clear nested focus; optimistic pane switch on browse */
    onShellTabClick(name) {
      // Settings / pulls navigation is handled by GitRepoShell.onTabChange
      if (name === 'settings' || name === 'pulls') return
      if (name === 'releases') {
        this.clearReleaseFocus()
        this.selectedCommitId = null
        this.activeTab = 'releases'
        return
      }
      if (name === 'code') {
        this.selectedReleaseId = null
        this.selectedCommitId = null
        this.activeTab = 'code'
        this.commitFilter.author = ''
        this.commitFilter.dateRange = null
        // 已在浏览页时再点「代码」：清掉 tab query，并回到仓库根目录
        if (this.$route.name === 'GitRepoBrowse' && this.$route.query.tab) {
          this.$router.replace(gitRepoBrowseRoute(this.$route)).catch(() => {})
        }
        this.viewMode = 'tree'
        this.blob = null
        this.navigateToPath('')
        this.loadCommits()
      }
    },
    clearReleaseFocus() {
      this.selectedReleaseId = null
      if (this.$route.name === 'GitRepoReleaseDetail') {
        this.$router.replace(gitRepoBrowseRoute(this.$route, { tab: 'releases' })).catch(() => {})
      }
    },
    clearCommitFocus() {
      this.selectedCommitId = null
      if (this.$route.name === 'GitRepoCommitDetail') {
        const query = { tab: 'commits' }
        const branch = this.currentBrowseBranch()
        if (branch) query.branch = branch
        this.$router.replace(gitRepoBrowseRoute(this.$route, query)).catch(() => {})
        this.activeTab = 'commits'
      }
    },
    isLatestRelease(release) {
      if (!release || release.draft === 1 || this.latestReleaseId == null) return false
      return Number(release.releaseId) === Number(this.latestReleaseId)
    },
    timeValue(t) {
      if (!t) return 0
      const n = new Date(t).getTime()
      return Number.isNaN(n) ? 0 : n
    },
    relativeTime(t) {
      if (!t) return '—'
      const ms = this.timeValue(t)
      if (!ms) return parseTime(t) || '—'
      return formatTime(ms)
    },
    releaseRelativeTime(release) {
      if (!release) return '—'
      return this.relativeTime(release.publishedAt || release.createTime)
    },
    openReleaseDetail(release) {
      if (!release || !release.releaseId || !this.repositoryId) return
      this.selectedReleaseId = release.releaseId
      this.activeTab = 'releases'
      this.$router.push(gitRepoReleaseDetailRoute(this.$route, release.releaseId)).catch(() => {})
    },
    onReleaseDeleted() {
      this.selectedReleaseId = null
      this.loadReleases()
      if (this.$route.name === 'GitRepoReleaseDetail') {
        this.$router.replace(gitRepoBrowseRoute(this.$route, { tab: 'releases' })).catch(() => {})
      }
      this.activeTab = 'releases'
    },
    /** 按 git author 匹配项目成员，用于展示平台头像 */
    memberForAuthor(commit) {
      const name = (commit && commit.authorName ? String(commit.authorName) : '').trim()
      const email = (commit && commit.authorEmail ? String(commit.authorEmail) : '').trim()
      const nameKey = name.toLowerCase()
      const emailKey = email.toLowerCase()
      const matched = this.projectMembers.find(m => {
        if (!m) return false
        const userName = String(m.userName || '').trim().toLowerCase()
        const nickName = String(m.nickName || '').trim().toLowerCase()
        const mail = String(m.email || '').trim().toLowerCase()
        return (nameKey && (nameKey === userName || nameKey === nickName))
          || (emailKey && emailKey === mail)
      })
      if (matched) return matched
      const userStore = useUserStore()
      const selfName = String(userStore.name || '').trim().toLowerCase()
      if (nameKey && selfName && nameKey === selfName) {
        return {
          userName: userStore.name,
          nickName: userStore.name,
          avatar: userStore.avatar
        }
      }
      return {
        nickName: name || '?',
        userName: name || '?',
        email: email || undefined
      }
    },
    /** 贡献者头像：优先 API userId/avatar，否则与 commit 作者一致 */
    memberForContributor(contributor) {
      if (!contributor) {
        return { nickName: '?', userName: '?' }
      }
      if (contributor.userId != null) {
        const uid = Number(contributor.userId)
        const fromMembers = this.projectMembers.find(m => m && Number(m.userId) === uid)
        if (fromMembers) return fromMembers
        return {
          userId: contributor.userId,
          nickName: contributor.displayName || '?',
          userName: contributor.displayName || '?',
          avatar: contributor.avatar || undefined,
          email: contributor.email || undefined
        }
      }
      return this.memberForAuthor({
        authorName: contributor.displayName,
        authorEmail: contributor.email
      })
    },
    contributorKey(c, idx) {
      const email = (c && c.email ? String(c.email) : '').trim()
      const name = (c && c.displayName ? String(c.displayName) : '').trim()
      return email || name || `contributor-${idx}`
    },
    loadReleases() {
      if (!this.repositoryId) return
      this.releasesLoading = true
      listGitRepoReleases(this.repositoryId).then(response => {
        this.releases = response.data || []
        this.releasesRefreshToken += 1
      }).catch(() => {
        this.releases = []
      }).finally(() => {
        this.releasesLoading = false
      })
    },
    loadContributors() {
      if (!this.parseRepositoryId(this.repositoryId) || !this.currentRef) {
        this.contributors = []
        return
      }
      this.contributorsLoading = true
      listGitRepoContributors(this.repositoryId, this.currentRef, 100).then(response => {
        this.contributors = response.data || []
      }).catch(() => {
        this.contributors = []
      }).finally(() => {
        this.contributorsLoading = false
      })
    },
    openContributorsDialog() {
      this.contributorsDialogVisible = true
    },
    openCreateRelease() {
      if (!this.canManageGitRepo) return
      this.editingReleaseId = null
      this.createReleaseForm.targetRef = this.resolveReleaseTargetBranch()
      this.createReleaseAssetList = []
      this.createReleaseVisible = true
    },
    openEditRelease(release) {
      if (!this.canManageGitRepo || !release) return
      this.editingReleaseId = release.releaseId
      this.createReleaseForm = {
        tagName: release.tagName || '',
        title: release.title || '',
        body: release.body || '',
        targetRef: this.resolveReleaseTargetBranch(),
        draft: release.draft === 1 ? 1 : 0
      }
      this.createReleaseAssetList = []
      this.createReleaseVisible = true
    },
    resetCreateReleaseForm() {
      this.editingReleaseId = null
      this.createReleaseForm = {
        tagName: '',
        title: '',
        body: '',
        targetRef: this.resolveReleaseTargetBranch(),
        draft: 0
      }
      this.createReleaseAssetList = []
    },
    onCreateReleaseAssetChange(file, fileList) {
      const raw = file && file.raw
      if (raw && raw.size > CREATE_RELEASE_MAX_ASSET_BYTES) {
        this.$modal.msgWarning(this.$t('git.repo.release.form.assets-too-large', { name: file.name }))
        this.createReleaseAssetList = (fileList || []).filter(f => f.uid !== file.uid)
        return
      }
      this.createReleaseAssetList = fileList || []
    },
    onCreateReleaseAssetRemove(file, fileList) {
      this.createReleaseAssetList = fileList || []
    },
    onCreateReleaseAssetExceed() {
      this.$modal.msgWarning(this.$t('git.repo.release.form.assets-limit', { n: CREATE_RELEASE_MAX_ASSETS }))
    },
    async submitCreateRelease() {
      if (!this.canManageGitRepo) return
      const tagName = (this.createReleaseForm.tagName || '').trim()
      const title = (this.createReleaseForm.title || '').trim()
      if (!tagName || !title) {
        this.$modal.msgWarning(this.$t('git.repo.release.form.tag') + ' / ' + this.$t('git.repo.release.form.title'))
        return
      }
      if (!this.editingReleaseId && !this.createReleaseForm.targetRef) {
        this.$modal.msgWarning(this.$t('git.repo.select-branch'))
        return
      }
      this.createReleaseSubmitting = true
      try {
        if (this.editingReleaseId) {
          await updateGitRepoRelease(this.repositoryId, this.editingReleaseId, {
            title,
            body: this.createReleaseForm.body || '',
            draft: this.createReleaseForm.draft,
            prerelease: 0
          })
          this.$modal.msgSuccess(this.$t('git.repo.release.update-success'))
          this.createReleaseVisible = false
          this.loadReleases()
          return
        }
        const payload = {
          tagName,
          title,
          body: this.createReleaseForm.body || '',
          targetRef: this.createReleaseForm.targetRef,
          draft: this.createReleaseForm.draft,
          prerelease: 0
        }
        const pendingFiles = (this.createReleaseAssetList || [])
          .map(f => f.raw)
          .filter(Boolean)
        const response = await createGitRepoRelease(this.repositoryId, payload)
        const release = response.data || {}
        const releaseId = release.releaseId
        let uploadFailed = 0
        if (releaseId && pendingFiles.length) {
          for (const file of pendingFiles) {
            try {
              await uploadGitRepoReleaseAsset(this.repositoryId, releaseId, file)
            } catch (e) {
              uploadFailed += 1
            }
          }
        }
        if (uploadFailed > 0) {
          this.$modal.msgWarning(this.$t('git.repo.release.create-assets-partial', {
            ok: pendingFiles.length - uploadFailed,
            fail: uploadFailed
          }))
        } else {
          this.$modal.msgSuccess(this.$t('git.repo.release.create-success'))
        }
        this.createReleaseVisible = false
        this.loadReleases()
      } finally {
        this.createReleaseSubmitting = false
      }
    },
    loadProjectMembers(projectId) {
      if (!projectId) {
        this.projectMembers = []
        return
      }
      listMemberOfProject(projectId, { pageNum: 1, pageSize: 500 }).then(res => {
        const rows = res.rows || res.data || []
        this.projectMembers = Array.isArray(rows) ? rows : []
      }).catch(() => {
        this.projectMembers = []
      })
    },
    copyText(text) {
      if (!text) {
        this.$modal.msgError(this.$t('git.repo.copy-empty'))
        return
      }
      const onOk = () => this.$modal.msgSuccess(this.$t('git.repo.copy-success'))
      const onFail = () => this.$modal.msgError(this.$t('git.repo.copy-failed'))
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(onOk).catch(() => {
          if (this.copyTextFallback(text)) onOk()
          else onFail()
        })
        return
      }
      if (this.copyTextFallback(text)) onOk()
      else onFail()
    },
    copyTextFallback(text) {
      try {
        const input = document.createElement('textarea')
        input.value = text
        input.setAttribute('readonly', 'readonly')
        input.style.position = 'fixed'
        input.style.left = '-9999px'
        document.body.appendChild(input)
        input.select()
        const ok = document.execCommand('copy')
        document.body.removeChild(input)
        return ok
      } catch (e) {
        return false
      }
    },
    copyCloneUrl() {
      this.copyText(this.fullCloneUrl)
    },
    downloadSourceZip() {
      if (!this.repositoryId || !this.archiveRef || this.archiveDownloading) return
      const slug = this.repoSlug || this.repoName || 'source'
      const refLabel = String(this.archiveRef).replace(/[\\/]+/g, '-')
      const fileName = `${slug}-${refLabel}.zip`
      this.archiveDownloading = true
      downloadGitRepoArchive(this.repositoryId, this.archiveRef, 'zip', fileName).catch(() => {
        this.$modal.msgError(this.$t('git.repo.download-source-zip-failed'))
      }).finally(() => {
        this.archiveDownloading = false
      })
    },
    copyCurrentPath() {
      const path = this.currentBrowsePath
      if (!path) {
        this.$modal.msgError(this.$t('git.repo.copy-path-empty'))
        return
      }
      this.copyText(path)
    },
    onFileTreeAddCommand(command) {
      if (command === 'new-file') {
        this.handleNewFile()
      } else if (command === 'new-folder') {
        this.handleNewFolder()
      } else if (command === 'upload-file') {
        this.handleUploadFile()
      }
    },
    handleNewFile() {
      // 在线写仓 API 尚未就绪，先保留入口
      this.$modal.msgWarning(this.$t('git.repo.write-coming-soon'))
    },
    handleNewFolder() {
      this.$modal.msgWarning(this.$t('git.repo.write-coming-soon'))
    },
    handleUploadFile() {
      this.$modal.msgWarning(this.$t('git.repo.write-coming-soon'))
    },
    init() {
      if (!this.repositoryId) {
        this.pageLoading = false
        return
      }
      const seq = ++this.loadSeq
      this.resetBrowseContent()
      this.activeTab = resolveBrowseActiveTab(this.$route)
      this.pageLoading = true
      getGitRepo(this.repositoryId).then(response => {
        if (seq !== this.loadSeq) return
        const repo = response.data || {}
        this.repoName = repo.name || this.repoName
        this.repoRemark = repo.remark || ''
        this.repoVisibility = repo.visibility === 'public' ? 'public' : 'project'
        this.defaultBranch = repo.defaultBranch || ''
        this.repoSlug = repo.slug || ''
        this.repoProjectId = repo.projectId
        this.repoTeamKey = repo.teamKey || ''
        this.repoProjectKey = repo.projectKey || ''
        this.cloneUrlPath = repo.cloneUrl || ''
        this.loadProjectMembers(repo.projectId)
        this.loadReleases()
      }).catch(() => {}).finally(() => {
        if (seq === this.loadSeq) {
          this.pageLoading = false
        }
      })
      this.loadBranches(seq)
    },
    loadBranches(seq) {
      if (!this.repositoryId) {
        this.branches = []
        this.branchesLoading = false
        return
      }
      const loadSeq = seq != null ? seq : this.loadSeq
      this.branchesLoading = true
      listGitRepoBranches(this.repositoryId).then(response => {
        if (loadSeq !== this.loadSeq) return
        this.branches = response.data || []
        this.branchesLoading = false
        const applied = applyBrowseRefState(
          this.$route.query,
          this.branches,
          this.selectedBranchName || this.defaultBranch
        )
        this.currentRef = applied.currentRef
        this.selectedBranchName = applied.selectedBranchName
        this.loadTree('')
        this.loadCommits()
        this.loadContributors()
        if (this.activeTab === 'commits') {
          this.loadCommitFilterContributors()
        }
      }).catch(() => {
        if (loadSeq !== this.loadSeq) return
        this.branchesLoading = false
        this.branches = []
        this.currentRef = null
        this.selectedBranchName = null
        this.treeEntries = []
        this.clearRootDocs()
      })
    },
    /** From commit detail: browse tree at commit, keep the branch that was selected */
    browseFilesAtCommit(commitId) {
      const ref = commitId || (this.$route.params && this.$route.params.commitId)
      if (!ref) return
      const branch = this.currentBrowseBranch()
      const query = { ref: String(ref), tab: 'code' }
      if (branch) {
        query.branch = branch
        this.selectedBranchName = branch
      }
      this.selectedCommitId = null
      this.$router.push(gitRepoBrowseRoute(this.$route, query)).catch(() => {})
    },
    onBranchSelect(name) {
      const next = name != null && String(name).trim() !== '' ? String(name) : null
      if (!next) return
      this.selectedBranchName = next
      if (next === this.currentRef && !this.$route.query.ref) return
      this.currentRef = next
      this.onRefChange()
      // Leave tip-of-branch browse without sticky ?ref=; keep ?branch= for history continuity
      const q = { ...(this.$route.query || {}) }
      delete q.ref
      if (this.activeTab === 'commits' || q.tab === 'commits') {
        q.tab = 'commits'
        q.branch = next
      } else {
        delete q.branch
        delete q.tab
      }
      this.$router.replace({
        name: 'GitRepoBrowse',
        params: { ...this.$route.params },
        query: q
      }).catch(() => {})
    },
    /** @deprecated kept for call sites that only need to drop commit pin */
    clearBrowseQueryRef() {
      const q = this.$route.query || {}
      if (q.ref == null || q.ref === '') return
      const query = { ...q }
      delete query.ref
      this.$router.replace({
        name: 'GitRepoBrowse',
        params: { ...this.$route.params },
        query
      }).catch(() => {})
    },
    onRefChange() {
      if (!this.parseRepositoryId(this.repositoryId) || !this.currentRef) {
        return
      }
      this.currentPath = ''
      this.viewMode = 'tree'
      this.commitFilter.author = ''
      this.commitFilter.dateRange = null
      this.loadTree('')
      this.loadCommits()
      this.loadContributors()
      if (this.activeTab === 'commits') {
        this.loadCommitFilterContributors()
      }
    },
    breadcrumbPathAt(idx) {
      return this.breadcrumbSegments.slice(0, idx + 1).join('/')
    },
    navigateToPath(path) {
      this.viewMode = 'tree'
      this.loadTree(path)
    },
    handleTreeRowClick(row) {
      if (row.type === 'tree') {
        this.loadTree(row.path)
      } else {
        this.loadBlob(row.path)
      }
    },
    mapSideTreeEntries(entries) {
      return (entries || []).map(row => ({
        name: row.name,
        path: row.path || row.name,
        type: row.type,
        isLeaf: row.type !== 'tree'
      }))
    },
    loadSideTreeNode(node, resolve) {
      if (!this.parseRepositoryId(this.repositoryId) || !this.currentRef) {
        resolve([])
        return
      }
      const path = node.level === 0 ? '' : (node.data && node.data.path) || ''
      if (node.level === 0) {
        this.sideTreeBootLoading = true
      }
      listGitRepoTree(this.repositoryId, this.currentRef, path).then(response => {
        resolve(this.mapSideTreeEntries(response.data || []))
        this.$nextTick(() => this.syncSideTreeHighlight())
      }).catch(() => {
        resolve([])
      }).finally(() => {
        if (node.level === 0) {
          this.sideTreeBootLoading = false
        }
      })
    },
    onSideTreeNodeClick(data, node) {
      if (!data || !data.path) return
      if (data.type === 'tree') {
        if (node && !node.expanded && typeof node.expand === 'function') {
          node.expand()
        }
        this.loadTree(data.path)
      } else {
        this.loadBlob(data.path)
      }
    },
    isSideTreeNodeActive(data) {
      if (!data || !data.path) return false
      return data.path === this.sideTreeActivePath
    },
    syncSideTreeHighlight() {
      const tree = this.$refs.sideFileTree
      if (!tree || typeof tree.setCurrentKey !== 'function') return
      const key = this.sideTreeActivePath || null
      tree.setCurrentKey(key)
      if (!key) return
      // 尽量展开祖先目录，便于在树中看到当前项
      const parts = String(key).split('/').filter(Boolean)
      let acc = ''
      parts.forEach((seg, idx) => {
        acc = idx === 0 ? seg : `${acc}/${seg}`
        const isLast = idx === parts.length - 1
        if (isLast && this.viewMode === 'blob') return
        const node = typeof tree.getNode === 'function' ? tree.getNode(acc) : null
        if (node && node.data && node.data.type === 'tree' && !node.expanded && typeof node.expand === 'function') {
          node.expand()
        }
      })
    },
    loadTree(path) {
      if (!this.parseRepositoryId(this.repositoryId) || !this.currentRef) {
        this.treeEntries = []
        this.clearRootDocs()
        return
      }
      this.treeLoading = true
      this.viewMode = 'tree'
      this.currentPath = path || ''
      listGitRepoTree(this.repositoryId, this.currentRef, this.currentPath).then(response => {
        this.treeEntries = response.data || []
        this.treeLoading = false
        this.syncRootDocsFromTree()
        this.$nextTick(() => this.syncSideTreeHighlight())
      }).catch(() => {
        this.treeEntries = []
        this.treeLoading = false
        this.clearRootDocs()
      })
    },
    clearRootDocs() {
      this.rootDocTabs = []
      this.activeRootDocTab = ''
      this.rootDocTabUserSelected = false
      this.rootDocCache = {}
      this.rootDocToc = []
    },
    findTreeFile(names) {
      return (this.treeEntries || []).find(row => {
        if (!row || row.type === 'tree') return false
        const name = String(row.name || '').toLowerCase()
        return names.has(name)
      }) || null
    },
    /** tab 文案去掉 .md / .markdown / .txt 扩展名 */
    rootDocTabLabel(fileName) {
      return String(fileName || '').replace(/\.(md|markdown|txt)$/i, '')
    },
    onRootDocTabClick() {
      this.rootDocTabUserSelected = true
    },
    preferredRootDocTabKey(tabs) {
      const list = tabs || []
      const readme = list.find(t => t && t.key === 'readme')
      return readme ? readme.key : (list[0] && list[0].key) || ''
    },
    syncRootDocsFromTree() {
      if (this.currentPath || this.viewMode !== 'tree') {
        this.rootDocTabs = []
        this.activeRootDocTab = ''
        this.rootDocTabUserSelected = false
        return
      }
      const tabs = []
      const readme = this.findTreeFile(README_NAMES)
      if (readme) {
        tabs.push({
          key: 'readme',
          name: readme.name,
          label: this.rootDocTabLabel(readme.name),
          path: readme.path || readme.name
        })
      }
      const license = this.findTreeFile(LICENSE_NAMES)
      if (license) {
        tabs.push({
          key: 'license',
          name: license.name,
          label: this.rootDocTabLabel(license.name),
          path: license.path || license.name
        })
      }
      this.rootDocTabs = tabs
      this.rootDocTabUserSelected = false
      // Always default to README when present (avoid LICENSE winning the load race)
      this.activeRootDocTab = this.preferredRootDocTabKey(tabs)
      // 切换分支时清空旧内容缓存，按新 ref 重拉
      this.rootDocCache = {}
      tabs.forEach(tab => this.loadRootDocContent(tab.path))
    },
    rootDocState(path) {
      return this.rootDocCache[path] || { content: '', loading: true, binary: false }
    },
    isRootDocContentVisible(state) {
      if (!state || state.binary) return false
      return !!(String(state.content || '').trim())
    },
    ensureActiveRootDocTab() {
      const visible = this.visibleRootDocTabs
      if (!visible.length) {
        this.activeRootDocTab = ''
        this.rootDocToc = []
        return
      }
      // Keep manual selection only after user clicked a tab
      if (this.rootDocTabUserSelected && visible.some(t => t.key === this.activeRootDocTab)) {
        return
      }
      this.activeRootDocTab = this.preferredRootDocTabKey(visible)
    },
    loadRootDocContent(path) {
      if (!path || !this.currentRef) return
      this.rootDocCache = {
        ...this.rootDocCache,
        [path]: { content: '', loading: true, binary: false }
      }
      getGitRepoBlob(this.repositoryId, this.currentRef, path).then(response => {
        const data = response.data || {}
        this.rootDocCache = {
          ...this.rootDocCache,
          [path]: {
            content: data.binary ? '' : (data.content || ''),
            loading: false,
            binary: !!data.binary
          }
        }
        this.ensureActiveRootDocTab()
      }).catch(() => {
        this.rootDocCache = {
          ...this.rootDocCache,
          [path]: { content: '', loading: false, binary: false }
        }
        this.ensureActiveRootDocTab()
      })
    },
    scheduleRefreshRootDocToc() {
      this.$nextTick(() => {
        // markdown-it 渲染后再扫一轮，避免拿不到 h1-h6
        window.setTimeout(() => this.refreshRootDocToc(), 0)
      })
    },
    refreshRootDocToc() {
      const root = this.$refs.rootDocMarkdown
      if (!root || !this.activeRootDocState || !this.activeRootDocState.content) {
        this.rootDocToc = []
        return
      }
      const headings = extractHeadings(root)
        .map(({ id, text, level }) => ({ id, text, level }))
        .filter(h => h.text)
      this.rootDocToc = headings
    },
    scrollToRootDocHeading(item) {
      if (!item || !item.id) return
      const root = this.$refs.rootDocMarkdown
      const target = root && root.querySelector(`#${CSS.escape(item.id)}`)
      if (!target) return
      target.scrollIntoView({ behavior: 'smooth', block: 'start' })
    },
    startRootDocTocResize(event) {
      this.stopRootDocTocResize()
      const startX = event.pageX
      const startWidth = this.rootDocTocWidth
      this.rootDocTocResizing = true
      this._rootDocTocOnMove = (ev) => {
        this.rootDocTocWidth = clampRootDocTocWidth(startWidth + (ev.pageX - startX))
      }
      this._rootDocTocOnUp = () => this.stopRootDocTocResize()
      window.addEventListener('mousemove', this._rootDocTocOnMove)
      window.addEventListener('mouseup', this._rootDocTocOnUp)
      document.body.style.cursor = 'col-resize'
      document.body.style.userSelect = 'none'
    },
    stopRootDocTocResize() {
      if (this._rootDocTocOnMove) {
        window.removeEventListener('mousemove', this._rootDocTocOnMove)
        this._rootDocTocOnMove = null
      }
      if (this._rootDocTocOnUp) {
        window.removeEventListener('mouseup', this._rootDocTocOnUp)
        this._rootDocTocOnUp = null
      }
      this.rootDocTocResizing = false
      document.body.style.cursor = ''
      document.body.style.userSelect = ''
    },
    loadBlob(path) {
      this.blobLoading = true
      this.viewMode = 'blob'
      // 进入文件时保留目录路径，便于面包屑回退；左侧树高亮用 blob.path
      const slash = String(path || '').lastIndexOf('/')
      this.currentPath = slash > 0 ? path.slice(0, slash) : ''
      getGitRepoBlob(this.repositoryId, this.currentRef, path).then(response => {
        this.blob = response.data || null
        this.blobLoading = false
        this.$nextTick(() => this.syncSideTreeHighlight())
      }).catch(() => {
        this.blob = null
        this.blobLoading = false
      })
    },
    /** README / 根文档 Markdown 内相对链接：优先按文件打开，失败则按目录打开 */
    openRepoPathFromMarkdown(path) {
      const target = String(path || '').replace(/^\/+|\/+$/g, '')
      if (!target || !this.currentRef) return
      this.selectedCommitId = null
      this.selectedReleaseId = null
      this.activeTab = 'code'
      if (target.endsWith('/')) {
        this.navigateToPath(target.replace(/\/+$/g, ''))
        return
      }
      this.blobLoading = true
      this.viewMode = 'blob'
      const slash = target.lastIndexOf('/')
      this.currentPath = slash > 0 ? target.slice(0, slash) : ''
      getGitRepoBlob(this.repositoryId, this.currentRef, target).then(response => {
        this.blob = response.data || null
        this.blobLoading = false
        this.$nextTick(() => this.syncSideTreeHighlight())
      }).catch(() => {
        this.blob = null
        this.blobLoading = false
        this.navigateToPath(target)
      })
    },
    loadCommits() {
      if (!this.parseRepositoryId(this.repositoryId) || !this.currentRef) {
        this.commits = []
        this.commitTotal = 0
        return
      }
      this.commitsLoading = true
      const params = {
        ref: this.currentRef,
        limit: 50
      }
      if (this.activeTab === 'commits') {
        const author = (this.commitFilter.author || '').trim()
        if (author) params.author = author
        const range = this.commitFilter.dateRange
        if (Array.isArray(range) && range.length === 2) {
          if (range[0]) params.since = range[0]
          if (range[1]) params.until = range[1]
        }
      }
      this.loadCommitTotal()
      listGitRepoCommits(this.repositoryId, params).then(response => {
        this.commits = response.data || []
        this.commitsLoading = false
      }).catch(() => {
        this.commits = []
        this.commitsLoading = false
      })
    },
    loadCommitTotal() {
      if (!this.parseRepositoryId(this.repositoryId) || !this.currentRef) {
        this.commitTotal = 0
        return
      }
      const ref = this.currentRef
      getGitRepoCommitCount(this.repositoryId, ref).then(response => {
        if (this.currentRef !== ref) return
        const n = Number(response && response.data)
        this.commitTotal = Number.isFinite(n) && n >= 0 ? n : 0
      }).catch(() => {
        if (this.currentRef !== ref) return
        this.commitTotal = 0
      })
    },
    resetCommitFilterDraft() {
      this.commitFilter.author = ''
      this.commitFilter.dateRange = null
    },
    contributorFilterValue(c) {
      if (!c) return ''
      const email = (c.email || '').trim()
      if (email) return email
      return (c.displayName || '').trim()
    },
    onCommitFilterRefChange() {
      const branch = this.currentRef
      if (branch) {
        this.selectedBranchName = branch
      }
      this.commitFilter.author = ''
      const query = { ...this.$route.query, tab: 'commits' }
      if (branch) query.branch = branch
      else delete query.branch
      delete query.ref
      this.$router.replace(gitRepoBrowseRoute(this.$route, query)).catch(() => {})
      this.loadCommitFilterContributors()
      this.loadCommits()
    },
    onCommitFilterChange() {
      this.loadCommits()
    },
    loadCommitFilterContributors() {
      if (!this.parseRepositoryId(this.repositoryId) || !this.currentRef) {
        this.commitFilterContributors = []
        return
      }
      this.commitFilterContributorsLoading = true
      listGitRepoContributors(this.repositoryId, this.currentRef, 100).then(response => {
        this.commitFilterContributors = response.data || []
      }).catch(() => {
        this.commitFilterContributors = []
      }).finally(() => {
        this.commitFilterContributorsLoading = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
/* 使用 theme-dark.scss 全局语义色：--text-color-* / --card-bg / --border-color-* */
.git-browse-page {
  /* 与缺陷页顶左右一致；底部留少量间距，避免内容贴底 */
  padding: 20px 20px 16px;
  box-sizing: border-box;
  color: var(--text-color-regular);
  /* 承接 AppMain 首子 flex + min-height:0；三栏在剩余高度内置底 */
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  overflow: hidden;

  /*
   * 提交列表/详情、代码概览：滚动条贴主内容区最右侧。
   * 去掉页级右侧 padding，改由顶栏/Tab 与滚动容器自己留白。
   */
  &:has(.git-browse-commits-pane),
  &:has(.git-browse-layout) {
    padding-right: 0;

    :deep(.git-repo-shell__top),
    :deep(.git-repo-shell__tabs) {
      padding-right: 20px;
      box-sizing: border-box;
    }
  }
}

.git-browse-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 16px;
  /* 根目录：两列随内容增高，由 layout 统一纵向滚动 */
  align-items: start;
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  overflow-x: hidden;
  overflow-y: auto;
  padding-right: 20px;
  box-sizing: border-box;

  &.has-side-tree {
    /* 左树 + 内容：列等高，各自内部滚动（右侧简介栏已隐藏） */
    grid-template-columns: 280px minmax(0, 1fr);
    align-items: stretch;
    overflow: hidden;

    .git-browse-main {
      height: 100%;
      overflow-x: hidden;
      overflow-y: auto;
    }

    .git-browse-main--blob {
      overflow: hidden;
    }
  }
}

.git-browse-file-tree {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  background: var(--card-bg);
  overflow: hidden;
  min-width: 0;
  padding: 0 15px;
  box-sizing: border-box;
}

.git-browse-file-tree__head {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 0 0;
  border-bottom: none;
  background: transparent;
}

.git-browse-file-tree__add {
  flex: 0 0 auto;
  padding: 2px;
  color: var(--text-color-secondary);

  &:hover {
    color: var(--el-color-primary);
  }
}

.git-browse-file-tree__branch {
  flex: 0 0 auto;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-width: 0;
  padding: 8px 0;
  border-bottom: none;
  background: transparent;
  box-sizing: border-box;
}

.git-browse-file-tree__title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.git-browse-file-tree__title-icon {
  flex-shrink: 0;
  font-size: 15px;
  color: var(--text-color-secondary);
}

.git-browse-file-tree__body {
  flex: 1 1 auto;
  min-height: 120px;
  overflow: auto;
  padding: 6px 0 10px;
  background: var(--card-bg);
}

.git-browse-file-tree__empty {
  padding: 16px 12px;
  font-size: 12px;
  color: var(--text-color-placeholder);
}

/* 覆盖全局/暗色主题 .el-tree 的 --bg-color-base，与外层卡片同色 */
.git-browse-file-tree .git-browse-file-tree__el.el-tree,
.git-browse-file-tree .git-browse-file-tree__el {
  background: var(--card-bg) !important;
  background-color: var(--card-bg) !important;
  color: var(--text-color-regular);

  :deep(.el-tree-node__content) {
    height: 28px;
    border-radius: 4px;
    background: transparent;
  }

  :deep(.el-tree-node__content:hover) {
    background: var(--table-row-hover-bg);
  }

  :deep(.el-tree-node.is-current > .el-tree-node__content) {
    background: var(--table-row-hover-bg);
  }

  :deep(.el-tree-node__expand-icon) {
    color: var(--text-color-secondary);
  }
}

.git-browse-file-tree__node {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  font-size: 13px;
  color: var(--text-color-primary);
  font-weight: 500;
}

.git-browse-file-tree__icon {
  flex: 0 0 auto;
  flex-shrink: 0;
  font-size: 14px;
  color: var(--text-color-secondary);

  &.is-dir {
    color: var(--text-color-regular);
  }
}

.git-browse-file-tree__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .git-browse-layout {
    grid-template-columns: minmax(0, 1fr);

    &.has-side-tree {
      grid-template-columns: 240px minmax(0, 1fr);
    }
  }

  .git-browse-aside {
    display: none;
  }

  .git-browse-layout {
    overflow: auto;
  }
}

@media (max-width: 768px) {
  .git-browse-layout,
  .git-browse-layout.has-side-tree {
    grid-template-columns: minmax(0, 1fr);
  }

  .git-browse-file-tree {
    display: none;
  }
}

.git-browse-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  height: auto;
  overflow: visible;
}

.git-browse-main--blob {
  overflow: hidden;
  min-height: 0;
}

.git-browse-commits-pane {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  /* 内容右内边距；滚动条在 pane 外缘（即主内容区最右） */
  padding-right: 20px;
  box-sizing: border-box;
}

.git-browse-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cat2bug-toolbar-section-gap, 10px);
  margin-bottom: 12px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.git-browse-toolbar-left,
.git-browse-toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--cat2bug-toolbar-item-gap, 10px);
  flex-wrap: wrap;

  /* EP/Element 默认 .el-button+.el-button margin 会叠在 flex gap 上，导致两按钮间距偏大 */
  .el-button + .el-button {
    margin-left: 0;
  }
}

.git-browse-branch-select {
  width: 220px;
  min-width: 180px;

  /* 侧栏整行：标准高度，单行水平对齐（勿改 EP selected-item 的 absolute 定位） */
  &.git-browse-branch-select--side {
    flex: 1 1 auto;
    width: 100%;
    min-width: 0;
    max-width: 100%;

    :deep(.el-select__wrapper) {
      min-width: 0;
      width: 100%;
      height: var(--cat2bug-toolbar-control-height, 32px) !important;
      min-height: var(--cat2bug-toolbar-control-height, 32px) !important;
      max-height: var(--cat2bug-toolbar-control-height, 32px) !important;
      padding: 0 8px !important;
      box-sizing: border-box;
      align-items: center;
    }

    :deep(.el-select__prefix) {
      display: inline-flex !important;
      flex: 0 0 auto;
      align-items: center;
      justify-content: center;
      height: 100%;
      margin-right: 6px;
      color: var(--text-color-secondary);
    }

    :deep(.el-select__suffix) {
      display: inline-flex !important;
      flex: 0 0 auto;
      align-items: center;
      height: 100%;
    }

    :deep(.el-select__prefix .el-icon),
    :deep(.el-select__prefix .git-browse-branch-icon) {
      display: inline-flex !important;
      align-items: center;
      justify-content: center;
      width: 14px;
      height: 14px;
      font-size: 14px;
      line-height: 1;
    }

    :deep(.el-select__caret),
    :deep(.el-select__icon) {
      display: inline-flex !important;
      color: var(--text-color-secondary);
    }

    :deep(.el-select__selection) {
      flex: 1 1 auto !important;
      min-width: 0 !important;
      height: 100%;
      position: relative;
      overflow: hidden;
    }

    :deep(.el-select__selected-item),
    :deep(.el-select__placeholder) {
      color: var(--text-color-regular) !important;
      line-height: var(--cat2bug-toolbar-control-height, 32px);
    }

    :deep(.el-select__placeholder.is-transparent) {
      color: var(--text-color-placeholder) !important;
    }
  }
}

.git-browse-branch-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.git-browse-branch-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1em;
  height: 1em;
  font-size: 14px;
  color: var(--text-color-secondary);

  svg {
    width: 1em;
    height: 1em;
  }
}

.git-browse-meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  height: var(--cat2bug-toolbar-control-height, 32px);
  padding: 0 12px;
  font-size: 13px;
  color: var(--text-color-secondary);

  &:hover {
    color: var(--el-color-primary);
    border-color: var(--el-color-primary);
  }

  /* 外描边会被 browse 页 overflow:hidden 裁掉上边；改用 inset */
  &:focus,
  &:focus-visible {
    outline: none !important;
    border-color: var(--border-color-base, #dcdfe6) !important;
    box-shadow: 0 0 0 1px var(--cat2bug-field-focus-color, var(--el-color-primary, #1890ff)) inset !important;
  }

  /* 自定义图标（非 el-icon 包裹）也走统一间距，避免与 EP margin 叠加倍增 */
  .git-browse-meta-chip-icon,
  > .el-icon {
    margin-right: 0 !important;
    margin-left: 0 !important;
  }

  [class*=el-icon] + span {
    margin-left: 0 !important;
  }
}

.git-browse-meta-chip-icon {
  font-size: 14px;
}

.git-browse-clone-btn {
  font-weight: 500;
}

.git-browse-toolbar-add {
  padding: 5px 8px;
  color: var(--text-color-regular);

  &:hover {
    color: var(--el-color-primary);
    border-color: var(--el-color-primary);
  }
}

.git-browse-clone-panel {
  padding: 14px 16px 16px;
  width: 520px;
  max-width: 90vw;
  max-height: min(70vh, 640px);
  overflow: auto;
  background: var(--dropdown-bg);
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  box-shadow: var(--el-box-shadow-light);
  color: var(--text-color-regular);
}

.git-browse-clone-proto-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.git-browse-clone-proto {
  display: inline-block;
  padding-bottom: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-color-primary);
  border-bottom: 2px solid var(--el-color-primary);
}

.git-browse-clone-zip {
  flex-shrink: 0;
  padding: 0 !important;
  height: auto !important;
  min-height: 0 !important;
  font-size: 13px;
}

.git-browse-clone-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 14px;

  .git-browse-clone-url {
    flex: 1 1 auto;
    min-width: 0;

    :deep(.el-input__wrapper) {
      height: var(--cat2bug-control-height, 32px) !important;
      min-height: var(--cat2bug-control-height, 32px) !important;
      box-sizing: border-box;
    }
  }

  .git-browse-clone-copy {
    flex-shrink: 0;
    width: var(--cat2bug-control-height, 32px) !important;
    height: var(--cat2bug-control-height, 32px) !important;
    min-height: var(--cat2bug-control-height, 32px) !important;
    padding: 0 !important;
    box-sizing: border-box;
  }
}

.git-browse-clone-tips-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 4px 0 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-color-primary);

  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: var(--border-color-light);
  }
}

.git-browse-clone-tips-desc {
  margin: 0 0 8px;
  font-size: 12px;
  line-height: 1.55;
  color: var(--text-color-secondary);
}

.git-browse-clone-code {
  position: relative;
  margin: 0 0 14px;
  padding: 10px 36px 10px 12px;
  border-radius: 4px;
  background: var(--bg-color-block, rgba(127, 127, 127, 0.12));
  border: 1px solid var(--border-color-light);

  pre {
    margin: 0;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    font-size: 12px;
    line-height: 1.55;
    white-space: pre-wrap;
    word-break: break-all;
    color: var(--text-color-regular);
  }

  .el-button {
    position: absolute;
    top: 6px;
    right: 4px;
  }
}

.git-browse-clone-code--example {
  margin-bottom: 0;
  padding-right: 12px;
}

.git-browse-commit-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  margin-bottom: 10px;
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  background: var(--card-bg);
  flex-wrap: wrap;
  flex-shrink: 0;
}

.git-browse-commit-bar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
  flex-wrap: wrap;
}

.git-browse-author {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-color-primary);
}

.git-browse-commit-bar-left :deep(.member-avatar),
.git-aside-activity-item :deep(.member-avatar),
.git-browse-commit-item :deep(.member-avatar) {
  flex-shrink: 0;
}

.git-browse-commit-msg {
  flex: 1;
  min-width: 80px;
  font-size: 13px;
  color: var(--text-color-regular);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &.is-link {
    color: var(--text-color-regular);
    text-decoration: none;
    cursor: pointer;

    &:hover {
      color: var(--el-color-primary);
      text-decoration: underline;
    }
  }
}

.git-browse-commit-hash {
  font-family: Menlo, Consolas, monospace;
  font-size: 12px;
  color: var(--el-color-primary);

  &.is-link {
    text-decoration: none;
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }
  }
}

.git-browse-commit-time {
  font-size: 12px;
  color: var(--text-color-secondary);
}

.git-browse-commit-total {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--el-color-primary);
  text-decoration: none;
  white-space: nowrap;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

.git-browse-breadcrumb {
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--text-color-secondary);

  a {
    color: var(--el-color-primary);
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  &.git-browse-breadcrumb--toolbar {
    margin-bottom: 0;
    min-width: 0;
    flex: 1 1 auto;
    display: inline-flex;
    align-items: center;
    gap: 4px;

    :deep(.el-breadcrumb) {
      font-size: 13px;
      line-height: 1.4;
      flex-wrap: wrap;
      min-width: 0;
    }
  }
}

.git-browse-path-copy {
  flex: 0 0 auto;
  padding: 2px;
  margin-left: 2px;
  color: var(--text-color-secondary);

  &:hover {
    color: var(--el-color-primary);
  }
}

.git-browse-tree-card {
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  overflow: hidden;
  background: var(--card-bg);
  min-height: 120px;
  flex: 0 0 auto;
  color: var(--text-color-regular);
}

.git-browse-blob-card {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  overflow: hidden;
  background: var(--card-bg);
  min-height: 0;
  flex: 1 1 auto;
  color: var(--text-color-regular);
}

.git-browse-blob-header {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--border-color-light);
  background: var(--bg-color-block, rgba(127, 127, 127, 0.06));
  flex-wrap: wrap;
}

.git-browse-blob-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1 1 auto;
}

.git-browse-blob-file-icon {
  flex: 0 0 auto;
  font-size: 16px;
  color: var(--text-color-secondary);
}

.git-browse-blob-file-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.git-browse-blob-file-size {
  flex: 0 0 auto;
  font-size: 12px;
  color: var(--text-color-secondary);
}

.git-browse-blob-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  flex: 0 0 auto;

  :deep(.el-button) {
    margin: 0 !important;
  }
}

.git-browse-blob-commit {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px 14px;
  font-size: 12px;
  color: var(--text-color-secondary);
  border-bottom: 1px solid var(--border-color-light);
}

.git-browse-blob-commit-text {
  min-width: 0;
}

.git-browse-blob-commit-sep {
  color: var(--text-color-placeholder);
}

.git-browse-blob-commit-link {
  flex: 0 0 auto;
  font-size: 12px;
  color: var(--el-color-primary);
  text-decoration: none;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

.git-browse-blob-body {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.git-browse-blob-code {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  align-items: stretch;
}

.git-browse-blob-gutter {
  flex: 0 0 auto;
  border-right: 1px solid var(--border-color-light);
  background: var(--bg-color-block, rgba(127, 127, 127, 0.04));
  user-select: none;
}

.git-browse-blob-ln-pre {
  margin: 0;
  padding: 14px 12px 14px 14px;
  text-align: right;
  font-family: Menlo, Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-color-secondary);
  white-space: pre;
}

.git-browse-blob-code-pre {
  flex: 1 1 auto;
  min-width: 0;
  margin: 0;
  padding: 14px 16px;
  max-height: none;
  overflow-x: auto;
  overflow-y: visible;
  border-radius: 0;
  background: var(--input-bg);
  white-space: pre;

  code {
    display: block;
  }
}

.git-browse-root-docs {
  margin-top: 16px;
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  background: var(--card-bg);
  overflow: visible;
  min-height: 80px;
}

.git-browse-root-docs-tabs {
  :deep(.el-tabs__header) {
    margin: 0;
    padding: 0 12px;
    background: var(--bg-color-block, rgba(127, 127, 127, 0.06));
    border-bottom: 1px solid var(--border-color-light);
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  :deep(.el-tabs__item) {
    height: 40px;
    line-height: 40px;
    font-weight: 600;
  }

  :deep(.el-tabs__content) {
    display: none;
  }
}

.git-browse-root-doc-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.git-browse-root-doc-tab-icon {
  font-size: 14px;
}

.git-browse-root-docs-panel {
  min-height: 80px;
}

.git-browse-root-docs-content {
  display: flex;
  align-items: stretch;
  min-height: 120px;
}

.git-browse-doc-toc {
  flex: 0 0 auto;
  max-height: min(70vh, 720px);
  overflow: auto;
  border-right: none;
  /* 覆盖全局 aside { background:#eef1f6; padding… }，与右侧正文同底 */
  margin: 0;
  padding: 0 8px 0 12px;
  border-radius: 0;
  line-height: inherit;
  font-size: inherit;
  font-family: inherit;
  color: var(--text-color-regular);
  background: var(--card-bg);
}

.git-browse-doc-toc__title {
  padding: 12px 6px 4px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-color-primary);
  position: sticky;
  top: 0;
  background: inherit;
  z-index: 1;
}

.git-browse-doc-toc__list {
  list-style: none;
  margin: 0;
  padding: 0 0 12px;
}

.git-browse-doc-toc__item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 3px 4px;
  margin: 0 2px;
  line-height: 1.4;
  color: var(--text-color-regular);
  border-radius: 4px;
  cursor: pointer;

  &:hover {
    color: var(--el-color-primary);
    background: rgba(255, 193, 7, 0.08);

    .git-browse-doc-toc__dot {
      background: var(--el-color-primary);
    }
  }

  &.is-level-1 {
    font-size: 14px;
    font-weight: 600;
  }

  &.is-level-2 {
    font-size: 13px;
    font-weight: 500;
  }

  &.is-level-3 {
    font-size: 12px;
    font-weight: 400;
  }

  &.is-level-4 {
    font-size: 12px;
    font-weight: 400;
    color: var(--text-color-secondary);
  }

  &.is-level-5,
  &.is-level-6 {
    font-size: 11px;
    font-weight: 400;
    color: var(--text-color-secondary);
  }
}

.git-browse-doc-toc__dot {
  flex: 0 0 auto;
  width: 5px;
  height: 5px;
  margin-top: 0.45em;
  border-radius: 50%;
  background: var(--text-color-secondary);
}

.git-browse-doc-toc__text {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.git-browse-doc-toc-resizer {
  flex: 0 0 2px;
  width: 2px;
  cursor: col-resize;
  background: transparent;
  position: relative;
  z-index: 2;

  &::after {
    content: '';
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    width: 1px;
    background: var(--border-color-light);
  }

  &:hover::after,
  &:active::after {
    background: var(--el-color-primary);
    width: 2px;
    left: 0;
  }
}

.git-browse-root-docs-body {
  flex: 1 1 auto;
  min-width: 0;
  max-width: 100%;
  overflow-x: auto;
  padding: 16px 20px 20px;
  color: var(--text-color-regular);
  font-size: 14px;
  line-height: 1.6;

  :deep(.markdown-body),
  :deep(.markdown-it-vue) {
    background: transparent;
    color: inherit;
  }

  :deep(img) {
    background-color: var(--markdown-img-bg, transparent);
    color: var(--markdown-img-alt-color, var(--text-color-secondary));
  }

  :deep(a) {
    color: var(--el-color-primary);
  }

  :deep(pre),
  :deep(code) {
    background: var(--bg-color-block, rgba(127, 127, 127, 0.12));
    border-radius: 4px;
  }

  :deep(pre) {
    padding: 12px;
    overflow-x: auto;
  }

  :deep(code) {
    padding: 0.1em 0.35em;
    font-size: 0.9em;
  }

  :deep(pre code) {
    padding: 0;
    background: transparent;
  }

  :deep(table) {
    border-collapse: collapse;
    width: 100%;
    margin: 12px 0;
    background: transparent;
  }

  :deep(th),
  :deep(td) {
    border: 1px solid var(--table-border-color, var(--border-color-light));
    padding: 6px 10px;
    color: var(--text-color-regular);
  }

  :deep(th) {
    background-color: var(--table-header-bg, var(--bg-color-block));
    color: var(--table-header-color, var(--text-color-primary));
    font-weight: 600;
  }

  :deep(tr) {
    background-color: var(--table-bg, var(--bg-color-base));
  }

  :deep(tr:nth-child(2n)) {
    background-color: var(--bg-color-block, var(--table-header-bg));
  }

  :deep(hr) {
    height: 0;
    margin: 24px 0;
    background: none;
    border: 0;
    border-top: 1px solid var(--divider-color, var(--border-color-light));
  }

  :deep(h1),
  :deep(h2) {
    border-bottom-color: var(--divider-color, var(--border-color-light));
  }

  :deep(blockquote) {
    margin: 12px 0;
    padding: 4px 12px;
    border-left: 3px solid var(--el-color-primary);
    color: var(--text-color-secondary);
  }

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4),
  :deep(h5),
  :deep(h6) {
    scroll-margin-top: 12px;
  }
}

.git-browse-root-docs-content.has-toc .git-browse-root-docs-body {
  padding-left: 20px;
}

.git-browse-root-docs-content:not(.has-toc) .git-browse-root-docs-body {
  width: 100%;
}

.git-browse-tree-row {
  display: grid;
  grid-template-columns: minmax(140px, 1.1fr) minmax(120px, 1.6fr) 100px;
  gap: 12px;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid var(--border-color-light);
  cursor: pointer;
  font-size: 13px;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: var(--table-row-hover-bg);
  }
}

.git-browse-tree-name {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: var(--text-color-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.git-browse-file-icon {
  color: var(--text-color-secondary);
  flex-shrink: 0;

  &.is-dir {
    color: var(--text-color-regular);
  }
}

.git-browse-tree-msg {
  color: var(--text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.git-browse-tree-time {
  text-align: right;
  color: var(--text-color-secondary);
  font-size: 12px;
}

.git-browse-code {
  margin: 0;
  padding: 14px 16px;
  overflow: auto;
  max-height: 70vh;
  font-family: Menlo, Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre;
  color: var(--text-color-regular);
  background: var(--input-bg);
}

.git-browse-truncated {
  padding: 8px 14px;
  color: var(--el-color-warning, #e6a23c);
  font-size: 12px;
  border-bottom: 1px solid var(--border-color-light);
}

.git-browse-empty {
  padding: 40px 0;
  text-align: center;
  color: var(--text-color-placeholder);
  font-size: 13px;
}

.git-browse-aside {
  min-height: 0;
  height: auto;
  padding: 14px 16px;
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  background: var(--card-bg);
  color: var(--text-color-regular);
  overflow: visible;
  box-sizing: border-box;
}

.git-aside-block {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color-light);

  &:last-child {
    border-bottom: none;
  }
}

.git-aside-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.git-aside-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;

  .git-aside-title {
    margin: 0;
    flex: 1;
    min-width: 0;
  }
}

.git-aside-releases-wrap {
  min-height: 24px;
}

.git-aside-releases {
  list-style: none;
  margin: 0;
  padding: 0;
}

.git-aside-release-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  margin-bottom: 14px;
  line-height: 1.4;
}

.git-aside-release-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-color-primary, #1890ff);
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.git-aside-release-icon {
  flex-shrink: 0;
  font-size: 14px;
  color: var(--text-color-secondary);
}

.git-aside-release-title-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.git-aside-release-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 8px;
  font-size: 12px;
  color: var(--text-color-secondary);
}

.git-aside-release-tag {
  font-weight: 500;
  color: var(--text-color-regular);
}

.git-aside-release-sep {
  color: var(--text-color-secondary);
}

.git-aside-release-time {
  color: var(--text-color-secondary);
}

.git-aside-contributors {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.git-aside-contributors :deep(.member-avatar),
.git-contributors-list :deep(.member-avatar) {
  flex-shrink: 0;
}

.git-contributors-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-color-light);

  &:last-child {
    border-bottom: none;
  }
}

.git-contributors-list-body {
  flex: 1;
  min-width: 0;
}

.git-contributors-list-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color-primary);
}

.git-contributors-list-email {
  font-size: 12px;
  color: var(--text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.git-contributors-list-count {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-color-secondary);
}

.git-aside-desc {
  margin: 0 0 12px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-color-secondary);
}

.git-aside-links {
  list-style: none;
  margin: 0;
  padding: 0;

  li {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
    font-size: 13px;
    color: var(--text-color-regular);

    a {
      color: var(--el-color-primary);
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.git-aside-activity-item {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.git-aside-activity-body {
  min-width: 0;
  flex: 1;
}

.git-aside-activity-text {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 6px;
  font-size: 12px;
  color: var(--text-color-regular);
  line-height: 1.4;
}

.git-aside-hash {
  font-family: Menlo, Consolas, monospace;
  color: var(--el-color-primary);

  &.is-link {
    text-decoration: none;
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }
  }
}

.git-aside-activity-msg {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &.is-link {
    text-decoration: none;
    cursor: pointer;

    &:hover {
      color: var(--el-color-primary);
      text-decoration: underline;
    }
  }
}

.git-aside-activity-time {
  flex: 0 0 auto;
  font-size: 12px;
  color: var(--text-color-secondary);
  white-space: nowrap;
}

.git-aside-empty {
  font-size: 12px;
  color: var(--text-color-placeholder);
}

.git-browse-releases {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  padding-right: 20px;
  box-sizing: border-box;
}

.git-releases-empty {
  padding: 48px 16px 64px;
}

.git-releases-empty__title {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color-regular);
  line-height: 1.5;
}

.git-releases-empty__hint {
  margin: 6px 0 0;
  max-width: 420px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-color-placeholder);
}

.git-browse-releases__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  /* 与 Pull Request 页工具栏一致：紧贴 Shell tabs 下沿，下方 12px */
  margin-top: 0;
  margin-bottom: 12px;
}

.git-browse-releases__heading {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.git-browse-releases__heading-icon {
  font-size: 20px;
  color: var(--text-color-secondary);
}

.git-browse-releases__timeline {
  width: 100%;
  position: relative;

  /* 与 PR 列表节奏接近：条目间距收紧，首条不再额外顶空 */
  :deep(.git-release-detail) {
    padding-top: 12px;
    padding-bottom: 12px;
  }

  :deep(.git-release-detail:first-child) {
    padding-top: 0;
  }

  /* 左右栏共用一条连续竖线（避免每条发行版各自 border 断开） */
  &:has(.git-release-detail)::before {
    content: '';
    position: absolute;
    top: 0;
    bottom: 0;
    left: 280px;
    width: 1px;
    background: var(--border-color-light);
    pointer-events: none;
    z-index: 1;
  }
}

@media (max-width: 800px) {
  .git-browse-releases__timeline:has(.git-release-detail)::before {
    display: none;
  }
}

.git-browse-commits {
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  overflow: hidden;
  background: var(--card-bg);
}

.git-browse-commits-toolbar {
  margin-bottom: 12px;
}

/* 与缺陷页查询条同构：inline form + 统一间距/高度 */
.git-browse-commits-query {
  display: flex !important;
  flex-wrap: wrap;
  align-items: center;
  column-gap: var(--cat2bug-toolbar-item-gap, 10px);
  row-gap: var(--cat2bug-toolbar-row-gap, 8px);

  :deep(.el-form-item) {
    margin: 0 !important;
    display: inline-flex;
    align-items: center;
  }

  :deep(.el-form-item__content) {
    display: inline-flex !important;
    align-items: center !important;
    line-height: 1 !important;
    min-height: var(--cat2bug-toolbar-control-height, 32px);
  }

  :deep(.el-select__wrapper),
  :deep(.el-input__wrapper) {
    height: var(--cat2bug-toolbar-control-height, 32px) !important;
    min-height: var(--cat2bug-toolbar-control-height, 32px) !important;
    max-height: var(--cat2bug-toolbar-control-height, 32px) !important;
    box-sizing: border-box;
  }

  :deep(.el-date-editor.el-input),
  :deep(.el-date-editor.el-input__wrapper),
  :deep(.el-date-editor.el-range-editor) {
    height: var(--cat2bug-toolbar-control-height, 32px) !important;
    min-height: var(--cat2bug-toolbar-control-height, 32px) !important;
    max-height: var(--cat2bug-toolbar-control-height, 32px) !important;
    box-sizing: border-box;
  }

  :deep(.el-date-editor.el-range-editor.is-active),
  :deep(.el-date-editor.el-range-editor.is-focus) {
    box-shadow: 0 0 0 1px var(--cat2bug-field-focus-color, var(--el-color-primary, #1890ff)) inset !important;
  }

  /* 与分支/贡献者下拉选中文字同色 */
  :deep(.el-date-editor .el-range-input) {
    color: var(--input-color, var(--text-color-regular)) !important;
  }

  :deep(.el-date-editor .el-range-separator) {
    color: var(--text-color-regular) !important;
  }
}

.git-browse-commits-filter-control {
  width: 180px;
}

.git-browse-commits-filter-daterange {
  width: 260px;
}

.git-browse-commit-group-title {
  display: flex;
  align-items: center;
  box-sizing: border-box;
  padding: 10px 16px;
  min-height: 36px;
  line-height: 1.25;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-color-secondary);
  background: var(--border-color-lighter, #f2f6fc);
  border-bottom: 1px solid var(--border-color-light);
}

.git-browse-commit-group + .git-browse-commit-group {
  border-top: 1px solid var(--border-color-light);
}

.git-browse-commit-item {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-color-light);

  &:last-child {
    border-bottom: none;
  }
}

.git-browse-commit-group .git-browse-commit-item:last-child {
  border-bottom: none;
}

.git-browse-commit-item-msg {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color-primary);
  margin-bottom: 4px;

  .is-link {
    color: inherit;
    text-decoration: none;
    cursor: pointer;

    &:hover {
      color: var(--el-color-primary);
      text-decoration: underline;
    }
  }
}

.git-browse-commit-item-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--text-color-secondary);
}

@media (max-width: 960px) {
  .git-browse-layout {
    grid-template-columns: 1fr;
  }

  .git-browse-aside {
    position: static;
    height: auto;
    order: 2;
  }

  .git-browse-tree-row {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .git-browse-tree-time {
    text-align: left;
  }
}
</style>

<style lang="scss">
/* append-to-body：弹窗样式不能写在 scoped 里 */
.git-create-release-dialog {
  .git-create-release-body-item {
    align-items: flex-start;

    .el-form-item__content {
      width: 100%;
    }
  }

  .cat2bug-textarea {
    width: 100%;
  }

  .git-create-release-markdown {
    width: 100%;
    height: 320px;
    border: 1px solid var(--border-color-base, #dcdfe6);
    border-radius: 6px;
    overflow: hidden;
    background: var(--card-bg, #fff);

    .markdown {
      height: 100%;
      display: flex;
      flex-direction: column;
      box-shadow: none;
    }

    .markdown-tools {
      flex-shrink: 0;
      padding: 2px 6px;
      min-height: 0;
      border-bottom: 1px solid var(--border-color-light, #ebeef5);

      .tools-menu.el-menu--horizontal {
        height: auto !important;
        border: none !important;
      }

      .el-menu-item,
      .el-sub-menu,
      .el-sub-menu__title {
        height: 26px !important;
        min-height: 26px !important;
        line-height: 26px !important;
        width: 26px !important;
        padding: 0 !important;
        margin: 0 1px !important;
      }

      .el-menu--horizontal > .el-menu-item,
      .el-menu--horizontal > .el-sub-menu .el-sub-menu__title {
        border-bottom: none !important;
      }

      .el-sub-menu__icon-arrow {
        display: none !important;
      }
    }

    .markdown-body {
      flex: 1;
      min-height: 0;
      background: var(--card-bg, #fff) !important;

      > .markdown-body-edit,
      > .markdown-body-view {
        padding: 8px 12px;
        background: var(--card-bg, #fff) !important;
        border-right: none !important;
      }

      .cat2bug-md-editor-pane,
      .md-editor {
        --md-bk-color: var(--card-bg, #fff);
        --md-color: var(--text-color-primary, #303133);
        background: var(--card-bg, #fff) !important;
        background-color: var(--card-bg, #fff) !important;
        border: none !important;
        box-shadow: none !important;
      }

      .md-editor .cm-editor,
      .md-editor .cm-scroller,
      .md-editor .cm-content,
      .md-editor .cm-gutters {
        background: var(--card-bg, #fff) !important;
        background-color: var(--card-bg, #fff) !important;
      }

      .md-editor .cm-gutters {
        border-right: none !important;
      }
    }
  }

  .git-create-release-upload {
    width: 100%;

    .el-upload,
    .el-upload-dragger {
      width: 100%;
    }

    .el-upload-dragger {
      padding: 20px 12px;
    }
  }

  .git-create-release-upload__icon {
    font-size: 40px;
    color: var(--el-color-primary, #1890ff);
    margin-bottom: 8px;
  }

  .el-upload__tip {
    margin-top: 6px;
    line-height: 1.4;
    color: var(--text-color-secondary, #909399);
  }
}
</style>

