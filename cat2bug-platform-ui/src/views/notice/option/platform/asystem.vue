<template>
  <el-form ref="form" :model="form" label-width="190px">
    <el-form-item :label="$t('notice.system.notice')" class="cat2bug-field-hint-exclude">
      <span class="notice-option-asystem-switch cat2bug-field-hint-host-inline">
        <el-switch v-model="form.switch" @change="handleSwitchChange"></el-switch>
      </span>
    </el-form-item>
    <el-form-item :label="$t('option')" class="cat2bug-field-hint-exclude">
      <div class="col notice-option-asystem-options">
        <div class="row">
          <span class="notice-option-asystem-bgm-cb cat2bug-field-hint-host-inline">
            <el-checkbox :label="$t('notice.system.bgm-enable')" v-model="form.backgroundMusic" @change="handleChange" :disabled="!form.switch"></el-checkbox>
          </span>
          <span class="notice-option-asystem-bgm-select cat2bug-field-hint-host-inline">
            <el-select v-model="form.backgroundMusicUrl" size="mini" placeholder="请选择" :disabled="!form.switch || !form.backgroundMusic">
              <el-option
                v-for="item in backgroundMusicOptions"
                :key="item.value"
                :label="$t(item.label)"
                :value="item.value">
              </el-option>
            </el-select>
          </span>
        </div>
        <div class="row">
          <span class="notice-option-asystem-panel-cb cat2bug-field-hint-host-inline">
            <el-checkbox :label="$t('notice.system.panel')" v-model="form.panel" @change="handleChange" :disabled="!form.switch"></el-checkbox>
          </span>
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
.notice-option-asystem-options {
  overflow: visible !important;
}
.notice-option-asystem-options .row {
  overflow: visible !important;
}
.cat2bug-field-hint-host-inline {
  position: relative;
  display: inline-flex;
  overflow: visible !important;
}
</style>
