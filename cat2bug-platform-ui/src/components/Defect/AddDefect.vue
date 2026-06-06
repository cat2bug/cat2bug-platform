<template>
  <el-drawer
    custom-class="defect-drawer-accent"
    size="65%"
    :visible.sync="visible"
    direction="rtl"
    :append-to-body="appendToBody"
    :close-on-press-escape="false"
    @close="cancel"
    :before-close="closeDefectDrawer">
    <template slot="title">
      <div class="defect-add-header">
        <div class="defect-add-title">
          <i class="el-icon-arrow-left" @click="cancel"></i>
          <h3>{{$t('defect.create')}}</h3>
        </div>
        <div>
          <el-button class="defect-kbd-hint-host" @click="cancel" icon="el-icon-close" size="small">
            {{$t('close')}}
            <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint" aria-hidden="true">{{ dialogCloseShortcutLabel }}</span>
          </el-button>
          <el-button class="defect-kbd-hint-host" type="primary" icon="el-icon-finished" @click="submitForm" size="small">
            {{$t('defect.save')}}
            <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
          </el-button>
        </div>
      </div>
    </template>
    <div class="app-container">
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item class="save-cache-form-item">
          <template slot="label"><span class="save-cache-label-placeholder"></span></template>
          <span class="save-form-cache-row">
            <span v-show="fieldHintsActive" class="cat2bug-field-hint save-cache-field-hint" aria-hidden="true">O</span>
            <el-checkbox class="save-form-cache" v-model="isSaveFormCache" @change="handleSaveFormCache">{{ $t('defect.added-save-form-cache') }}</el-checkbox>
          </span>
        </el-form-item>
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
          @module-change="moduleChangeHandle"
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
  <!--      <el-form-item label="测试用例id" prop="caseId">-->
  <!--        <el-input v-model="form.caseId" placeholder="请输入测试用例id" />-->
  <!--      </el-form-item>-->
  <!--      <el-form-item label="数据来源" prop="dataSources">-->
  <!--        <el-input v-model="form.dataSources" placeholder="请输入数据来源" />-->
  <!--      </el-form-item>-->
  <!--      <el-form-item label="数据来源相关参数" prop="dataSourcesParams">-->
  <!--        <el-input v-model="form.dataSourcesParams" placeholder="请输入数据来源相关参数" />-->
  <!--      </el-form-item>-->
  <!--      <el-form-item label="处理时间" prop="handleTime">-->
  <!--        <el-date-picker clearable-->
  <!--                        v-model="form.handleTime"-->
  <!--                        type="date"-->
  <!--                        value-format="yyyy-MM-dd"-->
  <!--                        placeholder="请选择处理时间">-->
  <!--        </el-date-picker>-->
  <!--      </el-form-item>-->
      </el-form>
      <div slot="footer" class="dialog-footer"></div>
    </div>
  </el-drawer>
</template>

<script>
import {addDefect, configDefect, updateDefect} from "@/api/system/defect";
import ProjectAiModelSelect from "@/components/Ai/ProjectAiModelSelect.vue";
import DefectFormOrderedFields from "@/components/Defect/DefectFormOrderedFields";
import {upload} from "@/api/common/upload";
import {
  makeDefect,
  makeDefectMember,
  makeDefectModule,
  makeDefectTitle,
  makeDefectType,
  makeDefectVersion
} from "@/api/ai/AiDefect";
import {strFormat} from "@/utils";
import {normalizeDefectTypeAndLevel} from "@/utils/defect-defaults";
import defectBuiltinFieldVisibility from "@/mixins/defect-builtin-field-visibility";
import dialogFormShortcuts from "@/mixins/dialog-form-shortcuts";
import formFieldHints from "@/mixins/form-field-hints";

// 是否保存缺陷添加表单缓存的key键值
const IS_SAVE_FORM_CACHE_KEY = 'defect-is-save-add-form-cache';
// 缺陷添加表单缓存的key键值
const FORM_CACHE_KEY = 'defect-add-form-cache';

