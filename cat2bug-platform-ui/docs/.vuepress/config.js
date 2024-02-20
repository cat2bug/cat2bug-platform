module.exports = {
  dest:'./assets/doc',
  locales: {
    '/': {
      lang: '简体中文', // 将会被设置为 <html> 的 lang 属性
      title: 'Cat2Bug-Platform文档',
      description: '',
    },
    '/lang/us/': {
      lang: 'English',
      title: 'Cat2Bug-Platform Document',
      description: '',
    }
  },
  themeConfig: {
    logo: 'logo.png',
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Guide', link: '/guide/' },
      { text: 'External', link: 'https://google.com' },
    ],
    sidebarDepth: 3,
    sidebar: [
      '/',
      '/system/',
      '/api/',
    ]
  },
  markdown: {
    lineNumbers: true,
    anchor: {
      permalink: true, permalinkBefore: true, permalinkSymbol: '#'
    }
  }
}
