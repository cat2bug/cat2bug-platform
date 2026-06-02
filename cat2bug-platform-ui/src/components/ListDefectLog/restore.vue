<template>
  <div class="defect-log-create">
    <defect-log-user size="medium" :name="log.createByName" :avatar="log.createByAvatar" />
    <span class="state orange">{{ $i18n.t('defect.restore') }}</span>
  </div>
</template>

<script>
import DefectLogUser from './DefectLogUser'

export default {
  name: 'RESTORE',
  components: { DefectLogUser },
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
  .orange {
    color: #ffba00;
  }
}
</style>