export default {
  name: "AddDefect",
  dicts: ['defect_level'],
  mixins: [defectBuiltinFieldVisibility, dialogFormShortcuts, formFieldHints],
  components: { ProjectAiModelSelect, DefectFormOrderedFields },
  data() {
    return {
      // 是否缓存缺陷表单
      isSaveFormCache: false,
      // 计划时间范围
      planTimeRange:[],
      // AI按钮加载是否显示
      aiButtonLoading: false,
      defectAiModelId: null,
      defectAiServiceType: 'ollama',
      // 显示窗口
      visible: false,
      config: {},
      // 表单参数
      form: {
        defectType: 'BUG',
        defectLevel: 'middle',
        customFields: {}
      },
      // 表单校验
      rules: {
        handleBy: [
          { required: true, message: this.$i18n.t('defect.handle-by-cannot-empty'), trigger: "input" }
        ],
        defectName: [
          { required: true, message: this.$i18n.t('defect.defect-name-cannot-empty'), trigger: "input" }
        ],
        // defectDescribe: [
        //   { required: true, message: this.$i18n.t('defect.describe-cannot-empty'), trigger: "input" }
        // ],
      },
      describeTools:[]
    }
  },
  props: {
    projectId: {
      type: Number,
      default: null
    },
    appendToBody: {
      type: Boolean,
      default: false
    }
  },
  mounted() {
    this.readIsSaveFormCacheValue();
    if(this.isSaveFormCache) {
      this.readAddFormCache();
    }
    setTimeout(()=>{
      this.getDefectConfig();
    },0)

  },
  methods:{
    /** 字段快捷聚焦的扫描容器：仅扫描表单内的字段 */
    getFieldHintContainer() {
      return (this.$refs.form && this.$refs.form.$el) || this.$el;
    },
    /** 固定快捷键：「保存本次选项」复选框，按键直接切换（徽标在模板中渲染于勾选框前） */
    getFixedFieldHints() {
      return [
        {
          letter: 'O',
          injectBadge: false,
          onActivate: () => {
            this.isSaveFormCache = !this.isSaveFormCache;
            this.handleSaveFormCache();
          }
        }
      ];
    },
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
    /** 处理保存表单缓存开关状态改变的操作 */
    handleSaveFormCache() {
      this.saveIsSaveFormCacheValue();
    },
    /** 读取是否添加的表单本地缓存的开关选项 */
    readIsSaveFormCacheValue() {
      const isCache = this.$cache.local.get(IS_SAVE_FORM_CACHE_KEY);
      this.isSaveFormCache = isCache&&isCache=='true'?true:false;
    },
    /** 获取是否添加的表单本地缓存的开关选项 */
    saveIsSaveFormCacheValue() {
      const isCache = this.$cache.local.set(IS_SAVE_FORM_CACHE_KEY,this.isSaveFormCache);
    },
    /** 获取添加的表单本地缓存（覆盖 reset 后的默认值，用于连续新建） */
    readAddFormCache() {
      const cacheForm = this.$cache.local.getJSON(FORM_CACHE_KEY) || {};
      if (cacheForm.defectType != null && cacheForm.defectType !== '') {
        this.form.defectType = cacheForm.defectType;
      }
      if (cacheForm.defectLevel != null && cacheForm.defectLevel !== '') {
        this.form.defectLevel = cacheForm.defectLevel;
      }
      if (cacheForm.handleBy != null) {
        this.form.handleBy = cacheForm.handleBy;
      }
      if (cacheForm.moduleVersion != null && cacheForm.moduleVersion !== '') {
        this.form.moduleVersion = cacheForm.moduleVersion;
      }
      if (cacheForm.moduleId != null) {
        this.form.moduleId = cacheForm.moduleId;
      }
    },
    /** 保存添加表单本地缓存 */
    saveAddFormCache() {
      this.$cache.local.setJSON(FORM_CACHE_KEY, {
        defectType: this.form.defectType,
        defectLevel: this.form.defectLevel,
        handleBy: this.form.handleBy,
        moduleVersion: this.form.moduleVersion,
        moduleId: this.form.moduleId
      });
    },
    getDefectConfig() {
      configDefect().then(res=>{
        this.config = res.data;
      })
    },
    open(data) {
      this.reset(data);
      this.visible = true;
      if (typeof this.$_bindDialogShortcuts === 'function') {
        this.$_bindDialogShortcuts();
      }
      this.$nextTick(() => {
        const orderedFields = this.$refs.orderedFields
        if (orderedFields) {
          orderedFields.refreshCustomFieldDefaults()
          const selectModule = orderedFields.getSelectModuleRef && orderedFields.getSelectModuleRef()
          if (selectModule && typeof selectModule.resetFromForm === 'function') {
            selectModule.resetFromForm(this.form.moduleId)
          }
          orderedFields.focusDefectName()
        }
      });
    },
    openByCase(data) {
      this.open(data);
    },
    // 取消按钮
    cancel(isReset) {
      this.visible = false;
      if(isReset){
        this.reset();
      }
      this.$emit('close');
    },
    // 表单重置
    reset(data) {
      this.planTimeRange=[];
      this.defectAiModelId = null;
      this.defectAiServiceType = 'ollama';
      data = data || {};
      this.form = {
        defectId: null,
        defectType: 'BUG',
        defectName: null,
        defectDescribe: null,
        annexUrls: null,
        imgUrls: data.imgUrls || null,
        projectId: data.projectId || this.projectId,
        testPlanId: null,
        caseId: data.caseId || null,
        dataSources: null,
        dataSourcesParams: null,
        moduleId: data.moduleId || null,
        moduleVersion: data.moduleVersion || null,
        createBy: null,
        updateTime: null,
        createTime: null,
        updateBy: null,
        defectState: null,
        caseStepId: 0,
        handleBy: null,
        handleTime: null,
        defectLevel: 'middle',
        customFields: {}
      };
      this.resetForm("form");
      this.readIsSaveFormCacheValue();
      if(this.isSaveFormCache) {
        this.readAddFormCache();
      }
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          normalizeDefectTypeAndLevel(this.form);
          this.form.projectId = this.projectId;
          if(this.planTimeRange.length>1) {
            this.form.planStartTime = this.planTimeRange[0];
            this.form.planEndTime = this.planTimeRange[1];
          }
          if (this.form.defectId != null) {
            updateDefect(this.form).then(res => {
              if(this.isSaveFormCache) {
                this.saveAddFormCache();
              }
              this.$modal.msgSuccess(this.$i18n.t('modify-success'));
              this.cancel();
              this.$emit('added',res)
            });
          } else {
            addDefect(this.form).then(res => {
              if (this.isSaveFormCache) {
                this.saveAddFormCache();
              }
              this.$modal.msgSuccess(this.$i18n.t('create-success'));
              this.$emit('added', res.data);
              if (this.isSaveFormCache) {
                this.reset();
                this.$nextTick(() => {
                  const orderedFields = this.$refs.orderedFields
                  if (orderedFields) {
                    const selectModule = orderedFields.getSelectModuleRef && orderedFields.getSelectModuleRef()
                    if (selectModule && typeof selectModule.resetFromForm === 'function') {
                      selectModule.resetFromForm(this.form.moduleId)
                    }
                    orderedFields.focusDefectName()
                    const selectCase = orderedFields.getSelectCaseRef()
                    if (this.form.moduleId && selectCase) {
                      selectCase.search(this.form.moduleId)
                    }
                  }
                });
              } else {
                this.cancel();
              }
            });
          }
        }
      });
    },
    /** 关闭缺陷抽屉窗口（点击遮罩 / 关闭按钮） */
    closeDefectDrawer(done) {
      this.cancel();
      if (typeof done === 'function') done();
    },
    handleByChangeHandle(members){
      // console.log(members,this.form.handleBy);
    },
    moduleChangeHandle() {
      const selectCase = this.$refs.orderedFields && this.$refs.orderedFields.getSelectCaseRef();
      selectCase && selectCase.search(this.form.moduleId);
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
    /** AI创建缺陷 */
    createDefectByAiHandle() {
      if(this.aiButtonLoading) {
        this.$message.error(this.$i18n.t('defect.ai-re-run').toString());
        return;
      }
      let startSeconds = new Date().getTime();
      if(!this.form.defectDescribe) {
        this.$message.error(this.$i18n.t('defect.describe-cannot-empty').toString());
        return;
      }
      if(!this.defectAiModelId) {
        this.$message.error(this.$i18n.t('case.model-content-not-empty').toString());
        return;
      }
      this.aiButtonLoading = true;
      let makeTitle,makeModule,makeType,makeMember,makeVersion;
      const params = {
        projectId: this.projectId,
        describe: this.form.defectDescribe,
        modelId: this.defectAiModelId != null ? String(this.defectAiModelId) : null,
        serviceType: this.defectAiServiceType || 'ollama'
      }
      if(!this.form.defectName) {
        makeTitle=makeDefectTitle(params).then(res => {
          if(!this.form.defectName && res.code==200 && res.data.title) {
            this.form.defectName = res.data.title;
            this.$message.success(strFormat(this.$i18n.t('fill-finish'),this.$i18n.t("title").toString()));
          }
        });
      }
      if(!this.form.moduleId) {
        makeModule=makeDefectModule(params).then(res => {
          if(!this.form.moduleId && res.code==200 && res.data && res.data.moduleId) {
            this.form.moduleId = res.data.moduleId;
            this.$message.success(strFormat(this.$i18n.t('fill-finish'),this.$i18n.t("module").toString()));
          }
        });
      }
      if(!this.form.defectType) {
        makeType = makeDefectType(params).then(res => {
          if(!this.form.defectType && res.code==200 && res.data.type) {
            this.form.defectType = res.data.type;
            this.$message.success(strFormat(this.$i18n.t('fill-finish'),this.$i18n.t("type").toString()));
          }
        });
      }
      if(this.defectHandleByNeedsAiFill()) {
        makeMember=makeDefectMember(params).then(res => {
          if(this.defectHandleByNeedsAiFill() && res.code==200 && res.data.memberId) {
            this.$set(this.form, 'handleBy', [res.data.memberId]);
            this.$message.success(strFormat(this.$i18n.t('fill-finish'),this.$i18n.t("handle-by").toString()));
          }
        });
      }
      if(!this.form.moduleVersion) {
        makeVersion = makeDefectVersion(params).then(res => {
          if (this.form.moduleVersion || res.code !== 200 || !res.data) {
            return;
          }
          const raw = res.data;
          const v =
            raw.version != null && String(raw.version).trim() !== ''
              ? String(raw.version).trim()
              : raw.moduleVersion != null && String(raw.moduleVersion).trim() !== ''
                ? String(raw.moduleVersion).trim()
                : null;
          if (!v) {
            return;
          }
          this.$set(this.form, 'moduleVersion', v);
          this.$message.success(strFormat(this.$i18n.t('fill-finish'), this.$i18n.t("version").toString()));
        });
      }
      if(!makeTitle && !makeModule && !makeType && !makeMember && !makeVersion) {
        this.aiButtonLoading = false;
        this.$message.error(this.$i18n.t('defect.no-fields-analyze').toString());
        return;
      }
      Promise.all([makeTitle,makeModule,makeType,makeMember,makeVersion]).then(res=>{
        const self = this;
        setTimeout(()=>{
          self.aiButtonLoading = false;
          self.$message({
            message:strFormat(self.$i18n.t('defect.analyze-finish'),Math.floor((new Date().getTime()-startSeconds)/1000)),
            type: 'success',
            duration: 5000,
          });
        },1000);
      }).catch(e=>{
        this.aiButtonLoading = false;
        this.$message.error(e);
      })
    }
  }
}
</script>

