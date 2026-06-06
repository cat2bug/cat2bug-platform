<template>
  <div class="component-upload-image">
    <div
      ref="focusTarget"
      class="cat2bug-upload-focus-target component-upload-image-focus-target"
      tabindex="0"
      @focus="onShellFocus"
      @blur="onShellBlur"
      @keydown="onFocusShellKeydown"
      @paste="onShellPaste"
    >
    <div class="flex-row">
      <el-upload
        multiple
        :action="uploadImgUrl"
        :accept="uploadAccept"
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
          <el-button class="update-button update-button-add" v-if="fileList.length < limit" :style="buttonStyle" @click.native.capture="prepareNativeFilePicker">
            <i class="el-icon-plus"></i>
          </el-button>
          <el-button v-if="isShowClipboardButton && fileList.length < limit" class="update-button" :style="buttonStyle" @click="clipboardImageHandle">
            <svg-icon icon-class="clipboard" />
          </el-button>
        </template>
      </el-upload>
    </div>
    </div>
    <!-- 上传提示 -->
    <div class="el-upload__tip" slot="tip" v-if="showTip">
      {{$t('upload.please-upload')}}
      <template v-if="fileSize"> {{$t('upload.size-not-exceeding')}} <b style="color: #f56c6c">{{ fileSize }}MB</b> </template>
      <template v-if="fileType && fileType.length"> {{$t('upload.format-is')}} <b style="color: #f56c6c" :title="fileType.join('/')">{{ fileTypeHint }}</b> </template>
      {{$t('upload.files')}}
    </div>

    <el-dialog
      :visible.sync="dialogVisible"
      :title="$t('preview')"
      width="800"
      append-to-body
    >
      <img
        v-img-fallback
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
import {setHeader} from "@/utils/request";
import {
  applyDefaultImageOnError,
  buildImageUploadAccept,
  isAllowedImageUploadFile,
  WEB_IMAGE_UPLOAD_EXTENSIONS
} from '@/utils/upload-asset'
import { observeUploadFocusTarget, patchUploadFocusTarget } from '@/utils/upload-focus-tab'
import { bindNativeFilePickerResume, beginNativeFilePickerSession, endNativeFilePickerSession, isNativeFilePickerOpen } from '@/utils/native-file-picker'

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
    // 文件类型扩展名，默认常见 Web 图片格式
    fileType: {
      type: Array,
      default: () => WEB_IMAGE_UPLOAD_EXTENSIONS.slice(),
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
    let headers = {};
    setHeader('/common/upload', headers);
    return {
      number: 0,
      uploadList: [],
      pendingUploadCount: 0,
      dialogImageUrl: "",
      dialogVisible: false,
      hideUpload: false,
      baseUrl: process.env.VUE_APP_BASE_API,
      uploadImgUrl: process.env.VUE_APP_BASE_API + "/common/upload", // 上传的图片服务器地址
      headers: headers,
      fileList: [],
      /** 键盘漫游：当前高亮的图片/按钮索引（焦点保持在外框） */
      keyboardFocusIndex: 0,
      /** 原生文件选择框打开中：暂停外框方向键拦截 */
      nativeFilePickerOpen: false
    };
  },
  watch: {
    value: {
      handler(val) {
        if (this.pendingUploadCount > 0) {
          return;
        }
        this.syncFileListFromValue(val);
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
    /** 与 fileType 一致，限制系统文件选择对话框可选类型（上传前仍会 beforeUpload 校验） */
    uploadAccept() {
      return buildImageUploadAccept(this.fileType);
    },
    fileTypeHint() {
      const types = this.fileType;
      if (!types || !types.length) {
        return "";
      }
      if (types.length <= 5) {
        return types.join("/");
      }
      return types.slice(0, 5).join("/") + "/…";
    },
  },
  mounted() {
    this.bindThumbnailErrors()
    this.$nextTick(() => {
      const shell = this.$refs.focusTarget
      if (shell) observeUploadFocusTarget(shell)
      this.patchUploadTabStop()
    })
  },
  updated() {
    this.$nextTick(() => {
      this.bindThumbnailErrors()
      this.patchUploadTabStop()
      if (this.$refs.focusTarget && document.activeElement === this.$refs.focusTarget) {
        this.applyKeyboardFocusVisual()
      }
    })
  },
  beforeDestroy() {
    this.closeNativeFilePickerSession();
  },
  methods: {
    patchUploadTabStop() {
      patchUploadFocusTarget(this.$refs.focusTarget);
    },
    focus() {
      const shell = this.$refs.focusTarget;
      if (shell && typeof shell.focus === 'function') shell.focus();
    },
    getDefaultKeyboardFocusIndex() {
      const items = this.getKeyboardItems();
      if (!items.length) return 0;
      const addIdx = items.findIndex((item) => item.type === 'add');
      return addIdx >= 0 ? addIdx : 0;
    },
    onShellFocus() {
      this.keyboardFocusIndex = this.getDefaultKeyboardFocusIndex();
      this.$nextTick(() => this.applyKeyboardFocusVisual());
    },
    onShellBlur() {
      this.clearKeyboardFocusVisual();
    },
    /** 可键盘操作的项：已上传图片 → 添加按钮 → 粘贴按钮 */
    getKeyboardItems() {
      const shell = this.$refs.focusTarget;
      if (!shell) return [];
      const items = [];
      shell.querySelectorAll('.el-upload-list__item').forEach((el, index) => {
        items.push({ type: 'image', el, fileIndex: index });
      });
      if (this.fileList.length < this.limit) {
        shell.querySelectorAll('.update-button').forEach((btn) => {
          if (btn.querySelector('.el-icon-plus')) {
            items.push({ type: 'add', el: btn });
          } else if (btn.querySelector('.svg-icon')) {
            items.push({ type: 'paste', el: btn });
          }
        });
      }
      return items;
    },
    moveKeyboardFocus(dir) {
      const items = this.getKeyboardItems();
      if (!items.length) {
        this.keyboardFocusIndex = this.getDefaultKeyboardFocusIndex();
        this.applyKeyboardFocusVisual();
        return;
      }
      let i = this.keyboardFocusIndex < 0 ? 0 : this.keyboardFocusIndex;
      i += dir;
      if (i < 0) i = 0;
      if (i >= items.length) i = items.length - 1;
      this.keyboardFocusIndex = i;
      this.applyKeyboardFocusVisual();
    },
    clearKeyboardFocusVisual() {
      const shell = this.$refs.focusTarget;
      if (!shell) return;
      shell.querySelectorAll('.is-keyboard-focus').forEach((el) => {
        el.classList.remove('is-keyboard-focus');
      });
    },
    applyKeyboardFocusVisual() {
      const items = this.getKeyboardItems();
      this.clearKeyboardFocusVisual();
      if (!items.length) return;
      if (this.keyboardFocusIndex >= items.length) {
        this.keyboardFocusIndex = items.length - 1;
      }
      if (this.keyboardFocusIndex < 0) this.keyboardFocusIndex = 0;
      const item = items[this.keyboardFocusIndex];
      if (item && item.el) {
        item.el.classList.add('is-keyboard-focus');
        if (typeof item.el.scrollIntoView === 'function') {
          item.el.scrollIntoView({ block: 'nearest', inline: 'nearest' });
        }
      }
    },
    prepareNativeFilePicker() {
      beginNativeFilePickerSession();
      this.nativeFilePickerOpen = true;
      const shell = this.$refs.focusTarget;
      if (shell && typeof shell.blur === 'function') shell.blur();
      if (!this._nativeFilePickerResumeHandler) {
        this._nativeFilePickerResumeHandler = bindNativeFilePickerResume(() => {
          this._nativeFilePickerResumeHandler = null;
          this.nativeFilePickerOpen = false;
        });
      }
    },
    closeNativeFilePickerSession() {
      endNativeFilePickerSession();
      this.nativeFilePickerOpen = false;
      if (this._nativeFilePickerResumeHandler) {
        window.removeEventListener('focus', this._nativeFilePickerResumeHandler, true);
        this._nativeFilePickerResumeHandler = null;
      }
    },
    openFilePicker() {
      this.prepareNativeFilePicker();
      const upload = this.$refs.imageUpload;
      if (!upload || !upload.$el) return;
      const input = upload.$el.querySelector('input[type="file"]');
      if (input && typeof input.click === 'function') input.click();
    },
    async pasteClipboardImage() {
      if (this.fileList.length >= this.limit) {
        this.handleExceed();
        return;
      }
      this.dialogVisible = false;
      await this.pasteFromClipboardApi();
    },
    /** 从 ClipboardEvent.clipboardData 提取图片（Cmd/Ctrl+V 的 paste 事件） */
    extractImageFilesFromPasteEvent(e) {
      const files = [];
      const cd = e && e.clipboardData;
      if (!cd) return files;
      if (cd.files && cd.files.length) {
        for (let i = 0; i < cd.files.length; i++) {
          const f = cd.files[i];
          if (f && f.type && f.type.startsWith('image/')) files.push(f);
        }
      }
      if (!files.length && cd.items) {
        for (let i = 0; i < cd.items.length; i++) {
          const item = cd.items[i];
          if (item.kind === 'file' && item.type.startsWith('image/')) {
            const f = item.getAsFile();
            if (f) files.push(f);
          }
        }
      }
      return files;
    },
    onShellPaste(e) {
      if (this.dialogVisible || e.isComposing) return;
      const files = this.extractImageFilesFromPasteEvent(e);
      if (!files.length) return;
      e.preventDefault();
      e.stopPropagation();
      this.uploadClipboardFiles(files);
    },
    async uploadClipboardFiles(files) {
      if (!files || !files.length) return;
      let remaining = this.limit - this.fileList.length;
      if (remaining <= 0) {
        this.handleExceed();
        return;
      }
      let uploaded = 0;
      let nextList = [...this.fileList];
      for (const file of files) {
        if (remaining <= 0) {
          this.handleExceed();
          break;
        }
        if (!isAllowedImageUploadFile(file, this.fileType)) continue;
        if (this.fileSize && file.size / 1024 / 1024 >= this.fileSize) {
          this.$modal.msgError(strFormat(i18n.t('upload.img-size-exceeds-range'), this.fileSize));
          continue;
        }
        try {
          const formData = new FormData();
          formData.append('file', file);
          const res = await upload(formData);
          nextList.push({ name: res.fileName, url: res.fileName });
          this.$emit('input', this.listToString(nextList));
          uploaded++;
          remaining--;
        } catch (err) {
          console.error(err);
        }
      }
      if (uploaded === 0) {
        this.$message.warning(this.$i18n.t('upload.clipboard-not-img').toString());
      }
    },
    async pasteFromClipboardApi() {
      try {
        const clipboardItems = await navigator.clipboard.read();
        const files = [];
        for (const clipboardItem of clipboardItems) {
          for (const type of clipboardItem.types) {
            if (!type.startsWith('image/')) continue;
            const blob = await clipboardItem.getType(type);
            const ext = (type.split('/')[1] || 'png').replace('jpeg', 'jpg');
            files.push(new File([blob], `clipboard.${ext}`, { type: blob.type || type }));
          }
        }
        if (!files.length) {
          this.$message.warning(this.$i18n.t('upload.clipboard-not-img').toString());
          return;
        }
        await this.uploadClipboardFiles(files);
      } catch (err) {
        console.error(err.name, err.message);
      }
    },
    activateKeyboardItem() {
      const items = this.getKeyboardItems();
      const item = items[this.keyboardFocusIndex];
      if (!item) return;
      if (item.type === 'image') {
        const file = this.fileList[item.fileIndex];
        if (file) this.handlePictureCardPreview(file);
      } else if (item.type === 'add') {
        this.openFilePicker();
      } else if (item.type === 'paste') {
        this.pasteClipboardImage();
      }
    },
    deleteActiveImage() {
      const items = this.getKeyboardItems();
      const item = items[this.keyboardFocusIndex];
      if (!item || item.type !== 'image') return;
      const file = this.fileList[item.fileIndex];
      if (!file || !this.$refs.imageUpload) return;
      this.$refs.imageUpload.handleRemove(file);
      this.$nextTick(() => {
        const next = this.getKeyboardItems();
        if (this.keyboardFocusIndex >= next.length) {
          this.keyboardFocusIndex = Math.max(0, next.length - 1);
        }
        this.applyKeyboardFocusVisual();
      });
    },
    onFocusShellKeydown(e) {
      if (this.nativeFilePickerOpen || isNativeFilePickerOpen() || this.dialogVisible || e.isComposing) return;
      const key = e.key;
      if (key === 'ArrowLeft') {
        e.preventDefault();
        this.moveKeyboardFocus(-1);
        return;
      }
      if (key === 'ArrowRight') {
        e.preventDefault();
        this.moveKeyboardFocus(1);
        return;
      }
      if (key === 'Enter' || key === ' ' || key === 'Spacebar' || e.code === 'Space') {
        e.preventDefault();
        this.activateKeyboardItem();
        return;
      }
      if (key === 'Delete' || key === 'Backspace') {
        const items = this.getKeyboardItems();
        const item = items[this.keyboardFocusIndex];
        if (item && item.type === 'image') {
          e.preventDefault();
          this.deleteActiveImage();
        }
        return;
      }
      const isMod = e.metaKey || e.ctrlKey;
      if (isMod && !e.altKey && (key === 'v' || key === 'V')) {
        if (this.fileList.length >= this.limit) return;
        e.preventDefault();
        e.stopPropagation();
        this.pasteClipboardImage();
        return;
      }
    },
    syncFileListFromValue(val) {
      if (val) {
        const list = Array.isArray(val) ? val : String(val).split(',');
        this.fileList = list.filter(Boolean).map(item => {
          if (typeof item === "string") {
            if (item.indexOf(this.baseUrl) === -1) {
              return { name: this.baseUrl + item, url: this.baseUrl + item };
            }
            return { name: item, url: item };
          }
          return item;
        });
      } else {
        this.fileList = [];
      }
    },
    finishUploadLoading() {
      if (this.pendingUploadCount > 0) {
        this.pendingUploadCount--;
        this.$modal.closeLoading();
      }
      if (this.pendingUploadCount <= 0) {
        this.pendingUploadCount = 0;
        this.$modal.closeAllLoading();
      }
    },
    bindThumbnailErrors() {
      if (!this.$el) {
        return
      }
      this.$el.querySelectorAll('.el-upload-list__item-thumbnail').forEach((img) => {
        applyDefaultImageOnError(img)
      })
    },
    // var imageUrl = URL.createObjectURL(blob);
    async clipboardImageHandle(event) {
      if (event) event.stopPropagation();
      await this.pasteClipboardImage();
    },
    // 上传前loading加载
    handleBeforeUpload(file) {
      this.closeNativeFilePickerSession();
      const isImg = isAllowedImageUploadFile(file, this.fileType);

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
      this.pendingUploadCount++;
      this.$modal.loading(this.$i18n.t('upload.img-loading'));
      this.number++;
    },
    // 文件个数超出
    handleExceed() {
      this.$modal.msgError(strFormat(i18n.t('upload.number-exceeds-range'),this.limit));
    },
    // 上传成功回调
    handleUploadSuccess(res, file) {
      const fileName = res && (res.fileName || (res.data && res.data.fileName));
      if (res && res.code === 200 && fileName) {
        this.uploadList.push({ name: fileName, url: fileName });
        this.uploadedSuccessfully();
      } else {
        this.number = Math.max(0, this.number - 1);
        this.finishUploadLoading();
        this.$modal.msgError((res && res.msg) || this.$i18n.t('upload.img-fail'));
        if (this.$refs.imageUpload) {
          this.$refs.imageUpload.handleRemove(file);
        }
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
      this.number = Math.max(0, this.number - 1);
      this.finishUploadLoading();
      this.$modal.msgError(this.$i18n.t('upload.img-fail'));
      this.uploadedSuccessfully();
    },
    // 上传结束处理
    uploadedSuccessfully() {
      if (this.number > 0 && this.uploadList.length === this.number) {
        this.fileList = this.fileList.concat(this.uploadList);
        this.uploadList = [];
        this.number = 0;
        this.pendingUploadCount = 0;
        this.$emit("input", this.listToString(this.fileList));
        this.$modal.closeAllLoading();
        this.syncFileListFromValue(this.value);
      } else if (this.pendingUploadCount > 0) {
        this.pendingUploadCount = 0;
        this.$modal.closeAllLoading();
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
.component-upload-image {
  overflow: visible;
}
.cat2bug-upload-focus-target.component-upload-image-focus-target {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: flex-start;
  width: fit-content;
  max-width: 100%;
  overflow: visible;
  border: 1px solid transparent;
  border-radius: var(--cat2bug-border-radius, 4px);
  outline: none;
  box-sizing: border-box;
  &:focus,
  &:focus-visible,
  &:focus-within {
    outline: none;
    border-color: transparent;
    box-shadow: none;
  }
  ::v-deep .el-button:focus,
  ::v-deep .update-button:focus {
    outline: none;
  }
}
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
  background-color: var(--upload-picture-add-bg, #fbfdff);
  border: 1px dashed var(--upload-picture-add-border, #c0ccda);
  border-radius: var(--cat2bug-border-radius, 4px);
  cursor: pointer;
  font-size: 26px;
  flex-shrink: 0;
}
::v-deep .el-upload-list--picture-card .el-upload-list__item {
  border-radius: var(--cat2bug-border-radius, 4px);
  overflow: hidden;
}
::v-deep .el-upload-list--picture-card .el-upload-list__item-thumbnail {
  border-radius: var(--cat2bug-border-radius, 4px);
}
::v-deep .el-upload-list--picture-card .el-upload-list__item.is-keyboard-focus {
  outline: 2px solid var(--cat2bug-field-focus-color, #409eff);
  outline-offset: 2px;
  z-index: 1;
}
.update-button.is-keyboard-focus {
  outline: 2px solid var(--cat2bug-field-focus-color, #409eff);
  outline-offset: 2px;
  border-color: var(--cat2bug-field-focus-color, #1890ff);
}
.update-button:hover {
  border-color: #1890ff;
  color: #1890ff;
}
.flex-row {
  display: inline-flex;
  flex-direction: row;
  gap: 8px;
}
::v-deep.cat2bug-upload > .el-upload--picture-card {
  width: max-content !important;
  max-width: 100%;
  display: inline-flex;
  flex-direction: row;
  flex-wrap: nowrap;
  gap: 8px;
  border: none;
  .update-button + .update-button {
    margin-left: 0;
  }
}
</style>

