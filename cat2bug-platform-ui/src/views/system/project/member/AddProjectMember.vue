<template>
  <el-dialog
    :title="$t('project.add-member')"
    :visible.sync="dialogVisible"
    width="400px"
    append-to-body
    :close-on-click-modal="false"
    custom-class="cat2bug-form-shortcut-dialog project-add-member-dialog"
    :close-on-press-escape="false"
    :before-close="onToolDialogBeforeClose"
    @opened="onToolDialogOpened"
  >
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
              :placeholder="$t('member.search-placeholder')"
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
import {addProjectMembers, listNotMemberOfProject, listProjectRole} from "@/api/system/project";
import Cat2BugAvatar from "@/components/Cat2BugAvatar";
import defectToolDialogKbd from '@/mixins/defect-tool-dialog-kbd'

export default {
  name: "AddProjectMember",
  mixins: [defectToolDialogKbd],
  components: { Cat2BugAvatar },
  data() {
    return {
      loading: true,
      selectMemberList: [],
      memberOptions: [],
      dialogVisible: false,
      form: {
        projectId: this.getProjectId(),
        memberIds: [],
        roleIds: []
      },
      rules: {},
      roleOptions: [],
      initPassword: null,
    }
  },
  methods: {
    open(){
      this.getRoleList();
    },
    getRoleList() {
      listProjectRole(0).then(res => {
        this.roleOptions = res.rows?res.rows.filter(r=>r.projectCreateBy==false).map(r=>{
          r.roleName = r.roleNameI18nKey?this.$t(r.roleNameI18nKey):r.roleName;
          return r;
        }):[];
        this.searchMemberHandle();
        this.dialogVisible = true;
      });
    },
    getProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    searchMemberHandle(query) {
      this.loading = true;
      listNotMemberOfProject(this.getProjectId(),{params:{search:query}}).then(res => {
        this.loading = false;
        this.memberOptions = res.rows;
      });
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          addProjectMembers(this.getProjectId(), this.form).then(res => {
            this.$modal.msgSuccess(this.$i18n.t('add-success'));
            this.doCloseToolDialog();
            this.$emit("invite", res);
          });
        }
      });
    },
    shortcutSave() {
      this.submitForm();
    },
    cancel() {
      this.requestCloseToolDialog();
    },
    reset() {
      this.form = {
        projectId: this.getProjectId(),
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
