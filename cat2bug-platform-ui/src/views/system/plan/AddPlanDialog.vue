<template>
  <!-- 添加或修改测试计划对话框 -->
  <el-dialog :title="$t(title)" :visible.sync="open" width="80%" append-to-body @closed="cancel">
    <el-form ref="form" :model="form" :rules="rules" label-width="120px">
      <el-form-item :label="$t('plan.name')" prop="planName">
        <el-input v-model="form.planName" :placeholder="$t('plan.enter-name')" maxlength="255" />
      </el-form-item>
      <el-row>
        <el-col :span="12">
          <el-form-item :label="$t('plan.version')" prop="planVersion">
            <el-input v-model="form.planVersion" :placeholder="$t('plan.enter-version')" maxlength="255" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('plan.time')" prop="planStartTime">
            <el-date-picker
              v-model="planTimeRang"
              type="datetimerange"
              value-format="yyyy-MM-dd HH:mm:ss"
              :range-separator="$t('time-to')"
              :start-placeholder="$t('plan.start-time')"
              :end-placeholder="$t('plan.end-time')">
            </el-date-picker>
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider content-position="center">{{ $t('plan.select-case') }}</el-divider>
      <!--    模块树和用例列表区域-->
      <multipane layout="vertical" ref="multiPane" class="custom-resizer" @pane-resize-start="dragStopHandle">
        <!--      树形模块选择组件-->
        <div class="tree-module" ref="treeModule" :style="treeModuleStyle">
          <tree-module ref="treeModuleRef" :project-id="projectId" @node-click="moduleClickHandle" @check-change="moduleCheckChangeHandle" :check-visible="true" :edit-visible="false" v-resize="setDragComponentSize" />
        </div>
        <multipane-resizer :style="multipaneStyle"></multipane-resizer>
        <!--      用例列表-->
        <div ref="caseContext" class="case-context">
          <el-table ref="planItemTable" v-loading="loading" :data="caseList" v-resize="setDragComponentSize" @select="handleSelectionChange" @select-all="handleAllSelectionChange">
            <el-table-column type="selection" width="50" align="center" fixed />
            <el-table-column :label="$t('id')" align="left" prop="caseNum" width="80" sortable fixed>
              <template slot-scope="scope">
                <span>{{ caseNumber(scope.row) }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="$t('title')" align="left" prop="caseName" min-width="200" sortable fixed>
              <template slot-scope="scope">
                <div class="table-case-title">
<!--                  <focus-member-list-->
<!--                    v-model="scope.row.focusList"-->
<!--                    module-name="case"-->
<!--                    :data-id="scope.row.caseId" />-->
                  <cat2-bug-text v-model="scope.row.caseName" :tooltip="scope.row.caseName" />
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="$t('module')" align="left" prop="moduleName" min-width="150" sortable />
            <el-table-column :label="$t('level')" align="left" prop="caseLevel" sortable width="80">
              <template slot-scope="scope">
                <cat2-bug-level :level="scope.row.caseLevel" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('preconditions')" align="left" prop="casePreconditions" min-width="250" sortable >
              <template slot-scope="scope">
                <cat2-bug-text v-model="scope.row.casePreconditions" :tooltip="scope.row.casePreconditions" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('step')" align="left" prop="caseStep" width="300" sortable>
              <template slot-scope="scope">
                <step :steps="scope.row.caseStep" style="width: 300px;" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('data')" align="left" prop="caseData" min-width="250" sortable />
            <el-table-column :label="$t('expect')" align="left" prop="caseExpect" min-width="250" sortable >
              <template slot-scope="scope">
                <cat2-bug-text v-model="scope.row.caseExpect" :tooltip="scope.row.caseExpect" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('image')" :key="$t('image')" align="center" prop="imgUrls" width="100">
              <template slot-scope="scope">
                <cat2-bug-preview-image :images="getUrl(scope.row.imgUrls)" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('annex')" :key="$t('annex')" align="left" prop="annexUrls" min-width="300">
              <template slot-scope="scope">
                <div class="annex-list">
                  <cat2-bug-text :content="file" type="down" :tooltip="file" v-for="(file,index) in getUrl(scope.row.annexUrls)" :key="index" />
                </div>
              </template>
            </el-table-column>
          </el-table>

          <pagination
            v-show="total>0"
            :total="total"
            :page.sync="caseQueryParams.pageNum"
            :limit.sync="caseQueryParams.pageSize"
            @pagination="getPlanItemCaseList"
          />
        </div>
      </multipane>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="submitForm">{{ $t('plan.save') }}</el-button>
      <el-button @click="cancel">{{ $t('cancel') }}</el-button>
    </div>
  </el-dialog>
</template>

<script>
import Cat2BugLevel from "@/components/Cat2BugLevel";
import Cat2BugText from "@/components/Cat2BugText";
import Step from "@/views/system/case/components/step";
import TreeModule from "@/components/Module/TreeModule";
import FocusMemberList from "@/components/FocusMemberList";
import Cat2BugPreviewImage from "@/components/Cat2BugPreviewImage";
import { Multipane, MultipaneResizer } from 'vue-multipane';
import {addPlan, getPlan, updatePlan} from "@/api/system/plan";
import {listPlanItemCase} from "@/api/system/PlanItem";
import {listCase, listPlanCaseId} from "@/api/system/case";

const TREE_MODULE_WIDTH_CACHE_KEY = 'plan_case_tree_module_width';

export default {
  name: "AddPlanDialog",
  dicts: ['plan_item_state'],
  components: { Cat2BugLevel,Step,TreeModule,Multipane,MultipaneResizer, FocusMemberList, Cat2BugPreviewImage,Cat2BugText },
  data() {
    return {
      multipaneStyle: {'--marginTop':'0px'},
      treeModuleStyle: {'--treeModuleWidth':'300px'},
      // 遮罩层
      loading: true,
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      planTimeRang: [new Date(), new Date()],
      // 查询参数
      caseQueryParams: {
        pageNum: 1,
        pageSize: 10,
        caseName: null,
        moduleId: null,
        caseType: null,
        caseExpect: null,
        caseLevel: null,
        casePreconditions: null,
        caseNum: null,
        projectId: this.projectId,
        params:{}
      },
      selectCaseIdSet: new Set(),
      // 表单参数
      form: {
        params: {
          modules: [],
          caseIds: []
        }
      },
      // 表单校验
      rules: {
        planName: [
          { required: true, message: this.$i18n.t('plan.name-not-empty'), trigger: "blur" }
        ],
      },
      // 测试计划子项表格数据
      sysPlanItemList: [],
      // 总条数
      total: 0,
      // 测试用例表格数据
      caseList: [],
    }
  },
  directives: {
    resize: {
      // 指令的名称
      bind(el, binding) {
        // el为绑定的元素，binding为绑定给指令的对象
        let width = ''
        let height = ''
        function isResize() {
          const style = document.defaultView.getComputedStyle(el)
          if (width !== style.width || height !== style.height) {
            binding.value({ width: style.width, height: style.height }) // 关键(这传入的是函数,所以执行此函数)
          }
          width = style.width
          height = style.height
        }
        el.__vueSetInterval__ = setInterval(isResize, 300)
      },
      unbind(el) {
        clearInterval(el.__vueSetInterval__)
      }
    }
  },
  computed: {
    /** 用于显示的用例编号 */
    caseNumber: function () {
      return function (val) {
        return '#'+val.caseNum;
      }
    },
    /** 项目ID */
    projectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 字符转url数组 */
    getUrl: function () {
      return function (urls){
        let imgs = urls?urls.split(','):[];
        return imgs.map(i=>{
          return process.env.VUE_APP_BASE_API + i;
        })
      }
    },
  },
  methods: {
    /** 初始化浮动菜单 */
    initFloatMenu() {
      this.$floatMenu.windowsInit(document.querySelector('.main-container'));
      this.$floatMenu.resetMenus([]);
    },
    /** 表单重置 */
    reset() {
      this.selectCaseIdSet.clear();
      this.form = {
        params: {
          modules: [],
          caseIds: []
        },
        planId: null,
        planName: null,
        planVersion: null,
        planStartTime: null,
        planEndTime: null,
        createById: null,
        createTime: null,
        updateById: null,
        updateTime: null,
        projectId: this.projectId,
        reportId: null
      };
      this.caseQueryParams = {
        pageNum: 1,
        pageSize: 10,
        caseName: null,
        moduleId: null,
        caseType: null,
        caseExpect: null,
        caseLevel: null,
        casePreconditions: null,
        caseNum: null,
        projectId: this.projectId,
        params:{
          planId: 'temp'
        }
      }
      this.planTimeRang = [new Date(), new Date()];
      this.sysPlanItemList = [];
      this.resetForm("form");
    },
    /** 取消按钮 */
    cancel() {
      this.open = false;
      this.reset();
      this.$emit('close')
    },
    /** 新增按钮操作 */
    openAdd() {
      this.reset();
      this.open = true;
      this.title = "plan.create";
      this.getTreeModuleWidth();
      this.caseQueryParams.params.planId= 'cat2bug-temp-id';
      this.getPlanItemCaseList();
      this.$nextTick(()=>{
        this.$refs.treeModuleRef.reloadData();
        this.initFloatMenu();
      });
    },
    /** 修改按钮操作 */
    openUpdate(plan) {
      this.reset();
      const planId = plan.planId;
      getPlan(planId).then(response => {
        this.form = response.data;
        this.planTimeRang = [new Date(this.form.planStartTime), new Date(this.form.planEndTime)];
        if(response.data.sysPlanItemList) {
          this.selectCaseIdSet = new Set(response.data.sysPlanItemList.map(i=>i.caseId));
        }
        this.open = true;
        this.title = "plan.edit";
      });
      this.getTreeModuleWidth();
      this.caseQueryParams.params.planId= planId;
      this.getPlanItemCaseList();
      this.initFloatMenu();
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.sysPlanItemList = [...this.selectCaseIdSet].map(id=>{
            return {
              caseId: id
            }
          });
          this.form.projectId = this.projectId;
          this.form.planStartTime = new Date(this.planTimeRang[0]).getTime();
          this.form.planEndTime = new Date(this.planTimeRang[1]).getTime();
          if (this.form.planId != null) {
            updatePlan(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('modify-success'));
              this.open = false;
              this.$emit('add', this.form);
            });
          } else {
            addPlan(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('create-success'));
              this.open = false;
              this.$emit('update', this.form);
            });
          }
        }
      });
    },
    /** 查询测试用例列表 */
    getPlanItemCaseList() {
      this.loading = true;
      this.caseQueryParams.projectId = this.projectId;
      listPlanItemCase(this.caseQueryParams).then(response => {
        this.loading = false;
        this.caseList = response.rows;
        // 设置是否选中
        this.$nextTick(()=>{
          this.caseList.forEach(c=>{
            if(this.selectCaseIdSet.has(c.caseId)) {
              this.$refs.planItemTable.toggleRowSelection(c, true);
            }
          });
        });
        this.total = response.total;
      });
    },
    /** 搜索按钮操作 */
    handleCaseQuery() {
      this.caseQueryParams.pageNum = 1;
      this.getPlanItemCaseList();
    },
    /** 点击模块树中的某个模块前的check操作 */
    moduleCheckChangeHandle(data, checked, indeterminate) {
      if(indeterminate) return;
      listPlanCaseId(data.id).then(res=>{
        if(!res.rows || res.rows.lenght==0) return;
        // 刷新选择的缓存
        res.rows.forEach(id=>{
          if(checked) {
            this.selectCaseIdSet.add(id);
          } else {
            this.selectCaseIdSet.delete(id);
          }
        });
        // 刷新用例列表中的check状态
        this.caseList.forEach(c=>{
          this.$refs.planItemTable.toggleRowSelection(c, this.selectCaseIdSet.has(c.caseId));
        })
      });
    },
    /** 点击模块树中的某个模块操作 */
    moduleClickHandle(moduleId) {
      this.caseQueryParams.moduleId = moduleId;
      this.handleCaseQuery();
    },
    /** 复选框选中数据 */
    handleSysPlanItemSelectionChange(selection) {
      this.checkedSysPlanItem = selection.map(item => item.index)
    },
    /** 测试计划子项序号 */
    rowSysPlanItemIndex({ row, rowIndex }) {
      row.index = rowIndex + 1;
    },
    /** 获取树模型宽度 */
    getTreeModuleWidth() {
      let treeModuleWidth = this.$cache.session.get(TREE_MODULE_WIDTH_CACHE_KEY);
      this.treeModuleStyle['--treeModuleWidth'] = (treeModuleWidth?treeModuleWidth:300)+'px';
    },
    /** 设置树模型宽度到本地缓存 */
    cacheTreeModuleWidth() {
      this.$cache.session.set(TREE_MODULE_WIDTH_CACHE_KEY,this.$refs.treeModule.clientWidth);
    },
    /** 拖动事件完成 */
    dragStopHandle(pane, container, size) {
      this.cacheTreeModuleWidth();
    },
    /** 设置模块与用例列表中间拖动块的尺寸 */
    setDragComponentSize() {
      this.multipaneStyle['--marginTop'] = '0px';
      this.$nextTick(()=> {
        let pageHeight = Math.max(this.$refs.treeModule.scrollHeight || 0, this.$refs.caseContext.scrollHeight || 0)
        this.multipaneStyle['--marginTop'] = pageHeight + 'px';
      })
    },
    handleAllSelectionChange(selection) {
      if(selection.length==0) {
        this.caseList.forEach(c=>{
          this.selectCaseIdSet.delete(c.caseId);
        })
      } else {
        this.caseList.forEach(c=>{
          this.selectCaseIdSet.add(c.caseId);
        })
      }
    },
    handleSelectionChange(selection, row) {
      if(selection.filter(c=>c.caseId==row.caseId).length>0) {
        this.selectCaseIdSet.add(row.caseId);
      } else {
        this.selectCaseIdSet.delete(row.caseId);
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.custom-resizer {
  width: 100%;
  height: auto;
}
.custom-resizer > .multipane-resizer {
  margin: 0; left: 0;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  height: 100%;
  text-align: center;
  &:before {
     display: block;
     content: "";
     width: 3px;
     height: var(--marginTop);
     margin-top: 0px;
     margin-left: -1.5px;
     border-left: 1px solid #DCDFE6;
     border-right: 1px solid #DCDFE6;
  }
  &:hover {
    &:before {
      border-color: #CCC;
    }
  }
}
.case-context {
  flex-grow: 1;
  overflow:hidden;
}
.annex-list {
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  > *:last-child {
    border-bottom: 0px;
  }
  > * {
    border-bottom: 1px dashed #E4E7ED;
  }
}
</style>
