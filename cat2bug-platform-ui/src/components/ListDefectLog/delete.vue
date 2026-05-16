<template>
  <div class="defect-log-create">
    <span class="user">[{{ log.createByName }}]</span>
    <span class="state red">{{ $i18n.t('delete') }}</span>
  </div>
</template>

<script>
export default {
  name: 'DELETE',
  props: {
    log: {
      type: Object,
      default: () => ({})
    }
  }
}
</script>

<style lang="scss" scoped>
.defect-log-create {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  flex-direction: row;
  flex-wrap: wrap;
  font-size: 14px;
  span {
    padding-left: 2px;
    padding-right: 2px;
  }
  .user {
    font-weight: 500;
  }
  .state {
    font-size: 14px;
    color: #409eff;
  }
  .red {
    color: #f56c6c;
  }
}
</style>
