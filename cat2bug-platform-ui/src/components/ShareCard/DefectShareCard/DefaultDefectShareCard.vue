<template>
  <div ref="share" class="share">
    <div class="header">
      <row-list-member :members="params.handleByList" />
      <h4 class="defect-title-num">#{{params.projectNum}}</h4>
      <h4 class="defect-title-name">{{params.defectName}}</h4>
    </div>
    <div class="body">
      <div>
        <label>{{$t('shard.aging-hour')}}:</label><span>{{defectAgingHour()}}</span>
      </div>
      <div v-if="password">
        <label>{{$t('password')}}:</label><span>{{password}}</span>
      </div>
    </div>
    <div class="body">
      <div class="img-list">
        <el-image
          :key="index"
          :index="index"
          v-for="(img,index) in getUrl(params.imgUrls)"
          :src="img"
          fit="cover"></el-image>
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
      <canvas class="qr" id="defect-qr-canvas" ></canvas>
      <span>{{$t('defect.shard.click-qrcode-info')}}</span>
    </div>
    <div class="link">
      <el-link>{{getDefectUrl(shard.defectShardId)}}</el-link>
    </div>
  </div>
</template>

<script>
import DefectTypeFlag from "@/components/Defect/DefectTypeFlag";
import DefectStateFlag from "@/components/Defect/DefectStateFlag";
import RowListMember from "@/components/RowListMember";
import QRCode from 'qrcode'
import html2canvas from 'html2canvas';
import {getShardDefect} from "@/api/system/DefectShard";
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
    await this.createQRCode(this.getDefectUrl(this.shard.defectShardId));
  },
  methods: {
    createQRCode (url) {
      //先用 QRCode 生成二维码 canvas，然后用 html2canvas 合成整张海报并转成 base64 显示出来
      let canvas = document.getElementById('defect-qr-canvas')
      QRCode.toCanvas(canvas, url, (error) => {
        if (error) {
          console.error(error)
        }
      })
    },
    async copy(shard) {
      this.shard = shard;
      await this.createQRCode(this.getDefectUrl(shard.defectShardId));
      html2canvas(this.$refs.share).then(canvas => {
        const base64Img = canvas.toDataURL("image/png"); // 通过toDataURL将此canvas对象转成base64编码
        const file = this.base64toFile(base64Img, "图片"); // 转file
        this.copyFile(file);
      });
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
    copyFile(file) {
      const self = this;
      const reader = new FileReader();
      reader.onload = (e) => {
        const newFile = e.target.result.toString();
        const img = new Image();
        img.src = newFile;
        img.onload = () => {
          //blob对象在写入时可能会出现格式出题,所以先将图片解析成base64，在转换成blob对象写入，减少后面不必要的麻烦
          let base64 = this.imageBase64(img);
          let blob = this.base64ToBlob(base64.replace("data:image/png;base64,", ""), "image/png", 512);
          navigator.clipboard.write([
            new ClipboardItem({
              "image/png": blob
            })
          ]);
          self.$emit('copy');
        };
      };
      reader.readAsDataURL(file);
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
  }
 ::v-deep.img-list {
    width: 100%;
    display: inline-flex;
    flex-direction: row;
   flex-wrap: wrap;
    margin-bottom: 10px;
   .el-image:first-child {
     width: 100%;
     flex: auto;
   }
    .el-image {
      margin:3px;
      flex: 1;
      .el-image__inner[index="0"] {
        max-height: 150px;
        object-fit: cover;
        width: 100%;
      }
      .el-image__inner {
        max-height: 50px;
        object-fit: cover;
      }
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
</style>
