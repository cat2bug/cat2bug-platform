<template>
  <div>
    <div class="user-info-head" @click="editCropper()">
      <cat2-bug-avatar
        :member="member"
        :size="120"
        class="profile-user-avatar"
      />
    </div>
    <el-dialog :title="title" v-model="open" width="800px" append-to-body @opened="modalOpened"  @close="closeDialog">
      <el-row>
        <el-col :xs="24" :md="12" :style="{height: '350px'}">
          <VueCropper
            ref="cropper"
            :key="cropperKey"
            :img="options.avatar"
            :info="true"
            :autoCrop="options.autoCrop"
            :autoCropWidth="options.autoCropWidth"
            :autoCropHeight="options.autoCropHeight"
            :fixed="options.fixed"
            :fixedNumber="options.fixedNumber"
            :fixedBox="options.fixedBox"
            :centerBox="options.centerBox"
            :canMoveBox="options.canMoveBox"
            :outputType="options.outputType"
            :mode="options.mode"
            @realTime="realTime"
            @imgLoad="onCropperImgLoad"
            v-if="visible"
          />
        </el-col>
        <el-col :xs="24" :md="12" class="avatar-preview-panel">
          <div class="avatar-preview-panel__content">
            <div class="avatar-upload-preview">
              <img :src="previews.url" :style="previews.img" />
            </div>
            <el-button type="primary" size="small" @click="uploadImg()">{{ $t('submit') }}</el-button>
          </div>
        </el-col>
      </el-row>
      <br />
      <el-row>
        <el-col :xs="24" :md="12">
          <div class="avatar-cropper-tools">
            <el-upload action="#" :http-request="requestUpload" :show-file-list="false" :before-upload="beforeUpload">
              <el-button size="small">
                {{ $t('select') }}
                <legacy-icon icon="el-icon-upload"></legacy-icon>
              </el-button>
            </el-upload>
            <el-button :icon="$legacyIcon('el-icon-plus')" size="small" @click="changeScale(1)"></el-button>
            <el-button :icon="$legacyIcon('el-icon-minus')" size="small" @click="changeScale(-1)"></el-button>
            <el-button :icon="$legacyIcon('el-icon-refresh-left')" size="small" @click="rotateLeft()"></el-button>
            <el-button :icon="$legacyIcon('el-icon-refresh-right')" size="small" @click="rotateRight()"></el-button>
          </div>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script>
import { VueCropper } from "vue-cropper/dist/vue-cropper.es.js";
import "vue-cropper/dist/index.css";
import { uploadAvatar } from "@/api/system/user";
import { debounce } from '@/utils'
import Cat2BugAvatar from "@/components/Cat2BugAvatar";
import { isNonEmptyUploadPath, resolveUploadUrl } from '@/utils/upload-asset'
import { useUserStore } from '@/stores/user'

