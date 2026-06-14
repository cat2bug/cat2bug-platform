'use strict'
const path = require('path')
const https = require('https');
const fs = require('fs');
const express = require('express')
const CopyWebpackPlugin = require('copy-webpack-plugin')

function resolve(dir) {
  return path.join(__dirname, dir)
}

const CompressionPlugin = require('compression-webpack-plugin')

const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin

/** 忽略 dev-server / 代理侧 ECONNRESET，避免 Node 因未处理 socket error 直接退出 */
function ignoreSocketReset(err) {
  if (err && (err.code === 'ECONNRESET' || err.code === 'EPIPE')) {
    return
  }
  console.error(err)
}

function attachDevServerSocketGuard(server) {
  const httpServer = server && (server.listeningApp || server)
  if (!httpServer || typeof httpServer.on !== 'function') {
    return
  }
  httpServer.on('connection', socket => {
    socket.on('error', ignoreSocketReset)
  })
  httpServer.on('clientError', (err, socket) => {
    ignoreSocketReset(err)
    if (socket && !socket.destroyed) {
      socket.destroy()
    }
  })
}

function proxyOnError(err, req, res) {
  ignoreSocketReset(err)
  if (res && !res.headersSent && typeof res.writeHead === 'function') {
    res.writeHead(502, { 'Content-Type': 'text/plain' })
    res.end('Bad gateway')
  }
}

function proxyOnProxyReqWs(_proxyReq, _req, socket) {
  if (socket && typeof socket.on === 'function') {
    socket.on('error', ignoreSocketReset)
  }
}

const name = process.env.VUE_APP_TITLE || '猫陪我改BUG' // 网页标题

const port = process.env.port || process.env.npm_config_port || 2222 // 端口
/** 默认 0.0.0.0：Local 显示 localhost，Network 显示本机局域网 IP；仅本机时可 DEV_HOST=localhost */
const devHost = process.env.DEV_HOST || '0.0.0.0'

/** 系统文档 Markdown 源目录（与 CopyWebpackPlugin 生产拷贝源一致） */
const docsStaticDir = path.resolve(__dirname, '../readme/production')

// vue.config.js 配置说明
//官方vue.config.js 参考文档 https://cli.vuejs.org/zh/config/#css-loaderoptions
// 这里只列一部分，具体配置参考文档
const webpack = require('webpack')
const isDev = process.env.NODE_ENV === 'development'
const isEmbedded = process.env.NODE_ENV === 'embedded'
const isAnalyze = process.env.ANALYZE === 'true'
const devToolsEnabled = process.env.VUE_APP_ENABLE_DEV_TOOLS === 'true'

