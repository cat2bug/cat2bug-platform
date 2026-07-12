<template>
  <div class="cat2bug-dynamic-image-block">
    <div class="cat2bug-dynamic-image-block__header">
      <svg-icon icon-class="mk-image" />
      <span>{{ $t('project.icon') }}</span>
      <el-popover placement="bottom" :width="320" trigger="click">
        <template #reference>
          <el-button size="small" text type="primary">{{ $t('edit') }}</el-button>
        </template>
        <el-form label-width="72px" size="small">
          <el-form-item :label="$t('field')">
            <el-input :model-value="path" @update:model-value="onPathChange" />
          </el-form-item>
          <el-form-item label="style">
            <el-input
              type="textarea"
              :rows="2"
              :model-value="style"
              placeholder="width:200px;height:200px;"
              @update:model-value="onStyleChange"
            />
          </el-form-item>
        </el-form>
      </el-popover>
      <el-button size="small" text type="danger" @click="$emit('remove')">{{ $t('delete') }}</el-button>
    </div>
    <code class="cat2bug-dynamic-image-block__token">{{ previewToken }}</code>
  </div>
</template>

<script>
export default {
  name: 'DynamicImageBlock',
  props: {
    path: {
      type: String,
      default: 'api.project.projectIcon'
    },
    style: {
      type: String,
      default: 'width:200px;height:200px;'
    }
  },
  emits: ['update:path', 'update:style', 'remove'],
  computed: {
    previewToken() {
      return `$image["style":"${this.style}"]{${this.path}}`
    }
  },
  methods: {
    onPathChange(value) {
      this.$emit('update:path', value)
    },
    onStyleChange(value) {
      this.$emit('update:style', value)
    }
  }
}
</script>

<style scoped>
.cat2bug-dynamic-image-block {
  border: 1px dashed var(--el-color-info-light-5);
  border-radius: 6px;
  padding: 10px 12px;
  margin: 8px 0;
  background: var(--el-fill-color-lighter);
}

.cat2bug-dynamic-image-block__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.cat2bug-dynamic-image-block__token {
  font-size: 12px;
  word-break: break-all;
  color: var(--el-text-color-secondary);
}
</style>
