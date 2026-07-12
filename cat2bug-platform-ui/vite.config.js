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

const vue2DatepickerShimRoot = path.resolve(__dirname, 'src/shims/vue2-datepicker')

/** vue-excel-editor 依赖 vue2-datepicker（Vue2）；强制走 Vue3 shim，避免预构建拉取 index.esm.js */
function vue2DatepickerShimPlugin() {
  return {
    name: 'vue2-datepicker-shim',
    enforce: 'pre',
    resolveId(source) {
      const normalized = source.replace(/\\/g, '/')
      if (normalized === 'vue2-datepicker' || normalized === 'vue2-datepicker/index.js') {
        return path.join(vue2DatepickerShimRoot, 'index.js')
      }
      if (normalized === 'vue2-datepicker/index.css') {
        return path.join(vue2DatepickerShimRoot, 'index.css')
      }
      if (normalized.includes('node_modules/vue2-datepicker/')) {
        return path.join(vue2DatepickerShimRoot, 'index.js')
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
        resolvers: [ElementPlusResolver({ importStyle: 'sass' })]
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
      alias: {
        '@': resolve('src'),
        '~@': resolve('src'),
        'vue-multipane': resolve('src/vendor/vue-multipane/index.js'),
        'element-ui': resolve('src/shims/element-ui.js'),
        'element-ui/lib/locale': resolve('src/shims/element-ui-locale.js'),
        'element-ui/packages/table/src/util': resolve('src/utils/element-table-util.js'),
        [path.resolve(__dirname, 'node_modules/markdown-it-vue/src/utils.js')]: resolve('src/vendor/markdown-it-vue-utils.js'),
        'markdown-it-vue/dist/markdown-it-vue.css': path.resolve(__dirname, 'node_modules/markdown-it-vue/dist/markdown-it-vue.css'),
        'markdown-it-vue': resolve('src/components/Markdown/MarkdownItVueCompat.vue'),
        'vuex': resolve('src/shims/vuex.js'),
        'path': resolve('src/utils/path-browser.js'),
        'html-docx-js/dist/html-docx': resolve('src/shims/html-docx-js.js'),
        'html-docx-js': resolve('src/shims/html-docx-js.js'),
        'vuedraggable': resolve('src/shims/vuedraggable.js'),
        'xlsx': resolve('src/shims/xlsx.js'),
        'vue2-datepicker': resolve('src/shims/vue2-datepicker'),
      },
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
      include: ['vue', 'vue-router', 'pinia', 'axios', 'js-cookie', 'nprogress', 'vue-excel-editor', 'sortablejs', 'xlsx', 'vue-datepicker-next', 'date-format-parse'],
      exclude: ['vue2-datepicker'],
      entries: ['index.html'],
      esbuildOptions: {
        alias: {
          'vue2-datepicker': path.join(vue2DatepickerShimRoot, 'index.js')
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
      proxy: {
        [baseApi]: {
          target: 'http://localhost:2020',
          changeOrigin: true,
          rewrite: (p) => p.replace(new RegExp(`^${baseApi}`), '')
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
