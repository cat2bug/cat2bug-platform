/** 统计模块弹框：序列化编辑状态，用于 Esc/关闭前判断是否有未保存修改 */

function normalizeRemindTimerRows(rows) {
  return (rows || []).map((row) => {
    const { _timePicker, ...rest } = row
    const schedule = rest.schedule ? { ...rest.schedule } : {}
    if (_timePicker) {
      schedule.time = _timePicker
    }
    return {
      id: rest.id,
      label: rest.label || '',
      enabled: rest.enabled !== false,
      sound: rest.sound || 'bell-soft',
      firedOnce: !!rest.firedOnce,
      schedule
    }
  })
}

export function serializeRemindTimerCloseState(editTimers) {
  return JSON.stringify(normalizeRemindTimerRows(editTimers))
}
