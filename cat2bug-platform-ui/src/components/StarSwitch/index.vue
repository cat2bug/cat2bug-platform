<template>
  <i
    :collect="collect ? 'true' : 'false'"
    :class="[collect ? 'el-icon-star-on' : 'el-icon-star-off', 'start-switch']"
    @click="clickCollectHandle"
  />
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
.start-switch {
  font-size: inherit;
  line-height: 1;
  cursor: pointer;
}

/* 已收藏：color 作描边，background-clip 作填充（勿在未选中态加 background !important，否则会盖掉本段） */
.start-switch[collect="true"] {
  color: var(--star-switch-on-stroke, var(--star-switch-on-color, rgb(247, 186, 42)));
  -webkit-text-stroke: 3px transparent;
  background: var(--star-switch-on-fill, var(--star-switch-on-color, rgb(247, 186, 42))) top left / 100% 100%;
  -webkit-background-clip: text;
  background-clip: text;
}

.start-switch:not([collect="true"]) {
  color: var(--star-switch-off-stroke, var(--star-switch-off-color, #c0c4cc));
  background: transparent;
  -webkit-background-clip: unset;
  background-clip: unset;
  -webkit-text-stroke: 0;
}
</style>
