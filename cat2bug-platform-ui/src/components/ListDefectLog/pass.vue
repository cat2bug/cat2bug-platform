<template>
  <div class="defect-log-create">
    <span class="user">[{{log.createByName}}]</span>
    <span class="state red">{{ $i18n.t('pass') }}</span>
    <span>,</span>
    <div>
      <span>{{$i18n.t('describe')}}:</span>
      <span>{{log.defectLogDescribe}}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: "PASS",
  props:{
    log: {
      type: Object,
      default: ()=>{}
    }
  },
  computed: {
    head: function () {
      return this.log.receiveByList[0].nickName;
    },
    assistant: function () {
      return this.log.receiveByList.filter((l,i)=>i!=0).map(l=>l.nickName).join(',');
    }
  },
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
    color: #409EFF;
  }
  .orange {
    color: rgb(255, 186, 0);
  }
  .red {
    color: #FF0000;
  }
}
</style>
