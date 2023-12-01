<template>
  <el-drawer
    size="45%"
    :visible.sync="visible"
    direction="rtl"
    :before-close="closeDefectDrawer">
    <template slot="title">
      <div class="defect-edit-header">
        <div class="defect-edit-title">
          <i class="el-icon-arrow-left" @click="cancel"></i>
          <h4 class="defect-edit-title-num">#{{defect.projectNum}}</h4>
          <defect-type-flag :defect="defect" />
          <h4 class="defect-edit-title-name">{{defect.defectName}}</h4>
        </div>
        <div>
          <defect-tools :defect="defect" @log="logHandle"></defect-tools>
        </div>
      </div>
    </template>
    <div class="app-container defect-edit-body">
      <div class="defect-edit-body-log-first">
        <list-defect-log ref="defectLogFirst" :pageSize="1" />
      </div>
      <el-collapse v-model="activeNames">
        <el-collapse-item :title="$i18n.t('describe')" name="defectDescribe">
          <div>
            {{defect.defectDescribe}}
          </div>
        </el-collapse-item>
        <el-collapse-item :title="$i18n.t('team.base-info')" name="base">
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
        </el-collapse-item>
        <el-collapse-item :title="$i18n.t('image')" name="imgUrls">
          <el-image
            v-for="(img,index) in getUrl(defect.imgUrls)"
            :key="index"
            style="width: 100px; height: 100px"
            :src="img"
            :preview-src-list="getUrl(defect.imgUrls)"
            fit="contain"></el-image>
        </el-collapse-item>
        <el-collapse-item :title="$i18n.t('annex')" name="annexUrls">
          <div class="defect-edit-body-annex">
            <el-link type="primary" v-for="(file,index) in getUrl(defect.annexUrls)" :key="index" :href="file">{{getFileName(file)}}</el-link>
          </div>
        </el-collapse-item>
        <el-collapse-item :title="$i18n.t('log')" name="log">
          <list-defect-log ref="defectLog" :pageSize="10" />
        </el-collapse-item>
      </el-collapse>
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
      activeNames: [],
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
      this.activeNames = ['base','log']
      getDefect(defectId).then(res=>{
        this.defect = res.data;
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
      this.getDefectInfo(defectId);
      this.visible = true;
      this.$nextTick(()=>{
        this.$refs.defectLogFirst.open(defectId);
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
      this.activeNames = ['base','log']
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
    handleByChangeHandle(members) {
      console.log(members, this.form.handleBy);
    },
    logHandle(log) {
      this.$refs.defectLog.addLog(log);
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
      justify-content: flex-start;
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
    .defect-edit-body-log-first {
      border:#E4E7ED 1px solid;
      border-radius: 5px;
      padding: 15px 15px 0px 15px;
      margin-bottom: 10px;
    }
    ::v-deep .el-collapse {
      border-width: 0px;
      .el-collapse-item:last-child {
        .el-collapse-item__wrap {
          border-width: 0px;
        }
      }
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
          justify-content: flex-start;
          margin-right: 10px;
          text-align: right;
        }
      }
    }
    .defect-edit-body-annex {
      display: flex;
      flex-direction: column;
      justify-content: flex-start;
      align-items: flex-start;
    }
  }
  ::v-deep .el-drawer__close-btn {
    display: none;
  }

</style>
