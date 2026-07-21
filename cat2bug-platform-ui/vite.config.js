import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'
import fs from 'node:fs'
import { fileURLToPath } from 'node:url'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import viteCompression from 'vite-plugin-compression'
import { viteStaticCopy } from 'vite-plugin-static-copy'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

function resolve(dir) {
  return path.join(__dirname, dir)
}

const docsStaticDir = path.resolve(__dirname, '../readme/production')

/** 生产环境排除 views/tool（与 webpack IgnorePlugin 对齐） */
function excludeToolViewsPlugin(enabled) {
  const emptyId = '\0empty-tool-view'
  return {
    name: 'exclude-tool-views',
    enforce: 'pre',
    resolveId(source) {
      if (enabled) return null
      const normalized = source.replace(/\\/g, '/')
      if (normalized.includes('/views/tool/') || normalized.includes('views/tool/')) {
        return emptyId
      }
      return null
    },
    load(id) {
      if (id === emptyId) {
        return 'export default { name: "EmptyToolView", render() { return null } }'
      }
      return null
    }
  }
}

/**
 * Git Smart HTTP 开发代理：透传 Content-Length，避免大 body push 被截断。
 */
function configureGitSmartHttpProxy(proxy) {
  proxy.on('proxyReq', (proxyReq, req) => {
    if (req.headers['content-length']) {
      proxyReq.setHeader('Content-Length', req.headers['content-length'])
    }
  })
}

/** 开发环境 /docs 静态目录（与 vue.config devServer.before 对齐） */
function docsDevMiddleware() {
  return {
    name: 'docs-dev-static',
    configureServer(server) {
      server.middlewares.use('/docs', (req, res, next) => {
        const urlPath = decodeURIComponent((req.url || '/').split('?')[0])
        const filePath = path.join(docsStaticDir, urlPath)
        if (filePath.startsWith(docsStaticDir) && fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
          fs.createReadStream(filePath).pipe(res)
          return
        }
        next()
      })
    }
  }
}

const vue2DatepickerAdapterRoot = path.resolve(__dirname, 'src/adapters/vue2-datepicker')
const vuedraggableLegacyPath = path.resolve(__dirname, 'src/shims/vuedraggable-legacy.js')

/** vue-excel-editor 依赖 vue2-datepicker（Vue2）；强制走 Vue3 adapter，避免预构建拉取 index.esm.js */
function vue2DatepickerShimPlugin() {
  return {
    name: 'vue2-datepicker-shim',
    enforce: 'pre',
    resolveId(source) {
      const normalized = source.replace(/\\/g, '/')
      if (normalized === 'vue2-datepicker' || normalized === 'vue2-datepicker/index.js') {
        return path.join(vue2DatepickerAdapterRoot, 'index.js')
      }
      if (normalized === 'vue2-datepicker/index.css') {
        return path.join(vue2DatepickerAdapterRoot, 'index.css')
      }
      if (normalized.includes('node_modules/vue2-datepicker/')) {
        return path.join(vue2DatepickerAdapterRoot, 'index.js')
      }
      return null
    }
  }
}

/** vue-excel-editor / tool builder / Cat2Doc 仍使用 v2 风格 API；其余组件走 vuedraggable v4 */
function vuedraggableLegacyShimPlugin() {
  return {
    name: 'vuedraggable-legacy-shim',
    enforce: 'pre',
    resolveId(source, importer) {
      if (source !== 'vuedraggable') return null
      const from = (importer || '').replace(/\\/g, '/')
      if (
        from.includes('node_modules/vue-excel-editor/')
        || from.includes('/vendor/vue-excel-editor/')
        || from.includes('/views/tool/build/')
        || from.includes('/components/Cat2BugDocument/')
      ) {
        return vuedraggableLegacyPath
      }
      return null
    }
  }
}

