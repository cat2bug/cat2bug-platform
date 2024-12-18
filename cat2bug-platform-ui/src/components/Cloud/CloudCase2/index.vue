<template>
  <el-drawer
    :visible.sync="visible"
    :direction="direction"
    size="90%"
    :before-close="handleClose">
<!--    标题-->
    <template slot="title">
      <div class="case-search-header">
        <div class="case-search-title">
          <i class="el-icon-arrow-left" @click="close"></i>
          <h4 class="case-search-title-name"><svg-icon icon-class="robot" style="margin-right: 10px;" />{{ $t('case.ai-create') }}</h4>
        </div>
        <div>
        </div>
      </div>
    </template>
    <div>
      <div>
        <component is="CaseDemand" />
      </div>
      <div class="send">
        <el-input
          type="textarea"
          placeholder="请输入内容"
          v-model="query.query"
          maxlength="30"
          :rows="5"
          show-word-limit />
        <el-button @click="handle">点击</el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script>
import CaseDemand from "@/components/Cloud/CloudCase2/chat/CaseDemand";
import {makeCaseDemand} from "@/api/ai/AiCase2";
export default {
  name: "index",
  components: { CaseDemand },
  data() {
    return {
      visible: false,
      query: {query: '登陆页面'},
    }
  },
  computed: {
  },
  props: {
    direction: {
      type: String,
      default: 'rtl'
    }
  },
  methods: {
    open() {
      this.visible = true;
    },
    close() {
      this.$emit('close')
    },
    handleClose(done) {
      // done();
    },
    handle() {
      makeCaseDemand(this.query).then(res=>{
        console.log(res)
      })
    }
  }
}
</script>
<style>

</style>
