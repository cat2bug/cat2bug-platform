<template>
  <div
    class="setup-page"
    :class="{ 'setup-page--rtl': isRtl }"
    :dir="isRtl ? 'rtl' : 'ltr'"
  >
    <div class="setup-page-watermark" aria-hidden="true">
      <div class="setup-page-watermark__pattern" />
    </div>
    <div class="setup-layout">
      <header class="setup-hero">
        <div class="setup-hero-bg" aria-hidden="true" />
        <div class="setup-hero-head">
          <h1>{{ $t('setup.title') }}</h1>
          <p class="setup-intro-lead">{{ $t('setup.introduce1') }}</p>
        </div>
        <div class="setup-hero-art" aria-hidden="true">
          <img
            class="setup-hero-art-img"
            :src="brandLogoSrc"
            alt=""
          >
        </div>
      </header>

      <div class="setup-body">
      <aside class="setup-aside">
        <div class="setup-aside-content">
          <h2 class="setup-roadmap-title">{{ $t('setup.roadmapTitle') }}</h2>
          <ol class="setup-roadmap">
            <li
              v-for="(key, index) in stepKeys"
              :key="key"
              class="setup-roadmap-item"
              :class="{
                'is-active': !finished && activeStep === index,
                'is-done': finished || activeStep > index
              }"
            >
              <div class="setup-roadmap-marker-col">
                <span class="setup-roadmap-marker">
                  <i v-if="finished || activeStep > index" class="el-icon-check" />
                  <span v-else>{{ index + 1 }}</span>
                </span>
                <span
                  v-if="index < stepKeys.length - 1"
                  class="setup-roadmap-connector"
                  aria-hidden="true"
                />
              </div>
              <span class="setup-roadmap-text">{{ $t(key) }}</span>
            </li>
          </ol>
        </div>
        <p v-if="!finished" class="setup-intro-note">{{ $t('setup.introduce3') }}</p>
      </aside>

      <main class="setup-main">
        <div class="setup-panel">
          <template v-if="finished">
            <div class="setup-success setup-panel-fill">
              <i class="el-icon-success setup-success-icon" />
              <h3>{{ $t('setup.success.title') }}</h3>
              <p>{{ $t('setup.success.message') }}</p>
              <p class="setup-tip setup-tip--warn" v-if="restartRequired">{{ restartHintText }}</p>
              <p v-if="installWarning" class="setup-tip setup-tip--warn">{{ installWarning }}</p>
              <p class="setup-success-wait">
                {{ restartRequired ? $t('setup.success.waitingRestart') : $t('setup.success.waitingReady') }}
              </p>
              <p class="setup-success-countdown">
                {{ $t('setup.success.waitElapsed', { seconds: waitElapsed }) }}
              </p>
            </div>
          </template>

          <template v-else>
            <header class="setup-panel-header">
              <div class="setup-panel-header-row">
                <h2 class="setup-panel-title">{{ currentStepTitle }}</h2>
                <span class="setup-panel-step">{{ $t('setup.progress', { current: activeStep + 1, total: stepKeys.length }) }}</span>
              </div>
              <el-progress
                :percentage="progressPercent"
                :show-text="false"
                :stroke-width="4"
                class="setup-progress-bar"
              />
            </header>

            <div class="setup-panel-body">
              <!-- Step 0: Database -->
              <div v-show="activeStep === 0" class="setup-step-panel">
                <el-form ref="dbForm" :model="form" :rules="dbRules" label-position="top" size="small" class="setup-inner-form">
                  <el-form-item :label="$t('setup.database.type')" prop="databaseType" class="setup-form-item-compact">
                    <div class="setup-pick-grid setup-pick-grid--2">
                      <button
                        type="button"
                        class="setup-pick"
                        :class="{ 'is-active': form.databaseType === 'h2' }"
                        @click="pickDatabaseType('h2')"
                      >
                        <i class="el-icon-coin setup-pick-icon" />
                        <strong>H2</strong>
                        <span>{{ $t('setup.database.h2CardDesc') }}</span>
                      </button>
                      <button
                        type="button"
                        class="setup-pick"
                        :class="{ 'is-active': form.databaseType === 'mysql' }"
                        @click="pickDatabaseType('mysql')"
                      >
                        <i class="el-icon-connection setup-pick-icon" />
                        <strong>MySQL</strong>
                        <span>{{ $t('setup.database.mysqlCardDesc') }}</span>
                      </button>
                    </div>
                  </el-form-item>
                  <el-form-item :label="$t('setup.database.database')" prop="databaseName">
                    <el-input
                      v-model="form.databaseName"
                      placeholder="cat2bug_platform"
                      @input="invalidateDatabaseTest"
                    />
                  </el-form-item>
                  <template v-if="form.databaseType === 'mysql'">
                    <div class="setup-form-row">
                      <el-form-item :label="$t('setup.database.host')" prop="mysqlHost" class="setup-form-row-item">
                        <el-input v-model="form.mysqlHost" placeholder="127.0.0.1" @input="invalidateDatabaseTest" />
                      </el-form-item>
                      <el-form-item :label="$t('setup.database.port')" prop="mysqlPort" class="setup-form-row-item setup-form-row-item--sm">
                        <el-input-number
                          v-model="form.mysqlPort"
                          :min="1"
                          :max="65535"
                          controls-position="right"
                          class="setup-full-width"
                          @change="invalidateDatabaseTest"
                        />
                      </el-form-item>
                    </div>
                    <div class="setup-form-row">
                      <el-form-item :label="$t('setup.database.username')" prop="mysqlUsername" class="setup-form-row-item">
                        <el-input v-model="form.mysqlUsername" @input="invalidateDatabaseTest" />
                      </el-form-item>
                      <el-form-item :label="$t('setup.database.password')" prop="mysqlPassword" class="setup-form-row-item">
                        <el-input v-model="form.mysqlPassword" type="password" show-password @input="invalidateDatabaseTest" />
                      </el-form-item>
                    </div>
                  </template>
                  <div class="setup-test-row">
                    <el-button :loading="dbTesting" @click="handleTestDatabase">{{ $t('setup.testConnection') }}</el-button>
                    <span v-if="dbTestPassed" class="setup-test-ok"><i class="el-icon-success" /> {{ $t('setup.testPassed') }}</span>
                  </div>
                  <p v-if="dbTestPassed && databaseMode === 'new'" class="setup-tip setup-tip--success">
                    <i class="el-icon-info" /> {{ $t('setup.database.modeNew') }}
                  </p>
                  <p v-else-if="dbTestPassed && databaseMode === 'existing'" class="setup-tip setup-tip--warn">
                    <i class="el-icon-warning" /> {{ $t('setup.database.modeExisting') }}
                  </p>
                  <p v-if="form.databaseType === 'mysql'" class="setup-tip">{{ $t('setup.database.mysqlTestHint') }}</p>
                  <p v-else class="setup-tip">{{ $t('setup.database.h2FileHint') }}</p>
                </el-form>
              </div>

              <!-- Step 1: Cache -->
              <div v-show="activeStep === 1" class="setup-step-panel">
                <el-form ref="cacheForm" :model="form" :rules="cacheRules" label-position="top" size="small" class="setup-inner-form">
                  <el-form-item :label="$t('setup.cache.type')" prop="cacheType" class="setup-form-item-compact">
                    <div class="setup-pick-grid setup-pick-grid--2">
                      <button
                        type="button"
                        class="setup-pick"
                        :class="{ 'is-active': form.cacheType === 'local' }"
                        @click="pickCacheType('local')"
                      >
                        <i class="el-icon-box setup-pick-icon" />
                        <strong>{{ $t('setup.cache.local') }}</strong>
                        <span>{{ $t('setup.cache.localCardDesc') }}</span>
                      </button>
                      <button
                        type="button"
                        class="setup-pick"
                        :class="{ 'is-active': form.cacheType === 'redis' }"
                        @click="pickCacheType('redis')"
                      >
                        <i class="el-icon-cpu setup-pick-icon" />
                        <strong>Redis</strong>
                        <span>{{ $t('setup.cache.redisCardDesc') }}</span>
                      </button>
                    </div>
                  </el-form-item>
                  <template v-if="form.cacheType === 'redis'">
                    <div class="setup-form-row">
                      <el-form-item :label="$t('setup.cache.host')" prop="redisHost" class="setup-form-row-item">
                        <el-input v-model="form.redisHost" placeholder="127.0.0.1" />
                      </el-form-item>
                      <el-form-item :label="$t('setup.cache.port')" prop="redisPort" class="setup-form-row-item setup-form-row-item--sm">
                        <el-input-number v-model="form.redisPort" :min="1" :max="65535" controls-position="right" class="setup-full-width" />
                      </el-form-item>
                    </div>
                    <div class="setup-form-row">
                      <el-form-item :label="$t('setup.cache.password')" class="setup-form-row-item">
                        <el-input v-model="form.redisPassword" type="password" show-password />
                      </el-form-item>
                      <el-form-item :label="$t('setup.cache.database')" class="setup-form-row-item setup-form-row-item--sm">
                        <el-input-number v-model="form.redisDatabase" :min="0" :max="15" controls-position="right" class="setup-full-width" />
                      </el-form-item>
                    </div>
                    <div class="setup-test-row">
                      <el-button :loading="redisTesting" @click="handleTestRedis">{{ $t('setup.testConnection') }}</el-button>
                      <span v-if="redisTestPassed" class="setup-test-ok"><i class="el-icon-success" /> {{ $t('setup.testPassed') }}</span>
                    </div>
                  </template>
                  <div v-else class="setup-feature-box">
                    <p class="setup-tip setup-tip--success">
                      <i class="el-icon-circle-check" /> {{ $t('setup.cache.localHint') }}
                    </p>
                  </div>
                </el-form>
              </div>

              <!-- Step 2: Storage -->
              <div v-show="activeStep === 2" class="setup-step-panel">
                <el-form ref="pathForm" :model="form" :rules="pathRules" label-position="top" size="small" class="setup-inner-form">
                  <el-form-item :label="$t('setup.storage.profile')" prop="profile">
                    <el-input v-model="form.profile" :placeholder="$t('setup.storage.profilePlaceholder')">
                      <i slot="prefix" class="el-input__icon el-icon-folder" />
                    </el-input>
                  </el-form-item>
                  <el-form-item :label="$t('setup.storage.logPath')" prop="logPath">
                    <el-input v-model="form.logPath" :placeholder="$t('setup.storage.logPlaceholder')">
                      <i slot="prefix" class="el-input__icon el-icon-document" />
                    </el-input>
                  </el-form-item>
                  <p class="setup-tip">{{ $t('setup.storage.restartHint') }}</p>
                </el-form>
              </div>

              <!-- Step 3: AI -->
              <div v-show="activeStep === 3" class="setup-step-panel">
                <el-form ref="aiForm" :model="form" label-position="top" size="small" class="setup-inner-form">
                  <el-form-item :label="$t('setup.ai.enabled')" class="setup-switch-line">
                    <el-switch v-model="form.aiEnabled" @change="onAiEnabledChange" />
                  </el-form-item>
                  <template v-if="form.aiEnabled">
                    <el-form-item :label="$t('setup.ai.host')" prop="aiHost">
                      <el-input v-model="form.aiHost" placeholder="http://127.0.0.1:11434" />
                      <p class="setup-tip setup-tip--inline">
                        {{ $t('setup.ai.hostHintPrefix') }}
                        <span class="setup-link">{{ ollamaDemoUrl }}</span>
                        <el-tooltip :content="$t('setup.ai.copyDemoUrlToInput')" placement="top">
                          <el-button type="text" class="setup-copy-btn" icon="el-icon-document-copy" @click="fillOllamaDemoUrl" />
                        </el-tooltip>
                        {{ $t('setup.ai.hostHintSuffix') }}
                      </p>
                    </el-form-item>
                    <div class="setup-test-row">
                      <el-button :loading="ollamaTesting" @click="handleTestOllama">{{ $t('setup.testConnection') }}</el-button>
                      <span v-if="ollamaTestPassed" class="setup-test-ok"><i class="el-icon-success" /> {{ $t('setup.testPassed') }}</span>
                    </div>
                  </template>
                  <p v-else class="setup-tip">{{ $t('setup.ai.disabledHint') }}</p>
                </el-form>
              </div>

              <!-- Step 4: Admin -->
              <div v-show="activeStep === 4" class="setup-step-panel">
                <el-form ref="adminForm" :model="form" :rules="adminRules" label-position="top" size="small" class="setup-inner-form">
                  <el-form-item :label="$t('setup.admin.username')" prop="adminUsername">
                    <el-input v-model="form.adminUsername" maxlength="30" autocomplete="username" />
                  </el-form-item>
                  <div class="setup-form-row">
                    <el-form-item :label="$t('setup.admin.password')" prop="adminPassword" class="setup-form-row-item">
                      <el-input v-model="form.adminPassword" type="password" show-password maxlength="50" autocomplete="new-password" />
                    </el-form-item>
                    <el-form-item :label="$t('setup.admin.confirmPassword')" prop="adminPasswordConfirm" class="setup-form-row-item">
                      <el-input v-model="form.adminPasswordConfirm" type="password" show-password maxlength="50" autocomplete="new-password" />
                    </el-form-item>
                  </div>
                  <div class="setup-panel-box">
                    <el-form-item :label="$t('setup.security.registerUser')" class="setup-switch-line">
                      <el-switch v-model="form.registerUser" />
                      <span class="setup-switch-hint">{{ $t('setup.security.registerUserHint') }}</span>
                    </el-form-item>
                    <el-form-item :label="$t('setup.security.loginCaptcha')" class="setup-switch-line">
                      <el-switch v-model="form.loginCaptchaEnabled" />
                      <span class="setup-switch-hint">{{ $t('setup.security.loginCaptchaHint') }}</span>
                    </el-form-item>
                  </div>
                </el-form>
              </div>

              <!-- Step 5: Confirm -->
              <div v-show="activeStep === 5" class="setup-step-panel">
                <p class="setup-step-desc">{{ $t('setup.confirm.desc') }}</p>
                <el-descriptions :column="1" border size="small" class="setup-summary">
                  <el-descriptions-item :label="$t('setup.database.type')">{{ form.databaseType.toUpperCase() }}</el-descriptions-item>
                  <el-descriptions-item :label="$t('setup.database.database')">{{ form.databaseName }}</el-descriptions-item>
                  <el-descriptions-item v-if="databaseMode" :label="$t('setup.database.modeLabel')">
                    {{ databaseMode === 'existing' ? $t('setup.database.modeExistingShort') : $t('setup.database.modeNewShort') }}
                  </el-descriptions-item>
                  <el-descriptions-item v-if="form.databaseType === 'mysql'" :label="$t('setup.database.connection')">
                    {{ form.mysqlHost }}:{{ form.mysqlPort }}/{{ form.databaseName }}
                  </el-descriptions-item>
                  <el-descriptions-item :label="$t('setup.cache.type')">{{ cacheTypeLabel }}</el-descriptions-item>
                  <el-descriptions-item :label="$t('setup.storage.profile')">{{ form.profile }}</el-descriptions-item>
                  <el-descriptions-item :label="$t('setup.storage.logPath')">{{ form.logPath }}</el-descriptions-item>
                  <el-descriptions-item :label="$t('setup.ai.enabled')">{{ form.aiEnabled ? $t('setup.yes') : $t('setup.no') }}</el-descriptions-item>
                  <el-descriptions-item v-if="form.aiEnabled" :label="$t('setup.ai.host')">{{ form.aiHost }}</el-descriptions-item>
                  <el-descriptions-item :label="$t('setup.admin.username')">{{ form.adminUsername }}</el-descriptions-item>
                  <el-descriptions-item :label="$t('setup.security.registerUser')">{{ form.registerUser ? $t('setup.yes') : $t('setup.no') }}</el-descriptions-item>
                  <el-descriptions-item :label="$t('setup.security.loginCaptcha')">{{ form.loginCaptchaEnabled ? $t('setup.yes') : $t('setup.no') }}</el-descriptions-item>
                </el-descriptions>
                <p class="setup-tip setup-tip--warn">{{ $t('setup.confirm.restartWarning') }}</p>
              </div>
            </div>

            <footer class="setup-panel-footer">
              <div class="setup-panel-footer-lang">
                <label class="setup-lang-label" for="setup-lang-select">{{ $t('setup.language') }}</label>
                <div class="setup-lang-control">
                  <svg-icon :icon-class="currentLangIcon" class="setup-lang-flag" />
                  <el-select
                    id="setup-lang-select"
                    :value="currentLocale"
                    size="small"
                    class="setup-lang-select"
                    @change="changeLang"
                  >
                    <el-option
                      v-for="lang in langOptions"
                      :key="lang.code"
                      :label="lang.label"
                      :value="lang.code"
                    >
                      <span class="setup-lang-option">
                        <svg-icon :icon-class="lang.icon" />
                        <span>{{ lang.label }}</span>
                      </span>
                    </el-option>
                  </el-select>
                </div>
              </div>
              <div class="setup-panel-footer-nav">
                <el-button v-if="activeStep > 0" size="small" @click="prevStep">{{ $t('setup.prev') }}</el-button>
                <el-button v-if="activeStep < 5" type="primary" size="small" :loading="nextStepLoading" @click="nextStep">{{ $t('setup.next') }}</el-button>
                <el-button v-else type="primary" size="small" :loading="submitting" @click="handleSubmit">{{ $t('setup.submit') }}</el-button>
              </div>
            </footer>
          </template>
        </div>
      </main>
      </div>

      <site-copyright tag="p" class="setup-copyright" />
    </div>
  </div>
