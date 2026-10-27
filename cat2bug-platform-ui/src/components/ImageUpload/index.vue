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
        v-model:file-list="fileList"
        :on-preview="handlePictureCardPreview"
        class="cat2bug-upload"
        :class="{hide: successFileCount >= limit}"
      >
        <template v-slot:trigger>
          <el-button class="update-button update-button-add" v-if="successFileCount < limit" :style="buttonStyle" @click.capture="prepareNativeFilePicker">
            <legacy-icon icon="el-icon-plus"></legacy-icon>
          </el-button>
          <el-button v-if="isShowClipboardButton && successFileCount < limit" class="update-button" :style="buttonStyle" @click="clipboardImageHandle">
            <svg-icon icon-class="clipboard" />
          </el-button>
        </template>
        <template v-if="showTip" #tip>
          <div class="el-upload__tip">
            {{$t('upload.please-upload')}}
            <template v-if="fileSize"> {{$t('upload.size-not-exceeding')}} <b style="color: #f56c6c">{{ fileSize }}MB</b> </template>
            <template v-if="fileType && fileType.length"> {{$t('upload.format-is')}} <b style="color: #f56c6c" :title="fileType.join('/')">{{ fileTypeHint }}</b> </template>
            {{$t('upload.files')}}
          </div>
        </template>
      </el-upload>
    </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
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
  emits: ['update:modelValue', 'input'],
  props: {
    modelValue: [String, Object, Array],
    /** @deprecated Vue2 兼容 */
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
      pendingUploadCount: 0,
      dialogImageUrl: "",
      dialogVisible: false,
      hideUpload: false,
      baseUrl: process.env.VUE_APP_BASE_API,
      uploadImgUrl: process.env.VUE_APP_BASE_API + "/common/upload", // 上传的图片服务器地址
      headers: headers,
      fileList: [],
      /** 抑制 value→fileList 回写，避免上传过程中冲掉 el-upload 已追加的项 */
      syncingFromUpload: false,
      /** 键盘漫游：当前高亮的图片/按钮索引（焦点保持在外框） */
      keyboardFocusIndex: 0,
      /** 原生文件选择框打开中：暂停外框方向键拦截 */
      nativeFilePickerOpen: false
    };
  },
  watch: {
    boundValue: {
      handler(val) {
        if (this.pendingUploadCount > 0 || this.syncingFromUpload) {
          return;
        }
        this.syncFileListFromValue(val);
      },
      deep: true,
      immediate: true
    }
  },
  computed: {
    boundValue() {
      if (this.modelValue !== undefined && this.modelValue !== null) {
        return this.modelValue;
      }
      return this.value;
    },
    /** 已成功（有服务端路径）的图片数，上传中的 blob 项不计入 limit */
    successFileCount() {
      return this.fileList.filter((f) => f && f.url && !String(f.url).startsWith('blob:')).length;
    },
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
    emitValue(val) {
      this.$emit('update:modelValue', val);
      this.$emit('input', val);
    },
    patchUploadTabStop() {
      patchUploadFocusTarget(this.$refs.focusTarget);
    },
    focus() {
      const shell = this.$refs.focusTarget;
      if (!shell || typeof shell.focus !== 'function') return;
      shell.focus();
      this.$nextTick(() => {
        if (document.activeElement !== shell) return;
        this.keyboardFocusIndex = this.getDefaultKeyboardFocusIndex();
        this.applyKeyboardFocusVisual();
      });
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
      if (this.successFileCount < this.limit) {
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
      if (this.successFileCount >= this.limit) {
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
      let remaining = this.limit - this.successFileCount;
      if (remaining <= 0) {
        this.handleExceed();
        return;
      }
      let uploaded = 0;
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
          const fileName = res && (res.fileName || (res.data && res.data.fileName));
          if (!fileName) continue;
          this.fileList = [...this.fileList, this.toUploadFileItem(fileName)];
          uploaded++;
          remaining--;
        } catch (err) {
          console.error(err);
        }
      }
      if (uploaded > 0) {
        this.commitFileListValue();
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
        if (this.successFileCount >= this.limit) return;
        e.preventDefault();
        e.stopPropagation();
        this.pasteClipboardImage();
        return;
      }
    },
    toUploadFileItem(fileName, extra = {}) {
      const path = String(fileName || '');
      const url = path.indexOf(this.baseUrl) === -1 ? this.baseUrl + path : path;
      return {
        name: url,
        url,
        status: 'success',
        ...extra
      };
    },
    /** 相对路径（入库）；忽略上传中的 blob */
    toStoredPath(url) {
      if (!url || String(url).startsWith('blob:')) return '';
      const s = String(url);
      return s.indexOf(this.baseUrl) === 0 ? s.slice(this.baseUrl.length) : s;
    },
    syncFileListFromValue(val) {
      if (val) {
        const list = Array.isArray(val) ? val : String(val).split(',');
        const prevByPath = new Map();
        (this.fileList || []).forEach((f) => {
          const p = this.toStoredPath(f && f.url);
          if (p && f.uid != null) prevByPath.set(p, f.uid);
        });
        this.fileList = list.filter(Boolean).map((item) => {
          if (typeof item !== 'string') return item;
          const path = item.trim();
          if (!path) return null;
          const url = path.indexOf(this.baseUrl) === -1 ? this.baseUrl + path : path;
          const stored = this.toStoredPath(url);
          const uid = prevByPath.get(stored);
          return uid != null
            ? { name: url, url, status: 'success', uid }
            : { name: url, url, status: 'success' };
        }).filter(Boolean);
      } else {
        this.fileList = [];
      }
    },
    commitFileListValue() {
      this.syncingFromUpload = true;
      const nextValue = this.listToString(this.fileList);
      this.emitValue(nextValue);
      this.$nextTick(() => {
        this.syncingFromUpload = false;
      });
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
      if (this.successFileCount >= this.limit) {
        this.handleExceed();
        return false;
      }
      this.pendingUploadCount++;
      this.$modal.loading(this.$i18n.t('upload.img-loading'));
      return true;
    },
    // 文件个数超出
    handleExceed() {
      this.$modal.msgError(strFormat(i18n.t('upload.number-exceeds-range'),this.limit));
    },
    // 上传成功：就地替换 el-upload 已追加的临时项为服务端 URL（追加而非覆盖上一张）
    handleUploadSuccess(res, file) {
      const fileName = res && (res.fileName || (res.data && res.data.fileName));
      if (res && res.code === 200 && fileName) {
        const item = this.toUploadFileItem(fileName, { uid: file && file.uid });
        const idx = file && file.uid != null
          ? this.fileList.findIndex((f) => f.uid === file.uid)
          : -1;
        if (idx >= 0) {
          const next = this.fileList.slice();
          next[idx] = { ...this.fileList[idx], ...item };
          this.fileList = next;
        } else {
          this.fileList = [...this.fileList, item];
        }
        this.commitFileListValue();
        this.finishUploadLoading();
      } else {
        this.finishUploadLoading();
        this.$modal.msgError((res && res.msg) || this.$i18n.t('upload.img-fail'));
        this.removeFileSilent(file);
      }
    },
    /** 失败项静默移除，避免再弹「确认删除」 */
    removeFileSilent(file) {
      if (!file) return;
      const uid = file.uid;
      const before = this.fileList.length;
      if (uid != null) {
        this.fileList = this.fileList.filter((f) => f.uid !== uid);
      } else if (file.url) {
        this.fileList = this.fileList.filter((f) => f.url !== file.url);
      }
      if (this.fileList.length !== before) {
        this.commitFileListValue();
      }
    },
    handleBeforeRemove(file) {
      // 上传失败/未成功的临时项直接删，不打断
      if (!file || file.status === 'fail' || (file.url && String(file.url).startsWith('blob:') && file.status !== 'success')) {
        return true;
      }
      const stored = this.toStoredPath(file.url);
      if (!stored) return true;
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
    // 删除图片（el-upload 已从 fileList 移除，这里只回写 v-model）
    handleDelete() {
      this.commitFileListValue();
    },
    // 上传失败
    handleUploadError() {
      this.finishUploadLoading();
      this.$modal.msgError(this.$i18n.t('upload.img-fail'));
    },
    // 预览
    handlePictureCardPreview(file) {
      this.dialogImageUrl = file.url;
      this.dialogVisible = true;
    },
    // 对象转成指定字符串分隔（跳过上传中的 blob）
    listToString(list, separator) {
      separator = separator || ",";
      const parts = [];
      (list || []).forEach((item) => {
        const path = this.toStoredPath(item && item.url);
        if (path) parts.push(path);
      });
      return parts.join(separator);
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
  :deep(.el-button:focus),
  :deep(.update-button:focus){
    outline: none;
  }
}
// .el-upload--picture-card 控制加号部分
:deep(.hide .el-upload--picture-card){
  display: none !important;
}
// 去掉动画效果
:deep(.el-list-enter-active),
:deep(.el-list-leave-active){
  transition: all 0s;
}

:deep(.el-list-enter),
:deep(.el-list-leave-active) {
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
:deep(.el-upload-list--picture-card .el-upload-list__item){
  width: 148px;
  height: 148px;
  border-radius: var(--cat2bug-border-radius, 4px);
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
:deep(.el-upload-list--picture-card .el-upload-list__item-thumbnail){
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  border-radius: var(--cat2bug-border-radius, 4px);
}
/* 加载失败回退 default-image：保留 72% 占位尺寸，并在卡片内居中 */
:deep(.el-upload-list--picture-card .el-upload-list__item-thumbnail.cat2bug-image-error__img){
  width: auto;
  height: auto;
  max-width: 72%;
  max-height: 72%;
  object-fit: contain;
  object-position: center center;
  margin: auto;
}
:deep(.el-upload-list--picture-card .el-upload-list__item.is-keyboard-focus){
  outline: none !important;
  border: 1px dashed var(--cat2bug-field-focus-color, #1890ff) !important;
  z-index: 1;
}
:deep(.update-button.is-keyboard-focus){
  outline: none !important;
  border: 1px dashed var(--cat2bug-field-focus-color, #1890ff) !important;
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
// Element Plus：触发器在 .el-upload-list--picture-card 内，旧版为 .cat2bug-upload 直接子节点
:deep(.cat2bug-upload){
  .el-upload-list--picture-card {
    width: max-content !important;
    max-width: 100%;
    display: inline-flex !important;
    flex-wrap: nowrap;
    vertical-align: top;
  }

  .el-upload-list--picture-card > .el-upload--picture-card,
  > .el-upload--picture-card {
    width: max-content !important;
    max-width: 100%;
    height: auto !important;
    display: inline-flex !important;
    flex-direction: row;
    flex-wrap: nowrap;
    gap: 8px;
    border: none;
    background: transparent;

    .update-button + .update-button {
      margin-left: 0;
    }
  }
}
</style>

