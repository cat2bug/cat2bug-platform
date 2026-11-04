import i18n from "@/utils/i18n/i18n";
import cache from '../plugins/cache'

export {
  DEFECT_DEFAULT_TYPE,
  DEFECT_DEFAULT_LEVEL,
  normalizeDefectTypeAndLevel,
} from './defect-defaults'

const DEFECT_TEMP_TAB_KEY = "defect-temp-tab-key";

/** 计算缺陷存活时间（优先 API 派生 handleStartTime/handlePassTime；起点缺则 createTime） */
export function lifeTime (defect){
  if (!defect) {
    return '0' + i18n.t('minute');
  }
  const startSource = defect.handleStartTime || defect.createTime;
  const startTime = startSource ? new Date(startSource) : null;
  const passTime = defect.handlePassTime
    ? new Date(defect.handlePassTime)
    : new Date();
  if (!startTime || Number.isNaN(startTime.getTime())) {
    return '0' + i18n.t('minute');
  }
  let lifeMs = passTime.getTime() - startTime.getTime();
  if (lifeMs < 0) {
    lifeMs = 0;
  }
  const day = Math.floor(lifeMs / (1000 * 60 * 60 * 24));
  lifeMs = lifeMs % (1000 * 60 * 60 * 24);
  const hour = Math.floor(lifeMs / (1000 * 60 * 60));
  lifeMs = lifeMs % (1000 * 60 * 60);
  const minute = Math.floor(lifeMs / (1000 * 60));
  let ret = [];
  if (day) ret.push(day + i18n.t('day'));
  if (hour) ret.push(hour + i18n.t('hour'));
  ret.push(minute + i18n.t('minute'));
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
