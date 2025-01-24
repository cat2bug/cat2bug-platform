<template>
  <div class="app-container">
    <el-page-header :title="$t('back')" @back="goBack" :content="$t('member.manage')">
    </el-page-header>
    <div class="member-tools">
      <!--用户数据-->
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="120px">
        <el-form-item label="" prop="userName">
          <el-input
            v-model="queryParams.userName"
            :placeholder="$t('member.please-enter-name')"
            prefix-icon="el-icon-user"
            clearable
            style="width: 240px"
            @input="handleQuery"
          />
        </el-form-item>
        <el-form-item label="" prop="phoneNumber">
          <el-input
            v-model="queryParams.phoneNumber"
            :placeholder="$t('member.please-enter-phone-number')"
            prefix-icon="el-icon-phone-outline"
            clearable
            style="width: 240px"
            @input="handleQuery"
          />
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
            v-hasPermi="['system:member:add']"
          >{{ $t('member.add') }}</el-button>
        </el-col>
      </el-row>
    </div>
    <el-row>
      <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column :label="$t('account')" align="center" key="userName" prop="userName" v-if="columns[0].visible" :show-overflow-tooltip="true" />
        <el-table-column :label="$t('name')" align="center" key="nickName" prop="nickName" v-if="columns[1].visible" :show-overflow-tooltip="true" />
        <el-table-column :label="$t('email')" align="center" key="email" prop="email" v-if="columns[2].visible"/>
        <el-table-column :label="$t('phone-number')" align="center" key="phoneNumber" prop="phoneNumber" v-if="columns[3].visible" width="120" />
        <el-table-column :label="$t('state')" align="center" key="status" v-if="columns[4].visible">
          <template slot-scope="scope">
            <el-switch
              v-model="scope.row.status"
              active-value="0"
              inactive-value="1"
              @change="handleStatusChange(scope.row)"
            ></el-switch>
          </template>
        </el-table-column>
        <el-table-column :label="$t('create-time')" align="center" prop="createTime" v-if="columns[5].visible" width="160">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('operate')"
          align="center"
          width="200"
          class-name="small-padding fixed-width"
        >
          <template slot-scope="scope" v-if="scope.row.userId !== 1">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['system:member:edit']"
            >{{ $t('modify') }}</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleResetPwd(scope.row)"
              v-hasPermi="['system:member:resetPwd']"
            >{{ $t('reset-password') }}</el-button>
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
    </el-row>

    <!-- 添加或修改用户配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item :label="$t('name')" prop="nickName">
              <el-input v-model="form.nickName" :placeholder="$t('member.please-enter-name')" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.userId == undefined" :label="$t('account')" prop="userName">
              <el-input v-model="form.userName" :placeholder="$t('member.please-enter-account')" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('phone-number')" prop="phoneNumber">
              <el-input v-model="form.phoneNumber" :placeholder="$t('member.please-enter-phone-number')" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('email')" prop="email">
              <el-input v-model="form.email" :placeholder="$t('member.please-enter-email')" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.userId == undefined" :label="$t('password')" prop="password">
              <el-input v-model="form.password" :placeholder="$t('member.please-enter-password')" type="password" maxlength="20" show-password/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('state')">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in dict.type.sys_normal_disable"
                  :key="dict.value"
                  :label="dict.value"
                >{{$t('sys.normal-disable-'+dict.value) || dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item :label="$t('remark')">
              <el-input v-model="form.remark" type="textarea" :placeholder="$t('enter-content')"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">{{  $t('ok') }}</el-button>
        <el-button @click="cancel">{{ $t('cancel') }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listUser, getUser, delUser, addUser, updateUser, resetUserPwd, changeUserStatus, deptTreeSelect } from "@/api/system/user";
import { getToken } from "@/utils/auth";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import {strFormat} from "@/utils";

export default {
  name: "User",
  dicts: ['sys_normal_disable', 'sys_user_sex'],
  components: { },
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
      // 用户表格数据
      userList: null,
      // 弹出层标题
      title: "",
      // 部门树选项
      deptOptions: undefined,
      // 是否显示弹出层
      open: false,
      // 部门名称
      deptName: undefined,
      // 默认密码
      initPassword: undefined,
      // 日期范围
      dateRange: [],
      // 岗位选项
      postOptions: [],
      // 角色选项
      roleOptions: [],
      // 表单参数
      form: {},
      defaultProps: {
        children: "children",
        label: "label"
      },
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        phoneNumber: undefined,
        status: undefined,
        deptId: undefined
      },
      // 列信息
      columns: [
        { key: 0, label: this.$i18n.t('account'), visible: true },
        { key: 1, label: this.$i18n.t('name'), visible: true },
        { key: 2, label: this.$i18n.t('email'), visible: true },
        { key: 2, label: this.$i18n.t('phone-number'), visible: true },
        { key: 3, label: `state`, visible: true },
        { key: 4, label: `create-time`, visible: true }
      ],
      // 表单校验
      rules: {
        userName: [
          { required: true, message: this.$t('member.account-cannot-empty'), trigger: "blur" },
          { min: 2, max: 20, message: this.$t('member.account-length-must-exception'), trigger: 'blur' }
        ],
        nickName: [
          { required: true, message: this.$t('member.name-cannot-empty'), trigger: "blur" }
        ],
        password: [
          { required: true, message: this.$t('member.password-cannot-empty'), trigger: "blur" },
          { min: 5, max: 20, message: this.$t('member.password-length-must-exception'), trigger: 'blur' }
        ],
        email: [
          { required: true, message: this.$t('member.email-cannot-empty'), trigger: "blur" },
          {
            type: "email",
            message: this.$t('member.email-format-exception'),
            trigger: ["blur", "change"]
          }
        ],
        phoneNumber: [
          { required: true, message: this.$t('member.phone-number-cannot-empty'), trigger: "blur" },
          {
            pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
            message: this.$t('member.phone-number-format-exception'),
            trigger: "blur"
          }
        ]
      },
    };
  },
  watch: {
    // 根据名称筛选部门树
    deptName(val) {
      this.$refs.tree.filter(val);
    }
  },
  created() {
    this.getList();
    this.getDeptTree();
    this.getConfigKey("sys.member.initPassword").then(response => {
      this.initPassword = response.msg;
    });
  },
  mounted() {
    this.initFloatMenu();
  },
  destroyed() {
    // 移除滚动条监听
    this.$floatMenu.windowsDestory();
  },
  methods: {
    /** 初始化浮动菜单 */
    initFloatMenu() {
      this.$floatMenu.windowsInit(document.querySelector('.main-container'));
      this.$floatMenu.resetMenus();
    },
    goBack() {
      this.$router.back();
    },
    /** 查询用户列表 */
    getList() {
      this.loading = true;
      listUser(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
          this.userList = response.rows;
          this.total = response.total;
          this.loading = false;
        }
      );
    },
    /** 查询部门下拉树结构 */
    getDeptTree() {
      deptTreeSelect().then(response => {
        this.deptOptions = response.data;
      });
    },
    // 筛选节点
    filterNode(value, data) {
      if (!value) return true;
      return data.label.indexOf(value) !== -1;
    },
    // 节点单击事件
    handleNodeClick(data) {
      this.queryParams.deptId = data.id;
      this.handleQuery();
    },
    // 用户状态修改
    handleStatusChange(row) {
      let text = row.status === "0" ? this.$i18n.t('enable') : this.$i18n.t('deactivate');
      this.$modal.confirm(strFormat(this.$i18n.t('member.change-user-state'),text + " " + row.userName)).then(function() {
        return changeUserStatus(row.userId, row.status);
      }).then(() => {
        this.$modal.msgSuccess(text + this.$i18n.t('char-span') + this.$i18n.t('success'));
      }).catch(function() {
        row.status = row.status === "0" ? "1" : "0";
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
        userId: undefined,
        deptId: undefined,
        userName: undefined,
        nickName: undefined,
        password: undefined,
        phoneNumber: undefined,
        email: undefined,
        sex: undefined,
        status: "0",
        remark: undefined,
        postIds: [],
        roleIds: []
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
      this.dateRange = [];
      this.resetForm("queryForm");
      this.queryParams.deptId = undefined;
      this.$refs.tree.setCurrentKey(null);
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.userId);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    // 更多操作触发
    handleCommand(command, row) {
      switch (command) {
        case "handleResetPwd":
          this.handleResetPwd(row);
          break;
        case "handleAuthRole":
          this.handleAuthRole(row);
          break;
        default:
          break;
      }
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      getUser().then(response => {
        this.postOptions = response.posts;
        this.roleOptions = response.roles;
        this.open = true;
        this.title = this.$i18n.t('member.add');
        this.form.password = this.initPassword;
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const userId = row.userId || this.ids;
      getUser(userId).then(response => {
        this.form = response.data;
        this.postOptions = response.posts;
        this.roleOptions = response.roles;
        this.$set(this.form, "postIds", response.postIds);
        this.$set(this.form, "roleIds", response.roleIds);
        this.open = true;
        this.title = this.$i18n.t('member.modify');
        this.form.password = "";
      });
    },
    /** 重置密码按钮操作 */
    handleResetPwd(row) {
      this.$prompt(strFormat(this.$i18n.t('member.reset-password'),row.userName), this.$i18n.t('prompted').toString(), {
        confirmButtonText: this.$i18n.t('ok'),
        cancelButtonText: this.$i18n.t('cancel'),
        closeOnClickModal: false,
        inputPattern: /^.{5,20}$/,
        inputErrorMessage: this.$i18n.t('member.password-length-must-exception')
      }).then(({ value }) => {
          resetUserPwd(row.userId, value).then(response => {
            this.$modal.msgSuccess(this.$i18n.t('modify-success'));
          });
        }).catch(() => {});
    },
    /** 分配角色操作 */
    handleAuthRole: function(row) {
      const userId = row.userId;
      this.$router.push("/system/member-auth/role/" + userId);
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.userId != undefined) {
            updateUser(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('modify-success'));
              this.open = false;
              this.getList();
            });
          } else {
            addUser(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('add-success'));
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const userIds = row.userId || this.ids;
      this.$modal.confirm(strFormat(this.$i18n.t('member.delete-member'),userIds)).then(function() {
        return delUser(userIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess(this.$i18n.t('delete-success'));
      }).catch(() => {});
    },
  }
};
</script>
<style lang="scss" scoped>
.member-tools {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  margin-bottom: 10px;
  > * {
    display: inline-block;
    justify-content: flex-start;
    margin-bottom: 0px;
    ::v-deep .el-form-item {
      margin-bottom: 0px;
    }
  }
}
</style>
