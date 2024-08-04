<template>
  <el-popover
    placement="bottom-start"
    width="350"
    v-model="popoverVisible"
    @show="popoverShowHandle"
    @hide="popoverHideHandle"
    trigger="click">
    <div slot="reference" :class="'el-input__inner select-project-member-input select-project-member-input-'+size">
      <i :class="icon" v-if="icon" style="margin: 0px 0px 0px 5px; color: #C0C4CC;"></i>
      <div class="selectProjectMemberInput_content">
        <el-tag class="select-project-member-tag" size="mini" v-for="member in selectMemberList()" :key="member.userId" closable @close="clickMenuHandle(member,$event)" :type="tagType(member)">{{member.nickName}}</el-tag>
        <el-input ref="selectProjectMemberInput" :size="size" :placeholder="selectMembers.size>0?'':$t(placeholder)" v-model="queryMember.params.search" @input="searchChangeHandle" @keydown.native="searchKeyDownHandle"></el-input>
      </div>
      <i class="select-project-member-input__icon el-icon-arrow-up" v-show="isClearButtonVisible==false" @mouseenter="showClearButtonHandle(true)"></i>
      <i class="select-project-member-input__icon el-icon-circle-close" v-show="isClearButtonVisible==true" @mouseleave="showClearButtonHandle(false)" @click="clearSelectMembersHandle"></i>
    </div>
    <el-tabs v-if="roleGroup" ref="tabs" class="select-project-member-tabs" v-model="queryMember.roleId" @tab-click="getMemberList">
      <el-tab-pane ref="tabAll" :label="$i18n.t('all')" name="0"></el-tab-pane>
      <el-tab-pane  v-for="(role,roleIndex) in roleList" :key="roleIndex" :label="role.roleNameI18nKey?$i18n.t(role.roleNameI18nKey):role.roleName" :name="role.roleId+''"></el-tab-pane>
    </el-tabs>

    <el-row v-if="options && options.length>0" class="select-project-member-menu">
      <el-col :span="24" v-for="(item,itemIndex) in options" :key="itemIndex" @click.native="clickMenuHandle(item,$event)" :active="optionsChecks.get(item.userId)?'true':'false'">
        <member-nameplate :member="item"></member-nameplate>
        <el-tag v-if="isHead" size="mini"  @click.native="setMasterHandle($event,item)" :type="tagType(item)">
          {{optionsChecks.get(item.userId)===1?$i18n.t('head'):$i18n.t('assistant')}}
        </el-tag>
        <i v-else class="el-icon-check"></i>
      </el-col>
    </el-row>
    <el-empty v-else :description="$i18n.t('no-data')"></el-empty>
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
import {listMemberOfProject, listProjectRole} from "@/api/system/project";
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
      // 查询的角色列表
      roleList: [],
      // 查询的成员列表
      options: [],
      // 列表中成员选择的优先级顺序
      optionsChecks: new Map(),
      // 当前成员id
      currentMemberId: null,
      // 当前选择的角色标签
      // 选择的成员
      selectMembers: new Map(),
      updateInt: 0,
      // 是否显示清除按钮
      isClearButtonVisible: false,
      // 是否显示成员下拉列表
      popoverVisible: false,
      // 成员查询参数
      queryMember: {
        pageNum: 1,
        pageSize: 10,
        roleId: null,
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
      type: Array,
      default: null,
    },
    isHead: {
      type: Boolean,
      default: true
    },
    projectId: {
      type: Number,
      default: null
    },
    roleId: {
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
      default: 'member.please-select-member'
    },
    clearable: {
      type: Boolean,
      default: true
    },
    size: {
      type: String,
      default: 'medium'
    },
    icon: {
      type: String,
      default: null
    }
  },
  computed: {
    tagType: function () {
      return function (member) {
        if(this.isHead) {
          return this.optionsChecks.get(member.userId)===1?'':'success';
        } else {
          return 'info';
        }
      }
    },
    selectMemberList: function () {
      return function () {
        const temp = this.updateInt;
        const arr = Array.from(this.selectMembers.values());
        return arr.sort((a, b) => this.optionsChecks.get(a.userId) - this.optionsChecks.get(b.userId));
      }
    }
  },
  watch: {
    memberId: function (newVal, oldVal) {
      if(newVal!=oldVal) {
        if (!newVal || newVal.length==0) {
          this.clear();
        } else if(this.selectMembers.size!=newVal.length || Array.from(this.selectMembers.keys()).filter(k=>newVal.includes(parseInt(k))).length!=newVal.length)  {
          this.clearSelectMembersHandle();
          this.getMemberList(newVal);
        }
      }
    }
  },
  created() {
    this.queryMember.roleId = this.roleId?this.roleId+'':'0';
    this.getRoleList();
    this.getMemberList();
  },
  mounted() {
  },
  methods: {
    clear() {
      this.clearSelectMembersHandle();
    },
    /** 搜索成员事件 */
    searchChangeHandle() {
      this.popoverVisible = true;
      this.getMemberList();
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
          this.optionsChecks.set(maxIndexId,0);
          this.selectMembers.delete(maxIndexId);
          this.updateMembers();
          this.$forceUpdate();
        }
      }
    },
    /** 查询角色集合 */
    getRoleList() {
      listProjectRole(this.projectId).then(res=>{
        this.roleList = res.rows;
      });
    },
    /** 查询成员集合 */
    getMemberList(memberIds) {
      this.queryMember.projectId = this.projectId;
      listMemberOfProject(this.projectId,this.queryMember).then(res=>{
        res.rows.forEach(m=>{
          if(!this.optionsChecks.get(m.userId)){
            this.optionsChecks.set(m.userId,0);
          }
        })
        this.options = res.rows;
        this.total = res.total;

        if(memberIds && memberIds.length>0) {
          this.options.forEach(m=> {
            if(memberIds.includes(m.userId)){
              this.clickMenuHandle(m);
            }
          });
        }
        this.$forceUpdate();
      });
    },
    /** 选择成员变化的处理 */
    selectMemberChangeHandle(id) {
      this.currentMemberId=id;
    },
    /** 弹窗显示事件 */
    popoverShowHandle() {
      // 设置打开时tab下面的横条宽度
      this.$nextTick(_ => {
        let index = 1;
        this.roleList.forEach((r,i)=>{
          if(this.queryMember.roleId==r.roleId) {
            index = i+1;
          }
        });
        // 获取第一个tab元素
        const tabFirstElement = this.$refs.tabs.$vnode.elm.childNodes[0].childNodes[0].childNodes[0].childNodes[0].childNodes[index];
        // 获取高亮显示条
        const activeBarElement = this.$refs.tabs.$vnode.elm.childNodes[0].childNodes[0].childNodes[0].childNodes[0].childNodes[0];
        // 第一个tab高亮
        if (tabFirstElement.classList.contains('is-active') && activeBarElement ) {
          // el-tab默认距离右边有20边距
          activeBarElement.style.width = tabFirstElement.clientWidth -16 + 'px'
        }
      });
      this.getMemberList();
      this.$refs.selectProjectMemberInput.focus();
    },
    /** 弹窗隐藏事件 */
    popoverHideHandle() {
      this.$refs.selectProjectMemberInput.blur();
      this.setMemberIdEvent();
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
      if(event){
        event.stopPropagation();
      }
    },
    /** 设置菜单索引顺序 */
    clickMenuHandle(member,event){
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
      if(event) {
        this.updateMembers();
        this.$forceUpdate();
      }
    },
    updateMembers(){
      this.setMemberIdEvent();
    },
    /** 显示或隐藏清除按钮 */
    showClearButtonHandle(visible) {
      if(this.clearable==false) return;
      if(visible && (this.selectMembers.size>0 || this.queryMember.params.search)) {
        this.isClearButtonVisible = true;
      } else {
        this.isClearButtonVisible = false;
      }
    },
    /** 清除选择的成员 */
    clearSelectMembersHandle(event) {
      this.optionsChecks.clear();
      this.selectMembers.clear();
      this.updateInt = (this.updateInt+1)%999;
      this.queryMember.roleId = this.roleId?this.roleId+'':'0';
      this.queryMember.params.search=null;
      this.queryMember.pageNum=1;
      this.popoverVisible = false;

      if(event) {
        this.updateMembers();
        this.getMemberList();
        this.$forceUpdate();
        event.stopPropagation();
      }
    },
    /** 翻页处理 */
    currentPageChangeHandle(val){
      this.queryMember.pageNum = val;
      this.getMemberList();
    },
    setMemberIdEvent() {
      let arr = Array.from(this.selectMembers.values());
      arr = arr.sort((a, b) => this.optionsChecks.get(a.userId) - this.optionsChecks.get(b.userId));
      let values = [];
      for(let i in arr){
        values.push(arr[i].userId);
      }
      this.$emit('input', values);
    }
  }
}
</script>
<style>
  .select-project-member-tabs .el-tabs__nav-prev, .select-project-member-tabs .el-tabs__nav-next {
    line-height: 27px;
  }
