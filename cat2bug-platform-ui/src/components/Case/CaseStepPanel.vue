<template>
  <div class="case-step">
    <draggable v-show="panelType!='input'" v-model="caseStepList" @change="updatePanelHandle">
      <div ref="caseStepInputGroup" v-for="(step,index) in caseStepList" :key="index" class="case-step-row">
        <label>{{index+1}}</label>
        <el-input type="textarea" ref="caseStepDescribeRef" v-model="step.stepDescribe" :key="step.caseStepDescribeKey" :placeholder="$t('case.please-enter-step-describe')" maxlength="128" :autosize="autoSizeRang(step)" @input.native="caseStepListChangeHandle($event, caseStepList, index, 'describe')" />
        <el-input type="textarea" ref="caseStepExpectRef" v-model="step.stepExpect" :key="step.caseStepExpectKey" :placeholder="$t('case.please-enter-step-expected')" maxlength="128" :autosize="autoSizeRang(step)" @input="caseStepListChangeHandle($event,caseStepList, index, 'expect')" />
        <div class="case-step-row-tools">
          <el-link :step="index" type="danger" :underline="false" @click="removeStepHandle(index)" v-show="caseStep.length>1"><i class="el-icon-error el-icon--right"></i></el-link>
          <el-link type="success" :underline="false" @click="addStepHandle(index)"><i class="el-icon-circle-plus el-icon--right"></i></el-link>
        </div>
      </div>
    </draggable>
    <el-input class="case-step-script-input" v-show="panelType!='list'" type="textarea" v-model="stepScript" :placeholder="$t('case.please-enter-step-script')" maxlength="10000" :autosize="{ minRows: 4, maxRows: 20 }" show-word-limit @input="scriptChangeHandle" />
    <el-button class="button-full" type="success" size="mini" plain @click="addStepHandle(undefined)">{{ $t('case.add-step') }}</el-button>
  </div>
</template>

<script>
import draggable from 'vuedraggable'
/** 测试用例步骤组件 */
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
      caseStepList: null,
      ctx: null
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
  computed: {
    maxRows: function () {
      return 10;
    },
    autoSizeRang: function () {
      return function (step) {
        return {minRows:step.caseStepRows||1,maxRows: this.maxRows};
      }
    }
  },
  mounted() {
    const canvas = document.createElement('canvas')
    this.ctx = canvas.getContext('2d');
    setTimeout(()=>{
      this.refreshStepInput(this.caseStep);
      this.$forceUpdate();
    },500)

  },
  watch: {
    caseStep: function (n) {
      this.refreshStepInput(n);
      this.$forceUpdate();
    }
  },
  methods: {
    /** 刷新步骤文本框 */
    refreshStepInput(stepList) {
      this.$nextTick(()=>{
        stepList.forEach((s,index)=> {
          const rows = Math.max(this.calcInputRows('caseStepExpectRef',index, s.stepExpect), this.calcInputRows('caseStepDescribeRef', index, s.stepDescribe));
          const time = new Date().getMilliseconds();
          s.caseStepRows = Math.min(rows, this.maxRows);
          s.caseStepExpectKey = "caseStepExpectKey"+index+time;
          s.caseStepDescribeKey = "caseStepDescribeKey"+index+time;
        });
      });
    },
    /** 重置 */
    reset() {
      const time = new Date().getMilliseconds();
      this.caseStepList=this.caseStep.map((c,index)=>{
        c.caseStepDescribeKey = "caseStepDescribeKey"+index+time;
        c.caseStepExpectKey = "caseStepExpectKey"+index+time;
        return c;
      });
      this.caseStepListChangeHandle({}, this.caseStep);
    },
    /** 添加步骤 */
    addStepHandle(index) {
      this.caseStepList.push({});
      this.caseStepListChangeHandle({},this.caseStepList);
      this.$emit('change',this.caseStepList);
    },
    /** 移除步骤 */
    removeStepHandle(index) {
      this.caseStepList.splice(index,1);
      this.caseStepListChangeHandle({},this.caseStepList);
      this.$emit('change',this.caseStepList);
    },
    /** 步骤拖动更新事件 */
    updatePanelHandle() {
      this.$emit('change',this.caseStepList);
    },
    /** 计算input的行高 */
    calcInputRows(name, index, text) {
      if(!this.$refs[name] || !this.$refs[name][index]) return 1;
      const el = this.$refs[name][index].$el.children[0];

      const style = getComputedStyle(el);
      const parentStyle = getComputedStyle(el.parentNode);
      this.ctx.font = [parentStyle.fontSize,parentStyle.fontFamily].join(' ');
      const paddingVert = parseFloat(style.paddingLeft) + parseFloat(style.paddingRight);
      const borderVert = parseFloat(style.borderLeftWidth) + parseFloat(style.borderRightWidth);
      const textareaWidth = parseFloat(el.clientWidth)-paddingVert-borderVert;

      const splitedTexts = (text||'').split(/\r|\r\n|\n/)
      const lineCount = splitedTexts.reduce((pre, cur) => {
        const width = Math.max(this.ctx.measureText(cur).width,1);
        return pre + Math.ceil(width / textareaWidth);
      }, 0)

      const rows = lineCount;
      return  rows;
    },
    /** 测试步骤改变的处理 */
    caseStepListChangeHandle(e, list, rowIndex, propertyName) {
      if(e) {
        this.stepScript = list?list.filter(s=>s.stepDescribe || s.stepExpect).map((s,index)=> {
          if(rowIndex==index) {
            this.$nextTick(()=>{
              const rows = Math.max(this.calcInputRows('caseStepExpectRef',index, s.stepExpect), this.calcInputRows('caseStepDescribeRef', index, s.stepDescribe));
              const time = new Date().getMilliseconds();
              if(propertyName=='describe') {
                s.caseStepRows = Math.min(rows, this.maxRows);
                s.caseStepExpectKey = "caseStepExpectKey"+index+time;
              } else if(propertyName=='expect') {
                s.caseStepRows = Math.min(rows, this.maxRows);
                s.caseStepDescribeKey = "caseStepDescribeKey"+index+time;
              }
            });
          }
          return (s.stepDescribe?s.stepDescribe:'')+'---'+(s.stepExpect?s.stepExpect:'');
        }).join('\n'):'';
      }
    },
    /** 步骤脚本改变处理 */
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
