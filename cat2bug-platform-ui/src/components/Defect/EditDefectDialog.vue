<template>
  <el-dialog
    :append-to-body="true"
    width="75%"
    :title="title"
    :visible.sync="visible">
    <div class="app-container defect-edit-body">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item :label="$t('defect.name')" prop="defectName">
              <el-input v-model="form.defectName" :placeholder="$t('defect.enter-name')" maxlength="128" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('type')" prop="defectType">
              <el-select v-model="form.defectType" clearable :placeholder="$t('defect.select-type')">
                <el-option
                  v-for="type in config.types"
                  :key="type.key"
                  :label="$t(type.value)"
                  :value="type.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('priority')" prop="defectLevel">
              <el-select v-model="form.defectLevel" clearable :placeholder="$t('defect.select-level')">
                <el-option
                  v-for="dict in dict.type.defect_level"
                  :key="dict.value"
                  :label="$t(dict.value)?$t(dict.value):dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('handle-by')" prop="handleBy">
              <select-project-member v-model="form.handleBy" :project-id="projectId"  />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('version')" prop="moduleVersion">
              <el-input v-model="form.moduleVersion" :placeholder="$t('defect.enter-version')" maxlength="128" style="max-width: 300px;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('module')" prop="moduleId">
              <select-module v-model="form.moduleId" :project-id="projectId"/>
            </el-form-item>
          </el-col>
          <el-col :span="12" class="selectTime">
            <el-form-item :label="$t('plan-time')" prop="planEndTime">
              <el-date-picker
                class="defect-plan-datetimerange"
                v-model="planTimeRange"
                type="datetimerange"
                :range-separator="$t('time-to')"
                :start-placeholder="$t('plan-start-time')"
                :end-placeholder="$t('plan-end-time')"
                value-format="yyyy-MM-dd HH:mm:ss"
                :placeholder="$t('defect.please-select-end-time')">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('case')" prop="caseId">
          <select-case ref="selectCase" v-model="form.caseId" :module-id="form.moduleId" :step-index="form.caseStepId" @step-change="stepChangeHandle" />
        </el-form-item>
        <el-form-item :label="$t('describe')" prop="defectDescribe">
          <cat2-bug-textarea
            ref="cat2bugTextarea"
            :name="$t('describe').toString()"
            :placeholder="$t('defect.enter-markdown-describe').toString()"
            :tools="describeTools"
            v-model="form.defectDescribe"
            maxlength="65536"
            rows="8"
            show-word-limit
            show-tools
          >
            <template v-slot:tools>
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
          </cat2-bug-textarea>
        </el-form-item>
        <el-form-item :label="$t('image')" prop="imgUrls">
          <image-upload v-model="form.imgUrls" :limit="9"></image-upload>
        </el-form-item>
        <el-form-item :label="$t('annex')" prop="annexUrls">
          <file-upload v-model="form.annexUrls" :limit="9" :file-type="[]"/>
        </el-form-item>
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
import SelectProjectMember from "@/components/Project/SelectProjectMember"
import SelectModule from "@/components/Module/SelectModule"
import ImageUpload from "@/components/ImageUpload";
import SelectCase from "@/components/Case/SelectCase";
import Cat2BugTextarea from "@/components/Cat2BugTextarea";
import ProjectAiModelSelect from "@/components/Ai/ProjectAiModelSelect.vue";
import {
  makeDefectMember,
  makeDefectModule,
  makeDefectTitle,
  makeDefectType,
  makeDefectVersion
} from "@/api/ai/AiDefect";
import {strFormat} from "@/utils";
import {normalizeDefectTypeAndLevel} from "@/utils/defect-defaults";

export default {
  name: "EditDefect",
  dicts: ['defect_level'],
  components: { ImageUpload, SelectProjectMember, SelectModule, SelectCase, Cat2BugTextarea, ProjectAiModelSelect },
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
        defectLevel: 'middle'
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
.dialog-footer {
  text-align: right;
}

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
