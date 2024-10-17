import i18n from "@/utils/i18n/i18n";
import cache from '../plugins/cache'

import {exp} from "qrcode/lib/core/galois-field";

const DEFECT_TEMP_TAB_KEY = "defect-temp-tab-key";

export function lifeTime (defect){
  let passTime = defect.handlePassTime ? new Date(defect.handlePassTime): new Date();
  let startTime = defect.handleStartTime ? new Date(defect.handleStartTime): new Date();
  let lifeTime = passTime.getTime() - startTime.getTime();
  const day = Math.floor(lifeTime/(1000 * 60 * 60 * 24));
  lifeTime = lifeTime%(1000 * 60 * 60 * 24);
  const hour = Math.floor(lifeTime/(1000 * 60 * 60));
  lifeTime = lifeTime%(1000 * 60 * 60);
  const minute = Math.floor(lifeTime/(1000 * 60));
  let ret = [];
  if(day) ret.push(day+i18n.t('day'));
  if(hour) ret.push(hour+i18n.t('hour'));
  ret.push(minute+i18n.t('minute'));
  return ret.join(' ');
}
/** 设置缺陷临时Tab标签数据 */
export function setDefectTempTab(data) {
  cache.local.setJSON(DEFECT_TEMP_TAB_KEY, data);
}
/** 获取缺陷临时Tab标签数据 */
export function getDefectTempTab() {
  return cache.local.getJSON(DEFECT_TEMP_TAB_KEY);
}
/** 移除缺陷临时Tab标签数据 */
export function removeDefectTempTab() {
  return cache.local.remove(DEFECT_TEMP_TAB_KEY);
}