</style>
<style lang="scss" scoped>
  ::v-deep .select-project-member-input {
    display: inline-flex;
    flex-direction: row;
    max-width: 300px;
    width: 100%;
    height: auto;
    line-height: 0;
    align-items: center;
    padding-left: 5px;
    .selectProjectMemberInput_content {
      display: inline-flex;
      flex-direction: row;
      align-items: center;
      flex-wrap: wrap;
      flex-grow: 1;
      overflow: hidden;
      min-height: 30px;
      padding: 2px 10px 2px 0px;
      .select-project-member-tag {
        margin: 0px 3px;
        flex-shrink: 0;
      }
      .select-project-member-tag:first-child {
        margin-left: 10px;
      }
      .el-input {
        flex-grow: 1;
        width: 0.1%;
        height: 22px;
        .el-input__inner {
          padding-left: 8px;
          padding-right: 0px;
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
  ::v-deep .select-project-member-input-medium {
    .selectProjectMemberInput_content {
      min-height: 34px;
    }
  }

  ::v-deep .select-project-member-input-small {
    .selectProjectMemberInput_content {
      min-height: 24px;
    }
  }
  ::v-deep .select-project-member-input-mini {
    .selectProjectMemberInput_content {
      min-height: 20px;
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
      i {
        margin-right: 15px;
      }
      .el-tag {
        margin-right: 5px;
      }
      .el-tag, i {
        display: none;
      }
    }
    .el-col[active='true'] {
      .el-tag, i {
        display: inline-block;
      }
      ::v-deep .member-nameplate-content p, i {
        color: #409eff;
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
