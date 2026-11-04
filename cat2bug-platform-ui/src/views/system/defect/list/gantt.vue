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
              <el-dropdown-item divided class="defect-gantt-layers-item" @click.stop.prevent>
                <el-checkbox
                  v-model="autoLevelOnDrag"
                  size="small"
                  :disabled="!canEdit"
                  :title="$t('defect.gantt.drag-yield-tip')"
                  @click.stop
                >
                  {{ $t('defect.gantt.drag-yield') }}
                </el-checkbox>
              </el-dropdown-item>
              <el-dropdown-item divided class="defect-gantt-layers-item" @click.stop.prevent>
                <el-checkbox v-model="showBarHandler" size="small" @click.stop>
                  {{ $t('defect.gantt.layer-handler') }}
                </el-checkbox>
              </el-dropdown-item>
              <el-dropdown-item class="defect-gantt-layers-item" @click.stop.prevent>
                <el-checkbox v-model="showBarDays" size="small" @click.stop>
                  {{ $t('defect.gantt.layer-days') }}
                </el-checkbox>
              </el-dropdown-item>
              <el-dropdown-item class="defect-gantt-layers-item" @click.stop.prevent>
                <el-checkbox v-model="showBarDefectTitle" size="small" @click.stop>
                  {{ $t('defect.gantt.layer-defect-title') }}
                </el-checkbox>
              </el-dropdown-item>
              <el-dropdown-item divided class="defect-gantt-layers-item" @click.stop.prevent>
                <el-checkbox v-model="showMinimap" size="small" @click.stop>
                  {{ $t('defect.gantt.layer-minimap') }}
                </el-checkbox>
              </el-dropdown-item>
              <el-dropdown-item
                divided
                :disabled="!canFreezeBaseline"
                class="defect-gantt-layers-action"
                @click="confirmFreezeBaseline"
              >
                <span class="defect-gantt-layers-action-label">
                  <el-icon><RefreshRight /></el-icon>
                  <span>{{ $t('defect.gantt.freeze-baseline') }}</span>
                </span>
              </el-dropdown-item>
              <el-dropdown-item
                :disabled="!canEdit || autoLevelLoading || autoLevelApplying"
                class="defect-gantt-layers-action"
                :title="$t('defect.gantt.auto-level-tip')"
                @click="startAutoLevel"
              >
                <span class="defect-gantt-layers-action-label">
                  <el-icon><Sort /></el-icon>
                  <span>{{ $t('defect.gantt.auto-level') }}</span>
                </span>
              </el-dropdown-item>
              <el-dropdown-item
                class="defect-gantt-layers-action"
                @click="openWorkCalendar"
              >
                <span class="defect-gantt-layers-action-label">
                  <el-icon><Calendar /></el-icon>
                  <span>{{ $t('defect.gantt.work-calendar') }}</span>
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

    <div v-if="autoLevelPreview" class="defect-gantt-auto-level-banner">
      <div class="defect-gantt-auto-level-banner-text">
        {{
          $t('defect.gantt.auto-level-preview', {
            n: autoLevelPreview.summary.changed,
            earlier: autoLevelPreview.summary.earlier,
            later: autoLevelPreview.summary.later
          })
        }}
        <span class="defect-gantt-auto-level-banner-note">{{ $t('defect.gantt.auto-level-air-note') }}</span>
        <el-checkbox
          v-model="autoLevelIgnoreCalendar"
          size="small"
          class="defect-gantt-auto-level-ignore-cal"
          :title="$t('defect.gantt.ignore-calendar-tip')"
          :disabled="autoLevelApplying"
          @change="rebuildAutoLevelPreview"
        >
          {{ $t('defect.gantt.ignore-calendar') }}
        </el-checkbox>
      </div>
      <div class="defect-gantt-auto-level-banner-actions">
        <el-button size="small" :disabled="autoLevelApplying" @click="cancelAutoLevel">
          {{ $t('defect.gantt.auto-level-cancel') }}
        </el-button>
        <el-button
          type="primary"
          size="small"
          :loading="autoLevelApplying"
          @click="confirmAutoLevel"
        >
          {{ $t('defect.gantt.auto-level-apply') }}
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="defect-gantt-body">
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
                <el-tab-pane v-if="deliverableModuleOn" name="module">
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
                      @click="onScheduledRowClick($event, row, g)"
                    >
                      <span class="defect-gantt-row-num">#{{ row.defect.projectNum }}</span>
                      <span class="defect-gantt-row-name" :title="row.defect.defectName">{{ row.defect.defectName }}</span>
                      <el-tooltip
                        v-if="isMultiHandlerRow(row)"
                        placement="top"
                        :content="multiHandlerTip(row)"
                      >
                        <svg-icon
                          icon-class="peoples"
                          class-name="defect-gantt-multi-icon"
                        />
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
                    v-for="(u, ui) in axisUnits"
                    :key="u.key"
                    class="defect-gantt-axis-unit"
                    :class="{
                      'is-rest': (!!u.weekOff || !!u.holiday) && !u.makeup,
                      holiday: !!u.holiday,
                      makeup: !!u.makeup,
                      'is-day-scale': scale === 'day',
                      'is-week-scale': scale === 'week'
                    }"
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
                    <span
                      class="defect-gantt-col-resize"
                      :title="$t('defect.gantt.resize-col')"
                      @mousedown.stop.prevent="startColResize($event, ui)"
                    />
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
                  :class="{
                    'is-rest': (!!u.weekOff || !!u.holiday) && !u.makeup,
                    holiday: !!u.holiday,
                    makeup: !!u.makeup
                  }"
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
                        @click="onAxisRowClick($event, row)"
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
                          v-if="autoLevelGhostStyle(row)"
                          class="defect-gantt-bar is-auto-level-ghost"
                          :class="'state-' + stateClass(row.defect)"
                          :style="autoLevelGhostStyle(row)"
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
                              'is-plan-readonly': !canEditPlanDefect(row.defect),
                              'is-critical': showCritical && isCriticalDefect(row.defect),
                              'is-link-target': isLinkDragTarget(row.defect),
                              'is-link-target-invalid': isLinkDragTargetInvalid(row.defect),
                              'is-auto-level-push': isAutoLevelPushBar(row.defect),
                              'is-auto-level-proposal': isAutoLevelProposalBar(row.defect),
                              'is-multi-handler': isMultiHandlerRow(row)
                            }
                          ]"
                          :style="barStyle(row)"
                          :data-defect-id="row.defect.defectId"
                          data-gantt-bar="1"
                          :title="barHoverTitle(row)"
                          @dragstart.prevent.stop
                          @mousedown.stop="onBarMouseDown($event, row, g, 'move')"
                          @click.stop="onBarClick($event, row.defect)"
                        >
                          <div class="defect-gantt-bar-rest-mask" aria-hidden="true">
                            <i
                              v-for="(seg, si) in barRestSegments(row)"
                              :key="'rest-' + row.rowKey + '-' + si"
                              class="defect-gantt-bar-rest"
                              :style="{ left: seg.left + 'px', width: seg.width + 'px' }"
                            />
                          </div>
                          <el-tooltip
                            v-if="isMultiHandlerRow(row)"
                            placement="top"
                            :content="multiHandlerTip(row)"
                          >
                            <span class="defect-gantt-bar-multi" @mousedown.stop @click.stop>
                              <svg-icon icon-class="peoples" class-name="defect-gantt-bar-multi-svg" />
                            </span>
                          </el-tooltip>
                          <!-- 拉宽手柄始终可用；关联线开启时端口在条外，互不顶替；条内不显示文字 -->
                          <span
                            v-if="canEditPlanDefect(row.defect)"
                            class="defect-gantt-bar-handle left"
                            @mousedown.stop="onBarMouseDown($event, row, g, 'start')"
                          />
                          <span
                            v-if="showDeps && canEditDefect"
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
                            v-if="showDeps && canEditDefect"
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
                            v-if="canEditPlanDefect(row.defect)"
                            class="defect-gantt-bar-handle right"
                            @mousedown.stop="onBarMouseDown($event, row, g, 'end')"
                          />
                        </div>
                        <div
                          v-if="showBarMeta && barMetaVisible(row, g)"
                          class="defect-gantt-bar-meta"
                          :style="barMetaStyle(row)"
                          @mousedown.stop
                          @click.stop
                        >
                          <row-list-member
                            v-if="showBarHandler && barHandlersOf(row, g).length"
                            class="defect-gantt-bar-handlers"
                            :members="barHandlersOf(row, g)"
                            :size="22"
                            :max="5"
                            :title="barHandlersTitle(row, g)"
                          />
                          <span
                            v-if="showBarDays && barDaysText(row)"
                            class="defect-gantt-bar-days"
                            :title="barDaysText(row)"
                          >{{ barDaysText(row) }}</span>
                          <span
                            v-if="showBarDefectTitle && row.defect"
                            class="defect-gantt-bar-defect-title"
                            :title="barDefectTitleText(row)"
                          >{{ barDefectTitleText(row) }}</span>
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
                  :class="{ 'is-interactive': canEditDefect && !linkDrag }"
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

          <!-- 时间轴缩略导航 -->
          <div
            v-if="showMinimap"
            ref="minimapPanel"
            class="defect-gantt-minimap"
            :class="{ 'is-docked': !minimapPos }"
            :style="minimapPanelStyle"
            @mousedown.stop
            @pointerdown.stop
          >
            <div
              class="defect-gantt-minimap-title"
              @mousedown.prevent="startMinimapPanelDrag"
            >
              <span class="defect-gantt-minimap-drag-icon" aria-hidden="true">
                <svg viewBox="0 0 16 16" width="12" height="12" focusable="false">
                  <circle cx="5" cy="3" r="1.35" fill="currentColor" />
                  <circle cx="11" cy="3" r="1.35" fill="currentColor" />
                  <circle cx="5" cy="8" r="1.35" fill="currentColor" />
                  <circle cx="11" cy="8" r="1.35" fill="currentColor" />
                  <circle cx="5" cy="13" r="1.35" fill="currentColor" />
                  <circle cx="11" cy="13" r="1.35" fill="currentColor" />
                </svg>
              </span>
              <span class="defect-gantt-minimap-title-text">{{ $t('defect.gantt.minimap-title') }}</span>
              <button
                type="button"
                class="defect-gantt-minimap-close"
                :title="$t('defect.gantt.minimap-close')"
                :aria-label="$t('defect.gantt.minimap-close')"
                @mousedown.stop
                @click.stop.prevent="closeMinimap"
              >
                <el-icon><CloseIcon /></el-icon>
              </button>
            </div>
            <div
              ref="minimapBody"
              class="defect-gantt-minimap-body"
              @mousedown.prevent="startMinimapNav"
            >
              <div
                v-for="m in minimapBarMarks"
                :key="m.key"
                class="defect-gantt-minimap-bar"
                :class="'state-' + m.state"
                :style="{
                  left: m.left + 'px',
                  top: m.top + 'px',
                  width: m.width + 'px',
                  height: m.height + 'px'
                }"
              />
              <div
                ref="minimapViewportEl"
                class="defect-gantt-minimap-viewport"
              />
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
                <span class="defect-gantt-unscheduled-count">({{ filteredUnscheduled.length }}{{ unscheduledFilterActive ? '/' + unscheduled.length : '' }})</span>
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
            <select-project-member
              v-if="!unscheduledCollapsed"
              v-model:memberId="unscheduledHandlerIds"
              class="defect-gantt-unscheduled-handler"
              :project-id="projectId"
              placeholder="defect.gantt.unscheduled-handler-placeholder"
              :is-head="false"
              :multiple="true"
              :role-group="false"
              size="small"
              icon="el-icon-user"
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
                  'is-dragging': pendingUnscheduled && pendingUnscheduled.defectId === d.defectId,
                  'is-plan-readonly': !canEditPlanDefect(d)
                }
              ]"
              :draggable="canEditPlanDefect(d)"
              :title="chipHoverTitle(d)"
              @click="selectUnscheduledDefect(d)"
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
      <div
        v-if="hasMoreDefects || loadingMore"
        class="defect-gantt-cap-hint"
        :class="{ 'is-loading': loadingMore }"
      >
        <span v-if="loadingMore">{{ $t('defect.gantt.loading-more') }}</span>
        <span v-else>
          {{ $t('defect.gantt.load-progress', { loaded: defectList.length, total }) }}
        </span>
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
    <project-work-calendar-dialog ref="workCalendarDialog" @saved="onWorkCalendarSaved" />
  </div>
</template>

