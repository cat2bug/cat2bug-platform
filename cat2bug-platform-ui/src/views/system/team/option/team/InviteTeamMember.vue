<template>
  <el-dialog :title="$t('team.invite-members')" :visible.sync="dialogVisible" width="400px" append-to-body>
    <el-form ref="form" :model="form" :rules="rules" label-width="120px">
      <el-row>
        <el-col :span="24">
          <el-form-item :label="$t('member')">
            <el-select
              class="select-member"
              v-model="form.memberIds"
              multiple
              filterable
              remote
              reserve-keyword
              :placeholder="$t('team.search-placeholder')"
              :remote-method="searchMemberHandle"
              :loading="loading">
              <el-option
                v-for="item in memberOptions"
                :key="item.userId"
                :label="item.nickName"
                :value="item.userId">
                <el-avatar shape="circle" v-if="item.avatar" :src="item.avatar" fit="cover" size="small"></el-avatar>
                <el-avatar shape="circle" size="small" v-else>{{item.userName}}</el-avatar>
                <p>{{ item.nickName }}</p>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
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
      </el-row>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="submitForm">{{$t('submit')}}</el-button>
      <el-button @click="cancel">{{$t('cancel')}}</el-button>
    </div>
  </el-dialog>
</template>

<script>
import {
  addMember,
  addTeam,
  getMemberByTeam,
  inviteMember,
  listMember,
  listNotMember,
  updateTeam
} from "@/api/system/team";
import {getUser} from "@/api/system/user";

export default {
  name: "CreateTeamMember",
  data() {
    return {
      // 遮罩层
      loading: true,
      selectMemberList: [],
      memberOptions: [],
      // 是否显示弹出层
      dialogVisible: false,
      // 表单参数
      form: {
        teamId: this.getTeamId(),
        memberIds: [],
        roleIds: []
      },
      // 表单校验
      rules: {

      },
      // 角色选项
      roleOptions: [],
      initPassword: null,
    }
  },
  methods: {
    open(){
      getUser().then(res => {
        this.roleOptions = res.roles?res.roles.filter(r=>r.isTeamRole).map(r=>{
          r.roleName = r.roleNameI18nKey?this.$t(r.roleNameI18nKey):r.roleName;
          return r;
        }):[];
        this.dialogVisible = true;
      });
    },
    /** 获取团队id */
    getTeamId() {
      return this.$store.state.user.config.currentTeamId;
    },
    /** 获取团队成员列表 */
    searchMemberHandle(query) {
      this.loading = true;
      listNotMember(this.getTeamId(),{params:{search:query}}).then(res => {
        this.loading = false;
        this.memberOptions = res.rows;
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
          inviteMember(this.getTeamId(), this.form).then(res => {
            this.$modal.msgSuccess("新增成功");
            this.dialogVisible = false;
            this.cancel();
            this.$emit("invite",res);
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
        teamId: this.getTeamId(),
        memberIds: [],
        roleIds: []
      };
      this.memberOptions=[];
      this.resetForm("form");
    },
  }
}
</script>

<style lang="scss" scoped>
  .member-tools>* {
    margin-left: 10px;
  }
  .el-select-dropdown.is-multiple .el-select-dropdown__item {
      height: 50px;
      display: flex;
      justify-content: start;
      align-items: center;
      flex-direction: row;
      flex-wrap: nowrap;
    > * {
      margin-right: 5px;
    }
  }
</style>
