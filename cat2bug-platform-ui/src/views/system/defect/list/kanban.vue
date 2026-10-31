<template>
  <div class="defect-kanban-root">
    <div class="defect-kanban-tools defect-view-toolbar">
      <slot name="left-tools"></slot>
      <div class="table-tools row defect-kanban-tools-right">
        <slot name="right-tools"></slot>
      </div>
    </div>

    <div
      ref="kanbanBoard"
      v-loading="loading"
      class="defect-kanban-board"
      :class="{
        'is-lane-resizing': laneResizing,
        'is-card-dragging': draggingIds.length > 0
      }"
    >
      <template v-for="(lane, laneIndex) in lanes" :key="lane.key">
      <div
        class="defect-kanban-lane"
        :class="[
          'lane-' + lane.key,
          {
            'is-drop-forbidden': isLaneDropForbiddenDuringDrag(lane.key)
          }
        ]"
        :style="laneResizing ? null : laneWidthStyleMap[lane.key]"
        @dragover.prevent="onLaneDragOver(lane.key, $event)"
        @dragleave="onLaneDragLeave(lane.key, $event)"
        @drop.prevent="onLaneDrop(lane.key)"
      >
        <div class="defect-kanban-lane-head">
          <span class="defect-kanban-lane-dot" aria-hidden="true" />
          <span class="defect-kanban-lane-title">{{ lane.label }}</span>
          <span class="defect-kanban-lane-count">{{ laneCards(lane.key).length }}</span>
          <!-- 无负责人槽时用 spacer 顶右；有负责人槽时由 assignees flex:1 占满剩余宽以便按宽算头像数 -->
          <div
            v-if="!laneShowsAssignees(lane.key)"
            class="defect-kanban-lane-head-spacer"
            aria-hidden="true"
          />
          <!-- 最右侧固定：处理中/待验证=负责人；已创建=添加 -->
          <div
            v-if="laneShowsAssignees(lane.key)"
            :ref="'laneAssigneesSlot-' + lane.key"
            class="defect-kanban-lane-assignees"
            @mousedown.stop
            @click.stop
          >
            <button
              type="button"
              class="defect-kanban-lane-assignee-avatars"
              :class="{ 'is-empty': !visibleLaneAssigneeMembers(lane.key).length }"
              :title="
                visibleLaneAssigneeMembers(lane.key).length
                  ? $t('defect.kanban.lane-assignees')
                  : $t('defect.kanban.lane-assignees-all')
              "
              :aria-label="
                visibleLaneAssigneeMembers(lane.key).length
                  ? $t('defect.kanban.lane-assignees')
                  : $t('defect.kanban.lane-assignees-all')
              "
              @click="openLaneAssigneePicker(lane.key, $event)"
            >
              <row-list-member
                v-if="visibleLaneAssigneeMembers(lane.key).length"
                :members="visibleLaneAssigneeMembers(lane.key)"
                :max="laneAssigneeAvatarMax(lane.key)"
                size="small"
              />
              <span
                v-else
                class="defect-kanban-lane-assignee-empty"
                aria-hidden="true"
              >
                <legacy-icon icon="el-icon-plus" class="defect-kanban-lane-assignee-empty-icon" />
              </span>
            </button>
            <div
              class="defect-kanban-lane-assignee-picker"
              aria-hidden="true"
              :style="laneAssigneePickerStyle[lane.key] || null"
            >
              <select-project-member
                :ref="'laneAssigneePicker-' + lane.key"
                :member-id="laneAssigneeIds[lane.key]"
                :project-id="projectId"
                :is-head="false"
                :multiple="true"
                size="small"
                placeholder="defect.kanban.lane-assignees"
                :enabled-member-ids="laneAssigneeEnabledIds"
                @update:memberId="(ids) => onLaneAssigneesChange(lane.key, ids)"
              />
            </div>
          </div>
          <button
            v-else-if="lane.key === 'created'"
            type="button"
            class="defect-kanban-lane-add-btn"
            :disabled="!canAddDefect"
            :title="$t('defect.create')"
            :aria-label="$t('defect.create')"
            @mousedown.stop
            @click.stop="onUnassignedAddClick"
          >
            <legacy-icon icon="el-icon-plus" class="defect-kanban-lane-add-icon" />
          </button>
          <button
            v-else-if="lane.key === 'closed' && canShowLaneTrash"
            type="button"
            class="defect-kanban-lane-trash"
            :class="{ 'is-armed': canDropDraggingToTrash }"
            data-kanban-trash="1"
            :title="$t('defect.kanban.trash-title')"
            :aria-label="$t('defect.kanban.trash-title')"
            @mousedown.stop
            @click.stop
            @dragover.prevent.stop="onTrashDragOver($event)"
            @dragleave="onTrashDragLeave($event)"
            @drop.prevent.stop="onTrashDrop"
          >
            <legacy-icon icon="el-icon-delete" class="defect-kanban-lane-trash-icon" />
          </button>
          <div
            v-else
            class="defect-kanban-lane-assignees is-spacer"
            aria-hidden="true"
          >
            <div class="defect-kanban-lane-assignee-avatars is-spacer" />
          </div>
        </div>
        <!-- 搜索在滚动区外，避免卡片上滚穿层 -->
        <div
          class="defect-kanban-lane-search-wrap"
          @mousedown.stop
          @click.stop
        >
          <el-input
            v-model="laneKeyword[lane.key]"
            class="defect-kanban-lane-search"
            size="small"
            clearable
            :prefix-icon="Search"
            :placeholder="$t('defect.kanban.lane-search')"
            @input="resetLaneVisible(lane.key)"
            @clear="resetLaneVisible(lane.key)"
          />
        </div>
        <div
          class="defect-kanban-lane-body"
          @scroll="onLaneScroll(lane.key, $event)"
        >
          <div
            v-if="laneShowsAssignees(lane.key)"
            v-show="showAssigneeDropBar(lane.key)"
            class="defect-kanban-assignee-drop-bar"
            @dragover.prevent.stop="onAssigneeDropBarDragOver(lane.key, $event)"
            @drop.prevent.stop="onAssigneeDropBarDrop(lane.key)"
          >
            <div class="defect-kanban-assignee-drop-hint">
              {{ assigneeDropHint(lane.key) }}
            </div>
            <div class="defect-kanban-assignee-drop-list">
              <div
                v-for="m in visibleLaneAssigneeMembers(lane.key)"
                :key="'drop-' + lane.key + '-' + m.userId"
                class="defect-kanban-assignee-drop-target"
                :data-user-id="m.userId"
                @dragover.prevent.stop="onAssigneeDragOver(lane.key, m.userId, $event)"
                @dragleave="onAssigneeDragLeave(m.userId, $event)"
                @drop.prevent.stop="onAssigneeDrop(lane.key, m.userId)"
              >
                <cat2-bug-avatar :member="m" :size="40" />
                <span class="defect-kanban-assignee-drop-name">{{ m.nickName || m.userName }}</span>
              </div>
              <div
                v-if="!visibleLaneAssigneeMembers(lane.key).length"
                class="defect-kanban-assignee-drop-empty"
              >{{ $t('defect.kanban.lane-assignees-empty') }}</div>
            </div>
          </div>
          <div
            v-if="showClosedStateDropBar(lane.key)"
            class="defect-kanban-assignee-drop-bar defect-kanban-state-drop-bar"
            @dragover.prevent.stop="onClosedStateDropBarDragOver($event)"
            @drop.prevent.stop="onClosedStateDropBarDrop"
          >
            <div class="defect-kanban-assignee-drop-hint">
              {{ $t('defect.kanban.closed-drop-hint') }}
            </div>
            <div class="defect-kanban-assignee-drop-list">
              <div
                v-if="canDropPassToClosed"
                class="defect-kanban-assignee-drop-target defect-kanban-state-drop-target is-pass is-default"
                data-drop-action="pass"
                @dragover.prevent.stop="onClosedStateDragOver('pass', $event)"
                @dragleave="onClosedStateDragLeave('pass', $event)"
                @drop.prevent.stop="onClosedStateDrop('pass')"
              >
                <span class="defect-kanban-state-drop-icon is-pass" aria-hidden="true">
                  <el-icon :size="26"><component :is="CircleCheckFilledIcon" /></el-icon>
                </span>
                <span class="defect-kanban-assignee-drop-name">{{ $t('RESOLVED') }}</span>
              </div>
              <div
                v-if="canDropCloseToClosed"
                class="defect-kanban-assignee-drop-target defect-kanban-state-drop-target is-close"
                data-drop-action="close"
                @dragover.prevent.stop="onClosedStateDragOver('close', $event)"
                @dragleave="onClosedStateDragLeave('close', $event)"
                @drop.prevent.stop="onClosedStateDrop('close')"
              >
                <span class="defect-kanban-state-drop-icon is-close" aria-hidden="true">
                  <el-icon :size="26"><component :is="CircleCloseFilledIcon" /></el-icon>
                </span>
                <span class="defect-kanban-assignee-drop-name">{{ $t('CLOSED') }}</span>
              </div>
            </div>
          </div>
          <div class="defect-kanban-card-list">
            <div
              v-for="(d, idx) in visibleLaneCards(lane.key)"
              :key="d.defectId"
              class="defect-kanban-card"
              :data-defect-id="d.defectId"
              :class="[
                'state-' + cardStateClass(d),
                {
                  'is-selected': isSelected(d.defectId),
                  'is-dragging': isDraggingCard(d.defectId),
                  'is-drag-locked': !canDragCard(d),
                  'is-collected': isDefectCollected(d),
                  'is-collect-flying': collectFlyingId === Number(d.defectId)
                }
              ]"
              :draggable="canDragCard(d)"
              :title="canDragCard(d) ? undefined : $t('defect.kanban.drag-locked')"
              @click="onCardClick(d, lane.key, idx, $event)"
              @dblclick.stop="openDefect(d)"
              @dragstart="onCardDragStart(d, lane.key, $event)"
              @dragend="onCardDragEnd"
            >
              <div class="defect-kanban-card-accent" aria-hidden="true" />
              <div class="defect-kanban-card-body">
                <div class="defect-kanban-card-meta">
                  <div
                    v-if="(d.handleByList || []).length"
                    class="defect-kanban-card-handlers"
                    @mousedown.stop
                    @click.stop
                  >
                    <row-list-member
                      :members="d.handleByList || []"
                      :max="3"
                      :size="28"
                    />
                  </div>
                  <span class="defect-kanban-card-num">#{{ d.projectNum }}</span>
                  <defect-type-flag :defect="d" class="defect-kanban-type-flag" />
                  <span
                    v-if="showCardPriority && d.defectLevel"
                    class="defect-kanban-level"
                    :title="$t('priority')"
                  >{{ defectLevelText(d.defectLevel) }}</span>
                </div>
                <div class="defect-kanban-card-title" :title="d.defectName">
                  <span
                    class="defect-kanban-card-title-text"
                    role="link"
                    @click.stop="openDefect(d)"
                  >{{ d.defectName }}</span>
                </div>
                <div
                  v-if="cardImages(d).length"
                  class="defect-kanban-card-media"
                  @mousedown.stop
                  @click.stop
                >
                  <cat2-bug-preview-image
                    class="defect-kanban-card-preview"
                    :images="cardImages(d)"
                    :width="220"
                    :height="88"
                    fit="cover"
                  />
                </div>
                <div class="defect-kanban-card-foot">
                  <defect-state-flag :defect="d" class="defect-kanban-state-flag" />
                  <!-- 勿用 v-if 隐藏：拖起/投放时拆装子树会打断原生 DnD，偶发导致下次拖不动 -->
                  <div
                    class="defect-kanban-card-tools"
                    @mousedown.stop
                    @click.stop
                    @dragstart.prevent
                  >
                    <defect-tools
                      class="defect-kanban-row-tools"
                      :is-text="true"
                      :compact="true"
                      :defect="d"
                      size="small"
                      :is-show-icon="true"
                      @view="openDefect"
                      @delete="onCardToolsDone"
                      @restore="onCardToolsDone"
                      @update="onCardToolsDone"
                      @log="onCardToolsDone"
                      @collect="onCardCollect"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-if="!laneCards(lane.key).length && !loading" class="defect-kanban-lane-empty">
            {{ $t('defect.kanban.empty-lane') }}
          </div>
        </div>
      </div>
      <div
        v-if="laneIndex < lanes.length - 1"
        class="defect-kanban-lane-resizer"
        role="separator"
        aria-orientation="vertical"
        :title="$t('defect.kanban.resize-lane')"
        :aria-label="$t('defect.kanban.resize-lane')"
        @mousedown.prevent="onLaneResizeStart(laneIndex, $event)"
        @dblclick.prevent="resetLaneWidths"
      />
      </template>
    </div>

    <assign-dialog
      ref="assignDialog"
      :project-id="projectId"
      :defect-id="actionDefectId"
      @log="onActionDone"
    />
    <repair-dialog
      ref="repairDialog"
      :project-id="projectId"
      :defect-id="actionDefectId"
      @log="onActionDone"
    />
    <reject-dialog
      ref="rejectDialog"
      :project-id="projectId"
      :defect-id="actionDefectId"
      @log="onActionDone"
    />
    <pass-dialog
      ref="passDialog"
      :project-id="projectId"
      :defect-id="actionDefectId"
      @log="onActionDone"
    />
    <close-dialog
      ref="closeDialog"
      :project-id="projectId"
      :defect-id="actionDefectId"
      @log="onActionDone"
    />
    <open-dialog
      ref="openDialog"
      :project-id="projectId"
      :defect-id="actionDefectId"
      @log="onActionDone"
    />
    <defect-batch-action-dialog
      ref="batchDialog"
      :project-id="projectId"
      @done="onBatchDone"
    />
  </div>
</template>

