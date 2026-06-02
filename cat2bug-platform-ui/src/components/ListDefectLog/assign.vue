<template>
  <div class="defect-log-create">
    <defect-log-user size="medium" :name="log.createByName" :avatar="log.createByAvatar" />
    <span class="state">{{ $i18n.t('defect.assigned-to') }}</span>
    <row-list-member
      v-if="log.receiveByList && log.receiveByList.length > 1"
      :members="log.receiveByList"
      size="small"
    />
    <defect-log-user v-else-if="log.receiveByList && log.receiveByList.length" :member="log.receiveByList[0]" />
    <span>,</span>
    <div>
      <span>{{$i18n.t('describe')}}:</span>
      <span>{{log.defectLogDescribe}}</span>
    </div>
  </div>
</template>

<script>
import DefectLogUser from './DefectLogUser'
import RowListMember from '@/components/RowListMember'

export default {
  name: "ASSIGN",
  components: { DefectLogUser, RowListMember },
  props:{
    log: {
      type: Object,
      default: ()=>{}
    }
  }
}
</script>

<style lang="scss" scoped>
  .defect-log-create {
    display: flex;
    align-items: center;
    flex-direction: row;
    justify-content: flex-start;
    align-items: center;
    flex-direction: row;
    flex-wrap: wrap;
    font-size: 14px;
    span {
      padding-left: 2px;
      padding-right: 2px;
    }
    .state {
      font-size: 14px;
      color: #409EFF;
    }
    .orange {
      color: rgb(255, 186, 0);
    }
  }
</style>
