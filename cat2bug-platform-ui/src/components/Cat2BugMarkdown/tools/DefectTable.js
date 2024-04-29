import {listDefect} from "@/api/system/defect";

export function DefectTable() {
  return {
    icon: 'mk-table',
    name: '缺陷表格',
    content: createDefectTable,
  }
}

async function createDefectTable() {
  let res = await listDefect();
  if(res.rows) {
    let table = '|缺陷编号|缺陷名称|缺陷详情|缺陷截图|\n';
    table += '|-|-|-|-|\n';
    res.rows.forEach(d=>{
      let arr = [d.number,d.defectName,d.defectIcon];
      table += '|' + arr.join('|') + '|\n';
    });
    // |content1|content2|content3|
    // table += JSON.stringify(res.rows)
    return table;
  }
  return ''
}
