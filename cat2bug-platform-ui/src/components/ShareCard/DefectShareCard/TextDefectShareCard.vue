<template>
  <div ref="share" class="share">
    <div class="header">
      <h4 class="defect-title-num">#{{params.projectNum}}</h4>
      <h4 class="defect-title-name">{{params.defectName}}</h4>
    </div>
    <div class="body">
      <span>{{`${$t('defect.type')}：${this.$i18n.t(this.params.defectTypeName)}`}}</span>
      <span>{{`${$t('defect.state')}：${this.$i18n.t(this.params.defectStateName)}`}}</span>
      <span v-if="this.params.handleByList">{{`${$t('handle-by')}：${this.params.handleByList.map(h=>h.nickName).join('、')}`}}</span>
      <span v-if="agingHour">{{`${$t('shard.aging-hour')}：${defectAgingHour()}`}}</span>
      <span v-if="password">{{`${$t('password')}：${this.password}`}}</span>
      <span class="describe">{{params.defectDescribe}}</span>
    </div>
    <div class="link">
      <span>{{$t('defect.shard.click-view')}}：</span>
      <el-link>{{getDefectUrl(shard.defectShardId)}}</el-link>
    </div>
  </div>
</template>
<script src="./src/utils/clipboard.js"></script>
<script>
import DefectTypeFlag from "@/components/Defect/DefectTypeFlag";
import DefectStateFlag from "@/components/Defect/DefectStateFlag";
import RowListMember from "@/components/RowListMember";
import ClipboardJS from "clipboard";
export default {
  name: "TextDefectShareCard",
  components:{DefectTypeFlag, DefectStateFlag, RowListMember},
  props: {
    params: {
      type: Object,
      default: ()=>{}
    },
    agingHour: {
      type: String,Number,
      default: 1
    },
    password: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      shard: {
        defectShardId: 'XXXXXXXXXX',
        agingTime: new Date(),
      }
    }
  },
  computed: {
    getDefectUrl: function () {
      return function (id) {
        let mode = this.$router.mode=='hash'?'/#':'';
        return `${window.location.protocol}//${window.location.host}${mode}/shard/defect?id=${id}`;
      }
    },
    defectAgingHour: function () {
      return function () {
        switch (parseInt(this.agingHour)) {
          case 1:
            return this.$i18n.t('shard.1hour');
          case 24:
            return this.$i18n.t('shard.1day');
          case 168:
            return this.$i18n.t('shard.7day');
          case 0:
            return this.$i18n.t('shard.permanent');
        }
      }
    },
  },
  async mounted () {
  },
  methods: {
    init() {},
    async copy(shard) {
      this.shard = shard;
      let self = this;
      let content = `#${this.params.projectNum} ${this.params.defectName}\n\n`;
      content += `${this.$i18n.t('defect.type')}：${this.params.defectTypeName}\n`;
      content += `${this.$i18n.t('defect.state')}：${this.params.defectStateName}\n`;
      content += `${this.$i18n.t('handle-by')}：${this.params.handleByList.map(h=>h.nickName).join('、')}\n`;
      content += `${this.$i18n.t('shard.aging-hour')}：${this.defectAgingHour()}\n`;
      if(this.password) {
        content += `${this.$i18n.t('password')}：${this.password}\n`;
      }

      let desc = this.params.defectDescribe;
      if(desc.length>255) {
        desc = desc.substring(0,255-2) +'...';
      }
      content += `\n${desc}\n`;
      content += `\n${this.$i18n.t('defect.shard.click-view')}：\n`;
      content += `${this.getDefectUrl(this.shard.defectShardId)}\n`;
      await navigator.clipboard.writeText(content);
      self.$emit('copy');
    },
  }
}
</script>

<style lang="scss" scoped>
.share {
  width: 300px;
  display: inline-flex;
  flex-direction: column;
  justify-content: flex-start;
  background-color: #f4f4f4;
  padding: 0px 10px 10px 10px;
  border-radius: 5px;
}
.header {
  display: inline-flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  gap: 10px;
  .defect-title-num {
    margin-left: 5px;
  }
  .defect-title-name {
    flex: 1;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
}
.body {
  padding: 15px 10px;
  background-color: #FFFFFF;
  border-radius: 5px;
}
.link {
  padding-top: 10px;
  .el-link {
    color: #1890ff;
    text-decoration: underline;
    text-decoration-color: #1890ff;
  }
}
.body, .link {
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
}
.describe {
  margin-top: 10px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 15;
  text-overflow: ellipsis;
}
</style>
