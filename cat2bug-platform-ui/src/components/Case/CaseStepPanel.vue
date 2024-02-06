<template>
  <div class="case-step">
    <draggable v-show="panelType!='input'" v-model="caseStepList" @change="updatePanelHandle">
      <div v-for="(step,index) in caseStepList" :key="index" class="case-step-row">
        <label>{{index+1}}</label>
        <el-input type="textarea" v-model="step.stepDescribe" :placeholder="$t('case.please-enter-step-describe')" maxlength="128" rows="1" @input="caseStepListChangeHandle($event,caseStepList)" />
        <el-input type="textarea" v-model="step.stepExpect" :placeholder="$t('case.please-enter-step-expected')" maxlength="128" rows="1" @input="caseStepListChangeHandle($event,caseStepList)" />
        <div class="case-step-row-tools">
          <el-link :step="index" type="danger" :underline="false" @click="removeStepHandle(index)" v-show="caseStep.length>1"><i class="el-icon-error el-icon--right"></i></el-link>
          <el-link type="success" :underline="false" @click="addStepHandle(index)"><i class="el-icon-circle-plus el-icon--right"></i></el-link>
        </div>
      </div>
    </draggable>
    <el-input class="case-step-script-input" v-show="panelType!='list'" type="textarea" v-model="stepScript" :placeholder="$t('case.please-enter-step-script')" maxlength="10000" :autosize="{ minRows: 4, maxRows: 10 }" show-word-limit @input="scriptChangeHandle" />
    <el-button class="button-full" type="success" size="mini" plain @click="addStepHandle(undefined)">{{ $t('case.add-step') }}</el-button>
  </div>
</template>

<script>
import draggable from 'vuedraggable'

export default {
  name: "CaseStepPanel",
  components: {draggable},
  model: {
    prop: 'caseStep',
    event: 'change'
  },
  data() {
    return {
      stepScript: null,
      caseStepList: this.caseStep,
    }
  },
  props: {
    caseStep: {
      type: Array,
      default: ()=>[{}]
    },
    /** 面板的显示类型：null=全部；list=列表；input=输入框 */
    panelType: {
      type: String,
      default: ''
    }
  },
  watch: {
    caseStep: function (n) {
      this.caseStepListChangeHandle(null, n);
    }
  },
  methods: {
    reset() {
      this.caseStepList=this.caseStep;
      this.caseStepListChangeHandle({}, this.caseStep);
    },
    addStepHandle(index) {
      if(index!=undefined){
        this.caseStepList.splice(index,0,{});
      } else {
        this.caseStepList.push({});
      }
      this.caseStepListChangeHandle({},this.caseStepList);
      this.$emit('change',this.caseStepList);
    },
    removeStepHandle(index) {
      this.caseStepList.splice(index,1);
      this.caseStepListChangeHandle({},this.caseStepList);
      this.$emit('change',this.caseStepList);
    },
    updatePanelHandle() {
      this.$emit('change',this.caseStepList);
    },
    caseStepListChangeHandle(e,list) {
      if(e) {
        this.stepScript = list?list.filter(s=>s.stepDescribe || s.stepExpect).map(s=> {
          return (s.stepDescribe?s.stepDescribe:'')+'---'+(s.stepExpect?s.stepExpect:'');
        }).join('\n'):'';
      }
    },
    scriptChangeHandle() {
      this.caseStepList = (this.stepScript||"").split('\n').filter(r=>r).map(r=>{
        let cols = r.split('---');
        return {
          stepDescribe: cols[0],
          stepExpect: cols[1]
        }
      });
      if(this.caseStepList.length==0){
        this.caseStepList=[{}];
      }
      this.$emit('change',this.caseStepList);
    }
  }
}
</script>

<style lang="scss" scoped>
.case-step {
  width: 100%;
  .case-step-script-input {
    margin-top: 10px;
  }
}
.case-step-row {
  display: flex;
  border-radius: 0px;
  flex-direction: row;
  align-items: center;
  > * {
    margin: 0px 5px;
  }
  > label {
    border-radius: 100%;
    background-color: #13ce66;
    width: 45px;
    height: 20px;
    line-height: 20px;
    font-size: 12px;
    text-align: center;
    color: #FFFFFF;
  }
  > ::v-deep .el-textarea {
    flew: 1;
    .el-textarea__inner {
      border-width: 0px;
      border-bottom: 1px solid #DCDFE6;
      border-radius: 0px;
    }
  }
  .case-step-row-tools {
    display: flex;
    flex-direction: column;
    justify-content: center;
    ::v-deep .el-link--inner {
      line-height: 20px;
    }
  }
}
.button-full {
  width: 100%;
}
</style>
