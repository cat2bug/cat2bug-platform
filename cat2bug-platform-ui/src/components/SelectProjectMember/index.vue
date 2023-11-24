<template>
  <el-popover
    placement="bottom"
    width="350"
    v-model="popoverVisible"
    @show="popoverShowHandle"
    @hide="popoverHideHandle"
    trigger="click">
    <div slot="reference" class="select-project-member-input el-input__inner">
      <div class="selectProjectMemberInput_content">
        <el-tag class="select-project-member-tag" size="mini" v-for="member in selectMemberList()" :key="member.userId" closable @close="clickMenuHandle(member)" :type="tagType(member)">{{member.nickName}}</el-tag>
        <el-input ref="selectProjectMemberInput" :placeholder="selectMembers.size>0?'':placeholder" v-model="queryMember.params.search" @input="searchChangeHandle" @keydown.native="searchKeyDownHandle"></el-input>
      </div>
      <i class="select-project-member-input__icon el-icon-arrow-up" v-show="isClearButtonVisible==false" @mouseenter="showClearButtonHandle(true)"></i>
      <i class="select-project-member-input__icon el-icon-circle-close" v-show="isClearButtonVisible==true" @mouseleave="showClearButtonHandle(false)" @click="clearSelectMembersHandle"></i>
    </div>
    <el-tabs v-if="roleGroup" class="select-project-member-tabs" v-model="activeRoleTabName" @tab-click="selectRoleTabHandle">
      <el-tab-pane :label="$i18n.t('all')" name=""></el-tab-pane>
      <el-tab-pane :label="$i18n.t('project.admin')" :name="$t('project.admin')"></el-tab-pane>
      <el-tab-pane :label="$i18n.t('project.develop')" :name="$t('project.develop')"></el-tab-pane>
      <el-tab-pane :label="$i18n.t('project.tester')" :name="$t('project.tester')"></el-tab-pane>
      <el-tab-pane :label="$i18n.t('project.outsider')" :name="$t('project.outsider')"></el-tab-pane>
    </el-tabs>
    <el-row class="select-project-member-menu">
      <el-col :span="24" v-for="item in options" :key="item.userId" @click.native="clickMenuHandle(item)">
        <member-nameplate :member="item"></member-nameplate>
        <el-tag size="mini" :active="optionsChecks.get(item.userId)?'true':'false'" @click.native="setMasterHandle($event,item)" :type="tagType(item)">
          {{optionsChecks.get(item.userId)===1?$i18n.t('head'):$i18n.t('assistant')}}
        </el-tag>
      </el-col>
    </el-row>
    <el-pagination
      small
      :hide-on-single-page="true"
      layout="prev, next"
      :current-page.sync="queryMember.pageNum"
      :page-size.sync="queryMember.pageSize"
      @current-change="currentPageChangeHandle"
      :total="total">
    </el-pagination>
  </el-popover>
</template>