<script>
import { ArrowDown, ArrowRight, Calendar, Close as CloseIcon, Operation, RefreshRight, Search, Sort, User } from '@element-plus/icons-vue'
import {
  listDefect,
  updateDefectPlanNode,
  getDefectPlanGanttMeta,
  createDefectPlanLink,
  deleteDefectPlanLink,
  freezeDefectPlanBaseline
} from '@/api/system/defect'
import { getProjectWorkCalendar } from '@/api/system/project-work-calendar'
import { normalizeWorkCalendar } from '@/utils/work-calendar'
import ProjectWorkCalendarDialog from '@/views/system/project/work-calendar/index.vue'
import { getProject } from '@/api/system/project'
import { listModulePath } from '@/api/system/module'
import { checkPermi } from '@/utils/permission'
import teamModuleGate from '@/mixins/team-module-gate'
import { useUserStore } from '@/stores/user'
import Cat2BugAvatar from '@/components/Cat2BugAvatar'
import RowListMember from '@/components/RowListMember'
import SelectProjectMember from '@/components/Project/SelectProjectMember'
import {
  GANTT_PAGE_SIZE,
  GANTT_LOAD_MORE_THRESHOLD_PX,
  GANTT_VIEWPORT_AUTO_FETCH_MAX,
  GANTT_GROUP_BY,
  GANTT_SCALE,
  GANTT_CACHE,
  GANTT_MINIMAP_WIDTH,
  GANTT_MINIMAP_HEIGHT,
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
  GANTT_COL_WIDTH_LIMITS,
  defaultColWidths,
  parseStoredColWidths,
  resolveColWidth,
  coarserScale,
  finerScale,
  wideThreshold,
  projectGanttRows,
  splitModulePath,
  defectStateClass,
  resolveHandlers,
  defaultPlanRangeFromDropDay,
  shiftPlanRange,
  resizePlanRange,
  computeAxisRange,
  shiftAxisBound,
  axisExtendChunkUnits,
  buildAxisUnits,
  buildAxisMajorBands,
  dateToOffsetPx,
  offsetPxToDate,
  pxPerDay,
  snapDurationDays,
  barGeometry,
  barRestOverlays,
  parsePlanDate,
  isoWeekNumber,
  planDurationDays,
  formatDurationDays,
  computeCriticalPathIds,
  buildHandlerLoadTrackSegments,
  findFsScheduleConflicts,
  canAutoLevelEditDefect,
  canEditGanttPlanDefect,
  filterAutoLevelCandidates,
  levelDefectPlans,
  primaryHandlerId,
  pushOverlapsAfterPinned,
  isProjectPlanAdmin,
  defaultGanttWorkCalendar
} from './defect-gantt'

const AUTO_LEVEL_PAGE_SIZE = 100
const AUTO_LEVEL_UPDATE_CONCURRENCY = 6

