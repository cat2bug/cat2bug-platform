<template>
  <cat2-bug-card :style="{'--color': form.color, '--fontSize': form.fontSize}" :title="$i18n.t('defect.my-life').toString()" v-loading="loading" :tools="tools" @tools-click="toolsHandle">
    <template slot="content">
      <div class=" life-text" @click="openEditHandle">
        <div class="life-body">
          {{myLife}}
        </div>
      </div>
      <el-dialog
        :title="$t('defect.my-life')"
        :visible.sync="dialogVisible"
        width="30%"
        :before-close="handleClose">
        <el-alert
          class="title"
          :title="$t('defect.my-life.title')"
          type="success"
          :closable="false">
        </el-alert>
        <el-form ref="form" :model="form" label-width="120px">
          <el-form-item :label="$t('font-size')">
            <el-radio v-model="form.fontSize" label="45px" border size="medium">{{$t('font-size.medium')}}</el-radio>
            <el-radio v-model="form.fontSize" label="35px" border size="small">{{$t('font-size.small0')}}</el-radio>
            <el-radio v-model="form.fontSize" label="20px" border size="mini">{{$t('font-size.mini')}}</el-radio>
          </el-form-item>
          <el-form-item :label="$t('font-color')">
            <el-color-picker v-model="form.color"></el-color-picker>
          </el-form-item>
          <el-form-item :label="$t('defect.my-life.content')">
            <el-input type="textarea"
                      v-model="form.lifeContent"
                      :placeholder="$t('defect.my-life.input-content')"
                      class="input-content"
                      :autosize="{ minRows: 4, maxRows: 4}"
                      maxlength="32"
                      show-word-limit
            ></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onSubmit">{{$t('update')}}</el-button>
            <el-button @click="dialogVisible=false">{{$t('cancel')}}</el-button>
          </el-form-item>
        </el-form>
        </el-dialog>
    </template>
  </cat2-bug-card>
</template>

<script>
import Cat2BugCard from "../Components/Card"
import Cat2ButLabel from "../Components/Label"
import {updateConfig} from "@/api/system/user-config";
export default {
  name: "MyLife",
  components: {Cat2ButLabel,Cat2BugCard},
  data() {
    return {
      loading: false,
      lifeContent: 'defect.my-life.click-input-content',
      dialogVisible: false,
      form: {
        color: '#303133',
        fontSize: '35px',
        lifeContent: null
      }
    }
  },
  props: {
    params: {
      type: Object,
      default: ()=>{}
    },
    tools: {
      type: Array,
      default: ()=>[]
    },
    parent: {
      type: Object,
      default: ()=>{}
    },
    read: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    currentProjectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    myLife: function () {
      return this.form.lifeContent || this.$i18n.t(this.lifeContent);
    },
  },
  created() {
    if( this.$store.state.user.config.lifeContent) {
      this.form = JSON.parse(this.$store.state.user.config.lifeContent);
    }
  },
  mounted() {
  },
  methods: {
    toolsHandle(e,tool) {
      this.$emit('tools-click',tool);
    },
    handleClose() {
      this.dialogVisible = false;
    },
    onSubmit() {
      let param = {
        lifeContent: JSON.stringify(this.form)
      }
      updateConfig(param).then(res=>{
        this.lifeContent = this.form.lifeContent;
        this.$message.success(this.$i18n.t('update.success').toString());
        this.dialogVisible = false;
      });
    },
    openEditHandle() {
      if(this.read) return;
      this.dialogVisible=true;
    }
  }
}
</script>

<style lang="scss" scoped vars="{ fontSize, color }">
.title {
  margin: 0px 0px 20px 0px;
}
.life-text{
  flex: 1;
}
.life-body {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;

  max-width: 230px;
  min-width: 230px;
  overflow:hidden;
}
.life-body:hover {
  cursor: pointer;
}
.life-body, ::v-deep .input-content > textarea {
  color: var(--color);
  font-size: var(--fontSize);
}
</style>
