<template>
  <div class="step">
    <div class="step-row" v-for="(step,index) in steps" :key="index" @click="edit && clickStepHandle($event, index)" :class="stateVisible&&edit?'pointer':''" :state="stateVisible?step.state:''">
      <p>{{ index + 1 }}</p>
      <span>{{ step.stepDescribe }}</span>
      <span>{{ step.stepExpect }}</span>
      <div v-show="stateVisible" class="step-state" :state="step.state">{{ stateName(step) }}</div>
    </div>
    <div v-show="stateVisible" class="step-tools">
      <el-link v-show="isShowFoldButton" :underline="false" type="primary" @click.native="foldChangedHandle"><svg-icon :icon-class="foldState?'up':'down'" />{{ foldButtonName }}</el-link>
    </div>
  </div>
</template>

<script>
// 没有测试
const NOT_TEST_KEY = 'case.not-tested';
// 通过测试
const PASS_TEST_KEY = 'case.pass-tested';
// 未通过
const FAILED_TEST_KEY = 'case.failed-tested';
export default {
  name: "step",
  model: {
    prop: 'stepIndex',
    event: 'change'
  },
  data() {
    return {
      foldState: false,
    }
  },
  props: {
    caseSteps: {
      type: Array,
      default: ()=>[]
    },
    stepIndex: {
      type: Number,
      default: null
    },
    edit: {
      type: Boolean,
      default: false
    },
    stateVisible: {
      type: Boolean,
      default: false
    },
    maxShowCaseCount: {
      type: Number,
      default: 3
    }
  },
  computed: {
    foldButtonName: function (){
      if(this.foldState) {
        return this.$i18n.t('case.fold-button-off');
      } else {
        return this.$i18n.t('case.fold-button-on');
      }
    },
    /** 显示的步骤 */
    steps: function() {
      let ret = [...this.caseSteps];
      if(this.stepIndex!=undefined) {
        for(let i = 0;i<this.stepIndex;i++){
          ret[i].state=PASS_TEST_KEY;
        }
        ret[this.stepIndex].state=FAILED_TEST_KEY;
        for(let i = this.stepIndex+1;i<ret.length;i++){
          ret[i].state=NOT_TEST_KEY;
        }
      }

      if(!this.stateVisible || this.caseSteps.length<=this.maxShowCaseCount || this.foldState) {
        ret = ret;
      } else {
        let tempArr = ret.filter(c=>c.state===PASS_TEST_KEY || c.state===FAILED_TEST_KEY);
        ret = [...tempArr,
          ...ret.filter(c=>!c.state || c.state===NOT_TEST_KEY).filter((c,i)=>i<Math.max(this.maxShowCaseCount-tempArr.length,1))];
      }

      return ret;
    },
    isShowFoldButton: function () {
      if(this.caseSteps.length<=this.maxShowCaseCount) {
        return false;
      }
      return this.caseSteps.filter(c=>!c.state || c.state===NOT_TEST_KEY).length>1;
    },
    /** 状态名 */
    stateName: function (){
      return function (step){
        if(step.state) {
          return this.$i18n.t(step.state);
        } else {
          return this.$i18n.t(NOT_TEST_KEY);
        }
      }
    },
  },
  methods: {
    clickStepHandle(e, index) {
      if(this.stateVisible) {
        this.$emit('change', index);
        e.stopPropagation();
      }
    },
    foldChangedHandle(e) {
      this.foldState=!this.foldState;
      e.stopPropagation();
    }
  }
}
</script>

<style lang="scss" scoped>
.step {
  display: flex;
  flex-direction: column;
  width: 100%;
  overflow: auto;
}
.step-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  width: 100%;
  padding: 5px;
  margin-bottom: 5px;
  border-radius: 3px;
  border: 1px solid #FFFFFF00;
  p {
    width: 15px;
    height: 15px;
    line-height: 15px;
    text-align: center;
    font-size: 12px;
    margin: 0px 10px;
    border-radius: 100%;
    background-color: #13ce66;
    color: #FFFFFF;
  }
  span {
    flex:1;
    overflow: hidden;
    overflow-wrap: break-word;
    //text-overflow: ellipsis;
    //white-space: nowrap;
    text-align: left;
    font-size: 14px;
    line-height: 15px;
    margin-right: 5px;
    //min-height: 10px;
  }
  .step-state {
    color: #FFFFFF;
    min-width: 70px;
    border-radius: 3px;
    text-align: center;
    background-color: #C0C4CC;
    font-size: 12px;
    height: 20px;
    line-height: 20px;
  }
  .step-state[state='case.not-tested'] {
    background-color: #C0C4CC;
  }
  .step-state[state='case.pass-tested'] {
    background-color: #67C23A;
  }
  .step-state[state='case.failed-tested'] {
    background-color: #F56C6C;
  }
  .pointer {
    cursor: pointer;
  }
}
.step-row[state='case.not-tested'] {
  //background-color: #f4f4f5;
  //border: 1px solid #d3d4d6;
}
.step-row[state='case.pass-tested'] {
  background-color: #f0f9eb;
  border: 1px solid #c2e7b0;
  color: #67c23a;
}
.step-row[state='case.failed-tested'] {
  background-color: #fef0f0;
  border: 1px solid #fbc4c4;
  color: #f56c6c;
}
.step-tools {
  display: flex;
  justify-content: center;
  width: 100%;
  flex:1;
  font-size: 14px;
}
</style>
