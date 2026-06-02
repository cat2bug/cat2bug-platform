import { applyDefaultImageOnError } from '@/utils/upload-asset'

/**
 * 原生 img：加载失败时统一回退 DEFAULT_IMAGE 并套用 cat2bug-image-error__img 样式。
 * 用法：<img v-img-fallback :src="url" />
 */
export default {
  inserted(el) {
    if (el && el.tagName === 'IMG') {
      applyDefaultImageOnError(el)
    }
  },
  componentUpdated(el) {
    if (el && el.tagName === 'IMG') {
      applyDefaultImageOnError(el)
    }
  }
}
