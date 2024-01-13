<template>
  <div>
    <svg-icon icon-class="github" @click="goto" />
  </div>
</template>

<script>
export default {
  name: 'RuoYiGit',
  data() {
    return {
      url: 'https://github.com/cat2bug'
    }
  },
  methods: {
    goto() {
      window.open(this.url)
    }
  }
}
</script>
