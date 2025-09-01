<template>
  <div class="app-container">
    <el-page-header @back="goBack" content="文档详情">
    </el-page-header>
    <cat2-bug-document v-if="doc" v-loading="loading" />
    <el-empty v-else />
  </div>

</template>

<script>
import Cat2BugDocument from "@/components/Cat2BugDocument";
import {getDocument} from "@/api/system/document";
export default {
  name: "HandleCat2Document",
  components: { Cat2BugDocument },
  data() {
    return {
      loading: false,
      doc: null
    }
  },
  mounted() {
    // const docId = this.$route.params.docId;
    const docId = 64;
    if(docId) {
      this.getDoc(docId);
    }
  },
  methods: {
    goBack() {
      this.$router.back();
    },
    getDoc(docId) {
      this.loading = true;
      getDocument(docId).then(res=>{
        this.loading = false;
        this.doc = res.data;
      }).catch(e=>this.loading = false);
    }
  }
}
</script>

<style scoped>

</style>
