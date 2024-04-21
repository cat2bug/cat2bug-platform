<template>
  <div ref="share" class="share">
    <div class="header">
      <row-list-member :members="params.handleByList" />
      <h4 class="defect-title-num">#{{params.projectNum}}</h4>
      <h4 class="defect-title-name">{{params.defectName}}</h4>
    </div>
    <div class="body">
      <div>
        <i class="gitee"></i>
        <label>{{$t('shard.aging-hour')}}:</label>
        <span>{{defectAgingHour()}}</span>
      </div>
      <div v-if="password">
        <label>{{$t('password')}}:</label><span>{{password}}</span>
      </div>
    </div>
    <div class="body">
      <div v-show="params.imgUrls" class="img-list">
        <canvas ref="canvas" :class="`defectSvg${index}`"
             :key="index"
             :index="index"
             v-for="(img,index) in getUrl(params.imgUrls)"
        ></canvas>
      </div>

      <div class="flag">
        <defect-type-flag :defect="params" class="font-mini" />
        <defect-state-flag :defect="params" class="font-mini" />
      </div>
      <div class="describe">
        {{params.defectDescribe}}
      </div>
    </div>
    <div class="footer">
      <canvas ref="qrcode" class="qr" id="defect-qr-canvas" ></canvas>
      <span>{{$t('defect.shard.click-qrcode-info')}}</span>
    </div>
    <div class="link">
      <el-link>{{getDefectUrl(shard.defectShardId)}}</el-link>
    </div>
    <img ref="myImg" class="myImg" :src="shardBase64Img?'data:image/jpeg;base64' + shardBase64Img:''" />
  </div>
</template>

<script>
import DefectTypeFlag from "@/components/Defect/DefectTypeFlag";
import DefectStateFlag from "@/components/Defect/DefectStateFlag";
import RowListMember from "@/components/RowListMember";
import QRCode from 'qrcode'
import html2canvas from 'html2canvas';
import Label from "@/components/Cat2BugStatistic/Components/Label";

export default {
  name: "DefaultDefectShareCard",
  components:{Label, DefectTypeFlag, DefectStateFlag, RowListMember},
  props: {
    params: {
      type: Object,
      default: ()=>{}
    },
    agingHour: {
      type: String,Number,
      default: 1
    },
    password: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      shardBase64Img: null,
      shard: {
        defectShardId: 'XXXXXXXXXX',
        agingTime: new Date(),
      }
    }
  },
  computed: {
    getDefectUrl: function () {
      return function (id) {
        let mode = this.$router.mode=='hash'?'/#':'';
        return `${window.location.protocol}//${window.location.host}${mode}/shard/defect?id=${id}`;
      }
    },
    getUrl: function () {
      return function (urls) {
        let imgs = urls ? urls.split(',') : [];
        if(imgs) {
          return imgs.map(i => {
            return process.env.VUE_APP_BASE_API + i;
          })
        } else {
          return [];
        }
      }
    },
    imgLen: function () {
      return function (urls) {
        let imgs = urls ? urls.split(',') : [];
        return imgs.length;
      }
    },
    defectAgingHour: function () {
      return function () {
        switch (parseInt(this.agingHour)) {
          case 1:
            return this.$i18n.t('shard.1hour');
          case 24:
            return this.$i18n.t('shard.1day');
          case 168:
            return this.$i18n.t('shard.7day');
          case 0:
            return this.$i18n.t('shard.permanent');
        }
      }
    },
  },
  async mounted () {

  },
  methods: {
    drawImage() {
      let imgs = this.getUrl(this.params.imgUrls);
      for(let i in imgs) {
        const canvas = this.$refs.canvas[i];
        const ctx = canvas.getContext('2d');
        canvas.width = canvas.clientWidth || canvas.width;
        canvas.height = canvas.clientHeight || canvas.height;
        const img = new Image();
        img.src = imgs[i]; // 替换为你的图片路径
        img.onload = () => {
          // 计算图片居中位置的坐标
          // 设置要绘制的图片区域
          let sourceX = 0; // 源图片X坐标
          let sourceY = 0; // 源图片Y坐标
          let sourceWidth = img.width; // 源图片宽度
          let sourceHeight = img.height; // 源图片高度

          // 设置目标canvas上的绘制位置和大小
          const destX = 0; // 目标canvasX坐标
          const destY = 0; // 目标canvasY坐标
          const destWidth = canvas.width; // 目标canvas宽度
          const destHeight = canvas.height; // 目标canvas高度

          if((img.width/img.height)<(canvas.width/canvas.height)){
            sourceX = 0;
            sourceWidth = img.width;
            sourceY = (img.height - img.width/(canvas.width/canvas.height))/2;
            sourceHeight = img.width/(canvas.width/canvas.height);
          } else {
            sourceX = (img.width - img.height * (canvas.width/canvas.height))/2;
            sourceWidth = img.height * (canvas.width/canvas.height);
            sourceY = 0
            sourceHeight = img.height;
          }
          // 绘制图片的一部分到canvas上
          ctx.drawImage(
            img,
            sourceX, sourceY, sourceWidth, sourceHeight,
            destX, destY, destWidth, destHeight
          );
        };
      }
    },
    createQRCode (url) {
      QRCode.toCanvas(this.$refs.qrcode, url, (error) => {
        if (error) {
          console.error(error);
        }
      })
    },
    async refresh(shard) {
      this.shard = shard;
      await this.createQRCode(this.getDefectUrl(this.shard.defectShardId));
      this.drawImage();
      let canvas = await html2canvas(this.$refs.share);
      this.shardBase64Img = canvas.toDataURL('image/png');
    },
   async copy(shard) {
      this.shard = shard;
      let self = this;
     if (navigator.clipboard) {
       setTimeout( async ()=>{
         await navigator.clipboard.write([
           new ClipboardItem({
             'image/png': new Promise(async (resolve) => {
               self.createQRCode(this.getDefectUrl(shard.defectShardId));
               let canvas = await html2canvas(self.$refs.share);
               const base64Img = canvas.toDataURL("image/png");
               let blob = self.base64ToBlob(base64Img.replace("data:image/png;base64,", ""), "image/png", 512);
               resolve(new Blob([blob], {type: 'image/png'}));
             }),
           })
         ]);
         this.$emit('copy');
       },0);
     } else if (document.execCommand && document.queryCommandSupported('copy')) {
       self.getSelect(self.$refs.myImg);
       document.execCommand('copy');
       // 清空选中区域
       window.getSelection().removeAllRanges();
       self.$emit('copy');
     }
    },
    getSelect(targetNode) {
      if (window.getSelection) {
        //chrome等主流浏览器
        let selection = window.getSelection();
        let range = document.createRange();
        range.selectNode(targetNode);
        // range.comparePoint(targetNode,0)
        // range.setStart(targetNode,0)
        // range.setEnd(targetNode,0);
        // range.insertNode(targetNode)
        selection.removeAllRanges();
        selection.addRange(range);
        // targetNode.focus();
      }
    },
    base64toFile (dataBase64, filename = "file") {
      const arr = dataBase64.split(",");
      const mime = arr[0].match(/:(.*?);/)[1]; // 获取file文件流的type名称
      const suffix = mime.split("/")[1]; // 获取文件类型
      const bstr = window.atob(arr[1]);
      let n = bstr.length;
      const u8arr = new Uint8Array(n);
      while (n--) {
        u8arr[n] = bstr.charCodeAt(n);
      }
      return new File([u8arr], `${filename}.${suffix}`, {
        type: mime
      });
    },
    imageBase64(img) {
      let canvas = document.createElement("canvas");
      canvas.width = img.width;
      canvas.height = img.height;
      let ctx = canvas.getContext("2d");
      ctx.drawImage(img, 0, 0, img.width, img.height);
      let dataURL = canvas.toDataURL("image/png");
      return dataURL;
    },
    base64ToBlob(b64Data, contentType, sliceSize) {
      contentType = contentType || "";
      sliceSize = sliceSize || 512;
      let byteCharacters = window.atob(b64Data);
      // var byteCharacters  = b64Data;
      // 该atob函数将base64编码的字符串解码为一个新字符串，其中包含二进制数据每个字节的字符。
      let byteArrays = [];
      for (let offset = 0; offset < byteCharacters.length; offset += sliceSize) {
        let slice = byteCharacters.slice(offset, offset + sliceSize);
        let byteNumbers = new Array(slice.length);
        // 通过使用.charCodeAt字符串中每个字符的方法应用它来创建一个新的数组。
        for (let i = 0; i < slice.length; i++) {
          byteNumbers[i] = slice.charCodeAt(i);
        }
        // 将这个数组转换为实际类型的数组，方法是将其传递给Uint8Array构造函数。
        let byteArray = new Uint8Array(byteNumbers);
        byteArrays.push(byteArray);
      }
      // 创建一个blob：包含这条数据的URL，返回去。
      let blob = new Blob(byteArrays, { type: contentType });
      return blob;
    },
  }
}
</script>