function buildProcessEnvDefine(env) {
  const define = {}
  Object.entries(env).forEach(([key, value]) => {
    define[`process.env.${key}`] = JSON.stringify(value)
  })
  define['process.env.NODE_ENV'] = JSON.stringify(env.NODE_ENV || process.env.NODE_ENV || 'development')
  return define
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), ['VUE_APP', 'VITE', 'ENV', 'NODE'])
  const mergedEnv = {
    ...env,
    NODE_ENV: mode === 'embedded' ? 'embedded' : (mode === 'development' ? 'development' : 'production')
  }

  const isDev = mode === 'development'
  const isEmbedded = mode === 'embedded'
  const isAnalyze = process.env.ANALYZE === 'true'
  const devToolsEnabled = mergedEnv.VUE_APP_ENABLE_DEV_TOOLS === 'true'
  const port = Number(process.env.port || process.env.npm_config_port || 2222)
  const devHost = process.env.DEV_HOST || '0.0.0.0'
  const baseApi = mergedEnv.VUE_APP_BASE_API || '/dev-api'
  const baseWs = mergedEnv.VUE_APP_BASE_WEBSOCKET || '/ws'
  const fileView = mergedEnv.VUE_APP_FILE_VIEW || '/file-view'

  const outDir = isAnalyze
    ? 'analyze'
    : (isEmbedded ? '../cat2bug-platform-admin/src/main/resources/static' : 'dist')

  const docsCopyTarget = isEmbedded
    ? '../cat2bug-platform-admin/src/main/resources/static/docs'
    : 'dist/docs'

  return {
    base: mergedEnv.VUE_PUBLIC_PATH || '/',
    esbuild: {
      jsxFactory: 'h',
      jsxFragment: 'Fragment',
      include: /src\/.*\.[jt]sx?$|src\/.*\.js$/
    },
    plugins: [
      vue2DatepickerShimPlugin(),
      vuedraggableLegacyShimPlugin(),
      vue(),
      ...(isDev ? [docsDevMiddleware()] : []),
      createSvgIconsPlugin({
        iconDirs: [resolve('src/assets/icons/svg')],
        symbolId: 'icon-[name]'
      }),
      AutoImport({
        resolvers: [ElementPlusResolver({ importStyle: 'sass' })]
      }),
      Components({
        resolvers: [ElementPlusResolver({ importStyle: 'sass' })],
        // CaseList.vue exists in CloudCase2 and Plan dialogs; both are imported explicitly.
        exclude: [
          /[\\/]Cloud[\\/]CloudCase2[\\/]chat[\\/]CaseList\.vue$/,
          /[\\/]Plan[\\/]HandlePlanDialog[\\/]CaseList\.vue$/
        ]
      }),
      ...(!isDev ? [
        viteCompression({
          verbose: false,
          disable: false,
          threshold: 0,
          algorithm: 'gzip',
          ext: '.gz',
          deleteOriginFile: false
        }),
        viteStaticCopy({
          targets: [
            {
              src: docsStaticDir,
              dest: isEmbedded ? '../cat2bug-platform-admin/src/main/resources/static' : 'dist',
              rename: 'docs'
            }
          ]
        })
      ] : [])
    ],
    resolve: {
      alias: [
        {
          find: /^vue-cropper$/,
          replacement: path.resolve(__dirname, 'node_modules/vue-cropper/dist/vue-cropper.es.js')
        },
        { find: '@', replacement: resolve('src') },
        { find: '~@', replacement: resolve('src') },
        { find: 'vue-multipane', replacement: resolve('src/vendor/vue-multipane/index.js') },
        { find: 'vue-excel-editor', replacement: resolve('src/vendor/vue-excel-editor/index.js') },
        { find: 'element-ui', replacement: resolve('src/shims/element-ui.js') },
        { find: 'element-ui/lib/locale', replacement: resolve('src/shims/element-ui-locale.js') },
        { find: 'element-ui/packages/table/src/util', replacement: resolve('src/utils/element-table-util.js') },
        { find: 'path', replacement: resolve('src/utils/path-browser.js') },
        { find: 'html-docx-js/dist/html-docx', replacement: resolve('src/shims/html-docx-js.js') },
        { find: 'html-docx-js', replacement: resolve('src/shims/html-docx-js.js') },
        { find: 'xlsx', replacement: resolve('src/shims/xlsx.js') },
        { find: 'vue2-datepicker', replacement: resolve('src/adapters/vue2-datepicker') }
      ],
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
    },
    css: {
      preprocessorOptions: {
        scss: {
          silenceDeprecations: ['legacy-js-api', 'import']
        }
      }
    },
    define: buildProcessEnvDefine(mergedEnv),
    optimizeDeps: {
      include: [
        'vue',
        'vue-router',
        'pinia',
        'axios',
        'js-cookie',
        'nprogress',
        'sortablejs',
        'xlsx',
        'vue-datepicker-next',
        'date-format-parse',
        'vuedraggable/dist/vuedraggable.umd.js'
      ],
      exclude: ['vue2-datepicker', 'vue-cropper'],
      entries: [
        'index.html',
        'src/views/system/defect/index.vue',
        'src/views/system/case/index.vue',
        'src/views/system/plan/index.vue'
      ],
      holdUntilCrawlEnd: true,
      esbuildOptions: {
        alias: {
          'vue2-datepicker': path.join(vue2DatepickerAdapterRoot, 'index.js'),
          'vue-cropper': path.resolve(__dirname, 'node_modules/vue-cropper/dist/vue-cropper.es.js')
        },
        loader: { '.js': 'jsx' },
        jsx: 'transform',
        jsxFactory: 'h',
        jsxFragment: 'Fragment'
      }
    },
    server: {
      host: devHost,
      port,
      open: true,
      warmup: {
        clientFiles: [
          './src/views/system/defect/index.vue',
          './src/views/system/defect/list/table.vue',
          './src/components/Cat2BugStatistic/index.vue',
          './src/views/system/case/index.vue',
          './src/views/system/plan/index.vue',
          './src/components/Cloud/CloudCase2/chat/CaseHelloWorld.vue'
        ]
      },
      proxy: {
        [baseApi]: {
          target: 'http://localhost:2020',
          changeOrigin: true,
          rewrite: (p) => p.replace(new RegExp(`^${baseApi}`), '')
        },
        // Git Smart HTTP：clone/push 走与页面同源的 /git/**，转发到后端（勿返回 SPA HTML）
        // 大仓 push 易超时/断流，需拉长代理超时并关闭响应缓冲
        '/git': {
          target: 'http://localhost:2020',
          changeOrigin: true,
          timeout: 0,
          proxyTimeout: 0,
          configure: configureGitSmartHttpProxy
        },
        // 命名空间 clone：/{teamKey}/{projectKey}/code/{slug}.git/**
        '^/[^/]+/[^/]+/code/[^/]+\\.git': {
          target: 'http://localhost:2020',
          changeOrigin: true,
          timeout: 0,
          proxyTimeout: 0,
          configure: configureGitSmartHttpProxy
        },
        [baseWs]: {
          target: 'ws://localhost:2020',
          changeOrigin: true,
          ws: true,
          rewrite: (p) => p.replace(new RegExp(`^${baseWs}`), '')
        },
        [fileView]: {
          target: 'ws://localhost:8012',
          changeOrigin: true,
          ws: true,
          rewrite: (p) => p.replace(new RegExp(`^${fileView}`), '')
        }
      }
    },
    build: {
      outDir,
      assetsDir: mergedEnv.VUE_APP_STATIC_PATH || 'static',
      sourcemap: false,
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (id.includes('node_modules')) {
              if (id.includes('element-plus')) {
                return 'chunk-element-plus'
              }
              return 'chunk-libs'
            }
            if (id.includes('/src/components/')) {
              return 'chunk-commons'
            }
          }
        }
      }
    }
  }
})
