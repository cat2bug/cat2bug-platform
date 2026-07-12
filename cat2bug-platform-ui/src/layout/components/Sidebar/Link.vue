<template>
  <a v-if="isExternal" v-bind="linkProps(to)">
    <slot :navigate="null" />
  </a>
  <router-link v-else v-slot="{ navigate }" custom :to="to">
    <slot :navigate="navigate" />
  </router-link>
</template>

<script>
import { isExternal } from '@/utils/validate'

export default {
  props: {
    to: {
      type: [String, Object],
      required: true
    }
  },
  computed: {
    isExternal() {
      return isExternal(this.to)
    }
  },
  methods: {
    linkProps(to) {
      return {
        href: to,
        target: '_blank',
        rel: 'noopener'
      }
    }
  }
}
</script>
