<template>
  <div :class="classnames" :style="{ cursor, userSelect }" @mousedown="onMouseDown">
    <slot></slot>
  </div>
</template>

<script src="./multipane.js"></script>

<style lang="scss">
.multipane {
    display: flex;

    &.layout-h {
        flex-direction: column;
    }

    &.layout-v {
        flex-direction: row;
    }
}

.multipane > div {
  position: relative;
  z-index: 1;
}

.multipane-resizer {
  display: block;
  position: relative;
  z-index: 2;
}

.layout-h > .multipane-resizer {
  width: 100%;
  height: 10px;
  margin-top: -10px;
  top: 5px;
  cursor: row-resize;
}

.layout-v > .multipane-resizer {
  width: 10px;
  height: 100%;
  margin-left: -10px;
  left: 5px;
  cursor: col-resize;
}

/*
 * div.multipane-resizer 同时匹配上面的 .multipane > div，后者 z-index:1 特异度更高，会盖掉 .multipane-resizer{z-index:2}，
 * 导致分隔条与左右 pane 同为 1，右侧 el-table 固定列等后绘制则压住拖动条（如新建计划弹窗）。
 */
.multipane.layout-v > div.multipane-resizer,
.multipane.layout-h > div.multipane-resizer {
  z-index: 100;
}
</style>
