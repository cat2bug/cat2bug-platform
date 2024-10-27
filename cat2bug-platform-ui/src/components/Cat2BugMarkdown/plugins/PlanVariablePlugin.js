export function PlanVariablePlugin (md,options) {
  const opt = options || {}

  function variable (state, silent) {
    let match;
    if (state.src.charCodeAt(state.pos) === 0x24 /* $ */) {
      let rg = /\$(t|text)?\{(.*?)(\[\s*\d+\s*]\s*)\}/;
      match = state.src.match(rg);
      if (!match) return false;

      let result = match[2];
      // console.log(match)
      if(!result || result.indexOf(opt.name)!=0) return false;
      let planNumber = parseInt(match[3].replace('[','').replace(']',''));
      if (!silent) {
        let planList = opt.value;
        let content = null;
        if(result.length>opt.name.length) {
          result = result.substring(opt.name.length+1);
          for(let i=0;i<planList.length;i++) {
            const plan = planList[i];
            if(plan.planNumber && plan.planNumber==planNumber) {
              // console.log(plan)
              const fields = result.split('.');
              for (let j in fields) {
                if (!fields[j])
                  return false;
                content = plan[fields[j]] + '';
                if (!content) return false;
              }
            }
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