<style lang="scss" scoped>
  ::v-deep .el-drawer {
    border-left: 3px solid #ff4949;
  }
  ::v-deep .el-drawer__header {
    margin-bottom: 0px;
    overflow: visible;
  }
  ::v-deep .el-drawer__close-btn {
    display: none;
  }
  .defect-add-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-direction: row;
    overflow: visible;
    > div:last-child {
      overflow: visible;
    }
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
  .defect-add-title {
    display: inline-flex;
    justify-content: flex-start;
    align-items: center;
    flex-direction: row;
    overflow: hidden;
    gap: 10px;
    > * {
      float: left;
    }
    .el-icon-arrow-left {
      font-size: 22px;
    }
    .el-icon-arrow-left:hover {
      cursor: pointer;
      color: #909399;
    }
  }
  ::v-deep .app-container {
    .save-form-cache {
      left: -25px;
    }
    /* 无文字标签的表单项：取消复选框左移以与内容列对齐 */
    .save-cache-form-item .save-form-cache {
      left: 0;
    }
    .el-form-item--medium .el-form-item__content {
      line-height: 35px !important;
    }
    /* Element 主题里 datetimerange 默认 400px，在 el-col span=12 时会撑出抽屉 */
    .defect-plan-datetimerange.el-date-editor--datetimerange.el-input__inner {
      width: 100% !important;
      max-width: 100%;
      min-width: 0 !important;
      box-sizing: border-box;
    }
  }
