<template>
  <div>
    <svg-icon icon-class="documentation" @click="goto" />
  </div>
</template>

<script>
import { SYSTEM_DOC_URL } from '@/utils/system-doc-url'

export default {
  name: 'Cat2BugDoc',
  methods: {
    goto() {
      window.open(SYSTEM_DOC_URL)
    }
  }
}
</script>
