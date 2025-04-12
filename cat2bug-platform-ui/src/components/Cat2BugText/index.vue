<template>
  <el-tooltip effect="dark" :placement="tooltipPosition">
    <div v-if="type=='down'" slot="content"><div v-html="fileName(tooltip)"></div></div>
    <div v-else slot="content"><div v-html="text(tooltip)"></div></div>
    <el-link v-if="type=='link'" v-html="text(content)" @click="handleClick" type="primary" class="cat2bug-text-content" ></el-link>
    <el-link v-else-if="type=='down'" icon="el-icon-paperclip" @click="handleClickDown($event, content)" type="primary" class="cat2bug-text-content" >{{ fileName(content) }}</el-link>
    <div v-else-if="type=='text'" class="cat2bug-text-content" v-html="text(content)"></div>
  </el-tooltip>
</template>

<script>
export default {
  name: "Cat2BugText",
  model: {
    prop: 'content',
    event: 'click'
  },
  props: {
    content: {
      type: String,
      default: null
    },
    tooltip: {
      type: String,
      default: null
    },
    tooltipPosition: {
      type: String,
      default: 'top-start'
    },
    type: {
      type: String,
      default: 'text'
    }
  },
  computed: {
    text: function () {
      return function (value) {
        return value?value.replace('\n', '<br />'):'';
      }
    },
    fileName: function () {
      return function (url) {
        if(!url) return null;
        let arr = url.split('\/');
        return arr[arr.length-1].replaceAll(/_[0-9a-zA-Z]+\./,'.');
      }
    },
  },
  methods: {
    handleClick(e) {
      this.$emit('click');
      e.stopPropagation();
    },
    handleClickDown(event, url) {
      if(url) {
        let link = document.createElement('a')
        let image = document.createElement('image')
        let downloadName = this.fileName(url);
        console.log(url, downloadName);
        // 这里是将url转成blob地址，
        image.setAttribute("crossOrigin", "anonymous") // 这句可以解决跨域问题
        fetch(url).then(res => res.blob()).then(blob => { // 将链接地址字符内容转变成blob地址
          link.href = URL.createObjectURL(blob)
          link.download = downloadName
          document.body.appendChild(link)
          link.click()
        });
      }
      event.stopPropagation();
    }
  }
}
</script>

<style lang="scss" scoped>
.cat2bug-text-content {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
  text-overflow: ellipsis;
  align-items: start;
}
</style>
