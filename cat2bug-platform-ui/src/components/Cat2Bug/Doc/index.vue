<template>
  <div>
    <svg-icon icon-class="documentation" @click="goto" />
  </div>
</template>

<script>
export default {
  name: 'Cat2BugDoc',
  methods: {
    goto() {
      this.$router.push('/system/doc')
    }
  }
}
</script>
