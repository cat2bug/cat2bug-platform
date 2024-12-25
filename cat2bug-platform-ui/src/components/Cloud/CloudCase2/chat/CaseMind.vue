<template>
  <div class="case-mind" ref="caseMind">
    <div class="col" v-if="mindList.length>0">
      <h3><svg-icon icon-class="mind"></svg-icon>通过思维导图创建用例关键信息描述</h3>
      <span><i class="el-icon-info"></i>以下内容可以直接编辑修改</span>
      <div class="case-mind-row" v-for="(d,index) in mindList" :key="index">
        <div class="case-mind-row-name">
          <el-checkbox :label="(index + 1) + '.'" v-model="d.checked"/>
          <el-input v-model="d.name" />
        </div>
        <div class="row">
          <label>测试场景</label>
          <el-input v-model="d.scene" />
        </div>
        <div class="row">
          <label>页面元素</label>
          <el-input v-model="d.pageElement" />
        </div>
        <div class="row">
          <label>测试数据</label>
          <el-input v-model="d.testData" />
        </div>
        <div class="row">
          <label>预期结果</label>
          <el-input v-model="d.expectedResults" />
        </div>
      </div>
      <el-button class="case-button" type="primary" plain @click="handleClick">生成测试用例</el-button>
    </div>
    <el-empty v-else description="没有找到数据" :image-size="50"></el-empty>
  </div>
</template>

<script>
import { makeCaseMind } from "@/api/ai/AiCase2";
import Label from "@/components/Cat2BugStatistic/Components/Label";
import {Loading} from "element-ui";

export default {
  name: "CaseMind",
  components: {Label},
  data() {
    return {
      mindList: [],
    }
  },
  props: {
    data: {
      type: Array,
      default: ()=>[]
    }
  },
  mounted() {
    this.getMindList(this.data);
  },
  methods: {
    getMindList(data) {
      const loadingInstance = Loading.service({
        target: this.$refs.caseMind,
        text: '思维导图分析中,请耐心等待...',
        fullscreen: false,
      });
      const query = {
        query: JSON.stringify(data)
      }
      makeCaseMind(query).then(res=>{
        this.mindList = res.rows.map(d=>{
          d.checked = true;
          d.pageElement = d.pageElement?d.pageElement.join(', '):'';
          d.testData = d.testData?JSON.stringify(d.testData):'';
          return d;
        });
        this.$nextTick(()=>{
          loadingInstance.close();
          this.$emit('change');
        })
      }).catch(e=>{
        loadingInstance.close();
      })
    },
    handleClick() {
      this.$emit('submit', 'CaseMind',this.mindList.filter(d=>d.checked));
    }
  }
}
</script>

<style lang="scss" scoped>
.case-mind {
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
.case-mind-row {
  display: inline-flex;
  flex-direction: column;
  width: 100%;
  gap: 5px;
  .case-mind-row-name {
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
  align-items: center;
  width: 100%;
  > label {
    flex-shrink: 0;
    font-size: 0.8rem;
  }
  > .el-input {
    flex: 1;
  }
}
.case-button {
  width: 50%;
  height: 50px;
  margin-top: 20px;
  margin-left: 25%;
  font-size: 1.2rem;
}
</style>
