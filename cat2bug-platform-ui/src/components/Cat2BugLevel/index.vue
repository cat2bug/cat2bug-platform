<template>
  <div class="level-item">
    <div class="ring" :style="`color:${ringColor(level)}`"></div>
    <span style="color: #8492a6; font-size: 13px">{{ getLevelName(level) }}</span>
  </div>
</template>

<script>
import {getLevelName} from "@/utils/case";
export default {
  name: "index",
  data() {
    return {
      value: null,
    }
  },
  props: {
    level: {
      type: Number,
      default: 1
    }
  },
  computed: {
    ringColor:function () {
      return function (id){
        switch ((id-1)%5) {
          case 0:
            return '#5470c6'
          case 1:
            return '#91cc75'
          case 2:
            return '#fac858'
          case 3:
            return '#ee6666'
          case 4:
            return '#73c0de'
        }
      }
    }
  },
  methods: {
    getLevelName
  }
}
</script>

<style lang="scss" scoped>
.level-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  width: 100%;
  > .ring {
    margin-right: 15px;
    width: 15px;
    height: 15px;
    border-radius: 100%;
    border-width: 3px;
    border-style: solid;
  }
  > span {
    flex: 1;
    text-align: left;
  }
}
</style>