export default {
  components: { VueCropper, Cat2BugAvatar },
  props: {
    profileUser: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    const userStore = useUserStore()
    return {
      // 是否显示弹出层
      open: false,
      // 是否显示cropper
      visible: false,
      // 弹出层标题
      title: "修改头像",
      options: {
        name: userStore.name,  // 当前用户名
        avatar: '', // vue-cropper 需完整 URL，打开弹窗时再解析
        autoCrop: true, // 是否默认生成截图框
        autoCropWidth: 200, // 默认生成截图框宽度
        autoCropHeight: 200, // 默认生成截图框高度
        fixed: true, // 固定 1:1 比例
        fixedNumber: [1, 1],
        fixedBox: true, // 固定截图框大小 不允许改变
        centerBox: true, // 截图框限制在图片内
        canMoveBox: false, // 固定截图框，拖动图片调整
        mode: 'cover', // 默认铺满裁剪框，避免头像留白
        outputType:"png" // 默认生成截图为PNG格式
      },
      avatar: userStore.avatar, //裁剪图片的地址
      cropperKey: 0,
      previews: {},
      resizeHandler: null
    };
  },
  computed: {
    member() {
      const profile = this.profileUser || {}
      return {
        avatar: this.avatar,
        nickName: profile.nickName,
        userName: profile.userName || this.options.name
      }
    }
  },
  watch: {
    profileUser: {
      handler(user) {
        const next = (user && user.avatar) || useUserStore().avatar || ''
        if (next !== this.avatar) {
          this.avatar = next
        }
      },
      immediate: true,
      deep: true
    }
  },
  methods: {
    getCropper() {
      return this.$refs.cropper
    },
    resolveCropperImage(path) {
      if (!isNonEmptyUploadPath(path)) {
        return ''
      }
      const trimmed = path.trim()
      if (trimmed.startsWith('data:')) {
        return trimmed
      }
      return resolveUploadUrl(trimmed, process.env.VUE_APP_BASE_API)
    },
    syncCropperImage() {
      const path = this.avatar || useUserStore().avatar || ''
      this.options.avatar = this.resolveCropperImage(path)
    },
    onCropperImgLoad(status) {
      if (status !== 'success') {
        return
      }
      this.$nextTick(() => {
        this.fitImageToCropBox()
      })
    },
    fitImageToCropBox() {
      const cropper = this.getCropper()
      if (!cropper || !cropper.imgs) {
        return
      }
      cropper.goAutoCrop()
      this.$nextTick(() => {
        const {
          trueWidth,
          trueHeight,
          cropW,
          cropH,
          cropOffsertX,
          cropOffsertY
        } = cropper
        if (!trueWidth || !trueHeight || !cropW || !cropH) {
          return
        }
        const scale = Math.max(cropW / trueWidth, cropH / trueHeight)
        cropper.scale = scale
        const displayW = trueWidth * scale
        const displayH = trueHeight * scale
        cropper.x = cropOffsertX + (cropW - displayW) / 2 - trueWidth * (1 - scale) / 2
        cropper.y = cropOffsertY + (cropH - displayH) / 2 - trueHeight * (1 - scale) / 2
        if (typeof cropper.showPreview === 'function') {
          cropper.showPreview()
        }
      })
    },
    // 编辑头像
    editCropper() {
      this.syncCropperImage()
      this.open = true;
    },
    // 打开弹出层结束时的回调
    modalOpened() {
      this.syncCropperImage()
      this.visible = true;
      if (!this.resizeHandler) {
        this.resizeHandler = debounce(() => {
          this.refresh()
        }, 100)
      }
      window.addEventListener("resize", this.resizeHandler)
    },
    // 刷新组件
    refresh() {
      const cropper = this.getCropper()
      if (cropper && typeof cropper.refresh === 'function') {
        cropper.refresh()
      }
    },
    // 覆盖默认的上传行为
    requestUpload() {
    },
    // 向左旋转
    rotateLeft() {
      this.getCropper()?.rotateLeft?.()
    },
    // 向右旋转
    rotateRight() {
      this.getCropper()?.rotateRight?.()
    },
    // 图片缩放
    changeScale(num) {
      num = num || 1;
      this.getCropper()?.changeScale?.(num)
    },
    // 上传预处理
    beforeUpload(file) {
      if (file.type.indexOf("image/") == -1) {
        this.$modal.msgError(this.$i18n.t('member.upload-file-format-exception'));
        return false;
      }
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => {
        this.options.avatar = reader.result;
        this.cropperKey += 1;
      };
      return false;
    },
    // 上传图片
    uploadImg() {
      const cropper = this.getCropper()
      if (!cropper || typeof cropper.getCropBlob !== 'function') {
        return
      }
      cropper.getCropBlob(data => {
        let formData = new FormData();
        formData.append("avatarfile", data);
        uploadAvatar(formData).then(response => {
          const imgUrl = response && response.imgUrl
          if (!imgUrl) {
            this.$modal.msgError(this.$i18n.t('member.upload-file-format-exception'))
            return
          }
          this.avatar = imgUrl
          useUserStore().avatar = imgUrl
          this.$emit('updated', imgUrl)
          this.$modal.msgSuccess(this.$i18n.t('modify-success'))
          this.visible = false
          this.open = false
        })
      });
    },
    // 实时预览
    realTime(data) {
      this.previews = data;
    },
    // 关闭窗口
    closeDialog() {
      this.options.avatar = ''
      this.visible = false
      if (this.resizeHandler) {
        window.removeEventListener("resize", this.resizeHandler)
      }
    }
  }
};
</script>
<style scoped lang="scss">
.user-info-head {
  position: relative;
  display: inline-block;
  cursor: pointer;

  ::v-deep .profile-user-avatar.member-avatar .el-avatar {
    width: 120px;
    height: 120px;
    font-size: 48px;
    border: 2px solid var(--border-color-light) !important;
    box-sizing: border-box;
    transition: border-color 0.2s ease;
  }
}

.user-info-head:hover::after {
  content: '+';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  font-size: 32px;
  font-style: normal;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  cursor: pointer;
  line-height: 120px;
  border-radius: 50%;
  pointer-events: none;
}

.avatar-preview-panel {
  height: 350px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-preview-panel__content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.avatar-preview-panel :deep(.avatar-upload-preview) {
  position: static;
  top: auto;
  left: auto;
  transform: none;
}

.avatar-cropper-tools {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
