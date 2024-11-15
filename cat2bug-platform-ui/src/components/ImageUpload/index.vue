<template>
  <div class="component-upload-image">
    <div class="flex-row">
      <el-upload
        multiple
        :action="uploadImgUrl"
        list-type="picture-card"
        :on-success="handleUploadSuccess"
        :before-upload="handleBeforeUpload"
        :limit="limit"
        :on-error="handleUploadError"
        :before-remove="handleBeforeRemove"
        :on-exceed="handleExceed"
        ref="imageUpload"
        :on-remove="handleDelete"
        :show-file-list="true"
        :headers="headers"
        :file-list="fileList"
        :on-preview="handlePictureCardPreview"
        class="cat2bug-upload"
        :class="{hide: fileList.length >= limit}"
      >
        <template v-slot:trigger>
          <el-button class="update-button" v-if="fileList.length < limit" :style="buttonStyle">
            <i class="el-icon-plus"></i>
          </el-button>
          <el-button v-if="isShowClipboardButton && fileList.length < limit" class="update-button" :style="buttonStyle" @click="clipboardImageHandle">
            <svg-icon icon-class="clipboard" />
          </el-button>
        </template>
      </el-upload>
    </div>
    <!-- 上传提示 -->
    <div class="el-upload__tip" slot="tip" v-if="showTip">
      {{$t('upload.please-upload')}}
      <template v-if="fileSize"> {{$t('upload.size-not-exceeding')}} <b style="color: #f56c6c">{{ fileSize }}MB</b> </template>
      <template v-if="fileType"> {{$t('upload.format-is')}} <b style="color: #f56c6c">{{ fileType.join("/") }}</b> </template>
      {{$t('upload.files')}}
    </div>

    <el-dialog
      :visible.sync="dialogVisible"
      :title="$t('preview')"
      width="800"
      append-to-body
    >
      <img
        :src="dialogImageUrl"
        style="display: block; max-width: 100%; margin: 0 auto"
      />
    </el-dialog>
  </div>
</template>

<script>
import { getToken } from "@/utils/auth";
import {upload} from "@/api/common/upload";
import {strFormat} from "@/utils";
import i18n from "@/utils/i18n/i18n";
import {delDefect} from "@/api/system/defect";

