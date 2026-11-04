/**
 * 缺陷 Excel / 看板 / 甘特：左侧交付物树显隐与宽度缓存。
 * 用法：mixins: [createDefectModuleTreeSidebarMixin({ visibleKey, widthKey })]
 * 宿主须有 queryParams、search(params)，以及可选 ref treeModule。
 */
export function createDefectModuleTreeSidebarMixin({ visibleKey, widthKey }) {
  if (!visibleKey || !widthKey) {
    throw new Error('createDefectModuleTreeSidebarMixin: visibleKey/widthKey required')
  }
  return {
    data() {
      return {
        showModuleTree: true,
        treeModuleStyle: { '--treeModuleWidth': '300px' },
        multipaneStyle: {},
        defectTreeToolbarHeight: null
      }
    },
    computed: {
      paneResizerRuleVars() {
        const h = this.defectTreeToolbarHeight
        return {
          '--multipane-header-rule-offset': h != null && h > 0 ? `${h}px` : '48px'
        }
      }
    },
    created() {
      const treeVis = this.$cache && this.$cache.local
        ? this.$cache.local.get(visibleKey)
        : null
      this.showModuleTree = !(treeVis === '0' || treeVis === 'false')
      this.getTreeModuleWidth()
    },
    methods: {
      moduleTreeClickHandle(moduleId) {
        const id =
          moduleId !== undefined && moduleId !== null && moduleId !== ''
            ? moduleId
            : null
        if (!this.queryParams || typeof this.queryParams !== 'object') {
          this.queryParams = {}
        }
        this.queryParams.moduleId = id
        if (Object.prototype.hasOwnProperty.call(this.queryParams, 'pageNum')) {
          this.queryParams.pageNum = 1
        }
        if (typeof this.search === 'function') {
          this.search(this.queryParams)
        }
      },
      getTreeModuleWidth() {
        const w =
          this.$cache && this.$cache.session
            ? this.$cache.session.get(widthKey)
            : null
        this.treeModuleStyle['--treeModuleWidth'] = (w ? w : 300) + 'px'
      },
      cacheTreeModuleWidth() {
        if (this.$refs.treeModule && this.$cache && this.$cache.session) {
          this.$cache.session.set(widthKey, this.$refs.treeModule.clientWidth)
        }
      },
      dragStopHandle() {
        if (this.showModuleTree) {
          this.cacheTreeModuleWidth()
        }
        if (typeof this.onModuleTreeLayoutChanged === 'function') {
          this.onModuleTreeLayoutChanged()
        }
      },
      toggleModuleTreeVisible() {
        this.showModuleTree = !this.showModuleTree
        if (this.$cache && this.$cache.local) {
          this.$cache.local.set(visibleKey, this.showModuleTree ? '1' : '0')
        }
        this.$nextTick(() => {
          if (typeof this.onModuleTreeLayoutChanged === 'function') {
            this.onModuleTreeLayoutChanged()
          }
        })
      },
      syncDefectTreeToolbarHeightFromTree() {
        this.$nextTick(() => {
          const root = this.$refs.treeModule
          if (!root || typeof root.querySelector !== 'function') return
          const bar = root.querySelector('.tree-tools')
          if (!bar) return
          const h = Math.round(bar.getBoundingClientRect().height)
          if (h > 0 && h !== this.defectTreeToolbarHeight) {
            this.defectTreeToolbarHeight = h
          }
        })
      }
    }
  }
}

export const DEFECT_EXCEL_TREE_VISIBLE_KEY = 'defect_excel_tree_module_visible'
export const DEFECT_EXCEL_TREE_WIDTH_KEY = 'defect_excel_tree_module_width'
export const DEFECT_KANBAN_TREE_VISIBLE_KEY = 'defect_kanban_tree_module_visible'
export const DEFECT_KANBAN_TREE_WIDTH_KEY = 'defect_kanban_tree_module_width'
