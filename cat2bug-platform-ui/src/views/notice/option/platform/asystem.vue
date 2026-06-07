<template>
  <el-form ref="form" :model="form" label-width="190px">
    <el-form-item :label="$t('notice.system.notice')">
      <span class="notice-option-asystem-switch cat2bug-field-hint-host-inline">
        <el-switch v-model="form.switch" @change="handleSwitchChange"></el-switch>
      </span>
    </el-form-item>
    <el-form-item :label="$t('option')">
      <div class="col">
        <div class="row">
          <span class="notice-option-asystem-bgm cat2bug-field-hint-host-inline">
            <el-checkbox :label="$t('notice.system.bgm-enable')" v-model="form.backgroundMusic" @change="handleChange" :disabled="!form.switch"></el-checkbox>
          </span>
          <el-select v-model="form.backgroundMusicUrl" size="mini" placeholder="请选择" :disabled="!form.switch || !form.backgroundMusic">
            <el-option
              v-for="item in backgroundMusicOptions"
              :key="item.value"
              :label="$t(item.label)"
              :value="item.value">
            </el-option>
          </el-select>
        </div>
        <div class="row">
          <el-checkbox :label="$t('notice.system.panel')" v-model="form.panel" @change="handleChange" :disabled="!form.switch"></el-checkbox>
        </div>
      </div>
    </el-form-item>
  </el-form>
</template>

<script>
export default {
  name: "SystemNoticePlatform",
  model: {
    prop: 'system',
    event: 'change'
  },
  data() {
    return {
      form: this.system,
      backgroundMusicOptions: [{
        value: 'default.mp3',
        label: 'notice.sound.default'
      }, {
        value: 'diandingdong.mp3',
        label: 'notice.sound.electric'
      }, {
        value: 'junhao.mp3',
        label: 'notice.sound.bugle'
      }, {
        value: 'huangshang.mp3',
        label: 'notice.sound.emperor'
      }, {
        value: 'shangxian.mp3',
        label: 'notice.sound.immortal'
      }, {
        value: 'shifu.mp3',
        label: 'notice.sound.teacher'
      }],
    }
  },
  props: {
    system: {
      type: Object,
      default: ()=>{
        return {}
      }
    }
  },
  watch: {
    system: function (n,o) {
      if(n && n!=o) {
        this.form = n;
      }
    },
  },
  methods: {
    /** 操作改变 */
    handleChange() {
      this.$emit('change', this.form);
    },
    /** 处理系统开关改变的操作 */
    handleSwitchChange() {
      this.$refs['form'].clearValidate();
      this.handleChange();
    },
    /** 重制表单 */
    resetFields() {
      this.$refs['form'].resetFields();
    },
    /** 验证表单 */
    validate() {
      const validatePromises = new Promise((resolve, reject) => {
        this.$refs['form'].validate((valid) => {
          if (valid) {
            resolve()
          } else {
            reject()
          }
        })
      });
      if (validatePromises) {
        return Promise.all([validatePromises])
          .then(res => {
            return true;
          }).catch(() => {
            return false;
          });
      } else {
        return false;
      }
    }
  }
}
</script>

<style scoped>
.row {
  display: inline-flex;
  flex-direction: row;
  gap: 10px;
}
.col {
  display: inline-flex;
  flex-direction: column;
}
.cat2bug-field-hint-host-inline {
  position: relative;
  display: inline-flex;
  overflow: visible !important;
}
</style>
