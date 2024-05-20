import i18n from "@/utils/i18n/i18n";

export function TablePlugin (md,options) {
  const opt = options || {}
  const values = opt.value || []
  const defaultTitles = opt.defaultTitles || []
  function table(state,silent) {
    if (!state.src || state.src.charCodeAt(state.pos) !== 0x24 /* $ */ || !opt.value) {
      return false;
    }
    let rg = /\s*\$table\{((\w|\.)*)(\[(((\w|\.)+(:(\w|[\u3040-\uD7AF]|\.)+)*){1}(,(\w|\.)+(:(\w|[\u3040-\uD7AF]|\.)+)*)*)\])?\}/;
    let match = state.src.match(rg);
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

      let headerMap = new Map();
      if(match.length > 4 && match[4]) {
        let titles = match[4].split(',');
        titles.forEach(t=>{
          const item =  t.split(':');
          const key = item[0];
          let val = item.length>1?item[1]:item[0];
          if(i18n.te(val)){
            val = i18n.t(val);
          }
          headerMap.set(key,val);
        })
      } else if(defaultTitles.length>0) {
        defaultTitles.forEach(t=>{
          let val = i18n.te(t)?i18n.t(t):t;
          headerMap.set(t,val);
        })
      } else if(values.length>0) {
        Object.keys(values[0]).forEach(k => {
          let val = i18n.te(k)?i18n.t(k):k;
          headerMap.set(k,val);
        });
      }

      if(headerMap.size>0) {
        for(const [k,v] of headerMap) {
          state.push('th_open', 'th', 1)
          const token_il = state.push('text', '', 0)
          token_il.content = v
          token_il.children = []
          state.push('th_close', 'th', -1)
        }
      }
      state.push('tr_close', 'tr', -1)
      state.push('thead_close', 'thead', -1)

      // body
      const token_body = state.push('tbody_open', 'tbody', 1)
      token_body.map = [startLien, startLien + 1]
      if(values.length>0) {
        values.forEach(row => {
          const token_btro = state.push('tr_open', 'tr', 1)
          token_btro.map = [startLien, startLien + 1]
          for(const [k,v] of headerMap) {
            state.push('td_open', 'td', 1)
            const token_til = state.push('text', '', 0)
            token_til.content = row[k] ? row[k] + '' : '';
            token_til.children = []
            state.push('td_close', 'td', -1)
          }
          state.push('tr_close', 'tr', -1);
        })
      }

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
