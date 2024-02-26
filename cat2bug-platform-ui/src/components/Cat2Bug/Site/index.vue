<template>
  <div>
    <svg-icon icon-class="website" @click="goto" />
  </div>
</template>

<script>
export default {
  name: 'RuoYiGit',
  data() {
    return {
      url: 'https://www.cat2bug.com'
    }
  },
  methods: {
    goto() {
      window.open(this.url)
    }
  }
}
</script>