</template>

<script>
import {
  getSetupStatus,
  testDatabase,
  testRedis,
  testOllama,
  submitSetup
} from '@/api/setup'
import { resetSetupStatusCache } from '@/utils/setup-status'
import store from '@/store'

const I18N_LOCALE_KEY = 'i18n-locale'
const SETUP_AWAITING_READY_KEY = 'cat2bug.setup.awaitingReady'
const SETUP_AWAITING_RESTART_KEY = 'cat2bug.setup.awaitingRestart'

const STEP_KEYS = [
  'setup.step.database',
  'setup.step.cache',
  'setup.step.storage',
  'setup.step.ai',
  'setup.step.admin',
  'setup.step.confirm'
]

function isTestSuccess(res) {
  if (res == null) return false
  if (res.success === true) return true
  if (res.success === false) return false
  return res.code === 200 || res.code === undefined
}

function testErrorMessage(res, fallback) {
  return (res && (res.message || res.msg)) || fallback
}

const DATABASE_NAME_PATTERN = /^[A-Za-z0-9_]+$/

function extractSetupApiData(res) {
  if (res && res.data != null && typeof res.data === 'object' && !Array.isArray(res.data)) {
    return res.data
  }
  return res || {}
}

function resolveDatabaseMode(res) {
  const mode = extractSetupApiData(res).databaseMode
  return mode === 'existing' || mode === 'new' ? mode : null
}

