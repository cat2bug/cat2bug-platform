import {listDefect} from "@/api/system/defect"

export async function api(apiName, query) {
  switch (apiName) {
    case 'api.defect.list':
      const res = await listDefect(query);
      return res.rows;
  }
}
