<template>
  <div class="defect-log-update">
    <div class="defect-log-update-header">
      <defect-log-user size="medium" :name="log.createByName" :avatar="log.createByAvatar" />
      <span class="state">{{$i18n.t('defect.update-summary',{count:changes.length})}}</span>
    </div>
    <ul v-if="changes.length>0" class="defect-log-update-changes">
      <li v-for="(change,index) in changes" :key="index" class="defect-log-update-change">
        <span class="field">{{ fieldLabel(change.field) }}:</span>
        <template v-if="change.type==='enum' && change.field === 'defectType'">
          <span class="defect-log-type-col defect-log-type-old">
            <defect-type-flag
              v-if="hasEnumValue(change.oldValue)"
              :defect="defectTypeFlagModel(change.oldValue)"
            />
            <span v-else class="type-enum-empty">{{ $i18n.t('defect.value-empty') }}</span>
          </span>
          <i class="el-icon-right arrow"></i>
          <span class="defect-log-type-col">
            <defect-type-flag
              v-if="hasEnumValue(change.newValue)"
              :defect="defectTypeFlagModel(change.newValue)"
            />
            <span v-else class="type-enum-empty">{{ $i18n.t('defect.value-empty') }}</span>
          </span>
        </template>
        <template v-else-if="change.type==='enum'">
          <span class="old">{{ enumLabel(change.field, change.oldValue, change.oldDisplay) }}</span>
          <i class="el-icon-right arrow"></i>
          <span class="new">{{ enumLabel(change.field, change.newValue, change.newDisplay) }}</span>
        </template>
        <template v-else-if="change.type==='users'">
          <span class="handle-by-tags-wrap handle-by-side-old" :title="change.oldDisplay">
            <row-list-member
              v-if="handleByMembers(change, 'old').length > 1"
              :members="handleByMembers(change, 'old')"
              size="small"
            />
            <defect-log-user
              v-else-if="handleByMembers(change, 'old').length === 1"
              :member="handleByMembers(change, 'old')[0]"
              muted
            />
            <span v-else class="plain-old">{{ displayOrEmpty(change.oldDisplay) }}</span>
          </span>
          <i class="el-icon-right arrow"></i>
          <span class="handle-by-tags-wrap" :title="change.newDisplay">
            <row-list-member
              v-if="handleByMembers(change, 'new').length > 1"
              :members="handleByMembers(change, 'new')"
              size="small"
            />
            <defect-log-user
              v-else-if="handleByMembers(change, 'new').length === 1"
              :member="handleByMembers(change, 'new')[0]"
            />
            <span v-else class="plain-new">{{ displayOrEmpty(change.newDisplay) }}</span>
          </span>
        </template>
        <template v-else-if="change.type==='module' || change.type==='case'">
          <span class="old" :title="change.oldDisplay">{{ displayOrEmpty(change.oldDisplay) }}</span>
          <i class="el-icon-right arrow"></i>
          <span class="new" :title="change.newDisplay">{{ displayOrEmpty(change.newDisplay) }}</span>
        </template>
        <template v-else-if="change.type==='time'">
          <span class="old">{{ displayOrEmpty(change.oldDisplay) }}</span>
          <i class="el-icon-right arrow"></i>
          <span class="new">{{ displayOrEmpty(change.newDisplay) }}</span>
        </template>
        <template v-else-if="change.type==='images' || change.type==='files'">
          <span class="media-change-row" v-for="lab in [mediaLabels(change)]" :key="'media-' + index">
            <span class="media-side media-old">{{ lab.old }}</span>
            <i class="el-icon-right arrow"></i>
            <span class="media-side media-new">
              <span class="media-new-main">{{ lab.newMain }}</span>
              <span class="media-new-delta">{{ lab.delta }}</span>
            </span>
          </span>
        </template>
        <template v-else>
          <el-tooltip
            v-if="isLong(change.oldValue) || isLong(change.newValue)"
            placement="top"
            :effect="change.field === 'defectName' ? 'dark' : 'light'"
          >
            <div
              slot="content"
              :class="[
                'defect-log-update-tooltip',
                change.field === 'defectName' ? 'defect-log-update-tooltip--dark' : ''
              ]"
            >
              <div class="tooltip-block">
                <div class="tooltip-title">{{$i18n.t('defect.value-old')}}</div>
                <pre class="tooltip-pre">{{ valueText(change.oldValue) }}</pre>
              </div>
              <div class="tooltip-block">
                <div class="tooltip-title">{{$i18n.t('defect.value-new')}}</div>
                <pre class="tooltip-pre">{{ valueText(change.newValue) }}</pre>
              </div>
            </div>
            <span class="long-text">
              <span class="old">{{ truncate(change.oldValue) }}</span>
              <i class="el-icon-right arrow"></i>
              <span class="new">{{ truncate(change.newValue) }}</span>
            </span>
          </el-tooltip>
          <template v-else>
            <span class="old">{{ valueText(change.oldValue) }}</span>
            <i class="el-icon-right arrow"></i>
            <span class="new">{{ valueText(change.newValue) }}</span>
          </template>
        </template>
      </li>
    </ul>
    <div v-else-if="rawDescribe" class="defect-log-update-raw">
      <span>{{$i18n.t('describe')}}:</span>
      <span>{{ rawDescribe }}</span>
    </div>
  </div>