<style lang="scss" scoped>
 .share {
   width: 300px;
   display: inline-flex;
   flex-direction: column;
   justify-content: flex-start;
   background-color: #f4f4f4;
   padding: 0px 10px 10px 10px;
   border-radius: 5px;
   position: relative;
 }
  .header {
    display: inline-flex;
    flex-direction: row;
    justify-content: flex-start;
    align-items: center;
    gap: 10px;
    .defect-title-num {
      margin-left: 5px;
    }
    .defect-title-name {
      flex: 1;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }
  }
  .body {
    padding: 10px 10px;
    background-color: #FFFFFF;
    border-radius: 5px;
    margin-bottom: 10px;
    font-size: 12px;
  }
 .img-list {
    width: 100%;
    display: inline-flex;
    flex-direction: row;
    flex-wrap: wrap;
    margin-bottom: 10px;
    > canvas:first-child {
     width: 100%;
     height: 150px;
     flex: auto;
    }
    > canvas {
      padding:3px;
      flex: 1;
      width: 0px;
      height: 50px;
    }
  }
  .footer {
    display: inline-flex;
    flex-direction: row;
    justify-content: flex-start;
    align-items: center;
    padding: 15px 10px;
    background-color: #FFFFFF;
    border-radius: 5px;
    width: 100%;
    overflow: hidden;
    .qr {
      width: 100px !important;
      height: 100px !important;
    }
    > span {
      flex: 1;
    }
  }
  .describe {
    margin-top: 10px;
    -webkit-line-clamp: 2;
    overflow: hidden;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 4;
    text-overflow: ellipsis;
  }
  .font-mini {
    font-size: 10px;
    margin-right: 5px;
    padding: 0 5px;
    height: 22px;
    line-height: 20px;
  }
  .flag {
    display: inline-flex;
    flex-direction: row;
    align-items: center;
    justify-content: flex-start;
  }
  .link {
    margin-top: 10px;
    padding: 0px 10px;
    width: 100%;
    word-break:break-all;
    white-space: normal;
  }
  label {
    padding-right: 10px;
  }
  .el-link {
    color: #1890ff;
    text-decoration: underline;
    text-decoration-color: #1890ff;
  }
  .myImg {
    opacity: 0;
    position: absolute;
  }
</style>
