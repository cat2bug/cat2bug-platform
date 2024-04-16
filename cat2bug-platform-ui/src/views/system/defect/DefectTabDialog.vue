<template>
  <el-dialog
    :title="$t('defect.create-tab')"
    :visible.sync="dialogVisible"
    width="40%"
    :before-close="close">
    <el-form ref="form" :rules="rules" :model="form" label-width="120px">
      <el-form-item :label="$t('defect.tab-name')" prop="tabName">
        <el-input
          v-model="form.tabName"
          :placeholder="$t('defect.enter-tab-name')"
          maxlength="32"
          clearable></el-input>
      </el-form-item>
      <el-form-item :label="$t('defect.title')" prop="defectName">
        <el-input
          v-model="form.config.defectName"
          :placeholder="$t('defect.enter-like-title')"
          maxlength="128"
          clearable
        />
      </el-form-item>
      <el-form-item :label="$t('defect.my-following')">
        <el-switch v-model="form.config.params.collect" active-value="1" inactive-value="0"></el-switch>
      </el-form-item>
      <el-form-item :label="$t('defect.type')" prop="defectType">
        <el-select v-model="form.config.defectType" clearable collapse-tags :placeholder="$t('defect.select-type')">
          <el-option
            v-for="type in config.types"
            :key="type.key"
            :label="$i18n.t(type.value)"
            :value="type.value">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('defect.state')" prop="defectState">
        <el-select v-model="form.config.params.defectStates" clearable multiple collapse-tags :placeholder="$t('defect.select-state')">
          <el-option
            v-for="state in config.states"
            :key="state.key"
            :label="$i18n.t(state.value)"
            :value="state.key">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('module')" prop="moduleId">
        <select-module v-model="form.config.moduleId" :project-id="form.projectId" :is-edit="false" size="medium" />
      </el-form-item>
      <el-form-item :label="$t('version')" prop="moduleVersion">
        <el-input
          v-model="form.moduleVersion"
          :placeholder="$t('defect.enter-version')"
          maxlength="64"
          clearable
        />
      </el-form-item>
      <el-form-item :label="$t('handle-by')" prop="handleBy">
        <select-project-member
          v-model="form.config.handleBy"
          :project-id="projectId"
          :placeholder="$t('defect.select-handle-by').toString()"
          :is-head="false"
          size="medium"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onCreateTab">{{$t('create')}}</el-button>
        <el-button @click="close">{{$t('cancel')}}</el-button>
      </el-form-item>
    </el-form>
  </el-dialog>
</template>

<script>
import {configDefect} from "@/api/system/defect";
import SelectProjectMember from "@/components/Project/SelectProjectMember";
import SelectModule from "@/components/Module/SelectModule";
import {addTabs} from "@/api/system/DefectTabs";

export default {
  name: "DefectTabDialog",
  components: {SelectProjectMember, SelectModule},
  data() {
    return {
      dialogVisible: false,
      config: {},
      form: {
        projectId: null,
        config: {
          params: {}
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
      this.dialogVisible = false;
    },
    open() {
      this.reset();
      this.dialogVisible = true;
    },
    reset() {
      this.form = {
        projectId: this.projectId,
        userId: this.memberId,
        config: {
          params: {}
        }
      }
      this.resetForm("form");
    },
    onCreateTab() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          addTabs(this.form).then(res=>{
            this.$message.success("创建成功");
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

</style>
