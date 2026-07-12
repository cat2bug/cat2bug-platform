const MENU_ICON_OVERRIDES = {
  'team-options': 'team-option',
  'team-option': 'team-option'
}

export function applyMenuIconOverrides(routes) {
  if (!Array.isArray(routes)) {
    return
  }
  routes.forEach(route => {
    const override = MENU_ICON_OVERRIDES[route.path]
    if (override && route.meta) {
      route.meta.icon = override
    }
    if (route.children?.length) {
      applyMenuIconOverrides(route.children)
    }
  })
}
