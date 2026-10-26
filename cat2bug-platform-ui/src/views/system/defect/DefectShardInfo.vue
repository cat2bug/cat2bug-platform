<template>
  <div class="shard-main">
    <div v-if="showPasswordGate" class="shard-pass-gate">
      <div class="shard-pass-gate__backdrop" aria-hidden="true" />
      <div class="shard-pass-card" role="dialog" aria-modal="true" :aria-label="$t('shard.password-gate-title')">
        <div class="shard-pass-card__brand">
          <img class="shard-pass-card__logo" :src="logoImg" alt="Cat2Bug" />
          <span class="shard-pass-card__brand-name">Cat2Bug</span>
        </div>
        <div class="shard-pass-card__lock" aria-hidden="true">
          <svg-icon icon-class="password" class="shard-pass-card__lock-icon" />
        </div>
        <h1 class="shard-pass-card__title">{{ $t('shard.password-gate-title') }}</h1>
        <p class="shard-pass-card__hint">{{ $t('shard.password-gate-hint') }}</p>
        <el-form
          ref="form"
          class="shard-pass-card__form"
          :model="params"
          label-width="0px"
          @submit.prevent="submitPassword"
        >
          <el-form-item :error="passError || undefined">
            <el-input
              ref="passwordInput"
              v-model="params.password"
              class="shard-pass-card__input"
              show-password
              :maxlength="4"
              :disabled="verifying"
              :placeholder="$t('shard.please-enter-password')"
              @keyup.enter="submitPassword"
              @input="onPasswordInput"
            >
              <template #prefix>
                <svg-icon icon-class="password" class="el-input__icon input-icon" />
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              class="shard-pass-card__submit"
              :loading="verifying"
              :disabled="!canSubmit"
              @click="submitPassword"
            >{{ $t('shard.verify') }}</el-button>
          </el-form-item>
        </el-form>
        <div class="shard-pass-card__foot">
          <lang-select class="lang-select" />
        </div>
      </div>
    </div>
    <div v-if="!defect.defectId" class="defect-info skeleton-main" :class="{ 'is-gated': showPasswordGate }">
      <div class="shard-header">
        <div class="shard-header__row"></div>
      </div>
      <div class="skeleton-parent">
        <el-skeleton :rows="6" animated />
        <div class="skeleton-images">
          <el-skeleton-item variant="image" style="width: 150px; height: 150px;" />
          <el-skeleton-item variant="image" style="width: 150px; height: 150px;" />
        </div>
        <el-skeleton :rows="6" animated />
      </div>
      <div class="shard-footer">
        <div>
          <el-image class="web-qrcode" :src="webQrcode" />
        </div>
        <div>
          <div class="row">
            <legacy-icon icon="el-icon-s-home"></legacy-icon> <span>{{$t('website')}}：</span><el-link
            href="https://www.cat2bug.com"
            target="_blank">https://www.cat2bug.com</el-link>
          </div>
          <div class="row">
            <legacy-icon icon="el-icon-user-solid"></legacy-icon> <span>{{$t('qq-group')}}：</span>
            <a target="_blank" href="https://qm.qq.com/cgi-bin/qm/qr?k=vpZaR_tWTwG0lPABP9NqgX2wSSSEEaWV&jump_from=webapi&authKey=8xIzBRe5T9RoEdisRBgYWbMmCoWXIYk4VikARN8S4RA3MZT7z7FUj71ZIIvEzGzq">731462000</a>
          </div>
          <div class="row">
            <svg-icon icon-class="gitee"/>
            <span>Gitee：</span><el-link
            href="https://gitee.com/cat2bug/cat2bug-platform"
            target="_blank">https://gitee.com/cat2bug/cat2bug-platform</el-link>
          </div>
          <div class="row">
            <svg-icon icon-class="github"/><span>GitHub：</span><el-link
            href="https://github.com/cat2bug/cat2bug-platform"
            target="_blank">https://github.com/cat2bug/cat2bug-platform</el-link>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="defect-info">
      <div class="shard-header">
        <div class="shard-header__row">
          <div class="shard-header__project">
            {{$t('project')}}: {{projectName || '—'}}
          </div>
          <lang-select class="lang-select" />
        </div>
      </div>
      <div class="defect-edit-header">
        <div class="defect-edit-title">
          <div style="line-height: 36px;">
            <span class="defect-edit-title-num">#{{defect.projectNum}}</span>
            <span class="defect-edit-title-content">
              <defect-type-flag :defect="defect" />
              <defect-state-flag :defect="defect" />
              {{defect.defectName}}
            </span>
          </div>
        </div>
      </div>
      <div class="app-container defect-edit-body">
        <div class="defect-edit-body-log-first">
          <list-defect-log
            ref="defectLogFirst"
            :pageSize="1"
            :fallback-describe="defect.defectDescribe || ''"
          />
        </div>
        <el-collapse v-model="activeNames">
          <el-collapse-item name="defectDescribe">
            <template #title>
              <span class="shard-collapse-title">{{ $i18n.t('describe') }}</span>
            </template>
            <div v-if="defect.defectDescribe" class="defect-describe-body markdown-rhythm">
              <markdown-content :content="defect.defectDescribe+''" />
            </div>
            <cat2-bug-empty v-else :description="$t('no-data')" />
          </el-collapse-item>
          <el-collapse-item name="customFields">
            <template #title>
              <span class="shard-collapse-title">{{ $i18n.t('defect.base-info') }}</span>
            </template>
            <defect-custom-fields-display
              ref="customFieldsDisplay"
              :project-id="defect.projectId"
              :defect="defect"
              :defect-case="defectCase"
              :custom-fields="defect.customFields"
              :preloaded-layout="columnLayout"
              @accordion-rows-change="onCustomFieldAccordionRowsChange"
            />
          </el-collapse-item>
          <el-collapse-item
            v-for="row in customFieldAccordionRows"
            :key="row.fieldKey"
            :name="customFieldAccordionCollapseName(row.fieldKey)"
          >
            <template #title>
              <span class="shard-collapse-title">{{ row.fieldLabel }}</span>
            </template>
            <defect-custom-field-accordion-body :row="row" />
          </el-collapse-item>
          <el-collapse-item v-if="defectCase && defectCase.caseId" name="caseId">
            <template #title>
              <span class="shard-collapse-title">{{ $i18n.t('case') }}</span>
            </template>
            <case-card :case-model="defectCase" :state-visible="true" :step-index="defect.caseStepId" :edit="false" />
          </el-collapse-item>
          <el-collapse-item name="log">
            <template #title>
              <span class="shard-collapse-title">{{ $i18n.t('log') }}</span>
            </template>
            <list-defect-log
              ref="defectLog"
              :pageSize="5"
              show-comment
              :fallback-describe="defect.defectDescribe || ''"
            />
          </el-collapse-item>
        </el-collapse>
      </div>
      <div class="shard-footer">
        <div>
          <el-image class="web-qrcode" :src="webQrcode" />
        </div>
        <div class="msg">
          <div class="row">
            <legacy-icon icon="el-icon-s-home"></legacy-icon> <span>{{$t('website')}}：</span><el-link
            href="https://www.cat2bug.com"
            target="_blank">https://www.cat2bug.com</el-link>
          </div>
          <div class="row">
            <legacy-icon icon="el-icon-user-solid"></legacy-icon> <span>{{$t('qq-group')}}：</span>
            <a target="_blank" href="https://qm.qq.com/cgi-bin/qm/qr?k=vpZaR_tWTwG0lPABP9NqgX2wSSSEEaWV&jump_from=webapi&authKey=8xIzBRe5T9RoEdisRBgYWbMmCoWXIYk4VikARN8S4RA3MZT7z7FUj71ZIIvEzGzq">731462000</a>
          </div>
          <div class="row">
            <svg-icon icon-class="gitee"/>
            <span>Gitee：</span><el-link
            href="https://gitee.com/cat2bug/cat2bug-platform"
            target="_blank">https://gitee.com/cat2bug/cat2bug-platform</el-link>
          </div>
          <div class="row">
            <svg-icon icon-class="github"/><span>GitHub：</span><el-link
            href="https://github.com/cat2bug/cat2bug-platform"
            target="_blank">https://github.com/cat2bug/cat2bug-platform</el-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {getShardDefect} from "@/api/system/DefectShard";
