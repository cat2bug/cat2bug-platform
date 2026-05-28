<template>
  <div class="logo-page setup-page">
    <div class="login setup-layout">
      <div class="login-introduce setup-introduce">
        <h1>{{ $t('setup.title') }}</h1>
        <p>{{ $t('setup.introduce1') }}</p>
        <p>{{ $t('setup.introduce2') }}</p>
        <p v-if="!finished">{{ $t('setup.introduce3') }}</p>
      </div>
      <div class="setup-body">
        <el-image
          class="setup-logo"
          :src="require('@/assets/images/cat2bug-logo.gif')"
        />
        <div class="login-form setup-form">
          <template v-if="finished">
            <div class="setup-success">
              <i class="el-icon-success setup-success-icon" />
              <h3>{{ $t('setup.success.title') }}</h3>
              <p>{{ $t('setup.success.message') }}</p>
              <p class="setup-success-hint">{{ $t('setup.success.restart') }}</p>
              <p v-if="installWarning" class="setup-success-hint setup-success-warning">{{ installWarning }}</p>
              <p class="setup-success-countdown">
                {{ $t('setup.success.redirectCountdown', { seconds: redirectCountdown }) }}
              </p>
              <el-link type="primary" class="setup-success-login-link" @click="goToLogin">
                {{ $t('setup.success.goLoginNow') }}
              </el-link>
            </div>
          </template>
          <template v-else>
            <el-steps :active="activeStep" align-center finish-status="success" class="setup-steps">
              <el-step :title="$t('setup.step.database')" />
              <el-step :title="$t('setup.step.cache')" />
              <el-step :title="$t('setup.step.storage')" />
              <el-step :title="$t('setup.step.ai')" />
              <el-step :title="$t('setup.step.admin')" />
              <el-step :title="$t('setup.step.confirm')" />
            </el-steps>

            <div class="setup-step-content">
              <!-- Step 0: Database -->
              <div v-show="activeStep === 0">
                <el-form ref="dbForm" :model="form" :rules="dbRules" label-position="top">
                  <el-form-item :label="$t('setup.database.type')" prop="databaseType">
                    <el-radio-group v-model="form.databaseType" @change="onDatabaseTypeChange">
                      <el-radio label="h2">H2</el-radio>
                      <el-radio label="mysql">MySQL</el-radio>
                    </el-radio-group>
                  </el-form-item>
                  <template v-if="form.databaseType === 'mysql'">
                    <el-form-item :label="$t('setup.database.host')" prop="mysqlHost">
                      <el-input v-model="form.mysqlHost" placeholder="127.0.0.1" />
                    </el-form-item>
                    <el-form-item :label="$t('setup.database.port')" prop="mysqlPort">
                      <el-input-number v-model="form.mysqlPort" :min="1" :max="65535" controls-position="right" />
                    </el-form-item>
                    <el-form-item :label="$t('setup.database.database')" prop="mysqlDatabase">
                      <el-input v-model="form.mysqlDatabase" placeholder="cat2bug_platform" />
                    </el-form-item>
                    <el-form-item :label="$t('setup.database.username')" prop="mysqlUsername">
                      <el-input v-model="form.mysqlUsername" />
                    </el-form-item>
                    <el-form-item :label="$t('setup.database.password')" prop="mysqlPassword">
                      <el-input v-model="form.mysqlPassword" type="password" show-password />
                    </el-form-item>
                  </template>
                  <p v-else class="setup-hint">{{ $t('setup.database.h2Hint') }}</p>
                  <el-form-item v-if="form.databaseType === 'mysql'">
                    <el-button :loading="dbTesting" @click="handleTestDatabase">{{ $t('setup.testConnection') }}</el-button>
                    <span v-if="dbTestPassed" class="setup-test-ok"><i class="el-icon-success" /> {{ $t('setup.testPassed') }}</span>
                  </el-form-item>
                  <p v-if="form.databaseType === 'mysql'" class="setup-hint">{{ $t('setup.database.mysqlAutoCreateHint') }}</p>
                </el-form>
              </div>

              <!-- Step 1: Cache -->
              <div v-show="activeStep === 1">
                <el-form ref="cacheForm" :model="form" :rules="cacheRules" label-position="top">
                  <el-form-item :label="$t('setup.cache.type')" prop="cacheType">
                    <el-radio-group v-model="form.cacheType" @change="onCacheTypeChange">
                      <el-radio label="local">{{ $t('setup.cache.local') }}</el-radio>
                      <el-radio label="redis">{{ $t('setup.cache.redis') }}</el-radio>
                    </el-radio-group>
                  </el-form-item>
                  <template v-if="form.cacheType === 'redis'">
                    <el-form-item :label="$t('setup.cache.host')" prop="redisHost">
                      <el-input v-model="form.redisHost" placeholder="127.0.0.1" />
                    </el-form-item>
                    <el-form-item :label="$t('setup.cache.port')" prop="redisPort">
                      <el-input-number v-model="form.redisPort" :min="1" :max="65535" controls-position="right" />
                    </el-form-item>
                    <el-form-item :label="$t('setup.cache.password')">
                      <el-input v-model="form.redisPassword" type="password" show-password />
                    </el-form-item>
                    <el-form-item :label="$t('setup.cache.database')">
                      <el-input-number v-model="form.redisDatabase" :min="0" :max="15" controls-position="right" />
                    </el-form-item>
                    <el-form-item>
                      <el-button :loading="redisTesting" @click="handleTestRedis">{{ $t('setup.testConnection') }}</el-button>
                      <span v-if="redisTestPassed" class="setup-test-ok"><i class="el-icon-success" /> {{ $t('setup.testPassed') }}</span>
                    </el-form-item>
                  </template>
                  <p v-else class="setup-hint">{{ $t('setup.cache.localHint') }}</p>
                </el-form>
              </div>

              <!-- Step 2: Storage & log -->
              <div v-show="activeStep === 2">
                <el-form ref="pathForm" :model="form" :rules="pathRules" label-position="top">
                  <el-form-item :label="$t('setup.storage.profile')" prop="profile">
                    <el-input v-model="form.profile" :placeholder="$t('setup.storage.profilePlaceholder')" />
                  </el-form-item>
                  <el-form-item :label="$t('setup.storage.logPath')" prop="logPath">
                    <el-input v-model="form.logPath" :placeholder="$t('setup.storage.logPlaceholder')" />
                  </el-form-item>
                  <p class="setup-hint">{{ $t('setup.storage.restartHint') }}</p>
                </el-form>
              </div>

              <!-- Step 3: AI -->
              <div v-show="activeStep === 3">
                <el-form ref="aiForm" :model="form" label-position="top">
                  <el-form-item :label="$t('setup.ai.enabled')">
                    <el-switch v-model="form.aiEnabled" @change="onAiEnabledChange" />
                  </el-form-item>
                  <template v-if="form.aiEnabled">
                    <el-form-item :label="$t('setup.ai.host')" prop="aiHost">
                      <el-input v-model="form.aiHost" placeholder="http://127.0.0.1:11434" />
                      <p class="setup-hint setup-ai-host-hint">
                        {{ $t('setup.ai.hostHintPrefix') }}
                        <span class="setup-ai-demo-url">{{ ollamaDemoUrl }}</span>
                        <el-tooltip :content="$t('setup.ai.copyDemoUrlToInput')" placement="top">
                          <el-button
                            type="text"
                            class="setup-ai-demo-copy"
                            icon="el-icon-document-copy"
                            @click="fillOllamaDemoUrl"
                          />
                        </el-tooltip>
                        {{ $t('setup.ai.hostHintSuffix') }}
                      </p>
                    </el-form-item>
                    <el-form-item>
                      <el-button :loading="ollamaTesting" @click="handleTestOllama">{{ $t('setup.testConnection') }}</el-button>
                      <span v-if="ollamaTestPassed" class="setup-test-ok"><i class="el-icon-success" /> {{ $t('setup.testPassed') }}</span>
                    </el-form-item>
                  </template>
                  <p v-else class="setup-hint">{{ $t('setup.ai.disabledHint') }}</p>
                </el-form>
              </div>

              <!-- Step 4: Admin & security -->
              <div v-show="activeStep === 4">
                <el-form ref="adminForm" :model="form" :rules="adminRules" label-position="top">
                  <el-form-item :label="$t('setup.admin.username')" prop="adminUsername">
                    <el-input v-model="form.adminUsername" maxlength="30" />
                  </el-form-item>
                  <el-form-item :label="$t('setup.admin.password')" prop="adminPassword">
                    <el-input v-model="form.adminPassword" type="password" show-password maxlength="50" />
                  </el-form-item>
                  <el-form-item :label="$t('setup.admin.confirmPassword')" prop="adminPasswordConfirm">
                    <el-input v-model="form.adminPasswordConfirm" type="password" show-password maxlength="50" />
                  </el-form-item>
                  <el-form-item :label="$t('setup.security.registerUser')">
                    <el-switch v-model="form.registerUser" />
                    <span class="setup-switch-hint">{{ $t('setup.security.registerUserHint') }}</span>
                  </el-form-item>
                  <el-form-item :label="$t('setup.security.loginCaptcha')">
                    <el-switch v-model="form.loginCaptchaEnabled" />
                    <span class="setup-switch-hint">{{ $t('setup.security.loginCaptchaHint') }}</span>
                  </el-form-item>
                </el-form>
              </div>

              <!-- Step 5: Confirm -->
              <div v-show="activeStep === 5">
                <p class="setup-step-desc">{{ $t('setup.confirm.desc') }}</p>
                <el-descriptions :column="1" border size="small" class="setup-summary">
                  <el-descriptions-item :label="$t('setup.database.type')">{{ form.databaseType.toUpperCase() }}</el-descriptions-item>
                  <el-descriptions-item v-if="form.databaseType === 'mysql'" :label="$t('setup.database.connection')">
                    {{ form.mysqlHost }}:{{ form.mysqlPort }}/{{ form.mysqlDatabase }}
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
                <p class="setup-hint setup-confirm-warning">{{ $t('setup.confirm.restartWarning') }}</p>
              </div>
            </div>

            <div class="setup-actions between-row">
              <div class="lang-group">
                <svg-icon
                  icon-class="lang_zh_CN"
                  :class="{ 'lang-active': currentLocale === 'zh_CN' }"
                  @click="changeLang('zh_CN')"
                />
                <svg-icon
                  icon-class="lang_zh_TW"
                  :class="{ 'lang-active': currentLocale === 'zh_TW' }"
                  @click="changeLang('zh_TW')"
                />
                <svg-icon
                  icon-class="lang_en_US"
                  :class="{ 'lang-active': currentLocale === 'en_US' }"
                  @click="changeLang('en_US')"
                />
                <svg-icon
                  icon-class="lang_ru"
                  :class="{ 'lang-active': currentLocale === 'ru' }"
                  @click="changeLang('ru')"
                />
                <svg-icon
                  icon-class="lang_ja_JP"
                  :class="{ 'lang-active': currentLocale === 'ja_JP' }"
                  @click="changeLang('ja_JP')"
                />
                <svg-icon
                  icon-class="lang_ko_KR"
                  :class="{ 'lang-active': currentLocale === 'ko_KR' }"
                  @click="changeLang('ko_KR')"
                />
                <svg-icon
                  icon-class="lang_ar"
                  :class="{ 'lang-active': currentLocale === 'ar' }"
                  @click="changeLang('ar')"
                />
              </div>
              <div class="setup-nav-buttons">
                <el-button v-if="activeStep > 0" @click="prevStep">{{ $t('setup.prev') }}</el-button>
                <el-button v-if="activeStep < 5" type="primary" @click="nextStep">{{ $t('setup.next') }}</el-button>
                <el-button v-else type="primary" :loading="submitting" @click="handleSubmit">{{ $t('setup.submit') }}</el-button>
              </div>
            </div>
          </template>
        </div>
        <span class="login-copyright">{{ $t('copyright') }}</span>
      </div>
    </div>
    <el-image
      class="login-mouse"
      style="width: 150px;"
      :src="require('@/assets/images/cat2bug-mouse.gif')"
    />
  </div>
