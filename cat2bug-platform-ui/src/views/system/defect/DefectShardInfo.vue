<template>
  <div class="shard-main">
    <div v-if="!defect.defectId" class="shard-pass-form">
      <el-form ref="form" :model="params" label-width="0px">
        <el-form-item>
          <el-input v-model="params.password" show-password maxlength="4" :placeholder="$t('shard.please-enter-password')">
            <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" @click="getShard">验证</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div v-if="!defect.defectId" class="defect-info skeleton-main">
      <div class="shard-header">
      </div>
      <div class="skeleton-parent">
        <el-skeleton :rows="6" />
        <div class="skeleton-images">
          <el-skeleton-item variant="image" style="width: 150px; height: 150px;" />
          <el-skeleton-item variant="image" style="width: 150px; height: 150px;" />
        </div>
        <el-skeleton :rows="6" />
      </div>
      <div class="shard-footer">
        <div>
          <el-image class="web-qrcode" :src="require('@/assets/images/web_qrcode.png')" />
        </div>
        <div>
          <div class="row">
            <i class="el-icon-s-home"></i> {{$t('website')}}：<el-link
            href="https://www.cat2bug.com"
            target="_blank">https://www.cat2bug.com</el-link>
          </div>
          <div class="row">
            <i class="el-icon-user-solid"></i> {{$t('qq-group')}}：
            <a target="_blank" href="https://qm.qq.com/cgi-bin/qm/qr?k=vpZaR_tWTwG0lPABP9NqgX2wSSSEEaWV&jump_from=webapi&authKey=8xIzBRe5T9RoEdisRBgYWbMmCoWXIYk4VikARN8S4RA3MZT7z7FUj71ZIIvEzGzq">731462000</a>
          </div>
          <div class="row">
            <svg-icon icon-class="gitee"/>
            Gitee：<el-link
            href="https://gitee.com/cat2bug/cat2bug-platform"
            target="_blank">https://gitee.com/cat2bug/cat2bug-platform</el-link>
          </div>
          <div class="row">
            <svg-icon icon-class="github"/>GitHub：<el-link
            href="https://github.com/cat2bug/cat2bug-platform"
            target="_blank">https://github.com/cat2bug/cat2bug-platform</el-link>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="defect-info">
      <div class="shard-header">
        <h1><i class="el-icon-takeaway-box" /> {{$t('project')}}: {{defect.projectName}}</h1>
        <lang-select />
      </div>
      <div class="defect-edit-header">
        <div class="defect-edit-title">
          <h4 class="defect-edit-title-num">#{{defect.projectNum}}</h4>
          <defect-type-flag :defect="defect" />
          <defect-state-flag :defect="defect" />
  <!--        <focus-member-list-->
  <!--          v-model="defect.focusList"-->
  <!--          module-name="defect"-->
  <!--          :data-id="defect.defectId"-->
  <!--          :tooltip="false"-->
  <!--        />-->
          <h4 class="defect-edit-title-name">{{defect.defectName}}</h4>
        </div>
      </div>
      <div class="app-container defect-edit-body">
        <div class="defect-edit-body-log-first">
          <list-defect-log ref="defectLogFirst" :pageSize="1" />
        </div>
        <el-collapse v-model="activeNames">
          <el-collapse-item :title="$i18n.t('defect.base-info')" name="base">
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
                <span>{{ $t(defect.defectStateName)}}</span>
              </el-col>
            </el-row>
          </el-collapse-item>
          <el-collapse-item v-if="defect.imgUrls" :title="$i18n.t('image')" name="imgUrls">
            <el-image
              v-for="(img,index) in getUrl(defect.imgUrls)"
              :key="index"
              style="width: 100px; height: 100px"
              :src="img"
              :preview-src-list="getUrl(defect.imgUrls)"
              fit="contain"></el-image>
          </el-collapse-item>
          <el-collapse-item :title="$i18n.t('describe')" name="defectDescribe">
            <markdown-it-vue :content="defect.defectDescribe+''" />
          </el-collapse-item>
          <el-collapse-item v-if="defect.annexUrls" :title="$i18n.t('annex')" name="annexUrls">
            <div class="defect-edit-body-annex">
              <el-link type="primary" v-for="(file,index) in getUrl(defect.annexUrls)" :key="index" :href="file">{{getFileName(file)}}</el-link>
            </div>
          </el-collapse-item>
          <el-collapse-item v-if="defectCase && defectCase.caseId" :title="$i18n.t('case')" name="caseId">
            <case-card :case-model="defectCase" :state-visible="true" :step-index.sync="defect.caseStepId" :edit="false" />
          </el-collapse-item>
          <el-collapse-item :title="$i18n.t('log')" name="log">
            <list-defect-log ref="defectLog" :pageSize="10" show-comment />
          </el-collapse-item>
        </el-collapse>
      </div>
      <div class="shard-footer">
        <div>
          <el-image class="web-qrcode" :src="require('@/assets/images/web_qrcode.png')" />
        </div>
        <div>
          <div class="row">
            <i class="el-icon-s-home"></i> {{$t('website')}}：<el-link
            href="https://www.cat2bug.com"
            target="_blank">https://www.cat2bug.com</el-link>
          </div>
          <div class="row">
            <i class="el-icon-user-solid"></i> {{$t('qq-group')}}：
            <a target="_blank" href="https://qm.qq.com/cgi-bin/qm/qr?k=vpZaR_tWTwG0lPABP9NqgX2wSSSEEaWV&jump_from=webapi&authKey=8xIzBRe5T9RoEdisRBgYWbMmCoWXIYk4VikARN8S4RA3MZT7z7FUj71ZIIvEzGzq">731462000</a>
          </div>
          <div class="row">
            <svg-icon icon-class="gitee"/>
            Gitee：<el-link
            href="https://gitee.com/cat2bug/cat2bug-platform"
            target="_blank">https://gitee.com/cat2bug/cat2bug-platform</el-link>
          </div>
          <div class="row">
            <svg-icon icon-class="github"/>GitHub：<el-link
            href="https://github.com/cat2bug/cat2bug-platform"
            target="_blank">https://github.com/cat2bug/cat2bug-platform</el-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {getShardDefect} from "@/api/system/DefectShard";
