<template>
  <el-dialog :title="$t('member.create')" :visible.sync="dialogVisible" width="800px" append-to-body>
    <el-form ref="form" :model="form" :rules="rules" label-width="120px">
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
          <el-form-item v-if="form.userId == undefined" :label="$t('password')" prop="password">
            <el-input v-model="form.password" :placeholder="$t('member.please-enter-password')" type="password" maxlength="20" show-password/>
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
<!--        <el-col :span="12">-->
<!--          <el-form-item label="状态">-->
<!--            <el-radio-group v-model="form.status">-->
<!--              <el-radio-->
<!--                v-for="dict in dict.type.sys_normal_disable"-->
<!--                :key="dict.value"-->
<!--                :label="dict.value"-->
<!--              >{{dict.label}}</el-radio>-->
<!--            </el-radio-group>-->
<!--          </el-form-item>-->
<!--        </el-col>-->
<!--        <el-col :span="12">-->
<!--          <el-form-item label="岗位">-->
<!--            <el-select v-model="form.postIds" multiple placeholder="请选择岗位">-->
<!--              <el-option-->
<!--                v-for="item in postOptions"-->
<!--                :key="item.postId"-->
<!--                :label="item.postName"-->
<!--                :value="item.postId"-->
<!--                :disabled="item.status == 1"-->
<!--              ></el-option>-->
<!--            </el-select>-->
<!--          </el-form-item>-->
<!--        </el-col>-->
        <el-col :span="12">
          <el-form-item :label="$t('role')">
            <el-select v-model="form.roleIds" multiple :placeholder="$t('member.please-select-role')">
              <el-option
                v-for="item in roleOptions"
                :key="item.roleId"
                :label="item.roleName"
                :value="item.roleId"
                :disabled="item.status == 1"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('remark')">
            <el-input v-model="form.remark"
                      type="textarea"
                      :placeholder="$t('enter-content')"
                      maxlength="500"
                      show-word-limit
                      rows="5"
            ></el-input>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="submitForm">{{$t('submit')}}</el-button>
      <el-button @click="cancel">{{$t('cancel')}}</el-button>
    </div>
  </el-dialog>
</template>

<script>
import {addMember, addTeam, getMemberByTeam, updateTeam} from "@/api/system/team";
import {getUser} from "@/api/system/user";

export default {
  name: "CreateTeamMember",
  dicts: ['sys_normal_disable', 'sys_user_sex'],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 是否显示弹出层
      dialogVisible: false,
      // 表单参数
      form: {},
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
      // 角色选项
      roleOptions: [],
      initPassword: null,
    }
  },
  created() {
    this.getConfigKey("sys.member.initPassword").then(response => {
      this.initPassword = response.msg;
    });
  },
  methods: {
    open(){
      getUser().then(res => {
        // this.postOptions = response.posts;
        this.roleOptions = res.roles?res.roles.filter(r=>r.isTeamRole).map(r=>{
          r.roleName = r.roleNameI18nKey?this.$t(r.roleNameI18nKey):r.roleName;
          return r;
        }):[];
        this.dialogVisible = true;
        this.form.password = this.initPassword;
      });
    },
    /** 返回 */
    goBack() {
      this.$router.back();
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          addMember(this.$store.state.user.config.currentTeamId, this.form).then(res => {
            this.$modal.msgSuccess("新增成功");
            this.dialogVisible = false;
            this.cancel();
            this.$emit("create",res);
          });
        }
      });
    },
    // 取消按钮
    cancel() {
      this.dialogVisible = false;
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
  }
}
</script>

<style lang="scss" scoped>
  .member-tools>* {
    margin-left: 10px;
  }
</style>
