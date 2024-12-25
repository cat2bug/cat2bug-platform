<template>
  <div class="case-demand" ref="caseList">
    <div class="col" v-if="caseList.length>0">
      <h3><svg-icon icon-class="case"></svg-icon>测试用例列表</h3>
      <span><i class="el-icon-info"></i>以下内容可以直接编辑修改</span>
      <div class="case-demand-row" v-for="(d,index) in caseList" :key="index">
        <div class="case-demand-row-name">
          <el-checkbox :label="(index + 1) + '.'" v-model="d.checked"/>
          <el-input v-model="d.caseName" />
        </div>
        <div class="row">
          <label>等级</label>
          <el-input v-model="d.caseLevel" />
        </div>
        <div class="row">
          <label>前提条件</label>
          <el-input v-model="d.casePreconditions" />
        </div>
        <div class="row">
          <label>测试预期</label>
          <el-input v-model="d.caseExpect" />
        </div>
        <div class="row">
          <label>测试数据</label>
          <el-input v-model="d.testData" />
        </div>
        <div class="row">
          <div class="label">
            <label>测试步骤</label>
            <div class="form-item-case-form-step">
              <el-radio-group v-model="d.caseStepSwitchType">
                <el-radio-button label=""><svg-icon icon-class="option" /></el-radio-button>
                <el-radio-button label="list"><svg-icon icon-class="list2" /></el-radio-button>
                <el-radio-button label="input"><svg-icon icon-class="textarea" /></el-radio-button>
              </el-radio-group>
            </div>
          </div>
          <case-step-panel class="case-step-panel" ref="caseStepPanel" :key="'caseStepPanel'+index" v-model="d.caseStep" :panel-type="d.caseStepSwitchType" />
        </div>
      </div>
      <div class="row">
<!--        <el-button class="case-button" type="warning" @click="handleClick">导出Excel</el-button>-->
        <el-button class="case-button" type="primary" @click="handleImportClick">批量导入系统</el-button>
      </div>
    </div>
    <el-empty v-else description="没有找到数据" :image-size="50"></el-empty>
  </div>
</template>

<script>
import {makeCaseList, makeCaseMind} from "@/api/ai/AiCase2";
import Label from "@/components/Cat2BugStatistic/Components/Label";
import CaseStepPanel from "@/components/Case/CaseStepPanel";
import CaseForm from "@/components/Case/CaseForm";
import {batchAddCase} from "@/api/system/case";
import {Loading} from "element-ui";

export default {
  name: "CaseList",
  components: {Label, CaseStepPanel, CaseForm},
  data() {
    return {
      loading: false,
      caseStepSwitchType: '',
      caseList: [],
    }
  },
  props: {
    data: {
      type: Array,
      default: ()=>[]
    }
  },
  computed: {
    projectId() {
      return this.$store.state.user.config.currentProjectId
    },
  },
  mounted() {
    this.getCaseList(this.data);
  },
  methods: {
    getCaseList(data) {
      const self = this;
      const loadingInstance = Loading.service({
        target: this.$refs.caseList,
        text: '测试用例分析中,请耐心等待...',
        fullscreen: false,
      });
      const query = {
        query: JSON.stringify(data)
      }
      makeCaseList(query).then(res=>{
        this.loading = false;
        this.caseList = res.rows.map(d=>{
          d.checked = true;
          d.caseStepSwitchType = 'list';
          d.testData = d.testData?JSON.stringify(d.testData):'';
          d.projectId = this.projectId;
          return d;
        });
        this.$nextTick(()=>{
          loadingInstance.close();
          this.caseList.forEach((c,index)=>{
            self.$refs['caseStepPanel'][index].reset();
          })

          this.$emit('change');
        })
      }).catch(e=>{
        loadingInstance.close();
      })
    },
    handleImportClick() {
      let importCaseList = this.caseList.filter(c=>c.checked);
      batchAddCase(importCaseList).then(res=>{
        if(importCaseList.length == res.data.length){
          for(let i=0;i<importCaseList.length;i++){
            importCaseList[i].caseId = res.data[i].caseId;
            this.$set(importCaseList[i],'isImport',true);
          }
        }
        this.$modal.msgSuccess(this.$i18n.t('import.success'));
        this.$emit('submit', 'CaseList', this.caseList.filter(d=>d.checked));
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.case-demand {
  width: 100%;
  min-height: 100px;
  h3 {
    color: #409EFF;
    ::v-deep svg {
      margin-right: 5px;
    }
  }
  span {
    font-size: 0.8rem;
    color: #b4bccc;
    padding-bottom: 10px;
  }
}
.case-demand-row {
  display: inline-flex;
  flex-direction: column;
  width: 100%;
  gap: 5px;
  .case-demand-row-name {
    display: inline-flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
    font-size: 1.1rem;
    font-weight: 500;
    gap: 0px;
    > .el-input {
      flex: 1;
      ::v-deep input {
        font-size: 1rem;
        font-weight: 500;
        color: #606266;
      }
    }
  }
  ::v-deep input {
    border: 0px;
    font-size: 0.8rem;
    color: #909399;
    height: 16px;
    line-height: 14px;
  }
  padding-bottom: 15px;
}
.col {
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  width: 100%;
}
.row {
  display: inline-flex;
  flex-direction: row;
  justify-content: center;
  align-items: flex-start;
  width: 100%;
  > label {
    flex-shrink: 0;
    font-size: 0.8rem;
    padding-right: 10px;
  }
  > .el-input {
    flex: 1;
  }
}
.case-button {
  width: 30%;
  height: 50px;
  margin-top: 20px;
  font-size: 1.2rem;
}
.label {
  display:flex;
  flex-direction:column;
  flex-shrink: 0;
  font-size: 0.8rem;
  padding-right: 10px;
  height: 100%;
  justify-content: flex-start;
  align-items: flex-start;
}
.form-item-case-form-step {
  display:flex;
  flex-direction:row;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
  margin-top: 5px;
  ::v-deep .el-radio-button__inner {
    padding: 0px;
  }
}
.case-step-panel {
  padding-left: 15px;
}
</style>
