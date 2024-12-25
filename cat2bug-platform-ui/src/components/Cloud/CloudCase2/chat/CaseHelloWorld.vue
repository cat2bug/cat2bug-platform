<template>
  <div class="case-hello-world">
    <h3><i class="el-icon-question"></i>测试用例创建说明</h3>
    <el-steps direction="vertical">
      <el-step title="生成测试用例描述" description="在底部输入框中输入测试用例需求，点击【创建用例描述】按钮创建测试用例描述，生成后根据需求勾选或编辑；"></el-step>
      <el-step title="生成测试用例思维导图" description="根据第一步的测试用例描述，点击【生成测试用例思维导图】按钮，分析并创建测试用例详细信息，并可勾选和编辑；"></el-step>
      <el-step title="生成测试用例" description="根据第二部创建的用例详细信息，点击【生成测试用例】按钮，分析并创建测试用例，并点击【批量导出系统】按钮将测试用例导入到系统中；"></el-step>
    </el-steps>
  </div>
</template>

<script>
export default {
  name: "CaseHelloWorld"
}
</script>

<style lang="scss" scoped>
.case-hello-world {
  width: 100%;
  min-height: 100px;
  h3 {
    color: #409EFF;
    ::v-deep i {
      margin-right: 5px;
    }
  }
}
</style>
