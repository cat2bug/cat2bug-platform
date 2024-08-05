<template>
  <el-drawer
    size="65%"
    :visible.sync="visible"
    direction="rtl"
    :before-close="closeDefectDrawer">
    <template slot="title">
      <div class="defect-add-header">
        <div class="defect-add-title">
          <i class="el-icon-arrow-left" @click="cancel"></i>
          <h3>{{$t('defect.create')}}</h3>
        </div>
        <div>
          <el-button @click="cancel" icon="el-icon-close" size="small">{{$t('close')}}</el-button>
          <el-button type="primary" icon="el-icon-finished" @click="submitForm" size="small">{{$t('defect.save')}}</el-button>
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
                  :key="type.value"
                  :label="$t(type.value)"
                  :value="type.value"
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
            <select-module v-model="form.moduleId" :project-id="projectId" @input="moduleChangeHandle"/>
          </el-form-item>
        </el-col>
        <el-col :span="12">
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
        <el-col :span="24">
        <el-form-item :label="$t('case')" prop="caseId">
          <select-case ref="selectCase" v-model="form.caseId" :module-id="form.moduleId" :step-index="form.caseStepId" @step-change="stepChangeHandle" />
        </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('describe')" prop="defectDescribe">
            <cat2-bug-textarea
              ref="cat2bugTextarea"
              :name="$t('describe').toString()"
              :placeholder="$t('defect.enter-markdown-describe').toString()"
              :tools = "describeTools"
              v-model="form.defectDescribe"
              maxlength="65536"
              rows="8"
              show-word-limit
              show-tools
            >
              <template v-slot:tools>
                <el-tooltip class="item" effect="dark" :content="$t('defect.ai-filling-in')" placement="top">
                  <el-button :handle="aiButtonLoading?'true':'false'" class="cat2-bug-textarea-button" type="text" @click="createDefectByAiHandle"><svg-icon icon-class="robot"></svg-icon><span v-show="aiButtonLoading">分析中...</span></el-button>
                </el-tooltip>
              </template>
            </cat2-bug-textarea>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('image')" prop="imgUrls">
            <image-upload v-model="form.imgUrls" :limit="9"></image-upload>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('annex')" prop="annexUrls">
            <file-upload v-model="form.annexUrls" :limit="9" :file-type="[]"/>
          </el-form-item>
        </el-col>
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
import {
  makeDefect,
  makeDefectMember,
  makeDefectModule,
  makeDefectTitle,
  makeDefectType,
  makeDefectVersion
} from "@/api/ai/AiDefect";
import {strFormat} from "@/utils";

export default {
  name: "AddDefect",
  dicts: ['defect_level'],
  components: { ImageUpload, SelectProjectMember, SelectModule,SelectCase,Cat2BugTextarea },
  data() {
    return {
      // 计划时间范围
      planTimeRange:[],
      // AI按钮加载是否显示
      aiButtonLoading: false,
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
      this.$nextTick(() => {
        this.$refs.cat2bugTextarea.focus();
      });
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
      this.planTimeRange=[];
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
          if(this.planTimeRange.length>1) {
            this.form.planStartTime = this.planTimeRange[0];
            this.form.planEndTime = this.planTimeRange[1];
          }
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
      // console.log(members,this.form.handleBy);
    },
    moduleChangeHandle() {
      this.$refs.selectCase.search(this.form.moduleId);
    },
    stepChangeHandle(index){
      this.form.caseStepId = index;
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
      this.aiButtonLoading = true;
      let makeTitle,makeModule,makeType,makeMember,makeVersion;
      const params = {
        projectId: this.projectId,
        describe: this.form.defectDescribe
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
      if(!this.form.handleBy) {
        makeMember=makeDefectMember(params).then(res => {
          if(!this.form.handleBy && res.code==200 && res.data.memberId) {
            this.form.handleBy = [res.data.memberId];
            this.$message.success(strFormat(this.$i18n.t('fill-finish'),this.$i18n.t("handle-by").toString()));
          }
        });
      }
      if(!this.form.moduleVersion) {
        makeVersion = makeDefectVersion(params).then(res => {
          if(!this.form.moduleVersion && res.code==200) {
            this.form.moduleVersion = res.data.version;
            this.$message.success(strFormat(this.$i18n.t('fill-finish'),this.$i18n.t("version").toString()));
          }
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
    .el-form-item--medium .el-form-item__content {
      line-height: 35px !important;
    }
  }
</style>
