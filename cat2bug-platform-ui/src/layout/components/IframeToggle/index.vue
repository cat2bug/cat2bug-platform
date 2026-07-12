<template>
  <transition-group name="fade-transform">
    <inner-link
      v-for="(item, index) in iframeViews"
      :key="item.path"
      :iframeId="'iframe' + index"
      v-show="$route.path === item.path"
      :src="iframeUrl(item.meta.link, item.query)"
    ></inner-link>
  </transition-group>
</template>

<script>
import { storeToRefs } from 'pinia'
import InnerLink from "../InnerLink/index";
import { useTagsViewStore } from '@/stores/tagsView'

export default {
  components: { InnerLink },
  setup() {
    const tagsViewStore = useTagsViewStore()
    const { iframeViews } = storeToRefs(tagsViewStore)
    return {
      iframeViews
    }
  },
  methods: {
    iframeUrl(url, query) {
      if (Object.keys(query).length > 0) {
        let params = Object.keys(query).map((key) => key + "=" + query[key]).join("&");
        return url + "?" + params;
      }
      return url;
    }
  }
}
</script>
