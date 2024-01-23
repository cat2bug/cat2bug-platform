<template>
  <el-dialog
    :append-to-body="true"
    size="45%"
    :title="title"
    :visible.sync="visible">
    <div class="app-container defect-edit-body">
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="24">
            <el-form-item :label="$t('title')" prop="defectName">
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
            <el-form-item :label="$t('handle-by')" prop="handleBy">
              <select-project-member v-model="form.handleBy" :project-id="projectId"  />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('module')" prop="moduleId">
              <select-module size="small" v-model="form.moduleId" :project-id="projectId"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('version')" prop="moduleVersion">
              <el-input v-model="form.moduleVersion" :placeholder="$t('defect.enter-version')" maxlength="128" style="max-width: 300px;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('level')" prop="defectLevel">
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
        </el-row>
        <el-form-item :label="$t('describe')" prop="defectDescribe">
          <el-input
            type="textarea"
            :placeholder="$t('enter-content')"
            v-model="form.defectDescribe"
            maxlength="5000"
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
import {addDefect, configDefect, getDefect, updateDefect} from "@/api/system/defect";
import SelectProjectMember from "@/components/SelectProjectMember"
import SelectModule from "@/components/SelectModule"
import ImageUpload from "@/components/ImageUpload";
import ListDefectLog from "@/components/ListDefectLog";
import DefectTools from "@/components/DefectTools";
import DefectTypeFlag from "@/components/DefectTypeFlag";
export default {
  name: "EditDefect",
  dicts: ['defect_level'],
  components: { ImageUpload, SelectProjectMember, SelectModule, ListDefectLog, DefectTools, DefectTypeFlag },
  data() {
    return {
      title: this.$i18n.t('defect.modify'),
      defectId: null,
      config: {},
      // 显示窗口
      visible: false,
      // 缺陷对象
      defect:{},
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
        defectDescribe: [
          { required: true, message: this.$i18n.t('defect.describe-cannot-empty'), trigger: "input" }
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
      this.activeNames = ['base','log']
      getDefect(defectId).then(res=>{
        this.form = res.data;
        if(this.defect.defectDescribe) {
          this.activeNames.push('defectDescribe');
        }
        if(this.defect.imgUrls && this.defect.imgUrls.length>0) {
          this.activeNames.push('imgUrls');
        }
        if(this.defect.annexUrls && this.defect.annexUrls.length>0) {
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
          updateDefect(this.form).then(res => {
            this.$modal.msgSuccess(this.$i18n.t('modify-success'));
            this.visible = false;
            this.$emit('log',res.data)
          });
        }
      });
    },
  }
}
</script>

<style lang="scss" scoped>
.dialog-footer {
  text-align: right;
}
</style>
