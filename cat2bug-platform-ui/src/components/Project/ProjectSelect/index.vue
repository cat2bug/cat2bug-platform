<template>
  <el-popover
    placement="bottom-start"
    v-model="popoverVisible"
    @show="popoverShowHandle"
    @hide="popoverHideHandle"
    trigger="hover">
    <div slot="reference" :class="'between-row-1 click select-project-member-input-'+size">
      <i class="yellow el-icon-star-on start-switch" :collect="currentProject.collect?'true':'false'" @click="handleProjectCollect(currentProject)"></i>
<!--      <el-image :src="currentProject.projectIcon" />-->
      <p class="prefix-project-name">{{currentProject.projectName}}</p>
      <i class="el-icon-arrow-down" />
    </div>
    <h3 class="title"><i class="el-icon-refresh"/> {{ $t('project.select') }}</h3>
    <el-divider></el-divider>
    <div v-for="(item,index) in options" :key="index" class="col click item" @click="handleProjectChange(item)">
      <div class="row">
        <i class="yellow el-icon-star-on start-switch" :collect="item.collect?'true':'false'" @click="handleProjectCollect(item, $event)"></i>
<!--        <el-image :src="item.projectIcon" />-->
        <p class="prefix-project-name">{{item.projectName}}</p>
      </div>
    </div>
    <el-divider v-if="total>queryParams.pageSize"></el-divider>
    <el-pagination
      style="float: right;"
      small
      :hide-on-single-page="true"
      layout="prev, next"
      :current-page.sync="queryParams.pageNum"
      :page-size.sync="queryParams.pageSize"
      :total="total"
      @current-change="getProjectList">
    </el-pagination>
  </el-popover>
</template>

<script>
import {collectProject, getProject, listProject} from "@/api/system/project";
import store from "@/store";
import Label from "@/components/Cat2BugStatistic/Components/Label";

export default {
  name: "ProjectSelect",
  components: {Label},
  data() {
    return {
      popoverVisible: false,
      options: [],
      currentProject: {},
      total: 0,
      queryParams: {
        teamId: this.teamId,
        params: {
          userId: this.memberId,
        },
        pageNum: 1,
        pageSize: 10
      }
    }
  },
  props: {
    teamId : {
      type: Number,
      default: 0
    },
    projectId : {
      type: Number,
      default: 0
    },
    memberId : {
      type: Number,
      default: 0
    },
    size: {
      type: String,
      default: 'default'
    },
  },
  created() {
  },
  mounted() {
    this.getCurrentProjectInfo();
    this.getProjectList();
  },
  methods: {
    /** 弹窗显示事件 */
    popoverShowHandle() {
    },
    /** 弹窗隐藏事件 */
    popoverHideHandle() {

    },
    /** 获取当前项目信息 */
    getCurrentProjectInfo() {
      getProject(this.projectId).then(res=>{
        this.currentProject = res.data;
      })
    },
    /** 查询收藏的项目列表 */
    getProjectList() {
      listProject(this.queryParams).then(res => {
        this.options = res.rows;
        this.total = res.total;
      });
    },
    /** 选择项目 */
    handleProjectChange(project) {
      store.dispatch('UpdateCurrentProjectId', project.projectId).then(() => {
        store.dispatch('GetInfo').then(() => {
          // 设置缺陷列表显示哪些列属性
          window.location.reload();
        });
      });
    },
    /** 设置当前项目是否收藏 */
    handleProjectCollect(project, event) {
      this.$set(project, 'collect', !project.collect);
      // 保存收藏状态
      collectProject(project.projectId, project).then(res=>{
        if(project.projectId == this.currentProject.projectId) {
          this.$set(this.currentProject, 'collect', project.collect);
        }
        if(project.collect) {
          this.$message.success(this.$i18n.t('collect-success').toString());
        } else {
          this.$message.success(this.$i18n.t('cancel-success').toString());
        }
        // 重新获取项目列表
        this.getProjectList();
      });
      if(event) {
        event.stopPropagation();
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.el-divider {
  background-color: #EBEEF5;
  margin: 10px 0px;
}
.between-row-1 {
  display: inline-flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  gap: 5px;
  .prefix-project-name {
    flex: 1;
  }
  .el-icon-arrow-down {
    color: #909399;
  }
}
.prefix-project-name {
  margin: 5px;
}
.row {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  gap: 5px;
}
.col {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}
.yellow {
  color: #f7ba2a;
}
.block {
  display: inline-block;
}
.click {
  cursor: pointer;
  padding: 2px 10px;
  border-radius: 3px;
}
.click:hover {
  background-color: #e8f4ff;
}
.item {
  font-size: 1rem;
}
.start-switch[collect="true"] {
  color: rgb(247, 186, 42);
  -webkit-text-stroke:3px transparent;
  background: rgb(247, 186, 42) top left / 100% 100%;
  -webkit-background-clip: text;
}
.start-switch {
  color: #FFFFFF;
  -webkit-text-stroke:3px transparent;
  background: rgb(198, 209, 222) top left / 100% 100%;
  -webkit-background-clip: text;
}
.title {
  margin: 5px 5px 15px 10px;
}
.el-image {
  width: 30px;
  height: 30px;
}
</style>
