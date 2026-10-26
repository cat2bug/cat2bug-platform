<template>
  <el-dialog
    :title="$t('workbench.tab-dialog.title')"
    v-model="dialogVisible"
    width="480px"
    append-to-body
    @closed="onClosed"
  >
    <el-form v-if="dialogVisible" ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item :label="$t('workbench.tab-dialog.name')" prop="tabName">
        <el-input
          v-model="form.tabName"
          :placeholder="$t('workbench.tab-dialog.name-ph')"
          maxlength="12"
          show-word-limit
          clearable
        />
      </el-form-item>
      <el-form-item :label="$t('workbench.tab-dialog.preset')" prop="preset">
        <el-select v-model="form.preset" style="width: 100%">
          <el-option
            v-for="id in createPresetIds"
            :key="id"
            :label="$t('workbench.preset.' + id)"
            :value="id"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        v-if="canManageShared"
        :label="$t('workbench.tab-dialog.visibility')"
        prop="visibility"
      >
        <el-radio-group v-model="form.visibility">
          <el-radio label="private">{{ $t('workbench.tab-dialog.private') }}</el-radio>
          <el-radio label="project" :disabled="sharedFull">
            {{ $t('workbench.tab-dialog.shared') }}
          </el-radio>
        </el-radio-group>
        <div v-if="sharedFull" class="wb-tab-dialog__hint">
          {{ $t('workbench.tab-dialog.shared-limit', [sharedLimit]) }}
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close">{{ $t('cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">{{ $t('create') }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { CREATE_PRESET_IDS, buildTabConfigFromPreset } from './presets'
import { createWorkbenchTab } from '@/api/system/workbench'

export default {
  name: 'WorkbenchTabDialog',
  props: {
    projectId: { type: [Number, String], default: null },
    canManageShared: { type: Boolean, default: false },
    sharedCount: { type: Number, default: 0 },
    sharedLimit: { type: Number, default: 24 }
  },
  emits: ['created'],
  data() {
    return {
      dialogVisible: false,
      submitting: false,
      createPresetIds: CREATE_PRESET_IDS,
      form: {
        tabName: '',
        preset: 'tester',
        visibility: 'private'
      },
      rules: {
        tabName: [
          { required: true, message: () => this.$t('workbench.tab-dialog.name-required'), trigger: 'blur' },
          {
            validator: (_r, v, cb) => {
              if (v && String(v).trim().length > 12) {
                cb(new Error(this.$t('workbench.tab-dialog.name-max')))
              } else cb()
            },
            trigger: 'blur'
          }
        ],
        preset: [{ required: true, trigger: 'change' }]
      }
    }
  },
  computed: {
    sharedFull() {
      return this.sharedCount >= this.sharedLimit
    }
  },
  methods: {
    open() {
      this.form = {
        tabName: '',
        preset: 'tester',
        visibility: 'private'
      }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.formRef?.clearValidate?.())
    },
    close() {
      this.dialogVisible = false
    },
    onClosed() {
      this.submitting = false
    },
    submit() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        if (!this.projectId) return
        let visibility = 'private'
        if (this.canManageShared && this.form.visibility === 'project') {
          if (this.sharedFull) {
            this.$modal.msgWarning(this.$t('workbench.tab-dialog.shared-limit', [this.sharedLimit]))
            return
          }
          visibility = 'project'
        }
        this.submitting = true
        try {
          const configJson = buildTabConfigFromPreset(this.form.preset)
          const res = await createWorkbenchTab(this.projectId, {
            tabName: String(this.form.tabName).trim(),
            preset: this.form.preset,
            visibility,
            configJson
          })
          const tab = res.data || res
          this.dialogVisible = false
          this.$emit('created', tab)
        } catch (e) {
          this.$modal.msgError(e?.message || this.$t('workbench.save-failed'))
        } finally {
          this.submitting = false
        }
      })
    }
  }
}
</script>

<style scoped>
.wb-tab-dialog__hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-color-warning);
  line-height: 1.4;
}
</style>