</template>

<script>
import {
  testDatabase,
  testRedis,
  testOllama,
  submitSetup
} from '@/api/setup'
import { resetSetupStatusCache } from '@/utils/setup-status'
import store from '@/store'

const I18N_LOCALE_KEY = 'i18n-locale'

function isTestSuccess(res) {
  if (res == null) return false
  if (res.success === true) return true
  if (res.success === false) return false
  return res.code === 200 || res.code === undefined
}

function testErrorMessage(res, fallback) {
  return (res && (res.message || res.msg)) || fallback
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
      activeStep: 0,
      ollamaDemoUrl: 'https://www.cat2bug.com:8023',
      finished: false,
      installWarning: null,
      redirectCountdown: 30,
      redirectTimer: null,
      submitting: false,
      dbTesting: false,
      redisTesting: false,
      ollamaTesting: false,
      dbTestPassed: true,
      redisTestPassed: false,
      ollamaTestPassed: false,
      form: {
        databaseType: 'h2',
        mysqlHost: '127.0.0.1',
        mysqlPort: 3306,
        mysqlDatabase: 'cat2bug_platform',
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
    cacheTypeLabel() {
      return this.form.cacheType === 'redis'
        ? this.$t('setup.cache.redis')
        : this.$t('setup.cache.local')
    },
    dbRules() {
      const rules = {
        databaseType: [{ required: true, message: this.$t('setup.database.typeRequired'), trigger: 'change' }]
      }
      if (this.form.databaseType === 'mysql') {
        rules.mysqlHost = [{ required: true, message: this.$t('setup.database.hostRequired'), trigger: 'blur' }]
        rules.mysqlPort = [{ required: true, message: this.$t('setup.database.portRequired'), trigger: 'change' }]
        rules.mysqlDatabase = [{ required: true, message: this.$t('setup.database.databaseRequired'), trigger: 'blur' }]
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
    const lang = this.$cache.local.get(I18N_LOCALE_KEY) || 'zh_CN'
    this.$i18n.locale = lang
    this.refreshFormRuleMessages()
  },
  beforeDestroy() {
    this.clearRedirectCountdown()
  },
  methods: {
    changeLang(lang) {
      this.$i18n.locale = lang
      this.$cache.local.set(I18N_LOCALE_KEY, lang)
      this.refreshFormRuleMessages()
    },
    refreshFormRuleMessages() {
      this.adminRules.adminUsername[0].message = this.$t('setup.admin.usernameRequired')
      this.adminRules.adminPassword[0].message = this.$t('setup.admin.passwordRequired')
      this.adminRules.adminPasswordConfirm[0].message = this.$t('setup.admin.passwordRequired')
    },
    onDatabaseTypeChange() {
      this.dbTestPassed = this.form.databaseType === 'h2'
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
      const payload = { databaseType: this.form.databaseType }
      if (this.form.databaseType === 'mysql') {
        payload.host = this.form.mysqlHost
        payload.port = this.form.mysqlPort
        payload.database = this.form.mysqlDatabase
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
      const payload = {
        databaseType: this.form.databaseType,
        cacheType: this.form.cacheType,
        profile: this.form.profile,
        temp: this.form.temp || this.resolveDefaultTemp(this.form.profile),
        logPath: this.form.logPath,
        adminUsername: this.form.adminUsername,
        adminPassword: this.form.adminPassword,
        registerUser: this.form.registerUser,
        captchaEnabled: this.form.loginCaptchaEnabled,
        aiEnabled: this.form.aiEnabled,
        aiHost: this.form.aiHost
      }
      if (this.form.databaseType === 'mysql') {
        payload.mysqlHost = this.form.mysqlHost
        payload.mysqlPort = this.form.mysqlPort
        payload.mysqlDatabase = this.form.mysqlDatabase
        payload.mysqlUsername = this.form.mysqlUsername
        payload.mysqlPassword = this.form.mysqlPassword
      }
      if (this.form.cacheType === 'redis') {
        payload.redisHost = this.form.redisHost
        payload.redisPort = this.form.redisPort
        payload.redisPassword = this.form.redisPassword || undefined
        payload.redisDatabase = this.form.redisDatabase
      }
      return payload
    },
    handleTestDatabase() {
      const formRef = this.$refs.dbForm
      if (!formRef) return
      formRef.validate(valid => {
        if (!valid) return
        this.dbTesting = true
        this.dbTestPassed = false
        testDatabase(this.buildDatabasePayload())
          .then(res => {
            if (isTestSuccess(res)) {
              this.dbTestPassed = true
              this.$message.success(this.$t('setup.testPassed'))
            } else {
              this.$message.error(testErrorMessage(res, this.$t('setup.testFailed')))
            }
          })
          .catch(err => {
            this.$message.error(testErrorMessage(err, this.$t('setup.testFailed')))
          })
          .finally(() => {
            this.dbTesting = false
          })
      })
    },
    handleTestRedis() {
      const formRef = this.$refs.cacheForm
      if (!formRef) return
      formRef.validate(valid => {
        if (!valid) return
        this.redisTesting = true
        this.redisTestPassed = false
        testRedis(this.buildRedisPayload())
          .then(res => {
            if (isTestSuccess(res)) {
              this.redisTestPassed = true
              this.$message.success(this.$t('setup.testPassed'))
            } else {
              this.$message.error(testErrorMessage(res, this.$t('setup.testFailed')))
            }
          })
          .catch(err => {
            this.$message.error(testErrorMessage(err, this.$t('setup.testFailed')))
          })
          .finally(() => {
            this.redisTesting = false
          })
      })
    },
    handleTestOllama() {
      if (!this.form.aiEnabled) return
      if (!this.form.aiHost) {
        this.$message.warning(this.$t('setup.ai.hostRequired'))
        return
      }
      this.ollamaTesting = true
      this.ollamaTestPassed = false
      testOllama({ enabled: true, host: this.form.aiHost })
        .then(res => {
          if (isTestSuccess(res)) {
            this.ollamaTestPassed = true
            this.$message.success(this.$t('setup.testPassed'))
          } else {
            this.$message.error(testErrorMessage(res, this.$t('setup.testFailed')))
          }
        })
        .catch(err => {
          this.$message.error(testErrorMessage(err, this.$t('setup.testFailed')))
        })
        .finally(() => {
          this.ollamaTesting = false
        })
    },
    validateCurrentStep() {
      return new Promise((resolve, reject) => {
        if (this.activeStep === 0) {
          this.$refs.dbForm.validate(valid => {
            if (!valid) return reject()
            if (this.form.databaseType === 'mysql' && !this.dbTestPassed) {
              this.$message.warning(this.$t('setup.database.testRequired'))
              return reject()
            }
            resolve()
          })
          return
        }
        if (this.activeStep === 1) {
          this.$refs.cacheForm.validate(valid => {
            if (!valid) return reject()
            if (this.form.cacheType === 'redis' && !this.redisTestPassed) {
              this.$message.warning(this.$t('setup.cache.testRequired'))
              return reject()
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
            this.$message.warning(this.$t('setup.ai.testRequired'))
            reject()
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
      if (this.form.databaseType === 'mysql' && !this.dbTestPassed) {
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
          this.finished = true
          this.$nextTick(() => {
            this.startRedirectCountdown()
          })
        })
        .catch(() => {})
        .finally(() => {
          this.submitting = false
        })
    },
    startRedirectCountdown() {
      this.clearRedirectCountdown()
      this.redirectCountdown = 30
      this.redirectTimer = setInterval(() => {
        if (this.redirectCountdown <= 1) {
          this.goToLogin()
          return
        }
        this.redirectCountdown -= 1
      }, 1000)
    },
    clearRedirectCountdown() {
      if (this.redirectTimer) {
        clearInterval(this.redirectTimer)
        this.redirectTimer = null
      }
    },
    goToLogin() {
      this.clearRedirectCountdown()
      store.dispatch('FedLogOut').finally(() => {
        resetSetupStatusCache()
        window.location.href = '/login'
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss">
body {
  overflow: hidden;
}

.logo-page {
  height: 100%;
  overflow: hidden;
}

.login {
  display: flex;
  align-items: center;
  height: 100%;
  background-size: cover;
  background-color: white;
}

.login-introduce {
  float: right;
  max-width: 45%;
  font-size: 22px;
  padding-right: 80px;
  border-right: 1px solid #DCDFE6;
}

.login-mouse {
  position: absolute;
  bottom: 0;
  width: 150px;
  animation: setup-mouse-move 16s linear infinite;
  left: 100%;
}

@keyframes setup-mouse-move {
  0% {
    left: 100%;
    transform: rotateY(0deg);
  }
  15% {
    left: calc(0% - 200px);
    transform: rotateY(0deg);
  }
  41% {
    left: calc(0% - 200px);
    transform: rotateY(-180deg);
  }
  55% {
    left: 100%;
    transform: rotateY(-180deg);
  }
  56% {
    left: 100%;
    transform: rotateY(0deg);
  }
  100% {
    left: 100%;
    transform: rotateY(0deg);
  }
}

.between-row {
  width: 100%;
  display: inline-flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;

  .lang-group {
    display: inline-flex;
    flex-direction: row;
    justify-content: flex-end;

    > * {
      font-size: 22px;
      margin-left: 3px;
      cursor: pointer;
      opacity: 0.45;
      transition: opacity 0.15s ease;

      &.lang-active {
        opacity: 1;
      }

      &:hover {
        opacity: 0.85;
      }

      &.lang-active:hover {
        opacity: 1;
      }
    }
  }
}

.login-copyright {
  font-family: Arial;
  font-size: 14px;
  color: #606266;
}

.login-form {
  border-radius: 6px;
  background: #ffffff;
  border: 3px solid #5A5A59;

  .el-input {
    height: 38px;

    input {
      height: 38px;
    }
  }
}

.setup-page {
  overflow: auto;
}

.setup-layout {
  min-height: 100%;
  padding: 24px 0;
}

.setup-introduce {
  max-width: 40%;
}

.setup-body {
  float: right;
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 620px;
  width: 100%;
}

.setup-logo {
  width: 320px;
  height: auto;
  z-index: 2;
  margin-top: -40px;
}

.setup-form {
  width: 100%;
  max-width: 620px;
  margin-top: -20px;
  padding: 20px 25px 15px;
}

.setup-steps {
  margin-bottom: 24px;

  .el-step__title {
    font-size: 12px;
    line-height: 1.3;
  }
}

.setup-step-content {
  min-height: 220px;
}

.setup-step-desc {
  color: #606266;
  margin-bottom: 16px;
  line-height: 1.6;
}

.setup-hint {
  color: #909399;
  font-size: 13px;
  line-height: 1.5;
  margin: 0 0 12px;
}

.setup-ai-host-hint {
  margin: 8px 0 0;
}

.setup-ai-demo-url {
  color: #409eff;
}

.setup-ai-demo-copy {
  padding: 0 4px;
  margin: 0 2px;
  vertical-align: middle;
  color: #909399;

  &:hover,
  &:focus {
    color: #409eff;
  }
}

.setup-switch-hint {
  margin-left: 12px;
  color: #909399;
  font-size: 13px;
}

.setup-test-ok {
  margin-left: 12px;
  color: #67c23a;
  font-size: 13px;
}

.setup-actions {
  margin-top: 16px;
  width: 100%;
}

.setup-nav-buttons {
  display: flex;
  gap: 8px;
}

.setup-summary {
  margin-bottom: 12px;
}

.setup-confirm-warning {
  color: #e6a23c;
}

.setup-success {
  text-align: center;
  padding: 32px 16px;

  h3 {
    margin: 16px 0 8px;
    color: #303133;
  }

  p {
    color: #606266;
    line-height: 1.6;
  }
}

.setup-success-icon {
  font-size: 64px;
  color: #67c23a;
}

.setup-success-hint {
  margin-top: 16px;
  font-weight: 500;
  color: #e6a23c;
}

.setup-success-warning {
  margin-top: 12px;
}

.setup-success-countdown {
  margin-top: 20px;
  color: #909399;
  font-size: 14px;
}

.setup-success-login-link {
  display: inline-block;
  margin-top: 12px;
  font-size: 15px;
}

@media screen and (max-width: 980px) {
  .setup-introduce {
    display: none;
  }

  .setup-body {
    padding: 0 16px;
  }
}
</style>
