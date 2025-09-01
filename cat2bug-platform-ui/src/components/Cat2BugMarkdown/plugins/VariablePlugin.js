export function VariablePlugin (md,options) {
  const opt = options || {}

  function variable (state, silent) {
    let match;
    if (state.src.charCodeAt(state.pos) === 0x24 /* $ */) {
      let rg = /^\$(t|text){0,1}\{([0-9a-zA-z\.]*?)\}/;
      match = state.src.substr(state.pos).match(rg);
      if (!match) return false;
      let result = match[2];
      if(!result || result.indexOf(opt.name)!=0) return false;

      if (!silent) {
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

        let vToken = state.push('text', opt.name, 0);
        vToken.content = content;
        vToken.children = []
      }
      state.pos += match[0].length;
      return true;
    }
    return false;
  }

  md.inline.ruler.before('text', 'variable', variable);
}
