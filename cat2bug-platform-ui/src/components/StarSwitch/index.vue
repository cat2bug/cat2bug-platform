<template>
  <i :collect="collect?'true':'false'" class="el-icon-star-on start-switch" @click="clickCollectHandle"></i>
</template>

<script>
export default {
  name: "StartSwitch",model: {
    prop: 'collect',
    event: 'change'
  },
  props: {
    collect: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    clickCollectHandle(event) {
      this.$emit('change',!this.collect);
      event.stopPropagation();
    }
  }
}
</script>

<style lang="scss" scoped>
  .start-switch:hover {
    cursor: pointer;
  }
  .start-switch[collect="true"] {
    color: rgb(247, 186, 42);
    -webkit-text-stroke:3px transparent;
    background: rgb(247, 186, 42) top left / 100% 100%;
    -webkit-background-clip: text;
  }
  .start-switch {
    color: #FFFFFF;
    -webkit-text-stroke:3px transparent;
    background: rgb(198, 209, 222) top left / 100% 100%;
    -webkit-background-clip: text;
  }
</style>
