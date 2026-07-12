/**
* v-dialogDragWidth 可拖动弹窗宽度（右侧边）
* Copyright (c) 2023 cat2bug
*/

export default {
    mounted(el) {
        const dragDom = el.querySelector('.el-dialog');
        if (!dragDom) return
        const lineEl = document.createElement('div');
        lineEl.style = 'width: 5px; background: inherit; height: 80%; position: absolute; right: 0; top: 0; bottom: 0; margin: auto; z-index: 1; cursor: w-resize;';
        lineEl.addEventListener('mousedown',
            function (e) {
                const disX = e.clientX - el.offsetLeft;
                const curWidth = dragDom.offsetWidth;
                document.onmousemove = function (e) {
                    e.preventDefault();
                    const l = e.clientX - disX;
                    dragDom.style.width = `${curWidth + l}px`;
                };
                document.onmouseup = function () {
                    document.onmousemove = null;
                    document.onmouseup = null;
                };
            }, false);
        dragDom.appendChild(lineEl);
    }
}
