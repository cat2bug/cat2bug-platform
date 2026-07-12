<template>
  <div class="app-container" ref="teamOptionSubMain">
    <el-row class="project-add-page-header team-option-sub-hint-back">
      <el-page-header @back="goBack" :content="$t('team.info')">
      </el-page-header>
    </el-row>
    <el-row>
      <el-col :span="16">
        <el-skeleton :loading="loading" animated>
          <template #template>
            <div style="padding: 14px;">
              <el-skeleton-item variant="h3" />
            </div>
            <el-skeleton-item
              variant="image"
              style="width: 150px; height: 150px;"
            />
            <div style="padding: 14px;">
              <el-skeleton-item variant="h3" />
              <el-skeleton-item variant="h3" />
            </div>
          </template>
          <template #default>
            <el-form ref="form" :model="form" :rules="rules" label-width="120px">
              <el-form-item :label="$t('team.name')" prop="teamName">
                <el-input v-model="form.teamName" :placeholder="$t('team.please-enter-name')" maxlength="64" />
              </el-form-item>
              <el-form-item prop="teamIcon" class="cat2bug-form-item-upload">
                <template #label>{{ $t('team.icon') }}</template>
                <image-upload v-model="form.teamIcon" :limit="1"/>
              </el-form-item>
              <el-form-item :label="$t('team.introduce')" prop="introduce">
                <el-input v-model="form.introduce" :placeholder="$t('team.please-enter-introduce')" type="textarea" maxlength="255" :rows="5" show-word-limit />
              </el-form-item>
              <el-form-item class="page-form-actions">
                <div class="page-form-actions__buttons">
                  <el-button @click="goBack">{{$t('cancel')}}</el-button>
                  <el-button class="defect-kbd-hint-host" type="primary" @click="submitForm">
                    {{$t('save')}}
                    <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
                  </el-button>
                </div>
              </el-form-item>
            </el-form>
          </template>
        </el-skeleton>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import {getTeam, updateTeam} from "@/api/system/team";
import teamOptionSubFormKbd from '@/mixins/team-option-sub-form-kbd'
import { storeToRefs } from 'pinia';
import { useUserStore } from '@/stores/user'

export default {
  name: "TeamBaseInfo",
  mixins: [teamOptionSubFormKbd],
  setup() {
    const userStore = useUserStore()
    const { config } = storeToRefs(userStore)
    return { config }
  },
  data() {
    return {
      loading: true,
      form: {},
      rules: {
        teamName: [
          { required: true, message: this.$t('team.name-cannot-empty'), trigger: "blur" }
        ],
        teamIcon: [
          { required: true, message: this.$t('team.icon-cannot-empty'), trigger: "blur" }
        ],
      }
    };
  },
  computed: {
  },
  mounted() {
    this.getTeamInfo();
  },
  methods: {
    getTeamId() {
      return this.config.currentTeamId;
    },
    getTeamInfo(){
      this.loading = true;
      getTeam(this.getTeamId()).then(res=>{
        this.loading = false;
        this.form = res.data;
        this.$nextTick(() => this.capturePageFormCloseBaseline())
      });
    },
    shortcutSave() {
      this.submitForm()
    },
    serializePageFormCloseState() {
      return JSON.stringify({ form: { ...this.form } })
    },
    reset() {
      this.form = {
        teamId: this.getTeamId(),
        teamName: null,
        teamIcon: null,
        introduce: null
      };
      this.resetForm("form");
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          updateTeam(this.form).then(() => {
            this.$modal.msgSuccess(this.$t('save.success'));
            this.capturePageFormCloseBaseline()
            this.$router.go(0);
          });
        }
      });
    },
  }
}
</script>
