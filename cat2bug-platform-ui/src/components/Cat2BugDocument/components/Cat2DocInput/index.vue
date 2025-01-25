<template>
  <div>
    <slot name="tools"></slot>
    <input ref="c2d-input" class="c2d-input" v-model="content.value"
           @keydown.enter="handleEnter"
           @keydown.delete="handleDelete"
    />
  </div>
</template>

<script>
export default {
  name: "Cat2DocInput",
  props: {
    content: {
      type: Object,
      default: ()=>{
        return {
          value: ''
        }
      }
    }
  },
  mounted() {
    this.focus();
  },
  methods: {
    focus() {
      this.$nextTick(()=>{
        this.$refs['c2d-input'].focus();
      });
    },
    toData() {

    },
    handleEnter() {
      this.$emit('enter');
    },
    handleDelete() {
      if(!this.content.value || this.content.value.length==0) {
        this.$emit('delete');
      }
    }
  }
}
</script>

<style scoped>
.c2d-input {
  width: 100%;
  font-size: 20px;
  border-width: 0px;
  outline: none;
}
</style>
