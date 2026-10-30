<template>
  <div class="defect-gantt-root">
    <div class="defect-gantt-tools defect-view-toolbar">
      <slot name="left-tools"></slot>
      <div class="table-tools row defect-gantt-tools-right">
        <el-radio-group v-model="scale" size="small" class="defect-gantt-scale-switch" @change="onScaleChange">
          <el-radio-button value="day">{{ $t('defect.gantt.scale-day') }}</el-radio-button>
          <el-radio-button value="week">{{ $t('defect.gantt.scale-week') }}</el-radio-button>
          <el-radio-button value="month">{{ $t('defect.gantt.scale-month') }}</el-radio-button>
        </el-radio-group>
        <el-button size="small" plain @click="scrollToToday">{{ $t('defect.gantt.today') }}</el-button>
        <el-divider direction="vertical" class="defect-gantt-tools-divider" />
        <el-dropdown
          trigger="click"
          :hide-on-click="false"
          class="defect-gantt-layers-dropdown"
        >
          <el-button size="small" plain class="defect-gantt-layers-btn" :title="$t('defect.gantt.layers')">
            <el-icon><Operation /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu class="defect-gantt-layers-menu">
              <el-dropdown-item class="defect-gantt-layers-item" @click.stop.prevent>
                <el-checkbox v-model="showDeps" size="small" @click.stop>
                  {{ $t('defect.gantt.layer-deps') }}
                </el-checkbox>
              </el-dropdown-item>
              <el-dropdown-item class="defect-gantt-layers-item" @click.stop.prevent>
                <el-checkbox v-model="showCritical" size="small" @click.stop>
                  {{ $t('defect.gantt.layer-critical') }}
                </el-checkbox>
              </el-dropdown-item>
              <el-dropdown-item class="defect-gantt-layers-item" @click.stop.prevent>
                <el-checkbox v-model="showLoad" size="small" @click.stop>
                  {{ $t('defect.gantt.layer-load') }}
                </el-checkbox>
              </el-dropdown-item>
              <el-dropdown-item class="defect-gantt-layers-item" @click.stop.prevent>
                <el-checkbox v-model="showBaseline" size="small" @click.stop>
                  {{ $t('defect.gantt.layer-baseline') }}
                </el-checkbox>
              </el-dropdown-item>
              <el-dropdown-item
                divided
                :disabled="!canEdit"
                class="defect-gantt-layers-action"
                @click="confirmFreezeBaseline"
              >
                <span class="defect-gantt-layers-action-label">
                  <el-icon><RefreshRight /></el-icon>
                  <span>{{ $t('defect.gantt.freeze-baseline') }}</span>
                </span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <slot name="right-tools"></slot>
      </div>
    </div>

    <div v-if="showLoad && !isHandlerGroupBy" class="defect-gantt-load-banner">
      {{ $t('defect.gantt.load-need-handler-group') }}
    </div>

    <div v-if="showCritical && criticalDefectIds.size === 0" class="defect-gantt-load-banner">
      {{ $t('defect.gantt.critical-empty') }}
    </div>

    <div v-if="showDeps && selectedLinkId != null" class="defect-gantt-load-banner">
      {{ $t('defect.gantt.link-selected-hint') }}
    </div>

    <div v-loading="loading" class="defect-gantt-body" :element-loading-text="$t('loading')">
      <div class="defect-gantt-main">
        <div class="defect-gantt-scheduled" ref="scheduledPane">
          <div class="defect-gantt-left" :style="{ width: leftWidth + 'px' }">
            <div class="defect-gantt-left-header" :style="{ height: headerHeight + 'px' }">
              <el-tabs
                v-model="groupBy"
                class="defect-gantt-group-tabs"
                @tab-change="onGroupByChange"
              >
                <el-tab-pane name="handler">
                  <template #label>
                    <span class="defect-gantt-group-tab-label">
                      <el-icon><User /></el-icon>
                      <span>{{ $t('defect.gantt.group-handler') }}</span>
                    </span>
                  </template>
                </el-tab-pane>
                <el-tab-pane name="module">
                  <template #label>
                    <span class="defect-gantt-group-tab-label">
                      <svg-icon icon-class="menu" class-name="defect-gantt-group-tab-svg" />
                      <span>{{ $t('defect.gantt.group-module') }}</span>
                    </span>
                  </template>
                </el-tab-pane>
              </el-tabs>
            </div>
            <div
              ref="leftScroll"
              class="defect-gantt-left-scroll"
              :class="{ 'is-empty': !groups.length }"
              @scroll="onLeftScroll"
            >
              <template v-for="(g, gi) in groups" :key="g.key">
                <template v-if="isTempModuleInsertAt(gi)">
                  <div
                    class="defect-gantt-group-header is-temp-module"
                    :style="{ height: groupHeaderHeight + 'px' }"
                  >
                    <span class="defect-gantt-group-label">{{ dropInsertPreview.groupLabel || emptyModuleLabel }}</span>
                  </div>
                  <div
                    class="defect-gantt-row-label is-drop-insert"
                    :style="{ height: rowHeight + 'px' }"
                  >
                    <span class="defect-gantt-row-num">#{{ dropInsertPreview.defect.projectNum }}</span>
                    <span class="defect-gantt-row-name" :title="dropInsertPreview.defect.defectName">{{ dropInsertPreview.defect.defectName }}</span>
                  </div>
                </template>
                <div
                  class="defect-gantt-group-header"
                  :class="{ 'is-collapsed': isGroupCollapsed(g.key), 'is-structural': !!g.structural }"
                  :style="groupHeaderStyle(g)"
                  :data-group-key="g.key"
                  role="button"
                  tabindex="0"
                  :title="groupHeaderTitle(g)"
                  :aria-expanded="!isGroupCollapsed(g.key)"
                  @click="toggleGroupCollapsed(g.key)"
                  @keydown.enter.prevent="toggleGroupCollapsed(g.key)"
                  @keydown.space.prevent="toggleGroupCollapsed(g.key)"
                >
                  <span class="defect-gantt-group-toggle" aria-hidden="true">
                    <el-icon>
                      <ArrowRight v-if="isGroupCollapsed(g.key)" />
                      <ArrowDown v-else />
                    </el-icon>
                  </span>
                  <cat2-bug-avatar
                    v-if="g.type === 'handler' && g.handlerMember"
                    class="defect-gantt-group-avatar"
                    :member="g.handlerMember"
                    :size="28"
                  />
                  <span
                    class="defect-gantt-group-label"
                    :title="g.modulePath || g.label || emptyModuleLabel"
                  >{{ g.label || emptyModuleLabel }}</span>
                  <span
                    v-if="formatGroupDays(g)"
                    class="defect-gantt-days is-group"
                    :title="$t('defect.gantt.group-days-tip', { n: formatGroupDays(g) })"
                  >{{ $t('defect.gantt.days', { n: formatGroupDays(g) }) }}</span>
                </div>
                <template v-if="!isGroupCollapsed(g.key)">
                  <template v-for="(row, ri) in g.rows" :key="row.rowKey">
                    <transition name="defect-gantt-insert">
                      <div
                        v-if="isDropInsertAt(g.key, ri)"
                        class="defect-gantt-row-label is-drop-insert"
                        :style="{ height: rowHeight + 'px' }"
                      >
                        <span class="defect-gantt-row-num">#{{ dropInsertPreview.defect.projectNum }}</span>
                        <span class="defect-gantt-row-name" :title="dropInsertPreview.defect.defectName">{{ dropInsertPreview.defect.defectName }}</span>
                      </div>
                    </transition>
                    <div
                      class="defect-gantt-row-label"
                      :class="{ 'is-selected': isSelectedDefect(row.defect) }"
                      :style="{ height: rowHeight + 'px' }"
                      :data-row-key="row.rowKey"
                      :data-group-key="g.key"
                      @click="focusGanttRow(row, g)"
                    >
                      <span class="defect-gantt-row-num">#{{ row.defect.projectNum }}</span>
                      <span class="defect-gantt-row-name" :title="row.defect.defectName">{{ row.defect.defectName }}</span>
                      <el-tooltip
                        v-if="row.mirrored"
                        placement="top"
                        :content="mirrorTip(row)"
                      >
                        <el-icon class="defect-gantt-mirror-icon"><Link /></el-icon>
                      </el-tooltip>
                      <span
                        v-if="formatDefectDays(row.defect)"
                        class="defect-gantt-days"
                        :title="$t('defect.gantt.days-tip', { n: formatDefectDays(row.defect) })"
                      >{{ $t('defect.gantt.days', { n: formatDefectDays(row.defect) }) }}</span>
                    </div>
                  </template>
                  <transition name="defect-gantt-insert">
                    <div
                      v-if="isDropInsertAt(g.key, g.rows.length)"
                      class="defect-gantt-row-label is-drop-insert"
                      :style="{ height: rowHeight + 'px' }"
                    >
                      <span class="defect-gantt-row-num">#{{ dropInsertPreview.defect.projectNum }}</span>
                      <span class="defect-gantt-row-name" :title="dropInsertPreview.defect.defectName">{{ dropInsertPreview.defect.defectName }}</span>
                    </div>
                  </transition>
                  <div
                    v-if="shouldShowLoadTrack(g)"
                    class="defect-gantt-load-label"
                    :style="{ height: loadTrackHeight + 'px' }"
                  >
                    {{ $t('defect.gantt.load-track-label') }}
                  </div>
                </template>
              </template>
              <template v-if="isTempModuleInsertAt(groups.length)">
                <div
                  class="defect-gantt-group-header is-temp-module"
                  :style="{ height: groupHeaderHeight + 'px' }"
                >
                  <span class="defect-gantt-group-label">{{ dropInsertPreview.groupLabel || emptyModuleLabel }}</span>
                </div>
                <div
                  class="defect-gantt-row-label is-drop-insert"
                  :style="{ height: rowHeight + 'px' }"
                >
                  <span class="defect-gantt-row-num">#{{ dropInsertPreview.defect.projectNum }}</span>
                  <span class="defect-gantt-row-name" :title="dropInsertPreview.defect.defectName">{{ dropInsertPreview.defect.defectName }}</span>
                </div>
              </template>
              <transition name="defect-gantt-insert">
                <div
                  v-if="dropInsertPreview && dropInsertPreview.kind !== 'temp-module' && dropInsertPreview.kind !== 'forbidden' && (dropInsertPreview.groupKey === '__tail__' || dropInsertPreview.groupKey === '__new__')"
                  class="defect-gantt-row-label is-drop-insert"
                  :style="{ height: rowHeight + 'px' }"
                >
                  <span class="defect-gantt-row-num">#{{ dropInsertPreview.defect.projectNum }}</span>
                  <span class="defect-gantt-row-name" :title="dropInsertPreview.defect.defectName">{{ dropInsertPreview.defect.defectName }}</span>
                </div>
              </transition>
              <cat2-bug-empty
                v-if="!groups.length && !loading && !dropInsertPreview"
                class="defect-gantt-empty"
                size="small"
                :description="$t('defect.gantt.empty-scheduled')"
              />
              <!-- 与右侧时间轴通底对齐的空白垫高（空态勿垫，否则空状态 + 通底会撑出纵向滚动条） -->
              <div
                v-if="groups.length && axisFillHeight > 0"
                class="defect-gantt-left-fill"
                :style="{ height: axisFillHeight + 'px' }"
              />
            </div>
          </div>

          <div
            class="defect-gantt-resizer"
            @mousedown.prevent="startResizeLeft"
          />

          <div class="defect-gantt-axis">
            <div
              ref="axisHeaderScroll"
              class="defect-gantt-axis-header-scroll"
              @scroll="onAxisHeaderScroll"
            >
              <div
                class="defect-gantt-axis-header"
                :style="{ width: axisWidthPx + 'px', height: headerHeight + 'px' }"
              >
                <div class="defect-gantt-axis-major" :style="{ height: headerMajorHeight + 'px' }">
                  <div
                    v-for="b in axisMajorBands"
                    :key="b.key"
                    class="defect-gantt-axis-major-cell"
                    :style="{ width: (b.unitCount * colWidth) + 'px' }"
                    :title="b.label"
                  >
                    {{ formatMajorLabel(b) }}
                  </div>
                </div>
                <div class="defect-gantt-axis-minor" :style="{ height: headerMinorHeight + 'px' }">
                  <div
                    v-for="u in axisUnits"
                    :key="u.key"
                    class="defect-gantt-axis-unit"
                    :class="{ weekend: !!u.weekend, 'is-day-scale': scale === 'day', 'is-week-scale': scale === 'week' }"
                    :style="{ width: colWidth + 'px' }"
                    :title="formatUnitTitle(u)"
                  >
                    <template v-if="scale === 'day'">
                      <span class="defect-gantt-axis-day-num">{{ formatUnitLabel(u) }}</span>
                      <span class="defect-gantt-axis-weekday">{{ formatWeekdayShort(u) }}</span>
                    </template>
                    <template v-else-if="scale === 'week'">
                      <span class="defect-gantt-axis-week-num">{{ formatWeekUnitLabel(u) }}</span>
                      <span class="defect-gantt-axis-week-range">{{ formatWeekUnitRange(u) }}</span>
                    </template>
                    <template v-else>
                      {{ formatUnitLabel(u) }}
                    </template>
                  </div>
                </div>
              </div>
            </div>
            <div
              ref="axisBodyScroll"
              class="defect-gantt-axis-body-scroll"
              :class="{ 'is-empty': !groups.length }"
              @scroll="onAxisBodyScroll"
              @dragover.prevent="onAxisDragOver"
              @drop.prevent="onAxisDrop"
            >
              <div
                class="defect-gantt-axis-body"
                :class="{
                  'is-critical-mode': showCritical && criticalDefectIds.size > 0,
                  'is-linking': !!linkDrag
                }"
                :style="axisBodyStyle"
                @mousedown="onAxisBlankDown"
              >
                <div
                  v-for="(u, ui) in axisUnits"
                  :key="'bg-' + u.key"
                  class="defect-gantt-axis-col-bg"
                  :class="{ weekend: !!u.weekend }"
                  :style="{ left: (ui * colWidth) + 'px', width: colWidth + 'px' }"
                />
                <div
                  v-if="todayLineLeft != null"
                  class="defect-gantt-today-line"
                  :style="{ left: todayLineLeft + 'px' }"
                />

                <template v-for="(g, gi) in groups" :key="'axis-' + g.key">
                  <template v-if="isTempModuleInsertAt(gi)">
                    <div
                      class="defect-gantt-axis-group-band is-temp-module"
                      :style="{ height: groupHeaderHeight + 'px' }"
                    />
                    <div
                      class="defect-gantt-axis-row is-drop-insert"
                      :style="{ height: rowHeight + 'px' }"
                    >
                      <div
                        class="defect-gantt-bar is-drop-preview"
                        :class="'state-' + stateClass(dropInsertPreview.defect)"
                        :style="{ left: dropInsertPreview.barX + 'px', width: dropInsertPreview.barWidth + 'px' }"
                      >
                        <span class="defect-gantt-bar-label">#{{ dropInsertPreview.defect.projectNum }} {{ dropInsertPreview.defect.defectName }}</span>
                      </div>
                    </div>
                  </template>
                  <div
                    class="defect-gantt-axis-group-band"
                    :class="{ 'is-collapsed': isGroupCollapsed(g.key) }"
                    :style="{ height: groupHeaderHeight + 'px' }"
                    :data-group-key="g.key"
                    @dragover.prevent.stop="onGroupDragOver($event, g)"
                    @drop.prevent.stop="onGroupDrop($event, g)"
                  />
                  <template v-if="!isGroupCollapsed(g.key)">
                    <template v-for="(row, ri) in g.rows" :key="'bar-' + row.rowKey">
                      <transition name="defect-gantt-insert">
                        <div
                          v-if="isDropInsertAt(g.key, ri)"
                          class="defect-gantt-axis-row is-drop-insert"
                          :style="{ height: rowHeight + 'px' }"
                          @dragover.prevent.stop="onGroupDragOver($event, g)"
                          @drop.prevent.stop="onGroupDrop($event, g)"
                        >
                          <div
                            class="defect-gantt-bar is-drop-preview"
                            :class="'state-' + stateClass(dropInsertPreview.defect)"
                            :style="{ left: dropInsertPreview.barX + 'px', width: dropInsertPreview.barWidth + 'px' }"
                          >
                            <span class="defect-gantt-bar-label">#{{ dropInsertPreview.defect.projectNum }} {{ dropInsertPreview.defect.defectName }}</span>
                          </div>
                        </div>
                      </transition>
                      <div
                        class="defect-gantt-axis-row"
                        :class="{ 'is-selected': isSelectedDefect(row.defect) }"
                        :style="{ height: rowHeight + 'px' }"
                        :data-row-key="row.rowKey"
                        :data-group-key="g.key"
                        @click="selectDefect(row.defect)"
                        @dragover.prevent.stop="onGroupDragOver($event, g)"
                        @drop.prevent.stop="onGroupDrop($event, g)"
                      >
                        <div
                          v-if="showBaseline && baselineBarStyle(row)"
                          class="defect-gantt-bar is-baseline"
                          :style="baselineBarStyle(row)"
                          aria-hidden="true"
                        />
                        <div
                          v-if="barStyle(row)"
                          class="defect-gantt-bar"
                          draggable="false"
                          :class="[
                            'state-' + stateClass(row.defect),
                            {
                              'is-dragging': dragState && dragState.rowKey === row.rowKey,
                              'is-selected': isSelectedDefect(row.defect),
                              'is-forbidden': isBarModuleForbidden(row),
                              'is-critical': showCritical && isCriticalDefect(row.defect),
                              'is-link-target': isLinkDragTarget(row.defect),
                              'is-link-target-invalid': isLinkDragTargetInvalid(row.defect)
                            }
                          ]"
                          :style="barStyle(row)"
                          :data-defect-id="row.defect.defectId"
                          data-gantt-bar="1"
                          :title="isBarModuleForbidden(row) ? $t('defect.gantt.module-drop-forbidden') : undefined"
                          @dragstart.prevent.stop
                          @mousedown.stop="onBarMouseDown($event, row, g, 'move')"
                          @click.stop="onBarClick($event, row.defect)"
                        >
                          <span
                            v-if="!showDeps"
                            class="defect-gantt-bar-handle left"
                            @mousedown.stop="onBarMouseDown($event, row, g, 'start')"
                          />
                          <span
                            v-if="showDeps && canEdit"
                            class="defect-gantt-bar-port left"
                            :class="{
                              'is-active': isLinkPortActive(row.defect, 'head'),
                              'is-valid-drop': isLinkPortValidDrop(row.defect, 'head')
                            }"
                            :title="$t('defect.gantt.port-head')"
                            data-gantt-port="head"
                            :data-defect-id="row.defect.defectId"
                            @mousedown.stop.prevent="onLinkPortDown($event, row.defect, 'head')"
                          />
                          <span
                            class="defect-gantt-bar-label"
                            :title="'#' + row.defect.projectNum + ' ' + (row.defect.defectName || '')"
                          >
                            <span class="defect-gantt-bar-num">#{{ row.defect.projectNum }}</span>
                            <span
                              v-if="row.defect.defectName"
                              class="defect-gantt-bar-name"
                            >{{ row.defect.defectName }}</span>
                          </span>
                          <span
                            v-if="showDeps && canEdit"
                            class="defect-gantt-bar-port right"
                            :class="{
                              'is-active': isLinkPortActive(row.defect, 'tail'),
                              'is-valid-drop': isLinkPortValidDrop(row.defect, 'tail')
                            }"
                            :title="$t('defect.gantt.port-tail')"
                            data-gantt-port="tail"
                            :data-defect-id="row.defect.defectId"
                            @mousedown.stop.prevent="onLinkPortDown($event, row.defect, 'tail')"
                          />
                          <span
                            v-if="!showDeps"
                            class="defect-gantt-bar-handle right"
                            @mousedown.stop="onBarMouseDown($event, row, g, 'end')"
                          />
                        </div>
                      </div>
                    </template>
                    <transition name="defect-gantt-insert">
                      <div
                        v-if="isDropInsertAt(g.key, g.rows.length)"
                        class="defect-gantt-axis-row is-drop-insert"
                        :style="{ height: rowHeight + 'px' }"
                        @dragover.prevent.stop="onGroupDragOver($event, g)"
                        @drop.prevent.stop="onGroupDrop($event, g)"
                      >
                        <div
                          class="defect-gantt-bar is-drop-preview"
                          :class="'state-' + stateClass(dropInsertPreview.defect)"
                          :style="{ left: dropInsertPreview.barX + 'px', width: dropInsertPreview.barWidth + 'px' }"
                        >
                          <span class="defect-gantt-bar-label">#{{ dropInsertPreview.defect.projectNum }} {{ dropInsertPreview.defect.defectName }}</span>
                        </div>
                      </div>
                    </transition>
                    <div
                      v-if="shouldShowLoadTrack(g)"
                      class="defect-gantt-load-track"
                      :style="{ height: loadTrackHeight + 'px', width: axisWidthPx + 'px' }"
                    >
                      <div class="defect-gantt-load-guide is-top" />
                      <div class="defect-gantt-load-guide is-mid" />
                      <div class="defect-gantt-load-guide is-base" />
                      <div
                        v-for="(seg, si) in loadTrackSegmentsOf(g)"
                        :key="'load-' + g.key + '-' + si"
                        class="defect-gantt-load-seg"
                        :class="seg.level > 1 ? 'is-overload' : 'is-normal'"
                        :style="{ left: seg.left + 'px', width: seg.width + 'px' }"
                        :title="$t('defect.gantt.load-seg-tip', { n: seg.count })"
                      />
                    </div>
                  </template>
                </template>
                <template v-if="isTempModuleInsertAt(groups.length)">
                  <div
                    class="defect-gantt-axis-group-band is-temp-module"
                    :style="{ height: groupHeaderHeight + 'px' }"
                  />
                  <div
                    class="defect-gantt-axis-row is-drop-insert"
                    :style="{ height: rowHeight + 'px' }"
                  >
                    <div
                      class="defect-gantt-bar is-drop-preview"
                      :class="'state-' + stateClass(dropInsertPreview.defect)"
                      :style="{ left: dropInsertPreview.barX + 'px', width: dropInsertPreview.barWidth + 'px' }"
                    >
                      <span class="defect-gantt-bar-label">#{{ dropInsertPreview.defect.projectNum }} {{ dropInsertPreview.defect.defectName }}</span>
                    </div>
                  </div>
                </template>
                <transition name="defect-gantt-insert">
                  <div
                    v-if="dropInsertPreview && dropInsertPreview.kind !== 'temp-module' && dropInsertPreview.kind !== 'forbidden' && (dropInsertPreview.groupKey === '__tail__' || dropInsertPreview.groupKey === '__new__')"
                    class="defect-gantt-axis-row is-drop-insert"
                    :style="{ height: rowHeight + 'px' }"
                  >
                    <div
                      class="defect-gantt-bar is-drop-preview"
                      :class="'state-' + stateClass(dropInsertPreview.defect)"
                      :style="{ left: dropInsertPreview.barX + 'px', width: dropInsertPreview.barWidth + 'px' }"
                    >
                      <span class="defect-gantt-bar-label">#{{ dropInsertPreview.defect.projectNum }} {{ dropInsertPreview.defect.defectName }}</span>
                    </div>
                  </div>
                </transition>

                <!-- 二期：依赖连线层（虚线；可点选后 Del 删除） -->
                <svg
                  v-if="showDeps && (depLinkPaths.length || linkDragPreviewPath)"
                  class="defect-gantt-dep-layer"
                  :class="{ 'is-interactive': canEdit && !linkDrag }"
                  :width="axisWidthPx"
                  :height="axisBodyHeight"
                >
                  <defs>
                    <marker
                      id="defect-gantt-dep-arrow"
                      markerWidth="10"
                      markerHeight="8"
                      refX="8"
                      refY="4"
                      orient="auto"
                      markerUnits="userSpaceOnUse"
                    >
                      <path
                        d="M1.5,1.2 C1.5,1.2 6.8,3.2 8.2,4 C6.8,4.8 1.5,6.8 1.5,6.8"
                        fill="none"
                        stroke="var(--el-color-primary, #409eff)"
                        stroke-width="1.4"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        opacity="0.85"
                      />
                    </marker>
                    <marker
                      id="defect-gantt-dep-arrow-selected"
                      markerWidth="10"
                      markerHeight="8"
                      refX="8"
                      refY="4"
                      orient="auto"
                      markerUnits="userSpaceOnUse"
                    >
                      <path
                        d="M1.5,1.2 C1.5,1.2 6.8,3.2 8.2,4 C6.8,4.8 1.5,6.8 1.5,6.8"
                        fill="none"
                        stroke="var(--el-color-warning, #e6a23c)"
                        stroke-width="1.5"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      />
                    </marker>
                    <marker
                      id="defect-gantt-dep-arrow-critical"
                      markerWidth="10"
                      markerHeight="8"
                      refX="8"
                      refY="4"
                      orient="auto"
                      markerUnits="userSpaceOnUse"
                    >
                      <path
                        d="M1.5,1.2 C1.5,1.2 6.8,3.2 8.2,4 C6.8,4.8 1.5,6.8 1.5,6.8"
                        fill="none"
                        stroke="var(--el-color-danger-light-3, #f89898)"
                        stroke-width="1.5"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      />
                    </marker>
                  </defs>
                  <g v-for="p in depLinkPaths" :key="'dep-' + p.linkId">
                    <path
                      :d="p.d"
                      class="defect-gantt-dep-hit"
                      fill="none"
                      @mousedown.stop.prevent="onDepLinkMouseDown($event, p)"
                    />
                    <path
                      :d="p.d"
                      class="defect-gantt-dep-path"
                      :class="{
                        'is-critical-link': showCritical && p.critical,
                        'is-selected': selectedLinkId != null && Number(selectedLinkId) === Number(p.linkId)
                      }"
                      fill="none"
                      :marker-end="depLinkMarkerEnd(p)"
                      pointer-events="none"
                    />
                  </g>
                  <path
                    v-if="linkDragPreviewPath"
                    :d="linkDragPreviewPath"
                    class="defect-gantt-dep-path is-preview"
                    fill="none"
                    marker-end="url(#defect-gantt-dep-arrow)"
                    pointer-events="none"
                  />
                </svg>

                <!-- 二期：框选矩形 -->
                <div
                  v-if="marqueeRect"
                  class="defect-gantt-marquee-rect"
                  :style="marqueeRectStyle"
                />
              </div>
            </div>
          </div>
        </div>

        <div
          v-if="!unscheduledCollapsed"
          class="defect-gantt-unscheduled-resizer"
          :title="$t('defect.gantt.resize-unscheduled')"
          @mousedown.prevent="startResizeUnscheduled"
        />
        <div
          class="defect-gantt-unscheduled"
          :class="{ 'is-collapsed': unscheduledCollapsed }"
          :style="unscheduledCollapsed ? undefined : { height: unscheduledHeight + 'px' }"
        >
          <div class="defect-gantt-unscheduled-head">
            <div
              class="defect-gantt-unscheduled-toggle"
              role="button"
              tabindex="0"
              :title="unscheduledCollapsed ? $t('defect.gantt.expand-unscheduled') : $t('defect.gantt.collapse-unscheduled')"
              :aria-expanded="!unscheduledCollapsed"
              @click="toggleUnscheduledCollapsed"
              @keydown.enter.prevent="toggleUnscheduledCollapsed"
              @keydown.space.prevent="toggleUnscheduledCollapsed"
            >
              <span class="defect-gantt-group-toggle" aria-hidden="true">
                <el-icon>
                  <ArrowRight v-if="unscheduledCollapsed" />
                  <ArrowDown v-else />
                </el-icon>
              </span>
              <span class="defect-gantt-unscheduled-title">
                {{ $t('defect.gantt.unscheduled') }}
                <span class="defect-gantt-unscheduled-count">({{ filteredUnscheduled.length }}{{ unscheduledKeywordTrimmed ? '/' + unscheduled.length : '' }})</span>
              </span>
            </div>
            <el-input
              v-if="!unscheduledCollapsed"
              v-model="unscheduledKeyword"
              class="defect-gantt-unscheduled-search"
              size="small"
              clearable
              :prefix-icon="Search"
              :placeholder="$t('defect.gantt.unscheduled-search-placeholder')"
            />
          </div>
          <div v-show="!unscheduledCollapsed" class="defect-gantt-unscheduled-pool">
            <div
              v-for="d in filteredUnscheduled"
              :key="'u-' + d.defectId"
              class="defect-gantt-chip"
              :class="[
                'state-' + stateClass(d),
                {
                  'is-selected': isSelectedDefect(d),
                  'is-dragging': pendingUnscheduled && pendingUnscheduled.defectId === d.defectId
                }
              ]"
              :draggable="canEdit"
              @click="selectDefect(d)"
              @dragstart="onUnscheduledDragStart($event, d)"
              @dragend="onUnscheduledDragEnd"
            >
              <span class="defect-gantt-chip-num">#{{ d.projectNum }}</span>
              <span class="defect-gantt-chip-name" :title="d.defectName">{{ d.defectName }}</span>
            </div>
            <span v-if="!unscheduled.length && !loading" class="defect-gantt-unscheduled-empty">
              {{ $t('defect.gantt.empty-unscheduled') }}
            </span>
            <span
              v-else-if="unscheduled.length && !filteredUnscheduled.length && !loading"
              class="defect-gantt-unscheduled-empty"
            >
              {{ $t('defect.gantt.unscheduled-search-empty') }}
            </span>
          </div>
        </div>
      </div>
      <div v-if="total > pageSize" class="defect-gantt-cap-hint">
        {{ $t('defect.gantt.cap-hint', { n: pageSize, total }) }}
      </div>
    </div>

    <!-- 拖拽跟随画面：位置走 transform，避免每帧改响应式 left/top -->
    <teleport to="body">
      <div
        v-show="showFloatingDragGhost"
        ref="dragGhostEl"
        class="defect-gantt-drag-ghost"
        :class="['state-' + dragGhost.stateClass, 'kind-' + dragGhost.kind, { 'is-forbidden': dragGhost.forbidden }]"
        :style="dragGhostStyle"
        :title="dragGhost.forbidden ? $t('defect.gantt.module-drop-forbidden') : undefined"
      >
        <span class="defect-gantt-drag-ghost-num">#{{ dragGhost.projectNum }}</span>
        <span class="defect-gantt-drag-ghost-name">{{ dragGhost.name }}</span>
      </div>
    </teleport>
  </div>
