<template>
  <div class="case-demand" ref="caseDemand">
    <div class="col" v-if="demandList.length>0">
      <h3><i class="el-icon-document"></i>需求分析</h3>
      <span><i class="el-icon-info"></i>以下内容可以直接编辑修改</span>
      <div class="case-demand-row" v-for="(d,index) in demandList" :key="index">
        <div class="case-demand-row-name">
          <el-checkbox :label="d.number+'.'" v-model="d.checked"/>
          <el-input v-model="d.name" />
        </div>
        <el-input v-model="d.describe" />
      </div>
      <el-button class="case-button" type="primary" plain @click="handleClick">生成测试用例思维导图</el-button>
    </div>
    <el-empty v-else description="没有找到数据"></el-empty>
  </div>
</template>

<script>
import {makeCaseDemand} from "@/api/ai/AiCase2";
import Label from "@/components/Cat2BugStatistic/Components/Label";
import { Loading } from 'element-ui';

export default {
  name: "CaseDemand",
  components: {Label},
  data() {
    return {
      loading: false,
      demandList: [],
    }
  },
  props: {
    data: {
      type: Object,
      default: null
    }
  },
  mounted() {
    this.getDemandList();
  },
  methods: {
    getDemandList() {
      const loadingInstance = Loading.service({
        target: this.$refs.caseDemand,
        text: '需求分析中,请耐心等待...',
        fullscreen: false,
      });
      makeCaseDemand(this.query).then(res=>{
        this.demandList = res.rows.map(d=>{
          d.checked = true;
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
      this.$emit('submit', 'CaseDemand',this.demandList.filter(d=>d.checked));
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
    ::v-deep i {
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
    color: #C0C4CC;
    height: 18px;
    line-height: 16px;
  }
  padding-bottom: 15px;
}
.col {
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  width: 100%;
}
.case-button {
  width: 50%;
  height: 50px;
  margin-top: 20px;
  margin-left: 25%;
  font-size: 1.2rem;
}
</style>
