<template>
  <el-dialog :title="$t('team.invite-members')" :visible.sync="dialogVisible" width="400px" append-to-body :close-on-click-modal="false" custom-class="cat2bug-form-shortcut-dialog team-invite-member-dialog" :close-on-press-escape="false" :before-close="onToolDialogBeforeClose" @opened="onToolDialogOpened">
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
                <cat2-bug-avatar :member="item" size="small" />
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
      <el-button class="defect-kbd-hint-host" type="primary" @click="submitForm">
        {{$t('submit')}}
        <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
      </el-button>
      <el-button @click="requestCloseToolDialog">{{$t('cancel')}}</el-button>
    </div>
  </el-dialog>
</template>

<script>
import {
  inviteMember,
  listNotMember, listTeamRole
} from "@/api/system/team";
import Cat2BugAvatar from "@/components/Cat2BugAvatar";
import defectToolDialogKbd from '@/mixins/defect-tool-dialog-kbd'

export default {
  name: "InviteTeamMember",
  mixins: [defectToolDialogKbd],
  components: { Cat2BugAvatar },
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
  computed: {
    /** 获取团队id */
    teamId() {
      return this.$store.state.user.config.currentTeamId;
    }
  },
  methods: {
    open(){
      this.reset();
      this.searchMemberHandle();
      listTeamRole(this.teamId).then(res => {
        this.roleOptions = res.data?res.data.filter(r=>r.isTeamRole && r.teamCreateBy==false).map(r=>{
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
            this.$modal.msgSuccess(this.$i18n.t('add-success'));
            this.doCloseToolDialog();
            this.$emit("invite",res);
          });
        }
      });
    },
    shortcutSave() {
      this.submitForm();
    },
    // 取消按钮
    cancel() {
      this.requestCloseToolDialog();
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
      justify-content:  flex-start;
      align-items: center;
      flex-direction: row;
      flex-wrap: nowrap;
    > * {
      margin-right: 5px;
    }
  }
</style>