</template>

<script>
import { ArrowDown, ArrowRight, Link, Operation, RefreshRight, Search, User } from '@element-plus/icons-vue'
import {
  listDefect,
  updateDefect,
  getDefectPlanGanttMeta,
  createDefectPlanLink,
  deleteDefectPlanLink,
  freezeDefectPlanBaseline
} from '@/api/system/defect'
import { listModulePath } from '@/api/system/module'
import { checkPermi } from '@/utils/permission'
import Cat2BugAvatar from '@/components/Cat2BugAvatar'
import {
  GANTT_PAGE_SIZE,
  GANTT_GROUP_BY,
  GANTT_SCALE,
  GANTT_CACHE,
  GANTT_LEFT_WIDTH_DEFAULT,
  GANTT_LEFT_WIDTH_MIN,
  GANTT_LEFT_WIDTH_MAX,
  GANTT_UNSCHEDULED_HEIGHT_DEFAULT,
  GANTT_UNSCHEDULED_HEIGHT_MIN,
  GANTT_UNSCHEDULED_HEIGHT_MAX,
  GANTT_ROW_HEIGHT,
  GANTT_HEADER_HEIGHT,
  GANTT_HEADER_MAJOR_HEIGHT,
  GANTT_HEADER_MINOR_HEIGHT,
  GANTT_GROUP_HEADER_HEIGHT,
  GANTT_LOAD_TRACK_HEIGHT,
  GANTT_COL_WIDTH,
  projectGanttRows,
  splitModulePath,
  defectStateClass,
  resolveHandlers,
  defaultPlanRangeFromDropDay,
  shiftPlanRange,
  resizePlanRange,
  computeAxisRange,
  buildAxisUnits,
  buildAxisMajorBands,
  dateToOffsetPx,
  offsetPxToDate,
  barGeometry,
  parsePlanDate,
  isoWeekNumber,
  planDurationDays,
  formatDurationDays,
  computeCriticalPathIds,
  buildHandlerLoadTrackSegments,
  findFsScheduleConflicts
} from './defect-gantt'

