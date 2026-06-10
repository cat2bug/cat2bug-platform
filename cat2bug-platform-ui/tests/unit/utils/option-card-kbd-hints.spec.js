import {
  hrefToOptionActionRouteKey,
  resolveOptionActionKey,
  PROJECT_OPTION_CARD_CATALOG
} from '@/utils/option-card-kbd-hints'
import { PROJECT_OPTION_ACTION_DEFAULTS } from '@/utils/option-card-kbd-catalog'

describe('option-card-kbd-hints', () => {
  it('hrefToOptionActionRouteKey normalizes project routes', () => {
    expect(hrefToOptionActionRouteKey('#/project/project-base-info')).toBe('route-project-base-info')
    expect(hrefToOptionActionRouteKey('/project/push')).toBe('route-push')
    expect(hrefToOptionActionRouteKey('#/team/team-member')).toBe('route-team-member')
  })

  it('resolveOptionActionKey falls back to card index', () => {
    const el = { getAttribute: () => null, closest: () => null, textContent: '' }
    expect(resolveOptionActionKey(el, 2, 1)).toBe('card2-action1')
  })

  it('PROJECT_OPTION_ACTION_DEFAULTS lists card button titleKeys', () => {
    const baseInfo = PROJECT_OPTION_ACTION_DEFAULTS.find((d) => d.key === 'route-project-base-info')
    expect(baseInfo).toBeDefined()
    expect(baseInfo.titleKey).toBe('project.base-info')
    expect(baseInfo.defaultLetter).toBe('1')
    expect(PROJECT_OPTION_ACTION_DEFAULTS.length).toBe(PROJECT_OPTION_CARD_CATALOG.length + 4)
    expect(PROJECT_OPTION_ACTION_DEFAULTS.find((d) => d.key === 'changeIcon').titleKey)
      .toBe('keyboard.act.project-change-icon')
  })
})