import ListDefectLog from "@/components/ListDefectLog";
import DefectTypeFlag from "@/components/Defect/DefectTypeFlag";
import DefectStateFlag from "@/components/Defect/DefectStateFlag";
import CaseCard from "@/components/Case/CaseCard";
import MarkdownContent from '@/components/Markdown/MarkdownContent.vue'
import LangSelect from "@/components/LangSelect";
import Cat2BugEmpty from '@/components/Cat2BugEmpty'
import {getCase} from "@/api/system/case";
import webQrcode from '@/assets/images/web_qrcode.png'
import logoImg from '@/assets/logo/logo.png'
import {
  accordionRowHasContent,
  customFieldAccordionCollapseName,
  isCustomFieldAccordionCollapseName
} from '@/components/DefectCustomField/defect-custom-field-accordion'

const I18N_LOCALE_KEY='i18n-locale'
const DEFECT_SHARD_PASSWORD_KEY = "defect-shard-password-"
export default {
  name: "DefectShardInfo",
  components: {
    ListDefectLog,
    DefectTypeFlag,
    DefectStateFlag,
    CaseCard,
    MarkdownContent,
    LangSelect,
    Cat2BugEmpty,
    DefectCustomFieldsDisplay: () => import('@/components/DefectCustomField/DefectCustomFieldsDisplay'),
    DefectCustomFieldAccordionBody: () => import('@/components/DefectCustomField/DefectCustomFieldAccordionBody')
  },
  data() {
    return {
      webQrcode,
      logoImg,
      defectShardId: null,
      params: {
        password: null
      },
      defect: {
        defectId: null,
      },
      projectName: '',
      columnLayout: null,
      activeNames: ['defectDescribe', 'customFields', 'log'],
      customFieldAccordionRows: [],
      defectCase: {},
      /** 是否展示密码门禁（需密码或校验失败） */
      needPassword: false,
      verifying: false,
      passError: '',
      /** 用户主动点验证后，才在卡片内展示错误文案 */
      passwordSubmitted: false,
    }
  },
  computed: {
    showPasswordGate() {
      return !this.defect.defectId && this.needPassword
    },
    canSubmit() {
      const pwd = this.params.password == null ? '' : String(this.params.password)
      return pwd.length === 4 && !this.verifying
    }
  },
  created() {
    this.defectShardId = this.$route.query.id;
    this.refreshLang();
    if(this.defectShardId) {
      let password = this.$cache.local.get(DEFECT_SHARD_PASSWORD_KEY+this.defectShardId);
      if(password) {
        this.params.password=password;
      }
    }
    // 无缓存密码时先亮门禁，避免探测完成前闪骨架
    if (!this.params.password) {
      this.needPassword = true
    }
    this.getShard({ probe: !this.params.password });
  },
  methods: {
    customFieldAccordionCollapseName,
    onCustomFieldAccordionRowsChange(rows) {
      const nextRows = rows || []
      this.customFieldAccordionRows = nextRows
      const validNames = new Set(
        nextRows.map((row) => customFieldAccordionCollapseName(row.fieldKey))
      )
      this.activeNames = this.activeNames.filter(
        (name) => !isCustomFieldAccordionCollapseName(name) || validNames.has(name)
      )
      nextRows.forEach((row) => {
        if (!accordionRowHasContent(row)) return
        const name = customFieldAccordionCollapseName(row.fieldKey)
        if (!this.activeNames.includes(name)) {
          this.activeNames.push(name)
        }
      })
    },
    refreshLang() {
      this.params.defaultLang = this.$cache.local.get(I18N_LOCALE_KEY) || this.params.defaultLang || 'zh_CN';
    },
    onPasswordInput() {
      this.passError = ''
    },
    submitPassword() {
      if (!this.canSubmit) return
      this.passwordSubmitted = true
      this.getShard({ probe: false })
    },
    /**
     * @param {{ probe?: boolean }} opts probe=true：首次探测，失败只亮门禁不弹全局 toast
     */
    getShard(opts = {}) {
      const probe = !!opts.probe
      let self = this;
      if (!this.defectShardId) {
        this.needPassword = true
        return
      }
      this.verifying = true
      this.passError = ''
      getShardDefect(0, this.defectShardId, this.params, { silentError: true }).then(res=>{
        if(res.data.redirect) {
          this.$router.replace({ path: '/project/defect', query: res.data.redirect })
          return;
        }
        this.needPassword = false
        this.passError = ''
        this.defect = res.data.defect || { defectId: null };
        this.projectName = res.data.projectName || '';
        this.columnLayout = res.data.columnLayout || null;
        this.activeNames = ['defectDescribe', 'customFields', 'log'];
        this.customFieldAccordionRows = [];
        if(this.defect && this.defect.defectId){
          if (this.defect.defectDescribe) {
            if (!this.activeNames.includes('defectDescribe')) {
              this.activeNames.push('defectDescribe');
            }
          }
          this.getCase(this.defect.caseId);
          if(res.data.logs && res.data.logs.length>0) {
            this.$nextTick(()=>{
              if (self.$refs.defectLogFirst) {
                self.$refs.defectLogFirst.refresh([res.data.logs[0]]);
              }
              if (self.$refs.defectLog) {
                self.$refs.defectLog.refresh(res.data.logs);
              }
            });
          }
        }
        if(this.params.password) {
          this.$cache.local.set(DEFECT_SHARD_PASSWORD_KEY+this.defectShardId,this.params.password);
        }
      }).catch((err) => {
        this.needPassword = true
        this.defect = { defectId: null }
        const msg = (err && (err.msg || err.message)) || ''
        // 探测阶段「请输入密码」不算错误；用户提交后才显示
        if (this.passwordSubmitted && this.params.password) {
          this.passError = msg || this.$t('shard.password-error')
        }
        this.$nextTick(() => {
          const input = this.$refs.passwordInput
          if (input && typeof input.focus === 'function') {
            input.focus()
          }
        })
        if (!probe && !this.passwordSubmitted && msg) {
          // 兜底：非探测失败仍可在卡片提示
          this.passError = msg
        }
      }).finally(() => {
        this.verifying = false
      });
    },
    getCase(caseId) {
      if(caseId) {
        getCase(caseId).then(res=>{
          this.defectCase = res.data || {};
          if (this.defectCase && this.defectCase.caseId && !this.activeNames.includes('caseId')) {
            this.activeNames.push('caseId');
          }
        }).catch(() => {
          this.defectCase = {};
        })
      } else {
        this.defectCase = {};
      }
    },
  }
}
</script>

