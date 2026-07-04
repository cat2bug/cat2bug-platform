<template>
  <div>
    <svg-icon icon-class="documentation" @click="goto" />
  </div>
</template>

<script>
const SYSTEM_DOC_URL = 'https://www.cat2bug.com/document/cat2bug-platform'

export default {
  name: 'Cat2BugDoc',
  methods: {
    goto() {
      window.open(SYSTEM_DOC_URL)
    }
  }
}
</script>