<script>
import { markRaw } from 'vue'
import { CircleCheckFilled, CircleCloseFilled, Search } from '@element-plus/icons-vue'
import { listDefect, assign, repair, delDefect } from '@/api/system/defect'
import { strFormat } from '@/utils'
import { getProject, listMemberOfProject, listProjectRole } from '@/api/system/project'
import { useUserStore } from '@/stores/user'
import { checkPermi } from '@/utils/permission'
import DefectTypeFlag from '@/components/Defect/DefectTypeFlag'
import DefectStateFlag from '@/components/Defect/DefectStateFlag'
import RowListMember from '@/components/RowListMember'
import Cat2BugAvatar from '@/components/Cat2BugAvatar'
import Cat2BugPreviewImage from '@/components/Cat2BugPreviewImage'
import SelectProjectMember from '@/components/Project/SelectProjectMember'
import DefectTools from '@/components/Defect/DefectTools'
import AssignDialog from '@/components/Defect/DefectTools/AssignDialog'
import RepairDialog from '@/components/Defect/DefectTools/RepairDialog'
import RejectDialog from '@/components/Defect/DefectTools/RejectDialog'
import PassDialog from '@/components/Defect/DefectTools/PassDialog'
import CloseDialog from '@/components/Defect/DefectTools/CloseDialog'
import OpenDialog from '@/components/Defect/DefectTools/OpenDialog'
import DefectBatchActionDialog from '@/components/Defect/DefectBatchActionDialog'
import { isDefectActionVisible } from '@/utils/defect-action-visibility'
import {
  loadEnabledImageFields,
  candidateUrlsForImageField
} from '@/utils/share-card-images'
import { loadDefectColumnLayout } from '@/utils/defect-custom-field-columns'
import { isBuiltinFormFieldVisible } from '@/utils/defect-field-layout'
import { resolveCommaSeparatedUrls } from '@/utils/upload-asset'
import {
  handleByFilterSet,
  filterIdsByHandleBy,
  defectMatchesLaneAssignees,
  buildMemberMapFromList,
  projectContextFromMembers,
  normalizeIdList,
  resolveLaneAssigneeIds,
  defaultAssigneeIdsForLane,
  writeLaneAssigneesCache,
  ROLE_DEVELOP_KEYS,
  ROLE_TESTER_KEYS,
  laneAssigneesStorageKey
} from './kanban-lane-assignees'

const KANBAN_IMG_CACHE = new WeakMap()

const LANE_CREATED = 'created'
const LANE_PROCESSING = 'processing'
const LANE_AUDIT = 'audit'
const LANE_CLOSED = 'closed'
/** @deprecated 兼容旧引用，等同 LANE_CREATED */
const LANE_UNASSIGNED = LANE_CREATED
const KANBAN_PAGE_SIZE = 200
const LANE_VISIBLE_PAGE = 10
const LOAD_MORE_THRESHOLD_PX = 80

function defaultLaneVisibleLimit() {
  return {
    [LANE_CREATED]: LANE_VISIBLE_PAGE,
    [LANE_PROCESSING]: LANE_VISIBLE_PAGE,
    [LANE_AUDIT]: LANE_VISIBLE_PAGE,
    [LANE_CLOSED]: LANE_VISIBLE_PAGE
  }
}

const LANE_WIDTH_KEYS = Object.freeze([
  LANE_CREATED,
  LANE_PROCESSING,
  LANE_AUDIT,
  LANE_CLOSED
])
/** 过窄时标题+负责人放不下，限制最小宽度 */
const LANE_WIDTH_MIN_PX = 220
/** 泳道负责人头像：宽度够时尽量多展，上限 */
const LANE_ASSIGNEE_AVATAR_MAX = 8
/** 与 Cat2BugAvatar size=small / .el-avatar--small 实测一致 */
const LANE_ASSIGNEE_AVATAR_SIZE = 29
/** 多用户叠放负 margin，与 RowListMember 默认一致 */
const LANE_ASSIGNEE_AVATAR_OVERLAP = 8
/** 溢出角标突出预留 */
const LANE_ASSIGNEE_BADGE_PAD = 12
const LANE_RESIZER_WIDTH_PX = 12
const LANE_WIDTH_EQUAL = Object.freeze({
  [LANE_CREATED]: 25,
  [LANE_PROCESSING]: 25,
  [LANE_AUDIT]: 25,
  [LANE_CLOSED]: 25
})

function laneWidthsStorageKey(projectId) {
  return `defect.kanban.laneWidths.${projectId}`
}

function defaultLaneWidthPct() {
  return { ...LANE_WIDTH_EQUAL }
}

/** @returns {Record<string, number>} */
function readLaneWidthPctCache(projectId) {
  if (projectId == null || projectId === '') return defaultLaneWidthPct()
  try {
    const raw = localStorage.getItem(laneWidthsStorageKey(projectId))
    if (!raw) return defaultLaneWidthPct()
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') return defaultLaneWidthPct()
    const next = {}
    let sum = 0
    for (const key of LANE_WIDTH_KEYS) {
      const n = Number(parsed[key])
      if (!Number.isFinite(n) || n <= 0) return defaultLaneWidthPct()
      next[key] = n
      sum += n
    }
    if (sum <= 0) return defaultLaneWidthPct()
    // 归一化为 100，避免历史脏数据导致溢出
    for (const key of LANE_WIDTH_KEYS) {
      next[key] = (next[key] / sum) * 100
    }
    return next
  } catch (e) {
    return defaultLaneWidthPct()
  }
}

function writeLaneWidthPctCache(projectId, pct) {
  if (projectId == null || projectId === '') return
  try {
    const payload = {}
    for (const key of LANE_WIDTH_KEYS) {
      payload[key] = Number(pct && pct[key]) || LANE_WIDTH_EQUAL[key]
    }
    localStorage.setItem(laneWidthsStorageKey(projectId), JSON.stringify(payload))
  } catch (e) {
    /* ignore quota */
  }
}

const SINGLE_DIALOG_REF = {
  assign: 'assignDialog',
  repair: 'repairDialog',
  reject: 'rejectDialog',
  pass: 'passDialog',
  close: 'closeDialog',
  open: 'openDialog'
}

function laneOf(defect) {
  const s = defect && defect.defectStateName
  if (s === 'CREATED') return LANE_CREATED
  if (s === 'PROCESSING' || s === 'REJECTED') return LANE_PROCESSING
  if (s === 'AUDIT') return LANE_AUDIT
  if (s === 'RESOLVED' || s === 'CLOSED') return LANE_CLOSED
  return null
}

/**
 * @param {string} fromLane
 * @param {string} toLane
 * @param {object[]} defects
 * @param {number|string} userId
 * @returns {string|null}
 */
export function resolveKanbanDropAction(fromLane, toLane, defects, userId) {
  if (!fromLane || !toLane || fromLane === toLane) return null
  if (!defects || !defects.length) return null
  /* 已创建只出不进；出列：处理中=assign，已结束=close */
  if (toLane === LANE_CREATED) return null
  if (fromLane === LANE_CREATED) {
    if (toLane === LANE_PROCESSING) return 'assign'
    if (toLane === LANE_CLOSED) return 'close'
    return null
  }
  if (fromLane === LANE_PROCESSING && toLane === LANE_AUDIT) return 'repair'
  if (fromLane === LANE_AUDIT && toLane === LANE_PROCESSING) return 'reject'
  if (fromLane === LANE_PROCESSING && toLane === LANE_CLOSED) return 'close'
  if (fromLane === LANE_AUDIT && toLane === LANE_CLOSED) {
    if (defects.every((d) => isDefectActionVisible('pass', d, userId))) return 'pass'
    if (defects.every((d) => isDefectActionVisible('close', d, userId))) return 'close'
    return null
  }
  if (fromLane === LANE_CLOSED && toLane === LANE_PROCESSING) return 'open'
  return null
}

/**
 * 看板卡片是否可拖向垃圾桶（与泳道流转权限独立）。
 * @param {object} defect
 * @param {number|string} userId
 * @returns {boolean}
 */
export function canKanbanTrashDrag(defect, userId) {
  if (!defect) return false
  if (!checkPermi(['system:defect:remove'])) return false
  return isDefectActionVisible('delete', defect, userId)
}

/**
 * 看板卡片是否因泳道流转而可拖（须为处理人等）。
 * 不含仅「关闭」——否则有关闭权的人几乎每张卡都可拖，UI 无法区分。
 * 不含删除：删除投放见 canKanbanTrashDrag，由 canDragCard 合并。
 * 已创建列：可指派或可关闭即可拖（拖向处理中 / 已结束）。
 * 处理中：仅当前处理人可拖向待验证；非本人若仅有删除权仍可拖（只进垃圾桶）。
 * @param {object} defect
 * @param {number|string} userId
 * @returns {boolean}
 */
export function canKanbanDragCard(defect, userId) {
  if (!defect) return false
  const lane = laneOf(defect)
  if (!lane) return false
  if (lane === LANE_CREATED) {
    return (
      isDefectActionVisible('assign', defect, userId) ||
      isDefectActionVisible('close', defect, userId)
    )
  }
  if (lane === LANE_PROCESSING) {
    /* 仅 repair（当前处理人）可进待验证；非处理人不在此放行 */
    return isDefectActionVisible('repair', defect, userId)
  }
  if (lane === LANE_AUDIT) {
    return (
      isDefectActionVisible('reject', defect, userId) ||
      isDefectActionVisible('pass', defect, userId)
    )
  }
  if (lane === LANE_CLOSED) {
    return isDefectActionVisible('open', defect, userId)
  }
  return false
}