module.exports = {
  // 开发模式下关闭 thread-loader，避免部分 macOS/Node 环境子进程 ECONNRESET
  parallel: !isDev,
  // 部署生产环境和开发环境下的URL。
  // 默认情况下，Vue CLI 会假设你的应用是被部署在一个域名的根路径上
  // 例如 https://www.ruoyi.vip/。如果应用被部署在一个子路径上，你就需要用这个选项指定这个子路径。例如，如果你的应用被部署在 https://www.ruoyi.vip/admin/，则设置 baseUrl 为 /admin/。
  publicPath: process.env.NODE_ENV === "production" ? "/" : "/",
  // 在npm run build 或 yarn build 时 ，生成文件的目录名称（要和baseUrl的生产环境路径一致）（默认dist）
  outputDir: isAnalyze
    ? 'analyze'
    : (isEmbedded ? '../cat2bug-platform-admin/src/main/resources/static' : 'dist'),
  // 用于放置生成的静态资源 (js、css、img、fonts) 的；（项目打包之后，静态资源会放在这个文件夹下）
  assetsDir: process.env.VUE_APP_STATIC_PATH,
  // 开发构建不走 eslint-loader（macOS/Node16 下子进程易出现 ECONNRESET）；提交前请 npm run lint
  lintOnSave: false,
  transpileDependencies: [/vue-excel-editor/],
  // 如果你不需要生产环境的 source map，可以将其设置为 false 以加速生产环境构建。
  productionSourceMap: false,
  // webpack-dev-server 相关配置
  devServer: {
    host: devHost,
    port: port,
    open: true,
    /**
     * 局域网 IP 访问且热更新异常时，可设 CAT2BUG_DEV_HMR=1 启用 ws 传输。
     */
    ...(process.env.CAT2BUG_DEV_HMR === '1' ? {
      transportMode: {
        client: 'ws',
        server: 'ws',
      },
    } : {}),
    // 开发环境提供 /docs/**，避免 history 回退到 index.html 导致系统文档页白屏
    before(app, server) {
      attachDevServerSocketGuard(server)
      app.use('/docs', express.static(docsStaticDir))
    },
    // https: {
    //   cert: fs.readFileSync(path.join(__dirname, 'ssl/default.crt')),
    //   key: fs.readFileSync(path.join(__dirname, 'ssl/default.key'))
    // },
    proxy: {
      // detail: https://cli.vuejs.org/config/#devserver-proxy
      [process.env.VUE_APP_BASE_API]: {
        target: `http://localhost:2020`,
        changeOrigin: true,
        onError: proxyOnError,
        onProxyReqWs: proxyOnProxyReqWs,
        pathRewrite: {
          ['^' + process.env.VUE_APP_BASE_API]: ''
        }
      },
      [process.env.VUE_APP_BASE_WEBSOCKET]: {
        target: `ws://localhost:2020`,
        changeOrigin: true,
        ws: true,
        onError: proxyOnError,
        onProxyReqWs: proxyOnProxyReqWs,
        pathRewrite: {
          ['^' + process.env.VUE_APP_BASE_WEBSOCKET]: ''
        }
      },
      [process.env.VUE_APP_FILE_VIEW]: {
        target: `ws://localhost:8012`,
        changeOrigin: true,
        ws: true,
        onError: proxyOnError,
        onProxyReqWs: proxyOnProxyReqWs,
        pathRewrite: {
          ['^' + process.env.VUE_APP_FILE_VIEW]: ''
        }
      }
    },
    disableHostCheck: true
  },
  css: {
    loaderOptions: {
      sass: {
        sassOptions: { outputStyle: "expanded" }
      }
    }
  },
  configureWebpack: {
    name: name,
    // 开发默认 eval-cheap-module-source-map 会把 xlsx（~2MB）包成单行 eval()，Safari 解析报 Unexpected EOF
    ...(isDev ? { devtool: 'cheap-module-source-map' } : {}),
    resolve: {
      alias: {
        '@': resolve('src'),
        // vue-multipane@0.9.5: mousedown uses target.className.match — breaks on SVG (SVGAnimatedString). Patched vendor build.
        'vue-multipane': resolve('src/vendor/vue-multipane/index.js'),
        // 与自定义表头滚动条厚度对齐 gutterWidth，避免固定列压住滚动条（不改 Element 默认 fixed 布局）
        'element-ui/src/utils/scrollbar-width': resolve('src/utils/element-scrollbar-width.js')
      }
    },
    plugins: [
      ...(isDev ? [] : [
        // http://doc.ruoyi.vip/ruoyi-vue/other/faq.html#使用gzip解压缩静态文件
        new CompressionPlugin({
          cache: false,                   // 不启用文件缓存
          test: /\.(js|css|html)?$/i,     // 压缩文件格式
          filename: '[path].gz[query]',   // 压缩后的文件名
          algorithm: 'gzip',              // 使用gzip压缩
          minRatio: 1                   // 压缩率小于1才会压缩
        }),
        ...(isAnalyze ? [
          new BundleAnalyzerPlugin({
            analyzerMode: 'static',
            reportFilename: 'report.html',
            defaultSizes: 'gzip',
            generateStatsFile: true,
            openAnalyzer: false,
            statsFilename: 'stats.json',
            statsOptions: { source: false }
          })
        ] : []),
        ...(!devToolsEnabled ? [
          new webpack.IgnorePlugin({
            resourceRegExp: /[\\/]views[\\/]tool[\\/]/
          })
        ] : []),
        new CopyWebpackPlugin([
          {
            from: docsStaticDir,
            to: path.resolve(__dirname, isEmbedded ? '../cat2bug-platform-admin/src/main/resources/static/docs' : 'dist/docs'),
            toType: 'dir'
          }
        ])
      ])
    ],
    module: {
      rules: [
        {
          test: /\.mjs$/,
          include: /node_modules/,
          type: "javascript/auto"
        },
      ]
    }
  },
  chainWebpack(config) {
    if (isDev) {
      config.plugins.delete('eslint')
      // 开发模式禁用 cache-loader，避免 macOS/Node 下子进程 IPC 触发 ECONNRESET
      const stripCacheLoader = rule => {
        if (rule.uses.has('cache-loader')) {
          rule.uses.delete('cache-loader')
        }
      }
      ;['vue', 'js', 'ts', 'tsx'].forEach(name => {
        if (config.module.rules.has(name)) {
          stripCacheLoader(config.module.rule(name))
        }
      })
      ;['css', 'postcss', 'scss', 'sass', 'less', 'stylus'].forEach(name => {
        if (config.module.rules.has(name)) {
          config.module.rule(name).oneOfs.store.forEach(oneOf => stripCacheLoader(oneOf))
        }
      })
    }
    config.plugins.delete('preload') // TODO: need test
    config.plugins.delete('prefetch') // TODO: need test
    // set svg-sprite-loader
    config.module
      .rule('svg')
      .exclude.add(resolve('src/assets/icons'))
      .end()
    config.module
      .rule('icons')
      .test(/\.svg$/)
      .include.add(resolve('src/assets/icons'))
      .end()
      .use('svg-sprite-loader')
      .loader('svg-sprite-loader')
      .options({
        symbolId: 'icon-[name]'
      })
      .end()

    config.when(process.env.NODE_ENV !== 'development', config => {
          config
            .plugin('ScriptExtHtmlWebpackPlugin')
            .after('html')
            .use('script-ext-html-webpack-plugin', [{
            // `runtime` must same as runtimeChunk name. default is `runtime`
              inline: /runtime\..*\.js$/
            }])
            .end()

          config.optimization.splitChunks({
            chunks: 'all',
            cacheGroups: {
              libs: {
                name: 'chunk-libs',
                test: /[\\/]node_modules[\\/]/,
                priority: 10,
                chunks: 'initial' // only package third parties that are initially dependent
              },
              elementUI: {
                name: 'chunk-elementUI', // split elementUI into a single package
                test: /[\\/]node_modules[\\/]_?element-ui(.*)/, // in order to adapt to cnpm
                priority: 20 // the weight needs to be larger than libs and app or it will be packaged into libs or app
              },
              commons: {
                name: 'chunk-commons',
                test: resolve('src/components'), // can customize your rules
                minChunks: 3, //  minimum common number
                priority: 5,
                reuseExistingChunk: true
              }
            }
          })

          config.optimization.runtimeChunk('single'),
          {
             from: path.resolve(__dirname, './assets/robots.txt'), //防爬虫文件
             to: './' //到根目录下
          }
    })
  }
}
