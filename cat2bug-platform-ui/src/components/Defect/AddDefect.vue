<template>
  <el-drawer
    size="50%"
    :visible.sync="visible"
    direction="rtl"
    :before-close="closeDefectDrawer">
    <template slot="title">
      <div class="defect-add-header">
        <h3>{{$t('defect.create')}}</h3>
        <div>
          <el-button @click="cancel" icon="el-icon-close" size="mini">{{$t('close')}}</el-button>
          <el-button type="primary" icon="el-icon-finished" @click="submitForm" size="mini">{{$t('create')}}</el-button>
        </div>
      </div>
    </template>
    <div class="app-container">
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item :label="$t('title')" prop="defectName">
          <el-input v-model="form.defectName" :placeholder="$t('defect.enter-name')" maxlength="128" />
        </el-form-item>
        <el-row>
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
        <el-row>
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
        </el-row>
        <el-form-item :label="$t('module')" prop="moduleId">
          <select-module v-model="form.moduleId" :project-id="projectId" @input="moduleChangeHandle"/>
        </el-form-item>
        <el-form-item :label="$t('case')" prop="caseId">
          <select-case ref="selectCase" v-model="form.caseId" :module-id="form.moduleId" :step-index="form.caseStepId" @step-change="stepChangeHandle" />
        </el-form-item>
        <el-form-item :label="$t('describe')" prop="defectDescribe">
<!--          <el-input-->
<!--            type="textarea"-->
<!--            :placeholder="$t('enter-content')"-->
<!--            v-model="form.defectDescribe"-->
<!--            maxlength="65536"-->
<!--            rows="8"-->
<!--            show-word-limit-->
<!--          >-->
<!--          </el-input>-->
          <cat2-bug-textarea
            :name="$t('describe').toString()"
            :placeholder="$t('enter-content').toString()"
            :tools = "describeTools"
            v-model="form.defectDescribe"
            maxlength="65536"
            rows="8"
            show-word-limit
            show-tools
          />
        </el-form-item>
        <el-form-item :label="$t('image')" prop="imgUrls">
          <image-upload v-model="form.imgUrls" :limit="9"></image-upload>
        </el-form-item>
        <el-form-item :label="$t('annex')" prop="annexUrls">
          <file-upload v-model="form.annexUrls" :limit="9" :file-type="[]"/>
        </el-form-item>
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
import SelectProjectMember from "@/components/Project/SelectProjectMember"
import SelectModule from "@/components/Module/SelectModule"
import ImageUpload from "@/components/ImageUpload";
import SelectCase from "@/components/Case/SelectCase";
import Cat2BugTextarea from "@/components/Cat2BugTextarea";
import {upload} from "@/api/common/upload";

export default {
  name: "AddDefect",
  dicts: ['defect_level'],
  components: { ImageUpload, SelectProjectMember, SelectModule,SelectCase,Cat2BugTextarea },
  data() {
    return {
      // 显示窗口
      visible: false,
      config: {},
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
      },
      describeTools:[{
        name: 'defect.ai-filling-in',
        icon: 'robot',
        method: this.createDefectByAiHandle
      },]
    }
  },
  props: {
    projectId: {
      type: Number,
      default: null
    },
  },
  mounted() {
    setTimeout(()=>{
      this.getDefectConfig();
    },0)

  },
  methods:{
    getDefectConfig() {
      configDefect().then(res=>{
        this.config = res.data;
      })
    },
    open(data) {
      this.reset();
      if(data){
        this.form.imgUrls = data.imgUrls||null
      }
      this.visible = true;
    },
    openByCase(c) {
      this.open();
      this.form.caseId = c.caseId;
      this.form.moduleId = c.moduleId;
    },
    // 取消按钮
    cancel(isReset) {
      this.visible = false;
      if(isReset){
        this.reset();
      }
    },
    // 表单重置
    reset() {
      this.form = {
        defectId: null,
        defectType: null,
        defectName: null,
        defectDescribe: null,
        annexUrls: null,
        imgUrls: null,
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
          this.form.projectId = this.projectId;
          if (this.form.defectId != null) {
            updateDefect(this.form).then(res => {
              this.$modal.msgSuccess("修改成功");
              this.visible = false;
              this.$emit('added',res)
            });
          } else {
            addDefect(this.form).then(res => {
              this.$modal.msgSuccess("新增成功");
              this.visible = false;
              this.$emit('added',res)
            });
          }
        }
      });
    },
    /** 关闭缺陷抽屉窗口 */
    closeDefectDrawer(done) {
      // done();
    },
    handleByChangeHandle(members){
      console.log(members,this.form.handleBy);
    },
    moduleChangeHandle() {
      this.$refs.selectCase.search(this.form.moduleId);
    },
    stepChangeHandle(index){
      this.form.caseStepId = index;
    },
    /** AI创建缺陷 */
    createDefectByAiHandle() {
      alert('dd')
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
  }
  ::v-deep .el-drawer__close-btn {
    display: none;
  }
  .defect-add-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-direction: row;
  }
</style>
