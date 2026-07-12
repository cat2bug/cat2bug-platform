/**
* v-dialogDrag 弹窗拖拽
* Copyright (c) 2023 cat2bug
*/

export default {
  mounted(el, binding) {
    const value = binding.value
    if (value == false) return
    const dialogHeaderEl = el.querySelector('.el-dialog__header');
    const dragDom = el.querySelector('.el-dialog');
    if (!dialogHeaderEl || !dragDom) return
    dialogHeaderEl.style.cursor = 'move';
    const sty = dragDom.currentStyle || window.getComputedStyle(dragDom, null);
    dragDom.style.position = 'absolute';
    dragDom.style.marginTop = 0;
    let width = dragDom.style.width;
    if (width.includes('%')) {
      width = +document.body.clientWidth * (+width.replace(/\%/g, '') / 100);
    } else {
      width = +width.replace(/\px/g, '');
    }
    dragDom.style.left = `${(document.body.clientWidth - width) / 2}px`;
    dialogHeaderEl.onmousedown = (e) => {
      const disX = e.clientX - dialogHeaderEl.offsetLeft;
      const disY = e.clientY - dialogHeaderEl.offsetTop;

      let styL, styT;

      if (sty.left.includes('%')) {
        styL = +document.body.clientWidth * (+sty.left.replace(/\%/g, '') / 100);
        styT = +document.body.clientHeight * (+sty.top.replace(/\%/g, '') / 100);
      } else {
        styL = +sty.left.replace(/\px/g, '');
        styT = +sty.top.replace(/\px/g, '');
      };

      document.onmousemove = function (e) {
        const l = e.clientX - disX;
        const t = e.clientY - disY;

        let finallyL = l + styL
        let finallyT = t + styT

        dragDom.style.left = `${finallyL}px`;
        dragDom.style.top = `${finallyT}px`;

      };

      document.onmouseup = function () {
        document.onmousemove = null;
        document.onmouseup = null;
      };
    }
  }
};
