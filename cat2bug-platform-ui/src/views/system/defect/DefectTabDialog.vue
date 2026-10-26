<template>
  <el-dialog
    :title="dialogTitle"
    v-model="dialogVisible"
    width="40%"
    :before-close="handleBeforeClose"
    @closed="handleClosed">
    <el-form v-if="dialogVisible" ref="form" :rules="rules" :model="form" label-width="120px">
      <el-form-item :label="$t('defect.tab-name')" prop="tabName">
        <el-input
          v-model="form.tabName"
          :placeholder="$t('defect.enter-tab-name')"
          maxlength="32"
          clearable></el-input>
      </el-form-item>
      <el-form-item :label="$t('defect.keyword')" prop="nameVersionKeyword">
        <el-input
          v-model="form.config.nameVersionKeyword"
          :placeholder="$t('defect.enter-name-or-version')"
          maxlength="128"
          clearable
        />
      </el-form-item>
      <el-row :gutter="16" class="defect-tab-switch-row">
        <el-col :span="12">
          <el-form-item :label="$t('defect.my-following')" label-width="120px">
            <el-switch v-model="form.config.params.collect" size="default" active-value="1" inactive-value="0" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('defect.show-deleted')" label-width="120px">
            <el-switch v-model="form.config.params.delFlag" size="default" active-value="2" inactive-value="0" />
          </el-form-item>
        </el-col>
      </el-row>
      <defect-tab-ordered-filter-fields
        v-if="projectId"
        :project-id="projectId"
        :config="form.config"
        :field-layout="fieldLayout"
        :defect-types="config.types || []"
        :defect-states-options="config.states || []"
      />
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">{{ $t('cancel') }}</el-button>
        <el-button type="primary" @click="onSubmitTab">{{ submitButtonLabel }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import {configDefect} from "@/api/system/defect";
import DefectTabOrderedFilterFields from "@/components/Defect/DefectTabOrderedFilterFields";
import {addTabs, updateTabs} from "@/api/system/DefectTabs";
import { loadDefectColumnLayout } from '@/utils/defect-custom-field-columns'

function cloneTabPayload(tab) {
  try {
    return JSON.parse(JSON.stringify(tab))
  } catch (e) {
    return null
  }
}

function ensureConfigShape(config) {
  const base = config && typeof config === 'object' ? config : {}
  const params = base.params && typeof base.params === 'object' ? { ...base.params } : {}
  if (params.delFlag == null || params.delFlag === '') {
    params.delFlag = '0'
  }
  if (params.collect == null || params.collect === '') {
    params.collect = '0'
  }
  if (!Array.isArray(params.defectStates)) {
    params.defectStates = []
  }
  return {
    ...base,
    params,
    customFieldFilters: Array.isArray(base.customFieldFilters) ? base.customFieldFilters : []
  }
}

export default {
  name: "DefectTabDialog",
  components: { DefectTabOrderedFilterFields },
  data() {
    return {
      dialogVisible: false,
      editMode: false,
      config: {},
      fieldLayout: null,
      form: {
        projectId: null,
        config: {
          params: {},
          customFieldFilters: []
        }
      },
      rules: {
        tabName: [
          {required: true, message: this.$t('defect.tab-name-cannot-empty'), trigger: "blur"},
        ],
      },
    }
  },
  props: {
    projectId: {
      type: Number,
      default: 0
    },
    memberId: {
      type: Number,
      default: 0
    }
  },
  computed: {
    dialogTitle() {
      return this.editMode
        ? this.$t('defect.edit-tab')
        : this.$t('defect.create-tab')
    },
    submitButtonLabel() {
      return this.editMode
        ? this.$t('save')
        : this.$t('create')
    }
  },
  created() {
    this.getDefectConfig();
  },
  methods: {
    getDefectConfig() {
      configDefect().then(res=>{
        this.config = res.data;
      })
    },
    close() {
      this.dialogVisible = false
    },
    handleBeforeClose(done) {
      this.close()
      if (typeof done === 'function') {
        done()
      }
    },
    handleClosed() {
      this.fieldLayout = null
      this.editMode = false
    },
    /** @param {object} [tab] 传入则进入编辑模式并预填 */
    open(tab) {
      if (tab && tab.tabId != null) {
        this.resetFromTab(tab)
      } else {
        this.reset()
      }
      this.loadFieldLayout();
      this.dialogVisible = true;
    },
    loadFieldLayout() {
      if (!this.projectId) {
        this.fieldLayout = null
        return
      }
      loadDefectColumnLayout(this.projectId).then(layout => {
        this.fieldLayout = layout
      }).catch(() => {
        this.fieldLayout = null
      })
    },
    reset() {
      this.editMode = false
      this.form = {
        projectId: this.projectId,
        userId: this.memberId,
        // 越大越靠前，与列表 order by tab_sort desc 一致
        tabSort: Date.now(),
        config: ensureConfigShape({
          customFieldFilters: [],
          params: {
            delFlag: '0',
            collect: '0',
            defectStates: []
          }
        })
      }
      this.resetForm("form");
    },
    resetFromTab(tab) {
      const cloned = cloneTabPayload(tab)
      this.editMode = true
      this.form = {
        tabId: cloned.tabId,
        tabName: cloned.tabName,
        projectId: cloned.projectId != null ? cloned.projectId : this.projectId,
        userId: cloned.userId != null ? cloned.userId : this.memberId,
        tabSort: cloned.tabSort,
        config: ensureConfigShape(cloned.config)
      }
      this.resetForm("form");
    },
    onSubmitTab() {
      this.$refs["form"].validate(valid => {
        if (!valid) return
        if (this.editMode) {
          updateTabs(this.form).then(res => {
            this.$message.success(this.$i18n.t('modify-success').toString());
            const payload = (res && res.data) ? res.data : this.form
            this.$emit("update", payload);
            this.close();
          });
        } else {
          addTabs(this.form).then(res=>{
            this.$message.success(this.$i18n.t('create-success').toString());
            this.$emit("add",res.data);
            this.close();
          });
        }
      });
    }
  }
}
</script>

<style scoped>
.defect-tab-switch-row :deep(.el-form-item){
  margin-bottom: 18px;
}

.defect-tab-switch-row :deep(.el-form-item__label){
  line-height: 32px;
}

.defect-tab-switch-row :deep(.el-form-item__content){
  display: flex;
  align-items: center;
  min-height: 32px;
  line-height: 32px;
}
</style>