</style>

<!-- 字段快捷聚焦提示徽标：通过 JS 注入到 label，需用非 scoped 全局样式 -->
<style lang="scss">
  .cat2bug-field-hint-host {
    position: relative !important;
    overflow: visible !important;
  }
  .el-form-item__label.cat2bug-field-hint-host,
  .el-form-item__content.cat2bug-field-hint-host {
    overflow: visible !important;
  }
  .cat2bug-field-hint {
    position: absolute;
    right: 2px;
    bottom: 1px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 16px;
    height: 16px;
    margin: 0;
    padding: 0 4px;
    font-family: 'SFMono-Regular', Consolas, Menlo, monospace;
    font-size: 10px;
    font-weight: 600;
    line-height: 1;
    color: #ffd54f;
    background: #2b2b2b;
    border: 1px solid #5a5a5a;
    border-radius: 3px;
    box-shadow: 0 1px 0 rgba(0, 0, 0, 0.3);
    user-select: none;
    pointer-events: none;
    z-index: 2;
    transform: translateY(15%);
  }
  /* 抽屉标题栏保存/关闭按钮：徽标锚在按钮右下角 */
  .defect-drawer-accent .defect-kbd-hint-host.el-button {
    position: relative;
    overflow: visible !important;
  }
  .defect-drawer-accent .defect-kbd-hint-host .defect-kbd-hint {
    right: 0;
    bottom: 0;
    transform: translate(50%, 50%);
    white-space: nowrap;
    color: #e8e8e8;
    border-color: #4a4a4a;
  }
  .defect-drawer-accent .defect-kbd-hint-host .defect-kbd-hint--primary {
    color: #ffffff;
    background: rgba(0, 0, 0, 0.55);
    border-color: rgba(0, 0, 0, 0.35);
  }
  /* 「保存本次选项」：徽标绝对定位在勾选框左侧，不挤占布局 */
  .defect-drawer-accent .save-form-cache-row {
    position: relative;
    display: inline-block;
    vertical-align: middle;
  }
  .defect-drawer-accent .save-cache-form-item .el-form-item__content {
    overflow: visible !important;
  }
  .defect-drawer-accent .save-form-cache-row .save-cache-field-hint {
    position: absolute;
    right: 100%;
    top: 50%;
    bottom: auto;
    transform: translateY(-50%);
    margin-right: 4px;
    flex-shrink: 0;
  }
  /* 字段聚焦高亮：仅在控件本体上加边框焦点环（不整行高亮） */
  .el-form-item.cat2bug-field-flash .el-input__inner,
  .el-form-item.cat2bug-field-flash .el-input-number .el-input__inner,
  .el-form-item.cat2bug-field-flash .el-textarea__inner,
  .el-form-item.cat2bug-field-flash .cat2bug-upload-focus-target:not(.component-upload-image-focus-target):not(.upload-file-focus-target) {
    border-color: var(--cat2bug-field-focus-color) !important;
    box-shadow: 0 0 0 2px var(--cat2bug-field-focus-ring), 0 0 8px var(--cat2bug-field-focus-glow) !important;
    animation: cat2bugFieldRing 1.6s ease forwards;
  }
  /* 文件选择器闪烁：实线外环，无荧光 glow */
  .el-form-item.cat2bug-field-flash .upload-file-focus-target {
    border-color: transparent !important;
    box-shadow: 0 0 0 1px var(--cat2bug-field-focus-color) !important;
    animation: cat2bugSwitchFieldRing 1.6s ease forwards;
  }
  /* 开关闪烁：box-shadow 外环，避免 __core 遮挡 border 中段 */
  .el-form-item.cat2bug-field-flash .el-switch {
    display: inline-flex;
    align-items: center;
    border-radius: 20px;
    padding: 0;
    border: none !important;
    box-shadow: 0 0 0 1px var(--cat2bug-field-focus-color) !important;
    animation: cat2bugSwitchFieldRing 1.6s ease forwards;
  }
  .el-form-item.cat2bug-field-flash .el-switch .el-switch__core {
    box-shadow: none !important;
  }
  .el-form-item.cat2bug-field-flash .component-upload-image-focus-target .update-button-add {
    border-color: var(--cat2bug-field-focus-color) !important;
    box-shadow: 0 0 0 2px var(--cat2bug-field-focus-ring), 0 0 8px var(--cat2bug-field-focus-glow) !important;
    animation: cat2bugFieldRing 1.6s ease forwards;
  }
  /* 成员/交付物选择器：组合键聚焦时仅外框闪动，内部 input 不重复显示焦点环 */
  .el-form-item.cat2bug-field-flash .select-project-member-input .select-project-member-search-input .el-input__inner,
  .el-form-item.cat2bug-field-flash .select-module-input .select-module-display-input .el-input__inner {
    border-color: transparent !important;
    box-shadow: none !important;
    animation: none !important;
  }
  @keyframes cat2bugFieldRing {
    0% { box-shadow: 0 0 0 0 var(--cat2bug-field-focus-ring-none); }
    12% { box-shadow: 0 0 0 2px var(--cat2bug-field-focus-glow-bright), 0 0 8px var(--cat2bug-field-focus-glow-bright); }
    70% { box-shadow: 0 0 0 2px var(--cat2bug-field-focus-ring), 0 0 8px var(--cat2bug-field-focus-glow); }
    100% { box-shadow: 0 0 0 0 var(--cat2bug-field-focus-ring-none); }
  }
  @keyframes cat2bugSwitchFieldRing {
    0% { box-shadow: 0 0 0 1px var(--cat2bug-field-focus-ring-fade); }
    12% { box-shadow: 0 0 0 1px var(--cat2bug-field-focus-color); }
    70% { box-shadow: 0 0 0 1px var(--cat2bug-field-focus-color); }
    100% { box-shadow: 0 0 0 1px var(--cat2bug-field-focus-ring-fade); }
  }
  /* 持久焦点环：成员/交付物组合选择框用 :focus-within 在获得焦点期间持续显示边框 */
  .defect-drawer-accent .el-form-item .select-project-member-input:focus-within,
  .defect-drawer-accent .el-form-item .select-module-input:focus-within {
    border-color: var(--cat2bug-field-focus-color) !important;
    box-shadow: none !important;
  }
  /* 开关聚焦：box-shadow 外环包裹整颗开关，不被 __core 遮挡 */
  .defect-drawer-accent .el-form-item .el-switch:focus-within {
    display: inline-flex;
    align-items: center;
    border-radius: 20px;
    padding: 0;
    border: none;
    outline: none;
    box-shadow: 0 0 0 1px var(--cat2bug-field-focus-color);
    background: transparent;
  }
  .defect-drawer-accent .el-form-item .el-form-item__content:has(.el-switch) {
    overflow: visible !important;
  }
  .defect-drawer-accent .el-form-item .el-switch:focus-within .el-switch__core {
    box-shadow: none !important;
  }
  html.dark .defect-drawer-accent .el-form-item .el-switch.is-checked:focus-within {
    box-shadow: 0 0 0 1px #ffffff;
  }
  html.dark .el-form-item.cat2bug-field-flash .el-switch.is-checked {
    box-shadow: 0 0 0 1px #ffffff !important;
    animation: cat2bugSwitchFieldRingChecked 1.6s ease forwards;
  }
  @keyframes cat2bugSwitchFieldRingChecked {
    0% { box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.35); }
    12% { box-shadow: 0 0 0 1px #ffffff; }
    70% { box-shadow: 0 0 0 1px #ffffff; }
    100% { box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.35); }
  }
  /* 数字输入框聚焦：与普通 input 一致的边框色 */
  .defect-drawer-accent .el-form-item .el-input-number:focus-within .el-input__inner {
    border-color: var(--cat2bug-field-focus-color) !important;
    box-shadow: none !important;
  }
  /* 文件上传：外框持久焦点环 */
  .defect-drawer-accent .el-form-item .cat2bug-upload-focus-target {
    overflow: visible;
  }
  .defect-drawer-accent .el-form-item .upload-file-focus-target:focus,
  .defect-drawer-accent .el-form-item .upload-file-focus-target:focus-within {
    border-color: transparent !important;
    box-shadow: 0 0 0 1px var(--cat2bug-field-focus-color) !important;
    outline: none;
    padding: 2px 4px;
    margin: -2px -4px;
  }
  /* 图片上传：焦点环仅落在当前键盘项（默认添加按钮），不外框整圈 */
  .defect-drawer-accent .el-form-item .component-upload-image-focus-target:focus,
  .defect-drawer-accent .el-form-item .component-upload-image-focus-target:focus-within {
    border-color: transparent !important;
    box-shadow: none !important;
    padding: 0;
    margin: 0;
  }
  .defect-drawer-accent .el-form-item .component-upload-image-focus-target .update-button.is-keyboard-focus {
    outline: 2px solid var(--cat2bug-field-focus-color) !important;
    outline-offset: 2px;
    border-color: var(--cat2bug-field-focus-color) !important;
  }
  .defect-drawer-accent .el-form-item .component-upload-image-focus-target .el-upload-list__item.is-keyboard-focus {
    outline-color: var(--cat2bug-field-focus-color) !important;
    border-color: var(--cat2bug-field-focus-color) !important;
  }
  html:not(.dark) .cat2bug-field-hint {
    color: #ffffff;
    background: #303133;
    border-color: #303133;
  }
  html:not(.dark) .defect-drawer-accent .defect-kbd-hint-host .defect-kbd-hint--primary {
    color: #ffffff;
    background: rgba(0, 0, 0, 0.55);
    border-color: rgba(0, 0, 0, 0.35);
  }
</style>
