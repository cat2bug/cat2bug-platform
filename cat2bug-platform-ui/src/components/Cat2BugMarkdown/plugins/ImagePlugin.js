export function ImagePlugin (md,options) {
  const opt = options || {}
  const values = opt.value || {}
  function image (state, silent) {
    if (state.src.charCodeAt(state.pos) === 0x24 /* $ */) {
      let rg = /^\$(img|image)(\[([\"'a-zA-Z0-9]+:.+)*\])*\{(.*?)\}/;
      let match = state.src.substr(state.pos).match(rg);
      if (!match) return false;
      let result = match[4];
      let attr = match[3]?match[3].split(','):[];
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

        let vToken = state.push('image', 'img', 0);
        let attrs = [['src', content], ['alt', '']]
        attr.forEach(t=>{
          let vs = JSON.parse('{'+t+'}');
          Object.keys(vs).forEach(k=>{
            let key = k.trim();
            attrs.push([key,vs[key].trim()]);
          });
        });
        vToken.attrs = attrs
        vToken.content = '';
        vToken.children = []
      }
      state.pos += match[0].length;
      // state.src=state.src.substr(match[0].length)
      return true;
    }
    return false;
  }

  md.inline.ruler.before('text', 'cat2bug_image_plugin', image);
}
