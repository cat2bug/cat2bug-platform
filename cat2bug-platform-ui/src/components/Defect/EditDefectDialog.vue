<template>
  <el-dialog
    :append-to-body="true"
    width="75%"
    :title="title"
    :visible.sync="visible">
    <div class="app-container defect-edit-body">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <defect-form-ordered-fields
          v-if="projectId"
          ref="orderedFields"
          :project-id="projectId"
          :form="form"
          :config="config"
          :plan-time-range.sync="planTimeRange"
          :describe-tools="describeTools"
          :defect-field-layout="defectFieldLayout"
          :is-builtin-defect-field-visible="isBuiltinDefectFieldVisible"
          @step-change="stepChangeHandle"
        >
          <template #describe-tools>
            <project-ai-model-select
              v-if="projectId"
              :project-id="projectId"
              v-model="defectAiModelId"
              embed-ai-button
              :ai-loading="aiButtonLoading"
              :ai-tooltip="$t('defect.ai-filling-in').toString()"
              @service-type="defectAiServiceType = $event"
              @ai-click="createDefectByAiHandle"
            />
          </template>
        </defect-form-ordered-fields>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="cancel">{{  $t('cancel') }}</el-button>
        <el-button type="primary" @click="submitForm">{{ $t('save') }}</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script>
import {configDefect, getDefect, updateDefect} from "@/api/system/defect";
import ProjectAiModelSelect from "@/components/Ai/ProjectAiModelSelect.vue";
import DefectFormOrderedFields from "@/components/Defect/DefectFormOrderedFields";
import {
  makeDefectMember,
  makeDefectModule,
  makeDefectTitle,
  makeDefectType,
  makeDefectVersion
} from "@/api/ai/AiDefect";
import {strFormat} from "@/utils";
import {normalizeDefectTypeAndLevel} from "@/utils/defect-defaults";
import defectBuiltinFieldVisibility from "@/mixins/defect-builtin-field-visibility";

