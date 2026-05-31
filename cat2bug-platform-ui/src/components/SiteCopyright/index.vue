<template>
  <component :is="tag" :class="copyrightClass">{{ text }}</component>
</template>

<script>
import { getCopyrightYearRange } from '@/config/site'

export default {
  name: 'SiteCopyright',
  props: {
    tag: {
      type: String,
      default: 'span'
    },
    copyrightClass: {
      type: String,
      default: ''
    }
  },
  computed: {
    text() {
      return this.$t('copyright', { yearRange: getCopyrightYearRange() })
    }
  }
}
</script>
