import {api} from "./ApiUtil";

export function VariablePlugin (md,options) {
  const opt = options || {}

  /**
   * 处理变量数据
   * @param state     状态
   * @param startLine 起始行
   * @param endLine   结束行
   * @returns {boolean} 是否成功
   */
  function variable (state,startLine,endLine) {
    let ch, token,
      pos = state.bMarks[startLine] + state.tShift[startLine],
      max = state.eMarks[startLine];
    ch  = state.src.charCodeAt(pos);

    if (ch !== 0x24/*$*/ || pos >= max) { return false; }
    let text = state.src.substring(pos, max);
    let rg = /^\$(v|variable){0,1}\{(.*)\}/;
    let match = text.match(rg);
    console.log(text,opt.name,match,match.length)
    if (match && match.length) {
      let result = match[2];
      if(!result || result.indexOf(opt.name)!=0) return false;

      let content = opt.value;
      if(result.length>opt.name.length) {
        result = result.substring(opt.name.length+1);
        const fields = result.split('.');
        for(let i in fields) {
          if(!fields[i])
            return false;
          content = content[fields[i]];
          if(!content) return false;
        }
      }
      console.log(result)
      token = state.push('inline', '', 0);
      token.content = JSON.stringify(content);
      token.map = [ startLine, state.line ];
      token.children = [];

      state.line = startLine + 1;
      return true;
    }
  }

  md.block.ruler.before('paragraph', 'cat2bug_variable_plugin', variable);
}
