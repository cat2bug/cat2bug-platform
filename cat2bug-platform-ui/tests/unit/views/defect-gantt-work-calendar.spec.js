import {
  isWorkday,
  nextWorkday,
  addWorkdays,
  countWorkdays,
  planDurationDays,
  endFromDurationDays,
  fsEarliestStartAfterEnd,
  levelDefectPlans,
  pushOverlapsAfterPinned,
  defaultPlanRangeFromDropDay,
  buildAxisUnits,
  barGeometry,
  barRestOverlays,
  GANTT_SCALE,
  GANTT_COL_WIDTH
} from '@/views/system/defect/list/defect-gantt'

const cal = {
  workWeek: [1, 2, 3, 4, 5],
  exceptions: {
    '2026-10-01': 'holiday',
    '2026-10-02': 'holiday',
    '2026-09-27': 'workday'
  }
}

describe('defect gantt work calendar', () => {
  it('isWorkday respects week, holiday and makeup', () => {
    expect(isWorkday(new Date(2026, 8, 27), cal)).toBe(true) // Sun makeup
    expect(isWorkday(new Date(2026, 8, 28), cal)).toBe(true) // Mon
    expect(isWorkday(new Date(2026, 9, 1), cal)).toBe(false) // holiday
    expect(isWorkday(new Date(2026, 9, 3), cal)).toBe(false) // Sat
  })

  it('addWorkdays and countWorkdays skip holidays', () => {
    // Thu Oct 1 holiday, Fri Oct 2 holiday → from Sep 30 (Wed) + 2 workdays = Oct 6 (Mon)?
    // Sep 30 Wed workday = day1, next workdays: Oct 3 Sat no, Oct 4 Sun no, Oct 5 Mon = day2
    const start = new Date(2026, 8, 30)
    expect(addWorkdays(start, 2, cal).getDate()).toBe(5)
    expect(countWorkdays(new Date(2026, 8, 30), new Date(2026, 9, 5), cal)).toBe(2)
  })

  it('fsEarliestStartAfterEnd skips weekend/holiday', () => {
    // end Fri Oct 2 holiday → next calendar day Sat → next workday Mon Oct 5
    const d = fsEarliestStartAfterEnd(new Date(2026, 9, 2), cal)
    expect(d.getFullYear()).toBe(2026)
    expect(d.getMonth()).toBe(9)
    expect(d.getDate()).toBe(5)
  })

  it('default drop aligns off holiday', () => {
    const range = defaultPlanRangeFromDropDay(new Date(2026, 9, 1), cal)
    expect(range.planStartTime.startsWith('2026-10-05')).toBe(true)
  })

  it('planDurationDays counts workdays', () => {
    const days = planDurationDays(
      {
        planStartTime: '2026-09-30 00:00:00',
        planEndTime: '2026-10-05 23:59:59'
      },
      cal
    )
    expect(days).toBe(2)
  })

  it('levelDefectPlans packs without parking on weekend', () => {
    const a = {
      defectId: 1,
      defectStateName: 'PROCESSING',
      planStartTime: '2026-10-05 00:00:00',
      planEndTime: '2026-10-05 23:59:59',
      handleBy: [1],
      handleByList: [{ userId: 1, nickName: 'a' }]
    }
    const b = {
      defectId: 2,
      defectStateName: 'PROCESSING',
      planStartTime: '2026-10-05 00:00:00',
      planEndTime: '2026-10-05 23:59:59',
      handleBy: [1],
      handleByList: [{ userId: 1, nickName: 'a' }]
    }
    const { proposals, summary } = levelDefectPlans([a, b], [], {
      movableIds: [1, 2],
      visualOrder: [1, 2],
      calendar: { workWeek: [1, 2, 3, 4, 5], exceptions: {} }
    })
    expect(summary.changed).toBeGreaterThan(0)
    const p1 = proposals.get(1) || {
      planStartTime: a.planStartTime,
      planEndTime: a.planEndTime
    }
    const p2 = proposals.get(2) || {
      planStartTime: b.planStartTime,
      planEndTime: b.planEndTime
    }
    const first = p1.planStartTime <= p2.planStartTime ? p1 : p2
    const second = first === p1 ? p2 : p1
    expect(second.planStartTime.startsWith('2026-10-06')).toBe(true)
  })

  it('pushOverlapsAfterPinned lands on next workday', () => {
    const pinned = {
      planStartTime: '2026-10-08 00:00:00',
      planEndTime: '2026-10-08 23:59:59'
    }
    const peer = {
      defectId: 2,
      defectStateName: 'PROCESSING',
      planStartTime: '2026-10-08 00:00:00',
      planEndTime: '2026-10-08 23:59:59',
      handleBy: [1]
    }
    // Oct 8 Thu; next after pin = Oct 9 Fri
    const map = pushOverlapsAfterPinned({
      pinnedId: 1,
      pinnedRange: pinned,
      peers: [peer],
      calendar: { workWeek: [1, 2, 3, 4, 5], exceptions: {} }
    })
    expect(map.get(2).planStartTime.startsWith('2026-10-09')).toBe(true)
  })

  it('axis marks holiday and makeup', () => {
    const units = buildAxisUnits(
      new Date(2026, 8, 27),
      new Date(2026, 9, 2),
      GANTT_SCALE.DAY,
      cal
    )
    const sep27 = units.find((u) => u.start.getDate() === 27 && u.start.getMonth() === 8)
    const oct1 = units.find((u) => u.start.getDate() === 1 && u.start.getMonth() === 9)
    expect(sep27.makeup).toBe(true)
    expect(sep27.weekend).toBe(false)
    expect(oct1.holiday).toBe(true)
    expect(oct1.weekend).toBe(true)
  })

  it('endFromDurationDays spans holidays', () => {
    const end = endFromDurationDays(new Date(2026, 8, 30), 2, cal)
    expect(end.getDate()).toBe(5)
    expect(end.getMonth()).toBe(9)
  })

  it('barRestOverlays merges weekend span inside a bar', () => {
    // Fri Oct 9 → Mon Oct 12：条跨六日，网纹一段约两列
    const defect = {
      planStartTime: '2026-10-09 00:00:00',
      planEndTime: '2026-10-12 23:59:59'
    }
    const rangeStart = new Date(2026, 9, 9)
    const col = GANTT_COL_WIDTH.day
    const geo = barGeometry(defect, rangeStart, GANTT_SCALE.DAY, col)
    const segs = barRestOverlays(defect, rangeStart, GANTT_SCALE.DAY, col, {
      workWeek: [1, 2, 3, 4, 5],
      exceptions: {}
    })
    expect(geo.width).toBe(4 * col)
    expect(segs).toHaveLength(1)
    expect(segs[0].left).toBeCloseTo(col, 0) // Sat 相对条左 = 1 列
    expect(segs[0].width).toBeCloseTo(2 * col, 0) // Sat+Sun
  })
})