function resolveOllamaTestErrorMessage(resOrErr, t) {
  const raw = (resOrErr && (resOrErr.message || resOrErr.msg)) || ''
  const lower = String(raw).toLowerCase()
  if (!raw) {
    return t('setup.ai.testFailed')
  }
  if (lower.includes('connection refused')
    || lower.includes('connectexception')
    || lower.includes('拒绝连接')
    || lower.includes('无法连接')) {
    return t('setup.ai.testFailedUnreachable')
  }
  if (lower.includes('timeout') || lower.includes('timed out') || lower.includes('超时')) {
    return t('setup.ai.testFailedTimeout')
  }
  if (lower.includes('unknown host')
    || lower.includes('name or service not known')
    || lower.includes('nodename nor servname')
    || lower.includes('无法解析')) {
    return t('setup.ai.testFailedBadHost')
  }
  return t('setup.ai.testFailed')
}

export default {
  name: 'SetupWizard',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.form.adminPassword) {
        callback(new Error(this.$t('setup.admin.passwordMismatch')))
      } else {
        callback()
      }
    }
    return {
      stepKeys: STEP_KEYS,
      langOptions: [
        { code: 'zh_CN', label: '简体中文', icon: 'lang_zh_CN' },
        { code: 'zh_TW', label: '繁體中文', icon: 'lang_zh_CN' },
        { code: 'en_US', label: 'English', icon: 'lang_en_US' },
        { code: 'ru', label: 'Русский', icon: 'lang_ru' },
        { code: 'ja_JP', label: '日本語', icon: 'lang_ja_JP' },
        { code: 'ko_KR', label: '한국어', icon: 'lang_ko_KR' },
        { code: 'ar', label: 'العربية', icon: 'lang_ar' }
      ],
      activeStep: 0,
      ollamaDemoUrl: 'https://www.cat2bug.com:8023',
      finished: false,
      restartRequired: false,
      installWarning: null,
      waitElapsed: 0,
      waitElapsedTimer: null,
      waitPollTimer: null,
      submitting: false,
      dbTesting: false,
      redisTesting: false,
      ollamaTesting: false,
      dbTestPassed: false,
      databaseMode: null,
      redisTestPassed: false,
      ollamaTestPassed: false,
      form: {
        databaseType: 'h2',
        databaseName: 'cat2bug_platform',
        mysqlHost: '127.0.0.1',
        mysqlPort: 3306,
        mysqlUsername: 'root',
        mysqlPassword: '',
        cacheType: 'local',
        redisHost: '127.0.0.1',
        redisPort: 6379,
        redisPassword: '',
        redisDatabase: 0,
        profile: './upload',
        temp: './uploadTemp',
        logPath: './logs',
        aiEnabled: false,
        aiHost: 'http://127.0.0.1:11434',
        adminUsername: 'admin',
        adminPassword: 'cat2bug',
        adminPasswordConfirm: 'cat2bug',
        registerUser: true,
        loginCaptchaEnabled: false
      },
      adminRules: {
        adminUsername: [{ required: true, message: '', trigger: 'blur' }],
        adminPassword: [{ required: true, message: '', trigger: 'blur' }],
        adminPasswordConfirm: [
          { required: true, message: '', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    currentLocale() {
      return this.$i18n.locale
    },
    currentLangIcon() {
      const found = this.langOptions.find(item => item.code === this.currentLocale)
      return found ? found.icon : 'lang_zh_CN'
    },
    isRtl() {
      return this.currentLocale === 'ar'
    },
    progressPercent() {
      return Math.round(((this.activeStep + 1) / this.stepKeys.length) * 100)
    },
    restartHintText() {
      return this.form.cacheType === 'redis'
        ? this.$t('setup.success.restartingRedis')
        : this.$t('setup.success.restarting')
    },
    currentStepTitle() {
      return this.$t(this.stepKeys[this.activeStep] || 'setup.title')
    },
    nextStepLoading() {
      return this.dbTesting || this.redisTesting || this.ollamaTesting
    },
    cacheTypeLabel() {
      return this.form.cacheType === 'redis'
        ? this.$t('setup.cache.redis')
        : this.$t('setup.cache.local')
    },
    brandLogoSrc() {
      return require('@/assets/images/setup-wizard-mascot.png')
    },
    dbRules() {
      const validateDatabaseName = (rule, value, callback) => {
        const name = (value || '').trim()
        if (!name) {
          callback(new Error(this.$t('setup.database.databaseRequired')))
          return
        }
        if (!DATABASE_NAME_PATTERN.test(name)) {
          callback(new Error(this.$t('setup.database.namePatternInvalid')))
          return
        }
        callback()
      }
      const rules = {
        databaseType: [{ required: true, message: this.$t('setup.database.typeRequired'), trigger: 'change' }],
        databaseName: [
          { required: true, message: this.$t('setup.database.databaseRequired'), trigger: 'blur' },
          { validator: validateDatabaseName, trigger: 'blur' }
        ]
      }
      if (this.form.databaseType === 'mysql') {
        rules.mysqlHost = [{ required: true, message: this.$t('setup.database.hostRequired'), trigger: 'blur' }]
        rules.mysqlPort = [{ required: true, message: this.$t('setup.database.portRequired'), trigger: 'change' }]
        rules.mysqlUsername = [{ required: true, message: this.$t('setup.database.usernameRequired'), trigger: 'blur' }]
        rules.mysqlPassword = [{ required: true, message: this.$t('setup.database.passwordRequired'), trigger: 'blur' }]
      }
      return rules
    },
    cacheRules() {
      const rules = {
        cacheType: [{ required: true, message: this.$t('setup.cache.typeRequired'), trigger: 'change' }]
      }
      if (this.form.cacheType === 'redis') {
        rules.redisHost = [{ required: true, message: this.$t('setup.cache.hostRequired'), trigger: 'blur' }]
        rules.redisPort = [{ required: true, message: this.$t('setup.cache.portRequired'), trigger: 'blur' }]
      }
      return rules
    },
    pathRules() {
      return {
        profile: [{ required: true, message: this.$t('setup.storage.profileRequired'), trigger: 'blur' }],
        logPath: [{ required: true, message: this.$t('setup.storage.logRequired'), trigger: 'blur' }]
      }
    }
  },
  created() {
    // 探测并应用当前主题模式，使安装向导完美跟随系统黑白主题
    const cachedMode = localStorage.getItem('theme-mode')
    const themeMode = cachedMode || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
    if (themeMode === 'dark') {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }

    document.documentElement.classList.add('setup-wizard-active')
    const lang = this.$cache.local.get(I18N_LOCALE_KEY) || 'zh_CN'
    this.$i18n.locale = lang
    this.refreshFormRuleMessages()
    getSetupStatus().then(res => {
      const payload = (res && res.data) ? res.data : (res || {})
      if (payload.restartRequired || sessionStorage.getItem(SETUP_AWAITING_READY_KEY) === '1') {
        this.finished = true
        this.restartRequired = payload.restartRequired === true
          || sessionStorage.getItem(SETUP_AWAITING_RESTART_KEY) === '1'
        this.startWaitingForReady()
      }
    }).catch(() => {
      if (sessionStorage.getItem(SETUP_AWAITING_READY_KEY) === '1') {
        this.finished = true
        this.restartRequired = sessionStorage.getItem(SETUP_AWAITING_RESTART_KEY) === '1'
        this.startWaitingForReady()
      }
    })
  },
  beforeDestroy() {
    document.documentElement.classList.remove('setup-wizard-active')
    this.clearWaitTimers()
  },
  methods: {
    changeLang(lang) {
      this.$i18n.locale = lang
      this.$cache.local.set(I18N_LOCALE_KEY, lang)
      this.refreshFormRuleMessages()
    },
    pickDatabaseType(type) {
      this.form.databaseType = type
      this.onDatabaseTypeChange()
      this.$nextTick(() => {
        if (this.$refs.dbForm) {
          this.$refs.dbForm.validateField('databaseType')
        }
      })
    },
    pickCacheType(type) {
      this.form.cacheType = type
      this.onCacheTypeChange()
      this.$nextTick(() => {
        if (this.$refs.cacheForm) {
          this.$refs.cacheForm.validateField('cacheType')
        }
      })
    },
    refreshFormRuleMessages() {
      this.adminRules.adminUsername[0].message = this.$t('setup.admin.usernameRequired')
      this.adminRules.adminPassword[0].message = this.$t('setup.admin.passwordRequired')
      this.adminRules.adminPasswordConfirm[0].message = this.$t('setup.admin.passwordRequired')
    },
    onDatabaseTypeChange() {
      this.invalidateDatabaseTest()
    },
    invalidateDatabaseTest() {
      this.dbTestPassed = false
      this.databaseMode = null
    },
    onCacheTypeChange() {
      this.redisTestPassed = false
    },
    onAiEnabledChange() {
      this.ollamaTestPassed = false
    },
    fillOllamaDemoUrl() {
      this.form.aiHost = this.ollamaDemoUrl
      this.ollamaTestPassed = false
      this.$message.success(this.$t('setup.ai.demoUrlFilled'))
    },
    resolveDefaultTemp(profile) {
      const p = (profile || '').trim()
      if (!p) return './uploadTemp'
      if (p.endsWith('Path')) return p.slice(0, -4) + 'Temp'
      if (p.endsWith('/') || p.endsWith('\\')) return p + 'temp'
      return p + 'Temp'
    },
    buildDatabasePayload() {
      const payload = {
        databaseType: this.form.databaseType,
        database: (this.form.databaseName || '').trim()
      }
      if (this.form.databaseType === 'mysql') {
        payload.host = this.form.mysqlHost
        payload.port = this.form.mysqlPort
        payload.username = this.form.mysqlUsername
        payload.password = this.form.mysqlPassword
      }
      return payload
    },
    buildRedisPayload() {
      return {
        host: this.form.redisHost,
        port: this.form.redisPort,
        password: this.form.redisPassword || undefined,
        database: this.form.redisDatabase
      }
    },
    buildSubmitPayload() {
      const databaseName = (this.form.databaseName || '').trim()
      const payload = {
        databaseType: this.form.databaseType,
        databaseMode: this.databaseMode,
        cacheType: this.form.cacheType,
        profile: this.form.profile,
        temp: this.form.temp || this.resolveDefaultTemp(this.form.profile),
        logPath: this.form.logPath,
        registerUser: this.form.registerUser,
        captchaEnabled: this.form.loginCaptchaEnabled,
        aiEnabled: this.form.aiEnabled,
        aiHost: this.form.aiHost,
        adminUsername: (this.form.adminUsername || '').trim(),
        adminPassword: this.form.adminPassword
      }
      if (this.form.databaseType === 'mysql') {
        payload.mysqlHost = this.form.mysqlHost
        payload.mysqlPort = this.form.mysqlPort
        payload.mysqlDatabase = databaseName
        payload.mysqlUsername = this.form.mysqlUsername
        payload.mysqlPassword = this.form.mysqlPassword
      } else {
        payload.h2Database = databaseName
      }
      if (this.form.cacheType === 'redis') {
        payload.redisHost = this.form.redisHost
        payload.redisPort = this.form.redisPort
        payload.redisPassword = this.form.redisPassword || undefined
        payload.redisDatabase = this.form.redisDatabase
      }
      return payload
    },
    runDatabaseTest(options = {}) {
      const { silentSuccess = false } = options
      return new Promise((resolve, reject) => {
        const formRef = this.$refs.dbForm
        if (!formRef) {
          reject()
          return
        }
        formRef.validate(valid => {
          if (!valid) {
            reject()
            return
          }
          this.dbTesting = true
          this.invalidateDatabaseTest()
          testDatabase(this.buildDatabasePayload())
            .then(res => {
              if (isTestSuccess(res)) {
                this.dbTestPassed = true
                this.databaseMode = resolveDatabaseMode(res)
                if (!silentSuccess) {
                  this.$message.success(this.$t('setup.testPassed'))
                }
                resolve()
              } else {
                this.$message.error(testErrorMessage(res, this.$t('setup.testFailed')))
                reject()
              }
            })
            .catch(err => {
              this.$message.error(testErrorMessage(err, this.$t('setup.testFailed')))
              reject()
            })
            .finally(() => {
              this.dbTesting = false
            })
        })
      })
    },
    runRedisTest(options = {}) {
      const { silentSuccess = false } = options
      return new Promise((resolve, reject) => {
        const formRef = this.$refs.cacheForm
        if (!formRef) {
          reject()
          return
        }
        formRef.validate(valid => {
          if (!valid) {
            reject()
            return
          }
          this.redisTesting = true
          this.redisTestPassed = false
          testRedis(this.buildRedisPayload())
            .then(res => {
              if (isTestSuccess(res)) {
                this.redisTestPassed = true
                if (!silentSuccess) {
                  this.$message.success(this.$t('setup.testPassed'))
                }
                resolve()
              } else {
                this.$message.error(testErrorMessage(res, this.$t('setup.testFailed')))
                reject()
              }
            })
            .catch(err => {
              this.$message.error(testErrorMessage(err, this.$t('setup.testFailed')))
              reject()
            })
            .finally(() => {
              this.redisTesting = false
            })
        })
      })
    },
    runOllamaTest(options = {}) {
      const { silentSuccess = false } = options
      return new Promise((resolve, reject) => {
        if (!this.form.aiEnabled) {
          resolve()
          return
        }
        if (!this.form.aiHost) {
          this.$message.warning(this.$t('setup.ai.hostRequired'))
          reject()
          return
        }
        this.ollamaTesting = true
        this.ollamaTestPassed = false
        testOllama({ enabled: true, host: this.form.aiHost })
          .then(res => {
            if (isTestSuccess(res)) {
              this.ollamaTestPassed = true
              if (!silentSuccess) {
                this.$message.success(this.$t('setup.testPassed'))
              }
              resolve()
            } else {
              this.$message.error(resolveOllamaTestErrorMessage(res, this.$t.bind(this)))
              reject()
            }
          })
          .catch(err => {
            this.$message.error(resolveOllamaTestErrorMessage(err, this.$t.bind(this)))
            reject()
          })
          .finally(() => {
            this.ollamaTesting = false
          })
      })
    },
    handleTestDatabase() {
      this.runDatabaseTest().catch(() => {})
    },
    handleTestRedis() {
      this.runRedisTest().catch(() => {})
    },
    handleTestOllama() {
      this.runOllamaTest().catch(() => {})
    },
    validateCurrentStep() {
      return new Promise((resolve, reject) => {
        if (this.activeStep === 0) {
          this.$refs.dbForm.validate(valid => {
            if (!valid) return reject()
            if (!this.dbTestPassed) {
              this.runDatabaseTest({ silentSuccess: true }).then(resolve).catch(reject)
              return
            }
            resolve()
          })
          return
        }
        if (this.activeStep === 1) {
          this.$refs.cacheForm.validate(valid => {
            if (!valid) return reject()
            if (this.form.cacheType === 'redis' && !this.redisTestPassed) {
              this.runRedisTest({ silentSuccess: true }).then(resolve).catch(reject)
              return
            }
            resolve()
          })
          return
        }
        if (this.activeStep === 2) {
          this.$refs.pathForm.validate(valid => {
            if (!valid) return reject()
            resolve()
          })
          return
        }
        if (this.activeStep === 3) {
          if (this.form.aiEnabled && !this.ollamaTestPassed) {
            this.runOllamaTest({ silentSuccess: true }).then(resolve).catch(reject)
            return
          }
          resolve()
          return
        }
        if (this.activeStep === 4) {
          this.$refs.adminForm.validate(valid => {
            if (!valid) return reject()
            resolve()
          })
          return
        }
        resolve()
      })
    },
    nextStep() {
      this.validateCurrentStep()
        .then(() => {
          this.activeStep += 1
        })
        .catch(() => {})
    },
    prevStep() {
      if (this.activeStep > 0) {
        this.activeStep -= 1
      }
    },
    validateBeforeSubmit() {
      if (!this.dbTestPassed || !this.databaseMode) {
        this.$message.warning(this.$t('setup.database.testRequired'))
        return Promise.reject()
      }
      if (this.form.cacheType === 'redis' && !this.redisTestPassed) {
        this.$message.warning(this.$t('setup.cache.testRequired'))
        return Promise.reject()
      }
      if (this.form.aiEnabled && !this.ollamaTestPassed) {
        this.$message.warning(this.$t('setup.ai.testRequired'))
        return Promise.reject()
      }
      return this.$refs.adminForm.validate()
    },
    handleSubmit() {
      this.validateBeforeSubmit()
        .then(() => {
          this.submitting = true
          return submitSetup(this.buildSubmitPayload())
        })
        .then(res => {
          resetSetupStatusCache()
          const payload = (res && res.data) ? res.data : (res || {})
          this.installWarning = payload.mysqlManualImportHint || null
          this.restartRequired = payload.restartRequired === true
          this.finished = true
          sessionStorage.setItem(SETUP_AWAITING_READY_KEY, '1')
          if (this.restartRequired) {
            sessionStorage.setItem(SETUP_AWAITING_RESTART_KEY, '1')
          } else {
            sessionStorage.removeItem(SETUP_AWAITING_RESTART_KEY)
          }
          this.startWaitingForReady()
        })
        .catch(() => {})
        .finally(() => {
          this.submitting = false
        })
    },
    startWaitingForReady() {
      this.clearWaitTimers()
      this.waitElapsed = 0
      this.waitElapsedTimer = setInterval(() => {
        this.waitElapsed += 1
      }, 1000)
      this.pollApplicationReady()
      this.waitPollTimer = setInterval(() => {
        this.pollApplicationReady()
      }, 2000)
    },
    pollApplicationReady() {
      resetSetupStatusCache()
      getSetupStatus()
        .then(res => {
          const payload = (res && res.data) ? res.data : (res || {})
          if (payload.installed === true && payload.restartRequired !== true) {
            this.goToLogin()
          }
        })
        .catch(() => {})
    },
    clearWaitTimers() {
      if (this.waitElapsedTimer) {
        clearInterval(this.waitElapsedTimer)
        this.waitElapsedTimer = null
      }
      if (this.waitPollTimer) {
        clearInterval(this.waitPollTimer)
        this.waitPollTimer = null
      }
    },
    goToLogin() {
      this.clearWaitTimers()
      sessionStorage.removeItem(SETUP_AWAITING_READY_KEY)
      sessionStorage.removeItem(SETUP_AWAITING_RESTART_KEY)
      store.dispatch('FedLogOut').finally(() => {
        resetSetupStatusCache()
        window.location.href = '/login'
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
$setup-text: #303133;
$setup-muted: #606266;
$setup-muted-light: #909399;
$setup-border: #e4e7ed;
$setup-aside-bg: #f5f7fa;
$setup-wm-tile: 168px;
$setup-wm-pattern: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='168' height='168' viewBox='0 0 168 168'%3E%3Ctext x='8' y='96' font-family='system-ui,-apple-system,sans-serif' font-size='24' font-weight='700' fill='rgba(64,158,255,0.1)' letter-spacing='0.06em'%3ECat2Bug%3C/text%3E%3C/svg%3E");

.setup-page {
  position: relative;
  width: 100%;
  max-width: 100vw;
  height: 100vh;
  max-height: 100vh;
  overflow: hidden;
  background: linear-gradient(165deg, #f0f5fc 0%, #f8fafc 42%, #fff 100%);
  box-sizing: border-box;
}

.setup-page-watermark {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}

/* 层旋转 -45° 后，仅沿层内 X 轴位移 → 屏幕上是纯 45° 斜向滚动 */
.setup-page-watermark__pattern {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 320vmax;
  height: 320vmax;
  margin-top: -160vmax;
  margin-left: -160vmax;
  background-image: $setup-wm-pattern;
  background-repeat: repeat;
  background-size: $setup-wm-tile $setup-wm-tile;
  transform: rotate(-45deg);
  animation: setup-watermark-bg 32s linear infinite;
  will-change: background-position;
}

@keyframes setup-watermark-bg {
  0% {
    background-position: 0 0;
  }

  100% {
    background-position: $setup-wm-tile 0;
  }
}

.setup-layout {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 1200px;
  height: 100%;
  margin: 0 auto;
  padding: 20px 40px 10px;
  box-sizing: border-box;
  overflow: visible;
}

.setup-hero {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  display: block;
  min-height: 0;
  margin: 0 0 12px;
  padding: 0;
  padding-inline-end: 320px;
  -webkit-padding-end: 320px;
  border-radius: 12px;
  box-sizing: border-box;
  overflow: visible;
  isolation: isolate;
}

.setup-hero-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  border-radius: inherit;
  background: linear-gradient(105deg, rgba(255, 255, 255, 0.92) 0%, rgba(245, 247, 250, 0.88) 55%, rgba(236, 244, 255, 0.5) 100%);
  border: 1px solid rgba(228, 231, 237, 0.9);
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
  pointer-events: none;

  &::after {
    content: '';
    position: absolute;
    right: 8%;
    bottom: -20%;
    width: 280px;
    height: 280px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(64, 158, 255, 0.12) 0%, transparent 70%);
    pointer-events: none;
  }
}

.setup-hero-head {
  position: relative;
  z-index: 2;
  min-width: 0;
  max-width: 640px;
  padding-block: 40px;
  padding-inline: 20px 0;
  box-sizing: border-box;

  h1 {
    margin: 0 0 6px;
    font-size: 20px;
    font-weight: 600;
    color: $setup-text;
    line-height: 1.3;
  }
}

.setup-hero-art {
  position: absolute;
  z-index: 1;
  top: -30px;
  right: auto;
  bottom: -36px;
  left: max(0px, calc(100% - 448px));
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  width: 440px;
  height: auto;
  box-sizing: border-box;
  pointer-events: none;
}

.setup-hero-art-img {
  display: block;
  width: 440px;
  max-width: none;
  height: 160px;
  max-height: 160px;
  object-fit: contain;
  object-position: right bottom;
  user-select: none;
  filter: drop-shadow(0 10px 28px rgba(15, 23, 42, 0.1));
}

.setup-intro-lead {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: $setup-muted;
}

.setup-body {
  position: relative;
  z-index: 2;
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(260px, 340px) minmax(0, 1fr);
  align-items: stretch;
  gap: 20px;

  > .setup-aside,
  > .setup-main {
    min-height: 0;
    height: 100%;
  }
}

.setup-aside {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  padding: 12px 10px;
  background: $setup-aside-bg;
  border: 1px solid $setup-border;
  border-radius: 12px;
  box-sizing: border-box;
}

.setup-aside-content {
  flex: 1;
  min-height: 0;
  padding-inline: 12px;
  overflow-y: auto;
  box-sizing: border-box;
}

.setup-roadmap-title {
  margin: 0 0 8px;
  padding-bottom: 6px;
  font-size: 13px;
  font-weight: 600;
  color: $setup-text;
  line-height: 1.4;
  border-bottom: 1px solid #ebeef5;
}

.setup-intro-note {
  flex-shrink: 0;
  margin: auto 12px 0;
  padding: 8px 10px;
  font-size: 12px;
  line-height: 1.5;
  color: $setup-muted;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}

.setup-roadmap {
  list-style: none;
  margin: 0;
  padding: 0;
}

.setup-roadmap-item {
  display: flex;
  align-items: stretch;
  gap: 12px;
  padding: 0;
  min-height: 44px;

  &.is-active .setup-roadmap-marker {
    background: #409eff;
    border-color: #409eff;
    color: #fff;
  }

  &.is-done .setup-roadmap-marker {
    background: #ecf5ff;
    border-color: #409eff;
    color: #409eff;
  }

  &.is-active .setup-roadmap-text {
    color: $setup-text;
    font-weight: 600;
  }
}

.setup-roadmap-marker-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 28px;
  align-self: stretch;
}

.setup-roadmap-marker {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: $setup-muted-light;
  background: #fff;
  z-index: 1;
}

.setup-roadmap-connector {
  flex: 1 1 auto;
  width: 2px;
  min-height: 10px;
  margin: 4px 0;
  background: #e4e7ed;
}

.setup-roadmap-item.is-done .setup-roadmap-connector {
  background: #b3d8ff;
}

.setup-roadmap-text {
  padding-top: 4px;
  font-size: 14px;
  line-height: 1.35;
  color: $setup-muted;
}

.setup-main {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  min-width: 0;
  max-width: 100%;
}

.setup-panel {
  flex: 1 1 auto;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid $setup-border;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
  overflow: hidden;
}

.setup-panel-fill {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.setup-panel-header {
  flex-shrink: 0;
  padding: 12px 18px 10px;
  border-bottom: 1px solid #ebeef5;
  background: #fafbfc;
}

.setup-panel-header-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}

.setup-panel-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: $setup-text;
}

.setup-panel-step {
  flex-shrink: 0;
  font-size: 12px;
  color: $setup-muted-light;
}

.setup-panel-body {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 12px 18px;
}

.setup-panel-footer {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 18px;
  border-top: 1px solid #ebeef5;
  background: #fafbfc;
}

.setup-panel-footer-lang {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.setup-panel-footer-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.setup-progress-bar {
  ::v-deep .el-progress-bar__outer {
    border-radius: 4px;
    background: #ebeef5;
  }

  ::v-deep .el-progress-bar__inner {
    border-radius: 4px;
  }
}

.setup-step-panel {
  animation: setup-in 0.2s ease;
}

@keyframes setup-in {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .setup-step-panel {
    animation: none;
  }

}

.setup-inner-form {
  ::v-deep .el-form-item {
    margin-bottom: 12px;
  }

  ::v-deep .el-form-item__label {
    padding-bottom: 4px;
    line-height: 1.3;
    font-weight: 500;
    color: $setup-text;
  }
}

.setup-form-item-compact {
  margin-bottom: 12px;
}

.setup-pick-grid {
  display: grid;
  gap: 12px;
  width: 100%;

  &--2 {
    grid-template-columns: 1fr 1fr;
  }
}

.setup-pick {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  width: 100%;
  padding: 10px 12px;
  text-align: start;
  border: 2px solid #dcdfe6;
  border-radius: 8px;
  background: #fafafa;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;

  strong {
    font-size: 15px;
    color: $setup-text;
  }

  span {
    font-size: 12px;
    line-height: 1.45;
    color: $setup-muted;
  }

  &:hover {
    border-color: #c6e2ff;
    background: #f5faff;
  }

  &.is-active {
    border-color: #409eff;
    background: #ecf5ff;
    box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.25);
  }
}

.setup-pick-icon {
  font-size: 22px;
  color: #409eff;
}

.setup-form-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.setup-form-row-item {
  flex: 1 1 180px;
  min-width: 0;
  margin-bottom: 0;

  &--sm {
    flex: 0 1 110px;
  }
}

.setup-full-width {
  width: 100%;
}

.setup-feature-box {
  margin-top: 4px;
}

.setup-tip {
  margin: 8px 0 0;
  padding: 10px 12px;
  font-size: 12px;
  line-height: 1.6;
  color: $setup-muted;
  background: #f5f7fa;
  border-radius: 6px;
  border: 1px solid #ebeef5;

  &--success {
    background: #f0f9eb;
    border-color: #e1f3d8;
    color: #529b2e;

    i {
      margin-right: 4px;
    }
  }

  &--warn {
    background: #fdf6ec;
    border-color: #faecd8;
    color: #b88230;
  }

  &--inline {
    margin-top: 8px;
    padding: 10px 12px;
  }
}

.setup-link {
  color: #409eff;
  word-break: break-all;
}

.setup-copy-btn {
  padding: 0 4px;
  vertical-align: middle;
}

.setup-test-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.setup-test-ok {
  font-size: 13px;
  color: #67c23a;
}

.setup-switch-line {
  ::v-deep .el-form-item__content {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px;
  }
}

.setup-switch-hint {
  flex: 1 1 180px;
  font-size: 13px;
  color: $setup-muted-light;
  line-height: 1.5;
}

.setup-panel-box {
  padding: 14px 16px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 6px;

  ::v-deep .el-form-item {
    margin-bottom: 14px;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.setup-step-desc {
  margin: 0 0 14px;
  font-size: 14px;
  line-height: 1.6;
  color: $setup-muted;
}

.setup-summary {
  margin-bottom: 12px;
}

.setup-lang-label {
  font-size: 12px;
  color: $setup-muted;
  white-space: nowrap;
}

.setup-lang-control {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.setup-lang-flag {
  flex-shrink: 0;
  font-size: 22px;
}

.setup-lang-select {
  width: 148px;
}

.setup-lang-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;

  .svg-icon {
    flex-shrink: 0;
    font-size: 20px;
  }
}

.setup-copyright {
  flex-shrink: 0;
  margin: 10px 0 0;
  font-size: 12px;
  color: $setup-muted-light;
  text-align: center;
}

.setup-success {
  text-align: center;
  padding: 24px 20px;

  h3 {
    margin: 16px 0 8px;
    color: $setup-text;
  }

  p {
    margin: 0;
    line-height: 1.6;
    color: $setup-muted;
  }
}

.setup-success-icon {
  font-size: 64px;
  color: #67c23a;
}

.setup-success-countdown {
  margin-top: 16px !important;
  font-size: 13px !important;
  color: $setup-muted-light !important;
}

.setup-page--rtl {
  .setup-hero-art {
    right: auto;
    left: max(0px, calc(100% - 448px));
    justify-content: flex-start;
  }

  .setup-hero-art-img {
    object-position: left bottom;
  }

  .setup-hero-bg::after {
    right: auto;
    left: 8%;
  }

  .setup-tip--success i {
    margin-right: 0;
    margin-left: 4px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .setup-page-watermark__pattern {
    animation: none;
  }

  .setup-hero-art-img {
    filter: none;
  }
}

@media screen and (max-width: 980px) {
  .setup-page {
    height: auto;
    max-height: none;
    overflow-x: hidden;
    overflow-y: auto;
  }

  .setup-layout {
    height: auto;
    max-width: 100%;
    padding: 12px 16px 20px;
    overflow-x: hidden;
  }

  .setup-body {
    grid-template-columns: 1fr;
    min-width: 0;
    max-width: 100%;
  }

  .setup-aside {
    display: none;
  }

  .setup-body > .setup-main {
    height: auto;
  }

  .setup-main {
    min-height: 0;
    width: 100%;
  }

  .setup-panel {
    height: auto;
    max-width: 100%;
  }

  .setup-panel-body {
    max-height: none;
  }

  .setup-panel-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .setup-panel-footer-lang {
    flex-wrap: wrap;
  }

  .setup-lang-control {
    flex: 1;
    min-width: 0;
  }

  .setup-lang-select {
    flex: 1;
    min-width: 0;
    max-width: 100%;
  }

  .setup-panel-footer-nav {
    justify-content: stretch;

    .el-button {
      flex: 1;
      min-width: 0;
    }
  }

  .setup-pick-grid--2 {
    grid-template-columns: 1fr;
  }

  .setup-form-row-item--sm {
    flex: 1 1 100%;
  }
}

@media screen and (max-width: 550px) {
  .setup-hero {
    padding-inline-end: 0;
    -webkit-padding-end: 0;
  }

  .setup-hero-art {
    display: none;
  }

  .setup-hero-head {
    max-width: none;
    padding-block: 40px;
    padding-inline: 20px;
  }
}

</style>

<style lang="scss">
html.setup-wizard-active,
html.setup-wizard-active body {
  width: 100%;
  max-width: 100vw;
  height: 100%;
  margin: 0;
  overflow: hidden;
}

html.setup-wizard-active #app {
  width: 100%;
  max-width: 100vw;
  height: 100%;
  overflow-x: hidden;
}

@media screen and (max-width: 980px) {
  html.setup-wizard-active,
  html.setup-wizard-active body {
    overflow-x: hidden;
    overflow-y: auto;
  }
}
</style>
