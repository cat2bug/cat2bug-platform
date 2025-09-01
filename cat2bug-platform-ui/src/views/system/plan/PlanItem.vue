<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="测试计划" prop="planId">
        <el-input
          v-model="queryParams.planId"
          placeholder="请输入测试计划"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="交付物" prop="moduleId">
        <el-input
          v-model="queryParams.moduleId"
          placeholder="请输入交付物"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="测试用例" prop="caseId">
        <el-input
          v-model="queryParams.caseId"
          placeholder="请输入测试用例"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="计划子项状态" prop="planItemState">
        <el-select v-model="queryParams.planItemState" placeholder="请选择计划子项状态" clearable>
          <el-option
            v-for="dict in dict.type.plan_item_state"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="更新人" prop="updateById">
        <el-input
          v-model="queryParams.updateById"
          placeholder="请输入更新人"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="更新时间">
        <el-date-picker
          v-model="daterangeUpdateTime"
          style="width: 240px"
          value-format="yyyy-MM-dd"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item label="缺陷" prop="defectId">
        <el-input
          v-model="queryParams.defectId"
          placeholder="请输入缺陷"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:PlanItem:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:PlanItem:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:PlanItem:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:PlanItem:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="PlanItemList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="测试计划子项ID" align="center" prop="planItemId" />
      <el-table-column label="测试计划" align="center" prop="planId" />
      <el-table-column label="交付物" align="center" prop="moduleId" />
      <el-table-column label="测试用例" align="center" prop="caseId" />
      <el-table-column label="计划子项状态" align="center" prop="planItemState">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.plan_item_state" :value="scope.row.planItemState"/>
        </template>
      </el-table-column>
      <el-table-column label="更新人" align="center" prop="updateById" />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="缺陷" align="center" prop="defectId" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:PlanItem:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:PlanItem:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改测试计划子项对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="测试计划" prop="planId">
          <el-input v-model="form.planId" placeholder="请输入测试计划" />
        </el-form-item>
        <el-form-item label="交付物" prop="moduleId">
          <el-input v-model="form.moduleId" placeholder="请输入交付物" />
        </el-form-item>
        <el-form-item label="测试用例" prop="caseId">
          <el-input v-model="form.caseId" placeholder="请输入测试用例" />
        </el-form-item>
        <el-form-item label="计划子项状态" prop="planItemState">
          <el-select v-model="form.planItemState" placeholder="请选择计划子项状态">
            <el-option
              v-for="dict in dict.type.plan_item_state"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="更新人" prop="updateById">
          <el-input v-model="form.updateById" placeholder="请输入更新人" />
        </el-form-item>
        <el-form-item label="缺陷" prop="defectId">
          <el-input v-model="form.defectId" placeholder="请输入缺陷" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPlanItem, getPlanItem, delPlanItem, addPlanItem, updatePlanItem } from "@/api/system/PlanItem";

export default {
  name: "PlanItem",
  dicts: ['plan_item_state'],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 测试计划子项表格数据
      PlanItemList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 缺陷时间范围
      daterangeUpdateTime: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        planId: null,
        moduleId: null,
        caseId: null,
        planItemState: null,
        updateById: null,
        updateTime: null,
        defectId: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询测试计划子项列表 */
    getList() {
      this.loading = true;
      this.queryParams.params = {};
      if (null != this.daterangeUpdateTime && '' != this.daterangeUpdateTime) {
        this.queryParams.params["beginUpdateTime"] = this.daterangeUpdateTime[0];
        this.queryParams.params["endUpdateTime"] = this.daterangeUpdateTime[1];
      }
      listPlanItem(this.queryParams).then(response => {
        this.PlanItemList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        planItemId: null,
        planId: null,
        moduleId: null,
        caseId: null,
        planItemState: null,
        updateById: null,
        updateTime: null,
        defectId: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.daterangeUpdateTime = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.planItemId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加测试计划子项";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const planItemId = row.planItemId || this.ids
      getPlanItem(planItemId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改测试计划子项";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.planItemId != null) {
            updatePlanItem(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addPlanItem(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const planItemIds = row.planItemId || this.ids;
      this.$modal.confirm('是否确认删除测试计划子项编号为"' + planItemIds + '"的数据项？').then(function() {
        return delPlanItem(planItemIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/PlanItem/export', {
        ...this.queryParams
      }, `PlanItem_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
