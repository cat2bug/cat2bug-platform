<template>
  <div class="case-step">
    <draggable v-model="caseStepList" @change="updatePanelHandle">
      <div v-for="(step,index) in caseStepList" :key="index" class="case-step-row">
        <label>{{index+1}}</label>
        <el-input type="textarea" v-model="step.stepDescribe" :placeholder="$t('case.please-enter-step-describe')" maxlength="128" rows="1" />
        <el-input type="textarea" v-model="step.stepExpect" :placeholder="$t('case.please-enter-step-expected')" maxlength="128" rows="1" />
        <div class="case-step-row-tools">
          <el-link :step="index" type="danger" :underline="false" @click="removeStepHandle(index)" v-show="caseStep.length>1"><i class="el-icon-error el-icon--right"></i></el-link>
          <el-link type="success" :underline="false" @click="addStepHandle(index)"><i class="el-icon-circle-plus el-icon--right"></i></el-link>
        </div>
      </div>
    </draggable>
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
      caseStepList: this.caseStep,
    }
  },
  props: {
    caseStep: {
      type: Array,
      default: ()=>[{}]
    }
  },
  watch: {
    caseStep: function (n) {
      this.caseStepList = n;
    }
  },
  methods: {
    addStepHandle(index) {
      if(index!=undefined){
        this.caseStepList.splice(index,0,{});
      } else {
        this.caseStepList.push({});
      }
      this.$emit('change',this.caseStepList);
    },
    removeStepHandle(index) {
      this.caseStepList.splice(index,1);
      this.$emit('change',this.caseStepList);
    },
    updatePanelHandle() {
      this.$emit('change',this.caseStepList);
    }
  }
}
</script>

<style lang="scss" scoped>
.case-step {
  width: 100%;
}
.case-step-row {
  display: flex;
  border-radius: 5px;
  flex-direction: row;
  align-items: center;
  > * {
    margin: 5px;
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