import SelectProjectMember from "@/components/Project/SelectProjectMember"
import SelectModule from "@/components/Module/SelectModule"
import ImageUpload from "@/components/ImageUpload";
import ListDefectLog from "@/components/ListDefectLog";
import DefectTools from "@/components/Defect/DefectTools";
import DefectTypeFlag from "@/components/Defect/DefectTypeFlag";
import DefectStateFlag from "@/components/Defect/DefectStateFlag";
import CaseCard from "@/components/Case/CaseCard";
import MarkdownItVue from "markdown-it-vue";
import FocusMemberList from "@/components/FocusMemberList";
import LangSelect from "@/components/LangSelect";
import {getCase} from "@/api/system/case";
const I18N_LOCALE_KEY='i18n-locale'
const DEFECT_SHARD_PASSWORD_KEY = "defect-shard-password-"
export default {
  name: "DefectShardInfo",
  components: { ImageUpload, SelectProjectMember, SelectModule, ListDefectLog, DefectTools, DefectTypeFlag, DefectStateFlag, CaseCard, MarkdownItVue,FocusMemberList, LangSelect },
  data() {
    return {
      defectShardId: null,
      params: {},
      defect: {},
      activeNames: ['defectDescribe','base','imgUrls','annexUrls','caseId','log'],
      // 用例
      defectCase: {},
    }
  },
  created() {
    this.defectShardId = this.$route.query.id;
    this.refreshLang();
    if(this.defectShardId) {
      let password = this.$cache.local.get(DEFECT_SHARD_PASSWORD_KEY+this.defectShardId);
      if(password) {
        this.params.password=password;
      }
    }

    this.getShard();
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
  methods: {
    refreshLang() {
      this.params.defaultLang = this.$cache.local.get(I18N_LOCALE_KEY) || this.params.defaultLang || 'zh_CN';
    },
    getShard() {
      let self = this;
      getShardDefect(0,this.defectShardId,this.params).then(res=>{
        this.defect = res.data.defect;
        if(this.defect){
          this.getCase(this.defect.caseId);
          if(res.data.logs && res.data.logs.length>0) {
            this.$nextTick(()=>{
              self.$refs.defectLogFirst.refresh(res.data.logs);
              self.$refs.defectLog.refresh([res.data.logs[0]]);
            });
          }
        }
        if(this.params.password) {
          this.$cache.local.set(DEFECT_SHARD_PASSWORD_KEY+this.defectShardId,this.params.password);
        }
      });
    },
    // 获取用例
    getCase(caseId) {
      if(caseId) {
        getCase(caseId).then(res=>{
          this.defectCase = res.data;
        })
      } else {
        this.defectCase = {};
      }
    },
  }
}
</script>

<style lang="scss" scoped>
.shard-main {
  margin: 0px;
  padding: 0px;
  width: 100%;
  height: 100%;
}
.defect-info {
  margin: 0px;
  padding: 0px 20%;
  background-color: #F2F6FC;
  > * {
    background-color: #FFFFFF;
  }
}
.shard-header {
  width: 100%;
  height: 50px;
  background-color: #414141;
  color: #FFFFFF;
  display: inline-flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  font-size: 13px;
}
.shard-footer {
  width: 100%;
  height: 200px;
  background-color: #414141;
  color: #FFFFFF;
  display: inline-flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  padding: 10px 30px;
  > div {
    display: inline-flex;
    flex-direction: column;
    justify-content: center;
    align-items: flex-start;
    margin-right: 20px;
  }
  .web-qrcode {
    width: 130px;
    height: 130px;
  }
}
.defect-edit-header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-direction: row;
  flex-wrap: wrap;
  .defect-edit-title {
    display: inline-block;
    display: inline-flex;
    justify-content: flex-start;
    align-items: center;
    flex-direction: row;
    overflow: hidden;
    padding: 10px 20px;
    > * {
      float:left;
    }
    .defect-edit-title-name {
      flex: 1;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
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
.row {
  display: inline-flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  margin: 5px 10px;
  color: #FFFFFF;
  font-size: 15px;
  .el-link {
    color: #FFFFFF;
  }
  .el-link:hover {
    border-bottom: 1px solid #FFFFFF;
  }
  i, .svg-icon {
    margin-right: 5px;
  }
}
label {
  padding-right: 5px;
}
.shard-pass-form {
  z-index: 999;
  position: absolute;
  width: 100%;
  height: 100%;
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #000000aa;
  > * {
    border: 2px solid #DCDFE6;
    border-radius: 5px;
    padding: 40px 35px 20px 35px;
    background-color: #FFFFFF;
  }
}
.skeleton-main {
  display: inline-flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
}
.skeleton-parent {
  padding: 30px;
  flex: 1;
  > * {
    margin-bottom: 20px;
  }
  .skeleton-images {
    display: inline-flex;
    flex-direction: row;
    gap: 10px;
  }
}
::v-deep .lang{
  .dropdown-title {
    color: #FFFFFF;
  }
}
</style>