<script>
import i18n from "@/utils/i18n/i18n";
import {listMemberOfProject} from "@/api/system/project";
import MemberNameplate from "@/components/MemberNameplate"
export default {
  name: "SelectProjectMember",
  model: {
    prop: 'memberId',
    event: 'input'
  },
  components: { MemberNameplate },
  data() {
    return {
      // 查询的成员列表
      options: [],
      // 列表中成员选择的优先级顺序
      optionsChecks: new Map(),
      // 当前成员id
      currentMemberId: null,
      // 当前选择的角色标签
      activeRoleTabName: '',
      // 选择的成员
      selectMembers: new Map(),
      // 是否显示清除按钮
      isClearButtonVisible: false,
      // 是否显示成员下拉列表
      popoverVisible: false,
      // 成员查询参数
      queryMember: {
        pageNum: 1,
        pageSize: 10,
        params: {
          search: null
        }
      },
      // 成员总数
      total: 0
    }
  },
  props: {
    memberId: {
      type: Number,
      default: null
    },
    projectId: {
      type: Number,
      default: null
    },
    multiple: {
      type: Boolean,
      default: true
    },
    roleGroup: {
      type: Boolean,
      default: true
    },
    placeholder: {
      type: String,
      default: i18n.t('member.please-select-member')
    },
    clearable: {
      type: Boolean,
      default: true
    },
  },
  computed: {
    tagType: function () {
      return function (member) {
        return this.optionsChecks.get(member.userId)===1?'':'success';
      }
    },
    selectMemberList: function () {
      return function () {
        const arr = Array.from(this.selectMembers.values());
        return arr.sort((a, b) => this.optionsChecks.get(a.userId) - this.optionsChecks.get(b.userId));
      }
    }
  },
  created() {
    this.getMember();
  },
  methods: {
    searchChangeHandle() {
      this.popoverVisible = true;
      this.getMember();
    },
    /** 输入字符事件 */
    searchKeyDownHandle(e) {
      // 如果按了退格键，并且input中没有字符了，就删除最有一个选中的成员
      if(e.keyCode==8 && !this.queryMember.params.search) {
        let maxIndexId = null;
        let maxIndexIndex = 0;
        for (let [key, value] of this.optionsChecks) {
          if(maxIndexIndex<value) {
            maxIndexIndex=value;
            maxIndexId=key;
          }
        }
        if(maxIndexId){
          this.selectMembers.delete(maxIndexId);
          this.optionsChecks.set(maxIndexId,0);
          this.$forceUpdate();
        }
      }
    },
    getMember() {
      this.queryMember.projectId = this.projectId;
      listMemberOfProject(this.projectId,this.queryMember).then(res=>{
        res.rows.forEach(m=>{
          if(!this.optionsChecks.get(m.userId)){
            this.optionsChecks.set(m.userId,0);
          }
        })
        this.options = res.rows;
        this.total = res.total;
      });
    },
    selectMemberChangeHandle(id){
      this.currentMemberId=id;
    },
    popoverShowHandle() {
      this.$refs.selectProjectMemberInput.focus();
    },
    popoverHideHandle() {
      this.$refs.selectProjectMemberInput.blur();
    },
    /** 设置主负责人 */
    setMasterHandle(event,member) {
      for (let [key, value] of this.optionsChecks) {
        if(value>0 && value<this.optionsChecks.get(member.userId)) {
          this.optionsChecks.set(key,value+1);
        }
      }
      this.optionsChecks.set(member.userId,1);
      this.$forceUpdate();
      event.stopPropagation();
    },
    /** 设置菜单索引顺序 */
    clickMenuHandle(member){
      if(this.optionsChecks.get(member.userId)){
        for (let [key, value] of this.optionsChecks) {
          if(value>this.optionsChecks.get(member.userId)) {
            this.optionsChecks.set(key,value-1);
          }
        }
        this.optionsChecks.set(member.userId,0);
        this.selectMembers.delete(member.userId);
      } else {
        let count = 0;
        for (let [key, value] of this.optionsChecks) {
          if(value>0) {
            count++;
          }
        }
        this.optionsChecks.set(member.userId,count+1);
        this.selectMembers.set(member.userId,member);
      }
      this.$forceUpdate();
    },
    /** 选择角色 */
    selectRoleTabHandle() {

    },
    /** 显示或隐藏清除按钮 */
    showClearButtonHandle(visible) {
      if(this.clearable==false) return;
      if(visible && this.selectMembers.size>0) {
        this.isClearButtonVisible = true;
      } else {
        this.isClearButtonVisible = false;
      }
    },
    /** 清除选择的成员 */
    clearSelectMembersHandle(event) {
      this.optionsChecks.clear();
      this.selectMembers.clear();
      this.$forceUpdate();
      event.stopPropagation();
    },
    /** 翻页处理 */
    currentPageChangeHandle(val){
      this.queryMember.pageNum = val;
      this.getMember();
    }
  }
}
</script>

<style lang="scss" scoped>
  ::v-deep .select-project-member-input {
    display: inline-flex;
    flex-direction: row;
    width: 300px;
    height: auto;
    line-height: 0;
    align-items: center;
    .selectProjectMemberInput_content {
      display: inline-flex;
      flex-direction: row;
      align-items: center;
      flex-wrap: wrap;
      flex-grow: 1;
      overflow: hidden;
      min-height: 28px;
      margin: 3px 10px 3px 0px;
      .select-project-member-tag {
        margin: 3px;
        flex-shrink: 0;
      }
      .el-input {
        flex-grow: 1;
        width: 0.1%;
        height: 22px;
        .el-input__inner {
          border-width: 0px;
          height: 22px;
          line-height: 22px;
          display: inline;
        }
      }
    }
    .select-project-member-input__icon {
      display: inline;
      color: #C0C4CC;
      font-size: 14px;
      transition: transform 0.3s, -webkit-transform 0.3s;
      -webkit-transform: rotateZ(180deg);
      transform: rotateZ(180deg);
      cursor: pointer;
    }
  }
  .select-project-member-input:focus {
    .select-project-member-input__icon {
      transform: rotateZ(0deg);
    }
  }
  .select-project-member-menu {
    .el-col {
      display: inline-flex;
      flex-shrink: 0;
      justify-content: space-between;
      align-items: center;
      .el-tag {
        display: none;
        margin-right: 5px;
      }
      .el-tag[active='true'] {
        display: inline-block;
      }
    }
    .el-col:hover {
      background-color: #F2F6FC;
      border-radius: 5px;
      cursor: pointer;
    }
  }
  ::v-deep .el-tabs__item {
    height: 27px !important;
    line-height: 27px !important;
    font-size: 12px;
    padding: 0 8px !important;
  }
  ::v-deep .el-tabs__nav-next, .el-tabs__nav-prev {
    line-height: 30px;
  }
  .el-pagination {
    float: right;
  }
</style>