<style lang="scss" scoped>
/* 针对桌面设备 */
@media screen and (min-width: 1024px) {
  .defect-info {
    padding: 0px 20%;
  }
  .row span{
    display: block;
  }
}

/* 针对手机设备 */
@media screen and (max-width: 1023px) {
  .defect-info {
    padding: 0px;
  }
  .lang-select {
    display: none;
  }
  .row span{
    display: none;
  }
}

.shard-main {
  margin: 0;
  padding: 0;
  width: 100%;
  min-height: 100%;
  background-color: var(--bg-color-page, #F2F6FC);
}
.defect-info {
  margin: 0;
  background-color: var(--bg-color-page, #F2F6FC);
  > * {
    background-color: var(--bg-color-block, #FFFFFF);
  }
}
.shard-header {
  background-color: #414141;
  color: #FFFFFF;
  padding: 10px 20px;
  font-size: 16px;
}
.shard-header__row {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.shard-header__project {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.shard-footer {
  width: 100%;
  background-color: #414141;
  color: #FFFFFF;
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  padding: 10px 10px;
  > div.msg {
    flex: 1;
    flex-direction: column;
    justify-content: center;
    align-items: flex-start;
  }
  .web-qrcode {
    width: 130px;
    height: 130px;
    margin-right: 10px;
  }
}
.defect-edit-header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-direction: row;
  flex-wrap: wrap;
  .defect-edit-title {
    display: inline-flex;
    justify-content: flex-start;
    align-items: center;
    flex-direction: row;
    overflow: hidden;
    padding: 10px 20px;
    .defect-edit-title-content {
      display: inline-flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 5px;
      font-size: 20px;
      color: var(--text-color-primary, #303133);
      font-weight: 500;
      line-height: 36px;
      vertical-align: middle;

      :deep(.defect-type-tag),
      :deep(.project-member-icons),
      :deep(.defect-state-tag),
      :deep(.el-tag--dark.el-tag--danger){
        float: none;
        margin: 0;
      }
    }
    .defect-edit-title-num {
      font-size: 20px;
      color: var(--text-color-primary, #303133);
      font-weight: 500;
      float: left;
      margin-right:10px;
    }
    > * {
      margin-right: 10px;
    }
  }
}
.defect-edit-body {
  padding-left: 30px;
  padding-right: 30px;
  background-color: var(--bg-color-block, #FFFFFF) !important;
  color: var(--text-color-regular, #606266);

  .defect-edit-body-log-first {
    margin-bottom: 20px;
  }

  :deep(.el-collapse) {
    border-top: 1px solid var(--border-color-light, #ebeef5);
    border-bottom: 1px solid var(--border-color-light, #ebeef5);
    border-left: none;
    border-right: none;
    background: transparent;

    .el-collapse-item__header {
      font-size: 16px;
      color: var(--text-color-primary, #303133);
      background-color: transparent;
      border-bottom: 1px solid var(--border-color-light, #ebeef5);
    }
    .el-collapse-item__wrap {
      background-color: transparent;
      border-bottom: 1px solid var(--border-color-light, #ebeef5);
    }
    .el-collapse-item__content {
      padding-top: 10px;
      padding-bottom: 10px;
      color: var(--text-color-regular, #606266);
      background-color: transparent;
    }
  }

  :deep(.defect-custom-fields-display) {
    label {
      color: var(--text-color-secondary, #909399);
    }
    span {
      color: var(--text-color-primary, #303133);
    }
  }
}
.row {
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 5px 0px;
  color: #FFFFFF;
  font-size: 15px;
  .el-link {
    color: #FFFFFF;
  }
  .el-link:hover {
    border-bottom: 1px solid #FFFFFF;
  }
  i, .svg-icon {
    margin-right: 5px;
  }
}
.shard-pass-gate {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  box-sizing: border-box;
}
.shard-pass-gate__backdrop {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 60% at 50% 40%, rgba(24, 144, 255, 0.12), transparent 70%),
    rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}
.shard-pass-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 380px;
  padding: 28px 28px 20px;
  border-radius: 12px;
  border: 1px solid var(--border-color-light, #ebeef5);
  background: var(--dialog-bg, #ffffff);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.28);
  box-sizing: border-box;
  color: var(--text-color-primary, #303133);
}
.shard-pass-card__brand {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}
.shard-pass-card__logo {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  object-fit: contain;
}
.shard-pass-card__brand-name {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--text-color-primary, #303133);
}
.shard-pass-card__lock {
  width: 52px;
  height: 52px;
  margin: 0 auto 14px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(24, 144, 255, 0.12);
  color: var(--cat2bug-primary, #1890ff);
}
.shard-pass-card__lock-icon {
  font-size: 22px;
}
.shard-pass-card__title {
  margin: 0 0 8px;
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--text-color-primary, #303133);
}
.shard-pass-card__hint {
  margin: 0 0 22px;
  text-align: center;
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-color-secondary, #909399);
}
.shard-pass-card__form {
  margin: 0;
  :deep(.el-form-item) {
    margin-bottom: 16px;
  }
  :deep(.el-form-item__error) {
    padding-top: 4px;
  }
}
.shard-pass-card__input {
  width: 100%;
  :deep(.el-input__wrapper) {
    min-height: 42px;
    padding-left: 12px;
    background-color: var(--input-bg, #fff);
    box-shadow: 0 0 0 1px var(--input-border, var(--border-color-base, #dcdfe6)) inset;
  }
  :deep(.el-input__inner) {
    letter-spacing: 0.28em;
    font-size: 16px;
    text-align: center;
  }
  :deep(.el-input__prefix) {
    color: var(--text-color-secondary, #909399);
  }
}
.shard-pass-card__submit {
  width: 100%;
  height: 42px;
  font-size: 15px;
  font-weight: 500;
}
.shard-pass-card__foot {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color-lighter, #f2f6fc);
}
.skeleton-main {
  display: inline-flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  &.is-gated {
    filter: blur(2px);
    pointer-events: none;
    user-select: none;
  }
}
.skeleton-parent {
  padding: 30px;
  flex: 1;
  > * {
    margin-bottom: 20px;
  }
  .skeleton-images {
    display: inline-flex;
    flex-direction: row;
    gap: 10px;
  }
}
.shard-header :deep(.lang) {
  .dropdown-title {
    color: #FFFFFF;
  }
}
.shard-pass-card__foot :deep(.lang) {
  .dropdown-title {
    color: var(--text-color-regular, #606266);
  }
}
.defect-describe-body {
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  box-sizing: border-box;
  font-size: 14px;
  line-height: 1.7;
  font-weight: 500;
  color: var(--text-color-primary, #303133);

  :deep(.markdown-it-vue),
  :deep(.md-body) {
    color: inherit;
    font-size: inherit;
    line-height: inherit;
  }
  :deep(ul),
  :deep(ol) {
    padding-left: 1.35em;
  }
}
</style>
