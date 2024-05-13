import i18n from "@/utils/i18n/i18n";

export function CaseCardPlugin (md,options) {
  const opt = options || {}
  const values = opt.value || [];
  const TITLE_STYLE=['style','font-weight:500'];
  function table(state,silent) {
    if (!state.src || state.src.charCodeAt(state.pos) !== 0x24 /* $ */ || !opt.value) {
      return false;
    }
    let rg = /.*\$card\{(api\.case\.list)\}.*/;
    let match = state.src.match(rg);
    if (match && match.length && !silent) {
      let result = match[1];
      let startLien = 0;
      if(opt.name!=result || values.length==0) return false;

      values.forEach(c => {
        const token_to = state.push('table_open', 'table', 1)
        token_to.attrs = [['style', 'display: table;']]
        const tableLines = [startLien, 0]
        token_to.map = tableLines

        // body
        const token_body = state.push('tbody_open', 'tbody', 1)
        token_body.map = [startLien, startLien + 1];

        // 第一行
        const token_row1 = state.push('tr_open', 'tr', 1);
        token_row1.map = [startLien, startLien + 1]
        // 测试用例标识
        const token_title_id_td = state.push('td_open', 'td', 1);
        token_title_id_td.attrs=[TITLE_STYLE];
        const token_title_id = state.push('text', '', 0)
        token_title_id.content = i18n.t('case.id');
        token_title_id.children = []
        state.push('td_close', 'td', -1);
        state.push('td_open', 'td', 1)
        const token_value_id = state.push('text', '', 0)
        token_value_id.content = '# '+c.caseNum||'';
        token_value_id.children = []
        state.push('td_close', 'td', -1);
        // 测试用例名称
        const token_title_name_td = state.push('td_open', 'td', 1);
        token_title_name_td.attrs=[TITLE_STYLE];
        const token_title_name = state.push('text', '', 0)
        token_title_name.content = i18n.t('case.name');
        token_title_name.children = []
        state.push('td_close', 'td', -1);
        state.push('td_open', 'td', 1)
        const token_value_name = state.push('text', '', 0)
        token_value_name.content = c.caseName||'';
        token_value_name.children = []
        state.push('td_close', 'td', -1);
        state.push('tr_close', 'tr', -1);

        // 第二行
        const token_row2 = state.push('tr_open', 'tr', 1);
        token_row2.map = [startLien, startLien + 1]
        // 测试用例名称
        const token_module_td = state.push('td_open', 'td', 1);
        token_module_td.attrs=[TITLE_STYLE];
        const token_title_module = state.push('text', '', 0)
        token_title_module.content = i18n.t('case.module');
        token_title_module.children = [];
        state.push('td_close', 'td', -1);
        const token_value_module_td = state.push('td_open', 'td', 1);
        token_value_module_td.attrs=[['colspan',3]];
        const token_value_module = state.push('text', '', 0)
        token_value_module.content = c.moduleName||'';
        token_value_module.children = [];
        state.push('td_close', 'td', -1);
        state.push('tr_close', 'tr', -1);

        // 第三行
        const token_row3 = state.push('tr_open', 'tr', 1);
        token_row3.map = [startLien, startLien + 1]
        // 预期效果
        const token_title_expect_td = state.push('td_open', 'td', 1);
        token_title_expect_td.attrs=[TITLE_STYLE];
        const token_title_expect = state.push('text', '', 0)
        token_title_expect.content = i18n.t('case.expect');
        token_title_expect.children = [];
        state.push('td_close', 'td', -1);
        const token_value_expect_td = state.push('td_open', 'td', 1);
        token_value_expect_td.attrs=[['colspan',3]];
        const token_value_expect = state.push('text', '', 0)
        token_value_expect.content = c.caseExpect||'';
        token_value_expect.children = [];
        state.push('td_close', 'td', -1);
        state.push('tr_close', 'tr', -1);

        // 第四行
        const token_row4 = state.push('tr_open', 'tr', 1);
        token_row4.map = [startLien, startLien + 1]
        // 前置条件
        const token_title_precondition_td = state.push('td_open', 'td', 1);
        token_title_precondition_td.attrs=[TITLE_STYLE];
        const token_title_precondition = state.push('text', '', 0);
        token_title_precondition.content = i18n.t('case.precondition');
        token_title_precondition.children = [];
        state.push('td_close', 'td', -1);
        const token_value_precondition_td = state.push('td_open', 'td', 1);
        token_value_precondition_td.attrs=[['colspan',3]];
        const token_value_precondition = state.push('text', '', 0)
        token_value_precondition.content = c.casePreconditions||'';
        token_value_precondition.children = [];
        state.push('td_close', 'td', -1);
        state.push('tr_close', 'tr', -1);

        // 如果有测试步骤，生成测试步骤表格
        if(c.caseStep && c.caseStep.length>0) {
          // 第五行
          const token_row5 = state.push('tr_open', 'tr', 1);
          token_row5.map = [startLien, startLien + 1]
          // 测试步骤标题
          const token_value_step_title_td = state.push('td_open', 'td', 1);
          token_value_step_title_td.attrs = [['colspan', 4], ['style', 'text-align: center;vertical-align: middle;font-weight:500;border-bottom:none;']];
          const token_value_step_title = state.push('text', '', 0)
          token_value_step_title.content = i18n.t('case.step');
          token_value_step_title.children = [];
          state.push('td_close', 'td', -1);
          state.push('tr_close', 'tr', -1);

          // 第六行
          const token_row6 = state.push('tr_open', 'tr', 1);
          token_row6.attrs = [['style','border-top:none;']]
          token_row6.map = [startLien, startLien + 1]
          // 测试步骤
          const token_value_step_value_td = state.push('td_open', 'td', 1);
          token_value_step_value_td.attrs = [['colspan', 4],['style','padding:0px;border:none;']];

          const token_step_to = state.push('table_open', 'table', 1)
          const stepTableLines = [startLien, startLien+1]
          token_step_to.attrs=[['style','width:100%;margin: 0px;display: table;']]
          token_step_to.map = stepTableLines

          // 步骤头部
          const token_step_header = state.push('thead_open', 'thead', 1)
          token_step_header.map = [startLien, startLien + 1]
          const token_step_header_tr = state.push('tr_open', 'tr', 1)
          token_step_header_tr.map = [startLien, startLien + 1]

          state.push('th_open', 'th', 1)
          const token_header_title1 = state.push('text', '', 0)
          token_header_title1.content = i18n.t('case.number')
          token_header_title1.children = []
          state.push('th_close', 'th', -1)

          state.push('th_open', 'th', 1)
          const token_header_title2 = state.push('text', '', 0)
          token_header_title2.content = i18n.t('case.precondition')
          token_header_title2.children = []
          state.push('th_close', 'th', -1)

          state.push('th_open', 'th', 1)
          const token_header_title3 = state.push('text', '', 0)
          token_header_title3.content = i18n.t('case.step-describe')
          token_header_title3.children = []
          state.push('th_close', 'th', -1)

          state.push('tr_close', 'tr', -1)
          state.push('thead_close', 'thead', -1)

          const token_step_body = state.push('tbody_open', 'tbody', 1)
          token_step_body.map = [startLien, startLien + 1];
          c.caseStep.forEach((s,index) => {
            // 第一行
            const token_step_row = state.push('tr_open', 'tr', 1);
            token_step_row.map = [startLien, startLien + 1];

            // 序列号
            const token_step_number_td = state.push('td_open', 'td', 1);
            token_step_number_td.attrs = [['style', 'text-align: center;vertical-align: middle;']];
            const token_step_number = state.push('text', '', 0)
            token_step_number.content = index+1;
            token_step_number.children = []
            state.push('td_close', 'td', -1);

            // 步骤的前置条件
            state.push('td_open', 'td', 1);
            const token_step_precondition = state.push('text', '', 0)
            token_step_precondition.content = s.stepExpect||'';
            token_step_precondition.children = []
            state.push('td_close', 'td', -1);

            // 步骤的前置条件
            state.push('td_open', 'td', 1);
            const token_step_desc = state.push('text', '', 0)
            token_step_desc.content = s.stepDescribe||'';
            token_step_desc.children = []
            state.push('td_close', 'td', -1);

            state.push('tr_close', 'tr', -1)
          });
          state.push('tbody_close', 'tbody', -1)
          state.push('table_close', 'table', -1)

          state.push('td_close', 'td', -1);
          state.push('tr_close', 'tr', -1);
          state.push('table_br', 'br', 0)
        }
        state.push('tbody_close', 'tbody', -1)
        state.push('table_close', 'table', -1)

      });
      state.pos += match[0].length;
      return true;
    }
    return false;
  }
  // md.block.ruler.before('paragraph', 'cat2bug_table_plugin'+opt.name, table);
  md.inline.ruler.before('text', 'cat2bug_case_card_plugin', table);
}
