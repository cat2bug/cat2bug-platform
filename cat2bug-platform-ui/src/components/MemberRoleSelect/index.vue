<template>
  <el-popover
    :visible="open"
    placement="bottom-start"
    :width="228"
    trigger="click"
    :disabled="disabled"
    :teleported="true"
    popper-class="member-role-select-popper"
    @update:visible="onVisible"
  >
    <template #reference>
      <button
        type="button"
        class="member-role-select"
        :class="{
          'is-open': open,
          'is-disabled': disabled,
          'is-empty': !displayRoles.length
        }"
        :disabled="disabled"
        :title="selectedTitle"
      >
        <span class="member-role-select__chips">
          <span
            v-for="role in displayRoles"
            :key="role.roleId"
            class="member-role-chip"
          >
            <span class="member-role-chip__text">{{ role.roleName }}</span>
            <span
              v-if="!disabled"
              class="member-role-chip__remove"
              role="button"
              tabindex="-1"
              @click.stop="removeRole(role.roleId)"
            >×</span>
          </span>
          <span v-if="!displayRoles.length" class="member-role-select__placeholder">
            {{ placeholder }}
          </span>
        </span>
        <el-icon class="member-role-select__caret" :class="{ 'is-reverse': open }">
          <ArrowDown />
        </el-icon>
      </button>
    </template>
    <div class="member-role-menu" @click.stop>
      <label
        v-for="opt in options"
        :key="opt.roleId"
        class="member-role-option"
        :class="{ 'is-disabled': isOptionDisabled(opt) }"
      >
        <el-checkbox
          :model-value="isChecked(opt.roleId)"
          :disabled="disabled || isOptionDisabled(opt)"
          @change="(val) => toggleRole(opt.roleId, val)"
        >
          {{ opt.roleName }}
        </el-checkbox>
      </label>
      <div v-if="!options.length" class="member-role-menu__empty">
        {{ placeholder }}
      </div>
    </div>
  </el-popover>
</template>

<script>
import { ArrowDown } from '@element-plus/icons-vue'

function toIdList(value) {
  if (Array.isArray(value)) return value.filter(v => v != null)
  if (value == null || value === '') return []
  return [value]
}

function sameIdSet(a, b) {
  const left = toIdList(a).map(String).sort()
  const right = toIdList(b).map(String).sort()
  if (left.length !== right.length) return false
  return left.every((id, i) => id === right[i])
}

export default {
  name: 'MemberRoleSelect',
  components: { ArrowDown },
  props: {
    modelValue: {
      type: [Array, Object],
      default: () => []
    },
    options: {
      type: Array,
      default: () => []
    },
    disabled: {
      type: Boolean,
      default: false
    },
    placeholder: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue', 'change'],
  data() {
    return {
      open: false,
      /** 打开弹层期间的草稿；关闭时再一次性提交，避免逐次保存互相覆盖 */
      draftIds: null
    }
  },
  computed: {
    committedIds() {
      return toIdList(this.modelValue)
    },
    activeIds() {
      return this.draftIds != null ? this.draftIds : this.committedIds
    },
    displayRoles() {
      const map = new Map((this.options || []).map(o => [String(o.roleId), o]))
      return this.activeIds
        .map(id => map.get(String(id)) || { roleId: id, roleName: String(id) })
        .filter(Boolean)
    },
    selectedTitle() {
      return this.displayRoles.map(r => r.roleName).join(' / ')
    }
  },
  methods: {
    onVisible(v) {
      if (this.disabled) {
        this.open = false
        this.draftIds = null
        return
      }
      if (v) {
        this.draftIds = [...this.committedIds]
        this.open = true
        return
      }
      this.open = false
      this.commitDraft()
    },
    isChecked(roleId) {
      const key = String(roleId)
      return this.activeIds.some(id => String(id) === key)
    },
    isOptionDisabled(opt) {
      return !opt || opt.status == 1 || opt.disabled === true
    },
    emitValue(next) {
      const normalized = Array.from(new Set(toIdList(next)))
      this.$emit('update:modelValue', normalized)
      this.$emit('change', normalized)
    },
    commitDraft() {
      if (this.draftIds == null) return
      const next = Array.from(new Set(toIdList(this.draftIds)))
      const prev = this.committedIds
      this.draftIds = null
      if (!sameIdSet(next, prev)) {
        this.emitValue(next)
      }
    },
    toggleRole(roleId, checked) {
      const next = [...this.activeIds]
      const idx = next.findIndex(id => String(id) === String(roleId))
      if (checked) {
        if (idx < 0) next.push(roleId)
      } else if (idx >= 0) {
        next.splice(idx, 1)
      }
      if (this.open) {
        this.draftIds = next
        return
      }
      this.emitValue(next)
    },
    removeRole(roleId) {
      if (this.disabled) return
      const next = this.activeIds.filter(id => String(id) !== String(roleId))
      if (this.open) {
        this.draftIds = next
        return
      }
      this.emitValue(next)
    }
  }
}
</script>

<style lang="scss" scoped>
.member-role-select {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  min-height: 30px;
  margin: 0;
  padding: 2px 4px 2px 2px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--el-text-color-regular, #606266);
  cursor: pointer;
  vertical-align: middle;
  transition: background-color 0.15s ease;

  &:hover:not(.is-disabled),
  &.is-open {
    background: var(--el-fill-color-light, rgba(255, 255, 255, 0.06));
  }

  &.is-disabled {
    cursor: default;
    opacity: 0.72;
  }

  &__chips {
    display: inline-flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px;
    min-width: 0;
    flex: 1 1 auto;
  }

  &__placeholder {
    font-size: 13px;
    line-height: 22px;
    color: var(--el-text-color-placeholder, #a8abb2);
  }

  &__caret {
    flex-shrink: 0;
    font-size: 12px;
    color: var(--el-text-color-placeholder, #a8abb2);
    transition: transform 0.2s ease;

    &.is-reverse {
      transform: rotate(180deg);
    }
  }
}

.member-role-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 140px;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: var(--el-color-primary-light-9, rgba(64, 158, 255, 0.16));
  color: var(--el-color-primary, #409eff);
  font-size: 12px;
  line-height: 24px;

  &__text {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__remove {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 14px;
    height: 14px;
    border-radius: 50%;
    font-size: 12px;
    line-height: 1;
    opacity: 0.75;
    cursor: pointer;

    &:hover {
      opacity: 1;
      background: rgba(64, 158, 255, 0.2);
    }
  }
}

.member-role-menu {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 260px;
  overflow: auto;
  padding: 2px 0;

  &__empty {
    padding: 8px 4px;
    font-size: 13px;
    color: var(--el-text-color-placeholder, #a8abb2);
  }
}

.member-role-option {
  display: flex;
  align-items: center;
  margin: 0;
  padding: 4px 6px;
  border-radius: 6px;
  cursor: pointer;

  &:hover:not(.is-disabled) {
    background: var(--el-fill-color-light, rgba(255, 255, 255, 0.06));
  }

  &.is-disabled {
    cursor: not-allowed;
    opacity: 0.55;
  }

  :deep(.el-checkbox) {
    width: 100%;
    height: auto;
    margin-right: 0;
  }

  :deep(.el-checkbox__label) {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>

<style lang="scss">
.member-role-select-popper.el-popover {
  padding: 8px;
}
</style>
