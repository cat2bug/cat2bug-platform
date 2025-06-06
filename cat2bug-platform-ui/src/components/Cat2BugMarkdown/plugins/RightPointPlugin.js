export function RightPointPlugin (md,options) {
  function rightRule (state,startLine,endLine) {
    let lineRg = /^(\s*--:\s*)(.*)/;
    let match = state.src.substr(state.pos).match(lineRg);
    if (!match) return false;
    const token_start = state.push('right_point_open', 'div', 1)
    token_start.attrs = [['style', 'display: inline-flex;flex-direction: column;justify-content: center;align-items: end;width:100%;']]

    const token_text = state.push('text', '', 0)
    token_text.content=md.renderInline(match[2]);
    token_start.children = [token_text]
    state.push('right_point_close', 'div', -1)
    state.pos += match[0].length;
    return true;
  };

// 在Markdown处理流程中添加居中规则
  md.inline.ruler.before('text', 'right-point', rightRule);
}
