<template>
  <el-dialog
    :title="$t('defect.create-tab')"
    :visible.sync="dialogVisible"
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
            <el-switch v-model="form.config.params.collect" active-value="1" inactive-value="0" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('defect.show-deleted')" label-width="120px">
            <el-switch v-model="form.config.params.delFlag" active-value="2" inactive-value="0" />
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
    <div slot="footer" class="dialog-footer">
      <el-button @click="close">{{ $t('cancel') }}</el-button>
      <el-button type="primary" @click="onCreateTab">{{ $t('create') }}</el-button>
    </div>
  </el-dialog>
</template>

<script>
import {configDefect} from "@/api/system/defect";
import DefectTabOrderedFilterFields from "@/components/Defect/DefectTabOrderedFilterFields";
import {addTabs} from "@/api/system/DefectTabs";
import { loadDefectColumnLayout } from '@/utils/defect-custom-field-columns'

export default {
  name: "DefectTabDialog",
  components: { DefectTabOrderedFilterFields },
  data() {
    return {
      dialogVisible: false,
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
    },
    open() {
      this.reset();
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
      this.form = {
        projectId: this.projectId,
        userId: this.memberId,
        config: {
          customFieldFilters: [],
          params: {
            delFlag: '0',
            collect: '0',
            defectStates: []
          }
        }
      }
      this.resetForm("form");
    },
    onCreateTab() {
      this.$refs["form"].validate(valid => {
        if (valid) {
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
.defect-tab-switch-row ::v-deep .el-form-item {
  margin-bottom: 18px;
}

.defect-tab-switch-row ::v-deep .el-form-item__label {
  line-height: 32px;
}

.defect-tab-switch-row ::v-deep .el-form-item__content {
  display: flex;
  align-items: center;
  min-height: 32px;
  line-height: 32px;
}
</style>
