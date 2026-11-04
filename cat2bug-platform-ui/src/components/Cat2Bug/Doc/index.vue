<template>
  <div>
    <svg-icon icon-class="documentation" @click="goto" />
  </div>
</template>

<script>
import { openSystemDoc } from '@/utils/system-doc-url'

export default {
  name: 'Cat2BugDoc',
  methods: {
    goto() {
      openSystemDoc()
    }
  }
}
</script>