export default {
  name: 'DefectGantt',
  components: { ArrowDown, ArrowRight, Link, Operation, RefreshRight, Search, User, Cat2BugAvatar },
  setup() {
    return { Search }
  },
  props: {
    query: {
      type: Object,
      default: () => ({})
    },
    projectId: {
      type: Number,
      default: null
    }
  },
  data() {
    const cache = this.$cache && this.$cache.local
    let groupBy = (cache && cache.get(GANTT_CACHE.GROUP_BY)) || GANTT_GROUP_BY.HANDLER
    if (groupBy !== GANTT_GROUP_BY.HANDLER && groupBy !== GANTT_GROUP_BY.MODULE) {
      groupBy = GANTT_GROUP_BY.HANDLER
    }
    let scale = (cache && cache.get(GANTT_CACHE.SCALE)) || GANTT_SCALE.DAY
    if (![GANTT_SCALE.DAY, GANTT_SCALE.WEEK, GANTT_SCALE.MONTH].includes(scale)) {
      scale = GANTT_SCALE.DAY
    }
    let leftWidth = Number(cache && cache.get(GANTT_CACHE.LEFT_WIDTH))
    if (!Number.isFinite(leftWidth) || leftWidth < GANTT_LEFT_WIDTH_MIN) {
      leftWidth = GANTT_LEFT_WIDTH_DEFAULT
    }
    leftWidth = Math.min(GANTT_LEFT_WIDTH_MAX, Math.max(GANTT_LEFT_WIDTH_MIN, leftWidth))
    let unscheduledHeight = Number(cache && cache.get(GANTT_CACHE.UNSCHEDULED_HEIGHT))
    if (!Number.isFinite(unscheduledHeight) || unscheduledHeight < GANTT_UNSCHEDULED_HEIGHT_MIN) {
      unscheduledHeight = GANTT_UNSCHEDULED_HEIGHT_DEFAULT
    }
    unscheduledHeight = Math.min(
      GANTT_UNSCHEDULED_HEIGHT_MAX,
      Math.max(GANTT_UNSCHEDULED_HEIGHT_MIN, unscheduledHeight)
    )
    const unscheduledCollapsedRaw = cache && cache.get(GANTT_CACHE.UNSCHEDULED_COLLAPSED)
    const unscheduledCollapsed =
      unscheduledCollapsedRaw === true ||
      unscheduledCollapsedRaw === 'true' ||
      unscheduledCollapsedRaw === '1'
    let groupRowOrder = {}
    try {
      const rawOrder = cache && cache.get(GANTT_CACHE.ROW_ORDER)
      if (rawOrder) {
        const parsed = typeof rawOrder === 'string' ? JSON.parse(rawOrder) : rawOrder
        if (parsed && typeof parsed === 'object') groupRowOrder = parsed
      }
    } catch (_) { /* ignore */ }
    let collapsedGroups = {}
    try {
      const rawCollapsed = cache && cache.get(GANTT_CACHE.COLLAPSED)
      if (rawCollapsed) {
        const parsed = typeof rawCollapsed === 'string' ? JSON.parse(rawCollapsed) : rawCollapsed
        if (Array.isArray(parsed)) {
          parsed.forEach((k) => {
            if (k != null && k !== '') collapsedGroups[String(k)] = true
          })
        } else if (parsed && typeof parsed === 'object') {
          collapsedGroups = parsed
        }
      }
    } catch (_) { /* ignore */ }
    let showDeps = false
    let showCritical = false
    let showLoad = false
    let showBaseline = false
    try {
      const rawLayers = cache && cache.get(GANTT_CACHE.LAYERS)
      if (rawLayers) {
        const parsed = typeof rawLayers === 'string' ? JSON.parse(rawLayers) : rawLayers
        if (parsed && typeof parsed === 'object') {
          showDeps = !!parsed.deps
          showCritical = !!parsed.critical
          showLoad = !!parsed.load
          showBaseline = !!parsed.baseline
        }
      }
    } catch (_) { /* ignore */ }
    return {
      loading: false,
      defectList: [],
      total: 0,
      queryParams: this.query || {},
      requestGen: 0,
      pageSize: GANTT_PAGE_SIZE,
      groupBy,
      scale,
      leftWidth,
      unscheduledHeight,
      /** 未排期池是否折叠 */
      unscheduledCollapsed,
      /** 分组内行序覆盖 { [groupKey]: defectId[] } */
      groupRowOrder,
      /** 已折叠分组 { [groupKey]: true } */
      collapsedGroups,
      /** 未排期池本地过滤关键字（编号 / 名称） */
      unscheduledKeyword: '',
      rowHeight: GANTT_ROW_HEIGHT,
      headerHeight: GANTT_HEADER_HEIGHT,
      headerMajorHeight: GANTT_HEADER_MAJOR_HEIGHT,
      headerMinorHeight: GANTT_HEADER_MINOR_HEIGHT,
      groupHeaderHeight: GANTT_GROUP_HEADER_HEIGHT,
      loadTrackHeight: GANTT_LOAD_TRACK_HEIGHT,
      syncingScroll: false,
      dragState: null,
      resizeLeft: null,
      resizeUnscheduled: null,
      pendingUnscheduled: null,
      saving: false,
      /** 当前高亮缺陷（按 defectId，镜像行同步高亮） */
      selectedDefectId: null,
      /** 时间轴可视区高度，用于格子通底 */
      axisViewportHeight: 0,
      /** 拖拽幽灵画面 */
      dragGhost: {
        visible: false,
        kind: 'chip',
        projectNum: '',
        name: '',
        stateClass: 'unknown',
        forbidden: false,
        x: 0,
        y: 0,
        width: 160
      },
      /** 落点日引导线（相对视口） */
      /** 按下点相对幽灵左上角，保证「点哪拖哪」 */
      _dragGhostOffsetX: 0,
      _dragGhostOffsetY: 0,
      _ghostClientX: 0,
      _ghostClientY: 0,
      _ghostRaf: 0,
      _previewClientX: 0,
      _previewClientY: 0,
      _previewRaf: 0,
      _barMoveEvent: null,
      _barMoveRaf: 0,
      /** 未松手前落点插入预览行 */
      dropInsertPreview: null,
      /** moduleId → 全路径，用于按交付物树序/缩进 */
      modulePathById: {},
      _modulePathGen: 0,
      _modulePathProjectId: null,
      /* —— 甘特二期（默认关，不影响一期） —— */
      showDeps,
      showCritical,
      showLoad,
      showBaseline,
      /** 计划依赖边 */
      planLinks: [],
      /** 活跃基线 { baselineId, items: [{ defectId, planStartTime, planEndTime }] } */
      planBaseline: null,
      /** 可视化连线拖拽：tail→head / head→tail 均为 FS */
      linkDrag: null,
      /** 当前选中的依赖 linkId（Del 删除） */
      selectedLinkId: null,
      /** 多选 defectId 列表（框选 / Ctrl·⌘·Shift 点选） */
      marqueeSelectedIds: [],
      /** 框选拖拽状态 */
      marqueeDrag: null,
      marqueeRect: null,
      _metaGen: 0,
      _onLinkKeyDown: null
    }
  },
  mounted() {
    this.bindAxisViewportObserver()
    this.ensureModulePathMap()
    this._onLinkKeyDown = (e) => this.onLinkKeyDown(e)
    window.addEventListener('keydown', this._onLinkKeyDown)
  },
  computed: {
    dragGhostStyle() {
      const g = this.dragGhost
      const style = {}
      if (g.width) style.width = g.width + 'px'
      return style
    },
    /**
     * 时间轴上已有落点预览/条位移时不再叠一层跟手幽灵，避免「鼠标画面」与「实际条」错位。
     * 未排期芯片、或按交付物拖到错误分组（禁止）时显示跟手幽灵。
     */
    showFloatingDragGhost() {
      if (!this.dragGhost.visible) return false
      /* 条拖到错误交付物：禁止图标画在原条上；未排期芯片用跟手幽灵 */
      if (this.dropInsertPreview && this.dropInsertPreview.kind === 'forbidden') {
        return !this.dragState
      }
      if (this.dropInsertPreview) return false
      if (this.dragState) return false
      return true
    },
    isModuleGroupBy() {
      return this.groupBy === GANTT_GROUP_BY.MODULE
    },
    isHandlerGroupBy() {
      return this.groupBy === GANTT_GROUP_BY.HANDLER
    },
    canEdit() {
      return checkPermi(['system:defect:edit'])
    },
    projected() {
      return projectGanttRows(this.defectList, this.groupBy, this.modulePathById)
    },
    groups() {
      const list = this.projected.groups || []
      const ordered = list.map((g) => ({
        ...g,
        rows: this.orderGroupRows(g)
      }))
      if (!this.isModuleGroupBy) return ordered
      return ordered.filter((g) => !this.isModuleGroupHiddenByAncestor(g, ordered))
    },
    unscheduled() {
      return this.projected.unscheduled
    },
    unscheduledKeywordTrimmed() {
      return String(this.unscheduledKeyword || '').trim()
    },
    filteredUnscheduled() {
      const list = this.unscheduled || []
      const q = this.unscheduledKeywordTrimmed.toLowerCase()
      if (!q) return list
      const qNum = q.replace(/^#/, '')
      return list.filter((d) => {
        if (!d) return false
        const num = d.projectNum != null ? String(d.projectNum) : ''
        const name = d.defectName != null ? String(d.defectName) : ''
        return (
          num.toLowerCase().includes(qNum) ||
          name.toLowerCase().includes(q) ||
          (`#${num}`).toLowerCase().includes(q)
        )
      })
    },
    scheduledDefects() {
      return this.defectList.filter((d) => d && parsePlanDate(d.planStartTime) && parsePlanDate(d.planEndTime))
    },
    axisRange() {
      return computeAxisRange(this.scheduledDefects, this.scale)
    },
    axisUnits() {
      return buildAxisUnits(this.axisRange.start, this.axisRange.end, this.scale)
    },
    axisMajorBands() {
      return buildAxisMajorBands(this.axisUnits, this.scale)
    },
    colWidth() {
      return GANTT_COL_WIDTH[this.scale] || GANTT_COL_WIDTH.day
    },
    axisWidthPx() {
      return Math.max(this.colWidth, this.axisUnits.length * this.colWidth)
    },
    /** 已排期行内容高度（不含通底空白） */
    axisContentHeight() {
      let h = 0
      for (let i = 0; i < this.groups.length; i++) {
        const g = this.groups[i]
        const collapsed = this.isGroupCollapsed(g.key)
        const rowCount = collapsed ? 0 : (g.rows || []).length
        h += this.groupHeaderHeight + rowCount * this.rowHeight
        if (!collapsed && this.shouldShowLoadTrack(g)) h += this.loadTrackHeight
      }
      const preview = this.dropInsertPreview
      if (preview) {
        if (preview.kind === 'temp-module') {
          h += this.groupHeaderHeight + this.rowHeight
        } else if (
          preview.kind !== 'forbidden' &&
          !this.isGroupCollapsed(preview.groupKey)
        ) {
          h += this.rowHeight
        }
      }
      return h
    },
    /** handlerId → 负荷轨段缓存，避免每组重复扫缺陷 */
    handlerLoadTrackById() {
      if (!this.showLoad || !this.isHandlerGroupBy) return new Map()
      const map = new Map()
      const defects = this.scheduledDefects
      const rangeStart = this.axisRange.start
      const scale = this.scale
      const axisW = this.axisWidthPx
      const groups = this.groups || []
      for (let i = 0; i < groups.length; i++) {
        const g = groups[i]
        if (!g || g.type !== 'handler' || g.handlerId == null) continue
        const id = Number(g.handlerId)
        if (!Number.isFinite(id) || map.has(id)) continue
        map.set(id, buildHandlerLoadTrackSegments(id, defects, rangeStart, scale, 1, axisW))
      }
      return map
    },
    /** 至少铺满可视区，无数据时格子仍通底 */
    axisBodyHeight() {
      return Math.max(this.axisContentHeight, this.axisViewportHeight || 0, 80)
    },
    axisFillHeight() {
      return Math.max(0, this.axisBodyHeight - this.axisContentHeight)
    },
    /** 空态用 100% 铺底，避免「高度=clientHeight + 横向滚动条」挤出纵向滚动条 */
    axisBodyStyle() {
      const style = { width: this.axisWidthPx + 'px' }
      if (!this.groups.length) {
        style.height = '100%'
        style.minHeight = '100%'
      } else {
        style.height = this.axisBodyHeight + 'px'
      }
      return style
    },
    todayLineLeft() {
      const x = dateToOffsetPx(new Date(), this.axisRange.start, this.scale)
      if (x < 0 || x > this.axisWidthPx) return null
      return x
    },
    emptyModuleLabel() {
      return this.$t('defect.gantt.no-module')
    },
    /** 基线条目 defectId → { planStartTime, planEndTime } */
    baselineItemByDefectId() {
      const map = new Map()
      const items = this.planBaseline && Array.isArray(this.planBaseline.items)
        ? this.planBaseline.items
        : []
      for (let i = 0; i < items.length; i++) {
        const it = items[i]
        if (!it || it.defectId == null) continue
        map.set(Number(it.defectId), it)
      }
      return map
    },
    criticalDefectIds() {
      if (!this.showCritical) return new Set()
      return computeCriticalPathIds(this.scheduledDefects)
    },
    /** 每个 defectId 首次出现条的锚点（相对 axis-body） */
    barAnchorByDefectId() {
      const map = new Map()
      let y = 0
      const groups = this.groups || []
      for (let gi = 0; gi < groups.length; gi++) {
        if (this.isTempModuleInsertAt(gi)) {
          y += this.groupHeaderHeight + this.rowHeight
        }
        const g = groups[gi]
        y += this.groupHeaderHeight
        if (this.isGroupCollapsed(g.key)) continue
        const rows = g.rows || []
        for (let ri = 0; ri < rows.length; ri++) {
          if (this.isDropInsertAt(g.key, ri)) y += this.rowHeight
          const row = rows[ri]
          const defect = row && row.defect
          const id = defect && defect.defectId != null ? Number(defect.defectId) : null
          if (id != null && Number.isFinite(id) && !map.has(id)) {
            let geo = barGeometry(defect, this.axisRange.start, this.scale)
            if (
              geo &&
              this.dragState &&
              this.dragState.rowKey === row.rowKey &&
              this.dragState.preview &&
              !(this.dropInsertPreview && this.dropInsertPreview.kind !== 'forbidden')
            ) {
              geo = { x: this.dragState.preview.x, width: this.dragState.preview.width }
            }
            if (geo) {
              map.set(id, {
                x: geo.x,
                width: geo.width,
                y,
                cy: y + this.rowHeight / 2,
                left: geo.x,
                right: geo.x + geo.width
              })
            }
          }
          y += this.rowHeight
        }
        if (this.isDropInsertAt(g.key, rows.length)) y += this.rowHeight
        if (this.shouldShowLoadTrack(g)) y += this.loadTrackHeight
      }
      return map
    },
    visiblePlanLinks() {
      const anchors = this.barAnchorByDefectId
      return (this.planLinks || []).filter((l) => {
        if (!l) return false
        const from = Number(l.fromDefectId)
        const to = Number(l.toDefectId)
        return anchors.has(from) && anchors.has(to)
      })
    },
    depLinkPaths() {
      const anchors = this.barAnchorByDefectId
      const critical = this.criticalDefectIds
      const out = []
      const links = this.visiblePlanLinks
      for (let i = 0; i < links.length; i++) {
        const link = links[i]
        const from = anchors.get(Number(link.fromDefectId))
        const to = anchors.get(Number(link.toDefectId))
        if (!from || !to) continue
        const d = this.buildDepPathD(from.right, from.cy, to.left, to.cy)
        const fid = Number(link.fromDefectId)
        const tid = Number(link.toDefectId)
        out.push({
          linkId: link.linkId != null ? link.linkId : `${fid}-${tid}`,
          fromDefectId: fid,
          toDefectId: tid,
          d,
          critical: critical.has(fid) && critical.has(tid)
        })
      }
      return out
    },
    linkDragPreviewPath() {
      const drag = this.linkDrag
      if (!drag) return null
      let x2 = drag.curX
      let y2 = drag.curY
      if (drag.targetDefectId != null && drag.targetSide && drag.valid) {
        const anchor = this.barAnchorByDefectId.get(Number(drag.targetDefectId))
        if (anchor) {
          x2 = drag.targetSide === 'head' ? anchor.left : anchor.right
          y2 = anchor.cy
        }
      }
      return this.buildDepPathD(drag.startX, drag.startY, x2, y2)
    },
    marqueeRectStyle() {
      const r = this.marqueeRect
      if (!r) return null
      return {
        left: r.left + 'px',
        top: r.top + 'px',
        width: r.width + 'px',
        height: r.height + 'px'
      }
    }
  },
  watch: {
    showDeps(val) {
      if (!val) {
        this.selectedLinkId = null
        this.teardownLinkDragListeners()
        this.linkDrag = null
      }
      this.persistGanttLayers()
    },
    showCritical() {
      this.persistGanttLayers()
    },
    showLoad() {
      this.persistGanttLayers()
    },
    showBaseline() {
      this.persistGanttLayers()
    }
  },
  beforeUnmount() {
    this.teardownDragListeners()
    this.teardownResizeLeft()
    this.teardownResizeUnscheduled()
    this.teardownAxisViewportObserver()
    this.clearDragGhost()
    this.teardownUnscheduledDragListeners()
    this.teardownMarqueeListeners()
    this.teardownLinkDragListeners()
    this.cancelDragRafs()
    if (this._onLinkKeyDown) {
      window.removeEventListener('keydown', this._onLinkKeyDown)
      this._onLinkKeyDown = null
    }
    if (this._emptyDragImage && this._emptyDragImage.parentNode) {
      this._emptyDragImage.parentNode.removeChild(this._emptyDragImage)
    }
    this._emptyDragImage = null
  },
  methods: {
    init() {
      /* 数据由父组件 search(queryParams) 拉取 */
    },
    /**
     * 柔和 S 曲线：出端/入端留短 stub，箭头前再缩一点，避免直角折线与实心三角贴死条边。
     */
    buildDepPathD(x1, y1, x2, y2) {
      const sx = x1 + 1
      const sy = y1
      const ex = x2 - 6
      const ey = y2
      const dx = Math.abs(ex - sx)
      const dy = Math.abs(ey - sy)
      if (ex >= sx + 10) {
        const stub = Math.min(48, Math.max(18, dx * 0.38))
        return `M ${sx} ${sy} C ${sx + stub} ${sy}, ${ex - stub} ${ey}, ${ex} ${ey}`
      }
      /* 回绕或上下叠层：先水平伸出，再柔和落到目标 */
      const out = Math.min(36, Math.max(22, 16 + dy * 0.15))
      const midY = (sy + ey) / 2
      const midX = (sx + ex) / 2
      return `M ${sx} ${sy} C ${sx + out} ${sy}, ${sx + out} ${midY}, ${midX} ${midY} S ${ex - out} ${ey}, ${ex} ${ey}`
    },
    /** SVG marker 不继承 path 的 stroke，需按状态换不同 marker */
    depLinkMarkerEnd(p) {
      if (!p) return 'url(#defect-gantt-dep-arrow)'
      if (this.selectedLinkId != null && Number(this.selectedLinkId) === Number(p.linkId)) {
        return 'url(#defect-gantt-dep-arrow-selected)'
      }
      if (this.showCritical && p.critical) {
        return 'url(#defect-gantt-dep-arrow-critical)'
      }
      return 'url(#defect-gantt-dep-arrow)'
    },
    shouldShowLoadTrack(group) {
      return !!(
        this.showLoad &&
        this.isHandlerGroupBy &&
        group &&
        group.type === 'handler' &&
        !this.isGroupCollapsed(group.key)
      )
    },
    loadTrackSegmentsOf(group) {
      if (!this.shouldShowLoadTrack(group) || group.handlerId == null) return []
      return this.handlerLoadTrackById.get(Number(group.handlerId)) || []
    },
    /** 分组垂直带宽：组头 + 行 + 插入预览 + 负荷轨 */
    groupBandHeight(group, opts = {}) {
      if (!group) return 0
      const collapsed = this.isGroupCollapsed(group.key)
      const rows = group.rows || []
      let h = this.groupHeaderHeight + (collapsed ? 0 : rows.length * this.rowHeight)
      if (!collapsed && opts.insertExtra) h += this.rowHeight
      if (!collapsed && this.shouldShowLoadTrack(group)) h += this.loadTrackHeight
      return h
    },
    search(params) {
      const gen = ++this.requestGen
      this.loading = true
      const base = params && typeof params === 'object' ? { ...params } : { ...(this.queryParams || {}) }
      this.queryParams = base
      this.ensureModulePathMap()
      const req = {
        ...base,
        pageNum: 1,
        pageSize: this.pageSize
      }
      listDefect(req)
        .then((response) => {
          if (gen !== this.requestGen) return
          this.defectList = Array.isArray(response.rows) ? response.rows : []
          this.total = Number(response.total) || 0
          this.fetchPlanGanttMeta(gen)
          this.$nextTick(() => {
            this.measureAxisViewport()
            this.scrollToToday(false)
          })
        })
        .catch(() => {
          if (gen !== this.requestGen) return
        })
        .finally(() => {
          if (gen === this.requestGen) this.loading = false
        })
    },
    /** search 成功后拉取依赖边与活跃基线 */
    fetchPlanGanttMeta(requestGen) {
      const pid = Number(
        this.projectId != null
          ? this.projectId
          : (this.queryParams && this.queryParams.projectId)
      )
      if (!pid) {
        this.planLinks = []
        this.planBaseline = null
        return
      }
      const ids = (this.defectList || [])
        .map((d) => (d && d.defectId != null ? Number(d.defectId) : null))
        .filter((id) => id != null && Number.isFinite(id))
      const metaGen = ++this._metaGen
      getDefectPlanGanttMeta(pid, ids)
        .then((res) => {
          if (requestGen != null && requestGen !== this.requestGen) return
          if (metaGen !== this._metaGen) return
          const data = (res && res.data) || {}
          this.planLinks = Array.isArray(data.links) ? data.links : []
          this.planBaseline = data.baseline || null
        })
        .catch(() => {
          if (metaGen !== this._metaGen) return
          this.planLinks = []
          this.planBaseline = null
        })
    },
    /** 拉取项目交付物全路径，供分组树序与缩进 */
    ensureModulePathMap() {
      const pid = Number(
        this.projectId != null
          ? this.projectId
          : (this.queryParams && this.queryParams.projectId)
      )
      if (!pid) {
        this.modulePathById = {}
        this._modulePathProjectId = null
        return
      }
      if (this._modulePathProjectId === pid && this.modulePathById && Object.keys(this.modulePathById).length) {
        return
      }
      const gen = ++this._modulePathGen
      this._modulePathProjectId = pid
      listModulePath(pid)
        .then((res) => {
          if (gen !== this._modulePathGen) return
          const map = {}
          ;(res.data || []).forEach((m) => {
            if (m && m.moduleId != null && m.modulePath) {
              const path = String(m.modulePath).trim()
              map[m.moduleId] = path
              map[String(m.moduleId)] = path
            }
          })
          this.modulePathById = map
        })
        .catch(() => {
          if (gen !== this._modulePathGen) return
          this.modulePathById = {}
        })
    },
    groupHeaderStyle(group) {
      const depth = group && group.type === 'module' ? Number(group.depth) || 0 : 0
      return {
        height: this.groupHeaderHeight + 'px',
        '--gantt-group-depth': String(Math.max(0, depth))
      }
    },
    groupHeaderTitle(group) {
      if (this.isGroupCollapsed(group && group.key)) {
        return this.$t('defect.gantt.expand-group')
      }
      return this.$t('defect.gantt.collapse-group')
    },
    /**
     * 祖先交付物组收起时，隐藏其子组（含占位祖先下的子孙）。
     */
    isModuleGroupHiddenByAncestor(group, allGroups) {
      if (!group || group.type !== 'module' || !group.modulePath) return false
      const parts = splitModulePath(group.modulePath)
      if (parts.length <= 1) return false
      const list = Array.isArray(allGroups) ? allGroups : []
      const pathToKey = {}
      for (let i = 0; i < list.length; i++) {
        const g = list[i]
        if (g && g.modulePath) pathToKey[g.modulePath] = g.key
      }
      for (let d = 1; d < parts.length; d++) {
        const ancestorPath = parts.slice(0, d).join('/')
        const key = pathToKey[ancestorPath]
        if (key && this.isGroupCollapsed(key)) return true
      }
      return false
    },
    setDragComponentSize() {
      this.measureAxisViewport()
    },
    bindAxisViewportObserver() {
      this.teardownAxisViewportObserver()
      const el = this.$refs.axisBodyScroll
      if (!el) {
        this.$nextTick(() => this.bindAxisViewportObserver())
        return
      }
      this.measureAxisViewport()
      if (typeof ResizeObserver === 'undefined') return
      this._axisViewportRo = new ResizeObserver(() => {
        this.measureAxisViewport()
      })
      this._axisViewportRo.observe(el)
    },
    teardownAxisViewportObserver() {
      if (this._axisViewportRo) {
        try { this._axisViewportRo.disconnect() } catch (_) { /* ignore */ }
        this._axisViewportRo = null
      }
    },
    measureAxisViewport() {
      const el = this.$refs.axisBodyScroll
      if (!el) return
      const h = el.clientHeight || 0
      if (h !== this.axisViewportHeight) this.axisViewportHeight = h
    },
    stateClass(defect) {
      return defectStateClass(defect)
    },
    isDropInsertAt(groupKey, insertIndex) {
      const p = this.dropInsertPreview
      if (!p || !p.defect || p.kind === 'temp-module' || p.kind === 'forbidden') return false
      return p.groupKey === groupKey && p.insertIndex === insertIndex
    },
    isTempModuleInsertAt(betweenIndex) {
      const p = this.dropInsertPreview
      return !!(p && p.kind === 'temp-module' && p.betweenIndex === betweenIndex && p.defect)
    },
    isBarModuleForbidden(row) {
      return !!(
        row &&
        this.dragState &&
        this.dragState.rowKey === row.rowKey &&
        this.dropInsertPreview &&
        this.dropInsertPreview.kind === 'forbidden'
      )
    },
    normalizeModuleId(moduleId) {
      if (moduleId == null || moduleId === '' || moduleId === '__none__') return '__none__'
      return String(moduleId)
    },
    moduleGroupKeyOf(defect) {
      if (!defect) return 'm:__none__'
      return `m:${this.normalizeModuleId(defect.moduleId)}`
    },
    moduleLabelOf(defect) {
      if (!defect) return this.emptyModuleLabel
      if (this.normalizeModuleId(defect.moduleId) === '__none__') return this.emptyModuleLabel
      const id = defect.moduleId
      const fromMap =
        id != null && this.modulePathById
          ? (this.modulePathById[id] || this.modulePathById[String(id)])
          : ''
      return fromMap || defect.modulePath || defect.moduleName || String(defect.moduleId)
    },
    findModuleGroupOf(defect) {
      const key = this.moduleGroupKeyOf(defect)
      return this.groups.find((g) => g.key === key) || null
    },
    isSameModuleGroup(defect, group) {
      if (!group || group.type !== 'module') return false
      return group.key === this.moduleGroupKeyOf(defect)
    },
    isNoneModuleDefect(defect) {
      return this.normalizeModuleId(defect && defect.moduleId) === '__none__'
    },
    /**
     * 用真实 DOM 命中分组，避免插入预览撑高后虚拟坐标漂移，误判「无交付物」等合法组为禁止。
     */
    findGroupByClientY(clientY) {
      const root = this.$refs.axisBodyScroll
      if (!root || typeof root.querySelectorAll !== 'function') return null
      const els = root.querySelectorAll('[data-group-key]')
      for (let i = 0; i < els.length; i++) {
        const el = els[i]
        const key = el.getAttribute('data-group-key')
        if (!key || key === '__temp_module__') continue
        const rect = el.getBoundingClientRect()
        if (clientY >= rect.top && clientY <= rect.bottom) {
          return this.groups.find((g) => g.key === key) || null
        }
      }
      return null
    },
    resolveGroupByKey(groupKey) {
      if (!groupKey) return null
      return this.groups.find((g) => g.key === groupKey) || null
    },
    /** 组之间插入临时交付物组的下标（0..groups.length） */
    findBetweenGroupIndex(clientY) {
      const body = this.$refs.axisBodyScroll
      if (!body) return 0
      const rect = body.getBoundingClientRect()
      let y = clientY - rect.top + body.scrollTop
      const preview = this.dropInsertPreview
      let acc = 0
      for (let i = 0; i < this.groups.length; i++) {
        if (preview && preview.kind === 'temp-module' && preview.betweenIndex === i) {
          acc += this.groupHeaderHeight + this.rowHeight
        }
        const g = this.groups[i]
        const collapsed = this.isGroupCollapsed(g.key)
        const insertExtra =
          !!(preview && preview.kind === 'insert' && preview.groupKey === g.key && !collapsed)
        const band = this.groupBandHeight(g, { insertExtra })
        if (y < acc + band / 2) return i
        acc += band
      }
      return this.groups.length
    },
    setDropInsertPreview(next) {
      const prev = this.dropInsertPreview
      if (!prev && !next) return
      if (
        prev &&
        next &&
        prev.kind === next.kind &&
        prev.groupKey === next.groupKey &&
        prev.insertIndex === next.insertIndex &&
        prev.betweenIndex === next.betweenIndex &&
        prev.barX === next.barX &&
        prev.barWidth === next.barWidth &&
        prev.groupLabel === next.groupLabel &&
        prev.defect &&
        next.defect &&
        prev.defect.defectId === next.defect.defectId
      ) {
        return
      }
      if (next && next.kind === 'insert' && next.groupKey) {
        this.ensureGroupExpanded(next.groupKey)
      }
      this.dropInsertPreview = next
      const forbidden = !!(next && next.kind === 'forbidden')
      this.setDraggingForbidden(forbidden)
      if (this.dragGhost && this.dragGhost.visible) {
        if (!!this.dragGhost.forbidden !== forbidden) {
          this.dragGhost = { ...this.dragGhost, forbidden }
          this.$nextTick(() => this.paintDragGhost())
        }
      }
    },
    orderGroupRows(group) {
      if (!group || !Array.isArray(group.rows)) return []
      const order = this.groupRowOrder && this.groupRowOrder[group.key]
      if (!Array.isArray(order) || !order.length) return group.rows
      const map = new Map()
      for (let i = 0; i < group.rows.length; i++) {
        const row = group.rows[i]
        const id = row && row.defect ? row.defect.defectId : null
        if (id != null) map.set(Number(id), row)
      }
      const ordered = []
      for (let i = 0; i < order.length; i++) {
        const id = Number(order[i])
        if (!map.has(id)) continue
        ordered.push(map.get(id))
        map.delete(id)
      }
      map.forEach((row) => ordered.push(row))
      return ordered
    },
    persistGroupRowOrder() {
      if (this.$cache && this.$cache.local) {
        try {
          this.$cache.local.set(GANTT_CACHE.ROW_ORDER, JSON.stringify(this.groupRowOrder || {}))
        } catch (_) { /* ignore */ }
      }
    },
    isGroupCollapsed(groupKey) {
      if (groupKey == null || groupKey === '') return false
      return !!(this.collapsedGroups && this.collapsedGroups[String(groupKey)])
    },
    /** 拖动落点需要展示行效果时，自动展开收起的分组 */
    ensureGroupExpanded(groupKey) {
      if (groupKey == null || groupKey === '') return false
      const key = String(groupKey)
      if (key === '__tail__' || key === '__new__' || key === '__temp_module__') return false
      if (!this.isGroupCollapsed(key)) return false
      const next = { ...(this.collapsedGroups || {}) }
      delete next[key]
      this.collapsedGroups = next
      this.persistCollapsedGroups()
      return true
    },
    toggleGroupCollapsed(groupKey) {
      if (groupKey == null || groupKey === '') return
      const key = String(groupKey)
      const next = { ...(this.collapsedGroups || {}) }
      if (next[key]) delete next[key]
      else next[key] = true
      this.collapsedGroups = next
      this.persistCollapsedGroups()
      if (this.dropInsertPreview && this.dropInsertPreview.groupKey === key && next[key]) {
        this.setDropInsertPreview(null)
      }
    },
    persistCollapsedGroups() {
      if (this.$cache && this.$cache.local) {
        try {
          const keys = Object.keys(this.collapsedGroups || {}).filter((k) => this.collapsedGroups[k])
          this.$cache.local.set(GANTT_CACHE.COLLAPSED, JSON.stringify(keys))
        } catch (_) { /* ignore */ }
      }
    },
    toggleUnscheduledCollapsed() {
      this.unscheduledCollapsed = !this.unscheduledCollapsed
      if (this.$cache && this.$cache.local) {
        this.$cache.local.set(
          GANTT_CACHE.UNSCHEDULED_COLLAPSED,
          this.unscheduledCollapsed ? 'true' : 'false'
        )
      }
      this.$nextTick(() => {
        this.measureAxisViewport()
      })
    },
    startResizeUnscheduled(e) {
      if (this.unscheduledCollapsed) return
      this.resizeUnscheduled = {
        startY: e.clientY,
        startHeight: this.unscheduledHeight
      }
      document.body.classList.add('defect-gantt-resizing-unscheduled')
      document.addEventListener('mousemove', this.onResizeUnscheduledMove)
      document.addEventListener('mouseup', this.onResizeUnscheduledUp)
    },
    /**
     * 将 defectId 插入到分组指定下标（0..n）；排除自身后按 insertIndex 落位。
     */
    applyGroupRowOrder(groupKey, defectId, insertIndex) {
      if (!groupKey || groupKey === '__tail__' || groupKey === '__new__' || defectId == null) return
      const g = this.groups.find((x) => x.key === groupKey)
      if (!g) return
      const id = Number(defectId)
      const others = g.rows
        .map((r) => (r && r.defect ? Number(r.defect.defectId) : null))
        .filter((x) => x != null && x !== id)
      let idx = Number(insertIndex)
      if (!Number.isFinite(idx) || idx < 0) idx = others.length
      if (idx > others.length) idx = others.length
      const next = others.slice()
      next.splice(idx, 0, id)
      const updated = { ...this.groupRowOrder, [groupKey]: next }
      /* 从其它分组序中去掉该缺陷，避免残留 */
      Object.keys(updated).forEach((k) => {
        if (k === groupKey || !Array.isArray(updated[k])) return
        updated[k] = updated[k].filter((x) => Number(x) !== id)
      })
      this.groupRowOrder = updated
      this.persistGroupRowOrder()
    },
    mirrorTip(row) {
      const names = (row.otherHandlers || []).map((h) => h.nickName).filter(Boolean)
      return this.$t('defect.gantt.mirror-tip', { names: names.join(', ') })
    },
    formatDefectDays(defect) {
      return formatDurationDays(planDurationDays(defect))
    },
    groupTotalDays(group) {
      const rows = (group && group.rows) || []
      let sum = 0
      let any = false
      for (let i = 0; i < rows.length; i++) {
        const d = planDurationDays(rows[i] && rows[i].defect)
        if (d == null || !Number.isFinite(d)) continue
        sum += d
        any = true
      }
      return any ? sum : null
    },
    formatGroupDays(group) {
      return formatDurationDays(this.groupTotalDays(group))
    },
    barStyle(row) {
      const geo = barGeometry(row.defect, this.axisRange.start, this.scale)
      if (!geo) return null
      let x = geo.x
      let width = geo.width
      const ds = this.dragState
      const dragging = ds && ds.rowKey === row.rowKey
      const defectId = row.defect && row.defect.defectId != null ? Number(row.defect.defectId) : null
      const batchPeer =
        !dragging &&
        ds &&
        ds.activated &&
        ds.mode === 'move' &&
        ds.preview &&
        defectId != null &&
        Array.isArray(ds.batchIds) &&
        ds.batchIds.some((id) => Number(id) === defectId)
      /* 已有插入预览行时原条留在原位半透，位移只画在落点行上（禁止态仍跟手改期） */
      if (
        dragging &&
        this.dropInsertPreview &&
        this.dropInsertPreview.kind !== 'forbidden'
      ) {
        return {
          left: geo.x + 'px',
          width: geo.width + 'px',
          opacity: 0.28
        }
      }
      if (dragging && ds.preview) {
        x = ds.preview.x
        width = ds.preview.width
      } else if (batchPeer) {
        x = geo.x + (ds.preview.x - ds.originX)
      }
      return {
        left: x + 'px',
        width: width + 'px'
      }
    },
    baselineBarStyle(row) {
      if (!row || !row.defect || row.defect.defectId == null) return null
      const item = this.baselineItemByDefectId.get(Number(row.defect.defectId))
      if (!item) return null
      const pseudo = {
        planStartTime: item.planStartTime,
        planEndTime: item.planEndTime
      }
      const geo = barGeometry(pseudo, this.axisRange.start, this.scale)
      if (!geo) return null
      return {
        left: geo.x + 'px',
        width: geo.width + 'px'
      }
    },
    isCriticalDefect(defect) {
      if (!defect || defect.defectId == null) return false
      return this.criticalDefectIds.has(Number(defect.defectId))
    },
    defectNumOf(defectId) {
      const id = Number(defectId)
      const d = (this.defectList || []).find((x) => x && Number(x.defectId) === id)
      return d && d.projectNum != null ? d.projectNum : id
    },
    formatMajorLabel(band) {
      if (!band || !band.label) return ''
      if (this.scale === GANTT_SCALE.MONTH) {
        return this.$t('defect.gantt.axis-year', { y: band.label })
      }
      const d = band.start
      if (!(d instanceof Date)) return band.label
      return this.$t('defect.gantt.axis-year-month', {
        y: d.getFullYear(),
        m: d.getMonth() + 1
      })
    },
    formatUnitLabel(u) {
      const d = u.start
      if (!(d instanceof Date)) return ''
      if (this.scale === GANTT_SCALE.MONTH) {
        return String(d.getMonth() + 1)
      }
      if (this.scale === GANTT_SCALE.WEEK) {
        return this.formatWeekUnitLabel(u)
      }
      return String(d.getDate())
    },
    /** 日尺度：周几短文案（日一二…） */
    formatWeekdayShort(u) {
      const d = u && u.start
      if (!(d instanceof Date)) return ''
      const keys = [
        'defect.gantt.weekday-sun',
        'defect.gantt.weekday-mon',
        'defect.gantt.weekday-tue',
        'defect.gantt.weekday-wed',
        'defect.gantt.weekday-thu',
        'defect.gantt.weekday-fri',
        'defect.gantt.weekday-sat'
      ]
      return this.$t(keys[d.getDay()] || keys[0])
    },
    formatWeekUnitLabel(u) {
      const d = u && u.start
      if (!(d instanceof Date)) return ''
      const w = isoWeekNumber(d)
      if (w == null) return ''
      return this.$t('defect.gantt.axis-week', { w })
    },
    formatWeekUnitRange(u) {
      const d = u && u.start
      if (!(d instanceof Date)) return ''
      const end = u.end instanceof Date ? new Date(u.end.getTime() - 86400000) : d
      const m1 = d.getMonth() + 1
      const d1 = d.getDate()
      const m2 = end.getMonth() + 1
      const d2 = end.getDate()
      return this.$t('defect.gantt.axis-week-range', { m1, d1, m2, d2 })
    },
    formatUnitTitle(u) {
      const d = u.start
      if (!(d instanceof Date)) return ''
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      if (this.scale === GANTT_SCALE.MONTH) return `${y}-${m}`
      if (this.scale === GANTT_SCALE.WEEK) {
        const w = isoWeekNumber(d)
        const range = this.formatWeekUnitRange(u)
        return w != null ? `${y} ${this.$t('defect.gantt.axis-week', { w })} (${range})` : `${y}-${m}-${day}`
      }
      return `${y}-${m}-${day} ${this.formatWeekdayShort(u)}`
    },
    persistGanttLayers() {
      if (!(this.$cache && this.$cache.local)) return
      try {
        this.$cache.local.set(
          GANTT_CACHE.LAYERS,
          JSON.stringify({
            deps: !!this.showDeps,
            critical: !!this.showCritical,
            load: !!this.showLoad,
            baseline: !!this.showBaseline
          })
        )
      } catch (_) { /* ignore */ }
    },
    onGroupByChange(val) {
      this.groupBy = val
      if (this.$cache && this.$cache.local) {
        this.$cache.local.set(GANTT_CACHE.GROUP_BY, val)
      }
      if (val === GANTT_GROUP_BY.MODULE) {
        this.ensureModulePathMap()
      }
    },
    onScaleChange(val) {
      this.scale = val
      if (this.$cache && this.$cache.local) {
        this.$cache.local.set(GANTT_CACHE.SCALE, val)
      }
      this.$nextTick(() => this.scrollToToday(false))
    },
    scrollToToday(smooth = true) {
      const body = this.$refs.axisBodyScroll
      const header = this.$refs.axisHeaderScroll
      if (!body) return
      const x = dateToOffsetPx(new Date(), this.axisRange.start, this.scale)
      const target = Math.max(0, x - body.clientWidth / 3)
      const behavior = smooth ? 'smooth' : 'auto'
      body.scrollTo({ left: target, behavior })
      if (header) header.scrollLeft = target
    },
    onLeftScroll() {
      if (this.syncingScroll) return
      this.syncingScroll = true
      const left = this.$refs.leftScroll
      const axis = this.$refs.axisBodyScroll
      if (left && axis) axis.scrollTop = left.scrollTop
      this.syncingScroll = false
    },
    onAxisBodyScroll() {
      if (this.syncingScroll) return
      this.syncingScroll = true
      const left = this.$refs.leftScroll
      const axis = this.$refs.axisBodyScroll
      const header = this.$refs.axisHeaderScroll
      if (left && axis) left.scrollTop = axis.scrollTop
      if (header && axis) header.scrollLeft = axis.scrollLeft
      this.syncingScroll = false
    },
    onAxisHeaderScroll() {
      if (this.syncingScroll) return
      this.syncingScroll = true
      const axis = this.$refs.axisBodyScroll
      const header = this.$refs.axisHeaderScroll
      if (header && axis) axis.scrollLeft = header.scrollLeft
      this.syncingScroll = false
    },
    startResizeLeft(e) {
      this.resizeLeft = { startX: e.clientX, startWidth: this.leftWidth }
      document.addEventListener('mousemove', this.onResizeLeftMove)
      document.addEventListener('mouseup', this.onResizeLeftUp)
    },
    onResizeLeftMove(e) {
      if (!this.resizeLeft) return
      const next = this.resizeLeft.startWidth + (e.clientX - this.resizeLeft.startX)
      this.leftWidth = Math.min(GANTT_LEFT_WIDTH_MAX, Math.max(GANTT_LEFT_WIDTH_MIN, next))
    },
    onResizeLeftUp() {
      this.teardownResizeLeft()
      if (this.$cache && this.$cache.local) {
        this.$cache.local.set(GANTT_CACHE.LEFT_WIDTH, String(this.leftWidth))
      }
    },
    teardownResizeLeft() {
      this.resizeLeft = null
      document.removeEventListener('mousemove', this.onResizeLeftMove)
      document.removeEventListener('mouseup', this.onResizeLeftUp)
    },
    onResizeUnscheduledMove(e) {
      if (!this.resizeUnscheduled) return
      /* 向上拖增大未排期高度 */
      const next =
        this.resizeUnscheduled.startHeight - (e.clientY - this.resizeUnscheduled.startY)
      this.unscheduledHeight = Math.min(
        GANTT_UNSCHEDULED_HEIGHT_MAX,
        Math.max(GANTT_UNSCHEDULED_HEIGHT_MIN, next)
      )
    },
    onResizeUnscheduledUp() {
      this.teardownResizeUnscheduled()
      if (this.$cache && this.$cache.local) {
        this.$cache.local.set(GANTT_CACHE.UNSCHEDULED_HEIGHT, String(this.unscheduledHeight))
      }
      this.$nextTick(() => {
        this.measureAxisViewport()
      })
    },
    teardownResizeUnscheduled() {
      this.resizeUnscheduled = null
      if (typeof document !== 'undefined' && document.body) {
        document.body.classList.remove('defect-gantt-resizing-unscheduled')
      }
      document.removeEventListener('mousemove', this.onResizeUnscheduledMove)
      document.removeEventListener('mouseup', this.onResizeUnscheduledUp)
    },
    isSelectedDefect(defect) {
      if (!defect || defect.defectId == null) return false
      const id = Number(defect.defectId)
      if ((this.marqueeSelectedIds || []).some((x) => Number(x) === id)) return true
      return this.selectedDefectId != null && Number(this.selectedDefectId) === id
    },
    selectDefect(defect, opts = {}) {
      if (!defect || defect.defectId == null) return
      const id = Number(defect.defectId)
      if (opts.additive) {
        const set = new Set((this.marqueeSelectedIds || []).map(Number))
        if (set.has(id)) set.delete(id)
        else set.add(id)
        this.marqueeSelectedIds = Array.from(set)
        this.selectedDefectId = set.has(id) ? id : (set.size ? Array.from(set)[set.size - 1] : null)
        return
      }
      this.selectedDefectId = defect.defectId
      this.marqueeSelectedIds = [id]
    },
    clearDefectSelection() {
      this.selectedDefectId = null
      this.marqueeSelectedIds = []
    },
    openDefect(defect) {
      if (defect && defect.defectId) {
        this.selectDefect(defect)
        this.$emit('defect-click', defect)
      }
    },
    /**
     * 左侧行：选中并滚动定位到时间轴上的条；不打开详情。
     */
    focusGanttRow(row, group) {
      if (!row || !row.defect) return
      this.selectDefect(row.defect)
      const key = group && group.key
      if (key != null && key !== '' && this.isGroupCollapsed(key)) {
        const next = { ...(this.collapsedGroups || {}) }
        delete next[String(key)]
        this.collapsedGroups = next
        this.persistCollapsedGroups()
      }
      this.$nextTick(() => {
        this.scrollToGanttBar(row)
      })
    },
    scrollToGanttBar(row) {
      if (!row || !row.defect) return
      const body = this.$refs.axisBodyScroll
      const header = this.$refs.axisHeaderScroll
      const left = this.$refs.leftScroll
      const geo = barGeometry(row.defect, this.axisRange.start, this.scale)
      if (body && geo) {
        const viewLeft = body.scrollLeft
        const viewRight = viewLeft + body.clientWidth
        const barLeft = geo.x
        const barRight = geo.x + Math.max(geo.width, this.colWidth)
        let nextLeft = viewLeft
        if (barRight < viewLeft || barLeft > viewRight) {
          nextLeft = Math.max(0, barLeft - body.clientWidth / 3)
        } else if (barLeft < viewLeft) {
          nextLeft = Math.max(0, barLeft - 40)
        } else if (barRight > viewRight) {
          nextLeft = Math.max(0, barRight - body.clientWidth + 40)
        }
        if (nextLeft !== viewLeft) {
          body.scrollTo({ left: nextLeft, behavior: 'smooth' })
          if (header) header.scrollLeft = nextLeft
        }
      }
      if (left && row.rowKey != null) {
        const el = left.querySelector(`[data-row-key="${String(row.rowKey).replace(/"/g, '\\"')}"]`)
        if (el && typeof el.scrollIntoView === 'function') {
          el.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
          this.$nextTick(() => {
            const axis = this.$refs.axisBodyScroll
            if (axis) axis.scrollTop = left.scrollTop
          })
        }
      }
    },
    /**
     * 条点击开详情；可编辑时 mousedown 会 preventDefault，改由 mouseup 未拖动时打开。
     */
    onBarClick(e, defect) {
      if (this.canEdit) return
      this.openDefect(defect)
    },
    ensureCanEdit() {
      if (this.canEdit) return true
      this.$modal.msgWarning(this.$t('defect.gantt.no-edit-perm'))
      return false
    },
    /**
     * @param {number} [grabOffsetX] 指针相对元素左上角 X；缺省则锚在指针下
     * @param {number} [grabOffsetY] 指针相对元素左上角 Y
     */
    showDragGhost(defect, clientX, clientY, kind, width, grabOffsetX, grabOffsetY) {
      if (!defect) return
      this._dragGhostOffsetX = Number.isFinite(grabOffsetX) ? grabOffsetX : 0
      this._dragGhostOffsetY = Number.isFinite(grabOffsetY) ? grabOffsetY : 0
      this._ghostClientX = clientX
      this._ghostClientY = clientY
      this.dragGhost = {
        visible: true,
        kind: kind || 'chip',
        projectNum: defect.projectNum != null ? defect.projectNum : '',
        name: defect.defectName || '',
        stateClass: this.stateClass(defect),
        forbidden: !!(this.dropInsertPreview && this.dropInsertPreview.kind === 'forbidden'),
        x: 0,
        y: 0,
        width: width || 160
      }
      this.$nextTick(() => this.paintDragGhost())
    },
    moveDragGhost(clientX, clientY) {
      if (!this.dragGhost.visible) return
      this._ghostClientX = clientX
      this._ghostClientY = clientY
      if (this._ghostRaf) return
      this._ghostRaf = requestAnimationFrame(() => {
        this._ghostRaf = 0
        this.paintDragGhost()
      })
    },
    paintDragGhost() {
      const el = this.$refs.dragGhostEl
      if (!el || !el.style) return
      const x = this._ghostClientX - this._dragGhostOffsetX
      const y = this._ghostClientY - this._dragGhostOffsetY
      el.style.transform = `translate3d(${x}px, ${y}px, 0)`
    },
    scheduleDropInsertPreview(clientX, clientY) {
      this._previewClientX = clientX
      this._previewClientY = clientY
      if (this._previewRaf) return
      this._previewRaf = requestAnimationFrame(() => {
        this._previewRaf = 0
        this.updateDropInsertPreview(this._previewClientX, this._previewClientY)
      })
    },
    cancelDragRafs() {
      if (this._ghostRaf) {
        cancelAnimationFrame(this._ghostRaf)
        this._ghostRaf = 0
      }
      if (this._previewRaf) {
        cancelAnimationFrame(this._previewRaf)
        this._previewRaf = 0
      }
      if (this._barMoveRaf) {
        cancelAnimationFrame(this._barMoveRaf)
        this._barMoveRaf = 0
      }
      this._barMoveEvent = null
    },
    clearDragGhost() {
      this.cancelDragRafs()
      if (this.dragGhost.visible) {
        this.dragGhost = { ...this.dragGhost, visible: false, forbidden: false }
      }
      this.setDropInsertPreview(null)
      this.setDraggingCursor(false)
      this.setDraggingForbidden(false)
    },
    /** 拖动时强制抓取光标，避免浏览器 HTML5 拖拽出现「地球」链光标 */
    setDraggingCursor(on) {
      if (typeof document === 'undefined' || !document.body) return
      document.body.classList.toggle('defect-gantt-is-dragging', !!on)
      if (!on) document.body.classList.remove('defect-gantt-is-dragging-forbidden')
    },
    setDraggingForbidden(on) {
      if (typeof document === 'undefined' || !document.body) return
      document.body.classList.toggle('defect-gantt-is-dragging-forbidden', !!on)
    },
    /**
     * 拖动未松手时，在落点分组下插入预览行（带动画）。
     */
    updateDropInsertPreview(clientX, clientY) {
      /* 左右拉宽只改原条，不出临时插入行 */
      if (this.dragState && (this.dragState.mode === 'start' || this.dragState.mode === 'end')) {
        this.setDropInsertPreview(null)
        return
      }
      const defect =
        this.pendingUnscheduled ||
        (this.dragState && this.dragState.defect) ||
        null
      if (!defect) {
        this.setDropInsertPreview(null)
        return
      }
      const body = this.$refs.axisBodyScroll
      if (!body) {
        this.setDropInsertPreview(null)
        return
      }
      const rect = body.getBoundingClientRect()
      const overAxis =
        clientX >= rect.left &&
        clientX <= rect.right &&
        clientY >= rect.top &&
        clientY <= rect.bottom
      if (!overAxis) {
        this.setDropInsertPreview(null)
        return
      }
      const day = this.clientXToDate(clientX)
      if (!day) {
        this.setDropInsertPreview(null)
        return
      }
      let planStartTime
      let planEndTime
      if (this.dragState && this.dragState.preview && this.dragState.mode === 'move') {
        const shifted = shiftPlanRange(
          this.dragState.planStartTime,
          this.dragState.planEndTime,
          this.scaleDayDelta(Math.round((this.dragState.preview.x - this.dragState.originX) / this.colWidth))
        )
        if (shifted) {
          planStartTime = shifted.planStartTime
          planEndTime = shifted.planEndTime
        }
      } else if (this.dragState && (this.dragState.mode === 'start' || this.dragState.mode === 'end')) {
        planStartTime = this.dragState.defect.planStartTime
        planEndTime = this.dragState.defect.planEndTime
        const edge = this.dragState.mode
        if (edge === 'start') {
          const resized = resizePlanRange(planStartTime, planEndTime, 'start', day)
          if (resized) {
            planStartTime = resized.planStartTime
            planEndTime = resized.planEndTime
          }
        } else {
          const resized = resizePlanRange(planStartTime, planEndTime, 'end', day)
          if (resized) {
            planStartTime = resized.planStartTime
            planEndTime = resized.planEndTime
          }
        }
      } else {
        const range = defaultPlanRangeFromDropDay(day)
        if (!range) {
          this.setDropInsertPreview(null)
          return
        }
        planStartTime = range.planStartTime
        planEndTime = range.planEndTime
      }
      const previewDefect = {
        ...defect,
        planStartTime,
        planEndTime
      }
      const geo = barGeometry(previewDefect, this.axisRange.start, this.scale)
      if (!geo) {
        this.setDropInsertPreview(null)
        return
      }

      /* 按交付物：只能落入本缺陷所属交付物（含无交付物→无交付物）；没有该组时在组间插临时组+行 */
      if (this.isModuleGroupBy) {
        const ownGroup = this.findModuleGroupOf(defect)
        const slot = this.findDropSlot(clientY, defect.defectId)
        const hoverGroup = this.findGroupByClientY(clientY) || (slot && slot.group) || null
        if (!ownGroup) {
          this.setDropInsertPreview({
            kind: 'temp-module',
            betweenIndex: this.findBetweenGroupIndex(clientY),
            groupKey: '__temp_module__',
            groupLabel: this.moduleLabelOf(defect),
            insertIndex: 0,
            defect: previewDefect,
            barX: geo.x,
            barWidth: geo.width
          })
          return
        }
        if (!hoverGroup || hoverGroup.key !== ownGroup.key) {
          this.setDropInsertPreview({
            kind: 'forbidden',
            groupKey: null,
            insertIndex: -1,
            defect: previewDefect,
            barX: geo.x,
            barWidth: geo.width
          })
          return
        }
        const insertIndex =
          slot && slot.groupKey === ownGroup.key && Number.isFinite(slot.insertIndex)
            ? slot.insertIndex
            : (ownGroup.rows || []).length
        if (
          this.dragState &&
          this.dragState.mode === 'move' &&
          this.dragState.group &&
          ownGroup.key === this.dragState.group.key
        ) {
          const id = Number(defect.defectId)
          const full = (ownGroup.rows || [])
            .map((r) => (r && r.defect ? Number(r.defect.defectId) : null))
            .filter((x) => x != null)
          const curIdx = full.indexOf(id)
          if (curIdx >= 0 && curIdx === insertIndex) {
            this.ensureGroupExpanded(ownGroup.key)
            this.setDropInsertPreview(null)
            return
          }
        }
        this.setDropInsertPreview({
          kind: 'insert',
          groupKey: ownGroup.key,
          group: ownGroup,
          insertIndex,
          defect: previewDefect,
          barX: geo.x,
          barWidth: geo.width
        })
        return
      }

      const slot = this.findDropSlot(clientY, defect.defectId)
      let groupKey = slot && slot.groupKey
      if (!groupKey) {
        groupKey = this.groups.length ? '__tail__' : '__new__'
      }
      const insertIndex = slot && Number.isFinite(slot.insertIndex) ? slot.insertIndex : 0

      /* 同组同下标仅改期：不插临时行，由原条 preview 跟手，避免双画面距离感 */
      if (this.dragState && this.dragState.mode === 'move' && this.dragState.group && groupKey === this.dragState.group.key) {
        const id = Number(defect.defectId)
        const gNow = this.groups.find((x) => x.key === groupKey)
        const full = ((gNow && gNow.rows) || [])
          .map((r) => (r && r.defect ? Number(r.defect.defectId) : null))
          .filter((x) => x != null)
        const curIdx = full.indexOf(id)
        if (curIdx >= 0 && curIdx === insertIndex) {
          this.setDropInsertPreview(null)
          return
        }
      }

      this.setDropInsertPreview({
        kind: 'insert',
        groupKey,
        insertIndex,
        defect: previewDefect,
        barX: geo.x,
        barWidth: geo.width
      })
    },
    /** 挂到 DOM 的 1×1 透明图，供 setDragImage 隐藏浏览器默认残影（地球图标） */
    getEmptyDragImage() {
      if (this._emptyDragImage && this._emptyDragImage.isConnected) {
        return this._emptyDragImage
      }
      const img = document.createElement('img')
      img.src =
        'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7'
      img.alt = ''
      img.setAttribute('aria-hidden', 'true')
      img.width = 1
      img.height = 1
      img.style.cssText =
        'position:fixed;left:-9999px;top:-9999px;width:1px;height:1px;opacity:0;pointer-events:none;'
      if (typeof document !== 'undefined' && document.body) {
        document.body.appendChild(img)
      }
      this._emptyDragImage = img
      return img
    },
    onUnscheduledDragStart(e, defect) {
      if (!this.ensureCanEdit()) {
        e.preventDefault()
        return
      }
      this.pendingUnscheduled = defect
      this.selectDefect(defect)
      this.setDraggingCursor(true)
      /* 所属交付物组若收起，先展开以便拖入时能看到插入行 */
      if (this.isModuleGroupBy) {
        const home = this.findModuleGroupOf(defect)
        if (home) this.ensureGroupExpanded(home.key)
      }
      try {
        /* 专用 MIME，避免浏览器把 text/plain 当成可导航链接而出现地球残影 */
        e.dataTransfer.setData('application/x-cat2bug-defect-id', String(defect.defectId))
        e.dataTransfer.setData('text/plain', String(defect.defectId))
        e.dataTransfer.effectAllowed = 'move'
        if (typeof e.dataTransfer.setDragImage === 'function') {
          e.dataTransfer.setDragImage(this.getEmptyDragImage(), 0, 0)
        }
      } catch (_) { /* ignore */ }
      {
        const el = e.currentTarget
        const rect = el && el.getBoundingClientRect ? el.getBoundingClientRect() : null
        const w = rect ? Math.max(80, Math.min(rect.width, 280)) : 180
        const ox = rect ? e.clientX - rect.left : 0
        const oy = rect ? e.clientY - rect.top : 0
        this.showDragGhost(defect, e.clientX, e.clientY, 'chip', w, ox, oy)
      }
      this.teardownUnscheduledDragListeners()
      document.addEventListener('drag', this.onUnscheduledDragMove)
      document.addEventListener('dragover', this.onUnscheduledDragMoveDoc, true)
    },
    onUnscheduledDragMove(e) {
      if (!this.pendingUnscheduled) return
      if (e.clientX === 0 && e.clientY === 0) return
      this.moveDragGhost(e.clientX, e.clientY)
      this.scheduleDropInsertPreview(e.clientX, e.clientY)
    },
    onUnscheduledDragMoveDoc(e) {
      if (!this.pendingUnscheduled) return
      e.preventDefault()
      if (e.dataTransfer) {
        e.dataTransfer.dropEffect =
          this.dropInsertPreview && this.dropInsertPreview.kind === 'forbidden' ? 'none' : 'move'
      }
      this.moveDragGhost(e.clientX, e.clientY)
      this.scheduleDropInsertPreview(e.clientX, e.clientY)
    },
    teardownUnscheduledDragListeners() {
      document.removeEventListener('drag', this.onUnscheduledDragMove)
      document.removeEventListener('dragover', this.onUnscheduledDragMoveDoc, true)
    },
    onUnscheduledDragEnd() {
      this.pendingUnscheduled = null
      this.teardownUnscheduledDragListeners()
      this.clearDragGhost()
    },
    onAxisDragOver(e) {
      if (!this.pendingUnscheduled) return
      e.dataTransfer.dropEffect =
        this.dropInsertPreview && this.dropInsertPreview.kind === 'forbidden' ? 'none' : 'move'
      this.moveDragGhost(e.clientX, e.clientY)
      this.scheduleDropInsertPreview(e.clientX, e.clientY)
    },
    onGroupDragOver(e, group) {
      if (!this.pendingUnscheduled && !this.dragState) return
      if (this.pendingUnscheduled) {
        e.dataTransfer.dropEffect =
          this.dropInsertPreview && this.dropInsertPreview.kind === 'forbidden' ? 'none' : 'move'
      }
      this._hoverGroupKey = group && group.key
      if (this.pendingUnscheduled) {
        this.moveDragGhost(e.clientX, e.clientY)
        this.scheduleDropInsertPreview(e.clientX, e.clientY)
      }
    },
    clientXToDate(clientX) {
      const body = this.$refs.axisBodyScroll
      if (!body) return null
      const rect = body.getBoundingClientRect()
      const offset = clientX - rect.left + body.scrollLeft
      return offsetPxToDate(offset, this.axisRange.start, this.scale)
    },
    findGroupAtPoint(clientY) {
      const slot = this.findDropSlot(clientY)
      return slot && slot.group ? slot.group : null
    },
    /**
     * 命中分组与插入下标（不计临时预览行高度）。
     * 垂直命中用完整行高（含自身），避免「排除自身后高度缩短 → 一点击就落到下一组」。
     * insertIndex 按去掉自身后的列表下标。
     */
    findDropSlot(clientY, excludeDefectId) {
      const body = this.$refs.axisBodyScroll
      if (!body) return null
      const rect = body.getBoundingClientRect()
      let y = clientY - rect.top + body.scrollTop
      const excludeId = excludeDefectId != null ? Number(excludeDefectId) : null
      const preview = this.dropInsertPreview
      for (let i = 0; i < this.groups.length; i++) {
        if (preview && preview.kind === 'temp-module' && preview.betweenIndex === i) {
          const tempBand = this.groupHeaderHeight + this.rowHeight
          if (y >= 0 && y < tempBand) {
            return { group: null, groupKey: '__temp_module__', insertIndex: 0 }
          }
          y -= tempBand
        }
        const g = this.groups[i]
        const rows = g.rows || []
        const collapsed = this.isGroupCollapsed(g.key)
        const insertExtra =
          !!(preview && preview.kind === 'insert' && preview.groupKey === g.key && !collapsed)
        const band = this.groupBandHeight(g, { insertExtra })
        if (y >= 0 && y < band) {
          /* 折叠组：落到组头即视为插到末尾 */
          if (collapsed) {
            let insertIndex = 0
            for (let j = 0; j < rows.length; j++) {
              const id = rows[j] && rows[j].defect ? Number(rows[j].defect.defectId) : null
              if (excludeId != null && id === excludeId) continue
              if (id != null) insertIndex += 1
            }
            return { group: g, groupKey: g.key, insertIndex }
          }
          const local = y - this.groupHeaderHeight
          const rowsBand = rows.length * this.rowHeight + (insertExtra ? this.rowHeight : 0)
          /* 落在负荷轨上：视为插到组末 */
          if (local >= rowsBand) {
            let insertIndex = 0
            for (let j = 0; j < rows.length; j++) {
              const id = rows[j] && rows[j].defect ? Number(rows[j].defect.defectId) : null
              if (excludeId != null && id === excludeId) continue
              if (id != null) insertIndex += 1
            }
            return { group: g, groupKey: g.key, insertIndex }
          }
          if (local < 0 || rows.length === 0) {
            return { group: g, groupKey: g.key, insertIndex: 0 }
          }
          let hoverRow = Math.floor(local / this.rowHeight)
          if (hoverRow >= rows.length) hoverRow = rows.length - 1
          const inLowerHalf = local - hoverRow * this.rowHeight >= this.rowHeight / 2
          const insertBeforeVisual = inLowerHalf ? hoverRow + 1 : hoverRow
          let insertIndex = 0
          for (let j = 0; j < insertBeforeVisual && j < rows.length; j++) {
            const id = rows[j] && rows[j].defect ? Number(rows[j].defect.defectId) : null
            if (excludeId != null && id === excludeId) continue
            if (id != null) insertIndex += 1
          }
          return { group: g, groupKey: g.key, insertIndex }
        }
        y -= band
      }
      if (preview && preview.kind === 'temp-module' && preview.betweenIndex === this.groups.length) {
        const tempBand = this.groupHeaderHeight + this.rowHeight
        if (y >= 0 && y < tempBand) {
          return { group: null, groupKey: '__temp_module__', insertIndex: 0 }
        }
      }
      if (this.groups.length) {
        const last = this.groups[this.groups.length - 1]
        const rows = last.rows || []
        let insertIndex = 0
        for (let j = 0; j < rows.length; j++) {
          const id = rows[j] && rows[j].defect ? Number(rows[j].defect.defectId) : null
          if (excludeId != null && id === excludeId) continue
          if (id != null) insertIndex += 1
        }
        return { group: last, groupKey: last.key, insertIndex }
      }
      return { group: null, groupKey: '__new__', insertIndex: 0 }
    },
    async dropUnscheduledToAxis(e, fallbackGroup) {
      const defect = this.pendingUnscheduled
      const insertSnap = this.dropInsertPreview
      this.pendingUnscheduled = null
      this.teardownUnscheduledDragListeners()
      this.clearDragGhost()
      if (!defect) return
      if (!this.ensureCanEdit()) return
      const day = this.clientXToDate(e.clientX)
      if (!day) return
      const range = defaultPlanRangeFromDropDay(day)
      if (!range) return
      if (this.isModuleGroupBy) {
        if (insertSnap && insertSnap.kind === 'forbidden') {
          this.$modal.msgWarning(this.$t('defect.gantt.module-drop-forbidden'))
          return
        }
        if (insertSnap && insertSnap.kind === 'temp-module') {
          await this.persistDefectUpdate(defect, range, null)
          const ownKey = this.moduleGroupKeyOf(defect)
          this.$nextTick(() => {
            this.applyGroupRowOrder(ownKey, defect.defectId, 0)
          })
          return
        }
        const slot = insertSnap || this.findDropSlot(e.clientY, defect.defectId)
        const group =
          (insertSnap && insertSnap.group) ||
          this.resolveGroupByKey(insertSnap && insertSnap.groupKey) ||
          (slot && slot.group) ||
          fallbackGroup ||
          this.findGroupByClientY(e.clientY) ||
          this.findGroupAtPoint(e.clientY)
        /* 无交付物缺陷允许落入无交付物组 */
        if (!group || !this.isSameModuleGroup(defect, group)) {
          this.$modal.msgWarning(this.$t('defect.gantt.module-drop-forbidden'))
          return
        }
        await this.persistDefectUpdate(defect, range, group)
        const orderKey = (insertSnap && insertSnap.groupKey) || (slot && slot.groupKey) || group.key
        const orderIndex =
          insertSnap && Number.isFinite(insertSnap.insertIndex)
            ? insertSnap.insertIndex
            : slot
              ? slot.insertIndex
              : (group.rows || []).length
        if (orderKey) {
          this.applyGroupRowOrder(orderKey, defect.defectId, orderIndex)
        }
        return
      }
      const slot = insertSnap || this.findDropSlot(e.clientY, defect.defectId)
      const group =
        (slot && slot.group) ||
        fallbackGroup ||
        this.findGroupAtPoint(e.clientY)
      await this.persistDefectUpdate(defect, range, group)
      if (slot && slot.groupKey) {
        this.applyGroupRowOrder(slot.groupKey, defect.defectId, slot.insertIndex)
      }
    },
    async onAxisDrop(e) {
      await this.dropUnscheduledToAxis(e, null)
    },
    async onGroupDrop(e, group) {
      await this.dropUnscheduledToAxis(e, group)
    },
    onAxisBlankDown(e) {
      if (!e || e.button !== 0) return
      const target = e.target
      if (!(target && target.closest && target.closest('.defect-gantt-dep-hit'))) {
        this.selectedLinkId = null
      }
      /* 点在条/端口上由条自己处理；空白启动框选 */
      if (target && target.closest && target.closest('.defect-gantt-bar')) return
      e.preventDefault()
      this.startMarquee(e)
    },
    onBarMouseDown(e, row, group, mode) {
      if (e.button !== 0) return
      this.selectedLinkId = null
      const additive = !!(e.ctrlKey || e.metaKey || e.shiftKey)
      if (additive) {
        this.selectDefect(row && row.defect, { additive: true })
      } else if (!this.isSelectedDefect(row && row.defect)) {
        this.selectDefect(row && row.defect)
      } else if ((this.marqueeSelectedIds || []).length <= 1) {
        this.selectDefect(row && row.defect)
      }
      if (!this.canEdit) return
      e.preventDefault()
      const geo = barGeometry(row.defect, this.axisRange.start, this.scale)
      if (!geo) return
      const batchIds =
        mode === 'move' &&
        (this.marqueeSelectedIds || []).length > 1 &&
        this.isSelectedDefect(row.defect)
          ? (this.marqueeSelectedIds || []).map(Number).filter((id) => Number.isFinite(id))
          : null
      this.dragState = {
        mode,
        rowKey: row.rowKey,
        defect: row.defect,
        group,
        startClientX: e.clientX,
        startClientY: e.clientY,
        originX: geo.x,
        originWidth: geo.width,
        planStartTime: row.defect.planStartTime,
        planEndTime: row.defect.planEndTime,
        preview: { x: geo.x, width: geo.width },
        hoverGroup: group,
        /** 超过阈值才算真正拖动，避免「只点一下」就出临时行 / 幽灵 */
        activated: false,
        grabOffsetX: 0,
        grabOffsetY: 0,
        ghostWidth: Math.max(120, Math.min(geo.width, 280)),
        batchIds,
        additiveSelect: additive
      }
      {
        const el =
          (e.currentTarget && e.currentTarget.closest && e.currentTarget.closest('.defect-gantt-bar')) ||
          e.currentTarget
        const rect = el && el.getBoundingClientRect ? el.getBoundingClientRect() : null
        if (rect) {
          this.dragState.ghostWidth = Math.max(80, Math.min(rect.width, 280))
          this.dragState.grabOffsetX = e.clientX - rect.left
          this.dragState.grabOffsetY = e.clientY - rect.top
        }
      }
      document.addEventListener('mousemove', this.onBarMouseMove)
      document.addEventListener('mouseup', this.onBarMouseUp)
    },
    onBarMouseMove(e) {
      if (!this.dragState) return
      this._barMoveEvent = e
      if (this._barMoveRaf) return
      this._barMoveRaf = requestAnimationFrame(() => {
        this._barMoveRaf = 0
        this.flushBarMouseMove()
      })
    },
    flushBarMouseMove() {
      const e = this._barMoveEvent
      const ds = this.dragState
      if (!e || !ds) return
      const dx = e.clientX - ds.startClientX
      const dy = e.clientY - ds.startClientY
      const dist = Math.sqrt(dx * dx + dy * dy)
      const resizing = ds.mode === 'start' || ds.mode === 'end'
      if (!ds.activated) {
        if (dist < 5) return
        ds.activated = true
        this.setDraggingCursor(true)
        this.ensureGroupExpanded(ds.group && ds.group.key)
        /* 平移才跟手幽灵；拉宽只在原行条上变宽 */
        if (!resizing) {
          this.showDragGhost(
            ds.defect,
            e.clientX,
            e.clientY,
            'bar',
            ds.ghostWidth,
            ds.grabOffsetX,
            ds.grabOffsetY
          )
        }
      }
      const col = this.colWidth
      let nextPreview = ds.preview
      if (ds.mode === 'move') {
        const snapped = Math.round(dx / col) * col
        nextPreview = { x: ds.originX + snapped, width: ds.originWidth }
      } else if (ds.mode === 'start') {
        const snapped = Math.round(dx / col) * col
        let x = ds.originX + snapped
        let width = ds.originWidth - snapped
        if (width < col) {
          x = ds.originX + ds.originWidth - col
          width = col
        }
        nextPreview = { x, width }
      } else if (ds.mode === 'end') {
        const snapped = Math.round(dx / col) * col
        let width = ds.originWidth + snapped
        if (width < col) width = col
        nextPreview = { x: ds.originX, width }
      }
      const previewChanged =
        !ds.preview ||
        ds.preview.x !== nextPreview.x ||
        ds.preview.width !== nextPreview.width
      ds.preview = nextPreview
      if (resizing) {
        this.setDropInsertPreview(null)
        if (previewChanged) this.dragState = { ...ds }
        return
      }
      /* 批量平移只改水平日期，不做跨组插入预览 */
      if (Array.isArray(ds.batchIds) && ds.batchIds.length > 1) {
        this.setDropInsertPreview(null)
        if (previewChanged) this.dragState = { ...ds }
        this.moveDragGhost(e.clientX, e.clientY)
        return
      }
      const slot = this.findDropSlot(e.clientY, ds.defect && ds.defect.defectId)
      const hoverGroup = (slot && slot.group) || ds.group
      const hoverInsertIndex = slot ? slot.insertIndex : null
      const hoverChanged =
        (ds.hoverGroup && ds.hoverGroup.key) !== (hoverGroup && hoverGroup.key) ||
        ds.hoverInsertIndex !== hoverInsertIndex
      ds.hoverGroup = hoverGroup
      ds.hoverInsertIndex = hoverInsertIndex
      if (previewChanged || hoverChanged) this.dragState = { ...ds }
      this.moveDragGhost(e.clientX, e.clientY)
      this.scheduleDropInsertPreview(e.clientX, e.clientY)
    },
    async onBarMouseUp(e) {
      const ds = this.dragState
      const insertSnap = this.dropInsertPreview
      this.teardownDragListeners()
      const endDragUi = () => {
        this.dragState = null
        this.clearDragGhost()
      }
      if (!ds || !ds.defect) {
        endDragUi()
        return
      }
      /* 未真正拖动：点击条主体打开详情；加选点击只改选中态 */
      if (!ds.activated) {
        const defect = ds.defect
        const openDetail = ds.mode === 'move' && !ds.additiveSelect
        endDragUi()
        if (openDetail) this.openDefect(defect)
        return
      }
      const dayDelta = Math.round((ds.preview.x - ds.originX) / this.colWidth)
      let range = null
      if (ds.mode === 'move') {
        range = shiftPlanRange(ds.planStartTime, ds.planEndTime, this.scaleDayDelta(dayDelta))
      } else if (ds.mode === 'start') {
        const day = offsetPxToDate(ds.preview.x, this.axisRange.start, this.scale)
        range = resizePlanRange(ds.planStartTime, ds.planEndTime, 'start', day)
      } else if (ds.mode === 'end') {
        const endOffset = ds.preview.x + ds.preview.width - this.colWidth
        const day = offsetPxToDate(endOffset, this.axisRange.start, this.scale)
        range = resizePlanRange(ds.planStartTime, ds.planEndTime, 'end', day)
      }
      if (!range) {
        endDragUi()
        return
      }
      /* 多选批量平移：只改期，不改组/排序 */
      if (
        ds.mode === 'move' &&
        Array.isArray(ds.batchIds) &&
        ds.batchIds.length > 1
      ) {
        const days = this.scaleDayDelta(dayDelta)
        endDragUi()
        if (days !== 0) {
          await this.shiftDefectsByDays(ds.batchIds, days)
        }
        return
      }
      /* 按交付物：禁止跨交付物改组；禁止态只允许同组改期 */
      const moduleForbidden = !!(insertSnap && insertSnap.kind === 'forbidden')
      let groupChanged = !!(ds.hoverGroup && ds.group && ds.group.key !== ds.hoverGroup.key)
      if (moduleForbidden || (this.isModuleGroupBy && groupChanged && !this.isSameModuleGroup(ds.defect, ds.hoverGroup))) {
        groupChanged = false
      }
      const targetGroupKey = groupChanged
        ? ((insertSnap && insertSnap.groupKey) ||
          (ds.hoverGroup && ds.hoverGroup.key) ||
          (ds.group && ds.group.key))
        : (ds.group && ds.group.key)
      let insertIndex = null
      if (!moduleForbidden) {
        if (
          insertSnap &&
          insertSnap.kind !== 'forbidden' &&
          insertSnap.kind !== 'temp-module' &&
          Number.isFinite(insertSnap.insertIndex)
        ) {
          insertIndex = insertSnap.insertIndex
        } else if (ds.hoverInsertIndex != null) {
          insertIndex = ds.hoverInsertIndex
        }
      }
      const orderTargetKey = groupChanged
        ? targetGroupKey
        : (ds.group && ds.group.key)
      let orderChanged = false
      if (orderTargetKey && insertIndex != null && ds.mode === 'move') {
        const gNow = this.groups.find((x) => x.key === orderTargetKey)
        const full = ((gNow && gNow.rows) || [])
          .map((r) => (r && r.defect ? Number(r.defect.defectId) : null))
          .filter((x) => x != null)
        const id = Number(ds.defect.defectId)
        const curIdx = full.indexOf(id)
        /* insertIndex 按「去掉自身」后的列表计算，与原下标相同即未改序 */
        orderChanged = curIdx < 0 || curIdx !== insertIndex
      }
      const datesUnchanged =
        dayDelta === 0 &&
        ds.mode === 'move' &&
        ds.preview.width === ds.originWidth
      const widthUnchanged =
        (ds.mode === 'start' || ds.mode === 'end') &&
        ds.preview.x === ds.originX &&
        ds.preview.width === ds.originWidth
      if (ds.mode !== 'move' && widthUnchanged) {
        endDragUi()
        return
      }
      if (ds.mode === 'move' && datesUnchanged && !groupChanged && !orderChanged) {
        endDragUi()
        return
      }
      const targetGroup = groupChanged ? ds.hoverGroup : null
      const needPersist = !(datesUnchanged && !groupChanged)
      /*
       * 先乐观写回本地排期，再撤掉 dragState/插入预览，
       * 避免「预览位 → 原位 → 新位」左右晃一下。
       */
      if (needPersist) {
        const rollback = this.snapshotDefectForRollback(ds.defect)
        const optimistic = {
          planStartTime: range.planStartTime,
          planEndTime: range.planEndTime
        }
        if (targetGroup) {
          if (targetGroup.type === 'handler' && targetGroup.handlerId != null) {
            optimistic.handleBy = [Number(targetGroup.handlerId)]
          } else if (targetGroup.type === 'module' && targetGroup.moduleId != null) {
            optimistic.moduleId = targetGroup.moduleId
          }
        }
        this.patchLocalDefect(ds.defect.defectId, optimistic, targetGroup)
        if (ds.mode === 'move' && orderTargetKey && insertIndex != null) {
          this.applyGroupRowOrder(orderTargetKey, ds.defect.defectId, insertIndex)
        }
        endDragUi()
        await this.persistDefectUpdate(ds.defect, range, targetGroup, {
          skipLocalPatch: true,
          rollback
        })
        return
      }
      if (ds.mode === 'move' && orderTargetKey && insertIndex != null) {
        this.applyGroupRowOrder(orderTargetKey, ds.defect.defectId, insertIndex)
      }
      endDragUi()
    },
    snapshotDefectForRollback(defect) {
      if (!defect) return null
      return {
        planStartTime: defect.planStartTime,
        planEndTime: defect.planEndTime,
        handleBy: Array.isArray(defect.handleBy) ? defect.handleBy.slice() : defect.handleBy,
        handleByList: Array.isArray(defect.handleByList)
          ? defect.handleByList.map((h) => ({ ...h }))
          : defect.handleByList,
        moduleId: defect.moduleId,
        moduleName: defect.moduleName,
        modulePath: defect.modulePath
      }
    },
    scaleDayDelta(unitDelta) {
      if (this.scale === GANTT_SCALE.WEEK) return unitDelta * 7
      if (this.scale === GANTT_SCALE.MONTH) return unitDelta * 30
      return unitDelta
    },
    teardownDragListeners() {
      document.removeEventListener('mousemove', this.onBarMouseMove)
      document.removeEventListener('mouseup', this.onBarMouseUp)
    },
    async persistDefectUpdate(defect, planRange, group, opts = {}) {
      if (!defect || !defect.defectId || !planRange) return
      if (this.saving) return
      if (!this.ensureCanEdit()) return
      const payload = {
        defectId: defect.defectId,
        projectId: defect.projectId != null ? defect.projectId : this.projectId,
        planStartTime: planRange.planStartTime,
        planEndTime: planRange.planEndTime
      }
      if (group) {
        if (group.type === 'handler' && group.handlerId != null) {
          payload.handleBy = [Number(group.handlerId)]
        } else if (group.type === 'module') {
          if (group.moduleId != null) payload.moduleId = group.moduleId
        }
      }
      this.saving = true
      try {
        await updateDefect(payload)
        if (!opts.skipLocalPatch) {
          this.patchLocalDefect(defect.defectId, payload, group)
        }
        this.$modal.msgSuccess(this.$t('defect.gantt.save-ok'))
      } catch (err) {
        console.warn('[DefectGantt] update failed', err)
        if (opts.rollback) {
          this.restoreDefectFromSnapshot(defect.defectId, opts.rollback)
        }
      } finally {
        this.saving = false
      }
    },
    restoreDefectFromSnapshot(defectId, snap) {
      if (!snap || defectId == null) return
      const idx = this.defectList.findIndex((d) => d && d.defectId === defectId)
      if (idx < 0) return
      const next = { ...this.defectList[idx], ...snap }
      if (Array.isArray(snap.handleBy)) next.handleBy = snap.handleBy.slice()
      if (Array.isArray(snap.handleByList)) {
        next.handleByList = snap.handleByList.map((h) => ({ ...h }))
      }
      const list = this.defectList.slice()
      list[idx] = next
      this.defectList = list
    },
    patchLocalDefect(defectId, payload, group) {
      const idx = this.defectList.findIndex((d) => d && d.defectId === defectId)
      if (idx < 0) return
      const next = { ...this.defectList[idx] }
      if (payload.planStartTime != null) next.planStartTime = payload.planStartTime
      if (payload.planEndTime != null) next.planEndTime = payload.planEndTime
      if (payload.handleBy) {
        next.handleBy = payload.handleBy.slice()
        const id = payload.handleBy[0]
        const handlers = resolveHandlers(next)
        const hit = handlers.find((h) => h.userId === id)
        next.handleByList = hit
          ? [{ userId: id, nickName: hit.nickName }]
          : [{ userId: id, nickName: group && group.label ? group.label : String(id) }]
      }
      if (payload.moduleId != null) {
        next.moduleId = payload.moduleId
        if (group) {
          if (group.label) next.moduleName = group.label
          if (group.modulePath) next.modulePath = group.modulePath
          else if (group.label) next.modulePath = group.label
        }
      }
      const list = this.defectList.slice()
      list[idx] = next
      this.defectList = list
    },
    /* —— 甘特二期：依赖 / 基线 / 框选 —— */
    resolveProjectId() {
      return Number(
        this.projectId != null
          ? this.projectId
          : (this.queryParams && this.queryParams.projectId)
      )
    },
    /** head=左端点，tail=右端点；FS：tail→head 或 head←tail */
    resolveFsPair(fromDefectId, fromSide, toDefectId, toSide) {
      const a = Number(fromDefectId)
      const b = Number(toDefectId)
      if (!Number.isFinite(a) || !Number.isFinite(b) || a === b) return null
      if (fromSide === 'tail' && toSide === 'head') {
        return { fromDefectId: a, toDefectId: b }
      }
      if (fromSide === 'head' && toSide === 'tail') {
        return { fromDefectId: b, toDefectId: a }
      }
      return null
    },
    isLinkPortActive(defect, side) {
      const drag = this.linkDrag
      if (!drag || !defect) return false
      return Number(drag.fromDefectId) === Number(defect.defectId) && drag.fromSide === side
    },
    isLinkPortValidDrop(defect, side) {
      const drag = this.linkDrag
      if (!drag || !defect) return false
      if (Number(drag.fromDefectId) === Number(defect.defectId)) return false
      return !!this.resolveFsPair(drag.fromDefectId, drag.fromSide, defect.defectId, side)
    },
    isLinkDragTarget(defect) {
      const drag = this.linkDrag
      if (!drag || !defect || drag.targetDefectId == null) return false
      return Number(drag.targetDefectId) === Number(defect.defectId) && !!drag.valid
    },
    isLinkDragTargetInvalid(defect) {
      const drag = this.linkDrag
      if (!drag || !defect || drag.targetDefectId == null) return false
      return Number(drag.targetDefectId) === Number(defect.defectId) && !drag.valid
    },
    onLinkPortDown(e, defect, side) {
      if (!this.ensureCanEdit()) return
      if (!defect || defect.defectId == null) return
      if (e.button != null && e.button !== 0) return
      const local = this.clientToAxisLocal(e.clientX, e.clientY)
      const anchor = this.barAnchorByDefectId.get(Number(defect.defectId))
      const startX = anchor
        ? (side === 'head' ? anchor.left : anchor.right)
        : (local ? local.x : 0)
      const startY = anchor ? anchor.cy : (local ? local.y : 0)
      this.selectedLinkId = null
      this.linkDrag = {
        fromDefectId: Number(defect.defectId),
        fromSide: side,
        startX,
        startY,
        curX: local ? local.x : startX,
        curY: local ? local.y : startY,
        targetDefectId: null,
        targetSide: null,
        valid: false
      }
      this._onLinkDragMove = (ev) => this.onLinkDragMove(ev)
      this._onLinkDragUp = (ev) => this.onLinkDragUp(ev)
      window.addEventListener('mousemove', this._onLinkDragMove)
      window.addEventListener('mouseup', this._onLinkDragUp)
    },
    /**
     * 解析拖线落点：优先端口；否则整条缺陷条也可接（跨处理人/交付物分组时不必精准点到小圆点）。
     * 从尾拖出 → 落到目标条视为接其头；从头拖出 → 落到目标条视为接其尾。
     */
    resolveLinkDropTarget(clientX, clientY, drag) {
      const el = document.elementFromPoint(clientX, clientY)
      if (!el || !el.closest) return null
      const port = el.closest('[data-gantt-port]')
      if (port) {
        const tid = Number(port.getAttribute('data-defect-id'))
        const side = port.getAttribute('data-gantt-port')
        if (!Number.isFinite(tid) || tid === Number(drag.fromDefectId)) return null
        const pair = this.resolveFsPair(drag.fromDefectId, drag.fromSide, tid, side)
        return { targetDefectId: tid, targetSide: side, valid: !!pair }
      }
      const bar = el.closest('[data-gantt-bar][data-defect-id]')
      if (bar) {
        const tid = Number(bar.getAttribute('data-defect-id'))
        if (!Number.isFinite(tid) || tid === Number(drag.fromDefectId)) return null
        const side = drag.fromSide === 'tail' ? 'head' : 'tail'
        const pair = this.resolveFsPair(drag.fromDefectId, drag.fromSide, tid, side)
        return { targetDefectId: tid, targetSide: side, valid: !!pair }
      }
      return null
    },
    onLinkDragMove(e) {
      const drag = this.linkDrag
      if (!drag) return
      const local = this.clientToAxisLocal(e.clientX, e.clientY)
      if (local) {
        drag.curX = local.x
        drag.curY = local.y
      }
      const hit = this.resolveLinkDropTarget(e.clientX, e.clientY, drag)
      if (hit) {
        drag.targetDefectId = hit.targetDefectId
        drag.targetSide = hit.targetSide
        drag.valid = hit.valid
      } else {
        drag.targetDefectId = null
        drag.targetSide = null
        drag.valid = false
      }
      this.linkDrag = { ...drag }
    },
    async onLinkDragUp() {
      const drag = this.linkDrag
      this.teardownLinkDragListeners()
      this.linkDrag = null
      if (!drag || !drag.valid || drag.targetDefectId == null || !drag.targetSide) return
      const pair = this.resolveFsPair(
        drag.fromDefectId,
        drag.fromSide,
        drag.targetDefectId,
        drag.targetSide
      )
      if (!pair) {
        this.$modal.msgWarning(this.$t('defect.gantt.link-port-rule'))
        return
      }
      await this.createPlanLinkFs(pair.fromDefectId, pair.toDefectId)
    },
    teardownLinkDragListeners() {
      if (this._onLinkDragMove) {
        window.removeEventListener('mousemove', this._onLinkDragMove)
        this._onLinkDragMove = null
      }
      if (this._onLinkDragUp) {
        window.removeEventListener('mouseup', this._onLinkDragUp)
        this._onLinkDragUp = null
      }
    },
    async createPlanLinkFs(fromDefectId, toDefectId) {
      if (!this.ensureCanEdit()) return
      const from = Number(fromDefectId)
      const to = Number(toDefectId)
      if (!Number.isFinite(from) || !Number.isFinite(to)) return
      if (from === to) {
        this.$modal.msgWarning(this.$t('defect.gantt.link-self-forbidden'))
        return
      }
      const projectId = this.resolveProjectId()
      if (!projectId) return
      try {
        await createDefectPlanLink({
          projectId,
          fromDefectId: from,
          toDefectId: to,
          linkType: 'FS'
        })
        this.$modal.msgSuccess(this.$t('defect.gantt.link-add-ok'))
        this.fetchPlanGanttMeta()
      } catch (err) {
        console.warn('[DefectGantt] create link failed', err)
      }
    },
    onDepLinkMouseDown(e, path) {
      if (!path || path.linkId == null) return
      if (e.button != null && e.button !== 0) return
      this.selectedLinkId = path.linkId
    },
    onLinkKeyDown(e) {
      if (!e) return
      const key = e.key || e.code
      if (key !== 'Delete' && key !== 'Del' && key !== 'Backspace') return
      if (this.selectedLinkId == null) return
      const tag = e.target && e.target.tagName ? String(e.target.tagName).toLowerCase() : ''
      if (tag === 'input' || tag === 'textarea' || (e.target && e.target.isContentEditable)) return
      e.preventDefault()
      this.deleteSelectedPlanLink()
    },
    async deleteSelectedPlanLink() {
      const linkId = this.selectedLinkId
      if (linkId == null) return
      if (!this.ensureCanEdit()) return
      try {
        await deleteDefectPlanLink(linkId)
        this.selectedLinkId = null
        this.$modal.msgSuccess(this.$t('defect.gantt.link-delete-ok'))
        this.fetchPlanGanttMeta()
      } catch (err) {
        console.warn('[DefectGantt] delete link failed', err)
      }
    },
    async removePlanLink(link) {
      if (!link || link.linkId == null) return
      this.selectedLinkId = link.linkId
      await this.deleteSelectedPlanLink()
    },
    async confirmFreezeBaseline() {
      if (!this.ensureCanEdit()) return
      const projectId = this.resolveProjectId()
      if (!projectId) return
      try {
        await this.$modal.confirm(this.$t('defect.gantt.freeze-baseline-confirm'))
      } catch (_) {
        return
      }
      try {
        await freezeDefectPlanBaseline(projectId)
        this.$modal.msgSuccess(this.$t('defect.gantt.freeze-baseline-ok'))
        this.fetchPlanGanttMeta()
      } catch (err) {
        console.warn('[DefectGantt] freeze baseline failed', err)
      }
    },
    /** 指针坐标 → axis-body 内本地坐标 */
    clientToAxisLocal(clientX, clientY) {
      const body = this.$refs.axisBodyScroll
      if (!body) return null
      const rect = body.getBoundingClientRect()
      return {
        x: clientX - rect.left + body.scrollLeft,
        y: clientY - rect.top + body.scrollTop
      }
    },
    startMarquee(e) {
      const local = this.clientToAxisLocal(e.clientX, e.clientY)
      if (!local) return
      this.marqueeDrag = {
        startX: local.x,
        startY: local.y,
        additive: !!(e.ctrlKey || e.metaKey || e.shiftKey)
      }
      this.marqueeRect = {
        left: local.x,
        top: local.y,
        width: 0,
        height: 0
      }
      if (!this.marqueeDrag.additive) {
        this.marqueeSelectedIds = []
      }
      document.addEventListener('mousemove', this.onMarqueeMove)
      document.addEventListener('mouseup', this.onMarqueeUp)
    },
    onMarqueeMove(e) {
      if (!this.marqueeDrag) return
      const local = this.clientToAxisLocal(e.clientX, e.clientY)
      if (!local) return
      const x1 = this.marqueeDrag.startX
      const y1 = this.marqueeDrag.startY
      const left = Math.min(x1, local.x)
      const top = Math.min(y1, local.y)
      const width = Math.abs(local.x - x1)
      const height = Math.abs(local.y - y1)
      this.marqueeRect = { left, top, width, height }
    },
    onMarqueeUp() {
      const rect = this.marqueeRect
      const additive = this.marqueeDrag && this.marqueeDrag.additive
      this.teardownMarqueeListeners()
      this.marqueeDrag = null
      this.marqueeRect = null
      if (!rect || (rect.width < 3 && rect.height < 3)) {
        if (!additive) this.clearDefectSelection()
        return
      }
      const hit = this.collectBarsInRect(rect)
      if (additive) {
        const set = new Set((this.marqueeSelectedIds || []).map(Number))
        hit.forEach((id) => set.add(Number(id)))
        this.marqueeSelectedIds = Array.from(set)
      } else {
        this.marqueeSelectedIds = hit
      }
      this.selectedDefectId = this.marqueeSelectedIds.length
        ? this.marqueeSelectedIds[this.marqueeSelectedIds.length - 1]
        : null
    },
    teardownMarqueeListeners() {
      document.removeEventListener('mousemove', this.onMarqueeMove)
      document.removeEventListener('mouseup', this.onMarqueeUp)
    },
    /** 矩形与整行相交的 defectId（首次出现锚点） */
    collectBarsInRect(rect) {
      const ids = []
      const anchors = this.barAnchorByDefectId
      anchors.forEach((a, id) => {
        if (!a) return
        const barLeft = a.x
        const barRight = a.x + a.width
        const barTop = a.y
        const barBottom = a.y + this.rowHeight
        const hit =
          barRight >= rect.left &&
          barLeft <= rect.left + rect.width &&
          barBottom >= rect.top &&
          barTop <= rect.top + rect.height
        if (hit) ids.push(id)
      })
      return ids
    },
    /** 批量按自然日平移已排期缺陷 */
    async shiftDefectsByDays(idsInput, days) {
      if (!this.ensureCanEdit()) return
      const delta = Number(days)
      if (!Number.isFinite(delta) || delta === 0) return
      const ids = (idsInput || []).map(Number).filter((id) => Number.isFinite(id))
      if (!ids.length) return
      let ok = 0
      let fail = 0
      for (let i = 0; i < ids.length; i++) {
        const id = ids[i]
        const defect = (this.defectList || []).find((d) => d && Number(d.defectId) === id)
        if (!defect) {
          fail += 1
          continue
        }
        const range = shiftPlanRange(defect.planStartTime, defect.planEndTime, delta)
        if (!range) {
          fail += 1
          continue
        }
        const rollback = this.snapshotDefectForRollback(defect)
        this.patchLocalDefect(id, range, null)
        try {
          await updateDefect({
            defectId: id,
            projectId: defect.projectId != null ? defect.projectId : this.resolveProjectId(),
            planStartTime: range.planStartTime,
            planEndTime: range.planEndTime
          })
          ok += 1
        } catch (err) {
          fail += 1
          console.warn('[DefectGantt] batch shift failed', id, err)
          if (rollback) this.restoreDefectFromSnapshot(id, rollback)
        }
      }
      if (ok) {
        this.$modal.msgSuccess(this.$t('defect.gantt.marquee-shift-ok', { n: ok }))
      }
      if (fail) {
        this.$modal.msgWarning(this.$t('defect.gantt.marquee-shift-partial', { ok, fail }))
      }
      const conflicts = findFsScheduleConflicts(this.scheduledDefects, this.planLinks)
      const idSet = new Set(ids)
      const related = conflicts.filter(
        (c) => idSet.has(Number(c.fromDefectId)) || idSet.has(Number(c.toDefectId))
      )
      if (related.length) {
        const tip = related
          .slice(0, 8)
          .map((c) => `#${this.defectNumOf(c.fromDefectId)}→#${this.defectNumOf(c.toDefectId)}`)
          .join(', ')
        this.$modal.msgWarning(this.$t('defect.gantt.fs-conflict', { list: tip }))
      }
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/multipane-resizer-grip.scss";

.defect-gantt-root {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  height: 100%;
}

.defect-gantt-tools {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--cat2bug-toolbar-row-gap, 10px);
  margin-bottom: 10px;
}

.defect-gantt-tools-right {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cat2bug-toolbar-item-gap, 10px);
}

.defect-gantt-body {
  position: relative;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: var(--cat2bug-border-radius, 4px);
  background: var(--bg-color-block, #fff);
  overflow: hidden;
}

.defect-gantt-main {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.defect-gantt-scheduled {
  display: flex;
  flex-direction: row;
  flex: 1;
  min-height: 0;
  min-width: 0;
}

.defect-gantt-left {
  display: flex;
  flex-direction: column;
  flex: 0 0 auto;
  min-width: 0;
  border-right: 1px solid var(--el-border-color-lighter, #ebeef5);
  background: var(--bg-color-base, #f5f7fa);
}

.defect-gantt-left-header {
  display: flex;
  align-items: stretch;
  padding: 0;
  border-bottom: 1px solid var(--el-border-color-lighter, #ebeef5);
  box-sizing: border-box;
  overflow: hidden;
}
.defect-gantt-group-tabs {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  :deep(.el-tabs__header) {
    margin: 0;
    flex: 1;
    display: flex;
    align-items: stretch;
  }
  :deep(.el-tabs__nav-wrap) {
    flex: 1;
    display: flex;
    &::after {
      height: 0;
    }
  }
  :deep(.el-tabs__nav-scroll) {
    flex: 1;
    display: flex;
  }
  :deep(.el-tabs__nav) {
    flex: 1;
    display: flex;
    width: 100%;
    height: 100%;
  }
  :deep(.el-tabs__item) {
    flex: 1;
    justify-content: center;
    height: 100%;
    line-height: normal;
    padding: 0 8px;
    font-size: 13px;
    font-weight: 600;
  }
  :deep(.el-tabs__active-bar) {
    height: 2px;
  }
  :deep(.el-tabs__content) {
    display: none;
  }
  .defect-gantt-group-tab-label {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    .el-icon {
      font-size: 14px;
    }
    .defect-gantt-group-tab-svg {
      width: 14px;
      height: 14px;
      vertical-align: middle;
    }
  }
}

.defect-gantt-left-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  &.is-empty {
    overflow-x: hidden;
    overflow-y: hidden;
  }
}

.defect-gantt-left-fill {
  flex: 0 0 auto;
  box-sizing: border-box;
}

.defect-gantt-group-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8px;
  /* 按交付物 depth 缩进：每层 16px */
  padding: 0 10px 0 calc(4px + var(--gantt-group-depth, 0) * 16px);
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-regular, #606266);
  background: var(--el-fill-color-light, #fafafa);
  border-bottom: 1px solid var(--el-border-color-extra-light, #f2f2f2);
  box-sizing: border-box;
  cursor: pointer;
  user-select: none;
  outline: none;
  &:hover {
    background: var(--el-fill-color, #f0f2f5);
    .defect-gantt-group-toggle {
      color: var(--el-color-primary, #409eff);
    }
  }
  &:focus-visible {
    box-shadow: inset 0 0 0 2px var(--el-color-primary-light-5, #a0cfff);
  }
  &.is-temp-module {
    cursor: default;
    border-style: dashed;
    background: rgba(64, 158, 255, 0.12);
    color: var(--el-color-primary, #409eff);
  }
  &.is-structural {
    font-weight: 500;
    color: var(--el-text-color-secondary, #909399);
    .defect-gantt-group-label {
      font-weight: 500;
      color: var(--el-text-color-regular, #606266);
    }
  }
}

.defect-gantt-group-toggle {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  margin: 0;
  padding: 0;
  border: none;
  border-radius: var(--cat2bug-border-radius, 4px);
  background: transparent;
  color: var(--el-text-color-secondary, #909399);
  line-height: 0;
  pointer-events: none;
  .el-icon {
    font-size: 12px;
  }
}

.defect-gantt-group-avatar {
  flex: 0 0 auto;
  line-height: 0;
  :deep(.el-avatar) {
    border: none !important;
  }
}

.defect-gantt-group-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--el-text-color-primary, #303133);
}

.defect-gantt-row-label {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  border-bottom: 1px solid var(--el-border-color-extra-light, #f2f2f2);
  box-sizing: border-box;
  cursor: pointer;
  user-select: none;
  &.is-selected {
    background: var(--el-color-primary-light-9, #ecf5ff);
    box-shadow: inset 3px 0 0 var(--el-color-primary, #409eff);
  }
  &.is-drop-insert {
    cursor: default;
    background: rgba(64, 158, 255, 0.22);
    box-shadow: inset 3px 0 0 var(--el-color-primary, #409eff);
    outline: 1px dashed var(--el-color-primary, #409eff);
    outline-offset: -1px;
    color: var(--el-text-color-primary, #303133);
    .defect-gantt-row-num,
    .defect-gantt-row-name {
      color: var(--el-text-color-primary, #303133);
      opacity: 1;
    }
  }
}

.defect-gantt-row-num {
  flex: 0 0 auto;
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
}

.defect-gantt-row-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.defect-gantt-days {
  flex: 0 0 auto;
  margin-left: auto;
  padding-left: 6px;
  font-size: 12px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  color: var(--el-text-color-secondary, #909399);
  white-space: nowrap;
  &.is-group {
    font-weight: 600;
    color: var(--el-text-color-regular, #606266);
  }
}

.defect-gantt-mirror-icon {
  flex: 0 0 auto;
  font-size: 14px;
  color: var(--el-color-warning, #e6a23c);
}

/* 与缺陷表格同款细轨道 + 手柄；占位更窄，避免整条灰带过粗 */
.defect-gantt-resizer {
  flex: 0 0 4px;
  margin: 0 0 0 -2px;
  position: relative;
  z-index: 6;
  box-sizing: border-box;
  cursor: col-resize;
  background: transparent !important;
  background-image: none !important;
  align-self: stretch;
  @include multipane-resizer-vertical-appearance;
  &:after {
    width: 5px;
    height: 28px;
  }
}

.defect-gantt-axis {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  min-height: 0;
}

.defect-gantt-axis-header-scroll {
  overflow-x: auto;
  overflow-y: hidden;
  border-bottom: 1px solid var(--el-border-color-lighter, #ebeef5);
  scrollbar-width: none;
  &::-webkit-scrollbar {
    display: none;
  }
}

.defect-gantt-axis-header {
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.defect-gantt-axis-major {
  display: flex;
  flex-direction: row;
  flex: 0 0 auto;
  border-bottom: 1px solid var(--el-border-color-lighter, #ebeef5);
  background: var(--el-fill-color-light, #fafafa);
  box-sizing: border-box;
}

.defect-gantt-axis-major-cell {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-regular, #606266);
  border-right: 1px solid var(--el-border-color-lighter, #ebeef5);
  box-sizing: border-box;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  padding: 0 4px;
}

.defect-gantt-axis-minor {
  display: flex;
  flex-direction: row;
  flex: 0 0 auto;
  box-sizing: border-box;
}

.defect-gantt-axis-unit {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
  font-size: 12px;
  line-height: 1.15;
  border-right: 1px solid var(--el-border-color-extra-light, #f2f2f2);
  box-sizing: border-box;
  &.weekend {
    background: var(--el-fill-color, #f0f2f5);
  }
  &.is-day-scale,
  &.is-week-scale {
    padding: 2px 0;
  }
}

.defect-gantt-axis-day-num {
  font-weight: 600;
  color: var(--el-text-color-primary, #303133);
}

.defect-gantt-axis-weekday {
  font-size: 10px;
  font-weight: 400;
  color: var(--el-text-color-secondary, #909399);
}

.defect-gantt-axis-week-num {
  font-weight: 600;
  font-size: 12px;
  color: var(--el-text-color-primary, #303133);
}

.defect-gantt-axis-week-range {
  font-size: 10px;
  font-weight: 400;
  color: var(--el-text-color-secondary, #909399);
  white-space: nowrap;
}

.defect-gantt-axis-body-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  position: relative;
  /* 作为通底高度参照；格子背景随 axis-body 至少撑满此区 */
  &.is-empty {
    overflow-x: auto;
    overflow-y: hidden;
  }
}

.defect-gantt-axis-body {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 100%;
  box-sizing: border-box;
}

.defect-gantt-axis-col-bg {
  position: absolute;
  top: 0;
  bottom: 0;
  height: 100%;
  border-right: 1px solid var(--el-border-color-extra-light, #f2f2f2);
  pointer-events: none;
  box-sizing: border-box;
  &.weekend {
    background: rgba(144, 147, 153, 0.08);
  }
}

.defect-gantt-today-line {
  position: absolute;
  top: 0;
  bottom: 0;
  height: 100%;
  width: 2px;
  background: var(--el-color-danger, #f56c6c);
  z-index: 3;
  pointer-events: none;
}

.defect-gantt-axis-group-band {
  position: relative;
  z-index: 1;
  box-sizing: border-box;
  border-bottom: 1px solid var(--el-border-color-extra-light, #f2f2f2);
  background: rgba(0, 0, 0, 0.02);
  &.is-temp-module {
    background: rgba(64, 158, 255, 0.12);
    border-bottom-style: dashed;
  }
}

.defect-gantt-axis-row {
  position: relative;
  z-index: 2;
  box-sizing: border-box;
  border-bottom: 1px solid var(--el-border-color-extra-light, #f2f2f2);
  &.is-selected {
    background: var(--el-color-primary-light-9, #ecf5ff);
  }
  &.is-drop-insert {
    background: rgba(64, 158, 255, 0.18);
    outline: 1px dashed var(--el-color-primary, #409eff);
    outline-offset: -1px;
  }
}

/* 每人组底资源负荷轨（参考图：导轨线 + 浅蓝矮条 / 粉红高块） */
.defect-gantt-load-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 10px;
  font-size: 11px;
  line-height: 1;
  color: var(--el-text-color-secondary, #909399);
  border-bottom: 1px solid var(--el-border-color-extra-light, #f2f2f2);
  box-sizing: border-box;
  user-select: none;
}
.defect-gantt-load-track {
  position: relative;
  z-index: 2;
  box-sizing: border-box;
  border-bottom: 1px solid var(--el-border-color-extra-light, #f2f2f2);
  background: transparent;
  overflow: hidden;
  pointer-events: none;
}
.defect-gantt-load-guide {
  position: absolute;
  left: 0;
  right: 0;
  height: 1px;
  pointer-events: none;
  z-index: 0;
  &.is-top {
    top: 5px;
    background: rgba(240, 149, 165, 0.55);
  }
  &.is-mid {
    top: 50%;
    margin-top: -0.5px;
    background: rgba(126, 184, 240, 0.7);
  }
  &.is-base {
    bottom: 5px;
    background: rgba(144, 147, 153, 0.35);
  }
}
.defect-gantt-load-seg {
  position: absolute;
  box-sizing: border-box;
  pointer-events: auto;
  z-index: 1;
  &.is-normal {
    top: 50%;
    height: 3px;
    margin-top: -1.5px;
    border-radius: 1px;
    background: #7eb8f0;
  }
  &.is-overload {
    top: 4px;
    height: calc(100% - 8px);
    border-radius: 2px;
    background: #f095a5;
    box-shadow: 0 0 0 1px rgba(240, 149, 165, 0.35);
  }
}

.defect-gantt-bar.is-drop-preview {
  pointer-events: none;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.55);
  /* 不再上下脉冲，避免看起来和鼠标位有「跳动距离」 */
}

.defect-gantt-bar {
  position: absolute;
  top: 6px;
  height: calc(100% - 12px);
  /* 周/月短任务按比例很窄；编号可溢出条外，勿再强制 28px 拉宽 */
  min-width: 4px;
  border-radius: var(--cat2bug-border-radius, 4px);
  display: flex;
  flex-direction: row;
  align-items: center;
  cursor: grab;
  user-select: none;
  -webkit-user-drag: none;
  z-index: 4;
  box-sizing: border-box;
  border: 1px solid transparent;
  overflow: visible;
  &.is-dragging {
    opacity: 0.85;
    cursor: grabbing;
  }
  &.is-forbidden {
    --gantt-forbid-mark: rgba(255, 255, 255, 0.92);
    overflow: hidden;
    outline: none;
    border-color: var(--gantt-forbid-mark);
    box-shadow: inset 0 0 0 1px var(--gantt-forbid-mark);
    &::before {
      content: '';
      position: absolute;
      inset: 0;
      background: rgba(0, 0, 0, 0.18);
      pointer-events: none;
      z-index: 2;
    }
    &::after {
      content: '';
      position: absolute;
      left: 50%;
      top: 50%;
      width: 160%;
      height: 3px;
      margin: 0;
      border-radius: 999px;
      background: var(--gantt-forbid-mark);
      transform: translate(-50%, -50%) rotate(32deg);
      pointer-events: none;
      z-index: 3;
    }
  }
  &.is-selected {
    outline: 2px solid var(--el-color-primary, #409eff);
    outline-offset: 1px;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.35);
    z-index: 5;
  }
  &.state-processing {
    background: var(--defect-state-processing-bg, #e04e4e);
    border-color: var(--defect-state-processing-border, #e04e4e);
    color: var(--defect-state-processing-color, #fff);
  }
  &.state-audit {
    background: var(--defect-state-audit-bg, #337ecc);
    border-color: var(--defect-state-audit-border, #337ecc);
    color: var(--defect-state-audit-color, #fff);
  }
  &.state-resolved {
    background: var(--defect-state-resolved-bg, #5daf34);
    border-color: var(--defect-state-resolved-border, #5daf34);
    color: var(--defect-state-resolved-color, #fff);
  }
  &.state-rejected {
    background: var(--defect-state-rejected-bg, #cf9236);
    border-color: var(--defect-state-rejected-border, #cf9236);
    color: var(--defect-state-rejected-color, #fff);
  }
  &.state-closed {
    background: var(--defect-state-closed-bg, #909399);
    border-color: var(--defect-state-closed-border, #909399);
    color: var(--defect-state-closed-color, #fff);
  }
  &.state-unknown {
    background: var(--defect-state-unknown-bg, #909399);
    border-color: var(--defect-state-unknown-border, #909399);
    color: var(--defect-state-unknown-color, #fff);
  }
}

.defect-gantt-bar-label {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: row;
  align-items: center;
  overflow: visible;
  white-space: nowrap;
  font-size: 12px;
  line-height: 1.2;
  padding: 0 4px;
  pointer-events: none;
}
.defect-gantt-bar-num {
  flex: 0 0 auto;
  font-weight: 600;
  white-space: nowrap;
}
.defect-gantt-bar-name {
  flex: 1 1 auto;
  min-width: 0;
  margin-left: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.defect-gantt-bar-handle {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 6px;
  z-index: 2;
  cursor: ew-resize;
  opacity: 0;
  &.left {
    left: 0;
  }
  &.right {
    right: 0;
  }
  &:hover {
    opacity: 0.85;
    background: rgba(255, 255, 255, 0.35);
  }
}
.defect-gantt-bar:hover .defect-gantt-bar-handle {
  opacity: 0.35;
}
.defect-gantt-bar-port {
  position: absolute;
  top: 50%;
  width: 10px;
  height: 10px;
  margin-top: -5px;
  border-radius: 50%;
  box-sizing: border-box;
  background: var(--el-bg-color, #fff);
  border: 2px solid var(--el-color-primary, #409eff);
  opacity: 0;
  pointer-events: auto;
  cursor: crosshair;
  z-index: 8;
  transition: opacity 0.12s ease, transform 0.12s ease, background 0.12s ease;
  &.left {
    left: -6px;
  }
  &.right {
    right: -6px;
  }
  &.is-active,
  &.is-valid-drop {
    opacity: 1;
    transform: scale(1.15);
  }
  &.is-valid-drop {
    background: var(--el-color-success, #67c23a);
    border-color: var(--el-color-success, #67c23a);
  }
}
.defect-gantt-bar:hover .defect-gantt-bar-port,
.defect-gantt-bar.is-link-target .defect-gantt-bar-port {
  opacity: 1;
}
.defect-gantt-axis-body.is-linking .defect-gantt-bar-port {
  opacity: 0.55;
}
.defect-gantt-bar.is-link-target {
  outline: 2px solid var(--el-color-success, #67c23a);
  outline-offset: 1px;
}
.defect-gantt-bar.is-link-target-invalid {
  outline: 2px solid var(--el-color-danger, #f56c6c);
  outline-offset: 1px;
}
.defect-gantt-unscheduled-resizer {
  flex: 0 0 4px;
  cursor: row-resize;
  background: transparent !important;
  background-image: none !important;
  position: relative;
  z-index: 6;
  box-sizing: border-box;
  @include multipane-resizer-horizontal-appearance;
  &:after {
    width: 28px;
    height: 5px;
  }
}

.defect-gantt-unscheduled {
  flex: 0 0 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--bg-color-base, #f5f7fa);
  overflow: hidden;
  &.is-collapsed {
    flex: 0 0 auto;
    height: auto !important;
  }
}

.defect-gantt-unscheduled-head {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  padding: 6px 10px 6px 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary, #909399);
}

.defect-gantt-unscheduled-toggle {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
  min-width: 0;
  margin: 0;
  padding: 2px 4px 2px 0;
  border: none;
  border-radius: var(--cat2bug-border-radius, 4px);
  background: transparent;
  color: inherit;
  cursor: pointer;
  user-select: none;
  outline: none;
  &:hover {
    color: var(--el-color-primary, #409eff);
    .defect-gantt-group-toggle {
      color: var(--el-color-primary, #409eff);
    }
  }
  &:focus-visible {
    box-shadow: inset 0 0 0 2px var(--el-color-primary-light-5, #a0cfff);
  }
  .defect-gantt-group-toggle {
    pointer-events: none;
  }
}

.defect-gantt-unscheduled-title {
  flex: 0 0 auto;
  white-space: nowrap;
}

.defect-gantt-unscheduled-count {
  font-weight: 400;
}

.defect-gantt-unscheduled-search {
  flex: 1 1 auto;
  max-width: 320px;
  min-width: 140px;
}

.defect-gantt-unscheduled-pool {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-content: flex-start;
  align-items: flex-start;
  gap: 8px;
  /* 顶部留白，避免第一行芯片选中描边 / 外发光被 overflow 裁切 */
  padding: 6px 10px 10px;
  overflow: auto;
  min-height: 0;
  flex: 1 1 auto;
}

.defect-gantt-chip {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  align-self: flex-start;
  flex: 0 0 auto;
  height: auto;
  max-width: 220px;
  padding: 4px 8px;
  border-radius: var(--cat2bug-border-radius, 4px);
  font-size: 12px;
  line-height: 1.3;
  cursor: grab;
  user-select: none;
  -webkit-user-drag: element;
  border: 1px solid transparent;
  &.state-processing {
    background: var(--defect-state-processing-bg, #e04e4e);
    color: var(--defect-state-processing-color, #fff);
  }
  &.state-audit {
    background: var(--defect-state-audit-bg, #337ecc);
    color: var(--defect-state-audit-color, #fff);
  }
  &.state-resolved {
    background: var(--defect-state-resolved-bg, #5daf34);
    color: var(--defect-state-resolved-color, #fff);
  }
  &.state-rejected {
    background: var(--defect-state-rejected-bg, #cf9236);
    color: var(--defect-state-rejected-color, #fff);
  }
  &.state-closed {
    background: var(--defect-state-closed-bg, #909399);
    color: var(--defect-state-closed-color, #fff);
  }
  &.state-unknown {
    background: var(--defect-state-unknown-bg, #909399);
    color: var(--defect-state-unknown-color, #fff);
  }
  &.is-selected {
    outline: 2px solid var(--el-color-primary, #409eff);
    outline-offset: 1px;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.35);
  }
  &.is-dragging {
    opacity: 0.35;
  }
}

.defect-gantt-chip-num {
  opacity: 0.9;
}

.defect-gantt-chip-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.defect-gantt-unscheduled-empty,
.defect-gantt-cap-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
}

.defect-gantt-cap-hint {
  padding: 4px 10px;
  border-top: 1px solid var(--el-border-color-extra-light, #f2f2f2);
}

.defect-gantt-empty {
  padding: 24px 0;
}

/* 周末列背景水平定位 */
.defect-gantt-axis-body {
  .defect-gantt-axis-col-bg {
    left: 0;
  }
}
</style>

<style lang="scss">
/* Teleport 到 body：拖拽幽灵与落点引导线 */
.defect-gantt-drag-ghost {
  position: fixed;
  left: 0;
  top: 0;
  z-index: 4000;
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  gap: 6px;
  max-width: 320px;
  padding: 6px 10px;
  border-radius: var(--cat2bug-border-radius, 4px);
  font-size: 12px;
  line-height: 1.2;
  pointer-events: none;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.28);
  opacity: 0.95;
  transform: translate3d(-9999px, -9999px, 0);
  will-change: transform;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  border: 1px solid transparent;
  &.kind-bar {
    min-height: 24px;
  }
  &.state-processing {
    background: var(--defect-state-processing-bg, #e04e4e);
    color: var(--defect-state-processing-color, #fff);
  }
  &.state-audit {
    background: var(--defect-state-audit-bg, #337ecc);
    color: var(--defect-state-audit-color, #fff);
  }
  &.state-resolved {
    background: var(--defect-state-resolved-bg, #5daf34);
    color: var(--defect-state-resolved-color, #fff);
  }
  &.state-rejected {
    background: var(--defect-state-rejected-bg, #cf9236);
    color: var(--defect-state-rejected-color, #fff);
  }
  &.state-closed,
  &.state-unknown {
    background: var(--defect-state-closed-bg, #909399);
    color: var(--defect-state-closed-color, #fff);
  }
  &.is-forbidden {
    --gantt-forbid-mark: rgba(255, 255, 255, 0.92);
    overflow: hidden;
    border-color: var(--gantt-forbid-mark);
    box-shadow:
      inset 0 0 0 1px var(--gantt-forbid-mark),
      0 8px 20px rgba(0, 0, 0, 0.35);
    &::before {
      content: '';
      position: absolute;
      inset: 0;
      background: rgba(0, 0, 0, 0.18);
      pointer-events: none;
      z-index: 2;
    }
    &::after {
      content: '';
      position: absolute;
      left: 50%;
      top: 50%;
      width: 160%;
      height: 3px;
      margin: 0;
      border-radius: 999px;
      background: var(--gantt-forbid-mark);
      transform: translate(-50%, -50%) rotate(32deg);
      pointer-events: none;
      z-index: 3;
    }
  }
}

.defect-gantt-drag-ghost-num {
  flex: 0 0 auto;
  font-weight: 600;
}

.defect-gantt-drag-ghost-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 拖动中：抓取光标，压过浏览器默认链/地球光标 */
body.defect-gantt-is-dragging,
body.defect-gantt-is-dragging * {
  cursor: grabbing !important;
}

body.defect-gantt-is-dragging-forbidden,
body.defect-gantt-is-dragging-forbidden * {
  cursor: not-allowed !important;
}

body.defect-gantt-resizing-unscheduled,
body.defect-gantt-resizing-unscheduled * {
  cursor: row-resize !important;
  user-select: none !important;
}

/* 落点插入行展开动画 */
.defect-gantt-insert-enter-active,
.defect-gantt-insert-leave-active {
  transition: height 0.18s ease, opacity 0.18s ease, margin 0.18s ease;
  overflow: hidden;
}
.defect-gantt-insert-enter-from,
.defect-gantt-insert-leave-to {
  height: 0 !important;
  opacity: 0;
  margin-top: 0;
  margin-bottom: 0;
}

@keyframes defect-gantt-bar-pulse {
  0%,
  100% {
    opacity: 0.72;
    transform: scaleY(1);
  }
  50% {
    opacity: 1;
    transform: scaleY(1.06);
  }
}

html.dark .defect-gantt-row-label.is-drop-insert {
  background: rgba(64, 158, 255, 0.28);
  color: var(--el-text-color-primary, #e5eaf3);
}
html.dark .defect-gantt-row-label.is-drop-insert .defect-gantt-row-num,
html.dark .defect-gantt-row-label.is-drop-insert .defect-gantt-row-name {
  color: var(--el-text-color-primary, #e5eaf3);
}
html.dark .defect-gantt-axis-row.is-drop-insert {
  background: rgba(64, 158, 255, 0.22);
}
html.dark .defect-gantt-load-guide.is-top {
  background: rgba(240, 149, 165, 0.65);
}
html.dark .defect-gantt-load-guide.is-mid {
  background: rgba(126, 184, 240, 0.8);
}
html.dark .defect-gantt-load-guide.is-base {
  background: rgba(255, 255, 255, 0.28);
}
html.dark .defect-gantt-load-seg.is-normal {
  background: #8ec4f5;
}
html.dark .defect-gantt-load-seg.is-overload {
  background: #f0a0ae;
}

/* —— 甘特二期：工具条 / 依赖 / 关键路径 / 负荷 / 基线 / 框选 —— */
.defect-gantt-tools-divider {
  margin: 0 4px;
  height: 1.2em;
}
.defect-gantt-tools-right {
  :deep(.el-checkbox) {
    margin-right: 6px;
  }
}
.defect-gantt-layers-dropdown {
  margin-right: 4px;
  .defect-gantt-layers-btn {
    padding: 5px 8px;
    .el-icon {
      font-size: 14px;
    }
  }
}
.defect-gantt-layers-menu {
  min-width: 148px;
  padding: 4px 0;
  .defect-gantt-layers-item {
    padding: 0 12px;
    line-height: 32px;
    height: auto;
    .el-checkbox {
      width: 100%;
      height: 32px;
      margin-right: 0;
    }
  }
  .defect-gantt-layers-action {
    justify-content: flex-start;
    text-align: left;
    .defect-gantt-layers-action-label {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      .el-icon {
        font-size: 14px;
      }
    }
  }
}
.defect-gantt-load-banner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 10px;
  padding: 4px 10px;
  font-size: 12px;
  background: var(--el-color-warning-light-9, #fdf6ec);
  color: var(--el-color-warning-dark-2, #b88230);
  border-bottom: 1px solid var(--el-border-color-lighter, #ebeef5);
  &.is-ok {
    background: var(--el-color-success-light-9, #f0f9eb);
    color: var(--el-color-success, #67c23a);
  }
  .defect-gantt-load-title {
    font-weight: 600;
  }
  .defect-gantt-load-item {
    white-space: nowrap;
  }
}
.defect-gantt-link-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 10px;
  padding: 4px 10px;
  font-size: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter, #ebeef5);
  .defect-gantt-link-list-title {
    font-weight: 600;
    color: var(--el-text-color-regular, #606266);
  }
  .defect-gantt-link-chip {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    padding: 0 6px;
    border-radius: 4px;
    background: var(--el-fill-color-light, #f5f7fa);
  }
}
.defect-gantt-dep-layer {
  position: absolute;
  left: 0;
  top: 0;
  pointer-events: none;
  z-index: 6;
  overflow: visible;
  &.is-interactive {
    pointer-events: none;
    .defect-gantt-dep-hit {
      pointer-events: stroke;
      cursor: pointer;
    }
  }
}
.defect-gantt-dep-hit {
  stroke: transparent;
  stroke-width: 12;
  fill: none;
}
.defect-gantt-dep-path {
  stroke: var(--el-color-primary-light-3, #79bbff);
  stroke-width: 1.35;
  stroke-dasharray: 3.5 7;
  stroke-linecap: round;
  stroke-linejoin: round;
  opacity: 0.78;
  &.is-critical-link {
    stroke: var(--el-color-danger-light-3, #f89898);
    stroke-width: 1.5;
  }
  &.is-selected {
    stroke: var(--el-color-warning, #e6a23c);
    stroke-width: 1.7;
    stroke-dasharray: 3 6;
    opacity: 0.95;
  }
  &.is-preview {
    stroke: var(--el-color-primary-light-5, #a0cfff);
    opacity: 0.7;
    stroke-dasharray: 2.5 6;
  }
}
.defect-gantt-marquee-rect {
  position: absolute;
  z-index: 8;
  pointer-events: none;
  border: 1px dashed var(--el-color-primary, #409eff);
  background: rgba(64, 158, 255, 0.12);
  box-sizing: border-box;
}
.defect-gantt-bar.is-baseline {
  z-index: 2;
  top: 10px;
  height: calc(100% - 16px);
  opacity: 0.35;
  background: var(--el-text-color-secondary, #909399) !important;
  border-color: transparent !important;
  pointer-events: none;
  box-shadow: none;
  outline: none;
}
.defect-gantt-axis-body.is-critical-mode {
  .defect-gantt-bar:not(.is-critical):not(.is-baseline):not(.is-drop-preview) {
    opacity: 0.38;
    filter: saturate(0.55);
  }
}
.defect-gantt-bar.is-critical {
  outline: 2px solid var(--el-color-danger, #f56c6c);
  outline-offset: 1px;
  box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.45);
  z-index: 5;
  opacity: 1;
  filter: none;
}
</style>