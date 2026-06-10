<template>
  <div class="app-container" ref="projectOptionSubMain">
    <el-row class="project-add-page-header project-option-sub-hint-back">
      <el-page-header @back="goBack" :content="$t('project.push')">
      </el-page-header>
    </el-row>
    <el-form ref="form" :model="form" :rules="rules" label-width="80px">
      <el-form-item :label="$t('project.push.key')" prop="pullKey">
        <el-input v-model="form.pullKey"></el-input>
      </el-form-item>
      <el-form-item class="page-form-actions">
        <div class="page-form-actions__buttons">
          <el-button @click="goBack">{{ $t('cancel') }}</el-button>
          <el-button class="defect-kbd-hint-host" type="primary" @click="onSubmit">
            {{ $t('sync') }}
            <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
          </el-button>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import {pullProject} from "@/api/system/project";
import projectOptionSubFormKbd from '@/mixins/project-option-sub-form-kbd'

export default {
  name: "index",
  mixins: [projectOptionSubFormKbd],
  data() {
    return {
      form: {
        pullKey: '',
      },
      rules: {
        pullKey: [
          {required: true, message: this.$i18n.t('project.push.key-cannot-empty'), trigger: "change"}
        ],
      }
    }
  },
  computed: {
    teamId() {
      return this.$store.state.user.config.currentTeamId;
    },
    projectId() {
      return this.$store.state.user.config.currentProjectId
    },
  },
  mounted() {
    this.$nextTick(() => this.capturePageFormCloseBaseline())
  },
  methods: {
    shortcutSave() {
      this.onSubmit()
    },
    serializePageFormCloseState() {
      return JSON.stringify({ form: { ...this.form } })
    },
    /** 提交项目同步操作 */
    onSubmit() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          pullProject(this.projectId, this.form).then(res => {
            this.$message.success('同步成功');
          });
        }
      });
    },
  }
}
</script>

<style scoped>

</style>