</template>

<script>
import DefectTypeFlag from '@/components/Defect/DefectTypeFlag';
import DefectLogUser from './DefectLogUser';
import RowListMember from '@/components/RowListMember';

const TRUNCATE_LIMIT = 60;

export default {
  name: "UPDATE",
  components: { DefectTypeFlag, DefectLogUser, RowListMember },
  props:{
    log: {
      type: Object,
      default: ()=>({})
    }
  },
  computed: {
    parsed: function () {
      const text = this.log && this.log.defectLogDescribe;
      if (!text) return null;
      try {
        const arr = JSON.parse(text);
        return Array.isArray(arr) ? arr : null;
      } catch (e) {
        return null;
      }
    },
    changes: function () {
      return this.parsed || [];
    },
    rawDescribe: function () {
      return this.parsed ? '' : (this.log && this.log.defectLogDescribe) || '';
    }
  },
  methods: {
    fieldLabel(field) {
      const key = 'defect.field.' + field;
      const label = this.$i18n.t(key);
      return label === key ? field : label;
    },
    /** 与详情标题 {@link DefectTypeFlag} 相同结构：defectTypeName 为 BUG/TASK/DEMAND 等 */
    defectTypeFlagModel(value) {
      return { defectTypeName: String(value) };
    },
    hasEnumValue(value) {
      return value !== null && value !== undefined && String(value).trim().length > 0;
    },
    enumLabel(field, value, fallback) {
      if (value === null || value === undefined || value === '') {
        return this.$i18n.t('defect.value-empty');
      }
      const i18nKey = String(value);
      const translated = this.$i18n.t(i18nKey);
      if (translated && translated !== i18nKey) {
        return translated;
      }
      return fallback != null ? String(fallback) : i18nKey;
    },
    displayOrEmpty(value) {
      if (value === null || value === undefined || value === '') {
        return this.$i18n.t('defect.value-empty');
      }
      return value;
    },
    splitHandleByNames(change, side) {
      const disp = side === 'old' ? change.oldDisplay : change.newDisplay;
      if (disp == null || disp === '') {
        return [];
      }
      return String(disp).split(',').map(s => s.trim()).filter(Boolean);
    },
    handleByMembers(change, side) {
      return this.splitHandleByNames(change, side).map(name => ({ nickName: name, userName: name }));
    },
    /** 兼容旧日志里的「3 (+1/-2)」展示格式 */
    parseLegacyMediaDisplay(newDisplay) {
      if (newDisplay == null || typeof newDisplay !== 'string') {
        return null;
      }
      const m = newDisplay.match(/^(\d+) \(\+(\d+)\/-(\d+)\)$/);
      if (!m) {
        return null;
      }
      return {
        newTotal: parseInt(m[1], 10),
        added: parseInt(m[2], 10),
        removed: parseInt(m[3], 10),
      };
    },
    mediaCounts(change) {
      const oldCount = Number(change.oldValue);
      let newCount = Number(change.newValue);
      let added = change.urlAddedCount;
      let removed = change.urlRemovedCount;
      if ((added == null || removed == null) && change.newDisplay != null) {
        const leg = this.parseLegacyMediaDisplay(String(change.newDisplay));
        if (leg) {
          newCount = leg.newTotal;
          added = leg.added;
          removed = leg.removed;
        }
      }
      if (added == null) {
        added = Math.max(0, newCount - oldCount);
      }
      if (removed == null) {
        removed = Math.max(0, oldCount - newCount);
      }
      return {
        oldCount: Number.isFinite(oldCount) ? oldCount : 0,
        newCount: Number.isFinite(newCount) ? newCount : 0,
        added: Number(added) || 0,
        removed: Number(removed) || 0,
      };
    },
    mediaLabels(change) {
      const suf = change.type === 'images' ? '-img' : '-file';
      const { oldCount, newCount, added, removed } = this.mediaCounts(change);
      return {
        old: this.$i18n.t('defect.log.media-old' + suf, { count: oldCount }),
        newMain: this.$i18n.t('defect.log.media-new' + suf, { count: newCount }),
        delta: this.$i18n.t('defect.log.media-delta' + suf, { added, removed }),
      };
    },
    valueText(value) {
      if (value === null || value === undefined || value === '') {
        return this.$i18n.t('defect.value-empty');
      }
      if (typeof value === 'object') {
        try { return JSON.stringify(value); } catch (e) { return String(value); }
      }
      return String(value);
    },
    isLong(value) {
      if (value === null || value === undefined) return false;
      const s = typeof value === 'string' ? value : (typeof value === 'object' ? JSON.stringify(value) : String(value));
      return s && s.length > TRUNCATE_LIMIT;
    },
    truncate(value) {
      const text = this.valueText(value);
      if (text === this.$i18n.t('defect.value-empty')) return text;
      if (text.length <= TRUNCATE_LIMIT) return text;
      return text.slice(0, TRUNCATE_LIMIT) + '...';
    }
  }
}
</script>

