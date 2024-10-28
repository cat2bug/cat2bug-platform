export function CardPlugin (md,options) {
  const opt = options || {}
  const values = opt.value || {}
  function table(state,silent) {
    if (!state.src || state.src.charCodeAt(state.pos) !== 0x24 /* $ */ || !opt.value) {
      return false;
    }
    let rg = /^\$card\{(.*?)\}/;
    let match = state.src.substr(state.pos).match(rg);
    if (match && match.length && !silent) {
      let result = match[1];
      let startLien = 0;
      if(opt.name!=result) return false;
      const token_to = state.push('table_open', 'table', 1)
      const tableLines = [startLien, 0]
      token_to.map = tableLines

      // header
      const token_tho = state.push('thead_open', 'thead', 1)
      token_tho.map = [startLien, startLien + 1]

      const token_htro = state.push('tr_open', 'tr', 1)
      token_htro.map = [startLien, startLien + 1]

      Object.keys(values[0]).forEach(k => {
        state.push('th_open', 'th', 1)
        const token_il = state.push('text', '', 0)
        token_il.content = k
        token_il.children = []
        state.push('th_close', 'th', -1)
      });
      state.push('tr_close', 'tr', -1)
      state.push('thead_close', 'thead', -1)

      // body
      const token_body = state.push('tbody_open', 'tbody', 1)
      token_body.map = [startLien, startLien + 1]

      values.forEach(o=>{
        const token_btro = state.push('tr_open', 'tr', 1)
        token_btro.map = [startLien, startLien + 1]
        Object.keys(o).forEach(k=>{
          state.push('td_open', 'td', 1)
          const token_til = state.push('text', '', 0)
          token_til.content = o[k]?o[k]+'':'';
          token_til.children = []
          state.push('td_close', 'td', -1)
        })
        state.push('tr_close', 'tr', -1);
      })

      state.push('tbody_close', 'tbody', -1)
      state.push('table_close', 'table', -1)
      state.pos += match[0].length;
      return true;
    }
    return false;
  }
  // md.block.ruler.before('paragraph', 'cat2bug_table_plugin'+opt.name, table);
  md.inline.ruler.before('text', 'cat2bug_table_plugin', table);
}
