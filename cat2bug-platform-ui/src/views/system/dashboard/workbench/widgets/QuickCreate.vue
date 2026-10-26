<template>
  <div class="quick-create">
    <el-button
      v-if="canDefect"
      type="primary"
      plain
      class="cat2bug-btn-outline"
      @click="openDefect"
    >
      <el-icon><Plus /></el-icon>{{ $t('defect.create') }}
    </el-button>
    <el-button v-if="canCase" @click="openCase">
      <el-icon><Plus /></el-icon>{{ $t('case.create') }}
    </el-button>
    <el-button v-if="canPlan" @click="openPlan">
      <el-icon><Plus /></el-icon>{{ $t('plan.create') }}
    </el-button>
    <el-button v-if="canDocument" :loading="creatingDoc" @click="createDocument">
      <el-icon><Plus /></el-icon>{{ $t('doc.create-file') }}
    </el-button>

    <add-defect
      v-if="projectId"
      ref="addDefect"
      :project-id="projectId"
      append-to-body
      @added="onCreated"
    />
    <add-case ref="addCase" append-to-body @added="onCreated" />
    <add-plan-dialog ref="addPlan" @add="onCreated" />
  </div>
</template>

<script>
import { Plus } from '@element-plus/icons-vue'
import { getCurrentProjectId } from '@/utils/store-context'
import { checkPermi } from '@/utils/permission'
import { addDocument, listDocument } from '@/api/system/document'
import AddDefect from '@/components/Defect/AddDefect'
import AddCase from '@/components/Case/AddCase'
import AddPlanDialog from '@/components/Plan/AddPlanDialog'

export default {
  name: 'QuickCreate',
  components: { Plus, AddDefect, AddCase, AddPlanDialog },
  data() {
    return {
      projectId: null,
      creatingDoc: false
    }
  },
  computed: {
    canDefect() { return checkPermi(['system:defect:add']) },
    canCase() { return checkPermi(['system:case:add']) },
    canPlan() { return checkPermi(['system:plan:add']) },
    canDocument() { return checkPermi(['system:document:add']) }
  },
  mounted() {
    this.projectId = getCurrentProjectId()
  },
  methods: {
    openDefect() {
      this.projectId = getCurrentProjectId()
      this.$nextTick(() => {
        if (this.$refs.addDefect) this.$refs.addDefect.open()
      })
    },
    openCase() {
      if (this.$refs.addCase) this.$refs.addCase.open()
    },
    openPlan() {
      if (this.$refs.addPlan) this.$refs.addPlan.openAdd()
    },
    async createDocument() {
      if (this.creatingDoc) return
      const projectId = getCurrentProjectId()
      if (!projectId) return
      this.creatingDoc = true
      try {
        const docName = await this.resolveDefaultMarkdownName(projectId)
        const res = await addDocument({
          projectId,
          docPid: 0,
          docName,
          docType: 1,
          fileExtension: 'md',
          fileUrl: null
        })
        const docId = res && res.data
        if (!docId) {
          this.$modal.msgError(this.$t('exception'))
          return
        }
        this.$router.push({
          name: 'HandleMarkdownDocument',
          params: { docId: String(docId) }
        })
      } catch (e) {
        /* 错误由 request 拦截器提示 */
      } finally {
        this.creatingDoc = false
      }
    },
    resolveDefaultMarkdownName(projectId) {
      const base = this.$t('doc.default-md-name')
      return listDocument({
        projectId,
        docPid: 0,
        pageNum: 1,
        pageSize: 9999
      }).then(response => {
        const rows = (response && response.rows) || []
        const names = new Set(rows.map(r => r && r.docName).filter(Boolean))
        if (!names.has(base)) return base
        let n = 2
        while (names.has(base + n)) n += 1
        return base + n
      })
    },
    onCreated() {
      this.$emit('created')
    }
  }
}
</script>

<style scoped lang="scss">
.quick-create {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cat2bug-toolbar-item-gap, 10px);

  /* 去掉 EP 相邻按钮默认 margin-left，避免与 gap 叠成双倍间隔 */
  :deep(.el-button + .el-button) {
    margin-left: 0;
  }
  :deep(.el-button) {
    margin: 0;
  }
  :deep(.el-button .el-icon) {
    margin-right: 4px;
  }
}
</style>
