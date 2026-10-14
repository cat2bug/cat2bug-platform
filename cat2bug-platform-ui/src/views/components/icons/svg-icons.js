const modules = import.meta.glob('../../../assets/icons/svg/*.svg', { eager: true })

const re = /\/([^/]+)\.svg$/

const svgIcons = Object.keys(modules)
  .map((path) => {
    const match = path.match(re)
    return match ? match[1] : null
  })
  .filter(Boolean)

export default svgIcons
