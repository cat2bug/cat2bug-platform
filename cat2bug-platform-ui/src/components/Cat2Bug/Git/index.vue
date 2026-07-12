<template>
  <el-dropdown trigger="click" class="cat2bug-git-dropdown" @command="handleCommand">
    <span class="git-dropdown-trigger">
      <svg-icon icon-class="github" />
    </span>
    <template #dropdown>
      <el-dropdown-menu class="dropdown-menu">
        <el-dropdown-item command="gitee">
          <svg-icon icon-class="gitee" />
          <span>Gitee</span>
        </el-dropdown-item>
        <el-dropdown-item command="github">
          <svg-icon icon-class="github" />
          <span>GitHub</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script>
const SOURCE_URLS = {
  gitee: 'https://gitee.com/cat2bug/cat2bug-platform',
  github: 'https://github.com/cat2bug/cat2bug-platform'
}

export default {
  name: 'Cat2BugGit',
  methods: {
    handleCommand(command) {
      const url = SOURCE_URLS[command]
      if (url) {
        window.open(url)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.cat2bug-git-dropdown {
  height: 100%;
  display: inline-flex;
  align-items: center;
}

.git-dropdown-trigger {
  display: inline-flex;
  align-items: center;
  height: 100%;
  cursor: pointer;

  .svg-icon {
    font-size: 18px;
  }
}

.dropdown-menu {
  :deep(.el-dropdown-menu__item) {
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 8px;

    .svg-icon {
      font-size: 20px;
      flex-shrink: 0;
    }
  }
}
</style>
