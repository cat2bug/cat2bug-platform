<template>
  <div class="case-card">
    <div class="case-card-body" v-if="caseModel && caseModel.caseId">
      <div class="case-card-tools" v-if="toolsVisible(caseModel)">
        <el-popover
          placement="left-start"
          trigger="hover">
          <div style="display: inline-flex; flex-direction: row; gap: 5px; max-width: 900px">
            <el-image
              v-for="(img,index) in list(caseModel.imgUrls)"
              :key="index"
              style="width: 100px; height: 100px"
              :src="img"
              class="image-click"
              :preview-src-list="list(caseModel.imgUrls)"
              fit="cover"></el-image>
          </div>
          <el-badge :value="num(caseModel.imgUrls)" class="item" slot="reference" v-if="num(caseModel.imgUrls)">
            <el-button icon="el-icon-picture-outline" size="mini"></el-button>
          </el-badge>
        </el-popover>
        <el-popover
          placement="left-start"
          trigger="hover">
          <div style="display: inline-flex; flex-direction: column; align-items: start;">
            <cat2-bug-text type="down" :content="file" :tooltip="file" v-for="(file,index) in list(caseModel.annexUrls)" :key="index"/>
          </div>
          <el-badge :value="num(caseModel.annexUrls)" class="item" slot="reference" v-if="num(caseModel.annexUrls)">
            <el-button icon="el-icon-paperclip" size="mini"></el-button>
          </el-badge>
        </el-popover>
      </div>
      <div class="case-card-content">
        <div class="case-card-header">
          <div class="row">
            <label>{{ $t('title') }}</label><span>{{ caseModel.caseName }}</span>
          </div>
          <div class="row">
            <label>{{ $t('preconditions') }}</label><span>{{ caseModel.casePreconditions }}</span>
          </div>
          <div class="row">
            <label>{{ $t('expect') }}</label><span>{{ caseModel.caseExpect }}</span>
          </div>
        </div>
        <div class="row footer">
          <label>{{ $t('step') }}</label><case-step ref="caseStep" class="step" :case-steps="caseModel.caseStep" v-bind="$attrs" v-on="$listeners" />
        </div>
      </div>
    </div>
    <div v-else class="case-card-empty">
      <span>{{ $t('empty-data') }}</span>
    </div>

  </div>
</template>

<script>
import CaseStep from "@/components/Case/CaseStep";
import Cat2BugText from "@/components/Cat2BugText";

export default {
  name: "CaseCard",
  components: {CaseStep, Cat2BugText},
  model: {
    prop: 'stepIndex',
    event:'change'
  },
  data() {
    return {
    }
  },
  props: {
    caseModel: {
      type: Object,
      default: {}
    },
  },
  computed: {
    list: function () {
      return function (str) {
        return str? str.split(',').map(i => process.env.VUE_APP_BASE_API + i):[];
      }
    },
    num: function () {
      return function (strArray) {
        return strArray? strArray.split(',').length:0;
      }
    },
    toolsVisible: function () {
      return function (model) {
        return model && model.imgUrls && model.annexUrls && (this.num(model.imgUrls) + this.num(model.annexUrls))>0;
      }
    },
    getFileName: function () {
      return function (url) {
        if(!url) return null;
        let arr = url.split('\/');
        return arr[arr.length-1];
      }
    },
  },
  methods: {
    /** 下载附件操作 */
    handleDown(event, file) {
      const a = document.createElement("a");
      const e = new MouseEvent("click");
      a.href = file;
      a.dispatchEvent(e);
      event.stopPropagation();
    },
  }
}
</script>

<style lang="scss" scoped>
.case-card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 10px 10px 5px 10px;
  .case-card-body {
    width: 100%;
    display: flex;
    flex-direction: row;
  }
  .case-card-tools {
    display: flex;
    flex-direction: column;
    padding-right: 10px;
    gap: 15px;
    > span {
      flex: 0;
    }
    .el-button {
      padding: 5px;
      margin: 0px;
    }
    .el-badge > ::v-deep.el-badge__content {
      font-size: 0.6rem;
      height: 15px;
      width: 15px;
      line-height: 13px;
      padding: 0px;
    }
  }
  .case-card-content {
    flex: 1;
  }
  .case-card-header {
    line-height: 30px;
    width: 100%;
  }
  .case-card-empty {
    width: 100%;
    height: 50px;
    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
    background-color: #fbfdff;
    border-radius: 5px;
    border: 1px dashed #c0ccdc;
    > span {
      flex: none;
      font-size: 16px;
    }
  }
  label {
    min-width: 60px;
    text-align: right;
    margin-right: 10px;
  }
  label {
    font-size: 12px;
    height: 15px;
    line-height: 15px;
  }
  span {
    min-height: 15px;
    line-height: 15px;
    flex: 1;
  }
  .footer {
    width: 100%;
    padding-top: 5px;
    margin-top: 5px;
    border-top: 1px dashed #E4E7ED;
  }
}
.row {
  display: flex;
  flex-direction: row;
  width: 100%;
}
.image-click {
  border-radius: 5px;
  border: 1px solid #F2F6FC;
}
.image-click:hover {
  scale: 1.1;
  z-index: 999;
  border: 1px solid #DCDFE6;
  -webkit-box-shadow: 0 10px 20px rgba(0, 0, 0, 0.3);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.3);
}
</style>