export default {
  name: 'DefectGantt',
  mixins: [teamModuleGate],
  components: {
    ArrowDown,
    ArrowRight,
    Calendar,
    CloseIcon,
    Operation,
    RefreshRight,
    Search,
    Sort,
    User,
    Cat2BugAvatar,
    RowListMember,
    SelectProjectMember,
    ProjectWorkCalendarDialog
  },
  setup() {
    const userStore = useUserStore()
    return { Search, userStore }
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
    let colWidths = defaultColWidths()
    try {
      const rawCol = cache && cache.get(GANTT_CACHE.COL_WIDTHS)
      if (rawCol) colWidths = parseStoredColWidths(rawCol)
    } catch (_) { /* ignore */ }
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
    /** 条后处理人头像 / 天数 / 缺陷标题（默认关） */
    let showBarHandler = false
    let showBarDays = false
    let showBarDefectTitle = false
    /** 拖动时自动把同人重叠条往后挤（默认关） */
    let autoLevelOnDrag = false
    /** 缩略图默认开；仅当本地显式存了 minimap 键时才覆盖 */
    let showMinimap = true
    try {
      const rawLayers = cache && cache.get(GANTT_CACHE.LAYERS)
      if (rawLayers) {
        const parsed = typeof rawLayers === 'string' ? JSON.parse(rawLayers) : rawLayers
        if (parsed && typeof parsed === 'object') {
          showDeps = !!parsed.deps
          showCritical = !!parsed.critical
          showLoad = !!parsed.load
          showBaseline = !!parsed.baseline
          // handler：新键；兼容曾写入的 handlerAvatar
          showBarHandler = !!(parsed.handler != null ? parsed.handler : parsed.handlerAvatar)
          showBarDays = !!parsed.days
          showBarDefectTitle = !!parsed.defectTitle
          autoLevelOnDrag = !!parsed.autoLevel
          if (Object.prototype.hasOwnProperty.call(parsed, 'minimap')) {
            showMinimap = !!parsed.minimap
          }
        }
      }
    } catch (_) { /* ignore */ }
    let minimapPos = null
    try {
      const rawPos = cache && cache.get(GANTT_CACHE.MINIMAP_POS)
      if (rawPos) {
        const parsed = typeof rawPos === 'string' ? JSON.parse(rawPos) : rawPos
        if (
          parsed &&
          typeof parsed === 'object' &&
          Number.isFinite(Number(parsed.x)) &&
          Number.isFinite(Number(parsed.y))
        ) {
          minimapPos = { x: Number(parsed.x), y: Number(parsed.y) }
        }
      }
    } catch (_) { /* ignore */ }
    return {
      loading: false,
      /** 触底 / 首屏补高追加中（与首屏 loading 分开，避免整页遮罩） */
      loadingMore: false,
      defectList: [],
      total: 0,
      queryParams: this.query || {},
      requestGen: 0,
      /** 已成功拉到的页码（search 重置为 1） */
      listPageNum: 1,
      pageSize: GANTT_PAGE_SIZE,
      /** 首屏高度不足时自动续拉计数 */
      viewportAutoFetchCount: 0,
      groupBy,
      scale,
      /** 每档列宽记忆 { day, week, month } */
      colWidths,
      /** 格缝拖拽态 */
      colResize: null,
      /** 切档后短暂忽略反向阈值（ms 时间戳） */
      _colResizeHysteresisUntil: 0,
      /** 无限滚动：可向两侧延展的活动轴范围（null 时回退 computeAxisRange） */
      axisLiveStart: null,
      axisLiveEnd: null,
      _axisExtending: false,
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
      /** 未排期池本地处理人过滤（空数组=全部；与 SelectProjectMember 一致为 id[]） */
      unscheduledHandlerIds: [],
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
      showBarHandler,
      showBarDays,
      showBarDefectTitle,
      autoLevelOnDrag,
      /** 拖动挤让时缓存是否项目管理员（懒加载） */
      autoLevelIsAdmin: null,
      /** 一键自动排列预览 { proposals, summary, universe } */
      autoLevelPreview: null,
      autoLevelLoading: false,
      autoLevelApplying: false,
      /** 本次自动排列是否忽略工作日历 */
      autoLevelIgnoreCalendar: false,
      /** 项目工作日历快照 */
      workCalendar: defaultGanttWorkCalendar(),
      workCalendarEditable: false,
      /** 缩略图（默认开） */
      showMinimap,
      /** 缩略图面板位置；null 表示右下角停靠 */
      minimapPos,
      minimapWidth: GANTT_MINIMAP_WIDTH,
      minimapHeight: GANTT_MINIMAP_HEIGHT,
      /** 轴视口在缩略图中的映射（随 scroll/resize 更新） */
      minimapViewport: { left: 0, top: 0, width: 1, height: 1 },
      minimapPanelDrag: null,
      minimapNavDrag: null,
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
      /** Shift 区间选锚点 defectId */
      selectionAnchorId: null,
      /** 框选拖拽状态 */
      marqueeDrag: null,
      marqueeRect: null,
      _metaGen: 0,
      _onLinkKeyDown: null
    }
  },
  mounted() {
    this.expandAxisLiveToCoverBase()
    this.bindAxisViewportObserver()
    this.ensureModulePathMap()
    this.loadWorkCalendar()
    this.ensureAutoLevelAdmin()
    this.$nextTick(() => {
      if (this.minimapPos) {
        this.minimapPos = this.clampMinimapPos(this.minimapPos.x, this.minimapPos.y)
      }
      this.updateMinimapViewport()
      this.maybeExtendAxisOnScroll()
    })
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
    /** 甘特计划节点拖改（独立权限，不复用 defect:edit） */
    canEdit() {
      return checkPermi(['system:defect:gantt:plan'])
    },
    /** 依赖线建删、冻基线入口仍走缺陷修改权 */
    canEditDefect() {
      return checkPermi(['system:defect:edit'])
    },
    /** 冻基线：需缺陷编辑权 + 项目创建人/管理员 */
    canFreezeBaseline() {
      return this.canEditDefect && !!this.autoLevelIsAdmin
    },
    planEditCtx() {
      return {
        userId: this.currentUserId(),
        isProjectAdmin: !!this.autoLevelIsAdmin
      }
    },
    projected() {
      const qp = this.queryParams || this.query || {}
      return projectGanttRows(this.defectList, this.groupBy, this.modulePathById, {
        handlerIds: qp.handleBy
      })
    },
    hasMoreDefects() {
      const loaded = Array.isArray(this.defectList) ? this.defectList.length : 0
      const total = Number(this.total)
      return Number.isFinite(total) && total > loaded
    },
    showBarMeta() {
      return !!(this.showBarHandler || this.showBarDays || this.showBarDefectTitle)
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
    unscheduledHandlerIdSet() {
      const raw = this.unscheduledHandlerIds
      if (!Array.isArray(raw) || !raw.length) return null
      const set = new Set()
      raw.forEach((id) => {
        const n = Number(id)
        if (Number.isFinite(n)) set.add(n)
      })
      return set.size ? set : null
    },
    unscheduledFilterActive() {
      return !!(this.unscheduledKeywordTrimmed || this.unscheduledHandlerIdSet)
    },
    filteredUnscheduled() {
      const list = this.unscheduled || []
      const q = this.unscheduledKeywordTrimmed.toLowerCase()
      const handlerSet = this.unscheduledHandlerIdSet
      if (!q && !handlerSet) return list
      const qNum = q.replace(/^#/, '')
      return list.filter((d) => {
        if (!d) return false
        if (handlerSet) {
          const handlers = resolveHandlers(d)
          if (!handlers.some((h) => handlerSet.has(Number(h.userId)))) return false
        }
        if (!q) return true
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
      if (this.axisLiveStart && this.axisLiveEnd) {
        return { start: this.axisLiveStart, end: this.axisLiveEnd }
      }
      return computeAxisRange(this.scheduledDefects, this.scale)
    },
    axisUnits() {
      return buildAxisUnits(this.axisRange.start, this.axisRange.end, this.scale, this.workCalendar)
    },
    activeWorkCalendar() {
      return this.workCalendar || defaultGanttWorkCalendar()
    },
    axisMajorBands() {
      return buildAxisMajorBands(this.axisUnits, this.scale)
    },
    colWidth() {
      return resolveColWidth(this.scale, this.colWidths)
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
        map.set(
          id,
          buildHandlerLoadTrackSegments(
            id,
            defects,
            rangeStart,
            scale,
            1,
            axisW,
            this.colWidth,
            this.activeWorkCalendar
          )
        )
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
      const x = dateToOffsetPx(new Date(), this.axisRange.start, this.scale, this.colWidth)
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
            let geo = barGeometry(defect, this.axisRange.start, this.scale, this.colWidth)
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
    },
    minimapPanelStyle() {
      const pos = this.minimapPos
      if (!pos) return null
      return {
        left: pos.x + 'px',
        top: pos.y + 'px',
        right: 'auto',
        bottom: 'auto'
      }
    },
    /**
     * 缩略图色块：与主轴相同的组/行遍历（含处理人镜像每行），
     * 按 axis 内容尺寸比例映射到固定 mini 尺寸。
     */
    minimapBarMarks() {
      if (!this.showMinimap) return []
      const contentW = Math.max(1, this.axisWidthPx || 0)
      const contentH = Math.max(1, this.axisContentHeight || 0)
      const mw = this.minimapWidth
      const mh = this.minimapHeight
      const rowH = this.rowHeight
      const marks = []
      let y = 0
      const groups = this.groups || []
      for (let gi = 0; gi < groups.length; gi++) {
        const g = groups[gi]
        y += this.groupHeaderHeight
        if (this.isGroupCollapsed(g.key)) continue
        const rows = g.rows || []
        for (let ri = 0; ri < rows.length; ri++) {
          const row = rows[ri]
          const defect = row && row.defect
          const geo = defect ? barGeometry(defect, this.axisRange.start, this.scale, this.colWidth) : null
          if (geo && geo.width > 0) {
            const left = (geo.x / contentW) * mw
            const width = Math.max(2, (geo.width / contentW) * mw)
            const bandH = Math.max(1, (rowH / contentH) * mh)
            const barH = Math.max(1, Math.min(bandH, bandH * 0.55))
            const top = (y / contentH) * mh + (bandH - barH) / 2
            marks.push({
              key: row.rowKey || `mm-${gi}-${ri}`,
              left: Math.max(0, Math.min(mw - 2, left)),
              top: Math.max(0, Math.min(mh - 1, top)),
              width: Math.min(width, mw - Math.max(0, left)),
              height: barH,
              state: this.stateClass(defect)
            })
          }
          y += rowH
        }
        if (this.shouldShowLoadTrack(g)) y += this.loadTrackHeight
      }
      return marks
    }
  },
  watch: {
    deliverableModuleOn: {
      immediate: true,
      handler(on) {
        if (!on && this.groupBy === 'module') this.groupBy = 'handler'
      }
    },
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
    },
    showBarHandler() {
      this.persistGanttLayers()
    },
    showBarDays() {
      this.persistGanttLayers()
    },
    showBarDefectTitle() {
      this.persistGanttLayers()
    },
    autoLevelOnDrag() {
      this.persistGanttLayers()
      if (this.autoLevelOnDrag) this.ensureAutoLevelAdmin()
    },
    showMinimap(val) {
      this.persistGanttLayers()
      if (val) {
        this.$nextTick(() => this.updateMinimapViewport())
      } else {
        this.teardownMinimapPanelDrag()
        this.teardownMinimapNav()
      }
    },
    defectList() {
      this.expandAxisLiveToCoverBase()
    },
    scale() {
      this.resetAxisLiveRange()
    }
  },
  beforeUnmount() {
    if (this._ganttLoadMoreTimer) {
      clearTimeout(this._ganttLoadMoreTimer)
      this._ganttLoadMoreTimer = null
    }
    this.teardownDragListeners()
    this.teardownResizeLeft()
    this.teardownResizeUnscheduled()
    this.teardownColResize()
    this.teardownAxisViewportObserver()
    this.teardownMinimapPanelDrag()
    this.teardownMinimapNav()
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
      this.expandAxisLiveToCoverBase()
    },
    /** 按缺陷基线重置活动轴（切尺度时） */
    resetAxisLiveRange() {
      const base = computeAxisRange(this.scheduledDefects, this.scale)
      this.axisLiveStart = base.start
      this.axisLiveEnd = base.end
    },
    /** 缺陷变化时只外扩、不收缩，保留用户已滚过的空白轴 */
    expandAxisLiveToCoverBase() {
      const base = computeAxisRange(this.scheduledDefects, this.scale)
      if (!this.axisLiveStart || !this.axisLiveEnd) {
        this.axisLiveStart = base.start
        this.axisLiveEnd = base.end
        return
      }
      if (base.start.getTime() < this.axisLiveStart.getTime()) {
        this.axisLiveStart = base.start
      }
      if (base.end.getTime() > this.axisLiveEnd.getTime()) {
        this.axisLiveEnd = base.end
      }
    },
    /**
     * 滚近左右边缘时向过去/未来延展；向前延展后补偿 scrollLeft，视口不跳。
     * @param {number} [depth] 递归深度上限，避免窄内容死循环
     */
    maybeExtendAxisOnScroll(depth = 0) {
      if (depth > 4) return
      if (this._axisExtending || this.colResize || this.syncingScroll) return
      const body = this.$refs.axisBodyScroll
      if (!body || body.clientWidth <= 0) return
      const threshold = Math.max(body.clientWidth * 0.8, this.colWidth * 4)
      const maxScroll = Math.max(0, body.scrollWidth - body.clientWidth)
      const chunk = axisExtendChunkUnits(this.scale, this.colWidth, body.clientWidth)
      /*
       * 整轴尚不足一屏：左右都会「贴边」。只向未来扩出可滚空间，
       * 避免 before+after 递归把轴无限拉长卡死。
       */
      if (maxScroll <= 2) {
        this.extendAxisAfter(chunk, depth)
        return
      }
      const nearLeft = body.scrollLeft <= threshold
      const nearRight = body.scrollLeft >= maxScroll - threshold
      if (nearLeft) this.extendAxisBefore(chunk, depth)
      else if (nearRight) this.extendAxisAfter(chunk, depth)
    },
    extendAxisBefore(unitCount, depth = 0) {
      const n = Math.max(1, Math.floor(Number(unitCount) || 0))
      if (!this.axisLiveStart || !this.axisLiveEnd) this.expandAxisLiveToCoverBase()
      const oldStart = this.axisLiveStart
      const newStart = shiftAxisBound(oldStart, this.scale, -n)
      if (!newStart || newStart.getTime() >= oldStart.getTime()) return
      const prependPx = dateToOffsetPx(oldStart, newStart, this.scale, this.colWidth)
      if (!(prependPx > 0)) return
      this._axisExtending = true
      this.axisLiveStart = newStart
      this.$nextTick(() => {
        const body = this.$refs.axisBodyScroll
        const header = this.$refs.axisHeaderScroll
        this.syncingScroll = true
        if (body) body.scrollLeft += prependPx
        if (header && body) header.scrollLeft = body.scrollLeft
        this.syncingScroll = false
        this._axisExtending = false
        this.updateMinimapViewport()
        this.$nextTick(() => this.maybeExtendAxisOnScroll(depth + 1))
      })
    },
    extendAxisAfter(unitCount, depth = 0) {
      const n = Math.max(1, Math.floor(Number(unitCount) || 0))
      if (!this.axisLiveStart || !this.axisLiveEnd) this.expandAxisLiveToCoverBase()
      const oldEnd = this.axisLiveEnd
      const newEnd = shiftAxisBound(oldEnd, this.scale, n)
      if (!newEnd || newEnd.getTime() <= oldEnd.getTime()) return
      this._axisExtending = true
      this.axisLiveEnd = newEnd
      this.$nextTick(() => {
        this._axisExtending = false
        this.updateMinimapViewport()
        this.$nextTick(() => this.maybeExtendAxisOnScroll(depth + 1))
      })
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
    /**
     * 与表格/Excel 同一套通用查询；首屏只拉一段，其余触底 / 首屏补高续拉。
     * @param {Object} [params] 父页 queryParams；缺省时用当前 prop/本地副本
     */
    search(params) {
      const gen = ++this.requestGen
      this.loading = true
      this.loadingMore = false
      this.listPageNum = 1
      this.viewportAutoFetchCount = 0
      const base =
        params && typeof params === 'object'
          ? params
          : (this.query && typeof this.query === 'object' ? this.query : (this.queryParams || {}))
      this.queryParams = base
      this.ensureModulePathMap()
      const nested =
        base.params && typeof base.params === 'object' ? { ...base.params } : {}
      const req = {
        ...base,
        params: nested,
        pageNum: 1,
        pageSize: this.pageSize
      }
      listDefect(req)
        .then((response) => {
          if (gen !== this.requestGen) return
          this.defectList = Array.isArray(response.rows) ? response.rows : []
          this.total = Number(response.total) || 0
          this.listPageNum = 1
          this.fetchPlanGanttMeta(gen)
          this.$nextTick(() => {
            this.measureAxisViewport()
            this.scrollToToday(false)
            this.maybeFillGanttViewportUntilScrollable()
          })
        })
        .catch(() => {
          if (gen !== this.requestGen) return
        })
        .finally(() => {
          if (gen === this.requestGen) this.loading = false
        })
    },
    /** 首屏内容高度不足一屏时自动续拉（受 GANTT_VIEWPORT_AUTO_FETCH_MAX 限制） */
    maybeFillGanttViewportUntilScrollable() {
      if (this.loading || this.loadingMore || !this.hasMoreDefects) return
      if (this.viewportAutoFetchCount >= GANTT_VIEWPORT_AUTO_FETCH_MAX) return
      const axis = this.$refs.axisBodyScroll
      if (!axis || axis.clientHeight <= 0) return
      /* 内容已超过视口则不必预拉 */
      if (axis.scrollHeight > axis.clientHeight + 40) return
      this.tryLoadMoreDefects({ fromViewportFill: true })
    },
    /**
     * 纵滚接近底部时追加下一段。
     * @param {{ fromViewportFill?: boolean }} [opts]
     */
    tryLoadMoreDefects(opts) {
      if (this.loading || this.loadingMore || !this.hasMoreDefects) return
      const fromViewportFill = !!(opts && opts.fromViewportFill)
      if (fromViewportFill && this.viewportAutoFetchCount >= GANTT_VIEWPORT_AUTO_FETCH_MAX) return
      const axis = this.$refs.axisBodyScroll
      const left = this.$refs.leftScroll
      const scroller = axis || left
      if (!fromViewportFill) {
        if (!scroller) return
        const remain =
          scroller.scrollHeight - scroller.scrollTop - scroller.clientHeight
        if (remain > GANTT_LOAD_MORE_THRESHOLD_PX) return
      }
      const gen = this.requestGen
      const nextPage = this.listPageNum + 1
      const base = this.queryParams || this.query || {}
      const nested =
        base.params && typeof base.params === 'object' ? { ...base.params } : {}
      if (fromViewportFill) this.viewportAutoFetchCount += 1
      this.loadingMore = true
      listDefect({
        ...base,
        params: nested,
        pageNum: nextPage,
        pageSize: this.pageSize
      })
        .then((response) => {
          if (gen !== this.requestGen) return
          const rows = Array.isArray(response.rows) ? response.rows : []
          if (response.total != null && response.total !== '') {
            this.total = Number(response.total) || this.total
          }
          if (!rows.length) {
            this.total = this.defectList.length
            return
          }
          const seen = new Set(
            (this.defectList || [])
              .map((d) => (d && d.defectId != null ? Number(d.defectId) : null))
              .filter((id) => id != null && Number.isFinite(id))
          )
          const appended = rows.filter((d) => {
            const id = d && d.defectId != null ? Number(d.defectId) : null
            if (id == null || !Number.isFinite(id) || seen.has(id)) return false
            seen.add(id)
            return true
          })
          if (appended.length) {
            this.defectList = this.defectList.concat(appended)
            this.listPageNum = nextPage
            this.fetchPlanGanttMeta(gen)
          } else if (rows.length < this.pageSize) {
            this.total = this.defectList.length
          } else {
            /* 本页全重复（排序抖动等）：仍推进页码避免死循环 */
            this.listPageNum = nextPage
          }
          this.$nextTick(() => {
            this.maybeFillGanttViewportUntilScrollable()
          })
        })
        .catch(() => {
          if (gen !== this.requestGen) return
        })
        .finally(() => {
          if (gen === this.requestGen) this.loadingMore = false
        })
    },
    scheduleGanttLoadMoreCheck() {
      if (this._ganttLoadMoreTimer) clearTimeout(this._ganttLoadMoreTimer)
      this._ganttLoadMoreTimer = setTimeout(() => {
        this._ganttLoadMoreTimer = null
        this.tryLoadMoreDefects()
      }, 120)
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
      this.updateMinimapViewport()
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
    isMultiHandlerRow(row) {
      if (!row || !row.defect) return false
      if (row.mirrored) return true
      return resolveHandlers(row.defect).length > 1
    },
    multiHandlerTip(row) {
      if (!row || !row.defect) return ''
      const others = Array.isArray(row.otherHandlers) ? row.otherHandlers : []
      if (others.length) {
        const names = others.map((h) => h.nickName).filter(Boolean)
        return this.$t('defect.gantt.mirror-tip', { names: names.join(', ') })
      }
      const names = resolveHandlers(row.defect).map((h) => h.nickName).filter(Boolean)
      return this.$t('defect.gantt.multi-handler-tip', { names: names.join(', ') })
    },
    formatDefectDays(defect) {
      return formatDurationDays(planDurationDays(defect, this.activeWorkCalendar))
    },
    groupTotalDays(group) {
      const rows = (group && group.rows) || []
      let sum = 0
      let any = false
      for (let i = 0; i < rows.length; i++) {
        const d = planDurationDays(rows[i] && rows[i].defect, this.activeWorkCalendar)
        if (d == null || !Number.isFinite(d)) continue
        sum += d
        any = true
      }
      return any ? sum : null
    },
    formatGroupDays(group) {
      return formatDurationDays(this.groupTotalDays(group))
    },
    /** 当前条用于几何/网纹的计划起止（含拖动预览、自动排列推挤） */
    barPlanSource(row) {
      if (!row || !row.defect) return null
      const defectId = row.defect.defectId != null ? Number(row.defect.defectId) : null
      const ds = this.dragState
      /* 正在拖的条：跟手改期，网纹按预览计划实时重算 */
      if (
        ds &&
        ds.activated &&
        ds.preview &&
        ds.rowKey === row.rowKey &&
        !(this.dropInsertPreview && this.dropInsertPreview.kind !== 'forbidden')
      ) {
        const live = this.planRangeFromDragPreview(ds)
        if (live) return live
      }
      /* 多选平移中的同伴条 */
      if (
        ds &&
        ds.activated &&
        ds.mode === 'move' &&
        ds.preview &&
        defectId != null &&
        Array.isArray(ds.batchIds) &&
        ds.batchIds.some((id) => Number(id) === defectId) &&
        ds.rowKey !== row.rowKey
      ) {
        const dayDelta = Math.round((ds.preview.x - ds.originX) / this.colWidth)
        const shifted = shiftPlanRange(
          row.defect.planStartTime,
          row.defect.planEndTime,
          this.scaleDayDelta(dayDelta),
          this.activeWorkCalendar
        )
        if (shifted) return shifted
      }
      const push =
        ds &&
        ds.autoLevelPushes &&
        defectId != null &&
        ds.autoLevelPushes[defectId]
      if (push) {
        return { planStartTime: push.planStartTime, planEndTime: push.planEndTime }
      }
      const proposal =
        this.autoLevelPreview &&
        defectId != null &&
        this.autoLevelPreview.proposals &&
        this.autoLevelPreview.proposals[defectId]
      if (proposal) {
        return { planStartTime: proposal.planStartTime, planEndTime: proposal.planEndTime }
      }
      return row.defect
    },
    /** 条内跨休息日网纹段（相对条左缘；拖动中随预览更新） */
    barRestSegments(row) {
      const source = this.barPlanSource(row)
      if (!source || !this.axisRange || !this.axisRange.start) return []
      return barRestOverlays(
        source,
        this.axisRange.start,
        this.scale,
        this.colWidth,
        this.activeWorkCalendar
      )
    },
    barStyle(row) {
      const defectId = row.defect && row.defect.defectId != null ? Number(row.defect.defectId) : null
      const ds = this.dragState
      const geoSource = this.barPlanSource(row)
      if (!geoSource) return null
      const geo = barGeometry(geoSource, this.axisRange.start, this.scale, this.colWidth)
      if (!geo) return null
      let x = geo.x
      let width = geo.width
      const dragging = ds && ds.rowKey === row.rowKey
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
    isAutoLevelPushBar(defect) {
      const ds = this.dragState
      if (!ds || !ds.autoLevelPushes || !defect || defect.defectId == null) return false
      return !!ds.autoLevelPushes[Number(defect.defectId)]
    },
    isAutoLevelProposalBar(defect) {
      if (!this.autoLevelPreview || !defect || defect.defectId == null) return false
      return !!(this.autoLevelPreview.proposals && this.autoLevelPreview.proposals[Number(defect.defectId)])
    },
    autoLevelGhostStyle(row) {
      if (!this.autoLevelPreview || !row || !row.defect || row.defect.defectId == null) return null
      const id = Number(row.defect.defectId)
      if (!(this.autoLevelPreview.proposals && this.autoLevelPreview.proposals[id])) return null
      const geo = barGeometry(row.defect, this.axisRange.start, this.scale, this.colWidth)
      if (!geo) return null
      return { left: geo.x + 'px', width: geo.width + 'px' }
    },
    baselineBarStyle(row) {
      if (!row || !row.defect || row.defect.defectId == null) return null
      const item = this.baselineItemByDefectId.get(Number(row.defect.defectId))
      if (!item) return null
      const pseudo = {
        planStartTime: item.planStartTime,
        planEndTime: item.planEndTime
      }
      const geo = barGeometry(pseudo, this.axisRange.start, this.scale, this.colWidth)
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
        return this.$t('defect.gantt.axis-month', { m: d.getMonth() + 1 })
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
            baseline: !!this.showBaseline,
            handler: !!this.showBarHandler,
            days: !!this.showBarDays,
            defectTitle: !!this.showBarDefectTitle,
            autoLevel: !!this.autoLevelOnDrag,
            minimap: !!this.showMinimap
          })
        )
      } catch (_) { /* ignore */ }
    },
    /**
     * 条后展示的处理人：按处理人分组时只显示本组处理人；按交付物时显示全部处理人。
     * @param {object} row
     * @param {object} [group]
     */
    barHandlersOf(row, group) {
      if (!row || !row.defect) return []
      const all = resolveHandlers(row.defect)
      if (!all.length) return []
      if (group && group.type === 'handler' && group.handlerId != null) {
        const gid = Number(group.handlerId)
        const hit = all.find((h) => Number(h.userId) === gid)
        if (hit) return [hit]
        if (group.handlerMember) {
          return [{
            userId: gid,
            nickName: group.handlerMember.nickName || group.label || String(gid),
            userName: group.handlerMember.userName,
            avatar: group.handlerMember.avatar,
            avatarUrl: group.handlerMember.avatarUrl
          }]
        }
        return [{ userId: gid, nickName: group.label || String(gid), userName: String(gid) }]
      }
      return all
    },
    /** 条后头像 native title：多人姓名逗号拼接 */
    barHandlersTitle(row, group) {
      return this.barHandlersOf(row, group)
        .map((h) => h && (h.nickName || h.userName || h.name))
        .filter(Boolean)
        .join(', ')
    },
    /** 条右缘外侧放置处理人/标题元信息 */
    barMetaStyle(row) {
      const st = this.barStyle(row)
      if (!st || st.left == null || st.width == null) return null
      const left = parseFloat(st.left)
      const width = parseFloat(st.width)
      if (!Number.isFinite(left) || !Number.isFinite(width)) return null
      return {
        left: left + width + 6 + 'px'
      }
    },
    /** 条后「天数」文案：计划工期 + 天 */
    barDaysText(row) {
      if (!row || !row.defect) return ''
      const days = planDurationDays(row.defect, this.activeWorkCalendar)
      const formatted = formatDurationDays(days)
      if (!formatted) return ''
      return this.$t('defect.gantt.bar-workdays', { n: formatted })
    },
    /** 条后「缺陷标题」文案：#编号 + 标题 */
    barDefectTitleText(row) {
      if (!row || !row.defect) return ''
      const num = row.defect.projectNum != null ? String(row.defect.projectNum) : ''
      const name = (row.defect.defectName || '').trim()
      if (num && name) return `#${num} ${name}`
      if (num) return `#${num}`
      return name
    },
    /** 条后元信息是否有可展示内容 */
    barMetaVisible(row, group) {
      if (!this.barMetaStyle(row)) return false
      if (this.showBarHandler && this.barHandlersOf(row, group).length) return true
      if (this.showBarDays && this.barDaysText(row)) return true
      if (this.showBarDefectTitle && this.barDefectTitleText(row)) return true
      return false
    },
    persistMinimapPos() {
      if (!(this.$cache && this.$cache.local)) return
      if (!this.minimapPos) return
      try {
        this.$cache.local.set(
          GANTT_CACHE.MINIMAP_POS,
          JSON.stringify({ x: this.minimapPos.x, y: this.minimapPos.y })
        )
      } catch (_) { /* ignore */ }
    },
    closeMinimap() {
      this.showMinimap = false
    },
    updateMinimapViewport() {
      if (!this.showMinimap) return
      const body = this.$refs.axisBodyScroll
      if (!body) return
      /* 缩略图拖导航中由 applyMinimapNavToScroll 直接写红框，避免每帧触发 Vue 重绘色块 */
      if (this.minimapNavDrag) return
      const contentW = Math.max(1, body.scrollWidth || 0)
      const contentH = Math.max(1, body.scrollHeight || 0)
      const clientW = Math.max(1, body.clientWidth || 0)
      const clientH = Math.max(1, body.clientHeight || 0)
      const mw = this.minimapWidth
      const mh = this.minimapHeight
      const width = Math.min(mw, (clientW / contentW) * mw)
      const height = Math.min(mh, (clientH / contentH) * mh)
      const left = Math.max(0, Math.min(mw - width, (body.scrollLeft / contentW) * mw))
      const top = Math.max(0, Math.min(mh - height, (body.scrollTop / contentH) * mh))
      this.applyMinimapViewportDom(left, top, width, height)
    },
    applyMinimapViewportDom(left, top, width, height) {
      /* 内缩 1px，避免贴边时 overflow 裁掉红框描边 */
      const edge = 1
      const mw = this.minimapWidth
      const mh = this.minimapHeight
      let w = Math.max(4, Math.min(width, mw - edge * 2))
      let h = Math.max(4, Math.min(height, mh - edge * 2))
      let l = Math.max(edge, Math.min(mw - edge - w, left))
      let t = Math.max(edge, Math.min(mh - edge - h, top))
      const v = this.minimapViewport
      v.left = l
      v.top = t
      v.width = w
      v.height = h
      const el = this.$refs.minimapViewportEl
      if (el) {
        el.style.left = l + 'px'
        el.style.top = t + 'px'
        el.style.width = w + 'px'
        el.style.height = h + 'px'
      }
    },
    clampMinimapPos(x, y) {
      const pane = this.$refs.scheduledPane
      const panel = this.$refs.minimapPanel
      const pad = 8
      const pw = (panel && panel.offsetWidth) || this.minimapWidth + 2
      const ph = (panel && panel.offsetHeight) || this.minimapHeight + 28
      const maxX = pane ? Math.max(pad, pane.clientWidth - pw - pad) : x
      const maxY = pane ? Math.max(pad, pane.clientHeight - ph - pad) : y
      return {
        x: Math.max(pad, Math.min(maxX, x)),
        y: Math.max(pad, Math.min(maxY, y))
      }
    },
    startMinimapPanelDrag(e) {
      if (e.button != null && e.button !== 0) return
      const pane = this.$refs.scheduledPane
      const panel = this.$refs.minimapPanel
      if (!pane || !panel) return
      const paneRect = pane.getBoundingClientRect()
      const panelRect = panel.getBoundingClientRect()
      const startLeft = panelRect.left - paneRect.left
      const startTop = panelRect.top - paneRect.top
      const clamped = this.clampMinimapPos(startLeft, startTop)
      this.minimapPanelDrag = {
        startX: e.clientX,
        startY: e.clientY,
        originX: clamped.x,
        originY: clamped.y,
        lastX: clamped.x,
        lastY: clamped.y
      }
      /* 拖动中用 DOM 定位，避免每帧写 reactive 触发整树重绘 */
      panel.classList.remove('is-docked')
      panel.style.left = clamped.x + 'px'
      panel.style.top = clamped.y + 'px'
      panel.style.right = 'auto'
      panel.style.bottom = 'auto'
      document.addEventListener('mousemove', this.onMinimapPanelDragMove)
      document.addEventListener('mouseup', this.onMinimapPanelDragUp)
    },
    onMinimapPanelDragMove(e) {
      const drag = this.minimapPanelDrag
      if (!drag) return
      drag._clientX = e.clientX
      drag._clientY = e.clientY
      if (this._minimapPanelRaf) return
      this._minimapPanelRaf = requestAnimationFrame(() => {
        this._minimapPanelRaf = 0
        const d = this.minimapPanelDrag
        const panel = this.$refs.minimapPanel
        if (!d || !panel) return
        const next = this.clampMinimapPos(
          d.originX + (d._clientX - d.startX),
          d.originY + (d._clientY - d.startY)
        )
        d.lastX = next.x
        d.lastY = next.y
        panel.style.left = next.x + 'px'
        panel.style.top = next.y + 'px'
      })
    },
    onMinimapPanelDragUp() {
      const drag = this.minimapPanelDrag
      if (drag) {
        this.minimapPos = { x: drag.lastX, y: drag.lastY }
        this.persistMinimapPos()
      }
      this.teardownMinimapPanelDrag()
    },
    teardownMinimapPanelDrag() {
      this.minimapPanelDrag = null
      if (this._minimapPanelRaf) {
        cancelAnimationFrame(this._minimapPanelRaf)
        this._minimapPanelRaf = 0
      }
      document.removeEventListener('mousemove', this.onMinimapPanelDragMove)
      document.removeEventListener('mouseup', this.onMinimapPanelDragUp)
    },
    startMinimapNav(e) {
      if (e.button != null && e.button !== 0) return
      const bodyEl = this.$refs.minimapBody
      const axis = this.$refs.axisBodyScroll
      if (!bodyEl || !axis) return
      const rect = bodyEl.getBoundingClientRect()
      const mx = e.clientX - rect.left
      const my = e.clientY - rect.top
      const v = this.minimapViewport
      let grabX = v.width / 2
      let grabY = v.height / 2
      const metrics = {
        contentW: Math.max(1, axis.scrollWidth || 0),
        contentH: Math.max(1, axis.scrollHeight || 0),
        mw: this.minimapWidth,
        mh: this.minimapHeight,
        viewW: v.width,
        viewH: v.height,
        bodyLeft: rect.left,
        bodyTop: rect.top
      }
      this.minimapNavDrag = { grabX, grabY, metrics }
      if (mx >= v.left && mx <= v.left + v.width && my >= v.top && my <= v.top + v.height) {
        grabX = mx - v.left
        grabY = my - v.top
        this.minimapNavDrag.grabX = grabX
        this.minimapNavDrag.grabY = grabY
      } else {
        this.applyMinimapNavToScroll(mx - v.width / 2, my - v.height / 2, metrics)
        this.minimapNavDrag.grabX = this.minimapViewport.width / 2
        this.minimapNavDrag.grabY = this.minimapViewport.height / 2
      }
      document.addEventListener('mousemove', this.onMinimapNavMove)
      document.addEventListener('mouseup', this.onMinimapNavUp)
    },
    onMinimapNavMove(e) {
      const drag = this.minimapNavDrag
      if (!drag) return
      drag._clientX = e.clientX
      drag._clientY = e.clientY
      if (this._minimapNavRaf) return
      this._minimapNavRaf = requestAnimationFrame(() => {
        this._minimapNavRaf = 0
        const d = this.minimapNavDrag
        if (!d || d._clientX == null) return
        const mx = d._clientX - d.metrics.bodyLeft
        const my = d._clientY - d.metrics.bodyTop
        this.applyMinimapNavToScroll(mx - d.grabX, my - d.grabY, d.metrics)
      })
    },
    onMinimapNavUp() {
      this.teardownMinimapNav()
      this.updateMinimapViewport()
    },
    teardownMinimapNav() {
      this.minimapNavDrag = null
      if (this._minimapNavRaf) {
        cancelAnimationFrame(this._minimapNavRaf)
        this._minimapNavRaf = 0
      }
      document.removeEventListener('mousemove', this.onMinimapNavMove)
      document.removeEventListener('mouseup', this.onMinimapNavUp)
    },
    applyMinimapNavToScroll(viewLeft, viewTop, metrics) {
      const axis = this.$refs.axisBodyScroll
      if (!axis) return
      const m = metrics || {
        contentW: Math.max(1, axis.scrollWidth || 0),
        contentH: Math.max(1, axis.scrollHeight || 0),
        mw: this.minimapWidth,
        mh: this.minimapHeight,
        viewW: this.minimapViewport.width,
        viewH: this.minimapViewport.height
      }
      const maxLeft = Math.max(0, m.mw - m.viewW)
      const maxTop = Math.max(0, m.mh - m.viewH)
      const left = Math.max(0, Math.min(maxLeft, viewLeft))
      const top = Math.max(0, Math.min(maxTop, viewTop))
      this.syncingScroll = true
      axis.scrollLeft = (left / m.mw) * m.contentW
      axis.scrollTop = (top / m.mh) * m.contentH
      const leftList = this.$refs.leftScroll
      const header = this.$refs.axisHeaderScroll
      if (leftList) leftList.scrollTop = axis.scrollTop
      if (header) header.scrollLeft = axis.scrollLeft
      this.syncingScroll = false
      this.applyMinimapViewportDom(left, top, m.viewW, m.viewH)
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
      /* colWidth 由 colWidths[scale] 记忆；无记忆则默认 */
      this.$nextTick(() => {
        this.scrollToToday(false)
        this.updateMinimapViewport()
      })
    },
    persistColWidths() {
      if (!(this.$cache && this.$cache.local)) return
      try {
        this.$cache.local.set(GANTT_CACHE.COL_WIDTHS, JSON.stringify(this.colWidths))
      } catch (_) { /* ignore */ }
    },
    setColWidthForScale(scale, width) {
      const n = Number(width)
      if (!Number.isFinite(n) || n <= 0) return
      if (Math.abs((this.colWidths[scale] || 0) - n) < 0.01) return
      /* 换新对象，确保 computed colWidth 一定刷新 */
      this.colWidths = { ...this.colWidths, [scale]: n }
    },
    /**
     * 列宽 1:1 跟鼠标；用「缝」内容坐标做滚动锚定，
     * 避免全局同宽时整轴狂奔、指针下日期漂移。
     */
    startColResize(e, unitIndex) {
      if (e.button != null && e.button !== 0) return
      const header = this.$refs.axisHeaderScroll
      const rect = header ? header.getBoundingClientRect() : null
      const axisLeft = rect ? rect.left : 0
      const col = Math.max(1, this.colWidth)
      const ui = Number.isFinite(Number(unitIndex)) ? Math.max(0, Math.floor(Number(unitIndex))) : 0
      const colsBeforeSeam = ui + 1
      /* 锚在被拖格缝（第 ui 列右缘），不是点击抖动点 */
      const anchorContentX = colsBeforeSeam * col
      this.colResize = {
        startX: e.clientX,
        startWidth: col,
        scale: this.scale,
        colsBeforeSeam,
        anchorContentX,
        axisLeft,
        _clientX: e.clientX,
        _raf: 0
      }
      if (typeof document !== 'undefined' && document.body) {
        document.body.classList.add('defect-gantt-col-resizing')
      }
      document.addEventListener('mousemove', this.onColResizeMove)
      document.addEventListener('mouseup', this.onColResizeUp)
    },
    onColResizeMove(e) {
      const drag = this.colResize
      if (!drag) return
      drag._clientX = e.clientX
      if (drag._raf) return
      drag._raf = requestAnimationFrame(() => {
        drag._raf = 0
        if (!this.colResize) return
        this.applyColResizeDelta(this.colResize._clientX - this.colResize.startX)
      })
    },
    onColResizeUp() {
      const drag = this.colResize
      if (drag) {
        if (drag._raf) {
          cancelAnimationFrame(drag._raf)
          drag._raf = 0
        }
        this.applyColResizeDelta(drag._clientX - drag.startX)
      }
      this.teardownColResize()
      this.persistColWidths()
      this.$nextTick(() => this.updateMinimapViewport())
    },
    teardownColResize() {
      const drag = this.colResize
      if (drag && drag._raf) cancelAnimationFrame(drag._raf)
      this.colResize = null
      if (typeof document !== 'undefined' && document.body) {
        document.body.classList.remove('defect-gantt-col-resizing')
      }
      document.removeEventListener('mousemove', this.onColResizeMove)
      document.removeEventListener('mouseup', this.onColResizeUp)
    },
    applyColResizeDelta(deltaPx) {
      const drag = this.colResize
      if (!drag) return
      const scale = this.scale
      const lim = GANTT_COL_WIDTH_LIMITS[scale]
      if (!lim) return
      /*
       * 列宽与鼠标 1:1（否则靠右拖 Δ/N 几乎不动）。
       * 用 scroll 把被拖格缝钉在指针下，避免全局同宽时整轴狂奔。
       */
      let next = drag.startWidth + deltaPx
      const hyst = this._colResizeHysteresisUntil && Date.now() < this._colResizeHysteresisUntil
      if (!hyst) {
        const coarser = coarserScale(scale)
        const finer = finerScale(scale)
        if (coarser && next < lim.min) {
          this.switchScaleFromColResize(coarser)
          return
        }
        if (finer && next > wideThreshold(scale)) {
          this.switchScaleFromColResize(finer)
          return
        }
      }
      next = Math.min(lim.max, Math.max(lim.min, next))
      if (Math.abs(next - this.colWidth) < 0.05) {
        this.syncColResizeScroll(drag, next)
        return
      }
      this.setColWidthForScale(scale, next)
      this.syncColResizeScroll(drag, next)
      this.updateMinimapViewport()
    },
    /** 被拖格缝留在指针下：scrollLeft = N*width - (clientX - axisLeft) */
    syncColResizeScroll(drag, widthOverride) {
      if (!drag) return
      const header = this.$refs.axisHeaderScroll
      const body = this.$refs.axisBodyScroll
      if (!header) return
      const rect = header.getBoundingClientRect()
      const axisLeft = rect.left
      const clientX = drag._clientX != null ? drag._clientX : drag.startX
      const w = widthOverride != null ? widthOverride : this.colWidth
      const n = Math.max(1, drag.colsBeforeSeam || 1)
      const seamContentX = n * w
      const target = Math.max(0, seamContentX - (clientX - axisLeft))
      this.syncingScroll = true
      header.scrollLeft = target
      if (body) body.scrollLeft = target
      this.syncingScroll = false
    },
    switchScaleFromColResize(newScale) {
      const drag = this.colResize
      const body = this.$refs.axisBodyScroll
      const header = this.$refs.axisHeaderScroll
      let centerDate = null
      if (body) {
        const centerOffset = body.scrollLeft + body.clientWidth / 2
        centerDate = offsetPxToDate(centerOffset, this.axisRange.start, this.scale, this.colWidth)
      }
      const defaultW = GANTT_COL_WIDTH[newScale] || GANTT_COL_WIDTH.day
      this.scale = newScale
      this.setColWidthForScale(newScale, defaultW)
      if (this.$cache && this.$cache.local) {
        this.$cache.local.set(GANTT_CACHE.SCALE, newScale)
      }
      this.persistColWidths()
      this._colResizeHysteresisUntil = Date.now() + 420
      const scaleLabel = this.$t(`defect.gantt.scale-${newScale}`)
      this.$modal.msg(this.$t('defect.gantt.scale-switched', { scale: scaleLabel }))
      this.$nextTick(() => {
        if (centerDate && body) {
          const newX = dateToOffsetPx(centerDate, this.axisRange.start, this.scale, this.colWidth)
          const target = Math.max(0, newX - body.clientWidth / 2)
          this.syncingScroll = true
          body.scrollLeft = target
          if (header) header.scrollLeft = target
          this.syncingScroll = false
        }
        /* 切档后按当前指针下的列重定基准，继续拖仍跟手 */
        if (drag) {
          const el = header || body
          const rect = el ? el.getBoundingClientRect() : null
          const axisLeft = rect ? rect.left : 0
          const scrollLeft = el ? el.scrollLeft : 0
          const clientX = drag._clientX != null ? drag._clientX : drag.startX
          const contentX = Math.max(0, scrollLeft + (clientX - axisLeft))
          const col = Math.max(1, this.colWidth)
          const unitIndex = Math.min(
            Math.max(0, Math.floor(contentX / col)),
            Math.max(0, (this.axisUnits || []).length - 1)
          )
          const colsBeforeSeam = unitIndex + 1
          drag.startX = clientX
          drag.startWidth = col
          drag.scale = newScale
          drag.colsBeforeSeam = colsBeforeSeam
          drag.anchorContentX = colsBeforeSeam * col
          drag.axisLeft = axisLeft
        }
        this.updateMinimapViewport()
      })
    },
    scrollToToday(smooth = true) {
      const body = this.$refs.axisBodyScroll
      const header = this.$refs.axisHeaderScroll
      if (!body) return
      const x = dateToOffsetPx(new Date(), this.axisRange.start, this.scale, this.colWidth)
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
      this.scheduleGanttLoadMoreCheck()
    },
    onAxisBodyScroll() {
      if (this.syncingScroll) {
        this.updateMinimapViewport()
        return
      }
      this.syncingScroll = true
      const left = this.$refs.leftScroll
      const axis = this.$refs.axisBodyScroll
      const header = this.$refs.axisHeaderScroll
      if (left && axis) left.scrollTop = axis.scrollTop
      if (header && axis) header.scrollLeft = axis.scrollLeft
      this.syncingScroll = false
      this.updateMinimapViewport()
      this.maybeExtendAxisOnScroll()
      this.scheduleGanttLoadMoreCheck()
    },
    onAxisHeaderScroll() {
      if (this.syncingScroll) return
      this.syncingScroll = true
      const axis = this.$refs.axisBodyScroll
      const header = this.$refs.axisHeaderScroll
      if (header && axis) axis.scrollLeft = header.scrollLeft
      this.syncingScroll = false
      this.maybeExtendAxisOnScroll()
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
    /** 点选修饰键：Ctrl/⌘ 加减；Shift 区间；Ctrl/⌘+Shift 并入区间 */
    selectionModifiersFromEvent(e) {
      if (!e) return {}
      const ctrl = !!(e.ctrlKey || e.metaKey)
      const shift = !!e.shiftKey
      if (ctrl && shift) return { mergeRange: true }
      if (shift) return { range: true }
      if (ctrl) return { additive: true }
      return {}
    },
    /** 可见已排期扁平行序（跳过折叠组，defectId 去重） */
    visibleScheduledDefectIds() {
      const ids = []
      const seen = new Set()
      const groups = this.groups || []
      for (let i = 0; i < groups.length; i++) {
        const g = groups[i]
        if (!g || this.isGroupCollapsed(g.key)) continue
        const rows = g.rows || []
        for (let j = 0; j < rows.length; j++) {
          const d = rows[j] && rows[j].defect
          if (!d || d.defectId == null) continue
          const id = Number(d.defectId)
          if (!Number.isFinite(id) || seen.has(id)) continue
          seen.add(id)
          ids.push(id)
        }
      }
      return ids
    },
    selectDefect(defect, opts = {}) {
      if (!defect || defect.defectId == null) return
      const id = Number(defect.defectId)
      if (!Number.isFinite(id)) return

      if (opts.range || opts.mergeRange) {
        const order = this.visibleScheduledDefectIds()
        const anchor =
          this.selectionAnchorId != null ? Number(this.selectionAnchorId) : null
        const ai = anchor != null ? order.indexOf(anchor) : -1
        const bi = order.indexOf(id)
        if (ai < 0 || bi < 0) {
          this.selectedDefectId = defect.defectId
          this.marqueeSelectedIds = [id]
          this.selectionAnchorId = id
          return
        }
        const lo = Math.min(ai, bi)
        const hi = Math.max(ai, bi)
        const rangeIds = order.slice(lo, hi + 1)
        if (opts.mergeRange) {
          const set = new Set((this.marqueeSelectedIds || []).map(Number))
          rangeIds.forEach((x) => set.add(x))
          this.marqueeSelectedIds = Array.from(set)
        } else {
          this.marqueeSelectedIds = rangeIds
        }
        this.selectedDefectId = id
        return
      }

      if (opts.additive) {
        const set = new Set((this.marqueeSelectedIds || []).map(Number))
        if (set.has(id)) set.delete(id)
        else set.add(id)
        this.marqueeSelectedIds = Array.from(set)
        this.selectedDefectId = set.has(id)
          ? id
          : set.size
            ? Array.from(set)[set.size - 1]
            : null
        this.selectionAnchorId = id
        return
      }

      this.selectedDefectId = defect.defectId
      this.marqueeSelectedIds = [id]
      this.selectionAnchorId = id
    },
    clearDefectSelection() {
      this.selectedDefectId = null
      this.marqueeSelectedIds = []
      this.selectionAnchorId = null
    },
    /** 未排期池：始终单选，忽略修饰键多选/区间 */
    selectUnscheduledDefect(defect) {
      this.selectDefect(defect)
    },
    openDefect(defect) {
      if (defect && defect.defectId) {
        this.selectDefect(defect)
        this.$emit('defect-click', defect)
      }
    },
    /** 左侧已排期行：无修饰 focus+滚轴；有修饰只改选中 */
    onScheduledRowClick(e, row, group) {
      if (!row || !row.defect) return
      const mods = this.selectionModifiersFromEvent(e)
      if (mods.additive || mods.range || mods.mergeRange) {
        if (e && typeof e.preventDefault === 'function') e.preventDefault()
        this.selectDefect(row.defect, mods)
        return
      }
      this.focusGanttRow(row, group)
    },
    /** 轴行空白点击：与左侧同一套修饰键语义，不滚轴 */
    onAxisRowClick(e, row) {
      if (!row || !row.defect) return
      const mods = this.selectionModifiersFromEvent(e)
      this.selectDefect(
        row.defect,
        mods.additive || mods.range || mods.mergeRange ? mods : {}
      )
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
      const geo = barGeometry(row.defect, this.axisRange.start, this.scale, this.colWidth)
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
      /* 可计划拖的条由 mouseup 开详情；无计划写权限时走 click */
      if (this.canEdit && this.canEditPlanDefect(defect)) return
      this.openDefect(defect)
    },
    ensureCanEdit() {
      if (this.canEdit) return true
      this.$modal.msgWarning(this.$t('defect.gantt.no-edit-perm'))
      return false
    },
    ensureCanEditDefect() {
      if (this.canEditDefect) return true
      this.$modal.msgWarning(this.$t('defect.gantt.no-defect-edit-perm'))
      return false
    },
    canEditPlanDefect(defect) {
      if (!this.canEdit) return false
      return canEditGanttPlanDefect(defect, this.planEditCtx)
    },
    ensureCanEditPlanDefect(defect) {
      if (!this.ensureCanEdit()) return false
      if (this.canEditPlanDefect(defect)) return true
      this.$modal.msgWarning(this.$t('defect.gantt.no-plan-edit-perm'))
      return false
    },
    /** 不可计划拖时 title 提示原因；可拖时显示编号+标题 */
    planReadonlyTip() {
      if (!this.canEdit) return this.$t('defect.gantt.no-edit-perm')
      return this.$t('defect.gantt.no-plan-edit-perm')
    },
    barHoverTitle(row) {
      if (!row || !row.defect) return ''
      if (this.isBarModuleForbidden(row)) return this.$t('defect.gantt.module-drop-forbidden')
      if (!this.canEditPlanDefect(row.defect)) return this.planReadonlyTip()
      return ('#' + row.defect.projectNum + ' ' + (row.defect.defectName || '')).trim()
    },
    chipHoverTitle(defect) {
      if (!defect) return ''
      if (!this.canEditPlanDefect(defect)) return this.planReadonlyTip()
      return ('#' + defect.projectNum + ' ' + (defect.defectName || '')).trim()
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
          this.scaleDayDelta(Math.round((this.dragState.preview.x - this.dragState.originX) / this.colWidth)), this.activeWorkCalendar)
        if (shifted) {
          planStartTime = shifted.planStartTime
          planEndTime = shifted.planEndTime
        }
      } else if (this.dragState && (this.dragState.mode === 'start' || this.dragState.mode === 'end')) {
        planStartTime = this.dragState.defect.planStartTime
        planEndTime = this.dragState.defect.planEndTime
        const edge = this.dragState.mode
        const preview = this.dragState.preview
        const ppd = pxPerDay(this.scale, this.colWidth)
        const widthDays =
          preview && ppd > 0
            ? snapDurationDays(preview.width / ppd, { min: 0.1, magnet: 0.12 })
            : null
        const resized =
          widthDays != null
            ? resizePlanRange(planStartTime, planEndTime, edge, null, { durationDays: widthDays, calendar: this.activeWorkCalendar })
            : resizePlanRange(planStartTime, planEndTime, edge, day)
        if (resized) {
          planStartTime = resized.planStartTime
          planEndTime = resized.planEndTime
        }
      } else {
        const range = defaultPlanRangeFromDropDay(day, this.activeWorkCalendar)
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
      const geo = barGeometry(previewDefect, this.axisRange.start, this.scale, this.colWidth)
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
      if (!this.ensureCanEditPlanDefect(defect)) {
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
      return offsetPxToDate(offset, this.axisRange.start, this.scale, this.colWidth)
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
      if (!this.ensureCanEditPlanDefect(defect)) return
      const day = this.clientXToDate(e.clientX)
      if (!day) return
      const range = defaultPlanRangeFromDropDay(day, this.activeWorkCalendar)
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
      const mods = this.selectionModifiersFromEvent(e)
      if (mods.additive || mods.range || mods.mergeRange) {
        this.selectDefect(row && row.defect, mods)
      } else if (!this.isSelectedDefect(row && row.defect)) {
        this.selectDefect(row && row.defect)
      } else if ((this.marqueeSelectedIds || []).length <= 1) {
        this.selectDefect(row && row.defect)
      }
      if (!this.canEditPlanDefect(row.defect)) return
      if (this.autoLevelPreview) {
        this.$modal.msgWarning(this.$t('defect.gantt.auto-level-busy'))
        return
      }
      e.preventDefault()
      const geo = barGeometry(row.defect, this.axisRange.start, this.scale, this.colWidth)
      if (!geo) return
      let batchIds =
        mode === 'move' &&
        (this.marqueeSelectedIds || []).length > 1 &&
        this.isSelectedDefect(row.defect)
          ? (this.marqueeSelectedIds || []).map(Number).filter((id) => Number.isFinite(id))
          : null
      if (batchIds && batchIds.length > 1) {
        batchIds = batchIds.filter((id) => {
          const d = (this.defectList || []).find((x) => x && Number(x.defectId) === id)
          return d && this.canEditPlanDefect(d)
        })
        if (batchIds.length <= 1) batchIds = null
      }
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
        additiveSelect: !!(mods && mods.additive)
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
      if (this.autoLevelOnDrag) this.ensureAutoLevelAdmin()
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
      const ppd = pxPerDay(this.scale, col)
      /* 拉边：0.1 天步进 + 整天磁吸；平移仍按列吸附 */
      const minBarW = Math.max(4, ppd * 0.1)
      let nextPreview = ds.preview
      if (ds.mode === 'move') {
        const moveSnap = Math.round(dx / col) * col
        nextPreview = { x: ds.originX + moveSnap, width: ds.originWidth }
      } else if (ds.mode === 'start') {
        const rawDays = (ds.originWidth - dx) / ppd
        const widthDays = snapDurationDays(rawDays, { min: 0.1, magnet: 0.12 })
        const width = Math.max(minBarW, widthDays * ppd)
        const x = ds.originX + ds.originWidth - width
        nextPreview = { x, width }
      } else if (ds.mode === 'end') {
        const rawDays = (ds.originWidth + dx) / ppd
        const widthDays = snapDurationDays(rawDays, { min: 0.1, magnet: 0.12 })
        const width = Math.max(minBarW, widthDays * ppd)
        nextPreview = { x: ds.originX, width }
      }
      const previewChanged =
        !ds.preview ||
        ds.preview.x !== nextPreview.x ||
        ds.preview.width !== nextPreview.width
      ds.preview = nextPreview
      this.refreshDragAutoLevelPushes(ds)
      if (resizing) {
        this.setDropInsertPreview(null)
        if (previewChanged) this.dragState = { ...ds }
        return
      }
      /* 批量平移只改水平日期，不做跨组插入预览 */
      if (Array.isArray(ds.batchIds) && ds.batchIds.length > 1) {
        this.setDropInsertPreview(null)
        ds.autoLevelPushes = null
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
    /** 拖动预览位 → 同人可改重叠条往后挤的临时提案 */
    refreshDragAutoLevelPushes(ds) {
      if (!ds || !this.autoLevelOnDrag || !ds.preview || !ds.defect) {
        if (ds) ds.autoLevelPushes = null
        return
      }
      if (Array.isArray(ds.batchIds) && ds.batchIds.length > 1) {
        ds.autoLevelPushes = null
        return
      }
      const pinnedRange = this.planRangeFromDragPreview(ds)
      if (!pinnedRange) {
        ds.autoLevelPushes = null
        return
      }
      const laneId = this.resolveDragLaneHandlerId(ds)
      const ctx = {
        userId: this.currentUserId(),
        isProjectAdmin: !!this.autoLevelIsAdmin
      }
      const peers = (this.defectList || []).filter((d) => {
        if (!d || Number(d.defectId) === Number(ds.defect.defectId)) return false
        if (!canAutoLevelEditDefect(d, ctx)) return false
        const hid = primaryHandlerId(d)
        if (laneId == null) return hid == null
        return Number(hid) === Number(laneId)
      })
      const visualOrder = []
      const seen = new Set()
      ;(this.groups || []).forEach((g) => {
        ;(g.rows || []).forEach((row) => {
          const id = row && row.defect && row.defect.defectId != null
            ? Number(row.defect.defectId)
            : null
          if (!Number.isFinite(id) || seen.has(id)) return
          seen.add(id)
          visualOrder.push(id)
        })
      })
      const map = pushOverlapsAfterPinned({
        pinnedId: Number(ds.defect.defectId),
        pinnedRange,
        peers,
        visualOrder,
        calendar: this.activeWorkCalendar
      })
      const obj = {}
      map.forEach((v, k) => {
        obj[k] = v
      })
      ds.autoLevelPushes = Object.keys(obj).length ? obj : null
    },
    resolveDragLaneHandlerId(ds) {
      if (!ds) return null
      if (ds.hoverGroup && ds.hoverGroup.type === 'handler' && ds.hoverGroup.handlerId != null) {
        return Number(ds.hoverGroup.handlerId)
      }
      if (ds.group && ds.group.type === 'handler' && ds.group.handlerId != null) {
        return Number(ds.group.handlerId)
      }
      return primaryHandlerId(ds.defect)
    },
    planRangeFromDragPreview(ds) {
      if (!ds || !ds.preview) return null
      if (ds.mode === 'move') {
        const dayDelta = Math.round((ds.preview.x - ds.originX) / this.colWidth)
        return shiftPlanRange(ds.planStartTime, ds.planEndTime, this.scaleDayDelta(dayDelta), this.activeWorkCalendar)
      }
      if (ds.mode === 'start' || ds.mode === 'end') {
        const ppd = pxPerDay(this.scale, this.colWidth)
        const widthDays = snapDurationDays(ds.preview.width / ppd, { min: 0.1, magnet: 0.12 })
        return resizePlanRange(ds.planStartTime, ds.planEndTime, ds.mode, null, { durationDays: widthDays, calendar: this.activeWorkCalendar })
      }
      return null
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
        range = shiftPlanRange(ds.planStartTime, ds.planEndTime, this.scaleDayDelta(dayDelta), this.activeWorkCalendar)
      } else if (ds.mode === 'start' || ds.mode === 'end') {
        const ppd = pxPerDay(this.scale, this.colWidth)
        const widthDays = snapDurationDays(ds.preview.width / ppd, { min: 0.1, magnet: 0.12 })
        range = resizePlanRange(ds.planStartTime, ds.planEndTime, ds.mode, null, { durationDays: widthDays, calendar: this.activeWorkCalendar })
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
        const pushEntries = Object.entries(ds.autoLevelPushes || {})
        this.patchLocalDefect(ds.defect.defectId, optimistic, targetGroup)
        pushEntries.forEach(([idStr, pr]) => {
          this.patchLocalDefect(Number(idStr), {
            planStartTime: pr.planStartTime,
            planEndTime: pr.planEndTime
          })
        })
        if (ds.mode === 'move' && orderTargetKey && insertIndex != null) {
          this.applyGroupRowOrder(orderTargetKey, ds.defect.defectId, insertIndex)
        }
        const dragged = ds.defect
        endDragUi()
        await this.persistDefectUpdate(dragged, range, targetGroup, {
          skipLocalPatch: true,
          rollback
        })
        if (pushEntries.length) {
          await this.persistAutoLevelPushes(pushEntries)
        }
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
        await updateDefectPlanNode(payload)
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
      if (!this.ensureCanEditDefect()) return
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
      if (!this.ensureCanEditDefect()) return
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
      if (!this.ensureCanEditDefect()) return
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
      if (!this.canFreezeBaseline) {
        this.$modal.msgWarning(this.$t('defect.gantt.no-baseline-admin'))
        return
      }
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
    currentUserId() {
      const raw = this.userStore && this.userStore.id
      const uid = Number(raw)
      return Number.isFinite(uid) && uid > 0 ? uid : NaN
    },
    async ensureAutoLevelAdmin() {
      if (this.autoLevelIsAdmin != null) return !!this.autoLevelIsAdmin
      const projectId = this.resolveProjectId()
      if (!projectId) {
        this.autoLevelIsAdmin = false
        return false
      }
      try {
        const res = await getProject(projectId)
        const project = res && (res.data != null ? res.data : res)
        this.autoLevelIsAdmin = isProjectPlanAdmin(project, this.currentUserId())
      } catch (err) {
        console.warn('[DefectGantt] getProject for auto-level failed', err)
        this.autoLevelIsAdmin = false
      }
      return !!this.autoLevelIsAdmin
    },
    async persistAutoLevelPushes(entries) {
      const projectId = this.resolveProjectId()
      const queue = entries.slice()
      let fail = 0
      const runOne = async () => {
        while (queue.length) {
          const [idStr, range] = queue.shift()
          const defectId = Number(idStr)
          const local = (this.defectList || []).find((d) => Number(d.defectId) === defectId)
          try {
            await updateDefectPlanNode({
              defectId,
              projectId: local && local.projectId != null ? local.projectId : projectId,
              planStartTime: range.planStartTime,
              planEndTime: range.planEndTime
            })
          } catch (err) {
            fail += 1
            console.warn('[DefectGantt] auto-level push update failed', defectId, err)
          }
        }
      }
      const workers = []
      const n = Math.min(AUTO_LEVEL_UPDATE_CONCURRENCY, entries.length)
      for (let i = 0; i < n; i++) workers.push(runOne())
      await Promise.all(workers)
      this.fetchPlanGanttMeta()
      if (fail > 0) {
        this.$modal.msgWarning(
          this.$t('defect.gantt.drag-yield-partial', {
            ok: entries.length - fail,
            fail
          })
        )
      }
    },
    /** 拉取项目未关闭已排期全集（分页），不依赖甘特当前已加载段 */
    async fetchAutoLevelDefectUniverse(projectId) {
      const all = []
      let pageNum = 1
      let total = Infinity
      while (all.length < total) {
        const res = await listDefect({
          projectId,
          pageNum,
          pageSize: AUTO_LEVEL_PAGE_SIZE,
          params: { delFlag: '0' }
        })
        const rows = Array.isArray(res.rows) ? res.rows : []
        total = Number(res.total)
        if (!Number.isFinite(total)) total = all.length + rows.length
        all.push(...rows)
        if (!rows.length || rows.length < AUTO_LEVEL_PAGE_SIZE) break
        pageNum += 1
        if (pageNum > 500) break
      }
      return all
    },
    async startAutoLevel() {
      if (!this.ensureCanEdit()) return
      if (this.autoLevelPreview) {
        this.$modal.msgWarning(this.$t('defect.gantt.auto-level-busy'))
        return
      }
      const projectId = this.resolveProjectId()
      if (!projectId) return
      this.autoLevelLoading = true
      try {
        const isAdmin = await this.ensureAutoLevelAdmin()
        const universe = await this.fetchAutoLevelDefectUniverse(projectId)
        const ctx = { userId: this.currentUserId(), isProjectAdmin: !!isAdmin }
        const candidates = filterAutoLevelCandidates(universe, ctx)
        if (!candidates.length) {
          this.$modal.msgWarning(this.$t('defect.gantt.auto-level-empty'))
          return
        }
        const meta = await getDefectPlanGanttMeta(projectId)
        const data = meta && (meta.data != null ? meta.data : meta)
        const links = data && Array.isArray(data.links) ? data.links : []
        const movableIds = candidates.map((d) => Number(d.defectId))
        const visualOrder = []
        const seenVisual = new Set()
        ;(this.groups || []).forEach((g) => {
          ;(g.rows || []).forEach((row) => {
            const id = row && row.defect && row.defect.defectId != null
              ? Number(row.defect.defectId)
              : null
            if (!Number.isFinite(id) || seenVisual.has(id)) return
            seenVisual.add(id)
            visualOrder.push(id)
          })
        })
        const { proposals, summary } = levelDefectPlans(universe, links, {
          movableIds,
          visualOrder,
          calendar: this.activeWorkCalendar,
          ignoreCalendar: !!this.autoLevelIgnoreCalendar
        })
        if (!summary.changed) {
          this.$modal.msgSuccess(this.$t('defect.gantt.auto-level-no-change'))
          return
        }
        const proposalObj = {}
        proposals.forEach((v, k) => {
          proposalObj[k] = v
        })
        this.autoLevelPreview = {
          proposals: proposalObj,
          summary,
          universe,
          links,
          movableIds,
          visualOrder
        }
      } catch (err) {
        console.warn('[DefectGantt] auto-level preview failed', err)
        this.$modal.msgError(this.$t('defect.gantt.auto-level-fail'))
      } finally {
        this.autoLevelLoading = false
      }
    },
    cancelAutoLevel() {
      if (this.autoLevelApplying) return
      this.autoLevelPreview = null
      this.autoLevelIgnoreCalendar = false
    },
    async loadWorkCalendar() {
      const projectId = this.resolveProjectId()
      if (!projectId) {
        this.workCalendar = defaultGanttWorkCalendar()
        return
      }
      try {
        const res = await getProjectWorkCalendar(projectId)
        const cal = normalizeWorkCalendar(res.data)
        this.workCalendar = {
          workWeek: cal.workWeek,
          exceptions: cal.exceptions
        }
        this.workCalendarEditable = !!cal.editable
      } catch (e) {
        this.workCalendar = defaultGanttWorkCalendar()
        this.workCalendarEditable = false
      }
    },
    openWorkCalendar() {
      this.$refs.workCalendarDialog && this.$refs.workCalendarDialog.open()
    },
    onWorkCalendarSaved(cal) {
      if (cal) {
        this.workCalendar = {
          workWeek: cal.workWeek,
          exceptions: cal.exceptions
        }
        this.workCalendarEditable = !!cal.editable
      } else {
        this.loadWorkCalendar()
      }
      if (this.autoLevelPreview) {
        this.rebuildAutoLevelPreview()
      }
    },
    rebuildAutoLevelPreview() {
      const preview = this.autoLevelPreview
      if (!preview || !preview.universe) return
      const { proposals, summary } = levelDefectPlans(preview.universe, preview.links || [], {
        movableIds: preview.movableIds || [],
        visualOrder: preview.visualOrder || [],
        calendar: this.activeWorkCalendar,
        ignoreCalendar: !!this.autoLevelIgnoreCalendar
      })
      if (!summary.changed) {
        this.$modal.msgSuccess(this.$t('defect.gantt.auto-level-no-change'))
        return
      }
      const proposalObj = {}
      proposals.forEach((v, k) => {
        proposalObj[k] = v
      })
      this.autoLevelPreview = {
        ...preview,
        proposals: proposalObj,
        summary
      }
    },
    async confirmAutoLevel() {
      if (!this.autoLevelPreview || this.autoLevelApplying) return
      const entries = Object.entries(this.autoLevelPreview.proposals || {})
      if (!entries.length) {
        this.autoLevelPreview = null
        return
      }
      this.autoLevelApplying = true
      const projectId = this.resolveProjectId()
      let ok = 0
      let fail = 0
      const queue = entries.slice()
      const runOne = async () => {
        while (queue.length) {
          const [idStr, range] = queue.shift()
          const defectId = Number(idStr)
          const fromUniverse =
            (this.autoLevelPreview.universe || []).find((d) => Number(d.defectId) === defectId) ||
            (this.defectList || []).find((d) => Number(d.defectId) === defectId)
          try {
            await updateDefectPlanNode({
              defectId,
              projectId: fromUniverse && fromUniverse.projectId != null ? fromUniverse.projectId : projectId,
              planStartTime: range.planStartTime,
              planEndTime: range.planEndTime
            })
            ok += 1
            const local = (this.defectList || []).find((d) => Number(d.defectId) === defectId)
            if (local) {
              local.planStartTime = range.planStartTime
              local.planEndTime = range.planEndTime
            }
          } catch (err) {
            fail += 1
            console.warn('[DefectGantt] auto-level update failed', defectId, err)
          }
        }
      }
      const workers = []
      const n = Math.min(AUTO_LEVEL_UPDATE_CONCURRENCY, entries.length)
      for (let i = 0; i < n; i++) workers.push(runOne())
      await Promise.all(workers)
      this.autoLevelApplying = false
      this.autoLevelPreview = null
      this.fetchPlanGanttMeta()
      if (fail === 0) {
        this.$modal.msgSuccess(this.$t('defect.gantt.auto-level-ok', { n: ok }))
      } else {
        this.$modal.msgWarning(this.$t('defect.gantt.auto-level-partial', { ok, fail }))
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
      this.selectionAnchorId = this.selectedDefectId != null ? Number(this.selectedDefectId) : null
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
    /** 批量按自然日平移已排期缺陷（跳过不可写节点） */
    async shiftDefectsByDays(idsInput, days) {
      if (!this.ensureCanEdit()) return
      const delta = Number(days)
      if (!Number.isFinite(delta) || delta === 0) return
      const ids = (idsInput || []).map(Number).filter((id) => Number.isFinite(id))
      if (!ids.length) return
      let ok = 0
      let fail = 0
      let skipped = 0
      for (let i = 0; i < ids.length; i++) {
        const id = ids[i]
        const defect = (this.defectList || []).find((d) => d && Number(d.defectId) === id)
        if (!defect) {
          fail += 1
          continue
        }
        if (!this.canEditPlanDefect(defect)) {
          skipped += 1
          continue
        }
        const range = shiftPlanRange(defect.planStartTime, defect.planEndTime, delta, this.activeWorkCalendar)
        if (!range) {
          fail += 1
          continue
        }
        const rollback = this.snapshotDefectForRollback(defect)
        this.patchLocalDefect(id, range, null)
        try {
          await updateDefectPlanNode({
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
      if (skipped && !ok && !fail) {
        this.$modal.msgWarning(this.$t('defect.gantt.no-plan-edit-perm'))
      } else if (skipped) {
        this.$modal.msgWarning(this.$t('defect.gantt.marquee-shift-skipped', { n: skipped }))
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
  position: relative;
  display: flex;
  flex-direction: row;
  flex: 1;
  min-height: 0;
  min-width: 0;
}

.defect-gantt-minimap {
  position: absolute;
  z-index: 20;
  width: 200px;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color, #dcdfe6);
  border-radius: 4px;
  background: var(--el-bg-color, #fff);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  user-select: none;
  &.is-docked {
    right: 12px;
    bottom: 12px;
  }
}
.defect-gantt-minimap-title {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 26px;
  padding: 0 6px 0 6px;
  font-size: 12px;
  color: var(--el-text-color-regular, #606266);
  background: var(--el-fill-color-light, #f5f7fa);
  border-bottom: 1px solid var(--el-border-color-lighter, #ebeef5);
  cursor: move;
}
.defect-gantt-minimap-drag-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  color: var(--el-text-color-secondary, #909399);
  opacity: 0.85;
}
.defect-gantt-minimap-title-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.defect-gantt-minimap-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  padding: 0;
  border: none;
  border-radius: 3px;
  background: transparent;
  color: var(--el-text-color-secondary, #909399);
  cursor: pointer;
  flex: 0 0 auto;
  &:hover {
    background: var(--el-fill-color, #e9e9eb);
    color: var(--el-text-color-primary, #303133);
  }
}
.defect-gantt-minimap-body {
  position: relative;
  width: 200px;
  height: 120px;
  background:
    linear-gradient(90deg, rgba(0, 0, 0, 0.03) 1px, transparent 1px) 0 0 / 20px 100%,
    linear-gradient(rgba(0, 0, 0, 0.03) 1px, transparent 1px) 0 0 / 100% 16px,
    var(--el-fill-color-lighter, #fafafa);
  cursor: crosshair;
  overflow: hidden;
}
.defect-gantt-minimap-bar {
  position: absolute;
  border-radius: 1px;
  pointer-events: none;
  opacity: 0.9;
  &.state-created {
    background: var(--defect-state-created-bg, #909399);
  }
  &.state-processing {
    background: var(--defect-state-processing-bg, #e04e4e);
  }
  &.state-audit {
    background: var(--defect-state-audit-bg, #337ecc);
  }
  &.state-resolved {
    background: var(--defect-state-resolved-bg, #5daf34);
  }
  &.state-rejected {
    background: var(--defect-state-rejected-bg, #cf9236);
  }
  &.state-closed {
    background: var(--defect-state-closed-bg, #909399);
  }
  &.state-unknown {
    background: var(--defect-state-unknown-bg, #909399);
  }
}
.defect-gantt-minimap-viewport {
  position: absolute;
  box-sizing: border-box;
  border: none;
  /* inset 描边画在框内，贴边时也不被 overflow 裁掉 */
  box-shadow: inset 0 0 0 2px #e74c3c;
  background: rgba(231, 76, 60, 0.12);
  pointer-events: none;
  z-index: 1;
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
  /* 左右同步滚动时只保留轴侧（最右）滚动条，左侧隐藏以免双条 */
  scrollbar-width: none;
  -ms-overflow-style: none;
  &::-webkit-scrollbar {
    display: none;
  }
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

.defect-gantt-multi-icon {
  flex: 0 0 auto;
  width: 14px;
  height: 14px;
  margin-left: 6px;
  color: var(--el-text-color-secondary, #909399);
  vertical-align: middle;
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
  position: relative;
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
  /* 休息日只弱化刻度文字；假日偏红；调休偏蓝浅底（网纹画在条上，不画在轴背景） */
  &.is-rest {
    .defect-gantt-axis-day-num,
    .defect-gantt-axis-weekday,
    .defect-gantt-axis-week-num,
    .defect-gantt-axis-week-range {
      color: var(--el-text-color-placeholder, #a8abb2);
    }
  }
  &.holiday {
    .defect-gantt-axis-day-num,
    .defect-gantt-axis-weekday,
    .defect-gantt-axis-week-num,
    .defect-gantt-axis-week-range {
      color: var(--el-color-danger, #f56c6c);
    }
  }
  &.makeup {
    background-color: color-mix(in srgb, var(--el-color-primary, #409eff) 14%, transparent);
    .defect-gantt-axis-day-num,
    .defect-gantt-axis-weekday,
    .defect-gantt-axis-week-num,
    .defect-gantt-axis-week-range {
      color: var(--el-color-primary, #409eff);
    }
  }
  &.is-day-scale,
  &.is-week-scale {
    padding: 2px 0;
  }
}

.defect-gantt-col-resize {
  position: absolute;
  top: 0;
  right: -4px;
  bottom: 0;
  width: 8px;
  z-index: 5;
  cursor: col-resize;
  touch-action: none;
}

body.defect-gantt-col-resizing {
  cursor: col-resize !important;
  user-select: none !important;
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
  &.is-rest {
    background: color-mix(in srgb, var(--el-text-color-secondary, #909399) 5%, transparent);
  }
  &.holiday {
    background: color-mix(in srgb, var(--el-color-danger, #f56c6c) 8%, transparent);
  }
  &.makeup {
    background: color-mix(in srgb, var(--el-color-primary, #409eff) 10%, transparent);
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
  /* 标题放到条外深色字：避免白字溢出到浅蓝插入行上看不清（明/暗主题同理） */
  .defect-gantt-bar-label {
    position: absolute;
    left: 100%;
    top: 50%;
    transform: translateY(-50%);
    margin-left: 6px;
    max-width: 280px;
    padding: 0;
    color: var(--el-text-color-regular, #606266);
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.defect-gantt-bar-rest-mask {
  position: absolute;
  /* 外扩 1px 盖住条边框，休息段内外一起被网纹打断 */
  inset: -1px;
  border-radius: inherit;
  overflow: hidden;
  pointer-events: none;
  z-index: 2;
}
.defect-gantt-bar-rest {
  position: absolute;
  top: 0;
  bottom: 0;
  display: block;
  /* 半透明底压住边框色 + 斜线网纹贯穿上下边 */
  background-color: rgba(255, 255, 255, 0.42);
  background-image: repeating-linear-gradient(
    -45deg,
    rgba(0, 0, 0, 0.38) 0,
    rgba(0, 0, 0, 0.38) 1.5px,
    transparent 1.5px,
    transparent 5px
  );
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
  &.is-plan-readonly {
    cursor: not-allowed;
  }
  &.is-forbidden {
    --gantt-forbid-mark: rgba(255, 255, 255, 0.92);
    cursor: not-allowed;
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
  &.is-multi-handler {
    padding-left: 18px;
  }
  &.state-created {
    background: var(--defect-state-created-bg, #909399);
    border-color: var(--defect-state-created-border, #909399);
    color: var(--defect-state-created-color, #fff);
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

.defect-gantt-bar-multi {
  position: absolute;
  left: 3px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  pointer-events: auto;
  color: rgba(255, 255, 255, 0.95);
  filter: drop-shadow(0 0 1px rgba(0, 0, 0, 0.35));
}
.defect-gantt-bar-multi-svg {
  width: 12px;
  height: 12px;
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
  /* 继承条上状态色字色，不要描边/光晕边框 */
  color: inherit;
}
.defect-gantt-bar-num {
  flex: 0 0 auto;
  font-weight: 600;
  white-space: nowrap;
  color: inherit;
}
.defect-gantt-bar-name {
  flex: 1 1 auto;
  min-width: 0;
  margin-left: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: inherit;
}

/* 条右外侧：处理人头像 / 缺陷标题（图层开关控制） */
.defect-gantt-bar-meta {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 3;
  display: inline-flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: center;
  gap: 6px;
  max-width: 280px;
  pointer-events: none;
  user-select: none;
}
/* 条后处理人：多人负间距叠加（RowListMember） */
.defect-gantt-bar-handlers {
  flex: 0 0 auto;
  line-height: 0;
  pointer-events: auto;
  :deep(.el-avatar) {
    border: 1px solid var(--el-bg-color, #fff) !important;
    box-sizing: border-box;
  }
}
.defect-gantt-bar-days {
  flex: 0 0 auto;
  white-space: nowrap;
  font-size: 12px;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
  color: var(--el-text-color-secondary, #909399);
}
.defect-gantt-bar-defect-title {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  line-height: 1.2;
  color: var(--el-text-color-regular, #606266);
}

.defect-gantt-bar-handle {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 8px;
  z-index: 6;
  cursor: ew-resize;
  opacity: 0;
  pointer-events: auto;
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
  opacity: 0.45;
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
  /* 整圆在条外，与内侧 8px 拉宽手柄错开 */
  &.left {
    left: auto;
    right: 100%;
    margin-right: 2px;
  }
  &.right {
    right: auto;
    left: 100%;
    margin-left: 2px;
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
  max-width: 240px;
  min-width: 120px;
  /* 与处理人选择器统一小号高度 */
  :deep(.el-input__wrapper) {
    height: var(--cat2bug-toolbar-control-height, var(--el-component-size-small, 32px));
    min-height: var(--cat2bug-toolbar-control-height, var(--el-component-size-small, 32px));
  }
}

.defect-gantt-unscheduled-handler {
  flex: 0 1 180px;
  min-width: 128px;
  max-width: 200px;
  display: inline-flex;
  align-items: center;
  align-self: center;
  height: var(--cat2bug-toolbar-control-height, var(--el-component-size-small, 32px));
  /* 未排期不在工具栏作用域，需本地压成与左侧 small input 同高 */
  :deep(.select-project-member-input) {
    width: 100% !important;
    max-width: 100% !important;
    height: var(--cat2bug-toolbar-control-height, var(--el-component-size-small, 32px)) !important;
    min-height: var(--cat2bug-toolbar-control-height, var(--el-component-size-small, 32px)) !important;
    max-height: var(--cat2bug-toolbar-control-height, var(--el-component-size-small, 32px)) !important;
    line-height: 1 !important;
    font-size: 12px;
    box-sizing: border-box;
  }
  :deep(.select-project-member-input__prefix-icon),
  :deep(.select-project-member-input__icon) {
    font-size: 12px;
  }
  :deep(.select-project-member-tag.el-tag) {
    height: 20px;
    line-height: 20px;
    font-size: 12px;
  }
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
  &.state-created {
    background: var(--defect-state-created-bg, #909399);
    color: var(--defect-state-created-color, #fff);
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
  &.is-plan-readonly {
    cursor: not-allowed;
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
  &.is-loading {
    color: var(--el-color-primary, #409eff);
  }
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
  &.state-created {
    background: var(--defect-state-created-bg, #909399);
    color: var(--defect-state-created-color, #fff);
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
html.dark .defect-gantt-bar.is-drop-preview .defect-gantt-bar-label {
  color: var(--el-text-color-regular, #cfd3dc);
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

.defect-gantt-auto-level-banner {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 16px;
  padding: 8px 12px;
  font-size: 13px;
  line-height: 1.4;
  color: var(--el-color-primary, #409eff);
  background: var(--el-color-primary-light-9, #ecf5ff);
  border-bottom: 1px solid var(--el-color-primary-light-7, #d9ecff);
  box-sizing: border-box;
}
.defect-gantt-auto-level-banner-text {
  flex: 1 1 240px;
  min-width: 0;
}
.defect-gantt-auto-level-banner-note {
  display: inline;
  margin-left: 6px;
  color: var(--el-text-color-secondary, #909399);
  font-size: 12px;
}
.defect-gantt-auto-level-banner-actions {
  display: inline-flex;
  flex-direction: row;
  gap: 10px;
  flex: 0 0 auto;
  :deep(.el-button + .el-button) {
    margin-left: 0;
  }
}
.defect-gantt-bar.is-auto-level-ghost {
  z-index: 3;
  opacity: 0.28;
  pointer-events: none;
  filter: grayscale(0.2);
}
.defect-gantt-bar.is-auto-level-proposal {
  outline: 2px dashed var(--el-color-primary, #409eff);
  outline-offset: 1px;
  z-index: 5;
}
.defect-gantt-bar.is-auto-level-push {
  outline: 2px dashed var(--el-color-primary, #409eff);
  outline-offset: 1px;
  z-index: 5;
  opacity: 0.92;
}

/* —— 甘特二期：工具条 / 依赖 / 关键路径 / 负荷 / 基线 / 框选 —— */
.defect-gantt-tools-divider {
  margin: 0 8px;
  align-self: center;
  height: calc(var(--cat2bug-toolbar-control-height, 32px) - 8px);
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