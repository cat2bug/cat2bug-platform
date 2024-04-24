import {api} from "./ApiUtil";

export function TablePlugin (md,options) {
  const opt = options || {}
  function table(state,startLine,endLine) {
    let ch, level, tmp, token,
      pos = state.bMarks[startLine] + state.tShift[startLine],
      max = state.eMarks[startLine];
    ch = state.src.charCodeAt(pos);

    if (ch !== 0x24/*$*/ || pos >= max || !opt.value) {
      return false;
    }
    let text = state.src.substring(pos, max);
    let rg = /^\$table\{(.*)\}/;
    let match = text.match(rg);
    if (match && match.length) {
      let result = match[1];
      if(opt.name!=result) return false;
      const token_to = state.push('table_open', 'table', 1)
      const tableLines = [startLine, 0]
      token_to.map = tableLines

      // header
      const token_tho = state.push('thead_open', 'thead', 1)
      token_tho.map = [startLine, startLine + 1]

      const token_htro = state.push('tr_open', 'tr', 1)
      token_htro.map = [startLine, startLine + 1]

      Object.keys(opt.value[0]).forEach(k => {
        state.push('th_open', 'th', 1)
        const token_il = state.push('inline', '', 0)
        token_il.content = k
        token_il.children = []
        state.push('th_close', 'th', -1)
      });
      state.push('tr_close', 'tr', -1)
      state.push('thead_close', 'thead', -1)

      // body
      const token_body = state.push('tbody_open', 'tbody', 1)
      token_body.map = [startLine, startLine + 1]

      opt.value.forEach(o=>{
        const token_btro = state.push('tr_open', 'tr', 1)
        token_btro.map = [startLine, startLine + 1]
        Object.keys(o).forEach(k=>{
          state.push('td_open', 'td', 1)
          const token_til = state.push('inline', '', 0)
          token_til.content = o[k]?o[k]+'':'';
          token_til.children = []
          state.push('td_close', 'td', -1)
        })
        state.push('tr_close', 'tr', -1);
      })

      state.push('tbody_close', 'tbody', -1)

      state.line = startLine + 1;
      return true;
    }
  }
  md.block.ruler.before('paragraph', 'cat2bug_table_plugin'+opt.name, table);
}
