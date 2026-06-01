<template>
  <div>
    <div class="user-info-head" @click="editCropper()">
      <cat2-bug-avatar
        :member="member"
        :size="120"
        class="profile-user-avatar"
      />
    </div>
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body @opened="modalOpened"  @close="closeDialog">
      <el-row>
        <el-col :xs="24" :md="12" :style="{height: '350px'}">
          <vue-cropper
            ref="cropper"
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
            @realTime="realTime"
            @imgLoad="onCropperImgLoad"
            v-if="visible"
          />
        </el-col>
        <el-col :xs="24" :md="12" :style="{height: '350px'}">
          <div class="avatar-upload-preview">
            <img :src="previews.url" :style="previews.img" />
          </div>
        </el-col>
      </el-row>
      <br />
      <el-row>
        <el-col :lg="2" :sm="3" :xs="3">
          <el-upload action="#" :http-request="requestUpload" :show-file-list="false" :before-upload="beforeUpload">
            <el-button size="small">
              {{ $t('select') }}
              <i class="el-icon-upload el-icon--right"></i>
            </el-button>
          </el-upload>
        </el-col>
        <el-col :lg="{span: 1, offset: 2}" :sm="2" :xs="2">
          <el-button icon="el-icon-plus" size="small" @click="changeScale(1)"></el-button>
        </el-col>
        <el-col :lg="{span: 1, offset: 1}" :sm="2" :xs="2">
          <el-button icon="el-icon-minus" size="small" @click="changeScale(-1)"></el-button>
        </el-col>
        <el-col :lg="{span: 1, offset: 1}" :sm="2" :xs="2">
          <el-button icon="el-icon-refresh-left" size="small" @click="rotateLeft()"></el-button>
        </el-col>
        <el-col :lg="{span: 1, offset: 1}" :sm="2" :xs="2">
          <el-button icon="el-icon-refresh-right" size="small" @click="rotateRight()"></el-button>
        </el-col>
        <el-col :lg="{span: 2, offset: 6}" :sm="2" :xs="2">
          <el-button type="primary" size="small" @click="uploadImg()">{{ $t('submit') }}</el-button>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script>
import store from "@/store";
import { VueCropper } from "vue-cropper";
import { uploadAvatar } from "@/api/system/user";
import { debounce } from '@/utils'
import Cat2BugAvatar from "@/components/Cat2BugAvatar";
import { isNonEmptyUploadPath, resolveUploadUrl } from '@/utils/upload-asset'

export default {
  components: { VueCropper, Cat2BugAvatar },
  props: {
    profileUser: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      // 是否显示弹出层
      open: false,
      // 是否显示cropper
      visible: false,
      // 弹出层标题
      title: "修改头像",
      options: {
        name: store.getters.name,  // 当前用户名
        avatar: '', // vue-cropper 需完整 URL，打开弹窗时再解析
        autoCrop: true, // 是否默认生成截图框
        autoCropWidth: 200, // 默认生成截图框宽度
        autoCropHeight: 200, // 默认生成截图框高度
        fixed: true, // 固定 1:1 比例
        fixedNumber: [1, 1],
        fixedBox: true, // 固定截图框大小 不允许改变
        centerBox: true, // 截图框限制在图片内
        canMoveBox: false, // 固定截图框，拖动图片调整
        outputType:"png" // 默认生成截图为PNG格式
      },
      avatar: store.getters.avatar, //裁剪图片的地址
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
        const next = (user && user.avatar) || store.getters.avatar || ''
        if (next !== this.avatar) {
          this.avatar = next
        }
      },
      immediate: true,
      deep: true
    }
  },
  methods: {
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
      const path = this.avatar || store.getters.avatar || ''
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
      const cropper = this.$refs.cropper
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
        const scale = Math.min(cropW / trueWidth, cropH / trueHeight)
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
      this.$refs.cropper.refresh();
    },
    // 覆盖默认的上传行为
    requestUpload() {
    },
    // 向左旋转
    rotateLeft() {
      this.$refs.cropper.rotateLeft();
    },
    // 向右旋转
    rotateRight() {
      this.$refs.cropper.rotateRight();
    },
    // 图片缩放
    changeScale(num) {
      num = num || 1;
      this.$refs.cropper.changeScale(num);
    },
    // 上传预处理
    beforeUpload(file) {
      if (file.type.indexOf("image/") == -1) {
        this.$modal.msgError(this.$i18n.t('member.upload-file-format-exception'));
      } else {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => {
          this.options.avatar = reader.result;
        };
      }
    },
    // 上传图片
    uploadImg() {
      this.$refs.cropper.getCropBlob(data => {
        let formData = new FormData();
        formData.append("avatarfile", data);
        uploadAvatar(formData).then(response => {
          const imgUrl = response && response.imgUrl
          if (!imgUrl) {
            this.$modal.msgError(this.$i18n.t('member.upload-file-format-exception'))
            return
          }
          this.avatar = imgUrl
          store.commit('SET_AVATAR', imgUrl)
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
      window.removeEventListener("resize", this.resizeHandler)
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
</style>
