/** Map Element UI size values to Element Plus control sizes. */
export function mapElementPlusSize(size) {
  const normalized = String(size || '').toLowerCase()
  if (normalized === 'mini') return 'small'
  if (normalized === 'medium') return 'default'
  if (normalized === 'small' || normalized === 'large' || normalized === 'default') return normalized
  return 'default'
}