<style lang="scss" scoped>
.defect-log-update {
  font-size: 14px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  width: 100%;

  .defect-log-update-header {
    display: inline-flex;
    align-items: center;
    flex-wrap: wrap;
    span {
      padding-left: 2px;
      padding-right: 2px;
    }
    .state {
      color: #409EFF;
    }
  }

  .defect-log-update-changes {
    list-style: none;
    margin: 4px 0 0 0;
    padding: 0;
    width: 100%;
  }

  .defect-log-update-change {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    line-height: 1.8;
    color: #606266;

    .field {
      color: #909399;
      margin-right: 6px;
      flex-shrink: 0;
    }
    .defect-log-type-col {
      display: inline-flex;
      align-items: center;
      vertical-align: middle;
      max-width: 280px;
    }
    .defect-log-type-old {
      opacity: 0.82;
      ::v-deep .defect-type-tag {
        text-decoration: line-through;
      }
    }
    .type-enum-empty {
      color: #909399;
      font-size: 13px;
    }
    .old {
      color: #909399;
      text-decoration: line-through;
      max-width: 240px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .new {
      color: #303133;
      max-width: 320px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .plain-old {
      color: #909399;
      text-decoration: line-through;
      max-width: 240px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .plain-new {
      color: #303133;
      max-width: 320px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .handle-by-tags-wrap {
      display: inline-flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 4px;
      max-width: 300px;
      vertical-align: middle;
    }
    .handle-by-side-old {
      opacity: 0.75;
    }
    .media-change-row {
      display: inline-flex;
      align-items: baseline;
      flex-wrap: wrap;
      row-gap: 4px;
      max-width: 100%;
    }
    .media-side.media-old {
      color: #909399;
      text-decoration: line-through;
      max-width: 220px;
    }
    .media-side.media-new {
      color: #303133;
      max-width: 360px;
    }
    .media-new-main {
      font-weight: 500;
    }
    .media-new-delta {
      color: #909399;
      font-size: 12px;
      margin-left: 6px;
      white-space: normal;
    }
    .arrow {
      margin: 0 6px;
      color: #c0c4cc;
    }
    .long-text {
      display: inline-flex;
      align-items: center;
      cursor: help;
    }
  }

  .defect-log-update-raw {
    margin-top: 4px;
    color: #606266;
  }
}
</style>

<style lang="scss">
.defect-log-update-tooltip {
  max-width: 480px;
  .tooltip-block + .tooltip-block {
    margin-top: 6px;
    padding-top: 6px;
    border-top: 1px dashed #ebeef5;
  }
  .tooltip-title {
    color: #909399;
    margin-bottom: 2px;
  }
  .tooltip-pre {
    margin: 0;
    white-space: pre-wrap;
    word-break: break-all;
    font-family: inherit;
    max-height: 240px;
    overflow: auto;
  }

  /** 缺陷名称：与 el-tooltip effect=dark 搭配 */
  .defect-log-update-tooltip--dark {
    .tooltip-title {
      color: rgba(255, 255, 255, 0.72);
    }
    .tooltip-pre {
      color: #ffffff;
    }
    .tooltip-block + .tooltip-block {
      border-top-color: rgba(255, 255, 255, 0.18);
    }
  }
}
</style>
