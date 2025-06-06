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
              <el-select v-model="form.defectType" :placeholder="$t('defect.select-type')">
                <el-option
                  v-for="type in config.types"
                  :key="type.key"
                  :label="$t(type.value)"
                  :value="type.key"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('priority')" prop="defectLevel">
              <el-select v-model="form.defectLevel" :placeholder="$t('defect.select-level')">
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
          <el-input
            type="textarea"
            :placeholder="$t('enter-content')"
            v-model="form.defectDescribe"
            maxlength="65536"
            rows="8"
            show-word-limit
          >
          </el-input>
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
import ListDefectLog from "@/components/ListDefectLog";
import DefectTools from "@/components/Defect/DefectTools";
import DefectTypeFlag from "@/components/Defect/DefectTypeFlag";
import SelectCase from "@/components/Case/SelectCase";

export default {
  name: "EditDefect",
  dicts: ['defect_level'],
  components: { ImageUpload, SelectProjectMember, SelectModule, ListDefectLog, DefectTools, DefectTypeFlag, SelectCase },
  data() {
    return {
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
        defectType: [
          { required: true, message: this.$i18n.t('defect.defect-type-cannot-empty'), trigger: "change" }
        ],
        handleBy: [
          { required: true, message: this.$i18n.t('defect.handle-by-cannot-empty'), trigger: "input" }
        ],
        defectName: [
          { required: true, message: this.$i18n.t('defect.defect-name-cannot-empty'), trigger: "input" }
        ],
        defectLevel: [
          { required: true, message: this.$i18n.t('defect.defect-level-cannot-empty'), trigger: "input" }
        ],
      }
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
      const self = this;
      this.activeNames = ['base','log']
      getDefect(defectId).then(res=>{
        this.form = res.data;
        if(this.form.planStartTime) {
          this.planTimeRange.push(this.form.planStartTime);
        }
        if(this.form.planEndTime) {
          this.planTimeRange.push(this.form.planEndTime);
        }
        if(this.form.defectDescribe) {
          this.activeNames.push('defectDescribe');
        }
        if(this.form.imgUrls && this.form.imgUrls.length>0) {
          this.activeNames.push('imgUrls');
        }
        if(this.form.annexUrls && this.form.annexUrls.length>0) {
          this.activeNames.push('annexUrls');
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
      this.activeNames = ['base','log']
      this.resetForm("form");
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
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
    }
  }
}
</script>

<style lang="scss" scoped>
.dialog-footer {
  text-align: right;
}

.selectTime .el-date-editor--datetimerange.el-input__inner{
  width: 100%;
}

</style>
