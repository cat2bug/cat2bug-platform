<template>
  <div class="case-card">
    <div class="case-card-body" v-if="caseModel && caseModel.caseId">
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
    <div v-else class="case-card-empty">
      <span>{{ $t('empty-data') }}</span>
    </div>

  </div>
</template>

<script>
import CaseStep from "@/components/Case/CaseStep";

export default {
  name: "CaseCard",
  components: {CaseStep},
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
  methods: {
  }
}
</script>

<style lang="scss" scoped>
.case-card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 5px 20px;
  .case-card-body {
    width: 100%;
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
</style>