export default {
  props: {
    value: [String, Object, Array],
    // 图片数量限制
    limit: {
      type: Number,
      default: 5,
    },
    // 大小限制(MB)
    fileSize: {
       type: Number,
      default: 5,
    },
    // 文件类型, 例如['png', 'jpg', 'jpeg']
    fileType: {
      type: Array,
      default: () => ["png", "jpg", "jpeg"],
    },
    // 是否显示剪切按钮
    isShowClipboardButton: {
      type: Boolean,
      default: true
    },
    // 是否显示提示
    isShowTip: {
      type: Boolean,
      default: true
    },
    buttonStyle: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      number: 0,
      uploadList: [],
      dialogImageUrl: "",
      dialogVisible: false,
      hideUpload: false,
      baseUrl: process.env.VUE_APP_BASE_API,
      uploadImgUrl: process.env.VUE_APP_BASE_API + "/common/upload", // 上传的图片服务器地址
      headers: {
        Authorization: "Bearer " + getToken(),
      },
      fileList: []
    };
  },
  watch: {
    value: {
      handler(val) {
        if (val) {
          // 首先将值转为数组
          const list = Array.isArray(val) ? val : this.value.split(',');
          // 然后将数组转为对象数组
          this.fileList = list.map(item => {
            if (typeof item === "string") {
              if (item.indexOf(this.baseUrl) === -1) {
                  item = { name: this.baseUrl + item, url: this.baseUrl + item };
              } else {
                  item = { name: item, url: item };
              }
            }
            return item;
          });
        } else {
          this.fileList = [];
          return [];
        }
      },
      deep: true,
      immediate: true
    }
  },
  computed: {
    // 是否显示提示
    showTip() {
      return this.isShowTip && (this.fileType || this.fileSize);
    },
  },
  methods: {
    // var imageUrl = URL.createObjectURL(blob);
    async clipboardImageHandle(event) {
      this.dialogVisible = false;
      event.stopPropagation();
      await this.getClipboardImage();
    },
    async getClipboardImage() {
      try {
        let self = this;
        const clipboardItems = await navigator.clipboard.read();
        let count = 0;
        for (const clipboardItem of clipboardItems) {
          for (const type of clipboardItem.types) {
            const blob = await clipboardItem.getType(type);
            if(blob.type=='image/png') {
              const formData = new FormData();
              formData.append('file', blob);
              let res = await upload(formData);
              self.$emit("input", self.listToString([...self.fileList,...[{ name: res.fileName, url: res.fileName }]]));
              count++;
            }
          }
        }
        if(count==0) {
          this.$message.warning(this.$i18n.t('upload.clipboard-not-img').toString());
        }
      } catch (err) {
        console.error(err.name, err.message);
      }
    },
    // 上传前loading加载
    handleBeforeUpload(file) {
      let isImg = false;
      if (this.fileType.length) {
        let fileExtension = "";
        if (file.name.lastIndexOf(".") > -1) {
          fileExtension = file.name.slice(file.name.lastIndexOf(".") + 1);
        }
        isImg = this.fileType.some(type => {
          if (file.type.indexOf(type) > -1) return true;
          if (fileExtension && fileExtension.indexOf(type) > -1) return true;
          return false;
        });
      } else {
        isImg = file.type.indexOf("image") > -1;
      }

      if (!isImg) {
        this.$modal.msgError(strFormat(i18n.t('upload.file-format-is-incorrect'),this.fileType.join("/")));
        return false;
      }
      if (this.fileSize) {
        const isLt = file.size / 1024 / 1024 < this.fileSize;
        if (!isLt) {
          this.$modal.msgError(strFormat(i18n.t('upload.img-size-exceeds-range'),this.fileSize));
          return false;
        }
      }
      this.$modal.loading(this.$i18n.t('upload.img-loading'));
      this.number++;
    },
    // 文件个数超出
    handleExceed() {
      this.$modal.msgError(strFormat(i18n.t('upload.number-exceeds-range'),this.limit));
    },
    // 上传成功回调
    handleUploadSuccess(res, file) {
      if (res.code === 200) {
        this.uploadList.push({ name: res.fileName, url: res.fileName });
        this.uploadedSuccessfully();
      } else {
        this.number--;
        this.$modal.closeLoading();
        this.$modal.msgError(res.msg);
        this.$refs.imageUpload.handleRemove(file);
        this.uploadedSuccessfully();
      }
    },
    handleBeforeRemove(file, fileList) {
      return this.$modal.confirm(
        this.$i18n.t('is-delete-img'),
        this.$i18n.t('prompted').toString(),
        {
          confirmButtonText: i18n.t('delete').toString(),
          cancelButtonText: i18n.t('cancel').toString(),
          confirmButtonClass: 'delete-button',
          type: "warning"
        }).then(function() {
         return true;
      });
    },
    // 删除图片
    handleDelete(file) {
      const findex = this.fileList.map(f => f.name).indexOf(file.name);
      if(findex > -1) {
        this.fileList.splice(findex, 1);
        this.$emit("input", this.listToString(this.fileList));
      }
    },
    // 上传失败
    handleUploadError() {
      this.$modal.msgError(this.$i18n.t('upload.img-fail'));
      this.$modal.closeLoading();
    },
    // 上传结束处理
    uploadedSuccessfully() {
      if (this.number > 0 && this.uploadList.length === this.number) {
        this.fileList = this.fileList.concat(this.uploadList);
        this.uploadList = [];
        this.number = 0;
        this.$emit("input", this.listToString(this.fileList));
        this.$modal.closeLoading();
      }
    },
    // 预览
    handlePictureCardPreview(file) {
      this.dialogImageUrl = file.url;
      this.dialogVisible = true;
    },
    // 对象转成指定字符串分隔
    listToString(list, separator) {
      let strs = "";
      separator = separator || ",";
      for (let i in list) {
        if (list[i].url) {
          strs += list[i].url.replace(this.baseUrl, "") + separator;
        }
      }
      return strs != '' ? strs.substr(0, strs.length - 1) : '';
    }
  }
};
</script>
<style scoped lang="scss">
// .el-upload--picture-card 控制加号部分
::v-deep.hide .el-upload--picture-card {
    display: none !important;
}
// 去掉动画效果
::v-deep .el-list-enter-active,
::v-deep .el-list-leave-active {
    transition: all 0s;
}

::v-deep .el-list-enter, .el-list-leave-active {
    opacity: 0;
    transform: translateY(0);
}
.update-button {
  width: 148px;
  height: 148px;
  background-color: #fbfdff;
  border: 1px dashed #c0ccda;
  border-radius: 6px;
  cursor: pointer;
  font-size: 26px;
  flex-shrink: 0;
}
.update-button:hover {
  border-color: #1890ff;
  color: #1890ff;
}
.flex-row {
  display: inline-flex;
  flex-direction: row;
  gap: 10px;
}
::v-deep.cat2bug-upload > .el-upload--picture-card {
  width: 300px !important;
  display: inline-flex;
  flex-direction: row;
  border: none;
}
</style>

