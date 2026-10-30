import {
  filterAutoLevelCandidates,
  levelDefectPlans,
  pushOverlapsAfterPinned,
  isDefectClosed,
  canAutoLevelEditDefect,
  isProjectPlanAdmin,
  naturalDayWorkCalendar
} from '@/views/system/defect/list/defect-gantt'

/** 旧夹具按自然日断言；工作日语义见 defect-gantt-work-calendar.spec.js */
const cal = naturalDayWorkCalendar()

function defect(partial) {
  return {
    defectId: partial.defectId,
    defectStateName: partial.defectStateName || 'PROCESSING',
    planStartTime: partial.planStartTime,
    planEndTime: partial.planEndTime,
    handleBy: partial.handleBy || [1],
    handleByList: (partial.handleBy || [1]).map((userId) => ({
      userId,
      nickName: String(userId)
    }))
  }
}

describe('defect gantt auto-level', () => {
  it('excludes closed from candidates', () => {
    const open = defect({
      defectId: 1,
      planStartTime: '2026-10-01 00:00:00',
      planEndTime: '2026-10-01 23:59:59',
      handleBy: [9]
    })
    const closed = defect({
      defectId: 2,
      defectStateName: 'CLOSED',
      planStartTime: '2026-10-01 00:00:00',
      planEndTime: '2026-10-02 23:59:59',
      handleBy: [9]
    })
    expect(isDefectClosed(closed)).toBe(true)
    const list = filterAutoLevelCandidates([open, closed], {
      userId: 9,
      isProjectAdmin: true
    })
    expect(list.map((d) => d.defectId)).toEqual([1])
  })

  it('non-admin can only edit own handled', () => {
    const mine = defect({
      defectId: 1,
      planStartTime: '2026-10-01 00:00:00',
      planEndTime: '2026-10-01 23:59:59',
      handleBy: [9]
    })
    const other = defect({
      defectId: 2,
      planStartTime: '2026-10-01 00:00:00',
      planEndTime: '2026-10-01 23:59:59',
      handleBy: [8]
    })
    expect(canAutoLevelEditDefect(mine, { userId: 9, isProjectAdmin: false })).toBe(true)
    expect(canAutoLevelEditDefect(other, { userId: 9, isProjectAdmin: false })).toBe(false)
    expect(
      filterAutoLevelCandidates([mine, other], { userId: 9, isProjectAdmin: false }).map(
        (d) => d.defectId
      )
    ).toEqual([1])
  })

  it('packs overlapping same-handler bars without gap', () => {
    const a = defect({
      defectId: 1,
      planStartTime: '2026-10-01 00:00:00',
      planEndTime: '2026-10-02 23:59:59',
      handleBy: [1]
    })
    const b = defect({
      defectId: 2,
      planStartTime: '2026-10-01 00:00:00',
      planEndTime: '2026-10-01 23:59:59',
      handleBy: [1]
    })
    const { proposals, summary } = levelDefectPlans([a, b], [], {
      movableIds: [1, 2],
      calendar: cal
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
    /* 按原开始日+id：1 先于 2；若同日起 1 在前 */
    const first = p1.planStartTime <= p2.planStartTime ? p1 : p2
    const second = first === p1 ? p2 : p1
    expect(second.planStartTime >= first.planEndTime.slice(0, 10) || second.planStartTime > first.planStartTime).toBe(true)
    /* 第二条：后继开始 = 前置结束次日 */
    const endDay = first.planEndTime.slice(0, 10)
    const end = new Date(endDay + 'T00:00:00')
    end.setDate(end.getDate() + 1)
    const y = end.getFullYear()
    const m = String(end.getMonth() + 1).padStart(2, '0')
    const d = String(end.getDate()).padStart(2, '0')
    expect(second.planStartTime.startsWith(`${y}-${m}-${d}`)).toBe(true)
  })

  it('respects FS order between movable defects', () => {
    const a = defect({
      defectId: 10,
      planStartTime: '2026-10-05 00:00:00',
      planEndTime: '2026-10-06 23:59:59',
      handleBy: [1]
    })
    const b = defect({
      defectId: 11,
      planStartTime: '2026-10-01 00:00:00',
      planEndTime: '2026-10-02 23:59:59',
      handleBy: [1]
    })
    const links = [{ fromDefectId: 10, toDefectId: 11, linkType: 'FS' }]
    const { proposals } = levelDefectPlans([a, b], links, { movableIds: [10, 11], calendar: cal })
    const pa = proposals.get(10) || { planStartTime: a.planStartTime, planEndTime: a.planEndTime }
    const pb = proposals.get(11) || { planStartTime: b.planStartTime, planEndTime: b.planEndTime }
    expect(pb.planStartTime.slice(0, 10) > pa.planEndTime.slice(0, 10)).toBe(true)
  })

  it('immovable predecessor constrains successor; air overlap allowed', () => {
    const locked = defect({
      defectId: 1,
      planStartTime: '2026-10-01 00:00:00',
      planEndTime: '2026-10-03 23:59:59',
      handleBy: [2]
    })
    const mine = defect({
      defectId: 2,
      planStartTime: '2026-10-01 00:00:00',
      planEndTime: '2026-10-01 23:59:59',
      handleBy: [1]
    })
    const links = [{ fromDefectId: 1, toDefectId: 2, linkType: 'FS' }]
    const { proposals } = levelDefectPlans([locked, mine], links, { movableIds: [2], calendar: cal })
    expect(proposals.has(1)).toBe(false)
    const p2 = proposals.get(2)
    expect(p2).toBeTruthy()
    expect(p2.planStartTime.startsWith('2026-10-04')).toBe(true)
  })

  it('empty movable yields empty proposals', () => {
    const { proposals, summary } = levelDefectPlans([], [], { movableIds: [] })
    expect(proposals.size).toBe(0)
    expect(summary.changed).toBe(0)
  })

  it('packs by list visual order: upper row earlier than lower', () => {
    const upper = defect({
      defectId: 20,
      planStartTime: '2026-10-10 00:00:00',
      planEndTime: '2026-10-10 23:59:59',
      handleBy: [1]
    })
    const lower = defect({
      defectId: 10,
      planStartTime: '2026-10-01 00:00:00',
      planEndTime: '2026-10-01 23:59:59',
      handleBy: [1]
    })
    const { proposals } = levelDefectPlans([upper, lower], [], {
      movableIds: [20, 10],
      visualOrder: [20, 10],
      calendar: cal
    })
    const pu = proposals.get(20) || {
      planStartTime: upper.planStartTime,
      planEndTime: upper.planEndTime
    }
    const pl = proposals.get(10) || {
      planStartTime: lower.planStartTime,
      planEndTime: lower.planEndTime
    }
    expect(pu.planStartTime <= pl.planStartTime).toBe(true)
    expect(pl.planStartTime.startsWith('2026-10-02') || pl.planStartTime > pu.planEndTime).toBe(
      true
    )
  })

  it('detects project plan admin from createBy or roleKey', () => {
    expect(isProjectPlanAdmin({ createBy: 5, members: [] }, 5)).toBe(true)
    expect(
      isProjectPlanAdmin(
        {
          createBy: 1,
          members: [{ userId: 9, roles: [{ roleKey: 'project.admin' }] }]
        },
        9
      )
    ).toBe(true)
    expect(
      isProjectPlanAdmin(
        {
          createBy: 1,
          members: [{ userId: 9, roles: [{ roleKey: 'project.develop' }] }]
        },
        9
      )
    ).toBe(false)
  })

  it('pushOverlapsAfterPinned moves overlapping peer after pin', () => {
    const peer = defect({
      defectId: 2,
      planStartTime: '2026-10-01 00:00:00',
      planEndTime: '2026-10-01 23:59:59',
      handleBy: [1]
    })
    const map = pushOverlapsAfterPinned({
      pinnedId: 1,
      pinnedRange: {
        planStartTime: '2026-10-01 00:00:00',
        planEndTime: '2026-10-02 23:59:59'
      },
      peers: [peer],
      visualOrder: [1, 2]
    , calendar: cal
    })
    expect(map.has(2)).toBe(true)
    expect(map.get(2).planStartTime.startsWith('2026-10-03')).toBe(true)
    expect(map.get(2).planEndTime.startsWith('2026-10-03')).toBe(true)
  })

  it('pushOverlapsAfterPinned leaves non-overlapping peer alone', () => {
    const peer = defect({
      defectId: 2,
      planStartTime: '2026-10-10 00:00:00',
      planEndTime: '2026-10-10 23:59:59',
      handleBy: [1]
    })
    const map = pushOverlapsAfterPinned({
      pinnedId: 1,
      pinnedRange: {
        planStartTime: '2026-10-01 00:00:00',
        planEndTime: '2026-10-02 23:59:59'
      },
      peers: [peer]
    , calendar: cal
    })
    expect(map.size).toBe(0)
  })

  it('pushOverlapsAfterPinned chains multiple overlapping peers by visual order', () => {
    const a = defect({
      defectId: 2,
      planStartTime: '2026-10-01 00:00:00',
      planEndTime: '2026-10-01 23:59:59',
      handleBy: [1]
    })
    const b = defect({
      defectId: 3,
      planStartTime: '2026-10-01 00:00:00',
      planEndTime: '2026-10-02 23:59:59',
      handleBy: [1]
    })
    const map = pushOverlapsAfterPinned({
      pinnedId: 1,
      pinnedRange: {
        planStartTime: '2026-10-01 00:00:00',
        planEndTime: '2026-10-01 23:59:59'
      },
      peers: [a, b],
      visualOrder: [1, 2, 3]
    , calendar: cal
    })
    expect(map.get(2).planStartTime.startsWith('2026-10-02')).toBe(true)
    expect(map.get(3).planStartTime.startsWith('2026-10-03')).toBe(true)
  })
})