export default {
  name: "EditDefect",
  dicts: ['defect_level'],
  mixins: [defectBuiltinFieldVisibility],
  components: { ProjectAiModelSelect, DefectFormOrderedFields },
  data() {
    return {
      describeTools: [],
      aiButtonLoading: false,
      defectAiModelId: null,
      defectAiServiceType: 'ollama',
      // 计划时间范围
      planTimeRange:[],
      // 标题
      title: this.$i18n.t('defect.modify'),
      // 当前缺陷ID
      defectId: null,
      config: {},
      // 显示窗口
      visible: false,
      // 表单参数
      form: {
        defectLevel: 'middle'
      },
      // 表单校验
      rules: {
        handleBy: [
          { required: true, message: this.$i18n.t('defect.handle-by-cannot-empty'), trigger: "input" }
        ],
        defectName: [
          { required: true, message: this.$i18n.t('defect.defect-name-cannot-empty'), trigger: "input" }
        ],
      },
    }
  },
  props: {
    projectId: {
      type: Number,
      default: null
    },
  },
  computed: {
    getUrl: function () {
      return function (urls){
        let imgs = urls?urls.split(','):[];
        return imgs.map(i=>{
          return process.env.VUE_APP_BASE_API + i;
        })
      }
    },
    getFileName: function () {
      return function (url) {
        if(!url) return null;
        let arr = url.split('\/');
        return arr[arr.length-1];
      }
    }
  },
  methods:{
    applyDefectFormRulesForBuiltinLayout() {
      const rules = {}
      if (this.isBuiltinDefectFieldVisible('handleBy')) {
        rules.handleBy = [
          { required: true, message: this.$i18n.t('defect.handle-by-cannot-empty'), trigger: 'input' }
        ]
      }
      if (this.isBuiltinDefectFieldVisible('defectName')) {
        rules.defectName = [
          { required: true, message: this.$i18n.t('defect.defect-name-cannot-empty'), trigger: 'input' }
        ]
      }
      this.rules = rules
    },
    /** 获取缺陷配置 */
    getDefectConfig() {
      configDefect().then(res=>{
        this.config = res.data;
      })
    },
    /** 获取缺陷信息 */
    getDefectInfo(defectId) {
      getDefect(defectId).then(res=>{
        this.form = res.data;
        if(this.form.planStartTime) {
          this.planTimeRange.push(this.form.planStartTime);
        }
        if(this.form.planEndTime) {
          this.planTimeRange.push(this.form.planEndTime);
        }
      });
    },
    // 打开操作
    open(defectId) {
      this.reset();
      this.defectId = defectId;
      this.getDefectConfig();
      this.getDefectInfo(defectId);
      this.visible = true;
    },
    // 取消按钮
    cancel() {
      this.visible = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.planTimeRange = [];
      this.defectAiModelId = null;
      this.defectAiServiceType = 'ollama';
      this.form = {
        defectId: null,
        defectType: null,
        defectName: null,
        defectDescribe: null,
        annexUrls: null,
        projectId: null,
        testPlanId: null,
        caseId: null,
        dataSources: null,
        dataSourcesParams: null,
        moduleId: null,
        moduleVersion: null,
        createBy: null,
        updateTime: null,
        createTime: null,
        updateBy: null,
        defectState: null,
        caseStepId: null,
        handleBy: null,
        handleTime: null,
        defectLevel: 'middle',
        customFields: {}
      };
      this.resetForm("form");
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          normalizeDefectTypeAndLevel(this.form);
          if(this.planTimeRange.length>1) {
            this.form.planStartTime = this.planTimeRange[0];
            this.form.planEndTime = this.planTimeRange[1];
          }
          updateDefect(this.form).then(res => {
            this.$modal.msgSuccess(this.$i18n.t('modify-success'));
            this.visible = false;
            this.$emit('log', this.form)
          });
        }
      });
    },
    stepChangeHandle(index){
      this.form.caseStepId = index;
    },
    /** 处理人：清空后为 []，[] 在 JS 中为 truthy，需单独判断 */
    defectHandleByNeedsAiFill() {
      const hb = this.form.handleBy;
      if (hb == null) return true;
      return Array.isArray(hb) && hb.length === 0;
    },
    /** 与新建缺陷一致：根据描述 AI 补全未填字段 */
    createDefectByAiHandle() {
      if (this.aiButtonLoading) {
        this.$message.error(this.$i18n.t("defect.ai-re-run").toString());
        return;
      }
      const startSeconds = new Date().getTime();
      if (!this.form.defectDescribe) {
        this.$message.error(this.$i18n.t("defect.describe-cannot-empty").toString());
        return;
      }
      if (!this.defectAiModelId) {
        this.$message.error(this.$i18n.t("case.model-content-not-empty").toString());
        return;
      }
      this.aiButtonLoading = true;
      let makeTitle;
      let makeModule;
      let makeType;
      let makeMember;
      let makeVersion;
      const params = {
        projectId: this.projectId,
        describe: this.form.defectDescribe,
        modelId: this.defectAiModelId != null ? String(this.defectAiModelId) : null,
        serviceType: this.defectAiServiceType || "ollama",
      };
      if (!this.form.defectName) {
        makeTitle = makeDefectTitle(params).then((res) => {
          if (!this.form.defectName && res.code == 200 && res.data.title) {
            this.form.defectName = res.data.title;
            this.$message.success(strFormat(this.$i18n.t("fill-finish"), this.$i18n.t("title").toString()));
          }
        });
      }
      if (!this.form.moduleId) {
        makeModule = makeDefectModule(params).then((res) => {
          if (!this.form.moduleId && res.code == 200 && res.data && res.data.moduleId) {
            this.form.moduleId = res.data.moduleId;
            this.$message.success(strFormat(this.$i18n.t("fill-finish"), this.$i18n.t("module").toString()));
          }
        });
      }
      if (!this.form.defectType) {
        makeType = makeDefectType(params).then((res) => {
          if (!this.form.defectType && res.code == 200 && res.data.type) {
            this.form.defectType = res.data.type;
            this.$message.success(strFormat(this.$i18n.t("fill-finish"), this.$i18n.t("type").toString()));
          }
        });
      }
      if (this.defectHandleByNeedsAiFill()) {
        makeMember = makeDefectMember(params).then((res) => {
          if (this.defectHandleByNeedsAiFill() && res.code == 200 && res.data.memberId) {
            this.$set(this.form, "handleBy", [res.data.memberId]);
            this.$message.success(strFormat(this.$i18n.t("fill-finish"), this.$i18n.t("handle-by").toString()));
          }
        });
      }
      if (!this.form.moduleVersion) {
        makeVersion = makeDefectVersion(params).then((res) => {
          if (this.form.moduleVersion || res.code !== 200 || !res.data) {
            return;
          }
          const raw = res.data;
          const v =
            raw.version != null && String(raw.version).trim() !== ""
              ? String(raw.version).trim()
              : raw.moduleVersion != null && String(raw.moduleVersion).trim() !== ""
                ? String(raw.moduleVersion).trim()
                : null;
          if (!v) {
            return;
          }
          this.$set(this.form, "moduleVersion", v);
          this.$message.success(
            strFormat(this.$i18n.t("fill-finish"), this.$i18n.t("version").toString())
          );
        });
      }
      if (!makeTitle && !makeModule && !makeType && !makeMember && !makeVersion) {
        this.aiButtonLoading = false;
        this.$message.error(this.$i18n.t("defect.no-fields-analyze").toString());
        return;
      }
      Promise.all([makeTitle, makeModule, makeType, makeMember, makeVersion])
        .then(() => {
          const self = this;
          setTimeout(() => {
            self.aiButtonLoading = false;
            self.$message({
              message: strFormat(
                self.$i18n.t("defect.analyze-finish"),
                Math.floor((new Date().getTime() - startSeconds) / 1000)
              ),
              type: "success",
              duration: 5000,
            });
          }, 1000);
        })
        .catch((e) => {
          this.aiButtonLoading = false;
          this.$message.error(e);
        });
    },
  }
}
</script>

<style lang="scss" scoped>
.selectTime .defect-plan-datetimerange.el-date-editor--datetimerange.el-input__inner {
  width: 100% !important;
  max-width: 100%;
  min-width: 0 !important;
  box-sizing: border-box;
}

.cat2-bug-textarea-button {
  span {
    margin-left: 3px;
    font-size: 12px;
  }
}
.cat2-bug-textarea-button[handle="true"] {
  background-color: rgb(236, 245, 255);
  border-radius: 15px;
}

</style>
