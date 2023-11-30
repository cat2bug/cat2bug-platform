<template>
  <el-drawer
    size="45%"
    :visible.sync="visible"
    direction="rtl"
    :before-close="closeDefectDrawer">
    <template slot="title">
      <div class="defect-edit-header">
        <div class="defect-edit-title">
          <i class="el-icon-arrow-left"></i>
          <h4 class="defect-edit-title-num">#{{defect.projectNum}}</h4>
          <defect-type-flag :defect="defect" />
          <h4 class="defect-edit-title-name">{{defect.defectName}}</h4>
        </div>
        <div>
          <defect-tools :defect="defect"></defect-tools>
        </div>
      </div>
    </template>
    <div class="app-container defect-edit-body">
      <div>
      </div>
      <div>
        <h5>{{$t('describe')}}</h5>
        <div>
          {{defect.defectDescribe}}
        </div>
      </div>
      <el-row class="defect-edit-body-base" :gutter="20">
        <el-col :span="12">
          <label>{{$t('project')}}:</label>
          <span>{{defect.projectName}}</span>
        </el-col>
        <el-col :span="12">
          <label>{{$t('module')}}:</label>
          <span>{{defect.moduleName}}</span>
        </el-col>
        <el-col :span="12">
          <label>{{$t('version')}}:</label>
          <span>{{defect.moduleVersion}}</span>
        </el-col>
        <el-col :span="12">
          <label>{{$t('state')}}:</label>
          <span>{{defect.defectStateName}}</span>
        </el-col>
      </el-row>
      <div>
        <h5>{{$t('image')}}</h5>
        <el-image
          v-for="(img,index) in getUrl(defect.imgUrls)"
          :key="index"
          style="width: 100px; height: 100px"
          :src="img"
          :preview-src-list="getUrl(defect.imgUrls)"
          fit="contain"></el-image>
      </div>
      <div class="defect-edit-body-annex">
        <h5>{{$t('annex')}}</h5>
        <el-link type="primary" v-for="(file,index) in getUrl(defect.annexUrls)" :key="index" :href="file">{{getFileName(file)}}</el-link>
      </div>
      <div>
        <h5>{{$t('log')}}</h5>
        <list-defect-log ref="defectLog" />
      </div>
      <div slot="footer" class="dialog-footer"></div>
    </div>
  </el-drawer>
</template>

<script>
import {addDefect, getDefect, updateDefect} from "@/api/system/defect";
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
    // 获取缺陷信息
    getDefectInfo(defectId) {
      getDefect(defectId).then(res=>{
        this.defect = res.data;
      });
    },
    // 打开操作
    open(defectId) {
      this.reset();
      this.getDefectInfo(defectId);
      this.visible = true;
      this.$nextTick(()=>{
        this.$refs.defectLog.open(defectId);
      });
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
      done();
    },
    handleByChangeHandle(members){
      console.log(members,this.form.handleBy);
    }
  }
}
</script>

<style lang="scss" scoped>
  ::v-deep .el-drawer__header {
    margin-bottom: 0px;
  }
  .defect-edit-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-direction: row;
    .defect-edit-title {
      display: inline-flex;
      justify-content: start;
      align-items: center;
      flex-direction: row;
      .el-icon-arrow-left {
        font-size: 22px;
      }
      .el-icon-arrow-left:hover {
        cursor: pointer;
        color: #909399;
      }
      .defect-edit-title-num, .defect-edit-title-name {
        font-size: 20px;
        color: #303133;
        font-weight: 500;
        margin-top: 10px;
        margin-bottom: 10px;
      }
      > * {
        margin-right: 10px;
      }
    }
  }
  .defect-edit-body {
    > div {
      width: 100%;
      border-top: 1px #F2F6FC solid;
      padding-top: 15px;
      padding-bottom: 20px;
    }
    > div:last-child {
      border-top-width: 0px;
    }
    h5 {
      font-size: 18px;
      color: #303133;
      margin: 10px 0px;
    }
    .defect-edit-body-base {
      .el-col {
        margin-bottom: 20px;
        label {
          font-size: 14px;
          color: #303133;
          width: 80px;
          display: inline-block;
          margin-right: 10px;
          text-align: right;
        }
      }
    }
    .defect-edit-body-annex {
      display: flex;
      flex-direction: column;
      justify-content: start;
      align-items: start;
    }
  }
  ::v-deep .el-drawer__close-btn {
    display: none;
  }

</style>
