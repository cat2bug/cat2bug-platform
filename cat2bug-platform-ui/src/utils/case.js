import i18n from "@/utils/i18n/i18n";
import cache from '../plugins/cache'

import {exp} from "qrcode/lib/core/galois-field";

const DEFECT_TEMP_TAB_KEY = "defect-temp-tab-key";

/** 获取测试用例等级名 */
export function getLevelName (level){
  if(!level) {
    return null;
  }
  return `${parseInt(level)-1}P`;
}
/** 获取测试用例等级说明 */
export function getLevelExplain (level){
  if(!level) {
    return null;
  }
  return i18n.t(`case.level${parseInt(level)}-explain`);
}
