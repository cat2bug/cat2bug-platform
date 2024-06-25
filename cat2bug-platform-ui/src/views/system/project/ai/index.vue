<template>
  <div class="app-container">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('project.ai-model-manager')">
      </el-page-header>
    </el-row>
    <el-row :gutter="10" class="mb8">
      <el-col :span="24" class="flex-right">
        <el-autocomplete
          class="inline-input"
          prefix-icon="el-icon-cloudy"
          v-model="downloadParams.name"
          :fetch-suggestions="searchDownloadModelAutocomplete"
          :placeholder="$t('ai.please-enter-download-name')"
          @select="handleSelectSearchModel"
        ></el-autocomplete>
        <el-button
          type="primary"
          style="float: right;"
          plain
          icon="el-icon-download"
          size="mini"
          @click="downloadModelHandle"
          v-hasPermi="['system:ai:add']"
        >{{$t('ai.download-model')}}</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="aiList" @selection-change="handleSelectionChange" :key="tableKey">
      <el-table-column label="业务模型名称" align="left" prop="name">
        <template slot-scope="scope">
          <div class="col">
            <span>{{ scope.row.name }}</span>
            <span v-if="isError(scope.row) && scope.row.error" class="error">{{ `${$t('download.error')}:${scope.row.error}` }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="size" width="150">
        <template slot-scope="scope">
          <el-progress v-if="isPulling(scope.row)" :percentage="pullProgress(scope.row)"></el-progress>
          <span v-else>{{ modelState(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="模型尺寸" align="center" prop="size" width="150">
        <template slot-scope="scope">
          <span v-if="isPulling(scope.row)">{{ `${fileSizeUnit(scope.row.pullCompleted+scope.row.pullCompletedLayerSize)}/${fileSizeUnit(scope.row.size)}` }}</span>
          <span v-else>{{ fileSizeUnit(scope.row.size) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="业务识别模型" align="center" width="100">
        <template slot-scope="scope">
          <el-radio v-model="modelOption.businessModule" :label="scope.row.name" @input="handleUpdateProjectModule">&nbsp;</el-radio>
        </template>
      </el-table-column>
      <el-table-column label="图像处理模型" align="center" width="100">
      <template slot-scope="scope">
        <el-radio v-model="modelOption.imageModule"  :label="scope.row.name" @input="handleUpdateProjectModule">&nbsp;</el-radio>
      </template>
    </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="100">
        <template slot-scope="scope">
          <el-button
            v-if="isError(scope.row) || isEmpty(scope.row)"
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="downloadModel(scope.row.name)"
            v-hasPermi="['system:ai:add']"
          >{{ $t('re-download') }}</el-button>
          <el-button
            v-if="isPulling(scope.row)==false"
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:ai:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

<!--    <pagination-->
<!--      v-show="total>0"-->
<!--      :total="total"-->
<!--      :page.sync="queryParams.pageNum"-->
<!--      :limit.sync="queryParams.pageSize"-->
<!--      @pagination="getList"-->
<!--    />-->

    <!-- 添加或修改AI模型配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="项目ID" prop="projectId">
          <el-input v-model="form.projectId" placeholder="请输入项目ID" />
        </el-form-item>
        <el-form-item label="创建人ID" prop="createById">
          <el-input v-model="form.createById" placeholder="请输入创建人ID" />
        </el-form-item>
        <el-form-item label="更新人ID" prop="updateById">
          <el-input v-model="form.updateById" placeholder="请输入更新人ID" />
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
import {fileSizeUnit} from "@/utils";
import {
  listAi,
  getAi,
  delAi,
  addAi,
  updateAi,
  downloadModel,
  delModel,
  defaultListAiModelList,
  updateProjectModelOption
} from "@/api/system/ai";
const AI_MODEL_SEARCH_LIST = 'ai_model_search_list';
const AI_MODEL_PULL_CACHE = 'ai_model_pull_cache';
const AI_MODEL_EMPTY_STATE = 'EMPTY';
const AI_MODEL_COMPLETED_STATE = 'COMPLETED';
const AI_MODEL_PULLING_STATE = 'PULLING';
const AI_MODEL_ERROR_STATE = 'ERROR';
const AI_MODEL_SUCCESS_STATE = 'SUCCESS';
export default {
  name: "Ai",
  data() {
    return {
      searchModelSet: [],
      tableKey: new Date().getTime(),
      topicId: null,
      modelOption: {
        businessModule:'',
        imageModule:''
      },
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
      aiList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      downloadParams: {},
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        businessModule: null,
        imageModule: null,
        projectId: null,
        createById: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
      }
    };
  },
  computed: {
    isPulling: function () {
      return function (model) {
        return model.state == AI_MODEL_PULLING_STATE;
      }
    },
    isEmpty: function () {
      return function (model) {
        return model.state == AI_MODEL_EMPTY_STATE;
      }
    },
    isError: function () {
      return function (model) {
        return model.state == AI_MODEL_ERROR_STATE;
      }
    },
    pullProgress: function () {
      return function (model) {
        if(model.pullCompleted && model.size && model.size>0) {
          return Math.floor(((model.pullCompleted||0)+(model.pullCompletedLayerSize||0))/(model.size||1) * 100);
        } else {
          return 0;
        }
      }
    },
    modelState: function () {
      return function (model) {
        switch (model.state) {
          case AI_MODEL_PULLING_STATE:
            return '下载中';
          case AI_MODEL_ERROR_STATE:
            return '下载错误'
          case AI_MODEL_SUCCESS_STATE:
          case AI_MODEL_COMPLETED_STATE:
            return '已下载'
          case AI_MODEL_EMPTY_STATE:
            return '未下载'
          default:
            return '-'
        }
      }
    }
  },
  created() {
    this.getDefaultList();
    this.getList();
  },
  mounted() {
    // 订阅WebSocket下载模型消息
    this.topicId = this.$topic.subscribe(this.$topic.AI_MODEL_PULL_TOPIC, (name, data) => {
      const pull = data.data;
      // 遍历所有模型列表，更新指定模型的下载进度
      for(let i=0;i<this.aiList.length;i++) {
        let m = this.aiList[i];
        if(m.name!=pull.name) continue;
        m.state = pull.state;
        switch (pull.state) {
          case AI_MODEL_PULLING_STATE:
            m.pullTotal = pull.total;
            m.pullCompleted = pull.completed;
            if(pull.layer && pull.layer != 'manifest' && m.layer!=pull.layer) {
              m.layer = pull.layer;
              m.pullCompletedLayerSize = m.size;
              m.size+=pull.total;
              this.savePullModel(m);
            }
            break;
          case AI_MODEL_ERROR_STATE:
            m.error=pull.error;
            this.$message.error(pull.error);
            break;
          case AI_MODEL_SUCCESS_STATE:
            this.removePullModel(m.name);
            this.getList();
            break;
          default:
            break;
        }
        this.tableKey='table_'+new Date().getTime();
      }
    });
  },
  beforeDestroy() {
    // 取消下载模型的WebSocket订阅
    this.$topic.unsubscribe(this.topicId);
    this.topicId = null;
  },
  methods: {
    /** 文件空间单位计算函数 */
    fileSizeUnit,
    /** 获取当前项目ID */
    getProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 保存下载的模型信息 */
    savePullModel(model) {
      let list = this.getPullModelList() || [];
      list = list.filter(m=>m.name != model.name);
      list.unshift(model);
      this.$cache.local.setJSON(AI_MODEL_PULL_CACHE, list);
    },
    /** 获取下载的模型信息 */
    getPullModel(modelName) {
      let list = this.getPullModelList();
      if(!list || list.length==0) return null;
      return list.find(m=>m.name==modelName);
    },
    /** 获取下载的模型列表 */
    getPullModelList() {
      let list = this.$cache.local.getJSON(AI_MODEL_PULL_CACHE);
      return list || [];
    },
    /** 移除下载的模型信息 */
    removePullModel(modelName) {
      let list = this.getPullModelList();
      if(!list || list.length==0) return;
      list = list.filter(m=>m.name!=modelName)
      this.$cache.local.setJSON(AI_MODEL_PULL_CACHE, list);
    },
    /** 保存下载模型input的自动补全数据列表 */
    saveDownloadModelAutocomplete(name) {
      this.searchModelSet = this.searchModelSet.filter(m=>m.value != name);
      this.searchModelSet.unshift({
        value: name
      });
      if(this.searchModelSet.length>9) {
        this.searchModelSet.shift();
      }
      this.$cache.local.setJSON(AI_MODEL_SEARCH_LIST,this.searchModelSet);
    },
    /** 搜索下载模型自动补全列表 */
    searchDownloadModelAutocomplete(queryString, cb) {
      let results = queryString ? this.searchModelSet.filter(m=>m.value && m.value.indexOf(queryString)>-1) : this.searchModelSet;
      cb(results);
    },
    /** 返回上一页 */
    goBack() {
      this.$goBack();
    },
    /** 选择模型名称 */
    handleSelectSearchModel(modelName) {
    },
    /** 获取默认模型列表 */
    getDefaultList() {
      defaultListAiModelList().then(res=>{
        let list = this.$cache.local.getJSON(AI_MODEL_SEARCH_LIST) || [];
        res.rows.forEach(m=>{
          if(!list.find(m1=>m1.value==m)) {
            list.push({
              value: m
            })
          }
        });
        this.searchModelSet = list;
      })
    },
    /** 查询AI模型配置列表 */
    getList() {
      this.loading = true;
      this.queryParams.projectId = this.getProjectId();
      listAi(this.queryParams).then(response => {
        let cacheModels = this.getPullModelList();
        let rows = response.rows.map(m=>{
          m.pullCompleted = 0;
          m.pullTotal = 100;
          m.pullCompletedLayerSize = 0;
          m.size = m.size||0;
          m.layer = null;

          if(m.businessModule) {
            this.modelOption.businessModule = m.businessModule;
          }
          if(m.imageModule) {
            this.modelOption.imageModule = m.imageModule;
          }
          return m;
        });
        let emptyModels = rows.filter(m=>m.state==AI_MODEL_EMPTY_STATE);
        let completedModels = rows.filter(m=>m.state==AI_MODEL_COMPLETED_STATE);
        // 筛选cacheModels，emptyModels同时有的，然后删除emptyModels重复的
        emptyModels = emptyModels.filter(m=>!cacheModels.find(m1=>m.name==m1.name));
        // 筛选cacheModels，completedModels同时有的，然后删除cacheModels重复的
        cacheModels = cacheModels.filter(m=>!completedModels.find(m1=>m.name==m1.name));
        this.aiList = [...cacheModels,...emptyModels,...completedModels];
        this.total = this.aiList.length;
        this.loading = false;
      });
    },
    /** 取消按钮 */
    cancel() {
      this.open = false;
      this.reset();
    },
    /** 表单重置 */
    reset() {
      this.form = {
        aiId: null,
        businessModule: null,
        imageModule: null,
        projectId: null,
        createTime: null,
        createById: null,
        updateTime: null,
        updateById: null
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
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.aiId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加AI模型配置";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const aiId = row.aiId || this.ids
      getAi(aiId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改AI模型配置";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.aiId != null) {
            updateAi(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addAi(this.form).then(response => {
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
      if(this.isError(row)) {
        this.getList();
        return;
      }
      this.$modal.confirm('是否确认删除名称为"' + row.name + '"的模型？').then(function() {
        return delModel({name:row.name});
      }).then(() => {
        this.$modal.msgSuccess("删除成功");
        this.removePullModel(row.name);
        this.getList();
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/ai/export', {
        ...this.queryParams
      }, `ai_${new Date().getTime()}.xlsx`)
    },
    downloadModelHandle() {
      this.downloadModel();
    },
    /** 下载模型 */
    downloadModel(name) {
      this.downloadParams.name = name || this.downloadParams.name;
      if(!this.downloadParams.name) {
        this.$message.error(this.$i18n.t('ai.name-cannot-empty').toString());
        return;
      }
      downloadModel(this.downloadParams).then(res=>{
        this.saveDownloadModelAutocomplete(this.downloadParams.name);
        // 如果当前模块table里没有要下的模型，就将正在下载的模型信息添加到table里
        if(!this.aiList.find(m=>m.name==this.downloadParams.name)) {
          let newModel = this.getPullModel(this.downloadParams.name) || {
            name: this.downloadParams.name,
            pullCompleted: 0,
            pullTotal: 100,
            pullCompletedLayerSize: 0,
            size: 0,
            layer: null,
            state: AI_MODEL_PULLING_STATE
          };
          this.aiList = [
            ...[newModel],
            ...this.aiList
          ];
        }
      });
    },
    handleUpdateProjectModule(name) {
      this.modelOption.projectId = this.getProjectId();
      updateProjectModelOption(this.modelOption).then(res=>{
        this.$message.success('设置模型成功');
      });
    }
  }
};
</script>
<style lang="scss" scoped>
.flex-right {
  display: inline-flex;
  justify-content: flex-end;
  gap: 10px;
}
.error {
  color: red;
  font-size: 12px;
}
.col {
  display: inline-flex;
  flex-direction: column;
}
</style>