export default {
  name: 'DefectKanban',
  components: {
    DefectTypeFlag,
    DefectStateFlag,
    RowListMember,
    Cat2BugAvatar,
    Cat2BugPreviewImage,
    SelectProjectMember,
    AssignDialog,
    RepairDialog,
    RejectDialog,
    PassDialog,
    CloseDialog,
    OpenDialog,
    DefectBatchActionDialog,
    DefectTools,
    Search
  },
  props: {
    projectId: { type: Number, default: null },
    query: { type: Object, default: () => ({}) }
  },
  emits: ['defect-click', 'refresh', 'add'],
  setup() {
    return {
      userStore: useUserStore(),
      /* Options API：setup 返回值作绑定用 :is / 属性，勿当未注册组件标签 */
      Search: markRaw(Search),
      CircleCheckFilledIcon: markRaw(CircleCheckFilled),
      CircleCloseFilledIcon: markRaw(CircleCloseFilled)
    }
  },
  data() {
    return {
      loading: false,
      loadingMore: false,
      requestGen: 0,
      defectList: [],
      total: 0,
      listPageNum: 1,
      queryParams: {},
      selectedIds: [],
      lastSelect: null,
      draggingIds: [],
      dragFromLane: null,
      /** 收藏置顶飞卡动画中的缺陷 id */
      collectFlyingId: null,
      /** 字段管理启用的内置字段；布局未加载完为 []，避免误显可选字段 */
      enabledBuiltinFieldKeys: [],
      _fieldLayoutProjectId: null,
      /** 拖起时缓存：可落入的泳道 key，避免 dragover 反复算权限 */
      dragAllowedLanes: null,
      /** 拖起时缓存：禁止落入的泳道（模板只查 Set，避免反复算） */
      dragForbiddenLanes: null,
      actionDefectId: null,
      imageFields: [],
      _imageFieldsProjectId: null,
      laneKeyword: {
        [LANE_CREATED]: '',
        [LANE_PROCESSING]: '',
        [LANE_AUDIT]: '',
        [LANE_CLOSED]: ''
      },
      laneVisibleLimit: defaultLaneVisibleLimit(),
      laneAssigneeIds: {
        [LANE_PROCESSING]: [],
        [LANE_AUDIT]: []
      },
      memberById: new Map(),
      deletingDirect: false,
      repairingDirect: false,
      assigningDirect: false,
      _assigneesProjectId: null,
      laneAssigneePickerStyle: {
        [LANE_PROCESSING]: null,
        [LANE_AUDIT]: null
      },
      /** 泳道宽度占比（合计约 100） */
      laneWidthPct: defaultLaneWidthPct(),
      laneResizing: false,
      /** 看板内容区宽度，用于按像素估算可展示头像数 */
      boardWidthPx: 0,
      /** 各泳道负责人槽实测宽度（px） */
      laneAssigneeSlotWidth: {
        [LANE_PROCESSING]: 0,
        [LANE_AUDIT]: 0
      }
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.setupBoardWidthObserver()
      this.setupAssigneeSlotObservers()
    })
  },
  watch: {
    projectId: {
      immediate: true,
      handler(pid) {
        this.ensureImageFields(pid)
        this.ensureLaneAssignees(pid)
        this.ensureFieldLayout(pid)
        this.loadLaneWidths(pid)
        this.$nextTick(() => {
          this.measureBoardWidth()
          this.setupAssigneeSlotObservers()
        })
      }
    },
    laneWidthPct: {
      deep: true,
      handler() {
        this.$nextTick(() => this.syncLaneAssigneeSlotWidths())
      }
    },
    laneAssigneeIds: {
      deep: true,
      handler() {
        this.$nextTick(() => this.syncLaneAssigneeSlotWidths())
      }
    }
  },
  computed: {
    /** 字段管理未启用优先级时卡片不展示；布局未加载完先不显示，避免闪一下 */
    showCardPriority() {
      if (this._fieldLayoutProjectId == null) return false
      return isBuiltinFormFieldVisible('defectLevel', this.enabledBuiltinFieldKeys)
    },
    lanes() {
      return [
        { key: LANE_CREATED, label: this.$t('defect.kanban.lane-created') },
        {
          key: LANE_PROCESSING,
          label: this.isLaneAssigneesOnlyMe(LANE_PROCESSING)
            ? this.$t('defect.kanban.lane-processing-mine')
            : this.$t('defect.kanban.lane-processing')
        },
        {
          key: LANE_AUDIT,
          label: this.isLaneAssigneesOnlyMe(LANE_AUDIT)
            ? this.$t('defect.kanban.lane-audit-mine')
            : this.$t('defect.kanban.lane-audit')
        },
        { key: LANE_CLOSED, label: this.$t('defect.kanban.lane-closed') }
      ]
    },
    currentUserId() {
      const raw = this.userStore && this.userStore.id
      return raw != null && raw !== '' ? Number(raw) : null
    },
    hasMoreDefects() {
      return this.defectList.length < this.total
    },
    canAddDefect() {
      return checkPermi(['system:defect:add'])
    },
    /** 已结束泳道标题右侧垃圾桶：需具备缺陷删除权限 */
    canShowLaneTrash() {
      return checkPermi(['system:defect:remove'])
    },
    /** 当前拖起的缺陷是否均可拖入垃圾桶删除 */
    canDropDraggingToTrash() {
      if (!this.canShowLaneTrash || !this.draggingIds.length) return false
      const idSet = this.draggingIdSet
      const defects = this.defectList.filter((d) => idSet.has(Number(d.defectId)))
      if (!defects.length || defects.length !== idSet.size) return false
      return defects.every((d) => canKanbanTrashDrag(d, this.currentUserId))
    },
    handleBySet() {
      const live = this.query && this.query.handleBy
      if (live != null && live !== '') return handleByFilterSet(live)
      return handleByFilterSet(this.queryParams && this.queryParams.handleBy)
    },
    /** 泳道负责人下拉：仅全局统一搜索中的执行人可选；null=不限制 */
    laneAssigneeEnabledIds() {
      const set = this.handleBySet
      return set ? Array.from(set) : null
    },
    draggingIdSet() {
      return new Set((this.draggingIds || []).map(Number))
    },
    /** 泳道宽度 style 引用稳定；拖宽中改用 DOM，不走本 map */
    laneWidthStyleMap() {
      const map = {}
      for (const key of LANE_WIDTH_KEYS) {
        const pct = Number(this.laneWidthPct && this.laneWidthPct[key])
        const grow = Number.isFinite(pct) && pct > 0 ? pct : 25
        map[key] = {
          flex: `${grow} ${grow} 0`,
          minWidth: `${LANE_WIDTH_MIN_PX}px`
        }
      }
      return map
    },
    draggingDefects() {
      if (!this.draggingIds.length) return []
      const idSet = this.draggingIdSet
      return this.defectList.filter((d) => idSet.has(Number(d.defectId)))
    },
    canDropPassToClosed() {
      const defects = this.draggingDefects
      if (!defects.length) return false
      return defects.every((d) =>
        isDefectActionVisible('pass', d, this.currentUserId)
      )
    },
    canDropCloseToClosed() {
      const defects = this.draggingDefects
      if (!defects.length) return false
      return defects.every((d) =>
        isDefectActionVisible('close', d, this.currentUserId)
      )
    },
  },
  beforeUnmount() {
    this.resetDragSession()
    this.teardownLaneResizeListeners()
    this.teardownBoardWidthObserver()
    this.teardownAssigneeSlotObservers()
  },
  methods: {
    init() {},
    setupBoardWidthObserver() {
      this.teardownBoardWidthObserver()
      const board = this.$refs.kanbanBoard
      if (!board || typeof ResizeObserver === 'undefined') {
        this.measureBoardWidth()
        return
      }
      this._boardWidthObserver = new ResizeObserver(() => {
        if (this.laneResizing || (this.draggingIds && this.draggingIds.length)) return
        this.measureBoardWidth()
        this.syncLaneAssigneeSlotWidths()
      })
      this._boardWidthObserver.observe(board)
      this.measureBoardWidth()
    },
    teardownBoardWidthObserver() {
      if (this._boardWidthObserver) {
        this._boardWidthObserver.disconnect()
        this._boardWidthObserver = null
      }
    },
    setupAssigneeSlotObservers() {
      this.teardownAssigneeSlotObservers()
      if (typeof ResizeObserver === 'undefined') {
        this.syncLaneAssigneeSlotWidths()
        return
      }
      this._assigneeSlotObservers = []
      ;[LANE_PROCESSING, LANE_AUDIT].forEach((key) => {
        const ref = this.$refs['laneAssigneesSlot-' + key]
        const el = Array.isArray(ref) ? ref[0] : ref
        if (!el) return
        const ro = new ResizeObserver(() => {
          /* 拖宽/拖卡片时勿写响应式，否则 Vue 重绘会卡死成员热区切换 */
          if (this.laneResizing || (this.draggingIds && this.draggingIds.length)) return
          this.syncLaneAssigneeSlotWidths()
        })
        ro.observe(el)
        this._assigneeSlotObservers.push(ro)
      })
      this.syncLaneAssigneeSlotWidths()
    },
    teardownAssigneeSlotObservers() {
      ;(this._assigneeSlotObservers || []).forEach((ro) => {
        try {
          ro.disconnect()
        } catch (e) {
          /* ignore */
        }
      })
      this._assigneeSlotObservers = []
    },
    measureBoardWidth() {
      /* 拖宽/拖卡片时勿写响应式宽度，避免整板重绘卡死换人热区 */
      if (this.laneResizing || (this.draggingIds && this.draggingIds.length)) return
      const board = this.$refs.kanbanBoard
      const w = board && board.clientWidth ? board.clientWidth : 0
      if (w > 0 && w !== this.boardWidthPx) this.boardWidthPx = w
    },
    syncLaneAssigneeSlotWidths() {
      if (this.laneResizing || (this.draggingIds && this.draggingIds.length)) return
      const next = { ...this.laneAssigneeSlotWidth }
      let changed = false
      ;[LANE_PROCESSING, LANE_AUDIT].forEach((key) => {
        const ref = this.$refs['laneAssigneesSlot-' + key]
        const el = Array.isArray(ref) ? ref[0] : ref
        /* 量「标题行剩余可排头像宽」，不要用量内容撑出来的窄宽（否则会锁死成 +N） */
        let w = el && el.clientWidth ? el.clientWidth : 0
        if (el && el.parentElement && w > 0) {
          const head = el.parentElement
          const spacer = head.querySelector('.defect-kanban-lane-head-spacer')
          if (spacer && spacer.clientWidth) w += spacer.clientWidth
        }
        if (w !== next[key]) {
          next[key] = w
          changed = true
        }
      })
      if (changed) this.laneAssigneeSlotWidth = next
    },
    estimateLaneWidthPx(laneKey) {
      const st = this._laneResize
      if (st && st.px && st.px[laneKey] != null) {
        return Number(st.px[laneKey]) || LANE_WIDTH_MIN_PX
      }
      const board = this.boardWidthPx || 0
      const resizerTotal = LANE_RESIZER_WIDTH_PX * Math.max(0, LANE_WIDTH_KEYS.length - 1)
      const usable = Math.max(LANE_WIDTH_MIN_PX, board - resizerTotal)
      const pct = Number(this.laneWidthPct && this.laneWidthPct[laneKey])
      const grow = Number.isFinite(pct) && pct > 0 ? pct : 25
      return (usable * grow) / 100
    },
    /** 按已量好的像素宽钉死各列（拖宽全程只走 DOM） */
    pinLaneWidthsFromPx(px) {
      const board = this.$refs.kanbanBoard
      if (!board || !px) return
      const els = board.querySelectorAll('.defect-kanban-lane')
      this.lanes.forEach((lane, i) => {
        this.applyLaneWidthInline(els[i], px[lane.key])
      })
    },
    /**
     * 按负责人槽实测宽度计算叠放可展示头像数（始终负间距叠加）：
     * 放得下 → 全显；放不下 → 少显并出 +N。按真实 29px 算，避免裁切。
     */
    laneAssigneeAvatarMax(laneKey) {
      const total = (this.visibleLaneAssigneeMembers(laneKey) || []).length
      if (!total) return 1
      let width = Number(this.laneAssigneeSlotWidth && this.laneAssigneeSlotWidth[laneKey]) || 0
      if (width < 8) {
        const laneW = this.estimateLaneWidthPx(laneKey)
        width = Math.max(28, laneW - 120)
      }
      const content = Math.max(0, width - 8)
      const size = LANE_ASSIGNEE_AVATAR_SIZE
      const step = size - LANE_ASSIGNEE_AVATAR_OVERLAP
      const stackAllNeed = size + Math.max(0, total - 1) * step
      if (stackAllNeed <= content) return total
      const usable = Math.max(0, content - LANE_ASSIGNEE_BADGE_PAD)
      let n = Math.floor((usable - size) / step) + 1
      if (!Number.isFinite(n) || n < 1) n = 1
      return Math.min(LANE_ASSIGNEE_AVATAR_MAX, n, Math.max(1, total - 1))
    },
    loadLaneWidths(projectId) {
      this.teardownLaneResizeListeners()
      this.clearLaneResizeInlineStyles()
      this.laneResizing = false
      this.laneWidthPct = readLaneWidthPctCache(projectId)
    },
    /** 双击分隔条恢复四列等宽 */
    resetLaneWidths() {
      this.teardownLaneResizeListeners()
      this.clearLaneResizeInlineStyles()
      this.laneResizing = false
      const pct = defaultLaneWidthPct()
      this.laneWidthPct = pct
      writeLaneWidthPctCache(this.projectId, pct)
    },
    applyLaneWidthInline(el, px) {
      if (!el) return
      const w = Math.max(LANE_WIDTH_MIN_PX, Math.round(px))
      el.style.flex = `0 0 ${w}px`
      el.style.width = `${w}px`
      el.style.minWidth = `${LANE_WIDTH_MIN_PX}px`
    },
    clearLaneResizeInlineStyles() {
      const board = this.$refs.kanbanBoard
      if (!board) return
      board.querySelectorAll('.defect-kanban-lane').forEach((el) => {
        el.style.flex = ''
        el.style.width = ''
        el.style.minWidth = ''
      })
    },
    onLaneResizeStart(leftIndex, e) {
      if (e.button != null && e.button !== 0) return
      if (this.laneResizing || this._laneResize) return
      const board = this.$refs.kanbanBoard
      if (!board || !this.lanes[leftIndex] || !this.lanes[leftIndex + 1]) return
      const laneEls = board.querySelectorAll('.defect-kanban-lane')
      if (!laneEls || laneEls.length < leftIndex + 2) return
      /* 必须在仍有 flex 占比时量宽；若先清 style 再量会量到内容塌缩宽 */
      const px = {}
      this.lanes.forEach((lane, i) => {
        const el = laneEls[i]
        px[lane.key] = el ? el.getBoundingClientRect().width : LANE_WIDTH_MIN_PX
      })
      const leftKey = this.lanes[leftIndex].key
      const rightKey = this.lanes[leftIndex + 1].key
      const leftStart = px[leftKey]
      const rightStart = px[rightKey]
      /* 先钉像素，再摘 Vue :style，避免中间塌缩；拖动中不写响应式宽度 */
      this.pinLaneWidthsFromPx(px)
      this._laneResize = {
        leftKey,
        rightKey,
        leftEl: laneEls[leftIndex],
        rightEl: laneEls[leftIndex + 1],
        startX: e.clientX,
        clientX: e.clientX,
        leftStart,
        rightStart,
        total: leftStart + rightStart,
        px: { ...px }
      }
      this.laneResizing = true
      document.body.classList.add('defect-kanban-lane-resizing')
      document.addEventListener('mousemove', this.onLaneResizeMove)
      document.addEventListener('mouseup', this.onLaneResizeEnd)
      this.$nextTick(() => {
        /* Vue 摘掉 :style 后可能冲掉 inline，按缓存像素再钉一次 */
        if (!this.laneResizing || !this._laneResize) return
        this.pinLaneWidthsFromPx(this._laneResize.px)
        const boardEl = this.$refs.kanbanBoard
        const els = boardEl && boardEl.querySelectorAll('.defect-kanban-lane')
        if (els && els.length >= leftIndex + 2) {
          this._laneResize.leftEl = els[leftIndex]
          this._laneResize.rightEl = els[leftIndex + 1]
        }
      })
    },
    onLaneResizeMove(e) {
      const st = this._laneResize
      if (!st) return
      st.clientX = e.clientX
      if (this._laneResizeRaf) return
      this._laneResizeRaf = requestAnimationFrame(() => {
        this._laneResizeRaf = 0
        const s = this._laneResize
        if (!s) return
        const dx = s.clientX - s.startX
        let left = s.leftStart + dx
        let right = s.rightStart - dx
        const total = s.total
        const min = LANE_WIDTH_MIN_PX
        if (left < min) {
          left = min
          right = Math.max(min, total - min)
        } else if (right < min) {
          right = min
          left = Math.max(min, total - min)
        }
        s.px[s.leftKey] = left
        s.px[s.rightKey] = right
        this.applyLaneWidthInline(s.leftEl, left)
        this.applyLaneWidthInline(s.rightEl, right)
      })
    },
    onLaneResizeEnd() {
      const st = this._laneResize
      if (this._laneResizeRaf) {
        cancelAnimationFrame(this._laneResizeRaf)
        this._laneResizeRaf = 0
      }
      let pct = null
      if (st && st.px) {
        let sum = 0
        for (const key of LANE_WIDTH_KEYS) {
          sum += Number(st.px[key]) || 0
        }
        if (sum > 0) {
          pct = {}
          for (const key of LANE_WIDTH_KEYS) {
            pct[key] = ((Number(st.px[key]) || 0) / sum) * 100
          }
        }
      }
      this._laneResize = null
      this.teardownLaneResizeListeners()
      if (pct) {
        this.laneWidthPct = pct
        writeLaneWidthPctCache(this.projectId, pct)
      }
      /* 先恢复占比 :style，再清 inline，避免中间塌缩 */
      this.laneResizing = false
      this.$nextTick(() => {
        this.clearLaneResizeInlineStyles()
        this.measureBoardWidth()
        this.syncLaneAssigneeSlotWidths()
        this.setupAssigneeSlotObservers()
      })
    },
    teardownLaneResizeListeners() {
      document.removeEventListener('mousemove', this.onLaneResizeMove)
      document.removeEventListener('mouseup', this.onLaneResizeEnd)
      document.body.classList.remove('defect-kanban-lane-resizing')
      if (this._laneResizeRaf) {
        cancelAnimationFrame(this._laneResizeRaf)
        this._laneResizeRaf = 0
      }
      this._laneResize = null
    },
    search(params) {
      const gen = ++this.requestGen
      this.loading = true
      this.loadingMore = false
      this.listPageNum = 1
      this.selectedIds = []
      this.lastSelect = null
      this.clearCanDragCache()
      this.laneVisibleLimit = defaultLaneVisibleLimit()
      const base =
        params && typeof params === 'object'
          ? params
          : this.query && typeof this.query === 'object'
            ? this.query
            : {}
      this.queryParams = base
      const nested =
        base.params && typeof base.params === 'object' ? { ...base.params } : {}
      listDefect({
        ...base,
        params: nested,
        pageNum: 1,
        pageSize: KANBAN_PAGE_SIZE
      })
        .then((response) => {
          if (gen !== this.requestGen) return
          this.defectList = Array.isArray(response.rows) ? response.rows : []
          this.total = Number(response.total) || 0
          this.listPageNum = 1
        })
        .catch(() => {
          if (gen !== this.requestGen) return
        })
        .finally(() => {
          if (gen === this.requestGen) this.loading = false
        })
    },
    tryLoadMore() {
      if (this.loading || this.loadingMore || !this.hasMoreDefects) {
        return Promise.resolve(false)
      }
      const gen = this.requestGen
      const nextPage = this.listPageNum + 1
      const base = this.queryParams || this.query || {}
      const nested =
        base.params && typeof base.params === 'object' ? { ...base.params } : {}
      this.loadingMore = true
      return listDefect({
        ...base,
        params: nested,
        pageNum: nextPage,
        pageSize: KANBAN_PAGE_SIZE
      })
        .then((response) => {
          if (gen !== this.requestGen) return false
          const rows = Array.isArray(response.rows) ? response.rows : []
          if (response.total != null && response.total !== '') {
            this.total = Number(response.total) || this.total
          }
          if (!rows.length) {
            this.total = this.defectList.length
            return false
          }
          const seen = new Set(
            this.defectList
              .map((d) => (d && d.defectId != null ? Number(d.defectId) : null))
              .filter((id) => id != null && Number.isFinite(id))
          )
          const append = rows.filter((d) => {
            const id = d && d.defectId != null ? Number(d.defectId) : null
            if (id == null || !Number.isFinite(id) || seen.has(id)) return false
            seen.add(id)
            return true
          })
          this.defectList = this.defectList.concat(append)
          this.listPageNum = nextPage
          return append.length > 0
        })
        .catch(() => false)
        .finally(() => {
          if (gen === this.requestGen) this.loadingMore = false
        })
    },
    resetLaneVisible(laneKey) {
      if (!laneKey) return
      this.laneVisibleLimit = {
        ...this.laneVisibleLimit,
        [laneKey]: LANE_VISIBLE_PAGE
      }
    },
    expandLaneVisible(laneKey) {
      const allLen = this.laneCards(laneKey).length
      const limit =
        (this.laneVisibleLimit && this.laneVisibleLimit[laneKey]) ||
        LANE_VISIBLE_PAGE
      if (limit < allLen) {
        this.laneVisibleLimit = {
          ...this.laneVisibleLimit,
          [laneKey]: Math.min(limit + LANE_VISIBLE_PAGE, allLen)
        }
        return Promise.resolve(true)
      }
      return this.tryLoadMore().then((added) => {
        if (!added) return false
        const nextLen = this.laneCards(laneKey).length
        if (nextLen <= limit) return false
        this.laneVisibleLimit = {
          ...this.laneVisibleLimit,
          [laneKey]: Math.min(limit + LANE_VISIBLE_PAGE, nextLen)
        }
        return true
      })
    },
    onLaneScroll(laneKey, e) {
      /* 拖卡片时插入投放条会扰动 scroll，禁止顺带扩页加重卡顿 */
      if (this.draggingIds && this.draggingIds.length) return
      /* 泳道上下滚时收起本列已展开的分享弹层，避免锚点错位悬空 */
      this.closeLaneSharePopovers(laneKey)
      const el = e && e.target
      if (!el) return
      const remain = el.scrollHeight - el.scrollTop - el.clientHeight
      if (remain <= LOAD_MORE_THRESHOLD_PX) this.expandLaneVisible(laneKey)
    },
    /** 从触发节点向上找到 ShareCard 实例 */
    findShareCardProxy(el) {
      let node = el
      while (node) {
        const inst = node.__vueParentComponent
        if (inst) {
          let p = inst
          while (p) {
            const proxy = p.proxy
            const name =
              (p.type && p.type.name) ||
              (proxy && proxy.$options && proxy.$options.name)
            if (name === 'ShareCard' && proxy && typeof proxy.hide === 'function') {
              return proxy
            }
            p = p.parent
          }
        }
        node = node.parentElement
      }
      return null
    },
    /** 关闭指定泳道内已打开的缺陷分享弹框 */
    closeLaneSharePopovers(laneKey) {
      if (typeof document === 'undefined') return
      /* 无打开的分享层时跳过，避免每次滚动扫全列 */
      if (!document.querySelector('.el-popper.share-card-popper')) return
      const board = this.$refs.kanbanBoard
      if (!board) return
      const lane = board.querySelector(`.defect-kanban-lane.lane-${laneKey}`)
      if (!lane) return
      lane.querySelectorAll('.share-card-trigger').forEach((trigger) => {
        const card = this.findShareCardProxy(trigger)
        if (card) card.hide()
      })
    },
    laneCards(laneKey) {
      const keyword = String((this.laneKeyword && this.laneKeyword[laneKey]) || '')
        .trim()
        .toLowerCase()
      /* 处理中/待验证：右上角成员选择作为本泳道执行人筛选 */
      const assigneeFilter =
        laneKey === LANE_PROCESSING || laneKey === LANE_AUDIT
          ? this.visibleLaneAssigneeIds(laneKey)
          : []
      const list = (this.defectList || []).filter((d) => {
        if (laneOf(d) !== laneKey) return false
        if (
          (laneKey === LANE_PROCESSING || laneKey === LANE_AUDIT) &&
          !defectMatchesLaneAssignees(d, assigneeFilter)
        ) {
          return false
        }
        if (!keyword) return true
        const num = d.projectNum != null ? String(d.projectNum) : ''
        const name = d.defectName != null ? String(d.defectName) : ''
        return (
          num.toLowerCase().includes(keyword) ||
          (`#${num}`).toLowerCase().includes(keyword) ||
          name.toLowerCase().includes(keyword)
        )
      })
      /* 已收藏优先置顶，同组保持原序 */
      return list
        .map((d, i) => ({ d, i }))
        .sort((a, b) => {
          const ac = this.isDefectCollected(a.d) ? 1 : 0
          const bc = this.isDefectCollected(b.d) ? 1 : 0
          if (ac !== bc) return bc - ac
          return a.i - b.i
        })
        .map((x) => x.d)
    },
    isDefectCollected(d) {
      if (!d) return false
      const c = d.collect
      return c === true || c === 1 || c === '1'
    },
    /** DefectTools 星标：v-model 已翻过，先回滚再 FLIP，run 内提交并置顶 */
    onCardCollect(collect, defect, run) {
      if (!defect || defect.defectId == null) return
      const next = !!collect
      /* StarSwitch 已写入新值，先还原以便量起点 */
      defect.collect = !next
      this.animateCollectReorder(defect, next, () => {
        if (typeof run === 'function') run()
        else defect.collect = next
      })
    },
    findLaneCardEl(defect) {
      const board = this.$refs.kanbanBoard
      if (!board || !defect) return { body: null, card: null }
      const laneKey = laneOf(defect)
      const lane =
        laneKey && board.querySelector(`.defect-kanban-lane.lane-${laneKey}`)
      const body = lane && lane.querySelector('.defect-kanban-lane-body')
      const card =
        body &&
        body.querySelector(
          `.defect-kanban-card[data-defect-id="${defect.defectId}"]`
        )
      return { body, card }
    },
    animateCollectReorder(defect, goingUp, applyOrder) {
      const id = Number(defect.defectId)
      const { body, card: beforeCard } = this.findLaneCardEl(defect)
      if (!body || !beforeCard || typeof beforeCard.animate !== 'function') {
        applyOrder()
        return
      }
      const first = beforeCard.getBoundingClientRect()
      applyOrder()
      this.$nextTick(() => {
        const { body: body2, card } = this.findLaneCardEl(defect)
        if (!body2 || !card) return
        /* 收藏置顶：滚到顶部再量终点，飞卡路径从原位拉到最上方 */
        if (goingUp) body2.scrollTop = 0
        const last = card.getBoundingClientRect()
        const dx = first.left - last.left
        const dy = first.top - last.top
        const dist = Math.hypot(dx, dy)
        this.collectFlyingId = id
        const clearFlying = () => {
          if (this.collectFlyingId === id) this.collectFlyingId = null
        }
        if (dist < 16) {
          const bounce = card.animate(
            [
              { transform: 'translateY(0) scale(1)', offset: 0 },
              { transform: 'translateY(-14px) scale(1.08)', offset: 0.35 },
              { transform: 'translateY(0) scale(1)', offset: 1 }
            ],
            { duration: 560, easing: 'cubic-bezier(0.22, 1.45, 0.36, 1)' }
          )
          bounce.onfinish = clearFlying
          bounce.oncancel = clearFlying
          return
        }
        const fly = card.animate(
          [
            {
              transform: `translate(${dx}px, ${dy}px) scale(1.05)`,
              boxShadow:
                '0 18px 40px rgba(0,0,0,0.28), 0 0 0 2px rgba(247,186,42,0.9)',
              offset: 0
            },
            {
              transform: `translate(${dx * 0.2}px, ${dy * 0.2}px) scale(1.09)`,
              boxShadow:
                '0 22px 48px rgba(0,0,0,0.3), 0 0 0 3px rgba(247,186,42,0.95)',
              offset: 0.5
            },
            {
              transform: 'translate(0, 0) scale(1)',
              boxShadow:
                '0 1px 2px rgba(0,0,0,0.06), 0 0 0 1px rgba(247,186,42,0.35)',
              offset: 1
            }
          ],
          { duration: 780, easing: 'cubic-bezier(0.16, 0.84, 0.22, 1)' }
        )
        fly.onfinish = clearFlying
        fly.oncancel = clearFlying
      })
    },
    visibleLaneCards(laneKey) {
      const limit =
        (this.laneVisibleLimit && this.laneVisibleLimit[laneKey]) ||
        LANE_VISIBLE_PAGE
      return this.laneCards(laneKey).slice(0, limit)
    },
    isSelected(defectId) {
      return this.selectedIds.some((id) => Number(id) === Number(defectId))
    },
    isDraggingCard(defectId) {
      return this.draggingIdSet.has(Number(defectId))
    },
    /** 可拖：泳道流转 或 有删除权可扔进垃圾桶（处理中非本人亦可拖，但不能进待验证） */
    canDragCard(d) {
      if (!d) return false
      const id = Number(d.defectId)
      if (!this._canDragCache) this._canDragCache = new Map()
      if (this._canDragCache.has(id)) return this._canDragCache.get(id)
      const ok =
        canKanbanTrashDrag(d, this.currentUserId) ||
        canKanbanDragCard(d, this.currentUserId)
      this._canDragCache.set(id, ok)
      return ok
    },
    clearCanDragCache() {
      this._canDragCache = null
    },
    laneShowsAssignees(laneKey) {
      return laneKey === LANE_PROCESSING || laneKey === LANE_AUDIT
    },
    onUnassignedAddClick() {
      if (!this.canAddDefect) return
      this.$emit('add')
    },
    onCardToolsDone() {
      this.search(this.queryParams)
      this.$emit('refresh')
    },
    cardStateClass(d) {
      const s = d && d.defectStateName
      if (s === 'CREATED') return 'created'
      if (s === 'PROCESSING') return 'processing'
      if (s === 'REJECTED') return 'rejected'
      if (s === 'AUDIT') return 'audit'
      if (s === 'RESOLVED') return 'resolved'
      if (s === 'CLOSED') return 'closed'
      return 'unknown'
    },
    async ensureImageFields(projectId) {
      const pid = Number(projectId)
      if (!pid) {
        this.imageFields = []
        this._imageFieldsProjectId = null
        return
      }
      if (this._imageFieldsProjectId === pid) return
      const fields = await loadEnabledImageFields(pid)
      this.imageFields = Array.isArray(fields) ? fields : []
      this._imageFieldsProjectId = pid
    },
    /** 优先级字典 value（如 middle）走 i18n，避免直接露出英文 key */
    defectLevelText(level) {
      if (level == null || level === '') return ''
      const key = String(level)
      const text = this.$t(key)
      return text && text !== key ? text : key
    },
    async ensureFieldLayout(projectId) {
      const pid = Number(projectId)
      if (!pid) {
        this.enabledBuiltinFieldKeys = []
        this._fieldLayoutProjectId = null
        return
      }
      if (this._fieldLayoutProjectId === pid) return
      try {
        const layout = await loadDefectColumnLayout(pid, { force: true })
        // 等布局返回后再标记已加载；keys 非数组时按未启用可选字段处理，避免误显优先级等
        const keys = layout && layout.enabledBuiltinFieldKeys
        this.enabledBuiltinFieldKeys = Array.isArray(keys) ? keys : []
        this._fieldLayoutProjectId = pid
      } catch (e) {
        this.enabledBuiltinFieldKeys = []
        /* 失败不锁死 projectId，便于下次进入再拉 */
        this._fieldLayoutProjectId = null
      }
    },
    async ensureLaneAssignees(projectId) {
      const pid = Number(projectId)
      if (!pid) {
        this.laneAssigneeIds = { [LANE_PROCESSING]: [], [LANE_AUDIT]: [] }
        this.memberById = new Map()
        this._assigneesProjectId = null
        return
      }
      if (this._assigneesProjectId === pid && this.memberById.size) return
      try {
        const [projectRes, memberRes, roleRes] = await Promise.all([
          getProject(pid).catch(() => null),
          listMemberOfProject(pid, { pageNum: 1, pageSize: 2000 }),
          listProjectRole(pid).catch(() => null)
        ])
        const project = (projectRes && projectRes.data) || projectRes || null
        const rows = (memberRes && memberRes.rows) || []
        /* getProject 详情常不带 members，头像必须靠成员列表 */
        this.memberById = buildMemberMapFromList(rows)
        let hasCache = false
        try {
          hasCache = localStorage.getItem(laneAssigneesStorageKey(pid)) != null
        } catch (e) {
          hasCache = false
        }
        if (hasCache) {
          const resolved = resolveLaneAssigneeIds(
            projectContextFromMembers(rows, project),
            pid
          )
          this.laneAssigneeIds = {
            [LANE_PROCESSING]: resolved.processing.slice(),
            [LANE_AUDIT]: resolved.audit.slice()
          }
        } else {
          const roleList = Array.isArray(roleRes)
            ? roleRes
            : (roleRes && roleRes.data) || []
          const findRoleId = (keys) => {
            const hit = roleList.find(
              (r) => r && keys.includes(String(r.roleKey || ''))
            )
            return hit && hit.roleId != null ? hit.roleId : null
          }
          const developRoleId = findRoleId(ROLE_DEVELOP_KEYS)
          const testerRoleId = findRoleId(ROLE_TESTER_KEYS)
          const [devRes, testRes] = await Promise.all([
            developRoleId
              ? listMemberOfProject(pid, {
                  pageNum: 1,
                  pageSize: 2000,
                  roleId: developRoleId
                })
              : Promise.resolve({ rows: [] }),
            testerRoleId
              ? listMemberOfProject(pid, {
                  pageNum: 1,
                  pageSize: 2000,
                  roleId: testerRoleId
                })
              : Promise.resolve({ rows: [] })
          ])
          let processing = normalizeIdList(
            ((devRes && devRes.rows) || []).map((m) => m.userId)
          )
          let audit = normalizeIdList(
            ((testRes && testRes.rows) || []).map((m) => m.userId)
          )
          const ctx = projectContextFromMembers(rows, project)
          if (!processing.length) {
            processing = defaultAssigneeIdsForLane(ctx, LANE_PROCESSING)
          }
          if (!audit.length) {
            audit = defaultAssigneeIdsForLane(ctx, LANE_AUDIT)
          }
          writeLaneAssigneesCache(pid, { processing, audit })
          this.laneAssigneeIds = { [LANE_PROCESSING]: processing, [LANE_AUDIT]: audit }
        }
        this._assigneesProjectId = pid
      } catch (e) {
        this.memberById = new Map()
        this.laneAssigneeIds = { [LANE_PROCESSING]: [], [LANE_AUDIT]: [] }
        this._assigneesProjectId = pid
      }
    },
    visibleLaneAssigneeIds(laneKey) {
      if (laneKey !== LANE_PROCESSING && laneKey !== LANE_AUDIT) return []
      const ids = (this.laneAssigneeIds && this.laneAssigneeIds[laneKey]) || []
      return filterIdsByHandleBy(ids, this.handleBySet)
    },
    visibleLaneAssigneeMembers(laneKey) {
      return this.visibleLaneAssigneeIds(laneKey).map((id) => {
        const hit = this.memberById && this.memberById.get(Number(id))
        if (hit) return hit
        return { userId: id, nickName: String(id) }
      })
    },
    onLaneAssigneesChange(laneKey, ids) {
      if (laneKey !== LANE_PROCESSING && laneKey !== LANE_AUDIT) return
      let next = normalizeIdList(ids)
      if (this.handleBySet) {
        next = filterIdsByHandleBy(next, this.handleBySet)
      }
      this.laneAssigneeIds = {
        ...this.laneAssigneeIds,
        [laneKey]: next
      }
      writeLaneAssigneesCache(this.projectId, {
        processing: this.laneAssigneeIds[LANE_PROCESSING],
        audit: this.laneAssigneeIds[LANE_AUDIT]
      })
      this.resetLaneVisible(laneKey)
    },
    closeLaneAssigneePickers() {
      ;[LANE_PROCESSING, LANE_AUDIT].forEach((key) => {
        const ref = this.$refs['laneAssigneePicker-' + key]
        const comp = Array.isArray(ref) ? ref[0] : ref
        if (comp && comp.popoverVisible) comp.popoverVisible = false
      })
    },
    openLaneAssigneePicker(laneKey, event) {
      const btn = event && event.currentTarget
      if (btn && typeof btn.getBoundingClientRect === 'function') {
        const rect = btn.getBoundingClientRect()
        this.laneAssigneePickerStyle = {
          ...this.laneAssigneePickerStyle,
          [laneKey]: {
            position: 'fixed',
            left: `${Math.round(rect.left)}px`,
            top: `${Math.round(rect.top)}px`,
            width: `${Math.max(Math.round(rect.width), 28)}px`,
            height: `${Math.max(Math.round(rect.height), 28)}px`,
            inset: 'auto'
          }
        }
      }
      this.$nextTick(() => {
        const ref = this.$refs['laneAssigneePicker-' + laneKey]
        const comp = Array.isArray(ref) ? ref[0] : ref
        if (comp) comp.popoverVisible = true
      })
    },
    /** 泳道负责人是否仅当前用户一人 */
    isLaneAssigneesOnlyMe(laneKey) {
      const uid = this.currentUserId
      if (uid == null || !Number.isFinite(uid)) return false
      const ids = (this.laneAssigneeIds && this.laneAssigneeIds[laneKey]) || []
      if (!Array.isArray(ids) || ids.length !== 1) return false
      return Number(ids[0]) === uid
    },
    showAssigneeDropBar(laneKey) {
      if (!this.draggingIds.length) return false
      if (laneKey === LANE_AUDIT && this.dragFromLane === LANE_PROCESSING) return true
      if (laneKey === LANE_PROCESSING && this.dragFromLane === LANE_CREATED) return true
      return false
    },
    showClosedStateDropBar(laneKey) {
      if (laneKey !== LANE_CLOSED) return false
      if (!this.draggingIds.length || this.dragFromLane !== LANE_AUDIT) return false
      return this.canDropPassToClosed || this.canDropCloseToClosed
    },
    assigneeDropHint(laneKey) {
      if (laneKey === LANE_PROCESSING) {
        return this.$t('defect.kanban.assign-drop-hint')
      }
      return this.$t('defect.kanban.repair-drop-hint')
    },
    onAssigneeDropBarDragOver(laneKey, event) {
      const allowed = this.isLaneDropAllowed(laneKey)
      this.setDragDropCursor(allowed, event)
      this.setDragOverLaneDom(allowed ? laneKey : null)
      this.setOverDropChooser(allowed)
      const t = event && event.target
      if (
        t &&
        typeof t.closest === 'function' &&
        !t.closest('.defect-kanban-assignee-drop-target')
      ) {
        this.setDropHoverUserIdDom(null)
      }
    },
    onAssigneeDropBarDrop(laneKey) {
      /* 落在提示条空白处：视为未命中头像，走弹框路径（与未命中头像一致） */
      this.onLaneDrop(laneKey)
    },
    onClosedStateDropBarDragOver(event) {
      const allowed = this.isLaneDropAllowed(LANE_CLOSED)
      this.setDragDropCursor(allowed, event)
      this.setDragOverLaneDom(allowed ? LANE_CLOSED : null)
      this.setOverDropChooser(allowed)
      const t = event && event.target
      if (
        t &&
        typeof t.closest === 'function' &&
        !t.closest('.defect-kanban-state-drop-target')
      ) {
        this.setDropHoverActionDom(null)
      }
    },
    onClosedStateDropBarDrop() {
      /* 未命中状态图标：有通过权则默认通过，否则关闭 */
      this.onLaneDrop(
        LANE_CLOSED,
        this.canDropPassToClosed ? 'pass' : 'close'
      )
    },
    onClosedStateDragOver(action, event) {
      const ok =
        action === 'pass' ? this.canDropPassToClosed : this.canDropCloseToClosed
      this.setDragDropCursor(ok, event)
      this.setDropHoverActionDom(ok ? action : null)
      this.setDragOverLaneDom(ok ? LANE_CLOSED : null)
      this.setOverDropChooser(ok)
    },
    onClosedStateDragLeave(action, event) {
      const el = event && event.currentTarget
      const related = event && event.relatedTarget
      if (el && related && typeof el.contains === 'function' && el.contains(related)) {
        return
      }
      if (this._dropHoverAction === action) this.setDropHoverActionDom(null)
    },
    onClosedStateDrop(action) {
      this.setDropHoverActionDom(null)
      this.onLaneDrop(LANE_CLOSED, action)
    },
    onAssigneeDragOver(laneKey, userId, event) {
      const allowed = this.isLaneDropAllowed(laneKey)
      this.setDragDropCursor(allowed, event)
      this.setDropHoverUserIdDom(
        allowed ? Number(userId) : null,
        event && event.currentTarget
      )
      this.setDragOverLaneDom(allowed ? laneKey : null)
      this.setOverDropChooser(allowed)
    },
    onAssigneeDragLeave(userId, event) {
      const el = event && event.currentTarget
      const related = event && event.relatedTarget
      if (el && related && typeof el.contains === 'function' && el.contains(related)) {
        return
      }
      if (this._dropHoverUserId === Number(userId)) this.setDropHoverUserIdDom(null)
    },
    async onAssigneeDrop(laneKey, userId) {
      const uid = Number(userId)
      /* 只快照，状态留给 dragend/resetDragSession，避免投放时拆装打断 DnD */
      const fromLane = this.dragFromLane
      const ids = this.draggingIds.slice()
      this.clearDragHoverDom()
      if (!ids.length || !Number.isFinite(uid)) return

      if (laneKey === LANE_AUDIT && fromLane === LANE_PROCESSING) {
        await this.runDirectRepair(ids, uid)
        return
      }
      if (laneKey === LANE_PROCESSING && fromLane === LANE_CREATED) {
        await this.runDirectAssign(ids, uid)
      }
    },
    async runDirectRepair(ids, uid) {
      if (this.repairingDirect) return
      const defects = this.defectList.filter((d) =>
        ids.includes(Number(d.defectId))
      )
      if (!defects.length) return
      const allOk = defects.every((d) =>
        isDefectActionVisible('repair', d, this.currentUserId)
      )
      if (!allOk) {
        this.$modal.msgWarning(this.$t('defect.kanban.repair-need-handler'))
        return
      }
      this.repairingDirect = true
      let done = 0
      try {
        for (const d of defects) {
          await repair(Number(d.defectId), { receiveBy: [uid] })
          done += 1
        }
        if (done) {
          this.$modal.msgSuccess(
            this.$t('defect.kanban.repair-direct-ok', [done])
          )
        }
        this.search(this.queryParams)
        this.$emit('refresh')
      } catch (e) {
        this.$modal.msgError(
          this.$t('defect.kanban.repair-direct-fail', [done, defects.length])
        )
        this.search(this.queryParams)
        this.$emit('refresh')
      } finally {
        this.repairingDirect = false
      }
    },
    async runDirectAssign(ids, uid) {
      if (this.assigningDirect) return
      const defects = this.defectList.filter((d) =>
        ids.includes(Number(d.defectId))
      )
      if (!defects.length) return
      const allOk = defects.every((d) =>
        isDefectActionVisible('assign', d, this.currentUserId)
      )
      if (!allOk) {
        this.$modal.msgWarning(this.$t('defect.kanban.drop-denied'))
        return
      }
      this.assigningDirect = true
      let done = 0
      try {
        for (const d of defects) {
          await assign(Number(d.defectId), { receiveBy: [uid] })
          done += 1
        }
        if (done) {
          this.$modal.msgSuccess(
            this.$t('defect.kanban.assign-direct-ok', [done])
          )
        }
        this.search(this.queryParams)
        this.$emit('refresh')
      } catch (e) {
        this.$modal.msgError(
          this.$t('defect.kanban.assign-direct-fail', [done, defects.length])
        )
        this.search(this.queryParams)
        this.$emit('refresh')
      } finally {
        this.assigningDirect = false
      }
    },
    cardImages(d) {
      if (!d) return []
      const fields = this.imageFields || []
      const parts = []
      if (fields.length) {
        fields.forEach((f) => {
          const key = f && f.fieldKey
          if (!key) return
          parts.push(...candidateUrlsForImageField(d, key))
        })
      } else if (d.imgUrls) {
        parts.push(...resolveCommaSeparatedUrls(d.imgUrls))
      }
      const cacheKey = parts.join('|')
      const hit = KANBAN_IMG_CACHE.get(d)
      if (hit && hit.key === cacheKey) return hit.urls
      const seen = new Set()
      const urls = []
      parts.forEach((url) => {
        if (!url || seen.has(url)) return
        seen.add(url)
        urls.push(url)
      })
      KANBAN_IMG_CACHE.set(d, { key: cacheKey, urls })
      return urls
    },
    openDefect(d) {
      this.$emit('defect-click', d)
    },
    onCardClick(d, laneKey, idx, event) {
      const id = Number(d.defectId)
      const meta = event.metaKey || event.ctrlKey
      const shift = event.shiftKey
      if (shift && this.lastSelect && this.lastSelect.laneKey === laneKey) {
        const cards = this.visibleLaneCards(laneKey)
        const from = Math.min(this.lastSelect.idx, idx)
        const to = Math.max(this.lastSelect.idx, idx)
        const rangeIds = cards.slice(from, to + 1).map((c) => Number(c.defectId))
        if (meta) {
          const set = new Set(this.selectedIds.map(Number))
          rangeIds.forEach((rid) => set.add(rid))
          this.selectedIds = [...set]
        } else {
          this.selectedIds = rangeIds
        }
      } else if (meta) {
        if (this.isSelected(id)) {
          this.selectedIds = this.selectedIds.filter((x) => Number(x) !== id)
        } else {
          this.selectedIds = this.selectedIds.concat([id])
        }
        this.lastSelect = { laneKey, idx, id }
      } else {
        this.selectedIds = [id]
        this.lastSelect = { laneKey, idx, id }
      }
    },
    /** 当前拖拽是否允许落到目标泳道（用于 dropEffect / 禁止光标；同列不算禁止） */
    isLaneDropAllowed(toLane) {
      if (!toLane || !this.dragFromLane) return false
      if (toLane === this.dragFromLane) return true
      const allowed = this.dragAllowedLanes
      if (allowed) return allowed.has(toLane)
      return this.computeLaneDropAllowed(toLane)
    },
    computeLaneDropAllowed(toLane) {
      const fromLane = this.dragFromLane
      if (!fromLane || !toLane) return false
      if (fromLane === toLane) return true
      if (!this.draggingIds.length) return false
      const idSet = new Set(this.draggingIds.map(Number))
      const defects = this.defectList.filter((d) => idSet.has(Number(d.defectId)))
      if (!defects.length) return false
      const action = resolveKanbanDropAction(
        fromLane,
        toLane,
        defects,
        this.currentUserId
      )
      if (!action) return false
      return defects.every((d) =>
        isDefectActionVisible(action, d, this.currentUserId)
      )
    },
    rebuildDragAllowedLanes() {
      const fromLane = this.dragFromLane
      if (!fromLane || !this.draggingIds.length) {
        this.dragAllowedLanes = null
        this.dragForbiddenLanes = null
        return
      }
      const set = new Set([fromLane])
      ;[LANE_CREATED, LANE_PROCESSING, LANE_AUDIT, LANE_CLOSED].forEach((lane) => {
        if (lane !== fromLane && this.computeLaneDropAllowed(lane)) set.add(lane)
      })
      const canTrash = this.canDropDraggingToTrash
      const forbidden = new Set()
      ;[LANE_CREATED, LANE_PROCESSING, LANE_AUDIT, LANE_CLOSED].forEach((lane) => {
        if (lane === fromLane) return
        if (lane === LANE_CLOSED && canTrash) return
        if (!set.has(lane)) forbidden.add(lane)
      })
      this.dragAllowedLanes = set
      this.dragForbiddenLanes = forbidden
    },
    /** 拖起后：非法目标泳道整列置灰（源泳道与合法目标除外；可删时已结束仍可显垃圾桶） */
    isLaneDropForbiddenDuringDrag(laneKey) {
      return !!(this.dragForbiddenLanes && this.dragForbiddenLanes.has(laneKey))
    },
    onTrashDragOver(event) {
      const ok = this.canDropDraggingToTrash
      this.setDragDropCursor(ok, event)
      this.setDropHoverTrashDom(ok)
      if (ok) this.setDragOverLaneDom(LANE_CLOSED)
      this.setOverDropChooser(ok)
    },
    onTrashDragLeave(event) {
      const el = event && event.currentTarget
      const related = event && event.relatedTarget
      if (el && related && typeof el.contains === 'function' && el.contains(related)) {
        return
      }
      this.setDropHoverTrashDom(false)
      this.setOverDropChooser(false)
    },
    onTrashDrop() {
      const ok = this.canDropDraggingToTrash
      const ids = this.draggingIds.slice()
      this.clearDragHoverDom()
      this.setOverDropChooser(false)
      this.clearDragDropCursor()
      if (!ok || !ids.length) {
        if (!ok) this.$modal.msgWarning(this.$t('defect.kanban.delete-denied'))
        return
      }
      const idSet = new Set(ids.map(Number))
      const defects = this.defectList.filter((d) => idSet.has(Number(d.defectId)))
      this.runDirectDelete(defects)
    },
    async runDirectDelete(defects) {
      if (this.deletingDirect) return
      const list = Array.isArray(defects) ? defects.filter(Boolean) : []
      if (!list.length) return
      const allOk = list.every((d) => canKanbanTrashDrag(d, this.currentUserId))
      if (!allOk) {
        this.$modal.msgWarning(this.$t('defect.kanban.delete-denied'))
        return
      }
      try {
        await this.$modal.confirm(
          strFormat(this.$t('defect.is-batch-delete'), list.length),
          this.$t('prompted').toString(),
          {
            confirmButtonText: this.$t('delete').toString(),
            cancelButtonText: this.$t('cancel').toString(),
            confirmButtonClass: 'delete-button',
            type: 'warning'
          }
        )
      } catch (e) {
        return
      }
      this.deletingDirect = true
      let ok = 0
      try {
        for (const d of list) {
          await delDefect(d.defectId)
          ok += 1
        }
        this.selectedIds = []
        if (ok) {
          this.$modal.msgSuccess(strFormat(this.$t('defect.batch-result-all-ok'), ok))
        }
        this.search(this.queryParams)
        this.$emit('refresh')
      } catch (e) {
        this.$modal.msgWarning(
          strFormat(this.$t('defect.batch-result-partial'), ok, list.length - ok)
        )
        this.search(this.queryParams)
        this.$emit('refresh')
      } finally {
        this.deletingDirect = false
      }
    },
    setDragDropCursor(allowed, event) {
      if (event && event.dataTransfer) {
        event.dataTransfer.dropEffect = allowed ? 'move' : 'none'
      }
      if (typeof document === 'undefined') return
      const forbid = !allowed
      if (this._dropForbiddenBody === forbid) return
      this._dropForbiddenBody = forbid
      document.body.classList.toggle('defect-kanban-drop-forbidden', forbid)
    },
    clearDragDropCursor() {
      if (typeof document === 'undefined') return
      this._dropForbiddenBody = false
      document.body.classList.remove('defect-kanban-drop-forbidden')
    },
    onCardDragStart(d, laneKey, event) {
      this.closeLaneAssigneePickers()
      /* 上次若 dragend 丢失，先清残留再开拖，避免第二次拖不动 */
      if (
        this._dragGhostEl ||
        this._docDragGhostBound ||
        this._dragSessionArmed ||
        (this.draggingIds && this.draggingIds.length)
      ) {
        this.resetDragSession()
      }
      if (!this.canDragCard(d)) {
        event.preventDefault()
        if (event.dataTransfer) {
          event.dataTransfer.effectAllowed = 'none'
        }
        this.setDragDropCursor(false, event)
        return
      }
      const id = Number(d.defectId)
      let ids = this.isSelected(id)
        ? this.selectedIds.map(Number)
        : [id]
      /* 多选必须同泳道；仅带走当前可拖的卡片 */
      let defects = this.defectList.filter((x) =>
        ids.includes(Number(x.defectId))
      )
      const lanes = new Set(defects.map((x) => laneOf(x)).filter(Boolean))
      if (lanes.size !== 1) {
        event.preventDefault()
        this.$modal.msgWarning(this.$t('defect.kanban.mixed-lane-drag'))
        this.setDragDropCursor(false, event)
        return
      }
      defects = defects.filter((x) => this.canDragCard(x))
      if (!defects.length) {
        event.preventDefault()
        if (event.dataTransfer) {
          event.dataTransfer.effectAllowed = 'none'
        }
        this.setDragDropCursor(false, event)
        return
      }
      /* 先写 dataTransfer / 拖影，再改响应式，减少 dragstart 中途重绘打断 */
      const fromLane = [...lanes][0] || laneKey
      if (event.dataTransfer) {
        event.dataTransfer.effectAllowed = 'move'
        event.dataTransfer.setData('text/plain', String(id))
        this.beginFollowDragGhost(event, defects)
      }
      this.dragFromLane = fromLane
      this.draggingIds = defects.map((x) => Number(x.defectId))
      this.rebuildDragAllowedLanes()
      this._overDropChooser = false
      if (!this.isSelected(id)) {
        this.selectedIds = [id]
      }
      this.armDragSessionCleanup()
      this.clearDragDropCursor()
    },
    beginFollowDragGhost(event, defects) {
      if (!event || !event.dataTransfer || typeof document === 'undefined') return
      this.clearDragGhostEl()
      /* 隐藏浏览器默认拖影，改用可缩放的卡片克隆跟随层 */
      const blank = document.createElement('div')
      blank.style.cssText = 'position:fixed;top:-9999px;left:-9999px;width:1px;height:1px;opacity:0;'
      document.body.appendChild(blank)
      this._dragBlankEl = blank
      try {
        event.dataTransfer.setDragImage(blank, 0, 0)
      } catch (e) {
        /* ignore */
      }
      const cardEl = event.currentTarget
      const rect = cardEl && cardEl.getBoundingClientRect
        ? cardEl.getBoundingClientRect()
        : null
      const wrap = document.createElement('div')
      wrap.className = 'defect-kanban-drag-ghost-wrap'
      const stack = document.createElement('div')
      stack.className = 'defect-kanban-drag-ghost-stack'
      const n = defects && defects.length > 1 ? defects.length : 0
      const width = rect && rect.width ? Math.round(rect.width) : 260
      const height = rect && rect.height ? Math.round(rect.height) : 88
      /* 多选：背后叠 1～2 层假卡，形成叠放 */
      const stackDepth = n > 1 ? Math.min(n - 1, 2) : 0
      for (let i = stackDepth; i >= 1; i -= 1) {
        const back = document.createElement('div')
        back.className = `defect-kanban-drag-ghost-layer is-back is-depth-${i}`
        back.style.width = `${width}px`
        back.style.height = `${height}px`
        stack.appendChild(back)
      }
      const ghost = cardEl && cardEl.cloneNode
        ? cardEl.cloneNode(true)
        : document.createElement('div')
      ghost.classList.add('defect-kanban-drag-ghost', 'defect-kanban-drag-ghost-layer', 'is-front')
      ghost.classList.remove('is-dragging', 'is-selected', 'is-drag-locked')
      ghost.removeAttribute('draggable')
      ghost.removeAttribute('title')
      /* 卡片本身 overflow:hidden，角标放在 stack 上避免被裁切 */
      ghost.style.overflow = 'visible'
      /* 去掉克隆里的交互控件与预览图，减轻拖影绘制 */
      ghost.querySelectorAll(
        'button, .el-dropdown, .defect-kanban-card-media, img, video, canvas'
      ).forEach((el) => {
        if (el && el.parentNode) el.parentNode.removeChild(el)
      })
      ghost.style.width = `${width}px`
      stack.appendChild(ghost)
      if (n > 1) {
        stack.classList.add('is-multi')
        const badge = document.createElement('span')
        badge.className = 'defect-kanban-drag-ghost-badge'
        badge.textContent = String(n)
        stack.appendChild(badge)
      }
      wrap.appendChild(stack)
      document.body.appendChild(wrap)
      this._dragGhostEl = wrap
      this._dragGhostCard = stack
      this._dragGhostSize = { w: width, h: height }
      const x = event.clientX != null ? event.clientX : 0
      const y = event.clientY != null ? event.clientY : 0
      this._dragGhostPtr = { x, y }
      this._overDropChooser = false
      this.moveFollowDragGhost(x, y)
      this.setupDocDragGhostListener()
    },
    setupDocDragGhostListener() {
      if (typeof document === 'undefined' || this._docDragGhostBound) return
      this._docDragGhostBound = true
      document.addEventListener('dragover', this.onDocDragOverForGhost, true)
    },
    teardownDocDragGhostListener() {
      if (typeof document === 'undefined' || !this._docDragGhostBound) return
      this._docDragGhostBound = false
      document.removeEventListener('dragover', this.onDocDragOverForGhost, true)
    },
    onDocDragOverForGhost(event) {
      if (!this._dragGhostEl || !event) return
      this.moveFollowDragGhost(event.clientX, event.clientY)
    },
    moveFollowDragGhost(clientX, clientY) {
      const x = clientX != null ? clientX : (this._dragGhostPtr && this._dragGhostPtr.x) || 0
      const y = clientY != null ? clientY : (this._dragGhostPtr && this._dragGhostPtr.y) || 0
      this._dragGhostPtr = { x, y }
      if (this._ghostMoveRaf) return
      this._ghostMoveRaf = requestAnimationFrame(() => {
        this._ghostMoveRaf = 0
        const wrap = this._dragGhostEl
        const ptr = this._dragGhostPtr
        if (!wrap || !ptr) return
        const compact = !!this._overDropChooser
        /* 锚点：普通跟卡片左上；缩小时更贴指针，少挡热区 */
        const ox = compact ? 18 : 22
        const oy = compact ? 14 : 18
        wrap.style.transform = `translate3d(${ptr.x - ox}px, ${ptr.y - oy}px, 0)`
      })
    },
    setOverDropChooser(on) {
      const next = !!on
      /* 非响应式：只改拖影，避免 Vue 重绘整板导致卡顿 */
      if (this._overDropChooser === next) return
      this._overDropChooser = next
      const stack = this._dragGhostCard
      if (stack) stack.classList.toggle('is-compact', next)
      const ptr = this._dragGhostPtr
      if (ptr) this.moveFollowDragGhost(ptr.x, ptr.y)
    },
    /** 高亮目标泳道：改 DOM class，避免 dragover 触发整板 Vue 重绘 */
    setDragOverLaneDom(laneKey) {
      const next = laneKey || null
      if (this._dragOverLane === next) return
      const board = this.$refs.kanbanBoard
      if (board && this._dragOverLane) {
        const prev = board.querySelector(
          `.defect-kanban-lane.lane-${this._dragOverLane}`
        )
        if (prev) prev.classList.remove('is-drag-over')
      }
      if (board && next) {
        const el = board.querySelector(`.defect-kanban-lane.lane-${next}`)
        if (el) el.classList.add('is-drag-over')
      }
      this._dragOverLane = next
    },
    setDropHoverUserIdDom(userId, targetEl) {
      const next =
        userId != null && Number.isFinite(Number(userId)) ? Number(userId) : null
      if (this._dropHoverUserId === next) return
      if (this._dropHoverUserEl) {
        this._dropHoverUserEl.classList.remove('is-hot')
        this._dropHoverUserEl = null
      } else {
        const board = this.$refs.kanbanBoard
        if (board && this._dropHoverUserId != null) {
          const prev = board.querySelector(
            `.defect-kanban-assignee-drop-target[data-user-id="${this._dropHoverUserId}"]`
          )
          if (prev) prev.classList.remove('is-hot')
        }
      }
      if (next != null) {
        let el = targetEl
        if (!el || !el.classList) {
          const board = this.$refs.kanbanBoard
          el =
            board &&
            board.querySelector(
              `.defect-kanban-assignee-drop-target[data-user-id="${next}"]`
            )
        }
        if (el) {
          el.classList.add('is-hot')
          this._dropHoverUserEl = el
        }
      }
      this._dropHoverUserId = next
    },
    setDropHoverActionDom(action) {
      const next = action === 'pass' || action === 'close' ? action : null
      if (this._dropHoverAction === next) return
      const board = this.$refs.kanbanBoard
      if (board) {
        board.querySelectorAll('.defect-kanban-state-drop-target').forEach((el) => {
          el.classList.remove('is-hot')
          if (el.getAttribute('data-drop-action') === 'pass') {
            el.classList.toggle('is-default', next == null)
          }
        })
        if (next) {
          const el = board.querySelector(
            `.defect-kanban-state-drop-target[data-drop-action="${next}"]`
          )
          if (el) {
            el.classList.add('is-hot')
            el.classList.remove('is-default')
          }
        }
      }
      this._dropHoverAction = next
    },
    setDropHoverTrashDom(on) {
      const next = !!on
      if (this._dropHoverTrash === next) return
      const board = this.$refs.kanbanBoard
      const el =
        board && board.querySelector('[data-kanban-trash="1"]')
      if (el) el.classList.toggle('is-hot', next)
      this._dropHoverTrash = next
    },
    clearDragHoverDom() {
      this.setDragOverLaneDom(null)
      this.setDropHoverUserIdDom(null)
      this.setDropHoverActionDom(null)
      this.setDropHoverTrashDom(false)
    },
    clearDragGhostEl() {
      this.teardownDocDragGhostListener()
      if (this._ghostMoveRaf) {
        cancelAnimationFrame(this._ghostMoveRaf)
        this._ghostMoveRaf = 0
      }
      if (this._dragGhostEl && this._dragGhostEl.parentNode) {
        this._dragGhostEl.parentNode.removeChild(this._dragGhostEl)
      }
      this._dragGhostEl = null
      this._dragGhostCard = null
      this._dragGhostPtr = null
      this._dragGhostSize = null
      this._overDropChooser = false
      if (this._dragBlankEl && this._dragBlankEl.parentNode) {
        this._dragBlankEl.parentNode.removeChild(this._dragBlankEl)
      }
      this._dragBlankEl = null
    },
    /** 文档级兜底：卡片被刷新卸载时仍能清掉拖拽会话 */
    armDragSessionCleanup() {
      if (typeof document === 'undefined' || this._dragSessionArmed) return
      this._dragSessionArmed = true
      document.addEventListener('dragend', this.onDocDragEndSafety, true)
    },
    disarmDragSessionCleanup() {
      if (typeof document === 'undefined' || !this._dragSessionArmed) return
      this._dragSessionArmed = false
      document.removeEventListener('dragend', this.onDocDragEndSafety, true)
    },
    onDocDragEndSafety() {
      this.resetDragSession()
    },
    /** 幂等：清空拖拽会话全部残留（状态 / 拖影 / 高亮 / 光标 / 监听） */
    resetDragSession() {
      this.disarmDragSessionCleanup()
      this.draggingIds = []
      this.dragFromLane = null
      this.dragAllowedLanes = null
      this.dragForbiddenLanes = null
      this.clearDragHoverDom()
      this.clearDragGhostEl()
      this.clearDragDropCursor()
    },
    onCardDragEnd() {
      this.resetDragSession()
    },
    onLaneDragOver(laneKey, event) {
      const t = event && event.target
      const closest =
        t && typeof t.closest === 'function' ? (sel) => t.closest(sel) : () => null
      /* 垃圾桶热区由自身处理，避免整列禁止光标盖过删除投放 */
      if (closest('.defect-kanban-lane-trash')) return
      const allowed = this.isLaneDropAllowed(laneKey)
      this.setDragDropCursor(allowed, event)
      this.setDragOverLaneDom(allowed ? laneKey : null)
      /* 非选择热区时恢复正常拖影大小 */
      if (!closest('.defect-kanban-assignee-drop-bar')) this.setOverDropChooser(false)
      this.setDropHoverTrashDom(false)
      if (!closest('.defect-kanban-assignee-drop-target')) {
        this.setDropHoverUserIdDom(null)
      }
      if (!closest('.defect-kanban-state-drop-target')) {
        this.setDropHoverActionDom(null)
      }
    },
    onLaneDragLeave(laneKey, event) {
      /* 进入子节点也会触发 leave，需判断是否仍在本泳道内 */
      const laneEl = event && event.currentTarget
      const related = event && event.relatedTarget
      if (laneEl && related && typeof laneEl.contains === 'function' && laneEl.contains(related)) {
        return
      }
      if (this._dragOverLane === laneKey) this.setDragOverLaneDom(null)
      if (laneKey === LANE_AUDIT || laneKey === LANE_PROCESSING) {
        this.setDropHoverUserIdDom(null)
      }
      if (laneKey === LANE_CLOSED) {
        this.setDropHoverActionDom(null)
        this.setDropHoverTrashDom(false)
      }
      this.setOverDropChooser(false)
    },
    onLaneDrop(toLane, forcedAction) {
      const fromLane = this.dragFromLane
      const ids = this.draggingIds.slice()
      this.clearDragHoverDom()
      this.setOverDropChooser(false)
      this.clearDragDropCursor()
      if (!fromLane || !ids.length) return
      if (fromLane === toLane) return
      const idSet = new Set(ids.map(Number))
      const defects = this.defectList.filter((d) => idSet.has(Number(d.defectId)))
      if (!defects.length) return
      let action =
        forcedAction === 'pass' || forcedAction === 'close'
          ? forcedAction
          : resolveKanbanDropAction(
              fromLane,
              toLane,
              defects,
              this.currentUserId
            )
      /* 待验证→已结束：未点状态图标时默认通过 */
      if (
        !forcedAction &&
        fromLane === LANE_AUDIT &&
        toLane === LANE_CLOSED &&
        action === 'close' &&
        defects.every((d) => isDefectActionVisible('pass', d, this.currentUserId))
      ) {
        action = 'pass'
      }
      if (!action) {
        this.$modal.msgWarning(this.$t('defect.kanban.drop-denied'))
        return
      }
      const allOk = defects.every((d) =>
        isDefectActionVisible(action, d, this.currentUserId)
      )
      if (!allOk) {
        const tip =
          action === 'repair'
            ? this.$t('defect.kanban.repair-need-handler')
            : this.$t('defect.kanban.drop-denied')
        this.$modal.msgWarning(tip)
        return
      }
      if (defects.length === 1) {
        this.openSingleAction(action, defects[0])
      } else {
        this.$refs.batchDialog.open(action, defects)
      }
    },
    openSingleAction(action, defect) {
      const refName = SINGLE_DIALOG_REF[action]
      if (!refName || !defect) return
      this.actionDefectId = Number(defect.defectId)
      this.$nextTick(() => {
        const dlg = this.$refs[refName]
        if (dlg && typeof dlg.open === 'function') dlg.open()
      })
    },
    onActionDone() {
      this.search(this.queryParams)
      this.$emit('refresh')
    },
    onBatchDone() {
      this.selectedIds = []
      this.search(this.queryParams)
      this.$emit('refresh')
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/multipane-resizer-grip.scss";

.defect-kanban-root {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
  height: 100%;
}

.defect-kanban-tools {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--cat2bug-toolbar-section-gap, 10px);
  margin-bottom: 10px;
}

.defect-kanban-tools-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.defect-kanban-board {
  display: flex;
  gap: 0;
  min-height: 0;
  flex: 1;
  overflow: hidden;
  &.is-lane-resizing {
    user-select: none;
    cursor: col-resize;
    /* 拖宽时禁止卡片抢事件，减轻布局抖动 */
    .defect-kanban-card,
    .defect-kanban-lane-body {
      pointer-events: none;
    }
  }
  /* 拖卡片时分隔条不抢指针，避免挡 drop */
  &.is-card-dragging .defect-kanban-lane-resizer {
    pointer-events: none;
  }
  /* 拖拽中隐藏操作区但不拆 DOM，避免打断 HTML5 drag */
  &.is-card-dragging .defect-kanban-card-tools {
    visibility: hidden;
    pointer-events: none;
  }
}

.defect-kanban-lane-resizer {
  flex: 0 0 12px;
  position: relative;
  z-index: 4;
  box-sizing: border-box;
  cursor: col-resize;
  background: transparent !important;
  align-self: stretch;
  touch-action: none;
  @include multipane-resizer-vertical-appearance;
  &:after {
    width: 5px;
    height: 28px;
  }
}

.defect-kanban-lane {
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--el-border-color-extra-light, #f2f2f2);
  border-radius: 12px;
  background: var(--el-fill-color-blank, var(--el-bg-color, #fff));
  &.is-drag-over {
    border-color: var(--lane-accent, var(--el-color-primary, #409eff));
    box-shadow:
      inset 0 0 0 1px var(--lane-accent, var(--el-color-primary, #409eff)),
      0 0 0 2px color-mix(in srgb, var(--lane-accent, var(--el-color-primary, #409eff)) 28%, transparent);
    background: color-mix(
      in srgb,
      var(--lane-accent, var(--el-color-primary, #409eff)) 10%,
      var(--el-fill-color-blank, var(--el-bg-color, #fff))
    );
  }
  /* 拖起后非法目标：整列淡化（不用 filter，避免进出禁拖泳道卡顿） */
  &.is-drop-forbidden {
    opacity: 0.45;
    border-color: var(--el-border-color-lighter, #e4e7ed);
    background: var(--el-fill-color-light, #f5f7fa);
    box-shadow: none;
  }
  &.is-drop-forbidden.is-drag-over {
    border-color: var(--el-border-color, #dcdfe6);
    box-shadow: none;
    background: var(--el-fill-color-light, #f5f7fa);
  }
  /* 泳道色：已创建灰 / 处理中红 / 待验证蓝 / 已结束绿 */
  &.lane-created {
    --lane-accent: var(--el-text-color-secondary, #909399);
  }
  &.lane-processing {
    --lane-accent: var(--defect-state-processing-bg, #e04e4e);
  }
  &.lane-audit {
    --lane-accent: var(--defect-state-audit-bg, #337ecc);
  }
  &.lane-closed {
    --lane-accent: var(--defect-state-resolved-bg, #5daf34);
  }
}

.defect-kanban-lane-head {
  --lane-head-control-h: 32px;
  display: flex;
  align-items: center;
  gap: 8px;
  box-sizing: border-box;
  /* 四列等高：含负责人头像行 / 无负责人 spacer */
  min-height: calc(12px + 12px + var(--lane-head-control-h));
  padding: 12px 14px;
  border-bottom: 1px solid var(--el-border-color-extra-light, #f2f2f2);
  border-radius: 12px 12px 0 0;
  background: color-mix(in srgb, var(--lane-accent, #909399) 8%, var(--el-bg-color, #fff));
  /* visible：负责人角标/平铺头像不被标题行裁切 */
  overflow: visible;
  min-width: 0;
}

.defect-kanban-lane-dot {
  flex: 0 0 auto;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--lane-accent, #909399);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--lane-accent, #909399) 22%, transparent);
}

.defect-kanban-lane-title {
  flex: 0 1 auto;
  min-width: 0;
  max-width: 38%;
  font-weight: 600;
  font-size: 13px;
  letter-spacing: 0.02em;
  color: var(--el-text-color-primary, #303133);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.defect-kanban-lane-head-spacer {
  /* 把加号 / 负责人 / 垃圾桶顶到泳道标题行右侧 */
  flex: 1 1 auto;
  min-width: 8px;
}

.defect-kanban-lane-search-wrap {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
  margin: 0;
  padding: 8px 10px 0;
  background: transparent;
}

.defect-kanban-lane-search {
  flex: 1 1 auto;
  width: 100%;
  min-width: 0;
  max-width: none;
  --el-input-border-radius: 8px;
  :deep(.el-input__wrapper) {
    height: 28px;
    min-height: 28px;
    padding: 0 10px 0 8px;
    border-radius: 8px;
    background: var(--el-bg-color, #fff);
    box-shadow: 0 0 0 1px var(--el-border-color-lighter, #ebeef5);
    transition: background 0.15s ease, box-shadow 0.15s ease;
  }
  :deep(.el-input__wrapper:hover) {
    background: var(--el-bg-color, #fff);
    box-shadow: 0 0 0 1px var(--el-border-color, #dcdfe6);
  }
  :deep(.el-input__wrapper.is-focus) {
    background: var(--el-bg-color, #fff);
    box-shadow: 0 0 0 1px var(--el-color-primary, #409eff);
  }
  :deep(.el-input__inner) {
    font-size: 12px;
    height: 26px;
    line-height: 26px;
    color: var(--el-text-color-regular, #606266);
  }
  :deep(.el-input__inner::placeholder) {
    color: var(--el-text-color-placeholder, #a8abb2);
  }
  :deep(.el-input__prefix) {
    display: inline-flex;
    align-items: center;
    color: var(--el-text-color-secondary, #909399);
  }
  :deep(.el-input__prefix .el-icon) {
    font-size: 14px;
    color: var(--el-text-color-secondary, #a8abb2);
  }
  :deep(.el-input__suffix) {
    display: inline-flex;
    align-items: center;
  }
  :deep(.el-input__clear) {
    color: var(--el-text-color-secondary, #909399);
  }
}

.defect-kanban-lane-count {
  flex: 0 0 auto;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  color: color-mix(in srgb, var(--lane-accent, #909399) 70%, var(--el-text-color-secondary, #909399));
  background: color-mix(in srgb, var(--lane-accent, #909399) 14%, transparent);
  border-radius: 999px;
  padding: 0 9px;
  line-height: 22px;
  min-width: 22px;
  text-align: center;
}

.defect-kanban-lane-add-btn {
  flex: 0 0 auto;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  margin: 0;
  padding: 0;
  border: 1px solid var(--el-border-color, #dcdfe6);
  border-radius: 6px;
  /* 用填充色变量，避免 dark 下 color-mix/透明叠出发白块 */
  background: var(--el-fill-color-light, #f5f7fa);
  color: var(--el-text-color-secondary, #909399);
  cursor: pointer;
  z-index: 2;
  font-size: 12px;
  line-height: 1;
  transition: color 0.15s ease, border-color 0.15s ease, background 0.15s ease;
  .defect-kanban-lane-add-icon {
    font-size: 12px;
    color: inherit;
  }
  &:hover:not(:disabled) {
    border-color: var(--el-border-color-darker, var(--el-border-color, #dcdfe6));
    background: var(--el-fill-color, #f0f2f5);
    color: var(--el-text-color-regular, #606266);
  }
  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
}

.defect-kanban-lane-trash {
  --trash-accent: var(--defect-state-resolved-bg, #5daf34);
  flex: 0 0 auto;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  margin: 0;
  padding: 0;
  border: 1px dashed color-mix(in srgb, var(--trash-accent) 45%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--trash-accent) 8%, transparent);
  color: var(--trash-accent);
  cursor: copy;
  z-index: 3;
  font-size: 14px;
  line-height: 1;
  transition:
    color 0.15s ease,
    border-color 0.15s ease,
    background 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.15s ease;
  .defect-kanban-lane-trash-icon {
    font-size: 14px;
    color: inherit;
    pointer-events: none;
  }
  &.is-armed {
    border-style: solid;
    background: color-mix(in srgb, var(--trash-accent) 14%, #fff);
    box-shadow: 0 0 0 2px color-mix(in srgb, var(--trash-accent) 22%, transparent);
  }
  &.is-hot {
    border-color: var(--trash-accent);
    background: var(--trash-accent);
    color: var(--el-color-white, #fff);
    box-shadow:
      0 0 0 3px color-mix(in srgb, var(--trash-accent) 35%, transparent),
      0 6px 14px color-mix(in srgb, var(--trash-accent) 35%, transparent);
  }
}

.defect-kanban-lane-assignees {
  position: relative;
  /* 占满标题行剩余宽度，头像右对齐；宽度按此计算，避免被内容缩窄后误判 +N */
  flex: 1 1 auto;
  min-width: 28px;
  max-width: none;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: var(--lane-head-control-h, 32px);
  margin-left: 0;
  overflow: visible;
  &.is-spacer {
    flex: 0 0 auto;
    width: var(--lane-head-control-h, 32px);
    min-width: var(--lane-head-control-h, 32px);
    max-width: none;
    overflow: visible;
    pointer-events: none;
  }
}

.defect-kanban-lane-assignee-avatars {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0;
  max-width: 100%;
  min-width: 0;
  margin: 0;
  padding: 0 4px;
  border: 1px dashed transparent;
  border-radius: 999px;
  background: transparent;
  cursor: pointer;
  line-height: 1;
  box-sizing: border-box;
  height: var(--lane-head-control-h, 32px);
  min-height: var(--lane-head-control-h, 32px);
  /* 角标可略突出；个数已按叠放宽度收敛 */
  overflow: visible;
  :deep(.project-member-icons) {
    max-width: 100%;
    overflow: visible;
  }
  &:hover {
    border-color: var(--el-border-color, #dcdfe6);
    background: var(--el-fill-color-light, #f5f7fa);
  }
  &.is-empty {
    width: 28px;
    min-width: 28px;
    height: 28px;
    min-height: 28px;
    padding: 0;
    border-color: var(--el-border-color, #dcdfe6);
    background: transparent;
    justify-content: center;
    &:hover {
      border-color: var(--el-border-color-darker, var(--el-border-color, #dcdfe6));
      color: var(--el-text-color-regular, #606266);
      background: var(--el-fill-color-light, #f5f7fa);
    }
  }
  &.is-spacer {
    width: var(--lane-head-control-h, 32px);
    min-width: var(--lane-head-control-h, 32px);
    visibility: hidden;
    pointer-events: none;
    border: none;
    cursor: default;
    padding: 0;
  }
}

.defect-kanban-lane-assignee-empty {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--el-text-color-secondary, #909399);
  .defect-kanban-lane-assignee-empty-icon {
    font-size: 14px;
    color: inherit;
  }
}

/*
 * 点击时用 fixed 贴齐头像矩形作 el-popover 锚点（见 openLaneAssigneePicker）。
 * 勿 width/height:0 / display:none，否则弹层会落到左上角。
 */
.defect-kanban-lane-assignee-picker {
  position: absolute;
  inset: 0;
  opacity: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
  :deep(.select-project-member-input) {
    width: 100% !important;
    min-width: 0 !important;
    max-width: none !important;
    height: 100% !important;
    min-height: 0 !important;
    box-sizing: border-box;
  }
}

.defect-kanban-lane-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.defect-kanban-card-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 0 0 auto;
  min-width: 0;
}

.defect-kanban-assignee-drop-bar {
  position: sticky;
  top: 0;
  z-index: 3;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  margin: -2px 0 6px;
  border-radius: 12px;
  border: 2px solid var(--lane-accent, #337ecc);
  background: color-mix(in srgb, var(--lane-accent, #337ecc) 22%, var(--el-bg-color, #fff));
  box-shadow:
    0 0 0 1px color-mix(in srgb, var(--lane-accent, #337ecc) 35%, transparent),
    0 8px 20px color-mix(in srgb, #000 18%, transparent);
  flex: 0 0 auto;
}

.defect-kanban-assignee-drop-hint {
  font-size: 12px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--lane-accent, #337ecc);
  text-align: center;
}

.defect-kanban-assignee-drop-list {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.defect-kanban-assignee-drop-target {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 72px;
  padding: 10px 8px;
  border-radius: 12px;
  border: 2px solid color-mix(in srgb, var(--lane-accent, #337ecc) 35%, transparent);
  background: var(--el-bg-color-overlay, var(--el-bg-color, #fff));
  box-shadow: 0 2px 8px color-mix(in srgb, #000 12%, transparent);
  cursor: copy;
  /* 不用 transform scale：换人时 sticky 投放条重排/合成易卡几秒 */
  transition: border-color 0.12s ease, box-shadow 0.12s ease, background 0.12s ease;
  &.is-hot {
    border-color: var(--lane-accent, #337ecc);
    background: color-mix(in srgb, var(--lane-accent, #337ecc) 18%, var(--el-bg-color-overlay, #fff));
    box-shadow:
      0 0 0 3px color-mix(in srgb, var(--lane-accent, #337ecc) 40%, transparent),
      0 6px 16px color-mix(in srgb, var(--lane-accent, #337ecc) 35%, transparent);
  }
}

.defect-kanban-assignee-drop-name {
  font-size: 12px;
  font-weight: 600;
  max-width: 72px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--el-text-color-primary, #303133);
}

.defect-kanban-assignee-drop-empty {
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
  padding: 6px;
}

.defect-kanban-state-drop-bar {
  border-color: var(--defect-state-resolved-bg, #5daf34);
  background: color-mix(
    in srgb,
    var(--defect-state-resolved-bg, #5daf34) 18%,
    var(--el-bg-color, #fff)
  );
  .defect-kanban-assignee-drop-hint {
    color: var(--defect-state-resolved-bg, #5daf34);
  }
}

.defect-kanban-state-drop-target {
  &.is-pass {
    border-color: color-mix(in srgb, var(--defect-state-resolved-bg, #5daf34) 40%, transparent);
  }
  &.is-pass.is-hot,
  &.is-pass.is-default {
    border-color: var(--defect-state-resolved-bg, #5daf34);
    background: color-mix(
      in srgb,
      var(--defect-state-resolved-bg, #5daf34) 16%,
      var(--el-bg-color-overlay, #fff)
    );
  }
  &.is-pass.is-default:not(.is-hot) {
    box-shadow: 0 0 0 2px color-mix(in srgb, var(--defect-state-resolved-bg, #5daf34) 28%, transparent);
  }
  &.is-close {
    border-color: color-mix(in srgb, var(--defect-state-closed-color, #909399) 40%, transparent);
  }
  &.is-close.is-hot {
    border-color: var(--defect-state-closed-color, #909399);
    background: color-mix(
      in srgb,
      var(--defect-state-closed-color, #909399) 14%,
      var(--el-bg-color-overlay, #fff)
    );
  }
}

.defect-kanban-state-drop-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  line-height: 1;
  &.is-pass {
    background: color-mix(in srgb, var(--defect-state-resolved-bg, #5daf34) 18%, #fff);
    color: var(--defect-state-resolved-bg, #5daf34);
  }
  &.is-close {
    background: color-mix(in srgb, var(--defect-state-closed-color, #909399) 14%, #fff);
    color: var(--defect-state-closed-color, #909399);
  }
  :deep(.el-icon) {
    color: inherit;
  }
  :deep(svg) {
    width: 1em;
    height: 1em;
    fill: currentColor;
  }
}

.defect-kanban-lane-empty {
  text-align: center;
  color: var(--el-text-color-placeholder, #a8abb2);
  font-size: 12px;
  padding: 36px 8px;
}

.defect-kanban-card {
  --card-accent: var(--defect-state-unknown-bg, #909399);
  position: relative;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  flex: 0 0 auto; /* 泳道纵 flex 下禁止被挤扁（多卡时默认 shrink 会压成一条线） */
  background: var(--el-bg-color-overlay, var(--el-bg-color, #fff));
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 10px;
  cursor: grab;
  user-select: none;
  box-sizing: border-box;
  overflow: hidden;
  box-shadow: 0 1px 2px color-mix(in srgb, #000 6%, transparent);
  /* transform 留给 TransitionGroup 重排动画，避免与 hover 抢同一属性 */
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  &.is-collected {
    box-shadow:
      0 0 0 1px color-mix(in srgb, rgb(247, 186, 42) 35%, transparent),
      0 1px 2px color-mix(in srgb, #000 6%, transparent);
  }
  &.is-collect-flying {
    z-index: 12;
    position: relative;
    will-change: transform, box-shadow;
  }
  &.state-processing {
    --card-accent: var(--defect-state-processing-bg, #e04e4e);
  }
  &.state-rejected {
    --card-accent: var(--defect-state-rejected-bg, #cf9236);
  }
  &.state-audit {
    --card-accent: var(--defect-state-audit-bg, #337ecc);
  }
  &.state-closed {
    --card-accent: var(--defect-state-closed-color, #909399);
  }
  &.state-created {
    --card-accent: var(--el-text-color-secondary, #909399);
  }
  &.state-resolved {
    --card-accent: var(--defect-state-resolved-bg, #5daf34);
  }
  &:hover {
    border-color: color-mix(in srgb, var(--card-accent) 45%, var(--el-border-color-lighter, #ebeef5));
    box-shadow: 0 4px 14px color-mix(in srgb, #000 10%, transparent);
  }
  &.is-collected:hover {
    box-shadow:
      0 0 0 1px color-mix(in srgb, rgb(247, 186, 42) 45%, transparent),
      0 4px 14px color-mix(in srgb, #000 10%, transparent);
  }
  &.is-selected {
    border-color: var(--lane-accent, var(--card-accent));
    box-shadow:
      0 0 0 1px color-mix(in srgb, var(--lane-accent, var(--card-accent)) 35%, transparent),
      0 4px 14px color-mix(in srgb, #000 10%, transparent);
  }
  &.is-dragging {
    opacity: 0.55;
    transform: none;
    box-shadow: none;
    transition: opacity 0.12s ease, transform 0.12s ease, max-height 0.12s ease;
  }
  /* 不可拖：外观保持正常，仅禁止光标 + 无上浮，仍可点选/打开 */
  &.is-drag-locked {
    cursor: not-allowed;
    &:hover {
      transform: none;
      box-shadow: 0 1px 2px color-mix(in srgb, #000 6%, transparent);
      border-color: var(--el-border-color-lighter, #ebeef5);
    }
    .defect-kanban-card-body,
    .defect-kanban-card-title-text,
    .defect-kanban-card-meta {
      cursor: not-allowed;
    }
    .defect-kanban-card-tools,
    .defect-kanban-card-media {
      cursor: pointer;
    }
  }
}

.defect-kanban-card-accent {
  flex: 0 0 3px;
  width: 3px;
  background: var(--card-accent);
}

.defect-kanban-card-body {
  flex: 1;
  min-width: 0;
  padding: 12px 12px 12px 11px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.defect-kanban-card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 24px;
}

.defect-kanban-card-num {
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--el-text-color-secondary, #909399);
  letter-spacing: 0.02em;
}

.defect-kanban-type-flag {
  transform: scale(0.92);
  transform-origin: left center;
}

.defect-kanban-level {
  font-size: 11px;
  line-height: 18px;
  padding: 0 6px;
  border-radius: 4px;
  color: var(--el-text-color-secondary, #909399);
  background: var(--el-fill-color-light, #f5f7fa);
  border: 1px solid var(--el-border-color-extra-light, #f2f2f2);
  max-width: 64px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 0 0 auto;
}

.defect-kanban-card-title {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.45;
}

.defect-kanban-card-title-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
  max-width: 100%;
  width: fit-content;
  color: var(--el-color-primary, #409eff);
  cursor: pointer;
  &:hover {
    text-decoration: underline;
  }
}

.defect-kanban-card-media {
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-fill-color-light, #f5f7fa);
  border: 1px solid var(--el-border-color-extra-light, #f2f2f2);
  line-height: 0;
}

.defect-kanban-card-preview {
  width: 100%;
  display: block;
  :deep(.cat2bug-preview-image > div),
  :deep(.preview) {
    width: 100% !important;
    height: 88px !important;
  }
  :deep(.el-image),
  :deep(.cat2bug-image),
  :deep(img) {
    width: 100% !important;
    height: 88px !important;
    object-fit: cover;
  }
  :deep(.number) {
    right: 6px;
    bottom: 6px;
  }
}

.defect-kanban-card-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 2px;
  min-height: 22px;
}

.defect-kanban-card-handlers {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  max-width: 88px;
  min-width: 0;
  :deep(.project-member-icons > *) {
    margin-right: -6px;
  }
  :deep(.project-member-icons__count) {
    height: 15px;
    min-width: 15px;
    font-size: 10px;
    padding: 0 3px;
    right: -3px;
    bottom: -3px;
  }
}

.defect-kanban-card-tools {
  margin-left: auto;
  min-width: 0;
  max-width: 100%;
  display: flex;
  justify-content: flex-end;
  /* 与表格行内 DefectTools 一致：图标+文字 link */
  :deep(.defect-tools),
  :deep(.defect-tools__bar) {
    flex-wrap: wrap;
    justify-content: flex-end;
    row-gap: 4px;
  }
}

/* 状态 tag 落在子组件根节点，与 defect-kanban-state-flag 同元素 */
:deep(.defect-kanban-state-flag) {
  flex: 0 0 auto;
  font-size: 11px !important;
  line-height: 16px !important;
  padding: 1px 7px !important;
  border-radius: 999px !important;
  font-weight: 500;
  border-width: 0 !important;
}
:deep(.defect-kanban-state-flag.processing) {
  background: color-mix(in srgb, var(--defect-state-processing-bg, #e04e4e) 18%, transparent) !important;
  color: var(--defect-state-processing-bg, #e04e4e) !important;
}
:deep(.defect-kanban-state-flag.audit) {
  background: color-mix(in srgb, var(--defect-state-audit-bg, #337ecc) 18%, transparent) !important;
  color: var(--defect-state-audit-bg, #337ecc) !important;
}
:deep(.defect-kanban-state-flag.rejected) {
  background: color-mix(in srgb, var(--defect-state-rejected-bg, #cf9236) 18%, transparent) !important;
  color: var(--defect-state-rejected-bg, #cf9236) !important;
}
:deep(.defect-kanban-state-flag.closed) {
  background: color-mix(in srgb, var(--defect-state-closed-bg, #909399) 18%, transparent) !important;
  color: var(--el-text-color-secondary, #909399) !important;
}
:deep(.defect-kanban-state-flag.created) {
  background: color-mix(in srgb, var(--defect-state-created-bg, #909399) 18%, transparent) !important;
  color: var(--el-text-color-secondary, #909399) !important;
}
:deep(.defect-kanban-state-flag.resolved) {
  background: color-mix(in srgb, var(--defect-state-resolved-bg, #5daf34) 18%, transparent) !important;
  color: var(--defect-state-resolved-bg, #5daf34) !important;
}
:deep(.defect-kanban-state-flag.unknown) {
  background: color-mix(in srgb, var(--defect-state-unknown-bg, #909399) 18%, transparent) !important;
  color: var(--el-text-color-secondary, #909399) !important;
}
</style>

<style lang="scss">
/* 拖到无权泳道时禁止光标（勿用 * 通配，会拖垮样式计算） */
body.defect-kanban-drop-forbidden {
  cursor: not-allowed !important;
}
body.defect-kanban-drop-forbidden .defect-kanban-board {
  cursor: not-allowed !important;
}
body.defect-kanban-lane-resizing {
  cursor: col-resize !important;
  user-select: none !important;
}
body.defect-kanban-lane-resizing * {
  cursor: col-resize !important;
  user-select: none !important;
}

/* 外层只跟手（无过渡）；叠层缩放可动画 */
.defect-kanban-drag-ghost-wrap {
  position: fixed;
  top: 0;
  left: 0;
  margin: 0;
  pointer-events: none;
  z-index: 99999;
  will-change: transform;
}
.defect-kanban-drag-ghost-stack {
  position: relative;
  transform-origin: top left;
  transform: scale(1);
  /* 短过渡，避免拖动中长时间合成层动画拖慢跟手 */
  transition: transform 0.12s ease;
  cursor: grabbing;
  overflow: visible;
  &.is-compact {
    transform: scale(0.42);
  }
  &.is-multi {
    /* 给角标与背后叠层留出空间，避免被裁切 */
    padding: 12px 16px 14px 0;
  }
}
.defect-kanban-drag-ghost-layer {
  box-sizing: border-box;
  margin: 0;
  border-radius: 10px;
  &.is-back {
    position: absolute;
    /* 与 is-multi 的 padding-top 对齐正面卡 */
    left: 0;
    top: 12px;
    background: var(--el-bg-color-overlay, var(--el-bg-color, #fff));
    border: 1px solid var(--el-border-color-lighter, #ebeef5);
    box-shadow: 0 6px 16px color-mix(in srgb, #000 16%, transparent);
    opacity: 0.92;
  }
  &.is-back.is-depth-1 {
    transform: translate(6px, 6px) rotate(1.2deg);
    z-index: 1;
  }
  &.is-back.is-depth-2 {
    transform: translate(12px, 10px) rotate(2.4deg);
    z-index: 0;
    opacity: 0.78;
  }
  &.is-front {
    position: relative;
    z-index: 2;
  }
}
.defect-kanban-drag-ghost {
  box-sizing: border-box;
  margin: 0;
  box-shadow: 0 10px 28px color-mix(in srgb, #000 28%, transparent);
  cursor: grabbing;
}
.defect-kanban-drag-ghost-stack.is-compact .defect-kanban-drag-ghost {
  box-shadow: 0 6px 16px color-mix(in srgb, #000 22%, transparent);
}
.defect-kanban-drag-ghost-badge {
  position: absolute;
  top: 0;
  right: 0;
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  border-radius: 999px;
  background: var(--el-color-primary, #409eff);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  line-height: 28px;
  text-align: center;
  box-shadow: 0 2px 8px color-mix(in srgb, #000 25%, transparent);
  z-index: 5;
}
</style>
